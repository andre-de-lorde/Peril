package adel.Peril.weapons;

import adel.Peril.combat.SmartFragmentSwarmHullMod;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.impl.combat.threat.FragmentSwarmHullmod;
import com.fs.starfarer.api.impl.combat.threat.FragmentWeapon;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class adelEvilAssLocustMineEffect implements EveryFrameWeaponEffectPlugin,OnFireEffectPlugin, OnHitEffectPlugin, FragmentWeapon {

    private static final String LOCUST_MINE_WEAPON_ID = "adel_fragmentdriver";
    private static final String LOCUST_SWARM_ID = "adel_locust_mine_swarm";
    private static final String LOCUST_SWARM_WEAPON_ID = "adel_locust_swarm_spawner";
    private static final String[] MINESPRITES = {"graphics/asteroids/ring_asteroid00.png",
                                                 "graphics/asteroids/ring_asteroid01.png",
                                                 "graphics/asteroids/ring_asteroid02.png",
                                                 "graphics/asteroids/ring_asteroid03.png",
                                                 "graphics/asteroids/ring_asteroid04.png",
                                                 "graphics/asteroids/ring_asteroid05.png",
                                                 "graphics/asteroids/ring_asteroid06.png",
                                                 "graphics/asteroids/ring_asteroid07.png",
                                                 "graphics/asteroids/ring_asteroid08.png",
                                                 "graphics/asteroids/ring_asteroid09.png",
                                                 "graphics/asteroids/ring_asteroid10.png",
                                                 "graphics/asteroids/ring_asteroid11.png"};

    private DamagingProjectileAPI projectile;

    @Override
    public void showNoFragmentSwarmWarning(WeaponAPI w, ShipAPI ship) {
        FragmentWeapon.super.showNoFragmentSwarmWarning(w, ship);
    }

    @Override
    public int getNumFragmentsToFire() {
        return 40;
    }

    public adelEvilAssLocustMineEffect() {
    }
    /*
    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {
        if (projectile == null) return;
        //projectile.getVelocity().normalise(projectile.getLocation());
    }*/

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        return;
    }

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        this.projectile = projectile;
//s        projectile.getProjectileSpec().setBulletSpriteName(MINESPRITES[MathUtils.getRandomNumberInRange(0,12)]);
    }

    public int calculateSwarmCount(DamagingProjectileAPI projectile) {
       final float liferatio = (20-projectile.getElapsed())/20;
       if (liferatio < 0) {
           return 0;
       }
       return Math.round(5 * liferatio);
    }

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        //if (projectile.getDamageAmount() <= 0f) return;

        final ShipAPI source = projectile.getSource();

        //int swarmcount = 1 + calculateSwarmCount(projectile);

        RoilingSwarmEffect swarm = new RoilingSwarmEffect(projectile);
        swarm.addMembers(getNumFragmentsToFire());
        WeaponAPI locustlauncher = engine.createFakeWeapon(source, LOCUST_SWARM_WEAPON_ID);

        for (int i = 0; i < 5; i++) {
            engine.spawnProjectile(source, locustlauncher, LOCUST_SWARM_WEAPON_ID, LOCUST_SWARM_ID, point, projectile.getFacing(), projectile.getVelocity());
        }
    }
}
