package edu.kit.tm.ptp;

import net.freehaven.tor.control.EventHandler;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TorEventHandler implements EventHandler {
  private static final Logger logger = Logger.getLogger(TorEventHandler.class.getName());
  private TorManager torManager;

  //private List<String> events =
  //    Arrays.asList("circ", "stream", "orconn", "bw", "newdesc", "info", "notice", "warn", "err", "NETWORK_LIVENESS", "DESCCHANGED", "STATUS_GENERAL", "STATUS_CLIENT", "STATUS_SERVER", "GUARD", "HS_DESC");
  private List<String> events =
      Arrays.asList("circ", "stream", "orconn", "bw", "newdesc", "info", "notice", "warn", "err", "DESCCHANGED", "STATUS_GENERAL", "STATUS_CLIENT", "STATUS_SERVER", "GUARD", "HS_DESC");
  
  public TorEventHandler(TorManager torManager) {
    this.torManager = torManager;
  }

  @Override
  public void bandwidthUsed(long arg0, long arg1) {
  }

  @Override
  public void circuitStatus(String arg0, String arg1, String arg2) {
    //logger.log(Level.FINE, arg0 + ";" +  arg1 + ";" + arg2);
  }

  @Override
  public void message(String arg0, String arg1) {
    logger.log(Level.FINE, arg0 + " " + arg1);
  }

  @Override
  public void newDescriptors(List<String> arg0) {
  }

  @Override
  public void orConnStatus(String arg0, String arg1) {
  }

  @Override
  public void streamStatus(String arg0, String arg1, String arg2) {
  }

  @Override
  public void unrecognized(String arg0, String arg1) {
    //logger.log(Level.FINE, arg0 + " " + arg1);
  }

  /**
   * Returns the list of events the event handler wants to register for.
   */
  public List<String> getEvents() {
    return events;
  }
}
