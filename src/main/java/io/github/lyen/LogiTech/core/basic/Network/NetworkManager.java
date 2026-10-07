package io.github.lyen.LogiTech.core.basic.Network;

import io.github.lyen.LogiTech.core.Storage.MemoryBlock;
import io.github.lyen.LogiTech.core.Storage.SingleItemStorageBlock;
import io.github.lyen.LogiTech.core.Storage.StorageBlock;
import io.github.pylonmc.rebar.block.RebarBlock;
import io.github.pylonmc.rebar.logistics.LogisticGroup;
import io.github.pylonmc.rebar.logistics.LogisticGroupType;
import io.github.pylonmc.rebar.logistics.slot.LogisticSlot;
import io.github.pylonmc.rebar.logistics.slot.VanillaInventoryLogisticSlot;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.TileState;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.xenondevs.invui.inventory.VirtualInventory;
import xyz.xenondevs.invui.inventory.event.UpdateReason;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 网络搜索与物品聚合。
 * 从任意网络方块出发，通过相邻方块洪泛搜索整张网络；
 * 网络内的物品只能存放在存储方块（StorageBlock / 量子存储）中。
 */
public final class NetworkManager {

    /** 网络最大方块数，防止无限蔓延拖垮服务器 */
    public static final int MAX_NETWORK_SIZE = 4096;

