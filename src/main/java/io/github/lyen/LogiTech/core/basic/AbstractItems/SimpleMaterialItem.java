package io.github.lyen.LogiTech.Core.Basic.AbstractItems;

import io.github.pylonmc.rebar.item.RebarItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 简单合成材料物品类
 * 用于没有任何特殊功能的合成材料
 */
public class SimpleMaterialItem extends RebarItem {

    private final Material material;

    /**
     * 从世界中的物品堆恢复（Rebar 注册要求有此构造函数）
     */
    public SimpleMaterialItem(@NotNull ItemStack stack) {
        super(stack);
        this.material = stack.getType();
    }

    public SimpleMaterialItem(@NotNull Material material) {
        super(new ItemStack(material));
        this.material = material;
    }

    public SimpleMaterialItem(@NotNull Material material, @NotNull String name, @NotNull String... lore) {
        super(createItemStack(material, name, lore));
        this.material = material;
    }

    private static ItemStack createItemStack(@NotNull Material material, @NotNull String name, @NotNull String... lore) {
        ItemStack item = new ItemStack(material);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text(name).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            if (lore.length > 0) {
                java.util.List<net.kyori.adventure.text.Component> loreList = new java.util.ArrayList<>();
                for (String line : lore) {
                    loreList.add(net.kyori.adventure.text.Component.text(line).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
                }
                meta.lore(loreList);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public @NotNull Material getMaterial() {
        return material;
    }

    /**
     * 创建一个简单的材料物品
     */
    public static ItemStack createSimpleItem(@NotNull Material material, @NotNull String name, @NotNull String... lore) {
        ItemStack item = new ItemStack(material);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text(name).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            if (lore.length > 0) {
                java.util.List<net.kyori.adventure.text.Component> loreList = new java.util.ArrayList<>();
                for (String line : lore) {
                    loreList.add(net.kyori.adventure.text.Component.text(line).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
                }
                meta.lore(loreList);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
