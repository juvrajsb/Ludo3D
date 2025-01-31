package ludo.core.network;

import java.io.*;

public interface NetworkMessage extends Serializable {
    /**
     * Gets the type identifier for this message
     */
    String getType();

//    /**
//     * Serializes this message to bytes
//     */
//    default byte[] serialize() {
//        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
//             ObjectOutputStream out = new ObjectOutputStream(bos)) {
//            out.writeObject(this);
//            return bos.toByteArray();
//        } catch (IOException e) {
//            throw new RuntimeException("Failed to serialize message: " + e.getMessage(), e);
//        }
//    }
//
//    /**
//     * Deserializes bytes back into a NetworkMessage
//     */
//    static NetworkMessage deserialize(byte[] data) {
//        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
//             ObjectInputStream in = new ObjectInputStream(bis)) {
//            return (NetworkMessage) in.readObject();
//        } catch (IOException | ClassNotFoundException e) {
//            throw new RuntimeException("Failed to deserialize message: " + e.getMessage(), e);
//        }
//    }
}
