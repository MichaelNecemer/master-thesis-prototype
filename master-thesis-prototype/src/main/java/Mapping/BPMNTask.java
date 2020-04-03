package Mapping;

import java.util.ArrayList;
import java.util.HashMap;

//A task has exactly one input sequence flow and one output sequence flow
//This is represented by having a predecessor and a successor 
public class BPMNTask extends BPMNElement{
	
	private static int votingTaskId = 0;
	
	private String name;
	private BPMNParticipant participant;
	//List of dataObjects connected to the task (task either reads or writes)
	private ArrayList<BPMNDataObject> dataObjects;
	
	//Writers can be attached the sphere for the written data object
	//Readers after that Task have to be at least in this sphere
	private HashMap<BPMNDataObject, String> sphereAnnotation;
	
	private HashMap<BPMNBusinessRuleTask, HashMap<BPMNDataObject, ArrayList<BPMNTask>>> weakDynamicHashMap;
	private HashMap<BPMNBusinessRuleTask, HashMap<BPMNDataObject, ArrayList<BPMNTask>>> strongDynamicHashMap;
	

	public BPMNTask (String id, String name) {
		super(id);
		this.name = name;
		this.dataObjects = new ArrayList<BPMNDataObject>();
		this.sphereAnnotation = new HashMap<BPMNDataObject, String>();
		this.weakDynamicHashMap=new HashMap<BPMNBusinessRuleTask, HashMap<BPMNDataObject, ArrayList<BPMNTask>>>();
		this.strongDynamicHashMap = new HashMap<BPMNBusinessRuleTask,HashMap<BPMNDataObject, ArrayList<BPMNTask>>>();
	}
	
	
	
	public String getName() {
		return this.name;
	}

	/*
	public void printElement() {
		System.out.println(this.id+", "+this.name+", "+this.participant.getName().trim()+", ");
		printDataObjectsConnected();
	}
	*/
	public void printElement() {
		System.out.println(this.id+", "+this.name+", ");
		this.labels.forEach(label->{label.printLabel();});
	}



	public BPMNParticipant getParticipant() {
		return participant;
	}



	public void setParticipant(BPMNParticipant participant) {
		this.participant = participant;
	}



	public void setName(String name) {
		this.name = name;
	}
	
	
	private void printDataObjectsConnected() {
		for(BPMNDataObject b: this.dataObjects) {
			System.out.println(b.getId()+", "+b.getName()+"\n");
		}
	}
	
	public void addBPMNDataObject(BPMNDataObject d) {
		this.dataObjects.add(d);
	}



	public ArrayList<BPMNDataObject> getDataObjects() {
		return dataObjects;
	}



	public void setDataObjects(ArrayList<BPMNDataObject> dataObjects) {
		this.dataObjects = dataObjects;
	}



	public HashMap<BPMNDataObject, String> getSphereAnnotation() {
		return sphereAnnotation;
	}



	public void setSphereAnnotation(HashMap<BPMNDataObject, String> sphereAnnotation) {
		this.sphereAnnotation = sphereAnnotation;
	}
	
	
	public void addTaskToWDHashMap(BPMNBusinessRuleTask brt, BPMNDataObject bpmndo, BPMNTask reader) {
		if(this.weakDynamicHashMap.get(brt)==null) {
			this.weakDynamicHashMap.put(brt, new HashMap<BPMNDataObject, ArrayList<BPMNTask>>());
			
			if(this.weakDynamicHashMap.get(brt).get(bpmndo)==null) {
				this.weakDynamicHashMap.get(brt).put(bpmndo, new ArrayList<BPMNTask>());
			}
			
		}	
		
		this.weakDynamicHashMap.get(brt).get(bpmndo).add(reader);
		
		
	}
	
	public void addTaskToSDHashMap(BPMNBusinessRuleTask brt, BPMNDataObject bpmndo, BPMNTask reader) {
		if(this.strongDynamicHashMap.get(brt)==null) {
			this.strongDynamicHashMap.put(brt, new HashMap<BPMNDataObject, ArrayList<BPMNTask>>());
					
			if(this.strongDynamicHashMap.get(brt).get(bpmndo)==null) {
				this.strongDynamicHashMap.get(brt).put(bpmndo, new ArrayList<BPMNTask>());
			}
			
		}	
		
		this.strongDynamicHashMap.get(brt).get(bpmndo).add(reader);
		
	}



	public HashMap<BPMNBusinessRuleTask, HashMap<BPMNDataObject, ArrayList<BPMNTask>>> getWDHashMap() {
		return this.weakDynamicHashMap;
	}



	public void setWDHashMap(HashMap<BPMNBusinessRuleTask, HashMap<BPMNDataObject, ArrayList<BPMNTask>>> wdHashMap) {
		this.weakDynamicHashMap = wdHashMap;
	}



	public HashMap<BPMNBusinessRuleTask, HashMap<BPMNDataObject, ArrayList<BPMNTask>>> getSDHashMap() {
		return this.strongDynamicHashMap;
	}



	public void setStrongDynamicList(HashMap<BPMNBusinessRuleTask, HashMap<BPMNDataObject, ArrayList<BPMNTask>>> sdHashMap) {
		this.strongDynamicHashMap = sdHashMap;
	}
	
	public static int getVotingTaskId() {
		return votingTaskId;
	}
	
	public static int increaseVotingTaskId() {
		return ++votingTaskId;
	}
	
}
