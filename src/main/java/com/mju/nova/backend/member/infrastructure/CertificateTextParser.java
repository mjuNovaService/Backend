package com.mju.nova.backend.member.infrastructure;

import com.mju.nova.backend.member.application.CertificateInfo;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OCR로 읽은 재학증명서 글자를 CertificateInfo로 바꾼다.
 * OCR은 줄 순서가 양식과 다르게 나올 수 있어서(라벨끼리, 값끼리 묶이는 등)
 * '어느 줄에 있는지'가 아니라 '글자가 들어 있는지'로 판단한다.
 */
final class CertificateTextParser {

    // "학부 · 학과 · 전공" 라벨 (OCR이 점·공백을 다르게 읽어도 잡히도록)
    private static final Pattern DEPARTMENT_LABEL =
            Pattern.compile("학\\s*부\\s*[·ㆍ.,]?\\s*학\\s*과\\s*[·ㆍ.,]?\\s*전\\s*공");

    // "융합소프트웨어학부" 또는 "○○학과", 뒤에 "○○전공"이 붙으면 함께
    private static final Pattern DEPARTMENT_VALUE =
            Pattern.compile("([가-힣]{2,}(?:학부|학과))(?:\\s*([가-힣]{2,}전공))?");

    private static final int DEPARTMENT_MAX_LENGTH = 50;   // student.department 칸 길이

    private CertificateTextParser() {
    }

    static CertificateInfo parse(String ocrText) {
        return new CertificateInfo(CertificateInfo.normalize(ocrText), findDepartment(ocrText));
    }

    private static String findDepartment(String ocrText) {
        // 라벨을 먼저 지워야 "학부·학과·전공" 자체가 학과로 잡히지 않는다
        String withoutLabel = DEPARTMENT_LABEL.matcher(ocrText).replaceAll(" ");
        Matcher matcher = DEPARTMENT_VALUE.matcher(withoutLabel);
        if (!matcher.find()) {
            return null;
        }
        String department = matcher.group(2) == null
                ? matcher.group(1)
                : matcher.group(1) + " " + matcher.group(2);
        return department.length() > DEPARTMENT_MAX_LENGTH
                ? department.substring(0, DEPARTMENT_MAX_LENGTH)
                : department;
    }
}
