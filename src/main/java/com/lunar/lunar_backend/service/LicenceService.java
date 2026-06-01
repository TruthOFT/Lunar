package com.lunar.lunar_backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lunar.lunar_backend.dto.LicenceActivateRequest;
import com.lunar.lunar_backend.dto.LicenceActivateResponse;
import com.lunar.lunar_backend.dto.LicenceGenerateRequest;
import com.lunar.lunar_backend.dto.LicenceResponse;
import com.lunar.lunar_backend.dto.LicenceVipStatus;
import com.lunar.lunar_backend.entity.Licence;
import java.util.List;

public interface LicenceService extends IService<Licence> {

    List<LicenceResponse> generate(LicenceGenerateRequest request);

    LicenceActivateResponse activate(Long userId, LicenceActivateRequest request);

    LicenceVipStatus currentVipStatus(Long userId);
}
