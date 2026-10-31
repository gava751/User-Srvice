package com.innoboard.userservice.controller;

import com.innoboard.userservice.dto.CreateUserRequest;
import com.innoboard.userservice.dto.UpdateStatusRequest;
import com.innoboard.userservice.dto.UpdateUserRequest;
import com.innoboard.userservice.dto.UserResponse;
import com.innoboard.userservice.dto.UserWithCardsResponse;
import com.innoboard.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    @GetMapping("/{id}")
    ResponseEntity<UserWithCardsResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @GetMapping
    ResponseEntity<Page<UserResponse>> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String surname,
            @PageableDefault(size = 10, sort = "id") Pageable pageable
    ) {
        return ResponseEntity.ok(userService.getAll(name, surname, pageable));
    }

    @PutMapping("/{id}")
    ResponseEntity<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    ResponseEntity<UserResponse> updateStatus(@PathVariable Long id,
                                              @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(userService.updateStatus(id, request.active()));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
