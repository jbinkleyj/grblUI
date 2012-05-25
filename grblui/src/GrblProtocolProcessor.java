
public interface GrblProtocolProcessor {
	public void newPosition(Float[] pos);
	public void verStrReceived(String verStr);
	public void settingReceived(int idx, Double val, String lable);
	public void lineSent(GCodeLine line);
	
}
