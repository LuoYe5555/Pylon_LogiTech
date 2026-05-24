package io.github.lyen.LogiTech.core;

import io.github.lyen.LogiTech.MyAddon;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * 指南书配方显示监听器
 * 当玩家在指南书中点击物品时显示其合成配方
 */
public class GuideRecipeDisplay implements Listener {

    public GuideRecipeDisplay() {
        Bukkit.getPluginManager().registerEvents(this, MyAddon.getInstance());
    }

    @EventHandler
    public void onGuideClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // 检查是否是指南书界面
        String title = event.getView().getTitle();
        if (title == null || (!title.contains("LogiTech") && !title.contains("逻辑工艺"))) {
            return;
        }

        // 获取点击的物品
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // 获取物品的配方
        List<Recipe> recipes = Bukkit.getRecipesFor(clickedItem);
        
        if (!recipes.isEmpty()) {
            // 显示第一个配方（通常只有一个配方）
            Recipe recipe = recipes.get(0);
            
            // 发送配方信息给玩家
            sendRecipeInfo(player, clickedItem, recipe);
        }
    }

    private void sendRecipeInfo(Player player, ItemStack result, Recipe recipe) {
        String itemName = result.getType().name();
        if (result.hasItemMeta()) {
            ItemMeta meta = result.getItemMeta();
            if (meta.hasDisplayName()) {
                itemName = meta.getDisplayName();
            }
        }

        player.sendMessage("§a=== 合成配方 ===");
        player.sendMessage("§6产物: " + itemName);
        
        if (recipe instanceof org.bukkit.inventory.ShapedRecipe shapedRecipe) {
            player.sendMessage("§6配方:");
            String[] shape = shapedRecipe.getShape();
            for (String row : shape) {
                StringBuilder rowStr = new StringBuilder("§7  ");
                for (char c : row.toCharArray()) {
                    org.bukkit.inventory.RecipeChoice choice = shapedRecipe.getChoiceMap().get(c);
                    if (choice != null) {
                        ItemStack example = choice.getItemStack();
                        rowStr.append(example.getType().name()).append(" ");
                    } else {
                        rowStr.append("   ");
                    }
                }
                player.sendMessage(rowStr.toString());
            }
        } else if (recipe instanceof org.bukkit.inventory.ShapelessRecipe shapelessRecipe) {
            player.sendMessage("§6材料:");
            for (org.bukkit.inventory.RecipeChoice choice : shapelessRecipe.getChoiceList()) {
                ItemStack example = choice.getItemStack();
                player.sendMessage("§7  - " + example.getType().name());
            }
        }
    }
}