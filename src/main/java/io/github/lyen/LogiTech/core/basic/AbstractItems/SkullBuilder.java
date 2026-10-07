package io.github.lyen.LogiTech.Core.Basic.AbstractItems;

import io.github.pylonmc.rebar.item.builder.ItemStackBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 头颅构建工具类
 * 支持多种格式：URL、Hash、Value、minecraft-heads.com 链接
 */
public class SkullBuilder {

    private final ItemStackBuilder builder;
    private String base64Texture;

    // 正则表达式
    private static final Pattern TEXTURE_URL_PATTERN = Pattern.compile("textures\\.minecraft\\.net/texture/([a-fA-F0-9]+)");
    private static final Pattern MINECRAFT_HEADS_HASH_PATTERN = Pattern.compile("hash=([a-fA-F0-9]+)");
    private static final Pattern VALUE_PATTERN = Pattern.compile("^[a-fA-F0-9+/=]{20,}$");

    /**
     * 私有构造函数
     */
    private SkullBuilder(ItemStackBuilder builder) {
        this.builder = builder;
    }

    /**
     * 创建头颅构建器
     */
    public static SkullBuilder skull(NamespacedKey key) {
        ItemStackBuilder builder = ItemStackBuilder.rebar(Material.PLAYER_HEAD, key);
        return new SkullBuilder(builder);
    }

    /**
     * 设置头颅皮肤 - 支持多种格式
     */
    public SkullBuilder setSkin(String input) {
        if (input == null || input.isEmpty()) {
            return this;
        }

        String normalized = input.trim();

        // 1. 如果是 textures.minecraft.net URL
        if (normalized.contains("textures.minecraft.net/texture/")) {
            Matcher matcher = TEXTURE_URL_PATTERN.matcher(normalized);
            if (matcher.find()) {
                String hash = matcher.group(1);
                this.base64Texture = createTextureValue("https://textures.minecraft.net/texture/" + hash);
                return this;
            }
        }

        // 2. 如果是 minecraft-heads.com URL
        if (normalized.contains("minecraft-heads.com")) {
            Matcher matcher = MINECRAFT_HEADS_HASH_PATTERN.matcher(normalized);
            if (matcher.find()) {
                String hash = matcher.group(1);
                String textureUrl = "https://textures.minecraft.net/texture/" + hash;
                this.base64Texture = createTextureValue(textureUrl);
                return this;
            }
            System.out.println("[LogiTech] minecraft-heads.com 链接需要包含 hash 参数");
            return this;
        }

        // 3. 如果是直接的 hash (32-64字符的十六进制)
        if (normalized.matches("[a-fA-F0-9]{32,64}")) {
            String textureUrl = "https://textures.minecraft.net/texture/" + normalized;
            this.base64Texture = createTextureValue(textureUrl);
            return this;
        }

        // 4. 如果是 Base64 编码的 value
        if (VALUE_PATTERN.matcher(normalized).matches() && normalized.length() > 50) {
            try {
                new String(Base64.getDecoder().decode(normalized));
                this.base64Texture = normalized;
                return this;
            } catch (Exception e) {
                // 不是有效的 Base64
            }
        }

        // 5. 尝试作为 URL 处理
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            if (normalized.contains("/texture/")) {
                Matcher matcher = TEXTURE_URL_PATTERN.matcher(normalized);
                if (matcher.find()) {
                    String hash = matcher.group(1);
                    this.base64Texture = createTextureValue("https://textures.minecraft.net/texture/" + hash);
                    return this;
                }
            }
        }

        System.out.println("[LogiTech] 无法识别的头颅格式: " + normalized);
        return this;
    }

    /**
     * 设置头颅皮肤 - 使用 Value
     */
    public SkullBuilder setSkinValue(String value) {
        this.base64Texture = value;
        return this;
    }

    /**
     * 设置物品显示名称
     */
    public SkullBuilder name(String name) {
        builder.name(name);
        return this;
    }

    /**
     * 设置物品描述
     */
    public SkullBuilder lore(String... lore) {
        builder.lore(lore);
        return this;
    }

    /**
     * 构建物品栈
     */
    public ItemStack build() {
        ItemStack item = builder.build();

        if (base64Texture != null) {
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            if (meta != null) {
                applySkin(meta);
            }
        }

        return item;
    }

    /**
     * 应用头颅纹理
     */
    private void applySkin(SkullMeta meta) {
        try {
            // 尝试新版本 API
            trySetProfileNew(meta);
        } catch (Exception e1) {
            // 尝试旧版本 API
            try {
                trySetProfileOld(meta);
            } catch (Exception e2) {
                System.out.println("[LogiTech] 头颅纹理设置失败: " + e2.getMessage());
            }
        }
    }

    /**
     * 新版本 API (1.20.5+)
     */
    private void trySetProfileNew(SkullMeta meta) throws Exception {
        Class<?> craftMetaSkullClass = Class.forName("org.bukkit.craftbukkit.inventory.CraftMetaSkull");
        Field profileField = craftMetaSkullClass.getDeclaredField("profile");
        profileField.setAccessible(true);

        // 创建 GameProfile
        Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
        Constructor<?> gameProfileConstructor = gameProfileClass.getConstructor(UUID.class, String.class);
        Object gameProfile = gameProfileConstructor.newInstance(UUID.randomUUID(), "MHF_Skull");

        // 获取 properties 字段
        Field propertiesField = gameProfileClass.getDeclaredField("properties");
        propertiesField.setAccessible(true);
        Object properties = propertiesField.get(gameProfile);

        // 创建 Property
        Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
        Constructor<?> propertyConstructor = propertyClass.getConstructor(String.class, String.class);
        Object property = propertyConstructor.newInstance("textures", base64Texture);

        // 添加 property
        Method addPropertyMethod = properties.getClass().getMethod("add", propertyClass);
        addPropertyMethod.invoke(properties, property);

        profileField.set(meta, gameProfile);
    }

    /**
     * 旧版本 API (1.20.4 及以下)
     */
    private void trySetProfileOld(SkullMeta meta) throws Exception {
        Class<?> craftMetaSkullClass = Class.forName("org.bukkit.craftbukkit.inventory.CraftMetaSkull");
        Field profileField = craftMetaSkullClass.getDeclaredField("profile");
        profileField.setAccessible(true);

        Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
        Constructor<?> gameProfileConstructor = gameProfileClass.getConstructor(UUID.class, String.class);
        Object gameProfile = gameProfileConstructor.newInstance(UUID.randomUUID(), "MHF_Skull");

        // getProperties() 方法
        Method getPropertiesMethod = gameProfileClass.getMethod("getProperties");
        Object properties = getPropertiesMethod.invoke(gameProfile);

        Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
        Constructor<?> propertyConstructor = propertyClass.getConstructor(String.class, String.class);
        Object property = propertyConstructor.newInstance("textures", base64Texture);

        Method addPropertyMethod = properties.getClass().getMethod("add", propertyClass);
        addPropertyMethod.invoke(properties, property);

        profileField.set(meta, gameProfile);
    }

    /**
     * 创建纹理 Value
     */
    private String createTextureValue(String url) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes());
    }
}