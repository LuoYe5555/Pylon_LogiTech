package io.github.lyen.LogiTech.core.Register;

import org.bukkit.NamespacedKey;


public class RegisterKeys {
    public static final String MOD_ID = "logitech";
    
    // 便携式物品
    public static final NamespacedKey STORAGE_BAG = new NamespacedKey(MOD_ID, "storage_bag");
    public static final NamespacedKey PORTABLE_WORKBENCH = new NamespacedKey(MOD_ID, "portable_workbench");
    public static final NamespacedKey PORTABLE_FURNACE = new NamespacedKey(MOD_ID, "portable_furnace");
    public static final NamespacedKey PORTABLE_ENDERCHEST = new NamespacedKey(MOD_ID, "portable_enderchest");
    public static final NamespacedKey PORTABLE_TRASHCAN = new NamespacedKey(MOD_ID, "portable_trashcan");

    // 特殊物品
    public static final NamespacedKey BUG_ITEM = new NamespacedKey(MOD_ID, "bug_item");

    // 合成配方展示物品
    public static final NamespacedKey RECIPE_PORTABLE_WORKBENCH = new NamespacedKey(MOD_ID, "recipe_portable_workbench");
    public static final NamespacedKey RECIPE_PORTABLE_FURNACE = new NamespacedKey(MOD_ID, "recipe_portable_furnace");
    public static final NamespacedKey RECIPE_PORTABLE_ENDERCHEST = new NamespacedKey(MOD_ID, "recipe_portable_enderchest");
    public static final NamespacedKey RECIPE_PORTABLE_TRASHCAN = new NamespacedKey(MOD_ID, "recipe_portable_trashcan");
}
