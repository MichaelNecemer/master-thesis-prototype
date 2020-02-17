package Mapping;



public abstract class BPMNGateway extends BPMNElement {
	
	private String name;
	private String type;

	public BPMNGateway(String id, String name, String type) {
		super(id);
		this.type = type;
		this.name = name;
	}		

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public void printElement() {
		System.out.print(this.name);
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	
}
