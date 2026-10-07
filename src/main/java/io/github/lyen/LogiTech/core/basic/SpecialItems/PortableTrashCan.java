package io.github.lyen.LogiTech.Core.Basic.SpecialItems;

import io.github.pylonmc.rebar.item.RebarItem;
import io.github.pylonmc.rebar.item.interfaces.InteractRebarItemHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 便携式垃圾桶
 * 右键打开3*9空间，放入物品后关闭，重新打开就清空（不存储数据）
 */
public class PortableTrashCan extends RebarItem implements InteractRebarItemHandler, Listener {

    private static final String GUI_TITLE = "§7便携式垃圾桶";
    private static final int GUI_SIZE = 27; // 3行9列

    public PortableTrashCan(@NotNull ItemStack stack) {
        super(stack);
    }

    @Override
    public void onInteract(@NotNull PlayerInteractEvent event, @NotNull EventPriority priority) {
        if (!event.getAction().isRightClick()) {
            return;
        }

        Player player = event.getPlayer();
        openTrashCanGUI(player);
        event.setCancelled(true);
    }

    private void openTrashCanGUI(Player player) {
        // 创建新的空界面，每次打开都是空的
        Inventory inventory = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);
        // 不填充任何物品，直接打开空界面
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) {
            return;
        }
        
        // 关闭界面时清空所有物品（销毁它们）
        Inventory inventory = event.getInventory();
        for (int i = 0; i < GUI_SIZE; i++) {
            inventory.setItem(i, null);
        }
    }
}
