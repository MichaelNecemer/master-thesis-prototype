package mapping;

import java.util.LinkedList;


public class VoterForXorArc{
	
	public static int id;
	public int idOfArc;
	
	private BPMNBusinessRuleTask brt;
	private BPMNExclusiveGateway xorSplit;
	
	//the chosenCombinationOfParticipants is a specific combination 
	private LinkedList<BPMNParticipant> chosenCombinationOfParticipants;
	
	
	public VoterForXorArc(BPMNBusinessRuleTask brt, BPMNExclusiveGateway xorSplit, LinkedList<BPMNParticipant>chosenCombinationOfParticipants) {
		this.brt=brt;
		this.xorSplit=xorSplit;
		this.chosenCombinationOfParticipants=chosenCombinationOfParticipants;	
		this.idOfArc=++id;
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

	

	public void printArc() {
		System.out.println("Following participants chosen for " +this.brt.getName()+" (ID: "+this.idOfArc+")");
		this.getChosenCombinationOfParticipants().forEach(f -> {
			f.printParticipant();
		});
		
	}
	
	
	

}
