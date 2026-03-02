package adel.Peril.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Items;

public class BetterFragmentCoordinator extends BaseHullMod {

    private static final int SIZE_INCREASE = 60;
    private static final int SMOD_SIZE_INCREASE = 40;

    @Override
    public CargoStackAPI getRequiredItem() {
        //return Global.getSettings().createCargoStack(CargoItemType.RESOURCES, Commodities.ALPHA_CORE, null);
        return Global.getSettings().createCargoStack(CargoAPI.CargoItemType.SPECIAL,
                new SpecialItemData(Items.THREAT_PROCESSING_UNIT, null), null);
    }

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "" + (int)SIZE_INCREASE + "%";
        return null;
    }

    @Override
    public String getSModDescriptionParam(int index, ShipAPI.HullSize hullSize, ShipAPI ship) {
        if (index == 0) return "" + (int)SMOD_SIZE_INCREASE + "%";
        return null;
    }

    public boolean isApplicableToShip(ShipAPI ship) {
        return ship.getVariant().hasHullMod(HullMods.FRAGMENT_SWARM);
    }

    public String getUnapplicableReason(ShipAPI ship) {
        return "Requires Fragment Swarm hullmod";
    }


}
