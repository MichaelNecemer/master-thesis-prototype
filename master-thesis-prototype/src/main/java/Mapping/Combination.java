package Mapping;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Combination {
	//we want to use the binomial coefficient to get the different combinations for the voters
	//e.g. we have 4 Voters (A,B,C,D) in the global sphere and need to get 3 of them for the first brt
	// -> get all the possible combinations/permutation without repetition (A,B,C), (A,B,D), (A,C,D), (B,C,D)
	public static<T> LinkedList<LinkedList<T>> getPermutations (LinkedList<T> elements, int k) {
	    return getPermutations (elements,k,0);
	}

	public static<T> LinkedList<LinkedList<T>> getPermutations (LinkedList<T> elements, int k, int i) {
	    LinkedList<LinkedList<T>> results = new LinkedList<>();
	    if(k > 0) {
	        int n = elements.size();
	        for(int j = i; j <= n-k; j++) {
	            T val = elements.get(j);
	            LinkedList<LinkedList<T>> tails = getPermutations(elements,k-1,j+1);
	            for(LinkedList<T> tail : tails) {
	                LinkedList<T> result = new LinkedList<>();
	                result.add(val);
	                result.addAll(tail);
	                results.add(result);
	            }
	        }
	    } else {
	        results.add(new LinkedList<T>());
	    }
	    return results;
	}
	
	/**
	 * Combines several collections of elements and create permutations of all of them, taking one element from each
	 * collection, and keeping the same order in resultant lists as the one in original list of collections.
	 * 
	 * <ul>Example
	 * <li>Input  = { {a,b,c} , {1,2,3,4} }</li>
	 * <li>Output = { {a,1} , {a,2} , {a,3} , {a,4} , {b,1} , {b,2} , {b,3} , {b,4} , {c,1} , {c,2} , {c,3} , {c,4} }</li>
	 * </ul>
	 * 
	 * @param brtCombs Original list of collections which elements have to be combined.
	 * @return Resultant collection of lists with all permutations of original list.
	 */
	public static <T> Collection<List<T>> permutations(List<LinkedList<BrtToBrtArc>> brtCombs) {
	  if (brtCombs == null || brtCombs.isEmpty()) {
	    return Collections.emptyList();
	  } else {
	    Collection<List<T>> res = new LinkedList<>();
	    permutationsImpl(brtCombs, res, 0, new LinkedList<T>());
	    return res;
	  }
	}

	
	private static <T> void permutationsImpl(List<LinkedList<BrtToBrtArc>> brtCombs, Collection<List<T>> res, int d, List<T> copy2) {
	  // if depth equals number of original collections, final reached, add and return
	  if (d == brtCombs.size()) {
	    res.add(copy2);
	    return;
	  }

	  // iterate from current collection and copy 'current' element N times, one for each element
	  Collection<T> currentCollection = (Collection<T>) brtCombs.get(d);
	  for (T element : currentCollection) {
	    List<T> copy = new LinkedList<>(copy2);
	    copy.add(element);
	    permutationsImpl(brtCombs, res, d + 1, copy);
	  }
	}
}
