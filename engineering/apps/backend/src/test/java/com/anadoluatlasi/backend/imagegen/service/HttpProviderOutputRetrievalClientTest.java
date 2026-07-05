package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.entity.ImageStorageErrorCode;
import com.anadoluatlasi.backend.imagegen.storage.S3PrivateStorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * FP-010 Task 02 — Real Provider Download Client + Storage Config Hardening.
 *
 * Tests use a mocked HttpURLConnection — no real network calls.
 */
@DisplayName("FP-010 Task 02 — HTTP Download Client + Config Hardening")
class HttpProviderOutputRetrievalClientTest {

    private ProviderOutputDownloadProperties config;
    private TestableHttpClient client;

    /** Subclass that allows mocking the connection for unit testing. */
    static class TestableHttpClient extends HttpProviderOutputRetrievalClient {
        HttpURLConnection mockedConnection;

        TestableHttpClient(ProviderOutputDownloadProperties config) {
            super(config);
        }

        @Override
        HttpURLConnection openConnection(URI uri, int redirectCount) throws Exception {
            if (mockedConnection == null) {
                throw new IOException("No mocked connection set");
            }
            return mockedConnection;
        }
    }

    @BeforeEach
    void setUp() {
        config = new ProviderOutputDownloadProperties();
        config.setEnabled(true);
        config.setConnectTimeoutMs(5000);
        config.setReadTimeoutMs(30000);
        config.setMaxBytes(20 * 1024 * 1024);
        config.setMaxRedirects(3);
        config.setAllowedContentTypes(Set.of("image/png", "image/jpeg", "image/webp"));
        client = new TestableHttpClient(config);
    }

    private ProviderOutputRetrievalRequest safeRequest() {
        return ProviderOutputRetrievalRequest.builder()
                .mediaAssetId(UUID.randomUUID())
                .generatedAssetId(UUID.randomUUID())
                .providerAlias("openai")
                .correlationId("test-corr-" + UUID.randomUUID())
                .build();
    }

    private HttpURLConnection mockSuccessConnection(byte[] body, String contentType) throws Exception {
        HttpURLConnection conn = mock(HttpURLConnection.class);
        when(conn.getResponseCode()).thenReturn(200);
        when(conn.getContentType()).thenReturn(contentType);
        when(conn.getContentLengthLong()).thenReturn((long) body.length);
        when(conn.getInputStream()).thenReturn(new ByteArrayInputStream(body));
        return conn;
    }

    private byte[] validPngBytes() {
        return MockProviderOutputRetrievalClient.MOCK_PNG_BYTES.clone();
    }

    // ── A. Successful HTTP Download ─────────────────────────────────

    @Nested
    @DisplayName("A. Successful HTTP Download")
    class SuccessfulDownload {

        @Test
        @DisplayName("Valid URL + 200 + image/png → success with bytes")
        void validDownload_returnsSuccess() throws Exception {
            byte[] pngBytes = validPngBytes();
            client.mockedConnection = mockSuccessConnection(pngBytes, "image/png");

            ProviderOutputRetrievalResult result = client.retrieve(
                    "https://provider.example.com/output.png", safeRequest());

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getImageBytes()).isEqualTo(pngBytes);
            assertThat(result.getDetectedContentType()).isEqualTo("image/png");
            assertThat(result.getContentLength()).isEqualTo(pngBytes.length);
            assertThat(result.getCorrelationId()).isNotNull();
            assertThat(result.getRetrievedAt()).isNotNull();
        }

