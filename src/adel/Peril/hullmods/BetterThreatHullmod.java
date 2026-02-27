package adel.Peril.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.threat.ThreatCombatStrategyAI;
import com.fs.starfarer.api.impl.combat.threat.ThreatHullmod;
import com.fs.starfarer.api.impl.combat.threat.ThreatShipReclamationScript;

public class BetterThreatHullmod extends ThreatHullmod {

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        return;
    }
        /*
        if (!ship.isHulk() || ship.hasTag(SHIP_BEING_RECLAIMED)) return;
        if (ThreatCombatStrategyAI.isFabricator(ship)) return;/*

        float elapsedAsHulk = 0f;
        String key = "elapsedAsHulkKey";
        if (ship.getCustomData().containsKey(key)) {
            elapsedAsHulk = (float) ship.getCustomData().get(key);
        }
        elapsedAsHulk += amount;
        ship.setCustomData(key, elapsedAsHulk);
        if (elapsedAsHulk > 1f) {
            CombatEngineAPI engine = Global.getCombatEngine();
            int owner = ship.getOriginalOwner();
            boolean found = false;
            for (ShipAPI curr : engine.getShips()) {
                if (curr == ship || curr.getOwner() != owner) continue;
                if (curr.isHulk() || curr.getOwner() == 100) continue;
                if (!ThreatCombatStrategyAI.isFabricator(curr)) continue;
                if (curr.getCurrentCR() >= 1f) continue;
                found = true;
                break;
            }
            if (found) {
                Global.getCombatEngine().addPlugin(new ThreatShipReclamationScript(ship, 3f));
            } else {
                ship.setCustomData(key, 0f);
            }
        }*/
}
