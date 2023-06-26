package functionality;

import java.util.Objects;

import mapping.BPMNDataObject;
import mapping.BPMNParticipant;
import mapping.BPMNTask;

public class DependentEntry {

	private BPMNParticipant participant;
	private BPMNTask origin;
	private BPMNDataObject dataO;

	public DependentEntry(BPMNParticipant participant, BPMNTask origin, BPMNDataObject dataO) {
		this.participant = participant;
		this.origin = origin;
		this.dataO = dataO;
	}
	
	
	
	public BPMNParticipant getParticipant() {
		return participant;
	}



	public void setParticipant(BPMNParticipant participant) {
		this.participant = participant;
	}



	public BPMNTask getOrigin() {
		return origin;
	}



	public void setOrigin(BPMNTask origin) {
		this.origin = origin;
	}



	public BPMNDataObject getDataO() {
		return dataO;
	}



	public void setDataO(BPMNDataObject dataO) {
		this.dataO = dataO;
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
		DependentEntry other = (DependentEntry) obj;
		return Objects.equals(this.participant, other.participant) && Objects.equals(this.origin, other.getOrigin())
				&& Objects.equals(this.dataO, other.dataO);
	}

	@Override
	public final int hashCode() {
		int result = 17;
		if (this.dataO != null) {
			result = 31 * result + this.dataO.hashCode();
		}
		if (this.origin != null) {
			result = 31 * result + this.origin.hashCode();
		}
		if (this.participant != null) {
			result = 31 * result + this.participant.hashCode();
		}
		

		return result;
	}

}
