// ReclamationPiscesDataStore - Data store for Reclamation Pisces database.

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

package rti.tscommandprocessor.commands.reclamationpisces;

import RTi.DMI.AbstractDatabaseDataStore;
import RTi.DMI.DMI;
import RTi.Util.Message.Message;
import RTi.Util.Time.DateTime;

/**
Data store for Reclamation Pisces database.  This class maintains the database connection information in a general way.
@author sam
*/
public class ReclamationPiscesDataStore extends AbstractDatabaseDataStore
{

/**
Construct a data store given a DMI instance, which is assumed to be open.
@param name identifier for the data store
@param description name for the data store
@param dmi DMI instance to use for the data store.
*/
public ReclamationPiscesDataStore ( String name, String description, DMI dmi )
{
    setName ( name );
    setDescription ( description );
    setDMI ( dmi );
}

/**
Check the database connection.  Sometimes the connection gets dropped due to timeout from inaction.
A simple, fast query is run and if it fails the connection is re-established.
It is assumed that the DMI instance has been populated with data that can be used for the connection.
Although this method could be called near low-level database statement calls (for example in DMI read/write
methods), for performance it is probably best to call at a higher level before a group of database
statements are executed.
@return true if the connection could be established, false if not.
*/
public boolean checkDatabaseConnection ()
{
	int retries = 5;
	for ( int i = 0; i < retries; i++ ) {
		DMI dmi = getDMI();
		try {
			if ( dmi == null ) {
				// Datastore was never initialized properly when software started.
				// This is a bigger problem than can be fixed here.
				return false;
			}
			ReclamationPiscesDMI rdmi = (ReclamationPiscesDMI)dmi;
			rdmi.dmiSelect("SELECT * from piscesinfo");
			// If here the connection is in place and the query was successful
			return true;
		}
		catch ( Exception e ) {
			// Error running query so try to (re)open the dmi
			Message.printWarning(3, "", e);
			try {
				dmi.open();
				// If no exception it was successful, but make sure query is OK so go to the top of the loop again
				setStatus(0);
				DateTime now = new DateTime(DateTime.DATE_CURRENT);
				setStatusMessage("Database connection automatically reopened at " + now );
				continue;
			}
			catch ( Exception e2 ) {
				// Failed to open - try again until max retries is over
				Message.printWarning(3, "", e);
				continue;
			}
		}
	}
	// Could not establish the connection even with retries.
	return false;
}

}
