package Mapping;

import java.util.LinkedList;


public class VoterForXorArc{
	
	public static int id;
	public int idOfArc;
	
	private BPMNBusinessRuleTask brt;
	private BPMNExclusiveGateway xorSplit;
	
	//the chosenCombinationOfParticipants is a specific combination 
	private LinkedList<BPMNParticipant> chosenCombinationOfParticipants;
	
	
	//the cost for adding the chosenCombinationOfParticipants for this arc
	private double cost;
	
	
	private LinkedList<RequiredUpdate> requiredUpdates;

	
	public VoterForXorArc(BPMNBusinessRuleTask brt, BPMNExclusiveGateway xorSplit, LinkedList<BPMNParticipant>chosenCombinationOfParticipants) {
		this.brt=brt;
		this.xorSplit=xorSplit;
		this.chosenCombinationOfParticipants=chosenCombinationOfParticipants;	
		this.cost=1;
		this.idOfArc=++id;
	}
	
	
	



	public LinkedList<RequiredUpdate> getRequiredUpdates() {
		return requiredUpdates;
	}






	public void setRequiredUpdates(LinkedList<RequiredUpdate> requiredUpdates) {
		this.requiredUpdates = requiredUpdates;
	}




	public BPMNBusinessRuleTask getBrt() {
		return brt;
	}






	public void setBrt(BPMNBusinessRuleTask brt) {
		this.brt = brt;
	}






	public BPMNExclusiveGateway getXorSplit() {
		return xorSplit;
	}






	public void setXorSplit(BPMNExclusiveGateway xorSplit) {
		this.xorSplit = xorSplit;
	}






	

	


	public LinkedList<BPMNParticipant> getChosenCombinationOfParticipants() {
		return chosenCombinationOfParticipants;
	}



	public void setChosenCombinationOfParticipants(LinkedList<BPMNParticipant> chosenCombinationOfParticipants) {
		this.chosenCombinationOfParticipants = chosenCombinationOfParticipants;
	}

	

	


	public double getCost() {
		return cost;
	}


	public void setCost(double cost) {
		this.cost = cost;
	}
	
	public void printArc() {
		System.out.println("Following participants chosen for " +this.brt.getName()+" (ID: "+this.idOfArc+")");
		this.getChosenCombinationOfParticipants().forEach(f -> {
			f.printParticipant();
		});
		
	}
	

}
