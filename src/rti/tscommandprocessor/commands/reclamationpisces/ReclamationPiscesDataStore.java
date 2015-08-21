package rti.tscommandprocessor.commands.reclamationpisces;

import java.security.InvalidParameterException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import RTi.DMI.AbstractDatabaseDataStore;
import RTi.DMI.DMI;
import RTi.DMI.DMIUtil;
import RTi.TS.TS;
import RTi.TS.TSIdent;
import RTi.TS.TSUtil;
import RTi.Util.GUI.InputFilter;
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
private List<ReclamationPisces_Ref_Parameter> parameterList = null;
    
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
{	// Get the intervals given the data type - will return "Daily", "Monthly", etc.
	if ( dataType.equals("*") ) {
		// Get intervals for all data types
		dataType = null;
	}
	List<String> intervals = readDistinctTimeIntervalList(dataType);
	// Do some extra work to sort and convert to generic representation
	List <String> intervals2 = new ArrayList<String>();
	for ( String i : intervals ) {
		if ( i.equalsIgnoreCase("Hourly") ) {
			intervals2.add(TimeInterval.getName(TimeInterval.HOUR, 0));
			break;
		}
	}
	for ( String i : intervals ) {
		if ( i.equalsIgnoreCase("Daily") ) {
			intervals2.add(TimeInterval.getName(TimeInterval.DAY, 0));
			break;
		}
	}
	for ( String i : intervals ) {
		if ( i.equalsIgnoreCase("Monthly") ) {
			intervals2.add(TimeInterval.getName(TimeInterval.MONTH, 0));
			break;
		}
	}
	for ( String i : intervals ) {
		if ( i.equalsIgnoreCase("Yearly") ) {
			intervals2.add(TimeInterval.getName(TimeInterval.YEAR, 0));
			break;
		}
	}
	return intervals2;
}

