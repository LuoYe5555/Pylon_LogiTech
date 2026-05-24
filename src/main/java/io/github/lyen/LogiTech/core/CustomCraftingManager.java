package io.github.lyen.LogiTech.core;

import io.github.lyen.LogiTech.MyAddon;
import io.github.lyen.LogiTech.core.basic.Items.BugItemManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * 自定义合成系统管理器
 * 支持使用自定义物品作为合成材料
 */
public class CustomCraftingManager implements Listener {

    // 存储自定义合成配方
    private static final Map<String, CustomRecipe> recipes = new HashMap<>();

    public CustomCraftingManager() {
        // 注册监听器
        Bukkit.getPluginManager().registerEvents(this, MyAddon.getInstance());
        // 注册自定义配方
        registerRecipes();
    }

    /**
     * 注册所有自定义配方
     */
    private void registerRecipes() {
        // 存储背包配方: BUG + 箱子 + BUG (垂直)
        CustomRecipe storageBag = new CustomRecipe()
                .addIngredient(0, BugItemManager.createBugItem())  // 上
                .addIngredient(1, new ItemStack(Material.CHEST)) // 中
                .addIngredient(2, BugItemManager.createBugItem())  // 下
                .setResult(new ItemStack(io.github.lyen.LogiTech.core.Register.RegisterItems.STORAGE_BAG));
        
        recipes.put("storage_bag", storageBag);

        // 便携式熔炉配方: BUG + 熔炉 + BUG (垂直)
        CustomRecipe portableFurnace = new CustomRecipe()
                .addIngredient(0, BugItemManager.createBugItem())  // 上
                .addIngredient(1, new ItemStack(Material.FURNACE)) // 中
                .addIngredient(2, BugItemManager.createBugItem())  // 下
                .setResult(new ItemStack(io.github.lyen.LogiTech.core.Register.RegisterItems.PORTABLE_FURNACE));
        
        recipes.put("portable_furnace", portableFurnace);

        // 便携式末影箱配方: BUG + 末影箱 + BUG (垂直)
        CustomRecipe portableEnderChest = new CustomRecipe()
                .addIngredient(0, BugItemManager.createBugItem())  // 上
                .addIngredient(1, new ItemStack(Material.ENDER_CHEST)) // 中
                .addIngredient(2, BugItemManager.createBugItem())  // 下
                .setResult(new ItemStack(io.github.lyen.LogiTech.core.Register.RegisterItems.PORTABLE_ENDERCHEST));
        
        recipes.put("portable_enderchest", portableEnderChest);

        // 便携式垃圾桶配方: BUG + 铁锭 + BUG (垂直)
        CustomRecipe portableTrashCan = new CustomRecipe()
                .addIngredient(0, BugItemManager.createBugItem())  // 上
                .addIngredient(1, new ItemStack(Material.IRON_INGOT)) // 中
                .addIngredient(2, BugItemManager.createBugItem())  // 下
                .setResult(new ItemStack(io.github.lyen.LogiTech.core.Register.RegisterItems.PORTABLE_TRASHCAN));
        
        recipes.put("portable_trashcan", portableTrashCan);

        // 便携式工作台配方: BUG + 工作台 + BUG (垂直)
        CustomRecipe portableWorkbench = new CustomRecipe()
                .addIngredient(0, BugItemManager.createBugItem())  // 上
                .addIngredient(1, new ItemStack(Material.CRAFTING_TABLE)) // 中
                .addIngredient(2, BugItemManager.createBugItem())  // 下
                .setResult(new ItemStack(io.github.lyen.LogiTech.core.Register.RegisterItems.PORTABLE_WORKBENCH));
        
        recipes.put("portable_workbench", portableWorkbench);

        System.out.println("[LogiTech] 自定义合成配方注册完成，共 " + recipes.size() + " 个配方");
    }

