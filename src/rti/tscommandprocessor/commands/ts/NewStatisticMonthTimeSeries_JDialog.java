// NewStatisticMonthTimeSeries_JDialog - editor for NewStatisticMonthTimeSeries command

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2022 Colorado Department of Natural Resources

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
import java.util.Vector;

import javax.swing.JCheckBox;
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
import RTi.TS.TSUtil_NewStatisticMonthTimeSeries;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.DateTime_JPanel;
import RTi.Util.Time.TimeInterval;

@SuppressWarnings("serial")
public class NewStatisticMonthTimeSeries_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private JFrame __parent_JFrame = null;
private NewStatisticMonthTimeSeries_Command __command = null;
private JTextArea __command_JTextArea=null;
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
private SimpleJComboBox	__TSID_JComboBox = null;// Time series to evaluate.
private JTextArea __NewTSID_JTextArea = null; // New TSID.
private SimpleJComboBox	__Statistic_JComboBox = null; // Statistic to analyze.
private JTextField __TestValue_JTextField = null; // Test value for the statistic.
private JTextField __MonthTestValues_JTextField = null; // Monthly test values for the statistic.
private JTextField __AllowMissingCount_JTextField = null; // Missing data count allowed in analysis interval.
private JTextField __MinimumSampleSize_JTextField = null;
private JTextField __AnalysisStart_JTextField = null; // Fields for analysis period (time series period).
private JTextField __AnalysisEnd_JTextField = null;
private JCheckBox __AnalysisWindow_JCheckBox = null;
private DateTime_JPanel __AnalysisWindowStart_JPanel = null; // Fields for analysis window within a year.
private DateTime_JPanel __AnalysisWindowEnd_JPanel = null;
private JTextField __SearchStart_JTextField = null;
private SimpleJButton __edit_JButton = null; // Edit TSID button.
private SimpleJButton __clear_JButton = null; // Clear NewTSID button.
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Whether OK has been pressed.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public NewStatisticMonthTimeSeries_JDialog ( JFrame parent, NewStatisticMonthTimeSeries_Command command )
{	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	Object o = event.getSource();
	String routine = "NewStatisticMonthTimeSeries_JDialog.actionPerformed";

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
				// Set some defaults to guide the user.
				tsident.setInterval(TimeInterval.MONTH, 1);
				tsident.setScenario(__Statistic_JComboBox.getSelected());
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
			Message.printWarning ( 1, routine, "Error creating time series identifier from \"" + NewTSID + "\"." );
			Message.printWarning ( 3, routine, e );
		}
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "NewStatisticMonthTimeSeries");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
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
Check the state of the dialog, disabling/enabling components as appropriate.
*/
private void checkGUIState() {	
    if ( __AnalysisWindow_JCheckBox.isSelected() ) {
        // Checked so enable the date panels.
        __AnalysisWindowStart_JPanel.setEnabled ( true );
        __AnalysisWindowEnd_JPanel.setEnabled ( true );
    }
    else {
        __AnalysisWindowStart_JPanel.setEnabled ( false );
        __AnalysisWindowEnd_JPanel.setEnabled ( false );
    }
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag to true.
This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check.
	PropList props = new PropList ( "" );
	String Alias = __Alias_JTextField.getText().trim();
	String TSID = __TSID_JComboBox.getSelected();
	String NewTSID = __NewTSID_JTextArea.getText().trim();
	String Statistic = __Statistic_JComboBox.getSelected();
	String TestValue = __TestValue_JTextField.getText().trim();
	String MonthTestValues = __MonthTestValues_JTextField.getText().trim();
	String AllowMissingCount = __AllowMissingCount_JTextField.getText().trim();
	String MinimumSampleSize = __MinimumSampleSize_JTextField.getText().trim();
	String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	String SearchStart = __SearchStart_JTextField.getText().trim();
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
	if ( (TestValue != null) && (TestValue.length() > 0) ) {
		props.set ( "TestValue", TestValue );
	}
    if ( (MonthTestValues != null) && (MonthTestValues.length() > 0) ) {
        props.set ( "MonthTestValues", MonthTestValues );
    }
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
    if ( SearchStart.length() > 0 ) {
        props.set ( "SearchStart", SearchStart );
    }
	if ( __AnalysisWindow_JCheckBox.isSelected() ){
    	String AnalysisWindowStart = __AnalysisWindowStart_JPanel.toString(false,true).trim();
        String AnalysisWindowEnd = __AnalysisWindowEnd_JPanel.toString(false,true).trim();
        if ( AnalysisWindowStart.length() > 0 ) {
            props.set ( "AnalysisWindowStart", AnalysisWindowStart );
        }
        if ( AnalysisWindowEnd.length() > 0 ) {
            props.set ( "AnalysisWindowEnd", AnalysisWindowEnd );
        }
	}
	try {
	    // This will warn the user.
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
	String TestValue = __TestValue_JTextField.getText().trim();
	String MonthTestValues = __MonthTestValues_JTextField.getText().trim();
	String AllowMissingCount = __AllowMissingCount_JTextField.getText().trim();
	String MinimumSampleSize = __MinimumSampleSize_JTextField.getText().trim();
	String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	String SearchStart = __SearchStart_JTextField.getText().trim();
	__command.setCommandParameter ( "Alias", Alias );
	__command.setCommandParameter ( "TSID", TSID );
	__command.setCommandParameter ( "NewTSID", NewTSID );
	__command.setCommandParameter ( "Statistic", Statistic );
	__command.setCommandParameter ( "TestValue", TestValue );
	__command.setCommandParameter ( "MonthTestValues", MonthTestValues );
	__command.setCommandParameter ( "AllowMissingCount", AllowMissingCount);
	__command.setCommandParameter ( "MinimumSampleSize", MinimumSampleSize);
	__command.setCommandParameter ( "AnalysisStart", AnalysisStart );
	__command.setCommandParameter ( "AnalysisEnd", AnalysisEnd );
	if ( __AnalysisWindow_JCheckBox.isSelected() ){
	    String AnalysisWindowStart = __AnalysisWindowStart_JPanel.toString(false,true).trim();
	    String AnalysisWindowEnd = __AnalysisWindowEnd_JPanel.toString(false,true).trim();
	    __command.setCommandParameter ( "AnalysisWindowStart", AnalysisWindowStart );
	    __command.setCommandParameter ( "AnalysisWindowEnd", AnalysisWindowEnd );
	}
	__command.setCommandParameter ( "SearchStart", SearchStart );
}

/**
Instantiates the GUI components.
@param parent Frame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, NewStatisticMonthTimeSeries_Command command )
{	__parent_JFrame = parent;
	__command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "<html><b>This command is under development.  Currently only daily input time series are supported.</b></html>" ),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Create a time series where each value is a statistic calculated from a month of data from the " +
		"input time series.  The output time series has an interval of Month." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"It is recommended that new time series identifier (TSID)" +
		" information be specified for the output time series to avoid confusing the output with the original." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
	JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++y, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel("Time series to analyze (TSID):"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__TSID_JComboBox = new SimpleJComboBox ( true );	// Allow edit.
	
	List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
			(TSCommandProcessor)__command.getCommandProcessor(), __command );
	
	if ( tsids == null ) {
		// User will not be able to select anything.
		tsids = new Vector<String>();
	}
	
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
	__NewTSID_JTextArea.addKeyListener ( this );
	// Make 3-high to fit in the edit button.
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__NewTSID_JTextArea),
		1, y, 2, 3, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Recommended - to avoid confusion with TSID from original time series."), 
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	y += 2;
    JGUIUtil.addComponent(main_JPanel, (__edit_JButton = new SimpleJButton ( "Edit", "Edit", this ) ),
		3, y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(main_JPanel, (__clear_JButton = new SimpleJButton ( "Clear", "Clear", this ) ),
		4, y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Statistic:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Statistic_JComboBox = new SimpleJComboBox(false);
	// TODO SAM 2009-11-11 Ideally should figure out the input time series interval and limit the choices.
	__Statistic_JComboBox.setData ( TSUtil_NewStatisticMonthTimeSeries.getStatisticChoicesForIntervalAsStrings (
	    TimeInterval.UNKNOWN, null ) );
	__Statistic_JComboBox.select ( 0 );
	__Statistic_JComboBox.addActionListener (this);
	JGUIUtil.addComponent(main_JPanel, __Statistic_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Required - statistic to calculate."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Test value:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__TestValue_JTextField = new JTextField (10);
	__TestValue_JTextField.addKeyListener (this);
	JGUIUtil.addComponent(main_JPanel, __TestValue_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - test value (required for comparison statistics)."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Test values (by month):"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MonthTestValues_JTextField = new JTextField (30);
    __MonthTestValues_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __MonthTestValues_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - test values by month (Jan,Feb,...)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Allow missing count:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__AllowMissingCount_JTextField = new JTextField (10);
	__AllowMissingCount_JTextField.addKeyListener (this);
	JGUIUtil.addComponent(main_JPanel, __AllowMissingCount_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - number of missing values allowed in analysis interval (default=allow missing)."),
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
	
	__AnalysisWindow_JCheckBox = new JCheckBox ( "Analysis window:", false );
	__AnalysisWindow_JCheckBox.setVisible(false);// TODO SAM 2014-04-03 Enable later.
	__AnalysisWindow_JCheckBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __AnalysisWindow_JCheckBox, 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    JPanel analysisWindow_JPanel = new JPanel();
    analysisWindow_JPanel.setVisible(false); // TODO SAM 2014-04-03 enable later.
    analysisWindow_JPanel.setLayout(new GridBagLayout());
    __AnalysisWindowStart_JPanel = new DateTime_JPanel ( "Start", TimeInterval.MONTH, TimeInterval.HOUR, null );
    __AnalysisWindowStart_JPanel.addActionListener(this);
    __AnalysisWindowStart_JPanel.addKeyListener ( this );
    JGUIUtil.addComponent(analysisWindow_JPanel, __AnalysisWindowStart_JPanel,
        1, 0, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    // TODO SAM 2008-01-23 Figure out how to display the correct limits given the time series interval.
    __AnalysisWindowEnd_JPanel = new DateTime_JPanel ( "End", TimeInterval.MONTH, TimeInterval.HOUR, null );
    __AnalysisWindowEnd_JPanel.setVisible(false); // TODO SAM 2014-04-03 enable later.
    __AnalysisWindowEnd_JPanel.addActionListener(this);
    __AnalysisWindowEnd_JPanel.addKeyListener ( this );
    JGUIUtil.addComponent(analysisWindow_JPanel, __AnalysisWindowEnd_JPanel,
        4, 0, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, analysisWindow_JPanel,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    //JGUIUtil.addComponent(main_JPanel, new JLabel(
    //    "Optional - analysis window within input year (default=full year)."),
    //    3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    //JGUIUtil.addComponent(main_JPanel, new JLabel ("Search start:"),
    //    0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SearchStart_JTextField = new JTextField (10);
    __SearchStart_JTextField.setVisible(false);// TODO 2014-04-03 enable later.
    __SearchStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __SearchStart_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    //JGUIUtil.addComponent(main_JPanel, new JLabel (
    //    "Optional - search start (needed for some statistics, default=full year)."),
    //    3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea (4,50);
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);

	// Refresh the contents.
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
@return true if the edits were committed, false if the user cancelled.
*/
public boolean ok ()
{	return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{	String routine = "NewStatisticMonthTimeSeries_JDialog.refresh";
	String Alias = "";
	String TSID = "";
	String NewTSID = "";
	String Statistic = "";
	String TestValue = "";
	String MonthTestValues = "";
	String AllowMissingCount = "";
	String MinimumSampleSize = "";
	String AnalysisStart = "";
	String AnalysisEnd = "";
	String AnalysisWindowStart = "";
	String AnalysisWindowEnd = "";
	String SearchStart = "";
	PropList props = __command.getCommandParameters();
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		Alias = props.getValue ( "Alias" );
		TSID = props.getValue ( "TSID" );
		NewTSID = props.getValue ( "NewTSID" );
		Statistic = props.getValue ( "Statistic" );
		TestValue = props.getValue ( "TestValue" );
		MonthTestValues = props.getValue ( "MonthTestValues" );
		AllowMissingCount = props.getValue ( "AllowMissingCount" );
		MinimumSampleSize = props.getValue ( "MinimumSampleSize" );
		AnalysisStart = props.getValue ( "AnalysisStart" );
		AnalysisEnd = props.getValue ( "AnalysisEnd" );
		AnalysisWindowStart = props.getValue ( "AnalysisWindowStart" );
        AnalysisWindowEnd = props.getValue ( "AnalysisWindowEnd" );
        SearchStart = props.getValue ( "SearchStart" );
		if ( Alias != null ) {
			__Alias_JTextField.setText ( Alias );
		}
		// Now select the item in the list.  If not a match, print a warning.
		if ( JGUIUtil.isSimpleJComboBoxItem( __TSID_JComboBox, TSID,	JGUIUtil.NONE, null, null ) ) {
				__TSID_JComboBox.select ( TSID );
		}
		else {	// Automatically add to the list after the blank.
			if ( (TSID != null) && (TSID.length() > 0) ) {
				__TSID_JComboBox.insertItemAt ( TSID, 1 );
				// Select.
				__TSID_JComboBox.select ( TSID );
			}
			else {
			    // Do not select anything.
			}
		}
		if ( NewTSID != null ) {
			__NewTSID_JTextArea.setText ( NewTSID );
		}
		if ( Statistic == null ) {
			// Select default.
			__Statistic_JComboBox.select ( 0 );
		}
		else {
		    if ( JGUIUtil.isSimpleJComboBoxItem( __Statistic_JComboBox, Statistic, JGUIUtil.NONE, null, null ) ) {
				__Statistic_JComboBox.select ( Statistic );
			}
			else {
			    Message.printWarning ( 1, routine,
				"Existing command references an invalid\nStatistic value \"" +
				Statistic +	"\".  Select a different value or Cancel.");
				__error_wait = true;
			}
		}
		if ( TestValue != null ) {
			__TestValue_JTextField.setText ( TestValue );
		}
        if ( MonthTestValues != null ) {
            __MonthTestValues_JTextField.setText ( MonthTestValues );
        }
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
        if ( (AnalysisWindowStart != null) && (AnalysisWindowStart.length() > 0) ) {
            try {
                // Add year because it is not part of the parameter value.
                DateTime AnalysisWindowStart_DateTime = DateTime.parse ( "0000-" + AnalysisWindowStart );
                Message.printStatus(2, routine, "Setting window start to " + AnalysisWindowStart_DateTime );
                __AnalysisWindowStart_JPanel.setDateTime ( AnalysisWindowStart_DateTime );
            }
            catch ( Exception e ) {
                Message.printWarning( 1, routine, "AnalysisWindowStart (" + AnalysisWindowStart +
                        ") is not a valid date/time." );
            }
        }
        if ( (AnalysisWindowEnd != null) && (AnalysisWindowEnd.length() > 0) ) {
            try {
                // Add year because it is not part of the parameter value.
                DateTime AnalysisWindowEnd_DateTime = DateTime.parse ( "0000-" + AnalysisWindowEnd );
                Message.printStatus(2, routine, "Setting window end to " + AnalysisWindowEnd_DateTime );
                __AnalysisWindowEnd_JPanel.setDateTime ( AnalysisWindowEnd_DateTime );
            }
            catch ( Exception e ) {
                Message.printWarning( 1, routine, "AnalysisWindowEnd (" + AnalysisWindowEnd +
                        ") is not a valid date/time." );
            }
        }
        if ( (AnalysisWindowStart != null) && (AnalysisWindowStart.length() != 0) &&
                (AnalysisWindowEnd != null) && (AnalysisWindowEnd.length() != 0)) {
            __AnalysisWindow_JCheckBox.setSelected ( true );
        }
        else {
            __AnalysisWindow_JCheckBox.setSelected ( false );
        }
        if ( SearchStart != null ) {
            __SearchStart_JTextField.setText( SearchStart );
        }
	}
	// Regardless, reset the command from the fields.
	checkGUIState();
	Alias = __Alias_JTextField.getText().trim();
	TSID = __TSID_JComboBox.getSelected();
	NewTSID = __NewTSID_JTextArea.getText().trim();
	Statistic = __Statistic_JComboBox.getSelected();
	TestValue = __TestValue_JTextField.getText();
	MonthTestValues = __MonthTestValues_JTextField.getText();
	AllowMissingCount = __AllowMissingCount_JTextField.getText();
	MinimumSampleSize = __MinimumSampleSize_JTextField.getText();
	AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	SearchStart = __SearchStart_JTextField.getText().trim();
	props = new PropList ( __command.getCommandName() );
	props.add ( "Alias=" + Alias );
	props.add ( "TSID=" + TSID );
	props.add ( "NewTSID=" + NewTSID );
	props.add ( "Statistic=" + Statistic );
	props.add ( "TestValue=" + TestValue );
	props.add ( "MonthTestValues=" + MonthTestValues );
	props.add ( "AllowMissingCount=" + AllowMissingCount );
	props.add ( "MinimumSampleSize=" + MinimumSampleSize );
	props.add ( "AnalysisStart=" + AnalysisStart );
	props.add ( "AnalysisEnd=" + AnalysisEnd );
	if ( __AnalysisWindow_JCheckBox.isSelected() ) {
	    AnalysisWindowStart = __AnalysisWindowStart_JPanel.toString(false,true).trim();
	    AnalysisWindowEnd = __AnalysisWindowEnd_JPanel.toString(false,true).trim();
	    props.add ( "AnalysisWindowStart=" + AnalysisWindowStart );
	    props.add ( "AnalysisWindowEnd=" + AnalysisWindowEnd );
	}
	props.add ( "SearchStart=" + SearchStart );
	__command_JTextArea.setText( __command.toString ( props ).trim() );
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed and the dialog is closed.
*/
private void response ( boolean ok )
{	__ok = ok;	// Save to be returned by ok().
	if ( ok ) {
		// Commit the changes.
		commitEdits ();
		if ( __error_wait ) {
			// Not ready to close out.
			return;
		}
	}
	// Now close out.
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