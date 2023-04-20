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
					"exceptionNaive", "exceptionIncrementalNaive", "exceptionAdvancedNaive",
					"totalAmountSolutionsExhaustive", "totalAmountSolutionsNaive",
					"totalAmountSolutionsIncrementalNaive", "totalAmountSolutionsAdvancedNaive",
					"amountCheapestSolutionsExhaustive", "amountCheapestSolutionsNaive",
					"amountSolutionsIncrementalNaive", "amountCheapestSolutionsAdvancedNaive",
					"costCheapestSolutionExhaustive", "costCheapestSolutionNaive",
					"costCheapestSolutionIncrementalNaive", "costCheapestSolutionAdvancedNaive",
					"exhaustiveCombsGenInSec", "exhaustiveCalcMeasureInSec", "exhaustiveExecutionTimeInSec",
					"naiveCombsGenInSec", "naiveCalcMeasureInSec", "naiveExecutionTimeInSec",
					"incrementalNaiveCombsGenInSec", "incrementalNaiveCalcMeasureInSec",
					"incrementalNaiveExecutionTimeInSec", "advancedNaiveCombsGenInSec", "advancedNaiveCalcMeasureInSec",
					"advancedNaiveExecutionTimeInSec", "amountReaders", "amountWriters", "amountDataObjects",
					"averageAmountDataObjectsPerDecision", "amountReadersPerDataObject", "amountWritersPerDataObject",
					"amountExclusiveGateways", "amountParallelGateways", "amountTasks", "amountElements",
					"amountSumAddActors", "averageAmountAddActors", "privateSphereSize", "averageSphereSum",
					"pathsThroughProcess", "amountMandConst", "amountExclConst" };
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

		String totalAmountSolutionsNaiveSearch = "";
		String amountCheapestSolutionsNaiveSearch = "null";
		String costCheapestSolutionNaiveSearch = "null";

		String totalAmountSolutionsIncrementalNaiveSearch = "";
		String amountCheapestSolutionsIncrementalNaiveSearch = "null";
		String costCheapestSolutionIncrementalNaiveSearch = "null";

		String totalAmountSolutionsAdvancedNaiveSearch = "";
		String costCheapestSolutionAdvancedNaive = "null";
		String amountCheapestSolutionsAdvancedNaive = "null";

		LinkedList<String> timeListExhaustive = api.getExecutionTimeMap().get(Enums.AlgorithmToPerform.EXHAUSTIVE);
		String exhaustiveSearchGenCombsTime = "null";
		String exhaustiveSearchCalcMeasureTime = "null";
		String exhaustiveSearchExecutionTime = "null";
		if (timeListExhaustive != null && !timeListExhaustive.isEmpty()) {
			exhaustiveSearchGenCombsTime = timeListExhaustive.get(0);
			exhaustiveSearchCalcMeasureTime = timeListExhaustive.get(1);
			exhaustiveSearchExecutionTime = timeListExhaustive.get(2);
		}

		LinkedList<String> timeListNaive = api.getExecutionTimeMap().get(Enums.AlgorithmToPerform.NAIVE);
		String naiveSearchGenCombsTime = "null";
		String naiveSearchCalcMeasureTime = "null";
		String naiveSearchExecutionTime = "null";
		if (timeListNaive != null && !timeListNaive.isEmpty()) {
			naiveSearchGenCombsTime = timeListNaive.get(0);
			naiveSearchCalcMeasureTime = timeListNaive.get(1);
			naiveSearchExecutionTime = timeListNaive.get(2);
		}

		LinkedList<String> timeListIncrementalNaive = api.getExecutionTimeMap()
				.get(Enums.AlgorithmToPerform.INCREMENTALNAIVE);
		String incrementalNaiveSearchGenCombsTime = "null";
		String incrementalNaiveSearchCalcMeasureTime = "null";
		String incrementalNaiveSearchExecutionTime = "null";
		if (timeListIncrementalNaive != null && !timeListIncrementalNaive.isEmpty()) {
			incrementalNaiveSearchGenCombsTime = timeListIncrementalNaive.get(0);
			incrementalNaiveSearchCalcMeasureTime = timeListIncrementalNaive.get(1);
			incrementalNaiveSearchExecutionTime = timeListIncrementalNaive.get(2);
		}

		LinkedList<String> timeListAdvancedNaive = api.getExecutionTimeMap()
				.get(Enums.AlgorithmToPerform.ADVANCEDNAIVE);
		String advancedNaiveSearchGenCombsTime = "null";
		String advancedNaiveSearchCalcMeasureTime = "null";
		String advancedNaiveSearchExecutionTime = "null";
		if (timeListAdvancedNaive != null && !timeListAdvancedNaive.isEmpty()) {
			advancedNaiveSearchGenCombsTime = timeListAdvancedNaive.get(0);
			advancedNaiveSearchCalcMeasureTime = timeListAdvancedNaive.get(1);
			advancedNaiveSearchExecutionTime = timeListAdvancedNaive.get(2);
		}

		String amountMandConst = api.getMandatoryParticipantConstraints().size() + "";
		String amountExclConst = api.getExcludeParticipantConstraints().size() + "";

		if (cheapestSolutionCostMap != null) {
			if (cheapestSolutionCostMap.containsKey(Enums.AlgorithmToPerform.EXHAUSTIVE)) {
				costCheapestSolutionExhaustive = cheapestSolutionCostMap.get(Enums.AlgorithmToPerform.EXHAUSTIVE) + "";
			}
			if (cheapestSolutionCostMap.containsKey(Enums.AlgorithmToPerform.NAIVE)) {
				costCheapestSolutionNaiveSearch = cheapestSolutionCostMap.get(Enums.AlgorithmToPerform.NAIVE) + "";
			}
			if (cheapestSolutionCostMap.containsKey(Enums.AlgorithmToPerform.INCREMENTALNAIVE)) {
				costCheapestSolutionIncrementalNaiveSearch = cheapestSolutionCostMap
						.get(Enums.AlgorithmToPerform.INCREMENTALNAIVE) + "";
			}
			if (cheapestSolutionCostMap.containsKey(Enums.AlgorithmToPerform.ADVANCEDNAIVE)) {
				costCheapestSolutionAdvancedNaive = cheapestSolutionCostMap.get(Enums.AlgorithmToPerform.ADVANCEDNAIVE)
						+ "";
			}
		}

		if (totalAmountSolutionsMap.get(Enums.AlgorithmToPerform.EXHAUSTIVE) != null) {
			totalAmountSolutionsExhaustiveSearch = totalAmountSolutionsMap.get(Enums.AlgorithmToPerform.EXHAUSTIVE)
					+ "";
			amountCheapestSolutionsExhaustive = cheapestSolutionsMap.get(Enums.AlgorithmToPerform.EXHAUSTIVE) + "";
			logging = loggingMap.get(Enums.AlgorithmToPerform.EXHAUSTIVE);
		}

		if (totalAmountSolutionsMap.get(Enums.AlgorithmToPerform.NAIVE) != null) {
			totalAmountSolutionsNaiveSearch = totalAmountSolutionsMap.get(Enums.AlgorithmToPerform.NAIVE) + "";
			amountCheapestSolutionsNaiveSearch = cheapestSolutionsMap.get(Enums.AlgorithmToPerform.NAIVE) + "";
			logging = loggingMap.get(Enums.AlgorithmToPerform.NAIVE);
		}

		if (totalAmountSolutionsMap.get(Enums.AlgorithmToPerform.INCREMENTALNAIVE) != null) {
			totalAmountSolutionsIncrementalNaiveSearch = totalAmountSolutionsMap
					.get(Enums.AlgorithmToPerform.INCREMENTALNAIVE) + "";
			amountCheapestSolutionsIncrementalNaiveSearch = cheapestSolutionsMap
					.get(Enums.AlgorithmToPerform.INCREMENTALNAIVE) + "";
			logging = loggingMap.get(Enums.AlgorithmToPerform.INCREMENTALNAIVE);
		}

		if (totalAmountSolutionsMap.get(Enums.AlgorithmToPerform.ADVANCEDNAIVE) != null) {
			totalAmountSolutionsAdvancedNaiveSearch = totalAmountSolutionsMap
					.get(Enums.AlgorithmToPerform.ADVANCEDNAIVE) + "";
			amountCheapestSolutionsAdvancedNaive = cheapestSolutionsMap.get(Enums.AlgorithmToPerform.ADVANCEDNAIVE)
					+ "";
			logging = loggingMap.get(Enums.AlgorithmToPerform.ADVANCEDNAIVE);
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
				for (FlowNode f : entr.getValue()) {
					readers++;
				}
			}

			amountReaders = readers + "";
			readersPerDataObject = (readers / sumDataObjects) + "";

			double writers = 0;
			for (Entry<DataObjectReference, LinkedList<FlowNode>> entr : CommonFunctionality
					.getWritersForDataObjects(api.getModelInstance()).entrySet()) {
				for (FlowNode f : entr.getValue()) {
					writers++;
				}
			}
			amountWriters = writers + "";
			writersPerDataObject = (writers / sumDataObjects) + "";

		}

		String exceptionNameExhaustive = "null";
		if (exceptionPerAlgorithm.get(Enums.AlgorithmToPerform.EXHAUSTIVE) != null) {
			exceptionNameExhaustive = exceptionPerAlgorithm.get(Enums.AlgorithmToPerform.EXHAUSTIVE).getClass()
					.getCanonicalName();
		}

		String exceptionNameNaive = "null";
		if (exceptionPerAlgorithm.get(Enums.AlgorithmToPerform.NAIVE) != null) {
			exceptionNameNaive = exceptionPerAlgorithm.get(Enums.AlgorithmToPerform.NAIVE).getClass()
					.getCanonicalName();
		}

		String exceptionNameIncrementalNaive = "null";
		if (exceptionPerAlgorithm.get(Enums.AlgorithmToPerform.INCREMENTALNAIVE) != null) {
			exceptionNameIncrementalNaive = exceptionPerAlgorithm.get(Enums.AlgorithmToPerform.INCREMENTALNAIVE)
					.getClass().getCanonicalName();
		}

		String exceptionNameAdvancedNaive = "null";
		if (exceptionPerAlgorithm.get(Enums.AlgorithmToPerform.ADVANCEDNAIVE) != null) {
			exceptionNameAdvancedNaive = exceptionPerAlgorithm.get(Enums.AlgorithmToPerform.ADVANCEDNAIVE).getClass()
					.getCanonicalName();
		}

		String[] row = new String[] { fileName, pathToFile, logging, exceptionNameExhaustive, exceptionNameNaive,
				exceptionNameIncrementalNaive, exceptionNameAdvancedNaive, totalAmountSolutionsExhaustiveSearch,
				totalAmountSolutionsNaiveSearch, totalAmountSolutionsIncrementalNaiveSearch,
				totalAmountSolutionsAdvancedNaiveSearch, amountCheapestSolutionsExhaustive,
				amountCheapestSolutionsNaiveSearch, amountCheapestSolutionsIncrementalNaiveSearch,
				amountCheapestSolutionsAdvancedNaive, costCheapestSolutionExhaustive, costCheapestSolutionNaiveSearch,
				costCheapestSolutionIncrementalNaiveSearch, costCheapestSolutionAdvancedNaive,
				exhaustiveSearchGenCombsTime, exhaustiveSearchCalcMeasureTime, exhaustiveSearchExecutionTime,
				naiveSearchGenCombsTime, naiveSearchCalcMeasureTime, naiveSearchExecutionTime,
				incrementalNaiveSearchGenCombsTime, incrementalNaiveSearchCalcMeasureTime,
				incrementalNaiveSearchExecutionTime, advancedNaiveSearchGenCombsTime,
				advancedNaiveSearchCalcMeasureTime, advancedNaiveSearchExecutionTime, amountReaders, amountWriters,
				amountDataObjects, averageAmountDataObjectsPerDecision, readersPerDataObject, writersPerDataObject,
				amountExclusiveGtwSplits, amountParallelGtwSplitsBeforePreprocessing, amountTasks, amountElements,
				sumAmountAddActorsOfModel, averageAmountAddActorsOfModel, privateSphereSize, sphereSumOfModel,
				allPathsThroughProcess, amountMandConst, amountExclConst };

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
