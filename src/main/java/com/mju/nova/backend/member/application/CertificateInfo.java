package com.mju.nova.backend.member.application;

/**
 * 재학증명서에서 읽은 내용.
 *
 * @param text OCR로 읽은 전체 글자에서 공백·점을 뺀 것 (대학 이름·성명이 들어 있는지 확인용)
 */
public record CertificateInfo(String text) {

    // 비교용 정리: 공백, 가운뎃점(·ㆍ), 마침표, 쌍점 등을 모두 뺀다
    // "인공지능·소프트웨어융합대학", "인공지능 소프트웨어융합대학" → "인공지능소프트웨어융합대학"
    public static String normalize(String value) {
        return value == null ? "" : value.replaceAll("[\\s·ㆍ.:;：,\\-]", "");
    }

    // 증명서 글자 안에 이 값이 들어 있나 (양쪽 다 정리한 뒤 비교)
    public boolean contains(String value) {
        String target = normalize(value);
        return !target.isEmpty() && text.contains(target);
    }
}
