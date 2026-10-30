package com.innoboard.userservice.service;

import com.innoboard.userservice.domain.UserEntity;
import com.innoboard.userservice.dto.CardResponse;
import com.innoboard.userservice.dto.CreateUserRequest;
import com.innoboard.userservice.dto.UpdateUserRequest;
import com.innoboard.userservice.dto.UserResponse;
import com.innoboard.userservice.dto.UserWithCardsResponse;
import com.innoboard.userservice.exception.EmailAlreadyExistsException;
import com.innoboard.userservice.exception.UserNotFoundException;
import com.innoboard.userservice.mapper.PaymentCardMapper;
import com.innoboard.userservice.mapper.UserMapper;
import com.innoboard.userservice.repository.PaymentCardRepository;
import com.innoboard.userservice.repository.UserRepository;
import com.innoboard.userservice.service.impl.UserServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final String EMAIL = "ivan@example.com";

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PaymentCardRepository paymentCardRepository;
    @Mock
    private PaymentCardMapper cardMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private CreateUserRequest createRequest() {
        return new CreateUserRequest("Ivan", "Ivanov", LocalDate.of(1990, 1, 1), EMAIL);
    }

    private UpdateUserRequest updateRequest() {
        return new UpdateUserRequest("Petr", "Ivanov", LocalDate.of(1990, 1, 1), "petr@example.com");
    }

    private UserResponse response(Long id, boolean active) {
        return new UserResponse(id, "Ivan", "Ivanov", LocalDate.of(1990, 1, 1), EMAIL, active);
    }

    private UserEntity entity(Long id, boolean active) {
        return UserEntity.builder()
                .id(id)
                .name("Ivan")
                .surname("Ivanov")
                .birthDate(LocalDate.of(1990, 1, 1))
                .email(EMAIL)
                .active(active)
                .build();
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("saves entity and returns mapped response")
        void savesAndReturnsResponse() {
            CreateUserRequest request = createRequest();
            UserEntity entity = entity(null, true);
            UserResponse expected = response(1L, true);

            when(userRepository.existsByEmailNormalized(EMAIL)).thenReturn(false);
            when(userMapper.toEntity(request)).thenReturn(entity);
            when(userRepository.save(entity)).thenReturn(entity);
            when(userMapper.toResponse(entity)).thenReturn(expected);

            UserResponse result = userService.create(request);

            assertThat(result).isEqualTo(expected);
            verify(userRepository).save(entity);
        }

        @Test
        @DisplayName("forces active = true regardless of request")
        void forcesActiveTrue() {
            UserEntity entity = entity(null, false);

            when(userRepository.existsByEmailNormalized(EMAIL)).thenReturn(false);
            when(userMapper.toEntity(createRequest())).thenReturn(entity);
            when(userRepository.save(entity)).thenReturn(entity);
            when(userMapper.toResponse(entity)).thenReturn(response(1L, true));

            userService.create(createRequest());

            ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getActive()).isTrue();
        }

        @Test
        @DisplayName("throws when email is already taken")
        void throwsWhenEmailExists() {
            when(userRepository.existsByEmailNormalized(EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> userService.create(createRequest()))
                    .isInstanceOf(EmailAlreadyExistsException.class);
            verify(userRepository, never()).save(any());
            verifyNoInteractions(userMapper);
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("returns cached aggregate: user with cards")
        void returnsAggregate() {
            UserEntity user = entity(1L, true);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(cardMapper.toResponseList(any())).thenReturn(List.of(
                    new CardResponse(10L, 1L, "4111111111111111", "IVAN IVANOV",
                            LocalDate.of(2030, 12, 31), true)));
            when(userMapper.toResponse(user)).thenReturn(response(1L, true));

            UserWithCardsResponse result = userService.getById(1L);

            assertThat(result.user()).isEqualTo(response(1L, true));
            assertThat(result.cards()).hasSize(1);
        }

        @Test
        @DisplayName("throws UserNotFoundException when user is missing")
        void throwsWhenMissing() {
            when(userRepository.findById(42L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getById(42L))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getAll")
    class GetAll {

        @Test
        @DisplayName("delegates to repository with specification and pageable, maps result")
        void delegatesAndMaps() {
            Pageable pageable = PageRequest.of(0, 10);
            when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(entity(1L, true))));

            var result = userService.getAll("Ivan", "Ivanov", pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(userRepository).findAll(any(Specification.class), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("updates existing entity via mapper and saves it")
        void updatesAndSaves() {
            UserEntity entity = entity(1L, true);
            UpdateUserRequest request = updateRequest();

            when(userRepository.existsByEmailNormalizedAndIdNot("petr@example.com", 1L)).thenReturn(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(entity));
            when(userRepository.save(entity)).thenReturn(entity);
            when(userMapper.toResponse(entity)).thenReturn(response(1L, true));

            userService.update(1L, request);

            verify(userMapper).updateEntityFromDto(request, entity);
            verify(userRepository).save(entity);
        }

        @Test
        @DisplayName("throws when email is taken by another user")
        void throwsWhenEmailTakenByOther() {
            when(userRepository.existsByEmailNormalizedAndIdNot("petr@example.com", 1L)).thenReturn(true);

            assertThatThrownBy(() -> userService.update(1L, updateRequest()))
                    .isInstanceOf(EmailAlreadyExistsException.class);
            verify(userRepository, never()).save(any());
            verifyNoInteractions(userMapper);
        }

        @Test
        @DisplayName("throws when user is missing")
        void throwsWhenMissing() {
            when(userRepository.existsByEmailNormalizedAndIdNot("petr@example.com", 42L)).thenReturn(false);
            when(userRepository.findById(42L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.update(42L, updateRequest()))
                    .isInstanceOf(UserNotFoundException.class);
            verify(userMapper, never()).updateEntityFromDto(any(), any());
        }
    }

    @Nested
    @DisplayName("updateStatus (@Modifying)")
    class UpdateStatus {

        @Test
        @DisplayName("updates status via bulk query and returns fresh response")
        void updatesStatus() {
            when(userRepository.updateActiveStatus(eq(1L), eq(true), any())).thenReturn(1);
            when(userRepository.findById(1L)).thenReturn(Optional.of(entity(1L, true)));
            when(userMapper.toResponse(any())).thenReturn(response(1L, true));

            userService.updateStatus(1L, true);

            verify(userRepository).updateActiveStatus(eq(1L), eq(true), any());
        }

        @Test
        @DisplayName("throws when bulk update affected 0 rows (user missing)")
        void throwsWhenNoRowsUpdated() {
            when(userRepository.updateActiveStatus(eq(42L), eq(true), any())).thenReturn(0);

            assertThatThrownBy(() -> userService.updateStatus(42L, true))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("deletes the user (cards go via cascade)")
        void deletesUser() {
            UserEntity entity = entity(1L, true);
            when(userRepository.findById(1L)).thenReturn(Optional.of(entity));

            userService.delete(1L);

            verify(userRepository).delete(entity);
        }

        @Test
        @DisplayName("throws when user is missing")
        void throwsWhenMissing() {
            when(userRepository.findById(42L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.delete(42L))
                    .isInstanceOf(UserNotFoundException.class);
            verify(userRepository, never()).delete(any(UserEntity.class));
        }
    }
}
