package adel.Peril.scripts.ai.autofire;

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

        while (!meals.isEmpty()) {
            ShipAPI ship = meals.pick();
            final ShieldAPI shield = ship.getShield();
            if ((shield == null) || !shield.isWithinArc(weapon.getLocation())) {
                break;
            } else {
                final float blindspot = 360 - shield.getActiveArc();
                weight += (ship.getMass() / 1000);
                if (weight < 1) weight = 1;
                weight *= (blindspot/360);
                weight *= (ship.getHardFluxLevel());
                int st = getSwarmsTargetting(ship);
                if (st > 0) weight /= st;
                if (weight < 1) meals.remove(ship);
                else break;
            }
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
        return DefabricationMissileAI.getSwarmsEating(ship);
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
