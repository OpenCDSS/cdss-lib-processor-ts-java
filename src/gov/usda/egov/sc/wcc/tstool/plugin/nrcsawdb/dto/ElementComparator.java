package gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto;

import java.util.Comparator;

/**
 * Comparator for Collections.sort to sort Element, currently by name.
 */
public class ElementComparator implements Comparator<Element> {

	/**
	 * Constructor.
	 */
	public ElementComparator () {
	}
	
	/**
	 * If elementA is < elementB, return -1.
	 * If elementA = elementB, return 0.
	 * If elementA is > elementB, return 1
	 */
	public int compare(Element elementA, Element elementB) {
		String nameA = elementA.getName();
		String nameB = elementB.getName();

		return nameA.compareToIgnoreCase(nameB);
	}
}