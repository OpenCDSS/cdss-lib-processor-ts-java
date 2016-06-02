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
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;
import RTi.Util.GUI.InputFilterStringCriterionType;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Command editor dialog for the SelectTimeSeries() command.
*/
public class SelectTimeSeries_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SelectTimeSeries_Command __command = null;
private JTextArea __command_JTextArea = null;
private JTabbedPane __main_JTabbedPane = null;
private SimpleJComboBox __TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox __TSID_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private JLabel __TSPosition_JLabel = null;
private JTextField __TSPosition_JTextField = null;
private JTextField __PropertyName_JTextField = null;
private SimpleJComboBox __PropertyCriterion_JComboBox = null;
private JTextField __PropertyValue_JTextField = null;
private SimpleJComboBox	__DeselectAllFirst_JComboBox = null;
private SimpleJComboBox __IfNotFound_JComboBox = null;
private JTextField __SelectCountProperty_JTextField = null;

private boolean __error_wait = false;
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK button has been pressed.

/**
Command editor dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public SelectTimeSeries_JDialog ( JFrame parent, SelectTimeSeries_Command command )
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
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
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
    if ( TSListType.TSPOSITION.equals(TSList)) {
        __TSPosition_JTextField.setEnabled(true);
        __TSPosition_JLabel.setEnabled ( true );
    }
    else {
        __TSPosition_JTextField.setEnabled(false);
        __TSPosition_JLabel.setEnabled ( false );
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
    String TSPosition = __TSPosition_JTextField.getText().trim();
    String DeselectAllFirst = __DeselectAllFirst_JComboBox.getSelected();
    String IfNotFound = __IfNotFound_JComboBox.getSelected();
    String SelectCountProperty = __SelectCountProperty_JTextField.getText().trim();
    String PropertyName = __PropertyName_JTextField.getText().trim();
    String PropertyCriterion = __PropertyCriterion_JComboBox.getSelected();
    String PropertyValue = __PropertyValue_JTextField.getText().trim();
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
    if ( TSPosition.length() > 0 ) {
        parameters.set ( "TSPosition", TSPosition );
    }
    if ( DeselectAllFirst.length() > 0 ) {
        parameters.set ( "DeselectAllFirst", DeselectAllFirst );
    }
    if ( IfNotFound.length() > 0 ) {
        parameters.set ( "IfNotFound", IfNotFound );
    }
    if ( SelectCountProperty.length() > 0 ) {
        parameters.set ( "SelectCountProperty", SelectCountProperty );
    }
    if ( PropertyName.length() > 0 ) {
        parameters.set ( "PropertyName", PropertyName );
    }
    if ( PropertyCriterion.length() > 0 ) {
        parameters.set ( "PropertyCriterion", PropertyCriterion );
    }
    if ( PropertyValue.length() > 0 ) {
        parameters.set ( "PropertyValue", PropertyValue );
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
    String TSPosition = __TSPosition_JTextField.getText().trim();
    String DeselectAllFirst = __DeselectAllFirst_JComboBox.getSelected();
    String IfNotFound = __IfNotFound_JComboBox.getSelected();
    String SelectCountProperty = __SelectCountProperty_JTextField.getText().trim();
    String PropertyName = __PropertyName_JTextField.getText().trim();
    String PropertyCriterion = __PropertyCriterion_JComboBox.getSelected();
    String PropertyValue = __PropertyValue_JTextField.getText().trim();
    __command.setCommandParameter ( "TSList", TSList );
    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
    __command.setCommandParameter ( "TSPosition", TSPosition );
    __command.setCommandParameter ( "DeselectAllFirst", DeselectAllFirst );
    __command.setCommandParameter ( "IfNotFound", IfNotFound );
    __command.setCommandParameter ( "SelectCountProperty", SelectCountProperty );
    __command.setCommandParameter ( "PropertyName", PropertyName );
    __command.setCommandParameter ( "PropertyCriterion", PropertyCriterion );
    __command.setCommandParameter ( "PropertyValue", PropertyValue );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, SelectTimeSeries_Command command )
{	__command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"This command selects time series, similar to how time series are interactively selected in TSTool results."),
    	0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Selected time series may then be used by other commands using the TSList=SelectedTS parameter."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"For example, commands may allow selected time series to be processed, rather than defaulting to all time series."),
    	0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Selections can be specified by the methods listed in the following tabs."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
   	
   	__main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
   	
    // Panel for TSList
    int yList = -1;
    JPanel list_JPanel = new JPanel();
    list_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "TS List", list_JPanel );
    
    JGUIUtil.addComponent(list_JPanel, new JLabel (
        "When matching a time series identifier (TSID) pattern:"),
        0, ++yList, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(list_JPanel, new JLabel (
        "    The dot-delimited time series identifier parts are " +
        "Location.DataSource.DataType.Interval.Scenario"),
        0, ++yList, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(list_JPanel, new JLabel (
        "    The pattern used to select/deselect time series will be " +
        "matched against aliases and identifiers."),
        0, ++yList, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(list_JPanel, new JLabel (
        "    Use * to match all time series."),
        0, ++yList, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(list_JPanel, new JLabel (
        "    Use A* to match all time series with alias or location starting with A."),
        0, ++yList, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(list_JPanel, new JLabel (
        "    Use *.*.XXXXX.*.* to match all time series with a data type XXXXX."),
        0, ++yList, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(list_JPanel, new JLabel (
    "<html>When selecting time series by specifying time series positions (<b>not recommended for production" +
    " work because positions may change</b>):</html>"),
    0, ++yList, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(list_JPanel, new JLabel (
    "    The first time series created is position 1."),
    0, ++yList, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(list_JPanel, new JLabel (
    "    Separate numbers by a comma.  Specify a range, for example, as 1-3.  A valid combination is: 1,5-10,13"),
    0, ++yList, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    __TSList_JComboBox = new SimpleJComboBox(false);
    yList = CommandEditorUtil.addTSListToEditorDialogPanel ( this, list_JPanel, __TSList_JComboBox, yList );
    // Remove SelectedTS from list since it would be redundant with this command
    __TSList_JComboBox.remove ( TSListType.SELECTED_TS.toString() );
    // Add the non-standard choice
    __TSList_JComboBox.add( TSListType.TSPOSITION.toString());

    __TSID_JLabel = new JLabel ("TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __TSID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __TSID_JComboBox.setToolTipText("Select a time series TSID/alias from the list or specify with ${Property} notation");
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    yList = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, list_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, yList );
    
    __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __EnsembleID_JComboBox.setToolTipText("Select an ensemble identifier from the list or specify with ${Property} notation");
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    yList = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
            this, this, list_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, yList );
    
    __TSPosition_JLabel = new JLabel ("Time series position(s) (for TSList=" + TSListType.TSPOSITION.toString() + "):");
    JGUIUtil.addComponent(list_JPanel, __TSPosition_JLabel,
		0, ++yList, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__TSPosition_JTextField = new JTextField ( "", 8 );
	__TSPosition_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(list_JPanel, __TSPosition_JTextField,
		1, yList, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(list_JPanel, new JLabel ( "For example, 1,2,7-8 (positions are 1+)." ),
		3, yList, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        
    // Panel for property
    int yProp = -1;
    JPanel prop_JPanel = new JPanel();
    prop_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Match Property", prop_JPanel );

    JGUIUtil.addComponent(prop_JPanel, new JLabel (
        "Time series can be matched by specifying a string property."),
        0, ++yProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel (
        "Property checks are additive to the TSList parameter."),
        0, ++yProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel ("Comparisons are case-independent."),
        0, ++yProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(prop_JPanel, new JLabel ( "Property name:" ), 
        0, ++yProp, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PropertyName_JTextField = new JTextField ( 20 );
    __PropertyName_JTextField.setToolTipText("Specify the property name to compare, can use ${Property} notation");
    __PropertyName_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(prop_JPanel, __PropertyName_JTextField,
        1, yProp, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel("Required - property name to match."), 
        3, yProp, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(prop_JPanel, new JLabel ( "Property criterion:" ), 
        0, ++yProp, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PropertyCriterion_JComboBox = new SimpleJComboBox ( false );
    __PropertyCriterion_JComboBox.addItem ( "" );
    __PropertyCriterion_JComboBox.addItem ( "" + InputFilterStringCriterionType.CONTAINS );
    __PropertyCriterion_JComboBox.addItem ( "" + InputFilterStringCriterionType.ENDS_WITH );
    __PropertyCriterion_JComboBox.addItem ( "" + InputFilterStringCriterionType.MATCHES );
    __PropertyCriterion_JComboBox.addItem ( "" + InputFilterStringCriterionType.STARTS_WITH );
    __PropertyCriterion_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(prop_JPanel, __PropertyCriterion_JComboBox,
        1, yProp, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel("Required - creterion for to match property."), 
        3, yProp, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(prop_JPanel, new JLabel ( "Property value:" ), 
        0, ++yProp, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PropertyValue_JTextField = new JTextField ( 20 );
    __PropertyValue_JTextField.setToolTipText("Specify property value to match or specify with ${Property} notation");
    __PropertyValue_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(prop_JPanel, __PropertyValue_JTextField,
        1, yProp, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel("Required - property value to match."), 
        3, yProp, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Panel for statistic
    int yStat = -1;
    JPanel stat_JPanel = new JPanel();
    stat_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Check Statistic", stat_JPanel );

    JGUIUtil.addComponent(stat_JPanel, new JLabel (
        "To select time series that have a specific statistic value:"),
        0, ++yStat, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(stat_JPanel, new JLabel (
        "1) Check for the statistic using the CheckTimeSeriesStatistic() command and set a property in that command."),
        0, ++yStat, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(stat_JPanel, new JLabel (
        "2) Select time series with the property using the parameters in the Match Property tab of this command."),
        0, ++yStat, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Remaining general parameters

    List<String> select_all_first = new Vector ( 3 );
	select_all_first.add ( "" );
	select_all_first.add ( __command._False );
	select_all_first.add ( __command._True );
    	JGUIUtil.addComponent(main_JPanel, new JLabel ( "Deselect all first?:" ),
	0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__DeselectAllFirst_JComboBox = new SimpleJComboBox ( true );
	__DeselectAllFirst_JComboBox.setData ( select_all_first );
	__DeselectAllFirst_JComboBox.addItemListener ( this );
    	JGUIUtil.addComponent(main_JPanel, __DeselectAllFirst_JComboBox,
	1, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - eliminates need for separate deselect (default=" +
        __command._False + ")."),
	3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel,new JLabel("If time series not found?:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __IfNotFound_JComboBox = new SimpleJComboBox ( false );
    __IfNotFound_JComboBox.addItem ( "" );
    __IfNotFound_JComboBox.addItem ( __command._Ignore );
    __IfNotFound_JComboBox.addItem ( __command._Warn );
    __IfNotFound_JComboBox.addItem ( __command._Fail );
    __IfNotFound_JComboBox.select ( 0 );
    __IfNotFound_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __IfNotFound_JComboBox,
        1, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - how to handle case of nothing matched (default=" + __command._Fail + ")."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel("Select count property:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SelectCountProperty_JTextField = new JTextField ( "", 20 );
    __SelectCountProperty_JTextField.setToolTipText("Specify name of the property to set to the selected count, or specify with ${Property} notation");
    __SelectCountProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __SelectCountProperty_JTextField,
        1, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - processor property to set for number selected." ),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
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

	__cancel_JButton = new SimpleJButton("Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__ok_JButton = new SimpleJButton("OK", this);
	button_JPanel.add ( __ok_JButton );

	setTitle ( "Edit " + __command.getCommandName() + "() Command" );

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
public void itemStateChanged ( ItemEvent event )
{	if ( event.getStateChange() != ItemEvent.SELECTED ) {
		return;
	}
    checkGUIState();
	refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	int code = event.getKeyCode();

	if ( code == KeyEvent.VK_ENTER ) {
		refresh ();
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
{   return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{	String routine = __command + ".refresh";
    String TSList = "";
	String TSID = "";
	String EnsembleID = "";
	String TSPosition = "";
	String DeselectAllFirst = "";
    String IfNotFound = "";
	String SelectCountProperty = "";
    String PropertyName = "";
    String PropertyCriterion = "";
    String PropertyValue = "";
    PropList props = __command.getCommandParameters();
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command...
        TSList = props.getValue ( "TSList" );
		TSID = props.getValue ( "TSID" );
		EnsembleID = props.getValue ( "EnsembleID" );
		TSPosition = props.getValue ( "TSPosition" );
		DeselectAllFirst = props.getValue ( "DeselectAllFirst" );
		IfNotFound = props.getValue ( "IfNotFound" );
		SelectCountProperty = props.getValue ( "SelectCountProperty" );
        PropertyName = props.getValue ( "PropertyName" );
        PropertyCriterion = props.getValue ( "PropertyCriterion" );
        PropertyValue = props.getValue ( "PropertyValue" );
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
		if ( TSPosition != null ) {
			__TSPosition_JTextField.setText ( TSPosition );
		}
		if ( DeselectAllFirst == null ) {
			// Select blank...
			__DeselectAllFirst_JComboBox.select ( 0 );
		}
		else {
		    if ( JGUIUtil.isSimpleJComboBoxItem(__DeselectAllFirst_JComboBox,
				DeselectAllFirst, JGUIUtil.NONE, null, null ) ){
				__DeselectAllFirst_JComboBox.select ( DeselectAllFirst );
			}
			else {
			    Message.printWarning ( 1, routine, "Existing " + __command + "() references an " +
				"invalid\nDeselectAllFirst \"" + DeselectAllFirst + "\".  Select a different value or Cancel.");
				__error_wait = true;
			}
		}
        if ( __IfNotFound_JComboBox != null ) {
            if ( IfNotFound == null ) {
                // Select default...
                __IfNotFound_JComboBox.select ( 0 );
            }
            else {
                if ( JGUIUtil.isSimpleJComboBoxItem(__IfNotFound_JComboBox, IfNotFound, JGUIUtil.NONE, null, null ) ) {
                    __IfNotFound_JComboBox.select ( IfNotFound );
                }
                else {
                    Message.printWarning ( 1, routine,
                    "Existing command references an invalid\n"+
                    "IfNotFound \"" + IfNotFound + "\".  Select a\ndifferent value or Cancel." );
                }
            }
        }
        if ( SelectCountProperty != null ) {
            __SelectCountProperty_JTextField.setText ( SelectCountProperty );
        }
        if ( PropertyName != null ) {
            __PropertyName_JTextField.setText ( PropertyName );
        }
        if ( PropertyCriterion == null ) {
            // Select default...
            __PropertyCriterion_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __PropertyCriterion_JComboBox,PropertyCriterion, JGUIUtil.NONE, null, null ) ) {
                __PropertyCriterion_JComboBox.select ( PropertyCriterion );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nPropertyCriterion value \"" + PropertyCriterion +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( PropertyValue != null ) {
            __PropertyValue_JTextField.setText ( PropertyValue );
        }
	}
	// Regardless, reset the command from the fields...
    TSList = __TSList_JComboBox.getSelected();
    TSID = __TSID_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
    TSPosition = __TSPosition_JTextField.getText().trim();
	DeselectAllFirst = __DeselectAllFirst_JComboBox.getSelected();
    IfNotFound = __IfNotFound_JComboBox.getSelected();
	SelectCountProperty = __SelectCountProperty_JTextField.getText().trim();
    PropertyName = __PropertyName_JTextField.getText().trim();
    PropertyCriterion = __PropertyCriterion_JComboBox.getSelected();
    PropertyValue = __PropertyValue_JTextField.getText().trim();
    props = new PropList ( __command.getCommandName() );
    props.add ( "TSList=" + TSList );
    props.add ( "TSID=" + TSID );
    props.add ( "EnsembleID=" + EnsembleID );
    props.add ( "TSPosition=" + TSPosition );
    props.add ( "DeselectAllFirst=" + DeselectAllFirst );
    props.add ( "IfNotFound=" + IfNotFound );
    props.add ( "SelectCountProperty=" + SelectCountProperty );
    props.add ( "PropertyName=" + PropertyName );
    props.add ( "PropertyCriterion=" + PropertyCriterion );
    props.add ( "PropertyValue=" + PropertyValue );
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