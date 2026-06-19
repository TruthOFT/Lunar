package com.lunar.lunar_backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lunar.lunar_backend.dto.AppVersionResponse;
import com.lunar.lunar_backend.dto.AppVersionSaveRequest;
import com.lunar.lunar_backend.entity.AppVersion;
import com.lunar.lunar_backend.exception.ApiException;
import com.lunar.lunar_backend.mapper.AppVersionMapper;
import com.lunar.lunar_backend.service.AppVersionService;
import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.ApkMeta;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class AppVersionServiceImpl extends ServiceImpl<AppVersionMapper, AppVersion> implements AppVersionService {

    @Value("${app.apk-dir:apk}")
    private String apkDir;

    @Value("${app.base-url:}")
    private String baseUrl;

    @Override
    public AppVersionResponse getLatest(String platform) {
        AppVersion version = lambdaQuery()
                .eq(AppVersion::getPlatform, platform)
                .orderByDesc(AppVersion::getVersionCode)
                .last("LIMIT 1")
                .one();
        if (version == null) return null;
        return new AppVersionResponse(
                version.getVersionCode(),
                version.getVersionName(),
                version.getDownloadUrl(),
                version.getChangelog(),
                version.getForceUpdate() != null && version.getForceUpdate() == 1
        );
    }

    @Override
    public void saveVersion(AppVersionSaveRequest request) {
        AppVersion existing = lambdaQuery()
                .eq(AppVersion::getPlatform, "android")
                .orderByDesc(AppVersion::getVersionCode)
                .last("LIMIT 1")
                .one();

        AppVersion version = existing != null ? existing : new AppVersion();
        version.setVersionCode(request.versionCode());
        version.setVersionName(request.versionName());
        version.setDownloadUrl(request.downloadUrl());
        version.setChangelog(request.changelog());
        version.setForceUpdate(Boolean.TRUE.equals(request.forceUpdate()) ? 1 : 0);
        version.setPlatform("android");

        saveOrUpdate(version);
    }

    @Override
    public AppVersionResponse uploadApk(MultipartFile file, String changelog, Boolean forceUpdate) {
        try {
            // Save to temp file for parsing
            Path tempFile = Files.createTempFile("upload_", ".apk");
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

            // Parse APK metadata
            ApkMeta meta;
            try (ApkFile apkFile = new ApkFile(tempFile.toFile())) {
                meta = apkFile.getApkMeta();
            }

            int versionCode = Math.toIntExact(meta.getVersionCode());
            String versionName = meta.getVersionName();

            // Save APK file using original uploaded filename
            String apkFileName = file.getOriginalFilename();
            if (apkFileName == null || apkFileName.isBlank()) apkFileName = "lunar.apk";
            Path apkPath = Paths.get(apkDir);
            Files.createDirectories(apkPath);
            Files.move(tempFile, apkPath.resolve(apkFileName), StandardCopyOption.REPLACE_EXISTING);

            // Build download URL
            String downloadUrl = baseUrl + "/api/apk/" + apkFileName;

            // Update DB
            AppVersion existing = lambdaQuery()
                    .eq(AppVersion::getPlatform, "android")
                    .orderByDesc(AppVersion::getVersionCode)
                    .last("LIMIT 1")
                    .one();

            AppVersion version = existing != null ? existing : new AppVersion();
            version.setVersionCode(versionCode);
            version.setVersionName(versionName);
            version.setDownloadUrl(downloadUrl);
            version.setChangelog(changelog != null ? changelog : "");
            version.setForceUpdate(Boolean.TRUE.equals(forceUpdate) ? 1 : 0);
            version.setPlatform("android");
            saveOrUpdate(version);

            return new AppVersionResponse(versionCode, versionName, downloadUrl,
                    version.getChangelog(), Boolean.TRUE.equals(forceUpdate));

        } catch (Exception e) {
            throw new ApiException(500, "APK 上传失败: " + e.getMessage());
        }
    }
}
