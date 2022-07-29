package functionality2;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;

import Mapping.BPMNDataObject;
import Mapping.BPMNParticipant;
import Mapping.BPMNTask;

public class SD_SphereEntry {

	private BPMNDataObject dataObject;
	private BPMNTask origin;
	private BPMNTask currBrt;
	private LinkedList<AdditionalActors> additionalActors;
	private HashSet<BPMNParticipant> sdSphereWithoutAdditionalActors;
	private HashSet<BPMNParticipant> sdSphereWithAdditionalActors;
	private HashSet<BPMNParticipant> lambdaSdSphere;
	private HashSet<BPMNTask> sdReaderBrts;
	private double amountPathsWhereOriginWritesForCurrBrt;
	private double weightingOfOriginForCurrBrt;
	private double weightingOfOrigin;
	private double score;
	private boolean contributingToGammaMin;

	public SD_SphereEntry(BPMNDataObject dataObject, BPMNTask origin, BPMNTask currBrt, HashSet<BPMNTask> sdReaderBrts,
			HashSet<BPMNParticipant> sdSphereWithAdditionalActors) {
		this.dataObject = dataObject;
		this.origin = origin;
		this.currBrt = currBrt;
		this.additionalActors = new LinkedList<AdditionalActors>();
		this.sdSphereWithAdditionalActors = sdSphereWithAdditionalActors;
		this.sdSphereWithoutAdditionalActors = new HashSet<BPMNParticipant>();
		this.lambdaSdSphere = new HashSet<BPMNParticipant>();
		this.sdReaderBrts = sdReaderBrts;
		this.amountPathsWhereOriginWritesForCurrBrt = 0;
		this.weightingOfOriginForCurrBrt = 0;
		this.weightingOfOrigin = Math.pow(2, -origin.getLabels().size());
		this.score = 0;
		this.contributingToGammaMin = true;
	}

	public SD_SphereEntry(BPMNDataObject dataObject, BPMNTask origin, BPMNTask currBrt) {
		this.dataObject = dataObject;
		this.origin = origin;
		this.currBrt = currBrt;
		this.additionalActors = new LinkedList<AdditionalActors>();
		this.sdReaderBrts = new HashSet<BPMNTask>();
		this.sdSphereWithoutAdditionalActors = new HashSet<BPMNParticipant>();
		this.lambdaSdSphere = new HashSet<BPMNParticipant>();
		this.sdSphereWithAdditionalActors = new HashSet<BPMNParticipant>();
		this.amountPathsWhereOriginWritesForCurrBrt = 0;
		this.weightingOfOriginForCurrBrt = 0;
		this.weightingOfOrigin = Math.pow(2, -origin.getLabels().size());
		this.score = 0;
		this.contributingToGammaMin = true;
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

	public HashSet<BPMNParticipant> getSdSphereWithoutAdditionalActors() {
		return sdSphereWithoutAdditionalActors;
	}

	public void setSdSphereWithoutAdditionalActors(HashSet<BPMNParticipant> sdSphereWithoutAdditionalActors) {
		this.sdSphereWithoutAdditionalActors = sdSphereWithoutAdditionalActors;
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

	public double getWeightingOfOrigin() {
		return weightingOfOrigin;
	}

	public void setWeightingOfOrigin(double weightingOfOrigin) {
		this.weightingOfOrigin = weightingOfOrigin;
	}

	public boolean isContributingToGammaMin() {
		return contributingToGammaMin;
	}

	public void setContributingToGammaMin(boolean contributingToGammaMin) {
		this.contributingToGammaMin = contributingToGammaMin;
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
		SD_SphereEntry other = (SD_SphereEntry) obj;
		return Objects.equals(this.dataObject, other.dataObject) && Objects.equals(this.origin, other.origin)
				&& Objects.equals(this.currBrt, other.currBrt);
	}

	@Override
	public final int hashCode() {
		int result = 17;
		if (this.dataObject != null) {
			result = 31 * result + this.dataObject.hashCode();
		}
		if (this.origin != null) {
			result = 31 * result + this.origin.hashCode();
		}
		if (this.currBrt != null) {
			result = 31 * result + this.currBrt.hashCode();
		}
		return result;
	}

}
