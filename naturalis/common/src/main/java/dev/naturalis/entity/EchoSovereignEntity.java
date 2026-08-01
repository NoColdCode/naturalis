package dev.naturalis.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Echo Sovereign — Evoker model/textures without Evoker spell AI (no vexes / fangs).
 * Combat specials are driven by {@link dev.naturalis.worldgen.EchoSovereignRuntime}.
 */
public class EchoSovereignEntity extends Evoker {

    public EchoSovereignEntity(EntityType<? extends EchoSovereignEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.6D));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }
}
