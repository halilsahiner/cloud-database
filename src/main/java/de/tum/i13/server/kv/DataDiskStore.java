package de.tum.i13.server.kv;

import de.tum.i13.shared.Config;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

public class DataDiskStore implements KVStoreInterface {
    public static Logger logger = Logger.getLogger(KVClientCommandProcessor.class.getName());
    String path;

    public DataDiskStore(Config cfg) {
        this.path = cfg.dataDir + "/database.json";
    }

    @Override
    public KVMessage put(String key, String value) {
        KVMessage message;
        try {
            // decide add or update.
            JSONObject mapObject = readElementFromFile(key).getRight();
            if (mapObject != null) {
                message = updateValue(key, value);
            } else {
                message = addKeyValue(key, value);
            }
        } catch (ParseException | IOException e) {
            message = new Message(key, value, KVMessage.StatusType.PUT_ERROR);
            logger.warning("Value can not be added/updated. Caused by: " + e.getMessage());
        }
        return message;
    }

    @Override
    public KVMessage get(String key) {
        Message message;
        try {
            //get the value from json object
            JSONObject mapObject = readElementFromFile(key).right;

            if (mapObject == null)
                throw new NullPointerException("Value with key: " + key + " can not be found!");

            String value = (String) mapObject.get(key);
            message = new Message(key, value, KVMessage.StatusType.GET_SUCCESS);
            logger.info("Key " + key + " read successfully. Value: " + value);
        } catch (NullPointerException | IOException | ParseException e) {
            logger.warning("Value can not be read. Caused by: " + e.getMessage());
            message = new Message(key, null, KVMessage.StatusType.GET_ERROR);
        }
        return message;
    }

    @Override
    public KVMessage delete(String key) {
        try {
            Pair<JSONArray, JSONObject> pair = readElementFromFile(key);
            JSONArray jsonArray = pair.getLeft();
            JSONObject mapObject = pair.getRight();
            if (mapObject == null)
                throw new NullPointerException("Value with key: " + key + " can not be found!");

            jsonArray.remove(mapObject);
            writeToFile(jsonArray);
        } catch (IOException | ParseException | NullPointerException e) {
            logger.warning("Value can not be deleted. Caused by: " + e.getMessage());
            return new Message(key, null, KVMessage.StatusType.DELETE_ERROR);
        }
        logger.info("Key " + key + " deleted successfully.");
        return new Message(key, null, KVMessage.StatusType.DELETE_SUCCESS);
    }

    /**
     * Updates value of given key with new value in the KVServer.
     *
     * @param key   the key that identifies the given value.
     * @param value the value that is indexed by the given key.
     * @return a message that confirms the update of the tuple or an error.
     */
    private KVMessage updateValue(String key, String value) {
        try {
            Pair<JSONArray, JSONObject> pair = readElementFromFile(key);
            JSONArray jsonArray = pair.getLeft();
            JSONObject mapObject = pair.getRight();
            int index = jsonArray.indexOf(mapObject);
            mapObject.put(key, value);
            jsonArray.set(index, mapObject);

            writeToFile(jsonArray);
        } catch (IOException | ParseException e) {
            logger.warning("Value can not be updated. Caused by: " + e.getMessage());
            return new Message(key, value, KVMessage.StatusType.PUT_ERROR);
        }
        logger.info("Key " + key + " updated with value: " + value + " successfully.");
        return new Message(key, value, KVMessage.StatusType.PUT_UPDATE);
    }

    /**
     * Helper for put method. Creates json object
     * from given key-value pair and write it to
     * json file.
     *
     * @param key   the key that identifies the given value.
     * @param value the value that is indexed by the given key.
     * @return a message that confirms the insertion of the tuple or an error.
     */
    private KVMessage addKeyValue(String key, String value) {
        try {
            JSONArray jsonArray = readElementFromFile(key).left;
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(key, value);
            jsonArray.add(jsonObject);
            writeToFile(jsonArray);
        } catch (IOException | ParseException e) {
            logger.warning("Value can not be added. Caused by: " + e.getMessage());
            return new Message(key, value, KVMessage.StatusType.PUT_ERROR);
        }
        return new Message(key, value, KVMessage.StatusType.PUT_SUCCESS);
    }

    /**
     * Finds value of given key from the json file.
     *
     * @param key the key that identifies the given value.
     * @return pair of json array and json object
     * that correspond to key-value pair.
     */
    private ImmutablePair<JSONArray, JSONObject> readElementFromFile(String key) throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        FileReader reader = new FileReader(path);
        Object obj = jsonParser.parse(reader);

        JSONArray jsonArray = (JSONArray) obj;

        JSONObject mapObject = (JSONObject) jsonArray.stream().filter(jsonObj -> ((JSONObject) jsonObj).keySet().toArray()[0].equals(key))
                .findFirst() // process the Stream until the first match is found
                .orElse(null);

        return new ImmutablePair<>(jsonArray, mapObject);
    }

    /**
     * Writes json array to file.
     *
     * @param jsonArray the key that identifies the given value.
     */
    private void writeToFile(JSONArray jsonArray) throws IOException {
        FileWriter file = new FileWriter(path);
        file.write(jsonArray.toJSONString());
        file.flush();
        file.close();
    }
}
