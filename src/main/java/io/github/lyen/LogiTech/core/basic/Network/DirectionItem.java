package io.github.lyen.LogiTech.Core.Basic.Network;

import io.github.pylonmc.rebar.item.builder.ItemStackBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.item.AbstractItem;
import xyz.xenondevs.invui.item.ItemProvider;

import java.util.List;
import java.util.function.Consumer;

/**
 * 共享的方向选择按钮：点击循环切换 上/下/北/南/西/东。
 * 网络推送器、抓取器、监视器共用。
 */
public final class DirectionItem extends AbstractItem {

    /** 六个可选方向，按循环顺序 */
    public static final BlockFace[] DIRECTIONS = {
            BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST
    };

    private final java.util.function.Supplier<BlockFace> getter;
    private final Consumer<BlockFace> setter;

    public DirectionItem(@NotNull java.util.function.Supplier<BlockFace> getter, @NotNull Consumer<BlockFace> setter) {
        this.getter = getter;
        this.setter = setter;
    }

    /**
     * 将 BlockFace 解析为配置存储用的短名
     */
    public static String name(@NotNull BlockFace face) {
        return face.name();
    }

    /**
     * 从配置短名还原 BlockFace，非法值回退为 UP
     */
    public static BlockFace of(@Nullable String name) {
        if (name != null) {
            for (BlockFace face : DIRECTIONS) {
                if (face.name().equals(name)) {
                    return face;
                }
            }
        }
        return BlockFace.UP;
    }

    public static String displayName(@NotNull BlockFace face) {
        return switch (face) {
            case UP -> "↑ 上";
            case DOWN -> "↓ 下";
            case NORTH -> "⬆ 北 (-Z)";
            case SOUTH -> "⬇ 南 (+Z)";
            case WEST -> "⬅ 西 (-X)";
            case EAST -> "➡ 东 (+X)";
            default -> face.name();
        };
    }

    public static Material icon(@NotNull BlockFace face) {
        return switch (face) {
            case UP -> Material.SPYGLASS;
            case DOWN -> Material.LEAD;
            case NORTH -> Material.COMPASS;
            case SOUTH -> Material.RECOVERY_COMPASS;
            case WEST -> Material.CLOCK;
            case EAST -> Material.NAME_TAG;
            default -> Material.PAPER;
        };
    }

    /**
     * 获取方块指定朝向的相邻方块
     */
    public static Block relative(@NotNull Block block, @NotNull BlockFace face) {
        return block.getRelative(face);
    }

    @Override
    public @NotNull ItemProvider getItemProvider(@NotNull Player viewer) {
        BlockFace face = getter.get();
        return ItemStackBuilder.of(icon(face))
                .name(Component.text("§e工作方向: §b" + displayName(face)))
                .lore(List.of(
                        Component.text("§7点击切换方向"),
                        Component.text("§8对所有接入网络的设备生效")));
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull Click click) {
        BlockFace current = getter.get();
        BlockFace next = DIRECTIONS[(current.ordinal() % DIRECTIONS.length + 1) % DIRECTIONS.length];
        setter.accept(next);
        // 立即刷新该玩家打开中的界面
        xyz.xenondevs.invui.window.Window window = xyz.xenondevs.invui.window.WindowManager.getInstance().getOpenWindow(player);
        if (window != null) {
            for (xyz.xenondevs.invui.gui.Gui gui : window.getGuis()) {
                gui.notifyWindows();
            }
        }
    }
}
