package io.github.lyen.LogiTech.core.basic.SpecialItems;

import io.github.pylonmc.rebar.item.RebarItem;
import io.github.pylonmc.rebar.item.base.RebarInteractor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 便携式工作台
 * 右键打开真正的工作台界面
 */
public class PortableWorkbench extends RebarItem implements RebarInteractor {

    public PortableWorkbench(@NotNull ItemStack stack) {
        super(stack);
    }

    @Override
    public void onUsedToClick(@NotNull PlayerInteractEvent event, @NotNull EventPriority priority) {
        if (!event.getAction().isRightClick()) {
            return;
        }

        Player player = event.getPlayer();
        openWorkbench(player);
        event.setCancelled(true);
    }

    private void openWorkbench(Player player) {
        // 在玩家位置打开工作台
        Location location = player.getLocation();
        player.openWorkbench(location, true);
    }
}
