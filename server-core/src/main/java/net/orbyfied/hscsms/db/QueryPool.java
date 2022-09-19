package net.orbyfied.hscsms.db;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.orbyfied.hscsms.util.Values;
import net.orbyfied.j8.util.functional.TriFunction;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unchecked")
public class QueryPool {

    public static int getHash(String op, DatabaseType<?> type) {
        return (op.hashCode() * 31) | System.identityHashCode(type);
    }

    ///////////////////////////////////

    QueryPool(QueryPool parent) {
        this.parent = parent;
    }

    // environment
    Values env = new Values();

    // the queries stored
    Int2ObjectOpenHashMap<DatabaseQuery<Object, Database>> queries = new Int2ObjectOpenHashMap<>();

    // the current database
    Database database;

    // the current database type
    DatabaseType<Database> currentType;

    // the parent query pool
    QueryPool parent;

    public QueryPool current(Database database) {
        this.database    = database;
        this.currentType = database.type;
        // define env
        return this;
    }

    public QueryPool fork() {
        return new QueryPool(this).current(this.database);
    }

    public <R, D extends Database> DatabaseQuery<R, D> getQuery(int h) {
        DatabaseQuery<R, D> query;
        // search local
        if ((query = (DatabaseQuery<R, D>) queries.get(h)) != null)
            return query;
        // search parent
        if (parent != null)
            return parent.getQuery(h);
        // return absent
        return null;
    }

    public <R, D extends Database> DatabaseQuery<R, D> getQuery(String op,
                                                                DatabaseType<D> type) {
        int h = getHash(op, type);
        return getQuery(h);
    }

    public <R, D extends Database> DatabaseQuery<R, D> getQuery(String op) {
        return (DatabaseQuery<R, D>) getQuery(op, currentType);
    }


    public QueryPool putQuery(DatabaseQuery<?, ? extends Database> query) {
        queries.put(getHash(query.op, query.type), (DatabaseQuery<Object, Database>) query);
        return this;
    }

    public <R, D extends Database> QueryPool putQuery(String op, DatabaseType<D> type,
                                                      TriFunction<DatabaseQuery<R, D>, D, Values, R> func) {
        return putQuery(new DatabaseQuery<>(op, type, func));
    }

    public <R> R querySync(String op, DatabaseType<Database> type, Values vals) {
        return (R) getQuery(op, type).doSync(database, vals);
    }

    public <R> CompletableFuture<R> queryAsync(String op, DatabaseType<Database> type, Values vals) {
        return (CompletableFuture<R>) getQuery(op, type).doAsync(database, vals);
    }

    public <R> R querySync(String op, Values vals) {
        return (R) getQuery(op, currentType).doSync(database, vals);
    }

    public <R> R querySync(String op, Object... vals) {
        return (R) getQuery(op, currentType).doSync(database, Values.ofVarargs(vals));
    }

    public <R> R querySync(String op, Values values, Object... vals) {
        return (R) getQuery(op, currentType).doSync(database, Values.ofVarargs(values, vals));
    }

    public <R> CompletableFuture<R> queryAsync(String op, Values vals) {
        return (CompletableFuture<R>) getQuery(op, currentType).doAsync(database, vals);
    }

    public <R> CompletableFuture<R> queryAsync(String op, Object... vals) {
        return (CompletableFuture<R>) getQuery(op, currentType).doAsync(database, Values.ofVarargs(vals));
    }

    public <R> CompletableFuture<R> queryAsync(String op, Values values, Object... vals) {
        return (CompletableFuture<R>) getQuery(op, currentType).doAsync(database, Values.ofVarargs(values, vals));
    }

}
