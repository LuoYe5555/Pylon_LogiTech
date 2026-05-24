package io.github.lyen.LogiTech.core.Register;

import io.github.pylonmc.pylon.PylonPages;
import io.github.pylonmc.rebar.content.guide.RebarGuide;
import io.github.pylonmc.rebar.guide.button.AddonPageButton;
import io.github.pylonmc.rebar.guide.pages.base.SimpleStaticGuidePage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 嵌套式物品组管理
 */
public final class Group {
    
    // 缓存创建的物品组页面
    private static SimpleStaticGuidePage currentPage;
    
    // 物品组缓存 - 按ID保存已创建的物品组
    private static java.util.Map<String, SimpleStaticGuidePage> pageCache = new java.util.HashMap<>();
    
    // LogiTech 根物品组
    private static SimpleStaticGuidePage logitechRoot;
    
    // 分隔符计数器
    private static int separatorCounter = 0;
    
    // 存储按钮列表用于控制顺序
    private static List<Object> buttonOrderList = new ArrayList<>();
    
    // ====================
    // 物品组定义
    // ====================
    public static SimpleStaticGuidePage INFO;            // 信息 
    public static SimpleStaticGuidePage MATERIAL;        // 材料 
    public static SimpleStaticGuidePage BASIC;           // 基础 
    public static SimpleStaticGuidePage CARGO;           // 货运 
    public static SimpleStaticGuidePage SINGULARITY;     // 奇点 
    public static SimpleStaticGuidePage ADVANCED;        // 进阶 
    public static SimpleStaticGuidePage BEYOND;          // 超越 
    public static SimpleStaticGuidePage VANILLA;         // 原版 
    public static SimpleStaticGuidePage MANUAL;          // 手动 
    public static SimpleStaticGuidePage SPECIAL;         // 特殊 
    public static SimpleStaticGuidePage SPACE;           // 空间 
    public static SimpleStaticGuidePage GENERATORS;      // 发电机 
    public static SimpleStaticGuidePage ENERGY;          // 能源 
    public static SimpleStaticGuidePage FUNCTIONAL;      // 功能性 
    public static SimpleStaticGuidePage EXPERIMENTAL;    // 实验性 
    public static SimpleStaticGuidePage TOOLS;           // 工具 
    public static SimpleStaticGuidePage MORE;            // 更多 
    public static SimpleStaticGuidePage MISC;            // 杂项
    public static SimpleStaticGuidePage MAGIC;           // 魔法 
    public static SimpleStaticGuidePage TECH;            // 科技 
    public static SimpleStaticGuidePage PORTABLE_TOOLS;  // 便携式工具 
    
