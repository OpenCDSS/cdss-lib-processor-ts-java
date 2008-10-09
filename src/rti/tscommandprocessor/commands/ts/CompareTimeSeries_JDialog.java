// ----------------------------------------------------------------------------
// compareTimeSeries_JDialog - editor for compareTimeSeries()
// ----------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
// ----------------------------------------------------------------------------
// History: 
//
// 2005-05-10	Steven A. Malers, RTi	Initial version (copy and modify
//					sortTimeSeries_Dialog).
// 2005-05-12	SAM, RTi		Add Precision parameter.
// 2005-05-16	SAM, RTi		Add DiffFlag parameter.
// 2005-05-18	SAM, RTi		* Add AnalysisStart, AnalysisEnd
//					  parameters.
//					* Add MatchLocation, MatchDataType
//					  parameters.
// 2005-05-19	SAM, RTi		* Move from TSTool package to TS.
// 2005-05-22	SAM, RTi		* Add WarnIfDifferent parameter.
// 2005-05-25	SAM, RTi		* The WarnIfDifferent parameter was not
//					  getting set properly for an existing
//					  command.
// 2006-05-02	SAM, RTi		* Add the WarnIfSame parameter to allow
//					  for automated testing.
// 2007-04-08	SAM, RTi		* Add CreateDiffTS=True|False parameter to
//						facilitate evaluating differences.
//						Clean up code based on Eclipse feedback.
// ----------------------------------------------------------------------------

package rti.tscommandprocessor.commands.ts;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.Command;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;

