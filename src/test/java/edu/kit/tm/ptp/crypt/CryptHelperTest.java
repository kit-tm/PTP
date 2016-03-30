package edu.kit.tm.ptp.crypt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.kit.tm.ptp.Identifier;
import edu.kit.tm.ptp.PTP;

public class CryptHelperTest {

  @Before
  public void setUp() throws Exception {}

  @After
  public void tearDown() throws Exception {}
  
  @Test
  public void testCalculateHiddenServiceIdentifier()
      throws IOException, InterruptedException, InvalidKeySpecException, InvalidKeyException,
      NoSuchAlgorithmException, NoSuchProviderException {
    PTP ptp = new PTP();
    
    ptp.init();
    ptp.createHiddenService();
    
    File hiddenServices = new File(ptp.getConfiguration().getHiddenServicesDirectory());
    
    int found = 0;
    File privateKey = null;
    BufferedReader reader = null;
    
    for (File dir : hiddenServices.listFiles()) {
      if (dir.isDirectory()) {
        File hostname = new File(dir.getAbsolutePath() + File.separator + "hostname");
        reader = new BufferedReader(new FileReader(hostname));
        String onionName = reader.readLine();
        
        if (onionName.equals(ptp.getIdentifier().toString())) {
          found++;
          privateKey = new File(dir.getAbsolutePath() + File.separator + "private_key");
        }
        
        reader.close();
      }
    }
    
    assertEquals(1, found);
    assertNotNull(privateKey);
    
    CryptHelper helper = CryptHelper.getInstance();
    helper.init();
    KeyPair pair = helper.readKeyPairFromFile(privateKey);
    
    Identifier ident = helper.calculateHiddenServiceIdentifier(pair.getPublic());
    
    assertEquals(ptp.getIdentifier().toString(), ident.toString());
  }

}
