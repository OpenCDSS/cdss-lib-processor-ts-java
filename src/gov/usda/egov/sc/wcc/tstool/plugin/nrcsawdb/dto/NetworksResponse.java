package gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The NetworksResponse class matches the `networks` service response,
 * which is an array of Network.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class NetworksResponse {

	/**
	 * List of Network data objects.
	 */
	private List<Network> networks = new ArrayList<>();

	/**
	 * Constructor needed by Jackson.
	 */
	public NetworksResponse () {
	}

	/**
	 * Return the list of Network objects.
	 * @return the list of Network objects
	 */
	public List<Network> getNetworks () {
		return this.networks;
	}
}