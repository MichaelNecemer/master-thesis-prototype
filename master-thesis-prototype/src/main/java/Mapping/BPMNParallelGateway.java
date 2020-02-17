package Mapping;



//Parallel Gateways are non binary
public class BPMNParallelGateway extends BPMNGateway{

	
	public BPMNParallelGateway(String id, String name, String type) {		
		super(id, name, type);
	}
		
	public void printElement() {
		super.printElement();
		System.out.println(", "+this.getType());
		this.printLabels();
	}
	

}
