package io.github.lyen.LogiTech.Core.Basic.Network;

import io.github.lyen.LogiTech.MyAddon;
import io.github.pylonmc.rebar.block.interfaces.GuiRebarBlock;
import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.pylonmc.rebar.fluid.RebarFluid;
import io.github.pylonmc.rebar.item.RebarItem;
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder;
import io.github.pylonmc.rebar.util.gui.GuiItems;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.Markers;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.inventory.VirtualInventory;
import xyz.xenondevs.invui.inventory.event.UpdateReason;
import xyz.xenondevs.invui.item.AbstractItem;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * 网格：查看并操作网络中的所有物品（类似 Network 附属的网格）。
 * - 点击物品取出（左键一组、右键一个、Shift 左键大量）
 * - 拿着物品点击"存入"按钮可存入光标物品，Shift 点击存入整个物品栏
 * - 搜索按钮通过聊天输入关键词过滤
 */
public class NetworkGridBlock extends NetworkNode implements GuiRebarBlock {

    public static class Item extends RebarItem {
        public Item(@NotNull ItemStack stack) {
            super(stack);
        }
    }

    /** 自动刷新间隔（tick）：10 tick = 0.5 秒 */
    public static final long AUTO_REFRESH_PERIOD_TICKS = 10L;

    /**
     * 所有已创建的网格方块（弱引用，方块卸载/回收后自动移出），
     * 供全局定时任务统一做自动刷新，避免每个方块各开一个定时任务。
     */
    private static final Set<NetworkGridBlock> ACTIVE_GRIDS =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    /** 全局定时任务调用：对正被玩家查看的网格做一次自动刷新 */
    public static void autoRefreshOpenGrids() {
        synchronized (ACTIVE_GRIDS) {
            for (NetworkGridBlock grid : ACTIVE_GRIDS) {
                if (grid.gui != null && !grid.gui.getCurrentViewers().isEmpty()) {
                    grid.refresh();
                }
            }
        }
    }

    /** 每位玩家的搜索关键词（临时数据，重启后失效） */
    private final Map<UUID, String> searchFilters = new HashMap<>();

    /** 最后一位搜索的玩家，网格内容按其关键词过滤 */
    private UUID lastSearcher;

    private PagedGui<xyz.xenondevs.invui.item.Item> gui;

    /** 上一次内容签名：仅在物品种类/流体种类变化时才重建内容，否则只重渲染数量 */
    private String lastContentSignature = "";

    /** 输入槽：放入的物品自动存入网络，成功后从槽位消失（网络满时剩余留槽可取回） */
    private final VirtualInventory inputSlot = new VirtualInventory(1);

    public NetworkGridBlock(@NotNull Block block, @NotNull BlockCreateContext context) {
        super(block, context);
        initInputSlot();
        ACTIVE_GRIDS.add(this);
    }

    public NetworkGridBlock(@NotNull Block block, @NotNull PersistentDataContainer pdc) {
        super(block, pdc);
        initInputSlot();
        ACTIVE_GRIDS.add(this);
    }

    /**
     * 放入输入槽的物品立即存入网络并清空槽位。
     * 注意：PostUpdate 触发时玩家点击事务还没结束，立即 setItem 清空会被 InvUI 的
     * 点击收尾写入覆盖掉，所以延迟 1 tick 等事务结束后再清空并刷新网格。
     */
    private void initInputSlot() {
        inputSlot.addPostUpdateHandler(event -> {
            ItemStack item = event.getNewItem();
            if (item == null || item.getType().isAir()) {
                return;
            }
            NetworkManager.Network network = getNetwork();
            if (network.isEmpty()) {
                return; // 未接入网络时物品留在槽内，玩家可自行取回
            }
            ItemStack toDeposit = item.clone();
            network.deposit(toDeposit);
            int leftover = toDeposit.getAmount();
            // 延迟 1 tick：等 InvUI 点击事务结束再改槽位，否则清空会被覆盖
            org.bukkit.Bukkit.getScheduler().runTaskLater(io.github.lyen.LogiTech.MyAddon.getInstance(), () -> {
                if (leftover <= 0) {
                    inputSlot.setItem(UpdateReason.SUPPRESSED, 0, null);
                } else if (leftover < item.getAmount()) {
                    ItemStack rest = item.clone();
                    rest.setAmount(leftover);
                    inputSlot.setItem(UpdateReason.SUPPRESSED, 0, rest);
                }
                // 存入成功后立即刷新网格内容显示
                refresh();
            }, 1L);
        });
    }

