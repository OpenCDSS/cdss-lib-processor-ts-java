// ----------------------------------------------------------------------------
// fillHistYearAverage_JDialog - editor for fillHistYearAverage()
// ----------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
// ----------------------------------------------------------------------------
// History: 
//
// 2005-05-18	Steven A. Malers, RTi	Discard old version and re-initialize
//					using fillHistMonthAverage_JDialog as a
//					starting point.
// 2005-05-19	SAM, RTi		Move from TSTool package to TS.
// 2005-06-02	SAM, RTi		Fix bug where extra blank was being
//					added to the TSID choice.
// 2007-02-16	SAM, RTi				Use new CommandProcessor interface.
//					Clean up code based on Eclipse feedback.
// 2007-05-08	SAM, RTi		Cleanup code based on Eclipse feedback.
// ----------------------------------------------------------------------------

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
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.Command;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

public class fillHistYearAverage_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton	__cancel_JButton = null,// Cancel Button
			__ok_JButton = null;	// Ok Button
private fillHistYearAverage_Command __command = null;
						// Command to edit
private JTextArea	__command_JTextArea=null;// Command as JTextArea
private SimpleJComboBox	__TSList_JComboBox = null;
						// Indicate how to get time
						// series list.
private SimpleJComboBox	__TSID_JComboBox = null;// Field for time series ID
private JTextField	__FillStart_JTextField, // Text fields for fill period.
			__FillEnd_JTextField,
			__FillFlag_JTextField;	// Flag to set for filled data.
private boolean		__first_time = true;
private boolean		__error_wait = false;
private boolean		__ok = false;

