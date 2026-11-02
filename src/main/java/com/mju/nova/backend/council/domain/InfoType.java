package com.mju.nova.backend.council.domain;

public enum InfoType {
    INTRO(true),       // 학생회 소개
    PLEDGE(true),      // 공약 (공약 전체를 글 하나에 담아 수정해서 사용)
    ORG_CHART(true),   // 조직도
    RULES(true),       // 회칙
    FAQ(false);        // 자주 묻는 질문

    // true: 글 1개를 올려두고 수정해서 쓰는 종류 → 1개만 등록 가능 (FAQ만 여러 개)
    private final boolean single;

    InfoType(boolean single) {
        this.single = single;
    }

    public boolean isSingle() {
        return single;
    }
}
