package doom.ui.alt;

import doom.ui.font.FontManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.Session;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.io.IOException;

public class GuiAltManager extends GuiScreen {
    // ... reszta pól bez zmian ...
    private GuiScreen parent;
    private String status = "§7Idle...";
    private AltList altList;
    private int selectedAltIndex = -1;
    private GuiTextField offlineField;

    public GuiAltManager(GuiScreen parent) { this.parent = parent; }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.altList = new AltList(this.mc, this.width, this.height, 40, this.height - 50, 26);
        int centerX = this.width / 2;
        int bottomY = this.height - 45;

        this.buttonList.add(new GuiButton(1, centerX - 154, bottomY, 100, 20, "Microsoft Login"));
        this.buttonList.add(new GuiButton(2, centerX - 50, bottomY, 100, 20, "Add Offline"));
        this.buttonList.add(new GuiButton(3, centerX + 54, bottomY, 100, 20, "Remove"));
        this.buttonList.add(new GuiButton(4, centerX - 50, bottomY + 24, 100, 20, "Back"));

        offlineField = new GuiTextField(0, this.mc.fontRendererObj, centerX - 100, 15, 200, 20);

        offlineField.setMaxStringLength(16);
        offlineField.setText("Username");
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 1) {
            // WEB LOGIN (Nowe konto)
            new MicrosoftLogin().loginWeb(new MicrosoftLogin.LoginCallback() {
                @Override
                public void onSuccess(Session session, String refreshToken) {
                    mc.session = session;
                    status = "§aLogged in: " + session.getUsername();
                    // Zapisz z refresh tokenem!
                    AltManager.INSTANCE.addAlt(new Alt(session.getUsername(), refreshToken));
                }
                @Override
                public void onStatus(String msg) { status = "§e" + msg; }
                @Override
                public void onError(String error) { status = "§c" + error; }
            });
        }
        if (button.id == 2) {
            String nick = offlineField.getText();
            if (!nick.isEmpty() && !nick.equals("Username")) {
                AltManager.INSTANCE.addAlt(new Alt(nick, Alt.AltType.OFFLINE));
                status = "§aAdded offline alt: " + nick;
            }
        }
        if (button.id == 3 && selectedAltIndex >= 0 && selectedAltIndex < AltManager.INSTANCE.getAlts().size()) {
            AltManager.INSTANCE.removeAlt(AltManager.INSTANCE.getAlts().get(selectedAltIndex));
            selectedAltIndex = -1;
        }
        if (button.id == 4) mc.displayGuiScreen(parent);
    }

    // Klasa wewnętrzna listy
    class AltList extends GuiSlot {
        public AltList(Minecraft mcIn, int width, int height, int top, int bottom, int slotHeight) {
            super(mcIn, width, height, top, bottom, slotHeight);
        }
        @Override protected int getSize() { return AltManager.INSTANCE.getAlts().size(); }
        @Override protected boolean isSelected(int slotIndex) { return slotIndex == selectedAltIndex; }
        @Override protected void drawBackground() {}

        @Override
        protected void elementClicked(int slotIndex, boolean isDoubleClick, int mouseX, int mouseY) {
            selectedAltIndex = slotIndex;
            if (isDoubleClick) {
                Alt alt = AltManager.INSTANCE.getAlts().get(slotIndex);

                if (alt.getType() == Alt.AltType.MICROSOFT) {
                    // LOGOWANIE Z ZAPISANEGO TOKENA
                    if (alt.getRefreshToken() != null && !alt.getRefreshToken().isEmpty()) {
                        status = "§eRefreshing Microsoft Session...";
                        new MicrosoftLogin().loginWithRefreshToken(alt.getRefreshToken(), new MicrosoftLogin.LoginCallback() {
                            @Override
                            public void onSuccess(Session session, String newRefreshToken) {
                                mc.session = session;
                                status = "§aLogged in: " + session.getUsername();
                                alt.setStatus(Alt.Status.Working);
                                // Aktualizujemy token w alcie, bo one się zmieniają
                                alt.setRefreshToken(newRefreshToken);
                                AltManager.INSTANCE.saveAlts();
                            }

                            @Override
                            public void onStatus(String msg) { status = "§e" + msg; }

                            @Override
                            public void onError(String error) {
                                status = "§cLogin Failed: " + error;
                                alt.setStatus(Alt.Status.Banned);
                            }
                        });
                    } else {
                        status = "§cNo Refresh Token found. Please re-add account.";
                    }
                } else {
                    alt.login(); // Offline login
                    status = "§aLogged in offline as " + alt.getUsername();
                }
            }
        }

        @Override
        protected void drawSlot(int entryID, int p_180791_2_, int p_180791_3_, int p_180791_4_, int mouseXIn, int mouseYIn) {
            Alt alt = AltManager.INSTANCE.getAlts().get(entryID);
            String name = alt.getUsername();
            String type = alt.getType() == Alt.AltType.MICROSOFT ? "§b[Microsoft]" : "§7[Offline]";
            String state = alt.getStatus().toFormatted();

            FontManager.r20.drawStringWithShadow(name, this.width / 2 - FontManager.r20.getStringWidth(name) / 2, p_180791_3_ + 2, -1);
            FontManager.r20.drawStringWithShadow(type, this.width / 2 - FontManager.r20.getStringWidth(type) / 2, p_180791_3_ + 13, -1);
            FontManager.r20.drawStringWithShadow(state, this.width - 50, p_180791_3_ + 8, -1);
        }
    }

    // ... metody drawScreen, handleMouseInput, keyTyped, mouseClicked z Twojego oryginalnego kodu (bez zmian) ...
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.altList.drawScreen(mouseX, mouseY, partialTicks);
        this.drawCenteredString(this.mc.fontRendererObj, "Alt Manager - " + AltManager.INSTANCE.getAlts().size() + " alts", this.width / 2, 4, -1);
        this.drawCenteredString(this.mc.fontRendererObj, "Current: §a" + (mc.session == null ? "?" : mc.session.getUsername()), this.width / 2, 28, -1);
        this.drawCenteredString(this.mc.fontRendererObj, status, this.width / 2, this.height - 65, Color.YELLOW.getRGB());
        offlineField.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void handleMouseInput() throws IOException { super.handleMouseInput(); this.altList.handleMouseInput(); }
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        offlineField.mouseClicked(mouseX, mouseY, mouseButton);
    }
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) { mc.displayGuiScreen(parent); return; }
        offlineField.textboxKeyTyped(typedChar, keyCode);
        if (keyCode == Keyboard.KEY_RETURN && offlineField.isFocused()) actionPerformed(buttonList.get(1));
    }
    @Override
    public void updateScreen() { offlineField.updateCursorCounter(); }
}