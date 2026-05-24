package io.github.lyen.LogiTech.core.basic.Items;

import io.github.lyen.LogiTech.MyAddon;
import io.github.pylonmc.rebar.item.RebarItem;
import io.github.pylonmc.rebar.item.base.RebarInteractor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 便携式末影箱
 * 右键打开末影箱GUI，数据持久化保存到文件
 */
public class PortableEnderChest extends RebarItem implements RebarInteractor, Listener {

    private static final String GUI_TITLE = "便携式末影箱";
    private static final int GUI_SLOTS = 27; // 末影箱容量

    // 内存缓存
    private static final Map<String, ItemStack[]> playerInventories = new HashMap<>();

    // 配置文件
    private static File dataFile;
    private static FileConfiguration dataConfig;

    public PortableEnderChest(@NotNull ItemStack stack) {
        super(stack);
        Bukkit.getPluginManager().registerEvents(this, MyAddon.getInstance());
        initDataFile();
    }

    private static void initDataFile() {
        if (dataFile == null) {
            dataFile = new File(MyAddon.getInstance().getDataFolder(), "portable_enderchest.yml");
            if (!dataFile.exists()) {
                try {
                    dataFile.getParentFile().mkdirs();
                    dataFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            dataConfig = YamlConfiguration.loadConfiguration(dataFile);
            loadAllData();
        }
    }

    private static void loadAllData() {
        for (String playerId : dataConfig.getKeys(false)) {
            String path = playerId + ".items";
            if (dataConfig.contains(path)) {
                try {
                    ItemStack[] items = ((java.util.List<ItemStack>) dataConfig.getList(path)).toArray(new ItemStack[0]);
                    playerInventories.put(playerId, items);
                } catch (Exception e) {
                    playerInventories.put(playerId, new ItemStack[GUI_SLOTS]);
                }
            }
        }
        System.out.println("[LogiTech] 已加载 " + playerInventories.size() + " 个便携式末影箱数据");
    }

    private static void savePlayerData(String playerId) {
        ItemStack[] items = playerInventories.get(playerId);
        if (items != null) {
            dataConfig.set(playerId + ".items", java.util.Arrays.asList(items));
            try {
                dataConfig.save(dataFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onUsedToClick(@NotNull PlayerInteractEvent event, @NotNull EventPriority priority) {
        if (!event.getAction().isRightClick()) {
            return;
        }

        Player player = event.getPlayer();
        openEnderChestGUI(player);
        event.setCancelled(true);
    }

    private void openEnderChestGUI(Player player) {
        Inventory inventory = Bukkit.createInventory(null, GUI_SLOTS, GUI_TITLE);
        loadPlayerInventory(player, inventory);
        player.openInventory(inventory);
    }

    private void loadPlayerInventory(Player player, Inventory inventory) {
        String playerId = player.getUniqueId().toString();
        
        ItemStack[] cachedItems = playerInventories.get(playerId);
        if (cachedItems != null) {
            inventory.setContents(cachedItems);
            return;
        }
        
        playerInventories.put(playerId, new ItemStack[GUI_SLOTS]);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        String playerId = player.getUniqueId().toString();
        
        ItemStack[] items = event.getInventory().getContents();
        playerInventories.put(playerId, items);
        savePlayerData(playerId);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String playerId = event.getPlayer().getUniqueId().toString();
        if (playerInventories.containsKey(playerId)) {
            savePlayerData(playerId);
        }
    }
}
