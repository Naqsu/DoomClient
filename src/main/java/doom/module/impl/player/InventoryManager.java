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

public class InventoryManager extends Module {

    private final NumberSetting delay = new NumberSetting("Delay", this, 80, 0, 500, 10);
    private final BooleanSetting invOpenOnly = new BooleanSetting("Inv Open Only", this, true);
    private final BooleanSetting autoArmor = new BooleanSetting("Auto Armor", this, true);
    private final BooleanSetting clean = new BooleanSetting("Cleaner", this, true);
    private final BooleanSetting sort = new BooleanSetting("Sorter", this, true);

    private final TimeHelper timer = new TimeHelper();

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

        // 1. AUTO ARMOR
        if (autoArmor.isEnabled()) {
            for (int i = 5; i < 9; i++) {
                int armorType = i - 5;
                if (equipBestArmor(i, armorType)) {
                    timer.reset();
                    return;
                }
            }
        }

        // 2. CLEANER
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

        // 3. SORTER
        if (sort.isEnabled()) {
            if (sortItem(getBestItemSlot(ItemSword.class), 36)) return;
            if (sortItem(findItem(Items.golden_apple), 37)) return;
            if (sortItem(findItem(Items.ender_pearl), 38)) return;

            int blockSlot1 = findBlockSlot();
            if (sortItem(blockSlot1, 39)) return;

            int blockSlot2 = findBlockSlotExcluding(39);
            if (sortItem(blockSlot2, 40)) return;
        }
    }

    private boolean isTrash(ItemStack stack, int slotId) {
        Item item = stack.getItem();

        if (item instanceof ItemSword) return slotId != getBestItemSlot(ItemSword.class);

        if (item instanceof ItemArmor) {
            ItemArmor armor = (ItemArmor) item;
            int type = armor.armorType;
            ItemStack equipped = mc.thePlayer.inventoryContainer.getSlot(5 + type).getStack();
            float equippedProt = getProtection(equipped);
            float thisProt = getProtection(stack);
            return thisProt <= equippedProt;
        }

        if (item == Items.golden_apple || item == Items.ender_pearl) return false;

        if (item == Items.water_bucket) return countItem(Items.water_bucket, slotId) > 0;

        if (item instanceof ItemBlock) return countTotalBlocks() > 192;

        if (item instanceof ItemPickaxe) return slotId != getBestItemSlot(ItemPickaxe.class);
        if (item instanceof ItemAxe) return slotId != getBestItemSlot(ItemAxe.class);

        return true;
    }

    private boolean sortItem(int bestSlot, int targetSlot) {
        if (bestSlot != -1 && bestSlot != targetSlot) {
            mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, bestSlot, targetSlot - 36, 2, mc.thePlayer);
            timer.reset();
            return true;
        }
        return false;
    }

    private void drop(int slot) {
        mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, slot, 1, 4, mc.thePlayer);
    }

    private int findItem(Item item) {
        for (int i = 9; i < 45; i++) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null && stack.getItem() == item) return i;
        }
        return -1;
    }

    private int findBlockSlot() {
        int bestSlot = -1;
        int maxStack = 0;
        for (int i = 9; i < 45; i++) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null && stack.getItem() instanceof ItemBlock) {
                if (stack.stackSize > maxStack) {
                    maxStack = stack.stackSize;
                    bestSlot = i;
                }
            }
        }
        return bestSlot;
    }

    private int findBlockSlotExcluding(int excludeSlot) {
        int bestSlot = -1;
        int maxStack = 0;
        for (int i = 9; i < 45; i++) {
            if (i == excludeSlot) continue;
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null && stack.getItem() instanceof ItemBlock) {
                if (stack.stackSize > maxStack) {
                    maxStack = stack.stackSize;
                    bestSlot = i;
                }
            }
        }
        return bestSlot;
    }

    private int countTotalBlocks() {
        int count = 0;
        for (int i = 9; i < 45; i++) {
            ItemStack s = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (s != null && s.getItem() instanceof ItemBlock) count += s.stackSize;
        }
        return count;
    }

    private int countItem(Item item, int upToSlot) {
        int count = 0;
        for (int i = 9; i < upToSlot; i++) {
            ItemStack s = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (s != null && s.getItem() == item) count++;
        }
        return count;
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

                if (score > bestScore) {
                    bestScore = score;
                    bestSlot = i;
                }
            }
        }
        return bestSlot;
    }

    private boolean equipBestArmor(int armorSlotId, int type) {
        float currentProt = -1;
        ItemStack current = mc.thePlayer.inventoryContainer.getSlot(armorSlotId).getStack();
        if (current != null && current.getItem() instanceof ItemArmor) currentProt = getProtection(current);

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

    // --- METODY STATYCZNE (DLA CHEST STEALERA) ---

    public static float getProtection(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemArmor)) return 0;
        ItemArmor armor = (ItemArmor) stack.getItem();
        float prot = armor.damageReduceAmount;
        prot += EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack) * 1.25f;
        prot += EnchantmentHelper.getEnchantmentLevel(Enchantment.blastProtection.effectId, stack) * 0.5f;
        prot += EnchantmentHelper.getEnchantmentLevel(Enchantment.fireProtection.effectId, stack) * 0.5f;
        prot += EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack) * 0.01f;
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

    // --- NOWE METODY STATYCZNE (NAPRAWIAJĄ BŁĄD KOMPILACJI) ---

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

    public static float getBestToolScore(Item targetItem) {
        float best = -1;
        for (int i = 9; i < 45; i++) {
            ItemStack stack = Minecraft.getMinecraft().thePlayer.inventoryContainer.getSlot(i).getStack();
            // Sprawdzamy czy to ta sama klasa narzędzia (np. czy to kilof, jeśli szukamy kilofa)
            if (stack != null && stack.getItem().getClass() == targetItem.getClass()) {
                float score = getToolSpeed(stack);
                if (score > best) best = score;
            }
        }
        return best;
    }
}