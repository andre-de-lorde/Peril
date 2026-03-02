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


    private static final float DISTANCE_TO_DISSOLVE = 20f; // no clue what this means


    private MissileAPI missile;
    private ShipAPI target;
    private CombatEngineAPI engine;
    final private ShipAPI source;
    boolean isAvoiding = false;
    boolean needsToDecelerate = false;
    float lastSwitch = -100;
    private RoilingSwarmEffect swarm;



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
        if (this.swarm != null) {
            final float distancetotarget = MathUtils.getDistance(missile.getLocation(), target.getLocation());
            final float despawndistance = swarm.getParams().despawnDist;

            if (distancetotarget > despawndistance) return false;
        }

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

        if (this.swarm == null) this.swarm = RoilingSwarmEffect.getSwarmFor(missile);

        if (target.equals(source)) {
            if (missile.isFading() || MathUtils.getDistance(missile,source) < DISTANCE_TO_DISSOLVE) {
                RoilingSwarmEffect pswarm = RoilingSwarmEffect.getSwarmFor(missile);
                RoilingSwarmEffect sswarm = RoilingSwarmEffect.getSwarmFor(source);

                SmartFragmentSwarmHullMod.transferAllFragments(pswarm,sswarm);
                missile.setHitpoints(0);
            }
        }

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

    public static int getSwarmsEating(CombatEntityAPI ship) {
        if (ship == null) return 0;
        if (ship.getCustomData().get("adel_peril_defabseating") == null) {
            ship.setCustomData("adel_peril_defabseating",(int) 0);
        }
        return (int) ship.getCustomData().get("adel_peril_defabseating");
    }

    public static void addSwarmsEating(CombatEntityAPI ship, int amount) {
        if (ship == null) return;
        final int s = getSwarmsEating(ship);
        setSwarmsEating(ship, s+amount);
    }

    public static void setSwarmsEating(CombatEntityAPI ship, int amount) {
        if (ship == null) return;
        ship.setCustomData("adel_peril_defabseating", amount);
    }

    public static void decSwarmsEating(CombatEntityAPI ship) {
        addSwarmsEating(ship,-1);
    }

    public static void incSwarmsEating(CombatEntityAPI ship) {
        addSwarmsEating(ship,1);
    }

    public static float calculateWeight(ShipAPI meal, CombatEntityAPI source, Vector2f searchPos) {
        final float FIGHTER_WEIGHT = 2;
        final float WRECK_WEIGHT = 5;
        final float SHIELD_BLINDSPOT_WEIGHT = 2;
        final float HARDFLUX_WEIGHT = 2;
        final float SHIELDLESS_WEIGHT = 2;
        final float VENTOVER_WEIGHT = 3;
        final float MASS_RATIO = 1/1000; // 1000 mass points = 1 weight

        final String[] bannedIDs = {"flare", "flare_fighter", "flare_seeker", "flare_standard"};

        if (!CombatUtils.isVisibleToSide(meal, source.getOwner())) return -1;
        if (meal == null) return -1;
        if (meal.equals(source)) return -1;
        if (meal.getOwner() == source.getOwner() && !meal.isHulk()) return -1;
        if (Arrays.stream(bannedIDs).anyMatch(meal.getHullSpec().getHullId()::equals)) return -1;
        if (meal.getCollisionClass() == null) return -1;


        final float distance = MathUtils.getDistance(meal.getLocation(),searchPos);

        float weight = 1000 / distance;

        final boolean hulk = meal.isHulk();
        final boolean fighter = meal.isFighter();
        final boolean noshield = meal.getShield() == null;
        final boolean ventover = meal.getFluxTracker().isOverloadedOrVenting();
        final float mass = meal.getMass();

        if ((mass * MASS_RATIO) > 1) {
            weight *= (mass * MASS_RATIO); //ensure that mass can only ever add to the weight
        }
        if (hulk) weight *= WRECK_WEIGHT;
        if (!hulk && noshield) weight *= SHIELDLESS_WEIGHT; //since hulks already have no shields, this is to avoid doubling up the bonus
        if (!hulk && !noshield) weight *= (meal.getHardFluxLevel() * HARDFLUX_WEIGHT);
        if (fighter) weight *= FIGHTER_WEIGHT * (mass / MathUtils.getDistance(meal.getLocation(),source.getLocation()));
        if (!noshield) weight *= SHIELD_BLINDSPOT_WEIGHT * (360-meal.getShield().getActiveArc());
        if (ventover) weight *= VENTOVER_WEIGHT;

        final int swarmstargetting = DefabricationMissileAutofireAI.getSwarmsTargetting(meal);

        weight /= (1+swarmstargetting);

        //Global.getCombatEngine().addFloatingText(meal.getLocation(),swarmstargetting + "|" + weight,50f, Color.white,meal,0,10);

        return weight;
    }

    public static WeightedRandomPicker<ShipAPI> findNextMeal(CombatEntityAPI source, Vector2f searchPos) {
        CombatEngineAPI engine = Global.getCombatEngine();
        WeightedRandomPicker<ShipAPI> shipPicker = new WeightedRandomPicker<>();

        List<ShipAPI> ships = engine.getShips();
        for (ShipAPI meal : ships) {
            float weight = calculateWeight(meal, source, searchPos);
            if (weight > 0) shipPicker.add(meal, weight);
        }

        return shipPicker;
    }
}