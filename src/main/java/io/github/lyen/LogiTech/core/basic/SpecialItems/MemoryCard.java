package io.github.lyen.LogiTech.Core.Basic.SpecialItems;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.github.lyen.LogiTech.Core.Register.RegisterKeys;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 容量卡（类似 AE 存储元件）：数据保存在卡片物品自身的 PDC 中，卡片不会消耗。
 * 容量限制的是物品总数量、不限种类
 * 种类任意。每种物品的模板（含自定义名/lore/附魔等完整 ItemMeta）序列化保存，取出原样还原。
 */
public final class MemoryCard {

    /** 卡片总容量（物品数，不限种类） */
    public static final int CAPACITY_1K = 1024;
    public static final int CAPACITY_4K = 4096;
    public static final int CAPACITY_16K = 16384;
    public static final int CAPACITY_64K = 65536;
    public static final int CAPACITY_256K = 262144;
    public static final int CAPACITY_1M = 1048576;
    public static final int CAPACITY_4M = 4194304;
    public static final int CAPACITY_16M = 16777216;

    private MemoryCard() {
    }

    /**
     * 该物品是否是本插件的容量卡
     * 唱片材质：1K=CAT / 4K=BLOCKS / 16K=FAR / 64K=CHIRP / 256K=WAIT /
     * 1M=STRAD / 4M=MELLOHI / 16M=MALL
     */
    public static boolean isMemoryCard(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        return switch (item.getType()) {
            case MUSIC_DISC_CAT, MUSIC_DISC_BLOCKS, MUSIC_DISC_FAR,
                 MUSIC_DISC_CHIRP, MUSIC_DISC_WAIT, MUSIC_DISC_STRAD,
                 MUSIC_DISC_MELLOHI, MUSIC_DISC_MALL -> isCardKey(item);
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
     */
    public static int getCapacity(@NotNull ItemStack card) {
        if (!isMemoryCard(card)) {
            return 0;
        }
        return switch (card.getType()) {
            case MUSIC_DISC_CAT -> CAPACITY_1K;
            case MUSIC_DISC_BLOCKS -> CAPACITY_4K;
            case MUSIC_DISC_FAR -> CAPACITY_16K;
            case MUSIC_DISC_CHIRP -> CAPACITY_64K;
            case MUSIC_DISC_WAIT -> CAPACITY_256K;
            case MUSIC_DISC_STRAD -> CAPACITY_1M;
            case MUSIC_DISC_MELLOHI -> CAPACITY_4M;
            case MUSIC_DISC_MALL -> CAPACITY_16M;
            default -> 0;
        };
    }

    /**
     * 读取卡中所有已存物品（模板含完整 ItemMeta + 各自数量）。
     * 走 CardDataCache：同一张卡内容未变时不重复 Base64 解码与反序列化。
     */
    public static @NotNull List<@NotNull StoredEntry> getEntries(@NotNull ItemStack card) {
        ItemMeta meta = card.getItemMeta();
        if (meta == null) {
            return new ArrayList<>();
        }
        String encoded = meta.getPersistentDataContainer()
                .get(RegisterKeys.CARD_ITEM_KEY, PersistentDataType.STRING);
        if (encoded == null) {
            return new ArrayList<>();
        }
        long cardId = MemoryCardItem.getId(card);
        List<StoredEntry> entries = CardDataCache.getItemEntries(cardId, encoded, MemoryCard::decodeItemEntries);
        // 返回副本，避免调用方的遍历/修改污染缓存（缓存本身在卡片写入时即失效）
        return new ArrayList<>(entries);
    }

    /**
     * 解析物品区编码字符串（Base64 + ItemStack[]/long[]）
     */
    private static @NotNull List<StoredEntry> decodeItemEntries(@NotNull String encoded) {
        List<StoredEntry> entries = new ArrayList<>();
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
        long cardId = MemoryCardItem.getId(card);
        if (entries.isEmpty()) {
            // 关键：getItemMeta 返回的是副本，清空后也必须 setItemMeta 写回，否则清空操作丢失，
            // 卡会永远"残留"最后一种物品，推送器因此无限取货
            pdc.remove(RegisterKeys.CARD_ITEM_KEY);
            card.setItemMeta(meta);
            CardDataCache.invalidateItem(cardId);
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
        CardDataCache.invalidateItem(cardId);
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

    // ==================== 流体存储区（同一张卡，与物品区独立） ====================

    /** 流体容量（mB）：与物品容量同比例（约 7.8125 mB/物品），最高 16M 卡 = 131072000 mB */
    public static final int FLUID_CAPACITY_1K = 8000;
    public static final int FLUID_CAPACITY_4K = 32000;
    public static final int FLUID_CAPACITY_16K = 128000;
    public static final int FLUID_CAPACITY_64K = 512000;
    public static final int FLUID_CAPACITY_256K = 2048000;
    public static final int FLUID_CAPACITY_1M = 8192000;
    public static final int FLUID_CAPACITY_4M = 32768000;
    public static final int FLUID_CAPACITY_16M = 131072000;

    /**
     * 卡的流体容量（mB，非容量卡返回 0）。物品卡与流体卡通用：每张卡物品容量与流体容量各自独立
     */
    public static int getFluidCapacity(@NotNull ItemStack card) {
        if (!isMemoryCard(card)) {
            return 0;
        }
        return switch (card.getType()) {
            case MUSIC_DISC_CAT -> FLUID_CAPACITY_1K;
            case MUSIC_DISC_BLOCKS -> FLUID_CAPACITY_4K;
            case MUSIC_DISC_FAR -> FLUID_CAPACITY_16K;
            case MUSIC_DISC_CHIRP -> FLUID_CAPACITY_64K;
            case MUSIC_DISC_WAIT -> FLUID_CAPACITY_256K;
            case MUSIC_DISC_STRAD -> FLUID_CAPACITY_1M;
            case MUSIC_DISC_MELLOHI -> FLUID_CAPACITY_4M;
            case MUSIC_DISC_MALL -> FLUID_CAPACITY_16M;
            default -> 0;
        };
    }

    /**
     * 卡内一种流体：流体类型 + 数量（mB）
     */
    public static final class FluidEntry {

        private final io.github.pylonmc.rebar.fluid.RebarFluid fluid;
        private double amountMb;

        FluidEntry(@NotNull io.github.pylonmc.rebar.fluid.RebarFluid fluid, double amountMb) {
            this.fluid = fluid;
            this.amountMb = amountMb;
        }

        public @NotNull io.github.pylonmc.rebar.fluid.RebarFluid fluid() {
            return fluid;
        }

        public double amountMb() {
            return amountMb;
        }

        void setAmountMb(double amountMb) {
            this.amountMb = amountMb;
        }
    }

    /**
     * 卡内流体条目（流体 key + mB 数组序列化在 CARD_FLUID_KEY）。
     * 走 CardDataCache：内容未变时不重复解码反序列化。
     */
    public static @NotNull List<@NotNull FluidEntry> getFluidEntries(@NotNull ItemStack card) {
        ItemMeta meta = card.getItemMeta();
        if (meta == null) {
            return new ArrayList<>();
        }
        String encoded = meta.getPersistentDataContainer()
                .get(RegisterKeys.CARD_FLUID_KEY, PersistentDataType.STRING);
        if (encoded == null) {
            return new ArrayList<>();
        }
        long cardId = MemoryCardItem.getId(card);
        return new ArrayList<>(CardDataCache.getFluidEntries(cardId, encoded, MemoryCard::decodeFluidEntries));
    }

    /**
     * 解析流体区编码字符串（Base64 + String[]/double[]）
     */
    private static @NotNull List<FluidEntry> decodeFluidEntries(@NotNull String encoded) {
        List<FluidEntry> entries = new ArrayList<>();
        try {
            byte[] bytes = Base64.getDecoder().decode(encoded);
            try (var in = new ByteArrayInputStream(bytes);
                 var dataInput = new org.bukkit.util.io.BukkitObjectInputStream(in)) {
                Object keys = dataInput.readObject();
                Object amounts = dataInput.readObject();
                if (keys instanceof String[] keyArr && amounts instanceof double[] amountArr) {
                    int n = Math.min(keyArr.length, amountArr.length);
                    for (int i = 0; i < n; i++) {
                        var fluid = io.github.pylonmc.rebar.registry.RebarRegistry.FLUIDS
                                .get(org.bukkit.NamespacedKey.fromString(keyArr[i]));
                        if (fluid != null && amountArr[i] > 0) {
                            entries.add(new FluidEntry(fluid, amountArr[i]));
                        }
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return entries;
    }

    /**
     * 卡内流体总量（mB）
     */
    public static double getFluidStoredAmount(@NotNull ItemStack card) {
        double total = 0;
        for (FluidEntry entry : getFluidEntries(card)) {
            total += entry.amountMb();
        }
        return total;
    }

    /**
     * 卡内某流体的数量（mB）
     */
    public static double getFluidTotal(@NotNull ItemStack card, @NotNull io.github.pylonmc.rebar.fluid.RebarFluid fluid) {
        for (FluidEntry entry : getFluidEntries(card)) {
            if (entry.fluid().equals(fluid)) {
                return entry.amountMb();
            }
        }
        return 0;
    }

    /**
     * 向卡中存入流体（不限种类，总量不超过卡的流体容量）
     *
     * @return 实际存入的 mB
     */
    public static double depositFluid(@NotNull ItemStack card, @NotNull io.github.pylonmc.rebar.fluid.RebarFluid fluid,
                                     double amountMb) {
        double capacity = getFluidCapacity(card);
        if (capacity <= 0 || amountMb <= 0) {
            return 0;
        }
        List<FluidEntry> entries = getFluidEntries(card);
        double used = 0;
        for (FluidEntry entry : entries) {
            used += entry.amountMb();
        }
        double canStore = Math.min(amountMb, capacity - used);
        if (canStore <= 0) {
            return 0;
        }
        // 同类累加
        for (FluidEntry entry : entries) {
            if (entry.fluid().equals(fluid)) {
                entry.setAmountMb(entry.amountMb() + canStore);
                saveFluidEntries(card, entries);
                return canStore;
            }
        }
        // 新流体
        entries.add(new FluidEntry(fluid, canStore));
        saveFluidEntries(card, entries);
        return canStore;
    }

    /**
     * 从卡中取出流体
     *
     * @return 实际取出的 mB（卡内无该流体或数量不足时取出剩余全部）
     */
    public static double withdrawFluid(@NotNull ItemStack card, @NotNull io.github.pylonmc.rebar.fluid.RebarFluid fluid,
                                       double amountMb) {
        List<FluidEntry> entries = getFluidEntries(card);
        for (int i = 0; i < entries.size(); i++) {
            FluidEntry entry = entries.get(i);
            if (!entry.fluid().equals(fluid)) {
                continue;
            }
            double take = Math.min(amountMb, entry.amountMb());
            if (take <= 0) {
                return 0;
            }
            double remaining = entry.amountMb() - take;
            if (remaining > 0) {
                entry.setAmountMb(remaining);
            } else {
                entries.remove(i);
            }
            saveFluidEntries(card, entries);
            return take;
        }
        return 0;
    }

    /**
     * 写回流体条目（key 数组 + mB 数组分开序列化；空条目时移除键并写回）
     */
    private static void saveFluidEntries(@NotNull ItemStack card, @NotNull List<@NotNull FluidEntry> entries) {
        ItemMeta meta = card.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        long cardId = MemoryCardItem.getId(card);
        if (entries.isEmpty()) {
            // getItemMeta 返回副本，清空后也必须写回，否则清空丢失（与物品区同类坑）
            pdc.remove(RegisterKeys.CARD_FLUID_KEY);
            card.setItemMeta(meta);
            CardDataCache.invalidateFluid(cardId);
            return;
        }
        try {
            String[] keys = new String[entries.size()];
            double[] amounts = new double[entries.size()];
            for (int i = 0; i < entries.size(); i++) {
                keys[i] = entries.get(i).fluid().getKey().toString();
                amounts[i] = entries.get(i).amountMb();
            }
            var bytes = new ByteArrayOutputStream();
            try (var dataOutput = new org.bukkit.util.io.BukkitObjectOutputStream(bytes)) {
                dataOutput.writeObject(keys);
                dataOutput.writeObject(amounts);
            }
            pdc.set(RegisterKeys.CARD_FLUID_KEY, PersistentDataType.STRING,
                    Base64.getEncoder().encodeToString(bytes.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        card.setItemMeta(meta);
        CardDataCache.invalidateFluid(cardId);
    }
}
