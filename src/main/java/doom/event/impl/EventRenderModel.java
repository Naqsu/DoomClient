package doom.event.impl;

import doom.event.Event;
import net.minecraft.entity.EntityLivingBase;

public class EventRenderModel extends Event {
    private final EntityLivingBase entity;
    private Runnable postRenderTask; // Zadanie do wykonania po renderowaniu (przywracanie)

    public EventRenderModel(EntityLivingBase entity) {
        this.entity = entity;
    }

    public EntityLivingBase getEntity() {
        return entity;
    }

    // Ta metoda pozwoli nam zapisać zadanie przywracania pitcha
    public void setPostRenderTask(Runnable task) {
        this.postRenderTask = task;
    }

    public void runPostRender() {
        if (postRenderTask != null) {
            postRenderTask.run();
        }
    }
}