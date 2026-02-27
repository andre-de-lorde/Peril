package adel.Peril.combat;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.threat.*;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect.RoilingSwarmParams;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect.SwarmMember;
import com.fs.starfarer.api.util.*;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.ColorShifterUtil;
import com.fs.starfarer.api.util.Misc;
import org.apache.log4j.Level;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;


import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Math.log;

public class SmartFragmentSwarmHullMod extends FragmentSwarmHullmod {

    IntervalUtil theCoolerTransferUtil = new IntervalUtil(0.0f,2.0f);
    private static final double LOGARITHMIC_CARGO_SCALING_BASE = 1.0076; // works out to give the fabricator 10 fragments per second at 2000 cargo cap
    final float CR_PERCENT_BOOST = 0.0025f;
    final static float MASS_TO_FRAGPOINT_RATIO = 1f/10f;

    public static float getCargoCapacity(ShipAPI ship) {
        final StatBonus cargomod = ship.getMutableStats().getCargoMod();
        return (ship.getHullSpec().getCargo() + cargomod.getMult()) + cargomod.getFlatBonus();
    }

    public static float calculateFragmentsPerSecond(ShipAPI ship) {
        final float cargoCapacity = getCargoCapacity(ship);
        final float effectiveCargoCapacity = cargoCapacity - (cargoCapacity - getFragmentPoints(ship));

        final double elog = Math.log(effectiveCargoCapacity);
        final double clog = Math.log(cargoCapacity);
        final double logbase = Math.log(LOGARITHMIC_CARGO_SCALING_BASE);
        final double cb = clog / logbase;
        final double eb = elog / logbase;

        if (eb > (2 * cb)) {
            return (float) (2 * cb / 100);
        } else {
            return (float) (eb / 100);
        }
        //uses change of base formula
        //in effect, this is log_1.0076(cargocap)
    }

    public static void incFragmentPoints(ShipAPI ship, float mass) {
        addFragmentPoints(ship, (int) (mass * MASS_TO_FRAGPOINT_RATIO));
    }

    public static int getFragmentPoints(ShipAPI ship) {
        if (ship.getCustomData().get("adel_peril_fragpoints") == null) {
            setFragmentPoints(ship,0);
        }
        return (int) ship.getCustomData().get("adel_peril_fragpoints");
    }
    public static void setFragmentPoints(ShipAPI ship, int value) {
        ship.setCustomData("adel_peril_fragpoints", value);
    }
    public static void addFragmentPoints(ShipAPI ship, int amount) {
        setFragmentPoints(ship, getFragmentPoints(ship) + amount);
    }
    public static void transferFragmentPoints(ShipAPI donor, ShipAPI recipient, int amount) {
        if (amount > getFragmentPoints(donor)) amount = getFragmentPoints(donor);
        addFragmentPoints(donor, -amount);
        addFragmentPoints(recipient, amount);
    }

    public static int getCRPoints(ShipAPI ship) {
        if (ship.getCustomData().get("adel_peril_crpoints") == null) {
            setCRPoints(ship,0);
        }
        return (int) ship.getCustomData().get("adel_peril_crpoints");
    }

    public static void setCRPoints(ShipAPI ship, int p) {
        ship.setCustomData("adel_peril_crpoints", p);
    }

    public static void incCRPoints(ShipAPI ship) {
        setCRPoints(ship, getCRPoints(ship)+1);
    }

