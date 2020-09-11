/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package transport.protocol;

import java.util.Timer;

/**
 *
 * @author Rowan Salem
 */
public class PacketTimer extends Timer {

    private int sequenceNumber;


    public PacketTimer(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

}
