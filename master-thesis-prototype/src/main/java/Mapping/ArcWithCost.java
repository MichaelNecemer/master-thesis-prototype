package Mapping;

import java.util.LinkedList;

import functionality.API;

//Class holding all necessary Information regarding costs and dependencies between BusinessRuleTasks in the process
public class ArcWithCost{
	//The current Arc object holds list to all preceeding arcs -> uniquely identifiable	
	//what does it cost when chosenCombinationOfParticipants are added as voters for the xorSplit
	
	public static int id;
	public int idOfArc;
	private LinkedList<ArcWithCost>preceedingArcs;
	//first arc goes from first brt into first XOR-Split
	private BPMNBusinessRuleTask brt;
	
	//the xorSplit should hold all ArcWithCost from all branches going into it
	//since the arcs already contain the cumulated necessary updates, just get the last one from all branches 
	//and cumulate costs
	
	private BPMNExclusiveGateway xorSplit;
	private boolean isLeaf;
	
	
	//the chosenCombinationOfParticipants is a specific combination 
	private LinkedList<BPMNParticipant> chosenCombinationOfParticipants;
	
	
	//the cost for adding the chosenCombinationOfParticipants for this arc
	private double cost;
	
	
	//the cumulated costs for adding the chosenCombinationOfParticipants for this arc and looping through preceeding Arcs and calculating the costs
	private double cumulatedCost;
	
	
	
	private LinkedList<RequiredUpgrade> requiredUpdates;

	
	public ArcWithCost(BPMNBusinessRuleTask brt, BPMNExclusiveGateway xorSplit, LinkedList<ArcWithCost>preceedingArcs, LinkedList<BPMNParticipant>chosenCombinationOfParticipants) {
		this.brt=brt;
		this.xorSplit=xorSplit;
		this.chosenCombinationOfParticipants=chosenCombinationOfParticipants;	
		this.cost=1;
		this.cumulatedCost=1;
		this.preceedingArcs=preceedingArcs;
		this.isLeaf=false;
		this.idOfArc=++id;
		this.requiredUpdates=new LinkedList<RequiredUpgrade>();
	}
	
	
	



	public LinkedList<RequiredUpgrade> getRequiredUpdates() {
		return requiredUpdates;
	}






	public void setRequiredUpdates(LinkedList<RequiredUpgrade> requiredUpdates) {
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






	public void setPreceedingBrtToBrtArcs(LinkedList<ArcWithCost> preceedingBrtToBrtArcs) {
		this.preceedingArcs = preceedingBrtToBrtArcs;
	}



	public boolean isLeaf() {
		return isLeaf;
	}





	public void setLeaf(boolean isLeaf) {
		this.isLeaf = isLeaf;
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



	public double getCumulatedCost() {
		return cumulatedCost;
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
		System.out.println("Following participants chosen for " +this.brt.getName()+" (ID: "+this.idOfArc+")");
		this.getChosenCombinationOfParticipants().forEach(f -> {
			f.printParticipant();
		});
		//this.requiredUpdates.forEach(reqUpdate->{reqUpdate.printUpdate();});
		
		if(!this.preceedingArcs.isEmpty()) {
			System.out.println("Preceedings Arcs:");
			this.preceedingArcs.forEach(f->{f.printArc();});
		} else {
			System.out.println("***************************");
		}
	}
	

}
