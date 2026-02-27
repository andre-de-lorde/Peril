package adel.Peril.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.threat.*;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.CountingMap;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lwjgl.util.vector.Vector2f;

public class BetterConstructionSwarmSystemScript extends ConstructionSwarmSystemScript {

    public static void init() {
        if (inited) return;
        inited = true;
        CONSTRUCTABLE.add(new SwarmConstructableVariant(SwarmConstructableType.COMBAT_UNIT, "skirmish_unit_Type100"));
        CONSTRUCTABLE.add(new SwarmConstructableVariant(SwarmConstructableType.COMBAT_UNIT, "skirmish_unit_Type101"));
        CONSTRUCTABLE.add(new SwarmConstructableVariant(SwarmConstructableType.COMBAT_UNIT, "assault_unit_Type200"));
        CONSTRUCTABLE.add(new SwarmConstructableVariant(SwarmConstructableType.COMBAT_UNIT, "assault_unit_Type201"));
        CONSTRUCTABLE.add(new SwarmConstructableVariant(SwarmConstructableType.COMBAT_UNIT, "standoff_unit_Type300"));
        CONSTRUCTABLE.add(new SwarmConstructableVariant(SwarmConstructableType.COMBAT_UNIT, "standoff_unit_Type301"));
        CONSTRUCTABLE.add(new SwarmConstructableVariant(SwarmConstructableType.COMBAT_UNIT, "standoff_unit_Type302"));

        CONSTRUCTABLE.add(new SwarmConstructableVariant(SwarmConstructableType.OVERSEER, "overseer_unit_Type250"));
        CONSTRUCTABLE.add(new SwarmConstructableVariant(SwarmConstructableType.HIVE, "hive_unit_Type350"));

        CONSTRUCTABLE.add(new SwarmConstructableVariant(SwarmConstructableType.COMBAT_UNIT, "skirmish_unit_Type102"));

        MIN_CR = 1f;
        MIN_DP = 100f;
        MIN_FRAGMENTS = 500;
        MAX_FRAGMENTS = 0;

        for (SwarmConstructableVariant v : CONSTRUCTABLE) {
            MIN_CR = Math.min(v.cr, MIN_CR);
            MIN_DP = Math.min(v.dp, MIN_DP);
            MIN_FRAGMENTS = Math.min(v.fragments, MIN_FRAGMENTS);
            MAX_FRAGMENTS = Math.max(v.fragments, MAX_FRAGMENTS);
        }
    }

