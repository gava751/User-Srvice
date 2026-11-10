package com.innoboard.userservice.security;

import com.innoboard.userservice.repository.PaymentCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component("cardAccess")
@RequiredArgsConstructor
public class CardAccessPolicy {

    private static final String USER_ID_CLAIM = "user_id";

    private final PaymentCardRepository paymentCardRepository;

    public boolean isOwner(Long cardId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof Jwt jwt)
                || !(jwt.getClaim(USER_ID_CLAIM) instanceof Number number)) {
            return false;
        }
        return paymentCardRepository.findById(cardId)
                .map(card -> number.longValue() == card.getUser().getId())
                .orElse(false);
    }
}
