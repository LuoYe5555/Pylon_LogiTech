package io.github.lyen.LogiTech.core.basic.Network;

import io.github.lyen.LogiTech.core.Storage.MemoryBlock;
import io.github.lyen.LogiTech.core.Storage.StorageBlock;
import io.github.pylonmc.rebar.block.RebarBlock;
import io.github.pylonmc.rebar.block.context.BlockCreateContext;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;

/**
 * 网络方块基类。
 * 类似粘液科技附属 Network：所有网络设备通过"相邻放置"连成一个网络，
 * 网桥用来延长网络跨度，存储方块负责真正存放物品。
 */
public abstract class NetworkNode extends RebarBlock {

    public NetworkNode(@NotNull Block block, @NotNull BlockCreateContext context) {
        super(block, context);
    }

    public NetworkNode(@NotNull Block block, @NotNull PersistentDataContainer pdc) {
        super(block, pdc);
    }

    /**
     * 该 Rebar 方块是否属于网络成员（网络设备或网络存储）。
     * 存储器（容量卡）是网络成员；量子存储不是，只能通过监视器接入。
     */
    public static boolean isNetworkMember(RebarBlock block) {
        return block instanceof NetworkNode
                || block instanceof StorageBlock
                || block instanceof MemoryBlock;
    }

    /**
     * 以此方块为起点搜索它所在的网络
     */
    public @NotNull NetworkManager.Network getNetwork() {
        return NetworkManager.findNetwork(this);
    }
}
