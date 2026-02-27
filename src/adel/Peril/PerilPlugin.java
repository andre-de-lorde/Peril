package adel.Peril;

import adel.Peril.scripts.ai.autofire.DefabricationMissileAutofireAI;
import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.combat.*;

import adel.Peril.scripts.ai.DefabricationMissileAI;


public class PerilPlugin extends BaseModPlugin {
    @Override
    public void onApplicationLoad() throws Exception {
        super.onApplicationLoad();

        // Test that the .jar is loaded and working, using the most obnoxious way possible.
        //throw new RuntimeException("Template mod loaded and working!\nRemove this crash in TemplateModPlugin.");
    }

    @Override
    public void onNewGame() {
        super.onNewGame();
        // Add your code here, or delete this method (it does nothing unless you add code)
    }

    @Override
    public void onAboutToLinkCodexEntries() {
        super.onAboutToLinkCodexEntries();
        //adel_CodexData.linkCodexEntries();
    }

    @Override
    public PluginPick<MissileAIPlugin> pickMissileAI(MissileAPI missile, ShipAPI launchingShip) {
        switch (missile.getProjectileSpecId()) {
            case "devouring_swarm_missile":
                MissileAIPlugin missileAI = new DefabricationMissileAI(missile,launchingShip);
                return new PluginPick<MissileAIPlugin>(missileAI, CampaignPlugin.PickPriority.MOD_SPECIFIC);
            default:
        }
        return null;
        // You can add more methods from ModPlugin here. Press Control-O in IntelliJ to see options.
    }

    @Override
    public PluginPick<AutofireAIPlugin> pickWeaponAutofireAI(WeaponAPI weapon) {
        switch (weapon.getId()) {
            case "devouring_swarm":
                AutofireAIPlugin autofireAI = new DefabricationMissileAutofireAI(weapon);
                return new PluginPick<AutofireAIPlugin>(autofireAI, CampaignPlugin.PickPriority.MOD_SPECIFIC);
            default:
        }
        return null;
        // You can add more methods from ModPlugin here. Press Control-O in IntelliJ to see options.
    }




}
