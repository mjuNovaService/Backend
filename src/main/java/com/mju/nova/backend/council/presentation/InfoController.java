package com.mju.nova.backend.council.presentation;

import com.mju.nova.backend.common.response.ApiResponse;
import com.mju.nova.backend.council.application.InfoQueryService;
import com.mju.nova.backend.council.application.dto.InfoResponse;
import com.mju.nova.backend.council.domain.InfoType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "학생회 소개", description = "소개·공약·조직도·회칙·FAQ 조회 (공개)")
@RestController
@RequestMapping("/api/infos")
@RequiredArgsConstructor
public class InfoController {

    private final InfoQueryService infoQueryService;

    @Operation(summary = "학생회 소개 글 조회",
            description = "type(INTRO, PLEDGE, ORG_CHART, RULES, FAQ)별 글 목록. 작성 순서대로. 없으면 빈 배열.")
    @GetMapping
    public ApiResponse<List<InfoResponse>> getInfos(
            // "PLEDGE" 같은 문자열을 Spring이 InfoType enum으로 바꿔준다. 없는 값이면 400 C001
            @RequestParam InfoType type) {
        return ApiResponse.success(infoQueryService.getInfos(type));
    }
}
