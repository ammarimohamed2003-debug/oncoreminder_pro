module org.example.demo {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens org.example.demo to javafx.fxml;
    exports org.example.demo;
    opens com.onco to javafx.fxml;
    opens com.onco.controller to javafx.fxml;
    exports com.onco;
}
