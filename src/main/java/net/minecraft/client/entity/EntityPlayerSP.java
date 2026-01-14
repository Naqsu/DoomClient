package net.minecraft.client.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.network.play.client.*;
import net.minecraft.potion.Potion;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.util.*;
import net.minecraft.world.World;

public class EntityPlayerSP extends AbstractClientPlayer {
    public final NetHandlerPlayClient sendQueue;
    private final StatFileWriter statWriter;
    private double lastReportedPosX;
    private double lastReportedPosY;
    private double lastReportedPosZ;
    private float lastReportedYaw;
    private float lastReportedPitch;
    private boolean serverSneakState;
    private boolean serverSprintState;
    private int positionUpdateTicks;
    private boolean hasValidHealth;
    private String clientBrand;
    public MovementInput movementInput;
    protected Minecraft mc;
    protected int sprintToggleTimer;
    public int sprintingTicksLeft;
    public float renderArmYaw;
    public float renderArmPitch;
    public float prevRenderArmYaw;
    public float prevRenderArmPitch;
    private int horseJumpPowerCounter;
    private float horseJumpPower;
    public float timeInPortal;
    public float prevTimeInPortal;

    public EntityPlayerSP(Minecraft mcIn, World worldIn, NetHandlerPlayClient netHandler, StatFileWriter statFile) {
        super(worldIn, netHandler.getGameProfile());
        this.sendQueue = netHandler;
        this.statWriter = statFile;
        this.mc = mcIn;
        this.dimension = 0;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) { return false; }
    @Override
    public void heal(float healAmount) {}

    @Override
    public void onUpdate() {
        if (this.worldObj.isBlockLoaded(new BlockPos(this.posX, 0.0D, this.posZ))) {

            this.movementInput.updatePlayerMoveState();

            doom.event.impl.EventUpdate event = new doom.event.impl.EventUpdate(
                    this.rotationYaw, this.rotationPitch, this.onGround,
                    this.movementInput.moveForward, this.movementInput.moveStrafe,
                    this.movementInput.jump, this.movementInput.sneak, this.isSprinting()
            );
            doom.event.EventManager.call(event);

            // Zapisujemy oryginalne dane
            float realForward = this.movementInput.moveForward;
            float realStrafe = this.movementInput.moveStrafe;
            float realYaw = this.rotationYaw;
            float realPitch = this.rotationPitch;

            // --- POPRAWKA: USUNĄŁEM LINIJKĘ "isRotating = event != rotation" ---
            // Teraz moduły same decydują kiedy włączyć MoveFixa.

            // Jeśli moduł ustawił flagę isRotating -> Aplikujemy MoveFix
            if (doom.util.RotationUtil.isRotating) {
                // Pobieramy kąt, który ustawił moduł (np. Scaffold)
                float targetYaw = doom.util.RotationUtil.targetYaw;

                // Obliczamy poprawne inputy (np. zamiana W na S)
                float[] fixed = doom.util.MoveUtil.getFixedInput(
                        this.rotationYaw,
                        targetYaw,
                        realForward,
                        realStrafe
                );

                // Aplikujemy do fizyki
                this.movementInput.moveForward = fixed[0];
                this.movementInput.moveStrafe = fixed[1];

                // Aplikujemy do pakietu
                event.setMoveForward(fixed[0]);
                event.setMoveStrafe(fixed[1]);
            }

            // Fizyka
            super.onUpdate();

            // Przywracanie
            this.movementInput.moveForward = realForward;
            this.movementInput.moveStrafe = realStrafe;
            this.rotationYaw = realYaw;
            this.rotationPitch = realPitch;

            if (!event.isGroundSpoofed()) {
                // ...to aktualizujemy stan zgodnie z fizyką gry.
                // Dzięki temu przy skoku onGround będzie false -> brak flagi Jump (A).
                event.setOnGround(this.onGround);
            }
            // Jeśli NoFall ustawił onGround na true, isGroundSpoofed będzie true,
            // więc powyższy if się nie wykona i zachowamy "oszukany" stan.

            event.setSprinting(this.isSprinting());
            event.setSneaking(this.isSneaking());

            this.rotationYawHead = event.getYaw();
            this.renderYawOffset = event.getYaw();

            this.onUpdateWalkingPlayer(event);
        }
    }


