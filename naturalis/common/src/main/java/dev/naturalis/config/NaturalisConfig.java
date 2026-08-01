package dev.naturalis.config;

import dev.naturalis.NaturalisMod;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * NeoForge TOML configuration for Naturalis (Mods → Config screen).
 * <p>
 * Server-wide behaviour lives in {@code naturalis-common.toml};
 * client-only rendering and audio in {@code naturalis-client.toml}.
 * Gamerules can still disable features when {@code respect_game_rules} is true.
 */
public final class NaturalisConfig {

    public static final ModConfigSpec COMMON_SPEC;
    public static final ModConfigSpec CLIENT_SPEC;

    // ── Common: gameplay ───────────────────────────────────────────────────────

    private static final ModConfigSpec.BooleanValue GAMEPLAY_ENABLE_QUADRUPED_RESTRICTIONS;
    private static final ModConfigSpec.BooleanValue GAMEPLAY_ENABLE_KNOWLEDGE_GATES;
    private static final ModConfigSpec.BooleanValue GAMEPLAY_ENABLE_PRIMAL_MOVEMENT;
    private static final ModConfigSpec.BooleanValue GAMEPLAY_ENABLE_FERAL_CURL_SLEEP;
    private static final ModConfigSpec.BooleanValue GAMEPLAY_RESPECT_GAME_RULES;

    // ── Common: vision (synced via gamerules + RuleFlagsPayload) ───────────────

    private static final ModConfigSpec.BooleanValue VISION_ENABLED;
    private static final ModConfigSpec.BooleanValue VISION_RESPECT_GAME_RULES;

    // ── Common: inventory / hotbar ─────────────────────────────────────────────

    private static final ModConfigSpec.BooleanValue INVENTORY_RESTRICTION_ENABLED;
    private static final ModConfigSpec.BooleanValue INVENTORY_RESPECT_GAME_RULES;
    private static final ModConfigSpec.IntValue INVENTORY_CANINE_HOTBAR_SLOTS_AT_RANK_0;
    private static final ModConfigSpec.IntValue INVENTORY_HANDLING_RANK_1_SLOTS;
    private static final ModConfigSpec.IntValue INVENTORY_HANDLING_RANK_2_SLOTS;
    private static final ModConfigSpec.IntValue INVENTORY_HANDLING_RANK_3_SLOTS;
    private static final ModConfigSpec.IntValue INVENTORY_HANDLING_RANK_4_SLOTS;
    private static final ModConfigSpec.IntValue INVENTORY_HANDLING_RANK_5_SLOTS;

    // ── Common: diet ───────────────────────────────────────────────────────────

    private static final ModConfigSpec.BooleanValue DIET_ENABLED;
    private static final ModConfigSpec.BooleanValue DIET_CARNIVORE_PENALTIES;
    private static final ModConfigSpec.BooleanValue DIET_HERBIVORE_PENALTIES;
    private static final ModConfigSpec.BooleanValue DIET_HUMAN_FOOD_PENALTY_WHILE_MORPHED;

    // ── Common: instincts ──────────────────────────────────────────────────────

    private static final ModConfigSpec.BooleanValue INSTINCTS_ENABLED;
    private static final ModConfigSpec.BooleanValue INSTINCTS_RESPECT_GAME_RULES;
    private static final ModConfigSpec.BooleanValue INSTINCTS_Scent_HINTS;

    // ── Common: humanity / resonance ───────────────────────────────────────────

    private static final ModConfigSpec.BooleanValue HUMANITY_ENABLED;
    private static final ModConfigSpec.BooleanValue RESONANCE_ENABLED;
    private static final ModConfigSpec.BooleanValue RESONANCE_CURL_REBIRTH_ENABLED;

    // ── Common: morph binding & brewed morph ───────────────────────────────────

    private static final ModConfigSpec.BooleanValue MORPH_BINDING_ENABLED;
    private static final ModConfigSpec.BooleanValue MORPH_BINDING_BLOCK_TRANSFORM_KEY;
    private static final ModConfigSpec.BooleanValue MORPH_BINDING_BLOCK_REMORPH_MENU;
    private static final ModConfigSpec.BooleanValue BREWED_MORPH_ENABLED;
    private static final ModConfigSpec.BooleanValue BREWED_MORPH_OVERRIDES_BINDING;

    // ── Common: knowledge / utilities ──────────────────────────────────────────

