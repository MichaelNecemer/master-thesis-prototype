package Mapping;

import java.util.HashMap;

//Exclusive Gateways are binary
public class BPMNExclusiveGateway extends BPMNGateway{
	
	
	private int amountVoters;
		
	public BPMNExclusiveGateway(String id, String name, String type) {
		super(id, name, type);
		this.amountVoters=0;		
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

}
