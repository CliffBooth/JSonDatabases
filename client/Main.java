package client;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.gson.*;
import com.google.gson.stream.MalformedJsonException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    static private final int PORT = 8000;
    static private final String ADDRESS = "127.0.0.1";

    @Parameter(names = "-t", description = "type of request")
    static private String type;

    @Parameter(names = "-k", description = "index of the cell")
    static private String key;

    @Parameter(names = "-v", description = "the value to save in the database")
    static private String value;

    @Parameter(names = "-in", description = "file from which to read a request")
    static private String file;

    private static String filePath = "src/client/data/";
//    private static String filePath = "src/client/data/";

    public static void main(String[] args) throws IOException {
        JCommander.newBuilder().addObject(new Main()).build().parse(args);
        try (
                Socket socket = new Socket(ADDRESS, PORT);
                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output = new DataOutputStream(socket.getOutputStream())
        ) {
            System.out.println("Client started!");

            JsonObject query = new JsonObject();
            String toSend;
            if (file != null) {
                toSend = Files.readString(Path.of(filePath + file));
            } else {
                if (type != null) {
                    query.add("type", new JsonPrimitive(type));
                }
                if (key != null) {
                    JsonElement element = JsonParser.parseString(key);
                    if (element instanceof JsonArray) {
                        query.add("key", element.getAsJsonArray());
                    } else {
                        query.add("key", new JsonPrimitive(element.getAsString()));
                    }
                }
                if (value != null) {
                    //value can be a json object or a primitive
                    try {
                        JsonElement jsonValue = JsonParser.parseString(value);
                        query.add("value", jsonValue);
                    } catch (JsonSyntaxException e) {
                        JsonPrimitive primitive = new JsonPrimitive(value);
                        query.add("value", primitive);
                    }
                }
                Gson gson = new Gson();
                toSend = gson.toJson(query);
            }
            output.writeUTF(toSend);
            System.out.println("Sent: " + toSend);
            String received = input.readUTF();
            System.out.println("Received: " + received);
        }
    }
}
