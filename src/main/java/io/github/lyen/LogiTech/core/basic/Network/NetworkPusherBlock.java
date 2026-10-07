package io.github.lyen.LogiTech.core.basic.Network;

import io.github.pylonmc.rebar.block.base.RebarGuiBlock;
import io.github.pylonmc.rebar.block.base.RebarTickingBlock;
import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.pylonmc.rebar.item.RebarItem;
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder;
import io.github.pylonmc.rebar.logistics.slot.LogisticSlot;
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
import xyz.xenondevs.invui.inventory.VirtualInventory;
import xyz.xenondevs.invui.item.AbstractItem;
import xyz.xenondevs.invui.item.ItemProvider;

import java.util.List;

/**
 * 网络推送器：周期性地把网络内的物品沿设定方向推送进相邻的机器/容器。
 * 右键打开 UI 可选择工作方向，并把样品物品放入过滤槽（物品不会消耗）；
 * 未设置过滤物品时推送任意物品。
 */
public class NetworkPusherBlock extends NetworkNode implements RebarTickingBlock, RebarGuiBlock {

    public static class Item extends RebarItem {
        public Item(@NotNull ItemStack stack) {
            super(stack);
        }
    }

    private static final org.bukkit.NamespacedKey FACE_KEY =
            new org.bukkit.NamespacedKey("logitech", "pusher_face");
    private static final org.bukkit.NamespacedKey FILTER_KEY =
            new org.bukkit.NamespacedKey("logitech", "pusher_filter");

    /** 每次传送的最大数量 */
    public static final int TRANSFER_RATE = 64;

    /** 工作间隔（tick），20 tick = 1 秒 */
    private static final int TICK_INTERVAL = 20;

    private BlockFace face = BlockFace.UP;
    /** 过滤样品；null 表示不限制类型。样品本身不属于网络物品，只是模板 */
    private ItemStack filterItem;

    private Gui gui;

    public NetworkPusherBlock(@NotNull Block block, @NotNull BlockCreateContext context) {
        super(block, context);
    }

    public NetworkPusherBlock(@NotNull Block block, @NotNull PersistentDataContainer pdc) {
        super(block, pdc);
        load(pdc);
    }

    private void load(@NotNull PersistentDataContainer pdc) {
        face = DirectionItem.of(pdc.get(FACE_KEY, PersistentDataType.STRING));
        String encoded = pdc.get(FILTER_KEY, PersistentDataType.STRING);
        if (encoded != null) {
            try {
                byte[] bytes = java.util.Base64.getDecoder().decode(encoded);
                try (var in = new java.io.ByteArrayInputStream(bytes);
                     var dataInput = new org.bukkit.util.io.BukkitObjectInputStream(in)) {
                    Object read = dataInput.readObject();
                    if (read instanceof ItemStack stack && !stack.getType().isAir()) {
                        filterItem = stack;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void write(@NotNull PersistentDataContainer pdc) {
        pdc.set(FACE_KEY, PersistentDataType.STRING, face.name());
        if (filterItem != null) {
            try {
                var bytes = new java.io.ByteArrayOutputStream();
                try (var dataOutput = new org.bukkit.util.io.BukkitObjectOutputStream(bytes)) {
                    dataOutput.writeObject(filterItem);
                }
                pdc.set(FILTER_KEY, PersistentDataType.STRING,
                        java.util.Base64.getEncoder().encodeToString(bytes.toByteArray()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void tick() {
        Block target = getBlock().getRelative(face);
        NetworkManager.Network network = getNetwork();
        if (network.isEmpty() || NetworkManager.isSameNetworkMember(target, network)) {
            return; // 空网络或目标是自己网络内的方块（自己搬自己）
        }
        List<LogisticSlot> slots = NetworkManager.getExternalSlots(target, true, network);
        if (slots.isEmpty()) {
            return;
        }
        pushOnce(network, slots);
    }

    /**
     * 尝试向目标槽位推送一份物品（受过滤样品限制）
     */
    private void pushOnce(@NotNull NetworkManager.Network network, @NotNull List<@NotNull LogisticSlot> slots) {
        for (LogisticSlot slot : slots) {
            ItemStack current = slot.getItemStack();
            long space;
            ItemStack wanted; // null 表示槽位为空，随便给什么都可以

            if (current == null || current.getType().isAir()) {
                space = 64;
                wanted = null;
            } else {
                wanted = current;
                space = slot.getMaxAmount(current) - slot.getAmount();
            }
            if (space <= 0) {
                continue;
            }
            if (!matchesFilter(wanted)) {
                continue;
            }

            int take = (int) Math.min(Math.min(TRANSFER_RATE, space), Integer.MAX_VALUE);
            ItemStack template = wanted != null ? wanted : filterItem;
            ItemStack withdrawn = network.withdraw(template, take);
            if (withdrawn == null || withdrawn.getAmount() <= 0) {
                continue;
            }

            if (wanted == null) {
                slot.set(withdrawn, withdrawn.getAmount());
            } else {
                slot.set(current, slot.getAmount() + withdrawn.getAmount());
            }
            return; // 每 tick 只传送一份
        }
    }

    /**
     * 过滤样品是否允许推送该类型
     */
    private boolean matchesFilter(@Nullable ItemStack target) {
        if (filterItem == null) {
            return true;
        }
        return target != null && target.isSimilar(filterItem);
    }

    @Override
    public @NotNull Gui createGui() {
        VirtualInventory filterInv = new VirtualInventory(1);
        filterInv.addPostUpdateHandler(event -> {
            // 玩家放入/取出样品后更新过滤并刷新显示
            ItemStack sample = filterInv.getItem(0);
            setFilterItem(sample == null || sample.getType().isAir() ? null : sample);
            notifyFilterChange();
        });
        var builder = Gui.builder()
                .setStructure(FaceSelectLayout.STRUCTURE)
                .addIngredient('#', GuiItems.background())
                .addIngredient('F', filterInv)
                .addIngredient('R', new FilterStatusItem());
        FaceSelectLayout.addFaceItems(builder, getBlock(), this::getFace, this::setFace);
        gui = builder.build();
        return gui;
    }

    private void notifyFilterChange() {
        if (gui != null) {
            gui.notifyWindows();
        }
    }

    /**
     * 过滤状态显示项
     */
    private class FilterStatusItem extends AbstractItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player viewer) {
            return filterDisplay();
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull Click click) {
        }
    }
    /**
     * 过滤状态显示文本
     */
    private @NotNull ItemProvider filterDisplay() {
        if (filterItem == null) {
            return ItemStackBuilder.of(Material.STRUCTURE_VOID)
                    .name(Component.text("§e未设置过滤"))
                    .lore(List.of(
                            Component.text("§7把样品物品放入上方过滤槽"),
                            Component.text("§7样品不会消耗，仅作为模板"),
                            Component.text("§7未设置时推送任意物品")));
        }
        return ItemStackBuilder.of(Material.PAPER)
                .name(Component.text("§a过滤: " + filterItem.getType().name()))
                .lore(List.of(
                        Component.text("§7只推送样品同类物品"),
                        Component.text("§8样品保存在过滤槽中")));
    }

    @Override
    public @NotNull Component getGuiTitle() {
        return Component.text("§8[§6网络推送器§8]");
    }

    public BlockFace getFace() {
        return face;
    }

    public void setFace(@NotNull BlockFace face) {
        this.face = face;
    }

    public @Nullable ItemStack getFilterItem() {
        return filterItem;
    }

    public void setFilterItem(@Nullable ItemStack item) {
        this.filterItem = item == null || item.getType().isAir() ? null : item.clone();
    }
}
