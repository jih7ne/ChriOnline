package com.chrionline.utils;

import javafx.application.Platform;

public class CaptchaBridge {

    private String   token;
    private final Runnable onSuccess;
    private final Runnable onExpired;
    private Runnable onChallengeOpen;
    private Runnable onChallengeClose;

    public CaptchaBridge(Runnable onSuccess, Runnable onExpired) {
        this.onSuccess = onSuccess;
        this.onExpired = onExpired;
    }

    public void setChallengeCallbacks(Runnable onOpen, Runnable onClose) {
        this.onChallengeOpen  = onOpen;
        this.onChallengeClose = onClose;
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

    /** Appelée depuis JavaScript quand le popup de défi s'ouvre */
    public void onChallengeOpen() {
        if (onChallengeOpen != null) Platform.runLater(onChallengeOpen);
    }

    /** Appelée depuis JavaScript quand le popup de défi se ferme */
    public void onChallengeClose() {
        if (onChallengeClose != null) Platform.runLater(onChallengeClose);
    }

    public String getToken() { return token; }
    public void reset()      { this.token = null; }
}
