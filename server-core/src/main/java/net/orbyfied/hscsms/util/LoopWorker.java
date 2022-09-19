package net.orbyfied.hscsms.util;

import net.orbyfied.hscsms.network.Packet;
import net.orbyfied.hscsms.service.Logging;

import java.util.function.Consumer;

public class LoopWorker extends SafeWorker {

    // the tick target
    Consumer<Float> tickTarget;

    // the target timings
    public  float targetUps;
    private float targetDt;

    // the timings
    public float dt;
    public float ups;
    
    public LoopWorker() {
        super();
    }
    
    public LoopWorker(Consumer<Float> tickTarget) {
        this();
        this.tickTarget = tickTarget;
    }

    public LoopWorker(String name) {
        super(name);
    }

    public LoopWorker(String name, Consumer<Float> tickTarget) {
        this(name);
        this.tickTarget = tickTarget;
    }

    public LoopWorker setTargetUps(float ups) {
        if (ups != -1) {
            this.targetUps = ups;
            this.targetDt  = 1f / ups;
        } else {
            this.targetUps = ups;
        }

        return this;
    }

    @Override
    public void run() {
        long t1;
        long t2;

        // main loop
        while (shouldRun()) {
            // timings
            t1 = System.currentTimeMillis();

            // call tick
            tick();

            // timings
            t2 = System.currentTimeMillis();
            long t = t2 - t1;
            dt = t / 1000f;

            // calculate and wait
            if (targetUps != -1) {
                ups = Math.min(targetUps, 1f / dt);
                // wait for next tick
                if (dt < targetDt)
                    sleepSafe((int) ((targetDt - dt) * 1000));
            } else {
                ups = 1f / dt;
            }
        }
    }

    /**
     * Called when ticked.
     */
    public void tick() {
        if (tickTarget != null)
            tickTarget.accept(dt);
    }

    private void sleepSafe(long ms) {
        try {
            sleep(ms);
        } catch (Exception e) {
            e.printStackTrace(Logging.ERR);
        }
    }

}
