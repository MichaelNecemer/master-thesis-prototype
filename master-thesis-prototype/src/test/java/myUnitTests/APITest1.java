package myUnitTests;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import Mapping.BPMNBusinessRuleTask;
import Mapping.BPMNDataObject;
import Mapping.BPMNElement;
import Mapping.BPMNExclusiveGateway;
import Mapping.BPMNParticipant;
import Mapping.BPMNTask;
import Mapping.Combination;
import functionality.API;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class APITest1 {
	ArrayList<Double> cost = new ArrayList<>(Arrays.asList(10.0, 5.0, 3.0, 2.0));
	double costForAddingReaderAfterBrt = 1.0;
	String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\modelle\\brtsIn2branches1.bpmn";
	
	@Test
	public void testAmountPossibleProcessInstances() {

		try {
			API a2 = new API(pathToFile, cost, costForAddingReaderAfterBrt);
			// 4 voters for Brt1
			// 6 voters for Brt2
			// 6 voters for Brt3
			// a processInstance contains a certain assignment of possible voters for each
			// brt
			// 4*6*6 possible combinations = 144 possible combinations

			int expectedAmountPossibleCombinations = 144;

			Assert.assertEquals(expectedAmountPossibleCombinations, a2.getAmountPossibleCombinationsOfParticipants(),
					0);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Test
	public void testAllLanesAsParticipantsInGlobalSphere() {

		try {
			API a2 = new API(pathToFile, cost, costForAddingReaderAfterBrt);
			LinkedList<BPMNParticipant> expected = new LinkedList<BPMNParticipant>();
			LinkedList<BPMNParticipant> globalSphereList = a2.getGlobalSphereList();

			BPMNParticipant p1 = new BPMNParticipant("Lane_07pzlbb", "RS");
			expected.add(p1);

			BPMNParticipant p2 = new BPMNParticipant("Lane_155vkn8", "GP");
			expected.add(p2);

			BPMNParticipant p3 = new BPMNParticipant("Lane_0m888yn", "IC");
			expected.add(p3);

			BPMNParticipant p4 = new BPMNParticipant("Lane_190r1hy", "DI");
			expected.add(p4);

			Assert.assertTrue(expected.size() == globalSphereList.size() && expected.containsAll(globalSphereList)
					&& globalSphereList.containsAll(expected));

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Test
	public void testStaticSpheresForDataObjects() {

		try {
			API a2 = new API(pathToFile, cost, costForAddingReaderAfterBrt);
			LinkedList<BPMNParticipant> expectedStaticSphereForD1 = new LinkedList<BPMNParticipant>();

			BPMNParticipant p1 = new BPMNParticipant("Lane_07pzlbb", "RS");
			expectedStaticSphereForD1.add(p1);

			BPMNParticipant p2 = new BPMNParticipant("Lane_155vkn8", "GP");
			expectedStaticSphereForD1.add(p2);

			BPMNParticipant p3 = new BPMNParticipant("Lane_0m888yn", "IC");
			expectedStaticSphereForD1.add(p3);

			BPMNParticipant p4 = new BPMNParticipant("Lane_190r1hy", "DI");
			expectedStaticSphereForD1.add(p4);

			List<BPMNParticipant> staticSphereList = a2.getDataObjects().get(0).getStaticSphere();
			Assert.assertTrue(expectedStaticSphereForD1.size() == staticSphereList.size()
					&& expectedStaticSphereForD1.containsAll(staticSphereList)
					&& staticSphereList.containsAll(expectedStaticSphereForD1));

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Test
	public void testEffectivePathsFromWriter1ToEnd() {

		try {
			API a2 = new API(pathToFile, cost, costForAddingReaderAfterBrt);
			LinkedList<LinkedList<BPMNElement>> expectedEffectivePathsWriter1 = new LinkedList<LinkedList<BPMNElement>>();
			LinkedList<BPMNElement> path1 = new LinkedList<BPMNElement>();
			path1.add(a2.getNodeById("Task_00qdu1v"));
			path1.add(a2.getNodeById("BusinessRuleTask_1qnt4kf"));
			path1.add(a2.getNodeById("ExclusiveGateway_1ns7t33"));
			path1.add(a2.getNodeById("Task_0fng24x"));
			path1.add(a2.getNodeById("Task_0hy8xp7"));
			path1.add(a2.getNodeById("ExclusiveGateway_1jeju3n"));
			path1.add(a2.getNodeById("Task_1bnxv6s"));
			path1.add(a2.getNodeById("ExclusiveGateway_1toqjx8"));
			path1.add(a2.getNodeById("ExclusiveGateway_1omk1f4"));
			path1.add(a2.getNodeById("EndEvent_0nbsfat"));

			LinkedList<BPMNElement> path2 = new LinkedList<BPMNElement>();
			path2.add(a2.getNodeById("Task_00qdu1v"));
			path2.add(a2.getNodeById("BusinessRuleTask_1qnt4kf"));
			path2.add(a2.getNodeById("ExclusiveGateway_1ns7t33"));
			path2.add(a2.getNodeById("Task_0fng24x"));
			path2.add(a2.getNodeById("Task_0hy8xp7"));
			path2.add(a2.getNodeById("ExclusiveGateway_1jeju3n"));
			path2.add(a2.getNodeById("Task_00uo1ca"));
			path2.add(a2.getNodeById("ExclusiveGateway_1toqjx8"));
			path2.add(a2.getNodeById("ExclusiveGateway_1omk1f4"));
			path2.add(a2.getNodeById("EndEvent_0nbsfat"));

			expectedEffectivePathsWriter1.add(path1);
			expectedEffectivePathsWriter1.add(path2);

			BPMNTask writerTask = (BPMNTask) a2.getNodeById("Task_00qdu1v");
			BPMNDataObject dataO = writerTask.getDataObjects().get(0);

			HashMap<Boolean, LinkedList<LinkedList<BPMNElement>>> list = a2.allEffectivePathsForWriters(dataO,
					writerTask, writerTask, a2.getBpmnEnd(), new LinkedList<BPMNElement>(),
					new LinkedList<BPMNElement>(), new LinkedList<BPMNElement>(),
					new LinkedList<LinkedList<BPMNElement>>());
			LinkedList<LinkedList<BPMNElement>> resultListCombined = new LinkedList<LinkedList<BPMNElement>>();
			resultListCombined.addAll(list.get(true));
			Assert.assertEquals(expectedEffectivePathsWriter1, resultListCombined);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Test
	public void testEffectivePathsFromWriter2ToEnd() {

		try {
			API a2 = new API(pathToFile, cost, costForAddingReaderAfterBrt);
			LinkedList<LinkedList<BPMNElement>> expectedEffectivePathsWriter2 = new LinkedList<LinkedList<BPMNElement>>();
			LinkedList<BPMNElement> path1 = new LinkedList<BPMNElement>();
			path1.add(a2.getNodeById("Task_0zvbv9t"));
			path1.add(a2.getNodeById("Task_02f5d5q"));
			path1.add(a2.getNodeById("ExclusiveGateway_0eoal93"));
			path1.add(a2.getNodeById("Task_1mdsju4"));
			path1.add(a2.getNodeById("ExclusiveGateway_0qvvriv"));
			path1.add(a2.getNodeById("ExclusiveGateway_1omk1f4"));
			path1.add(a2.getNodeById("EndEvent_0nbsfat"));

			LinkedList<BPMNElement> path2 = new LinkedList<BPMNElement>();
			path2.add(a2.getNodeById("Task_0zvbv9t"));
			path2.add(a2.getNodeById("Task_02f5d5q"));
			path2.add(a2.getNodeById("ExclusiveGateway_0eoal93"));
			path2.add(a2.getNodeById("Task_06czjs7"));
			path2.add(a2.getNodeById("ExclusiveGateway_0qvvriv"));
			path2.add(a2.getNodeById("ExclusiveGateway_1omk1f4"));
			path2.add(a2.getNodeById("EndEvent_0nbsfat"));

			expectedEffectivePathsWriter2.add(path1);
			expectedEffectivePathsWriter2.add(path2);

			BPMNTask writerTask = (BPMNTask) a2.getNodeById("Task_0zvbv9t");
			BPMNDataObject dataO = writerTask.getDataObjects().get(0);

			HashMap<Boolean, LinkedList<LinkedList<BPMNElement>>> list = a2.allEffectivePathsForWriters(dataO,
					writerTask, writerTask, a2.getBpmnEnd(), new LinkedList<BPMNElement>(),
					new LinkedList<BPMNElement>(), new LinkedList<BPMNElement>(),
					new LinkedList<LinkedList<BPMNElement>>());
			LinkedList<LinkedList<BPMNElement>> resultListCombined = new LinkedList<LinkedList<BPMNElement>>();
			resultListCombined.addAll(list.get(true));
			Assert.assertEquals(expectedEffectivePathsWriter2, resultListCombined);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Test
	public void testSphereOfReaderParticipantsForWriter1ForBrt1() {

		try {
			API a2 = new API(pathToFile, cost, costForAddingReaderAfterBrt);
			BPMNTask writerTask = (BPMNTask) a2.getNodeById("Task_00qdu1v");
			BPMNBusinessRuleTask brt1 = (BPMNBusinessRuleTask) a2.getNodeById("BusinessRuleTask_1qnt4kf");
			BPMNDataObject dataO = writerTask.getDataObjects().get(0);
			BPMNParticipant readerDI = new BPMNParticipant("Lane_190r1hy", "DI");
			BPMNParticipant readerGP = new BPMNParticipant("Lane_155vkn8", "GP");
			BPMNParticipant readerRS = new BPMNParticipant("Lane_07pzlbb", "RS");
			BPMNParticipant readerIC = new BPMNParticipant("Lane_0m888yn", "IC");

			String sphereForDI = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(brt1, writerTask,
					dataO, readerDI, new HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>>());
			Assert.assertEquals("Static", sphereForDI);
			String sphereForGP = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(brt1, writerTask,
					dataO, readerGP, new HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>>());
			Assert.assertEquals("Static", sphereForGP);
			String sphereForRS = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(brt1, writerTask,
					dataO, readerRS, new HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>>());
			Assert.assertEquals("Strong-Dynamic", sphereForRS);
			String sphereForIC = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(brt1, writerTask,
					dataO, readerIC, new HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>>());
			Assert.assertEquals("Weak-Dynamic", sphereForIC);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Test
	public void testSphereOfParticipantsForBrt3WhenGPChosenForBrt1() {

		try {
			API a2 = new API(pathToFile, cost, costForAddingReaderAfterBrt);
			BPMNTask writerTask = (BPMNTask) a2.getNodeById("Task_00qdu1v");
			BPMNBusinessRuleTask brt3 = (BPMNBusinessRuleTask) a2.getNodeById("Task_0hy8xp7");
			BPMNDataObject dataO = writerTask.getDataObjects().get(0);

			BPMNParticipant readerGP = new BPMNParticipant("Lane_155vkn8", "GP");
			BPMNParticipant readerDI = new BPMNParticipant("Lane_190r1hy", "DI");
			BPMNParticipant readerRS = new BPMNParticipant("Lane_07pzlbb", "RS");
			BPMNParticipant readerIC = new BPMNParticipant("Lane_0m888yn", "IC");

			// consider the GP has already been chosen as the voter for brt1!!!

			// Let the voter for Brt1 be the GP
			HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> alreadyChosenVoterGP = new HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>>();
			LinkedList<BPMNParticipant> voterForBrt1GP = new LinkedList<BPMNParticipant>();
			voterForBrt1GP.add(readerGP);
			alreadyChosenVoterGP.put((BPMNBusinessRuleTask) a2.getNodeById("BusinessRuleTask_1qnt4kf"), voterForBrt1GP);
			String sphereForGP = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(brt3, writerTask,
					dataO, readerGP, alreadyChosenVoterGP);
			Assert.assertEquals("Strong-Dynamic", sphereForGP);
			String sphereForDI = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(brt3, writerTask,
					dataO, readerDI, alreadyChosenVoterGP);
			Assert.assertEquals("Static", sphereForDI);
			String sphereForRS = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(brt3, writerTask,
					dataO, readerRS, alreadyChosenVoterGP);
			Assert.assertEquals("Strong-Dynamic", sphereForRS);
			String sphereForIC = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(brt3, writerTask,
					dataO, readerIC, alreadyChosenVoterGP);
			Assert.assertEquals("Strong-Dynamic", sphereForIC);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Test
	public void testSphereOfParticipantsForBrt3WhenICChosenForBrt1() {

		try {
			API a2 = new API(pathToFile, cost, costForAddingReaderAfterBrt);
			BPMNTask writerTask = (BPMNTask) a2.getNodeById("Task_00qdu1v");
			BPMNBusinessRuleTask brt3 = (BPMNBusinessRuleTask) a2.getNodeById("Task_0hy8xp7");
			BPMNDataObject dataO = writerTask.getDataObjects().get(0);

			BPMNParticipant readerGP = new BPMNParticipant("Lane_155vkn8", "GP");
			BPMNParticipant readerDI = new BPMNParticipant("Lane_190r1hy", "DI");
			BPMNParticipant readerRS = new BPMNParticipant("Lane_07pzlbb", "RS");
			BPMNParticipant readerIC = new BPMNParticipant("Lane_0m888yn", "IC");

			// Let the voter for Brt1 be the IC
			HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> alreadyChosenVoterIC = new HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>>();
			LinkedList<BPMNParticipant> voterForBrt1IC = new LinkedList<BPMNParticipant>();
			voterForBrt1IC.add(readerIC);
			alreadyChosenVoterIC.put((BPMNBusinessRuleTask) a2.getNodeById("BusinessRuleTask_1qnt4kf"), voterForBrt1IC);
			String sphereForGP = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(brt3, writerTask,
					dataO, readerGP, alreadyChosenVoterIC);
			Assert.assertEquals("Static", sphereForGP);
			String sphereForDI = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(brt3, writerTask,
					dataO, readerDI, alreadyChosenVoterIC);
			Assert.assertEquals("Static", sphereForDI);
			String sphereForRS = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(brt3, writerTask,
					dataO, readerRS, alreadyChosenVoterIC);
			Assert.assertEquals("Strong-Dynamic", sphereForRS);
			String sphereForIC = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(brt3, writerTask,
					dataO, readerIC, alreadyChosenVoterIC);
			Assert.assertEquals("Strong-Dynamic", sphereForIC);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Test
	public void testSphereOfParticipantsForBrt3WhenDIChosenForBrt1() {

		try {
			API a2 = new API(pathToFile, cost, costForAddingReaderAfterBrt);
			BPMNTask writerTask = (BPMNTask) a2.getNodeById("Task_00qdu1v");
			BPMNBusinessRuleTask brt3 = (BPMNBusinessRuleTask) a2.getNodeById("Task_0hy8xp7");
			BPMNDataObject dataO = writerTask.getDataObjects().get(0);

			BPMNParticipant readerGP = new BPMNParticipant("Lane_155vkn8", "GP");
			BPMNParticipant readerDI = new BPMNParticipant("Lane_190r1hy", "DI");
			BPMNParticipant readerRS = new BPMNParticipant("Lane_07pzlbb", "RS");
			BPMNParticipant readerIC = new BPMNParticipant("Lane_0m888yn", "IC");

			// Let the voter for Brt1 be the DI
			HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> alreadyChosenVoterDI = new HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>>();
			LinkedList<BPMNParticipant> voterForBrt1DI = new LinkedList<BPMNParticipant>();
			voterForBrt1DI.add(readerDI);
			alreadyChosenVoterDI.put((BPMNBusinessRuleTask) a2.getNodeById("BusinessRuleTask_1qnt4kf"), voterForBrt1DI);
			String sphereForGP = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(brt3, writerTask,
					dataO, readerGP, alreadyChosenVoterDI);
			Assert.assertEquals("Static", sphereForGP);
			String sphereForDI = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(brt3, writerTask,
					dataO, readerDI, alreadyChosenVoterDI);
			Assert.assertEquals("Strong-Dynamic", sphereForDI);
			String sphereForRS = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(brt3, writerTask,
					dataO, readerRS, alreadyChosenVoterDI);
			Assert.assertEquals("Strong-Dynamic", sphereForRS);
			String sphereForIC = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(brt3, writerTask,
					dataO, readerIC, alreadyChosenVoterDI);
			Assert.assertEquals("Strong-Dynamic", sphereForIC);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Test
	public void testSphereOfParticipantsForBrt3WhenRSChosenForBrt1() {

		try {
			API a2 = new API(pathToFile, cost, costForAddingReaderAfterBrt);
			BPMNTask writerTask = (BPMNTask) a2.getNodeById("Task_00qdu1v");
			BPMNBusinessRuleTask brt3 = (BPMNBusinessRuleTask) a2.getNodeById("Task_0hy8xp7");
			BPMNDataObject dataO = writerTask.getDataObjects().get(0);

			BPMNParticipant readerGP = new BPMNParticipant("Lane_155vkn8", "GP");
			BPMNParticipant readerDI = new BPMNParticipant("Lane_190r1hy", "DI");
			BPMNParticipant readerRS = new BPMNParticipant("Lane_07pzlbb", "RS");
			BPMNParticipant readerIC = new BPMNParticipant("Lane_0m888yn", "IC");

			// Let the voter for Brt1 be the RS
			HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> alreadyChosenVoterRS = new HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>>();
			LinkedList<BPMNParticipant> voterForBrt1RS = new LinkedList<BPMNParticipant>();
			voterForBrt1RS.add(readerRS);
			alreadyChosenVoterRS.put((BPMNBusinessRuleTask) a2.getNodeById("BusinessRuleTask_1qnt4kf"), voterForBrt1RS);
			String sphereForGP = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(brt3, writerTask,
					dataO, readerGP, alreadyChosenVoterRS);
			Assert.assertEquals("Static", sphereForGP);
			String sphereForDI = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(brt3, writerTask,
					dataO, readerDI, alreadyChosenVoterRS);
			Assert.assertEquals("Static", sphereForDI);
			String sphereForRS = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(brt3, writerTask,
					dataO, readerRS, alreadyChosenVoterRS);
			Assert.assertEquals("Strong-Dynamic", sphereForRS);
			String sphereForIC = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(brt3, writerTask,
					dataO, readerIC, alreadyChosenVoterRS);
			Assert.assertEquals("Strong-Dynamic", sphereForIC);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Test
	public void testSphereOfParticipantsForBrt2WhenGPChosenForBrt1() {

		try {
			API a2 = new API(pathToFile, cost, costForAddingReaderAfterBrt);
			// for brt2 the lastWriter is the writer 2!!!
			// therefore the chosen voter for brt1 should not have an impact on the spheres
			// for the participants for brt2!!!

			BPMNTask writerTask = (BPMNTask) a2.getNodeById("Task_0zvbv9t");
			BPMNBusinessRuleTask brt2 = (BPMNBusinessRuleTask) a2.getNodeById("Task_02f5d5q");
			BPMNDataObject dataO = writerTask.getDataObjects().get(0);

			BPMNParticipant readerGP = new BPMNParticipant("Lane_155vkn8", "GP");
			BPMNParticipant readerDI = new BPMNParticipant("Lane_190r1hy", "DI");
			BPMNParticipant readerRS = new BPMNParticipant("Lane_07pzlbb", "RS");
			BPMNParticipant readerIC = new BPMNParticipant("Lane_0m888yn", "IC");

			// Let the voter for Brt1 be the GP
			HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> alreadyChosenVoterGP = new HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>>();
			LinkedList<BPMNParticipant> voterForBrt1GP = new LinkedList<BPMNParticipant>();
			voterForBrt1GP.add(readerGP);
			alreadyChosenVoterGP.put((BPMNBusinessRuleTask) a2.getNodeById("BusinessRuleTask_1qnt4kf"), voterForBrt1GP);
			String sphereForGP = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(brt2, writerTask,
					dataO, readerGP, alreadyChosenVoterGP);
			Assert.assertEquals("Strong-Dynamic", sphereForGP);
			String sphereForDI = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(brt2, writerTask,
					dataO, readerDI, alreadyChosenVoterGP);
			Assert.assertEquals("Weak-Dynamic", sphereForDI);
			String sphereForRS = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(brt2, writerTask,
					dataO, readerRS, alreadyChosenVoterGP);
			Assert.assertEquals("Static", sphereForRS);
			String sphereForIC = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(brt2, writerTask,
					dataO, readerIC, alreadyChosenVoterGP);
			Assert.assertEquals("Static", sphereForIC);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Test
	public void testSphereOfParticipantsForBrt2WhenDIChosenForBrt1() {

		try {
			API a2 = new API(pathToFile, cost, costForAddingReaderAfterBrt);
			// for brt2 the lastWriter is the writer 2!!!

			BPMNTask writerTask = (BPMNTask) a2.getNodeById("Task_0zvbv9t");
			BPMNBusinessRuleTask brt2 = (BPMNBusinessRuleTask) a2.getNodeById("Task_02f5d5q");
			BPMNDataObject dataO = writerTask.getDataObjects().get(0);

			BPMNParticipant readerGP = new BPMNParticipant("Lane_155vkn8", "GP");
			BPMNParticipant readerDI = new BPMNParticipant("Lane_190r1hy", "DI");
			BPMNParticipant readerRS = new BPMNParticipant("Lane_07pzlbb", "RS");
			BPMNParticipant readerIC = new BPMNParticipant("Lane_0m888yn", "IC");

			// Let the voter for Brt1 be the DI
			HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> alreadyChosenVoterDI = new HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>>();
			LinkedList<BPMNParticipant> voterForBrt1DI = new LinkedList<BPMNParticipant>();
			voterForBrt1DI.add(readerDI);
			alreadyChosenVoterDI.put((BPMNBusinessRuleTask) a2.getNodeById("BusinessRuleTask_1qnt4kf"), voterForBrt1DI);
			String sphereForGP = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(brt2, writerTask,
					dataO, readerGP, alreadyChosenVoterDI);
			Assert.assertEquals("Strong-Dynamic", sphereForGP);
			String sphereForDI = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(brt2, writerTask,
					dataO, readerDI, alreadyChosenVoterDI);
			Assert.assertEquals("Weak-Dynamic", sphereForDI);
			String sphereForRS = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(brt2, writerTask,
					dataO, readerRS, alreadyChosenVoterDI);
			Assert.assertEquals("Static", sphereForRS);
			String sphereForIC = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(brt2, writerTask,
					dataO, readerIC, alreadyChosenVoterDI);
			Assert.assertEquals("Static", sphereForIC);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Test
	public void testSphereOfParticipantsForBrt2WhenRSChosenForBrt1() {

		try {
			API a2 = new API(pathToFile, cost, costForAddingReaderAfterBrt);
			// for brt2 the lastWriter is the writer 2!!!

			BPMNTask writerTask = (BPMNTask) a2.getNodeById("Task_0zvbv9t");
			BPMNBusinessRuleTask brt2 = (BPMNBusinessRuleTask) a2.getNodeById("Task_02f5d5q");
			BPMNDataObject dataO = writerTask.getDataObjects().get(0);

			BPMNParticipant readerGP = new BPMNParticipant("Lane_155vkn8", "GP");
			BPMNParticipant readerDI = new BPMNParticipant("Lane_190r1hy", "DI");
			BPMNParticipant readerRS = new BPMNParticipant("Lane_07pzlbb", "RS");
			BPMNParticipant readerIC = new BPMNParticipant("Lane_0m888yn", "IC");

			// Let the voter for Brt1 be the RS
			HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> alreadyChosenVoterRS = new HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>>();
			LinkedList<BPMNParticipant> voterForBrt1RS = new LinkedList<BPMNParticipant>();
			voterForBrt1RS.add(readerRS);
			alreadyChosenVoterRS.put((BPMNBusinessRuleTask) a2.getNodeById("BusinessRuleTask_1qnt4kf"), voterForBrt1RS);
			String sphereForGP = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(brt2, writerTask,
					dataO, readerGP, alreadyChosenVoterRS);
			Assert.assertEquals("Strong-Dynamic", sphereForGP);
			String sphereForDI = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(brt2, writerTask,
					dataO, readerDI, alreadyChosenVoterRS);
			Assert.assertEquals("Weak-Dynamic", sphereForDI);
			String sphereForRS = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(brt2, writerTask,
					dataO, readerRS, alreadyChosenVoterRS);
			Assert.assertEquals("Static", sphereForRS);
			String sphereForIC = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(brt2, writerTask,
					dataO, readerIC, alreadyChosenVoterRS);
			Assert.assertEquals("Static", sphereForIC);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Test
	public void testSphereOfParticipantsForBrt2WhenICChosenForBrt1() {

		try {
			API a2 = new API(pathToFile, cost, costForAddingReaderAfterBrt);
			// for brt2 the lastWriter is the writer 2!!!

			BPMNTask writerTask = (BPMNTask) a2.getNodeById("Task_0zvbv9t");
			BPMNBusinessRuleTask brt2 = (BPMNBusinessRuleTask) a2.getNodeById("Task_02f5d5q");
			BPMNDataObject dataO = writerTask.getDataObjects().get(0);

			BPMNParticipant readerGP = new BPMNParticipant("Lane_155vkn8", "GP");
			BPMNParticipant readerDI = new BPMNParticipant("Lane_190r1hy", "DI");
			BPMNParticipant readerRS = new BPMNParticipant("Lane_07pzlbb", "RS");
			BPMNParticipant readerIC = new BPMNParticipant("Lane_0m888yn", "IC");

			// Let the voter for Brt1 be the IC
			HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> alreadyChosenVoterIC = new HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>>();
			LinkedList<BPMNParticipant> voterForBrt1IC = new LinkedList<BPMNParticipant>();
			voterForBrt1IC.add(readerIC);
			alreadyChosenVoterIC.put((BPMNBusinessRuleTask) a2.getNodeById("BusinessRuleTask_1qnt4kf"), voterForBrt1IC);
			String sphereForGP = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(brt2, writerTask,
					dataO, readerGP, alreadyChosenVoterIC);
			Assert.assertEquals("Strong-Dynamic", sphereForGP);
			String sphereForDI = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(brt2, writerTask,
					dataO, readerDI, alreadyChosenVoterIC);
			Assert.assertEquals("Weak-Dynamic", sphereForDI);
			String sphereForRS = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(brt2, writerTask,
					dataO, readerRS, alreadyChosenVoterIC);
			Assert.assertEquals("Static", sphereForRS);
			String sphereForIC = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(brt2, writerTask,
					dataO, readerIC, alreadyChosenVoterIC);
			Assert.assertEquals("Static", sphereForIC);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
