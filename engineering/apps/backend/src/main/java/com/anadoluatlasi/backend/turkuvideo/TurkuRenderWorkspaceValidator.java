package com.anadoluatlasi.backend.turkuvideo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * FP-024 Task 03: Workspace/file reference validator.
 * No filesystem access. No real file operations.
 */
public class TurkuRenderWorkspaceValidator {

    private static final List<String> TRAVERSAL = List.of(
            "..", "/../", "\\..\\", "%2e%2e", "%2E%2E", "..\\", "../");

    private static final List<String> ABSOLUTE_UNIX = List.of(
            "/usr", "/bin", "/etc", "/tmp/", "/var", "/home", "/root", "/opt");

    private static final List<String> ABSOLUTE_WINDOWS = List.of(
            "C:\\", "D:\\", "C:/", "D:/", "\\\\");

    private static final List<String> URLS = List.of(
            "http://", "https://", "ftp://", "file://",
            "s3://", "gs://", "az://");

    private static final List<String> STORAGE = List.of(
            "storageKey", "bucketKey", "objectKey", "signedUrl",
            "publicUrl", "cdnUrl", "exportUrl", "downloadUrl");

    private static final List<String> BASE64 = List.of(
            "base64", "data:image", "data:video", "data:audio", "data:application");

    private static final List<String> SECRETS = List.of(
            "password=", "secret=", "token=", "apiKey=", "Bearer ",
            "Authorization:", "jwt=", "session=");

    private static final List<String> COMMANDS = List.of(
            "commandLine", "shellCommand", "ffmpegCommand", "processBuilder",
            "Runtime.exec", "cmd.exe", "/bin/sh", "/bin/bash");

    private static final List<String> SHELL_META = List.of(
            ";", "&&", "||", "$(", "`", "|", ">", "<", "&");

    private final TurkuRenderWorkspacePolicy policy;

    public TurkuRenderWorkspaceValidator(TurkuRenderWorkspacePolicy policy) {
        this.policy = policy != null ? policy : new TurkuRenderWorkspacePolicy();
    }

    public TurkuRenderWorkspaceValidator() {
        this(new TurkuRenderWorkspacePolicy());
    }

    // ── Validate opaque ref ──

    public TurkuRenderWorkspaceValidationResult validateOpaqueRef(String ref, String field) {
        List<TurkuRenderWorkspaceIssue> issues = new ArrayList<>();
        checkRef(ref, field, issues);
        return issues.isEmpty()
                ? TurkuRenderWorkspaceValidationResult.ok()
                : TurkuRenderWorkspaceValidationResult.fail(issues);
    }

    // ── Validate file ref with extension ──

    public TurkuRenderWorkspaceValidationResult validateFileRef(TurkuRenderSafeFileRef fileRef) {
        List<TurkuRenderWorkspaceIssue> issues = new ArrayList<>();
        if (fileRef == null) {
            issues.add(new TurkuRenderWorkspaceIssue(
                    TurkuRenderWorkspaceIssue.Code.REF_EMPTY, "fileRef", "File ref is null"));
            return TurkuRenderWorkspaceValidationResult.fail(issues);
        }
        checkRef(fileRef.getOpaqueRef(), "opaqueRef", issues);
        checkRef(fileRef.getFileId(), "fileId", issues);
        checkExtension(fileRef.getExtension(), issues);
        return issues.isEmpty()
                ? TurkuRenderWorkspaceValidationResult.ok()
                : TurkuRenderWorkspaceValidationResult.fail(issues);
    }

    // ── Validate workspace ref ──

    public TurkuRenderWorkspaceValidationResult validateWorkspaceRef(TurkuRenderWorkspaceRef wsRef) {
        List<TurkuRenderWorkspaceIssue> issues = new ArrayList<>();
        if (wsRef == null) {
            issues.add(new TurkuRenderWorkspaceIssue(
                    TurkuRenderWorkspaceIssue.Code.REF_EMPTY, "workspaceRef", "Workspace ref is null"));
            return TurkuRenderWorkspaceValidationResult.fail(issues);
        }
        checkRef(wsRef.getOpaqueRef(), "opaqueRef", issues);
        checkRef(wsRef.getWorkspaceId(), "workspaceId", issues);
        return issues.isEmpty()
                ? TurkuRenderWorkspaceValidationResult.ok()
                : TurkuRenderWorkspaceValidationResult.fail(issues);
    }

    // ── Detection methods (public for testing) ──

    public boolean isPathTraversal(String value) {
        if (value == null) return false;
        for (String p : TRAVERSAL) { if (value.contains(p)) return true; }
        return false;
    }

