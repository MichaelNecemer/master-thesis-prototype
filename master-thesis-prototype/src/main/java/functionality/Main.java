
package functionality;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
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
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;

import Mapping.BPMNBusinessRuleTask;
import Mapping.BPMNDataObject;
import Mapping.BPMNElement;
import Mapping.BPMNExclusiveGateway;
import Mapping.BPMNParticipant;
import Mapping.BPMNTask;
import Mapping.InfixToPostfix;

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
		 
		 int rueckgabeWert = chooser.showOpenDialog(null); if(rueckgabeWert ==
		 JFileChooser.APPROVE_OPTION) { a2 = new API(
		  chooser.getSelectedFile().getAbsolutePath());
		  
		  }
		*/
		
		
		API a2 = new API(
				"C:\\Users\\Micha\\git\\master-thesis-prototype\\master-thesis-prototype\\src\\main\\resources\\process3.bpmn");
		
		LinkedHashMap<BPMNBusinessRuleTask, HashMap<BPMNDataObject, ArrayList<BPMNTask>>> participantList = new LinkedHashMap<BPMNBusinessRuleTask, HashMap<BPMNDataObject, ArrayList<BPMNTask>>>();
		JFrame frame = new JFrame("GUITest");
		frame.setVisible(true);
		frame.setLayout(new GridLayout(0, a2.getBusinessRuleTasks().size(), 10, 0));
		ArrayList<JCheckBoxWithId> checkboxes= new ArrayList<JCheckBoxWithId>();
		ArrayList<JPanel> panelList = new ArrayList<JPanel>();
		HashMap<BPMNExclusiveGateway, Integer> countVotesMap = new HashMap<BPMNExclusiveGateway, Integer>();
		
		for (BPMNBusinessRuleTask brt : a2.getBusinessRuleTasks()) {
			JPanel panel = new JPanel();
			panelList.add(panel);
			
			ArrayList<JCheckBoxWithId> checkBoxesStrongDynamic = new ArrayList<JCheckBoxWithId>();
			ArrayList<JCheckBoxWithId> checkBoxesWeakDynamic = new ArrayList<JCheckBoxWithId>();
			ArrayList<JCheckBoxWithId> testBoxes = new ArrayList<JCheckBoxWithId>();

			panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
			bpmnEx = (BPMNExclusiveGateway) brt.getSuccessors().iterator().next();
			sumVotes = bpmnEx.getAmountVoters();
			countVotesMap.put(bpmnEx, bpmnEx.getAmountVoters());
			panel.add(new JLabel("CHOOSE "+bpmnEx.getAmountVoters()  +" VOTERS FOR EXCLUSIVE GATEWAY " + bpmnEx.getName()));			
			

			for (BPMNTask lastWriterTask : brt.getLastWriterList()) {				

				for (Entry<BPMNBusinessRuleTask, HashMap<BPMNDataObject, ArrayList<BPMNTask>>> t2 : lastWriterTask
						.getSDHashMap().entrySet()) {

					if (t2.getKey().getId().equals(brt.getId())) {

						for (Entry<BPMNDataObject, ArrayList<BPMNTask>> entry : t2.getValue().entrySet()) {
							
							for (BPMNTask task : entry.getValue()) {
								JCheckBoxWithId box = new JCheckBoxWithId(
										task.getParticipant().getName() + ", " + task.getName()+" (Writer: "+lastWriterTask.getName()+")", lastWriterTask,
										lastWriterTask.getSDHashMap(), task, bpmnEx, entry.getKey(), "Strong-Dynamic");
								checkBoxesStrongDynamic.add(box);
								
							}
						}
					}
				}
				for (Entry<BPMNBusinessRuleTask, HashMap<BPMNDataObject, ArrayList<BPMNTask>>> t2 : lastWriterTask
						.getWDHashMap().entrySet()) {
					if (t2.getKey().getId().equals(brt.getId())) {
						for (Entry<BPMNDataObject, ArrayList<BPMNTask>> entry : t2.getValue().entrySet()) {
							
							for (BPMNTask task : entry.getValue()) {
								JCheckBoxWithId box = new JCheckBoxWithId(
										task.getParticipant().getName() + ", " + task.getName()+" (Writer: "+lastWriterTask.getName()+")", lastWriterTask,
										lastWriterTask.getWDHashMap(), task, bpmnEx, entry.getKey(), "Weak-Dynamic");
								checkBoxesWeakDynamic.add(box);
								
							}
						}
					}
				}
				
				
				
			}
			
			checkboxes.addAll(checkBoxesStrongDynamic);
			checkboxes.addAll(checkBoxesWeakDynamic);
			testBoxes.addAll(checkBoxesStrongDynamic);
			testBoxes.addAll(checkBoxesWeakDynamic);
			
			for(BPMNDataObject dataObject: brt.getDataObjects()) {
				JLabel colorLabelDataObject = new JLabel("Data Object: "+dataObject.getName());
				colorLabelDataObject.setForeground(Color.PINK);
				panel.add(colorLabelDataObject);

				boolean help = false;
				boolean help2 = false;
				int boxCount = 0;
				
				for(int i = 0; i < testBoxes.size(); i++) {	
					JCheckBoxWithId currentBox = testBoxes.get(i);
					if(currentBox.dataObject.getId().equals(dataObject.getId())) {						
						if(currentBox.sphere.equals("Strong-Dynamic")) {
							if(help==false) {
								JLabel colorLabelSD = new JLabel("Strong-Dynamic Sphere");
								colorLabelSD.setForeground(Color.GREEN);
								panel.add(colorLabelSD);
							help=true;
							}
							//Preselect the Strong-Dynamic checkboxes until limit annotated at the xor gateway is reached!
							if(currentBox.bpmnEx.getAmountVoters()>boxCount) {
								currentBox.setSelected(true);
								currentBox.preSelected=true;
								boxCount++;
							}							
							panel.add(currentBox);
							
							
						} else if (currentBox.sphere.equals("Weak-Dynamic")) {
							if(help2==false) {	
								JLabel colorLabelWD = new JLabel("Weak-Dynamic Sphere");
								colorLabelWD.setForeground(Color.BLUE);	
								panel.add(colorLabelWD);
								help2=true;
							}
							panel.add(currentBox);
							
						}
						
						
					}
				}
			}
						
			
			frame.add(panel);
			frame.pack();
		}

		
		JPanel panel = panelList.get(panelList.size()-1);
		JButton button = new JButton("Add marked participants to voting");		
		panel.add(new JLabel("Settings: "));
		
		JRadioButton mapModelBtn = new JRadioButton("Map Model");
		mapModelBtn.setSelected(true);
		panel.add(mapModelBtn);
		panel.add(button);
		
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				
				ArrayList<BPMNExclusiveGateway>notEnoughVotesList = new ArrayList<BPMNExclusiveGateway>();
				boolean allSelected = true;
				
				for(Entry<BPMNExclusiveGateway, Integer> vote: countVotesMap.entrySet()) {
					int count = 0;
					BPMNExclusiveGateway exKey = vote.getKey();
					int votes = vote.getValue();
				
				for (JCheckBoxWithId checkBox : checkboxes) {
					if (checkBox.isSelected()) {						
						if(exKey.equals(checkBox.bpmnEx)) {
							count++;
						}
						
						
						for (Entry<BPMNBusinessRuleTask, HashMap<BPMNDataObject, ArrayList<BPMNTask>>> br : checkBox.map
								.entrySet()) {
							
							for (Entry<BPMNDataObject, ArrayList<BPMNTask>> entry : br.getValue().entrySet()) {
								
								
								if (!participantList.containsKey(br.getKey())) {
									participantList.put(br.getKey(),
											new HashMap<BPMNDataObject, ArrayList<BPMNTask>>());

								}
								if (participantList.get(br.getKey()).get(entry.getKey()) == null) {
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
				
				if(count!=votes) {
					allSelected=false;
					notEnoughVotesList.add(exKey);
				}
				}
			
				
				if (allSelected) {
					try {
						a2.addVotingTasksToProcess(participantList, mapModelBtn.isSelected());
			
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				} else {
					for (JCheckBoxWithId checkBox : checkboxes) {
						if(!checkBox.preSelected) {
						checkBox.setSelected(false);
						} else {
							checkBox.setSelected(true);
						}
					}
					participantList.clear();
					for(BPMNExclusiveGateway exGtw: notEnoughVotesList) {
					JOptionPane.showMessageDialog(null, "Select " +exGtw.getAmountVoters() + " participants for "+exGtw.getName() +"!",
							"InfoBox: " + "Selection failed!", JOptionPane.INFORMATION_MESSAGE);
					}
				}

			}

		});
	
		frame.add(panel);
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

}
