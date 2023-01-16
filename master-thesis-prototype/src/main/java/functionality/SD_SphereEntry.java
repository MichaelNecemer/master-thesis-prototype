package functionality;

import java.util.HashSet;
import java.util.Objects;

import mapping.BPMNDataObject;
import mapping.BPMNParticipant;
import mapping.BPMNTask;

public class SD_SphereEntry {

	private BPMNDataObject dataObject;
	private BPMNTask origin;
	private BPMNTask currBrt;
	private BPMNParticipant additionalActor;
	private HashSet<BPMNParticipant> sdSphereWithoutAdditionalActor;
	private HashSet<BPMNParticipant> sdSphereWithAdditionalActor;
	private HashSet<BPMNParticipant> lambdaSdSphere;
	private HashSet<BPMNTask> sdReaderBrts;
	private double amountPathsWhereOriginWritesForCurrBrt;
	private double weightOfOriginForCurrBrt;
	private double weightOfOrigin;
	private double score;
	private boolean contributingToGammaMin;

	public SD_SphereEntry(BPMNDataObject dataObject, BPMNTask origin, BPMNTask currBrt,
			BPMNParticipant additionalActor) {
		this.dataObject = dataObject;
		this.origin = origin;
		this.currBrt = currBrt;
		this.additionalActor = additionalActor;
		this.sdReaderBrts = new HashSet<BPMNTask>();
		this.sdSphereWithoutAdditionalActor = new HashSet<BPMNParticipant>();
		this.lambdaSdSphere = new HashSet<BPMNParticipant>();
		this.sdSphereWithAdditionalActor = new HashSet<BPMNParticipant>();
		this.amountPathsWhereOriginWritesForCurrBrt = 0;
		this.weightOfOriginForCurrBrt = 0;
		this.weightOfOrigin = Math.pow(2, -origin.getLabels().size());
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

	public BPMNParticipant getAdditionalActor() {
		return additionalActor;
	}

	public void setAdditionalActor(BPMNParticipant additionalActor) {
		this.additionalActor = additionalActor;
	}

	public HashSet<BPMNParticipant> getSdSphereWithoutAdditionalActor() {
		return sdSphereWithoutAdditionalActor;
	}

	public void setSdSphereWithoutAdditionalActor(HashSet<BPMNParticipant> sdSphereWithoutAdditionalActors) {
		this.sdSphereWithoutAdditionalActor = sdSphereWithoutAdditionalActors;
	}

	public HashSet<BPMNParticipant> getSdSphereWithAdditionalActor() {
		return sdSphereWithAdditionalActor;
	}

	public void setSdSphereWithAdditionalActors(HashSet<BPMNParticipant> sdSphereWithAdditionalActors) {
		this.sdSphereWithAdditionalActor = sdSphereWithAdditionalActors;
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

	public double getWeightOfOriginForCurrBrt() {
		return weightOfOriginForCurrBrt;
	}

	public void setWeightOfOriginForCurrBrt(double weightingOfOriginForCurrBrt) {
		this.weightOfOriginForCurrBrt = weightingOfOriginForCurrBrt;
	}

	public double getAmountPathsWhereOriginWritesForCurrBrt() {
		return amountPathsWhereOriginWritesForCurrBrt;
	}

	public void setAmountPathsWhereOriginWritesForCurrBrt(double amountPathsWhereOriginWritesForCurrBrt) {
		this.amountPathsWhereOriginWritesForCurrBrt = amountPathsWhereOriginWritesForCurrBrt;
	}

	public double getWeightOfOrigin() {
		return weightOfOrigin;
	}

	public void setWeightOfOrigin(double weightingOfOrigin) {
		this.weightOfOrigin = weightingOfOrigin;
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
				&& Objects.equals(this.currBrt, other.currBrt)
				&& Objects.equals(this.additionalActor, other.additionalActor);
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
		if (this.additionalActor != null) {
			result = 31 * result + this.additionalActor.hashCode();
		}

		return result;
	}

}
