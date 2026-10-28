package com.koperasi.payment.controller;

import com.koperasi.payment.domain.dto.ApiResponse;
import com.koperasi.payment.event.WebhookResult;
import com.koperasi.payment.service.WebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/midtrans")
    public ResponseEntity<ApiResponse<Map<String, Object>>> midtrans(
            @RequestBody Map<String, Object> payload) {
        WebhookResult result = webhookService.handleMidtrans(payload);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("processed", result.processed()), "Webhook diproses"));
    }

    @PostMapping("/xendit")
    public ResponseEntity<ApiResponse<Map<String, Object>>> xendit(
            @RequestHeader(name = "X-Callback-Token", required = false) String callbackToken,
            @RequestBody Map<String, Object> payload) {
        WebhookResult result = webhookService.handleXendit(callbackToken, payload);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("processed", result.processed()), "Webhook diproses"));
    }
}
