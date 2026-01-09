import os

# --- KONFIGURACJA ---
# Główna nazwa paczki
package_base = "doom"
# Ścieżka do folderu z kodem źródłowym (MavenMCP standardowo)
source_root = os.path.join("src", "main", "java")

# --- LISTA PLIKÓW DO STWORZENIA ---
structure = {
    # Główny folder
    "": ["Client.java"],

    # Event System
    "event": ["Event.java", "EventManager.java"],
    "event/impl": ["EventUpdate.java", "EventRender2D.java", "EventKey.java"],

    # Module System
    "module": ["Module.java", "ModuleManager.java", "Category.java"],
    "module/impl/combat": [],   # Puste foldery na przyszłość
    "module/impl/movement": [],
    "module/impl/player": [],
    "module/impl/render": [],
    "module/impl/world": [],
    "module/impl/misc": [],

    # Settings / Values
    "settings": ["Setting.java", "BooleanSetting.java", "NumberSetting.java", "ModeSetting.java"],

    # UI & Managers
    "ui/clickgui": [],
    "ui/alt": ["AltManager.java"],
    "ui/hud": [],

    # Commands
    "command": ["Command.java", "CommandManager.java"],
    "command/impl": [],

    # Config
    "config": ["ConfigManager.java"],

    # Utilities
    "util": ["RenderUtil.java", "TimeHelper.java", "MathUtil.java", "FileUtil.java"]
}

def create_structure():
    # Zamiana kropek w paczce na ścieżkę folderów (dev/naqsu/doom)
    package_path = package_base.replace(".", os.sep)
    full_base_path = os.path.join(source_root, package_path)

    print(f"Tworzenie struktury w: {full_base_path}...\n")

    if not os.path.exists(full_base_path):
        print(f"UWAGA: Folder {full_base_path} nie istnieje. Tworzę go.")
        os.makedirs(full_base_path, exist_ok=True)

    for subfolder, files in structure.items():
        # Pełna ścieżka do folderu (np. .../doom/event/impl)
        current_dir = os.path.join(full_base_path, subfolder.replace("/", os.sep))

        # Tworzenie folderu
        os.makedirs(current_dir, exist_ok=True)

        # Generowanie nazwy paczki dla tego folderu (np. package dev.naqsu.doom.event;)
        current_package = package_base
        if subfolder:
            current_package += "." + subfolder.replace("/", ".")

        for filename in files:
            file_path = os.path.join(current_dir, filename)

            # Nie nadpisujemy plików, które już istnieją!
            if os.path.exists(file_path):
                print(f"[POMINIĘTO] {filename} już istnieje.")
                continue

            class_name = filename.replace(".java", "")

            # Zawartość pliku
            content = f"""package {current_package};

public class {class_name} {{

}}
"""
            with open(file_path, "w") as f:
                f.write(content)

            print(f"[STWORZONO] {subfolder}/{filename}")

    print("\nGotowe! Odśwież projekt w IntelliJ (kliknij ikonkę Reload z dysku).")

if __name__ == "__main__":
    create_structure()