        @Test
        @DisplayName("Content type with charset suffix normalized correctly")
        void contentTypeWithCharset_normalizedCorrectly() throws Exception {
            byte[] pngBytes = validPngBytes();
            client.mockedConnection = mockSuccessConnection(pngBytes, "image/png; charset=utf-8");

            ProviderOutputRetrievalResult result = client.retrieve(
                    "https://provider.example.com/output.png", safeRequest());

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getDetectedContentType()).isEqualTo("image/png");
        }

        @Test
        @DisplayName("JPEG content type accepted")
        void jpegContentType_accepted() throws Exception {
            byte[] pngBytes = validPngBytes();
            client.mockedConnection = mockSuccessConnection(pngBytes, "image/jpeg");

            ProviderOutputRetrievalResult result = client.retrieve(
                    "https://provider.example.com/output.jpg", safeRequest());

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getDetectedContentType()).isEqualTo("image/jpeg");
        }

        @Test
        @DisplayName("WebP content type accepted")
        void webpContentType_accepted() throws Exception {
            byte[] pngBytes = validPngBytes();
            client.mockedConnection = mockSuccessConnection(pngBytes, "image/webp");

            ProviderOutputRetrievalResult result = client.retrieve(
                    "https://provider.example.com/output.webp", safeRequest());

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getDetectedContentType()).isEqualTo("image/webp");
        }

        @Test
        @DisplayName("Result has no URL/storage key/base64/raw payload fields")
        void result_hasNoForbiddenFields() throws Exception {
            client.mockedConnection = mockSuccessConnection(validPngBytes(), "image/png");

            ProviderOutputRetrievalResult result = client.retrieve(
                    "https://provider.example.com/output.png", safeRequest());

            // result toString should not contain any URL
            String str = result.toString();
            assertThat(str).doesNotContain("https://");
            assertThat(str).doesNotContain("provider.example.com");
        }
    }

    // ── B. URL Safety ───────────────────────────────────────────────

    @Nested
    @DisplayName("B. URL Safety")
    class UrlSafety {

        @Test
        @DisplayName("Null URL → safe failure")
        void nullUrl_safeFailure() {
            ProviderOutputRetrievalResult result = client.retrieve(null, safeRequest());
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.PROVIDER_OUTPUT_UNAVAILABLE);
            assertThat(result.getErrorMessage()).doesNotContain("null");
        }

        @Test
        @DisplayName("Blank URL → safe failure")
        void blankUrl_safeFailure() {
            ProviderOutputRetrievalResult result = client.retrieve("   ", safeRequest());
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.PROVIDER_OUTPUT_UNAVAILABLE);
        }

        @Test
        @DisplayName("Invalid URL → safe failure")
        void invalidUrl_safeFailure() {
            ProviderOutputRetrievalResult result = client.retrieve("not a valid url %%", safeRequest());
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.PROVIDER_OUTPUT_UNAVAILABLE);
        }

        @Test
        @DisplayName("FTP scheme rejected")
        void ftpScheme_rejected() {
            ProviderOutputRetrievalResult result = client.retrieve("ftp://evil.com/file.png", safeRequest());
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.PROVIDER_OUTPUT_UNAVAILABLE);
        }

        @Test
        @DisplayName("Provider URL never appears in error message")
        void providerUrl_neverInErrorMessage() {
            String secretUrl = "https://oaidalleapiprodscus.blob.core.windows.net/secret-image-123.png";
            ProviderOutputRetrievalResult result = client.retrieve(secretUrl, safeRequest());
            // Will fail because no mocked connection, but error should be safe
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).doesNotContain("oaidalleapiprodscus");
            assertThat(result.getErrorMessage()).doesNotContain("secret-image");
            assertThat(result.getErrorMessage()).doesNotContain("https://");
        }

        @Test
        @DisplayName("Provider URL not in result toString")
        void providerUrl_notInResultToString() {
            ProviderOutputRetrievalResult result = client.retrieve(
                    "https://secret-provider.example.com/img.png", safeRequest());
            String str = result.toString();
            assertThat(str).doesNotContain("secret-provider");
            assertThat(str).doesNotContain("https://");
        }
    }

    // ── C. HTTP Failure Safety ──────────────────────────────────────

    @Nested
    @DisplayName("C. HTTP Failure Safety")
    class HttpFailureSafety {

        @Test
        @DisplayName("404 response → safe failure, not retryable")
        void notFound_safeFailure() throws Exception {
            HttpURLConnection conn = mock(HttpURLConnection.class);
            when(conn.getResponseCode()).thenReturn(404);
            client.mockedConnection = conn;

            ProviderOutputRetrievalResult result = client.retrieve(
                    "https://provider.example.com/output.png", safeRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.PROVIDER_OUTPUT_DOWNLOAD_FAILED);
            assertThat(result.isRetryable()).isFalse();
        }

        @Test
        @DisplayName("500 response → safe failure, retryable")
        void serverError_retryable() throws Exception {
            HttpURLConnection conn = mock(HttpURLConnection.class);
            when(conn.getResponseCode()).thenReturn(500);
            client.mockedConnection = conn;

            ProviderOutputRetrievalResult result = client.retrieve(
                    "https://provider.example.com/output.png", safeRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.PROVIDER_OUTPUT_DOWNLOAD_FAILED);
            assertThat(result.isRetryable()).isTrue();
        }

        @Test
        @DisplayName("429 response → safe failure, retryable")
        void tooManyRequests_retryable() throws Exception {
            HttpURLConnection conn = mock(HttpURLConnection.class);
            when(conn.getResponseCode()).thenReturn(429);
            client.mockedConnection = conn;

            ProviderOutputRetrievalResult result = client.retrieve(
                    "https://provider.example.com/output.png", safeRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.isRetryable()).isTrue();
        }

        @Test
        @DisplayName("Timeout → safe failure, retryable")
        void timeout_safeFailure() throws Exception {
            HttpURLConnection conn = mock(HttpURLConnection.class);
            when(conn.getResponseCode()).thenThrow(new SocketTimeoutException("timeout"));
            client.mockedConnection = conn;

            ProviderOutputRetrievalResult result = client.retrieve(
                    "https://provider.example.com/output.png", safeRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.PROVIDER_OUTPUT_DOWNLOAD_FAILED);
            assertThat(result.isRetryable()).isTrue();
            assertThat(result.getErrorMessage()).doesNotContain("timeout");
        }

        @Test
        @DisplayName("Connection failure → safe failure")
        void connectionFailure_safeFailure() throws Exception {
            HttpURLConnection conn = mock(HttpURLConnection.class);
            when(conn.getResponseCode()).thenThrow(new java.net.ConnectException("Connection refused"));
            client.mockedConnection = conn;

            ProviderOutputRetrievalResult result = client.retrieve(
                    "https://provider.example.com/output.png", safeRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.PROVIDER_OUTPUT_DOWNLOAD_FAILED);
            assertThat(result.getErrorMessage()).doesNotContain("Connection refused");
        }

        @Test
        @DisplayName("Error messages never contain headers or raw response")
        void errorMessages_neverContainRawDetails() throws Exception {
            HttpURLConnection conn = mock(HttpURLConnection.class);
            when(conn.getResponseCode()).thenThrow(
                    new IOException("unexpected end of stream on http://secret.com/path"));
            client.mockedConnection = conn;

            ProviderOutputRetrievalResult result = client.retrieve(
                    "https://provider.example.com/output.png", safeRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).doesNotContain("secret.com");
            assertThat(result.getErrorMessage()).doesNotContain("http://");
        }
    }

    // ── D. Content Safety ───────────────────────────────────────────

    @Nested
    @DisplayName("D. Content Safety")
    class ContentSafety {

        @Test
        @DisplayName("Unsupported content type → safe failure")
        void unsupportedContentType_rejected() throws Exception {
            HttpURLConnection conn = mock(HttpURLConnection.class);
            when(conn.getResponseCode()).thenReturn(200);
            when(conn.getContentType()).thenReturn("application/json");
            when(conn.getContentLengthLong()).thenReturn(100L);
            client.mockedConnection = conn;

            ProviderOutputRetrievalResult result = client.retrieve(
                    "https://provider.example.com/output.png", safeRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.UNSUPPORTED_MEDIA_TYPE);
        }

        @Test
        @DisplayName("Missing content type → safe failure")
        void missingContentType_rejected() throws Exception {
            HttpURLConnection conn = mock(HttpURLConnection.class);
            when(conn.getResponseCode()).thenReturn(200);
            when(conn.getContentType()).thenReturn(null);
            when(conn.getContentLengthLong()).thenReturn(100L);
            client.mockedConnection = conn;

            ProviderOutputRetrievalResult result = client.retrieve(
                    "https://provider.example.com/output.png", safeRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.UNSUPPORTED_MEDIA_TYPE);
        }

        @Test
        @DisplayName("Empty body → safe failure")
        void emptyBody_rejected() throws Exception {
            HttpURLConnection conn = mock(HttpURLConnection.class);
            when(conn.getResponseCode()).thenReturn(200);
            when(conn.getContentType()).thenReturn("image/png");
            when(conn.getContentLengthLong()).thenReturn(0L);
            when(conn.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
            client.mockedConnection = conn;

            ProviderOutputRetrievalResult result = client.retrieve(
                    "https://provider.example.com/output.png", safeRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.PROVIDER_OUTPUT_DOWNLOAD_FAILED);
        }

        @Test
        @DisplayName("Oversized Content-Length → safe failure before read")
        void oversizedContentLength_rejectedBeforeRead() throws Exception {
            HttpURLConnection conn = mock(HttpURLConnection.class);
            when(conn.getResponseCode()).thenReturn(200);
            when(conn.getContentType()).thenReturn("image/png");
            when(conn.getContentLengthLong()).thenReturn(100 * 1024 * 1024L); // 100MB
            client.mockedConnection = conn;

            ProviderOutputRetrievalResult result = client.retrieve(
                    "https://provider.example.com/output.png", safeRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.MEDIA_TOO_LARGE);
        }

        @Test
        @DisplayName("Oversized body during read → safe failure (stream limit)")
        void oversizedBodyDuringRead_rejected() throws Exception {
            config.setMaxBytes(100); // 100 bytes max
            byte[] bigBody = new byte[200]; // 200 bytes

            HttpURLConnection conn = mock(HttpURLConnection.class);
            when(conn.getResponseCode()).thenReturn(200);
            when(conn.getContentType()).thenReturn("image/png");
            when(conn.getContentLengthLong()).thenReturn(-1L); // unknown length
            when(conn.getInputStream()).thenReturn(new ByteArrayInputStream(bigBody));
            client.mockedConnection = conn;

            ProviderOutputRetrievalResult result = client.retrieve(
                    "https://provider.example.com/output.png", safeRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.MEDIA_TOO_LARGE);
        }

        @Test
        @DisplayName("text/html content type rejected")
        void htmlContentType_rejected() throws Exception {
            HttpURLConnection conn = mock(HttpURLConnection.class);
            when(conn.getResponseCode()).thenReturn(200);
            when(conn.getContentType()).thenReturn("text/html");
            when(conn.getContentLengthLong()).thenReturn(100L);
            client.mockedConnection = conn;

            ProviderOutputRetrievalResult result = client.retrieve(
                    "https://provider.example.com/output.png", safeRequest());

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.UNSUPPORTED_MEDIA_TYPE);
        }
    }

    // ── E. Config Hardening ─────────────────────────────────────────

    @Nested
    @DisplayName("E. S3 Config Hardening")
    class S3ConfigHardening {

        @Test
        @DisplayName("S3Properties.toString() excludes accessKey and secretKey")
        void toString_excludesSecrets() {
            S3PrivateStorageProperties props = new S3PrivateStorageProperties();
            props.setAccessKey("AKIAIOSFODNN7EXAMPLE");
            props.setSecretKey("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
            props.setEndpoint("https://s3.amazonaws.com");
            props.setBucket("my-private-bucket");
            props.setRegion("eu-west-1");

            String str = props.toString();
            assertThat(str).doesNotContain("AKIAIOSFODNN7EXAMPLE");
            assertThat(str).doesNotContain("wJalrXUtnFEMI");
            assertThat(str).doesNotContain("EXAMPLEKEY");
            assertThat(str).contains("my-private-bucket");
        }

        @Test
        @DisplayName("isEnabledAndConfigured returns true when fully configured")
        void fullyConfigured_returnsTrue() {
            S3PrivateStorageProperties props = new S3PrivateStorageProperties();
            props.setEnabled(true);
            props.setBucket("test-bucket");
            props.setEndpoint("https://s3.amazonaws.com");
            props.setRegion("eu-west-1");

            assertThat(props.isEnabledAndConfigured()).isTrue();
        }

        @Test
        @DisplayName("isEnabledAndConfigured returns false when disabled")
        void disabled_returnsFalse() {
            S3PrivateStorageProperties props = new S3PrivateStorageProperties();
            props.setEnabled(false);
            assertThat(props.isEnabledAndConfigured()).isFalse();
        }

        @Test
        @DisplayName("isEnabledAndConfigured returns false when bucket missing")
        void missingBucket_returnsFalse() {
            S3PrivateStorageProperties props = new S3PrivateStorageProperties();
            props.setEnabled(true);
            props.setEndpoint("https://s3.amazonaws.com");
            props.setRegion("eu-west-1");
            // bucket empty by default
            assertThat(props.isEnabledAndConfigured()).isFalse();
        }

        @Test
        @DisplayName("validateForProduction returns null when fully valid")
        void fullyValid_returnsNull() {
            S3PrivateStorageProperties props = new S3PrivateStorageProperties();
            props.setEnabled(true);
            props.setBucket("test-bucket");
            props.setEndpoint("https://s3.amazonaws.com");
            props.setRegion("eu-west-1");
            props.setAccessKey("AKIAFAKE");
            props.setSecretKey("fakesecret");

            assertThat(props.validateForProduction()).isNull();
        }

        @Test
        @DisplayName("validateForProduction returns safe message when disabled")
        void disabled_returnsSafeMessage() {
            S3PrivateStorageProperties props = new S3PrivateStorageProperties();
            props.setEnabled(false);

            String msg = props.validateForProduction();
            assertThat(msg).isNotNull();
            assertThat(msg).doesNotContain("accessKey");
            assertThat(msg).doesNotContain("secretKey");
        }

        @Test
        @DisplayName("validateForProduction error never contains secrets")
        void validationError_neverContainsSecrets() {
            S3PrivateStorageProperties props = new S3PrivateStorageProperties();
            props.setEnabled(true);
            props.setBucket("test");
            props.setEndpoint("https://s3.example.com");
            props.setRegion("us-east-1");
            // accessKey blank

            String msg = props.validateForProduction();
            assertThat(msg).isNotNull();
            assertThat(msg).doesNotContain("AKIA");
            assertThat(msg).contains("credentials");
        }

        @Test
        @DisplayName("Default S3Properties — disabled and safe")
        void defaults_disabledAndSafe() {
            S3PrivateStorageProperties props = new S3PrivateStorageProperties();
            assertThat(props.isEnabled()).isFalse();
            assertThat(props.isEnabledAndConfigured()).isFalse();
            assertThat(props.validateForProduction()).isNotNull();
        }
    }

    // ── F. Download Config Properties ───────────────────────────────

    @Nested
    @DisplayName("F. Download Config Properties")
    class DownloadConfigProperties {

        @Test
        @DisplayName("Default config values are safe")
        void defaults_areSafe() {
            ProviderOutputDownloadProperties p = new ProviderOutputDownloadProperties();
            assertThat(p.isEnabled()).isFalse();
            assertThat(p.getConnectTimeoutMs()).isGreaterThan(0);
            assertThat(p.getReadTimeoutMs()).isGreaterThan(0);
            assertThat(p.getMaxBytes()).isGreaterThan(0);
            assertThat(p.getMaxRedirects()).isGreaterThanOrEqualTo(0);
            assertThat(p.getAllowedContentTypes()).isNotEmpty();
        }

        @Test
        @DisplayName("Config toString does not contain secrets (no secrets in this config)")
        void toString_noSecrets() {
            ProviderOutputDownloadProperties p = new ProviderOutputDownloadProperties();
            String str = p.toString();
            assertThat(str).doesNotContain("apiKey");
            assertThat(str).doesNotContain("secretKey");
            assertThat(str).doesNotContain("password");
        }

        @Test
        @DisplayName("Allowed content types are image-only")
        void allowedTypes_imageOnly() {
            ProviderOutputDownloadProperties p = new ProviderOutputDownloadProperties();
            for (String type : p.getAllowedContentTypes()) {
                assertThat(type).startsWith("image/");
            }
        }
    }

    // ── G. Contract Integration ─────────────────────────────────────

    @Nested
    @DisplayName("G. Contract Integration")
    class ContractIntegration {

        @Test
        @DisplayName("HttpProviderOutputRetrievalClient implements ProviderOutputRetrievalClient")
        void implementsContract() {
            assertThat(ProviderOutputRetrievalClient.class)
                    .isAssignableFrom(HttpProviderOutputRetrievalClient.class);
        }

        @Test
        @DisplayName("MockProviderOutputRetrievalClient still works")
        void mockClient_stillWorks() {
            MockProviderOutputRetrievalClient mock = new MockProviderOutputRetrievalClient();
            ProviderOutputRetrievalResult result = mock.retrieve(
                    "https://example.com/image.png", safeRequest());
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getImageBytes()).isNotNull();
        }

        @Test
        @DisplayName("Both clients return ProviderOutputRetrievalResult")
        void bothClients_returnSameResultType() throws Exception {
            // Mock client
            MockProviderOutputRetrievalClient mockClient = new MockProviderOutputRetrievalClient();
            ProviderOutputRetrievalResult mockResult = mockClient.retrieve(
                    "https://example.com/image.png", safeRequest());

            // HTTP client
            client.mockedConnection = mockSuccessConnection(validPngBytes(), "image/png");
            ProviderOutputRetrievalResult httpResult = client.retrieve(
                    "https://example.com/image.png", safeRequest());

            assertThat(mockResult.getClass()).isEqualTo(httpResult.getClass());
            assertThat(mockResult.isSuccess()).isTrue();
            assertThat(httpResult.isSuccess()).isTrue();
        }
    }

    // ── H. Regression ───────────────────────────────────────────────

    @Nested
    @DisplayName("H. Regression")
    class Regression {

        @Test
        @DisplayName("No READY_FOR_PUBLISH in any result")
        void noReadyForPublish() throws Exception {
            client.mockedConnection = mockSuccessConnection(validPngBytes(), "image/png");
            ProviderOutputRetrievalResult result = client.retrieve(
                    "https://provider.example.com/output.png", safeRequest());

            String str = result.toString();
            assertThat(str).doesNotContainIgnoringCase("READY_FOR_PUBLISH");
            assertThat(str).doesNotContainIgnoringCase("COMPLETED");
            assertThat(str).doesNotContainIgnoringCase("APPROVED");
        }

        @Test
        @DisplayName("No storage key in any result")
        void noStorageKey() throws Exception {
            client.mockedConnection = mockSuccessConnection(validPngBytes(), "image/png");
            ProviderOutputRetrievalResult result = client.retrieve(
                    "https://provider.example.com/output.png", safeRequest());

            assertThat(result.toString()).doesNotContain("storageObjectKey");
            assertThat(result.toString()).doesNotContain("storageBucket");
        }

        @Test
        @DisplayName("No base64 in any result")
        void noBase64() throws Exception {
            client.mockedConnection = mockSuccessConnection(validPngBytes(), "image/png");
            ProviderOutputRetrievalResult result = client.retrieve(
                    "https://provider.example.com/output.png", safeRequest());

            assertThat(result.toString()).doesNotContain("base64");
        }
    }
}
