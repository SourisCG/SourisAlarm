package com.souris;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.awt.Toolkit;

/**
 * CLASE PRINCIPAL
 * Punto de entrada de la aplicación. Configura la ventana, el menú y el icono de la bandeja.
 */
public class App extends Application {
    
    private Stage primaryStage;
    
    // Iconos para JavaFX (Ventana) y AWT (Bandeja del sistema)
    public static Image iconJavaFX;      
    public static java.awt.Image iconAWT; 

    @Override
    public void start(Stage stage) throws IOException {
        this.primaryStage = stage;
        
        // Evitar que la app muera al cerrar la ventana (para minimizar a bandeja)
        Platform.setImplicitExit(false);

        cargarIconoDesdeArchivo();

        if (iconJavaFX != null) stage.getIcons().add(iconJavaFX);

        // Cargar la vista (FXML)
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("primary.fxml"));
        StackPane rootLayout = fxmlLoader.load();
        PrimaryController controller = fxmlLoader.getController();

        // Crear y montar el menú superior
        MenuBar menuBar = crearMenuBar(controller, stage);
        insertarMenuEnLayout(rootLayout, menuBar);

        // Configurar Escena Transparente (para bordes redondeados personalizados)
        Scene scene = new Scene(rootLayout, 700, 650);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

        // Estilo de ventana sin decoración estándar OS
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(scene);
        stage.setTitle("SourisAlarm");
        
        // Configurar icono en la bandeja de Windows/Mac/Linux
        configurarSystemTray();

