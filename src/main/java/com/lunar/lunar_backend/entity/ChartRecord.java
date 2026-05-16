package com.lunar.lunar_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("chart_record")
public class ChartRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String chartName;

    private String gender;

    private String birthTime;

    private String resultJson;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDelete;
}
