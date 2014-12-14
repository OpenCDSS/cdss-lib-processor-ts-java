package rti.tscommandprocessor.commands.reclamationhdb;

import java.io.IOException;

import RTi.DMI.AbstractDatabaseDataStore;
import RTi.DMI.DMI;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;

/**
Data store for Reclamation HDB database.  This class maintains the database connection information
in a general way.
@author sam
*/
public class ReclamationHDBDataStore extends AbstractDatabaseDataStore
{
    
/**
Construct a data store given a DMI instance, which is assumed to be open.
@param name identifier for the data store
@param description name for the data store
@param dmi DMI instance to use for the data store.
*/
public ReclamationHDBDataStore ( String name, String description, DMI dmi )
{
    setName ( name );
    setDescription ( description );
    setDMI ( dmi );
}
    
/**
Factory method to construct a data store connection from a properties file.
@param filename name of file containing property strings
*/
public static ReclamationHDBDataStore createFromFile ( String filename )
throws IOException, Exception
{
    // Read the properties from the file
    PropList props = new PropList ("");
    props.setPersistentName ( filename );
    props.readPersistent ( false );
    String name = IOUtil.expandPropertyForEnvironment("Name",props.getValue("Name"));
    String description = IOUtil.expandPropertyForEnvironment("Description",props.getValue("Description"));
    String databaseEngine = IOUtil.expandPropertyForEnvironment("DatabaseEngine",props.getValue("DatabaseEngine"));
    String databaseServer = IOUtil.expandPropertyForEnvironment("DatabaseServer",props.getValue("DatabaseServer"));
    String databaseName = IOUtil.expandPropertyForEnvironment("DatabaseName",props.getValue("DatabaseName"));
    String databasePort = IOUtil.expandPropertyForEnvironment("DatabasePort",props.getValue("DatabasePort"));
    String systemLogin = IOUtil.expandPropertyForEnvironment("SystemLogin",props.getValue("SystemLogin"));
    String systemPassword = IOUtil.expandPropertyForEnvironment("SystemPassword",props.getValue("SystemPassword"));
    int port = -1;
    if ( (databasePort != null) && !databasePort.equals("") ) {
        try {
            port = Integer.parseInt(databasePort);
        }
        catch ( NumberFormatException e ) {
            port = -1;
        }
    }
    String keepAliveSql = props.getValue("KeepAliveSQL");
    String keepAliveFrequency = props.getValue("KeepAliveFrequency");
    String tsidStyle = props.getValue("TSIDStyle");
    boolean tsidStyleSDI = true;
    if ( (tsidStyle != null) && tsidStyle.equalsIgnoreCase("CommonName") ) {
        tsidStyleSDI = false;
    }
    String readNHour = props.getValue("ReadNHourEndDateTime");
    boolean readNHourEndDateTime = true;
    if ( (readNHour != null) && readNHour.equalsIgnoreCase("StartDateTimePlusInterval") ) {
        readNHourEndDateTime = false;
    }
    String ConnectTimeout = props.getValue("ConnectTimeout");
    int connectTimeout = 0;
    if ( (ConnectTimeout != null) && !ConnectTimeout.equals("") ) {
        try {
            connectTimeout = Integer.parseInt(ConnectTimeout);
        }
        catch ( Exception e ) {
            connectTimeout = 0;
        }
    }
    String ReadTimeout = props.getValue("ReadTimeout");
    int readTimeout = 0;
    if ( (ReadTimeout != null) && !ReadTimeout.equals("") ) {
        try {
            readTimeout = Integer.parseInt(ReadTimeout);
        }
        catch ( Exception e ) {
            readTimeout = 0;
        }
    }
    
    // Get the properties and create an instance
    ReclamationHDB_DMI dmi = new ReclamationHDB_DMI ( databaseEngine, databaseServer, databaseName, port, systemLogin, systemPassword );
    dmi.setKeepAlive ( keepAliveSql, keepAliveFrequency ); // Needed for remote access to keep connection open
    dmi.setTSIDStyleSDI ( tsidStyleSDI );
    dmi.setReadNHourEndDateTime( readNHourEndDateTime );
    dmi.setLoginTimeout(connectTimeout);
    dmi.setReadTimeout(readTimeout);
    dmi.open();
    ReclamationHDBDataStore ds = new ReclamationHDBDataStore( name, description, dmi );
    return ds;
}

}