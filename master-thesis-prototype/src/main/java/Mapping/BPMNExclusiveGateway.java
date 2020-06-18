package Mapping;

import java.util.HashMap;

//Exclusive Gateways are binary
public class BPMNExclusiveGateway extends BPMNGateway{
	
	
	private int amountVoters;
	private int votersSameChoice;
	private int amountLoops;
	private static int exclusiveGtwCount = 0;
		
	public BPMNExclusiveGateway(String id, String name, String type) {
		super(id, name, type);
		//by default a gateway needs 1 voter and no loops
		this.amountVoters=1;	
		this.votersSameChoice=1;
		this.amountLoops=0;
	}

	
	public void printElement() {
		super.printElement();
		System.out.println(", "+this.getType());
		this.printLabels();
	}


	
	public int getAmountVoters() {
		return this.amountVoters;
	}
	
	public void setAmountVoters(int amountVoters) {
		this.amountVoters=amountVoters;
	}


	public int getVotersSameChoice() {
		return votersSameChoice;
	}


	public void setVotersSameChoice(int votersSameChoice) {
		this.votersSameChoice = votersSameChoice;
	}


	public int getAmountLoops() {
		return amountLoops;
	}


	public void setAmountLoops(int amountLoops) {
		this.amountLoops = amountLoops;
	}


	public static int getExclusiveGtwCount() {
		return exclusiveGtwCount;
	}

	public static int increaseExclusiveGtwCount() {
		return ++exclusiveGtwCount;
	}
	
	

}
