package functionality2;

import java.util.HashSet;
import java.util.LinkedList;

import Mapping.BPMNDataObject;
import Mapping.BPMNElement;
import Mapping.BPMNParticipant;
import Mapping.BPMNTask;

public class WD_SphereEntry {
	
	private BPMNDataObject dataObject;
	private BPMNTask origin;
	private LinkedList<AdditionalActors>additionalActors;
	private HashSet<BPMNParticipant>wdSphere;
	private HashSet<BPMNParticipant>wdSphereWithAdditionalActors;
	private HashSet<BPMNParticipant>lambdaWdSphere;
	private HashSet<BPMNTask>wdReaderBrts;
	private double weightingOfOrigin;
	private double score;
	
	public WD_SphereEntry(BPMNDataObject dataObject, BPMNTask origin, HashSet<BPMNTask>wdReaderBrts, HashSet<BPMNParticipant>wdSphere) {
		this.dataObject = dataObject;
		this.origin = origin;
		this.additionalActors = new LinkedList<AdditionalActors>();
		this.wdSphere = wdSphere;
		this.wdSphereWithAdditionalActors = new HashSet<BPMNParticipant>();
		this.lambdaWdSphere = new HashSet<BPMNParticipant>();
		this.wdReaderBrts = wdReaderBrts;	
		this.weightingOfOrigin = Math.pow(2, -origin.getLabels().size());
		this.score = 0;
	}

	public BPMNDataObject getDataObject() {
		return dataObject;
	}

	public void setDataObject(BPMNDataObject dataObject) {
		this.dataObject = dataObject;
	}

	public BPMNTask getOrigin() {
		return origin;
	}

	public void setOrigin(BPMNTask origin) {
		this.origin = origin;
	}

	public LinkedList<AdditionalActors> getAdditionalActors() {
		return additionalActors;
	}

	public void setAdditionalActors(LinkedList<AdditionalActors> additionalActors) {
		this.additionalActors = additionalActors;
	}

	public HashSet<BPMNParticipant> getWdSphere() {
		return wdSphere;
	}

	public void setWdSphere(HashSet<BPMNParticipant> wdSphere) {
		this.wdSphere = wdSphere;
	}

	public HashSet<BPMNParticipant> getWdSphereWithAdditionalActors() {
		return wdSphereWithAdditionalActors;
	}

	public void setWdSphereWithAdditionalActors(HashSet<BPMNParticipant> wdSphereWithAdditionalActors) {
		this.wdSphereWithAdditionalActors = wdSphereWithAdditionalActors;
	}

	public HashSet<BPMNParticipant> getLambdaWdSphere() {
		return lambdaWdSphere;
	}

	public void setLambdaWdSphere(HashSet<BPMNParticipant> lambdaWdSphere) {
		this.lambdaWdSphere = lambdaWdSphere;
	}

	public HashSet<BPMNTask> getWdReaderBrts() {
		return wdReaderBrts;
	}

	public void setWdReaderBrts(HashSet<BPMNTask> wdReaderBrts) {
		this.wdReaderBrts = wdReaderBrts;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public double getWeightingOfOrigin() {
		return weightingOfOrigin;
	}

	public void setWeightingOfOrigin(double weightingOfOrigin) {
		this.weightingOfOrigin = weightingOfOrigin;
	}
	
	

}
