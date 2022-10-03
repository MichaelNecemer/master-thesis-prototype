package functionality2;

import java.util.Arrays;
import java.util.LinkedList;

import functionality.CommonFunctionality;

public class TestClass {
	
	
	public static void main(String[]args) {
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\diagram_fig2.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\diagram_fig2_addActorB.bpmn";
		String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\diagram_fig2_addActorC.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\diagram_fig2_addActorCnew.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\diagram_fig2_addActorBuC.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\test_clustering.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\randomProcessModel262_annotated1_annotated263sWmR_Static_voters4.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\brtsIn2branches1.bpmn";
		
		LinkedList<Double> costForUpgradingSpheres = new LinkedList<>(Arrays.asList(1.0, 1.0, 1.0));

		try {
			API2 api2 = new API2(pathToFile, costForUpgradingSpheres);

			
			LinkedList<PModelWithAdditionalActors>pInstExhaustive = api2.exhaustiveSearch();
			LinkedList<PModelWithAdditionalActors> cheapestExhaustive = CommonFunctionality2.getCheapestPModelsWithAdditionalActors(pInstExhaustive);
			System.out.println("Cheapest exhaustive: "+cheapestExhaustive.size());
			
			LinkedList<PModelWithAdditionalActors>pInstHeuristic = api2.newMeasureHeuristic(0);
			for(PModelWithAdditionalActors pModel: pInstHeuristic) {
				pModel.printMeasure();
			}
			// garbage collector will remove
			//pInst = null;
			
			String result = CommonFunctionality2.compareResultsOfAlgorithmsForDifferentAPIs(pInstExhaustive, pInstHeuristic, 0);
			System.out.println("Result: "+result);
			
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	

}
