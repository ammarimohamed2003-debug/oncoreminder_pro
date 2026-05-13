package com.oncoreminder.models;

import java.time.LocalDateTime;

public class LogConnexion {
    private int id;
    private int userId;
    private LocalDateTime dateConnexion;
    private String userEmail; // For display

    public LogConnexion() {}

    public LogConnexion(int id, int userId, LocalDateTime dateConnexion) {
        this.id = id;
        this.userId = userId;
        this.dateConnexion = dateConnexion;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public LocalDateTime getDateConnexion() { return dateConnexion; }
    public void setDateConnexion(LocalDateTime dateConnexion) { this.dateConnexion = dateConnexion; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
}
