package Mapping;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

public class ProcessInstanceWithVoters {
	//class holds information about the process model with a possible combination of voters for each brt as well as the required updates
	
	private static int processID;
	private int processInstanceID;
	private HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> votersMap;
	private HashMap<BPMNBusinessRuleTask, LinkedList<RequiredUpdate>>requiredUpdates;
	private double costForModelInstance;
	private LinkedList<VoterForXorArc>listOfArcs;
	private LinkedList<RequiredUpdate>listOfRequiredUpdates;
	
	public ProcessInstanceWithVoters() {
		this.processInstanceID=++processID;
		this.votersMap = new HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>>();
		this.requiredUpdates = new HashMap<BPMNBusinessRuleTask, LinkedList<RequiredUpdate>>();
		this.listOfArcs = new LinkedList<VoterForXorArc>();
		this.listOfRequiredUpdates=new LinkedList<RequiredUpdate>();
		this.costForModelInstance=0;
	}
	
	
	
	public LinkedList<RequiredUpdate> getListOfRequiredUpdates() {
		return listOfRequiredUpdates;
	}



	public void setListOfRequiredUpdates(LinkedList<RequiredUpdate> listOfRequiredUpdates) {
		this.listOfRequiredUpdates = listOfRequiredUpdates;
	}



	public void addVoterArcList(List<VoterForXorArc>voters) {
		for(VoterForXorArc arc: voters) {
			if(!listOfArcs.contains(arc)) {
				listOfArcs.add(arc);				
				this.getVotersMap().putIfAbsent(arc.getBrt(), arc.getChosenCombinationOfParticipants());
			}
		}
	}
	
	
	public void addVoterArc(VoterForXorArc voters) {
		if(!listOfArcs.contains(voters)) {
			listOfArcs.add(voters);			
			this.getVotersMap().putIfAbsent(voters.getBrt(), voters.getChosenCombinationOfParticipants());
		}
	}

	public HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> getVotersMap() {
		return votersMap;
	}


	public void setVotersMap(HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> votersMap) {
		this.votersMap = votersMap;
	}


	public double getCostForModelInstance() {
		return costForModelInstance;
	}


	public void setCostForModelInstance(double costForModelInstance) {
		this.costForModelInstance = costForModelInstance;
	}


	public HashMap<BPMNBusinessRuleTask, LinkedList<RequiredUpdate>> getRequiredUpdates() {
		return requiredUpdates;
	}


	public void setRequiredUpdates(HashMap<BPMNBusinessRuleTask, LinkedList<RequiredUpdate>> requiredUpdates) {
		this.requiredUpdates = requiredUpdates;
	}

	public void addVotersToBrt(BPMNBusinessRuleTask brt, LinkedList<BPMNParticipant>voters) {
		this.votersMap.putIfAbsent(brt, voters);
	}
	
	public void printProcessInstance() {
		System.out.println("********************************************");
		System.out.println("ProcessInstanceID: "+this.processInstanceID);
		System.out.println("Cost for ProcessInstance: "+this.costForModelInstance);
		for(VoterForXorArc arc: this.getListOfArcs()) {
			arc.printArc();
			for(RequiredUpdate update: this.listOfRequiredUpdates) {
				if(update.getCurrentBrt().equals(arc.getBrt())) {
				update.printUpdate();
				}
			}
		}
		
	}
	
	
	/*
	public void printProcessInstance() {
		System.out.println("ProcessInstanceID: "+this.processInstanceID);
		for(Entry<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> entry: votersMap.entrySet()) {
			System.out.print(entry.getKey().getName()+": ");
			for(BPMNParticipant p: entry.getValue()) {
				System.out.print(p.getName()+", ");
			}
			System.out.println();
		}
	}*/


	public LinkedList<VoterForXorArc> getListOfArcs() {
		return listOfArcs;
	}


	public void setListOfArcs(LinkedList<VoterForXorArc> listOfArcs) {
		this.listOfArcs = listOfArcs;
	}


	public int getProcessInstanceID() {
		return processInstanceID;
	}


	public void setProcessInstanceID(int processInstanceID) {
		this.processInstanceID = processInstanceID;
	}


	public static int getProcessID() {
		return processID;
	}


	public static void setProcessID(int processID) {
		ProcessInstanceWithVoters.processID = processID;
	}
	
	

}
