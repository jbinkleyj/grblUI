
import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.swing.JOptionPane;

public class GrblComm implements SerialPortEventListener {
	public static final int SETTINGS_GOTTEN= 0;
	public static final int SETTINGS_TIME_OUT= 1;
	public static final int SETTINGS_MISMATCH= 2;
	private SerialPort serialPort;
    private InputStream in;
    private OutputStream out;
    private GCodeLineBuffer lineBuffer;
    private byte[] inBuffer = new byte[1024];
    private int bytesInBuffer= 0;
    private int ticksWithoutMotion= 0;
    private final int NO_MOTION_TICKS= 4;
    public Float[] currPos= new Float[3];
    private Float[] lastPos= new Float[3];
    public String verString;
    public GrblSettings settings;
    
    private QueryTickWriter queryTickWriter;
    private SerialWriter serialWriter;
    private Thread queryTickWriterThread;
    private Thread serialWriterThread;
    private boolean gettingSettings= false;
    private boolean ignoreNextOK= false;
    
    private CNCPositionListener positionListener= null;
	private boolean resetting= false;

	
    public GrblComm(GCodeLineBuffer lineBuffer) {
    	this.lineBuffer= lineBuffer;
    }
    
    public String connect(String portName) throws Exception {
        CommPortIdentifier portIdentifier= CommPortIdentifier.getPortIdentifier(portName);
        if (portIdentifier.isCurrentlyOwned()) {
        	JOptionPane.showMessageDialog(null, "Error: Port is currently in use", "Com Port Error", JOptionPane.ERROR_MESSAGE);
            return null;
        } else {
        	CommPort commPort= null;
        	try {
        		commPort= portIdentifier.open(this.getClass().getName(),2000);
        	} catch (gnu.io.PortInUseException e) {
            	JOptionPane.showMessageDialog(null, "Error: Port is currently in use", "Com Port Error", JOptionPane.ERROR_MESSAGE);
                return null;
        	}
            
            if(commPort instanceof SerialPort) {
                serialPort = (SerialPort)commPort;
                serialPort.setSerialPortParams(9600,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
                
                in= serialPort.getInputStream();
                out= serialPort.getOutputStream();
                               
                (serialWriterThread= (new Thread(serialWriter= new SerialWriter(lineBuffer)))).start();
                (queryTickWriterThread= (new Thread(queryTickWriter= new QueryTickWriter()))).start();
                
                serialPort.addEventListener(this);
                serialPort.notifyOnDataAvailable(true);
                
                return getVersionString();
            } else {
            	JOptionPane.showMessageDialog(null, "Error: Only serial ports are allowed", "Com Port Error", JOptionPane.ERROR_MESSAGE);
            	return null;
            }
        }
    }
    
    public void setPositionListener(CNCPositionListener positionListener) {
    	this.positionListener= positionListener;
    }
    
    public synchronized void send(byte[] b) {
    	try {
    		// TODO send byte by byte unsynchronized to be able to insert a
    		//		runtime command at any time
			out.write(b);
		} catch (IOException e) {}
    }
    
    private synchronized String getVersionString() {
    	if(verString==null)
			try {
				wait(3000);
			} catch (InterruptedException e) {}
    	return verString;
    }
    
    private synchronized void setVersionString(String s) {
    	verString= s;
    	notify();
    }
    
	public void serialEvent(SerialPortEvent arg0) {
		int data= -1;
		try {
			while((data = in.read()) > -1 ) {
			    if(data == '\n' || data == '\r')
			    	break;
			    inBuffer[bytesInBuffer++]= (byte)data;
			}
		} catch (IOException e) {}
		
		if((data == '\n' || data == '\r') && bytesInBuffer>0) {
			String rxLine = new String(inBuffer,0,bytesInBuffer);
//			if(!rxLine.startsWith("MPos:"))
//				System.out.println(rxLine);
			bytesInBuffer= 0;
            if(rxLine.startsWith("MPos:")) {
//            	System.out.println(rxLine);
            	String[] s = rxLine.split("[\\]\\[xyz,\\s]");
//            	for(int i= 0; i<s.length; i++)
//            		System.out.print("'" + s[i] + "', ");
//        		System.out.println();
            	
            	boolean noMotion= true;
            	for(int i= 0; i<3; i++) {
            		lastPos[i]= currPos[i];
//            		currPos[i]= Float.parseFloat(s[i+1]);
            		currPos[i]= Float.parseFloat(s[i+6]);
            		if(Math.abs(lastPos[i]-currPos[i])>1e-6) {
            			noMotion= false;
//            			System.out.println("inMotion " + i);
            		}
            	}
            	if(noMotion && lineBuffer.inMotion) {
            		ticksWithoutMotion++;
//            		System.out.println("ticksWithoutMotion " + ticksWithoutMotion);
            	} else
            		ticksWithoutMotion= 0;
            	
            	if(ticksWithoutMotion>=NO_MOTION_TICKS) {
            		if(!lineBuffer.stopMotion()) ticksWithoutMotion= 0;
            	}
            	
            	if(positionListener!=null) positionListener.updatePosition(currPos);
            } else if(rxLine.startsWith("Grbl ")) {
            	setVersionString(rxLine.substring(5));
//            	System.out.println(rxLine);
            } else if(rxLine.startsWith("'$' ")) {
//            	System.out.println(rxLine);
            } else if(rxLine.startsWith("$")) {
            	String[] s = rxLine.substring(1).split("=");
//            	System.out.println(s[0] + "==" + s[1]);
            	Integer idx;
				try {
					idx = Integer.parseInt(s[0].trim());
					s[1]= s[1].trim();
					int endOfVal= s[1].indexOf(' ');
//					System.out.println("s(0, " + endOfVal + ")= " + s[1].substring(0, endOfVal));
					if(endOfVal>0) {
						Double val= Double.parseDouble(s[1].substring(0, endOfVal));
						endOfVal++;
						String lable;
						if(endOfVal>s[1].length()) lable= "no lable";
						else lable= s[1].substring(endOfVal).trim();
						if(lable.startsWith("(")) lable= lable.substring(1);
						if(lable.endsWith(")")) lable= lable.substring(0, lable.length()-1);
						if(lable.length()>1) lable= lable.substring(0, 1).toUpperCase() + lable.substring(1); 						
//						System.out.println("[" + idx + "==" + val + "]" + lable);
						settings.set(idx.intValue(), val, lable);
					}
				} catch (NumberFormatException e) {}
            	
//				System.out.println(settings.toString());
            } else if(rxLine.startsWith("Stored new setting")) {
        		lineBuffer.lineDispatched(rxLine);
            	ignoreNextOK= true;
            } else
            	if(gettingSettings)
            		doneGettingSettings();
            	else if(!ignoreNextOK) {
            		lineBuffer.lineDispatched(rxLine);
            		ignoreNextOK= false;
            	}
		}
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
	
	public void setParameter(int idx, double val) {
		String s= "$" + idx + "=" + val;
		System.out.println(s);
		send((s + "\n").getBytes());
	}
	
	public class SerialWriter implements Runnable {
		private GCodeLineBuffer lineBuffer;
		public boolean exit = false;

		public SerialWriter(GCodeLineBuffer lineBuffer) {
			this.lineBuffer = lineBuffer;
		}

		public void run() {
			String s;
			while (!exit) {
				s= lineBuffer.getNextToSend().line;
				if(!resetting)
					send((s + "\n").getBytes());
			}
		}
	}

	public class QueryTickWriter implements Runnable {
		public boolean exit = false;

		public void run() {
			while (!exit) {
				send("?".getBytes());
				
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {}
			}
		}
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
	     if(serialWriter != null) {
	    	 serialWriter.exit = true;
	         try {
	        	 serialWriterThread.join();
	         } catch (InterruptedException e) {}
	         
	         serialWriter = null;
	         serialWriterThread = null;
	     }
	     if(queryTickWriter != null) {
	    	 queryTickWriter.exit = true;
	         try {
	        	 queryTickWriterThread.join();
	         } catch (InterruptedException e) {}
	         
	         queryTickWriter = null;
	         queryTickWriterThread = null;
        }
		if(serialPort!=null)
			serialPort.close();
	}
}
