package rti.tscommandprocessor.commands.reclamationpisces;

import java.security.InvalidParameterException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import RTi.DMI.AbstractDatabaseDataStore;
import RTi.DMI.DMI;
import RTi.DMI.DMIUtil;
import RTi.TS.TS;
import RTi.TS.TSIdent;
import RTi.TS.TSUtil;
import RTi.Util.GUI.InputFilter_JPanel;
import RTi.Util.Message.Message;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;

/**
Data store for Reclamation Pisces database.  This class maintains the database connection information in a general way.
@author sam
*/
public class ReclamationPiscesDataStore extends AbstractDatabaseDataStore
{

/**
List of unique parameters from series.catalog.
*/
private List<String> parameterList = null;
    
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

/**
Return the list of data intervals valid for a data type.
*/
public List<String> getDataIntervalStringsForDataType(String dataType)
{	// TODO SAM 2015-08-06 need to do a database query and not hard code
	List<String> intervals = new ArrayList<String>();
	intervals.add(TimeInterval.getName(TimeInterval.DAY,0));
	return intervals;
}

/**
Return the HDB data type list (global data initialized when database connection is opened).
@return the list of data types 
*/
public List<String> getParameterList ()
{	String routine = getClass().getSimpleName() + ".getParameterList";
	if ( this.parameterList == null ) {
		// Read the unique parameters and save for future access
		ReclamationPiscesDMI dmi = (ReclamationPiscesDMI)getDMI();
		ResultSet rs = null;
		List<String> parameterList = new ArrayList<String>();
		try {
			rs = dmi.dmiSelect("SELECT distinct parameter from seriescatalog WHERE parameter <> '' order by parameter");
			String s;
			while ( rs.next() ) {
				s = rs.getString ( 1 );
				if ( !rs.wasNull() ) {
					parameterList.add ( s.trim() );
				}
			}
			this.parameterList = parameterList;
		}
	    catch (Exception e) {
	        Message.printWarning(3, routine, "Error getting distinct seriescatalog.parameter from Pisces database \"" +
	            dmi.getDatabaseName() + "\" (" + e + ")." );
	        Message.printWarning(3, routine, e );
	    }
	    finally {
	        if ( rs != null ) {
	            try {
	            	rs.close();
	            }
	            catch ( Exception e ) {
	            	// OK to absorb
	            }
	        }
	    }
	}
    return this.parameterList;
}

/**
Convert a TSID interval (e.g., "Day") to Pisces interval (e.g., "Daily").
*/
public String getPiscesIntervalFromTSIDInterval(String interval)
{
	if ( interval.equalsIgnoreCase(TimeInterval.getName(TimeInterval.HOUR, 0)) ) {
		return "Hourly";
	}
	else if ( interval.equalsIgnoreCase(TimeInterval.getName(TimeInterval.DAY, 0)) ) {
		return "Daily";
	}
	else if ( interval.equalsIgnoreCase(TimeInterval.getName(TimeInterval.MONTH, 0)) ) {
		return "Monthly";
	}
	else if ( interval.equalsIgnoreCase(TimeInterval.getName(TimeInterval.YEAR, 0)) ) {
		return "Yearly";
	}
	else {
		// Leave as is and deal with errors in code in future updates.
		return interval;
	}
}

/**
Convert a Pisces interval (e.g., "Daily") to TSID interval (e.g., "Day").
*/
public String getTSIDIntervalFromPiscesInterval(String interval)
{
	if ( interval.equalsIgnoreCase("Hourly") ) {
		return TimeInterval.getName(TimeInterval.HOUR, 0);
	}
	else if ( interval.equalsIgnoreCase("Daily") ) {
		return TimeInterval.getName(TimeInterval.DAY, 0);
	}
	else if ( interval.equalsIgnoreCase("Monthly") ) {
		return TimeInterval.getName(TimeInterval.MONTH, 0);
	}
	else if ( interval.equalsIgnoreCase("Yearly") ) {
		return TimeInterval.getName(TimeInterval.YEAR, 0);
	}
	else {
		// Leave as is and deal with errors in code in future updates.
		return interval;
	}
}

/**
Read site/series metadata from join of sitecatalog and seriescatalog tables, useful for listing time series.
@param siteID if not null, use to filter the query, used for individual time series reads (location ID).
@param responsibility if not null, use to filter the query, used for individual time series reads (data source).
@param parameter if not null and not "*", use to filter the query, used for all time series reads (data type).
@param interval if not null and not "*", use to filter the query, used for all time series reads (interval).
@param panel filter panel used to filter the query for catalog values.
@return list of catalog objects
*/
public List<ReclamationPisces_SiteCatalogSeriesCatalog> readSiteCatalogSeriesCatalogList (
	String siteID, String responsibility, String parameter, String interval, InputFilter_JPanel panel )
{	String routine = getClass().getSimpleName() + ".readSiteCatalogSeriesCatalogList";
	List<ReclamationPisces_SiteCatalogSeriesCatalog> metaList = new ArrayList<ReclamationPisces_SiteCatalogSeriesCatalog>();
	ReclamationPiscesDMI dmi = (ReclamationPiscesDMI)this.getDMI();
	// Get the query parameters from the input filter...
	//List<String> realModelType = ifp.getInput("Real, Model, Ensemble Data", null, true, null);
	// Read all the data so values can be set as time series properties
	ResultSet rs = null;
	try {
		// Create a where clause - some from direct specification and also handle input filter if non-null
		StringBuilder whereClause = new StringBuilder();
		if ( (siteID != null) && !siteID.isEmpty() ) {
			if ( whereClause.length() > 0 ) {
				whereClause.append(" AND ");
			}
			whereClause.append ( "sitecatalog.siteid = '" + siteID + "'" );
		}
		if ( (responsibility != null) && !responsibility.isEmpty() ) {
			if ( whereClause.length() > 0 ) {
				whereClause.append(" AND ");
			}
			whereClause.append ( "sitecatalog.responsibility = '" + responsibility + "'" );
		}
		if ( (parameter != null) && !parameter.isEmpty() && !parameter.equals("*") ) {
			if ( whereClause.length() > 0 ) {
				whereClause.append(" AND ");
			}
			whereClause.append ( "seriescatalog.parameter = '" + parameter + "'" );
		}
		if ( (interval != null) && !interval.isEmpty() && !interval.equals("*") ) {
			if ( whereClause.length() > 0 ) {
				whereClause.append(" AND ");
			}
			whereClause.append ( "seriescatalog.timeinterval = '" + getPiscesIntervalFromTSIDInterval(interval) + "'" );
		}
		if ( whereClause.length() > 0 ) {
			whereClause.insert(0, " WHERE ");
		}
		rs = dmi.dmiSelect("SELECT sitecatalog.siteid, sitecatalog.description, "
			+ "sitecatalog.state, sitecatalog.latitude, sitecatalog.longitude, sitecatalog.elevation, sitecatalog.timezone, sitecatalog.install, "
			+ "sitecatalog.horizontal_datum, sitecatalog.vertical_datum, sitecatalog.vertical_accuracy, sitecatalog.elevation_method, "
			+ "sitecatalog.tz_offset, sitecatalog.active_flag, sitecatalog.type, sitecatalog.responsibility, "
			+ "seriescatalog.id, seriescatalog.parentid, seriescatalog.isfolder, seriescatalog.sortorder, seriescatalog.iconname, "
			+ "seriescatalog.name, seriescatalog.units, seriescatalog.timeinterval, seriescatalog.parameter, "
			+ "seriescatalog.tablename, seriescatalog.provider, seriescatalog.connectionstring, seriescatalog.expression, "
			+ "seriescatalog.notes, seriescatalog.enabled "
			+ "FROM sitecatalog "
			+ "INNER JOIN seriescatalog ON sitecatalog.siteid=seriescatalog.siteid " + whereClause + " ORDER BY sitecatalog.siteid");
		Message.printStatus(2,routine,"SQL:" + dmi.getLastSQLString());
		// Convert to objects
		metaList = toSiteCatalogSeriesCatalogList ( rs );
	}
    catch (Exception e) {
        Message.printWarning(3, routine, "Error getting sitecatalog+seriescatalog data from Pisces database \"" +
            dmi.getDatabaseName() + "\" (" + e + ")." );
        Message.printWarning(3, routine, e );
    }
    finally {
        if ( rs != null ) {
            try {
            	rs.close();
            }
            catch ( Exception e ) {
            	// OK to absorb
            }
        }
    }
	return metaList;
}

/**
Read a time series from the ReclamationPisces database using the string time series identifier.
@param tsidentString time series identifier string.
@param readStart the starting date/time to read.
@param readEnd the ending date/time to read.
@param readData if true, read the data; if false, only read the time series metadata.
@return the time series
*/
public TS readTimeSeries ( String tsidentString, DateTime readStart, DateTime readEnd, boolean readData )
throws Exception
{   String routine = getClass().getSimpleName() + "readTimeSeries";
    TSIdent tsident = TSIdent.parseIdentifier(tsidentString );

    if ( (tsident.getIntervalBase() != TimeInterval.HOUR) && (tsident.getIntervalBase() != TimeInterval.DAY)
    	&& (tsident.getIntervalBase() != TimeInterval.MONTH) && (tsident.getIntervalBase() != TimeInterval.YEAR )) {
        // Not able to handle multiples for non-hourly
        throw new IllegalArgumentException("Unsupported interval for \"" + tsidentString
        	+ "\" - only hour, day, month, and year are supported for Reclamation Pisces database." );
    }
    // Read time series metadata
    String siteID = tsident.getLocation();
    String responsible = tsident.getSource();
    String parameter = tsident.getType();
    String interval = getPiscesIntervalFromTSIDInterval(tsident.getInterval());
    List<ReclamationPisces_SiteCatalogSeriesCatalog>tsMetadataList = readSiteCatalogSeriesCatalogList (siteID, responsible, parameter, interval, null );
    if ( tsMetadataList.size() != 1 ) {
        throw new InvalidParameterException ( "Time series identifier \"" + tsidentString +
            "\" matches " + tsMetadataList.size() + " time series - should match exactly one but have " + tsMetadataList.size() + "." );
    }
    // Create the time series...
    TS ts = TSUtil.newTimeSeries(tsidentString, true);
    ts.setIdentifier(tsident);
    // Set the missing value to NaN.
    ts.setMissing(Double.NaN);
    // Set additional time series properties from the metadata
    ReclamationPisces_SiteCatalogSeriesCatalog tsMetadata = tsMetadataList.get(0);
    ts.setDataUnitsOriginal(tsMetadata.getUnits());
    ts.setDataUnits(tsMetadata.getUnits());
    // Now read the data - inline processing of the data table resultset to improve performance...
    if ( readData ) {
    	// Only set the properties if reading data - can be a lot of data and is not needed to list catalog in TSTool
        setTimeSeriesProperties ( ts, tsMetadata );
    	ResultSet rs = null;
    	ReclamationPiscesDMI dmi = (ReclamationPiscesDMI)this.getDMI();
		String tableName = tsMetadata.getTableName();
    	// First get the min and max dates
		int index;
		Date dt = null;
		DateTime dbStart = null;
		DateTime dbEnd = null;
    	rs = dmi.dmiSelect ( "SELECT MIN(" + tableName + ".datetime) FROM " + tableName );
    	try {
    		while ( rs.next() ) {
    			dt = rs.getTimestamp( 1 );
    			if ( !rs.wasNull() ) {
    				dbStart = new DateTime(dt);
    			}
    		}
    	}
	    catch (Exception e) {
	        Message.printWarning(3, routine, "Error reading time eries data from Pisces database \"" +
	            dmi.getDatabaseName() + "\" table \"" + tableName + "\" (" + e + ")." );
	        Message.printWarning(3, routine, e );
	    }
	    finally {
	        if ( rs != null ) {
	            try {
	            	rs.close();
	            }
	            catch ( Exception e ) {
	            	// OK to absorb
	            }
	        }
	    }
    	rs = dmi.dmiSelect ( "SELECT MAX(" + tableName + ".datetime) FROM " + tableName );
    	try {
    		while ( rs.next() ) {
    			dt = rs.getTimestamp( 1 );
    			if ( !rs.wasNull() ) {
    				dbEnd = new DateTime(dt);
    			}
    		}
    	}
	    catch (Exception e) {
	        Message.printWarning(3, routine, "Error reading time series max date from Pisces database \"" +
	            dmi.getDatabaseName() + "\" table \"" + tableName + "\" (" + e + ")." );
	        Message.printWarning(3, routine, e );
	    }
	    finally {
	        if ( rs != null ) {
	            try {
	            	rs.close();
	            }
	            catch ( Exception e ) {
	            	// OK to absorb
	            }
	        }
	    }
    	// Next read the data records and transfer
    	if ( readStart != null ) {
    		ts.setDate1(readStart);
    	}
    	else {
    		ts.setDate1(dbStart);
    	}
    	ts.setDate1Original(dbStart);
    	if ( readEnd != null ) {
    		ts.setDate2(readEnd);
    	}
    	else {
    		ts.setDate2(dbEnd);
    	}
    	ts.setDate2Original(dbEnd);
    	ts.allocateDataSpace();
    	try {
    		String sql = "SELECT " + tableName + ".datetime, " + tableName + ".value, "
        		+ tableName + ".flag FROM " + tableName + " ORDER BY " + tableName + ".datetime";
    		rs = dmi.dmiSelect(sql);
    		Message.printStatus(2,routine,"SQL: "+sql);
    		double value;
    		String flag;
    		double missing = ts.getMissing();
    		DateTime dt2;
    		while ( rs.next() ) {
    			index = 1;
    			dt = rs.getTimestamp( index++ );
    			if ( rs.wasNull() ) {
    				dt = null;
    			}
    			value = rs.getDouble( index++ );
    			if ( rs.wasNull() ) {
    				value = missing;
    			}
    			flag = rs.getString( index++ );
    			if ( rs.wasNull() ) {
    				flag = null;
    			}
    			else {
    				flag = flag.trim();
    			}
    			if ( dt != null ) {
    				dt2 = new DateTime(dt);
	    			if ( flag != null ) {
	    				ts.setDataValue(dt2,value,flag,0);
	    			}
	    			else {
	    				ts.setDataValue(dt2,value);
	    			}
    			}
    		}
    	}
        catch (Exception e) {
            Message.printWarning(3, routine, "Error reading time series data from Pisces database \"" +
                dmi.getDatabaseName() + "\" table \"" + tableName + "\" (" + e + ")." );
            Message.printWarning(3, routine, e );
        }
        finally {
            if ( rs != null ) {
                try {
                	rs.close();
                }
                catch ( Exception e ) {
                	// OK to absorb
                }
            }
        }
    }
    return ts;
}

/**
Set the time series properties based on metadata in the database.
@param ts time series process.
@param tsm catalog metadata for time series.
*/
private void setTimeSeriesProperties ( TS ts, ReclamationPisces_SiteCatalogSeriesCatalog tsm )
{
	// Site data (from "sitecatalog" table)...
    ts.setProperty("siteid", tsm.getSiteID() );
    ts.setProperty("description", tsm.getDescription() );
    ts.setProperty("state", tsm.getState() );
    ts.setProperty("latitude", DMIUtil.isMissing(tsm.getLatitude()) ? null : new Double(tsm.getLatitude()) );
    ts.setProperty("longitude", DMIUtil.isMissing(tsm.getLongitude()) ? null : new Double(tsm.getLongitude()) );
    ts.setProperty("elevation", DMIUtil.isMissing(tsm.getElevation()) ? null : new Double(tsm.getElevation()) );
    ts.setProperty("timezone", tsm.getTimeZone() );
    ts.setProperty("install", tsm.getInstall() );
    ts.setProperty("horizontal_datum", tsm.getHorizontalDatum() );
    ts.setProperty("vertical_datum", tsm.getVerticalDatum() );
    ts.setProperty("vertical_accuracy", DMIUtil.isMissing(tsm.getVerticalAccuracy()) ? null : new Double(tsm.getVerticalAccuracy()) );
    ts.setProperty("elevation_method", tsm.getElevationMethod() );
    ts.setProperty("tz_offset", tsm.getTimeZoneOffset() );
    ts.setProperty("active_flag", tsm.getActiveFlag() );
    ts.setProperty("type", tsm.getType() );
    ts.setProperty("responsibility", tsm.getResponsibility() );
    
    // Series data (from "seriescatalog" table)...
    ts.setProperty("id", DMIUtil.isMissing(tsm.getID()) ? null : new Integer(tsm.getID()) );
    ts.setProperty("parentid", DMIUtil.isMissing(tsm.getParentID()) ? null : new Integer(tsm.getParentID()) );
    ts.setProperty("isfolder", DMIUtil.isMissing(tsm.getIsFolder()) ? null : new Integer(tsm.getIsFolder()) );
    ts.setProperty("sortorder", DMIUtil.isMissing(tsm.getSortOrder()) ? null : new Integer(tsm.getSortOrder()) );
    ts.setProperty("iconname", tsm.getIconName() );
    ts.setProperty("name", tsm.getName() );
    ts.setProperty("units", tsm.getUnits() );
    ts.setProperty("timeinterval", tsm.getTimeInterval() );
    ts.setProperty("parameter", tsm.getParameter() );
    ts.setProperty("tablename", tsm.getTableName() );
    ts.setProperty("provider", tsm.getProvider() );
    ts.setProperty("connectionstring", tsm.getConnectionString() );
    ts.setProperty("expression", tsm.getExpression() );
    ts.setProperty("notes", tsm.getNotes() );
    ts.setProperty("enabled", DMIUtil.isMissing(tsm.getEnabled()) ? null : new Integer(tsm.getEnabled()) );
}

/**
Convert a ResultSet to a Vector of RiversideDB_MeasLocType
@param rs ResultSet from a MeasLocType table query.
@throws Exception if an error occurs
*/
private List<ReclamationPisces_SiteCatalogSeriesCatalog> toSiteCatalogSeriesCatalogList ( ResultSet rs )
throws SQLException
{
	List<ReclamationPisces_SiteCatalogSeriesCatalog> v = new ArrayList<ReclamationPisces_SiteCatalogSeriesCatalog>();
	int index = 1;
	String s;
	double d;
	int i;
	ReclamationPisces_SiteCatalogSeriesCatalog data = null;
	while ( rs.next() ) {
		data = new ReclamationPisces_SiteCatalogSeriesCatalog();
		index = 1;
		s = rs.getString ( index++ );
		if ( !rs.wasNull() ) {
			data.setSiteID ( s.trim() );
		}
		s = rs.getString ( index++ );
		if ( !rs.wasNull() ) {
			data.setDescription ( s.trim() );
		}
		s = rs.getString ( index++ );
		if ( !rs.wasNull() ) {
			data.setState ( s.trim() );
		}
		d = rs.getDouble ( index++ );
		if ( !rs.wasNull() ) {
			data.setLatitude ( d );
		}
		d = rs.getDouble ( index++ );
		if ( !rs.wasNull() ) {
			data.setLongitude ( d );
		}
		d = rs.getDouble ( index++ );
		if ( !rs.wasNull() ) {
			data.setElevation ( d );
		}
		s = rs.getString ( index++ );
		if ( !rs.wasNull() ) {
			data.setTimeZone ( s.trim() );
		}
		s = rs.getString ( index++ );
		if ( !rs.wasNull() ) {
			data.setInstall ( s.trim() );
		}
		s = rs.getString ( index++ );
		if ( !rs.wasNull() ) {
			data.setHorizontalDatum ( s.trim() );
		}
		s = rs.getString ( index++ );
		if ( !rs.wasNull() ) {
			data.setVerticalDatum ( s.trim() );
		}
		d = rs.getDouble ( index++ );
		if ( !rs.wasNull() ) {
			data.setVerticalAccuracy ( d );
		}
		s = rs.getString ( index++ );
		if ( !rs.wasNull() ) {
			data.setElevationMethod ( s.trim() );
		}
		s = rs.getString ( index++ );
		if ( !rs.wasNull() ) {
			data.setTimeZoneOffset ( s.trim() );
		}
		s = rs.getString ( index++ );
		if ( !rs.wasNull() ) {
			data.setActiveFlag ( s.trim() );
		}
		s = rs.getString ( index++ );
		if ( !rs.wasNull() ) {
			data.setType ( s.trim() );
		}
		s = rs.getString ( index++ );
		if ( !rs.wasNull() ) {
			data.setResponsibility ( s.trim() );
		}
		i = rs.getInt ( index++ );
		if ( !rs.wasNull() ) {
			data.setID ( i );
		}
		i = rs.getInt ( index++ );
		if ( !rs.wasNull() ) {
			data.setParentID ( i );
		}
		i = rs.getInt ( index++ );
		if ( !rs.wasNull() ) {
			data.setIsFolder ( i );
		}
		i = rs.getInt ( index++ );
		if ( !rs.wasNull() ) {
			data.setSortOrder ( i );
		}
		s = rs.getString ( index++ );
		if ( !rs.wasNull() ) {
			data.setIconName ( s.trim() );
		}
		s = rs.getString ( index++ );
		if ( !rs.wasNull() ) {
			data.setName ( s.trim() );
		}
		s = rs.getString ( index++ );
		if ( !rs.wasNull() ) {
			data.setUnits ( s.trim() );
		}
		s = rs.getString ( index++ );
		if ( !rs.wasNull() ) {
			data.setTimeInterval ( s.trim() );
		}
		s = rs.getString ( index++ );
		if ( !rs.wasNull() ) {
			data.setParameter ( s.trim() );
		}
		s = rs.getString ( index++ );
		if ( !rs.wasNull() ) {
			data.setTableName ( s.trim() );
		}
		s = rs.getString ( index++ );
		if ( !rs.wasNull() ) {
			data.setProvider ( s.trim() );
		}
		s = rs.getString ( index++ );
		if ( !rs.wasNull() ) {
			data.setConnectionString ( s.trim() );
		}
		s = rs.getString ( index++ );
		if ( !rs.wasNull() ) {
			data.setExpression ( s.trim() );
		}
		s = rs.getString ( index++ );
		if ( !rs.wasNull() ) {
			data.setNotes ( s.trim() );
		}
		i = rs.getInt ( index++ );
		if ( !rs.wasNull() ) {
			data.setEnabled ( i );
		}
		v.add(data);
	}
	return v;
}

}