package com.innoboard.userservice.exception;

public class CardNumberAlreadyExistsException extends RuntimeException {

    public CardNumberAlreadyExistsException(String number) {
        super("Card with number=%s already exists".formatted(number));
    }
}
