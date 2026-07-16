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
                String videoClipPath = String.format("turku-video/%s/scenes/%d_video.mp4", projectId, sceneNo);
                String imagePath = String.format("turku-video/%s/scenes/%d.png", projectId, sceneNo);
                String audioPath = String.format("turku-video/%s/audio/%d.mp3", projectId, sceneNo);

                // MinIO'dan dosyaları kontrol et
                boolean hasVideoClip = existsInMinio(videoClipPath);
                boolean hasImage = existsInMinio(imagePath);
                boolean hasAudio = existsInMinio(audioPath);

                if (!hasImage && !hasAudio && !hasVideoClip) {
                    log.warn("Sahne {} için ne görsel ne ses ne video var, atlanıyor", sceneNo);
                    continue;
                }

                Path segmentFile;
                if (hasVideoClip && hasAudio) {
                    // Kling video klip + Ses → Video segment (EN İYİ sonuç)
                    Path videoClipFile = downloadFromMinio(videoClipPath, tempDir.resolve("clip_" + sceneNo + ".mp4"));
                    Path audioFile = downloadFromMinio(audioPath, tempDir.resolve("scene_" + sceneNo + ".mp3"));
                    log.info("Sahne {}: KLING VIDEO+SES — clipSize={}KB, audioSize={}KB",
                            sceneNo, Files.size(videoClipFile) / 1024, Files.size(audioFile) / 1024);
                    segmentFile = tempDir.resolve("segment_" + sceneNo + ".mp4");
                    mergeVideoClipWithAudio(videoClipFile, audioFile, segmentFile);
                } else if (hasVideoClip) {
                    // Sadece Kling video klip (sessiz — loop edilecek)
                    Path videoClipFile = downloadFromMinio(videoClipPath, tempDir.resolve("clip_" + sceneNo + ".mp4"));
                    log.info("Sahne {}: KLING VIDEO (sessiz) — clipSize={}KB", sceneNo, Files.size(videoClipFile) / 1024);
                    segmentFile = tempDir.resolve("segment_" + sceneNo + ".mp4");
                    Files.copy(videoClipFile, segmentFile);
                } else if (hasImage && hasAudio) {
                    // Görsel + Ses → Video segment (Ken Burns fallback)
                    Path imageFile = downloadFromMinio(imagePath, tempDir.resolve("scene_" + sceneNo + ".png"));
                    Path audioFile = downloadFromMinio(audioPath, tempDir.resolve("scene_" + sceneNo + ".mp3"));
                    log.info("Sahne {}: GÖRSEL+SES (Ken Burns) — imageSize={}KB, audioSize={}KB",
                            sceneNo, Files.size(imageFile) / 1024, Files.size(audioFile) / 1024);
                    segmentFile = tempDir.resolve("segment_" + sceneNo + ".mp4");
                    createVideoSegment(imageFile, audioFile, segmentFile);
                } else if (hasAudio) {
                    // Sadece ses → koyu arka plan + ses
                    Path audioFile = downloadFromMinio(audioPath, tempDir.resolve("scene_" + sceneNo + ".mp3"));
                    log.info("Sahne {}: SADECE SES — audioSize={}KB", sceneNo, Files.size(audioFile) / 1024);
                    segmentFile = tempDir.resolve("segment_" + sceneNo + ".mp4");
                    createAudioOnlySegment(audioFile, segmentFile);
                } else {
                    // Sadece görsel → Ken Burns
                    Path imageFile = downloadFromMinio(imagePath, tempDir.resolve("scene_" + sceneNo + ".png"));
                    log.info("Sahne {}: SADECE GÖRSEL (Ken Burns) — imageSize={}KB, duration={}s",
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

    // ── FFmpeg: Kling Video Klip + Ses → Birleşik Segment ──

    /**
     * Kling AI tarafından üretilen hareketli video klibi TTS ses ile birleştirir.
     * Video süresi ses süresine göre ayarlanır (loop veya trim).
     */
    private void mergeVideoClipWithAudio(Path videoClip, Path audioFile, Path outputFile) throws Exception {
        // Ses süresini öğren
        double audioDuration = getMediaDuration(audioFile);
        double videoDuration = getMediaDuration(videoClip);

        List<String> cmd;
        if (videoDuration >= audioDuration) {
            // Video yeterince uzun — sadece trim + ses ekle
            cmd = List.of(
                    "ffmpeg", "-y",
                    "-i", videoClip.toString(),
                    "-i", audioFile.toString(),
                    "-t", String.valueOf(audioDuration),
                    "-map", "0:v",
                    "-map", "1:a",
                    "-c:v", "libx264",
                    "-preset", "medium",
                    "-crf", "23",
                    "-c:a", "aac", "-b:a", "192k",
                    "-pix_fmt", "yuv420p",
                    "-movflags", "+faststart",
                    outputFile.toString()
            );
        } else {
            // Video kısa — loop et + ses ekle
            int loopCount = (int) Math.ceil(audioDuration / videoDuration);
            cmd = List.of(
                    "ffmpeg", "-y",
                    "-stream_loop", String.valueOf(loopCount),
                    "-i", videoClip.toString(),
                    "-i", audioFile.toString(),
                    "-t", String.valueOf(audioDuration),
                    "-map", "0:v",
                    "-map", "1:a",
                    "-c:v", "libx264",
                    "-preset", "medium",
                    "-crf", "23",
                    "-c:a", "aac", "-b:a", "192k",
                    "-pix_fmt", "yuv420p",
                    "-movflags", "+faststart",
                    "-shortest",
                    outputFile.toString()
            );
        }

        log.info("Kling video+ses birleştirme: audioDuration={}s, videoDuration={}s, looping={}",
                audioDuration, videoDuration, videoDuration < audioDuration);
        runFFmpeg(cmd, "kling-merge");
    }

    /**
     * Medya dosyasının süresini saniye olarak döndürür (ffprobe ile).
     */
    private double getMediaDuration(Path file) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "ffprobe",
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                file.toString()
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes()).trim();
        process.waitFor(15, java.util.concurrent.TimeUnit.SECONDS);
        try {
            return Double.parseDouble(output);
        } catch (NumberFormatException e) {
            log.warn("ffprobe duration okunamadı: {}, default 5s", output);
            return 5.0;
        }
    }

    // ── FFmpeg: Görsel + Ses → Video Segment (Ken Burns efekti ile) ──

    private void createVideoSegment(Path imageFile, Path audioFile, Path outputFile) throws Exception {
        // Ken Burns: zoom-in + hafif pan hareketi — görseli canlı ve sinematik yapar
        // zoompan: z (zoom), d (duration frames), x/y (pan), s (output size), fps
        String kenBurnsFilter = String.format(
                "scale=3840:2160,zoompan=z='min(zoom+0.001,1.3)':d=%d:x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':s=1920x1080:fps=24",
                24 * 60  // max 60sn yeterli
        );

        List<String> cmd = List.of(
                "ffmpeg", "-y",
                "-loop", "1",
                "-i", imageFile.toAbsolutePath().toString(),
                "-i", audioFile.toAbsolutePath().toString(),
                "-c:v", "libx264", "-preset", "medium",
                "-c:a", "aac", "-b:a", "192k",
                "-vf", kenBurnsFilter,
                "-pix_fmt", "yuv420p",
                "-shortest",
                outputFile.toAbsolutePath().toString()
        );
        runFFmpeg(cmd, "segment (image+audio, Ken Burns)");
    }

    private void createAudioOnlySegment(Path audioFile, Path outputFile) throws Exception {
        // Siyah ekran yerine gradient arka plan + ses
        List<String> cmd = List.of(
                "ffmpeg", "-y",
                "-f", "lavfi", "-i",
                "color=c=0x1a1a2e:s=1920x1080:d=600,format=yuv420p",
                "-i", audioFile.toAbsolutePath().toString(),
                "-c:v", "libx264", "-c:a", "aac", "-b:a", "192k",
                "-pix_fmt", "yuv420p",
                "-shortest",
                outputFile.toAbsolutePath().toString()
        );
        runFFmpeg(cmd, "segment (audio-only, dark bg)");
    }

    private void createImageOnlySegment(Path imageFile, Path outputFile, int durationSeconds) throws Exception {
        int duration = durationSeconds > 0 ? durationSeconds : 10;
        String kenBurnsFilter = String.format(
                "scale=3840:2160,zoompan=z='min(zoom+0.001,1.3)':d=%d:x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':s=1920x1080:fps=24",
                24 * duration
        );

        List<String> cmd = List.of(
                "ffmpeg", "-y",
                "-loop", "1",
                "-i", imageFile.toAbsolutePath().toString(),
                "-t", String.valueOf(duration),
                "-c:v", "libx264", "-preset", "medium",
                "-vf", kenBurnsFilter,
                "-pix_fmt", "yuv420p",
                "-an",
                outputFile.toAbsolutePath().toString()
        );
        runFFmpeg(cmd, "segment (image-only, Ken Burns)");
    }

    // ── FFmpeg: Segmentleri Birleştir ──

    private void concatenateSegments(List<Path> segments, Path outputFile, Path tempDir) throws Exception {
        if (segments.size() == 1) {
            Files.copy(segments.get(0), outputFile);
            return;
        }

        // Crossfade ile birleştirme: xfade filtresi
        // Her segment arasında 0.6sn fade geçişi
        double crossfadeDuration = 0.6;

        // Segment sürelerini öğren (ffprobe ile)
        List<Double> durations = new ArrayList<>();
        for (Path seg : segments) {
            double dur = getVideoDuration(seg);
            durations.add(dur);
        }

        // Çok fazla segment varsa xfade çok karmaşıklaşır — concat + re-encode fallback
        if (segments.size() > 15) {
            log.info("{} segment var — concat+re-encode kullanılıyor (crossfade yerine)", segments.size());
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
                    "-c:v", "libx264", "-preset", "medium",
                    "-c:a", "aac", "-b:a", "192k",
                    "-pix_fmt", "yuv420p",
                    outputFile.toAbsolutePath().toString()
            );
            runFFmpeg(cmd, "concat (re-encode)");
            return;
        }

        // xfade ile crossfade birleştirme
        StringBuilder filterComplex = new StringBuilder();
        int inputCount = segments.size();

        // Input'ları oluştur
        List<String> cmd = new ArrayList<>();
        cmd.add("ffmpeg");
        cmd.add("-y");
        for (Path seg : segments) {
            cmd.add("-i");
            cmd.add(seg.toAbsolutePath().toString());
        }

        // xfade chain: [0:v][1:v]xfade → [v1], [v1][2:v]xfade → [v2], ...
        // SES: acrossfade yerine concat kullan — seslendirme çakışmasını önler
        double offset = durations.get(0) - crossfadeDuration;
        String lastVideoLabel = "[0:v]";

        for (int i = 1; i < inputCount; i++) {
            String outLabel = "[v" + i + "]";

            filterComplex.append(String.format(java.util.Locale.US,
                    "%s[%d:v]xfade=transition=fade:duration=%.1f:offset=%.1f%s;",
                    lastVideoLabel, i, crossfadeDuration, Math.max(0, offset), outLabel
            ));

            if (i < inputCount - 1) {
                offset += durations.get(i) - crossfadeDuration;
            }
            lastVideoLabel = outLabel;
        }

        // Ses concat: tüm audio stream'lerini birleştir (overlap yok)
        StringBuilder audioConcat = new StringBuilder();
        for (int i = 0; i < inputCount; i++) {
            audioConcat.append("[").append(i).append(":a]");
        }
        String lastAudioLabel = "[aout]";
        filterComplex.append(String.format("%sconcat=n=%d:v=0:a=1%s",
                audioConcat.toString(), inputCount, lastAudioLabel));

        String filter = filterComplex.toString();

        cmd.add("-filter_complex");
        cmd.add(filter);
        cmd.add("-map");
        cmd.add(lastVideoLabel);
        cmd.add("-map");
        cmd.add(lastAudioLabel);
        cmd.add("-c:v");
        cmd.add("libx264");
        cmd.add("-preset");
        cmd.add("medium");
        cmd.add("-c:a");
        cmd.add("aac");
        cmd.add("-b:a");
        cmd.add("192k");
        cmd.add("-pix_fmt");
        cmd.add("yuv420p");
        cmd.add(outputFile.toAbsolutePath().toString());

        runFFmpeg(cmd, "crossfade concat");
    }

    /**
     * FFprobe ile video süresini öğren (saniye cinsinden)
     */
    private double getVideoDuration(Path videoFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffprobe", "-v", "quiet", "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    videoFile.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String output = new String(p.getInputStream().readAllBytes()).trim();
            p.waitFor();
            return Double.parseDouble(output);
        } catch (Exception e) {
            log.warn("FFprobe hatası, varsayılan 20sn kullanılıyor: {}", e.getMessage());
            return 20.0;
        }
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

        // Adım 2: Türkü kaydını sona ekle (varsa) — GÖRSEL SLIDESHOW ile
        if (hasTurku) {
            String turkuMinioPath = String.format("turku-video/%s/music/turku.mp3", projectId);
            Path turkuFile = downloadFromMinio(turkuMinioPath, tempDir.resolve("turku_music.mp3"));

            // Türkü bölümü: sahne görsellerini slideshow olarak göster
            Path turkuSegment = tempDir.resolve("turku_segment.mp4");

            // MinIO'dan mevcut sahne görsellerini bul
            List<Path> sceneImages = new ArrayList<>();
            for (int i = 1; i <= 35; i++) {
                String imgPath = String.format("turku-video/%s/scenes/%d.png", projectId, i);
                try {
                    minioClient.statObject(io.minio.StatObjectArgs.builder()
                            .bucket(minioConfig.getBucketName()).object(imgPath).build());
                    Path img = downloadFromMinio(imgPath, tempDir.resolve("turku_slide_" + i + ".png"));
                    sceneImages.add(img);
                } catch (Exception ignored) {}
            }

            if (!sceneImages.isEmpty()) {
                // Türkü süresi
                double turkuDuration = getVideoDuration(turkuFile);
                if (turkuDuration <= 0) turkuDuration = 180; // fallback 3dk
                double perImageDuration = turkuDuration / sceneImages.size();
                if (perImageDuration < 5) perImageDuration = 5;

                // Her görselden Ken Burns segment oluştur
                List<Path> turkuSlides = new ArrayList<>();
                for (int i = 0; i < sceneImages.size(); i++) {
                    Path slideOut = tempDir.resolve("turku_slideseg_" + i + ".mp4");
                    createImageOnlySegment(sceneImages.get(i), slideOut, (int) perImageDuration);
                    turkuSlides.add(slideOut);
                }

                // Slideshow concat
                Path slideshowConcat = tempDir.resolve("turku_slideshow.mp4");
                Path concatTxt = tempDir.resolve("turku_slides_concat.txt");
                StringBuilder slidesSb = new StringBuilder();
                for (Path slide : turkuSlides) {
                    slidesSb.append("file '").append(slide.toAbsolutePath().toString().replace("\\", "/")).append("'\n");
                }
                Files.writeString(concatTxt, slidesSb.toString());

                List<String> slideshowCmd = List.of(
                        "ffmpeg", "-y",
                        "-f", "concat", "-safe", "0",
                        "-i", concatTxt.toAbsolutePath().toString(),
                        "-c:v", "libx264", "-preset", "medium",
                        "-pix_fmt", "yuv420p",
                        slideshowConcat.toAbsolutePath().toString()
                );
                runFFmpeg(slideshowCmd, "türkü slideshow");

                // Slideshow + türkü ses birleştir
                List<String> mergeCmd = List.of(
                        "ffmpeg", "-y",
                        "-i", slideshowConcat.toAbsolutePath().toString(),
                        "-i", turkuFile.toAbsolutePath().toString(),
                        "-c:v", "copy", "-c:a", "aac", "-b:a", "192k",
                        "-shortest",
                        turkuSegment.toAbsolutePath().toString()
                );
                runFFmpeg(mergeCmd, "türkü slideshow+ses");
            } else {
                // Görsel yoksa koyu arka plan kullan (siyah değil)
                List<String> turkuCmd = new ArrayList<>(List.of(
                        "ffmpeg", "-y",
                        "-f", "lavfi", "-i", "color=c=0x1a1a2e:s=1920x1080:r=24",
                        "-i", turkuFile.toAbsolutePath().toString(),
                        "-c:v", "libx264", "-c:a", "aac", "-b:a", "192k",
                        "-pix_fmt", "yuv420p",
                        "-shortest",
                        turkuSegment.toAbsolutePath().toString()
                ));
                runFFmpeg(turkuCmd, "türkü segment (no images)");
            }

            // Ana video + türkü segmentini birleştir
            Path concatWithTurku = tempDir.resolve("concat_turku.txt");
            Files.writeString(concatWithTurku,
                    "file '" + currentVideo.toAbsolutePath().toString().replace("\\", "/") + "'\n" +
                    "file '" + turkuSegment.toAbsolutePath().toString().replace("\\", "/") + "'\n");

            Path turkuFinal = tempDir.resolve("turku_final.mp4");
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
            log.info("Türkü kaydı görsel slideshow ile eklendi: turkuSize={}KB, imageCount={}",
                    Files.size(turkuFile) / 1024, sceneImages.size());
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
