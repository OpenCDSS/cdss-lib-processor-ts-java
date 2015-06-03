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

import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;

import java.util.List;
import java.util.Vector;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.IO.Command;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;

/**
Editor dialog for the Add() command.
*/
public class Add_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, ListSelectionListener, WindowListener
{

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private Add_Command __command = null;
private JTextArea __command_JTextArea=null;
private SimpleJComboBox	__TSID_JComboBox = null; // To receive 
private SimpleJComboBox __EnsembleID_JComboBox = null; // To receive
private SimpleJComboBox	__AddTSList_JComboBox = null; // To supply time series to add...
private JLabel __AddTSID_JLabel = null;
private SimpleJComboBox __AddTSID_JComboBox = null;
private JLabel __AddEnsembleID_JLabel = null;
private SimpleJComboBox __AddEnsembleID_JComboBox = null;
private JLabel __AddSpecifiedTSID_JLabel = null;
private DefaultListModel __AddSpecifiedTSID_JListModel = null;
private JList __AddSpecifiedTSID_JList= null;
private SimpleJComboBox	__HandleMissingHow_JComboBox = null; // How to handle missing data in time series.
private SimpleJComboBox __IfTSListToAddIsEmpty_JComboBox = null;
private boolean	__error_wait = false;
private boolean	__first_time = true;
private boolean __ok = false; // Indicates whether OK button has been pressed.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public Add_JDialog ( JFrame parent, Command command )
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
		refresh();
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
    String TSList = __AddTSList_JComboBox.getSelected();
    if ( TSListType.ALL_MATCHING_TSID.equals(TSList) ||
            TSListType.FIRST_MATCHING_TSID.equals(TSList) ||
            TSListType.LAST_MATCHING_TSID.equals(TSList) ) {
        __AddTSID_JComboBox.setEnabled(true);
        __AddTSID_JLabel.setEnabled ( true );
    }
    else {
        __AddTSID_JComboBox.setEnabled(false);
        __AddTSID_JLabel.setEnabled ( false );
    }
    if ( TSListType.ENSEMBLE_ID.equals(TSList)) {
        __AddEnsembleID_JComboBox.setEnabled(true);
        __AddEnsembleID_JLabel.setEnabled ( true );
    }
    else {
        __AddEnsembleID_JComboBox.setEnabled(false);
        __AddEnsembleID_JLabel.setEnabled ( false );
    }
    if ( TSListType.SPECIFIED_TSID.equals(TSList)) {
        __AddSpecifiedTSID_JList.setEnabled(true);
        __AddSpecifiedTSID_JLabel.setEnabled ( true );
    }
    else {
        __AddSpecifiedTSID_JList.setEnabled(false);
        __AddSpecifiedTSID_JLabel.setEnabled ( false );
    }
}

