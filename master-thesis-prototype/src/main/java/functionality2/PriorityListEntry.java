package functionality2;

import Mapping.BPMNDataObject;
import Mapping.BPMNElement;
import Mapping.BPMNParticipant;
import Mapping.BPMNTask;

public class PriorityListEntry {
	
	private BPMNTask origin;
	private BPMNDataObject dataObject;
	private BPMNParticipant reader;
	private double amountPathsWhereReaderReadsDataObject;
	private double fractionOfPathsWhereReaderReadsDataObject;
	// fractionOfPathsWhereParticipantOccurs will be the fraction of paths from origin to end where a participant occurs (no other writer in between) 
	// in this case the participant does not have to be a reader of the data object 
	private double fractionOfPathsWhereParticipantOccurs;
	private double penaltyForReading;
	
	public PriorityListEntry(BPMNTask origin, BPMNDataObject dataObject, BPMNParticipant reader, double amountPathsWhereReaderReadsDataObject, double fractionOfPathsWhereReaderReadsDataObject, double fractionOfPathsWhereParticipantOccurs, double penaltyForReading) {
		this.origin = origin;
		this.dataObject = dataObject;
		this.reader = reader; 
		this.amountPathsWhereReaderReadsDataObject = amountPathsWhereReaderReadsDataObject;
		this.fractionOfPathsWhereReaderReadsDataObject = fractionOfPathsWhereReaderReadsDataObject;
		this.penaltyForReading = penaltyForReading;
		this.fractionOfPathsWhereParticipantOccurs = fractionOfPathsWhereParticipantOccurs;
	}

	public BPMNTask getOrigin() {
		return origin;
	}

	public void setOrigin(BPMNTask origin) {
		this.origin = origin;
	}

	public BPMNDataObject getDataObject() {
		return dataObject;
	}

	public void setDataObject(BPMNDataObject dataObject) {
		this.dataObject = dataObject;
	}

	public BPMNParticipant getReader() {
		return reader;
	}

	public void setReader(BPMNParticipant reader) {
		this.reader = reader;
	}

	public double getAmountPathsWhereReaderReadsDataObject() {
		return amountPathsWhereReaderReadsDataObject;
	}

	public void setAmountPathsWhereReaderReadsDataObject(double amountPathsWhereReaderReadsDataObject) {
		this.amountPathsWhereReaderReadsDataObject = amountPathsWhereReaderReadsDataObject;
	}

	public double getPenaltyForReading() {
		return penaltyForReading;
	}

	public void setPenaltyForReading(double penaltyForReading) {
		this.penaltyForReading = penaltyForReading;
	}

	public double getFractionOfPathsWhereReaderReadsDataObject() {
		return fractionOfPathsWhereReaderReadsDataObject;
	}

	public void setFractionOfPathsWhereReaderReadsDataObject(double fractionOfPathsWhereReaderReadsDataObject) {
		this.fractionOfPathsWhereReaderReadsDataObject = fractionOfPathsWhereReaderReadsDataObject;
	}

	public double getFractionOfPathsWhereParticipantOccurs() {
		return fractionOfPathsWhereParticipantOccurs;
	}

	public void setFractionOfPathsWhereParticipantOccurs(double fractionOfPathsWhereParticipantOccurs) {
		this.fractionOfPathsWhereParticipantOccurs = fractionOfPathsWhereParticipantOccurs;
	}

	
		

}
