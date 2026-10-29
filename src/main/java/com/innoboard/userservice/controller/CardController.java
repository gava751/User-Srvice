package com.innoboard.userservice.controller;

import com.innoboard.userservice.dto.PaymentCardDto;
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
    ResponseEntity<PaymentCardDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentCardService.getById(id));
    }

    @PutMapping("/{id}")
    ResponseEntity<PaymentCardDto> update(@PathVariable Long id,
                                          @Valid @RequestBody PaymentCardDto dto) {
        return ResponseEntity.ok(paymentCardService.update(id, dto));
    }

    @PatchMapping("/{id}/status")
    ResponseEntity<PaymentCardDto> updateStatus(@PathVariable Long id,
                                                @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(paymentCardService.updateStatus(id, request.active()));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable Long id) {
        paymentCardService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