    public static void addCRPoints(ShipAPI ship, int amnt) {
        setCRPoints(ship, getCRPoints(ship)+amnt);
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (amount <= 0f || ship == null) return;

        RoilingSwarmEffect swarm = RoilingSwarmEffect.getSwarmFor(ship);
        if (swarm == null) swarm = createSwarmFor(ship);

        if (ship.isFighter()) return;

        theCoolerTransferUtil.advance(amount);
        if (theCoolerTransferUtil.intervalElapsed()) runTransferCheck(ship);

        boolean playerShip = Global.getCurrentState() == GameState.COMBAT &&
                Global.getCombatEngine() != null && Global.getCombatEngine().getPlayerShip() == ship;

        RoilingSwarmParams params = swarm.getParams();
        //params.baseMembersToMaintain = (int) ship.getMutableStats().getDynamic().getValue(
                //Stats.FRAGMENT_SWARM_SIZE_MOD, getBaseSwarmSize(ship.getHullSize()));
        //params.memberRespawnRate = getBaseSwarmRespawnRateMult(ship.getHullSize()) *
               // ship.getMutableStats().getDynamic().getValue(Stats.FRAGMENT_SWARM_RESPAWN_RATE_MULT);

//		if (ship.getHullSpec().getHullId().equals(ThreatHullmod.HIVE_UNIT)) {
//			params.baseMembersToMaintain = SwarmLauncherEffect.FRAGMENT_NUM.get(SwarmLauncherEffect.ATTACK_SWARM_WING);
//			params.baseMembersToMaintain *= 8;
//			params.memberRespawnRate = 15 * ship.getMutableStats().getDynamic().getValue(Stats.FRAGMENT_SWARM_RESPAWN_RATE_MULT);
//		}

        params.maxNumMembersToAlwaysRemoveAbove = (int) (params.baseMembersToMaintain * 1.5f);
        //params.initialMembers = params.baseMembersToMaintain;

        params.memberRespawnRate = calculateFragmentsPerSecond(ship);

        if (playerShip) {
            int active = swarm.getNumActiveMembers();

            int maxRequired = 0;
            for (WeaponAPI w : ship.getAllWeapons()) {
                if (w.getEffectPlugin() instanceof FragmentWeapon) {
                    FragmentWeapon fw = (FragmentWeapon) w.getEffectPlugin();
                    maxRequired = Math.max(maxRequired, fw.getNumFragmentsToFire());
                }
            }

            boolean debuff = active < maxRequired;
            Global.getCombatEngine().maintainStatusForPlayerShip(STATUS_KEY1,
                    Global.getSettings().getSpriteName("ui", "icon_tactical_fragment_swarm"),
                    spec.getDisplayName(),
                    "FRAGMENTS: " + active + String.format(" [%.2f/s]",params.memberRespawnRate),
                    debuff);
        }
    }

    public static void transferCR(ShipAPI donor, ShipAPI recipient, float amount, int pointsUsed) {
        addCRPoints(donor,-pointsUsed);
        addCRPoints(recipient,pointsUsed);
        donor.setCurrentCR(donor.getCurrentCR()+amount);
        recipient.setCurrentCR(recipient.getCurrentCR()-amount);
    }

    public void runTransferCheck(ShipAPI donor) {
        WeightedRandomPicker<ShipAPI> buddies = findBuddies(donor, 500);

        if (buddies.isEmpty()) return;
        else {
            boolean CRSkip = false;
            RoilingSwarmEffect dswarm = RoilingSwarmEffect.getSwarmFor(donor);
            ShipAPI recipient = null;
            for (ShipAPI buddy : buddies.getItems()) {
                if (isBiggerThan(donor,buddy) && (buddy.getCurrentCR() < donor.getCurrentCR()) && (getCRPoints(donor) > 500)) {
                    CRSkip = true;
                    recipient = buddy;
                    break;
                }
            }
            if (CRSkip && recipient != null) {
                while (dswarm.getNumActiveMembers() >= 5 && getCRPoints(donor) >= 500) {
                    transferFragments(dswarm, RoilingSwarmEffect.getSwarmFor(recipient),5);
                    transferCR(donor,recipient,CR_PERCENT_BOOST*5,500);
                }
                return;
            }
            recipient = buddies.getItemWithHighestWeight();
            if (recipient == null) return;
            for (int i = 0; i <= 10; i++) {
                if (dswarm.getNumActiveMembers() < 5) return;
                if (recipient.getCurrentCR() >= 1) return;
                transferFragments(dswarm, RoilingSwarmEffect.getSwarmFor(recipient), 5);
                final float generosity = (float) getFragmentPoints(donor) / getCargoCapacity(donor);
                final float need = (float) getFragmentPoints(recipient) / getCargoCapacity(recipient);
                if (generosity > need) transferFragmentPoints(donor,recipient,50);
                if (isBiggerThan(donor, recipient) && getCRPoints(donor) > 500) transferCR(donor, recipient, CR_PERCENT_BOOST * 5, 500);
            }
        }
    }

