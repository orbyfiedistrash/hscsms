package net.orbyfied.hscsms.db;

import net.orbyfied.hscsms.util.Values;
import net.orbyfied.j8.util.functional.TriFunction;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DatabaseQuery<R, D extends Database> {

    Executor executor = Executors.newFixedThreadPool(2);

    ////////////////////////////////////////////

    final String op;
    final DatabaseType<D> type;
    final TriFunction<DatabaseQuery<R, D>, D, Values, R> func;

    public DatabaseQuery(String op,
                         DatabaseType<D> type,
                         TriFunction<DatabaseQuery<R, D>, D, Values, R> func) {
        this.op   = op;
        this.type = type;
        this.func = func;
    }

    public DatabaseType<D> getType() {
        return type;
    }

    public String getOperation() {
        return op;
    }

    public R doSync(D db, Values vals) {
        final Values fvals = vals == null ? new Values() : vals;
        return func.apply(this, db, fvals);
    }

    public CompletableFuture<R> doAsync(final D db, final Values vals) {
        final Values fvals = vals == null ? new Values() : vals;
        return CompletableFuture.supplyAsync(
                () -> func.apply(this, db, fvals),
                executor
        );
    }

}
