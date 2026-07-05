package com.anadoluatlasi.backend.imagegen.storage;

/**
 * FP-010 Task 03: Internal S3 client abstraction for testability.
 *
 * <p>Provides a thin layer over S3/MinIO SDK operations.
 * Production implementation uses real S3 SDK; tests use mocked instance.</p>
 *
 * <p>This interface is backend-internal only — never exposed in DTO/API.</p>
 *
 * <p>Security rules:</p>
 * <ul>
 *   <li>No public URL generation</li>
 *   <li>No signed URL generation</li>
 *   <li>Raw SDK responses are never exposed beyond this boundary</li>
 *   <li>Bucket/key details are internal only</li>
 * </ul>
 */
public interface S3StorageClient {

    /**
     * Put an object into private storage.
     *
     * @param bucket      target bucket name (internal)
     * @param objectKey   object key (internal, never exposed)
     * @param bytes       validated image bytes
     * @param contentType MIME type
     * @throws S3StorageClientException on any failure
     */
    void putObject(String bucket, String objectKey, byte[] bytes, String contentType)
            throws S3StorageClientException;

    /**
     * Get an object from private storage.
     *
     * @param bucket    source bucket name (internal)
     * @param objectKey object key (internal, never exposed)
     * @return object bytes
     * @throws S3StorageClientException on any failure
     */
    byte[] getObject(String bucket, String objectKey) throws S3StorageClientException;

    /**
     * Check if an object exists in storage.
     *
     * @param bucket    bucket name (internal)
     * @param objectKey object key (internal)
     * @return true if exists
     * @throws S3StorageClientException on any failure
     */
    boolean objectExists(String bucket, String objectKey) throws S3StorageClientException;

    /**
     * Safe storage exception — must never contain bucket/key/credential details.
     */
    class S3StorageClientException extends Exception {
        private final String safeMessage;

        public S3StorageClientException(String safeMessage) {
            super(safeMessage);
            this.safeMessage = safeMessage;
        }

        public S3StorageClientException(String safeMessage, Throwable cause) {
            super(safeMessage, cause);
            this.safeMessage = safeMessage;
        }

        public String getSafeMessage() {
            return safeMessage;
        }

        /**
         * Override toString to prevent raw exception chain from leaking
         * bucket/key/credential details.
         */
        @Override
        public String toString() {
            return "S3StorageClientException{safeMessage='" + safeMessage + "'}";
        }
    }
}
