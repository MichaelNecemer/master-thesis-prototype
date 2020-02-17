package Mapping;



//Exclusive Gateways are binary
public class BPMNExclusiveGateway extends BPMNGateway{
	
	private int voters;
	
	
	public BPMNExclusiveGateway(String id, String name, String type) {
		super(id, name, type);
		this.voters = 0;
		
	}

	
	public void printElement() {
		super.printElement();
		System.out.println(", "+this.getType());
		this.printLabels();
	}


	public int getVoters() {
		return voters;
	}


	public void setVoters(int voters) {
		this.voters = voters;
	}
	
	
	
	
	

}
