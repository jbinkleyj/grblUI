import java.util.LinkedList;


public class GCodeLineBuffer extends LinkedList<GCodeLine> {
	private static final long serialVersionUID = 1L;
	
	public boolean inMotion= false;
	private MotionChangeListener motionChangeListener= null;
	private int currentLineNo= 1;
	private int offset= 0;
	public int numSentLines= 0;
	private GrblCommHistoryTableModel tableModel;
	
	public GCodeLineBuffer(GrblCommHistoryTableModel tableModel) {
		super();
		this.tableModel= tableModel;
	}

	public int getNumUnsentLines() {
		return (size()+offset)-numSentLines;
	}
	
	public int getLastSentLineTableIdx() {
		return(numSentLines-offset-1);
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
		if(getLast().answer.isEmpty()) return false;
		if(motionChangeListener!=null && inMotion) {
//			System.out.println("calling motionChanged(false)");
			motionChangeListener.motionChanged(false);
		}
		inMotion= false;
		return true;
	}
	
	public synchronized void reset() {
//		GCodeLine firstIgnored= null;
//		while(sentLines.size()>0) {
//			GCodeLine l= sentLines.removeFirst();
//			if(firstIgnored==null) firstIgnored= l;
//			l.answer= "Ignored due to reset";
//		}
//		while(getNumUnsentLines()>0) {
//			GCodeLine l= get((nextToSendLineNo++)-offset-1);
//			if(firstIgnored==null) firstIgnored= l;
//			l.answer= "Ignored due to reset";			
//		}
//		
//		nextToSendMaxLength= GrblComm.GRBL_RX_BUFFER_SIZE;
//		if(firstIgnored!=null)
//			tableModel.	fireTableRowsUpdated(firstIgnored.no-offset-1, size()-1);
		
		stopMotion();
	}
	
	public synchronized GCodeLine getLineNo(int i) {
		i= i-offset-1;
		while(i>=size())
			try {
				wait();
			} catch (InterruptedException e) {}
		return get(i);
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
		notifyAll();
		startMotion();
		return result; 
	}
	
	public void setDirty(int lineNo) {
		tableModel.fireTableRowsUpdated(lineNo-offset-1, lineNo-offset-1);		
	}
}
