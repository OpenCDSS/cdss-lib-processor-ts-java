// ReadWaterOneFlow_JDialog - Editor for the ReadWaterOneFlow() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2025 Colorado Department of Natural Resources

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

package rti.tscommandprocessor.commands.wateroneflow.ws;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
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
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Editor for the ReadWaterOneFlow() command.
*/
@SuppressWarnings("serial")
public class ReadWaterOneFlow_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{

private final String __AddWorkingDirectory = "Add Working Directory";
private final String __RemoveWorkingDirectory = "Remove Working Directory";

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __browse_JButton = null;
private SimpleJButton __path_JButton = null;
private ReadWaterOneFlow_Command __command = null;
private SimpleJComboBox __DataStore_JComboBox = null;
private JTextField __NetworkName_JTextField;
private JTextField __SiteID_JTextField;
private SimpleJComboBox __Variable_JComboBox = null;
private SimpleJComboBox __Interval_JComboBox = null;
private JTextField __InputStart_JTextField;
private JTextField __InputEnd_JTextField;
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
private JTextField __OutputFile_JTextField = null;
			
private JTextArea __command_JTextArea = null; // Command as JTextArea
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK was pressed when closing the dialog.
private String __working_dir = null; // Working directory.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public ReadWaterOneFlow_JDialog ( JFrame parent, ReadWaterOneFlow_Command command )
{	super(parent, true);

	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
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
        fc.setDialogTitle("Select Time Series File to Write");
        SimpleFileFilter sff = new SimpleFileFilter("xml", "WaterML Time Series File");
        fc.addChoosableFileFilter(sff);
        sff = new SimpleFileFilter("waterml", "WaterML Time Series File");
        fc.addChoosableFileFilter(sff);
        
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String directory = fc.getSelectedFile().getParent();
            String filename = fc.getSelectedFile().getName(); 
            String path = fc.getSelectedFile().getPath(); 
    
            if (filename == null || filename.equals("")) {
                return;
            }
    
            if (path != null) {
                __OutputFile_JTextField.setText(path );
                JGUIUtil.setLastFileDialogDirectory(directory );
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
			response ( true );
		}
	}
    else if ( o == __path_JButton ) {
        if ( __path_JButton.getText().equals(__AddWorkingDirectory) ) {
            __OutputFile_JTextField.setText (
            IOUtil.toAbsolutePath(__working_dir, __OutputFile_JTextField.getText() ) );
        }
        else if ( __path_JButton.getText().equals(__RemoveWorkingDirectory) ) {
            try {
                __OutputFile_JTextField.setText (
                IOUtil.toRelativePath ( __working_dir, __OutputFile_JTextField.getText() ) );
            }
            catch ( Exception e ) {
                Message.printWarning ( 1, "ReadWaterOneFlow_JDialog", "Error converting file to relative path." );
            }
        }
        refresh ();
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
{	// Put together a list of parameters to check...
	PropList props = new PropList ( "" );
	__error_wait = false;
	String DataStore = __DataStore_JComboBox.getSelected();
	if ( (DataStore != null) && (DataStore.length() > 0) ) {
		props.set ( "DataStore", DataStore );
	}
    String NetworkName = __NetworkName_JTextField.getText().trim();
    if ( NetworkName.length() > 0 ) {
        props.set ( "NetworkName", NetworkName );
    }
    String SiteID = __SiteID_JTextField.getText().trim();
    if ( SiteID.length() > 0 ) {
        props.set ( "SiteID", SiteID );
    }
	//String Variable = StringUtil.getToken(__Variable_JComboBox.getSelected().trim(), " ", 0, 0 );
    String Variable = __Variable_JComboBox.getSelected();
    if ( Variable.length() > 0 ) {
        props.set ( "Variable", Variable );
    }
    String Interval = __Interval_JComboBox.getSelected();
    if ( (Interval != null) && (Interval.length() > 0) ) {
        props.set ( "Interval", Interval );
    }
    /* TODO SAM 2012-02-28 Evaluate whether to enable
	int numWhere = __inputFilter_JPanel.getNumFilterGroups();
	for ( int i = 1; i <= numWhere; i++ ) {
	    String where = getWhere ( i - 1 );
	    if ( where.length() > 0 ) {
	        props.set ( "Where" + i, where );
	    }
    }
    */
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
    String OutputFile = __OutputFile_JTextField.getText().trim();
    if ( OutputFile.length() > 0 ) {
        props.set ( "OutputFile", OutputFile );
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
    String NetworkName = __NetworkName_JTextField.getText().trim();
    String SiteID = __SiteID_JTextField.getText().trim();
    String Variable = __Variable_JComboBox.getSelected();
    String Interval = __Interval_JComboBox.getSelected();
    String InputStart = __InputStart_JTextField.getText().trim();
    String InputEnd = __InputEnd_JTextField.getText().trim();
    String Alias = __Alias_JTextField.getText().trim();
    String OutputFile = __OutputFile_JTextField.getText().trim();
	__command.setCommandParameter ( "DataStore", DataStore );
	__command.setCommandParameter ( "NetworkName", NetworkName );
	__command.setCommandParameter ( "SiteID", SiteID );
	__command.setCommandParameter ( "Variable", Variable );
	__command.setCommandParameter ( "Interval", Interval );
	__command.setCommandParameter ( "InputStart", InputStart );
    __command.setCommandParameter ( "InputEnd", InputEnd );
    __command.setCommandParameter ( "Alias", Alias );
    __command.setCommandParameter ( "OutputFile", OutputFile );
	/* TODO SAM 2012-02-28 Evaluate whether to enable
	String delim = ";";
	int numWhere = __inputFilter_JPanel.getNumFilterGroups();
	for ( int i = 1; i <= numWhere; i++ ) {
	    String where = getWhere ( i - 1 );
	    if ( where.startsWith(delim) ) {
	        where = "";
	    }
	    __command.setCommandParameter ( "Where" + i, where );
	}
	*/
}

/**
Get the selected data store.
*/
private WaterOneFlowDataStore getSelectedDataStore ()
{   String routine = getClass().getName() + ".getSelectedDataStore";
    String DataStore = __DataStore_JComboBox.getSelected();
    WaterOneFlowDataStore dataStore = (WaterOneFlowDataStore)((TSCommandProcessor)
        __command.getCommandProcessor()).getDataStoreForName(
        DataStore, WaterOneFlowDataStore.class );
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
/*
private String getWhere ( int ifg )
{
	// TODO SAM 2006-04-24 Need to enable other input filter panels
	String delim = ";";	// To separate input filter parts
	InputFilter_JPanel filter_panel = __inputFilter_JPanel;
	String where = filter_panel.toString(ifg,delim).trim();
	return where;
}
*/

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, ReadWaterOneFlow_Command command )
{	//String routine = getClass().getName() + ".initialize";
	__command = command;
	CommandProcessor processor = __command.getCommandProcessor();
	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( processor, __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "<html><b>This command is under development.</b></html>"),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"Read one or more time series from a WaterOneFlow web service."),
    	0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Refer to the WaterOneFlow Data Store documentation for more information." ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"<html>Constrain the query by specifying time series metadata to match.  " +
    	"<b>A location constraint must be specified.</b></html>" ), 
    	0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"If not specified, the input period defaults to the input period from SetInputPeriod()."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	
   	// List available data stores of the correct type
   	
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data store:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStore_JComboBox = new SimpleJComboBox ( false );
    TSCommandProcessor tsProcessor = (TSCommandProcessor)processor;
    List<DataStore> dataStoreList = tsProcessor.getDataStoresByType( WaterOneFlowDataStore.class );
    List<String> datastoreList = new ArrayList<String>();
    for ( DataStore dataStore: dataStoreList ) {
    	datastoreList.add ( dataStore.getName() );
    }
    if ( dataStoreList.size() > 0 ) {
    	__DataStore_JComboBox.setData(datastoreList);
        __DataStore_JComboBox.select ( 0 );
    }
    __DataStore_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __DataStore_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - data store containing data."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Network name:"), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NetworkName_JTextField = new JTextField (20);
    __NetworkName_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __NetworkName_JTextField,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Required - data network name."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Site ID:"), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SiteID_JTextField = new JTextField (20);
    __SiteID_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __SiteID_JTextField,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Required - site identifier."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Data types are particular to the data store...
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Variable:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Variable_JComboBox = new SimpleJComboBox ( false );
    populateVariableChoices();
    __Variable_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Variable_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - variable (data type) for time series."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Intervals are hard-coded
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data interval:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Interval_JComboBox = new SimpleJComboBox ( false );
    setIntervalChoices();
    __Interval_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Interval_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - data interval (time step) for time series."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	
   	// Input filters
    // TODO SAM 2010-11-02 Need to use SetInputFilters() so the filters can change when a
    // data store is selected.  For now it is OK because the input filters do not provide choices.
    /** TODO SAM need to enable
	int buffer = 3;
	Insets insets = new Insets(0,buffer,0,0);
	try {
	    // Add input filters for ReclamationHDB time series...
		__inputFilter_JPanel = new RccAcis_TimeSeries_InputFilter_JPanel(
		    getSelectedDataStore(), __command.getNumFilterGroups() );
		JGUIUtil.addComponent(main_JPanel, __inputFilter_JPanel,
			0, ++y, 7, 1, 0.0, 0.0, insets, GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST );
   		__inputFilter_JPanel.addEventListeners ( this );
   	    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - query filters."),
   	        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	}
	catch ( Exception e ) {
		Message.printWarning ( 2, routine, "Unable to initialize RCC ACIS input filter." );
		Message.printWarning ( 2, routine, e );
	}
	*/

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Input start:"), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputStart_JTextField = new JTextField (20);
    __InputStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __InputStart_JTextField,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - YYYY-MM-DD, override the global input start."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Input end:"), 
        0, ++y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputEnd_JTextField = new JTextField (20);
    __InputEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __InputEnd_JTextField,
        1, y, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - YYYY-MM-DD, override the global input end."),
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
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Output file to write:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputFile_JTextField = new JTextField ( 50 );
    __OutputFile_JTextField.addKeyListener ( this );
    __OutputFile_JTextField.setToolTipText (
        "Optional output file to save time series data, which can be read by other commands");
    JGUIUtil.addComponent(main_JPanel, __OutputFile_JTextField,
        1, y, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    __browse_JButton = new SimpleJButton ( "Browse", this );
    JGUIUtil.addComponent(main_JPanel, __browse_JButton,
        6, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);

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

    if ( __working_dir != null ) {
        // Add the button to allow conversion to/from relative path...
        __path_JButton = new SimpleJButton( __RemoveWorkingDirectory, __RemoveWorkingDirectory, this);
        button_JPanel.add ( __path_JButton );
    }
	__cancel_JButton = new SimpleJButton( "Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__ok_JButton = new SimpleJButton("OK", this);
	button_JPanel.add ( __ok_JButton );

	setTitle ( "Edit " + __command.getCommandName() + " Command" );

	// Dialogs do not need to be resizable...
	setResizable ( true );
    pack();
    JGUIUtil.center( this );
	refresh();	// Sets the __path_JButton status
    super.setVisible( true );
}

/**
Handle ItemListener events.
*/
public void itemStateChanged ( ItemEvent event )
{
    // If a new data store has been selected, update the data type, interval, list and the input filter
    if ( event.getSource() == __DataStore_JComboBox ) {
        populateVariableChoices();
        setIntervalChoices();
        setInputFilters();
    }
    refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	refresh();
}

/**
Need this to properly capture key events, especially deletes.
*/
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
Populate the data type choices in response to a new data store being selected.
*/
private void populateVariableChoices ()
{   String routine = getClass().getName() + ".populateVariableChoices";
    WaterOneFlowDataStore ds = getSelectedDataStore();
    List<String> variables = new Vector<String>();
    variables.add("");
    try {
        List<String> variableList = ds.getVariableList ( true, 30 );
        for ( String variable: variableList ) {
            variables.add(variable);
        }
    }
    catch ( Exception e ) {
        // Hopefully should not happen
        Message.printWarning(2, routine, "Unable to get variable list for data store \"" +
            ds.getName() + "\" - web service unavailable?");
    }
    __Variable_JComboBox.setData ( variables );
    if ( variables.size() > 0 ) {
        __Variable_JComboBox.select ( 0 );
    }
}

/**
Refresh the command string from the dialog contents.
*/
private void refresh ()
{	String routine = "ReadReclamationHDB_JDialog.refresh";
	__error_wait = false;
	String DataStore = "";
	String NetworkName = "";
	String SiteID = "";
	String Variable = "";
	String Interval = "";
	//String filter_delim = ";";
	String InputStart = "";
	String InputEnd = "";
	String Alias = "";
	String OutputFile = "";
	PropList props = null;
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		props = __command.getCommandParameters();
		DataStore = props.getValue ( "DataStore" );
		NetworkName = props.getValue ( "NetworkName" );
		SiteID = props.getValue ( "SiteID" );
		Variable = props.getValue ( "Variable" );
		Interval = props.getValue ( "Interval" );
		InputStart = props.getValue ( "InputStart" );
		InputEnd = props.getValue ( "InputEnd" );
		Alias = props.getValue ( "Alias" );
		OutputFile = props.getValue ( "OutputFile" );
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
        if ( NetworkName != null ) {
            __NetworkName_JTextField.setText ( NetworkName );
        }
        if ( SiteID != null ) {
            __SiteID_JTextField.setText ( SiteID );
        }
        // Data types as displayed are verbose:  "4 - Precipitation (daily)" but parameter uses only "4" to
        // ensure uniqueness.  Therefore, select in the list based only on the first token
        int [] index = new int[1];
        if ( JGUIUtil.isSimpleJComboBoxItem(__Variable_JComboBox, Variable, JGUIUtil.CHECK_SUBSTRINGS, " ", 0, index, true ) ) {
            __Variable_JComboBox.select ( index[0] );
        }
        else {
            if ( (Variable == null) || Variable.equals("") ) {
                // New command...select the default...
                if ( __Variable_JComboBox.getItemCount() > 0 ) {
                    __Variable_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "Variable parameter \"" + Variable + "\".  Select a\ndifferent value or Cancel." );
            }
        }
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
                  "Interval parameter \"" + DataStore + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        //InputFilter_JPanel filter_panel = __inputFilter_JPanel;
        /* TODO SAM 2012-02-28 Need to enable
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
        */
		if ( InputStart != null ) {
			__InputStart_JTextField.setText ( InputStart );
		}
		if ( InputEnd != null ) {
			__InputEnd_JTextField.setText ( InputEnd );
		}
        if ( Alias != null ) {
            __Alias_JTextField.setText ( Alias );
        }
        if ( OutputFile != null ) {
            __OutputFile_JTextField.setText ( OutputFile );
        }
	}
	// Regardless, reset the command from the fields...
	props = new PropList ( __command.getCommandName() );
	DataStore = __DataStore_JComboBox.getSelected().trim();
	NetworkName = __NetworkName_JTextField.getText().trim();
	SiteID = __SiteID_JTextField.getText().trim();
	Variable = __Variable_JComboBox.getSelected().trim();
	Interval = __Interval_JComboBox.getSelected().trim();
    InputStart = __InputStart_JTextField.getText().trim();
    InputEnd = __InputEnd_JTextField.getText().trim();
	Alias = __Alias_JTextField.getText().trim();
    props.add ( "DataStore=" + DataStore );
    props.add ( "NetworkName=" + NetworkName );
    props.add ( "SiteID=" + SiteID );
    props.add ( "Variable=" + Variable );
    props.add ( "Interval=" + Interval );
    props.add ( "InputStart=" + InputStart );
    props.add ( "InputEnd=" + InputEnd );
    props.add ( "Alias=" + Alias );
    props.add ( "OutputFile=" + OutputFile );
    /*
	// Add the where clause(s)...
	InputFilter_JPanel filter_panel = __inputFilter_JPanel;
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
	*/
	__command_JTextArea.setText( __command.toString ( props ) );
    if ( (OutputFile == null) || (OutputFile.length() == 0) ) {
        if ( __path_JButton != null ) {
            __path_JButton.setEnabled ( false );
        }
    }
    if ( __path_JButton != null ) {
        __path_JButton.setEnabled ( true );
        File f = new File ( OutputFile );
        if ( f.isAbsolute() ) {
            __path_JButton.setText ( __RemoveWorkingDirectory );
        }
        else {
            __path_JButton.setText ( __AddWorkingDirectory );
        }
    }

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
Set the data interval choices in response to a new data store being selected.
*/
private void setIntervalChoices ()
{
    __Interval_JComboBox.removeAll();
    __Interval_JComboBox.add ( "Day" );
    __Interval_JComboBox.select ( 0 );
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
