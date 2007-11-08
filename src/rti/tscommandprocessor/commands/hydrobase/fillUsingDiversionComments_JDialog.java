// ----------------------------------------------------------------------------
// fillUsingDiversionComments_JDialog - editor for fillUsingDiversionComments()
// ----------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
// ----------------------------------------------------------------------------
// History: 
//
// 2007-01-25	Kurt Tometich, RTi		Initial Version.
// 2007-01-29	KAT, RTi		Added components needed for new CIU
//							parameters -> FillUsingCIU and FillUsingCIUFlag.
// 2007-02-26	SAM, RTi		Clean up code based on Eclipse feedback.
// 2007-03-02	SAM, RTi		Add notes to dialog for CIU.
//							Use JTextArea for command.
// ----------------------------------------------------------------------------

package rti.tscommandprocessor.commands.hydrobase;

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
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

public class fillUsingDiversionComments_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{
private String __TRUE = "True";
private String __FALSE = "False";

private fillUsingDiversionComments_Command __command = null;
private SimpleJButton	__cancel_JButton = null,// Cancel Button
						__ok_JButton = null;	// Ok Button
private JTextArea	__command_JTextArea= null;// Command as JTextArea
private SimpleJComboBox	__TSID_JComboBox = null;// Field for time series alias
private SimpleJComboBox	__RecalcLimits_JComboBox = null;
						// Field for recalculation indicator
private SimpleJComboBox __FillUsingCIU_JComboBox = null;
						// Flag for using CIU value
private JTextField	__FillStart_JTextField = null;
						// Field for fill start
private JTextField	__FillEnd_JTextField = null;

private JTextField	__FillFlag_JTextField = null;
						// Flag for data filling
private JTextField __FillUsingCIUFlag_JTextField = null;
						// CIU value

private boolean		__error_wait = false;	// Is there an error that we
						// are waiting to be cleared up
						// or Cancel?
private boolean		__first_time = true;
private boolean		__ok = false;		// Indicates whether OK button
										// has been pressed.

/**
fillUsingDiversionComments_JDialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to parse.
@param tsids Time series identifiers for time series available to fill.
*/
public fillUsingDiversionComments_JDialog (	JFrame parent, Command command )
{	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	
	Object o = event.getSource();

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
}

/**
Check the user input for errors and set __error_wait accordingly.
*/
private void checkInput ()
{	
	PropList props = new PropList ( "" );
	String TSID = __TSID_JComboBox.getSelected();
	String FillStart = __FillStart_JTextField.getText().trim();
	String FillEnd = __FillEnd_JTextField.getText().trim();
	String FillFlag = __FillFlag_JTextField.getText().trim();
	String RecalcLimits = __RecalcLimits_JComboBox.getSelected();
	String FillUsingCIU = __FillUsingCIU_JComboBox.getSelected();
	String FillUsingCIUFlag = __FillUsingCIUFlag_JTextField.getText().trim();
	
	__error_wait = false;

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
	if ( RecalcLimits.length() > 0 ) {
		props.set( "RecalcLimits", RecalcLimits );
	}
	if ( FillUsingCIU.length() > 0 ) {
		props.set( "FillUsingCIU", FillUsingCIU );
	}
	if ( FillUsingCIUFlag.length() > 0 ) {
		props.set( "FillUsingCIUFlag", FillUsingCIUFlag );
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
{	
	String TSID = __TSID_JComboBox.getSelected();
	String FillStart = __FillStart_JTextField.getText().trim();
	String FillEnd = __FillEnd_JTextField.getText().trim();
	String FillFlag = __FillFlag_JTextField.getText().trim();
	String RecalcLimits = __RecalcLimits_JComboBox.getSelected();
	String FillUsingCIU = __FillUsingCIU_JComboBox.getSelected();
	String FillUsingCIUFlag = __FillUsingCIUFlag_JTextField.getText().trim();
	
	__command.setCommandParameter ( "TSID", TSID );
	__command.setCommandParameter ( "FillStart", FillStart );
	__command.setCommandParameter ( "FillEnd", FillEnd );
	__command.setCommandParameter ( "FillFlag", FillFlag );
	__command.setCommandParameter ( "RecalcLimits", RecalcLimits );
	__command.setCommandParameter ( "FillUsingCIU", FillUsingCIU );
	__command.setCommandParameter ( "FillUsingCIUFlag", FillUsingCIUFlag );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__TSID_JComboBox = null;
	__RecalcLimits_JComboBox = null;
	__FillStart_JTextField = null;
	__FillEnd_JTextField = null;
	__FillFlag_JTextField = null;
	__FillUsingCIUFlag_JTextField = null;
	__FillUsingCIU_JComboBox = null;
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
{	__command = (fillUsingDiversionComments_Command)command;
	CommandProcessor processor = __command.getCommandProcessor();

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
	"This command can be used to fill monthly, daily, and" +
	" yearly diversions and reservoir releases for the " +
	"HydroBase input type." ), 
	0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
	"The diversion comments in HydroBase indicate years when no" +
	" water was carried for an entire irrigation year." ), 
	0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
	"Consequently, missing values in diversion time series" +
	" can be set to zero for the period November to October."),
	0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
	"If a yearly time series is filled, the zero value in an" +
	" irrigation year will be matched with the time series year."),
	0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
	"For the fill period, use standard date formats " +
	"appropriate for the date precision of the time series."),
	0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
	"The recalculate limits flag, if set to True, will cause the " +
	"average to be recalculated, for use in other fill commands (see CIU note below)."), 
	0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
	"For example, use True with a fillUsingDiversionComments() " +
	"command immediately after reading diversions."), 
	0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
    "If the \"currently in use\" (CIU) flag is used for filling, additional zeros " +
    "will be added and limits are recalculated a specific way (see documentation)."), 
    0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel (
	"Time series to fill:" ), 
	0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TSID_JComboBox = new SimpleJComboBox ( false );
    
    int size = 0;
    
    Vector tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
			(TSCommandProcessor)__command.getCommandProcessor(), __command );
    
    if ( tsids != null ) {
    	size = tsids.size();
    }
    if ( size == 0 ) {
    	tsids = new Vector();
    }
   	__TSID_JComboBox.setData ( tsids );
	// Always allow a "*" to let all time series be filled...
	__TSID_JComboBox.add ( "*" );
	__TSID_JComboBox.addItemListener ( this );
	    JGUIUtil.addComponent(main_JPanel, __TSID_JComboBox,
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel,new JLabel(
	"Fill start date:"),
	0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__FillStart_JTextField = new JTextField ( "", 10 );
	__FillStart_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __FillStart_JTextField,
	1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
	"Start of period to fill."), 
	3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel,new JLabel(
	"Fill end date:"),
	0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__FillEnd_JTextField = new JTextField ( "", 10 );
	__FillEnd_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __FillEnd_JTextField,
	1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
	"End of period to fill."), 
	3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Fill flag:" ), 
    0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __FillFlag_JTextField = new JTextField ( 5 );
    __FillFlag_JTextField.addKeyListener ( this );
       JGUIUtil.addComponent(main_JPanel, __FillFlag_JTextField,
    1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
    "1-character (or \"Auto\") flag on values to indicate fill."), 
    3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel (
	"Fill using CIU:"), 
	0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__FillUsingCIU_JComboBox = new SimpleJComboBox ( false );
	__FillUsingCIU_JComboBox.addItem ( __TRUE );
	__FillUsingCIU_JComboBox.addItem ( __FALSE );
	__FillUsingCIU_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __FillUsingCIU_JComboBox,
	1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
	"Use currently in use information."), 
	3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Fill using CIU flag:" ), 
    0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __FillUsingCIUFlag_JTextField = new JTextField ( 5 );
    __FillUsingCIUFlag_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __FillUsingCIUFlag_JTextField,
    1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
    "1-character (or \"Auto\") flag on values to indicate fill."), 
    3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel (
	"Recalculate limits:"), 
	0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__RecalcLimits_JComboBox = new SimpleJComboBox ( false );
	__RecalcLimits_JComboBox.addItem ( __TRUE );
	__RecalcLimits_JComboBox.addItem ( __FALSE );
	__RecalcLimits_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __RecalcLimits_JComboBox,
	1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
	"Recalculate original data limits after fill (CIU does automatically)?"), 
	3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
	0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea (2,60);
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

	__cancel_JButton = new SimpleJButton("Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__ok_JButton = new SimpleJButton("OK", this);
	button_JPanel.add ( __ok_JButton );

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
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
}

