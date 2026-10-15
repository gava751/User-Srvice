package com.innoboard.userservice.service;

import com.innoboard.userservice.domain.UserEntity;
import com.innoboard.userservice.dto.UserDto;
import com.innoboard.userservice.exception.UserPrincipalNotFoundException;
import com.innoboard.userservice.mapper.UserMapper;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private UserDto dto(Long id, boolean active) {
        return new UserDto(id,"Ivan","Ivanov", LocalDate.of(1990,1,1),
                "ivan@example.com",active);
    }

    private UserEntity entity(Long id, boolean active) {
        return UserEntity.builder()
                .id(id)
                .name("Ivan")
                .surname("Ivanov")
                .birthDate(LocalDate.of(1990,1,1))
                .email("ivan@example.com")
                .active(active)
                .build();
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("saves entity and returns mapped DTO")
        void saveAndReturnsDto(){
            UserDto request = dto(null,false);
            UserEntity entity = entity(null,true);
            UserDto response =dto(1L,true);

            when(userMapper.toEntity(request)).thenReturn(entity);
            when(userRepository.save(entity)).thenReturn(entity);
            when(userMapper.toDto(entity)).thenReturn(response);

            UserDto result=userService.create(request);

            assertThat(result).isEqualTo(response);
            verify(userRepository).save(entity);
        }

        @Test
        @DisplayName("forces active = true regardless of request")
        void forcesActiveTrue(){
            UserDto request = dto(null,false);
            UserEntity entity = entity(null,false);

            when(userMapper.toEntity(request)).thenReturn(entity);
            when(userRepository.save(entity)).thenReturn(entity);
            when(userMapper.toDto(entity)).thenReturn(dto(1L,true));

            userService.create(request);

            ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("getAll")
    class GetAll {

        @Test
        @DisplayName("delegates to repository with specification and pageable, maps result")
        void delegatesAndMaps(){
            Pageable pageable = PageRequest.of(0,10);
            when(userRepository.findAll(any(Specification.class),any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(entity(1L,true))));

            var result = userService.getAll("Ivan","Ivanov",pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(userRepository).findAll(any(Specification.class),any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("updates existing entity via mapper and saves it")
        void updatesAndSaves() {
            UserEntity entity = entity(1L,true);
            UserDto request = dto(1L,true);

            when(userRepository.findById(1L)).thenReturn(Optional.of(entity));
            when(userRepository.save(entity)).thenReturn(entity);
            when(userMapper.toDto(entity)).thenReturn(request);

            userService.update(1L,request);

            verify(userMapper).updateEntityFromDto(request,entity);
            verify(userRepository).save(entity);
        }

        @Test
        @DisplayName("throws when user is missing")
        void throwsWhenMissing() {
            when(userRepository.findById(42L)).thenReturn(Optional.empty());

            assertThatThrownBy(()->userService.update(42L,dto(42L,true)))
                    .isInstanceOf(UserPrincipalNotFoundException.class);
            verify(userMapper, org.mockito.Mockito.never()).updateEntityFromDto(any(),any());
        }
    }

    @Nested
    @DisplayName("activate / deactivate")
    class Activation {

        @Test
        @DisplayName("activate sets active = true")
        void activateSetsTrue() {
            UserEntity entity = entity(1L, false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(entity));
            when(userRepository.save(entity)).thenReturn(entity);
            when(userMapper.toDto(entity)).thenReturn(dto(1L, true));

            userService.activate(1L);

            ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getActive()).isTrue();
        }

        @Test
        @DisplayName("deactivate sets active = false")
        void deactivateSetsFalse() {
            UserEntity entity = entity(1L, true);
            when(userRepository.findById(1L)).thenReturn(Optional.of(entity));
            when(userRepository.save(entity)).thenReturn(entity);
            when(userMapper.toDto(entity)).thenReturn(dto(1L, false));

            userService.deactivate(1L);

            ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getActive()).isFalse();
        }

        @Test
        @DisplayName("throws when user is missing")
        void throwsWhenMissing() {
            when(userRepository.findById(42L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.activate(42L))
                    .isInstanceOf(UserPrincipalNotFoundException.class);
            verifyNoInteractions(userMapper);
        }
    }
}
