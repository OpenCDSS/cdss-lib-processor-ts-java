package rti.tscommandprocessor.commands.datastore;

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

import java.util.List;
import java.util.Vector;

import riverside.datastore.DataStore;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;

import RTi.DMI.DMIWriteModeType;
import RTi.DMI.DatabaseDataStore;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Command editor dialog for the WriteTimeSeriesToDataStore() command.
*/
public class WriteTimeSeriesToDataStore_JDialog extends JDialog
implements ActionListener, KeyListener, ItemListener, WindowListener
{

private SimpleJButton __cancel_JButton = null;;
private SimpleJButton __ok_JButton = null;
private WriteTimeSeriesToDataStore_Command __command = null;
private JTextArea __command_JTextArea=null;
private JTextField __OutputStart_JTextField = null;
private JTextField __OutputEnd_JTextField = null;
private SimpleJComboBox	__TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox __TSID_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private SimpleJComboBox __DataStore_JComboBox = null;
private JTextField __DataStoreLocationType_JTextField = null;
private JTextField __DataStoreLocationID_JTextField = null;
private JTextField __DataStoreDataSource_JTextField = null;
private JTextField __DataStoreDataType_JTextField = null;
private JTextField __DataStoreScenario_JTextField = null;
private JTextField __DataStoreInterval_JTextField = null;
private JTextField __DataStoreUnits_JTextField = null;
private JTextField __DataStoreMissingValue_JTextField = null;
private SimpleJComboBox __WriteMode_JComboBox = null;
private boolean __error_wait = false;	// Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false;		// Has user pressed OK to close the dialog.
private DatabaseDataStore __dataStore = null; // selected data store

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public WriteTimeSeriesToDataStore_JDialog (	JFrame parent, WriteTimeSeriesToDataStore_Command command )
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
	String OutputStart = __OutputStart_JTextField.getText().trim();
	String OutputEnd = __OutputEnd_JTextField.getText().trim();
	String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String DataStore = __DataStore_JComboBox.getSelected();
    String DataStoreLocationType = __DataStoreLocationType_JTextField.getText().trim();
    String DataStoreLocationID = __DataStoreLocationID_JTextField.getText().trim();
    String DataStoreDataSource = __DataStoreDataSource_JTextField.getText().trim();
    String DataStoreDataType = __DataStoreDataType_JTextField.getText().trim();
    String DataStoreScenario = __DataStoreScenario_JTextField.getText().trim();
    String DataStoreInterval = __DataStoreInterval_JTextField.getText().trim();
    String DataStoreUnits = __DataStoreUnits_JTextField.getText().trim();
    String DataStoreMissingValue = __DataStoreMissingValue_JTextField.getText().trim();
    String WriteMode = __WriteMode_JComboBox.getSelected();

	__error_wait = false;
	
	if ( TSList.length() > 0 ) {
		parameters.set ( "TSList", TSList );
	}
    if ( TSID.length() > 0 ) {
        parameters.set ( "TSID", TSID );
    }
    if ( EnsembleID.length() > 0 ) {
        parameters.set ( "EnsembleID", EnsembleID );
    }
	if ( OutputStart.length() > 0 ) {
		parameters.set ( "OutputStart", OutputStart );
	}
	if ( OutputEnd.length() > 0 ) {
		parameters.set ( "OutputEnd", OutputEnd );
	}
    if ( (DataStore != null) && DataStore.length() > 0 ) {
        parameters.set ( "DataStore", DataStore );
        __dataStore = getSelectedDataStore();
    }
    else {
        parameters.set ( "DataStore", "" );
    }
    if ( DataStoreLocationID.length() > 0 ) {
        parameters.set ( "DataStoreLocationID", DataStoreLocationID );
    }
    if ( DataStoreLocationType.length() > 0 ) {
        parameters.set ( "DataStoreLocationType", DataStoreLocationType );
    }
    if ( DataStoreDataSource.length() > 0 ) {
        parameters.set ( "DataStoreDataSource", DataStoreDataSource );
    }
    if ( DataStoreDataType.length() > 0 ) {
        parameters.set ( "DataStoreDataType", DataStoreDataType );
    }
    if ( DataStoreScenario.length() > 0 ) {
        parameters.set ( "DataStoreScenario", DataStoreScenario );
    }
    if ( DataStoreInterval.length() > 0 ) {
        parameters.set ( "DataStoreInterval", DataStoreInterval );
    }
    if ( DataStoreUnits.length() > 0 ) {
        parameters.set ( "DataStoreUnits", DataStoreUnits );
    }
    if ( DataStoreMissingValue.length() > 0 ) {
        parameters.set ( "DataStoreMissingValue", DataStoreMissingValue );
    }
    if ( WriteMode.length() > 0 ) {
        parameters.set ( "WriteMode", WriteMode );
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
{	String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();  
	String OutputStart = __OutputStart_JTextField.getText().trim();
	String OutputEnd = __OutputEnd_JTextField.getText().trim();
    String DataStore = __DataStore_JComboBox.getSelected();
    String DataStoreLocationType = __DataStoreLocationType_JTextField.getText().trim();
    String DataStoreLocationID = __DataStoreLocationID_JTextField.getText().trim();
    String DataStoreDataSource = __DataStoreDataSource_JTextField.getText().trim();
    String DataStoreDataType = __DataStoreDataType_JTextField.getText().trim();
    String DataStoreScenario = __DataStoreScenario_JTextField.getText().trim();
    String DataStoreInterval = __DataStoreInterval_JTextField.getText().trim();
    String DataStoreUnits = __DataStoreUnits_JTextField.getText().trim();
    String DataStoreMissingValue = __DataStoreMissingValue_JTextField.getText().trim();
    String WriteMode = __WriteMode_JComboBox.getSelected();
	__command.setCommandParameter ( "TSList", TSList );
    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
	__command.setCommandParameter ( "OutputStart", OutputStart );
	__command.setCommandParameter ( "OutputEnd", OutputEnd );
    __command.setCommandParameter ( "DataStore", DataStore );
    __command.setCommandParameter ( "DataStoreLocationType", DataStoreLocationType );
    __command.setCommandParameter ( "DataStoreLocationID", DataStoreLocationID );
    __command.setCommandParameter ( "DataStoreDataSource", DataStoreDataSource );
    __command.setCommandParameter ( "DataStoreDataType", DataStoreDataType );
    __command.setCommandParameter ( "DataStoreScenario", DataStoreScenario );
    __command.setCommandParameter ( "DataStoreInterval", DataStoreInterval );
    __command.setCommandParameter ( "DataStoreUnits", DataStoreUnits );
    __command.setCommandParameter ( "DataStoreMissingValue", DataStoreMissingValue );
    __command.setCommandParameter ( "WriteMode", WriteMode );
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
Get the selected data store.
*/
private DatabaseDataStore getSelectedDataStore ()
{   String routine = getClass().getName() + ".getSelectedDataStore";
    String DataStore = __DataStore_JComboBox.getSelected();
    DatabaseDataStore dataStore = (DatabaseDataStore)((TSCommandProcessor)
        __command.getCommandProcessor()).getDataStoreForName( DataStore, DatabaseDataStore.class );
    if ( dataStore != null ) {
        Message.printStatus(2, routine, "Selected data store is \"" + dataStore.getName() + "\"." );
    }
    else {
        Message.printStatus(2, routine, "Cannot get data store for \"" + DataStore + "\"." );
    }
    return dataStore;
}

/**
Instantiates the GUI components.
@param parent Frame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, WriteTimeSeriesToDataStore_Command command )
{	__command = command;
    CommandProcessor processor = __command.getCommandProcessor();
	addWindowListener( this );

    Insets insetsTLBR = new Insets(1,2,1,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Write time series to a database datastore," +
		" where time series to database table mapping is defined in the datastore configuration." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Currently the choices do not cascade.  In the future, cascading set of choices may be implemented to help with writing " +
        "single time series." ),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Enter date/times to a precision appropriate for output time series."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JPanel ts_JPanel = new JPanel ();
    ts_JPanel.setLayout( new GridBagLayout() );
    int yTS = -1;
    ts_JPanel.setBorder(
        BorderFactory.createTitledBorder ( BorderFactory.createLineBorder(Color.black),
        "Specify time series information" ));
    JGUIUtil.addComponent(main_JPanel, ts_JPanel,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    __TSList_JComboBox = new SimpleJComboBox(false);
    yTS = CommandEditorUtil.addTSListToEditorDialogPanel ( this, ts_JPanel, __TSList_JComboBox, yTS );

    __TSID_JLabel = new JLabel ("TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __TSID_JComboBox = new SimpleJComboBox ( true );  // Allow edits
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    yTS = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, ts_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, yTS);
    
    __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    yTS = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
        this, this, ts_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, yTS );
   
    JGUIUtil.addComponent(ts_JPanel, new JLabel ("Output start:"), 
		0, ++yTS, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OutputStart_JTextField = new JTextField (20);
	__OutputStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(ts_JPanel, __OutputStart_JTextField,
		1, yTS, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel (
		"Optional - override the global output start (default=write all data)."),
		3, yTS, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Output end:"), 
		0, ++yTS, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OutputEnd_JTextField = new JTextField (20);
	__OutputEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(ts_JPanel, __OutputEnd_JTextField,
		1, yTS, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel (
		"Optional - override the global output end (default=write all data)."),
		3, yTS, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JPanel ds_JPanel = new JPanel ();
    ds_JPanel.setLayout( new GridBagLayout() );
    int yDS = -1;
    ds_JPanel.setBorder(
        BorderFactory.createTitledBorder ( BorderFactory.createLineBorder(Color.black),
        "Specify datastore information" ));
    JGUIUtil.addComponent(main_JPanel, ds_JPanel,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // List available datastores of the correct type
    
    JGUIUtil.addComponent(ds_JPanel, new JLabel ( "Datastore:"),
        0, ++yDS, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStore_JComboBox = new SimpleJComboBox ( false );
    TSCommandProcessor tsProcessor = (TSCommandProcessor)processor;
    List<DataStore> dataStoreList = tsProcessor.getDataStoresByType( DatabaseDataStore.class );
    for ( DataStore dataStore: dataStoreList ) {
        __DataStore_JComboBox.addItem ( dataStore.getName() );
    }
    if ( dataStoreList.size() == 0 ) {
        // Add an empty item so users can at least bring up the editor
        __DataStore_JComboBox.addItem ( "" );
    }
    __DataStore_JComboBox.select ( 0 );
    __DataStore_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(ds_JPanel, __DataStore_JComboBox,
        1, yDS, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ds_JPanel, new JLabel("Required - database datastore to receive data."), 
        3, yDS, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(ds_JPanel, new JLabel ( "Datastore location type:"),
        0, ++yDS, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStoreLocationType_JTextField = new JTextField ( 10 );
    __DataStoreLocationType_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(ds_JPanel, __DataStoreLocationType_JTextField,
        1, yDS, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ds_JPanel, new JLabel("Optional - specify location type to use in datastore (default=time series location type)."), 
        3, yDS, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(ds_JPanel, new JLabel ( "Datastore location ID:"),
        0, ++yDS, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStoreLocationID_JTextField = new JTextField ( 10 );
    __DataStoreLocationID_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(ds_JPanel, __DataStoreLocationID_JTextField,
        1, yDS, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ds_JPanel, new JLabel("Optional - specify location identifier to use in datastore (default=time series location ID)."), 
        3, yDS, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(ds_JPanel, new JLabel ( "Datastore data source:"),
        0, ++yDS, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStoreDataSource_JTextField = new JTextField ( 10 );
    __DataStoreDataSource_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(ds_JPanel, __DataStoreDataSource_JTextField,
        1, yDS, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ds_JPanel, new JLabel("Optional - specify data source to use in datastore (default=time series data source)."), 
        3, yDS, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(ds_JPanel, new JLabel ( "Datastore data type:"),
        0, ++yDS, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStoreDataType_JTextField = new JTextField ( 10 );
    __DataStoreDataType_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(ds_JPanel, __DataStoreDataType_JTextField,
        1, yDS, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ds_JPanel, new JLabel("Optional - specify data type to use in datastore (default=time series data type)."), 
        3, yDS, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(ds_JPanel, new JLabel ( "Datastore interval:"),
        0, ++yDS, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStoreInterval_JTextField = new JTextField ( 10 );
    __DataStoreInterval_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(ds_JPanel, __DataStoreInterval_JTextField,
        1, yDS, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ds_JPanel, new JLabel("Optional - specify data interval to use in datastore (default=time series data interval)."), 
        3, yDS, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(ds_JPanel, new JLabel ( "Datastore scenario:"),
        0, ++yDS, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStoreScenario_JTextField = new JTextField ( 10 );
    __DataStoreScenario_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(ds_JPanel, __DataStoreScenario_JTextField,
        1, yDS, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ds_JPanel, new JLabel("Optional - specify scenario to use in datastore (default=time series scenario)."), 
        3, yDS, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(ds_JPanel, new JLabel ( "Datastore missing value:" ),
        0, ++yDS, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStoreMissingValue_JTextField = new JTextField ( "", 20 );
    __DataStoreMissingValue_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(ds_JPanel, __DataStoreMissingValue_JTextField,
        1, yDS, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ds_JPanel, new JLabel (
        "Optional - value to write for missing data (default=time series missing value)."),
        3, yDS, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(ds_JPanel, new JLabel ( "Datastore units:"),
        0, ++yDS, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStoreUnits_JTextField = new JTextField ( 10 );
    __DataStoreUnits_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(ds_JPanel, __DataStoreUnits_JTextField,
        1, yDS, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ds_JPanel, new JLabel("Optional - specify units to use in datastore (default=time series units)."), 
        3, yDS, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // How to write the data...
    
    JGUIUtil.addComponent(ds_JPanel, new JLabel ( "Write mode:"),
        0, ++yDS, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __WriteMode_JComboBox = new SimpleJComboBox ( false );
    List<String> writeModeChoices = new Vector<String>();
    writeModeChoices.add ( "" );
    writeModeChoices.add ( "" + __command._DeleteAllThenInsert );
    writeModeChoices.add ( "" + __command._DeletePeriodThenInsert );
    writeModeChoices.add ( "" + DMIWriteModeType.DELETE_INSERT );
    writeModeChoices.add ( "" + DMIWriteModeType.INSERT );
    writeModeChoices.add ( "" + DMIWriteModeType.INSERT_UPDATE );
    writeModeChoices.add ( "" + DMIWriteModeType.UPDATE );
    writeModeChoices.add ( "" + DMIWriteModeType.UPDATE_INSERT );
    __WriteMode_JComboBox.setData(writeModeChoices);
    __WriteMode_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(ds_JPanel, __WriteMode_JComboBox,
        1, yDS, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ds_JPanel, new JLabel("Optional - how to write data records (default=" +
        DMIWriteModeType.INSERT_UPDATE + ")."), 
        3, yDS, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
    		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __command_JTextArea = new JTextArea ( 4, 50 );
    __command_JTextArea.setLineWrap ( true );
    __command_JTextArea.setWrapStyleWord ( true );
    __command_JTextArea.setEditable ( false );
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
    		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

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
    
	setResizable ( true );
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
    if ( (e.getSource() == __DataStore_JComboBox) && (e.getStateChange() == ItemEvent.SELECTED) ) {
        // User has selected a data store.
        actionPerformedDataStoreSelected ();
    }
    checkGUIState();
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
Refresh the command from the other text field contents.
*/
private void refresh ()
{	String routine = "WriteTimeSeriesToDataStore_JDialog.refresh";
	String OutputStart = "";
	String OutputEnd = "";
	String TSList = "";
    String TSID = "";
    String EnsembleID = "";
    String DataStore = "";
    String DataStoreLocationType = "";
    String DataStoreLocationID = "";
    String DataStoreDataSource = "";
    String DataStoreDataType = "";
    String DataStoreInterval = "";
    String DataStoreScenario = "";
    String DataStoreUnits = "";
    String DataStoreMissingValue = "";
    String WriteMode = "";
	__error_wait = false;
	PropList parameters = null;
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		parameters = __command.getCommandParameters();
		OutputStart = parameters.getValue ( "OutputStart" );
		OutputEnd = parameters.getValue ( "OutputEnd" );
		TSList = parameters.getValue ( "TSList" );
        TSID = parameters.getValue ( "TSID" );
        EnsembleID = parameters.getValue ( "EnsembleID" );
        DataStore = parameters.getValue ( "DataStore" );
        DataStoreLocationID = parameters.getValue ( "DataStoreLocationID" );
        DataStoreLocationType = parameters.getValue ( "DataStoreLocationType" );
        DataStoreDataSource = parameters.getValue ( "DataStoreDataSource" );
        DataStoreDataType = parameters.getValue ( "DataStoreDataType" );
        DataStoreInterval = parameters.getValue ( "DataStoreInterval" );
        DataStoreScenario = parameters.getValue ( "DataStoreScenario" );
        DataStoreUnits = parameters.getValue ( "DataStoreUnits" );
        DataStoreMissingValue = parameters.getValue("DataStoreMissingValue");
        WriteMode = parameters.getValue ( "WriteMode" );
		if ( OutputStart != null ) {
			__OutputStart_JTextField.setText (OutputStart);
		}
		if ( OutputEnd != null ) {
			__OutputEnd_JTextField.setText (OutputEnd);
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
        // The data store list is set up in initialize() but is selected here
        if ( JGUIUtil.isSimpleJComboBoxItem(__DataStore_JComboBox, DataStore, JGUIUtil.NONE, null, null ) ) {
            __DataStore_JComboBox.select ( null ); // To ensure that following causes an event
            __DataStore_JComboBox.select ( DataStore ); // This will trigger getting the DMI for use in the editor
        }
        else {
            if ( (DataStore == null) || DataStore.equals("") ) {
                // New command...select the default...
                __DataStore_JComboBox.select ( null ); // To ensure that following causes an event
                __DataStore_JComboBox.select ( 0 );
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "DataStore parameter \"" + DataStore + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        if ( DataStoreLocationType != null ) {
            __DataStoreLocationType_JTextField.setText (DataStoreLocationType);
        }
        if ( DataStoreLocationID != null ) {
            __DataStoreLocationID_JTextField.setText (DataStoreLocationID);
        }
        if ( DataStoreDataSource != null ) {
            __DataStoreDataSource_JTextField.setText (DataStoreDataSource);
        }
        if ( DataStoreDataType != null ) {
            __DataStoreDataType_JTextField.setText (DataStoreDataType);
        }
        if ( DataStoreInterval != null ) {
            __DataStoreInterval_JTextField.setText (DataStoreInterval);
        }
        if ( DataStoreScenario != null ) {
            __DataStoreScenario_JTextField.setText (DataStoreScenario);
        }
        if ( DataStoreUnits != null ) {
            __DataStoreUnits_JTextField.setText (DataStoreUnits);
        }
        if ( DataStoreMissingValue != null ) {
            __DataStoreMissingValue_JTextField.setText ( DataStoreMissingValue );
        }
        if ( WriteMode == null ) {
            // Select default...
            __WriteMode_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __WriteMode_JComboBox,WriteMode, JGUIUtil.NONE, null, null ) ) {
                __WriteMode_JComboBox.select ( WriteMode );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nWriteMode value \"" + WriteMode +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
	}
	// Regardless, reset the command from the fields...
	OutputStart = __OutputStart_JTextField.getText().trim();
	OutputEnd = __OutputEnd_JTextField.getText().trim();
	TSList = __TSList_JComboBox.getSelected();
    TSID = __TSID_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
    DataStore = __DataStore_JComboBox.getSelected();
    if ( DataStore == null ) {
        DataStore = "";
    }
    DataStoreLocationType = __DataStoreLocationType_JTextField.getText().trim();
    DataStoreLocationID = __DataStoreLocationID_JTextField.getText().trim();
    DataStoreDataSource = __DataStoreDataSource_JTextField.getText().trim();
    DataStoreDataType = __DataStoreDataType_JTextField.getText().trim();
    DataStoreInterval = __DataStoreInterval_JTextField.getText().trim();
    DataStoreScenario = __DataStoreScenario_JTextField.getText().trim();
    DataStoreUnits = __DataStoreUnits_JTextField.getText().trim();
    DataStoreMissingValue = __DataStoreMissingValue_JTextField.getText().trim();
    WriteMode = __WriteMode_JComboBox.getSelected();
	parameters = new PropList ( __command.getCommandName() );
	parameters.add ( "TSList=" + TSList );
    parameters.add ( "TSID=" + TSID );
    parameters.add ( "EnsembleID=" + EnsembleID );
	parameters.add ( "OutputStart=" + OutputStart );
	parameters.add ( "OutputEnd=" + OutputEnd );
	parameters.add ( "DataStore=" + DataStore );
	parameters.add ( "DataStoreLocationType=" + DataStoreLocationType );
	parameters.add ( "DataStoreLocationID=" + DataStoreLocationID );
	parameters.add ( "DataStoreDataSource=" + DataStoreDataSource );
	parameters.add ( "DataStoreDataType=" + DataStoreDataType );
	parameters.add ( "DataStoreInterval" + DataStoreInterval );
	parameters.add ( "DataStoreScenario=" + DataStoreScenario );
	parameters.add ( "DataStoreUnits=" + DataStoreUnits );
	parameters.add ( "DataStoreMissingValue=" + DataStoreMissingValue );
	parameters.add ( "WriteMode=" + WriteMode );
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