package functionality;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map.Entry;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.text.NumberFormatter;

import org.camunda.bpm.model.bpmn.instance.DataObjectReference;
import org.camunda.bpm.model.bpmn.instance.FlowNode;

import Mapping.BPMNDataObject;
import Mapping.BPMNElement;
import Mapping.BPMNExclusiveGateway;
import Mapping.BPMNGateway;
import Mapping.BPMNParticipant;
import Mapping.ProcessInstanceWithVoters;

public class GUI {
	// small GUI where users can import a camunda bpmn model and generate the
	// optimal solutions
	static API api = null;
	static BPMNExclusiveGateway bpmnEx = null;
	static int sumVotes = 0;
	static ArrayList<Double> costForUpgradingSpheres = new ArrayList<>(Arrays.asList(10.0, 5.0, 3.0, 2.0));
	static double costForAddingReaderAfterBrt = 0.0;
	static JCheckBox compareResultsAgainstBruteForce;
	static JCheckBox jrbBruteForce;
	static JCheckBox jrbLocalMinimum;
	static JCheckBox jrbLocalMinimumWithBound;
	static JFormattedTextField jFormattedTextField1;
	static JFormattedTextField jFormattedTextField2;
	static JFormattedTextField jFormattedTextField3;
	static Dimension dimension = new Dimension(Integer.MAX_VALUE, 20);
	static JLabel localMinWithBoundLabel;



	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String pathToFile = "";
		JFileChooser chooser = new JFileChooser();

		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.showOpenDialog(null);

		int rueckgabeWert = chooser.showOpenDialog(null);
		if (rueckgabeWert == JFileChooser.APPROVE_OPTION) {
			pathToFile = chooser.getSelectedFile().getAbsolutePath();
		}

