import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;


public class BinaryNumberFormat extends NumberFormat {
	private static final long serialVersionUID = -7212729566407962410L;

    public static final int BYTE = 2*4;
    public static final int WORD = 4*4;
    public static final int DWORD = 8*4;
    public static final int QWORD = 16*4;
    private int m_numDigits = BYTE;

    public BinaryNumberFormat() {
        this(BYTE);
    }

    public BinaryNumberFormat(int digits) {
        super();
        this.m_numDigits = digits;
    }

    public final int getNumberOfDigits() {
        return this.m_numDigits;
    }

    public void setNumberOfDigits(int digits) {
        this.m_numDigits = digits;
    }

    public StringBuffer format(double number, StringBuffer toAppendTo,
            FieldPosition pos) {
        return format((long) number, toAppendTo, pos);
    }

    public StringBuffer format(long number, StringBuffer toAppendTo,
            FieldPosition pos) {
        String l_bin = Long.toBinaryString(number);

        int l_pad = this.m_numDigits - l_bin.length();
        l_pad = (0 < l_pad) ? l_pad : 0;

        StringBuffer l_extended = new StringBuffer();
        for (int i = 0; i < l_pad; i++) {
            l_extended.append(0);
        }
        l_extended.append(l_bin);

        return l_extended;
    }

    public Number parse (String source, ParsePosition parsePosition) {
//    	System.out.println("parsing " + source  + "@" + parsePosition.getIndex());
    	long l= 0;
    	boolean anyFound= false;
    	int i= parsePosition.getIndex();
    	for(; i<source.length(); i++) {
    		int digit;
    		if(source.charAt(i)=='1') digit= 1;
    		else if(source.charAt(i)=='0') digit= 0;
    		else break;
    		anyFound= true;
    		l= (l<<1) + digit;
    		
    	}
    	parsePosition.setIndex(i);
//    	System.out.println(anyFound + "parsed " + l  + "to" + parsePosition.getIndex());
    	if(anyFound) return (Number)new Long(l);
    	else return null;
    }
}
