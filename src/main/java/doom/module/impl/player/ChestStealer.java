package doom.module.impl.player;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventUpdate;
import doom.module.Module;
import doom.settings.impl.BooleanSetting;
import doom.settings.impl.NumberSetting;
import doom.util.TimeHelper;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.*;

public class ChestStealer extends Module {

    private final NumberSetting delay = new NumberSetting("Delay (ms)", this, 90, 10, 300, 10);
    private final BooleanSetting autoClose = new BooleanSetting("Auto Close", this, true);
    private final BooleanSetting checkName = new BooleanSetting("Check Name", this, true);
    // Jeśli wyłączysz "Smart Loot", będzie brał wszystko jak leci (dla testów)
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

            // Zabezpieczenie przed kradzieżą z menu serwerowych (SkyBlock menu, Teleporter itp.)
            if (checkName.isEnabled()) {
                String[] badNames = {"Menu", "Select", "Game", "Play", "Shop", "Teleport", "Server", "Kit"};
                for (String bad : badNames) {
                    if (chestName.contains(bad)) return;
                }
            }

            // Sprawdzamy czy w skrzyni zostało cokolwiek wartościowego
            if (isChestEmptyOrUseless(chest)) {
                if (autoClose.isEnabled()) {
                    mc.thePlayer.closeScreen();
                }
                return;
            }

            // Główna pętla zabierania itemów
            if (timer.hasReached(delay.getValue())) {
                for (int i = 0; i < chest.getLowerChestInventory().getSizeInventory(); i++) {
                    ItemStack stack = chest.getLowerChestInventory().getStackInSlot(i);

                    // Jeśli slot nie jest pusty i (SmartLoot wyłączony LUB item jest potrzebny)
                    if (stack != null && (!smart.isEnabled() || shouldTakeItem(stack))) {
                        // Shift + Click
                        mc.playerController.windowClick(chest.windowId, i, 0, 1, mc.thePlayer);
                        timer.reset();
                        return; // Bierzemy jeden na cykl (zgodnie z delayem)
                    }
                }
            }
        }
    }

    /**
     * Sprawdza, czy w skrzyni są jeszcze jakiekolwiek przedmioty, które chcemy wziąć.
     */
    private boolean isChestEmptyOrUseless(ContainerChest chest) {
        for (int i = 0; i < chest.getLowerChestInventory().getSizeInventory(); i++) {
            ItemStack stack = chest.getLowerChestInventory().getStackInSlot(i);
            if (stack != null) {
                // Jeśli SmartLoot wyłączony -> skrzynia nie jest pusta (bierzemy wszystko)
                if (!smart.isEnabled()) return false;

                // Jeśli SmartLoot włączony -> sprawdzamy czy ten konkretny item nam się przyda
                if (shouldTakeItem(stack)) {
                    return false; // Znaleziono coś wartościowego, skrzynia nie jest "pusta"
                }
            }
        }
        return true; // Przeszukano wszystko i same śmieci lub pusto
    }

    /**
     * Logika Whitelisty - czy wziąć ten przedmiot?
     */
    private boolean shouldTakeItem(ItemStack stack) {
        Item item = stack.getItem();

        // 1. GAPY I PERŁY - Bierzemy zawsze
        if (item == Items.golden_apple || item == Items.ender_pearl) return true;

        // 2. MIECZE - Bierzemy tylko jeśli lepszy od obecnego
        if (item instanceof ItemSword) {
            float myDamage = InventoryManager.getBestDamageScore(); // Używamy metody z InventoryManager
            float chestDamage = InventoryManager.getDamage(stack);
            return chestDamage > myDamage;
        }

        // 3. ZBROJA - Bierzemy tylko jeśli lepsza
        if (item instanceof ItemArmor) {
            ItemArmor armor = (ItemArmor) item;
            int type = armor.armorType; // 0=helm, 1=klata, 2=spodnie, 3=buty

            // Pobieramy item z naszego slotu zbroi
            ItemStack myArmorStack = mc.thePlayer.inventoryContainer.getSlot(5 + type).getStack();

            float myProt = InventoryManager.getProtection(myArmorStack);
            float chestProt = InventoryManager.getProtection(stack);

            return chestProt > myProt;
        }

        // 4. NARZĘDZIA (Kilof/Siekiera) - Bierzemy tylko jeśli lepsze
        if (item instanceof ItemTool) {
            float myTool = InventoryManager.getBestToolScore(item);
            float chestTool = InventoryManager.getToolSpeed(stack);
            return chestTool > myTool;
        }

        // 5. WODA - Bierzemy jeśli nie mamy wiadra
        if (item == Items.water_bucket) {
            return !hasItem(Items.water_bucket);
        }

        // 6. BLOKI - Bierzemy tylko jeśli mamy mniej niż 3 stacki
        if (item instanceof ItemBlock) {
            return getTotalBlocks() < 192;
        }

        // Wszystko inne (patyki, śmieci) -> Ignoruj
        return false;
    }

    // --- Helpery ---

    private boolean hasItem(Item item) {
        for (int i = 0; i < 45; i++) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null && stack.getItem() == item) return true;
        }
        return false;
    }

    private int getTotalBlocks() {
        int count = 0;
        for (int i = 0; i < 45; i++) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null && stack.getItem() instanceof ItemBlock) {
                count += stack.stackSize;
            }
        }
        return count;
    }
}