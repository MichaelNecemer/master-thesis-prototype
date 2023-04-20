package processModelGeneratorAndAnnotater;

import java.io.File;
import java.util.concurrent.ThreadLocalRandom;

public class ProcessGeneratorMain {
	
	public static void main(String[]args) {
	String pathWhereToCreateModels = "C:\\Users\\Micha\\OneDrive\\Desktop\\PGen2";
	int amountParticipants = 6;
	int amountTasksToBeInserted = 12;
	int amountXorsToBeInserted = 2;
	int amountParallelsToBeInserted = 2;	
	int amountModelsToCreate = 1;
	int probTask = 33;
	int probXorGtw = 33;
	int probParallelGtw = 33;
	boolean firstElementAfterStartIsTask = true;
	boolean allowNestedXors = true;
	
	while(amountModelsToCreate>0) {
	ProcessGenerator pGen;
	try {
		int probabilityForJoinGtw = ThreadLocalRandom.current().nextInt(0,50);
		int nestingDepthFactor = ThreadLocalRandom.current().nextInt(0,50);
		System.out.println("ProbJoinGtw"+probabilityForJoinGtw);
		System.out.println("NestingDepth"+nestingDepthFactor);
		pGen = new ProcessGenerator(pathWhereToCreateModels,amountParticipants,amountTasksToBeInserted,
				amountXorsToBeInserted, amountParallelsToBeInserted, probTask, probXorGtw, probParallelGtw, probabilityForJoinGtw,
				nestingDepthFactor, firstElementAfterStartIsTask, allowNestedXors);
		try {
			File f = pGen.call();
			if(f!=null) {
			amountModelsToCreate--;
			} else {
				ProcessGenerator.decreaseProcessGeneratorId();
			}
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
