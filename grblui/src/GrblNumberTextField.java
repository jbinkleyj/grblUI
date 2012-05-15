import java.awt.Color;
import java.text.ParseException;

import javax.swing.JFormattedTextField;
import javax.swing.text.InternationalFormatter;


public class GrblNumberTextField extends JFormattedTextField {
	private static final long serialVersionUID = 1L;
	private double originalValue;

	public GrblNumberTextField(InternationalFormatter formatter, double originalValue) {
		super(formatter);
		addFocusListener(new SelectAllOfFocus());
		this.originalValue= originalValue;
	}

	@Override
	public void commitEdit() throws ParseException {
        try {
            super.commitEdit();
            // Give it a chance to reformat.
            
            setValue(getValue());
            if(Math.abs(originalValue-getDoubleValue())>1e-6)
            	setForeground(Color.RED);
            else
            	setForeground(Color.BLACK);
            	
        } catch (ParseException pe) {
            setValue(getValue());
        }
	}

	public double getDoubleValue() {
		Number val= (Number)getValue();
		return val.doubleValue();
	}
	
	public void resetOriginalValue() {
		originalValue= getDoubleValue();
		setForeground(Color.BLACK);
	}
}
