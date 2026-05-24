package io.github.lyen.LogiTech.core.basic.Items;

import io.github.lyen.LogiTech.MyAddon;
import io.github.pylonmc.rebar.item.RebarItem;
import io.github.pylonmc.rebar.item.base.RebarInteractor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 便携式熔炉
 * 使用原版熔炉方块实现，完全沿用原版UI和功能
 */
public class PortableFurnace extends RebarItem implements RebarInteractor, Listener {

    // 配置文件
    private static File dataFile;
    private static FileConfiguration dataConfig;
    
    // 存储玩家的熔炉数据
    private static final Map<UUID, FurnaceData> playerFurnaceData = new HashMap<>();
    
    // 存储活跃的临时熔炉位置
    private static final Map<UUID, Location> activeFurnaces = new HashMap<>();
    
    // 是否已初始化
    private static boolean initialized = false;

    public PortableFurnace(@NotNull ItemStack stack) {
        super(stack);
        if (!initialized) {
            Bukkit.getPluginManager().registerEvents(this, MyAddon.getInstance());
            initDataFile();
            startSaveTask();
            initialized = true;
        }
    }

    private static void initDataFile() {
        dataFile = new File(MyAddon.getInstance().getDataFolder(), "portable_furnaces.yml");
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

    private static void loadAllData() {
        for (String uuidStr : dataConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                ItemStack smelting = dataConfig.getItemStack(uuidStr + ".smelting");
                ItemStack fuel = dataConfig.getItemStack(uuidStr + ".fuel");
                ItemStack result = dataConfig.getItemStack(uuidStr + ".result");
                int burnTime = dataConfig.getInt(uuidStr + ".burnTime", 0);
                int cookTime = dataConfig.getInt(uuidStr + ".cookTime", 0);
                
                FurnaceData data = new FurnaceData(smelting, fuel, result, burnTime, cookTime);
                playerFurnaceData.put(uuid, data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("[LogiTech] 已加载 " + playerFurnaceData.size() + " 个便携式熔炉数据");
    }

    private static void savePlayerData(UUID uuid) {
        FurnaceData data = playerFurnaceData.get(uuid);
        if (data != null) {
            dataConfig.set(uuid.toString() + ".smelting", data.smelting);
            dataConfig.set(uuid.toString() + ".fuel", data.fuel);
            dataConfig.set(uuid.toString() + ".result", data.result);
            dataConfig.set(uuid.toString() + ".burnTime", data.burnTime);
            dataConfig.set(uuid.toString() + ".cookTime", data.cookTime);
            try {
                dataConfig.save(dataFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void startSaveTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // 保存所有玩家的熔炉数据
                for (Map.Entry<UUID, FurnaceData> entry : playerFurnaceData.entrySet()) {
                    UUID uuid = entry.getKey();
                    // 如果玩家在线且有活跃的熔炉，从熔炉获取最新数据
                    if (activeFurnaces.containsKey(uuid)) {
                        Location loc = activeFurnaces.get(uuid);
                        Block block = loc.getBlock();
                        if (block.getType() == Material.FURNACE) {
                            Furnace furnace = (Furnace) block.getState();
                            FurnaceData data = entry.getValue();
                            data.smelting = furnace.getInventory().getSmelting();
                            data.fuel = furnace.getInventory().getFuel();
                            data.result = furnace.getInventory().getResult();
                            data.burnTime = furnace.getBurnTime();
                            data.cookTime = furnace.getCookTime();
                        }
                    }
                    savePlayerData(uuid);
                }
            }
        }.runTaskTimer(MyAddon.getInstance(), 20 * 30, 20 * 30); // 每30秒保存一次
    }

    @Override
    public void onUsedToClick(@NotNull PlayerInteractEvent event, @NotNull EventPriority priority) {
        if (!event.getAction().isRightClick()) {
            return;
        }

        Player player = event.getPlayer();
        openPortableFurnace(player);
        event.setCancelled(true);
    }

    private void openPortableFurnace(Player player) {
        UUID uuid = player.getUniqueId();
        
        // 检查是否已有活跃的熔炉
        if (activeFurnaces.containsKey(uuid)) {
            Location loc = activeFurnaces.get(uuid);
            Block block = loc.getBlock();
            
            // 如果熔炉还在，直接打开
            if (block.getType() == Material.FURNACE) {
                Furnace furnace = (Furnace) block.getState();
                player.openInventory(furnace.getInventory());
                return;
            }
        }
        
        // 创建新的临时熔炉
        createTemporaryFurnace(player);
    }

    private void createTemporaryFurnace(Player player) {
        UUID uuid = player.getUniqueId();
        Location playerLoc = player.getLocation();
        World world = player.getWorld();
        
        // 在玩家上方找一个安全的位置（20格高处）
        Location furnaceLoc = playerLoc.add(0, 20, 0);
        
        // 确保位置安全
        while (furnaceLoc.getBlock().getType() != Material.AIR && furnaceLoc.getY() < world.getMaxHeight()) {
            furnaceLoc.add(0, 1, 0);
        }
        
        // 设置熔炉方块
        furnaceLoc.getBlock().setType(Material.FURNACE);
        
        // 获取熔炉状态
        Furnace furnace = (Furnace) furnaceLoc.getBlock().getState();
        
        // 加载之前保存的数据
        FurnaceData data = playerFurnaceData.get(uuid);
        if (data != null) {
            furnace.getInventory().setSmelting(data.smelting);
            furnace.getInventory().setFuel(data.fuel);
            furnace.getInventory().setResult(data.result);
            furnace.setBurnTime((short) data.burnTime);
            furnace.setCookTime((short) data.cookTime);
        } else {
            // 创建新数据
            data = new FurnaceData(null, null, null, 0, 0);
            playerFurnaceData.put(uuid, data);
        }
        
        furnace.update();
        
        // 记录活跃熔炉位置
        activeFurnaces.put(uuid, furnaceLoc);
        
        // 打开熔炉界面
        player.openInventory(furnace.getInventory());
        
        System.out.println("[LogiTech] 为玩家 " + player.getName() + " 创建了临时熔炉");
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // 检查是否是熔炉界面
        if (!(event.getView().getTopInventory().getHolder() instanceof Furnace)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // 检查是否是我们创建的临时熔炉
        if (!activeFurnaces.containsKey(uuid)) {
            return;
        }
        
        // 保存数据
        Furnace furnace = (Furnace) event.getView().getTopInventory().getHolder();
        FurnaceData data = playerFurnaceData.get(uuid);
        if (data != null) {
            data.smelting = furnace.getInventory().getSmelting();
            data.fuel = furnace.getInventory().getFuel();
            data.result = furnace.getInventory().getResult();
            data.burnTime = furnace.getBurnTime();
            data.cookTime = furnace.getCookTime();
        }
        
        // 不要立即删除熔炉，让它继续燃烧
        // 删除操作在玩家退出时执行
        
        savePlayerData(uuid);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        
        // 如果有活跃的熔炉，删除它并保存数据
        if (activeFurnaces.containsKey(uuid)) {
            Location loc = activeFurnaces.get(uuid);
            Block block = loc.getBlock();
            
            if (block.getType() == Material.FURNACE) {
                Furnace furnace = (Furnace) block.getState();
                
                // 保存最后状态
                FurnaceData data = playerFurnaceData.get(uuid);
                if (data != null) {
                    data.smelting = furnace.getInventory().getSmelting();
                    data.fuel = furnace.getInventory().getFuel();
                    data.result = furnace.getInventory().getResult();
                    data.burnTime = furnace.getBurnTime();
                    data.cookTime = furnace.getCookTime();
                }
                
                // 删除熔炉方块
                block.setType(Material.AIR);
            }
            
            activeFurnaces.remove(uuid);
            savePlayerData(uuid);
            
            System.out.println("[LogiTech] 玩家 " + event.getPlayer().getName() + " 退出，删除临时熔炉");
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        // 如果卸载的区块中有我们的熔炉，保存数据
        for (Map.Entry<UUID, Location> entry : activeFurnaces.entrySet()) {
            Location loc = entry.getValue();
            if (event.getChunk().equals(loc.getChunk())) {
                // 保存数据但不删除熔炉
                // 玩家下次打开时会重新加载
            }
        }
    }

    private static class FurnaceData {
        ItemStack smelting;
        ItemStack fuel;
        ItemStack result;
        int burnTime;
        int cookTime;

        FurnaceData(ItemStack smelting, ItemStack fuel, ItemStack result, int burnTime, int cookTime) {
            this.smelting = smelting;
            this.fuel = fuel;
            this.result = result;
            this.burnTime = burnTime;
            this.cookTime = cookTime;
        }
    }
}
