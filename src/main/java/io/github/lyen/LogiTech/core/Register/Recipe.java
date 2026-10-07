package io.github.lyen.LogiTech.core.Register;

import io.github.lyen.LogiTech.MyAddon;
import io.github.pylonmc.pylon.PylonItems;
import io.github.pylonmc.rebar.recipe.RecipeType;

import static io.github.lyen.LogiTech.core.basic.SpecialItems.BugItemManager.createBugItem;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.recipe.CraftingBookCategory;

public class Recipe {

    public static void initialize() {
        registerStorageBagRecipe();
        registerPortableWorkbenchRecipe();
        registerPortableEnderChestRecipe();
        registerPortableTrashCanRecipe();
        registerStorageBlockRecipe();
        registerQuantumStorageRecipe();
        registerMagicCrystalRecipe();
        registerMagicBookCoverRecipe();
        registerKnowledgeBookRecipe();
        registerNetworkBlockRecipes();
        System.out.println("[LogiTech] 配方注册完成");
    }

    private static void registerStorageBagRecipe() {
        NamespacedKey key = new NamespacedKey(MyAddon.getInstance(), "storage_bag");
        ShapedRecipe recipe = new ShapedRecipe(key, RegisterItems.STORAGE_BAG);
        recipe.shape(" B ", " C ", " B ");
        recipe.setIngredient('B', new RecipeChoice.ExactChoice(createBugItem()));
        recipe.setIngredient('C', Material.CHEST);
        recipe.setCategory(CraftingBookCategory.MISC);
        RecipeType.VANILLA_SHAPED.addRecipe(recipe);
    }

    private static void registerPortableWorkbenchRecipe() {
        NamespacedKey key = new NamespacedKey(MyAddon.getInstance(), "portable_workbench");
        ShapedRecipe recipe = new ShapedRecipe(key, RegisterItems.PORTABLE_WORKBENCH);
        recipe.shape(" B ", " C ", " B ");
        recipe.setIngredient('B', new RecipeChoice.ExactChoice(createBugItem()));
        recipe.setIngredient('C', Material.CRAFTING_TABLE);
        recipe.setCategory(CraftingBookCategory.MISC);
        RecipeType.VANILLA_SHAPED.addRecipe(recipe);
    }

    private static void registerPortableEnderChestRecipe() {
        NamespacedKey key = new NamespacedKey(MyAddon.getInstance(), "portable_enderchest");
        ShapedRecipe recipe = new ShapedRecipe(key, RegisterItems.PORTABLE_ENDERCHEST);
        recipe.shape(" B ", " E ", " B ");
        recipe.setIngredient('B', new RecipeChoice.ExactChoice(createBugItem()));
        recipe.setIngredient('E', Material.ENDER_CHEST);
        recipe.setCategory(CraftingBookCategory.MISC);
        RecipeType.VANILLA_SHAPED.addRecipe(recipe);
    }

    private static void registerPortableTrashCanRecipe() {
        NamespacedKey key = new NamespacedKey(MyAddon.getInstance(), "portable_trashcan");
        ShapedRecipe recipe = new ShapedRecipe(key, RegisterItems.PORTABLE_TRASHCAN);
        recipe.shape(" B ", " I ", " B ");
        recipe.setIngredient('B', new RecipeChoice.ExactChoice(createBugItem()));
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setCategory(CraftingBookCategory.MISC);
        RecipeType.VANILLA_SHAPED.addRecipe(recipe);
    }

    private static void registerStorageBlockRecipe() {
        NamespacedKey key = new NamespacedKey(MyAddon.getInstance(), "storage_block");
        ShapedRecipe recipe = new ShapedRecipe(key, RegisterItems.STORAGE_BLOCK);
        recipe.shape("GBG", "B B", "GBG");
        recipe.setIngredient('G', Material.GLASS);
        recipe.setIngredient('B', new RecipeChoice.ExactChoice(createBugItem()));
        recipe.setCategory(CraftingBookCategory.MISC);
        RecipeType.VANILLA_SHAPED.addRecipe(recipe);
    }

    private static void registerQuantumStorageRecipe() {
        NamespacedKey key = new NamespacedKey(MyAddon.getInstance(), "quantum_storage");
        ShapedRecipe recipe = new ShapedRecipe(key, RegisterItems.QUANTUM_STORAGE);
        recipe.shape("SBS", "B B", "SBS");
        recipe.setIngredient('S', RegisterItems.STORAGE_BLOCK);
        recipe.setIngredient('B', new RecipeChoice.ExactChoice(createBugItem()));
        recipe.setCategory(CraftingBookCategory.MISC);
        RecipeType.VANILLA_SHAPED.addRecipe(recipe);
    }

