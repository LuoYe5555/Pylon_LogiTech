package io.github.lyen.LogiTech.Core.Basic.Network;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.NotNull;

/**
 * 六面方位布局工具：把 6 个方位格子按现实方位嵌入设备的 3x9 主 UI。
 * <pre>
 * # # # # # # 北 # 上
 * # # # # # 西 # 东 #
 * # # # # # # 南 # 下
 * </pre>
 * 各方位格子（FaceSelectItem）显示该面贴着的方块实物，点击即选定工作方向，
 * 与过滤槽等其它槽位互不干扰。使用方式：
 * <pre>
 * var builder = Gui.builder()
 *         .setStructure(FaceSelectLayout.STRUCTURE)
 *         .addIngredient('#', GuiItems.background())
 *         ...其它槽位;
 * FaceSelectLayout.addFaceItems(builder, getBlock(), this::getFace, this::setFace);
 * gui = builder.build();
 * </pre>
 */
public final class FaceSelectLayout {

    private FaceSelectLayout() {
    }

    /**
     * 六面方位 3x9 结构（右半区十字 + 最右列上下；其余字符可自行映射其它槽位）
     */
    public static final String[] STRUCTURE = {
            "# # # # # # N # U",
            "# # # # # W # E #",
            "# # # # # # S # D"
    };

    /**
     * 监视器用结构：在 STRUCTURE 基础上左侧留出 F（监视内容）与 R（刷新）两个槽位
     */
    public static final String[] STRUCTURE_DISPLAY = {
            "F # # # # # N # U",
            "# # # # # W # E #",
            "R # # # # # S # D"
    };

    /**
     * 向 builder 注册六个方位格子（N/S/W/E/U/D）
     */
    public static void addFaceItems(@NotNull Object builder, @NotNull Block block,
                                    @NotNull java.util.function.Supplier<BlockFace> getter,
                                    @NotNull java.util.function.Consumer<BlockFace> setter) {
        @SuppressWarnings("unchecked")
        xyz.xenondevs.invui.gui.Gui.Builder<xyz.xenondevs.invui.gui.Gui, ?> b =
                (xyz.xenondevs.invui.gui.Gui.Builder<xyz.xenondevs.invui.gui.Gui, ?>) builder;
        b.addIngredient('N', new FaceSelectItem(block, BlockFace.NORTH, getter, setter));
        b.addIngredient('S', new FaceSelectItem(block, BlockFace.SOUTH, getter, setter));
        b.addIngredient('W', new FaceSelectItem(block, BlockFace.WEST, getter, setter));
        b.addIngredient('E', new FaceSelectItem(block, BlockFace.EAST, getter, setter));
        b.addIngredient('U', new FaceSelectItem(block, BlockFace.UP, getter, setter));
        b.addIngredient('D', new FaceSelectItem(block, BlockFace.DOWN, getter, setter));
    }
}
