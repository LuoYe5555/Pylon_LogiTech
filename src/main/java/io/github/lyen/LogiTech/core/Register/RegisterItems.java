package io.github.lyen.LogiTech.core.Register;

import io.github.lyen.LogiTech.core.Storage.MemoryBlock;
import io.github.lyen.LogiTech.core.basic.SpecialItems.CheatGuideItem;
import io.github.lyen.LogiTech.core.basic.SpecialItems.MemoryCardItem;
import io.github.lyen.LogiTech.core.Storage.SingleItemStorageBlock;
import io.github.lyen.LogiTech.core.Storage.StorageBlock;
import io.github.lyen.LogiTech.core.basic.AbstractItems.SimpleMaterialItem;
import io.github.lyen.LogiTech.core.basic.AbstractItems.SkullBuilder;
import io.github.lyen.LogiTech.core.basic.Network.NetworkBridgeBlock;
import io.github.lyen.LogiTech.core.basic.Network.NetworkGridBlock;
import io.github.lyen.LogiTech.core.basic.Network.NetworkMonitorBlock;
import io.github.lyen.LogiTech.core.basic.Network.NetworkPullerBlock;
import io.github.lyen.LogiTech.core.basic.Network.NetworkPusherBlock;
import io.github.lyen.LogiTech.core.basic.SpecialItems.BugItemManager;
import io.github.lyen.LogiTech.core.basic.SpecialItems.KnowledgeBook;
import io.github.lyen.LogiTech.core.basic.SpecialItems.MemoryCard;
import io.github.lyen.LogiTech.core.basic.SpecialItems.PortableEnderChest;
import io.github.lyen.LogiTech.core.basic.SpecialItems.PortableTrashCan;
import io.github.lyen.LogiTech.core.basic.SpecialItems.PortableWorkbench;
import io.github.lyen.LogiTech.core.basic.SpecialItems.StorageBag;
import io.github.pylonmc.rebar.item.RebarItem;
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;


public final class RegisterItems {

    // 便携式存储背包
    public static final ItemStack STORAGE_BAG = ItemStackBuilder.rebar(Material.CHEST, RegisterKeys.STORAGE_BAG)
            .name("<gold>便携式存储背包")
            .build();

    // 便携式工作台
    public static final ItemStack PORTABLE_WORKBENCH = ItemStackBuilder.rebar(Material.CRAFTING_TABLE, RegisterKeys.PORTABLE_WORKBENCH)
            .name("<blue>便携式工作台")
            .build();

    // 便携式末影箱
    public static final ItemStack PORTABLE_ENDERCHEST = ItemStackBuilder.rebar(Material.ENDER_CHEST, RegisterKeys.PORTABLE_ENDERCHEST)
            .name("<gold>便携式末影箱")
            .build();

    // 便携式垃圾桶
    public static final ItemStack PORTABLE_TRASHCAN = ItemStackBuilder.rebar(Material.HOPPER, RegisterKeys.PORTABLE_TRASHCAN)
            .name("<red>便携式垃圾桶")
            .build();

    // BUG物品（骨粉材质，发光效果）
    public static final ItemStack BUG = ItemStackBuilder.rebar(Material.BONE_MEAL, RegisterKeys.BUG)
            .name("<gold>BUG")
            .lore("<gray>经常出现在意想不到的地方......")
            .build();

    // 存储方块
    public static final ItemStack STORAGE_BLOCK = ItemStackBuilder.rebar(Material.LIGHT_GRAY_STAINED_GLASS, RegisterKeys.STORAGE_BLOCK)
            .name("<gold>一个普通的存储方块")
            .build();

    // 量子存储方块
    public static final ItemStack QUANTUM_STORAGE = ItemStackBuilder.rebar(Material.RED_TERRACOTTA, RegisterKeys.QUANTUM_STORAGE)
            .name("<gold>量子存储(∞)")
            .build();

    // 存储器方块（容量来自插入的容量卡）
    public static final ItemStack MEMORY_BLOCK = ItemStackBuilder.rebar(Material.RESPAWN_ANCHOR, RegisterKeys.MEMORY_BLOCK)
            .build();

    // 容量卡（唱片材质、不可堆叠；每张卡有唯一编号，数据存在卡片 PDC 中，卡不消耗）
    public static final ItemStack MEMORY_CARD_1K = createMemoryCard(Material.MUSIC_DISC_CAT, RegisterKeys.MEMORY_CARD_1K);
    public static final ItemStack MEMORY_CARD_4K = createMemoryCard(Material.MUSIC_DISC_BLOCKS, RegisterKeys.MEMORY_CARD_4K);
    public static final ItemStack MEMORY_CARD_16K = createMemoryCard(Material.MUSIC_DISC_FAR, RegisterKeys.MEMORY_CARD_16K);

