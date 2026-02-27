package adel.Peril.weapons;

import adel.Peril.combat.SmartFragmentSwarmHullMod;
import adel.Peril.scripts.ai.DefabricationMissileAI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.impl.combat.DisintegratorEffect;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import com.fs.starfarer.api.impl.combat.threat.DevouringSwarmMissileEffect;

import java.awt.*;
import java.util.Random;

public class adelDevouringSwarmMissileEffect extends DevouringSwarmMissileEffect {

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        super.advance(amount, engine, weapon);

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

        DefabricationMissileAI.incSwarmsEating((ShipAPI) target);

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
            private int fragsMade = 0;

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
                    DefabricationMissileAI.decSwarmsEating((ShipAPI) target);
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

                if (target.isHulk()) {
                    if ((float) Math.random() < 0.33f) return;
                    if ( (Math.random() * target.getMass()) < 20000 ) {
                        engine.applyDamage(target, point, 300f, DamageType.FRAGMENTATION, 0f, false, false, source);
                        //engine.spawnExplosion(point,target.getVelocity(),new Color(55,0,0,20),2f, 0.3f);
                        float cr = source.getCurrentCR();
                        final float PERCENT_BOOST = 0.0025f;
                        cr += (PERCENT_BOOST / 100);
                        if (cr > 1) cr = 1;
                        adel.Peril.combat.SmartFragmentSwarmHullMod.incCRPoints(source);

                        source.setCurrentCR(cr);
                    }
                } else {
                    if ((float) Math.random() < 0.33f) return;
                }/*
                RoilingSwarmEffect.SwarmMember p = sourceSwarm.addMember();
                p.loc.set(loc);
                p.fader.setDurationIn(0.3f);*/
                massStolen += target.getMass();
                fragsMade += 1;
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
