package com.universalfit.controller;

import com.universalfit.domain.model.FitRecommendation;
import com.universalfit.security.JwtUtil;
import com.universalfit.service.RecommendationService;
import com.universalfit.security.CustomUserDetailsService;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
@Import(com.universalfit.security.SecurityConfig.class)
class RecommendationControllerIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtUtil jwtUtil; // real bean with test secret

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public JwtUtil jwtUtil() {
            String secret = "testsecrettestsecrettestsecrettestsecret"; // 32+ chars
            return new JwtUtil(secret, 15, 14);
        }
    }

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
        Mockito.when(customUserDetailsService.loadUserByUsername("user@example.com")).thenReturn(user);
    }

    @Test
    @DisplayName("No token → 401 Unauthorized")
    void noToken() throws Exception {
        mvc.perform(post("/api/recommendations/" + new ObjectId().toHexString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Valid token + invalid product id → 400 Bad Request")
    void invalidId() throws Exception {
        String token = jwtUtil.generateAccessToken("user@example.com");
        mvc.perform(post("/api/recommendations/invalid-id")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Valid token + valid product id → reaches RecommendationService and returns 200")
    void validRequest() throws Exception {
        ObjectId productId = new ObjectId();
        String token = jwtUtil.generateAccessToken("user@example.com");

        RecommendationService mockService = Mockito.mock(RecommendationService.class);
        FitRecommendation dummy = FitRecommendation.builder().build();
        Mockito.when(mockService.recommend(Mockito.eq("user@example.com"), Mockito.eq(productId)))
                .thenReturn(dummy);

        // Inject mock into controller
        var controller = mvc.getDispatcherServlet().getWebApplicationContext()
                .getBean(com.universalfit.controller.RecommendationController.class);
        var field = com.universalfit.controller.RecommendationController.class.getDeclaredField("recommendationService");
        field.setAccessible(true);
        field.set(controller, mockService);

        ResultActions result = mvc.perform(post("/api/recommendations/" + productId.toHexString())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk());
    }
}
