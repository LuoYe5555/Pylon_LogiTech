package io.github.lyen.LogiTech.Core.Basic.Network;

import io.github.pylonmc.rebar.block.RebarBlock;
import io.github.pylonmc.rebar.i18n.RebarTranslator;
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.item.AbstractItem;
import xyz.xenondevs.invui.item.ItemProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 六面方位格子（嵌入设备主 UI 用）：显示设备该面贴着的方块实物。
 * Rebar 方块显示其物品名 并跟随其自带 lore；
 * 点击即把工作方向设为该面。
 */
final class FaceSelectItem extends AbstractItem {

    private final Block block;
    private final BlockFace face;
    private final java.util.function.Supplier<BlockFace> getter;
    private final Consumer<BlockFace> setter;

    FaceSelectItem(@NotNull Block block, @NotNull BlockFace face,
                   @NotNull java.util.function.Supplier<BlockFace> getter,
                   @NotNull Consumer<BlockFace> setter) {
        this.block = block;
        this.face = face;
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public @NotNull ItemProvider getItemProvider(@NotNull Player viewer) {
        BlockFace current = getter.get();
        Block neighbor = block.getRelative(face);
        boolean loaded = neighbor.getWorld().isChunkLoaded(neighbor.getX() >> 4, neighbor.getZ() >> 4);
        ItemStack icon;
        if (!loaded) {
            icon = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        } else {
            icon = neighborItem(neighbor, viewer);
        }
        if (icon.getType().isAir()) {
            icon = new ItemStack(Material.STRUCTURE_VOID);
        }
        // 关键：图标可能自带 lore（Rebar 物品的翻译 lore），不清空会和我们追加的说明 lore 叠成两遍
        icon.setData(io.papermc.paper.datacomponent.DataComponentTypes.LORE,
                io.papermc.paper.datacomponent.item.ItemLore.lore());

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(face == current ? "§a✔ 当前方向" : "§7点击选择"));
        // 方块名 + 自带 lore（空气/未加载没有额外信息）
        if (loaded && !neighbor.getType().isAir()) {
            lore.add(Component.text("§f" + neighborName(neighbor, viewer)));
            lore.addAll(neighborLore(neighbor, viewer));
        }
        // 被选中的当前方向：图标加附魔光效，一眼可辨
        var provider = ItemStackBuilder.of(icon)
                .name(Component.text("§e" + DirectionItem.displayName(face)))
                .lore(lore);
        if (face == current) {
            provider.set(io.papermc.paper.datacomponent.DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        return provider;
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull Click click) {
        setter.accept(face);
        // 点击后实时刷新所有打开中的窗口（更新 ✔ 标记）
        var window = xyz.xenondevs.invui.window.WindowManager.getInstance().getOpenWindow(player);
        if (window != null) {
            for (var gui : window.getGuis()) {
                gui.notifyWindows();
            }
        }
    }

    /**
     * 邻居方块的可显示物品：Rebar 方块用其拾取物（带翻译键名），原版用方块本身
     */
    private static ItemStack neighborItem(@NotNull Block neighbor, @NotNull Player viewer) {
        RebarBlock rebar = RebarBlock.getRebarBlock(neighbor);
        if (rebar != null) {
            ItemStack pick = rebar.getPickItem(viewer);
            if (pick != null) {
                return pick;
            }
        }
        return new ItemStack(neighbor.getType());
    }

    /**
     * 邻居方块的显示名：用 Rebar 的物品翻译流程在服务端渲染成文字。
     * Rebar 物品渲染出物品名，原版方块渲染出本地化方块名。
     */
    private static String neighborName(@NotNull Block neighbor, @NotNull Player viewer) {
        ItemStack info = neighborItem(neighbor, viewer).clone();
        try {
            RebarTranslator.translateItem(info, viewer);
        } catch (Exception ignored) {
        }
        Component name = info.getData(DataComponentTypes.ITEM_NAME);
        if (name != null) {
            try {
                String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                        .serialize(GlobalTranslator.render(name, java.util.Locale.CHINA));
                if (!plain.isBlank() && !plain.contains(".")) {
                    return plain;
                }
            } catch (Exception ignored) {
            }
        }
        // 渲染失败（如缺失翻译）时回退为原版方块名
        try {
            String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(GlobalTranslator.render(Component.translatable(neighbor.getType()),
                            java.util.Locale.CHINA));
            if (!plain.isBlank()) {
                return plain;
            }
        } catch (Exception ignored) {
        }
        return neighbor.getType().name();
    }

    /**
     * 邻居方块物品自带的 lore（Rebar 物品的翻译键 lore 也会被渲染成文字）
     */
    private static List<Component> neighborLore(@NotNull Block neighbor, @NotNull Player viewer) {
        ItemStack info = neighborItem(neighbor, viewer).clone();
        try {
            RebarTranslator.translateItem(info, viewer);
        } catch (Exception ignored) {
        }
        ItemLore itemLore = info.getData(DataComponentTypes.LORE);
        if (itemLore == null) {
            return List.of();
        }
        List<Component> rendered = new ArrayList<>();
        for (Component line : itemLore.lines()) {
            try {
                rendered.add(GlobalTranslator.render(line, java.util.Locale.CHINA));
            } catch (Exception e) {
                rendered.add(line);
            }
        }
        return rendered;
    }
}