    public static void transferAllFragments(RoilingSwarmEffect donor, RoilingSwarmEffect recipient) {
        final int amount = donor.getNumActiveMembers();
        transferFragments(donor,recipient,amount);
    }

    public static void transferFragments(RoilingSwarmEffect donor, RoilingSwarmEffect recipient, int amount) {
        for (int i = 0; i < amount; i++) {
            RoilingSwarmEffect.SwarmMember frag = donor.pick(0f);
            transferFragment(donor, recipient, frag);
        }
    }

    private boolean isBiggerThan(ShipAPI ship1, ShipAPI ship2) {return calculateSizeDifference(ship1,ship2) > 0;}

    public static void transferFragment(RoilingSwarmEffect donor, RoilingSwarmEffect recipient, SwarmMember frag) {
        if (frag == null || recipient == null || recipient.isDespawning() || donor.equals(recipient)) return;

        final SpriteAPI s = frag.sprite;
        final float a = frag.angle;
        final Vector2f l = frag.loc;
        final float x = s.getTexX();
        final float y = s.getTexY();
        donor.removeMember(frag);

        frag = recipient.addMember();
        frag.fader.setDurationIn(0);
        frag.sprite.setTexX(x);
        frag.sprite.setTexY(y);
        frag.angle = a;
        frag.loc = l;
    }