    /**
     * 初始化根物品组和所有子物品组
     */
    public static void initialize() {
        // 创建 LogiTech 根页面（父物品组）
        logitechRoot = new SimpleStaticGuidePage(registerKey("logitech"));
        pageCache.put("logitech", logitechRoot);
        
        // 添加到 Rebar 指南根页面
        RebarGuide.getRootPage().addButton(new AddonPageButton(io.github.lyen.LogiTech.MyAddon.getInstance(), logitechRoot));
        
        // 设置当前活动物品组为根物品组
        currentPage = logitechRoot;
        
        // ====================
        // 创建子物品组
        // 5行 × 9列 = 45格界面
        // 紫色玻璃 = 分隔符
        // ====================
        
        // 初始化按钮列表（45个位置）
        for (int i = 0; i < 45; i++) {
            buttonOrderList.add(null);
        }
        
        // ============ 第一行 (slots 0-8) ============
        // [纸][紫色玻璃][紫水晶][深色橡木原木][圆石][熔炉][木棍][紫色玻璃][下界合金锄]
        setButtonAt(0, createSubGroup("info", Material.PAPER));                 // 版本与说明 - 纸
        setButtonAt(1, createSeparator());                                      // 紫色玻璃分隔符
        setButtonAt(2, createSubGroup("material", Material.END_CRYSTAL));       // 材料 - 末地水晶
        setButtonAt(3, createSubGroup("portable", Material.CRAFTING_TABLE));        // 便携式工具 - 工作台
        setButtonAt(4, createSubGroup("magic", Material.AMETHYST_SHARD));       // 魔法 - 紫水晶碎片
        setButtonAt(5, createSubGroup("generators", Material.FURNACE));         // 发电机 - 熔炉
        setButtonAt(6, createSubGroup("tools", Material.STICK));                // 工具 - 木棍
        setButtonAt(7, createSeparator());                                      // 紫色玻璃分隔符
        setButtonAt(8, createSubGroup("forging", Material.NETHERITE_HOE));         // 工具与锻造 - 下界合金锄
        
        // ============ 第二行 (slots 9-17) ============
        // [铁块][紫色玻璃][紫色玻璃][紫色玻璃][紫色玻璃][紫色玻璃][紫色玻璃][紫色玻璃][龙蛋]
        setButtonAt(9, createSubGroup("basic", Material.IRON_BLOCK));           // 基础 - 铁块
        setButtonAt(10, createSeparator());                                     // 紫色玻璃分隔符
        setButtonAt(11, createSeparator());                                     // 紫色玻璃分隔符
        setButtonAt(12, createSeparator());                                     // 紫色玻璃分隔符
        setButtonAt(13, createSeparator());                                     // 紫色玻璃分隔符
        setButtonAt(14, createSeparator());                                     // 紫色玻璃分隔符
        setButtonAt(15, createSeparator());                                     // 紫色玻璃分隔符
        setButtonAt(16, createSeparator());                                     // 紫色玻璃分隔符
        setButtonAt(17, createSubGroup("beyond", Material.DRAGON_EGG));         // 超越 - 龙蛋
        
        // ============ 第三行 (slots 18-26) ============
        // [书][紫色玻璃][漏斗][箱子][发射器][信标][下界之星][紫色玻璃][红石]
        setButtonAt(18, createSubGroup("vanilla", Material.BOOK));              // 原版 - 书
        setButtonAt(19, createSeparator());                                     // 紫色玻璃分隔符
        setButtonAt(20, createSubGroup("cargo", Material.HOPPER));              // 货运 - 漏斗
        setButtonAt(21, createSubGroup("functional", Material.CHEST));          // 功能性 - 箱子
        setButtonAt(22, createSubGroup("advanced", Material.DISPENSER));        // 进阶 - 发射器
        setButtonAt(23, createSubGroup("special", Material.BEACON));            // 特殊 - 信标
        setButtonAt(24, createSubGroup("singularity", Material.NETHER_STAR));   // 奇点 - 下界之星
        setButtonAt(25, createSeparator());                                     // 紫色玻璃分隔符
        setButtonAt(26, createSubGroup("energy", Material.REDSTONE));           // 能源 - 红石
        
        // ============ 第四行 (slots 27-35) ============
        // [石头][紫色玻璃][紫色玻璃][紫色玻璃][紫水晶块][紫色玻璃][紫色玻璃][紫色玻璃][海晶碎片]
        setButtonAt(27, createSubGroup("misc", Material.STONE));                // 杂项 - 石头
        setButtonAt(28, createSeparator());                                     // 紫色玻璃分隔符
        setButtonAt(29, createSeparator());                                     // 紫色玻璃分隔符
        setButtonAt(30, createSeparator());                                     // 紫色玻璃分隔符
        setButtonAt(31, createSubGroup("experimental", Material.AMETHYST_BLOCK)); // 实验性 - 紫水晶块
        setButtonAt(32, createSeparator());                                     // 紫色玻璃分隔符
        setButtonAt(33, createSeparator());                                     // 紫色玻璃分隔符
        setButtonAt(34, createSeparator());                                     // 紫色玻璃分隔符
        setButtonAt(35, createSubGroup("tech", Material.PRISMARINE_SHARD));     // 科技 - 海晶碎片
        
        // ============ 第五行 (slots 36-44) ============
        // [紫色玻璃][紫色玻璃][紫色玻璃][紫色玻璃][紫色玻璃][紫色玻璃][紫色玻璃][海晶石][紫色玻璃]
        setButtonAt(36, createSeparator());                                     // 紫色玻璃分隔符
        setButtonAt(37, createSeparator());                                     // 紫色玻璃分隔符
        setButtonAt(38, createSeparator());                                     // 紫色玻璃分隔符
        setButtonAt(39, createSeparator());                                     // 紫色玻璃分隔符
        setButtonAt(40, createSeparator());                                     // 紫色玻璃分隔符
        setButtonAt(41, createSeparator());                                     // 紫色玻璃分隔符
        setButtonAt(42, createSeparator());                                     // 紫色玻璃分隔符
        setButtonAt(43, createSubGroup("space", Material.PRISMARINE));          // 空间 - 海晶石
        setButtonAt(44, createSeparator());                                     // 紫色玻璃分隔符
        
        // 将按钮列表应用到页面
        applyButtonOrder();
        
        System.out.println("[LogiTech] 嵌套式物品组系统初始化完成");
        System.out.println("[LogiTech] 物品组结构已完成布局");
    }
    
    /**
     * 在指定位置设置按钮
     */
    private static void setButtonAt(int slot, Object buttonInfo) {
        if (slot >= 0 && slot < buttonOrderList.size()) {
            buttonOrderList.set(slot, buttonInfo);
        }
    }
    
