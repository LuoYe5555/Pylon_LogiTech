package io.github.lyen.LogiTech.core.Register;

import io.github.lyen.LogiTech.core.Storage.MemoryBlock;
import io.github.lyen.LogiTech.core.Storage.SingleItemStorageBlock;
import io.github.lyen.LogiTech.core.Storage.StorageBlock;
import io.github.lyen.LogiTech.core.basic.Network.NetworkBridgeBlock;
import io.github.lyen.LogiTech.core.basic.Network.NetworkGridBlock;
import io.github.lyen.LogiTech.core.basic.Network.NetworkMonitorBlock;
import io.github.lyen.LogiTech.core.basic.Network.NetworkPullerBlock;
import io.github.lyen.LogiTech.core.basic.Network.NetworkPusherBlock;
import io.github.pylonmc.rebar.block.RebarBlock;
import org.bukkit.Material;


public final class RegisterBlocks {

    public static void initialize() {
        RebarBlock.register(RegisterKeys.STORAGE_BLOCK, Material.LIGHT_GRAY_STAINED_GLASS, StorageBlock.class);
        RebarBlock.register(RegisterKeys.QUANTUM_STORAGE, Material.RED_TERRACOTTA, SingleItemStorageBlock.class);
        RebarBlock.register(RegisterKeys.MEMORY_BLOCK, Material.RESPAWN_ANCHOR, MemoryBlock.class);

        RebarBlock.register(RegisterKeys.NETWORK_MONITOR, Material.GREEN_STAINED_GLASS, NetworkMonitorBlock.class);
        RebarBlock.register(RegisterKeys.NETWORK_BRIDGE, Material.WHITE_STAINED_GLASS, NetworkBridgeBlock.class);
        RebarBlock.register(RegisterKeys.NETWORK_PUSHER, Material.BROWN_STAINED_GLASS, NetworkPusherBlock.class);
        RebarBlock.register(RegisterKeys.NETWORK_PULLER, Material.MAGENTA_STAINED_GLASS, NetworkPullerBlock.class);
        RebarBlock.register(RegisterKeys.NETWORK_GRID, Material.NOTE_BLOCK, NetworkGridBlock.class);
    }
}