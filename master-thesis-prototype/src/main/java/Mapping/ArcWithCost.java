package Mapping;

import java.util.LinkedList;

import functionality.API;

//Class holding all necessary Information regarding costs and dependencies between BusinessRuleTasks in the process
public class ArcWithCost{
	//The current Arc object holds list to all preceeding arcs -> uniquely identifiable	
	//what does it cost when chosenCombinationOfParticipants are added as voters?
	
	public static int id;
	public int idOfArc;
	private LinkedList<ArcWithCost>preceedingArcs;
	//first arc goes into the first bpmnBrt 
	private BPMNElement preceedingBpmnBrt;
	
	private BPMNElement currentBpmnBrt;
	private boolean isLeaf;
	
	
	//the chosenCombinationOfParticipants is a specific combination 
	private LinkedList<BPMNParticipant> chosenCombinationOfParticipants;
	
	
	//the cost for adding the chosenCombinationOfParticipants for this arc
	private double cost;
	
	
	//the cumulated costs for adding the chosenCombinationOfParticipants for this arc and looping through preceeding Arcs and calculating the costs
	private double cumulatedCost;
	private boolean costHasBeenSet;
	
	//
	private LinkedList<RequiredUpdate> requiredUpdates;

	
	public ArcWithCost(BPMNElement preceedingBpmnBrt, BPMNElement targetBPMNElement, LinkedList<ArcWithCost>preceedingArcs, LinkedList<BPMNParticipant>chosenCombinationOfParticipants) {
		this.preceedingBpmnBrt = preceedingBpmnBrt;
		this.currentBpmnBrt = targetBPMNElement;
		this.chosenCombinationOfParticipants=chosenCombinationOfParticipants;	
		this.cost=1;
		this.cumulatedCost=1;
		this.costHasBeenSet=false;
		this.preceedingArcs=preceedingArcs;
		this.isLeaf=false;
		this.idOfArc=++id;
		this.requiredUpdates=new LinkedList<RequiredUpdate>();
	}
	
	
	



	public LinkedList<RequiredUpdate> getRequiredUpdates() {
		return requiredUpdates;
	}






	public void setRequiredUpdates(LinkedList<RequiredUpdate> requiredUpdates) {
		this.requiredUpdates = requiredUpdates;
	}






	public LinkedList<ArcWithCost> getPreceedingBrtToBrtArcs() {
		return preceedingArcs;
	}





	public void setPreceedingBrtToBrtArcs(LinkedList<ArcWithCost> preceedingBrtToBrtArcs) {
		this.preceedingArcs = preceedingBrtToBrtArcs;
	}



	public boolean isLeaf() {
		return isLeaf;
	}





	public void setLeaf(boolean isLeaf) {
		this.isLeaf = isLeaf;
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

	

	

	public LinkedList<ArcWithCost> getPreceedingArcs() {
		return preceedingArcs;
	}



	public void setPreceedingArcs(LinkedList<ArcWithCost> preceedingArcs) {
		this.preceedingArcs = preceedingArcs;
	}



	public BPMNElement getPreceedingBpmnBrt() {
		return preceedingBpmnBrt;
	}



	public void setPreceedingBpmnBrt(BPMNElement preceedingBpmnBrt) {
		this.preceedingBpmnBrt = preceedingBpmnBrt;
	}



	public double getCumulatedCost() {
		return cumulatedCost;
	}



	public BPMNElement getCurrentBpmnBrt() {
		return currentBpmnBrt;
	}



	public void setCurrentBpmnBrt(BPMNElement currentBpmnBrt) {
		this.currentBpmnBrt = currentBpmnBrt;
	}



	public void setCumulatedCost(double cumulatedCost) {
		this.cumulatedCost = cumulatedCost;
	}



	public double getCost() {
		return cost;
	}


	public void setCost(double cost) {
		this.cost = cost;
	}
	
	public void printArc() {
		System.out.println("Following participants chosen for " +((BPMNTask)this.getCurrentBpmnBrt()).getName()+" (ID: "+this.idOfArc+")");
		this.getChosenCombinationOfParticipants().forEach(f -> {
			f.printParticipant();
		});
		
		if(!this.preceedingArcs.isEmpty()) {
			System.out.println("Preceedings Arcs:");
			this.preceedingArcs.forEach(f->{f.printArc();});
		} else {
			System.out.println("***************************");
		}
	}
	

}
