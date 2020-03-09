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
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;

import Mapping.BPMNBusinessRuleTask;
import Mapping.BPMNDataObject;
import Mapping.BPMNElement;
import Mapping.BPMNParticipant;
import Mapping.BPMNTask;

public class Main {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
	
		//API a2 = null;
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
		//a2 = new API("C:\\Users\\Micha\\git\\master-thesis-prototype\\master-thesis-prototype\\src\\main\\resources\\process.bpmn");
		API a2 = new API("C:\\Users\\Micha\\git\\master-thesis-prototype\\master-thesis-prototype\\src\\main\\resources\\process1.bpmn");
		

		HashMap<BPMNDataObject, ArrayList<BPMNTask>> participantList = new HashMap<BPMNDataObject, ArrayList<BPMNTask>>();
		
		
		 JFrame frame=new JFrame("GUITest");
		    frame.setVisible(true);		 
		 
		 JPanel panel = new JPanel();		   
		    panel.add(new JLabel("Choose participants"));
		    panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		    
		   
		    ArrayList<JCheckBoxWithId> checkboxes = new ArrayList<JCheckBoxWithId>();
		   
		    
		    for(BPMNTask t: a2.lastWriters) {
		    	panel.add(new JLabel("Writer: "+t.getName()));
		    	   	for(Entry<BPMNDataObject, ArrayList<BPMNTask>> t2: t.getSDHashMap().entrySet()) {
		    	   		panel.add(new JLabel(t2.getKey().getName()+" Strong-Dynamic:"));
		    		for(BPMNTask task: t2.getValue()) {
		    		JCheckBoxWithId box = new JCheckBoxWithId(task.getParticipant().getName()+ ", "+task.getName(),t, t.getSDHashMap(), task);
		    		checkboxes.add(box);
		    		panel.add(box);
		    		}
		    	}
		    	
		    	for(Entry<BPMNDataObject, ArrayList<BPMNTask>> t2: t.getWDHashMap().entrySet()) {
		    		panel.add(new JLabel(t2.getKey().getName()+" Weak-Dynamic:"));
		    		for(BPMNTask task: t2.getValue()) {
		    		JCheckBoxWithId box = new JCheckBoxWithId(task.getParticipant().getName()+ ", "+task.getName(),t,t.getWDHashMap(),task);
		    		checkboxes.add(box);
		    		panel.add(box);
		    		}
		    	}
		    	
		    }
		    
		    
		    
		    
		    JButton button = new JButton("Add marked participants to voting");
		    button.addActionListener(new ActionListener() {		    	
		        @Override
		        public void actionPerformed(ActionEvent e) {
		           for(JCheckBoxWithId checkBox: checkboxes) {
		        	   if(checkBox.isSelected()) {
		        		   for(BPMNDataObject dataKey: checkBox.map.keySet()) {	
		        			   if(participantList.get(dataKey)==null) {
			        			   ArrayList<BPMNTask> list = new ArrayList<BPMNTask>();
			        			   list.add(checkBox.t2);
			        			   participantList.put(dataKey, list);			        			   
		        		   } else {
		        			   participantList.get(dataKey).add(checkBox.t2);
		        		   }
		        		          			   
		        		  
		        		   } 
		        		 
		        		 
		        	   }
		           }
		       
		          try {
					a2.addVotingTasksToProcess(((BPMNBusinessRuleTask)a2.getNodeById("BusinessRuleTask_165zomh")), participantList);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
		        }
		        
		    });
		   
		   
		    panel.add(button);
		    frame.add(panel);
		    frame.pack();
		    
		  
		    /*
		    JDialog meinJDialog = new JDialog();
	        meinJDialog.setTitle("Model has to be repaired");
	     
	        meinJDialog.setSize(200,200);
	      
	        meinJDialog.setModal(true);
	        meinJDialog.setVisible(true);
			*/
	}

}