    @Override
    public @NotNull Gui createGui() {
        gui = PagedGui.itemsBuilder()
                .setStructure(
                        "# # # # # # # F R",
                        "i i i i i i i i i",
                        "i i i i i i i i i",
                        "i i i i i i i i i",
                        "i i i i i i i i i",
                        "I # # # # # # P N"
                )
                .addIngredient('#', GuiItems.background())
                .addIngredient('i', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('I', inputSlot)
                .addIngredient('F', new SearchItem())
                .addIngredient('R', new RefreshItem())
                .addIngredient('P', GuiItems.pagePrevious())
                .addIngredient('N', GuiItems.pageNext())
                .setContent(buildContent())
                .build();
        lastContentSignature = buildContentSignature();
        return gui;
    }

    @Override
    public @NotNull Component getGuiTitle() {
        return Component.text("§8[§3网格§8]");
    }

    /**
     * 根据当前网络物品和搜索关键词构建网格内容
     */
    private @NotNull List<xyz.xenondevs.invui.item.Item> buildContent() {
        List<xyz.xenondevs.invui.item.Item> items = new ArrayList<>();
        NetworkManager.Network network = getNetwork();
        if (network.isEmpty()) {
            return items;
        }
        String filter = currentFilter();
        for (NetworkManager.StackEntry entry : network.collectEntries()) {
            if (matches(entry.template, filter)) {
                items.add((xyz.xenondevs.invui.item.Item) new GridItem(entry.template));
            }
        }
        // 流体条目只读展示：流体只能通过流体输入器/输出器与管道交互，不能直接拿取
        for (Map.Entry<RebarFluid, Double> fluidEntry : network.collectFluids().entrySet()) {
            if (fluidEntry.getValue() > 0 && matchesFluid(fluidEntry.getKey(), filter)) {
                items.add((xyz.xenondevs.invui.item.Item) new FluidGridItem(fluidEntry.getKey()));
            }
        }
        return items;
    }

    private @Nullable String currentFilter() {
        return lastSearcher != null ? searchFilters.get(lastSearcher) : null;
    }

    /**
     * 重新扫描网络并刷新网格显示。
     * 内容种类（物品类型/流体种类/筛选词）变化时才重建分页内容并恢复原页码，
     * 只有数量变化时仅通知窗口重渲染，避免自动刷新打断玩家翻页。
     */
    private void refresh() {
        if (gui == null) {
            return;
        }
        String signature = buildContentSignature();
        if (signature.equals(lastContentSignature)) {
            gui.notifyWindows();
            return;
        }
        int page = gui.getPage();
        gui.setContent(buildContent());
        // 内容变短可能使原页码越界，夹到合法范围
        gui.setPage(Math.min(page, Math.max(0, gui.getPageCount() - 1)));
        lastContentSignature = signature;
    }

    /**
     * 当前展示内容的签名：筛选词 + 通过筛选的物品类型和流体类型（不含数量）。
     * 数量变化不改变签名，新增/消失一种物品或流体才改变。
     */
    private @NotNull String buildContentSignature() {
        NetworkManager.Network network = getNetwork();
        if (network.isEmpty()) {
            return "empty|" + currentFilter();
        }
        String filter = currentFilter();
        StringBuilder sb = new StringBuilder("f:").append(filter == null ? "" : filter);
        for (NetworkManager.StackEntry entry : network.collectEntries()) {
            if (matches(entry.template, filter)) {
                sb.append('|').append(typeKey(entry.template));
            }
        }
        for (Map.Entry<RebarFluid, Double> fluidEntry : network.collectFluids().entrySet()) {
            if (fluidEntry.getValue() > 0 && matchesFluid(fluidEntry.getKey(), filter)) {
                sb.append("|F:").append(fluidEntry.getKey().getKey());
            }
        }
        return sb.toString();
    }

    private static @NotNull String typeKey(@NotNull ItemStack item) {
        return item.getType().name() + "|" + (item.hasItemMeta() ? item.getItemMeta().hashCode() : "0");
    }

    /**
     * 判断物品是否匹配搜索关键词（匹配显示文本或物品ID，忽略大小写）
     */
    private static boolean matches(@NotNull ItemStack template, @Nullable String filter) {
        if (filter == null || filter.isEmpty()) {
            return true;
        }
        String lower = filter.toLowerCase();
        Component displayName = template.hasItemMeta() ? template.getItemMeta().displayName() : null;
        if (displayName != null && plainText(displayName).toLowerCase().contains(lower)) {
            return true;
        }
        return template.getType().getKey().getKey().contains(lower);
    }

    private static String plainText(@NotNull Component component) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(component);
    }

    /**
     * 判断流体是否匹配搜索关键词（匹配流体 ID，忽略大小写）
     */
    private static boolean matchesFluid(@NotNull RebarFluid fluid, @Nullable String filter) {
        if (filter == null || filter.isEmpty()) {
            return true;
        }
        return fluid.getKey().getKey().toLowerCase().contains(filter.toLowerCase())
                || fluid.getKey().getNamespace().toLowerCase().contains(filter.toLowerCase());
    }

    /**
     * 网格中的一件物品：显示全网该类物品总量，点击取出
     */
    private class GridItem extends AbstractItem {
        private final ItemStack template;

