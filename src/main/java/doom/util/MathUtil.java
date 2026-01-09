package doom.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MathUtil {

    // Zaokrągla liczbę do podanej ilości miejsc po przecinku
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    // Losuje liczbę z przedziału (dla np. losowego AimAssist)
    public static double getRandom(double min, double max) {
        return Math.random() * (max - min) + min;
    }
}