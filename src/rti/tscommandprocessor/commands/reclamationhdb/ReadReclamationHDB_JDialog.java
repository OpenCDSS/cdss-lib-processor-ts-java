package rti.tscommandprocessor.commands.reclamationhdb;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import riverside.datastore.DataStore;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.Util.GUI.DictionaryJDialog;
import RTi.Util.GUI.InputFilter_JPanel;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

/**
Editor for the ReadReclamationHDB() command.
*/
public class ReadReclamationHDB_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton __help_JButton = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private ReadReclamationHDB_Command __command = null;
private SimpleJComboBox __DataStore_JComboBox = null;
private SimpleJComboBox __DataType_JComboBox = null;
private SimpleJComboBox __Interval_JComboBox = null;
private JLabel __TimeZone_JLabel = null;
private JTextField __NHourIntervalOffset_JTextField = null;
private JTabbedPane __main_JTabbedPane = null;
private JTabbedPane __inner_JTabbedPane = null;
private JTabbedPane __sdi_JTabbedPane = null;
private SimpleJComboBox __SiteCommonName_JComboBox = null;
private SimpleJComboBox __DataTypeCommonName_JComboBox = null;
private JLabel __selectedSiteID_JLabel = null;
private JLabel __selectedSiteDataTypeID_JLabel = null;
private SimpleJComboBox __SiteDataTypeID_JComboBox = null;
private SimpleJComboBox __ModelName_JComboBox = null;
private SimpleJComboBox __ModelRunName_JComboBox = null;
private SimpleJComboBox __ModelRunDate_JComboBox = null;
private SimpleJComboBox __HydrologicIndicator_JComboBox = null;
private JLabel __selectedModelID_JLabel = null;
private JLabel __selectedModelRunID_JLabel = null;
private SimpleJComboBox __ModelRunID_JComboBox = null;
private SimpleJComboBox __EnsembleName_JComboBox = null;
private JTextField __OutputEnsembleID_JTextField = null; // This is the output EnsembleID, not HDB REF_ENSEMBLE.ENSEMBLE_ID
// TODO SAM 2016-04-29 Why are these disabled?
//private TSFormatSpecifiersJPanel __EnsembleTraceID_JTextField = null;
//private SimpleJComboBox __EnsembleModelName_JComboBox = null;
//private SimpleJComboBox __EnsembleModelRunDate_JComboBox = null;
//private JLabel __selectedEnsembleID_JLabel = null;
//private JLabel __selectedEnsembleModelID_JLabel = null;
//private JLabel __selectedEnsembleModelRunID_JLabel = null;
//private SimpleJComboBox __EnsembleModelRunID_JComboBox = null;
private JTextArea __Properties_JTextArea = null;
private JTextField __InputStart_JTextField;
private JTextField __InputEnd_JTextField;
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
			
private JTextArea __command_JTextArea = null; // Command as JTextArea
private InputFilter_JPanel __inputFilter_JPanel =null;
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK was pressed when closing the dialog.
private JFrame __parent = null;

private boolean __ignoreEvents = false; // Used to ignore cascading events when initializing the components

private ReclamationHDB_DMI __dmi = null; // ReclamationHDB_DMI to do queries.

