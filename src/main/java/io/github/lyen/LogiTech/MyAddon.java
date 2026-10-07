package io.github.lyen.LogiTech;

import io.github.lyen.LogiTech.Core.Basic.Listeners.RebarFooterFixListener;
import io.github.lyen.LogiTech.Core.Basic.Network.NetworkGridBlock;
import io.github.lyen.LogiTech.Core.Util.TickClock;
import io.github.lyen.LogiTech.Core.Register.Group;
import io.github.lyen.LogiTech.Core.Register.Recipe;
import io.github.lyen.LogiTech.Core.Register.RegisterBlocks;
import io.github.lyen.LogiTech.Core.Register.RegisterItems;
import io.github.pylonmc.rebar.addon.RebarAddon;
import org.bukkit.Material;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

@SuppressWarnings("unused")
public class MyAddon extends JavaPlugin implements RebarAddon {

    private static MyAddon instance;

    public static MyAddon getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        registerWithRebar();

        Group.initialize();
        RegisterItems.initialize();
        RegisterBlocks.initialize();
        Recipe.initialize();

        // 修复 Rebar 0.44 创造模式搬运导致物品页脚无限追加的问题
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new RebarFooterFixListener(), this);

        // 全局 tick 计数器：网络拓扑/聚合快照/卡片解析的同 tick 缓存依赖它判定有效期
        TickClock.start(this);
        // 网格自动刷新：每 0.5 秒让正打开的网格同步后台搬运/流体变化
        getServer().getScheduler().runTaskTimer(this,
                NetworkGridBlock::autoRefreshOpenGrids,
                NetworkGridBlock.AUTO_REFRESH_PERIOD_TICKS,
                NetworkGridBlock.AUTO_REFRESH_PERIOD_TICKS);
    }

    @Override
    public @NotNull JavaPlugin getJavaPlugin() {
        return this;
    }

    @Override
    public @NotNull Locale getDefaultLanguage() {
        return Locale.SIMPLIFIED_CHINESE;
    }

    @Override
    public @NotNull Material getMaterial() {
        return Material.AMETHYST_BLOCK;
    }
}