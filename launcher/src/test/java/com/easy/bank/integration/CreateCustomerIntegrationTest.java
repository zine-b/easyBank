package com.easy.bank.integration;

import com.easy.bank.launcher.EasyBankApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = EasyBankApplication.class)
@AutoConfigureMockMvc
@Transactional
@DisplayName("POST /api/v1/customers — tests d'intégration")
class CreateCustomerIntegrationTest {

    private static final String URL = "/api/v1/customers";

    @Autowired
    private MockMvc mockMvc;

    // ─── CAS NOMINAUX ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("201 — création réussie avec tous les champs")
    void shouldReturn201_whenRequestIsValid() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Ahmed",
                                  "lastName":  "Benali",
                                  "email":     "ahmed.benali@example.com",
                                  "phone":     "+33612345678"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").isNotEmpty())
                .andExpect(jsonPath("$.firstName").value("Ahmed"))
                .andExpect(jsonPath("$.lastName").value("Benali"))
                .andExpect(jsonPath("$.email").value("ahmed.benali@example.com"))
                .andExpect(jsonPath("$.phone").value("+33612345678"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("201 — téléphone optionnel (null accepté)")
    void shouldReturn201_whenPhoneIsAbsent() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Sara",
                                  "lastName":  "Lopez",
                                  "email":     "sara.lopez@example.com"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").isNotEmpty())
                .andExpect(jsonPath("$.email").value("sara.lopez@example.com"));
    }

    // ─── EMAIL DÉJÀ UTILISÉ (RG) ───────────────────────────────────────────────

    @Test
    @DisplayName("409 EMAIL_ALREADY_USED — doublon d'e-mail dans la même session")
    void shouldReturn409_whenEmailAlreadyExists() throws Exception {
        String body = """
                {
                  "firstName": "Ahmed",
                  "lastName":  "Benali",
                  "email":     "doublon@example.com",
                  "phone":     "+33612345678"
                }
                """;

        // première inscription → 201
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // même e-mail → 409
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_USED"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("doublon@example.com")));
    }

    // ─── VALIDATION 400 ────────────────────────────────────────────────────────

    @Test
    @DisplayName("400 VALIDATION_ERROR — e-mail invalide")
    void shouldReturn400_whenEmailIsInvalid() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Ahmed",
                                  "lastName":  "Benali",
                                  "email":     "pas-un-email",
                                  "phone":     "+33612345678"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("email")));
    }

    @Test
    @DisplayName("400 VALIDATION_ERROR — prénom vide")
    void shouldReturn400_whenFirstNameIsBlank() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "   ",
                                  "lastName":  "Benali",
                                  "email":     "ahmed@example.com",
                                  "phone":     "+33612345678"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("400 VALIDATION_ERROR — nom de famille vide")
    void shouldReturn400_whenLastNameIsBlank() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Ahmed",
                                  "lastName":  "",
                                  "email":     "ahmed@example.com",
                                  "phone":     "+33612345678"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("400 VALIDATION_ERROR — format téléphone invalide")
    void shouldReturn400_whenPhoneFormatIsInvalid() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Ahmed",
                                  "lastName":  "Benali",
                                  "email":     "ahmed@example.com",
                                  "phone":     "abc"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("phone")));
    }

    @Test
    @DisplayName("400 VALIDATION_ERROR — e-mail absent (champ obligatoire)")
    void shouldReturn400_whenEmailIsMissing() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Ahmed",
                                  "lastName":  "Benali"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("400 — Content-Type absent (pas de JSON)")
    void shouldReturn400_whenContentTypeIsMissing() throws Exception {
        mockMvc.perform(post(URL)
                        .content("{}"))
                .andExpect(status().is4xxClientError());
    }
}
