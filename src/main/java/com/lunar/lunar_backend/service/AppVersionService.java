package com.lunar.lunar_backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lunar.lunar_backend.dto.AppVersionResponse;
import com.lunar.lunar_backend.dto.AppVersionSaveRequest;
import com.lunar.lunar_backend.entity.AppVersion;
import org.springframework.web.multipart.MultipartFile;

public interface AppVersionService extends IService<AppVersion> {

    AppVersionResponse getLatest(String platform);

    void saveVersion(AppVersionSaveRequest request);

    AppVersionResponse uploadApk(MultipartFile file, String changelog, Boolean forceUpdate);
}
