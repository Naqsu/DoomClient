package doom.module.impl.player;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventUpdate;
import doom.module.Category;
import doom.module.Module;
import doom.settings.impl.BooleanSetting;
import doom.settings.impl.NumberSetting;
import doom.util.TimeHelper;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import java.util.Arrays;
import java.util.List;

public class InventoryManager extends Module {

    // Settings
    private final NumberSetting delay = new NumberSetting("Delay", this, 150, 0, 500, 10);
    private final BooleanSetting invOpenOnly = new BooleanSetting("Inv Open Only", this, true);
    private final BooleanSetting autoArmor = new BooleanSetting("Auto Armor", this, true);
    private final BooleanSetting clean = new BooleanSetting("Cleaner", this, true);
    private final BooleanSetting sort = new BooleanSetting("Sorter", this, true);

    private final TimeHelper timer = new TimeHelper();

    // Hotbar IDs (0-8)
    private final int SLOT_SWORD = 0;   // Slot 1
    private final int SLOT_GAPPLE = 1;  // Slot 2
    private final int SLOT_PEARL = 2;   // Slot 3
    private final int SLOT_BLOCK_1 = 7; // Slot 8
    private final int SLOT_BLOCK_2 = 8; // Slot 9

    // List of useless items/junk
    private final List<String> junkItems = Arrays.asList(
            "stick", "egg", "string", "cake", "mushroom", "flint", "compass", "feather",
            "tnt", "seeds", "sapling", "flower", "reeds", "sugar", "bowl", "shears", "torch"
    );

    public InventoryManager() {
        super("InvManager", 0, Category.PLAYER);
        Client.INSTANCE.settingsManager.rSetting(delay);
        Client.INSTANCE.settingsManager.rSetting(invOpenOnly);
        Client.INSTANCE.settingsManager.rSetting(autoArmor);
        Client.INSTANCE.settingsManager.rSetting(clean);
        Client.INSTANCE.settingsManager.rSetting(sort);
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        // Safety checks
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (invOpenOnly.isEnabled() && !(mc.currentScreen instanceof GuiInventory)) return;
        if (mc.currentScreen != null && !(mc.currentScreen instanceof GuiInventory)) return;

        // Timer check
        if (!timer.hasReached(delay.getValue())) return;

        // Priorytet 1: Auto Armor (Zakładanie najlepszej zbroi)
        if (autoArmor.isEnabled()) {
            for (int i = 5; i < 9; i++) { // Sloty zbroi (5=Helm, 6=Klatka, 7=Spodnie, 8=Buty)
                int armorType = 3 - (i - 5); // Konwersja na typ zbroi (0=Helm... czekaj, w itemstack 0=helm, w slotach odwrotnie)
                // Naprawa logiki typów:
                // Slot 5 (Helm) -> ItemArmor type 0
                // Slot 6 (Chest) -> ItemArmor type 1
                // Slot 7 (Legs) -> ItemArmor type 2
                // Slot 8 (Boots) -> ItemArmor type 3
                int requiredType = i - 5;

                if (equipBestArmor(i, requiredType)) {
                    timer.reset();
                    return; // Jedna akcja na tick/delay
                }
            }
        }

        // Priorytet 2: Cleaner (Wyrzucanie śmieci i gorszego sprzętu)
        if (clean.isEnabled()) {
            // Skanujemy cały Inventory (9-44)
            for (int i = 9; i < 45; i++) {
                if (mc.thePlayer.inventoryContainer.getSlot(i).getHasStack()) {
                    ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();

                    if (shouldDrop(stack, i)) {
                        drop(i);
                        timer.reset();
                        return;
                    }
                }
            }
        }

        // Priorytet 3: Sorter (Układanie itemów)
        if (sort.isEnabled()) {
            // 1. Miecz -> Slot 0
            if (sortItem(getBestItem(ItemSword.class), SLOT_SWORD)) return;
            // 2. Gapple -> Slot 1
            if (sortItem(FindItem(Items.golden_apple), SLOT_GAPPLE)) return;
            // 3. Perły -> Slot 2
            if (sortItem(FindItem(Items.ender_pearl), SLOT_PEARL)) return;

            // 4. Bloki -> Slot 8 i 7 (Kolejność odwrotna, żeby zapełniać od końca)
            // Znajdź pierwszy stack bloków
            int firstBlockSlot = findBlockSlot(-1);
            if (firstBlockSlot != -1) {
                if (sortItem(firstBlockSlot, SLOT_BLOCK_2)) return;
            }

            // Znajdź drugi stack bloków (ignorując ten, który już jest w SLOT_BLOCK_2 jeśli tam jest)
            int ignore = (mc.thePlayer.inventory.currentItem == SLOT_BLOCK_2) ? -1 : (SLOT_BLOCK_2 + 36);
            // Proste zabezpieczenie: jeśli już coś posortowaliśmy w poprzednim kroku, return zadziałał.
            // Tutaj szukamy kolejnego bloku.
            int secondBlockSlot = findBlockSlot(SLOT_BLOCK_2 + 36); // Ignoruj slot 8 (id 44)
            if (secondBlockSlot != -1) {
                if (sortItem(secondBlockSlot, SLOT_BLOCK_1)) return;
            }
        }
    }