/**
Check the user input for errors and set __error_wait accordingly.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
    PropList props = new PropList ( "" );
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String AddTSList = __AddTSList_JComboBox.getSelected();
    String AddTSID = __AddTSID_JComboBox.getSelected();
    String AddSpecifiedTSID = getAddSpecifiedTSIDFromList();
    String AddEnsembleID = __AddEnsembleID_JComboBox.getSelected();
    //String SetStart = __SetStart_JTextField.getText().trim();
   // String SetEnd = __SetEnd_JTextField.getText().trim();
    String HandleMissingHow = __HandleMissingHow_JComboBox.getSelected();
    String IfTSListToAddIsEmpty = __IfTSListToAddIsEmpty_JComboBox.getSelected();
    __error_wait = false;
    
    // AddTSID is used for several variations of AddTSList
    if ( TSListType.SPECIFIED_TSID.equals(AddTSList) ) {
        AddTSID = AddSpecifiedTSID;
    }

    if ( TSID.length() > 0 ) {
        props.set ( "TSID", TSID );
    }
    if ( EnsembleID.length() > 0 ) {
        props.set ( "EnsembleID", EnsembleID );
    }
    if ( AddTSList.length() > 0 ) {
        props.set ( "AddTSList", AddTSList );
    }
    if ( AddTSID.length() > 0 ) {
        props.set ( "AddTSID", AddTSID );
    }
    if ( AddEnsembleID.length() > 0 ) {
        props.set ( "AddEnsembleID", AddEnsembleID );
    }
    /*
    if ( SetStart.length() > 0 ) {
        props.set ( "SetStart", SetStart );
    }
    if ( SetEnd.length() > 0 ) {
        props.set ( "SetEnd", SetEnd );
    }
    */
    if ( HandleMissingHow.length() > 0 ) {
        props.set ( "HandleMissingHow", HandleMissingHow );
    }
    if ( IfTSListToAddIsEmpty.length() > 0 ) {
        props.set ( "IfTSListToAddIsEmpty", IfTSListToAddIsEmpty );
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
{   String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String AddTSList = __AddTSList_JComboBox.getSelected();
    String AddTSID = __AddTSID_JComboBox.getSelected();
    String AddSpecifiedTSID = getAddSpecifiedTSIDFromList();
    String AddEnsembleID = __AddEnsembleID_JComboBox.getSelected();
    String HandleMissingHow = __HandleMissingHow_JComboBox.getSelected();
    //String SetStart = __SetStart_JTextField.getText().trim();
    //String SetEnd = __SetEnd_JTextField.getText().trim();
    //String TransferHow = __TransferHow_JComboBox.getSelected();
    String IfTSListToAddIsEmpty = __IfTSListToAddIsEmpty_JComboBox.getSelected();
    
    // AddTSID is used for several variations of AddTSList
    if ( TSListType.SPECIFIED_TSID.equals(AddTSList) ) {
        AddTSID = AddSpecifiedTSID;
    }
    
    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
    __command.setCommandParameter ( "AddTSList", AddTSList );
    __command.setCommandParameter ( "AddTSID", AddTSID );
    __command.setCommandParameter ( "AddEnsembleID", AddEnsembleID );
    __command.setCommandParameter ( "HandleMissingHow", HandleMissingHow );
    //__command.setCommandParameter ( "SetStart", SetStart );
    //__command.setCommandParameter ( "SetEnd", SetEnd );
    //__command.setCommandParameter ( "TransferHow", TransferHow );
    __command.setCommandParameter ( "IfTSListToAddIsEmpty", IfTSListToAddIsEmpty );
}

/**
Get the AddSpecifiedTSID parameter from the JList and put into a string.
@return a String containing the selected specified time series, separated by commas.
*/
private String getAddSpecifiedTSIDFromList()
{   StringBuffer buffer = new StringBuffer();
    if ( JGUIUtil.selectedSize(__AddSpecifiedTSID_JList) > 0 ) {
        // Get the selected and format...
        int selected[] = __AddSpecifiedTSID_JList.getSelectedIndices();
        int size = JGUIUtil.selectedSize(__AddSpecifiedTSID_JList);
        for ( int i = 0; i < size; i++ ) {
            if ( i > 0 ) {
                buffer.append ( ",");
            }
            buffer.append ( __AddSpecifiedTSID_JListModel.elementAt( selected[i]) );
        }
    }
    return buffer.toString();
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, Command command )
{   __command = (Add_Command)command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

   	JGUIUtil.addComponent(main_JPanel,
		new JLabel ( "Add one or more time series to a time series (or ensemble of time series)." +
		"  The receiving time series (or ensemble) is modified."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The time series to be added are selected using the AddTSList parameter:"),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    y = CommandEditorUtil.addTSListNotesWithSpecifiedTSIDToEditorDialogPanel ( main_JPanel, y );
    JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // Time series to be added to...
    
    JLabel TSID_JLabel = new JLabel ("Time series to receive results:");
    __TSID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __TSID_JComboBox.setToolTipText("Select a time series TSID/alias from the list or specify with ${Property} notation");
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addTSIDToEditorDialogPanel (
            this, this, main_JPanel, TSID_JLabel, __TSID_JComboBox, tsids, y, false );
   
    JLabel EnsembleID_JLabel = new JLabel ("Ensemble to receive results:");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __EnsembleID_JComboBox.setToolTipText("Select an ensemble identifier from the list or specify with ${Property} notation");
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
            this, this, main_JPanel, EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, y );
    
    // The time series to supply values (time series to add)...
    
    __AddTSList_JComboBox = new SimpleJComboBox(false);
    y = CommandEditorUtil.addTSListToEditorDialogPanel (
            this, main_JPanel, new JLabel ("Time series to add (AddTSlist):"), __AddTSList_JComboBox, y );
    // Default is not to add SpecifiedTSID so add it here...
    __AddTSList_JComboBox.add(TSListType.SPECIFIED_TSID.toString());

    __AddTSID_JLabel = new JLabel ("Add TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __AddTSID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __AddTSID_JComboBox.setToolTipText("Select a time series TSID/alias from the list or specify with ${Property} notation");
    tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    // Automatically adds "*"
    y = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, main_JPanel, __AddTSID_JLabel, __AddTSID_JComboBox, tsids, y );
    
    __AddEnsembleID_JLabel = new JLabel ("Add EnsembleID (for AddTSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __AddEnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __AddEnsembleID_JComboBox.setToolTipText("Select a time series TSID/alias from the list or specify with ${Property} notation");
    List<String> AddEnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
            this, this, main_JPanel, __AddEnsembleID_JLabel, __AddEnsembleID_JComboBox, AddEnsembleIDs, y );

    __AddSpecifiedTSID_JLabel =
        new JLabel ("Add specified TSID (for AddTSList=" + TSListType.SPECIFIED_TSID.toString() + "):");
    JGUIUtil.addComponent(main_JPanel, __AddSpecifiedTSID_JLabel,
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__AddSpecifiedTSID_JListModel = new DefaultListModel();
    // Get the list again because above list will have "*" which we don't want
	List<String> tsids2 = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    int size = tsids2.size();
	for ( int i = 0; i < size; i++ ) {
		__AddSpecifiedTSID_JListModel.addElement( (String)tsids2.get(i));
	}
	__AddSpecifiedTSID_JList = new JList ( __AddSpecifiedTSID_JListModel );
    __AddSpecifiedTSID_JList.setVisibleRowCount(Math.min(5,size));
	__AddSpecifiedTSID_JList.addListSelectionListener ( this );
	__AddSpecifiedTSID_JList.addKeyListener ( this );
	__AddSpecifiedTSID_JList.clearSelection();
	DefaultListSelectionModel sm = new DefaultListSelectionModel();
	sm.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
	__AddSpecifiedTSID_JList.setSelectionModel ( sm );
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__AddSpecifiedTSID_JList),
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Handle missing data how?:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__HandleMissingHow_JComboBox = new SimpleJComboBox ( false );
	__HandleMissingHow_JComboBox.addItem ( __command._IgnoreMissing );
	__HandleMissingHow_JComboBox.addItem ( __command._SetMissingIfOtherMissing );
	__HandleMissingHow_JComboBox.addItem ( __command._SetMissingIfAnyMissing );
	__HandleMissingHow_JComboBox.addItemListener ( this );
        JGUIUtil.addComponent(main_JPanel, __HandleMissingHow_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - how to handle missing values in time series (default=" + __command._IgnoreMissing + ")."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "If time series list to add is empty?:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    List<String> IfTSListToAddIsEmptyChoices = new Vector();
    IfTSListToAddIsEmptyChoices.add ( "" );
    IfTSListToAddIsEmptyChoices.add ( __command._Warn );
    IfTSListToAddIsEmptyChoices.add ( __command._Ignore );
    IfTSListToAddIsEmptyChoices.add ( __command._Fail );
    __IfTSListToAddIsEmpty_JComboBox = new SimpleJComboBox ( false );// Do not allow edit
    __IfTSListToAddIsEmpty_JComboBox.setData ( IfTSListToAddIsEmptyChoices );
    __IfTSListToAddIsEmpty_JComboBox.addItemListener ( this );
    //__Statistic_JComboBox.setMaximumRowCount(statisticChoices.size());
    JGUIUtil.addComponent(main_JPanel, __IfTSListToAddIsEmpty_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - action if time series list to add is empty (default=" + __command._Fail + ")."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __command_JTextArea = new JTextArea ( 4, 50 );
    __command_JTextArea.setLineWrap ( true );
    __command_JTextArea.setWrapStyleWord ( true );
    __command_JTextArea.setEditable ( false );
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Refresh the contents...
    checkGUIState();
	refresh();

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	__cancel_JButton = new SimpleJButton("Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__ok_JButton = new SimpleJButton("OK", this);
	button_JPanel.add ( __ok_JButton );

	// Visualize it...

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

    refresh();
	if ( code == KeyEvent.VK_ENTER ) {
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
{	String routine = __command + "_JDialog.refresh";
	String TSID = "";
    String EnsembleID = "";
	String AddTSList = "";
	String AddTSID = "";
    String AddEnsembleID = "";
    String HandleMissingHow = "";
    String IfTSListToAddIsEmpty = "";
    PropList props = __command.getCommandParameters();
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command...
        TSID = props.getValue ( "TSID" );
        EnsembleID = props.getValue ( "EnsembleID" );
        AddTSList = props.getValue ( "AddTSList" );
        AddTSID = props.getValue ( "AddTSID" );
        AddEnsembleID = props.getValue ( "AddEnsembleID" );
        HandleMissingHow = props.getValue ( "HandleMissingHow" );
        IfTSListToAddIsEmpty = props.getValue ( "IfTSListToAddIsEmpty" );
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
        if ( AddTSList == null ) {
            // Select default...
            __AddTSList_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __AddTSList_JComboBox,AddTSList, JGUIUtil.NONE, null, null ) ) {
                __AddTSList_JComboBox.select ( AddTSList );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nAddTSList value \"" + AddTSList +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if (    JGUIUtil.isSimpleJComboBoxItem( __AddTSID_JComboBox, AddTSID,
                JGUIUtil.NONE, null, null ) ) {
                __AddTSID_JComboBox.select ( AddTSID );
        }
        else {  // Automatically add to the list after the blank...
            if ( (AddTSID != null) && (AddTSID.length() > 0) ) {
                if ( !TSListType.SPECIFIED_TSID.equals(AddTSList) ) {
                    __AddTSID_JComboBox.insertItemAt ( AddTSID, 1 );
                    // Select...
                    __AddTSID_JComboBox.select ( AddTSID );
                }
                else {  // Select the blank...
                    __AddTSID_JComboBox.select ( 0 );
                }
            }
            else {  // Select the blank...
                __AddTSID_JComboBox.select ( 0 );
            }
        }
        if ( AddEnsembleID == null ) {
            // Select default...
            __AddEnsembleID_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __AddEnsembleID_JComboBox,AddEnsembleID, JGUIUtil.NONE, null, null ) ) {
                __AddEnsembleID_JComboBox.select ( AddEnsembleID );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nAddEnsembleID value \"" + AddEnsembleID +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        // Setup the list of time series to add, which will be a list of available time series,
        // selecting the ones that were in the previous command.
        //setupAddSpecifiedTSID ( AddTSList, AddSpecifiedTSID );
        setupAddSpecifiedTSID ( AddTSList, AddTSID );
        if ( HandleMissingHow == null ) {
            // Select default...
            __HandleMissingHow_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem(
                __HandleMissingHow_JComboBox, HandleMissingHow, JGUIUtil.NONE, null, null )) {
                __HandleMissingHow_JComboBox.select ( HandleMissingHow );
            }
            else {
                Message.printWarning ( 1, routine, "Existing command references an invalid\n" +
                "HandleMissingHow value \"" + HandleMissingHow + "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( IfTSListToAddIsEmpty == null ) {
            // Select default...
            __IfTSListToAddIsEmpty_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __IfTSListToAddIsEmpty_JComboBox,IfTSListToAddIsEmpty,
                JGUIUtil.NONE, null, null ) ) {
                __IfTSListToAddIsEmpty_JComboBox.select ( IfTSListToAddIsEmpty );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nIfTSListToAddIsEmpty value \"" + IfTSListToAddIsEmpty +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
	}
    // Regardless, reset the command from the fields...
    TSID = __TSID_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
    AddTSList = __AddTSList_JComboBox.getSelected();
    AddTSID = __AddTSID_JComboBox.getSelected();
    String AddSpecifiedTSID = getAddSpecifiedTSIDFromList();
    AddEnsembleID = __AddEnsembleID_JComboBox.getSelected();
    //FillStart = __FillStart_JTextField.getText().trim();
    //FillEnd = __FillEnd_JTextField.getText().trim();
    HandleMissingHow = __HandleMissingHow_JComboBox.getSelected();
    IfTSListToAddIsEmpty = __IfTSListToAddIsEmpty_JComboBox.getSelected();
    // Use the list of specified TSID instead of the __AddTSID_JComboBox above
    if ( TSListType.SPECIFIED_TSID.equals(AddTSList) ) {
        AddTSID = AddSpecifiedTSID;
    }
    props = new PropList ( __command.getCommandName() );
    props.add ( "TSID=" + TSID );
    props.add ( "EnsembleID=" + EnsembleID );
    props.add ( "AddTSList=" + AddTSList );
    props.add ( "AddTSID=" + AddTSID );
    //props.add ( "AddSpecifiedTSID=" + AddSpecifiedTSID );
    props.add ( "AddEnsembleID=" + AddEnsembleID );
    //props.add ( "FillStart=" + FillStart );
    //props.add ( "FillEnd=" + FillEnd );
    props.add ( "HandleMissingHow=" + HandleMissingHow );
    props.add ( "IfTSListToAddIsEmpty=" + IfTSListToAddIsEmpty );
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
Setup the AddSpecifiedTSID list at initialization,
selecting items in the list that match the AddTSID parameter.
@param AddSpecifiedTSID The value of the parameter, of form "TSID,TSID,TSID,...".
*/
private void setupAddSpecifiedTSID ( String AddTSList, String AddSpecifiedTSID )
{   String routine = "Add_JDialog.setupAddSelectedTSID";
    // Check all the items in the list and highlight the ones that match the command being edited...
    if ( (AddTSList != null) && TSListType.SPECIFIED_TSID.equals(AddTSList) && (AddSpecifiedTSID != null) ) {
        // Break list by commas since identifiers may have spaces and other "special" characters (but no commas)
    	List v = StringUtil.breakStringList ( AddSpecifiedTSID, ",", StringUtil.DELIM_SKIP_BLANKS );
        int size = v.size();
        int pos = 0;
        List selected = new Vector();
        String independent = "";
        for ( int i = 0; i < size; i++ ) {
            independent = (String)v.get(i);
            if ( (pos = JGUIUtil.indexOf( __AddSpecifiedTSID_JList, independent, false, true))>= 0 ) {
                // Select it because it is in the command and the list...
                selected.add ( "" + pos );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references a non-existent\n"+
                "specified time series \"" + independent +
                "\".  Select a\n" + "different time series or Cancel." );
            }
        }
        // Select the matched time series...
        if ( selected.size() > 0  ) {
            int [] iselected = new int[selected.size()];
            for ( int is = 0; is < iselected.length; is++ ){
                iselected[is] = StringUtil.atoi ( (String)selected.get(is));
            }
            __AddSpecifiedTSID_JList.setSelectedIndices( iselected );
        }
    }
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
