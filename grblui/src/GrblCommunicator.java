import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;


public abstract class GrblCommunicator {
	public static final int SETTINGS_GOTTEN= 0;
	public static final int SETTINGS_TIME_OUT= 1;
	public static final int SETTINGS_MISMATCH= 2;
	
	private int nextToSendLineNo= 1;
	private static final int GRBL_RX_BUFFER_SIZE= 128;
	private int nextToSendMaxLength= GRBL_RX_BUFFER_SIZE;
	protected LinkedList<GCodeLine> sentLines = new LinkedList<GCodeLine>();
	protected GCodeLineBuffer lineBuffer;
	public boolean exit = false;
    private boolean gettingSettings= false;
	protected OutputStream out;
	private GrblWriter writer;
    private Thread writerThread;

    public GrblSettings settings;
    public String verString;

	protected boolean resetting= false;
	protected boolean ignoreNextOK= false;

	public GrblCommunicator(GCodeLineBuffer lineBuffer) {
		this.lineBuffer = lineBuffer;
	}

	protected void setupWriter() {
        (writerThread= (new Thread(writer= new GrblWriter()))).start();
	}
	
	private class GrblWriter implements Runnable {
		public boolean exit = false;
		@Override
		public void run() {
			String s;
			while (!exit) {
				s= getNextToSend().line;
				if(!resetting)
					send((s + "\n").getBytes());
			}
		}
	}
	
    protected synchronized void send(byte[] b) {
    	try {
    		// TODO send byte by byte unsynchronized to be able to insert a
    		//		runtime command at any time
			out.write(b);
		} catch (IOException e) {}
    }
    
	public synchronized GCodeLine getNextToSend() {
		GCodeLine l= lineBuffer.getLineNo(nextToSendLineNo);
		nextToSendLineNo++;
		
		while(l.line.length()>nextToSendMaxLength)
			try {
				wait();
			} catch (InterruptedException e) {}
		
		sentLines.add(l);
		nextToSendMaxLength-= l.line.length();
		lineSent(l);
		return l;
	}
	
	public synchronized void lineDispatched(String answer) {
		if(sentLines.size()==0) return;
		GCodeLine l= sentLines.removeFirst();
		l.answer= answer;
		lineBuffer.setDirty(l.no);
		
		nextToSendMaxLength+= l.line.length();
		notify();
	}
	
	public void lineReceived(String rxLine) {
        if(rxLine.startsWith("MPos:")) {
        	String[] s = rxLine.split("[\\]\\[xyz,\\s]");
        	
        	Float[] pos= new Float[3];
        	for(int i= 0; i<3; i++) {
        		pos[i]= Float.parseFloat(s[i+6]);
        	}
    		newPosition(pos);
        } else if(rxLine.startsWith("Grbl ")) {
        	verStrReceived(rxLine.substring(5));
        } else if(rxLine.startsWith("'$' ")) {
        } else if(rxLine.startsWith("$")) {
        	String[] s = rxLine.substring(1).split("=");
        	Integer idx;
			try {
				idx = Integer.parseInt(s[0].trim());
				s[1]= s[1].trim();
				int endOfVal= s[1].indexOf(' ');
				if(endOfVal>0) {
					Double val= Double.parseDouble(s[1].substring(0, endOfVal));
					endOfVal++;
					String lable;
					if(endOfVal>s[1].length()) lable= "no lable";
					else lable= s[1].substring(endOfVal).trim();
					if(lable.startsWith("(")) lable= lable.substring(1);
					if(lable.endsWith(")")) lable= lable.substring(0, lable.length()-1);
					if(lable.length()>1) lable= lable.substring(0, 1).toUpperCase() + lable.substring(1);
					settingReceived(idx.intValue(), val, lable);
				}
			} catch (NumberFormatException e) {}
        	
//			System.out.println(settings.toString());
        } else if(rxLine.startsWith("Stored new setting")) {
    		lineDispatched(rxLine);
        	ignoreNextOK= true;
        } else
        	if(gettingSettings)
        		doneGettingSettings();
        	else if(!ignoreNextOK) {
        		lineDispatched(rxLine);
        		ignoreNextOK= false;
        	}
		
	}
	
	public void setParameter(int idx, double val) {
		String s= "$" + idx + "=" + val;
		System.out.println(s);
		send((s + "\n").getBytes());
	}
	
	public int getSettings() {
		settings= new GrblSettings(this);
		gettingSettings= true;
		send("$\n".getBytes());
		
		if(gotAllSettings())
			if(settings.match(verString)) return SETTINGS_GOTTEN;
			else return SETTINGS_MISMATCH;
		else
			return SETTINGS_TIME_OUT;
	}
	
	private synchronized boolean gotAllSettings() {
		if(gettingSettings)
			try {
				wait(3000);
			} catch (InterruptedException e) {}
		return !gettingSettings;		
	}
	
	private synchronized void doneGettingSettings() {
		gettingSettings= false;
		notify();
	}
	
//	public int getLastSentLineTableIdx() {
//		if(sentLines.size()==0) {
//			if(size()==0) return -1;
//			else return size()-1;
//		} else {
//			return(sentLines.getLast().no-offset-1);
//		}
//			
//	}
//	
	public synchronized int getNumUnsentLines() {
		if(lineBuffer.size()>0)
			return lineBuffer.getLast().no-nextToSendLineNo;
		else
			return 0;
	}

	public synchronized void pause() {
//		System.out.println("Sending Pause");
		send("!".getBytes());
	}
	
	public synchronized void start() {
		send("~".getBytes());
	}
	
	public synchronized void reset() {
		verString= null;
		resetting= true;
		byte[] resetChar= new byte[1];
		resetChar[0]= 0x18;
		send(resetChar);
		getVersionString();
		
		lineBuffer.reset();
		resetting= false;
	}
	
	public void dispose() {
	     if(writer != null) {
	    	 writer.exit = true;
	         try {
	        	 writerThread.join();
	         } catch (InterruptedException e) {}
	         
	         writer = null;
	         writerThread = null;
	     }
	}

    protected synchronized String getVersionString() {
    	if(verString==null)
			try {
				wait(3000);
			} catch (InterruptedException e) {}
    	return verString;
    }
    
	public abstract void newPosition(Float[] pos);
	
	public void verStrReceived(String verStr) {
    	verString= verStr;
    	notify();
    }
	
	public abstract void settingReceived(int idx, Double val, String lable);
	public abstract void lineSent(GCodeLine line);
}
