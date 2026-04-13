package gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.util;

/**
 * Useful web utility methods.
 */
public class WebUtil {

	/**
	 * Append a URL query parameter to the URL, automatically adding ? and &.
	 * @param urlString URL string builder to update.
	 */
	public static void appendUrlQueryParameter ( StringBuilder urlString, String paramName, String paramValue ) {
		if ( urlString.toString().contains("?") ) {
			// ? was found so need to add & in front of the query parameter
			urlString.append("&");
		}
		else {
			// ? was not found so need to add ? in front of the query parameter
			urlString.append("?");
		}
		urlString.append(paramName);
		urlString.append("=");
		// TODO smalers 2020-01-24 evaluate whether need to escape any characters, etc.
		urlString.append(paramValue);
	}

}