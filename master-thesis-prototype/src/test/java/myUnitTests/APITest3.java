package myUnitTests;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import functionality.API;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class APITest3 {
	ArrayList<Double> cost = new ArrayList<>(Arrays.asList(10.0, 5.0, 3.0, 2.0));
	String pathToFile = "C:\\Users\\Micha\\OneDrive\\Desktop\\modelle\\algorithmTest.bpmn";
	
	@Test
	public void testAmountPossibleProcessInstances() {
		

		try {
			API a2 = new API(pathToFile, cost);
			//4 voters for Brt1
			//6 voters for Brt2
			//6 voters for Brt3
			//4 voters for Brt4
			//a processInstance contains a certain assignment of possible voters for each brt
			//4*6*6*4 possible combinations = 576 possible combinations
			 
			
			int expectedAmountPossibleCombinations = 576;
		
			Assert.assertEquals(expectedAmountPossibleCombinations, Integer.parseInt(a2.getAmountPossibleCombinationsOfParticipants()), 0);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


	
	
}
