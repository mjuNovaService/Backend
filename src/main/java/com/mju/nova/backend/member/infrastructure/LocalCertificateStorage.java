package com.mju.nova.backend.member.infrastructure;

import com.mju.nova.backend.member.application.CertificateStorage;
import com.mju.nova.backend.member.application.SignupProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 재학증명서를 서버 폴더(auth.signup.certificate-dir)에 저장한다.
 * TODO(로그인 파트): 배포 전에 AWS S3 비공개 버킷에 저장하는 S3CertificateStorage로 교체.
 *  서버 폴더는 재배포하면 파일이 사라질 수 있어 운영에서는 쓰지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalCertificateStorage implements CertificateStorage {

    private final SignupProperties properties;

    @Override
    public String store(String studentId, MultipartFile file) {
        // 파일 이름은 원래 이름 대신 무작위(UUID)로: 겹치지 않고, 원래 이름에 든 개인정보도 남기지 않음
        String key = "certificates/%s/%s.%s".formatted(studentId, UUID.randomUUID(), extensionOf(file));
        Path target = resolve(key);
        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(target.getParent());
            Files.copy(in, target);
        } catch (IOException e) {
            throw new IllegalStateException("재학증명서 저장에 실패했습니다", e);
        }
        log.info("재학증명서 저장: {}", key);
        return key;
    }

    @Override
    public Optional<Resource> load(String key) {
        Path path = resolve(key);
        // FileSystemResource: 파일을 통째로 메모리에 올리지 않고, 응답할 때 조금씩 읽어서 보냄
        return Files.isRegularFile(path) ? Optional.of(new FileSystemResource(path)) : Optional.empty();
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            log.warn("재학증명서 삭제 실패: {}", key, e);
        }
    }

    // 키 → 실제 파일 경로 (./uploads + certificates/학번/UUID.pdf)
    private Path resolve(String key) {
        return Path.of(properties.certificateDir()).resolve(key).normalize();
    }

    private String extensionOf(MultipartFile file) {
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        return extension == null ? "bin" : extension.toLowerCase(Locale.ROOT);
    }
}
