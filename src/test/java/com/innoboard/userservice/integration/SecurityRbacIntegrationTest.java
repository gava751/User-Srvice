package com.innoboard.userservice.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityRbacIntegrationTest extends IntegrationTestBase {

    @Value("${app.internal.token}")
    private String internalToken;

    @Test
    @DisplayName("no token -> 401 on secured endpoints")
    void missingTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("ivan@example.com")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("health endpoint is open without a token")
    void healthEndpointIsPermitted() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("USER role cannot access admin-only endpoints")
    void userRoleRejectedOnAdminEndpoints() throws Exception {
        long userId = createUser("ivan@example.com");

        mockMvc.perform(post("/api/users")
                        .with(userToken(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("petr@example.com")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/users")
                        .with(userToken(userId)))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/users/{id}/status", userId)
                        .with(userToken(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusJson(false)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/users/{id}", userId)
                        .with(userToken(userId)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("USER can read and update only own profile")
    void userAccessesOnlyOwnProfile() throws Exception {
        long ownId = createUser("ivan@example.com");
        long otherId = createUser("petr@example.com");

        mockMvc.perform(get("/api/users/{id}", ownId)
                        .with(userToken(ownId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value((int) ownId));

        mockMvc.perform(get("/api/users/{id}", otherId)
                        .with(userToken(ownId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(put("/api/users/{id}", otherId)
                        .with(userToken(ownId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("updated@example.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("USER card requests with a foreign owner id are rejected (BOLA)")
    void userCardTargetComesFromToken() throws Exception {
        long ownId = createUser("ivan@example.com");
        long otherId = createUser("petr@example.com");

        mockMvc.perform(post("/api/users/{userId}/cards", otherId)
                        .with(userToken(ownId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson("4111111111111111")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(post("/api/users/{userId}/cards", ownId)
                        .with(userToken(ownId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson("4111111111111111")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value((int) ownId));

        mockMvc.perform(get("/api/users/{userId}/cards", otherId)
                        .with(userToken(ownId)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users/{userId}/cards", ownId)
                        .with(userToken(ownId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value((int) ownId))
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/users/{userId}/cards", ownId)
                        .with(userToken(otherId)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("USER can manage own cards but not foreign cards")
    void userManagesOnlyOwnCards() throws Exception {
        long ownId = createUser("ivan@example.com");
        long otherId = createUser("petr@example.com");
        long ownCardId = createCard(ownId, "4111111111111111");
        long otherCardId = createCard(otherId, "4222222222222222");

        mockMvc.perform(get("/api/cards/{id}", ownCardId)
                        .with(userToken(ownId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) ownCardId));

        mockMvc.perform(get("/api/cards/{id}", otherCardId)
                        .with(userToken(ownId)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/cards/{id}", otherCardId)
                        .with(userToken(ownId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson("4222222222222222")))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/cards/{id}/status", otherCardId)
                        .with(userToken(ownId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusJson(false)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/cards/{id}", otherCardId)
                        .with(userToken(ownId)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/cards/{id}", ownCardId)
                        .with(userToken(ownId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/cards/{id}", ownCardId)
                        .with(adminToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("ADMIN manages any user and any card")
    void adminHasUnrestrictedAccess() throws Exception {
        long userId = createUser("ivan@example.com");
        long cardId = createCard(userId, "4111111111111111");

        mockMvc.perform(get("/api/users/{id}", userId)
                        .with(adminToken()))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/users/{id}/status", userId)
                        .with(adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusJson(false)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/cards/{id}", cardId)
                        .with(adminToken()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("internal endpoints require the internal token")
    void internalEndpointsRequireToken() throws Exception {
        mockMvc.perform(post("/internal/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("ivan@example.com")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/internal/users/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/internal/users")
                        .header("X-Internal-Token", "wrong-value")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("ivan@example.com")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/internal/users")
                        .header("X-Internal-Token", internalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("ivan@example.com")))
                .andExpect(status().isCreated());
    }
}
