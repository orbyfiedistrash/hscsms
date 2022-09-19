package net.orbyfied.hscsms.util;

import net.orbyfied.hscsms.service.Logging;
import net.orbyfied.j8.util.functional.ThrowableRunnable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class SafeWorker extends Thread {

    // is active flag
    final AtomicBoolean active = new AtomicBoolean(false);
    // runnable target
    ThrowableRunnable target;

    // activity predicate
    Predicate<SafeWorker> activityPredicate;
    // error handler
    BiConsumer<SafeWorker, Throwable> errorHandler;

    public SafeWorker() {
        this.target = null;
    }

    public SafeWorker(ThrowableRunnable target) {
        this.target = target;
    }

    public SafeWorker(String name) {
        super(name);
        this.target = null;
    }

    public SafeWorker(String name, ThrowableRunnable target) {
        super(name);
        this.target = target;
    }

    public Predicate<SafeWorker> getActivityPredicate() {
        return activityPredicate;
    }

    public SafeWorker withActivityPredicate(Predicate<SafeWorker> predicate) {
        this.activityPredicate = predicate;
        return this;
    }

    public BiConsumer<SafeWorker, Throwable> getErrorHandler() {
        return errorHandler;
    }

    public SafeWorker withErrorHandler(BiConsumer<SafeWorker, Throwable> consumer) {
        this.errorHandler = consumer;
        return this;
    }

    public SafeWorker withTarget(ThrowableRunnable runnable) {
        this.target = runnable;
        return this;
    }

    public SafeWorker setActive(boolean b) {
        active.set(b);
        return this;
    }

    public boolean isSetActive() {
        return active.get();
    }

    /**
     * Should be called by eventual loops
     * and processes reliant on checking
     * a condition.
     * @return If it should be active.
     */
    public boolean shouldRun() {
        return active.get() &&
                (activityPredicate == null || activityPredicate.test(this));
    }

    @Override
    public synchronized void start() {
        active.set(true);
        super.start();
    }

    public synchronized SafeWorker commence() {
        start();
        return this;
    }

    public void terminate() {
        active.set(false);
        try {
            super.stop();
        } catch (Exception ignored) { }
    }

    public void await() {
        try {
            join();
        } catch (Exception e) {
            e.printStackTrace(Logging.ERR);
        }
    }

    @Override
    public void run() {
        try {
            // call run safe
            runSafe();
        } catch (Throwable t) {
            if (errorHandler != null) {
                errorHandler.accept(this, t);
            } else {
                System.err.println("Error in worker thread " + getName());
                t.printStackTrace(Logging.ERR);
            }
        }

        // set inactive status
        active.set(false);
    }

    /**
     * Safe run target.
     */
    public void runSafe() throws Throwable {
        if (target != null)
            target.run();
    }

}
