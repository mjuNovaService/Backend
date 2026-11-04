package com.mju.nova.backend.member.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mju.nova.backend.member.application.CertificateInfo;
import com.mju.nova.backend.member.application.CertificateReader;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

/**
 * Google Cloud Vision이 월 1000건까지 무료 이용이라 선택.
 *  - 이미지(JPG/PNG): POST /v1/images:annotate
 *  - PDF:            POST /v1/files:annotate (첫 페이지만)
 * API 키가 없거나 호출이 실패하면 Optional.empty() → 회원가입은 PENDING (가입 자체는 됨).
 */
@Slf4j
@Component
public class GoogleVisionCertificateReader implements CertificateReader {

    private static final String BASE_URL = "https://vision.googleapis.com/v1";
    private static final String API_KEY_HEADER = "X-Goog-Api-Key";   // 키를 URL에 넣지 않아 로그에 남지 않게
    private static final Map<String, String> FEATURE = Map.of("type", "DOCUMENT_TEXT_DETECTION");

    private final GoogleVisionProperties properties;
    private final RestClient restClient;

    public GoogleVisionCertificateReader(GoogleVisionProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(15));
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory(requestFactory)
                .build();

        // 서버를 켤 때 OCR이 켜졌는지 로그로 뜸 (키 값은 남기지 않음)
        if (StringUtils.hasText(properties.apiKey())) {
            log.info("재학증명서 OCR: 켜짐 (Google Cloud Vision)");
        } else {
            log.warn("재학증명서 OCR: 꺼짐 (GOOGLE_VISION_API_KEY 없음) → 모든 회원가입이 PENDING");
        }
    }

    @Override
    public Optional<CertificateInfo> read(MultipartFile file) {
        if (!StringUtils.hasText(properties.apiKey())) {
            log.warn("GOOGLE_VISION_API_KEY가 없어 OCR을 건너뜁니다 (PENDING 처리)");
            return Optional.empty();
        }
        try {
            String content = Base64.getEncoder().encodeToString(file.getBytes());
            String text = isPdf(file) ? readPdf(content) : readImage(content);
            if (!StringUtils.hasText(text)) {
                log.info("재학증명서에서 글자를 찾지 못했습니다 (PENDING 처리)");
                return Optional.empty();
            }
            return Optional.of(new CertificateInfo(CertificateInfo.normalize(text)));
        } catch (Exception e) {
            // 네트워크 오류, 키 오류, 할당량 초과 등 → 가입은 진행하고 관리자 확인 단계로 넘어감
            log.warn("재학증명서 OCR 실패 (PENDING 처리): {}", e.getMessage());
            return Optional.empty();
        }
    }

    private String readImage(String content) {
        Map<String, Object> request = Map.of(
                "image", Map.of("content", content),
                "features", List.of(FEATURE),
                "imageContext", Map.of("languageHints", List.of("ko")));
        AnnotateResponse response = restClient.post()
                .uri("/images:annotate")
                .header(API_KEY_HEADER, properties.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("requests", List.of(request)))
                .retrieve()
                .body(AnnotateResponse.class);
        return response == null ? null : response.firstText();
    }

    private String readPdf(String content) {
        Map<String, Object> request = Map.of(
                "inputConfig", Map.of("content", content, "mimeType", "application/pdf"),
                "features", List.of(FEATURE),
                "imageContext", Map.of("languageHints", List.of("ko")),
                "pages", List.of(1));
        FileAnnotateResponse response = restClient.post()
                .uri("/files:annotate")
                .header(API_KEY_HEADER, properties.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("requests", List.of(request)))
                .retrieve()
                .body(FileAnnotateResponse.class);
        return response == null ? null : response.firstText();
    }

    private boolean isPdf(MultipartFile file) {
        String filename = file.getOriginalFilename();
        return MediaType.APPLICATION_PDF_VALUE.equals(file.getContentType())
                || (filename != null && filename.toLowerCase().endsWith(".pdf"));
    }

    // ── Vision API 응답 중 필요한 부분만 ──

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AnnotateResponse(List<ImageResult> responses) {
        String firstText() {
            if (responses == null || responses.isEmpty()) {
                return null;
            }
            return responses.get(0).text();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FileAnnotateResponse(List<AnnotateResponse> responses) {
        String firstText() {
            if (responses == null || responses.isEmpty()) {
                return null;
            }
            return responses.get(0).firstText();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ImageResult(FullTextAnnotation fullTextAnnotation, ErrorStatus error) {
        String text() {
            if (error != null) {
                throw new IllegalStateException("Vision API 오류: " + error.message());
            }
            return fullTextAnnotation == null ? null : fullTextAnnotation.text();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FullTextAnnotation(String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ErrorStatus(Integer code, String message) {
    }
}
