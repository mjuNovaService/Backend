package com.mju.nova.backend.member.application.dto;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

/**
 * 재학증명서 보기 응답에 쓸 파일과 형식 (application/pdf, image/jpeg, image/png).
 */
public record CertificateFile(Resource resource, MediaType mediaType) {
}
