package processModelGeneratorAndAnnotater;

public class ProcessGeneratorMain {
	
	public static void main(String[]args) {
	String pathWhereToCreateModels = "C:\\Users\\Micha\\OneDrive\\Desktop\\PGen2";
	int amountParticipants = 4;
	int amountTasksToBeInserted = 10;
	int amountXorsToBeInserted = 4;
	int amountParallelsToBeInserted = 0;
	int probabilityForJoinGtw = 50;
	int nestingDepthFactor = 50;
	int amountModelsToCreate = 5;
	int probTask = 33;
	int probXorGtw = 33;
	int probParallelGtw = 33;
	boolean testIfElementsInBranches = false;
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
