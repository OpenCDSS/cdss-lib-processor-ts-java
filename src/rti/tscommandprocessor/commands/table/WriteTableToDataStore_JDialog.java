package rti.tscommandprocessor.commands.table;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import riverside.datastore.DataStore;
import rti.tscommandprocessor.core.TSCommandProcessor;

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
import java.util.List;
import java.util.Vector;

import RTi.DMI.DMI;
import RTi.DMI.DMIUtil;
import RTi.DMI.DMIWriteModeType;
import RTi.DMI.DatabaseDataStore;
import RTi.Util.GUI.DictionaryJDialog;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

@SuppressWarnings("serial")
public class WriteTableToDataStore_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{

private boolean __error_wait = false;
private boolean __first_time = true;
private JTextArea __command_JTextArea=null;
private SimpleJComboBox __TableID_JComboBox = null;
private JTextField __IncludeColumns_JTextField = null;
private JTextField __ExcludeColumns_JTextField = null;
private SimpleJComboBox __DataStore_JComboBox = null;
private SimpleJComboBox __DataStoreTable_JComboBox = null;
private JTextArea __ColumnMap_JTextArea = null;
private JTextArea __DataStoreRelatedColumnsMap_JTextArea = null;
private SimpleJComboBox __WriteMode_JComboBox = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;	
private WriteTableToDataStore_Command __command = null;
private boolean __ok = false;
private JFrame __parent = null;

private boolean __ignoreItemEvents = false; // Used to ignore cascading events when working with choices

private DatabaseDataStore __dataStore = null; // selected data store
private DMI __dmi = null; // DMI to do queries.

/**
Command dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices list of table identifiers to provide as choices
*/
public WriteTableToDataStore_JDialog ( JFrame parent, WriteTableToDataStore_Command command,
    List<String> tableIDChoices )
{	super(parent, true);
	initialize ( parent, command, tableIDChoices );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed(ActionEvent event)
{	Object o = event.getSource();

    if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput ();
		if ( !__error_wait ) {
			// Command has been edited...
			response ( true );
		}
	}
    else if ( event.getActionCommand().equalsIgnoreCase("EditColumnMap") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String ColumnMap = __ColumnMap_JTextArea.getText().trim();
        String [] notes = { "Table Column - specify the TSTool table column being written.",
            "Datastore Column - specify the datastore table column being written",
            "If the first column is actual data values and the second is a foreign key column,",
            "then use the related table parameter to indicate how to look up the foreign key value."};
        String dict = (new DictionaryJDialog ( __parent, true, ColumnMap,
            "Edit ColumnMap Parameter", notes, "Table Column", "Datastore Column",10)).response();
        if ( dict != null ) {
            __ColumnMap_JTextArea.setText ( dict );
            refresh();
        }
    }
    else if ( event.getActionCommand().equalsIgnoreCase("EditDataStoreRelatedColumnsMap") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String DataStoreRelatedColumnsMap = __DataStoreRelatedColumnsMap_JTextArea.getText().trim();
        String [] notes = { "Related Table Key Column - specify the foreign key column " +
        		"(table is determined by database key relationship, if defined).",
                "Related Table Value Column - specify the foreign table column used to look up the foreign key from data value",
                "For example, specify Related Table Key Column = DataTypeID to indicate the foreign key being processed,",
                "and specify Related Table Value Column = DataTypeAbbreviation to indicate the string DataType value to look up.",
                "When writing an association (relation) table, it is OK to specify the Related Table Value Column as",
                "The original data table and matching data value column (e.g., DataTypes.DataTypesID."};
        String dict = (new DictionaryJDialog ( __parent, true, DataStoreRelatedColumnsMap,
            "Edit DataStoreRelatedColumnsMap Parameter", notes, "Related Table Key Column",
            "Related Table Value Column [RelatedTable.]RelatedColumn for Value",10)).response();
        if ( dict != null ) {
            __DataStoreRelatedColumnsMap_JTextArea.setText ( dict );
            refresh();
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
    __dmi = ((DatabaseDataStore)__dataStore).getDMI();
    //Message.printStatus(2, "", "Selected data store " + __dataStore + " __dmi=" + __dmi );
    // Now populate the data type choices corresponding to the data store
    populateDataStoreTableChoices ( __dmi );
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
	PropList props = new PropList ( "" );
	String TableID = __TableID_JComboBox.getSelected();
	String IncludeColumns = __IncludeColumns_JTextField.getText().trim();
	String ExcludeColumns = __ExcludeColumns_JTextField.getText().trim();
    String DataStore = __DataStore_JComboBox.getSelected();
	String DataStoreTable = __DataStoreTable_JComboBox.getSelected();
	String ColumnMap = __ColumnMap_JTextArea.getText().trim().replace("\n"," ");
	String DataStoreRelatedColumnsMap = __DataStoreRelatedColumnsMap_JTextArea.getText().trim().replace("\n"," ");
	String WriteMode = __WriteMode_JComboBox.getSelected();
	__error_wait = false;

    if ( TableID.length() > 0 ) {
        props.set ( "TableID", TableID );
    }
    if ( IncludeColumns.length() > 0 ) {
        props.set ( "IncludeColumns", IncludeColumns );
    }
    if ( ExcludeColumns.length() > 0 ) {
        props.set ( "ExcludeColumns", ExcludeColumns );
    }
    if ( (DataStore != null) && DataStore.length() > 0 ) {
        props.set ( "DataStore", DataStore );
        __dataStore = getSelectedDataStore();
        __dmi = ((DatabaseDataStore)__dataStore).getDMI();
    }
    else {
        props.set ( "DataStore", "" );
    }
	if ( DataStoreTable.length() > 0 ) {
		props.set ( "DataStoreTable", DataStoreTable );
	}
    if ( ColumnMap.length() > 0 ) {
        props.set ( "ColumnMap", ColumnMap );
    }
    if ( DataStoreRelatedColumnsMap.length() > 0 ) {
        props.set ( "DataStoreRelatedColumnsMap", DataStoreRelatedColumnsMap );
    }
    if ( WriteMode.length() > 0 ) {
        props.set ( "WriteMode", WriteMode );
    }
	try {
	    // This will warn the user...
		__command.checkCommandParameters ( props, null, 1 );
	}
	catch ( Exception e ) {
        Message.printWarning(2,"", e);
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
    String TableID = __TableID_JComboBox.getSelected();
    String IncludeColumns = __IncludeColumns_JTextField.getText().trim();
    String ExcludeColumns = __ExcludeColumns_JTextField.getText().trim();
    String DataStore = __DataStore_JComboBox.getSelected();
    String DataStoreTable = __DataStoreTable_JComboBox.getSelected();
    String ColumnMap = __ColumnMap_JTextArea.getText().trim();
    String DataStoreRelatedColumnsMap = __DataStoreRelatedColumnsMap_JTextArea.getText().trim();
    String WriteMode = __WriteMode_JComboBox.getSelected();
    __command.setCommandParameter ( "TableID", TableID );
    __command.setCommandParameter ( "IncludeColumns", IncludeColumns );
    __command.setCommandParameter ( "ExcludeColumns", ExcludeColumns );
    __command.setCommandParameter ( "DataStore", DataStore );
	__command.setCommandParameter ( "DataStoreTable", DataStoreTable );
	__command.setCommandParameter ( "ColumnMap", ColumnMap );
	__command.setCommandParameter ( "DataStoreRelatedColumnsMap", DataStoreRelatedColumnsMap );
	__command.setCommandParameter ( "WriteMode", WriteMode );
}

/**
Return the DMI that is currently being used for database interaction, based on the selected data store.
*/
private DMI getDMI ()
{
    return __dmi;
}

/**
Get the selected data store.
*/
private DatabaseDataStore getSelectedDataStore ()
{   String routine = getClass().getName() + ".getSelectedDataStore";
    String DataStore = __DataStore_JComboBox.getSelected();
    DatabaseDataStore dataStore = (DatabaseDataStore)((TSCommandProcessor)
        __command.getCommandProcessor()).getDataStoreForName(
        DataStore, DatabaseDataStore.class );
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
@param parent JFrame class instantiating this class.
@param command Command to edit and possibly run.
*/
private void initialize ( JFrame parent, WriteTableToDataStore_Command command, List<String> tableIDChoices )
{	__command = command;
    __parent = parent;
	CommandProcessor processor = __command.getCommandProcessor();

	addWindowListener(this);

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout(new GridBagLayout());
	getContentPane().add ("North", main_JPanel);
	int y = 0;

	JPanel paragraph = new JPanel();
	paragraph.setLayout(new GridBagLayout());
	int yy = -1;

   	JGUIUtil.addComponent(paragraph, new JLabel (
        "This command writes a table to a database datastore table or view."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "The table column names and types by default must match the database table columns but can " +
        "be mapped with the ColumnMap parameter."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "The write mode can impact performance and should be consistent with data management processes."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);

	JGUIUtil.addComponent(main_JPanel, paragraph,
		0, y, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Table ID:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableID_JComboBox = new SimpleJComboBox ( 12, true );    // Allow edit
    tableIDChoices.add(0,""); // Add blank to ignore table
    __TableID_JComboBox.setData ( tableIDChoices );
    __TableID_JComboBox.addItemListener ( this );
    //__TableID_JComboBox.setMaximumRowCount(tableIDChoices.size());
    JGUIUtil.addComponent(main_JPanel, __TableID_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Required - identifier of table to write."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Table columns to write:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __IncludeColumns_JTextField = new JTextField (10);
    __IncludeColumns_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __IncludeColumns_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - columns from TableID, separated by commas (default=all)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Table columns to NOT write:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ExcludeColumns_JTextField = new JTextField (10);
    __ExcludeColumns_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __ExcludeColumns_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - columns from TableID, separated by commas (default=all)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
    // List available data stores of the correct type
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Datastore:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStore_JComboBox = new SimpleJComboBox ( false );
    TSCommandProcessor tsProcessor = (TSCommandProcessor)processor;
    List<DataStore> dataStoreList = tsProcessor.getDataStoresByType( DatabaseDataStore.class );
    List<String> datastoreChoices = new ArrayList<String>();
    for ( DataStore dataStore: dataStoreList ) {
    	datastoreChoices.add ( dataStore.getName() );
    }
    if ( dataStoreList.size() == 0 ) {
        // Add an empty item so users can at least bring up the editor
    	datastoreChoices.add ( "" );
    }
    __DataStore_JComboBox.setData(datastoreChoices);
    __DataStore_JComboBox.select ( 0 );
    __DataStore_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __DataStore_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - database datastore to receive data."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Data tables are particular to the data store...
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Datastore table/view:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStoreTable_JComboBox = new SimpleJComboBox ( false );
    __DataStoreTable_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __DataStoreTable_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - database table/view to receive data."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Table to datastore column map:"),
        0, ++y, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ColumnMap_JTextArea = new JTextArea (6,35);
    __ColumnMap_JTextArea.setLineWrap ( true );
    __ColumnMap_JTextArea.setWrapStyleWord ( true );
    __ColumnMap_JTextArea.setToolTipText("TableColumn:DatastoreColumn,TableColumn:DataStoreColumn");
    __ColumnMap_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__ColumnMap_JTextArea),
        1, y, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - if column names differ (default=names are same)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(main_JPanel, new SimpleJButton ("Edit","EditColumnMap",this),
        3, ++y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Datastore related columns map:"),
        0, ++y, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStoreRelatedColumnsMap_JTextArea = new JTextArea (6,35);
    __DataStoreRelatedColumnsMap_JTextArea.setLineWrap ( true );
    __DataStoreRelatedColumnsMap_JTextArea.setWrapStyleWord ( true );
    __DataStoreRelatedColumnsMap_JTextArea.setToolTipText("DatastoreColumn:RelatedTable.RelatedColumn,...");
    __DataStoreRelatedColumnsMap_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__DataStoreRelatedColumnsMap_JTextArea),
        1, y, 2, 2, 2, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - table column to datatstore value column map."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(main_JPanel, new SimpleJButton ("Edit","EditDataStoreRelatedColumnsMap",this),
        3, ++y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // How to write the data...
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Write mode:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __WriteMode_JComboBox = new SimpleJComboBox ( false );
    List<String> writeModeChoices = new Vector<String>();
    writeModeChoices.add ( "" );
    writeModeChoices.add ( "" + DMIWriteModeType.DELETE_INSERT );
    writeModeChoices.add ( "" + DMIWriteModeType.INSERT );
    writeModeChoices.add ( "" + DMIWriteModeType.INSERT_UPDATE );
    writeModeChoices.add ( "" + DMIWriteModeType.UPDATE );
    writeModeChoices.add ( "" + DMIWriteModeType.UPDATE_INSERT );
    __WriteMode_JComboBox.setData(writeModeChoices);
    __WriteMode_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __WriteMode_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Optional - how to write (default=" +
        DMIWriteModeType.INSERT_UPDATE + ")."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Command:"), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea (6,50);
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.setEditable (false);
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Refresh the contents...
	refresh ();

	// South JPanel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	__cancel_JButton = new SimpleJButton("Cancel", this);
	button_JPanel.add (__cancel_JButton);
	__cancel_JButton.setToolTipText ( "Close window without saving changes." );
	__ok_JButton = new SimpleJButton("OK", this);
	button_JPanel.add (__ok_JButton);
	__ok_JButton.setToolTipText ( "Close window and save changes to command." );

	setTitle ( "Edit " + __command.getCommandName() + "() Command");
	//setResizable (false);
    pack();
    JGUIUtil.center(this);
	refresh();
    super.setVisible(true);
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged (ItemEvent event)
{
    if ( !__ignoreItemEvents ) {
        if ( (event.getSource() == __DataStore_JComboBox) && (event.getStateChange() == ItemEvent.SELECTED) ) {
            // User has selected a data store.
            actionPerformedDataStoreSelected ();
        }
    }
    refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed (KeyEvent event) {
	int code = event.getKeyCode();

	if (code == KeyEvent.VK_ENTER) {
		refresh ();
		checkInput();
		if (!__error_wait) {
			response ( true );
		}
	}
}

public void keyReleased (KeyEvent event) {
	refresh();
}

public void keyTyped (KeyEvent event) {}

/**
Indicate if the user pressed OK (cancel otherwise).
@return true if the edits were committed, false if the user canceled.
*/
public boolean ok ()
{	return __ok;
}

/**
Populate the data type list based on the selected database.
@param dmi DMI to use when selecting table list
*/
private void populateDataStoreTableChoices ( DMI dmi )
{   String routine = getClass().getName() + "populateDataStoreTableChoices";
    //__TableID_JTextField.removeAll ();
    List<String> tableList = null;
    List<String> notIncluded = new Vector<String>(); // TODO SAM 2012-01-31 need to omit system tables
    if ( dmi == null ) {
        tableList = new Vector<String>();
    }
    else {
        try {
            tableList = DMIUtil.getDatabaseTableNames(dmi, null, null, true, notIncluded);
        }
        catch ( Exception e ) {
            Message.printWarning ( 1, routine, "Error getting tables table list (" + e + ")." );
            Message.printWarning ( 3, routine, e );
            tableList = null;
        }
    }
    if ( tableList == null ) {
        tableList = new Vector<String>();
    }
    // Always add a blank option at the start to help with initialization
    tableList.add ( 0, "" );
    __DataStoreTable_JComboBox.removeAll();
    for ( String table : tableList ) {
        __DataStoreTable_JComboBox.add( table );
    }
    // Set large so that new table list from selected datastore does not found up layout
    String longest = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    __DataStoreTable_JComboBox.setPrototypeDisplayValue(longest);
    // Select first choice (may get reset from existing parameter values).
    __DataStoreTable_JComboBox.select ( null );
    if ( __DataStoreTable_JComboBox.getItemCount() > 0 ) {
        __DataStoreTable_JComboBox.select ( 0 );
    }
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{	String routine = getClass().getName() + ".refresh";
try{
    String TableID = "";
    String IncludeColumns = "";
    String ExcludeColumns = "";
    String DataStore = "";
    String DataStoreTable = "";
    String ColumnMap = "";
    String DataStoreRelatedColumnsMap = "";
    String WriteMode = "";
	PropList props = __command.getCommandParameters();
	if (__first_time) {
		__first_time = false;
        TableID = props.getValue ( "TableID" );
        IncludeColumns = props.getValue ( "IncludeColumns" );
        ExcludeColumns = props.getValue ( "ExcludeColumns" );
		DataStoreTable = props.getValue ( "DataStoreTable" );
	    DataStore = props.getValue ( "DataStore" );
	    ColumnMap = props.getValue ( "ColumnMap" );
	    DataStoreRelatedColumnsMap = props.getValue ( "DataStoreRelatedColumnsMap" );
	    WriteMode = props.getValue ( "WriteMode" );
        if ( TableID == null ) {
            // Select default...
            __TableID_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __TableID_JComboBox,TableID, JGUIUtil.NONE, null, null ) ) {
                __TableID_JComboBox.select ( TableID );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nTableID value \"" + TableID +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( IncludeColumns != null ) {
            __IncludeColumns_JTextField.setText ( IncludeColumns );
        }
        if ( ExcludeColumns != null ) {
            __ExcludeColumns_JTextField.setText ( ExcludeColumns );
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
        // First populate the data type choices...
        populateDataStoreTableChoices(getDMI() );
        // Now select what the command had previously (if specified)...
        if ( JGUIUtil.isSimpleJComboBoxItem(__DataStoreTable_JComboBox, DataStoreTable, JGUIUtil.NONE, null, null ) ) {
            __DataStoreTable_JComboBox.select ( DataStoreTable );
        }
        else {
            if ( (DataStoreTable == null) || DataStoreTable.equals("") ) {
                // New command...select the default...
                __DataStoreTable_JComboBox.select ( 0 );
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "DataStoreTable parameter \"" + DataStore + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        if ( ColumnMap != null ) {
            __ColumnMap_JTextArea.setText ( ColumnMap );
        }
        if ( DataStoreRelatedColumnsMap != null ) {
            __DataStoreRelatedColumnsMap_JTextArea.setText ( DataStoreRelatedColumnsMap );
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
	TableID = __TableID_JComboBox.getSelected();
	IncludeColumns = __IncludeColumns_JTextField.getText().trim();
	ExcludeColumns = __ExcludeColumns_JTextField.getText().trim();
    DataStore = __DataStore_JComboBox.getSelected();
    if ( DataStore == null ) {
        DataStore = "";
    }
    DataStoreTable = __DataStoreTable_JComboBox.getSelected();
    if ( DataStoreTable == null ) {
        DataStoreTable = "";
    }
    ColumnMap = __ColumnMap_JTextArea.getText().trim();
    DataStoreRelatedColumnsMap = __DataStoreRelatedColumnsMap_JTextArea.getText().trim();
    WriteMode = __WriteMode_JComboBox.getSelected();
	props = new PropList ( __command.getCommandName() );
    props.add ( "TableID=" + TableID );
    props.add ( "IncludeColumns=" + IncludeColumns );
    props.add ( "ExcludeColumns=" + ExcludeColumns );
	props.add ( "DataStore=" + DataStore );
	props.add ( "DataStoreTable=" + DataStoreTable );
	props.add ( "ColumnMap=" + ColumnMap );
	props.add ( "DataStoreRelatedColumnsMap=" + DataStoreRelatedColumnsMap );
	props.add ( "WriteMode=" + WriteMode );
	__command_JTextArea.setText( __command.toString ( props ) );
}
catch ( Exception e ) {
    Message.printWarning ( 3, routine, e );
}
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed and the dialog is closed.
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
public void windowClosing(WindowEvent event) {
	response ( false );
}

// The following methods are all necessary because this class implements WindowListener
public void windowActivated(WindowEvent evt)	{}
public void windowClosed(WindowEvent evt)	{}
public void windowDeactivated(WindowEvent evt)	{}
public void windowDeiconified(WindowEvent evt)	{}
public void windowIconified(WindowEvent evt)	{}
public void windowOpened(WindowEvent evt)	{}

}