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

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;
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
public class ReplaceValue_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private SimpleJComboBox __TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox __TSID_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private ReplaceValue_Command __command = null;
private JTextArea __command_JTextArea=null;
private JTextField __MinValue_JTextField = null;
private JTextField __MaxValue_JTextField = null;
private JTextField __MatchFlag_JTextField = null;
private JTextField __NewValue_JTextField = null;
private SimpleJComboBox __Action_JComboBox = null;
private JTextField __SetStart_JTextField = null;
private JTextField __SetEnd_JTextField = null;
private JCheckBox __AnalysisWindow_JCheckBox = null;
private DateTime_JPanel __AnalysisWindowStart_JPanel = null;
private DateTime_JPanel __AnalysisWindowEnd_JPanel = null;
private JTextField __SetFlag_JTextField = null;
private JTextField __SetFlagDesc_JTextField = null;
private boolean __error_wait = false; // Is there an error waiting to be cleared up
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK button has been pressed.

/**
Command editor dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public ReplaceValue_JDialog ( JFrame parent, ReplaceValue_Command command )
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
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "ReplaceValue");
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
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{   // Put together a list of parameters to check...
    PropList parameters = new PropList ( "" );
    String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String MinValue = __MinValue_JTextField.getText().trim();
    String MaxValue = __MaxValue_JTextField.getText().trim();
    String MatchFlag = __MatchFlag_JTextField.getText().trim();
    String NewValue = __NewValue_JTextField.getText().trim();
    String SetStart = __SetStart_JTextField.getText().trim();
    String SetEnd = __SetEnd_JTextField.getText().trim();
    String SetFlag = __SetFlag_JTextField.getText().trim();
    String SetFlagDesc = __SetFlagDesc_JTextField.getText().trim();
    String Action = __Action_JComboBox.getSelected();
    
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
    if ( MinValue.length() > 0 ) {
        parameters.set ( "MinValue", MinValue );
    }
    if ( MaxValue.length() > 0 ) {
        parameters.set ( "MaxValue", MaxValue );
    }
    if ( MatchFlag.length() > 0 ) {
        parameters.set ( "MatchFlag", MatchFlag );
    }
    if ( NewValue.length() > 0 ) {
        parameters.set ( "NewValue", NewValue );
    }
    if ( Action.length() > 0 ) {
        parameters.set ( "Action", Action );
    }
    if ( SetStart.length() > 0 ) {
        parameters.set ( "SetStart", SetStart );
    }
    if ( SetEnd.length() > 0 ) {
        parameters.set ( "SetEnd", SetEnd );
    }
    if ( __AnalysisWindow_JCheckBox.isSelected() ){
        String AnalysisWindowStart = __AnalysisWindowStart_JPanel.toString(false,true).trim();
        String AnalysisWindowEnd = __AnalysisWindowEnd_JPanel.toString(false,true).trim();
        if ( AnalysisWindowStart.length() > 0 ) {
            parameters.set ( "AnalysisWindowStart", AnalysisWindowStart );
        }
        if ( AnalysisWindowEnd.length() > 0 ) {
            parameters.set ( "AnalysisWindowEnd", AnalysisWindowEnd );
        }
    }
    if ( SetFlag.length() > 0 ) {
        parameters.set ( "SetFlag", SetFlag );
    }
    if ( SetFlagDesc.length() > 0 ) {
        parameters.set ( "SetFlagDesc", SetFlagDesc );
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
    String MaxValue = __MaxValue_JTextField.getText().trim();
    String MinValue = __MinValue_JTextField.getText().trim();
    String MatchFlag = __MatchFlag_JTextField.getText().trim();
    String NewValue = __NewValue_JTextField.getText().trim();
    String SetStart = __SetStart_JTextField.getText().trim();
    String SetEnd = __SetEnd_JTextField.getText().trim();
    String Action = __Action_JComboBox.getSelected();
    String SetFlag = __SetFlag_JTextField.getText().trim();
    String SetFlagDesc = __SetFlagDesc_JTextField.getText().trim();
    __command.setCommandParameter ( "TSList", TSList );
    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
    __command.setCommandParameter ( "MaxValue", MaxValue );
    __command.setCommandParameter ( "MinValue", MinValue );
    __command.setCommandParameter ( "MatchFlag", MatchFlag );
    __command.setCommandParameter ( "NewValue", NewValue );
    __command.setCommandParameter ( "SetStart", SetStart );
    __command.setCommandParameter ( "SetEnd", SetEnd );
    __command.setCommandParameter ( "Action", Action );
    if ( __AnalysisWindow_JCheckBox.isSelected() ){
        String AnalysisWindowStart = __AnalysisWindowStart_JPanel.toString(false,true).trim();
        String AnalysisWindowEnd = __AnalysisWindowEnd_JPanel.toString(false,true).trim();
        __command.setCommandParameter ( "AnalysisWindowStart", AnalysisWindowStart );
        __command.setCommandParameter ( "AnalysisWindowEnd", AnalysisWindowEnd );
    }
    __command.setCommandParameter ( "SetFlag", SetFlag );
    __command.setCommandParameter ( "SetFlagDesc", SetFlagDesc );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, ReplaceValue_Command command )
{	__command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);
    Insets insetsMin = insetsTLBR;

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Replace a single data value or range of data values with a constant."),
		0, ++y, 7, 1, 0, 0, insetsMin, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The data values and/or flags are matched to determine values to replace."),
        0, ++y, 7, 1, 0, 0, insetsMin, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optionally, set missing or remove the values entirely (if an irregular interval time series)."),
        0, ++y, 7, 1, 0, 0, insetsMin, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "If the missing value indicator is a number in the given range, missing values also will be replaced." ),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specify dates with precision appropriate for the data." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    __TSList_JComboBox = new SimpleJComboBox(false);
    y = CommandEditorUtil.addTSListToEditorDialogPanel ( this, main_JPanel, __TSList_JComboBox, y );

    __TSID_JLabel = new JLabel ("TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __TSID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __TSID_JComboBox.setToolTipText("Select a time series TSID/alias from the list or specify with ${Property} notation");
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, main_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, y );
    
    __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __EnsembleID_JComboBox.setToolTipText("Select an ensemble identifier from the list or specify with ${Property} notation");
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
            this, this, main_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, y );

    JGUIUtil.addComponent(main_JPanel, new JLabel("Minimum value to replace:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__MinValue_JTextField = new JTextField ( 10 );
	__MinValue_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __MinValue_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional (maximum value also can be specified)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel( "Maximum value to replace:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__MaxValue_JTextField = new JTextField ( 10 );
	__MaxValue_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __MaxValue_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - use when specifying range."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Flag to match for replace:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MatchFlag_JTextField = new JTextField ( 10 );
    __MatchFlag_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __MatchFlag_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - use when matching flags."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Constant value to replace with:"), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__NewValue_JTextField = new JTextField ( 10 );
	__NewValue_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __NewValue_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Required - or specify action."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Action:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Action_JComboBox = new SimpleJComboBox ( 12, false ); // Do not allow edit
    List<String> actionChoices = new Vector<String>();
    actionChoices.add("");
    actionChoices.add(__command._Remove);
    actionChoices.add(__command._SetMissing);
    __Action_JComboBox.setData ( actionChoices );
    __Action_JComboBox.select(0);
    __Action_JComboBox.addItemListener ( this );
    __Action_JComboBox.setMaximumRowCount(actionChoices.size());
    JGUIUtil.addComponent(main_JPanel, __Action_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Optional - action for matched values (default=just replace)."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Replacement start:"), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SetStart_JTextField = new JTextField (20);
    __SetStart_JTextField.setToolTipText("Specify the set start using a date/time string or ${Property} notation");
    __SetStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __SetStart_JTextField,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - start of replacement (default is all)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Replacement end:"), 
        0, ++y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SetEnd_JTextField = new JTextField (20);
    __SetEnd_JTextField.setToolTipText("Specify the set end using a date/time string or ${Property} notation");
    __SetEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __SetEnd_JTextField,
        1, y, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - end of replacement (default is all)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    __AnalysisWindow_JCheckBox = new JCheckBox ( "Analysis window:", false );
    __AnalysisWindow_JCheckBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __AnalysisWindow_JCheckBox, 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    JPanel analysisWindow_JPanel = new JPanel();
    analysisWindow_JPanel.setLayout(new GridBagLayout());
    __AnalysisWindowStart_JPanel = new DateTime_JPanel ( "Start", TimeInterval.MONTH, TimeInterval.MINUTE, null );
    __AnalysisWindowStart_JPanel.addActionListener(this);
    __AnalysisWindowStart_JPanel.addKeyListener ( this );
    JGUIUtil.addComponent(analysisWindow_JPanel, __AnalysisWindowStart_JPanel,
        1, 0, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    // TODO SAM 2008-01-23 Figure out how to display the correct limits given the time series interval
    __AnalysisWindowEnd_JPanel = new DateTime_JPanel ( "End", TimeInterval.MONTH, TimeInterval.MINUTE, null );
    __AnalysisWindowEnd_JPanel.addActionListener(this);
    __AnalysisWindowEnd_JPanel.addKeyListener ( this );
    JGUIUtil.addComponent(analysisWindow_JPanel, __AnalysisWindowEnd_JPanel,
        4, 0, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, analysisWindow_JPanel,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - analysis window within each year (default=full year)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel,new JLabel( "Set flag:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SetFlag_JTextField = new JTextField ( "", 10 );
    __SetFlag_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __SetFlag_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - string to mark replaced data."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel,new JLabel( "Set flag description:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SetFlagDesc_JTextField = new JTextField ( "", 20 );
    __SetFlagDesc_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __SetFlagDesc_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - description for set flag (default=auto-generated)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
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
		// Only return from dialog if specifying the new value...
		if ( event.getComponent().equals(__NewValue_JTextField) ) {
			checkInput();
			if ( !__error_wait ) {
				response ( true );
			}
		}
	}
	refresh();
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
{   String routine = getClass().getSimpleName() + ".refresh";
    String TSList = "";
    String TSID = "";
    String EnsembleID = "";
    String MinValue = "";
    String MaxValue = "";
    String MatchFlag = "";
    String NewValue = "";
    String Action = "";
    String SetStart = "";
    String SetEnd = "";
    String AnalysisWindowStart = "";
    String AnalysisWindowEnd = "";
    String SetFlag = "";
    String SetFlagDesc = "";
    PropList props = __command.getCommandParameters();
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command...
        TSList = props.getValue ( "TSList" );
        TSID = props.getValue ( "TSID" );
        EnsembleID = props.getValue ( "EnsembleID" );
        MinValue = props.getValue ( "MinValue" );
        MaxValue = props.getValue ( "MaxValue" );
        MatchFlag = props.getValue ( "MatchFlag" );
        NewValue = props.getValue ( "NewValue" );
        Action = props.getValue ( "Action" );
        SetStart = props.getValue ( "SetStart" );
        SetEnd = props.getValue ( "SetEnd" );
        AnalysisWindowStart = props.getValue ( "AnalysisWindowStart" );
        AnalysisWindowEnd = props.getValue ( "AnalysisWindowEnd" );
        SetFlag = props.getValue ( "SetFlag" );
        SetFlagDesc = props.getValue ( "SetFlagDesc" );
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
        if ( MinValue != null ) {
            __MinValue_JTextField.setText( MinValue );
        }
        if ( MaxValue != null ) {
            __MaxValue_JTextField.setText( MaxValue );
        }
        if ( MatchFlag != null ) {
            __MatchFlag_JTextField.setText( MatchFlag );
        }
        if ( NewValue != null ) {
            __NewValue_JTextField.setText( NewValue );
        }
        if ( SetStart != null ) {
            __SetStart_JTextField.setText( SetStart );
        }
        if ( SetEnd != null ) {
            __SetEnd_JTextField.setText( SetEnd );
        }
        if ( Action == null ) {
            // Select default...
            __Action_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __Action_JComboBox,Action, JGUIUtil.NONE, null, null ) ) {
                __Action_JComboBox.select ( Action );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nAction value \"" + Action +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
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
        if ( SetFlag != null ) {
            __SetFlag_JTextField.setText ( SetFlag );
        }
        if ( SetFlagDesc != null ) {
            __SetFlagDesc_JTextField.setText ( SetFlagDesc );
        }
    }
    // Regardless, reset the command from the fields...
    checkGUIState();
    TSList = __TSList_JComboBox.getSelected();
    TSID = __TSID_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
    MinValue = __MinValue_JTextField.getText().trim();
    MaxValue = __MaxValue_JTextField.getText().trim();
    MatchFlag = __MatchFlag_JTextField.getText().trim();
    NewValue = __NewValue_JTextField.getText().trim();
    Action = __Action_JComboBox.getSelected();
    SetStart = __SetStart_JTextField.getText().trim();
    SetEnd = __SetEnd_JTextField.getText().trim();
    SetFlag = __SetFlag_JTextField.getText().trim();
    SetFlagDesc = __SetFlagDesc_JTextField.getText().trim();
    props = new PropList ( __command.getCommandName() );
    props.add ( "TSList=" + TSList );
    props.add ( "TSID=" + TSID );
    props.add ( "EnsembleID=" + EnsembleID );
    props.add ( "MinValue=" + MinValue );
    props.add ( "MaxValue=" + MaxValue );
    props.add ( "MatchFlag=" + MatchFlag );
    props.add ( "NewValue=" + NewValue );
    props.add ( "Action=" + Action );
    props.add ( "SetStart=" + SetStart );
    props.add ( "SetEnd=" + SetEnd );
    if ( __AnalysisWindow_JCheckBox.isSelected() ) {
        AnalysisWindowStart = __AnalysisWindowStart_JPanel.toString(false,true).trim();
        AnalysisWindowEnd = __AnalysisWindowEnd_JPanel.toString(false,true).trim();
        props.add ( "AnalysisWindowStart=" + AnalysisWindowStart );
        props.add ( "AnalysisWindowEnd=" + AnalysisWindowEnd );
    }
    props.add ( "SetFlag=" + SetFlag );
    props.add ( "SetFlagDesc=" + SetFlagDesc );
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