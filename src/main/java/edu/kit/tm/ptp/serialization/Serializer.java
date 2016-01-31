package edu.kit.tm.ptp.serialization;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import edu.kit.tm.ptp.DummyAuthenticator.AuthenticationMessage;

import java.io.IOException;


/**
 * Serializes and deserializes objects using Kryo. To be able to serialize a class it must contain a
 * constructor without any arguments. Classes need to be registered before they can be serialized
 * and deserialized. Also the order in which they are registered is important.
 *
 * @author Timon Hackenjos
 */
public class Serializer {
  private Kryo kryo = new Kryo();
  private int defaultBufferSize = 1024;

  public Serializer() {
    kryo.register(ByteArrayMessage.class);
    kryo.register(AuthenticationMessage.class);
  }

  /**
   * Serialize an object of a previously registered class.
   * 
   * @param obj The object to serialize.
   * @return The bytes representing the object.
   */
  public byte[] serialize(Object obj) {
    if (obj == null) {
      throw new IllegalArgumentException("Object to serialize is null");
    }

    // No maximum buffer size
    Output out = new Output(defaultBufferSize, -1);

    kryo.writeClassAndObject(out, obj);

    return out.getBuffer();
  }

  /**
   * Deserializes a previously serialized object of a class.
   * 
   * @param data The bytes representing the object.
   * @return The deserialized object.
   * @throws IOException If an error occurs while deserializing.
   */
  public Object deserialize(byte[] data) throws IOException {
    Input input = new Input(data);

    Object obj = kryo.readClassAndObject(input);

    if (obj == null) {
      throw new IOException("Error while deserializing data");
    }

    return obj;
  }

  /**
   * Registers the supplied class to be able to serialize objects of the class.
   * Keep in mind that the order of registration matters.
   * 
   * @param type The class to register.
   */
  public <T> void registerClass(Class<T> type) {
    kryo.register(type);
  }
}
