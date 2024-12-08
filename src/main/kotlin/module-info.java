module io.vdartsabvile.avspeak {
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlin.stdlib;
    requires atlantafx.base;
    requires java.desktop;
    requires javax.mail.api;
    requires okhttp3;
    requires java.sql;
    requires org.json;
    requires javafx.swing;
    requires javafx.media;
    requires java.prefs;
    requires javafx.web;
    requires org.java_websocket;
    requires kotlinx.coroutines.core;
    requires webrtc.java;
    requires fmj;
    opens io.vdartsabvile.avspeak to javafx.fxml;
    exports io.vdartsabvile.avspeak;
    exports io.vdartsabvile.avspeak.Client to javafx.graphics;

}