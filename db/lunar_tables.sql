CREATE TABLE IF NOT EXISTS `lunar_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `account` varchar(32) NOT NULL COMMENT '账号',
  `password` varchar(64) NOT NULL COMMENT 'MD5加盐密码',
  `nickname` varchar(64) NOT NULL DEFAULT '' COMMENT '昵称',
  `tokenVersion` int NOT NULL DEFAULT 1 COMMENT 'token版本',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account` (`account`),
  KEY `idx_isDelete` (`isDelete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `chart_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `userId` bigint NOT NULL COMMENT '用户id',
  `title` varchar(128) NOT NULL DEFAULT '' COMMENT '记录标题',
  `chartName` varchar(64) NOT NULL DEFAULT '' COMMENT '排盘姓名',
  `gender` varchar(16) NOT NULL DEFAULT '' COMMENT '性别',
  `birthTime` varchar(32) NOT NULL DEFAULT '' COMMENT '出生时间',
  `resultJson` longtext NOT NULL COMMENT '排盘接口返回json',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_userId_createTime` (`userId`, `createTime`),
  KEY `idx_isDelete` (`isDelete`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排盘记录表';
