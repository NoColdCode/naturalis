package dev.naturalis.client;

import dev.naturalis.NaturalisMod;
import dev.naturalis.config.NaturalisConfig;
import dev.naturalis.client.perception.MorphEmbodimentProfiles;
import dev.naturalis.client.perception.MorphIdentityDriftClient;
import dev.naturalis.client.perception.MorphMotionVisionClient;
import dev.naturalis.client.perception.MorphSniffClientState;
import dev.naturalis.environment.EnvironmentalSusceptibilityManager;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;

/**
 * Drives {@code ChromaticMode} (dichrome / monochrome / quadrochrome), {@code PhotoStress},
 * kaleidoscope / spectral, and motion streak uniforms on Naturalis morph post shaders each frame.
 */
public final class MorphVisionPaletteUniforms {

    /** Opponent-process dichromacy (existing palette projection). */
    public static final float CHROMATIC_DICHROME = 0.0F;
    /** Rod-heavy, near grayscale night vision. */
    public static final float CHROMATIC_MONO = 1.0F;
    /** UV-augmented separation similar to avian tetrachromacy. */
    public static final float CHROMATIC_QUAD = 2.0F;

    private static volatile Field livingStrafeField;
    private static volatile Field livingForwardField;
    private static volatile boolean livingStrafeLookupDone;
    private static volatile boolean livingForwardLookupDone;

    private MorphVisionPaletteUniforms() {
    }

    public static void tick(Minecraft mc, ResourceLocation morphId, ResourceLocation activeShaderId) {
        if (mc == null || mc.gameRenderer == null || morphId == null || activeShaderId == null) {
            return;
        }
        if (!NaturalisMod.ID.equals(activeShaderId.getNamespace())) {
            return;
        }

        Object chain = resolvePostChain(mc.gameRenderer);
        if (chain == null) {
            return;
        }

        float mode = chromaticModeForMorph(morphId);
        float stress = computePhotoStress(mc, morphId);
        float kaleidoStrength = kaleidoStrengthForMorph(morphId);
        float kaleidoFolds = kaleidoFoldCountForMorph(morphId);
        float spectral = spectralProfileForMorph(morphId);
        float motionTrail = motionTrailStrength(morphId);
        boolean scentVision = MorphSniffClientState.isScentVisionActive();
        boolean motionFov = MorphMotionVisionClient.shouldApplyMotionFov();
        float motionScale = MorphMotionVisionClient.motionFovMultiplier();

        if (scentVision) {
            motionTrail = 0.0F;
        } else if (!motionFov) {
            motionTrail *= 0.08F;
        } else {
            motionTrail *= 0.35F + motionScale * 0.65F;
        }

        if (isCaninePredator(morphId)) {
            float cap = (float) NaturalisConfig.clientVisionPhotoStressCap();
            stress = Math.min(Math.max(stress, canineAmbientPhotoStress(mc) * 0.42F), cap);
            spectral = Math.min(Math.max(spectral, 0.35F), 0.35F);
            if (!scentVision && motionFov) {
                motionTrail = Math.min(Math.max(motionTrail, 0.38F), 0.38F);
            }
        } else if (MorphEmbodimentProfiles.resolve(morphId).hasEmbodiment() && motionFov && !scentVision) {
            float drift = MorphIdentityDriftClient.embodimentBlend();
            stress = Math.max(stress, 0.32F + drift * 0.35F);
            motionTrail = Math.max(motionTrail, 0.22F + drift * 0.28F);
        }

        float drift = MorphIdentityDriftClient.embodimentBlend();
        if (motionFov && !scentVision) {
            stress *= 1.0F + drift * 0.28F;
            motionTrail *= 1.0F + drift * 0.22F;
        }
        spectral *= 1.0F + drift * 0.15F;

        final float photoStress = stress;
        final float spectralProfile = spectral;
        final float motionTrailStrength = motionTrail;
        float[] motion = scentVision ? new float[] {0F, 0F} : computeMotionComponents(mc, motionTrailStrength);

        if ("wolf_vision".equals(activeShaderId.getPath())) {
            return;
        }

        ResourceLocation postEffectId = MorphVisionPostEffects.resolveForSetPostEffect(activeShaderId);
        // Colored palettes are baked into naturalis:post/*_vision.fsh (same model as wolf_vision).
        if (!"vision_palette".equals(postEffectId.getPath())) {
            return;
        }
        MorphVisionPaletteDefaults.Palette palette = MorphVisionPaletteDefaults.forShader(activeShaderId, postEffectId);
        float strengthBoost = (float) (NaturalisConfig.clientVisionIntensityMultiplier() * (1.0F + drift * 0.12F));

        boolean uploaded = false;
        if (NaturalisConfig.clientVisionUploadPaletteUbo()) {
            uploaded = MorphPostEffectUniformWriter.upload(
                chain,
                palette,
                mode,
                photoStress,
                kaleidoStrength,
                kaleidoFolds,
                spectralProfile,
                motionTrailStrength,
                motion[0],
                motion[1],
                strengthBoost
            );
        }

        if (!uploaded) {
            pushLegacyPaletteUniforms(chain, palette, mode, photoStress, kaleidoStrength, kaleidoFolds,
                spectralProfile, motionTrailStrength, motion[0], motion[1], strengthBoost);
        }
    }

