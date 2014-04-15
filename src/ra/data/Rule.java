package ra.data;

import java.util.ArrayList;
import java.util.List;


public class Rule {
	private Itemset antecedent;
	private Itemset consequent;

	/**
	 * Empty-parameter constructor
	 */
	public Rule() {
		this.antecedent = new Itemset();
		this.consequent = new Itemset();
	}

	/**
	 * Constructor
	 * @param antecedent The antecedent of the rule
	 * @param consequent The consequent of the rule
	 */
	public Rule(Itemset antecedent, Itemset consequent) {
		this.antecedent = antecedent;
		this.consequent = consequent;
	}

	/**
	 * 
	 * @param transactions The transactions associated with the rule
	 * @return The confidence of the rule
	 */
	public double calcConfidence(Database database) {
		List<Itemset> itemsets = new ArrayList<Itemset>();
		
		// Itemset formation to compute rule confidence
		Itemset numerator = this.antecedent.getUnion(this.consequent);
		
		// Support confidence
		itemsets.add(numerator);
		itemsets.add(this.antecedent);
		database.calcSupport(itemsets);
		
		return numerator.getSupport()*1.0/this.antecedent.getSupport();
	}
	
	/**
	 * Accessor to the antecedent
	 * @return The antecedent
	 */
	public Itemset getAntecedent() {
		return this.antecedent;
	}
	
	/**
	 * Accessor to the consequent
	 * @return The consequent
	 */
	public Itemset getConsequent() {
		return this.consequent;
	}

	/**
	 * @param value The value to add to the rule as antecedent
	 */
	public void addToAntecedent(Item value) {
		this.antecedent.add(value);
	}

	/**
	 * @param value The value to add to the rule as consequent
	 */
	public void addToConsequent(Item value) {
		this.consequent.add(value);
	}

	/**
	 * Derive the k+1 consequent rules from a given k consequent rule by transferring
	 * each antecedent item as consequent
	 * @return The derived rules
	 */
	public List<Rule> deriveRules() {
		List<Rule> derivedRules = new ArrayList<Rule>();

		// The rule must have at least an antecedent of size two to derive rules
		if(this.antecedent.size() >= 2) {
			for(Item i: this.antecedent.getItems()) {
				Itemset derivedAntecedent = this.antecedent.clone();
				Itemset derivedConsequent = this.consequent.clone();
				derivedAntecedent.remove(i);
				derivedConsequent.add(i);

				derivedRules.add(new Rule(derivedAntecedent, derivedConsequent));
			}
		}

		return derivedRules;
	}

	@Override
	public String toString() {
		String rule = "Rule: ";
		for(Item item: this.antecedent.getItems()) {
			rule += item+" ";
		}
		rule += "-> ";
		for(Item item: this.consequent.getItems()) {
			rule += item+" ";
		}
		return rule;
	}
}
