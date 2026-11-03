package com.mju.nova.backend.member.application;

import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 재학증명서 파일에서 글자를 읽는다 (OCR).
 * 읽지 못하면(키 없음, OCR 실패, 글자 없음) Optional.empty() → 회원가입은 PENDING으로 처리한다.
 * 구현: GoogleVisionCertificateReader (infrastructure)
 */
public interface CertificateReader {

    Optional<CertificateInfo> read(MultipartFile file);
}
