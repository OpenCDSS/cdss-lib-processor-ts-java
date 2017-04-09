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

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;
import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.TS.TSStatisticType;
import RTi.TS.TSUtil_CalculateTimeSeriesStatistic;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.DateTime_JPanel;
import RTi.Util.Time.TimeInterval;

@SuppressWarnings("serial")
public class CalculateTimeSeriesStatistic_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private CalculateTimeSeriesStatistic_Command __command = null;
private JTabbedPane __main_JTabbedPane = null;
private JTextArea __command_JTextArea=null;
private SimpleJComboBox __TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox	__TSID_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private SimpleJComboBox __Statistic_JComboBox = null;
private JTextField __Value1_JTextField = null;
private JTextField __Value2_JTextField = null;
private JTextField __Value3_JTextField = null;
private JTextField __AnalysisStart_JTextField = null;
private JTextField __AnalysisEnd_JTextField = null;
private JCheckBox __AnalysisWindow_JCheckBox = null;
private DateTime_JPanel __AnalysisWindowStart_JPanel = null; // Fields for analysis window within a year
private DateTime_JPanel __AnalysisWindowEnd_JPanel = null;
private SimpleJComboBox __TableID_JComboBox = null;
private JTextField __TableTSIDColumn_JTextField = null;
private TSFormatSpecifiersJPanel __TableTSIDFormat_JTextField = null; // Format for time series identifiers
private JTextField __TableStatisticColumn_JTextField = null;
private JTextField __TableStatisticDateTimeColumn_JTextField = null;
private JTextField __TimeSeriesProperty_JTextField = null;
private JTextField __StatisticValueProperty_JTextField = null;
private boolean __error_wait = false; // Is there an error to be cleared up or Cancel?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK button has been pressed.

/**
Command dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices choices for TableID value.
*/
public CalculateTimeSeriesStatistic_JDialog ( JFrame parent, CalculateTimeSeriesStatistic_Command command,
    List<String> tableIDChoices )
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
	else {
        checkGUIState();
        refresh ();
	}
}

