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

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.TS.TSIdent;
import RTi.TS.TSIdent_JDialog;
import RTi.TS.TSRegression;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.PropList;
import RTi.Util.Math.DataTransformationType;
import RTi.Util.Message.Message;
import RTi.Util.Table.LookupMethodType;
import RTi.Util.Table.OutOfRangeLookupMethodType;

/**
Editor for the LookupTimeSeriesFromTable command.
*/
public class LookupTimeSeriesFromTable_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private JFrame __parent_JFrame = null;
private LookupTimeSeriesFromTable_Command __command = null;
private JTextArea __command_JTextArea=null;
private SimpleJComboBox	__TSID_JComboBox = null;// Time series available to operate on.
private JTextArea __NewTSID_JTextArea = null; // New TSID.
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
private SimpleJComboBox __TableID_JComboBox = null;
private JTextField __TableTSIDColumn_JTextField = null;
private TSFormatSpecifiersJPanel __TableTSIDFormat_JTextField = null; // Format for time series identifiers
private JTextField __TableValue1Column_JTextField = null;
private JTextField __TableValue2Column_JTextField = null;
private JTextField __Units_JTextField = null;
private JTextField __EffectiveDateColumn_JTextField = null;
private SimpleJComboBox __LookupMethod_JComboBox = null;
private SimpleJComboBox __OutOfRangeLookupMethod_JComboBox = null;
private SimpleJComboBox __OutOfRangeNotification_JComboBox = null;
private SimpleJComboBox __Transformation_JComboBox = null;
private JTextField __LEZeroLogValue_JTextField = null;
private JTextField __AnalysisStart_JTextField = null;
private JTextField __AnalysisEnd_JTextField = null;
// TODO SAM 2012-02-04 Might need flag for values outside the rating
private SimpleJButton __edit_JButton = null;
private SimpleJButton __clear_JButton = null;
private boolean	__error_wait = false; // Is there an error to be cleared up?
private boolean	__first_time = true;
private boolean	__ok = false; // Whether OK has been pressed.

/**
Command editor dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices choices for TableID value.
*/
public LookupTimeSeriesFromTable_JDialog ( JFrame parent,
    LookupTimeSeriesFromTable_Command command, List<String> tableIDChoices )
{	super(parent, true);
	initialize ( parent, command, tableIDChoices );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	Object o = event.getSource();
	String routine = "Copy_JDialog.actionPerformed";

	if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __clear_JButton ) {
		__NewTSID_JTextArea.setText ( "" );
        refresh();
	}
	else if ( o == __edit_JButton ) {
		// Edit the NewTSID in the dialog.  It is OK for the string to be blank.
		String NewTSID = __NewTSID_JTextArea.getText().trim();
		TSIdent tsident;
		try {
		    if ( NewTSID.length() == 0 ) {
				tsident = new TSIdent();
			}
			else {
			    tsident = new TSIdent ( NewTSID );
			}
			TSIdent tsident2=(new TSIdent_JDialog ( __parent_JFrame, true, tsident, null )).response();
			if ( tsident2 != null ) {
				__NewTSID_JTextArea.setText ( tsident2.toString(true) );
				refresh();
			}
		}
		catch ( Exception e ) {
			Message.printWarning ( 1, routine,
			"Error creating time series identifier from \"" + NewTSID + "\"." );
			Message.printWarning ( 3, routine, e );
		}
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
        refresh ();
    }
}

