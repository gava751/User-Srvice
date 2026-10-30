package com.innoboard.userservice.service;

import com.innoboard.userservice.dto.CardRequest;
import com.innoboard.userservice.dto.CardResponse;

import java.util.List;

public interface PaymentCardService {

    CardResponse create(Long userId, CardRequest request);

    CardResponse getById(Long id);

    CardResponse update(Long id, CardRequest request);

    List<CardResponse> getAllByUserId(Long userId);

    CardResponse updateStatus(Long id, boolean active);

    void delete(Long id);
}
