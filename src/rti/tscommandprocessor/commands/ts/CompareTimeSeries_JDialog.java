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
import java.util.ArrayList;
import java.util.List;

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
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Editor dialog for CompareTimeSeries() command.
*/
@SuppressWarnings("serial")
public class CompareTimeSeries_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private JTabbedPane __main_JTabbedPane = null;
private SimpleJComboBox	__TSID1_JComboBox = null;
private SimpleJComboBox	__TSID2_JComboBox = null;
private SimpleJComboBox	__EnsembleID1_JComboBox = null;
private SimpleJComboBox	__EnsembleID2_JComboBox = null;
private SimpleJComboBox	__MatchLocation_JComboBox = null;
private SimpleJComboBox	__MatchDataType_JComboBox = null;
private JTextField __Precision_JTextField = null;
private JTextField __Tolerance_JTextField = null;
private JTextField __AnalysisStart_JTextField;
private JTextField __AnalysisEnd_JTextField;
private JTextField __DiffFlag_JTextField = null;
private SimpleJComboBox	__CreateDiffTS_JComboBox = null;
private SimpleJComboBox __TableID_JComboBox = null;
private JTextField __DiffCountProperty_JTextField = null;
private SimpleJComboBox	__WarnIfDifferent_JComboBox = null;
private SimpleJComboBox	__WarnIfSame_JComboBox = null;
private JTextArea __command_JTextArea = null;
private boolean __error_wait = false;
private boolean __first_time = true;
private CompareTimeSeries_Command __command = null;
private boolean __ok = false; // Indicates whether user has pressed OK to close the dialog.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices choices for TableID value.
*/
public CompareTimeSeries_JDialog ( JFrame parent, CompareTimeSeries_Command command, List<String> tableIDChoices )
{	super(parent, true);
	initialize ( parent, command, tableIDChoices );
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

//...End event handlers for DocumentListener

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
	PropList props = new PropList ( "" );
	String TSID1 = __TSID1_JComboBox.getSelected();
	String TSID2 = __TSID2_JComboBox.getSelected();
	String EnsembleID1 = __EnsembleID1_JComboBox.getSelected();
	String EnsembleID2 = __EnsembleID2_JComboBox.getSelected();
	String MatchLocation = __MatchLocation_JComboBox.getSelected();
	String MatchDataType = __MatchDataType_JComboBox.getSelected();
	String Precision = __Precision_JTextField.getText().trim();
	String Tolerance = __Tolerance_JTextField.getText().trim();
	String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	String DiffFlag = __DiffFlag_JTextField.getText().trim();
	String CreateDiffTS = __CreateDiffTS_JComboBox.getSelected();
	String TableID = __TableID_JComboBox.getSelected();
	String DiffCountProperty = __DiffCountProperty_JTextField.getText().trim();
	String WarnIfDifferent = __WarnIfDifferent_JComboBox.getSelected();
	String WarnIfSame = __WarnIfSame_JComboBox.getSelected();
	__error_wait = false;
	if ( TSID1.length() > 0 ) {
		props.set ( "TSID1", TSID1 );
	}
	if ( TSID2.length() > 0 ) {
		props.set ( "TSID2", TSID2 );
	}
	if ( EnsembleID1.length() > 0 ) {
		props.set ( "EnsembleID1", EnsembleID1 );
	}
	if ( EnsembleID2.length() > 0 ) {
		props.set ( "EnsembleID2", EnsembleID2 );
	}
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
    if ( TableID.length() > 0 ) {
    	props.set ( "TableID", TableID );
    }
    if ( DiffCountProperty.length() > 0 ) {
    	props.set ( "DiffCountProperty", DiffCountProperty );
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
{	String TSID1 = __TSID1_JComboBox.getSelected();
	String TSID2 = __TSID2_JComboBox.getSelected();
	String EnsembleID1 = __EnsembleID1_JComboBox.getSelected();
	String EnsembleID2 = __EnsembleID2_JComboBox.getSelected();
	String MatchLocation = __MatchLocation_JComboBox.getSelected();
	String MatchDataType = __MatchDataType_JComboBox.getSelected();
	String Precision = __Precision_JTextField.getText().trim();
	String Tolerance = __Tolerance_JTextField.getText().trim();
	String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	String DiffFlag = __DiffFlag_JTextField.getText().trim();
	String CreateDiffTS = __CreateDiffTS_JComboBox.getSelected();
    String TableID = __TableID_JComboBox.getSelected();
	String DiffCountProperty = __DiffCountProperty_JTextField.getText().trim();
	String WarnIfDifferent = __WarnIfDifferent_JComboBox.getSelected();
	String WarnIfSame = __WarnIfSame_JComboBox.getSelected();
	__command.setCommandParameter ( "TSID1", TSID1 );
	__command.setCommandParameter ( "TSID2", TSID2 );
	__command.setCommandParameter ( "EnsembleID1", EnsembleID1 );
	__command.setCommandParameter ( "EnsembleID2", EnsembleID2 );
	__command.setCommandParameter ( "MatchLocation", MatchLocation );
	__command.setCommandParameter ( "MatchDataType", MatchDataType );
	__command.setCommandParameter ( "Precision", Precision );
	__command.setCommandParameter ( "Tolerance", Tolerance );
	__command.setCommandParameter ( "AnalysisStart", AnalysisStart );
	__command.setCommandParameter ( "AnalysisEnd", AnalysisEnd );
	__command.setCommandParameter ( "DiffFlag", DiffFlag );
	__command.setCommandParameter ( "CreateDiffTS", CreateDiffTS );
    __command.setCommandParameter ( "TableID", TableID );
    __command.setCommandParameter ( "DiffCountProperty", DiffCountProperty );
	__command.setCommandParameter ( "WarnIfDifferent", WarnIfDifferent );
	__command.setCommandParameter ( "WarnIfSame", WarnIfSame );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices list of choices for table identifiers
*/
private void initialize ( JFrame parent, CompareTimeSeries_Command command, List<String> tableIDChoices )
{	__command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"This command compares time series, in particular to detect differences for matching pairs of time series." ),
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST); 
    
    __main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
     
    // Panel specifying two time series
    int yts2 = -1;
    JPanel ts2_JPanel = new JPanel();
    ts2_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Time Series (2)", ts2_JPanel );
    
    JGUIUtil.addComponent(ts2_JPanel, new JLabel (
		"Use these parameters to specify two time series to compare." ),
		0, ++yts2, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts2_JPanel, new JLabel (
	    "For example, compare two time series to validate software or a procedure." ),
	    0, ++yts2, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts2_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yts2, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(ts2_JPanel, new JLabel ( "First time series to compare:" ), 
		0, ++yts2, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TSID1_JComboBox = new SimpleJComboBox ( true ); // Allow edit
    __TSID1_JComboBox.setToolTipText("Specify the TSID for the first time series, can use ${Property}");
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    if ( tsids.size() == 0 ) {
    	tsids.add("");
    }
    else {
    	tsids.add(0, "");
    }
    __TSID1_JComboBox.setData ( tsids );
    __TSID1_JComboBox.addItemListener ( this );
    __TSID1_JComboBox.getJTextComponent().getDocument().addDocumentListener ( this );
    JGUIUtil.addComponent(ts2_JPanel, __TSID1_JComboBox,
        1, yts2, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(ts2_JPanel, new JLabel ( "Second time series compare:" ), 
		0, ++yts2, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TSID2_JComboBox = new SimpleJComboBox ( true ); // Allow edit
    __TSID2_JComboBox.setToolTipText("Specify the TSID for the second time series, can use ${Property}");
    __TSID2_JComboBox.setData ( tsids );
    __TSID2_JComboBox.addItemListener ( this );
    __TSID2_JComboBox.getJTextComponent().getDocument().addDocumentListener ( this );
    JGUIUtil.addComponent(ts2_JPanel, __TSID2_JComboBox,
        1, yts2, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // Panel specifying two ensembles
    int yEnsemble = -1;
    JPanel ensemble_JPanel = new JPanel();
    ensemble_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Ensembles (2)", ensemble_JPanel );
    
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel (
		"Use these parameters to specify two ensembles to compare." ),
		0, ++yEnsemble, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel (
	    "For example, compare two ensembles to validate software or a procedure." ),
	    0, ++yEnsemble, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel (
	    "The time series in the ensembles will be compared in sequence." ),
	    0, ++yEnsemble, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ensemble_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yEnsemble, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JLabel ensemble1Label = new JLabel ( "First ensemble to compare:" );
    JGUIUtil.addComponent(ensemble_JPanel, ensemble1Label, 
		0, ++yEnsemble, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __EnsembleID1_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __EnsembleID1_JComboBox.setToolTipText("Select an ensemble identifier from the list or specify with ${Property} notation");
    List<String> ensembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    ensembleIDs.add(0,""); // Always add default
    __EnsembleID1_JComboBox.setData ( ensembleIDs );
    __EnsembleID1_JComboBox.addItemListener ( this );
    __EnsembleID1_JComboBox.getJTextComponent().getDocument().addDocumentListener ( this );
    JGUIUtil.addComponent(ensemble_JPanel, __EnsembleID1_JComboBox,
        1, yEnsemble, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JLabel ensemble2Label = new JLabel ( "Second ensemble to compare:" );
    JGUIUtil.addComponent(ensemble_JPanel, ensemble2Label, 
		0, ++yEnsemble, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __EnsembleID2_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __EnsembleID2_JComboBox.setToolTipText("Select an ensemble identifier from the list or specify with ${Property} notation");
    __EnsembleID2_JComboBox.setData ( ensembleIDs );
    __EnsembleID2_JComboBox.addItemListener ( this );
    __EnsembleID2_JComboBox.getJTextComponent().getDocument().addDocumentListener ( this );
    JGUIUtil.addComponent(ensemble_JPanel, __EnsembleID2_JComboBox,
        1, yEnsemble, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // Panel specifying many time series
    int yts = -1;
    JPanel ts_JPanel = new JPanel();
    ts_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Time Series (many)", ts_JPanel );
    
    JGUIUtil.addComponent(ts_JPanel, new JLabel (
		"Use these parameters to specify information to pair time series from the full time series list." ),
		0, ++yts, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel (
	    "For example, compare time series from from two model runs to determine changes." ),
	    0, ++yts, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel (
		"Currently all available time series are evaluated, comparing time series that have the same time series identifier location and/or data type." ),
		0, ++yts, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yts, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Match location:"),
		0, ++yts, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__MatchLocation_JComboBox = new SimpleJComboBox ( false );
	List<String> matchChoices = new ArrayList<String>();
	matchChoices.add ( "" );	// Default
	matchChoices.add ( __command._False );
	matchChoices.add ( __command._True );
	__MatchLocation_JComboBox.setData(matchChoices);
	__MatchLocation_JComboBox.select ( 0 );
	__MatchLocation_JComboBox.addActionListener ( this );
        JGUIUtil.addComponent(ts_JPanel, __MatchLocation_JComboBox,
		1, yts, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(ts_JPanel, new JLabel(
		"Optional - match location to find time series pair? (default=" + __command._True + ")."), 
		3, yts, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Match data type:"),
		0, ++yts, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__MatchDataType_JComboBox = new SimpleJComboBox ( false );
	List<String> matchDataTypeChoices = new ArrayList<String>();
	matchDataTypeChoices.add ( "" );	// Default
	matchDataTypeChoices.add ( __command._False );
	matchDataTypeChoices.add ( __command._True );
	__MatchDataType_JComboBox.setData(matchDataTypeChoices);
	__MatchDataType_JComboBox.select ( 0 );
	__MatchDataType_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(ts_JPanel, __MatchDataType_JComboBox,
		1, yts, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel(
		"Optional - match data type to find time series pair? (default=" + __command._False + ")."), 
		3, yts, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Panel specifying analysis parameters
    int yAnalysis = -1;
    JPanel analysis_JPanel = new JPanel();
    analysis_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Analysis", analysis_JPanel );
    
    JGUIUtil.addComponent(analysis_JPanel, new JLabel (
		"Specify one or more tolerances, separated by commas.  Differences greater than these values will be noted." ),
		0, ++yAnalysis, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yAnalysis, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);    

    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Precision:" ), 
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Precision_JTextField = new JTextField ( 5 );
	__Precision_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(analysis_JPanel, __Precision_JTextField,
		1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel(
		"Optional - digits after decimal to compare (default=available digits are used)."), 
		3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Tolerance:" ), 
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Tolerance_JTextField = new JTextField ( 15 );
	__Tolerance_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(analysis_JPanel, __Tolerance_JTextField,
		1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel(
		"Optional - tolerance(s) to indicate difference (e.g., .01, .1, default=exact comparison)."), 
		3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

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
	
    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Difference flag:" ), 
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__DiffFlag_JTextField = new JTextField ( 15 );
	__DiffFlag_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(analysis_JPanel, __DiffFlag_JTextField,
		1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel(
		"Optional - 1-character flag to use for values that are different."),
		3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Warn if different?:"),
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__WarnIfDifferent_JComboBox = new SimpleJComboBox ( false );
	List<String> diffChoices = new ArrayList<String>();
	diffChoices.add ( "" );	// Default
	diffChoices.add ( __command._False );
	diffChoices.add ( __command._True );
	__WarnIfDifferent_JComboBox.setData(diffChoices);
	__WarnIfDifferent_JComboBox.select ( 0 );
	__WarnIfDifferent_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(analysis_JPanel, __WarnIfDifferent_JComboBox,
		1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel(
		"Optional - generate a warning if different? (default=" + __command._False + ")."), 
		3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Warn if same?:"),
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__WarnIfSame_JComboBox = new SimpleJComboBox ( false );
	List<String> sameChoices = new ArrayList<String>();
	sameChoices.add ( "" );	// Default
	sameChoices.add ( __command._False );
	sameChoices.add ( __command._True );
	__WarnIfSame_JComboBox.setData(sameChoices);
	__WarnIfSame_JComboBox.select ( 0 );
	__WarnIfSame_JComboBox.addActionListener ( this );
	    JGUIUtil.addComponent(analysis_JPanel, __WarnIfSame_JComboBox,
		1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel(
		"Optional - generate a warning if same? (default=" + __command._False + ")."), 
		3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Panel specifying output parameters
    int yOut = -1;
    JPanel out_JPanel = new JPanel();
    out_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Output", out_JPanel );

    JGUIUtil.addComponent(out_JPanel, new JLabel (
		"Indicate whether output time series should be created indicating the difference between time series." ),
		0, ++yOut, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(out_JPanel, new JLabel (
		"An output table containing time series differences can also be created (or appended to)." ),
		0, ++yOut, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(out_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yOut, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST); 
    
    JGUIUtil.addComponent(out_JPanel, new JLabel ( "Create difference time series?:"),
		0, ++yOut, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
  	__CreateDiffTS_JComboBox = new SimpleJComboBox ( false );
  	List<String> createChoices = new ArrayList<String>();
  	createChoices.add ( "" );	// Default
  	createChoices.add ( __command._False );
  	createChoices.add ( __command._True );
  	__CreateDiffTS_JComboBox.setData(createChoices);
   	__CreateDiffTS_JComboBox.select ( 0 );
   	__CreateDiffTS_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(out_JPanel, __CreateDiffTS_JComboBox,
    		1, yOut, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(out_JPanel, new JLabel(
		"Optional - create a time series TS2 - TS1? (default=" + __command._False + ")."), 
		3, yOut, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(out_JPanel, new JLabel ( "Table ID:" ), 
        0, ++yOut, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableID_JComboBox = new SimpleJComboBox ( 12, true ); // Allow edit
    __TableID_JComboBox.setToolTipText("Specify the table ID for comparison output or use ${Property} notation");
    tableIDChoices.add(0,""); // Add blank to ignore table
    __TableID_JComboBox.setData ( tableIDChoices );
    __TableID_JComboBox.addItemListener ( this );
    //__TableID_JComboBox.setMaximumRowCount(tableIDChoices.size());
    JGUIUtil.addComponent(out_JPanel, __TableID_JComboBox,
        1, yOut, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(out_JPanel, new JLabel( "Optional - table to receive difference output."), 
        3, yOut, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(out_JPanel, new JLabel ( "Difference count property:" ), 
        0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DiffCountProperty_JTextField = new JTextField ( "", 20 );
    __DiffCountProperty_JTextField.setToolTipText("Property name to set difference count (use to check non-zero count).");
    __DiffCountProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(out_JPanel, __DiffCountProperty_JTextField,
        1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(out_JPanel, new JLabel(
        "Optional - property to set to difference count (default=don't set)."),
        3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

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
Refresh the command from the other text field contents.
*/
private void refresh ()
{	String routine = getClass().getSimpleName() + ".refresh";
	String TSID1 = "";
	String TSID2 = "";
	String EnsembleID1 = "";
	String EnsembleID2 = "";
	String MatchLocation = "";
	String MatchDataType = "";
	String Precision = "";
	String Tolerance = "";
	String AnalysisStart = "";
	String AnalysisEnd = "";
	String DiffFlag = "";
	String CreateDiffTS = "";
	String TableID = "";
	String DiffCountProperty = "";
	String WarnIfDifferent = "";
	String WarnIfSame = "";
	PropList props = __command.getCommandParameters();
	if ( __first_time ) {
		__first_time = false;
		TSID1 = props.getValue ( "TSID1" );
		TSID2 = props.getValue ( "TSID2" );
		EnsembleID1 = props.getValue ( "EnsembleID1" );
		EnsembleID2 = props.getValue ( "EnsembleID2" );
		MatchLocation = props.getValue ( "MatchLocation" );
		MatchDataType = props.getValue ( "MatchDataType" );
		Precision = props.getValue ( "Precision" );
		Tolerance = props.getValue ( "Tolerance" );
		AnalysisStart = props.getValue("AnalysisStart");
		AnalysisEnd = props.getValue("AnalysisEnd");
		DiffFlag = props.getValue ( "DiffFlag" );
		CreateDiffTS = props.getValue ( "CreateDiffTS" );
        TableID = props.getValue ( "TableID" );
        DiffCountProperty = props.getValue ( "DiffCountProperty" );
		WarnIfDifferent = props.getValue ( "WarnIfDifferent" );
		WarnIfSame = props.getValue ( "WarnIfSame" );
        // Select the item in the list.  If not a match, print a warning.
        if ( JGUIUtil.isSimpleJComboBoxItem( __TSID1_JComboBox, TSID1, JGUIUtil.NONE, null, null ) ) {
            __TSID1_JComboBox.select ( TSID1 );
        }
        else {
            // Automatically add to the list...
            if ( (TSID1 != null) && (TSID1.length() > 0) ) {
                __TSID1_JComboBox.insertItemAt ( TSID1, 0 );
                // Select...
                __TSID1_JComboBox.select ( TSID1 );
            }
            else {
                // Select the first choice...
                if ( __TSID1_JComboBox.getItemCount() > 0 ) {
                    __TSID1_JComboBox.select ( 0 );
                }
            }
        }
        // Select the item in the list.  If not a match, print a warning.
        if ( JGUIUtil.isSimpleJComboBoxItem( __TSID2_JComboBox, TSID2, JGUIUtil.NONE, null, null ) ) {
            __TSID2_JComboBox.select ( TSID2 );
        }
        else {
            // Automatically add to the list...
            if ( (TSID2 != null) && (TSID2.length() > 0) ) {
                __TSID2_JComboBox.insertItemAt ( TSID2, 0 );
                // Select...
                __TSID2_JComboBox.select ( TSID2 );
            }
            else {
                // Select the first choice...
                if ( __TSID2_JComboBox.getItemCount() > 0 ) {
                    __TSID2_JComboBox.select ( 0 );
                }
            }
        }
        if ( EnsembleID1 == null ) {
            // Select default...
            __EnsembleID1_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __EnsembleID1_JComboBox, EnsembleID1, JGUIUtil.NONE, null, null ) ) {
                __EnsembleID1_JComboBox.select ( EnsembleID1 );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nEnsembleID1 value \"" + EnsembleID1 +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( EnsembleID2 == null ) {
            // Select default...
            __EnsembleID2_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __EnsembleID2_JComboBox, EnsembleID2, JGUIUtil.NONE, null, null ) ) {
                __EnsembleID2_JComboBox.select ( EnsembleID2 );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nEnsembleID2 value \"" + EnsembleID2 +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
		if ( JGUIUtil.isSimpleJComboBoxItem(__MatchLocation_JComboBox, MatchLocation,JGUIUtil.NONE, null, null ) ) {
			__MatchLocation_JComboBox.select ( MatchLocation );
		}
		else {
		    if ( (MatchLocation == null) || MatchLocation.equals("") ) {
				// New command...select the default...
				__MatchLocation_JComboBox.select ( 0 );
			}
			else {
			    // Bad user command...
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"MatchLocation parameter \"" + MatchLocation +
				"\".  Select a\ndifferent value or Cancel." );
			}
		}
		if ( JGUIUtil.isSimpleJComboBoxItem( __MatchDataType_JComboBox, MatchDataType, JGUIUtil.NONE, null, null ) ) {
			__MatchDataType_JComboBox.select ( MatchDataType );
		}
		else {
		    if ( (MatchDataType == null) || MatchDataType.equals("") ) {
				// New command...select the default...
				__MatchDataType_JComboBox.select ( 0 );
			}
			else {
			    // Bad user command...
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
		if ( JGUIUtil.isSimpleJComboBoxItem(__CreateDiffTS_JComboBox, CreateDiffTS, JGUIUtil.NONE, null, null ) ) {
				__CreateDiffTS_JComboBox.select ( CreateDiffTS );
		}
		else {
		    if ( (CreateDiffTS == null) || CreateDiffTS.equals("") ) {
				// New command...select the default...
				__CreateDiffTS_JComboBox.select ( 0 );
			}
			else {
			    // Bad user command...
				Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
				"CreateDiffTS parameter \"" + CreateDiffTS +
				"\".  Select a\ndifferent value or Cancel." );
			}
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
            	// OK to add to list since does not need to exist
            	__TableID_JComboBox.add(TableID);
            	__TableID_JComboBox.select(TableID);
                //Message.printWarning ( 1, routine,
                //"Existing command references an invalid\nTableID value \"" + TableID +
                //"\".  Select a different value or Cancel.");
                //__error_wait = true;
            }
        }
		if ( DiffCountProperty != null ) {
			__DiffCountProperty_JTextField.setText ( DiffCountProperty );
		}
		if ( JGUIUtil.isSimpleJComboBoxItem(__WarnIfDifferent_JComboBox, WarnIfDifferent, JGUIUtil.NONE, null, null ) ) {
			__WarnIfDifferent_JComboBox.select ( WarnIfDifferent );
		}
		else {
		    if ( (WarnIfDifferent == null) || WarnIfDifferent.equals("") ) {
				// New command...select the default...
				__WarnIfDifferent_JComboBox.select ( 0 );
			}
			else {
			    // Bad user command...
				Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
				"WarnIfDifferent parameter \"" +
				WarnIfDifferent + "\".  Select a\ndifferent value or Cancel." );
			}
		}
		if ( JGUIUtil.isSimpleJComboBoxItem(__WarnIfSame_JComboBox, WarnIfSame, JGUIUtil.NONE, null, null ) ) {
			__WarnIfSame_JComboBox.select ( WarnIfSame );
		}
		else {
		    if ( (WarnIfSame == null) || WarnIfSame.equals("") ) {
				// New command...select the default...
				__WarnIfSame_JComboBox.select ( 0 );
			}
			else {
			    // Bad user command...
				Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
				"WarnIfSame parameter \"" + WarnIfSame + "\".  Select a\ndifferent value or Cancel." );
			}
		}
	}
	// Regardless, reset the command from the fields.  This is only  visible
	// information that has not been committed in the command.
	TSID1 = __TSID1_JComboBox.getSelected();
	TSID2 = __TSID2_JComboBox.getSelected();
	EnsembleID1 = __EnsembleID1_JComboBox.getSelected();
	EnsembleID2 = __EnsembleID2_JComboBox.getSelected();
	MatchLocation = __MatchLocation_JComboBox.getSelected();
	MatchDataType = __MatchDataType_JComboBox.getSelected();
	Precision = __Precision_JTextField.getText().trim();
	Tolerance = __Tolerance_JTextField.getText().trim();
	AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	DiffFlag = __DiffFlag_JTextField.getText().trim();
	CreateDiffTS = __CreateDiffTS_JComboBox.getSelected();
    TableID = __TableID_JComboBox.getSelected();
    DiffCountProperty = __DiffCountProperty_JTextField.getText().trim();
	WarnIfDifferent = __WarnIfDifferent_JComboBox.getSelected();
	WarnIfSame = __WarnIfSame_JComboBox.getSelected();
	props = new PropList ( __command.getCommandName() );
	props.add ( "TSID1=" + TSID1 );
	props.add ( "TSID2=" + TSID2 );
	props.add ( "EnsembleID1=" + EnsembleID1 );
	props.add ( "EnsembleID2=" + EnsembleID2 );
	props.add ( "MatchLocation=" + MatchLocation );
	props.add ( "MatchDataType=" + MatchDataType );
	props.add ( "Precision=" + Precision );
	props.add ( "Tolerance=" + Tolerance );
	props.add ( "AnalysisStart=" + AnalysisStart );
	props.add ( "AnalysisEnd=" + AnalysisEnd );
	props.add ( "DiffFlag=" + DiffFlag );
	props.add ( "CreateDiffTS=" + CreateDiffTS );
    props.add ( "TableID=" + TableID );
    props.add ( "DiffCountProperty=" + DiffCountProperty );
	props.add ( "WarnIfDifferent=" + WarnIfDifferent );
	props.add ( "WarnIfSame=" + WarnIfSame );
	__command_JTextArea.setText( __command.toString(props) );
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed
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

}