    public static int getNumDefabricatorsDeployed(CombatFleetManagerAPI manager) {
        init();
        int count = 0;
        for (DeployedFleetMemberAPI dfm : manager.getDeployedCopyDFM()) {
            ShipAPI ship = dfm.getShip();
            if (ship == null) continue;
            if (ship.getHullSize() == HullSize.CAPITAL_SHIP) continue;

            for (WeaponAPI weapon : ship.getAllWeapons()) {
                if (weapon.getId().equals("devouring_swarm")) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    protected void launchSwarm(ShipAPI ship) {
        findSlots(ship);

        String wingId = SwarmLauncherEffect.CONSTRUCTION_SWARM_WING;

        CombatEngineAPI engine = Global.getCombatEngine();
        CombatFleetManagerAPI manager = engine.getFleetManager(ship.getOwner());
        manager.setSuppressDeploymentMessages(true);

        WeaponSlotAPI slot = slots.pick();

        Vector2f loc = slot.computePosition(ship);
        float facing = slot.computeMidArcAngle(ship);

        ShipAPI fighter = manager.spawnShipOrWing(wingId, loc, facing, 0f, null);
        fighter.getWing().setSourceShip(ship);

        manager.setSuppressDeploymentMessages(false);

        fighter.getMutableStats().getMaxSpeed().modifyMult("construction_swarm", CONSTRUCTION_SWARM_SPEED_MULT);

        Vector2f takeoffVel = Misc.getUnitVectorAtDegreeAngle(facing);
        takeoffVel.scale(fighter.getMaxSpeed() * 1f);

        fighter.setDoNotRender(true);
        fighter.setExplosionScale(0f);
        fighter.setHulkChanceOverride(0f);
        fighter.setImpactVolumeMult(SwarmLauncherEffect.IMPACT_VOLUME_MULT);
        fighter.getArmorGrid().clearComponentMap(); // no damage to weapons/engines
        Vector2f.add(fighter.getVelocity(), takeoffVel, fighter.getVelocity());

        RoilingSwarmEffect sourceSwarm = RoilingSwarmEffect.getSwarmFor(ship);
        if (sourceSwarm == null) return;

        RoilingSwarmEffect swarm = FragmentSwarmHullmod.createSwarmFor(fighter);
        swarm.getParams().flashFringeColor = VoltaicDischargeOnFireEffect.EMP_FRINGE_COLOR;
        RoilingSwarmEffect.getFlockingMap().remove(swarm.getParams().flockingClass, swarm);
        swarm.getParams().flockingClass = FragmentSwarmHullmod.CONSTRUCTION_SWARM_FLOCKING_CLASS;
        RoilingSwarmEffect.getFlockingMap().add(swarm.getParams().flockingClass, swarm);


        SwarmConstructableVariant pick = pickVariant(ship);
        if (pick == null) return;

        String variantId = pick.variantId;
//		variantId = "standoff_unit_Type300";
//		variantId = "overseer_unit_Type250";
//		variantId = "skirmish_unit_Type100";
//		variantId = "assault_unit_Type200";
//		variantId = "assault_unit_Type201"; // no swarm
//		variantId = "hive_unit_Type350";

        ShipVariantAPI variant = Global.getSettings().getVariant(variantId);
        if (variant == null) return;

        ship.setCurrentCR(ship.getCurrentCR() - pick.cr);

        float dp = variant.getHullSpec().getSuppliesToRecover();

        int numFragments = pick.fragments;
        float radiusMult = 1f;
        float collisionMult = 2f;
        float hpMult = 1f;
        float travelTime = 3f;

        if (variant.getHullSize() == HullSize.DESTROYER) {
            radiusMult = 2f;
            collisionMult = 4f;
            hpMult = radiusMult;
            travelTime = 4f;
        } else if (variant.getHullSize() == HullSize.CRUISER) {
            radiusMult = 3.5f;
            collisionMult = 6f;
            hpMult = radiusMult;
            travelTime = 5f;
        } else if (variant.getHullSize() == HullSize.CAPITAL_SHIP) {
            radiusMult = 4;
            collisionMult = 8f;
            hpMult = radiusMult;
            travelTime = 6f;
        }

        for (BoundsAPI.SegmentAPI s : fighter.getExactBounds().getOrigSegments()) {
            s.getP1().scale(collisionMult);
            s.getP2().scale(collisionMult);
            s.set(s.getP1().x, s.getP1().y, s.getP2().x, s.getP2().y);
        }
        fighter.setCollisionRadius(fighter.getCollisionRadius() * collisionMult);

        fighter.setMaxHitpoints(fighter.getMaxHitpoints() * hpMult);
        fighter.setHitpoints(fighter.getHitpoints() * hpMult);

        swarm.getParams().maxOffset *= radiusMult;
//		swarm.params.initialMembers *= numMult;
//		swarm.params.baseMembersToMaintain = swarm.params.initialMembers;
//		requiredFragments = swarm.params.initialMembers;
        swarm.getParams().initialMembers = numFragments;
        swarm.getParams().baseMembersToMaintain = numFragments;

        boolean overseer = variant.getHullSpec().hasTag(Tags.THREAT_OVERSEER);

        SwarmConstructionData data = new SwarmConstructionData();
        data.variantId = variantId;
        data.constructionTime = BASE_CONSTRUCTION_TIME + dp * CONSTRUCTION_TIME_DP_MULT;
        if (overseer) {
            data.constructionTime += CONSTRUCTION_TIME_OVERSEER_EXTRA;
        }
        data.preConstructionTravelTime = travelTime;

        if (fastConstructionLeft > 0) {
            if (pick.size == HullSize.FRIGATE) {
                fastConstructionLeft--;
                data.constructionTime = 2f;
            } else {
                fastConstructionLeft = 0;
            }
        }

        swarm.custom1 = data;



        int transfer = Math.min(numFragments, sourceSwarm.getNumActiveMembers());
        if (transfer > 0) {
            loc = new Vector2f(takeoffVel);
            loc.scale(0.5f);
            Vector2f.add(loc, fighter.getLocation(), loc);
            sourceSwarm.transferMembersTo(swarm, transfer, loc, 100f);
        }

        int add = numFragments - transfer;
        if (add > 0) {
            swarm.addMembers(add);
        }
        swarm.getAttachedTo().setCustomData("adel_peril_constructionship", "yeah");
    }

    @Override
    public SwarmConstructableVariant pickVariant(ShipAPI ship) {
        init();

        //if (true) return new SwarmConstructableVariant(SwarmConstructableType.COMBAT_UNIT, "skirmish_unit_Type102");

//		if (true) {
//			return new SwarmConstructableVariant(SwarmConstructableType.COMBAT_UNIT, "standoff_unit_Type302");
//		}

        CombatEngineAPI engine = Global.getCombatEngine();
        CombatFleetManagerAPI manager = engine.getFleetManager(ship.getOwner());
        if (manager == null) return null;

        RoilingSwarmEffect swarm = RoilingSwarmEffect.getSwarmFor(ship);
        int fragments = swarm == null ? 0 : swarm.getNumActiveMembers();

        int dpLeft = manager.getMaxStrength() - manager.getCurrStrength();
        float cr = ship.getCurrentCR();

        int overseers = getNumOverseersDeployed(manager);
        int hives = getNumHivesDeployed(manager);
        int fabricators = getNumFabricatorsDeployed(manager);
        int defabricators = getNumDefabricatorsDeployed(manager);
        float combatWeight = getCombatWeightDeployed(manager);


        int wantDefabricators = Math.round(combatWeight / (OTHER_SHIP_WEIGHT_PER_OVERSEER/2));
        int wantOverseers = Math.round(combatWeight / OTHER_SHIP_WEIGHT_PER_OVERSEER);
        if (wantOverseers < 1) wantOverseers = 1;

        combatWeight += Math.max(0, fabricators - 1f) * 16f;
        int wantHives = (int) (combatWeight / 16f);

        if (wantHives < 1) wantHives = 1;
        if (wantHives > 2) wantHives = 2;

        wantOverseers -= overseers;
        wantHives -= hives;
        wantDefabricators -= defabricators;

        float frigates = getCombatDeployed(manager, HullSize.FRIGATE);
        float destroyers = getCombatDeployed(manager, HullSize.DESTROYER);
        float cruisers = getCombatDeployed(manager, HullSize.CRUISER);
        float capitals = getCombatDeployed(manager, HullSize.CAPITAL_SHIP);
        float large = cruisers + capitals;

        if (frigates >= 2) {
            fastConstructionLeft = 0;
        }

        CountingMap<HullSize> numCombatVariants = new CountingMap<>();
        for (SwarmConstructableVariant curr : CONSTRUCTABLE) {
            if (curr.type == SwarmConstructableType.COMBAT_UNIT) {
                numCombatVariants.add(curr.size);
            }
        }

        WeightedRandomPicker<SwarmConstructableVariant> hivePicker = new WeightedRandomPicker<>();
        WeightedRandomPicker<SwarmConstructableVariant> overseerPicker = new WeightedRandomPicker<>();
        WeightedRandomPicker<SwarmConstructableVariant> smallPicker = new WeightedRandomPicker<>();
        WeightedRandomPicker<SwarmConstructableVariant> mediumPicker = new WeightedRandomPicker<>();
        WeightedRandomPicker<SwarmConstructableVariant> largePicker = new WeightedRandomPicker<>();

        for (SwarmConstructableVariant curr : CONSTRUCTABLE) {
            if (curr.dp > dpLeft) continue;
            if (curr.cr > cr) continue;
            if (curr.fragments > fragments) continue;

            if (curr.type == SwarmConstructableType.HIVE) {
                hivePicker.add(curr, 1f / curr.dp);
            } else if (curr.type == SwarmConstructableType.OVERSEER) {
                overseerPicker.add(curr, 1f / curr.dp);
            } else {
                float wMult = 1f / Math.max(1f, numCombatVariants.getCount(curr.size));
                if (curr.size == HullSize.FRIGATE) {
                    smallPicker.add(curr, 1f / curr.dp * wMult);
                } else if (curr.size == HullSize.DESTROYER) {
                    mediumPicker.add(curr, 1f / curr.dp * wMult);
                } else {
                    largePicker.add(curr, 1f / curr.dp * wMult);
                }
            }
        }

        if (defabricators <= 1) {
            return new SwarmConstructableVariant(SwarmConstructableType.COMBAT_UNIT, "skirmish_unit_Type102");
        }

        if (frigates <= 1 && !smallPicker.isEmpty()) {
            return smallPicker.pick();
        }

        if (wantDefabricators > 0) {
            return new SwarmConstructableVariant(SwarmConstructableType.COMBAT_UNIT, "skirmish_unit_Type102");
        }

        if (wantOverseers > 0 || wantHives > 0) {
            if (wantOverseers >= wantHives && !overseerPicker.isEmpty()) {
                return overseerPicker.pick();
            } else if (!hivePicker.isEmpty()) {
                return hivePicker.pick();
            }
        }

        if (large <= destroyers * NUM_LARGE_AS_FRACTION_OF_DESTROYERS && !largePicker.isEmpty()) {
            return largePicker.pick();
        }

        if (destroyers <= frigates * NUM_DESTROYERS_AS_FRACTION_OF_FRIGATES && !mediumPicker.isEmpty()) {
            return mediumPicker.pick();
        }

        return smallPicker.pick();
    }
}
