package com.mju.nova.backend.member.application;

import org.springframework.web.multipart.MultipartFile;

/**
 * PENDING 회원의 재학증명서를 보관한다. 관리자가 확인한 뒤에는 지운다.
 * DB(student.certificate_image_key)에는 파일 자체가 아니라 store()가 돌려준 키(위치)만 저장한다.
 * 구현: LocalCertificateStorage (지금) → S3CertificateStorage (배포 전 교체 예정)
 */
public interface CertificateStorage {

    // 저장하고 키를 돌려준다. 예: "certificates/60231210/3f2a....pdf"
    String store(String studentId, MultipartFile file);

    void delete(String key);
}
