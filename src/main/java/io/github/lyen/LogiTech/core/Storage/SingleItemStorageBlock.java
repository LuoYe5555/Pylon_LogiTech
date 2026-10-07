package io.github.lyen.LogiTech.core.Storage;

import io.github.pylonmc.rebar.block.RebarBlock;
import io.github.pylonmc.rebar.block.base.RebarGuiBlock;
import io.github.pylonmc.rebar.block.base.RebarLogisticBlock;
import io.github.pylonmc.rebar.block.base.RebarVirtualInventoryBlock;
import io.github.pylonmc.rebar.block.context.BlockBreakContext;
import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.pylonmc.rebar.logistics.LogisticGroup;
import io.github.pylonmc.rebar.logistics.LogisticGroupType;
import io.github.pylonmc.rebar.item.RebarItem;
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.inventory.VirtualInventory;
import xyz.xenondevs.invui.item.AbstractItem;
import xyz.xenondevs.invui.item.ItemProvider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class SingleItemStorageBlock extends RebarBlock implements RebarGuiBlock, RebarVirtualInventoryBlock, org.bukkit.inventory.InventoryHolder, RebarLogisticBlock {

    public static class Item extends RebarItem {
        public Item(@NotNull ItemStack stack) {
            super(stack);
        }
    }

    private static final int MAX_STORAGE = Integer.MAX_VALUE;

    private ItemStack storedItem = null;
    private int storedAmount = 0;
    
    private Gui gui;
    
    // 虚拟Inventory用于外部访问
    private VirtualInventory inputInventory;
    private VirtualInventory outputInventory;
    
    // 物流组 - 使用外部inventory作为物流组的基础
    private LogisticGroup inputLogisticGroup;
    private LogisticGroup outputLogisticGroup;

    public SingleItemStorageBlock(@NotNull Block block, @NotNull BlockCreateContext context) {
        super(block, context);
        
        // 初始化虚拟inventory
        initVirtualInventories();
        
        // 尝试从放置的物品中获取PDC数据
        ItemStack placedItem = context.getItem();
        if (placedItem != null) {
            org.bukkit.inventory.meta.ItemMeta meta = placedItem.getItemMeta();
            if (meta != null) {
                PersistentDataContainer itemPdc = meta.getPersistentDataContainer();
                if (itemPdc.has(RegisterKeys.STORED_ITEM_KEY, PersistentDataType.STRING)) {
                    // 从物品PDC加载数据
                    loadFromPdc(itemPdc);
                    return;
                }
            }
        }
        
        // 从方块的PDC加载数据
        if (block.getState() instanceof TileState) {
            TileState tileState = (TileState) block.getState();
            loadFromPdc(tileState.getPersistentDataContainer());
        }
    }

    public SingleItemStorageBlock(@NotNull Block block, @NotNull PersistentDataContainer pdc) {
        super(block, pdc);
        initVirtualInventories();
        loadFromPdc(pdc);
    }
    
    private void initVirtualInventories() {
        // 创建输入和输出虚拟inventory
        inputInventory = new VirtualInventory(1);
        outputInventory = new VirtualInventory(1);
        
        // 设置输出inventory的初始状态
        updateOutputInventory();
        
        // 初始化物流组
        inputLogisticGroup = new LogisticGroup(LogisticGroupType.INPUT);
        outputLogisticGroup = new LogisticGroup(LogisticGroupType.OUTPUT);
    }
    
    private void updateOutputInventory() {
        // VirtualInventory 没有 setItem 方法，
        // 外部访问通过 InventoryHolder.getInventory() 返回的 Bukkit Inventory 进行
        // GUI 显示通过自定义 OutputSlotItem 处理
    }
    
    @Override
    public @NotNull Map<@NotNull String, @NotNull VirtualInventory> getVirtualInventories() {
        return Map.of(
                "input", inputInventory,
                "output", outputInventory
        );
    }
    
    @Override
    public void write(@NotNull PersistentDataContainer pdc) {
        saveToPdc(pdc);
    }

    private void saveToPdc(PersistentDataContainer pdc) {
        if (storedItem != null && storedAmount > 0) {
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
                dataOutput.writeObject(storedItem);
                dataOutput.close();
                
                String encodedItem = Base64.getEncoder().encodeToString(outputStream.toByteArray());
                pdc.set(RegisterKeys.STORED_ITEM_KEY, PersistentDataType.STRING, encodedItem);
                pdc.set(RegisterKeys.STORED_AMOUNT_KEY, PersistentDataType.INTEGER, storedAmount);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            pdc.remove(RegisterKeys.STORED_ITEM_KEY);
            pdc.remove(RegisterKeys.STORED_AMOUNT_KEY);
        }
    }

    private void loadFromPdc(PersistentDataContainer pdc) {
        if (pdc.has(RegisterKeys.STORED_ITEM_KEY, PersistentDataType.STRING) && 
            pdc.has(RegisterKeys.STORED_AMOUNT_KEY, PersistentDataType.INTEGER)) {
            try {
                String encodedItem = pdc.get(RegisterKeys.STORED_ITEM_KEY, PersistentDataType.STRING);
                byte[] bytes = Base64.getDecoder().decode(encodedItem);
                ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
                BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
                storedItem = (ItemStack) dataInput.readObject();
                dataInput.close();
                storedAmount = pdc.get(RegisterKeys.STORED_AMOUNT_KEY, PersistentDataType.INTEGER);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
    
    // 用于货运系统访问的外部inventory
    private org.bukkit.inventory.Inventory externalInventory;
    
    @Override
    public @NotNull org.bukkit.inventory.Inventory getInventory() {
        if (externalInventory == null) {
            externalInventory = org.bukkit.Bukkit.createInventory(this, 9, "量子存储");
        }
        // 更新输出槽 (slot 1)
        if (storedItem != null && storedAmount > 0) {
            ItemStack outputStack = storedItem.clone();
            outputStack.setAmount(Math.min(storedAmount, 64));
            externalInventory.setItem(1, outputStack);
        } else {
            externalInventory.setItem(1, null);
        }
        return externalInventory;
    }

    private void notifyGuiChange() {
        if (gui != null) {
            gui.notifyWindows();
        }
        // 更新输出inventory
        updateOutputInventory();
    }

    @Override
    public @NotNull Gui createGui() {
        gui = Gui.builder()
                .setStructure(
                        "y i y x s x t o t",
                        "g g g g g g p g g"
                )
                .addIngredient('g', new GlassItem(Material.GREEN_STAINED_GLASS_PANE))
                .addIngredient('y', new GlassItem(Material.LIME_STAINED_GLASS_PANE))
                .addIngredient('x', new GlassItem(Material.BLUE_STAINED_GLASS_PANE))
                .addIngredient('t', new GlassItem(Material.BROWN_STAINED_GLASS_PANE))
                .addIngredient('i', new InputSlotItem())
                .addIngredient('o', new OutputSlotItem())
                .addIngredient('s', new StorageDisplayItem())
                .addIngredient('p', new QuickDepositItem())
                .build();
        return gui;
    }

    @Override
    public @NotNull Component getGuiTitle() {
        return noItalic(Component.text("§8[§6量子存储(∞)§8]"));
    }

    @Override
    public void onBreak(@NotNull List<@NotNull ItemStack> drops, @NotNull BlockBreakContext context) {
        // 创建自定义掉落物品（使用注册的RebarItem）
        ItemStack dropItem = ItemStackBuilder.rebar(Material.RED_TERRACOTTA, io.github.lyen.LogiTech.core.Register.RegisterKeys.QUANTUM_STORAGE)
                .name("<gold>量子存储(∞)")
                .build();
        
        // 如果有存储数据，添加lore和PDC
        if (storedItem != null && storedAmount > 0) {
            org.bukkit.inventory.meta.ItemMeta meta = dropItem.getItemMeta();
            if (meta != null) {
                // 获取物品显示名称（优先使用自定义名称）
                Component itemName;
                if (storedItem.hasItemMeta() && storedItem.getItemMeta().hasDisplayName()) {
                    itemName = storedItem.getItemMeta().displayName();
                } else {
                    itemName = Component.translatable(storedItem.getType().translationKey());
                }
                
                // 设置lore
                meta.lore(List.of(
                        noItalic(Component.text("")),
                        noItalic(Component.text("§7物品: §e").append(itemName)),
                        noItalic(Component.text("§7数量: §e" + storedAmount))
                ));
                
                // 保存数据到物品的PDC
                try {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
                    dataOutput.writeObject(storedItem);
                    dataOutput.close();
                    
                    String encodedItem = Base64.getEncoder().encodeToString(outputStream.toByteArray());
                    meta.getPersistentDataContainer().set(RegisterKeys.STORED_ITEM_KEY, PersistentDataType.STRING, encodedItem);
                    meta.getPersistentDataContainer().set(RegisterKeys.STORED_AMOUNT_KEY, PersistentDataType.INTEGER, storedAmount);
                    dropItem.setItemMeta(meta);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        drops.add(dropItem);
    }

    private void quickDeposit(Player player) {
        if (storedItem == null) {
            player.sendMessage("§c请先设置存储物品类型！");
            return;
        }

        int canStore = MAX_STORAGE - storedAmount;
        if (canStore <= 0) {
            player.sendMessage("§c存储已满！");
            return;
        }

        int deposited = 0;

        for (int i = 0; i < player.getInventory().getSize() && canStore > 0; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && isSameItem(item, storedItem)) {
                int take = Math.min(item.getAmount(), canStore);
                item.setAmount(item.getAmount() - take);
                storedAmount += take;
                deposited += take;
                canStore -= take;
            }
        }

        notifyGuiChange();

        if (deposited > 0) {
            player.sendMessage(Component.text("§a快速存入了 " + deposited + "x ").append(Component.translatable(storedItem.getType().translationKey())));
        } else {
            player.sendMessage("§c没有可存入的物品！");
        }
    }

    /**
     * 向量子存储存入物品（会修改传入物品的 amount），供网络系统调用
     */
    public void depositItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        if (storedItem == null) {
            storedItem = item.clone();
            storedItem.setAmount(1);
            storedAmount = item.getAmount();
            item.setAmount(0);
        } else if (isSameItem(item, storedItem)) {
            int canStore = MAX_STORAGE - storedAmount;
            int toStore = Math.min(item.getAmount(), canStore);
            storedAmount += toStore;
            item.setAmount(item.getAmount() - toStore);
        }

        notifyGuiChange();
    }

    /**
     * 从量子存储取出物品，供网络系统调用
     */
    public ItemStack withdrawItem(int amount) {
        if (storedItem == null || storedAmount <= 0) {
            return null;
        }

        int toWithdraw = Math.min(amount, storedAmount);
        ItemStack itemToGive = storedItem.clone();
        itemToGive.setAmount(toWithdraw);
        storedAmount -= toWithdraw;

        if (storedAmount <= 0) {
            storedItem = null;
            storedAmount = 0;
        }

        notifyGuiChange();
        return itemToGive;
    }

    private boolean isSameItem(ItemStack item1, ItemStack item2) {
        if (item1.getType() != item2.getType()) {
            return false;
        }
        if (item1.hasItemMeta() != item2.hasItemMeta()) {
            return false;
        }
        if (item1.hasItemMeta() && !item1.getItemMeta().equals(item2.getItemMeta())) {
            return false;
        }
        return true;
    }

    private static Component noItalic(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }

    public ItemStack getStoredItem() {
        return storedItem;
    }

    public int getStoredAmount() {
        return storedAmount;
    }

    // ===== RebarLogisticBlock 接口实现 =====
    
    @Override
    public @NotNull Map<@NotNull String, @NotNull LogisticGroup> getLogisticGroups() {
        // 更新输出物流组的物品信息
        org.bukkit.inventory.Inventory inv = getInventory();
        if (inv != null && inv.getItem(1) != null) {
            // 物流组会通过Inventory访问物品
        }
        return Map.of(
                "input", inputLogisticGroup,
                "output", outputLogisticGroup
        );
    }

    // ===== 内部类 =====

    private class GlassItem extends AbstractItem {
        private final Material glassType;

        public GlassItem(Material glassType) {
            this.glassType = glassType;
        }

        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player viewer) {
            return ItemStackBuilder.of(glassType);
        }

        @Override
        public void handleClick(@NotNull org.bukkit.event.inventory.ClickType clickType, @NotNull Player player, @NotNull xyz.xenondevs.invui.Click click) {}
    }

    private class InputSlotItem extends AbstractItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player viewer) {
            return ItemStackBuilder.of(Material.AIR);
        }

        @Override
        public void handleClick(@NotNull org.bukkit.event.inventory.ClickType clickType, @NotNull Player player, @NotNull xyz.xenondevs.invui.Click click) {
            ItemStack cursor = player.getItemOnCursor();
            
            if (cursor != null && cursor.getType() != Material.AIR) {
                if (storedItem == null || isSameItem(cursor, storedItem)) {
                    // 直接存入全部物品
                    if (storedItem == null) {
                        storedItem = cursor.clone();
                        storedItem.setAmount(1);
                        storedAmount = cursor.getAmount();
                    } else {
                        int canStore = MAX_STORAGE - storedAmount;
                        int toStore = Math.min(cursor.getAmount(), canStore);
                        storedAmount += toStore;
                    }
                    
                    // 清空光标上的物品
                    player.setItemOnCursor(null);
                    
                    notifyGuiChange();
                } else {
                    player.sendMessage("§c只能存储相同类型的物品！");
                }
            }
        }
    }

    private class OutputSlotItem extends AbstractItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player viewer) {
            if (storedItem != null && storedAmount > 0) {
                ItemStack displayStack = storedItem.clone();
                displayStack.setAmount(1);
                // 使用ItemStackBuilder包装，保留物品原本的名称和颜色
                return ItemStackBuilder.of(displayStack);
            } else {
                return ItemStackBuilder.of(Material.AIR);
            }
        }

        @Override
        public void handleClick(@NotNull org.bukkit.event.inventory.ClickType clickType, @NotNull Player player, @NotNull xyz.xenondevs.invui.Click click) {
            if (storedItem != null && storedAmount > 0) {
                int amount = clickType.isRightClick() ? 64 : 1;
                ItemStack itemToGive = withdrawItem(amount);
                
                if (itemToGive != null) {
                    ItemStack cursor = player.getItemOnCursor();
                    
                    if (cursor == null || cursor.getType() == Material.AIR) {
                        player.setItemOnCursor(itemToGive);
                    } else if (isSameItem(cursor, itemToGive)) {
                        int remaining = cursor.getMaxStackSize() - cursor.getAmount();
                        int toAdd = Math.min(remaining, itemToGive.getAmount());
                        cursor.setAmount(cursor.getAmount() + toAdd);
                        itemToGive.setAmount(itemToGive.getAmount() - toAdd);
                        
                        if (itemToGive.getAmount() > 0) {
                            player.getInventory().addItem(itemToGive);
                        }
                    } else {
                        player.getInventory().addItem(itemToGive);
                    }
                }
            }
        }
    }

    private class StorageDisplayItem extends AbstractItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player viewer) {
            if (storedItem != null) {
                ItemStack displayStack = storedItem.clone();
                displayStack.setAmount(1);
                
                // 获取物品原本的lore
                List<Component> originalLore = List.of();
                if (storedItem.hasItemMeta() && storedItem.getItemMeta().hasLore()) {
                    originalLore = storedItem.getItemMeta().lore();
                }
                
                // 创建新的lore列表，在原有lore基础上添加数量信息
                List<Component> newLore = new java.util.ArrayList<>();
                if (originalLore != null) {
                    newLore.addAll(originalLore);
                }
                newLore.add(noItalic(Component.text("")));
                newLore.add(noItalic(Component.text("§7数量: §e" + storedAmount + " / " + MAX_STORAGE)));
                
                // 设置新的lore到displayStack
                org.bukkit.inventory.meta.ItemMeta meta = displayStack.getItemMeta();
                if (meta != null) {
                    meta.lore(newLore);
                    displayStack.setItemMeta(meta);
                }
                
                // 使用ItemStackBuilder包装displayStack，保留原有名称和lore
                return ItemStackBuilder.of(displayStack);
            } else {
                return ItemStackBuilder.of(Material.GRAY_STAINED_GLASS_PANE)
                        .name(noItalic(Component.text("§7空")))
                        .lore(List.of(
                                noItalic(Component.text("§7放入物品到输入槽以设置类型"))));
            }
        }

        @Override
        public void handleClick(@NotNull org.bukkit.event.inventory.ClickType clickType, @NotNull Player player, @NotNull xyz.xenondevs.invui.Click click) {}
    }

    private class QuickDepositItem extends AbstractItem {
        @Override
        public @NotNull ItemProvider getItemProvider(@NotNull Player viewer) {
            return ItemStackBuilder.of(Material.PINK_STAINED_GLASS_PANE)
                    .name(noItalic(Component.text("§d一键存入")))
                    .lore(List.of(
                            noItalic(Component.text("§7点击将物品栏中所有可用物品")),
                            noItalic(Component.text("§7存入存储"))));
        }

        @Override
        public void handleClick(@NotNull org.bukkit.event.inventory.ClickType clickType, @NotNull Player player, @NotNull xyz.xenondevs.invui.Click click) {
            quickDeposit(player);
        }
    }

    public static class RegisterKeys {
        public static final org.bukkit.NamespacedKey STORED_ITEM_KEY = 
                new org.bukkit.NamespacedKey("logitech", "single_item_stored");
        public static final org.bukkit.NamespacedKey STORED_AMOUNT_KEY = 
                new org.bukkit.NamespacedKey("logitech", "single_item_amount");
    }
}