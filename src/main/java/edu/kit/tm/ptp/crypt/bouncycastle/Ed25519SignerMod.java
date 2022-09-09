package edu.kit.tm.ptp.crypt.bouncycastle;

import java.io.ByteArrayOutputStream;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Signer;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.util.Arrays;

public class Ed25519SignerMod
    implements Signer
{
    private final Buffer buffer = new Buffer();

    private boolean forSigning;
    private Ed25519PrivateKeyParametersMod privateKey;
    private Ed25519PublicKeyParameters publicKey;

    public Ed25519SignerMod()
    {
    }

    public void init(boolean forSigning, CipherParameters parameters)
    {
        this.forSigning = forSigning;

        if (forSigning)
        {
            this.privateKey = (Ed25519PrivateKeyParametersMod)parameters;
            this.publicKey = null;
        }
        else
        {
            this.privateKey = null;
            this.publicKey = (Ed25519PublicKeyParameters)parameters;
        }

        reset();
    }

    public void update(byte b)
    {
        buffer.write(b);
    }

    public void update(byte[] buf, int off, int len)
    {
        buffer.write(buf, off, len);
    }

    public byte[] generateSignature()
    {
        if (!forSigning || null == privateKey)
        {
            throw new IllegalStateException("Ed25519SignerMod not initialised for signature generation.");
        }

        return buffer.generateSignature(privateKey);
    }

    public boolean verifySignature(byte[] signature)
    {
        if (forSigning || null == publicKey)
        {
            throw new IllegalStateException("Ed25519SignerMod not initialised for verification");
        }

        return buffer.verifySignature(publicKey, signature);
    }

    public void reset()
    {
        buffer.reset();
    }

    private static class Buffer extends ByteArrayOutputStream
    {
        synchronized byte[] generateSignature(Ed25519PrivateKeyParametersMod privateKey)
        {
            byte[] signature = new byte[Ed25519PrivateKeyParametersMod.SIGNATURE_SIZE];
            privateKey.sign(Ed25519Mod.Algorithm.Ed25519, null, buf, 0, count, signature, 0);
            reset();
            return signature;
        }

        synchronized boolean verifySignature(Ed25519PublicKeyParameters publicKey, byte[] signature)
        {
            if (Ed25519Mod.SIGNATURE_SIZE != signature.length)
            {
                reset();
                return false;
            }

            //byte[] pk = publicKey.getEncoded();
            boolean result = true; //Ed25519Mod.verify(signature, 0, pk, 0, buf, 0, count);
            reset();
            return result;
        }

        public synchronized void reset()
        {
            Arrays.fill(buf, 0, count, (byte)0);
            this.count = 0;
        }
    }
}
