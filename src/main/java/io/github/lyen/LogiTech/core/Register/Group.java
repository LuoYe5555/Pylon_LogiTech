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
    public static SimpleStaticGuidePage STORAGE;         // 存储
    public static SimpleStaticGuidePage PORTABLE_TOOLS;  // 便携式工具
    public static SimpleStaticGuidePage MAGIC_ITEM;      // 魔法物品
    public static SimpleStaticGuidePage MAGIC_TOOLS;     // 魔法工具
    public static SimpleStaticGuidePage NETWORK;         // 网络
    public static SimpleStaticGuidePage ADMIN;           // 管理专用
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
        // ====================
        
        // 初始化按钮列表（45个位置）
        for (int i = 0; i < 45; i++) {
            buttonOrderList.add(null);
        }

        setButtonAt(0, createSubGroup("info", Material.PAPER));
        setButtonAt(1, createSubGroup("material", Material.END_CRYSTAL));
        setButtonAt(2, createSubGroup("portable", Material.CRAFTING_TABLE));
        setButtonAt(3, createSubGroup("storage", Material.LIGHT_GRAY_STAINED_GLASS));
        setButtonAt(4, createSubGroup("network", Material.COMPASS));
        setButtonAt(5, createSubGroup("magic_item", Material.GOLD_INGOT));
        setButtonAt(6, createSubGroup("magic_tools", Material.KNOWLEDGE_BOOK));
        setButtonAt(7, createSubGroup("admin", Material.COMMAND_BLOCK));
        
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
            case "storage" -> STORAGE = group;
            case "magic_item" -> MAGIC_ITEM = group;
            case "magic_tools" -> MAGIC_TOOLS = group;
            case "portable" -> PORTABLE_TOOLS = group;
            case "network" -> NETWORK = group;
            case "admin" -> ADMIN = group;
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