package adel.Peril.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.ai.ModularFleetAIAPI;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.combat.threat.DisposableThreatFleetManager;
import com.fs.starfarer.api.util.Misc;

public class ThreatFleetBehaviorScript extends com.fs.starfarer.api.impl.combat.threat.ThreatFleetBehaviorScript {
    public ThreatFleetBehaviorScript(CampaignFleetAPI fleet, StarSystemAPI system) {
        super(fleet, system);
    }

    public void advance(float amount) {
        if (fleet.getCurrentAssignment() == null) {
            pickNext();
        }

        seenByPlayerTimeout -= amount;

        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player == null) return;

        boolean playerHasSensorMods = Global.getSector().getPlayerMemoryWithoutUpdate().getBoolean(
                DisposableThreatFleetManager.SENSOR_MODS_KEY);
        //playerHasSensorMods = true;
        if (playerHasSensorMods) {
            fleet.getStats().getDynamic().getStat(Stats.DETECTED_BY_PLAYER_RANGE_MULT).modifyMult(
                    DisposableThreatFleetManager.THREAT_DETECTED_RANGE_MULT_ID,
                    DisposableThreatFleetManager.ONSLAUGHT_MKI_SENSOR_MODIFICATIONS_RANGE_MULT);
        } else {
            fleet.getStats().getDynamic().getStat(Stats.DETECTED_BY_PLAYER_RANGE_MULT).unmodifyMult(
                    DisposableThreatFleetManager.THREAT_DETECTED_RANGE_MULT_ID);
        }


        boolean visibleToPlayer = fleet.isVisibleToPlayerFleet() && player.isVisibleToSensorsOf(fleet);
        if (!Global.getSettings().isCampaignSensorsOn() && fleet.isInCurrentLocation()) {
            float dist = Misc.getDistance(fleet, player);
            dist -= fleet.getRadius() + player.getRadius();
            if (playerHasSensorMods) {
                dist /= DisposableThreatFleetManager.ONSLAUGHT_MKI_SENSOR_MODIFICATIONS_RANGE_MULT;
            }
            boolean asb = player.getAbility(Abilities.SENSOR_BURST) != null &&
                    player.getAbility(Abilities.SENSOR_BURST).isActive();
            visibleToPlayer = dist < 150f || asb && dist < 500f;
        }


        if (visibleToPlayer) {
            setSeenByPlayer();
        }
        if (seenByPlayerTimeout > 0f) {
            visibleToPlayer = true;
        }
        //visibleToPlayer = false;

        if (false) {//!visibleToPlayer) {
            if (fleet.getAI() instanceof ModularFleetAIAPI) {
                ModularFleetAIAPI ai = (ModularFleetAIAPI) fleet.getAI();
                for (int i = 0; i < 3; i++) {
                    ai.getNavModule().avoidEntity(player, 3000f, 5000f, 0.2f);
                }

                fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, false);
                fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE, true);
            }
        } else {
            fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
            fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE, false);
        }
    }
}
