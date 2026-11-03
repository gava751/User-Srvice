package com.innoboard.userservice.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentCardFlowIntegrationTest extends IntegrationTestBase {

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

        mockMvc.perform(put("/api/cards/{id}", cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson("4111111111111111")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value("4111111111111111"));

        mockMvc.perform(patch("/api/cards/{id}/status", cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusJson(false)))
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
                        .content(statusJson(false)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/users/{userId}/cards", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson("4222222222221119")))
                .andExpect(status().isCreated());

        mockMvc.perform(patch("/api/cards/{id}/status", cardId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusJson(true)))
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

    @Test
    @DisplayName("expired card -> 400 (@Future)")
    void expiredCardReturns400() throws Exception {
        long userId = createUser("ivan@example.com");

        mockMvc.perform(post("/api/users/{userId}/cards", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson("4111111111111111", LocalDate.of(2020, 1, 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.expirationDate").exists());
    }

    @Test
    @DisplayName("card number with letters -> 400 (@Pattern)")
    void invalidCardNumberReturns400() throws Exception {
        long userId = createUser("ivan@example.com");

        mockMvc.perform(post("/api/users/{userId}/cards", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson("ABCD-1234")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.number").exists());
    }

    @Test
    @DisplayName("card mutations evict the owner's cache entry")
    void cardMutationsEvictUserCache() throws Exception {
        long userId = createUser("ivan@example.com");
        long cardId = createCard(userId, "4111111111111111");

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk());
        assertThatCachePresent(userId);

        mockMvc.perform(put("/api/cards/{id}", cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson("4111111111111111")))
                .andExpect(status().isOk());
        assertThatCacheAbsent(userId);
    }

    private void assertThatCachePresent(long userId) {
        org.assertj.core.api.Assertions.assertThat(redisTemplate.hasKey(cacheKey(userId))).isTrue();
    }

    private void assertThatCacheAbsent(long userId) {
        org.assertj.core.api.Assertions.assertThat(redisTemplate.hasKey(cacheKey(userId))).isFalse();
    }
}
