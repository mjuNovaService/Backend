package com.mju.nova.backend.locker.infrastructure;

import com.mju.nova.backend.locker.application.LockerPasswordCipher;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AES-256-GCM으로 사물함 비밀번호를 암호화한다.
 * 저장값 = Base64(IV 12바이트 + 암호문 + 인증 태그 16바이트). 숫자 4자리면 44자.
 * IV를 값마다 새로 만들어서 같은 비밀번호도 저장값이 매번 다르다 (DB만 봐서는 같은 비밀번호인지 알 수 없음).
 *
 * 키(locker.password-key)는 Base64로 인코딩한 32바이트. 서버를 켤 때 형식이 틀리면 바로 실패한다.
 * 키를 잃어버리거나 바꾸면 기존 값은 복호화할 수 없다 (학생이 비밀번호 수정으로 다시 입력하면 새 키로 저장됨).
 */
@Component
public class AesLockerPasswordCipher implements LockerPasswordCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_LENGTH = 32;
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public AesLockerPasswordCipher(@Value("${locker.password-key}") String base64Key) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(base64Key.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("locker.password-key는 Base64 형식이어야 합니다", e);
        }
        if (keyBytes.length != KEY_LENGTH) {
            throw new IllegalStateException("locker.password-key는 32바이트(AES-256)여야 합니다. 지금: " + keyBytes.length + "바이트");
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    @Override
    public String encrypt(String password) {
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(IV_LENGTH + encrypted.length).put(iv).put(encrypted).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("사물함 비밀번호 암호화 실패", e);
        }
    }

    // 키가 다르거나 저장값이 바뀌었으면 인증 태그 검사에서 실패한다 → 500 C003
    @Override
    public String decrypt(String encrypted) {
        try {
            byte[] stored = Base64.getDecoder().decode(encrypted);
            if (stored.length <= IV_LENGTH) {
                throw new IllegalStateException("사물함 비밀번호 저장값 형식이 올바르지 않습니다");
            }
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, stored, 0, IV_LENGTH));
            byte[] plain = cipher.doFinal(stored, IV_LENGTH, stored.length - IV_LENGTH);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("사물함 비밀번호 복호화 실패 (키가 바뀌었거나 저장값이 손상됨)", e);
        }
    }
}
