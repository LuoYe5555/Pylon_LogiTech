package io.github.lyen.LogiTech.Core.Basic.Storage;

import io.github.lyen.LogiTech.Core.Basic.Network.NetworkManager;
import io.github.lyen.LogiTech.Core.Basic.SpecialItems.MemoryCard;
import io.github.pylonmc.rebar.block.RebarBlock;
import io.github.pylonmc.rebar.block.interfaces.BlockBreakRebarBlockHandler;
import io.github.pylonmc.rebar.block.interfaces.GuiRebarBlock;
import io.github.pylonmc.rebar.block.context.BlockBreakContext;
import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.pylonmc.rebar.item.RebarItem;
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder;
import io.github.pylonmc.rebar.util.gui.GuiItems;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
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
import java.util.Map;

/**
 * 存储器：网络存储单元，本身不存物品，容量来自插入的容量卡（类似 AE 的存储箱+存储元件）。
 * 右键打开 GUI，中间 3x3 共 9 个卡槽，可混插卡；卡中的物品会接入网络供网格存取。
 * 容量卡限制的是物品总数量、不限种类；数据保存在卡片物品自身，取出卡不丢失内容。
 */
public class MemoryBlock extends RebarBlock implements GuiRebarBlock, BlockBreakRebarBlockHandler {

    public static class Item extends RebarItem {
        public Item(@NotNull ItemStack stack) {
            super(stack);
        }
    }

    /** 卡槽（3x3 共 9 张卡）。用 VirtualInventory 承接 InvUI 的点击交互 */
    private final VirtualInventory cardSlot = new VirtualInventory(9);

    /** 卡组数据键（新版：9 张卡的数组） */
    private static final org.bukkit.NamespacedKey CARDS_DATA_KEY =
            new org.bukkit.NamespacedKey("logitech", "memory_cards_data");
    /** 旧版单卡数据键，读取时兼容迁移，写入时清除 */
    private static final org.bukkit.NamespacedKey LEGACY_CARD_DATA_KEY =
            new org.bukkit.NamespacedKey("logitech", "memory_card_data");

    private Gui gui;

    public MemoryBlock(@NotNull Block block, @NotNull BlockCreateContext context) {
        super(block, context);
        initCardSlot();
    }

    public MemoryBlock(@NotNull Block block, @NotNull PersistentDataContainer pdc) {
        super(block, pdc);
        initCardSlot();
        load(pdc);
    }

    /**
     * 卡槽只允许放入容量卡（通过 PreUpdate 事件拦截），变动时刷新说明项。
     * 放入的是没编号的模板卡（如作弊书直接领取的）时自动分配编号。
     */
    private void initCardSlot() {
        cardSlot.addPreUpdateHandler(event -> {
            ItemStack newItem = event.getNewItem();
            if (newItem != null && !newItem.getType().isAir() && !MemoryCard.isMemoryCard(newItem)) {
                event.setCancelled(true);
                return;
            }
            if (newItem != null && !newItem.getType().isAir()) {
                // 无编号卡补发编号（会同时写容量标记），确保识别与存取链路完整
                io.github.lyen.LogiTech.Core.Basic.SpecialItems.MemoryCardItem.ensureId(newItem);
            }
            notifyCardChange();
        });
    }

