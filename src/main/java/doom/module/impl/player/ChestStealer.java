package doom.module.impl.player;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventUpdate;
import doom.module.Module;
import doom.settings.impl.BooleanSetting;
import doom.settings.impl.NumberSetting;
import doom.util.TimeHelper;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.*;

public class ChestStealer extends Module {

    private final NumberSetting delay = new NumberSetting("Delay (ms)", this, 90, 10, 300, 10);
    private final BooleanSetting autoClose = new BooleanSetting("Auto Close", this, true);
    private final BooleanSetting checkName = new BooleanSetting("Check Name", this, true);
    private final BooleanSetting smart = new BooleanSetting("Smart Loot", this, true);

    private final TimeHelper timer = new TimeHelper();

    public ChestStealer() {
        super("ChestStealer", 0, Category.PLAYER);
        Client.INSTANCE.settingsManager.rSetting(delay);
        Client.INSTANCE.settingsManager.rSetting(autoClose);
        Client.INSTANCE.settingsManager.rSetting(checkName);
        Client.INSTANCE.settingsManager.rSetting(smart);
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.thePlayer.openContainer instanceof ContainerChest) {
            ContainerChest chest = (ContainerChest) mc.thePlayer.openContainer;
            String chestName = chest.getLowerChestInventory().getDisplayName().getUnformattedText();

            if (checkName.isEnabled()) {
                String[] badNames = {"Menu", "Select", "Game", "Play", "Shop", "Teleport", "Server", "Kit"};
                for (String bad : badNames) {
                    if (chestName.contains(bad)) return;
                }
            }

            if (isChestEmptyOrUseless(chest)) {
                if (autoClose.isEnabled()) {
                    mc.thePlayer.closeScreen();
                }
                return;
            }

            if (timer.hasReached(delay.getValue())) {
                for (int i = 0; i < chest.getLowerChestInventory().getSizeInventory(); i++) {
                    ItemStack stack = chest.getLowerChestInventory().getStackInSlot(i);
                    if (stack != null && shouldTakeItem(stack)) {
                        mc.playerController.windowClick(chest.windowId, i, 0, 1, mc.thePlayer);
                        timer.reset();
                        return;
                    }
                }
            }
        }
    }

    private boolean isChestEmptyOrUseless(ContainerChest chest) {
        for (int i = 0; i < chest.getLowerChestInventory().getSizeInventory(); i++) {
            ItemStack stack = chest.getLowerChestInventory().getStackInSlot(i);
            if (stack != null) {
                if (!smart.isEnabled() || shouldTakeItem(stack)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean shouldTakeItem(ItemStack stack) {
        if (!smart.isEnabled()) return true;

        if (InventoryManager.isJunk(stack)) return false;

        // FIXED ARMOR CHECK
        if (stack.getItem() instanceof ItemArmor) {
            ItemArmor armor = (ItemArmor) stack.getItem();
            int armorSlot = 5 + armor.armorType;
            ItemStack equipped = mc.thePlayer.inventoryContainer.getSlot(armorSlot).getStack();

            float myProt = InventoryManager.getProtection(equipped);
            float chestProt = InventoryManager.getProtection(stack);

            return chestProt > myProt || myProt <= 0;
        }

        if (stack.getItem() instanceof ItemSword) {
            float myDamage = InventoryManager.getBestDamageScore();
            float chestDamage = InventoryManager.getDamage(stack);
            return chestDamage > myDamage;
        }

        if (stack.getItem() instanceof ItemTool) {
            float myTool = InventoryManager.getBestToolScore(stack.getItem());
            float chestTool = InventoryManager.getToolSpeed(stack);
            return chestTool > myTool;
        }

        return true;
    }
}