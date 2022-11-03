package de.tum.i13.server.kv;

public interface KVMessage {

    /**
     * @return the key that is associated with this message,
     * null if not key is associated.
     */
    String getKey();

    /**
     * @return the value that is associated with this message,
     * null if not value is associated.
     */
    String getValue();

    /**
     * @return a status string that is used to identify request types,
     * response types and error types associated to the message.
     */
    StatusType getStatus();

    /**
     * Creates string for KVMessages.
     *
     * @return a string of KVMessage object.
     */
    String toString();

    enum StatusType {
        GET,            /* Get - request */
        GET_ERROR,        /* requested tuple (i.e. value) not found */
        GET_SUCCESS,    /* requested tuple (i.e. value) found */
        GET_DELETED,    /* requested tuple is recently deleted */
        PUT,            /* Put - request */
        PUT_SUCCESS,    /* Put - request successful, tuple inserted */
        PUT_UPDATE,    /* Put - request successful, i.e. value updated */
        PUT_ERROR,        /* Put - request not successful */
        DELETE,        /* Delete - request */
        DELETE_SUCCESS, /* Delete - request successful */
        DELETE_ERROR,    /* Delete - request successful */
        SERVER_STOPPED, /* Server is under initialization, not serving for clients */
        SERVER_NOT_RESPONSIBLE, /* Key is not under the server's range */
        SERVER_WRITE_LOCK, /* Server locked because of reallocation of keys&data */
        KEY_RANGE_SUCCESS, /* Gets the key range of servers*/
        KEY_RANGE_READ_SUCCESS, /* Gets the coordinated and replicated key ranges of the servers */
        SERVER_RECEIVED_EMPTY_DATA, /* The data received from another KVServer is empty*/
        ECS_ME, /* The current KV-Server serve as an ECS */
        ECS_IP, /* The current KV-Server knows the location of ECS */
        ECS_NO_INFO, /* There is an election in motion so no info about ecs*/
        PARTICIPANT, /* If there is an election */
        ELECTED, /* If the node is elected*/
        UNRECOGNIZED_COMMAND /* Couldn't recognize message*/
    }

}
