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
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.IO.Command;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;

@SuppressWarnings("serial")
public class FillProrate_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, ListSelectionListener, WindowListener
{

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private FillProrate_Command __command = null;
private JTextArea __command_JTextArea=null;
private SimpleJComboBox	__InitialValue_JComboBox = null;
private SimpleJComboBox __TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox __TSID_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private SimpleJComboBox __IndependentTSID_JComboBox = null;
private SimpleJComboBox	__FillDirection_JComboBox = null;
private JTextField __FillStart_JTextField = null;
private JTextField __FillEnd_JTextField = null;
private SimpleJComboBox	__FactorMethod_JComboBox = null;
private JTextField __AnalysisStart_JTextField =null;
private JTextField __AnalysisEnd_JTextField =null;
private JTextField __FillFlag_JTextField =null;
private boolean __error_wait = false;	// Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK button has been pressed.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public FillProrate_JDialog ( JFrame parent, Command command )
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
		__command = null;
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "FillProrate");
	}
	else if ( o == __ok_JButton ) {
		refresh();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else {
	    // Choices...
		refresh();
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
            TSListType.LAST_MATCHING_TSID.equals(TSList)) {
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
}

/**
Check the user input for errors and set __error_wait accordingly.
*/
private void checkInput ()
{   // Put together a list of parameters to check...
    PropList props = new PropList ( "" );
    String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String IndependentTSID = __IndependentTSID_JComboBox.getSelected();
    String FillStart = __FillStart_JTextField.getText().trim();
    String FillEnd = __FillEnd_JTextField.getText().trim();
    String FillFlag = __FillFlag_JTextField.getText().trim();
    String FillDirection = __FillDirection_JComboBox.getSelected();
    String FactorMethod = __FactorMethod_JComboBox.getSelected();
    String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
    String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
    String InitialValue = __InitialValue_JComboBox.getSelected();
    __error_wait = false;

    if ( TSList.length() > 0 ) {
        props.set ( "TSList", TSList );
    }
    if ( TSID.length() > 0 ) {
        props.set ( "TSID", TSID );
    }
    if ( EnsembleID.length() > 0 ) {
        props.set ( "EnsembleID", EnsembleID );
    }
    if ( IndependentTSID.length() > 0 ) {
        props.set ( "IndependentTSID", IndependentTSID );
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
    if ( FillDirection.length() > 0 ) {
        props.set( "FillDirection", FillDirection );
    }
    if ( FactorMethod.length() > 0 ) {
        props.set( "FactorMethod", FactorMethod );
    }
    if ( AnalysisStart.length() > 0 ) {
        props.set( "AnalysisStart", AnalysisStart );
    }
    if ( AnalysisEnd.length() > 0 ) {
        props.set( "AnalysisEnd", AnalysisEnd );
    }
    if ( InitialValue.length() > 0 ) {
        props.set( "InitialValue", InitialValue );
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
{   String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String IndependentTSID = __IndependentTSID_JComboBox.getSelected();
    String FillStart = __FillStart_JTextField.getText().trim();
    String FillEnd = __FillEnd_JTextField.getText().trim();
    String FillFlag = __FillFlag_JTextField.getText().trim();
    String FillDirection = __FillDirection_JComboBox.getSelected();
    String FactorMethod = __FactorMethod_JComboBox.getSelected();
    String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
    String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
    String InitialValue = __InitialValue_JComboBox.getSelected();
    __command.setCommandParameter ( "TSList", TSList );
    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
    __command.setCommandParameter ( "IndependentTSID", IndependentTSID );
    __command.setCommandParameter ( "FillStart", FillStart );
    __command.setCommandParameter ( "FillFlag", FillFlag );
    __command.setCommandParameter ( "FillEnd", FillEnd );
    __command.setCommandParameter ( "FillDirection", FillDirection );
    __command.setCommandParameter ( "FactorMethod", FactorMethod );
    __command.setCommandParameter ( "AnalysisStart", AnalysisStart );
    __command.setCommandParameter ( "AnalysisEnd", AnalysisEnd );
    __command.setCommandParameter ( "InitialValue", InitialValue );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__TSID_JComboBox = null;
	__IndependentTSID_JComboBox = null;
	__FillDirection_JComboBox = null;
	__FactorMethod_JComboBox = null;
	__FillStart_JTextField = null;
	__FillEnd_JTextField = null;
	__AnalysisStart_JTextField = null;
	__AnalysisEnd_JTextField = null;
	__InitialValue_JComboBox = null;
	__cancel_JButton = null;
	__command_JTextArea = null;
	__command = null;
	__ok_JButton = null;
	super.finalize ();
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, Command command )
{	__command = (FillProrate_Command)command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"This command fills missing data in time series by" +
		" prorating values from an independent time series." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The proration factor is calculated in one of the following " +
		"ways (indicated by \"FactorMethod\"):" ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"1)  Recompute the factor at each point where a non-missing " +
		"value is found in both time series (NearestPoint)." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"     The initial value in the filled time series is used to " +
		"compute the initial factor, for missing data at the end of the fill period."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"2)  Recompute the factor as the dependent time series "
		+ "value divided by the average of independent values (AnalysisAverage)." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The factor (ratio) is TS/IndependentTS and the filled value "+
		"is calculated as factor*IndependentTS." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"If the independent time series data value is zero, the factor "
		+"remains as previously calculated to avoid division by zero."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The independent time series is not itself filled if matched for the TSList."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The fill start and end, if specified, will limit the period that is filled." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Use standard date/time formats appropriate for the date/time precision of the time series."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
	JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++y, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    __TSList_JComboBox = new SimpleJComboBox(false);
    y = CommandEditorUtil.addTSListToEditorDialogPanel (
            this, main_JPanel, new JLabel ("TS List:"), __TSList_JComboBox, y );

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
    
    JLabel IndependentTSID_JLabel = new JLabel("Independent time series:");
    __IndependentTSID_JComboBox = new SimpleJComboBox ( true );  // Allow edits
    y = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, main_JPanel, IndependentTSID_JLabel, __IndependentTSID_JComboBox, tsids, y );

    JGUIUtil.addComponent(main_JPanel,new JLabel("Fill start date/time:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__FillStart_JTextField = new JTextField ( "", 10 );
	__FillStart_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __FillStart_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (	"Optional - start date/time for filling (blank=fill all)."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel,new JLabel("Fill end date/time:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__FillEnd_JTextField = new JTextField ( "", 10 );
	__FillEnd_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __FillEnd_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - end date/time for filling (blank=fill all)."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel,new JLabel( "Fill flag:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__FillFlag_JTextField = new JTextField ( "", 10 );
	__FillFlag_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __FillFlag_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - string to mark filled data."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Fill direction:"), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__FillDirection_JComboBox = new SimpleJComboBox ( false );
	List<String> fillChoices = new ArrayList<String>();
	fillChoices.add ( "" );
	fillChoices.add ( __command._Backward );
	fillChoices.add ( __command._Forward );
	__FillDirection_JComboBox.setData(fillChoices);
	__FillDirection_JComboBox.select ( 0 );
	__FillDirection_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __FillDirection_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - direction to traverse data when filling (default=" + __command._Forward + ")."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

   JGUIUtil.addComponent(main_JPanel, new JLabel("Calculate factor how?:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__FactorMethod_JComboBox = new SimpleJComboBox ( false );
	List<String> factorChoices = new ArrayList<String>();
	factorChoices.add ( "" );
	factorChoices.add ( __command._AnalyzeAverage );
	factorChoices.add ( __command._NearestPoint );
	__FactorMethod_JComboBox.setData(factorChoices);
	__FactorMethod_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __FactorMethod_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - how will factor be calculated? (default=" + __command._NearestPoint + ")."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel,new JLabel("Analysis start date/time:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__AnalysisStart_JTextField = new JTextField ( "", 10 );
	__AnalysisStart_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __AnalysisStart_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
            "Optional - analysis start date/time, for FactorMethod=" + __command._AnalyzeAverage + ")."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel,new JLabel( "Analysis end date/time:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__AnalysisEnd_JTextField = new JTextField ( "", 10 );
	__AnalysisEnd_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __AnalysisEnd_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - analysis end date/time, for FactorMethod=" +
            __command._AnalyzeAverage + ")."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel,new JLabel( "Initial value:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InitialValue_JComboBox = new SimpleJComboBox ( true );
	List<String> InitialValue_Vector = new Vector<String>( 3 );
	InitialValue_Vector.add ( "" );
	InitialValue_Vector.add ( __command._NearestBackward );
	InitialValue_Vector.add ( __command._NearestForward );
	__InitialValue_JComboBox.setData ( InitialValue_Vector );
	__InitialValue_JComboBox.addKeyListener ( this );
	__InitialValue_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __InitialValue_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - initial value in time series for missing end-points."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea ( 5, 55 );
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

	__ok_JButton = new SimpleJButton("OK", this);
	__ok_JButton.setToolTipText("Save changes to command");
	button_JPanel.add ( __ok_JButton );
	__cancel_JButton = new SimpleJButton("Cancel", this);
	button_JPanel.add ( __cancel_JButton );
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
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( false );
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
{   return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{   String routine = "FillProrate_JDialog.refresh";
    String TSList = "";
    String TSID = "";
    String EnsembleID = "";
    String IndependentTSID = "";
    String FillStart = "";
    String FillEnd = "";
    String FillFlag = "";
    String FillDirection = "";
    String FactorMethod = "";
    String AnalysisStart = "";
    String AnalysisEnd = "";
    String InitialValue = "";
    PropList props = __command.getCommandParameters();
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command...
        TSList = props.getValue ( "TSList" );
        TSID = props.getValue ( "TSID" );
        EnsembleID = props.getValue ( "EnsembleID" );
        IndependentTSID = props.getValue ( "IndependentTSID" );
        FillStart = props.getValue ( "FillStart" );
        FillEnd = props.getValue ( "FillEnd" );
        FillFlag = props.getValue ( "FillFlag" );
        FillDirection = props.getValue ( "FillDirection" );
        FactorMethod = props.getValue ( "FactorMethod" );
        AnalysisStart = props.getValue ( "AnalysisStart" );
        AnalysisEnd = props.getValue ( "AnalysisEnd" );
        InitialValue = props.getValue ( "InitialValue" );
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
        if (    JGUIUtil.isSimpleJComboBoxItem( __IndependentTSID_JComboBox, IndependentTSID,
                JGUIUtil.NONE, null, null ) ) {
                __IndependentTSID_JComboBox.select ( IndependentTSID );
        }
        else {  // Automatically add to the list after the blank...
            if ( (IndependentTSID != null) && (IndependentTSID.length() > 0) ) {
                __IndependentTSID_JComboBox.insertItemAt ( IndependentTSID, 1 );
                // Select...
                __IndependentTSID_JComboBox.select ( IndependentTSID );
            }
            else {
                // Select the blank...
                __IndependentTSID_JComboBox.select ( 0 );
            }
        }
        if ( FillStart != null ) {
            __FillStart_JTextField.setText ( FillStart );
        }
        if ( FillEnd != null ) {
            __FillEnd_JTextField.setText ( FillEnd );
        }
        if ( FillFlag != null ) {
            __FillFlag_JTextField.setText ( FillFlag );
        }
        if ( FillDirection == null ) {
            // Select default...
            __FillDirection_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __FillDirection_JComboBox,FillDirection, JGUIUtil.NONE, null, null ) ) {
                __FillDirection_JComboBox.select ( FillDirection );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nFillDirection value \"" + FillDirection +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( FactorMethod == null ) {
            // Select default...
            __FactorMethod_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __FactorMethod_JComboBox,FactorMethod, JGUIUtil.NONE, null, null ) ) {
                __FactorMethod_JComboBox.select ( FactorMethod );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nFactorMethod value \"" + FactorMethod +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( AnalysisStart != null ) {
            __AnalysisStart_JTextField.setText ( AnalysisStart );
        }
        if ( AnalysisEnd != null ) {
            __AnalysisEnd_JTextField.setText ( AnalysisEnd );
        }
        if ( InitialValue == null ) {
            // Select default...
            __InitialValue_JComboBox.select ( 0 );
        }
        else {  if (    JGUIUtil.isSimpleJComboBoxItem(
                __InitialValue_JComboBox, InitialValue, JGUIUtil.NONE, null, null)){
                __InitialValue_JComboBox.select( InitialValue);
            }
            else if ( StringUtil.isDouble(InitialValue) ) {
                __InitialValue_JComboBox.setText ( InitialValue );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nInitialValue \"" + InitialValue +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
    }
    // Regardless, reset the command from the fields...
    TSList = __TSList_JComboBox.getSelected();
    TSID = __TSID_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
    IndependentTSID = __IndependentTSID_JComboBox.getSelected();
    FillStart = __FillStart_JTextField.getText().trim();
    FillEnd = __FillEnd_JTextField.getText().trim();
    FillFlag = __FillFlag_JTextField.getText().trim();
    FillDirection = __FillDirection_JComboBox.getSelected();
    FactorMethod = __FactorMethod_JComboBox.getSelected();
    AnalysisStart = __AnalysisStart_JTextField.getText().trim();
    AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
    InitialValue = __InitialValue_JComboBox.getSelected();
    props = new PropList ( __command.getCommandName() );
    props.add ( "TSList=" + TSList );
    props.add ( "TSID=" + TSID );
    props.add ( "EnsembleID=" + EnsembleID );
    props.add ( "IndependentTSID=" + IndependentTSID );
    props.add ( "FillStart=" + FillStart );
    props.add ( "FillEnd=" + FillEnd );
    props.add ( "FillFlag=" + FillFlag );
    props.add ( "FillDirection=" + FillDirection);
    props.add ( "FactorMethod=" + FactorMethod);
    props.add ( "AnalysisStart=" + AnalysisStart);
    props.add ( "AnalysisEnd=" + AnalysisEnd);
    props.add ( "InitialValue=" + InitialValue);
    __command_JTextArea.setText( __command.toString ( props ) );
}

/**
React to the user response.
@param ok if false, then the edit is cancelled.  If true, the edit is committed
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