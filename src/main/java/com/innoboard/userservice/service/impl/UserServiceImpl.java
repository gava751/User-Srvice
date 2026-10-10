package com.innoboard.userservice.service.impl;

import com.innoboard.userservice.domain.UserEntity;
import com.innoboard.userservice.dto.UserDto;
import com.innoboard.userservice.exception.UserPrincipalNotFoundException;
import com.innoboard.userservice.mapper.UserMapper;
import com.innoboard.userservice.repository.UserRepository;
import com.innoboard.userservice.repository.specification.UserSpecifications;
import com.innoboard.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

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
    public UserDto update(Long id, UserDto dto) {
        UserEntity entity = findUserOrThrow(id);
        userMapper.updateEntityFromDto(dto, entity);
        return userMapper.toDto(userRepository.save(entity));
    }

    @Override
    @Transactional
    public void activate(Long id) {
        setActive(id, true);
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        setActive(id, false);
    }

    private void setActive(Long id, Boolean active) {
        UserEntity entity = findUserOrThrow(id);
        entity.setActive(active);
        userRepository.save(entity);
    }

    private UserEntity findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(()->new UserPrincipalNotFoundException(id));
    }
}