//Start event handlers for DocumentListener...

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void changedUpdate ( DocumentEvent e )
{
    refresh();
}

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void insertUpdate ( DocumentEvent e )
{
    refresh();
}

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void removeUpdate ( DocumentEvent e )
{
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
	String Alias = __Alias_JTextField.getText().trim();
	String TSID = __TSID_JComboBox.getSelected();
	String NewTSID = __NewTSID_JTextArea.getText().trim();
    String TableID = __TableID_JComboBox.getSelected();
    String TableTSIDColumn = __TableTSIDColumn_JTextField.getText().trim();
    String TableTSIDFormat = __TableTSIDFormat_JTextField.getText().trim();
    String TableValue1Column = __TableValue1Column_JTextField.getText().trim();
    String TableValue2Column = __TableValue1Column_JTextField.getText().trim();
	String Units = __Units_JTextField.getText().trim();
	String EffectiveDateColumn = __EffectiveDateColumn_JTextField.getText().trim();
	String LookupMethod = __LookupMethod_JComboBox.getSelected();
	String OutOfRangeLookupMethod = __OutOfRangeLookupMethod_JComboBox.getSelected();
	String OutOfRangeNotification = __OutOfRangeNotification_JComboBox.getSelected();
    String Transformation = __Transformation_JComboBox.getSelected();
    String LEZeroLogValue = __LEZeroLogValue_JTextField.getText().trim();
	String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	__error_wait = false;

	if ( Alias.length() > 0 ) {
		props.set ( "Alias", Alias );
	}
	if ( (TSID != null) && (TSID.length() > 0) ) {
		props.set ( "TSID", TSID );
	}
	if ( (NewTSID != null) && (NewTSID.length() > 0) ) {
		props.set ( "NewTSID", NewTSID );
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
    if ( TableValue2Column.length() > 0 ) {
        props.set ( "TableValue2Column", TableValue2Column );
    }
    if ( Units.length() > 0 ) {
        props.set ( "Units", Units );
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
    if ( AnalysisStart.length() > 0 ) {
        props.set ( "AnalysisStart", AnalysisStart );
    }
    if ( AnalysisEnd.length() > 0 ) {
        props.set ( "AnalysisEnd", AnalysisEnd );
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
{	String Alias = __Alias_JTextField.getText().trim();
	String TSID = __TSID_JComboBox.getSelected();
	String NewTSID = __NewTSID_JTextArea.getText().trim();
    String TableID = __TableID_JComboBox.getSelected();
    String TableTSIDColumn = __TableTSIDColumn_JTextField.getText().trim();
    String TableTSIDFormat = __TableTSIDFormat_JTextField.getText().trim();
    String TableValue1Column = __TableValue1Column_JTextField.getText().trim();
    String TableValue2Column = __TableValue2Column_JTextField.getText().trim();
	String Units = __Units_JTextField.getText().trim();
	String EffectiveDateColumn = __EffectiveDateColumn_JTextField.getText().trim();
    String Transformation = __Transformation_JComboBox.getSelected();
    String LookupMethod = __LookupMethod_JComboBox.getSelected();
    String OutOfRangeLookupMethod = __OutOfRangeLookupMethod_JComboBox.getSelected();
    String OutOfRangeNotification = __OutOfRangeNotification_JComboBox.getSelected();
    String LEZeroLogValue = __LEZeroLogValue_JTextField.getText().trim();
    String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
    String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	__command.setCommandParameter ( "Alias", Alias );
	__command.setCommandParameter ( "TSID", TSID );
	__command.setCommandParameter ( "NewTSID", NewTSID );
    __command.setCommandParameter ( "TableID", TableID );
    __command.setCommandParameter ( "TableTSIDColumn", TableTSIDColumn );
    __command.setCommandParameter ( "TableTSIDFormat", TableTSIDFormat );
    __command.setCommandParameter ( "TableValue1Column", TableValue1Column );
    __command.setCommandParameter ( "TableValue2Column", TableValue2Column );
	__command.setCommandParameter ( "Units", Units );
    __command.setCommandParameter ( "EffectiveDateColumn", EffectiveDateColumn );
    __command.setCommandParameter ( "LookupMethod", LookupMethod );
    __command.setCommandParameter ( "OutOfRangeLookupMethod", OutOfRangeLookupMethod );
    __command.setCommandParameter ( "OutOfRangeNotification", OutOfRangeNotification );
    __command.setCommandParameter ( "Transformation", Transformation );
    __command.setCommandParameter ( "LEZeroLogValue", LEZeroLogValue );
	__command.setCommandParameter ( "AnalysisStart", AnalysisStart );
	__command.setCommandParameter ( "AnalysisEnd", AnalysisEnd );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__Alias_JTextField = null;
	__TSID_JComboBox = null;
	__NewTSID_JTextArea = null;
	__cancel_JButton = null;
	__command_JTextArea = null;
	__ok_JButton = null;
	__parent_JFrame = null;
	super.finalize ();
}

/**
Instantiates the GUI components.
@param parent Frame class instantiating this class.
@param command Command to edit.
@param tableIDChoices list of choices for table identifiers
*/
private void initialize ( JFrame parent, LookupTimeSeriesFromTable_Command command, List<String> tableIDChoices )
{	__parent_JFrame = parent;
	__command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

	JGUIUtil.addComponent(main_JPanel, new JLabel (
        "<html><b>This command is under development - parameter editing is enabled; however all output " +
        "is missing.</b></html>." ),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Create a new time series by using an input time series and lookup table." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specify new time series identifier (TSID) information for the copy to avoid errors " +
		"with the copy being mistaken for the original." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel("Input time series:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__TSID_JComboBox = new SimpleJComboBox ( true ); // Allow edit
	List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
		(TSCommandProcessor)__command.getCommandProcessor(), __command );
	__TSID_JComboBox.setData ( tsids );
	__TSID_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __TSID_JComboBox,
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "New time series ID:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__NewTSID_JTextArea = new JTextArea ( 3, 25 );
    __NewTSID_JTextArea.setEditable(false);
	__NewTSID_JTextArea.setLineWrap ( true );
	__NewTSID_JTextArea.setWrapStyleWord ( true );
	__NewTSID_JTextArea.addKeyListener ( this );
	// Make 3-high to fit in the edit button...
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__NewTSID_JTextArea),
		1, y, 2, 3, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Required - specify to avoid confusion with TSID from original time series."), 
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	y += 2;
    JGUIUtil.addComponent(main_JPanel, (__edit_JButton =
		new SimpleJButton ( "Edit", "Edit", this ) ),
		3, y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(main_JPanel, (__clear_JButton =
		new SimpleJButton ( "Clear", "Clear", this ) ),
		4, y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel("Alias to assign:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Alias_JTextField = new TSFormatSpecifiersJPanel(15);
    __Alias_JTextField.addKeyListener ( this );
    __Alias_JTextField.getDocument().addDocumentListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Alias_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - use %L for location, etc."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Lookup table ID:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableID_JComboBox = new SimpleJComboBox ( 12, true );    // Allow edit
    tableIDChoices.add(0,""); // Add blank to ignore table
    __TableID_JComboBox.setData ( tableIDChoices );
    __TableID_JComboBox.addItemListener ( this );
    //__TableID_JComboBox.setMaximumRowCount(tableIDChoices.size());
    JGUIUtil.addComponent(main_JPanel, __TableID_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Required - lookup table."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Table TSID column:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableTSIDColumn_JTextField = new JTextField ( 10 );
    __TableTSIDColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __TableTSIDColumn_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Optional - column name for input time series TSID."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel("Format of TSID:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableTSIDFormat_JTextField = new TSFormatSpecifiersJPanel(10);
    __TableTSIDFormat_JTextField.setToolTipText("Use %L for location, %T for data type, %I for interval.");
    __TableTSIDFormat_JTextField.addKeyListener ( this );
    __TableTSIDFormat_JTextField.getDocument().addDocumentListener(this);
    __TableTSIDFormat_JTextField.setToolTipText("%L for location, %T for data type.");
    JGUIUtil.addComponent(main_JPanel, __TableTSIDFormat_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - use %L for location, etc. (default=alias or TSID)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Column for input value:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableValue1Column_JTextField = new JTextField ( "", 10 );
    JGUIUtil.addComponent(main_JPanel, __TableValue1Column_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __TableValue1Column_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Required - column for input time series values."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Column for output value:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableValue2Column_JTextField = new JTextField ( "", 10 );
    JGUIUtil.addComponent(main_JPanel, __TableValue2Column_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __TableValue2Column_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Required - column for output time series values."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Output data units:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Units_JTextField = new JTextField ( "", 10 );
    JGUIUtil.addComponent(main_JPanel, __Units_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __Units_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - for example:  ACFT, CFS, IN (default=no units)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Column for effective date:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __EffectiveDateColumn_JTextField = new JTextField ( "", 10 );
    JGUIUtil.addComponent(main_JPanel, __EffectiveDateColumn_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __EffectiveDateColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - column for lookup data effective date."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Lookup method:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __LookupMethod_JComboBox = new SimpleJComboBox ( false );
    __LookupMethod_JComboBox.addItem ( "" );
    __LookupMethod_JComboBox.addItem ( "" + LookupMethodType.INTERPOLATE );
    __LookupMethod_JComboBox.addItem ( "" + LookupMethodType.PREVIOUS_VALUE );
    __LookupMethod_JComboBox.addItem ( "" + LookupMethodType.NEXT_VALUE );
    __LookupMethod_JComboBox.select ( 0 );
    __LookupMethod_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __LookupMethod_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - how to lookup values (blank=" + LookupMethodType.INTERPOLATE + ")."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Out of range lookup method:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutOfRangeLookupMethod_JComboBox = new SimpleJComboBox ( false );
    __OutOfRangeLookupMethod_JComboBox.addItem ( "" );
    __OutOfRangeLookupMethod_JComboBox.addItem ( "" + OutOfRangeLookupMethodType.EXTRAPOLATE );
    __OutOfRangeLookupMethod_JComboBox.addItem ( "" + OutOfRangeLookupMethodType.SET_MISSING );
    __OutOfRangeLookupMethod_JComboBox.addItem ( "" + OutOfRangeLookupMethodType.USE_END_VALUE );
    __OutOfRangeLookupMethod_JComboBox.select ( 0 );
    __OutOfRangeLookupMethod_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __OutOfRangeLookupMethod_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - how to lookup values outside table values (default=" + OutOfRangeLookupMethodType.SET_MISSING  + ")."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Out of range notification:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutOfRangeNotification_JComboBox = new SimpleJComboBox ( false );
    __OutOfRangeNotification_JComboBox.addItem ( "" );
    __OutOfRangeNotification_JComboBox.addItem ( "" + __command._Ignore );
    __OutOfRangeNotification_JComboBox.addItem ( "" + __command._Warn );
    __OutOfRangeNotification_JComboBox.addItem ( "" + __command._Fail );
    __OutOfRangeNotification_JComboBox.select ( 0 );
    __OutOfRangeNotification_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __OutOfRangeNotification_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - how to notify about out of range values (default=" + __command._Ignore + ")."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Transformation:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Transformation_JComboBox = new SimpleJComboBox ( false );
    __Transformation_JComboBox.addItem ( "" );
    __Transformation_JComboBox.addItem ( "" + DataTransformationType.NONE );
    __Transformation_JComboBox.addItem ( "" + DataTransformationType.LOG );
    __Transformation_JComboBox.select ( 0 );
    __Transformation_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Transformation_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - how to transform data if interpolating (blank=" + DataTransformationType.NONE + ")."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Value to use when log and <= 0:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __LEZeroLogValue_JTextField = new JTextField ( 10 );
    __LEZeroLogValue_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __LEZeroLogValue_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - value to substitute when original is <= 0 and log transform (default=" +
        TSRegression.getDefaultLEZeroLogValue() + ")."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Analysis start:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __AnalysisStart_JTextField = new JTextField ( "", 20 );
    __AnalysisStart_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __AnalysisStart_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - analysis start date/time (default=full time series period)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Analysis end:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __AnalysisEnd_JTextField = new JTextField ( "", 20 );
    __AnalysisEnd_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __AnalysisEnd_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - analysis end date/time (default=full time series period)."),
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
	refresh();

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	button_JPanel.add(__cancel_JButton = new SimpleJButton("Cancel", this));
	button_JPanel.add ( __ok_JButton = new SimpleJButton("OK", this) );

	setTitle ( "Edit " + __command.getCommandName() + "() Command" );

	setResizable ( true );
    pack();
    JGUIUtil.center( this );
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
    String Alias = "";
	String TSID = "";
	String NewTSID = "";
    String TableID = "";
    String TableTSIDColumn = "";
    String TableTSIDFormat = "";
    String TableValue1Column = "";
    String TableValue2Column = "";
	String Units = "";
	String EffectiveDateColumn = "";
	String LookupMethod = "";
    String OutOfRangeLookupMethod = "";
    String OutOfRangeNotification = "";
    String Transformation = "";
    String LEZeroLogValue = "";
    String AnalysisStart = "";
    String AnalysisEnd = "";
	PropList props = __command.getCommandParameters();
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		Alias = props.getValue ( "Alias" );
		TSID = props.getValue ( "TSID" );
		NewTSID = props.getValue ( "NewTSID" );
        TableID = props.getValue ( "TableID" );
        TableTSIDColumn = props.getValue ( "TableTSIDColumn" );
        TableTSIDFormat = props.getValue ( "TableTSIDFormat" );
        TableValue1Column = props.getValue ( "TableValue1Column" );
        TableValue2Column = props.getValue ( "TableValue2Column" );
		Units = props.getValue ( "Units" );
		EffectiveDateColumn = props.getValue ( "EffectiveDateColumn" );
		LookupMethod = props.getValue("LookupMethod");
		OutOfRangeLookupMethod = props.getValue("OutOfRangeLookupMethod");
		OutOfRangeNotification = props.getValue("OutOfRangeNotification");
		Transformation = props.getValue("Transformation");
        LEZeroLogValue = props.getValue ( "LEZeroLogValue" );
        AnalysisStart = props.getValue ( "AnalysisStart" );
        AnalysisEnd = props.getValue ( "AnalysisEnd" );
		if ( Alias != null ) {
			__Alias_JTextField.setText ( Alias );
		}
		// Now select the item in the list.  If not a match, print a warning.
		if ( JGUIUtil.isSimpleJComboBoxItem( __TSID_JComboBox, TSID, JGUIUtil.NONE, null, null ) ) {
		    __TSID_JComboBox.select ( TSID );
		}
		else {
		    // Automatically add to the list...
			if ( (TSID != null) && (TSID.length() > 0) ) {
				__TSID_JComboBox.insertItemAt ( TSID, 0 );
				// Select...
				__TSID_JComboBox.select ( TSID );
			}
			else {
			    // Select the first choice...
				if ( __TSID_JComboBox.getItemCount() > 0 ) {
					__TSID_JComboBox.select ( 0 );
				}
			}
		}
		if ( NewTSID != null ) {
			__NewTSID_JTextArea.setText ( NewTSID );
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
        if (TableValue2Column != null ) {
            __TableValue2Column_JTextField.setText(TableValue2Column.trim());
        }
        if ( Units != null ) {
            __Units_JTextField.setText( Units );
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
        if ( AnalysisStart != null ) {
            __AnalysisStart_JTextField.setText( AnalysisStart );
        }
        if ( AnalysisEnd != null ) {
            __AnalysisEnd_JTextField.setText ( AnalysisEnd );
        }
	}
	// Regardless, reset the command from the fields...
	Alias = __Alias_JTextField.getText().trim();
	TSID = __TSID_JComboBox.getSelected();
	NewTSID = __NewTSID_JTextArea.getText().trim();
    TableID = __TableID_JComboBox.getSelected();
    TableTSIDColumn = __TableTSIDColumn_JTextField.getText().trim();
    TableTSIDFormat = __TableTSIDFormat_JTextField.getText().trim();
    TableValue1Column = __TableValue1Column_JTextField.getText().trim();
    TableValue2Column = __TableValue2Column_JTextField.getText().trim();
	Units = __Units_JTextField.getText().trim();
	EffectiveDateColumn = __EffectiveDateColumn_JTextField.getText().trim();
	LookupMethod = __LookupMethod_JComboBox.getSelected();
	OutOfRangeLookupMethod = __OutOfRangeLookupMethod_JComboBox.getSelected();
	OutOfRangeNotification = __OutOfRangeNotification_JComboBox.getSelected();
    Transformation = __Transformation_JComboBox.getSelected();
    LEZeroLogValue = __LEZeroLogValue_JTextField.getText().trim();
    AnalysisStart = __AnalysisStart_JTextField.getText().trim();
    AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	props = new PropList ( __command.getCommandName() );
	props.add ( "Alias=" + Alias );
	props.add ( "TSID=" + TSID );
	props.add ( "NewTSID=" + NewTSID );
    props.add ( "TableID=" + TableID );
    props.add ( "TableTSIDColumn=" + TableTSIDColumn );
    props.add ( "TableTSIDFormat=" + TableTSIDFormat );
    props.add ( "TableValue1Column=" + TableValue1Column );
    props.add ( "TableValue2Column=" + TableValue2Column );
    props.add ( "Units=" + Units );
    props.add ( "EffectiveDateColumn=" + EffectiveDateColumn );
    props.add ( "LookupMethod=" + LookupMethod );
    props.add ( "OutOfRangeLookupMethod=" + OutOfRangeLookupMethod );
    props.add ( "OutOfRangeNotification=" + OutOfRangeNotification );
    props.add ( "Transformation=" + Transformation );
    props.add ( "LEZeroLogValue=" + LEZeroLogValue );
    props.add ( "AnalysisStart=" + AnalysisStart );
    props.add ( "AnalysisEnd=" + AnalysisEnd );
	__command_JTextArea.setText( __command.toString ( props ) );
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