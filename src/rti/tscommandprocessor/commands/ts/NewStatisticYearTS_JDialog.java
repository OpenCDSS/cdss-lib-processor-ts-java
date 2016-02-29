package rti.tscommandprocessor.commands.ts;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;
import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.TS.TSIdent;
import RTi.TS.TSIdent_JDialog;
import RTi.TS.TSUtil_NewStatisticYearTS;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.DateTime_JPanel;
import RTi.Util.Time.TimeInterval;
import RTi.Util.Time.YearType;

public class NewStatisticYearTS_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private JFrame __parent_JFrame = null;
private NewStatisticYearTS_Command __command = null;
private JTabbedPane __main_JTabbedPane = null;
private JTextArea __command_JTextArea=null;
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
private SimpleJComboBox __TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox __TSID_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private JLabel __NewEnsembleID_JLabel = null;
private JTextField __NewEnsembleID_JTextField;
private JLabel __NewEnsembleName_JLabel = null;
private JTextField __NewEnsembleName_JTextField;
private JTextArea __NewTSID_JTextArea = null;
private SimpleJComboBox	__Statistic_JComboBox = null;
private JTextField __TestValue_JTextField = null;
private JTextField __Value1_JTextField = null;
private JTextField __AllowMissingCount_JTextField = null;
private JTextField __MinimumSampleSize_JTextField = null;
private SimpleJComboBox __OutputYearType_JComboBox = null;
private JTextField __AnalysisStart_JTextField = null; // Fields for analysis period (time series period)
private JTextField __AnalysisEnd_JTextField = null;
private JCheckBox __AnalysisWindow_JCheckBox = null;
private DateTime_JPanel __AnalysisWindowStart_JPanel = null; // Fields for analysis window within a year
private DateTime_JPanel __AnalysisWindowEnd_JPanel = null;
private JTextField __SearchStart_JTextField = null;
private SimpleJButton __edit_JButton = null; // Edit TSID button
private SimpleJButton __clear_JButton = null; // Clear NewTSID button
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Whether OK has been pressed.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public NewStatisticYearTS_JDialog ( JFrame parent, NewStatisticYearTS_Command command )
{	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	Object o = event.getSource();
	String routine = "NewStatisticYearTS_JDialog.actionPerformed";

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
				// Set some defaults to guide the user
				tsident.setInterval(TimeInterval.YEAR, 1);
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
private void checkGUIState()
{	// Handle TSList-related parameter editing...
    String TSList = __TSList_JComboBox.getSelected();
    if ( TSListType.ALL_MATCHING_TSID.equals(TSList) ||
        TSListType.FIRST_MATCHING_TSID.equals(TSList) ||
        TSListType.LAST_MATCHING_TSID.equals(TSList) ) {
        __TSID_JComboBox.setEnabled(true);
        __TSID_JLabel.setEnabled ( true );
    }
    else {
        __TSID_JComboBox.setEnabled(false);
        __TSID_JLabel.setEnabled ( false );
    }
    if ( TSListType.ENSEMBLE_ID.equals(TSList)) {
        __EnsembleID_JLabel.setEnabled ( true );
        __EnsembleID_JComboBox.setEnabled(true);
        __NewEnsembleID_JLabel.setEnabled ( true );
        __NewEnsembleID_JTextField.setEnabled(true);
        __NewEnsembleName_JLabel.setEnabled ( true );
        __NewEnsembleName_JTextField.setEnabled(true);
    }
    else {
        __EnsembleID_JLabel.setEnabled ( false );
        __EnsembleID_JComboBox.setEnabled(false);
        __NewEnsembleID_JLabel.setEnabled ( false );
        __NewEnsembleID_JTextField.setEnabled(false);
        __NewEnsembleName_JLabel.setEnabled ( false );
        __NewEnsembleName_JTextField.setEnabled(false);
    }
    if ( __AnalysisWindow_JCheckBox.isSelected() ) {
        // Checked so enable the date panels
        __AnalysisWindowStart_JPanel.setEnabled ( true );
        __AnalysisWindowEnd_JPanel.setEnabled ( true );
    }
    else {
        __AnalysisWindowStart_JPanel.setEnabled ( false );
        __AnalysisWindowEnd_JPanel.setEnabled ( false );
    }
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
	PropList props = new PropList ( "" );
	String Alias = __Alias_JTextField.getText().trim();
	String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String NewEnsembleID = __NewEnsembleID_JTextField.getText().trim();
    String NewEnsembleName = __NewEnsembleName_JTextField.getText().trim();
	String NewTSID = __NewTSID_JTextArea.getText().trim();
	String Statistic = __Statistic_JComboBox.getSelected();
	String TestValue = __TestValue_JTextField.getText().trim();
	String Value1 = __Value1_JTextField.getText().trim();
	String AllowMissingCount = __AllowMissingCount_JTextField.getText().trim();
	String MinimumSampleSize = __MinimumSampleSize_JTextField.getText().trim();
    String OutputYearType = __OutputYearType_JComboBox.getSelected();
	String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	String SearchStart = __SearchStart_JTextField.getText().trim();
	__error_wait = false;

	if ( Alias.length() > 0 ) {
		props.set ( "Alias", Alias );
	}
    if ( TSList.length() > 0 ) {
        props.set ( "TSList", TSList );
    }
    if ( TSID.length() > 0 ) {
        props.set ( "TSID", TSID );
    }
    if ( EnsembleID.length() > 0 ) {
        props.set ( "EnsembleID", EnsembleID );
    }
    if ( NewEnsembleID.length() > 0 ) {
        props.set ( "NewEnsembleID", NewEnsembleID );
    }
    if ( NewEnsembleName.length() > 0 ) {
        props.set ( "NewEnsembleName", NewEnsembleName );
    }
	if ( (NewTSID != null) && (NewTSID.length() > 0) ) {
		props.set ( "NewTSID", NewTSID );
	}
	if ( (Statistic != null) && (Statistic.length() > 0) ) {
		props.set ( "Statistic", Statistic );
	}
	if ( (TestValue != null) && (TestValue.length() > 0) ) {
		props.set ( "TestValue", TestValue );
	    // If TestValue is populated set the background to yellow because Value1 should be used instead
		__TestValue_JTextField.setBackground(Color.yellow);
		// If Value1 is not set, automatically set to transition to future
		if ( (Value1 == null) || (Value1.isEmpty()) ) {
			Value1 = TestValue;
		}
	}
	else {
		// Background is white
		__TestValue_JTextField.setBackground(Color.white);
	}
	if ( (Value1 != null) && (Value1.length() > 0) ) {
		props.set ( "Value1", Value1 );
	}
	if ( (AllowMissingCount != null) && (AllowMissingCount.length() > 0) ) {
		props.set ( "AllowMissingCount", AllowMissingCount );
	}
    if ( (MinimumSampleSize != null) && (MinimumSampleSize.length() > 0) ) {
        props.set ( "MinimumSampleSize", MinimumSampleSize );
    }
    if ( OutputYearType.length() > 0 ) {
        props.set ( "OutputYearType", OutputYearType );
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
	String TSList = __TSList_JComboBox.getSelected();
	String TSID = __TSID_JComboBox.getSelected();
	String EnsembleID = __EnsembleID_JComboBox.getSelected();
	String NewEnsembleID = __NewEnsembleID_JTextField.getText().trim();
	String NewEnsembleName = __NewEnsembleName_JTextField.getText().trim();
	String NewTSID = __NewTSID_JTextArea.getText().trim();
	String Statistic = __Statistic_JComboBox.getSelected();
	String TestValue = __TestValue_JTextField.getText().trim();
	String Value1 = __Value1_JTextField.getText().trim();
	String AllowMissingCount = __AllowMissingCount_JTextField.getText().trim();
	String MinimumSampleSize = __MinimumSampleSize_JTextField.getText().trim();
	String OutputYearType = __OutputYearType_JComboBox.getSelected();
	String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	String SearchStart = __SearchStart_JTextField.getText().trim();
	__command.setCommandParameter ( "Alias", Alias );
    __command.setCommandParameter ( "TSList", TSList );
    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
    __command.setCommandParameter ( "NewEnsembleID", NewEnsembleID );
    __command.setCommandParameter ( "NewEnsembleName", NewEnsembleName );
	__command.setCommandParameter ( "NewTSID", NewTSID );
	__command.setCommandParameter ( "Statistic", Statistic );
	__command.setCommandParameter ( "TestValue", TestValue );
	__command.setCommandParameter ( "Value1", Value1 );
	__command.setCommandParameter ( "AllowMissingCount", AllowMissingCount);
	__command.setCommandParameter ( "MinimumSampleSize", MinimumSampleSize);
	__command.setCommandParameter ( "OutputYearType", OutputYearType );
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
private void initialize ( JFrame parent, NewStatisticYearTS_Command command )
{	__parent_JFrame = parent;
	__command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Create a time series (or ensemble of time series) where each value is a statistic calculated from a sample determined from a year of data from the input time series." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The output time series has an interval of year." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    __main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
     
    // Panel for input
    int yInput = -1;
    JPanel input_JPanel = new JPanel();
    input_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Input", input_JPanel );

    JGUIUtil.addComponent(input_JPanel, new JLabel (
		"Each year of the input time series will be processed to provide the sample for the statistic." ),
		0, ++yInput, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(input_JPanel, new JLabel (
		"If an ensemble is specified as input, each trace time series in the ensemble is processed." ),
		0, ++yInput, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(input_JPanel, new JLabel (
		"Optionally specify an output ensemble ID and name to create a new output ensemble." ),
		0, ++yInput, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(input_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yInput, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    __TSList_JComboBox = new SimpleJComboBox(false);
    yInput = CommandEditorUtil.addTSListToEditorDialogPanel ( this, input_JPanel, __TSList_JComboBox, yInput );

    __TSID_JLabel = new JLabel ("TSID (for TSList=*TSID:");
    __TSID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __TSID_JComboBox.setToolTipText("Select a time series TSID/alias from the list or specify with ${Property} notation");
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    yInput = CommandEditorUtil.addTSIDToEditorDialogPanel (
        this, this, input_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, yInput );
    
    __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __EnsembleID_JComboBox.setToolTipText("Select an ensemble identifier from the list or specify with ${Property} notation");
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    yInput = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
        this, this, input_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, yInput );
    
    // Panel for distribution
    int yDist = -1;
    JPanel dist_JPanel = new JPanel();
    dist_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Distribution", dist_JPanel );
    
    JGUIUtil.addComponent(dist_JPanel, new JLabel (
		"A distribution cannot currently be specified.  Statistics are calculated by treating the data as simple sample data." ),
		0, ++yDist, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(dist_JPanel, new JLabel (
		"Support for a distribution may be added in the future, similar to other commands that calculate statistics." ),
		0, ++yDist, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(dist_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yDist, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // Panel for analysis
    int yAnalysis = -1;
    JPanel analysis_JPanel = new JPanel();
    analysis_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Analysis", analysis_JPanel );
    
    JGUIUtil.addComponent(analysis_JPanel, new JLabel (
		"Specify parameters for the statistical analysis." ),
		0, ++yAnalysis, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yAnalysis, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(analysis_JPanel, new JLabel ("Statistic:"),
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Statistic_JComboBox = new SimpleJComboBox(false);
	// TODO SAM 2009-11-11 Ideally should figure out the input time series interval and limit the choices.
	__Statistic_JComboBox.setData ( TSUtil_NewStatisticYearTS.getStatisticChoicesForIntervalAsStrings (
	    TimeInterval.UNKNOWN, null ) );
	__Statistic_JComboBox.select ( 0 );
	__Statistic_JComboBox.addActionListener (this);
	JGUIUtil.addComponent(analysis_JPanel, __Statistic_JComboBox,
		1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Required - statistic to calculate."),
		3, yAnalysis, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(analysis_JPanel, new JLabel ("Test value:"),
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__TestValue_JTextField = new JTextField (10);
	__TestValue_JTextField.setToolTipText("Use Value1 instead of this parameter - TestValue parameter will be removed in the future.");
	__TestValue_JTextField.addKeyListener (this);
	JGUIUtil.addComponent(analysis_JPanel, __TestValue_JTextField,
		1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Legacy - use Value1 as of TSTool 11.09.00."),
		3, yAnalysis, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(analysis_JPanel, new JLabel ("Value1:"),
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Value1_JTextField = new JTextField (10);
	__Value1_JTextField.addKeyListener (this);
	JGUIUtil.addComponent(analysis_JPanel, __Value1_JTextField,
		1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Optional - required by some statistics."),
		3, yAnalysis, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(analysis_JPanel, new JLabel ("Allow missing count:"),
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__AllowMissingCount_JTextField = new JTextField (10);
	__AllowMissingCount_JTextField.addKeyListener (this);
	JGUIUtil.addComponent(analysis_JPanel, __AllowMissingCount_JTextField,
		1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel (
		"Optional - number of missing values allowed in analysis interval (default=allow missing)."),
		3, yAnalysis, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(analysis_JPanel, new JLabel ("Minimum sample size:"),
        0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MinimumSampleSize_JTextField = new JTextField (10);
    __MinimumSampleSize_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(analysis_JPanel, __MinimumSampleSize_JTextField,
        1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel (
        "Optional - minimum required sample size (default=determined by statistic)."),
        3, yAnalysis, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Output year type:" ), 
        0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputYearType_JComboBox = new SimpleJComboBox ( false );
    __OutputYearType_JComboBox.add ( "" );
    __OutputYearType_JComboBox.add ( "" + YearType.CALENDAR );
    __OutputYearType_JComboBox.add ( "" + YearType.NOV_TO_OCT );
    __OutputYearType_JComboBox.add ( "" + YearType.WATER );
    __OutputYearType_JComboBox.add ( "" + YearType.YEAR_MAY_TO_APR );
    __OutputYearType_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(analysis_JPanel, __OutputYearType_JComboBox,
        1, yAnalysis, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel (
        "Optional - to define year span (default=" + YearType.CALENDAR + ")."),
        3, yAnalysis, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Analysis start:" ),
        0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __AnalysisStart_JTextField = new JTextField ( "", 20 );
    __AnalysisStart_JTextField.setToolTipText("Specify the analysis start using a date/time string or ${Property} notation");
    __AnalysisStart_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(analysis_JPanel, __AnalysisStart_JTextField,
        1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel(
        "Optional - analysis start date/time (default=full time series period)."),
        3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Analysis end:" ), 
        0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __AnalysisEnd_JTextField = new JTextField ( "", 20 );
    __AnalysisEnd_JTextField.setToolTipText("Specify the analysis end using a date/time string or ${Property} notation");
    __AnalysisEnd_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(analysis_JPanel, __AnalysisEnd_JTextField,
        1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel(
        "Optional - analysis end date/time (default=full time series period)."),
        3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
	__AnalysisWindow_JCheckBox = new JCheckBox ( "Analysis window:", false );
	__AnalysisWindow_JCheckBox.addActionListener ( this );
    JGUIUtil.addComponent(analysis_JPanel, __AnalysisWindow_JCheckBox, 
        0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    JPanel analysisWindow_JPanel = new JPanel();
    analysisWindow_JPanel.setLayout(new GridBagLayout());
    __AnalysisWindowStart_JPanel = new DateTime_JPanel ( "Start", TimeInterval.MONTH, TimeInterval.HOUR, null );
    __AnalysisWindowStart_JPanel.addActionListener(this);
    __AnalysisWindowStart_JPanel.addKeyListener ( this );
    JGUIUtil.addComponent(analysisWindow_JPanel, __AnalysisWindowStart_JPanel,
        1, 0, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    // TODO SAM 2008-01-23 Figure out how to display the correct limits given the time series interval
    __AnalysisWindowEnd_JPanel = new DateTime_JPanel ( "End", TimeInterval.MONTH, TimeInterval.HOUR, null );
    __AnalysisWindowEnd_JPanel.addActionListener(this);
    __AnalysisWindowEnd_JPanel.addKeyListener ( this );
    JGUIUtil.addComponent(analysisWindow_JPanel, __AnalysisWindowEnd_JPanel,
        4, 0, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, analysisWindow_JPanel,
        1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel(
        "Optional - analysis window within input year (default=full year)."),
        3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(analysis_JPanel, new JLabel ("Search start:"),
        0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SearchStart_JTextField = new JTextField (10);
    __SearchStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(analysis_JPanel, __SearchStart_JTextField,
        1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel (
        "Optional - search start (needed for some statistics, default=full year)."),
        3, yAnalysis, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Panel for output
    int yOutput = -1;
    JPanel output_JPanel = new JPanel();
    output_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Output", output_JPanel );

    JGUIUtil.addComponent(output_JPanel, new JLabel (
		"The result of the analysis is a new year interval time series." ), 
		0, ++yOutput, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel (
		"It is recommended that new time series identifier (TSID)" +
		" information be specified for the output time series to avoid confusing the output with the original." ), 
		0, ++yOutput, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel (
		"An alias can also be defined to identify the output time series." ), 
		0, ++yOutput, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel (
		"If an ensemble was specified as input, a new ensemble can be created as output." ), 
		0, ++yOutput, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yOutput, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(output_JPanel, new JLabel("Alias to assign:"),
        0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Alias_JTextField = new TSFormatSpecifiersJPanel(15);
    __Alias_JTextField.getTextField().setToolTipText("Specify the alias using % specifiers, ${ts:property}, and ${Property} notation");
    __Alias_JTextField.addKeyListener ( this );
    __Alias_JTextField.getDocument().addDocumentListener(this);
    JGUIUtil.addComponent(output_JPanel, __Alias_JTextField,
        1, yOutput, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel ("Required - use %L for location, etc."),
        3, yOutput, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(output_JPanel, new JLabel ( "New time series ID:" ),
		0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__NewTSID_JTextArea = new JTextArea ( 3, 25 );
	__NewTSID_JTextArea.setToolTipText("Specify the new time series identifier, can use ${Property} notation");
	__NewTSID_JTextArea.setLineWrap ( true );
	__NewTSID_JTextArea.setWrapStyleWord ( true );
	__NewTSID_JTextArea.addKeyListener ( this );
	// Make 3-high to fit in the edit button...
    JGUIUtil.addComponent(output_JPanel, new JScrollPane(__NewTSID_JTextArea),
		1, yOutput, 2, 3, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel(
		"Recommended - to avoid confusion with TSID from original time series."), 
		3, yOutput, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    yOutput += 2;
    JGUIUtil.addComponent(output_JPanel, (__edit_JButton = new SimpleJButton ( "Edit", "Edit", this ) ),
		3, yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(output_JPanel, (__clear_JButton = new SimpleJButton ( "Clear", "Clear", this ) ),
		4, yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    __NewEnsembleID_JLabel = new JLabel ( "New ensemble ID:" );
    JGUIUtil.addComponent(output_JPanel, __NewEnsembleID_JLabel, 
        0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NewEnsembleID_JTextField = new JTextField ( "", 20 );
    __NewEnsembleID_JTextField.setToolTipText("Specify the new ensemble identifier, can use ${Property} notation");
    __NewEnsembleID_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(output_JPanel, __NewEnsembleID_JTextField,
        1, yOutput, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel( "Optional - to create ensemble when input is an ensemble."), 
        3, yOutput, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    __NewEnsembleName_JLabel = new JLabel ( "New ensemble name:" );
    JGUIUtil.addComponent(output_JPanel, __NewEnsembleName_JLabel,
        0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NewEnsembleName_JTextField = new JTextField ( "", 30 );
    __NewEnsembleName_JTextField.setToolTipText("Specify the new ensemble name, can use ${Property} notation");
    __NewEnsembleName_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(output_JPanel, __NewEnsembleName_JTextField,
        1, yOutput, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel( "Optional - name for new ensemble."), 
        3, yOutput, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

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
{	String routine = getClass().getSimpleName() + ".refresh";
	String Alias = "";
	String TSList = "";
    String TSID = "";
    String EnsembleID = "";
    String NewEnsembleID = "";
    String NewEnsembleName = "";
	String NewTSID = "";
	String Statistic = "";
	String TestValue = "";
	String Value1 = "";
	String AllowMissingCount = "";
	String MinimumSampleSize = "";
	String OutputYearType = "";
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
		TSList = props.getValue ( "TSList" );
        TSID = props.getValue ( "TSID" );
        EnsembleID = props.getValue ( "EnsembleID" );
        NewEnsembleID = props.getValue ( "NewEnsembleID" );
        NewEnsembleName = props.getValue ( "NewEnsembleName" );
		NewTSID = props.getValue ( "NewTSID" );
		Statistic = props.getValue ( "Statistic" );
		TestValue = props.getValue ( "TestValue" );
		Value1 = props.getValue ( "Value1" );
		AllowMissingCount = props.getValue ( "AllowMissingCount" );
		MinimumSampleSize = props.getValue ( "MinimumSampleSize" );
		OutputYearType = props.getValue ( "OutputYearType" );
		AnalysisStart = props.getValue ( "AnalysisStart" );
		AnalysisEnd = props.getValue ( "AnalysisEnd" );
		AnalysisWindowStart = props.getValue ( "AnalysisWindowStart" );
        AnalysisWindowEnd = props.getValue ( "AnalysisWindowEnd" );
        SearchStart = props.getValue ( "SearchStart" );
		if ( Alias != null ) {
			__Alias_JTextField.setText ( Alias );
		}
        if ( TSList == null ) {
            // Select default...
            __TSList_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __TSList_JComboBox,TSList, JGUIUtil.NONE, null, null ) ) {
                __TSList_JComboBox.select ( TSList );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nTSList value \"" + TSList +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if (    JGUIUtil.isSimpleJComboBoxItem( __TSID_JComboBox, TSID,
                JGUIUtil.NONE, null, null ) ) {
                __TSID_JComboBox.select ( TSID );
        }
        else {  // Automatically add to the list after the blank...
            if ( (TSID != null) && (TSID.length() > 0) ) {
                __TSID_JComboBox.insertItemAt ( TSID, 1 );
                // Select...
                __TSID_JComboBox.select ( TSID );
            }
            else {  // Select the blank...
                __TSID_JComboBox.select ( 0 );
            }
        }
        if ( EnsembleID == null ) {
            // Select default...
            __EnsembleID_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __EnsembleID_JComboBox,EnsembleID, JGUIUtil.NONE, null, null ) ) {
                __EnsembleID_JComboBox.select ( EnsembleID );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nEnsembleID value \"" + EnsembleID +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
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
			    Message.printWarning ( 1, routine,
				"Existing command references an invalid\nStatistic value \"" +
				Statistic +	"\".  Select a different value or Cancel.");
				__error_wait = true;
			}
		}
		if ( TestValue != null ) {
			__TestValue_JTextField.setText ( TestValue );
		}
		if ( Value1 != null ) {
			__Value1_JTextField.setText ( Value1 );
		}
		if ( AllowMissingCount != null ) {
			__AllowMissingCount_JTextField.setText ( AllowMissingCount );
		}
        if ( MinimumSampleSize != null ) {
            __MinimumSampleSize_JTextField.setText ( MinimumSampleSize );
        }
        if ( OutputYearType == null ) {
            // Select default...
            __OutputYearType_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __OutputYearType_JComboBox,OutputYearType, JGUIUtil.NONE, null, null ) ) {
                __OutputYearType_JComboBox.select ( OutputYearType );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nOutputYearType value \"" + OutputYearType +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
		if ( AnalysisStart != null ) {
			__AnalysisStart_JTextField.setText( AnalysisStart );
		}
		if ( AnalysisEnd != null ) {
			__AnalysisEnd_JTextField.setText ( AnalysisEnd );
		}
        if ( (AnalysisWindowStart != null) && (AnalysisWindowStart.length() > 0) ) {
            try {
                // Add year because it is not part of the parameter value...
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
                // Add year because it is not part of the parameter value...
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
        if ( NewEnsembleID != null ) {
            __NewEnsembleID_JTextField.setText ( NewEnsembleID );
        }
        if ( NewEnsembleName != null ) {
            __NewEnsembleName_JTextField.setText ( NewEnsembleName );
        }
	}
	// Regardless, reset the command from the fields...
	checkGUIState();
    TSList = __TSList_JComboBox.getSelected();
    TSID = __TSID_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
	Statistic = __Statistic_JComboBox.getSelected();
	TestValue = __TestValue_JTextField.getText();
	Value1 = __Value1_JTextField.getText();
	AllowMissingCount = __AllowMissingCount_JTextField.getText();
	MinimumSampleSize = __MinimumSampleSize_JTextField.getText();
	OutputYearType = __OutputYearType_JComboBox.getSelected();
	AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	SearchStart = __SearchStart_JTextField.getText().trim();
	Alias = __Alias_JTextField.getText().trim();
	NewTSID = __NewTSID_JTextArea.getText().trim();
	props = new PropList ( __command.getCommandName() );
    props.add ( "TSList=" + TSList );
    props.add ( "TSID=" + TSID );
    props.add ( "EnsembleID=" + EnsembleID );
	props.add ( "Statistic=" + Statistic );
	props.add ( "TestValue=" + TestValue );
	props.add ( "Value1=" + Value1 );
	props.add ( "AllowMissingCount=" + AllowMissingCount );
	props.add ( "MinimumSampleSize=" + MinimumSampleSize );
	props.add ( "OutputYearType=" + OutputYearType );
	props.add ( "AnalysisStart=" + AnalysisStart );
	props.add ( "AnalysisEnd=" + AnalysisEnd );
	if ( __AnalysisWindow_JCheckBox.isSelected() ) {
	    AnalysisWindowStart = __AnalysisWindowStart_JPanel.toString(false,true).trim();
	    AnalysisWindowEnd = __AnalysisWindowEnd_JPanel.toString(false,true).trim();
	    props.add ( "AnalysisWindowStart=" + AnalysisWindowStart );
	    props.add ( "AnalysisWindowEnd=" + AnalysisWindowEnd );
	}
	props.add ( "SearchStart=" + SearchStart );
	props.add ( "Alias=" + Alias );
	props.add ( "NewTSID=" + NewTSID );
    props.add ( "NewEnsembleID=" + NewEnsembleID );
    props.add ( "NewEnsembleName=" + NewEnsembleName );
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