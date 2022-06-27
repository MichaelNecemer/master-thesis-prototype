package functionality2;

import java.util.Arrays;
import java.util.LinkedList;

public class TestClass {
	
	
	public static void main(String[]args) {
		String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\diagram_fig2.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\diagram_fig2_addActorB.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\randomProcessModel262_annotated1_annotated263sWmR_Static_voters4.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\brtsIn2branches1.bpmn";
		
		LinkedList<Double> costForUpgradingSpheres = new LinkedList<>(Arrays.asList(1.0, 1.0, 1.0));

		try {
			API2 api2 = new API2(pathToFile, costForUpgradingSpheres);
			api2.newMeasureAlgorithm();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	

}
