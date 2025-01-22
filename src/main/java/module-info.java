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
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;
    requires com.google.gson;
    requires jdk.compiler;

    requires java.xml;
    requires org.apache.logging.log4j;

    requires java.desktop;
    requires org.apache.commons.io;
    requires java.sql;
//    requires com.sun.jna;
    requires org.apache.pdfbox;
    requires org.apache.commons.codec;
    requires com.google.api.client;
    requires commons.math3;
    requires org.apache.pdfbox.jbig2;
    requires google.api.client;

    opens com.isetda.idpengine to javafx.fxml;
    exports com.isetda.idpengine;
}