module com.souris {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.desktop;

    opens com.souris to javafx.fxml;
    exports com.souris;
}
