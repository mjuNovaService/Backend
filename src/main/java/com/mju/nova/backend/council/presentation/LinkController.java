package com.mju.nova.backend.council.presentation;

import com.mju.nova.backend.common.response.ApiResponse;
import com.mju.nova.backend.council.application.LinkQueryService;
import com.mju.nova.backend.council.application.dto.LinkResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "바로가기 링크", description = "외부 링크 목록 조회 (공개)")
@RestController
@RequestMapping("/api/links")
@RequiredArgsConstructor
public class LinkController {

    private final LinkQueryService linkQueryService;

    @Operation(summary = "링크 목록 조회", description = "등록 순서대로. 없으면 빈 배열.")
    @GetMapping
    public ApiResponse<List<LinkResponse>> getLinks() {
        return ApiResponse.success(linkQueryService.getLinks());
    }
}
