package adel.Peril.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.combat.threat.SecondaryFabricatorHullmod;

public class BetterSecondaryFabricator extends BaseHullMod {
    private static final float RATE_INCREASE = 30f;
    private static final float SMOD_RATE_INCREASE = 20f;

    @Override
    public CargoStackAPI getRequiredItem() {
        return Global.getSettings().createCargoStack(CargoAPI.CargoItemType.SPECIAL,
                new SpecialItemData(Items.FRAGMENT_FABRICATOR, null), null);
    }


    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "" + (int)RATE_INCREASE + "%";
        return null;
    }

    @Override
    public String getSModDescriptionParam(int index, ShipAPI.HullSize hullSize, ShipAPI ship) {
        if (index == 0) return "" + (int)SMOD_RATE_INCREASE + "%";
        return null;
    }

    public boolean isApplicableToShip(ShipAPI ship) {
        return ship.getVariant().hasHullMod(HullMods.FRAGMENT_SWARM);
    }

    public String getUnapplicableReason(ShipAPI ship) {
        return "Requires Fragment Swarm hullmod";
    }
}
