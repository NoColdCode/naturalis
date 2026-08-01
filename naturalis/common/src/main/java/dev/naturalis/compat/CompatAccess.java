package dev.naturalis.compat;

import dev.naturalis.NaturalisMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.BiFunction;

public final class CompatAccess {

    private CompatAccess() {
    }

    /**
     * Loader-neutral access to mod persistent data on entities (NeoForge/Forge patch {@code getPersistentData()};
     * Fabric supplies {@link NaturalisPersistentDataHolder} via mixin).
     */
    public static CompoundTag getPersistentData(Entity entity) {
        if (entity instanceof NaturalisPersistentDataHolder holder) {
            return holder.naturalis$getPersistentData();
        }
        try {
            return (CompoundTag) entity.getClass().getMethod("getPersistentData").invoke(entity);
        } catch (ReflectiveOperationException e) {
            return new CompoundTag();
        }
    }

    public static GameRules.Key<GameRules.BooleanValue> registerBooleanGameRule(String id, GameRules.Category category, boolean defaultValue) {
        return GameRules.register(id, category, GameRules.BooleanValue.create(defaultValue));
    }

    /**
     * NeoForge/Forge may expose {@code openMenu(MenuProvider, BlockPos)}; Fabric routes through {@link dev.naturalis.fabric.FabricMenuHooks}.
     */
    public static void openMenuAtBlock(ServerPlayer player, MenuProvider menuProvider, BlockPos pos) {
        try {
            player.getClass().getMethod("openMenu", MenuProvider.class, BlockPos.class).invoke(player, menuProvider, pos);
            return;
        } catch (NoSuchMethodException ignored) {
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }

        try {
            Class<?> hooks = Class.forName("dev.naturalis.fabric.FabricMenuHooks");
            hooks.getMethod("openMenuAt", ServerPlayer.class, MenuProvider.class, BlockPos.class).invoke(null, player, menuProvider, pos);
        } catch (ClassNotFoundException e) {
            player.openMenu(menuProvider);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    public static CompoundTag getCompound(CompoundTag tag, String key) {
        Object raw = tag.getCompound(key);
        if (raw instanceof CompoundTag ct) {
            return ct;
        }
        Object value = unwrapOptional(raw);
        if (value instanceof CompoundTag ct) {
            return ct;
        }
        return new CompoundTag();
    }

    public static String getString(CompoundTag tag, String key) {
        Object raw = tag.getString(key);
        if (raw instanceof String s) {
            return s;
        }
        Object value = unwrapOptional(raw);
        if (value instanceof String s) {
            return s;
        }
        return "";
    }

    public static int getInt(CompoundTag tag, String key) {
        Object raw = tag.getInt(key);
        if (raw instanceof Integer i) {
            return i;
        }
        if (raw instanceof OptionalInt oi) {
            return oi.orElse(0);
        }
        Object value = unwrapOptional(raw);
        if (value instanceof Integer i) {
            return i;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        // 1.21.x OptionalInt may not be an Optional<?>
        if (raw != null) {
            try {
                Object asInt = raw.getClass().getMethod("orElse", int.class).invoke(raw, 0);
                if (asInt instanceof Integer i) {
                    return i;
                }
            } catch (ReflectiveOperationException ignored) {
            }
            try {
                Object present = raw.getClass().getMethod("isPresent").invoke(raw);
                if (present instanceof Boolean b && b) {
                    Object v = raw.getClass().getMethod("getAsInt").invoke(raw);
                    if (v instanceof Integer i) {
                        return i;
                    }
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return 0;
    }

    public static long getLong(CompoundTag tag, String key) {
        Object raw = tag.getLong(key);
        if (raw instanceof Long l) {
            return l;
        }
        Object value = unwrapOptional(raw);
        if (value instanceof Long l) {
            return l;
        }
        return 0L;
    }

    public static double getDouble(CompoundTag tag, String key) {
        Object raw = tag.getDouble(key);
        if (raw instanceof Double d) {
            return d;
        }
        Object value = unwrapOptional(raw);
        if (value instanceof Double d) {
            return d;
        }
        return 0.0D;
    }

    public static float getFloat(CompoundTag tag, String key) {
        Object raw = tag.getFloat(key);
        if (raw instanceof Float f) {
            return f;
        }
        Object value = unwrapOptional(raw);
        if (value instanceof Float f) {
            return f;
        }
        return 0.0F;
    }

    public static boolean getBoolean(CompoundTag tag, String key) {
        if (tag == null || key == null) {
            return false;
        }
        Object raw = tag.getBoolean(key);
        if (raw instanceof Boolean b) {
            return b;
        }
        Object value = unwrapOptional(raw);
        return value instanceof Boolean b && b;
    }

    public static short getShort(CompoundTag tag, String key) {
        Object raw = tag.getShort(key);
        if (raw instanceof Short s) {
            return s;
        }
        Object value = unwrapOptional(raw);
        if (value instanceof Short s) {
            return s;
        }
        return 0;
    }

    public static byte getByte(CompoundTag tag, String key) {
        Object raw = tag.getByte(key);
        if (raw instanceof Byte b) {
            return b;
        }
        Object value = unwrapOptional(raw);
        if (value instanceof Byte b) {
            return b;
        }
        return 0;
    }

    private static Object unwrapOptional(Object raw) {
        if (raw instanceof Optional<?> optional) {
            return optional.orElse(null);
        }
        return raw;
    }

    public static boolean contains(CompoundTag tag, String key) {
        try {
            return tag.contains(key);
        } catch (Throwable ignored) {
            return !getString(tag, key).isEmpty() || getInt(tag, key) != 0 || getLong(tag, key) != 0L;
        }
    }

    public static CompoundTag serializeItemStacks(NonNullList<ItemStack> items, HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        try {
            for (java.lang.reflect.Method method : ContainerHelper.class.getMethods()) {
                if (!"saveAllItems".equals(method.getName()) || method.getParameterCount() != 3) {
                    continue;
                }
                Class<?>[] params = method.getParameterTypes();
                if (params[0] == CompoundTag.class) {
                    method.invoke(null, tag, items, registries);
                    return tag;
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }
        return tag;
    }

    public static void loadItemStacks(NonNullList<ItemStack> items, HolderLookup.Provider registries, CompoundTag tag) {
        if (tag == null || items == null) {
            return;
        }
        try {
            for (java.lang.reflect.Method method : ContainerHelper.class.getMethods()) {
                if (!"loadAllItems".equals(method.getName())) {
                    continue;
                }
                Class<?>[] params = method.getParameterTypes();
                if (method.getParameterCount() == 3 && params[0] == CompoundTag.class) {
                    method.invoke(null, tag, items, registries);
                    return;
                }
                if (method.getParameterCount() == 2 && params[0] == CompoundTag.class) {
                    method.invoke(null, tag, items);
                    return;
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // No-op fallback.
        }
    }

    /**
     * Forge {@code ItemStackHandler} / NeoForge item handlers expose {@code serializeNBT} / {@code deserializeNBT}.
     * Optional registry argument is ignored here so callers stay loader-neutral.
     */
    public static CompoundTag serializeItemHandler(Object handler, Object ignoredRegistry) {
        if (handler == null) {
            return new CompoundTag();
        }
        try {
            Object raw = handler.getClass().getMethod("serializeNBT").invoke(handler);
            if (raw instanceof CompoundTag compoundTag) {
                return compoundTag;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }
        return new CompoundTag();
    }

    public static void deserializeItemHandler(Object handler, Object ignoredRegistry, CompoundTag tag) {
        if (handler == null || tag == null) {
            return;
        }
        try {
            handler.getClass().getMethod("deserializeNBT", CompoundTag.class).invoke(handler, tag);
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }
    }

    public static ResourceLocation naturalisRl(String path) {
        return ResourceLocation.fromNamespaceAndPath(NaturalisMod.ID, path);
    }

    public static Item naturalisItem(String path) {
        Item item = unwrapRegistryValue(BuiltInRegistries.ITEM.get(naturalisRl(path)), Item.class);
        return item != null ? item : Items.AIR;
    }

    public static Block naturalisBlock(String path) {
        Block block = getBlock(naturalisRl(path));
        return block != null ? block : Blocks.AIR;
    }

    @SuppressWarnings("unchecked")
    public static <T extends BlockEntity> BlockEntityType<T> naturalisBlockEntityType(String path) {
        BlockEntityType<?> type = unwrapRegistryValue(
            BuiltInRegistries.BLOCK_ENTITY_TYPE.get(naturalisRl(path)),
            BlockEntityType.class);
        return (BlockEntityType<T>) type;
    }

    public static MenuType<?> naturalisMenuType(String path) {
        return unwrapRegistryValue(BuiltInRegistries.MENU.get(naturalisRl(path)), MenuType.class);
    }

    @SuppressWarnings("unchecked")
    public static Holder<MobEffect> naturalisMobEffectHolder(String path) {
        ResourceKey<MobEffect> key = ResourceKey.create(Registries.MOB_EFFECT, naturalisRl(path));
        try {
            java.lang.reflect.Method m = BuiltInRegistries.MOB_EFFECT.getClass().getMethod("getHolder", ResourceKey.class);
            Object raw = m.invoke(BuiltInRegistries.MOB_EFFECT, key);
            if (raw instanceof Optional<?> optional && optional.orElse(null) instanceof Holder<?> holder) {
                return (Holder<MobEffect>) holder;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }

        MobEffect effect = unwrapRegistryValue(BuiltInRegistries.MOB_EFFECT.get(naturalisRl(path)), MobEffect.class);
        if (effect == null) {
            return MobEffects.WEAKNESS;
        }

        try {
            java.lang.reflect.Method wrap = BuiltInRegistries.MOB_EFFECT.getClass().getMethod("wrapAsHolder", MobEffect.class);
            Object holder = wrap.invoke(BuiltInRegistries.MOB_EFFECT, effect);
            if (holder instanceof Holder<?> h) {
                return (Holder<MobEffect>) h;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }

        return MobEffects.WEAKNESS;
    }

    @SuppressWarnings("unchecked")
    private static <T> T unwrapRegistryValue(Object raw, Class<T> type) {
        if (type.isInstance(raw)) {
            return type.cast(raw);
        }
        if (raw instanceof Optional<?> optional) {
            Object value = optional.orElse(null);
            if (type.isInstance(value)) {
                return type.cast(value);
            }
            if (value != null) {
                try {
                    Object unwrapped = value.getClass().getMethod("value").invoke(value);
                    if (type.isInstance(unwrapped)) {
                        return type.cast(unwrapped);
                    }
                } catch (ReflectiveOperationException ignored) {
                    // Fall through.
                }
            }
        }
        return null;
    }

    public static Entity createEntity(EntityType<?> type, ServerLevel level) {
        if (type == null || level == null) {
            return null;
        }

        try {
            Class<?> spawnReasonClass = Class.forName("net.minecraft.world.entity.EntitySpawnReason");
            Object commandReason = Enum.valueOf((Class<? extends Enum>) spawnReasonClass.asSubclass(Enum.class), "COMMAND");
            Entity created = (Entity) type.getClass()
                .getMethod("create", net.minecraft.world.level.Level.class, spawnReasonClass)
                .invoke(type, level, commandReason);
            if (created != null) {
                return created;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }

        try {
            Entity created = (Entity) type.getClass().getMethod("create", ServerLevel.class).invoke(type, level);
            if (created != null) {
                return created;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }

        try {
            Class<?> mobSpawnTypeClass = Class.forName("net.minecraft.world.entity.MobSpawnType");
            Object commandSpawn = Enum.valueOf((Class<? extends Enum>) mobSpawnTypeClass.asSubclass(Enum.class), "COMMAND");
            return (Entity) type.getClass()
                .getMethod(
                    "create",
                    ServerLevel.class,
                    java.util.function.Consumer.class,
                    net.minecraft.core.BlockPos.class,
                    mobSpawnTypeClass,
                    boolean.class,
                    boolean.class
                )
                .invoke(type, level, null, net.minecraft.core.BlockPos.ZERO, commandSpawn, false, false);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    public static boolean isInWaterOrBubble(LivingEntity entity) {
        if (entity == null) {
            return false;
        }

        try {
            Object raw = entity.getClass().getMethod("isInWaterOrBubble").invoke(entity);
            if (raw instanceof Boolean b) {
                return b;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }

        try {
            return entity.isInWater();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void spawnEntityItemDrop(LivingEntity entity, ServerLevel level, ItemStack stack) {
        if (entity == null || stack == null || stack.isEmpty()) {
            return;
        }

        try {
            entity.getClass()
                .getMethod("spawnAtLocation", ServerLevel.class, ItemStack.class, float.class)
                .invoke(entity, level, stack, 0.0F);
            return;
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }

        try {
            entity.getClass()
                .getMethod("spawnAtLocation", ItemStack.class, float.class)
                .invoke(entity, stack, 0.0F);
            return;
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }

        try {
            entity.getClass().getMethod("spawnAtLocation", ItemStack.class).invoke(entity, stack);
        } catch (ReflectiveOperationException ignored) {
            // No-op fallback.
        }
    }

    public static void moveEntity(LivingEntity entity, double x, double y, double z, float yRot, float xRot) {
        if (entity == null) {
            return;
        }

        try {
            entity.getClass()
                .getMethod("moveTo", double.class, double.class, double.class, float.class, float.class)
                .invoke(entity, x, y, z, yRot, xRot);
            return;
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }

        entity.setPos(x, y, z);
        entity.setYRot(yRot);
        entity.setXRot(xRot);
    }

    @SuppressWarnings("unchecked")
    public static EntityType<?> getEntityType(ResourceLocation id) {
        if (id == null) {
            return null;
        }

        EntityType<?> type = unwrapEntityType(BuiltInRegistries.ENTITY_TYPE.get(id));
        if (type != null) {
            return type;
        }

        type = lookupEntityTypeByString(id.toString());
        if (type != null) {
            return type;
        }

        if (!"minecraft".equals(id.getNamespace())) {
            ResourceLocation vanilla = ResourceLocation.fromNamespaceAndPath("minecraft", id.getPath());
            type = unwrapEntityType(BuiltInRegistries.ENTITY_TYPE.get(vanilla));
            if (type != null) {
                return type;
            }
            type = lookupEntityTypeByString(vanilla.toString());
            if (type != null) {
                return type;
            }
        }

        for (ResourceLocation key : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            if (key.getPath().equals(id.getPath())) {
                type = unwrapEntityType(BuiltInRegistries.ENTITY_TYPE.get(key));
                if (type != null) {
                    return type;
                }
            }
        }

        return null;
    }

    private static EntityType<?> unwrapEntityType(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof EntityType<?> type) {
            return type;
        }
        if (raw instanceof Optional<?> optional) {
            return unwrapEntityType(optional.orElse(null));
        }
        try {
            return unwrapEntityType(raw.getClass().getMethod("value").invoke(raw));
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static EntityType<?> lookupEntityTypeByString(String id) {
        try {
            Object raw = EntityType.class.getMethod("byString", String.class).invoke(null, id);
            if (raw instanceof Optional<?> optional) {
                return unwrapEntityType(optional.orElse(null));
            }
            return unwrapEntityType(raw);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    public static Block getBlock(ResourceLocation id) {
        Object raw = BuiltInRegistries.BLOCK.get(id);
        if (raw instanceof Block block) {
            return block;
        }
        if (raw instanceof Optional<?> optional) {
            Object value = optional.orElse(null);
            if (value == null) {
                return null;
            }
            try {
                Object unwrapped = value.getClass().getMethod("value").invoke(value);
                if (unwrapped instanceof Block block) {
                    return block;
                }
            } catch (ReflectiveOperationException ignored) {
                if (value instanceof Block block) {
                    return block;
                }
            }
        }
        return null;
    }

    public static int getMinBuildHeight(net.minecraft.world.level.Level level) {
        try {
            Object raw = level.getClass().getMethod("getMinBuildHeight").invoke(level);
            if (raw instanceof Integer i) {
                return i;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }

        try {
            Object raw = level.getClass().getMethod("getMinY").invoke(level);
            if (raw instanceof Integer i) {
                return i;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }

        return -64;
    }

    public static int getMaxBuildHeight(net.minecraft.world.level.Level level) {
        try {
            Object raw = level.getClass().getMethod("getMaxBuildHeight").invoke(level);
            if (raw instanceof Integer i) {
                return i;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }

        try {
            Object raw = level.getClass().getMethod("getMaxY").invoke(level);
            if (raw instanceof Integer i) {
                return i + 1;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }

        try {
            Object raw = level.getClass().getMethod("getHeight").invoke(level);
            if (raw instanceof Integer i) {
                return i;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }

        return 384;
    }

    public static boolean getGameRuleBoolean(Level level, GameRules.Key<GameRules.BooleanValue> key, boolean defaultValue) {
        if (level == null || key == null) {
            return defaultValue;
        }

        try {
            Object rules = level.getClass().getMethod("getGameRules").invoke(level);
            Object raw = rules.getClass().getMethod("getBoolean", GameRules.Key.class).invoke(rules, key);
            if (raw instanceof Boolean b) {
                return b;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }

        if (level instanceof ServerLevel serverLevel) {
            MinecraftServer server = serverLevel.getServer();
            if (server != null) {
                try {
                    Object rules = server.getClass().getMethod("getGameRules").invoke(server);
                    Object raw = rules.getClass().getMethod("getBoolean", GameRules.Key.class).invoke(rules, key);
                    if (raw instanceof Boolean b) {
                        return b;
                    }
                } catch (ReflectiveOperationException ignored) {
                    // Fall through.
                }
            }
        }

        return defaultValue;
    }

    /**
     * Mojang mappings hide {@code BlockEntityType.BlockEntitySupplier} as package-private; use a {@link BiFunction} factory instead.
     */
    @SuppressWarnings("unchecked")
    public static <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(BiFunction<BlockPos, BlockState, T> factory, Block... blocks) {
        if (factory == null || blocks == null || blocks.length == 0) {
            throw new IllegalArgumentException("factory and at least one block required");
        }

        Class<?> supplierIface;
        try {
            supplierIface = Class.forName("net.minecraft.world.level.block.entity.BlockEntityType$BlockEntitySupplier");
        } catch (ClassNotFoundException e) {
            supplierIface = null;
        }

        Object supplierProxy = supplierIface == null ? null : java.lang.reflect.Proxy.newProxyInstance(
            supplierIface.getClassLoader(),
            new Class<?>[]{supplierIface},
            (proxy, method, args) -> {
                if (args != null && args.length >= 2 && args[0] instanceof BlockPos pos && args[1] instanceof BlockState state) {
                    return factory.apply(pos, state);
                }
                return null;
            });

        try {
            Class<?> builderClass = Class.forName("net.minecraft.world.level.block.entity.BlockEntityType$Builder");
            java.lang.reflect.Method ofMethod = null;
            if (supplierIface != null) {
                for (java.lang.reflect.Method m : builderClass.getMethods()) {
                    if ("of".equals(m.getName()) && m.getParameterCount() == 2) {
                        Class<?>[] pt = m.getParameterTypes();
                        if (supplierIface.isAssignableFrom(pt[0]) && pt[1].isArray() && pt[1].getComponentType() == Block.class) {
                            ofMethod = m;
                            break;
                        }
                    }
                }
            }

            if (ofMethod != null && supplierProxy != null) {
                Object builder = ofMethod.invoke(null, supplierProxy, blocks);
                for (java.lang.reflect.Method method : builderClass.getMethods()) {
                    if ("build".equals(method.getName()) && method.getParameterCount() == 1) {
                        Object built = method.invoke(builder, new Object[]{null});
                        if (built instanceof BlockEntityType<?> type) {
                            return (BlockEntityType<T>) type;
                        }
                    }
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }

        try {
            if (supplierIface != null && supplierProxy != null) {
                for (Constructor<?> constructor : BlockEntityType.class.getDeclaredConstructors()) {
                    Class<?>[] params = constructor.getParameterTypes();
                    if (params.length >= 2 && supplierIface.isAssignableFrom(params[0])) {
                        try {
                            constructor.setAccessible(true);
                            Object[] args = new Object[params.length];
                            args[0] = supplierProxy;
                            if (params[1].isArray() && Block.class.equals(params[1].getComponentType())) {
                                args[1] = blocks;
                            } else if (java.util.Set.class.isAssignableFrom(params[1])) {
                                args[1] = new HashSet<>(Arrays.asList(blocks));
                            } else if (Number.class.isAssignableFrom(params[1]) || params[1].isPrimitive()) {
                                args[1] = 0;
                            } else {
                                args[1] = null;
                            }
                            for (int i = 2; i < params.length; i++) {
                                if (params[i].isPrimitive()) {
                                    if (params[i] == boolean.class) {
                                        args[i] = false;
                                    } else if (params[i] == int.class || params[i] == short.class || params[i] == byte.class) {
                                        args[i] = 0;
                                    } else if (params[i] == long.class) {
                                        args[i] = 0L;
                                    } else if (params[i] == float.class) {
                                        args[i] = 0.0F;
                                    } else if (params[i] == double.class) {
                                        args[i] = 0.0D;
                                    } else {
                                        args[i] = null;
                                    }
                                } else {
                                    args[i] = null;
                                }
                            }
                            Object created = constructor.newInstance(args);
                            if (created instanceof BlockEntityType<?> type) {
                                return (BlockEntityType<T>) type;
                            }
                        } catch (Throwable ignored) {
                            // Try next constructor variant.
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
            // Fall through.
        }

        throw new IllegalStateException("Unable to create BlockEntityType across mappings");
    }

    @SuppressWarnings("unchecked")
    public static Holder<MobEffect> resolveMobEffect(String primaryField, String fallbackField) {
        try {
            Object primary = MobEffects.class.getField(primaryField).get(null);
            if (primary instanceof Holder<?> holder) {
                return (Holder<MobEffect>) holder;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }

        try {
            Object fallback = MobEffects.class.getField(fallbackField).get(null);
            if (fallback instanceof Holder<?> holder) {
                return (Holder<MobEffect>) holder;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }

        return MobEffects.WEAKNESS;
    }

    public static SoundEvent resolveSoundEvent(String primaryField, String fallbackField) {
        SoundEvent primary = resolveSoundEventField(primaryField);
        if (primary != null) {
            return primary;
        }

        SoundEvent fallback = resolveSoundEventField(fallbackField);
        if (fallback != null) {
            return fallback;
        }

        return null;
    }

    private static SoundEvent resolveSoundEventField(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            return null;
        }

        try {
            Object raw = SoundEvents.class.getField(fieldName).get(null);
            return extractSoundEvent(raw);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static SoundEvent extractSoundEvent(Object raw) {
        if (raw instanceof SoundEvent se) {
            return se;
        }

        Object unwrapped = unwrapOptional(raw);
        if (unwrapped instanceof SoundEvent se) {
            return se;
        }

        if (raw != null) {
            try {
                Object value = raw.getClass().getMethod("value").invoke(raw);
                if (value instanceof SoundEvent se) {
                    return se;
                }
            } catch (ReflectiveOperationException ignored) {
                // Fall through.
            }
        }

        return null;
    }

    public static void addItemCooldown(net.minecraft.world.entity.player.Player player, ItemStack stack, int ticks) {
        if (player == null || stack == null || stack.isEmpty()) {
            return;
        }

        Object cooldowns = player.getCooldowns();

        try {
            cooldowns.getClass().getMethod("addCooldown", Item.class, int.class).invoke(cooldowns, stack.getItem(), ticks);
            return;
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }

        try {
            cooldowns.getClass().getMethod("addCooldown", ItemStack.class, int.class).invoke(cooldowns, stack, ticks);
        } catch (ReflectiveOperationException ignored) {
            // No-op fallback.
        }
    }

    public static boolean isItemOnCooldown(net.minecraft.world.entity.player.Player player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return false;
        }

        Object cooldowns = player.getCooldowns();

        try {
            Object raw = cooldowns.getClass().getMethod("isOnCooldown", Item.class).invoke(cooldowns, stack.getItem());
            if (raw instanceof Boolean b) {
                return b;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }

        try {
            Object raw = cooldowns.getClass().getMethod("isOnCooldown", ItemStack.class).invoke(cooldowns, stack);
            if (raw instanceof Boolean b) {
                return b;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }

        return false;
    }

    public static void teleportCrossDimension(ServerPlayer player, ServerLevel target, double x, double y, double z, float yaw, float pitch) {
        if (player == null || target == null) {
            return;
        }

        try {
            player.getClass()
                .getMethod("teleportTo", ServerLevel.class, double.class, double.class, double.class, java.util.Set.class, float.class, float.class, boolean.class)
                .invoke(player, target, x, y, z, java.util.Set.of(), yaw, pitch, true);
            return;
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }

        try {
            player.getClass()
                .getMethod("teleportTo", ServerLevel.class, double.class, double.class, double.class, java.util.Set.class, float.class, float.class)
                .invoke(player, target, x, y, z, java.util.Set.of(), yaw, pitch);
            return;
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }

        try {
            player.getClass()
                .getMethod("teleportTo", ServerLevel.class, double.class, double.class, double.class, float.class, float.class)
                .invoke(player, target, x, y, z, yaw, pitch);
        } catch (ReflectiveOperationException ignored) {
            // No-op fallback.
        }
    }

    public static MinecraftServer getServer(net.minecraft.server.level.ServerPlayer player) {
        if (player == null) {
            return null;
        }

        try {
            Object raw = player.getClass().getMethod("getServer").invoke(player);
            if (raw instanceof MinecraftServer server) {
                return server;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }

        try {
            java.lang.reflect.Field field = player.getClass().getDeclaredField("server");
            field.setAccessible(true);
            Object raw = field.get(player);
            if (raw instanceof MinecraftServer server) {
                return server;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through.
        }

        return null;
    }

    private static volatile java.lang.reflect.Field ABILITIES_FLYING_SPEED_FIELD;

    public static float getAbilitiesFlyingSpeed(Abilities abilities) {
        if (abilities == null) {
            return 0.05F;
        }
        try {
            if (ABILITIES_FLYING_SPEED_FIELD == null) {
                java.lang.reflect.Field f = Abilities.class.getDeclaredField("flyingSpeed");
                f.setAccessible(true);
                ABILITIES_FLYING_SPEED_FIELD = f;
            }
            return ABILITIES_FLYING_SPEED_FIELD.getFloat(abilities);
        } catch (ReflectiveOperationException e) {
            return 0.05F;
        }
    }

    public static void setAbilitiesFlyingSpeed(Abilities abilities, float speed) {
        if (abilities == null) {
            return;
        }
        try {
            if (ABILITIES_FLYING_SPEED_FIELD == null) {
                java.lang.reflect.Field f = Abilities.class.getDeclaredField("flyingSpeed");
                f.setAccessible(true);
                ABILITIES_FLYING_SPEED_FIELD = f;
            }
            ABILITIES_FLYING_SPEED_FIELD.setFloat(abilities, speed);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    /** Cross-version ServerPlayer respawn setter (signature drifts across 1.21.x). */
    public static void setRespawnPosition(
        ServerPlayer player,
        ResourceKey<Level> dimension,
        BlockPos pos,
        float yaw,
        boolean forced,
        boolean sendMessage
    ) {
        if (player == null || dimension == null || pos == null) {
            return;
        }
        try {
            for (java.lang.reflect.Method method : player.getClass().getMethods()) {
                if (!"setRespawnPosition".equals(method.getName())) {
                    continue;
                }
                Class<?>[] params = method.getParameterTypes();
                if (params.length == 5
                    && ResourceKey.class.isAssignableFrom(params[0])
                    && BlockPos.class.isAssignableFrom(params[1])) {
                    method.invoke(player, dimension, pos, yaw, forced, sendMessage);
                    return;
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
