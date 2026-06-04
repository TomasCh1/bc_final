package com.basketball.app.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Proxy controller for location search (Nominatim/OpenStreetMap).
 * Avoids CORS and keeps external API calls server-side.
 */
@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";
    private static final String USER_AGENT = "BasketballTeamManagement/1.0";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/search")
    public ResponseEntity<?> searchLocations(@RequestParam String q) {
        if (q == null || q.trim().length() < 3) {
            return ResponseEntity.ok(List.of());
        }
        try {
            String encodedQuery = URLEncoder.encode(q.trim(), StandardCharsets.UTF_8);
            String url = NOMINATIM_URL + "?format=json&q=" + encodedQuery + "&limit=5&addressdetails=1";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                return ResponseEntity.status(response.statusCode()).body(Map.of("error", "Location service unavailable"));
            }
            List<Map<String, Object>> results = objectMapper.readValue(response.body(), new TypeReference<>() {});
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch location suggestions: " + e.getMessage()));
        }
    }
}
