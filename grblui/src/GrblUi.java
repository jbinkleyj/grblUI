import gnu.io.CommPortIdentifier;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Rectangle;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;



public class GrblUi implements CNCPositionListener, MotionChangeListener, ChangeListener {

	private JFrame frame;
	private JTextField jtfInput;
	private static GrblCommHistoryTableModel grblCommHistory;
	private JTable table;
	private static GrblSerialComm grblComm;
	private JSpinner[] posSpinner= new JSpinner[3];
	private JButton[] posReset= new JButton[3];
	private JButton stopButton;
	private JButton playButton;
	private JButton pauseButton;
	JTabbedPane tabbedPane;
    public GrblSettings settings;
	private Timer timer;
	private int lastRowScroll= -1;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		String port= getComPort();
		if(port!=null) {
			String verString= null;
			try {
				grblCommHistory= new GrblCommHistoryTableModel();
				grblComm= new GrblSerialComm(grblCommHistory.data);
				verString= grblComm.connect(port);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}

			if(verString!=null) {
				int settingsOK= grblComm.getSettings();
				if(settingsOK==GrblSerialComm.SETTINGS_GOTTEN) {
//					Iterator<GrblParameter> pIter= grblComm.settings.iterator();
//					while(pIter.hasNext())
//						System.out.println(pIter.next().lable);
					
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							try {
								GrblUi window = new GrblUi(grblComm.verString);
								grblComm.setPositionListener(window);
								grblCommHistory.data.setMotionChangeListener(window);
								window.frame.setVisible(true);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					});
				} else {
					switch(settingsOK) {
					case GrblSerialComm.SETTINGS_MISMATCH:
						JOptionPane.showMessageDialog(null, "Error: mismatch between grbl version and settings", "Grbl Error", JOptionPane.ERROR_MESSAGE);
					case GrblSerialComm.SETTINGS_TIME_OUT:
						JOptionPane.showMessageDialog(null, "Error: time out getting grbl settings", "Grbl Error", JOptionPane.ERROR_MESSAGE);						
					}					
				}
			} // else
//				JOptionPane.showMessageDialog(null, "Error: time out getting grbl version", "Grbl Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	public GrblUi(String verString) {
		initialize(verString);
	}

	private void initialize(String verString) {
		frame = new JFrame("grblUI (grbl version " + verString + ")");
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel commandPane = new JPanel(new BorderLayout());
		tabbedPane = new JTabbedPane();
		
		JPanel inputPane= new JPanel();
		inputPane.setLayout(new BoxLayout(inputPane, BoxLayout.LINE_AXIS));
		
		jtfInput = new JTextField(20);
		jtfInput.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				String text = jtfInput.getText();
				grblCommHistory.addLine(text);
				jtfInput.setText("");
			}
			
		});
		class DropLinesTarget extends DropTarget {
			private static final long serialVersionUID = 1L;
			
	        public synchronized void drop(DropTargetDropEvent evt) {
	            try {
	                evt.acceptDrop(DnDConstants.ACTION_COPY);
	                String s= (String)evt.getTransferable().getTransferData(DataFlavor.stringFlavor);
	                addLines(s.split("\\r?\\n"));
	            } catch (Exception ex) {
	                ex.printStackTrace();
	            }
	        }
	    };
		jtfInput.setDropTarget(new DropLinesTarget());

		jtfInput.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ctrl pressed V"), "paste-from-clipboard");
		class PasteLinesAction extends AbstractAction {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				Clipboard clipboard= jtfInput.getToolkit().getSystemClipboard();
	            Transferable clipData = clipboard.getContents(this);
	            try {
	            	String[] s = ((String) (clipData.getTransferData(DataFlavor.stringFlavor))).split("\\r?\\n");
	            	addLines(s);
	            } catch (Exception ex) {}
			}
		}		
		jtfInput.getActionMap().put("paste-from-clipboard", new PasteLinesAction());
		inputPane.add(jtfInput);
		
		JButton openFile= new JButton("file");
		openFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				JFileChooser fc = new JFileChooser();
				int returnVal = fc.showOpenDialog(table);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
		            File file = fc.getSelectedFile();
		            LinkedList<String> lines= new LinkedList<String>();
					try {
						BufferedReader input = new BufferedReader(
								new FileReader(file));
						try {
							String line = null; // not declared within while
												// loop
							while ((line = input.readLine()) != null) {
								lines.add(line);
							}
							String[] s= new String[1];
							addLines((String[])lines.toArray(s));
						} finally {
							input.close();
						}
					} catch (IOException ex) {
						JOptionPane.showMessageDialog(null,
								"Error: Something went wrong while reading the file " + file.getName(),
								"File read error",
								JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});
		
		inputPane.add(Box.createRigidArea(new Dimension(10, 0)));
		inputPane.add(openFile);
		
		commandPane.add(inputPane, BorderLayout.PAGE_END);
	
		table = new JTable(grblCommHistory);
		table.setFillsViewportHeight(true);
		table.getColumnModel().getColumn(0).setMaxWidth(45);
		table.getColumnModel().getColumn(0).setMinWidth(45);
		table.getColumnModel().getColumn(1).setPreferredWidth(200);
		table.getColumnModel().getColumn(2).setMaxWidth(35);
		table.getColumnModel().getColumn(2).setMinWidth(35);
		table.getColumnModel().getColumn(4).setMaxWidth(35);
		table.getColumnModel().getColumn(4).setMinWidth(35);
		JScrollPane scrollPane = new JScrollPane(table,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		
		commandPane.add(scrollPane, BorderLayout.CENTER);
		
		table.getActionMap().put("copy", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				grblCommHistory.copyLines(table.getSelectedRows(), table.getToolkit().getSystemClipboard());
			}
		});
		
		table.addMouseListener(new MouseAdapter(){
		    @Override
		    public void mouseClicked(MouseEvent e){
		        if(e.getClickCount()==2){
		        	String[] s= grblCommHistory.getSelectedLines(table.getSelectedRows());
		        	if(s!=null)
		        		addLines(s);
		        }
		    }
		});

		table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).getParent().remove(KeyStroke.getKeyStroke("ctrl V"));
		
		JPanel xyzPane= new JPanel(new GridLayout(2, 3));
		xyzPane.add(new JLabel("WPos. X"));
		xyzPane.add(new JLabel("WPos. Y"));
		xyzPane.add(new JLabel("WPos. Z"));
		for(int i= 0; i<3; i++) {
			JPanel posPane= new JPanel();
			posPane.setLayout(new BoxLayout(posPane, BoxLayout.LINE_AXIS));
			
			posPane.add(posSpinner[i]= new JSpinner(new SpinnerNumberModel(0, -10000, 10000, 0.1)));
			posSpinner[i].setEditor(new JSpinner.NumberEditor(posSpinner[i], "0.00"));
			posSpinner[i].addChangeListener(this);
			posPane.add(Box.createRigidArea(new Dimension(2, 0)));
			
			posReset[i]= new JButton("reset");
			posReset[i].addActionListener(new ResetAction(i));
			posPane.add(posReset[i]);			
			posPane.add(Box.createRigidArea(new Dimension(5, 0)));
			xyzPane.add(posPane);
		}
		
		
		playButton= new JButton(new ImageIcon(this.getClass().getResource("/toolbarButtonGraphics/media/Play24.gif")));
		playButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
