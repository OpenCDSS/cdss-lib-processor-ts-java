// CreateDataStoreDataDictionary_JDialog - editor for CreateDataStoreDataDictionary command

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

package rti.tscommandprocessor.commands.datastore;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import riverside.datastore.DataStore;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

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
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import RTi.DMI.DMI;
import RTi.DMI.DatabaseDataStore;
import RTi.DMI.ERDiagram_JFrame;
import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PrintUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Command parameter editor.
*/
@SuppressWarnings("serial")
public class CreateDataStoreDataDictionary_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{
    
private final String __RemoveWorkingDirectory = "Rel";
private final String __AddWorkingDirectory = "Abs";

private SimpleJButton __browse_JButton = null;
private SimpleJButton __path_JButton = null;
private SimpleJButton __view_JButton = null;
private boolean __error_wait = false;
private boolean __first_time = true;
private JTextArea __command_JTextArea=null;
private JTabbedPane __main_JTabbedPane = null;
private SimpleJComboBox __DataStore_JComboBox = null;
private JTextField __ReferenceTables_JTextField = null;
private JTextField __ExcludeTables_JTextField = null;
private JTextField __DataStoreMetaTableForTables_JTextField = null; // Datastore table
private JTextField __DataStoreMetaTableForColumns_JTextField = null; // Datastore table
private JTextField __MetaTableForTables_JTextField = null; // TSTool table
private JTextField __MetaTableForColumns_JTextField = null; // TSTool table
private JTextField __OutputFile_JTextField = null;
private JTextField __Newline_JTextField = null;
private SimpleJComboBox __SurroundWithPre_JComboBox = null;
private SimpleJComboBox __EncodeHtmlChars_JComboBox = null;
private SimpleJComboBox __ERDiagramLayoutTableID_JComboBox = null;
private JTextField __ERDiagramLayoutTableNameColumn_JTextField = null;
private JTextField __ERDiagramLayoutTableXColumn_JTextField = null;
private JTextField __ERDiagramLayoutTableYColumn_JTextField = null;
private SimpleJComboBox __ERDiagramPageSize_JComboBox = null;
private SimpleJComboBox __ERDiagramOrientation_JComboBox = null;
private SimpleJComboBox	__ViewERDiagram_JComboBox = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private CreateDataStoreDataDictionary_Command __command = null;
private boolean __ok = false;
private String __working_dir = null;

private boolean __ignoreItemEvents = false; // Used to ignore cascading events when working with choices

private DatabaseDataStore __dataStore = null; // selected data store
private DMI __dmi = null; // DMI to do queries.
private List<DatabaseDataStore> datastores = new ArrayList<>();

/**
Command dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices table identifiers that can be used for layout
@param datastores list of database datastores
*/
public CreateDataStoreDataDictionary_JDialog ( JFrame parent, CreateDataStoreDataDictionary_Command command,
	List<String> tableIDChoices, List<DatabaseDataStore> datastores )
{	super(parent, true);
	initialize ( parent, command, tableIDChoices, datastores );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed(ActionEvent event)
{	Object o = event.getSource();

    if ( o == __browse_JButton ) {
        String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
        JFileChooser fc = null;
        if ( last_directory_selected != null ) {
            fc = JFileChooserFactory.createJFileChooser( last_directory_selected );
        }
        else {
            fc = JFileChooserFactory.createJFileChooser( __working_dir );
        }
        fc.setDialogTitle("Select Data Dictionary File to Write");
        SimpleFileFilter sff = new SimpleFileFilter("html", "Data Dictionary (HTML) File");
        fc.addChoosableFileFilter(sff);
        
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String directory = fc.getSelectedFile().getParent();
            String filename = fc.getSelectedFile().getName(); 
            String path = fc.getSelectedFile().getPath(); 
    
            if (filename == null || filename.equals("")) {
                return;
            }
    
            if (path != null) {
				// Convert path to relative path by default.
				try {
					__OutputFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1,"CreateDataStoreDataDictionary_JDialog", "Error converting file to relative path." );
				}
                JGUIUtil.setLastFileDialogDirectory(directory );
                refresh();
            }
        }
    }
    else if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "CreateDataStoreDataDictionary");
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
            __OutputFile_JTextField.setText ( IOUtil.toAbsolutePath(__working_dir,__OutputFile_JTextField.getText() ) );
        }
        else if ( __path_JButton.getText().equals( __RemoveWorkingDirectory) ) {
            try {
                __OutputFile_JTextField.setText ( IOUtil.toRelativePath ( __working_dir, __OutputFile_JTextField.getText() ) );
            }
            catch ( Exception e ) {
                Message.printWarning ( 1, "CreateDataStoreDataDictionary_JDialog", "Error converting file to relative path." );
            }
        }
        refresh ();
    }
    else if ( o == __view_JButton ) {
		// Display the ER Diagram viewer
    	// TODO SAM 2015-05-09 need to figure out how to connect a table with layout coordinates
    	viewDiagram();
	}
}

