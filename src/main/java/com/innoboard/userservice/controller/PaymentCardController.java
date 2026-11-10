package com.innoboard.userservice.controller;

import com.innoboard.userservice.dto.CardRequest;
import com.innoboard.userservice.dto.CardResponse;
import com.innoboard.userservice.security.CurrentUser;
import com.innoboard.userservice.service.PaymentCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/cards")
@RequiredArgsConstructor
public class PaymentCardController {

    private final PaymentCardService paymentCardService;
    private final CurrentUser currentUser;

    @PostMapping
    ResponseEntity<CardResponse> create(@AuthenticationPrincipal Jwt jwt,
                                        @PathVariable Long userId,
                                        @Valid @RequestBody CardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentCardService.create(currentUser.resolveTarget(jwt, userId), request));
    }

    @GetMapping
    ResponseEntity<List<CardResponse>> getAllByUserId(@AuthenticationPrincipal Jwt jwt,
                                                      @PathVariable Long userId) {
        return ResponseEntity.ok(paymentCardService.getAllByUserId(currentUser.resolveTarget(jwt, userId)));
    }
}
