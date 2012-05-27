
public class GCodeLine {
	public int no;
	public String line;
	public boolean sent= false;
	public boolean sentSim= false;
	public String answer= "";
	public String answerSim= "";
	public boolean done= false;
	public Integer[] steps;
	
	public GCodeLine(String line) {
		super();
		this.line = line;
	}
}
