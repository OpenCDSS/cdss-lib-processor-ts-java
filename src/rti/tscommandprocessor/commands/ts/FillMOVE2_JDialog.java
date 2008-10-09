// ----------------------------------------------------------------------------
// fillMOVE2_JDialog - editor for fillMOVE2()
// ----------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
// ----------------------------------------------------------------------------
// History: 
//
// 2002-02-25	Steven A. Malers, RTi	Initial version.  Copy and modify
//					fillRegression().
// 2002-03-22	SAM, RTi		Add parameter to indicate whether the
//					MOVE1 or MOVE2 procedure is used.
//					Enable the Log/None transformation and
//					Single/Monthly equations.
// 2002-03-31	SAM, RTi		Previously had an analysis period.
//					However, really need independent and
//					dependent analysis periods, in
//					addition to a fill period!
// 2002-12-11	SAM, RTi		Update to Swing.
// 2004-02-22	SAM, RTi		Change independent TS to single
//					selection mode.
// 2006-04-13	SAM, RTi		Update to new command class design.
// 2007-02-17	SAM, RTi		Use new CommandProcessor interface.
//					Clean up code based on Eclipse feedback.
// 2007-05-08	SAM, RTi		Cleanup code based on Eclipse feedback.
// ----------------------------------------------------------------------------
// EndHeader

package rti.tscommandprocessor.commands.ts;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.Vector;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.IO.Command;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

public class FillMOVE2_JDialog extends JDialog
implements ActionListener, KeyListener, ListSelectionListener, WindowListener
{

private SimpleJButton	__cancel_JButton = null,// Cancel Button
			__ok_JButton = null;	// Ok Button
private JTextArea	__command_JTextArea=null;
						// Command as JTextArea
/* REVISIT SAM 2006-04-16
Evaluate whether this can be supported
private JTextField	__Intercept_JTextField = null;
						// Intercept value as JTextField
*/
private JTextField	__DependentAnalysisStart_JTextField,
			__DependentAnalysisEnd_JTextField,
			__IndependentAnalysisStart_JTextField,
			__IndependentAnalysisEnd_JTextField,
						// Text fields for dependent
						// time series analysis period.
			__FillStart_JTextField, // Text fields for fill period.
			__FillEnd_JTextField,
			__FillFlag_JTextField;	// Flag to set for filled data.
private SimpleJComboBox	__TSID_JComboBox = null;// Field for time series alias
private SimpleJComboBox	__IndependentTSID_JComboBox= null;
						// List for independent time
						// series identifier(s)
private SimpleJComboBox	__NumberOfEquations_JComboBox = null;
						// One or monthly equations.
/* REVISIT SAM 2006-04-16
Evaluate whether this can be supported
private SimpleJComboBox	__AnalysisMonth_JComboBox = null;
						// Month to analyze - monthly
						// equations only.
*/
private SimpleJComboBox	__Transformation_JComboBox=null;// Linear or log
						// transformation.
private boolean		__error_wait = false;	// True if there is an error
						// in the input.
private boolean		__first_time = true;
private boolean		__ok = false;		// Was OK pressed last (false=
						// cancel)?
private FillMOVE2_Command __command = null;	// Command to edit

/**
fillMOVE2_JDialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public FillMOVE2_JDialog ( JFrame parent, Command command )
{	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	String s = event.getActionCommand();

	if ( s.equals("Cancel") ) {
		response ( false );
	}
	else if ( s.equals("OK") ) {
		refresh();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	/* REVISIT SAM 2005-04-27 enable button for TEMPTS
	else if ( s.equals(__CHANGE_TO_TEMPTS) ) {
		// Make sure that all selected time series start with the
		// string "TEMPTS" but don't duplicate it...
		JGUIUtil.addStringToSelected(__IndependentTSID_JList,"TEMPTS ");
		refresh();
	}
	else if ( s.equals(__REMOVE_TEMPTS_FLAG) ) {
		// If selected time series start with the string "TEMPTS" and
		// there is more than one token, remove the leading TEMPTS...
		JGUIUtil.removeStringFromSelected(
		__IndependentTSID_JList,"TEMPTS ");
		refresh();
	}
	*/
	else {	// A combo box.  Refresh the command...
		checkGUIState();
		refresh ();
	}
}

