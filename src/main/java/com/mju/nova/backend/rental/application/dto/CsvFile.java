package com.mju.nova.backend.rental.application.dto;

// 내려줄 CSV 파일. content는 BOM이 붙은 UTF-8 바이트
public record CsvFile(
        String fileName,
        byte[] content
) {
}
