package io.github.lyen.LogiTech.core.basic.SpecialItems;

import io.github.lyen.LogiTech.core.Register.RegisterKeys;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 容量卡（类似 AE 存储元件）：数据保存在卡片物品自身的 PDC 中，卡片不会消耗。
 * 容量限制的是物品总数量、不限种类：1K 卡可存 1024 个、4K 卡 4096 个、16K 卡 16384 个，
 * 种类任意。每种物品的模板（含自定义名/lore/附魔等完整 ItemMeta）序列化保存，取出原样还原。
 */
public final class MemoryCard {

    /** 卡片总容量（物品数，不限种类） */
    public static final int CAPACITY_1K = 1024;
    public static final int CAPACITY_4K = 4096;
    public static final int CAPACITY_16K = 16384;

    private MemoryCard() {
    }

    /**
     * 该物品是否是本插件的容量卡
     * 唱片材质是现行卡（1K=CAT / 4K=BLOCKS / 16K=FAR）
     */
    public static boolean isMemoryCard(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        return switch (item.getType()) {
            case MUSIC_DISC_CAT, MUSIC_DISC_BLOCKS, MUSIC_DISC_FAR -> isCardKey(item);
            default -> false;
        };
    }

    private static boolean isCardKey(@NotNull ItemStack item) {
        // 现行唱片卡：直接认 Rebar 物品键（作弊书/指令发的是模板克隆，可能还没有编号与容量标记）
        if (io.github.pylonmc.rebar.item.RebarItem.isRebarItem(item, MemoryCardItem.class)) {
            return true;
        }
        // 旧染料卡：靠容量标记识别
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(RegisterKeys.CARD_AMOUNT_KEY, PersistentDataType.INTEGER);
    }

    /**
     * 获取卡片总容量（非容量卡返回 0）
     * 唱片材质是现行卡（1K=CAT / 4K=BLOCKS / 16K=FAR）
     */
    public static int getCapacity(@NotNull ItemStack card) {
        if (!isMemoryCard(card)) {
            return 0;
        }
        return switch (card.getType()) {
            case MUSIC_DISC_CAT -> CAPACITY_1K;
            case MUSIC_DISC_BLOCKS -> CAPACITY_4K;
            case MUSIC_DISC_FAR -> CAPACITY_16K;
            default -> 0;
        };
    }

