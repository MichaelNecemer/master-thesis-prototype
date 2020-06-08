package Mapping;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//is used for mapping the decisions annotated at BusinessRuleTasks
public class InfixToPostfix {
	
	public static String getLastGroupMatches(Matcher matcher) {
		int groupCount = matcher.groupCount();
		
		for(int i = groupCount; i >=0; i--) {
			if(matcher.group(i)!=null) {
				return matcher.group(i);
			}
		}
		return null;
	}
	
	public static String mapDecision(String expression, DecisionEvaluation decEval) {
		//the decision needs to be mapped first 
		//substitute variable names e.g. D1.someVar -> a
		//substitute booleans and strings with variable names e.g. true -> t
		StringBuffer sb = new StringBuffer();
		
		Pattern pattern = Pattern.compile("(D\\d*\\.[a-zA-Z0-9]*|true|false|\"([a-zA-Z0-9]*)\"|(?<=[\\>|\\<|\\=])[0-9]*)");
		Matcher matcher = pattern.matcher(expression);
		
		
		Map<Character, Object> map = new HashMap<Character, Object>();		
		
		for(char ch = 'a'; ch<='z'; ch++) {			
			while(matcher.find()) {	
			//	System.out.println(getLastGroupMatches(matcher));
				boolean isInList = false;
				for(Entry<Character, Object> entry: map.entrySet()) {
					if(entry.getValue().equals(matcher.group())) {
						isInList=true;
						matcher.appendReplacement(sb, Character.toString(entry.getKey()));
					}
				}
				if(isInList==false) {					
					map.put(ch, getLastGroupMatches(matcher));
					matcher.appendReplacement(sb, Character.toString(ch));
					ch++;	
					}
			}
			
			}
		matcher.appendTail(sb);
		
		
		map.entrySet().forEach(e->{System.out.println(e.getKey()+" "+e.getValue());});
		System.out.println("SB " +sb.toString());
		decEval.setDecisionLegend(map);
		
		return sb.toString();
	}
	
	
	private static int getPrecedence(char ch) {
		switch(ch) {
		case '=': return 0;
		case '&': 
		case '|': return 1;
		case '<':
		case '>': return 2;
		case '+': 
		case '-': return 3;
		case '*': 
		case '/': return 4;
		case '^': return 5;
		default: return -1;			
		}
	}
	
	public static String convertInfixToPostfix(String exp) {
		//call method mapDecision first!
		StringBuilder postFix = new StringBuilder();
		Stack<Character> stack = new Stack<Character>();
		
		for(int i = 0; i < exp.length(); ++i) {
			
			char currentChar = exp.charAt(i);
			
			//if scanned character is an operand (variable), add it to output
			if(Character.isLetterOrDigit(currentChar)) {
				postFix.append(currentChar);
			} else if (currentChar == '(') {
				stack.push(currentChar);
			} else if (currentChar == ')') {
				//pop and output from the stack until '(' 
				while(!stack.isEmpty() && stack.peek() != '(') {
					postFix.append(stack.pop());
				}
				if(!stack.isEmpty() && stack.peek() != '(') {
					return "Invalid Expression";
				} else {
					stack.pop();
				}
				
				
			} else {
				//currentChar is operator
				while(!stack.isEmpty() && getPrecedence(currentChar)<=getPrecedence(stack.peek())) {
					if(stack.peek()=='(') {
						return "Invalid Expression";
					}
					postFix.append(stack.pop());
				}
				stack.push(currentChar);
			}
			
		}
		
		//pop all operators from stack
		while(!stack.isEmpty()) {
			if(stack.peek()=='(') {
				return "Invalid Expression";
			}
			postFix.append(stack.pop());
		}
		return postFix.toString();
	}
	
	
}
