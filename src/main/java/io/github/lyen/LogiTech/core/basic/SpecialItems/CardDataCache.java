package io.github.lyen.LogiTech.Core.Basic.SpecialItems;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 容量卡解析缓存：卡片内容以 Base64 字符串存在卡片 PDC 中，每次读取都要
 * Base64 解码 + JDK 反序列化，网格一次重绘会反复读取同一张卡，开销很大。
 *
 * 这里按卡片编号缓存已解析的物品/流体条目，以 PDC 中的编码字符串为内容签名：
 * 内容没变（签名相同）直接复用解析结果；卡片写入后必须调用 invalidate 让缓存失效。
 * 物品的存取与缓存在主线程完成，使用普通 HashMap 语义即可，ConcurrentHashMap 仅为安全兜底。
 */
public final class CardDataCache {

    private CardDataCache() {
    }

    private static final class Cached<T> {
        final String signature;
        final T value;

        Cached(@NotNull String signature, @NotNull T value) {
            this.signature = signature;
            this.value = value;
        }
    }

    private static final ConcurrentHashMap<Long, Cached<List<MemoryCard.StoredEntry>>> ITEM_CACHE =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Cached<List<MemoryCard.FluidEntry>>> FLUID_CACHE =
            new ConcurrentHashMap<>();

    /**
     * 取卡片已解析的物品条目；无编号卡（cardId < 0）不缓存，直接解析
     */
    public static @NotNull List<MemoryCard.StoredEntry> getItemEntries(long cardId, @NotNull String encoded,
                                                                       @NotNull Function<String,
                                                                               List<MemoryCard.StoredEntry>> parser) {
        if (cardId < 0) {
            return parser.apply(encoded);
        }
        Cached<List<MemoryCard.StoredEntry>> cached = ITEM_CACHE.get(cardId);
        if (cached != null && cached.signature.equals(encoded)) {
            return cached.value;
        }
        List<MemoryCard.StoredEntry> parsed = parser.apply(encoded);
        ITEM_CACHE.put(cardId, new Cached<>(encoded, parsed));
        return parsed;
    }

    /**
     * 取卡片已解析的流体条目；无编号卡（cardId < 0）不缓存，直接解析
     */
    public static @NotNull List<MemoryCard.FluidEntry> getFluidEntries(long cardId, @NotNull String encoded,
                                                                       @NotNull Function<String,
                                                                               List<MemoryCard.FluidEntry>> parser) {
        if (cardId < 0) {
            return parser.apply(encoded);
        }
        Cached<List<MemoryCard.FluidEntry>> cached = FLUID_CACHE.get(cardId);
        if (cached != null && cached.signature.equals(encoded)) {
            return cached.value;
        }
        List<MemoryCard.FluidEntry> parsed = parser.apply(encoded);
        FLUID_CACHE.put(cardId, new Cached<>(encoded, parsed));
        return parsed;
    }

    /** 卡片物品区写入后调用，强制下次重新解析 */
    public static void invalidateItem(long cardId) {
        if (cardId >= 0) {
            ITEM_CACHE.remove(cardId);
        }
    }

    /** 卡片区流体写入后调用，强制下次重新解析 */
    public static void invalidateFluid(long cardId) {
        if (cardId >= 0) {
            FLUID_CACHE.remove(cardId);
        }
    }

    /** 卡片完全失效（如删除）时清除全部缓存 */
    public static void invalidate(long cardId) {
        if (cardId >= 0) {
            ITEM_CACHE.remove(cardId);
            FLUID_CACHE.remove(cardId);
        }
    }

    /** 仅供调试：当前缓存卡片数 */
    public static int cachedItemCardCount() {
        return ITEM_CACHE.size();
    }
}
