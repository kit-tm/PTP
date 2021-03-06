package edu.kit.tm.ptp.serialization;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;


/**
 * Serializes and deserializes objects using Kryo. To be able to serialize a class it must contain a
 * constructor without any arguments. Classes need to be registered before they can be serialized
 * and deserialized. Also the order in which they are registered is important.
 *
 * @author Timon Hackenjos
 */
public class Serializer {
  private Kryo kryo = new Kryo();
  private Set<Class<?>> registeredClasses = new HashSet<>();
  
  public Serializer() {
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
    Output out = new Output(0, -1);

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
    if (data.length == 0) {
      throw new IOException("Can't deserialize empty byte array");
    }

    Object obj = null;

    try {
      Input input = new Input(data);

      obj = kryo.readClassAndObject(input);
    } catch (KryoException e) { // Handling an unchecked exception
      throw new IOException(e.getMessage());
    }

    if (obj == null) {
      throw new IOException("Kryo returned null");
    }

    return obj;
  }

  /**
   * Registers the supplied class to be able to serialize objects of the class.
   * Keep in mind that the order of registration matters.
   * Registering a class several times has no effect.
   * 
   * @param type The class to register.
   */
  public <T> void registerClass(Class<T> type) {
    registeredClasses.add(type);
    kryo.register(type);
  }

  /**
   * Returns true if the supplied class type has already been registered.
   */
  public <T> boolean isRegistered(Class<T> type) {
    return registeredClasses.contains(type);
  }
}