    private void load(@NotNull PersistentDataContainer pdc) {
        // 新格式：9 张卡数组
        String encoded = pdc.get(CARDS_DATA_KEY, PersistentDataType.STRING);
        if (encoded != null) {
            try {
                byte[] bytes = java.util.Base64.getDecoder().decode(encoded);
                try (var in = new java.io.ByteArrayInputStream(bytes);
                     var dataInput = new org.bukkit.util.io.BukkitObjectInputStream(in)) {
                    if (dataInput.readObject() instanceof ItemStack[] cards) {
                        for (int i = 0; i < cards.length && i < cardSlot.getSize(); i++) {
                            ItemStack card = cards[i];
                            if (card != null && !card.getType().isAir()) {
                                // 重启直接还原的卡没经过"放入"事件，补一次编号分配：
                                // 已有编号是 no-op；无编号卡补发，保证解析缓存与名称兜底一致
                                io.github.lyen.LogiTech.Core.Basic.SpecialItems.MemoryCardItem.ensureId(card);
                                cardSlot.setItem(null, i, card);
                            }
                        }
                    }
                }
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 旧格式（单卡）迁移到 0 号槽
        encoded = pdc.get(LEGACY_CARD_DATA_KEY, PersistentDataType.STRING);
        if (encoded == null) {
            return;
        }
        try {
            byte[] bytes = java.util.Base64.getDecoder().decode(encoded);
            try (var in = new java.io.ByteArrayInputStream(bytes);
                 var dataInput = new org.bukkit.util.io.BukkitObjectInputStream(in)) {
                if (dataInput.readObject() instanceof ItemStack card && !card.getType().isAir()) {
                    io.github.lyen.LogiTech.Core.Basic.SpecialItems.MemoryCardItem.ensureId(card);
                    cardSlot.setItem(null, 0, card);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void write(@NotNull PersistentDataContainer pdc) {
        ItemStack[] cards = new ItemStack[cardSlot.getSize()];
        boolean any = false;
        for (int i = 0; i < cards.length; i++) {
            cards[i] = cardSlot.getItem(i);
            if (cards[i] != null && !cards[i].getType().isAir()) {
                any = true;
            }
        }
        try {
            if (!any) {
                // 没卡时清掉数据键（含旧版残留），避免重启后卡"复活"
                pdc.remove(CARDS_DATA_KEY);
                pdc.remove(LEGACY_CARD_DATA_KEY);
                return;
            }
            var bytes = new java.io.ByteArrayOutputStream();
            try (var dataOutput = new org.bukkit.util.io.BukkitObjectOutputStream(bytes)) {
                dataOutput.writeObject(cards);
            }
            pdc.set(CARDS_DATA_KEY, PersistentDataType.STRING,
                    java.util.Base64.getEncoder().encodeToString(bytes.toByteArray()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 指定槽位中的容量卡（无卡或不是容量卡返回 null）
     */
    private @Nullable ItemStack getCardInSlot(int slot) {
        ItemStack card = cardSlot.getItem(slot);
        return MemoryCard.isMemoryCard(card) ? card : null;
    }

    /**
     * 把修改后的卡写回卡槽（VirtualInventory.getItem 返回克隆，必须显式写回）
     */
    private void saveCard(int slot, @Nullable ItemStack card) {
        cardSlot.setItem(null, slot, card);
    }

    /**
     * 卡槽总数（3x3 = 9）
     */
    public int getCardSlotCount() {
        return cardSlot.getSize();
    }

    /**
     * 把所有卡中的物品合并进 merged
     */
    public void collectEntries(@NotNull Map<String, NetworkManager.StackEntry> merged) {
        for (int i = 0; i < cardSlot.getSize(); i++) {
            ItemStack card = getCardInSlot(i);
            if (card == null) {
                continue;
            }
            for (MemoryCard.StoredEntry entry : MemoryCard.getEntries(card)) {
                NetworkManager.Network.mergePublic(merged, entry.template(), entry.amount());
            }
        }
    }

    /**
     * 随便取一种已存物品的模板（供推送/抓取器的槽位适配器展示用），无卡或全空返回 null
     */
    public @Nullable ItemStack peekAny() {
        for (int i = 0; i < cardSlot.getSize(); i++) {
            ItemStack card = getCardInSlot(i);
            if (card == null) {
                continue;
            }
            for (MemoryCard.StoredEntry entry : MemoryCard.getEntries(card)) {
                return entry.template();
            }
        }
        return null;
    }

    /**
     * 所有卡中某类物品的总数
     */
    public long getTotal(@NotNull ItemStack template) {
        long total = 0;
        for (int i = 0; i < cardSlot.getSize(); i++) {
            ItemStack card = getCardInSlot(i);
            if (card == null) {
                continue;
            }
            total += MemoryCard.getTotal(card, template);
        }
        return total;
    }

    /**
     * 向各卡的容量余量中存入物品（会减少 stack 的 amount），优先塞已有同类的卡
     *
     * @return 实际存入数量
     */
    public int deposit(@NotNull ItemStack stack, int maxAmount) {
        int remaining = Math.min(maxAmount, stack.getAmount());
        int deposited = 0;
        for (int i = 0; i < cardSlot.getSize() && remaining > 0; i++) {
            ItemStack card = getCardInSlot(i);
            if (card == null) {
                continue;
            }
            int n = MemoryCard.deposit(card, stack, remaining);
            if (n > 0) {
                saveCard(i, card);
                deposited += n;
                remaining -= n;
            }
        }
        if (deposited > 0) {
            notifyCardChange();
        }
        return deposited;
    }

    /**
     * 从各卡中取出物品（可跨卡凑满）
     */
    public @Nullable ItemStack withdraw(@NotNull ItemStack template, int maxAmount) {
        ItemStack result = null;
        for (int i = 0; i < cardSlot.getSize(); i++) {
            if (result != null && result.getAmount() >= maxAmount) {
                break;
            }
            ItemStack card = getCardInSlot(i);
            if (card == null) {
                continue;
            }
            int need = maxAmount - (result == null ? 0 : result.getAmount());
            ItemStack taken = MemoryCard.withdraw(card, template, need);
            if (taken != null && taken.getAmount() > 0) {
                saveCard(i, card);
                if (result == null) {
                    result = taken;
                } else {
                    result.setAmount(result.getAmount() + taken.getAmount());
                }
            }
        }
        if (result != null) {
            notifyCardChange();
        }
        return result;
    }

    /**
     * 把所有卡的流体合并进 merged（流体输出器供液查询用）
     */
    public void collectFluids(@NotNull List<MemoryCard.FluidEntry> merged) {
        for (int i = 0; i < cardSlot.getSize(); i++) {
            ItemStack card = getCardInSlot(i);
            if (card == null) {
                continue;
            }
            merged.addAll(MemoryCard.getFluidEntries(card));
        }
    }

    /**
     * 所有卡中某流体的总量（mB）
     */
    public double getFluidTotal(@NotNull io.github.pylonmc.rebar.fluid.RebarFluid fluid) {
        double total = 0;
        for (int i = 0; i < cardSlot.getSize(); i++) {
            ItemStack card = getCardInSlot(i);
            if (card == null) {
                continue;
            }
            total += MemoryCard.getFluidTotal(card, fluid);
        }
        return total;
    }

    /**
     * 所有卡的流体总容量（mB）
     */
    public double getFluidCapacity() {
        double total = 0;
        for (int i = 0; i < cardSlot.getSize(); i++) {
            ItemStack card = getCardInSlot(i);
            if (card == null) {
                continue;
            }
            total += MemoryCard.getFluidCapacity(card);
        }
        return total;
    }

    /**
     * 所有卡的流体已用量（mB）
     */
    public double getFluidStoredAmount() {
        double total = 0;
        for (int i = 0; i < cardSlot.getSize(); i++) {
            ItemStack card = getCardInSlot(i);
            if (card == null) {
                continue;
            }
            total += MemoryCard.getFluidStoredAmount(card);
        }
        return total;
    }

    /**
     * 向各卡存入流体（可跨卡分配）
     *
     * @return 实际存入的 mB
     */
    public double depositFluid(@NotNull io.github.pylonmc.rebar.fluid.RebarFluid fluid, double amountMb) {
        double remaining = amountMb;
        double deposited = 0;
        for (int i = 0; i < cardSlot.getSize() && remaining > 0; i++) {
            ItemStack card = getCardInSlot(i);
            if (card == null) {
                continue;
            }
            double n = MemoryCard.depositFluid(card, fluid, remaining);
            if (n > 0) {
                saveCard(i, card);
                deposited += n;
                remaining -= n;
            }
        }
        if (deposited > 0) {
            notifyCardChange();
        }
        return deposited;
    }

    /**
     * 从各卡取出流体（可跨卡凑量）
     *
     * @return 实际取出的 mB
     */
    public double withdrawFluid(@NotNull io.github.pylonmc.rebar.fluid.RebarFluid fluid, double amountMb) {
        double remaining = amountMb;
        double withdrawn = 0;
        for (int i = 0; i < cardSlot.getSize() && remaining > 0; i++) {
            ItemStack card = getCardInSlot(i);
            if (card == null) {
                continue;
            }
            double n = MemoryCard.withdrawFluid(card, fluid, remaining);
            if (n > 0) {
                saveCard(i, card);
                withdrawn += n;
                remaining -= n;
            }
        }
        if (withdrawn > 0) {
            notifyCardChange();
        }
        return withdrawn;
    }

    private void notifyCardChange() {
        if (gui != null) {
            gui.notifyWindows();
        }
    }

    @Override
    public @NotNull Gui createGui() {
        // 中间 3x3 为卡槽区，右侧说明格
        gui = Gui.builder()
                .setStructure(
                        "# # # C C C # # #",
                        "# # # C C C # # #",
                        "# # # C C C # # #"
                )
                .addIngredient('#', GuiItems.background())
                .addIngredient('C', cardSlot)
                .build();
        return gui;
    }

    @Override
    public @NotNull Component getGuiTitle() {
        return Component.text("§8[§6存储器§8]");
    }

    /**
     * 卡槽状态说明（跨所有卡统计）
     */
    private class CardInfoItem extends AbstractItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player viewer) {
            int cardCount = 0;
            long capacity = 0;
            long used = 0;
            List<ItemStack> seenTypes = new ArrayList<>();
            for (int i = 0; i < cardSlot.getSize(); i++) {
                ItemStack card = getCardInSlot(i);
                if (card == null) {
                    continue;
                }
                cardCount++;
                capacity += MemoryCard.getCapacity(card);
                used += MemoryCard.getStoredAmount(card);
                for (MemoryCard.StoredEntry entry : MemoryCard.getEntries(card)) {
                    boolean dup = false;
                    for (ItemStack seen : seenTypes) {
                        if (seen.isSimilar(entry.template())) {
                            dup = true;
                            break;
                        }
                    }
                    if (!dup) {
                        seenTypes.add(entry.template());
                    }
                }
            }
            if (cardCount == 0) {
                return ItemStackBuilder.of(Material.STRUCTURE_VOID)
                        .name(Component.text("§e未插入容量卡"))
                        .lore(List.of(
                                Component.text("§7把容量卡放入中间 3x3 卡槽"),
                                Component.text("§7可混插 9 张卡"),
                                Component.text("§7卡中物品会接入网络")));
            }
            return ItemStackBuilder.of(Material.RESPAWN_ANCHOR)
                    .name(Component.text("§a容量卡 " + cardCount + "§7/§e9"))
                    .lore(List.of(
                            Component.text("§7总容量: §e" + capacity),
                            Component.text("§7已用: §e" + used),
                            Component.text("§7物品种类: §e" + seenTypes.size()),
                            Component.text("§8取出卡不会丢失内容")));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull Click click) {
        }
    }

    @Override
    public void onBlockBreak(@NotNull List<@NotNull ItemStack> drops, @NotNull BlockBreakContext context) {
        // 掉落卡槽中的所有容量卡（方块本体由 Rebar 默认逻辑处理）
        for (int i = 0; i < cardSlot.getSize(); i++) {
            ItemStack card = cardSlot.getItem(i);
            if (card != null && !card.getType().isAir()) {
                drops.add(card.clone());
                cardSlot.setItemAmount(null, i, 0);
            }
        }
    }
}
