package com.mju.nova.backend.council.domain;

import com.mju.nova.backend.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uk_link_link_name", columnNames = {"link_name"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Link extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "link_id")
    private Long id;

    // 링크 구분 이름. 예: TRANSPARENCY, INSTAGRAM
    @Column(nullable = false, length = 50)
    private String linkName;

    @Column(nullable = false, length = 500)
    private String url;

    public static Link create(String linkName, String url) {
        Link link = new Link();
        link.linkName = linkName;
        link.url = url;
        return link;
    }

    // PATCH: null인 칸은 그대로 둔다
    public void update(String linkName, String url) {
        if (linkName != null) {
            this.linkName = linkName;
        }
        if (url != null) {
            this.url = url;
        }
    }
}
