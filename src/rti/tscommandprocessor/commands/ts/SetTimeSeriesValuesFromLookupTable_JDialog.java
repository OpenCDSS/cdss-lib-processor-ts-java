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

import javax.swing.JCheckBox;
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
import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.TS.TSRegression;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.PropList;
import RTi.Util.Math.DataTransformationType;
import RTi.Util.Message.Message;
import RTi.Util.Table.LookupMethodType;
import RTi.Util.Table.OutOfRangeLookupMethodType;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.DateTime_JPanel;
import RTi.Util.Time.TimeInterval;

/**
Editor for the SetTimeSeriesValuesFromLookupTable command.
*/
@SuppressWarnings("serial")
public class SetTimeSeriesValuesFromLookupTable_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private SetTimeSeriesValuesFromLookupTable_Command __command = null;
private JTextArea __command_JTextArea=null;
private JTabbedPane __main_JTabbedPane = null;
private SimpleJComboBox	__InputTSID_JComboBox = null;
private SimpleJComboBox __OutputTSID_JComboBox = null;
private SimpleJComboBox __TableID_JComboBox = null;
private JTextField __TableTSIDColumn_JTextField = null;
private TSFormatSpecifiersJPanel __TableTSIDFormat_JTextField = null; // Format for time series identifiers
private JTextField __TableValue1Column_JTextField = null;
private SimpleJComboBox __SortInput_JComboBox = null;
private JTextField __TableValue2Column_JTextField = null;
private JTextField __EffectiveDateColumn_JTextField = null;
private SimpleJComboBox __LookupMethod_JComboBox = null;
private SimpleJComboBox __OutOfRangeLookupMethod_JComboBox = null;
private SimpleJComboBox __OutOfRangeNotification_JComboBox = null;
private SimpleJComboBox __Transformation_JComboBox = null;
private JTextField __LEZeroLogValue_JTextField = null;
private JTextField __SetStart_JTextField = null;
private JTextField __SetEnd_JTextField = null;
private JCheckBox __SetWindow_JCheckBox = null;
private DateTime_JPanel __SetWindowStart_JPanel = null;
private DateTime_JPanel __SetWindowEnd_JPanel = null;
private JTextField __SetWindowStart_JTextField = null; // Used for properties
private JTextField __SetWindowEnd_JTextField = null;
// TODO SAM 2012-02-04 Might need flag for values outside the rating
private boolean	__error_wait = false; // Is there an error to be cleared up?
private boolean	__first_time = true;
private boolean	__ok = false; // Whether OK has been pressed.