    // 学识巨著
    public static final ItemStack KNOWLEDGE_BOOK = ItemStackBuilder.rebar(Material.KNOWLEDGE_BOOK, RegisterKeys.KNOWLEDGE_BOOK)
            .name("<gold>学识巨著")
            .build();

    // 魔法结晶 I
    public static final ItemStack MAGIC_CRYSTAL_I = SimpleMaterialItem.createSimpleItem(
        Material.GOLD_NUGGET,
        "§6魔法结晶 - I",
         "");
    // 魔法结晶 II
    public static final ItemStack MAGIC_CRYSTAL_II = SimpleMaterialItem.createSimpleItem(
        Material.GOLD_NUGGET,
        "§6魔法结晶 - II",
        "");
    // 魔法结晶 III
    public static final ItemStack MAGIC_CRYSTAL_III = SimpleMaterialItem.createSimpleItem(
        Material.GOLD_NUGGET,
        "§6魔法结晶 - III",
        "");

    // 魔法书皮
    public static final ItemStack MAGIC_BOOK_COVER = SimpleMaterialItem.createSimpleItem(
        Material.PAPER,
        "§d魔法书皮",
        "",
        "§a§o用于各种魔法书");

    // 网络相关物品（名称/描述由 lang/zh_CN.yml 提供，不要在此硬编码 name）
    public static final ItemStack NETWORK_MONITOR = ItemStackBuilder.rebar(Material.GREEN_STAINED_GLASS, RegisterKeys.NETWORK_MONITOR)
            .build();
    public static final ItemStack NETWORK_BRIDGE = ItemStackBuilder.rebar(Material.WHITE_STAINED_GLASS, RegisterKeys.NETWORK_BRIDGE)
            .build();
    public static final ItemStack NETWORK_PUSHER = ItemStackBuilder.rebar(Material.BROWN_STAINED_GLASS, RegisterKeys.NETWORK_PUSHER)
            .build();
    public static final ItemStack NETWORK_PULLER = ItemStackBuilder.rebar(Material.MAGENTA_STAINED_GLASS, RegisterKeys.NETWORK_PULLER)
            .build();
    public static final ItemStack NETWORK_GRID = ItemStackBuilder.rebar(Material.NOTE_BLOCK, RegisterKeys.NETWORK_GRID)
            .build();

    // Rebar Guide(作弊版)：knowledge_book 材质，无配方，仅放于管理专用物品组
    public static final ItemStack CHEAT_GUIDE = ItemStackBuilder.rebar(Material.KNOWLEDGE_BOOK, RegisterKeys.CHEAT_GUIDE)
            .build();


    /** 容量卡编号发号器（持久化，跨重启递增） */
    private static final io.github.lyen.LogiTech.core.basic.SpecialItems.MemoryCardIdIssuer CARD_ID_ISSUER =
            new io.github.lyen.LogiTech.core.basic.SpecialItems.MemoryCardIdIssuer();

    /**
     * 创建容量卡模板：唱片材质、不可堆叠；名称与 lore 由 zh_CN.yml 翻译键提供（不要在此硬编码，否则 lore 会重复）
     */
    private static ItemStack createMemoryCard(Material material, org.bukkit.NamespacedKey key) {
        return ItemStackBuilder.rebar(material, key)
                .set(io.papermc.paper.datacomponent.DataComponentTypes.MAX_STACK_SIZE, 1)
                .build();
    }

