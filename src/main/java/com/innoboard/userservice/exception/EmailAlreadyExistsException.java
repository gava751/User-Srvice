package com.innoboard.userservice.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("User with email=%s already exists".formatted(email));
    }
}
