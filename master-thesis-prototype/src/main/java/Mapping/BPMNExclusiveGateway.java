package Mapping;

import java.util.HashMap;

//Exclusive Gateways are binary
public class BPMNExclusiveGateway extends BPMNGateway{
	
	private HashMap<BPMNDataObject, Integer> voters;
	private int cumulatedVoters;
		
	public BPMNExclusiveGateway(String id, String name, String type) {
		super(id, name, type);
		this.voters = new HashMap<BPMNDataObject, Integer>();
		this.cumulatedVoters=0;		
	}

	
	public void printElement() {
		super.printElement();
		System.out.println(", "+this.getType());
		this.printLabels();
	}

	public void addVoter(BPMNDataObject bpmndo, int amount) {
		if(this.voters.get(bpmndo)==null) {
			this.voters.put(bpmndo, amount);
			this.cumulatedVoters+=amount;
		} 		
	}


	public HashMap<BPMNDataObject, Integer> getVoters() {
		return voters;
	}
	
	public int getCumulatedVoters() {
		return this.cumulatedVoters;
	}
	
	

}
