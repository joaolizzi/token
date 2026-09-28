package com.joaolizzi.token.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.joaolizzi.token.model.SignatureRequest;
import com.joaolizzi.token.service.SignatureRequestService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class SignatureRequestController {

    public record CreateRequestBody(String userId, String documentName, String documentHash) {}
    public record SignRequestBody(String userId, String token) {}

    public record CreatedResponse(
            UUID requestId,
            String userId,
            String documentName,
            String status,
            Instant expiresAt,
            String token,
            String warning
    ) {}

    public record RequestStatusResponse(
            UUID requestId,
            String userId,
            String documentName,
            String documentHash,
            String status,
            Instant createdAt,
            Instant expiresAt,
            Instant signedAt,
            String validationCode
    ) {}

    public record ValidationResponse(
            boolean valid,
            String status,
            String validationCode,
            String userId,
            String documentName,
            String documentHash,
            Instant signedAt
    ) {}

    private final SignatureRequestService service;
    private final String publicBaseUrl;

    public SignatureRequestController(
            SignatureRequestService service,
            @Value("${app.public-base-url:http://localhost:8080}") String publicBaseUrl
    ) {
        this.service = service;
        this.publicBaseUrl = publicBaseUrl;
    }

    @PostMapping("/requests")
    public ResponseEntity<CreatedResponse> create(@RequestBody CreateRequestBody body) {
        SignatureRequestService.CreatedRequest created = service.create(
                body.userId(),
                body.documentName(),
                body.documentHash()
        );

        SignatureRequest request = created.request();

        return ResponseEntity.status(201).body(new CreatedResponse(
                request.getId(),
                request.getUserId(),
                request.getDocumentName(),
                request.getStatus().name(),
                request.getExpiresAt(),
                created.token(),
                "TESTE: o token aparece nesta resposta apenas durante o protótipo"
        ));
    }

    @GetMapping("/requests/{id}")
    public RequestStatusResponse status(@PathVariable UUID id) {
        return toStatus(service.get(id));
    }

    @PostMapping("/requests/{id}/sign")
    public RequestStatusResponse sign(@PathVariable UUID id, @RequestBody SignRequestBody body) {
        return toStatus(service.sign(id, body.userId(), body.token()));
    }

    @GetMapping("/validation/{validationCode}")
    public ValidationResponse validate(@PathVariable String validationCode) {
        SignatureRequest request = service.validate(validationCode);

        return new ValidationResponse(
                true,
                "VALID",
                request.getValidationCode(),
                request.getUserId(),
                request.getDocumentName(),
                request.getDocumentHash(),
                request.getSignedAt()
        );
    }

    @GetMapping(value = "/validation/{validationCode}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] qr(@PathVariable String validationCode) throws WriterException, IOException {
        service.validate(validationCode);

        String validationUrl = publicBaseUrl + "/api/validation/" + validationCode;
        BitMatrix matrix = new QRCodeWriter().encode(validationUrl, BarcodeFormat.QR_CODE, 320, 320);

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        }
    }

    private RequestStatusResponse toStatus(SignatureRequest request) {
        return new RequestStatusResponse(
                request.getId(),
                request.getUserId(),
                request.getDocumentName(),
                request.getDocumentHash(),
                request.getStatus().name(),
                request.getCreatedAt(),
                request.getExpiresAt(),
                request.getSignedAt(),
                request.getValidationCode()
        );
    }
}
