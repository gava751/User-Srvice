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
@RequestMapping("/users/{userId}/cards")
@RequiredArgsConstructor
public class PaymentCardController {

    private final PaymentCardService paymentCardService;

    @PostMapping
    ResponseEntity<PaymentCardDto> create(@PathVariable Long userId,
                                          @Valid @RequestBody PaymentCardDto dto){
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentCardService.create(userId, dto));
    }

    @GetMapping("/{id}")
    ResponseEntity<PaymentCardDto> getById(@PathVariable Long userId, @PathVariable Long id){
        return ResponseEntity.ok(paymentCardService.getById(userId, id));
    }

    @GetMapping
    ResponseEntity<List<PaymentCardDto>> getAllByUserId(@PathVariable Long userId){
        return ResponseEntity.ok(paymentCardService.getAllByUserId(userId));
    }

    @PatchMapping("/{id}/activate")
    ResponseEntity<PaymentCardDto> activate(@PathVariable Long userId, @PathVariable Long id){
        return ResponseEntity.ok(paymentCardService.activate(userId, id));
    }

    @PatchMapping("/{id}/deactivate")
    ResponseEntity<PaymentCardDto> deactivate(@PathVariable Long userId, @PathVariable Long id){
        return ResponseEntity.ok(paymentCardService.deactivate(userId, id));
    }

    @PutMapping("/{id}")
    ResponseEntity<PaymentCardDto> update(@PathVariable Long userId,
                                          @PathVariable Long id,
                                          @Valid @RequestBody PaymentCardDto dto){
        return ResponseEntity.ok(paymentCardService.update(userId, id, dto));
    }
}
