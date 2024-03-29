package mapping;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Combination {
	
	public static<T> LinkedList<LinkedList<T>> getPermutations (LinkedList<T> elements, int k) throws InterruptedException {
	    return getPermutations (elements,k,0);
	}
	
	public static<T> LinkedList<LinkedList<T>> getPermutationsWithBound (LinkedList<T> elements, int k, int bound) throws InterruptedException {
		if(bound<=0) {
			return getPermutations(elements, k, 0);
		} else {
		    return getPermutations (elements,k,0,bound);
		}
	}
	
	public static<T> LinkedList<LinkedList<T>> getPermutations (LinkedList<T> elements, int k, int i, int bound) {
	    LinkedList<LinkedList<T>> results = new LinkedList<>();
	    if(k > 0 && results.size()<bound) {
	        int n = elements.size();
	        for(int j = i; j <= n-k; j++) {
	            T val = elements.get(j);
	            LinkedList<LinkedList<T>> tails = getPermutations(elements,k-1,j+1, bound);
	            for(LinkedList<T> tail : tails) {
	            	if(results.size()<bound) {
	                LinkedList<T> result = new LinkedList<>();
	                result.add(val);
	                result.addAll(tail);
	                results.add(result);
	            	} else {
	            		return results;
	            	}
	            }
	        }
	    } else {
	        results.add(new LinkedList<T>());
	    }
	    return results;
	}


	public static<T> LinkedList<LinkedList<T>> getPermutations (LinkedList<T> elements, int k, int i) throws InterruptedException {
	    LinkedList<LinkedList<T>> results = new LinkedList<>();
	    if(k > 0) {
	        int n = elements.size();
	        for(int j = i; j <= n-k; j++) {
	            T val = elements.get(j);
	            LinkedList<LinkedList<T>> tails = getPermutations(elements,k-1,j+1);
	            for(LinkedList<T> tail : tails) {
	            	if (Thread.currentThread().isInterrupted()) {
	    				System.err.println("Interrupted! " + Thread.currentThread().getName());
	    				throw new InterruptedException();
	    			}
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
	 * @param toCombine Original list of collections which elements have to be combined.
	 * @return Resultant collection of lists with all permutations of original list.
	 * @throws InterruptedException 
	 */
	public static <T> Collection<List<T>> permutations(LinkedList<LinkedList<Object>> toCombine) throws InterruptedException {
	  if (toCombine == null || toCombine.isEmpty()) {
	    return Collections.emptyList();
	  } else {
	    Collection<List<T>> res = new LinkedList<>();
	    permutationsImpl(toCombine, res, 0, new LinkedList<T>());
	    return res;
	  }
	}

	
	private static <T> void permutationsImpl(LinkedList<LinkedList<Object>> toCombine, Collection<List<T>> res, int d, LinkedList<T> linkedList) throws InterruptedException {
	  // if depth equals number of original collections, final reached, add and return
	  if (d == toCombine.size()) {
	    res.add(linkedList);
	    return;
	  }
	  if (Thread.currentThread().isInterrupted()) {
			System.err.println("Interrupted! " + Thread.currentThread().getName());
			throw new InterruptedException();
		}

	  // iterate from current collection and copy 'current' element N times, one for each element
	  Collection<T> currentCollection = (Collection<T>) toCombine.get(d);
	  for (T element : currentCollection) {
	    List<T> copy = new LinkedList<>(linkedList);
	    copy.add(element);
	    permutationsImpl(toCombine, res, d + 1, (LinkedList<T>) copy);
	  }
	}
}
