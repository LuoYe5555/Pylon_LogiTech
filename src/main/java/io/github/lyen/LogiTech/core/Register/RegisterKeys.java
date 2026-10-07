package io.github.lyen.LogiTech.core.Register;

import org.bukkit.NamespacedKey;


public class RegisterKeys {
    public static final String MOD_ID = "logitech";
    
    // 便携式物品
    public static final NamespacedKey STORAGE_BAG = new NamespacedKey(MOD_ID, "storage_bag");
    public static final NamespacedKey PORTABLE_WORKBENCH = new NamespacedKey(MOD_ID, "portable_workbench");
    public static final NamespacedKey PORTABLE_ENDERCHEST = new NamespacedKey(MOD_ID, "portable_enderchest");
    public static final NamespacedKey PORTABLE_TRASHCAN = new NamespacedKey(MOD_ID, "portable_trashcan");

    // 特殊物品
    public static final NamespacedKey BUG = new NamespacedKey(MOD_ID, "bug");

    // 存储方块
    public static final NamespacedKey STORAGE_BLOCK = new NamespacedKey(MOD_ID, "storage_block");
    public static final NamespacedKey QUANTUM_STORAGE = new NamespacedKey(MOD_ID, "quantum_storage");
    public static final NamespacedKey MEMORY_BLOCK = new NamespacedKey(MOD_ID, "memory_block");

    // 容量卡
    public static final NamespacedKey MEMORY_CARD_1K = new NamespacedKey(MOD_ID, "memory_card_1k");
    public static final NamespacedKey MEMORY_CARD_4K = new NamespacedKey(MOD_ID, "memory_card_4k");
    public static final NamespacedKey MEMORY_CARD_16K = new NamespacedKey(MOD_ID, "memory_card_16k");
    /** 容量卡 PDC：存储的物品（Base64 序列化）与数量 */
    public static final NamespacedKey CARD_ITEM_KEY = new NamespacedKey(MOD_ID, "card_item");
    public static final NamespacedKey CARD_AMOUNT_KEY = new NamespacedKey(MOD_ID, "card_amount");

    //  学识之书
    public static final NamespacedKey KNOWLEDGE_BOOK = new NamespacedKey(MOD_ID, "knowledge_book");

    // 管理专用
    public static final NamespacedKey CHEAT_GUIDE = new NamespacedKey(MOD_ID, "cheat_guide");

    // 魔法结晶
    public static final NamespacedKey MAGIC_CRYSTAL_I = new NamespacedKey(MOD_ID, "magic_crystal_i");
    public static final NamespacedKey MAGIC_CRYSTAL_II = new NamespacedKey(MOD_ID, "magic_crystal_ii");
    public static final NamespacedKey MAGIC_CRYSTAL_III = new NamespacedKey(MOD_ID, "magic_crystal_iii");

    // 魔法书皮
    public static final NamespacedKey MAGIC_BOOK_COVER = new NamespacedKey(MOD_ID, "magic_book_cover");

    // 网络方块
    public static final NamespacedKey NETWORK_MONITOR = new NamespacedKey(MOD_ID, "network_monitor");
    public static final NamespacedKey NETWORK_BRIDGE = new NamespacedKey(MOD_ID, "network_bridge");
    public static final NamespacedKey NETWORK_PUSHER = new NamespacedKey(MOD_ID, "network_pusher");
    public static final NamespacedKey NETWORK_PULLER = new NamespacedKey(MOD_ID, "network_puller");
    public static final NamespacedKey NETWORK_GRID = new NamespacedKey(MOD_ID, "network_grid");

}
