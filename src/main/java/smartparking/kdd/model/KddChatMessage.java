package smartparking.kdd.model;

import java.time.LocalDateTime;

public class KddChatMessage {
    private String senderName;
    private String content;
    private LocalDateTime timestamp;

    public KddChatMessage(String senderName, String content) {
        this.senderName = senderName;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    public String getSenderName() {
        return senderName;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
