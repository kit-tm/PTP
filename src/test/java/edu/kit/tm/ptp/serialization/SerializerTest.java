package edu.kit.tm.ptp.serialization;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class SerializerTest {
  private Serializer serializer;

  @Before
  public void setUp() throws Exception {
    serializer = new Serializer();
  }

  @After
  public void tearDown() throws Exception {}

  @Test
  public void testSerializeByteArrayMessage() throws IOException {    
    serializer.registerClass(ByteArrayMessage.class);
    
    byte[] bytes = new String("Hallo").getBytes();
    ByteArrayMessage message = new ByteArrayMessage(bytes);
    
    byte[] serializedMessage = serializer.serialize(message);
    
    Object deserializedMessage = serializer.deserialize(serializedMessage);
    
    assertEquals(true, deserializedMessage instanceof ByteArrayMessage);
    assertArrayEquals(message.getData(), ((ByteArrayMessage) deserializedMessage).getData());
  }

}
