package functionality;

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

import mapping.BPMNBusinessRuleTask;

public class ResultsToCSVWriter {

	private CSVWriter writer;
	private List<String[]> rows;

	public ResultsToCSVWriter(File csvFile) {
		try {
			this.writer = new CSVWriter(new FileWriter(csvFile, true), ';', CSVWriter.NO_QUOTE_CHARACTER,
					CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);

			rows = new ArrayList<>();
			String[] header = new String[] { "fileName", "pathToFile", "logging", "exceptionExhaustive",
					"exceptionBaseHeuristic", "exceptionIncrementalHeuristic", "exceptionAdvancedHeuristic",
					"totalAmountSolutionsExhaustive", "totalAmountSolutionsBaseHeuristic",
					"totalAmountSolutionsIncrementalHeuristic", "totalAmountSolutionsAdvancedHeuristic",
					"amountCheapestSolutionsExhaustive", "amountCheapestSolutionsBaseHeuristic",
					"amountSolutionsIncrementalHeuristic", "amountCheapestSolutionsAdvancedHeuristic",
					"costCheapestSolutionExhaustive", "costCheapestSolutionBaseHeuristic",
					"costCheapestSolutionIncrementalHeuristic", "costCheapestSolutionAdvancedHeuristic",
					"exhaustiveCombsGenInSec", "exhaustiveCalcMeasureInSec", "exhaustiveExecutionTimeInSec",
					"baseHeuristicCombsGenInSec", "baseHeuristicCalcMeasureInSec", "baseHeuristicExecutionTimeInSec",
					"incrementalHeuristicCombsGenInSec", "incrementalHeuristicCalcMeasureInSec",
					"incrementalHeuristicExecutionTimeInSec", "advancedHeuristicCombsGenInSec",
					"advancedHeuristicCalcMeasureInSec", "advancedHeuristicExecutionTimeInSec", "amountReaders",
					"amountWriters", "amountDataObjects", "averageAmountDataObjectsPerDecision",
					"amountReadersPerDataObject", "amountWritersPerDataObject", "amountExclusiveGateways",
					"amountParallelGateways", "amountTasks", "amountElements", "amountSumAddActors",
					"averageAmountAddActors", "privateSphereSize", "averageSphereSum", "pathsThroughProcess",
					"amountMandConst", "amountExclConst" };
			this.rows.add(header);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void writeResultsOfAlgorithmsToCSVFile(API api,
			HashMap<Enums.AlgorithmToPerform, Double> cheapestSolutionCostMap,
			HashMap<Enums.AlgorithmToPerform, Integer> totalAmountSolutionsMap,
			HashMap<Enums.AlgorithmToPerform, Integer> cheapestSolutionsMap,
			HashMap<Enums.AlgorithmToPerform, String> loggingMap,
			HashMap<Enums.AlgorithmToPerform, Exception> exceptionPerAlgorithm) {
		// call this method after algorithms ran
		// compare the execution of algorithms
		// write the metadata to a csv file

		String pathToFile = api.getProcessModelFile().getAbsolutePath();
		String fileName = api.getProcessModelFile().getName();
		String logging = "null";

		String totalAmountSolutionsExhaustiveSearch = "";
		String amountCheapestSolutionsExhaustive = "null";
		String costCheapestSolutionExhaustive = "null";

		String totalAmountSolutionsBaseHeuristicSearch = "";
		String amountCheapestSolutionsBaseHeuristicSearch = "null";
		String costCheapestSolutionBaseHeuristicSearch = "null";

		String totalAmountSolutionsIncrementalHeuristicSearch = "";
		String amountCheapestSolutionsIncrementalHeuristicSearch = "null";
		String costCheapestSolutionIncrementalHeuristicSearch = "null";

		String totalAmountSolutionsAdvancedHeuristicSearch = "";
		String costCheapestSolutionAdvancedHeuristicSearch = "null";
		String amountCheapestSolutionsAdvancedHeuristicSearch = "null";

		LinkedList<String> timeListExhaustive = api.getExecutionTimeMap().get(Enums.AlgorithmToPerform.EXHAUSTIVE);
		String exhaustiveSearchGenCombsTime = "null";
		String exhaustiveSearchCalcMeasureTime = "null";
		String exhaustiveSearchExecutionTime = "null";
		if (timeListExhaustive != null && !timeListExhaustive.isEmpty()) {
			exhaustiveSearchGenCombsTime = timeListExhaustive.get(0);
			exhaustiveSearchCalcMeasureTime = timeListExhaustive.get(1);
			exhaustiveSearchExecutionTime = timeListExhaustive.get(2);
		}

		LinkedList<String> timeListBaseHeuristic = api.getExecutionTimeMap()
				.get(Enums.AlgorithmToPerform.BASEHEURISTIC);
		String baseHeuristicSearchGenCombsTime = "null";
		String baseHeuristicSearchCalcMeasureTime = "null";
		String baseHeuristicSearchExecutionTime = "null";
		if (timeListBaseHeuristic != null && !timeListBaseHeuristic.isEmpty()) {
			baseHeuristicSearchGenCombsTime = timeListBaseHeuristic.get(0);
			baseHeuristicSearchCalcMeasureTime = timeListBaseHeuristic.get(1);
			baseHeuristicSearchExecutionTime = timeListBaseHeuristic.get(2);
		}

		LinkedList<String> timeListIncrementalHeuristic = api.getExecutionTimeMap()
				.get(Enums.AlgorithmToPerform.INCREMENTALHEURISTIC);
		String incrementalHeuristicSearchGenCombsTime = "null";
		String incrementalHeuristicSearchCalcMeasureTime = "null";
		String incrementalHeuristicSearchExecutionTime = "null";
		if (timeListIncrementalHeuristic != null && !timeListIncrementalHeuristic.isEmpty()) {
			incrementalHeuristicSearchGenCombsTime = timeListIncrementalHeuristic.get(0);
			incrementalHeuristicSearchCalcMeasureTime = timeListIncrementalHeuristic.get(1);
			incrementalHeuristicSearchExecutionTime = timeListIncrementalHeuristic.get(2);
		}

		LinkedList<String> timeListAdvancedHeuristic = api.getExecutionTimeMap()
				.get(Enums.AlgorithmToPerform.ADVANCEDHEURISTIC);
		String advancedHeuristicSearchGenCombsTime = "null";
		String advancedHeuristicSearchCalcMeasureTime = "null";
		String advancedHeuristicSearchExecutionTime = "null";
		if (timeListAdvancedHeuristic != null && !timeListAdvancedHeuristic.isEmpty()) {
			advancedHeuristicSearchGenCombsTime = timeListAdvancedHeuristic.get(0);
			advancedHeuristicSearchCalcMeasureTime = timeListAdvancedHeuristic.get(1);
			advancedHeuristicSearchExecutionTime = timeListAdvancedHeuristic.get(2);
		}

		String amountMandConst = api.getMandatoryParticipantConstraints().size() + "";
		String amountExclConst = api.getExcludeParticipantConstraints().size() + "";

		if (cheapestSolutionCostMap != null) {
			if (cheapestSolutionCostMap.containsKey(Enums.AlgorithmToPerform.EXHAUSTIVE)) {
				costCheapestSolutionExhaustive = cheapestSolutionCostMap.get(Enums.AlgorithmToPerform.EXHAUSTIVE) + "";
			}
			if (cheapestSolutionCostMap.containsKey(Enums.AlgorithmToPerform.BASEHEURISTIC)) {
				costCheapestSolutionBaseHeuristicSearch = cheapestSolutionCostMap
						.get(Enums.AlgorithmToPerform.BASEHEURISTIC) + "";
			}
			if (cheapestSolutionCostMap.containsKey(Enums.AlgorithmToPerform.INCREMENTALHEURISTIC)) {
				costCheapestSolutionIncrementalHeuristicSearch = cheapestSolutionCostMap
						.get(Enums.AlgorithmToPerform.INCREMENTALHEURISTIC) + "";
			}
			if (cheapestSolutionCostMap.containsKey(Enums.AlgorithmToPerform.ADVANCEDHEURISTIC)) {
				costCheapestSolutionAdvancedHeuristicSearch = cheapestSolutionCostMap
						.get(Enums.AlgorithmToPerform.ADVANCEDHEURISTIC) + "";
			}
		}

		if (totalAmountSolutionsMap.get(Enums.AlgorithmToPerform.EXHAUSTIVE) != null) {
			totalAmountSolutionsExhaustiveSearch = totalAmountSolutionsMap.get(Enums.AlgorithmToPerform.EXHAUSTIVE)
					+ "";
			amountCheapestSolutionsExhaustive = cheapestSolutionsMap.get(Enums.AlgorithmToPerform.EXHAUSTIVE) + "";
			logging = loggingMap.get(Enums.AlgorithmToPerform.EXHAUSTIVE);
		}

		if (totalAmountSolutionsMap.get(Enums.AlgorithmToPerform.BASEHEURISTIC) != null) {
			totalAmountSolutionsBaseHeuristicSearch = totalAmountSolutionsMap
					.get(Enums.AlgorithmToPerform.BASEHEURISTIC) + "";
			amountCheapestSolutionsBaseHeuristicSearch = cheapestSolutionsMap
					.get(Enums.AlgorithmToPerform.BASEHEURISTIC) + "";
			logging = loggingMap.get(Enums.AlgorithmToPerform.BASEHEURISTIC);
		}

		if (totalAmountSolutionsMap.get(Enums.AlgorithmToPerform.INCREMENTALHEURISTIC) != null) {
			totalAmountSolutionsIncrementalHeuristicSearch = totalAmountSolutionsMap
					.get(Enums.AlgorithmToPerform.INCREMENTALHEURISTIC) + "";
			amountCheapestSolutionsIncrementalHeuristicSearch = cheapestSolutionsMap
					.get(Enums.AlgorithmToPerform.INCREMENTALHEURISTIC) + "";
			logging = loggingMap.get(Enums.AlgorithmToPerform.INCREMENTALHEURISTIC);
		}

		if (totalAmountSolutionsMap.get(Enums.AlgorithmToPerform.ADVANCEDHEURISTIC) != null) {
			totalAmountSolutionsAdvancedHeuristicSearch = totalAmountSolutionsMap
					.get(Enums.AlgorithmToPerform.ADVANCEDHEURISTIC) + "";
			amountCheapestSolutionsAdvancedHeuristicSearch = cheapestSolutionsMap
					.get(Enums.AlgorithmToPerform.ADVANCEDHEURISTIC) + "";
			logging = loggingMap.get(Enums.AlgorithmToPerform.ADVANCEDHEURISTIC);
		}

		String readersPerDataObject = "null";
		String writersPerDataObject = "null";
		String allPathsThroughProcess = "null";
		String amountExclusiveGtwSplits = "null";
		String amountParallelGtwSplitsBeforePreprocessing = "null";
		String amountTasks = "null";
		String amountElements = "null";
		String sumAmountAddActorsOfModel = "null";
		String averageAmountAddActorsOfModel = "null";
		String privateSphereSize = "null";
		String sphereSumOfModel = "null";
		String amountReaders = "null";
		String amountWriters = "null";
		String amountDataObjects = "null";
		String averageAmountDataObjectsPerDecision = "null";

		if (api != null) {
			amountDataObjects = CommonFunctionality.getAmountDataObjects(api.getModelInstance()) + "";

			double sumDataObjects = 0;
			for (BPMNBusinessRuleTask brt : api.getBusinessRuleTasks()) {
				sumDataObjects += brt.getDataObjects().size();
			}
			averageAmountDataObjectsPerDecision = sumDataObjects / api.getBusinessRuleTasks().size() + "";

			allPathsThroughProcess = api.getAllPathsThroughProcess().size() + "";
			amountExclusiveGtwSplits = CommonFunctionality.getAmountExclusiveGtwSplits(api.getModelInstance()) + "";
			amountParallelGtwSplitsBeforePreprocessing = api.getAmountParallelsBeforePreprocessing() + "";
			amountTasks = CommonFunctionality.getAmountTasks(api.getModelInstance()) + "";
			amountElements = CommonFunctionality.getAmountElements(api.getModelInstance()) + "";
			sumAmountAddActorsOfModel = CommonFunctionality.getSumAmountAddActorsOfModel(api.getModelInstance()) + "";
			averageAmountAddActorsOfModel = CommonFunctionality
					.getAverageAmountAdditionalActorsOfModel(api.getModelInstance()) + "";
			privateSphereSize = CommonFunctionality.getPrivateSphere(api.getModelInstance(), false) + "";
			sphereSumOfModel = CommonFunctionality.getSphereSumOfModel(api.getModelInstance()) + "";
			double readers = 0;
			for (Entry<DataObjectReference, LinkedList<FlowNode>> entr : CommonFunctionality
					.getReadersForDataObjects(api.getModelInstance()).entrySet()) {
				readers += entr.getValue().size();
			}

			amountReaders = readers + "";
			readersPerDataObject = (readers / Double.parseDouble(amountDataObjects)) + "";

			double writers = 0;
			for (Entry<DataObjectReference, LinkedList<FlowNode>> entr : CommonFunctionality
					.getWritersForDataObjects(api.getModelInstance()).entrySet()) {
				writers += entr.getValue().size();
			}
			amountWriters = writers + "";
			writersPerDataObject = (writers / Double.parseDouble(amountDataObjects)) + "";

		}

		String exceptionNameExhaustive = "null";
		if (exceptionPerAlgorithm.get(Enums.AlgorithmToPerform.EXHAUSTIVE) != null) {
			exceptionNameExhaustive = exceptionPerAlgorithm.get(Enums.AlgorithmToPerform.EXHAUSTIVE).getClass()
					.getCanonicalName();
		}

		String exceptionNameBaseHeuristic = "null";
		if (exceptionPerAlgorithm.get(Enums.AlgorithmToPerform.BASEHEURISTIC) != null) {
			exceptionNameBaseHeuristic = exceptionPerAlgorithm.get(Enums.AlgorithmToPerform.BASEHEURISTIC).getClass()
					.getCanonicalName();
		}

		String exceptionNameIncrementalHeuristic = "null";
		if (exceptionPerAlgorithm.get(Enums.AlgorithmToPerform.INCREMENTALHEURISTIC) != null) {
			exceptionNameIncrementalHeuristic = exceptionPerAlgorithm.get(Enums.AlgorithmToPerform.INCREMENTALHEURISTIC)
					.getClass().getCanonicalName();
		}

		String exceptionNameAdvancedHeuristic = "null";
		if (exceptionPerAlgorithm.get(Enums.AlgorithmToPerform.ADVANCEDHEURISTIC) != null) {
			exceptionNameAdvancedHeuristic = exceptionPerAlgorithm.get(Enums.AlgorithmToPerform.ADVANCEDHEURISTIC)
					.getClass().getCanonicalName();
		}

		String[] row = new String[] { fileName, pathToFile, logging, exceptionNameExhaustive,
				exceptionNameBaseHeuristic, exceptionNameIncrementalHeuristic, exceptionNameAdvancedHeuristic,
				totalAmountSolutionsExhaustiveSearch, totalAmountSolutionsBaseHeuristicSearch,
				totalAmountSolutionsIncrementalHeuristicSearch, totalAmountSolutionsAdvancedHeuristicSearch,
				amountCheapestSolutionsExhaustive, amountCheapestSolutionsBaseHeuristicSearch,
				amountCheapestSolutionsIncrementalHeuristicSearch, amountCheapestSolutionsAdvancedHeuristicSearch,
				costCheapestSolutionExhaustive, costCheapestSolutionBaseHeuristicSearch,
				costCheapestSolutionIncrementalHeuristicSearch, costCheapestSolutionAdvancedHeuristicSearch,
				exhaustiveSearchGenCombsTime, exhaustiveSearchCalcMeasureTime, exhaustiveSearchExecutionTime,
				baseHeuristicSearchGenCombsTime, baseHeuristicSearchCalcMeasureTime, baseHeuristicSearchExecutionTime,
				incrementalHeuristicSearchGenCombsTime, incrementalHeuristicSearchCalcMeasureTime,
				incrementalHeuristicSearchExecutionTime, advancedHeuristicSearchGenCombsTime,
				advancedHeuristicSearchCalcMeasureTime, advancedHeuristicSearchExecutionTime, amountReaders,
				amountWriters, amountDataObjects, averageAmountDataObjectsPerDecision, readersPerDataObject,
				writersPerDataObject, amountExclusiveGtwSplits, amountParallelGtwSplitsBeforePreprocessing, amountTasks,
				amountElements, sumAmountAddActorsOfModel, averageAmountAddActorsOfModel, privateSphereSize,
				sphereSumOfModel, allPathsThroughProcess, amountMandConst, amountExclConst };

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
