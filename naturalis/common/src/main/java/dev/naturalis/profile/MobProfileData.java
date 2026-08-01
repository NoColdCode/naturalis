package dev.naturalis.profile;

import dev.naturalis.diet.DietManager;
import dev.naturalis.morph.quickslot.MorphQuickSlotCategory;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/** Resolved per-entity mob profile after archetype inheritance. */
public final class MobProfileData {

    private final ResourceLocation entityId;
    private final DietManager.DietType diet;
    private final Double mass;
    private final Boolean wander;
    private final Boolean flightOnly;
    private final Boolean staticMorph;
    private final Boolean floating;
    private final Double walkSpeed;
    private final Integer smellStrength;
    private final Boolean nyctalopHostile;
    private final Set<String> fears;
    private final Set<String> huntPreyPaths;
    private final Boolean coldVulnerable;
    private final Boolean hotVulnerable;
    private final Boolean wetVulnerable;
    private final Boolean dryVulnerable;
    private final Boolean sunlightSensitive;
    private final Boolean volcanicAdapted;
    private final Boolean snowAdapted;
    private final Boolean enderAdapted;
    private final Boolean caveAdapted;
    private final ResourceLocation visionShader;
    private final String chromaticMode;
    private final Double kaleidoStrength;
    private final Integer kaleidoFolds;
    private final Double spectralProfile;
    private final Double motionTrail;
    private final Boolean caninePredator;
    private final Set<MorphQuickSlotCategory> quickSlotCategories;
    private final MorphQuickSlotCategory quickSlotPrimary;
    private final String resonanceArchetype;
    private final Set<String> tags;

    private MobProfileData(Builder builder) {
        this.entityId = builder.entityId;
        this.diet = builder.diet;
        this.mass = builder.mass;
        this.wander = builder.wander;
        this.flightOnly = builder.flightOnly;
        this.staticMorph = builder.staticMorph;
        this.floating = builder.floating;
        this.walkSpeed = builder.walkSpeed;
        this.smellStrength = builder.smellStrength;
        this.nyctalopHostile = builder.nyctalopHostile;
        this.fears = builder.fears == null ? Set.of() : Set.copyOf(builder.fears);
        this.huntPreyPaths = builder.huntPreyPaths == null ? Set.of() : Set.copyOf(builder.huntPreyPaths);
        this.coldVulnerable = builder.coldVulnerable;
        this.hotVulnerable = builder.hotVulnerable;
        this.wetVulnerable = builder.wetVulnerable;
        this.dryVulnerable = builder.dryVulnerable;
        this.sunlightSensitive = builder.sunlightSensitive;
        this.volcanicAdapted = builder.volcanicAdapted;
        this.snowAdapted = builder.snowAdapted;
        this.enderAdapted = builder.enderAdapted;
        this.caveAdapted = builder.caveAdapted;
        this.visionShader = builder.visionShader;
        this.chromaticMode = builder.chromaticMode;
        this.kaleidoStrength = builder.kaleidoStrength;
        this.kaleidoFolds = builder.kaleidoFolds;
        this.spectralProfile = builder.spectralProfile;
        this.motionTrail = builder.motionTrail;
        this.caninePredator = builder.caninePredator;
        this.quickSlotCategories = builder.quickSlotCategories == null
            ? Set.of() : EnumSet.copyOf(builder.quickSlotCategories);
        this.quickSlotPrimary = builder.quickSlotPrimary;
        this.resonanceArchetype = builder.resonanceArchetype;
        this.tags = builder.tags == null ? Set.of() : Set.copyOf(builder.tags);
    }

    public ResourceLocation entityId() {
        return entityId;
    }

    public Optional<DietManager.DietType> diet() {
        return Optional.ofNullable(diet);
    }

    public Optional<Double> mass() {
        return Optional.ofNullable(mass);
    }

    public Optional<Boolean> wander() {
        return Optional.ofNullable(wander);
    }

