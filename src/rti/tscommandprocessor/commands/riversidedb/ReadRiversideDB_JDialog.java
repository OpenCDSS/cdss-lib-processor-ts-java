// ReadRiversideDB_JDialog - Editor for the ReadRiversideDB() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2019 Colorado Department of Natural Resources

CDSS Time Series Processor Java Library is free software:  you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    CDSS Time Series Processor Java Library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with CDSS Time Series Processor Java Library.  If not, see <https://www.gnu.org/licenses/>.

NoticeEnd */

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
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import riverside.datastore.DataStore;
import rti.tscommandprocessor.core.TSCommandProcessor;

import RTi.DMI.DMIUtil;
import RTi.DMI.DatabaseDataStore;
import RTi.DMI.RiversideDB_DMI.RiversideDBDataStore;
import RTi.DMI.RiversideDB_DMI.RiversideDB_DMI;
import RTi.DMI.RiversideDB_DMI.RiversideDB_DataType;
import RTi.DMI.RiversideDB_DMI.RiversideDB_MeasType;
import RTi.DMI.RiversideDB_DMI.RiversideDB_MeasTypeMeasLocGeoloc_InputFilter_JPanel;
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
Editor for the ReadRiversideDB() command.
*/
public class ReadRiversideDB_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private ReadRiversideDB_Command __command = null;
private SimpleJComboBox __DataStore_JComboBox = null;
private SimpleJComboBox __DataType_JComboBox = null;
private SimpleJComboBox __Interval_JComboBox = null;
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
private JTextField __MissingValue_JTextField = null;
private JTextField __InputStart_JTextField;
private JTextField __InputEnd_JTextField;	
		
private JTextArea __command_JTextArea = null;

private InputFilter_JPanel __inputFilter_JPanel = null;
private RiversideDBDataStore __dataStore = null; // selected RiversideDB_DataStore
private RiversideDB_DMI __dmi = null; // RiversideDB_DMI to do queries.
private boolean __error_wait = false; // Is there an error to be cleared up
private boolean __first_time = true;
private boolean __ok = false; // Was OK pressed when closing the dialog.

private boolean __ignoreEvents = false; // Used to ignore cascading events when initializing the components

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public ReadRiversideDB_JDialog ( JFrame parent, ReadRiversideDB_Command command )
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
		checkInput ();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else {
		// combo boxes...
		refresh();
	}
}

