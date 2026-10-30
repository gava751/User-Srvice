package com.innoboard.userservice.controller;

import com.innoboard.userservice.dto.CardRequest;
import com.innoboard.userservice.dto.CardResponse;
import com.innoboard.userservice.dto.UpdateStatusRequest;
import com.innoboard.userservice.service.PaymentCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

    private final PaymentCardService paymentCardService;

    @GetMapping("/{id}")
    ResponseEntity<CardResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentCardService.getById(id));
    }

    @PutMapping("/{id}")
    ResponseEntity<CardResponse> update(@PathVariable Long id, @Valid @RequestBody CardRequest request) {
        return ResponseEntity.ok(paymentCardService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    ResponseEntity<CardResponse> updateStatus(@PathVariable Long id,
                                              @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(paymentCardService.updateStatus(id, request.active()));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable Long id) {
        paymentCardService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
