package functionality;

import mapping.BPMNBusinessRuleTask;
import mapping.BPMNDataObject;
import mapping.BPMNTask;

public class WeightWRD {
	// this class holds information about the weight(w,r,d) i.e. the amount of instance types
	// in which writer/origin writes data object for reader
	
	private BPMNTask origin;
	private BPMNBusinessRuleTask readerBrt;
	private BPMNDataObject dataO;
	private double weightWRD;
	
	public WeightWRD(BPMNTask origin, BPMNBusinessRuleTask readerBrt, BPMNDataObject dataO) {
		this.origin = origin;
		this.readerBrt = readerBrt; 
		this.dataO = dataO;
		this.weightWRD = 0;	
	}

	public BPMNTask getOrigin() {
		return origin;
	}

	public void setOrigin(BPMNTask origin) {
		this.origin = origin;
	}

	
	public BPMNBusinessRuleTask getReaderBrt() {
		return readerBrt;
	}

	public void setReaderBrt(BPMNBusinessRuleTask readerBrt) {
		this.readerBrt = readerBrt;
	}

	public BPMNDataObject getDataO() {
		return dataO;
	}

	public void setDataO(BPMNDataObject dataO) {
		this.dataO = dataO;
	}

	public double getWeightWRD() {
		return weightWRD;
	}

	public void setWeightWRD(double weightWRD) {
		this.weightWRD = weightWRD;
	}

	
	
	
}
