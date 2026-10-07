package io.github.lyen.LogiTech;

import io.github.lyen.LogiTech.core.Register.Group;
import io.github.lyen.LogiTech.core.Register.Recipe;
import io.github.lyen.LogiTech.core.Register.RegisterBlocks;
import io.github.lyen.LogiTech.core.Register.RegisterItems;

import io.github.pylonmc.rebar.addon.RebarAddon;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Set;

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