package edu.kit.tm.ptp.serialization;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.IOException;


/**
 * Class.
 *
 * @author Timon Hackenjos
 */
public class Serializer {
  private Kryo kryo = new Kryo();
  private int defaultBufferSize = 1024;
  
  public Serializer() {
    kryo.register(Message.class);
  }
  
  public byte[] serialize(Object obj) {
    if (obj == null) {
      throw new IllegalArgumentException("Object to serialize is null");
    }
    
    // No maximum buffer size
    Output out = new Output(defaultBufferSize, -1);
    
    kryo.writeClassAndObject(out, obj);
    
    return out.getBuffer();
  }

  public Object deserialize(byte[] data) throws IOException {
    Input input = new Input(data);
    
    Object obj = kryo.readClassAndObject(input);
    
    if (obj == null) {
      throw new IOException("Error while deserializing data");
    }
    
    return obj;
  }

  public <T> void registerClass(Class<T> type) {
    kryo.register(type);
  }
}
