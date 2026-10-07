package io.github.lyen.LogiTech.Core.Basic.Listeners;

import io.github.pylonmc.rebar.item.RebarItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 修复 Rebar 0.44 创造模式搬运物品时 addon 页脚（"逻辑工艺"）在 lore 中无限追加的问题。
 *
 * Rebar 的 CreativeActionTranslationHandler 在 InventoryCreativeEvent(HIGHEST) 对客户端回传的
 * 物品调用 resetItem：把已经翻译过（含页脚）的 lore 重新存为"原始 lore"并移除 footer_appended
 * 标记，下一次发包翻译时 PlayerTranslationHandler 发现没有标记就会再追加一行页脚，
 * 玩家每次在创造模式背包/GUI 中移动物品，页脚就多一行。
 *
 * 这里在 MONITOR（晚于 Rebar 的 HIGHEST）处理：只针对本插件的 Rebar 物品，
 * 把末尾连续重复的页脚行收敛为一行，并补回 footer_appended 标记，使页脚稳定只显示一次。
 */
public class RebarFooterFixListener implements Listener {

    /** Rebar PlayerTranslationHandler 的页脚已追加标记（rebar:footer_appended） */
    private static final NamespacedKey FOOTER_APPENDED_KEY = new NamespacedKey("rebar", "footer_appended");

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreativeAction(@NotNull InventoryCreativeEvent event) {
        ItemStack stack = event.getCursor();
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) {
            return;
        }
        RebarItem rebarItem = RebarItem.fromStack(stack);
        if (rebarItem == null || rebarItem.getAddon() == null
                || !"logitech".equals(rebarItem.getAddon().getKey().getNamespace())) {
            return;
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta == null || !meta.hasLore() || meta.lore() == null || meta.lore().isEmpty()) {
            return;
        }
        List<Component> lore = new ArrayList<>(meta.lore());

        Locale locale = event.getWhoClicked() instanceof Player player ? player.locale() : Locale.CHINA;
        Component footer = GlobalTranslator.render(rebarItem.getAddon().getFooterName(), locale);
        String footerPlain = PlainTextComponentSerializer.plainText().serialize(footer);

        // 统计并移除末尾连续的页脚行（客户端回传的物品可能已累积多行）
        int footerCount = 0;
        while (!lore.isEmpty()) {
            String lastPlain = PlainTextComponentSerializer.plainText().serialize(lore.get(lore.size() - 1));
            if (!lastPlain.equals(footerPlain)) {
                break;
            }
            lore.remove(lore.size() - 1);
            footerCount++;
        }
        // 没有页脚行说明是干净物品（尚未被回传污染），保持原样不干预
        if (footerCount == 0) {
            return;
        }

        // 收敛为一行页脚并补回标记，阻止后续发包继续追加
        lore.add(footer);
        meta.lore(lore);
        meta.getPersistentDataContainer().set(FOOTER_APPENDED_KEY, PersistentDataType.BOOLEAN, true);
        stack.setItemMeta(meta);
    }
}
