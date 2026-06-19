package com.lunar.lunar_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("app_version")
public class AppVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer versionCode;

    private String versionName;

    private String downloadUrl;

    private String changelog;

    private Integer forceUpdate;

    private String platform;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDelete;
}