		if (pathToFile != null) {
			JFrame frame = new JFrame(pathToFile);
			frame.setLayout(new GridLayout(1, 0, 40, 30));
			frame.setVisible(true);

			JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

			try {
				api = new API(pathToFile, costForUpgradingSpheres, costForAddingReaderAfterBrt);
				panel.add(new JLabel("Troubleshooter: " + api.getTroubleShooter().getName()));
				panel.add(new JLabel("Paths through process: " + api.getAllPathsThroughProcess().size()));
				int sumConstraints = 0;
				for (BPMNElement element : api.getProcessElements()) {
					if (element instanceof BPMNExclusiveGateway) {
						sumConstraints += ((BPMNExclusiveGateway) element).getConstraints().size();
					}
				}
				panel.add(new JLabel("Amount of constraints: " + sumConstraints));
				panel.add(new JLabel("Global sphere size: " + api.getGlobalSphereList().size()));

				// create buttons for choosing the algorithm to perform
				panel.add(new JLabel("Algorithms: "));
				ArrayList<JRadioButton> listOfButtons = new ArrayList<JRadioButton>();
				compareResultsAgainstBruteForce = new JCheckBox("compareResultsAgainstBruteForce");
				compareResultsAgainstBruteForce.setEnabled(false);
				jrbBruteForce = new JCheckBox("BruteForce", true);
				jrbBruteForce.addItemListener(new ItemListener() {

					@Override
					public void itemStateChanged(ItemEvent e) {
						// TODO Auto-generated method stub
						if (e.getStateChange() == ItemEvent.SELECTED) {
							compareResultsAgainstBruteForce.setSelected(false);
							compareResultsAgainstBruteForce.setEnabled(false);
						}

					}

				});

				jrbLocalMinimum = new JCheckBox("LocalMinimum", false);

				jrbLocalMinimum.addItemListener(new ItemListener() {

					@Override
					public void itemStateChanged(ItemEvent e) {
						// TODO Auto-generated method stub
						if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
							compareResultsAgainstBruteForce.setEnabled(true);
							compareResultsAgainstBruteForce.setSelected(jrbLocalMinimum.isSelected());
						
						}

					}

				});

				jrbLocalMinimumWithBound = new JCheckBox("LocalMinimumWithBound", false);
				
				NumberFormat format = NumberFormat.getIntegerInstance();
				format.setGroupingUsed(false);

				NumberFormatter numberFormatter = new NumberFormatter(format);
				numberFormatter.setValueClass(Integer.class);
				numberFormatter.setAllowsInvalid(false); 

				jFormattedTextField1 = new JFormattedTextField(numberFormatter);
				jFormattedTextField1.setMaximumSize(dimension);
				jFormattedTextField1.setEnabled(false);
				
				jrbLocalMinimumWithBound.addItemListener(new ItemListener() {

					@Override
					public void itemStateChanged(ItemEvent e) {
						// TODO Auto-generated method stub
						if (e.getStateChange() == ItemEvent.SELECTED ) {
							compareResultsAgainstBruteForce.setEnabled(true);
							compareResultsAgainstBruteForce.setSelected(true);
							localMinWithBoundLabel.setVisible(true);
							jFormattedTextField1.setVisible(true);
							jFormattedTextField1.setEnabled(true);
							frame.pack();
						} else if(e.getStateChange() == ItemEvent.DESELECTED) {
							compareResultsAgainstBruteForce.setSelected(false);
							localMinWithBoundLabel.setVisible(false);
							jFormattedTextField1.setEnabled(false);
							jFormattedTextField1.setVisible(false);
							frame.pack();
							}
						
						
					}

				});

				compareResultsAgainstBruteForce.addItemListener(new ItemListener() {

					@Override
					public void itemStateChanged(ItemEvent e) {
						// TODO Auto-generated method stub
						if (e.getStateChange() == ItemEvent.SELECTED) {
							if (jrbBruteForce.isSelected()) {
								compareResultsAgainstBruteForce.setSelected(false);
							}
						}

					}

				});

				ButtonGroup jCheckBoxGroup = new ButtonGroup();
				jCheckBoxGroup.add(jrbBruteForce);
				jCheckBoxGroup.add(jrbLocalMinimum);
				jCheckBoxGroup.add(jrbLocalMinimumWithBound);
				
				panel.add(jrbBruteForce);
				panel.add(jrbLocalMinimum);
				panel.add(jrbLocalMinimumWithBound);
				localMinWithBoundLabel = new JLabel("Enter bound for localMinimumWithBound Algorithm: ");
				panel.add(localMinWithBoundLabel);
				localMinWithBoundLabel.setVisible(false);

				panel.add(jFormattedTextField1);
				jFormattedTextField1.setVisible(false);			

				frame.add(panel);

				// next column add the settings
				JPanel settingsPanel = new JPanel();
				settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.PAGE_AXIS));
				settingsPanel.add(new JLabel("Enter max amount new models to be stored (0... all models):"));
				
				JFormattedTextField jFormattedTextField2 = new JFormattedTextField(numberFormatter);
				jFormattedTextField2.setText("0");
				jFormattedTextField2.setMaximumSize(dimension);
				settingsPanel.add(jFormattedTextField2);
				
				JCheckBox votingAsConstructBox = new JCheckBox("VotingAsConstruct", true);
				JCheckBox votingAsAnnotationBox = new JCheckBox("VotingAsAnnotation", false);
				ButtonGroup jCheckBoxGroupSettings = new ButtonGroup();
				jCheckBoxGroupSettings.add(votingAsAnnotationBox);
				jCheckBoxGroupSettings.add(votingAsConstructBox);

				settingsPanel.add(compareResultsAgainstBruteForce);
				settingsPanel.add(votingAsConstructBox);
				settingsPanel.add(votingAsAnnotationBox);
				
				settingsPanel.add(new JLabel("Enter timeout for algorithms in seconds:"));
				
				
				JFormattedTextField jFormattedTextField3 = new JFormattedTextField(numberFormatter);
				jFormattedTextField3.setText("30");
				jFormattedTextField3.setMaximumSize(dimension);

				settingsPanel.add(jFormattedTextField3);		
				
				settingsPanel.add(new JLabel("Enter amount of threads used for computing solutions:"));
								
				JFormattedTextField jFormattedTextField4 = new JFormattedTextField(numberFormatter);
				jFormattedTextField4.setText("1");
				jFormattedTextField3.setMaximumSize(dimension);

				settingsPanel.add(jFormattedTextField4);		
				
				JButton runButton = new JButton("RUN");
				runButton.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent ae) {
						// TODO Auto-generated method stub
						//check which algorithm will be performed
						// read timeout value 
						// read amount of models to create
						
						
						if(jrbBruteForce.isSelected()) {
							LinkedList<ProcessInstanceWithVoters> bruteForceSolutions = null;
							try {
								bruteForceSolutions = api.bruteForceAlgorithm();

							} catch(Exception e) {
								panel.add(new JLabel("Error: "+e.getMessage()));
							}
						}
					}
					
				});
				
				
				
				settingsPanel.add(runButton);
				frame.add(settingsPanel);

			} catch (Exception e2) {
				// TODO Auto-generated catch block
				// System.err.println("Error found: ");
				// e2.printStackTrace();
				panel.add(new JLabel("Error: " + e2.getMessage()));
				frame.add(panel);

			} finally {
				frame.pack();
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			}
		}
	}
}