//				System.out.println("playButton");
				grblComm.start();
//				playButton.setVisible(false);
//				pauseButton.setVisible(true);					
			}
		});
		
		pauseButton= new JButton(new ImageIcon(this.getClass().getResource("/toolbarButtonGraphics/media/Pause24.gif")));
//		pauseButton.setVisible(false);
		pauseButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
//				System.out.println("playButton");
				grblComm.pause();
//				playButton.setVisible(true);
//				pauseButton.setVisible(false);					
			}
		});
		
		stopButton= new JButton(new ImageIcon(this.getClass().getResource("/toolbarButtonGraphics/media/Stop24.gif")));
		stopButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				grblComm.reset();
//				playButton.setVisible(true);
//				pauseButton.setVisible(false);					
			}
		});
		

		JPanel playStopPane= new JPanel();
		JPanel centerPane= new JPanel(new GridBagLayout());
		
		GroupLayout layout = new GroupLayout(playStopPane);
		playStopPane.setLayout(layout);
		
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		GroupLayout.SequentialGroup hGroup = layout.createSequentialGroup();
		GroupLayout.ParallelGroup vGroup = layout.createParallelGroup();
		hGroup.addComponent(stopButton).addComponent(playButton).addComponent(pauseButton);
//		hGroup.addComponent(stopButton).addGroup(layout.createParallelGroup().addComponent(playButton).addComponent(pauseButton));
		vGroup.addComponent(stopButton).addComponent(playButton).addComponent(pauseButton);
		
		layout.setHorizontalGroup(hGroup);
		layout.setVerticalGroup(vGroup);
		
		centerPane.add(playStopPane, new GridBagConstraints());
		JPanel topPanel= new JPanel(new BorderLayout());
		topPanel.add(xyzPane, BorderLayout.PAGE_START);
		topPanel.add(centerPane, BorderLayout.CENTER);
		
		commandPane.add(topPanel, BorderLayout.PAGE_START);
		
		tabbedPane.addTab("Command", null, commandPane,
                "Send gcode commands to grbl");
		
		tabbedPane.addTab("Settings", null, createSettingsPanel(),
                        "Manage grlb settings");
		
		frame.getContentPane().add(tabbedPane);
		frame.pack();
		
		timer = new Timer(100, new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				int row= grblCommHistory.data.getLastSentLineTableIdx();
				if(row>=0 && row!=lastRowScroll) {
					table.scrollRectToVisible(new Rectangle(table.getCellRect(row, 0, true)));
					lastRowScroll= row;
				}
			}
		});
		timer.setInitialDelay(1000);
		timer.start(); 
	}
	
	private JPanel createSettingsPanel() {
		JPanel panel = new JPanel();
		GroupLayout layout = new GroupLayout(panel);
		panel.setLayout(layout);

		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);

		GroupLayout.ParallelGroup hGroup = layout.createParallelGroup(GroupLayout.Alignment.TRAILING);
		GroupLayout.SequentialGroup inGroup = layout.createSequentialGroup();
		ParallelGroup hLabels = layout.createParallelGroup();
		ParallelGroup hInputs = layout.createParallelGroup();
		Iterator<GrblParameter> pIter = grblComm.settings.iterator();
		while (pIter.hasNext()) {
			GrblParameter p = pIter.next();
			hLabels.addComponent(p.jLabel);
			hInputs.addComponent(p.input, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
			          GroupLayout.PREFERRED_SIZE);
		}
		inGroup.addGroup(hLabels);
		inGroup.addGroup(hInputs);
		hGroup.addGroup(inGroup);

		GroupLayout.SequentialGroup vGroup = layout.createSequentialGroup();
		ParallelGroup vLine;
		pIter = grblComm.settings.iterator();
		while (pIter.hasNext()) {
			GrblParameter p = pIter.next();
			vLine= layout.createParallelGroup(Alignment.BASELINE);
			vLine.addComponent(p.jLabel);
			vLine.addComponent(p.input);
			vGroup.addGroup(vLine);
		}

		JButton apply= new JButton("Apply");
		apply.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				grblComm.settings.apply();
			}
		});
		JButton revert= new JButton("Revert");
		revert.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				grblComm.settings.revert();
			}
		});
		hGroup.addGroup(layout.createSequentialGroup().addComponent(apply).addComponent(revert));
		vGroup.addGroup(layout.createParallelGroup(Alignment.BASELINE).addComponent(apply).addComponent(revert));
		
		layout.setHorizontalGroup(hGroup);
		layout.setVerticalGroup(vGroup);
		return panel;
	}
	
	private static String getComPort() {
//		return new String("COM7");
		LinkedList<String> list = new LinkedList<String>();
		Enumeration thePorts = CommPortIdentifier.getPortIdentifiers();
        while (thePorts.hasMoreElements()) {
            CommPortIdentifier com = (CommPortIdentifier) thePorts.nextElement();
            if(com.getPortType()==CommPortIdentifier.PORT_SERIAL) {
            	list.add(com.getName());
            }
        }
        if(list.size()==0) {
        	JOptionPane.showMessageDialog(null, "Error: No COM ports available", "Com Port Error", JOptionPane.ERROR_MESSAGE);
        	return null;
        }
        return (String)JOptionPane.showInputDialog(
                            null,
                            "Please select the COM port that grbl is connected to",
                            "Select COM port",
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            list.toArray(),
                            list.get(0));

	}
	
	public void updatePosition(Float[] pos) {
		for(int i= 0; i<3; i++) {
			if(posSpinner[i]!=null)
				posSpinner[i].setValue(pos[i]);
		}
	}

	public void motionChanged(boolean inMotion) {
		for(int i= 0; i<3; i++) {
			if(posSpinner[i]!=null)
				posSpinner[i].setEnabled(!inMotion);
			if(posReset[i]!=null)
				posReset[i].setEnabled(!inMotion);
		}
		if(tabbedPane!=null) tabbedPane.setEnabledAt(1, !inMotion);
	}

	private class ResetAction extends AbstractAction {
		private static final long serialVersionUID = 1L;
		private int xyz;

		public ResetAction(int xyz) {
			super();
			this.xyz = xyz;
		}

		public void actionPerformed(ActionEvent e) {
//			System.out.println("G92" + ('X'+xyz) + "0");
			grblCommHistory.addLine("G92" + ((char)('X'+xyz)) + "0");
		}
	}

	public void stateChanged(ChangeEvent e) {
//		System.out.println("stateChanged");
		if(e.getSource() instanceof JSpinner) {
//			System.out.println("stateChanged JSpinner");
			JSpinner thisSpinner = (JSpinner)(e.getSource());
			for(int i= 0; i<3; i++) {
				if(thisSpinner==posSpinner[i]) {
					Float newPos= Float.parseFloat(thisSpinner.getValue().toString());
//					System.out.println("stateChanged JSpinner to " + newPos);
					if(thisSpinner.isEnabled() && Math.abs(newPos-grblComm.currPos[i])>1e-6) {
						grblCommHistory.addLine("G0" + ((char)('X'+i)) + newPos.toString());
					}
					break;
				}
			}
		}
	}

	public void addLines(String[] s) {
		int maxLines= grblCommHistory.maxLinesFree();
		if(s.length==1)
			jtfInput.setText(s[0]);
		
		else if(s.length>maxLines) {
			JOptionPane.showMessageDialog(null, "Error: Currently no more than " + maxLines + " can be pasted", "Multi line send error", JOptionPane.ERROR_MESSAGE);
		} else
			for(int i= 0; i<s.length; i++) {
				grblCommHistory.addLine(s[i]);
			}
	}
}
