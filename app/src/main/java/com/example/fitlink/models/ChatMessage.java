package com.example.fitlink.models;

public class ChatMessage {
    private String messageId;
    private String senderId;
    private String senderName;
    private String text;
    private long timestamp;

    public ChatMessage() {
        // Required empty constructor for Firebase
    }

    public ChatMessage(String senderId, String senderName, String text, long timestamp) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}