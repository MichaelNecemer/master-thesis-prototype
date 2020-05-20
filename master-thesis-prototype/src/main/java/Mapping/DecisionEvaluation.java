package Mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.javassist.compiler.ast.Pair;

public class DecisionEvaluation {
	
	private String decisionExpressionPostfix;
	private Map<Character, Object> decisionLegend;
	
	public DecisionEvaluation() {
		decisionLegend = new HashMap<Character, Object>();
	}
	

	public void setDecisionExpressionPostfix(String decisionExpressionPostfix) {
		this.decisionExpressionPostfix=decisionExpressionPostfix;
	}
	public String getDecisionExpressionPostfix() {
		return this.decisionExpressionPostfix;
	}


	public Map<Character, Object> getDecisionLegend() {
		return decisionLegend;
	}


	public void setDecisionLegend(Map<Character, Object> decisionLegend) {
		this.decisionLegend = decisionLegend;
	}
	
	
}
