package com.innoboard.userservice.integration;

import tools.jackson.databind.ObjectMapper;
import com.innoboard.userservice.dto.PaymentCardDto;
import com.innoboard.userservice.dto.UserDto;
import com.innoboard.userservice.repository.PaymentCardRepository;
import com.innoboard.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class UserFlowIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    @Container
    @ServiceConnection
    static GenericContainer<?> redis = new GenericContainer<>("redis:7").withExposedPorts(6379);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentCardRepository paymentCardRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void cleanState() {
        paymentCardRepository.deleteAll();
        userRepository.deleteAll();
        redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });
    }

    private String userJson(String email) throws Exception {
        UserDto dto = new UserDto(null, "Ivan", "Ivanov",
                LocalDate.of(1990, 1, 1), email, true);
        return objectMapper.writeValueAsString(dto);
    }

    @Test
    @DisplayName("full lifecycle: create -> get -> update -> deactivate")
    void fullUserLifecycle() throws Exception {
        String created = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("ivan@example.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ivan"))
                .andExpect(jsonPath("$.email").value("ivan@example.com"));

        mockMvc.perform(put("/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("petr@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("petr@example.com"));

        mockMvc.perform(patch("/users/{id}/deactivate", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/users")
                        .param("surname", "Ivanov").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id));

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("invalid email -> 400 with field error from validation")
    void invalidEmailReturns400WithFieldError() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("not-an-email")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    @DisplayName("missing user -> 404 with ErrorResponse")
    void missingUserReturns404() throws Exception {
        mockMvc.perform(get("/users/{id}", 9999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("sixth card -> 409 TooManyCards through all layers")
    void sixthCardReturns409() throws Exception {
        String created = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("ivan@example.com")))
                .andReturn().getResponse().getContentAsString();
        long userId = objectMapper.readTree(created).get("id").asLong();

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/users/{userId}/cards", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cardJson("411111111111111" + i)))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(post("/users/{userId}/cards", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson("4111111111111119")))
                .andExpect(status().isConflict());
    }

    private String cardJson(String number) throws Exception {
        PaymentCardDto card = new PaymentCardDto(null, null, number,
                "IVAN IVANOV", LocalDate.of(2030, 12, 31), true);
        return objectMapper.writeValueAsString(card);
    }
}