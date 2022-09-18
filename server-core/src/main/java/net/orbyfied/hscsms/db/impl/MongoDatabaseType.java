package net.orbyfied.hscsms.db.impl;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import net.orbyfied.hscsms.db.DatabaseManager;
import net.orbyfied.hscsms.db.DatabaseType;
import net.orbyfied.hscsms.db.Login;
import net.orbyfied.j8.registry.Identifier;
import net.orbyfied.j8.util.logging.Logger;

public class MongoDatabaseType extends DatabaseType<MongoDatabase> {

    public static final Identifier ID = Identifier.of("mongodb");

    public MongoDatabaseType() {
        super(ID);
    }

    @Override
    protected void login(MongoDatabase database, Login login) {
        Logger logger = DatabaseManager.LOGGER;

        try {
            logger.info("Logging in database '" + database.getName() + "' of type " + ID);

            if (!(login instanceof Login.URILogin))
                throw new IllegalArgumentException("login must be a URILogin");
            Login.URILogin ul = (Login.URILogin) login;

            // create connection string
            // and client settings
            ConnectionString connectionString = new ConnectionString(ul.getURI());
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connectionString)
                    .build();

            // login to client
            MongoClient mongoClient = MongoClients.create(settings);
            database.client = mongoClient;

            // get database
            database.db = mongoClient.getDatabase("mc");

            logger.ok("Successfully logged in database '" + database.getName() + "'");
        } catch (Exception e) {
            logger.err("Error while logging in database '" + database.getName() + "'", e);
            e.printStackTrace();
        }
    }

    @Override
    protected void close(MongoDatabase database) {
        database.client.close();
    }

}
