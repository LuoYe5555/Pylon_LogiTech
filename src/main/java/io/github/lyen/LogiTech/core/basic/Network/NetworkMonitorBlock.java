package io.github.lyen.LogiTech.core.basic.Network;

import io.github.lyen.LogiTech.core.Storage.SingleItemStorageBlock;
import io.github.pylonmc.rebar.block.RebarBlock;
import io.github.pylonmc.rebar.block.base.RebarGuiBlock;
import io.github.pylonmc.rebar.block.context.BlockCreateContext;
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

import java.util.List;

/**
 * 网络监视器：只读监视设定方向上的量子存储，把其中的内容展示在监视格里。
 * GUI 内点击方位格子切换监视方向（内容是动态渲染的，切换立即生效），不支持存取操作。
 * 量子存储因此只需与监视器相邻即可接入网络。
 */
public class NetworkMonitorBlock extends NetworkNode implements RebarGuiBlock {

    public static class Item extends RebarItem {
        public Item(@NotNull ItemStack stack) {
            super(stack);
        }
    }

    private static final org.bukkit.NamespacedKey FACE_KEY =
            new org.bukkit.NamespacedKey("logitech", "monitor_face");

    private BlockFace face = BlockFace.UP;

    private Gui gui;

    public NetworkMonitorBlock(@NotNull Block block, @NotNull BlockCreateContext context) {
        super(block, context);
    }

    public NetworkMonitorBlock(@NotNull Block block, @NotNull PersistentDataContainer pdc) {
        super(block, pdc);
        face = DirectionItem.of(pdc.get(FACE_KEY, PersistentDataType.STRING));
    }

    @Override
    public void write(@NotNull PersistentDataContainer pdc) {
        pdc.set(FACE_KEY, PersistentDataType.STRING, face.name());
    }

    /**
     * 获取被监视方向上的量子存储（可能为 null）
     */
    public @Nullable SingleItemStorageBlock getMonitoredStorage() {
        Block target = getBlock().getRelative(face);
        if (!target.getWorld().isChunkLoaded(target.getX() >> 4, target.getZ() >> 4)) {
            return null;
        }
        RebarBlock rebar = RebarBlock.getRebarBlock(target);
        return rebar instanceof SingleItemStorageBlock storage ? storage : null;
    }

    @Override
    public @NotNull Gui createGui() {
        // 普通 3x9 界面（与推送器/抓取器同款方位布局）：F=监视内容、R=刷新
        var builder = Gui.builder()
                .setStructure(FaceSelectLayout.STRUCTURE_DISPLAY)
                .addIngredient('#', GuiItems.background())
                .addIngredient('F', new MonitoredDisplayItem())
                .addIngredient('R', new RefreshItem());
        FaceSelectLayout.addFaceItems(builder, getBlock(), this::getFace, this::setFace);
        gui = builder.build();
        return gui;
    }

    @Override
    public @NotNull Component getGuiTitle() {
        return Component.text("§8[§5网络监视器§8]");
    }

    /**
     * 监视内容展示：动态读取被监视量子存储（只读，不可取出）
     */
    private class MonitoredDisplayItem extends AbstractItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player viewer) {
            SingleItemStorageBlock storage = getMonitoredStorage();
            if (storage == null) {
                return ItemStackBuilder.of(Material.BARRIER)
                        .name(Component.text("§c未连接量子存储"))
                        .lore(List.of(
                                Component.text("§7当前方向: §b" + DirectionItem.displayName(getFace())),
                                Component.text("§7把量子存储放在监视器设定方向上"),
                                Component.text("§8点击右侧方位格子切换监视方向")));
            }
            ItemStack stored = storage.getStoredItem();
            long amount = storage.getStoredAmount();
            if (stored == null || amount <= 0) {
                return ItemStackBuilder.of(Material.GLASS_PANE)
                        .name(Component.text("§7量子存储为空"))
                        .lore(List.of(Component.text("§7监视方向: §b" + DirectionItem.displayName(getFace()))));
            }
            ItemStack display = stored.clone();
            display.setAmount((int) Math.max(1, Math.min(amount, stored.getMaxStackSize())));
            return ItemStackBuilder.of(display)
                    .lore(List.of(
                            Component.text("§7总量: §e" + amount),
                            Component.text("§8监视内容为只读"),
                            Component.text("§8存取请使用网络网格")));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull Click click) {
            // 只读：不允许取出
        }
    }

    /**
     * 刷新按钮：重新读取量子存储内容
     */
    private class RefreshItem extends AbstractItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player viewer) {
            return ItemStackBuilder.of(Material.SUNFLOWER)
                    .name(Component.text("§e刷新"))
                    .lore(List.of(Component.text("§7点击重新读取量子存储")));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull Click click) {
            if (gui != null) {
                gui.notifyWindows();
            }
        }
    }

    public BlockFace getFace() {
        return face;
    }

    public void setFace(@NotNull BlockFace face) {
        this.face = face;
        // 界面内容是动态渲染的，方位格子点击后已刷新所有窗口，这里无需额外处理
    }
}
