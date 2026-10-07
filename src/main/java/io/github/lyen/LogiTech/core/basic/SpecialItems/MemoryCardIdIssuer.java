package io.github.lyen.LogiTech.Core.Basic.SpecialItems;

import io.github.lyen.LogiTech.MyAddon;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 容量卡编号发号器：编号持久化在插件数据文件夹（memory-card-ids.yml），
 * 跨重启持续递增，保证每张卡的全局唯一编号不重复。
 */
public final class MemoryCardIdIssuer {

    private static final String FILE_NAME = "memory-card-ids.yml";

    private final AtomicLong counter = new AtomicLong(0);

    public MemoryCardIdIssuer() {
        File file = file();
        if (file.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            counter.set(config.getLong("next-id", 1));
        }
    }

    /**
     * 取下一个唯一编号
     */
    public synchronized long nextId() {
        long id = counter.getAndIncrement();
        save();
        return id;
    }

    private File file() {
        return new File(MyAddon.getInstance().getDataFolder(), FILE_NAME);
    }

    private void save() {
        File file = file();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            return;
        }
        YamlConfiguration config = new YamlConfiguration();
        config.set("next-id", counter.get());
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