public class CompareTimeSeries_JDialog extends JDialog
implements ActionListener, KeyListener, WindowListener
{
private SimpleJButton	__cancel_JButton = null,	// Cancel Button
			__ok_JButton = null;		// Ok Button
private SimpleJComboBox	__MatchLocation_JComboBox =null;
private SimpleJComboBox	__MatchDataType_JComboBox =null;
private JTextField	__Precision_JTextField = null;	// Precision
private JTextField	__Tolerance_JTextField = null;	// Tolerance as
							// JTextField
private JTextField	__AnalysisStart_JTextField,	// Text fields for
			__AnalysisEnd_JTextField;	// dependent time series
							// analysis period.
private JTextField	__DiffFlag_JTextField = null;	// Flag to note
							// differences.
private SimpleJComboBox	__CreateDiffTS_JComboBox =null;
private SimpleJComboBox	__WarnIfDifferent_JComboBox =null;
private SimpleJComboBox	__WarnIfSame_JComboBox =null;
private JTextArea	__command_JTextArea = null;	// Command as JTextField
private boolean		__error_wait = false;
private boolean		__first_time = true;
private CompareTimeSeries_Command __command = null;	// Command to edit
private boolean		__ok = false;		// Indicates whether the user
						// has pressed OK to close the
						// dialog.
/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public CompareTimeSeries_JDialog ( JFrame parent, Command command )
{	super(parent, true);
	initialize ( parent, command );
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
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else {	// Choices...
		refresh();
	}
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
	PropList props = new PropList ( "" );
	String MatchLocation = __MatchLocation_JComboBox.getSelected();
	String MatchDataType = __MatchDataType_JComboBox.getSelected();
	String Precision = __Precision_JTextField.getText().trim();
	String Tolerance = __Tolerance_JTextField.getText().trim();
	String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	String DiffFlag = __DiffFlag_JTextField.getText().trim();
	String CreateDiffTS = __CreateDiffTS_JComboBox.getSelected();
	String WarnIfDifferent = __WarnIfDifferent_JComboBox.getSelected();
	String WarnIfSame = __WarnIfSame_JComboBox.getSelected();
	__error_wait = false;
	if ( MatchLocation.length() > 0 ) {
		props.set ( "MatchLocation", MatchLocation );
	}
	if ( MatchDataType.length() > 0 ) {
		props.set ( "MatchDataType", MatchDataType );
	}
	if ( Precision.length() > 0 ) {
		props.set ( "Precision", Precision );
	}
	if ( Tolerance.length() > 0 ) {
		props.set ( "Tolerance", Tolerance );
	}
	if ( AnalysisStart.length() > 0 ) {
		props.set ( "AnalysisStart", AnalysisStart );
	}
	if ( AnalysisEnd.length() > 0 ) {
		props.set ( "AnalysisEnd", AnalysisEnd );
	}
	if ( DiffFlag.length() > 0 ) {
		props.set ( "DiffFlag", DiffFlag );
	}
	if ( CreateDiffTS.length() > 0 ) {
		props.set ( "CreateDiffTS", CreateDiffTS );
	}
	if ( WarnIfDifferent.length() > 0 ) {
		props.set ( "WarnIfDifferent", WarnIfDifferent );
	}
	if ( WarnIfSame.length() > 0 ) {
		props.set ( "WarnIfSame", WarnIfSame );
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
{	String MatchLocation = __MatchLocation_JComboBox.getSelected();
	String MatchDataType = __MatchDataType_JComboBox.getSelected();
	String Precision = __Precision_JTextField.getText().trim();
	String Tolerance = __Tolerance_JTextField.getText().trim();
	String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	String DiffFlag = __DiffFlag_JTextField.getText().trim();
	String CreateDiffTS = __CreateDiffTS_JComboBox.getSelected();
	String WarnIfDifferent = __WarnIfDifferent_JComboBox.getSelected();
	String WarnIfSame = __WarnIfSame_JComboBox.getSelected();
	__command.setCommandParameter ( "MatchLocation", MatchLocation );
	__command.setCommandParameter ( "MatchDataType", MatchDataType );
	__command.setCommandParameter ( "Precision", Precision );
	__command.setCommandParameter ( "Tolerance", Tolerance );
	__command.setCommandParameter ( "AnalysisStart", AnalysisStart );
	__command.setCommandParameter ( "AnalysisEnd", AnalysisEnd );
	__command.setCommandParameter ( "DiffFlag", DiffFlag );
	__command.setCommandParameter ( "CreateDiffTS", CreateDiffTS );
	__command.setCommandParameter ( "WarnIfDifferent", WarnIfDifferent );
	__command.setCommandParameter ( "WarnIfSame", WarnIfSame );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__cancel_JButton = null;
	__command_JTextArea = null;
	__command = null;
	__MatchLocation_JComboBox = null;
	__MatchDataType_JComboBox = null;
	__Precision_JTextField = null;
	__Tolerance_JTextField = null;
	__AnalysisStart_JTextField = null;
	__AnalysisEnd_JTextField = null;
	__DiffFlag_JTextField = null;
	__CreateDiffTS_JComboBox = null;
	__WarnIfDifferent_JComboBox = null;
	__WarnIfSame_JComboBox = null;
	__ok_JButton = null;
	super.finalize ();
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, Command command )
{	__command = (CompareTimeSeries_Command)command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"This command compares time series.  Currently all available time series are evaluated," ),
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"comparing time series that have the same time series identifier location and/or data type." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specify one or more tolerances, separated by commas.  " +
		"Differences greater than these values will be noted." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Match location:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__MatchLocation_JComboBox = new SimpleJComboBox ( false );
	__MatchLocation_JComboBox.addItem ( "" );	// Default
	__MatchLocation_JComboBox.addItem ( __command._False );
	__MatchLocation_JComboBox.addItem ( __command._True );
	__MatchLocation_JComboBox.select ( 0 );
	__MatchLocation_JComboBox.addActionListener ( this );
        JGUIUtil.addComponent(main_JPanel, __MatchLocation_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Match location to find time series pair? (default=true)"), 
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

        JGUIUtil.addComponent(main_JPanel, new JLabel ( "Match data type:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__MatchDataType_JComboBox = new SimpleJComboBox ( false );
	__MatchDataType_JComboBox.addItem ( "" );	// Default
	__MatchDataType_JComboBox.addItem ( __command._False );
	__MatchDataType_JComboBox.addItem ( __command._True );
	__MatchDataType_JComboBox.select ( 0 );
	__MatchDataType_JComboBox.addActionListener ( this );
        JGUIUtil.addComponent(main_JPanel, __MatchDataType_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Match data type to find time series pair? (default=false)"), 
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

        JGUIUtil.addComponent(main_JPanel, new JLabel ( "Precision:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Precision_JTextField = new JTextField ( 5 );
	__Precision_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __Precision_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Precision for comparison (digits after decimal)."), 
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

        JGUIUtil.addComponent(main_JPanel, new JLabel ( "Tolerance:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Tolerance_JTextField = new JTextField ( 15 );
	__Tolerance_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __Tolerance_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Tolerance(s) to indicate difference (e.g., .01, .1)."), 
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

        JGUIUtil.addComponent(main_JPanel, new JLabel ( "Difference flag:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__DiffFlag_JTextField = new JTextField ( 15 );
	__DiffFlag_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __DiffFlag_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel(
		"1-character flag to use for values that are different."),
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

   JGUIUtil.addComponent(main_JPanel, new JLabel ( "Create difference time series?:"),
    		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
      	__CreateDiffTS_JComboBox = new SimpleJComboBox ( false );
      	__CreateDiffTS_JComboBox.addItem ( "" );	// Default
       	__CreateDiffTS_JComboBox.addItem ( __command._False );
       	__CreateDiffTS_JComboBox.addItem ( __command._True );
       	__CreateDiffTS_JComboBox.select ( 0 );
       	__CreateDiffTS_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __CreateDiffTS_JComboBox,
       		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
               JGUIUtil.addComponent(main_JPanel, new JLabel(
       		"Create a time series TS1 - TS2? (default=false)"), 
       		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);        
        
        JGUIUtil.addComponent(main_JPanel, new JLabel ( "Warn if different?:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__WarnIfDifferent_JComboBox = new SimpleJComboBox ( false );
	__WarnIfDifferent_JComboBox.addItem ( "" );	// Default
	__WarnIfDifferent_JComboBox.addItem ( __command._False );
	__WarnIfDifferent_JComboBox.addItem ( __command._True );
	__WarnIfDifferent_JComboBox.select ( 0 );
	__WarnIfDifferent_JComboBox.addActionListener ( this );
        JGUIUtil.addComponent(main_JPanel, __WarnIfDifferent_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Generate a warning if different? (default=false)"), 
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

        JGUIUtil.addComponent(main_JPanel, new JLabel ( "Warn if same?:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__WarnIfSame_JComboBox = new SimpleJComboBox ( false );
	__WarnIfSame_JComboBox.addItem ( "" );	// Default
	__WarnIfSame_JComboBox.addItem ( __command._False );
	__WarnIfSame_JComboBox.addItem ( __command._True );
	__WarnIfSame_JComboBox.select ( 0 );
	__WarnIfSame_JComboBox.addActionListener ( this );
        JGUIUtil.addComponent(main_JPanel, __WarnIfSame_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Generate a warning if same? (default=false)"), 
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

        JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea ( 4, 60 );
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.addKeyListener ( this );
	__command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Refresh the contents...
	refresh ();

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	button_JPanel.add(__cancel_JButton = new SimpleJButton("Cancel", this));
	button_JPanel.add ( __ok_JButton = new SimpleJButton("OK", this) );

	setTitle ( "Edit " + __command.getCommandName() + "() command" );

	// Dialogs do not need to be resizable...
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
	}
}

public void keyReleased ( KeyEvent event )
{	refresh();
}

public void keyTyped ( KeyEvent event ) {;}

/**
Indicate if the user pressed OK (cancel otherwise).
*/
public boolean ok ()
{	return __ok;
}

/**
Refresh the command from the other text field contents.  The command is
of the form:
<pre>
compareTimeSeries(MatchLocation=X,MatchDataType=X,Precision=X,
Tolerance="X,X,...",AnalysisStart="X",AnalysisEnd="X",DiffFlag="X",
WarnIfDifferent=X,WarnIfSame=X)
</pre>
*/
private void refresh ()
{	String routine = "compareTimeSeries_JDialog.refresh";
	String MatchLocation = "";
	String MatchDataType = "";
	String Precision = "";
	String Tolerance = "";
	String AnalysisStart = "";
	String AnalysisEnd = "";
	String DiffFlag = "";
	String CreateDiffTS = "";
	String WarnIfDifferent = "";
	String WarnIfSame = "";
	if ( __first_time ) {
		__first_time = false;
		Vector v = StringUtil.breakStringList (
			__command.toString(),"()",
			StringUtil.DELIM_SKIP_BLANKS );
		PropList props = null;
		if (	(v != null) && (v.size() > 1) &&
			(((String)v.elementAt(1)).indexOf("=") > 0) ) {
			props = PropList.parse (
				(String)v.elementAt(1), routine, "," );
		}
		if ( props == null ) {
			props = new PropList ( __command.getCommandName() );
		}
		MatchLocation = props.getValue ( "MatchLocation" );
		MatchDataType = props.getValue ( "MatchDataType" );
		Precision = props.getValue ( "Precision" );
		Tolerance = props.getValue ( "Tolerance" );
		AnalysisStart = props.getValue("AnalysisStart");
		AnalysisEnd = props.getValue("AnalysisEnd");
		DiffFlag = props.getValue ( "DiffFlag" );
		CreateDiffTS = props.getValue ( "CreateDiffTS" );
		WarnIfDifferent = props.getValue ( "WarnIfDifferent" );
		WarnIfSame = props.getValue ( "WarnIfSame" );
		if (	JGUIUtil.isSimpleJComboBoxItem(
			__MatchLocation_JComboBox, MatchLocation,
			JGUIUtil.NONE, null, null ) ) {
			__MatchLocation_JComboBox.select ( MatchLocation );
		}
		else {	if (	(MatchLocation == null) ||
				MatchLocation.equals("") ) {
				// New command...select the default...
				__MatchLocation_JComboBox.select ( 0 );
			}
			else {	// Bad user command...
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"MatchLocation parameter \"" + MatchLocation +
				"\".  Select a\ndifferent value or Cancel." );
			}
		}
		if (	JGUIUtil.isSimpleJComboBoxItem(
			__MatchDataType_JComboBox, MatchDataType,
			JGUIUtil.NONE, null, null ) ) {
			__MatchDataType_JComboBox.select ( MatchDataType );
		}
		else {	if (	(MatchDataType == null) ||
				MatchDataType.equals("") ) {
				// New command...select the default...
				__MatchDataType_JComboBox.select ( 0 );
			}
			else {	// Bad user command...
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"MatchDataType parameter \"" + MatchDataType +
				"\".  Select a\ndifferent value or Cancel." );
			}
		}
		if ( Precision != null ) {
			__Precision_JTextField.setText ( Precision );
		}
		if ( Tolerance != null ) {
			__Tolerance_JTextField.setText ( Tolerance );
		}
		if ( AnalysisStart != null ) {
			__AnalysisStart_JTextField.setText ( AnalysisStart );
		}
		if ( AnalysisEnd != null ) {
			__AnalysisEnd_JTextField.setText ( AnalysisEnd );
		}
		if ( DiffFlag != null ) {
			__DiffFlag_JTextField.setText ( DiffFlag );
		}
		if (	JGUIUtil.isSimpleJComboBoxItem(
				__CreateDiffTS_JComboBox, CreateDiffTS,
				JGUIUtil.NONE, null, null ) ) {
				__CreateDiffTS_JComboBox.select ( CreateDiffTS );
		}
		else {	if (	(CreateDiffTS == null) ||
				CreateDiffTS.equals("") ) {
				// New command...select the default...
				__CreateDiffTS_JComboBox.select ( 0 );
			}
			else {	// Bad user command...
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"CreateDiffTS parameter \"" +
				CreateDiffTS +
				"\".  Select a\ndifferent value or Cancel." );
			}
		}
		if (	JGUIUtil.isSimpleJComboBoxItem(
			__WarnIfDifferent_JComboBox, WarnIfDifferent,
			JGUIUtil.NONE, null, null ) ) {
			__WarnIfDifferent_JComboBox.select ( WarnIfDifferent );
		}
		else {	if (	(WarnIfDifferent == null) ||
				WarnIfDifferent.equals("") ) {
				// New command...select the default...
				__WarnIfDifferent_JComboBox.select ( 0 );
			}
			else {	// Bad user command...
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"WarnIfDifferent parameter \"" +
				WarnIfDifferent +
				"\".  Select a\ndifferent value or Cancel." );
			}
		}
		if (	JGUIUtil.isSimpleJComboBoxItem(
			__WarnIfSame_JComboBox, WarnIfSame,
			JGUIUtil.NONE, null, null ) ) {
			__WarnIfSame_JComboBox.select ( WarnIfSame );
		}
		else {	if (	(WarnIfSame == null) ||
				WarnIfSame.equals("") ) {
				// New command...select the default...
				__WarnIfSame_JComboBox.select ( 0 );
			}
			else {	// Bad user command...
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"WarnIfSame parameter \"" +
				WarnIfSame +
				"\".  Select a\ndifferent value or Cancel." );
			}
		}
	}
	// Regardless, reset the command from the fields.  This is only  visible
	// information that has not been committed in the command.
	MatchLocation = __MatchLocation_JComboBox.getSelected();
	MatchDataType = __MatchDataType_JComboBox.getSelected();
	Precision = __Precision_JTextField.getText().trim();
	Tolerance = __Tolerance_JTextField.getText().trim();
	AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	DiffFlag = __DiffFlag_JTextField.getText().trim();
	CreateDiffTS = __CreateDiffTS_JComboBox.getSelected();
	WarnIfDifferent = __WarnIfDifferent_JComboBox.getSelected();
	WarnIfSame = __WarnIfSame_JComboBox.getSelected();
	PropList props = new PropList ( __command.getCommandName() );
	props.add ( "MatchLocation=" + MatchLocation );
	props.add ( "MatchDataType=" + MatchDataType );
	props.add ( "Precision=" + Precision );
	props.add ( "Tolerance=" + Tolerance );
	props.add ( "AnalysisStart=" + AnalysisStart );
	props.add ( "AnalysisEnd=" + AnalysisEnd );
	props.add ( "DiffFlag=" + DiffFlag );
	props.add ( "CreateDiffTS=" + CreateDiffTS );
	props.add ( "WarnIfDifferent=" + WarnIfDifferent );
	props.add ( "WarnIfSame=" + WarnIfSame );
	__command_JTextArea.setText( __command.toString(props) );
}

/**
React to the user response.
@param ok if false, then the edit is cancelled.  If true, the edit is committed
and the dialog is closed.
*/
public void response ( boolean ok )
{	__ok = ok;
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

} // end compareTimeSeries_JDialog
