package rti.tscommandprocessor.commands.ts;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;
import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Editor for the SetTimeSeriesValuesFromTable command.
*/
@SuppressWarnings("serial")
public class SetTimeSeriesValuesFromTable_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private SetTimeSeriesValuesFromTable_Command __command = null;
private JTextArea __command_JTextArea=null;
private JTabbedPane __main_JTabbedPane = null;
private SimpleJComboBox	__TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox __TSID_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private TSFormatSpecifiersJPanel __TSIDFormat_JTextField = null; // Format for time series identifiers, to match table TSID column
private JTextField __SetStart_JTextField = null;
private JTextField __SetEnd_JTextField = null;
private JTextField __SetFlag_JTextField = null;
private JTextField __SetFlagDesc_JTextField = null;
private SimpleJComboBox __TableID_JComboBox = null;
private JTextField __TableTSIDColumn_JTextField = null;
private JTextField __TableDateTimeColumn_JTextField = null;
private JTextField __TableValueColumn_JTextField = null;
private JTextField __TableSetFlagColumn_JTextField = null;
private JTextField __TableSetFlagDescColumn_JTextField = null;
private SimpleJComboBox __SortOrder_JComboBox = null;
// TODO SAM 2015-05-25 maybe add set window later
//private JCheckBox __SetWindow_JCheckBox = null;
//private DateTime_JPanel __SetWindowStart_JPanel = null;
//private DateTime_JPanel __SetWindowEnd_JPanel = null;
private boolean	__error_wait = false; // Is there an error to be cleared up?
private boolean	__first_time = true;
private boolean	__ok = false; // Whether OK has been pressed.

