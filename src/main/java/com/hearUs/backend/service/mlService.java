package com.hearUs.backend.service;

import com.hearUs.backend.dto.mlAnalysis;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Talks to the HearUs ML service (Python FastAPI, default http://127.0.0.1:8000).
 *
 * The ML service is a helper, never a blocker: if it is down, slow or returns
 * something unexpected, every method here logs the problem and returns an
 * empty result, and the complaint is still saved with the student's own values.
 */
@Service
public class mlService {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON_MAP =
            new ParameterizedTypeReference<>() {};

    private final RestClient client;
    private final boolean enabled;

    public mlService(
            @Value("${ml.service.url:http://127.0.0.1:8000}") String baseUrl,
            @Value("${ml.service.enabled:true}") boolean enabled,
            @Value("${ml.service.timeout-ms:5000}") long timeoutMs,
            @Value("${ml.service.api-key:}") String apiKey) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory);

        // Only needed if HEARUS_API_KEY is set on the ML service
        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader("X-API-Key", apiKey);
        }

        this.client = builder.build();
        this.enabled = enabled;
    }

    /**
     * Runs every model on a new complaint (category, urgency, safety rules,
     * profanity, duplicates). Call this BEFORE the complaint is saved.
     */
    public Optional<mlAnalysis> analyze(String title, String description) {
        if (!enabled) {
            return Optional.empty();
        }

        Map<String, Object> body = new HashMap<>();
        body.put("text", joinText(title, description));
        // the complaint has no ID yet; it is added to the duplicate index after saving
        body.put("register_for_duplicates", false);

        try {
            Map<String, Object> json = client.post()
                    .uri("/analyze")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JSON_MAP);

            return Optional.ofNullable(json).map(this::toAnalysis);

        } catch (Exception e) {
            System.err.println("ML service unavailable, saving complaint without ML: "
                    + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Adds a saved complaint to the ML duplicate index, so later complaints
     * about the same issue are linked to it. Call this AFTER saving.
     */
    public void registerForDuplicates(Long complaintId, String title,
                                      String description, String mlCategory) {
        if (!enabled || complaintId == null) {
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("complaint_id", String.valueOf(complaintId));
        body.put("text", joinText(title, description));
        if (mlCategory != null) {
            body.put("category", mlCategory);
        }

        try {
            client.post()
                    .uri("/duplicates/add")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            System.err.println("Could not add complaint " + complaintId
                    + " to ML duplicate index: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- helpers

    private static String joinText(String title, String description) {
        String t = title == null ? "" : title.trim();
        String d = description == null ? "" : description.trim();
        if (t.isEmpty()) return d;
        if (d.isEmpty()) return t;
        return t + ". " + d;
    }

    /** Reads the parts of the /analyze JSON the backend uses. */
    @SuppressWarnings("unchecked")
    private mlAnalysis toAnalysis(Map<String, Object> json) {
        mlAnalysis a = new mlAnalysis();

        Map<String, Object> category = asMap(json.get("category"));
        a.setCategory(asString(category.get("label")));
        a.setCategoryConfidence(asDouble(category.get("confidence")));

        Map<String, Object> urgency = asMap(json.get("urgency"));
        a.setUrgency(asString(urgency.get("label")));
        a.setSafetyHits(asStringList(urgency.get("rule_hits")));

        Map<String, Object> routing = asMap(json.get("routing"));
        a.setNotify(asStringList(routing.get("notify")));

        Map<String, Object> profanity = asMap(json.get("profanity"));
        a.setProfane(Boolean.TRUE.equals(profanity.get("is_profane")));

        a.setNeedsReview(Boolean.TRUE.equals(json.get("needs_review")));
        a.setReviewReasons(asStringList(json.get("review_reasons")));

        List<String> duplicateIds = new ArrayList<>();
        Object dupes = json.get("duplicates");
        if (dupes instanceof List<?> list) {
            for (Object item : list) {
                String id = asString(asMap(item).get("complaint_id"));
                if (id != null) {
                    duplicateIds.add(id);
                }
            }
        }
        a.setDuplicateIds(duplicateIds);

        return a;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static double asDouble(Object value) {
        return value instanceof Number n ? n.doubleValue() : 0.0;
    }

    private static List<String> asStringList(Object value) {
        List<String> out = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) {
                    out.add(String.valueOf(item));
                }
            }
        }
        return out;
    }
}
