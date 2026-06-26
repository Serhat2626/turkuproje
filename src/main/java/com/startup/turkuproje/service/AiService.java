package com.startup.turkuproje.service;

import com.startup.turkuproje.entity.AiGenerationLog;
import com.startup.turkuproje.entity.Project;
import com.startup.turkuproje.entity.PromptType;
import com.startup.turkuproje.exception.AiServiceException;
import com.startup.turkuproje.repository.AiGenerationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// OpenAI API ile haberleşen servis
@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private final ChatClient.Builder chatClientBuilder;
    private final AiGenerationLogRepository logRepository;

    @Value("${app.ai.default-model:gpt-4o}")
    private String defaultModel;

    /**
     * AI'ya prompt gönder ve yanıt al.
     * Her çağrı loglanır.
     */
    public String generate(Project project, PromptType promptType, String prompt) {
        log.info("AI üretim başlatılıyor: projectId={}, promptType={}", project.getId(), promptType);
        long startTime = System.currentTimeMillis();

        try {
            ChatClient chatClient = chatClientBuilder.build();

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            long durationMs = System.currentTimeMillis() - startTime;
            log.info("AI yanıt alındı: projectId={}, süre={}ms", project.getId(), durationMs);

            // AI çağrısını logla
            saveLog(project, promptType, prompt, response, durationMs);

            return response;

        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startTime;
            log.error("AI servisi hatası: projectId={}, hata={}", project.getId(), e.getMessage(), e);

            // Hatalı çağrıyı da logla
            saveLog(project, promptType, prompt, "HATA: " + e.getMessage(), durationMs);

            throw new AiServiceException("AI yanıt üretemedi: " + e.getMessage(), e);
        }
    }

    // AI çağrı logunu kaydet
    private void saveLog(Project project, PromptType promptType, String prompt,
                         String response, long durationMs) {
        try {
            AiGenerationLog aiLog = AiGenerationLog.builder()
                    .project(project)
                    .promptType(promptType)
                    .promptSent(prompt)
                    .responseReceived(response)
                    .model(defaultModel)
                    .durationMs(durationMs)
                    .build();
            logRepository.save(aiLog);
        } catch (Exception e) {
            log.warn("AI log kaydedilemedi: {}", e.getMessage());
        }
    }
}
