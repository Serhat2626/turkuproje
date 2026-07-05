package com.anadoluatlasi.backend.imagegen.provider;

import com.anadoluatlasi.backend.imagegen.entity.ImageGenerationErrorCode;
import com.anadoluatlasi.backend.imagegen.service.ProviderOutputRetrievalRequest;
import com.anadoluatlasi.backend.imagegen.service.ProviderOutputRetrievalResult;
import com.anadoluatlasi.backend.imagegen.service.RetrievalValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * FP-009 Task 07 — Provider URL Gap Resolution Tests.
 *
 * Verifies:
 * 1. URL is extracted transiently from OpenAI response.
 * 2. URL is NOT in ImageProviderGenerationResult (safe result).
 * 3. URL is NOT in any DTO/entity/persisted model.
 * 4. ProviderOutputReference.toString() does not leak the URL.
 * 5. Missing/null URL returns null safely.
 * 6. No storage/approval/publish side effects.
 */
class ProviderOutputReferenceAndUrlExtractionTest {

    private static final UUID REQUEST_ID = UUID.randomUUID();
    private static final UUID CORRELATION_ID = UUID.randomUUID();
    private static final String OPENAI_URL = "https://api.openai.com/v1/images/generations";

    private static final Set<String> FORBIDDEN_FIELD_NAMES = Set.of(
            "providerUrl", "providerOutputUrl", "providerImageUrl",
            "providerDownloadUrl", "signedUrl", "publicUrl", "cdnUrl",
            "storageObjectKey", "storageKey", "base64", "imageBase64",
            "rawProviderResponse", "rawResponse", "apiKey", "secretKey",
            "bearerToken", "password", "stackTrace"
    );

    private ImageGenerationProviderConfig config;

    @BeforeEach
    void setUp() {
        config = new ImageGenerationProviderConfig();
        config.setMode("openai");
        config.setModelName("dall-e-3");
        config.setImageSize("1024x1024");
        config.setMaxOutputCount(1);
        config.setTimeoutMs(60000);
    }

    private ImageProviderGenerationRequest createRequest() {
        return ImageProviderGenerationRequest.builder()
                .requestId(REQUEST_ID)
                .correlationId(CORRELATION_ID)
                .safePromptSummary("A serene landscape")
                .itemType("SCENE_IMAGE")
                .build();
    }

    // ── A. URL Extraction ───────────────────────────────────────────

    @Nested
    @DisplayName("A. OpenAI URL Extraction")
    class UrlExtraction {

        @Test
        @DisplayName("Extracts URL from valid OpenAI response data[0].url")
        void extractsUrlFromValidResponse() {
            OpenAiImageGenerationProviderGateway gateway =
                    new OpenAiImageGenerationProviderGateway(config, "key", RestClient.create());

            Map<String, Object> response = Map.of(
                    "data", List.of(Map.of(
                            "url", "https://oai.example.com/image-123.png",
                            "revised_prompt", "revised"
                    ))
            );

            String url = gateway.extractProviderOutputUrl(response);
            assertThat(url).isEqualTo("https://oai.example.com/image-123.png");
        }

        @Test
        @DisplayName("generateWithOutputReference returns URL in ProviderOutputReference")
        void generateWithRef_returnsUrlInReference() {
            RestClient.Builder builder = RestClient.builder().baseUrl(OPENAI_URL);
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

            String responseBody = """
                    {
                      "created": 1234567890,
                      "data": [{"url": "https://oai.example.com/gen-img-789.png"}]
                    }
                    """;
            server.expect(requestTo(OPENAI_URL))
                    .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

            OpenAiImageGenerationProviderGateway gateway =
                    new OpenAiImageGenerationProviderGateway(config, "test-key", builder.build());

            ProviderOutputReference ref = gateway.generateWithOutputReference(createRequest());

            assertThat(ref.generationResult().isSuccess()).isTrue();
            assertThat(ref.providerOutputUrl()).isEqualTo("https://oai.example.com/gen-img-789.png");
            server.verify();
        }

        @Test
        @DisplayName("URL is NOT in the safe ImageProviderGenerationResult")
        void urlNotInSafeResult() {
            RestClient.Builder builder = RestClient.builder().baseUrl(OPENAI_URL);
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

            String responseBody = """
                    {
                      "data": [{"url": "https://oai.example.com/secret-img.png"}]
                    }
                    """;
            server.expect(requestTo(OPENAI_URL))
                    .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

            OpenAiImageGenerationProviderGateway gateway =
                    new OpenAiImageGenerationProviderGateway(config, "test-key", builder.build());

            ProviderOutputReference ref = gateway.generateWithOutputReference(createRequest());
            ImageProviderGenerationResult safeResult = ref.generationResult();

            // Safe result toString must NOT contain the provider URL
            String resultStr = safeResult.toString();
            assertThat(resultStr).doesNotContain("oai.example.com");
            assertThat(resultStr).doesNotContain("secret-img");

            server.verify();
        }

