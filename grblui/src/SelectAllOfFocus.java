import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JFormattedTextField;


public class SelectAllOfFocus extends FocusAdapter {
    public void focusGained(FocusEvent e) {
        if (! e.isTemporary()) {
            JFormattedTextField textField=  (JFormattedTextField)e.getSource(); // Get your text field here, it depends on your own code
            // This is needed to put the text field in edited mode, so that its processFocusEvent doesn't
            // do anything. Otherwise, it calls setValue, and the selection is lost.
            textField.setText(textField.getText());
            textField.selectAll();
        }
    }
}
