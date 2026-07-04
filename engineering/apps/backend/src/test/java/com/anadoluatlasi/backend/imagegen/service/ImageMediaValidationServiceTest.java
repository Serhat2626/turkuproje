package com.anadoluatlasi.backend.imagegen.service;

import com.anadoluatlasi.backend.imagegen.entity.ImageStorageErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-008 Task 06: ImageMediaValidationService unit tests.
 *
 * <p>In-memory byte array kullanır — external download/storage yok.</p>
 */
class ImageMediaValidationServiceTest {

    private ImageMediaValidationService service;
    private static final String CORRELATION = "test-corr-001";

    @BeforeEach
    void setUp() {
        service = new ImageMediaValidationService();
    }

    // ── Helper: Create in-memory test images ────────────────────────

    private byte[] createPngBytes(int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    private byte[] createJpegBytes(int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        return baos.toByteArray();
    }

    // ── Valid Image Tests ───────────────────────────────────────────

    @Nested
    @DisplayName("Valid images")
    class ValidImages {

        @Test
        @DisplayName("Valid PNG accepted")
        void validPngAccepted() throws IOException {
            byte[] png = createPngBytes(100, 50);
            ImageMediaValidationResult result = service.validate(png, CORRELATION);

            assertThat(result.isValid()).isTrue();
            assertThat(result.getDetectedMimeType()).isEqualTo("image/png");
            assertThat(result.getFileSizeBytes()).isEqualTo(png.length);
            assertThat(result.getWidth()).isEqualTo(100);
            assertThat(result.getHeight()).isEqualTo(50);
            assertThat(result.getChecksumSha256()).isNotNull().hasSize(64);
            assertThat(result.getErrorCode()).isNull();
            assertThat(result.getErrorMessage()).isNull();
            assertThat(result.getValidatedAt()).isNotNull();
            assertThat(result.getCorrelationId()).isEqualTo(CORRELATION);
        }

        @Test
        @DisplayName("Valid JPEG accepted")
        void validJpegAccepted() throws IOException {
            byte[] jpeg = createJpegBytes(200, 150);
            ImageMediaValidationResult result = service.validate(jpeg, CORRELATION);

            assertThat(result.isValid()).isTrue();
            assertThat(result.getDetectedMimeType()).isEqualTo("image/jpeg");
            assertThat(result.getWidth()).isEqualTo(200);
            assertThat(result.getHeight()).isEqualTo(150);
            assertThat(result.getChecksumSha256()).isNotNull().hasSize(64);
        }

        @Test
        @DisplayName("Null correlationId accepted")
        void nullCorrelationIdAccepted() throws IOException {
            byte[] png = createPngBytes(10, 10);
            ImageMediaValidationResult result = service.validate(png, null);

            assertThat(result.isValid()).isTrue();
            assertThat(result.getCorrelationId()).isNull();
        }
    }

    // ── Rejection Tests ─────────────────────────────────────────────

    @Nested
    @DisplayName("Rejected inputs")
    class RejectedInputs {

        @Test
        @DisplayName("Null bytes rejected")
        void nullBytesRejected() {
            ImageMediaValidationResult result = service.validate(null, CORRELATION);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.MEDIA_VALIDATION_FAILED);
            assertThat(result.getErrorMessage()).isNotNull();
        }

        @Test
        @DisplayName("Empty bytes rejected")
        void emptyBytesRejected() {
            ImageMediaValidationResult result = service.validate(new byte[0], CORRELATION);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.MEDIA_VALIDATION_FAILED);
        }

