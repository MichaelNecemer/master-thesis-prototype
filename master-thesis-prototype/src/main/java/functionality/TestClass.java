package functionality;

import java.util.Arrays;
import java.util.LinkedList;

public class TestClass {
	
	
	public static void main(String[]args) {
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\diagram_fig2.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\diagram_fig2_addActorC.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\diagram_1_without_annotation.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\randomProcessModel13_annotated1_annotated22lWlR_Weak-Dynamic_verifiers2.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\randomProcessModel1_annotated1.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\diagram_fig2_addActorBuC.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\randomProcessModel173_annotated1_sWlR_Strong-Dynamic_addActors2.bpmn";
		String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\randomProcessModel82_annotated1_mWsR_Strong-Dynamic_addActors1.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\assignment_gen.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\randomProcessModel164_annotated1_mWlR_Weak-Dynamic_addActors2.bpmn";

		
		LinkedList<Double> costForUpgradingSpheres = new LinkedList<>(Arrays.asList(1.0, 1.0, 1.0));

		try {
			API api2 = new API(pathToFile, costForUpgradingSpheres, true, true);			
		
			
			//LinkedList<PModelWithAdditionalActors> pInstNaive = api2.naiveSearch(1);
			//LinkedList<PModelWithAdditionalActors> pInstNaive = api2.incrementalNaiveSearch(1);
			LinkedList<PModelWithAdditionalActors> pInstNaive = api2.advancedNaive(1);
			
			for(PModelWithAdditionalActors pModel: pInstNaive) {
				pModel.printMeasure();
			}
			
			
			LinkedList<PModelWithAdditionalActors>pInstExhaustive = api2.exhaustiveSearch(0);
			LinkedList<PModelWithAdditionalActors> cheapestExhaustive = CommonFunctionality.getCheapestPModelsWithAdditionalActors(pInstExhaustive);
			System.out.println("Cheapest exhaustive: "+cheapestExhaustive.size());
			
			
			for(PModelWithAdditionalActors pModel: cheapestExhaustive) {
			pModel.printMeasure();
			}
			
			
			
			// garbage collector will remove
			//pInst = null;
			//String result = CommonFunctionality.compareCostOfCheapestSolutionsOfAlgorithms(pInstHeuristic, pInstExhaustive);
			//System.out.println("Cheapest heuristic: "+pInstHeuristic.size());
			//System.out.println("Result: "+result);		
			System.out.println("__________________");
			//System.out.println("naiveIsCheapest: "+naiveIsCheapest);
			//System.out.println(pInstNaive.get(0).getSumMeasure());
			System.out.println("costOfCheapestSolutionIsSame: "+CommonFunctionality.compareCostOfCheapestSolutionsOfAlgorithms(pInstNaive, pInstExhaustive));

			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	

}
