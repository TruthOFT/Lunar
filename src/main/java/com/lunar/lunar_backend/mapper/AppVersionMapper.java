package com.lunar.lunar_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lunar.lunar_backend.entity.AppVersion;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AppVersionMapper extends BaseMapper<AppVersion> {
}
