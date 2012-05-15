import java.util.LinkedList;


public class GCodeLineBuffer extends LinkedList<GCodeLine> {
	private static final long serialVersionUID = 1L;
	
	public boolean inMotion= false;
	private MotionChangeListener motionChangeListener= null;
	private int currentLineNo= 1;
	private int offset= 0;
	private int nextToSendLineNo= 1;
	private int nextToSendMaxLength= GrblComm.GRBL_RX_BUFFER_SIZE;
	private LinkedList<GCodeLine> sentLines = new LinkedList<GCodeLine>();
	private GrblCommHistoryTableModel tableModel;
	
	public GCodeLineBuffer(GrblCommHistoryTableModel tableModel) {
		super();
		this.tableModel= tableModel;
	}

	public synchronized int getNumUnsentLines() {
		return size()-(nextToSendLineNo-offset-1);
		
	}
	
	public void setMotionChangeListener(MotionChangeListener motionChangeListener) {
		this.motionChangeListener= motionChangeListener;
	}
	
	public void startMotion() {
		if(motionChangeListener!=null && !inMotion) {
//			System.out.println("calling motionChanged(true)");
			motionChangeListener.motionChanged(true);
		}
		inMotion= true;
	}
	
	public boolean stopMotion() {
//		System.out.println("stopMotion");
		if(size()==0) return false;
		GCodeLine lastLine= this.getLast();
		if(getNumUnsentLines()>0 || lastLine.answer.isEmpty()) return false;
		if(motionChangeListener!=null && inMotion) {
//			System.out.println("calling motionChanged(false)");
			motionChangeListener.motionChanged(false);
		}
		inMotion= false;
		return true;
	}
	
	public synchronized void reset() {
		GCodeLine firstIgnored= null;
		while(sentLines.size()>0) {
			GCodeLine l= sentLines.removeFirst();
			if(firstIgnored==null) firstIgnored= l;
			l.answer= "Ignored due to reset";
		}
		while(getNumUnsentLines()>0) {
			GCodeLine l= get((nextToSendLineNo++)-offset-1);
			if(firstIgnored==null) firstIgnored= l;
			l.answer= "Ignored due to reset";			
		}
		
		nextToSendMaxLength= GrblComm.GRBL_RX_BUFFER_SIZE;
		if(firstIgnored!=null)
			tableModel.	fireTableRowsUpdated(firstIgnored.no-offset-1, size()-1);
		
		stopMotion();
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
		while(getNumUnsentLines()==0) {
			try {
				wait();
			} catch (InterruptedException e) {}
		}
		GCodeLine l= get((nextToSendLineNo++)-offset-1);
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
	
	public int getLastSentLineTableIdx() {
		if(sentLines.size()==0) {
			if(size()==0) return -1;
			else return size()-1;
		} else {
			return(sentLines.getLast().no-offset-1);
		}
			
	}
	
	@Override
	public GCodeLine get(int arg0) {
		return super.get(arg0);
	}

	@Override
	public synchronized GCodeLine removeFirst() {
		offset++;
		return super.removeFirst();
	}
	
	@Override
	public synchronized boolean add(GCodeLine l) {
		l.no= currentLineNo++;
		boolean result= super.add(l);
		notify();
		startMotion();
		return result; 
	}
}
