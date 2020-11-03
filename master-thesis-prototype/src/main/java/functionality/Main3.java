package functionality;

import java.awt.Color;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import Mapping.BPMNDataObject;
import Mapping.BPMNExclusiveGateway;
import Mapping.BPMNParticipant;

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
		//a2 = new API("C:\\Users\\Micha\\OneDrive\\Desktop\\modelle\\overlappingLastWriters1.bpmn", cost);
		a2 = new API("C:\\Users\\Micha\\OneDrive\\Desktop\\modelle\\brtsIn2branches1.bpmn", cost);
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
	panel.add(new JLabel("Amount of possible combinations with constraints: "));
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
	
	
	frame.pack();
	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

}
}