    // --- LOGIKA ARMOR ---

    private boolean equipBestArmor(int armorSlotId, int type) {
        int bestSlot = -1;
        float bestProt = -1;

        // Sprawdź aktualnie założoną zbroję
        ItemStack current = mc.thePlayer.inventoryContainer.getSlot(armorSlotId).getStack();
        if (current != null && current.getItem() instanceof ItemArmor) {
            bestProt = getProtection(current);
        }

        // Szukaj lepszej w EQ
        for (int i = 9; i < 45; i++) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null && stack.getItem() instanceof ItemArmor) {
                ItemArmor armor = (ItemArmor) stack.getItem();
                if (armor.armorType == type) {
                    float prot = getProtection(stack);
                    if (prot > bestProt) {
                        bestProt = prot;
                        bestSlot = i;
                    }
                }
            }
        }

        // Jeśli znaleźliśmy lepszą -> Shift Click
        if (bestSlot != -1) {
            mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, bestSlot, 0, 1, mc.thePlayer);
            return true;
        }
        return false;
    }

    // --- LOGIKA CLEANER ---

    private boolean shouldDrop(ItemStack stack, int slotId) {
        // 1. Hardcoded Junk
        for (String junk : junkItems) {
            if (stack.getItem().getUnlocalizedName().toLowerCase().contains(junk)) return true;
        }

        // 2. Bad Potions (np. trucizna)
        if (stack.getItem() instanceof ItemPotion) {
            if (ItemPotion.isSplash(stack.getMetadata())) return false; // Zostaw splash (mogą być debuffy na wrogów)
            // Sprawdź efekty picia (negatywne wywalamy)
            ItemPotion potion = (ItemPotion) stack.getItem();
            List<PotionEffect> effects = potion.getEffects(stack);
            if (effects != null) {
                for (PotionEffect effect : effects) {
                    if (Potion.potionTypes[effect.getPotionID()].isBadEffect()) return true;
                }
            }
        }

        // 3. Gorszy Ekwipunek (To o co prosiłeś - diament vs żelazo)
        if (stack.getItem() instanceof ItemArmor) {
            ItemArmor armor = (ItemArmor) stack.getItem();
            // Jeśli to nie jest "najlepsza dostępna zbroja tego typu", to wyrzuć.
            // Uwaga: getBestProtInInvAndEquip uwzględnia też ten przedmiot.
            // Jeśli ten przedmiot ma prot 5, a najlepszy w eq ma prot 7 -> ten jest gorszy -> true.
            return getProtection(stack) < getBestProtectionScore(armor.armorType);
        }

        if (stack.getItem() instanceof ItemSword) {
            return getDamage(stack) < getBestDamageScore();
        }

        if (stack.getItem() instanceof ItemTool) {
            // Sprawdzamy czy to najlepszy kilof/siekiera danego typu
            return getToolSpeed(stack) < getBestToolScore(stack.getItem());
        }

        return false;
    }

    private void drop(int slot) {
        mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, slot, 1, 4, mc.thePlayer);
    }

    // --- LOGIKA SORTER ---

    private boolean sortItem(int bestSlotIndex, int hotbarTarget) {
        if (bestSlotIndex != -1) {
            // Konwertuj hotbarTarget (0-8) na slot ID inventory (36-44)
            int targetInventoryId = hotbarTarget + 36;

            if (bestSlotIndex != targetInventoryId) {
                mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, bestSlotIndex, hotbarTarget, 2, mc.thePlayer);
                timer.reset();
                return true;
            }
        }
        return false;
    }

    private int getBestItem(Class<? extends Item> itemClass) {
        int bestSlot = -1;
        float bestScore = -1;

        for (int i = 9; i < 45; i++) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null && itemClass.isInstance(stack.getItem())) {
                float score = 0;
                if (stack.getItem() instanceof ItemSword) score = getDamage(stack);
                else if (stack.getItem() instanceof ItemTool) score = getToolSpeed(stack);

                if (score > bestScore) {
                    bestScore = score;
                    bestSlot = i;
                }
            }
        }
        return bestSlot;
    }

    private int FindItem(Item item) {
        for (int i = 9; i < 45; i++) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null && stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private int findBlockSlot(int ignoreSlot) {
        for (int i = 9; i < 45; i++) {
            if (i == ignoreSlot) continue;
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null && stack.getItem() instanceof ItemBlock) {
                // Filtrujemy bloki, których nie chcemy (np. pajęczyna, kowadło)
                if (!junkItems.contains(stack.getItem().getUnlocalizedName())) {
                    return i;
                }
            }
        }
        return -1;
    }

    // --- MATH & SCORES ---

    private float getProtection(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemArmor)) return 0;
        ItemArmor armor = (ItemArmor) stack.getItem();
        float prot = armor.damageReduceAmount;
        prot += EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack) * 1.25f;
        prot += EnchantmentHelper.getEnchantmentLevel(Enchantment.blastProtection.effectId, stack) * 0.5f;
        prot += EnchantmentHelper.getEnchantmentLevel(Enchantment.fireProtection.effectId, stack) * 0.5f;
        prot += EnchantmentHelper.getEnchantmentLevel(Enchantment.thorns.effectId, stack) * 0.1f;
        prot += EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack) * 0.05f;
        return prot;
    }

    private float getBestProtectionScore(int type) {
        float best = -1;
        // Sprawdzamy sloty pancerza + inventory
        for (int i = 5; i < 45; i++) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null && stack.getItem() instanceof ItemArmor) {
                ItemArmor a = (ItemArmor) stack.getItem();
                if (a.armorType == type) {
                    float score = getProtection(stack);
                    if (score > best) best = score;
                }
            }
        }
        return best;
    }

    private float getDamage(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemSword)) return 0;
        float dmg = 4 + ((ItemSword) stack.getItem()).getDamageVsEntity();
        dmg += EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack) * 1.25f;
        return dmg;
    }

    private float getBestDamageScore() {
        float best = -1;
        for (int i = 9; i < 45; i++) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null && stack.getItem() instanceof ItemSword) {
                float score = getDamage(stack);
                if (score > best) best = score;
            }
        }
        return best;
    }

    private float getToolSpeed(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemTool)) return 0;
        float speed = ((ItemTool) stack.getItem()).getToolMaterial().getEfficiencyOnProperMaterial();
        speed += EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, stack) * 0.75f;
        return speed;
    }

    private float getBestToolScore(Item type) {
        float best = -1;
        for (int i = 9; i < 45; i++) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null && stack.getItem() == type) { // Porównujemy np. kilof z kilofem
                float score = getToolSpeed(stack);
                if (score > best) best = score;
            }
        }
        return best;
    }
}