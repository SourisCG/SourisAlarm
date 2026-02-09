package com.souris;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedList;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.util.StringConverter;
import java.awt.Desktop;
import java.net.URI;

/**
 * CONTROLADOR PRINCIPAL
 * Se encarga de toda la lógica de la interfaz: el reloj, las alarmas,
 * la configuración y los eventos de los botones.
 */
public class PrimaryController {

    // =========================================================================
    //                            ELEMENTOS DE LA UI (FXML)
    // =========================================================================
    @FXML private StackPane rootStack;          // Contenedor raíz (para efectos globales)
    @FXML private BorderPane mainContainer;     // Contenedor principal
    @FXML private ImageView fondoImageView;     // Imagen de fondo
    @FXML private ImageView windowIcon;         // Icono de la ventana
    
    // --- Reloj ---
    @FXML private Spinner<Integer> spinnerHoras;
    @FXML private Spinner<Integer> spinnerMinutos;
    @FXML private ToggleButton toggleAmPm;
    
    // --- Controles ---
    @FXML private HBox dayContainer;            // Contenedor de botones de días (L, M, M...)
    @FXML private ListView<Alarma> listViewAlarmas; // Lista visual de alarmas
    @FXML private Slider sliderVolumen;         // Control de volumen

    // =========================================================================
    //                            VARIABLES DE ESTADO
    // =========================================================================
    // Lista observable: Si la modificamos, la UI se actualiza sola
    private ObservableList<Alarma> listaAlarmas = FXCollections.observableArrayList();
    
    // Historial de eventos (para saber cuándo sonaron las alarmas)
    private LinkedList<String> historialLog = new LinkedList<>();
    
    // Servicio de audio (maneja la reproducción de sonido)
    private AudioService audioService = new AudioService();
    
    // El "corazón" del reloj (se ejecuta cada segundo)
    private Timeline timeline;
    
    // Array para controlar los 7 botones de los días
    private ToggleButton[] dayToggles = new ToggleButton[7];
    private final String[] dayLabels = {"D", "L", "M", "M", "J", "V", "S"};
    
    // Variables para evitar que la alarma suene repetidamente en el mismo segundo
    private Alarma ultimaAlarmaSonada = null;
    private LocalDate diaUltimoSonido = null;
    
    // Variables para mover la ventana sin bordes
    private double xOffset = 0;
    private double yOffset = 0;
    
    // Configuración actual
    private String fuenteActual = "Segoe UI";
    private String estiloFondo = ""; 
    private boolean esOscuro = false;
    private String rutaFondoActual = null;
    private String rutaSonidoActual = null;

    // Archivo donde guardamos los datos
    private static final String ARCHIVO_DATOS = "alarma_config.dat";

    // =========================================================================
    //                            INICIALIZACIÓN
    // =========================================================================
    
    /**
     * Método que se ejecuta automáticamente al abrir la ventana.
     * Aquí configuramos todo lo necesario para empezar.
     */
    @FXML
    public void initialize() {
        setupIconoVentana();
        setupSpinners();      // Configurar los números del reloj
        setupAmPmToggle();    // Configurar botón AM/PM
        setupDayToggles();    // Crear los botones de los días
        setupList();          // Configurar la lista de alarmas
        
        iniciarReloj();       // ¡Arrancar el segundero!
        
        // Hacer que la imagen de fondo se estire con la ventana
        fondoImageView.fitWidthProperty().bind(rootStack.widthProperty());
        fondoImageView.fitHeightProperty().bind(rootStack.heightProperty());
        
        // Conectar el slider de volumen
        sliderVolumen.valueProperty().addListener((o, ov, nv) -> audioService.setVolumen(nv.doubleValue()));
        
        // Cargar los datos guardados anteriormente
        cargarConfiguracion();
    }

    private void setupIconoVentana() {
        if (App.getIcono() != null && windowIcon != null) {
            windowIcon.setImage(App.getIcono());
        }
    }

