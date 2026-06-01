package com.lunar.lunar_backend.controller;

import com.lunar.lunar_backend.annotation.AuthCheck;
import com.lunar.lunar_backend.common.ApiResponse;
import com.lunar.lunar_backend.common.AuthContext;
import com.lunar.lunar_backend.common.AuthUser;
import com.lunar.lunar_backend.dto.LicenceActivateRequest;
import com.lunar.lunar_backend.dto.LicenceActivateResponse;
import com.lunar.lunar_backend.dto.LicenceGenerateRequest;
import com.lunar.lunar_backend.dto.LicenceResponse;
import com.lunar.lunar_backend.service.LicenceService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/licences")
public class LicenceController {

    @Resource
    private LicenceService licenceService;

    @PostMapping("/generate")
    @AuthCheck(role = "admin")
    public ApiResponse<List<LicenceResponse>> generate(@RequestBody LicenceGenerateRequest requestBody) {
        return ApiResponse.success(licenceService.generate(requestBody));
    }

    @PostMapping("/activate")
    public ApiResponse<LicenceActivateResponse> activate(@RequestBody LicenceActivateRequest requestBody,
                                                         HttpServletRequest request) {
        AuthUser authUser = (AuthUser) request.getAttribute(AuthContext.REQUEST_ATTRIBUTE);
        return ApiResponse.success(licenceService.activate(authUser.userId(), requestBody));
    }
}
