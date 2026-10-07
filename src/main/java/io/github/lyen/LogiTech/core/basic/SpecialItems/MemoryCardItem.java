package io.github.lyen.LogiTech.Core.Basic.SpecialItems;

import io.github.pylonmc.rebar.item.RebarItem;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

/**
 * 容量卡物品（Rebar 物品）。
 *
 * 重要：本构造函数会被 Rebar 在各种"只读包装"场景高频调用——最典型的是
 * RebarItem.fromStack：发包翻译器 PlayerTranslationHandler 对每一个 Rebar 物品、
 * 每一次发包都会反射调用本构造函数（传入的往往是发包副本）。
 * 因此这里【绝对不能】做发号/改名等有副作用的操作，否则每发一次包就消耗一个编号，
 * 玩家会看到卡号不停跳变、发号器被刷爆。
 *
 * 编号只在卡被真实放入存储器卡槽（写入真实物品）时由 ensureId 分配一次。
 */
public class MemoryCardItem extends RebarItem {

    /** 卡片编号 PDC 键 */
    public static final org.bukkit.NamespacedKey CARD_ID_KEY =
            new org.bukkit.NamespacedKey("logitech", "card_id");

    /** 全局发号器（与注册模板共享） */
    public static final MemoryCardIdIssuer ID_ISSUER = new MemoryCardIdIssuer();

    public MemoryCardItem(@NotNull ItemStack stack) {
        super(stack);
    }

    /**
     * 卡片没有编号时分配一个并写回，名称后缀追加 "#编号"；
     * 同时写入容量标记（CARD_AMOUNT_KEY=0），使 MemoryCard.isMemoryCard 能识别。
     * 仅在卡真实放入存储器卡槽时调用（写入的是世界中的真实物品，不是发包副本）。
     */
    public static void ensureId(@NotNull ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }
        org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
        boolean hasId = pdc.has(CARD_ID_KEY, PersistentDataType.LONG);
        boolean hasAmountKey = pdc.has(
                io.github.lyen.LogiTech.Core.Register.RegisterKeys.CARD_AMOUNT_KEY, PersistentDataType.INTEGER);
        if (hasId && hasAmountKey) {
            return;
        }

        long id;
        if (hasId) {
            // PDC 有编号但缺容量标记：沿用原编号，只补标记
            id = pdc.get(CARD_ID_KEY, PersistentDataType.LONG);
        } else {
            // 兜底：卡片经创造模式客户端回传等链路可能丢失整个 PDC，
            // 优先从自定义名"#编号"后缀恢复，保证同一张卡的编号永远不变；只有新模板卡才发新号
            Long recovered = parseIdFromName(meta);
            if (recovered != null) {
                id = recovered;
                io.github.lyen.LogiTech.MyAddon.getInstance().getLogger().warning(
                        "检测到存储卡 PDC 编号丢失，已从名称恢复编号 #" + id + "（材质 " + stack.getType() + "）");
            } else {
                id = ID_ISSUER.nextId();
            }
            pdc.set(CARD_ID_KEY, PersistentDataType.LONG, id);
        }
        if (!hasAmountKey) {
            pdc.set(io.github.lyen.LogiTech.Core.Register.RegisterKeys.CARD_AMOUNT_KEY,
                    PersistentDataType.INTEGER, 0);
        }

        // 名称：已有自定义名时先剥掉历史"#编号"后缀再拼接，避免反复追加后缀；
        // 没有自定义名（新模板卡）时从 item_name 数据组件（翻译键）渲染模板名
        String plain;
        if (meta.hasDisplayName() && meta.displayName() != null) {
            plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(meta.displayName())
                    .replaceAll("\\s*#\\d+\\s*$", "").trim();
            if (plain.isEmpty()) {
                plain = fallbackName(stack.getType());
            }
        } else {
            net.kyori.adventure.text.Component currentName =
                    stack.getData(io.papermc.paper.datacomponent.DataComponentTypes.ITEM_NAME);
            if (currentName == null) {
                currentName = net.kyori.adventure.text.Component.translatable(stack.getType());
            }
            plain = null;
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
        }
        meta.displayName(net.kyori.adventure.text.Component.text("§6" + plain + " §8#" + id)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        stack.setItemMeta(meta);
    }

    /**
     * 从卡片自定义名末尾的"#编号"后缀解析编号，解析不到返回 null
     */
    private static @org.jetbrains.annotations.Nullable Long parseIdFromName(@NotNull ItemMeta meta) {
        if (!meta.hasDisplayName() || meta.displayName() == null) {
            return null;
        }
        String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(meta.displayName());
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("#(\\d+)\\s*$").matcher(plain);
        return matcher.find() ? Long.parseLong(matcher.group(1)) : null;
    }

    private static String fallbackName(@NotNull org.bukkit.Material type) {
        return switch (type) {
            case MUSIC_DISC_CAT -> "1K存储卡";
            case MUSIC_DISC_BLOCKS -> "4K存储卡";
            case MUSIC_DISC_FAR -> "16K存储卡";
            case MUSIC_DISC_CHIRP -> "64K存储卡";
            case MUSIC_DISC_WAIT -> "256K存储卡";
            case MUSIC_DISC_STRAD -> "1M存储卡";
            case MUSIC_DISC_MELLOHI -> "4M存储卡";
            case MUSIC_DISC_MALL -> "16M存储卡";
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
