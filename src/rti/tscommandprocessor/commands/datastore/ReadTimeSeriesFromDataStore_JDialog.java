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
import java.util.List;
import java.util.Vector;

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

import riverside.datastore.DataStore;
import riverside.datastore.GenericDatabaseDataStore;
import riverside.datastore.GenericDatabaseDataStore_TimeSeries_InputFilter_JPanel;
import rti.tscommandprocessor.core.TSCommandProcessor;

import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.Util.GUI.InputFilter_JPanel;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;

/**
Editor for the ReadTimeSeriesFromDataStore() command.
*/
public class ReadTimeSeriesFromDataStore_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private ReadTimeSeriesFromDataStore_Command __command = null;
private SimpleJComboBox __DataStore_JComboBox = null;
private SimpleJComboBox __DataType_JComboBox = null;
private SimpleJComboBox __Interval_JComboBox = null;
private SimpleJComboBox __LocationType_JComboBox;
private SimpleJComboBox __LocationID_JComboBox;
private SimpleJComboBox __DataSource_JComboBox;
private SimpleJComboBox __Scenario_JComboBox;
private JTextField __InputStart_JTextField;
private JTextField __InputEnd_JTextField;
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
private JTabbedPane __tsInfo_JTabbedPane = null;
			
private JTextArea __command_JTextArea = null; // Command as JTextArea
private InputFilter_JPanel __inputFilter_JPanel =null;
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK was pressed when closing the dialog.
private boolean __ignoreEvents = false; // Used to ignore cascading events when initializing the components

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public ReadTimeSeriesFromDataStore_JDialog ( JFrame parent, ReadTimeSeriesFromDataStore_Command command )
{	super(parent, true);

	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	Object o = event.getSource();
    if ( __ignoreEvents ) {
        return; // Startup.
    }

	if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput ();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else {
		refresh();
	}
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
{	if ( __ignoreEvents ) {
        // Startup
        return;
    }
    // Put together a list of parameters to check...
	PropList props = new PropList ( "" );
	__error_wait = false;
	String DataStore = __DataStore_JComboBox.getSelected();
	if ( (DataStore != null) && DataStore.length() > 0 ) {
		props.set ( "DataStore", DataStore );
	}
	String DataType = StringUtil.getToken(__DataType_JComboBox.getSelected().trim(), " ", 0, 0 );
    if ( (DataType != null) && (DataType.length() > 0) ) {
        props.set ( "DataType", DataType );
    }
    String Interval = __Interval_JComboBox.getSelected();
    if ( (Interval != null) && (Interval.length() > 0) ) {
        props.set ( "Interval", Interval );
    }
    String LocationType = __LocationType_JComboBox.getSelected();
    if ( (LocationType != null) && (LocationType.length() > 0) ) {
        props.set ( "LocationType", LocationType );
    }
    String LocationID = __LocationID_JComboBox.getSelected();
    if ( (LocationID != null) && (LocationID.length() > 0) ) {
        props.set ( "LocationID", LocationID );
    }
    String DataSource = __DataSource_JComboBox.getSelected();
    if ( (DataSource != null) && (DataSource.length() > 0) ) {
        props.set ( "DataSource", DataSource );
    }
    String Scenario = __Scenario_JComboBox.getSelected();
    if ( (Scenario != null) && (Scenario.length() > 0) ) {
        props.set ( "Scenario", Scenario );
    }
    if ( __inputFilter_JPanel != null ) {
    	int numWhere = __inputFilter_JPanel.getNumFilterGroups();
    	for ( int i = 1; i <= numWhere; i++ ) {
    	    String where = getWhere ( i - 1 );
    	    if ( where.length() > 0 ) {
    	        props.set ( "Where" + i, where );
    	    }
        }
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
    String DataType = StringUtil.getToken(__DataType_JComboBox.getSelected().trim(), " ", 0, 0 );
    String Interval = __Interval_JComboBox.getSelected();
	__command.setCommandParameter ( "DataStore", DataStore );
	__command.setCommandParameter ( "DataType", DataType );
	__command.setCommandParameter ( "Interval", Interval );
    String LocationType = __LocationType_JComboBox.getSelected();
    __command.setCommandParameter ( "LocationType", LocationType );
	String LocationID = __LocationID_JComboBox.getSelected();
    __command.setCommandParameter ( "LocationID", LocationID );
    String DataSource = __DataSource_JComboBox.getSelected();
    __command.setCommandParameter ( "DataSource", DataSource );
    String Scenario = __Scenario_JComboBox.getSelected();
    __command.setCommandParameter ( "Scenario", Scenario );
	if ( __inputFilter_JPanel != null ) {
	    String delim = ";";
    	int numWhere = __inputFilter_JPanel.getNumFilterGroups();
    	for ( int i = 1; i <= numWhere; i++ ) {
    	    String where = getWhere ( i - 1 );
    	    if ( where.startsWith(delim) ) {
    	        where = "";
    	    }
    	    __command.setCommandParameter ( "Where" + i, where );
    	}
	}
	String InputStart = __InputStart_JTextField.getText().trim();
	__command.setCommandParameter ( "InputStart", InputStart );
	String InputEnd = __InputEnd_JTextField.getText().trim();
	__command.setCommandParameter ( "InputEnd", InputEnd );
	String Alias = __Alias_JTextField.getText().trim();
    __command.setCommandParameter ( "Alias", Alias );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__Alias_JTextField = null;
	__InputStart_JTextField = null;
	__InputEnd_JTextField = null;
	__cancel_JButton = null;
	__command_JTextArea = null;
	__command = null;
	__ok_JButton = null;
	super.finalize ();
}

/**
Get the selected data store.
*/
private GenericDatabaseDataStore getSelectedDataStore ()
{   String routine = getClass().getName() + ".getSelectedDataStore";
    String DataStore = __DataStore_JComboBox.getSelected();
    GenericDatabaseDataStore dataStore = (GenericDatabaseDataStore)((TSCommandProcessor)
        __command.getCommandProcessor()).getDataStoreForName(
        DataStore, GenericDatabaseDataStore.class );
    if ( dataStore == null ) {
        Message.printStatus(2, routine, "Selected data store is \"" + DataStore + "\"." );
    }
    return dataStore;
}

/**
Return the "WhereN" parameter for the requested input filter.
@return the "WhereN" parameter for the requested input filter.
@param ifg the Input filter to process (zero index).
*/
private String getWhere ( int ifg )
{
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
private void initialize ( JFrame parent, ReadTimeSeriesFromDataStore_Command command )
{	String routine = "ReadTimeSeriesFromDataStore_JDialog.initialize";
	__command = command;
	CommandProcessor processor = __command.getCommandProcessor();

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"Read one or more time series from a database datastore that has been configured to provide time series metadata."),
    	0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Refer to the Generic Database Datastore documentation for more information." ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"If not specified, the global input period is used (see SetInputPeriod())."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	
    __ignoreEvents = true; // So that a full pass of initialization can occur
   	
   	// List available data stores of the correct type
   	
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Datastore:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStore_JComboBox = new SimpleJComboBox ( false );
    TSCommandProcessor tsProcessor = (TSCommandProcessor)processor;
    List<DataStore> dataStoreList = tsProcessor.getDataStoresByType( GenericDatabaseDataStore.class );
    GenericDatabaseDataStore ds;
    for ( DataStore dataStore: dataStoreList ) {
        ds = (GenericDatabaseDataStore)dataStore;
        if ( ds.hasTimeSeriesInterface(true) ) {
            __DataStore_JComboBox.addItem ( dataStore.getName() );
        }
    }
    if ( __DataStore_JComboBox.getItemCount() > 0 ) {
        __DataStore_JComboBox.select ( 0 );
    }
    __DataStore_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __DataStore_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - data store containing time series."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Data types are particular to the data store...
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data type:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataType_JComboBox = new SimpleJComboBox ( false );
    populateDataTypeChoices();
    __DataType_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __DataType_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - data type for time series."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Intervals are hard-coded
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data interval:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Interval_JComboBox = new SimpleJComboBox ( false );
    populateIntervalChoices();
    __Interval_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Interval_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - data interval (time step) for time series."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    __tsInfo_JTabbedPane = new JTabbedPane ();
    __tsInfo_JTabbedPane.setBorder(
        BorderFactory.createTitledBorder ( BorderFactory.createLineBorder(Color.black),
        "Indicate how to match time series in the datastore" ));
    JGUIUtil.addComponent(main_JPanel, __tsInfo_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JPanel singleTS_JPanel = new JPanel();
    singleTS_JPanel.setLayout(new GridBagLayout());
    __tsInfo_JTabbedPane.addTab ( "Match Single Time Series", singleTS_JPanel );
    
    int ySingle = -1;
    JGUIUtil.addComponent(singleTS_JPanel,
        new JLabel ("Specify a location ID when a specific time series is being processed.  Choices will cascade based in selections."), 
        0, ++ySingle, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel ("Location type:"), 
        0, ++ySingle, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __LocationType_JComboBox = new SimpleJComboBox ( false );
    populateLocationTypeChoices();
    __LocationType_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(singleTS_JPanel, __LocationType_JComboBox,
        1, ySingle, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(singleTS_JPanel,
        new JLabel ("Optional - location type."),
        3, ySingle, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel ("Location ID:"), 
        0, ++ySingle, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __LocationID_JComboBox = new SimpleJComboBox ( false );
    populateLocationIDChoices();
    __LocationID_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(singleTS_JPanel, __LocationID_JComboBox,
        1, ySingle, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(singleTS_JPanel,
        new JLabel ("Required - location identifier (e.g., station ID)."),
        3, ySingle, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel ("Data source:"), 
        0, ++ySingle, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataSource_JComboBox = new SimpleJComboBox ( false );
    populateDataSourceChoices();
    __DataSource_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(singleTS_JPanel, __DataSource_JComboBox,
        1, ySingle, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(singleTS_JPanel,
        new JLabel ("Optional - data source (e.g., agency abbreviation)."),
        3, ySingle, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(singleTS_JPanel, new JLabel ("Scenario:"), 
        0, ++ySingle, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Scenario_JComboBox = new SimpleJComboBox ( false );
    populateScenarioChoices();
    __Scenario_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(singleTS_JPanel, __Scenario_JComboBox,
        1, ySingle, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(singleTS_JPanel,
        new JLabel ("Optional - scenario."),
        3, ySingle, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JPanel multipleTS_JPanel = new JPanel();
    multipleTS_JPanel.setLayout(new GridBagLayout());
    __tsInfo_JTabbedPane.addTab ( "Match 1+ Time Series Using Filter", multipleTS_JPanel );
 
    int yMultiple = -1;
   	
   	// Input filters
    // TODO SAM 2013-07-17 Need to use SetInputFilters() so the filters can change when a data store is selected.

	int buffer = 3;
	Insets insets = new Insets(0,buffer,0,0);
	try {
	    JGUIUtil.addComponent(multipleTS_JPanel,
            new JLabel ("Specify filters when multiple time series are being processed.  Choices do not cascade based on previous selections."), 
            0, ++yMultiple, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	    // Add input filters for time series metadata...
		__inputFilter_JPanel = new GenericDatabaseDataStore_TimeSeries_InputFilter_JPanel(
		    getSelectedDataStore(), __command.getNumFilterGroups() );
		JGUIUtil.addComponent(multipleTS_JPanel, __inputFilter_JPanel,
			0, ++yMultiple, 3, 1, 0.0, 0.0, insets, GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST );
   		__inputFilter_JPanel.addEventListeners ( this );
   	    JGUIUtil.addComponent(multipleTS_JPanel, new JLabel ( "Optional - query filters."),
   	        3, yMultiple, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	}
	catch ( Exception e ) {
		Message.printWarning ( 2, routine, "Unable to initialize datastore input filter." );
		Message.printWarning ( 2, routine, e );
	}

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Input start:"), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputStart_JTextField = new JTextField (20);
    __InputStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __InputStart_JTextField,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - override the global input start (default=read all)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Input end:"), 
        0, ++y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputEnd_JTextField = new JTextField (20);
    __InputEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __InputEnd_JTextField,
        1, y, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - override the global input end (default=read all)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel("Alias to assign:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Alias_JTextField = new TSFormatSpecifiersJPanel(10);
    __Alias_JTextField.setToolTipText("Use %L for location, %T for data type, %I for interval.");
    __Alias_JTextField.addKeyListener ( this );
    __Alias_JTextField.getDocument().addDocumentListener(this);
    __Alias_JTextField.setToolTipText("%L for location, %T for data type.");
    JGUIUtil.addComponent(main_JPanel, __Alias_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - use %L for location, etc. (default=no alias)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea (4,50);
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);

	// Refresh the contents...
	refresh ();

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
    JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	__cancel_JButton = new SimpleJButton( "Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__ok_JButton = new SimpleJButton("OK", this);
	button_JPanel.add ( __ok_JButton );

	setTitle ( "Edit " + __command.getCommandName() + " Command" );

	// Dialogs do not need to be resizable...
	setResizable ( true );
    pack();
    JGUIUtil.center( this );
	refresh();
	__ignoreEvents = false; // After initialization of components let events happen to dynamically cause cascade
    super.setVisible( true );
}

/**
Handle ItemListener events.
*/
public void itemStateChanged ( ItemEvent event )
{   if ( __ignoreEvents ) {
        return; // Startup
    }
    // The calls are cascaded, based on the order of components in the UI
    // If a new data store has been selected, update the data type, interval, list and the input filter
    if ( event.getSource() == __DataStore_JComboBox ) {
        populateDataTypeChoices();
        setInputFilters();
    }
    else if ( event.getSource() == __DataType_JComboBox ) {
        populateIntervalChoices();
    }
    else if ( event.getSource() == __Interval_JComboBox ) {
        populateLocationTypeChoices();
    }
    else if ( event.getSource() == __LocationType_JComboBox ) {
        populateLocationIDChoices();
    }
    else if ( event.getSource() == __LocationID_JComboBox ) {
        populateDataSourceChoices();
    }
    else if ( event.getSource() == __DataSource_JComboBox ) {
        populateScenarioChoices();
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
    refresh();
}

/**
Need this to properly capture key events, especially deletes.
*/
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
Populate the data source choices in response to a new location type or interval being selected.
*/
private void populateDataSourceChoices ()
{   String routine = getClass().getName() + ".populateDataSourceChoices";
    GenericDatabaseDataStore ds = getSelectedDataStore();
    List<String> dataSources = new Vector<String>();
    try {
        // Pass null for scenario because scenario is listed after data source in UI
        String locID = __LocationID_JComboBox.getSelected();
        if ( (locID != null) && !locID.equals("") ) {
            dataSources = ds.readTimeSeriesMetaDataSourceList (
               __LocationType_JComboBox.getSelected(), __LocationID_JComboBox.getSelected(),
               __DataType_JComboBox.getSelected(), __Interval_JComboBox.getSelected(), null );
        }
    }
    catch ( Exception e ) {
        // Hopefully should not happen
        Message.printWarning(2, routine, "Unable to get data sources for data store \"" +
            ds.getName() + "\" (" + e + ").");
    }
    __DataSource_JComboBox.setData ( dataSources );
    if ( __DataSource_JComboBox.getItemCount() > 0 ) {
        __DataSource_JComboBox.select ( null );
        __DataSource_JComboBox.select ( 0 );
    }
}

/**
Populate the data type choices in response to a new data store being selected.
*/
private void populateDataTypeChoices ()
{   String routine = getClass().getName() + ".populateDataTypeChoices";
    GenericDatabaseDataStore ds = getSelectedDataStore();
    List<String> dataTypes = new Vector<String>();
    try {
        // Pass null for parts that are later components in the UI
        dataTypes = ds.readTimeSeriesMetaDataTypeList ( false, null, null, null, null, null );
    }
    catch ( Exception e ) {
        // Hopefully should not happen
        Message.printWarning(2, routine, "Unable to get data types for data store \"" +
            ds.getName() + "\" (" + e + ").");
    }
    dataTypes.add ( "*" ); // Read all data types
    __DataType_JComboBox.setData ( dataTypes );
    if ( __DataType_JComboBox.getItemCount() > 0 ) {
        __DataType_JComboBox.select ( null );
        __DataType_JComboBox.select ( 0 );
    }
}

/**
Populate the data interval choices in response to a new data type being selected.
*/
private void populateIntervalChoices ()
{   String routine = "ReadTimeeriesFromDataStore.populateIntervalChoices()";
    __Interval_JComboBox.removeAll();
    GenericDatabaseDataStore ds = getSelectedDataStore();
    List<String> intervals = new Vector<String>();
    try {
        // Pass null for choices that are later in the UI
        intervals = ds.readTimeSeriesMetaIntervalList ( null, null, null, __DataType_JComboBox.getSelected(), null );
    }
    catch ( Exception e ) {
        // Hopefully should not happen
        Message.printWarning(2, routine, "Unable to get intervals for data store \"" +
            ds.getName() + "\" (" + e + ").");
    }
    __Interval_JComboBox.setData ( intervals );
    if ( __Interval_JComboBox.getItemCount() > 0 ) {
        __Interval_JComboBox.select ( null );
        __Interval_JComboBox.select ( 0 );
    }
}

/**
Populate the location ID choices in response to a new location type or interval being selected.
*/
private void populateLocationIDChoices ()
{   String routine = getClass().getName() + ".populateLocationIDChoices";
    GenericDatabaseDataStore ds = getSelectedDataStore();
    List<String> locIds = new Vector<String>();
    try {
        // Pass null for the data source and scenario since the are later choices in the UI
        locIds = ds.readTimeSeriesMetaLocationIDList ( __LocationType_JComboBox.getSelected(),
            null, __DataType_JComboBox.getSelected(), __Interval_JComboBox.getSelected(), null );
    }
    catch ( Exception e ) {
        // Hopefully should not happen
        Message.printWarning(2, routine, "Unable to get location IDs for data store \"" +
            ds.getName() + "\" (" + e + ").");
    }
    // Add a blank at the beginning to indicate that filters are used
    locIds.add ( 0, "" );
    __LocationID_JComboBox.setData ( locIds );
    if ( __LocationID_JComboBox.getItemCount() > 0 ) {
        __LocationID_JComboBox.select ( null );
        __LocationID_JComboBox.select ( 0 );
    }
}

/**
Populate the location type choices in response to a new data store being selected.
*/
private void populateLocationTypeChoices ()
{   String routine = getClass().getName() + ".populateLocationTypeChoices";
    GenericDatabaseDataStore ds = getSelectedDataStore();
    List<String> locTypes = new Vector<String>();
    try {
        // Pass null for location ID, data source, and scenario since they are later choices in UI
        locTypes = ds.readTimeSeriesMetaLocationTypeList (
            null, null, __DataType_JComboBox.getSelected(), __Interval_JComboBox.getSelected(), null );
    }
    catch ( Exception e ) {
        // Hopefully should not happen
        Message.printWarning(2, routine, "Unable to get location types for data store \"" +
            ds.getName() + "\" (" + e + ").");
    }
    __LocationType_JComboBox.setData ( locTypes );
    if ( __LocationType_JComboBox.getItemCount() > 0 ) {
        __LocationType_JComboBox.select ( null );
        __LocationType_JComboBox.select ( 0 );
    }
}

/**
Populate the scenario choices in response to a new location type or interval being selected.
*/
private void populateScenarioChoices ()
{   String routine = getClass().getName() + ".populateDataSourceChoices";
    GenericDatabaseDataStore ds = getSelectedDataStore();
    List<String> scenarioIds = new Vector<String>();
    try {
        // Have values for all parts because scenario is last in UI
        String locID = __LocationID_JComboBox.getSelected();
        if ( (locID != null) && !locID.equals("") ) {
            scenarioIds = ds.readTimeSeriesMetaScenarioList (
               __LocationType_JComboBox.getSelected(), __LocationID_JComboBox.getSelected(),
               __DataSource_JComboBox.getSelected(), __DataType_JComboBox.getSelected(), __Interval_JComboBox.getSelected() );
        }
    }
    catch ( Exception e ) {
        // Hopefully should not happen
        Message.printWarning(2, routine, "Unable to get scenarios for datastore \"" +
            ds.getName() + "\" (" + e + ").");
    }
    __Scenario_JComboBox.setData ( scenarioIds );
    __Scenario_JComboBox.select ( null );
    if ( __Scenario_JComboBox.getItemCount() > 0 ) {
        __Scenario_JComboBox.select ( 0 );
    }
}

/**
Refresh the command string from the dialog contents.
*/
private void refresh ()
{	String routine = "ReadTimeSeriesFromDataStore_JDialog.refresh";
	__error_wait = false;
	String DataStore = "";
	String DataType = "";
	String Interval = "";
	String LocationType = "";
	String LocationID = "";
	String DataSource = "";
	String Scenario = "";
	String filter_delim = ";";
	String InputStart = "";
	String InputEnd = "";
	String Alias = "";
	PropList props = null;
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		props = __command.getCommandParameters();
		DataStore = props.getValue ( "DataStore" );
		DataType = props.getValue ( "DataType" );
		Interval = props.getValue ( "Interval" );
		LocationType = props.getValue ( "LocationType" );
		LocationID = props.getValue ( "LocationID" );
		DataSource = props.getValue ( "DataSource" );
		Scenario = props.getValue ( "Scenario" );
		InputStart = props.getValue ( "InputStart" );
		InputEnd = props.getValue ( "InputEnd" );
		Alias = props.getValue ( "Alias" );
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
        // Data types currently are one word but may be expanded to in the future
        populateDataTypeChoices();
        //int [] index = new int[1];
        //if ( JGUIUtil.isSimpleJComboBoxItem(__DataType_JComboBox, DataType, JGUIUtil.CHECK_SUBSTRINGS, " ", 0, index, true ) ) {
        //    __DataType_JComboBox.select ( index[0] );
        // }
        if ( JGUIUtil.isSimpleJComboBoxItem(__DataType_JComboBox, DataType, JGUIUtil.NONE, null, null ) ) {
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
                  "DataType parameter \"" + DataType + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        populateIntervalChoices();
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
                  "Interval parameter \"" + Interval + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        if ( __inputFilter_JPanel != null ) {
            InputFilter_JPanel filter_panel = __inputFilter_JPanel;
            int nfg = filter_panel.getNumFilterGroups();
            String where;
            int numSet = 0;
            for ( int ifg = 0; ifg < nfg; ifg ++ ) {
                where = props.getValue ( "Where" + (ifg + 1) );
                if ( (where != null) && (where.length() > 0) ) {
                    // Set the filter...
                    try {
                        filter_panel.setInputFilter (ifg, where, filter_delim );
                        ++numSet;
                    }
                    catch ( Exception e ) {
                        Message.printWarning ( 1, routine, "Error setting where information using \"" + where + "\"" );
                        Message.printWarning ( 3, routine, e );
                    }
                }
            }
            if ( numSet > 0 ) {
                __tsInfo_JTabbedPane.setSelectedIndex(1);
            }
        }
        // Put this after filters because if LocationID is specified the tab should be shown
        populateLocationTypeChoices();
        if ( JGUIUtil.isSimpleJComboBoxItem(__LocationType_JComboBox, LocationType, JGUIUtil.NONE, null, null ) ) {
            __LocationType_JComboBox.select ( LocationType );
        }
        else {
            if ( (LocationType == null) || LocationType.equals("") ) {
                // New command...select the default...
                if ( __LocationType_JComboBox.getItemCount() > 0 ) {
                    __LocationType_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "location type parameter \"" + LocationType + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        populateLocationIDChoices();
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
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "location ID parameter \"" + LocationID + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        populateDataSourceChoices();
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
                  "data source parameter \"" + DataSource + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        populateScenarioChoices();
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
                  "scenario parameter \"" + Scenario + "\".  Select a\ndifferent value or Cancel." );
            }
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
	}
	// Regardless, reset the command from the fields...
	Alias = __Alias_JTextField.getText().trim();
	// Regardless, reset the command from the fields...
	props = new PropList ( __command.getCommandName() );
	DataStore = __DataStore_JComboBox.getSelected();
	if ( DataStore != null ) {
	    DataStore = DataStore.trim();
	}
	// Only save the major variable number because parentheses cause problems in properties
	DataType = StringUtil.getToken(__DataType_JComboBox.getSelected().trim(), " ", 0, 0 );
	Interval = __Interval_JComboBox.getSelected();
    props.add ( "DataStore=" + DataStore );
    props.add ( "DataType=" + DataType );
    props.add ( "Interval=" + Interval );
    LocationType = __LocationType_JComboBox.getSelected();
    props.add ( "LocationType=" + LocationType );
    LocationID = __LocationID_JComboBox.getSelected();
    props.add ( "LocationID=" + LocationID );
	// Add the where clause(s)...
	InputFilter_JPanel filter_panel = __inputFilter_JPanel;
    if ( filter_panel != null ) {
    	int nfg = filter_panel.getNumFilterGroups();
    	String where;
    	String delim = ";";	// To separate input filter parts
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
Set the input filters in response to a new data store being selected.
*/
private void setInputFilters ()
{

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