        stage.show();
    }

    /**
     * Inserta la barra de menú manualmente en el diseño personalizado.
     */
    private void insertarMenuEnLayout(StackPane root, MenuBar menuBar) {
        BorderPane mainContainer = (BorderPane) root.getChildren().get(1);
        Node topNode = mainContainer.getTop();
        VBox topContainer = new VBox();
        topContainer.setStyle("-fx-background-color: transparent;");
        
        mainContainer.setTop(null);
        if (topNode != null) topContainer.getChildren().add(topNode);
        topContainer.getChildren().add(menuBar);
        
        mainContainer.setTop(topContainer);
    }

    private void cargarIconoDesdeArchivo() {
        try {
            String rutaIcono = "/com/souris/icon.png";
            URL url = getClass().getResource(rutaIcono);
            if (url != null) {
                iconJavaFX = new Image(url.toExternalForm());
                iconAWT = Toolkit.getDefaultToolkit().createImage(url);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static Image getIcono() { return iconJavaFX; }

    /**
     * Configura el pequeño icono al lado del reloj de Windows.
     */
    private void configurarSystemTray() {
        if (!java.awt.SystemTray.isSupported() || iconAWT == null) return;
        
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                java.awt.SystemTray tray = java.awt.SystemTray.getSystemTray();
                java.awt.TrayIcon trayIcon = new java.awt.TrayIcon(iconAWT, "Alarma Pro");
                trayIcon.setImageAutoSize(true);
                
                // Doble clic para abrir
                trayIcon.addActionListener(e -> Platform.runLater(() -> mostrarVentana()));
                
                // Menú contextual (Clic derecho)
                java.awt.PopupMenu popup = new java.awt.PopupMenu();
                
                java.awt.MenuItem showItem = new java.awt.MenuItem("Abrir");
                showItem.addActionListener(e -> Platform.runLater(() -> mostrarVentana()));
                
                java.awt.MenuItem exitItem = new java.awt.MenuItem("Cerrar definitivamente");
                exitItem.addActionListener(e -> { 
                    tray.remove(trayIcon); 
                    Platform.exit(); 
                    System.exit(0); 
                });
                
                popup.add(showItem);
                popup.addSeparator();
                popup.add(exitItem);
                trayIcon.setPopupMenu(popup);
                
                tray.add(trayIcon);
            } catch (Exception e) { e.printStackTrace(); }
        });
    }
    
    private void mostrarVentana() {
        if (!primaryStage.isShowing()) primaryStage.show();
        primaryStage.toFront();
    }

    /**
     * Construye la barra de menú superior (Apariencia, Herramientas, etc.)
     */
    private MenuBar crearMenuBar(PrimaryController c, Stage s) {
        MenuBar mb = new MenuBar();
        
        // --- MENÚ APARIENCIA ---
        Menu m1 = new Menu("Apariencia");
        
        Menu t = new Menu("Temas");
        ToggleGroup tg = new ToggleGroup();
        RadioMenuItem rc = new RadioMenuItem("Claro"); rc.setToggleGroup(tg);
        RadioMenuItem ro = new RadioMenuItem("Oscuro"); ro.setToggleGroup(tg);
        
        if (c.isModoOscuro()) ro.setSelected(true); else rc.setSelected(true);
        rc.setOnAction(e->c.cambiarTema("Claro")); 
        ro.setOnAction(e->c.cambiarTema("Oscuro"));
        t.getItems().addAll(rc,ro);
        
        Menu f = new Menu("Fondo");
        MenuItem fi = new MenuItem("Seleccionar Imagen...");
        fi.setOnAction(e->{ 
            File file = fileChooser(s, "Imágenes", "*.jpg", "*.png", "*.jpeg"); 
            if(file!=null) c.cambiarImagenFondo(file); 
        });
        CheckMenuItem fb = new CheckMenuItem("Difuminar Fondo");
        fb.setOnAction(e->c.alternarDifuminado(fb.isSelected()));
        f.getItems().addAll(fi,fb);

        Menu fts = new Menu("Tipografía");
        ToggleGroup tgf = new ToggleGroup();
        crearItemFuente(c, fts, tgf, "Moderna (Segoe UI)", "Segoe UI");
        crearItemFuente(c, fts, tgf, "Elegante (Georgia)", "Georgia");
        crearItemFuente(c, fts, tgf, "Futurista (Consolas)", "Consolas");
        crearItemFuente(c, fts, tgf, "Limpia (Arial)", "Arial");
        
        m1.getItems().addAll(t, f, new SeparatorMenuItem(), fts);

        // --- MENÚ HERRAMIENTAS ---
        Menu m2 = new Menu("Herramientas");
        MenuItem h = new MenuItem("Historial"); h.setOnAction(e->c.mostrarHistorial());
        MenuItem a = new MenuItem("Seleccionar Audio..."); 
        a.setOnAction(e->{ 
            File file = fileChooser(s, "Audio", "*.mp3", "*.wav"); 
            if(file!=null) c.setArchivoSonido(file); 
        });
        m2.getItems().addAll(h,new SeparatorMenuItem(),a);
        
        // --- MENÚ CRÉDITOS ---
        Menu m3 = new Menu("Créditos");
        MenuItem ac = new MenuItem("Acerca de"); ac.setOnAction(e->c.mostrarCreditos());
        MenuItem sa = new MenuItem("Salir"); 
        sa.setOnAction(e->{ Platform.exit(); System.exit(0); });
        m3.getItems().addAll(ac,new SeparatorMenuItem(),sa);
        
        mb.getMenus().addAll(m1,m2,m3);
        return mb;
    }
    
    // Helper para crear items de menú de fuentes
    private void crearItemFuente(PrimaryController c, Menu m, ToggleGroup g, String label, String font) {
        RadioMenuItem item = new RadioMenuItem(label);
        item.setToggleGroup(g);
        item.setOnAction(e -> c.cambiarTipografia(font));
        if (c.getFuenteActual().equals(font)) item.setSelected(true);
        m.getItems().add(item);
    }
    
    private File fileChooser(Stage s, String desc, String... ex) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(desc, ex));
        return fc.showOpenDialog(s);
    }

    public static void main(String[] args) { launch(); }
}