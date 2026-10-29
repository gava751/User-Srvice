package com.innoboard.userservice.integration;

import com.innoboard.userservice.dto.PaymentCardDto;
import com.innoboard.userservice.dto.UserDto;
import com.innoboard.userservice.repository.PaymentCardRepository;
import com.innoboard.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;

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
class PaymentCardFlowIntegrationTest {

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

    private long createUser(String email) throws Exception {
        UserDto dto = new UserDto(null, "Ivan", "Ivanov",
                LocalDate.of(1990, 1, 1), email, true);
        String created = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
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
    @DisplayName("card lifecycle: create -> list -> get by flat route -> update -> status")
    void fullCardLifecycle() throws Exception {
        long userId = createUser("ivan@example.com");
        long cardId = createCard(userId, "4111111111111111");

        mockMvc.perform(get("/api/users/{userId}/cards", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].number").value("4111111111111111"))
                .andExpect(jsonPath("$[0].userId").value((int) userId));

        mockMvc.perform(get("/api/cards/{id}", cardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holder").value("IVAN IVANOV"));

        PaymentCardDto update = new PaymentCardDto(null, null, "4111111111111111",
                "PETR IVANOV", LocalDate.of(2031, 6, 30), true);
        mockMvc.perform(put("/api/cards/{id}", cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holder").value("PETR IVANOV"));

        mockMvc.perform(patch("/api/cards/{id}/status", cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @DisplayName("duplicate card number -> 409")
    void duplicateCardNumberReturns409() throws Exception {
        long userId = createUser("ivan@example.com");
        createCard(userId, "4111111111111111");

        mockMvc.perform(post("/api/users/{userId}/cards", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson("4111111111111111")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("card for missing user -> 404")
    void cardForMissingUserReturns404() throws Exception {
        mockMvc.perform(post("/api/users/{userId}/cards", 9999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson("4111111111111111")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("cards of missing user -> 404 (not an empty list)")
    void cardsOfMissingUserReturn404() throws Exception {
        mockMvc.perform(get("/api/users/{userId}/cards", 9999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("card limit counts only ACTIVE cards (deactivate -> add -> reactivate = 409)")
    void limitCountsOnlyActiveCards() throws Exception {
        long userId = createUser("ivan@example.com");
        long cardId1 = 0;

        for (int i = 0; i < 5; i++) {
            long id = createCard(userId, "411111111111111" + i);
            if (i == 0) {
                cardId1 = id;
            }
        }

        mockMvc.perform(post("/api/users/{userId}/cards", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson("4222222222221119")))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/cards/{id}/status", cardId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": false}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/users/{userId}/cards", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson("4222222222221119")))
                .andExpect(status().isCreated());

        mockMvc.perform(patch("/api/cards/{id}/status", cardId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": true}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("DELETE card -> 204, then card is gone")
    void deleteCardReturns204AndRemovesCard() throws Exception {
        long userId = createUser("ivan@example.com");
        long cardId = createCard(userId, "4111111111111111");

        mockMvc.perform(delete("/api/cards/{id}", cardId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/cards/{id}", cardId))
                .andExpect(status().isNotFound());
    }
}
