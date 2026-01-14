package doom.module;

import doom.event.EventManager;
import doom.event.EventTarget;
import doom.event.impl.EventKey;
import doom.module.impl.combat.HitLag;
import doom.module.impl.player.AntiVoid;
import doom.module.impl.player.NoFall;

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
        modules.add(new doom.module.impl.render.ChestESP());
        modules.add(new doom.module.impl.misc.AnticheatDetector());
        modules.add(new doom.module.impl.combat.FakeLag());
        modules.add(new doom.module.impl.combat.Backtrack());
        modules.add(new doom.module.impl.combat.TickBase());
        modules.add(new NoFall());
        modules.add(new HitLag());
        modules.add(new AntiVoid());
        modules.add(new doom.module.impl.render.GlowESP());
        modules.add(new doom.module.impl.misc.Disabler());

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
    public java.util.List<Module> getModulesByCategory(Module.Category category) {
        java.util.List<Module> list = new java.util.ArrayList<>();
        for (Module m : modules) {
            // Teraz porównujemy ten sam typ enuma
            if (m.getCategory() == category) {
                list.add(m);
            }
        }
        return list;
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