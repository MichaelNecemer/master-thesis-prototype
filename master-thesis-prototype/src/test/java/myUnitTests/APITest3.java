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
public class APITest3 {
	
	@Test
	public void testAmountPossibleProcessInstances() {
		ArrayList<Double> cost = new ArrayList<>(Arrays.asList(10.0, 5.0, 3.0, 2.0));

		try {
			API a2 = new API("C:\\Users\\Micha\\OneDrive\\Desktop\\modelle\\algorithmTest.bpmn", cost);
			//4 voters for Brt1
			//6 voters for Brt2
			//6 voters for Brt3
			//4 voters for Brt4
			//a processInstance contains a certain assignment of possible voters for each brt
			//4*6*6*4 possible combinations = 576 possible combinations
			 
			
			int expectedAmountPossibleCombinations = 576;
		
			Assert.assertEquals(expectedAmountPossibleCombinations,a2.getAmountPossibleCombinationsOfParticipants(), 0);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


	
	
}
