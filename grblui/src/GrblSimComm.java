import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class GrblSimComm extends GrblCommunicator {
	public static final int CONNECT_OK= 0;
	public static final int CONNECT_PROCESS_NOT_STARTED= 1;	
	public static final int CONNECT_PROMPT_TIMEOUT= 5;
	
    private Process process;
    
    private InputStream inStd;
    private BufferedReader inStdReader; 
    private InputStream inErr;
    private GrblReader reader;
    private Thread readerThread;
    private NewBlockListener newBlockListener;


	public GrblSimComm(GCodeLineBuffer lineBuffer) {
		super(lineBuffer);
	}
	
	public void setNewBlockListener(NewBlockListener newBlockListener) {
		this.newBlockListener= newBlockListener;
	}

	public int connect() {
		ProcessBuilder builder = new ProcessBuilder("a.exe");
		builder.redirectErrorStream(true);
		try {
			process = builder.start();
		} catch (IOException e) {
			return CONNECT_PROCESS_NOT_STARTED;
		}

		out= process.getOutputStream ();
		inErr= process.getErrorStream ();
		inStdReader= new BufferedReader(new InputStreamReader(inStd= process.getInputStream ()));
        (readerThread= (new Thread(reader= new GrblReader()))).start();
        setupWriter();
        
        if(versionStringOK()) return CONNECT_OK;
        else return CONNECT_PROMPT_TIMEOUT;
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
//						if(rxLine==null)
//							System.out.println("null line");
//						else
//							System.out.println("empty line");
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
		if(rxLine.startsWith("  block")) {
			rxLine= rxLine.substring(8);
        	String[] s = rxLine.split(",");
        	Integer[] steps= new Integer[3];
        	for(int i= 0; i<3; i++)
        		steps[i]= Integer.parseInt(s[i].trim());
        	sentLines.getFirst().steps= steps;
        	if(newBlockListener!=null) newBlockListener.newBlock(steps);
		} else {
			super.lineReceived(rxLine);
		}
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
//			try {
//				readerThread.join();
//			} catch (InterruptedException e) {}

			reader = null;
			readerThread = null;
		}
		try {
			out.close();
			inStd.close();
			inErr.close();
		} catch (IOException e) {}
		process.destroy();
	}

	@Override
	public void newPosition(Float[] pos) {
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

	public synchronized void lineReceived(GCodeLine line, String answer) {
		line.answerSim= answer;
		lineBuffer.setDirty(line.no);
	}
}
