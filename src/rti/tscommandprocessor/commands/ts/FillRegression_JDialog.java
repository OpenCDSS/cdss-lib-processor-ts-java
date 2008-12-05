// ----------------------------------------------------------------------------
// fillRegression_JDialog - editor for fillRegression()
// ----------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
// ----------------------------------------------------------------------------
// History: 
//
// 12 Dec 2000	Steven A. Malers, RTi	Initial version.
// 25 Feb 2001	SAM, RTi		Add TEMPTS capability.  To do this with
//					a popup had to change the independent
//					time series from a choice to a single
//					selection list.
// 2002-04-17	SAM, RTi		Update to have analysis and fill period
//					to be consistent with MOVE2.  Clean up
//					appearance consistent with other
//					dialogs.
// 2003-05-12	SAM, RTi		Enable the Intercept property.
// 2003-12-11	SAM, RTi		Update to Swing.
// 2004-02-22	SAM, RTi		Change the independent list to single
//					selection mode.
// 2005-04-27	SAM, RTi		* Update to free-format notation.
//					* Remove the popup menu in favor of a
//					  button.
//					* Change the independent time series
//					  list to a combo box.
//					* Allow the combo box to be empty and
//					  make the text editable.
//					* Add the AnalysisMonth parameter.
// 2005-05-05	SAM, RTi		* Change to use new Command object.
// 2005-05-12	SAM, RTi		* Add the FillFlag parameter.
// 2005-05-19	SAM, RTi		Move from TSTool package to TS.
// 2005-08-05	SAM, RTi		Only add AnalysisMonth in toString() if
//					the combobox is enabled.
// 2007-02-16	SAM, RTi		Update to use new CommandProcessor interface.
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

import java.util.List;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.IO.Command;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

public class FillRegression_JDialog extends JDialog
implements ActionListener, KeyListener, ListSelectionListener, WindowListener
{

private SimpleJButton	__cancel_JButton = null,// Cancel Button
			__ok_JButton = null;	// Ok Button
private JTextArea	__command_JTextArea=null; // Command as JTextArea
private JTextField	__Intercept_JTextField = null; // Intercept value as JTextField
private JTextField	__AnalysisStart_JTextField,
			__AnalysisEnd_JTextField, // Text fields for dependent time series analysis period.
			__FillStart_JTextField, // Text fields for fill period.
			__FillEnd_JTextField,
			__FillFlag_JTextField;	// Flag to set for filled data.
private SimpleJComboBox	__TSID_JComboBox = null;// Field for time series ID
private SimpleJComboBox	__IndependentTSID_JComboBox= null; // List for independent time series identifier(s)
private SimpleJComboBox	__NumberOfEquations_JComboBox = null; // One or monthly equations.
private SimpleJComboBox	__AnalysisMonth_JComboBox = null; // Month to analyze - monthly equations only.
private SimpleJComboBox	__Transformation_JComboBox=null;// None or log transformation.
private boolean		__error_wait = false;	// True if there is an error in the input.
private boolean		__first_time = true;
private boolean		__ok = false;		// Was OK pressed last (false=cancel)?
private FillRegression_Command __command = null;// Command to edit

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public FillRegression_JDialog ( JFrame parent, Command command )
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
	else {
	    // A combo box.  Refresh the command...
		checkGUIState();
		refresh ();
	}
}

