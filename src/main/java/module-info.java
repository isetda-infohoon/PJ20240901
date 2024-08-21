module com.isetda.idpengine {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.json;
    requires google.cloud.storage;
    requires com.google.auth;
    requires com.google.auth.oauth2;
    requires google.cloud.core;
    requires google.cloud.vision;
    requires com.google.common;
    requires okhttp3;
    requires java.desktop;
    requires javafx.swing;
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;

    opens com.isetda.idpengine to javafx.fxml;
    exports com.isetda.idpengine;
}