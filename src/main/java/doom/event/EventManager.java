package doom.event;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class EventManager {

    // Tutaj przechowujemy zarejestrowane obiekty (np. moduły)
    private static final Map<Object, Method[]> REGISTRY_MAP = new HashMap<Object, Method[]>();

    // Metoda do rejestrowania modułu (żeby słuchał eventów)
    public static void register(Object o) {
        Method[] methods = o.getClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            // Szukamy metod z adnotacją @EventTarget
            if (method.isAnnotationPresent(EventTarget.class) && method.getParameterTypes().length == 1) {
                if (!REGISTRY_MAP.containsKey(o)) {
                    method.setAccessible(true);
                }

                // Dodajemy do mapy, jeśli jeszcze nie ma
                if(!REGISTRY_MAP.containsKey(o)) {
                    REGISTRY_MAP.put(o, new Method[] {method});
                } else {
                    // Rozszerzamy tablicę metod (prymitywne ale skuteczne)
                    Method[] cache = REGISTRY_MAP.get(o);
                    Method[] newCache = new Method[cache.length + 1];
                    System.arraycopy(cache, 0, newCache, 0, cache.length);
                    newCache[cache.length] = method;
                    REGISTRY_MAP.put(o, newCache);
                }
            }
        }
    }

    // Metoda do wyrejestrowania (np. jak wyłączasz moduł)
    public static void unregister(Object o) {
        if (REGISTRY_MAP.containsKey(o)) {
            REGISTRY_MAP.remove(o);
        }
    }

    // Metoda do czyszczenia wszystkiego (przy zamykaniu gry)
    public static void shutdown() {
        REGISTRY_MAP.clear();
    }

    // GŁÓWNA METODA - wywołuje eventy w modułach
    public static void call(Event event) {
        // Tworzymy kopię zbioru wpisów, aby uniknąć ConcurrentModificationException
        // Dzięki temu, jeśli moduł wyłączy się w trakcie eventu, nie wywali crasha.
        Map<Object, Method[]> copyMap = new HashMap<>(REGISTRY_MAP);

        for (Entry<Object, Method[]> entry : copyMap.entrySet()) {
            Object object = entry.getKey();
            Method[] methods = entry.getValue();

            for (int i = 0; i < methods.length; i++) {
                try {
                    // Sprawdzamy, czy metoda obsługuje ten typ eventu
                    if (methods[i].getParameterTypes()[0].isAssignableFrom(event.getClass())) {
                        methods[i].invoke(object, event);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}