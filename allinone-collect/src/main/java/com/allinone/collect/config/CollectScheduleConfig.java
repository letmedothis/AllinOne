package com.allinone.collect.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 开启 Spring @Scheduled 支持：collect 模块的导出文件清理等轻量定时任务
 * 不走 Quartz（无需可调度的运维入口与持久化触发记录）。
 */
@Configuration
@EnableScheduling
public class CollectScheduleConfig {
}
