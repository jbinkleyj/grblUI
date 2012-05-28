
import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TooManyListenersException;

import javax.swing.JOptionPane;

public class GrblSerialComm extends GrblCommunicator implements SerialPortEventListener, GrblProtocolProcessor {
	public static final int CONNECT_OK= 0;
	public static final int CONNECT_PORT_IN_USE= 1;
	public static final int CONNECT_NO_SERIAL_PORT= 2;
	public static final int CONNECT_NO_SUCH_PORT= 3;
	public static final int CONNECT_IO_EXEPTION= 4;
	public static final int CONNECT_PROMPT_TIMEOUT= 5;
	
	private SerialPort serialPort;
    private InputStream in;
    private byte[] inBuffer = new byte[1024];
    private int bytesInBuffer= 0;
    private int ticksWithoutMotion= 0;
    private final int NO_MOTION_TICKS= 4;
    public Float[] currPos= new Float[3];
    private Float[] lastPos= new Float[3];
    
    private QueryTickWriter queryTickWriter;
    private Thread queryTickWriterThread;
    
    private CNCPositionListener positionListener= null;
	
	public GrblSerialComm(GCodeLineBuffer lineBuffer) {
		super(lineBuffer);
		
	}

    public int connect(String portName) {
        CommPortIdentifier portIdentifier;
		try {
			portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
		} catch (NoSuchPortException e1) {
			return CONNECT_NO_SUCH_PORT;
		}
        if (portIdentifier.isCurrentlyOwned()) {
            return CONNECT_PORT_IN_USE;
        } else {
        	CommPort commPort= null;
        	try {
        		commPort= portIdentifier.open(this.getClass().getName(),2000);
        	} catch (gnu.io.PortInUseException e) {
                return CONNECT_PORT_IN_USE;
        	}
            
            if(commPort instanceof SerialPort) {
                serialPort = (SerialPort)commPort;
                try {
					serialPort.setSerialPortParams(9600,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
				} catch (UnsupportedCommOperationException e) {}
                
                try {
					in= serialPort.getInputStream();
				} catch (IOException e) {
					return CONNECT_IO_EXEPTION; 
				}
                               
                
                try {
					serialPort.addEventListener(this);
				} catch (TooManyListenersException e) {
					return CONNECT_IO_EXEPTION; 					
				}
                
                serialPort.notifyOnDataAvailable(true);
                
                setupWriter();
                
                if(versionStringOK()) return CONNECT_OK;
                else return CONNECT_PROMPT_TIMEOUT;
            } else {
            	return CONNECT_NO_SERIAL_PORT;
            }
        }
    }
    
    protected void setupWriter() {
		super.setupWriter();
        (queryTickWriterThread= (new Thread(queryTickWriter= new QueryTickWriter()))).start();		
	}
	
    public void setPositionListener(CNCPositionListener positionListener) {
    	this.positionListener= positionListener;
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
			lineReceived(rxLine);
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

	public void dispose() {
		super.dispose();
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
		lineBuffer.numSentLines++;
		lineBuffer.setDirty(line.no);
	}
	
	public void settingReceived(int idx, Double val, String lable) {
		settings.set(idx, val, lable);
	}
	
	public synchronized void lineReceived(GCodeLine line, String answer) {
		line.answer= answer;
		lineBuffer.setDirty(line.no);
	}

}
