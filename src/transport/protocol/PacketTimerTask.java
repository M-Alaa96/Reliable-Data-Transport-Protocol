
package transport.protocol;

import java.util.TimerTask;

public class PacketTimerTask extends TimerTask {

    private int sequenceNumber;


    public PacketTimerTask(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    @Override
    public void run() {
    }

}
