/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package transport.protocol;

import java.util.TimerTask;

/**
 *
 * @author Rowan Salem
 */
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