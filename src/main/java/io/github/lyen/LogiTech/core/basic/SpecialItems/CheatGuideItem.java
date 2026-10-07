package io.github.lyen.LogiTech.Core.Basic.SpecialItems;

import io.github.lyen.LogiTech.MyAddon;
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler;
import io.github.pylonmc.rebar.guide.pages.base.GuidePage;
import io.github.pylonmc.rebar.guide.pages.base.SimpleDynamicGuidePage;
import io.github.pylonmc.rebar.item.RebarItem;
import io.github.pylonmc.rebar.item.interfaces.InteractRebarItemHandler;
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder;
import io.github.pylonmc.rebar.registry.RebarRegistry;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.item.AbstractBoundItem;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.window.Window;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.IdentityHashMap;

/**
 * Rebar Guide(作弊版)：右键打开与 Rebar 指南相同的界面，
 * 左键点击物品获得 1 个，Shift+右键获得一组（类似粘液科技的作弊版指南）。
 */
public class CheatGuideItem extends RebarItem implements InteractRebarItemHandler {

    /** 作弊指南页面的 key（标题取 lang 里的 guide.page.cheat_guide） */
    public static final org.bukkit.NamespacedKey PAGE_KEY =
            new org.bukkit.NamespacedKey(MyAddon.getInstance(), "cheat_guide");

    /**
     * 当前打开着的作弊指南窗口集合。
     * BUG 彩蛋只在 Rebar 正版指南中触发，用它把作弊指南窗口排除掉
     * （作弊指南里点 BUG 本来就是必给，不应再走概率）。
     */
    public static final Set<Window> CHEAT_WINDOWS =
            Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));

    /** 作弊指南页面（懒加载，展示全部 Rebar 物品） */
    private static GuidePage cheatPage;

    public CheatGuideItem(@NotNull ItemStack stack) {
        super(stack);
    }

    /**
     * 获取/创建作弊版页面：把 Rebar 全部注册物品以 CheatItemButton 展示。
     * 自定义 open：不写入 Rebar 指南的历史栈，避免"下次打开正版指南却弹出作弊版"。
     */
    private static GuidePage page() {
        if (cheatPage != null) {
            return cheatPage;
        }
        cheatPage = new SimpleDynamicGuidePage(PAGE_KEY, () -> {
            List<Item> buttons = new java.util.ArrayList<>();
            for (io.github.pylonmc.rebar.item.RebarItemSchema schema : RebarRegistry.ITEMS) {
                try {
                    buttons.add(new CheatItemButton(schema.createNewItemStack()));
                } catch (Exception ignored) {
                }
            }
            return buttons;
        }) {
            @Override
            public Window open(@NotNull org.bukkit.entity.Player player) {
                try {
                    // 关键区别：不调用 GuidePage.open 的历史记录逻辑，也不清空玩家在正版指南中的浏览记录
                    Window window = Window.builder()
                            .setUpperGui(getGui(player))
                            .setTitle(getTitle())
                            .setViewer(player)
                            .build();
                    // 登记为作弊指南窗口，关闭时移除，供 BUG 彩蛋监听器区分正版/作弊指南
                    CHEAT_WINDOWS.add(window);
                    window.addCloseHandler(reason -> CHEAT_WINDOWS.remove(window));
                    window.open();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                return null;
            }
        };
        return cheatPage;
    }

    /**
     * 右键打开作弊指南（Rebar 物品交互入口）
     */
    @MultiHandler(priorities = {EventPriority.NORMAL, EventPriority.MONITOR})
    @Override
    public void onInteract(@NotNull PlayerInteractEvent event, @NotNull EventPriority priority) {
        if (!event.getAction().isRightClick() || event.useItemInHand() == Event.Result.DENY) {
            return;
        }
        if (priority == EventPriority.NORMAL) {
            event.setUseInteractedBlock(Event.Result.DENY);
        } else {
            page().open(event.getPlayer());
        }
    }

    /**
     * 作弊物品按钮：左键给 1 个，Shift+右键给一组
     */
    private static class CheatItemButton extends AbstractBoundItem {

        private final ItemStack stack;

        CheatItemButton(@NotNull ItemStack stack) {
            this.stack = stack.clone();
            this.stack.setAmount(1);
        }

        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player viewer) {
            return ItemStackBuilder.of(stack);
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull Click click) {
            int amount;
            if (clickType == ClickType.SHIFT_RIGHT) {
                amount = stack.getMaxStackSize();
            } else if (clickType == ClickType.LEFT) {
                amount = 1;
            } else {
                return;
            }

            ItemStack given = stack.clone();
            // Rebar 物品用其 schema 模板发放，保证 PDC/翻译键完整；普通物品直接给
            var rebarItem = io.github.pylonmc.rebar.item.RebarItem.fromStack(stack);
            if (rebarItem != null) {
                given = rebarItem.getSchema().createNewItemStack();
            }
            given.setAmount(amount);

            var leftover = player.getInventory().addItem(given);
            for (ItemStack rest : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), rest);
            }
        }
    }
}
