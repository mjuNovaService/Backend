package com.mju.nova.backend.council.presentation;

import com.mju.nova.backend.common.response.ApiResponse;
import com.mju.nova.backend.council.application.LinkCommandService;
import com.mju.nova.backend.council.application.dto.LinkIdResponse;
import com.mju.nova.backend.council.presentation.dto.LinkCreateRequest;
import com.mju.nova.backend.council.presentation.dto.LinkUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "바로가기 링크 (관리자)", description = "링크 등록·수정·삭제")
@RestController
@RequestMapping("/api/admin/links")
@RequiredArgsConstructor
public class LinkAdminController {

    private final LinkCommandService linkCommandService;

    @Operation(summary = "링크 등록", description = "링크 이름은 중복될 수 없다 (409 LK002).")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LinkIdResponse> create(@RequestBody @Valid LinkCreateRequest request) {
        return ApiResponse.success(linkCommandService.create(request.linkName(), request.url()));
    }

    @Operation(summary = "링크 주소 수정", description = "url만 바꾼다. 이름을 바꾸려면 삭제 후 다시 등록.")
    @PatchMapping("/{linkId}")
    public ApiResponse<LinkIdResponse> update(@PathVariable Long linkId,
                                              @RequestBody @Valid LinkUpdateRequest request) {
        return ApiResponse.success(linkCommandService.update(linkId, request.url()));
    }

    @Operation(summary = "링크 삭제")
    @DeleteMapping("/{linkId}")
    public ApiResponse<Void> delete(@PathVariable Long linkId) {
        linkCommandService.delete(linkId);
        return ApiResponse.success();
    }
}
