package io.github.lyen.LogiTech.core.Register;

import io.github.lyen.LogiTech.core.basic.Items.BugItemManager;
import io.github.lyen.LogiTech.core.basic.Items.PortableEnderChest;
import io.github.lyen.LogiTech.core.basic.Items.PortableFurnace;
import io.github.lyen.LogiTech.core.basic.Items.PortableTrashCan;
import io.github.lyen.LogiTech.core.basic.Items.PortableWorkbench;
import io.github.lyen.LogiTech.core.basic.Items.SkullBuilder;
import io.github.lyen.LogiTech.core.basic.Items.StorageBag;
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
            .lore("<gray>右键打开工作台界面", "<gray>随时随地进行合成")
            .build();

    // 便携式熔炉
    public static final ItemStack PORTABLE_FURNACE = ItemStackBuilder.rebar(Material.FURNACE, RegisterKeys.PORTABLE_FURNACE)
            .name("<gray>便携式熔炉")
            .lore("<gray>右键打开熔炉界面")
            .build();

    // 便携式末影箱
    public static final ItemStack PORTABLE_ENDERCHEST = ItemStackBuilder.rebar(Material.ENDER_CHEST, RegisterKeys.PORTABLE_ENDERCHEST)
            .name("<gold>便携式末影箱")
            .lore("<gray>右键打开末影箱界面")
            .build();

    // 便携式垃圾桶
    public static final ItemStack PORTABLE_TRASHCAN = ItemStackBuilder.rebar(Material.HOPPER, RegisterKeys.PORTABLE_TRASHCAN)
            .name("<red>便携式垃圾桶")
            .lore("<gray>右键打开垃圾桶界面")
            .build();

    // BUG物品（骨粉材质，发光效果）
    public static final ItemStack BUG = ItemStackBuilder.rebar(Material.BONE_MEAL, RegisterKeys.BUG_ITEM)
            .name("<gold>BUG")
            .lore("<gray>经常出现在意想不到的地方......")
            .build();

    public static void initialize() {
        // ====================
        // 注册物品到 Rebar
        // ====================
        
        // 将物品添加到对应的物品组

        // 便携式存储背包 -> PORTABLE_TOOLS 物品组（便携式工具相关）
        RebarItem.register(StorageBag.class, STORAGE_BAG);
        Group.PORTABLE_TOOLS.addItem(STORAGE_BAG);

        // 便携式工作台 -> PORTABLE_TOOLS 物品组（便携式工具相关）
        RebarItem.register(PortableWorkbench.class, PORTABLE_WORKBENCH);
        Group.PORTABLE_TOOLS.addItem(PORTABLE_WORKBENCH);

        // 便携式熔炉 -> PORTABLE_TOOLS 物品组（便携式工具相关）
        RebarItem.register(PortableFurnace.class, PORTABLE_FURNACE);
        Group.PORTABLE_TOOLS.addItem(PORTABLE_FURNACE);

        // 便携式末影箱 -> PORTABLE_TOOLS 物品组（便携式工具相关）
        RebarItem.register(PortableEnderChest.class, PORTABLE_ENDERCHEST);
        Group.PORTABLE_TOOLS.addItem(PORTABLE_ENDERCHEST);

        // 便携式垃圾桶 -> PORTABLE_TOOLS 物品组（便携式工具相关）
        RebarItem.register(PortableTrashCan.class, PORTABLE_TRASHCAN);
        Group.PORTABLE_TOOLS.addItem(PORTABLE_TRASHCAN);

        // BUG物品 -> MATERIAL 物品组（基础材料）
        RebarItem.register(BugItemManager.class, BUG);
        Group.MATERIAL.addItem(BUG);
        System.out.println("[LogiTech] BUG物品已添加到MATERIAL物品组");

        System.out.println("[LogiTech] 物品注册完成");
        System.out.println("[LogiTech] 物品已分配到对应物品组");
    }
}