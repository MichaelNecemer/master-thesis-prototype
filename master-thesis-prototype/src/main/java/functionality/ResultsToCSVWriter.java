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

import Mapping.BPMNBusinessRuleTask;
import Mapping.ProcessInstanceWithVoters;

public class ResultsToCSVWriter {

	private CSVWriter writer;
	private List<String[]> rows;

	public ResultsToCSVWriter(File csvFile) {
		try {
			this.writer = new CSVWriter(new FileWriter(csvFile, true), ';', CSVWriter.NO_QUOTE_CHARACTER,
					CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);

			rows = new ArrayList<>();
			String[] header = new String[] { "fileName", "pathToFile", "logging", "exceptionLocalMin",
					"exceptionBruteForce", "exceptionLocalMinWithLimit", "totalAmountSolutions",
					"solution(s) bruteForce", "solution(s) localMinimumAlgorithm",
					"solution(s) localMinimumAlgorithmWithLimit", "amount cheapest solution(s) bruteForce",
					"costCheapestSolutionBruteForce", "costCheapestSolutionLocalMin",
					"costCheapestSolutionLocalMinWithLimit", "averageCostAllBruteForceSolutions",
					"executionTimeLocalMinimumAlogrithm in sec", "executionTimeLocalMinWithLimit in sec",
					"executionTimeBruteForce in sec", "deltaExecutionTime in sec (localMin - bruteForce)",
					"deltaExecutionTime in sec (localMinWithLimit - bruteForce)",
					"isCheapestSolutionOfLocalMinInBruteForce", "isCheapestSolutionOfLocalMinWithLimitInBruteForce",
					"amountPaths", "amountReaders", "amountWriters", "amountDataObjects",
					"averageAmountDataObjectsPerDecision", "amountReadersPerDataObject", "amountWritersPerDataObject",
					"amountExclusiveGateways", "amountParallelGateways", "amountTasks", "amountElements",
					"amountSumVoters", "averageAmountVoters", "globalSphereSize", "averageSphereSum", "dependentBrts","pathsThroughProcess",
					"deciderOrVerifier" };
			this.rows.add(header);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void writeResultsOfOneAlgorithmToCSVFile(API api,
			HashMap<String, LinkedList<ProcessInstanceWithVoters>> algorithmMap,
			HashMap<String, Exception> exceptionPerAlgorithm) {
		String pathToFile = "null";
		String fileName = "null";
		LinkedList<ProcessInstanceWithVoters> pInstancesLocalMin = null;
		LinkedList<ProcessInstanceWithVoters> pInstancesBruteForce = null;
		String localMinAlgorithmTime = "null";
		String bruteForceAlgorithmTime = "null";
		String timeDifference = "null";
		String logging = "null";

		if (api != null && pathToFile.contentEquals("null") && fileName.contentEquals("null")) {
			pathToFile = api.getProcessFile().getAbsolutePath();
			fileName = api.getProcessFile().getName();
		}

		for (Entry<String, LinkedList<ProcessInstanceWithVoters>> algorithmEntry : algorithmMap.entrySet()) {
			String algorithm = algorithmEntry.getKey();
			if (algorithm.contentEquals("bruteForce")) {
				pInstancesBruteForce = algorithmEntry.getValue();
				bruteForceAlgorithmTime = api.getExecutionTimeBruteForceAlgorithm() + "";
			} else if (algorithm.contentEquals("localMin")) {
				pInstancesLocalMin = algorithmEntry.getValue();
				localMinAlgorithmTime = api.getExecutionTimeLocalMinimumAlgorithm() + "";
			}

		}

		if ((!localMinAlgorithmTime.contentEquals("null")) && (!bruteForceAlgorithmTime.contentEquals("null"))) {
			timeDifference = (Double.parseDouble(localMinAlgorithmTime) - Double.parseDouble(bruteForceAlgorithmTime))
					+ "";
		}

		// map the cheapest process instances to rows in the csv file

		String amountCheapestSolutionsLocalMin = "null";
		String costCheapestSolution = "null";
		if (pInstancesLocalMin != null && !pInstancesLocalMin.isEmpty()) {
			costCheapestSolution = pInstancesLocalMin.get(0).getCostForModelInstance() + "";
			amountCheapestSolutionsLocalMin = pInstancesLocalMin.size() + "";
		}

		String amountCheapestSolutionsBruteForce = "null";
		String averageCostAllSolutions = "null";
		String amountSolutionsBruteForce = "null";
		if (pInstancesBruteForce != null && !pInstancesBruteForce.isEmpty()) {
			LinkedList<ProcessInstanceWithVoters> cheapestBruteForceInst = CommonFunctionality
					.getCheapestProcessInstancesWithVoters(pInstancesBruteForce);
			amountCheapestSolutionsBruteForce = cheapestBruteForceInst.size() + "";
			amountSolutionsBruteForce = pInstancesBruteForce.size() + "";
			averageCostAllSolutions = CommonFunctionality.getAverageCostForAllModelInstances(pInstancesBruteForce) + "";
			if (costCheapestSolution.contentEquals("null")) {
				costCheapestSolution = cheapestBruteForceInst.get(0).getCostForModelInstance() + "";

			}

		}

		String readersPerDataObject = "null";
		String writersPerDataObject = "null";
		String amountSolutions = "null";
		String allPathsThroughProcess = "null";
		String amountExclusiveGtwSplits = "null";
		String amountParallelGtwSplits = "null";
		String amountTasks = "null";
		String amountElements = "null";
		String sumAmountVotersOfModel = "null";
		String averageAmountVotersOfModel = "null";
		String globalSphereSize = "null";
		String sphereSumOfModel = "null";
		String deciderOrVerifier = "null";
		String amountReaders = "null";
		String amountWriters = "null";
		String amountDataObjects = "null";
		String averageAmountDataObjectsPerDecision = "null";
		String pathsThroughProcess = "null";

		if (api != null) {
			allPathsThroughProcess = api.getAllPathsThroughProcess().size() + "";
			amountExclusiveGtwSplits = CommonFunctionality.getAmountExclusiveGtwSplits(api.getModelInstance()) + "";
			amountParallelGtwSplits = CommonFunctionality.getAmountParallelGtwSplits(api.getModelInstance()) + "";
			amountTasks = CommonFunctionality.getAmountTasks(api.getModelInstance()) + "";
			amountElements = CommonFunctionality.getAmountElements(api.getModelInstance()) + "";
			sumAmountVotersOfModel = CommonFunctionality.getSumAmountVotersOfModel(api.getModelInstance()) + "";
			averageAmountVotersOfModel = CommonFunctionality.getAverageAmountVotersOfModel(api.getModelInstance()) + "";
			globalSphereSize = CommonFunctionality.getGlobalSphere(api.getModelInstance(), api.modelWithLanes()) + "";
			sphereSumOfModel = CommonFunctionality.getSphereSumOfModel(api.getModelInstance()) + "";
			amountSolutions = api.getAmountPossibleCombinationsOfParticipants();
			deciderOrVerifier = api.getDeciderOrVerifier();
			amountDataObjects = CommonFunctionality.getAmountDataObjects(api.getModelInstance()) + "";

			double sumDataObjects = 0;
			for (BPMNBusinessRuleTask brt : api.getBusinessRuleTasks()) {
				sumDataObjects += brt.getDataObjects().size();
			}
			averageAmountDataObjectsPerDecision = sumDataObjects / api.getBusinessRuleTasks().size() + "";

			double readers = 0;
			for (Entry<DataObjectReference, LinkedList<FlowNode>> entr : CommonFunctionality
					.getReadersForDataObjects(api.getModelInstance()).entrySet()) {
				for (FlowNode f : entr.getValue()) {
					readers++;
				}
			}

			amountReaders = readers + "";
			readersPerDataObject = (readers/sumDataObjects) + "";

			double writers = 0;
			for (Entry<DataObjectReference, LinkedList<FlowNode>> entr : CommonFunctionality
					.getWritersForDataObjects(api.getModelInstance()).entrySet()) {
				for (FlowNode f : entr.getValue()) {
					writers++;
				}
			}
			amountWriters = writers + "";
			writersPerDataObject = (writers/sumDataObjects) + "";
			pathsThroughProcess = api.getPathsThroughProcess()+"";
		}

		String exceptionNameLocalMin = "null";
		if (exceptionPerAlgorithm.get("localMin") != null) {
			exceptionNameLocalMin = exceptionPerAlgorithm.get("localMin").getClass().getCanonicalName();
		}
		String exceptionNameBruteForce = "null";
		if (exceptionPerAlgorithm.get("bruteForce") != null) {
			exceptionNameBruteForce = exceptionPerAlgorithm.get("bruteForce").getClass().getCanonicalName();
		}

		String[] row = new String[] { fileName, pathToFile, logging, exceptionNameLocalMin, exceptionNameBruteForce,
				amountSolutions, amountSolutionsBruteForce, amountCheapestSolutionsLocalMin,
				amountCheapestSolutionsBruteForce, costCheapestSolution, averageCostAllSolutions, localMinAlgorithmTime,
				bruteForceAlgorithmTime, timeDifference, "null", allPathsThroughProcess, amountReaders, amountWriters,
				amountDataObjects, averageAmountDataObjectsPerDecision, readersPerDataObject, writersPerDataObject,
				amountExclusiveGtwSplits, amountParallelGtwSplits, amountTasks, amountElements, sumAmountVotersOfModel,
				averageAmountVotersOfModel, globalSphereSize, sphereSumOfModel, pathsThroughProcess, deciderOrVerifier };

		this.rows.add(row);

	}

	public void writeResultsOfAlgorithmsToCSVFile(API bruteForceApi, API localMinApi, API localMinWithLimitApi,
			HashMap<String, LinkedList<ProcessInstanceWithVoters>> algorithmMap,
			HashMap<String, Exception> exceptionPerAlgorithm, String isCheapestSolutionInBruteForce,
			String isCheapestSolutionWithLimitInBruteForce) {
		// call this method after algorithms ran
		// compare the execution of algorithms
		// write the metadata to a csv file

		String pathToFile = bruteForceApi.getProcessFile().getAbsolutePath();
		String fileName = bruteForceApi.getProcessFile().getName();
		String logging = "null";

		LinkedList<ProcessInstanceWithVoters> pInstancesLocalMin = null;
		LinkedList<ProcessInstanceWithVoters> pInstancesBruteForce = null;
		LinkedList<ProcessInstanceWithVoters> pInstancesLocalMinWithLimit = null;
		String localMinAlgorithmTime = "null";
		String bruteForceAlgorithmTime = "null";
		String localMinWithLimitAlgorithmTime = "null";
		String timeDifferenceLocalMinBruteForce = "null";
		String timeDifferenceLocalMinWithLimitBruteForce = "null";
		String pathsThroughProcess = "null";

		if (algorithmMap.get(localMinApi.getAlgorithmToPerform()) != null) {
			pInstancesLocalMin = algorithmMap.get(localMinApi.getAlgorithmToPerform());
			localMinAlgorithmTime = localMinApi.getExecutionTimeLocalMinimumAlgorithm() + "";
			if(pathsThroughProcess.contentEquals("null")) {
				pathsThroughProcess = localMinApi.getPathsThroughProcess()+"";
			}
			for (ProcessInstanceWithVoters pInst : pInstancesLocalMin) {
				for (BPMNBusinessRuleTask brt : localMinApi.getBusinessRuleTasks()) {
					if (brt.getSuccessors().get(0).getSuccessors().size() == 2) {
						if (!pInst.getVotersMap().containsKey(brt)) {
							// when a processInstance does not contain a brt needed for a decision
							// log
							logging = "Not every decision has been calculated with localMinApi!";
						}
					}

				}

			}

		}

		if (algorithmMap.get(bruteForceApi.getAlgorithmToPerform()) != null) {
			pInstancesBruteForce = algorithmMap.get(bruteForceApi.getAlgorithmToPerform());
			bruteForceAlgorithmTime = bruteForceApi.getExecutionTimeBruteForceAlgorithm() + "";
			if(pathsThroughProcess.contentEquals("null")) {
				pathsThroughProcess = bruteForceApi.getPathsThroughProcess()+"";
			}
			for (ProcessInstanceWithVoters pInst : pInstancesBruteForce) {
				for (BPMNBusinessRuleTask brt : bruteForceApi.getBusinessRuleTasks()) {
					if (brt.getSuccessors().get(0).getSuccessors().size() == 2) {
						if (!pInst.getVotersMap().containsKey(brt)) {
							// when a processInstance does not contain a brt needed for a decision
							// log
							logging = "Not every decision has been calculated with bruteForce!";
						}
					}

				}

			}

		}

		if (algorithmMap.get(localMinWithLimitApi.getAlgorithmToPerform()) != null) {
			pInstancesLocalMinWithLimit = algorithmMap.get(localMinWithLimitApi.getAlgorithmToPerform());
			localMinWithLimitAlgorithmTime = localMinWithLimitApi.getExecutionTimeLocalMinimumAlgorithmWithLimit() + "";
			if(pathsThroughProcess.contentEquals("null")) {
				pathsThroughProcess = localMinWithLimitApi.getPathsThroughProcess()+"";
			}
			for (ProcessInstanceWithVoters pInst : pInstancesLocalMinWithLimit) {
				for (BPMNBusinessRuleTask brt : localMinWithLimitApi.getBusinessRuleTasks()) {
					if (brt.getSuccessors().get(0).getSuccessors().size() == 2) {
						if (!pInst.getVotersMap().containsKey(brt)) {
							// when a processInstance does not contain a brt needed for a decision
							// log
							logging = "Not every decision has been calculated with localMinWithLimit!";
						}
					}

				}

			}

		}

		if ((!localMinAlgorithmTime.contentEquals("null")) && (!bruteForceAlgorithmTime.contentEquals("null"))) {
			timeDifferenceLocalMinBruteForce = (Double.parseDouble(localMinAlgorithmTime)
					- Double.parseDouble(bruteForceAlgorithmTime)) + "";
		}

		if ((!localMinWithLimitAlgorithmTime.contentEquals("null"))
				&& (!bruteForceAlgorithmTime.contentEquals("null"))) {
			timeDifferenceLocalMinWithLimitBruteForce = (Double.parseDouble(localMinWithLimitAlgorithmTime)
					- Double.parseDouble(bruteForceAlgorithmTime)) + "";
		}

		// map the cheapest process instances to rows in the csv file

		String amountCheapestSolutionsLocalMin = "null";
		String costCheapestSolutionLocalMin = "null";
		if (pInstancesLocalMin != null && !pInstancesLocalMin.isEmpty()) {
			costCheapestSolutionLocalMin = pInstancesLocalMin.get(0).getCostForModelInstance() + "";
			amountCheapestSolutionsLocalMin = pInstancesLocalMin.size() + "";
		}

		String amountCheapestSolutionsBruteForce = "null";
		String averageCostAllSolutions = "null";
		String amountSolutionsBruteForce = "null";
		String costCheapestSolutionBruteForce = "null";
		String amountDataObjects = "null";
		String averageAmountDataObjectsPerDecision = "null";

		if (pInstancesBruteForce != null && !pInstancesBruteForce.isEmpty()) {
			LinkedList<ProcessInstanceWithVoters> cheapestBruteForceInst = CommonFunctionality
					.getCheapestProcessInstancesWithVoters(pInstancesBruteForce);
			amountCheapestSolutionsBruteForce = cheapestBruteForceInst.size() + "";
			amountSolutionsBruteForce = pInstancesBruteForce.size() + "";
			averageCostAllSolutions = CommonFunctionality.getAverageCostForAllModelInstances(pInstancesBruteForce) + "";
			costCheapestSolutionBruteForce = cheapestBruteForceInst.get(0).getCostForModelInstance() + "";
		}

		String amountCheapestSolutionsLocalMinWithLimit = "null";
		String costCheapestSolutionLocalMinWithLimit = "null";
		if (pInstancesLocalMinWithLimit != null && !pInstancesLocalMinWithLimit.isEmpty()) {
			costCheapestSolutionLocalMinWithLimit = pInstancesLocalMinWithLimit.get(0).getCostForModelInstance() + "";
			amountCheapestSolutionsLocalMinWithLimit = pInstancesLocalMinWithLimit.size() + "";

		}

		String readersPerDataObject = "null";
		String writersPerDataObject = "null";
		StringBuilder dependentBrts = new StringBuilder();
		String allPathsThroughProcess = "null";
		String amountExclusiveGtwSplits = "null";
		String amountParallelGtwSplits = "null";
		String amountTasks = "null";
		String amountElements = "null";
		String sumAmountVotersOfModel = "null";
		String averageAmountVotersOfModel = "null";
		String globalSphereSize = "null";
		String sphereSumOfModel = "null";
		String amountMaxSolutionsBruteForce = "null";
		String amountMaxSolutionsLocalMin = "null";
		String amountMaxSolutionsLocalMinWithLimit = "null";
		String deciderOrVerifier = "null";
		String amountReaders = "null";
		String amountWriters = "null";

		if (bruteForceApi != null) {
			amountDataObjects = CommonFunctionality.getAmountDataObjects(bruteForceApi.getModelInstance()) + "";

			double sumDataObjects = 0;
			for (BPMNBusinessRuleTask brt : bruteForceApi.getBusinessRuleTasks()) {
				sumDataObjects += brt.getDataObjects().size();
			}
			averageAmountDataObjectsPerDecision = sumDataObjects / bruteForceApi.getBusinessRuleTasks().size() + "";

			allPathsThroughProcess = bruteForceApi.getAllPathsThroughProcess().size() + "";
			amountExclusiveGtwSplits = CommonFunctionality.getAmountExclusiveGtwSplits(bruteForceApi.getModelInstance())
					+ "";
			amountParallelGtwSplits = CommonFunctionality.getAmountParallelGtwSplits(bruteForceApi.getModelInstance())
					+ "";
			amountTasks = CommonFunctionality.getAmountTasks(bruteForceApi.getModelInstance()) + "";
			amountElements = CommonFunctionality.getAmountElements(bruteForceApi.getModelInstance()) + "";
			sumAmountVotersOfModel = CommonFunctionality.getSumAmountVotersOfModel(bruteForceApi.getModelInstance())
					+ "";
			averageAmountVotersOfModel = CommonFunctionality
					.getAverageAmountVotersOfModel(bruteForceApi.getModelInstance()) + "";
			globalSphereSize = CommonFunctionality.getGlobalSphere(bruteForceApi.getModelInstance(),
					bruteForceApi.modelWithLanes()) + "";
			sphereSumOfModel = CommonFunctionality.getSphereSumOfModel(bruteForceApi.getModelInstance()) + "";
			deciderOrVerifier = bruteForceApi.getDeciderOrVerifier();
			double readers = 0;
			for (Entry<DataObjectReference, LinkedList<FlowNode>> entr : CommonFunctionality
					.getReadersForDataObjects(bruteForceApi.getModelInstance()).entrySet()) {
				for (FlowNode f : entr.getValue()) {
					readers++;
				}
			}

			amountReaders = readers + "";
			readersPerDataObject = (readers/sumDataObjects) + "";

			double writers = 0;
			for (Entry<DataObjectReference, LinkedList<FlowNode>> entr : CommonFunctionality
					.getWritersForDataObjects(bruteForceApi.getModelInstance()).entrySet()) {
				for (FlowNode f : entr.getValue()) {
					writers++;
				}
			}
			amountWriters = writers + "";
			writersPerDataObject = (writers/sumDataObjects) + "";

			boolean dependentBrtsInProcess = false;
			/*for (BPMNBusinessRuleTask brt : bruteForceApi.getBusinessRuleTasks()) {
				if (!brt.getRelatedBrts().isEmpty()) {
					dependentBrtsInProcess = true;
					break;
				}
			}*/

			dependentBrts.append(dependentBrtsInProcess + "");

		}

		String exceptionNameLocalMin = "null";
		if (exceptionPerAlgorithm.get("localMin") != null) {
			exceptionNameLocalMin = exceptionPerAlgorithm.get("localMin").getClass().getCanonicalName();
		} else {
			amountMaxSolutionsLocalMin = localMinApi.getAmountPossibleCombinationsOfParticipants();
		}
		String exceptionNameBruteForce = "null";
		if (exceptionPerAlgorithm.get("bruteForce") != null) {
			exceptionNameBruteForce = exceptionPerAlgorithm.get("bruteForce").getClass().getCanonicalName();
		} else {
			amountMaxSolutionsBruteForce = bruteForceApi.getAmountPossibleCombinationsOfParticipants();
		}

		String exceptionNameLocalMinWithLimit = "null";
		if (exceptionPerAlgorithm.get(localMinWithLimitApi.getAlgorithmToPerform()) != null) {
			exceptionNameLocalMinWithLimit = exceptionPerAlgorithm.get(localMinWithLimitApi.getAlgorithmToPerform())
					.getClass().getCanonicalName();
		} else {
			amountMaxSolutionsLocalMinWithLimit = localMinWithLimitApi.getAmountPossibleCombinationsOfParticipants();
		}

		LinkedList<String> amountMaxSolutionsList = new LinkedList<String>();
		amountMaxSolutionsList.add(amountMaxSolutionsBruteForce);
		amountMaxSolutionsList.add(amountMaxSolutionsLocalMin);
		amountMaxSolutionsList.add(amountMaxSolutionsLocalMinWithLimit);
		String amountSolutions = "null";
		for (int i = 0; i < amountMaxSolutionsList.size() - 1; i++) {
			String currStr = amountMaxSolutionsList.get(i);
			String nextStr = amountMaxSolutionsList.get(i + 1);
			int currStrInt = 0;
			int nextStrInt = 0;
			if (amountSolutions.contentEquals("null")) {
				if (currStr.contentEquals("Overflow") || nextStr.contentEquals("Overflow")) {
					amountSolutions = "Overflow";
				}
			}

			if ((!currStr.contentEquals("null")) && (!currStr.contentEquals("Overflow"))) {
				currStrInt = Integer.parseInt(currStr);
			}

			if ((!nextStr.contentEquals("null")) && (!nextStr.contentEquals("Overflow"))) {
				nextStrInt = Integer.parseInt(nextStr);
			}

			if (nextStrInt > currStrInt) {
				amountSolutions = nextStr;
			} else if (nextStrInt <= currStrInt) {
				amountSolutions = currStr;
			}
		}

		String[] row = new String[] { fileName, pathToFile, logging, exceptionNameLocalMin, exceptionNameBruteForce,
				exceptionNameLocalMinWithLimit, amountSolutions, amountSolutionsBruteForce,
				amountCheapestSolutionsLocalMin, amountCheapestSolutionsLocalMinWithLimit,
				amountCheapestSolutionsBruteForce, costCheapestSolutionBruteForce, costCheapestSolutionLocalMin,
				costCheapestSolutionLocalMinWithLimit, averageCostAllSolutions, localMinAlgorithmTime,
				localMinWithLimitAlgorithmTime, bruteForceAlgorithmTime, timeDifferenceLocalMinBruteForce,
				timeDifferenceLocalMinWithLimitBruteForce, isCheapestSolutionInBruteForce,
				isCheapestSolutionWithLimitInBruteForce, allPathsThroughProcess, amountReaders, amountWriters,
				amountDataObjects, averageAmountDataObjectsPerDecision, readersPerDataObject, writersPerDataObject,
				amountExclusiveGtwSplits, amountParallelGtwSplits, amountTasks, amountElements, sumAmountVotersOfModel,
				averageAmountVotersOfModel, globalSphereSize, sphereSumOfModel, dependentBrts.toString(), pathsThroughProcess,
				deciderOrVerifier };

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
