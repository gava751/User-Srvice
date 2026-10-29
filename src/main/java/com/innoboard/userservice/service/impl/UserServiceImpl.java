package com.innoboard.userservice.service.impl;

import com.innoboard.userservice.config.RedisCacheConfig;
import com.innoboard.userservice.domain.UserEntity;
import com.innoboard.userservice.dto.PaymentCardDto;
import com.innoboard.userservice.dto.UserDto;
import com.innoboard.userservice.dto.UserWithCardsDto;
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
    public UserDto create(UserDto dto) {
        if (userRepository.existsByEmailIgnoreCase(dto.email())) {
            throw new EmailAlreadyExistsException(dto.email());
        }
        UserEntity entity = userMapper.toEntity(dto);
        entity.setActive(true);   // состояние решает сервис, не клиент
        return userMapper.toDto(userRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = RedisCacheConfig.USERS_WITH_CARDS, key = "#id")
    public UserWithCardsDto getById(Long id) {
        UserEntity user = findUserOrThrow(id);
        List<PaymentCardDto> cards = cardMapper.toDtoList(paymentCardRepository.findByUserId(id));
        return new UserWithCardsDto(userMapper.toDto(user), cards);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> getAll(String name, String surname, Pageable pageable) {
        Specification<UserEntity> spec = UserSpecifications.hasName(name)
                .and(UserSpecifications.hasSurname(surname));
        return userRepository.findAll(spec, pageable).map(userMapper::toDto);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = RedisCacheConfig.USERS_WITH_CARDS, key = "#id")
    public UserDto update(Long id, UserDto dto) {
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(dto.email(), id)) {
            throw new EmailAlreadyExistsException(dto.email());
        }
        UserEntity entity = findUserOrThrow(id);
        userMapper.updateEntityFromDto(dto, entity);
        return userMapper.toDto(userRepository.save(entity));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = RedisCacheConfig.USERS_WITH_CARDS, key = "#id")
    public UserDto updateStatus(Long id, boolean active) {
        int updated = userRepository.updateActiveStatus(id, active, Instant.now());
        if (updated == 0) {
            throw new UserNotFoundException(id);
        }
        return userMapper.toDto(userRepository.findById(id).orElseThrow());
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