    private static final ModConfigSpec.IntValue KNOWLEDGE_UTILITIES_RANK_TO_MINE;
    private static final ModConfigSpec.IntValue KNOWLEDGE_UTILITIES_RANK_TO_PLACE;
    private static final ModConfigSpec.IntValue KNOWLEDGE_UTILITIES_RANK_FOR_WORLD_USE;
    private static final ModConfigSpec.BooleanValue KNOWLEDGE_QUADRUPED_DIG_FEEDBACK;

    // ── Client: vision ─────────────────────────────────────────────────────────

    private static final ModConfigSpec.BooleanValue CLIENT_VISION_ENABLED;
    private static final ModConfigSpec.BooleanValue CLIENT_VISION_USE_WOLF_LEGACY_SHADER;
    private static final ModConfigSpec.BooleanValue CLIENT_VISION_UPLOAD_PALETTE_UBO;
    private static final ModConfigSpec.DoubleValue CLIENT_VISION_INTENSITY_MULTIPLIER;
    private static final ModConfigSpec.DoubleValue CLIENT_VISION_PHOTO_STRESS_CAP;

    // ── Client: embodiment ─────────────────────────────────────────────────────

    private static final ModConfigSpec.BooleanValue CLIENT_EMBODIMENT_FIRST_PERSON_BODY;
    private static final ModConfigSpec.BooleanValue CLIENT_EMBODIMENT_PAW_DIG_VISUALS;
    private static final ModConfigSpec.BooleanValue CLIENT_EMBODIMENT_CAMERA_OFFSETS;
    private static final ModConfigSpec.BooleanValue CLIENT_EMBODIMENT_HIDE_VANILLA_ARMS;
    private static final ModConfigSpec.DoubleValue CLIENT_EMBODIMENT_FP_BODY_MIN_ARM_HIDE;

    // ── Client: audio & HUD ────────────────────────────────────────────────────

    private static final ModConfigSpec.BooleanValue CLIENT_MUTE_MORPH_PERCEPTION_SOUNDS;
    private static final ModConfigSpec.BooleanValue CLIENT_HUD_KNOWLEDGE_HINTS;
    private static final ModConfigSpec.BooleanValue CLIENT_HUD_SCENT_TRAILS;

