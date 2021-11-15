package testing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class JsonTest {
    public static void main(String[] args) throws IOException {
        FileReader reader = new FileReader("db.json");
        JsonObject db = JsonParser.parseReader(reader).getAsJsonObject();
        reader.close();

        JsonObject person = db.get("person").getAsJsonObject();
        person.addProperty("name", 20);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        FileWriter writer = new FileWriter("db.json");
        writer.write(gson.toJson(db));
        writer.close();
    }
}
