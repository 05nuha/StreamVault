package com.streamvault.db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class MongoConnection {

    private static final String URI =
            System.getenv().getOrDefault("MONGO_URI", "mongodb://localhost:27017");
    private static final String DB_NAME =
            System.getenv().getOrDefault("MONGO_DB", "StreamVault");

    private static volatile MongoClient client;

    // double-checked locking: servlets can race on first access
    public static MongoDatabase getDatabase() {
        if (client == null) {
            synchronized (MongoConnection.class) {
                if (client == null) {
                    client = MongoClients.create(URI);
                }
            }
        }
        return client.getDatabase(DB_NAME);
    }
}
