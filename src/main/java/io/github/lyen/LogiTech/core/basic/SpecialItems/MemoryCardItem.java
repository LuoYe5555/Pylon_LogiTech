package io.github.lyen.LogiTech.core.basic.SpecialItems;

import io.github.pylonmc.rebar.item.RebarItem;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

/**
 * 容量卡物品：实例化（进入世界/发放）时若无编号则分配一个全局唯一编号，
 * 编号持久化在卡片 PDC 中并显示在名称后缀，使多张同类卡内容互不混淆。
 */
public class MemoryCardItem extends RebarItem {

    /** 卡片编号 PDC 键 */
    public static final org.bukkit.NamespacedKey CARD_ID_KEY =
            new org.bukkit.NamespacedKey("logitech", "card_id");

    /** 全局发号器（与注册模板共享） */
    public static final MemoryCardIdIssuer ID_ISSUER = new MemoryCardIdIssuer();

    public MemoryCardItem(@NotNull ItemStack stack) {
        super(stack);
        ensureId(stack);
    }

    /**
     * 卡片没有编号时分配一个并写回，名称后缀追加 "#编号"；
     * 同时写入容量标记（CARD_AMOUNT_KEY=0），使 MemoryCard.isMemoryCard 能识别。
     * 公开供存储器卡槽、作弊指南等发放路径调用（模板本身不带编号）。
     */
    public static void ensureId(@NotNull ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }
        boolean hasAmountKey = meta.getPersistentDataContainer()
                .has(io.github.lyen.LogiTech.core.Register.RegisterKeys.CARD_AMOUNT_KEY, PersistentDataType.INTEGER);
        if (meta.getPersistentDataContainer().has(CARD_ID_KEY, PersistentDataType.LONG) && hasAmountKey) {
            return;
        }
        long id = meta.getPersistentDataContainer().getOrDefault(CARD_ID_KEY, PersistentDataType.LONG, -1L);
        if (id < 0) {
            id = ID_ISSUER.nextId();
            meta.getPersistentDataContainer().set(CARD_ID_KEY, PersistentDataType.LONG, id);
        }
        if (!hasAmountKey) {
            meta.getPersistentDataContainer().set(
                    io.github.lyen.LogiTech.core.Register.RegisterKeys.CARD_AMOUNT_KEY, PersistentDataType.INTEGER, 0);
        }

        // 模板名存在 item_name 数据组件（翻译键）里，meta.displayName()（custom_name）读不到，
        // 需从数据组件读取；自定义翻译键客户端无法解析，必须在服务端渲染成文字再拼接编号
        net.kyori.adventure.text.Component currentName = meta.displayName();
        if (currentName == null) {
            currentName = stack.getData(io.papermc.paper.datacomponent.DataComponentTypes.ITEM_NAME);
        }
        if (currentName == null) {
            currentName = net.kyori.adventure.text.Component.translatable(stack.getType());
        }
        String plain = null;
        try {
            plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(net.kyori.adventure.translation.GlobalTranslator.render(
                            currentName, java.util.Locale.CHINA));
        } catch (Exception ignored) {
        }
        // 渲染失败或仍是翻译键（含"."）时按材质回退
        if (plain == null || plain.isBlank() || plain.contains(".")) {
            plain = fallbackName(stack.getType());
        }
        meta.displayName(net.kyori.adventure.text.Component.text("§6" + plain + " §8#" + id)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        stack.setItemMeta(meta);
    }

    private static String fallbackName(@NotNull org.bukkit.Material type) {
        return switch (type) {
            case MUSIC_DISC_CAT -> "1K存储卡";
            case MUSIC_DISC_BLOCKS -> "4K存储卡";
            case MUSIC_DISC_FAR -> "16K存储卡";
            default -> type.name();
        };
    }

    /**
     * 读取卡片编号（未编号返回 -1）
     */
    public static long getId(@NotNull ItemStack card) {
        ItemMeta meta = card.getItemMeta();
        if (meta == null) {
            return -1;
        }
        Long id = meta.getPersistentDataContainer().get(CARD_ID_KEY, PersistentDataType.LONG);
        return id == null ? -1 : id;
    }
}
