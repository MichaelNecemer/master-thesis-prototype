package functionality2;

import java.util.Arrays;
import java.util.LinkedList;

import functionality.CommonFunctionality;

public class TestClass {
	
	
	public static void main(String[]args) {
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\diagram_fig2.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\diagram_fig2_addActorB.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\diagram_fig2_addActorC.bpmn";
		String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\diagram_fig2_addActorBuC.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\test_clustering.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\randomProcessModel262_annotated1_annotated263sWmR_Static_voters4.bpmn";
		//String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\brtsIn2branches1.bpmn";
		
		LinkedList<Double> costForUpgradingSpheres = new LinkedList<>(Arrays.asList(1.0, 1.0, 1.0));

		try {
			API2 api2 = new API2(pathToFile, costForUpgradingSpheres);

			
			//LinkedList<PModelWithAdditionalActors>pInst = api2.exhaustiveSearch();
			LinkedList<PModelWithAdditionalActors>pInst = api2.newMeasureHeuristic(1);
			for(PModelWithAdditionalActors pModel: pInst) {
				pModel.printMeasure();
			}
			// garbage collector will remove
			//pInst = null;
			/*
			
			int boundForAmountPossibleCombsPerBrt = 0;
			int bound = 0;
			LinkedList<PModelWithAdditionalActors>pInst2 = api2.newMeasureHeuristic(boundForAmountPossibleCombsPerBrt,bound, Enums.ClusterCondition.ALLORIGINSTHESAME);
			for(PModelWithAdditionalActors pModel: pInst2) {
				pModel.printMeasure();
			}
			/*
			String exhaustiveName = Enums.AlgorithmToPerform.EXHAUSTIVE.name();
			double exhaustive = api2.getExecutionTimeMap().get(exhaustiveName);
			String heuristicWithBound = Enums.AlgorithmToPerform.HEURISTICWITHBOUND.name()+bound;
			double heuristicWithBoundTime = api2.getExecutionTimeMap().get(heuristicWithBound);
			double delta = heuristicWithBoundTime - exhaustive;
			System.out.println("Heuristic with bound: "+api2.getExecutionTimeMap().get(heuristicWithBound));
			System.out.println("diff: "+delta);
			*/
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	

}
