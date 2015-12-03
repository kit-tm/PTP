package edu.kit.tm.ptp.serialization;

/**
 * Class.
 *
 * @author Timon Hackenjos
 */
public class Serialization {
    public byte[] serialize(Object obj) {
        return new byte[0];
    }

    public Object deserialize(byte[] data) {
        return null;
    }

    public <T> void registerClass(Class<T> type) {

    }
}
