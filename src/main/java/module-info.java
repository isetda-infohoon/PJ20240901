module com.isetda.idpengine {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;

    opens com.isetda.idpengine to javafx.fxml;
    exports com.isetda.idpengine;
}