/**
Return the HDB data type list (global data initialized when database connection is opened).
@return the list of data types 
*/
public List<ReclamationPisces_Ref_Parameter> getParameterList ()
{	if ( this.parameterList == null ) {
		// Read the unique parameters and save for future access
		this.parameterList = readRefParameterList();
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
Create a list of where clauses give an InputFilter_JPanel.  The InputFilter
instances that are managed by the InputFilter_JPanel must have been defined with
the database table and field names in the internal (non-label) data.
@return a list of where clauses, each of which can be added to a DMI statement.
@param dmi The DMI instance being used, which may be checked for specific formatting.
@param panel The InputFilter_JPanel instance to be converted.  If null, an empty list will be returned.
*/
private List<String> getWhereClausesFromInputFilter ( DMI dmi, InputFilter_JPanel panel ) 
{
    // Loop through each filter group.  There will be one where clause per filter group.

    if (panel == null) {
        return new Vector();
    }

    int nfg = panel.getNumFilterGroups ();
    InputFilter filter;
    List<String> where_clauses = new Vector();
    String where_clause=""; // A where clause that is being formed.
    for ( int ifg = 0; ifg < nfg; ifg++ ) {
        filter = panel.getInputFilter ( ifg );  
        where_clause = DMIUtil.getWhereClauseFromInputFilter(dmi, filter,panel.getOperator(ifg), true);
        if (where_clause != null) {
            where_clauses.add(where_clause);
        }
    }
    return where_clauses;
}

/**
Create a where string given an InputFilter_JPanel.  The InputFilter
instances that are managed by the InputFilter_JPanel must have been defined with
the database table and field names in the internal (non-label) data.
@return a list of where clauses as a string, each of which can be added to a DMI statement.
@param dmi The DMI instance being used, which may be checked for specific formatting.
@param panel The InputFilter_JPanel instance to be converted.  If null, an empty list will be returned.
@param tableName the name of the table for which to get where clauses.  This will be the leading XXXX. of
the matching strings.
@param useAnd if true, then "and" is used instead of "where" in the where strings.  The former can be used
with "join on" SQL syntax.
@param addNewline if true, add a newline if the string is non-blank - this simply helps with formatting of
the big SQL, so that logging has reasonable line breaks
*/
private String getWhereClauseStringFromInputFilter ( DMI dmi, InputFilter_JPanel panel, String tableName,
   boolean addNewline )
{
    List<String> whereClauses = getWhereClausesFromInputFilter ( dmi, panel );
    StringBuffer whereString = new StringBuffer();
    String tableNameDot = (tableName + ".").toUpperCase();
    for ( String whereClause : whereClauses ) {
        // TODO SAM 2011-09-30 If the new code works then remove the following old code
        //if ( !whereClause.toUpperCase().startsWith(tableNameDot) ) {
        //Message.printStatus(2, "", "Checking where clause \"" + whereClause + "\" against \"" + tableNameDot + "\"" );
        if ( whereClause.toUpperCase().indexOf(tableNameDot) < 0 ) {
            // Not for the requested table so don't include the where clause
            continue;
        }
        if ( (whereString.length() > 0)  ) {
            // Need to concatenate
            whereString.append ( " and ");
        }
        whereString.append ( "(" + whereClause + ")");
    }
    if ( addNewline && (whereString.length() > 0) ) {
        whereString.append("\n");
    }
    return whereString.toString();
}

/**
Read distinct time intervals from a join of sitecatalog and view_seriescatalog
(ensures that only records with time series are checked).
@return list of time intervals (e.g., "Daily").
*/
public List<String> readDistinctTimeIntervalList ( String parameter )
{	String routine = getClass().getSimpleName() + ".readDistinctTimeIntervalList";
	List<String> iList = new ArrayList<String>();
	ReclamationPiscesDMI dmi = (ReclamationPiscesDMI)this.getDMI();
	ResultSet rs = null;
	try {
		StringBuilder where = new StringBuilder("WHERE (sitecatalog.siteid <> '') and (view_series.tablename <> ''");
		if ( (parameter != null) && !parameter.isEmpty() ) {
			where.append( " AND view_seriescatalog.parameter = '" + parameter + "'" );
		}
		rs = dmi.dmiSelect("SELECT distinct view_seriescatalog.timeinterval from view_seriescatalog "
			+ "INNER JOIN sitecatalog ON sitecatalog.siteid=view_seriescatalog.siteid " + where );
		// Convert to objects
		String s;
		while ( rs.next() ) {
			s = rs.getString ( 1 );
			if ( !rs.wasNull() ) {
				iList.add(s);
			}
		}
	}
    catch (Exception e) {
        Message.printWarning(3, routine, "Error reading ref_parameter from Pisces database \"" +
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
	return iList;
}

/**
Read the ref_parameter table, useful for creating a list of parameters for choices.
@return list of parameter objects
*/
public List<ReclamationPisces_Ref_Parameter> readRefParameterList ()
{	String routine = getClass().getSimpleName() + ".readRefParameterList";
	List<ReclamationPisces_Ref_Parameter> pList = new ArrayList<ReclamationPisces_Ref_Parameter>();
	ReclamationPiscesDMI dmi = (ReclamationPiscesDMI)this.getDMI();
	// Get the query parameters from the input filter...
	//List<String> realModelType = ifp.getInput("Real, Model, Ensemble Data", null, true, null);
	// Read all the data so values can be set as time series properties
	ResultSet rs = null;
	try {
		rs = dmi.dmiSelect("SELECT ref_parameter.id, ref_parameter.description "
			+ "FROM ref_parameter ORDER BY ref_parameter.id");
		// Convert to objects
		pList = toRefParameterList ( rs );
	}
    catch (Exception e) {
        Message.printWarning(3, routine, "Error reading ref_parameter from Pisces database \"" +
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
	return pList;
}

/**
Read site/series metadata from join of sitecatalog and seriescatalog tables, useful for listing time series.
@param siteID if not null, use to filter the query, used for individual time series reads (location ID).
@param server if not null, use to filter the query, used for individual time series reads (data source).
@param parameter if not null and not "*", use to filter the query, used for all time series reads (data type).
@param interval if not null and not "*", use to filter the query, used for all time series reads (interval).
@param ifp filter panel used to filter the query for catalog values.
@return list of catalog objects
*/
public List<ReclamationPisces_SiteCatalogSeriesCatalog> readSiteCatalogSeriesCatalogList (
	String siteID, String server, String parameter, String interval, InputFilter_JPanel ifp )
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
		// Always prohibit empty site ID (artifact of processing bad data?)
		whereClause.append (" WHERE ((sitecatalog.siteid <> '') AND (view_seriescatalog.tablename <> '')" );
		if ( (siteID != null) && !siteID.isEmpty() ) {
			whereClause.append ( " AND (sitecatalog.siteid = '" + siteID + "')" );
		}
		if ( (server != null) && !server.isEmpty() ) {
			whereClause.append ( " AND (view_seriescatalog.server = '" + server + "')" );
		}
		if ( (parameter != null) && !parameter.isEmpty() && !parameter.equals("*") ) {
			whereClause.append ( " AND (view_seriescatalog.parameter = '" + parameter + "')" );
		}
		if ( (interval != null) && !interval.isEmpty() && !interval.equals("*") ) {
			whereClause.append ( " AND (view_seriescatalog.timeinterval = '" + getPiscesIntervalFromTSIDInterval(interval) + "')" );
		}
		// Process the where clauses by extracting input filter where clauses that reference specific tables.
	    // Include where clauses for specific tables.  Do this rather than in bulk to make sure that
	    // inappropriate filters are not applied (e.g., model filters when only real data are queried)
	    List<String> whereClauses = new ArrayList<String>();
	    // First include general where clauses
	    //whereClauses.add ( dataTypeWhereString );
	    if ( ifp != null ) {
		    whereClauses.add ( getWhereClauseStringFromInputFilter ( dmi, ifp, "sitecatalog", true ) );
		    whereClauses.add ( getWhereClauseStringFromInputFilter ( dmi, ifp, "view_seriescatalog", true ) );
		    for ( String where : whereClauses ) {
		    	if ( !where.isEmpty() ) {
		            whereClause.append ( " and " );
		            whereClause.append ( "(" + where + ")" );
		    	}
		    }
	    }
		whereClause.append ( ")" );
		String sql = "SELECT sitecatalog.siteid, sitecatalog.description, "
			+ "sitecatalog.state, sitecatalog.latitude, sitecatalog.longitude, sitecatalog.elevation, sitecatalog.timezone, sitecatalog.install, "
			+ "sitecatalog.horizontal_datum, sitecatalog.vertical_datum, sitecatalog.vertical_accuracy, sitecatalog.elevation_method, "
			+ "sitecatalog.tz_offset, sitecatalog.active_flag, sitecatalog.type, sitecatalog.responsibility, sitecatalog.agency_region, "
			+ "view_seriescatalog.id, view_seriescatalog.parentid, view_seriescatalog.isfolder, view_seriescatalog.sortorder, view_seriescatalog.iconname, "
			+ "view_seriescatalog.name, view_seriescatalog.units, view_seriescatalog.timeinterval, view_seriescatalog.parameter, "
			+ "view_seriescatalog.tablename, view_seriescatalog.provider, view_seriescatalog.connectionstring, view_seriescatalog.expression, "
			+ "view_seriescatalog.notes, view_seriescatalog.enabled, view_seriescatalog.server "
			+ "FROM sitecatalog "
			+ "INNER JOIN view_seriescatalog ON sitecatalog.siteid=view_seriescatalog.siteid " + whereClause + " ORDER BY sitecatalog.siteid";
		Message.printStatus(2,routine,"SQL:" + sql);
		rs = dmi.dmiSelect(sql);
		// Convert to objects
		metaList = toSiteCatalogSeriesCatalogList ( rs );
	}
    catch (Exception e) {
        Message.printWarning(3, routine, "Error reading sitecatalog+view_seriescatalog data from Pisces database \"" +
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
@param readStartReq the requested starting date/time to read or null to read all
@param readEnd the requested ending date/time to read or null to read all
@param readData if true, read the data; if false, only read the time series metadata.
@return the time series
*/
public TS readTimeSeries ( String tsidentString, DateTime readStartReq, DateTime readEndReq, boolean readData )
throws Exception
{   String routine = getClass().getSimpleName() + ".readTimeSeries";
    TSIdent tsident = TSIdent.parseIdentifier(tsidentString );

    if ( (tsident.getIntervalBase() != TimeInterval.HOUR) && (tsident.getIntervalBase() != TimeInterval.DAY)
    	&& (tsident.getIntervalBase() != TimeInterval.MONTH) && (tsident.getIntervalBase() != TimeInterval.YEAR )) {
        // Not able to handle multiples for non-hourly
        throw new IllegalArgumentException("Unsupported interval for \"" + tsidentString
        	+ "\" - only hour, day, month, and year are supported for Reclamation Pisces database." );
    }
    // Read time series metadata
    String siteID = tsident.getLocation();
    String server = tsident.getSource();
    String parameter = tsident.getType();
    String interval = getPiscesIntervalFromTSIDInterval(tsident.getInterval());
    List<ReclamationPisces_SiteCatalogSeriesCatalog>tsMetadataList = readSiteCatalogSeriesCatalogList (siteID, server, parameter, interval, null );
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
    ts.setDescription(tsMetadata.getDescription());
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
		String sql0 = "SELECT MIN(" + tableName + ".datetime) FROM " + tableName;
		//Message.printStatus(2, routine, "SQL: " + sql0);
    	rs = dmi.dmiSelect ( sql0 );
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
    	if ( readStartReq != null ) {
    		ts.setDate1(readStartReq);
    	}
    	else {
    		ts.setDate1(dbStart);
    	}
    	ts.setDate1Original(dbStart);
    	if ( readEndReq != null ) {
    		ts.setDate2(readEndReq);
    	}
    	else {
    		ts.setDate2(dbEnd);
    	}
    	ts.setDate2Original(dbEnd);
    	ts.allocateDataSpace();
    	try {
    		StringBuilder where = new StringBuilder("");
    		if ( readStartReq != null ) {
    			where.append ( tableName + ".datetime >= " + DMIUtil.formatDateTime(dmi, readStartReq) );
    		}
    		if ( readEndReq != null ) {
    			if ( where.length() > 0 ) {
    				where.append ( " AND ");
    			}
    			where.append ( tableName + ".datetime <= " + DMIUtil.formatDateTime(dmi, readEndReq) );
    		}
    		if ( where.length() > 0 ) {
    			where.insert(0, " WHERE ");
    		}
    		String sql = "SELECT " + tableName + ".datetime, " + tableName + ".value, "
        		+ tableName + ".flag FROM " + tableName + where + " ORDER BY " + tableName + ".datetime";
    		rs = dmi.dmiSelect(sql);
    		//Message.printStatus(2,routine,"readStartReq=" + readStartReq + " readEndReq=" + readEndReq + " SQL: "+sql);
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
    ts.setProperty("agency_region", tsm.getAgencyRegion() );
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
    ts.setProperty("server", tsm.getServer() );
}

/**
Convert a ResultSet to a list of Reclamation_Ref_Parameter.
@param rs ResultSet from a ref_parameter table query.
@throws Exception if an error occurs
*/
private List<ReclamationPisces_Ref_Parameter> toRefParameterList ( ResultSet rs )
throws SQLException
{
	List<ReclamationPisces_Ref_Parameter> v = new ArrayList<ReclamationPisces_Ref_Parameter>();
	int index = 1;
	String s;
	ReclamationPisces_Ref_Parameter data = null;
	while ( rs.next() ) {
		data = new ReclamationPisces_Ref_Parameter();
		index = 1;
		s = rs.getString ( index++ );
		if ( !rs.wasNull() ) {
			data.setID ( s.trim() );
		}
		s = rs.getString ( index++ );
		if ( !rs.wasNull() ) {
			data.setDescription ( s.trim() );
		}
		v.add(data);
	}
	return v;
}

/**
Convert a ResultSet to a list of ReclamationPisces_SiteCatalogSeriesCatalog
@param rs ResultSet from a sitecatalog and view_seriescatalog query.
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
		s = rs.getString ( index++ );
		if ( !rs.wasNull() ) {
			data.setAgencyRegion ( s.trim() );
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
		s = rs.getString ( index++ );
		if ( !rs.wasNull() ) {
			data.setServer ( s.trim() );
		}
		v.add(data);
	}
	return v;
}

}