/**
Check the GUI state and make sure the proper components are enabled/disabled.
*/
private void checkGUIState()
{	String NumberOfEquations = __NumberOfEquations_JComboBox.getSelected();
	if ( NumberOfEquations.equalsIgnoreCase(__command._MonthlyEquations) ) {
		JGUIUtil.setEnabled(__AnalysisMonth_JComboBox,true);
	}
	else {
        JGUIUtil.setEnabled(__AnalysisMonth_JComboBox,false);
	}

	String Transformation = __Transformation_JComboBox.getSelected();
	if ( Transformation.equalsIgnoreCase(__command._None) || Transformation.equals("") ) {
		JGUIUtil.setEnabled(__Intercept_JTextField,true);
	}
	else {
        JGUIUtil.setEnabled(__Intercept_JTextField,false);
	}
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
	String Intercept = __Intercept_JTextField.getText().trim();
	String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
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
	if ( Intercept.length() > 0 ) {
		props.set ( "Intercept", Intercept );
	}
	if ( AnalysisStart.length() > 0 ) {
		props.set ( "AnalysisStart", AnalysisStart );
	}
	if ( AnalysisEnd.length() > 0 ) {
		props.set ( "AnalysisEnd", AnalysisEnd );
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
	String AnalysisMonth = __AnalysisMonth_JComboBox.getSelected();
	String Transformation = __Transformation_JComboBox.getSelected();
	String Intercept = __Intercept_JTextField.getText().trim();
	String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	String FillStart = __FillStart_JTextField.getText().trim();
	String FillEnd = __FillEnd_JTextField.getText().trim();
	String FillFlag = __FillFlag_JTextField.getText().trim();
	__command.setCommandParameter ( "TSID", TSID );
	__command.setCommandParameter ( "IndependentTSID", IndependentTSID );
	__command.setCommandParameter ( "NumberOfEquations", NumberOfEquations);
	__command.setCommandParameter ( "AnalysisMonth", AnalysisMonth );
	__command.setCommandParameter ( "Transformation", Transformation );
	__command.setCommandParameter ( "Intercept", Intercept );
	__command.setCommandParameter ( "AnalysisStart", AnalysisStart );
	__command.setCommandParameter ( "AnalysisEnd", AnalysisEnd );
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
{	__command = (FillRegression_Command)command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Fill missing data using ordinary least squares (OLS) regression."),
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The analysis period will be used to determine relationships used for filling." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Use a setOutputPeriod() command before reading to extend " +
		"the dependent time series, if necessary." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specify dates with precision appropriate for the data, " +
		"use blank for all available data, OutputStart, or OutputEnd."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Time series to fill (dependent):" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__TSID_JComboBox = new SimpleJComboBox ( true );

	// Get the time series identifiers from the processor...
	
	List tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
			(TSCommandProcessor)__command.getCommandProcessor(), __command );
	
	__TSID_JComboBox.setData ( tsids );
	__TSID_JComboBox.addKeyListener ( this );
	__TSID_JComboBox.addActionListener ( this );
        JGUIUtil.addComponent(main_JPanel, __TSID_JComboBox,
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Independent time series:"),
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

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Intercept:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Intercept_JTextField = new JTextField ( 5 );
	__Intercept_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Intercept_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Blank or 0.0 are allowed with no transformation."), 
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	JGUIUtil.addComponent(main_JPanel, new JLabel ( "Analysis period:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__AnalysisStart_JTextField = new JTextField ( "", 15 );
	__AnalysisStart_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(main_JPanel,
		__AnalysisStart_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel ( "to" ), 
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	__AnalysisEnd_JTextField = new JTextField ( "", 15 );
	__AnalysisEnd_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(main_JPanel, __AnalysisEnd_JTextField,
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
	__command_JTextArea = new JTextArea ( 5, 65 );
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
	else {
	    // One of the combo boxes...
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
Refresh the expression from the other text field contents.  The syntax is:
<pre>
fillRegression(TSID="X",IndependentID="X",NumberOfEquations=X,AnalysisMonth=X,
Transformation=X,AnalysisStart="X",AnalysisEnd="X",FillStart="X",FillEnd="X",
Intercept=0,FillFlag="X")
</pre>
Old syntax is:
<pre>
fillRegression(Alias,IndependentID,NumberOfEquations,Transformation,
AnalysisStart,AnalysisEnd,FillStart,FillEnd,Intercept=0)
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
	String AnalysisMonth = "";
	String Transformation = "";
	String Intercept = "";
	String AnalysisStart = "";
	String AnalysisEnd = "";
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
		AnalysisMonth = props.getValue("AnalysisMonth");
		Transformation = props.getValue("Transformation");
		Intercept = props.getValue("Intercept");
		AnalysisStart = props.getValue("AnalysisStart");
		AnalysisEnd = props.getValue("AnalysisEnd");
		FillStart = props.getValue("FillStart");
		FillEnd = props.getValue("FillEnd");
		FillFlag = props.getValue("FillFlag");
		// Now check the information and set in the GUI...
		if ( JGUIUtil.isSimpleJComboBoxItem( __TSID_JComboBox, TSID, JGUIUtil.NONE, null, null ) ) {
			__TSID_JComboBox.select ( TSID );
		}
		else {
            /* TODO SAM 2005-04-27 disable since this may
			prohibit advanced users.
			Message.printWarning ( 1,
				"fillRegression_JDialog.refresh", "Existing " +
				"fillRegression() references a non-existent\n"+
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
	/* TODO SAM 2005-04-27 Figure out how to do with combo box
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
		else {
            /* TODO SAM 2005-04-27  disable and add
			Message.printWarning ( 1,
				"fillRegression_JDialog.refresh", "Existing " +
				"fillRegression() references a non-existent\n"+
				"time series \"" + independent +
				"\".  Select a\n" +
				"different time series or Cancel." );
			*/
			if ( (IndependentTSID == null) || IndependentTSID.equals("") ) {
				// For new command... Select the second item in the list...
				if ( __IndependentTSID_JComboBox.getItemCount() > 1 ) {
					__IndependentTSID_JComboBox.select (1);
				}
				// Else select the first.  This will generate a warning when input is checked...
				else if(__IndependentTSID_JComboBox.getItemCount() > 0 ) {
					__IndependentTSID_JComboBox.select (0);
				}
			}
			else {	// Automatically add to the list at the top... 
				__IndependentTSID_JComboBox.insertItemAt ( IndependentTSID, 0 );
				// Select...
				__IndependentTSID_JComboBox.select ( IndependentTSID );
			}
		}
		if ( JGUIUtil.isSimpleJComboBoxItem( __NumberOfEquations_JComboBox, NumberOfEquations, JGUIUtil.NONE, null, null ) ) {
			__NumberOfEquations_JComboBox.select ( NumberOfEquations );
		}
		else {
            if ( (NumberOfEquations == null) ||	NumberOfEquations.equals("") ) {
				// New command...select the default...
				__NumberOfEquations_JComboBox.select ( 0 );
			}
			else {
                // Bad user command...
				Message.printWarning ( 1,
				"fillRegression_JDialog.refresh", "Existing " +
				"fillRegression() references an invalid\n"+
				"number of equations \"" + NumberOfEquations +
				"\".  Select a\ndifferent value or Cancel." );
			}
		}
		if ( JGUIUtil.isSimpleJComboBoxItem( __AnalysisMonth_JComboBox, AnalysisMonth, JGUIUtil.NONE, null, null ) ) {
			__AnalysisMonth_JComboBox.select ( AnalysisMonth );
		}
		else {
            if ( (AnalysisMonth == null) ||	AnalysisMonth.equals("") ) {
				// New command...select the default...
				__AnalysisMonth_JComboBox.select ( 0 );
			}
			else {	// Bad user command...
				Message.printWarning ( 1,
				"fillRegression_JDialog.refresh", "Existing " +
				"fillRegression() references an invalid\n"+
				"analysis month \"" + AnalysisMonth +
				"\".  Select a\ndifferent value or Cancel." );
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
			else {	Message.printWarning ( 1,
				"fillRegression_JDialog.refresh", "Existing " +
				"fillRegression() references an invalid\n"+
				"transformation \"" + Transformation +
				"\".  Select a\n" +
				"different type or Cancel." );
			}
		}
		if ( Intercept != null ) {
			__Intercept_JTextField.setText ( Intercept );
		}
		if ( AnalysisStart != null ) {
			__AnalysisStart_JTextField.setText ( AnalysisStart );
		}
		if ( AnalysisEnd != null ) {
			__AnalysisEnd_JTextField.setText ( AnalysisEnd );
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
	AnalysisMonth = __AnalysisMonth_JComboBox.getSelected();
	Transformation = __Transformation_JComboBox.getSelected();
	Intercept = __Intercept_JTextField.getText().trim();
	AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	FillStart = __FillStart_JTextField.getText().trim();
	FillEnd = __FillEnd_JTextField.getText().trim();
	FillFlag = __FillFlag_JTextField.getText().trim();
	props = new PropList ( __command.getCommandName() );
	props.add ( "TSID=" + TSID );
	props.add ( "IndependentTSID=" + IndependentTSID );
	props.add ( "NumberOfEquations=" + NumberOfEquations );
	if ( __AnalysisMonth_JComboBox.isEnabled() ) {
		props.add ( "AnalysisMonth=" + AnalysisMonth );
	}
	props.add ( "Transformation=" + Transformation );
	props.add ( "Intercept=" + Intercept );
	props.add ( "AnalysisStart=" + AnalysisStart );
	props.add ( "AnalysisEnd=" + AnalysisEnd );
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

} // end fillRegression_JDialog
