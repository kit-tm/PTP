package edu.kit.tm.ptp;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

/**
 * Container to store a listener for a class type.
 * Inspired by typesafe heterogeneous containers.
 * Effective Java, Second Edition, Item 29.
 * 
 * @author Timon Hackenjos
 *
 */
public class ListenerContainer {
  private Map<Class<?>, Object> listeners = new Hashtable<Class<?>, Object>();
  protected Set<Class<?>> registerClasses = new HashSet<Class<?>>();

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
  
  protected Class<?> getType(Object obj) {
    for (Class<?> cl : registerClasses) {
      if (cl.isInstance(obj)) {
        return cl;
      }
    }
    
    throw new IllegalStateException("Type of object hasn't been registered before");
  }
  
  protected Class<?> getTypeOrNull(Object obj) {
    for (Class<?> cl : registerClasses) {
      if (cl.isInstance(obj)) {
        return cl;
      }
    }
    
    return null;
  }
  
  
  /**
   * Calls a previously registered listener.
   */
  public void callReceiveListener(Object obj, Identifier source) {
    if (!hasListener(obj)) {
      throw new IllegalArgumentException();
    }
    
    callListener(getType(obj).cast(obj), source);
  }
  
  public boolean hasListener(Object obj) {
    return listeners.get(getType(obj)) != null;
  }
  
  private <T> void callListener(T object, Identifier source) {
    @SuppressWarnings("unchecked")
    MessageReceivedListener<T> listener = 
        (MessageReceivedListener<T>) listeners.get(object.getClass());
    
    if (listener == null) {
      throw new IllegalArgumentException("Type of object hasn't been registered before");
    }
    
    listener.messageReceived(object, source);
  }
}