    static {
        ModConfigSpec.Builder common = new ModConfigSpec.Builder();
        common.comment("Naturalis — server & shared gameplay settings").push("gameplay");

        GAMEPLAY_ENABLE_QUADRUPED_RESTRICTIONS = common
            .comment("Inventory hotbar locks and quadruped container rules.")
            .translation("naturalis.config.common.gameplay.enable_quadruped_restrictions")
            .define("enable_quadruped_restrictions", true);

        GAMEPLAY_ENABLE_KNOWLEDGE_GATES = common
            .comment("Morph knowledge ranks gate mining, placing, and tool use.")
            .translation("naturalis.config.common.gameplay.enable_knowledge_gates")
            .define("enable_knowledge_gates", true);

        GAMEPLAY_ENABLE_PRIMAL_MOVEMENT = common
            .comment("Primal movement keybind and related morph movement tags.")
            .translation("naturalis.config.common.gameplay.enable_primal_movement")
            .define("enable_primal_movement", true);

        GAMEPLAY_ENABLE_FERAL_CURL_SLEEP = common
            .comment("Curl-sleep / feral rest mechanics and rebirth from curl.")
            .translation("naturalis.config.common.gameplay.enable_feral_curl_sleep")
            .define("enable_feral_curl_sleep", true);

        GAMEPLAY_RESPECT_GAME_RULES = common
            .comment("When true, /gamerule naturalis* flags can disable features below.")
            .translation("naturalis.config.common.gameplay.respect_game_rules")
            .define("respect_game_rules", true);

        common.pop();
        common.push("vision");

        VISION_ENABLED = common
            .comment("Master switch for morph post-processing / color filters.")
            .translation("naturalis.config.common.vision.enabled")
            .define("enabled", true);

        VISION_RESPECT_GAME_RULES = common
            .comment("Honor naturalisEnableColorFilter gamerule when respect_game_rules is on.")
            .translation("naturalis.config.common.vision.respect_game_rules")
            .define("respect_game_rules", true);

        common.pop();
        common.push("inventory");

        INVENTORY_RESTRICTION_ENABLED = common
            .comment("Hotbar slot limits and canine mouth-carrier rules.")
            .translation("naturalis.config.common.inventory.restriction_enabled")
            .define("restriction_enabled", true);

        INVENTORY_RESPECT_GAME_RULES = common
            .comment("Honor naturalisEnableInventoryRestriction gamerule.")
            .translation("naturalis.config.common.inventory.respect_game_rules")
            .define("respect_game_rules", true);

        INVENTORY_CANINE_HOTBAR_SLOTS_AT_RANK_0 = common
            .comment("Hotbar slots for wolf/fox morphs at Handling rank 0.")
            .translation("naturalis.config.common.inventory.canine_hotbar_slots_at_rank_0")
            .defineInRange("canine_hotbar_slots_at_rank_0", 1, 1, 9);

        INVENTORY_HANDLING_RANK_1_SLOTS = common
            .comment("Hotbar slots at Handling rank 1.")
            .translation("naturalis.config.common.inventory.handling_rank_1_slots")
            .defineInRange("handling_rank_1_slots", 4, 1, 9);

        INVENTORY_HANDLING_RANK_2_SLOTS = common
            .comment("Hotbar slots at Handling rank 2.")
            .translation("naturalis.config.common.inventory.handling_rank_2_slots")
            .defineInRange("handling_rank_2_slots", 5, 1, 9);

        INVENTORY_HANDLING_RANK_3_SLOTS = common
            .comment("Hotbar slots at Handling rank 3.")
            .translation("naturalis.config.common.inventory.handling_rank_3_slots")
            .defineInRange("handling_rank_3_slots", 6, 1, 9);

        INVENTORY_HANDLING_RANK_4_SLOTS = common
            .comment("Hotbar slots at Handling rank 4.")
            .translation("naturalis.config.common.inventory.handling_rank_4_slots")
            .defineInRange("handling_rank_4_slots", 7, 1, 9);

        INVENTORY_HANDLING_RANK_5_SLOTS = common
            .comment("Hotbar slots at Handling rank 5.")
            .translation("naturalis.config.common.inventory.handling_rank_5_slots")
            .defineInRange("handling_rank_5_slots", 8, 1, 9);

        common.pop();
        common.push("diet");

        DIET_ENABLED = common
            .comment("Morph diet checks when eating.")
            .translation("naturalis.config.common.diet.enabled")
            .define("enabled", true);

        DIET_CARNIVORE_PENALTIES = common
            .comment("Nausea / penalties for carnivore morphs eating plants.")
            .translation("naturalis.config.common.diet.carnivore_penalties")
            .define("carnivore_penalties", true);

        DIET_HERBIVORE_PENALTIES = common
            .comment("Penalties for herbivore morphs eating meat.")
            .translation("naturalis.config.common.diet.herbivore_penalties")
            .define("herbivore_penalties", true);

        DIET_HUMAN_FOOD_PENALTY_WHILE_MORPHED = common
            .comment("Human-connection diet penalties while morphed.")
            .translation("naturalis.config.common.diet.human_food_penalty_while_morphed")
            .define("human_food_penalty_while_morphed", true);

        common.pop();
        common.push("instincts");

        INSTINCTS_ENABLED = common
            .comment("Passive morph instinct triggers (flee, hunt cues, etc.).")
            .translation("naturalis.config.common.instincts.enabled")
            .define("enabled", true);

        INSTINCTS_RESPECT_GAME_RULES = common
            .comment("Honor naturalisEnableInstincts gamerule.")
            .translation("naturalis.config.common.instincts.respect_game_rules")
            .define("respect_game_rules", true);

        INSTINCTS_Scent_HINTS = common
            .comment("Send scent-trail hints to clients for supported morphs.")
            .translation("naturalis.config.common.instincts.scent_hints")
            .define("scent_hints", true);

        common.pop();
        common.push("humanity");

        HUMANITY_ENABLED = common
            .comment("Humanity meter and related progression.")
            .translation("naturalis.config.common.humanity.enabled")
            .define("enabled", true);

        RESONANCE_ENABLED = common
            .comment("Resonance bonds, rebirth, and instinct key.")
            .translation("naturalis.config.common.humanity.resonance_enabled")
            .define("resonance_enabled", true);

        RESONANCE_CURL_REBIRTH_ENABLED = common
            .comment("Allow human rebirth from feral curl when conditions are met.")
            .translation("naturalis.config.common.humanity.curl_rebirth_enabled")
            .define("curl_rebirth_enabled", true);

        common.pop();
        common.push("morph_binding");

        MORPH_BINDING_ENABLED = common
            .comment("Morph binding potion effect and enforcement.")
            .translation("naturalis.config.common.morph_binding.enabled")
            .define("enabled", true);

        MORPH_BINDING_BLOCK_TRANSFORM_KEY = common
            .comment("Block Woodwalkers transform (G) key while morph_binding is active.")
            .translation("naturalis.config.common.morph_binding.block_transform_key")
            .define("block_transform_key", true);

        MORPH_BINDING_BLOCK_REMORPH_MENU = common
            .comment("Block ReMorphed menu while bound (storm-attuned bypass still applies).")
            .translation("naturalis.config.common.morph_binding.block_remorphed_menu")
            .define("block_remorphed_menu", true);

        common.pop();
        common.push("brewed_morph");

        BREWED_MORPH_ENABLED = common
            .comment("Brewed morph potions and cloud effects.")
            .translation("naturalis.config.common.brewed_morph.enabled")
            .define("enabled", true);

        BREWED_MORPH_OVERRIDES_BINDING = common
            .comment("Brewed morph clears morph_binding on application.")
            .translation("naturalis.config.common.brewed_morph.overrides_binding")
            .define("overrides_binding", true);

        common.pop();
        common.push("knowledge");

        KNOWLEDGE_UTILITIES_RANK_TO_MINE = common
            .comment("Utilities branch rank required for quadrupeds to mine blocks (0–5).")
            .translation("naturalis.config.common.knowledge.utilities_rank_to_mine")
            .defineInRange("utilities_rank_to_mine", 1, 0, 5);

        KNOWLEDGE_UTILITIES_RANK_TO_PLACE = common
            .comment("Utilities rank required to place blocks as a quadruped morph.")
            .translation("naturalis.config.common.knowledge.utilities_rank_to_place")
            .defineInRange("utilities_rank_to_place", 2, 0, 5);

        KNOWLEDGE_UTILITIES_RANK_FOR_WORLD_USE = common
            .comment("Utilities rank for doors, levers, and similar world interactions.")
            .translation("naturalis.config.common.knowledge.utilities_rank_for_world_use")
            .defineInRange("utilities_rank_for_world_use", 3, 0, 5);

        KNOWLEDGE_QUADRUPED_DIG_FEEDBACK = common
            .comment("Scratch particles and sounds when mining is blocked by knowledge.")
            .translation("naturalis.config.common.knowledge.quadruped_dig_feedback")
            .define("quadruped_dig_feedback", true);

        common.pop();
        COMMON_SPEC = common.build();

        ModConfigSpec.Builder client = new ModConfigSpec.Builder();
        client.comment("Naturalis — client rendering & audio").push("vision");

        CLIENT_VISION_ENABLED = client
            .comment("Apply morph vision post effects on this client.")
            .translation("naturalis.config.client.vision.enabled")
            .define("enabled", true);

        CLIENT_VISION_USE_WOLF_LEGACY_SHADER = client
            .comment("Wolf/fox use wolf_vision.fsh instead of vision_palette UBO path.")
            .translation("naturalis.config.client.vision.use_wolf_legacy_shader")
            .define("use_wolf_legacy_shader", true);

        CLIENT_VISION_UPLOAD_PALETTE_UBO = client
            .comment("Upload live VisionPaletteConfig UBO for non-wolf palette shaders. Disable if post shaders crash on your GPU.")
            .translation("naturalis.config.client.vision.upload_palette_ubo")
            .define("upload_palette_ubo", false);

        CLIENT_VISION_INTENSITY_MULTIPLIER = client
            .comment("Global multiplier for morph vision filter strength (0–2).")
            .translation("naturalis.config.client.vision.intensity_multiplier")
            .defineInRange("intensity_multiplier", 1.0D, 0.0D, 2.0D);

        CLIENT_VISION_PHOTO_STRESS_CAP = client
            .comment("Cap PhotoStress uniform for canine vision (reduces gray washout).")
            .translation("naturalis.config.client.vision.photo_stress_cap")
            .defineInRange("photo_stress_cap", 0.35D, 0.0D, 1.0D);

        client.pop();
        client.push("embodiment");

        CLIENT_EMBODIMENT_FIRST_PERSON_BODY = client
            .comment("Render Walkers morph shape in first person for strong embodiment morphs.")
            .translation("naturalis.config.client.embodiment.first_person_body")
            .define("first_person_body", true);

        CLIENT_EMBODIMENT_PAW_DIG_VISUALS = client
            .comment("Paw scratch overlay when mining is blocked or using paw dig style.")
            .translation("naturalis.config.client.embodiment.paw_dig_visuals")
            .define("paw_dig_visuals", true);

        CLIENT_EMBODIMENT_CAMERA_OFFSETS = client
            .comment("Eye height / forward camera offsets per morph profile.")
            .translation("naturalis.config.client.embodiment.camera_offsets")
            .define("camera_offsets", true);

        CLIENT_EMBODIMENT_HIDE_VANILLA_ARMS = client
            .comment("Hide vanilla first-person arms when morph arms are hidden.")
            .translation("naturalis.config.client.embodiment.hide_vanilla_arms")
            .define("hide_vanilla_arms", true);

        CLIENT_EMBODIMENT_FP_BODY_MIN_ARM_HIDE = client
            .comment("Minimum firstPersonArmHideStrength to show full FP morph body (0–1).")
            .translation("naturalis.config.client.embodiment.fp_body_min_arm_hide")
            .defineInRange("fp_body_min_arm_hide", 0.75D, 0.0D, 1.0D);

        client.pop();
        client.push("audio");

        CLIENT_MUTE_MORPH_PERCEPTION_SOUNDS = client
            .comment("Mute morph hearing / combat feedback sounds (also in naturalis-client.json).")
            .translation("naturalis.config.client.audio.mute_morph_perception_sounds")
            .define("mute_morph_perception_sounds", false);

        client.pop();
        client.push("hud");

        CLIENT_HUD_KNOWLEDGE_HINTS = client
            .comment("Show knowledge-related HUD hints.")
            .translation("naturalis.config.client.hud.knowledge_hints")
            .define("knowledge_hints", true);

        CLIENT_HUD_SCENT_TRAILS = client
            .comment("Render scent trail particles from server hints.")
            .translation("naturalis.config.client.hud.scent_trails")
            .define("scent_trails", true);

        client.pop();
        CLIENT_SPEC = client.build();
    }

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, COMMON_SPEC, NaturalisMod.ID + "-common.toml");
        container.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC, NaturalisMod.ID + "-client.toml");
    }

    // ── Effective feature gates (config ∧ optional gamerule) ───────────────────

    public static boolean isColorFilterEnabled(Level level) {
        if (!VISION_ENABLED.get()) {
            return false;
        }
        if (GAMEPLAY_RESPECT_GAME_RULES.get() && VISION_RESPECT_GAME_RULES.get()) {
            return dev.naturalis.rule.NaturalisGameRules.isColorFilterEnabled(level);
        }
        return true;
    }

    public static boolean isInventoryRestrictionEnabled(Level level) {
        if (!GAMEPLAY_ENABLE_QUADRUPED_RESTRICTIONS.get() || !INVENTORY_RESTRICTION_ENABLED.get()) {
            return false;
        }
        if (GAMEPLAY_RESPECT_GAME_RULES.get() && INVENTORY_RESPECT_GAME_RULES.get()) {
            return dev.naturalis.rule.NaturalisGameRules.isInventoryRestrictionEnabled(level);
        }
        return true;
    }

    public static boolean isInstinctsEnabled(Level level) {
        if (!INSTINCTS_ENABLED.get()) {
            return false;
        }
        if (GAMEPLAY_RESPECT_GAME_RULES.get() && INSTINCTS_RESPECT_GAME_RULES.get()) {
            return dev.naturalis.rule.NaturalisGameRules.isInstinctsEnabled(level);
        }
        return true;
    }

    public static boolean isClientVisionEnabled() {
        return CLIENT_VISION_ENABLED.get() && VISION_ENABLED.get();
    }

    // ── Common accessors ───────────────────────────────────────────────────────

    public static boolean gameplayEnableQuadrupedRestrictions() {
        return GAMEPLAY_ENABLE_QUADRUPED_RESTRICTIONS.get();
    }

    public static boolean gameplayEnableKnowledgeGates() {
        return GAMEPLAY_ENABLE_KNOWLEDGE_GATES.get();
    }

    public static boolean gameplayEnablePrimalMovement() {
        return GAMEPLAY_ENABLE_PRIMAL_MOVEMENT.get();
    }

    public static boolean gameplayEnableFeralCurlSleep() {
        return GAMEPLAY_ENABLE_FERAL_CURL_SLEEP.get();
    }

    public static boolean dietEnabled() {
        return DIET_ENABLED.get();
    }

    public static boolean dietCarnivorePenalties() {
        return DIET_CARNIVORE_PENALTIES.get();
    }

    public static boolean dietHerbivorePenalties() {
        return DIET_HERBIVORE_PENALTIES.get();
    }

    public static boolean dietHumanFoodPenaltyWhileMorphed() {
        return DIET_HUMAN_FOOD_PENALTY_WHILE_MORPHED.get();
    }

    public static boolean humanityEnabled() {
        return HUMANITY_ENABLED.get();
    }

    public static boolean resonanceEnabled() {
        return RESONANCE_ENABLED.get();
    }

    public static boolean resonanceCurlRebirthEnabled() {
        return RESONANCE_CURL_REBIRTH_ENABLED.get();
    }

    public static boolean morphBindingEnabled() {
        return MORPH_BINDING_ENABLED.get();
    }

    public static boolean morphBindingBlockTransformKey() {
        return MORPH_BINDING_BLOCK_TRANSFORM_KEY.get();
    }

    public static boolean morphBindingBlockRemorphedMenu() {
        return MORPH_BINDING_BLOCK_REMORPH_MENU.get();
    }

    public static boolean brewedMorphEnabled() {
        return BREWED_MORPH_ENABLED.get();
    }

    public static boolean brewedMorphOverridesBinding() {
        return BREWED_MORPH_OVERRIDES_BINDING.get();
    }

    public static boolean instinctsScentHints() {
        return INSTINCTS_Scent_HINTS.get();
    }

    public static int inventoryCanineHotbarSlotsAtRank0() {
        return INVENTORY_CANINE_HOTBAR_SLOTS_AT_RANK_0.get();
    }

    public static int inventoryHandlingRank1Slots() {
        return INVENTORY_HANDLING_RANK_1_SLOTS.get();
    }

    public static int inventoryHandlingRank2Slots() {
        return INVENTORY_HANDLING_RANK_2_SLOTS.get();
    }

    public static int inventoryHandlingRank3Slots() {
        return INVENTORY_HANDLING_RANK_3_SLOTS.get();
    }

    public static int inventoryHandlingRank4Slots() {
        return INVENTORY_HANDLING_RANK_4_SLOTS.get();
    }

    public static int inventoryHandlingRank5Slots() {
        return INVENTORY_HANDLING_RANK_5_SLOTS.get();
    }

    public static int knowledgeUtilitiesRankToMine() {
        return KNOWLEDGE_UTILITIES_RANK_TO_MINE.get();
    }

    public static int knowledgeUtilitiesRankToPlace() {
        return KNOWLEDGE_UTILITIES_RANK_TO_PLACE.get();
    }

    public static int knowledgeUtilitiesRankForWorldUse() {
        return KNOWLEDGE_UTILITIES_RANK_FOR_WORLD_USE.get();
    }

    public static boolean knowledgeQuadrupedDigFeedback() {
        return KNOWLEDGE_QUADRUPED_DIG_FEEDBACK.get();
    }

    // ── Client accessors ───────────────────────────────────────────────────────

    public static boolean clientVisionUseWolfLegacyShader() {
        return CLIENT_VISION_USE_WOLF_LEGACY_SHADER.get();
    }

    public static boolean clientVisionUploadPaletteUbo() {
        return CLIENT_VISION_UPLOAD_PALETTE_UBO.get();
    }

    public static double clientVisionIntensityMultiplier() {
        return CLIENT_VISION_INTENSITY_MULTIPLIER.get();
    }

    public static double clientVisionPhotoStressCap() {
        return CLIENT_VISION_PHOTO_STRESS_CAP.get();
    }

    public static boolean clientEmbodimentFirstPersonBody() {
        return CLIENT_EMBODIMENT_FIRST_PERSON_BODY.get();
    }

    public static boolean clientEmbodimentPawDigVisuals() {
        return CLIENT_EMBODIMENT_PAW_DIG_VISUALS.get();
    }

    public static boolean clientEmbodimentCameraOffsets() {
        return CLIENT_EMBODIMENT_CAMERA_OFFSETS.get();
    }

    public static boolean clientEmbodimentHideVanillaArms() {
        return CLIENT_EMBODIMENT_HIDE_VANILLA_ARMS.get();
    }

    public static double clientEmbodimentFpBodyMinArmHide() {
        return CLIENT_EMBODIMENT_FP_BODY_MIN_ARM_HIDE.get();
    }

    public static boolean clientMuteMorphPerceptionSounds() {
        return CLIENT_MUTE_MORPH_PERCEPTION_SOUNDS.get();
    }

    public static boolean clientHudKnowledgeHints() {
        return CLIENT_HUD_KNOWLEDGE_HINTS.get();
    }

    public static boolean clientHudScentTrails() {
        return CLIENT_HUD_SCENT_TRAILS.get();
    }

    private NaturalisConfig() {
    }
}
