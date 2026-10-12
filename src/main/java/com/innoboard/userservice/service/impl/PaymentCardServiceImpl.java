package com.innoboard.userservice.service.impl;

import com.innoboard.userservice.config.RedisCacheConfig;
import com.innoboard.userservice.domain.PaymentCardEntity;
import com.innoboard.userservice.domain.UserEntity;
import com.innoboard.userservice.dto.PaymentCardDto;
import com.innoboard.userservice.exception.CardNotFoundException;
import com.innoboard.userservice.exception.TooManyCardsException;
import com.innoboard.userservice.exception.UserPrincipalNotFoundException;
import com.innoboard.userservice.mapper.PaymentCardMapper;
import com.innoboard.userservice.repository.PaymentCardRepository;
import com.innoboard.userservice.repository.UserRepository;
import com.innoboard.userservice.service.PaymentCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentCardServiceImpl implements PaymentCardService {

    private final static int MAX_CARD_PER_USER=5;

    private final PaymentCardRepository paymentCardRepository;
    private final UserRepository userRepository;
    private final PaymentCardMapper cardMapper;

    @Override
    @Transactional
    @CacheEvict(cacheNames = RedisCacheConfig.USERS_WITH_CARDS, key = "#userId")
    public PaymentCardDto create(Long userId, PaymentCardDto dto) {
        UserEntity user= userRepository.findById(userId)
                .orElseThrow(()->new UserPrincipalNotFoundException(userId));

        if (paymentCardRepository.countByUserId(userId)>=MAX_CARD_PER_USER) {
            throw new TooManyCardsException(userId);
        }

        PaymentCardEntity card = cardMapper.toEntity(dto);
        card.setActive(true);
        user.addCard(card);
        userRepository.save(user);

        return cardMapper.toDto(card);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentCardDto getById(Long userId, Long id) {
        PaymentCardEntity card = findCardOrThrow(userId, id);
        return cardMapper.toDto(card);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentCardDto> getAllByUserId(Long userId) {
        return cardMapper.toDtoList(paymentCardRepository.findByUserId(userId));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = RedisCacheConfig.USERS_WITH_CARDS, key = "#userId")
    public PaymentCardDto update(Long userId, Long id, PaymentCardDto dto) {
        PaymentCardEntity card = findCardOrThrow(userId, id);
        cardMapper.updateEntityFromDto(dto, card);
        return cardMapper.toDto(paymentCardRepository.save(card));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = RedisCacheConfig.USERS_WITH_CARDS, key = "#userId")
    public PaymentCardDto activate(Long userId, Long id) {
        return setActive(userId, id, true);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = RedisCacheConfig.USERS_WITH_CARDS, key = "#userId")
    public PaymentCardDto deactivate(Long userId, Long id) {
        return setActive(userId, id, true);
    }

    private PaymentCardDto setActive(Long userId, Long id, boolean active) {
        PaymentCardEntity card = findCardOrThrow(userId, id);
        card.setActive(active);
        return cardMapper.toDto(paymentCardRepository.save(card));
    }

    private PaymentCardEntity findCardOrThrow(Long userId, Long id) {
        if (!paymentCardRepository.existsByIdAndUserId(id, userId)) {
            throw new CardNotFoundException(id);
        }
        return paymentCardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException(id));
    }
}
