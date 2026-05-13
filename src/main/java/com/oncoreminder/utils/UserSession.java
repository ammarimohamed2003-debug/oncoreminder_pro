package com.oncoreminder.utils;

import com.oncoreminder.models.Utilisateur;

public class UserSession {
    private static UserSession instance;
    private Utilisateur currentUser;

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public void login(Utilisateur user) {
        this.currentUser = user;
    }

    public void logout() {
        this.currentUser = null;
    }

    public Utilisateur getCurrentUser() {
        return currentUser;
    }

    /** Met à jour l'utilisateur en session (après modification du profil). */
    public void setCurrentUser(Utilisateur user) {
        this.currentUser = user;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }
}