    public boolean isAbsolutePath(String value) {
        if (value == null) return false;
        for (String p : ABSOLUTE_UNIX) { if (value.startsWith(p)) return true; }
        for (String p : ABSOLUTE_WINDOWS) { if (value.startsWith(p)) return true; }
        return false;
    }

    public boolean isRelativePath(String value) {
        if (value == null) return false;
        return value.contains("/") || value.contains("\\");
    }

    public boolean isWindowsPath(String value) {
        if (value == null) return false;
        for (String p : ABSOLUTE_WINDOWS) { if (value.contains(p)) return true; }
        return value.matches("^[A-Za-z]:[/\\\\].*");
    }

    public boolean isUnixPath(String value) {
        if (value == null) return false;
        for (String p : ABSOLUTE_UNIX) { if (value.startsWith(p)) return true; }
        return value.startsWith("/");
    }

    public boolean isUrl(String value) {
        if (value == null) return false;
        for (String p : URLS) { if (value.contains(p)) return true; }
        return false;
    }

    public boolean isStorageKey(String value) {
        if (value == null) return false;
        for (String p : STORAGE) { if (value.contains(p)) return true; }
        return false;
    }

    public boolean isBase64(String value) {
        if (value == null) return false;
        for (String p : BASE64) { if (value.contains(p)) return true; }
        return false;
    }

    public boolean isSecret(String value) {
        if (value == null) return false;
        for (String p : SECRETS) { if (value.contains(p)) return true; }
        return false;
    }

    public boolean isCommand(String value) {
        if (value == null) return false;
        for (String p : COMMANDS) { if (value.contains(p)) return true; }
        return false;
    }

    public boolean isShellMeta(String value) {
        if (value == null) return false;
        for (String p : SHELL_META) { if (value.contains(p)) return true; }
        return false;
    }

    // ── Internal ──

    private void checkRef(String value, String field, List<TurkuRenderWorkspaceIssue> issues) {
        if (value == null || value.isBlank()) {
            issues.add(new TurkuRenderWorkspaceIssue(
                    TurkuRenderWorkspaceIssue.Code.REF_EMPTY, field, "Empty ref"));
            return;
        }
        if (isPathTraversal(value)) {
            issues.add(new TurkuRenderWorkspaceIssue(
                    TurkuRenderWorkspaceIssue.Code.PATH_TRAVERSAL, field, "Path traversal detected"));
        }
        if (isAbsolutePath(value)) {
            issues.add(new TurkuRenderWorkspaceIssue(
                    TurkuRenderWorkspaceIssue.Code.ABSOLUTE_PATH, field, "Absolute path detected"));
        }
        if (isWindowsPath(value)) {
            issues.add(new TurkuRenderWorkspaceIssue(
                    TurkuRenderWorkspaceIssue.Code.WINDOWS_PATH, field, "Windows path detected"));
        }
        if (isUrl(value)) {
            issues.add(new TurkuRenderWorkspaceIssue(
                    TurkuRenderWorkspaceIssue.Code.URL_DETECTED, field, "URL detected"));
        }
        if (isStorageKey(value)) {
            issues.add(new TurkuRenderWorkspaceIssue(
                    TurkuRenderWorkspaceIssue.Code.STORAGE_KEY_DETECTED, field, "Storage key detected"));
        }
        if (isBase64(value)) {
            issues.add(new TurkuRenderWorkspaceIssue(
                    TurkuRenderWorkspaceIssue.Code.BASE64_DETECTED, field, "Base64 detected"));
        }
        if (isSecret(value)) {
            issues.add(new TurkuRenderWorkspaceIssue(
                    TurkuRenderWorkspaceIssue.Code.SECRET_DETECTED, field, "Secret detected"));
        }
        if (isCommand(value)) {
            issues.add(new TurkuRenderWorkspaceIssue(
                    TurkuRenderWorkspaceIssue.Code.COMMAND_DETECTED, field, "Command detected"));
        }
        if (isShellMeta(value)) {
            issues.add(new TurkuRenderWorkspaceIssue(
                    TurkuRenderWorkspaceIssue.Code.REF_UNSAFE, field, "Shell metacharacter detected"));
        }
    }

    private void checkExtension(String ext, List<TurkuRenderWorkspaceIssue> issues) {
        if (ext == null || ext.isBlank()) {
            issues.add(new TurkuRenderWorkspaceIssue(
                    TurkuRenderWorkspaceIssue.Code.EXTENSION_NOT_ALLOWED, "extension", "Empty extension"));
            return;
        }
        if (!policy.isExtensionAllowed(ext)) {
            issues.add(new TurkuRenderWorkspaceIssue(
                    TurkuRenderWorkspaceIssue.Code.EXTENSION_NOT_ALLOWED, "extension",
                    "Extension not allowed: " + ext));
        }
    }
}
