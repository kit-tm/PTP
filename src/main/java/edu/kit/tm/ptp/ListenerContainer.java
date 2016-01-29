package edu.kit.tm.ptp;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Container to store a listener for a class type.
 * Inspired by typesafe heterogeneous containers.
 * Effective Java, Second Edition, Item 29.
 * 
 * @author Timon Hackenjos
 *
 */
public class ListenerContainer {
  private Map<Class<?>, Object> listeners = new HashMap<Class<?>, Object>();
  private List<Class<?>> registerClasses = new LinkedList<Class<?>>();

  /**
   * Maps the listener to the supplied class type.
   */
  public <T> void putListener(Class<T> type, MessageReceivedListener<T> listener) {
    if (type == null || listener == null) {
      throw new NullPointerException("Parameter is null");
    }
    listeners.put(type, listener);
    registerClasses.add(type);
  }
  
  private Class<?> getType(Object obj) {
    for (Class<?> cl : registerClasses) {
      if (cl.isInstance(obj)) {
        return cl;
      }
    }
    
    throw new IllegalStateException("Type of object hasn't been registered before");
  }
  
  private <T> void callListener2(T object, Identifier source) {
    @SuppressWarnings("unchecked")
    MessageReceivedListener<T> listener = 
        (MessageReceivedListener<T>) listeners.get(object.getClass());
    
    if (listener == null) {
      throw new IllegalArgumentException("Type of object hasn't been registered before");
    }
    
    listener.messageReceived(object, source);
  }
  
  /**
   * Calls a previously registered listener.
   */
  public void callListener(Object obj, Identifier source) {   
    callListener2(getType(obj).cast(obj), source);
  }
}
