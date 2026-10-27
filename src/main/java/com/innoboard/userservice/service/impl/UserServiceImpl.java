package com.innoboard.userservice.service.impl;

import com.innoboard.userservice.config.RedisCacheConfig;
import com.innoboard.userservice.domain.UserEntity;
import com.innoboard.userservice.dto.PaymentCardDto;
import com.innoboard.userservice.dto.UserDto;
import com.innoboard.userservice.dto.UserWithCardsDto;
import com.innoboard.userservice.exception.UserPrincipalNotFoundException;
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
        UserEntity entity=userMapper.toEntity(dto);
        entity.setActive(true);
        return userMapper.toDto(userRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getById(Long id) {
        return userMapper.toDto(findUserOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> getAll (String name, String surname, Pageable pageable) {
        Specification<UserEntity> spec= UserSpecifications.hasName(name)
                .and(UserSpecifications.hasSurname(surname));
        return userRepository.findAll(spec, pageable).map(userMapper::toDto);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = RedisCacheConfig.USERS_WITH_CARDS, key = "#id")
    public UserDto update(Long id, UserDto dto) {
        UserEntity entity = findUserOrThrow(id);
        userMapper.updateEntityFromDto(dto, entity);
        return userMapper.toDto(userRepository.save(entity));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = RedisCacheConfig.USERS_WITH_CARDS, key = "#id")
    public UserDto activate(Long id) {
        return setActive(id, true);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = RedisCacheConfig.USERS_WITH_CARDS, key = "#id")
    public UserDto deactivate(Long id) {
        return setActive(id, false);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = RedisCacheConfig.USERS_WITH_CARDS, key = "#id", unless = "#result == null")
    public UserWithCardsDto getByIdWithCards(Long id) {
        UserEntity user = findUserOrThrow(id);
        List<PaymentCardDto> cards = cardMapper.toDtoList(paymentCardRepository.findByUserId(id));
        return new UserWithCardsDto(userMapper.toDto(user), cards);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = RedisCacheConfig.USERS_WITH_CARDS, key = "#id")
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserPrincipalNotFoundException(id);
        }
        paymentCardRepository.deleteByUserId(id);
        userRepository.deleteById(id);
    }

    private UserDto setActive(Long id, boolean active) {
        UserEntity entity = findUserOrThrow(id);
        entity.setActive(active);
        return userMapper.toDto(userRepository.save(entity));
    }

    private UserEntity findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(()->new UserPrincipalNotFoundException(id));
    }
}
