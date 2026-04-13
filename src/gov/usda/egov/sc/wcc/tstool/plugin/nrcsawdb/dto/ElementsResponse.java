package gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The ElementsResponse class matches the `elements` service response,
 * which is an array of Element.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class ElementsResponse {

	/**
	 * List of Element data objects.
	 */
	private List<Element> elements = new ArrayList<>();

	/**
	 * Constructor needed by Jackson.
	 */
	public ElementsResponse () {
	}

	/**
	 * Return the list of Element objects.
	 * @return the list of Element objects
	 */
	public List<Element> getElements () {
		return this.elements;
	}
}