    /**
     * 创建子物品组并返回其信息对象
     */
    private static Object createSubGroup(String groupId, Material icon) {
        SimpleStaticGuidePage group = new SimpleStaticGuidePage(registerKey(groupId));
        
        // 保存到缓存
        pageCache.put(groupId, group);
        
        // 根据物品组ID设置对应的静态变量
        switch (groupId) {
            case "info" -> INFO = group;
            case "material" -> MATERIAL = group;
            case "basic" -> BASIC = group;
            case "cargo" -> CARGO = group;
            case "singularity" -> SINGULARITY = group;
            case "advanced" -> ADVANCED = group;
            case "beyond" -> BEYOND = group;
            case "vanilla" -> VANILLA = group;
            case "portable" -> PORTABLE_TOOLS = group;
            case "special" -> SPECIAL = group;
            case "space" -> SPACE = group;
            case "generators" -> GENERATORS = group;
            case "energy" -> ENERGY = group;
            case "functional" -> FUNCTIONAL = group;
            case "experimental" -> EXPERIMENTAL = group;
            case "tools" -> TOOLS = group;
            case "more" -> MORE = group;
            case "misc" -> MISC = group;
            case "magic" -> MAGIC = group;
            case "tech" -> TECH = group;
        }
        
        System.out.println("[LogiTech] 创建子物品组: " + groupId);
        
        // 返回包含图标和页面的对象
        return new ButtonInfo(icon, group);
    }
    
    /**
     * 创建紫色玻璃板分隔符并返回其信息对象
     */
    private static Object createSeparator() {
        String separatorId = "separator_" + separatorCounter++;
        SimpleStaticGuidePage separatorPage = new SimpleStaticGuidePage(registerKey(separatorId));
        
        pageCache.put(separatorId, separatorPage);
        
        System.out.println("[LogiTech] 创建分隔符: " + separatorId);
        
        return new ButtonInfo(Material.PURPLE_STAINED_GLASS_PANE, separatorPage);
    }
    
    /**
     * 将按钮列表应用到页面
     */
    private static void applyButtonOrder() {
        try {
            // 获取 buttons 字段
            Field buttonsField = SimpleStaticGuidePage.class.getDeclaredField("buttons");
            buttonsField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            List<Object> buttons = (List<Object>) buttonsField.get(currentPage);
            buttons.clear();
            
            // 按顺序添加按钮
            for (Object obj : buttonOrderList) {
                if (obj instanceof ButtonInfo buttonInfo) {
                    // 直接调用 addPage 方法
                    currentPage.addPage(buttonInfo.icon, buttonInfo.page);
                }
            }
            
        } catch (Exception e) {
            System.err.println("[LogiTech] 应用按钮顺序失败: " + e.getMessage());
            e.printStackTrace();
            // 回退到简单方式
            fallbackApply();
        }
    }
    
    /**
     * 回退方式：直接按顺序添加
     */
    private static void fallbackApply() {
        for (Object obj : buttonOrderList) {
            if (obj instanceof ButtonInfo buttonInfo) {
                currentPage.addPage(buttonInfo.icon, buttonInfo.page);
            }
        }
    }
    
    /**
     * 按钮信息内部类
     */
    private static class ButtonInfo {
        final Material icon;
        final SimpleStaticGuidePage page;
        
        ButtonInfo(Material icon, SimpleStaticGuidePage page) {
            this.icon = icon;
            this.page = page;
        }
    }
    
    /**
     * 设置当前活动物品组
     * @param page 物品组页面
     */
    public static void setCurrentPage(SimpleStaticGuidePage page) {
        currentPage = page;
    }
    
    /**
     * 获取当前活动物品组
     * @return 当前物品组页面
     */
    public static SimpleStaticGuidePage getCurrentPage() {
        return currentPage;
    }
    
    /**
     * 获取指定物品组
     * @param groupId 物品组ID
     * @return 物品组页面，如果不存在返回null
     */
    public static SimpleStaticGuidePage getGroup(String groupId) {
        return pageCache.get(groupId);
    }
    
    /**
     * 向当前活动物品组添加物品
     * @param item 要添加的物品
     */
    public static void addItemToGroup(ItemStack item) {
        if (currentPage != null) {
            currentPage.addItem(item);
        }
    }
    
    /**
     * 向指定物品组添加物品
     * @param groupId 物品组ID
     * @param item 要添加的物品
     */
    public static void addItemToGroup(String groupId, ItemStack item) {
        var group = pageCache.get(groupId);
        if (group != null) {
            group.addItem(item);
        }
    }
    
    /**
     * 注册 NamespacedKey
     */
    private static org.bukkit.NamespacedKey registerKey(String key) {
        return new org.bukkit.NamespacedKey("logitech", key);
    }
}