    public static RoilingSwarmEffect createSwarmFor(ShipAPI ship) {
        RoilingSwarmEffect existing = RoilingSwarmEffect.getSwarmFor(ship);
        if (existing != null) return existing;

        if (ship.getCustomData().get("adel_peril_constructionship") == null) setFragmentPoints(ship, (int) getCargoCapacity(ship));

        RoilingSwarmParams params = new RoilingSwarmParams();
        if (ship.isFighter()) {
            float radius = 20f;
            int numMembers = 50;

            String wingId = ship.getWing() == null ? null : ship.getWing().getWingId();
            if (SwarmLauncherEffect.SWARM_RADIUS.containsKey(wingId)) {
                radius = SwarmLauncherEffect.SWARM_RADIUS.get(wingId);
            }
            if (SwarmLauncherEffect.FRAGMENT_NUM.containsKey(wingId)) {
                numMembers = SwarmLauncherEffect.FRAGMENT_NUM.get(wingId);
            }

            params.memberExchangeClass = STANDARD_SWARM_EXCHANGE_CLASS;
            params.flockingClass = FragmentSwarmHullmod.STANDARD_SWARM_FLOCKING_CLASS;
            params.maxSpeed = ship.getMaxSpeedWithoutBoost() +
                    Math.max(ship.getMaxSpeedWithoutBoost() * 0.25f + 50f, 100f);

            params.flashRateMult = 0.25f;
            params.flashCoreRadiusMult = 0f;
            params.flashRadius = 120f;
            params.flashFringeColor = new Color(255,0,0,40);
            params.flashCoreColor = new Color(255,255,255,127);

            // if this is set to true and the swarm is glowing, missile-fragments pop over the glow and it looks bad
            //params.renderFlashOnSameLayer = true;

            params.maxOffset = radius;
            params.initialMembers = numMembers;
            params.baseMembersToMaintain = params.initialMembers;
        } else {
            params.memberExchangeClass = STANDARD_SWARM_EXCHANGE_CLASS;
            params.maxSpeed = ship.getMaxSpeedWithoutBoost() +
                    Math.max(ship.getMaxSpeedWithoutBoost() * 0.25f + 50f, 100f) +
                    ship.getMutableStats().getZeroFluxSpeedBoost().getModifiedValue();

            params.flashRateMult = 0.25f;
            params.flashCoreRadiusMult = 0f;
            params.flashRadius = 120f;
            params.flashFringeColor = new Color(255,0,0,40);
            params.flashCoreColor = new Color(255,255,255,127);

            // if this is set to true and the swarm is glowing, missile-fragments pop over the glow and it looks bad
            //params.renderFlashOnSameLayer = true;

            params.minOffset = 0f;
            params.maxOffset = Math.min(100f, ship.getCollisionRadius() * 0.5f);
            params.generateOffsetAroundAttachedEntityOval = true;
            params.despawnSound = null; // ship explosion does the job instead
            params.spawnOffsetMult = 0.33f;
            params.spawnOffsetMultForInitialSpawn = 1f;

            params.baseMembersToMaintain = getBaseSwarmSize(ship.getHullSize());
            params.memberRespawnRate = getBaseSwarmRespawnRateMult(ship.getHullSize());
            params.maxNumMembersToAlwaysRemoveAbove = params.baseMembersToMaintain * 2;

            //params.offsetRerollFractionOnMemberRespawn = 0.05f;
        }

        List<WeaponAPI> glowWeapons = new ArrayList<>();
        for (WeaponAPI w : ship.getAllWeapons()) {
            if (w.usesAmmo() && w.getSpec().hasTag(Tags.FRAGMENT_GLOW)) {
                glowWeapons.add(w);
            }
            if (w.getSpec().hasTag(Tags.OVERSEER_CHARGE) ||
                    (ship.isFighter() && w.getSpec().hasTag(Tags.OVERSEER_CHARGE_FIGHTER))) {
                w.setAmmo(0);
            }
        }

        if (!ship.isFighter()) {
            params.memberRespawnRate = SmartFragmentSwarmHullMod.calculateFragmentsPerSecond(ship);
            params.baseMembersToMaintain = (int) (params.memberRespawnRate * 100);
            params.initialMembers = (int) (params.memberRespawnRate * 10);
        }

        /**if (!ship.getHullSpec().getHullId().equals("fabricator_unit")) {
            params.withInitialMembers = false;
        } else {
            params.initialMembers = params.baseMembersToMaintain;
        }*/
        params.memberExchangeRate = 0;

        return new RoilingSwarmEffect(ship, params) {
            protected ColorShifterUtil glowColorShifter = new ColorShifterUtil(new Color(0, 0, 0, 0));
            protected boolean resetFlash = false;

            private float shouldSpawn;

            @Override
            public int getNumMembersToMaintain() {
                if (ship.isFighter()) {
                    return (int) Math.round(((0.2f + 0.8f * ship.getHullLevel()) * super.getNumMembersToMaintain()));
                }
                return super.getNumMembersToMaintain();
            }

            @Override
            public void advance(float amount) {
                //if (true) return;

                if (Global.getCombatEngine().isPaused() || entity == null || isExpired()) return;

                if (params.withInitialMembers && !spawnedInitial) {
                    final float origSpawnOffsetMult = params.spawnOffsetMult;
                    if (params.spawnOffsetMultForInitialSpawn >= 0) {
                        params.spawnOffsetMult = params.spawnOffsetMultForInitialSpawn;
                    }
                    addMembers(params.initialMembers - getNumActiveMembers());
                    params.spawnOffsetMult = origSpawnOffsetMult;
                    spawnedInitial = true;
                }

                entity.getLocation().set(attachedTo.getLocation());

                elapsed += amount;

                Vector2f aVel = attachedTo.getVelocity();
                final float aSpeed = aVel.length();
                final float leadAmount = aSpeed * params.swarmLeadsByFractionOfVelocity;

                Vector2f facingDir = Misc.getUnitVectorAtDegreeAngle(attachedTo.getFacing());
                if (attachedTo.getVelocity().length() > 1f) {
                    facingDir = Misc.normalise(new Vector2f(attachedTo.getVelocity()));
                }

                Vector2f aLoc = new Vector2f(attachedTo.getLocation());
//		if (params.generateOffsetAroundAttachedEntityOval && attachedTo instanceof ShipAPI) {
//			ShipAPI ship = (ShipAPI) attachedTo;
//			aLoc = new Vector2f(ship.getShieldCenterEvenIfNoShield());
//		}

                List<SwarmMember> remove = new ArrayList<>();

                float maxSpeed = params.maxSpeed;
                if (params.outspeedAttachedEntityBy != 0) {
                    final float minMaxSpeed = attachedTo.getVelocity().length() + params.outspeedAttachedEntityBy;
                    if (minMaxSpeed > maxSpeed) maxSpeed = minMaxSpeed;
                }

                // springs! (sort of, sqrt instead of linear) and friction
                final boolean despawnAll = shouldDespawnAll();

                float maxOffsetForProx = params.maxOffset;
                if (params.generateOffsetAroundAttachedEntityOval) {
                    maxOffsetForProx += attachedTo.getCollisionRadius() * 0.75f;
                }


//		int flashing = 0;
//		for (SwarmMember p : members) {
//			if (p.flash != null) flashing++;
//		}
//		System.out.println("Flashing: " + flashing + " / " + members.size());

                float maxDistSq = 0f;
                maxDistFromCenterToFragment = 0f;
                for (SwarmMember p : members) {
                    final float distSq = (aLoc.x - p.loc.x) * (aLoc.x - p.loc.x) + (aLoc.y - p.loc.y) * (aLoc.y - p.loc.y);
                    maxDistSq = Math.max(maxDistSq, distSq);
                    if (params.despawnDist > 0 && params.despawnDist * params.despawnDist < distSq) {
                        p.fader.fadeOut();
                    }

                    if (!despawnAll) {
                        Vector2f offset = new Vector2f(p.offset);
                        //offset.y *= p.offsetDrift;
                        //offset.y = p.offsetDrift * params.maxOffset;
                        //offset = Misc.rotateAroundOrigin(offset, attachedTo.getFacing() + elapsed * 5f);

                        float prox = offset.length() / maxOffsetForProx;
                        prox = 1f - prox;


                        offset = Misc.rotateAroundOrigin(offset, attachedTo.getFacing() + elapsed * params.offsetRotationDegreesPerSecond);
                        //offset = Misc.rotateAroundOrigin(offset, attachedTo.getFacing());
                        offset.x += facingDir.x * leadAmount;
                        offset.y += facingDir.y * leadAmount;

                        if (!params.keepProxBasedScaleForAllMembers) {
                            p.scale = params.baseScale + (1f - prox) * params.scaleRange;
                            if (p.scale > 1f) p.scale = 1f;
                        }

                        Vector2f dest = new Vector2f(aLoc);
                        Vector2f.add(dest, offset, dest);
                        float dist = Misc.getDistance(p.loc, dest);

                        final Vector2f dirToDest = Misc.getUnitVector(p.loc, dest);
                        final Vector2f perp = new Vector2f(-dirToDest.y, dirToDest.x);

                        float friction = params.baseFriction + params.frictionRange * prox;

                        final float k = params.baseSpringConstant - params.springConstantNegativeRange * prox;
                        final float freeLength = params.baseSpringFreeLength + params.springFreeLengthRange * prox;

                        float stretch = dist - freeLength;

                        stretch = (float) (Math.sqrt(Math.abs(stretch * params.springStretchMult)) * Math.signum(stretch));

                        float forceMag = k * Math.abs(stretch);
                        if (stretch < 0) forceMag = 0; // one-way spring, only pulls

                        final float forceMagReduction = Math.min(Math.abs(forceMag), friction);
                        forceMag -= forceMagReduction;
                        friction -= forceMagReduction;


                        Vector2f force = new Vector2f(dirToDest);
                        force.scale(forceMag * Math.signum(stretch));

                        Vector2f acc = new Vector2f(force);
                        acc.scale(amount);
                        Vector2f.add(p.vel, acc, p.vel);

                        // leftover friction - apply against current velocity
                        if (friction > 0) {
                            float relSpeed = Vector2f.sub(aVel, p.vel, new Vector2f()).length();
                            if (relSpeed > params.minSpeedForFriction) {
                                Vector2f frictionDec = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(p.vel));
                                frictionDec.negate();
                                frictionDec.scale(Math.min(friction, p.vel.length()) * amount);
                                Vector2f.add(p.vel, frictionDec, p.vel);
                            }
                        }

                        // lateral friction to damp out any orbiting behavior fast
                        float lateralSpeed = Math.abs(Vector2f.dot(p.vel, perp));
                        if (lateralSpeed > 0) {// && lateralSpeed > params.minSpeedForFriction) {
                            Vector2f frictionDec = new Vector2f(perp);
                            if (Vector2f.dot(frictionDec, p.vel) > 0) {
                                frictionDec.negate();
                            }
                            float lateralFactor = params.lateralFrictionFactor;
                            lateralFactor += Math.min(Math.abs(attachedTo.getAngularVelocity()), 100f) * params.lateralFrictionTurnRateFactor;
                            float lateralFriction = lateralSpeed * lateralFactor;
                            frictionDec.scale(Math.min(lateralFriction, p.vel.length()) * amount);
                            Vector2f.add(p.vel, frictionDec, p.vel);
                        }



                        final float speed = p.vel.length();
                        if (speed > maxSpeed) {
                            p.vel.scale(maxSpeed / speed);
                        }

                    }

                    p.advance(amount, params);
                    //p.loc.set(dest);

                    if (despawnAll) {
                        if (!p.fader.isFadingOut() && !p.fader.isFadedOut()) {
                            //p.fader.setDurationOut(2f + (float) Math.random() * 1f);
                            p.fader.setDurationOut(params.minDespawnTime +
                                    (params.maxDespawnTime - params.minDespawnTime) * (float) Math.random());
                            p.fader.fadeOut();
                        }
                    }


                    if (p.fader.isFadedOut()) {
                        remove.add(p);
                    }
                }

                maxDistFromCenterToFragment = (float) Math.sqrt(maxDistSq);

                members.removeAll(remove);

                if (despawnAll) {
                    if (!despawning) {
                        if (params.despawnSound != null) {
                            Global.getSoundPlayer().playSound(params.despawnSound, 1f, 1f, entity.getLocation(), aVel);
                            despawning = true;
                        }
                    }
                }

                if (!despawnAll) {
                    shouldSpawn += amount * params.memberRespawnRate;
                    final int num = getNumMembersToMaintain();
                    while (shouldSpawn >= 1) {
                        if (members.size() < num) {
                            addMembers(1);
                            addFragmentPoints(ship,-1);

                            if (params.offsetRerollFractionOnMemberRespawn > 0f) {
                                int reroll = Math.round(params.offsetRerollFractionOnMemberRespawn * members.size());
                                if (reroll < 1) reroll = 1;
                                WeightedRandomPicker<SwarmMember> picker = getPicker(true, false);
                                for (int i = 0; i < reroll; i++) {
                                    SwarmMember pick = picker.pickAndRemove();
                                    if (pick == null) break;
                                    pick.rollOffset(params, attachedTo);
                                }
                            }

                        } else if (members.size() > num && params.removeMembersAboveMaintainLevel) {
                            despawnMembers(1);
                            addFragmentPoints(ship,1);
                        } else if (params.maxNumMembersToAlwaysRemoveAbove >= 0 &&
                                members.size() > params.maxNumMembersToAlwaysRemoveAbove) {
                            int extra = members.size() - params.maxNumMembersToAlwaysRemoveAbove;
                            int numRemove = (int) Math.min(extra * 0.1f, 5f);
                            if (numRemove < 1) numRemove = 1;
                            despawnMembers(numRemove);
                            addFragmentPoints(ship,numRemove);
                        }
                        shouldSpawn -= 1;
                    }

                    flashChecker.advance(amount * params.flashFrequency);
                    params.preFlashDelay -= amount;
                    if (params.preFlashDelay < 0) params.preFlashDelay = 0;
                    if (flashChecker.intervalElapsed() && params.preFlashDelay <= 0) {
                        if (params.flashProbability > 0) {
                            WeightedRandomPicker<SwarmMember> notFlashing = new WeightedRandomPicker<>();
                            for (SwarmMember p : members) {
                                if (p.flash == null) {
                                    notFlashing.add(p);
                                }
                            }
                            for (int i = 0; i < params.numToFlash; i++) {
                                if ((float) Math.random() < params.flashProbability) {
                                    final SwarmMember pick = notFlashing.pickAndRemove();
                                    if (pick != null) pick.flash();
                                }
                            }
                        }
                    }
                }

                glowColorShifter.advance(amount);

                // this is actually QUITE performance-intensive on the rendering, at least doubles the cost per swarm
                // (comment was from when flashFrequency was *10 with a shorter flashRateMult; *2 is pretty ok -am
                if (VoltaicDischargeOnFireEffect.isSwarmPhaseMode(ship)) {
                    params.flashFrequency = 4f;
                    params.flashProbability = 1f;
                    resetFlash = true;
                } else {
                    if (!glowWeapons.isEmpty()) {
                        float ammoFractionTotal = 0f;
                        float totalOP = 0f;
                        for (WeaponAPI w : glowWeapons) {
                            float f = w.getAmmo() / Math.max(1f, w.getMaxAmmo());
                            Color glowColor = w.getSpec().getGlowColor();
                            //						if (f > 0) {
                            //							glowColorShifter.shift(w, glowColor, 0.5f, 0.5f, 1f);
                            //						}
                            glowColorShifter.shift(w, glowColor, 0.5f, 0.5f, 1f);
                            float weight = w.getSpec().getOrdnancePointCost(null);
                            ammoFractionTotal += f * weight;
                            totalOP += weight;
                        }

                        float ammoFraction = ammoFractionTotal / Math.max(1f, totalOP);
                        params.flashFrequency = (1f + ammoFraction) * 2f;
                        params.flashFrequency *= Math.max(1f, Math.min(2f, params.baseMembersToMaintain / 50f));
                        params.flashProbability = 1f;

                        if (ammoFraction <= 0f) params.flashProbability = 0f;

                        float glowAlphaBase = 30f;
                        if (ship.isFighter()) glowAlphaBase = 18f;

                        float extraGlow = (totalOP - 10f) / 90f;
                        if (extraGlow < 0) extraGlow = 0;
                        if (extraGlow > 1f) extraGlow = 1f;

                        int glowAlpha = (int)(glowAlphaBase + glowAlphaBase * (ammoFraction + extraGlow * 0.5f));
                        if (glowAlpha > 255) glowAlpha = 255;
                        //params.flashFringeColor = Misc.setAlpha(glowColorShifter.getCurr(), glowAlpha);
                        params.flashFringeColor = Misc.setBrightness(glowColorShifter.getCurr(), 255);
                        params.flashFringeColor = Misc.setAlpha(params.flashFringeColor, glowAlpha);

                        resetFlash = true;
                    } else {
                        //if (ThreatSwarmAI.isAttackSwarm(ship)) {
                        if (resetFlash) {
                            params.flashProbability = 0f;
                            resetFlash = false;
                        }
                    }
                }
            }

        };

        }

    public WeightedRandomPicker<ShipAPI> findBuddies(ShipAPI source, float maxRange) {

        final CombatEngineAPI engine = Global.getCombatEngine();
        WeightedRandomPicker<ShipAPI> shipPicker = new WeightedRandomPicker<ShipAPI>();

        final List<ShipAPI> ships = engine.getShips();
        for (ShipAPI buddy : ships) {

            if (buddy.getCollisionClass() == null || !buddy.isAlive() || buddy.equals(source)) continue;

            final float dist = MathUtils.getDistance(buddy, source);
            if (dist > maxRange) continue;

            RoilingSwarmEffect bswarm = RoilingSwarmEffect.getSwarmFor(buddy);
            if (bswarm == null) continue;

            if (!bswarm.getParams().memberExchangeClass.equals(RoilingSwarmEffect.getSwarmFor(source).getParams().memberExchangeClass)) continue;

            final int sizediff = calculateSizeDifference(buddy, source);
            final float generosity = (float) (getCargoCapacity(source)-getFragmentPoints(buddy)) / getCargoCapacity(source);
            final float need = (float) (getCargoCapacity(source)-getFragmentPoints(buddy)) / getCargoCapacity(source);

            final float weightfloor = 1/dist;

            float weight = generosity/dist;

            if (sizediff == 1) weight *= 5 * need;
            else if (sizediff > 1) weight *= 1.5 * (need/3);
            else {
                if (need < 0.25) continue;
                weight *= 2 * (need/2);
            }
            if (weight > weightfloor || bswarm.getNumActiveMembers() < 50) {
                shipPicker.add(buddy, weight);
            }
        }
        return shipPicker;
    }

    private int calculateSizeDifference(ShipAPI ship1, ShipAPI ship2) {
        Map<HullSize, Integer> sizeMap = new HashMap<>();
        sizeMap.put(HullSize.CAPITAL_SHIP, 4);
        sizeMap.put(HullSize.CRUISER, 3);
        sizeMap.put(HullSize.DESTROYER, 2);
        sizeMap.put(HullSize.FRIGATE, 1);
        sizeMap.put(HullSize.FIGHTER, 0);

        final int size1 = sizeMap.get(ship1.getHullSize());
        final int size2 = sizeMap.get(ship2.getHullSize());

        return size1 - size2;
    }
}
