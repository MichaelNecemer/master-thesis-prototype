package mapping;



//Parallel Gateways are non binary
public class BPMNParallelGateway extends BPMNGateway{
	
	private static int votingTaskCount = 0;
	
	public BPMNParallelGateway(String id, String name, String type) {		
		super(id, name, type);
	}
		
	public void printElement() {
		super.printElement();
		System.out.println(", "+this.getType());
		this.printLabels();
	}

	public static int getVotingTaskCount() {
		return votingTaskCount;
	}
	
	public static int increaseVotingTaskCount() {
		return ++votingTaskCount;
	}

	
	

}
