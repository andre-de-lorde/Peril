package adel.Peril.scripts.ai.autofire;

import adel.Peril.combat.SmartFragmentSwarmHullMod;
import adel.Peril.scripts.ai.DefabricationMissileAI;
import adel.Peril.weapons.adelDevouringSwarmMissileEffect;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.DisintegratorEffect;
import com.fs.starfarer.api.impl.combat.threat.DevouringSwarmMissileEffect;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.Objects;

public class DefabricationMissileAutofireAI implements AutofireAIPlugin {

    private CombatEngineAPI engine;
    private ShipAPI target;
    private final WeaponAPI weapon;
    private boolean shouldFire;

    public DefabricationMissileAutofireAI(WeaponAPI weapon) {
        this.weapon = weapon;
    }

    @Override
    public void advance(float amount) {
        if (engine != Global.getCombatEngine()) {
            this.engine = Global.getCombatEngine();
        }

        WeightedRandomPicker<ShipAPI> meals = DefabricationMissileAI.findNextMeal(weapon.getShip(),weapon.getLocation());
        int weight = 0;
        final float size = SmartFragmentSwarmHullMod.calculateSize(weapon.getShip());

        while (!meals.isEmpty()) {
            ShipAPI ship = meals.pick();
            if (size > 1) {
                if (meals.getWeight(ship)/size < 1) {
                    meals.remove(ship);
                    continue;
                }
            }
            break;
        }
        if (meals.isEmpty()) {
            shouldFire = false;
            target = null;
        } else {
            shouldFire = true;
            target = meals.pick();
        }
    }

    public static int getSwarmsTargetting(ShipAPI ship) {
        final int amount = DefabricationMissileAI.getSwarmsEating(ship);
        return amount;
    }



    @Override
    public boolean shouldFire() {
        return shouldFire;
    }

    @Override
    public void forceOff() {
       shouldFire = false;
    }

    @Override
    public Vector2f getTarget() {
        if (target instanceof ShipAPI) {
            return target.getLocation();
        }
        return null;//target.getLocation();
    }

    @Override
    public ShipAPI getTargetShip() {
        if (target instanceof ShipAPI) {
            return target;//null; //target;
        }
        return null;
    }

    @Override
    public WeaponAPI getWeapon() {
        return weapon;
    }

    @Override
    public MissileAPI getTargetMissile() {
        return null;
    }
}
