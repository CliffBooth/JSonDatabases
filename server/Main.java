package server;

import com.google.gson.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Main {
    static private final int PORT = 8000;

    private static ReadWriteLock lock = new ReentrantReadWriteLock();
    private static Lock readLock = lock.readLock();
    private static Lock writeLock = lock.writeLock();

    private static ServerSocket serverSocket;
    private static String filePath = "src/server/data/db.json";

    public static void main(String[] args) throws IOException {
        serverSocket = new ServerSocket(PORT);
        System.out.println("Server started!");
        ExecutorService executor = Executors.newCachedThreadPool();
        while (true) {
            Socket socket = serverSocket.accept();
            executor.execute(() -> {
                try {
                    run(socket);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private static void run(Socket socket) throws IOException {
        DataInputStream input = new DataInputStream(socket.getInputStream());
        DataOutputStream output = new DataOutputStream(socket.getOutputStream());
        readLock.lock();
        FileReader reader = new FileReader(filePath);
        JsonObject database = JsonParser.parseReader(reader).getAsJsonObject();
        reader.close();
        readLock.unlock();
        Gson dbGson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        JsonObject response = null;
        String strQuery = input.readUTF();
        JsonObject query = JsonParser.parseString(strQuery).getAsJsonObject();
        switch (query.get("type").getAsString()) {
            case "exit":
                response = new JsonObject();
                response.addProperty("response", "OK");
                output.writeUTF(dbGson.toJson(response));
                serverSocket.close();
                System.exit(0);
            case "get":
                response = get(query, database);
                break;
            case "delete":
                response = delete(query, database);
                break;
            case "set":
                response = set(query, database);
                break;
        }
        writeLock.lock();
        FileWriter writer = new FileWriter(filePath);
        writer.write(dbGson.toJson(database));
        writer.close();
        writeLock.unlock();

        Gson outputGson = new Gson();
        output.writeUTF(outputGson.toJson(response));

        socket.close();
    }

    private static JsonObject get(JsonObject query, JsonObject database) {
        JsonObject response = new JsonObject();

        JsonArray array = new JsonArray();
        JsonElement element = query.get("key");
        if (query.get("key") instanceof  JsonArray) {
            array.addAll(element.getAsJsonArray());
        } else {
            array.add(element.getAsJsonPrimitive());
        }
        JsonElement value;
        if (array.size() <= 1) {
            value = database.get(array.get(0).getAsString());
        } else {
            JsonObject parentElement = database.get(array.get(0).getAsString()).getAsJsonObject();
            for (int i = 1; i < array.size() - 1; i++) {
                String newKey = array.get(i).getAsString();
                parentElement = parentElement.get(newKey).getAsJsonObject();
            }
            //we need to get last element as JsonElement because it may not be object
            value = parentElement.get(array.get(array.size() - 1).getAsString());
        }

        if (value == null) {
            response.addProperty("response", "ERROR");
            response.addProperty("reason", "No such key");
        } else {
            response.addProperty("response", "OK");
            response.add("value", value);
        }
        return response;
    }

    private static JsonObject set(JsonObject query, JsonObject database) {
        //HERE IS THE PROBLEM: IF WE HAVE "text": "Some text", we can't go like this: -k ["text", "poperty"]
        //because "some text" is not an JsonObject!
        JsonObject response = new JsonObject();

        JsonArray array = getArrayKey(query);

        JsonElement newValue = query.get("value");
        JsonObject parentElement;
        if (array.size() <= 1) {
            parentElement = database;
        } else {
            String firstKey = array.get(0).getAsString();
            if (!database.has(firstKey)) {
                database.add(firstKey, new JsonObject());
            }
            parentElement = database.get(firstKey).getAsJsonObject();
            for (int i = 1; i < array.size() - 1; i++) {
                String newKey = array.get(i).getAsString();
                if (!parentElement.has(newKey)) {
                    parentElement.add(newKey, new JsonObject());
                }
                parentElement = parentElement.get(newKey).getAsJsonObject();
            }
        }
        parentElement.add(array.get(array.size() - 1).getAsString(), newValue);
        response.addProperty("response", "OK");
        return response;
    }

    private static JsonObject delete(JsonObject query, JsonObject database) {
        JsonObject response = new JsonObject();
        JsonObject parentElement;

        JsonArray array = getArrayKey(query);

        if (array.size() <= 1) {
            parentElement = database;
        } else {
            parentElement = database.get(array.get(0).getAsString()).getAsJsonObject();
            for (int i = 1; i < array.size() - 1; i++) {
                String newKey = array.get(i).getAsString();
                if (!parentElement.has(newKey)) {
                    parentElement = null;
                    break;
                }
                parentElement = parentElement.get(newKey).getAsJsonObject();
            }
        }
        String toRemove = array.get(array.size() - 1).getAsString();
        if (parentElement == null || !parentElement.has(toRemove)) {
            response.addProperty("response", "ERROR");
            response.addProperty("reason", "No such key");
        } else {
            parentElement.remove(toRemove);
            response.addProperty("response", "OK");
        }
        return response;
    }

    //utility function to get key parameter as an array
    private static JsonArray getArrayKey(JsonObject query) {
        JsonArray array = new JsonArray();
        JsonElement element = query.get("key");
        if (query.get("key") instanceof  JsonArray) {
            array.addAll(element.getAsJsonArray());
        } else {
            array.add(element.getAsJsonPrimitive());
        }
        return array;
    }
}