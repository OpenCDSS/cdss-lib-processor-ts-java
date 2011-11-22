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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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

import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.TS.TSRegression;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.IO.PropList;
import RTi.Util.Math.DataTransformationType;
import RTi.Util.Math.NumberOfEquationsType;
import RTi.Util.Message.Message;

public class FillRegression_JDialog extends JDialog
implements ActionListener, KeyListener, ItemListener, ListSelectionListener, WindowListener
{

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private JTextArea __command_JTextArea=null;
private JTextField __AnalysisStart_JTextField;
private JTextField __AnalysisEnd_JTextField;
private JTextField __FillStart_JTextField;
private JTextField __FillEnd_JTextField;
private JTextField __FillFlag_JTextField;
private SimpleJComboBox	__TSID_JComboBox = null;
private SimpleJComboBox	__IndependentTSID_JComboBox= null;
private SimpleJComboBox	__NumberOfEquations_JComboBox = null;
private SimpleJComboBox	__AnalysisMonth_JComboBox = null;
private SimpleJComboBox	__Transformation_JComboBox=null;
private JTextField __Intercept_JTextField = null;
private JTextField __LEZeroLogValue_JTextField = null;
private JTextField __MinimumSampleSize_JTextField = null;
private JTextField __MinimumR_JTextField = null;
private JTextField __ConfidenceInterval_JTextField = null;
private SimpleJComboBox __TableID_JComboBox = null;
private JTextField __TableTSIDColumn_JTextField = null;
private TSFormatSpecifiersJPanel __TableTSIDFormat_JTextField = null;
private boolean __error_wait = false; // True if there is an error in the input.
private boolean __first_time = true;
private boolean __ok = false; // Was OK pressed last (false=cancel)?
private FillRegression_Command __command = null;

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices choices for TableID value.
*/
public FillRegression_JDialog ( JFrame parent, FillRegression_Command command, List<String> tableIDChoices )
{	super(parent, true);
	initialize ( parent, command, tableIDChoices );
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
	if ( NumberOfEquations.equalsIgnoreCase(""+NumberOfEquationsType.MONTHLY_EQUATIONS) ) {
		JGUIUtil.setEnabled(__AnalysisMonth_JComboBox,true);
	}
	else {
        JGUIUtil.setEnabled(__AnalysisMonth_JComboBox,false);
	}

	String Transformation = __Transformation_JComboBox.getSelected();
	if ( Transformation.equalsIgnoreCase(""+DataTransformationType.NONE) || Transformation.equals("") ) {
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
	String LEZeroLogValue = __LEZeroLogValue_JTextField.getText().trim();
    String MinimumSampleSize = __MinimumSampleSize_JTextField.getText().trim();
    String MinimumR = __MinimumR_JTextField.getText().trim();
    String ConfidenceInterval = __ConfidenceInterval_JTextField.getText().trim();
	String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	String FillStart = __FillStart_JTextField.getText().trim();
	String FillEnd = __FillEnd_JTextField.getText().trim();
	String FillFlag = __FillFlag_JTextField.getText().trim();
	String TableID = __TableID_JComboBox.getSelected();
    String TableTSIDColumn = __TableTSIDColumn_JTextField.getText().trim();
    String TableTSIDFormat = __TableTSIDFormat_JTextField.getText().trim();
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
    if ( LEZeroLogValue.length() > 0 ) {
        props.set ( "LEZeroLogValue", LEZeroLogValue );
    }
    if ( MinimumSampleSize.length() > 0 ) {
        props.set ( "MinimumSampleSize", MinimumSampleSize );
    }
    if ( MinimumR.length() > 0 ) {
        props.set ( "MinimumR", MinimumR );
    }
    if ( ConfidenceInterval.length() > 0 ) {
        props.set ( "ConfidenceInterval", ConfidenceInterval );
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
    if ( TableID.length() > 0 ) {
        props.set ( "TableID", TableID );
    }
    if ( TableTSIDColumn.length() > 0 ) {
        props.set ( "TableTSIDColumn", TableTSIDColumn );
    }
    if ( TableTSIDFormat.length() > 0 ) {
        props.set ( "TableTSIDFormat", TableTSIDFormat );
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
{	String TSID = __TSID_JComboBox.getSelected();
	String IndependentTSID = __IndependentTSID_JComboBox.getSelected();
	String NumberOfEquations = __NumberOfEquations_JComboBox.getSelected();
	String AnalysisMonth = __AnalysisMonth_JComboBox.getSelected();
	String Transformation = __Transformation_JComboBox.getSelected();
	String Intercept = __Intercept_JTextField.getText().trim();
    String LEZeroLogValue = __LEZeroLogValue_JTextField.getText().trim();
    String MinimumSampleSize = __MinimumSampleSize_JTextField.getText().trim();
    String MinimumR = __MinimumR_JTextField.getText().trim();
    String ConfidenceInterval = __ConfidenceInterval_JTextField.getText().trim();
	String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	String FillStart = __FillStart_JTextField.getText().trim();
	String FillEnd = __FillEnd_JTextField.getText().trim();
	String FillFlag = __FillFlag_JTextField.getText().trim();
    String TableID = __TableID_JComboBox.getSelected();
    String TableTSIDColumn = __TableTSIDColumn_JTextField.getText().trim();
    String TableTSIDFormat = __TableTSIDFormat_JTextField.getText().trim();
	__command.setCommandParameter ( "TSID", TSID );
	__command.setCommandParameter ( "IndependentTSID", IndependentTSID );
	__command.setCommandParameter ( "NumberOfEquations", NumberOfEquations);
	__command.setCommandParameter ( "AnalysisMonth", AnalysisMonth );
	__command.setCommandParameter ( "Transformation", Transformation );
	__command.setCommandParameter ( "Intercept", Intercept );
	__command.setCommandParameter ( "LEZeroLogValue", LEZeroLogValue );
	__command.setCommandParameter ( "MinimumSampleSize", MinimumSampleSize );
	__command.setCommandParameter ( "MinimumR", MinimumR );
	__command.setCommandParameter ( "ConfidenceInterval", ConfidenceInterval );
	__command.setCommandParameter ( "AnalysisStart", AnalysisStart );
	__command.setCommandParameter ( "AnalysisEnd", AnalysisEnd );
	__command.setCommandParameter ( "FillStart", FillStart );
	__command.setCommandParameter ( "FillEnd", FillEnd );
	__command.setCommandParameter ( "FillFlag", FillFlag );
    __command.setCommandParameter ( "TableID", TableID );
    __command.setCommandParameter ( "TableTSIDColumn", TableTSIDColumn );
    __command.setCommandParameter ( "TableTSIDFormat", TableTSIDFormat );
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
private void initialize ( JFrame parent, FillRegression_Command command, List<String> tableIDChoices  )
{	__command = command;

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
		"The analysis period is used to determine relationships used for filling." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Use a SetOutputPeriod() command before reading to extend the dependent time series, if necessary." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specify dates with precision appropriate for the data, " +
		"use blank for all available data, OutputStart, or OutputEnd."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The MinimumSampleSize, MinimumR, and ConfidenceInterval parameters constrain filling - " +
        "if criteria are not met, the filling will not occur." ),
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
	__NumberOfEquations_JComboBox.addItem ( "" + NumberOfEquationsType.ONE_EQUATION );
	__NumberOfEquations_JComboBox.addItem ( "" + NumberOfEquationsType.MONTHLY_EQUATIONS );
	__NumberOfEquations_JComboBox.select ( 0 );
	__NumberOfEquations_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __NumberOfEquations_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional - number of equations (default=" + NumberOfEquationsType.ONE_EQUATION + ")."), 
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
		"Optional - use with monthly equations (default=process all months)."), 
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
		"Optional - how to transform data before analysis (blank=" + DataTransformationType.NONE + ")."), 
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Value to use when log and <= 0:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __LEZeroLogValue_JTextField = new JTextField ( 5 );
    __LEZeroLogValue_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __LEZeroLogValue_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - value to substitute when original is <= 0 and log transform (default=" +
        TSRegression.getDefaultLEZeroLogValue() + ")."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Intercept:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Intercept_JTextField = new JTextField ( 5 );
	__Intercept_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Intercept_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional - blank or 0.0 are allowed with no transformation."), 
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Minimum sample size
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Minimum sample size:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MinimumSampleSize_JTextField = new JTextField ( 10 );
    __MinimumSampleSize_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __MinimumSampleSize_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - minimum number of overlapping points required for analysis (default=no limit)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Minimum R
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Minimum R:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MinimumR_JTextField = new JTextField ( 10 );
    __MinimumR_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __MinimumR_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - minimum correlation coefficient R required for a best fit (default=no limit)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Confidence interval
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Confidence interval:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ConfidenceInterval_JTextField = new JTextField ( 10 );
    __ConfidenceInterval_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __ConfidenceInterval_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - confidence interval (%) for line slope (default=do not check interval)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Analysis start:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __AnalysisStart_JTextField = new JTextField ( "", 20 );
    __AnalysisStart_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __AnalysisStart_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - starting date/time (default=full period)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Analysis end:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __AnalysisEnd_JTextField = new JTextField ( "", 20 );
    __AnalysisEnd_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __AnalysisEnd_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - ending date/time (default=full period)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel,new JLabel( "Fill start:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __FillStart_JTextField = new JTextField ( "", 10 );
    __FillStart_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __FillStart_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - fill start date/time (default=full period)."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel,new JLabel("Fill end:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __FillEnd_JTextField = new JTextField ( "", 10 );
    __FillEnd_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __FillEnd_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - fill end date/time (default=full period)."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Fill flag:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__FillFlag_JTextField = new JTextField ( 5 );
	__FillFlag_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __FillFlag_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Optional - string to indicate filled values."), 
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Table ID for output:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableID_JComboBox = new SimpleJComboBox ( 12, true );    // Allow edit
    tableIDChoices.add(0,""); // Add blank to ignore table
    __TableID_JComboBox.setData ( tableIDChoices );
    __TableID_JComboBox.addItemListener ( this );
    //__TableID_JComboBox.setMaximumRowCount(tableIDChoices.size());
    JGUIUtil.addComponent(main_JPanel, __TableID_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - specify to output statistics to table."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Table TSID column:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableTSIDColumn_JTextField = new JTextField ( 10 );
    __TableTSIDColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __TableTSIDColumn_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Required if using table - column name for dependent TSID."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel("Format of TSID:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableTSIDFormat_JTextField = new TSFormatSpecifiersJPanel(10);
    __TableTSIDFormat_JTextField.setToolTipText("Use %L for location, %T for data type, %I for interval.");
    __TableTSIDFormat_JTextField.addKeyListener ( this );
    __TableTSIDFormat_JTextField.setToolTipText("%L for location, %T for data type.");
    JGUIUtil.addComponent(main_JPanel, __TableTSIDFormat_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - use %L for location, etc. (default=alias or TSID)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

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
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged ( ItemEvent e )
{   checkGUIState();
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
Refresh the expression from the other text field contents.
*/
private void refresh ()
{	String routine = getClass().getName() + ".refresh";
    String TSID = "";
	String IndependentTSID = "";
	String NumberOfEquations = "";
	String AnalysisMonth = "";
	String Transformation = "";
	String Intercept = "";
	String LEZeroLogValue = "";
    String MinimumSampleSize = "";
    String MinimumR = "";
    String ConfidenceInterval = "";
	String AnalysisStart = "";
	String AnalysisEnd = "";
	String FillStart = "";
	String FillEnd = "";
	String FillFlag = "";
    String TableID = "";
    String TableTSIDColumn = "";
    String TableTSIDFormat = "";
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
		LEZeroLogValue = props.getValue ( "LEZeroLogValue" );
	    MinimumSampleSize = props.getValue ( "MinimumSampleSize" );
	    MinimumR = props.getValue ( "MinimumR" );
	    ConfidenceInterval = props.getValue ( "ConfidenceInterval" );
		AnalysisStart = props.getValue("AnalysisStart");
		AnalysisEnd = props.getValue("AnalysisEnd");
		FillStart = props.getValue("FillStart");
		FillEnd = props.getValue("FillEnd");
		FillFlag = props.getValue("FillFlag");
        TableID = props.getValue ( "TableID" );
        TableTSIDColumn = props.getValue ( "TableTSIDColumn" );
        TableTSIDFormat = props.getValue ( "TableTSIDFormat" );
		// Now check the information and set in the GUI...
		if ( JGUIUtil.isSimpleJComboBoxItem( __TSID_JComboBox, TSID, JGUIUtil.NONE, null, null ) ) {
			__TSID_JComboBox.select ( TSID );
		}
		else {
            /* TODO SAM 2005-04-27 disable since this may prohibit advanced users.
			Message.printWarning ( 1,
				"fillRegression_JDialog.refresh", "Existing " +
				"fillRegression() references a non-existent\n"+
				"time series \"" + alias + "\".  Select a\n" +
				"different time series or Cancel." );
			}
			*/
			if ( (TSID == null) || TSID.equals("") ) {
				// For new command... Select the first item in the list...
				if ( __TSID_JComboBox.getItemCount() > 0 ) {
					__TSID_JComboBox.select ( 0 );
				}
			}
			else {
			    // Automatically add to the list at the top... 
				__TSID_JComboBox.insertItemAt ( TSID, 0 );
				// Select...
				__TSID_JComboBox.select ( TSID );
			}
		}
		if ( JGUIUtil.isSimpleJComboBoxItem( __IndependentTSID_JComboBox, IndependentTSID, JGUIUtil.NONE, null, null ) ) {
			__IndependentTSID_JComboBox.select ( IndependentTSID );
		}
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
			else {
			    // Automatically add to the list at the top... 
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
			else {
			    // Bad user command...
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
        if ( LEZeroLogValue != null ) {
            __LEZeroLogValue_JTextField.setText ( LEZeroLogValue );
        }
        if ( MinimumSampleSize != null ) {
            __MinimumSampleSize_JTextField.setText ( MinimumSampleSize );
        }
        if ( MinimumR != null ) {
            __MinimumR_JTextField.setText ( MinimumR );
        }
        if ( ConfidenceInterval != null ) {
            __ConfidenceInterval_JTextField.setText ( ConfidenceInterval );
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
	}
	// Regardless, reset the expression from the fields.  This is only the
	// visible information and has not yet been committed in the command.
	TSID = __TSID_JComboBox.getSelected();
	IndependentTSID = __IndependentTSID_JComboBox.getSelected();
	NumberOfEquations = __NumberOfEquations_JComboBox.getSelected();
	AnalysisMonth = __AnalysisMonth_JComboBox.getSelected();
	Transformation = __Transformation_JComboBox.getSelected();
	Intercept = __Intercept_JTextField.getText().trim();
	LEZeroLogValue = __LEZeroLogValue_JTextField.getText().trim();
    MinimumSampleSize = __MinimumSampleSize_JTextField.getText().trim();
    MinimumR = __MinimumR_JTextField.getText().trim();
    ConfidenceInterval = __ConfidenceInterval_JTextField.getText().trim();
	AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	FillStart = __FillStart_JTextField.getText().trim();
	FillEnd = __FillEnd_JTextField.getText().trim();
	FillFlag = __FillFlag_JTextField.getText().trim();
    TableID = __TableID_JComboBox.getSelected();
    TableTSIDColumn = __TableTSIDColumn_JTextField.getText().trim();
    TableTSIDFormat = __TableTSIDFormat_JTextField.getText().trim();
	props = new PropList ( __command.getCommandName() );
	props.add ( "TSID=" + TSID );
	props.add ( "IndependentTSID=" + IndependentTSID );
	props.add ( "NumberOfEquations=" + NumberOfEquations );
	if ( __AnalysisMonth_JComboBox.isEnabled() ) {
		props.add ( "AnalysisMonth=" + AnalysisMonth );
	}
	props.add ( "Transformation=" + Transformation );
	props.add ( "Intercept=" + Intercept );
	props.add ( "LEZeroLogValue=" + LEZeroLogValue );
    props.add ( "MinimumSampleSize=" + MinimumSampleSize );
    props.add ( "MinimumR=" + MinimumR );
    props.add ( "ConfidenceInterval=" + ConfidenceInterval );
	props.add ( "AnalysisStart=" + AnalysisStart );
	props.add ( "AnalysisEnd=" + AnalysisEnd );
	props.add ( "FillStart=" + FillStart );
	props.add ( "FillEnd=" + FillEnd );
	props.add ( "FillFlag=" + FillFlag );
    props.add ( "TableID=" + TableID );
    props.add ( "TableTSIDColumn=" + TableTSIDColumn );
    props.add ( "TableTSIDFormat=" + TableTSIDFormat );
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

}