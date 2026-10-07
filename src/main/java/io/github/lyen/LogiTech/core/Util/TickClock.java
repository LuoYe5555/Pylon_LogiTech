package io.github.lyen.LogiTech.Core.Util;

import org.bukkit.plugin.Plugin;

/**
 * 全局服务器 tick 计数器：供同 tick 缓存（网络拓扑、聚合快照、卡片解析）判定有效期。
 * 缓存只在当前 tick 内有效，下一 tick 的第一次访问自动重算，兼顾性能与跨 tick 正确性。
 */
public final class TickClock {

    private static volatile int currentTick;

    private TickClock() {
    }

    /** 启动每 tick 自增任务（插件启用时调用一次） */
    public static void start(@org.jetbrains.annotations.NotNull Plugin plugin) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> currentTick++, 1L, 1L);
    }

    /** 当前服务器 tick */
    public static int current() {
        return currentTick;
    }
}
