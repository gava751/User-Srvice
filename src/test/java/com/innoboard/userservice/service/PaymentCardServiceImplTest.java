package com.innoboard.userservice.service;

import com.innoboard.userservice.domain.PaymentCardEntity;
import com.innoboard.userservice.domain.UserEntity;
import com.innoboard.userservice.dto.PaymentCardDto;
import com.innoboard.userservice.exception.CardNotFoundException;
import com.innoboard.userservice.exception.TooManyCardsException;
import com.innoboard.userservice.exception.UserPrincipalNotFoundException;
import com.innoboard.userservice.mapper.PaymentCardMapper;
import com.innoboard.userservice.repository.PaymentCardRepository;
import com.innoboard.userservice.repository.UserRepository;
import com.innoboard.userservice.service.impl.PaymentCardServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCardServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long CARD_ID = 10L;

    @Mock
    private PaymentCardRepository paymentCardRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PaymentCardMapper cardMapper;

    @InjectMocks
    private PaymentCardServiceImpl cardService;

    private UserEntity user(Long id) {
        return UserEntity.builder().id(id).name("Ivan").surname("Ivanov")
                .email("ivan@example.com").active(true).build();
    }

    private PaymentCardDto cardDto(boolean active) {
        return new PaymentCardDto(null, USER_ID, "4111111111111111",
                "IVAN IVANOV", LocalDate.of(2030, 12, 31), active);
    }

    private PaymentCardEntity card(boolean active) {
        return PaymentCardEntity.builder().id(CARD_ID).number("4111111111111111")
                .holder("IVAN IVANOV").expirationDate(LocalDate.of(2030, 12, 31))
                .active(active).build();
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("throws when user is missing")
        void throwsWhenUserMissing() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.create(USER_ID, cardDto(true)))
                    .isInstanceOf(UserPrincipalNotFoundException.class);
            verifyNoInteractions(paymentCardRepository, cardMapper);
        }

        @Test
        @DisplayName("throws TooManyCardsException when user already has 5 cards")
        void throwsWhenLimitReached() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID)));
            when(paymentCardRepository.countByUserId(USER_ID)).thenReturn(5L);

            assertThatThrownBy(() -> cardService.create(USER_ID, cardDto(true)))
                    .isInstanceOf(TooManyCardsException.class);
            verify(paymentCardRepository, never()).save(any());
        }

        @Test
        @DisplayName("links card to user and persists it directly (merge-cascade copy trap)")
        void createsAndLinksCard() {
            UserEntity owner = user(USER_ID);
            PaymentCardEntity entity = card(false);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
            when(paymentCardRepository.countByUserId(USER_ID)).thenReturn(0L);
            when(cardMapper.toEntity(cardDto(true))).thenReturn(entity);
            when(paymentCardRepository.save(entity)).thenReturn(entity);
            when(cardMapper.toDto(entity)).thenReturn(cardDto(true));

            cardService.create(USER_ID, cardDto(true));

            ArgumentCaptor<PaymentCardEntity> captor = ArgumentCaptor.forClass(PaymentCardEntity.class);
            verify(paymentCardRepository).save(captor.capture());
            assertThat(captor.getValue().getUser()).isEqualTo(owner);
            assertThat(owner.getPaymentCards()).contains(entity);
            assertThat(entity.getActive()).isTrue();
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("throws when card does not belong to the user")
        void throwsWhenNotOwned() {
            when(paymentCardRepository.existsByIdAndUserId(CARD_ID, USER_ID)).thenReturn(false);

            assertThatThrownBy(() -> cardService.getById(USER_ID, CARD_ID))
                    .isInstanceOf(CardNotFoundException.class);
            verify(paymentCardRepository, never()).findById(any());
        }

        @Test
        @DisplayName("returns mapped DTO when card belongs to the user")
        void returnsDto() {
            PaymentCardEntity entity = card(true);
            when(paymentCardRepository.existsByIdAndUserId(CARD_ID, USER_ID)).thenReturn(true);
            when(paymentCardRepository.findById(CARD_ID)).thenReturn(Optional.of(entity));
            when(cardMapper.toDto(entity)).thenReturn(cardDto(true));

            assertThat(cardService.getById(USER_ID, CARD_ID)).isEqualTo(cardDto(true));
        }
    }

    @Nested
    @DisplayName("getAllByUserId")
    class GetAllByUserId {

        @Test
        @DisplayName("returns all cards of the user")
        void returnsCards() {
            when(paymentCardRepository.findByUserId(USER_ID)).thenReturn(List.of(card(true)));
            when(cardMapper.toDtoList(any())).thenReturn(List.of(cardDto(true)));

            assertThat(cardService.getAllByUserId(USER_ID)).hasSize(1);
            verify(paymentCardRepository).findByUserId(USER_ID);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("throws when card is not owned by the user")
        void throwsWhenNotOwned() {
            when(paymentCardRepository.existsByIdAndUserId(CARD_ID, USER_ID)).thenReturn(false);

            assertThatThrownBy(() -> cardService.update(USER_ID, CARD_ID, cardDto(true)))
                    .isInstanceOf(CardNotFoundException.class);
        }

        @Test
        @DisplayName("updates fields via mapper and saves")
        void updatesAndSaves() {
            PaymentCardEntity entity = card(true);
            PaymentCardDto request = cardDto(true);
            when(paymentCardRepository.existsByIdAndUserId(CARD_ID, USER_ID)).thenReturn(true);
            when(paymentCardRepository.findById(CARD_ID)).thenReturn(Optional.of(entity));
            when(paymentCardRepository.save(entity)).thenReturn(entity);
            when(cardMapper.toDto(entity)).thenReturn(request);

            cardService.update(USER_ID, CARD_ID, request);

            verify(cardMapper).updateEntityFromDto(request, entity);
            verify(paymentCardRepository).save(entity);
        }
    }

    @Nested
    @DisplayName("activate / deactivate")
    class Activation {

        @Test
        @DisplayName("activate sets active = true")
        void activateSetsTrue() {
            PaymentCardEntity entity = card(false);
            when(paymentCardRepository.existsByIdAndUserId(CARD_ID, USER_ID)).thenReturn(true);
            when(paymentCardRepository.findById(CARD_ID)).thenReturn(Optional.of(entity));
            when(paymentCardRepository.save(entity)).thenReturn(entity);
            when(cardMapper.toDto(entity)).thenReturn(cardDto(true));

            cardService.activate(USER_ID, CARD_ID);

            ArgumentCaptor<PaymentCardEntity> captor = ArgumentCaptor.forClass(PaymentCardEntity.class);
            verify(paymentCardRepository).save(captor.capture());
            assertThat(captor.getValue().getActive()).isTrue();
        }

        @Test
        @DisplayName("deactivate sets active = false")
        void deactivateSetsFalse() {
            PaymentCardEntity entity = card(true);
            when(paymentCardRepository.existsByIdAndUserId(CARD_ID, USER_ID)).thenReturn(true);
            when(paymentCardRepository.findById(CARD_ID)).thenReturn(Optional.of(entity));
            when(paymentCardRepository.save(entity)).thenReturn(entity);
            when(cardMapper.toDto(entity)).thenReturn(cardDto(false));

            cardService.deactivate(USER_ID, CARD_ID);

            ArgumentCaptor<PaymentCardEntity> captor = ArgumentCaptor.forClass(PaymentCardEntity.class);
            verify(paymentCardRepository).save(captor.capture());
            assertThat(captor.getValue().getActive()).isFalse();
        }

        @Test
        @DisplayName("throws when card is not owned by the user")
        void throwsWhenNotOwned() {
            when(paymentCardRepository.existsByIdAndUserId(CARD_ID, USER_ID)).thenReturn(false);

            assertThatThrownBy(() -> cardService.deactivate(USER_ID, CARD_ID))
                    .isInstanceOf(CardNotFoundException.class);
        }
    }
}