/**
Check the GUI state to make sure that appropriate components are enabled/disabled.
*/
private void checkGUIState ()
{
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
        __EnsembleID_JComboBox.setEnabled(true);
        __EnsembleID_JLabel.setEnabled ( true );
    }
    else {
        __EnsembleID_JComboBox.setEnabled(false);
        __EnsembleID_JLabel.setEnabled ( false );
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
    
    // Set the tooltips on the Value input fields to help users know what to enter
    __Value1_JTextField.setToolTipText("");
    __Value2_JTextField.setToolTipText("");
    __Value3_JTextField.setToolTipText("");
    String Statistic = __Statistic_JComboBox.getSelected();
    TSStatisticType stat = TSStatisticType.valueOfIgnoreCase(Statistic);
    if ( stat != null ) {
    	if ( stat == TSStatisticType.GE_COUNT ) {
    		__Value1_JTextField.setToolTipText("Value that is compared to determine count >= the value");
    	}
    	else if ( stat == TSStatisticType.GT_COUNT ) {
    		__Value1_JTextField.setToolTipText("Value that is compared to determine count > the value");
    	}
    	else if ( stat == TSStatisticType.LE_COUNT ) {
    		__Value1_JTextField.setToolTipText("Value that is compared to determine count <= the value");
    	}
    	else if ( stat == TSStatisticType.LT_COUNT ) {
    		__Value1_JTextField.setToolTipText("Value that is compared to determine count < the value");
    	}
    	else if ( stat == TSStatisticType.NQYY ) {
    		__Value1_JTextField.setToolTipText("N = number of days centered on date/time to average, must be odd number");
    		__Value2_JTextField.setToolTipText("Q = the return interval, e.g., 10 for 1 year in 10 return interval");
    		__Value3_JTextField.setToolTipText("Number of missing values allowed to compute average (default is 0)");
    	}
    }
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
	PropList parameters = new PropList ( "" );
    String TSList = __TSList_JComboBox.getSelected();
	String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String Statistic = __Statistic_JComboBox.getSelected();
	String Value1 = __Value1_JTextField.getText().trim();
	String Value2 = __Value2_JTextField.getText().trim();
	String Value3 = __Value3_JTextField.getText().trim();
	String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	String TableID = __TableID_JComboBox.getSelected();
	String TableTSIDColumn = __TableTSIDColumn_JTextField.getText().trim();
	String TableTSIDFormat = __TableTSIDFormat_JTextField.getText().trim();
	String TableStatisticColumn = __TableStatisticColumn_JTextField.getText().trim();
	String TableStatisticDateTimeColumn = __TableStatisticDateTimeColumn_JTextField.getText().trim();
	String TimeSeriesProperty = __TimeSeriesProperty_JTextField.getText().trim();
	String StatisticValueProperty = __StatisticValueProperty_JTextField.getText().trim();
	__error_wait = false;

    if ( TSList.length() > 0 ) {
        parameters.set ( "TSList", TSList );
    }
	if ( TSID.length() > 0 ) {
		parameters.set ( "TSID", TSID );
	}
    if ( EnsembleID.length() > 0 ) {
        parameters.set ( "EnsembleID", EnsembleID );
    }
    if ( Statistic.length() > 0 ) {
        parameters.set ( "Statistic", Statistic );
    }
	if ( Value1.length() > 0 ) {
		parameters.set ( "Value1", Value1 );
	}
    if ( Value2.length() > 0 ) {
        parameters.set ( "Value2", Value2 );
    }
    if ( Value3.length() > 0 ) {
        parameters.set ( "Value3", Value3 );
    }
	if ( AnalysisStart.length() > 0 ) {
		parameters.set ( "AnalysisStart", AnalysisStart );
	}
	if ( AnalysisEnd.length() > 0 ) {
		parameters.set ( "AnalysisEnd", AnalysisEnd );
	}
    if ( __AnalysisWindow_JCheckBox.isSelected() ){
        String AnalysisWindowStart = __AnalysisWindowStart_JPanel.toString(false,true).trim();
        String AnalysisWindowEnd = __AnalysisWindowEnd_JPanel.toString(false,true).trim();
        // 99 is used for month if not specified - don't want that in the parameters
        if ( AnalysisWindowStart.startsWith("99") ) {
        	AnalysisWindowStart = "";
        }
        if ( AnalysisWindowEnd.startsWith("99") ) {
        	AnalysisWindowEnd = "";
        }
        if ( AnalysisWindowStart.length() > 0 ) {
            parameters.set ( "AnalysisWindowStart", AnalysisWindowStart );
        }
        if ( AnalysisWindowEnd.length() > 0 ) {
            parameters.set ( "AnalysisWindowEnd", AnalysisWindowEnd );
        }
    }
    if ( TableID.length() > 0 ) {
        parameters.set ( "TableID", TableID );
    }
    if ( TableTSIDColumn.length() > 0 ) {
        parameters.set ( "TableTSIDColumn", TableTSIDColumn );
    }
    if ( TableTSIDFormat.length() > 0 ) {
        parameters.set ( "TableTSIDFormat", TableTSIDFormat );
    }
    if ( TableStatisticColumn.length() > 0 ) {
        parameters.set ( "TableStatisticColumn", TableStatisticColumn );
    }
    if ( TableStatisticDateTimeColumn.length() > 0 ) {
        parameters.set ( "TableStatisticDateTimeColumn", TableStatisticDateTimeColumn );
    }
    if ( TimeSeriesProperty.length() > 0 ) {
        parameters.set ( "TimeSeriesProperty", TimeSeriesProperty );
    }
    if ( StatisticValueProperty.length() > 0 ) {
        parameters.set ( "StatisticValueProperty", StatisticValueProperty );
    }
	try {
	    // This will warn the user...
		__command.checkCommandParameters ( parameters, null, 1 );
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
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String Statistic = __Statistic_JComboBox.getSelected();
	String Value1 = __Value1_JTextField.getText().trim();
	String Value2 = __Value2_JTextField.getText().trim();
	String Value3 = __Value3_JTextField.getText().trim();
	String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
    String TableID = __TableID_JComboBox.getSelected();
    String TableTSIDColumn = __TableTSIDColumn_JTextField.getText().trim();
    String TableTSIDFormat = __TableTSIDFormat_JTextField.getText().trim();
    String TableStatisticColumn = __TableStatisticColumn_JTextField.getText().trim();
    String TableStatisticDateTimeColumn = __TableStatisticDateTimeColumn_JTextField.getText().trim();
    String TimeSeriesProperty = __TimeSeriesProperty_JTextField.getText().trim();
	String StatisticValueProperty = __StatisticValueProperty_JTextField.getText().trim();
    __command.setCommandParameter ( "TSList", TSList );
	__command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
    __command.setCommandParameter ( "Statistic", Statistic );
	__command.setCommandParameter ( "Value1", Value1 );
	__command.setCommandParameter ( "Value2", Value2 );
	__command.setCommandParameter ( "Value3", Value3 );
	__command.setCommandParameter ( "AnalysisStart", AnalysisStart );
	__command.setCommandParameter ( "AnalysisEnd", AnalysisEnd );
    if ( __AnalysisWindow_JCheckBox.isSelected() ){
        String AnalysisWindowStart = __AnalysisWindowStart_JPanel.toString(false,true).trim();
        String AnalysisWindowEnd = __AnalysisWindowEnd_JPanel.toString(false,true).trim();
        if ( AnalysisWindowStart.startsWith("99") ) {
        	AnalysisWindowStart = "";
        }
        if ( AnalysisWindowEnd.startsWith("99") ) {
        	AnalysisWindowEnd = "";
        }
        __command.setCommandParameter ( "AnalysisWindowStart", AnalysisWindowStart );
        __command.setCommandParameter ( "AnalysisWindowEnd", AnalysisWindowEnd );
    }
    else {
    	// Clear the properties because they may have been set during editing but should not be propagated
    	__command.getCommandParameters().unSet ( "AnalysisWindowStart" );
    	__command.getCommandParameters().unSet ( "AnalysisWindowEnd" );
    }
	__command.setCommandParameter ( "TableID", TableID );
    __command.setCommandParameter ( "TableTSIDColumn", TableTSIDColumn );
    __command.setCommandParameter ( "TableTSIDFormat", TableTSIDFormat );
    __command.setCommandParameter ( "TableStatisticColumn", TableStatisticColumn );
    __command.setCommandParameter ( "TableStatisticDateTimeColumn", TableStatisticDateTimeColumn );
    __command.setCommandParameter ( "TimeSeriesProperty", TimeSeriesProperty );
    __command.setCommandParameter ( "StatisticValueProperty", StatisticValueProperty );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command The command to edit.
@param tableIDChoices list of choices for table identifiers
*/
private void initialize ( JFrame parent, CalculateTimeSeriesStatistic_Command command, List<String> tableIDChoices )
{	__command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Calculate a statistic for time series and optionally save in a table and/or time series property." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "An example of a statistic is the count of missing values." ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    __main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
     
    // Panel for input
    int yInput = 0;
    JPanel input_JPanel = new JPanel();
    input_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Input", input_JPanel );
    
    __TSList_JComboBox = new SimpleJComboBox(false);
    yInput = CommandEditorUtil.addTSListToEditorDialogPanel ( this, input_JPanel, __TSList_JComboBox, yInput );

    __TSID_JLabel = new JLabel ("TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __TSID_JComboBox = new SimpleJComboBox ( true );  // Allow edits
    __TSID_JComboBox.setToolTipText("Select a time series TSID/alias from the list or specify with ${Property} notation");
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    yInput = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, input_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, yInput );
    
    __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __EnsembleID_JComboBox.setToolTipText("Select an ensemble identifier from the list or specify with ${Property} notation");
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    yInput = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
        this, this, input_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, yInput );
    
    // Panel for analysis
    int yAnalysis = -1;
    JPanel analysis_JPanel = new JPanel();
    analysis_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Analysis", analysis_JPanel );

    JGUIUtil.addComponent(analysis_JPanel, new JLabel (
        "Statistics results may include 1+ values and may include the date/time of the result." ), 
        0, ++yAnalysis, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel (
        "Mouse over Value1, Value2, and Value3 fields for help understanding input - help will be blank if value is not used for a statistic." ), 
        0, ++yAnalysis, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel (
		"Specify dates with precision appropriate for the data, " +
		"use blank for all available data, OutputStart, or OutputEnd."),
		0, ++yAnalysis, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++yAnalysis, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Statistic to calculate:" ), 
        0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Statistic_JComboBox = new SimpleJComboBox ( 12, false ); // Do not allow edit
    __Statistic_JComboBox.setData ( TSUtil_CalculateTimeSeriesStatistic.getStatisticChoicesAsStrings() );
    __Statistic_JComboBox.addItemListener ( this );
    //__Statistic_JComboBox.setMaximumRowCount(statisticChoices.size());
    JGUIUtil.addComponent(analysis_JPanel, __Statistic_JComboBox,
        1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel(
        "Required - may require other parameters."), 
        3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Value1:" ), 
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Value1_JTextField = new JTextField ( 10 );
	__Value1_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(analysis_JPanel, __Value1_JTextField,
		1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel(
		"Optional - may be needed as input to calculate statistic."), 
		3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Value2:" ), 
        0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Value2_JTextField = new JTextField ( 10 );
    __Value2_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(analysis_JPanel, __Value2_JTextField,
        1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel(
        "Optional - may be needed as input to calculate statistic."), 
        3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Value3:" ), 
        0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Value3_JTextField = new JTextField ( 10 );
    __Value3_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(analysis_JPanel, __Value3_JTextField,
        1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel(
        "Optional - may be needed as input to calculate statistic."), 
        3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Analysis start:" ),
        0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __AnalysisStart_JTextField = new JTextField ( "", 20 );
    __AnalysisStart_JTextField.setToolTipText("Specify the analysis start using a date/time string or ${Property} notation");
    __AnalysisStart_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(analysis_JPanel, __AnalysisStart_JTextField,
        1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel(
        "Optional - analysis start date/time, or ${Property} (default=full time series period)."),
        3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Analysis end:" ), 
        0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __AnalysisEnd_JTextField = new JTextField ( "", 20 );
    __AnalysisEnd_JTextField.setToolTipText("Specify the analysis end using a date/time string or ${Property} notation");
    __AnalysisEnd_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(analysis_JPanel, __AnalysisEnd_JTextField,
        1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel(
        "Optional - analysis end date/time, or ${Property} (default=full time series period)."),
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
    JGUIUtil.addComponent(analysis_JPanel, new JLabel(""),
        3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // Panel for table
    int yTable = -1;
    JPanel table_JPanel = new JPanel();
    table_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Output Table", table_JPanel );
    
	JGUIUtil.addComponent(table_JPanel, new JLabel (
       "Statistics results can be saved to a table. The table and its columns will be created if not found." ), 
       0, ++yTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(table_JPanel, new JLabel (
        "The TSID column is used to match a row in the table to receive output statistics, which are written to columns." ), 
        0, ++yTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(table_JPanel, new JLabel (
        "The date/time corresponding to a statistic can be output for some statististics (e.g., Last, Max)." ), 
        0, ++yTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(table_JPanel, new JLabel (
	    "Use table commands to save the table results to a file." ), 
	    0, ++yTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++yTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Table ID for output:" ), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableID_JComboBox = new SimpleJComboBox ( 12, true ); // Allow edit
    __TableID_JComboBox.setToolTipText("Specify the table ID for statistic output or use ${Property} notation");
    tableIDChoices.add(0,""); // Add blank to ignore table
    __TableID_JComboBox.setData ( tableIDChoices );
    __TableID_JComboBox.addItemListener ( this );
    //__TableID_JComboBox.setMaximumRowCount(tableIDChoices.size());
    JGUIUtil.addComponent(table_JPanel, __TableID_JComboBox,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel(
        "Optional - if statistic should be saved in table."), 
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Table TSID column:" ), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableTSIDColumn_JTextField = new JTextField ( 20 );
    __TableTSIDColumn_JTextField.setToolTipText("Specify the table TSID column or use ${Property} notation");
    __TableTSIDColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __TableTSIDColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel( "Required if using table - column name for TSID."), 
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(table_JPanel, new JLabel("Format of TSID:"),
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableTSIDFormat_JTextField = new TSFormatSpecifiersJPanel(10);
    __TableTSIDFormat_JTextField.setToolTipText("Use %L for location, %T for data type, %I for interval.");
    __TableTSIDFormat_JTextField.addKeyListener ( this );
    __TableTSIDFormat_JTextField.setToolTipText("%L for location, %T for data type.");
    JGUIUtil.addComponent(table_JPanel, __TableTSIDFormat_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Optional - use %L for location, etc. (default=alias or TSID)."),
        3, yTable, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Table statistic column:" ), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableStatisticColumn_JTextField = new JTextField ( 20 );
    __TableStatisticColumn_JTextField.setToolTipText("Specify the table statistic column or use ${Property} notation");
    __TableStatisticColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __TableStatisticColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel(
        "Required if using table - column name(s) for statistic(s)."), 
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Table statistic date/time column:" ), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableStatisticDateTimeColumn_JTextField = new JTextField ( 20 );
    __TableStatisticDateTimeColumn_JTextField.setToolTipText("Specify the table statistic date/time column or use ${Property} notation");
    __TableStatisticDateTimeColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __TableStatisticDateTimeColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel(
        "Optional - column name for statistic date/time."), 
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Panel for property
    int yProp = -1;
    JPanel prop_JPanel = new JPanel();
    prop_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Output Property", prop_JPanel );
    
	JGUIUtil.addComponent(prop_JPanel, new JLabel (
		"The calculated statistic can be set as a time series and processor property." ), 
		0, ++yProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(prop_JPanel, new JLabel (
		"The time series property can be referenced later in commands using ${ts:Property} syntax." ), 
		0, ++yProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(prop_JPanel, new JLabel (
		"The processor property can be referenced later in commands using ${Property} syntax." ), 
		0, ++yProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++yProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    /* TODO SAM 2015-05-15 Enable later if needed
    JGUIUtil.addComponent(prop_JPanel, new JLabel("Statistic property:"),
        0, ++yProp, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __StatisticProperty_JTextField = new JTextField ( "", 20 );
    __StatisticProperty_JTextField.setToolTipText("The property can be referenced in other commands using ${Property}.");
    __StatisticProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(prop_JPanel, __StatisticProperty_JTextField,
        1, yProp, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel ( "Optional - property to set as statistic." ),
        3, yProp, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    */
    
    JGUIUtil.addComponent(prop_JPanel, new JLabel("Time series property:"),
        0, ++yProp, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TimeSeriesProperty_JTextField = new JTextField ( "", 40 );
    __TimeSeriesProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(prop_JPanel, __TimeSeriesProperty_JTextField,
        1, yProp, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel ( "Optional - time series property to set as statistic value." ),
        3, yProp, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(prop_JPanel, new JLabel("Statistic value property:"),
        0, ++yProp, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __StatisticValueProperty_JTextField = new JTextField ( "", 40 );
    __StatisticValueProperty_JTextField.setToolTipText("Name of processor property to set to statistic value, can include ${Property}");
    __StatisticValueProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(prop_JPanel, __StatisticValueProperty_JTextField,
        1, yProp, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel ( "Optional - processor property to set as statistic value." ),
        3, yProp, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea ( 4, 55 );
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Refresh the contents...
    checkGUIState();
	refresh ();

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
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else {
	    // Combo box...
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
    String TSList = "";
    String TSID = "";
    String EnsembleID = "";
    String Statistic = "";
	String Value1 = "";
	String Value2 = "";
	String Value3 = "";
	String AnalysisStart = "";
	String AnalysisEnd = "";
    String AnalysisWindowStart = "";
    String AnalysisWindowEnd = "";
	String TableID = "";
	String TableTSIDColumn = "";
	String TableTSIDFormat = "";
	String TableStatisticColumn = "";
	String TableStatisticDateTimeColumn = "";
	String TimeSeriesProperty = "";
	String StatisticValueProperty = "";
	PropList props = __command.getCommandParameters();
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
        TSList = props.getValue ( "TSList" );
		TSID = props.getValue ( "TSID" );
        EnsembleID = props.getValue ( "EnsembleID" );
        Statistic = props.getValue ( "Statistic" );
		Value1 = props.getValue ( "Value1" );
		Value2 = props.getValue ( "Value2" );
		Value3 = props.getValue ( "Value3" );
		AnalysisStart = props.getValue ( "AnalysisStart" );
		AnalysisEnd = props.getValue ( "AnalysisEnd" );
        AnalysisWindowStart = props.getValue ( "AnalysisWindowStart" );
        AnalysisWindowEnd = props.getValue ( "AnalysisWindowEnd" );
		TableID = props.getValue ( "TableID" );
		TableTSIDColumn = props.getValue ( "TableTSIDColumn" );
		TableTSIDFormat = props.getValue ( "TableTSIDFormat" );
		TableStatisticColumn = props.getValue ( "TableStatisticColumn" );
		TableStatisticDateTimeColumn = props.getValue ( "TableStatisticDateTimeColumn" );
		TimeSeriesProperty = props.getValue ( "TimeSeriesProperty" );
		StatisticValueProperty = props.getValue ( "StatisticValueProperty" );
        if ( TSList == null ) {
            // Select default...
            __TSList_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __TSList_JComboBox,TSList, JGUIUtil.NONE, null, null ) ) {
                __TSList_JComboBox.select ( TSList );
            }
            else {
                Message.printWarning ( 1, routine, "Existing command references an invalid\nTSList value \"" + TSList +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
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
			    // Select the blank...
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
        if ( Statistic == null ) {
            // Select default...
            __Statistic_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __Statistic_JComboBox,Statistic, JGUIUtil.NONE, null, null ) ) {
                __Statistic_JComboBox.select ( Statistic );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nStatistic value \"" + Statistic +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
		if ( Value1 != null ) {
			__Value1_JTextField.setText ( Value1 );
		}
        if ( Value2 != null ) {
            __Value2_JTextField.setText ( Value2 );
        }
        if ( Value3 != null ) {
            __Value3_JTextField.setText ( Value3 );
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
        if ( TableID == null ) {
            // Select default...
            __TableID_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __TableID_JComboBox,TableID, JGUIUtil.NONE, null, null ) ) {
                __TableID_JComboBox.select ( TableID );
            }
            else {
                // Creating new table so add in the first position
                if ( __TableID_JComboBox.getItemCount() == 0 ) {
                    __TableID_JComboBox.add(TableID);
                }
                else {
                    __TableID_JComboBox.insert(TableID, 0);
                }
                __TableID_JComboBox.select(0);
            }
        }
        if ( TableTSIDColumn != null ) {
            __TableTSIDColumn_JTextField.setText ( TableTSIDColumn );
        }
        if (TableTSIDFormat != null ) {
            __TableTSIDFormat_JTextField.setText(TableTSIDFormat.trim());
        }
        if ( TableStatisticColumn != null ) {
            __TableStatisticColumn_JTextField.setText ( TableStatisticColumn );
        }
        if ( TableStatisticDateTimeColumn != null ) {
            __TableStatisticDateTimeColumn_JTextField.setText ( TableStatisticDateTimeColumn );
        }
        if ( TimeSeriesProperty != null ) {
            __TimeSeriesProperty_JTextField.setText ( TimeSeriesProperty );
        }
        if ( StatisticValueProperty != null ) {
            __StatisticValueProperty_JTextField.setText ( StatisticValueProperty );
        }
	}
	// Regardless, reset the command from the fields...
	checkGUIState();
    TSList = __TSList_JComboBox.getSelected();
	TSID = __TSID_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
    Statistic = __Statistic_JComboBox.getSelected();
    Value1 = __Value1_JTextField.getText().trim();
    Value2 = __Value2_JTextField.getText().trim();
	Value3 = __Value3_JTextField.getText().trim();
	AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	TableID = __TableID_JComboBox.getSelected();
    TableTSIDColumn = __TableTSIDColumn_JTextField.getText().trim();
    TableTSIDFormat = __TableTSIDFormat_JTextField.getText().trim();
    TableStatisticColumn = __TableStatisticColumn_JTextField.getText().trim();
    TableStatisticDateTimeColumn = __TableStatisticDateTimeColumn_JTextField.getText().trim();
    TimeSeriesProperty = __TimeSeriesProperty_JTextField.getText().trim();
    StatisticValueProperty = __StatisticValueProperty_JTextField.getText().trim();
	props = new PropList ( __command.getCommandName() );
    props.add ( "TSList=" + TSList );
	props.add ( "TSID=" + TSID );
    props.add ( "EnsembleID=" + EnsembleID );
    props.add ( "Statistic=" + Statistic );
    props.add ( "Value1=" + Value1 );
    props.add ( "Value2=" + Value2 );
    props.add ( "Value3=" + Value3 );
	props.add ( "AnalysisStart=" + AnalysisStart );
	props.add ( "AnalysisEnd=" + AnalysisEnd );
    if ( __AnalysisWindow_JCheckBox.isSelected() ) {
        AnalysisWindowStart = __AnalysisWindowStart_JPanel.toString(false,true).trim();
        AnalysisWindowEnd = __AnalysisWindowEnd_JPanel.toString(false,true).trim();
        if ( AnalysisWindowStart.startsWith("99") ) {
        	// 99 is used as placeholder when month is not set... artifact of setting choices to blank during editing
        	AnalysisWindowStart = "";
        }
        AnalysisWindowEnd = __AnalysisWindowEnd_JPanel.toString(false,true).trim();
        if ( AnalysisWindowEnd.startsWith("99") ) {
        	// 99 is used as placeholder when month is not set... artifact of setting choices to blank during editing
        	AnalysisWindowEnd = "";
        }
        props.add ( "AnalysisWindowStart=" + AnalysisWindowStart );
        props.add ( "AnalysisWindowEnd=" + AnalysisWindowEnd );
    }
    props.add ( "TableID=" + TableID );
    props.add ( "TableTSIDColumn=" + TableTSIDColumn );
    props.add ( "TableTSIDFormat=" + TableTSIDFormat );
    props.add ( "TableStatisticColumn=" + TableStatisticColumn );
    props.add ( "TableStatisticDateTimeColumn=" + TableStatisticDateTimeColumn );
    props.add ( "TimeSeriesProperty=" + TimeSeriesProperty );
    props.add ( "StatisticValueProperty=" + StatisticValueProperty );
	__command_JTextArea.setText( __command.toString ( props ) );
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