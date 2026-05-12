package com.chrionline.network.protocol;

import com.chrionline.core.utils.JsonUtils;
import com.chrionline.network.enums.NotificationType;
import com.chrionline.network.enums.Severity;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AppNotification {
    private String id;
    private NotificationType type;
    private Severity severity;
    private String message;
    private long timestamp;
    private String source;
    private int version = 1;
    private Map<String, String> data;
    private String hmacSignature;


    private AppNotification() {
        this.data = new HashMap<>();
    }

    public String getId() {
        return id;
    }


    public NotificationType getType() {
        return type;
    }


    public Severity getSeverity() {
        return severity;
    }


    public String getMessage() {
        return message;
    }


    public long getTimestamp() {
        return timestamp;
    }


    public String getSource() {
        return source;
    }


    public int getVersion() {
        return version;
    }



    public Map<String, String> getData() {
        return data;
    }


    public String toJson() {
        return JsonUtils.toJson(this);
    }

    public String generateSignaturePayload() {
        return String.format("%s|%s|%s|%s|%s|%s|%d", 
                id, type, severity, message, timestamp, source, version);
    }

    public void sign() {
        this.hmacSignature = com.chrionline.security.utils.HmacUtil.sign(generateSignaturePayload());
    }

    public boolean verifySignature() {
        return com.chrionline.security.utils.HmacUtil.verify(generateSignaturePayload(), this.hmacSignature);
    }


    public static AppNotification fromJson(String json) {
        return JsonUtils.fromJson(json, AppNotification.class);
    }


    public static AppNotification info(String id, String message, String source) {
        return new Builder()
                .id(id)
                .type(NotificationType.INFO)
                .severity(Severity.LOW)
                .message(message)
                .source(source)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static AppNotification error(String id, String message, String source, Map<String, String> data) {
        return new Builder()
                .id(id)
                .type(NotificationType.ERROR)
                .severity(Severity.HIGH)
                .message(message)
                .source(source)
                .timestamp(System.currentTimeMillis())
                .data(data)
                .build();
    }

    public static class Builder {
        private final AppNotification notification;

        public Builder() {
            notification = new AppNotification();
        }
        public Builder id(String id) {
            notification.id = id;
            return this;
        }
        public Builder type(NotificationType type) {
            notification.type = type;
            return this;
        }
        public Builder severity(Severity severity) {
            notification.severity = severity;
            return this;
        }
        public Builder message(String message) {
            notification.message = message;
            return this;
        }
        public Builder timestamp(long timestamp) {
            notification.timestamp = timestamp;
            return this;
        }
        public Builder source(String source) {
            notification.source = source;
            return this;
        }

        public Builder data(Map<String, String> data) {
            notification.data = data;
            return this;
        }
        public Builder version(int version) {
            notification.version = version;
            return this;
        }
        public Builder hmacSignature(String hmacSignature) {
            notification.hmacSignature = hmacSignature;
            return this;
        }
        public AppNotification build() {
            return notification;
        }
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppNotification that = (AppNotification) o;
        return timestamp == that.timestamp &&
                version == that.version &&
                Objects.equals(id, that.id) &&
                type == that.type &&
                severity == that.severity &&
                Objects.equals(message, that.message) &&
                Objects.equals(source, that.source) &&
                Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, severity, message, timestamp, source, version, data);
    }

    @Override
    public String toString() {
        return "AppNotification{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", severity=" + severity +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                ", source='" + source + '\'' +
                ", version=" + version +
                ", data=" + data +
                '}';
    }
}
