package mapping;

import java.util.Objects;

//Note: The process has exactly 1 pool. Each lane is a participant of the process doing some tasks (e.g. different companies are represented as lanes and not pools)
//Each lane of the process is mapped to a participant
public class BPMNParticipant implements Comparable<BPMNParticipant> {

	private String id;
	private String name;
	private String nameWithoutBrackets;

	public BPMNParticipant(String name) {
		this.name = name;
		if (name.contains("[") && name.contains("]")) {
			this.nameWithoutBrackets = name.substring(name.indexOf('[') + 1, name.indexOf(']'));
		} else {
			this.nameWithoutBrackets = name;
		}
		this.id = name;
	}

	public BPMNParticipant(String id, String name) {
		this.id = id;
		this.name = name;
		if (name.contains("[") && name.contains("]")) {
			this.nameWithoutBrackets = name.substring(name.indexOf('[') + 1, name.indexOf(']'));
		} else {
			this.nameWithoutBrackets = name;
		}

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

	public String getNameWithoutBrackets() {
		return this.nameWithoutBrackets;
	}

	public void printParticipant() {
		System.out.println(this.name);
	}

	public boolean equals(Object obj) {

		// same instance
		if (obj == this) {
			return true;
		}
		// null
		if (obj == null) {
			return false;
		}
		// type
		if (!getClass().equals(obj.getClass())) {
			return false;
		}
		// cast and compare state
		BPMNParticipant other = (BPMNParticipant) obj;
		return Objects.equals(this.name, other.name) && Objects.equals(this.id, other.id);
	}

	@Override
	public final int hashCode() {
		int result = 17;
		if (this.id != null) {
			result = 31 * result + this.id.hashCode();
		}
		if (this.name != null) {
			result = 31 * result + this.name.hashCode();
		}
		return result;
	}

	@Override
	public int compareTo(BPMNParticipant p2) {
		// TODO Auto-generated method stub
		return this.getId().compareTo(p2.getId());		
	}

	

}
