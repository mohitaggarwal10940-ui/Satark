package com.satark.backend.ai;


import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
        name = "satark.ai.provider",
        havingValue = "groq"
)
public class GroqLlmClient implements LlmClient {
    private static final String BASE_URL =
            "https://api.groq.com/openai/v1/chat/completions";

    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GroqLlmClient(
            AiProperties properties,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getTimeout())
                .build();
    }

    @Override
    public String getProvider() {
        return "groq";
    }

    @Override
    public boolean isAvailable() {
        return properties.isEnabled()
                && "groq".equalsIgnoreCase(properties.getProvider())
                && System.getenv("GROQ_API_KEY") != null
                && !System.getenv("GROQ_API_KEY").isBlank();
    }

    @Override
    public LlmResult analyze(
            String rawText,
            String language,
            String prompt
    ) {

        if (!isAvailable()) {
            throw new IllegalStateException("Groq is not available");
        }

        try {
            String apiKey = System.getenv("GROQ_API_KEY");

            String requestBody = """
        {
          "model": "%s",
          "messages": [
            {
              "role": "user",
              "content": %s
            }
          ],
          "temperature": 0.2,
          "max_completion_tokens": 800,
          "reasoning_effort": "low",
          "reasoning_format": "hidden",
          "response_format": {
            "type": "json_object"
          }
        }
        """.formatted(
                    escapeJson(properties.getModel()),
                    objectMapper.writeValueAsString(prompt)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL))
                    .timeout(properties.getTimeout())
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (response.statusCode() < 200 ||
                    response.statusCode() >= 300) {

                System.out.println("========== GROQ HTTP ERROR ==========");
                System.out.println("Status: " + response.statusCode());
                System.out.println("Body: " + response.body());
                System.out.println("=====================================");

                throw new IllegalStateException(
                        "Groq API returned HTTP " + response.statusCode()
                );
            }

            JsonNode root =
                    objectMapper.readTree(response.body());

            String content = root
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText();

            if (content == null || content.isBlank()) {
                throw new IllegalStateException(
                        "Groq returned empty content"
                );
            }

            JsonNode result =
                    objectMapper.readTree(content);

            String explanation =
                    result.path("explanation").asText("");

            List<String> actions =
                    objectMapper.convertValue(
                            result.path("recommendedActions"),
                            objectMapper.getTypeFactory()
                                    .constructCollectionType(
                                            List.class,
                                            String.class
                                    )
                    );
            if (explanation.isBlank()) {
                throw new IllegalStateException(
                        "Groq response missing explanation"
                );
            }

            if (actions == null || actions.size() < 3 || actions.size() > 6) {
                throw new IllegalStateException(
                        "Groq returned " +
                                (actions == null ? 0 : actions.size()) +
                                " recommended actions; expected 3 to 6"
                );
            }
            return new LlmResult(
                    explanation,
                    actions
            );

        } catch (Exception e) {
            System.out.println("========== GROQ ERROR ==========");
            System.out.println("Type: " + e.getClass().getName());
            System.out.println("Message: " + e.getMessage());

            if (e.getCause() != null) {
                System.out.println("Cause: " + e.getCause().getClass().getName());
                System.out.println("Cause message: " + e.getCause().getMessage());
            }

            e.printStackTrace();
            System.out.println("================================");

            throw new IllegalStateException("Groq analysis failed", e);
        }
    }

    private String escapeJson(String value) {
        return value == null ? "" :
                value.replace("\\", "\\\\")
                        .replace("\"", "\\\"");
    }
}