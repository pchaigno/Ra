package ra.algo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import ra.data.Database;
import ra.data.Item;
import ra.data.Itemset;
import ra.data.ItemsetWithoutSupportException;
import ra.data.Rule;

public class APriori {
	protected Database database;
	protected List<List<Itemset>> itemsets;
	private List<List<Rule>> rules;
	// If false computes the support in two shots.
	protected boolean calcSupportTwoShots;
	// If true computes the support entirely.
	protected boolean calcSupportComplitely;
	
	/**
	 * Constructor
	 * @param database The database containing the transactions.
	 * @param calcSupportComplitely If true computes the support complitely.
	 */
	public APriori(Database database, boolean calcSupportComplitely) {
		this.database = database;
		this.itemsets = new ArrayList<List<Itemset>>();
		this.rules = new ArrayList<List<Rule>>();
		this.calcSupportTwoShots = true;
		this.calcSupportComplitely = calcSupportComplitely;
	}
	
	/**
	 * A Priori algorithm to compute the k-itemsets.
	 * @param minSupport The minimum support to keep an itemset.
	 * @return All the computed k-itemsets.
	 */
	public List<List<Itemset>> aPriori(int minSupport) {
		// Initiates with the 1-itemsets.
		this.calc1Itemset(minSupport);
		
		// Loops to create all others itemsets:
		boolean noMoreItemsets = false;
		for(int i=1; !noMoreItemsets; i++) {
			List<Itemset> newItemsets = this.calcK1Itemset(itemsets.get(i-1), minSupport);
			if(newItemsets.size() == 0) {
				noMoreItemsets = true;
			} else {
				this.itemsets.add(newItemsets);
			}
		}
		
		// If the support calculation was incomplete and we want a complete support
		// we need to update it:
		if(this.calcSupportComplitely) {
			updateSupport();
		}
		
		return this.itemsets;
	}
	
	/**
	 * Updates the support of the itemsets found.
	 * This can be usefull for maximum and closed itemsets because we don't compute completly the support the first time.
	 * Indeed, some of the itemsets on which we compute the support will then be removed (non-closed or non-maximum).
	 */
	private void updateSupport() {
		for(int level=0; level<this.itemsets.size(); level++) {
			this.database.updateSupport(this.itemsets.get(level));
		}
	}
	
	/**
	 * Computes the 1-itemsets from the transactions.
	 * @param minSupport The minimum support to keep an itemset.
	 */
	private void calc1Itemset(int minSupport) {
		Set<Item> items = this.database.retrieveItems();
		
		// Generates the 1-itemsets:
		List<Itemset> itemsets = new Vector<Itemset>();
		for(Item item: items) {
			Itemset itemset = new Itemset();
			itemset.add(item);
			itemsets.add(itemset);
		}
		
		// Checks the support of all itemsets:
		boolean completeSupportCalc = !this.calcSupportTwoShots && !this.calcSupportComplitely;
		List<Itemset> frequentItemsets = this.database.withMinSupport(itemsets, minSupport, completeSupportCalc);
		
		this.itemsets.add(frequentItemsets);
	}
	
	/**
	 * Computes the k+1-itemsets from the k-itemsets.
	 * @param itemsets The k-itemsets.
	 * @param minSupport The minimum support to keep a k+1-itemset.
	 * @return The k+1-itemsets.
	 */
	private List<Itemset> calcK1Itemset(List<Itemset> itemsets, int minSupport) {
		List<Itemset> candidates = new Vector<Itemset>();
		
		// Generates candidates of size k+1 for k-itemsets:
		for(int i=0; i<itemsets.size(); i++) {
			for(int j=i+1; j<itemsets.size(); j++) {
				candidates.addAll(itemsets.get(i).calcItemsetsK1(itemsets.get(j)));
			}
		}
		
		// Checks that all subsets of each candidate are frequent:
		for(int i=0; i<candidates.size(); i++) {
			if(!allSubItemsetsFrequent(itemsets, candidates.get(i))) {
				candidates.remove(i);
			}
		}
		
		// Checks support for all candidates:
		boolean completeSupportCalc = !this.calcSupportTwoShots && !this.calcSupportComplitely;
		List<Itemset> frequentItemsets = this.database.withMinSupport(candidates, minSupport, completeSupportCalc);
		
		return frequentItemsets;
	}
	
