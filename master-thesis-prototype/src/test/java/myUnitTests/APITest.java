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
import Mapping.BPMNParticipant;
import Mapping.BPMNTask;
import functionality.API;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class APITest {

	@Test
	public void testAllLanesAsParticipantsInGlobalSphere() {
		ArrayList<Double> cost = new ArrayList<>(Arrays.asList(10.0, 5.0, 3.0, 2.0));

		try {
			API a2 = new API("C:\\Users\\Micha\\OneDrive\\Desktop\\modelle\\brtsIn2branches1.bpmn", cost);
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
		ArrayList<Double> cost = new ArrayList<>(Arrays.asList(10.0, 5.0, 3.0, 2.0));

		try {
			API a2 = new API("C:\\Users\\Micha\\OneDrive\\Desktop\\modelle\\brtsIn2branches1.bpmn", cost);

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

		ArrayList<Double> cost = new ArrayList<>(Arrays.asList(10.0, 5.0, 3.0, 2.0));

		try {
			API a2 = new API("C:\\Users\\Micha\\OneDrive\\Desktop\\modelle\\brtsIn2branches1.bpmn", cost);

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

		ArrayList<Double> cost = new ArrayList<>(Arrays.asList(10.0, 5.0, 3.0, 2.0));

		try {
			API a2 = new API("C:\\Users\\Micha\\OneDrive\\Desktop\\modelle\\brtsIn2branches1.bpmn", cost);

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
		ArrayList<Double> cost = new ArrayList<>(Arrays.asList(10.0, 5.0, 3.0, 2.0));

		try {
			API a2 = new API("C:\\Users\\Micha\\OneDrive\\Desktop\\modelle\\brtsIn2branches1.bpmn", cost);

			BPMNTask writerTask = (BPMNTask) a2.getNodeById("Task_00qdu1v");
			BPMNDataObject dataO = writerTask.getDataObjects().get(0);
			BPMNParticipant readerDI = new BPMNParticipant("Lane_190r1hy", "DI");
			BPMNParticipant readerGP = new BPMNParticipant("Lane_155vkn8", "GP");
			BPMNParticipant readerRS = new BPMNParticipant("Lane_07pzlbb", "RS");
			BPMNParticipant readerIC = new BPMNParticipant("Lane_0m888yn", "IC");

			HashMap<Boolean, LinkedList<LinkedList<BPMNElement>>> effectivePaths = a2.allEffectivePathsForWriters(dataO,
					writerTask, writerTask, a2.getBpmnEnd(), new LinkedList<BPMNElement>(),
					new LinkedList<BPMNElement>(), new LinkedList<BPMNElement>(),
					new LinkedList<LinkedList<BPMNElement>>());

			String sphereForDI = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(writerTask, dataO,
					readerDI, effectivePaths, new HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>>());
			Assert.assertEquals("Static", sphereForDI);
			String sphereForGP = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(writerTask, dataO,
					readerGP, effectivePaths, new HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>>());
			Assert.assertEquals("Static", sphereForGP);
			String sphereForRS = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(writerTask, dataO,
					readerRS, effectivePaths, new HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>>());
			Assert.assertEquals("Strong-Dynamic", sphereForRS);
			String sphereForIC = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(writerTask, dataO,
					readerIC, effectivePaths, new HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>>());
			Assert.assertEquals("Weak-Dynamic", sphereForIC);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	
	@Test
	public void testSphereOfReaderParticipantsForWriter2ForBrt2() {
		ArrayList<Double> cost = new ArrayList<>(Arrays.asList(10.0, 5.0, 3.0, 2.0));

		try {
			API a2 = new API("C:\\Users\\Micha\\OneDrive\\Desktop\\modelle\\brtsIn2branches1.bpmn", cost);

			BPMNTask writerTask = (BPMNTask) a2.getNodeById("Task_0zvbv9t");
			BPMNDataObject dataO = writerTask.getDataObjects().get(0);
			BPMNParticipant readerDI = new BPMNParticipant("Lane_190r1hy", "DI");
			BPMNParticipant readerGP = new BPMNParticipant("Lane_155vkn8", "GP");
			BPMNParticipant readerRS = new BPMNParticipant("Lane_07pzlbb", "RS");
			BPMNParticipant readerIC = new BPMNParticipant("Lane_0m888yn", "IC");

			HashMap<Boolean, LinkedList<LinkedList<BPMNElement>>> effectivePaths = a2.allEffectivePathsForWriters(dataO,
					writerTask, writerTask, a2.getBpmnEnd(), new LinkedList<BPMNElement>(),
					new LinkedList<BPMNElement>(), new LinkedList<BPMNElement>(),
					new LinkedList<LinkedList<BPMNElement>>());
			
			//this testcase only checks for spheres without considering already chosen participants for brt1
			String sphereForDI = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(writerTask, dataO,
					readerDI, effectivePaths, new HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>>());
			Assert.assertEquals("Weak-Dynamic", sphereForDI);
			String sphereForGP = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(writerTask, dataO,
					readerGP, effectivePaths, new HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>>());
			Assert.assertEquals("Strong-Dynamic", sphereForGP);
			String sphereForRS = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(writerTask, dataO,
					readerRS, effectivePaths, new HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>>());
			Assert.assertEquals("Static", sphereForRS);
			String sphereForIC = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(writerTask, dataO,
					readerIC, effectivePaths, new HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>>());
			Assert.assertEquals("Static", sphereForIC);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	
	
	
	
	

	@Test
	public void testSphereOfGPForBrt3WhenGPChosenForBrt1() {
		ArrayList<Double> cost = new ArrayList<>(Arrays.asList(10.0, 5.0, 3.0, 2.0));

		try {
			API a2 = new API("C:\\Users\\Micha\\OneDrive\\Desktop\\modelle\\brtsIn2branches1.bpmn", cost);

			BPMNTask writerTask = (BPMNTask) a2.getNodeById("Task_00qdu1v");
			BPMNDataObject dataO = writerTask.getDataObjects().get(0);

			BPMNParticipant readerGP = new BPMNParticipant("Lane_155vkn8", "GP");

			HashMap<Boolean, LinkedList<LinkedList<BPMNElement>>> effectivePaths = a2.allEffectivePathsForWriters(dataO,
					writerTask, writerTask, a2.getBpmnEnd(), new LinkedList<BPMNElement>(),
					new LinkedList<BPMNElement>(), new LinkedList<BPMNElement>(),
					new LinkedList<LinkedList<BPMNElement>>());
			// consider there is already a voter for brt1!!!

			// Let the voter for Brt1 be the GP
			HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> alreadyChosenVoterGP = new HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>>();
			LinkedList<BPMNParticipant> voterForBrt1GP = new LinkedList<BPMNParticipant>();
			voterForBrt1GP.add(readerGP);
			alreadyChosenVoterGP.put((BPMNBusinessRuleTask) a2.getNodeById("BusinessRuleTask_1qnt4kf"), voterForBrt1GP);
			String sphereForGP = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(writerTask, dataO,
					readerGP, effectivePaths, alreadyChosenVoterGP);
			Assert.assertEquals("Strong-Dynamic", sphereForGP);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Test
	public void testSphereOfDIForBrt3WhenGPChosenForBrt1() {
		ArrayList<Double> cost = new ArrayList<>(Arrays.asList(10.0, 5.0, 3.0, 2.0));

		try {
			API a2 = new API("C:\\Users\\Micha\\OneDrive\\Desktop\\modelle\\brtsIn2branches1.bpmn", cost);

			BPMNTask writerTask = (BPMNTask) a2.getNodeById("Task_00qdu1v");
			BPMNDataObject dataO = writerTask.getDataObjects().get(0);

			BPMNParticipant readerGP = new BPMNParticipant("Lane_155vkn8", "GP");
			BPMNParticipant readerDI = new BPMNParticipant("Lane_190r1hy", "DI");

			HashMap<Boolean, LinkedList<LinkedList<BPMNElement>>> effectivePaths = a2.allEffectivePathsForWriters(dataO,
					writerTask, writerTask, a2.getBpmnEnd(), new LinkedList<BPMNElement>(),
					new LinkedList<BPMNElement>(), new LinkedList<BPMNElement>(),
					new LinkedList<LinkedList<BPMNElement>>());
			// consider there is already a voter for brt1!!!

			// Let the voter for Brt1 be the GP
			HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> alreadyChosenVoterGP = new HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>>();
			LinkedList<BPMNParticipant> voterForBrt1GP = new LinkedList<BPMNParticipant>();
			voterForBrt1GP.add(readerGP);
			alreadyChosenVoterGP.put((BPMNBusinessRuleTask) a2.getNodeById("BusinessRuleTask_1qnt4kf"), voterForBrt1GP);
			String sphereForDI = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(writerTask, dataO,
					readerDI, effectivePaths, alreadyChosenVoterGP);
			Assert.assertEquals("Static", sphereForDI);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	
	@Test
	public void testSphereOfRSForBrt3WhenGPChosenForBrt1() {
		ArrayList<Double> cost = new ArrayList<>(Arrays.asList(10.0, 5.0, 3.0, 2.0));

		try {
			API a2 = new API("C:\\Users\\Micha\\OneDrive\\Desktop\\modelle\\brtsIn2branches1.bpmn", cost);

			BPMNTask writerTask = (BPMNTask) a2.getNodeById("Task_00qdu1v");
			BPMNDataObject dataO = writerTask.getDataObjects().get(0);

			BPMNParticipant readerGP = new BPMNParticipant("Lane_155vkn8", "GP");
			BPMNParticipant readerRS = new BPMNParticipant("Lane_07pzlbb", "RS");

			HashMap<Boolean, LinkedList<LinkedList<BPMNElement>>> effectivePaths = a2.allEffectivePathsForWriters(dataO,
					writerTask, writerTask, a2.getBpmnEnd(), new LinkedList<BPMNElement>(),
					new LinkedList<BPMNElement>(), new LinkedList<BPMNElement>(),
					new LinkedList<LinkedList<BPMNElement>>());
			// consider there is already a voter for brt1!!!

			// Let the voter for Brt1 be the GP
			HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> alreadyChosenVoterGP = new HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>>();
			LinkedList<BPMNParticipant> voterForBrt1GP = new LinkedList<BPMNParticipant>();
			voterForBrt1GP.add(readerGP);
			alreadyChosenVoterGP.put((BPMNBusinessRuleTask) a2.getNodeById("BusinessRuleTask_1qnt4kf"), voterForBrt1GP);
			String sphereForRS = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(writerTask, dataO,
					readerRS, effectivePaths, alreadyChosenVoterGP);
			Assert.assertEquals("Strong-Dynamic", sphereForRS);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		

	}
	
	@Test
	public void testSphereOfICForBrt3WhenGPChosenForBrt1() {
		ArrayList<Double> cost = new ArrayList<>(Arrays.asList(10.0, 5.0, 3.0, 2.0));

		try {
			API a2 = new API("C:\\Users\\Micha\\OneDrive\\Desktop\\modelle\\brtsIn2branches1.bpmn", cost);

			BPMNTask writerTask = (BPMNTask) a2.getNodeById("Task_00qdu1v");
			BPMNDataObject dataO = writerTask.getDataObjects().get(0);

			BPMNParticipant readerGP = new BPMNParticipant("Lane_155vkn8", "GP");
			BPMNParticipant readerIC = new BPMNParticipant("Lane_0m888yn", "IC");
			
			HashMap<Boolean, LinkedList<LinkedList<BPMNElement>>> effectivePaths = a2.allEffectivePathsForWriters(dataO,
					writerTask, writerTask, a2.getBpmnEnd(), new LinkedList<BPMNElement>(),
					new LinkedList<BPMNElement>(), new LinkedList<BPMNElement>(),
					new LinkedList<LinkedList<BPMNElement>>());
			// consider there is already a voter for brt1!!!

			// Let the voter for Brt1 be the GP
			HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>> alreadyChosenVoterGP = new HashMap<BPMNBusinessRuleTask, LinkedList<BPMNParticipant>>();
			LinkedList<BPMNParticipant> voterForBrt1GP = new LinkedList<BPMNParticipant>();
			voterForBrt1GP.add(readerGP);
			alreadyChosenVoterGP.put((BPMNBusinessRuleTask) a2.getNodeById("BusinessRuleTask_1qnt4kf"), voterForBrt1GP);
			String sphereForIC = a2.getSphereForParticipantOnEffectivePathsWithAlreadyChosenVoters(writerTask, dataO,
					readerIC, effectivePaths, alreadyChosenVoterGP);
			Assert.assertEquals("Weak-Dynamic", sphereForIC);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		

	}
	
	

}
