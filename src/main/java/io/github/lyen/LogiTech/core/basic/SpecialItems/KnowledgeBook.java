package io.github.lyen.LogiTech.core.basic.SpecialItems;

import io.github.lyen.LogiTech.MyAddon;
import io.github.pylonmc.rebar.item.RebarItem;
import io.github.pylonmc.rebar.item.base.RebarInteractor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class KnowledgeBook extends RebarItem implements RebarInteractor {

    public KnowledgeBook(@NotNull ItemStack stack) {
        super(stack);
    }

    @Override
    public void onUsedToClick(@NotNull PlayerInteractEvent event, @NotNull EventPriority priority) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item != null && item.getAmount() > 0) {
            // 给玩家解锁所有研究
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "rb research add " + player.getName() + " *");
            
            // 消耗物品
            item.setAmount(item.getAmount() - 1);
            
            player.sendMessage("§a[LogiTech] §7你已解锁所有研究！");
        }
        
        event.setCancelled(true);
    }
}