    public static void initialize() {
        // ====================
        // 注册物品到 Rebar
        // ====================
        
        // 将物品添加到对应的物品组

        // 存储器方块 -> STORAGE 物品组
        RebarItem.register(MemoryBlock.Item.class, MEMORY_BLOCK, RegisterKeys.MEMORY_BLOCK);
        Group.STORAGE.addItem(MEMORY_BLOCK);

        // 容量卡 -> 注册到 Rebar（/rb give 可获取）并加入 STORAGE 物品组
        // 用 MemoryCardItem：物品实例化时分配唯一编号（不可堆叠）
        RebarItem.register(MemoryCardItem.class, MEMORY_CARD_1K, RegisterKeys.MEMORY_CARD_1K);
        RebarItem.register(MemoryCardItem.class, MEMORY_CARD_4K, RegisterKeys.MEMORY_CARD_4K);
        RebarItem.register(MemoryCardItem.class, MEMORY_CARD_16K, RegisterKeys.MEMORY_CARD_16K);
        Group.STORAGE.addItem(MEMORY_CARD_1K);
        Group.STORAGE.addItem(MEMORY_CARD_4K);
        Group.STORAGE.addItem(MEMORY_CARD_16K);

        // Rebar Guide(作弊版) -> ADMIN 物品组（管理专用，无配方）
        RebarItem.register(CheatGuideItem.class, CHEAT_GUIDE, RegisterKeys.CHEAT_GUIDE);
        Group.ADMIN.addItem(CHEAT_GUIDE);

        // 便携式存储背包 -> PORTABLE_TOOLS 物品组（便携式工具相关）
        RebarItem.register(StorageBag.class, STORAGE_BAG);
        Group.PORTABLE_TOOLS.addItem(STORAGE_BAG);

        // 便携式工作台 -> PORTABLE_TOOLS 物品组（便携式工具相关）
        RebarItem.register(PortableWorkbench.class, PORTABLE_WORKBENCH);
        Group.PORTABLE_TOOLS.addItem(PORTABLE_WORKBENCH);

        // 便携式末影箱 -> PORTABLE_TOOLS 物品组（便携式工具相关）
        RebarItem.register(PortableEnderChest.class, PORTABLE_ENDERCHEST);
        Group.PORTABLE_TOOLS.addItem(PORTABLE_ENDERCHEST);

        // 便携式垃圾桶 -> PORTABLE_TOOLS 物品组（便携式工具相关）
        RebarItem.register(PortableTrashCan.class, PORTABLE_TRASHCAN);
        Group.PORTABLE_TOOLS.addItem(PORTABLE_TRASHCAN);

        // BUG物品 -> MATERIAL 物品组（基础材料）
        RebarItem.register(BugItemManager.class, BUG);
        Group.MATERIAL.addItem(BUG);
        // 存储方块 -> STORAGE 物品组
        RebarItem.register(StorageBlock.Item.class, STORAGE_BLOCK, RegisterKeys.STORAGE_BLOCK);
        Group.STORAGE.addItem(STORAGE_BLOCK);

        // 量子存储 -> STORAGE 物品组
        RebarItem.register(SingleItemStorageBlock.Item.class, QUANTUM_STORAGE, RegisterKeys.QUANTUM_STORAGE);
        Group.STORAGE.addItem(QUANTUM_STORAGE);

        // 魔法结晶 - I -> MAGIC 物品组
        Group.MAGIC_ITEM.addItem(MAGIC_CRYSTAL_I);
        // 魔法结晶 - II -> MAGIC 物品组
        Group.MAGIC_ITEM.addItem(MAGIC_CRYSTAL_II);
        // 魔法结晶 - III -> MAGIC 物品组
        Group.MAGIC_ITEM.addItem(MAGIC_CRYSTAL_III);
        
        // 学识之书 -> MAGIC_ITEM 物品组
        RebarItem.register(KnowledgeBook.class, KNOWLEDGE_BOOK);
        Group.MAGIC_TOOLS.addItem(KNOWLEDGE_BOOK);

        // 魔法书皮 -> MAGIC_ITEM 物品组
        Group.MAGIC_ITEM.addItem(MAGIC_BOOK_COVER);

        // 网络相关物品 -> NETWORK 物品组
        RebarItem.register(NetworkMonitorBlock.Item.class, NETWORK_MONITOR, RegisterKeys.NETWORK_MONITOR);
        Group.NETWORK.addItem(NETWORK_MONITOR);
        
        RebarItem.register(NetworkBridgeBlock.Item.class, NETWORK_BRIDGE, RegisterKeys.NETWORK_BRIDGE);
        Group.NETWORK.addItem(NETWORK_BRIDGE);
        
        RebarItem.register(NetworkPusherBlock.Item.class, NETWORK_PUSHER, RegisterKeys.NETWORK_PUSHER);
        Group.NETWORK.addItem(NETWORK_PUSHER);
        
        RebarItem.register(NetworkPullerBlock.Item.class, NETWORK_PULLER, RegisterKeys.NETWORK_PULLER);
        Group.NETWORK.addItem(NETWORK_PULLER);
        
        RebarItem.register(NetworkGridBlock.Item.class, NETWORK_GRID, RegisterKeys.NETWORK_GRID);
        Group.NETWORK.addItem(NETWORK_GRID);

        System.out.println("[LogiTech] 物品注册完成");
        System.out.println("[LogiTech] 物品已分配到对应物品组");
    }
}