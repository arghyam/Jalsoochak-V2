package com.example.user.controller;

import com.example.user.repository.UserCommonRepository;
import com.example.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private UserCommonRepository userCommonRepository;

    @Test
    void completeProfile_shouldReturnValidationErrors_forInvalidEmailAndPhone() throws Exception {
        when(userCommonRepository.existsTenantByStateCode(anyString())).thenReturn(true);

        String payload = """
                {
                  "firstName": "Blossom",
                  "lastName": "Esezobor",
                  "email": "invalid-email",
                  "phoneNumber": "123",
                  "personType": "super_user",
                  "token": "token-1",
                  "password": "Password7@",
                  "tenantId": "KA"
                }
                """;

        mockMvc.perform(post("/api/v2/auth/complete/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.phoneNumber").exists());
    }

    @Test
    void refresh_shouldReturnValidationError_whenRefreshTokenBlank() throws Exception {
        String payload = """
                {
                  "refreshToken": ""
                }
                """;

        mockMvc.perform(post("/api/v2/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.refreshToken").exists());
    }
}