/**
Command editor dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices choices for TableID value.
*/
public SetTimeSeriesValuesFromTable_JDialog ( JFrame parent,
    SetTimeSeriesValuesFromTable_Command command, List<String> tableIDChoices )
{	super(parent, true);
	initialize ( parent, command, tableIDChoices );
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
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "SetTimeSeriesValuesFromTable");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
    else {
        // A combo box.  Refresh the command...
        checkGUIState();
        refresh ();
    }
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
Check the state of the dialog, disabling/enabling components as appropriate.
*/
private void checkGUIState()
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
	
    /*
    if ( __SetWindow_JCheckBox.isSelected() ) {
        // Checked so enable the date panels
        __SetWindowStart_JPanel.setEnabled ( true );
        __SetWindowEnd_JPanel.setEnabled ( true );
    }
    else {
        __SetWindowStart_JPanel.setEnabled ( false );
        __SetWindowEnd_JPanel.setEnabled ( false );
    }*/
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
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
	PropList props = new PropList ( "" );
	String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String TSIDFormat = __TSIDFormat_JTextField.getText().trim();
	String SetStart = __SetStart_JTextField.getText().trim();
	String SetEnd = __SetEnd_JTextField.getText().trim();
	String SetFlag = __SetFlag_JTextField.getText().trim();
    String SetFlagDesc = __SetFlagDesc_JTextField.getText().trim();
    String TableID = __TableID_JComboBox.getSelected();
    String TableTSIDColumn = __TableTSIDColumn_JTextField.getText().trim();
	String TableDateTimeColumn = __TableDateTimeColumn_JTextField.getText().trim();
    String TableValueColumn = __TableValueColumn_JTextField.getText().trim();
    String TableSetFlagColumn = __TableSetFlagColumn_JTextField.getText().trim();
    String TableSetFlagDescColumn = __TableSetFlagDescColumn_JTextField.getText().trim();
    String SortOrder = __SortOrder_JComboBox.getSelected();
	__error_wait = false;

	if ( TSList.length() > 0 ) {
		props.set ( "TSList", TSList );
	}
	if ( TSID.length() > 0 ) {
		props.set ( "TSID", TSID );
	}
    if ( EnsembleID.length() > 0 ) {
    	props.set ( "EnsembleID", EnsembleID );
    }
    if ( TSIDFormat.length() > 0 ) {
        props.set ( "TableTSIDFormat", TSIDFormat );
    }
    if ( SetStart.length() > 0 ) {
        props.set ( "SetStart", SetStart );
    }
    if ( SetEnd.length() > 0 ) {
        props.set ( "SetEnd", SetEnd );
    }
	if ( SetFlag.length() > 0 ) {
		props.set ( "SetFlag", SetFlag );
	}
    if ( SetFlagDesc.length() > 0 ) {
        props.set ( "SetFlagDesc", SetFlagDesc );
    }
    if ( TableID.length() > 0 ) {
        props.set ( "TableID", TableID );
    }
    if ( TableTSIDColumn.length() > 0 ) {
        props.set ( "TableTSIDColumn", TableTSIDColumn );
    }
    if ( TableDateTimeColumn.length() > 0 ) {
        props.set ( "TableDateTimeColumn", TableDateTimeColumn );
    }
    if ( TableValueColumn.length() > 0 ) {
        props.set ( "TableValueColumn", TableValueColumn );
    }
    if ( TableSetFlagColumn.length() > 0 ) {
        props.set ( "TableSetFlagColumn", TableSetFlagColumn );
    }
    if ( TableSetFlagDescColumn.length() > 0 ) {
        props.set ( "TableSetFlagDescColumn", TableSetFlagDescColumn );
    }
    if ( SortOrder.length() > 0 ) {
        props.set ( "SortOrder", SortOrder );
    }
    /*
    if ( __SetWindow_JCheckBox.isSelected() ){
        String SetWindowStart = __SetWindowStart_JPanel.toString(false,true).trim();
        String SetWindowEnd = __SetWindowEnd_JPanel.toString(false,true).trim();
        if ( SetWindowStart.length() > 0 ) {
            props.set ( "SetWindowStart", SetWindowStart );
        }
        if ( SetWindowEnd.length() > 0 ) {
            props.set ( "SetWindowEnd", SetWindowEnd );
        }
    }*/
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
{	String TSList = __TSList_JComboBox.getSelected();
	String TSID = __TSID_JComboBox.getSelected();
	String EnsembleID = __EnsembleID_JComboBox.getSelected();
	String TSIDFormat = __TSIDFormat_JTextField.getText().trim();
	String SetStart = __SetStart_JTextField.getText().trim();
	String SetEnd = __SetEnd_JTextField.getText().trim();
	String SetFlag = __SetFlag_JTextField.getText().trim();
	String SetFlagDesc = __SetFlagDesc_JTextField.getText().trim();
	String TableID = __TableID_JComboBox.getSelected();
	String TableTSIDColumn = __TableTSIDColumn_JTextField.getText().trim();
	String TableDateTimeColumn = __TableDateTimeColumn_JTextField.getText().trim();
	String TableValueColumn = __TableValueColumn_JTextField.getText().trim();
	String TableSetFlagColumn = __TableSetFlagColumn_JTextField.getText().trim();
	String TableSetFlagDescColumn = __TableSetFlagDescColumn_JTextField.getText().trim();
	String SortOrder = __SortOrder_JComboBox.getSelected();
	__command.setCommandParameter ( "TSList", TSList );
	__command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
    __command.setCommandParameter ( "TSIDFormat", TSIDFormat );
    __command.setCommandParameter ( "SetStart", SetStart );
    __command.setCommandParameter ( "SetEnd", SetEnd );
	__command.setCommandParameter ( "SetFlag", SetFlag );
	__command.setCommandParameter ( "SetFlagDesc", SetFlagDesc );
    __command.setCommandParameter ( "TableID", TableID );
    __command.setCommandParameter ( "TableTSIDColumn", TableTSIDColumn );
    __command.setCommandParameter ( "TableDateTimeColumn", TableDateTimeColumn );
    __command.setCommandParameter ( "TableValueColumn", TableValueColumn );
    __command.setCommandParameter ( "TableSetFlagColumn", TableSetFlagColumn );
    __command.setCommandParameter ( "TableSetFlagDescColumn", TableSetFlagDescColumn );
    __command.setCommandParameter ( "SortOrder", SortOrder );
    /*
    if ( __SetWindow_JCheckBox.isSelected() ){
        String SetWindowStart = __SetWindowStart_JPanel.toString(false,true).trim();
        String SetWindowEnd = __SetWindowEnd_JPanel.toString(false,true).trim();
        __command.setCommandParameter ( "SetWindowStart", SetWindowStart );
        __command.setCommandParameter ( "SetWindowEnd", SetWindowEnd );
    }*/
}

