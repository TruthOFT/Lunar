package com.lunar.lunar_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("lunar_user")
public class LunarUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String account;

    private String password;

    private String nickname;

    private Integer tokenVersion;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDelete;
}
