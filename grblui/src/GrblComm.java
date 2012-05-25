
import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.swing.JOptionPane;

public class GrblComm implements SerialPortEventListener, GrblProtocolProcessor {
	public static final int SETTINGS_GOTTEN= 0;
	public static final int SETTINGS_TIME_OUT= 1;
	public static final int SETTINGS_MISMATCH= 2;
	private SerialPort serialPort;
    private InputStream in;
    private ThreadSafeSerialSender sender;
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
    private GrblBufferWriter serialWriter;
    private Thread queryTickWriterThread;
    private Thread serialWriterThread;
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
                sender= new ThreadSafeSerialSender(serialPort.getOutputStream());
                               
                (serialWriterThread= (new Thread(serialWriter= new GrblBufferWriter(sender, lineBuffer, this)))).start();
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
			serialWriter.lineReceived(rxLine);
		}
	}

	public class QueryTickWriter implements Runnable {
		public boolean exit = false;

		public void run() {
			while (!exit) {
				sender.send("?".getBytes());
				
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {}
			}
		}
	}

	public synchronized void pause() {
//		System.out.println("Sending Pause");
		sender.send("!".getBytes());
	}
	
	public synchronized void start() {
		sender.send("~".getBytes());
	}
	
	public synchronized void reset() {
		verString= null;
		resetting= true;
		byte[] resetChar= new byte[1];
		resetChar[0]= 0x18;
		sender.send(resetChar);
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

	@Override
	public void newPosition(Float[] pos) {
    	boolean noMotion= true;
    	for(int i= 0; i<3; i++) {
    		lastPos[i]= currPos[i];
    		currPos[i]= pos[i];
    		if(Math.abs(lastPos[i]-currPos[i])>1e-6) {
    			noMotion= false;
    		}
    	}
    	if(noMotion && lineBuffer.inMotion) {
    		ticksWithoutMotion++;
//    		System.out.println("ticksWithoutMotion " + ticksWithoutMotion);
    	} else
    		ticksWithoutMotion= 0;
    	
    	if(ticksWithoutMotion>=NO_MOTION_TICKS) {
    		if(!lineBuffer.stopMotion()) ticksWithoutMotion= 0;
    	}
    	
    	if(positionListener!=null) positionListener.updatePosition(currPos);
	}
	
	public void lineSent(GCodeLine line) {
		line.sent= true;
		lineBuffer.setDirty(line.no);
		
	}
	
	public void verStrReceived(String verStr) {
		setVersionString(verStr);
	}
	
	public void settingReceived(int idx, Double val, String lable) {
		settings.set(idx, val, lable);
	}
}
