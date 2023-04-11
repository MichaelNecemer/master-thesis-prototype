package functionality;

import java.util.Arrays;
import java.util.LinkedList;

public class TestClass {
	
	
	public static void main(String[]args) {
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\diagram_fig2.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\diagram_fig2_addActorB.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\diagram_fig2_addActorC.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\diagram_1_without_annotation.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\randomProcessModel13_annotated1_annotated22lWlR_Weak-Dynamic_verifiers2.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\randomProcessModel208_annotated1.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\randomProcessModel6_annotated1.bpmn";
		String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\diagram_fig2_addActorCnew.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\diagram_fig2_addActorBuC.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\test_clustering.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\randomProcessModel10_annotated1_annotated15lWmR_Strong-Dynamic_addActors3_mand_constrained.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\brtsIn2branches1.bpmn";
		
		LinkedList<Double> costForUpgradingSpheres = new LinkedList<>(Arrays.asList(1.0, 1.0, 1.0));

		try {
			API api2 = new API(pathToFile, costForUpgradingSpheres, false, true);			
		
			
			//LinkedList<PModelWithAdditionalActors> pInstNaive = api2.naiveSearch(false, 1);
			//LinkedList<PModelWithAdditionalActors> pInstNaive = api2.naiveSearch(true, 1);

			/*
			for(PModelWithAdditionalActors pModel: pInstNaive) {
				pModel.printMeasure();
			}*/
			
			
			LinkedList<PModelWithAdditionalActors>pInstExhaustive = api2.exhaustiveSearch();
			LinkedList<PModelWithAdditionalActors> cheapestExhaustive = CommonFunctionality.getCheapestPModelsWithAdditionalActors(pInstExhaustive);
			System.out.println("Cheapest exhaustive: "+cheapestExhaustive.size());
			
			
			for(PModelWithAdditionalActors pModel: cheapestExhaustive) {
			pModel.printMeasure();
			}
			
						
			int bound = 1;
			LinkedList<PModelWithAdditionalActors>pInstHeuristic = api2.heuristicSearch(bound);
			for(PModelWithAdditionalActors pModel: pInstHeuristic) {
				pModel.printMeasure();
			}
			
	
			
			// garbage collector will remove
			//pInst = null;
			//String result = CommonFunctionality.compareCostOfCheapestSolutionsOfAlgorithms(pInstHeuristic, pInstExhaustive);
			System.out.println("Cheapest heuristic: "+pInstHeuristic.size());
			//System.out.println("Result: "+result);		
			System.out.println("__________________");
			//String naiveIsCheapest = CommonFunctionality.compareResultsOfAlgorithmsForDifferentAPIs(pInstNaive, pInstExhaustive, bound);
			//System.out.println("naiveIsCheapest: "+naiveIsCheapest);
			//System.out.println(pInstNaive.get(0).getSumMeasure());
		//	System.out.println("costOfCheapestSolutionIsSame: "+CommonFunctionality.compareCostOfCheapestSolutionsOfAlgorithms(pInstNaive, pInstExhaustive));

			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	

}