/**
Check the GUI state and make sure the proper components are enabled/disabled.
*/
private void checkGUIState()
{	// TODO SAM 2007-06-04-16 Evaluate NumberOfEquations
	//String NumberOfEquations = __NumberOfEquations_JComboBox.getSelected();
	/* REVISIT SAM 2006-04-16
		Evaluate whether this can be supported
	if ( NumberOfEquations.equalsIgnoreCase(__command._MonthlyEquations) ) {
		JGUIUtil.setEnabled(__AnalysisMonth_JComboBox,true);
	}
	else {	JGUIUtil.setEnabled(__AnalysisMonth_JComboBox,false);
	}
	*/

	//String Transformation = __Transformation_JComboBox.getSelected();
	/* REVISIT SAM 2006-04-16
	Evaluate whether this can be supported
	if (	Transformation.equalsIgnoreCase(__command._None) ||
		Transformation.equals("") ) {
		JGUIUtil.setEnabled(__Intercept_JTextField,true);
	}
	else {	JGUIUtil.setEnabled(__Intercept_JTextField,false);
	}
	*/
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
	PropList props = new PropList ( "" );
	String TSID = __TSID_JComboBox.getSelected();
	String IndependentTSID = __IndependentTSID_JComboBox.getSelected ();
	String Transformation = __Transformation_JComboBox.getSelected();
	/* REVISIT SAM 2006-04-16
	Evaluate whether this can be supported
	String Intercept = __Intercept_JTextField.getText().trim();
	*/
	String DependentAnalysisStart =
		__DependentAnalysisStart_JTextField.getText().trim();
	String DependentAnalysisEnd =
		__DependentAnalysisEnd_JTextField.getText().trim();
	String IndependentAnalysisStart =
		__IndependentAnalysisStart_JTextField.getText().trim();
	String IndependentAnalysisEnd =
		__IndependentAnalysisEnd_JTextField.getText().trim();
	String FillStart = __FillStart_JTextField.getText().trim();
	String FillEnd = __FillEnd_JTextField.getText().trim();
	String FillFlag = __FillFlag_JTextField.getText().trim();
	__error_wait = false;

	if ( TSID.length() > 0 ) {
		props.set ( "TSID", TSID );
	}
	if ( IndependentTSID.length() > 0 ) {
		props.set ( "IndependentTSID", IndependentTSID );
	}
	if ( Transformation.length() > 0 ) {
		props.set ( "Transformation", Transformation );
	}
	/* REVISIT SAM 2006-04-16
	Evaluate whether this can be supported
	if ( Intercept.length() > 0 ) {
		props.set ( "Intercept", Intercept );
	}
	*/
	if ( DependentAnalysisStart.length() > 0 ) {
		props.set ( "DependentAnalysisStart", DependentAnalysisStart );
	}
	if ( DependentAnalysisEnd.length() > 0 ) {
		props.set ( "DependentAnalysisEnd", DependentAnalysisEnd );
	}
	if ( IndependentAnalysisStart.length() > 0 ) {
		props.set ("IndependentAnalysisStart",IndependentAnalysisStart);
	}
	if ( IndependentAnalysisEnd.length() > 0 ) {
		props.set ( "IndependentAnalysisEnd", IndependentAnalysisEnd );
	}
	if ( FillStart.length() > 0 ) {
		props.set ( "FillStart", FillStart );
	}
	if ( FillEnd.length() > 0 ) {
		props.set ( "FillEnd", FillEnd );
	}
	if ( FillFlag.length() > 0 ) {
		props.set ( "FillFlag", FillFlag );
	}
	try {	// This will warn the user...
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
{	String TSID = __TSID_JComboBox.getSelected();
	String IndependentTSID = __IndependentTSID_JComboBox.getSelected();
	String NumberOfEquations = __NumberOfEquations_JComboBox.getSelected();
	// REVISIT SAM 2006-04-16
	// Evaluate whether this can be supported
	//String AnalysisMonth = __AnalysisMonth_JComboBox.getSelected();
	String Transformation = __Transformation_JComboBox.getSelected();
	/* REVISIT SAM 2006-04-16
	Evaluate whether this can be supported
	String Intercept = __Intercept_JTextField.getText().trim();
	*/
	String DependentAnalysisStart =
		__DependentAnalysisStart_JTextField.getText().trim();
	String DependentAnalysisEnd =
		__DependentAnalysisEnd_JTextField.getText().trim();
	String IndependentAnalysisStart =
		__IndependentAnalysisStart_JTextField.getText().trim();
	String IndependentAnalysisEnd =
		__IndependentAnalysisEnd_JTextField.getText().trim();
	String FillStart = __FillStart_JTextField.getText().trim();
	String FillEnd = __FillEnd_JTextField.getText().trim();
	String FillFlag = __FillFlag_JTextField.getText().trim();
	__command.setCommandParameter ( "TSID", TSID );
	__command.setCommandParameter ( "IndependentTSID", IndependentTSID );
	__command.setCommandParameter ( "NumberOfEquations", NumberOfEquations);
	/* REVISIT SAM 2006-04-16
	Evaluate whether this can be supported
	__command.setCommandParameter ( "AnalysisMonth", AnalysisMonth );
	*/
	__command.setCommandParameter ( "Transformation", Transformation );
	/* REVISIT SAM 2006-04-16
	Evaluate whether this can be supported
	__command.setCommandParameter ( "Intercept", Intercept );
	*/
	__command.setCommandParameter ( "DependentAnalysisStart",
		DependentAnalysisStart );
	__command.setCommandParameter ( "DependentAnalysisEnd",
		DependentAnalysisEnd );
	__command.setCommandParameter ( "IndependentAnalysisStart",
		IndependentAnalysisStart );
	__command.setCommandParameter ( "IndependentAnalysisEnd",
		IndependentAnalysisEnd );
	__command.setCommandParameter ( "FillStart", FillStart );
	__command.setCommandParameter ( "FillEnd", FillEnd );
	__command.setCommandParameter ( "FillFlag", FillFlag );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__TSID_JComboBox = null;
	__cancel_JButton = null;
	__NumberOfEquations_JComboBox = null;
	__command = null;
	__command_JTextArea = null;
	__IndependentTSID_JComboBox = null;
	__ok_JButton = null;
	__Transformation_JComboBox = null;
	super.finalize ();
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, Command command )
{	__command = (FillMOVE2_Command)command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"See the TSTool documentation for a description of the MOVE2 procedure." ), 
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The analysis period(s) will be used to determine the relationships used for filling." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Use a setOutputPeriod() command if the dependent time series period will be extended." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specify dates with precision appropriate for the data, " +
		"use * for all available data, OutputStart, or OutputEnd." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Time series to fill (dependent):" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__TSID_JComboBox = new SimpleJComboBox ( true );

	// Get the time series identifiers from the processor...
	
	Vector tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
			(TSCommandProcessor)__command.getCommandProcessor(), __command );
	
	__TSID_JComboBox.setData ( tsids );
	__TSID_JComboBox.addKeyListener ( this );
	__TSID_JComboBox.addActionListener ( this );
        JGUIUtil.addComponent(main_JPanel, __TSID_JComboBox,
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

        JGUIUtil.addComponent(main_JPanel,
		new JLabel ("Independent time series:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__IndependentTSID_JComboBox = new SimpleJComboBox ( true );
	__IndependentTSID_JComboBox.setData ( tsids );
	__IndependentTSID_JComboBox.addKeyListener ( this );
	__IndependentTSID_JComboBox.addActionListener ( this );
        JGUIUtil.addComponent(main_JPanel, __IndependentTSID_JComboBox,
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

        JGUIUtil.addComponent(main_JPanel, new JLabel ( "Number of equations:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__NumberOfEquations_JComboBox = new SimpleJComboBox ( false );
	__NumberOfEquations_JComboBox.addItem ( "" );	// Default
	__NumberOfEquations_JComboBox.addItem ( __command._OneEquation );
	__NumberOfEquations_JComboBox.addItem ( __command._MonthlyEquations );
	__NumberOfEquations_JComboBox.select ( __command._OneEquation );
	__NumberOfEquations_JComboBox.addActionListener ( this );
        JGUIUtil.addComponent(main_JPanel, __NumberOfEquations_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Number of equations to use (blank=one equation)."), 
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	/* TODO SAM 2006-04-16
		Evaluate whether this can be supported
        JGUIUtil.addComponent(main_JPanel, new JLabel ( "Analysis month:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__AnalysisMonth_JComboBox = new SimpleJComboBox ( false );
	__AnalysisMonth_JComboBox.setMaximumRowCount ( 13 );
	__AnalysisMonth_JComboBox.addItem ( "" );
	for ( int i = 1; i <= 12; i++ ) {
		__AnalysisMonth_JComboBox.addItem ( "" + i );
	}
	__AnalysisMonth_JComboBox.select ( 0 );	// No analysis month
	__AnalysisMonth_JComboBox.addActionListener ( this );
        JGUIUtil.addComponent(main_JPanel, __AnalysisMonth_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Can be used with monthly equations (blank=all months)."), 
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	*/

        JGUIUtil.addComponent(main_JPanel, new JLabel ( "Transformation:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Transformation_JComboBox = new SimpleJComboBox ( false );
	__Transformation_JComboBox.addItem ( "" );
	__Transformation_JComboBox.addItem ( __command._None );
	__Transformation_JComboBox.addItem ( __command._Log );
	__Transformation_JComboBox.select ( 0 );
	__Transformation_JComboBox.addActionListener ( this );
        JGUIUtil.addComponent(main_JPanel, __Transformation_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel(
		"How to transform data before analysis (blank=None)."), 
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	/* TODO SAM 2006-04-16
	Evaluate whether this can be supported
        JGUIUtil.addComponent(main_JPanel, new JLabel ( "Intercept:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Intercept_JTextField = new JTextField ( 5 );
	__Intercept_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __Intercept_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Blank or 0.0 are allowed with no transformation."), 
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	*/

	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Dependent analysis period:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__DependentAnalysisStart_JTextField = new JTextField ( "", 15 );
	__DependentAnalysisStart_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(main_JPanel,
		__DependentAnalysisStart_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel ( "to" ), 
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	__DependentAnalysisEnd_JTextField = new JTextField ( "", 15 );
	__DependentAnalysisEnd_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(main_JPanel, __DependentAnalysisEnd_JTextField,
		5, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Independent analysis period:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__IndependentAnalysisStart_JTextField = new JTextField ( "", 15 );
	__IndependentAnalysisStart_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(main_JPanel,
		__IndependentAnalysisStart_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel ( "to" ), 
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	__IndependentAnalysisEnd_JTextField = new JTextField ( "", 15 );
	__IndependentAnalysisEnd_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(main_JPanel, __IndependentAnalysisEnd_JTextField,
		5, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	JGUIUtil.addComponent(main_JPanel, new JLabel ( "Fill Period:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__FillStart_JTextField = new JTextField ( "", 15 );
	__FillStart_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(main_JPanel, __FillStart_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel ( "to" ), 
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	__FillEnd_JTextField = new JTextField ( "", 15 );
	__FillEnd_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(main_JPanel, __FillEnd_JTextField,
		5, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

        JGUIUtil.addComponent(main_JPanel, new JLabel ( "Fill flag:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__FillFlag_JTextField = new JTextField ( 5 );
	__FillFlag_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __FillFlag_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel(
		"1-character flag to indicate fill."), 
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

        JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea ( 7, 65 );
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Refresh the contents...
	refresh();
	checkGUIState();

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	__cancel_JButton = new SimpleJButton("Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__ok_JButton = new SimpleJButton("OK", this);
	button_JPanel.add ( __ok_JButton );

	setTitle ( "Edit " + __command.getCommandName() + "() command" );
	setResizable ( true );
        pack();
        JGUIUtil.center( this );
        super.setVisible( true );
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	int code = event.getKeyCode();

	if ( code == KeyEvent.VK_ENTER ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else {	// One of the combo boxes...
		refresh();
	}
}

public void keyReleased ( KeyEvent event )
{	refresh();
}

public void keyTyped ( KeyEvent event )
{
}

/**
Indicate if the user pressed OK (cancel otherwise).
@return true if the edits were committed, false if the user cancelled.
*/
public boolean ok ()
{	return __ok;
}

/**
Refresh the expression from the other text field contents.
The syntax is:
<pre>
fillMOVE2(TSID="X",IndependentID="X",NumberOfEquations=X,
Transformation=X,DependentAnalysisStart="X",DependentAnalysisEnd="X",
IndependentAnalysisStart="X",IndependentAnalysisEnd="X",
FillStart="X",FillEnd="X",FillFlag="X")
</pre>
Old syntax is:
<pre>
fillMOVE2(Alias,IndependentTSID,NumberOfEquations,Transformation,
DependentAnalysisStart,DependentAnalysisEnd,IndepenentAnalysisStart,
IndependentAnalysisEnd,FillStart,FillEnd)
</pre>
where the analysis and fill periods are optional to be backward compatible.
The Intercept property is also optional and can occur anywhere in the command.
Additionally, parse old-style commands and convert to new syntax.  Very old
syntax is:
<pre>
regress(TSID,TSID)
regress12(TSID,TSID)
regressMonthly(TSID,TSID)
regresslog(TSID,TSID)
regresslog12(TSID,TSID)
regressMonthlyLog(TSID,TSID)
</pre>
*/
private void refresh ()
{	String TSID = "";
	String IndependentTSID = "";
	String NumberOfEquations = "";
	/* REVISIT SAM 2006-04-16
	Evaluate whether this can be supported
	String AnalysisMonth = "";
	*/
	String Transformation = "";
	/* REVISIT SAM 2006-04-16
	Evaluate whether this can be supported
	String Intercept = "";
	*/
	String DependentAnalysisStart = "";
	String DependentAnalysisEnd = "";
	String IndependentAnalysisStart = "";
	String IndependentAnalysisEnd = "";
	String FillStart = "";
	String FillEnd = "";
	String FillFlag = "";
	PropList props = null;		// Parameters as PropList.
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		props = __command.getCommandParameters ();
		TSID = props.getValue ( "TSID" );
		IndependentTSID = props.getValue ( "IndependentTSID" );
		NumberOfEquations = props.getValue("NumberOfEquations");
		/* REVISIT SAM 2006-04-16
		Evaluate whether this can be supported
		AnalysisMonth = props.getValue("AnalysisMonth");
		*/
		Transformation = props.getValue("Transformation");
		/* REVISIT SAM 2006-04-16
		Evaluate whether this can be supported
		Intercept = props.getValue("Intercept");
		*/
		DependentAnalysisStart = props.getValue(
			"DependentAnalysisStart");
		DependentAnalysisEnd = props.getValue("DependentAnalysisEnd");
		IndependentAnalysisStart = props.getValue(
			"IndependentAnalysisStart");
		IndependentAnalysisEnd = props.getValue(
			"IndependentAnalysisEnd");
		FillStart = props.getValue("FillStart");
		FillEnd = props.getValue("FillEnd");
		FillFlag = props.getValue("FillFlag");
		// Now check the information and set in the GUI...
		if (	JGUIUtil.isSimpleJComboBoxItem(
			__TSID_JComboBox, TSID,
			JGUIUtil.NONE, null, null ) ) {
			__TSID_JComboBox.select ( TSID );
		}
		else {	/* REVISIT SAM 2005-04-27 disable since this may
			prohibit advanced users.
			Message.printWarning ( 1,
				"fillMOVE2_JDialog.refresh", "Existing " +
				"fillMOVE2() references a non-existent\n"+
				"time series \"" + alias + "\".  Select a\n" +
				"different time series or Cancel." );
			}
			*/
			if ( (TSID == null) || TSID.equals("") ) {
				// For new command... Select the first item
				// in the list...
				if ( __TSID_JComboBox.getItemCount() > 0 ) {
					__TSID_JComboBox.select ( 0 );
				}
			}
			else {	// Automatically add to the list at the top... 
				__TSID_JComboBox.insertItemAt ( TSID, 0 );
				// Select...
				__TSID_JComboBox.select ( TSID );
			}
		}
		if (	JGUIUtil.isSimpleJComboBoxItem(
			__IndependentTSID_JComboBox, IndependentTSID,
			JGUIUtil.NONE, null, null ) ) {
			__IndependentTSID_JComboBox.select ( IndependentTSID );
		}
	/* REVISIT SAM 2005-04-27 Figure out how to do with combo box
		else if ( IndependentTSID.regionMatches(
			true,0,"TEMPTS",0,6) ) {
			// The time series is a TEMPTS so look
			// for the rest of the time series in
			// the list.  If it exists, convert to
			// TEMPTS to match the command.  If not
			// add to the list as a TEMPTS to match
			// the command.
			token =	StringUtil.getToken(
				independent, " ",
				StringUtil.DELIM_SKIP_BLANKS,1);
			if ( token != null ) {
				token = token.trim();
				pos =	JGUIUtil.indexOf(
					__IndependentTSID_JComboBox,
					token,false,true);
				if ( (pos >=0) ) {
					token = "TEMPTS " + token;
					__IndependentTSID_JComboBox.
					setElementAt( token, pos );
					JGUIUtil.select (
					__IndependentTSID_JComboBox,
					token, true );
					found_ts = true;
				}
			}
			if ( !found_ts ) {
				// Probably not in the original
				// list so add to the bottom.
				// The TEMPTS is already at the
				// front of the independent TS..
				__IndependentTSID_JComboBox.addElement(
						independent);
				JGUIUtil.select (
					__IndependentTSID_JComboBox,
					independent, true );
			}
		}
	*/
		else {	/* REVISIT SAM 2005-04-27  disable and add
			Message.printWarning ( 1,
				"fillMOVE2_JDialog.refresh", "Existing " +
				"fillMOVE2() references a non-existent\n"+
				"time series \"" + independent +
				"\".  Select a\n" +
				"different time series or Cancel." );
			*/
			if (	(IndependentTSID == null) ||
				IndependentTSID.equals("") ) {
				// For new command... Select the second item
				// in the list...
				if (	__IndependentTSID_JComboBox.
					getItemCount() > 1 ) {
					__IndependentTSID_JComboBox.select (1);
				}
				// Else select the first.  This will generate a
				// warning when input is checked...
				else if(__IndependentTSID_JComboBox.
					getItemCount() > 0 ) {
					__IndependentTSID_JComboBox.select (0);
				}
			}
			else {	// Automatically add to the list at the top... 
				__IndependentTSID_JComboBox.insertItemAt (
					IndependentTSID, 0 );
				// Select...
				__IndependentTSID_JComboBox.select (
					IndependentTSID );
			}
		}
		if (	JGUIUtil.isSimpleJComboBoxItem(
			__NumberOfEquations_JComboBox, NumberOfEquations,
			JGUIUtil.NONE, null, null ) ) {
			__NumberOfEquations_JComboBox.select (
			NumberOfEquations );
		}
		else {	if (	(NumberOfEquations == null) ||
				NumberOfEquations.equals("") ) {
				// New command...select the default...
				__NumberOfEquations_JComboBox.select ( 0 );
			}
			else {	// Bad user command...
				Message.printWarning ( 1,
				"fillMOVE2_JDialog.refresh", "Existing " +
				"fillMOVE2() references an invalid\n"+
				"number of equations \"" + NumberOfEquations +
				"\".  Select a\ndifferent value or Cancel." );
			}
		}
		/* REVISIT SAM 2006-04-16
			Evaluate whether this can be supported
		if (	JGUIUtil.isSimpleJComboBoxItem(
			__AnalysisMonth_JComboBox, AnalysisMonth,
			JGUIUtil.NONE, null, null ) ) {
			__AnalysisMonth_JComboBox.select ( AnalysisMonth );
		}
		else {	if (	(AnalysisMonth == null) ||
				AnalysisMonth.equals("") ) {
				// New command...select the default...
				__AnalysisMonth_JComboBox.select ( 0 );
			}
			else {	// Bad user command...
				Message.printWarning ( 1,
				"fillMOVE2_JDialog.refresh", "Existing " +
				"fillMOVE2() references an invalid\n"+
				"analysis month \"" + AnalysisMonth +
				"\".  Select a\ndifferent value or Cancel." );
			}
		}
		*/
		if (	JGUIUtil.isSimpleJComboBoxItem(
			__Transformation_JComboBox, Transformation,
			JGUIUtil.NONE, null, null ) ) {
			__Transformation_JComboBox.select ( Transformation );
		}
		else {	if (	(Transformation == null) ||
				Transformation.equals("") ) {
				// Set default...
				__Transformation_JComboBox.select ( 0 );
			}
			else {	Message.printWarning ( 1,
				"fillMOVE2_JDialog.refresh", "Existing " +
				"fillMOVE2() references an invalid\n"+
				"transformation \"" + Transformation +
				"\".  Select a\n" +
				"different type or Cancel." );
			}
		}
		/* REVISIT SAM 2006-04-16
		Evaluate whether this can be supported
		if ( Intercept != null ) {
			__Intercept_JTextField.setText ( Intercept );
		}
		*/
		if ( DependentAnalysisStart != null ) {
			__DependentAnalysisStart_JTextField.setText (
			DependentAnalysisStart );
		}
		if ( DependentAnalysisEnd != null ) {
			__DependentAnalysisEnd_JTextField.setText (
			DependentAnalysisEnd );
		}
		if ( IndependentAnalysisStart != null ) {
			__IndependentAnalysisStart_JTextField.setText (
			IndependentAnalysisStart );
		}
		if ( IndependentAnalysisEnd != null ) {
			__IndependentAnalysisEnd_JTextField.setText (
			IndependentAnalysisEnd );
		}
		if ( FillStart != null ) {
			__FillStart_JTextField.setText( FillStart );
		}
		if ( FillEnd != null ) {
			__FillEnd_JTextField.setText ( FillEnd );
		}
		if ( FillFlag != null ) {
			__FillFlag_JTextField.setText ( FillFlag );
		}
	}
	// Regardless, reset the expression from the fields.  This is only the
	// visible information and has not yet been committed in the command.
	TSID = __TSID_JComboBox.getSelected();
	IndependentTSID = __IndependentTSID_JComboBox.getSelected();
	NumberOfEquations = __NumberOfEquations_JComboBox.getSelected();
	// REVISIT SAM 2006-04-16
	// Evaluate whether this can be supported.
	//AnalysisMonth = __AnalysisMonth_JComboBox.getSelected();
	Transformation = __Transformation_JComboBox.getSelected();
	/* REVISIT SAM 2006-04-16
	Evaluate whether this can be supported
	Intercept = __Intercept_JTextField.getText().trim();
	*/
	DependentAnalysisStart =
		__DependentAnalysisStart_JTextField.getText().trim();
	DependentAnalysisEnd =
		__DependentAnalysisEnd_JTextField.getText().trim();
	IndependentAnalysisStart =
		__IndependentAnalysisStart_JTextField.getText().trim();
	IndependentAnalysisEnd =
		__IndependentAnalysisEnd_JTextField.getText().trim();
	FillStart = __FillStart_JTextField.getText().trim();
	FillEnd = __FillEnd_JTextField.getText().trim();
	FillFlag = __FillFlag_JTextField.getText().trim();
	props = new PropList ( __command.getCommandName() );
	props.add ( "TSID=" + TSID );
	props.add ( "IndependentTSID=" + IndependentTSID );
	props.add ( "NumberOfEquations=" + NumberOfEquations );
	/* REVISIT SAM 2006-04-16
		Evaluate whether this can be supported.
	if ( __AnalysisMonth_JComboBox.isEnabled() ) {
		props.add ( "AnalysisMonth=" + AnalysisMonth );
	}
	*/
	props.add ( "Transformation=" + Transformation );
	/* REVISIT SAM 2006-04-16
	Evaluate whether this can be supported
	props.add ( "Intercept=" + Intercept );
	*/
	props.add ( "DependentAnalysisStart=" + DependentAnalysisStart );
	props.add ( "DependentAnalysisEnd=" + DependentAnalysisEnd );
	props.add ( "IndependentAnalysisStart=" + IndependentAnalysisStart );
	props.add ( "IndependentAnalysisEnd=" + IndependentAnalysisEnd );
	props.add ( "FillStart=" + FillStart );
	props.add ( "FillEnd=" + FillEnd );
	props.add ( "FillFlag=" + FillFlag );
	__command_JTextArea.setText( __command.toString ( props ) );
}

/**
React to the user response.
@param ok if false, then the edit is cancelled.  If true, the edit is committed
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
Handle ListSelectionListener events.
*/
public void valueChanged ( ListSelectionEvent e )
{	refresh ();
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

} // end fillMOVE2_JDialog
