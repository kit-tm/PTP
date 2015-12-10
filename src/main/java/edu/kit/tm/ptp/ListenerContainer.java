package edu.kit.tm.ptp;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ListenerContainer {
  private Map<Class<?>, Object> listeners = new HashMap<Class<?>, Object>();
  private List<Class<?>> registerClasses = new LinkedList<Class<?>>();

  public <T> void putListener(Class<T> type, MessageReceivedListener<T> listener) {
    if (type == null || listener == null) {
      throw new NullPointerException("Parameter is null");
    }
    listeners.put(type, listener);
    registerClasses.add(type);
  }

  /*public <T> MessageReceivedListener<T> getListener(Class<T> type) {
    return (MessageReceivedListener<T>) listeners.get(type);
  }*/
  
  private Class<?> getType(Object obj) {
    for (Class<?> cl : registerClasses) {
      if (cl.isInstance(obj)) {
        return cl;
      }
    }
    
    throw new IllegalStateException("Type of object hasn't been registered before");
  }
  
  /*public <T> void callListener(Class<T> type, T object, Identifier source) {
    MessageReceivedListener<T> listener = (MessageReceivedListener<T>) listeners.get(type);
    listener.messageReceived(object, source);
  }*/
  
  private <T> void callListener2(T object, Identifier source) {
    @SuppressWarnings("unchecked")
    MessageReceivedListener<T> listener = 
        (MessageReceivedListener<T>) listeners.get(object.getClass());
    
    if (listener == null) {
      throw new IllegalArgumentException("Type of object hasn't been registered before");
    }
    
    listener.messageReceived(object, source);
  }
  
  public void callListener(Object obj, Identifier source) {   
    callListener2(getType(obj).cast(obj), source);
  }
}