        GridItem(@NotNull ItemStack template) {
            this.template = template.clone();
            this.template.setAmount(1);
        }

        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player viewer) {
            NetworkManager.Network network = getNetwork();
            long total = network.getTotal(template);

            ItemStack display = template.clone();
            display.setAmount((int) Math.max(1, Math.min(total, template.getMaxStackSize())));

            return ItemStackBuilder.of(display)
                    .lore(List.of(
                            Component.text("§7总量: §e" + total),
                            Component.text("§8左键取一个 | 右键取一组"),
                            Component.text("§8Shift+左键取出更多")));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull Click click) {
            NetworkManager.Network network = getNetwork();
            long total = network.getTotal(template);
            if (total <= 0) {
                refresh();
                return;
            }

            int take;
            if (clickType.isShiftClick() && clickType.isLeftClick()) {
                take = (int) Math.min(total, template.getMaxStackSize() * 9L);
            } else if (clickType.isRightClick()) {
                take = (int) Math.min(total, template.getMaxStackSize());
            } else {
                take = 1;
            }

            ItemStack stack = network.withdraw(template, take);
            if (stack == null || stack.getAmount() <= 0) {
                refresh();
                return;
            }

            if (clickType.isShiftClick()) {
                var leftover = player.getInventory().addItem(stack);
                for (ItemStack rest : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), rest);
                }
            } else {
                ItemStack cursor = player.getItemOnCursor();
                if (cursor == null || cursor.getType().isAir()) {
                    player.setItemOnCursor(stack);
                } else if (cursor.isSimilar(stack)
                        && cursor.getAmount() + stack.getAmount() <= cursor.getMaxStackSize()) {
                    cursor.setAmount(cursor.getAmount() + stack.getAmount());
                } else {
                    player.getInventory().addItem(stack);
                }
            }

            // 物品被取空时重建内容，否则只刷新数量显示
            if (network.getTotal(template) <= 0) {
                refresh();
            } else {
                notifyWindows();
            }
        }
    }

    /**
     * 网格中的一类流体：只读显示全网该流体总量（mB / 桶）。
     * 流体没有可直接拿取的物品形态，只能通过流体输入器/输出器与 Rebar 流体管道交互。
     */
    private class FluidGridItem extends AbstractItem {
        private final RebarFluid fluid;

        FluidGridItem(@NotNull RebarFluid fluid) {
            this.fluid = fluid;
        }

        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player viewer) {
            double total = getNetwork().getFluidTotal(fluid);
            String mb = (total == Math.floor(total)) ? Long.toString((long) total) : Double.toString(total);
            return ItemStackBuilder.of(fluid.getItem())
                    .name(Component.text("§b流体: §f" + fluid.getKey().getKey()))
                    .lore(List.of(
                            Component.text("§7总量: §e" + mb + " mB §7(§f" + String.format("%.2f", total / 1000.0) + " §7桶)"),
                            Component.text("§8流体只能通过流体输入器/输出器存取")));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull Click click) {
            // 只读条目
        }
    }

    /**
     * 搜索按钮：通过聊天输入关键词
     */
    private class SearchItem extends AbstractItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player viewer) {
            String filter = searchFilters.get(viewer.getUniqueId());
            return ItemStackBuilder.of(Material.SPYGLASS)
                    .name(Component.text("§e搜索"))
                    .lore(List.of(
                            Component.text("§7点击后在聊天框输入关键词"),
                            Component.text("§7当前筛选: " + (filter == null ? "§f无" : "§b" + filter))));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull Click click) {
            player.closeInventory();
            new ConversationFactory(MyAddon.getInstance())
                    .withModality(true)
                    .withLocalEcho(false)
                    .withTimeout(30)
                    .withFirstPrompt(new StringPrompt() {
                        @Override
                        public @NotNull String getPromptText(@NotNull ConversationContext context) {
                            return "§e[LogiTech] 请在聊天框输入搜索关键词，输入 §f0 §e清除筛选:";
                        }

                        @Override
                        public @NotNull Prompt acceptInput(@NotNull ConversationContext context, @NotNull String input) {
                            String filter = input.trim();
                            lastSearcher = player.getUniqueId();
                            if (filter.isEmpty() || filter.equals("0")) {
                                searchFilters.remove(player.getUniqueId());
                                lastSearcher = null;
                                player.sendMessage("§a[LogiTech] 已清除筛选");
                            } else {
                                searchFilters.put(player.getUniqueId(), filter);
                                player.sendMessage("§a[LogiTech] 筛选: " + filter);
                            }
                            refresh();
                            reopen(player);
                            return Prompt.END_OF_CONVERSATION;
                        }
                    })
                    .buildConversation(player)
                    .begin();
        }
    }

    /**
     * 重新打开网格界面（搜索后调用）
     */
    private void reopen(@NotNull Player player) {
        if (gui == null) {
            createGui();
        }
        Window.builder()
                .setUpperGui(gui)
                .setTitle(getGuiTitle())
                .setViewer(player)
                .build()
                .open();
    }

    /**
     * 刷新按钮：重新扫描网络
     */
    private class RefreshItem extends AbstractItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player viewer) {
            return ItemStackBuilder.of(Material.SUNFLOWER)
                    .name(Component.text("§e刷新"))
                    .lore(List.of(Component.text("§7点击重新扫描网络物品")));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull Click click) {
            refresh();
        }
    }
}
