package service;

import util.AppLogger;

public class UserSession {
    private String currentUser;

    public void setLogin(String login) {
        currentUser = login;
        AppLogger.admin("Session: current user changed to \"" + login + "\".");
    }

    public String getLogin() {
        return currentUser;
    }
}