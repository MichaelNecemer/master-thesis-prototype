package functionality;

import java.awt.Color;
import java.awt.GridLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map.Entry;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.camunda.bpm.model.bpmn.instance.DataObjectReference;
import org.camunda.bpm.model.bpmn.instance.FlowNode;

import Mapping.BPMNDataObject;
import Mapping.BPMNElement;
import Mapping.BPMNExclusiveGateway;
import Mapping.BPMNParticipant;
import Mapping.ProcessInstanceWithVoters;
import Mapping.VoterForXorArc;
import ProcessModelGeneratorAndAnnotater.ProcessModelAnnotater;

public class Main1 {
	

	static API a2 = null;
	static BPMNExclusiveGateway bpmnEx = null;
	static int sumVotes = 0;
	static LinkedList<String>defaultNamesSeqFlowsXorSplits = new LinkedList<String>( Arrays.asList("yes","no"));
	static LinkedList<String> defaultSpheres = new LinkedList<String>(
			Arrays.asList("Global", "Static", "Weak-Dynamic", "Strong-Dynamic"));
	public static void main(String[] args) {
	
	/*String pathToFile = "";
	JFileChooser chooser = new JFileChooser();
	 
	 chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
	 chooser.showOpenDialog(null);
	 
	 int rueckgabeWert = chooser.showOpenDialog(null); if(rueckgabeWert ==
	 JFileChooser.APPROVE_OPTION) {pathToFile=
	  chooser.getSelectedFile().getAbsolutePath();	  
	  }*/
	
	//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Dokumente\\brtsIn2branches2.bpmn";
	//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\test_parallel.bpmn";
	//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\test_parallel2.bpmn";
	String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\randomProcessModel262_annotated1_annotated263sWsR_Strong-Dynamic_voters2.bpmn";

	ArrayList<Double> costForUpgradingSpheres = new ArrayList<>(Arrays.asList(10.0, 5.0, 3.0, 2.0));
	//String pathForAddingRandomModels = "C:\\Users\\Micha\\OneDrive\\Desktop";
		
	
	//ProcessGenerator 
	try {
		//ProcesGenerator g= new ProcessGenerator(pathForAddingRandomModels,4000, 15, 20, 40, 18,50,30,20,20,5);
	

	
	} catch (Exception e1) {
		System.err.println(e1.getMessage());
	}
	//File f = null;
	Exception ex = null;
	/*
	do {
	try {
		String pathToCreateAnnotatedFile = new File(pathToFile).getParent();
		ProcessModelAnnotater p = new ProcessModelAnnotater(pathToFile, pathToCreateAnnotatedFile, "");
		//p.addNamesForOutgoingFlowsOfXorSplits(defaultNamesSeqFlowsXorSplits);
		System.out.println("Test3");
		//p.generateDataObjects(7, defaultSpheres);
		System.out.println("Test4");
		//p.connectDataObjectsToBrtsAndTuplesForXorSplits(1,
			//	1, 3, 3, 0, true);
		System.out.println("Test5");
		p.setDataObjectsConnectedToBrts(true);
		p.annotateModelWithFixedAmountOfReadersAndWriters(5, 15, 0, defaultSpheres);
		System.out.println("Test6");
		 f = p.checkCorrectnessAndWriteChangesToFile();
		//f = p.call();
		System.out.println("File annotated at: "+f.getAbsolutePath());
		ex = null;
		
	} catch (Exception e1) {
		// TODO Auto-generated catch block
		ex = e1;
		System.err.println("Exception in Annotater: "+e1.getMessage());
	}
	} while(ex!=null);
	
	*/
	//API
	try {	
		
		File f = new File(pathToFile);
		String pathToAnnotatedFile = f.getAbsolutePath();
		a2 = new API(pathToAnnotatedFile, costForUpgradingSpheres);
		LinkedList<LinkedList<FlowNode>>nodes = a2.getAllPathsThroughProcess();
		nodes.size();
		
	} catch (Exception e2) {
		// TODO Auto-generated catch block
		System.err.println("Exception in API: "+e2.getMessage());

	}

	
	
	
	//use local minimum Algorithm to find cheapest combinations
	
	LinkedList<ProcessInstanceWithVoters> pInstances = null;
	try {
		pInstances = a2.localMinimumAlgorithm();
		System.out.println("Amount of solutions found with localMinimumAlgorithm: "+pInstances.size());
		
		for(ProcessInstanceWithVoters pInstance:pInstances) {		
		pInstance.printProcessInstance();
		}
		
		//CommonFunctionality.generateNewModelsWithVotersAsBpmnConstruct(a2, pInstances, 3, "", true);
		
		//CommonFunctionality.generateNewModelsWithAnnotatedChosenParticipants(a2, pInstances, 3, "");
		
		for(ProcessInstanceWithVoters pInstance: pInstances) {
			StringBuilder sb = new StringBuilder();
			for(VoterForXorArc a: pInstance.getListOfArcs()) {
				
				sb.append(a.getBrt().getName()+": ");
				for(BPMNParticipant part: a.getChosenCombinationOfParticipants()) {
					sb.append(part.getName()+", ");
				}
			}
			
			
			sb.deleteCharAt(sb.length()-2);
			System.out.println(sb.toString());
		}
	} catch (NullPointerException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	} catch (Exception e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
	
	LinkedList<ProcessInstanceWithVoters> bruteForceSolutions = null;
	try {
		bruteForceSolutions = a2.bruteForceAlgorithm();
		System.out.println("Solutions found with BruteForce: "+bruteForceSolutions.size());
		System.out.println("Cheapest brute Force solutions: "+CommonFunctionality.getCheapestProcessInstancesWithVoters(bruteForceSolutions).size());
		for(ProcessInstanceWithVoters pInstance:CommonFunctionality.getCheapestProcessInstancesWithVoters(bruteForceSolutions)) {		
			pInstance.printProcessInstance();
			}
		
		
	} catch (NullPointerException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
	

	try {
	//	CommonFunctionality.generateNewModelsWithAnnotatedChosenParticipants(a2, pInstances, 1, a2.getProcessFile().getParent());

		//CommonFunctionality.generateNewModelsWithVotersAsBpmnConstruct(a2, bruteForceSolutions, 1, "", true);
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}


	System.out.println("Compare:" +CommonFunctionality.compareResultsOfAlgorithmsForDifferentAPIs(pInstances, bruteForceSolutions, 100000));
	
	
	}
}
