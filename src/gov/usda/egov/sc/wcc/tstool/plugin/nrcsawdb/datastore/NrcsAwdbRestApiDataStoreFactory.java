// NrcsAwdbRestApiDataStoreFactory - class to create a NrcsAwdbRestApiDataStore instance

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 2026 Colorado Department of Natural Resources

CDSS Time Series Processor Java Library is free software:  you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

CDSS Time Series Processor Java Library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

You should have received a copy of the GNU General Public License
    along with CDSS Time Series Processor Java Library.  If not, see <https://www.gnu.org/licenses/>.

NoticeEnd */

package gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.datastore;

import java.net.URI;

import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

import riverside.datastore.DataStore;
import riverside.datastore.DataStoreFactory;

public class NrcsAwdbRestApiDataStoreFactory implements DataStoreFactory {

	/**
	Create a NrcsAwdbRestApiDataStore instance.
	@param props datastore configuration properties, such as read from the configuration file
	*/
	public DataStore create ( PropList props ) {  
	    String name = props.getValue ( "Name" );
	    if ( (name == null) || name.isEmpty() ) {
	    	throw new RuntimeException("NrcsAwdbRestApiDataStore Name is not defined.");
	    }
	    String description = props.getValue ( "Description" );
	    if ( description == null ) {
	        description = "";
	    }
	    String serviceRootUrl = props.getValue ( "ServiceRootUrl" );
	    if ( (serviceRootUrl == null) || serviceRootUrl.isEmpty() ) {
	    	throw new RuntimeException("NrcsAwdbRestApiDataStore ServiceRootUrl is not defined.");
	    }
	    try {
	    	Message.printStatus(2, "", "ServiceRootUrl=\"" + serviceRootUrl + "\"");
	    	if ( ! serviceRootUrl.endsWith("/") ) {
	    		// Append a trailing slash:
	    		// - also reset in the original properties
	    		serviceRootUrl += "/";
	    		props.set("ServiceRootUrl", serviceRootUrl);
	    	}
	        DataStore ds = new NrcsAwdbRestApiDataStore ( name, description, new URI(serviceRootUrl), props );
	        return ds;
	    }
	    catch ( Exception e ) {
	        Message.printWarning(3,"",e);
	        throw new RuntimeException ( e );
	    }
	}
}