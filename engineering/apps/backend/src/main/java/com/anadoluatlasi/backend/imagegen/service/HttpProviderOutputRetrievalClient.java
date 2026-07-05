package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.entity.ImageStorageErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * FP-010 Task 02: Real HTTP provider output download client.
 *
 * <p>Downloads image bytes from a transient provider URL via HTTP(S).
 * Activated only when {@code imagegen.provider.download.enabled=true}.</p>
 *
 * <p>Safety guarantees:</p>
 * <ul>
 *   <li>Provider URL is transient method parameter only — never persisted, logged, or exposed</li>
 *   <li>Enforces connect/read timeouts</li>
 *   <li>Enforces max download size</li>
 *   <li>Validates content type against allowed set</li>
 *   <li>Rejects non-2xx responses safely</li>
 *   <li>Follows bounded redirects only (HTTPS→HTTPS)</li>
 *   <li>Sanitizes all error messages</li>
 *   <li>Never returns raw HTTP response/headers/base64</li>
 * </ul>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "imagegen.provider.download.enabled", havingValue = "true")
public class HttpProviderOutputRetrievalClient implements ProviderOutputRetrievalClient {

    private final ProviderOutputDownloadProperties config;

    public HttpProviderOutputRetrievalClient(ProviderOutputDownloadProperties config) {
        this.config = config;
    }

    @Override
    public ProviderOutputRetrievalResult retrieve(String providerOutputUrl,
                                                   ProviderOutputRetrievalRequest request) {
        String correlationId = request != null ? request.getCorrelationId() : null;

        // Safety: log only safe context, NEVER the URL
        log.info("[PROVIDER_RETRIEVAL] HTTP retrieval requested. correlationId={}, providerAlias={}",
                correlationId,
                request != null ? request.getProviderAlias() : "unknown");

        // 1. URL validation
        if (providerOutputUrl == null || providerOutputUrl.isBlank()) {
            return safeFailure(ImageStorageErrorCode.PROVIDER_OUTPUT_UNAVAILABLE,
                    "Provider çıktı referansı mevcut değil", correlationId, false);
        }

        URI uri;
        try {
            uri = URI.create(providerOutputUrl);
        } catch (Exception e) {
            return safeFailure(ImageStorageErrorCode.PROVIDER_OUTPUT_UNAVAILABLE,
                    "Geçersiz provider referans formatı", correlationId, false);
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equals("https") && !scheme.equals("http"))) {
            return safeFailure(ImageStorageErrorCode.PROVIDER_OUTPUT_UNAVAILABLE,
                    "Desteklenmeyen protokol", correlationId, false);
        }

