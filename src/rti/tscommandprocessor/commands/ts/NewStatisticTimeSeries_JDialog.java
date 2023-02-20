// NewStatisticTimeSeries_JDialog - editor for NewStatisticTimeSeries command

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2019 Colorado Department of Natural Resources

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
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.TS.TSIdent;
import RTi.TS.TSIdent_JDialog;
import RTi.TS.TSUtil_NewStatisticTimeSeries;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

@SuppressWarnings("serial")
public class NewStatisticTimeSeries_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private JFrame __parent_JFrame = null;
private NewStatisticTimeSeries_Command __command = null;
private JTextArea __command_JTextArea=null;
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
private SimpleJComboBox __TSID_JComboBox = null;
private JTextArea __NewTSID_JTextArea = null;
private SimpleJComboBox __Statistic_JComboBox = null;
/* TODO SAM 2007-11-05 Enable later
private JTextField	__TestValue_JTextField = null;
						// Test value for the statistic.
						 */
private JTextField __AllowMissingCount_JTextField = null;
private JTextField __MinimumSampleSize_JTextField = null;
private JTextField __AnalysisStart_JTextField = null;
private JTextField __AnalysisEnd_JTextField = null;
private JTextField __OutputStart_JTextField = null;
private JTextField __OutputEnd_JTextField = null;
private SimpleJButton __edit_JButton = null;
private SimpleJButton __clear_JButton = null;
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Whether OK has been pressed.

/**
NewStatisticTimeSeries_JDialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public NewStatisticTimeSeries_JDialog ( JFrame parent, NewStatisticTimeSeries_Command command )
{	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	Object o = event.getSource();
	String routine = "NewStatisticTimeSeries_JDialog.actionPerformed";

	if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __clear_JButton ) {
		__NewTSID_JTextArea.setText ( "" );
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
			}
		}
		catch ( Exception e ) {
			Message.printWarning ( 1, routine, "Error creating time series identifier from \"" +
			NewTSID + "\"." );
			Message.printWarning ( 3, routine, e );
		}
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "NewStatisticTimeSeries");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else if ( (__Statistic_JComboBox != null) && (o == __Statistic_JComboBox) ) {
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
Check the state of the dialog, disabling/enabling components as appropriate.
*/
private void checkGUIState()
{	// TODO SAM 2005-09-08
	// Once more statistics are added, may need to disable TestValue, etc.	
}

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
	String Statistic = __Statistic_JComboBox.getSelected();
	//String TestValue = __TestValue_JTextField.getText().trim();
	String AllowMissingCount = __AllowMissingCount_JTextField.getText().trim();
	String MinimumSampleSize = __MinimumSampleSize_JTextField.getText().trim();
	String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
    String OutputStart = __OutputStart_JTextField.getText().trim();
    String OutputEnd = __OutputEnd_JTextField.getText().trim();
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
	if ( (Statistic != null) && (Statistic.length() > 0) ) {
		props.set ( "Statistic", Statistic );
	}
	//if ( (TestValue != null) && (TestValue.length() > 0) ) {
	//	props.set ( "TestValue", TestValue );
	//}
	if ( (AllowMissingCount != null) && (AllowMissingCount.length() > 0) ) {
		props.set ( "AllowMissingCount", AllowMissingCount );
	}
    if ( (MinimumSampleSize != null) && (MinimumSampleSize.length() > 0) ) {
        props.set ( "MinimumSampleSize", MinimumSampleSize );
    }
	if ( AnalysisStart.length() > 0 ) {
		props.set ( "AnalysisStart", AnalysisStart );
	}
	if ( AnalysisEnd.length() > 0 ) {
		props.set ( "AnalysisEnd", AnalysisEnd );
	}
    if ( OutputStart.length() > 0 ) {
        props.set ( "OutputStart", OutputStart );
    }
    if ( OutputEnd.length() > 0 ) {
        props.set ( "OutputEnd", OutputEnd );
    }
	//if ( SearchStart.length() > 0 ) {
	//	props.set ( "SearchStart", SearchStart );
	//}
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
{	String Alias = __Alias_JTextField.getText().trim();
	String TSID = __TSID_JComboBox.getSelected();
	String NewTSID = __NewTSID_JTextArea.getText().trim();
	String Statistic = __Statistic_JComboBox.getSelected();
	//String TestValue = __TestValue_JTextField.getText().trim();
	String AllowMissingCount = __AllowMissingCount_JTextField.getText().trim();
	String MinimumSampleSize = __MinimumSampleSize_JTextField.getText().trim();
	String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
    String OutputStart = __OutputStart_JTextField.getText().trim();
    String OutputEnd = __OutputEnd_JTextField.getText().trim();
	//String SearchStart = __SearchStart_JTextField.getText().trim();
	__command.setCommandParameter ( "Alias", Alias );
	__command.setCommandParameter ( "TSID", TSID );
	__command.setCommandParameter ( "NewTSID", NewTSID );
	__command.setCommandParameter ( "Statistic", Statistic );
	//__command.setCommandParameter ( "TestValue", TestValue );
	__command.setCommandParameter ( "AllowMissingCount", AllowMissingCount);
	__command.setCommandParameter ( "MinimumSampleSize", MinimumSampleSize);
	__command.setCommandParameter ( "AnalysisStart", AnalysisStart );
	__command.setCommandParameter ( "AnalysisEnd", AnalysisEnd );
    __command.setCommandParameter ( "OutputStart", OutputStart );
    __command.setCommandParameter ( "OutputEnd", OutputEnd );
	//__command.setCommandParameter ( "SearchStart", SearchStart );
}

