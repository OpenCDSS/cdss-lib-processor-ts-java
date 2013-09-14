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

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;

import RTi.TS.RunningAverageType;
import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.TS.TSUtil_RunningStatistic;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Editor dialog for the RunningStatisticTimeSeries() command.
*/
public class RunningStatisticTimeSeries_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{

private final String __BRACKET_LABEL_CENTERED = "Number of intervals on each side:";
private final String __BRACKET_LABEL_NYEAR = "Number of years:";
private final String __BRACKET_LABEL_PREVIOUS = "Number of previous intervals:";
private final String __BRACKET_LABEL_FUTURE = "Number of future intervals:";

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private RunningStatisticTimeSeries_Command __command = null;
private JTextArea __command_JTextArea = null;
private SimpleJComboBox __TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox __TSID_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private SimpleJComboBox __Statistic_JComboBox = null;
private SimpleJComboBox __SampleMethod_JComboBox = null;
private JTextField __Bracket_JTextField = null;
private JTextField __AllowMissingCount_JTextField = null;
private JTextField __MinimumSampleSize_JTextField = null;
private JLabel __Bracket_JLabel = null; // Label for bracket
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
private SimpleJComboBox __ProbabilityUnits_JComboBox = null;
private boolean __error_wait = false; // Is there an error to be cleared up
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK button has been pressed.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public RunningStatisticTimeSeries_JDialog ( JFrame parent, RunningStatisticTimeSeries_Command command )
{   super(parent, true);
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
    
    __Bracket_JLabel.setEnabled(true);
    __Bracket_JTextField.setEnabled(true);
    String AverageMethod = __SampleMethod_JComboBox.getSelected();
    if ( AverageMethod.equalsIgnoreCase( "" + RunningAverageType.CENTERED ) ) {
        __Bracket_JLabel.setText ( __BRACKET_LABEL_CENTERED );
    }
    else if ( AverageMethod.equalsIgnoreCase( "" + RunningAverageType.NYEAR ) ){
        __Bracket_JLabel.setText ( __BRACKET_LABEL_NYEAR );
    }
    else if ( AverageMethod.equalsIgnoreCase( "" + RunningAverageType.N_ALL_YEAR) ) {
        // Bracket is not needed
        __Bracket_JLabel.setText ( "Bracket:" );
        __Bracket_JLabel.setEnabled(false);
        __Bracket_JTextField.setEnabled(false);
        __Bracket_JTextField.setText("");
    }
    else if ( AverageMethod.equalsIgnoreCase( "" + RunningAverageType.PREVIOUS) ||
        AverageMethod.equalsIgnoreCase( "" + RunningAverageType.PREVIOUS_INCLUSIVE) ) {
        __Bracket_JLabel.setText ( __BRACKET_LABEL_PREVIOUS );
    }
    else if ( AverageMethod.equalsIgnoreCase( "" + RunningAverageType.FUTURE) ||
        AverageMethod.equalsIgnoreCase( "" + RunningAverageType.FUTURE_INCLUSIVE) ) {
        __Bracket_JLabel.setText ( __BRACKET_LABEL_FUTURE );
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
    String SampleMethod = __SampleMethod_JComboBox.getSelected();
    String Bracket = __Bracket_JTextField.getText().trim();
    String AllowMissingCount = __AllowMissingCount_JTextField.getText().trim();
    String MinimumSampleSize = __MinimumSampleSize_JTextField.getText().trim();
    String Alias = __Alias_JTextField.getText().trim();
    String ProbabilityUnits = __ProbabilityUnits_JComboBox.getSelected();
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
    if ( (Statistic != null) && (Statistic.length() > 0) ) {
        parameters.set ( "Statistic", Statistic );
    }
    if ( SampleMethod.length() > 0 ) {
        parameters.set ( "SampleMethod", SampleMethod );
    }
    if ( Bracket.length() > 0 ) {
        parameters.set ( "Bracket", Bracket );
    }
    if ( (AllowMissingCount != null) && (AllowMissingCount.length() > 0) ) {
        parameters.set ( "AllowMissingCount", AllowMissingCount );
    }
    if ( (MinimumSampleSize != null) && (MinimumSampleSize.length() > 0) ) {
        parameters.set ( "MinimumSampleSize", MinimumSampleSize );
    }
    if (Alias.length() > 0) {
        parameters.set("Alias", Alias);
    }
    if (ProbabilityUnits.length() > 0) {
        parameters.set("ProbabilityUnits", ProbabilityUnits);
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
{   String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String Statistic = __Statistic_JComboBox.getSelected();
    String SampleMethod = __SampleMethod_JComboBox.getSelected();
    String Bracket = __Bracket_JTextField.getText().trim();
    String AllowMissingCount = __AllowMissingCount_JTextField.getText().trim();
    String MinimumSampleSize = __MinimumSampleSize_JTextField.getText().trim();
    String Alias = __Alias_JTextField.getText().trim();
    String ProbabilityUnits = __ProbabilityUnits_JComboBox.getSelected();
    __command.setCommandParameter ( "TSList", TSList );
    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
    __command.setCommandParameter ( "Statistic", Statistic );
    __command.setCommandParameter ( "SampleMethod", SampleMethod );
    __command.setCommandParameter ( "Bracket", Bracket );
    __command.setCommandParameter ( "AllowMissingCount", AllowMissingCount);
    __command.setCommandParameter ( "MinimumSampleSize", MinimumSampleSize);
    __command.setCommandParameter ( "Alias", Alias );
    __command.setCommandParameter ( "ProbabilityUnits", ProbabilityUnits );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, RunningStatisticTimeSeries_Command command )
{   __command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Create running statistic time series, where each new value is a statistic determined from a" +
        " moving window of sample data (e.g., a running average)."), 
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "A centered running statistic is computed from the values at a date/time and on either side."), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Previous and future running statistics use points only on one side of the current point, and optionally" +
        " inclusive of the current point."), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "An NYear running statistic uses the values for the date/time and previous years (N years total)."), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "An NAllYear running statistic uses the values for the date/time and all previous years."), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    __TSList_JComboBox = new SimpleJComboBox(false);
    y = CommandEditorUtil.addTSListToEditorDialogPanel ( this, main_JPanel, __TSList_JComboBox, y );

    __TSID_JLabel = new JLabel ("TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __TSID_JComboBox = new SimpleJComboBox ( true );  // Allow edits
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, main_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, y );
    
    __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
            this, this, main_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, y );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Statistic:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Statistic_JComboBox = new SimpleJComboBox(false);
    __Statistic_JComboBox.setData ( TSUtil_RunningStatistic.getStatisticChoicesAsStrings() );
    __Statistic_JComboBox.select ( 0 );
    __Statistic_JComboBox.addActionListener (this);
    JGUIUtil.addComponent(main_JPanel, __Statistic_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel ("Required - statistic to calculate."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Sample method:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__SampleMethod_JComboBox = new SimpleJComboBox ( false );
	for ( RunningAverageType type : TSUtil_RunningStatistic.getRunningAverageTypeChoices() ) {
	    __SampleMethod_JComboBox.addItem ( "" + type );
	}
	__SampleMethod_JComboBox.addItemListener ( this );
        JGUIUtil.addComponent(main_JPanel, __SampleMethod_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Required - how to determine sample to analyze."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

	__Bracket_JLabel = new JLabel ( __BRACKET_LABEL_CENTERED );
    JGUIUtil.addComponent(main_JPanel, __Bracket_JLabel,
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Bracket_JTextField = new JTextField ( 10 );
	__Bracket_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Bracket_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Required (except for " + RunningAverageType.N_ALL_YEAR + "," +
        RunningAverageType.ALL_YEARS + ")."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
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
        "Optional - minimum sample size to do calculation (default=no limit)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel("Alias to assign:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Alias_JTextField = new TSFormatSpecifiersJPanel(10);
    __Alias_JTextField.setToolTipText("Use %L for location, %T for data type, %I for interval.");
    __Alias_JTextField.addKeyListener ( this );
    __Alias_JTextField.getDocument().addDocumentListener(this);
    __Alias_JTextField.setToolTipText("%L for location, %T for data type.");
    JGUIUtil.addComponent(main_JPanel, __Alias_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - use %L for location, etc. (default=no alias)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Probability units:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ProbabilityUnits_JComboBox = new SimpleJComboBox(false);
    List<String> probabilityUnits = new Vector();
    probabilityUnits.add ( "" );
    probabilityUnits.add ( "Fraction" );
    probabilityUnits.add ( "Percent" );
    probabilityUnits.add ( "%" );
    __ProbabilityUnits_JComboBox.setData ( probabilityUnits );
    __ProbabilityUnits_JComboBox.select ( 0 );
    __ProbabilityUnits_JComboBox.addActionListener (this);
    JGUIUtil.addComponent(main_JPanel, __ProbabilityUnits_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - units for probability statistic (default=Fraction)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __command_JTextArea = new JTextArea ( 4, 40 );
    __command_JTextArea.setLineWrap ( true );
    __command_JTextArea.setWrapStyleWord ( true );
    __command_JTextArea.setEditable ( false );
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Refresh the contents...
    checkGUIState();
	refresh ();

	// South Panel: North
	JPanel button_Panel = new JPanel();
	button_Panel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_Panel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	__cancel_JButton = new SimpleJButton("Cancel", this);
	button_Panel.add ( __cancel_JButton );
	__ok_JButton = new SimpleJButton("OK", this);
	button_Panel.add ( __ok_JButton );

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
{   return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{	String routine = "RunningStatistic_JDialog.refresh";
    String TSList = "";
    String TSID = "";
    String EnsembleID = "";
    String Statistic = "";
    String SampleMethod = "";
    String Bracket = "";
    String AllowMissingCount = "";
    String MinimumSampleSize = "";
    String Alias = "";
    String ProbabilityUnits = "";
    PropList props = __command.getCommandParameters();
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command...
        TSList = props.getValue ( "TSList" );
        TSID = props.getValue ( "TSID" );
        EnsembleID = props.getValue ( "EnsembleID" );
        Statistic = props.getValue ( "Statistic" );
        SampleMethod = props.getValue ( "SampleMethod" );
        Bracket = props.getValue ( "Bracket" );
        AllowMissingCount = props.getValue ( "AllowMissingCount" );
        MinimumSampleSize = props.getValue ( "MinimumSampleSize" );
        Alias = props.getValue ( "Alias" );
        ProbabilityUnits = props.getValue ( "ProbabilityUnits" );
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
            if ( JGUIUtil.isSimpleJComboBoxItem( __Statistic_JComboBox, Statistic, JGUIUtil.NONE, null, null ) ) {
                __Statistic_JComboBox.select ( Statistic );
            }
            else {
                Message.printWarning ( 1, routine, "Existing command references an invalid\nStatistic value \"" +
                Statistic + "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( SampleMethod == null ) {
            // Select default...
            __SampleMethod_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __SampleMethod_JComboBox,SampleMethod, JGUIUtil.NONE, null, null ) ) {
                __SampleMethod_JComboBox.select ( SampleMethod );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nAverageMethod value \"" + SampleMethod +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( Bracket != null ) {
            __Bracket_JTextField.setText( Bracket );
        }
        if ( AllowMissingCount != null ) {
            __AllowMissingCount_JTextField.setText ( AllowMissingCount );
        }
        if ( MinimumSampleSize != null ) {
            __MinimumSampleSize_JTextField.setText ( MinimumSampleSize );
        }
        if (Alias != null ) {
            __Alias_JTextField.setText(Alias.trim());
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
	}
	// Regardless, reset the command from the fields...
    TSList = __TSList_JComboBox.getSelected();
    TSID = __TSID_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
    Statistic = __Statistic_JComboBox.getSelected();
    SampleMethod = __SampleMethod_JComboBox.getSelected();
    Bracket = __Bracket_JTextField.getText().trim();
    AllowMissingCount = __AllowMissingCount_JTextField.getText();
    MinimumSampleSize = __MinimumSampleSize_JTextField.getText();
    Alias = __Alias_JTextField.getText().trim();
    ProbabilityUnits = __ProbabilityUnits_JComboBox.getSelected();
    props = new PropList ( __command.getCommandName() );
    props.add ( "TSList=" + TSList );
    props.add ( "TSID=" + TSID );
    props.add ( "EnsembleID=" + EnsembleID );
    props.add ( "Statistic=" + Statistic );
    props.add ( "SampleMethod=" + SampleMethod );
    props.add ( "Bracket=" + Bracket );
    props.add ( "AllowMissingCount=" + AllowMissingCount );
    props.add ( "MinimumSampleSize=" + MinimumSampleSize );
    props.add ( "Alias=" + Alias );
    props.add ( "ProbabilityUnits=" + ProbabilityUnits );
    __command_JTextArea.setText( __command.toString ( props ) );
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed
and the dialog is closed.
*/
private void response ( boolean ok )
{   __ok = ok;  // Save to be returned by ok()
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