    public void onUpdateWalkingPlayer(doom.event.impl.EventUpdate event) {

        // 1. SYNC SERVER STATE (Sprint/Sneak)
        // We use the state from the Event/MoveUtil, not the raw player state
        boolean eventSprint = event.isSprinting();
        if (eventSprint != this.serverSprintState) {
            if (eventSprint) {
                this.sendQueue.addToSendQueue(new C0BPacketEntityAction(this, C0BPacketEntityAction.Action.START_SPRINTING));
            } else {
                this.sendQueue.addToSendQueue(new C0BPacketEntityAction(this, C0BPacketEntityAction.Action.STOP_SPRINTING));
            }
            this.serverSprintState = eventSprint;
        }

        boolean eventSneak = event.isSneaking();
        if (eventSneak != this.serverSneakState) {
            if (eventSneak) {
                this.sendQueue.addToSendQueue(new C0BPacketEntityAction(this, C0BPacketEntityAction.Action.START_SNEAKING));
            } else {
                this.sendQueue.addToSendQueue(new C0BPacketEntityAction(this, C0BPacketEntityAction.Action.STOP_SNEAKING));
            }
            this.serverSneakState = eventSneak;
        }

        // 2. SEND MOTION PACKETS
        if (this.isCurrentViewEntity()) {
            double d0 = this.posX - this.lastReportedPosX;
            double d1 = this.getEntityBoundingBox().minY - this.lastReportedPosY;
            double d2 = this.posZ - this.lastReportedPosZ;

            // Calculate rotation difference using EVENT Data (Server Yaw)
            double d3 = (double)(event.getYaw() - this.lastReportedYaw);
            double d4 = (double)(event.getPitch() - this.lastReportedPitch);

            boolean isMoving = d0 * d0 + d1 * d1 + d2 * d2 > 9.0E-4D || this.positionUpdateTicks >= 20;
            boolean isRotating = d3 != 0.0D || d4 != 0.0D;

            if (this.ridingEntity == null) {
                // Send packets using EVENT Data (Fixed Inputs + Server Rotation)
                if (isMoving && isRotating) {
                    this.sendQueue.addToSendQueue(new C03PacketPlayer.C06PacketPlayerPosLook(this.posX, this.getEntityBoundingBox().minY, this.posZ, event.getYaw(), event.getPitch(), event.isOnGround()));
                } else if (isMoving) {
                    this.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(this.posX, this.getEntityBoundingBox().minY, this.posZ, event.isOnGround()));
                } else if (isRotating) {
                    this.sendQueue.addToSendQueue(new C03PacketPlayer.C05PacketPlayerLook(event.getYaw(), event.getPitch(), event.isOnGround()));
                } else {
                    this.sendQueue.addToSendQueue(new C03PacketPlayer(event.isOnGround()));
                }
            } else {
                // Riding logic
                this.sendQueue.addToSendQueue(new C03PacketPlayer.C06PacketPlayerPosLook(this.motionX, -999.0D, this.motionZ, event.getYaw(), event.getPitch(), event.isOnGround()));
                isMoving = false;
            }

            ++this.positionUpdateTicks;
            if (isMoving) {
                this.lastReportedPosX = this.posX;
                this.lastReportedPosY = this.getEntityBoundingBox().minY;
                this.lastReportedPosZ = this.posZ;
                this.positionUpdateTicks = 0;
            }
            if (isRotating) {
                this.lastReportedYaw = event.getYaw();
                this.lastReportedPitch = event.getPitch();
            }
        }

