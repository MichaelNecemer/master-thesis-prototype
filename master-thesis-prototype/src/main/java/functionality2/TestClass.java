package functionality2;

import java.util.Arrays;
import java.util.LinkedList;

public class TestClass {
	
	
	public static void main(String[]args) {
		String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\diagram_fig2.bpmn";
		LinkedList<Double> costForUpgradingSpheres = new LinkedList<>(Arrays.asList(10.0, 5.0, 3.0, 2.0));

		try {
			API2 api2 = new API2(pathToFile, costForUpgradingSpheres);
			api2.newMeasureAlgorithm();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	

}
