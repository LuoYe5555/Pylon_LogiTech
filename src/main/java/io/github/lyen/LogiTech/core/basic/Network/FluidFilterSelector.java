package io.github.lyen.LogiTech.Core.Basic.Network;

import io.github.pylonmc.rebar.fluid.RebarFluid;
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder;
import io.github.pylonmc.rebar.registry.RebarRegistry;
import io.github.pylonmc.rebar.util.gui.GuiItems;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.gui.Markers;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.AbstractItem;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 流体种类选择界面（流体输入器/输出器共用）。
 * 列出所有已注册流体供直接点选，替代原来"左键一个个循环切换"的操作。
 * 选择后回调 onPick（null 表示不限），由调用方写入配置并重新打开设备主界面。
 */
public final class FluidFilterSelector {

    private FluidFilterSelector() {
    }

    /**
     * 打开流体选择界面。
     *
     * @param viewer  查看的玩家
     * @param current 当前已选过滤流体（null = 不限，仅用于高亮显示）
     * @param title   界面标题
     * @param onPick  选择回调，参数 null 表示"不限流体"
     */
    public static void open(@NotNull Player viewer,
                            @Nullable RebarFluid current,
                            @NotNull Component title,
                            @NotNull Consumer<@Nullable RebarFluid> onPick) {
        List<xyz.xenondevs.invui.item.Item> content = new ArrayList<>();
        for (RebarFluid fluid : RebarRegistry.FLUIDS.getValues()) {
            content.add(new FluidOptionItem(fluid, fluid.equals(current), onPick));
        }

        PagedGui<xyz.xenondevs.invui.item.Item> gui = PagedGui.itemsBuilder()
                .setStructure(
                        "# # # # # # # # #",
                        "x x x x x x x x x",
                        "x x x x x x x x x",
                        "x x x x x x x x x",
                        "x x x x x x x x x",
                        "A # # # # # # P N"
                )
                .addIngredient('#', GuiItems.background())
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('A', new AnyFluidOptionItem(current == null, onPick))
                .addIngredient('P', GuiItems.pagePrevious())
                .addIngredient('N', GuiItems.pageNext())
                .setContent(content)
                .build();

        Window.builder()
                .setUpperGui(gui)
                .setTitle(title)
                .setViewer(viewer)
                .build()
                .open();
    }

    /**
     * "不限流体"选项
     */
    private static class AnyFluidOptionItem extends AbstractItem {
        private final boolean selected;
        private final Consumer<@Nullable RebarFluid> onPick;

        AnyFluidOptionItem(boolean selected, @NotNull Consumer<@Nullable RebarFluid> onPick) {
            this.selected = selected;
            this.onPick = onPick;
        }

        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player viewer) {
            List<Component> lore = new ArrayList<>();
            if (selected) {
                lore.add(Component.text("§a当前已选: 不限流体"));
            }
            lore.add(Component.text("§7点击设置为不限流体"));
            lore.add(Component.text("§8任何流体都可以通过"));
            return ItemStackBuilder.of(Material.STRUCTURE_VOID)
                    .name(Component.text((selected ? "§a✔ " : "§e") + "不限流体种类"))
                    .lore(lore);
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull Click click) {
            if (clickType.isLeftClick() || clickType.isRightClick()) {
                onPick.accept(null);
            }
        }
    }

    /**
     * 某一种流体的选项
     */
    private static class FluidOptionItem extends AbstractItem {
        private final RebarFluid fluid;
        private final boolean selected;
        private final Consumer<@Nullable RebarFluid> onPick;

        FluidOptionItem(@NotNull RebarFluid fluid, boolean selected,
                        @NotNull Consumer<@Nullable RebarFluid> onPick) {
            this.fluid = fluid;
            this.selected = selected;
            this.onPick = onPick;
        }

        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player viewer) {
            ItemStack icon = fluid.getItem().clone();
            List<Component> lore = new ArrayList<>();
            if (selected) {
                lore.add(Component.text("§a当前已选"));
            }
            lore.add(Component.text("§7点击选择该流体"));
            return ItemStackBuilder.of(icon)
                    .name(Component.text((selected ? "§a✔ " : "§b") + fluid.getKey().getKey()))
                    .lore(lore);
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull Click click) {
            if (clickType.isLeftClick() || clickType.isRightClick()) {
                onPick.accept(fluid);
            }
        }
    }
}
