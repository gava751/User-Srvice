package com.innoboard.userservice.exception;

public class TooManyCardsException extends RuntimeException {
    public TooManyCardsException(Long userId) {
        super("Too many cards for user " + userId);
    }
}