    private static void pushLegacyPaletteUniforms(
        Object postChain,
        MorphVisionPaletteDefaults.Palette palette,
        float chromaticMode,
        float photoStress,
        float kaleidoStrength,
        float kaleidoFoldCount,
        float spectralProfile,
        float motionTrail,
        float motionUx,
        float motionUz,
        float strengthBoost
    ) {
        forEachShaderInChain(postChain, shader -> {
            setUniform3f(shader, "AxisA", palette.axisA());
            setUniform3f(shader, "AxisB", palette.axisB());
            setUniform3f(shader, "AxisC", palette.axisC());
            setUniform3f(shader, "ColorA", palette.colorA());
            setUniform3f(shader, "ColorB", palette.colorB());
            setUniform3f(shader, "ColorC", palette.colorC());
            setUniform1f(shader, "Strength", palette.strength() * strengthBoost);
            setUniform1f(shader, "ShadowLift", palette.shadowLift());
            setUniform1f(shader, "LumaPreserve", palette.lumaPreserve());
            setUniform1f(shader, "ChromaticMode", chromaticMode);
            setUniform1f(shader, "PhotoStress", photoStress);
            setUniform1f(shader, "KaleidoStrength", kaleidoStrength);
            setUniform1f(shader, "KaleidoFoldCount", kaleidoFoldCount);
            setUniform1f(shader, "SpectralProfile", spectralProfile);
            setUniform1f(shader, "MotionTrail", motionTrail);
            setUniform1f(shader, "MotionUx", motionUx);
            setUniform1f(shader, "MotionUz", motionUz);
        });
    }

    private static float chromaticModeForMorph(ResourceLocation id) {
        var profileMode = dev.naturalis.profile.MobProfileRegistry.getChromaticMode(id);
        if (profileMode.isPresent()) {
            return switch (profileMode.get().toLowerCase()) {
                case "mono", "monochrome" -> CHROMATIC_MONO;
                case "quad", "compound" -> CHROMATIC_QUAD;
                default -> CHROMATIC_DICHROME;
            };
        }

        String ns = id.getNamespace();
        String path = id.getPath();

        if (isQuadChromat(ns, path)) {
            return CHROMATIC_QUAD;
        }
        if (isMonoChromat(ns, path)) {
            return CHROMATIC_MONO;
        }
        return CHROMATIC_DICHROME;
    }

