package com.innoboard.userservice.service.impl;

import com.innoboard.userservice.config.RedisCacheConfig;
import com.innoboard.userservice.domain.UserEntity;
import com.innoboard.userservice.dto.CardResponse;
import com.innoboard.userservice.dto.CreateUserRequest;
import com.innoboard.userservice.dto.UpdateUserRequest;
import com.innoboard.userservice.dto.UserResponse;
import com.innoboard.userservice.dto.UserWithCardsResponse;
import com.innoboard.userservice.exception.EmailAlreadyExistsException;
import com.innoboard.userservice.exception.UserNotFoundException;
import com.innoboard.userservice.mapper.PaymentCardMapper;
import com.innoboard.userservice.mapper.UserMapper;
import com.innoboard.userservice.repository.PaymentCardRepository;
import com.innoboard.userservice.repository.UserRepository;
import com.innoboard.userservice.repository.specification.UserSpecifications;
import com.innoboard.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PaymentCardRepository paymentCardRepository;
    private final PaymentCardMapper cardMapper;

    @Override
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByEmailNormalized(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }
        UserEntity entity = userMapper.toEntity(request);
        entity.setActive(true);
        return userMapper.toResponse(userRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = RedisCacheConfig.USERS_WITH_CARDS, key = "#id")
    public UserWithCardsResponse getById(Long id) {
        UserEntity user = findUserOrThrow(id);
        List<CardResponse> cards = cardMapper.toResponseList(paymentCardRepository.findByUserId(id));
        return new UserWithCardsResponse(userMapper.toResponse(user), cards);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAll(String name, String surname, Pageable pageable) {
        Specification<UserEntity> spec = UserSpecifications.hasName(name)
                .and(UserSpecifications.hasSurname(surname));
        return userRepository.findAll(spec, pageable).map(userMapper::toResponse);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = RedisCacheConfig.USERS_WITH_CARDS, key = "#id")
    public UserResponse update(Long id, UpdateUserRequest request) {
        if (userRepository.existsByEmailNormalizedAndIdNot(request.email(), id)) {
            throw new EmailAlreadyExistsException(request.email());
        }
        UserEntity entity = findUserOrThrow(id);
        userMapper.updateEntityFromDto(request, entity);
        return userMapper.toResponse(userRepository.save(entity));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = RedisCacheConfig.USERS_WITH_CARDS, key = "#id")
    public UserResponse updateStatus(Long id, boolean active) {
        int updated = userRepository.updateActiveStatus(id, active, Instant.now());
        if (updated == 0) {
            throw new UserNotFoundException(id);
        }

        return userMapper.toResponse(userRepository.findById(id).orElseThrow());
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = RedisCacheConfig.USERS_WITH_CARDS, key = "#id")
    public void delete(Long id) {
        UserEntity user = findUserOrThrow(id);
        userRepository.delete(user);
    }

    private UserEntity findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}
