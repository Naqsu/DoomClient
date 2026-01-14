package doom.module.impl.combat;

import doom.Client;
import doom.module.Module;
import doom.settings.impl.BooleanSetting;
import net.minecraft.client.network.NetworkPlayerInfo;
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
        // Jeśli moduł wyłączony lub sprawdzamy siebie - nie jest botem
        if (!this.isToggled()) return false;
        if (player == null || player == mc.thePlayer) return false;

        // --- ZABEZPIECZENIE PRZED CRASHEM PRZY ZMIANIE SERWERA ---
        // Jeśli gra ładuje świat, ignorujemy wszystkie checki (zwracamy false - nie jest botem)
        if (mc.getNetHandler() == null) return false;

        // Jeśli gracz nie ma jeszcze UUID (ładuje się), to nie jest botem
        if (player.getUniqueID() == null) return false;

        // 1. Sprawdzanie TabListy (POPRAWIONE)
        if (tabCheck.isEnabled()) {
            // Jeśli NIE MA go w tabie -> to BOT
            if (!isInTab(player)) {
                return true;
            }
        }

        // 2. Sprawdzanie Entity ID (Hypixel Watchdog)
        if (entityIDCheck.isEnabled()) {
            if (player.getEntityId() >= 1000000000 || player.getEntityId() <= -1) {
                return true;
            }
        }

        // 3. Sprawdzanie nazwy (Color Codes)
        if (colorCheck.isEnabled()) {
            if (player.getName() != null && player.getDisplayName() != null) {
                String formatted = player.getDisplayName().getFormattedText();
                String cleanName = player.getName();

                if (formatted.startsWith("§") && !cleanName.contains("§")) {
                    if (!NAME_PATTERN.matcher(cleanName).matches()) {
                        return true;
                    }
                }
            }
        }

        // 4. UUID Check
        if (uuidCheck.isEnabled()) {
            if (isInvalidUUID(player)) {
                return true;
            }
        }

        // 5. Invisible Check
        if (invisibleCheck.isEnabled()) {
            if (player.isInvisible() && player.posY > mc.thePlayer.posY + 1.0) {
                return true;
            }
        }

        return false;
    }

    // --- Metody pomocnicze ---

    private boolean isInTab(EntityPlayer player) {
        // CRITICAL FIX:
        // Jeśli NetHandler jest null (zmiana świata), zwracamy TRUE.
        // Oznacza to "Zakładamy, że jest w tabie, żeby go nie usunąć".
        // Wcześniej zwracałeś FALSE -> kod myślał że to bot -> próbował coś robić -> CRASH.
        if (mc.getNetHandler() == null) return true;

        // Pobieramy info bezpośrednio z mapy (bez pętli!)
        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(player.getUniqueID());

        // Sprawdzamy czy info istnieje
        return info != null && info.getGameProfile() != null;
    }

    private boolean isInvalidUUID(EntityPlayer player) {
        UUID uuid = player.getUniqueID();
        if (uuid == null) return false; // Bezpiecznik
        return uuid.version() != 4 && uuid.version() != 3;
    }
}