import javax.swing.JLabel;


public class GrblParameter {
	public String lable;
	public Double val;
	public Double min= null;
	public Double max= null;
	public String formatStr= "0.00";
	public JLabel jLabel;
	public GrblNumberTextField input;

	public GrblParameter(Double val, String lable) {
		super();
		this.lable = lable;
		this.val = val;
	}
}