	/**
	 * Checks that all k-itemsets of a k+1-itemset are frequent.
	 * @param itemsets The k-itemsets.
	 * @param itemset The k+1-itemsets.
	 * @return True if all k-itemsets of the k+1-itemsets are frequent.
	 */
	private static boolean allSubItemsetsFrequent(List<Itemset> itemsets, Itemset itemset) {
		List<Itemset> subItemsets = itemset.calcSubItemsets();
		for(Itemset subItemset: subItemsets) {
			if(!itemsets.contains(subItemset)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Generates the rules associated with itemsets generated by the first step of Apriori
	 * @param minConfidence The required minimum confidence to keep rules during the process
	 * @return The generated rules
	 */
	public List<Rule> generateRules(double minConfidence) {
		// Initialites the rules from the frequent itemsets:
		this.generateSimpleRules(minConfidence);
		
		// Loops to generate all others rules:
		boolean noMoreRules = false;
		for(int i=1; !noMoreRules; i++) {
			List<Rule> newRules = this.generateDerivedRules(this.rules.get(i-1), minConfidence);
			if(newRules.size() == 0) {
				noMoreRules = true;
			} else {
				this.rules.add(newRules);
			}
		}
		
		// Join lists of rules:
		List<Rule> allRules = new ArrayList<Rule>();
		for(List<Rule> ruleList: this.rules) {
			allRules.addAll(ruleList);
		}
		return allRules;
	}
	
	/**
	 * Generates the simple rules from the frequent itemsets.
	 * @param minConfidence The minimum confidence for a rule to be valid.
	 */
	private void generateSimpleRules(double minConfidence) {
		// Generates every possible rules for each itemset:
		List<Rule> candidates = new ArrayList<Rule>();
		for(int level=1; level<this.itemsets.size(); level++) {
			for(Itemset itemset: this.itemsets.get(level)) {
				candidates.addAll(itemset.generateSimpleRules());
			}
		}
		
		// Computes the confidence.
		this.calcConfidence(candidates);
		
		// Removes rules with confidence below minimum:
		try  {
			for(int i=0; i<candidates.size(); i++) {
				if(candidates.get(i).getConfidence() < minConfidence) {
					candidates.remove(i);
					i--;
				}
			}
		} catch(ItemsetWithoutSupportException e) {
			System.err.println(e.getMessage());
		}
		
		this.rules.add(candidates);
	}
	
	/**
	 * Generates the derived rules from a specified set of rules.
	 * @param rules The set of rules.
	 * @param minConfidence The minimum confidence for a rule to be valid.
	 * @return The derived rules.
	 */
	private List<Rule> generateDerivedRules(List<Rule> rules, double minConfidence) {
		// Generates every possible derived rules from the specified rules:
		List<Rule> derivedRules = new ArrayList<Rule>();
		for(Rule rule: rules) {
			derivedRules.addAll(rule.deriveRules());
		}

		// Computes the confidence.
		this.calcConfidence(derivedRules);
		
		// Removes rules with confidence below minimum:
		try  {
			for(int i=0; i<derivedRules.size(); i++) {
				if(derivedRules.get(i).getConfidence() < minConfidence) {
					derivedRules.remove(i);
					i--;
				}
			}
		} catch(ItemsetWithoutSupportException e) {
			System.err.println(e.getMessage());
		}
		
		return derivedRules;
	}
	
	/**
	 * Computes the confidence for the numerators and antecedents of all specified rules.
	 * @param candidates The rules.
	 */
	private void calcConfidence(List<Rule> candidates) {
		List<Itemset> itemsets = new ArrayList<Itemset>();
		for(Rule rule: candidates) {
			itemsets.add(rule.getAntecedent());
			itemsets.add(rule.getNumerator());
		}
		this.database.calcSupport(itemsets);
	}
}