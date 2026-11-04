package com.mju.nova.backend.locker.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class AesLockerPasswordCipherTest {

    static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);
    static final String OTHER_KEY = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    AesLockerPasswordCipher cipher = new AesLockerPasswordCipher(KEY);

    @Test
    void 암호화한_값을_복호화하면_원래_비밀번호() {
        String encrypted = cipher.encrypt("0420");

        assertThat(encrypted).doesNotContain("0420");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("0420");
    }

    @Test
    void 같은_비밀번호도_IV가_달라_저장값이_매번_다르다() {
        String first = cipher.encrypt("0420");
        String second = cipher.encrypt("0420");

        assertThat(first).isNotEqualTo(second);
        assertThat(Base64.getDecoder().decode(first)).hasSize(12 + 4 + 16);
        assertThat(first).hasSize(44);
        assertThat(cipher.decrypt(second)).isEqualTo("0420");
    }

    @Test
    void 다른_키로는_복호화할_수_없다() {
        String encrypted = new AesLockerPasswordCipher(OTHER_KEY).encrypt("0420");

        assertThatThrownBy(() -> cipher.decrypt(encrypted)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 저장값이_바뀌면_복호화에_실패한다() {
        byte[] stored = Base64.getDecoder().decode(cipher.encrypt("0420"));
        stored[stored.length - 1] ^= 1;

        assertThatThrownBy(() -> cipher.decrypt(Base64.getEncoder().encodeToString(stored)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 키가_32바이트가_아니면_서버를_켤_때_실패한다() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> new AesLockerPasswordCipher(shortKey)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new AesLockerPasswordCipher("not-base64!")).isInstanceOf(IllegalStateException.class);
    }
}
