package de.tum.i13;

import de.tum.i13.shared.MetadataHandler;
import org.javatuples.Pair;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class MetadataHandlerTest {

    @Test
    public void getKeyRangeReadWithReplicationTest() {
        String serverAddress = "s3-gr10-kv-server0:46383";
        String keyrange = "f2e7fa90bef59f342329231e435221eb,50310ce5672e7285b8d419f50f263886,s3-gr10-kv-server0:46383;50310ce5672e7285b8d419f50f263886,7dc86741a9006e19362a8812b86d3c25,s3-gr10-kv-server1:39943;7dc86741a9006e19362a8812b86d3c25,7f7bab0aee7c67fa6349a254ddb40732,s3-gr10-kv-server4:45171;7f7bab0aee7c67fa6349a254ddb40732,c302ed0c69fd4bb45c424245fb5be4d2,s3-gr10-kv-server2:36035;c302ed0c69fd4bb45c424245fb5be4d2,f2e7fa90bef59f342329231e435221eb,s3-gr10-kv-server3:42367;";
        MetadataHandler.updateMetadata(keyrange, serverAddress);

        String keyrange_success_message = MetadataHandler.getKeyrangeRead();
        String expectedMessage = " 7f7bab0aee7c67fa6349a254ddb40732,50310ce5672e7285b8d419f50f263886,s3-gr10-kv-server0:46383;c302ed0c69fd4bb45c424245fb5be4d2,7dc86741a9006e19362a8812b86d3c25,s3-gr10-kv-server1:39943;f2e7fa90bef59f342329231e435221eb,7f7bab0aee7c67fa6349a254ddb40732,s3-gr10-kv-server4:45171;50310ce5672e7285b8d419f50f263886,c302ed0c69fd4bb45c424245fb5be4d2,s3-gr10-kv-server2:36035;7dc86741a9006e19362a8812b86d3c25,f2e7fa90bef59f342329231e435221eb,s3-gr10-kv-server3:42367;";

        assertEquals(keyrange_success_message, expectedMessage);
    }

    @Test
    public void getKeyRangeReadWithoutReplicationTest() {
        String serverAddress = "s3-gr10-kv-server0:46383";
        String keyrange = "f2e7fa90bef59f342329231e435221eb,50310ce5672e7285b8d419f50f263886,s3-gr10-kv-server0:46383;50310ce5672e7285b8d419f50f263886,7dc86741a9006e19362a8812b86d3c25,s3-gr10-kv-server1:39943;";
        MetadataHandler.updateMetadata(keyrange, serverAddress);

        String keyrange_success_message = MetadataHandler.getKeyrangeRead();
        String expectedMessage = " f2e7fa90bef59f342329231e435221eb,50310ce5672e7285b8d419f50f263886,s3-gr10-kv-server0:46383;50310ce5672e7285b8d419f50f263886,7dc86741a9006e19362a8812b86d3c25,s3-gr10-kv-server1:39943;";

        assertEquals(keyrange_success_message, expectedMessage);
    }

    @Test
    void getReadRangeWithReplicationTest() {
        String serverAddress = "s3-gr10-kv-server1:39943";
        String keyrange = "f2e7fa90bef59f342329231e435221eb,50310ce5672e7285b8d419f50f263886,s3-gr10-kv-server0:46383;50310ce5672e7285b8d419f50f263886,7dc86741a9006e19362a8812b86d3c25,s3-gr10-kv-server1:39943;7dc86741a9006e19362a8812b86d3c25,7f7bab0aee7c67fa6349a254ddb40732,s3-gr10-kv-server4:45171;7f7bab0aee7c67fa6349a254ddb40732,c302ed0c69fd4bb45c424245fb5be4d2,s3-gr10-kv-server2:36035;c302ed0c69fd4bb45c424245fb5be4d2,f2e7fa90bef59f342329231e435221eb,s3-gr10-kv-server3:42367;";
        MetadataHandler.updateMetadata(keyrange, serverAddress);

        assertEquals(MetadataHandler.getReadRange(), new Pair<>("c302ed0c69fd4bb45c424245fb5be4d2", "7dc86741a9006e19362a8812b86d3c25"));
    }

    @Test
    void getReadRangeWithoutReplicationTest() {
        String serverAddress = "s3-gr10-kv-server1:39943";
        String keyrange = "f2e7fa90bef59f342329231e435221eb,50310ce5672e7285b8d419f50f263886,s3-gr10-kv-server0:46383;50310ce5672e7285b8d419f50f263886,7dc86741a9006e19362a8812b86d3c25,s3-gr10-kv-server1:39943;";
        MetadataHandler.updateMetadata(keyrange, serverAddress);

        assertEquals(MetadataHandler.getReadRange(), new Pair<>("50310ce5672e7285b8d419f50f263886", "7dc86741a9006e19362a8812b86d3c25"));
    }

    @Test
    void getCoordinatedRangeWithReplicationTest() {
        String serverAddress = "s3-gr10-kv-server1:39943";
        String keyrange = "f2e7fa90bef59f342329231e435221eb,50310ce5672e7285b8d419f50f263886,s3-gr10-kv-server0:46383;50310ce5672e7285b8d419f50f263886,7dc86741a9006e19362a8812b86d3c25,s3-gr10-kv-server1:39943;7dc86741a9006e19362a8812b86d3c25,7f7bab0aee7c67fa6349a254ddb40732,s3-gr10-kv-server4:45171;7f7bab0aee7c67fa6349a254ddb40732,c302ed0c69fd4bb45c424245fb5be4d2,s3-gr10-kv-server2:36035;c302ed0c69fd4bb45c424245fb5be4d2,f2e7fa90bef59f342329231e435221eb,s3-gr10-kv-server3:42367;";
        MetadataHandler.updateMetadata(keyrange, serverAddress);

        assertEquals(MetadataHandler.getCoordinatedRange(), new Pair<>("50310ce5672e7285b8d419f50f263886", "7dc86741a9006e19362a8812b86d3c25"));
    }

    @Test
    void getCoordinatedRangeWithoutReplicationTest() {
        String serverAddress = "s3-gr10-kv-server1:39943";
        String keyrange = "f2e7fa90bef59f342329231e435221eb,50310ce5672e7285b8d419f50f263886,s3-gr10-kv-server0:46383;50310ce5672e7285b8d419f50f263886,7dc86741a9006e19362a8812b86d3c25,s3-gr10-kv-server1:39943;";
        MetadataHandler.updateMetadata(keyrange, serverAddress);

        assertEquals(MetadataHandler.getCoordinatedRange(), new Pair<>("50310ce5672e7285b8d419f50f263886", "7dc86741a9006e19362a8812b86d3c25"));
    }

    @Test
    void getCoordinatedServersWithReplicationTest() {
        String serverAddress = "s3-gr10-kv-server0:46383";
        String keyrange = "f2e7fa90bef59f342329231e435221eb,50310ce5672e7285b8d419f50f263886,s3-gr10-kv-server0:46383;50310ce5672e7285b8d419f50f263886,7dc86741a9006e19362a8812b86d3c25,s3-gr10-kv-server1:39943;7dc86741a9006e19362a8812b86d3c25,7f7bab0aee7c67fa6349a254ddb40732,s3-gr10-kv-server4:45171;7f7bab0aee7c67fa6349a254ddb40732,c302ed0c69fd4bb45c424245fb5be4d2,s3-gr10-kv-server2:36035;c302ed0c69fd4bb45c424245fb5be4d2,f2e7fa90bef59f342329231e435221eb,s3-gr10-kv-server3:42367;";
        MetadataHandler.updateMetadata(keyrange, serverAddress);

        assertEquals(MetadataHandler.getCoordinatedServer1(), "s3-gr10-kv-server1:39943");
        assertEquals(MetadataHandler.getCoordinatedServer2(), "s3-gr10-kv-server4:45171");

        serverAddress = "s3-gr10-kv-server2:36035";
        keyrange = "f2e7fa90bef59f342329231e435221eb,50310ce5672e7285b8d419f50f263886,s3-gr10-kv-server0:46383;50310ce5672e7285b8d419f50f263886,7dc86741a9006e19362a8812b86d3c25,s3-gr10-kv-server1:39943;7dc86741a9006e19362a8812b86d3c25,7f7bab0aee7c67fa6349a254ddb40732,s3-gr10-kv-server4:45171;7f7bab0aee7c67fa6349a254ddb40732,c302ed0c69fd4bb45c424245fb5be4d2,s3-gr10-kv-server2:36035;c302ed0c69fd4bb45c424245fb5be4d2,f2e7fa90bef59f342329231e435221eb,s3-gr10-kv-server3:42367;";
        MetadataHandler.updateMetadata(keyrange, serverAddress);

        assertEquals(MetadataHandler.getCoordinatedServer1(), "s3-gr10-kv-server3:42367");
        assertEquals(MetadataHandler.getCoordinatedServer2(), "s3-gr10-kv-server0:46383");

        serverAddress = "s3-gr10-kv-server3:42367";
        keyrange = "f2e7fa90bef59f342329231e435221eb,50310ce5672e7285b8d419f50f263886,s3-gr10-kv-server0:46383;50310ce5672e7285b8d419f50f263886,7dc86741a9006e19362a8812b86d3c25,s3-gr10-kv-server1:39943;7dc86741a9006e19362a8812b86d3c25,7f7bab0aee7c67fa6349a254ddb40732,s3-gr10-kv-server4:45171;7f7bab0aee7c67fa6349a254ddb40732,c302ed0c69fd4bb45c424245fb5be4d2,s3-gr10-kv-server2:36035;c302ed0c69fd4bb45c424245fb5be4d2,f2e7fa90bef59f342329231e435221eb,s3-gr10-kv-server3:42367;";
        MetadataHandler.updateMetadata(keyrange, serverAddress);

        assertEquals(MetadataHandler.getCoordinatedServer1(), "s3-gr10-kv-server0:46383");
        assertEquals(MetadataHandler.getCoordinatedServer2(), "s3-gr10-kv-server1:39943");
    }

    @Test
    void getCoordinatedServersWithoutReplicationTest() {
        String serverAddress = "s3-gr10-kv-server1:39943";
        String keyrange = "f2e7fa90bef59f342329231e435221eb,50310ce5672e7285b8d419f50f263886,s3-gr10-kv-server0:46383;50310ce5672e7285b8d419f50f263886,7dc86741a9006e19362a8812b86d3c25,s3-gr10-kv-server1:39943;";
        MetadataHandler.updateMetadata(keyrange, serverAddress);

        assertNull(MetadataHandler.getCoordinatedServer1());
        assertNull(MetadataHandler.getCoordinatedServer2());
    }
}