private List<ReclamationHDB_SiteDataType> __siteDataTypeList = new ArrayList<ReclamationHDB_SiteDataType>(); // Corresponds to displayed list
private List<ReclamationHDB_Model> __modelList = new ArrayList<ReclamationHDB_Model>(); // Corresponds to displayed list (has model_id)
private List<ReclamationHDB_ModelRun> __modelRunList = new ArrayList<ReclamationHDB_ModelRun>(); // Corresponds to models matching model_id

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public ReadReclamationHDB_JDialog ( JFrame parent, ReadReclamationHDB_Command command )
{	super(parent, true);

	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param e ActionEvent object
*/
public void actionPerformed( ActionEvent e )
{   if ( __ignoreEvents ) {
        return; // Startup.
    }
    Object o = e.getSource();
    if ( o == __help_JButton ) {
		TSCommandProcessorUtil.displayCommandDocumentation(__command);
	}
	else if ( o == __cancel_JButton ) {
        response ( false );
    }
    else if ( o == __ok_JButton ) {
        refresh ();
        checkInput();
        if ( !__error_wait ) {
            response ( true );
        }
    }
    else if ( e.getActionCommand().equalsIgnoreCase("EditProperties") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String Properties = __Properties_JTextArea.getText().trim();
        String [] notes = {
            "Time series properties will be assigned after reading.",
            "Use % specifiers to assign properties from internal time series data.",
            "Use ${ts:Property} to assign to a time series property from HDB metadata.",
            "Set the TableViewHeaderFormat property to format columns in time series tables."
        };
        String properties = (new DictionaryJDialog ( __parent, true, Properties, "Edit Properties Parameter",
            notes, "Property", "Property Value",10)).response();
        if ( properties != null ) {
            __Properties_JTextArea.setText ( properties );
            refresh();
        }
    }
}

/**
Refresh the site common name choices in response to the currently selected ReclamationHDB datastore.
*/
private void actionPerformedDataStoreSelected ( )
{
    if ( __DataStore_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    setDMIForSelectedDataStore();
    //Message.printStatus(2, "", "Selected datastore " + __dataStore + " __dmi=" + __dmi );
    // Now populate the data type choices corresponding to the datastore
    populateSiteCommonNameChoices ( __dmi );
    populateSiteDataTypeIDChoices ( __dmi );
    // Model run time series can be determined once the interval and site_datatype_id are selected
    // Once the MRI list is determined, the model names start the cascade
    populateModelRunIDChoices ( __dmi );
    populateModelNameChoices ( __dmi );
    // Ensemble model run time series can be determined once the interval and site_datatype_id are selected
    // Once the ensemble MRI list is determined, the ensemble model names start the cascade
    //populateEnsembleModelRunIDChoices ( __dmi );
    populateEnsembleNameChoices ( __dmi );
    //populateEnsembleModelNameChoices ( __dmi );
    populateTimeZoneLabel ( __dmi );
}

/**
Refresh the query choices for the currently selected ReclamationHDB data type common name.
*/
private void actionPerformedDataTypeCommonNameSelected ( )
{
    if ( __DataTypeCommonName_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    // No further action needed to populate choices but show selected site_datatype_id for those who
    // are familiar with the database internals
    updateSiteIDTextFields();
}

/**
Refresh the query choices for the currently selected ReclamationHDB ensemble name.
*/
private void actionPerformedEnsembleNameSelected ( )
{
    if ( __EnsembleName_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    // No further action needed to populate choices but show selected ensemble_id those who
    // are familiar with the database internals
    updateEnsembleIDTextFields ();
    // Now populate the model run choices corresponding to the ensemble name, which will cascade to
    // populating the other choices
    //populateEnsembleModelNameChoices ( __dmi );
}

/**
Refresh the query choices for the currently selected ReclamationHDB model name.
*/
/*
private void actionPerformedEnsembleModelNameSelected ( )
{
    if ( __EnsembleModelName_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    // No further action needed to populate choices but show selected model_id those who
    // are familiar with the database internals
    updateEnsembleIDTextFields ();
    // Now populate the model run choices corresponding to the model name, which will cascade to
    // populating the other choices
    // This is not a selectable item with ensembles - just key off of model run name
    //populateModelRunNameChoices ( __dmi );
}
*/

/**
Refresh the query choices for the currently selected ReclamationHDB hydrologic indicator name.
*/
private void actionPerformedHydrologicIndicatorSelected ( )
{
    if ( __HydrologicIndicator_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    // No further action needed to populate choices but show selected model_run_id for those who
    // are familiar with the database internals
    //updateModelIDTextFields ();
    populateModelRunDateChoices ( __dmi );
}

/**
Refresh the query choices for the currently selected interval.
*/
private void actionPerformedIntervalSelected ( )
{
    if ( __Interval_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    // Now populate the model run ID choices corresponding to the site data type and data type
    // If the site data type is blank the MRI choices will not be filled
    populateModelRunIDChoices ( __dmi );
    populateModelNameChoices ( __dmi );
    // Also populate ensemble list
    // TODO SAM 2013-09-30 Evaluate how to avoid double work on query
    populateEnsembleNameChoices(__dmi);
}

/**
Refresh the query choices for the currently selected ReclamationHDB model name.
*/
private void actionPerformedModelNameSelected ( )
{   ReclamationHDB_DMI rdmi = getReclamationHDB_DMI();
    if ( (rdmi == null) || (__ModelName_JComboBox.getSelected() == null) ) {
        // Startup initialization
        return;
    }
    // No further action needed to populate choices but show selected model_id those who
    // are familiar with the database internals
    updateModelIDTextFields ();
    // Now populate the model run choices corresponding to the model name, which will cascade to
    // populating the other choices
    populateModelRunNameChoices ( __dmi );
}

/**
Refresh the query choices for the currently selected ReclamationHDB model run date.
*/
private void actionPerformedModelRunDateSelected ( )
{
    if ( __ModelRunDate_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    // No further action needed to populate choices but show selected model_run_id for those who
    // are familiar with the database internals
    updateModelIDTextFields ();
    // Now populate the hydrologic indicator choices corresponding to the model run date
    //populateHydrologicIndicatorChoices ( __dmi );
}

/**
Refresh the query choices for the currently selected ReclamationHDB model run name.
*/
private void actionPerformedModelRunNameSelected ( )
{
    if ( __ModelRunName_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    // No further action needed to populate choices but show selected model_run_id for those who
    // are familiar with the database internals
    updateModelIDTextFields ();
    // Now populate the model run choices corresponding to the model run name
    //populateModelRunDateChoices ( __dmi );
    populateHydrologicIndicatorChoices( __dmi );
}

/**
Refresh the query choices for the currently selected ReclamationHDB site common name.
*/
private void actionPerformedSiteCommonNameSelected ( )
{
    if ( __SiteCommonName_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    // Now populate the data type choices corresponding to the site common name
    populateDataTypeCommonNameChoices ( __dmi );
    updateSiteIDTextFields();
}

/**
Refresh the query choices for the currently selected ReclamationHDB site common name.
*/
private void actionPerformedSiteDataTypeIDSelected ( )
{
    if ( __SiteDataTypeID_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    // Now populate the model run ID choices corresponding to the site data type
    populateModelRunIDChoices ( __dmi );
    populateModelNameChoices ( __dmi );
    // Also populate ensemble list
    // TODO SAM 2013-09-30 Evaluate how to avoid double work on query
    populateEnsembleNameChoices(__dmi);
}

// Start event handlers for DocumentListener...

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void changedUpdate ( DocumentEvent e )
{   checkGUIState();
    refresh();
}

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void insertUpdate ( DocumentEvent e )
{   checkGUIState();
    refresh();
}

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void removeUpdate ( DocumentEvent e )
{   checkGUIState();
    refresh();
}

// ...End event handlers for DocumentListener

/**
Check the state of the dialog, disabling/enabling components as appropriate.
*/
private void checkGUIState()
{
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{   if ( __ignoreEvents ) {
        // Startup
        return;
    }
    // Put together a list of parameters to check...
	PropList props = new PropList ( "" );
	__error_wait = false;
	String DataStore = __DataStore_JComboBox.getSelected();
	if ( DataStore.length() > 0 ) {
		props.set ( "DataStore", DataStore );
	}
    String DataType = __DataType_JComboBox.getSelected();
    if ( DataType.length() > 0 ) {
        props.set ( "DataType", DataType );
    }
    String Interval = __Interval_JComboBox.getSelected();
    if ( Interval.length() > 0 ) {
        props.set ( "Interval", Interval );
    }
    String NHourIntervalOffset = __NHourIntervalOffset_JTextField.getText().trim();
    if ( NHourIntervalOffset.length() > 0 ) {
        props.set ( "NHourIntervalOffset", NHourIntervalOffset );
    }
	int numWhere = __inputFilter_JPanel.getNumFilterGroups();
	for ( int i = 1; i <= numWhere; i++ ) {
	    String where = getWhere ( i - 1 );
	    if ( where.length() > 0 ) {
	        props.set ( "Where" + i, where );
	    }
    }
    String SiteCommonName = __SiteCommonName_JComboBox.getSelected();
    if ( (SiteCommonName != null) && (SiteCommonName.length() > 0) ) {
        props.set ( "SiteCommonName", SiteCommonName );
    }
    String DataTypeCommonName = __DataTypeCommonName_JComboBox.getSelected();
    if ( (DataTypeCommonName != null) && (DataTypeCommonName.length() > 0) ) {
        props.set ( "DataTypeCommonName", DataTypeCommonName );
    }
    String SiteDataTypeID = getSelectedSiteDataTypeID();
    if ( (SiteDataTypeID != null) && (SiteDataTypeID.length() > 0) ) {
        props.set ( "SiteDataTypeID", SiteDataTypeID );
    }
    String ModelName = __ModelName_JComboBox.getSelected();
    if ( (ModelName != null) && (ModelName.length() > 0) ) {
        props.set ( "ModelName", ModelName );
    }
    String ModelRunName = __ModelRunName_JComboBox.getSelected();
    if ( (ModelRunName != null) && (ModelRunName.length() > 0) ) {
        props.set ( "ModelRunName", ModelRunName );
    }
    String ModelRunDate = __ModelRunDate_JComboBox.getSelected();
    if ( (ModelRunDate != null) && (ModelRunDate.length() > 0) ) {
        props.set ( "ModelRunDate", ModelRunDate );
    }
    String HydrologicIndicator = __HydrologicIndicator_JComboBox.getSelected();
    if ( HydrologicIndicator.length() > 0 ) {
        props.set ( "HydrologicIndicator", HydrologicIndicator );
    }
    String ModelRunID = getSelectedModelRunID();
    if ( (ModelRunID != null) && (ModelRunID.length() > 0) ) {
        props.set ( "ModelRunID", ModelRunID );
    }
    String EnsembleName = __EnsembleName_JComboBox.getSelected();
    if ( (EnsembleName != null) && (EnsembleName.length() > 0) ) {
        props.set ( "EnsembleName", EnsembleName );
    }
    String OutputEnsembleID = __OutputEnsembleID_JTextField.getText();
    if ( (OutputEnsembleID != null) && (OutputEnsembleID.length() > 0) ) {
        props.set ( "OutputEnsembleID", OutputEnsembleID );
    }
    //String EnsembleTraceID = __EnsembleTraceID_JTextField.getText().trim();
    //if ( EnsembleTraceID.length() > 0 ) {
    //    props.set ( "EnsembleTraceID", EnsembleTraceID );
    //}
    /*
    String EnsembleModelName = __EnsembleModelName_JComboBox.getSelected();
    if ( (EnsembleModelName != null) && (EnsembleModelName.length() > 0) ) {
        props.set ( "EnsembleModelName", EnsembleModelName );
    }
    String EnsembleModelRunDate = __EnsembleModelRunDate_JComboBox.getSelected();
    if ( (EnsembleModelRunDate != null) && (EnsembleModelRunDate.length() > 0) ) {
        props.set ( "EnsembleModelRunDate", EnsembleModelRunDate );
    }
    String EnsembleModelRunID = getSelectedEnsembleModelRunID();
    if ( (EnsembleModelRunID != null) && (EnsembleModelRunID.length() > 0) ) {
        props.set ( "EnsembleModelRunID", EnsembleModelRunID );
    }
    */
    String Properties = __Properties_JTextArea.getText().trim().replace("\n"," ");
    if ( Properties.length() > 0 ) {
        props.set ( "Properties", Properties );
    }
	String InputStart = __InputStart_JTextField.getText().trim();
	if ( InputStart.length() > 0 ) {
		props.set ( "InputStart", InputStart );
	}
	String InputEnd = __InputEnd_JTextField.getText().trim();
	if ( InputEnd.length() > 0 ) {
		props.set ( "InputEnd", InputEnd );
	}
    String Alias = __Alias_JTextField.getText().trim();
    if ( Alias.length() > 0 ) {
        props.set ( "Alias", Alias );
    }
	try {
	    // This will warn the user...
		__command.checkCommandParameters ( props, null, 1 );
	}
	catch ( Exception e ) {
		// The warning would have been printed in the check code.
		__error_wait = true;
	}
}

/**
Commit the edits to the command.  In this case the command parameters have
already been checked and no errors were detected.
*/
private void commitEdits ()
{	String DataStore = __DataStore_JComboBox.getSelected();
    String DataType = __DataType_JComboBox.getSelected();
    String Interval = __Interval_JComboBox.getSelected();
    String NHourIntervalOffset = __NHourIntervalOffset_JTextField.getText().trim();
	__command.setCommandParameter ( "DataStore", DataStore );
	__command.setCommandParameter ( "DataType", DataType );
	__command.setCommandParameter ( "Interval", Interval );
	__command.setCommandParameter ( "NHourIntervalOffset", NHourIntervalOffset );
	String delim = ";";
	int numWhere = __inputFilter_JPanel.getNumFilterGroups();
	for ( int i = 1; i <= numWhere; i++ ) {
	    String where = getWhere ( i - 1 );
	    if ( where.startsWith(delim) ) {
	        where = "";
	    }
	    __command.setCommandParameter ( "Where" + i, where );
	}
    String SiteCommonName = __SiteCommonName_JComboBox.getSelected();
    __command.setCommandParameter ( "SiteCommonName", SiteCommonName );
    String DataTypeCommonName = __DataTypeCommonName_JComboBox.getSelected();
    __command.setCommandParameter ( "DataTypeCommonName", DataTypeCommonName );
    String SiteDataTypeID = getSelectedSiteDataTypeID();
    __command.setCommandParameter ( "SiteDataTypeID", SiteDataTypeID );
    String ModelName = __ModelName_JComboBox.getSelected();
    __command.setCommandParameter ( "ModelName", ModelName );
    String ModelRunName = __ModelRunName_JComboBox.getSelected();
    __command.setCommandParameter ( "ModelRunName", ModelRunName );
    String ModelRunDate = __ModelRunDate_JComboBox.getSelected();
    __command.setCommandParameter ( "ModelRunDate", ModelRunDate );
    String HydrologicIndicator = __HydrologicIndicator_JComboBox.getSelected();
    __command.setCommandParameter ( "HydrologicIndicator", HydrologicIndicator );
    String ModelRunID = getSelectedModelRunID();
    __command.setCommandParameter ( "ModelRunID", ModelRunID );
    String EnsembleName = __EnsembleName_JComboBox.getSelected();
    __command.setCommandParameter ( "EnsembleName", EnsembleName );
    String OutputEnsembleID = __OutputEnsembleID_JTextField.getText();
    __command.setCommandParameter ( "OutputEnsembleID", OutputEnsembleID );
    //String EnsembleTraceID = __EnsembleTraceID_JTextField.getText().trim();
    //__command.setCommandParameter ( "EnsembleTraceID", EnsembleTraceID );
    /*
    String EnsembleModelName = __EnsembleModelName_JComboBox.getSelected();
    __command.setCommandParameter ( "EnsembleModelName", EnsembleModelName );
    String EnsembleModelRunDate = __EnsembleModelRunDate_JComboBox.getSelected();
    __command.setCommandParameter ( "EnsembleModelRunDate", EnsembleModelRunDate );
    String EnsembleModelRunID = getSelectedEnsembleModelRunID();
    __command.setCommandParameter ( "EnsembleModelRunID", EnsembleModelRunID );
    */
    String Properties = __Properties_JTextArea.getText().trim().replace("\n"," ");
    __command.setCommandParameter ( "Properties", Properties );
	String InputStart = __InputStart_JTextField.getText().trim();
	__command.setCommandParameter ( "InputStart", InputStart );
	String InputEnd = __InputEnd_JTextField.getText().trim();
	__command.setCommandParameter ( "InputEnd", InputEnd );
	String Alias = __Alias_JTextField.getText().trim();
    __command.setCommandParameter ( "Alias", Alias );
}

/**
Return the ReclamationHDB_DMI that is currently being used for database interaction,
based on the selected datastore.
*/
private ReclamationHDB_DMI getReclamationHDB_DMI ()
{
    return __dmi;
}

/**
Return the selected datastore, used to provide intelligent parameter choices.
@return the selected datastore, or null if nothing selected (or none available)
*/
private ReclamationHDBDataStore getSelectedDataStore ()
{   
	// Get all matching data stores, not just the active ones
    List<DataStore> dataStoreList =
        ((TSCommandProcessor)__command.getCommandProcessor()).getDataStoresByType(
            ReclamationHDBDataStore.class, false );
    String dataStoreNameSelected = __DataStore_JComboBox.getSelected();
    if ( (dataStoreNameSelected != null) && !dataStoreNameSelected.equals("") ) {
        for ( DataStore dataStore : dataStoreList ) {
            if ( dataStore.getName().equalsIgnoreCase(dataStoreNameSelected) ) {
            	// Check the connection in case the connection timed out.
            	ReclamationHDBDataStore ds = (ReclamationHDBDataStore)dataStore;
            	ds.checkDatabaseConnection();
                return ds;
            }
        }
    }
    return null;
}

/**
Return the selected ensemble model ID corresponding to the selected ensemble model name by querying the database.
@return the selected model ID or -1 if the model ID cannot be determined.
*/
/*
private int getSelectedEnsembleModelID()
{   String modelName = __EnsembleModelName_JComboBox.getSelected();
    ReclamationHDB_DMI rdmi = getReclamationHDB_DMI();
    if ( (rdmi == null) || (modelName == null) ) {
        return -1;
    }
    // Get the corresponding model object
    try {
        List<ReclamationHDB_Model> models = rdmi.readHdbModelList(modelName);
        if ( models.size() == 1 ) {
            return models.get(0).getModelID();
        }
    }
    catch ( Exception e ) {
        // Should not happen
        Message.printWarning(3,"",e);
    }
    return -1;
}
*/

/**
Return the selected ensemble model run ID, used to provide intelligent parameter choices.
The displayed format is:  "MRI - Other information"
@return the selected MRI, or "" if nothing selected
*/
/*
private String getSelectedEnsembleModelRunID()
{   String mri = __EnsembleModelRunID_JComboBox.getSelected();
    if ( mri == null ) {
        return "";
    }
    else if ( mri.indexOf(" ") > 0 ) {
        return mri.substring(0,mri.indexOf(" ")).trim();
    }
    else {
        return mri.trim();
    }
}
*/

/**
Return the selected model ID corresponding to the selected model name by querying the database.
@return the selected model ID or -1 if the model ID cannot be determined.
*/
private int getSelectedModelID()
{   String modelName = __ModelName_JComboBox.getSelected();
    ReclamationHDB_DMI rdmi = getReclamationHDB_DMI();
    if ( (rdmi == null) || (modelName == null) ) {
        return -1;
    }
    // Get the corresponding model object
    try {
        List<ReclamationHDB_Model> models = rdmi.readHdbModelList(modelName);
        if ( models.size() == 1 ) {
            return models.get(0).getModelID();
        }
    }
    catch ( Exception e ) {
        // Should not happen
        Message.printWarning(3,"",e);
    }
    return -1;
}

/**
Return the selected model run ID, used to provide intelligent parameter choices.
The displayed format is:  "MRI - Other information"
@return the selected MRI, or "" if nothing selected
*/
private String getSelectedModelRunID()
{   String mri = __ModelRunID_JComboBox.getSelected();
    if ( mri == null ) {
        return "";
    }
    else if ( mri.indexOf(" ") > 0 ) {
        return mri.substring(0,mri.indexOf(" ")).trim();
    }
    else {
        return mri.trim();
    }
}

/**
Return the selected SDI, used to provide intelligent parameter choices.
The displayed format is:  "SDI - Other information"
@return the selected SDMI, or "" if nothing selected
*/
private String getSelectedSiteDataTypeID()
{   String sdi = __SiteDataTypeID_JComboBox.getSelected();
    if ( sdi == null ) {
        return "";
    }
    else if ( sdi.indexOf(" ") > 0 ) {
        return sdi.substring(0,sdi.indexOf(" ")).trim();
    }
    else {
        return sdi.trim();
    }
}

/**
Return the "WhereN" parameter for the requested input filter.
@return the "WhereN" parameter for the requested input filter.
@param ifg the Input filter to process (zero index).
*/
private String getWhere ( int ifg )
{
	// TODO SAM 2006-04-24 Need to enable other input filter panels
	String delim = ";";	// To separate input filter parts
	InputFilter_JPanel filter_panel = __inputFilter_JPanel;
	String where = filter_panel.toString(ifg,delim).trim();
	return where;
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, ReadReclamationHDB_Command command )
{	String routine = getClass().getSimpleName() + ".initialize";
    __parent = parent;
	__command = command;
	CommandProcessor processor = __command.getCommandProcessor();

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int yMain = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"Read one or more time series, or an ensemble, from a Reclamation HDB database."),
    	0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"Constrain the query by specifying time series metadata to match." ), 
    	0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Specify date/times using the format YYYY-MM-DD hh:mm:ss, to a precision appropriate for the data " +
        "interval (default=input period from SetInputPeriod())."),
        0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __TimeZone_JLabel = new JLabel();
    JGUIUtil.addComponent(main_JPanel, __TimeZone_JLabel,
        0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    __ignoreEvents = true; // So that a full pass of initialization can occur
   	
   	// List available datastores of the correct type
   	
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Datastore:"),
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStore_JComboBox = new SimpleJComboBox ( false );
    // Get all datastores for ReclamationHDB, even if not open (DMI open will be checked when selected).
    List<DataStore> dataStoreList = ((TSCommandProcessor)processor).getDataStoresByType(
        ReclamationHDBDataStore.class, false );
    List<String> datastoreChoices = new ArrayList<String>();
    for ( DataStore dataStore: dataStoreList ) {
    	datastoreChoices.add ( dataStore.getName() );
    }
    if ( __DataStore_JComboBox.getItemCount() > 0 ) {
        __DataStore_JComboBox.select ( 0 );
    }
    __DataStore_JComboBox.setData(datastoreChoices);
    __DataStore_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __DataStore_JComboBox,
        1, yMain, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - datastore containing data."), 
        3, yMain, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Intervals are hard-coded
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data interval:"),
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Interval_JComboBox = new SimpleJComboBox ( false );
    __Interval_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Interval_JComboBox,
        1, yMain, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - data interval (time step) for time series."), 
        3, yMain, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "NHour interval offset:"),
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NHourIntervalOffset_JTextField = new JTextField ( 5 );
    __NHourIntervalOffset_JTextField.setToolTipText(
    	"This is needed in some cases where all NHour data don't exactly align with midnight of the database time zone, such as tests");
    __NHourIntervalOffset_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __NHourIntervalOffset_JTextField,
        1, yMain, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Optional - hours that NHour data are offset from midnight."), 
        3, yMain, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Top-level tabbed panel to separate filter input and specific choices
    __main_JTabbedPane = new JTabbedPane ();
    __main_JTabbedPane.setBorder(
        BorderFactory.createTitledBorder ( BorderFactory.createLineBorder(Color.black),
        "Specify how to match HDB time series or ensemble, and set time series properties" ));
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++yMain, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // Panel to query time series using filter
    int yFilter = -1;
    JPanel filter_JPanel = new JPanel();
    filter_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Read 1+ time series using filter", filter_JPanel );
    
    JGUIUtil.addComponent(filter_JPanel, new JLabel (
        "Use these parameters when reading 1+ time series from HDB."), 
        0, ++yFilter, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Data types are particular to the datastore...
    // This is somewhat redundant with the CommonDataTypeName but need to keep bulk read separate
    // from single time series/ensemble read
    
    JGUIUtil.addComponent(filter_JPanel, new JLabel ( "Data type:"),
        0, ++yFilter, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataType_JComboBox = new SimpleJComboBox ( false );
    __DataType_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(filter_JPanel, __DataType_JComboBox,
        1, yFilter, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(filter_JPanel, new JLabel("Required - data type for time series."), 
        3, yFilter, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	
   	// Input filters
    // TODO SAM 2010-11-02 Need to use SetInputFilters() so the filters can change when a
    // datastore is selected.  For now it is OK because the input filters do not provide choices.

	int buffer = 3;
	Insets insets = new Insets(0,buffer,0,0);
	try {
	    // Add input filters for ReclamationHDB time series...
		__inputFilter_JPanel = new ReclamationHDB_TimeSeries_InputFilter_JPanel(
		    getSelectedDataStore(), __command.getNumFilterGroups() );
		JGUIUtil.addComponent(filter_JPanel, __inputFilter_JPanel,
			0, ++yFilter, 2, 1, 0.0, 0.0, insets, GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST );
		if ( ((ReclamationHDB_TimeSeries_InputFilter_JPanel)__inputFilter_JPanel).getDataStore() != null ) {
		    __inputFilter_JPanel.addEventListeners ( this );
		}
   	    JGUIUtil.addComponent(filter_JPanel, new JLabel ( "Optional - query filters."),
   	        3, yFilter, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	}
	catch ( Exception e ) {
		Message.printWarning ( 2, routine, "Unable to initialize ReclamationHDB input filter." );
		Message.printWarning ( 2, routine, e );
	}

	int yInner = -1;
    JPanel inner_JPanel = new JPanel();
    inner_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Read single time series or ensemble", inner_JPanel );
    
    __sdi_JTabbedPane = new JTabbedPane ();
    __sdi_JTabbedPane.setBorder(
        BorderFactory.createTitledBorder ( BorderFactory.createLineBorder(Color.black),
        "Specify how to match the HDB site_datatype_id (required for all time series and ensembles)" ));
    JGUIUtil.addComponent(inner_JPanel, __sdi_JTabbedPane,
        0, ++yInner, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // Panel to select site_datatype_id directly
    int ysdi = -1;
    JPanel sdi_JPanel = new JPanel();
    sdi_JPanel.setLayout( new GridBagLayout() );
    __sdi_JTabbedPane.addTab ( "Select site_datatype_id (SDI)", sdi_JPanel );
    
    JGUIUtil.addComponent(sdi_JPanel, new JLabel (
        "The choices below indicate: \"site_datatype_id - object type name - site common name - site name - datatype name\", " +
        "sorted by object type name and site common name."), 
        0, ++ysdi, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(sdi_JPanel, new JLabel (
        "The choices currently are not constrained by whether time series for the given interval are available " +
        "because other parameters below indicate whether the time series are real, model, or ensemble."), 
        0, ++ysdi, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(sdi_JPanel, new JLabel ("Site data type ID:"), 
        0, ++ysdi, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SiteDataTypeID_JComboBox = new SimpleJComboBox (false);
    __SiteDataTypeID_JComboBox.setMaximumRowCount(20);
    __SiteDataTypeID_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(sdi_JPanel, __SiteDataTypeID_JComboBox,
        1, ysdi, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(sdi_JPanel, new JLabel ( "Required."),
        3, ysdi, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Panel to control site_datatype_id selection
    int ySiteCommonName = -1;
    JPanel siteCommon_JPanel = new JPanel();
    siteCommon_JPanel.setLayout( new GridBagLayout() );
    __sdi_JTabbedPane.addTab ( "OBSOLETE - select SDI using site common name", siteCommon_JPanel );
    
    JGUIUtil.addComponent(siteCommon_JPanel, new JLabel (
        "The following parameters, if specified, cause a matching SDI to be selected in the other tab.  DO NOT SPECIFY HERE."), 
        0, ++ySiteCommonName, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(siteCommon_JPanel, new JLabel ("Site common name:"), 
        0, ++ySiteCommonName, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SiteCommonName_JComboBox = new SimpleJComboBox (false);
    __SiteCommonName_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(siteCommon_JPanel, __SiteCommonName_JComboBox,
        1, ySiteCommonName, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(siteCommon_JPanel, new JLabel (
        "Required - used with data type common name to determine site_datatype_id."),
        3, ySiteCommonName, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(siteCommon_JPanel, new JLabel ("Matching site_id:"), 
        0, ++ySiteCommonName, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __selectedSiteID_JLabel = new JLabel ( "");
    JGUIUtil.addComponent(siteCommon_JPanel, __selectedSiteID_JLabel,
        1, ySiteCommonName, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(siteCommon_JPanel, new JLabel (
        "Information - useful when comparing to database contents."),
        3, ySiteCommonName, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(siteCommon_JPanel, new JLabel ("Data type common name:"), 
        0, ++ySiteCommonName, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataTypeCommonName_JComboBox = new SimpleJComboBox (false);
    __DataTypeCommonName_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(siteCommon_JPanel, __DataTypeCommonName_JComboBox,
        1, ySiteCommonName, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(siteCommon_JPanel, new JLabel (
        "Required - used with site common name to determine site_datatype_id."),
        3, ySiteCommonName, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(siteCommon_JPanel, new JLabel ("Matching site_datatype_id:"), 
        0, ++ySiteCommonName, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __selectedSiteDataTypeID_JLabel = new JLabel ( "");
    JGUIUtil.addComponent(siteCommon_JPanel, __selectedSiteDataTypeID_JLabel,
        1, ySiteCommonName, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(siteCommon_JPanel, new JLabel (
        "Information - useful when comparing to database contents."),
        3, ySiteCommonName, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    // Tabbed pane for model and ensemble data
    __inner_JTabbedPane = new JTabbedPane ();
    __inner_JTabbedPane.setBorder(
        BorderFactory.createTitledBorder ( BorderFactory.createLineBorder(Color.black),
        "Specify how to match HDB model_run_id for single model time series or ensemble of model time series" ));
    JGUIUtil.addComponent(inner_JPanel, __inner_JTabbedPane,
        0, ++yInner, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
    // Panel to query single time series
    int yModel = -1;
    JPanel model_JPanel = new JPanel();
    model_JPanel.setLayout( new GridBagLayout() );
    __inner_JTabbedPane.addTab ( "Single model time series", model_JPanel );
    
    JGUIUtil.addComponent(model_JPanel, new JLabel (
        "Use these parameters to read a single model time series from HDB.  The site_datatype_id and data interval from above are used " +
        "to limit selections to available time series."), 
        0, ++yModel, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(model_JPanel, new JLabel ("Model name:"), 
        0, ++yModel, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ModelName_JComboBox = new SimpleJComboBox (false);
    __ModelName_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(model_JPanel, __ModelName_JComboBox,
        1, yModel, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(model_JPanel, new JLabel (
        "Required - used to determine the model_run_id."),
        3, yModel, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(model_JPanel, new JLabel ("Selected model_id:"), 
        0, ++yModel, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __selectedModelID_JLabel = new JLabel ( "");
    JGUIUtil.addComponent(model_JPanel, __selectedModelID_JLabel,
        1, yModel, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(model_JPanel, new JLabel (
        "Information - useful when comparing to database contents."),
        3, yModel, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(model_JPanel, new JLabel ("Model run name:"), 
        0, ++yModel, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ModelRunName_JComboBox = new SimpleJComboBox (false);
    __ModelRunName_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(model_JPanel, __ModelRunName_JComboBox,
        1, yModel, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(model_JPanel, new JLabel (
        "Required - used to determine the model_run_id."),
        3, yModel, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(model_JPanel, new JLabel ("Hydrologic indicator:"), 
        0, ++yModel, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __HydrologicIndicator_JComboBox = new SimpleJComboBox (false);
    __HydrologicIndicator_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(model_JPanel, __HydrologicIndicator_JComboBox,
        1, yModel, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(model_JPanel, new JLabel (
        "Required - used to determine the model_run_id (can be blank)."),
        3, yModel, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(model_JPanel, new JLabel ("Model run date:"), 
        0, ++yModel, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ModelRunDate_JComboBox = new SimpleJComboBox (false);
    __ModelRunDate_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(model_JPanel, __ModelRunDate_JComboBox,
        1, yModel, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(model_JPanel, new JLabel (
        "Required - YYYY-MM-DD hh:mm, used to determine the model_run_id."),
        3, yModel, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(model_JPanel, new JLabel ("Selected model_run_id:"), 
        0, ++yModel, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __selectedModelRunID_JLabel = new JLabel ( "");
    JGUIUtil.addComponent(model_JPanel, __selectedModelRunID_JLabel,
        1, yModel, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(model_JPanel, new JLabel (
        "Information - useful when comparing to database contents."),
        3, yModel, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(model_JPanel, new JLabel ("Model run ID (MRI):"), 
        0, ++yModel, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ModelRunID_JComboBox = new SimpleJComboBox (false);
    __ModelRunID_JComboBox.setToolTipText("Optional - use instead of above:  MRI - " +
        "model name - model run name - hydrologic indicator - run date");
    __ModelRunID_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(model_JPanel, __ModelRunID_JComboBox,
        1, yModel, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    //JGUIUtil.addComponent(model_JPanel, new JLabel (
    //    "Optional - alternative to selecting above choices."),
    //    3, yModel, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
    // Panel to query ensemble time series
    int yEnsemble = -1;
    JPanel ensemble_JPanel = new JPanel();
    ensemble_JPanel.setLayout( new GridBagLayout() );
    __inner_JTabbedPane.addTab ( "Ensemble of model time series", ensemble_JPanel );
    
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel (
        "Use these parameters to read an ensemble of model time series from HDB.  The site_datatype_id and data interval from above are used " +
        "to limit selections to available ensembles."), 
        0, ++yEnsemble, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel ("Ensemble name:"), 
        0, ++yEnsemble, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __EnsembleName_JComboBox = new SimpleJComboBox (false); // New value is specified with separate text field for clarity
    __EnsembleName_JComboBox.addItemListener (this);
    __EnsembleName_JComboBox.addKeyListener (this);
    JGUIUtil.addComponent(ensemble_JPanel, __EnsembleName_JComboBox,
        1, yEnsemble, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel (
        "Required - used to determine the ensemble ID -> ensemble traces -> ensemble model_run_id -> model time series."),
        3, yEnsemble, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel ("Output ensemble ID:"), 
        0, ++yEnsemble, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputEnsembleID_JTextField = new JTextField (20);
    __OutputEnsembleID_JTextField.setToolTipText("This ID is NOT used to query HDB.  It is purely to use for the output.");
    __OutputEnsembleID_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(ensemble_JPanel, __OutputEnsembleID_JTextField,
        1, yEnsemble, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel ("Optional - ensemble ID for output (default=ensemble name)."),
        3, yEnsemble, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // TODO SAM 2016-04-29 Why are these commented out?
    /*
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel ("Selected ensemble_id:"), 
        0, ++yEnsemble, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __selectedEnsembleID_JLabel = new JLabel ( "");
    JGUIUtil.addComponent(ensemble_JPanel, __selectedEnsembleID_JLabel,
        1, yEnsemble, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel (
        "Information - useful when comparing to database contents."),
        3, yEnsemble, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        */
    /*
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel("Ensemble trace number:"),
        0, ++yEnsemble, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __EnsembleTrace_JTextField = new TSFormatSpecifiersJPanel(10);
    __EnsembleTrace_JTextField.setToolTipText("Use %L for location, %T for data type, %I for interval.");
    __EnsembleTrace_JTextField.addKeyListener ( this );
    __EnsembleTrace_JTextField.getDocument().addDocumentListener(this);
    __EnsembleTrace_JTextField.setToolTipText("%L for location, %T for data type, ${TS:property} to use property.");
    JGUIUtil.addComponent(ensemble_JPanel, __EnsembleTrace_JTextField,
        1, yEnsemble, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel (
        "Optional - use %z for sequence (trace) ID or ${TS:property} (default=sequence ID)."),
        3, yEnsemble, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        */
    /*
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel ("Ensemble model name:"), 
        0, ++yEnsemble, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __EnsembleModelName_JComboBox = new SimpleJComboBox (false);
    // Set the size to handle example data - otherwise may have layout issues.
    __EnsembleModelName_JComboBox.setPrototypeDisplayValue("MMMMMMMMMMMMMMMMMMMMMMMMM");
    __EnsembleModelName_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(ensemble_JPanel, __EnsembleModelName_JComboBox,
        1, yEnsemble, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel (
        "Required - used to determine the ensemble model_run_id."),
        3, yEnsemble, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel ("Selected ensemble model_id:"), 
        0, ++yEnsemble, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __selectedEnsembleModelID_JLabel = new JLabel ( "");
    JGUIUtil.addComponent(ensemble_JPanel, __selectedEnsembleModelID_JLabel,
        1, yEnsemble, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel (
        "Information - useful when comparing to database contents."),
        3, yEnsemble, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel ("Ensemble model run date:"), 
        0, ++yEnsemble, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __EnsembleModelRunDate_JComboBox = new SimpleJComboBox (false); // New value is specified with separate text field for clarity
    __EnsembleModelRunDate_JComboBox.setPrototypeDisplayValue("MMMM-MM-MM MM:MM   ");
    __EnsembleModelRunDate_JComboBox.addItemListener (this);
    __EnsembleModelRunDate_JComboBox.addKeyListener (this);
    JGUIUtil.addComponent(ensemble_JPanel, __EnsembleModelRunDate_JComboBox,
        1, yEnsemble, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel (
        "Optional - used to determine model_run_id (default=run date not used)."),
        3, yEnsemble, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel ("Selected ensemble model_run_id:"), 
        0, ++yEnsemble, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __selectedEnsembleModelRunID_JLabel = new JLabel ( "Determined from model name and run date when command is run");
    JGUIUtil.addComponent(ensemble_JPanel, __selectedEnsembleModelRunID_JLabel,
        1, yEnsemble, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    //JGUIUtil.addComponent(ensemble_JPanel, new JLabel (
    //    ""),
    //    3, yEnsemble, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel ("Ensemble model run ID (model_run_id):"), 
        0, ++yEnsemble, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __EnsembleModelRunID_JComboBox = new SimpleJComboBox (false);
    __EnsembleModelRunID_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(ensemble_JPanel, __EnsembleModelRunID_JComboBox,
        1, yEnsemble, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel (
        "Optional - alternative to selecting above choices."),
        3, yEnsemble, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        */

    // Panel to specify time series properties to set upon reading
    int yOutput = -1;
    JPanel output_JPanel = new JPanel();
    output_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Set time series properties", output_JPanel );
    
    JGUIUtil.addComponent(output_JPanel, new JLabel (
        "The Properties parameter below sets time series properties so that they can be used later in processing or output."), 
        0, ++yOutput, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel (
        "Expansion of properties defined with specifiers or other properties occurs when this command is executed."), 
        0, ++yOutput, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        
    JGUIUtil.addComponent(output_JPanel, new JLabel ("Properties:"),
        0, ++yOutput, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Properties_JTextArea = new JTextArea (3,35);
    __Properties_JTextArea.setLineWrap ( true );
    __Properties_JTextArea.setWrapStyleWord ( true );
    __Properties_JTextArea.setToolTipText("PropertyName1:PropertyValue1,PropertyName2:PropertyValue2");
    __Properties_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(output_JPanel, new JScrollPane(__Properties_JTextArea),
        1, yOutput, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel ("Optional - string properties to assign to time series."),
        3, yOutput, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(output_JPanel, new SimpleJButton ("Edit","EditProperties",this),
        3, ++yOutput, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // General parameters...
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Input start:"), 
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputStart_JTextField = new JTextField (20);
    __InputStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __InputStart_JTextField,
        1, yMain, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - override the global input start."),
        3, yMain, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Input end:"), 
        0, ++yMain, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputEnd_JTextField = new JTextField (20);
    __InputEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __InputEnd_JTextField,
        1, yMain, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - override the global input end."),
        3, yMain, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel("Alias to assign:"),
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Alias_JTextField = new TSFormatSpecifiersJPanel(10);
    __Alias_JTextField.setToolTipText("Use %L for location, %T for data type, %I for interval.");
    __Alias_JTextField.addKeyListener ( this );
    __Alias_JTextField.getDocument().addDocumentListener(this);
    JGUIUtil.addComponent(main_JPanel, __Alias_JTextField,
        1, yMain, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - use %L for location, etc. (default=no alias)."),
        3, yMain, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:"),
		0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea (4,50);
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, yMain, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	
    // All of the components have been initialized above but now generate an event to populate...
    if ( __DataStore_JComboBox.getItemCount() > 0 ) {
        __DataStore_JComboBox.select(null);
        __DataStore_JComboBox.select(0);
    }

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
    JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++yMain, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

    __help_JButton = new SimpleJButton("Help", "Help", this);
    __help_JButton.setToolTipText("Show command documentation in web browser" );
	button_JPanel.add ( __help_JButton );
	if ( !Desktop.isDesktopSupported() ) {
		__help_JButton.setEnabled(false);
	}
	__cancel_JButton = new SimpleJButton( "Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__ok_JButton = new SimpleJButton("OK", this);
	button_JPanel.add ( __ok_JButton );

	setTitle ( "Edit " + __command.getCommandName() + " Command" );

	// Refresh the contents...
    checkGUIState();
    refresh ();
    __ignoreEvents = false; // After initialization of components let events happen to dynamically cause cascade
    checkGUIState(); // Do this again because it may not have happened due to the special event handling
    updateSiteIDTextFields();
    updateModelIDTextFields();
    // Dialogs do not need to be resizable...
	setResizable ( true );
    pack();
    JGUIUtil.center( this );
    super.setVisible( true );
}

/**
Handle ItemListener events.
@param e item event
*/
public void itemStateChanged ( ItemEvent e )
{
    if ( __ignoreEvents ) {
        return; // Startup
    }

    checkGUIState();
    Object source = e.getSource();
    int sc = e.getStateChange();
    if ( (source == __DataStore_JComboBox) && (sc == ItemEvent.SELECTED) ) {
        // User has selected a datastore.
        actionPerformedDataStoreSelected ();
    }
    else if ( (source == __Interval_JComboBox) && (sc == ItemEvent.SELECTED) ) {
        // User has selected an interval.
        actionPerformedIntervalSelected ();
    }
    else if ( (source == __SiteCommonName_JComboBox) && (sc == ItemEvent.SELECTED) ) {
        // User has selected a site common name.
        actionPerformedSiteCommonNameSelected ();
    }
    else if ( (source == __DataTypeCommonName_JComboBox) && (sc == ItemEvent.SELECTED) ) {
        // User has selected a data type common name.
        actionPerformedDataTypeCommonNameSelected ();
    }
    else if ( (source == __SiteDataTypeID_JComboBox) && (sc == ItemEvent.SELECTED) ) {
        // User has selected a data type common name.
        actionPerformedSiteDataTypeIDSelected ();
    }
    else if ( (source == __ModelName_JComboBox) && (sc == ItemEvent.SELECTED) ) {
        // User has selected a model name.
        actionPerformedModelNameSelected ();
    }
    else if ( (source == __ModelRunName_JComboBox) && (sc == ItemEvent.SELECTED) ) {
        // User has selected a model run name.
        actionPerformedModelRunNameSelected ();
    }
    else if ( (source == __ModelRunDate_JComboBox) && (sc == ItemEvent.SELECTED) ) {
        // User has selected a model run date.
        actionPerformedModelRunDateSelected ();
    }
    else if ( (source == __HydrologicIndicator_JComboBox) && (sc == ItemEvent.SELECTED) ) {
        // User has selected a model name.
        actionPerformedHydrologicIndicatorSelected ();
    }
    else if ( (source == __EnsembleName_JComboBox) && (sc == ItemEvent.SELECTED) ) {
        // User has selected an ensemble name.
        actionPerformedEnsembleNameSelected ();
    }
    /*
    else if ( (source == __EnsembleModelName_JComboBox) && (sc == ItemEvent.SELECTED) ) {
        // User has selected an ensemble model name.
        actionPerformedEnsembleModelNameSelected ();
    }
    */
 
    refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{
    if ( __ignoreEvents ) {
        return; // Startup.
    }
    int code = event.getKeyCode();
    if ( code == KeyEvent.VK_ENTER ) {
        refresh ();
        checkInput();
        if ( !__error_wait ) {
            response ( true );
        }
    }
}

/**
Need this to properly capture key events, especially deletes.
*/
public void keyReleased ( KeyEvent event )
{
    if ( __ignoreEvents ) {
        return; // Startup.
    }
    refresh();
}

public void keyTyped ( KeyEvent event )
{
}

/**
Indicate if the user pressed OK (cancel otherwise).
@return true if the edits were committed, false if the user canceled.
*/
public boolean ok ()
{	return __ok;
}

/**
Set the data type choices in response to a new datastore being selected.
The data types are used with the filters because DataTypeCommonName is used with the specific data queries.
*/
private void populateDataTypeChoices ()
{   String routine = getClass().getName() + ".populateDataTypeChoices";
    ReclamationHDBDataStore hdbDataStore = getSelectedDataStore();
    List<String> dataTypes = new ArrayList<String>();
    if ( hdbDataStore == null ) {
        // Case when HDB not available, such as off-line development
    }
    else {
        ReclamationHDB_DMI dmi = (ReclamationHDB_DMI)hdbDataStore.getDMI();
        try {
            dataTypes = dmi.getObjectDataTypes ( true );
        }
        catch ( Exception e ) {
            // Hopefully should not happen
            Message.printWarning(2, routine, "Unable to get object types and associated data types for datastore \"" +
                __DataStore_JComboBox.getSelected() + "\" - no database connection?");
        }
    }
    // Add a blank option if the filters are not being used
    dataTypes.add(0,"");
    // Add a wildcard option to get all data types
    dataTypes.add(1,"*");
    __DataType_JComboBox.setData ( dataTypes );
    __DataType_JComboBox.select ( 0 );
}

/**
Populate the data type choice list based on the selected site common name.
*/
private void populateDataTypeCommonNameChoices ( ReclamationHDB_DMI rdmi )
{   //String routine = getClass().getName() + ".populateDataTypeCommonNameChoices";
    if ( (rdmi == null) || (__DataTypeCommonName_JComboBox == null) ) {
        // Initialization
        return;
    }
    // Populate the data types from datatype that match the site_id via site_datatype_id
    // First find the site_id for the selected site
    String selectedSiteCommonName = __SiteCommonName_JComboBox.getSelected();
    List<String> dataTypeCommonNameStrings = new Vector<String>();
    dataTypeCommonNameStrings.add("");
    if ( selectedSiteCommonName != null ) {
        List<ReclamationHDB_SiteDataType> siteDataTypeList =
            rdmi.findSiteDataType(__siteDataTypeList, selectedSiteCommonName, null );
        for ( ReclamationHDB_SiteDataType siteDataType: siteDataTypeList ) {
            dataTypeCommonNameStrings.add ( siteDataType.getDataTypeCommonName() );
        }
        Collections.sort(dataTypeCommonNameStrings,String.CASE_INSENSITIVE_ORDER);
    }
    __DataTypeCommonName_JComboBox.removeAll ();
    __DataTypeCommonName_JComboBox.setData(dataTypeCommonNameStrings);
    // Select first choice (may get reset from existing parameter values).
    __DataTypeCommonName_JComboBox.select ( null );
    if ( __DataTypeCommonName_JComboBox.getItemCount() > 0 ) {
        __DataTypeCommonName_JComboBox.select ( 0 );
    }
}

/**
Populate the ensemble model name list based on the selected datastore.
The model names are the same as the non-ensemble list so reuse what was read from HDB.
*/
/*
private void populateEnsembleModelNameChoices ( ReclamationHDB_DMI rdmi )
{   if ( (rdmi == null) || (__EnsembleModelName_JComboBox == null) ) {
        // Initialization
        return;
    }
    List<String> modelNameStrings = new Vector();
    modelNameStrings.add ( "" ); // Always add blank because user may not want model time series
    for ( ReclamationHDB_Model model: __modelList ) {
        modelNameStrings.add ( model.getModelName() );
    }
    Collections.sort(modelNameStrings,String.CASE_INSENSITIVE_ORDER);
    StringUtil.removeDuplicates(modelNameStrings, true, true);
    __EnsembleModelName_JComboBox.removeAll ();
    __EnsembleModelName_JComboBox.setData(modelNameStrings);
    // Select first choice (may get reset from existing parameter values).
    __EnsembleModelName_JComboBox.select ( null );
    if ( __EnsembleModelName_JComboBox.getItemCount() > 0 ) {
        __EnsembleModelName_JComboBox.select ( 0 );
    }
}
*/

/**
Populate the model run ID list based on the selected datastore.
*/
/* TODO SAM 2013-09-28 Evaluate whether to enable for reading single traces in ensemble
private void populateEnsembleModelRunIDChoices ( ReclamationHDB_DMI rdmi )
{   String routine = getClass().getName() + ".populateEnsembleModelRunIDhoices";
    if ( (rdmi == null) || (__EnsembleModelRunID_JComboBox == null) ) {
        // Initialization
        return;
    }
    List<String> modelRunIDStrings = new ArrayList<String>();
    List<String> sortStrings = new ArrayList<String>();
    modelRunIDStrings.add ( "" ); // Always add blank because user may not want model time series
    sortStrings.add("");
    String mriString;
    try {
        // This is the full list of model run identifiers.
        List<ReclamationHDB_ModelRun> modelRunList = rdmi.readHdbModelRunList(-1,null,null,null,null);
        String hydrologicIndicator;
        Message.printStatus(2,routine,"Have " + modelRunList.size() + " model runs." );
        for ( ReclamationHDB_ModelRun modelRun: modelRunList ) {
            hydrologicIndicator = modelRun.getHydrologicIndicator();
            if ( hydrologicIndicator.equals("") ) {
                hydrologicIndicator = "no hydrologic indicator";
            }
            mriString = modelRun.getModelRunID() + " - " + modelRun.getModelRunName() + " - " +
                hydrologicIndicator + " - " + modelRun.getRunDate().toString().replace(":00.0","");
            if ( mriString.length() > 85 ) {
                mriString = mriString.substring(0,85) + "...";
            }
            modelRunIDStrings.add ( "" + mriString );
            // Only show the date to the minute
            sortStrings.add ( modelRun.getModelRunName() + " - " +
                hydrologicIndicator + " - " + modelRun.getRunDate().toString().replace(":00.0","") );
        }
        // Sort the descriptive strings and then resort the main list to be in the same order
        int [] sortOrder = new int[sortStrings.size()];
        StringUtil.sortStringList(sortStrings, StringUtil.SORT_ASCENDING, sortOrder, true, true);
        StringUtil.reorderStringList(modelRunIDStrings,sortOrder,false);
    }
    catch ( Exception e ) {
        Message.printWarning(3, routine, "Error getting HDB model run list (" + e + ")." );
        modelRunIDStrings = new ArrayList<String>();
    }
    __EnsembleModelRunID_JComboBox.removeAll ();
    __EnsembleModelRunID_JComboBox.setData(modelRunIDStrings);
    // Select first choice (may get reset from existing parameter values).
    __EnsembleModelRunID_JComboBox.select ( null );
    if ( __EnsembleModelRunID_JComboBox.getItemCount() > 0 ) {
        __EnsembleModelRunID_JComboBox.select ( 0 );
    }
}
*/

/**
Populate the model name list based on the selected datastore.
*/
private void populateEnsembleNameChoices ( ReclamationHDB_DMI rdmi )
{   String routine = getClass().getName() + ".populateEnsembleNameChoices";
    if ( (rdmi == null) || (__EnsembleName_JComboBox == null) ) {
        // Initialization
        return;
    }
    String selectedInterval = __Interval_JComboBox.getSelected();
    if ( selectedInterval == null ) {
        return;
    }
    String selectedSiteDataTypeID = getSelectedSiteDataTypeID();
    if ( selectedSiteDataTypeID.equals("") ) {
        return;
    }
    List<Integer> ensembleIDList = new ArrayList<Integer>();
    List<String> ensembleNameStrings = new ArrayList<String>();
    ensembleNameStrings.add ( "" ); // Always add blank because user may not want ensemble time series
    try {
        // Get the list of distinct model_run_identifiers from the model table corresponding to the interval
        List<Integer> modelRunIDs = rdmi.readHdbModelRunIDListForModelTable(
            Integer.parseInt(selectedSiteDataTypeID),selectedInterval);
        Message.printStatus(2, routine, "Have " + modelRunIDs.size() +
             " distinct model run IDs for SDI=" + selectedSiteDataTypeID + " and interval=" + selectedInterval);
        if ( modelRunIDs.size() > 0 ) {
            // Read the ensemble traces that match the specified MRIs
            List<ReclamationHDB_EnsembleTrace> traceList = rdmi.readRefEnsembleTraceList(-1, -1, -1, modelRunIDs );
            // Get the unique list of ensemble identifiers from the list
            for ( ReclamationHDB_EnsembleTrace trace: traceList ) {
                boolean found = false;
                for ( Integer i: ensembleIDList ) {
                    if ( trace.getEnsembleID() == i ) {
                        found = true;
                        break;
                    }
                }
                if ( !found ) {
                    ensembleIDList.add(new Integer(trace.getEnsembleID()));
                }
            }
        }
        List<ReclamationHDB_Ensemble> ensembleList = rdmi.readRefEnsembleList(null,ensembleIDList,-1);
        for ( ReclamationHDB_Ensemble ensemble: ensembleList ) {
            ensembleNameStrings.add ( ensemble.getEnsembleName() );
        }
        Collections.sort(ensembleNameStrings,String.CASE_INSENSITIVE_ORDER);
        StringUtil.removeDuplicates(ensembleNameStrings, true, true);
    }
    catch ( Exception e ) {
        Message.printWarning(3, routine, "Error getting HDB ensemble list (" + e + ")." );
        ensembleNameStrings = new Vector<String>();
    }
    __EnsembleName_JComboBox.removeAll ();
    __EnsembleName_JComboBox.setData(ensembleNameStrings);
    int max = ensembleNameStrings.size();
    if ( max > 25 ) {
        max = 25;
    }
    __EnsembleName_JComboBox.setMaximumRowCount(max);
    // Select first choice (may get reset from existing parameter values).
    __EnsembleName_JComboBox.select ( null );
    if ( __EnsembleName_JComboBox.getItemCount() > 0 ) {
        __EnsembleName_JComboBox.select ( 0 );
    }
}

/**
Populate the model hydrologic indicator list based on the selected datastore.
*/
private void populateHydrologicIndicatorChoices ( ReclamationHDB_DMI rdmi )
{   if ( (rdmi == null) || (__HydrologicIndicator_JComboBox == null) ) {
        // Initialization
        return;
    }
    List<String> hydrologicIndicatorStrings = new ArrayList<String>();
    hydrologicIndicatorStrings.add ( "" ); // Always add blank because user may not want model time series
    try {
        //readModelList(rdmi);
        // Get the model run list that is valid for the currently selected site_datatype_id and interval
        List<ReclamationHDB_ModelRun> modelRuns = __modelRunList;
        // Get the currently selected model name
        String modelName = __ModelName_JComboBox.getSelected();
        String modelRunName = __ModelRunName_JComboBox.getSelected();
        if ( (modelName != null) && !modelName.equals("") && (modelRunName != null) && !modelRunName.equals("") ) {
            // Get the model run names that correspond to the model runs and selected model name
            ReclamationHDB_Model model;
            for ( ReclamationHDB_ModelRun modelRun: modelRuns ) {
                model = rdmi.lookupModel(modelRun.getModelID());
                if ( (model != null) && !modelName.equalsIgnoreCase(model.getModelName()) ) {
                    continue;
                }
                if ( !modelRunName.equalsIgnoreCase(modelRun.getModelRunName()) ) {
                    continue;
                }
                hydrologicIndicatorStrings.add ( modelRun.getHydrologicIndicator() );
            }
            Collections.sort(hydrologicIndicatorStrings,String.CASE_INSENSITIVE_ORDER);
            // Results should list unique hydrologic indicators
            StringUtil.removeDuplicates(hydrologicIndicatorStrings, true, true);
        }
    }
    catch ( Exception e ) {
        // Should not happen
        Message.printWarning(3,"",e);
    }
    __HydrologicIndicator_JComboBox.removeAll ();
    __HydrologicIndicator_JComboBox.setData(hydrologicIndicatorStrings);
    // Select first choice (may get reset from existing parameter values).
    __HydrologicIndicator_JComboBox.select ( null );
    if ( __HydrologicIndicator_JComboBox.getItemCount() > 0 ) {
        __HydrologicIndicator_JComboBox.select ( 0 );
    }
}

/**
TODO SAM 2013-04-06 Need to enable this to refresh based on datastore selection.
Set the input filters in response to a new datastore being selected.
*/
private void populateInputFilters ()
{

}

/**
Set the data interval choices in response to a new datastore being selected.
*/
private void populateIntervalChoices ()
{
    __Interval_JComboBox.removeAll();
    __Interval_JComboBox.add ( "Hour" ); // Keep this and 1Hour for flexibility and historical reasons
    __Interval_JComboBox.add ( "1Hour" );
    __Interval_JComboBox.add ( "2Hour" );
    __Interval_JComboBox.add ( "3Hour" );
    __Interval_JComboBox.add ( "4Hour" );
    __Interval_JComboBox.add ( "6Hour" );
    __Interval_JComboBox.add ( "12Hour" );
    __Interval_JComboBox.add ( "24Hour" ); // Theoretically possible, HDB design may have issues
    __Interval_JComboBox.add ( "Day" );
    __Interval_JComboBox.add ( "Month" );
    __Interval_JComboBox.add ( "Year" );
    // FIXME SAM 2010-10-26 Could handle WY as YEAR, but need to think about it
    __Interval_JComboBox.add ( "Irregular" );
    __Interval_JComboBox.setMaximumRowCount(12);
    __Interval_JComboBox.select ( 0 );
}

/**
Populate the model name list based on the selected datastore.
*/
private void populateModelNameChoices ( ReclamationHDB_DMI rdmi )
{   String routine = getClass().getName() + ".populateModelNameChoices";
    if ( (rdmi == null) || (__ModelName_JComboBox == null) ) {
        // Initialization
        return;
    }
    List<String> modelNameStrings = new ArrayList<String>();
    modelNameStrings.add ( "" ); // Always add blank because user may not want model time series
    try {
        //readModelList(rdmi);
        // Get the model run list that is valid for the currently selected site_datatype_id and interval
        List<ReclamationHDB_ModelRun> modelRuns = __modelRunList;
        // Get the models that correspond to the model runs
        ReclamationHDB_Model model;
        for ( ReclamationHDB_ModelRun modelRun: modelRuns ) {
            model = rdmi.lookupModel(modelRun.getModelID());
            if ( model != null ) {
                modelNameStrings.add ( model.getModelName() );
            }
        }
        Collections.sort(modelNameStrings,String.CASE_INSENSITIVE_ORDER);
        StringUtil.removeDuplicates(modelNameStrings, true, true);
    }
    catch ( Exception e ) {
        Message.printWarning(3, routine, "Error getting HDB model list (" + e + ")." );
        modelNameStrings = new ArrayList<String>();
    }
    __ModelName_JComboBox.removeAll ();
    __ModelName_JComboBox.setData(modelNameStrings);
    // Select first choice (may get reset from existing parameter values).
    __ModelName_JComboBox.select ( null );
    if ( __ModelName_JComboBox.getItemCount() > 0 ) {
        __ModelName_JComboBox.select ( 0 );
    }
}

/**
Populate the model run date list based on the selected datastore.
*/
private void populateModelRunDateChoices ( ReclamationHDB_DMI rdmi )
{   //String routine = getClass().getName() + ".populateModelRunDateChoices";
    if ( (rdmi == null) || (__ModelRunDate_JComboBox == null) ) {
        // Initialization
        return;
    }
    List<String> runDateStrings = new ArrayList<String>();
    runDateStrings.add ( "" ); // Always add blank because user may not want model time series
    try {
        //readModelList(rdmi);
        // Get the model run list that is valid for the currently selected site_datatype_id and interval
        List<ReclamationHDB_ModelRun> modelRuns = __modelRunList;
        // Get the currently selected model name
        String modelName = __ModelName_JComboBox.getSelected();
        String modelRunName = __ModelRunName_JComboBox.getSelected();
        String hydrologicIndicator = __HydrologicIndicator_JComboBox.getSelected();
        if ( hydrologicIndicator == null ) {
            hydrologicIndicator = "";
        }
        if ( (modelName != null) && !modelName.equals("") && (modelRunName != null) && !modelRunName.equals("") ) {
            // Get the model run names that correspond to the model runs and selected model name
            ReclamationHDB_Model model;
            for ( ReclamationHDB_ModelRun modelRun: modelRuns ) {
                model = rdmi.lookupModel(modelRun.getModelID());
                if ( (model != null) && !modelName.equalsIgnoreCase(model.getModelName()) ) {
                    continue;
                }
                if ( !modelRunName.equalsIgnoreCase(modelRun.getModelRunName()) ) {
                    continue;
                }
                if ( !hydrologicIndicator.equalsIgnoreCase(modelRun.getHydrologicIndicator()) ) {
                    continue;
                }
                Date d = modelRun.getRunDate();
                DateTime dt = new DateTime(d);
                // Shows seconds and hundredths...
                //runDateStrings.add ( "" + modelRun.getRunDate() );
                runDateStrings.add ( "" + dt.toString(DateTime.FORMAT_YYYY_MM_DD_HH_mm) );
            }
            Collections.sort(runDateStrings,String.CASE_INSENSITIVE_ORDER);
            // There should not be duplicates
            //StringUtil.removeDuplicates(runDateStrings, true, true);
        }
    }
    catch ( Exception e ) {
        // Should not happen
        Message.printWarning(3,"",e);
    }
    __ModelRunDate_JComboBox.removeAll ();
    __ModelRunDate_JComboBox.setData(runDateStrings);
    // Select first choice (may get reset from existing parameter values).
    __ModelRunDate_JComboBox.select ( null );
    if ( __ModelRunDate_JComboBox.getItemCount() > 0 ) {
        try {
        __ModelRunDate_JComboBox.select ( 0 );
        }
        catch ( Exception e ) {
            // TODO SAM 2013-09-27 Figure out what is going on
        }
    }
}

/**
Populate the model run ID list based on the selected datastore and the selected site_datatype_id.
This requires doing a distinct query on the time series data table to get available model_run_id
*/
private void populateModelRunIDChoices ( ReclamationHDB_DMI rdmi )
{   String routine = getClass().getName() + ".populateModelRunIDChoices";
    if ( (rdmi == null) || (__ModelRunID_JComboBox == null) || (__Interval_JComboBox == null) ) {
        // Initialization
        return;
    }
    String selectedInterval = __Interval_JComboBox.getSelected();
    if ( selectedInterval == null ) {
        return;
    }
    String selectedSiteDataTypeID = getSelectedSiteDataTypeID();
    if ( selectedSiteDataTypeID.equals("") ) {
        return;
    }
    List<String> modelRunIDStrings = new ArrayList<String>();
    List<String> sortStrings = new ArrayList<String>();
    modelRunIDStrings.add ( "" ); // Always add blank because user may not want model time series
    sortStrings.add("");
    String mriString;
    try {
        // Get the list of distinct model_run_identifiers from the model table corresponding to the interval
        List<Integer> modelRunIDs = rdmi.readHdbModelRunIDListForModelTable(
            Integer.parseInt(selectedSiteDataTypeID),selectedInterval);
        Message.printStatus(2, routine, "Have " + modelRunIDs.size() +
             " distinct model run IDs for SDI=" + selectedSiteDataTypeID + " and interval=" + selectedInterval);
        if ( modelRunIDs.size() == 0 ) {
            // No matching SDI for the time series interval data table
            List<ReclamationHDB_ModelRun> modelRunList = new ArrayList<ReclamationHDB_ModelRun>();
            setModelRunList(modelRunList);
        }
        else {
            // This is the full list of model run identifiers.
            List<ReclamationHDB_ModelRun> modelRunList = rdmi.readHdbModelRunList(-1,modelRunIDs,null,null,null);
            // Save for use by other parameters
            setModelRunList(modelRunList);
            String hydrologicIndicator, modelName;
            ReclamationHDB_Model model;
            Message.printStatus(2,routine,"Have " + modelRunList.size() + " model runs." );
            for ( ReclamationHDB_ModelRun modelRun: modelRunList ) {
                model = rdmi.lookupModel ( modelRun.getModelID() );
                if ( model == null ) {
                    modelName = "model unknown";
                }
                else {
                    modelName = model.getModelName();
                }
                hydrologicIndicator = modelRun.getHydrologicIndicator();
                if ( hydrologicIndicator.equals("") ) {
                    hydrologicIndicator = "no hydrologic indicator";
                }
                mriString = modelRun.getModelRunID() + " - " + modelName + " - " + modelRun.getModelRunName() + " - " +
                    hydrologicIndicator + " - " + modelRun.getRunDate().toString().replace(":00.0","");
                if ( mriString.length() > 120 ) {
                    mriString = mriString.substring(0,120) + "...";
                }
                modelRunIDStrings.add ( "" + mriString );
                // Only show the date to the minute
                sortStrings.add ( modelName + " - " + modelRun.getModelRunName() + " - " +
                    hydrologicIndicator + " - " + modelRun.getRunDate().toString().replace(":00.0","") );
            }
        }
        // Sort the descriptive strings and then resort the main list to be in the same order
        int [] sortOrder = new int[sortStrings.size()];
        StringUtil.sortStringList(sortStrings, StringUtil.SORT_ASCENDING, sortOrder, true, true);
        StringUtil.reorderStringList(modelRunIDStrings,sortOrder,false);
    }
    catch ( Exception e ) {
        Message.printWarning(3, routine, "Error getting HDB model run list (" + e + ")." );
        modelRunIDStrings = new ArrayList<String>();
    }
    __ModelRunID_JComboBox.removeAll ();
    __ModelRunID_JComboBox.setData(modelRunIDStrings);
    // Select first choice (may get reset from existing parameter values).
    __ModelRunID_JComboBox.select ( null );
    if ( __ModelRunID_JComboBox.getItemCount() > 0 ) {
        __ModelRunID_JComboBox.select ( 0 );
    }
}

/**
Populate the model run name list based on the selected datastore.
*/
private void populateModelRunNameChoices ( ReclamationHDB_DMI rdmi )
{   //String routine = getClass().getName() + ".populateModelRunNameChoices";
    if ( (rdmi == null) || (__ModelRunName_JComboBox == null) ) {
        // Initialization
        return;
    }
    // TODO SAM 2013-09-27 Remove if functionality works out.
    //readModelRunListForSelectedModel(rdmi);
    List<String> modelRunNameStrings = new ArrayList<String>();
    modelRunNameStrings.add ( "" );
    try {
        //readModelList(rdmi);
        // Get the model run list that is valid for the currently selected site_datatype_id and interval
        List<ReclamationHDB_ModelRun> modelRuns = __modelRunList;
        // Get the currently selected model name
        String modelName = __ModelName_JComboBox.getSelected();
        if ( (modelName != null) && !modelName.equals("") ) {
            // Get the model run names that correspond to the model runs and selected model name
            ReclamationHDB_Model model;
            for ( ReclamationHDB_ModelRun modelRun: modelRuns ) {
                model = rdmi.lookupModel(modelRun.getModelID());
                if ( (model != null) && !modelName.equalsIgnoreCase(model.getModelName()) ) {
                    continue;
                }
                modelRunNameStrings.add ( modelRun.getModelRunName() );
            }
            Collections.sort(modelRunNameStrings,String.CASE_INSENSITIVE_ORDER);
            StringUtil.removeDuplicates(modelRunNameStrings, true, true);
        }
    }
    catch ( Exception e ) {
        // Should not happen
        Message.printWarning(3,"",e);
    }
    __ModelRunName_JComboBox.removeAll ();
    __ModelRunName_JComboBox.setData(modelRunNameStrings);
    // Select first choice (may get reset from existing parameter values).
    __ModelRunName_JComboBox.select ( null );
    if ( __ModelRunName_JComboBox.getItemCount() > 0 ) {
        __ModelRunName_JComboBox.select ( 0 );
    }
}

/**
Populate the site common name list based on the selected datastore.
*/
private void populateSiteCommonNameChoices ( ReclamationHDB_DMI rdmi )
{   String routine = getClass().getName() + ".populateSiteCommonNameChoices";
    if ( (rdmi == null) || (__SiteCommonName_JComboBox == null) ) {
        // Initialization
        return;
    }
    List<String> siteCommonNameStrings = new Vector<String>();
    try {
        readSiteDataTypeList(rdmi);
        siteCommonNameStrings.add("");
        for ( ReclamationHDB_SiteDataType siteDataType: __siteDataTypeList ) {
            siteCommonNameStrings.add ( siteDataType.getSiteCommonName() );
        }
        Collections.sort(siteCommonNameStrings,String.CASE_INSENSITIVE_ORDER);
        StringUtil.removeDuplicates(siteCommonNameStrings, true, true);
    }
    catch ( Exception e ) {
        Message.printWarning(3, routine, "Error getting HDB site data type list (" + e + ")." );
        siteCommonNameStrings = new Vector<String>();
        siteCommonNameStrings.add("");
    }
    __SiteCommonName_JComboBox.removeAll ();
    __SiteCommonName_JComboBox.setData(siteCommonNameStrings);
    // Select first choice (may get reset from existing parameter values).
    __SiteCommonName_JComboBox.select ( null );
    if ( __SiteCommonName_JComboBox.getItemCount() > 0 ) {
        __SiteCommonName_JComboBox.select ( 0 );
    }
}

/**
Populate the site common name list based on the selected datastore.
*/
private void populateSiteDataTypeIDChoices ( ReclamationHDB_DMI rdmi )
{   String routine = getClass().getName() + ".populateSiteDataTypeIDChoices";
    if ( (rdmi == null) || (__SiteDataTypeID_JComboBox == null) ) {
        // Initialization
        return;
    }
    List<String> siteDataTypeIDStrings = new ArrayList<String>();
    List<String> sortStrings = new ArrayList<String>();
    siteDataTypeIDStrings.add ( "" );
    sortStrings.add("");
    ReclamationHDB_DataType dt;
    ReclamationHDB_Site site;
    String dtString, objectTypeName, siteCommonName, siteName, sdiString;
    try {
        // The following are not currently cached in the DMI so read here
        List<ReclamationHDB_SiteDataType> siteDataTypeList = rdmi.readHdbSiteDataTypeList();
        List<ReclamationHDB_Site> siteList = rdmi.readHdbSiteList();
        for ( ReclamationHDB_SiteDataType siteDataType: siteDataTypeList ) {
            // Since user is selecting SDI directly, provide site name and datatype name as FYI
            dt = rdmi.lookupDataType(siteDataType.getDataTypeID());
            if ( dt == null ) {
                dtString = "data type unknown";
            }
            else {
                dtString = dt.getDataTypeName().trim();
            }
            site = rdmi.lookupSite(siteList, siteDataType.getSiteID());
            if ( site == null ) {
                objectTypeName = "object type unknown";
                siteName = "site name unknown";
                siteCommonName = "site common name unknown";
            }
            else {
                objectTypeName = site.getObjectTypeName();
                siteName = site.getSiteName().trim();
                siteCommonName = site.getSiteCommonName().trim();
            }
            sdiString = "" + siteDataType.getSiteDataTypeID() + " - " + objectTypeName + " - " + siteCommonName +
                " - " + siteName + " - " + dtString;
            // Truncate what is shown to the user if too long (length determined from UI inspection)
            if ( sdiString.length() > 120 ) {
                sdiString = sdiString.substring(0,120) + "...";
            }
            siteDataTypeIDStrings.add ( sdiString );
            sortStrings.add ( objectTypeName + " - " + siteCommonName + " - " + siteName + " - " + dtString );
        }
        // Sort the descriptive strings and then resort the main list to be in the same order
        int [] sortOrder = new int[sortStrings.size()];
        StringUtil.sortStringList(sortStrings, StringUtil.SORT_ASCENDING, sortOrder, true, true);
        StringUtil.reorderStringList(siteDataTypeIDStrings,sortOrder,false);
    }
    catch ( Exception e ) {
        Message.printWarning(3, routine, "Error getting HDB site data type list (" + e + ")." );
        Message.printWarning(3,routine,e);
        siteDataTypeIDStrings = new ArrayList<String>();
    }
    __SiteDataTypeID_JComboBox.removeAll ();
    __SiteDataTypeID_JComboBox.setData(siteDataTypeIDStrings);
    int max = siteDataTypeIDStrings.size();
    if ( max > 25 ) {
        max = 25;
    }
    __SiteDataTypeID_JComboBox.setMaximumRowCount(max);
    // Select first choice (may get reset from existing parameter values).
    __SiteDataTypeID_JComboBox.select ( null );
    if ( __SiteDataTypeID_JComboBox.getItemCount() > 0 ) {
        __SiteDataTypeID_JComboBox.select ( 0 );
    }
}

/**
Populate the time zone label, which uses the HDB default time zone.
*/
private void populateTimeZoneLabel ( ReclamationHDB_DMI rdmi )
{
    String defaultTZ = __dmi.getDatabaseTimeZone();
    __TimeZone_JLabel.setText("Output time series for hourly and irregular (instantaneous) interval will be in HDB time zone " + defaultTZ + ".");
}

/**
Read the model list and set for use in the editor.
*/
private void readModelList ( ReclamationHDB_DMI rdmi )
throws Exception
{
    try {
        List<ReclamationHDB_Model> modelList = rdmi.readHdbModelList(null);
        setModelList(modelList);
    }
    catch ( Exception e ) {
        setModelList(new ArrayList<ReclamationHDB_Model>());
        throw e;
    }
}

/**
Read the model run list for the selected model.
*/
/*
private void readModelRunListForSelectedModel ( ReclamationHDB_DMI rdmi )
{   String routine = getClass().getName() + ".readModelRunList";
    String selectedModelName = __ModelName_JComboBox.getSelected();
    List<ReclamationHDB_Model> modelList = rdmi.findModel(__modelList, selectedModelName);
    List<String> modelRunNameStrings = new Vector();
    modelRunNameStrings.add ( "" ); // Always add blank because user may not want model time series
    if ( modelList.size() == 1 ) {
        ReclamationHDB_Model model = modelList.get(0);
        int modelID = model.getModelID();
        Message.printStatus ( 2, routine, "Model ID=" + modelID + " for model name \"" + selectedModelName + "\"" );
        try {
            // There may be no run names for the model id.
            List<ReclamationHDB_ModelRun> modelRunList = rdmi.readHdbModelRunList( modelID,null,null,null,null );
            // The following list matches the model_id and can be used for further filtering
            setModelRunList(modelRunList);
        }
        catch ( Exception e ) {
            Message.printWarning(3, routine, "Error getting HDB model run list (" + e + ")." );
            setModelRunList(new Vector<ReclamationHDB_ModelRun>());
        }
    }
    else {
        Message.printStatus ( 2, routine, "Have " + modelList.size() + " models matching name \"" +
            selectedModelName + "\" - unable to find matching model runs." );
    }
}
*/

/**
Read the site_datatype list and set for use in the editor.
*/
private void readSiteDataTypeList ( ReclamationHDB_DMI rdmi )
{   String routine = getClass().getName() + ".readSiteDataTypeIdList";
    try {
        List<ReclamationHDB_SiteDataType> siteDataTypeList = rdmi.readHdbSiteDataTypeList();
        setSiteDataTypeList(siteDataTypeList);
    }
    catch ( Exception e ) {
        Message.printWarning(3, routine, "Error getting HDB site data type list (" + e + ")." );
        setSiteDataTypeList(new Vector<ReclamationHDB_SiteDataType>());
    }
}

/**
Refresh the command string from the dialog contents.
*/
private void refresh ()
{	String routine = getClass().getSimpleName() + ".refresh";
	__error_wait = false;
	String DataStore = "";
	String Interval = "";
	String NHourIntervalOffset = "";
	String DataType = "";
	String filter_delim = ";";
    String SiteCommonName = "";
    String DataTypeCommonName = "";
    String SiteDataTypeID = "";
    String ModelName = "";
    String ModelRunName = "";
    String HydrologicIndicator = "";
    String ModelRunDate = "";
    String ModelRunID = "";
    String EnsembleName = "";
    String OutputEnsembleID = "";
    String EnsembleTraceID = "";
    String EnsembleModelName = "";
    String EnsembleModelRunDate = "";
    String EnsembleModelRunID = "";
    String Properties = "";
	String InputStart = "";
	String InputEnd = "";
	String Alias = "";
	PropList props = null;
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		props = __command.getCommandParameters();
		DataStore = props.getValue ( "DataStore" );
		Interval = props.getValue ( "Interval" );
		NHourIntervalOffset = props.getValue ( "NHourIntervalOffset" );
	    DataType = props.getValue ( "DataType" );
        SiteCommonName = props.getValue ( "SiteCommonName" );
        DataTypeCommonName = props.getValue ( "DataTypeCommonName" );
        SiteDataTypeID = props.getValue ( "SiteDataTypeID" );
        ModelName = props.getValue ( "ModelName" );
        ModelRunName = props.getValue ( "ModelRunName" );
        HydrologicIndicator = props.getValue ( "HydrologicIndicator" );
        ModelRunDate = props.getValue ( "ModelRunDate" );
        ModelRunID = props.getValue ( "ModelRunID" );
        EnsembleName = props.getValue ( "EnsembleName" );
        OutputEnsembleID = props.getValue ( "OutputEnsembleID" );
        EnsembleTraceID = props.getValue ( "EnsembleTraceID" );
        EnsembleModelName = props.getValue ( "EnsembleModelName" );
        EnsembleModelRunDate = props.getValue ( "EnsembleModelRunDate" );
        EnsembleModelRunID = props.getValue ( "EnsembleModelRunID" );
        Properties = props.getValue ( "Properties" );
		InputStart = props.getValue ( "InputStart" );
		InputEnd = props.getValue ( "InputEnd" );
		Alias = props.getValue ( "Alias" );
        if ( JGUIUtil.isSimpleJComboBoxItem(__DataStore_JComboBox, DataStore, JGUIUtil.NONE, null, null ) ) {
            __DataStore_JComboBox.select ( DataStore );
            if ( __ignoreEvents ) {
                // Also need to make sure that the datastore and DMI are actually selected
                // Call manually because events are disabled at startup to allow cascade to work properly
                setDMIForSelectedDataStore();
            }
            if ( __ignoreEvents ) {
                // Also need to make sure that the __siteDataTypeList is populated
                // Call manually because events are disabled at startup to allow cascade to work properly
                readSiteDataTypeList(__dmi);
            }
            if ( __ignoreEvents ) {
                // Also need to make sure that the __modelList is populated
                // Call manually because events are disabled at startup to allow cascade to work properly
                try {
                    readModelList(__dmi);
                }
                catch ( Exception e ) {
                    // The above call will set the list to empty.
                }
            }
        }
        else {
            if ( (DataStore == null) || DataStore.equals("") ) {
                // New command...select the default...
                if ( __DataStore_JComboBox.getItemCount() > 0 ) {
                    __DataStore_JComboBox.select ( 0 );
                    if ( __ignoreEvents ) {
                        // Also need to make sure that the datastore and DMI are actually selected
                        // Call manually because events are disabled at startup to allow cascade to work properly
                        setDMIForSelectedDataStore();
                    }
                    if ( __ignoreEvents ) {
                        // Also need to make sure that the __siteDataTypeList is populated
                        // Call manually because events are disabled at startup to allow cascade to work properly
                        readSiteDataTypeList(__dmi);
                    }
                    if ( __ignoreEvents ) {
                        // Also need to make sure that the __modelList is populated
                        // Call manually because events are disabled at startup to allow cascade to work properly
                        try {
                            readModelList(__dmi);
                        }
                        catch ( Exception e ) {
                            // The above call will set the list to empty.
                        }
                    }
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "DataStore parameter \"" + DataStore + "\".  Select a\ndifferent value or Cancel." );
                // Select the first so at least something is visible to user
                __DataStore_JComboBox.select ( 0 );
                if ( __ignoreEvents ) {
                    // Also need to make sure that the datastore and DMI are actually selected
                    // Call manually because events are disabled at startup to allow cascade to work properly
                    setDMIForSelectedDataStore();
                }
                if ( __ignoreEvents ) {
                    // Also need to make sure that the __siteDataTypeList is populated
                    // Call manually because events are disabled at startup to allow cascade to work properly
                    readSiteDataTypeList(__dmi);
                }
                if ( __ignoreEvents ) {
                    // Also need to make sure that the __modelList is populated
                    // Call manually because events are disabled at startup to allow cascade to work properly
                    try {
                        readModelList(__dmi);
                    }
                    catch ( Exception e ) {
                        // The above call will set the list to empty.
                    }
                }
            }
        }
        // Time zone label for information
        populateTimeZoneLabel(getReclamationHDB_DMI() );
        // First populate the choices...
        populateIntervalChoices();
        if ( JGUIUtil.isSimpleJComboBoxItem(__Interval_JComboBox, Interval, JGUIUtil.NONE, null, null ) ) {
            __Interval_JComboBox.select ( Interval );
        }
        else {
            if ( (Interval == null) || Interval.equals("") ) {
                // New command...select the default...
                __Interval_JComboBox.select ( 0 );
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "Interval parameter \"" + Interval + "\".  Select a\ndifferent value or Cancel." );
            }
        }
		if ( NHourIntervalOffset != null ) {
			__NHourIntervalOffset_JTextField.setText ( NHourIntervalOffset );
		}
        // First populate the choices...
        populateDataTypeChoices();
        __main_JTabbedPane.setSelectedIndex(0); // Default unless SiteCommonName is specified
        if ( JGUIUtil.isSimpleJComboBoxItem(__DataType_JComboBox, DataType, JGUIUtil.NONE, null, null ) ) {
            __DataType_JComboBox.select ( DataType );
        }
        else {
            if ( (DataType == null) || DataType.equals("") ) {
                // New command...select the default...
                __DataType_JComboBox.select ( 0 );
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "DataType parameter \"" + DataType + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        InputFilter_JPanel filter_panel = __inputFilter_JPanel;
        if ( filter_panel != null ) {
            int nfg = filter_panel.getNumFilterGroups();
            String where;
            for ( int ifg = 0; ifg < nfg; ifg ++ ) {
                where = props.getValue ( "Where" + (ifg + 1) );
                if ( (where != null) && (where.length() > 0) ) {
                    // Set the filter...
                    try {
                        filter_panel.setInputFilter (ifg, where, filter_delim );
                    }
                    catch ( Exception e ) {
                        Message.printWarning ( 1, routine, "Error setting where information using \"" + where + "\"" );
                        Message.printWarning ( 3, routine, e );
                    }
                }
            }
        }
        // First populate the choices...
        populateSiteDataTypeIDChoices(getReclamationHDB_DMI() );
        // Select based on the first token
        int [] index = new int[1];
        if ( JGUIUtil.isSimpleJComboBoxItem(__SiteDataTypeID_JComboBox, SiteDataTypeID, JGUIUtil.CHECK_SUBSTRINGS, " ", 0, index, false) ) {
            __SiteDataTypeID_JComboBox.select ( index[0] );
            __sdi_JTabbedPane.setSelectedIndex(0);
        }
        else {
            if ( (SiteDataTypeID == null) || SiteDataTypeID.equals("") ) {
                // New command...select the default...
                if ( __SiteDataTypeID_JComboBox.getItemCount() > 0 ) {
                    __SiteDataTypeID_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "SiteDataTypeID parameter \"" + SiteDataTypeID + "\".  Select a different value or Cancel." );
            }
        }
        // First populate the choices...
        populateSiteCommonNameChoices(getReclamationHDB_DMI() );
        if ( JGUIUtil.isSimpleJComboBoxItem(__SiteCommonName_JComboBox, SiteCommonName, JGUIUtil.NONE, null, null ) ) {
            __SiteCommonName_JComboBox.select ( SiteCommonName );
            __main_JTabbedPane.setSelectedIndex(1);
        }
        else {
            if ( (SiteCommonName == null) || SiteCommonName.equals("") ) {
                // New command...select the default...
                if ( __SiteCommonName_JComboBox.getItemCount() > 0 ) {
                    __SiteCommonName_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "SiteCommonName parameter \"" + SiteCommonName + "\".  Select a different value or Cancel." );
            }
        }
        // First populate the choices...
        populateDataTypeCommonNameChoices(getReclamationHDB_DMI() );
        if ( JGUIUtil.isSimpleJComboBoxItem(__DataTypeCommonName_JComboBox, DataTypeCommonName, JGUIUtil.NONE, null, null ) ) {
            __DataTypeCommonName_JComboBox.select ( DataTypeCommonName );
        }
        else {
            if ( (DataTypeCommonName == null) || DataTypeCommonName.equals("") ) {
                // New command...select the default...
                if ( __DataTypeCommonName_JComboBox.getItemCount() > 0 ) {
                    __DataTypeCommonName_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "DataTypeCommonName parameter \"" + DataTypeCommonName + "\".  Select a different value or Cancel." );
            }
        }
        // First populate the choices - put this before other model parameters because it creates data used by the others
        populateModelRunIDChoices(getReclamationHDB_DMI() );
        // Select based on the first token
        index = new int[1];
        if ( JGUIUtil.isSimpleJComboBoxItem(__ModelRunID_JComboBox, ModelRunID, JGUIUtil.CHECK_SUBSTRINGS, " ", 0, index, false) ) {
            __ModelRunID_JComboBox.select ( index[0] );
        }
        else {
            if ( (ModelRunID == null) || ModelRunID.equals("") ) {
                // New command...select the default...
                if ( __ModelRunID_JComboBox.getItemCount() > 0 ) {
                    __ModelRunID_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "ModelRunID parameter \"" + ModelRunID + "\".  Select a different value or Cancel." );
            }
        }
        // First populate the choices...
        populateModelNameChoices(getReclamationHDB_DMI() );
        if ( JGUIUtil.isSimpleJComboBoxItem(__ModelName_JComboBox, ModelName, JGUIUtil.NONE, null, null ) ) {
            __ModelName_JComboBox.select ( ModelName );
            __main_JTabbedPane.setSelectedIndex(0);
            if ( __ignoreEvents ) {
                // Also need to make sure that the __modelRunList is populated
                // Call manually because events are disabled at startup to allow cascade to work properly
                //xxx
                //readModelRunListForSelectedModel(__dmi);
            }
        }
        else {
            if ( (ModelName == null) || ModelName.equals("") ) {
                // New command...select the default...
                if ( __ModelName_JComboBox.getItemCount() > 0 ) {
                    __ModelName_JComboBox.select ( 0 );
                    if ( __ignoreEvents ) {
                        // Also need to make sure that the __modelRunList is populated
                        // Call manually because events are disabled at startup to allow cascade to work properly
                        //xxx
                        //readModelRunListForSelectedModel(__dmi);
                    }
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "ModelName parameter \"" + ModelName + "\".  Select a different value or Cancel." );
            }
        }
        // First populate the choices...
        populateModelRunNameChoices(getReclamationHDB_DMI() );
        if ( JGUIUtil.isSimpleJComboBoxItem(__ModelRunName_JComboBox, ModelRunName, JGUIUtil.NONE, null, null ) ) {
            __ModelRunName_JComboBox.select ( ModelRunName );
        }
        else {
            if ( (ModelRunName == null) || ModelRunName.equals("") ) {
                // New command...select the default...
                if ( __ModelRunName_JComboBox.getItemCount() > 0 ) {
                    __ModelRunName_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "ModelRunName parameter \"" + ModelRunName + "\".  Select a different value or Cancel." );
            }
        }
        // First populate the choices...
        populateHydrologicIndicatorChoices(getReclamationHDB_DMI() );
        if ( JGUIUtil.isSimpleJComboBoxItem(__HydrologicIndicator_JComboBox, HydrologicIndicator, JGUIUtil.NONE, null, null ) ) {
            __HydrologicIndicator_JComboBox.select ( HydrologicIndicator );
        }
        else {
            if ( (HydrologicIndicator == null) || HydrologicIndicator.equals("") ) {
                // New command...select the default...
                if ( __HydrologicIndicator_JComboBox.getItemCount() > 0 ) {
                    __HydrologicIndicator_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "HydrologicIndicator parameter \"" + HydrologicIndicator + "\".  Select a different value or Cancel." );
            }
        }
        // First populate the choices...
        populateModelRunDateChoices(getReclamationHDB_DMI() );
        if ( JGUIUtil.isSimpleJComboBoxItem(__ModelRunDate_JComboBox, ModelRunDate, JGUIUtil.NONE, null, null ) ) {
            __ModelRunDate_JComboBox.select ( ModelRunDate );
        }
        else {
            if ( (ModelRunDate == null) || ModelRunDate.equals("") ) {
                // New command...select the default...
                if ( __ModelRunDate_JComboBox.getItemCount() > 0 ) {
                    __ModelRunDate_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "ModelRunDate parameter \"" + ModelRunDate + "\".  Select a different value or Cancel." );
            }
        }
        // First populate the choices...
        populateEnsembleNameChoices(getReclamationHDB_DMI() );
        if ( JGUIUtil.isSimpleJComboBoxItem(__EnsembleName_JComboBox, EnsembleName, JGUIUtil.NONE, null, null ) ) {
            __EnsembleName_JComboBox.select ( EnsembleName );
            __main_JTabbedPane.setSelectedIndex(1);
            if ( __ignoreEvents ) {
                // Also need to make sure that the __modelRunList is populated
                // Call manually because events are disabled at startup to allow cascade to work properly
                // xxx
                //readModelRunListForSelectedModel(__dmi);
            }
        }
        else {
            if ( (EnsembleName == null) || EnsembleName.equals("") ) {
                // New command...select the default...
                if ( __EnsembleName_JComboBox.getItemCount() > 0 ) {
                    __EnsembleName_JComboBox.select ( 0 );
                    if ( __ignoreEvents ) {
                        // Also need to make sure that the __modelRunList is populated
                        // Call manually because events are disabled at startup to allow cascade to work properly
                        //xxx
                        //readModelRunListForSelectedModel(__dmi);
                    }
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "EnsembleName parameter \"" + EnsembleName + "\".  Select a different value or Cancel." );
            }
        }
		if ( OutputEnsembleID != null ) {
			__OutputEnsembleID_JTextField.setText ( OutputEnsembleID );
		}
        /*
        // First populate the choices...
        populateEnsembleModelRunIDChoices(getReclamationHDB_DMI() );
        // Select based on the first token
        index = new int[1];
        if ( JGUIUtil.isSimpleJComboBoxItem(__EnsembleModelRunID_JComboBox, EnsembleModelRunID, JGUIUtil.CHECK_SUBSTRINGS, " ", 0, index, false) ) {
            __EnsembleModelRunID_JComboBox.select ( index[0] );
        }
        else {
            if ( (EnsembleModelRunID == null) || EnsembleModelRunID.equals("") ) {
                // New command...select the default...
                if ( __EnsembleModelRunID_JComboBox.getItemCount() > 0 ) {
                    __EnsembleModelRunID_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "EnsembleModelRunID parameter \"" + EnsembleModelRunID + "\".  Select a different value or Cancel." );
            }
        }
        */
        if ( Properties != null ) {
            __Properties_JTextArea.setText ( Properties );
        }
		if ( InputStart != null ) {
			__InputStart_JTextField.setText ( InputStart );
		}
		if ( InputEnd != null ) {
			__InputEnd_JTextField.setText ( InputEnd );
		}
        if ( Alias != null ) {
            __Alias_JTextField.setText ( Alias );
        }
        // Select tabs based on specified parameters
        if ( ((SiteDataTypeID != null) && !SiteDataTypeID.equals("")) ||
            ((SiteCommonName != null) && !SiteCommonName.equals("")) ) {
            __main_JTabbedPane.setSelectedIndex(1);
        }
        if ( ((EnsembleName != null) && !EnsembleName.equals("")) ||
            ((EnsembleModelRunID != null) && !EnsembleModelRunID.equals(""))) {
            __inner_JTabbedPane.setSelectedIndex(1);
        }
	}
	// Regardless, reset the command from the fields...
	Alias = __Alias_JTextField.getText().trim();
	// Regardless, reset the command from the fields...
	props = new PropList ( __command.getCommandName() );
	DataStore = __DataStore_JComboBox.getSelected();
	if ( DataStore == null ) {
	    DataStore = "";
	}
	else {
	    DataStore = DataStore.trim();
	}
	DataType = __DataType_JComboBox.getSelected().trim();
	Interval = __Interval_JComboBox.getSelected().trim();
	NHourIntervalOffset = __NHourIntervalOffset_JTextField.getText().trim();
    props.add ( "DataStore=" + DataStore );
    props.add ( "DataType=" + DataType );
    props.add ( "Interval=" + Interval );
	props.add ( "NHourIntervalOffset=" + NHourIntervalOffset );
	// Add the where clause(s)...
	InputFilter_JPanel filter_panel = __inputFilter_JPanel;
	int nfg = filter_panel.getNumFilterGroups();
	String where;
	String delim = ";";	// To separate input filter parts
	if ( ((ReclamationHDB_TimeSeries_InputFilter_JPanel)filter_panel).getDataStore() != null ) {
    	for ( int ifg = 0; ifg < nfg; ifg ++ ) {
    		where = filter_panel.toString(ifg,delim).trim();
    		// Make sure there is a field that is being checked in a where clause...
    		if ( (where.length() > 0) && !where.startsWith(delim) ) {
    		    // FIXME SAM 2010-11-01 The following discards '=' in the quoted string
    			//props.add ( "Where" + (ifg + 1) + "=" + where );
    			props.set ( "Where" + (ifg + 1), where );
    		}
    	}
	}
    // FIXME SAM 2011-10-03 Should be able to remove check for null if events and list population are
    // implemented correctly
    SiteCommonName = __SiteCommonName_JComboBox.getSelected();
    if ( SiteCommonName == null ) {
        SiteCommonName = "";
    }
    DataTypeCommonName = __DataTypeCommonName_JComboBox.getSelected();
    if ( DataTypeCommonName == null ) {
        DataTypeCommonName = "";
    }
    SiteDataTypeID = getSelectedSiteDataTypeID();
    ModelName = __ModelName_JComboBox.getSelected();
    if ( ModelName == null ) {
        ModelName = "";
    }
    ModelRunName = __ModelRunName_JComboBox.getSelected();
    if ( ModelRunName == null ) {
        ModelRunName = "";
    }
    ModelRunDate = __ModelRunDate_JComboBox.getSelected();
    if ( ModelRunDate == null ) {
        ModelRunDate = "";
    }
    HydrologicIndicator = __HydrologicIndicator_JComboBox.getSelected();
    if ( HydrologicIndicator == null ) {
        HydrologicIndicator = "";
    }
    ModelRunID = getSelectedModelRunID();
    if ( ModelRunID == null ) {
        ModelRunID = "";
    }
    EnsembleName = __EnsembleName_JComboBox.getSelected();
    if ( EnsembleName == null ) {
        EnsembleName = "";
    }
    OutputEnsembleID = __OutputEnsembleID_JTextField.getText();
    // TODO SAM 2016-04-29 Why are some of these commented out?
    //EnsembleTraceID = __EnsembleTraceID_JTextField.getText().trim();
    //EnsembleModelName = __EnsembleModelName_JComboBox.getSelected();
    if ( EnsembleModelName == null ) {
        EnsembleModelName = "";
    }
    //EnsembleModelRunDate = __EnsembleModelRunDate_JComboBox.getSelected();
    if ( EnsembleModelRunDate == null ) {
        EnsembleModelRunDate = "";
    }
    //EnsembleModelRunID = getSelectedEnsembleModelRunID();
    if ( EnsembleModelRunID == null ) {
        EnsembleModelRunID = "";
    }
    props.add ( "SiteCommonName=" + SiteCommonName );
    props.add ( "DataTypeCommonName=" + DataTypeCommonName );
    props.add ( "SiteDataTypeID=" + SiteDataTypeID );
    props.add ( "ModelName=" + ModelName );
    props.add ( "ModelRunName=" + ModelRunName );
    props.add ( "ModelRunDate=" + ModelRunDate );
    props.add ( "HydrologicIndicator=" + HydrologicIndicator );
    props.add ( "ModelRunID=" + ModelRunID );
    props.add ( "EnsembleName=" + EnsembleName );
    props.add ( "OutputEnsembleID=" + OutputEnsembleID );
    //props.add ( "EnsembleTraceID=" + EnsembleTraceID );
    props.add ( "EnsembleModelName=" + EnsembleModelName );
    props.add ( "EnsembleModelRunDate=" + EnsembleModelRunDate );
    props.add ( "EnsembleModelRunID=" + EnsembleModelRunID );
    Properties = __Properties_JTextArea.getText().trim().replace("\n"," ");
    props.add ( "Properties=" + Properties );
	InputStart = __InputStart_JTextField.getText().trim();
	props.add ( "InputStart=" + InputStart );
	InputEnd = __InputEnd_JTextField.getText().trim();
	props.add ( "InputEnd=" + InputEnd );
	props.add ( "Alias=" + Alias );
	__command_JTextArea.setText( __command.toString ( props ) );

	// Check the GUI state to determine whether some controls should be disabled.

	checkGUIState();
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed
and the dialog is closed.
*/
private void response ( boolean ok )
{	__ok = ok;	// Save to be returned by ok()
	if ( ok ) {
		// Commit the changes...
		commitEdits ();
		if ( __error_wait ) {
			// Not ready to close out!
			return;
		}
	}
	// Now close out...
	setVisible( false );
	dispose();
}

/**
Set the internal data based on the selected datastore.
*/
private void setDMIForSelectedDataStore()
{   ReclamationHDBDataStore ds = getSelectedDataStore();
	if ( ds == null ) {
		Message.printWarning(1,"ReadReclamationHDB","Unable to match datastore \"" +
			__DataStore_JComboBox.getSelected() + "\" in command  with available datastores.");
	}
    __dmi = (ReclamationHDB_DMI)ds.getDMI();
}

/**
Set the HDB model list corresponding to the displayed list.
*/
private void setModelList ( List<ReclamationHDB_Model> modelList )
{
    __modelList = modelList;
}

/**
Set the HDB model run list corresponding to the displayed site_data_type id and data interval.
*/
private void setModelRunList ( List<ReclamationHDB_ModelRun> modelRunList )
{
    __modelRunList = modelRunList;
}

/**
Set the HDB site data type list corresponding to the displayed list.
*/
private void setSiteDataTypeList ( List<ReclamationHDB_SiteDataType> siteDataTypeList )
{
    __siteDataTypeList = siteDataTypeList;
}

/**
Update the ensemble information text fields.
*/
private void updateEnsembleIDTextFields ()
{   // Ensemble information...
    List<ReclamationHDB_Ensemble> ensembleList = null;
    /*
    try {
        ensembleList = __dmi.findEnsemble(__ensembleList, __EnsembleName_JComboBox.getSelected() );
    }
    catch ( Exception e ) {
        // Generally due to startup with bad datastore
        ensembleList = null;
    }
    if ( (ensembleList == null) || (ensembleList.size() == 0) ) {
        __selectedEnsembleID_JLabel.setText ( "No matches" );
    }
    else if ( ensembleList.size() == 1 ) {
        __selectedEnsembleID_JLabel.setText ( "" + ensembleList.get(0).getEnsembleID() );
    }
    else {
        __selectedEnsembleID_JLabel.setText ( "" + ensembleList.size() + " matches" );
    }
    // Model information...
    List<ReclamationHDB_Model> modelList = null;
    try {
        modelList = __dmi.findModel(__modelList, __EnsembleModelName_JComboBox.getSelected() );
    }
    catch ( Exception e ) {
        // Generally due to startup with bad datastore
        modelList = null;
    }
    if ( (modelList == null) || (modelList.size() == 0) ) {
        __selectedEnsembleModelID_JLabel.setText ( "No matches" );
    }
    else if ( modelList.size() == 1 ) {
        __selectedEnsembleModelID_JLabel.setText ( "" + modelList.get(0).getModelID() );
    }
    else {
        __selectedEnsembleModelID_JLabel.setText ( "" + modelList.size() + " matches" );
    }
    */
}

/**
Update the model information text fields.
*/
private void updateModelIDTextFields ()
{   // Model information...
    List<ReclamationHDB_Model> modelList = null;
    try {
        modelList = __dmi.findModel(__modelList, __ModelName_JComboBox.getSelected() );
    }
    catch ( Exception e ) {
        // Generally due to startup with bad datastore
        modelList = null;
    }
    if ( (modelList == null) || (modelList.size() == 0) ) {
        __selectedModelID_JLabel.setText ( "No matches" );
    }
    else if ( modelList.size() == 1 ) {
        __selectedModelID_JLabel.setText ( "" + modelList.get(0).getModelID() );
    }
    else {
        __selectedModelID_JLabel.setText ( "" + modelList.size() + " matches" );
    }
    // Model run information...
    List<ReclamationHDB_ModelRun> modelRunList = null;
    try {
        String hydrologicIndicator = __HydrologicIndicator_JComboBox.getSelected();
        if ( hydrologicIndicator == null ) {
            hydrologicIndicator = ""; // There are nulls/blanks in the database so need to match
        }
        modelRunList = __dmi.findModelRun(__modelRunList,
            Integer.parseInt(__selectedModelID_JLabel.getText()),
            __ModelRunName_JComboBox.getSelected(),
            __ModelRunDate_JComboBox.getSelected(),
            hydrologicIndicator );
    }
    catch ( Exception e ) {
        // Generally due to startup with bad datastore
        modelRunList = null;
    }
    if ( (modelRunList == null) || (modelRunList.size() == 0) ) {
        __selectedModelRunID_JLabel.setText ( "No matches" );
    }
    else if ( modelRunList.size() == 1 ) {
        __selectedModelRunID_JLabel.setText ( "" + modelRunList.get(0).getModelRunID() );
    }
    else {
        __selectedModelRunID_JLabel.setText ( "" + modelRunList.size() + " matches" );
    }
}

/**
Update the model information text fields.
*/
private void updateSiteIDTextFields ()
{
    List<ReclamationHDB_SiteDataType> stdList = null;
    try {
        stdList = __dmi.findSiteDataType(__siteDataTypeList, __SiteCommonName_JComboBox.getSelected(), null );
    }
    catch ( Exception e ) {
        // Generally at startup with a bad datastore configuration
        stdList = null;
    }
    if ( (stdList == null) || (stdList.size() == 0) ) {
        __selectedSiteID_JLabel.setText ( "No matches" );
    }
    else if ( stdList.size() > 0 ) {
        __selectedSiteID_JLabel.setText ( "" + stdList.get(0).getSiteID() + " (" + stdList.size() + " matches)" );
    }
    else {
        __selectedSiteID_JLabel.setText ( "" + stdList.size() + " matches" );
    }

    try {
        stdList = __dmi.findSiteDataType(__siteDataTypeList,
            __SiteCommonName_JComboBox.getSelected(), __DataTypeCommonName_JComboBox.getSelected() );
    }
    catch ( Exception e ) {
        // Generally at startup with a bad datastore configuration
        stdList = null;
    }
    if ( (stdList == null) || (stdList.size() == 0) ) {
        __selectedSiteDataTypeID_JLabel.setText ( "No matches" );
    }
    else if ( stdList.size() == 1 ) {
        String sdi = "" + stdList.get(0).getSiteDataTypeID();
        __selectedSiteDataTypeID_JLabel.setText ( "" + sdi );
        // Select the item in the SiteDataTypeID choice, but only if it is not already set (want SDI to take precedence if specified)
        if ( (__SiteDataTypeID_JComboBox.getSelected() == null) || __SiteDataTypeID_JComboBox.getSelected().equals("") ) {
            int [] index = new int[1];
            if ( JGUIUtil.isSimpleJComboBoxItem(__SiteDataTypeID_JComboBox, sdi, JGUIUtil.CHECK_SUBSTRINGS, " ", 0, index, false) ) {
                __SiteDataTypeID_JComboBox.select ( index[0] );
            }
            else {
                // New command...select the default...
                if ( __SiteDataTypeID_JComboBox.getItemCount() > 0 ) {
                    __SiteDataTypeID_JComboBox.select ( 0 );
                }
            }
        }
    }
    else {
        __selectedSiteDataTypeID_JLabel.setText ( "" + stdList.size() + " matches" );
    }
}

/**
Responds to WindowEvents.
@param event WindowEvent object
*/
public void windowClosing( WindowEvent event )
{	response ( false );
}

public void windowActivated( WindowEvent evt ){;}
public void windowClosed( WindowEvent evt ){;}
public void windowDeactivated( WindowEvent evt ){;}
public void windowDeiconified( WindowEvent evt ){;}
public void windowIconified( WindowEvent evt ){;}
public void windowOpened( WindowEvent evt ){;}

}