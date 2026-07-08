package com.lunar.lunar_backend.controller;

import com.lunar.lunar_backend.common.ApiResponse;
import com.lunar.lunar_backend.dto.NameWuxingResponse;
import com.lunar.lunar_backend.service.KingdictService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dict")
public class DictController {

    @Resource
    private KingdictService kingdictService;

    @GetMapping("/name-wuxing")
    public ApiResponse<NameWuxingResponse> nameWuxing(@RequestParam String name) {
        String normalizedName = name == null ? "" : name.trim();
        return ApiResponse.success(new NameWuxingResponse(
                normalizedName,
                kingdictService.formatNameWuxing(normalizedName)
        ));
    }
}
