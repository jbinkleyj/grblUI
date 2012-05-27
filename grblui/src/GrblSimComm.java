import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class GrblSimComm extends GrblCommunicator {
    private Process process;
    
    private InputStream inStd;
    private BufferedReader inStdReader; 
    private InputStream inErr;
    private GrblReader reader;
    private Thread readerThread;


	public GrblSimComm(GCodeLineBuffer lineBuffer) {
		super(lineBuffer);
	}

	public int connect() {
    	try {
    		ProcessBuilder builder = new ProcessBuilder("a.exe");
    		builder.redirectErrorStream(true);
    		process = builder.start();

    		out= process.getOutputStream ();
    		inErr= process.getErrorStream ();
    		inStdReader= new BufferedReader(new InputStreamReader(inStd= process.getInputStream ()));
            (readerThread= (new Thread(reader= new GrblReader()))).start();
    	} catch (IOException e) {}
		
		return 0;
	}
	
	private class GrblReader implements Runnable {
		public boolean exit = false;
		@Override
		public void run() {
			while (!exit) {
		    	String rxLine= null;
				try {
					rxLine= inStdReader.readLine();
					if(rxLine==null || rxLine.length()==0) {
						if(rxLine==null)
							System.out.println("null line");
						else
							System.out.println("empty line");
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {}
					} else {
						lineReceived(rxLine);
					}
				} catch (IOException e) {}
				
				waitForMore();
			}
		}
		
		private synchronized void waitForMore() {
			while(sentLines.size()==0 && !(verString==null))
				try {
					wait();
				} catch (InterruptedException e) {}
		}
		
		public synchronized void lineAdded() {
			notify();
		}
	}
	
	public void lineReceived(String rxLine) {
		if(!rxLine.startsWith("  block")) {
		} else
			super.lineReceived(rxLine);
	}
	
    protected synchronized void send(byte[] b) {
    	try {
			out.write(b);
			out.flush();
		} catch (IOException e) {}
    }
    
	public void dispose() {
		super.dispose();
	     if(reader != null) {
	    	 reader.exit = true;
	         try {
	        	 readerThread.join();
	         } catch (InterruptedException e) {}
	         
	         reader = null;
	         readerThread = null;
      }
	}

	@Override
	public void newPosition(Float[] pos) {
	}

	@Override
	public void verStrReceived(String verStr) {
		// TODO Auto-generated method stub

	}

	@Override
	public void settingReceived(int idx, Double val, String lable) {
		// TODO Auto-generated method stub

	}

	@Override
	public void lineSent(GCodeLine line) {
		reader.lineAdded();
		line.sentSim= true;
		lineBuffer.setDirty(line.no);
	}

}
