package rti.tscommandprocessor.commands.riversidedb;

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

import java.util.Collections;
import java.util.List;
import java.util.Vector;

import riverside.datastore.DataStore;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;

import RTi.DMI.DMIUtil;
import RTi.DMI.DatabaseDataStore;
import RTi.DMI.RiversideDB_DMI.RiversideDBDataStore;
import RTi.DMI.RiversideDB_DMI.RiversideDB_DMI;
import RTi.DMI.RiversideDB_DMI.RiversideDB_DataType;
import RTi.DMI.RiversideDB_DMI.RiversideDB_MeasLoc;
import RTi.DMI.RiversideDB_DMI.RiversideDB_MeasType;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;

/**
Command editor dialog for the WriteRiversideDB() command.
Very important - this dialog uses RiversideDB data to populate lists and requires that the time series
metadata are already defined.  Consequently, list choices cascade to valid options rather than
letting the user define new combinations.
*/
public class WriteRiversideDB_JDialog extends JDialog
implements ActionListener, KeyListener, ItemListener, WindowListener
{

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJComboBox __DataStore_JComboBox = null;
private SimpleJComboBox __TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox __TSID_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private WriteRiversideDB_Command __command = null;
private JTextArea __command_JTextArea=null;
private SimpleJComboBox __LocationID_JComboBox = null;
private SimpleJComboBox __DataSource_JComboBox = null;
private SimpleJComboBox __DataType_JComboBox = null;
private SimpleJComboBox __DataSubType_JComboBox = null;
private SimpleJComboBox __Interval_JComboBox = null;
private SimpleJComboBox __Scenario_JComboBox = null;
private SimpleJComboBox __SequenceNumber_JComboBox = null;
private JLabel __selectedMeasLocNum_JLabel = null;
private JLabel __selectedMeasTypeNum_JLabel = null;
private SimpleJComboBox __WriteDataFlags_JComboBox = null;
private JTextField __OutputStart_JTextField = null;
private JTextField __OutputEnd_JTextField = null;
private RiversideDBDataStore __dataStore = null; // selected RiversideDB_DataStore
private RiversideDB_DMI __dmi = null; // RiversideDB_DMI to do queries.
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Has user pressed OK to close the dialog?
private boolean __ignoreEvents = false; // Used to ignore cascading events when working with choices

private JTabbedPane __tsInfo_JTabbedPane = null;

// Populated in readMeasTypeListForDataType and corresponds to displayed list
// The list is used for "findMeasType" calls to filter the list of MeasType based on parameter values
// This is cleaner in some cases than making repeated calls to the database
// TODO SAM 2012-04-03 One issue is that if the list of MeasType for a data type is huge, it will
// temporarily eat up memory
private List<RiversideDB_MeasType> __measTypeList = new Vector();

// List in the order of the UI (NOT the TSID!)
private enum Parameter {
    DATA_TYPE, DATA_SUBTYPE, INTERVAL, LOCATION, DATA_SOURCE, SCENARIO, SEQUENCE_NUMBER
};

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public WriteRiversideDB_JDialog ( JFrame parent, WriteRiversideDB_Command command )
{	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{   if ( __ignoreEvents ) {
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
Refresh the query choices for the currently selected RiversideDB data source.
*/
private void actionPerformedDataSourceSelected ( )
{
    if ( __DataSource_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    // Now populate the scenario choices corresponding to the data type, data sub-type, interval, location,
    // and data source
    populateScenarioChoices ( __dmi );
    // Update the matched information
    updateInfoTextFields();
}

/**
Refresh the data type choices in response to the currently selected RiversideDB data store.
@param value if non-null, then the selection is from the command initialization, in which case the
specified data type should be selected
*/
private void actionPerformedDataStoreSelected ( )
{
    if ( __DataStore_JComboBox.getSelected() == null ) {
        // Startup initialization
        //Message.printStatus(2, "", "Selected null data store...initialization" );
        return;
    }
    __dataStore = getSelectedDataStore();
    __dmi = (RiversideDB_DMI)((DatabaseDataStore)__dataStore).getDMI();
    //Message.printStatus(2, "", "Selected data store " + __dataStore + " __dmi=" + __dmi );
    // Now populate the data type choices corresponding to the data store
    populateDataTypeChoices ( __dmi );
    // Update the matched information
    updateInfoTextFields();
}

/**
Refresh the query choices for the currently selected RiversideDB data store.
*/
private void actionPerformedDataSubTypeSelected ( )
{
    if ( __DataSubType_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    // Now populate the interval choices corresponding to the data type and data sub-type
    populateIntervalChoices ( __dmi );
    // Update the matched information
    updateInfoTextFields();
}

/**
Refresh the query choices for the currently selected RiversideDB data store.
*/
private void actionPerformedDataTypeSelected ( )
{
    if ( __DataType_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    // Now populate the data sub-type choices corresponding to the data type
    populateDataSubTypeChoices ( __dmi );
    // Update the matched information
    updateInfoTextFields();
}

/**
Refresh the query choices for the currently selected RiversideDB data store.
*/
private void actionPerformedIntervalSelected ( )
{
    if ( __Interval_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    // Now populate the location choices corresponding to the data type, sub-type, and interval
    populateLocationChoices ( __dmi );
    // Update the matched information
    updateInfoTextFields();
}

/**
Refresh the query choices for the currently selected RiversideDB data store.
*/
private void actionPerformedLocationIDSelected ( )
{
    if ( __LocationID_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    // Now populate the data source choices corresponding to the data type, sub-type, interval, and location
    populateDataSourceChoices ( __dmi );
    // Update the matched information
    updateInfoTextFields();
}

/**
Refresh the query choices for the currently selected RiversideDB scenario.
*/
private void actionPerformedScenarioSelected ( )
{
    if ( __Scenario_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    // Now populate the sequence number choices corresponding to the data type, data sub-type, interval, location,
    // and data source
    populateSequenceNumberChoices ( __dmi );
    // Update the matched information
    updateInfoTextFields();
}

/**
Refresh the query choices for the currently selected RiversideDB sequence number.
*/
private void actionPerformedSequenceNumberSelected ( )
{
    if ( __SequenceNumber_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    // No need to populate any additional choices
    // Update the matched information
    updateInfoTextFields();
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
{	// Put together a list of parameters to check...
    if ( __ignoreEvents ) {
        // Startup
        return;
    }
	PropList parameters = new PropList ( "" );
    String DataStore = __DataStore_JComboBox.getSelected();
    String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String LocationID = __LocationID_JComboBox.getSelected();
    String DataSource = __DataSource_JComboBox.getSelected();
    String DataType = getSelectedDataType();
    String DataSubType = __DataSubType_JComboBox.getSelected();
    String Interval = __Interval_JComboBox.getSelected();
    String Scenario = __Scenario_JComboBox.getSelected();
    String SequenceNumber = __SequenceNumber_JComboBox.getSelected();
    String WriteDataFlags = __WriteDataFlags_JComboBox.getSelected();
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
    if ( (LocationID != null) && (LocationID.length() > 0) ) {
        parameters.set ( "LocationID", LocationID );
    }
    if ( (DataSource != null) && (DataSource.length() > 0) ) {
        parameters.set ( "DataSource", DataSource );
    }
    if ( (DataType != null) && (DataType.length() > 0) ) {
        parameters.set ( "DataType", DataType );
    }
    if ( (DataSubType != null) && (DataSubType.length() > 0) ) {
        parameters.set ( "DataSubType", DataSubType );
    }
    if ( (Interval != null) && (Interval.length() > 0) ) {
        parameters.set ( "Interval", Interval );
    }
    if ( (Scenario != null) && (Scenario.length() > 0) ) {
        parameters.set ( "Scenario", Scenario );
    }
    if ( (SequenceNumber != null) && (SequenceNumber.length() > 0) ) {
        parameters.set ( "SequenceNumber", SequenceNumber );
    }
    if ( (WriteDataFlags != null) && (WriteDataFlags.length() > 0) ) {
        parameters.set ( "WriteDataFlags", WriteDataFlags );
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
    String LocationID = __LocationID_JComboBox.getSelected();
    String DataSource = __DataSource_JComboBox.getSelected();
    String DataType = getSelectedDataType();
    String DataSubType = __DataSubType_JComboBox.getSelected();
    String Interval = __Interval_JComboBox.getSelected();
    String Scenario = __Scenario_JComboBox.getSelected();
    String SequenceNumber = __SequenceNumber_JComboBox.getSelected();
    String WriteDataFlags = __WriteDataFlags_JComboBox.getSelected();
	String OutputStart = __OutputStart_JTextField.getText().trim();
	String OutputEnd = __OutputEnd_JTextField.getText().trim();
	__command.setCommandParameter ( "DataStore", DataStore );
	__command.setCommandParameter ( "TSList", TSList );
    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
    __command.setCommandParameter ( "LocationID", LocationID );
    __command.setCommandParameter ( "DataSource", DataSource );
    __command.setCommandParameter ( "DataType", DataType );
    __command.setCommandParameter ( "DataSubType", DataSubType );
    __command.setCommandParameter ( "Interval", Interval );
    __command.setCommandParameter ( "Scenario", Scenario );
    __command.setCommandParameter ( "SequenceNumber", SequenceNumber );
    __command.setCommandParameter ( "WriteDataFlags", WriteDataFlags );
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
Return the RiversideDB_DMI that is currently being used for database interaction, based on the selected data store.
*/
private RiversideDB_DMI getRiversideDB_DMI ()
{
    return __dmi;
}

/**
Get the selected data store.
*/
private RiversideDBDataStore getSelectedDataStore ()
{   String routine = getClass().getName() + ".getSelectedDataStore";
    String DataStore = __DataStore_JComboBox.getSelected();
    RiversideDBDataStore dataStore = (RiversideDBDataStore)((TSCommandProcessor)
        __command.getCommandProcessor()).getDataStoreForName(
        DataStore, RiversideDBDataStore.class );
    if ( dataStore != null ) {
        Message.printStatus(2, routine, "Selected data store is \"" + dataStore.getName() + "\"." );
    }
    else {
        Message.printStatus(2, routine, "Cannot get data store for \"" + DataStore + "\"." );
    }
    return dataStore;
}

/**
Return the selected data type, omitting the description.
*/
private String getSelectedDataType()
{
    if ( __DataType_JComboBox == null ) {
        return null;
    }
    String dataType = __DataType_JComboBox.getSelected();
    if ( dataType.indexOf(" ") > 0 ) {
        dataType = StringUtil.getToken(dataType," ",0,0).trim();
    }
    else {
        dataType = dataType.trim();
    }
    return dataType;
}

/**
Return the selected DMI (the one from the selected data store).
*/
private RiversideDB_DMI getSelectedDMI()
{
    RiversideDBDataStore selectedDataStore = getSelectedDataStore ();
    if ( selectedDataStore != null ) {
        return (RiversideDB_DMI)selectedDataStore.getDMI();
    }
    return null;
}

// TODO SAM 2012-04-02 After other cleanup this seems way too complicated - need to get rid of "parameter"
/**
Form a TSID string from the selected parameters.  Checks are done based on the parameter that is being
processed, to reflect the cascade of changes that occur as a user selects parameter values.
The further down the sequence of selecting parameters, the more information is added to the TSID.  However,
the information is not added left to right because the order of TSID parts presented to the user starts with
data type so as to present a more reasonable (short) lists of choices.
The parameters are listed in the order of cascading choices, as values from the component (may be null if
component data are not initialized).
@param parameter the parameter to include in the TSID, which should be one "less" than the current parameter
being processed to create a list (e.g., if the choices for location are being populated, pass the UI parameter
above this (interval).  Specify as null to include all specified parameter values.
*/
private String getTSIDFromParameters ( Parameter parameter, String dataType, String dataSubType, String interval,
    String location, String dataSource, String scenario, String sequenceNumber )
{   // Strings used in the final formatting of the TSID
    String location2 = "";
    String dataSource2 = "";
    String dataType2 = "";
    String interval2 = "";
    String scenario2 = "";
    String sequenceNumber2 = "";
    // Data type is selected first
    if ( (parameter == null) || (parameter.ordinal() >= Parameter.DATA_TYPE.ordinal()) ) {
        if ( dataType == null ) {
            dataType = "";
        }
        dataType2 = dataType.trim();
    }
    if ( (parameter == null) || (parameter.ordinal() >= Parameter.DATA_SUBTYPE.ordinal()) ) {
        if ( dataSubType == null ) {
            dataSubType = "";
        }
        dataSubType = dataSubType.trim();
        if ( dataSubType.length() > 0 ) {
            // Modify the data type
            dataType2 = dataType2 + "-" + dataSubType;
        }
    }
    if ( (parameter == null) || (parameter.ordinal() >= Parameter.INTERVAL.ordinal()) ) {
        if ( interval == null ) {
            interval = "";
        }
        interval2 = interval.trim();
    }
    if ( (parameter == null) || (parameter.ordinal() >= Parameter.LOCATION.ordinal()) ) {
        if ( location == null ) {
            location = "";
        }
        location2 = location.trim();
    }
    if ( (parameter == null) || (parameter.ordinal() >= Parameter.DATA_SOURCE.ordinal()) ) {
        if ( dataSource == null ) {
            dataSource = "";
        }
        dataSource2 = dataSource.trim();
    }
    if ( (parameter == null) || (parameter.ordinal() >= Parameter.SCENARIO.ordinal()) ) {
        if ( scenario == null ) {
            scenario = "";
        }
        scenario2 = scenario.trim();
    }
    if ( (parameter == null) || (parameter.ordinal() >= Parameter.SEQUENCE_NUMBER.ordinal()) ) {
        if ( sequenceNumber == null ) {
            sequenceNumber = "";
        }
        sequenceNumber2 = sequenceNumber.trim();
        if ( sequenceNumber2.length() > 0 ) {
            sequenceNumber2 = "[" + sequenceNumber2 + "]";
        }
    }
    StringBuffer tsid = new StringBuffer ( "" );
    tsid.append ( location2 + "." );
    tsid.append ( dataSource2 + "." );
    tsid.append ( dataType2 + "." );
    tsid.append ( interval2 + "." );
    tsid.append ( scenario2 );
    tsid.append ( sequenceNumber2 );
    return tsid.toString();
}

/**
Instantiates the GUI components.
@param parent Frame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, WriteRiversideDB_Command command )
{	__command = command;
    CommandProcessor processor = __command.getCommandProcessor();

	addWindowListener( this );

    Insets insetsTLBR = new Insets(1,2,1,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int yMain = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "<html><b>This command currently will write only one time series.  " +
        "Use parameters to match a single time series in the database.</b></html>." ),
        0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Use the parameters to match a specific time series; " +
		"choices will be updated based on previous selections." ),
		0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "TSTool will only write time series records.  TSTool will not write records for " +
        "time series metadata (the time series must have been previously defined)."),
        0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Enter output date/times to a " +
		"precision appropriate for output time series."),
		0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // List available data stores of the correct type
    // Other lists are NOT populated until a data store is selected (driven by events)

    __ignoreEvents = true; // So that a full pass of initialization can occur
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data store:"),
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStore_JComboBox = new SimpleJComboBox ( false );
    List<DataStore> dataStoreList = ((TSCommandProcessor)processor).getDataStoresByType(RiversideDBDataStore.class );
    for ( DataStore dataStore: dataStoreList ) {
        __DataStore_JComboBox.addItem ( dataStore.getName() );
    }
    if ( __DataStore_JComboBox.getItemCount() > 0 ) {
        __DataStore_JComboBox.select ( 0 );
    }
    __DataStore_JComboBox.addItemListener ( this ); // Add after initial select to avoid event
    JGUIUtil.addComponent(main_JPanel, __DataStore_JComboBox,
        1, yMain, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - open data store for RiversideDB database."), 
        3, yMain, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    __TSList_JComboBox = new SimpleJComboBox(false);
    yMain = CommandEditorUtil.addTSListToEditorDialogPanel ( this, main_JPanel, __TSList_JComboBox, yMain );

    __TSID_JLabel = new JLabel ("TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __TSID_JComboBox = new SimpleJComboBox ( true );  // Allow edits
    List tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    yMain = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, main_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, yMain );
    
    __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    yMain = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
        this, this, main_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, yMain );
    
    // Create a tabbed pane to allow specifying time series specifically or using the time series
    // properties (allows processing multiple time series)
    
    __tsInfo_JTabbedPane = new JTabbedPane ();
    __tsInfo_JTabbedPane.setBorder(
        BorderFactory.createTitledBorder ( BorderFactory.createLineBorder(Color.black),
        "Indicate how to match time series in database" ));
    JGUIUtil.addComponent(main_JPanel, __tsInfo_JTabbedPane,
        0, ++yMain, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    JPanel singleTS_JPanel = new JPanel();
    singleTS_JPanel.setLayout(new GridBagLayout());
    __tsInfo_JTabbedPane.addTab ( "Match Specified Single Time Series", singleTS_JPanel );
 
    int ySingle = -1;
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel ("Data type:"), 
        0, ++ySingle, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataType_JComboBox = new SimpleJComboBox (false);
    __DataType_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(singleTS_JPanel, __DataType_JComboBox,
        1, ySingle, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel (
        "Required - matching (main) data type in the database."),
        3, ySingle, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel ("Data sub-type:"), 
        0, ++ySingle, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataSubType_JComboBox = new SimpleJComboBox (false);
    __DataSubType_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(singleTS_JPanel, __DataSubType_JComboBox,
        1, ySingle, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel (
        "Required - matching (sub) data type in the database (may be blank)."),
        3, ySingle, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel ("Data interval:"), 
        0, ++ySingle, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Interval_JComboBox = new SimpleJComboBox (false);
    __Interval_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(singleTS_JPanel, __Interval_JComboBox,
        1, ySingle, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel (
        "Required - matching data interval in the database."),
        3, ySingle, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Put the following after data type and interval so that the list of locations is reasonably short
    
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel ("Location (station/area) ID:"), 
        0, ++ySingle, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __LocationID_JComboBox = new SimpleJComboBox (false);
    __LocationID_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(singleTS_JPanel, __LocationID_JComboBox,
        1, ySingle, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel (
        "Required - matching location ID in the database."),
        3, ySingle, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel ("Data source abbreviation:"), 
        0, ++ySingle, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataSource_JComboBox = new SimpleJComboBox (false);
    __DataSource_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(singleTS_JPanel, __DataSource_JComboBox,
        1, ySingle, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel (
        "Required - matching data source in the database."),
        3, ySingle, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel ("Scenario:"), 
        0, ++ySingle, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Scenario_JComboBox = new SimpleJComboBox ( false );
    __Scenario_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(singleTS_JPanel, __Scenario_JComboBox,
        1, ySingle, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel (
        "Required - matching scenario in the database (may be blank)."),
        3, ySingle, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel ("Sequence number:"), 
        0, ++ySingle, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SequenceNumber_JComboBox = new SimpleJComboBox ( false );
    __SequenceNumber_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(singleTS_JPanel, __SequenceNumber_JComboBox,
        1, ySingle, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel (
        "Required - for ensembles, the sequence number (trace starting year)."),
        3, ySingle, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(singleTS_JPanel, new JLabel ("Matching MeasLoc_num:"), 
        0, ++ySingle, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __selectedMeasLocNum_JLabel = new JLabel ( "");
    JGUIUtil.addComponent(singleTS_JPanel, __selectedMeasLocNum_JLabel,
        1, ySingle, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel (
        "Information - useful when comparing to database contents."),
        3, ySingle, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel ("Matching MeasType_num:"), 
        0, ++ySingle, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __selectedMeasTypeNum_JLabel = new JLabel ( "");
    JGUIUtil.addComponent(singleTS_JPanel, __selectedMeasTypeNum_JLabel,
        1, ySingle, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel (
        "Information - useful when comparing to database contents."),
        3, ySingle, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JPanel multipleTS_JPanel = new JPanel();
    multipleTS_JPanel.setLayout(new GridBagLayout());
    __tsInfo_JTabbedPane.addTab ( "Match Time Series Using Properties", multipleTS_JPanel );
 
    int yMultiple = -1;
    JGUIUtil.addComponent(multipleTS_JPanel, new JLabel (
        "In the future matching time series will be specified using time series properties (e.g., %L for location), " +
        "to allow multiple time series to be written with one command."),
        0, yMultiple, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Select the single time series
    
    __tsInfo_JTabbedPane.setSelectedIndex(0);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Write data flags?:"), 
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __WriteDataFlags_JComboBox = new SimpleJComboBox ( false );
    __WriteDataFlags_JComboBox.addItemListener (this);
    List<String> writeDataFlagsList = new Vector();
    writeDataFlagsList.add("");
    writeDataFlagsList.add(__command._False);
    writeDataFlagsList.add(__command._True);
    __WriteDataFlags_JComboBox.setData ( writeDataFlagsList );
    __WriteDataFlags_JComboBox.select(0);
    JGUIUtil.addComponent(main_JPanel, __WriteDataFlags_JComboBox,
        1, yMain, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - should data flags be written? (default=" + __command._True + ")."),
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
    // All of the components have been initialized above but now generate an event to populate choices
    // based on the data store...
    /*
    Message.printStatus(2,"","Selecting first data store to cascade choices");
    if ( __DataStore_JComboBox.getItemCount() > 0 ) {
        __DataStore_JComboBox.select(null);
        __DataStore_JComboBox.select(0);
    }
    */
    //Message.printStatus(2,"","Calling refresh()");
    refresh ();
    __ignoreEvents = false; // After initialization of components let events happen
    updateInfoTextFields();
    
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
{   
    if ( __ignoreEvents ) {
        return;
    }
    checkGUIState();
    Object source = e.getSource();
    int sc = e.getStateChange();
    if ( (source == __DataStore_JComboBox) && (sc == ItemEvent.SELECTED) ) {
        // User has selected a data store.
        actionPerformedDataStoreSelected ();
    }
    else if ( (source == __DataSubType_JComboBox) && (sc == ItemEvent.SELECTED) ) {
        // User has selected a data sub-type.
        actionPerformedDataSubTypeSelected ();
    }
    else if ( (source == __DataType_JComboBox) && (sc == ItemEvent.SELECTED) ) {
        // User has selected a data type.
        actionPerformedDataTypeSelected ();
    }
    else if ( (source == __Interval_JComboBox) && (sc == ItemEvent.SELECTED) ) {
        // User has selected an interval.
        actionPerformedIntervalSelected ();
    }
    else if ( (source == __LocationID_JComboBox) && (sc == ItemEvent.SELECTED) ) {
        // User has selected a location.
        actionPerformedLocationIDSelected ();
    }
    else if ( (source == __DataSource_JComboBox) && (sc == ItemEvent.SELECTED) ) {
        // User has selected a data source.
        actionPerformedDataSourceSelected ();
    }
    else if ( (source == __Scenario_JComboBox) && (sc == ItemEvent.SELECTED) ) {
        // User has selected a scenario.
        actionPerformedScenarioSelected ();
    }
    else if ( (source == __SequenceNumber_JComboBox) && (sc == ItemEvent.SELECTED) ) {
        // User has selected a sequence number.
        actionPerformedSequenceNumberSelected ();
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
Populate the data source choices.
*/
private void populateDataSourceChoices ( RiversideDB_DMI dmi )
{   String routine = getClass().getName() + ".populateDataSourceChoices";
    if ( (dmi == null) || (__DataType_JComboBox == null) || (__DataSubType_JComboBox == null) ||
        (__Interval_JComboBox == null) || (__LocationID_JComboBox == null) ) {
        // Initialization
        return;
    }
    String tsid = getTSIDFromParameters ( Parameter.LOCATION,
            getSelectedDataType(),
            __DataSubType_JComboBox.getSelected(),
            __Interval_JComboBox.getSelected(),
            __LocationID_JComboBox.getSelected(),
            null, // data source
            null, // scenario
            null ); // sequence number
    List<RiversideDB_MeasType> measTypeList = null;
    try {
        measTypeList = dmi.readMeasTypeListForTSIdent ( tsid );
    }
    catch ( Exception e ) {
        Message.printWarning(2, routine, "Error getting MeasTypes from RiversideDB \"" +
            __dataStore.getName() + " tsid=\"" + tsid + "\" (" + e + ").");
        Message.printWarning(2, routine, e);
        return;
    }
    Message.printStatus(2, routine, "Got " + measTypeList.size() + " MeasType using tsid=\"" + tsid + "\"." );
    __DataSource_JComboBox.removeAll ();
    String dataSource;
    List<String> dataSourceList = new Vector();
    for ( RiversideDB_MeasType measType : measTypeList ) {
        // Only add if not already listed. Alternatively - need to enable a "distinct" query
        dataSource = measType.getSource_abbrev();
        if ( dataSource == null ) {
            continue;
        }
        dataSource = dataSource.trim();
        if ( StringUtil.indexOfIgnoreCase(dataSourceList, dataSource) < 0 ){
            dataSourceList.add(dataSource);
        }
    }
    java.util.Collections.sort(dataSourceList);
    __DataSource_JComboBox.setData ( dataSourceList );
    // Select first choice (may get reset from existing parameter values).
    __DataSource_JComboBox.select ( null );
    if ( __DataSource_JComboBox.getItemCount() > 0 ) {
        __DataSource_JComboBox.select ( 0 );
    }
}

/**
Populate the data sub-type choices for the given data type.
*/
private void populateDataSubTypeChoices ( RiversideDB_DMI dmi )
{   String routine = getClass().getName() + ".populateDataSubTypeChoices";
    if ( (dmi == null) || (__DataType_JComboBox == null) || (__DataSubType_JComboBox == null) ) {
        // Still in initialization
        return;
    }
    String dataType = getSelectedDataType();
    if ( dataType == null ) {
        return;
    }
    List<String> dataSubTypeStrings = new Vector();
    dataSubTypeStrings.add(""); // Add empty string because may be null in database
    List<RiversideDB_MeasType> measTypeList = dmi.findMeasType(
        __measTypeList, -1, null, dataType, null, null, null, null);
    __DataSubType_JComboBox.removeAll ();
    for ( RiversideDB_MeasType measType : measTypeList ) {
        dataSubTypeStrings.add ( measType.getSub_type() );
    }
    Collections.sort(dataSubTypeStrings,String.CASE_INSENSITIVE_ORDER);
    StringUtil.removeDuplicates(dataSubTypeStrings, true, true);
    __DataSubType_JComboBox.setData(dataSubTypeStrings);
    // Select first choice (may get reset from existing parameter values).
    __DataSubType_JComboBox.select ( null );
    if ( __DataSubType_JComboBox.getItemCount() > 0 ) {
        __DataSubType_JComboBox.select ( 0 );
    }
}

/**
Populate the data type list based on the selected database.
*/
private void populateDataTypeChoices ( RiversideDB_DMI rdmi )
{   String routine = getClass().getName() + ".populateDataTypeChoices";
    if ( rdmi == null ) {
        // Initialization
        //Message.printStatus(2,routine,"DMI is null - not initializing choices" );
        return;
    }
    if ( __DataType_JComboBox == null ) {
        // Initialization
        //Message.printStatus(2,routine,"__DataType_Combobox is null - not initializing choices" );
        return;
    }
    __DataType_JComboBox.removeAll ();
    List<RiversideDB_MeasType> measTypeList = null;
    List<RiversideDB_DataType> dataTypeList = null;
    try {
        // MeasTypes come back sorted by data type abbreviation
        measTypeList = rdmi.readMeasTypeListForDistinctData_type();
        // Unique data types in RiversideDB
        dataTypeList = rdmi.readDataTypeList();
    }
    catch ( Exception e ) {
        Message.printWarning ( 1, routine, "Error getting time series for data type choices (" + e + ")." );
        Message.printWarning ( 3, routine, e );
        Message.printWarning ( 3, routine, rdmi.getLastSQLString() );
        measTypeList = null;
    }
    int dataTypeLengthMax = 80;
    int pos;
    String dataType;
    for ( RiversideDB_MeasType mt: measTypeList ) {
        pos = RiversideDB_DataType.indexOf (dataTypeList, mt.getData_type() );
        if ( pos < 0 ) {
            // Data type is not in the DataType table so can't add description
            __DataType_JComboBox.add(mt.getData_type() );
        }
        else {
            // Have matching data type object so can get description of data type - still unique since only appending to end
            // of each string
            dataType = mt.getData_type() + " - " + dataTypeList.get(pos).getDescription();
            if ( dataType.length() > dataTypeLengthMax ) {
                // Shorten the string
                __DataType_JComboBox.add( dataType.substring(0,dataTypeLengthMax) + "..." );
            }
            else {
                __DataType_JComboBox.add( dataType );
            }
        }
    }
    //Message.printStatus(2,routine,"Got " + __DataType_JComboBox.getItemCount() + " data types" );
    readMeasTypeListForDataType(rdmi);
    // Select first choice (may get reset from existing parameter values).
    __DataType_JComboBox.select ( null );
    if ( __DataType_JComboBox.getItemCount() > 0 ) {
        __DataType_JComboBox.select ( 0 );
    }
}

/**
Populate the interval choices.
*/
private void populateIntervalChoices ( RiversideDB_DMI dmi )
{   String routine = getClass().getName() + ".populateIntervalChoices";
    if ( (dmi == null) || (__DataType_JComboBox == null) || (__DataSubType_JComboBox == null) ) {
        // Initialization
        return;
    }
    String tsid = getTSIDFromParameters ( Parameter.DATA_SUBTYPE, getSelectedDataType(),
            __DataSubType_JComboBox.getSelected(),
            __Interval_JComboBox.getSelected(),
            null, // location
            null, // data source
            null, // scenario
            null ); // sequence number
    List<RiversideDB_MeasType> measTypeList = null;
    try {
        measTypeList = dmi.readMeasTypeListForTSIdent ( tsid );
    }
    catch ( Exception e ) {
        Message.printWarning(2, routine, "Error getting MeasTypes from RiversideDB \"" +
            __dataStore.getName() + " tsid=\"" + tsid + "\" (" + e + ").");
        Message.printWarning(2, routine, e);
        return;
    }
    String timestep;
    String time_step_base;
    long time_step_mult;
    __Interval_JComboBox.removeAll ();
    for ( RiversideDB_MeasType measType : measTypeList ) {
        // Only add if not already listed. Alternatively - need to enable a "distinct" query
        time_step_base = measType.getTime_step_base();
        time_step_mult = measType.getTime_step_mult();
        if ( time_step_base.equalsIgnoreCase( "IRREGULAR") || DMIUtil.isMissing(time_step_mult) ) {
            timestep = measType.getTime_step_base();
        }
        else {
            timestep = "" + measType.getTime_step_mult() + measType.getTime_step_base();
        }
        if ( !JGUIUtil.isSimpleJComboBoxItem(__Interval_JComboBox, timestep, JGUIUtil.NONE, null, null)){
            __Interval_JComboBox.add(timestep);
        }
    }
    // Select first choice (may get reset from existing parameter values).
    // TODO SAM 2012-02-19 Need a way to intelligently sort the intervals
    __Interval_JComboBox.select ( null );
    if ( __Interval_JComboBox.getItemCount() > 0 ) {
        __Interval_JComboBox.select ( 0 );
    }
}

/**
Populate the location choices.
*/
private void populateLocationChoices ( RiversideDB_DMI dmi )
{   String routine = getClass().getName() + ".populateLocationChoices";
    if ( (dmi == null) || (__DataType_JComboBox == null) ||
        (__DataSubType_JComboBox == null) || (__Interval_JComboBox == null) ||
        (__LocationID_JComboBox == null)) {
        // Initialization
        return;
    }
    String tsid = getTSIDFromParameters ( Parameter.INTERVAL, getSelectedDataType(),
        __DataSubType_JComboBox.getSelected(),
        __Interval_JComboBox.getSelected(),
        null, // location
        null, // data source
        null, // scenario
        null ); // sequence number
    if ( tsid == null ) {
        return;
    }
    List<RiversideDB_MeasType> measTypeList;
    try {
        measTypeList = dmi.readMeasTypeListForTSIdent ( tsid );
    }
    catch ( Exception e ) {
        Message.printWarning(2, routine, "Error getting MeasTypes from RiversideDB \"" +
            __dataStore.getName() + " tsid=\"" + tsid + "\" (" + e + ").");
        Message.printWarning(2, routine, e);
        return;
    }
    Message.printStatus(2, routine, "Got " + measTypeList.size() + " MeasType using tsid=\"" + tsid + "\"." );
    __LocationID_JComboBox.removeAll ();
    List<String> locationList = new Vector();
    String id;
    RiversideDB_MeasLoc measLoc;
    for ( RiversideDB_MeasType measType : measTypeList ) {
        try {
            measLoc = dmi.readMeasLocForMeasLoc_num(measType.getMeasLoc_num() );
        }
        catch ( Exception e ) {
            Message.printWarning(2, routine, "Error getting MeasLoc from RiversideDB \"" +
                __dataStore.getName() + " measLoc_num=" + measType.getMeasLoc_num() + " (" + e + ").");
            Message.printWarning(2, routine, e);
            continue;
        }
        if ( measLoc == null ) {
            continue;
        }
        // Only add if not already listed. Alternatively - need to add a "distinct" query
        // Get the MeasType...
        id = measLoc.getIdentifier();
        if ( id == null ) {
            continue;
        }
        id = id.trim();
        if ( StringUtil.indexOfIgnoreCase(locationList, id) < 0 ){
            locationList.add(id);
        }
    }
    java.util.Collections.sort(locationList);
    __LocationID_JComboBox.setData ( locationList );
    // Select first choice (may get reset from existing parameter values).
    __LocationID_JComboBox.select ( null );
    if ( __LocationID_JComboBox.getItemCount() > 0 ){
        __LocationID_JComboBox.select ( 0 );
    }
}

/**
Populate the scenario choices.
*/
private void populateScenarioChoices ( RiversideDB_DMI dmi )
{   String routine = getClass().getName() + ".populateScenarioChoices";
    if ( (dmi == null) || (__DataType_JComboBox == null) || (__DataSubType_JComboBox == null) ||
        (__Interval_JComboBox == null) || (__LocationID_JComboBox == null) || (__DataSource_JComboBox == null)) {
        // Initialization
        return;
    }
    String tsid = getTSIDFromParameters ( Parameter.DATA_SOURCE,
            getSelectedDataType(),
            __DataSubType_JComboBox.getSelected(),
            __Interval_JComboBox.getSelected(),
            __LocationID_JComboBox.getSelected(),
            __DataSource_JComboBox.getSelected(),
            null, // scenario
            null ); // sequence number
    List<RiversideDB_MeasType> measTypeList = null;
    try {
        measTypeList = dmi.readMeasTypeListForTSIdent ( tsid );
    }
    catch ( Exception e ) {
        Message.printWarning(2, routine, "Error getting MeasTypes from RiversideDB \"" +
            __dataStore.getName() + " tsid=\"" + tsid + "\" (" + e + ").");
        Message.printWarning(2, routine, e);
        return;
    }
    Message.printStatus(2, routine, "Got " + measTypeList.size() + " MeasType using tsid=\"" + tsid + "\"." );
    __Scenario_JComboBox.removeAll ();
    String scenario;
    List<String> scenarioList = new Vector();
    for ( RiversideDB_MeasType measType : measTypeList ) {
        // Only add if not already listed. Alternatively - need to enable a "distinct" query
        scenario = measType.getScenario();
        if ( scenario == null ) {
            continue;
        }
        scenario = scenario.trim();
        if ( StringUtil.indexOfIgnoreCase(scenarioList, scenario) < 0 ){
            scenarioList.add(scenario);
        }
    }
    java.util.Collections.sort(scenarioList);
    __Scenario_JComboBox.setData ( scenarioList );
    // Select first choice (may get reset from existing parameter values).
    __Scenario_JComboBox.select ( null );
    if ( __Scenario_JComboBox.getItemCount() > 0 ) {
        __Scenario_JComboBox.select ( 0 );
    }
}

/**
Populate the sequence number choices.
*/
private void populateSequenceNumberChoices ( RiversideDB_DMI dmi )
{   String routine = getClass().getName() + ".populateSequenceNumberChoices";
    if ( (dmi == null) || (__DataType_JComboBox == null) || (__DataSubType_JComboBox == null) ||
        (__Interval_JComboBox == null) || (__LocationID_JComboBox == null) || (__DataSource_JComboBox == null) ||
        (__Scenario_JComboBox == null)) {
        // Initialization
        return;
    }
    String tsid = getTSIDFromParameters ( Parameter.SCENARIO,
            getSelectedDataType(),
            __DataSubType_JComboBox.getSelected(),
            __Interval_JComboBox.getSelected(),
            __LocationID_JComboBox.getSelected(),
            __DataSource_JComboBox.getSelected(),
            __Scenario_JComboBox.getSelected(),
            null ); // sequence number
    List<RiversideDB_MeasType> measTypeList = null;
    try {
        measTypeList = dmi.readMeasTypeListForTSIdent ( tsid );
    }
    catch ( Exception e ) {
        Message.printWarning(2, routine, "Error getting MeasTypes from RiversideDB \"" +
            __dataStore.getName() + " tsid=\"" + tsid + "\" (" + e + ").");
        Message.printWarning(2, routine, e);
        return;
    }
    Message.printStatus(2, routine, "Got " + measTypeList.size() + " MeasType using tsid=\"" + tsid + "\"." );
    __SequenceNumber_JComboBox.removeAll ();
    int sequenceNumber;
    List<String> sequenceNumberList = new Vector();
    sequenceNumberList.add ( "" );
    for ( RiversideDB_MeasType measType : measTypeList ) {
        // Only add if not already listed. Alternatively - need to enable a "distinct" query
        sequenceNumber = measType.getSequence_num();
        if ( DMIUtil.isMissing(sequenceNumber) ) {
            continue;
        }
        sequenceNumberList.add("" + sequenceNumber);
    }
    java.util.Collections.sort(sequenceNumberList);
    StringUtil.removeDuplicates(sequenceNumberList, true, true);
    __SequenceNumber_JComboBox.setData ( sequenceNumberList );
    // Select first choice (may get reset from existing parameter values).
    __SequenceNumber_JComboBox.select ( null );
    if ( __SequenceNumber_JComboBox.getItemCount() > 0 ) {
        __SequenceNumber_JComboBox.select ( 0 );
    }
}

/**
Read the MeasType list for the selected data type, used as the master list for some filters.
*/
private void readMeasTypeListForDataType ( RiversideDB_DMI rdmi )
{   String routine = getClass().getName() + "readMeasTypeForDataType";
    // TODO SAM 2012-04-02 May need to save this list filtered a bit more to improve performance but go with
    // it for now
    // Additionally, query all matching MeasType so that these choices can be filtered without having
    // to requery the database
    String tsid = getTSIDFromParameters ( null, getSelectedDataType(),
        null, // data sub-type
        null, // interval
        null, // location
        null, // data source
        null, // scenario
        null ); // sequence number
    if ( tsid != null ) {
        try {
            __measTypeList = rdmi.readMeasTypeListForTSIdent ( tsid );
            Message.printStatus(2,routine,"Got " + __measTypeList.size() +
                " MeasTypes for TSID \"" + tsid + "\" to use as list to filter choices" );
        }
        catch ( Exception e ) {
            Message.printWarning(2, routine, "Error getting MeasTypes from RiversideDB \"" +
                __dataStore.getName() + " tsid=\"" + tsid + "\" (" + e + ").");
            Message.printWarning(2, routine, e);
            return;
        }
    }
}

/**
Refresh the command from the other text field contents.
When first called this method will attempt to populate the components using parameter values.
Subsequent to the first call the response is passive in that the command string is reconstructed
from displayed values.
*/
private void refresh ()
{
    String routine = "WriteRiversideDB_JDialog.refresh";
    //Message.printStatus ( 2, routine, "__first_time=" + __first_time );
    String DataStore = "";
    String TSList = "";
    String TSID = "";
    String EnsembleID = "";
    String LocationID = "";
    String DataType = "";
    String DataSource = "";
    String DataSubType = "";
    String Interval = "";
    String Scenario = "";
    String SequenceNumber = "";
    String WriteDataFlags = "";
	String OutputStart = "";
	String OutputEnd = "";
	__error_wait = false;
	PropList parameters = null;
	if ( __first_time ) {
		__first_time = false;
		//Message.printStatus(2,routine,"Start initializing parameter components from command");
		// Get the parameters from the command...
		parameters = __command.getCommandParameters();
		DataStore = parameters.getValue ( "DataStore" );
        TSList = parameters.getValue ( "TSList" );
        TSID = parameters.getValue ( "TSID" );
        EnsembleID = parameters.getValue ( "EnsembleID" );
        LocationID = parameters.getValue ( "LocationID" );
        DataSource = parameters.getValue ( "DataSource" );
        DataType = parameters.getValue ( "DataType" );
        DataSubType = parameters.getValue ( "DataSubType" );
        Interval = parameters.getValue ( "Interval" );
        Scenario = parameters.getValue ( "Scenario" );
        SequenceNumber = parameters.getValue ( "SequenceNumber" );
        WriteDataFlags = parameters.getValue ( "WriteDataFlags" );
		OutputStart = parameters.getValue ( "OutputStart" );
		OutputEnd = parameters.getValue ( "OutputEnd" );
        if ( JGUIUtil.isSimpleJComboBoxItem(__DataStore_JComboBox, DataStore, JGUIUtil.NONE, null, null ) ) {
            // This will force a cascading event...
            __DataStore_JComboBox.select ( DataStore );
            if ( __ignoreEvents ) {
                // Also need to make sure that the data store and DMI are actually selected
                // Call manually because events are disabled at startup to allow cascade to work properly
                actionPerformedDataStoreSelected();
            }
        }
        else {
            if ( (DataStore == null) || DataStore.equals("") ) {
                // New command...select the default...
                if ( __DataStore_JComboBox.getItemCount() > 0 ) {
                    __DataStore_JComboBox.select ( 0 );
                    if ( __ignoreEvents ) {
                        // Also need to make sure that the data store and DMI are actually selected
                        // Call manually because events are disabled at startup to allow cascade to work properly
                        actionPerformedDataStoreSelected();
                    }
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid "+
                  "DataStore parameter \"" + DataStore + "\".  Select a\ndifferent value or Cancel." );
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
                "Existing command references an invalid TSList value \"" + TSList +
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
                "Existing command references an invalid EnsembleID value \"" + EnsembleID +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        // These need to be in the order of the dialog in order to properly cascade
        // First populate the choices...
        //Message.printStatus(2,routine,"Initializing data type choices");
        populateDataTypeChoices(getRiversideDB_DMI() );
        // Then set to the initial value.
        int [] dataTypeIndex = new int[1];
        if ( JGUIUtil.isSimpleJComboBoxItem(__DataType_JComboBox, DataType, JGUIUtil.NONE, null, null) ) {
            // Exact match
            __DataType_JComboBox.select ( DataType );
            if ( __ignoreEvents ) {
                // Also need to make sure that the __measTypeList is populated
                // Call manually because events are disabled at startup to allow cascade to work properly
                readMeasTypeListForDataType(__dmi);
            }
        }
        else if ( JGUIUtil.isSimpleJComboBoxItem( __DataType_JComboBox,
            DataType, JGUIUtil.CHECK_SUBSTRINGS, " ", 0, dataTypeIndex, true) ) {
            // DataType will be like "QIN" but choice will be like "QIN - Description" so select based on the
            // first token
            __DataType_JComboBox.select ( dataTypeIndex[0] );
            if ( __ignoreEvents ) {
                // Also need to make sure that the __measTypeList is populated
                // Call manually because events are disabled at startup to allow cascade to work properly
                readMeasTypeListForDataType(__dmi);
            }
        }
        else {
            if ( (DataType == null) || DataType.equals("") ) {
                // New command...select the default...
                if ( __DataType_JComboBox.getItemCount() > 0 ) {
                    __DataType_JComboBox.select ( 0 );
                    if ( __ignoreEvents ) {
                        // Also need to make sure that the __measTypeList is populated
                        // Call manually because events are disabled at startup to allow cascade to work properly
                        readMeasTypeListForDataType(__dmi);
                    }
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid "+
                    "DataType parameter \"" + DataType + "\".  Select a different value or Cancel." );
            }
        }
        // First populate the choices...
        populateDataSubTypeChoices(getRiversideDB_DMI() );
        // Then set to the initial value
        if ( JGUIUtil.isSimpleJComboBoxItem(__DataSubType_JComboBox, DataSubType, JGUIUtil.NONE, null, null ) ) {
            __DataSubType_JComboBox.select ( DataSubType );
        }
        else {
            if ( (DataSubType == null) || DataSubType.equals("") ) {
                // New command...select the default...
                if ( __DataSubType_JComboBox.getItemCount() > 0 ) {
                    __DataSubType_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid "+
                  "DataSubType parameter \"" + DataSubType + "\".  Select a different value or Cancel." );
            }
        }
        // First populate the choices...
        populateIntervalChoices(getRiversideDB_DMI() );
        // Then set to the initial value
        if ( JGUIUtil.isSimpleJComboBoxItem(__Interval_JComboBox, Interval, JGUIUtil.NONE, null, null ) ) {
            __Interval_JComboBox.select ( Interval );
        }
        else {
            if ( (Interval == null) || Interval.equals("") ) {
                // New command...select the default...
                if ( __Interval_JComboBox.getItemCount() > 0 ) {
                    __Interval_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid "+
                  "Interval parameter \"" + Interval + "\".  Select a different value or Cancel." );
            }
        }
        // First populate the choices...
        populateLocationChoices(getRiversideDB_DMI() );
        // Then set to the initial value
        if ( JGUIUtil.isSimpleJComboBoxItem(__LocationID_JComboBox, LocationID, JGUIUtil.NONE, null, null ) ) {
            __LocationID_JComboBox.select ( LocationID );
        }
        else {
            if ( (LocationID == null) || LocationID.equals("") ) {
                // New command...select the default...
                if ( __LocationID_JComboBox.getItemCount() > 0 ) {
                    __LocationID_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid "+
                  "LocationID parameter \"" + LocationID + "\".  Select a different value or Cancel." );
            }
        }
        // First populate the choices...
        populateDataSourceChoices(getRiversideDB_DMI() );
        // Then set to the initial value
        if ( JGUIUtil.isSimpleJComboBoxItem(__DataSource_JComboBox, DataSource, JGUIUtil.NONE, null, null ) ) {
            __DataSource_JComboBox.select ( DataSource );
        }
        else {
            if ( (DataSource == null) || DataSource.equals("") ) {
                // New command...select the default...
                if ( __DataSource_JComboBox.getItemCount() > 0 ) {
                    __DataSource_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid "+
                  "DataSource parameter \"" + DataSource + "\".  Select a different value or Cancel." );
            }
        }
        // First populate the choices...
        populateScenarioChoices(getRiversideDB_DMI() );
        // Then set to the initial value
        if ( JGUIUtil.isSimpleJComboBoxItem(__Scenario_JComboBox, Scenario, JGUIUtil.NONE, null, null ) ) {
            __Scenario_JComboBox.select ( Scenario );
        }
        else {
            if ( (Scenario == null) || Scenario.equals("") ) {
                // New command...select the default...
                if ( __Scenario_JComboBox.getItemCount() > 0 ) {
                    __Scenario_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid "+
                  "Scenario parameter \"" + Scenario + "\".  Select a different value or Cancel." );
            }
        }
        // First populate the choices...
        populateSequenceNumberChoices(getRiversideDB_DMI() );
        // Then set to the initial value
        if ( JGUIUtil.isSimpleJComboBoxItem(__SequenceNumber_JComboBox, SequenceNumber, JGUIUtil.NONE, null, null ) ) {
            __SequenceNumber_JComboBox.select ( SequenceNumber );
        }
        else {
            if ( (SequenceNumber == null) || SequenceNumber.equals("") ) {
                // New command...select the default...
                if ( __SequenceNumber_JComboBox.getItemCount() > 0 ) {
                    __SequenceNumber_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid "+
                  "SequenceNumber parameter \"" + SequenceNumber + "\".  Select a different value or Cancel." );
            }
        }
        if ( JGUIUtil.isSimpleJComboBoxItem(__WriteDataFlags_JComboBox, WriteDataFlags, JGUIUtil.NONE, null, null ) ) {
            __WriteDataFlags_JComboBox.select ( WriteDataFlags );
        }
        else {
            if ( (WriteDataFlags == null) || WriteDataFlags.equals("") ) {
                // New command...select the default...
                if ( __WriteDataFlags_JComboBox.getItemCount() > 0 ) {
                    __WriteDataFlags_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid "+
                  "WriteDataFlags parameter \"" + WriteDataFlags + "\".  Select a different value or Cancel." );
            }
        }
		if ( OutputStart != null ) {
			__OutputStart_JTextField.setText (OutputStart);
		}
		if ( OutputEnd != null ) {
			__OutputEnd_JTextField.setText (OutputEnd);
		}
		Message.printStatus(2,routine,"...done initializing parameter components from command");
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
    // FIXME SAM 2011-10-03 Might be able to remove check for null if events and list population are
    // implemented correctly
    DataType = getSelectedDataType();
    DataSubType = __DataSubType_JComboBox.getSelected();
    if ( DataSubType == null ) {
        DataSubType = "";
    }
    Interval = __Interval_JComboBox.getSelected();
    if ( Interval == null ) {
        Interval = "";
    }
    LocationID = __LocationID_JComboBox.getSelected();
    if ( LocationID == null ) {
        LocationID = "";
    }
    DataSource = __DataSource_JComboBox.getSelected();
    if ( DataSource == null ) {
        DataSource = "";
    }
    Scenario = __Scenario_JComboBox.getSelected();
    if ( Scenario == null ) {
        Scenario = "";
    }
    SequenceNumber = __SequenceNumber_JComboBox.getSelected();
    if ( SequenceNumber == null ) {
        SequenceNumber = "";
    }
    WriteDataFlags = __WriteDataFlags_JComboBox.getSelected();
    if ( WriteDataFlags == null ) {
        WriteDataFlags = "";
    }
	OutputStart = __OutputStart_JTextField.getText().trim();
	OutputEnd = __OutputEnd_JTextField.getText().trim();
	parameters = new PropList ( __command.getCommandName() );
	parameters.add ( "DataStore=" + DataStore );
	parameters.add ( "TSList=" + TSList );
    parameters.add ( "TSID=" + TSID );
    parameters.add ( "EnsembleID=" + EnsembleID );
    parameters.add ( "DataType=" + DataType );
    parameters.add ( "DataSubType=" + DataSubType );
    parameters.add ( "Interval=" + Interval );
    parameters.add ( "LocationID=" + LocationID );
    parameters.add ( "DataSource=" + DataSource );
    parameters.add ( "Scenario=" + Scenario );
    parameters.add ( "SequenceNumber=" + SequenceNumber );
    parameters.add ( "WriteDataFlags=" + WriteDataFlags );
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
Update the information text fields.
*/
private void updateInfoTextFields ()
{   // First find the MeasLoc
    RiversideDB_MeasLoc measLoc = null;
    String locationID = __LocationID_JComboBox.getSelected();
    if ( (locationID != null) && !locationID.equals("") ) {
        try {
            measLoc = __dmi.readMeasLocForIdentifier(locationID);
        }
        catch ( Exception e ) {
            
        }
    }
    if ( measLoc == null ) {
        __selectedMeasLocNum_JLabel.setText ( "No matches" );
    }
    else {
        __selectedMeasLocNum_JLabel.setText ( "" + measLoc.getMeasLoc_num() );
    }

    if ( measLoc == null ) {
        __selectedMeasTypeNum_JLabel.setText ( "No matches because MeasLoc_num not matched." );
    }
    else {
        // Next use the MeasLoc_num with other data to find the MeasType_num
        List<RiversideDB_MeasType> measTypeList = __dmi.findMeasType(__measTypeList,
            measLoc.getMeasLoc_num(),
            __DataSource_JComboBox.getSelected(),
            getSelectedDataType(),
            __DataSubType_JComboBox.getSelected(),
            __Interval_JComboBox.getSelected(),
            __Scenario_JComboBox.getSelected(),
            __SequenceNumber_JComboBox.getSelected() );
        if ( measTypeList.size() == 0 ) {
            __selectedMeasTypeNum_JLabel.setText ( "No matches" );
        }
        else if ( measTypeList.size() > 0 ) {
            __selectedMeasTypeNum_JLabel.setText ( "" + measTypeList.get(0).getMeasType_num() );
        }
        else {
            __selectedMeasTypeNum_JLabel.setText ( "" + measTypeList.size() + " matches" );
        }
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