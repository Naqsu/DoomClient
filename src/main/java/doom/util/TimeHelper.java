package doom.util;

public class TimeHelper {
    private long lastTime = System.currentTimeMillis();

    public void reset() {
        lastTime = System.currentTimeMillis();
    }

    public boolean hasReached(double milliseconds) {
        return System.currentTimeMillis() - lastTime >= milliseconds;
    }

    public long getTime() {
        return System.currentTimeMillis() - lastTime;
    }

    public void setLastTime(long time) {
        this.lastTime = time;
    }
}