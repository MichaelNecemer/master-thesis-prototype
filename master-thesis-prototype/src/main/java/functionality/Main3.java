package functionality;

import java.awt.Color;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import Mapping.BPMNDataObject;
import Mapping.BPMNExclusiveGateway;
import Mapping.BPMNParticipant;
import Mapping.ProcessInstanceWithVoters;
import Mapping.VoterForXorArc;
import ProcessModelAnnotaterAlgorithm.ProcessModelAnnotater;

public class Main3 {
	

	static API a2 = null;
	static BPMNExclusiveGateway bpmnEx = null;
	static int sumVotes = 0;
	
	public static void main(String[] args) {
	/*
	 JFileChooser chooser = new JFileChooser();
	 
	 chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
	 chooser.showOpenDialog(null);
	 
	 int rueckgabeWert = chooser.showOpenDialog(null); if(rueckgabeWert ==
	 JFileChooser.APPROVE_OPTION) { a2 = new API(
	  chooser.getSelectedFile().getAbsolutePath());
	  
	  }
	*/
	
	
	ArrayList<Double> cost = new ArrayList<>(Arrays.asList(10.0, 5.0, 3.0, 2.0));
	
	try {
		//a2 = new API("C:\\Users\\Micha\\git\\master-thesis-prototype\\master-thesis-prototype\\src\\main\\resources\\process3.bpmn", cost);
		ProcessModelAnnotater.annotateModel("C:\\Users\\Micha\\OneDrive\\Desktop\\modelle\\testAnnotatingAlgorithm.bpmn", new LinkedList<Integer>(Arrays.asList(1,3)), new LinkedList<String>(Arrays.asList("Global","Static","Weak-Dynamic","Strong-Dynamic")),50, 30);
		a2 = new API("C:\\Users\\Micha\\OneDrive\\Desktop\\modelle\\overlappingLastWriters1.bpmn", cost);
		//a2 = new API("C:\\Users\\Micha\\OneDrive\\Desktop\\modelle\\brtsIn2branches1.bpmn", cost);
		//a2 = new API("C:\\Users\\Micha\\OneDrive\\Desktop\\modelle\\brtsIn2branches2.bpmn", cost);
		//a2 = new API("C:\\Users\\Micha\\OneDrive\\Desktop\\modelle\\algorithmTest.bpmn", cost);

	} catch (Exception e2) {
		// TODO Auto-generated catch block
		e2.printStackTrace();
	}
	
	JFrame frame = new JFrame("GUI");
	frame.setVisible(true);
	frame.setLayout(new GridLayout(0, a2.getDataObjects().size()+1, 10, 0));
	
	JPanel panel = new JPanel();
	panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
	panel.add(new JLabel("Amount of possible combinations of participants: "+a2.getAmountPossibleCombinationsOfParticipants()));
	panel.add(new JLabel("Amount of possible paths from StartEvent to EndEvent: "+a2.getAllPathsThroughProcess().size()));
	frame.add(panel);

	
	for(BPMNDataObject dataO: a2.getDataObjects()) {
		JPanel dataPanel = new JPanel();
		frame.add(dataPanel);
		dataPanel.setLayout(new BoxLayout(dataPanel, BoxLayout.PAGE_AXIS));
		JLabel staticSphereLabel = new JLabel("Static Sphere for DataObject "+dataO.getNameId());
		staticSphereLabel.setForeground(Color.MAGENTA);
		dataPanel.add(staticSphereLabel);
		for(BPMNParticipant participant: dataO.getStaticSphere()) {
			dataPanel.add(new JLabel(participant.getName()));
		}
		
		
	}
	
	//Brute Force Attempt
	//print out all cheapest solutions
	for(ProcessInstanceWithVoters pInstance: a2.getCheapestProcessInstancesWithVoters()) {
		JPanel dataPanel = new JPanel();
		frame.add(dataPanel);
		dataPanel.setLayout(new BoxLayout(dataPanel, BoxLayout.PAGE_AXIS));
		JLabel pInstLabel = new JLabel("ProcessInstance "+pInstance.getProcessInstanceID() +" with cost: "+pInstance.getCostForModelInstance());
		pInstLabel.setForeground(Color.blue);
		dataPanel.add(pInstLabel);
		StringBuilder sb = new StringBuilder();
		for(VoterForXorArc a: pInstance.getListOfArcs()) {
			
			sb.append(a.getBrt().getName()+": ");
			for(BPMNParticipant part: a.getChosenCombinationOfParticipants()) {
				sb.append(part.getName()+", ");
			}
		}
		
		sb.deleteCharAt(sb.length()-2);
		dataPanel.add(new JLabel(sb.toString()));
		
	}
	
	frame.pack();
	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

}
}
