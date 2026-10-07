package io.github.lyen.LogiTech.core.Storage;

import io.github.lyen.LogiTech.MyAddon;
import io.github.pylonmc.rebar.block.RebarBlock;
import io.github.pylonmc.rebar.block.base.RebarGuiBlock;
import io.github.pylonmc.rebar.block.base.RebarVirtualInventoryBlock;
import io.github.pylonmc.rebar.block.context.BlockBreakContext;
import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.pylonmc.rebar.item.RebarItem;
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder;
import io.github.pylonmc.rebar.util.gui.GuiItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.inventory.VirtualInventory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class StorageBlock extends RebarBlock implements RebarGuiBlock, RebarVirtualInventoryBlock {

    public static class Item extends RebarItem {
        public Item(@NotNull ItemStack stack) {
            super(stack);
        }
    }

    public static class RegisterKeys {
        public static final org.bukkit.NamespacedKey STORAGE_DATA_KEY = 
                new org.bukkit.NamespacedKey("logitech", "storage_items");
    }

    private final VirtualInventory storageInventory = new VirtualInventory(54);
    private ItemStack[] storedItems = new ItemStack[54];

    /**
     * 获取存储方块的虚拟背包（网络系统通过它读写物品）
     */
    public VirtualInventory getStorageInventory() {
        return storageInventory;
    }

    public StorageBlock(@NotNull Block block, @NotNull BlockCreateContext context) {
        super(block, context);
    }

    public StorageBlock(@NotNull Block block, @NotNull PersistentDataContainer pdc) {
        super(block, pdc);
        loadFromPdc(pdc);
    }

    @Override
    public void write(@NotNull PersistentDataContainer pdc) {
        saveToPdc(pdc);
    }

    private void saveToPdc(PersistentDataContainer pdc) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            org.bukkit.util.io.BukkitObjectOutputStream dataOutput = new org.bukkit.util.io.BukkitObjectOutputStream(outputStream);
            
            // 获取所有物品
            ItemStack[] items = new ItemStack[54];
            for (int i = 0; i < 54; i++) {
                items[i] = storageInventory.getItem(i);
            }
            
            dataOutput.writeObject(items);
            dataOutput.close();
            
            String encodedItems = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            pdc.set(RegisterKeys.STORAGE_DATA_KEY, PersistentDataType.STRING, encodedItems);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadFromPdc(PersistentDataContainer pdc) {
        if (pdc.has(RegisterKeys.STORAGE_DATA_KEY, PersistentDataType.STRING)) {
            try {
                String encodedItems = pdc.get(RegisterKeys.STORAGE_DATA_KEY, PersistentDataType.STRING);
                byte[] bytes = Base64.getDecoder().decode(encodedItems);
                ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
                org.bukkit.util.io.BukkitObjectInputStream dataInput = new org.bukkit.util.io.BukkitObjectInputStream(inputStream);
                
                ItemStack[] items = (ItemStack[]) dataInput.readObject();
                dataInput.close();
                
                // 将物品加载到虚拟背包
                if (items != null) {
                    storedItems = items;
                    for (int i = 0; i < items.length && i < storageInventory.getSize(); i++) {
                        ItemStack item = items[i];
                        if (item != null && !item.getType().isAir()) {
                            storageInventory.setItem(
                                    xyz.xenondevs.invui.inventory.event.UpdateReason.SUPPRESSED, i, item);
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public @NotNull Gui createGui() {
        return Gui.builder()
                .setStructure(
                        "s s s s s s s s s",
                        "s s s s s s s s s",
                        "s s s s s s s s s",
                        "s s s s s s s s s",
                        "s s s s s s s s s",
                        "s s s s s s s s s"
                )
                .addIngredient('s', storageInventory)
                .build();
    }

    @Override
    public @NotNull Component getGuiTitle() {
        return Component.text("§8[§6§l存储方块§8§l]");
    }

    @Override
    public void onBreak(@NotNull List<@NotNull ItemStack> drops, @NotNull BlockBreakContext context) {
        // 创建存储方块本体掉落
        ItemStack blockItem = ItemStackBuilder.rebar(Material.LIGHT_GRAY_STAINED_GLASS, io.github.lyen.LogiTech.core.Register.RegisterKeys.STORAGE_BLOCK)
                .name("<gold>一个普通的存储方块")
                .build();
        drops.add(blockItem);
        
        // 掉落存储的物品
        for (int i = 0; i < 54; i++) {
            ItemStack item = storageInventory.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                drops.add(item.clone());
            }
        }
    }

    @Override
    public @NotNull Map<String, VirtualInventory> getVirtualInventories() {
        return Map.of("storage", storageInventory);
    }

    private static Component noItalic(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }
}
