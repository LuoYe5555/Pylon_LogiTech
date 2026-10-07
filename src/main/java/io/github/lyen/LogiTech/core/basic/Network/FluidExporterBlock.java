package io.github.lyen.LogiTech.Core.Basic.Network;

import io.github.lyen.LogiTech.Core.Basic.SpecialItems.MemoryCard;
import io.github.pylonmc.rebar.block.interfaces.FluidRebarBlock;
import io.github.pylonmc.rebar.block.interfaces.GuiRebarBlock;
import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.pylonmc.rebar.fluid.FluidPointType;
import io.github.pylonmc.rebar.fluid.RebarFluid;
import io.github.pylonmc.rebar.item.RebarItem;
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder;
import io.github.pylonmc.rebar.util.gui.GuiItems;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.AbstractItem;
import xyz.xenondevs.invui.item.ItemProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * 流体输出器：把网络流体存储器（容量卡）中的流体供给 Rebar 流体管道。
 * 放置时自动在朝向玩家的一面生成流体 OUTPUT 点，用流体管道右键该面即可连接。
 * GUI 可循环设置允许通过的流体种类（不限 → 各注册流体）。
 */
public class FluidExporterBlock extends NetworkNode implements FluidRebarBlock, GuiRebarBlock {

    public static class Item extends RebarItem {
        public Item(@NotNull ItemStack stack) {
            super(stack);
        }
    }

    private static final org.bukkit.NamespacedKey FILTER_KEY =
            new org.bukkit.NamespacedKey("logitech", "fluid_exporter_filter");

    /** 允许供出的流体（null = 不限） */
    private RebarFluid filterFluid;

    private Gui gui;

    public FluidExporterBlock(@NotNull Block block, @NotNull BlockCreateContext context) {
        super(block, context);
        // 朝向玩家的一面是流体输出口（NORTH 在玩家参照系中即朝向放置者的正面）
        createFluidPoint(FluidPointType.OUTPUT, BlockFace.NORTH, context, false);
    }

    public FluidExporterBlock(@NotNull Block block, @NotNull PersistentDataContainer pdc) {
        super(block, pdc);
        String key = pdc.get(FILTER_KEY, PersistentDataType.STRING);
        if (key != null) {
            filterFluid = io.github.pylonmc.rebar.registry.RebarRegistry.FLUIDS
                    .get(org.bukkit.NamespacedKey.fromString(key));
        }
    }

    @Override
    public void write(@NotNull PersistentDataContainer pdc) {
        if (filterFluid != null) {
            pdc.set(FILTER_KEY, PersistentDataType.STRING, filterFluid.getKey().toString());
        }
    }

    /**
     * 供液查询：从本网络所有存储器（容量卡）的流体区汇总可用流体（按种类合并，单种限速）
     */
    @Override
    public @NotNull List<kotlin.Pair<RebarFluid, Double>> getSuppliedFluids() {
        // 同网络多个输出器共享卡内流体池，只让主设备上报，避免重复上报凭空产生流体
        if (!isPrimaryFluidDevice(FluidExporterBlock.class)) {
            return List.of();
        }
        // 按流体种类汇总可供应量（mB）。不再自限每次供出量：Rebar 结算时会按所连
        // 流体管道等级（fluid-per-second）统一封顶，上报实际存量即可，管道多快就供多快。
        var totals = new java.util.LinkedHashMap<RebarFluid, Double>();
        NetworkManager.Network network = getNetwork();
        for (io.github.lyen.LogiTech.Core.Basic.Storage.MemoryBlock memory : network.getMemories()) {
            List<MemoryCard.FluidEntry> entries = new ArrayList<>();
            memory.collectFluids(entries);
            for (MemoryCard.FluidEntry entry : entries) {
                if (filterFluid != null && !entry.fluid().equals(filterFluid)) {
                    continue;
                }
                totals.merge(entry.fluid(), entry.amountMb(), Double::sum);
            }
        }
        List<kotlin.Pair<RebarFluid, Double>> supply = new ArrayList<>();
        for (var e : totals.entrySet()) {
            supply.add(new kotlin.Pair<>(e.getKey(), e.getValue()));
        }
        return supply;
    }

    /**
     * 供液扣除：管道实际取走 fluid 时从各卡的存余中扣
     */
    @Override
    public void onFluidRemoved(@NotNull RebarFluid fluid, double amount) {
        NetworkManager.Network network = getNetwork();
        double remaining = amount;
        for (io.github.lyen.LogiTech.Core.Basic.Storage.MemoryBlock memory : network.getMemories()) {
            if (remaining <= 0) {
                break;
            }
            remaining -= memory.withdrawFluid(fluid, remaining);
        }
    }

    public @Nullable RebarFluid getFilterFluid() {
        return filterFluid;
    }

    public void setFilterFluid(@Nullable RebarFluid fluid) {
        this.filterFluid = fluid;
    }

    /**
     * 打开流体种类选择界面；选择后写回过滤器并重新打开设备主界面
     */
    public void openFilterSelector(@NotNull Player player) {
        FluidFilterSelector.open(player, filterFluid,
                Component.text("§8[§9选择流体§8]"), picked -> {
                    setFilterFluid(picked);
                    if (gui != null) {
                        gui.notifyWindows();
                    }
                    reopenGui(player);
                });
    }

    /**
     * 重新打开设备主界面（流体选择后调用）
     */
    private void reopenGui(@NotNull Player player) {
        if (gui == null) {
            createGui();
        }
        xyz.xenondevs.invui.window.Window.builder()
                .setUpperGui(gui)
                .setTitle(getGuiTitle())
                .setViewer(player)
                .build()
                .open();
    }

    @Override
    public @NotNull Gui createGui() {
        gui = Gui.builder()
                .setStructure(
                        "# # # # # # # # #",
                        "# # # # F # # # #",
                        "# # # # # # # # #"
                )
                .addIngredient('#', GuiItems.background())
                .addIngredient('F', new FilterStatusItem(this))
                .build();
        return gui;
    }

    @Override
    public @NotNull Component getGuiTitle() {
        return Component.text("§8[§9流体输出器§8]");
    }

    /**
     * 过滤状态显示项：点击打开流体选择界面
     */
    private class FilterStatusItem extends AbstractItem {
        private final FluidExporterBlock owner;

        FilterStatusItem(@NotNull FluidExporterBlock owner) {
            this.owner = owner;
        }

        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player viewer) {
            RebarFluid fluid = owner.getFilterFluid();
            if (fluid == null) {
                return ItemStackBuilder.of(Material.STRUCTURE_VOID)
                        .name(Component.text("§e不限流体种类"))
                        .lore(List.of(
                                Component.text("§7点击打开流体选择界面")));
            }
            return ItemStackBuilder.of(fluid.getItem())
                    .name(Component.text("§a只通过: §b" + fluid.getKey().getKey()))
                    .lore(List.of(
                            Component.text("§7点击打开流体选择界面"),
                            Component.text("§8可在列表中选择或改回不限")));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull Click click) {
            if (clickType.isLeftClick() || clickType.isRightClick()) {
                owner.openFilterSelector(player);
            }
        }
    }
}
