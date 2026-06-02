package com.lunar.lunar_backend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lunar.lunar_backend.dto.LicenceActivateRequest;
import com.lunar.lunar_backend.dto.LicenceActivateResponse;
import com.lunar.lunar_backend.dto.LicenceAdminResponse;
import com.lunar.lunar_backend.dto.LicenceGenerateRequest;
import com.lunar.lunar_backend.dto.LicenceResponse;
import com.lunar.lunar_backend.dto.LicenceUpdateRequest;
import com.lunar.lunar_backend.dto.LicenceVipStatus;
import com.lunar.lunar_backend.entity.Licence;
import java.util.List;

public interface LicenceService extends IService<Licence> {

    List<LicenceResponse> generate(LicenceGenerateRequest request);

    LicenceActivateResponse activate(Long userId, LicenceActivateRequest request);

    LicenceVipStatus currentVipStatus(Long userId);

    IPage<LicenceAdminResponse> listLicences(Integer page, Integer pageSize, Integer status);

    void updateLicence(Long id, LicenceUpdateRequest request);

    void deleteLicence(Long id);
}