    public static final BlockFace[] FACES = {
            BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

    private NetworkManager() {
    }

    /**
     * 从起点洪泛搜索它所在的整张网络
     */
    public static @NotNull Network findNetwork(@NotNull NetworkNode origin) {
        Network network = new Network();
        Set<Block> visited = new HashSet<>();
        Deque<Block> queue = new ArrayDeque<>();

        visited.add(origin.getBlock());
        queue.add(origin.getBlock());

        while (!queue.isEmpty() && visited.size() <= MAX_NETWORK_SIZE) {
            Block current = queue.poll();
            // 区块未加载时 Rebar 不允许取方块（如 RebarBlockLoadEvent 阶段），直接跳过
            if (!isChunkLoaded(current)) {
                continue;
            }
            RebarBlock block = RebarBlock.getRebarBlock(current);
            if (block == null) {
                continue;
            }
            network.add(block);

            for (BlockFace face : FACES) {
                Block next = current.getRelative(face);
                if (visited.contains(next)) {
                    continue;
                }
                if (!isChunkLoaded(next)) {
                    continue;
                }
                RebarBlock neighbor = RebarBlock.getRebarBlock(next);
                if (neighbor != null && NetworkNode.isNetworkMember(neighbor)) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }
        return network;
    }

    /**
     * 判断方块所在区块是否已加载（不触发加载），避免在 RebarBlockLoadEvent 等阶段
     * 访问未加载区块导致 IllegalArgumentException。
     */
    private static boolean isChunkLoaded(@NotNull Block block) {
        return block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4);
    }

    /**
     * 目标方块是否属于给定网络（含监视器桥接进来的量子存储）。
     * 推送/抓取器用它跳过自己网络内的方块，避免自己搬自己。
     */
    public static boolean isSameNetworkMember(@NotNull Block target, @NotNull Network network) {
        for (NetworkNode node : network.getNodes()) {
            if (node.getBlock().equals(target)) {
                return true;
            }
        }
        for (StorageBlock storage : network.getStorages()) {
            if (storage.getBlock().equals(target)) {
                return true;
            }
        }
        for (MemoryBlock memory : network.getMemories()) {
            if (memory.getBlock().equals(target)) {
                return true;
            }
        }
        for (SingleItemStorageBlock single : network.getMonitoredStorages()) {
            if (single.getBlock().equals(target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取目标方块上可插入（推入）或可抽取（抓取）的物流槽位。
     * 依次覆盖：本插件存储方块/存储器/监视器桥接的量子存储、
     * Rebar 物流机器（按组类型过滤）、原版容器（箱子/木桶/熔炉等整包包装）。
     */
    public static @NotNull List<@NotNull LogisticSlot> getExternalSlots(@NotNull Block target, boolean insertable,
                                                                        @NotNull Network network) {
        // 本插件的存储方块：把整个虚拟背包包装成槽位（可读可写）
        if (target.getState() instanceof TileState tile) {
            RebarBlock own = RebarBlock.getRebarBlock(target);
            if (own instanceof StorageBlock storage) {
                VirtualInventory inv = storage.getStorageInventory();
                List<LogisticSlot> slots = new ArrayList<>();
                for (int i = 0; i < inv.getSize(); i++) {
                    slots.add(new VirtualInventoryLogisticSlot(inv, i));
                }
                return slots;
            }
            if (own instanceof SingleItemStorageBlock single) {
                // 量子存储只有一个虚拟大槽：抓取读它、推送写它
                return List.of(new SingleItemStorageLogisticSlot(single, insertable));
            }
            if (own instanceof MemoryBlock memory) {
                // 存储器经容量卡中转（无卡时槽位为空）
                return List.of(new MemoryBlockLogisticSlot(memory));
            }
            // 本插件其它方块（网桥/监视器等）不参与
            if (own != null) {
                return List.of();
            }
        }

        // Rebar 物流机器：按物流组类型过滤
        RebarBlock rebar = RebarBlock.getRebarBlock(target);
        if (rebar instanceof io.github.pylonmc.rebar.block.base.RebarLogisticBlock logistic) {
            List<LogisticSlot> slots = new ArrayList<>();
            for (LogisticGroup group : logistic.getLogisticGroups().values()) {
                LogisticGroupType type = group.getSlotType();
                boolean matches = insertable
                        ? (type == LogisticGroupType.INPUT || type == LogisticGroupType.BOTH)
                        : (type == LogisticGroupType.OUTPUT || type == LogisticGroupType.BOTH);
                if (matches) {
                    slots.addAll(group.getSlots());
                }
            }
            return slots;
        }

        // 其余 Rebar 方块（无物流接口）不参与
        if (rebar != null) {
            return List.of();
        }

        // 原版容器
        if (target.getState() instanceof InventoryHolder holder) {
            List<LogisticSlot> slots = new ArrayList<>();
            for (int i = 0; i < holder.getInventory().getSize(); i++) {
                slots.add(new VanillaInventoryLogisticSlot(holder.getInventory(), i));
            }
            return slots;
        }
        return List.of();
    }

    /**
     * 网络内的一类物品（模板 + 总数）
     */
    public static final class StackEntry {
        public final @NotNull ItemStack template;
        public long total;

        StackEntry(@NotNull ItemStack template, long total) {
            this.template = template;
            this.total = total;
        }
    }

    /**
     * 一张网络：网络设备节点 + 存储方块集合，提供跨存储的存取操作。
     * 量子存储不是网络成员；由网络监视器桥接——扫描时把每个监视器
     * 所监视的量子存储纳入本网络的量子存储池（去重）。
     */
    public static final class Network {

        private final List<NetworkNode> nodes = new ArrayList<>();
        private final List<StorageBlock> storages = new ArrayList<>();
        private final List<MemoryBlock> memories = new ArrayList<>();
        /** 监视器桥接进来的量子存储（按方块位置去重） */
        private final List<SingleItemStorageBlock> monitoredStorages = new ArrayList<>();
        private final Set<Block> seenMonitored = new HashSet<>();

        void add(RebarBlock block) {
            if (block instanceof StorageBlock storage) {
                storages.add(storage);
            } else if (block instanceof MemoryBlock memory) {
                memories.add(memory);
            } else if (block instanceof NetworkMonitorBlock monitor) {
                nodes.add(monitor);
                SingleItemStorageBlock monitored = monitor.getMonitoredStorage();
                if (monitored != null && seenMonitored.add(monitored.getBlock())) {
                    monitoredStorages.add(monitored);
                }
            } else if (block instanceof NetworkNode node) {
                nodes.add(node);
            }
        }

        public boolean isEmpty() {
            return nodes.isEmpty() && storages.isEmpty() && memories.isEmpty() && monitoredStorages.isEmpty();
        }

        public @NotNull List<@NotNull NetworkNode> getNodes() {
            return nodes;
        }

        public @NotNull List<@NotNull StorageBlock> getStorages() {
            return storages;
        }

        public @NotNull List<@NotNull MemoryBlock> getMemories() {
            return memories;
        }

        /**
         * 监视器桥接进来的量子存储
         */
        public @NotNull List<@NotNull SingleItemStorageBlock> getMonitoredStorages() {
            return monitoredStorages;
        }

        public int size() {
            return nodes.size() + storages.size() + memories.size();
        }

        /**
         * 统计全网所有物品，按类型合并
         */
        public @NotNull List<@NotNull StackEntry> collectEntries() {
            Map<String, StackEntry> merged = new LinkedHashMap<>();
            for (StorageBlock storage : storages) {
                VirtualInventory inv = storage.getStorageInventory();
                for (ItemStack item : inv.getItems()) {
                    if (item == null || item.getType().isAir()) {
                        continue;
                    }
                    merge(merged, item, item.getAmount());
                }
            }
            for (MemoryBlock memory : memories) {
                memory.collectEntries(merged);
            }
            for (SingleItemStorageBlock single : monitoredStorages) {
                ItemStack stored = single.getStoredItem();
                if (stored != null && single.getStoredAmount() > 0) {
                    merge(merged, stored, single.getStoredAmount());
                }
            }
            return new ArrayList<>(merged.values());
        }

        private static void merge(Map<String, StackEntry> merged, ItemStack item, long amount) {
            for (StackEntry entry : merged.values()) {
                if (entry.template.isSimilar(item)) {
                    entry.total += amount;
                    return;
                }
            }
            ItemStack template = item.clone();
            template.setAmount(1);
            merged.put(keyOf(template), new StackEntry(template, amount));
        }

        /**
         * 供外部（如存储器）向合并表累加一类物品
         */
        public static void mergePublic(@NotNull Map<String, StackEntry> merged, @NotNull ItemStack item, long amount) {
            merge(merged, item, amount);
        }

        private static String keyOf(ItemStack item) {
            return item.getType().name() + "|" + (item.hasItemMeta() ? item.getItemMeta().hashCode() : "0");
        }

        /**
         * 查询某类物品在全网的总数
         */
        public long getTotal(@NotNull ItemStack template) {
            long total = 0;
            for (StorageBlock storage : storages) {
                VirtualInventory inv = storage.getStorageInventory();
                for (ItemStack item : inv.getItems()) {
                    if (item != null && !item.getType().isAir() && item.isSimilar(template)) {
                        total += item.getAmount();
                    }
                }
            }
            for (MemoryBlock memory : memories) {
                total += memory.getTotal(template);
            }
            for (SingleItemStorageBlock single : monitoredStorages) {
                ItemStack stored = single.getStoredItem();
                if (stored != null && stored.isSimilar(template)) {
                    total += single.getStoredAmount();
                }
            }
            return total;
        }

        /**
         * 向网络存入物品。
         *
         * @param stack 要存入的物品（会按存入数量减少其 amount）
         * @return 实际存入的数量
         */
        public int deposit(@NotNull ItemStack stack) {
            if (stack.getType().isAir() || stack.getAmount() <= 0) {
                return 0;
            }
            int remaining = stack.getAmount();
            int original = remaining;

            // 优先塞监视器桥接的量子存储
            for (SingleItemStorageBlock single : monitoredStorages) {
                if (remaining <= 0) {
                    break;
                }
                ItemStack tryPut = stack.clone();
                tryPut.setAmount(remaining);
                single.depositItem(tryPut);
                remaining = tryPut.getAmount();
            }

            // 再塞存储器的容量卡
            for (MemoryBlock memory : memories) {
                if (remaining <= 0) {
                    break;
                }
                remaining -= memory.deposit(stack, remaining);
            }

            // 最后塞普通存储方块
            for (StorageBlock storage : storages) {
                if (remaining <= 0) {
                    break;
                }
                ItemStack tryPut = stack.clone();
                tryPut.setAmount(remaining);
                int leftover = storage.getStorageInventory().addItem(UpdateReason.SUPPRESSED, tryPut);
                remaining = leftover;
            }

            stack.setAmount(remaining);
            return original - remaining;
        }

        /**
         * 从网络取出物品。
         *
         * @param template  要取出的物品模板（null 表示随便取一种）
         * @param maxAmount 最多取出数量
         * @return 取出的物品（amount 为实际取出数），网络中没有对应物品时返回 null
         */
        public @Nullable ItemStack withdraw(@Nullable ItemStack template, int maxAmount) {
            if (maxAmount <= 0) {
                return null;
            }
            // 没指定类型时随便找一种
            if (template == null) {
                for (StackEntry entry : collectEntries()) {
                    return withdraw(entry.template, maxAmount);
                }
                return null;
            }

            int remaining = maxAmount;
            ItemStack result = null;

            for (StorageBlock storage : storages) {
                if (remaining <= 0) {
                    break;
                }
                VirtualInventory inv = storage.getStorageInventory();
                ItemStack[] items = inv.getItems();
                for (int slot = 0; slot < items.length && remaining > 0; slot++) {
                    ItemStack item = items[slot];
                    if (item == null || item.getType().isAir() || !item.isSimilar(template)) {
                        continue;
                    }
                    int take = Math.min(remaining, item.getAmount());
                    inv.setItemAmount(UpdateReason.SUPPRESSED, slot, item.getAmount() - take);
                    remaining -= take;
                    if (result == null) {
                        result = item.clone();
                        result.setAmount(take);
                    } else {
                        result.setAmount(result.getAmount() + take);
                    }
                }
            }

            for (MemoryBlock memory : memories) {
                if (remaining <= 0) {
                    break;
                }
                ItemStack taken = memory.withdraw(template, remaining);
                if (taken != null && taken.getAmount() > 0) {
                    remaining -= taken.getAmount();
                    if (result == null) {
                        result = taken;
                    } else {
                        result.setAmount(result.getAmount() + taken.getAmount());
                    }
                }
            }

            // 从监视器桥接的量子存储取
            for (SingleItemStorageBlock single : monitoredStorages) {
                if (remaining <= 0) {
                    break;
                }
                ItemStack stored = single.getStoredItem();
                if (stored == null || !stored.isSimilar(template)) {
                    continue;
                }
                ItemStack taken = single.withdrawItem(remaining);
                if (taken != null && taken.getAmount() > 0) {
                    remaining -= taken.getAmount();
                    if (result == null) {
                        result = taken;
                    } else {
                        result.setAmount(result.getAmount() + taken.getAmount());
                    }
                }
            }

            return result;
        }
    }

    /**
     * Rebar 虚拟背包的槽位适配器：让推送/抓取器能直接读写 StorageBlock 的虚拟背包。
     * VirtualInventory 有自己的堆叠上限，与普通容器槽位一致。
     */
    private static final class VirtualInventoryLogisticSlot implements LogisticSlot {

        private final VirtualInventory inventory;
        private final int slot;

        VirtualInventoryLogisticSlot(@NotNull VirtualInventory inventory, int slot) {
            this.inventory = inventory;
            this.slot = slot;
        }

        @Override
        public ItemStack getItemStack() {
            return inventory.getItem(slot);
        }

        @Override
        public long getAmount() {
            ItemStack item = inventory.getItem(slot);
            return item == null ? 0 : item.getAmount();
        }

        @Override
        public long getMaxAmount(ItemStack stack) {
            return inventory.getMaxStackSize(slot);
        }

        @Override
        public void set(ItemStack stack, long amount) {
            if (stack == null || amount <= 0) {
                inventory.setItem(UpdateReason.SUPPRESSED, slot, null);
            } else {
                ItemStack set = stack.clone();
                set.setAmount((int) Math.min(amount, Integer.MAX_VALUE));
                inventory.setItem(UpdateReason.SUPPRESSED, slot, set);
            }
        }
    }

    /**
     * 量子存储的槽位适配器：绝对数量语义。
     * set(stack, amount)：amount > 当前存量 → 存入差额（推送）；amount < 当前存量 → 取出差额（抓取扣除）。
     */
    private static final class SingleItemStorageLogisticSlot implements LogisticSlot {

        private final SingleItemStorageBlock storage;
        private final boolean insertable;

        SingleItemStorageLogisticSlot(@NotNull SingleItemStorageBlock storage, boolean insertable) {
            this.storage = storage;
            this.insertable = insertable;
        }

        @Override
        public ItemStack getItemStack() {
            ItemStack stored = storage.getStoredItem();
            if (stored == null || storage.getStoredAmount() <= 0) {
                return null;
            }
            ItemStack display = stored.clone();
            display.setAmount((int) Math.min(storage.getStoredAmount(), display.getMaxStackSize()));
            return display;
        }

        @Override
        public long getAmount() {
            return insertable ? 0 : storage.getStoredAmount();
        }

        @Override
        public long getMaxAmount(ItemStack stack) {
            return Integer.MAX_VALUE;
        }

        @Override
        public void set(ItemStack stack, long amount) {
            long current = storage.getStoredAmount();
            long target = Math.max(0, amount);
            if (target > current) {
                if (stack == null) {
                    return;
                }
                ItemStack put = stack.clone();
                put.setAmount((int) Math.min(target - current, Integer.MAX_VALUE));
                storage.depositItem(put);
            } else if (target < current) {
                // 抓取后的扣除：必须真正取出，否则源存储不减、物品无限复制
                storage.withdrawItem((int) Math.min(current - target, Integer.MAX_VALUE));
            }
        }
    }

    /**
     * 存储器的槽位适配器：内容来自其容量卡（无卡时为空槽）。绝对数量语义同量子存储适配器。
     */
    private static final class MemoryBlockLogisticSlot implements LogisticSlot {

        private final MemoryBlock memory;

        MemoryBlockLogisticSlot(@NotNull MemoryBlock memory) {
            this.memory = memory;
        }

        @Override
        public ItemStack getItemStack() {
            return memory.peekAny();
        }

        @Override
        public long getAmount() {
            ItemStack peeked = memory.peekAny();
            return peeked == null ? 0 : memory.getTotal(peeked);
        }

        @Override
        public long getMaxAmount(ItemStack stack) {
            return Integer.MAX_VALUE;
        }

        @Override
        public void set(ItemStack stack, long amount) {
            ItemStack peeked = memory.peekAny();
            long current = peeked == null ? 0 : memory.getTotal(peeked);
            long target = Math.max(0, amount);
            if (target > current) {
                if (stack == null) {
                    return;
                }
                ItemStack put = stack.clone();
                put.setAmount((int) Math.min(target - current, Integer.MAX_VALUE));
                memory.deposit(put, put.getAmount());
            } else if (target < current && peeked != null) {
                // 抓取后的扣除：必须真正从卡中取出，否则卡不消耗、物品无限复制
                memory.withdraw(peeked, (int) Math.min(current - target, Integer.MAX_VALUE));
            }
        }
    }
}
