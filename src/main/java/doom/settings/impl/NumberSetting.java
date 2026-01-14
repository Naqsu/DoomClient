package doom.settings.impl;

import doom.module.Module;
import doom.settings.Setting;

public class NumberSetting extends Setting {
    public double value;
    public double min;
    public double max;
    public double increment; // O ile ma się przesuwać (np. o 0.1)

    public NumberSetting(String name, Module parent, double value, double min, double max, double increment) {
        super(name, parent);
        this.value = value;
        this.min = min;
        this.max = max;
        this.increment = increment;
    }

    public double getValue() {
        return value;
    }

    // Zwraca wartość jako float (przydatne w renderowaniu)
    public float getValueFloat() {
        return (float) value;
    }

    // Zwraca wartość jako int (przydatne np. do delayu w ms)
    public int getValueInt() {
        return (int) value;
    }

    public void setValue(double value) {
        // Zabezpieczenie, żeby nie wyjść poza min/max
        double precision = 1.0D / this.increment;
        this.value = Math.round(Math.max(this.min, Math.min(this.max, value)) * precision) / precision;
    }

    public double getMin() { return min; }
    public double getMax() { return max; }
}