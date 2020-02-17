package functionality;

import java.awt.BorderLayout;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;

import Mapping.BPMNBusinessRuleTask;
import Mapping.BPMNElement;
import Mapping.BPMNParticipant;

public class Main {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		//Get a JavaDomTraversal object to query the process xml file
		//JavaDomTraversal d1 = new JavaDomTraversal("C:\\Users\\Micha\\eclipse-workspace\\master-thesis-prototype\\src\\main\\resources\\process.bpmn");
		//BpmnAPI a1 = new BpmnAPI("C:\\Users\\Micha\\eclipse-workspace\\master-thesis-prototype\\src\\main\\resources\\process.bpmn");
		//a1.getFlowingFlowNodes(sequenceFlow.getSource());
		//a1.printTasks();
		//a1.printGlobalSphere();
		//a1.printAllElements();
		//a1.printFlowingFlowNodes(a1.getStartEvent().iterator().next());
		//Collection<FlowNode> f = a1.getAllFlowNodesAfterNode(a1.getStartEvent().iterator().next());
		//a1.printAllFlowNodes();
		
		//a1.computeStaticSphere();
		//a1.getDataObjectsForNode(a1.getNodeById("Task_13s70q3")).forEach(f->{a1.getNameOfDataObject(f);});
		//a1.getDataObjectsTheTaskWritesTo(a1.getNodeById("Task_13s70q3")).forEach(f->{a1.getNameOfDataObject(f);});
		//a1.getAllNodesConnectedToDataObject(a1.dataObjects.iterator().next()).forEach(f->{System.out.println(f.getId());});
		API a2 = null;
		/*
		  JFileChooser chooser = new JFileChooser();
		    
		    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		    chooser.showOpenDialog(null);
		    
		    int rueckgabeWert = chooser.showOpenDialog(null);	        
	        if(rueckgabeWert == JFileChooser.APPROVE_OPTION)
	        {
	               a2 = new API( chooser.getSelectedFile().getAbsolutePath());  
	                
	        }
		*/
		a2 = new API("C:\\Users\\Micha\\eclipse-workspace\\master-thesis-prototype\\src\\main\\resources\\process.bpmn");
		//a2 = new API("C:\\Users\\Micha\\eclipse-workspace\\master-thesis-prototype\\src\\main\\resources\\process1.bpmn");
		a2.setSuccessors();
		a2.setPredecessors();
		a2.storeLanePerTask();
		a2.mapDataObjects();
		a2.createDataObjectAsssociatons();
		//a2.printDataObjects();
		a2.computeGlobalSphere();
		//a2.printGlobalSphere();
		
		a2.computeStaticSphere();
		//a2.printStaticSphere();
		//a2.printElementPredecessorAndSuccessor();
		a2.getPathBetweenNodes();		
		System.out.println(a2.checkProcessModel());
		a2.addVotingSystem(a2.getNodeById("BusinessRuleTask_0dplu08"));
		
		/*
		LinkedList<LinkedList<BPMNElement>> test = 	a2.allElementsWithinParallelSplit(a2.getNodeById("ParallelGateway_0bzuner"));
		for(LinkedList<BPMNElement> l: test) {
			for(BPMNElement p: l) {
				p.printElement();
			}
		}*/
		
		//a2.printProcessElements();
		//a2.printElementPredecessorAndSuccessor();
		//a2.computeWeakDynamicSphere();
		
		/*
		 JFrame frame=new JFrame("GUI");
		 JPanel panel = new JPanel();
		    frame.setSize(600, 500);
		    frame.setResizable(false);
		    frame.setLocation(50, 50);		  
		    frame.setVisible(true);		    
		    panel.add(new JLabel("Global Sphere contains:"));
		    DefaultListModel<String> listModel = new DefaultListModel<String>();
		    JList<String> myJList = new JList<String>(listModel);
		  
		    for(BPMNParticipant p: a2.getGlobalSphereList()) {
		    	listModel.addElement(p.getName());
		    }
		    JScrollPane listScrollPane = new JScrollPane(myJList);
		    panel.add(listScrollPane);
		   frame.add(panel);
		  */
		    /*
		    JDialog meinJDialog = new JDialog();
	        meinJDialog.setTitle("Model has to be repaired");
	     
	        meinJDialog.setSize(200,200);
	      
	        meinJDialog.setModal(true);
	        meinJDialog.setVisible(true);
			*/
	}

}
