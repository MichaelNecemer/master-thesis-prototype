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
	
	
	public PriorityListEntry(BPMNTask origin, BPMNDataObject dataObject, BPMNParticipant reader, double amountPathsWhereReaderReadsDataObject) {
		this.origin = origin;
		this.dataObject = dataObject;
		this.reader = reader; 
		this.amountPathsWhereReaderReadsDataObject = amountPathsWhereReaderReadsDataObject;
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
	
	

}
