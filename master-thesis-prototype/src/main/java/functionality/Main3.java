package functionality;

import java.awt.Color;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import Mapping.BPMNDataObject;
import Mapping.BPMNExclusiveGateway;
import Mapping.BPMNParticipant;
import Mapping.ProcessInstanceWithVoters;
import Mapping.VoterForXorArc;
import ProcessModelGeneratorAndAnnotater.ProcessGenerator;
import ProcessModelGeneratorAndAnnotater.ProcessModellAnnotater;

public class Main3 {
	

	static API a2 = null;
	static BPMNExclusiveGateway bpmnEx = null;
	static int sumVotes = 0;
	
	public static void main(String[] args) {
	
	/*String pathToFile = "";
	JFileChooser chooser = new JFileChooser();
	 
	 chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
	 chooser.showOpenDialog(null);
	 
	 int rueckgabeWert = chooser.showOpenDialog(null); if(rueckgabeWert ==
	 JFileChooser.APPROVE_OPTION) {pathToFile=
	  chooser.getSelectedFile().getAbsolutePath();	  
	  }*/
	//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\modelle\\overlappingLastWriters1.bpmn";
	//String pathToFile = "C:\\Users\\Micha\\git\\master-thesis-prototype\\master-thesis-prototype\\src\\main\\resources\\process3.bpmn";
	String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\modelle\\brtsIn2branches1.bpmn";
	//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\modelle\\brtsIn2branches2.bpmn";
	//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\modelle\\algorithmTest.bpmn";
	//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\modelle\\brtsIn2BranchesWith2DataObjects1.bpmn";
	//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\randomProcessModels\\randomProcessModel10_annotated.bpmn";

		JFrame frame = new JFrame(pathToFile);
	frame.setVisible(true);
	
	JPanel panel = new JPanel();
	panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
	panel.add(new JLabel("Algorithms: "));
	panel.add(new JLabel("Final Decider: "));
	frame.add(panel);
	
	ArrayList<Double> costForUpgradingSpheres = new ArrayList<>(Arrays.asList(10.0, 5.0, 3.0, 2.0));
	double costForAddingReaderAfterBrt = 1.0;
	//String pathForAddingRandomModels = "C:\\Users\\Micha\\OneDrive\\Desktop";
	
	
	try {
		//ProcessGenerator g= new ProcessGenerator(pathForAddingRandomModels, 8, 30, 8, 6,50,30,20,20,10);
	} catch (Exception e1) {
		System.out.println("Random process could not be generated");
		e1.printStackTrace();
	}
	
	try {
		//ProcessModellAnnotater.annotateModel("C:\\Users\\Micha\\OneDrive\\Desktop\\modelle\\generatedModel1.bpmn", new LinkedList<Integer>(Arrays.asList(1,2)), new LinkedList<String>(Arrays.asList("Global","Static","Weak-Dynamic","Strong-Dynamic")),50,30, 20);
		//ProcessModellAnnotater.annotateModel("C:\\Users\\Micha\\OneDrive\\Desktop\\randomProcessModels\\randomProcessModel7.bpmn", new LinkedList<Integer>(Arrays.asList(1,2)), new LinkedList<String>(Arrays.asList("Public","Global","Static","Weak-Dynamic","Strong-Dynamic")),50,30, 20, 30);
		//ProcessModellAnnotater.annotateModel("C:\\Users\\Micha\\OneDrive\\Desktop\\randomProcessModel2.bpmn", new LinkedList<Integer>(Arrays.asList(1,2)), new LinkedList<String>(Arrays.asList("Public","Global","Static","Weak-Dynamic","Strong-Dynamic")),50,30, 20, 30);

		
		a2 = new API(pathToFile, costForUpgradingSpheres, costForAddingReaderAfterBrt);
		

	} catch (Exception e2) {
		// TODO Auto-generated catch block
		e2.printStackTrace();
	}
	

	frame.setLayout(new GridLayout(0, a2.getDataObjects().size()+1, 10, 0));
	
	/*
	JPanel panel2 = new JPanel();
	panel2.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
	panel2.add(new JLabel("Amount of possible combinations of participants: "+a2.getAmountPossibleCombinationsOfParticipants()));
	panel2.add(new JLabel("Amount of possible paths from StartEvent to EndEvent: "+a2.getAllPathsThroughProcess().size()));
	frame.add(panel2);
	*/
	
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
	
	
	
	//use local minimum Algorithm to find cheapest combinations
	LinkedList<ProcessInstanceWithVoters>pInstances = a2.localMinimumAlgorithm();
	for(ProcessInstanceWithVoters pInstance: pInstances) {
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
	
	//write the results to a csv file
	File csvFile = new File(a2.getProcessFile().getParent(), "resultsOfAlgorithm1.csv");
	try {
		if(csvFile.createNewFile()) {
			System.out.println("CSV File has been created at: "+csvFile.getAbsolutePath());
		}  else {
			System.out.println("CSV File already exists!");
		}
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	csvFile.getParentFile().mkdirs();
	ResultsToCSVWriter writer = new ResultsToCSVWriter(csvFile);
	writer.writeResultsOfAlgorithmToCSVFile(a2, pInstances);
	writer.writeRowsToCSVAndcloseWriter();
	
	
	a2.annotateModelWithChosenParticipants(pInstances);
	
	
	/*
	//Brute Force Attempt
	//print out all cheapest solutions
	for(ProcessInstanceWithVoters pInstance: a2.bruteForceAlgorithm()) {
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
	*/
	frame.pack();
	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

}
}