    public Optional<Boolean> flightOnly() {
        return Optional.ofNullable(flightOnly);
    }

    public Optional<Boolean> staticMorph() {
        return Optional.ofNullable(staticMorph);
    }

    /** Surface floater — cannot dive; stays on water. */
    public Optional<Boolean> floating() {
        return Optional.ofNullable(floating);
    }

    /** Species gait multiplier (1.0 = normal player walk). Independent of mass inertia. */
    public Optional<Double> walkSpeed() {
        return Optional.ofNullable(walkSpeed);
    }

    public Optional<Integer> smellStrength() {
        return Optional.ofNullable(smellStrength);
    }

    public Optional<Boolean> nyctalopHostile() {
        return Optional.ofNullable(nyctalopHostile);
    }

    public Set<String> fears() {
        return fears;
    }

    public Set<String> huntPreyPaths() {
        return huntPreyPaths;
    }

    public Optional<Boolean> coldVulnerable() {
        return Optional.ofNullable(coldVulnerable);
    }

    public Optional<Boolean> hotVulnerable() {
        return Optional.ofNullable(hotVulnerable);
    }

    public Optional<Boolean> wetVulnerable() {
        return Optional.ofNullable(wetVulnerable);
    }

    public Optional<Boolean> dryVulnerable() {
        return Optional.ofNullable(dryVulnerable);
    }

    public Optional<Boolean> sunlightSensitive() {
        return Optional.ofNullable(sunlightSensitive);
    }

    public Optional<Boolean> volcanicAdapted() {
        return Optional.ofNullable(volcanicAdapted);
    }

    public Optional<Boolean> snowAdapted() {
        return Optional.ofNullable(snowAdapted);
    }

    public Optional<Boolean> enderAdapted() {
        return Optional.ofNullable(enderAdapted);
    }

    public Optional<Boolean> caveAdapted() {
        return Optional.ofNullable(caveAdapted);
    }

    public Optional<ResourceLocation> visionShader() {
        return Optional.ofNullable(visionShader);
    }

    public Optional<String> chromaticMode() {
        return Optional.ofNullable(chromaticMode);
    }

    public Optional<Double> kaleidoStrength() {
        return Optional.ofNullable(kaleidoStrength);
    }

    public Optional<Integer> kaleidoFolds() {
        return Optional.ofNullable(kaleidoFolds);
    }

    public Optional<Double> spectralProfile() {
        return Optional.ofNullable(spectralProfile);
    }

    public Optional<Double> motionTrail() {
        return Optional.ofNullable(motionTrail);
    }

    public Optional<Boolean> caninePredator() {
        return Optional.ofNullable(caninePredator);
    }

    public Set<MorphQuickSlotCategory> quickSlotCategories() {
        return quickSlotCategories;
    }

    public Optional<MorphQuickSlotCategory> quickSlotPrimary() {
        return Optional.ofNullable(quickSlotPrimary);
    }

    public Optional<String> resonanceArchetype() {
        return Optional.ofNullable(resonanceArchetype);
    }

    public Set<String> tags() {
        return tags;
    }

    public boolean hasFear(String fearId) {
        return fears.contains(fearId);
    }

    public Builder toBuilder() {
        return new Builder(entityId)
            .diet(diet)
            .mass(mass)
            .wander(wander)
            .flightOnly(flightOnly)
            .staticMorph(staticMorph)
            .floating(floating)
            .walkSpeed(walkSpeed)
            .smellStrength(smellStrength)
            .nyctalopHostile(nyctalopHostile)
            .fears(fears)
            .huntPreyPaths(huntPreyPaths)
            .coldVulnerable(coldVulnerable)
            .hotVulnerable(hotVulnerable)
            .wetVulnerable(wetVulnerable)
            .dryVulnerable(dryVulnerable)
            .sunlightSensitive(sunlightSensitive)
            .volcanicAdapted(volcanicAdapted)
            .snowAdapted(snowAdapted)
            .enderAdapted(enderAdapted)
            .caveAdapted(caveAdapted)
            .visionShader(visionShader)
            .chromaticMode(chromaticMode)
            .kaleidoStrength(kaleidoStrength)
            .kaleidoFolds(kaleidoFolds)
            .spectralProfile(spectralProfile)
            .motionTrail(motionTrail)
            .caninePredator(caninePredator)
            .quickSlotCategories(quickSlotCategories)
            .quickSlotPrimary(quickSlotPrimary)
            .resonanceArchetype(resonanceArchetype)
            .tags(tags);
    }

