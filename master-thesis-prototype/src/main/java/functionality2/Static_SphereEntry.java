package functionality2;

import java.util.HashSet;
import java.util.LinkedList;

import Mapping.BPMNDataObject;
import Mapping.BPMNParticipant;
import Mapping.BPMNTask;

public class Static_SphereEntry {
	
	private BPMNDataObject dataObject;
	private LinkedList<AdditionalActors>additionalActors;
	private HashSet<BPMNParticipant>staticSphere;
	private HashSet<BPMNParticipant>staticSphereWithAdditionalActors;
	private HashSet<BPMNParticipant>lambdaStaticSphere;
	private HashSet<BPMNTask>readerBrts;
	private double score;
	
	public Static_SphereEntry(BPMNDataObject dataObject, HashSet<BPMNParticipant>staticSphere, HashSet<BPMNTask>readerBrts) {
		this.dataObject = dataObject;
		this.additionalActors = new LinkedList<AdditionalActors>();
		this.staticSphere = staticSphere;
		this.lambdaStaticSphere = new HashSet<BPMNParticipant>();
		this.readerBrts = readerBrts;
		this.score = 0;
	}

	public BPMNDataObject getDataObject() {
		return dataObject;
	}

	public void setDataObject(BPMNDataObject dataObject) {
		this.dataObject = dataObject;
	}

	public LinkedList<AdditionalActors> getAdditionalActors() {
		return additionalActors;
	}

	public void setAdditionalActors(LinkedList<AdditionalActors> additionalActors) {
		this.additionalActors = additionalActors;
	}

	public HashSet<BPMNParticipant> getStaticSphere() {
		return staticSphere;
	}

	public void setStaticSphere(HashSet<BPMNParticipant> staticSphere) {
		this.staticSphere = staticSphere;
	}

	public HashSet<BPMNParticipant> getStaticSphereWithAdditionalActors() {
		return staticSphereWithAdditionalActors;
	}

	public void setStaticSphereWithAdditionalActors(HashSet<BPMNParticipant> staticSphereWithAdditionalActors) {
		this.staticSphereWithAdditionalActors = staticSphereWithAdditionalActors;
	}

	public HashSet<BPMNParticipant> getLambdaStaticSphere() {
		return lambdaStaticSphere;
	}

	public void setLambdaStaticSphere(HashSet<BPMNParticipant> lambdaStaticSphere) {
		this.lambdaStaticSphere = lambdaStaticSphere;
	}

	public HashSet<BPMNTask> getReaderBrts() {
		return readerBrts;
	}

	public void setReaderBrts(HashSet<BPMNTask> readerBrts) {
		this.readerBrts = readerBrts;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}
	
	

}
