import java.io.IOException;
import java.io.OutputStream;


public class ThreadSafeSerialSender {
	private OutputStream out;

	public ThreadSafeSerialSender(OutputStream out) {
		super();
		this.out = out;
	}
	
    public synchronized void send(byte[] b) {
    	try {
    		// TODO send byte by byte unsynchronized to be able to insert a
    		//		runtime command at any time
			out.write(b);
		} catch (IOException e) {}
    }
}
