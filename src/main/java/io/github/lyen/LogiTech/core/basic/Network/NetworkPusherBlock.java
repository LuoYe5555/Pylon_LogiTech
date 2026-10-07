package io.github.lyen.LogiTech.Core.Basic.Network;

import io.github.pylonmc.rebar.block.interfaces.GuiRebarBlock;
import io.github.pylonmc.rebar.block.interfaces.TickingRebarBlock;
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

import java.util.ArrayList;
import java.util.List;

/**
 * 网络推送器：周期性地把网络内的物品沿设定方向推送进相邻的机器/容器。
 * 右键打开 UI：左侧 3×3 格子放过滤样品（物品不会消耗），并可选择工作方向。
 * 【必须放入至少一个样品】才会推送，没放任何样品时推送器不工作；放多个样品时，
 * 槽内已有同类物品按样品匹配，空槽则按样品顺序尝试送出。
 */
public class NetworkPusherBlock extends NetworkNode implements TickingRebarBlock, GuiRebarBlock {

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

    /** 过滤槽数量（左侧 3×3） */
    private static final int FILTER_SLOT_COUNT = 9;

    /**
     * 推送器专用 3x9 结构：左侧第 2-4 列三行为 3×3 过滤槽 F，i 为状态说明，
     * 右侧六格保留六面方位选择（与 FaceSelectLayout 相同位置）。
     */
    private static final String[] STRUCTURE = {
            "# F F F # # N # U",
            "# F F F # W # E #",
            "# F F F # # S # D"
    };

    private BlockFace face = BlockFace.UP;
    /** 过滤样品列表；为空表示未设置任何过滤，此时推送器不工作。样品只是模板，不属于网络物品 */
    private final List<ItemStack> filterItems = new ArrayList<>();

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
                    if (read instanceof ItemStack[] arr) {
                        for (ItemStack stack : arr) {
                            if (stack != null && !stack.getType().isAir()) {
                                filterItems.add(stack);
                            }
                        }
                    } else if (read instanceof ItemStack stack && !stack.getType().isAir()) {
                        // 兼容旧版本：只有一个样品
                        filterItems.add(stack);
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
        if (filterItems.isEmpty()) {
            pdc.remove(FILTER_KEY);
            return;
        }
        try {
            var bytes = new java.io.ByteArrayOutputStream();
            try (var dataOutput = new org.bukkit.util.io.BukkitObjectOutputStream(bytes)) {
                dataOutput.writeObject(filterItems.toArray(new ItemStack[0]));
            }
            pdc.set(FILTER_KEY, PersistentDataType.STRING,
                    java.util.Base64.getEncoder().encodeToString(bytes.toByteArray()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void tick() {
        // 没放任何样品时不推送任何东西
        if (filterItems.isEmpty()) {
            return;
        }
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
     * 尝试向目标槽位推送一份物品（只推送过滤样品内的类型）
     */
    private void pushOnce(@NotNull NetworkManager.Network network, @NotNull List<@NotNull LogisticSlot> slots) {
        for (LogisticSlot slot : slots) {
            ItemStack current = slot.getItemStack();
            long space;
            boolean empty = current == null || current.getType().isAir();

            if (empty) {
                space = 64;
            } else {
                if (!matchesFilter(current)) {
                    continue; // 槽里已有物品但不是任何样品类型：跳过，不能污染该槽
                }
                space = slot.getMaxAmount(current) - slot.getAmount();
            }
            if (space <= 0) {
                continue;
            }

            int take = (int) Math.min(Math.min(TRANSFER_RATE, space), Integer.MAX_VALUE);

            ItemStack withdrawn;
            if (empty) {
                // 空槽：按样品顺序依次尝试从网络取出
                withdrawn = null;
                for (ItemStack sample : filterItems) {
                    withdrawn = network.withdraw(sample, take);
                    if (withdrawn != null && withdrawn.getAmount() > 0) {
                        break;
                    }
                }
            } else {
                // 非空且已匹配：取同类
                withdrawn = network.withdraw(current, take);
            }
            if (withdrawn == null || withdrawn.getAmount() <= 0) {
                continue;
            }

            if (empty) {
                slot.set(withdrawn, withdrawn.getAmount());
            } else {
                slot.set(current, slot.getAmount() + withdrawn.getAmount());
            }
            return; // 每 tick 只传送一份
        }
    }

    /**
     * 目标物品是否匹配某个过滤样品
     */
    private boolean matchesFilter(@Nullable ItemStack target) {
        if (target == null || target.getType().isAir()) {
            return false;
        }
        for (ItemStack sample : filterItems) {
            if (target.isSimilar(sample)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @NotNull Gui createGui() {
        VirtualInventory filterInv = new VirtualInventory(FILTER_SLOT_COUNT);
        // 还原已保存的样品（GUI 每次打开都会重建；先装填再挂变更处理器，避免无谓触发）
        for (int i = 0; i < filterItems.size() && i < FILTER_SLOT_COUNT; i++) {
            filterInv.setItem(null, i, filterItems.get(i).clone());
        }
        filterInv.addPostUpdateHandler(event -> {
            // 玩家放入/取出样品后重建过滤列表并刷新说明项
            filterItems.clear();
            for (ItemStack stack : filterInv.getItems()) {
                if (stack != null && !stack.getType().isAir()) {
                    filterItems.add(stack.clone());
                }
            }
            notifyFilterChange();
        });
        var builder = Gui.builder()
                .setStructure(STRUCTURE)
                .addIngredient('#', GuiItems.background())
                .addIngredient('i', new FilterStatusItem())
                .addIngredient('F', filterInv);
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
     * 过滤状态说明项（左上角）
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
        if (filterItems.isEmpty()) {
            return ItemStackBuilder.of(Material.STRUCTURE_VOID)
                    .name(Component.text("§c未放入样品"))
                    .lore(List.of(
                            Component.text("§7把样品放入右侧 3×3 格子"),
                            Component.text("§7样品不会消耗，仅作为模板"),
                            Component.text("§c没有样品时推送器不工作")));
        }
        var lines = new java.util.ArrayList<Component>();
        lines.add(Component.text("§7只推送下列类型的物品"));
        for (ItemStack sample : filterItems) {
            lines.add(Component.text("§8- §f" + sample.getType().name()));
        }
        return ItemStackBuilder.of(Material.HOPPER)
                .name(Component.text("§a过滤中 (" + filterItems.size() + " 种)"))
                .lore(lines);
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
}
