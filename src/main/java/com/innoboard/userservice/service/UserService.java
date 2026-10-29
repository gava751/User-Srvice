package com.innoboard.userservice.service;

import com.innoboard.userservice.dto.UserDto;
import com.innoboard.userservice.dto.UserWithCardsDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserDto create(UserDto dto);

    UserWithCardsDto getById(Long id);

    Page<UserDto> getAll(String name, String surname, Pageable pageable);

    UserDto update(Long id, UserDto dto);

    UserDto updateStatus(Long id, boolean active);

    void delete(Long id);
}
