package Mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

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
	
	private HashMap<BPMNDataObject, ArrayList<BPMNParticipant>> weakDynamicHashMap;
	private HashMap<BPMNDataObject, ArrayList<BPMNParticipant>> strongDynamicHashMap;
	
	private HashMap<Boolean, LinkedList<LinkedList<BPMNElement>>> effectivePaths;
	

	public BPMNTask (String id, String name) {
		super(id);
		this.name = name;
		this.dataObjects = new ArrayList<BPMNDataObject>();
		this.sphereAnnotation = new HashMap<BPMNDataObject, String>();
		this.weakDynamicHashMap= new HashMap<BPMNDataObject, ArrayList<BPMNParticipant>>();
		this.strongDynamicHashMap = new HashMap<BPMNDataObject, ArrayList<BPMNParticipant>>();
		this.effectivePaths = new HashMap<Boolean, LinkedList<LinkedList<BPMNElement>>>();
		}
		
	
	
	public HashMap<BPMNDataObject, ArrayList<BPMNParticipant>> getWeakDynamicHashMap() {
		return weakDynamicHashMap;
	}
	
	


	public HashMap<Boolean, LinkedList<LinkedList<BPMNElement>>> getEffectivePaths() {
		return effectivePaths;
	}



	public void setEffectivePaths(HashMap<Boolean, LinkedList<LinkedList<BPMNElement>>> effectivePaths) {
		this.effectivePaths = effectivePaths;
	}



	public void setWeakDynamicHashMap(HashMap<BPMNDataObject, ArrayList<BPMNParticipant>> weakDynamicHashMap) {
		this.weakDynamicHashMap = weakDynamicHashMap;
	}







	public HashMap<BPMNDataObject, ArrayList<BPMNParticipant>> getStrongDynamicHashMap() {
		return strongDynamicHashMap;
	}







	public void setStrongDynamicHashMap(HashMap<BPMNDataObject, ArrayList<BPMNParticipant>> strongDynamicHashMap) {
		this.strongDynamicHashMap = strongDynamicHashMap;
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
	


	
	public static int getVotingTaskId() {
		return votingTaskId;
	}
	
	public static int increaseVotingTaskId() {
		return ++votingTaskId;
	}
	
	
	
}
