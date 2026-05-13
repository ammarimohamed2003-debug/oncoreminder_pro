package com.onco.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDataBase {

    private static MyDataBase instance;
    private Connection cnx;

    private final String URL = "jdbc:mysql://localhost:3306/oncoreminder?useSSL=false&serverTimezone=UTC";
    private final String USER = "root";
    private final String PASSWORD = "123456";

    private MyDataBase() {
        try {
            cnx = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ DB Connected");
        } catch (SQLException e) {
            System.out.println("❌ DB Error: " + e.getMessage());
        }
    }

    public static MyDataBase getInstance() {
        if (instance == null) {
            instance = new MyDataBase();
        }
        return instance;
    }

    public Connection getCnx() {
        return cnx;
    }
}