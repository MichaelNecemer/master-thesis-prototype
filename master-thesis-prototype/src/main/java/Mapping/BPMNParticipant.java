package Mapping;

//Note: The process has exactly 1 pool. Each lane is a participant of the process doing some tasks (e.g. different companies are represented as lanes and not pools)
//Each lane of the process is mapped to a participant
public class BPMNParticipant {
	
	private String id;
	private String name;
	
	public BPMNParticipant(String id, String name) {
		this.id = id;
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public void printParticipant() {
		System.out.println(this.name);
	}
	
	
	

}
