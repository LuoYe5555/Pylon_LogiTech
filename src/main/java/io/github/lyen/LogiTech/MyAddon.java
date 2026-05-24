package io.github.lyen.LogiTech;

import io.github.lyen.LogiTech.core.CustomCraftingManager;
import io.github.lyen.LogiTech.core.GuideRecipeDisplay;
import io.github.lyen.LogiTech.core.Register.Group;
import io.github.lyen.LogiTech.core.Register.Recipe;
import io.github.lyen.LogiTech.core.Register.RegisterBlocks;
import io.github.lyen.LogiTech.core.Register.RegisterItems;
import io.github.pylonmc.rebar.addon.RebarAddon;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Set;

@SuppressWarnings("unused")
public class MyAddon extends JavaPlugin implements RebarAddon {

    // Stores the instance of the addon (there's only ever one)
    @Getter private static MyAddon instance;

    // Called when the addon is enabled
    @Override
    public void onEnable() {
        instance = this;

        // Every Rebar addon must call this BEFORE doing anything Rebar-related
        registerWithRebar();

        Group.initialize();
        RegisterItems.initialize();
        RegisterBlocks.initialize();
        Recipe.initialize();
        
        // 初始化自定义合成系统
        new CustomCraftingManager();
        
        // 初始化指南书配方显示
        new GuideRecipeDisplay();
    }

    @Override
    public @NotNull JavaPlugin getJavaPlugin() {
        return this;
    }

    @Override
    public @NotNull Set<@NotNull Locale> getLanguages() {
        return Set.of(Locale.SIMPLIFIED_CHINESE);
    }

    @Override
    public @NotNull Material getMaterial() {
        return Material.AMETHYST_BLOCK;
    }
}
