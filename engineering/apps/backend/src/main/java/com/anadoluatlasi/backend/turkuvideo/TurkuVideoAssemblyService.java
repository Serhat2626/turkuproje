package com.anadoluatlasi.backend.turkuvideo;

import com.anadoluatlasi.backend.config.MinioConfig;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * FFmpeg ile sahne görsellerini ve TTS seslerini birleştirerek video üretir.
 *
 * <p>Akış:
 * 1. MinIO'dan sahne görselleri + ses dosyaları indirilir
 * 2. Her sahne için: görsel + ses → video segment (FFmpeg)
 * 3. Tüm segmentler birleştirilir → final MP4
 * 4. Final video MinIO'ya yüklenir
 * </p>
 */
@Slf4j
@Service
public class TurkuVideoAssemblyService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final TurkuVideoProjectRepository repository;

    public TurkuVideoAssemblyService(MinioClient minioClient,
                                      MinioConfig minioConfig,
                                      TurkuVideoProjectRepository repository) {
        this.minioClient = minioClient;
        this.minioConfig = minioConfig;
        this.repository = repository;
    }

    /**
     * Proje için video birleştirme.
     */
    public VideoAssemblyResult assembleVideo(UUID projectId) {
        TurkuVideoProject project = repository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Proje bulunamadı: " + projectId));

        TurkuStoryboardPlan storyboard = project.getStoryboardPlan();
        if (storyboard == null || storyboard.getScenes() == null || storyboard.getScenes().isEmpty()) {
            throw new RuntimeException("Storyboard hazır değil");
        }

        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("turku-video-" + projectId);
            log.info("Video birleştirme başladı: projectId={}, tempDir={}", projectId, tempDir);

            List<Path> segmentFiles = new ArrayList<>();
            int sceneCount = storyboard.getScenes().size();

            for (TurkuStoryboardScene scene : storyboard.getScenes()) {
                int sceneNo = scene.getSceneNo();
                String imagePath = String.format("turku-video/%s/scenes/%d.png", projectId, sceneNo);
                String audioPath = String.format("turku-video/%s/audio/%d.mp3", projectId, sceneNo);

                // MinIO'dan dosyaları kontrol et ve indir
                boolean hasImage = existsInMinio(imagePath);
                boolean hasAudio = existsInMinio(audioPath);

                if (!hasImage && !hasAudio) {
                    log.warn("Sahne {} için ne görsel ne ses var, atlanıyor", sceneNo);
                    continue;
                }

                Path segmentFile;
                if (hasImage && hasAudio) {
                    // Görsel + Ses → Video segment
                    Path imageFile = downloadFromMinio(imagePath, tempDir.resolve("scene_" + sceneNo + ".png"));
                    Path audioFile = downloadFromMinio(audioPath, tempDir.resolve("scene_" + sceneNo + ".mp3"));
                    log.info("Sahne {}: GÖRSEL+SES mevcut — imageSize={}KB, audioSize={}KB",
                            sceneNo, Files.size(imageFile) / 1024, Files.size(audioFile) / 1024);
                    segmentFile = tempDir.resolve("segment_" + sceneNo + ".mp4");
                    createVideoSegment(imageFile, audioFile, segmentFile);
                } else if (hasAudio) {
                    // Sadece ses → siyah ekran + ses
                    Path audioFile = downloadFromMinio(audioPath, tempDir.resolve("scene_" + sceneNo + ".mp3"));
                    log.info("Sahne {}: SADECE SES — audioSize={}KB (siyah ekran ile)", sceneNo, Files.size(audioFile) / 1024);
                    segmentFile = tempDir.resolve("segment_" + sceneNo + ".mp4");
                    createAudioOnlySegment(audioFile, segmentFile);
                } else {
                    // Sadece görsel → 10 saniye sabit görsel
                    Path imageFile = downloadFromMinio(imagePath, tempDir.resolve("scene_" + sceneNo + ".png"));
                    log.info("Sahne {}: SADECE GÖRSEL — imageSize={}KB, duration={}s",
                            sceneNo, Files.size(imageFile) / 1024, scene.getEstimatedDurationSeconds());
                    segmentFile = tempDir.resolve("segment_" + sceneNo + ".mp4");
                    createImageOnlySegment(imageFile, segmentFile, scene.getEstimatedDurationSeconds());
                }

                if (Files.exists(segmentFile) && Files.size(segmentFile) > 0) {
                    segmentFiles.add(segmentFile);
                    log.info("Segment oluşturuldu: sahne={}, size={}KB, hasImage={}, hasAudio={}",
                            sceneNo, Files.size(segmentFile) / 1024, hasImage, hasAudio);
                }
            }

            if (segmentFiles.isEmpty()) {
                throw new RuntimeException("Hiç segment oluşturulamadı — görsel veya ses dosyaları MinIO'da bulunamadı");
            }

            // Segmentleri birleştir
            Path concatVideo = tempDir.resolve("concat_raw.mp4");
            concatenateSegments(segmentFiles, concatVideo, tempDir);

            if (!Files.exists(concatVideo) || Files.size(concatVideo) == 0) {
                throw new RuntimeException("Concat video oluşturulamadı");
            }

            // Fon müziği ve türkü kaydı kontrol et
            Path finalVideo = tempDir.resolve("final.mp4");
            String fonPath = String.format("turku-video/%s/music/fon.mp3", projectId);
            String turkuPath = String.format("turku-video/%s/music/turku.mp3", projectId);
            boolean hasFon = existsInMinio(fonPath);
            boolean hasTurku = existsInMinio(turkuPath);

            if (hasFon || hasTurku) {
                log.info("Müzik mix başlıyor: fon={}, türkü={}", hasFon, hasTurku);
                mixMusicIntoVideo(concatVideo, finalVideo, tempDir, projectId, hasFon, hasTurku);
            } else {
                // Müzik yok — direkt concat kullan
                log.info("Müzik dosyası yok, concat video direkt kullanılıyor");
                Files.move(concatVideo, finalVideo);
            }

            if (!Files.exists(finalVideo) || Files.size(finalVideo) == 0) {
                throw new RuntimeException("Final video oluşturulamadı");
            }

            // MinIO'ya yükle
            String videoMinioPath = String.format("turku-video/%s/video/final.mp4", projectId);
            uploadToMinio(videoMinioPath, finalVideo);

            long videoSize = Files.size(finalVideo);
            log.info("Video birleştirme tamamlandı: projectId={}, segments={}, size={}MB, hasFon={}, hasTurku={}",
                    projectId, segmentFiles.size(), videoSize / (1024 * 1024), hasFon, hasTurku);

            return VideoAssemblyResult.builder()
                    .projectId(projectId)
                    .success(true)
                    .videoPath(videoMinioPath)
                    .segmentCount(segmentFiles.size())
                    .totalScenes(sceneCount)
                    .videoSizeBytes(videoSize)
                    .build();

        } catch (Exception e) {
            log.error("Video birleştirme hatası: projectId={}, error={}", projectId, e.getMessage(), e);
            return VideoAssemblyResult.builder()
                    .projectId(projectId)
                    .success(false)
                    .error(e.getMessage())
                    .build();
        } finally {
            // Temp dosyaları temizle
            if (tempDir != null) {
                try {
                    Files.walk(tempDir)
                            .sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                } catch (IOException e) {
                    log.warn("Temp dizin temizlenemedi: {}", tempDir);
                }
            }
        }
    }

    // ── FFmpeg: Görsel + Ses → Video Segment ──

    private void createVideoSegment(Path imageFile, Path audioFile, Path outputFile) throws Exception {
        // ffmpeg -loop 1 -i image.png -i audio.mp3
        //   -c:v libx264 -tune stillimage -c:a aac -b:a 192k
        //   -vf "scale=1920:1080:force_original_aspect_ratio=decrease,pad=1920:1080:-1:-1:color=black"
        //   -pix_fmt yuv420p -shortest -y output.mp4
        List<String> cmd = List.of(
                "ffmpeg", "-y",
                "-loop", "1",
                "-i", imageFile.toAbsolutePath().toString(),
                "-i", audioFile.toAbsolutePath().toString(),
                "-c:v", "libx264", "-tune", "stillimage",
                "-c:a", "aac", "-b:a", "192k",
                "-vf", "scale=1920:1080:force_original_aspect_ratio=decrease,pad=1920:1080:-1:-1:color=black",
                "-pix_fmt", "yuv420p",
                "-shortest",
                outputFile.toAbsolutePath().toString()
        );
        runFFmpeg(cmd, "segment (image+audio)");
    }

    private void createAudioOnlySegment(Path audioFile, Path outputFile) throws Exception {
        // Siyah ekran + ses
        List<String> cmd = List.of(
                "ffmpeg", "-y",
                "-f", "lavfi", "-i", "color=c=black:s=1920x1080:d=600",
                "-i", audioFile.toAbsolutePath().toString(),
                "-c:v", "libx264", "-c:a", "aac", "-b:a", "192k",
                "-pix_fmt", "yuv420p",
                "-shortest",
                outputFile.toAbsolutePath().toString()
        );
        runFFmpeg(cmd, "segment (audio-only)");
    }

    private void createImageOnlySegment(Path imageFile, Path outputFile, int durationSeconds) throws Exception {
        int duration = durationSeconds > 0 ? durationSeconds : 10;
        List<String> cmd = List.of(
                "ffmpeg", "-y",
                "-loop", "1",
                "-i", imageFile.toAbsolutePath().toString(),
                "-t", String.valueOf(duration),
                "-c:v", "libx264", "-tune", "stillimage",
                "-vf", "scale=1920:1080:force_original_aspect_ratio=decrease,pad=1920:1080:-1:-1:color=black",
                "-pix_fmt", "yuv420p",
                "-an",
                outputFile.toAbsolutePath().toString()
        );
        runFFmpeg(cmd, "segment (image-only)");
    }

    // ── FFmpeg: Segmentleri Birleştir ──

    private void concatenateSegments(List<Path> segments, Path outputFile, Path tempDir) throws Exception {
        // Concat dosyası oluştur
        Path concatFile = tempDir.resolve("concat.txt");
        StringBuilder sb = new StringBuilder();
        for (Path seg : segments) {
            sb.append("file '").append(seg.toAbsolutePath().toString().replace("\\", "/")).append("'\n");
        }
        Files.writeString(concatFile, sb.toString());

        List<String> cmd = List.of(
                "ffmpeg", "-y",
                "-f", "concat", "-safe", "0",
                "-i", concatFile.toAbsolutePath().toString(),
                "-c", "copy",
                outputFile.toAbsolutePath().toString()
        );
        runFFmpeg(cmd, "concat");
    }

    // ── FFmpeg: Müzik Mix ──

    /**
     * Fon müziği ve/veya türkü kaydını videoya ekler.
     * - Fon müziği: narration altında %12 ses ile loop olarak çalar
     * - Türkü: videonun sonuna tam sesle eklenir
     */
    private void mixMusicIntoVideo(Path inputVideo, Path outputVideo, Path tempDir,
                                    UUID projectId, boolean hasFon, boolean hasTurku) throws Exception {
        Path currentVideo = inputVideo;

        // Adım 1: Fon müziği ekle (varsa)
        if (hasFon) {
            String fonMinioPath = String.format("turku-video/%s/music/fon.mp3", projectId);
            Path fonFile = downloadFromMinio(fonMinioPath, tempDir.resolve("fon_music.mp3"));
            Path fonMixed = tempDir.resolve("fon_mixed.mp4");

            // Fon müziğini %12 ses ile loop olarak arka plana ekle
            // amix: narration (ana ses) + fon müziği (düşük ses)
            List<String> cmd = new ArrayList<>(List.of(
                    "ffmpeg", "-y",
                    "-i", currentVideo.toAbsolutePath().toString(),
                    "-stream_loop", "-1",
                    "-i", fonFile.toAbsolutePath().toString(),
                    "-filter_complex",
                    "[1:a]volume=0.12[bg];[0:a][bg]amix=inputs=2:duration=first:dropout_transition=3[out]",
                    "-map", "0:v", "-map", "[out]",
                    "-c:v", "copy", "-c:a", "aac", "-b:a", "192k",
                    "-shortest",
                    fonMixed.toAbsolutePath().toString()
            ));
            runFFmpeg(cmd, "fon müzik mix");
            currentVideo = fonMixed;
            log.info("Fon müziği eklendi: size={}KB", Files.size(fonFile) / 1024);
        }

        // Adım 2: Türkü kaydını sona ekle (varsa)
        if (hasTurku) {
            String turkuMinioPath = String.format("turku-video/%s/music/turku.mp3", projectId);
            Path turkuFile = downloadFromMinio(turkuMinioPath, tempDir.resolve("turku_music.mp3"));

            // Türkü ses dosyasından video segment oluştur (son görsel + türkü ses)
            // Son sahne görselini kullan, yoksa siyah ekran
            Path turkuSegment = tempDir.resolve("turku_segment.mp4");

            // Siyah ekran + türkü tam ses (türkü uzunluğu kadar)
            List<String> turkuCmd = new ArrayList<>(List.of(
                    "ffmpeg", "-y",
                    "-f", "lavfi", "-i", "color=c=black:s=1920x1080:r=24",
                    "-i", turkuFile.toAbsolutePath().toString(),
                    "-c:v", "libx264", "-c:a", "aac", "-b:a", "192k",
                    "-pix_fmt", "yuv420p",
                    "-shortest",
                    turkuSegment.toAbsolutePath().toString()
            ));
            runFFmpeg(turkuCmd, "türkü segment");

            // Ana video + türkü segmentini birleştir
            Path concatWithTurku = tempDir.resolve("concat_turku.txt");
            Files.writeString(concatWithTurku,
                    "file '" + currentVideo.toAbsolutePath().toString().replace("\\", "/") + "'\n" +
                    "file '" + turkuSegment.toAbsolutePath().toString().replace("\\", "/") + "'\n");

            Path turkuFinal = tempDir.resolve("turku_final.mp4");
            // concat ile codec uyumu olması için re-encode
            List<String> concatCmd = new ArrayList<>(List.of(
                    "ffmpeg", "-y",
                    "-f", "concat", "-safe", "0",
                    "-i", concatWithTurku.toAbsolutePath().toString(),
                    "-c:v", "libx264", "-c:a", "aac", "-b:a", "192k",
                    "-pix_fmt", "yuv420p",
                    turkuFinal.toAbsolutePath().toString()
            ));
            runFFmpeg(concatCmd, "türkü concat");
            currentVideo = turkuFinal;
            log.info("Türkü kaydı eklendi: turkuSize={}KB", Files.size(turkuFile) / 1024);
        }

        // Son video'yu output'a kopyala
        Files.copy(currentVideo, outputVideo);
    }

    // ── FFmpeg Process Runner ──

    private void runFFmpeg(List<String> command, String label) throws Exception {
        log.debug("FFmpeg komutu ({}): {}", label, String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // FFmpeg çıktısını oku (buffer taşmasını önle)
        String output;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            output = reader.lines().reduce("", (a, b) -> b); // Son satırı tut
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.error("FFmpeg {} hatası (exit={}): {}", label, exitCode, output);
            throw new RuntimeException("FFmpeg " + label + " hatası (exit=" + exitCode + ")");
        }
        log.debug("FFmpeg {} tamamlandı", label);
    }

    // ── MinIO Yardımcıları ──

    private boolean existsInMinio(String objectPath) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectPath)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Path downloadFromMinio(String objectPath, Path targetFile) throws Exception {
        try (InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(minioConfig.getBucketName())
                .object(objectPath)
                .build());
             OutputStream out = new FileOutputStream(targetFile.toFile())) {
            stream.transferTo(out);
        }
        log.debug("MinIO'dan indirildi: {} → {}", objectPath, targetFile);
        return targetFile;
    }

    private void uploadToMinio(String objectPath, Path sourceFile) throws Exception {
        try (InputStream stream = new FileInputStream(sourceFile.toFile())) {
            long size = Files.size(sourceFile);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectPath)
                    .stream(stream, size, -1)
                    .contentType("video/mp4")
                    .build());
        }
        log.info("MinIO'ya yüklendi: {}", objectPath);
    }

    // ── Response DTO ──

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VideoAssemblyResult {
        private UUID projectId;
        private boolean success;
        private String videoPath;
        private int segmentCount;
        private int totalScenes;
        private long videoSizeBytes;
        private String error;
    }
}