    /**
     * 读取卡中所有已存物品（模板含完整 ItemMeta + 各自数量）
     */
    public static @NotNull List<@NotNull StoredEntry> getEntries(@NotNull ItemStack card) {
        List<StoredEntry> entries = new ArrayList<>();
        ItemMeta meta = card.getItemMeta();
        if (meta == null) {
            return entries;
        }
        String encoded = meta.getPersistentDataContainer()
                .get(RegisterKeys.CARD_ITEM_KEY, PersistentDataType.STRING);
        if (encoded == null) {
            return entries;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(encoded);
            try (var in = new ByteArrayInputStream(bytes);
                 var dataInput = new org.bukkit.util.io.BukkitObjectInputStream(in)) {
                Object templates = dataInput.readObject();
                Object amounts = dataInput.readObject();
                if (templates instanceof ItemStack[] stacks && amounts instanceof long[] counts) {
                    int n = Math.min(stacks.length, counts.length);
                    for (int i = 0; i < n; i++) {
                        ItemStack stack = stacks[i];
                        if (stack == null || stack.getType().isAir() || counts[i] <= 0) {
                            continue;
                        }
                        entries.add(new StoredEntry(stack, counts[i]));
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return entries;
    }

    /**
     * 卡内已存物品总数
     */
    public static long getStoredAmount(@NotNull ItemStack card) {
        long total = 0;
        for (StoredEntry entry : getEntries(card)) {
            total += entry.amount();
        }
        return total;
    }

    /**
     * 卡内某类物品的数量
     */
    public static long getTotal(@NotNull ItemStack card, @NotNull ItemStack template) {
        for (StoredEntry entry : getEntries(card)) {
            if (entry.template().isSimilar(template)) {
                return entry.amount();
            }
        }
        return 0;
    }

    /**
     * 向卡片存入物品（会减少 stack 的 amount）。
     * 已有同类物品则累加；否则新增一种。总数量不超过卡容量。
     *
     * @return 实际存入数量
     */
    public static int deposit(@NotNull ItemStack card, @NotNull ItemStack stack, int maxAmount) {
        int capacity = getCapacity(card);
        if (capacity <= 0 || stack.getType().isAir() || maxAmount <= 0) {
            return 0;
        }
        List<StoredEntry> entries = getEntries(card);
        long used = 0;
        for (StoredEntry entry : entries) {
            used += entry.amount();
        }
        int canStore = (int) Math.min(maxAmount, (long) capacity - used + Math.max(0, maxAmount - stack.getAmount()));
        // 实际可存 = min(maxAmount, stack.amount, 剩余容量)
        canStore = Math.min(canStore, stack.getAmount());
        canStore = (int) Math.min(canStore, (long) capacity - used);
        if (canStore <= 0) {
            return 0;
        }

        // 同类累加
        for (StoredEntry entry : entries) {
            if (entry.template().isSimilar(stack)) {
                entry.setAmount(entry.amount() + canStore);
                saveEntries(card, entries);
                stack.setAmount(stack.getAmount() - canStore);
                return canStore;
            }
        }

        // 新种类
        ItemStack template = stack.clone();
        template.setAmount(1);
        entries.add(new StoredEntry(template, canStore));
        saveEntries(card, entries);
        stack.setAmount(stack.getAmount() - canStore);
        return canStore;
    }

    /**
     * 从卡片取出物品
     *
     * @param template  要取出的物品模板（用于校验类型）
     * @param maxAmount 最多取出数量
     * @return 取出的物品（完整 meta），卡内无同类返回 null
     */
    public static @Nullable ItemStack withdraw(@NotNull ItemStack card, @NotNull ItemStack template, int maxAmount) {
        List<StoredEntry> entries = getEntries(card);
        for (int i = 0; i < entries.size(); i++) {
            StoredEntry entry = entries.get(i);
            if (!entry.template().isSimilar(template)) {
                continue;
            }
            int take = (int) Math.min(maxAmount, entry.amount());
            if (take <= 0) {
                return null;
            }
            ItemStack result = entry.template().clone();
            result.setAmount(take);

            long remaining = entry.amount() - take;
            if (remaining > 0) {
                entry.setAmount(remaining);
            } else {
                entries.remove(i);
            }
            saveEntries(card, entries);
            return result;
        }
        return null;
    }

    /**
     * 把全部条目写回卡片：模板数组（各 amount=1，meta 完整）+ 数量数组分开序列化
     */
    private static void saveEntries(@NotNull ItemStack card, @NotNull List<@NotNull StoredEntry> entries) {
        ItemMeta meta = card.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (entries.isEmpty()) {
            // 关键：getItemMeta 返回的是副本，清空后也必须 setItemMeta 写回，否则清空操作丢失，
            // 卡会永远"残留"最后一种物品，推送器因此无限取货
            pdc.remove(RegisterKeys.CARD_ITEM_KEY);
            card.setItemMeta(meta);
            return;
        }
        try {
            ItemStack[] stacks = new ItemStack[entries.size()];
            long[] amounts = new long[entries.size()];
            for (int i = 0; i < entries.size(); i++) {
                stacks[i] = entries.get(i).template().clone();
                stacks[i].setAmount(1);
                amounts[i] = entries.get(i).amount();
            }
            var bytes = new ByteArrayOutputStream();
            try (var dataOutput = new org.bukkit.util.io.BukkitObjectOutputStream(bytes)) {
                dataOutput.writeObject(stacks);
                dataOutput.writeObject(amounts);
            }
            pdc.set(RegisterKeys.CARD_ITEM_KEY, PersistentDataType.STRING,
                    Base64.getEncoder().encodeToString(bytes.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        card.setItemMeta(meta);
    }

    /**
     * 卡内一种物品：模板（完整 ItemMeta）+ 数量
     */
    public static final class StoredEntry {

        private final ItemStack template;
        private long amount;

        StoredEntry(@NotNull ItemStack template, long amount) {
            this.template = template.clone();
            this.template.setAmount(1);
            this.amount = amount;
        }

        public @NotNull ItemStack template() {
            return template;
        }

        public long amount() {
            return amount;
        }

        void setAmount(long amount) {
            this.amount = amount;
        }
    }
}
