package com.innoboard.userservice.service;

import com.innoboard.userservice.dto.PaymentCardDto;

import java.util.List;

public interface PaymentCardService {

    PaymentCardDto create(Long userId, PaymentCardDto dto);

    PaymentCardDto getById(Long id);

    PaymentCardDto update(Long id, PaymentCardDto dto);

    List<PaymentCardDto> getAllByUserId(Long userId);

    PaymentCardDto updateStatus(Long id, boolean active);

    void delete(Long id);
}