    private static boolean isQuadChromat(String ns, String path) {
        if ("minecraft".equals(ns)) {
            return switch (path) {
                case "chicken", "parrot", "bee" -> true;
                default -> false;
            };
        }
        if ("alexsmobs".equals(ns)) {
            return matchesAny(path,
                "hummingbird", "jay", "crow", "roadrunner", "emu", "cassowary", "toucan",
                "shoebill", "cockatoo", "sunbird", "booby", "manakin", "skua", "tropicbird",
                "pigeon", "gull", "tanager"
            );
        }
        if (matchesAny(path, "hummingbird", "toucan", "roadrunner", "cockatoo", "cassowary")) {
            return true;
        }
        if (matchesAny(path, "bird", "finch", "sparrow", "raptor", "falcon", "eagle", "hawk", "owl", "vulture")) {
            return true;
        }
        return false;
    }

    private static boolean isMonoChromat(String ns, String path) {
        if ("minecraft".equals(ns)) {
            return switch (path) {
                case "bat", "silverfish", "phantom", "glow_squid" -> true;
                default -> false;
            };
        }
        if ("alexsmobs".equals(ns)) {
            return matchesAny(path,
                "blobfish", "cockroach", "fly", "cod", "fish", "stradpole", "ant",
                "dropbear", "centipede", "mosquito", "pupfish", "silverfish"
            );
        }
        if (matchesAny(path, "mole", "shrew", "rat", "mouse", "vole", "lemming", "hamster")) {
            return true;
        }
        return false;
    }

    private static float computePhotoStress(Minecraft mc, ResourceLocation morphId) {
        if (!EnvironmentalSusceptibilityManager.isSunlightSensitive(morphId)) {
            return 0.0F;
        }
        if (mc.level == null || mc.player == null) {
            return 0.0F;
        }

        var level = mc.level;
        var pos = mc.player.blockPosition();
        if (EnvironmentalSusceptibilityManager.isSunPhotophobiaBiomeExempt(level, morphId, pos)) {
            return 0.0F;
        }

        int sky = level.getBrightness(LightLayer.SKY, pos);
        float skyFactor = sky / 15.0F;

        long t = level.getDayTime() % 24000L;
        float dayGate = 1.0F;
        if (t < 1200L || t > 22800L) {
            dayGate = 0.18F;
        } else if (t < 6000L || t > 18000L) {
            dayGate = 0.55F;
        }

        float rainDamp = level.isRaining() ? 0.62F : 1.0F;
        float thunderDamp = level.isThundering() ? 0.78F : 1.0F;

        float stress = skyFactor * dayGate * rainDamp * thunderDamp;
        if (EnvironmentalSusceptibilityManager.isClearSunnyExposure(level, pos)) {
            stress = Math.max(stress, 0.96F);
            stress *= 1.28F;
        }

        return Mth.clamp(stress, 0.0F, 1.35F);
    }

    /** Kaleidoscope blend — arthropods / compound-eye morphs (not used for vanilla spider shader). */
    private static float kaleidoStrengthForMorph(ResourceLocation morphId) {
        var profile = dev.naturalis.profile.MobProfileRegistry.getKaleidoStrength(morphId);
        if (profile.isPresent()) {
            return profile.get().floatValue();
        }

        String ns = morphId.getNamespace();
        String path = morphId.getPath();

        if ("minecraft".equals(ns)) {
            return switch (path) {
                case "bee" -> 0.88F;
                case "silverfish", "endermite" -> 0.76F;
                case "guardian", "elder_guardian" -> 0.62F;
                default -> 0F;
            };
        }

        if ("alexsmobs".equals(ns)) {
            if (matchesAny(path, "dragonfly", "damselfly")) {
                return 0.90F;
            }
            if (matchesAny(path, "fly", "mosquito", "moth", "bee", "wasp", "hornet", "beetle")) {
                return 0.84F;
            }
            if (matchesAny(path, "ant", "termite", "flea", "tarantula", "scorpion")) {
                return 0.78F;
            }
            if (matchesAny(path, "spider", "centipede", "roach", "cockroach")) {
                return 0.80F;
            }
        }

        if (matchesAny(path, "dragonfly", "damselfly", "firefly", "locust", "cricket", "aphid")) {
            return 0.82F;
        }
        if (matchesAny(path, "fly", "mosquito", "moth", "bee", "wasp", "hornet", "beetle")) {
            return 0.82F;
        }
        if (matchesAny(path, "ant", "termite", "mite", "tick")) {
            return 0.74F;
        }

        return 0F;
    }

