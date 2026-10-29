package com.innoboard.userservice.service.impl;

import com.innoboard.userservice.config.RedisCacheConfig;
import com.innoboard.userservice.domain.PaymentCardEntity;
import com.innoboard.userservice.domain.UserEntity;
import com.innoboard.userservice.dto.PaymentCardDto;
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
import org.springframework.cache.annotation.CacheEvict;
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
    @CacheEvict(cacheNames = RedisCacheConfig.USERS_WITH_CARDS, key = "#userId")
    public PaymentCardDto create(Long userId, PaymentCardDto dto) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (paymentCardRepository.existsByNumber(dto.number())) {
            throw new CardNumberAlreadyExistsException(dto.number());
        }
        if (paymentCardRepository.countByUserIdAndActiveTrue(userId) >= MAX_CARDS_PER_USER) {
            throw new CardLimitExceededException(userId);
        }

        PaymentCardEntity card = cardMapper.toEntity(dto);
        card.setActive(true);
        user.addCard(card);
        paymentCardRepository.save(card);

        return cardMapper.toDto(card);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentCardDto getById(Long id) {
        return cardMapper.toDto(findCardOrThrow(id));
    }

    @Override
    @Transactional
    public PaymentCardDto update(Long id, PaymentCardDto dto) {
        PaymentCardEntity card = findCardOrThrow(id);
        if (paymentCardRepository.existsByNumberAndIdNot(dto.number(), id)) {
            throw new CardNumberAlreadyExistsException(dto.number());
        }
        cardMapper.updateEntityFromDto(dto, card);
        PaymentCardDto result = cardMapper.toDto(paymentCardRepository.save(card));
        evictOwnerCache(card.getUser().getId());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentCardDto> getAllByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        return cardMapper.toDtoList(paymentCardRepository.findByUserId(userId));
    }

    @Override
    @Transactional
    public PaymentCardDto updateStatus(Long id, boolean active) {
        PaymentCardEntity card = findCardOrThrow(id);
        if (active && !card.getActive()
                && paymentCardRepository.countByUserIdAndActiveTrue(card.getUser().getId()) >= MAX_CARDS_PER_USER) {
            throw new CardLimitExceededException(card.getUser().getId());
        }
        int updated = paymentCardRepository.updateActiveStatus(id, active, Instant.now());
        if (updated == 0) {
            throw new CardNotFoundException(id);
        }
        PaymentCardDto result = cardMapper.toDto(paymentCardRepository.findById(id)
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