public void keyReleased ( KeyEvent event )
{	refresh();
}

public void keyTyped ( KeyEvent event ) {;}

/**
Indicate if the user pressed OK (cancel otherwise).
@return true if the edits were committed, false if the user cancelled.
*/
public boolean ok ()
{	return __ok;
}

/**
Refresh the command from the other text field contents.  The command is
of the form:
<pre>
fillUsingDiversionComments(TSID=xxx,FillStart=xxx,FillEnd=xxx,RecalcLimits=xxx)
</pre>
*/
private void refresh()
{	
	String routine = "fillUsingDiversionComments_JDialog.refresh";
	String TSID = "";
	String FillStart = "";
	String FillEnd = "";
	String FillFlag = "";
	String RecalcLimits = "";
	String FillUsingCIU = "";
	String FillUsingCIUFlag = "";
	
	PropList props = __command.getCommandParameters();
	try {
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		TSID = props.getValue ( "TSID" );
		RecalcLimits = props.getValue ( "RecalcLimits" );
		FillStart = props.getValue( "FillStart" );
		FillEnd = props.getValue( "FillEnd" );
		FillFlag = props.getValue( "FillFlag" );
		FillUsingCIU = props.getValue( "FillUsingCIU" );
		FillUsingCIUFlag = props.getValue( "FillUsingCIUFlag" );
		
		if (	JGUIUtil.isSimpleJComboBoxItem( __TSID_JComboBox, TSID,
				JGUIUtil.NONE, null, null ) ) {
				__TSID_JComboBox.select ( TSID );
		}
		else {	
			if ( (TSID != null) && (TSID.length() > 0) ) {
				__TSID_JComboBox.insertItemAt ( TSID, 1 );
				// Select...
				__TSID_JComboBox.select ( TSID );
			}
			else {	// Select the default (*)...
				__TSID_JComboBox.select ( 0 );
			}
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
		if ( FillUsingCIU != null && 
				JGUIUtil.isSimpleJComboBoxItem( __FillUsingCIU_JComboBox, 
				FillUsingCIU, JGUIUtil.NONE, null, null ) ) {
				__FillUsingCIU_JComboBox.select ( FillUsingCIU );
		}
		if ( FillUsingCIUFlag != null ) {
			__FillUsingCIUFlag_JTextField.setText ( FillUsingCIUFlag );
		}
		if ( RecalcLimits != null &&
				JGUIUtil.isSimpleJComboBoxItem( __RecalcLimits_JComboBox, 
				RecalcLimits, JGUIUtil.NONE, null, null ) ) {
				__RecalcLimits_JComboBox.select ( RecalcLimits );
		}
	}
	// Regardless, reset the command from the fields...
	TSID = __TSID_JComboBox.getSelected();
	FillStart = __FillStart_JTextField.getText().trim();
	FillEnd = __FillEnd_JTextField.getText().trim();
	FillFlag = __FillFlag_JTextField.getText().trim();
	FillUsingCIU = __FillUsingCIU_JComboBox.getSelected();
	FillUsingCIUFlag = __FillUsingCIUFlag_JTextField.getText().trim();
	RecalcLimits = __RecalcLimits_JComboBox.getSelected();
	props = new PropList ( __command.getCommandName() );
	props.add ( "TSID=" + TSID );
	props.add ( "FillStart=" + FillStart );
	props.add ( "FillEnd=" + FillEnd );
	props.add ( "FillFlag=" + FillFlag );
	props.add ( "FillUsingCIU=" + FillUsingCIU );
	props.add ( "FillUsingCIUFlag=" + FillUsingCIUFlag );
	props.add ( "RecalcLimits=" + RecalcLimits);
	__command_JTextArea.setText( __command.toString ( props ) );
	
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
	}

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

} // end fillUsingDiversionComments_JDialog