    /** Fold count (∝ facet bands); shader rounds to integer wedges. */
    private static float kaleidoFoldCountForMorph(ResourceLocation morphId) {
        var profile = dev.naturalis.profile.MobProfileRegistry.getKaleidoFolds(morphId);
        if (profile.isPresent()) {
            return profile.get().floatValue();
        }

        String ns = morphId.getNamespace();
        String path = morphId.getPath();

        if ("minecraft".equals(ns)) {
            return switch (path) {
                case "bee" -> 16F;
                case "silverfish" -> 11F;
                case "endermite" -> 9F;
                case "guardian" -> 21F;
                case "elder_guardian" -> 24F;
                default -> 0F;
            };
        }

        if ("alexsmobs".equals(ns)) {
            if (matchesAny(path, "dragonfly", "damselfly")) {
                return 20F;
            }
            if (matchesAny(path, "fly", "mosquito")) {
                return 13F;
            }
            if (matchesAny(path, "bee", "wasp", "hornet")) {
                return 15F;
            }
            if (matchesAny(path, "moth", "butterfly")) {
                return 14F;
            }
            if (matchesAny(path, "ant", "termite")) {
                return 10F;
            }
            if (matchesAny(path, "beetle", "scorpion", "tarantula")) {
                return 12F;
            }
        }

        if (matchesAny(path, "dragonfly")) {
            return 20F;
        }
        if (matchesAny(path, "firefly")) {
            return 13F;
        }
        if (matchesAny(path, "fly", "mosquito")) {
            return 13F;
        }
        if (matchesAny(path, "bee", "wasp", "hornet")) {
            return 15F;
        }
        if (matchesAny(path, "moth", "butterfly")) {
            return 14F;
        }
        if (matchesAny(path, "ant", "termite")) {
            return 10F;
        }

        return kaleidoStrengthForMorph(morphId) > 0.001F ? 12F : 0F;
    }

    /**
     * Spectral emphasis: [0, ~1.1] IR / thermal tint ladder; &gt;1.2 UV accent (often stacked with quad chromatic).
     */
    private static float spectralProfileForMorph(ResourceLocation morphId) {
        var profile = dev.naturalis.profile.MobProfileRegistry.getSpectralProfile(morphId);
        if (profile.isPresent()) {
            return profile.get().floatValue();
        }

        String ns = morphId.getNamespace();
        String path = morphId.getPath();

        if ("minecraft".equals(ns)) {
            if ("bee".equals(path)) {
                return 2.0F;
            }
            if ("bat".equals(path)) {
                return 0.92F;
            }
        }

        if (matchesAny(path, "bee", "butterfly", "moth", "dragonfly", "damselfly", "firefly", "wasp", "hornet")) {
            return path.contains("dragonfly") || path.contains("damselfly") ? 1.95F : 2.0F;
        }

        if (matchesAny(path, "pit_viper", "viper", "rattlesnake", "boa", "python", "cobra", "anaconda")) {
            return 1.05F;
        }
        if (matchesAny(path, "snake", "serpent", "lizard", "gecko", "iguana", "croc", "alligator", "gator")) {
            return 0.72F;
        }

        if (matchesAny(path, "mole", "shrew", "rat", "mouse", "vole", "hamster")) {
            return 0.62F;
        }

        if ("alexsmobs".equals(ns) && matchesAny(path, "rattlesnake", "anaconda", "python")) {
            return 1.02F;
        }

        if (isQuadChromat(ns, path)) {
            return 1.35F;
        }

        return 0F;
    }

