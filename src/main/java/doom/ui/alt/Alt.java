package doom.ui.alt;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;

public class Alt {
    private String username;
    private String email;
    private String password;
    private String refreshToken; // <--- NOWE POLE
    private AltType type;
    private Status status = Status.Unchecked;

    public Alt(String username, AltType type) {
        this(username, "", type, null);
    }

    // Konstruktor dla Microsoftu z Tokenem
    public Alt(String username, String refreshToken) {
        this(username, "", Alt.AltType.MICROSOFT, refreshToken);
    }

    public Alt(String username, String password, AltType type, String refreshToken) {
        this.username = username;
        this.email = username;
        this.password = password;
        this.type = type;
        this.refreshToken = refreshToken;
    }

    public void login() {
        if (type == AltType.OFFLINE) {
            Minecraft.getMinecraft().session = new Session(username, "", "", "legacy");
            status = Status.Working;
        } else if (type == AltType.MICROSOFT) {
            // Teraz to obsłużymy w GuiAltManager za pomocą MicrosoftLogin.loginWithRefreshToken
            // Ta metoda służy tylko do ustawienia statusu
            status = Status.Unchecked;
        }
    }

    public String getUsername() { return username; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String token) { this.refreshToken = token; }
    public AltType getType() { return type; }
    public void setStatus(Status status) { this.status = status; }
    public Status getStatus() { return status; }

    public enum AltType { OFFLINE, MICROSOFT }

    public enum Status {
        Working("§aActive"),
        Banned("§cBanned"),
        Unchecked("§eIdle");

        String name;
        Status(String name) { this.name = name; }
        public String toFormatted() { return name; }
    }
}