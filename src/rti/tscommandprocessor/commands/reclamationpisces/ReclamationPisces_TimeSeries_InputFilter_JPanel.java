package rti.tscommandprocessor.commands.reclamationpisces;

import java.util.ArrayList;
import java.util.List;

import RTi.Util.GUI.InputFilter;
import RTi.Util.GUI.InputFilter_JPanel;
import RTi.Util.String.StringUtil;

/**
This class is an input filter for querying ReclamationPisces.
*/
@SuppressWarnings("serial")
public class ReclamationPisces_TimeSeries_InputFilter_JPanel extends InputFilter_JPanel //implements ItemListener, KeyListener
{

/**
ReclamationPisces database connection.
*/
private ReclamationPiscesDataStore datastore = null;

/**
Constructor.
@param dataStore the data store to use to connect to the Reclamation Pisces database.  Cannot be null.
@param numFilterGroups the number of filter groups to display
*/
public ReclamationPisces_TimeSeries_InputFilter_JPanel( ReclamationPiscesDataStore dataStore, int numFilterGroups )
{   super();
    this.datastore = dataStore;
    if ( this.datastore != null ) {
        ReclamationPiscesDMI dmi = (ReclamationPiscesDMI)dataStore.getDMI();
        setFilters ( dmi, numFilterGroups );
    }
}

/**
Set the filter data.  This method is called at setup and when refreshing the list with a new subject type.
*/
public void setFilters ( ReclamationPiscesDMI dmi, int numFilterGroups )
{   //String routine = getClass().getSimpleName() + ".setFilters";
    //String rd = dmi.getRightIdDelim();
    //String ld = dmi.getLeftIdDelim();

    List<InputFilter> filters = new ArrayList<InputFilter>();//new ArrayList<InputFilter>(); // TODO SAM 2015-08-20 Why is Vector required?

    // The database may have timed out so check here
    this.datastore.checkDatabaseConnection();

    // Always add blank to top of filter
    filters.add(new InputFilter("", "", StringUtil.TYPE_STRING, null, null, false)); // Blank

    filters.add(new InputFilter("Site - Description",
            "sitecatalog.description", "",
            StringUtil.TYPE_STRING, null, null, true));
    
    filters.add(new InputFilter("Site - Elevation",
            "sitecatalog.elevation", "",
            StringUtil.TYPE_DOUBLE, null, null, true));
    
    filters.add(new InputFilter("Site - ID",
        "sitecatalog.siteid", "",
        StringUtil.TYPE_STRING, null, null, true));
    
    filters.add(new InputFilter("Site - Latitude",
            "sitecatalog.latitude", "",
            StringUtil.TYPE_DOUBLE, null, null, true));
    
    filters.add(new InputFilter("Site - Longitude",
            "sitecatalog.longitude", "",
            StringUtil.TYPE_DOUBLE, null, null, true));

    filters.add(new InputFilter("Site - Region",
            "sitecatalog.agency_region", "",
            StringUtil.TYPE_STRING, null, null, true));
    
    filters.add(new InputFilter("Site - Responsibility",
            "sitecatalog.responsibility", "",
            StringUtil.TYPE_STRING, null, null, true));
    
    filters.add(new InputFilter("Site - State",
            "sitecatalog.state", "",
            StringUtil.TYPE_STRING, null, null, true));
    
    filters.add(new InputFilter("Site - Type",
            "sitecatalog.type", "",
            StringUtil.TYPE_STRING, null, null, true));
    
    filters.add(new InputFilter("Series - Name",
            "view_seriescatalog.name", "",
            StringUtil.TYPE_STRING, null, null, true));
    
    filters.add(new InputFilter("Series - Provider",
            "view_seriescatalog.provider", "",
            StringUtil.TYPE_STRING, null, null, true));
    
    filters.add(new InputFilter("Series - Table Name",
            "view_seriescatalog.tablename", "",
            StringUtil.TYPE_STRING, null, null, true));
    
    filters.add(new InputFilter("Series - Server",
            "view_seriescatalog.server", "",
            StringUtil.TYPE_STRING, null, null, true));
    
    setToolTipText("<html>Reclamation Pisces queries can be filtered based on site and time series metadata.</html>");
    int numVisible = 14;
    setInputFilters(filters, numFilterGroups, numVisible);
}

/**
Return the data store corresponding to this input filter panel.
@return the data store corresponding to this input filter panel.
*/
public ReclamationPiscesDataStore getDataStore ( )
{
    return this.datastore;
}

}