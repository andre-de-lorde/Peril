package adel.Peril.weapons;

import adel.Peril.combat.SmartFragmentSwarmHullMod;
import adel.Peril.scripts.ai.DefabricationMissileAI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.impl.combat.DisintegratorEffect;
import com.fs.starfarer.api.impl.combat.threat.DevouringSwarmMissileEffect;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class adelEvilAssLocustMissileEffect extends DevouringSwarmMissileEffect {

    private static final float CR_PENALTY = 0.1f; // percentage

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        super.advance(amount, engine, weapon);

    }

    protected void configureMissileSwarmParams(RoilingSwarmEffect.RoilingSwarmParams params) {
//		params.flashFringeColor = new Color(183,65,13,255);
//		params.flashCoreColor = Color.white;
//		params.flashRadius = 40f;
//		params.flashCoreRadiusMult = 0.75f;

        params.tags.add(DISMANTLING_SWARM);

//		params.flashFringeColor = new Color(183,65,13,80);
//		params.flashCoreColor = new Color(183,65,13,127);
        params.flashFringeColor = new Color(64,41,0,50);
        params.flashCoreColor = new Color(128,83,0,127);
        //params.flashCoreColor = new Color(50,165,50,127);

//		params.flashFringeColor = new Color(100,165,100,127);
//		params.flashCoreColor = Color.white;


        //params.flashCoreColor = new Color(183,65,13,127);
        params.flashCoreRadiusMult = 0f;
        params.renderFlashOnSameLayer = true;
        params.flashRadius = 40f;
        params.preFlashDelay = 0.5f * (float) Math.random();

        params.flashFrequency = 40f;
        params.flashProbability = 1f;
    }



    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit,
                      ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (shieldHit) return;
        if (projectile.getDamageAmount() <= 0f) return;
        //if (projectile.isFading()) return;
        if (!(target instanceof ShipAPI)) return;

        ShipAPI source = projectile.getSource();
        RoilingSwarmEffect sourceSwarm = RoilingSwarmEffect.getSwarmFor(source);

        Vector2f offset = Vector2f.sub(point, target.getLocation(), new Vector2f());
        offset = Misc.rotateAroundOrigin(offset, -target.getFacing());

        DisintegratorEffect effect = new DisintegratorEffect(projectile, (ShipAPI) target, offset) {
            protected float getTotalDamage() {
                return projectile.getDamageAmount();
            }
            protected int getNumTicks() {
                return NUM_TICKS;
            }
            protected boolean canDamageHull() {
                return true;
            }
            protected int getNumParticlesPerTick() {
                return 5;
            }
            protected String getSoundLoopId() {
                return "devouring_swarm_loop";
            }
            private boolean gaveFrags = false;
            private double massStolen = 0;

            @Override
            public void advance(float amount) {
                super.advance(amount);
                if (!gaveFrags && (this.isExpired() || target.getCollisionClass() == CollisionClass.NONE)) {
                    if (sourceSwarm != null) {
                        final Vector2f loc = target.getLocation();
                        SmartFragmentSwarmHullMod.incFragmentPoints(source, (float) massStolen);
                        for (int i = 0; i < getNumFragmentsToFire(); i++) {
                            RoilingSwarmEffect.SwarmMember p = sourceSwarm.addMember();
                            p.loc.set(loc);
                            p.fader.setDurationIn(0.3f);
                        }
                    }
                    this.cleanup();
                    gaveFrags = true;
                }
            }

            protected void addParticle() {
                ParticleData p = new ParticleData(25f, 3f + (float) Math.random() * 2f, 1f);
                p.color = new Color(125,100,200,25);
//				p.color = RiftLanceEffect.getColorForDarkening(VoltaicDischargeOnFireEffect.EMP_FRINGE_COLOR);
//				p.color = Misc.setAlpha(p.color, 25);
                particles.add(p);
                p.offset = Misc.getPointWithinRadius(p.offset, 10f);
            }
            protected void damageDealt(Vector2f loc, float hullDamage, float armorDamage) {
                if (sourceSwarm == null || source == null || !source.isAlive()) return;
                if (source.isFighter()) return;


                if (!(hullDamage > 0 || armorDamage > 0)) return;

                if ((float) Math.random() < 0.33f) return;

                target.setCurrentCR(ship.getCurrentCR()-(CR_PENALTY/100));
            }
        };
        CombatEntityAPI e = engine.addLayeredRenderingPlugin(effect);
        e.getLocation().set(projectile.getLocation());

        if (projectile instanceof MissileAPI) {
            MissileAPI missile = (MissileAPI) projectile;
            missile.setDidDamage(true);
            Global.getSoundPlayer().playSound("devouring_swarm_hit_ship", 1f, 1f, point,
                    missile.getVelocity());
        }
    }
}
