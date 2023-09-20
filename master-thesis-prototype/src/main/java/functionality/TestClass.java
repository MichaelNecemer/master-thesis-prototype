package functionality;

import java.util.Arrays;
import java.util.LinkedList;

public class TestClass {

	public static void main(String[] args) {
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\diagram_fig2_addActorBuC.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\randomProcessModel82_annotated1_mWsR_Strong-Dynamic_addActors1.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\assignment_gen.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\adv.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\adv2.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\adv4.bpmn";
		String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\adv5.bpmn";



		LinkedList<Double> costForUpgradingSpheres = new LinkedList<>(Arrays.asList(1.0, 1.0, 1.0));

		try {
			API api2 = new API(pathToFile, costForUpgradingSpheres, true, true);

			LinkedList<PModelWithAdditionalActors> pInstExhaustive = api2.exhaustiveSearch(0);
			LinkedList<PModelWithAdditionalActors> cheapestExhaustive = CommonFunctionality
					.getCheapestPModelsWithAdditionalActors(pInstExhaustive);
			System.out.println("Cheapest exhaustive: " + cheapestExhaustive.size());

			for (PModelWithAdditionalActors pModel : cheapestExhaustive) {
				pModel.printMeasure();
			}
			
			
			/*LinkedList<PModelWithAdditionalActors> pInstBaseHeuristic = api2.baseHeuristicSearch(1);

			for (PModelWithAdditionalActors pModel : pInstBaseHeuristic) {
				pModel.printMeasure();
			}
			/*
			
			LinkedList<PModelWithAdditionalActors> pInstIncrementalHeuristic = api2.incrementalHeuristicSearch(1);

			for (PModelWithAdditionalActors pModel : pInstIncrementalHeuristic) {
				pModel.printMeasure();
			}
			*/
			
			LinkedList<PModelWithAdditionalActors> pInstAdvancedHeuristic = api2.advancedHeuristicSearch(1);

			for (PModelWithAdditionalActors pModel : pInstAdvancedHeuristic) {
				pModel.printMeasure();
			}

			/*
			System.out.println("CombsGenTimeExhaustive: "
					+ api2.getExecutionTimeMap().get(Enums.AlgorithmToPerform.EXHAUSTIVE).get(0));
			System.out.println("CombsGenTimeBaseHeuristic: "
					+ api2.getExecutionTimeMap().get(Enums.AlgorithmToPerform.BASEHEURISTIC).get(0));
			System.out.println("CombsGenTimeIncrementalHeuristic: "
					+ api2.getExecutionTimeMap().get(Enums.AlgorithmToPerform.INCREMENTALHEURISTIC).get(0));
			System.out.println("CombsGenTimeAdvancedHeuristic: "
					+ api2.getExecutionTimeMap().get(Enums.AlgorithmToPerform.ADVANCEDHEURISTIC).get(0));
			*/
			
			//pInst = null;
			//String result = CommonFunctionality.compareCostOfCheapestSolutionsOfAlgorithms(pInstHeuristic, pInstExhaustive);
			//System.out.println("Cheapest heuristic: "+pInstHeuristic.size());
			//System.out.println("Result: "+result);		
			System.out.println("__________________");
			//System.out.println("naiveIsCheapest: "+naiveIsCheapest);
			//System.out.println(pInstNaive.get(0).getSumMeasure());
			System.out.println("costOfCheapestSolutionIsSame: " + CommonFunctionality
					.compareCostOfCheapestSolutionsOfAlgorithms(pInstAdvancedHeuristic, cheapestExhaustive));
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
