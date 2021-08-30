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
	private HashMap<BPMNBusinessRuleTask, LinkedList<RequiredUpgrade>>requiredUpgrades;
	private double costForModelInstance;
	private LinkedList<VoterForXorArc>listOfArcs;
	private LinkedList<RequiredUpgrade>listOfRequiredUpgrades;
	
	public ProcessInstanceWithVoters() {
		this.processInstanceID=++processID;
		this.votersMap = new HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>>();
		this.requiredUpgrades = new HashMap<BPMNBusinessRuleTask, LinkedList<RequiredUpgrade>>();
		this.listOfArcs = new LinkedList<VoterForXorArc>();
		this.listOfRequiredUpgrades=new LinkedList<RequiredUpgrade>();
		this.costForModelInstance=0;
	}
	
	
	
	public LinkedList<RequiredUpgrade> getListOfRequiredUpdates() {
		return listOfRequiredUpgrades;
	}



	public void setListOfRequiredUpgrades (LinkedList<RequiredUpgrade> listOfRequiredUpgrades) {
		this.listOfRequiredUpgrades = listOfRequiredUpgrades;
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


	public HashMap<BPMNBusinessRuleTask, LinkedList<RequiredUpgrade>> getRequiredUpdates() {
		return requiredUpgrades;
	}


	public void setRequiredUpdates(HashMap<BPMNBusinessRuleTask, LinkedList<RequiredUpgrade>> requiredUpdates) {
		this.requiredUpgrades = requiredUpdates;
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
			for(RequiredUpgrade update: this.listOfRequiredUpgrades) {
				if(update.getCurrentBrt().equals(arc.getBrt())) {
				update.printUpgrade();
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