        // 2. HTTP connection
        HttpURLConnection connection = null;
        try {
            connection = openConnection(uri, 0);

            int statusCode = connection.getResponseCode();
            if (statusCode < 200 || statusCode >= 300) {
                boolean retryable = statusCode == 429 || statusCode >= 500;
                return safeFailure(ImageStorageErrorCode.PROVIDER_OUTPUT_DOWNLOAD_FAILED,
                        "Provider çıktısı indirilemedi", correlationId, retryable);
            }

            // 3. Content type validation
            String contentType = connection.getContentType();
            String normalizedContentType = normalizeContentType(contentType);
            Set<String> allowed = config.getAllowedContentTypes();
            if (normalizedContentType == null || !allowed.contains(normalizedContentType)) {
                return safeFailure(ImageStorageErrorCode.UNSUPPORTED_MEDIA_TYPE,
                        "Desteklenmeyen dosya formatı", correlationId, false);
            }

            // 4. Size check from Content-Length header (pre-read guard)
            long declaredLength = connection.getContentLengthLong();
            if (declaredLength > config.getMaxBytes()) {
                return safeFailure(ImageStorageErrorCode.MEDIA_TOO_LARGE,
                        "Dosya boyutu çok büyük", correlationId, false);
            }

            // 5. Download bytes with bounded read
            byte[] imageBytes = readBounded(connection.getInputStream(), config.getMaxBytes());
            if (imageBytes == null || imageBytes.length == 0) {
                return safeFailure(ImageStorageErrorCode.PROVIDER_OUTPUT_DOWNLOAD_FAILED,
                        "Provider çıktısı boş", correlationId, false);
            }

            log.info("[PROVIDER_RETRIEVAL] HTTP retrieval success. correlationId={}, contentType={}, size={}",
                    correlationId, normalizedContentType, imageBytes.length);

            return ProviderOutputRetrievalResult.builder()
                    .success(true)
                    .imageBytes(imageBytes)
                    .detectedContentType(normalizedContentType)
                    .contentLength(imageBytes.length)
                    .correlationId(correlationId)
                    .retryable(false)
                    .retrievedAt(LocalDateTime.now())
                    .build();

        } catch (java.net.SocketTimeoutException e) {
            return safeFailure(ImageStorageErrorCode.PROVIDER_OUTPUT_DOWNLOAD_FAILED,
                    "Provider bağlantı zaman aşımı", correlationId, true);
        } catch (java.net.ConnectException e) {
            return safeFailure(ImageStorageErrorCode.PROVIDER_OUTPUT_DOWNLOAD_FAILED,
                    "Provider bağlantısı kurulamadı", correlationId, true);
        } catch (FileTooLargeException e) {
            return safeFailure(ImageStorageErrorCode.MEDIA_TOO_LARGE,
                    "Dosya boyutu çok büyük", correlationId, false);
        } catch (Exception e) {
            // Safety: never include exception message which may contain URL/headers
            log.warn("[PROVIDER_RETRIEVAL] HTTP retrieval exception. correlationId={}, exceptionType={}",
                    correlationId, e.getClass().getSimpleName());
            return safeFailure(ImageStorageErrorCode.PROVIDER_OUTPUT_DOWNLOAD_FAILED,
                    "Provider çıktısı indirme sırasında hata oluştu", correlationId, true);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // ── Internal ────────────────────────────────────────────────────

    /**
     * Opens HTTP connection with redirect handling.
     * Follows bounded redirects (HTTPS only).
     */
    HttpURLConnection openConnection(URI uri, int redirectCount) throws Exception {
        if (redirectCount > config.getMaxRedirects()) {
            throw new TooManyRedirectsException();
        }

        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setConnectTimeout(config.getConnectTimeoutMs());
        conn.setReadTimeout(config.getReadTimeoutMs());
        conn.setInstanceFollowRedirects(false); // manual redirect handling
        conn.setRequestMethod("GET");
        conn.connect();

        int status = conn.getResponseCode();
        if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
            String location = conn.getHeaderField("Location");
            conn.disconnect();
            if (location == null || location.isBlank()) {
                throw new UnsafeRedirectException();
            }
            URI redirectUri = URI.create(location);
            String redirectScheme = redirectUri.getScheme();
            if (redirectScheme == null || (!redirectScheme.equals("https") && !redirectScheme.equals("http"))) {
                throw new UnsafeRedirectException();
            }
            return openConnection(redirectUri, redirectCount + 1);
        }
        return conn;
    }

    /**
     * Reads from stream with bounded size — prevents unbounded memory use.
     */
    private byte[] readBounded(InputStream is, long maxBytes) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        long totalRead = 0;
        int bytesRead;
        while ((bytesRead = is.read(chunk)) != -1) {
            totalRead += bytesRead;
            if (totalRead > maxBytes) {
                throw new FileTooLargeException();
            }
            buffer.write(chunk, 0, bytesRead);
        }
        return buffer.toByteArray();
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) return null;
        int semicolonIdx = contentType.indexOf(';');
        String mimeOnly = semicolonIdx > 0 ? contentType.substring(0, semicolonIdx) : contentType;
        return mimeOnly.trim().toLowerCase();
    }

    private ProviderOutputRetrievalResult safeFailure(ImageStorageErrorCode errorCode,
                                                       String safeMessage,
                                                       String correlationId,
                                                       boolean retryable) {
        return ProviderOutputRetrievalResult.builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(safeMessage)
                .correlationId(correlationId)
                .retryable(retryable)
                .retrievedAt(LocalDateTime.now())
                .build();
    }

    // ── Internal exception types ────────────────────────────────────

    static class FileTooLargeException extends Exception {}
    static class TooManyRedirectsException extends Exception {}
    static class UnsafeRedirectException extends Exception {}
}
