package com.lunar.lunar_backend.controller;

import com.lunar.lunar_backend.annotation.AuthCheck;
import com.lunar.lunar_backend.common.ApiResponse;
import com.lunar.lunar_backend.dto.AppVersionResponse;
import com.lunar.lunar_backend.dto.AppVersionSaveRequest;
import com.lunar.lunar_backend.service.AppVersionService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/app/version")
public class AppVersionController {

    @Resource
    private AppVersionService appVersionService;

    @GetMapping("/latest")
    public ApiResponse<AppVersionResponse> latest(@RequestParam(defaultValue = "android") String platform) {
        return ApiResponse.success(appVersionService.getLatest(platform));
    }

    @PostMapping
    @AuthCheck(role = "admin")
    public ApiResponse<Void> save(@RequestBody AppVersionSaveRequest request) {
        appVersionService.saveVersion(request);
        return ApiResponse.success(null);
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @AuthCheck(role = "admin")
    public ApiResponse<AppVersionResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "changelog", required = false) String changelog,
            @RequestParam(value = "forceUpdate", required = false) Boolean forceUpdate) {
        return ApiResponse.success(appVersionService.uploadApk(file, changelog, forceUpdate));
    }
}