    public static Builder builder(ResourceLocation entityId) {
        return new Builder(entityId);
    }

    public static final class Builder {
        private final ResourceLocation entityId;
        private DietManager.DietType diet;
        private Double mass;
        private Boolean wander;
        private Boolean flightOnly;
        private Boolean staticMorph;
        private Boolean floating;
        private Double walkSpeed;
        private Integer smellStrength;
        private Boolean nyctalopHostile;
        private Set<String> fears = new HashSet<>();
        private Set<String> huntPreyPaths = new HashSet<>();
        private Boolean coldVulnerable;
        private Boolean hotVulnerable;
        private Boolean wetVulnerable;
        private Boolean dryVulnerable;
        private Boolean sunlightSensitive;
        private Boolean volcanicAdapted;
        private Boolean snowAdapted;
        private Boolean enderAdapted;
        private Boolean caveAdapted;
        private ResourceLocation visionShader;
        private String chromaticMode;
        private Double kaleidoStrength;
        private Integer kaleidoFolds;
        private Double spectralProfile;
        private Double motionTrail;
        private Boolean caninePredator;
        private Set<MorphQuickSlotCategory> quickSlotCategories = EnumSet.noneOf(MorphQuickSlotCategory.class);
        private MorphQuickSlotCategory quickSlotPrimary;
        private String resonanceArchetype;
        private Set<String> tags = new HashSet<>();

        public Builder(ResourceLocation entityId) {
            this.entityId = entityId;
        }

        public Builder mergeFrom(MobProfileData other) {
            if (other == null) {
                return this;
            }
            if (other.diet != null) diet = other.diet;
            if (other.mass != null) mass = other.mass;
            if (other.wander != null) wander = other.wander;
            if (other.flightOnly != null) flightOnly = other.flightOnly;
            if (other.staticMorph != null) staticMorph = other.staticMorph;
            if (other.floating != null) floating = other.floating;
            if (other.walkSpeed != null) walkSpeed = other.walkSpeed;
            if (other.smellStrength != null) smellStrength = other.smellStrength;
            if (other.nyctalopHostile != null) nyctalopHostile = other.nyctalopHostile;
            if (!other.fears.isEmpty()) fears = new HashSet<>(other.fears);
            if (!other.huntPreyPaths.isEmpty()) huntPreyPaths = new HashSet<>(other.huntPreyPaths);
            if (other.coldVulnerable != null) coldVulnerable = other.coldVulnerable;
            if (other.hotVulnerable != null) hotVulnerable = other.hotVulnerable;
            if (other.wetVulnerable != null) wetVulnerable = other.wetVulnerable;
            if (other.dryVulnerable != null) dryVulnerable = other.dryVulnerable;
            if (other.sunlightSensitive != null) sunlightSensitive = other.sunlightSensitive;
            if (other.volcanicAdapted != null) volcanicAdapted = other.volcanicAdapted;
            if (other.snowAdapted != null) snowAdapted = other.snowAdapted;
            if (other.enderAdapted != null) enderAdapted = other.enderAdapted;
            if (other.caveAdapted != null) caveAdapted = other.caveAdapted;
            if (other.visionShader != null) visionShader = other.visionShader;
            if (other.chromaticMode != null) chromaticMode = other.chromaticMode;
            if (other.kaleidoStrength != null) kaleidoStrength = other.kaleidoStrength;
            if (other.kaleidoFolds != null) kaleidoFolds = other.kaleidoFolds;
            if (other.spectralProfile != null) spectralProfile = other.spectralProfile;
            if (other.motionTrail != null) motionTrail = other.motionTrail;
            if (other.caninePredator != null) caninePredator = other.caninePredator;
            if (!other.quickSlotCategories.isEmpty()) {
                quickSlotCategories = EnumSet.copyOf(other.quickSlotCategories);
            }
            if (other.quickSlotPrimary != null) quickSlotPrimary = other.quickSlotPrimary;
            if (other.resonanceArchetype != null) resonanceArchetype = other.resonanceArchetype;
            if (!other.tags.isEmpty()) tags = new HashSet<>(other.tags);
            return this;
        }

