package rti.tscommandprocessor.commands.ipp;

import java.security.InvalidParameterException;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import rti.tscommandprocessor.core.TimeSeriesNotFoundException;

import RTi.DMI.DMI;
import RTi.DMI.DMISelectStatement;
import RTi.DMI.DMIUtil;
import RTi.DMI.DMIWriteStatement;
import RTi.DMI.DMIStatement;

import RTi.TS.TS;
import RTi.TS.TSData;
import RTi.TS.TSIdent;
import RTi.TS.YearTS;

import RTi.Util.Message.Message;

import RTi.Util.GUI.InputFilter_JPanel;
import RTi.Util.IO.IOUtil;

import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

/**
The IppDMI provides an interface to the State of Colorado's IPP database.

<b>SQL Method Naming Conventions</b><p>

The first word in the method name is one of the following:<br>
<ol>
<li>read</li>
<li>write</li>
<li>delete</li>
<li>count</li>
</ol>

The second part of the method name is the data object being operated on.
If a list is returned, then "List" is included in the method name.
Finally, if a select based on a where clause is used, the method includes the
field for the Where.  Examples are:

<ol>
<li>	readMeasTypeList</li>
<li>	readMeasTypeForMeasType_num</li>
</ol>

<p>
<b>Notes on versioning:</b><br>
Version changes require changes throughout the code base.  The following
example tells all the changes that need to be made when a new field is
added to an existing table:<p>
<ul>
<li>in buildSQL(), add the new field to all the select and write statement
sections for the appropriate table.  Do not forget to wrap this new code
with tests for the proper version (DMI.isDatabaseVersionAtLeast())</li>
<li>if, for the table XXXX, a method exists like:<br>
<code>private Vector toXXXXList</code><br>
then add the field to the Vector-filling code in this method</li>
<li>go to the RiversideDB_XXXX.java object that represents the table for
which this field was added.  Add the data member for the field, 
get/set statements, and then add the field (with brief information on the
version in which it was added) to the toString()</li>
<li>add the field, and the appropriate version-checking code, to the 
writeXXXX() method</li>
<li>update determineDatabaseVersion()</li>
</ul>
<p>
<b>User Permissions</b>
User permissions are determined on a record- or table-level basis.  Database
users are not the same as the login/password that is used to actually connect
to the RiversideDB database.  
<p>
In an application, once a user logs in, the database user should be set up
with a call to <tt>setDBUser</tt>.  This method sets a local variable in the 
DMI with the user's information, and also reads the user's group information
and stores it in the DMI.  
<p>
At that point, calls can be made to theDMI methods:<ul>
<li>canCreate</li>
<li>canDelete</li>
<li>canInsert</li>
<li>canRead</li>
<li>canUpdate</li>
<li>canWrite</li>
</ul>
to see if the user has the permissions to perform an action on a table or
record.
<p>
If the user group needs to be changed, a call can be made to 
<tt>changeCurrentGroup()</tt>
*/
public class IppDMI extends DMI {
//implements TSProductDMI, TSSupplier {

/**
IPP initial version handled by Java.
*/
public final static long _VERSION_010000_20090312 = 1000020090312L;
// This member was made public to be used by the main application, which most
// likely than not will not be derived from the RiversideDB_DMI.

protected final static long _VERSION_LATEST = _VERSION_010000_20090312;

/**
List of valid subject types.
*/
private List<String> __subjectList = new Vector();

/**
Hashtable of unique method lists for each subject type.
*/
private Hashtable<IPPSubjectType,List<String>> __dataMetaDataMethodList = new Hashtable();

/**
Hashtable of unique data type lists for each subject type.
*/
private Hashtable<IPPSubjectType,List<String>> __dataMetaDataDataTypeList = new Hashtable();

/**
Hashtable of unique scenario lists for each subject type.
*/
private Hashtable<IPPSubjectType,List<String>> __dataMetaDataScenarioList = new Hashtable();

/**
Hashtable of unique data source lists for each subject type.
*/
private Hashtable<IPPSubjectType,List<String>> __dataMetaDataSourceList = new Hashtable();

/**
Hashtable of unique subject ID lists for each subject type.
*/
private Hashtable<IPPSubjectType,List<String>> __dataMetaDataSubjectIDList = new Hashtable();

/**
Hashtable of unique data subtype lists for each subject type.
*/
private Hashtable<IPPSubjectType,List<String>> __dataMetaDataSubTypeList = new Hashtable();

/**
Database table/view owner prefix, which needs to be added to all queries.  This seems to be
needed at the State, but is optional at Riverside (so put it in all the time).  If the need
goes away, set the string to "".  Most SQL in this package uses simple table and view names
and the prefix is prepended.  This allows flexibility for changing the prefix if necessary.
*/
private String __schemaPrefix = "ippadmin.";

/**
Flags for doing specific select, write and delete queries, sorted by 
table name.  Descriptions of the actual queries are in the read*() methods.
*/
//CountyData (table)
private final int _S_CountyData = 100;

//CountyDataMetaData (view)
private final int _S_CountyDataMetaData = 1000;
//private final int _W_CountyDataMetaData = 1001;
//private final int _D_CountyDataMetaData = 1002;

//Meta-data for county-related time series data
private final int _S_CountyDataMetaDataDistinctDataType = 1010;
private final int _S_CountyDataMetaDataDistinctMethod = 1011;
private final int _S_CountyDataMetaDataDistinctScenario = 1012;
private final int _S_CountyDataMetaDataDistinctSource = 1013;
private final int _S_CountyDataMetaDataDistinctSubjectID = 1014;
private final int _S_CountyDataMetaDataDistinctSubType = 1015;

//IPPData (table)
private final int _S_ProjectData = 1800;

// Meta-data for project-related time series data (view)
private final int _S_ProjectDataMetaData = 2200;
private final int _S_ProjectDataMetaDataDistinctDataType = 2201;
private final int _S_ProjectDataMetaDataDistinctMethod = 2202;
private final int _S_ProjectDataMetaDataDistinctScenario = 2203;
private final int _S_ProjectDataMetaDataDistinctSource = 2204;
private final int _S_ProjectDataMetaDataDistinctSubjectID = 2205;
private final int _S_ProjectDataMetaDataDistinctSubType = 2206;

//ProviderData (table)
private final int _S_ProviderData = 2500;

//ProviderDataMetaData (view)
private final int _S_ProviderDataMetaData = 3000;
//private final int _W_ProviderDataMetaData = 3001;
//private final int _D_ProviderDataMetaData = 3002;

//Meta-data for provider-related time series data
private final int _S_ProviderDataMetaDataDistinctDataType = 3500;
private final int _S_ProviderDataMetaDataDistinctMethod = 3501;
private final int _S_ProviderDataMetaDataDistinctScenario = 3502;
private final int _S_ProviderDataMetaDataDistinctSource = 3503;
private final int _S_ProviderDataMetaDataDistinctSubjectID = 3504;
private final int _S_ProviderDataMetaDataDistinctSubType = 3505;

/** 
Constructor for a database server and database name, to use an automatically created URL.
@param databaseEngine The database engine to use (see the DMI constructor), will default to SQLServer2000.
@param databaseServer The IP address or DSN-resolvable database server machine name.
@param databaseName The database name on the server.  If null, default to "IPP".
@param port Port number used by the database.  If <= 0, default to that for the database engine.
@param systemLogin If not null, this is used as the system login to make the
connection.  If null, the default system login is used.
@param systemPassword If not null, this is used as the system password to make
the connection.  If null, the default system password is used.
*/
public IppDMI ( String databaseEngine, String databaseServer,
String databaseName, int port, String systemLogin, String systemPassword)
throws Exception {
	// Use the default system login and password
	super ( databaseEngine, databaseServer, databaseName, port, systemLogin, systemPassword );
    if ( databaseEngine == null ) {
        // Use the default...
        setDatabaseEngine("SQLServer");
    }
    if ( databaseServer == null ) {
        // Use the default...
        setDatabaseServer("hbserver");
    }
    if ( databaseName == null ) {
        // Use the default...
        setDatabaseName("IPP");
    }
	if ( systemLogin == null ) {
		// Use the default...
		setSystemLogin("ippadmin");
	}
	if ( systemPassword == null ) {
		// Use the default...
		setSystemPassword("r1ver");
	}
	setEditable(true);
	setSecure(false);
}

// A FUNCTIONS
// B FUNCTIONS

/** 
Build an SQL string based on a requested SQL statement code.  This defines 
the basic statement and allows overloaded methods to avoid redundant code.
This method is used to eliminate redundant code where methods use the same
basic statement but with different where clauses.
@param statement Statement to set values in.
@param sqlNumber the number of the SQL statement to build.  Usually defined
as a private constant as a mnemonic aid.
@throws Exception if an error occurs
*/
private void buildSQL ( DMIStatement statement, int sqlNumber )
throws Exception
{   String routine = getClass().getName() + ".buildSQL";
	DMISelectStatement select;
	//DMIWriteStatement write;
	//DMIDeleteStatement del;
	String schemaPrefix = getSchemaPrefix();
	switch ( sqlNumber ) {
        case _S_CountyData:
            select = (DMISelectStatement)statement;
            select.addField ( schemaPrefix + "tblCountyData.id" );
            select.addField ( schemaPrefix + "tblCountyData.year" );
            select.addField ( schemaPrefix + "tblCountyData.value" );
            select.addTable ( schemaPrefix + "tblCountyData" );
            break;
        case _S_CountyDataMetaData:
            select = (DMISelectStatement)statement;
            select.addField ( schemaPrefix + "vCountyDataMetaData.id" );
            select.addField ( schemaPrefix + "vCountyDataMetaData.subjectID" );
            select.addField ( schemaPrefix + "vCountyDataMetaData.name" );
            select.addField ( schemaPrefix + "vCountyDataMetaData.dataType" );
            select.addField ( schemaPrefix + "vCountyDataMetaData.subType" );
            select.addField ( schemaPrefix + "vCountyDataMetaData.units" );
            select.addField ( schemaPrefix + "vCountyDataMetaData.method" );
            select.addField ( schemaPrefix + "vCountyDataMetaData.subMethod" );
            select.addField ( schemaPrefix + "vCountyDataMetaData.source" );
            select.addField ( schemaPrefix + "vCountyDataMetaData.scenario" );
            select.addTable ( schemaPrefix + "vCountyDataMetaData" );
            break;
        case _S_CountyDataMetaDataDistinctDataType:
            select = (DMISelectStatement)statement;
            select.addField ( schemaPrefix + "vCountyDataMetaData.dataType" );
            select.addTable ( schemaPrefix + "vCountyDataMetaData" );
            select.selectDistinct(true);
            break;
        case _S_CountyDataMetaDataDistinctMethod:
            select = (DMISelectStatement)statement;
            select.addField ( schemaPrefix + "vCountyDataMetaData.method" );
            select.addTable ( schemaPrefix + "vCountyDataMetaData" );
            select.selectDistinct(true);
            break;
        case _S_CountyDataMetaDataDistinctScenario:
            select = (DMISelectStatement)statement;
            select.addField ( schemaPrefix + "vCountyDataMetaData.scenario" );
            select.addTable ( schemaPrefix + "vCountyDataMetaData" );
            select.selectDistinct(true);
            break;
        case _S_CountyDataMetaDataDistinctSource:
            select = (DMISelectStatement)statement;
            select.addField ( schemaPrefix + "vCountyDataMetaData.source" );
            select.addTable ( schemaPrefix + "vCountyDataMetaData" );
            select.selectDistinct(true);
            break;
        case _S_CountyDataMetaDataDistinctSubjectID:
            select = (DMISelectStatement)statement;
            select.addField ( schemaPrefix + "vCountyDataMetaData.subjectID" );
            select.addTable ( schemaPrefix + "vCountyDataMetaData" );
            select.selectDistinct(true);
            break;
        case _S_CountyDataMetaDataDistinctSubType:
            select = (DMISelectStatement)statement;
            select.addField ( schemaPrefix + "vCountyDataMetaData.subType" );
            select.addTable ( schemaPrefix + "vCountyDataMetaData" );
            select.selectDistinct(true);
            break;
        case _S_ProjectData:
            select = (DMISelectStatement)statement;
            select.addField ( schemaPrefix + "tblProjectData.id" );
            select.addField ( schemaPrefix + "tblProjectData.year" );
            select.addField ( schemaPrefix + "tblProjectData.value" );
            select.addTable ( schemaPrefix + "tblProjectData" );
            break;
        case _S_ProjectDataMetaData:
            select = (DMISelectStatement)statement;
            select.addField ( schemaPrefix + "vProjectDataMetaData.id" );
            select.addField ( schemaPrefix + "vProjectDataMetaData.subjectID" );
            select.addField ( schemaPrefix + "vProjectDataMetaData.name" );
            select.addField ( schemaPrefix + "vProjectDataMetaData.dataType" );
            select.addField ( schemaPrefix + "vProjectDataMetaData.subType" );
            select.addField ( schemaPrefix + "vProjectDataMetaData.units" );
            select.addField ( schemaPrefix + "vProjectDataMetaData.method" );
            select.addField ( schemaPrefix + "vProjectDataMetaData.subMethod" );
            select.addField ( schemaPrefix + "vProjectDataMetaData.source" );
            select.addField ( schemaPrefix + "vProjectDataMetaData.scenario" );
            select.addTable ( schemaPrefix + "vProjectDataMetaData" );
            break;
        case _S_ProjectDataMetaDataDistinctDataType:
            select = (DMISelectStatement)statement;
            select.addField ( schemaPrefix + "vProjectDataMetaData.dataType" );
            select.addTable ( schemaPrefix + "vProjectDataMetaData" );
            select.selectDistinct(true);
            break;
        case _S_ProjectDataMetaDataDistinctMethod:
            select = (DMISelectStatement)statement;
            select.addField ( schemaPrefix + "vProjectDataMetaData.method" );
            select.addTable ( schemaPrefix + "vProjectDataMetaData" );
            select.selectDistinct(true);
            break;
        case _S_ProjectDataMetaDataDistinctScenario:
            select = (DMISelectStatement)statement;
            select.addField ( schemaPrefix + "vProjectDataMetaData.scenario" );
            select.addTable ( schemaPrefix + "vProjectDataMetaData" );
            select.selectDistinct(true);
            break;
        case _S_ProjectDataMetaDataDistinctSource:
            select = (DMISelectStatement)statement;
            select.addField ( schemaPrefix + "vProjectDataMetaData.source" );
            select.addTable ( schemaPrefix + "vProjectDataMetaData" );
            select.selectDistinct(true);
            break;
        case _S_ProjectDataMetaDataDistinctSubjectID:
            select = (DMISelectStatement)statement;
            select.addField ( schemaPrefix + "vProjectDataMetaData.subjectID" );
            select.addTable ( schemaPrefix + "vProjectDataMetaData" );
            select.selectDistinct(true);
            break;
        case _S_ProjectDataMetaDataDistinctSubType:
            select = (DMISelectStatement)statement;
            select.addField ( schemaPrefix + "vProjectDataMetaData.subType" );
            select.addTable ( schemaPrefix + "vProjectDataMetaData" );
            select.selectDistinct(true);
            break;
        case _S_ProviderData:
            select = (DMISelectStatement)statement;
            select.addField ( schemaPrefix + "tblProviderData.id" );
            select.addField ( schemaPrefix + "tblProviderData.year" );
            select.addField ( schemaPrefix + "tblProviderData.value" );
            select.addTable ( schemaPrefix + "tblProviderData" );
            break;
		case _S_ProviderDataMetaData:
			select = (DMISelectStatement)statement;
			select.addField ( schemaPrefix + "vProviderDataMetaData.id" );
			select.addField ( schemaPrefix + "vProviderDataMetaData.subjectID" );
			select.addField ( schemaPrefix + "vProviderDataMetaData.name" );
			select.addField ( schemaPrefix + "vProviderDataMetaData.dataType" );
			select.addField ( schemaPrefix + "vProviderDataMetaData.subType" );
			select.addField ( schemaPrefix + "vProviderDataMetaData.units" );
			select.addField ( schemaPrefix + "vProviderDataMetaData.method" );
			select.addField ( schemaPrefix + "vProviderDataMetaData.subMethod" );
			select.addField ( schemaPrefix + "vProviderDataMetaData.source" );
			select.addField ( schemaPrefix + "vProviderDataMetaData.scenario" );
			select.addTable ( schemaPrefix + "vProviderDataMetaData" );
			break;
        case _S_ProviderDataMetaDataDistinctDataType:
            select = (DMISelectStatement)statement;
            select.addField ( schemaPrefix + "vProviderDataMetaData.dataType" );
            select.addTable ( schemaPrefix + "vProviderDataMetaData" );
            select.selectDistinct(true);
            break;
        case _S_ProviderDataMetaDataDistinctMethod:
            select = (DMISelectStatement)statement;
            select.addField ( schemaPrefix + "vProviderDataMetaData.method" );
            select.addTable ( schemaPrefix + "vProviderDataMetaData" );
            select.selectDistinct(true);
            break;
        case _S_ProviderDataMetaDataDistinctScenario:
            select = (DMISelectStatement)statement;
            select.addField ( schemaPrefix + "vProviderDataMetaData.scenario" );
            select.addTable ( schemaPrefix + "vProviderDataMetaData" );
            select.selectDistinct(true);
            break;
        case _S_ProviderDataMetaDataDistinctSource:
            select = (DMISelectStatement)statement;
            select.addField ( schemaPrefix + "vProviderDataMetaData.source" );
            select.addTable ( schemaPrefix + "vProviderDataMetaData" );
            select.selectDistinct(true);
            break;
        case _S_ProviderDataMetaDataDistinctSubjectID:
            select = (DMISelectStatement)statement;
            select.addField ( schemaPrefix + "vProviderDataMetaData.subjectID" );
            select.addTable ( schemaPrefix + "vProviderDataMetaData" );
            select.selectDistinct(true);
            break;
        case _S_ProviderDataMetaDataDistinctSubType:
            select = (DMISelectStatement)statement;
            select.addField ( schemaPrefix + "vProviderDataMetaData.subType" );
            select.addTable ( schemaPrefix + "vProviderDataMetaData" );
            select.selectDistinct(true);
            break;
		default:
			Message.printWarning ( 2, routine, "Unknown statement code: " + sqlNumber );
			break;
	}
}

// C FUNCTIONS

/**
Determine whether the user can create the database table/record, given a set of permissions.
@param DBUser_num the DBUser_num that owns the table/record in the database
@param DBGroup_num the GBGroup that owns the table/record in the database
@param permissions the permissions string (see permissions documentation above)
for the table/record being checked
@return true if the user can create the table/record
@throws Exception if an error occurs
*/
public boolean canCreate(int DBUser_num, int DBGroup_num, String permissions) 
throws Exception {
	String routine = "IppDMI.canCreate";
	int dl = 5;

	Message.printDebug(dl, routine, "canCreate(" + DBUser_num + ", " + DBGroup_num + ", " + permissions + ")");
	boolean canCreate = false;
/*
	if (_dbuser.getLogin().trim().equalsIgnoreCase("root")) {
		Message.printDebug(dl, routine, "Current user is root, can always create.");
		// root can do ANYTHING
		return true;
	}

	// start with the least-restrictive and move to the more-restrictive
	boolean canCreate = false;

	// first check other
	if (StringUtil.indexOfIgnoreCase(permissions, "OC+", 0) > -1) {
		Message.printDebug(dl, routine, "OC+ set, canCreate = true");
		canCreate = true;
	}
	// next check group
	if (DBGroup_num == _dbgroup._DBGroup_num) {
		canCreate = false;
		Message.printDebug(dl, routine, "Group num matches ...");
		if (StringUtil.indexOfIgnoreCase(permissions, "GC+", 0) > -1) {
			Message.printDebug(dl, routine, "GC+ set, canCreate = "
				+ "true");
			canCreate = true;
		}
		else {
			Message.printDebug(dl, routine, "GC+ not set.");
		}
	}

	// finally, user
	if (DBUser_num == _dbuser._DBUser_num) {
		canCreate = false;
		Message.printDebug(dl, routine, "User num matches ...");
		if (StringUtil.indexOfIgnoreCase(permissions, "UC+", 0) > -1) {
			Message.printDebug(dl, routine, "UC+ set, canCreate = true");
			canCreate = true;
		}
		else {
			Message.printDebug(dl, routine, "UC+ not set.");
		}
	}
	*/

	return canCreate;	
}

/**
Determine whether the user can delete the database table/record, given a set of permissions.
@param DBUser_num the DBUser_num that owns the table/record in the database
@param DBGroup_num the GBGroup that owns the table/record in the database
@param permissions the permissions string (see permissions documentation above)
for the table/record being checked
@return true if the user can delete the table/record
@throws Exception if an error occurs
*/
public boolean canDelete(int DBUser_num, int DBGroup_num, String permissions)
throws Exception {
	String routine = "IppDMI.canDelete";
	int dl = 5;

	Message.printDebug(dl, routine, "canDelete(" + DBUser_num + ", " + DBGroup_num + ", " + permissions + ")");

	boolean canDelete = false;
	/*if (_dbuser.getLogin().trim().equalsIgnoreCase("root")) {
		Message.printDebug(dl, routine, "Current user is root, can always delete.");
		// root can do ANYTHING
		return true;
	}

	// start with the least-restrictive and move to the more-restrictive
	boolean canDelete = false;

	// first check other
	if (StringUtil.indexOfIgnoreCase(permissions, "OD+", 0) > -1) {
		Message.printDebug(dl, routine, "OD+ set, canDelete = true");
		canDelete = true;
	}
	// next check group
	if (DBGroup_num == _dbgroup._DBGroup_num) {
		canDelete = false;
		Message.printDebug(dl, routine, "Group num matches ...");
		if (StringUtil.indexOfIgnoreCase(permissions, "GD+", 0) > -1) {
			Message.printDebug(dl, routine, "GD+ set, canDelete = true");
			canDelete = true;
		}
		else {
			Message.printDebug(dl, routine, "GD+ not set.");
		}
	}

	// finally, user
	if (DBUser_num == _dbuser._DBUser_num) {
		canDelete = false;
		Message.printDebug(dl, routine, "User num matches ...");
		if (StringUtil.indexOfIgnoreCase(permissions, "UD+", 0) > -1) {
			Message.printDebug(dl, routine, "UD+ set, canDelete = true");
			canDelete = true;
		}
		else {
			Message.printDebug(dl, routine, "UD+ not set.");
		}
	}
	*/

	return canDelete;	
}

/**
Determine whether the user can insert the database table/record, given a set of permissions.
@param DBUser_num the DBUser_num that owns the table/record in the database
@param DBGroup_num the GBGroup that owns the table/record in the database
@param permissions the permissions string (see permissions documentation above)
for the table/record being checked
@return true if the user can insert the table/record
@throws Exception if an error occurs
*/
public boolean canInsert(int DBUser_num, int DBGroup_num, String permissions)
throws Exception {
	String routine = "IppDMI.canInsert";
	int dl = 5;

	Message.printDebug(dl, routine, "canInsert(" + DBUser_num + ", " + DBGroup_num + ", " + permissions + ")");

	boolean canInsert = false;
	/*
	if (_dbuser.getLogin().trim().equalsIgnoreCase("root")) {
		Message.printDebug(dl, routine, "Current user is root, can always insert.");
		// root can do ANYTHING
		return true;
	}

	// start with the least-restrictive and move to the more-restrictive
	boolean canInsert = false;

	// first check other
	if (StringUtil.indexOfIgnoreCase(permissions, "OI+", 0) > -1) {
		Message.printDebug(dl, routine, "OI+ set, canInsert = true");
		canInsert = true;
	}
	// next check group
	if (DBGroup_num == _dbgroup._DBGroup_num) {
		canInsert = false;
		Message.printDebug(dl, routine, "Group num matches ...");
		if (StringUtil.indexOfIgnoreCase(permissions, "GI+", 0) > -1) {
			Message.printDebug(dl, routine, "GI+ set, canInsert = true");
			canInsert = true;
		}
		else {
			Message.printDebug(dl, routine, "GI+ not set.");
		}
	}

	// finally, user
	if (DBUser_num == _dbuser._DBUser_num) {
		canInsert = false;
		Message.printDebug(dl, routine, "User num matches ...");
		if (StringUtil.indexOfIgnoreCase(permissions, "UI+", 0) > -1) {
			Message.printDebug(dl, routine, "UI+ set, canInsert = true");
			canInsert = true;
		}
		else {
			Message.printDebug(dl, routine, "UI+ not set.");
		}
	}
	*/

	return canInsert;	
}

/**
Determine whether the user can read the database table/record, given a set of permissions.
@param DBUser_num the DBUser_num that owns the table/record in the database
@param DBGroup_num the GBGroup that owns the table/record in the database
@param permissions the permissions string (see permissions documentation above)
for the table/record being checked
@return true if the user can read the table/record
@throws Exception if an error occurs
*/
public boolean canRead(int DBUser_num, int DBGroup_num, String permissions)
throws Exception {
	String routine = "IppDMI.canRead";
	int dl = 5;

	Message.printDebug(dl, routine, "canRead(" + DBUser_num + ", " + DBGroup_num + ", " + permissions + ")");

	boolean canRead = true;
	/*
	if (_dbuser.getLogin().trim().equalsIgnoreCase("root")) {
		Message.printDebug(dl, routine, "Current user is root, can always read.");
		// root can do ANYTHING
		return true;
	}

	// start with the least-restrictive and move to the more-restrictive
	boolean canRead = false;

	// first check other
	if (StringUtil.indexOfIgnoreCase(permissions, "OR+", 0) > -1) {
		Message.printDebug(dl, routine, "OR+ set, canRead = true");
		canRead = true;
	}
	// next check group
	if (DBGroup_num == _dbgroup._DBGroup_num) {
		canRead = false;
		Message.printDebug(dl, routine, "Group num matches ...");
		if (StringUtil.indexOfIgnoreCase(permissions, "GR+", 0) > -1) {
			Message.printDebug(dl, routine, "GR+ set, canRead = true");
			canRead = true;
		}
		else {
			Message.printDebug(dl, routine, "GR+ not set.");
		}
	}

	// finally, user
	if (DBUser_num == _dbuser._DBUser_num) {
		canRead = false;
		Message.printDebug(dl, routine, "User num matches ...");
		if (StringUtil.indexOfIgnoreCase(permissions, "UR+", 0) > -1) {
			Message.printDebug(dl, routine, "UR+ set, canRead = true");
			canRead = true;
		}
		else {
			Message.printDebug(dl, routine, "UR+ not set.");
		}
	}
	*/

	return canRead;	
}

/**
Determine whether the user can update the database table/record, given a set of permissions.
@param DBUser_num the DBUser_num that owns the table/record in the database
@param DBGroup_num the GBGroup that owns the table/record in the database
@param permissions the permissions string (see permissions documentation above)
for the table/record being checked
@return true if the user can update the table/record
@throws Exception if an error occurs
*/
public boolean canUpdate(int DBUser_num, int DBGroup_num, String permissions)
throws Exception {
	String routine = "IppDMI.canUpdate";
	int dl = 5;

	Message.printDebug(dl, routine, "canUpdate(" + DBUser_num + ", " + DBGroup_num + ", " + permissions + ")");

	boolean canUpdate = false;
	/*
	if (_dbuser.getLogin().trim().equalsIgnoreCase("root")) {
		Message.printDebug(dl, routine, "Current user is root, can always read.");
		// root can do ANYTHING
		return true;
	}

	// start with the least-restrictive and move to the more-restrictive
	boolean canUpdate = false;

	// first check other
	if (StringUtil.indexOfIgnoreCase(permissions, "OU+", 0) > -1) {
		Message.printDebug(dl, routine, "OU+ set, canUpdate = true");
		canUpdate = true;
	}
	// next check group
	if (DBGroup_num == _dbgroup._DBGroup_num) {
		canUpdate = false;
		Message.printDebug(dl, routine, "Group num matches ...");
		if (StringUtil.indexOfIgnoreCase(permissions, "GU+", 0) > -1) {
			Message.printDebug(dl, routine, "GU+ set, canUpdate = true");
			canUpdate = true;
		}
		else {
			Message.printDebug(dl, routine, "GU+ not set.");
		}
	}

	// finally, user
	if (DBUser_num == _dbuser._DBUser_num) {
		canUpdate = false;
		Message.printDebug(dl, routine, "User num matches ...");
		if (StringUtil.indexOfIgnoreCase(permissions, "UU+", 0) > -1) {
			Message.printDebug(dl, routine, "UU+ set, canUpdate = true");
			canUpdate = true;
		}
		else {
			Message.printDebug(dl, routine, "UU+ not set.");
		}
	}
    */
	return canUpdate;	
}

/**
Determine whether the user can write to the table/record, given a set of 
permissions.  This method calls canInsert() and canUpdate() and returns true
only if both methods return true.  This method is meant for data records but
the canCreate() method may be needed to be called in cases where a high-level record is being created.
@param DBUser_num the DBUser_num that owns the table/record in the database
@param DBGroup_num the GBGroup that owns the table/record in the database
@param permissions the permissions string (see permissions documentation above)
for the table/record being checked
@return true if the user can write the table/record
@throws Exception if an error occurs
*/
public boolean canWrite(int DBUser_num, int DBGroup_num, String permissions) 
throws Exception {
	return (canInsert(DBUser_num, DBGroup_num, permissions) && canUpdate(DBUser_num, DBGroup_num, permissions));
}

// D FUNCTIONS

/**
Determine the database version by examining the table structure for the
database.  The following versions are known for IppDMI:
<ul>
</ul>
*/
public void determineDatabaseVersion() {
	// Default until more checks are added...
	String routine = "IppDMI.determineDatabaseVersion";
	boolean version_found = false;
	/*
	try {
		if (DMIUtil.databaseTableHasColumn(this,
			"State", "OperationStateRelation_num")) {
			setDatabaseVersion(_VERSION_030000_20041001);
			version_found = true;
		}
	}
	catch (Exception e) {
		// Ignore ...
		Message.printWarning(2, routine, e);
	}

	if (!version_found) {
		try {	
			if (DMIUtil.databaseTableHasColumn(this, 
				"Tables", "IsReference")) {
				setDatabaseVersion(_VERSION_020800_20030422);
				version_found = true;
			}
		}
		catch (Exception e) {
			// Ignore...
			Message.printWarning ( 2, routine, e );
		}
	}
	*/

	if (!version_found) {
		// Assume this...
		setDatabaseVersion ( _VERSION_010000_20090312 );
	}
	Message.printStatus ( 1, routine, "IPP database version determined to be at least " + getDatabaseVersion() );
}

/**
Wrapped for dmiDelete() method that prints the query being executed to Status(2) if IOUtil.testing() is on.
*/
public int dmiDelete(String q) 
throws java.sql.SQLException {
	if (IOUtil.testing()) {
		Message.printStatus(2, "", "" + q.toString());
	}

	return super.dmiDelete(q);
}

/**
Wrapped for dmiSelect() method that prints the query being executed to Status(2) if IOUtil.testing() is on.
*/
public ResultSet dmiSelect(DMISelectStatement q)
throws java.sql.SQLException {
	if (IOUtil.testing()) {
		Message.printStatus(2, "", "" + q.toString());
	}

	return super.dmiSelect(q);
}

/**
Wrapped for dmiSelect() method that prints the query being executed to Status(2) if IOUtil.testing() is on.
*/
public ResultSet dmiSelect(String q) 
throws java.sql.SQLException {
	if (IOUtil.testing()) {
		Message.printStatus(2, "", "" + q.toString());
	}

	return super.dmiSelect(q);
}

/**
Wrapped for dmiWrite() method that prints the query being executed to Status(2) if IOUtil.testing() is on.
*/
public int dmiWrite(String q) 
throws java.sql.SQLException {
	if (IOUtil.testing()) {
		Message.printStatus(2, "", "" + q.toString());
	}

	return super.dmiWrite(q);
}

/**
Wrapped for dmiWrite() method that prints the query being executed to Status(2) if IOUtil.testing() is on.
*/
public int dmiWrite(DMIWriteStatement q, int type)
throws Exception {
	if (IOUtil.testing()) {
		Message.printStatus(2, "", "" + q.toString());
	}

	return super.dmiWrite(q, type);
}

// E FUNCTIONS
// F FUNCTIONS

/**
Finalize for garbage collection.
@exception Throwable if there is an error.
*/
protected void finalize() throws Throwable {
	super.finalize();
}

// G FUNCTIONS

/**
Return a list of String containing useful database property information.
@return a list containing database properties as Strings.
@param level A numerical value that can be used to control the amount
of output.  A value of 3 returns full output, including database version,
history of changes, server information (e.g., for use in a properties dialog);
2 returns a concise output including server name (e.g., for use in the header
of an output file - NOT IMPLEMENTED; 1 for very concise output (e.g.,
the database name and version, for use in a product footer) - 
<b>NOT IMPLEMENTED</b>.
*/
public List getDatabaseProperties ( int level )
{	List v = new Vector ();
	v.add ( "Database Engine:  " + getDatabaseEngine() );
	if ( getDatabaseName() == null ) {
		v.add ( "Connect Method:  ODBC DSN" );
	}
	else {
	    v.add (
		"Connect Method:  JDBC using the following information  " );
		v.add ( "Database server:  " + getDatabaseServer() );
		v.add ( "Database name:  " + getDatabaseName() );
	}
	v.add ( "Database version appears to be (VVVVVVYYYYMMDD):  " + getDatabaseVersion() );
	v.add ( "" );
	v.add ( "Database history (most recent at top):" );
	v.add ( "" );
	return v;
}

/**
Returns "ColoradoIPP"
@return "ColoradoIPP"
*/
public String getDMIName() {
	return "ColoradoIPP";
}

/**
Return the unique list of data types for a subject type.
*/
public List<String> getDataMetaDataDataTypeList ( IPPSubjectType subjectType )
{
    return __dataMetaDataDataTypeList.get ( subjectType );
}

/**
Return the unique list of methods for a subject type.
*/
public List<String> getDataMetaDataMethodList ( IPPSubjectType subjectType )
{
    return __dataMetaDataMethodList.get ( subjectType );
}

/**
Return the unique list of scenarios for a subject type.
*/
public List<String> getDataMetaDataScenarioList ( IPPSubjectType subjectType )
{
    return __dataMetaDataScenarioList.get ( subjectType );
}

/**
Return the unique list of data sources for a subject type.
*/
public List<String> getDataMetaDataSourceList ( IPPSubjectType subjectType )
{
    return __dataMetaDataSourceList.get ( subjectType );
}

/**
Return the unique list of subject IDs for a subject type.
*/
public List<String> getDataMetaDataSubjectIDList ( IPPSubjectType subjectType )
{
    return __dataMetaDataSubjectIDList.get ( subjectType );
}

/**
Return the unique list of data subtypes for a subject type.
*/
public List<String> getDataMetaDataSubTypeList ( IPPSubjectType subjectType )
{
    return __dataMetaDataSubTypeList.get ( subjectType );
}

/**
Returns the database version numbers, each stored in a separate element of a
3-element String array.  For instance, database version 2.08.00 would be 
returned in the following array:<p>
<ul>
<li>[0] = "02"</li>
<li>[1] = "08"</li>
<li>[2] = "00"</li>
</ul>
@return the database version numbers, each stored in a separate element of a
3-element String array.  null is returned if the database version cannot be determined.
*/
public String[] getDatabaseVersionArray() {
	String[] version = new String[3];

	if (isDatabaseVersionAtLeast(_VERSION_010000_20090312)) {
		version[0] = "01";
		version[1] = "00";
		version[2] = "00";
		return version;
	}
	
	return null;
}

/**
Return the schema prefix for table and view names, necessary to function at the State with SQL Server 2005+.
*/
public String getSchemaPrefix()
{
    return __schemaPrefix;
}

/**
Return the list of subject types.
@return the list of subject types.
*/
public List getSubjectList ()
{
    // Make a copy
    List<String> subjectList = new Vector();
    for ( String subject: __subjectList ) {
        subjectList.add ( subject );
    }
    return subjectList;
}

/**
Return the name of the TSSupplier.  This is used for messages.
*/
public String getTSSupplierName() {
	return "ColoradoIPP";
}

// H FUNCTIONS
// I FUNCTIONS

// J FUNCTIONS
// K FUNCTIONS
// L FUNCTIONS
// M FUNCTIONS
// N FUNCTIONS
// O FUNCTIONS
// P FUNCTIONS
// Q FUNCTIONS

// R FUNCTIONS

/**
Reads all the CountyDataMetaData view records that match the given constraints.
@return a list of matching IPP_CountyDataMetaData objects.
@throws Exception if an error occurs
*/
public List readCountyDataMetaDataList( String name, String source, String dataType, String subType,
    String method, String subMethod, String scenario, boolean matchBlanks ) 
throws Exception {
    DMISelectStatement q = new DMISelectStatement ( this );
    buildSQL ( q, _S_CountyDataMetaData );
    String schemaPrefix = getSchemaPrefix();
    if ( (name != null) && !name.equals("") ) {
        q.addWhereClause(schemaPrefix+"vCountyDataMetaData.name = '" + escape(name) + "'");
    }
    else if ( (name == null) || name.equals("") && matchBlanks ) {
        q.addWhereClause(schemaPrefix+"vCountyDataMetaData.name = '" + escape("") + "'");
    }
    if ( (dataType != null) && !dataType.equals("") ) {
        q.addWhereClause(schemaPrefix+"vCountyDataMetaData.dataType = '" + escape(dataType) + "'");
    }
    else if ( (dataType == null) || dataType.equals("") && matchBlanks ) {
        q.addWhereClause(schemaPrefix+"vCountyDataMetaData.dataType = '" + escape("") + "'");
    }
    if ( (subType != null) && !subType.equals("") ) {
        q.addWhereClause(schemaPrefix+"vCountyDataMetaData.subType = '" + escape(subType) + "'");
    }
    else if ( (subType == null) || subType.equals("") && matchBlanks ) {
        q.addWhereClause(schemaPrefix+"vCountyDataMetaData.subType = '" + escape("") + "'");
    }
    if ( (method != null) && !method.equals("") ) {
        q.addWhereClause(schemaPrefix+"vCountyDataMetaData.method = '" + escape(method) + "'");
    }
    else if ( (method == null) || method.equals("") && matchBlanks ) {
        q.addWhereClause(schemaPrefix+"vCountyDataMetaData.method = '" + escape("") + "'");
    }
    if ( (subMethod != null) && !subMethod.equals("") ) {
        q.addWhereClause(schemaPrefix+"vCountyDataMetaData.subMethod = '" + escape(subMethod) + "'");
    }
    else if ( (subMethod == null) || subMethod.equals("") && matchBlanks ) {
        q.addWhereClause(schemaPrefix+"vCountyDataMetaData.subMethod = '" + escape("") + "'");
    }
    if ( (source != null) && !source.equals("") ) {
        q.addWhereClause(schemaPrefix+"vCountyDataMetaData.source = '" + escape(source) + "'");
    }
    else if ( (source == null) || source.equals("") && matchBlanks ) {
        q.addWhereClause(schemaPrefix+"vCountyDataMetaData.source = '" + escape("") + "'");
    }
    if ( (scenario != null) && !scenario.equals("") ) {
        q.addWhereClause(schemaPrefix+"vCountyDataMetaData.scenario = '" + escape(scenario) + "'");
    }
    else if ( (scenario == null) || scenario.equals("") && matchBlanks ) {
        q.addWhereClause(schemaPrefix+"vCountyDataMetaData.scenario = '" + escape("") + "'");
    }
    ResultSet rs = dmiSelect(q);
    List v = toCountyDataMetaDataList (rs);
    closeResultSet(rs);
    return v;
}

/**
Read the distinct data meta-data data type strings.
@param subjectType the subject type to which time series are connected.
*/
public List<String> readDataMetaDataDistinctDataTypeList ( IPPSubjectType subjectType )
throws Exception
{   String routine = getClass().getName() + ".readDataMetaDataDistinctDataTypeList";
    DMISelectStatement q = new DMISelectStatement ( this );
    if ( subjectType == IPPSubjectType.COUNTY ) {
        buildSQL ( q, _S_CountyDataMetaDataDistinctDataType );
    }
    else if ( subjectType == IPPSubjectType.PROJECT ) {
        buildSQL ( q, _S_ProjectDataMetaDataDistinctDataType );
    }
    else if ( subjectType == IPPSubjectType.PROVIDER ) {
        buildSQL ( q, _S_ProviderDataMetaDataDistinctDataType );
    }
    ResultSet rs = dmiSelect(q);
    List<String> v = DMIUtil.toStringList (rs);
    Message.printStatus ( 2, routine, "Got " + v.size() + " distinct data types for " + subjectType );
    closeResultSet(rs);
    return v;
}

/**
Read the distinct data meta-data method strings.
@param subjectType the subject type to which time series are connected.
*/
public List<String> readDataMetaDataDistinctMethodList ( IPPSubjectType subjectType )
throws Exception
{   String routine = getClass().getName() + ".readDataMetaDataDistinctMethodList";
    DMISelectStatement q = new DMISelectStatement ( this );
    if ( subjectType == IPPSubjectType.COUNTY ) {
        buildSQL ( q, _S_CountyDataMetaDataDistinctMethod );
    }
    else if ( subjectType == IPPSubjectType.PROJECT ) {
        buildSQL ( q, _S_ProjectDataMetaDataDistinctMethod );
    }
    else if ( subjectType == IPPSubjectType.PROVIDER ) {
        buildSQL ( q, _S_ProviderDataMetaDataDistinctMethod );
    }
    ResultSet rs = dmiSelect(q);
    List<String> v = DMIUtil.toStringList (rs);
    Message.printStatus ( 2, routine, "Got " + v.size() + " distinct methods for " + subjectType );
    closeResultSet(rs);
    return v;
}

/**
Read the distinct data meta-data scenario strings.
@param subjectType the subject type to which time series are connected.
*/
public List<String> readDataMetaDataDistinctScenarioList ( IPPSubjectType subjectType )
throws Exception
{   String routine = getClass().getName() + ".readDataMetaDataDistinctScenarioList";
    DMISelectStatement q = new DMISelectStatement ( this );
    if ( subjectType == IPPSubjectType.COUNTY ) {
        buildSQL ( q, _S_CountyDataMetaDataDistinctScenario );
    }
    else if ( subjectType == IPPSubjectType.PROJECT ) {
        buildSQL ( q, _S_ProjectDataMetaDataDistinctScenario );
    }
    else if ( subjectType == IPPSubjectType.PROVIDER ) {
        buildSQL ( q, _S_ProviderDataMetaDataDistinctScenario );
    }
    ResultSet rs = dmiSelect(q);
    List<String> v = DMIUtil.toStringList (rs);
    Message.printStatus ( 2, routine, "Got " + v.size() + " distinct scenarios for " + subjectType );
    closeResultSet(rs);
    return v;
}

/**
Read the distinct data meta-data source strings.
@param subjectType the subject type to which time series are connected.
*/
public List<String> readDataMetaDataDistinctSourceList ( IPPSubjectType subjectType )
throws Exception
{   String routine = getClass().getName() + ".readDataMetaDataDistinctSourceList";
    DMISelectStatement q = new DMISelectStatement ( this );
    if ( subjectType == IPPSubjectType.COUNTY ) {
        buildSQL ( q, _S_CountyDataMetaDataDistinctSource );
    }
    else if ( subjectType == IPPSubjectType.PROJECT ) {
        buildSQL ( q, _S_ProjectDataMetaDataDistinctSource );
    }
    else if ( subjectType == IPPSubjectType.PROVIDER ) {
        buildSQL ( q, _S_ProviderDataMetaDataDistinctSource );
    }
    ResultSet rs = dmiSelect(q);
    List<String> v = DMIUtil.toStringList (rs);
    Message.printStatus ( 2, routine, "Got " + v.size() + " distinct data sources for " + subjectType );
    closeResultSet(rs);
    return v;
}

/**
Read the distinct data meta-data subject ID strings.
@param subjectType the subject type to which time series are connected.
*/
public List<String> readDataMetaDataDistinctSubjectIDList ( IPPSubjectType subjectType )
throws Exception
{   String routine = getClass().getName() + ".readDataMetaDataDistinctSubjectIDList";
    DMISelectStatement q = new DMISelectStatement ( this );
    if ( subjectType == IPPSubjectType.COUNTY ) {
        buildSQL ( q, _S_CountyDataMetaDataDistinctSubjectID );
    }
    else if ( subjectType == IPPSubjectType.PROJECT ) {
        buildSQL ( q, _S_ProjectDataMetaDataDistinctSubjectID );
    }
    else if ( subjectType == IPPSubjectType.PROVIDER ) {
        buildSQL ( q, _S_ProviderDataMetaDataDistinctSubjectID );
    }
    ResultSet rs = dmiSelect(q);
    List<String> v = DMIUtil.toStringList (rs);
    Message.printStatus ( 2, routine, "Got " + v.size() + " distinct subject IDs for " + subjectType );
    closeResultSet(rs);
    return v;
}

/**
Read the distinct data meta-data data subtype strings.
@param subjectType the subject type to which time series are connected.
*/
public List<String> readDataMetaDataDistinctSubTypeList ( IPPSubjectType subjectType )
throws Exception
{   String routine = getClass().getName() + ".readDataMetaDataDistinctSubTypeList";
    DMISelectStatement q = new DMISelectStatement ( this );
    if ( subjectType == IPPSubjectType.COUNTY ) {
        buildSQL ( q, _S_CountyDataMetaDataDistinctSubType );
    }
    else if ( subjectType == IPPSubjectType.PROJECT ) {
        buildSQL ( q, _S_ProjectDataMetaDataDistinctSubType );
    }
    else if ( subjectType == IPPSubjectType.PROVIDER ) {
        buildSQL ( q, _S_ProviderDataMetaDataDistinctSubType );
    }
    ResultSet rs = dmiSelect(q);
    List<String> v = DMIUtil.toStringList (rs);
    Message.printStatus ( 2, routine, "Got " + v.size() + " distinct data subtypes for " + subjectType );
    closeResultSet(rs);
    return v;
}

/**
Read global data for the database, to keep in memory and improve performance.
*/
public void readGlobalData()
{   String routine = "IppDMI.readGlobalData";
    // Subjects (data object) types that have time series
    __subjectList = new Vector(); // Strings
    __subjectList.add ( "" + IPPSubjectType.COUNTY );
    //__subjectList.add ( "" + IPPSubjectType.BASIN );
    //__subjectList.add ( "" + IPPSubjectType.STATE );
    __subjectList.add ( "" + IPPSubjectType.PROVIDER );
    __subjectList.add ( "" + IPPSubjectType.PROJECT );
    
    for ( IPPSubjectType subjectType : IPPSubjectType.values() ) {
        // Distinct data sources and other filter choices for the different subject types
        try {
            __dataMetaDataDataTypeList.put( subjectType, readDataMetaDataDistinctDataTypeList ( subjectType ) );
        }
        catch ( Exception e ) {
            Message.printWarning ( 3, routine, "Error reading distinct " + subjectType + " data types (" + e + ")." );
        }
        try {
            __dataMetaDataMethodList.put( subjectType, readDataMetaDataDistinctMethodList ( subjectType ) );
        }
        catch ( Exception e ) {
            Message.printWarning ( 3, routine, "Error reading distinct " + subjectType + " methods (" + e + ")." );
        }
        try {
            __dataMetaDataScenarioList.put( subjectType, readDataMetaDataDistinctScenarioList ( subjectType ) );
        }
        catch ( Exception e ) {
            Message.printWarning ( 3, routine, "Error reading distinct " + subjectType + " scenarios (" + e + ")." );
        }
        try {
            __dataMetaDataSourceList.put( subjectType, readDataMetaDataDistinctSourceList ( subjectType ) );
        }
        catch ( Exception e ) {
            Message.printWarning ( 3, routine, "Error reading distinct " + subjectType + " data sources (" + e + ")." );
        }
        try {
            __dataMetaDataSubTypeList.put( subjectType, readDataMetaDataDistinctSubTypeList ( subjectType ) );
        }
        catch ( Exception e ) {
            Message.printWarning ( 3, routine, "Error reading distinct " + subjectType + " data subtypes (" + e + ")." );
        }
        try {
            __dataMetaDataSubjectIDList.put( subjectType, readDataMetaDataDistinctSubjectIDList ( subjectType ) );
        }
        catch ( Exception e ) {
            Message.printWarning ( 3, routine, "Error reading distinct " + subjectType + " subject IDs (" + e + ")." );
        }
    }
}

/**
Read IPP_IPPDataMetaData records for distinct data types, ordered by MeasLoc.identifier.
@return a list of objects of type RiversideDB_MeasTypeMeasLocGeoloc.
@param ifp An InputFilter_JPanel instance from which to retrieve where clause information.
@throws Exception if an error occurs
*/
public List readDataMetaDataList ( InputFilter_JPanel ifp, IPPSubjectType subjectType ) 
throws Exception
{
    DMISelectStatement q = new DMISelectStatement ( this );
    // For now focus on the county data...
    if ( subjectType == IPPSubjectType.COUNTY ) {
        buildSQL ( q, _S_CountyDataMetaData );
    }
    else if ( subjectType == IPPSubjectType.PROJECT ) {
        buildSQL ( q, _S_ProjectDataMetaData );
    }
    else if ( subjectType == IPPSubjectType.PROVIDER ) {
        buildSQL ( q, _S_ProviderDataMetaData );
    }
    /* TODO SAM 2010-04-29 May do something like this if the Subject is passed in from external code
    if ( (dataType != null) && (dataType.length() > 0) ) {
        // Data type has been specified so add a where clause
        q.addWhereClause("vCountyDataMetaData.dataType = '" + escape(dataType) + "'");
    }
    */
    // Add where clauses for the input filter
    String schemaPrefix = getSchemaPrefix();
    if ( ifp != null ) {
        List whereClauses = DMIUtil.getWhereClausesFromInputFilter(this, ifp);       
        // Add additional where clauses...
        if (whereClauses != null) {
            q.addWhereClauses(whereClauses);
        }
    }
    // Sort based on common use
    q.addOrderByClause ( schemaPrefix + "v" + subjectType + "DataMetaData.subjectID" );
    q.addOrderByClause ( schemaPrefix + "v" + subjectType + "DataMetaData.dataType" );
    q.addOrderByClause ( schemaPrefix + "v" + subjectType + "DataMetaData.subType" );
    q.addOrderByClause ( schemaPrefix + "v" + subjectType + "DataMetaData.method" );
    q.addOrderByClause ( schemaPrefix + "v" + subjectType + "DataMetaData.subMethod" );
    q.addOrderByClause ( schemaPrefix + "v" + subjectType + "DataMetaData.scenario" );
    q.addOrderByClause ( schemaPrefix + "v" + subjectType + "DataMetaData.source" );
    ResultSet rs = dmiSelect(q);
    List v = null;
    if ( subjectType == IPPSubjectType.COUNTY ) {
        v = toCountyDataMetaDataList (rs);
    }
    else if ( subjectType == IPPSubjectType.PROJECT ) {
        v = toProjectDataMetaDataList (rs);
    }
    else if ( subjectType == IPPSubjectType.PROVIDER ) {
        v = toProviderDataMetaDataList (rs);
    }
    closeResultSet(rs);
    return v;
}

/**
Reads all the ProjectDataMetaData view records that match the given constraints.
@return a list of matching IPP_ProjectDataMetaData objects.
@throws Exception if an error occurs
*/
public List readProjectDataMetaDataList( String subjectID, String source, String dataType, String subType,
    String method, String subMethod, String scenario, boolean matchBlanks ) 
throws Exception {
    DMISelectStatement q = new DMISelectStatement ( this );
    buildSQL ( q, _S_ProjectDataMetaData );
    String schemaPrefix = getSchemaPrefix();
    if ( (subjectID != null) && !subjectID.equals("") ) {
        q.addWhereClause(schemaPrefix+"vProjectDataMetaData.subjectID = " + subjectID );
    }
    if ( (dataType != null) && !dataType.equals("") ) {
        q.addWhereClause(schemaPrefix+"vProjectDataMetaData.dataType = '" + escape(dataType) + "'");
    }
    else if ( (dataType == null) || dataType.equals("") && matchBlanks ) {
        q.addWhereClause(schemaPrefix+"vProjectDataMetaData.dataType = '" + escape("") + "'");
    }
    if ( (subType != null) && !subType.equals("") ) {
        q.addWhereClause(schemaPrefix+"vProjectDataMetaData.subType = '" + escape(subType) + "'");
    }
    else if ( (subType == null) || subType.equals("") && matchBlanks ) {
        q.addWhereClause(schemaPrefix+"vProjectDataMetaData.subType = '" + escape("") + "'");
    }
    if ( (method != null) && !method.equals("") ) {
        q.addWhereClause(schemaPrefix+"vProjectDataMetaData.method = '" + escape(method) + "'");
    }
    else if ( (method == null) || method.equals("") && matchBlanks ) {
        q.addWhereClause(schemaPrefix+"vProjectDataMetaData.method = '" + escape("") + "'");
    }
    if ( (subMethod != null) && !subMethod.equals("") ) {
        q.addWhereClause(schemaPrefix+"vProjectDataMetaData.subMethod = '" + escape(subMethod) + "'");
    }
    else if ( (subMethod == null) || subMethod.equals("") && matchBlanks ) {
        q.addWhereClause(schemaPrefix+"vProjectDataMetaData.subMethod = '" + escape("") + "'");
    }
    if ( (source != null) && !source.equals("") ) {
        q.addWhereClause(schemaPrefix+"vProjectDataMetaData.source = '" + escape(source) + "'");
    }
    else if ( (source == null) || source.equals("") && matchBlanks ) {
        q.addWhereClause(schemaPrefix+"vProjectDataMetaData.source = '" + escape("") + "'");
    }
    if ( (scenario != null) && !scenario.equals("") ) {
        q.addWhereClause(schemaPrefix+"vProjectDataMetaData.scenario = '" + escape(scenario) + "'");
    }
    else if ( (scenario == null) || scenario.equals("") && matchBlanks ) {
        q.addWhereClause(schemaPrefix+"vProjectDataMetaData.scenario = '" + escape("") + "'");
    }
    ResultSet rs = dmiSelect(q);
    List v = toProjectDataMetaDataList (rs);
    closeResultSet(rs);
    return v;
}

/**
Reads all the ProviderDataMetaData view records that match the given constraints.
@param matchBlanks if true, then blank parameters will be matched in the query (returning a
more specific result); if false, more records will be returned
@return a list of matching IPP_ProviderDataMetaData objects.
@throws Exception if an error occurs
*/
public List readProviderDataMetaDataList( String subjectID, String source, String dataType, String subType,
    String method, String subMethod, String scenario, boolean matchBlanks ) 
throws Exception {
	DMISelectStatement q = new DMISelectStatement ( this );
	buildSQL ( q, _S_ProviderDataMetaData );
	String schemaPrefix = getSchemaPrefix();
    if ( (subjectID != null) && !subjectID.equals("") ) {
        q.addWhereClause(schemaPrefix+"vProviderDataMetaData.subjectID = " + subjectID );
    }
	if ( (dataType != null) && !dataType.equals("") ) {
        q.addWhereClause(schemaPrefix+"vProviderDataMetaData.dataType = '" + escape(dataType) + "'");
	}
	else if ( (dataType == null) || dataType.equals("") && matchBlanks ) {
        q.addWhereClause(schemaPrefix+"vProviderDataMetaData.dataType = '" + escape("") + "'");
    }
    if ( (subType != null) && !subType.equals("") ) {
        q.addWhereClause(schemaPrefix+"vProviderDataMetaData.subType = '" + escape(subType) + "'");
    }
    else if ( (subType == null) || subType.equals("") && matchBlanks ) {
        q.addWhereClause(schemaPrefix+"vProviderDataMetaData.subType = '" + escape("") + "'");
    }
    if ( (method != null) && !method.equals("") ) {
        q.addWhereClause(schemaPrefix+"vProviderDataMetaData.method = '" + escape(method) + "'");
    }
    else if ( (method == null) || method.equals("") && matchBlanks ) {
        q.addWhereClause(schemaPrefix+"vProviderDataMetaData.method = '" + escape("") + "'");
    }
    if ( (subMethod != null) && !subMethod.equals("") ) {
        q.addWhereClause(schemaPrefix+"vProviderDataMetaData.subMethod = '" + escape(subMethod) + "'");
    }
    else if ( (subMethod == null) || subMethod.equals("") && matchBlanks ) {
        q.addWhereClause(schemaPrefix+"vProviderDataMetaData.subMethod = '" + escape("") + "'");
    }
    if ( (source != null) && !source.equals("") ) {
        q.addWhereClause(schemaPrefix+"vProviderDataMetaData.source = '" + escape(source) + "'");
    }
    else if ( (source == null) || source.equals("") && matchBlanks ) {
        q.addWhereClause(schemaPrefix+"vProviderDataMetaData.source = '" + escape("") + "'");
    }
    if ( (scenario != null) && !scenario.equals("") ) {
        q.addWhereClause(schemaPrefix+"vProviderDataMetaData.scenario = '" + escape(scenario) + "'");
    }
    else if ( (scenario == null) || scenario.equals("") && matchBlanks ) {
        q.addWhereClause(schemaPrefix+"vProviderDataMetaData.scenario = '" + escape("") + "'");
    }
	ResultSet rs = dmiSelect(q);
	List v = toProviderDataMetaDataList (rs);
	closeResultSet(rs);
	return v;
}

/**
Read a time series given the id of the time series.  Other parameters specify metadata
to simplify creating the time series).
@param id the identifier for the "DataMetaData" (time series) record (this is NOT the subjectID!).
*/
public TS readTimeSeries ( String subject, long id, String name, String source, String dataType,
        String subType, String units, String method, String subMethod, String scenario, 
        DateTime reqStart, DateTime reqEnd, boolean readData )
throws Exception
{   DMISelectStatement q = new DMISelectStatement ( this );
    String schemaPrefix = getSchemaPrefix();
    // Query the time series data records...
    if ( subject.equalsIgnoreCase(""+IPPSubjectType.COUNTY)) {
        buildSQL ( q, _S_CountyData );
        q.addWhereClause(schemaPrefix+"tblCountyData.id = " + id );
        q.addOrderByClause(schemaPrefix+"tblCountyData.year");
    }
    else if ( subject.equalsIgnoreCase(""+IPPSubjectType.PROJECT)) {
        buildSQL ( q, _S_ProjectData );
        q.addWhereClause(schemaPrefix+"tblProjectData.id = " + id );
        q.addOrderByClause(schemaPrefix+"tblProjectData.year");
    }
    else if ( subject.equalsIgnoreCase(""+IPPSubjectType.PROVIDER)) {
        buildSQL ( q, _S_ProviderData );
        q.addWhereClause(schemaPrefix+"tblProviderData.id = " + id );
        q.addOrderByClause(schemaPrefix+"tblProviderData.year");
    }
    ResultSet rs = dmiSelect(q);
    List v = toTSDataList (rs);
    // Define the time series with metadata
    TS ts = new YearTS();
    if ( units != null ) {
        ts.setDataUnits ( units );
        ts.setDataUnitsOriginal ( units );
    }
    TSIdent tsident = null;
    if ( subject.equalsIgnoreCase(""+IPPSubjectType.COUNTY)) {
        // The name can be used in the identifier
        tsident = new TSIdent ( subject + ":" + name + "." + source + "." +
            dataType + "-" + subType + "-" + method + "-" + subMethod + ".Year." + scenario );
    }
    else {
        // The name is cumbersome and no short string identifier has been defined so use the id.
        tsident = new TSIdent ( subject + ":" + id + "." + source + "." +
            dataType + "-" + subType + "-" + method + "-" + subMethod + ".Year." + scenario );
    }
    ts.setIdentifier( tsident );
    ts.setDescription ( name );
    if ( (reqStart != null) && (reqEnd != null) ) {
        ts.setDate1 ( reqStart );
        ts.setDate2 ( reqEnd );
    }
    if ( v.size() >= 1 ) {
        // Define the data
        TSData d1 = (TSData)v.get(0);
        int size = v.size();
        TSData d2 = (TSData)v.get(size - 1);
        if ( (reqStart == null) && (reqEnd == null) ) {
            ts.setDate1 ( d1.getDate() );
            ts.setDate2 ( d2.getDate() );
        }
        ts.setDate1Original ( d1.getDate() );
        ts.setDate2Original ( d2.getDate() );
        if ( readData ) {
            ts.allocateDataSpace();
            for ( int i = 0; i < size; i++ ) {
                d1 = (TSData)v.get(i);
                ts.setDataValue(d1.getDate(), d1.getData());
            }
        }
    }
    closeResultSet(rs);
    return ts;
}

/**
Read a time series matching a time series identifier.
@return a time series or null if the time series is not defined in the database.
If no data records are available within the requested period, a call to
hasData() on the returned time series will return false.
@param tsidentString TSIdent string identifying the time series.
@param reqStart Optional date to specify the start of the query (specify 
null to read the entire time series).
@param reqEnd Optional date to specify the end of the query (specify 
null to read the entire time series).
@param reqUnits requested data units (specify null or blank string to return units from the database).
@param readData Indicates whether data should be read (specify false to only read header information).
@exception if there is an error reading the time series.
*/
public TS readTimeSeries (String tsidentString, DateTime reqStart,
    DateTime reqEnd, String reqUnits, boolean readData )
throws Exception
{	String routine = getClass().getName() + ".readTimeSeries";
    String message;
    // Declare a TSIdent object to do most of the parsing of the string...
    TSIdent tsident = new TSIdent(tsidentString);
    String subjectAndLocation = tsident.getLocation(); // Will be Subject:location
    String subject = StringUtil.getToken(subjectAndLocation,":",0,0);
    String location = StringUtil.getToken(subjectAndLocation,":",0,1);
    String dataTypeAll = tsident.getType(); // Will be main-subtype-method-submethod
    String dataType = null;
    String subType = null;
    String method = null;
    String subMethod = null;
    List<String>tokens = StringUtil.breakStringList(dataTypeAll,"-",0);
    if ( tokens.size() > 0 ) {
        dataType = tokens.get(0);
    }
    if ( tokens.size() > 1 ) {
        subType = tokens.get(1);
    }
    if ( tokens.size() > 2 ) {
        method = tokens.get(2);
    }
    if ( tokens.size() > 3 ) {
        subMethod = tokens.get(3);
    }
    String source = tsident.getSource();
    String scenario = tsident.getScenario();
    // Read the database record for the time series metadata, to get the units and name
    List<IPP_DataMetaData> dataList = null;
    if ( subject.equalsIgnoreCase(""+IPPSubjectType.COUNTY) ) {
        dataList = readCountyDataMetaDataList(location, source, dataType, subType, method, subMethod, scenario, true);
    }
    else if ( subject.equalsIgnoreCase(""+IPPSubjectType.PROJECT) ) {
        dataList = readProjectDataMetaDataList(location, source, dataType, subType, method, subMethod, scenario, true);
    }
    else if ( subject.equalsIgnoreCase(""+IPPSubjectType.PROVIDER) ) {
        dataList = readProviderDataMetaDataList(location, source, dataType, subType, method, subMethod, scenario, true);
    }
    if ( dataList.size() == 0 ) {
        message = "Time series identifier \"" + tsidentString + "\" does not match any " + subject +
            " time series meta data - unable to read data.";
        Message.printWarning(3, routine, message);
        throw new TimeSeriesNotFoundException(message);
    }
    else if ( dataList.size() > 1 ) {
        message = "Time series identifier \"" + tsidentString + "\" matches " + dataList.size() +
            " time series - should only match one.";
        Message.printWarning(3, routine, message);
        throw new InvalidParameterException(message);
    }
    IPP_DataMetaData data = dataList.get(0);
    // Get the internal data (time series) ID resulting from the time series (this is NOT the subjectID)
    long id = data.getID();
    // The following uses the id to read the time series.  All other parameters are provided to avoid
    // a second query (or to use values exactly matching the TSID).
    return readTimeSeries ( subject, id, data.getName(), source, dataType,
        subType, reqUnits, method, subMethod, scenario, 
        reqStart, reqEnd, readData );
}

/**
Unsupported.
*/
public TS readTimeSeries(TS req_ts, String fname, DateTime date1, DateTime date2, String req_units, boolean read_data)
throws Exception {
	return null;
}

/**
Unsupported.
*/
public List readTimeSeriesList(String fname, DateTime date1, DateTime date2, String req_units, boolean read_data)
throws Exception {
	return null;
}

/**
Unsupported.
*/
public List readTimeSeriesList(TSIdent tsident, String fname, DateTime date1, 
DateTime date2, String req_units, boolean read_data)
throws Exception {
	return null;
}

/**
Convert a ResultSet to a list of IPP_CountyDataMetaData.
@param rs ResultSet from a IPP_ProviderDataMetaData view query.
@throws Exception if an error occurs
*/
private List toCountyDataMetaDataList ( ResultSet rs ) 
throws Exception {
    List v = new Vector();
    IPP_CountyDataMetaData data = null;
    while ( rs.next() ) {
        data = new IPP_CountyDataMetaData();
        data.setSubject( "County" );
        toDataMetaData ( data, rs );
        v.add(data);
    }
    return v;
}

/**
Process a result set record into an IPP_DataMetaData object.
@throws SQLException 
*/
private void toDataMetaData ( IPP_DataMetaData data, ResultSet rs )
throws SQLException
{   String s;
    int index = 1;
    long l = rs.getLong ( index++ );
    if ( !rs.wasNull() ) {
        data.setID ( l );
    }
    l = rs.getLong ( index++ );
    if ( !rs.wasNull() ) {
        data.setSubjectID ( l);
    }
    s = rs.getString ( index++ );
    if ( !rs.wasNull() ) {
        data.setName ( s.trim() );
    }
    s = rs.getString ( index++ );
    if ( !rs.wasNull() ) {
        data.setDataType ( s.trim() );
    }
    s = rs.getString ( index++ );
    if ( !rs.wasNull() ) {
        data.setSubType ( s.trim() );
    }
    s = rs.getString ( index++ );
    if ( !rs.wasNull() ) {
        data.setUnits ( s.trim() );
    }
    s = rs.getString ( index++ );
    if ( !rs.wasNull() ) {
        data.setMethod ( s.trim() );
    }
    s = rs.getString ( index++ );
    if ( !rs.wasNull() ) {
        data.setSubMethod ( s.trim() );
    }
    s = rs.getString ( index++ );
    if ( !rs.wasNull() ) {
        data.setSource ( s.trim() );
    }
    s = rs.getString ( index++ );
    if ( !rs.wasNull() ) {
        data.setScenario ( s.trim() );
    }
}

/**
Convert a ResultSet to a list of IPP_ProjectDataMetaData.
@param rs ResultSet from a IPP_ProjectDataMetaData view query.
@throws Exception if an error occurs
*/
private List toProjectDataMetaDataList ( ResultSet rs ) 
throws Exception {
    List v = new Vector();
    IPP_ProjectDataMetaData data = null;
    while ( rs.next() ) {
        data = new IPP_ProjectDataMetaData();
        data.setSubject( "Project" );
        toDataMetaData ( data, rs );
        v.add(data);
    }
    return v;
}

/**
Convert a ResultSet to a list of IPP_ProviderDataMetaData.
@param rs ResultSet from a IPP_ProviderDataMetaData view query.
@throws Exception if an error occurs
*/
private List toProviderDataMetaDataList ( ResultSet rs ) 
throws Exception {
	List v = new Vector();
	IPP_ProviderDataMetaData data = null;
	while ( rs.next() ) {
		data = new IPP_ProviderDataMetaData();
		data.setSubject( "Provider" );
		toDataMetaData ( data, rs );
		v.add(data);
	}
	return v;
}

/**
Process a result set record into an IPP_DataMetaData object.
@throws SQLException 
*/
private List toTSDataList ( ResultSet rs )
throws SQLException
{   List v = new Vector();
    TSData data = null; // FIXME SAM 2009-03-12 Not extremely efficient since it uses DateTime
    int i;
    double d;
    while ( rs.next() ) {
        data = new TSData();
        int index = 1;
        rs.getLong ( index++ ); // foreign key to MetaData
        i = rs.getInt ( index++ );
        if ( !rs.wasNull() ) {
            DateTime dt = new DateTime(DateTime.PRECISION_YEAR);
            dt.setYear(i);
            data.setDate(dt);
        }
        d = rs.getDouble ( index++ );
        if ( !rs.wasNull() ) {
            data.setData ( d );
        }
        v.add(data);
    }
    return v;
}

}