    private static void registerMagicCrystalRecipe() {
        // 魔法结晶 I：一个下界疣合成一个
        NamespacedKey key1 = new NamespacedKey(MyAddon.getInstance(), "magic_crystal_i");
        ShapedRecipe recipe1 = new ShapedRecipe(key1, RegisterItems.MAGIC_CRYSTAL_I);
        recipe1.shape("N", " ", " ");
        recipe1.setIngredient('N', Material.NETHER_WART);
        recipe1.setCategory(CraftingBookCategory.MISC);
        RecipeType.VANILLA_SHAPED.addRecipe(recipe1);

        // 魔法结晶 II：2x2 魔法结晶 I 合成
        NamespacedKey key2 = new NamespacedKey(MyAddon.getInstance(), "magic_crystal_ii");
        ShapedRecipe recipe2 = new ShapedRecipe(key2, RegisterItems.MAGIC_CRYSTAL_II);
        recipe2.shape("II", "II");
        recipe2.setIngredient('I', new RecipeChoice.ExactChoice(RegisterItems.MAGIC_CRYSTAL_I));
        recipe2.setCategory(CraftingBookCategory.MISC);
        RecipeType.VANILLA_SHAPED.addRecipe(recipe2);

        // 魔法结晶 III：2x2 魔法结晶 II 合成
        NamespacedKey key3 = new NamespacedKey(MyAddon.getInstance(), "magic_crystal_iii");
        ShapedRecipe recipe3 = new ShapedRecipe(key3, RegisterItems.MAGIC_CRYSTAL_III);
        recipe3.shape("II", "II");
        recipe3.setIngredient('I', new RecipeChoice.ExactChoice(RegisterItems.MAGIC_CRYSTAL_II));
        recipe3.setCategory(CraftingBookCategory.MISC);
        RecipeType.VANILLA_SHAPED.addRecipe(recipe3);
    }

    private static void registerMagicBookCoverRecipe() {
        //   魔法书皮配方
        //   null 魔法结晶II null
        //   魔法结晶II 书 魔法结晶II
        //   null 魔法结晶II null
        NamespacedKey key = new NamespacedKey(MyAddon.getInstance(), "magic_book_cover");
        ShapedRecipe recipe = new ShapedRecipe(key, RegisterItems.MAGIC_BOOK_COVER);
        recipe.shape(" I ", "IBI", " I ");
        recipe.setIngredient('I', new RecipeChoice.ExactChoice(RegisterItems.MAGIC_CRYSTAL_II));
        recipe.setIngredient('B', Material.BOOK);
        recipe.setCategory(CraftingBookCategory.MISC);
        RecipeType.VANILLA_SHAPED.addRecipe(recipe);
    }

    
    private static void registerKnowledgeBookRecipe() {
        // 学识巨著配方
        NamespacedKey key = new NamespacedKey(MyAddon.getInstance(), "knowledge_book");
        ShapedRecipe recipe = new ShapedRecipe(key, RegisterItems.KNOWLEDGE_BOOK);
        recipe.shape(" A ", "BCD", " E ");
        recipe.setIngredient('A', Material.FEATHER);
        recipe.setIngredient('B', Material.INK_SAC);
        recipe.setIngredient('C', new RecipeChoice.ExactChoice(RegisterItems.MAGIC_BOOK_COVER));
        recipe.setIngredient('D', Material.GLASS_BOTTLE);
        recipe.setIngredient('E', Material.WRITABLE_BOOK);
        recipe.setCategory(CraftingBookCategory.MISC);
        RecipeType.VANILLA_SHAPED.addRecipe(recipe);
    }

    private static void registerNetworkBlockRecipes() {
        // 网桥：BUG + 白色染色玻璃
        registerNetworkRecipe("network_bridge", RegisterItems.NETWORK_BRIDGE, Material.WHITE_STAINED_GLASS);

        // 网络监视器：BUG + 绿色染色玻璃
        registerNetworkRecipe("network_monitor", RegisterItems.NETWORK_MONITOR, Material.GREEN_STAINED_GLASS);

        // 网络推送器：BUG + 棕色染色玻璃
        registerNetworkRecipe("network_pusher", RegisterItems.NETWORK_PUSHER, Material.BROWN_STAINED_GLASS);

        // 网络抓取器：BUG + 品红色染色玻璃
        registerNetworkRecipe("network_puller", RegisterItems.NETWORK_PULLER, Material.MAGENTA_STAINED_GLASS);

        // 网格：存储方块 + BUG + 音符盒
        NamespacedKey key = new NamespacedKey(MyAddon.getInstance(), "network_grid");
        ShapedRecipe recipe = new ShapedRecipe(key, RegisterItems.NETWORK_GRID);
        recipe.shape(" B ", "BSB", " B ");
        recipe.setIngredient('B', new RecipeChoice.ExactChoice(createBugItem()));
        recipe.setIngredient('S', new RecipeChoice.ExactChoice(RegisterItems.STORAGE_BLOCK));
        recipe.setCategory(CraftingBookCategory.MISC);
        RecipeType.VANILLA_SHAPED.addRecipe(recipe);
    }

    /**
     * 网络设备通用配方：周围一圈 BUG，中心为对应颜色玻璃板
     */
    private static void registerNetworkRecipe(String name, ItemStack result, Material pane) {
        NamespacedKey key = new NamespacedKey(MyAddon.getInstance(), name);
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape("BBB", "BPB", "BBB");
        recipe.setIngredient('B', new RecipeChoice.ExactChoice(createBugItem()));
        recipe.setIngredient('P', pane);
        recipe.setCategory(CraftingBookCategory.MISC);
        RecipeType.VANILLA_SHAPED.addRecipe(recipe);
    }
}