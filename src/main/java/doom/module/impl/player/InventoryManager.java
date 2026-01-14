package doom.module.impl.player;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventUpdate;
import doom.module.Module;
import doom.settings.impl.BooleanSetting;
import doom.settings.impl.NumberSetting;
import doom.util.TimeHelper;
import net.minecraft.client.Minecraft;
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

    private final NumberSetting delay = new NumberSetting("Delay", this, 120, 0, 500, 10);
    private final BooleanSetting invOpenOnly = new BooleanSetting("Inv Open Only", this, true);
    private final BooleanSetting autoArmor = new BooleanSetting("Auto Armor", this, true);
    private final BooleanSetting clean = new BooleanSetting("Cleaner", this, true);
    private final BooleanSetting sort = new BooleanSetting("Sorter", this, true);

    private final TimeHelper timer = new TimeHelper();

    // Usunąłem "chest" i "egg" z listy, bo psuły nazwy armorów.
    // Zamiast "chest" jest "chest_minecart" itp.
    public static final List<String> junkList = Arrays.asList(
            "stick", "string", "cake", "mushroom", "flint", "compass", "feather",
            "tnt", "seeds", "sapling", "flower", "reeds", "sugar", "bowl", "shears",
            "torch", "flesh", "wheat", "dye", "boat", "paper", "book", "coal", "cobblestone", "dirt"
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
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (invOpenOnly.isEnabled() && !(mc.currentScreen instanceof GuiInventory)) return;
        if (mc.currentScreen != null && !(mc.currentScreen instanceof GuiInventory)) return;

        if (!timer.hasReached(delay.getValue())) return;

        // --- 1. AUTO ARMOR ---
        if (autoArmor.isEnabled()) {
            for (int i = 5; i < 9; i++) {
                int armorType = i - 5;
                if (equipBestArmor(i, armorType)) {
                    timer.reset();
                    return;
                }
            }
        }

        // --- 2. CLEANER ---
        if (clean.isEnabled()) {
            for (int i = 9; i < 45; i++) {
                if (mc.thePlayer.inventoryContainer.getSlot(i).getHasStack()) {
                    ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
                    if (isTrash(stack, i)) {
                        drop(i);
                        timer.reset();
                        return;
                    }
                }
            }
        }

        // --- 3. SORTER (FIXED) ---
        if (sort.isEnabled()) {
            // Miecz (Slot 1)
            if (sortItem(getBestItemSlot(ItemSword.class), 0)) return;

            // Łuk (Slot 2)
            if (sortItem(getBestItemSlot(ItemBow.class), 1)) return;

            // Kilof (Slot 3)
            if (sortItem(getBestItemSlot(ItemPickaxe.class), 2)) return;

            // Siekiera (Slot 4)
            if (sortItem(getBestItemSlot(ItemAxe.class), 3)) return;

            // Złote Jabłka (Slot 5) - FIX INFINITE SWAP
            // Sprawdzamy czy slot 5 (index 36+4 = 40) już ma gapple
            ItemStack slot5 = mc.thePlayer.inventoryContainer.getSlot(36 + 4).getStack();
            boolean hasGapple = slot5 != null && slot5.getItem() == Items.golden_apple;

            if (!hasGapple) {
                int gapSlot = FindItem(Items.golden_apple);
                if (gapSlot != -1) {
                    if (sortItem(gapSlot, 4)) return;
                }
            }

            // Bloki (Slot 9) - FIX
            ItemStack slot9 = mc.thePlayer.inventoryContainer.getSlot(36 + 8).getStack();
            boolean hasBlocks = slot9 != null && slot9.getItem() instanceof ItemBlock;

            if (!hasBlocks) {
                int blockSlot = findBlockSlot();
                if (blockSlot != -1) {
                    if (sortItem(blockSlot, 8)) return;
                }
            }
        }
    }

    private boolean isTrash(ItemStack stack, int currentSlotId) {
        // FIX: Najpierw sprawdzamy czy to użyteczny przedmiot, ZANIM sprawdzimy nazwę
        if (stack.getItem() instanceof ItemArmor || stack.getItem() instanceof ItemTool ||
                stack.getItem() instanceof ItemSword || stack.getItem() instanceof ItemBow ||
                stack.getItem() == Items.golden_apple) {

            // Sprawdzamy statystyki tylko dla ekwipunku (nie wyrzucamy przez nazwę)
        } else {
            // Dopiero teraz sprawdzamy nazwę (śmieci)
            if (isJunk(stack)) return true;
        }

        if (stack.getItem() instanceof ItemPotion) {
            if (isBadPotion(stack)) return true;
        }

        // Limit bloków
        if (stack.getItem() instanceof ItemBlock) {
            if (countTotalBlocks() > 128) return true;
        }

        // --- GORSZA ZBROJA ---
        if (stack.getItem() instanceof ItemArmor) {
            ItemArmor armor = (ItemArmor) stack.getItem();
            int type = armor.armorType;
            ItemStack equipped = mc.thePlayer.inventoryContainer.getSlot(5 + type).getStack();
            float equippedScore = getProtection(equipped);
            float thisScore = getProtection(stack);

            if (equippedScore >= thisScore) return true;
        }

        // --- GORSZE NARZĘDZIA ---
        if (stack.getItem() instanceof ItemSword) {
            if (currentSlotId != getBestItemSlot(ItemSword.class)) return true;
        }
        if (stack.getItem() instanceof ItemPickaxe) {
            if (currentSlotId != getBestItemSlot(ItemPickaxe.class)) return true;
        }
        if (stack.getItem() instanceof ItemAxe) {
            if (currentSlotId != getBestItemSlot(ItemAxe.class)) return true;
        }

        return false;
    }

    public static boolean isBadPotion(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemPotion)) return false;
        ItemPotion potion = (ItemPotion) stack.getItem();
        if (!ItemPotion.isSplash(stack.getMetadata())) return true; // Wyrzuć pitne

        List<PotionEffect> effects = potion.getEffects(stack);
        if (effects == null || effects.isEmpty()) return true;

        for (PotionEffect effect : effects) {
            int id = effect.getPotionID();
            // Speed, Strength, Jump, Regen, FireRes, Invis, Absorption
            if (id == 1 || id == 5 || id == 8 || id == 10 || id == 12 || id == 14 || id == 22) {
                return false;
            }
        }
        return true;
    }

    private boolean equipBestArmor(int armorSlotId, int type) {
        float currentProt = -1;
        ItemStack current = mc.thePlayer.inventoryContainer.getSlot(armorSlotId).getStack();
        if (current != null && current.getItem() instanceof ItemArmor) {
            currentProt = getProtection(current);
        }

        int bestSlot = -1;
        float bestProt = currentProt;

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

        if (bestSlot != -1) {
            mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, bestSlot, 0, 1, mc.thePlayer);
            return true;
        }
        return false;
    }

    private boolean sortItem(int bestSlot, int hotbarNum) {
        int targetSlot = 36 + hotbarNum;
        if (bestSlot != -1 && bestSlot != targetSlot) {
            swap(bestSlot, hotbarNum);
            timer.reset();
            return true;
        }
        return false;
    }

    private void drop(int slot) {
        mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, slot, 1, 4, mc.thePlayer);
    }

    private void swap(int slot, int hotbarNum) {
        mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, slot, hotbarNum, 2, mc.thePlayer);
    }

    private int getBestItemSlot(Class<? extends Item> itemClass) {
        int bestSlot = -1;
        float bestScore = -1;

        for (int i = 9; i < 45; i++) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null && itemClass.isInstance(stack.getItem())) {
                float score = 0;
                if (stack.getItem() instanceof ItemSword) score = getDamage(stack);
                else if (stack.getItem() instanceof ItemTool) score = getToolSpeed(stack);
                else if (stack.getItem() instanceof ItemBow) score = getBowScore(stack);

                if (score > bestScore) {
                    bestScore = score;
                    bestSlot = i;
                }
            }
        }
        return bestSlot;
    }

    public static float getProtection(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemArmor)) return 0;
        ItemArmor armor = (ItemArmor) stack.getItem();
        float prot = armor.damageReduceAmount;
        prot += EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack) * 1.25f;
        prot += EnchantmentHelper.getEnchantmentLevel(Enchantment.blastProtection.effectId, stack) * 0.5f;
        prot += EnchantmentHelper.getEnchantmentLevel(Enchantment.fireProtection.effectId, stack) * 0.5f;
        prot += EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack) * 0.05f;
        return prot;
    }

    public static float getDamage(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemSword)) return 0;
        float dmg = 4 + ((ItemSword) stack.getItem()).getDamageVsEntity();
        dmg += EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack) * 1.25f;
        dmg += EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, stack) * 0.5f;
        return dmg;
    }

    public static float getToolSpeed(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemTool)) return 0;
        float speed = ((ItemTool) stack.getItem()).getToolMaterial().getEfficiencyOnProperMaterial();
        speed += EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, stack) * 0.75f;
        return speed;
    }

    public static float getBowScore(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemBow)) return 0;
        float score = 1;
        score += EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, stack) * 1.0f;
        score += EnchantmentHelper.getEnchantmentLevel(Enchantment.punch.effectId, stack) * 0.5f;
        return score;
    }

    public static boolean isJunk(ItemStack stack) {
        // Zabezpieczenie przed wyrzucaniem zbroi/broni przez nazwę
        if (stack.getItem() instanceof ItemArmor || stack.getItem() instanceof ItemTool ||
                stack.getItem() instanceof ItemSword || stack.getItem() instanceof ItemBow) {
            return false;
        }

        for (String junk : junkList) {
            if (stack.getItem().getUnlocalizedName().toLowerCase().contains(junk)) return true;
        }
        return false;
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

    private int findBlockSlot() {
        for (int i = 9; i < 45; i++) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null && stack.getItem() instanceof ItemBlock && !isJunk(stack)) {
                return i;
            }
        }
        return -1;
    }

    private int countTotalBlocks() {
        int count = 0;
        for (int i = 9; i < 45; i++) {
            ItemStack s = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (s != null && s.getItem() instanceof ItemBlock && !isJunk(s)) {
                count += s.stackSize;
            }
        }
        return count;
    }

    public static float getBestDamageScore() {
        float best = -1;
        for (int i = 9; i < 45; i++) {
            ItemStack stack = Minecraft.getMinecraft().thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null && stack.getItem() instanceof ItemSword) {
                float score = getDamage(stack);
                if (score > best) best = score;
            }
        }
        return best;
    }

    public static float getBestToolScore(Item type) {
        float best = -1;
        for (int i = 9; i < 45; i++) {
            ItemStack stack = Minecraft.getMinecraft().thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null && stack.getItem() == type) {
                float score = getToolSpeed(stack);
                if (score > best) best = score;
            }
        }
        return best;
    }
}