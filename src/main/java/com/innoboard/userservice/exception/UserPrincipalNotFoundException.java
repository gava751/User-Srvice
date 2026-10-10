package com.innoboard.userservice.exception;

public class UserPrincipalNotFoundException extends RuntimeException {

    public UserPrincipalNotFoundException(Long id) {
        super("User with id=%d not found".formatted(id));
    }
}
