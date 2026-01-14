package doom.module.impl.misc;

import doom.Client;
import doom.event.EventTarget;
import doom.event.impl.EventPacket;
import doom.module.Module;
import doom.util.TimeHelper;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;

import java.util.ArrayList;
import java.util.List;

public class AnticheatDetector extends Module {

    private final List<Short> transactions = new ArrayList<>();
    private final TimeHelper lastPacketTimer = new TimeHelper();
    private final int CHAT_ID = 133769;
    private boolean detectionDisplayed = false;

    public AnticheatDetector() {
        super("AnticheatDetector", 0, Category.MISC);
    }

    @Override
    public void onEnable() {
        reset();
    }

    private void reset() {
        transactions.clear();
        detectionDisplayed = false;
        lastPacketTimer.reset();
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (event.getDirection() == EventPacket.Direction.RECEIVE) {

            if (event.getPacket() instanceof S01PacketJoinGame || event.getPacket() instanceof S07PacketRespawn) {
                reset();
                return;
            }

            if (event.getPacket() instanceof S32PacketConfirmTransaction) {
                S32PacketConfirmTransaction packet = (S32PacketConfirmTransaction) event.getPacket();
                short id = packet.getActionNumber();

                // Auto-Reset
                if (lastPacketTimer.hasReached(2000)) {
                    transactions.clear();
                    detectionDisplayed = false;
                }
                lastPacketTimer.reset();

                if (detectionDisplayed) return;

                transactions.add(id);
                updateStatus();

                if (transactions.size() >= 5) {
                    checkAntiCheat();
                    detectionDisplayed = true;
                }
            }
        }
    }

    private void updateStatus() {
        int count = Math.min(transactions.size(), 5);
        StringBuilder bar = new StringBuilder("§8[");
        for (int i = 0; i < 5; i++) {
            bar.append(i < count ? "§a=" : "§7.");
        }
        bar.append("§8]");

        String lastId = transactions.isEmpty() ? "?" : String.valueOf(transactions.get(transactions.size() - 1));

        try {
            Client.addChatMessageWithId("Scanning AC... " + bar.toString() + " §7ID: §e" + lastId, CHAT_ID);
        } catch (Exception e) {
            Client.addChatMessage("Scanning AC... " + bar.toString());
        }
    }

    private void checkAntiCheat() {
        String serverIP = mc.getCurrentServerData() != null ? mc.getCurrentServerData().serverIP : "";
        String detectedAC = guessAntiCheat(serverIP);

        if (detectedAC != null && !detectedAC.equals("Unknown")) {
            Client.addChatMessageWithId("§aDetected AntiCheat: §e" + detectedAC, CHAT_ID);
        } else {
            // Debug info dla Ciebie w razie innych serwerów
            StringBuilder sb = new StringBuilder();
            int limit = Math.min(transactions.size(), 5);
            for(int i=0; i<limit; i++) sb.append(transactions.get(i)).append(", ");
            Client.addChatMessageWithId("§cUnknown AC. IDs: §7" + sb.toString(), CHAT_ID);
        }
    }

    private String guessAntiCheat(String address) {
        if (transactions.size() < 3) return null;

        List<Integer> diffs = new ArrayList<>();
        // Pomijamy pierwszy pakiet (często śmieć, np. -11391 w Twoim logu), analizujemy resztę
        // Zaczynamy od i=2, czyli sprawdzamy różnicę między 3. a 2. pakietem itd.
        // Jeśli lista ma 5 elementów: 0, 1, 2, 3, 4. Sprawdzamy różnice (2-1), (3-2), (4-3).

        // Czasami pierwszy pakiet (index 0) to inventory transaction, a AC zaczyna się od index 1.
        // Dlatego bezpieczniej analizować "ciągłość" w dowolnym miejscu listy.

        boolean sequenceFound = false;
        int consistentDiff = 0;
        short startVal = 0;

        for (int i = 0; i < transactions.size() - 1; i++) {
            int diff = transactions.get(i+1) - transactions.get(i);

            // Szukamy sekwencji co najmniej 3 pakietów z tą samą różnicą (np. +1 lub -1)
            if (i > 0) {
                int prevDiff = transactions.get(i) - transactions.get(i-1);
                if (diff == prevDiff && Math.abs(diff) == 1) {
                    sequenceFound = true;
                    consistentDiff = diff;
                    startVal = transactions.get(i); // Bierzemy przykładową wartość z sekwencji
                    break;
                }
            }
        }

        if (address != null && address.toLowerCase().endsWith("hypixel.net")) return "Watchdog";

        if (sequenceFound) {
            if (consistentDiff == 1) {
                // Vulcan (Rosnące ujemne)
                // Twoje logi: -32347, -32346...
                if (startVal < -20000) return "Vulcan";
                if (startVal > -20000 && startVal < -10000) return "Vulcan / Verus";

                // Matrix (Okolice -20000 lub +100)
                if (startVal > -20005 && startVal < -19995) return "Matrix";

                return "Verus"; // Domyślnie rosnące to Verus
            } else if (consistentDiff == -1) {
                if (startVal > -10 && startVal <= 0) return "Grim";
                return "Polar / Karhu";
            }
        }

        // Specyficzne dla Vulcana (gdy nie wykryto sekwencji, ale wartości pasują)
        // Twoje wartości są w okolicach -32000.
        short last = transactions.get(transactions.size() - 1);
        if (last < -30000 && last > -33000) {
            return "Vulcan";
        }

        return "Unknown";
    }
}