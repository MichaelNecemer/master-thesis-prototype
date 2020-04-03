package functionality;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
	static BPMNExclusiveGateway bpmnEx = null;
	static int sumVotes = 0;

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
		API("C:\\Users\\Micha\\git\\master-thesis-prototype\\master-thesis-prototype\\src\\main\\resources\\process3.bpmn");
/*	API a2 = new API(
		"C:\\Users\\Micha\\git\\master-thesis-prototype\\master-thesis-prototype\\src\\main\\resources\\process1.bpmn");
	*/	
		 LinkedHashMap<BPMNBusinessRuleTask, HashMap<BPMNDataObject, ArrayList<BPMNTask>>> participantList = new LinkedHashMap<BPMNBusinessRuleTask, HashMap<BPMNDataObject, ArrayList<BPMNTask>>>();
		 JFrame frame = new JFrame("GUITest");
			frame.setVisible(true);
			frame.setLayout(new GridLayout(0,a2.getBusinessRuleTasks().size(), 10,0));
			
						
			ArrayList<JCheckBoxWithId> checkboxes = new ArrayList<JCheckBoxWithId>();
			
		for (BPMNBusinessRuleTask brt : a2.getBusinessRuleTasks()) {
			JPanel panel = new JPanel();
			
			panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
			 bpmnEx = (BPMNExclusiveGateway) brt.getSuccessors().iterator().next();
			 panel.add(new JLabel("CHOOSE VOTERS FOR EXCLUSIVE GATEWAY "+bpmnEx.getName()));
			for(Entry<BPMNDataObject, Integer> voters: bpmnEx.getVoters().entrySet()) {
				panel.add(new JLabel(voters.getValue()+" needed for "+voters.getKey().getName()));
			}

			for (BPMNTask t : brt.getLastWriterList()) {
				
				for (Entry<BPMNBusinessRuleTask, HashMap<BPMNDataObject, ArrayList<BPMNTask>>> t2 : t.getSDHashMap().entrySet()) {
					if(t2.getKey().getId().equals(brt.getId())) {
					for(Entry<BPMNDataObject, ArrayList<BPMNTask>> entry: t2.getValue().entrySet()) {
						panel.add(new JLabel("Writer: " + t.getName() +" to "+entry.getKey().getName()));
						panel.add(new JLabel("Reader: "+ t2.getKey().getName() + "is in Strong-Dynamic Sphere:"));
					
					for (BPMNTask task : entry.getValue()) {
						JCheckBoxWithId box = new JCheckBoxWithId(
								task.getParticipant().getName() + ", " + task.getName(), t, t.getSDHashMap(), task, bpmnEx);
						checkboxes.add(box);
						panel.add(box);
					}
					}
				}
				}
				for (Entry<BPMNBusinessRuleTask, HashMap<BPMNDataObject, ArrayList<BPMNTask>>> t2 : t.getWDHashMap().entrySet()) {
					if(t2.getKey().getId().equals(brt.getId())) {
					for(Entry<BPMNDataObject, ArrayList<BPMNTask>> entry: t2.getValue().entrySet()) {
						panel.add(new JLabel("Writer: " + t.getName() +" to "+entry.getKey().getName()));
						panel.add(new JLabel("Reader: "+ t2.getKey().getName() + "is in Weak-Dynamic Sphere:"));
					for (BPMNTask task : entry.getValue()) {
						JCheckBoxWithId box = new JCheckBoxWithId(
								task.getParticipant().getName() + ", " + task.getName(), t, t.getWDHashMap(), task, bpmnEx);
						checkboxes.add(box);
						panel.add(box);
					}
				}
				}
			}
			}
			sumVotes+=bpmnEx.getCumulatedVoters();
			frame.add(panel);
			frame.pack();
	}
		

		JButton button = new JButton("Add marked participants to voting");
		ArrayList<JButton> buttonList = new ArrayList<JButton>();
		buttonList.add(button);
		
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int count = 0;
				for (JCheckBoxWithId checkBox : checkboxes) {
					if (checkBox.isSelected()) {
						count++;
						for(Entry<BPMNBusinessRuleTask, HashMap<BPMNDataObject, ArrayList<BPMNTask>>> br: checkBox.map.entrySet()) {
							for(Entry<BPMNDataObject, ArrayList<BPMNTask>> entry: br.getValue().entrySet()) {
								if(!participantList.containsKey(br.getKey())) {
									participantList.put(br.getKey(), new HashMap<BPMNDataObject, ArrayList<BPMNTask>>());
									
								} 
								if(participantList.get(br.getKey()).get(entry.getKey())==null) {
									ArrayList<BPMNTask> list = new ArrayList<BPMNTask>();
									list.add(checkBox.oneTaskOutOfMap);
									participantList.get(br.getKey()).put(entry.getKey(), list);
								}								
								
								else {
									participantList.get(br.getKey()).get(entry.getKey()).add(checkBox.oneTaskOutOfMap);
								}
								}
							}
							
						}
					
									
				}

				if (count == sumVotes) {
					try {
						a2.addVotingTasksToProcess(participantList);
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
							"Select at least " + sumVotes + " participants!",
							"InfoBox: " + "Selection failed!", JOptionPane.INFORMATION_MESSAGE);

				}

			}

		});
		
		JPanel panel = new JPanel();
		panel.add(button);
		frame.add(panel);
		
		frame.pack();
	}

}
