package com.lunar.lunar_backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("kingdict")
public class Kingdict {

    @TableId
    private String zi;

    private Integer number;

    private String wuxing;
}
