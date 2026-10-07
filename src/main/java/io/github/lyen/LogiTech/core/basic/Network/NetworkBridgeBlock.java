package io.github.lyen.LogiTech.Core.Basic.Network;

import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import io.github.pylonmc.rebar.item.RebarItem;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.bukkit.persistence.PersistentDataContainer;

/**
 * 网桥：纯连接方块（类似 Network 附属里的网桥/线缆），
 * 用来把相距较远的网络设备连接到同一张网络。
 */
public class NetworkBridgeBlock extends NetworkNode {

    public static class Item extends RebarItem {
        public Item(@NotNull ItemStack stack) {
            super(stack);
        }
    }

    public NetworkBridgeBlock(@NotNull Block block, @NotNull BlockCreateContext context) {
        super(block, context);
    }

    public NetworkBridgeBlock(@NotNull Block block, @NotNull PersistentDataContainer pdc) {
        super(block, pdc);
    }
}
