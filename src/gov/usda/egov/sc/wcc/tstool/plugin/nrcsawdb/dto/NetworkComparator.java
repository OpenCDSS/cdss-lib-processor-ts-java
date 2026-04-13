package gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto;

import java.util.Comparator;

/**
 * Comparator for Collections.sort to sort Network, currently by code.
 */
public class NetworkComparator implements Comparator<Network> {

	/**
	 * Constructor.
	 */
	public NetworkComparator () {
	}
	
	/**
	 * If networkA is < networkB, return -1.
	 * If networkA = networkB, return 0.
	 * If networkA is > networkB, return 1
	 */
	public int compare(Network networkA, Network networkB) {
		String nameA = networkA.getName();
		String nameB = networkB.getName();

		return nameA.compareTo(nameB);
	}
}