package rti.tscommandprocessor.commands.ensemble;

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
import java.util.Vector;

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
import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.TS.TSIdent;
import RTi.TS.TSIdent_JDialog;
import RTi.TS.TSStatisticType;
import RTi.TS.TSUtil_NewStatisticTimeSeriesFromEnsemble;
import RTi.Util.GUI.DictionaryJDialog;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.PropList;
import RTi.Util.Math.DistributionType;
import RTi.Util.Message.Message;

@SuppressWarnings("serial")
public class NewStatisticTimeSeriesFromEnsemble_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private JFrame __parent_JFrame = null;
private NewStatisticTimeSeriesFromEnsemble_Command __command = null;
private JTabbedPane __main_JTabbedPane = null;
private JTextArea __command_JTextArea = null;
private SimpleJComboBox	__EnsembleID_JComboBox = null;
private SimpleJComboBox __Distribution_JComboBox = null;
private JTextArea __DistributionParameters_JTextArea = null;
private SimpleJComboBox __ProbabilityUnits_JComboBox = null;
private SimpleJComboBox	__Statistic_JComboBox = null;
private JTextField __Value1_JTextField = null;
private JTextField __AllowMissingCount_JTextField = null;
private JTextField __MinimumSampleSize_JTextField = null;
private JTextField __AnalysisStart_JTextField = null;
private JTextField __AnalysisEnd_JTextField = null;
private JTextArea __NewTSID_JTextArea = null;
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
private JTextField __Description_JTextField = null;
private JTextField __OutputStart_JTextField = null;
private JTextField __OutputEnd_JTextField = null;
private SimpleJButton __edit_JButton = null;
private SimpleJButton __clear_JButton = null; // Clear NewTSID button
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Whether OK has been pressed.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public NewStatisticTimeSeriesFromEnsemble_JDialog ( JFrame parent, NewStatisticTimeSeriesFromEnsemble_Command command )
{	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	Object o = event.getSource();
	String routine = "NewStatisticTimeSeriesFromEnsemble_JDialog.actionPerformed";

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
			Message.printWarning ( 1, routine,
			"Error creating time series identifier from \"" + NewTSID + "\"." );
			Message.printWarning ( 3, routine, e );
		}
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "NewStatisticTimeSeriesFromEnsemble");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
    else if ( event.getActionCommand().equalsIgnoreCase("EditDistributionParameters") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String DistributionParameters = __DistributionParameters_JTextArea.getText().trim();
        String [] notes = {
            "Distributions have the following parameters:",
            //"The choices are pre-populated with parameter names but values must be provided.",
            "   Gringorten - a",
            "   Weibull - none",
            "Refer to the command documentation for more information.",
            "Warnings will be generated if insufficient input is provided."
        };
        String properties = (new DictionaryJDialog ( __parent_JFrame, true, DistributionParameters, "Edit DistributionParameters Parameter",
            notes, "Parameter", "Parameter Value",10)).response();
        if ( properties != null ) {
            __DistributionParameters_JTextArea.setText ( properties );
            refresh();
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
{
    // Set the tooltips on the Value input fields to help users know what to enter
    __Value1_JTextField.setToolTipText("");
    
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
    	else if ( stat == TSStatisticType.EXCEEDANCE_PROBABILITY ) {
    		__Value1_JTextField.setToolTipText("Exceedance probability value as fraction");
    	}
    }
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
	PropList props = new PropList ( "" );
	String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String Distribution = __Distribution_JComboBox.getSelected();
    String DistributionParameters = __DistributionParameters_JTextArea.getText().trim().replace("\n"," ");
    String ProbabilityUnits = __ProbabilityUnits_JComboBox.getSelected();
	String Statistic = __Statistic_JComboBox.getSelected();
	String Value1 = __Value1_JTextField.getText().trim();
	String AllowMissingCount = __AllowMissingCount_JTextField.getText().trim();
	String MinimumSampleSize = __MinimumSampleSize_JTextField.getText().trim();
	String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	String NewTSID = __NewTSID_JTextArea.getText().trim();
	String Alias = __Alias_JTextField.getText().trim();
	String Description = __Description_JTextField.getText().trim();
    String OutputStart = __OutputStart_JTextField.getText().trim();
    String OutputEnd = __OutputEnd_JTextField.getText().trim();
	__error_wait = false;

	if ( (EnsembleID != null) && (EnsembleID.length() > 0) ) {
		props.set ( "EnsembleID", EnsembleID );
	}
    if ( (Distribution != null) && (Distribution.length() > 0) ) {
    	props.set ( "Distribution", Distribution );
    }
    if ( (DistributionParameters != null) && (DistributionParameters.length() > 0) ) {
    	props.set ( "DistributionParameters", DistributionParameters );
    }
    if (ProbabilityUnits.length() > 0) {
    	props.set("ProbabilityUnits", ProbabilityUnits);
    }
	if ( (Statistic != null) && (Statistic.length() > 0) ) {
		props.set ( "Statistic", Statistic );
	}
	if ( Value1.length() > 0 ) {
		props.set ( "Value1", Value1 );
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
	if ( (NewTSID != null) && (NewTSID.length() > 0) ) {
		props.set ( "NewTSID", NewTSID );
	}
	if ( Alias.length() > 0 ) {
		props.set ( "Alias", Alias );
	}
    if ( Description.length() > 0 ) {
        props.set ( "Description", Description );
    }
    if ( OutputStart.length() > 0 ) {
        props.set ( "OutputStart", OutputStart );
    }
    if ( OutputEnd.length() > 0 ) {
        props.set ( "OutputEnd", OutputEnd );
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
{	String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String Distribution = __Distribution_JComboBox.getSelected();
    String DistributionParameters = __DistributionParameters_JTextArea.getText().trim().replace("\n"," ");
    String ProbabilityUnits = __ProbabilityUnits_JComboBox.getSelected();
	String Statistic = __Statistic_JComboBox.getSelected();
	String Value1 = __Value1_JTextField.getText().trim();
	String AllowMissingCount = __AllowMissingCount_JTextField.getText().trim();
	String MinimumSampleSize = __MinimumSampleSize_JTextField.getText().trim();
	String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	String NewTSID = __NewTSID_JTextArea.getText().trim();
	String Alias = __Alias_JTextField.getText().trim();
	String Description = __Description_JTextField.getText().trim();
    String OutputStart = __OutputStart_JTextField.getText().trim();
    String OutputEnd = __OutputEnd_JTextField.getText().trim();
	__command.setCommandParameter ( "EnsembleID", EnsembleID );
    __command.setCommandParameter ( "Distribution", Distribution );
    __command.setCommandParameter ( "DistributionParameters", DistributionParameters );
    __command.setCommandParameter ( "ProbabilityUnits", ProbabilityUnits );
	__command.setCommandParameter ( "Statistic", Statistic );
	__command.setCommandParameter ( "Value1", Value1 );
	__command.setCommandParameter ( "AllowMissingCount", AllowMissingCount);
	__command.setCommandParameter ( "MinimumSampleSize", MinimumSampleSize );
	__command.setCommandParameter ( "AnalysisStart", AnalysisStart );
	__command.setCommandParameter ( "AnalysisEnd", AnalysisEnd );
	__command.setCommandParameter ( "NewTSID", NewTSID );
	__command.setCommandParameter ( "Alias", Alias );
	__command.setCommandParameter ( "Description", Description );
    __command.setCommandParameter ( "OutputStart", OutputStart );
    __command.setCommandParameter ( "OutputEnd", OutputEnd );
}

/**
Instantiates the GUI components.
@param parent Frame class instantiating this class.
@param title Dialog title.
@param command Command to edit.
*/
private void initialize ( JFrame parent, NewStatisticTimeSeriesFromEnsemble_Command command )
{	__parent_JFrame = parent;
	__command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;
	
	TSUtil_NewStatisticTimeSeriesFromEnsemble tsu = new TSUtil_NewStatisticTimeSeriesFromEnsemble();

	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"<html><b>This command is being enhanced.  Strict checks on distribution and statistic combinations are being enabled." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Create a time series as a statistic determined from an ensemble of time series." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"A value from each ensemble trace (time series) is used to create the sample at each time interval." ),
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
		"Select the time series ensemble to process." ),
		0, ++yInput, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(input_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yInput, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
    JGUIUtil.addComponent(input_JPanel, new JLabel("Ensemble to analyze (EnsembleID):"),
		0, ++yInput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edit
	__EnsembleID_JComboBox.setToolTipText("Select a time series ensemble ID from the list or specify with ${Property} notation");
	List<String> tsensembleids = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
	    (TSCommandProcessor)__command.getCommandProcessor(), __command );
	if ( tsensembleids == null ) {
		// User will not be able to select anything.
        tsensembleids = new ArrayList<String>();
	}
	__EnsembleID_JComboBox.setData ( tsensembleids );
	__EnsembleID_JComboBox.addItemListener ( this );
	__EnsembleID_JComboBox.getJTextComponent().addKeyListener ( this );
    JGUIUtil.addComponent(input_JPanel, __EnsembleID_JComboBox,
		1, yInput, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // Panel for distribution parameters
    int yDist = -1;
    JPanel dist_JPanel = new JPanel();
    dist_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Distribution", dist_JPanel );
 
    // Distribution-related parameters
    JGUIUtil.addComponent(dist_JPanel, new JLabel (
        "Parameters related to distribution are needed for plotting position, nonexceedance probability, and exceedance probability statistics."), 
        0, ++yDist, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(dist_JPanel, new JSeparator (SwingConstants.HORIZONTAL), 
        0, ++yDist, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        
    JGUIUtil.addComponent(dist_JPanel, new JLabel ("Distribition:"),
        0, ++yDist, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Distribution_JComboBox = new SimpleJComboBox(false);
    __Distribution_JComboBox.setToolTipText(
        "Distribution is used with PlottingPosition*, *ExceedanceProbability statistics.  Default is " + DistributionType.SAMPLE + ".");
    List<String> distributions = tsu.getDistributionChoicesAsStrings();
    distributions.add(0,"");
    __Distribution_JComboBox.setData ( distributions );
    __Distribution_JComboBox.select ( 0 );
    __Distribution_JComboBox.addActionListener (this);
    JGUIUtil.addComponent(dist_JPanel, __Distribution_JComboBox,
        1, yDist, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(dist_JPanel, new JLabel ("Optional - distribution for statistics that require it (default=" +
        DistributionType.WEIBULL + ")."),
        3, yDist, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(dist_JPanel, new JLabel ("Distribution parameters:"),
        0, ++yDist, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DistributionParameters_JTextArea = new JTextArea (3,35);
    __DistributionParameters_JTextArea.setLineWrap ( true );
    __DistributionParameters_JTextArea.setWrapStyleWord ( true );
    __DistributionParameters_JTextArea.setToolTipText("Parameter1:Value1,Parameter2:Value2,...");
    __DistributionParameters_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(dist_JPanel, new JScrollPane(__DistributionParameters_JTextArea),
        1, yDist, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(dist_JPanel, new JLabel ("Optional - parameters needed by distribution."),
        3, yDist, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(dist_JPanel, new SimpleJButton ("Edit","EditDistributionParameters",this),
        3, ++yDist, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(dist_JPanel, new JLabel ("Probability units:"),
        0, ++yDist, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ProbabilityUnits_JComboBox = new SimpleJComboBox(false);
    List<String> probabilityUnits = new Vector<String>();
    probabilityUnits.add ( "" );
    probabilityUnits.add ( "Fraction" );
    probabilityUnits.add ( "Percent" );
    probabilityUnits.add ( "%" );
    __ProbabilityUnits_JComboBox.setData ( probabilityUnits );
    __ProbabilityUnits_JComboBox.select ( 0 );
    __ProbabilityUnits_JComboBox.addActionListener (this);
    JGUIUtil.addComponent(dist_JPanel, __ProbabilityUnits_JComboBox,
        1, yDist, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(dist_JPanel, new JLabel ("Optional - units for probability statistic (default=Fraction)."),
        3, yDist, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Panel for analysis
    int yAnalysis = -1;
    JPanel analysis_JPanel = new JPanel();
    analysis_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Analysis", analysis_JPanel );
    
    JGUIUtil.addComponent(analysis_JPanel, new JLabel (
		"A statistic is a value computed from a sample consisting of time series values from each interval in the analyis period."),
		0, ++yAnalysis, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel (
		"Mouse over the Value1 input field for help understanding the input - help will be blank if the value is not used for the statistic."),
		0, ++yAnalysis, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
    	0, ++yAnalysis, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(analysis_JPanel, new JLabel ("Statistic:"),
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Statistic_JComboBox = new SimpleJComboBox(false);
	__Statistic_JComboBox.setData ( tsu.getStatisticChoicesAsStrings() );
	__Statistic_JComboBox.select ( 0 );
	__Statistic_JComboBox.addActionListener (this);
	JGUIUtil.addComponent(analysis_JPanel, __Statistic_JComboBox,
		1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel ("Required - statistic to calculate."),
		3, yAnalysis, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Value1:" ), 
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Value1_JTextField = new JTextField ( 10 );
	__Value1_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(analysis_JPanel, __Value1_JTextField,
		1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel(
		"Optional - may be needed as input to calculate statistic."), 
		3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        
    JGUIUtil.addComponent(analysis_JPanel, new JLabel ("Allow missing count:"),
        0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __AllowMissingCount_JTextField = new JTextField (10);
    __AllowMissingCount_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(analysis_JPanel, __AllowMissingCount_JTextField,
        1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel (
        "Optional - number of missing values allowed in sample (default=no limit)."),
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
    
    // Panel for output
    int yOut = -1;
    JPanel out_JPanel = new JPanel();
    out_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Output", out_JPanel );

    JGUIUtil.addComponent(out_JPanel, new JLabel (
		"Specify parameters to define the output time series created by the command."),
		0, ++yOut, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(out_JPanel, new JLabel (
		"The output time series will have the same data interval as the ensemble that is used as input."),
		0, ++yOut, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(out_JPanel, new JLabel (
		"It is recommended that a new time series identifier (TSID) be specified for the result " +
		"to avoid confusion with the original time series."),
		0, ++yOut, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(out_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++yOut, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(out_JPanel, new JLabel ( "New time series ID:" ),
		0, ++yOut, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__NewTSID_JTextArea = new JTextArea ( 3, 25 );
	__NewTSID_JTextArea.setToolTipText("Specify new time series pattern (without sequence number, can use ${Property} notation");
	__NewTSID_JTextArea.setLineWrap ( true );
	__NewTSID_JTextArea.setWrapStyleWord ( true );
	__NewTSID_JTextArea.setEditable ( false );
	__NewTSID_JTextArea.addKeyListener ( this );
	// Make 3-high to fit in the edit button...
    JGUIUtil.addComponent(out_JPanel, new JScrollPane(__NewTSID_JTextArea),
		1, yOut, 2, 3, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(out_JPanel, new JLabel(
		"Specify to avoid confusion with TSID from original TS."), 
		3, yOut, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    yOut += 2;
    JGUIUtil.addComponent(out_JPanel, (__edit_JButton = new SimpleJButton ( "Edit", "Edit", this ) ),
		3, yOut, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(out_JPanel, (__clear_JButton =
		new SimpleJButton ( "Clear", "Clear", this ) ),
		4, yOut, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(out_JPanel, new JLabel("Alias to assign:"),
        0, ++yOut, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Alias_JTextField = new TSFormatSpecifiersJPanel(15);
    __Alias_JTextField.addKeyListener ( this );
    __Alias_JTextField.getDocument().addDocumentListener(this);
    JGUIUtil.addComponent(out_JPanel, __Alias_JTextField,
        1, yOut, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(out_JPanel, new JLabel ("Required - use %L for location, etc."),
        3, yOut, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(out_JPanel, new JLabel ( "Description:" ),
        0, ++yOut, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Description_JTextField = new JTextField ( "", 20 );
    __Description_JTextField.setToolTipText("Specify the description or ${Property}, ${ts:property} notation");
    __Description_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(out_JPanel, __Description_JTextField,
        1, yOut, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(out_JPanel, new JLabel(
        "Optional - description (default=statistic)."),
        3, yOut, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(out_JPanel, new JLabel ( "Output start:" ),
        0, ++yOut, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputStart_JTextField = new JTextField ( "", 20 );
    __OutputStart_JTextField.setToolTipText("Specify the output start using a date/time string or ${Property} notation");
    __OutputStart_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(out_JPanel, __OutputStart_JTextField,
        1, yOut, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(out_JPanel, new JLabel(
        "Optional - output start date/time (default=full time series period)."),
        3, yOut, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(out_JPanel, new JLabel ( "Output end:" ), 
        0, ++yOut, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputEnd_JTextField = new JTextField ( "", 20 );
    __OutputEnd_JTextField.setToolTipText("Specify the output end using a date/time string or ${Property} notation");
    __OutputEnd_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(out_JPanel, __OutputEnd_JTextField,
        1, yOut, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(out_JPanel, new JLabel(
        "Optional - output end date/time (default=full time series period)."),
        3, yOut, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

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
{	String routine = getClass().getSimpleName() + ".refresh";
	String EnsembleID = "";
    String Distribution = "";
    String DistributionParameters = "";
    String ProbabilityUnits = "";
	String Statistic = "";
	String Value1 = "";
	String AllowMissingCount = "";
	String MinimumSampleSize = "";
	String AnalysisStart = "";
	String AnalysisEnd = "";
	String NewTSID = "";
	String Alias = "";
	String Description = "";
    String OutputStart = "";
    String OutputEnd = "";
	PropList props = __command.getCommandParameters();
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
        EnsembleID = props.getValue ( "EnsembleID" );
        Distribution = props.getValue ( "Distribution" );
        DistributionParameters = props.getValue ( "DistributionParameters" );
        ProbabilityUnits = props.getValue ( "ProbabilityUnits" );
		Statistic = props.getValue ( "Statistic" );
		Value1 = props.getValue ( "Value1" );
		AllowMissingCount = props.getValue ( "AllowMissingCount" );
		MinimumSampleSize = props.getValue ( "MinimumSampleSize" );
		AnalysisStart = props.getValue ( "AnalysisStart" );
		AnalysisEnd = props.getValue ( "AnalysisEnd" );
		NewTSID = props.getValue ( "NewTSID" );
		Alias = props.getValue ( "Alias" );
		Description = props.getValue ( "Description" );
        OutputStart = props.getValue ( "OutputStart" );
        OutputEnd = props.getValue ( "OutputEnd" );
		// Now select the item in the list.  If not a match, print a warning.
		if ( JGUIUtil.isSimpleJComboBoxItem( __EnsembleID_JComboBox, EnsembleID, JGUIUtil.NONE, null, null ) ) {
			__EnsembleID_JComboBox.select ( EnsembleID );
		}
		else {
		    // Automatically add to the list at the start...
			if ( (EnsembleID != null) && (EnsembleID.length() > 0) ) {
			    if ( __EnsembleID_JComboBox.getItemCount() > 0 ) {
			        __EnsembleID_JComboBox.insertItemAt ( EnsembleID, 1 );
			    }
			    else {
			        __EnsembleID_JComboBox.add( EnsembleID );
			    }
				// Select...
				__EnsembleID_JComboBox.select ( EnsembleID );
			}
			else {
			    // Do not select anything...
			}
		}
        if ( Distribution == null ) {
            // Select default...
            __Distribution_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __Distribution_JComboBox, Distribution, JGUIUtil.NONE, null, null ) ) {
                __Distribution_JComboBox.select ( Distribution );
            }
            else {
                Message.printWarning ( 1, routine, "Existing command references an invalid\nDistribution value \"" +
                    Distribution + "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( DistributionParameters != null ) {
            __DistributionParameters_JTextArea.setText ( DistributionParameters );
        }
        if ( ProbabilityUnits == null ) {
            // Select default...
            __ProbabilityUnits_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __ProbabilityUnits_JComboBox, ProbabilityUnits, JGUIUtil.NONE, null, null ) ) {
                __ProbabilityUnits_JComboBox.select ( ProbabilityUnits );
            }
            else {
                Message.printWarning ( 1, routine, "Existing command references an invalid ProbabilityUnits value \"" +
                    ProbabilityUnits + "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
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
				Statistic + "\".  Select a different value or Cancel.");
				__error_wait = true;
			}
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
		if ( AnalysisStart != null ) {
			__AnalysisStart_JTextField.setText( AnalysisStart );
		}
		if ( AnalysisEnd != null ) {
			__AnalysisEnd_JTextField.setText ( AnalysisEnd );
		}
		if ( NewTSID != null ) {
			__NewTSID_JTextArea.setText ( NewTSID );
		}
		if ( Alias != null ) {
			__Alias_JTextField.setText ( Alias );
		}
		if ( Description != null ) {
			__Description_JTextField.setText ( Description );
		}
        if ( OutputStart != null ) {
            __OutputStart_JTextField.setText( OutputStart );
        }
        if ( OutputEnd != null ) {
            __OutputEnd_JTextField.setText ( OutputEnd );
        }
	}
	// Regardless, reset the command from the fields...
	EnsembleID = __EnsembleID_JComboBox.getSelected();
    Distribution = __Distribution_JComboBox.getSelected();
    DistributionParameters = __DistributionParameters_JTextArea.getText().trim().replace("\n"," ");
    ProbabilityUnits = __ProbabilityUnits_JComboBox.getSelected();
	Statistic = __Statistic_JComboBox.getSelected();
    Value1 = __Value1_JTextField.getText().trim();
	AllowMissingCount = __AllowMissingCount_JTextField.getText();
	MinimumSampleSize = __MinimumSampleSize_JTextField.getText();
	AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	NewTSID = __NewTSID_JTextArea.getText().trim();
	Alias = __Alias_JTextField.getText().trim();
	Description = __Description_JTextField.getText().trim();
    OutputStart = __OutputStart_JTextField.getText().trim();
    OutputEnd = __OutputEnd_JTextField.getText().trim();
	props = new PropList ( __command.getCommandName() );
	props.add ( "EnsembleID=" + EnsembleID );
    props.add ( "Distribution=" + Distribution );
    props.add ( "DistributionParameters=" + DistributionParameters );
    props.add ( "ProbabilityUnits=" + ProbabilityUnits );
	props.add ( "Statistic=" + Statistic );
	props.add ( "Value1=" + Value1 );
	props.add ( "AllowMissingCount=" + AllowMissingCount );
	props.add ( "MinimumSampleSize=" + MinimumSampleSize );
	props.add ( "AnalysisStart=" + AnalysisStart );
	props.add ( "AnalysisEnd=" + AnalysisEnd );
	props.add ( "NewTSID=" + NewTSID );
	props.add ( "Alias=" + Alias );
	props.add ( "Description=" + Description );
    props.add ( "OutputStart=" + OutputStart );
    props.add ( "OutputEnd=" + OutputEnd );
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