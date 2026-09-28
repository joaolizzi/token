package com.joaolizzi.token.service;

import com.joaolizzi.token.model.SignatureRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SignatureRequestService {

    public record CreatedRequest(SignatureRequest request, String token) {}

    private static final Duration TOKEN_TTL = Duration.ofMinutes(10);

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<UUID, SignatureRequest> requests = new ConcurrentHashMap<>();
    private final Map<String, UUID> validationIndex = new ConcurrentHashMap<>();

    public CreatedRequest create(String userId, String documentName, String documentHash) {
        requireText(userId, "userId");
        requireText(documentName, "documentName");
        requireText(documentHash, "documentHash");

        String token = randomToken(32);
        Instant now = Instant.now();

        SignatureRequest request = new SignatureRequest(
                UUID.randomUUID(),
                userId,
                documentName,
                documentHash,
                sha256(token),
                now,
                now.plus(TOKEN_TTL)
        );

        requests.put(request.getId(), request);
        return new CreatedRequest(request, token);
    }

    public SignatureRequest get(UUID id) {
        SignatureRequest request = requests.get(id);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada");
        }
        return request;
    }

    public synchronized SignatureRequest sign(UUID id, String authenticatedUserId, String token) {
        SignatureRequest request = get(id);

        requireText(authenticatedUserId, "userId");
        requireText(token, "token");

        if (!request.getUserId().equals(authenticatedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta solicitação pertence a outro usuário");
        }

        if (request.getStatus() == SignatureRequest.Status.SIGNED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solicitação já assinada");
        }

        if (Instant.now().isAfter(request.getExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Token expirado");
        }

        byte[] informedTokenHash = sha256(token);
        if (!MessageDigest.isEqual(request.getTokenHash(), informedTokenHash)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido");
        }

        String validationCode = randomToken(18);
        request.markSigned(Instant.now(), validationCode);
        validationIndex.put(validationCode, request.getId());

        return request;
    }

    public SignatureRequest validate(String validationCode) {
        UUID requestId = validationIndex.get(validationCode);
        if (requestId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Código de validação inválido");
        }

        SignatureRequest request = get(requestId);
        if (request.getStatus() != SignatureRequest.Status.SIGNED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Documento ainda não foi assinado");
        }

        return request;
    }

    private String randomToken(int bytes) {
        byte[] data = new byte[bytes];
        secureRandom.nextBytes(data);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private byte[] sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível", e);
        }
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " é obrigatório");
        }
    }
}
