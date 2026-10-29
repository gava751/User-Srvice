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
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });
    }

    private String userJson(String email) throws Exception {
        UserDto dto = new UserDto(null, "Ivan", "Ivanov",
                LocalDate.of(1990, 1, 1), email, true);
        return objectMapper.writeValueAsString(dto);
    }

    private long createUser(String email) throws Exception {
        String created = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(created).get("id").asLong();
    }

    private String cardJson(String number) throws Exception {
        PaymentCardDto card = new PaymentCardDto(null, null, number,
                "IVAN IVANOV", LocalDate.of(2030, 12, 31), true);
        return objectMapper.writeValueAsString(card);
    }

    private long createCard(long userId, String number) throws Exception {
        String created = mockMvc.perform(post("/api/users/{userId}/cards", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson(number)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(created).get("id").asLong();
    }

    @Test
    @DisplayName("full lifecycle: create -> get (aggregate) -> update -> status -> list")
    void fullUserLifecycle() throws Exception {
        long id = createUser("ivan@example.com");

        mockMvc.perform(get("/api/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value((int) id))
                .andExpect(jsonPath("$.user.name").value("Ivan"))
                .andExpect(jsonPath("$.user.email").value("ivan@example.com"))
                .andExpect(jsonPath("$.cards").isArray());

        mockMvc.perform(put("/api/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("petr@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("petr@example.com"));

        mockMvc.perform(patch("/api/users/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/users")
                        .param("surname", "Ivanov").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value((int) id));

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("invalid email -> 400 with field error from validation")
    void invalidEmailReturns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("not-an-email")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    @DisplayName("duplicate email -> 409")
    void duplicateEmailReturns409() throws Exception {
        createUser("ivan@example.com");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("ivan@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("missing user -> 404 with ErrorResponse")
    void missingUserReturns404() throws Exception {
        mockMvc.perform(get("/api/users/{id}", 9999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("sixth card -> 409 card limit through all layers")
    void sixthCardReturns409() throws Exception {
        long userId = createUser("ivan@example.com");

        for (int i = 0; i < 5; i++) {
            createCard(userId, "411111111111111" + i);
        }

        mockMvc.perform(post("/api/users/{userId}/cards", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson("4111111111111119")))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("deleting a user returns 204, removes its cards, and the user becomes 404")
    void deletingUserRemovesCards() throws Exception {
        long userId = createUser("ivan@example.com");
        createCard(userId, "4111111111111111");

        mockMvc.perform(delete("/api/users/{id}", userId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isNotFound());

        assertThat(paymentCardRepository.count()).isZero();
    }

    @Test
    @DisplayName("cache: second GET is served from Redis; update evicts the entry")
    void cacheReadAndEviction() throws Exception {
        long id = createUser("ivan@example.com");

        mockMvc.perform(get("/api/users/{id}", id))
                .andExpect(status().isOk());
        assertThat(redisTemplate.hasKey("usersWithCards::" + id)).isTrue();

        mockMvc.perform(get("/api/users/{id}", id))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("petr@example.com")))
                .andExpect(status().isOk());

        assertThat(redisTemplate.hasKey("usersWithCards::" + id)).isFalse();
    }
}
