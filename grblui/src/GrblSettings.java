import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.text.InternationalFormatter;


public class GrblSettings extends ArrayList<GrblParameter>{
	private static final long serialVersionUID = 1L;
	private GrblComm grblComm;

	public GrblSettings(GrblComm grblComm) {
		super();
		this.grblComm = grblComm;
	}

	public boolean match(String verString) {
		Iterator<GrblParameter> pIter= this.iterator();
		while(pIter.hasNext()) {
			GrblParameter p= pIter.next();
			p.jLabel= new JLabel(p.lable);
			InternationalFormatter formatter = new InternationalFormatter(new DecimalFormat(p.formatStr));
			if(p.max!=null) formatter.setMaximum(p.max);
			if(p.min!=null) formatter.setMinimum(p.min);
			
			p.input= new GrblNumberTextField(formatter, p.val.doubleValue());
			p.input.setValue(p.val);
			p.input.setColumns(10);
//			p.input.setMinimumSize(p.input.getPreferredSize());
//			p.input.setMaximumSize(p.input.getPreferredSize());
			p.input.setHorizontalAlignment(JTextField.RIGHT);
		}
		
    	return true;
    }
    
	public String set(int idx, Double val, String lable) {
		GrblParameter p= null;
		try {
			while(size()<=idx) add(null);
			p = set(idx, new GrblParameter(val, lable));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(p==null) return null;
		else return p.lable;
	}
	
	public void revert() {
		Iterator<GrblParameter> pIter= this.iterator();
		while(pIter.hasNext()) {
			GrblParameter p= pIter.next();
			p.input.setValue(p.val);
			p.input.resetOriginalValue();
		}
	}
	
	public void apply() {
		for(int i=0 ; i<size(); i++) {
			GrblParameter p= this.get(i);
			if(p.input.getDoubleValue()!=p.val) {
				grblComm.setParameter(i, p.input.getDoubleValue());
				p.val= p.input.getDoubleValue();
				p.input.resetOriginalValue();
			}
		}		
	}
	
	public double get(String name) {
		return 0.0;
	}
}