/**
Refresh the query choices for the currently selected RiversideDB data store.
@param value if non-null, then the selection is from the command initialization, in which case the
specified data type should be selected
*/
private void actionPerformedDataTypeSelected ( )
{
    if ( __DataType_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    // Now populate the interval choices corresponding to the data type
    populateIntervalChoices ( __dmi );
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
    setDMIForSelectedDataStore();
    //Message.printStatus(2, "", "Selected data store " + __dataStore + " __dmi=" + __dmi );
    // Now populate the data type choices corresponding to the data store
    populateDataTypeChoices ( __dmi );
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
        return; // Startup.
    }
    // Put together a list of parameters to check...
	PropList props = new PropList ( "" );
	__error_wait = false;
    String DataStore = __DataStore_JComboBox.getSelected();
    if ( DataStore.length() > 0 ) {
        props.set ( "DataStore", DataStore );
    }
    String DataType = getSelectedDataType();
    if ( DataType.length() > 0 ) {
        props.set ( "DataType", DataType );
    }
    String Interval = __Interval_JComboBox.getSelected();
    if ( Interval.length() > 0 ) {
        props.set ( "Interval", Interval );
    }
    int numWhere = __inputFilter_JPanel.getNumFilterGroups();
    for ( int i = 1; i <= numWhere; i++ ) {
        String where = getWhere ( i - 1 );
        if ( where.length() > 0 ) {
            props.set ( "Where" + i, where );
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
    String MissingValue = __MissingValue_JTextField.getText().trim();
    if ( MissingValue.length() > 0 ) {
        props.set ( "MissingValue", MissingValue );
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
{
    String DataStore = __DataStore_JComboBox.getSelected();
    String DataType = getSelectedDataType();
    String Interval = __Interval_JComboBox.getSelected();
    __command.setCommandParameter ( "DataStore", DataStore );
    __command.setCommandParameter ( "DataType", DataType );
    __command.setCommandParameter ( "Interval", Interval );
    String delim = ";";
    int numWhere = __inputFilter_JPanel.getNumFilterGroups();
    for ( int i = 1; i <= numWhere; i++ ) {
        String where = getWhere ( i - 1 );
        if ( where.startsWith(delim) ) {
            where = "";
        }
        __command.setCommandParameter ( "Where" + i, where );
    }
    String InputStart = __InputStart_JTextField.getText().trim();
    __command.setCommandParameter ( "InputStart", InputStart );
    String InputEnd = __InputEnd_JTextField.getText().trim();
    __command.setCommandParameter ( "InputEnd", InputEnd );
    String Alias = __Alias_JTextField.getText().trim();
    __command.setCommandParameter ( "Alias", Alias );
    String MissingValue = __MissingValue_JTextField.getText().trim();
    __command.setCommandParameter ( "MissingValue", MissingValue );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{
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
Return the selected data type, omitting the trailing " - description".
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
Get the selected data store from the processor.
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
private void initialize ( JFrame parent, ReadRiversideDB_Command command )
{	String routine = getClass().getName() + "initialize";
	__command = command;
	CommandProcessor processor = __command.getCommandProcessor();

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"Read 1+ time series from a RiversideDB database."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specifying the period will limit data that are available " +
		"for fill commands but can increase performance." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	
    __ignoreEvents = true; // So that a full pass of initialization can occur
   	
    // List available data stores of the correct type
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data store:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStore_JComboBox = new SimpleJComboBox ( false );
    TSCommandProcessor tsProcessor = (TSCommandProcessor)processor;
    List<DataStore> dataStoreList = tsProcessor.getDataStoresByType( RiversideDBDataStore.class );
    for ( DataStore dataStore: dataStoreList ) {
        __DataStore_JComboBox.addItem ( dataStore.getName() );
    }
    if ( dataStoreList.size() == 0 ) {
        // Add an empty item so users can at least bring up the editor
        __DataStore_JComboBox.addItem ( "" );
    }
    __DataStore_JComboBox.select ( 0 );
    __DataStore_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __DataStore_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - data store containing data."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Data types are particular to the data store...
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data type:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataType_JComboBox = new SimpleJComboBox ( false );
    __DataType_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __DataType_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - data type for time series."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Intervals are hard-coded
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data interval:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Interval_JComboBox = new SimpleJComboBox ( false );
    __DataType_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Interval_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - data interval (time step) for time series."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Input filters
    // TODO SAM 2010-11-02 Need to use SetInputFilters() so the filters can change when a
    // data store is selected.  For now it is OK because the input filters do not provide choices.

    int buffer = 3;
    Insets insets = new Insets(0,buffer,0,0);
    try {
        // Add input filters for RiversideDB time series...
        __inputFilter_JPanel = new RiversideDB_MeasTypeMeasLocGeoloc_InputFilter_JPanel(
            getSelectedDataStore(), __command.getNumFilterGroups() );
        JGUIUtil.addComponent(main_JPanel, __inputFilter_JPanel,
            0, ++y, 7, 1, 0.0, 0.0, insets, GridBagConstraints.HORIZONTAL,
            GridBagConstraints.WEST );
        __inputFilter_JPanel.addEventListeners ( this );
        JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - query filters."),
            3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    }
    catch ( Exception e ) {
        Message.printWarning ( 2, routine, "Unable to initialize RiversideDB input filter (" + e + ")." );
        Message.printWarning ( 2, routine, e );
    }

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Input start:"), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputStart_JTextField = new JTextField (20);
    __InputStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __InputStart_JTextField,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - override the global input start."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Input end:"), 
        0, ++y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputEnd_JTextField = new JTextField (20);
    __InputEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __InputEnd_JTextField,
        1, y, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - override the global input end."),
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
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Missing value:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MissingValue_JTextField = new JTextField ( "", 10 );
    JGUIUtil.addComponent(main_JPanel, __MissingValue_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __MissingValue_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - missing data value (default=-999, recommended=NaN)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

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

	checkGUIState(); // FIXME SAM 2012-09-13 Probably don't need this
	refresh();	// Sets the __path_JButton status
    __ignoreEvents = false; // After initialization of components let events happen to dynamically cause cascade
    checkGUIState(); // Do this again because it may not have happened due to the special event handling

    // Dialogs do not need to be resizable...
    setResizable ( true );
    pack();
    JGUIUtil.center( this );
    super.setVisible( true );
}

/**
Respond to ItemEvents.
*/
public void itemStateChanged ( ItemEvent event )
{   if ( __ignoreEvents ) {
        return; // Startup
    }
    if ( (event.getSource() == __DataStore_JComboBox) && (event.getStateChange() == ItemEvent.SELECTED) ) {
        // User has selected a data store.
        actionPerformedDataStoreSelected ();
    }
    else if ( (event.getSource() == __DataType_JComboBox) && (event.getStateChange() == ItemEvent.SELECTED) ) {
        // User has selected a data store.
        actionPerformedDataTypeSelected ();
    }
    refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	if ( __ignoreEvents ) {
        return; // Startup
    }
    refresh();
}

/**
Need this to properly capture key events, especially deletes.
*/
public void keyReleased ( KeyEvent event )
{	if ( __ignoreEvents ) {
        return; // Startup
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
Populate the data type list based on the selected database.
*/
private void populateDataTypeChoices ( RiversideDB_DMI rdmi )
{   String routine = getClass().getName() + "populateDataTypeChoices";
    __DataType_JComboBox.removeAll ();
    List<RiversideDB_MeasType> mts = null;
    List<RiversideDB_DataType> dts = null;
    try {
        mts = rdmi.readMeasTypeListForDistinctData_type();
        dts = rdmi.readDataTypeList();
    }
    catch ( Exception e ) {
        Message.printWarning ( 1, routine, "Error getting time series for data type choices (" + e + ")." );
        Message.printWarning ( 3, routine, e );
        Message.printWarning ( 3, routine, rdmi.getLastSQLString() );
        mts = null;
    }
    int size = 0;
    if ( mts != null ) {
        size = mts.size();
    }
    int dataTypeLengthMax = 80;
    if ( size > 0 ) {
        RiversideDB_MeasType mt = null;
        int pos;
        String data_type;
        for ( int i = 0; i < size; i++ ) {
            mt = mts.get(i);
            pos = RiversideDB_DataType.indexOf (dts, mt.getData_type() );
            if ( pos < 0 ) {
                __DataType_JComboBox.add(mt.getData_type() );
            }
            else {
                data_type = mt.getData_type() + " - " + dts.get(pos).getDescription();
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
    String dataType = getSelectedDataType();
    List<RiversideDB_MeasType> v = null;
    try {
        v = dmi.readMeasTypeListForTSIdent ( ".." + dataType + ".." );
    }
    catch ( Exception e ) {
        Message.printWarning(2, routine, "Error getting time steps from RiversideDB \"" +
            __dataStore.getName() + "(" + e + ").");
        Message.printWarning(2, routine, e);
        v = null;
    }
    int size = 0;
    if ( v != null ) {
        size = v.size();
    }
    RiversideDB_MeasType mt = null;
    String timestep;
    String time_step_base;
    long time_step_mult;
    __Interval_JComboBox.removeAll ();
    for ( int i = 0; i < size; i++ ) {
        mt = v.get(i);
        // Only add if not already listed. Alternatively - add a "distinct" query
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
Refresh the command string from the dialog contents.
*/
private void refresh ()
{	String routine = getClass().getName() + "refresh";
	__error_wait = false;
    String DataStore = "";
    String DataType = "";
    String Interval = "";
    String filter_delim = ";";
    String InputStart = "";
    String InputEnd = "";
    String Alias = "";
    String MissingValue = "";
    PropList props = null;
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command...
        props = __command.getCommandParameters();
        DataStore = props.getValue ( "DataStore" );
        DataType = props.getValue ( "DataType" );
        Interval = props.getValue ( "Interval" );
        InputStart = props.getValue ( "InputStart" );
        InputEnd = props.getValue ( "InputEnd" );
        Alias = props.getValue ( "Alias" );
        MissingValue = props.getValue ( "MissingValue" );
        // The data store list is set up in initialize() but is selected here
        if ( JGUIUtil.isSimpleJComboBoxItem(__DataStore_JComboBox, DataStore, JGUIUtil.NONE, null, null ) ) {
            __DataStore_JComboBox.select ( null ); // To ensure that following causes an event
            __DataStore_JComboBox.select ( DataStore ); // This will trigger getting the DMI for use in the editor
            if ( __ignoreEvents ) {
                // Also need to make sure that the data store and DMI are actually selected
                // Call manually because events are disabled at startup to allow cascade to work properly
                setDMIForSelectedDataStore();
            }
        }
        else {
            if ( (DataStore == null) || DataStore.equals("") ) {
                // New command...select the default...
                __DataStore_JComboBox.select ( null ); // To ensure that following causes an event
                __DataStore_JComboBox.select ( 0 );
                if ( __ignoreEvents ) {
                    // Also need to make sure that the data store and DMI are actually selected
                    // Call manually because events are disabled at startup to allow cascade to work properly
                    setDMIForSelectedDataStore();
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "DataStore parameter \"" + DataStore + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        // First populate the data type choices...
        populateDataTypeChoices(getRiversideDB_DMI() );
        // Then set to the initial value.
        int [] dataTypeIndex = new int[1];
        if ( JGUIUtil.isSimpleJComboBoxItem(__DataType_JComboBox, DataType, JGUIUtil.NONE, null, null) ) {
            // Exact match
            __DataType_JComboBox.select ( DataType );
        }
        else if ( JGUIUtil.isSimpleJComboBoxItem( __DataType_JComboBox,
            DataType, JGUIUtil.CHECK_SUBSTRINGS, " ", 0, dataTypeIndex, true) ) {
            // DataType will be like "QIN" but choice will be like "QIN - Description" so select based on the
            // first token
            __DataType_JComboBox.select ( dataTypeIndex[0] );
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
                Message.printWarning ( 1, routine, "Existing command references an invalid "+
                    "DataType parameter \"" + DataType + "\".  Select a different value or Cancel." );
            }
        }
        // First populate the interval choices...
        populateIntervalChoices(getRiversideDB_DMI() );
        // Now select what the command had previously (if specified)...
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
        InputFilter_JPanel filter_panel = __inputFilter_JPanel;
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
        if ( InputStart != null ) {
            __InputStart_JTextField.setText ( InputStart );
        }
        if ( InputEnd != null ) {
            __InputEnd_JTextField.setText ( InputEnd );
        }
        if ( Alias != null ) {
            __Alias_JTextField.setText ( Alias );
        }
        if ( MissingValue != null ) {
            __MissingValue_JTextField.setText( MissingValue );
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
    DataType = getSelectedDataType();
    Interval = __Interval_JComboBox.getSelected();
    if ( Interval == null ) {
        Interval = "";
    }
    props.add ( "DataStore=" + DataStore );
    props.add ( "DataType=" + DataType );
    props.add ( "Interval=" + Interval );
    // Add the where clause(s)...
    InputFilter_JPanel filter_panel = __inputFilter_JPanel;
    int nfg = filter_panel.getNumFilterGroups();
    String where;
    String delim = ";"; // To separate input filter parts
    for ( int ifg = 0; ifg < nfg; ifg ++ ) {
        where = filter_panel.toString(ifg,delim).trim();
        // Make sure there is a field that is being checked in a where clause...
        if ( (where.length() > 0) && !where.startsWith(delim) ) {
            // FIXME SAM 2010-11-01 The following discards '=' in the quoted string
            //props.add ( "Where" + (ifg + 1) + "=" + where );
            props.set ( "Where" + (ifg + 1), where );
        }
    }
    InputStart = __InputStart_JTextField.getText().trim();
    props.add ( "InputStart=" + InputStart );
    InputEnd = __InputEnd_JTextField.getText().trim();
    props.add ( "InputEnd=" + InputEnd );
    props.add ( "Alias=" + Alias );
    MissingValue = __MissingValue_JTextField.getText().trim();
    props.add ( "MissingValue=" + MissingValue );
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
Set the internal data based on the selected data store.
*/
private void setDMIForSelectedDataStore()
{   __dataStore = getSelectedDataStore();
    __dmi = (RiversideDB_DMI)((DatabaseDataStore)__dataStore).getDMI();
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