/**
Instantiates the GUI components.
@param parent Frame class instantiating this class.
@param command Command to edit.
@param tableIDChoices list of choices for table identifiers
*/
private void initialize ( JFrame parent, SetTimeSeriesValuesFromTable_Command command, List<String> tableIDChoices )
{	__command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Set values in the time series by transferring values from a table, using time series identifier and date/time to match time series and table values." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JSeparator ( SwingConstants.HORIZONTAL ),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    __main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // Panel for input time series
    int yTs = -1;
    JPanel ts_JPanel = new JPanel();
    ts_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Time series", ts_JPanel );
    
    JGUIUtil.addComponent(ts_JPanel, new JLabel (
		"Specify the time series to process.  Only data in the table that match the specified time series will be used." ),
		0, ++yTs, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel (
		"The date/time precision for the time series will be used to match the date/time column values in the table." ),
		0, ++yTs, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yTs, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    __TSList_JComboBox = new SimpleJComboBox(false);
    yTs = CommandEditorUtil.addTSListToEditorDialogPanel ( this, ts_JPanel, __TSList_JComboBox, yTs );

    __TSID_JLabel = new JLabel ("TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __TSID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __TSID_JComboBox.setToolTipText("Select a time series TSID/alias from the list or specify with ${Property} notation");
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    yTs = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, ts_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, yTs );
    
    __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __EnsembleID_JComboBox.setToolTipText("Select an ensemble identifier from the list or specify with ${Property} notation");
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    yTs = CommandEditorUtil.addTSIDToEditorDialogPanel (
        this, this, ts_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, yTs );
    
    JGUIUtil.addComponent(ts_JPanel, new JLabel("Format of TSID:"),
        0, ++yTs, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TSIDFormat_JTextField = new TSFormatSpecifiersJPanel(10);
    __TSIDFormat_JTextField.setToolTipText("Use %L for location, %T for data type, %I for interval.");
    __TSIDFormat_JTextField.addKeyListener ( this );
    __TSIDFormat_JTextField.getDocument().addDocumentListener(this);
    __TSIDFormat_JTextField.setToolTipText("%L for location, %T for data type, ${ts:property} to match time series property.");
    JGUIUtil.addComponent(ts_JPanel, __TSIDFormat_JTextField,
        1, yTs, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel ("Required - format time series TSID to match table TSID."),
        3, yTs, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Set start:" ),
        0, ++yTs, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SetStart_JTextField = new JTextField ( "", 20 );
    __SetStart_JTextField.setToolTipText("Specify the set start using a date/time string or ${Property} notation");
    __SetStart_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(ts_JPanel, __SetStart_JTextField,
        1, yTs, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel(
        "Optional - set start date/time or ${Property} (default=set full period)."),
        3, yTs, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Set end:" ), 
        0, ++yTs, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SetEnd_JTextField = new JTextField ( "", 20 );
    __SetEnd_JTextField.setToolTipText("Specify the set end using a date/time string or ${Property} notation");
    __SetEnd_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(ts_JPanel, __SetEnd_JTextField,
        1, yTs, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel(
        "Optional - set end date/time or ${Property} (default=set full period)."),
        3, yTs, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Set flag:" ), 
		0, ++yTs, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__SetFlag_JTextField = new JTextField ( 10 );
	__SetFlag_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(ts_JPanel, __SetFlag_JTextField,
		1, yTs, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel(
		"Optional - string to flag values that are set."), 
		3, yTs, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Set flag description:" ), 
        0, ++yTs, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SetFlagDesc_JTextField = new JTextField ( 15 );
    __SetFlagDesc_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(ts_JPanel, __SetFlagDesc_JTextField,
        1, yTs, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel( "Optional - description for set flag."), 
        3, yTs, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
  
    // Panel for table
    int yTable = -1;
    JPanel table_JPanel = new JPanel();
    table_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Table", table_JPanel );
    
    JGUIUtil.addComponent(table_JPanel, new JLabel (
		"Rows in the table are matched with time series using the TSID columnn." ),
		0, ++yTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel (
		"This subset of rows is then matched to time series values using the date/time column contents." ),
		0, ++yTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel (
		"Matching TSID and date/time result in the values from the table being set in the time series." ),
		0, ++yTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel (
		"If columns are specified for the set flag and description, they also will be set.  " +
		"Alternatively, set the flag globally using SetFlag and SetFlagDesc in the time series tab." ),
		0, ++yTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Table ID:" ), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableID_JComboBox = new SimpleJComboBox ( 12, true ); // Allow edit
    __TableID_JComboBox.setToolTipText("Specify the table ID or use ${Property} notation");
    tableIDChoices.add(0,""); // Add blank to ignore table
    __TableID_JComboBox.setData ( tableIDChoices );
    __TableID_JComboBox.addItemListener ( this );
    //__TableID_JComboBox.setMaximumRowCount(tableIDChoices.size());
    JGUIUtil.addComponent(table_JPanel, __TableID_JComboBox,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel( "Required - table that provides values."), 
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Table TSID column:" ), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableTSIDColumn_JTextField = new JTextField ( 10 );
    __TableTSIDColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __TableTSIDColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel( "Required - column name to match time series TSID."), 
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Table column for date/time:" ), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableDateTimeColumn_JTextField = new JTextField ( "", 10 );
    JGUIUtil.addComponent(table_JPanel, __TableDateTimeColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __TableDateTimeColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, new JLabel(
        "Required - column to match date/time."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Table column for value:" ), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableValueColumn_JTextField = new JTextField ( "", 20 );
    JGUIUtil.addComponent(table_JPanel, __TableValueColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __TableValueColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, new JLabel(
        "Required - column time series values."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Table column for data flag:" ), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableSetFlagColumn_JTextField = new JTextField ( "", 20 );
    JGUIUtil.addComponent(table_JPanel, __TableSetFlagColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __TableSetFlagColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, new JLabel(
        "Optional - column for time series flag."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Table column for data flag description:" ), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableSetFlagDescColumn_JTextField = new JTextField ( "", 20 );
    JGUIUtil.addComponent(table_JPanel, __TableSetFlagDescColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __TableSetFlagDescColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, new JLabel(
        "Optional - column for time series flag description."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Sort order for date/time:" ), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SortOrder_JComboBox = new SimpleJComboBox ( false );
    List<String> sortChoices = new ArrayList<String>();
    sortChoices.add ( "" );
    //sortChoices.add ( __command._Ascending );
    //sortChoices.add ( __command._Descending );
    sortChoices.add ( __command._None );
    __SortOrder_JComboBox.setData(sortChoices);
    __SortOrder_JComboBox.select ( 0 );
    __SortOrder_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(table_JPanel, __SortOrder_JComboBox,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel(
        "Optional - sort order for table date/times (default=" + __command._None + ")."), 
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    /* TODO SAM 2015-05-25 Enable later
    __SetWindow_JCheckBox = new JCheckBox ( "Set window:", false );
    __SetWindow_JCheckBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __SetWindow_JCheckBox, 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    JPanel setWindow_JPanel = new JPanel();
    setWindow_JPanel.setLayout(new GridBagLayout());
    __SetWindowStart_JPanel = new DateTime_JPanel ( "Start", TimeInterval.MONTH, TimeInterval.HOUR, null );
    __SetWindowStart_JPanel.addActionListener(this);
    __SetWindowStart_JPanel.addKeyListener ( this );
    JGUIUtil.addComponent(setWindow_JPanel, __SetWindowStart_JPanel,
        1, 0, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    // TODO SAM 2008-01-23 Figure out how to display the correct limits given the time series interval
    __SetWindowEnd_JPanel = new DateTime_JPanel ( "End", TimeInterval.MONTH, TimeInterval.HOUR, null );
    __SetWindowEnd_JPanel.addActionListener(this);
    __SetWindowEnd_JPanel.addKeyListener ( this );
    JGUIUtil.addComponent(setWindow_JPanel, __SetWindowEnd_JPanel,
        4, 0, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, setWindow_JPanel,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - set window within input year (default=full year)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        */

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea (4,50);
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);

	// Refresh the contents...
	refresh();

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

    button_JPanel.add ( __ok_JButton = new SimpleJButton("OK", this) );
	__ok_JButton.setToolTipText("Save changes to command");
	button_JPanel.add(__cancel_JButton = new SimpleJButton("Cancel", this));
	__cancel_JButton.setToolTipText("Cancel without saving changes to command");
	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");

	setTitle ( "Edit " + __command.getCommandName() + " Command" );

    pack();
    JGUIUtil.center( this );
	setResizable ( false );
    super.setVisible( true );
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged ( ItemEvent e )
{	refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	int code = event.getKeyCode();

	if ( code == KeyEvent.VK_ENTER ) {
		refresh();
		checkInput();
		if ( !__error_wait ) {
			response ( false );
		}
	}
}

public void keyReleased ( KeyEvent event )
{	refresh();
}

public void keyTyped ( KeyEvent event ) {;}

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
{	String routine = getClass().getName() + ".refresh";
	String TSList = "";
	String TSID = "";
	String EnsembleID = "";
	String TSIDFormat = "";
	String SetStart = "";
	String SetEnd = "";
	String SetFlag = "";
    String SetFlagDesc = "";
	String TableID = "";
	String TableTSIDColumn = "";
	String TableDateTimeColumn = "";
	String TableValueColumn = "";
	String TableSetFlagColumn = "";
	String TableSetFlagDescColumn = "";
	String SortOrder = "";
    //String SetWindowStart = "";
    //String SetWindowEnd = "";
	PropList props = __command.getCommandParameters();
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		TSList = props.getValue ( "TSList" );
        TSID = props.getValue ( "TSID" );
        EnsembleID = props.getValue ( "EnsembleID" );
        TSIDFormat = props.getValue ( "TSIDFormat" );
        SetStart = props.getValue ( "SetStart" );
        SetEnd = props.getValue ( "SetEnd" );
		SetFlag = props.getValue("SetFlag");
		SetFlagDesc = props.getValue("SetFlagDesc");
        TableID = props.getValue ( "TableID" );
        TableTSIDColumn = props.getValue ( "TableTSIDColumn" );
		TableDateTimeColumn = props.getValue ( "TableDateTimeColumn" );
		TableValueColumn = props.getValue ( "TableValueColumn" );
		TableSetFlagColumn = props.getValue ( "TableSetFlagColumn" );
		TableSetFlagDescColumn = props.getValue ( "TableSetFlagDescColumn" );
        SortOrder = props.getValue ( "SortOrder" );
        //SetWindowStart = props.getValue ( "SetWindowStart" );
        //SetWindowEnd = props.getValue ( "SetWindowEnd" );
		// Now select the item in the list.  If not a match, print a warning.
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
				"Existing command references an invalid\nTSList value \"" +	TSList +
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
        if (TSIDFormat != null ) {
            __TSIDFormat_JTextField.setText(TSIDFormat.trim());
        }
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
        if ( TableTSIDColumn != null ) {
            __TableTSIDColumn_JTextField.setText ( TableTSIDColumn );
        }
        if ( SetStart != null ) {
            __SetStart_JTextField.setText( SetStart );
        }
        if ( SetEnd != null ) {
            __SetEnd_JTextField.setText ( SetEnd );
        }
		if ( SetFlag != null ) {
			__SetFlag_JTextField.setText ( SetFlag );
		}
        if ( SetFlagDesc != null ) {
            __SetFlagDesc_JTextField.setText ( SetFlagDesc );
        }
        if (TableTSIDColumn != null ) {
            __TableTSIDColumn_JTextField.setText(TableTSIDColumn.trim());
        }
        if (TableDateTimeColumn != null ) {
            __TableDateTimeColumn_JTextField.setText(TableDateTimeColumn.trim());
        }
        if (TableValueColumn != null ) {
            __TableValueColumn_JTextField.setText(TableValueColumn.trim());
        }
        if (TableSetFlagColumn != null ) {
            __TableSetFlagColumn_JTextField.setText(TableSetFlagColumn.trim());
        }
        if (TableSetFlagDescColumn != null ) {
            __TableSetFlagDescColumn_JTextField.setText(TableSetFlagDescColumn.trim());
        }
        if ( JGUIUtil.isSimpleJComboBoxItem( __SortOrder_JComboBox, SortOrder, JGUIUtil.NONE, null, null ) ) {
            __SortOrder_JComboBox.select ( SortOrder );
        }
        else {
            if ( (SortOrder == null) || SortOrder.equals("") ) {
                // Set default...
                __SortOrder_JComboBox.select ( 0 );
            }
            else {
                Message.printWarning ( 1, routine, "Existing command references an invalid SortOrder \"" +
                    SortOrder + "\".  Select a different value or Cancel." );
            }
        }
        /*
        if ( (SetWindowEnd != null) && (SetWindowEnd.length() > 0) ) {
            try {
                // Add year because it is not part of the parameter value...
                DateTime SetWindowEnd_DateTime = DateTime.parse ( "0000-" + SetWindowEnd );
                Message.printStatus(2, routine, "Setting window end to " + SetWindowEnd_DateTime );
                __SetWindowEnd_JPanel.setDateTime ( SetWindowEnd_DateTime );
            }
            catch ( Exception e ) {
                Message.printWarning( 1, routine, "SetWindowEnd (" + SetWindowEnd + ") is not a valid date/time." );
            }
        }
        if ( (SetWindowStart != null) && (SetWindowStart.length() != 0) &&
            (SetWindowEnd != null) && (SetWindowEnd.length() != 0)) {
            __SetWindow_JCheckBox.setSelected ( true );
        }
        else {
            __SetWindow_JCheckBox.setSelected ( false );
        }*/
	}
	// Regardless, reset the command from the fields...
	checkGUIState();
	TSList = __TSList_JComboBox.getSelected();
    TSID = __TSID_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
    TSIDFormat = __TSIDFormat_JTextField.getText().trim();
	SetStart = __SetStart_JTextField.getText().trim();
	SetEnd = __SetEnd_JTextField.getText().trim();
	SetFlag = __SetFlag_JTextField.getText().trim();
    SetFlagDesc = __SetFlagDesc_JTextField.getText().trim();
    TableID = __TableID_JComboBox.getSelected();
    TableTSIDColumn = __TableTSIDColumn_JTextField.getText().trim();
	TableDateTimeColumn = __TableDateTimeColumn_JTextField.getText().trim();
    TableValueColumn = __TableValueColumn_JTextField.getText().trim();
    TableSetFlagColumn = __TableSetFlagColumn_JTextField.getText().trim();
    TableSetFlagDescColumn = __TableSetFlagDescColumn_JTextField.getText().trim();
    SortOrder = __SortOrder_JComboBox.getSelected();
	props = new PropList ( __command.getCommandName() );
	props.add ( "TSList=" + TSList );
	props.add ( "TSID=" + TSID );
    props.add ( "EnsembleID=" + EnsembleID );
    props.add ( "TSIDFormat=" + TSIDFormat );
    props.add ( "SetStart=" + SetStart );
    props.add ( "SetEnd=" + SetEnd );
	props.add ( "SetFlag=" + SetFlag );
	props.add ( "SetFlagDesc=" + SetFlagDesc );
    props.add ( "TableID=" + TableID );
    props.add ( "TableTSIDColumn=" + TableTSIDColumn );
    props.add ( "TableDateTimeColumn=" + TableDateTimeColumn );
    props.add ( "TableValueColumn=" + TableValueColumn );
    props.add ( "TableSetFlagColumn=" + TableSetFlagColumn );
    props.add ( "TableSetFlagDescColumn=" + TableSetFlagDescColumn );
    props.add ( "SortOrder=" + SortOrder );
    /*
    if ( __SetWindow_JCheckBox.isSelected() ) {
        SetWindowStart = __SetWindowStart_JPanel.toString(false,true).trim();
        SetWindowEnd = __SetWindowEnd_JPanel.toString(false,true).trim();
        props.add ( "SetWindowStart=" + SetWindowStart );
        props.add ( "SetWindowEnd=" + SetWindowEnd );
    }
    */
	__command_JTextArea.setText( __command.toString ( props ) );
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