package rti.tscommandprocessor.commands.table;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import riverside.datastore.DataStore;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

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
import java.io.File;

import java.util.List;
import java.util.Vector;

import RTi.DMI.DMI;
import RTi.DMI.DMIUtil;
import RTi.DMI.DatabaseDataStore;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

public class ReadTableFromDataStore_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{
    
private final String __RemoveWorkingDirectory = "Remove Working Directory";
private final String __AddWorkingDirectory = "Add Working Directory";

private SimpleJButton __browse_JButton = null;
private SimpleJButton __path_JButton = null;
private boolean __error_wait = false;
private boolean __first_time = true;
private JTextArea __command_JTextArea=null;
private SimpleJComboBox __DataStore_JComboBox = null;
private JTextField __TableID_JTextField = null;
private SimpleJComboBox __DataStoreTable_JComboBox = null;
private JTextField __DataStoreColumns_JTextField = null;
private JTextField __OrderBy_JTextField = null;
private JTextArea __Sql_JTextArea = null;
private JTextField __SqlFile_JTextField = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;	
private ReadTableFromDataStore_Command __command = null;
private boolean __ok = false;
private String __working_dir = null;

private boolean __ignoreItemEvents = false; // Used to ignore cascading events when working with choices

private DatabaseDataStore __dataStore = null; // selected data store
private DMI __dmi = null; // DMI to do queries.

/**
Command dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param runnable If true, the command can be run from the dialog, as a Tool.
*/
public ReadTableFromDataStore_JDialog ( JFrame parent, ReadTableFromDataStore_Command command )
{	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed(ActionEvent event)
{	Object o = event.getSource();

    if ( o == __browse_JButton ) {
        // Browse for the file to read...
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle( "Select SQL File");
        SimpleFileFilter sff = new SimpleFileFilter("sql","SQL File");
        fc.addChoosableFileFilter(sff);
        
        String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
        if ( last_directory_selected != null ) {
            fc.setCurrentDirectory( new File(last_directory_selected));
        }
        else {
            fc.setCurrentDirectory(new File(__working_dir));
        }
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String directory = fc.getSelectedFile().getParent();
            String filename = fc.getSelectedFile().getName(); 
            String path = fc.getSelectedFile().getPath(); 
    
            if (filename == null || filename.equals("")) {
                return;
            }
    
            if (path != null) {
                __SqlFile_JTextField.setText(path );
                JGUIUtil.setLastFileDialogDirectory( directory);
                refresh();
            }
        }
    }
    else if ( o == __cancel_JButton ) {
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
    else if ( o == __path_JButton ) {
        if ( __path_JButton.getText().equals( __AddWorkingDirectory) ) {
            __SqlFile_JTextField.setText ( IOUtil.toAbsolutePath(__working_dir,__SqlFile_JTextField.getText() ) );
        }
        else if ( __path_JButton.getText().equals( __RemoveWorkingDirectory) ) {
            try {
                __SqlFile_JTextField.setText ( IOUtil.toRelativePath ( __working_dir, __SqlFile_JTextField.getText() ) );
            }
            catch ( Exception e ) {
                Message.printWarning ( 1, "ReadTableFromDataStore_JDialog", "Error converting file to relative path." );
            }
        }
        refresh ();
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
    String DataStore = __DataStore_JComboBox.getSelected();
    if ( (DataStore != null) && DataStore.length() > 0 ) {
        props.set ( "DataStore", DataStore );
        __dataStore = getSelectedDataStore();
        __dmi = ((DatabaseDataStore)__dataStore).getDMI();
    }
    else {
        props.set ( "DataStore", "" );
    }
	String DataStoreTable = __DataStoreTable_JComboBox.getSelected();
	String DataStoreColumns = __DataStoreColumns_JTextField.getText().trim();
	String OrderBy = __OrderBy_JTextField.getText().trim();
	String Sql = __Sql_JTextArea.getText().trim();
	String SqlFile = __SqlFile_JTextField.getText().trim();
    String TableID = __TableID_JTextField.getText().trim();
	__error_wait = false;

    //if ( __DataStore_JComboBox.getSelected() == null ) {
        // Startup initialization
    //    return;
    //}
	if ( DataStoreTable.length() > 0 ) {
		props.set ( "DataStoreTable", DataStoreTable );
	}
    if ( DataStoreColumns.length() > 0 ) {
        props.set ( "DataStoreColumns", DataStoreColumns );
    }
    if ( OrderBy.length() > 0 ) {
        props.set ( "OrderBy", OrderBy );
    }
    if ( Sql.length() > 0 ) {
        props.set ( "Sql", Sql );
    }
    if ( SqlFile.length() > 0 ) {
        props.set ( "SqlFile", SqlFile );
    }
    if ( TableID.length() > 0 ) {
        props.set ( "TableID", TableID );
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
{	String DataStore = __DataStore_JComboBox.getSelected();
    String TableID = __TableID_JTextField.getText().trim();
    String DataStoreTable = __DataStoreTable_JComboBox.getSelected();
    String DataStoreColumns = __DataStoreColumns_JTextField.getText().trim();
    String OrderBy = __OrderBy_JTextField.getText().trim();
    String Sql = __Sql_JTextArea.getText().trim().replace('\n', ' ').replace('\t', ' ');
    String SqlFile = __SqlFile_JTextField.getText().trim();
    __command.setCommandParameter ( "DataStore", DataStore );
	__command.setCommandParameter ( "DataStoreTable", DataStoreTable );
	__command.setCommandParameter ( "DataStoreColumns", DataStoreColumns );
	__command.setCommandParameter ( "OrderBy", OrderBy );
	__command.setCommandParameter ( "Sql", Sql );
	__command.setCommandParameter ( "SqlFile", SqlFile );
    __command.setCommandParameter ( "TableID", TableID );
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
@param runnable If true, the command can be run from the dialog, as a Tool.
*/
private void initialize ( JFrame parent, ReadTableFromDataStore_Command command )
{	__command = command;
	CommandProcessor processor = __command.getCommandProcessor();
    __working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( (TSCommandProcessor)processor, __command );

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
        "This command reads a table from a database datastore table or view."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "The query can be specified in one of three ways:"),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "    1) Specify a single table or view, related columns, and order (allows for more up-front checks)."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "    2) Specify a free form SQL select statement (allows joins and other SQL constructs " +
        "supported by the database software, but few up-front checks)."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "    3) Similar to 2; however, the SQL statement is read from a file, " +
        "which can be specified relative to the working directory."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    if ( __working_dir != null ) {
        JGUIUtil.addComponent(paragraph, new JLabel (
        "        The working directory is: " + __working_dir ), 
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    JGUIUtil.addComponent(paragraph, new JLabel (
        "The resulting table columns will have data types based on the query results."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);

	JGUIUtil.addComponent(main_JPanel, paragraph,
		0, y, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
    // List available data stores of the correct type
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data store:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
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
    JGUIUtil.addComponent(main_JPanel, __DataStore_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - data store containing data to read."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for parsed query
    int yParsed = -1;
    JPanel parsed_JPanel = new JPanel();
    parsed_JPanel.setLayout( new GridBagLayout() );
    parsed_JPanel.setBorder( BorderFactory.createTitledBorder (
        BorderFactory.createLineBorder(Color.black),
        "Specify query using table and column choices" ));
    JGUIUtil.addComponent( main_JPanel, parsed_JPanel,
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Data tables are particular to the data store...
    
    JGUIUtil.addComponent(parsed_JPanel, new JLabel ( "Data store table:"),
        0, ++yParsed, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStoreTable_JComboBox = new SimpleJComboBox ( false );
    __DataStoreTable_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(parsed_JPanel, __DataStoreTable_JComboBox,
        1, yParsed, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(parsed_JPanel, new JLabel("Required - database table/view to read."), 
        3, yParsed, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(parsed_JPanel, new JLabel ("Data store columns:"),
        0, ++yParsed, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStoreColumns_JTextField = new JTextField (10);
    __DataStoreColumns_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(parsed_JPanel, __DataStoreColumns_JTextField,
        1, yParsed, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(parsed_JPanel, new JLabel ("Optional - database table/view columns, separated by commas (default=all)."),
        3, yParsed, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    JGUIUtil.addComponent(parsed_JPanel, new JLabel ("Order by:"),
        0, ++yParsed, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OrderBy_JTextField = new JTextField (10);
    __OrderBy_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(parsed_JPanel, __OrderBy_JTextField,
        1, yParsed, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(parsed_JPanel, new JLabel ("Optional - columns to sort by, separated by commas (default=no sort)."),
        3, yParsed, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Panel for SQL string query
    int ySql = -1;
    JPanel sql_JPanel = new JPanel();
    sql_JPanel.setLayout( new GridBagLayout() );
    sql_JPanel.setBorder( BorderFactory.createTitledBorder (
        BorderFactory.createLineBorder(Color.black),
        "OR... Specify query using SQL" ));
    JGUIUtil.addComponent( main_JPanel, sql_JPanel,
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(sql_JPanel, new JLabel ("SQL String:"), 
        0, ++ySql, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Sql_JTextArea = new JTextArea (6,50);
    __Sql_JTextArea.setLineWrap ( true );
    __Sql_JTextArea.setWrapStyleWord ( true );
    __Sql_JTextArea.addKeyListener(this);
    JGUIUtil.addComponent(sql_JPanel, new JScrollPane(__Sql_JTextArea),
        1, ySql, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "SQL file to read:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SqlFile_JTextField = new JTextField ( 50 );
    __SqlFile_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __SqlFile_JTextField,
        1, y, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    __browse_JButton = new SimpleJButton ( "Browse", this );
        JGUIUtil.addComponent(main_JPanel, __browse_JButton,
        6, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Table ID:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableID_JTextField = new JTextField (10);
    __TableID_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __TableID_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Required - unique identifier for the output table."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

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

    if ( __working_dir != null ) {
        // Add the button to allow conversion to/from relative path...
        __path_JButton = new SimpleJButton( __RemoveWorkingDirectory, this);
        button_JPanel.add ( __path_JButton );
    }
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
    __TableID_JTextField.removeAll ();
    List<String> tableList = null;
    List<String> notIncluded = new Vector(); // TODO SAM 2012-01-31 need to omit system tables
    if ( dmi == null ) {
        tableList = new Vector();
    }
    else {
        try {
            tableList = DMIUtil.getDatabaseTableNames(dmi, true, notIncluded);
        }
        catch ( Exception e ) {
            Message.printWarning ( 1, routine, "Error getting tables table list (" + e + ")." );
            Message.printWarning ( 3, routine, e );
            tableList = null;
        }
    }
    if ( tableList == null ) {
        tableList = new Vector();
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
    String DataStore = "";
    String TableID = "";
    String DataStoreTable = "";
    String DataStoreColumns = "";
    String OrderBy = "";
    String Sql = "";
    String SqlFile = "";
	PropList props = __command.getCommandParameters();
	if (__first_time) {
		__first_time = false;
		DataStore = props.getValue ( "DataStore" );
        TableID = props.getValue ( "TableID" );
		DataStoreTable = props.getValue ( "DataStoreTable" );
		DataStoreColumns = props.getValue ( "DataStoreColumns" );
		OrderBy = props.getValue ( "OrderBy" );
		Sql = props.getValue ( "Sql" );
		SqlFile = props.getValue ( "SqlFile" );
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
        if ( TableID != null ) {
            __TableID_JTextField.setText ( TableID );
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
        if ( DataStoreColumns != null ) {
            __DataStoreColumns_JTextField.setText ( DataStoreColumns );
        }
        if ( OrderBy != null ) {
            __OrderBy_JTextField.setText ( OrderBy );
        }
        if ( Sql != null ) {
            __Sql_JTextArea.setText ( Sql );
        }
        if ( SqlFile != null ) {
            __SqlFile_JTextField.setText(SqlFile);
        }
	}
	// Regardless, reset the command from the fields...
    DataStore = __DataStore_JComboBox.getSelected();
    if ( DataStore == null ) {
        DataStore = "";
    }
    TableID = __TableID_JTextField.getText().trim();
    DataStoreTable = __DataStoreTable_JComboBox.getSelected();
    if ( DataStoreTable == null ) {
        DataStoreTable = "";
    }
	DataStoreColumns = __DataStoreColumns_JTextField.getText().trim();
	OrderBy = __OrderBy_JTextField.getText().trim();
	Sql = __Sql_JTextArea.getText().trim();
	SqlFile = __SqlFile_JTextField.getText().trim();
	props = new PropList ( __command.getCommandName() );
	props.add ( "DataStore=" + DataStore );
	props.add ( "DataStoreTable=" + DataStoreTable );
	props.add ( "DataStoreColumns=" + DataStoreColumns );
	props.add ( "OrderBy=" + OrderBy );
	props.add ( "Sql=" + Sql );
	props.add ( "SqlFile=" + SqlFile);
    props.add ( "TableID=" + TableID );
	__command_JTextArea.setText( __command.toString ( props ) );
	// Refresh the Path text.
    refreshPathControl();
}
catch ( Exception e ) {
    Message.printWarning ( 3, routine, e );
}
}

/**
Refresh the PathControl text based on the contents of the input text field contents.
*/
private void refreshPathControl()
{
    String SqlFile = __SqlFile_JTextField.getText().trim();
    if ( (SqlFile == null) || (SqlFile.trim().length() == 0) ) {
        if ( __path_JButton != null ) {
            __path_JButton.setEnabled ( false );
        }
        return;
    }

    // Check the path and determine what the label on the path button should be...
    if ( __path_JButton != null ) {
        __path_JButton.setEnabled ( true );
        File f = new File ( SqlFile );
        if ( f.isAbsolute() ) {
            __path_JButton.setText( __RemoveWorkingDirectory );
        }
        else {
            __path_JButton.setText( __AddWorkingDirectory );
        }
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