/**
fillHistYearAverage_JDialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public fillHistYearAverage_JDialog ( JFrame parent,
					Command command )
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
	else if ( (__TSList_JComboBox != null) && (o == __TSList_JComboBox) ) {
		checkGUIState();
		refresh ();
	}
}

/**
Check the state of the dialog, disabling/enabling components as appropriate.
*/
private void checkGUIState()
{	// If "AllMatchingTSID", enable the list.
	// Otherwise, clear and disable...
	String TSList = __TSList_JComboBox.getSelected();
    if ( TSListType.ALL_MATCHING_TSID.equals(TSList) ||
            TSListType.FIRST_MATCHING_TSID.equals(TSList) ||
            TSListType.LAST_MATCHING_TSID.equals(TSList) ) {
		__TSID_JComboBox.setEnabled(true);
	}
	else {	__TSID_JComboBox.setEnabled(false);
		// Set the the first choice, which is blank...
		__TSID_JComboBox.select ( 0 );
	}
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
	PropList props = new PropList ( "" );
	String TSList = __TSList_JComboBox.getSelected();
	String TSID = __TSID_JComboBox.getSelected();
	String FillStart = __FillStart_JTextField.getText().trim();
	String FillEnd = __FillEnd_JTextField.getText().trim();
	String FillFlag = __FillFlag_JTextField.getText().trim();
	__error_wait = false;

	if ( TSList.length() > 0 ) {
		props.set ( "TSList", TSList );
	}
	if ( TSID.length() > 0 ) {
		props.set ( "TSID", TSID );
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
{	String TSList = __TSList_JComboBox.getSelected();
	String TSID = __TSID_JComboBox.getSelected();
	String FillStart = __FillStart_JTextField.getText().trim();
	String FillEnd = __FillEnd_JTextField.getText().trim();
	String FillFlag = __FillFlag_JTextField.getText().trim();
	__command.setCommandParameter ( "TSList", TSList );
	__command.setCommandParameter ( "TSID", TSID );
	__command.setCommandParameter ( "FillStart", FillStart );
	__command.setCommandParameter ( "FillEnd", FillEnd );
	__command.setCommandParameter ( "FillFlag", FillFlag );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__TSList_JComboBox = null;
	__TSID_JComboBox = null;
	__FillStart_JTextField = null;
	__FillEnd_JTextField = null;
	__FillFlag_JTextField = null;
	__cancel_JButton = null;
	__command_JTextArea = null;
	__ok_JButton = null;
	super.finalize ();
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, Command command )
{	__command = (fillHistYearAverage_Command)command;

	addWindowListener( this );

        Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Fill yearly time series with historical yearly averages." ), 
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Historical averages are computed immediately after reading " +
		"the data and therefore do not consider filled values." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Only yearly time series can be processed." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
       	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The time series to process are indicated using the TS list."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
       	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"If TS list is \"" + __command._AllMatchingTSID + "\", "+
		"pick a single time series, " +
		"or enter a wildcard time series identifier pattern."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

        JGUIUtil.addComponent(main_JPanel, new JLabel ("TS list:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	Vector tslist_Vector = new Vector();
	tslist_Vector.addElement ( __command._AllTS );
	tslist_Vector.addElement ( __command._AllMatchingTSID );
	tslist_Vector.addElement ( __command._SelectedTS );
	__TSList_JComboBox = new SimpleJComboBox(false);
	__TSList_JComboBox.setData ( tslist_Vector );
	__TSList_JComboBox.select ( 0 );
	__TSList_JComboBox.addActionListener (this);
	JGUIUtil.addComponent(main_JPanel, __TSList_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"How to get the time series to fill."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Identifier (TSID) to match:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);

	// Allow edits...
	__TSID_JComboBox = new SimpleJComboBox ( true );
	
	Vector tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
			(TSCommandProcessor)__command.getCommandProcessor(), __command );
	
	if ( tsids == null ) {
		tsids = new Vector ();
	}
	int size = tsids.size();
	// Blank for default
	if ( size > 0 ) {
		tsids.insertElementAt ( "", 0 );
	}
	else {	tsids.addElement ( "" );
	}
	// Always allow a "*" to let all time series be filled (put at end)...
	tsids.addElement ( "*" );
	__TSID_JComboBox.setData ( tsids );
	__TSID_JComboBox.addItemListener ( this );
	__TSID_JComboBox.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __TSID_JComboBox,
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	JGUIUtil.addComponent(main_JPanel, new JLabel ( "Fill period:" ), 
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
	__command_JTextArea = new JTextArea ( 4, 50 );
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
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

	button_JPanel.add ( __cancel_JButton=new SimpleJButton("Cancel", this));
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
{	checkGUIState();
    refresh();
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
Refresh the command from the other text field contents.
*/
private void refresh ()
{	String routine = "fillHistYearAverage_JDialog.refresh";
	String TSList = "";
	String TSID = "";
	String FillStart = "";
	String FillEnd = "";
	String FillFlag = "";
	PropList props = __command.getCommandParameters();
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		TSList = props.getValue ( "TSList" );
		TSID = props.getValue ( "TSID" );
		FillStart = props.getValue("FillStart");
		FillEnd = props.getValue("FillEnd");
		FillFlag = props.getValue("FillFlag");
		if ( TSList == null ) {
			// Select default...
			__TSList_JComboBox.select ( 0 );
		}
		else {	if (	JGUIUtil.isSimpleJComboBoxItem(
				__TSList_JComboBox,
				TSList, JGUIUtil.NONE, null, null ) ) {
				__TSList_JComboBox.select ( TSList );
			}
			else {	Message.printWarning ( 1, routine,
				"Existing command " +
				"references an invalid\nTSList value \"" +
				TSList +
				"\".  Select a different value or Cancel.");
				__error_wait = true;
			}
		}
		if (	JGUIUtil.isSimpleJComboBoxItem( __TSID_JComboBox, TSID,
				JGUIUtil.NONE, null, null ) ) {
				__TSID_JComboBox.select ( TSID );
		}
		else {	// Automatically add to the list after the blank...
			if ( (TSID != null) && (TSID.length() > 0) ) {
				__TSID_JComboBox.insertItemAt ( TSID, 1 );
				// Select...
				__TSID_JComboBox.select ( TSID );
			}
			else {	// Select the blank...
				__TSID_JComboBox.select ( 0 );
			}
		}
		// Check the GUI state to make sure that components are
		// enabled as expected (mainly enable/disable the TSID).  If
		// disabled, the TSID will not be added as a parameter below.
		checkGUIState();
		if ( !__TSID_JComboBox.isEnabled() ) {
			// Not needed because some other method of specifying
			// the time series is being used...
			TSID = null;
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
	// Regardless, reset the command from the fields...
	TSList = __TSList_JComboBox.getSelected();
	TSID = __TSID_JComboBox.getSelected();
	FillStart = __FillStart_JTextField.getText().trim();
	FillEnd = __FillEnd_JTextField.getText().trim();
	FillFlag = __FillFlag_JTextField.getText().trim();
	props = new PropList ( __command.getCommandName() );
	props.add ( "TSList=" + TSList );
	props.add ( "TSID=" + TSID );
	props.add ( "FillStart=" + FillStart );
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

} // end fillHistYearAverage_JDialog