    /**
     * Configura los selectores de Hora y Minutos.
     * Define los límites (1-12 horas, 0-59 minutos) y el formato "00".
     */
    private void setupSpinners() {
        // --- Configuración Horas (1 a 12) ---
        SpinnerValueFactory<Integer> valHoras = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 12, 12);
        valHoras.setWrapAround(true); // Si pasas de 12, vuelve a 1
        spinnerHoras.setValueFactory(valHoras);

        // --- Configuración Minutos (0 a 59) ---
        SpinnerValueFactory<Integer> valMin = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0);
        valMin.setWrapAround(true); 
        
        // Convertidor para que muestre "05" en lugar de "5"
        valMin.setConverter(new StringConverter<Integer>() {
            @Override
            public String toString(Integer value) {
                if (value == null) return "";
                return String.format("%02d", value);
            }
            @Override
            public Integer fromString(String string) {
                if (string == null || string.trim().isEmpty()) return 0;
                return Integer.valueOf(string);
            }
        });
        spinnerMinutos.setValueFactory(valMin);
    }

    private void setupAmPmToggle() {
        toggleAmPm.setText("AM");
        toggleAmPm.selectedProperty().addListener((obs, oldVal, newVal) -> 
            toggleAmPm.setText(newVal ? "PM" : "AM")
        );
    }

    private void setupDayToggles() {
        // Creamos los 7 botones para los días de la semana
        for (int i = 0; i < 7; i++) {
            ToggleButton tb = new ToggleButton(dayLabels[i]);
            tb.getStyleClass().add("day-toggle");
            dayToggles[i] = tb;
            dayContainer.getChildren().add(tb);
        }
    }

    // =========================================================================
    //                        LÓGICA DE ALARMAS
    // =========================================================================

    /**
     * Acción del botón "Guardar".
     * Lee la hora del reloj, convierte a formato 24h internamente y guarda la alarma.
     */
    @FXML 
    private void handleAgregarAlarma() {
        int horaSeleccionada = spinnerHoras.getValue();
        int minutoSeleccionado = spinnerMinutos.getValue();
        boolean esPM = toggleAmPm.isSelected();
        
        // Conversión a formato 24h para lógica interna
        int hora24 = horaSeleccionada;
        if (esPM) {
            if (horaSeleccionada != 12) hora24 = horaSeleccionada + 12;
        } else {
            if (horaSeleccionada == 12) hora24 = 0;
        }
        
        // Ver qué días están marcados
        boolean[] diasActivos = new boolean[7];
        for (int i = 0; i < 7; i++) {
            diasActivos[i] = dayToggles[i].isSelected();
        }
        
        // Crear y añadir la alarma
        Alarma nuevaAlarma = new Alarma(LocalTime.of(hora24, minutoSeleccionado), diasActivos);
        listaAlarmas.add(nuevaAlarma);
        
        guardarConfiguracion(); // Guardar cambios en disco
    }

    /**
     * Configura cómo se ve cada celda de la lista de alarmas.
     * Define los botones ON/OFF y Eliminar.
     */
    private void setupList() {
        listViewAlarmas.setItems(listaAlarmas);
        
        listViewAlarmas.setCellFactory(param -> new ListCell<Alarma>() {
            @Override protected void updateItem(Alarma item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    // Diseño de la tarjeta de alarma
                    HBox card = new HBox(15);
                    card.getStyleClass().add("alarm-card");
                    card.setAlignment(Pos.CENTER_LEFT);
                    
                    // Info de texto (Hora y Días)
                    VBox info = new VBox(3); 
                    String horaStr = item.getHora().format(DateTimeFormatter.ofPattern("h:mm a")).toUpperCase();
                    
                    Label lblHora = new Label(horaStr);
                    lblHora.getStyleClass().add("alarm-time-text");
                    lblHora.setStyle("-fx-font-family: '" + fuenteActual + "';");
                    
                    String diasStr = item.esUnaSolaVez() ? "Una vez" : construirTextoDias(item); 
                    Label lblDias = new Label(diasStr);
                    lblDias.getStyleClass().add("alarm-date-text");
                    lblDias.setStyle("-fx-font-family: '" + fuenteActual + "';");
                    
                    info.getChildren().addAll(lblHora, lblDias);
                    
                    // Espaciador flexible
                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    
                    // Botón ON/OFF
                    Button btnToggle = new Button(item.isActiva() ? "ON" : "OFF");
                    btnToggle.getStyleClass().addAll("status-button", item.isActiva() ? "status-on" : "status-off"); 
                    btnToggle.setOnAction(e -> {
                        item.setActiva(!item.isActiva());
                        listViewAlarmas.refresh();
                        guardarConfiguracion();
                    });
                    
                    // Botón Eliminar
                    Button btnDelete = new Button("✕");
                    btnDelete.getStyleClass().add("btn-delete"); 
                    btnDelete.setOnAction(e -> {
                        listaAlarmas.remove(item);
                        guardarConfiguracion();
                    });
                    
                    card.getChildren().addAll(info, spacer, btnToggle, btnDelete);
                    setGraphic(card);
                }
            }
        });
    }

    // =========================================================================
    //                        EL CORAZÓN DEL RELOJ
    // =========================================================================

    /**
     * Inicia el temporizador que revisa cada segundo si debe sonar una alarma.
     */
    private void iniciarReloj() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> verificarAlarmas()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    /**
     * Lógica crítica: Compara la hora actual con todas las alarmas activas.
     */
    private void verificarAlarmas() {
        LocalTime ahora = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
        LocalDate hoy = LocalDate.now();
        int diaSemanaHoy = hoy.getDayOfWeek().getValue(); // 1=Lunes, 7=Domingo
        
        for (Alarma alarma : listaAlarmas) {
            if (!alarma.isActiva()) continue; // Si está apagada, saltar
            
            // Si coincide la hora y el minuto
            if (alarma.getHora().equals(ahora)) {
                
                // Verificar si toca sonar hoy (o si es de una sola vez)
                boolean tocaSonar = alarma.esUnaSolaVez() || alarma.debeSonarHoy(diaSemanaHoy);
                
                if (tocaSonar) {
                    // Evitar que suene muchas veces en el mismo segundo
                    if (alarma != ultimaAlarmaSonada || !hoy.equals(diaUltimoSonido)) {
                        dispararAlarma();
                        
                        ultimaAlarmaSonada = alarma;
                        diaUltimoSonido = hoy;
                        
                        // Si era de "una sola vez", la apagamos después de sonar
                        if (alarma.esUnaSolaVez()) {
                            alarma.setActiva(false);
                            listViewAlarmas.refresh();
                        }
                    }
                }
            }
        }
    }

    private void dispararAlarma() {
        // Guardar en historial
        historialLog.addFirst(LocalDate.now() + " " + LocalTime.now());
        if (historialLog.size() > 10) historialLog.removeLast();
        
        // Reproducir sonido y mostrar ventana
        audioService.reproducir();
        mostrarPantallaAlarma();
    }

    // =========================================================================
    //                        PANTALLA DE ALERTA (FULLSCREEN)
    // =========================================================================
    private void mostrarPantallaAlarma() {
        Stage stage = new Stage();
        if (App.getIcono() != null) stage.getIcons().add(App.getIcono());
        
        BorderPane layout = new BorderPane();
        layout.setStyle("-fx-background-color: #1e272e;"); // Fondo oscuro elegante
        
        // Parte Superior: Hora y Fecha
        VBox top = new VBox(10);
        top.setAlignment(Pos.CENTER);
        top.setPadding(new javafx.geometry.Insets(80,0,0,0));
        
        Label lblHora = new Label(LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a")).toUpperCase());
        lblHora.getStyleClass().add("alarm-screen-time");
        
        Label lblFecha = new Label(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM")).toUpperCase());
        lblFecha.getStyleClass().add("alarm-screen-date");
        
        top.getChildren().addAll(lblHora, lblFecha);
        layout.setTop(top);
        
        // Centro: Botón Detener Gigante
        Button btnStop = new Button("DETENER");
        btnStop.getStyleClass().add("stop-alarm-button");
        btnStop.setOnAction(e -> {
            stage.close();
            audioService.detener();
        });
        
        VBox centerZone = new VBox(btnStop);
        centerZone.setAlignment(Pos.CENTER);
        layout.setCenter(centerZone);
        
        // Configurar Escena
        Scene scene = new Scene(layout);
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
        
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setFullScreen(true);
        stage.setScene(scene);
        stage.setAlwaysOnTop(true);
        stage.show();
    }

    // =========================================================================
    //                        PERSISTENCIA (GUARDAR/CARGAR)
    // =========================================================================
    public void guardarConfiguracion() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(ARCHIVO_DATOS))) {
            AppState estado = new AppState();
            estado.alarmasGuardadas = new ArrayList<>(listaAlarmas);
            estado.modoOscuroActivado = esOscuro;
            estado.nivelVolumen = sliderVolumen.getValue();
            estado.rutaImagenFondo = rutaFondoActual;
            estado.nombreFuente = fuenteActual;
            estado.rutaSonido = rutaSonidoActual;
            
            oos.writeObject(estado);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cargarConfiguracion() {
        File f = new File(ARCHIVO_DATOS);
        
        // Si no hay archivo, ponemos la hora actual y salimos
        if (!f.exists()) {
            setupHoraActualEnSpinners();
            return;
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            AppState estado = (AppState) ois.readObject();
            
            // Restaurar datos
            if (estado.alarmasGuardadas != null) listaAlarmas.setAll(estado.alarmasGuardadas);
            if (estado.modoOscuroActivado) cambiarTema("Oscuro"); else cambiarTema("Claro");
            
            sliderVolumen.setValue(estado.nivelVolumen);
            
            if (estado.rutaImagenFondo != null) {
                File imgFile = new File(estado.rutaImagenFondo);
                if (imgFile.exists()) cambiarImagenFondo(imgFile);
            }
            if (estado.nombreFuente != null) cambiarTipografia(estado.nombreFuente);
            if (estado.rutaSonido != null) {
                File sonidoFile = new File(estado.rutaSonido);
                if (sonidoFile.exists()) setArchivoSonido(sonidoFile);
            }
            
            setupHoraActualEnSpinners();
            
        } catch (Exception e) {
            e.printStackTrace();
            setupHoraActualEnSpinners();
        }
    }

    private void setupHoraActualEnSpinners() {
        LocalTime now = LocalTime.now();
        int h24 = now.getHour();
        boolean esPm = false;
        int h12 = h24;
        
        // Convertir 24h a 12h para mostrar al inicio
        if (h24 >= 12) {
            esPm = true;
            if (h24 > 12) h12 = h24 - 12;
        } else {
            if (h24 == 0) h12 = 12;
        }
        
        spinnerHoras.getValueFactory().setValue(h12);
        spinnerMinutos.getValueFactory().setValue(now.getMinute());
        toggleAmPm.setSelected(esPm);
    }

    // =========================================================================
    //                        MÉTODOS PÚBLICOS (API)
    // =========================================================================
    public boolean isModoOscuro() { return esOscuro; }
    public String getFuenteActual() { return fuenteActual; }

    public void cambiarTipografia(String fontName) {
        this.fuenteActual = fontName;
        aplicarEstiloGlobal();
        listViewAlarmas.refresh();
        guardarConfiguracion();
    }

    public void setArchivoSonido(File file) {
        if (file != null) {
            this.rutaSonidoActual = file.getAbsolutePath();
            audioService.setArchivoPersonalizado(file);
            guardarConfiguracion();
        }
    }
    
    public void cambiarTema(String tema) {
        rootStack.getStyleClass().removeAll("dark-mode");
        if ("Oscuro".equals(tema)) {
            rootStack.getStyleClass().add("dark-mode");
            esOscuro = true;
        } else {
            esOscuro = false;
        }
        aplicarEstiloGlobal();
    }
    
    public void cambiarImagenFondo(File file) {
        if (file != null) {
            this.rutaFondoActual = file.getAbsolutePath();
            fondoImageView.setImage(new Image(file.toURI().toString()));
            
            // Ajustar opacidad según tema para legibilidad
            estiloFondo = "-fx-background-color: rgba(255,255,255,0.7);";
            if (esOscuro) estiloFondo = "-fx-background-color: rgba(0,0,0,0.7);";
            
            aplicarEstiloGlobal();
            guardarConfiguracion();
        }
    }
    
    public void alternarDifuminado(boolean activar) {
        fondoImageView.setEffect(activar ? new GaussianBlur(20) : null);
    }

    // --- Helpers visuales ---
    private void aplicarEstiloGlobal() {
        String style = "-fx-font-family: '" + fuenteActual + "';";
        if (fondoImageView.getImage() != null) {
            style += estiloFondo;
        }
        mainContainer.setStyle(style);
    }

    private String construirTextoDias(Alarma a) {
        StringBuilder sb = new StringBuilder();
        String[] cortos = {"Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"};
        for(int i=0; i<7; i++) {
            if(a.isDiaActivo(i)) {
                if(sb.length() > 0) sb.append(", ");
                sb.append(cortos[i]);
            }
        }
        return sb.length() > 0 ? sb.toString() : "Repetir";
    }

    // --- Ventanas de información ---
    public void mostrarHistorial() {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Historial");
        aplicarIconoAlert(a);
        StringBuilder sb = new StringBuilder();
        historialLog.forEach(s -> sb.append("🔔 ").append(s).append("\n"));
        a.setContentText(sb.length() > 0 ? sb.toString() : "Sin eventos recientes.");
        estilizarAlerta(a);
        a.showAndWait();
    }

    public void mostrarCreditos() {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Créditos");
        a.setHeaderText("Acerca de SourisAlaram");
        aplicarIconoAlert(a);
        
        VBox content = new VBox(10);
        content.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label("Desarrollado por: Souris\nVersión: 1.0\n\nVisita mi perfil:");
        lbl.setWrapText(true);
        if(esOscuro) lbl.setStyle("-fx-text-fill: white;");
        
        Hyperlink link = new Hyperlink("https://github.com/SourisCG");
        link.setStyle("-fx-border-color: transparent; -fx-font-size: 14px;");
        link.setOnAction(e -> {
            try { Desktop.getDesktop().browse(new URI("https://github.com/SourisCG")); } 
            catch (Exception ex) { ex.printStackTrace(); }
        });
        
        content.getChildren().addAll(lbl, link);
        a.getDialogPane().setContent(content);
        estilizarAlerta(a);
        a.showAndWait();
    }

    private void aplicarIconoAlert(Alert alert) {
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        if(App.getIcono() != null) stage.getIcons().add(App.getIcono());
    }

    private void estilizarAlerta(Alert a) {
        DialogPane dp = a.getDialogPane();
        dp.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
        if (esOscuro) dp.getStyleClass().add("dark-mode");
    }

    // =========================================================================
    //                        EVENTOS DE VENTANA (CERRAR/MOVER)
    // =========================================================================
    @FXML private void handleCerrar() {
        guardarConfiguracion();
        Stage stage = (Stage) rootStack.getScene().getWindow();
        stage.hide(); // Ocultar, no cerrar (se queda en la bandeja)
        
        // Si no hay bandeja de sistema soportada, cerrar del todo
        if (!java.awt.SystemTray.isSupported()) {
            Platform.exit();
            System.exit(0);
        }
    }
    
    @FXML private void handleMinimizar() {
        ((Stage) rootStack.getScene().getWindow()).setIconified(true);
    }
    
    @FXML private void handleMousePressed(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }
    
    @FXML private void handleMouseDragged(MouseEvent event) {
        Stage stage = (Stage) rootStack.getScene().getWindow();
        stage.setX(event.getScreenX() - xOffset);
        stage.setY(event.getScreenY() - yOffset);
    }
}