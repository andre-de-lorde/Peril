package adel.Peril.scripts.ai;

import adel.Peril.combat.SmartFragmentSwarmHullMod;
import adel.Peril.scripts.ai.autofire.DefabricationMissileAutofireAI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.threat.DevouringSwarmMissileEffect;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
//import data.scripts.weapons.Diableavionics_virtuous_itanoEffect;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import javax.swing.*;

public class DefabricationMissileAI implements MissileAIPlugin, GuidedMissileAI{


    private MissileAPI missile;
    private ShipAPI target;
    private CombatEngineAPI engine;
    final private ShipAPI source;
    boolean isAvoiding = false;
    boolean needsToDecelerate = false;
    float lastSwitch = -100;
    boolean dead;



    public DefabricationMissileAI(MissileAPI missile, ShipAPI source) {
        this.missile = missile;
        this.target = null;
        this.source = source;
        missile.setMaxFlightTime(99999);
    }

    @Override
    public CombatEntityAPI getTarget() {
        if (target != null && !target.isExpired()) {
            return target;
        } else {
            return null;
        }
    }

    //private void findTarget() {findTarget(null);}

    private void findTarget(ShipAPI lastship) {

        missile.giveCommand(ShipCommand.ACCELERATE);

        WeightedRandomPicker<ShipAPI> targetPicker = findNextMeal(source, missile.getLocation());
        targetPicker.add(source, missile.getElapsed()/50);


        ShipAPI t = null;
        boolean found = false;

        while (!targetPicker.isEmpty()) {

            t = targetPicker.pickAndRemove();
            if (t == lastship || !canReach(missile,t)) {
                continue;
            } else if (t == null) {
                break;
            } else {
                found = true;
                break;
            }
        }
        if (!found && !canReach(missile,source)) {
            t = source;
        }

        incSwarmsEating(target);
        decSwarmsEating(lastship);

        target = t;
    }

    public final boolean canReach(MissileAPI missile, ShipAPI target) {
        final Vector2f interpos = AIUtils.getBestInterceptPoint(missile.getLocation(),missile.getMaxSpeed(),target.getLocation(),target.getVelocity());
        return interpos != null;
    }

    @Override
    public void setTarget(CombatEntityAPI newTarget) {
        //this.target = newTarget;
    }

    @Override
    public void advance(float amount) {

        if (engine != Global.getCombatEngine()) {
            this.engine = Global.getCombatEngine();
        }

        if (Global.getCombatEngine().isPaused()) return;

        //engine.addFloatingText(this.missile.getLocation(), ""+missile.getElapsed(), 20f, Color.white, missile, 5f, 0.2f);


        if (target == null) {
            if (missile.isFading() || missile.isFizzling()) return;
            missile.giveCommand(ShipCommand.ACCELERATE);
            findTarget(target);
            return;
        }

        if (target.equals(source)) {
            if (missile.isFading() || MathUtils.getDistance(missile,source) < 20f) {
                RoilingSwarmEffect pswarm = RoilingSwarmEffect.getSwarmFor(missile);
                RoilingSwarmEffect sswarm = RoilingSwarmEffect.getSwarmFor(source);

                SmartFragmentSwarmHullMod.transferAllFragments(pswarm,sswarm);
                missile.setHitpoints(0);
            }
        }

        if (missile.isFading() || missile.isFizzling() || missile.getHitpoints() <= 0) {
            if (!dead && target != null) {
                decSwarmsEating(target);
            }
            dead = true;
            return;
        };

        if (target.wasRemoved() || target.isExpired() || target.getCollisionClass() == CollisionClass.NONE) {
            findTarget(target);
            return;
        }

        move(missile,target,amount);
    }

    public static boolean canAvoidShield(ShipAPI ship, MissileAPI missile) {
        if (ship.isHulk()) return true;
        if (ship.isExpired()) return true;
        if (!ship.isAlive()) return true;
        if (ship.getFluxTracker().isOverloadedOrVenting()) return true;
        final ShieldAPI shield = ship.getShield();
        if (shield == null) return true;

        final float distance = MathUtils.getDistance(ship.getLocation(),missile.getLocation());

        if (distance > (((shield.getRadius()) * 1.5) + 150) * 2) return true;
        if (distance < shield.getRadius()) return true;
        //if (Math.random() < ship.getHardFluxLevel()) return true;
        // makes them dive towards the ship a little, chance increasing as the hardflux rises
        // they're just a bit rowdy :)

        //Global.getCombatEngine().addFloatingText(missile.getLocation(),"bitch",10f,Color.white,missile,0f,1f);

        final float directiontoship = VectorUtils.getFacing(VectorUtils.getDirectionalVector(missile.getLocation(), ship.getLocation()));

        return !shield.isWithinArc(missile.getLocation());//canSlip(shield, directiontoship);
    }

