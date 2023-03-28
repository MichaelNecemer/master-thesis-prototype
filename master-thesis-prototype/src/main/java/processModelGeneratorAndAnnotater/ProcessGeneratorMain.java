package processModelGeneratorAndAnnotater;

public class ProcessGeneratorMain {
	
	public static void main(String[]args) {
	String pathWhereToCreateModels = "C:\\Users\\Micha\\OneDrive\\Desktop\\PGen2";
	int amountParticipants = 4;
	int amountTasksToBeInserted = 10;
	int amountXorsToBeInserted = 2;
	int amountParallelsToBeInserted = 1;
	int probabilityForJoinGtw = 20;
	int nestingDepthFactor = 10;
	int amountModelsToCreate = 1;
	int probTask = 33;
	int probXorGtw = 33;
	int probParallelGtw = 33;
	boolean testIfElementsInBranches = true;
	boolean firstElementAfterStartIsTask = true;
	
	while(amountModelsToCreate>0) {
	ProcessGenerator pGen;
	try {
		pGen = new ProcessGenerator(pathWhereToCreateModels,amountParticipants,amountTasksToBeInserted,
				amountXorsToBeInserted, amountParallelsToBeInserted, probTask, probXorGtw, probParallelGtw, probabilityForJoinGtw,
				nestingDepthFactor, testIfElementsInBranches, firstElementAfterStartIsTask);
		try {
			pGen.call();
			amountModelsToCreate--;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	} catch (Exception e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
	
	}
	
	}

}
