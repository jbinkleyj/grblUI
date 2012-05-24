import java.util.LinkedList;


public class GrblBufferWriter implements Runnable {
	private int nextToSendLineNo= 1;
	private static final int GRBL_RX_BUFFER_SIZE= 128;
	private int nextToSendMaxLength= GRBL_RX_BUFFER_SIZE;
	private LinkedList<GCodeLine> sentLines = new LinkedList<GCodeLine>();
	private GCodeLineBuffer lineBuffer;
	public boolean exit = false;

	public GrblBufferWriter(GCodeLineBuffer lineBuffer) {
		this.lineBuffer = lineBuffer;
	}

	@Override
	public void run() {
		String s;
		while (!exit) {
			s= getNextToSend().line;
			if(!resetting)
				send((s + "\n").getBytes());
		}
	}

	public synchronized void lineDispatched(String answer) {
		if(sentLines.size()==0) return;
		GCodeLine l= sentLines.removeFirst();
		l.answer= answer;
		tableModel.fireTableCellUpdated(l.no-offset-1, GrblCommHistoryTableModel.ANSWER_ROW);
		
		nextToSendMaxLength+= l.line.length();
		notify();
//		System.out.println("lineDispatched " + nextToSendMaxLength);
	}
	
	public synchronized GCodeLine getNextToSend() {
		GCodeLine l= lineBuffer.getLinNo(nextToSendLineNo);
		nextToSendLineNo++;
		
		while(l.line.length()>nextToSendMaxLength)
			try {
				wait();
			} catch (InterruptedException e) {}
		
		sentLines.add(l);
		nextToSendMaxLength-= l.line.length();
		l.sent= true;
		tableModel.fireTableCellUpdated(l.no-offset-1, GrblCommHistoryTableModel.SENT_ROW);
		return l;
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
	
}
