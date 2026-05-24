package io.github.lyen.LogiTech.core.basic.Items;

import io.github.lyen.LogiTech.MyAddon;
import io.github.pylonmc.rebar.item.RebarItem;
import io.github.pylonmc.rebar.item.base.RebarInteractor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * BUG物品管理器
 * 处理BUG物品的挖掘掉落
 */
public class BugItemManager extends RebarItem implements RebarInteractor, Listener {

    // 是否已初始化
    private static boolean initialized = false;
    // 随机数生成器
    private static final Random random = new Random();
    // 掉落概率 25%
    private static final double DROP_RATE = 0.25;

    public BugItemManager(@NotNull ItemStack stack) {
        super(stack);
        // 只初始化一次
        if (!initialized) {
            Bukkit.getPluginManager().registerEvents(this, MyAddon.getInstance());
            initialized = true;
        }
    }

    /**
     * 创建BUG物品
     */
    public static ItemStack createBugItem() {
        // 直接克隆已注册的BUG物品，确保完全一致
        return io.github.lyen.LogiTech.core.Register.RegisterItems.BUG.clone();
    }

    @Override
    public void onUsedToClick(@NotNull PlayerInteractEvent event, @NotNull EventPriority priority) {
        // BUG物品在指南书中被点击时，这个方法不会直接被调用
    }

    /**
     * 挖掘方块时25%概率掉落BUG
     */
    @org.bukkit.event.EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        
        // 检查玩家是否为空
        if (player == null) {
            return;
        }
        
        // 检查是否使用恰当的工具（可选：只有用锹或镐才掉落）
        Material toolType = player.getInventory().getItemInMainHand().getType();
        
        // 排除空气和一些基础方块
        Material blockType = event.getBlock().getType();
        if (blockType == Material.AIR || blockType == Material.BEDROCK || 
            blockType == Material.CAVE_AIR || blockType == Material.VOID_AIR) {
            return;
        }
        
        // 25%概率掉落BUG
        if (random.nextDouble() < DROP_RATE) {
            // 生成BUG物品
            ItemStack bugItem = createBugItem();
            
            // 在方块位置掉落一个BUG
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), bugItem);
            
            player.sendMessage("§a[LogiTech] §7挖掘时发现了 §6BUG §7！");
            System.out.println("[LogiTech] 玩家 " + player.getName() + " 挖掘方块时获得了BUG物品");
        }
    }
}
