package Mapping;

import java.util.LinkedList;

import functionality.API;

//Class holding all necessary Information regarding costs etc. between one BusinessRuleTask and a succeeding one
public class BrtToBrtArc{
	//uniquely identifiable by looking at all three variables currentBPMNElement, targetBPMNElement, and the chosenCombinationOfParticipants
	
	private BPMNBusinessRuleTask currentBPMNBrt;
	//target element can be also an End Event
	private BPMNElement targetBPMNElement;
	//all possible combinations of participants for a certain brt are annotated at the the brt
	//the chosenCombinationOfParticipants is a specific combination 
	private LinkedList<BPMNParticipant> chosenCombinationOfParticipants;
	
	//the cost for going from currentBPMNBrt adding the chosenCombinationOfParticipants to the targetBPMNBrt
	private double cost;
	private boolean costHasBeenSet;

	public BrtToBrtArc(BPMNBusinessRuleTask currentBPMNBrt, BPMNElement targetBPMNElement, LinkedList<BPMNParticipant>chosenCombinationOfParticipants) {
		this.currentBPMNBrt = currentBPMNBrt;
		this.targetBPMNElement = targetBPMNElement;
		this.chosenCombinationOfParticipants=chosenCombinationOfParticipants;	
		this.cost=1;
		this.costHasBeenSet=false;
	}
	
	
	
	
	
	public boolean isCostHasBeenSet() {
		return costHasBeenSet;
	}





	public void setCostHasBeenSet(boolean costHasBeenSet) {
		this.costHasBeenSet = costHasBeenSet;
	}





	public LinkedList<BPMNParticipant> getChosenCombinationOfParticipants() {
		return chosenCombinationOfParticipants;
	}



	public void setChosenCombinationOfParticipants(LinkedList<BPMNParticipant> chosenCombinationOfParticipants) {
		this.chosenCombinationOfParticipants = chosenCombinationOfParticipants;
	}



	public void calculateCostForArc(API api) {
		//go back to root to get unique path
		//what does it cost if the chosenCombinationOfParticipants vote for the currentBPMNBrt
		//check the possible paths between the currentBrt and the targetBrt
		//check the writers and possible sphere upgrades needed
		double cost = 1;
		//api.allPathsBetweenNodes(this.currentBPMNBrt, this.targetBPMNElement, new LinkedList<BPMNBusinessRuleTask>(), new LinkedList<BPMNBusinessRuleTask>(), new LinkedList<BPMNBusinessRuleTask>(), new LinkedList<LinkedList<BPMNBusinessRuleTask>>());
		
		
		//now calculate the path costs for the given paths
		//consider there are have been already voters chosen on the path
		this.cost=cost;
	}


	

	public BPMNBusinessRuleTask getCurrentBPMNBrt() {
		return currentBPMNBrt;
	}





	public void setCurrentBPMNBrt(BPMNBusinessRuleTask currentBPMNBrt) {
		this.currentBPMNBrt = currentBPMNBrt;
	}





	public BPMNElement getTargetBPMNElement() {
		return targetBPMNElement;
	}





	public void setTargetBPMNElement(BPMNElement targetBPMNElement) {
		this.targetBPMNElement = targetBPMNElement;
	}





	public double getCost() {
		return cost;
	}


	public void setCost(double cost) {
		this.cost = cost;
	}
	
	public void printBrtToBrtArc() {
		if(currentBPMNBrt instanceof BPMNTask && targetBPMNElement instanceof BPMNTask) {
			System.out.println((((BPMNTask) this.currentBPMNBrt).getName()) +" TO "+(((BPMNTask) this.targetBPMNElement).getName()) +" WITH COST: "+this.cost);
		} else {
		System.out.println(this.currentBPMNBrt.getId() +" TO "+this.targetBPMNElement.getId() +" WITH COST: "+this.cost);
		}
		
		this.getChosenCombinationOfParticipants().forEach(f -> {
			f.printParticipant();
		});
	}
	

}