/**
Command editor dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices choices for TableID value.
*/
public SetTimeSeriesValuesFromLookupTable_JDialog ( JFrame parent,
    SetTimeSeriesValuesFromLookupTable_Command command, List<String> tableIDChoices )
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
		HelpViewer.getInstance().showHelp("command", "SetTimeSeriesValuesFromLookupTable");
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
    if ( __SetWindow_JCheckBox.isSelected() ) {
        // Checked so enable the date panels
        __SetWindowStart_JPanel.setEnabled ( true );
        __SetWindowEnd_JPanel.setEnabled ( true );
        __SetWindowStart_JTextField.setEnabled ( true );
        __SetWindowEnd_JTextField.setEnabled ( true );
    }
    else {
        __SetWindowStart_JPanel.setEnabled ( false );
        __SetWindowEnd_JPanel.setEnabled ( false );
        __SetWindowStart_JTextField.setEnabled ( false );
        __SetWindowEnd_JTextField.setEnabled ( false );
    }
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
	String InputTSID = __InputTSID_JComboBox.getSelected();
	String OutputTSID = __OutputTSID_JComboBox.getSelected();
    String TableID = __TableID_JComboBox.getSelected();
    String TableTSIDColumn = __TableTSIDColumn_JTextField.getText().trim();
    String TableTSIDFormat = __TableTSIDFormat_JTextField.getText().trim();
    String TableValue1Column = __TableValue1Column_JTextField.getText().trim();
    String SortInput = __SortInput_JComboBox.getSelected();
    String TableValue2Column = __TableValue2Column_JTextField.getText().trim();
	String EffectiveDateColumn = __EffectiveDateColumn_JTextField.getText().trim();
	String LookupMethod = __LookupMethod_JComboBox.getSelected();
	String OutOfRangeLookupMethod = __OutOfRangeLookupMethod_JComboBox.getSelected();
	String OutOfRangeNotification = __OutOfRangeNotification_JComboBox.getSelected();
    String Transformation = __Transformation_JComboBox.getSelected();
    String LEZeroLogValue = __LEZeroLogValue_JTextField.getText().trim();
	String SetStart = __SetStart_JTextField.getText().trim();
	String SetEnd = __SetEnd_JTextField.getText().trim();
	__error_wait = false;

	if ( (InputTSID != null) && (InputTSID.length() > 0) ) {
		props.set ( "InputTSID", InputTSID );
	}
    if ( (OutputTSID != null) && (OutputTSID.length() > 0) ) {
        props.set ( "OutputTSID", OutputTSID );
    }
    if ( TableID.length() > 0 ) {
        props.set ( "TableID", TableID );
    }
    if ( TableTSIDColumn.length() > 0 ) {
        props.set ( "TableTSIDColumn", TableTSIDColumn );
    }
    if ( TableTSIDFormat.length() > 0 ) {
        props.set ( "TableTSIDFormat", TableTSIDFormat );
    }
    if ( TableValue1Column.length() > 0 ) {
        props.set ( "TableValue1Column", TableValue1Column );
    }
    if ( SortInput.length() > 0 ) {
        props.set ( "SortInput", SortInput );
    }
    if ( TableValue2Column.length() > 0 ) {
        props.set ( "TableValue2Column", TableValue2Column );
    }
    if ( EffectiveDateColumn.length() > 0 ) {
        props.set ( "EffectiveDateColumn", EffectiveDateColumn );
    }
    if ( LookupMethod.length() > 0 ) {
        props.set ( "LookupMethod", LookupMethod );
    }
    if ( OutOfRangeLookupMethod.length() > 0 ) {
        props.set ( "OutOfRangeLookupMethod", OutOfRangeLookupMethod );
    }
    if ( OutOfRangeNotification.length() > 0 ) {
        props.set ( "OutOfRangeNotification", OutOfRangeNotification );
    }
    if ( Transformation.length() > 0 ) {
        props.set ( "Transformation", Transformation );
    }
    if ( LEZeroLogValue.length() > 0 ) {
        props.set ( "LEZeroLogValue", LEZeroLogValue );
    }
    if ( SetStart.length() > 0 ) {
        props.set ( "SetStart", SetStart );
    }
    if ( SetEnd.length() > 0 ) {
        props.set ( "SetEnd", SetEnd );
    }
    if ( __SetWindow_JCheckBox.isSelected() ){
        String SetWindowStart = __SetWindowStart_JPanel.toString(false,true).trim();
        String SetWindowEnd = __SetWindowEnd_JPanel.toString(false,true).trim();
        // 99 is used for month if not specified - don't want that in the parameters
        if ( SetWindowStart.startsWith("99") ) {
            SetWindowStart = "";
        }
        if ( SetWindowEnd.startsWith("99") ) {
            SetWindowEnd = "";
        }
        // This will override the above
        String SetWindowStart2 = __SetWindowStart_JTextField.getText().trim();
        String SetWindowEnd2 = __SetWindowEnd_JTextField.getText().trim();
        if ( !SetWindowStart2.isEmpty() ) {
            SetWindowStart = SetWindowStart2;
        }
        if ( !SetWindowEnd2.isEmpty() ) {
            SetWindowEnd = SetWindowEnd2;
        }
        if ( !SetWindowStart.isEmpty() ) {
            props.set ( "SetWindowStart", SetWindowStart );
        }
        if ( SetWindowEnd.isEmpty() ) {
            props.set ( "SetWindowEnd", SetWindowEnd );
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
{	String InputTSID = __InputTSID_JComboBox.getSelected();
    String OutputTSID = __OutputTSID_JComboBox.getSelected();
    String TableID = __TableID_JComboBox.getSelected();
    String TableTSIDColumn = __TableTSIDColumn_JTextField.getText().trim();
    String TableTSIDFormat = __TableTSIDFormat_JTextField.getText().trim();
    String TableValue1Column = __TableValue1Column_JTextField.getText().trim();
    String SortInput = __SortInput_JComboBox.getSelected();
    String TableValue2Column = __TableValue2Column_JTextField.getText().trim();
	String EffectiveDateColumn = __EffectiveDateColumn_JTextField.getText().trim();
    String Transformation = __Transformation_JComboBox.getSelected();
    String LookupMethod = __LookupMethod_JComboBox.getSelected();
    String OutOfRangeLookupMethod = __OutOfRangeLookupMethod_JComboBox.getSelected();
    String OutOfRangeNotification = __OutOfRangeNotification_JComboBox.getSelected();
    String LEZeroLogValue = __LEZeroLogValue_JTextField.getText().trim();
    String SetStart = __SetStart_JTextField.getText().trim();
    String SetEnd = __SetEnd_JTextField.getText().trim();
	__command.setCommandParameter ( "InputTSID", InputTSID );
	__command.setCommandParameter ( "OutputTSID", OutputTSID );
    __command.setCommandParameter ( "TableID", TableID );
    __command.setCommandParameter ( "TableTSIDColumn", TableTSIDColumn );
    __command.setCommandParameter ( "TableTSIDFormat", TableTSIDFormat );
    __command.setCommandParameter ( "TableValue1Column", TableValue1Column );
    __command.setCommandParameter ( "SortInput", SortInput );
    __command.setCommandParameter ( "TableValue2Column", TableValue2Column );
    __command.setCommandParameter ( "EffectiveDateColumn", EffectiveDateColumn );
    __command.setCommandParameter ( "LookupMethod", LookupMethod );
    __command.setCommandParameter ( "OutOfRangeLookupMethod", OutOfRangeLookupMethod );
    __command.setCommandParameter ( "OutOfRangeNotification", OutOfRangeNotification );
    __command.setCommandParameter ( "Transformation", Transformation );
    __command.setCommandParameter ( "LEZeroLogValue", LEZeroLogValue );
	__command.setCommandParameter ( "SetStart", SetStart );
	__command.setCommandParameter ( "SetEnd", SetEnd );
    if ( __SetWindow_JCheckBox.isSelected() ){
        String SetWindowStart = __SetWindowStart_JPanel.toString(false,true).trim();
        String SetWindowEnd = __SetWindowEnd_JPanel.toString(false,true).trim();
        if ( SetWindowStart.startsWith("99") ) {
            SetWindowStart = "";
        }
        if ( SetWindowEnd.startsWith("99") ) {
            SetWindowEnd = "";
        }
        String SetWindowStart2 = __SetWindowStart_JTextField.getText().trim();
        String SetWindowEnd2 = __SetWindowEnd_JTextField.getText().trim();
        if ( !SetWindowStart2.isEmpty() ) {
        	SetWindowStart = SetWindowStart2;
        }
        if ( !SetWindowEnd2.isEmpty() ) {
        	SetWindowEnd = SetWindowEnd2;
        }
        __command.setCommandParameter ( "SetWindowStart", SetWindowStart );
        __command.setCommandParameter ( "SetWindowEnd", SetWindowEnd );
    }
    else {
    	// Clear the properties because they may have been set during editing but should not be propagated
    	__command.getCommandParameters().unSet ( "SetWindowStart" );
    	__command.getCommandParameters().unSet ( "SetWindowEnd" );
    }
}

/**
Instantiates the GUI components.
@param parent Frame class instantiating this class.
@param command Command to edit.
@param tableIDChoices list of choices for table identifiers
*/
private void initialize ( JFrame parent, SetTimeSeriesValuesFromLookupTable_Command command, List<String> tableIDChoices )
{	__command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Set values in the output time series by using an input time series and lookup table." ),
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    __main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // Panel for time series parameters
    int yts = -1;
    JPanel ts_JPanel = new JPanel();
    ts_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Time Series", ts_JPanel );
    
    JGUIUtil.addComponent(ts_JPanel, new JLabel (
		"Select input and output time series.  The output time series period will not be automatically extended." ),
		0, ++yts, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JSeparator (SwingConstants.HORIZONTAL ), 
        0, ++yts, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(ts_JPanel, new JLabel("Input time series:"),
		0, ++yts, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputTSID_JComboBox = new SimpleJComboBox ( true ); // Allow edit
	__InputTSID_JComboBox.setToolTipText("Select an input time series TSID/alias from the list or specify with ${Property} notation");
	List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
		(TSCommandProcessor)__command.getCommandProcessor(), __command );
	__InputTSID_JComboBox.setData ( tsids );
	__InputTSID_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(ts_JPanel, __InputTSID_JComboBox,
		1, yts, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(ts_JPanel, new JLabel("Output time series:"),
        0, ++yts, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputTSID_JComboBox = new SimpleJComboBox ( true ); // Allow edit
    __OutputTSID_JComboBox.setToolTipText("Select an output time series TSID/alias from the list or specify with ${Property} notation");
    __OutputTSID_JComboBox.setData ( tsids );
    __OutputTSID_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(ts_JPanel, __OutputTSID_JComboBox,
        1, yts, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Panel for lookup table parameters
    int yTable = -1;
    JPanel table_JPanel = new JPanel();
    table_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Lookup Table", table_JPanel );
    
    JGUIUtil.addComponent(table_JPanel, new JLabel (
		"Specify the lookup table and configuration." ),
		0, ++yTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel (
        "<html><b>Currently the lookup table must contain data for only one input time series and the effective" +
        " date is not checked.</b></html>." ),
        0, ++yTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JSeparator (SwingConstants.HORIZONTAL ), 
        0, ++yTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Lookup table ID:" ), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableID_JComboBox = new SimpleJComboBox ( 12, true ); // Allow edit
    __TableID_JComboBox.setToolTipText("Specify the lookup table ID for output or use ${Property} notation");
    tableIDChoices.add(0,""); // Add blank to ignore table
    __TableID_JComboBox.setData ( tableIDChoices );
    __TableID_JComboBox.addItemListener ( this );
    //__TableID_JComboBox.setMaximumRowCount(tableIDChoices.size());
    JGUIUtil.addComponent(table_JPanel, __TableID_JComboBox,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel( "Required - lookup table."), 
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Table TSID column:" ), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableTSIDColumn_JTextField = new JTextField ( 10 );
    __TableTSIDColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __TableTSIDColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel( "Future Feature - column name for input time series TSID."), 
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __TableTSIDColumn_JTextField.setEnabled(false);
    
    JGUIUtil.addComponent(table_JPanel, new JLabel("Format of TSID:"),
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableTSIDFormat_JTextField = new TSFormatSpecifiersJPanel(10);
    __TableTSIDFormat_JTextField.setToolTipText("Use %L for location, %T for data type, %I for interval.");
    __TableTSIDFormat_JTextField.addKeyListener ( this );
    __TableTSIDFormat_JTextField.getDocument().addDocumentListener(this);
    __TableTSIDFormat_JTextField.setToolTipText("%L for location, %T for data type.");
    JGUIUtil.addComponent(table_JPanel, __TableTSIDFormat_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Future Feature - use %L for location, etc. (default=alias or TSID)."),
        3, yTable, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    __TableTSIDFormat_JTextField.setEnabled(false);
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Column for input value:" ), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableValue1Column_JTextField = new JTextField ( "", 20 );
    __TableValue1Column_JTextField.setToolTipText("Specify the value 1 (input) column, can use ${Property} notation");
    JGUIUtil.addComponent(table_JPanel, __TableValue1Column_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __TableValue1Column_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, new JLabel(
        "Required - column for input time series values."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Sort input?:" ), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SortInput_JComboBox = new SimpleJComboBox ( false );
    List<String> sortChoices = new ArrayList<String>();
    sortChoices.add ( "" );
    sortChoices.add ( __command._False );
    sortChoices.add ( __command._True );
    __SortInput_JComboBox.setData(sortChoices);
    __SortInput_JComboBox.select ( 0 );
    __SortInput_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(table_JPanel, __SortInput_JComboBox,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel(
        "Optional - sort input table (default=" + __command._False + ")."), 
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Column for output value:" ), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableValue2Column_JTextField = new JTextField ( "", 20 );
    __TableValue2Column_JTextField.setToolTipText("Specify the value 2 (output) column, can use ${Property} notation");
    JGUIUtil.addComponent(table_JPanel, __TableValue2Column_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __TableValue2Column_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, new JLabel(
        "Required - column for output time series values."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Column for effective date:" ), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __EffectiveDateColumn_JTextField = new JTextField ( "", 10 );
    JGUIUtil.addComponent(table_JPanel, __EffectiveDateColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __EffectiveDateColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, new JLabel(
        "Future Feature - column for lookup data effective date."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __EffectiveDateColumn_JTextField.setEnabled(false);
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Lookup method:" ), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __LookupMethod_JComboBox = new SimpleJComboBox ( false );
    List<String> lookupChoices = new ArrayList<String>();
    lookupChoices.add ( "" );
    lookupChoices.add ( "" + LookupMethodType.INTERPOLATE );
    lookupChoices.add ( "" + LookupMethodType.PREVIOUS_VALUE );
    lookupChoices.add ( "" + LookupMethodType.NEXT_VALUE );
    __LookupMethod_JComboBox.setData(lookupChoices);
    __LookupMethod_JComboBox.select ( 0 );
    __LookupMethod_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(table_JPanel, __LookupMethod_JComboBox,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel(
        "Optional - how to lookup values (blank=" + LookupMethodType.INTERPOLATE + ")."), 
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Out of range lookup method:" ), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutOfRangeLookupMethod_JComboBox = new SimpleJComboBox ( false );
    List<String> rangeChoices = new ArrayList<String>();
    rangeChoices.add ( "" );
    rangeChoices.add ( "" + OutOfRangeLookupMethodType.EXTRAPOLATE );
    rangeChoices.add ( "" + OutOfRangeLookupMethodType.SET_MISSING );
    rangeChoices.add ( "" + OutOfRangeLookupMethodType.USE_END_VALUE );
    __OutOfRangeLookupMethod_JComboBox.setData(rangeChoices);
    __OutOfRangeLookupMethod_JComboBox.select ( 0 );
    __OutOfRangeLookupMethod_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(table_JPanel, __OutOfRangeLookupMethod_JComboBox,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel(
        "Optional - how to lookup values outside table values (default=" + OutOfRangeLookupMethodType.SET_MISSING  + ")."), 
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Out of range notification:" ), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutOfRangeNotification_JComboBox = new SimpleJComboBox ( false );
    List<String> notifyChoices = new ArrayList<String>();
    notifyChoices.add ( "" );
    notifyChoices.add ( "" + __command._Ignore );
    notifyChoices.add ( "" + __command._Warn );
    notifyChoices.add ( "" + __command._Fail );
    __OutOfRangeNotification_JComboBox.setData(notifyChoices);
    __OutOfRangeNotification_JComboBox.select ( 0 );
    __OutOfRangeNotification_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(table_JPanel, __OutOfRangeNotification_JComboBox,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel(
        "Optional - how to notify about out of range values (default=" + __command._Ignore + ")."), 
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Transformation:" ), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Transformation_JComboBox = new SimpleJComboBox ( false );
    List<String> transChoices = new ArrayList<String>();
    transChoices.add ( "" );
    transChoices.add ( "" + DataTransformationType.NONE );
    transChoices.add ( "" + DataTransformationType.LOG );
    __Transformation_JComboBox.setData(transChoices);
    __Transformation_JComboBox.select ( 0 );
    __Transformation_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(table_JPanel, __Transformation_JComboBox,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel(
        "Optional - how to transform data if interpolating (blank=" + DataTransformationType.NONE + ")."), 
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Value to use when log and <= 0:" ), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __LEZeroLogValue_JTextField = new JTextField ( 10 );
    __LEZeroLogValue_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __LEZeroLogValue_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel(
        "Optional - value to substitute when original is <= 0 and log transform (default=" +
        TSRegression.getDefaultLEZeroLogValue() + ")."), 
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for period and window parameters
    int yTime = -1;
    JPanel time_JPanel = new JPanel();
    time_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Period and Window", time_JPanel );
    
    JGUIUtil.addComponent(time_JPanel, new JLabel (
        "Specify the period to set data.  The window also can be specified to process only part of each year." ),
        0, ++yTime, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(time_JPanel, new JLabel (
        "The window can be specified using the choices, or processor ${Property}." ),
        0, ++yTime, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(time_JPanel, new JSeparator (SwingConstants.HORIZONTAL ), 
        0, ++yTime, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(time_JPanel, new JLabel ( "Set start:" ),
        0, ++yTime, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SetStart_JTextField = new JTextField ( "", 20 );
    __SetStart_JTextField.setToolTipText("Specify the set start using a date/time string or ${Property} notation");
    __SetStart_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(time_JPanel, __SetStart_JTextField,
        1, yTime, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(time_JPanel, new JLabel(
        "Optional - set start date/time (default=full time series period)."),
        3, yTime, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(time_JPanel, new JLabel ( "Set end:" ), 
        0, ++yTime, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SetEnd_JTextField = new JTextField ( "", 20 );
    __SetEnd_JTextField.setToolTipText("Specify the set end using a date/time string or ${Property} notation");
    __SetEnd_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(time_JPanel, __SetEnd_JTextField,
        1, yTime, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(time_JPanel, new JLabel(
        "Optional - set end date/time (default=full time series period)."),
        3, yTime, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    __SetWindow_JCheckBox = new JCheckBox ( "Set window:", false );
    __SetWindow_JCheckBox.addActionListener ( this );
    JGUIUtil.addComponent(time_JPanel, __SetWindow_JCheckBox, 
        0, ++yTime, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    JPanel setWindow_JPanel = new JPanel();
    setWindow_JPanel.setLayout(new GridBagLayout());
    __SetWindowStart_JPanel = new DateTime_JPanel ( "Start", TimeInterval.MONTH, TimeInterval.MINUTE, null );
    __SetWindowStart_JPanel.addActionListener(this);
    __SetWindowStart_JPanel.addKeyListener ( this );
    JGUIUtil.addComponent(setWindow_JPanel, __SetWindowStart_JPanel,
        1, 0, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    // TODO SAM 2008-01-23 Figure out how to display the correct limits given the time series interval
    __SetWindowEnd_JPanel = new DateTime_JPanel ( "End", TimeInterval.MONTH, TimeInterval.MINUTE, null );
    __SetWindowEnd_JPanel.addActionListener(this);
    __SetWindowEnd_JPanel.addKeyListener ( this );
    JGUIUtil.addComponent(setWindow_JPanel, __SetWindowEnd_JPanel,
        4, 0, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(time_JPanel, setWindow_JPanel,
        1, yTime, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(time_JPanel, new JLabel(
        "Optional - set window within input year (default=full year)."),
        3, yTime, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(time_JPanel,new JLabel( "Set window start ${Property}:"),
        0, ++yTime, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SetWindowStart_JTextField = new JTextField ( "", 20 );
    __SetWindowStart_JTextField.setToolTipText("Specify the output window start ${Property} - will override the above.");
    __SetWindowStart_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(time_JPanel, __SetWindowStart_JTextField,
        1, yTime, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(time_JPanel, new JLabel ( "Optional (default=full year)."),
        3, yTime, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(time_JPanel,new JLabel("Set window end ${Property}:"),
        0, ++yTime, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SetWindowEnd_JTextField = new JTextField ( "", 20 );
    __SetWindowEnd_JTextField.setToolTipText("Specify the set window end ${Property} - will override the above.");
    __SetWindowEnd_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(time_JPanel, __SetWindowEnd_JTextField,
        1, yTime, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(time_JPanel, new JLabel ( "Optional (default=full year)."),
        3, yTime, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

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
	checkGUIState(); // To make sure the set window is OK

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
{	String routine = getClass().getSimpleName() + ".refresh";
	String InputTSID = "";
	String OutputTSID = "";
    String TableID = "";
    String TableTSIDColumn = "";
    String TableTSIDFormat = "";
    String TableValue1Column = "";
    String SortInput = "";
    String TableValue2Column = "";
	String EffectiveDateColumn = "";
	String LookupMethod = "";
    String OutOfRangeLookupMethod = "";
    String OutOfRangeNotification = "";
    String Transformation = "";
    String LEZeroLogValue = "";
    String SetStart = "";
    String SetEnd = "";
    String SetWindowStart = "";
    String SetWindowEnd = "";
	PropList props = __command.getCommandParameters();
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		InputTSID = props.getValue ( "InputTSID" );
		OutputTSID = props.getValue ( "OutputTSID" );
        TableID = props.getValue ( "TableID" );
        TableTSIDColumn = props.getValue ( "TableTSIDColumn" );
        TableTSIDFormat = props.getValue ( "TableTSIDFormat" );
        TableValue1Column = props.getValue ( "TableValue1Column" );
        SortInput = props.getValue ( "SortInput" );
        TableValue2Column = props.getValue ( "TableValue2Column" );
		EffectiveDateColumn = props.getValue ( "EffectiveDateColumn" );
		LookupMethod = props.getValue("LookupMethod");
		OutOfRangeLookupMethod = props.getValue("OutOfRangeLookupMethod");
		OutOfRangeNotification = props.getValue("OutOfRangeNotification");
		Transformation = props.getValue("Transformation");
        LEZeroLogValue = props.getValue ( "LEZeroLogValue" );
        SetStart = props.getValue ( "SetStart" );
        SetEnd = props.getValue ( "SetEnd" );
        SetWindowStart = props.getValue ( "SetWindowStart" );
        SetWindowEnd = props.getValue ( "SetWindowEnd" );
		// Now select the item in the list.  If not a match, print a warning.
		if ( JGUIUtil.isSimpleJComboBoxItem( __InputTSID_JComboBox, InputTSID, JGUIUtil.NONE, null, null ) ) {
		    __InputTSID_JComboBox.select ( InputTSID );
		}
		else {
		    // Automatically add to the list...
			if ( (InputTSID != null) && (InputTSID.length() > 0) ) {
				__InputTSID_JComboBox.insertItemAt ( InputTSID, 0 );
				// Select...
				__InputTSID_JComboBox.select ( InputTSID );
			}
			else {
			    // Select the first choice...
				if ( __InputTSID_JComboBox.getItemCount() > 0 ) {
					__InputTSID_JComboBox.select ( 0 );
				}
			}
		}
        if ( JGUIUtil.isSimpleJComboBoxItem( __OutputTSID_JComboBox, OutputTSID, JGUIUtil.NONE, null, null ) ) {
            __OutputTSID_JComboBox.select ( OutputTSID );
        }
        else {
            // Automatically add to the list...
            if ( (OutputTSID != null) && (OutputTSID.length() > 0) ) {
                __OutputTSID_JComboBox.insertItemAt ( OutputTSID, 0 );
                // Select...
                __OutputTSID_JComboBox.select ( OutputTSID );
            }
            else {
                // Select the first choice...
                if ( __OutputTSID_JComboBox.getItemCount() > 0 ) {
                    __OutputTSID_JComboBox.select ( 0 );
                }
            }
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
        if (TableTSIDFormat != null ) {
            __TableTSIDFormat_JTextField.setText(TableTSIDFormat.trim());
        }
        if (TableValue1Column != null ) {
            __TableValue1Column_JTextField.setText(TableValue1Column.trim());
        }
        if ( JGUIUtil.isSimpleJComboBoxItem( __SortInput_JComboBox, SortInput, JGUIUtil.NONE, null, null ) ) {
            __SortInput_JComboBox.select ( SortInput );
        }
        else {
            if ( (SortInput == null) || SortInput.equals("") ) {
                // Set default...
                __SortInput_JComboBox.select ( 0 );
            }
            else {
                Message.printWarning ( 1, routine, "Existing command references an invalid SortInput \"" +
                    SortInput + "\".  Select a different value or Cancel." );
            }
        }
        if (TableValue2Column != null ) {
            __TableValue2Column_JTextField.setText(TableValue2Column.trim());
        }
        if (EffectiveDateColumn != null ) {
            __EffectiveDateColumn_JTextField.setText(EffectiveDateColumn.trim());
        }
        if ( JGUIUtil.isSimpleJComboBoxItem( __LookupMethod_JComboBox, LookupMethod, JGUIUtil.NONE, null, null ) ) {
            __LookupMethod_JComboBox.select ( LookupMethod );
        }
        else {
            if ( (LookupMethod == null) || LookupMethod.equals("") ) {
                // Set default...
                __LookupMethod_JComboBox.select ( 0 );
            }
            else {
                Message.printWarning ( 1, routine, "Existing command references an invalid LookupMethod \"" +
                    LookupMethod + "\".  Select a different type or Cancel." );
            }
        }
        if ( JGUIUtil.isSimpleJComboBoxItem( __OutOfRangeLookupMethod_JComboBox, OutOfRangeLookupMethod, JGUIUtil.NONE, null, null ) ) {
            __OutOfRangeLookupMethod_JComboBox.select ( OutOfRangeLookupMethod );
        }
        else {
            if ( (OutOfRangeLookupMethod == null) || OutOfRangeLookupMethod.equals("") ) {
                // Set default...
                __OutOfRangeLookupMethod_JComboBox.select ( 0 );
            }
            else {
                Message.printWarning ( 1, routine, "Existing command references an invalid OutOfRangeLookupMethod \"" +
                    OutOfRangeLookupMethod + "\".  Select a different type or Cancel." );
            }
        }
        if ( JGUIUtil.isSimpleJComboBoxItem( __OutOfRangeNotification_JComboBox, OutOfRangeNotification, JGUIUtil.NONE, null, null ) ) {
            __OutOfRangeNotification_JComboBox.select ( OutOfRangeNotification );
        }
        else {
            if ( (OutOfRangeNotification == null) || OutOfRangeNotification.equals("") ) {
                // Set default...
                __OutOfRangeNotification_JComboBox.select ( 0 );
            }
            else {
                Message.printWarning ( 1, routine, "Existing command references an invalid OutOfRangeNotification \"" +
                    OutOfRangeNotification + "\".  Select a different type or Cancel." );
            }
        }
        if ( JGUIUtil.isSimpleJComboBoxItem( __Transformation_JComboBox, Transformation, JGUIUtil.NONE, null, null ) ) {
            __Transformation_JComboBox.select ( Transformation );
        }
        else {
            if ( (Transformation == null) || Transformation.equals("") ) {
                // Set default...
                __Transformation_JComboBox.select ( 0 );
            }
            else {
                Message.printWarning ( 1, routine, "Existing command references an invalid transformation \"" +
                    Transformation + "\".  Select a different type or Cancel." );
            }
        }
        if ( LEZeroLogValue != null ) {
            __LEZeroLogValue_JTextField.setText ( LEZeroLogValue );
        }
        if ( SetStart != null ) {
            __SetStart_JTextField.setText( SetStart );
        }
        if ( SetEnd != null ) {
            __SetEnd_JTextField.setText ( SetEnd );
        }
        if ( (SetWindowStart != null) && !SetWindowStart.isEmpty() ) {
        	if ( SetWindowStart.indexOf("${") >= 0 ) {
        		__SetWindowStart_JTextField.setText ( SetWindowStart );
        	}
        	else {
	            try {
	                // Add year because it is not part of the parameter value...
	                DateTime SetWindowStart_DateTime = DateTime.parse ( "0000-" + SetWindowStart );
	                Message.printStatus(2, routine, "Setting window start to " + SetWindowStart_DateTime );
	                __SetWindowStart_JPanel.setDateTime ( SetWindowStart_DateTime );
	            }
	            catch ( Exception e ) {
	                Message.printWarning( 1, routine, "SetWindowStart (" + SetWindowStart +
	                        ") is not a valid date/time." );
	            }
        	}
        }
        if ( (SetWindowEnd != null) && !SetWindowEnd.isEmpty() ) {
        	if ( SetWindowEnd.indexOf("${") >= 0 ) {
        		__SetWindowEnd_JTextField.setText ( SetWindowEnd );
        	}
        	else {
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
        }
        if ( (SetWindowStart != null) && !SetWindowStart.isEmpty() &&
            (SetWindowEnd != null) && !SetWindowEnd.isEmpty() ) {
            __SetWindow_JCheckBox.setSelected ( true );
        }
        else {
            __SetWindow_JCheckBox.setSelected ( false );
        }
	}
	// Regardless, reset the command from the fields...
	checkGUIState();
	InputTSID = __InputTSID_JComboBox.getSelected();
	OutputTSID = __OutputTSID_JComboBox.getSelected();
    TableID = __TableID_JComboBox.getSelected();
    TableTSIDColumn = __TableTSIDColumn_JTextField.getText().trim();
    TableTSIDFormat = __TableTSIDFormat_JTextField.getText().trim();
    TableValue1Column = __TableValue1Column_JTextField.getText().trim();
    SortInput = __SortInput_JComboBox.getSelected();
    TableValue2Column = __TableValue2Column_JTextField.getText().trim();
	EffectiveDateColumn = __EffectiveDateColumn_JTextField.getText().trim();
	LookupMethod = __LookupMethod_JComboBox.getSelected();
	OutOfRangeLookupMethod = __OutOfRangeLookupMethod_JComboBox.getSelected();
	OutOfRangeNotification = __OutOfRangeNotification_JComboBox.getSelected();
    Transformation = __Transformation_JComboBox.getSelected();
    LEZeroLogValue = __LEZeroLogValue_JTextField.getText().trim();
    SetStart = __SetStart_JTextField.getText().trim();
    SetEnd = __SetEnd_JTextField.getText().trim();
	props = new PropList ( __command.getCommandName() );
	props.add ( "InputTSID=" + InputTSID );
	props.add ( "OutputTSID=" + OutputTSID );
    props.add ( "TableID=" + TableID );
    props.add ( "TableTSIDColumn=" + TableTSIDColumn );
    props.add ( "TableTSIDFormat=" + TableTSIDFormat );
    props.add ( "TableValue1Column=" + TableValue1Column );
    props.add ( "SortInput=" + SortInput );
    props.add ( "TableValue2Column=" + TableValue2Column );
    props.add ( "EffectiveDateColumn=" + EffectiveDateColumn );
    props.add ( "LookupMethod=" + LookupMethod );
    props.add ( "OutOfRangeLookupMethod=" + OutOfRangeLookupMethod );
    props.add ( "OutOfRangeNotification=" + OutOfRangeNotification );
    props.add ( "Transformation=" + Transformation );
    props.add ( "LEZeroLogValue=" + LEZeroLogValue );
    props.add ( "SetStart=" + SetStart );
    props.add ( "SetEnd=" + SetEnd );
    if ( __SetWindow_JCheckBox.isSelected() ) {
        SetWindowStart = __SetWindowStart_JPanel.toString(false,true).trim();
        if ( SetWindowStart.startsWith("99") ) {
        	// 99 is used as placeholder when month is not set... artifact of setting choices to blank during editing
        	SetWindowStart = "";
        }
        SetWindowEnd = __SetWindowEnd_JPanel.toString(false,true).trim();
        if ( SetWindowEnd.startsWith("99") ) {
        	// 99 is used as placeholder when month is not set... artifact of setting choices to blank during editing
        	SetWindowEnd = "";
        }
        String SetWindowStart2 = __SetWindowStart_JTextField.getText().trim();
        String SetWindowEnd2 = __SetWindowEnd_JTextField.getText().trim();
        if ( (SetWindowStart2 != null) && !SetWindowStart2.isEmpty() ) {
        	SetWindowStart = SetWindowStart2;
        }
        if ( (SetWindowEnd2 != null) && !SetWindowEnd2.isEmpty() ) {
        	SetWindowEnd = SetWindowEnd2;
        }
        props.add ( "SetWindowStart=" + SetWindowStart );
        props.add ( "SetWindowEnd=" + SetWindowEnd );
    }
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