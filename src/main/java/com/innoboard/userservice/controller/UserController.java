package com.innoboard.userservice.controller;

import com.innoboard.userservice.dto.UserDto;
import com.innoboard.userservice.dto.UserWithCardsDto;
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
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    ResponseEntity<UserDto> create(@Valid @RequestBody UserDto userDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(userDto));
    }

    @GetMapping("/{id}")
    ResponseEntity<UserDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @GetMapping
    ResponseEntity<Page<UserDto>> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String surname,
            @PageableDefault(size=10,sort = "id") Pageable pageable
    ) {
        return ResponseEntity.ok(userService.getAll(name, surname, pageable));
    }

    @PutMapping("/{id}")
    ResponseEntity<UserDto> update(@PathVariable Long id, @Valid @RequestBody UserDto userDto) {
        return ResponseEntity.ok(userService.update(id, userDto));
    }

    @PatchMapping("/{id}/activate")
    ResponseEntity<UserDto> activate(@PathVariable Long id) {
        return ResponseEntity.ok(userService.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    ResponseEntity<UserDto> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(userService.deactivate(id));
    }

    @GetMapping("/{id}/full")
    ResponseEntity<UserWithCardsDto> getByIdWithCards(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getByIdWithCards(id));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
