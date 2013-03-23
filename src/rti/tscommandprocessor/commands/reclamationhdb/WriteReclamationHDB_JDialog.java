package rti.tscommandprocessor.commands.reclamationhdb;

import java.awt.Color;
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

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

import riverside.datastore.DataStore;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;

import RTi.DMI.DatabaseDataStore;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;

/**
Command editor dialog for the WriteReclamationHDB() command.
Very important - this dialog uses HDB data to populate lists and requires that the time series
metadata are already defined.  Consequently, list choices cascade to valid options rather than
letting the user define new combinations.
*/
public class WriteReclamationHDB_JDialog extends JDialog
implements ActionListener, KeyListener, ItemListener, WindowListener
{

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJComboBox __DataStore_JComboBox = null;
private SimpleJComboBox __TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox __TSID_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private WriteReclamationHDB_Command __command = null;
private JTextArea __command_JTextArea = null;
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
private SimpleJComboBox __Agency_JComboBox = null;
private SimpleJComboBox __ValidationFlag_JComboBox = null;
private SimpleJComboBox __OverwriteFlag_JComboBox = null;
private JTextField __DataFlags_JTextField = null;
private SimpleJComboBox __TimeZone_JComboBox = null;
private JLabel __TimeZone_JLabel = null;
private JTextField __OutputStart_JTextField = null;
private JTextField __OutputEnd_JTextField = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Has user pressed OK to close the dialog?

private boolean __ignoreEvents = false; // Used to ignore cascading events when initializing the components

private ReclamationHDBDataStore __dataStore = null; // selected ReclamationHDBDataStore
private ReclamationHDB_DMI __dmi = null; // ReclamationHDB_DMI to do queries.

private List<ReclamationHDB_SiteDataType> __siteDataTypeList = new Vector(); // Corresponds to displayed list
private List<ReclamationHDB_Model> __modelList = new Vector(); // Corresponds to displayed list (has model_id)
private List<ReclamationHDB_ModelRun> __modelRunList = new Vector(); // Corresponds to models matching model_id

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public WriteReclamationHDB_JDialog ( JFrame parent, WriteReclamationHDB_Command command )
{	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	if ( __ignoreEvents ) {
        return; // Startup.
    }
    Object o = event.getSource();
	if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
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
    populateModelNameChoices ( __dmi );
    populateModelRunIDChoices ( __dmi );
    populateAgencyChoices ( __dmi );
    populateValidationFlagChoices ( __dmi );
    populateOverwriteFlagChoices ( __dmi );
    populateTimeZoneChoices ( __dmi );
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
    updateModelIDTextFields ();
}

/**
Refresh the query choices for the currently selected ReclamationHDB model name.
*/
private void actionPerformedModelNameSelected ( )
{
    if ( __ModelName_JComboBox.getSelected() == null ) {
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
    populateHydrologicIndicatorChoices ( __dmi );
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
    populateModelRunDateChoices ( __dmi );
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
Check the GUI state to make sure that appropriate components are enabled/disabled.
*/
private void checkGUIState ()
{
    String TSList = __TSList_JComboBox.getSelected();
    if ( TSListType.ALL_MATCHING_TSID.equals(TSList) ||
        TSListType.FIRST_MATCHING_TSID.equals(TSList) ||
        TSListType.LAST_MATCHING_TSID.equals(TSList) ) {
        __TSID_JComboBox.setEnabled(true);
        __TSID_JLabel.setEnabled ( true );
    }
    else {
        __TSID_JComboBox.setEnabled(false);
        __TSID_JLabel.setEnabled ( false );
    }
    if ( TSListType.ENSEMBLE_ID.equals(TSList)) {
        __EnsembleID_JComboBox.setEnabled(true);
        __EnsembleID_JLabel.setEnabled ( true );
    }
    else {
        __EnsembleID_JComboBox.setEnabled(false);
        __EnsembleID_JLabel.setEnabled ( false );
    }
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	if ( __ignoreEvents ) {
        // Startup
        return;
    }
    // Put together a list of parameters to check...
	PropList parameters = new PropList ( "" );
    String DataStore = __DataStore_JComboBox.getSelected();
    String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String SiteCommonName = __SiteCommonName_JComboBox.getSelected();
    String DataTypeCommonName = __DataTypeCommonName_JComboBox.getSelected();
    String SiteDataTypeID = __SiteDataTypeID_JComboBox.getSelected();
    String ModelName = __ModelName_JComboBox.getSelected();
    String ModelRunName = __ModelRunName_JComboBox.getSelected();
    String ModelRunDate = __ModelRunDate_JComboBox.getSelected();
    String HydrologicIndicator = __HydrologicIndicator_JComboBox.getSelected();
    String ModelRunID = __ModelRunID_JComboBox.getSelected();
    String Agency = getSelectedAgency();
    String ValidationFlag = getSelectedValidationFlag();
    String OverwriteFlag = getSelectedOverwriteFlag();
    String DataFlags = __DataFlags_JTextField.getText().trim();
    String TimeZone = __TimeZone_JComboBox.getSelected();
	String OutputStart = __OutputStart_JTextField.getText().trim();
	String OutputEnd = __OutputEnd_JTextField.getText().trim();

	__error_wait = false;

    if ( DataStore.length() > 0 ) {
        parameters.set ( "DataStore", DataStore );
    }
	if ( TSList.length() > 0 ) {
		parameters.set ( "TSList", TSList );
	}
    if ( TSID.length() > 0 ) {
        parameters.set ( "TSID", TSID );
    }
    if ( EnsembleID.length() > 0 ) {
        parameters.set ( "EnsembleID", EnsembleID );
    }
    if ( (SiteCommonName != null) && (SiteCommonName.length() > 0) ) {
        parameters.set ( "SiteCommonName", SiteCommonName );
    }
    if ( (DataTypeCommonName != null) && (DataTypeCommonName.length() > 0) ) {
        parameters.set ( "DataTypeCommonName", DataTypeCommonName );
    }
    if ( (SiteDataTypeID != null) && (SiteDataTypeID.length() > 0) ) {
        parameters.set ( "SiteDataTypeID", SiteDataTypeID );
    }
    if ( (ModelName != null) && (ModelName.length() > 0) ) {
        parameters.set ( "ModelName", ModelName );
    }
    if ( (ModelRunName != null) && (ModelRunName.length() > 0) ) {
        parameters.set ( "ModelRunName", ModelRunName );
    }
    if ( (ModelRunDate != null) && (ModelRunDate.length() > 0) ) {
        parameters.set ( "ModelRunDate", ModelRunDate );
    }
    if ( HydrologicIndicator.length() > 0 ) {
        parameters.set ( "HydrologicIndicator", HydrologicIndicator );
    }
    if ( (ModelRunID != null) && (ModelRunID.length() > 0) ) {
        parameters.set ( "ModelRunID", ModelRunID );
    }
    if ( (Agency != null) && (Agency.length() > 0) ) {
        parameters.set ( "Agency", Agency );
    }
    if ( (ValidationFlag != null) && (ValidationFlag.length() > 0) ) {
        parameters.set ( "ValidationFlag", ValidationFlag );
    }
    if ( (OverwriteFlag != null) && (OverwriteFlag.length() > 0) ) {
        parameters.set ( "OverwriteFlag", OverwriteFlag );
    }
    if ( DataFlags.length() > 0 ) {
        parameters.set ( "DataFlags", DataFlags );
    }
    if ( TimeZone.length() > 0 ) {
        parameters.set ( "TimeZone", TimeZone );
    }
	if ( OutputStart.length() > 0 ) {
		parameters.set ( "OutputStart", OutputStart );
	}
	if ( OutputEnd.length() > 0 ) {
		parameters.set ( "OutputEnd", OutputEnd );
	}
	try {
	    // This will warn the user...
		__command.checkCommandParameters ( parameters, null, 1 );
	}
	catch ( Exception e ) {
		// The warning would have been printed in the check code.
		Message.printWarning(2,"",e);
		__error_wait = true;
	}
}

/**
Commit the edits to the command.  In this case the command parameters have
already been checked and no errors were detected.
*/
private void commitEdits ()
{	String DataStore = __DataStore_JComboBox.getSelected();
    String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String SiteCommonName = __SiteCommonName_JComboBox.getSelected();
    String DataTypeCommonName = __DataTypeCommonName_JComboBox.getSelected();
    String SiteDataTypeID = __SiteDataTypeID_JComboBox.getSelected();
    String ModelName = __ModelName_JComboBox.getSelected();
    String ModelRunName = __ModelRunName_JComboBox.getSelected();
    String ModelRunDate = __ModelRunDate_JComboBox.getSelected();
    String HydrologicIndicator = __HydrologicIndicator_JComboBox.getSelected();
    String ModelRunID = __ModelRunID_JComboBox.getSelected();
    String Agency = getSelectedAgency();
    String ValidationFlag = getSelectedValidationFlag();
    String OverwriteFlag = getSelectedOverwriteFlag();
    String DataFlags = __DataFlags_JTextField.getText().trim();
    String TimeZone = __TimeZone_JComboBox.getSelected();
	String OutputStart = __OutputStart_JTextField.getText().trim();
	String OutputEnd = __OutputEnd_JTextField.getText().trim();
	__command.setCommandParameter ( "DataStore", DataStore );
	__command.setCommandParameter ( "TSList", TSList );
    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
    __command.setCommandParameter ( "SiteCommonName", SiteCommonName );
    __command.setCommandParameter ( "DataTypeCommonName", DataTypeCommonName );
    __command.setCommandParameter ( "SiteDataTypeID", SiteDataTypeID );
    __command.setCommandParameter ( "ModelName", ModelName );
    __command.setCommandParameter ( "ModelRunName", ModelRunName );
    __command.setCommandParameter ( "ModelRunDate", ModelRunDate );
    __command.setCommandParameter ( "HydrologicIndicator", HydrologicIndicator );
    __command.setCommandParameter ( "ModelRunID", ModelRunID );
    __command.setCommandParameter ( "Agency", Agency );
    __command.setCommandParameter ( "ValidationFlag", ValidationFlag );
    __command.setCommandParameter ( "OverwriteFlag", OverwriteFlag );
    __command.setCommandParameter ( "DataFlags", DataFlags );
    __command.setCommandParameter ( "TimeZone", TimeZone );
	__command.setCommandParameter ( "OutputStart", OutputStart );
	__command.setCommandParameter ( "OutputEnd", OutputEnd );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__cancel_JButton = null;
	__command_JTextArea = null;
	__OutputStart_JTextField = null;
	__OutputEnd_JTextField = null;
	__TSList_JComboBox = null;
	__command = null;
	__ok_JButton = null;
	super.finalize ();
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
Return the selected agency abbreviation, used to provide intelligent parameter choices.
The displayed format is:  "AgencyAbbrev - AgencyName"
@return the selected agency, or "" if nothing selected
*/
private String getSelectedAgency ()
{   String agency = __Agency_JComboBox.getSelected();
    if ( agency == null ) {
        return "";
    }
    else if ( agency.indexOf("-") > 0 ) {
        return agency.substring(0,agency.indexOf("-")).trim();
    }
    else {
        return agency.trim();
    }
}

/**
Return the selected datastore, used to provide intelligent parameter choices.
@return the selected datastore, or null if nothing selected (or none available)
*/
private ReclamationHDBDataStore getSelectedDataStore ()
{   
    List<DataStore> dataStoreList =
        ((TSCommandProcessor)__command.getCommandProcessor()).getDataStoresByType(
            ReclamationHDBDataStore.class );
    String dataStoreNameSelected = __DataStore_JComboBox.getSelected();
    if ( (dataStoreNameSelected != null) && !dataStoreNameSelected.equals("") ) {
        for ( DataStore dataStore : dataStoreList ) {
            if ( dataStore.getName().equalsIgnoreCase(dataStoreNameSelected) ) {
                return (ReclamationHDBDataStore)dataStore;
            }
        }
    }
    return null;
}

/**
Return the selected overwrite flag, used to provide intelligent parameter choices.
The displayed format is:  "OverwriteFlag - Name"
@return the selected flag, or "" if nothing selected
*/
private String getSelectedOverwriteFlag()
{   String overwriteFlag = __OverwriteFlag_JComboBox.getSelected();
    if ( overwriteFlag == null ) {
        return "";
    }
    else if ( overwriteFlag.indexOf("-") > 0 ) {
        return overwriteFlag.substring(0,overwriteFlag.indexOf("-")).trim();
    }
    else {
        return overwriteFlag.trim();
    }
}

/**
Return the selected validation flag, used to provide intelligent parameter choices.
The displayed format is:  "ValidationFlag - Name"
@return the selected flag, or "" if nothing selected
*/
private String getSelectedValidationFlag()
{   String validationFlag = __ValidationFlag_JComboBox.getSelected();
    if ( validationFlag == null ) {
        return "";
    }
    else if ( validationFlag.indexOf("-") > 0 ) {
        return validationFlag.substring(0,validationFlag.indexOf("-")).trim();
    }
    else {
        return validationFlag.trim();
    }
}

/**
Instantiates the GUI components.
@param parent Frame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, WriteReclamationHDB_Command command )
{	__command = command;
    CommandProcessor processor = __command.getCommandProcessor();

	addWindowListener( this );

    Insets insetsTLBR = new Insets(1,2,1,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int yMain = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Write one time series or ensemble to a Reclamation HDB database." ),
		0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The string parameters are used to determine database internal numeric primary keys " +
        "(site_datatype_id and optionally model_run_id for model data)."),
        0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "As an alternative, the site_datatype_id and model_run_id can be specified directly if the values are known."),
        0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The time series table is determined from the data interval, with irregular data being written to the " +
        "instantaneous data table." ),
        0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "TSTool will only write time series records but will not write records for " +
        "time series metadata (time series must have been previously defined)."),
        0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Enter output date/times to a " +
		"precision appropriate for output time series."),
		0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    __ignoreEvents = true; // So that a full pass of initialization can occur
    
    // List available datastores of the correct type
    // Other lists are NOT populated until a datastore is selected (driven by events)
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Datastore:"),
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStore_JComboBox = new SimpleJComboBox ( false );
    List<DataStore> dataStoreList = ((TSCommandProcessor)processor).getDataStoresByType(
        ReclamationHDBDataStore.class );
    for ( DataStore dataStore: dataStoreList ) {
        __DataStore_JComboBox.addItem ( dataStore.getName() );
    }
    if ( __DataStore_JComboBox.getItemCount() > 0 ) {
        __DataStore_JComboBox.select ( 0 );
    }
    __DataStore_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __DataStore_JComboBox,
        1, yMain, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - datastore for HDB database."), 
        3, yMain, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    __TSList_JComboBox = new SimpleJComboBox(false);
    yMain = CommandEditorUtil.addTSListToEditorDialogPanel ( this, main_JPanel, __TSList_JComboBox, yMain );

    __TSID_JLabel = new JLabel ("TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __TSID_JComboBox = new SimpleJComboBox ( true );  // Allow edits
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    yMain = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, main_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, yMain );
    
    __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    yMain = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
        this, this, main_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, yMain );
    
    // Panel to control site_datatype_id selection
    int ySiteDataType = -1;
    JPanel siteDataType_JPanel = new JPanel();
    siteDataType_JPanel.setLayout( new GridBagLayout() );
    siteDataType_JPanel.setBorder( BorderFactory.createTitledBorder (
        BorderFactory.createLineBorder(Color.black),"Specify how to match the HDB site_datatype_id" ));
    JGUIUtil.addComponent( main_JPanel, siteDataType_JPanel,
        0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(siteDataType_JPanel, new JLabel ("Site common name:"), 
        0, ++ySiteDataType, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SiteCommonName_JComboBox = new SimpleJComboBox (false);
    __SiteCommonName_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(siteDataType_JPanel, __SiteCommonName_JComboBox,
        1, ySiteDataType, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(siteDataType_JPanel, new JLabel (
        "Required - used with data type common name to determine site_datatype_id."),
        3, ySiteDataType, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(siteDataType_JPanel, new JLabel ("Data type common name:"), 
        0, ++ySiteDataType, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataTypeCommonName_JComboBox = new SimpleJComboBox (false);
    __DataTypeCommonName_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(siteDataType_JPanel, __DataTypeCommonName_JComboBox,
        1, ySiteDataType, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(siteDataType_JPanel, new JLabel (
        "Required - used with site common name to determine site_datatype_id."),
        3, ySiteDataType, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(siteDataType_JPanel, new JLabel ("Matching site_id:"), 
        0, ++ySiteDataType, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __selectedSiteID_JLabel = new JLabel ( "");
    JGUIUtil.addComponent(siteDataType_JPanel, __selectedSiteID_JLabel,
        1, ySiteDataType, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(siteDataType_JPanel, new JLabel (
        "Information - useful when comparing to database contents."),
        3, ySiteDataType, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(siteDataType_JPanel, new JLabel ("Matching site_datatype_id:"), 
        0, ++ySiteDataType, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __selectedSiteDataTypeID_JLabel = new JLabel ( "");
    JGUIUtil.addComponent(siteDataType_JPanel, __selectedSiteDataTypeID_JLabel,
        1, ySiteDataType, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(siteDataType_JPanel, new JLabel (
        "Information - useful when comparing to database contents."),
        3, ySiteDataType, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(siteDataType_JPanel, new JLabel ("Site data type ID:"), 
        0, ++ySiteDataType, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SiteDataTypeID_JComboBox = new SimpleJComboBox (false);
    __SiteDataTypeID_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(siteDataType_JPanel, __SiteDataTypeID_JComboBox,
        1, ySiteDataType, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(siteDataType_JPanel, new JLabel (
        "Optional - alternative to selecting above choices."),
        3, ySiteDataType, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Panel to control model selection
    int yModel = -1;
    JPanel model_JPanel = new JPanel();
    model_JPanel.setLayout( new GridBagLayout() );
    model_JPanel.setBorder( BorderFactory.createTitledBorder (
        BorderFactory.createLineBorder(Color.black),
        "Specify how to match the HDB model_run_id (leave blank if not writing model time series data)" ));
    JGUIUtil.addComponent( main_JPanel, model_JPanel,
        0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(model_JPanel, new JLabel ("Model name:"), 
        0, ++yModel, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ModelName_JComboBox = new SimpleJComboBox (false);
    __ModelName_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(model_JPanel, __ModelName_JComboBox,
        1, yModel, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(model_JPanel, new JLabel (
        "Required - used to determine the model_run_id."),
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
    
    JGUIUtil.addComponent(model_JPanel, new JLabel ("Model run date:"), 
        0, ++yModel, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ModelRunDate_JComboBox = new SimpleJComboBox (false);
    __ModelRunDate_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(model_JPanel, __ModelRunDate_JComboBox,
        1, yModel, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(model_JPanel, new JLabel (
        "Required - YYYY-MM-DD hh:mm:ss, used to determine the model_run_id."),
        3, yModel, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(model_JPanel, new JLabel ("Hydrologic indicator:"), 
        0, ++yModel, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __HydrologicIndicator_JComboBox = new SimpleJComboBox (false);
    __HydrologicIndicator_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(model_JPanel, __HydrologicIndicator_JComboBox,
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
    
    JGUIUtil.addComponent(model_JPanel, new JLabel ("Selected model_run_id:"), 
        0, ++yModel, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __selectedModelRunID_JLabel = new JLabel ( "");
    JGUIUtil.addComponent(model_JPanel, __selectedModelRunID_JLabel,
        1, yModel, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(model_JPanel, new JLabel (
        "Information - useful when comparing to database contents."),
        3, yModel, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(model_JPanel, new JLabel ("Model run ID:"), 
        0, ++yModel, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ModelRunID_JComboBox = new SimpleJComboBox (false);
    __ModelRunID_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(model_JPanel, __ModelRunID_JComboBox,
        1, yModel, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(model_JPanel, new JLabel (
        "Optional - alternative to selecting above choices."),
        3, yModel, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Agency:"), 
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Agency_JComboBox = new SimpleJComboBox ( false );
    __Agency_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(main_JPanel, __Agency_JComboBox,
        1, yMain, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - agency supplying data (default=no agency)."),
        3, yMain, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Validation flag:"), 
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ValidationFlag_JComboBox = new SimpleJComboBox ( false );
    __ValidationFlag_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(main_JPanel, __ValidationFlag_JComboBox,
        1, yMain, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - validation flag (default=no flag)."),
        3, yMain, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Overwrite flag:"), 
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OverwriteFlag_JComboBox = new SimpleJComboBox ( false );
    __OverwriteFlag_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(main_JPanel, __OverwriteFlag_JComboBox,
        1, yMain, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - overwrite flag (default=O)."),
        3, yMain, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Data flags:"), 
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataFlags_JTextField = new JTextField (20);
    __DataFlags_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __DataFlags_JTextField,
        1, yMain, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - user-defined flag (default=no flag)."),
        3, yMain, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Time zone:"), 
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TimeZone_JComboBox = new SimpleJComboBox ( false );
    __TimeZone_JComboBox.addItemListener (this);
    __TimeZone_JComboBox.setToolTipText ( "The time zone in time series is NOT used by default.  " +
    	"Use this parameter to tell the database the time zone for data." );
    JGUIUtil.addComponent(main_JPanel, __TimeZone_JComboBox,
        1, yMain, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __TimeZone_JLabel = new JLabel ("");
    JGUIUtil.addComponent(main_JPanel, __TimeZone_JLabel,
        3, yMain, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Output start:"), 
		0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OutputStart_JTextField = new JTextField (20);
	__OutputStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __OutputStart_JTextField,
		1, yMain, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - override the global output start (default=write all data)."),
		3, yMain, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Output end:"), 
		0, ++yMain, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OutputEnd_JTextField = new JTextField (20);
	__OutputEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __OutputEnd_JTextField,
		1, yMain, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - override the global output end (default=write all data)."),
		3, yMain, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
    		0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __command_JTextArea = new JTextArea ( 4, 50 );
    __command_JTextArea.setLineWrap ( true );
    __command_JTextArea.setWrapStyleWord ( true );
    __command_JTextArea.setEditable ( false );
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
    		1, yMain, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
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
	__cancel_JButton = new SimpleJButton("Cancel", "Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__ok_JButton = new SimpleJButton("OK", "OK", this);
	button_JPanel.add ( __ok_JButton );

	setTitle ( "Edit " + __command.getCommandName() + "() Command" );
	
	// Refresh the contents...
    checkGUIState();
    refresh ();
    __ignoreEvents = false; // After initialization of components let events happen to dynamically cause cascade
    checkGUIState(); // Do this again because it may not have happened due to the special event handling
    updateSiteIDTextFields();
    updateModelIDTextFields();
    
	setResizable ( false ); // TODO SAM 2010-12-10 Resizing causes some problems
    pack();
    JGUIUtil.center( this );
    super.setVisible( true );
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged (ItemEvent e)
{   if ( __ignoreEvents ) {
        return; // Startup
    }

    checkGUIState();
    Object source = e.getSource();
    int sc = e.getStateChange();
    if ( (source == __DataStore_JComboBox) && (sc == ItemEvent.SELECTED) ) {
        // User has selected a datastore.
        actionPerformedDataStoreSelected ();
    }
    else if ( (source == __SiteCommonName_JComboBox) && (sc == ItemEvent.SELECTED) ) {
        // User has selected a site common name.
        actionPerformedSiteCommonNameSelected ();
    }
    else if ( (source == __DataTypeCommonName_JComboBox) && (sc == ItemEvent.SELECTED) ) {
        // User has selected a data type common name.
        actionPerformedDataTypeCommonNameSelected ();
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
 
    refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	if ( __ignoreEvents ) {
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

public void keyReleased ( KeyEvent event )
{	if ( __ignoreEvents ) {
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
Populate the agency list based on the selected datastore.
*/
private void populateAgencyChoices ( ReclamationHDB_DMI rdmi )
{   String routine = getClass().getName() + ".populateAgencyChoices";
    if ( (rdmi == null) || (__Agency_JComboBox == null) ) {
        // Initialization
        return;
    }
    List<String> agencyStrings = new Vector();
    try {
        List<ReclamationHDB_Agency> agencyList = rdmi.getAgencyList();
        agencyStrings.add(""); // No agency will be used
        String agencyAbbrev;
        String agencyName;
        for ( ReclamationHDB_Agency agency: agencyList ) {
            agencyAbbrev = agency.getAgenAbbrev();
            agencyName = agency.getAgenName();
            if ( agencyAbbrev.length() > 0 ) {
                if ( agencyName.length() > 0 ) {
                    agencyStrings.add ( agencyAbbrev + " - " + agencyName );
                }
                else {
                    agencyStrings.add ( agencyAbbrev );
                }
            }
        }
        Collections.sort(agencyStrings,String.CASE_INSENSITIVE_ORDER);
    }
    catch ( Exception e ) {
        Message.printWarning(3, routine, "Error getting HDB agency list (" + e + ")." );
        agencyStrings = new Vector();
    }
    __Agency_JComboBox.removeAll ();
    __Agency_JComboBox.setData(agencyStrings);
    // Select first choice (may get reset from existing parameter values).
    __Agency_JComboBox.select ( null );
    if ( __Agency_JComboBox.getItemCount() > 0 ) {
        __Agency_JComboBox.select ( 0 );
    }
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
    List<String> dataTypeCommonNameStrings = new Vector();
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
Populate the model hydrologic indicator list based on the selected datastore.
*/
private void populateHydrologicIndicatorChoices ( ReclamationHDB_DMI rdmi )
{   String routine = getClass().getName() + ".populateHydrologicIndicatorChoices";
    if ( (rdmi == null) || (__HydrologicIndicator_JComboBox == null) ) {
        // Initialization
        return;
    }
    String selectedModelName = __ModelName_JComboBox.getSelected();
    String selectedModelRunName = __ModelRunName_JComboBox.getSelected();
    String selectedModelRunDate = __ModelRunDate_JComboBox.getSelected();
    List<ReclamationHDB_Model> modelList = rdmi.findModel(__modelList, selectedModelName);
    List<String> hydrologicIndicatorStrings = new Vector();
    hydrologicIndicatorStrings.add ( "" ); // Always add blank because user may not want model time series
    if ( modelList.size() == 1 ) {
        ReclamationHDB_Model model = modelList.get(0);
        int modelID = model.getModelID();
        List<ReclamationHDB_ModelRun> modelRunList = rdmi.findModelRun(__modelRunList, modelID,
                selectedModelRunName,
                selectedModelRunDate,
                null); // Don't match on hydrologic indicator
        // Results should list unique hydrologic indicators
        for ( ReclamationHDB_ModelRun modelRun: modelRunList ) {
            hydrologicIndicatorStrings.add ( modelRun.getHydrologicIndicator() );
        }
        Collections.sort(hydrologicIndicatorStrings,String.CASE_INSENSITIVE_ORDER);
        StringUtil.removeDuplicates(hydrologicIndicatorStrings, true, true);
    }
    else {
        Message.printStatus ( 2, routine, "Have " + modelList.size() + " models matching name \"" +
            selectedModelName + "\" - unable to find matching model runs." );
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
Populate the model name list based on the selected datastore.
*/
private void populateModelNameChoices ( ReclamationHDB_DMI rdmi )
{   String routine = getClass().getName() + ".populateModelNameChoices";
    if ( (rdmi == null) || (__ModelName_JComboBox == null) ) {
        // Initialization
        return;
    }
    List<String> modelNameStrings = new Vector();
    modelNameStrings.add ( "" ); // Always add blank because user may not want model time series
    try {
        readModelList(rdmi);
        for ( ReclamationHDB_Model model: __modelList ) {
            modelNameStrings.add ( model.getModelName() );
        }
        Collections.sort(modelNameStrings,String.CASE_INSENSITIVE_ORDER);
        StringUtil.removeDuplicates(modelNameStrings, true, true);
    }
    catch ( Exception e ) {
        Message.printWarning(3, routine, "Error getting HDB model list (" + e + ")." );
        modelNameStrings = new Vector();
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
{   String routine = getClass().getName() + ".populateModelRunDateChoices";
    if ( (rdmi == null) || (__ModelRunDate_JComboBox == null) ) {
        // Initialization
        return;
    }
    String selectedModelName = __ModelName_JComboBox.getSelected();
    String selectedModelRunName = __ModelRunName_JComboBox.getSelected();
    List<ReclamationHDB_Model> modelList = rdmi.findModel(__modelList, selectedModelName);
    List<String> runDateStrings = new Vector();
    runDateStrings.add ( "" ); // Always add blank because user may not want model time series
    if ( modelList.size() == 1 ) {
        ReclamationHDB_Model model = modelList.get(0);
        int modelID = model.getModelID();
        List<ReclamationHDB_ModelRun> modelRunList = rdmi.findModelRun(__modelRunList, modelID,
            selectedModelRunName,
            null, // Don't match on run date
            null); // Don't match on hydrologic indicator
        // Results should list unique hydrologic indicators
        for ( ReclamationHDB_ModelRun modelRun: modelRunList ) {
            runDateStrings.add ( "" + modelRun.getRunDate() );
        }
        Collections.sort(runDateStrings,String.CASE_INSENSITIVE_ORDER);
        StringUtil.removeDuplicates(runDateStrings, true, true);
    }
    else {
        Message.printStatus ( 2, routine, "Have " + modelList.size() + " models matching name \"" +
            selectedModelName + "\" - unable to find matching model runs." );
    }
    __ModelRunDate_JComboBox.removeAll ();
    __ModelRunDate_JComboBox.setData(runDateStrings);
    // Select first choice (may get reset from existing parameter values).
    __ModelRunDate_JComboBox.select ( null );
    if ( __ModelRunDate_JComboBox.getItemCount() > 0 ) {
        __ModelRunDate_JComboBox.select ( 0 );
    }
}

/**
Populate the model run ID list based on the selected datastore.
*/
private void populateModelRunIDChoices ( ReclamationHDB_DMI rdmi )
{   String routine = getClass().getName() + ".populateModelRunIDhoices";
    if ( (rdmi == null) || (__ModelRunID_JComboBox == null) ) {
        // Initialization
        return;
    }
    List<String> modelRunIDStrings = new Vector();
    modelRunIDStrings.add ( "" ); // Always add blank because user may not want model time series
    try {
        // There may be no run names for the model id.
        List<ReclamationHDB_ModelRun> modelRunList = rdmi.readHdbModelRunListForModelID(-1);
        for ( ReclamationHDB_ModelRun modelRun: modelRunList ) {
            modelRunIDStrings.add ( "" + modelRun.getModelRunID() );
        }
        Collections.sort(modelRunIDStrings,String.CASE_INSENSITIVE_ORDER);
        StringUtil.removeDuplicates(modelRunIDStrings, true, true);
    }
    catch ( Exception e ) {
        Message.printWarning(3, routine, "Error getting HDB model run list (" + e + ")." );
        modelRunIDStrings = new Vector();
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
    readModelRunListForSelectedModel(rdmi);
    List<String> modelRunNameStrings = new Vector();
    modelRunNameStrings.add ( "" );
    for ( ReclamationHDB_ModelRun modelRun: __modelRunList ) {
        modelRunNameStrings.add ( modelRun.getModelRunName() );
    }
    Collections.sort(modelRunNameStrings,String.CASE_INSENSITIVE_ORDER);
    StringUtil.removeDuplicates(modelRunNameStrings, true, true);
    __ModelRunName_JComboBox.removeAll ();
    __ModelRunName_JComboBox.setData(modelRunNameStrings);
    // Select first choice (may get reset from existing parameter values).
    __ModelRunName_JComboBox.select ( null );
    if ( __ModelRunName_JComboBox.getItemCount() > 0 ) {
        __ModelRunName_JComboBox.select ( 0 );
    }
}

/**
Populate the overwrite flag list based on the selected datastore.
*/
private void populateOverwriteFlagChoices ( ReclamationHDB_DMI rdmi )
{   String routine = getClass().getName() + ".populateOverwriteFlagChoices";
    if ( (rdmi == null) || (__OverwriteFlag_JComboBox == null) ) {
        // Initialization
        return;
    }
    List<String> overwriteFlagStrings = new Vector();
    try {
        List<ReclamationHDB_OverwriteFlag> overwriteFlagList = rdmi.getOverwriteFlagList();
        overwriteFlagStrings.add(""); // No flag specified by parameter
        for ( ReclamationHDB_OverwriteFlag overwriteFlag: overwriteFlagList ) {
            overwriteFlagStrings.add ( overwriteFlag.getOverwriteFlag() + " - " +
                overwriteFlag.getOverwriteFlagName());
        }
        Collections.sort(overwriteFlagStrings,String.CASE_INSENSITIVE_ORDER);
    }
    catch ( Exception e ) {
        Message.printWarning(3, routine, "Error getting HDB overwrite flag list (" + e + ")." );
        overwriteFlagStrings = new Vector();
    }
    __OverwriteFlag_JComboBox.removeAll ();
    __OverwriteFlag_JComboBox.setData(overwriteFlagStrings);
    // Select first choice (may get reset from existing parameter values).
    __OverwriteFlag_JComboBox.select ( null );
    if ( __OverwriteFlag_JComboBox.getItemCount() > 0 ) {
        __OverwriteFlag_JComboBox.select ( 0 );
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
    List<String> siteCommonNameStrings = new Vector();
    try {
        readSiteDataTypeList(rdmi);
        for ( ReclamationHDB_SiteDataType siteDataType: __siteDataTypeList ) {
            siteCommonNameStrings.add ( siteDataType.getSiteCommonName() );
        }
        Collections.sort(siteCommonNameStrings,String.CASE_INSENSITIVE_ORDER);
        StringUtil.removeDuplicates(siteCommonNameStrings, true, true);
    }
    catch ( Exception e ) {
        Message.printWarning(3, routine, "Error getting HDB site data type list (" + e + ")." );
        siteCommonNameStrings = new Vector();
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
    List<String> siteDataTypeIDStrings = new Vector();
    siteDataTypeIDStrings.add ( "" );
    try {
        List<ReclamationHDB_SiteDataType> siteDataTypeList = rdmi.readHdbSiteDataTypeList();
        for ( ReclamationHDB_SiteDataType siteDataType: siteDataTypeList ) {
            siteDataTypeIDStrings.add ( "" + siteDataType.getSiteDataTypeID() );
        }
        Collections.sort(siteDataTypeIDStrings,String.CASE_INSENSITIVE_ORDER);
        StringUtil.removeDuplicates(siteDataTypeIDStrings, true, true);
    }
    catch ( Exception e ) {
        Message.printWarning(3, routine, "Error getting HDB site data type list (" + e + ")." );
        siteDataTypeIDStrings = new Vector();
    }
    __SiteDataTypeID_JComboBox.removeAll ();
    __SiteDataTypeID_JComboBox.setData(siteDataTypeIDStrings);
    // Select first choice (may get reset from existing parameter values).
    __SiteDataTypeID_JComboBox.select ( null );
    if ( __SiteDataTypeID_JComboBox.getItemCount() > 0 ) {
        __SiteDataTypeID_JComboBox.select ( 0 );
    }
}

/**
Populate the time zone choices based on the selected datastore.
*/
private void populateTimeZoneChoices ( ReclamationHDB_DMI rdmi )
{
    __TimeZone_JComboBox.removeAll ();
    List<String> timeZoneChoices = rdmi.getTimeZoneList();
    timeZoneChoices.add(0,"");
    __TimeZone_JComboBox.setData(timeZoneChoices);
    // Select first choice (may get reset from existing parameter values).
    __TimeZone_JComboBox.select ( null );
    if ( __TimeZone_JComboBox.getItemCount() > 0 ) {
        __TimeZone_JComboBox.select ( 0 );
    }
}

/**
Populate the time zone label, which uses the HDB default time zone.
*/
private void populateTimeZoneLabel ( ReclamationHDB_DMI rdmi )
{
    String defaultTZ = __dmi.getDatabaseTimeZone();
    __TimeZone_JLabel.setText("Optional - time zone for instantaneous and daily data (default="+
        defaultTZ + ").");
}

/**
Populate the validation flag list based on the selected datastore.
*/
private void populateValidationFlagChoices ( ReclamationHDB_DMI rdmi )
{   String routine = getClass().getName() + ".populateValidationFlagChoices";
    if ( (rdmi == null) || (__ValidationFlag_JComboBox == null) ) {
        // Initialization
        return;
    }
    List<String> validationFlagStrings = new Vector();
    try {
        List<ReclamationHDB_Validation> validationList = rdmi.getHdbValidationList();
        validationFlagStrings.add(""); // No flag specified by parameter
        String flag;
        for ( ReclamationHDB_Validation validation: validationList ) {
            flag = validation.getValidation();
            if ( (flag.length() > 0) && Character.isLetter(flag.charAt(0)) &&
                Character.isUpperCase(flag.charAt(0))) {
                // Only add uppercase characters
                validationFlagStrings.add ( flag + " - " + validation.getCmmnt() );
            }
        }
        Collections.sort(validationFlagStrings,String.CASE_INSENSITIVE_ORDER);
    }
    catch ( Exception e ) {
        Message.printWarning(3, routine, "Error getting HDB validation flag list (" + e + ")." );
        validationFlagStrings = new Vector();
    }
    __ValidationFlag_JComboBox.removeAll ();
    __ValidationFlag_JComboBox.setData(validationFlagStrings);
    // Select first choice (may get reset from existing parameter values).
    __ValidationFlag_JComboBox.select ( null );
    if ( __ValidationFlag_JComboBox.getItemCount() > 0 ) {
        __ValidationFlag_JComboBox.select ( 0 );
    }
}

/**
Read the model list and set for use in the editor.
*/
private void readModelList ( ReclamationHDB_DMI rdmi )
throws Exception
{
    try {
        List<ReclamationHDB_Model> modelList = rdmi.readHdbModelList();
        setModelList(modelList);
    }
    catch ( Exception e ) {
        setModelList(new Vector());
        throw e;
    }
}

/**
Read the model run list for the selected model.
*/
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
            List<ReclamationHDB_ModelRun> modelRunList = rdmi.readHdbModelRunListForModelID( modelID );
            // The following list matches the model_id and can be used for further filtering
            setModelRunList(modelRunList);
        }
        catch ( Exception e ) {
            Message.printWarning(3, routine, "Error getting HDB model run list (" + e + ")." );
            setModelRunList(new Vector());
        }
    }
    else {
        Message.printStatus ( 2, routine, "Have " + modelList.size() + " models matching name \"" +
            selectedModelName + "\" - unable to find matching model runs." );
    }
}

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
        setSiteDataTypeList(new Vector());
    }
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{	String routine = "WriteReclamationHDB_JDialog.refresh";
    String DataStore = "";
    String TSList = "";
    String TSID = "";
    String EnsembleID = "";
    String SiteCommonName = "";
    String DataTypeCommonName = "";
    String SiteDataTypeID = "";
    String ModelName = "";
    String ModelRunName = "";
    String HydrologicIndicator = "";
    String ModelRunDate = "";
    String ModelRunID = "";
    String Agency = "";
    String ValidationFlag = "";
    String OverwriteFlag = "";
    String TimeZone = "";
    String DataFlags = "";
	String OutputStart = "";
	String OutputEnd = "";
	__error_wait = false;
	PropList parameters = null;
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		parameters = __command.getCommandParameters();
		DataStore = parameters.getValue ( "DataStore" );
        TSList = parameters.getValue ( "TSList" );
        TSID = parameters.getValue ( "TSID" );
        EnsembleID = parameters.getValue ( "EnsembleID" );
        SiteCommonName = parameters.getValue ( "SiteCommonName" );
        DataTypeCommonName = parameters.getValue ( "DataTypeCommonName" );
        SiteDataTypeID = parameters.getValue ( "SiteDataTypeID" );
        ModelName = parameters.getValue ( "ModelName" );
        ModelRunName = parameters.getValue ( "ModelRunName" );
        HydrologicIndicator = parameters.getValue ( "HydrologicIndicator" );
        ModelRunDate = parameters.getValue ( "ModelRunDate" );
        ModelRunID = parameters.getValue ( "ModelRunID" );
        Agency = parameters.getValue ( "Agency" );
        ValidationFlag = parameters.getValue ( "ValidationFlag" );
        OverwriteFlag = parameters.getValue ( "OverwriteFlag" );
        DataFlags = parameters.getValue ( "DataFlags" );
        TimeZone = parameters.getValue ( "TimeZone" );
		OutputStart = parameters.getValue ( "OutputStart" );
		OutputEnd = parameters.getValue ( "OutputEnd" );
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
        if ( TSList == null ) {
            // Select default...
            __TSList_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __TSList_JComboBox,TSList, JGUIUtil.NONE, null, null ) ) {
                __TSList_JComboBox.select ( TSList );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nTSList value \"" + TSList +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( JGUIUtil.isSimpleJComboBoxItem( __TSID_JComboBox, TSID, JGUIUtil.NONE, null, null ) ) {
                __TSID_JComboBox.select ( TSID );
        }
        else {
            // Automatically add to the list after the blank...
            if ( (TSID != null) && (TSID.length() > 0) ) {
                __TSID_JComboBox.insertItemAt ( TSID, 1 );
                // Select...
                __TSID_JComboBox.select ( TSID );
            }
            else {
                // Select the blank...
                __TSID_JComboBox.select ( 0 );
            }
        }
        if ( EnsembleID == null ) {
            // Select default...
            __EnsembleID_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __EnsembleID_JComboBox,EnsembleID, JGUIUtil.NONE, null, null ) ) {
                __EnsembleID_JComboBox.select ( EnsembleID );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nEnsembleID value \"" + EnsembleID +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        // First populate the choices...
        populateSiteCommonNameChoices(getReclamationHDB_DMI() );
        if ( JGUIUtil.isSimpleJComboBoxItem(__SiteCommonName_JComboBox, SiteCommonName, JGUIUtil.NONE, null, null ) ) {
            __SiteCommonName_JComboBox.select ( SiteCommonName );
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
        // First populate the choices...
        populateSiteDataTypeIDChoices(getReclamationHDB_DMI() );
        if ( JGUIUtil.isSimpleJComboBoxItem(__SiteDataTypeID_JComboBox, SiteDataTypeID, JGUIUtil.NONE, null, null ) ) {
            __SiteDataTypeID_JComboBox.select ( SiteDataTypeID );
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
                  "SateDataTypeID parameter \"" + SiteDataTypeID + "\".  Select a different value or Cancel." );
            }
        }
        // First populate the choices...
        populateModelNameChoices(getReclamationHDB_DMI() );
        if ( JGUIUtil.isSimpleJComboBoxItem(__ModelName_JComboBox, ModelName, JGUIUtil.NONE, null, null ) ) {
            __ModelName_JComboBox.select ( ModelName );
            if ( __ignoreEvents ) {
                // Also need to make sure that the __modelRunList is populated
                // Call manually because events are disabled at startup to allow cascade to work properly
                readModelRunListForSelectedModel(__dmi);
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
                        readModelRunListForSelectedModel(__dmi);
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
        populateModelRunIDChoices(getReclamationHDB_DMI() );
        if ( JGUIUtil.isSimpleJComboBoxItem(__ModelRunID_JComboBox, ModelRunID, JGUIUtil.NONE, null, null ) ) {
            __ModelRunID_JComboBox.select ( ModelRunID );
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
                  "SateDataTypeID parameter \"" + ModelRunID + "\".  Select a different value or Cancel." );
            }
        }
        // First populate the choices...
        populateAgencyChoices(getReclamationHDB_DMI() );
        int [] index = new int[1];
        if ( JGUIUtil.isSimpleJComboBoxItem(__Agency_JComboBox, Agency, JGUIUtil.CHECK_SUBSTRINGS,
            " ", 0, index, true ) ) {
            __Agency_JComboBox.select ( index[0] );
        }
        else {
            if ( (Agency == null) || Agency.equals("") ) {
                // New command...select the default...
                if ( __Agency_JComboBox.getItemCount() > 0 ) {
                    __Agency_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "Agency parameter \"" + Agency + "\".  Select a different value or Cancel." );
            }
        }
        // First populate the choices...
        populateValidationFlagChoices(getReclamationHDB_DMI() );
        if ( JGUIUtil.isSimpleJComboBoxItem(__ValidationFlag_JComboBox, ValidationFlag,JGUIUtil.CHECK_SUBSTRINGS,
            " ", 0, index, true )) {
            __ValidationFlag_JComboBox.select ( index[0] );
        }
        else {
            if ( (ValidationFlag == null) || ValidationFlag.equals("") ) {
                // New command...select the default...
                if ( __ValidationFlag_JComboBox.getItemCount() > 0 ) {
                    __ValidationFlag_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "ValidationFlag parameter \"" + ValidationFlag + "\".  Select a different value or Cancel." );
            }
        }
        // First populate the choices...
        populateOverwriteFlagChoices(getReclamationHDB_DMI() );
        if ( JGUIUtil.isSimpleJComboBoxItem(__OverwriteFlag_JComboBox, OverwriteFlag, JGUIUtil.CHECK_SUBSTRINGS,
            " ", 0, index, true ) ) {
            __OverwriteFlag_JComboBox.select ( index[0] );
        }
        else {
            if ( (OverwriteFlag == null) || OverwriteFlag.equals("") ) {
                // New command...select the default...
                if ( __OverwriteFlag_JComboBox.getItemCount() > 0 ) {
                    __OverwriteFlag_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "OverwriteFlag parameter \"" + OverwriteFlag + "\".  Select a different value or Cancel." );
            }
        }
        if ( DataFlags != null ) {
            __DataFlags_JTextField.setText (DataFlags);
        }
        // First populate the choices...
        populateTimeZoneChoices(getReclamationHDB_DMI() );
        populateTimeZoneLabel(getReclamationHDB_DMI() );
        if ( JGUIUtil.isSimpleJComboBoxItem(__TimeZone_JComboBox, TimeZone, JGUIUtil.NONE, null, null ) ) {
            __TimeZone_JComboBox.select ( TimeZone );
        }
        else {
            if ( (TimeZone == null) || TimeZone.equals("") ) {
                // New command...select the default...
                if ( __TimeZone_JComboBox.getItemCount() > 0 ) {
                    __TimeZone_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "TimeZone parameter \"" + TimeZone + "\".  Select a different value or Cancel." );
            }
        }
		if ( OutputStart != null ) {
			__OutputStart_JTextField.setText (OutputStart);
		}
		if ( OutputEnd != null ) {
			__OutputEnd_JTextField.setText (OutputEnd);
		}
	}
	// Regardless, reset the command from the fields...
	DataStore = __DataStore_JComboBox.getSelected();
	if ( DataStore == null ) {
	    DataStore = "";
	}
	else {
	    DataStore = DataStore.trim();
	}
    TSList = __TSList_JComboBox.getSelected();
    TSID = __TSID_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
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
    SiteDataTypeID = __SiteDataTypeID_JComboBox.getSelected();
    if ( SiteDataTypeID == null ) {
        SiteDataTypeID = "";
    }
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
    ModelRunID = __ModelRunID_JComboBox.getSelected();
    if ( ModelRunID == null ) {
        ModelRunID = "";
    }
    Agency = getSelectedAgency();
    ValidationFlag = getSelectedValidationFlag();
    OverwriteFlag = getSelectedOverwriteFlag();
    DataFlags = __DataFlags_JTextField.getText().trim();
    TimeZone = __TimeZone_JComboBox.getSelected();
    if ( TimeZone == null ) {
        TimeZone = "";
    }
	OutputStart = __OutputStart_JTextField.getText().trim();
	OutputEnd = __OutputEnd_JTextField.getText().trim();
	parameters = new PropList ( __command.getCommandName() );
	parameters.add ( "DataStore=" + DataStore );
	parameters.add ( "TSList=" + TSList );
    parameters.add ( "TSID=" + TSID );
    parameters.add ( "EnsembleID=" + EnsembleID );
    parameters.add ( "SiteCommonName=" + SiteCommonName );
    parameters.add ( "DataTypeCommonName=" + DataTypeCommonName );
    parameters.add ( "SiteDataTypeID=" + SiteDataTypeID );
    parameters.add ( "ModelName=" + ModelName );
    parameters.add ( "ModelRunName=" + ModelRunName );
    parameters.add ( "ModelRunDate=" + ModelRunDate );
    parameters.add ( "HydrologicIndicator=" + HydrologicIndicator );
    parameters.add ( "ModelRunID=" + ModelRunID );
    parameters.add ( "Agency=" + Agency );
    parameters.add ( "ValidationFlag=" + ValidationFlag );
    parameters.add ( "OverwriteFlag=" + OverwriteFlag );
    parameters.add ( "DataFlags=" + DataFlags );
    parameters.add ( "TimeZone=" + TimeZone );
	parameters.add ( "OutputStart=" + OutputStart );
	parameters.add ( "OutputEnd=" + OutputEnd );
	__command_JTextArea.setText( __command.toString ( parameters ) );
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
{   __dataStore = getSelectedDataStore();
    __dmi = (ReclamationHDB_DMI)((DatabaseDataStore)__dataStore).getDMI();
}

/**
Set the HDB model list corresponding to the displayed list.
*/
private void setModelList ( List<ReclamationHDB_Model> modelList )
{
    __modelList = modelList;
}

/**
Set the HDB model run list corresponding to the displayed list.
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
        modelRunList = new Vector();
    }
    catch ( Exception e ) {
        // Generally due to startup with bad datastore
        modelRunList = null;
    }
    // TODO SAM 2012-03-25 Need to enable
    //__dmi.findModelRun(__modelRunList,
    //    __ModelName_JComboBox.getSelected(),
    //    __ModelRunName_JComboBox.getSelected(),
    //    __ModelRunDate_JComboBox.getSelected(),
    //    __HydrologicIndicator_JTextField.getText().trim() );
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
        stdList = __dmi.findSiteDataType(__siteDataTypeList,
            __SiteCommonName_JComboBox.getSelected(), null );
    }
    catch ( Exception e ) {
        // Generally at startup with a bad datastore configuration
        stdList = null;
    }
    if ( (stdList == null) || (stdList.size() == 0) ) {
        __selectedSiteID_JLabel.setText ( "No matches" );
    }
    else if ( stdList.size() > 0 ) {
        __selectedSiteID_JLabel.setText ( "" + stdList.get(0).getSiteID() +
            " (" + stdList.size() + " matches)" );
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
        __selectedSiteDataTypeID_JLabel.setText ( "" + stdList.get(0).getSiteDataTypeID() );
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