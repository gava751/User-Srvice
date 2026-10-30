package com.innoboard.userservice.service;

import com.innoboard.userservice.dto.CreateUserRequest;
import com.innoboard.userservice.dto.UpdateUserRequest;
import com.innoboard.userservice.dto.UserResponse;
import com.innoboard.userservice.dto.UserWithCardsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponse create(CreateUserRequest request);

    UserWithCardsResponse getById(Long id);

    Page<UserResponse> getAll(String name, String surname, Pageable pageable);

    UserResponse update(Long id, UpdateUserRequest request);

    UserResponse updateStatus(Long id, boolean active);

    void delete(Long id);
}
