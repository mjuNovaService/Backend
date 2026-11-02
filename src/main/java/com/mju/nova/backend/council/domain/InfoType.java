package com.mju.nova.backend.council.domain;

public enum InfoType {
    INTRO(true),       // 학생회 소개
    PLEDGE(false),     // 공약
    ORG_CHART(true),   // 조직도
    RULES(true),       // 회칙
    FAQ(false);        // 자주 묻는 질문

    // true: 화면에 글 1개만 보여주는 종류 → 1개만 등록 가능
    private final boolean single;

    InfoType(boolean single) {
        this.single = single;
    }

    public boolean isSingle() {
        return single;
    }
}
