package Mapping;

import java.util.ArrayList;

public class DecisionEvaluation {
	
	private String decisionExpression;
	private ArrayList<DecisionComponent>decisionComponents;
	private String decisionLogic;
	
	public DecisionEvaluation(String decisionExpression, ArrayList<DecisionComponent>decisionComponents, String decisionLogic) {
		this.decisionExpression=decisionExpression;	
		this.decisionLogic=decisionLogic;
	}
	
	public String getDecisionExpression() {
		return this.decisionExpression;
	}

}
