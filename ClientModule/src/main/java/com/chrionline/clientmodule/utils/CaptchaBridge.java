package com.chrionline.clientmodule.utils;

import javafx.application.Platform;

public class CaptchaBridge {

    private String   token;
    private final Runnable onSuccess;
    private final Runnable onExpired;

    public CaptchaBridge(Runnable onSuccess, Runnable onExpired) {
        this.onSuccess = onSuccess;
        this.onExpired = onExpired;
    }

    /** Appelée depuis JavaScript — ne pas renommer */
    public void onTokenReceived(String token) {
        this.token = token;
        Platform.runLater(onSuccess);
    }

    /** Appelée depuis JavaScript quand le captcha expire */
    public void onTokenExpired() {
        this.token = null;
        Platform.runLater(onExpired);
    }

    public String getToken() { return token; }
    public void reset()      { this.token = null; }
}