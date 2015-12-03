package edu.kit.tm.ptp.channels;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Class.
 *
 * @author Timon Hackenjos
 */

public class MessageChannel {
    private ByteBuffer sendBuffer;
    private ByteBuffer receiveBuffer;
    private SocketChannel channel;
    private int bufferLength;

    public MessageChannel(SocketChannel channel) {
        this.channel = channel;
        // Initialize buffers
    }

    public void read() {

    }

    public void write() {

    }

    public long addMessage(byte[] data) {
        return 0;
    }
}
