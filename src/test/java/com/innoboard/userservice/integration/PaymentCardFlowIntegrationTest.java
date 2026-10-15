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

    private long createUser() throws Exception {
        UserDto dto = new UserDto(null, "Ivan", "Ivanov",
                LocalDate.of(1990, 1, 1), "ivan@example.com", true);
        String created = mockMvc.perform(post("/users")
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
        String created = mockMvc.perform(post("/users/{userId}/cards", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson(number)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(created).get("id").asLong();
    }

    @Test
    @DisplayName("card lifecycle: create -> list -> get -> update -> deactivate")
    void fullCardLifecycle() throws Exception {
        long userId = createUser();
        long cardId = createCard(userId, "4111111111111111");

        mockMvc.perform(get("/users/{userId}/cards", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].number").value("4111111111111111"))
                .andExpect(jsonPath("$[0].userId").value((int) userId));

        mockMvc.perform(get("/users/{userId}/cards/{id}", userId, cardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holder").value("IVAN IVANOV"));

        PaymentCardDto update = new PaymentCardDto(null, null, "4111111111111111",
                "PETR IVANOV", LocalDate.of(2031, 6, 30), true);
        mockMvc.perform(put("/users/{userId}/cards/{id}", userId, cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holder").value("PETR IVANOV"));

        mockMvc.perform(patch("/users/{userId}/cards/{id}/deactivate", userId, cardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @DisplayName("card of another user looks like missing -> 404 (ownership)")
    void foreignCardReturns404() throws Exception {
        long userId = createUser();
        long cardId = createCard(userId, "4111111111111111");

        long otherUserId = createUser2();
        mockMvc.perform(get("/users/{userId}/cards/{id}", otherUserId, cardId))
                .andExpect(status().isNotFound());
    }

    private long createUser2() throws Exception {
        UserDto dto = new UserDto(null, "Petr", "Petrov",
                LocalDate.of(1985, 5, 5), "petr@example.com", true);
        String created = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(created).get("id").asLong();
    }

    @Test
    @DisplayName("card for missing user -> 404")
    void cardForMissingUserReturns404() throws Exception {
        mockMvc.perform(post("/users/{userId}/cards", 9999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson("4111111111111111")))
                .andExpect(status().isNotFound());
    }
}
