
package common_utils;

/**
 * Andrew G. West -  pair.java - Generic Pair implementation
 */
public class pair<F,S>{

	// **************************** PUBLIC FIELDS ****************************
	
	/**
	 * The first element of the pair.
	 */
	public F fst;
	
	/**
	 * The second element of the pair.
	 */
	public S snd;
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Constructor: Create a long_pair by providing both pair elements.
	 * @param fst First element of the pair.
	 * @param snd Second element of the pair.
	 */
	public pair(F fst, S snd){
		this.fst = fst;
		this.snd = snd;
	}
	
	/**
	 * OVERRIDING: Determine this pairs equivalence to another object.
	 */
	@SuppressWarnings("unchecked")
	public boolean equals(Object other){
		pair<F,S> other_pair = (pair<F,S>) other;		
		if(this.fst.equals(other_pair.fst) && this.snd == other_pair.snd)
			return true;
		else return false;
	}
	
	/**
	 * OVERRIDING: Output hash-code for this pair per hash-code contract.
	 */
	public int hashCode(){
		return (this.fst.hashCode() * this.snd.hashCode());
	}

}
