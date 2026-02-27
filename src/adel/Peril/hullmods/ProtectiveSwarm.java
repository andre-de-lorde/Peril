package adel.Peril.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.impl.combat.threat.BaseFragmentMissileEffect;
import com.fs.starfarer.api.impl.combat.threat.FragmentSwarmHullmod;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;


public class ProtectiveSwarm extends BaseHullMod {
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {

    }

    public boolean canIntercept(ShipAPI ship, DamagingProjectileAPI projectile) {
        return true;//if (AIUtils.getBestInterceptPoint(ship.getLocation(), sacFrag.)
    }

    public WeightedRandomPicker<DamagingProjectileAPI> getEnemyProjectiles(ShipAPI ship, float maxRange) {
        final int owner = ship.getOwner();
        List<DamagingProjectileAPI> projectiles = CombatUtils.getProjectilesWithinRange(ship.getLocation(), maxRange);
        for(int i = 0; i < projectiles.size(); i++) {
            final DamagingProjectileAPI p = projectiles.get(i);
            if (p.getOwner() != owner) {
                projectiles.remove(i);
                i--;
            }

        }
        return null;
    }
}