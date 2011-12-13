package rti.tscommandprocessor.commands.table;

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

import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;

import java.util.List;

import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.DateTimeWindow;
import RTi.Util.Time.DateTime_JPanel;
import RTi.Util.Time.TimeInterval;

/**
Editor for TimeSeriesToTable command.
*/
public class TimeSeriesToTable_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{

private SimpleJButton __cancel_JButton = null;// Cancel Button
private SimpleJButton __ok_JButton = null;	// Ok Button
private TimeSeriesToTable_Command __command = null;// Command to edit
private JTextArea __command_JTextArea=null;// Command as JTextField
private SimpleJComboBox __TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox __TSID_JComboBox = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private SimpleJComboBox __TableID_JComboBox = null;
private JTextField __DateTimeColumn_JTextField = null;
private TSFormatSpecifiersJPanel __DataColumn_JTextField = null;
private JTextField __DataRow_JTextField = null;
private JTextField __OutputStart_JTextField = null;
private JTextField __OutputEnd_JTextField = null;
private JCheckBox __OutputWindow_JCheckBox = null;
private DateTime_JPanel __OutputWindowStart_JPanel = null; // Fields for output window within a year
private DateTime_JPanel __OutputWindowEnd_JPanel = null;
private SimpleJComboBox __IfTableNotFound_JComboBox = null;
private boolean __error_wait = false;	// Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK button has been pressed.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public TimeSeriesToTable_JDialog ( JFrame parent, TimeSeriesToTable_Command command )
{   super(parent, true);
    initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	Object o = event.getSource();

	if ( o == __cancel_JButton ) {
		__command = null;
		response ( false );
	}
	else if ( o == __ok_JButton ) {
		refresh();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
    else {
        checkGUIState();
        refresh ();
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
    
    if ( __OutputWindow_JCheckBox.isSelected() ) {
        // Checked so enable the date panels
        __OutputWindowStart_JPanel.setEnabled ( true );
        __OutputWindowEnd_JPanel.setEnabled ( true );
    }
    else {
        __OutputWindowStart_JPanel.setEnabled ( false );
        __OutputWindowEnd_JPanel.setEnabled ( false );
    }
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{   // Put together a list of parameters to check...
    PropList props = new PropList ( "" );
    String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String TableID = __TableID_JComboBox.getSelected();
    String DateTimeColumn = __DateTimeColumn_JTextField.getText().trim();
    String DataColumn = __DataColumn_JTextField.getText().trim();
    String DataRow = __DataRow_JTextField.getText().trim();
    String OutputStart = __OutputStart_JTextField.getText().trim();
    String OutputEnd = __OutputEnd_JTextField.getText().trim();
    String IfTableNotFound = __IfTableNotFound_JComboBox.getSelected();
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
    if ( TableID.length() > 0 ) {
        props.set ( "TableID", TableID );
    }
    if ( DateTimeColumn.length() > 0 ) {
        props.set ( "DateTimeColumn", DateTimeColumn );
    }
    if ( DataColumn.length() > 0 ) {
        props.set ( "DataColumn", DataColumn );
    }
    if ( DataRow.length() > 0 ) {
        props.set ( "DataRow", DataRow );
    }
    if ( OutputStart.length() > 0 ) {
        props.set ( "OutputStart", OutputStart );
    }
    if ( OutputEnd.length() > 0 ) {
        props.set ( "OutputEnd", OutputEnd );
    }
    if ( IfTableNotFound.length() > 0 ) {
        props.set ( "IfTableNotFound", IfTableNotFound );
    }
    if ( __OutputWindow_JCheckBox.isSelected() ){
        String OutputWindowStart = __OutputWindowStart_JPanel.toString(false,true).trim();
        String OutputWindowEnd = __OutputWindowEnd_JPanel.toString(false,true).trim();
        if ( OutputWindowStart.length() > 0 ) {
            props.set ( "OutputWindowStart", OutputWindowStart );
        }
        if ( OutputWindowEnd.length() > 0 ) {
            props.set ( "OutputWindowEnd", OutputWindowEnd );
        }
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
{   String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String TableID = __TableID_JComboBox.getSelected();
    String DateTimeColumn = __DateTimeColumn_JTextField.getText().trim();
    String DataColumn = __DataColumn_JTextField.getText().trim();
    String DataRow = __DataRow_JTextField.getText().trim();
    String OutputStart = __OutputStart_JTextField.getText().trim();
    String OutputEnd = __OutputEnd_JTextField.getText().trim();
    String IfTableNotFound = __IfTableNotFound_JComboBox.getSelected();
    __command.setCommandParameter ( "TSList", TSList );
    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
    __command.setCommandParameter ( "TableID", TableID );
    __command.setCommandParameter ( "DateTimeColumn", DateTimeColumn );
    __command.setCommandParameter ( "DataColumn", DataColumn );
    __command.setCommandParameter ( "DataRow", DataRow );
    __command.setCommandParameter ( "OutputStart", OutputStart );
    __command.setCommandParameter ( "OutputEnd", OutputEnd );
    __command.setCommandParameter ( "IfTableNotFound", IfTableNotFound );
    if ( __OutputWindow_JCheckBox.isSelected() ){
        String OutputWindowStart = __OutputWindowStart_JPanel.toString(false,true).trim();
        String OutputWindowEnd = __OutputWindowEnd_JPanel.toString(false,true).trim();
        __command.setCommandParameter ( "OutputWindowStart", OutputWindowStart );
        __command.setCommandParameter ( "OutputWindowEnd", OutputWindowEnd );
    }
    else {
        __command.setCommandParameter ( "OutputWindowStart", "" );
        __command.setCommandParameter ( "OutputWindowEnd", "" );
    }
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__TSID_JComboBox = null;
	__DataColumn_JTextField = null;
	__DateTimeColumn_JTextField = null;
	__cancel_JButton = null;
	__command_JTextArea = null;
	__command = null;
	__ok_JButton = null;
	super.finalize ();
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, TimeSeriesToTable_Command command )
{	__command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Copy time series date/time and value pairs to columns in a new table." ), 
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The time series must have the same data interval." ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "If the output window is specified, use a date/time precision consistent with data." ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    __TSList_JComboBox = new SimpleJComboBox(false);
    y = CommandEditorUtil.addTSListToEditorDialogPanel ( this, main_JPanel, __TSList_JComboBox, y );

    __TSID_JLabel = new JLabel ("TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __TSID_JComboBox = new SimpleJComboBox ( true );  // Allow edits
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, main_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, y );
    
    __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
        this, this, main_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, y );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Table ID:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableID_JComboBox = new SimpleJComboBox ( 10, true ); // Allow edits
    List<String> TableIDs = TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    // If the TableID has been specified and is not in the list, then a new table is being added -
    // add the table ID at the start of the list
    String TableID = __command.getCommandParameters().getValue("TableID");
    if ( (TableID != null) && !TableID.equals("") ) {
        if ( StringUtil.indexOfIgnoreCase(TableIDs, TableID) < 0 ) {
            TableIDs.add(0,TableID);
        }
    }
    __TableID_JComboBox.setData(TableIDs);
    __TableID_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __TableID_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Required - table identifier."),
    3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel,new JLabel( "Date/time column in table:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DateTimeColumn_JTextField = new JTextField ( "", 10 );
    __DateTimeColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __DateTimeColumn_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Required - column name for date/times."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel,new JLabel( "Column(s) for data:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataColumn_JTextField = new TSFormatSpecifiersJPanel(10);
    __DataColumn_JTextField.setToolTipText("Use %L for location, %T for data type, %I for interval.");
    __DataColumn_JTextField.addKeyListener ( this );
    __DataColumn_JTextField.getDocument().addDocumentListener(this);
    JGUIUtil.addComponent(main_JPanel, __DataColumn_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Required - data column name(s) for 1+ time series."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel,new JLabel( "First row for data:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataRow_JTextField = new JTextField ( "", 10 );
    __DataRow_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __DataRow_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Required - row number (1+) for first data value."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel,new JLabel( "Output start date/time:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputStart_JTextField = new JTextField ( "", 10 );
    __OutputStart_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __OutputStart_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional (default=copy all)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel,new JLabel("Output end date/time:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputEnd_JTextField = new JTextField ( "", 10 );
    __OutputEnd_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __OutputEnd_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional (default=copy all)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    // Always request month to minute because don't know for sure what time series interval is specified
    __OutputWindow_JCheckBox = new JCheckBox ( "Output window:", false );
    __OutputWindow_JCheckBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __OutputWindow_JCheckBox, 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    JPanel OutputWindow_JPanel = new JPanel();
    OutputWindow_JPanel.setLayout(new GridBagLayout());
    __OutputWindowStart_JPanel = new DateTime_JPanel ( "Start", TimeInterval.MONTH, TimeInterval.MINUTE, null );
    __OutputWindowStart_JPanel.addActionListener(this);
    __OutputWindowStart_JPanel.addKeyListener ( this );
    JGUIUtil.addComponent(OutputWindow_JPanel, __OutputWindowStart_JPanel,
        1, 0, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    // TODO SAM 2008-01-23 Figure out how to display the correct limits given the time series interval
    __OutputWindowEnd_JPanel = new DateTime_JPanel ( "End", TimeInterval.MONTH, TimeInterval.MINUTE, null );
    __OutputWindowEnd_JPanel.addActionListener(this);
    __OutputWindowEnd_JPanel.addKeyListener ( this );
    JGUIUtil.addComponent(OutputWindow_JPanel, __OutputWindowEnd_JPanel,
        4, 0, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, OutputWindow_JPanel,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - output window within each year (default=full year)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Action if table not found:"), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__IfTableNotFound_JComboBox = new SimpleJComboBox ( false );
	//__IfTableNotFound_JComboBox.addItem ( "" );
	__IfTableNotFound_JComboBox.addItem ( __command._Create );
	//__IfTableNotFound_JComboBox.addItem ( __command._Warn );
	__IfTableNotFound_JComboBox.addItemListener ( this );
        JGUIUtil.addComponent(main_JPanel, __IfTableNotFound_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Required."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __command_JTextArea = new JTextArea ( 4, 50 );
    __command_JTextArea.setLineWrap ( true );
    __command_JTextArea.setWrapStyleWord ( true );
    __command_JTextArea.setEditable ( false );
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Refresh the contents...
    checkGUIState();
	refresh ();

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	__cancel_JButton = new SimpleJButton("Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__ok_JButton = new SimpleJButton("OK", this);
	button_JPanel.add ( __ok_JButton );

	setResizable ( true );
    setTitle ( "Edit " + __command.getCommandName() + "() Command" );
    pack();
    JGUIUtil.center( this );
    super.setVisible( true );
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged ( ItemEvent e )
{	checkGUIState();
    refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	int code = event.getKeyCode();

    refresh ();
	if ( code == KeyEvent.VK_ENTER ) {
		checkInput();
		if ( !__error_wait ) {
			response ( false );
		}
	}
}

public void keyReleased ( KeyEvent event )
{    refresh();
}

public void keyTyped ( KeyEvent event ) {;}

/**
Indicate if the user pressed OK (cancel otherwise).
@return true if the edits were committed, false if the user canceled.
*/
public boolean ok ()
{   return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{	String routine = "TimeSeriesToTable_JDialog.refresh";
    String TSList = "";
    String TSID = "";
    String EnsembleID = "";
    String TableID = "";
    String DateTimeColumn = "";
    String DataColumn = "";
    String DataRow = "";
    String OutputStart = "";
    String OutputEnd = "";
    String OutputWindowStart = "";
    String OutputWindowEnd = "";
    String IfTableNotFound = "";
    PropList props = __command.getCommandParameters();
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command...
        TSList = props.getValue ( "TSList" );
        TSID = props.getValue ( "TSID" );
        EnsembleID = props.getValue ( "EnsembleID" );
        TableID = props.getValue ( "TableID" );
        OutputStart = props.getValue ( "OutputStart" );
        OutputEnd = props.getValue ( "OutputEnd" );
        OutputWindowStart = props.getValue ( "OutputWindowStart" );
        OutputWindowEnd = props.getValue ( "OutputWindowEnd" );
        DateTimeColumn = props.getValue ( "DateTimeColumn" );
        DataColumn = props.getValue ( "DataColumn" );
        DataRow = props.getValue ( "DataRow" );
        IfTableNotFound = props.getValue ( "IfTableNotFound" );
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
        if ( TableID == null ) {
            // Select default...
            if ( __TableID_JComboBox.getItemCount() > 0 ) {
                __TableID_JComboBox.select ( 0 );
            }
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
        if ( DateTimeColumn != null ) {
            __DateTimeColumn_JTextField.setText ( DateTimeColumn );
        }
        if ( DataColumn != null ) {
            __DataColumn_JTextField.setText ( DataColumn );
        }
        if ( DataRow != null ) {
            __DataRow_JTextField.setText ( DataRow );
        }
        if ( OutputStart != null ) {
            __OutputStart_JTextField.setText ( OutputStart );
        }
        if ( OutputEnd != null ) {
            __OutputEnd_JTextField.setText ( OutputEnd );
        }
        if ( (OutputWindowStart != null) && (OutputWindowStart.length() > 0) ) {
            try {
                // Add year because it is not part of the parameter value...
                DateTime OutputWindowStart_DateTime = DateTime.parse (
                    "" + DateTimeWindow.WINDOW_YEAR + "-" + OutputWindowStart );
                Message.printStatus(2, routine, "Setting window start to " + OutputWindowStart_DateTime );
                __OutputWindowStart_JPanel.setDateTime ( OutputWindowStart_DateTime );
            }
            catch ( Exception e ) {
                Message.printWarning( 1, routine, "OutputWindowStart (" + OutputWindowStart +
                    ") prepended with " + DateTimeWindow.WINDOW_YEAR + " is not a valid date/time." );
            }
        }
        if ( (OutputWindowEnd != null) && (OutputWindowEnd.length() > 0) ) {
            try {
                // Add year because it is not part of the parameter value...
                DateTime OutputWindowEnd_DateTime = DateTime.parse (
                    "" + DateTimeWindow.WINDOW_YEAR + "-" + OutputWindowEnd );
                Message.printStatus(2, routine, "Setting window end to " + OutputWindowEnd_DateTime );
                __OutputWindowEnd_JPanel.setDateTime ( OutputWindowEnd_DateTime );
            }
            catch ( Exception e ) {
                Message.printWarning( 1, routine, "OutputWindowEnd (" + OutputWindowEnd +
                    ") prepended with " + DateTimeWindow.WINDOW_YEAR + " is not a valid date/time." );
            }
        }
        if ( ((OutputWindowStart != null) && (OutputWindowStart.length() != 0)) ||
            ((OutputWindowEnd != null) && (OutputWindowEnd.length() != 0)) ) {
            __OutputWindow_JCheckBox.setSelected ( true );
        }
        else {
            __OutputWindow_JCheckBox.setSelected ( false );
        }
        if ( IfTableNotFound == null ) {
            // Select default...
            __IfTableNotFound_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem(__IfTableNotFound_JComboBox,
                IfTableNotFound, JGUIUtil.NONE, null, null )) {
                __IfTableNotFound_JComboBox.select ( IfTableNotFound );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\n" +
                "IfTableNotFound value \"" + IfTableNotFound +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
	}
    // Regardless, reset the command from the fields...
    checkGUIState();
    TSList = __TSList_JComboBox.getSelected();
    TSID = __TSID_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
    TableID = __TableID_JComboBox.getSelected();
    DateTimeColumn = __DateTimeColumn_JTextField.getText().trim();
    DataColumn = __DataColumn_JTextField.getText().trim();
    DataRow = __DataRow_JTextField.getText().trim();
    OutputStart = __OutputStart_JTextField.getText().trim();
    OutputEnd = __OutputEnd_JTextField.getText().trim();
    IfTableNotFound = __IfTableNotFound_JComboBox.getSelected();
    props = new PropList ( __command.getCommandName() );
    props.add ( "TSList=" + TSList );
    props.add ( "TSID=" + TSID );
    props.add ( "EnsembleID=" + EnsembleID );
    props.add ( "TableID=" + TableID );
    props.add ( "DateTimeColumn=" + DateTimeColumn );
    props.add ( "DataColumn=" + DataColumn );
    props.add ( "DataRow=" + DataRow );
    props.add ( "Transformation=" + IfTableNotFound );
    props.add ( "OutputStart=" + OutputStart );
    props.add ( "OutputEnd=" + OutputEnd );
    props.add ( "IfTableNotFound=" + IfTableNotFound );
    if ( __OutputWindow_JCheckBox.isSelected() ) {
        OutputWindowStart = __OutputWindowStart_JPanel.toString(false,true).trim();
        OutputWindowEnd = __OutputWindowEnd_JPanel.toString(false,true).trim();
        props.add ( "OutputWindowStart=" + OutputWindowStart );
        props.add ( "OutputWindowEnd=" + OutputWindowEnd );
    }
    __command_JTextArea.setText( __command.toString ( props ) );
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed and the dialog is closed.
*/
private void response ( boolean ok )
{   __ok = ok;  // Save to be returned by ok()
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