        // 3. POST UPDATE EVENT (For Killaura Blocking)
        doom.event.EventManager.call(new doom.event.impl.EventPostUpdate());
    }



    // --- Standard MCP Methods (Required for compilation) ---
    // I removed the content to save space, but you must keep the signatures.
    // Ensure you copy-paste the methods from your original file or the ones provided in previous steps
    // that implement dropOneItem, joinEntityItemWithWorld, sendChatMessage etc.
    public EntityItem dropOneItem(boolean dropAll) { this.sendQueue.addToSendQueue(new C07PacketPlayerDigging(dropAll ? C07PacketPlayerDigging.Action.DROP_ALL_ITEMS : C07PacketPlayerDigging.Action.DROP_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
        return null;
    }
    public void joinEntityItemWithWorld(net.minecraft.entity.item.EntityItem itemIn) {}
    public void sendChatMessage(String message) {
        doom.event.impl.EventChat event = new doom.event.impl.EventChat(message);
        event.call();
        if (!event.isCancelled()) this.sendQueue.addToSendQueue(new C01PacketChatMessage(message));
    }
    public void swingItem() { super.swingItem(); this.sendQueue.addToSendQueue(new C0APacketAnimation()); }
    public void respawnPlayer() { this.sendQueue.addToSendQueue(new C16PacketClientStatus(C16PacketClientStatus.EnumState.PERFORM_RESPAWN)); }
    protected void damageEntity(DamageSource damageSrc, float damageAmount) { if (!this.isEntityInvulnerable(damageSrc)) this.setHealth(this.getHealth() - damageAmount); }
    public void closeScreen() { this.sendQueue.addToSendQueue(new C0DPacketCloseWindow(this.openContainer.windowId)); this.closeScreenAndDropStack(); }
    public void closeScreenAndDropStack() { this.inventory.setItemStack((net.minecraft.item.ItemStack)null); super.closeScreen(); this.mc.displayGuiScreen(null); }
    public void setPlayerSPHealth(float health) { if (this.hasValidHealth) { float f = this.getHealth() - health; if (f <= 0.0F) { this.setHealth(health); if (f < 0.0F) this.hurtResistantTime = this.maxHurtResistantTime / 2; } else { this.lastDamage = f; this.setHealth(this.getHealth()); this.hurtResistantTime = this.maxHurtResistantTime; this.damageEntity(DamageSource.generic, f); this.hurtTime = this.maxHurtTime = 10; } } else { this.setHealth(health); this.hasValidHealth = true; } }
    public void addStat(net.minecraft.stats.StatBase stat, int amount) { if (stat != null && stat.isIndependent) super.addStat(stat, amount); }
    public void sendPlayerAbilities() { this.sendQueue.addToSendQueue(new C13PacketPlayerAbilities(this.capabilities)); }
    public boolean isUser() { return true; }
    protected void sendHorseJump() { this.sendQueue.addToSendQueue(new C0BPacketEntityAction(this, C0BPacketEntityAction.Action.RIDING_JUMP, (int)(this.getHorseJumpPower() * 100.0F))); }
    public void sendHorseInventory() { this.sendQueue.addToSendQueue(new C0BPacketEntityAction(this, C0BPacketEntityAction.Action.OPEN_INVENTORY)); }
    public void setClientBrand(String brand) { this.clientBrand = brand; }
    public String getClientBrand() { return this.clientBrand; }
    public StatFileWriter getStatFileWriter() { return this.statWriter; }
    public void addChatComponentMessage(net.minecraft.util.IChatComponent chatComponent) { this.mc.ingameGUI.getChatGUI().printChatMessage(chatComponent); }
    protected boolean pushOutOfBlocks(double x, double y, double z) {
        if (this.noClip) return false;
        BlockPos blockpos = new BlockPos(x, y, z);
        double d0 = x - (double)blockpos.getX();
        double d1 = z - (double)blockpos.getZ();
        if (!this.isOpenBlockSpace(blockpos)) {
            int i = -1; double d2 = 9999.0D;
            if (this.isOpenBlockSpace(blockpos.west()) && d0 < d2) { d2 = d0; i = 0; }
            if (this.isOpenBlockSpace(blockpos.east()) && 1.0D - d0 < d2) { d2 = 1.0D - d0; i = 1; }
            if (this.isOpenBlockSpace(blockpos.north()) && d1 < d2) { d2 = d1; i = 4; }
            if (this.isOpenBlockSpace(blockpos.south()) && 1.0D - d1 < d2) { d2 = 1.0D - d1; i = 5; }
            float f = 0.1F;
            if (i == 0) this.motionX = -f; if (i == 1) this.motionX = f;
            if (i == 4) this.motionZ = -f; if (i == 5) this.motionZ = f;
        }
        return false;
    }
    private boolean isOpenBlockSpace(BlockPos pos) { return !this.worldObj.getBlockState(pos).getBlock().isNormalCube() && !this.worldObj.getBlockState(pos.up()).getBlock().isNormalCube(); }
    public void setSprinting(boolean sprinting) { super.setSprinting(sprinting); this.sprintingTicksLeft = sprinting ? 600 : 0; }
    public void setXPStats(float currentXP, int maxXP, int level) { this.experience = currentXP; this.experienceTotal = maxXP; this.experienceLevel = level; }
    public void addChatMessage(net.minecraft.util.IChatComponent component) { this.mc.ingameGUI.getChatGUI().printChatMessage(component); }
    public boolean canCommandSenderUseCommand(int permLevel, String commandName) { return permLevel <= 0; }
    public BlockPos getPosition() { return new BlockPos(this.posX + 0.5D, this.posY + 0.5D, this.posZ + 0.5D); }
    public void playSound(String name, float volume, float pitch) { this.worldObj.playSound(this.posX, this.posY, this.posZ, name, volume, pitch, false); }
    public boolean isServerWorld() { return true; }
    public boolean isRidingHorse() { return this.ridingEntity != null && this.ridingEntity instanceof net.minecraft.entity.passive.EntityHorse && ((net.minecraft.entity.passive.EntityHorse)this.ridingEntity).isHorseSaddled(); }
    public float getHorseJumpPower() { return this.horseJumpPower; }
    public void openEditSign(net.minecraft.tileentity.TileEntitySign signTile) { this.mc.displayGuiScreen(new net.minecraft.client.gui.inventory.GuiEditSign(signTile)); }
    public void openEditCommandBlock(net.minecraft.command.server.CommandBlockLogic cmdBlockLogic) { this.mc.displayGuiScreen(new net.minecraft.client.gui.GuiCommandBlock(cmdBlockLogic)); }
    public void displayGUIBook(net.minecraft.item.ItemStack bookStack) { net.minecraft.item.Item item = bookStack.getItem(); if (item == net.minecraft.init.Items.writable_book) this.mc.displayGuiScreen(new net.minecraft.client.gui.GuiScreenBook(this, bookStack, true)); }
    public void displayGUIChest(net.minecraft.inventory.IInventory chestInventory) { String s = chestInventory instanceof net.minecraft.world.IInteractionObject ? ((net.minecraft.world.IInteractionObject)chestInventory).getGuiID() : "minecraft:container"; if ("minecraft:chest".equals(s)) this.mc.displayGuiScreen(new net.minecraft.client.gui.inventory.GuiChest(this.inventory, chestInventory)); else if ("minecraft:hopper".equals(s)) this.mc.displayGuiScreen(new net.minecraft.client.gui.GuiHopper(this.inventory, chestInventory)); else if ("minecraft:furnace".equals(s)) this.mc.displayGuiScreen(new net.minecraft.client.gui.inventory.GuiFurnace(this.inventory, chestInventory)); else if ("minecraft:brewing_stand".equals(s)) this.mc.displayGuiScreen(new net.minecraft.client.gui.inventory.GuiBrewingStand(this.inventory, chestInventory)); else if ("minecraft:beacon".equals(s)) this.mc.displayGuiScreen(new net.minecraft.client.gui.inventory.GuiBeacon(this.inventory, chestInventory)); else if (!"minecraft:dispenser".equals(s) && !"minecraft:dropper".equals(s)) this.mc.displayGuiScreen(new net.minecraft.client.gui.inventory.GuiChest(this.inventory, chestInventory)); else this.mc.displayGuiScreen(new net.minecraft.client.gui.inventory.GuiDispenser(this.inventory, chestInventory)); }
    public void displayGUIHorse(net.minecraft.entity.passive.EntityHorse horse, net.minecraft.inventory.IInventory horseInventory) { this.mc.displayGuiScreen(new net.minecraft.client.gui.inventory.GuiScreenHorseInventory(this.inventory, horseInventory, horse)); }
    public void displayGui(net.minecraft.world.IInteractionObject guiOwner) { String s = guiOwner.getGuiID(); if ("minecraft:crafting_table".equals(s)) this.mc.displayGuiScreen(new net.minecraft.client.gui.inventory.GuiCrafting(this.inventory, this.worldObj)); else if ("minecraft:enchanting_table".equals(s)) this.mc.displayGuiScreen(new net.minecraft.client.gui.GuiEnchantment(this.inventory, this.worldObj, guiOwner)); else if ("minecraft:anvil".equals(s)) this.mc.displayGuiScreen(new net.minecraft.client.gui.GuiRepair(this.inventory, this.worldObj)); }
    public void displayVillagerTradeGui(net.minecraft.entity.IMerchant villager) { this.mc.displayGuiScreen(new net.minecraft.client.gui.GuiMerchant(this.inventory, villager, this.worldObj)); }
    public void onCriticalHit(net.minecraft.entity.Entity entityHit) { this.mc.effectRenderer.emitParticleAtEntity(entityHit, net.minecraft.util.EnumParticleTypes.CRIT); }
    public void onEnchantmentCritical(net.minecraft.entity.Entity entityHit) { this.mc.effectRenderer.emitParticleAtEntity(entityHit, net.minecraft.util.EnumParticleTypes.CRIT_MAGIC); }
    public boolean isSneaking() { boolean flag = this.movementInput != null ? this.movementInput.sneak : false; return flag && !this.sleeping; }
    public void updateEntityActionState() { super.updateEntityActionState(); if (this.isCurrentViewEntity()) { this.moveStrafing = this.movementInput.moveStrafe; this.moveForward = this.movementInput.moveForward; this.isJumping = this.movementInput.jump; this.prevRenderArmYaw = this.renderArmYaw; this.prevRenderArmPitch = this.renderArmPitch; this.renderArmPitch = (float)((double)this.renderArmPitch + (double)(this.rotationPitch - this.renderArmPitch) * 0.5D); this.renderArmYaw = (float)((double)this.renderArmYaw + (double)(this.rotationYaw - this.renderArmYaw) * 0.5D); } }
    protected boolean isCurrentViewEntity() { return this.mc.getRenderViewEntity() == this; }
    /**
     * Called frequently so the entity can update its state every tick as required. For example, zombies and skeletons
     * use this to react to sunlight and start to burn.
     */
    @Override
    public void onLivingUpdate()
    {
        if (this.sprintingTicksLeft > 0)
        {
            --this.sprintingTicksLeft;

            if (this.sprintingTicksLeft == 0)
            {
                this.setSprinting(false);
            }
        }

        if (this.sprintToggleTimer > 0)
        {
            --this.sprintToggleTimer;
        }

        this.prevTimeInPortal = this.timeInPortal;

        if (this.inPortal)
        {
            if (this.mc.currentScreen != null && !this.mc.currentScreen.doesGuiPauseGame())
            {
                this.mc.displayGuiScreen((GuiScreen)null);
            }

            if (this.timeInPortal == 0.0F)
            {
                this.mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("portal.trigger"), this.rand.nextFloat() * 0.4F + 0.8F));
            }

            this.timeInPortal += 0.0125F;

            if (this.timeInPortal >= 1.0F)
            {
                this.timeInPortal = 1.0F;
            }

            this.inPortal = false;
        }
        else if (this.isPotionActive(Potion.confusion) && this.getActivePotionEffect(Potion.confusion).getDuration() > 60)
        {
            this.timeInPortal += 0.006666667F;

            if (this.timeInPortal > 1.0F)
            {
                this.timeInPortal = 1.0F;
            }
        }
        else
        {
            if (this.timeInPortal > 0.0F)
            {
                this.timeInPortal -= 0.05F;
            }

            if (this.timeInPortal < 0.0F)
            {
                this.timeInPortal = 0.0F;
            }
        }

        if (this.timeUntilPortal > 0)
        {
            --this.timeUntilPortal;
        }

        boolean flag = this.movementInput.jump;
        boolean flag1 = this.movementInput.sneak;
        float f = 0.8F;
        boolean flag2 = this.movementInput.moveForward >= f;

        // This updates inputs from keyboard.
        // In "Physics First" mode, we want this to happen normally so we move based on camera.
        this.movementInput.updatePlayerMoveState();

        // Custom Event Hook
        doom.event.EventManager.call(new doom.event.impl.EventLivingUpdate());

        if (this.isUsingItem() && !this.isRiding())
        {
            this.movementInput.moveStrafe *= 0.2F;
            this.movementInput.moveForward *= 0.2F;
            this.sprintToggleTimer = 0;
        }

        this.pushOutOfBlocks(this.posX - (double)this.width * 0.35D, this.getEntityBoundingBox().minY + 0.5D, this.posZ + (double)this.width * 0.35D);
        this.pushOutOfBlocks(this.posX - (double)this.width * 0.35D, this.getEntityBoundingBox().minY + 0.5D, this.posZ - (double)this.width * 0.35D);
        this.pushOutOfBlocks(this.posX + (double)this.width * 0.35D, this.getEntityBoundingBox().minY + 0.5D, this.posZ - (double)this.width * 0.35D);
        this.pushOutOfBlocks(this.posX + (double)this.width * 0.35D, this.getEntityBoundingBox().minY + 0.5D, this.posZ + (double)this.width * 0.35D);

        boolean flag3 = (float)this.getFoodStats().getFoodLevel() > 6.0F || this.capabilities.allowFlying;

        if (this.onGround && !flag1 && !flag2 && this.movementInput.moveForward >= f && !this.isSprinting() && flag3 && !this.isUsingItem() && !this.isPotionActive(Potion.blindness))
        {
            if (this.sprintToggleTimer <= 0 && !this.mc.gameSettings.keyBindSprint.isKeyDown())
            {
                this.sprintToggleTimer = 7;
            }
            else
            {
                this.setSprinting(true);
            }
        }

        if (!this.isSprinting() && this.movementInput.moveForward >= f && flag3 && !this.isUsingItem() && !this.isPotionActive(Potion.blindness) && this.mc.gameSettings.keyBindSprint.isKeyDown())
        {
            this.setSprinting(true);
        }

        boolean isRotationActive = doom.util.RotationUtil.isRotating;

        if (this.isSprinting() && (this.movementInput.moveForward < f || this.isCollidedHorizontally || !flag3))
        {
            // Wyłącz sprint TYLKO JEŚLI nie mamy aktywnej rotacji (Killaury)
            if (!isRotationActive) {
                this.setSprinting(false);
            }
        }

        if (this.capabilities.allowFlying)
        {
            if (this.mc.playerController.isSpectatorMode())
            {
                if (!this.capabilities.isFlying)
                {
                    this.capabilities.isFlying = true;
                    this.sendPlayerAbilities();
                }
            }
            else if (!flag && this.movementInput.jump)
            {
                if (this.flyToggleTimer == 0)
                {
                    this.flyToggleTimer = 7;
                }
                else
                {
                    this.capabilities.isFlying = !this.capabilities.isFlying;
                    this.sendPlayerAbilities();
                    this.flyToggleTimer = 0;
                }
            }
        }

        if (this.capabilities.isFlying && this.isCurrentViewEntity())
        {
            if (this.movementInput.sneak)
            {
                this.motionY -= (double)(this.capabilities.getFlySpeed() * 3.0F);
            }

            if (this.movementInput.jump)
            {
                this.motionY += (double)(this.capabilities.getFlySpeed() * 3.0F);
            }
        }

        if (this.isRidingHorse())
        {
            if (this.horseJumpPowerCounter < 0)
            {
                ++this.horseJumpPowerCounter;

                if (this.horseJumpPowerCounter == 0)
                {
                    this.horseJumpPower = 0.0F;
                }
            }

            if (flag && !this.movementInput.jump)
            {
                this.horseJumpPowerCounter = -10;
                this.sendHorseJump();
            }
            else if (!flag && this.movementInput.jump)
            {
                this.horseJumpPowerCounter = 0;
                this.horseJumpPower = 0.0F;
            }
            else if (flag)
            {
                ++this.horseJumpPowerCounter;

                if (this.horseJumpPowerCounter < 10)
                {
                    this.horseJumpPower = (float)this.horseJumpPowerCounter * 0.1F;
                }
                else
                {
                    this.horseJumpPower = 0.8F + 2.0F / (float)(this.horseJumpPowerCounter - 9) * 0.1F;
                }
            }
        }
        else
        {
            this.horseJumpPower = 0.0F;
        }

        super.onLivingUpdate();

        if (this.onGround && this.capabilities.isFlying && !this.mc.playerController.isSpectatorMode())
        {
            this.capabilities.isFlying = false;
            this.sendPlayerAbilities();
        }
    }
}