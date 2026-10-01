package com.zidio.keystone;

import com.jayway.jsonpath.JsonPath;
import com.zidio.keystone.repository.WorkOrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the rules the brief says matter most: authentication,
 * logout, role-based access, and the work-order lifecycle. They call the real API
 * (MockMvc) against the seeded data, so they prove the rules hold on the server,
 * not just in the UI.
 *
 * Seed data used: WO-0001 (NEW, Meridian), WO-0002 (ASSIGNED to the technician,
 * Meridian), WO-0003 (IN_PROGRESS, Blue Harbor). customer@keystone.dev belongs to Meridian.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthAndAccessIntegrationTest {

    private static final String PASSWORD = "Passw0rd!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorkOrderRepository workOrderRepository;

    // ---------- helpers ----------

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private Long idOf(String code) {
        return workOrderRepository.findAll().stream()
                .filter(w -> code.equals(w.getCode()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Seed work order missing: " + code))
                .getId();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    // ---------- authentication ----------

    @Test
    void loginWithCorrectPasswordReturnsToken() throws Exception {
        String token = login("admin@keystone.dev", PASSWORD);
        org.junit.jupiter.api.Assertions.assertFalse(token.isBlank());
    }

    @Test
    void loginWithWrongPasswordIsRejectedWith401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@keystone.dev\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithoutTokenIsRejected() throws Exception {
        // 401 or 403 depending on the entry point; either way anonymous access is denied.
        mockMvc.perform(get("/api/work-orders"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void tamperedTokenIsRejected() throws Exception {
        String token = login("admin@keystone.dev", PASSWORD);
        mockMvc.perform(get("/api/work-orders")
                        .header("Authorization", bearer(token + "x")))
                .andExpect(status().is4xxClientError());
    }

    // ---------- logout ----------

    @Test
    void loggedOutTokenIsRejectedWith401() throws Exception {
        String token = login("manager@keystone.dev", PASSWORD);

        mockMvc.perform(get("/api/work-orders").header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout").header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/work-orders").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    // ---------- authorisation (the threats named in the brief) ----------

    @Test
    void customerCanReadOwnWorkOrder() throws Exception {
        String token = login("customer@keystone.dev", PASSWORD);
        mockMvc.perform(get("/api/work-orders/" + idOf("WO-0001")).header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void customerCannotReadAnotherCustomersWorkOrder() throws Exception {
        String token = login("customer@keystone.dev", PASSWORD);
        // WO-0003 belongs to Blue Harbor, not Meridian.
        mockMvc.perform(get("/api/work-orders/" + idOf("WO-0003")).header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void technicianCannotCloseAWorkOrder() throws Exception {
        String token = login("technician@keystone.dev", PASSWORD);
        mockMvc.perform(post("/api/work-orders/" + idOf("WO-0002") + "/status")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toStatus\":\"CLOSED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotDeleteAWorkOrder() throws Exception {
        String token = login("customer@keystone.dev", PASSWORD);
        mockMvc.perform(delete("/api/work-orders/" + idOf("WO-0001")).header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    // ---------- lifecycle ----------

    @Test
    void illegalTransitionIsRejectedWith409() throws Exception {
        String token = login("manager@keystone.dev", PASSWORD);
        // WO-0001 is NEW; NEW -> COMPLETED is not an allowed jump.
        mockMvc.perform(post("/api/work-orders/" + idOf("WO-0001") + "/status")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toStatus\":\"COMPLETED\"}"))
                .andExpect(status().isConflict());
    }
}
