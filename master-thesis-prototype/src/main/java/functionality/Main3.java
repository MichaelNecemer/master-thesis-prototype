package functionality;

import java.awt.Color;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.DataObjectReference;
import org.camunda.bpm.model.bpmn.instance.FlowNode;

import Mapping.BPMNBusinessRuleTask;
import Mapping.BPMNDataObject;
import Mapping.BPMNExclusiveGateway;
import Mapping.BPMNParticipant;
import Mapping.BPMNTask;
import Mapping.ProcessInstanceWithVoters;
import Mapping.RequiredUpdate;
import Mapping.VoterForXorArc;
import ProcessModelGeneratorAndAnnotater.ProcessGenerator;
import ProcessModelGeneratorAndAnnotater.ProcessModelAnnotater;

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
	//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\modelle\\brtsIn2branches1.bpmn";
	//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\modelle\\brtsIn2branches2.bpmn";
	//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\modelle\\algorithmTest.bpmn";
	//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\modelle\\brtsIn2BranchesWith2DataObjects1.bpmn";
	//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\randomProcessModels\\randomProcessModel10_annotated.bpmn";
	//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\randomProcessModels\\randomProcessModel3_annotated.bpmn";
	//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\randomProcessModel5_annotated.bpmn";
	//String pathToFile = "C\\Users\\Micha\\OneDrive\\Desktop\\randomProcessModel2.bpmn";
	//String pathToFile =	 "C:\\Users\\Micha\\OneDrive\\Desktop\\camunda-diagrams\\diagram_2.bpmn";
	//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\test.bpmn";
	//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\EvaluationSetup\\Test2\\SmallProcessesAnnotatedFolder\\Spheres\\randomProcessModel84_annotated1_annotated92lWmR_Static1.bpmn";
	//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\modelle\\brtsIn2branches3.bpmn";
	String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\EvaluationSetup\\Test1\\MediumProcessesAnnotatedFolder\\ModelsForEvaluation\\randomProcessModel198_annotated1_annotated203lWmR210_Static_voters2.bpmn";
		
		JFrame frame = new JFrame(pathToFile);
	frame.setVisible(true);
	
	JPanel panel = new JPanel();
	panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
	panel.add(new JLabel("Algorithms: "));
	panel.add(new JLabel("Final Decider: "));
	
	
	ArrayList<Double> costForUpgradingSpheres = new ArrayList<>(Arrays.asList(10.0, 5.0, 3.0, 2.0));
	double costForAddingReaderAfterBrt = 0.0;
	String pathForAddingRandomModels = "C:\\Users\\Micha\\OneDrive\\Desktop";
	
	
	try {
		//ProcessGenerator g= new ProcessGenerator(pathForAddingRandomModels,4000, 15, 20, 40, 18,50,30,20,20,5);
	

	
	} catch (Exception e1) {
		panel.add(new JLabel("Error: "+ e1.getMessage()));
		frame.add(panel);
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	try {
		//ProcessModellAnnotater.annotateModel("C:\\Users\\Micha\\OneDrive\\Desktop\\modelle\\generatedModel1.bpmn", new LinkedList<Integer>(Arrays.asList(1,2)), new LinkedList<String>(Arrays.asList("Global","Static","Weak-Dynamic","Strong-Dynamic")),50,30, 20);
		//ProcessModellAnnotater.annotateModel("C:\\Users\\Micha\\OneDrive\\Desktop\\randomProcessModels\\randomProcessModel7.bpmn", new LinkedList<Integer>(Arrays.asList(1,2)), new LinkedList<String>(Arrays.asList("Public","Global","Static","Weak-Dynamic","Strong-Dynamic")),50,30, 20, 30);
		//ProcessModellAnnotater.annotateModel("C:\\Users\\Micha\\OneDrive\\Desktop\\randomProcessModel2.bpmn", new LinkedList<Integer>(Arrays.asList(1,2)), new LinkedList<String>(Arrays.asList("Public","Global","Static","Weak-Dynamic","Strong-Dynamic")),50,30, 20, 30);
		//ProcessModellAnnotater.annotateModel("C:\\Users\\Micha\\OneDrive\\Desktop\\randomProcessModel5.bpmn", new LinkedList<Integer>(Arrays.asList(1,2)), new LinkedList<String>(Arrays.asList("Public","Global","Static","Weak-Dynamic","Strong-Dynamic")),50,30, 20, 30);
		//ProcessModellAnnotater.annotateModelWithReaderAndWriterProbabilities("C:\\Users\\Micha\\OneDrive\\Desktop\\randomProcessModel2.bpmn", "C:\\Users\\Micha\\OneDrive\\Desktop",new LinkedList<Integer>(Arrays.asList(1,2)), new LinkedList<String>(Arrays.asList("Public","Global","Static","Weak-Dynamic","Strong-Dynamic")),50,30, 20, 30);
		//ProcessModellAnnotater.annotateModel("C:\\Users\\Micha\\OneDrive\\Desktop\\randomProcessModel1.bpmn","C:\\Users\\Micha\\OneDrive\\Desktop", new LinkedList<Integer>(Arrays.asList(1,2)), new LinkedList<String>(Arrays.asList("Public","Global","Static","Weak-Dynamic","Strong-Dynamic")),50,30, 20, 30);

		
		
		a2 = new API(pathToFile, costForUpgradingSpheres, costForAddingReaderAfterBrt);
		
		panel.add(new JLabel("Paths through process: "+a2.getAllPathsThroughProcess().size()));
		frame.add(panel);
		frame.setLayout(new GridLayout(0, a2.getDataObjects().size()+1, 10, 0));
		
		for(Entry<DataObjectReference, LinkedList<FlowNode>>  entry: a2.getReadersMap().entrySet()) {
			System.out.println(entry.getKey().getName());
			for(FlowNode f: entry.getValue()) {
				System.out.println(f.getName());
			}
			
		}
		
		for(Entry<DataObjectReference, LinkedList<FlowNode>>  entry: a2.getWritersMap().entrySet()) {
			System.out.println(entry.getKey().getName());
			for(FlowNode f: entry.getValue()) {
				System.out.println(f.getName());
			}
			
		}
		
	} catch (Exception e2) {
		// TODO Auto-generated catch block
		System.err.println("Error found: ");
		e2.printStackTrace();
		panel.add(new JLabel("Error: "+ e2.getMessage()));
		frame.add(panel);
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	

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
	LinkedList<ProcessInstanceWithVoters> pInstances;
	try {
		pInstances = a2.localMinimumAlgorithm();
		System.out.println("Amount of solutions found with localMinimumAlgorithm: "+pInstances.size());
		System.out.println("Cheapest Heuristic Search Instances: ");
		
		for(ProcessInstanceWithVoters pInstance:pInstances) {
			
		
		pInstance.printProcessInstance();
		}
		
		//a2.annotateModelWithChosenParticipants(pInstances);
		
		
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
			System.out.println(sb.toString());
		}
	} catch (NullPointerException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	} catch (Exception e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
	
	LinkedList<ProcessInstanceWithVoters> bruteForceSolutions;
	try {
		bruteForceSolutions = a2.bruteForceAlgorithm();
		System.out.println("Solutions found with BruteForce: "+bruteForceSolutions.size());
		System.out.println("Cheapest brute Force solutions: "+a2.getCheapestProcessInstancesWithVoters(bruteForceSolutions).size());
		
		
		
	} catch (NullPointerException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	

	
	
	//a2.annotateModelWithChosenParticipants(pInstances);
	
	/*
	//Brute Force Attempt
	//print out all solutions
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
	
	/*
	try {
		a2.addVotingTasksToProcess(1, pInstances.get(0), a2.getTroubleShooter(), true);
	} catch (IOException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}*/
	frame.pack();
	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	
}
}
