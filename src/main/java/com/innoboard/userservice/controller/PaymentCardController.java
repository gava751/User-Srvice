package com.innoboard.userservice.controller;

import com.innoboard.userservice.dto.PaymentCardDto;
import com.innoboard.userservice.service.PaymentCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/cards")
@RequiredArgsConstructor
public class PaymentCardController {

    private final PaymentCardService paymentCardService;

    @PostMapping
    ResponseEntity<PaymentCardDto> create(@PathVariable Long userId,
                                          @Valid @RequestBody PaymentCardDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentCardService.create(userId, dto));
    }

    @GetMapping
    ResponseEntity<List<PaymentCardDto>> getAllByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(paymentCardService.getAllByUserId(userId));
    }
}
