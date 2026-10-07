package io.github.lyen.LogiTech.Core.Basic.Network;

import io.github.pylonmc.rebar.block.interfaces.GuiRebarBlock;
import io.github.pylonmc.rebar.block.interfaces.TickingRebarBlock;
import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.pylonmc.rebar.item.RebarItem;
import io.github.pylonmc.rebar.logistics.slot.LogisticSlot;
import io.github.pylonmc.rebar.util.gui.GuiItems;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;

import java.util.List;

/**
 * 网络抓取器：周期性地沿设定方向从相邻的机器/容器抓取物品存入网络。
 * 右键打开 UI 可选择工作方向。
 */
public class NetworkPullerBlock extends NetworkNode implements TickingRebarBlock, GuiRebarBlock {

    public static class Item extends RebarItem {
        public Item(@NotNull ItemStack stack) {
            super(stack);
        }
    }

    private static final org.bukkit.NamespacedKey FACE_KEY =
            new org.bukkit.NamespacedKey("logitech", "puller_face");

    /** 每次传送的最大数量 */
    public static final int TRANSFER_RATE = 64;

    /** 工作间隔（tick），20 tick = 1 秒 */
    private static final int TICK_INTERVAL = 20;

    private BlockFace face = BlockFace.UP;

    private Gui gui;

    public NetworkPullerBlock(@NotNull Block block, @NotNull BlockCreateContext context) {
        super(block, context);
    }

    public NetworkPullerBlock(@NotNull Block block, @NotNull PersistentDataContainer pdc) {
        super(block, pdc);
        face = DirectionItem.of(pdc.get(FACE_KEY, PersistentDataType.STRING));
    }

    @Override
    public void write(@NotNull PersistentDataContainer pdc) {
        pdc.set(FACE_KEY, PersistentDataType.STRING, face.name());
    }

    @Override
    public void tick() {
        Block target = getBlock().getRelative(face);
        NetworkManager.Network network = getNetwork();
        if (network.isEmpty() || NetworkManager.isSameNetworkMember(target, network)) {
            return; // 空网络或目标是自己网络内的方块（自己搬自己）
        }
        List<LogisticSlot> slots = NetworkManager.getExternalSlots(target, false, network);
        if (slots.isEmpty()) {
            return;
        }
        pullOnce(network, slots);
    }

    /**
     * 尝试从目标槽位抓取一份物品存入网络
     */
    private void pullOnce(@NotNull NetworkManager.Network network, @NotNull List<@NotNull LogisticSlot> slots) {
        for (LogisticSlot slot : slots) {
            ItemStack current = slot.getItemStack();
            if (current == null || current.getType().isAir() || current.getAmount() <= 0) {
                continue;
            }

            long available = slot.getAmount();
            int take = (int) Math.min(TRANSFER_RATE, available);
            if (take <= 0) {
                continue;
            }

            ItemStack toDeposit = current.clone();
            toDeposit.setAmount(take);
            int deposited = network.deposit(toDeposit);
            if (deposited <= 0) {
                continue;
            }

            // 从槽位扣除已存入网络的数量
            long newAmount = available - deposited;
            if (newAmount <= 0) {
                slot.set(null, 0);
            } else {
                slot.set(current, newAmount);
            }
            return; // 每 tick 只传送一份
        }
    }

    @Override
    public @NotNull Gui createGui() {
        var builder = Gui.builder()
                .setStructure(FaceSelectLayout.STRUCTURE)
                .addIngredient('#', GuiItems.background());
        FaceSelectLayout.addFaceItems(builder, getBlock(), this::getFace, this::setFace);
        gui = builder.build();
        return gui;
    }

    @Override
    public @NotNull Component getGuiTitle() {
        return Component.text("§8[§6网络抓取器§8]");
    }

    public BlockFace getFace() {
        return face;
    }

    public void setFace(@NotNull BlockFace face) {
        this.face = face;
    }
}
