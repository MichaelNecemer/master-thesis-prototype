package functionality;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;

import Mapping.BPMNBusinessRuleTask;
import Mapping.BPMNDataObject;
import Mapping.BPMNElement;
import Mapping.BPMNExclusiveGateway;
import Mapping.BPMNParticipant;
import Mapping.BPMNTask;

public class Main {

	static API a2 = null;
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		/*
		JFileChooser chooser = new JFileChooser();
		  
		 chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		 chooser.showOpenDialog(null);
		  
		 int rueckgabeWert = chooser.showOpenDialog(null); 
		 if(rueckgabeWert == JFileChooser.APPROVE_OPTION) {			 
			a2 = new API(
		 chooser.getSelectedFile().getAbsolutePath());
		  
		 }
		*/
		 API a2 = new
		API("C:\\Users\\Micha\\git\\master-thesis-prototype\\master-thesis-prototype\\src\\main\\resources\\process.bpmn");
/*	API a2 = new API(
		"C:\\Users\\Micha\\git\\master-thesis-prototype\\master-thesis-prototype\\src\\main\\resources\\process1.bpmn");
	*/	
		for (BPMNBusinessRuleTask brt : a2.getBusinessRuleTasks()) {

			HashMap<BPMNDataObject, ArrayList<BPMNTask>> participantList = new HashMap<BPMNDataObject, ArrayList<BPMNTask>>();

			BPMNExclusiveGateway bpmnEx = (BPMNExclusiveGateway) brt.getSuccessors().iterator().next();

			JFrame frame = new JFrame("GUITest");
			frame.setVisible(true);

			JPanel panel = new JPanel();
			panel.add(new JLabel("CHOOSE VOTERS"));
			panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
			
			
			for(Entry<BPMNDataObject, Integer> voters: bpmnEx.getVoters().entrySet()) {
				panel.add(new JLabel(voters.getValue()+" needed for "+voters.getKey().getName()));
			}
			
			
			ArrayList<JCheckBoxWithId> checkboxes = new ArrayList<JCheckBoxWithId>();

			for (BPMNTask t : brt.getLastWriterList()) {
				panel.add(new JLabel("Writer: " + t.getName()));
				for (Entry<BPMNDataObject, ArrayList<BPMNTask>> t2 : t.getSDHashMap().entrySet()) {
					panel.add(new JLabel(t2.getKey().getName() + " Strong-Dynamic:"));
					for (BPMNTask task : t2.getValue()) {
						JCheckBoxWithId box = new JCheckBoxWithId(
								task.getParticipant().getName() + ", " + task.getName(), t, t.getSDHashMap(), task);
						checkboxes.add(box);
						panel.add(box);
					}
				}

				for (Entry<BPMNDataObject, ArrayList<BPMNTask>> t2 : t.getWDHashMap().entrySet()) {
					panel.add(new JLabel(t2.getKey().getName() + " Weak-Dynamic:"));
					for (BPMNTask task : t2.getValue()) {
						JCheckBoxWithId box = new JCheckBoxWithId(
								task.getParticipant().getName() + ", " + task.getName(), t, t.getWDHashMap(), task);
						checkboxes.add(box);
						panel.add(box);
					}
				}

			}

			JButton button = new JButton("Add marked participants to voting");
			button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					int count = 0;
					for (JCheckBoxWithId checkBox : checkboxes) {
						if (checkBox.isSelected()) {
							count++;
							for (BPMNDataObject dataKey : checkBox.map.keySet()) {
								if (participantList.get(dataKey) == null) {
									ArrayList<BPMNTask> list = new ArrayList<BPMNTask>();
									list.add(checkBox.oneTaskOutOfMap);
									participantList.put(dataKey, list);
								} else {
									participantList.get(dataKey).add(checkBox.oneTaskOutOfMap);
								}

							}

						}
					}

					if (count == bpmnEx.getCumulatedVoters()) {
						try {
							a2.addVotingTasksToProcess(brt, participantList);
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					} else {
						for (JCheckBoxWithId checkBox : checkboxes) {
							checkBox.setSelected(false);
						}
						participantList.clear();
						JOptionPane.showMessageDialog(null,
								"Select at least " + bpmnEx.getCumulatedVoters() + " participants!",
								"InfoBox: " + "Selection failed!", JOptionPane.INFORMATION_MESSAGE);

					}

				}

			});

			panel.add(button);
			frame.add(panel);
			
			frame.pack();
		}

	}

}
