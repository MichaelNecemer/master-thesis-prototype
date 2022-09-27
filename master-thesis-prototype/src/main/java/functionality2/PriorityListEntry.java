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
	private double fractionOfPaths;
	private double penaltyForReading;
	
	public PriorityListEntry(BPMNTask origin, BPMNDataObject dataObject, BPMNParticipant reader, double amountPathsWhereReaderReadsDataObject, double fractionOfPaths, double penaltyForReading) {
		this.origin = origin;
		this.dataObject = dataObject;
		this.reader = reader; 
		this.amountPathsWhereReaderReadsDataObject = amountPathsWhereReaderReadsDataObject;
		this.fractionOfPaths = fractionOfPaths;
		this.penaltyForReading = penaltyForReading;
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

	public double getFractionOfPaths() {
		return fractionOfPaths;
	}

	public void setFractionOfPaths(double fractionOfPaths) {
		this.fractionOfPaths = fractionOfPaths;
	}
	
		

}
