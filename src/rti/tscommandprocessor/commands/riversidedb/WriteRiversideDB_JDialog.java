package rti.tscommandprocessor.commands.riversidedb;

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

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

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
private SimpleJComboBox __Location_JComboBox = null;
private SimpleJComboBox __DataSource_JComboBox = null;
private SimpleJComboBox __DataType_JComboBox = null;
private SimpleJComboBox __DataSubType_JComboBox = null;
private SimpleJComboBox __Interval_JComboBox = null;
private SimpleJComboBox __Scenario_JComboBox = null;
private SimpleJComboBox __SequenceNumber_JComboBox = null;
private SimpleJComboBox __CopyDataFlags_JComboBox = null;
private JTextField __OutputStart_JTextField = null;
private JTextField __OutputEnd_JTextField = null;
private RiversideDBDataStore __dataStore = null; // selected RiversideDB_DataStore
private RiversideDB_DMI __dmi = null; // RiversideDB_DMI to do queries.
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Has user pressed OK to close the dialog?
private boolean __ignoreItemEvents = false; // Used to ignore cascading events when working with choices

//private List<ReclamationHDB_SiteDataType> __siteDataTypeList = new Vector(); // Corresponds to displayed list

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
{	Object o = event.getSource();

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
Refresh the data type choices in response to the currently selected RiversideDB data store.
@param value if non-null, then the selection is from the command initialization, in which case the
specified data type should be selected
*/
private void actionPerformedDataStoreSelected ( )
{
    if ( __DataStore_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    __dataStore = getSelectedDataStore();
    __dmi = (RiversideDB_DMI)((DatabaseDataStore)__dataStore).getDMI();
    //Message.printStatus(2, "", "Selected data store " + __dataStore + " __dmi=" + __dmi );
    // Now populate the data type choices corresponding to the data store
    populateDataTypeChoices ( __dmi );
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
    // Now populate the data location choices corresponding to the data type, sub-type, and interval
    populateLocationChoices ( __dmi );
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
	PropList parameters = new PropList ( "" );
    String DataStore = __DataStore_JComboBox.getSelected();
    String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String Location = __Location_JComboBox.getSelected();
    String DataSource = __DataSource_JComboBox.getSelected();
    String DataType = __DataType_JComboBox.getSelected();
    if ( DataType != null ) {
        if ( DataType.indexOf(" ") > 0 ) {
            DataType = StringUtil.getToken(DataType," ",0,0).trim();
        }
        else {
            DataType = DataType.trim();
        }
    }
    String DataSubType = __DataSubType_JComboBox.getSelected();
    String Interval = __Interval_JComboBox.getSelected();
    String Scenario = __Scenario_JComboBox.getSelected();
    String SequenceNumber = __SequenceNumber_JComboBox.getSelected();
    String CopyDataFlags = __CopyDataFlags_JComboBox.getSelected();
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
    if ( (Location != null) && (Location.length() > 0) ) {
        parameters.set ( "Location", Location );
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
    if ( (CopyDataFlags != null) && (CopyDataFlags.length() > 0) ) {
        parameters.set ( "CopyDataFlags", CopyDataFlags );
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
    String Location = __Location_JComboBox.getSelected();
    String DataSource = __DataSource_JComboBox.getSelected();
    String DataType = __DataType_JComboBox.getSelected();
    if ( DataType != null ) {
        if ( DataType.indexOf(" ") > 0 ) {
            DataType = StringUtil.getToken(DataType," ",0,0).trim();
        }
        else {
            DataType = DataType.trim();
        }
    }
    String DataSubType = __DataSubType_JComboBox.getSelected();
    String Interval = __Interval_JComboBox.getSelected();
    String Scenario = __Scenario_JComboBox.getSelected();
    String SequenceNumber = __SequenceNumber_JComboBox.getSelected();
    String CopyDataFlags = __CopyDataFlags_JComboBox.getSelected();
	String OutputStart = __OutputStart_JTextField.getText().trim();
	String OutputEnd = __OutputEnd_JTextField.getText().trim();
	__command.setCommandParameter ( "DataStore", DataStore );
	__command.setCommandParameter ( "TSList", TSList );
    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
    __command.setCommandParameter ( "Location", Location );
    __command.setCommandParameter ( "DataSource", DataSource );
    __command.setCommandParameter ( "DataType", DataType );
    __command.setCommandParameter ( "DataSubType", DataSubType );
    __command.setCommandParameter ( "Interval", Interval );
    __command.setCommandParameter ( "Scenario", Scenario );
    __command.setCommandParameter ( "SequenceNumber", SequenceNumber );
    __command.setCommandParameter ( "CopyDataFlags", CopyDataFlags );
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
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "<html><b>This command currently will only write one time series.  " +
        "Use parameters to match a time series in the database.</b></html>." ),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Use the parameters to match a specific time series; " +
		"choices will be updated based on selections above a specific parameter." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "TSTool will only write time series records.  TSTool will not write records for " +
        "time series metadata (the time series must have been previously defined)."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Enter output date/times to a " +
		"precision appropriate for output time series."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // List available data stores of the correct type
    // Other lists are NOT populated until a data store is selected (driven by events)
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data store:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStore_JComboBox = new SimpleJComboBox ( false );
    List<DataStore> dataStoreList = ((TSCommandProcessor)processor).getDataStoresByType(
        RiversideDBDataStore.class );
    for ( DataStore dataStore: dataStoreList ) {
        __DataStore_JComboBox.addItem ( dataStore.getName() );
    }
    if ( __DataStore_JComboBox.getItemCount() > 0 ) {
        __DataStore_JComboBox.select ( 0 );
    }
    __DataStore_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __DataStore_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - open data store for HDB database."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    __TSList_JComboBox = new SimpleJComboBox(false);
    y = CommandEditorUtil.addTSListToEditorDialogPanel ( this, main_JPanel, __TSList_JComboBox, y );

    __TSID_JLabel = new JLabel ("TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __TSID_JComboBox = new SimpleJComboBox ( true );  // Allow edits
    List tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, main_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, y );
    
    __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
        this, this, main_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, y );
  
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Data type:"), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataType_JComboBox = new SimpleJComboBox (false);
    __DataType_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(main_JPanel, __DataType_JComboBox,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Required - matching (main) data type in the database."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Data sub-type:"), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataSubType_JComboBox = new SimpleJComboBox (false);
    __DataSubType_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(main_JPanel, __DataSubType_JComboBox,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Required - matching (sub) data type in the database (may be blank)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Data interval:"), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Interval_JComboBox = new SimpleJComboBox (false);
    __Interval_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(main_JPanel, __Interval_JComboBox,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Required - matching data interval in the database."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Put the following after data type and interval so that the list of locations is reasonably short
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Location (station/area) ID:"), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Location_JComboBox = new SimpleJComboBox (false);
    __Location_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(main_JPanel, __Location_JComboBox,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Required - matching location ID in the database."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Data source abbreviation:"), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataSource_JComboBox = new SimpleJComboBox (false);
    __DataSource_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(main_JPanel, __DataSource_JComboBox,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Required - matching data source in the database."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Scenario:"), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Scenario_JComboBox = new SimpleJComboBox ( false );
    __Scenario_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(main_JPanel, __Scenario_JComboBox,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Required - matching scenario in the database (may be blank)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Sequence number:"), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SequenceNumber_JComboBox = new SimpleJComboBox ( false );
    __SequenceNumber_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(main_JPanel, __SequenceNumber_JComboBox,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Required - for ensembles, the sequence number (trace starting year)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Copy data flags?:"), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CopyDataFlags_JComboBox = new SimpleJComboBox ( false );
    __CopyDataFlags_JComboBox.addItemListener (this);
    List<String> copyDataFlagsList = new Vector();
    copyDataFlagsList.add("");
    copyDataFlagsList.add(__command._False);
    copyDataFlagsList.add(__command._True);
    __CopyDataFlags_JComboBox.setData ( copyDataFlagsList );
    __CopyDataFlags_JComboBox.select(0);
    JGUIUtil.addComponent(main_JPanel, __CopyDataFlags_JComboBox,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - should data flags be copied? (default=" + __command._True + ")."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Output start:"), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OutputStart_JTextField = new JTextField (20);
	__OutputStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __OutputStart_JTextField,
		1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - override the global output start (default=write all data)."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Output end:"), 
		0, ++y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OutputEnd_JTextField = new JTextField (20);
	__OutputEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __OutputEnd_JTextField,
		1, y, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - override the global output end (default=write all data)."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
    		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __command_JTextArea = new JTextArea ( 4, 50 );
    __command_JTextArea.setLineWrap ( true );
    __command_JTextArea.setWrapStyleWord ( true );
    __command_JTextArea.setEditable ( false );
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
    		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // All of the components have been initialized above but now generate an event to populate...
    if ( __DataStore_JComboBox.getItemCount() > 0 ) {
        __DataStore_JComboBox.select(null);
        __DataStore_JComboBox.select(0);
    }

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	__cancel_JButton = new SimpleJButton("Cancel", "Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__ok_JButton = new SimpleJButton("OK", "OK", this);
	button_JPanel.add ( __ok_JButton );

	setTitle ( "Edit " + __command.getCommandName() + "() Command" );
	
	// Refresh the contents...
    checkGUIState();
    refresh ();
    
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
{   checkGUIState();

    Object source = e.getSource();
    int sc = e.getStateChange();
    if ( !__ignoreItemEvents ) {
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
            // User has selected a data type.
            actionPerformedIntervalSelected ();
        }
    }
 
	refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	int code = event.getKeyCode();

	if ( code == KeyEvent.VK_ENTER ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
}

public void keyReleased ( KeyEvent event )
{	refresh();
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
Populate the data sub-type choices for the given data type.
*/
private void populateDataSubTypeChoices ( RiversideDB_DMI dmi )
{   String routine = getClass().getName() + ".populateDataSubTypeChoices";
    if ( (__DataType_JComboBox == null) || (__DataSubType_JComboBox == null) ) {
        // Still in initialization
        return;
    }
    String dataType = __DataType_JComboBox.getSelected();
    if ( dataType == null ) {
        return;
    }
    dataType = StringUtil.getToken(dataType," ",0,0).trim();
    List<RiversideDB_MeasType> measTypeList = null;
    try {
        measTypeList = dmi.readMeasTypeListForData_type ( dataType );
    }
    catch ( Exception e ) {
        Message.printWarning(2, routine, "Error getting MeasType list from RiversideDB \"" +
            __dataStore.getName() + " and data type \"" + dataType + "\" (" + e + ").");
        Message.printWarning(2, routine, e);
        measTypeList = null;
    }
    __DataSubType_JComboBox.removeAll ();
    List<String> dataSubTypeList = new Vector();
    String dataSubType;
    for ( RiversideDB_MeasType measType : measTypeList ) {
        // Only add if not already listed. Alternatively - add a "distinct" query
        dataSubType = measType.getSub_type();
        if ( dataSubType == null ) {
            dataSubType = "";
        }
        else {
            dataSubType = dataSubType.trim();
        }
        if ( StringUtil.indexOfIgnoreCase(dataSubTypeList, dataSubType) < 0 ) {
            dataSubTypeList.add(dataSubType);
        }
    }
    java.util.Collections.sort(dataSubTypeList);
    __DataSubType_JComboBox.setData(dataSubTypeList);
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
{   String routine = getClass().getName() + "populateDataTypeChoices";
    if ( __DataType_JComboBox == null ) {
        return;
    }
    __DataType_JComboBox.removeAll ();
    List<RiversideDB_MeasType> measTypeList = null;
    List<RiversideDB_DataType> dataTypeList = null;
    try {
        if ( rdmi == null ) {
            Message.printStatus(2, routine, "RiversideDB_DMI is null." );
            return; // Nothing selected
        }
        measTypeList = rdmi.readMeasTypeListForDistinctData_type();
        dataTypeList = rdmi.readDataTypeList();
    }
    catch ( Exception e ) {
        Message.printWarning ( 1, routine, "Error getting time series for data type choices (" + e + ")." );
        Message.printWarning ( 3, routine, e );
        Message.printWarning ( 3, routine, rdmi.getLastSQLString() );
        measTypeList = null;
    }
    int size = 0;
    if ( measTypeList != null ) {
        size = measTypeList.size();
    }
    int dataTypeLengthMax = 80;
    if ( size > 0 ) {
        RiversideDB_MeasType mt = null;
        int pos;
        String data_type;
        for ( int i = 0; i < size; i++ ) {
            mt = measTypeList.get(i);
            pos = RiversideDB_DataType.indexOf (dataTypeList, mt.getData_type() );
            if ( pos < 0 ) {
                __DataType_JComboBox.add(mt.getData_type() );
            }
            else {
                data_type = mt.getData_type() + " - " + dataTypeList.get(pos).getDescription();
                if ( data_type.length() > dataTypeLengthMax ) {
                    __DataType_JComboBox.add( data_type.substring(0,dataTypeLengthMax) + "..." );
                }
                else {
                    __DataType_JComboBox.add( data_type );
                }
            }
        }
        // Select first choice (may get reset from existing parameter values).
        __DataType_JComboBox.select ( null );
        if ( __DataType_JComboBox.getItemCount() > 0 ) {
            __DataType_JComboBox.select ( 0 );
        }
    }
}

/**
Populate the interval choices.
*/
private void populateIntervalChoices ( RiversideDB_DMI dmi )
{   String routine = getClass().getName() + ".populateIntervalChoices";
    if ( (__DataType_JComboBox == null) || (__DataSubType_JComboBox == null) ) {
        // Initialization
        return;
    }
    String dataType = __DataType_JComboBox.getSelected();
    if ( dataType == null ) {
        return;
    }
    dataType = StringUtil.getToken(dataType," ",0,0).trim();
    String dataSubType = __DataSubType_JComboBox.getSelected();
    if ( dataSubType == null ) {
        return;
    }
    dataSubType = dataSubType.trim();
    List<RiversideDB_MeasType> measTypeList = null;
    try {
        if ( dataSubType.equals("") ) {
            measTypeList = dmi.readMeasTypeListForTSIdent ( ".." + dataType + ".." );
        }
        else {
            measTypeList = dmi.readMeasTypeListForTSIdent ( ".." + dataType + "-" + dataSubType+ ".." );
        }
    }
    catch ( Exception e ) {
        Message.printWarning(2, routine, "Error getting MeasTypes from RiversideDB \"" +
            __dataStore.getName() + "(" + e + ").");
        Message.printWarning(2, routine, e);
        measTypeList = null;
    }
    int size = 0;
    if ( measTypeList != null ) {
        size = measTypeList.size();
    }
    RiversideDB_MeasType mt = null;
    String timestep;
    String time_step_base;
    long time_step_mult;
    __Interval_JComboBox.removeAll ();
    for ( int i = 0; i < size; i++ ) {
        mt = measTypeList.get(i);
        // Only add if not already listed. Alternatively - need to enable a "distinct" query
        time_step_base = mt.getTime_step_base();
        time_step_mult = mt.getTime_step_mult();
        if ( time_step_base.equalsIgnoreCase( "IRREGULAR") || DMIUtil.isMissing(time_step_mult) ) {
            timestep = mt.getTime_step_base();
        }
        else {
            timestep = "" + mt.getTime_step_mult() + mt.getTime_step_base();
        }
        if ( !JGUIUtil.isSimpleJComboBoxItem(__Interval_JComboBox, timestep, JGUIUtil.NONE, null, null)){
            __Interval_JComboBox.add(timestep);
        }
    }
    // Select first choice (may get reset from existing parameter values).
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
    if ( (__DataType_JComboBox == null) || (__DataSubType_JComboBox == null) || (__Interval_JComboBox == null) ) {
        // Initialization
        return;
    }
    String dataType = __DataType_JComboBox.getSelected();
    if ( dataType == null ) {
        return;
    }
    dataType = StringUtil.getToken(dataType," ",0,0).trim();
    String dataSubType = __DataSubType_JComboBox.getSelected();
    if ( dataSubType == null ) {
        return;
    }
    dataSubType = dataSubType.trim();
    String interval = __Interval_JComboBox.getSelected();
    if ( interval == null ) {
        return;
    }
    interval = interval.trim();
    List<RiversideDB_MeasType> measTypeList = null;
    String tsid = "";
    try {
        if ( dataSubType.equals("") ) {
            tsid = ".." + dataType + "." + interval + ".";
        }
        else {
            tsid = ".." + dataType + "-" + dataSubType+ "." + interval + ".";
        }
        measTypeList = dmi.readMeasTypeListForTSIdent ( tsid );
    }
    catch ( Exception e ) {
        Message.printWarning(2, routine, "Error getting MeasTypes from RiversideDB \"" +
            __dataStore.getName() + " tsid=\"" + tsid + "\" (" + e + ").");
        Message.printWarning(2, routine, e);
        measTypeList = null;
    }
    Message.printStatus(2, routine, "Got " + measTypeList.size() + " MeasType using tsid=\"" + tsid + "\"." );
    __Location_JComboBox.removeAll ();
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
    // Select first choice (may get reset from existing parameter values).
    __Location_JComboBox.select ( null );
    __Location_JComboBox.setData ( locationList );
    if ( __Location_JComboBox.getItemCount() > 0 ){
        __Location_JComboBox.select ( 0 );
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
    String Location = "";
    String DataType = "";
    String DataSource = "";
    String DataSubType = "";
    String Interval = "";
    String Scenario = "";
    String SequenceNumber = "";
    String CopyDataFlags = "";
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
        Location = parameters.getValue ( "Location" );
        DataSource = parameters.getValue ( "DataSource" );
        DataType = parameters.getValue ( "DataType" );
        DataSubType = parameters.getValue ( "DataSubType" );
        Interval = parameters.getValue ( "Interval" );
        Scenario = parameters.getValue ( "Scenario" );
        SequenceNumber = parameters.getValue ( "SequenceNumber" );
        CopyDataFlags = parameters.getValue ( "CopyDataFlags" );
		OutputStart = parameters.getValue ( "OutputStart" );
		OutputEnd = parameters.getValue ( "OutputEnd" );
        if ( JGUIUtil.isSimpleJComboBoxItem(__DataStore_JComboBox, DataStore, JGUIUtil.NONE, null, null ) ) {
            __DataStore_JComboBox.select ( DataStore );
        }
        else {
            if ( (DataStore == null) || DataStore.equals("") ) {
                // New command...select the default...
                if ( __DataStore_JComboBox.getItemCount() > 0 ) {
                    __DataStore_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
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
        populateDataTypeChoices(getRiversideDB_DMI() );
        // Then set to the initial value.  Parameter will be like "QIN" but choice will be like "QIN - Description"
        if ( JGUIUtil.isSimpleJComboBoxItem(__DataType_JComboBox, DataType, JGUIUtil.NONE, null, null ) ) {
            __DataType_JComboBox.select ( DataType );
        }
        else if ( (DataType != null) && (DataType.indexOf(" ") > 0) &&
            JGUIUtil.isSimpleJComboBoxItem( __DataType_JComboBox,
            DataType, StringUtil.DELIM_ALLOW_STRINGS, " ", 0, null, true) ) {
            __DataType_JComboBox.select ( DataType );
        }
        else {
            if ( (DataType == null) || DataType.equals("") ) {
                // New command...select the default...
                if ( __DataType_JComboBox.getItemCount() > 0 ) {
                    __DataType_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "DataType parameter \"" + DataType + "\".  Select a different value or Cancel." );
            }
        }
        // First populate the choices...
        // TODO SAM 2012-02-18 Need to enable
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
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
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
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "Interval parameter \"" + Interval + "\".  Select a different value or Cancel." );
            }
        }
        // First populate the choices...
        populateLocationChoices(getRiversideDB_DMI() );
        // Then set to the initial value
        if ( JGUIUtil.isSimpleJComboBoxItem(__Location_JComboBox, Location, JGUIUtil.NONE, null, null ) ) {
            __Location_JComboBox.select ( Location );
        }
        else {
            if ( (Location == null) || Location.equals("") ) {
                // New command...select the default...
                if ( __Location_JComboBox.getItemCount() > 0 ) {
                    __Location_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "Location parameter \"" + Location + "\".  Select a different value or Cancel." );
            }
        }
        // First populate the choices...
        // TODO SAM 2012-02-18 Need to enable
        //populateDataSourceChoices(getRiversideDB_DMI() );
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
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "DataSource parameter \"" + DataSource + "\".  Select a different value or Cancel." );
            }
        }
        // First populate the choices...
        // TODO SAM 2012-02-18 Need to enable
        //populateScenarioChoices(getRiversideDB_DMI() );
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
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "Scenario parameter \"" + Scenario + "\".  Select a different value or Cancel." );
            }
        }
        // First populate the choices...
        // TODO SAM 2012-02-18 Need to enable
        //populateSequenceNumberChoices(getRiversideDB_DMI() );
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
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "SequenceNumber parameter \"" + SequenceNumber + "\".  Select a different value or Cancel." );
            }
        }
        if ( JGUIUtil.isSimpleJComboBoxItem(__CopyDataFlags_JComboBox, CopyDataFlags, JGUIUtil.NONE, null, null ) ) {
            __CopyDataFlags_JComboBox.select ( CopyDataFlags );
        }
        else {
            if ( (CopyDataFlags == null) || CopyDataFlags.equals("") ) {
                // New command...select the default...
                if ( __CopyDataFlags_JComboBox.getItemCount() > 0 ) {
                    __CopyDataFlags_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "CopyDataFlags parameter \"" + CopyDataFlags + "\".  Select a different value or Cancel." );
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
    // FIXME SAM 2011-10-03 Might be able to remove check for null if events and list population are
    // implemented correctly
    DataType = __DataType_JComboBox.getSelected();
    if ( DataType == null ) {
        DataType = "";
    }
    else {
        if ( DataType.indexOf(" ") > 0 ) {
            DataType = StringUtil.getToken(DataType," ",0,0).trim();
        }
        else {
            DataType = DataType.trim();
        }
    }
    DataSubType = __DataSubType_JComboBox.getSelected();
    if ( DataSubType == null ) {
        DataSubType = "";
    }
    Interval = __Interval_JComboBox.getSelected();
    if ( Interval == null ) {
        Interval = "";
    }
    Location = __Location_JComboBox.getSelected();
    if ( Location == null ) {
        Location = "";
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
    CopyDataFlags = __CopyDataFlags_JComboBox.getSelected();
    if ( CopyDataFlags == null ) {
        CopyDataFlags = "";
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
    parameters.add ( "Location=" + Location );
    parameters.add ( "DataSource=" + DataSource );
    parameters.add ( "Scenario=" + Scenario );
    parameters.add ( "SequenceNumber=" + SequenceNumber );
    parameters.add ( "CopyDataFlags=" + CopyDataFlags );
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
Set the HDB site list corresponding to the displayed list.
*/
private void setSiteDataTypeList ( )//List<ReclamationHDB_SiteDataType> siteDataTypeList )
{
    //__siteDataTypeList = siteDataTypeList;
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

private Object makeSiteListObject ( final String item )  {
    return new Object() {
        public String toString() { return item; } };
  }

}