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
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import riverside.datastore.DataStore;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;

import RTi.DMI.DatabaseDataStore;
import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

/**
Command editor dialog for the WriteReclamationHDB() command.
Very important - this dialog uses HDB data to populate lists and requires that the time series
metadata are already defined.  Consequently, list choices cascade to valid options rather than
letting the user define new combinations.
*/
public class WriteReclamationHDB_JDialog extends JDialog
implements ActionListener, DocumentListener, KeyListener, ItemListener, WindowListener
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
private JTabbedPane __sdi_JTabbedPane = null;
private SimpleJComboBox __SiteCommonName_JComboBox = null;
private SimpleJComboBox __DataTypeCommonName_JComboBox = null;
private JLabel __selectedSiteID_JLabel = null;
private JLabel __selectedSiteDataTypeID_JLabel = null;
private SimpleJComboBox __SiteDataTypeID_JComboBox = null;
private JTabbedPane __model_JTabbedPane = null;
private SimpleJComboBox __ModelName_JComboBox = null;
private SimpleJComboBox __ModelRunName_JComboBox = null;
private SimpleJComboBox __ModelRunDate_JComboBox = null;
private JTextField __NewModelRunDate_JTextField = null;
private SimpleJComboBox __HydrologicIndicator_JComboBox = null;
private JLabel __selectedModelID_JLabel = null;
private JLabel __selectedModelRunID_JLabel = null;
private SimpleJComboBox __ModelRunID_JComboBox = null;
private SimpleJComboBox __EnsembleName_JComboBox = null;
private JTextField __NewEnsembleName_JTextField = null;
private TSFormatSpecifiersJPanel __EnsembleTraceID_JTextField = null;
private SimpleJComboBox __EnsembleModelName_JComboBox = null;
private SimpleJComboBox __EnsembleModelRunDate_JComboBox = null;
private JTextField __NewEnsembleModelRunDate_JTextField = null;
private JLabel __selectedEnsembleID_JLabel = null;
private JLabel __selectedEnsembleModelID_JLabel = null;
private JLabel __selectedEnsembleModelRunID_JLabel = null;
//private SimpleJComboBox __EnsembleModelRunID_JComboBox = null;
private SimpleJComboBox __Agency_JComboBox = null;
private SimpleJComboBox __ValidationFlag_JComboBox = null;
private SimpleJComboBox __OverwriteFlag_JComboBox = null;
private JTextField __DataFlags_JTextField = null;
private SimpleJComboBox __TimeZone_JComboBox = null;
private JLabel __TimeZone_JLabel = null;
private JTextField __OutputStart_JTextField = null;
private JTextField __OutputEnd_JTextField = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
//TODO SAM 2013-04-20 Current thought is irregular data is OK to instantaneous table - remove later
//private SimpleJComboBox __IntervalOverride_JComboBox = null;
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Has user pressed OK to close the dialog?

private boolean __ignoreEvents = false; // Used to ignore cascading events when initializing the components

private ReclamationHDBDataStore __dataStore = null; // selected ReclamationHDBDataStore
private ReclamationHDB_DMI __dmi = null; // ReclamationHDB_DMI to do queries.

private List<ReclamationHDB_Ensemble> __ensembleList = new ArrayList<ReclamationHDB_Ensemble>(); // Corresponds to displayed list (has ensemble_id)
private List<ReclamationHDB_SiteDataType> __siteDataTypeList = new ArrayList<ReclamationHDB_SiteDataType>(); // Corresponds to displayed list
private List<ReclamationHDB_Model> __modelList = new ArrayList<ReclamationHDB_Model>(); // Corresponds to displayed list (has model_id)
private List<ReclamationHDB_ModelRun> __modelRunList = new ArrayList<ReclamationHDB_ModelRun>(); // Corresponds to models matching model_id

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
@param e ActionEvent object
*/
public void actionPerformed( ActionEvent e )
{	if ( __ignoreEvents ) {
        return; // Startup.
    }
    Object o = e.getSource();
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
    populateEnsembleNameChoices ( __dmi );
    populateEnsembleModelNameChoices ( __dmi );
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
    // are familiar with the database internals.  Also select the SiteDataTypeID since that is the new convention.
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
    // Now populate the model name name choices corresponding to the ensemble name, which will cascade to
    // populating the other choices
    populateEnsembleModelNameChoices ( __dmi );
}

