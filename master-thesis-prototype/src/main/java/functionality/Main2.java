
package functionality;
/*
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import Mapping.BPMNBusinessRuleTask;
import Mapping.BPMNDataObject;
import Mapping.BPMNElement;
import Mapping.BPMNExclusiveGateway;
import Mapping.BPMNParticipant;
import Mapping.BPMNTask;

public class Main2 {

	static API a2 = null;
	static BPMNExclusiveGateway bpmnEx = null;
	static int sumVotes = 0;
	

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		
		 JFileChooser chooser = new JFileChooser();
		 
		 chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		 chooser.showOpenDialog(null);
		 
		 int rueckgabeWert = chooser.showOpenDialog(null); if(rueckgabeWert ==
		 JFileChooser.APPROVE_OPTION) { a2 = new API(
		  chooser.getSelectedFile().getAbsolutePath());
		  
		  }
		
		
		ArrayList<Double> cost = new ArrayList<>(Arrays.asList(10.0, 5.0, 3.0, 2.0));
		double cost2 = 1;
		
		
		
		try {
			a2 = new API(
					"C:\\Users\\Micha\\git\\master-thesis-prototype\\master-thesis-prototype\\src\\main\\resources\\process3.bpmn", cost, cost2);
		} catch (Exception e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		LinkedHashMap<BPMNBusinessRuleTask, HashMap<BPMNDataObject, ArrayList<BPMNTask>>> participantList = new LinkedHashMap<BPMNBusinessRuleTask, HashMap<BPMNDataObject, ArrayList<BPMNTask>>>();
		LinkedHashMap<BPMNBusinessRuleTask, BPMNParticipant> finalDeciderList = new LinkedHashMap<BPMNBusinessRuleTask, BPMNParticipant>();
		JFrame frame = new JFrame("GUITest");
		frame.setVisible(true);
		frame.setLayout(new GridLayout(0, a2.getBusinessRuleTasks().size(), 10, 0));
		ArrayList<JCheckBoxWithId> checkboxes= new ArrayList<JCheckBoxWithId>();
		ArrayList<JCheckBoxFinalDecider> finalDeciderBoxes = new ArrayList<JCheckBoxFinalDecider>();
		ArrayList<JPanel> panelList = new ArrayList<JPanel>();
		HashMap<BPMNExclusiveGateway, Integer> countVotesMap = new HashMap<BPMNExclusiveGateway, Integer>();
		
		for (BPMNBusinessRuleTask brt : a2.getBusinessRuleTasks()) {
			if(brt.getSuccessors().iterator().next() instanceof BPMNExclusiveGateway) {
			BPMNExclusiveGateway xorGtw = (BPMNExclusiveGateway)brt.getSuccessors().iterator().next();		
				
			JPanel panel = new JPanel();
			panelList.add(panel);
			
			
			panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
			bpmnEx = (BPMNExclusiveGateway) brt.getSuccessors().iterator().next();
			sumVotes = bpmnEx.getAmountVoters();
			countVotesMap.put(bpmnEx, bpmnEx.getAmountVoters());
			panel.add(new JLabel("CHOOSE "+bpmnEx.getAmountVoters()  +" VOTERS FOR EXCLUSIVE GATEWAY " + bpmnEx.getName()));
			
			if(xorGtw.getConstraints()!=null) {
				String[] test = xorGtw.getConstraints();
				StringBuilder constrBuilder = new StringBuilder();
				constrBuilder.append("Constraints: ");
				for(String constr: test) {
					constrBuilder.append(constr+", ");			
			}
				constrBuilder.delete(constrBuilder.length()-2, constrBuilder.length());
				JLabel constrLabel = new JLabel(constrBuilder.toString());
				constrLabel.setForeground(Color.RED);
				panel.add(constrLabel);
			}
			
		
			
			for(Entry<BPMNDataObject,ArrayList<BPMNTask>> entry : brt.getLastWriterList().entrySet()) {
				JLabel colorLabelDataObject = new JLabel("Data Object: "+entry.getKey().getName());
				colorLabelDataObject.setForeground(Color.BLUE);
				panel.add(colorLabelDataObject);
						
											
				for(BPMNTask lastWriterTask: entry.getValue()) {
				JLabel lastWriterTaskLabel = new JLabel();
				String writerSphere = lastWriterTask.getSphereAnnotation().get(entry.getKey());
				
				if(lastWriterTask.hasLabel()) {
				lastWriterTaskLabel.setText("LastWriter "+lastWriterTask.getName() + " has Label: "+lastWriterTask.getLabelsAsString()+ " ("+writerSphere+")");
				} else {
					lastWriterTaskLabel.setText("LastWriter "+lastWriterTask.getName() +" ("+writerSphere+")");
				}
				panel.add(lastWriterTaskLabel);
				//if there are not enough possible readers between lastWriter and brt, the search can be extended
				//if lastWriter writes weak-dynamically, statically or globally: extend search beyond brt till end of the process
				//if lastWriter writes strong-dynamically - check if the reader is in each path of the process!
							
				JCheckBoxPossiblePath box= new JCheckBoxPossiblePath(lastWriterTask, entry.getKey(), brt, xorGtw, a2);
								
				int count = 0;
				ArrayList<BPMNElement>foundReaders = new ArrayList<BPMNElement>();
				
				for(LinkedList<BPMNElement> pathList: box.pathsBetweenLastWriterAndReader) {					
					for(int i = 1; i < pathList.size()-1; i++) {
						if(box.dataO.getReaders().contains(pathList.get(i))) {
							//a reader is found on the path from lastWriter to brt
							foundReaders.add(pathList.get(i));
							count++;				
						}
					}
					
				}
				
				
				
				//try if there can be found enough readers for the gateway with extended search
				//if not enough are found already
								
					while(count<xorGtw.getAmountVoters()) {							
					
					BPMNTask foundReader = (BPMNTask)a2.searchReadersAfterBrt(lastWriterTask, box.dataO, brt, foundReaders);
					foundReaders.add(foundReader);			
						
					count++;
					
					if(!foundReader.equals(brt)) {
						JLabel pathLabel = new JLabel();
						pathLabel.setText("Extended Search to: "+foundReader.getName());
						panel.add(pathLabel);
						
						//box = new JCheckBoxPossiblePath(lastWriterTask, entry.getKey(), lastReader, xorGtw, a2);
						}
				}
				
			
				
				//show the possible paths from lastWriter onwards to the endpoint of the search to the user
				for(int i = 0; i < box.pathsAsString.size(); i++) {
					JLabel pathLabel = new JLabel();
					pathLabel.setText("Path "+(i+1)+": "+box.pathsAsString.get(i));
					panel.add(pathLabel);	
				}
				
				}
			}
				
			
			JLabel colorLabelFD = new JLabel("CHOOSE 1 FINAL DECIDER");
			colorLabelFD.setForeground(Color.MAGENTA);	
			panel.add(colorLabelFD);
			
			//add the final decider checkboxes to the panel
			//preselect the first box
			for(int i = 0; i < a2.getGlobalSphereList().size(); i++) {
				BPMNParticipant participant = a2.getGlobalSphereList().get(i);			
				JCheckBoxFinalDecider box = new JCheckBoxFinalDecider(participant, bpmnEx, brt);
				box.setText(participant.getName());
				finalDeciderBoxes.add(box);
				panel.add(box);
					if(i==0) {
						box.setSelected(true);
					}
			}
				
			frame.add(panel);
			frame.pack();
		}
		}
		
		JPanel panel = panelList.get(panelList.size()-1);
		JButton button = new JButton("Add marked participants to voting");		
		panel.add(new JLabel("Settings: "));
	
		JRadioButton mapModelBtn = new JRadioButton("Map Model");
		mapModelBtn.setSelected(true);
		panel.add(mapModelBtn);
		
		panel.add(new JLabel("Choose between static and dynamic voting sytem:"));
		JRadioButton staticBtn = new JRadioButton("Static Voting");
		JRadioButton dynamicBtn = new JRadioButton("Dynamic Voting");
		dynamicBtn.setSelected(true);
		panel.add(staticBtn);
		panel.add(dynamicBtn);
		panel.add(button);
		
		
		
		
		
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				
				ArrayList<BPMNExclusiveGateway>notEnoughVotesList = new ArrayList<BPMNExclusiveGateway>();
				ArrayList<BPMNExclusiveGateway>notOneFinalDeciderList = new ArrayList<BPMNExclusiveGateway>();
				boolean finalDeciderSelected = true;
				boolean allSelected = true;
				
				for(Entry<BPMNExclusiveGateway, Integer> vote: countVotesMap.entrySet()) {
					int count = 0;
					int finalDeciderCount = 0;
					BPMNExclusiveGateway exKey = vote.getKey();
					int votes = vote.getValue();
					
				for(JCheckBoxFinalDecider box: finalDeciderBoxes) {
					if(box.isSelected()) {
						if(exKey.equals(box.bpmnEx)) {
							finalDeciderCount++;
						}						
					}
				}
				
				
				
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
									if(!participantList.get(br.getKey()).get(entry.getKey()).contains(checkBox.oneTaskOutOfMap)) {
									participantList.get(br.getKey()).get(entry.getKey()).add(checkBox.oneTaskOutOfMap);
									}
								}
							}
							
						}
						
					}
					
				}
				
				if(count!=votes) {
					allSelected=false;
					notEnoughVotesList.add(exKey);
				}
				
				if(finalDeciderCount!=1) {
					finalDeciderSelected=false;
					notOneFinalDeciderList.add(exKey);
				} 
				
				
				}
			
				
				if (allSelected&&finalDeciderSelected) {
					try {
						for(JCheckBoxFinalDecider box: finalDeciderBoxes) {
							if(box.isSelected()) {
								finalDeciderList.put(box.bpmnBrt, box.participant);
							}
						}
						
						a2.addVotingTasksToProcess(participantList, finalDeciderList, mapModelBtn.isSelected());
			
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
					for(BPMNExclusiveGateway exGt: notOneFinalDeciderList) {
						JOptionPane.showMessageDialog(null, "Select 1 Final Decider for "+exGt.getName() +"!",
								"InfoBox: " + "Selection failed!", JOptionPane.INFORMATION_MESSAGE);
						}
					}
			

			}

		});
	
		frame.add(panel);
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

}*/

