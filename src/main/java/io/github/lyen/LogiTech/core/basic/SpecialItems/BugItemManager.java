package io.github.lyen.LogiTech.Core.Basic.SpecialItems;

import io.github.lyen.LogiTech.MyAddon;
import io.github.pylonmc.rebar.item.RebarItem;
import io.github.pylonmc.rebar.item.interfaces.InteractRebarItemHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.window.Window;
import xyz.xenondevs.invui.window.WindowManager;

import java.util.Random;

/**
 * BUG物品管理器
 * 在 Rebar 正版指南中左键点击"BUG"时有 5% 概率直接获得一个（彩蛋）。
 * 作弊指南中点击 BUG 仍然是必给，不走本概率。
 */
public class BugItemManager extends RebarItem implements InteractRebarItemHandler, Listener {

    // 是否已初始化
    private static boolean initialized = false;
    // 随机数生成器
    private static final Random random = new Random();
    // 在正版指南中点击 BUG 时的获得概率 5%
    private static final double GUIDE_DROP_RATE = 0.05;

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
        return io.github.lyen.LogiTech.Core.Register.RegisterItems.BUG.clone();
    }

    @Override
    public void onInteract(@NotNull PlayerInteractEvent event, @NotNull EventPriority priority) {
        // BUG物品本身没有右键交互
    }

    /**
     * 在 Rebar 正版指南界面左键点击 BUG 时，5% 概率获得一个。
     * MONITOR 且不取消事件：只做额外发奖，不影响 Rebar 原本的配方/用途跳转逻辑。
     */
    @org.bukkit.event.EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGuideClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        // 只响应左键（含 Shift+左键）
        if (event.getClick() == null || !event.getClick().isLeftClick()) {
            return;
        }
        // 必须点在界面上半部分（指南页），点自己背包里的 BUG 不触发
        if (event.getClickedInventory() == null
                || event.getClickedInventory().equals(player.getInventory())) {
            return;
        }
        // 仅在 InvUI 窗口（各类 Rebar 指南/设备界面）中处理
        Window window = WindowManager.getInstance().getOpenWindow(player);
        if (window == null) {
            return;
        }
        // 排除作弊指南：那里点 BUG 本来就是必给
        if (CheatGuideItem.CHEAT_WINDOWS.contains(window)) {
            return;
        }
        // 被点击的必须是 BUG 物品
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()
                || !RebarItem.isRebarItem(clicked, BugItemManager.class)) {
            return;
        }
        // 5% 概率获得
        if (random.nextDouble() >= GUIDE_DROP_RATE) {
            return;
        }

        ItemStack bugItem = createBugItem();
        var leftover = player.getInventory().addItem(bugItem);
        for (ItemStack rest : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), rest);
        }
        player.sendMessage("§6[LogiTech] §7你在指南里抓到了一只 §eBUG§7！");
    }
}