    /** Temporal motion streak — predators and fast movers. */
    private static float motionTrailStrength(ResourceLocation morphId) {
        var profile = dev.naturalis.profile.MobProfileRegistry.getMotionTrail(morphId);
        if (profile.isPresent()) {
            return profile.get().floatValue();
        }

        String ns = morphId.getNamespace();
        String path = morphId.getPath();

        if ("minecraft".equals(ns)) {
            return switch (path) {
                case "wolf" -> 0.86F;
                case "fox" -> 0.72F;
                case "cat", "ocelot" -> 0.42F;
                case "horse", "donkey", "mule", "camel" -> 0.30F;
                case "cow", "pig", "sheep", "goat" -> 0.20F;
                case "rabbit" -> 0.34F;
                case "dolphin", "salmon", "cod", "tropical_fish", "pufferfish" -> 0.44F;
                case "phantom", "enderman" -> 0.48F;
                case "creeper" -> 0.24F;
                case "polar_bear", "panda", "llama", "trader_llama" -> 0.28F;
                default -> 0F;
            };
        }

        if ("alexsmobs".equals(ns)) {
            if (matchesAny(path, "cheetah", "lion", "tiger", "leopard", "panther", "serval", "caracal")) {
                return 0.56F;
            }
            if (matchesAny(path, "wolf", "fox", "dog", "coyote", "hyena", "jackal")) {
                return 0.48F;
            }
            if (matchesAny(path, "shark", "orca", "dolphin")) {
                return 0.50F;
            }
            if (matchesAny(path, "bear", "grizzly", "panda")) {
                return 0.32F;
            }
        }

        if (matchesAny(path, "cheetah", "lion", "tiger", "leopard", "panther", "serval", "lynx", "cougar", "puma")) {
            return 0.52F;
        }
        if (matchesAny(path, "wolf", "fox", "dog", "coyote", "hyena", "jackal", "dingo")) {
            return 0.80F;
        }
        if (matchesAny(path, "shark", "orca", "marlin", "tuna")) {
            return 0.48F;
        }

        return 0F;
    }

    private static float[] computeMotionComponents(Minecraft mc, float motionTrailStrength) {
        if (motionTrailStrength < 0.001F || mc.player == null) {
            return new float[]{0F, 0F};
        }

        Player pl = mc.player;
        Vec3 v = pl.getDeltaMovement();
        double dx = v.x;
        double dz = v.z;
        double hs = Math.sqrt(dx * dx + dz * dz);

        Vec3 look = pl.getLookAngle();
        Vec3 fwd = new Vec3(look.x, 0.0, look.z);
        if (fwd.lengthSqr() < 1e-8) {
            fwd = new Vec3(0.0, 0.0, -1.0);
        } else {
            fwd = fwd.normalize();
        }

        Vec3 right = new Vec3(-fwd.z, 0.0, fwd.x);
        if (right.lengthSqr() > 1e-8) {
            right = right.normalize();
        }

        double mx;
        double mz;
        float basis;

        if (hs >= 0.018) {
            Vec3 vel = new Vec3(dx, 0.0, dz);
            mx = vel.dot(right);
            mz = vel.dot(fwd);
            basis = Mth.clamp((float) (hs * 32.0), 0.12f, 1.0f);
        } else {
            float strafe = readLivingSidewaysImpulse(pl);
            float forward = readLivingForwardImpulse(pl);
            if (Math.abs(strafe) + Math.abs(forward) < 1e-3f) {
                return new float[]{0F, 0F};
            }
            Vec3 wish = right.scale(strafe).add(fwd.scale(forward));
            mx = wish.x;
            mz = wish.z;
            double wl = Math.sqrt(mx * mx + mz * mz);
            if (wl < 1e-6) {
                return new float[]{0F, 0F};
            }
            mx /= wl;
            mz /= wl;
            basis = 0.48f;
        }

        float u = Mth.clamp((float) (mx * basis), -1f, 1f);
        float w = Mth.clamp((float) (-mz * basis), -1f, 1f);
        return new float[]{u, w};
    }