    /**
     * 检查物品是否匹配（考虑自定义物品的lore和displayName）
     */
    private boolean isItemMatch(ItemStack source, ItemStack target) {
        if (source == null || target == null) {
            return false;
        }
        
        // 如果是相同材质
        if (source.getType() != target.getType()) {
            return false;
        }
        
        // 获取物品元数据
        var sourceMeta = source.getItemMeta();
        var targetMeta = target.getItemMeta();
        
        if (sourceMeta == null || targetMeta == null) {
            return false;
        }
        
        // 检查displayName
        if (sourceMeta.hasDisplayName() && targetMeta.hasDisplayName()) {
            return sourceMeta.getDisplayName().equals(targetMeta.getDisplayName());
        }
        
        // 检查lore
        if (sourceMeta.hasLore() && targetMeta.hasLore()) {
            return sourceMeta.getLore().equals(targetMeta.getLore());
        }
        
        return false;
    }

    /**
     * 检查合成配方
     */
    private CustomRecipe findMatchingRecipe(ItemStack[] ingredients) {
        for (CustomRecipe recipe : recipes.values()) {
            if (recipe.matches(ingredients, this::isItemMatch)) {
                return recipe;
            }
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCraftItem(CraftItemEvent event) {
        if (event.isCancelled()) {
            return;
        }
        
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        // 获取合成栏中的物品
        ItemStack[] matrix = event.getInventory().getMatrix();
        
        // 检查是否有空位
        boolean hasItems = false;
        for (ItemStack item : matrix) {
            if (item != null && item.getType() != Material.AIR) {
                hasItems = true;
                break;
            }
        }
        
        if (!hasItems) {
            return;
        }
        
        // 查找匹配的配方
        CustomRecipe matchingRecipe = findMatchingRecipe(matrix);
        
        if (matchingRecipe != null) {
            // 检查是否是原版配方已经被处理
            // 如果是我们自定义的配方，取消原版行为，使用自定义逻辑
            event.setCancelled(true);
            
            // 消耗材料
            for (int i = 0; i < matrix.length; i++) {
                if (matrix[i] != null && matrix[i].getType() != Material.AIR) {
                    // 如果是BUG物品，只消耗1个
                    if (isItemMatch(matrix[i], BugItemManager.createBugItem())) {
                        matrix[i].setAmount(matrix[i].getAmount() - 1);
                        if (matrix[i].getAmount() <= 0) {
                            matrix[i] = null;
                        }
                    } else {
                        // 其他材料消耗1个
                        matrix[i].setAmount(matrix[i].getAmount() - 1);
                        if (matrix[i].getAmount() <= 0) {
                            matrix[i] = null;
                        }
                    }
                }
            }
            
            // 给予玩家产物
            ItemStack result = matchingRecipe.getResult().clone();
            player.getInventory().addItem(result);
            
            player.sendMessage("§a[LogiTech] §7合成成功！");
            System.out.println("[LogiTech] 玩家 " + player.getName() + " 合成成功: " + result.getType());
            
            // 更新合成栏
            event.getInventory().setMatrix(matrix);
        }
    }

    /**
     * 自定义配方类
     */
    public static class CustomRecipe {
        private final ItemStack[] ingredients = new ItemStack[9]; // 3x3合成栏
        private ItemStack result;

        public CustomRecipe addIngredient(int slot, ItemStack item) {
            if (slot >= 0 && slot < 9) {
                ingredients[slot] = item.clone();
            }
            return this;
        }

        public CustomRecipe setResult(ItemStack result) {
            this.result = result.clone();
            return this;
        }

        public ItemStack getResult() {
            return result;
        }

        public ItemStack[] getIngredients() {
            return ingredients.clone();
        }

        /**
         * 检查配方是否匹配
         * @param matrix 合成栏物品
         * @param matcher 物品匹配器
         */
        public boolean matches(ItemStack[] matrix, java.util.function.BiFunction<ItemStack, ItemStack, Boolean> matcher) {
            // 检查每个槽位
            for (int i = 0; i < 9; i++) {
                ItemStack required = ingredients[i];
                ItemStack provided = (i < matrix.length) ? matrix[i] : null;
                
                if (required == null) {
                    // 如果配方中这个位置是空的，提供的内容也应该是空的
                    if (provided != null && provided.getType() != Material.AIR) {
                        return false;
                    }
                } else {
                    // 如果配方中需要物品
                    if (provided == null || provided.getType() == Material.AIR) {
                        return false;
                    }
                    // 使用提供的匹配器检查
                    if (!matcher.apply(provided, required)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }
}
