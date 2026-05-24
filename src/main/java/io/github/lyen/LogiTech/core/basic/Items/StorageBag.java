package io.github.lyen.LogiTech.core.basic.Items;

import io.github.lyen.LogiTech.MyAddon;
import io.github.pylonmc.rebar.item.RebarItem;
import io.github.pylonmc.rebar.item.base.RebarInteractor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 便携式存储背包类
 * 每个背包有独立的UUID，数据存储在物品上，认背包不认人
 * 支持多个背包，背包之间不能堆叠
 */
public class StorageBag extends RebarItem implements RebarInteractor, Listener {

    // 存储GUI的行数（6行 * 9列 = 54格）
    private static final int GUI_SLOTS = 54;

    // 存储GUI标题
    private static final String GUI_TITLE = "§8§l[§6§l背包§8§l] §7便携式存储背包";

    // 内存缓存 - 存储背包UUID到物品数组的映射
    private static final Map<String, ItemStack[]> bagInventories = new HashMap<>();

    // 配置文件
    private static File dataFile;
    private static FileConfiguration dataConfig;

    // PDC键
    private static final NamespacedKey BAG_UUID_KEY = new NamespacedKey(MyAddon.getInstance(), "bag_uuid");

    // 是否已初始化
    private static boolean initialized = false;

    public StorageBag(@NotNull ItemStack stack) {
        super(stack);
        // 只初始化一次
        if (!initialized) {
            Bukkit.getPluginManager().registerEvents(this, MyAddon.getInstance());
            initDataFile();
            initialized = true;
        }
    }

    /**
     * 初始化数据文件
     */
    private static void initDataFile() {
        if (dataFile == null) {
            dataFile = new File(MyAddon.getInstance().getDataFolder(), "storage_bags.yml");
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

    /**
     * 加载所有背包数据
     */
    private static void loadAllData() {
        for (String bagId : dataConfig.getKeys(false)) {
            String path = bagId + ".items";
            if (dataConfig.contains(path)) {
                try {
                    ItemStack[] items = ((java.util.List<ItemStack>) dataConfig.getList(path)).toArray(new ItemStack[0]);
                    // 确保数组长度正确
                    if (items.length < GUI_SLOTS) {
                        ItemStack[] newItems = new ItemStack[GUI_SLOTS];
                        System.arraycopy(items, 0, newItems, 0, items.length);
                        items = newItems;
                    }
                    bagInventories.put(bagId, items);
                } catch (Exception e) {
                    bagInventories.put(bagId, new ItemStack[GUI_SLOTS]);
                }
            }
        }
        System.out.println("[LogiTech] 已加载 " + bagInventories.size() + " 个存储背包数据");
    }

    /**
     * 保存指定背包的数据
     */
    private static void saveBagData(String bagId) {
        ItemStack[] items = bagInventories.get(bagId);
        if (items != null) {
            dataConfig.set(bagId + ".items", java.util.Arrays.asList(items));
            try {
                dataConfig.save(dataFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取背包的UUID，如果没有则生成一个新的
     */
    private String getBagUUID(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            // 创建新的ItemMeta
            meta = Bukkit.getItemFactory().getItemMeta(Material.CHEST);
        }
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        
        // 如果没有UUID，生成一个新的
        if (!pdc.has(BAG_UUID_KEY, PersistentDataType.STRING)) {
            String newUUID = UUID.randomUUID().toString();
            pdc.set(BAG_UUID_KEY, PersistentDataType.STRING, newUUID);
            stack.setItemMeta(meta);
            return newUUID;
        }
        
        return pdc.get(BAG_UUID_KEY, PersistentDataType.STRING);
    }

    /**
     * 右键使用时触发
     */
    @Override
    public void onUsedToClick(@NotNull PlayerInteractEvent event, @NotNull EventPriority priority) {
        if (!event.getAction().isRightClick()) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack stack = event.getItem();
        
        if (stack != null && stack.getType() != Material.AIR) {
            openStorageGUI(player, stack);
            event.setCancelled(true);
        }
    }

    /**
     * 打开存储GUI
     */
    private void openStorageGUI(Player player, ItemStack bagStack) {
        // 获取背包UUID
        String bagId = getBagUUID(bagStack);
        
        Inventory inventory = Bukkit.createInventory(null, GUI_SLOTS, GUI_TITLE);
        loadBagInventory(bagId, inventory);
        player.openInventory(inventory);
    }

    /**
     * 加载背包的存储数据
     */
    private void loadBagInventory(String bagId, Inventory inventory) {
        ItemStack[] cachedItems = bagInventories.get(bagId);
        if (cachedItems != null) {
            inventory.setContents(cachedItems);
            return;
        }
        
        // 初始化空数据
        bagInventories.put(bagId, new ItemStack[GUI_SLOTS]);
    }

    /**
     * GUI关闭事件
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        ItemStack bagStack = player.getInventory().getItemInMainHand();
        
        // 检查是否拿着存储背包
        if (bagStack != null && isStorageBag(bagStack)) {
            String bagId = getBagUUID(bagStack);
            ItemStack[] items = event.getInventory().getContents();
            bagInventories.put(bagId, items);
            saveBagData(bagId);
        }
    }

    /**
     * 检查物品是否为存储背包
     */
    private boolean isStorageBag(ItemStack stack) {
        if (stack == null || stack.getType() != Material.CHEST) {
            return false;
        }
        
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        return meta.getPersistentDataContainer().has(BAG_UUID_KEY, PersistentDataType.STRING);
    }

    /**
     * 构建方法
     */
    public ItemStack build() {
        return getStack();
    }
}
