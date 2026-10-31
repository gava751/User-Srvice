package com.innoboard.userservice.exception;

public class CardNotFoundException extends RuntimeException {
    public CardNotFoundException(Long id) {
        super(String.format("Card not found with id %d", id));
    }
}
