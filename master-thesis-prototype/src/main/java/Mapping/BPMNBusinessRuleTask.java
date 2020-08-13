package Mapping;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

public class BPMNBusinessRuleTask extends BPMNTask {
	
	private HashMap<BPMNDataObject, ArrayList<BPMNTask>> lastWriters;
	private DecisionEvaluation decisionEvaluation;
	private double falseRate;

	public BPMNBusinessRuleTask(String id, String name) {
		super(id, name);
		// TODO Auto-generated constructor stub
		this.lastWriters = new HashMap<BPMNDataObject, ArrayList<BPMNTask>>();
		//false rate is only for testing purposes 
		//it is a pseudo random double between min and max (e.g. 0.01 and 0.1)
		this.setRandomFalseRateBetween(0.01, 0.1);	
	}

	public HashMap<BPMNDataObject,ArrayList<BPMNTask>> getLastWriterList() {
		return this.lastWriters;
	}
	
	public void setLastWriterList(BPMNDataObject dataObject, ArrayList<BPMNTask> lastWriterTasks) {
		this.lastWriters.putIfAbsent(dataObject, lastWriterTasks);
		
	}
	
	public void setDecisionEvaluation(DecisionEvaluation decisionEvaluation) {
		this.decisionEvaluation=decisionEvaluation;
	}
	
	public DecisionEvaluation getDecisionEvaluation() {
		return this.decisionEvaluation;
	}
	
	private void setRandomFalseRateBetween(double min, double max) {
		double zufallszahl = ThreadLocalRandom.current().nextDouble(min, max);
		double roundToTwoDecPlaces = Math.round(zufallszahl * 100.0) / 100.0;
		this.falseRate=roundToTwoDecPlaces;
	}
	
	public double getFalseRate() {
		return this.falseRate;
	}
}
