# SourisAlarm

**SourisAlarm** es una aplicación de escritorio que hice con fines de aprender java. Usa javaFX, y creo que fue una buena idea porque se ve mejor que una tipica app Java.

## Características Principales

* **Diseño Minimalista UI/UX:** Interfaz moderna, es lo que me interesaba hacer.
* **Reloj Digital 12h/24h:** Visualización clara con indicadores AM/PM dinámicos.
* **Temas Visuales:** Soporte completo para **Modo Claro** y **Modo Oscuro** con persistencia de configuración ya que odio que me quemen la retina xd.
* **Gestión de Alarmas:**
    * Crear alarmas únicas o repetitivas (días de la semana).
    * Activar/Desactivar alarmas con un clic.
    * Persistencia de datos (las alarmas no se borran al cerrar).
* **Personalización:**
    * Cambio de tipografías (Segoe UI, Roboto, etc.).
    * Selección de sonidos personalizados (.mp3, .wav).
    * Fondo de pantalla personalizable con efecto de desenfoque (Blur).
* **Integración con el Sistema:** Minimiza a la bandeja del sistema (System Tray) para funcionar en segundo plano.

## Tecnologías Utilizadas

* **Lenguaje:** Java 25
* **Framework UI:** JavaFX
* **Gestión de Dependencias:** Maven
* **Estilos:** CSS3
* **IDE Recomendado:** Visual Studio Code

## Instalación y Ejecución

Sigue estos pasos para ejecutar el proyecto en tu máquina local:

1.  **Clonar el repositorio:**
    ```bash
    git clone https://github.com/SourisCG/SourisAlarm.git
    cd SourisAlarm
    ```

2.  **Compilar el proyecto (con Maven):**
    ```bash
    mvn clean install
    ```

3.  **Ejecutar:**
    Puedes ejecutarlo desde tu IDE o buscar el archivo `.jar` generado en la carpeta `target/`.

##  Autor

**[Sebastián García]**
* Estudiante de Ingeniería de Software
* [GitHub](https://github.com/SourisCG)
* [LinkedIn](www.linkedin.com/in/souriscg)

---
*Este proyecto fue desarrollado con fines educativos y de portafolio.*