package doom.module.impl.combat;

import doom.Client;
import doom.module.Category;
import doom.module.Module;
import doom.settings.impl.BooleanSetting;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import java.util.UUID;
import java.util.regex.Pattern;

public class AntiBot extends Module {

    // Ustawienia detekcji
    public BooleanSetting tabCheck;
    public BooleanSetting entityIDCheck;
    public BooleanSetting colorCheck;
    public BooleanSetting uuidCheck;
    public BooleanSetting invisibleCheck;

    // Regex do sprawdzania poprawnych nazw graczy (a-z, A-Z, 0-9, _)
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");

    public AntiBot() {
        super("AntiBot", 0, Category.COMBAT);

        tabCheck = new BooleanSetting("Tab Check", this, true);
        entityIDCheck = new BooleanSetting("EntityID Check", this, true);
        colorCheck = new BooleanSetting("Color Check", this, true);
        uuidCheck = new BooleanSetting("UUID Check", this, true);
        invisibleCheck = new BooleanSetting("Invis Check", this, true);

        Client.INSTANCE.settingsManager.rSetting(tabCheck);
        Client.INSTANCE.settingsManager.rSetting(entityIDCheck);
        Client.INSTANCE.settingsManager.rSetting(colorCheck);
        Client.INSTANCE.settingsManager.rSetting(uuidCheck);
        Client.INSTANCE.settingsManager.rSetting(invisibleCheck);
    }

    /**
     * GŁÓWNA METODA SPRAWDZAJĄCA
     * Zwraca true, jeśli encja jest botem.
     */
    public boolean isBot(EntityPlayer player) {
        if (!this.isToggled()) return false;
        if (player == mc.thePlayer) return false;

        // 1. Sprawdzanie TabListy (Najskuteczniejsze na Watchdoga/Matrixa)
        // Boty często są spawnowane tylko w świecie, ale serwer nie wysyła info o nich w liście graczy.
        if (tabCheck.isEnabled() && !isInTab(player)) {
            return true;
        }

        // 2. Sprawdzanie Entity ID (Hypixel Watchdog)
        // Boty Watchdoga często mają bardzo wysokie lub ujemne ID, żeby nie kolidować z normalnymi graczami.
        if (entityIDCheck.isEnabled()) {
            if (player.getEntityId() >= 1000000000 || player.getEntityId() <= -1) {
                return true;
            }
        }

        // 3. Sprawdzanie nazwy (Color Codes)
        // Jeśli nazwa zawiera "§" (paragraf) i nie jest to NPC z pluginu Citizens (które czasem mają),
        // to często jest to bot antycheata udający gracza.
        if (colorCheck.isEnabled()) {
            if (player.getDisplayName().getFormattedText().startsWith("§") && !player.getName().contains("§")) {
                // Sprawdzamy czy "czysta" nazwa pasuje do regexa Minecrafta
                if (!NAME_PATTERN.matcher(player.getName()).matches()) {
                    return true;
                }
            }
        }

        // 4. UUID Check (Advanced)
        // Boty często mają UUID w wersji 2 lub losowe, gracze mają wersję 4 (Mojang) lub 3 (Offline).
        if (uuidCheck.isEnabled()) {
            if (isInvalidUUID(player)) {
                return true;
            }
        }

        // 5. Invisible Check
        // Niektóre boty są niewidzialne i latają nad głową (Watchdog)
        if (invisibleCheck.isEnabled()) {
            if (player.isInvisible() && player.posY > mc.thePlayer.posY + 1.0) {
                return true;
            }
        }

        return false;
    }

    // --- Metody pomocnicze ---

    private boolean isInTab(EntityPlayer player) {
        if (mc.getNetHandler() == null) return false;
        for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            if (info.getGameProfile() != null && info.getGameProfile().getName().equals(player.getName())) {
                return true;
            }
        }
        return false;
    }

    private boolean isInvalidUUID(EntityPlayer player) {
        UUID uuid = player.getUniqueID();
        // Sprawdzenie czy UUID jest "Offline mode" (v3) lub "Online mode" (v4)
        // Boty czasem mają dziwne UUID
        return uuid.version() != 4 && uuid.version() != 3;
    }
}