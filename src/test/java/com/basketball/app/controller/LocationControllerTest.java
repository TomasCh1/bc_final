package com.basketball.app.controller;

import com.basketball.app.security.CustomUserDetailsService;
import com.basketball.app.security.JwtTokenProvider;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = LocationController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Tag("api")
class LocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void searchLocations_ShortQuery_ReturnsEmptyArrayWithoutExternalCall() throws Exception {
        mockMvc.perform(get("/api/locations/search").param("q", "ab"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}
