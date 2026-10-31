package com.innoboard.userservice.service.impl;

import com.innoboard.userservice.config.RedisCacheConfig;
import com.innoboard.userservice.domain.PaymentCardEntity;
import com.innoboard.userservice.domain.UserEntity;
import com.innoboard.userservice.dto.CardRequest;
import com.innoboard.userservice.dto.CardResponse;
import com.innoboard.userservice.exception.CardLimitExceededException;
import com.innoboard.userservice.exception.CardNotFoundException;
import com.innoboard.userservice.exception.CardNumberAlreadyExistsException;
import com.innoboard.userservice.exception.UserNotFoundException;
import com.innoboard.userservice.mapper.PaymentCardMapper;
import com.innoboard.userservice.repository.PaymentCardRepository;
import com.innoboard.userservice.repository.UserRepository;
import com.innoboard.userservice.service.PaymentCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentCardServiceImpl implements PaymentCardService {

    private static final int MAX_CARDS_PER_USER = 5;

    private final CacheManager cacheManager;
    private final PaymentCardRepository paymentCardRepository;
    private final UserRepository userRepository;
    private final PaymentCardMapper cardMapper;

    @Override
    @Transactional
    public CardResponse create(Long userId, CardRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (paymentCardRepository.existsByNumber(request.number())) {
            throw new CardNumberAlreadyExistsException(request.number());
        }
        if (paymentCardRepository.countByUserIdAndActiveTrue(userId) >= MAX_CARDS_PER_USER) {
            throw new CardLimitExceededException(userId);
        }

        PaymentCardEntity card = cardMapper.toEntity(request);
        card.setActive(true);
        user.addCard(card);
        paymentCardRepository.save(card);
        evictOwnerCache(userId);

        return cardMapper.toResponse(card);
    }

    @Override
    @Transactional(readOnly = true)
    public CardResponse getById(Long id) {
        return cardMapper.toResponse(findCardOrThrow(id));
    }

    @Override
    @Transactional
    public CardResponse update(Long id, CardRequest request) {
        PaymentCardEntity card = findCardOrThrow(id);
        if (paymentCardRepository.existsByNumberAndIdNot(request.number(), id)) {
            throw new CardNumberAlreadyExistsException(request.number());
        }
        cardMapper.updateEntityFromDto(request, card);
        CardResponse result = cardMapper.toResponse(paymentCardRepository.save(card));
        evictOwnerCache(card.getUser().getId());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CardResponse> getAllByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        return cardMapper.toResponseList(paymentCardRepository.findByUserId(userId));
    }

    @Override
    @Transactional
    public CardResponse updateStatus(Long id, boolean active) {
        PaymentCardEntity card = findCardOrThrow(id);
        if (active && !card.getActive()
                && paymentCardRepository.countByUserIdAndActiveTrue(card.getUser().getId()) >= MAX_CARDS_PER_USER) {
            throw new CardLimitExceededException(card.getUser().getId());
        }
        int updated = paymentCardRepository.updateActiveStatus(id, active, Instant.now());
        if (updated == 0) {
            throw new CardNotFoundException(id);
        }
        CardResponse result = cardMapper.toResponse(paymentCardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException(id)));
        evictOwnerCache(card.getUser().getId());
        return result;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        PaymentCardEntity card = findCardOrThrow(id);
        Long ownerId = card.getUser().getId();
        paymentCardRepository.delete(card);
        evictOwnerCache(ownerId);
    }

    private void evictOwnerCache(Long ownerId) {
        Cache cache = cacheManager.getCache(RedisCacheConfig.USERS_WITH_CARDS);
        if (cache != null) {
            cache.evictIfPresent(ownerId);
        }
    }

    private PaymentCardEntity findCardOrThrow(Long id) {
        return paymentCardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException(id));
    }
}
