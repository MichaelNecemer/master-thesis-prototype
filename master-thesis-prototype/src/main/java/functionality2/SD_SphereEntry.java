package functionality2;

import java.util.HashSet;
import java.util.LinkedList;

import Mapping.BPMNDataObject;
import Mapping.BPMNParticipant;
import Mapping.BPMNTask;

public class SD_SphereEntry {

	private BPMNDataObject dataObject;
	private BPMNTask origin;
	private BPMNTask currBrt;
	private LinkedList<AdditionalActors>additionalActors;
	private HashSet<BPMNParticipant>sdSphereWithoutAddActorOfCurrBrt;
	private HashSet<BPMNParticipant>sdSphereWithAdditionalActors;
	private HashSet<BPMNParticipant>lambdaSdSphere;
	private HashSet<BPMNTask>sdReaderBrts;
	private double amountPathsWhereOriginWritesForCurrBrt;
	private double weightingOfOriginForCurrBrt;
	private double score;
	
	public SD_SphereEntry(BPMNDataObject dataObject, BPMNTask origin, BPMNTask currBrt, HashSet<BPMNTask>sdReaderBrts, HashSet<BPMNParticipant>sdSphereWithAdditionalActors) {
		this.dataObject = dataObject;
		this.origin = origin;
		this.currBrt = currBrt;
		this.additionalActors = new LinkedList<AdditionalActors>();
		this.sdSphereWithAdditionalActors = sdSphereWithAdditionalActors;
		this.sdSphereWithoutAddActorOfCurrBrt = new HashSet<BPMNParticipant>();
		this.lambdaSdSphere = new HashSet<BPMNParticipant>();
		this.sdReaderBrts = sdReaderBrts;
		this.amountPathsWhereOriginWritesForCurrBrt = 0;
		this.weightingOfOriginForCurrBrt = 0;
		this.score = 0;
	}
	
	public SD_SphereEntry(BPMNDataObject dataObject, BPMNTask origin, BPMNTask currBrt) {
		this.dataObject = dataObject;
		this.origin = origin;
		this.currBrt = currBrt;
		this.additionalActors = new LinkedList<AdditionalActors>();
		this.sdReaderBrts = new HashSet<BPMNTask>();
		this.sdSphereWithoutAddActorOfCurrBrt = new HashSet<BPMNParticipant>();
		this.lambdaSdSphere = new HashSet<BPMNParticipant>();
		this.sdSphereWithAdditionalActors = new HashSet<BPMNParticipant>();
		this.amountPathsWhereOriginWritesForCurrBrt = 0;
		this.weightingOfOriginForCurrBrt = 0;
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

	public BPMNTask getCurrBrt() {
		return currBrt;
	}

	public void setCurrBrt(BPMNTask currBrt) {
		this.currBrt = currBrt;
	}

	public LinkedList<AdditionalActors> getAdditionalActors() {
		return additionalActors;
	}

	public void setAdditionalActors(LinkedList<AdditionalActors> additionalActors) {
		this.additionalActors = additionalActors;
	}

	public HashSet<BPMNParticipant> getSdSphereWithoutAddActorOfCurrBrt() {
		return sdSphereWithoutAddActorOfCurrBrt;
	}

	public void setSdSphereWithoutAddActorOfCurrBrt(HashSet<BPMNParticipant> sdSphereWithoutAddActorOfCurrBrt) {
		this.sdSphereWithoutAddActorOfCurrBrt = sdSphereWithoutAddActorOfCurrBrt;
	}

	public HashSet<BPMNParticipant> getSdSphereWithAdditionalActors() {
		return sdSphereWithAdditionalActors;
	}

	public void setSdSphereWithAdditionalActors(HashSet<BPMNParticipant> sdSphereWithAdditionalActors) {
		this.sdSphereWithAdditionalActors = sdSphereWithAdditionalActors;
	}

	public HashSet<BPMNParticipant> getLambdaSdSphere() {
		return lambdaSdSphere;
	}

	public void setLambdaSdSphere(HashSet<BPMNParticipant> lambdaSdSphere) {
		this.lambdaSdSphere = lambdaSdSphere;
	}

	public HashSet<BPMNTask> getSdReaderBrts() {
		return sdReaderBrts;
	}

	public void setSdReaderBrts(HashSet<BPMNTask> sdReaderBrts) {
		this.sdReaderBrts = sdReaderBrts;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public double getWeightingOfOriginForCurrBrt() {
		return weightingOfOriginForCurrBrt;
	}

	public void setWeightingOfOriginForCurrBrt(double weightingOfOriginForCurrBrt) {
		this.weightingOfOriginForCurrBrt = weightingOfOriginForCurrBrt;
	}

	public double getAmountPathsWhereOriginWritesForCurrBrt() {
		return amountPathsWhereOriginWritesForCurrBrt;
	}

	public void setAmountPathsWhereOriginWritesForCurrBrt(double amountPathsWhereOriginWritesForCurrBrt) {
		this.amountPathsWhereOriginWritesForCurrBrt = amountPathsWhereOriginWritesForCurrBrt;
	}
	
	
	
	

}
