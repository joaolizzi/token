package com.joaolizzi.token.model;

import java.time.Instant;
import java.util.UUID;

public class SignatureRequest {

    public enum Status {
        PENDING,
        SIGNED
    }

    private final UUID id;
    private final String userId;
    private final String documentName;
    private final String documentHash;
    private final byte[] tokenHash;
    private final Instant createdAt;
    private final Instant expiresAt;

    private Status status;
    private Instant signedAt;
    private String validationCode;

    public SignatureRequest(
            UUID id,
            String userId,
            String documentName,
            String documentHash,
            byte[] tokenHash,
            Instant createdAt,
            Instant expiresAt
    ) {
        this.id = id;
        this.userId = userId;
        this.documentName = documentName;
        this.documentHash = documentHash;
        this.tokenHash = tokenHash.clone();
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.status = Status.PENDING;
    }

    public UUID getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getDocumentName() {
        return documentName;
    }

    public String getDocumentHash() {
        return documentHash;
    }

    public byte[] getTokenHash() {
        return tokenHash.clone();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getSignedAt() {
        return signedAt;
    }

    public String getValidationCode() {
        return validationCode;
    }

    public void markSigned(Instant signedAt, String validationCode) {
        this.status = Status.SIGNED;
        this.signedAt = signedAt;
        this.validationCode = validationCode;
    }
}
