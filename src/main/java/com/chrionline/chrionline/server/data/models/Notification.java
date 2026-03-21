package com.chrionline.chrionline.server.data.models;

import com.chrionline.chrionline.network.enums.NotificationStatus;
import com.chrionline.chrionline.network.enums.NotificationType;
import com.chrionline.chrionline.network.enums.Severity;

import java.time.LocalDateTime;
import java.util.UUID;

public class Notification {

    private Integer id;

    private String notificationUuid;
    private Utilisateur utilisateur;


    private NotificationType notificationType;


    private Severity severity;

    private String title;


    private String message;


    private Object data;

    private String source;
    private Integer version;


    private LocalDateTime dateCreation;


    private LocalDateTime dateEnvoi;


    private LocalDateTime dateLue;


    private LocalDateTime dateExpiration;


    private NotificationStatus statut;

    private Integer priorite;


    private Integer tentativeEnvoi = 0;


    private LocalDateTime derniereTentative;


    private String metadata;

    // Constructors
    public Notification() {
        this.notificationUuid = UUID.randomUUID().toString();
        this.dateCreation = LocalDateTime.now();
        this.statut = NotificationStatus.PENDING;
        this.tentativeEnvoi = 0;
        this.version = 1;
    }


    public void marquerCommeEnvoyee() {
        this.statut = NotificationStatus.SENT;
        this.dateEnvoi = LocalDateTime.now();
        this.tentativeEnvoi++;
        this.derniereTentative = LocalDateTime.now();
    }

    public void marquerCommeLue() {
        this.statut = NotificationStatus.READ;
        this.dateLue = LocalDateTime.now();
    }



    public boolean estExpiree() {
        return dateExpiration != null && dateExpiration.isBefore(LocalDateTime.now());
    }


}
