package io.github.lyen.LogiTech.Core.Basic.Network;

import io.github.lyen.LogiTech.Core.Basic.Storage.MemoryBlock;
import io.github.lyen.LogiTech.Core.Basic.Storage.StorageBlock;
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

    /**
     * 同网络中同类流体设备里本块是否为坐标最小的"主设备"。
     * 多个同类流体设备共享同一容量卡流体池时，Rebar 按各设备上报量结算；
     * 若每台都独立上报全池余量，多台会重复上报，可能凭空产生/丢失流体。
     * 因此只让主设备参与 Rebar 的供液/请求结算。
     */
    protected boolean isPrimaryFluidDevice(@NotNull Class<?> type) {
        Block self = getBlock();
        for (NetworkNode node : getNetwork().getNodes()) {
            if (type.isInstance(node) && isPosBefore(node.getBlock(), self)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPosBefore(@NotNull Block a, @NotNull Block b) {
        if (a.getX() != b.getX()) {
            return a.getX() < b.getX();
        }
        if (a.getY() != b.getY()) {
            return a.getY() < b.getY();
        }
        return a.getZ() < b.getZ();
    }
}
