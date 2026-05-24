package io.github.lyen.LogiTech.core.Register;

import io.github.lyen.LogiTech.MyAddon;
import io.github.pylonmc.rebar.recipe.RecipeType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.recipe.CraftingBookCategory;

import static io.github.lyen.LogiTech.core.basic.Items.BugItemManager.createBugItem;

/**
 * Recipe registration using Rebar's RecipeType for vanilla recipe book display
 */
public class Recipe {

    /**
     * Initialize all recipes
     */
    public static void initialize() {
        // 使用Rebar的RecipeType注册配方，这样会在原版配方书中显示
        registerStorageBagRecipe();
        registerPortableWorkbenchRecipe();
        registerPortableFurnaceRecipe();
        registerPortableEnderChestRecipe();
        registerPortableTrashCanRecipe();

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

    private static void registerPortableFurnaceRecipe() {
        NamespacedKey key = new NamespacedKey(MyAddon.getInstance(), "portable_furnace");
        ShapedRecipe recipe = new ShapedRecipe(key, RegisterItems.PORTABLE_FURNACE);
        recipe.shape(" B ", " F ", " B ");
        recipe.setIngredient('B', new RecipeChoice.ExactChoice(createBugItem()));
        recipe.setIngredient('F', Material.FURNACE);
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
}