/**
Refresh the query choices for the currently selected ReclamationHDB model name.
*/
private void actionPerformedEnsembleModelNameSelected ( )
{
    if ( __EnsembleModelName_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    // No further action needed to populate choices but show selected model_id those who
    // are familiar with the database internals
    updateEnsembleIDTextFields ();
    // Now populate the ensemble model run date choices corresponding to the model name, which will cascade to
    // populating the other choices
    // This is not a selectable item with ensembles - just key off of model run name
    //populateEnsembleModelRunDateChoices ( __dmi );
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
    //updateModelIDTextFields ();
    populateModelRunDateChoices ( __dmi );
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

//Start event handlers for DocumentListener...

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
    String NewModelRunDate = __NewModelRunDate_JTextField.getText().trim();
    String ModelRunID = __ModelRunID_JComboBox.getSelected();
    String EnsembleName = getSelectedEnsembleName();
    String NewEnsembleName = __NewEnsembleName_JTextField.getText().trim();
    String EnsembleTraceID = __EnsembleTraceID_JTextField.getText().trim();
    String EnsembleModelName = __EnsembleModelName_JComboBox.getSelected();
    String EnsembleModelRunDate = __EnsembleModelRunDate_JComboBox.getSelected();
    String NewEnsembleModelRunDate = __NewEnsembleModelRunDate_JTextField.getText().trim();
    String Agency = getSelectedAgency();
    String ValidationFlag = getSelectedValidationFlag();
    String OverwriteFlag = getSelectedOverwriteFlag();
    String DataFlags = __DataFlags_JTextField.getText().trim();
    String TimeZone = __TimeZone_JComboBox.getSelected();
	String OutputStart = __OutputStart_JTextField.getText().trim();
	String OutputEnd = __OutputEnd_JTextField.getText().trim();
	// TODO SAM 2013-04-20 Current thought is irregular data is OK to instantaneous table - remove later
	//String IntervalOverride = __IntervalOverride_JComboBox.getSelected();

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
    if ( (NewModelRunDate != null) && (NewModelRunDate.length() > 0) ) {
        parameters.set ( "NewModelRunDate", NewModelRunDate );
    }
    if ( HydrologicIndicator.length() > 0 ) {
        parameters.set ( "HydrologicIndicator", HydrologicIndicator );
    }
    if ( (ModelRunID != null) && (ModelRunID.length() > 0) ) {
        parameters.set ( "ModelRunID", ModelRunID );
    }
    if ( (EnsembleName != null) && (EnsembleName.length() > 0) ) {
        parameters.set ( "EnsembleName", EnsembleName );
    }
    if ( (NewEnsembleName != null) && (NewEnsembleName.length() > 0) ) {
        parameters.set ( "NewEnsembleName", NewEnsembleName );
    }
    if ( EnsembleTraceID.length() > 0 ) {
        parameters.set ( "EnsembleTraceID", EnsembleTraceID );
    }
    if ( (EnsembleModelName != null) && (EnsembleModelName.length() > 0) ) {
        parameters.set ( "EnsembleModelName", EnsembleModelName );
    }
    if ( (EnsembleModelRunDate != null) && (EnsembleModelRunDate.length() > 0) ) {
        parameters.set ( "EnsembleModelRunDate", EnsembleModelRunDate );
    }
    if ( (NewEnsembleModelRunDate != null) && (NewEnsembleModelRunDate.length() > 0) ) {
        parameters.set ( "NewEnsembleModelRunDate", NewEnsembleModelRunDate );
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
	// TODO SAM 2013-04-20 Current thought is irregular data is OK to instantaneous table - remove later
    //if ( IntervalOverride.length() > 0 ) {
    //    parameters.set ( "IntervalOverride", IntervalOverride );
    //}
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
    String NewModelRunDate = __NewModelRunDate_JTextField.getText().trim();
    String HydrologicIndicator = __HydrologicIndicator_JComboBox.getSelected();
    String ModelRunID = __ModelRunID_JComboBox.getSelected();
    String EnsembleName = getSelectedEnsembleName();
    String NewEnsembleName = __NewEnsembleName_JTextField.getText().trim();
    String EnsembleTraceID = __EnsembleTraceID_JTextField.getText().trim();
    String EnsembleModelName = __EnsembleModelName_JComboBox.getSelected();
    String EnsembleModelRunDate = __EnsembleModelRunDate_JComboBox.getSelected();
    String NewEnsembleModelRunDate = __NewEnsembleModelRunDate_JTextField.getText().trim();
    String Agency = getSelectedAgency();
    String ValidationFlag = getSelectedValidationFlag();
    String OverwriteFlag = getSelectedOverwriteFlag();
    String DataFlags = __DataFlags_JTextField.getText().trim();
    String TimeZone = __TimeZone_JComboBox.getSelected();
	String OutputStart = __OutputStart_JTextField.getText().trim();
	String OutputEnd = __OutputEnd_JTextField.getText().trim();
	//String IntervalOverride = __IntervalOverride_JComboBox.getSelected();
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
    __command.setCommandParameter ( "NewModelRunDate", NewModelRunDate );
    __command.setCommandParameter ( "HydrologicIndicator", HydrologicIndicator );
    __command.setCommandParameter ( "ModelRunID", ModelRunID );
    __command.setCommandParameter ( "EnsembleName", EnsembleName );
    __command.setCommandParameter ( "NewEnsembleName", NewEnsembleName );
    __command.setCommandParameter ( "EnsembleTraceID", EnsembleTraceID );
    __command.setCommandParameter ( "EnsembleModelName", EnsembleModelName );
    __command.setCommandParameter ( "EnsembleModelRunDate", EnsembleModelRunDate );
    __command.setCommandParameter ( "NewEnsembleModelRunDate", NewEnsembleModelRunDate );
    __command.setCommandParameter ( "Agency", Agency );
    __command.setCommandParameter ( "ValidationFlag", ValidationFlag );
    __command.setCommandParameter ( "OverwriteFlag", OverwriteFlag );
    __command.setCommandParameter ( "DataFlags", DataFlags );
    __command.setCommandParameter ( "TimeZone", TimeZone );
	__command.setCommandParameter ( "OutputStart", OutputStart );
	__command.setCommandParameter ( "OutputEnd", OutputEnd );
	//__command.setCommandParameter ( "IntervalOverride", IntervalOverride );
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
Return the selected ensemble name, which can be from the choice or user-supplied.
*/
private String getSelectedEnsembleName()
{   String EnsembleName = __EnsembleName_JComboBox.getSelected();
    Message.printStatus(2, "", "EnsembleName from choice is \"" + EnsembleName + "\"" );
    if ( (EnsembleName == null) || EnsembleName.equals("") ) {
        // See if user has specified by typing in the box.
        String text = __EnsembleName_JComboBox.getFieldText().trim();
        Message.printStatus(2, "", "EnsembleName from text is \"" + EnsembleName + "\"" );
        if ( !text.equals("") ) {
            return text;
        }
    }
    return "";
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
		"Write a single \"real\" or model time series, or write an ensemble of model time series to a Reclamation HDB database." ),
		0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The HDB time series table is determined from the data interval, with irregular data being written to the " +
        "instantaneous data table." ),
        0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "TSTool will only write time series records and will not write records for " +
        "time series metadata (site, data type, and model data must have been previously defined)."),
        0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Specify output date/times to a " +
		"precision appropriate for output time series."),
		0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    __ignoreEvents = true; // So that a full pass of initialization can occur
    
    // List available datastores of the correct type
    // Other lists are NOT populated until a datastore is selected (driven by events)
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Datastore:"),
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStore_JComboBox = new SimpleJComboBox ( false );
    List<DataStore> dataStoreList = ((TSCommandProcessor)processor).getDataStoresByType( ReclamationHDBDataStore.class );
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
    
    __sdi_JTabbedPane = new JTabbedPane ();
    __sdi_JTabbedPane.setBorder(
        BorderFactory.createTitledBorder ( BorderFactory.createLineBorder(Color.black),
        "Specify how to match the HDB site_datatype_id (required for all time series and ensembles)" ));
    JGUIUtil.addComponent(main_JPanel, __sdi_JTabbedPane,
        0, ++yMain, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // Panel to select site_datatype_id directly
    int ysdi = -1;
    JPanel sdi_JPanel = new JPanel();
    sdi_JPanel.setLayout( new GridBagLayout() );
    __sdi_JTabbedPane.addTab ( "Select site_datatype_id (SDI)", sdi_JPanel );
    
    JGUIUtil.addComponent(sdi_JPanel, new JLabel (
        "The choices below include: \"site_datatype_id - site name - datatype name\", sorted by site name."), 
        0, ++ysdi, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(sdi_JPanel, new JLabel ("Site data type ID:"), 
        0, ++ysdi, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SiteDataTypeID_JComboBox = new SimpleJComboBox (false);
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
    
    JGUIUtil.addComponent(siteCommon_JPanel, new JLabel ("Data type common name:"), 
        0, ++ySiteCommonName, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataTypeCommonName_JComboBox = new SimpleJComboBox (false);
    __DataTypeCommonName_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(siteCommon_JPanel, __DataTypeCommonName_JComboBox,
        1, ySiteCommonName, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(siteCommon_JPanel, new JLabel (
        "Required - used with site common name to determine site_datatype_id."),
        3, ySiteCommonName, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(siteCommon_JPanel, new JLabel ("Matching site_id:"), 
        0, ++ySiteCommonName, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __selectedSiteID_JLabel = new JLabel ( "");
    JGUIUtil.addComponent(siteCommon_JPanel, __selectedSiteID_JLabel,
        1, ySiteCommonName, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(siteCommon_JPanel, new JLabel (
        "Information - useful when comparing to database contents."),
        3, ySiteCommonName, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(siteCommon_JPanel, new JLabel ("Matching site_datatype_id:"), 
        0, ++ySiteCommonName, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __selectedSiteDataTypeID_JLabel = new JLabel ( "");
    JGUIUtil.addComponent(siteCommon_JPanel, __selectedSiteDataTypeID_JLabel,
        1, ySiteCommonName, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(siteCommon_JPanel, new JLabel (
        "Information - useful when comparing to database contents."),
        3, ySiteCommonName, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    __model_JTabbedPane = new JTabbedPane ();
    __model_JTabbedPane.setBorder(
        BorderFactory.createTitledBorder ( BorderFactory.createLineBorder(Color.black),
        "Specify how to match the HDB model_run_id (leave blank if not writing single or ensemble model time series data)" ));
    JGUIUtil.addComponent(main_JPanel, __model_JTabbedPane,
        0, ++yMain, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // Panel to control model selection for single time series
    int yModel = -1;
    JPanel model_JPanel = new JPanel();
    model_JPanel.setLayout( new GridBagLayout() );
    __model_JTabbedPane.addTab ( "Single model time series", model_JPanel );
    
    JGUIUtil.addComponent(model_JPanel, new JLabel (
        "Use these parameters to write a single model time series to HDB.  The model_run_id choices show model_run_id " +
        "- model run name - hydrologic indicator - model run date"), 
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
    
    JGUIUtil.addComponent(model_JPanel, new JLabel ("OR new  model run date:"), 
        0, ++yModel, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NewModelRunDate_JTextField = new JTextField (20);
    __NewModelRunDate_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(model_JPanel, __NewModelRunDate_JTextField,
        1, yModel, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(model_JPanel, new JLabel (
        "Optional - specify if new model run date is being defined (default=specify existing)."),
        3, yModel, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(model_JPanel, new JLabel ("Model run ID (model_run_id):"), 
        0, ++yModel, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ModelRunID_JComboBox = new SimpleJComboBox (false);
    __ModelRunID_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(model_JPanel, __ModelRunID_JComboBox,
        1, yModel, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(model_JPanel, new JLabel (
        "Optional - alternative to selecting above choices."),
        3, yModel, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Panel to control model selection for an ensemble
    int yEnsemble = -1;
    JPanel ensemble_JPanel = new JPanel();
    ensemble_JPanel.setLayout( new GridBagLayout() );
    __model_JTabbedPane.addTab ( "Ensemble of model time series", ensemble_JPanel );

    JGUIUtil.addComponent(ensemble_JPanel, new JLabel (
        "Use these parameters to write an ensemble of model time series to an existing or new HDB ensemble.  " +
        "The model name must exist but a new run date can be specified."), 
        0, ++yEnsemble, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel (
        "The trace number for each time series in the ensemble will be determined from ensemble traces at run time."), 
        0, ++yEnsemble, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel (
        "If the run date is specified, the ensemble time series will be uniquely identified with the " +
        "run date (to the minute)."), 
        0, ++yEnsemble, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel ("Ensemble name:"), 
        0, ++yEnsemble, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __EnsembleName_JComboBox = new SimpleJComboBox (false); // New value is specified with separate text field for clarity
    __EnsembleName_JComboBox.addItemListener (this);
    __EnsembleName_JComboBox.addKeyListener (this);
    JGUIUtil.addComponent(ensemble_JPanel, __EnsembleName_JComboBox,
        1, yEnsemble, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel (
        "Required - used to determine the ensemble model_run_id."),
        3, yEnsemble, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel ("OR new ensemble name:"), 
        0, ++yEnsemble, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NewEnsembleName_JTextField = new JTextField (20);
    __NewEnsembleName_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(ensemble_JPanel, __NewEnsembleName_JTextField,
        1, yEnsemble, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel (
        "Optional - specify if new ensemble is being defined (default=specify existing)."),
        3, yEnsemble, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
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
    
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel("Ensemble trace ID:"),
        0, ++yEnsemble, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __EnsembleTraceID_JTextField = new TSFormatSpecifiersJPanel(10);
    __EnsembleTraceID_JTextField.setToolTipText("Use %L for location, %T for data type, %I for interval.");
    __EnsembleTraceID_JTextField.addKeyListener ( this );
    __EnsembleTraceID_JTextField.getDocument().addDocumentListener(this);
    __EnsembleTraceID_JTextField.setToolTipText("%L for location, %T for data type, ${TS:property} to use property.");
    JGUIUtil.addComponent(ensemble_JPanel, __EnsembleTraceID_JTextField,
        1, yEnsemble, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel (
        "Optional - use %z for sequence (trace) ID, etc. or ${TS:property} (default=sequence ID)."),
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
        "Optional - YYYY-MM-DD hh:mm, used to determine the ensemble model_run_id (default=run date not used)."),
        3, yEnsemble, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel ("OR new ensemble model run date:"), 
        0, ++yEnsemble, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NewEnsembleModelRunDate_JTextField = new JTextField (20);
    __NewEnsembleModelRunDate_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(ensemble_JPanel, __NewEnsembleModelRunDate_JTextField,
        1, yEnsemble, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel (
        "Optional - specify if new model run date is being defined (default=specify existing)."),
        3, yEnsemble, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel ("Selected ensemble_id:"), 
        0, ++yEnsemble, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __selectedEnsembleID_JLabel = new JLabel ( "");
    JGUIUtil.addComponent(ensemble_JPanel, __selectedEnsembleID_JLabel,
        1, yEnsemble, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel (
        "Information - useful when comparing to database contents."),
        3, yEnsemble, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel ("Selected ensemble model_id:"), 
        0, ++yEnsemble, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __selectedEnsembleModelID_JLabel = new JLabel ( "");
    JGUIUtil.addComponent(ensemble_JPanel, __selectedEnsembleModelID_JLabel,
        1, yEnsemble, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel (
        "Information - useful when comparing to database contents."),
        3, yEnsemble, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel ("Selected ensemble model_run_id:"), 
        0, ++yEnsemble, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __selectedEnsembleModelRunID_JLabel = new JLabel ( "Determined when command is run");
    JGUIUtil.addComponent(ensemble_JPanel, __selectedEnsembleModelRunID_JLabel,
        1, yEnsemble, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    //JGUIUtil.addComponent(ensemble_JPanel, new JLabel (
    //    ""),
    //    3, yEnsemble, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Additional general write parameters...
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
    
    // TODO SAM 2013-04-20 Current thought is irregular data is OK to instantaneous table - remove later
    /*
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Interval override:"),
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __IntervalOverride_JComboBox = new SimpleJComboBox ( false );
    List<String> overrideChoices =
        TimeInterval.getTimeIntervalChoices(TimeInterval.HOUR, TimeInterval.HOUR,false,1);
    overrideChoices.add(0,"");
    __IntervalOverride_JComboBox.setData ( overrideChoices );
    // Select a default...
    __IntervalOverride_JComboBox.select ( 0 );
    __IntervalOverride_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __IntervalOverride_JComboBox,
        1, yMain, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - for irregular interval, treat as hourly instead of instantaneous when writing."),
        3, yMain, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        */

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
    
	setResizable ( true ); // TODO SAM 2010-12-10 Resizing causes some problems
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
    else if ( (source == __EnsembleName_JComboBox) && (sc == ItemEvent.SELECTED) ) {
        // User has selected an ensemble name.
        actionPerformedEnsembleNameSelected ();
    }
    else if ( (source == __EnsembleModelName_JComboBox) && (sc == ItemEvent.SELECTED) ) {
        // User has selected an ensemble model name.
        actionPerformedEnsembleModelNameSelected ();
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
Populate the ensemble model name list based on the selected datastore.
The model names are the same as the non-ensemble list so reuse what was read from HDB.
*/
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

/**
Populate the model name list based on the selected datastore.
*/
private void populateEnsembleNameChoices ( ReclamationHDB_DMI rdmi )
{   String routine = getClass().getName() + ".populateEnsembleNameChoices";
    if ( (rdmi == null) || (__EnsembleName_JComboBox == null) ) {
        // Initialization
        return;
    }
    List<String> ensembleNameStrings = new Vector();
    ensembleNameStrings.add ( "" ); // Always add blank because user may not want ensemble time series
    ensembleNameStrings.add ( "HardcodedTest" ); // Add so something is in the list, FIXME SAM 2013-03-23 remove when code tested
    try {
        readEnsembleList(rdmi);
        for ( ReclamationHDB_Ensemble ensemble: __ensembleList ) {
            ensembleNameStrings.add ( ensemble.getEnsembleName() );
        }
        Collections.sort(ensembleNameStrings,String.CASE_INSENSITIVE_ORDER);
        StringUtil.removeDuplicates(ensembleNameStrings, true, true);
    }
    catch ( Exception e ) {
        Message.printWarning(3, routine, "Error getting HDB ensemble list (" + e + ")." );
        ensembleNameStrings = new Vector();
    }
    __EnsembleName_JComboBox.removeAll ();
    __EnsembleName_JComboBox.setData(ensembleNameStrings);
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
{   String routine = getClass().getName() + ".populateHydrologicIndicatorChoices";
    if ( (rdmi == null) || (__HydrologicIndicator_JComboBox == null) ) {
        // Initialization
        return;
    }
    String selectedModelName = __ModelName_JComboBox.getSelected();
    String selectedModelRunName = __ModelRunName_JComboBox.getSelected();
    //String selectedModelRunDate = __ModelRunDate_JComboBox.getSelected();
    List<ReclamationHDB_Model> modelList = rdmi.findModel(__modelList, selectedModelName);
    List<String> hydrologicIndicatorStrings = new Vector();
    hydrologicIndicatorStrings.add ( "" ); // Always add blank because user may not want model time series
    if ( modelList.size() == 1 ) {
        ReclamationHDB_Model model = modelList.get(0);
        int modelID = model.getModelID();
        List<ReclamationHDB_ModelRun> modelRunList = rdmi.findModelRun(__modelRunList, modelID,
                selectedModelRunName,
                null,
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
    String hydrologicIndicator = __HydrologicIndicator_JComboBox.getSelected();
    if ( hydrologicIndicator == null ) {
        hydrologicIndicator = "";
    }
    List<ReclamationHDB_Model> modelList = rdmi.findModel(__modelList, selectedModelName);
    List<String> runDateStrings = new ArrayList<String>();
    runDateStrings.add ( "" ); // Always add blank because user may not want model time series
    if ( modelList.size() == 1 ) {
        ReclamationHDB_Model model = modelList.get(0);
        int modelID = model.getModelID();
        List<ReclamationHDB_ModelRun> modelRunList = rdmi.findModelRun(__modelRunList, modelID,
            selectedModelRunName,
            null, // Don't match on run date
            hydrologicIndicator);
        // Results should list unique hydrologic indicators
        // Model run date is formatted to minute
        Date d;
        DateTime dt;
        for ( ReclamationHDB_ModelRun modelRun: modelRunList ) {
            d = modelRun.getRunDate();
            dt = new DateTime(d);
            // Shows seconds and hundredths...
            //runDateStrings.add ( "" + modelRun.getRunDate() );
            runDateStrings.add ( "" + dt.toString(DateTime.FORMAT_YYYY_MM_DD_HH_mm) );
        }
        Collections.sort(runDateStrings,String.CASE_INSENSITIVE_ORDER);
        // There should not be duplicates
        //StringUtil.removeDuplicates(runDateStrings, true, true);
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
    List<String> modelRunIDStrings = new ArrayList<String>();
    List<String> sortStrings = new ArrayList<String>();
    modelRunIDStrings.add ( "" ); // Always add blank because user may not want model time series
    sortStrings.add("");
    try {
        // There may be no run names for the model id.
        List<ReclamationHDB_ModelRun> modelRunList = rdmi.readHdbModelRunListForModelID(-1);
        String hydrologicIndicator;
        for ( ReclamationHDB_ModelRun modelRun: modelRunList ) {
            hydrologicIndicator = modelRun.getHydrologicIndicator();
            if ( hydrologicIndicator.equals("") ) {
                hydrologicIndicator = "no hydrologic indicator";
            }
            modelRunIDStrings.add ( "" + modelRun.getModelRunID() + " - " + modelRun.getModelRunName() + " - " +
                hydrologicIndicator + " - " + modelRun.getRunDate().toString().replace(":00.0","") );
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
    List<String> modelRunNameStrings = new ArrayList<String>();
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
    List<String> siteDataTypeIDStrings = new ArrayList<String>();
    List<String> sortStrings = new ArrayList<String>();
    siteDataTypeIDStrings.add ( "" );
    sortStrings.add("");
    ReclamationHDB_DataType dt;
    ReclamationHDB_Site site;
    String dtString, siteString;
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
                siteString = "site name unknown";
            }
            else {
                siteString = site.getSiteName().trim();
            }
            siteDataTypeIDStrings.add ( "" + siteDataType.getSiteDataTypeID() + " - " + siteString + " - " + dtString );
            sortStrings.add ( siteString + " - " + dtString );
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
    __TimeZone_JLabel.setText("Optional - time zone for instantaneous and hourly data (default="+
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
Read the ensemble list and set for use in the editor.
*/
private void readEnsembleList ( ReclamationHDB_DMI rdmi )
throws Exception
{
    try {
        List<ReclamationHDB_Ensemble> modelList = rdmi.readRefEnsembleList();
        setEnsembleList(modelList);
    }
    catch ( Exception e ) {
        setEnsembleList(new Vector<ReclamationHDB_Ensemble>());
        throw e;
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
        setModelList(new Vector<ReclamationHDB_Model>());
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
            setModelRunList(new Vector<ReclamationHDB_ModelRun>());
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
        setSiteDataTypeList(new Vector<ReclamationHDB_SiteDataType>());
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
    String NewModelRunDate = "";
    String ModelRunID = "";
    String EnsembleName = "";
    String NewEnsembleName = "";
    String EnsembleTraceID = "";
    String EnsembleModelName = "";
    String EnsembleModelRunDate = "";
    String NewEnsembleModelRunDate = "";
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
        NewModelRunDate = parameters.getValue ( "NewModelRunDate" );
        ModelRunID = parameters.getValue ( "ModelRunID" );
        EnsembleName = parameters.getValue ( "EnsembleName" );
        NewEnsembleName = parameters.getValue ( "NewEnsembleName" );
        EnsembleTraceID = parameters.getValue ( "EnsembleTraceID" );
        EnsembleModelName = parameters.getValue ( "EnsembleModelName" );
        EnsembleModelRunDate = parameters.getValue ( "EnsembleModelRunDate" );
        NewEnsembleModelRunDate = parameters.getValue ( "NewEnsembleModelRunDate" );
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
                // The model list is also used for ensembles
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
                    // This is also used to populate the ensemble list
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
                  "SateDataTypeID parameter \"" + SiteDataTypeID + "\".  Select a different value or Cancel." );
            }
        }
        // First populate the choices...
        populateSiteCommonNameChoices(getReclamationHDB_DMI() );
        if ( JGUIUtil.isSimpleJComboBoxItem(__SiteCommonName_JComboBox, SiteCommonName, JGUIUtil.NONE, null, null ) ) {
            __SiteCommonName_JComboBox.select ( SiteCommonName );
            if ( (SiteDataTypeID != null) && !SiteDataTypeID.equals("") ) {
                __sdi_JTabbedPane.setSelectedIndex(1);
            }
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
        populateModelNameChoices(getReclamationHDB_DMI() );
        if ( JGUIUtil.isSimpleJComboBoxItem(__ModelName_JComboBox, ModelName, JGUIUtil.NONE, null, null ) ) {
            __ModelName_JComboBox.select ( ModelName );
            __model_JTabbedPane.setSelectedIndex(0);
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
        if ( NewModelRunDate != null ) {
            __NewModelRunDate_JTextField.setText ( NewModelRunDate );
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
        populateEnsembleNameChoices(getReclamationHDB_DMI() );
        if ( JGUIUtil.isSimpleJComboBoxItem(__EnsembleName_JComboBox, EnsembleName, JGUIUtil.NONE, null, null ) ) {
            __EnsembleName_JComboBox.select ( EnsembleName );
            __model_JTabbedPane.setSelectedIndex(1);
            if ( __ignoreEvents ) {
                // Also need to make sure that the __modelRunList is populated
                // Call manually because events are disabled at startup to allow cascade to work properly
                // TODO SAM 2013-04-07 Don't need to read because selecting datastore reads
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
                        // TODO SAM 2013-04-07 Don't need to read because selecting datastore reads
                        //readModelRunListForSelectedModel(__dmi);
                    }
                }
            }
            else {
                // User supplied and not in the database so add as a choice...
                __EnsembleName_JComboBox.add ( EnsembleName );
                __EnsembleName_JComboBox.select(__EnsembleName_JComboBox.getItemCount() - 1);
            }
        }
        if ( NewEnsembleName != null ) {
            __NewEnsembleName_JTextField.setText ( NewEnsembleName );
        }
        if ( EnsembleTraceID != null ) {
            __EnsembleTraceID_JTextField.setText ( EnsembleTraceID );
        }
        /* TODO SAM 2013-09-24 Is this needed?
        // First populate the choices...
        populateEnsembleModelRunDateChoices(getReclamationHDB_DMI() );
        if ( JGUIUtil.isSimpleJComboBoxItem(__EnsembleModelRunDate_JComboBox, EnsembleModelRunDate, JGUIUtil.NONE, null, null ) ) {
            __EnsembleModelRunDate_JComboBox.select ( EnsembleModelRunDate );
        }
        else {
            if ( (EnsembleModelRunDate == null) || EnsembleModelRunDate.equals("") ) {
                // New command...select the default...
                if ( __EnsembleModelRunDate_JComboBox.getItemCount() > 0 ) {
                    __EnsembleModelRunDate_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "EnsembleModelRunDate parameter \"" + EnsembleModelRunDate + "\".  Select a different value or Cancel." );
            }
        }
        */
        // First populate the choices...
        populateAgencyChoices(getReclamationHDB_DMI() );
        if ( JGUIUtil.isSimpleJComboBoxItem(__Agency_JComboBox, Agency, JGUIUtil.CHECK_SUBSTRINGS, " ", 0, index, true ) ) {
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
		// TODO SAM 2013-04-20 Current thought is irregular data is OK to instantaneous table - remove later
		/*
        if ( JGUIUtil.isSimpleJComboBoxItem(__IntervalOverride_JComboBox, IntervalOverride, JGUIUtil.NONE, null, null ) ) {
            __IntervalOverride_JComboBox.select ( IntervalOverride );
        }
        else {
            if ( (IntervalOverride == null) || IntervalOverride.equals("") ) {
                // New command...select the default...
                if ( __IntervalOverride_JComboBox.getItemCount() > 0 ) {
                    __IntervalOverride_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "IntervalOverride parameter \"" + IntervalOverride + "\".  Select a different value or Cancel." );
            }
        }
        */
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
    NewModelRunDate = __NewModelRunDate_JTextField.getText().trim();
    HydrologicIndicator = __HydrologicIndicator_JComboBox.getSelected();
    if ( HydrologicIndicator == null ) {
        HydrologicIndicator = "";
    }
    ModelRunID = __ModelRunID_JComboBox.getSelected();
    if ( ModelRunID == null ) {
        ModelRunID = "";
    }
    EnsembleName = getSelectedEnsembleName();
    NewEnsembleName = __NewEnsembleName_JTextField.getText().trim();
    EnsembleTraceID = __EnsembleTraceID_JTextField.getText().trim();
    EnsembleModelName = __EnsembleModelName_JComboBox.getSelected();
    if ( EnsembleModelName == null ) {
        EnsembleModelName = "";
    }
    EnsembleModelRunDate = __EnsembleModelRunDate_JComboBox.getSelected();
    if ( EnsembleModelRunDate == null ) {
        EnsembleModelRunDate = "";
    }
    NewEnsembleModelRunDate = __NewEnsembleModelRunDate_JTextField.getText().trim();
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
	// TODO SAM 2013-04-20 Current thought is irregular data is OK to instantaneous table - remove later
	/*
    IntervalOverride = __IntervalOverride_JComboBox.getSelected();
    if ( IntervalOverride == null ) {
        IntervalOverride = "";
    }
    */
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
    parameters.add ( "NewModelRunDate=" + NewModelRunDate );
    parameters.add ( "ModelRunDate=" + ModelRunDate );
    parameters.add ( "HydrologicIndicator=" + HydrologicIndicator );
    parameters.add ( "ModelRunID=" + ModelRunID );
    parameters.add ( "EnsembleName=" + EnsembleName );
    parameters.add ( "NewEnsembleName=" + NewEnsembleName );
    parameters.add ( "EnsembleTraceID=" + EnsembleTraceID );
    parameters.add ( "EnsembleModelName=" + EnsembleModelName );
    parameters.add ( "EnsembleModelRunDate=" + EnsembleModelRunDate );
    parameters.add ( "NewEnsembleModelRunDate=" + NewEnsembleModelRunDate );
    parameters.add ( "Agency=" + Agency );
    parameters.add ( "ValidationFlag=" + ValidationFlag );
    parameters.add ( "OverwriteFlag=" + OverwriteFlag );
    parameters.add ( "DataFlags=" + DataFlags );
    parameters.add ( "TimeZone=" + TimeZone );
	parameters.add ( "OutputStart=" + OutputStart );
	parameters.add ( "OutputEnd=" + OutputEnd );
	// TODO SAM 2013-04-20 Current thought is irregular data is OK to instantaneous table - remove later
	//parameters.add ( "IntervalOverride=" + IntervalOverride );
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
Set the HDB ensemble list corresponding to the displayed list.
*/
private void setEnsembleList ( List<ReclamationHDB_Ensemble> ensembleList )
{
    __ensembleList = ensembleList;
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
Update the ensemble information text fields.
*/
private void updateEnsembleIDTextFields ()
{   // Ensemble information...
    List<ReclamationHDB_Ensemble> ensembleList = null;
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
        // Select the item in the SiteDataTypeID choice
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