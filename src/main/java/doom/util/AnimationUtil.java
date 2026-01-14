package doom.util;

public class AnimationUtil {
    public static float animate(float target, float current, float speed) {
        if (Math.abs(target - current) < 0.0005f) return target;
        return current + (target - current) * speed;
    }

    public static float easeOutElastic(float x) {
        double c4 = (2 * Math.PI) / 3;
        return x == 0 ? 0 : x == 1 ? 1 : (float) (Math.pow(2, -10 * x) * Math.sin((x * 10 - 0.75) * c4) + 1);
    }

    public static float easeOutExpo(float x) {
        return x == 1 ? 1 : (float) (1 - Math.pow(2, -10 * x));
    }

    public static float easeOutBack(float x) {
        double c1 = 1.70158;
        double c3 = c1 + 1;
        return (float) (1 + c3 * Math.pow(x - 1, 3) + c1 * Math.pow(x - 1, 2));
    }

    public static float easeOutBounce(float x) {
        float n1 = 7.5625f;
        float d1 = 2.75f;
        if (x < 1 / d1) return n1 * x * x;
        else if (x < 2 / d1) return n1 * (x -= 1.5f / d1) * x + 0.75f;
        else if (x < 2.5 / d1) return n1 * (x -= 2.25f / d1) * x + 0.9375f;
        else return n1 * (x -= 2.625f / d1) * x + 0.984375f;
    }

    public static float getEase(float progress, String mode) {
        switch (mode) {
            case "Elastic": return easeOutElastic(progress);
            case "Expo": return easeOutExpo(progress);
            case "Back": return easeOutBack(progress);
            case "Bounce": return easeOutBounce(progress);
            default: return progress;
        }
    }
}