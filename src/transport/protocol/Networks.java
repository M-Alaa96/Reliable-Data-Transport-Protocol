

package transport.protocol;

public abstract class Networks implements Runnable {

    protected static Mode mode;
    protected Thread thread;
    
    public static void setMode(Networks.Mode mode) {
        Networks.mode = mode;
    }

    public void run() {
    }

    public void start() {
        thread = new Thread(this);
        thread.start();
    }

    public enum Mode {
        StopAndWait,
        SelectiveRepeat,
        GoBackN;
    }

}

