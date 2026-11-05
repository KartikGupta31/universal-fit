package com.universalfit.controller;

import com.universalfit.AbstractMongoIntegrationTest;
import com.universalfit.domain.model.FitRecommendation;
import com.universalfit.security.CustomUserDetailsService;
import com.universalfit.security.JwtUtil;
import com.universalfit.service.RecommendationService;
import java.util.List;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecommendationControllerIntegrationTest extends AbstractMongoIntegrationTest {

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        UserDetails user = User.builder()
                .username("user@example.com")
                .password("dummy")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
        Mockito.when(customUserDetailsService.loadUserByUsername("user@example.com"))
                .thenReturn(user);
    }

    @Test
    @DisplayName("No token returns 401 Unauthorized")
    void noToken() throws Exception {
        mockMvc.perform(post("/api/recommendations/" + new ObjectId().toHexString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Valid access token with invalid product ID returns 400")
    void invalidId() throws Exception {
        String token = jwtUtil.generateAccessToken("user@example.com");

        mockMvc.perform(post("/api/recommendations/invalid-id")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Valid access token reaches the recommendation service")
    void validRequest() throws Exception {
        ObjectId productId = new ObjectId();
        String token = jwtUtil.generateAccessToken("user@example.com");
        FitRecommendation expected = FitRecommendation.builder()
                .recommendedSize("M")
                .confidence(0.8)
                .uncertain(false)
                .reason("Test recommendation")
                .build();

        Mockito.when(recommendationService.recommend(eq("user@example.com"), eq(productId)))
                .thenReturn(expected);

        mockMvc.perform(post("/api/recommendations/" + productId.toHexString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        Mockito.verify(recommendationService)
                .recommend("user@example.com", productId);
    }
}