        @Test
        @DisplayName("Random corrupt bytes rejected")
        void corruptBytesRejected() {
            byte[] corrupt = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
                    0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F};
            ImageMediaValidationResult result = service.validate(corrupt, CORRELATION);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.UNSUPPORTED_MEDIA_TYPE);
        }

        @Test
        @DisplayName("Oversized bytes rejected")
        void oversizedBytesRejected() {
            byte[] oversized = new byte[(int) (ImageMediaValidationService.MAX_FILE_SIZE_BYTES + 1)];
            // Add PNG magic so it passes MIME check first — but size fails before that
            System.arraycopy(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
                    0, oversized, 0, 8);

            ImageMediaValidationResult result = service.validate(oversized, CORRELATION);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.MEDIA_TOO_LARGE);
        }

        @Test
        @DisplayName("Unsupported MIME type rejected — GIF")
        void gifMimeRejected() {
            // GIF magic: GIF89a
            byte[] gif = new byte[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61,
                    0x01, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00};
            ImageMediaValidationResult result = service.validate(gif, CORRELATION);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(ImageStorageErrorCode.UNSUPPORTED_MEDIA_TYPE);
        }

        @Test
        @DisplayName("PNG magic but corrupt content rejected")
        void pngMagicCorruptContentRejected() {
            // PNG magic header followed by garbage
            byte[] corruptPng = new byte[100];
            System.arraycopy(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
                    0, corruptPng, 0, 8);

            ImageMediaValidationResult result = service.validate(corruptPng, CORRELATION);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorCode()).isIn(
                    ImageStorageErrorCode.MEDIA_DIMENSION_INVALID,
                    ImageStorageErrorCode.MEDIA_VALIDATION_FAILED
            );
        }
    }

    // ── MIME Detection ──────────────────────────────────────────────

    @Nested
    @DisplayName("MIME detection")
    class MimeDetection {

        @Test
        @DisplayName("PNG magic bytes detected")
        void pngMagicDetected() throws IOException {
            byte[] png = createPngBytes(10, 10);
            assertThat(service.detectMimeType(png)).isEqualTo("image/png");
        }

        @Test
        @DisplayName("JPEG magic bytes detected")
        void jpegMagicDetected() throws IOException {
            byte[] jpeg = createJpegBytes(10, 10);
            assertThat(service.detectMimeType(jpeg)).isEqualTo("image/jpeg");
        }

        @Test
        @DisplayName("Too short data returns null")
        void tooShortDataReturnsNull() {
            assertThat(service.detectMimeType(new byte[]{0x01, 0x02})).isNull();
        }

        @Test
        @DisplayName("Null data returns null")
        void nullDataReturnsNull() {
            assertThat(service.detectMimeType(null)).isNull();
        }
    }

    // ── Checksum ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Checksum calculation")
    class Checksum {

        @Test
        @DisplayName("SHA-256 checksum is 64 hex chars")
        void checksumLength() throws IOException {
            byte[] png = createPngBytes(10, 10);
            ImageMediaValidationResult result = service.validate(png, CORRELATION);

            assertThat(result.getChecksumSha256()).hasSize(64);
            assertThat(result.getChecksumSha256()).matches("[0-9a-f]{64}");
        }

        @Test
        @DisplayName("Same content produces same checksum")
        void deterministicChecksum() throws Exception {
            byte[] data = new byte[]{1, 2, 3, 4, 5};
            String hash1 = ImageMediaValidationService.calculateSha256(data);
            String hash2 = ImageMediaValidationService.calculateSha256(data);

            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("Different content produces different checksum")
        void differentContentDifferentChecksum() throws Exception {
            String hash1 = ImageMediaValidationService.calculateSha256(new byte[]{1, 2, 3});
            String hash2 = ImageMediaValidationService.calculateSha256(new byte[]{4, 5, 6});

            assertThat(hash1).isNotEqualTo(hash2);
        }
    }

    // ── Safe Error Messages ─────────────────────────────────────────

    @Nested
    @DisplayName("Safe error messages")
    class SafeErrors {

        @Test
        @DisplayName("Error messages do not contain URLs")
        void errorMessagesNoUrls() {
            ImageMediaValidationResult result = service.validate(null, CORRELATION);
            if (result.getErrorMessage() != null) {
                assertThat(result.getErrorMessage()).doesNotContainIgnoringCase("http");
                assertThat(result.getErrorMessage()).doesNotContainIgnoringCase("://");
            }
        }

        @Test
        @DisplayName("Error messages do not contain storage keys")
        void errorMessagesNoStorageKeys() {
            ImageMediaValidationResult result = service.validate(new byte[0], CORRELATION);
            if (result.getErrorMessage() != null) {
                assertThat(result.getErrorMessage()).doesNotContainIgnoringCase("s3://");
                assertThat(result.getErrorMessage()).doesNotContainIgnoringCase("gs://");
                assertThat(result.getErrorMessage()).doesNotContainIgnoringCase("storageKey");
                assertThat(result.getErrorMessage()).doesNotContainIgnoringCase("objectKey");
            }
        }
    }

    // ── Result Model Safety ─────────────────────────────────────────

    @Nested
    @DisplayName("Result model safety")
    class ResultModelSafety {

        private static final Set<String> FORBIDDEN_FIELDS = Set.of(
                "providerUrl", "providerImageUrl", "providerTemporaryUrl",
                "signedUrl", "publicUrl", "cdnUrl",
                "base64", "imageBase64", "imageBinary", "rawBytes",
                "storageObjectKey", "storageBucket", "storageProvider",
                "rawProviderPayload", "rawProviderResponse",
                "rawStorageProviderResponse",
                "apiKey", "secret", "internalFullPrompt",
                "readyForPublish", "publishStatus", "exportStatus"
        );

        @Test
        @DisplayName("Result model has no forbidden fields")
        void resultModelNoForbiddenFields() {
            Set<String> fields = Arrays.stream(ImageMediaValidationResult.class.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.toSet());

            for (String forbidden : FORBIDDEN_FIELDS) {
                assertThat(fields)
                        .as("ImageMediaValidationResult should not have '%s'", forbidden)
                        .doesNotContain(forbidden);
            }
        }
    }

    // ── MIME Allowlist ───────────────────────────────────────────────

    @Nested
    @DisplayName("MIME allowlist config")
    class MimeAllowlist {

        @Test
        @DisplayName("Allowlist contains expected types")
        void allowlistContainsExpected() {
            assertThat(ImageMediaValidationService.ALLOWED_MIME_TYPES)
                    .containsExactlyInAnyOrder("image/png", "image/jpeg", "image/webp");
        }

        @Test
        @DisplayName("Allowlist does not contain video/audio")
        void allowlistNoVideoAudio() {
            assertThat(ImageMediaValidationService.ALLOWED_MIME_TYPES)
                    .noneMatch(t -> t.startsWith("video/"))
                    .noneMatch(t -> t.startsWith("audio/"));
        }
    }
}