/**
Refresh the database choices in response to the currently selected datastore.
*/
private void actionPerformedDataStoreSelected ( )
{
    if ( __DataStore_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    __dataStore = getSelectedDataStore();
    __dmi = ((DatabaseDataStore)__dataStore).getDMI();
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
        if ( DataStore.indexOf("${") < 0 ) {
        	__dmi = ((DatabaseDataStore)__dataStore).getDMI();
        }
    }
    else {
        props.set ( "DataStore", "" );
    }
    String ReferenceTables = __ReferenceTables_JTextField.getText().trim();
    String ExcludeTables = __ExcludeTables_JTextField.getText().trim();
    String DataStoreMetaTableForTables = __DataStoreMetaTableForTables_JTextField.getText().trim();
    String DataStoreMetaTableForColumns = __DataStoreMetaTableForColumns_JTextField.getText().trim();
    String MetaTableForTables = __MetaTableForTables_JTextField.getText().trim();
    String MetaTableForColumns = __MetaTableForColumns_JTextField.getText().trim();
	String OutputFile = __OutputFile_JTextField.getText().trim();
	String Newline = __Newline_JTextField.getText().trim();
	String SurroundWithPre = __SurroundWithPre_JComboBox.getSelected();
	String EncodeHtmlChars = __EncodeHtmlChars_JComboBox.getSelected();
	String ERDiagramLayoutTableID = __ERDiagramLayoutTableID_JComboBox.getSelected();
	String ERDiagramLayoutTableNameColumn = __ERDiagramLayoutTableXColumn_JTextField.getText().trim();
	String ERDiagramLayoutTableXColumn = __ERDiagramLayoutTableXColumn_JTextField.getText().trim();
	String ERDiagramLayoutTableYColumn = __ERDiagramLayoutTableYColumn_JTextField.getText().trim();
	String ERDiagramPageSize = __ERDiagramPageSize_JComboBox.getSelected();
	String ERDiagramOrientation = __ERDiagramOrientation_JComboBox.getSelected();
	String ViewERDiagram = __ViewERDiagram_JComboBox.getSelected();
	__error_wait = false;

    if ( ReferenceTables.length() > 0 ) {
        props.set ( "ReferenceTables", ReferenceTables );
    }
    if ( !ExcludeTables.isEmpty() ) {
        props.set ( "ExcludeTables", ExcludeTables );
    }
    if ( !DataStoreMetaTableForTables.isEmpty() ) {
        props.set ( "DataStoreMetaTableForTables", DataStoreMetaTableForTables );
    }
    if ( !DataStoreMetaTableForColumns.isEmpty() ) {
        props.set ( "DataStoreMetaTableForColumns", DataStoreMetaTableForColumns );
    }
    if ( !MetaTableForTables.isEmpty() ) {
        props.set ( "MetaTableForTables", MetaTableForTables );
    }
    if ( !MetaTableForColumns.isEmpty() ) {
        props.set ( "MetaTableForColumns", MetaTableForColumns );
    }
    if ( OutputFile.length() > 0 ) {
        props.set ( "OutputFile", OutputFile );
    }
    if ( Newline.length() > 0 ) {
        props.set ( "Newline", Newline );
    }
    if ( SurroundWithPre.length() > 0 ) {
        props.set ( "SurroundWithPre", SurroundWithPre );
    }
    if ( EncodeHtmlChars.length() > 0 ) {
        props.set ( "EncodeHtmlChars", EncodeHtmlChars );
    }
    if ( ERDiagramLayoutTableID.length() > 0 ) {
    	props.set ( "ERDiagramLayoutTableID", ERDiagramLayoutTableID );
    }
    if ( ERDiagramLayoutTableNameColumn.length() > 0 ) {
    	props.set ( "ERDiagramLayoutTableNameColumn", ERDiagramLayoutTableNameColumn );
    }
    if ( ERDiagramLayoutTableXColumn.length() > 0 ) {
    	props.set ( "ERDiagramLayoutTableXColumn", ERDiagramLayoutTableXColumn );
    }
    if ( ERDiagramLayoutTableYColumn.length() > 0 ) {
    	props.set ( "ERDiagramLayoutTableYColumn", ERDiagramLayoutTableYColumn );
    }
    if ( ERDiagramPageSize.length() > 0 ) {
    	props.set ( "ERDiagramPageSize", ERDiagramPageSize );
    }
    if ( ERDiagramOrientation.length() > 0 ) {
    	props.set ( "ERDiagramOrientation", ERDiagramOrientation );
    }
    if ( ViewERDiagram.length() > 0 ) {
        props.set ( "ViewERDiagram", ViewERDiagram );
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
    String ReferenceTables = __ReferenceTables_JTextField.getText().trim();
    String ExcludeTables = __ExcludeTables_JTextField.getText().trim();
    String DataStoreMetaTableForTables = __DataStoreMetaTableForTables_JTextField.getText().trim();
    String DataStoreMetaTableForColumns = __DataStoreMetaTableForColumns_JTextField.getText().trim();
    String MetaTableForTables = __MetaTableForTables_JTextField.getText().trim();
    String MetaTableForColumns = __MetaTableForColumns_JTextField.getText().trim();
    String OutputFile = __OutputFile_JTextField.getText().trim();
	String Newline = __Newline_JTextField.getText().trim();
	String SurroundWithPre = __SurroundWithPre_JComboBox.getSelected();
	String EncodeHtmlChars = __EncodeHtmlChars_JComboBox.getSelected();
	String ERDiagramLayoutTableID = __ERDiagramLayoutTableID_JComboBox.getSelected();
	String ERDiagramLayoutTableNameColumn = __ERDiagramLayoutTableNameColumn_JTextField.getText().trim();
	String ERDiagramLayoutTableXColumn = __ERDiagramLayoutTableXColumn_JTextField.getText().trim();
	String ERDiagramLayoutTableYColumn = __ERDiagramLayoutTableYColumn_JTextField.getText().trim();
	String ERDiagramPageSize = __ERDiagramPageSize_JComboBox.getSelected();
	String ERDiagramOrientation = __ERDiagramOrientation_JComboBox.getSelected();
    String ViewERDiagram = __ViewERDiagram_JComboBox.getSelected();
    __command.setCommandParameter ( "DataStore", DataStore );
	__command.setCommandParameter ( "ReferenceTables", ReferenceTables );
	__command.setCommandParameter ( "ExcludeTables", ExcludeTables );
	__command.setCommandParameter ( "DataStoreMetaTableForTables", DataStoreMetaTableForTables );
	__command.setCommandParameter ( "DataStoreMetaTableForColumns", DataStoreMetaTableForColumns );
	__command.setCommandParameter ( "MetaTableForTables", MetaTableForTables );
	__command.setCommandParameter ( "MetaTableForColumns", MetaTableForColumns );
	__command.setCommandParameter ( "OutputFile", OutputFile );
	__command.setCommandParameter ( "Newline", Newline );
	__command.setCommandParameter ( "SurroundWithPre", SurroundWithPre );
	__command.setCommandParameter ( "EncodeHtmlChars", EncodeHtmlChars );
	__command.setCommandParameter ( "ERDiagramLayoutTableID", ERDiagramLayoutTableID );
	__command.setCommandParameter ( "ERDiagramLayoutTableNameColumn", ERDiagramLayoutTableNameColumn );
	__command.setCommandParameter ( "ERDiagramLayoutTableXColumn", ERDiagramLayoutTableXColumn );
	__command.setCommandParameter ( "ERDiagramLayoutTableYColumn", ERDiagramLayoutTableYColumn );
	__command.setCommandParameter ( "ERDiagramPageSize", ERDiagramPageSize );
	__command.setCommandParameter ( "ERDiagramOrientation", ERDiagramOrientation );
	__command.setCommandParameter ( "ViewERDiagram", ViewERDiagram );
}

/**
Return the DMI that is currently being used for database interaction, based on the selected data store.
*/
private DMI getDMI ()
{
    return __dmi;
}

// TODO smalers 2021-10-24 remove when tested out.
/**
Get the selected data store.
*/
/*
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
*/

/**
Get the selected datastore from the processor using the datastore name.
If there is no datastore in the processor based on startup,
it may be a dynamic datastore created with OpenDataStore,
which will have a discovery datastore that is good enough for getting database metadata.
*/
private DatabaseDataStore getSelectedDataStore () {
    String routine = getClass().getSimpleName() + ".getSelectedDataStore";
    String DataStore = __DataStore_JComboBox.getSelected();
    DatabaseDataStore dataStore = null;
   	dataStore = null;
   	for ( DatabaseDataStore dataStore2 : this.datastores ) {
   		if ( dataStore2.getName().equals(DataStore) ) {
   			dataStore = dataStore2;
   		}
   	}
   	if ( dataStore == null ) {
       	Message.printStatus(2, routine, "Cannot get datastore for \"" + DataStore +
       		"\".  Can read with SQL but cannot choose from list of tables or procedures." );
   	}
    else {
    	// Have an active datastore from software startup.
        Message.printStatus(2, routine, "Selected datastore is \"" + dataStore.getName() + "\"." );
        // Make sure database connection is open.
        dataStore.checkDatabaseConnection();
    }
    return dataStore;
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit and possibly run.
@param tableIDChoices table identifiers candidates for layout table
@param datastores list of database datastores
*/
private void initialize ( JFrame parent, CreateDataStoreDataDictionary_Command command, List<String> tableIDChoices, List<DatabaseDataStore> datastores )
{	this.__command = command;
	this.datastores = datastores;
	TSCommandProcessor processor = (TSCommandProcessor)__command.getCommandProcessor();
    __working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( processor, __command );

	addWindowListener(this);

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout(new GridBagLayout());
	getContentPane().add ("North", main_JPanel);
	int y = -1;

	JPanel paragraph = new JPanel();
	paragraph.setLayout(new GridBagLayout());
	int yy = -1;
	
   	JGUIUtil.addComponent(paragraph, new JLabel (
        "This command creates an HTML data dictionary for the specified database datastore."),
        0, ++yy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "Metadata such as column names, types, and descriptions are read from the database."),
        0, ++yy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "Some databases may not support features that allow metadata to be determined."),
        0, ++yy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);

	JGUIUtil.addComponent(main_JPanel, paragraph,
		0, ++y, 8, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
    // List available data stores of the correct type
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Datastore:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStore_JComboBox = new SimpleJComboBox ( true ); // Editable to allow properties to be specified
    __DataStore_JComboBox.setToolTipText("Select the datastore to use as input, can use ${Property}");
    // Copy the list of datastore names to internal list.
    List<String> datastoreChoices = new ArrayList<>();
    for ( DataStore dataStore : this.datastores ) {
    	datastoreChoices.add(dataStore.getName());
    }
    // Also list any substitute datastore names so the original or substitute can be used.
    HashMap<String,String> datastoreSubstituteMap = processor.getDataStoreSubstituteMap();
    for ( Map.Entry<String,String> set : datastoreSubstituteMap.entrySet() ) {
    	boolean found = false;
    	for ( String choice : datastoreChoices ) {
    		if ( choice.equals(set.getKey()) ) {
    			// The substitute original name matches a datastore name so also add the alias.
    			found = true;
    			break;
    		}
    	}
    	if ( found ) {
    		datastoreChoices.add(set.getValue());
    	}
    }
    Collections.sort(datastoreChoices, String.CASE_INSENSITIVE_ORDER);
    if ( datastoreChoices.size() == 0 ) {
        // Add an empty item so users can at least bring up the editor
    	datastoreChoices.add ( "" );
    }
    __DataStore_JComboBox.setData(datastoreChoices);
    __DataStore_JComboBox.select ( 0 );
    __DataStore_JComboBox.addItemListener ( this );
    __DataStore_JComboBox.getJTextComponent().addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __DataStore_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - datastore of interest."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    __main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Panel for tables
    int yTable = -1;
    JPanel table_JPanel = new JPanel();
    table_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Tables", table_JPanel );
    
    JGUIUtil.addComponent(table_JPanel, new JLabel (
        "Reference tables will by output in their entirety in the data dictionary."),
        0, ++yTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel (
        "Tables that are excluded below won't be included in the data dictionary."),
        0, ++yTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++yTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Reference tables:"), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ReferenceTables_JTextField = new JTextField (10);
    __ReferenceTables_JTextField.setToolTipText("Specify the list of reference tables, separated by commas, can use ${Property}");
    __ReferenceTables_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __ReferenceTables_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Optional - names of reference tables (default=none)."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Exclude tables:"), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ExcludeTables_JTextField = new JTextField (20);
    __ExcludeTables_JTextField.setToolTipText("Specify the list of tables to exclude, separated by commas, can use ${Property}");
    __ExcludeTables_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __ExcludeTables_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Optional - tables to exclude, *=wildcard (default=include all)."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    // Panel for metadata

    int yMeta = -1;
    JPanel meta_JPanel = new JPanel();
    meta_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Metadata", meta_JPanel );
    
    JGUIUtil.addComponent(meta_JPanel, new JLabel (
        "Metadata such as descriptions for tables and columns are automatically read from the database schema, if possible."),
        0, ++yMeta, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(meta_JPanel, new JLabel (
        "However, some database technologies do not store sufficient metadata to complete the data dictionary."),
        0, ++yMeta, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(meta_JPanel, new JLabel (
        "Use the following parameters to provide metadata about tables and table columns."),
        0, ++yMeta, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(meta_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++yMeta, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(meta_JPanel, new JLabel ("Database metadata table for tables:"), 
        0, ++yMeta, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStoreMetaTableForTables_JTextField = new JTextField (10);
    __DataStoreMetaTableForTables_JTextField.setToolTipText("Name of datastore table containing table metadata (id, name, description), can use ${Property}");
    __DataStoreMetaTableForTables_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(meta_JPanel, __DataStoreMetaTableForTables_JTextField,
        1, yMeta, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(meta_JPanel, new JLabel ("Optional - table metadata (default=none)."),
        3, yMeta, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(meta_JPanel, new JLabel ("Database metadata table for columns:"), 
        0, ++yMeta, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStoreMetaTableForColumns_JTextField = new JTextField (20);
    __DataStoreMetaTableForColumns_JTextField.setToolTipText("Name of datastore table containing column metadata (id, table_id, name, description), can use ${Property}");
    __DataStoreMetaTableForColumns_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(meta_JPanel, __DataStoreMetaTableForColumns_JTextField,
        1, yMeta, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(meta_JPanel, new JLabel ("Optional - column metadata (default=include all)."),
        3, yMeta, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(meta_JPanel, new JLabel ("Metadata table for tables:"), 
        0, ++yMeta, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MetaTableForTables_JTextField = new JTextField (10);
    __MetaTableForTables_JTextField.setToolTipText("Name of TSTool table containing table metadata (id, name, description), can use ${Property}");
    __MetaTableForTables_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(meta_JPanel, __MetaTableForTables_JTextField,
        1, yMeta, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(meta_JPanel, new JLabel ("Optional - table metadata (default=none)."),
        3, yMeta, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(meta_JPanel, new JLabel ("Metadata table for columns:"), 
        0, ++yMeta, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MetaTableForColumns_JTextField = new JTextField (20);
    __MetaTableForColumns_JTextField.setToolTipText("Name of TSTool table containing column metadata (id, table_id, name, description), can use ${Property}");
    __MetaTableForColumns_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(meta_JPanel, __MetaTableForColumns_JTextField,
        1, yMeta, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(meta_JPanel, new JLabel ("Optional - column metadata (default=include all)."),
        3, yMeta, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    // Panel for data dictionary
    int yDict = -1;
    JPanel dict_JPanel = new JPanel();
    dict_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Data Dictionary", dict_JPanel );
    
    JGUIUtil.addComponent(dict_JPanel, new JLabel (
        "Specify the output file and how to format content of the dictionary."),
        0, ++yDict, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(dict_JPanel, new JLabel (
        "If comments (remarks) are defined with surrounding <html> and </html> the content will be passed through to HTML output."),
        0, ++yDict, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(dict_JPanel, new JLabel (
        "Parameters are provided to format comment/remark content in HTML output."),
        0, ++yDict, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(dict_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++yDict, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(dict_JPanel, new JLabel ( "Output file:" ), 
        0, ++yDict, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputFile_JTextField = new JTextField ( 50 );
    __OutputFile_JTextField.setToolTipText("Specify an output file for the data dictionary as *.hmtl, can use ${Property}");
    __OutputFile_JTextField.addKeyListener ( this );
    // Output file layout fights back with other rows so put in its own panel
	JPanel OutputFile_JPanel = new JPanel();
	OutputFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(OutputFile_JPanel, __OutputFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );
	__browse_JButton = new SimpleJButton ( "...", this );
	__browse_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(OutputFile_JPanel, __browse_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__path_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(OutputFile_JPanel, __path_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	}
	JGUIUtil.addComponent(dict_JPanel, OutputFile_JPanel,
		1, yDict, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        
    JGUIUtil.addComponent(dict_JPanel, new JLabel ( "Surround with <pre></pre>?:" ), 
        0, ++yDict, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SurroundWithPre_JComboBox = new SimpleJComboBox ( false );
    List<String> preChoices = new ArrayList<String>(3);
    preChoices.add("");
    preChoices.add(__command._False);
    preChoices.add(__command._True);
    __SurroundWithPre_JComboBox.setData ( preChoices );
    __SurroundWithPre_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(dict_JPanel, __SurroundWithPre_JComboBox,
        1, yDict, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(dict_JPanel,
    	new JLabel( "Optional - surround content with <pre></pre> (default=" + __command._False + ")."), 
        3, yDict, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(dict_JPanel, new JLabel ("Newline:"), 
        0, ++yDict, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Newline_JTextField = new JTextField (10);
    __Newline_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(dict_JPanel, __Newline_JTextField,
        1, yDict, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(dict_JPanel, new JLabel ("Optional - string to replace newlines (default=blank)."),
        3, yDict, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(dict_JPanel, new JLabel ( "Encode HTML characters?:" ), 
        0, ++yDict, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __EncodeHtmlChars_JComboBox = new SimpleJComboBox ( false );
    List<String> encodeChoices = new ArrayList<String>(3);
    encodeChoices.add("");
    encodeChoices.add(__command._False);
    encodeChoices.add(__command._True);
    __EncodeHtmlChars_JComboBox.setData ( encodeChoices );
    __EncodeHtmlChars_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(dict_JPanel, __EncodeHtmlChars_JComboBox,
        1, yDict, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(dict_JPanel,
    	new JLabel( "Optional - encode < >, etc. to protect in HTML (default=" + __command._True + ")."), 
        3, yDict, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        
    // Panel for entity relationship (ER) diagram
    int yDiag = -1;
    JPanel diag_JPanel = new JPanel();
    diag_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Entity-Relationship Diagram", diag_JPanel );

    JGUIUtil.addComponent(diag_JPanel, new JLabel (
        "<html><b>Features to create an Entity Relation Diagram are under development.</b></html>"),
        0, ++yDiag, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(diag_JPanel, new JLabel (
        "In the future this command will use an input table with table name and XY coordinates as input to create the diagram."),
        0, ++yDiag, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(diag_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++yDiag, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(diag_JPanel, new JLabel ( "Layout table ID:" ), 
        0, ++yDiag, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ERDiagramLayoutTableID_JComboBox = new SimpleJComboBox ( 12, true ); // Allow edit to specify property
    __ERDiagramLayoutTableID_JComboBox.setToolTipText("Specify the table name containing table and XY coordinates for diagram, can use ${Property}");
    tableIDChoices.add(0,""); // Add blank to ignore table
    __ERDiagramLayoutTableID_JComboBox.setData ( tableIDChoices );
    __ERDiagramLayoutTableID_JComboBox.addItemListener ( this );
    __ERDiagramLayoutTableID_JComboBox.getJTextComponent().addKeyListener ( this );
    //__TableID_JComboBox.setMaximumRowCount(tableIDChoices.size());
    JGUIUtil.addComponent(diag_JPanel, __ERDiagramLayoutTableID_JComboBox,
        1, yDiag, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(diag_JPanel, new JLabel( "Required - table containing ER diagram layout data."), 
        3, yDiag, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(diag_JPanel, new JLabel ("Layout table name column:"), 
        0, ++yDiag, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ERDiagramLayoutTableNameColumn_JTextField = new JTextField (10);
    __ERDiagramLayoutTableNameColumn_JTextField.setToolTipText("Specify the layout table column for table names, can use ${Property}");
    __ERDiagramLayoutTableNameColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(diag_JPanel, __ERDiagramLayoutTableNameColumn_JTextField,
        1, yDiag, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(diag_JPanel, new JLabel ("Required - name of column containing table names."),
        3, yDiag, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(diag_JPanel, new JLabel ("Layout table X column:"), 
        0, ++yDiag, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ERDiagramLayoutTableXColumn_JTextField = new JTextField (20);
    __ERDiagramLayoutTableXColumn_JTextField.setToolTipText("Specify the layout table column containing X, can use ${Property}");
    __ERDiagramLayoutTableXColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(diag_JPanel, __ERDiagramLayoutTableXColumn_JTextField,
        1, yDiag, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(diag_JPanel, new JLabel ("Required - name of X-coordinate column."),
        3, yDiag, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(diag_JPanel, new JLabel ("Layout table Y column:"), 
        0, ++yDiag, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ERDiagramLayoutTableYColumn_JTextField = new JTextField (20);
    __ERDiagramLayoutTableYColumn_JTextField.setToolTipText("Specify the layout table column containing Y, can use ${Property}");
    __ERDiagramLayoutTableYColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(diag_JPanel, __ERDiagramLayoutTableYColumn_JTextField,
        1, yDiag, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(diag_JPanel, new JLabel ("Required - name of Y-coordinate column."),
        3, yDiag, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(diag_JPanel, new JLabel ( "Page size:"),
        0, ++yDiag, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ERDiagramPageSize_JComboBox = new SimpleJComboBox ( false );
    List<String> pageSizeChoices = new ArrayList<String>();
    pageSizeChoices.add ( "" ); // Default
    // TODO SAM 2015-05-11 Need to fill dynamically - can it be done independent of printer?
    pageSizeChoices.add ( "A" );
    pageSizeChoices.add ( "B" );
    pageSizeChoices.add ( "C" );
    pageSizeChoices.add ( "D" );
    pageSizeChoices.add ( "E" );
    __ERDiagramPageSize_JComboBox.setData(pageSizeChoices);
    // TODO SAM 2011-06-24 Get from a PrinterJob
    __ERDiagramPageSize_JComboBox.select ( 0 );
    __ERDiagramPageSize_JComboBox.addActionListener ( this );
   JGUIUtil.addComponent(diag_JPanel, __ERDiagramPageSize_JComboBox,
        1, yDiag, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(diag_JPanel, new JLabel(
        "Optional - page size name (default=11x17 [B])."), 
        3, yDiag, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(diag_JPanel, new JLabel ( "Orientation:"),
        0, ++yDiag, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ERDiagramOrientation_JComboBox = new SimpleJComboBox ( false );
    List<String> orientChoices = new ArrayList<String>();
    orientChoices.add ( "" ); // Default
    orientChoices.add ( PrintUtil.getOrientationAsString(PageFormat.LANDSCAPE) );
    orientChoices.add ( PrintUtil.getOrientationAsString(PageFormat.PORTRAIT) );
    __ERDiagramOrientation_JComboBox.setData(orientChoices);
    __ERDiagramOrientation_JComboBox.select ( 0 );
    __ERDiagramOrientation_JComboBox.addActionListener ( this );
   JGUIUtil.addComponent(diag_JPanel, __ERDiagramOrientation_JComboBox,
        1, yDiag, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(diag_JPanel, new JLabel(
        "Optional - page orientation (default=" + PrintUtil.getOrientationAsString(PageFormat.LANDSCAPE) + ")."), 
        3, yDiag, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(diag_JPanel, new JLabel ( "View diagram:" ), 
		0, ++yDiag, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__ViewERDiagram_JComboBox = new SimpleJComboBox ( false );
	__ViewERDiagram_JComboBox.add ( "" );
	__ViewERDiagram_JComboBox.add ( __command._False );
	__ViewERDiagram_JComboBox.add ( __command._True );
	__ViewERDiagram_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(diag_JPanel, __ViewERDiagram_JComboBox,
		1, yDiag, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(diag_JPanel,
		new JLabel ( "Optional - display ER diagram in window (default=" + __command._False + ")." ), 
		2, yDiag, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    //JGUIUtil.addComponent(diag_JPanel, new JLabel ( "Output file:" ), 
    //    0, ++yDict, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    //__OutputFile_JTextField = new JTextField ( 50 );
    //__OutputFile_JTextField.setToolTipText("Specify an output file for the data dictionary as *.hmtl");
    //__OutputFile_JTextField.addKeyListener ( this );
    //    JGUIUtil.addComponent(diag_JPanel, __OutputFile_JTextField,
    //    1, yDict, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    __view_JButton = new SimpleJButton ( "View Diagram", this );
    JGUIUtil.addComponent(diag_JPanel, __view_JButton,
        1, ++yDiag, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
        
     JGUIUtil.addComponent(main_JPanel, new JLabel ("Command:"), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea (6,50);
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.setEditable (false);
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Refresh the contents...
	refresh ();

	// South JPanel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	__ok_JButton = new SimpleJButton("OK", this);
	__ok_JButton.setToolTipText("Save changes to command");
	button_JPanel.add (__ok_JButton);
	__cancel_JButton = new SimpleJButton("Cancel", this);
	button_JPanel.add (__cancel_JButton);
	__cancel_JButton.setToolTipText("Cancel without saving changes to command");
	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");

	setTitle ( "Edit " + __command.getCommandName() + " Command");
    pack();
    JGUIUtil.center(this);
	refresh();
	setResizable (false);
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
            // User has selected a datastore.
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
Refresh the command from the other text field contents.
*/
private void refresh ()
{	String routine = getClass().getSimpleName() + ".refresh";
try{
    String DataStore = "";
    String ReferenceTables = "";
    String ExcludeTables = "";
    String DataStoreMetaTableForTables = "";
    String DataStoreMetaTableForColumns = "";
    String MetaTableForTables = "";
    String MetaTableForColumns = "";
    String OutputFile = "";
    String Newline = "";
    String SurroundWithPre = "";
    String EncodeHtmlChars = "";
    String ERDiagramLayoutTableID = "";
    String ERDiagramLayoutTableNameColumn = "";
    String ERDiagramLayoutTableXColumn = "";
    String ERDiagramLayoutTableYColumn = "";
    String ERDiagramPageSize = "";
    String ERDiagramOrientation = "";
    String ViewERDiagram = "";
	PropList props = __command.getCommandParameters();
	if (__first_time) {
		__first_time = false;
		DataStore = props.getValue ( "DataStore" );
		ReferenceTables = props.getValue ( "ReferenceTables" );
		ExcludeTables = props.getValue ( "ExcludeTables" );
		DataStoreMetaTableForTables = props.getValue ( "DataStoreMetaTableForTables" );
		DataStoreMetaTableForColumns = props.getValue ( "DataStoreMetaTableForColumns" );
		MetaTableForTables = props.getValue ( "MetaTableForTables" );
		MetaTableForColumns = props.getValue ( "MetaTableForColumns" );
		OutputFile = props.getValue ( "OutputFile" );
		Newline = props.getValue ( "Newline" );
		SurroundWithPre = props.getValue ( "SurroundWithPre" );
		EncodeHtmlChars = props.getValue ( "EncodeHtmlChars" );
		ERDiagramLayoutTableID = props.getValue ( "ERDiagramLayoutTableID" );
		ERDiagramLayoutTableXColumn = props.getValue ( "ERDiagramLayoutTableXColumn" );
		ERDiagramLayoutTableNameColumn = props.getValue ( "ERDiagramLayoutTableNameColumn" );
		ERDiagramLayoutTableYColumn = props.getValue ( "ERDiagramLayoutTableYColumn" );
		ERDiagramPageSize = props.getValue ( "ERDiagramPageSize" );
		ERDiagramOrientation = props.getValue ( "ERDiagramOrientation" );
		ViewERDiagram = props.getValue ( "ViewERDiagram" );
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
            else if ( DataStore.indexOf("${") >= 0 ) {
        		// OK to add as first position and select
        		__DataStore_JComboBox.insertItemAt(DataStore, 1);
        		__DataStore_JComboBox.select(1);
        	}
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "DataStore parameter \"" + DataStore + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        if ( (ReferenceTables != null) && !ReferenceTables.isEmpty() ) {
            __ReferenceTables_JTextField.setText ( ReferenceTables );
        }
        if ( (ExcludeTables != null) && !ExcludeTables.isEmpty() ) {
            __ExcludeTables_JTextField.setText ( ExcludeTables );
        }
        if ( (DataStoreMetaTableForTables != null) && !DataStoreMetaTableForTables.isEmpty() ) {
            __DataStoreMetaTableForTables_JTextField.setText ( DataStoreMetaTableForTables );
        }
        if ( (DataStoreMetaTableForColumns != null) && !DataStoreMetaTableForColumns.isEmpty() ) {
            __DataStoreMetaTableForColumns_JTextField.setText ( DataStoreMetaTableForColumns );
        }
        if ( (MetaTableForTables != null) && !MetaTableForTables.isEmpty() ) {
            __MetaTableForTables_JTextField.setText ( MetaTableForTables );
        }
        if ( (MetaTableForColumns != null) && !MetaTableForColumns.isEmpty() ) {
            __MetaTableForColumns_JTextField.setText ( MetaTableForColumns );
        }
        if ( (OutputFile != null) && !OutputFile.isEmpty() ) {
            __OutputFile_JTextField.setText(OutputFile);
        }
        if ( (Newline != null) && !Newline.isEmpty() ) {
            __Newline_JTextField.setText(Newline);
        }
        if ( SurroundWithPre == null ) {
            // Select default...
            __SurroundWithPre_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __SurroundWithPre_JComboBox,SurroundWithPre, JGUIUtil.NONE, null, null ) ) {
                __SurroundWithPre_JComboBox.select ( SurroundWithPre );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nSurroundWithPre value \"" + SurroundWithPre +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( EncodeHtmlChars == null ) {
            // Select default...
            __EncodeHtmlChars_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __EncodeHtmlChars_JComboBox,EncodeHtmlChars, JGUIUtil.NONE, null, null ) ) {
                __EncodeHtmlChars_JComboBox.select ( EncodeHtmlChars );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nEncodeHtmlChars value \"" + EncodeHtmlChars +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( ERDiagramLayoutTableID == null ) {
            // Select default...
            __ERDiagramLayoutTableID_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __ERDiagramLayoutTableID_JComboBox,ERDiagramLayoutTableID, JGUIUtil.NONE, null, null ) ) {
                __ERDiagramLayoutTableID_JComboBox.select ( ERDiagramLayoutTableID );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nERDiagramLayoutTableID value \"" + ERDiagramLayoutTableID +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( (ERDiagramLayoutTableNameColumn != null) && !ERDiagramLayoutTableNameColumn.isEmpty() ) {
            __ERDiagramLayoutTableNameColumn_JTextField.setText(ERDiagramLayoutTableNameColumn);
        }
        if ( (ERDiagramLayoutTableXColumn != null) && !ERDiagramLayoutTableXColumn.isEmpty() ) {
            __ERDiagramLayoutTableXColumn_JTextField.setText(ERDiagramLayoutTableXColumn);
        }
        if ( (ERDiagramLayoutTableYColumn != null) && !ERDiagramLayoutTableYColumn.isEmpty() ) {
            __ERDiagramLayoutTableYColumn_JTextField.setText(ERDiagramLayoutTableYColumn);
        }
        if ( ERDiagramPageSize == null ) {
            // Select default...
            __ERDiagramPageSize_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __ERDiagramPageSize_JComboBox,ERDiagramPageSize, JGUIUtil.NONE, null, null ) ) {
                __ERDiagramPageSize_JComboBox.select ( ERDiagramPageSize );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nERDiagramPageSize value \"" + ERDiagramPageSize +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( ERDiagramOrientation == null ) {
            // Select default...
            __ERDiagramOrientation_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __ERDiagramOrientation_JComboBox,ERDiagramOrientation, JGUIUtil.NONE, null, null ) ) {
                __ERDiagramOrientation_JComboBox.select ( ERDiagramOrientation );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nERDiagramOrientation value \"" + ERDiagramOrientation +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( (OutputFile != null) && !OutputFile.equals("") ) {
            __OutputFile_JTextField.setText(OutputFile);
        }
		if ( (ViewERDiagram == null) || ViewERDiagram.isEmpty() ) {
			// Select default...
			__ViewERDiagram_JComboBox.select ( 0 );
		}
		else {
		    if ( JGUIUtil.isSimpleJComboBoxItem( __ViewERDiagram_JComboBox,
		    	ViewERDiagram, JGUIUtil.NONE, null, null ) ) {
				__ViewERDiagram_JComboBox.select ( ViewERDiagram );
			}
			else {
			    Message.printWarning ( 1,
				routine, "Existing command references an invalid\n"+
				"ViewERDiagram parameter \"" + ViewERDiagram +
				"\".  Select a different value or Cancel." );
			}
		}
	}
	// Regardless, reset the command from the fields...
    DataStore = __DataStore_JComboBox.getSelected();
    if ( DataStore == null ) {
        DataStore = "";
    }
	ReferenceTables = __ReferenceTables_JTextField.getText().trim();
	ExcludeTables = __ExcludeTables_JTextField.getText().trim();
	DataStoreMetaTableForTables = __DataStoreMetaTableForTables_JTextField.getText().trim();
	DataStoreMetaTableForColumns = __DataStoreMetaTableForColumns_JTextField.getText().trim();
	MetaTableForTables = __MetaTableForTables_JTextField.getText().trim();
	MetaTableForColumns = __MetaTableForColumns_JTextField.getText().trim();
	OutputFile = __OutputFile_JTextField.getText().trim();
	Newline = __Newline_JTextField.getText().trim();
	SurroundWithPre = __SurroundWithPre_JComboBox.getSelected();
	EncodeHtmlChars = __EncodeHtmlChars_JComboBox.getSelected();
	ERDiagramLayoutTableID = __ERDiagramLayoutTableID_JComboBox.getSelected();
	ERDiagramLayoutTableNameColumn = __ERDiagramLayoutTableNameColumn_JTextField.getText().trim();
	ERDiagramLayoutTableXColumn = __ERDiagramLayoutTableXColumn_JTextField.getText().trim();
	ERDiagramLayoutTableYColumn = __ERDiagramLayoutTableYColumn_JTextField.getText().trim();
	ERDiagramPageSize = __ERDiagramPageSize_JComboBox.getSelected();
	ERDiagramOrientation = __ERDiagramOrientation_JComboBox.getSelected();
	ViewERDiagram = __ViewERDiagram_JComboBox.getSelected();
	props = new PropList ( __command.getCommandName() );
	props.add ( "DataStore=" + DataStore );
	props.add ( "ReferenceTables=" + ReferenceTables );
	props.add ( "ExcludeTables=" + ExcludeTables );
	props.add ( "DataStoreMetaTableForTables=" + DataStoreMetaTableForTables );
	props.add ( "DataStoreMetaTableForColumns=" + DataStoreMetaTableForColumns );
	props.add ( "MetaTableForTables=" + MetaTableForTables );
	props.add ( "MetaTableForColumns=" + MetaTableForColumns );
	props.add ( "OutputFile=" + OutputFile);
	props.add ( "Newline=" + Newline);
	props.add ( "SurroundWithPre=" + SurroundWithPre);
	props.add ( "EncodeHtmlChars=" + EncodeHtmlChars);
	props.add ( "ERDiagramLayoutTableID=" + ERDiagramLayoutTableID);
	props.add ( "ERDiagramLayoutTableNameColumn=" + ERDiagramLayoutTableNameColumn);
	props.add ( "ERDiagramLayoutTableXColumn=" + ERDiagramLayoutTableXColumn);
	props.add ( "ERDiagramLayoutTableYColumn=" + ERDiagramLayoutTableYColumn);
	props.add ( "ERDiagramPageSize=" + ERDiagramPageSize);
	props.add ( "ERDiagramOrientation=" + ERDiagramOrientation);
	props.add ( "ViewERDiagram=" + ViewERDiagram);
	__command_JTextArea.setText( __command.toString ( props ) );
	// Check the path and determine what the label on the path button should be...
	if ( __path_JButton != null ) {
		if ( (OutputFile != null) && !OutputFile.isEmpty() ) {
			__path_JButton.setEnabled ( true );
			File f = new File ( OutputFile );
			if ( f.isAbsolute() ) {
				__path_JButton.setText ( __RemoveWorkingDirectory );
				__path_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
            	__path_JButton.setText ( __AddWorkingDirectory );
            	__path_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__path_JButton.setEnabled(false);
		}
	}
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
Display/view the ER diagram.
*/
private void viewDiagram ()
{
	String tablesTableName = "";
	String tableNameField = "";
	String erdXField = "";
	String erdYField = "";
	PageFormat pageFormat = new PageFormat();
	pageFormat.setOrientation(PageFormat.LANDSCAPE);
	Paper paper = new Paper();
	pageFormat.setPaper(paper);
	DMI dmi = getDMI();
	if ( dmi == null ) {
		Message.printWarning(1,"","Database connection is unavailable.  Can't display diagram.");
	}
	else {
		new ERDiagram_JFrame ( getDMI(), tablesTableName, tableNameField, erdXField, erdYField, pageFormat );
	}
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
