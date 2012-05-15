
public class GCodeLine {
	public int no;
	public String line;
	public boolean sent= false;
	public String answer= "";
	public boolean done= false;
	
	public GCodeLine(String line) {
		super();
		this.line = line;
	}
}
