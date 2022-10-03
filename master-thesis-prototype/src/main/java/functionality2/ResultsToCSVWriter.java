package functionality2;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.camunda.bpm.model.bpmn.instance.DataObjectReference;
import org.camunda.bpm.model.bpmn.instance.FlowNode;

import com.opencsv.CSVWriter;

import Mapping.BPMNBusinessRuleTask;

public class ResultsToCSVWriter {

	private CSVWriter writer;
	private List<String[]> rows;

	public ResultsToCSVWriter(File csvFile) {
		try {
			this.writer = new CSVWriter(new FileWriter(csvFile, true), ';', CSVWriter.NO_QUOTE_CHARACTER,
					CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);

			rows = new ArrayList<>();
			String[] header = new String[] { "fileName", "pathToFile", "logging", "exceptionExhaustive",
					"exceptionHeuristic", "exceptionLocalMinWithLimit", "total amount solutions exhaustive", "amount cheapest solutions exhaustive", "solutions heuristic",
					"solution heuristicWithBound", "costCheapestSolutionExhaustive", "costCheapestSolutionHeuristic",
					"costCheapestSolutionHeuristicWithBound", "executionTimeExhaustive in sec",
					"executionTimeHeuristic in sec", "executionTimeHeuristicWithLimit in sec",
					"deltaExecutionTime in sec (heuristic - exhaustive)",
					"deltaExecutionTime in sec (heuristicWithBound - exhaustive)",
					"isCheapestSolutionOfHeuristicEqualToExhaustive",
					"isCheapestSolutionOfHeuristicWithBoundEqualToExhaustive", "amountReaders", "amountWriters",
					"amountDataObjects", "averageAmountDataObjectsPerDecision", "amountReadersPerDataObject",
					"amountWritersPerDataObject", "amountExclusiveGateways", "amountParallelGateways", "amountTasks",
					"amountElements", "amountSumVoters", "averageAmountVoters", "privateSphereSize", "averageSphereSum",
					"clusterSize", "pathsThroughProcess", "deciderOrVerifier" };
			this.rows.add(header);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void writeResultsOfAlgorithmsToCSVFile(API2 api,
			HashMap<Enums.AlgorithmToPerform, Double> cheapestSolutionCostMap,
			HashMap<Enums.AlgorithmToPerform, Integer> totalAmountSolutionsMap,
			HashMap<Enums.AlgorithmToPerform, Integer> cheapestSolutionsMap,
			HashMap<Enums.AlgorithmToPerform, String> loggingMap,
			HashMap<Enums.AlgorithmToPerform, Exception> exceptionPerAlgorithm, String isCheapestSolutionInBruteForce,
			String isCheapestSolutionWithLimitInBruteForce) {
		// call this method after algorithms ran
		// compare the execution of algorithms
		// write the metadata to a csv file

		String pathToFile = api.getProcessModelFile().getAbsolutePath();
		String fileName = api.getProcessModelFile().getName();
		String logging = "null";

		String totalAmountSolutionsExhaustiveSearch = "";
		String amountCheapestSolutionsExhaustive = "null";
		String costCheapestSolutionExhaustive = "null";
		
		String totalAmountSolutionsHeuristicSearch = "";
		String costCheapestSolutionHeuristic = "null";
		String amountCheapestSolutionsHeuristic = "null";
		
		
		String totalAmountSolutionsHeuristicSearchWithBound = "";
		String amountCheapestSolutionsLocalMinWithLimit = "null";
		String costCheapestSolutionLocalMinWithLimit = "null";
		

		String heuristicSearchExecutionTime = "null";
		String exhaustiveSearchExecutionTime = "null";
		String heuristicSearchWithBoundExecutionTime = "null";
		String timeDifferenceHeuristicAndExhaustive = "null";
		String timeDifferenceHeuristicWithBoundAndExhaustive = "null";

		if (totalAmountSolutionsMap.get(Enums.AlgorithmToPerform.EXHAUSTIVE) != null) {
			totalAmountSolutionsExhaustiveSearch = totalAmountSolutionsMap.get(Enums.AlgorithmToPerform.EXHAUSTIVE) + "";
			amountCheapestSolutionsExhaustive = cheapestSolutionsMap.get(Enums.AlgorithmToPerform.EXHAUSTIVE) + "";
			costCheapestSolutionExhaustive = cheapestSolutionCostMap.get(Enums.AlgorithmToPerform.EXHAUSTIVE) + "";
			exhaustiveSearchExecutionTime = api.getExecutionTimeMap().get(Enums.AlgorithmToPerform.EXHAUSTIVE) + "";
			logging = loggingMap.get(Enums.AlgorithmToPerform.EXHAUSTIVE);
		}

		if (totalAmountSolutionsMap.get(Enums.AlgorithmToPerform.HEURISTIC) != null) {
			totalAmountSolutionsHeuristicSearch = totalAmountSolutionsMap.get(Enums.AlgorithmToPerform.HEURISTIC) + "";
			costCheapestSolutionHeuristic = cheapestSolutionCostMap.get(Enums.AlgorithmToPerform.HEURISTIC) + "";
			amountCheapestSolutionsHeuristic = cheapestSolutionsMap.get(Enums.AlgorithmToPerform.HEURISTIC) + "";
			heuristicSearchExecutionTime = api.getExecutionTimeMap().get(Enums.AlgorithmToPerform.HEURISTIC) + "";
			logging = loggingMap.get(Enums.AlgorithmToPerform.HEURISTIC);
		}

		if (totalAmountSolutionsMap.get(Enums.AlgorithmToPerform.HEURISTICWITHBOUND) != null) {
			totalAmountSolutionsHeuristicSearchWithBound = totalAmountSolutionsMap.get(Enums.AlgorithmToPerform.HEURISTICWITHBOUND)
					+ "";
			costCheapestSolutionLocalMinWithLimit = cheapestSolutionCostMap
					.get(Enums.AlgorithmToPerform.HEURISTICWITHBOUND) + "";
			amountCheapestSolutionsLocalMinWithLimit = cheapestSolutionsMap
					.get(Enums.AlgorithmToPerform.HEURISTICWITHBOUND) + "";
			heuristicSearchWithBoundExecutionTime = api.getExecutionTimeMap()
					.get(Enums.AlgorithmToPerform.HEURISTICWITHBOUND) + "";
			logging = loggingMap.get(Enums.AlgorithmToPerform.HEURISTICWITHBOUND);
		}

		if ((!heuristicSearchExecutionTime.contentEquals("null"))
				&& (!exhaustiveSearchExecutionTime.contentEquals("null"))) {
			timeDifferenceHeuristicAndExhaustive = (Double.parseDouble(heuristicSearchExecutionTime)
					- Double.parseDouble(exhaustiveSearchExecutionTime)) + "";
		}

		if ((!heuristicSearchWithBoundExecutionTime.contentEquals("null"))
				&& (!exhaustiveSearchExecutionTime.contentEquals("null"))) {
			timeDifferenceHeuristicWithBoundAndExhaustive = (Double.parseDouble(heuristicSearchWithBoundExecutionTime)
					- Double.parseDouble(exhaustiveSearchExecutionTime)) + "";
		}


		String readersPerDataObject = "null";
		String writersPerDataObject = "null";
		String clusterSize = "null";
		String allPathsThroughProcess = "null";
		String amountExclusiveGtwSplits = "null";
		String amountParallelGtwSplits = "null";
		String amountTasks = "null";
		String amountElements = "null";
		String sumAmountVotersOfModel = "null";
		String averageAmountVotersOfModel = "null";
		String privateSphereSize = "null";
		String sphereSumOfModel = "null";
		String amountReaders = "null";
		String amountWriters = "null";
		String amountDataObjects = "null";
		String averageAmountDataObjectsPerDecision = "null";

		if (api != null) {
			amountDataObjects = CommonFunctionality2.getAmountDataObjects(api.getModelInstance()) + "";

			double sumDataObjects = 0;
			for (BPMNBusinessRuleTask brt : api.getBusinessRuleTasks()) {
				sumDataObjects += brt.getDataObjects().size();
			}
			averageAmountDataObjectsPerDecision = sumDataObjects / api.getBusinessRuleTasks().size() + "";

			allPathsThroughProcess = api.getAllPathsThroughProcess().size() + "";
			amountExclusiveGtwSplits = CommonFunctionality2.getAmountExclusiveGtwSplits(api.getModelInstance()) + "";
			amountParallelGtwSplits = CommonFunctionality2.getAmountParallelGtwSplits(api.getModelInstance()) + "";
			amountTasks = CommonFunctionality2.getAmountTasks(api.getModelInstance()) + "";
			amountElements = CommonFunctionality2.getAmountElements(api.getModelInstance()) + "";
			sumAmountVotersOfModel = CommonFunctionality2.getSumAmountVerifiersOfModel(api.getModelInstance()) + "";
			averageAmountVotersOfModel = CommonFunctionality2.getAverageAmountVerifiersOfModel(api.getModelInstance())
					+ "";
			privateSphereSize = CommonFunctionality2.getPrivateSphere(api.getModelInstance(), false) + "";
			sphereSumOfModel = CommonFunctionality2.getSphereSumOfModel(api.getModelInstance()) + "";
			double readers = 0;
			for (Entry<DataObjectReference, LinkedList<FlowNode>> entr : CommonFunctionality2
					.getReadersForDataObjects(api.getModelInstance()).entrySet()) {
				for (FlowNode f : entr.getValue()) {
					readers++;
				}
			}

			amountReaders = readers + "";
			readersPerDataObject = (readers / sumDataObjects) + "";

			double writers = 0;
			for (Entry<DataObjectReference, LinkedList<FlowNode>> entr : CommonFunctionality2
					.getWritersForDataObjects(api.getModelInstance()).entrySet()) {
				for (FlowNode f : entr.getValue()) {
					writers++;
				}
			}
			amountWriters = writers + "";
			writersPerDataObject = (writers / sumDataObjects) + "";

			clusterSize = api.getClusterSet().size() + "";

		}

		String exceptionNameExhaustive = "null";
		if (exceptionPerAlgorithm.get(Enums.AlgorithmToPerform.EXHAUSTIVE) != null) {
			exceptionNameExhaustive = exceptionPerAlgorithm.get(Enums.AlgorithmToPerform.EXHAUSTIVE).getClass()
					.getCanonicalName();
		}

		String exceptionNameHeuristic = "null";
		if (exceptionPerAlgorithm.get(Enums.AlgorithmToPerform.HEURISTIC) != null) {
			exceptionNameHeuristic = exceptionPerAlgorithm.get(Enums.AlgorithmToPerform.HEURISTIC).getClass()
					.getCanonicalName();
		}

		String exceptionNameHeuristicWithBound = "null";
		if (exceptionPerAlgorithm.get(Enums.AlgorithmToPerform.HEURISTICWITHBOUND) != null) {
			exceptionNameHeuristicWithBound = exceptionPerAlgorithm.get(Enums.AlgorithmToPerform.HEURISTICWITHBOUND)
					.getClass().getCanonicalName();
		}

		String[] row = new String[] { fileName, pathToFile, logging, exceptionNameExhaustive, exceptionNameHeuristic,
				exceptionNameHeuristicWithBound, totalAmountSolutionsExhaustiveSearch, amountCheapestSolutionsExhaustive, amountCheapestSolutionsHeuristic,
				amountCheapestSolutionsLocalMinWithLimit, costCheapestSolutionExhaustive, costCheapestSolutionHeuristic,
				costCheapestSolutionLocalMinWithLimit, exhaustiveSearchExecutionTime, heuristicSearchExecutionTime,
				heuristicSearchWithBoundExecutionTime, timeDifferenceHeuristicAndExhaustive,
				timeDifferenceHeuristicWithBoundAndExhaustive, isCheapestSolutionInBruteForce,
				isCheapestSolutionWithLimitInBruteForce, amountReaders, amountWriters, amountDataObjects,
				averageAmountDataObjectsPerDecision, readersPerDataObject, writersPerDataObject,
				amountExclusiveGtwSplits, amountParallelGtwSplits, amountTasks, amountElements, sumAmountVotersOfModel,
				averageAmountVotersOfModel, privateSphereSize, sphereSumOfModel, clusterSize, allPathsThroughProcess};

		this.rows.add(row);

	}

	public void addEmptyRow() {
		String[] emptyRow = new String[this.rows.get(0).length];
		for (int i = 0; i < this.rows.get(0).length; i++) {
			emptyRow[i] = "";
		}
		this.rows.add(emptyRow);

	}

	public void addNullValueRowForModel(String fileName, String pathToFile, String log) {
		String[] nullValueRow = new String[this.rows.get(0).length];
		nullValueRow[0] = fileName;
		nullValueRow[1] = pathToFile;
		nullValueRow[2] = log;
		for (int i = 3; i < this.rows.get(0).length; i++) {
			nullValueRow[i] = "null";
		}
		this.rows.add(nullValueRow);
	}

	public void writeRowsToCSVAndcloseWriter() {
		try {
			this.writer.writeAll(this.rows);
			this.writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
