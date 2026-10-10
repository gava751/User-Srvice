package com.innoboard.userservice.service;

import com.innoboard.userservice.dto.PaymentCardDto;

import java.util.List;

public interface PaymentCardService {

    PaymentCardDto create (Long userId, PaymentCardDto dto);

    PaymentCardDto getById(Long userId, Long id);

    PaymentCardDto update(Long userId, Long id, PaymentCardDto dto);

    List<PaymentCardDto> getAllByUserId(Long userId);

    void activate(Long id);

    void deactivate(Long id);
}
