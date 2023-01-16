package mapping;

public class DecisionComponent {
	
	private static int id = 0;
	private String term;
	private String operator; 
	private String result; 
	
	public DecisionComponent(String term, String operator, String result) {
		id++;
		this.term=term;
		this.operator=operator;
		this.result=result;
	}
	
	
	
	

}
