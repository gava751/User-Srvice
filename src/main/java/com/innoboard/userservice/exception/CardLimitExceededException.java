package com.innoboard.userservice.exception;

public class CardLimitExceededException extends RuntimeException {
    public CardLimitExceededException(Long userId) {
        super("Too many cards for user " + userId);
    }
}
