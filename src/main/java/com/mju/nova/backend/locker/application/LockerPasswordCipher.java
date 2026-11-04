package com.mju.nova.backend.locker.application;

/**
 * 사물함 비밀번호 암호화·복호화. DB(locker_assignment.locker_password)에는 encrypt()가 돌려준 값만 저장한다.
 * 복호화는 회장단 비밀번호 열람에서만 쓴다. 학생 응답에는 비밀번호를 넣지 않는다.
 * 구현: AesLockerPasswordCipher (AES-256-GCM, 키는 환경변수 LOCKER_PASSWORD_KEY)
 */
public interface LockerPasswordCipher {

    String encrypt(String password);

    String decrypt(String encrypted);
}