        public Builder diet(DietManager.DietType diet) { this.diet = diet; return this; }
        public Builder mass(Double mass) { this.mass = mass; return this; }
        public Builder wander(Boolean wander) { this.wander = wander; return this; }
        public Builder flightOnly(Boolean flightOnly) { this.flightOnly = flightOnly; return this; }
        public Builder staticMorph(Boolean staticMorph) { this.staticMorph = staticMorph; return this; }
        public Builder floating(Boolean floating) { this.floating = floating; return this; }
        public Builder walkSpeed(Double walkSpeed) { this.walkSpeed = walkSpeed; return this; }
        public Builder smellStrength(Integer smellStrength) { this.smellStrength = smellStrength; return this; }
        public Builder nyctalopHostile(Boolean nyctalopHostile) { this.nyctalopHostile = nyctalopHostile; return this; }
        public Builder fears(Set<String> fears) { this.fears = new HashSet<>(fears); return this; }
        public Builder huntPreyPaths(Set<String> paths) { this.huntPreyPaths = new HashSet<>(paths); return this; }
        public Builder coldVulnerable(Boolean v) { this.coldVulnerable = v; return this; }
        public Builder hotVulnerable(Boolean v) { this.hotVulnerable = v; return this; }
        public Builder wetVulnerable(Boolean v) { this.wetVulnerable = v; return this; }
        public Builder dryVulnerable(Boolean v) { this.dryVulnerable = v; return this; }
        public Builder sunlightSensitive(Boolean v) { this.sunlightSensitive = v; return this; }
        public Builder volcanicAdapted(Boolean v) { this.volcanicAdapted = v; return this; }
        public Builder snowAdapted(Boolean v) { this.snowAdapted = v; return this; }
        public Builder enderAdapted(Boolean v) { this.enderAdapted = v; return this; }
        public Builder caveAdapted(Boolean v) { this.caveAdapted = v; return this; }
        public Builder visionShader(ResourceLocation shader) { this.visionShader = shader; return this; }
        public Builder chromaticMode(String mode) { this.chromaticMode = mode; return this; }
        public Builder kaleidoStrength(Double v) { this.kaleidoStrength = v; return this; }
        public Builder kaleidoFolds(Integer v) { this.kaleidoFolds = v; return this; }
        public Builder spectralProfile(Double v) { this.spectralProfile = v; return this; }
        public Builder motionTrail(Double v) { this.motionTrail = v; return this; }
        public Builder caninePredator(Boolean v) { this.caninePredator = v; return this; }
        public Builder quickSlotCategories(Set<MorphQuickSlotCategory> cats) {
            this.quickSlotCategories = cats == null ? EnumSet.noneOf(MorphQuickSlotCategory.class) : EnumSet.copyOf(cats);
            return this;
        }
        public Builder quickSlotPrimary(MorphQuickSlotCategory cat) { this.quickSlotPrimary = cat; return this; }
        public Builder resonanceArchetype(String archetype) { this.resonanceArchetype = archetype; return this; }
        public Builder tags(Set<String> tags) { this.tags = new HashSet<>(tags); return this; }

        public MobProfileData build() {
            return new MobProfileData(this);
        }
    }
}
