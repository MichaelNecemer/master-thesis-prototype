package functionality;

import java.util.HashSet;

import mapping.BPMNBusinessRuleTask;
import mapping.BPMNDataObject;
import mapping.BPMNParticipant;
import mapping.BPMNTask;

public class PriorityListEntry {
	
	private BPMNTask origin;
	private BPMNDataObject dataObject;
	private BPMNParticipant reader;
	private HashSet<BPMNBusinessRuleTask>dependentBrts;
	private double amountPathsWhereReaderReadsDataObject;
	private double amountPathsWhereReaderReadsDataObjectWithAdditionalActors;
	private double fractionOfPathsWhereReaderReadsDataObject;
	private double fractionOfPathsWhereReaderReadsDataObjectWithAddActors;
	// fractionOfPathsWhereParticipantOccurs will be the fraction of paths from origin to end where a participant occurs (no other writer in between) 
	// in this case the participant does not have to be a reader of the data object 
	private double minLabelSizeOfReader;
	private double penaltyForReading;
	private double penaltyForReadingWithAdditionalActors;
	
	public PriorityListEntry(BPMNTask origin, BPMNDataObject dataObject, BPMNParticipant reader,HashSet<BPMNBusinessRuleTask>dependentBrts, double amountPathsWhereReaderReadsDataObject, double amountPathsWhereReaderReadsDataObjectWithAdditionalActors, double fractionOfPathsWhereReaderReadsDataObjectWithoutAddActors, double fractionOfPathsWhereReaderReadsDataObjectWithAddActors, double minLabelSizeOfReader, double penaltyForReading, double penaltyForReadingWithAdditionalActors) {
		this.origin = origin;
		this.dataObject = dataObject;
		this.reader = reader; 
		this.dependentBrts = dependentBrts;
		this.amountPathsWhereReaderReadsDataObject = amountPathsWhereReaderReadsDataObject;
		this.amountPathsWhereReaderReadsDataObjectWithAdditionalActors = amountPathsWhereReaderReadsDataObjectWithAdditionalActors;
		this.fractionOfPathsWhereReaderReadsDataObject = fractionOfPathsWhereReaderReadsDataObjectWithoutAddActors;
		this.fractionOfPathsWhereReaderReadsDataObjectWithAddActors = fractionOfPathsWhereReaderReadsDataObjectWithAddActors;
		this.penaltyForReading = penaltyForReading;
		this.minLabelSizeOfReader = minLabelSizeOfReader;
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

	
	public double getMinLabelSizeOfReader() {
		return minLabelSizeOfReader;
	}

	public void setMinLabelSizeOfReader(double minLabelSizeOfReader) {
		this.minLabelSizeOfReader = minLabelSizeOfReader;
	}

	public double getFractionOfPathsWhereReaderReadsDataObjectWithAddActors() {
		return fractionOfPathsWhereReaderReadsDataObjectWithAddActors;
	}

	public void setFractionOfPathsWhereReaderReadsDataObjectWithAddActors(
			double fractionOfPathsWhereReaderReadsDataObjectWithAddActors) {
		this.fractionOfPathsWhereReaderReadsDataObjectWithAddActors = fractionOfPathsWhereReaderReadsDataObjectWithAddActors;
	}

	public HashSet<BPMNBusinessRuleTask> getDependentBrts() {
		return dependentBrts;
	}

	public void setDependentBrts(HashSet<BPMNBusinessRuleTask> dependentBrts) {
		this.dependentBrts = dependentBrts;
	}

	public double getPenaltyForReadingWithAdditionalActors() {
		return penaltyForReadingWithAdditionalActors;
	}

	public void setPenaltyForReadingWithAdditionalActors(double penaltyForReadingWithAdditionalActors) {
		this.penaltyForReadingWithAdditionalActors = penaltyForReadingWithAdditionalActors;
	}

	public double getAmountPathsWhereReaderReadsDataObjectWithAdditionalActors() {
		return amountPathsWhereReaderReadsDataObjectWithAdditionalActors;
	}

	public void setAmountPathsWhereReaderReadsDataObjectWithAdditionalActors(
			double amountPathsWhereReaderReadsDataObjectWithAdditionalActors) {
		this.amountPathsWhereReaderReadsDataObjectWithAdditionalActors = amountPathsWhereReaderReadsDataObjectWithAdditionalActors;
	}	
	
	
			

}
