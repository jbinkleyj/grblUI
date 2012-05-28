import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.LinkedList;

import javax.swing.table.AbstractTableModel;


public class GrblCommHistoryTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;
	
	private static final int MAX_LINES= 1000;
	private String[] columnNames = {"No", "GCode line", "Sent", "Answer", "SimSent", "SimAnswer", "Steps", "Done"};
	public static final int NO_ROW= 0;
	public static final int LINE_ROW= 1;
	public static final int SENT_ROW= 2;
	public static final int ANSWER_ROW= 3;
	public static final int SENT_SIM_ROW= 4;
	public static final int ANSWER_SIM_ROW= 5;
	public static final int STEPS= 6;
	public static final int DONE_ROW= 7;
	
    public GCodeLineBuffer data;

    public GrblCommHistoryTableModel() {
		super();
		data= new GCodeLineBuffer(this);
	}

	public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
        return data.size();
    }

    public String getColumnName(int col) {
        return columnNames[col];
    }

    public Object getValueAt(int row, int col) {
    	GCodeLine line= data.get(row);
    	switch(col) {
    	case NO_ROW:
    		return line.no;
    	case LINE_ROW:
    		return line.line;
    	case SENT_ROW:
    		return line.sent;
    	case ANSWER_ROW:
    		return line.answer;
    	case DONE_ROW:
    		return line.done;
    	case SENT_SIM_ROW:
    		return line.sentSim;
    	case ANSWER_SIM_ROW:
    		return line.answerSim;
    	case STEPS:
    		if(line.steps==null) return null;
    		else {
    			String s= line.steps[0].toString() +  ", " + line.steps[1].toString() +  ", " + line.steps[2].toString();
    			return s;
    		}
    	default:
    		return null;
    	}
    }

    public void addLine(String line) {
    	if(data.size()>=MAX_LINES) {
    		data.removeFirst();
    		this.fireTableRowsDeleted(0, 0);
    	}
    	data.add(new GCodeLine(line));
    	this.fireTableRowsInserted(data.size()-1, data.size()-1);
    }
    
    public int maxLinesFree() {
    	return MAX_LINES-data.getNumUnsentLines();
    }
    
    public String[] getSelectedLines(int[] selectedLines) {
    	if(selectedLines.length>0) {
    		LinkedList<String> lines= new LinkedList<String>();
    		
        	for(int i= 0; i<selectedLines.length; i++) {
        		lines.add(data.get(selectedLines[i]).line);
        	}
        	String[] s= new String[1];
        	return (String[])lines.toArray(s);
    	} else
    		return null;
    }
    
    public void copyLines(int[] selectedLines, Clipboard clipboard) {
//    	System.out.println(selectedLines.length);
    	if(selectedLines.length>0) {
    		String s= new String();
        	for(int i= 0; i<selectedLines.length; i++) {
        		s= s + data.get(selectedLines[i]).line + "\n";
        	}
            StringSelection data = new StringSelection(s);
            
            clipboard.setContents(data, data);    		
    	}
    }
    
    public Class getColumnClass(int c) {
    	switch(c) {
    	case NO_ROW:
    		return Number.class;
    	case LINE_ROW:
    		return String.class;
    	case SENT_ROW:
    		return Boolean.class;
    	case ANSWER_ROW:
    		return String.class;
    	case DONE_ROW:
    		return Boolean.class;
    	case SENT_SIM_ROW:
    		return Boolean.class;
    	case ANSWER_SIM_ROW:
    		return String.class;
    	case STEPS:
    		return String.class;
    	default:
    		return String.class;
    	}
    }

    public boolean isCellEditable(int row, int col) {
    	return false;
    }
}
