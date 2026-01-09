package doom.module;

import doom.event.EventManager;
import doom.event.EventTarget;
import doom.event.impl.EventKey;
import java.util.ArrayList;

// Tutaj będziemy importować konkretne mody, np.:
// import doom.module.impl.movement.Sprint;

public class ModuleManager {

    public ArrayList<Module> modules = new ArrayList<>();

    public ModuleManager() {
        modules.add(new doom.module.impl.render.Watermark());
        modules.add(new doom.module.impl.render.InfoHUD());
        modules.add(new doom.module.impl.render.ActiveModules());
        modules.add(new doom.module.impl.render.SessionInfo());
        modules.add(new doom.module.impl.render.TargetHUD());
        modules.add(new doom.module.impl.render.Animations());
        modules.add(new doom.module.impl.render.HUD());
        modules.add(new doom.module.impl.combat.Killaura());
        modules.add(new doom.module.impl.render.ClickGuiModule());
        modules.add(new doom.module.impl.movement.Speed());
        modules.add(new doom.module.impl.render.NotificationsMod());
        modules.add(new doom.module.impl.combat.AntiBot());
        modules.add(new doom.module.impl.render.ESP());
        modules.add(new doom.module.impl.player.Scaffold()); // Jeśli jeszcze nie dodałeś Scaffolda
        modules.add(new doom.module.impl.render.BlockCounter()); // Twój nowy licznik
        modules.add(new doom.module.impl.combat.Velocity());
        modules.add(new doom.module.impl.misc.GrimExploits());
        modules.add(new doom.module.impl.movement.Fly());
        modules.add(new doom.module.impl.render.Trail());
        modules.add(new doom.module.impl.render.JumpCircles());
        modules.add(new doom.module.impl.render.SpiritAura());
        modules.add(new doom.module.impl.render.BlockOverlay());
        modules.add(new doom.module.impl.render.ChinaHat());
        modules.add(new doom.module.impl.render.Wings());
        modules.add(new doom.module.impl.player.BedBreaker());
        modules.add(new doom.module.impl.misc.Debugger());
        modules.add(new doom.module.impl.player.InventoryManager());
        modules.add(new doom.module.impl.player.ChestStealer());
        modules.add(new doom.module.impl.movement.Phase());

        EventManager.register(this);
    }

    // Metoda pomocnicza do pobierania listy modułów
    public ArrayList<Module> getModules() {
        return modules;
    }

    // Metoda do szukania modułu po nazwie (przydatne do komend/configu)
    public Module getModuleByName(String name) {
        for (Module m : modules) {
            if (m.getName().equalsIgnoreCase(name)) return m;
        }
        return null;
    }

    // Metoda do szukania modułu po klasie (profesjonalne podejście)
    public <T extends Module> T getModule(Class<T> clazz) {
        for (Module m : modules) {
            if (m.getClass() == clazz) {
                return (T) m;
            }
        }
        return null;
    }

    // To jest "ucho" ModuleManagera - słucha, jaki klawisz wcisnąłeś
    @EventTarget
    public void onKey(EventKey event) {
        for (Module m : modules) {
            // Jeśli klawisz modułu zgadza się z wciśniętym...
            if (m.getKey() == event.getKey()) {
                m.toggle(); // ...to go włącz/wyłącz
            }
        }
    }
}