package com.innoboard.userservice.service;

import com.innoboard.userservice.config.RedisCacheConfig;
import com.innoboard.userservice.domain.PaymentCardEntity;
import com.innoboard.userservice.domain.UserEntity;
import com.innoboard.userservice.dto.PaymentCardDto;
import com.innoboard.userservice.exception.CardLimitExceededException;
import com.innoboard.userservice.exception.CardNotFoundException;
import com.innoboard.userservice.exception.CardNumberAlreadyExistsException;
import com.innoboard.userservice.exception.UserNotFoundException;
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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCardServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long CARD_ID = 10L;

    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache cache;
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

    private PaymentCardEntity ownedCard(boolean active) {
        PaymentCardEntity entity = card(active);
        entity.setUser(user(USER_ID));
        return entity;
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("throws when user is missing")
        void throwsWhenUserMissing() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.create(USER_ID, cardDto(true)))
                    .isInstanceOf(UserNotFoundException.class);
            verify(paymentCardRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when card number is already taken")
        void throwsWhenDuplicateNumber() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID)));
            when(paymentCardRepository.existsByNumber("4111111111111111")).thenReturn(true);

            assertThatThrownBy(() -> cardService.create(USER_ID, cardDto(true)))
                    .isInstanceOf(CardNumberAlreadyExistsException.class);
            verify(paymentCardRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when user already has 5 ACTIVE cards")
        void throwsWhenActiveLimitReached() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID)));
            when(paymentCardRepository.existsByNumber("4111111111111111")).thenReturn(false);
            when(paymentCardRepository.countByUserIdAndActiveTrue(USER_ID)).thenReturn(5L);

            assertThatThrownBy(() -> cardService.create(USER_ID, cardDto(true)))
                    .isInstanceOf(CardLimitExceededException.class);
            verify(paymentCardRepository, never()).save(any());
        }

        @Test
        @DisplayName("links card to user and persists it directly")
        void createsAndLinksCard() {
            UserEntity owner = user(USER_ID);
            PaymentCardEntity entity = card(false);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
            when(paymentCardRepository.existsByNumber("4111111111111111")).thenReturn(false);
            when(paymentCardRepository.countByUserIdAndActiveTrue(USER_ID)).thenReturn(0L);
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
    @DisplayName("getById (flat)")
    class GetById {

        @Test
        @DisplayName("returns mapped DTO when card exists")
        void returnsDto() {
            PaymentCardEntity entity = card(true);
            when(paymentCardRepository.findById(CARD_ID)).thenReturn(Optional.of(entity));
            when(cardMapper.toDto(entity)).thenReturn(cardDto(true));

            assertThat(cardService.getById(CARD_ID)).isEqualTo(cardDto(true));
        }

        @Test
        @DisplayName("throws when card is missing")
        void throwsWhenMissing() {
            when(paymentCardRepository.findById(CARD_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.getById(CARD_ID))
                    .isInstanceOf(CardNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("update (flat)")
    class Update {

        @Test
        @DisplayName("updates fields via mapper, saves and evicts owner cache")
        void updatesAndSaves() {
            PaymentCardEntity entity = ownedCard(true);
            PaymentCardDto request = cardDto(true);
            when(paymentCardRepository.findById(CARD_ID)).thenReturn(Optional.of(entity));
            when(paymentCardRepository.existsByNumberAndIdNot("4111111111111111", CARD_ID)).thenReturn(false);
            when(paymentCardRepository.save(entity)).thenReturn(entity);
            when(cardMapper.toDto(entity)).thenReturn(request);
            when(cacheManager.getCache(RedisCacheConfig.USERS_WITH_CARDS)).thenReturn(cache);

            cardService.update(CARD_ID, request);

            verify(cardMapper).updateEntityFromDto(request, entity);
            verify(paymentCardRepository).save(entity);
            verify(cache).evictIfPresent(USER_ID);
        }

        @Test
        @DisplayName("throws when number is taken by another card")
        void throwsWhenNumberTaken() {
            PaymentCardEntity entity = ownedCard(true);
            when(paymentCardRepository.findById(CARD_ID)).thenReturn(Optional.of(entity));
            when(paymentCardRepository.existsByNumberAndIdNot("4111111111111111", CARD_ID)).thenReturn(true);

            assertThatThrownBy(() -> cardService.update(CARD_ID, cardDto(true)))
                    .isInstanceOf(CardNumberAlreadyExistsException.class);
            verify(paymentCardRepository, never()).save(any());
            verify(cacheManager, never()).getCache(any());
        }

        @Test
        @DisplayName("throws when card is missing")
        void throwsWhenMissing() {
            when(paymentCardRepository.findById(CARD_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.update(CARD_ID, cardDto(true)))
                    .isInstanceOf(CardNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getAllByUserId")
    class GetAllByUserId {

        @Test
        @DisplayName("returns all cards of an existing user")
        void returnsCards() {
            when(userRepository.existsById(USER_ID)).thenReturn(true);
            when(paymentCardRepository.findByUserId(USER_ID)).thenReturn(List.of(card(true)));
            when(cardMapper.toDtoList(any())).thenReturn(List.of(cardDto(true)));

            assertThat(cardService.getAllByUserId(USER_ID)).hasSize(1);
            verify(paymentCardRepository).findByUserId(USER_ID);
        }

        @Test
        @DisplayName("throws when user does not exist (not an empty list)")
        void throwsWhenUserMissing() {
            when(userRepository.existsById(USER_ID)).thenReturn(false);

            assertThatThrownBy(() -> cardService.getAllByUserId(USER_ID))
                    .isInstanceOf(UserNotFoundException.class);
            verify(paymentCardRepository, never()).findByUserId(any());
        }
    }

    @Nested
    @DisplayName("updateStatus (flat, active-only limit)")
    class UpdateStatus {

        @Test
        @DisplayName("activates when there is a free slot and evicts owner cache")
        void activatesWhenSlotAvailable() {
            PaymentCardEntity inactive = ownedCard(false);
            PaymentCardEntity activated = ownedCard(true);
            when(paymentCardRepository.findById(CARD_ID))
                    .thenReturn(Optional.of(inactive), Optional.of(activated));
            when(paymentCardRepository.countByUserIdAndActiveTrue(USER_ID)).thenReturn(4L);
            when(paymentCardRepository.updateActiveStatus(eq(CARD_ID), eq(true), any())).thenReturn(1);
            when(cardMapper.toDto(activated)).thenReturn(cardDto(true));
            when(cacheManager.getCache(RedisCacheConfig.USERS_WITH_CARDS)).thenReturn(cache);

            cardService.updateStatus(CARD_ID, true);

            verify(paymentCardRepository).updateActiveStatus(eq(CARD_ID), eq(true), any());
            verify(cache).evictIfPresent(USER_ID);
        }

        @Test
        @DisplayName("throws when activating would exceed the active-card limit")
        void throwsWhenActiveLimitReached() {
            PaymentCardEntity inactive = ownedCard(false);
            when(paymentCardRepository.findById(CARD_ID)).thenReturn(Optional.of(inactive));
            when(paymentCardRepository.countByUserIdAndActiveTrue(USER_ID)).thenReturn(5L);

            assertThatThrownBy(() -> cardService.updateStatus(CARD_ID, true))
                    .isInstanceOf(CardLimitExceededException.class);
            verify(paymentCardRepository, never()).updateActiveStatus(any(), anyBoolean(), any());
        }

        @Test
        @DisplayName("deactivate does not check the limit")
        void deactivatesWithoutLimitCheck() {
            PaymentCardEntity active = ownedCard(true);
            when(paymentCardRepository.findById(CARD_ID))
                    .thenReturn(Optional.of(active), Optional.of(card(false)));
            when(paymentCardRepository.updateActiveStatus(eq(CARD_ID), eq(false), any())).thenReturn(1);
            when(cardMapper.toDto(any())).thenReturn(cardDto(false));
            when(cacheManager.getCache(RedisCacheConfig.USERS_WITH_CARDS)).thenReturn(cache);

            cardService.updateStatus(CARD_ID, false);

            verify(paymentCardRepository, never()).countByUserIdAndActiveTrue(any());
            verify(cache).evictIfPresent(USER_ID);
        }

        @Test
        @DisplayName("throws when card is missing")
        void throwsWhenMissing() {
            when(paymentCardRepository.findById(CARD_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.updateStatus(CARD_ID, true))
                    .isInstanceOf(CardNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("deletes the card and evicts the owner's cache entry")
        void deletesAndEvictsOwnerCache() {
            PaymentCardEntity entity = ownedCard(true);
            when(paymentCardRepository.findById(CARD_ID)).thenReturn(Optional.of(entity));
            when(cacheManager.getCache(RedisCacheConfig.USERS_WITH_CARDS)).thenReturn(cache);

            cardService.delete(CARD_ID);

            verify(paymentCardRepository).delete(entity);
            verify(cache).evictIfPresent(USER_ID);
        }

        @Test
        @DisplayName("throws when card is missing")
        void throwsWhenMissing() {
            when(paymentCardRepository.findById(CARD_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.delete(CARD_ID))
                    .isInstanceOf(CardNotFoundException.class);
            verify(cacheManager, never()).getCache(any());
        }
    }
}
