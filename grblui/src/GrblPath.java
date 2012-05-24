import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;


public class GrblPath {
    String line;
    private OutputStream stdin = null;
    private InputStream stderr = null;
    private InputStream stdout = null;
    private BufferedReader stdoutReader; 
    private Process process;
    
    public GrblPath() {
    	// launch EXE and grab stdin/stdout and stderr
    	try {
    		ProcessBuilder builder = new ProcessBuilder("a.exe");
    		builder.redirectErrorStream(true);
    		process = builder.start();

//    		process = Runtime.getRuntime ().exec ("a.exe");
    		stdin = process.getOutputStream ();
    		stderr = process.getErrorStream ();
    		stdout = process.getInputStream ();
    	} catch (IOException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	}
    	stdoutReader= new BufferedReader(new InputStreamReader(stdout));
    	String answer= null;
		try {
			while(true) {
				answer= stdoutReader.readLine();
				if(answer==null || answer.length()==0) {
					if(answer==null)
						System.out.println("null line");
					else
						System.out.println("empty line");
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {}
				} else {
					System.out.println(answer);
				}
				if(answer.startsWith("'$'")) break;
//				if(answer!=null) System.out.println(answer.toCharArray());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//			if(stdout.available()<=0)
//				try {
//					Thread.sleep(20);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//    		while(stdout.available()>0) System.out.print((char)stdout.read());
//    		System.out.println();
   	
    }

    public String sendLine(String line) {
    	String answer= null;
    	try {
			stdin.write((line+ "\n").getBytes() );
			stdin.flush();
//			if(stdout.available()<=0)
//				try {
//					Thread.sleep(200);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//    		while(stdout.available()>0) System.out.print((char)stdout.read());
//    		System.out.println();
//			int i= 0;
			while(true) {
				answer= stdoutReader.readLine();
				if(answer==null || answer.length()==0) {
					if(answer==null)
						System.out.println("null line");
					else
						System.out.println("empty line");
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {}
				} else {
					System.out.println(": " + answer);
					if(!answer.startsWith("  block")) break;
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return answer;
    }

    
    
    public void end() throws Throwable {
    	System.out.println("finalize");
        try {
			stdin.close();
//			stdout.close();
			stdoutReader.close();
			stderr.close();
			process.destroy();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
