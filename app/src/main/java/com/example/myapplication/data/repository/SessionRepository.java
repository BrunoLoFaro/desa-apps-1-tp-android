package com.example.myapplication.data.repository;

import com.example.myapplication.data.model.User;
import com.example.myapplication.data.session.SessionManager;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SessionRepository {

    private final SessionManager sessionManager;

    @Inject
    public SessionRepository(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public void saveSession(String token, String refreshToken, User user) {
        sessionManager.saveSession(token, refreshToken, user.id, user.email, user.firstName, user.lastName);
    }

    public String getAccessToken() {
        return sessionManager.getAccessToken();
    }

    public long getUserId() {
        return sessionManager.getUserId();
    }

    public String getUserEmail() {
        return sessionManager.getUserEmail();
    }

    public String getFirstName() {
        return sessionManager.getFirstName();
    }

    public String getLastName() {
        return sessionManager.getLastName();
    }

    public boolean hasValidSession() {
        return sessionManager.hasValidSession();
    }

    public void clearSession() {
        sessionManager.clearSession();
    }
}
