package rti.tscommandprocessor.commands.bndss;

import java.io.IOException;

import RTi.DMI.AbstractDatabaseDataStore;
import RTi.DMI.DMI;
import RTi.Util.IO.PropList;

/**
Data store for Colorado BNDSS database.  This class maintains the database connection information
in a general way.
@author sam
*/
public class ColoradoBNDSSDataStore extends AbstractDatabaseDataStore
{
    
/**
Construct a data store given a DMI instance, which is assumed to be open.
@param name identifier for the data store
@param description name for the data store
@param dmi DMI instance to use for the data store.
*/
public ColoradoBNDSSDataStore ( String name, String description, DMI dmi )
{
    setName ( name );
    setDescription ( description );
    setDMI ( dmi );
}
    
/**
Factory method to construct a data store connection from a properties file.
@param filename name of file containing property strings
*/
public static ColoradoBNDSSDataStore createFromFile ( String filename )
throws IOException, Exception
{
    // Read the properties from the file
    PropList props = new PropList ("");
    props.setPersistentName ( filename );
    props.readPersistent ( false );
    String name = props.getValue("Name");
    String description = props.getValue("Description");
    String databaseEngine = props.getValue("DatabaseEngine");
    String databaseServer = props.getValue("DatabaseServer");
    String databaseName = props.getValue("DatabaseName");
    String systemLogin = props.getValue("SystemLogin");
    String systemPassword = props.getValue("SystemPassword");
    
    // Get the properties and create an instance
    BNDSS_DMI dmi = new BNDSS_DMI ( databaseEngine, databaseServer, databaseName, -1, systemLogin, systemPassword );
    dmi.open();
    ColoradoBNDSSDataStore ds = new ColoradoBNDSSDataStore( name, description, dmi );
    return ds;
}

}