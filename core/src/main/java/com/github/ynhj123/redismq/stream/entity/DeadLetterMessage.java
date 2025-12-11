package com.github.ynhj123.redismq.stream.entity;

import java.io.Serializable;

public class DeadLetterMessage<T> implements Serializable {
    private T originalMessage;
    private String reason;
    private int attempts;
    private long timestamp;

    // getter and setter
    public T getOriginalMessage() {
        return originalMessage;
    }

    public void setOriginalMessage(T originalMessage) {
        this.originalMessage = originalMessage;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "DeadLetterMessage{" +
                "originalMessage=" + originalMessage +
                ", reason='" + reason + '\'' +
                ", attempts=" + attempts +
                ", timestamp=" + timestamp +
                '}';
    }
}
