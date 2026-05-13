module org.example.demo {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;
    requires java.net.http;
    requires com.google.zxing;
    requires com.google.zxing.javase;
    requires com.github.librepdf.openpdf;
    requires jbcrypt;
    requires com.google.gson;
    requires javafx.web;


    opens org.example.demo to javafx.fxml;
    exports org.example.demo;

    // Packages pour votre module Ordonnance
    opens com.onco to javafx.fxml;
    opens com.onco.controller to javafx.fxml;
    opens com.onco.model to javafx.fxml;
    opens com.onco.dao to javafx.fxml;
    exports com.onco;
    exports com.onco.controller;
    exports com.onco.model;

    // Packages pour le projet du groupe
    opens com.oncoreminder.app to javafx.fxml;
    opens com.oncoreminder.controllers to javafx.fxml;
    opens com.oncoreminder.models to javafx.fxml;
    opens com.oncoreminder.services to javafx.fxml;
    opens com.oncoreminder.utils to javafx.fxml;
    
    exports com.oncoreminder.app;
    exports com.oncoreminder.controllers;
    exports com.oncoreminder.models;
}
