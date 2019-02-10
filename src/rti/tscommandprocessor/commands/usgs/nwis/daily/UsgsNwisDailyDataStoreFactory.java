// UsgsNwisDailyDataStoreFactory - Factory to instantiate UsgsNwisDailyDataStore instances.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2019 Colorado Department of Natural Resources

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

package rti.tscommandprocessor.commands.usgs.nwis.daily;

import java.net.URI;

import RTi.Util.IO.PropList;
import riverside.datastore.DataStore;
import riverside.datastore.DataStoreFactory;

/**
 * Factory to instantiate UsgsNwisDailyDataStore instances.
 * @author sam
 *
 */
public class UsgsNwisDailyDataStoreFactory implements DataStoreFactory
{

/**
Create a UsgsNwisDailyDataStore instance.
*/
public DataStore create ( PropList props )
{
    String name = props.getValue ( "Name" );
    String description = props.getValue ( "Description" );
    if ( description == null ) {
        description = "";
    }
    String serviceRootURI = props.getValue ( "ServiceRootURI" );
    try {
        UsgsNwisDailyDataStore dataStore =
            new UsgsNwisDailyDataStore ( name, description, new URI(serviceRootURI) );
        dataStore.setProperties ( props );
        return dataStore;
    }
    catch ( Exception e ) {
        throw new RuntimeException ( e );
    }
}

}