    private static float readLivingSidewaysImpulse(LivingEntity entity) {
        Field f = resolveLivingStrafeField();
        if (f != null) {
            try {
                return f.getFloat(entity);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return 0F;
    }

    private static float readLivingForwardImpulse(LivingEntity entity) {
        Field f = resolveLivingForwardField();
        if (f != null) {
            try {
                return f.getFloat(entity);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return 0F;
    }

    private static Field resolveLivingStrafeField() {
        if (livingStrafeLookupDone) {
            return livingStrafeField;
        }
        synchronized (MorphVisionPaletteUniforms.class) {
            if (livingStrafeLookupDone) {
                return livingStrafeField;
            }
            livingStrafeField = findFloatField(LivingEntity.class, "xxa", "f_20891_", "field_70702_", "sidewaysImpulse");
            livingStrafeLookupDone = true;
            return livingStrafeField;
        }
    }

    private static Field resolveLivingForwardField() {
        if (livingForwardLookupDone) {
            return livingForwardField;
        }
        synchronized (MorphVisionPaletteUniforms.class) {
            if (livingForwardLookupDone) {
                return livingForwardField;
            }
            livingForwardField = findFloatField(LivingEntity.class, "zza", "f_20892_", "field_70703_", "forwardImpulse");
            livingForwardLookupDone = true;
            return livingForwardField;
        }
    }

    private static Field findFloatField(Class<?> owner, String... names) {
        for (String name : names) {
            try {
                Field f = owner.getDeclaredField(name);
                if (f.getType() == float.class || f.getType() == Float.TYPE) {
                    f.setAccessible(true);
                    return f;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private static boolean matchesAny(String path, String... tokens) {
        for (String token : tokens) {
            if (path.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static Object resolvePostChain(Object gameRenderer) {
        try {
            Method m = gameRenderer.getClass().getMethod("getPostEffect");
            Object v = unwrapOptional(m.invoke(gameRenderer));
            if (isLikelyPostChain(v)) {
                return v;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        for (String name : new String[]{"postEffect", "postProcessor", "postChain"}) {
            try {
                Field f = gameRenderer.getClass().getDeclaredField(name);
                f.setAccessible(true);
                Object v = unwrapOptional(f.get(gameRenderer));
                if (isLikelyPostChain(v)) {
                    return v;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }

        for (Field f : gameRenderer.getClass().getDeclaredFields()) {
            String simple = f.getType().getSimpleName();
            if (!simple.contains("Post") && !simple.contains("Effect")) {
                continue;
            }
            try {
                f.setAccessible(true);
                Object v = unwrapOptional(f.get(gameRenderer));
                if (isLikelyPostChain(v)) {
                    return v;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private static Object unwrapOptional(Object raw) {
        if (raw instanceof Optional<?> opt) {
            return opt.orElse(null);
        }
        return raw;
    }

    private static boolean isLikelyPostChain(Object v) {
        if (v == null) {
            return false;
        }
        String simple = v.getClass().getSimpleName();
        return simple.contains("PostChain") || simple.contains("ShaderEffect") || simple.contains("EffectProcessor");
    }

    private static void forEachShaderInChain(Object postChain, java.util.function.Consumer<Object> shaderConsumer) {
        Collection<?> passes = extractPasses(postChain);
        if (passes == null) {
            return;
        }
        for (Object pass : passes) {
            Object shader = readShaderFromPass(pass);
            if (shader != null) {
                shaderConsumer.accept(shader);
            }
        }
    }

    private static Collection<?> extractPasses(Object postChain) {
        try {
            Method m = postChain.getClass().getMethod("getPasses");
            Object raw = m.invoke(postChain);
            if (raw instanceof Collection<?> c) {
                return c;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        for (String fn : new String[]{"passes", "sortedPasses"}) {
            try {
                Field f = postChain.getClass().getDeclaredField(fn);
                f.setAccessible(true);
                Object raw = f.get(postChain);
                if (raw instanceof Collection<?> c) {
                    return c;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private static Object readShaderFromPass(Object pass) {
        for (String mn : new String[]{"getShader", "getEffect"}) {
            try {
                Method m = pass.getClass().getMethod(mn);
                Object raw = unwrapOptional(m.invoke(pass));
                if (raw != null) {
                    return raw;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        for (String name : new String[]{"shader", "effect"}) {
            try {
                Field f = pass.getClass().getDeclaredField(name);
                f.setAccessible(true);
                return f.get(pass);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private static void setUniform3f(Object shaderInstance, String uniformName, float[] rgb) {
        if (shaderInstance == null || rgb == null || rgb.length < 3) {
            return;
        }
        try {
            Method safe = shaderInstance.getClass().getMethod("safeGetUniform", String.class);
            Object raw = safe.invoke(shaderInstance, uniformName);
            if (raw instanceof Optional<?> opt && opt.isPresent()) {
                invokeUniformSet3(opt.get(), rgb[0], rgb[1], rgb[2]);
                return;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Method get = shaderInstance.getClass().getMethod("getUniform", String.class);
            Object u = get.invoke(shaderInstance, uniformName);
            invokeUniformSet3(u, rgb[0], rgb[1], rgb[2]);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void invokeUniformSet3(Object uniform, float x, float y, float z) {
        if (uniform == null) {
            return;
        }
        Class<?> c = uniform.getClass();
        try {
            Method m = c.getMethod("set", float.class, float.class, float.class);
            m.invoke(uniform, x, y, z);
            return;
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Method m = c.getMethod("set", Float.TYPE, Float.TYPE, Float.TYPE);
            m.invoke(uniform, x, y, z);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void setUniform1f(Object shaderInstance, String uniformName, float value) {
        if (shaderInstance == null) {
            return;
        }
        try {
            Method safe = shaderInstance.getClass().getMethod("safeGetUniform", String.class);
            Object raw = safe.invoke(shaderInstance, uniformName);
            if (raw instanceof Optional<?> opt && opt.isPresent()) {
                invokeUniformSet(opt.get(), value);
                return;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Method get = shaderInstance.getClass().getMethod("getUniform", String.class);
            Object u = get.invoke(shaderInstance, uniformName);
            invokeUniformSet(u, value);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static boolean isCaninePredator(ResourceLocation morphId) {
        String path = morphId.getPath().toLowerCase();
        if ("minecraft".equals(morphId.getNamespace())) {
            return "wolf".equals(path) || "fox".equals(path);
        }
        return matchesAny(path, "wolf", "fox", "dog", "coyote", "hyena", "jackal", "dingo");
    }

    /** Baseline dichrome stress so canine morphs never feel like vanilla human vision. */
    private static float canineAmbientPhotoStress(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            return 0.62F;
        }
        int block = mc.level.getBrightness(LightLayer.BLOCK, mc.player.blockPosition());
        int sky = mc.level.getBrightness(LightLayer.SKY, mc.player.blockPosition());
        float light = Math.max(block, sky) / 15.0F;
        long t = mc.level.getDayTime() % 24000L;
        boolean night = t < 13000L || t > 23000L;
        float nightBoost = night ? 0.72F : 0.38F;
        return Mth.clamp(0.48F + light * 0.35F + nightBoost, 0.55F, 1.15F);
    }

    private static void invokeUniformSet(Object uniform, float value) {
        if (uniform == null) {
            return;
        }
        Class<?> c = uniform.getClass();
        try {
            Method m = c.getMethod("set", float.class);
            m.invoke(uniform, value);
            return;
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Method m = c.getMethod("set", Float.TYPE);
            m.invoke(uniform, value);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
