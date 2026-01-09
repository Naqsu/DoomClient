package doom.module.impl.player;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventUpdate;
import doom.module.Category;
import doom.module.Module;
import doom.settings.impl.BooleanSetting;
import doom.settings.impl.NumberSetting;
import doom.util.TimeHelper;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

public class ChestStealer extends Module {

    private final NumberSetting delay = new NumberSetting("Delay (ms)", this, 100, 10, 300, 10);
    private final BooleanSetting autoClose = new BooleanSetting("Auto Close", this, true);
    private final BooleanSetting checkName = new BooleanSetting("Check Name", this, true); // Ignoruje menu serwerowe
    private final BooleanSetting randomDelay = new BooleanSetting("Random Delay", this, true);

    private final TimeHelper timer = new TimeHelper();

    public ChestStealer() {
        super("ChestStealer", 0, Category.PLAYER);
        Client.INSTANCE.settingsManager.rSetting(delay);
        Client.INSTANCE.settingsManager.rSetting(autoClose);
        Client.INSTANCE.settingsManager.rSetting(checkName);
        Client.INSTANCE.settingsManager.rSetting(randomDelay);
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.thePlayer.openContainer != null && mc.thePlayer.openContainer instanceof ContainerChest) {
            ContainerChest chest = (ContainerChest) mc.thePlayer.openContainer;
            String chestName = chest.getLowerChestInventory().getDisplayName().getUnformattedText();

            // 1. Sprawdzanie czy to nie jest Menu Serwerowe (np. "Play", "Select Team")
            if (checkName.isEnabled()) {
                String[] badNames = {"Menu", "Select", "Game", "Play", "Shop", "Teleport", "Server"};
                for (String bad : badNames) {
                    if (chestName.contains(bad)) return;
                }
            }

            // 2. Sprawdzanie czy skrzynia jest pusta (czy ukradliśmy wszystko)
            if (isChestEmpty(chest)) {
                if (autoClose.isEnabled()) {
                    mc.thePlayer.closeScreen();
                }
                return;
            }

            // 3. Obliczanie opóźnienia
            long currentDelay = (long) delay.getValue();
            if (randomDelay.isEnabled()) {
                currentDelay += (Math.random() * 50) - 25; // +/- 25ms losowości
            }

            // 4. Kradzież itemów
            if (timer.hasReached(currentDelay)) {
                for (int i = 0; i < chest.getLowerChestInventory().getSizeInventory(); i++) {
                    ItemStack stack = chest.getLowerChestInventory().getStackInSlot(i);
                    if (stack != null) {
                        // Shift-Click (Mode 1)
                        mc.playerController.windowClick(chest.windowId, i, 0, 1, mc.thePlayer);
                        timer.reset();
                        return; // Bierzemy jeden item na update (dla bezpieczeństwa)
                    }
                }
            }
        }
    }

    private boolean isChestEmpty(ContainerChest chest) {
        for (int i = 0; i < chest.getLowerChestInventory().getSizeInventory(); i++) {
            if (chest.getLowerChestInventory().getStackInSlot(i) != null) {
                return false;
            }
        }
        return true;
    }
}