/**
Instantiates the GUI components.
@param parent Frame class instantiating this class.
@param title Dialog title.
@param command Command to edit.
*/
private void initialize ( JFrame parent, NewStatisticTimeSeries_Command command )
{	__parent_JFrame = parent;
	__command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Create a time series as a repeating statistic determined from the input time series." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The statistic is computed from a sample consisting of values from " +
		"the same date/time in each year of the time series."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"For example, the Mean statistic applied to a daily time series will result in the output time " +
    	"series having the mean of all January 1 data on each January 1 in the result."),
    	0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"A new time series identifier can be assigned and is highly recommended if there is any chance that the " +
		"new time series will be mistaken for the original." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
	JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++y, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel( "Time series to analyze (TSID):"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__TSID_JComboBox = new SimpleJComboBox ( true );	// Allow edit
	List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
		(TSCommandProcessor)__command.getCommandProcessor(), __command );
	__TSID_JComboBox.setData ( tsids );
	__TSID_JComboBox.addItemListener ( this );
	__TSID_JComboBox.getJTextComponent().addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __TSID_JComboBox,
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel("Alias to assign:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Alias_JTextField = new TSFormatSpecifiersJPanel(15);
    __Alias_JTextField.addKeyListener ( this );
    __Alias_JTextField.getDocument().addDocumentListener(this);
    JGUIUtil.addComponent(main_JPanel, __Alias_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Required - use %L for location, etc."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "New time series ID:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__NewTSID_JTextArea = new JTextArea ( 3, 25 );
	__NewTSID_JTextArea.setLineWrap ( true );
	__NewTSID_JTextArea.setWrapStyleWord ( true );
	__NewTSID_JTextArea.setEditable ( false );
	__NewTSID_JTextArea.addKeyListener ( this );
	// Make 3-high to fit in the edit button...
        JGUIUtil.addComponent(main_JPanel, new JScrollPane(__NewTSID_JTextArea),
		1, y, 2, 3, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional - specify unique TSID information to define time series."), 
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	y += 2;
    JGUIUtil.addComponent(main_JPanel, (__edit_JButton = new SimpleJButton ( "Edit", "Edit", this ) ),
		3, y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        JGUIUtil.addComponent(main_JPanel, (__clear_JButton =
		new SimpleJButton ( "Clear", "Clear", this ) ),
		4, y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Statistic:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Statistic_JComboBox = new SimpleJComboBox(false);
	__Statistic_JComboBox.setData ( TSUtil_NewStatisticTimeSeries.getStatisticChoicesAsStrings() );
	__Statistic_JComboBox.select ( 0 );
	__Statistic_JComboBox.addActionListener (this);
	JGUIUtil.addComponent(main_JPanel, __Statistic_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Required - statistic to calculate."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

        /*
        JGUIUtil.addComponent(main_JPanel, new JLabel ("Test value:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__TestValue_JTextField = new JTextField (10);
	__TestValue_JTextField.addKeyListener (this);
	JGUIUtil.addComponent(main_JPanel, __TestValue_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Test value (needed for some statistics)."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        */
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Allow missing count:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__AllowMissingCount_JTextField = new JTextField (10);
	__AllowMissingCount_JTextField.addKeyListener (this);
	JGUIUtil.addComponent(main_JPanel, __AllowMissingCount_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - number of missing values allowed in sample (default=no limit)."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Minimum sample size:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MinimumSampleSize_JTextField = new JTextField (10);
    __MinimumSampleSize_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __MinimumSampleSize_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - minimum required sample size (default=determined by statistic)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

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
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Output start:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputStart_JTextField = new JTextField ( "", 20 );
    __OutputStart_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __OutputStart_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - output start date/time (default=full time series period)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Output end:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputEnd_JTextField = new JTextField ( "", 20 );
    __OutputEnd_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __OutputEnd_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - output end date/time (default=full time series period)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	/*
        JGUIUtil.addComponent(main_JPanel, new JLabel ("Search start:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__SearchStart_JTextField = new JTextField (10);
	__SearchStart_JTextField.addKeyListener (this);
	JGUIUtil.addComponent(main_JPanel, __SearchStart_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Search start (needed for some statistics)."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
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
{	checkGUIState();
    refresh();
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
			response ( true );
		}
	}
	else {
	    refresh();
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
{	String routine = "NewStatisticTimeSeries_JDialog.refresh";
	String Alias = "";
	String TSID = "";
	String NewTSID = "";
	String Statistic = "";
	//String TestValue = "";
	String AllowMissingCount = "";
	String MinimumSampleSize = "";
	String AnalysisStart = "";
	String AnalysisEnd = "";
    String OutputStart = "";
    String OutputEnd = "";
	//String SearchStart = "";
	PropList props = __command.getCommandParameters();
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		Alias = props.getValue ( "Alias" );
		TSID = props.getValue ( "TSID" );
		NewTSID = props.getValue ( "NewTSID" );
		Statistic = props.getValue ( "Statistic" );
		//TestValue = props.getValue ( "TestValue" );
		AllowMissingCount = props.getValue ( "AllowMissingCount" );
		MinimumSampleSize = props.getValue ( "MinimumSampleSize" );
		AnalysisStart = props.getValue ( "AnalysisStart" );
		AnalysisEnd = props.getValue ( "AnalysisEnd" );
        OutputStart = props.getValue ( "OutputStart" );
        OutputEnd = props.getValue ( "OutputEnd" );
		//SearchStart = props.getValue ( "SearchStart" );
		if ( Alias != null ) {
			__Alias_JTextField.setText ( Alias );
		}
		// Now select the item in the list.  If not a match, print a warning.
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
			    // Do not select anything...
			}
		}
		if ( NewTSID != null ) {
			__NewTSID_JTextArea.setText ( NewTSID );
		}
		if ( Statistic == null ) {
			// Select default...
			__Statistic_JComboBox.select ( 0 );
		}
		else {
		    if ( JGUIUtil.isSimpleJComboBoxItem( __Statistic_JComboBox, Statistic, JGUIUtil.NONE, null, null ) ) {
				__Statistic_JComboBox.select ( Statistic );
			}
			else {
			    Message.printWarning ( 1, routine, "Existing command references an invalid\nStatistic value \"" +
				Statistic + "\".  Select a different value or Cancel.");
				__error_wait = true;
			}
		}
		/*
		if ( TestValue != null ) {
			__TestValue_JTextField.setText ( TestValue );
		}
		*/
		if ( AllowMissingCount != null ) {
			__AllowMissingCount_JTextField.setText ( AllowMissingCount );
		}
        if ( MinimumSampleSize != null ) {
            __MinimumSampleSize_JTextField.setText ( MinimumSampleSize );
        }
		if ( AnalysisStart != null ) {
			__AnalysisStart_JTextField.setText( AnalysisStart );
		}
		if ( AnalysisEnd != null ) {
			__AnalysisEnd_JTextField.setText ( AnalysisEnd );
		}
        if ( OutputStart != null ) {
            __OutputStart_JTextField.setText( OutputStart );
        }
        if ( OutputEnd != null ) {
            __OutputEnd_JTextField.setText ( OutputEnd );
        }
	}
	// Regardless, reset the command from the fields...
	Alias = __Alias_JTextField.getText().trim();
	TSID = __TSID_JComboBox.getSelected();
	NewTSID = __NewTSID_JTextArea.getText().trim();
	Statistic = __Statistic_JComboBox.getSelected();
	//TestValue = __TestValue_JTextField.getText();
	AllowMissingCount = __AllowMissingCount_JTextField.getText();
	MinimumSampleSize = __MinimumSampleSize_JTextField.getText();
	AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
    OutputStart = __OutputStart_JTextField.getText().trim();
    OutputEnd = __OutputEnd_JTextField.getText().trim();
	//SearchStart = __SearchStart_JTextField.getText().trim();
	props = new PropList ( __command.getCommandName() );
	props.add ( "Alias=" + Alias );
	props.add ( "TSID=" + TSID );
	props.add ( "NewTSID=" + NewTSID );
	props.add ( "Statistic=" + Statistic );
	//props.add ( "TestValue=" + TestValue );
	props.add ( "AllowMissingCount=" + AllowMissingCount );
	props.add ( "MinimumSampleSize=" + MinimumSampleSize );
	props.add ( "AnalysisStart=" + AnalysisStart );
	props.add ( "AnalysisEnd=" + AnalysisEnd );
    props.add ( "OutputStart=" + OutputStart );
    props.add ( "OutputEnd=" + OutputEnd );
	//props.add ( "SearchStart=" + SearchStart );
	__command_JTextArea.setText( __command.toString ( props ).trim() );
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