    private void move(MissileAPI missile, ShipAPI target, float amount) {

        final Vector2f ntarget = AIUtils.getBestInterceptPoint(missile.getLocation(), missile.getMoveSpeed(), target.getLocation(), target.getVelocity());

        if (ntarget == null) return;

        if (needsToDecelerate) {
            missile.giveCommand(ShipCommand.DECELERATE);
            if (missile.getVelocity().length() < 5) {
                needsToDecelerate = false;
            }
        } else {
            missile.giveCommand(ShipCommand.ACCELERATE);
        }

        if (canAvoidShield(target,missile)) {
            if (isAvoiding){// && (missile.getElapsed() - lastSwitch) > 1) {
                isAvoiding = false;
                needsToDecelerate = true;
                lastSwitch = missile.getElapsed();
            }
        } else {
            if (!isAvoiding){// && (missile.getElapsed() - lastSwitch) > 3) {
                lastSwitch = missile.getElapsed();
                isAvoiding = true;
                needsToDecelerate = true;
            }
        }

        if (!isAvoiding) {
            final float directiontontarget = VectorUtils.getFacing(VectorUtils.getDirectionalVector(missile.getLocation(), ntarget));
            missile.setFacing(directiontontarget);
        } else {
            final float directiontomissile = VectorUtils.getFacing(VectorUtils.getDirectionalVector(target.getLocation(),missile.getLocation()));
            final Vector2f tauntpoint = MathUtils.getPointOnCircumference(target.getLocation(),(target.getShieldRadiusEvenIfNoShield() * 1.5f) + 150, directiontomissile+1);
            final float directiontotauntpoint = VectorUtils.getFacing(VectorUtils.getDirectionalVector(missile.getLocation(),tauntpoint));
            missile.setFacing(directiontotauntpoint);
        }
    }

    public static int getSwarmsEating(ShipAPI ship) {
        if (ship == null) return 0;
        if (ship.getCustomData().get("adel_peril_defabseating") == null) {
            ship.setCustomData("adel_peril_defabseating",(int) 0);
        }
        return (int) ship.getCustomData().get("adel_peril_defabseating");
    }

    public static void setSwarmsEating(ShipAPI ship, int amount) {
        if (ship == null) return;
        final int s = getSwarmsEating(ship);
        ship.setCustomData("adel_peril_defabseating", s+amount);
    }

    public static void decSwarmsEating(ShipAPI ship) {
        setSwarmsEating(ship,getSwarmsEating(ship)-1);
    }

    public static void incSwarmsEating(ShipAPI ship) {
        setSwarmsEating(ship,getSwarmsEating(ship)+1);
    }

    public static WeightedRandomPicker<ShipAPI> findNextMeal(CombatEntityAPI source, Vector2f searchPos) {
        CombatEngineAPI engine = Global.getCombatEngine();
        WeightedRandomPicker<ShipAPI> shipPicker = new WeightedRandomPicker<>();

        final String[] bannedIDs = {"flare", "flare_fighter", "flare_seeker", "flare_standard"};


        List<ShipAPI> ships = engine.getShips();
        for (ShipAPI meal : ships) {
            if (!CombatUtils.isVisibleToSide(meal, source.getOwner())) continue;
            if (meal == source) continue;
            if (meal.getOwner() == source.getOwner() && !meal.isHulk()) continue;
            if (meal.isFighter()) continue;

            if (meal == null) continue;
            if (Arrays.stream(bannedIDs).anyMatch(meal.getHullSpec().getHullId()::equals)) continue;
            if (meal.getCollisionClass() == null) continue;
            if (meal.isExpired()) continue;
            float weight = meal.getMass() / MathUtils.getDistance(searchPos,meal.getLocation());

            if                 (meal.isHulk()) weight *= 10;
            else if (meal.getShield() == null) weight *= 6;
            else                               weight *= 3d * (0.1d + meal.getHardFluxLevel()) * (330d/meal.getShield().getRadius());

            weight /= DefabricationMissileAutofireAI.getSwarmsTargetting(meal);

            shipPicker.add(meal, weight);
        }

        return shipPicker;
    }
}