        @Test
        @DisplayName("generate() backward compat — still returns safe result without URL")
        void generate_backwardCompat() {
            RestClient.Builder builder = RestClient.builder().baseUrl(OPENAI_URL);
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

            server.expect(requestTo(OPENAI_URL))
                    .andRespond(withSuccess(
                            "{\"data\":[{\"url\":\"https://oai.example.com/img.png\"}]}",
                            MediaType.APPLICATION_JSON));

            OpenAiImageGenerationProviderGateway gateway =
                    new OpenAiImageGenerationProviderGateway(config, "test-key", builder.build());

            // Existing generate() still works — URL is discarded
            ImageProviderGenerationResult result = gateway.generate(createRequest());
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.toString()).doesNotContain("oai.example.com");
            server.verify();
        }
    }

    // ── B. Missing URL ──────────────────────────────────────────────

    @Nested
    @DisplayName("B. Missing URL")
    class MissingUrl {

        @Test
        @DisplayName("Null response → null URL")
        void nullResponse_returnsNull() {
            OpenAiImageGenerationProviderGateway gateway =
                    new OpenAiImageGenerationProviderGateway(config, "key", RestClient.create());
            assertThat(gateway.extractProviderOutputUrl(null)).isNull();
        }

        @Test
        @DisplayName("Empty data list → null URL")
        void emptyDataList_returnsNull() {
            OpenAiImageGenerationProviderGateway gateway =
                    new OpenAiImageGenerationProviderGateway(config, "key", RestClient.create());
            assertThat(gateway.extractProviderOutputUrl(Map.of("data", List.of()))).isNull();
        }

        @Test
        @DisplayName("No url key in data item → null URL")
        void noUrlKey_returnsNull() {
            OpenAiImageGenerationProviderGateway gateway =
                    new OpenAiImageGenerationProviderGateway(config, "key", RestClient.create());
            Map<String, Object> response = Map.of(
                    "data", List.of(Map.of("revised_prompt", "text only"))
            );
            assertThat(gateway.extractProviderOutputUrl(response)).isNull();
        }

        @Test
        @DisplayName("Blank url string → null URL")
        void blankUrl_returnsNull() {
            OpenAiImageGenerationProviderGateway gateway =
                    new OpenAiImageGenerationProviderGateway(config, "key", RestClient.create());
            Map<String, Object> response = new HashMap<>();
            response.put("data", List.of(Map.of("url", "   ")));
            assertThat(gateway.extractProviderOutputUrl(response)).isNull();
        }

        @Test
        @DisplayName("Missing URL in full generation → ref has null URL, result is still success")
        void generateWithRef_missingUrl_stillSucceeds() {
            RestClient.Builder builder = RestClient.builder().baseUrl(OPENAI_URL);
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

            server.expect(requestTo(OPENAI_URL))
                    .andRespond(withSuccess(
                            "{\"data\":[{\"revised_prompt\":\"no url\"}]}",
                            MediaType.APPLICATION_JSON));

            OpenAiImageGenerationProviderGateway gateway =
                    new OpenAiImageGenerationProviderGateway(config, "test-key", builder.build());

            ProviderOutputReference ref = gateway.generateWithOutputReference(createRequest());
            assertThat(ref.generationResult().isSuccess()).isTrue();
            assertThat(ref.providerOutputUrl()).isNull();
            server.verify();
        }
    }

    // ── C. Forbidden Field Regression ────────────────────────────────

    @Nested
    @DisplayName("C. Forbidden Field Regression")
    class ForbiddenFieldRegression {

        @Test
        @DisplayName("ImageProviderGenerationResult has no provider URL field")
        void safeResult_noUrlField() {
            Set<String> fields = getFieldNames(ImageProviderGenerationResult.class);
            for (String forbidden : FORBIDDEN_FIELD_NAMES) {
                assertThat(fields)
                        .as("ImageProviderGenerationResult must not contain: %s", forbidden)
                        .doesNotContain(forbidden);
            }
        }

        @Test
        @DisplayName("ProviderOutputRetrievalRequest has no provider URL field")
        void retrievalRequest_noUrlField() {
            Set<String> fields = getFieldNames(ProviderOutputRetrievalRequest.class);
            for (String forbidden : FORBIDDEN_FIELD_NAMES) {
                assertThat(fields).doesNotContain(forbidden);
            }
        }

        @Test
        @DisplayName("ProviderOutputRetrievalResult has no provider URL field")
        void retrievalResult_noUrlField() {
            Set<String> fields = getFieldNames(ProviderOutputRetrievalResult.class);
            for (String forbidden : FORBIDDEN_FIELD_NAMES) {
                assertThat(fields).doesNotContain(forbidden);
            }
        }

        @Test
        @DisplayName("RetrievalValidationResult has no provider URL field")
        void validationResult_noUrlField() {
            Set<String> fields = getFieldNames(RetrievalValidationResult.class);
            for (String forbidden : FORBIDDEN_FIELD_NAMES) {
                assertThat(fields).doesNotContain(forbidden);
            }
        }
    }

    // ── D. Logging / Sanitization ────────────────────────────────────

    @Nested
    @DisplayName("D. Logging / Sanitization")
    class LoggingSanitization {

        @Test
        @DisplayName("ProviderOutputReference.toString() does not leak URL")
        void toString_doesNotLeakUrl() {
            ImageProviderGenerationResult result = ImageProviderGenerationResult.builder()
                    .success(true)
                    .providerAlias("OPENAI")
                    .build();

            ProviderOutputReference ref = new ProviderOutputReference(
                    result, "https://secret.openai.com/image/sensitive-token-xyz");

            String str = ref.toString();
            assertThat(str).doesNotContain("secret.openai.com");
            assertThat(str).doesNotContain("sensitive-token-xyz");
            assertThat(str).doesNotContain("https://");
            assertThat(str).contains("hasProviderOutputUrl=true");
        }

        @Test
        @DisplayName("ProviderOutputReference.toString() shows false when URL is null")
        void toString_showsFalseWhenUrlNull() {
            ProviderOutputReference ref = new ProviderOutputReference(
                    ImageProviderGenerationResult.builder().build(), null);

            assertThat(ref.toString()).contains("hasProviderOutputUrl=false");
        }

        @Test
        @DisplayName("API key failure error message contains no provider URL or key")
        void apiKeyFailure_safeErrorMessage() {
            OpenAiImageGenerationProviderGateway gateway =
                    new OpenAiImageGenerationProviderGateway(config, "", RestClient.create());

            ProviderOutputReference ref = gateway.generateWithOutputReference(createRequest());

            assertThat(ref.generationResult().isSuccess()).isFalse();
            assertThat(ref.providerOutputUrl()).isNull();
            String msg = ref.generationResult().getErrorMessage();
            assertThat(msg).doesNotContain("https://");
            assertThat(msg).doesNotContainIgnoringCase("apiKey");
            assertThat(msg).doesNotContainIgnoringCase("secret");
        }
    }

    // ── E. No Storage/Review/Publish Side Effects ────────────────────

    @Nested
    @DisplayName("E. No Side Effects")
    class NoSideEffects {

        @Test
        @DisplayName("ProviderOutputReference does not contain approval/publish fields")
        void reference_noApprovalFields() {
            Set<String> fields = Arrays.stream(ProviderOutputReference.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            // Record components only: generationResult, providerOutputUrl
            assertThat(fields).containsExactlyInAnyOrder("generationResult", "providerOutputUrl");
        }

        @Test
        @DisplayName("ProviderOutputReference is a record (not entity)")
        void reference_isRecord() {
            assertThat(ProviderOutputReference.class.isRecord()).isTrue();
        }

        @Test
        @DisplayName("URL extraction does not change result status model")
        void urlExtraction_noStatusChange() {
            RestClient.Builder builder = RestClient.builder().baseUrl(OPENAI_URL);
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

            server.expect(requestTo(OPENAI_URL))
                    .andRespond(withSuccess(
                            "{\"data\":[{\"url\":\"https://oai.example.com/img.png\"}]}",
                            MediaType.APPLICATION_JSON));

            OpenAiImageGenerationProviderGateway gateway =
                    new OpenAiImageGenerationProviderGateway(config, "test-key", builder.build());

            ProviderOutputReference ref = gateway.generateWithOutputReference(createRequest());
            ImageProviderGenerationResult r = ref.generationResult();

            // No approval/storage/publish fields set
            String rStr = r.toString();
            assertThat(rStr).doesNotContain("APPROVED");
            assertThat(rStr).doesNotContain("READY_FOR_PUBLISH");
            assertThat(rStr).doesNotContain("COMPLETED");
            assertThat(rStr).doesNotContain("STORED");

            server.verify();
        }
    }

    private static Set<String> getFieldNames(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
    }
}
