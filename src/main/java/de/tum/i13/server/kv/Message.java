package de.tum.i13.server.kv;

public class Message implements KVMessage {
    private final String key;
    private final String value;
    private final StatusType status;

    public Message(String key, String value, StatusType status) {
        this.key = key;
        this.value = value;
        this.status = status;
    }

    public Message(StatusType status) {
        this.status = status;
        this.key = "";
        this.value = "";
    }

    @Override
    public String getKey() {
        return this.key;
    }

    @Override
    public String getValue() {
        return this.value;
    }

    @Override
    public StatusType getStatus() {
        return this.status;
    }

    @Override
    public String toString() {
        switch (status) {
            case GET_SUCCESS:
                return "get_success " + key + " " + value + "\r\n";
            case GET_ERROR:
            case GET_DELETED:
                return "get_error " + key + "\r\n";
            case PUT_SUCCESS:
                return "put_success " + key + "\r\n";
            case PUT_UPDATE:
                return "put_update " + key + "\r\n";
            case PUT_ERROR:
                return "put_error " + key + " " + value + "\r\n";
            case DELETE_SUCCESS:
                return "delete_success " + key + "\r\n";
            case DELETE_ERROR:
                return "delete_error " + key + "\r\n";
            case SERVER_STOPPED:
                return "server_stopped" + "\r\n";
            case SERVER_NOT_RESPONSIBLE:
                return "server_not_responsible" + "\r\n";
            case SERVER_WRITE_LOCK:
                return "server_write_lock" + "\r\n";
            case KEY_RANGE_SUCCESS:
                return "keyrange_success" + value + "\r\n";
            case KEY_RANGE_READ_SUCCESS:
                return "keyrange_read_success" + value + "\r\n";
            case ECS_ME:
                return "ecs_me" + "\r\n";
            case ECS_IP:
                return "ecs_ip " + value +"\r\n";
            case ECS_NO_INFO:
                return "ecs_no_info" +"\r\n";
            case PARTICIPANT:
                return "participant "+ value +"\r\n";
            case ELECTED:
                return "elected "+value+"\r\n";
            case UNRECOGNIZED_COMMAND:
                return "couldn't recognize message"+"\r\n";
            default:
                return "" + "\r\n";
        }
    }
}
