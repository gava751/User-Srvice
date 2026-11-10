package com.innoboard.userservice.integration;

import com.innoboard.userservice.dto.CardRequest;
import com.innoboard.userservice.dto.CreateUserRequest;
import com.innoboard.userservice.dto.UpdateStatusRequest;
import com.innoboard.userservice.repository.PaymentCardRepository;
import com.innoboard.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTestBase {

    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    @ServiceConnection
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7").withExposedPorts(6379);

    static {
        postgres.start();
        redis.start();
    }

    @DynamicPropertySource
    static void internalTokenProperties(DynamicPropertyRegistry registry) {
        registry.add("app.internal.token", () -> "test-internal-token");
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PaymentCardRepository paymentCardRepository;

    @Autowired
    protected StringRedisTemplate redisTemplate;

    @BeforeEach
    void cleanState() {
        paymentCardRepository.deleteAll();
        userRepository.deleteAll();
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });
    }

    protected RequestPostProcessor adminToken() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    protected RequestPostProcessor userToken(long userId) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.claim("user_id", userId))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    protected String userJson(String email) throws Exception {
        CreateUserRequest request = new CreateUserRequest("Ivan", "Ivanov",
                LocalDate.of(1990, 1, 1), email);
        return objectMapper.writeValueAsString(request);
    }

    protected long createUser(String email) throws Exception {
        String created = mockMvc.perform(post("/api/users")
                        .with(adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(created).get("id").asLong();
    }

    protected String cardJson(String number) throws Exception {
        return cardJson(number, LocalDate.of(2030, 12, 31));
    }

    protected String cardJson(String number, LocalDate expirationDate) throws Exception {
        CardRequest request = new CardRequest(number, "IVAN IVANOV", expirationDate);
        return objectMapper.writeValueAsString(request);
    }

    protected long createCard(long userId, String number) throws Exception {
        String created = mockMvc.perform(post("/api/users/{userId}/cards", userId)
                        .with(adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson(number)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(created).get("id").asLong();
    }

    protected String statusJson(boolean active) throws Exception {
        return objectMapper.writeValueAsString(new UpdateStatusRequest(active));
    }

    protected String cacheKey(long userId) {
        return "usersWithCards::" + userId;
    }

    protected void awaitCacheEntry(long userId) {
        await().atMost(Duration.ofSeconds(2)).untilAsserted(() ->
                assertThat(redisTemplate.hasKey(cacheKey(userId))).isTrue());
    }

    protected void awaitNoCacheEntry(long userId) {
        await().atMost(Duration.ofSeconds(2)).untilAsserted(() ->
                assertThat(redisTemplate.hasKey(cacheKey(userId))).isFalse());
    }
}
