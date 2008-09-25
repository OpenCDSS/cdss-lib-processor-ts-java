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
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;

import java.util.Vector;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.IO.Command;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;

public class SetToMin_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, ListSelectionListener, WindowListener
{

private SimpleJButton	__cancel_JButton = null,// Cancel Button
			__ok_JButton = null;	// Ok Button
private SetToMin_Command __command = null;
private JTextArea __command_JTextArea=null;
private SimpleJComboBox	__TSID_JComboBox = null;
private SimpleJComboBox __IndependentTSList_JComboBox = null;
private JLabel __IndependentTSID_JLabel = null;
private SimpleJComboBox __IndependentTSID_JComboBox = null;
private JLabel __IndependentEnsembleID_JLabel = null;
private SimpleJComboBox __IndependentEnsembleID_JComboBox = null;
private JLabel __IndependentSpecifiedTSID_JLabel = null;
private DefaultListModel __IndependentSpecifiedTSID_JListModel = null;
private JList __IndependentSpecifiedTSID_JList= null;
private boolean __error_wait = false;
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK button has been pressed.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Time series command to edit.
*/
public SetToMin_JDialog ( JFrame parent, Command command )
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
    // Independent...
    
    String IndependentTSList = __IndependentTSList_JComboBox.getSelected();
    if ( TSListType.ALL_MATCHING_TSID.equals(IndependentTSList) ) {
        __IndependentTSID_JComboBox.setEnabled(true);
        __IndependentTSID_JLabel.setEnabled ( true );
    }
    else {
        __IndependentTSID_JComboBox.setEnabled(false);
        __IndependentTSID_JLabel.setEnabled ( false );
    }
    if ( TSListType.ENSEMBLE_ID.equals(IndependentTSList)) {
        __IndependentEnsembleID_JComboBox.setEnabled(true);
        __IndependentEnsembleID_JLabel.setEnabled ( true );
    }
    else {
        __IndependentEnsembleID_JComboBox.setEnabled(false);
        __IndependentEnsembleID_JLabel.setEnabled ( false );
    }
    if ( TSListType.SPECIFIED_TSID.equals(IndependentTSList)) {
        __IndependentSpecifiedTSID_JList.setEnabled(true);
        __IndependentSpecifiedTSID_JLabel.setEnabled ( true );
    }
    else {
        __IndependentSpecifiedTSID_JList.setEnabled(false);
        __IndependentSpecifiedTSID_JLabel.setEnabled ( false );
    }
}

/**
Check the user input for errors and set __error_wait accordingly.
*/
private void checkInput ()
{   // Put together a list of parameters to check...
    PropList props = new PropList ( "" );
    String TSID = __TSID_JComboBox.getSelected();
    //String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String IndependentTSList = __IndependentTSList_JComboBox.getSelected();
    String IndependentTSID = __IndependentTSID_JComboBox.getSelected();
    String IndependentEnsembleID = __IndependentEnsembleID_JComboBox.getSelected();
    String IndependentSpecifiedTSID = getIndependentSpecifiedTSIDFromList();
    //String SetStart = __SetStart_JTextField.getText().trim();
    //String SetEnd = __SetEnd_JTextField.getText().trim();
    __error_wait = false;
    
    // IndependentTSID is used for several variations of IndependentTSList
    if ( TSListType.SPECIFIED_TSID.equals(IndependentTSList) ) {
        IndependentTSID = IndependentSpecifiedTSID;
    }

    if ( TSID.length() > 0 ) {
        props.set ( "TSID", TSID );
    }
    /*
    if ( EnsembleID.length() > 0 ) {
        props.set ( "EnsembleID", EnsembleID );
    }*/
    if ( IndependentTSList.length() > 0 ) {
        props.set ( "IndependentTSList", IndependentTSList );
    }
    if ( IndependentTSID.length() > 0 ) {
        props.set ( "IndependentTSID", IndependentTSID );
    }
    if ( IndependentEnsembleID.length() > 0 ) {
        props.set ( "IndependentEnsembleID", IndependentEnsembleID );
    }
    /*
    if ( SetStart.length() > 0 ) {
        props.set ( "SetStart", SetStart );
    }
    if ( SetEnd.length() > 0 ) {
        props.set ( "SetEnd", SetEnd );
    }
    */
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
    //String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String IndependentTSList = __IndependentTSList_JComboBox.getSelected();
    String IndependentTSID = __IndependentTSID_JComboBox.getSelected();
    String IndependentEnsembleID = __IndependentEnsembleID_JComboBox.getSelected();
    String IndependentSpecifiedTSID = getIndependentSpecifiedTSIDFromList();
    //String SetStart = __SetStart_JTextField.getText().trim();
    //String SetEnd = __SetEnd_JTextField.getText().trim();
    
    // IndependentTSID is used for several variations of IndependentTSList
    if ( TSListType.SPECIFIED_TSID.equals(IndependentTSList) ) {
        IndependentTSID = IndependentSpecifiedTSID;
    }
    
    __command.setCommandParameter ( "TSID", TSID );
    //__command.setCommandParameter ( "EnsembleID", EnsembleID );
    __command.setCommandParameter ( "IndependentTSList", IndependentTSList );
    __command.setCommandParameter ( "IndependentTSID", IndependentTSID );
    __command.setCommandParameter ( "IndependentEnsembleID", IndependentEnsembleID );
    //__command.setCommandParameter ( "SetStart", SetStart );
    //__command.setCommandParameter ( "SetEnd", SetEnd );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__TSID_JComboBox = null;
	__cancel_JButton = null;
	__command_JTextArea = null;
	__command = null;
	__ok_JButton = null;
	super.finalize ();
}

/**
Get the AddSpecifiedTSID parameter from the JList and put into a string.
@return a String containing the selected specified time series, separated by commas.
*/
private String getIndependentSpecifiedTSIDFromList()
{   StringBuffer buffer = new StringBuffer();
    if ( JGUIUtil.selectedSize(__IndependentSpecifiedTSID_JList) > 0 ) {
        // Get the selected and format...
        int selected[] = __IndependentSpecifiedTSID_JList.getSelectedIndices();
        int size = JGUIUtil.selectedSize(__IndependentSpecifiedTSID_JList);
        for ( int i = 0; i < size; i++ ) {
            if ( i > 0 ) {
                buffer.append ( ",");
            }
            buffer.append ( __IndependentSpecifiedTSID_JListModel.elementAt( selected[i]) );
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
{	__command = (SetToMin_Command)command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel,
		new JLabel ( "Set the time series data values to the minimum of itself and one or " +
				"more (independent) time series."),
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        		
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Time series to receive results:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TSID_JComboBox = new SimpleJComboBox ( true );    // Allow edit
    Vector tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    __TSID_JComboBox.setData ( tsids );
    __TSID_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __TSID_JComboBox,
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    __IndependentTSList_JComboBox = new SimpleJComboBox(false);
    y = CommandEditorUtil.addTSListToEditorDialogPanel (
            this, main_JPanel, new JLabel ("Independent TS List:"), __IndependentTSList_JComboBox, y );
    // Default is not to add SpecifiedTSID so add it here...
    __IndependentTSList_JComboBox.add(TSListType.SPECIFIED_TSID.toString());

    __IndependentTSID_JLabel = new JLabel (
            "Independent TSID (for Independent TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __IndependentTSID_JComboBox = new SimpleJComboBox ( true );  // Allow edits
    y = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, main_JPanel, __IndependentTSID_JLabel, __IndependentTSID_JComboBox, tsids, y );
    
    Vector EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    __IndependentEnsembleID_JLabel = new JLabel (
            "Independent EnsembleID (for Independent TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __IndependentEnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    y = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
            this, this, main_JPanel, __IndependentEnsembleID_JLabel, __IndependentEnsembleID_JComboBox, EnsembleIDs, y );

    __IndependentSpecifiedTSID_JLabel =
        new JLabel ("Independent specified TSID (for IndependentTSList=" + TSListType.SPECIFIED_TSID.toString() + "):");
    JGUIUtil.addComponent(main_JPanel, __IndependentSpecifiedTSID_JLabel,
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __IndependentSpecifiedTSID_JListModel = new DefaultListModel();
    // Get the list again because above list will have "*" which we don't want
    Vector tsids2 = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    int size = tsids2.size();
    for ( int i = 0; i < size; i++ ) {
        __IndependentSpecifiedTSID_JListModel.addElement( (String)tsids2.elementAt(i));
    }
    __IndependentSpecifiedTSID_JList = new JList ( __IndependentSpecifiedTSID_JListModel );
    __IndependentSpecifiedTSID_JList.setVisibleRowCount(Math.min(5,size));
    __IndependentSpecifiedTSID_JList.addListSelectionListener ( this );
    __IndependentSpecifiedTSID_JList.addKeyListener ( this );
    __IndependentSpecifiedTSID_JList.clearSelection();
    DefaultListSelectionModel sm = new DefaultListSelectionModel();
    sm.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
    __IndependentSpecifiedTSID_JList.setSelectionModel ( sm );
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__IndependentSpecifiedTSID_JList),
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST );
    
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
@return true if the edits were committed, false if the user cancelled.
*/
public boolean ok ()
{   return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{   String routine = "SetToMin_JDialog.refresh";
    String TSID = "";
    //String EnsembleID = "";
    String IndependentTSList = "";
    String IndependentTSID = "";
    String IndependentEnsembleID = "";
    //String SetStart = "";
    //String SetEnd = "";
    PropList props = __command.getCommandParameters();
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command...
        TSID = props.getValue ( "TSID" );
        //EnsembleID = props.getValue ( "EnsembleID" );
        IndependentTSList = props.getValue ( "IndependentTSList" );
        IndependentTSID = props.getValue ( "IndependentTSID" );
        IndependentEnsembleID = props.getValue ( "IndependentEnsembleID" );
        //SetStart = props.getValue ( "SetStart" );
        //SetEnd = props.getValue ( "SetEnd" );
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
        /*
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
        */
        if ( IndependentTSList == null ) {
            // Select default...
            __IndependentTSList_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __IndependentTSList_JComboBox,IndependentTSList, JGUIUtil.NONE, null, null ) ) {
                __IndependentTSList_JComboBox.select ( IndependentTSList );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nIndependentTSList value \"" + IndependentTSList +
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
            else {  // Select the blank...
                __IndependentTSID_JComboBox.select ( 0 );
            }
        }
        if ( IndependentEnsembleID == null ) {
            // Select default...
            __IndependentEnsembleID_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __IndependentEnsembleID_JComboBox,IndependentEnsembleID, JGUIUtil.NONE, null, null ) ) {
                __IndependentEnsembleID_JComboBox.select ( IndependentEnsembleID );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nIndependentEnsembleID value \"" + IndependentEnsembleID +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        setupIndependentSpecifiedTSID ( IndependentTSList, IndependentTSID );
        /*
        if ( SetStart != null ) {
            __SetStart_JTextField.setText ( SetStart );
        }
        if ( SetEnd != null ) {
            __SetEnd_JTextField.setText ( SetEnd );
        }
        */
    }
    // Regardless, reset the command from the fields...
    TSID = __TSID_JComboBox.getSelected();
    //EnsembleID = __EnsembleID_JComboBox.getSelected();
    IndependentTSList = __IndependentTSList_JComboBox.getSelected();
    IndependentTSID = __IndependentTSID_JComboBox.getSelected();
    IndependentEnsembleID = __IndependentEnsembleID_JComboBox.getSelected();
    String IndependentSpecifiedTSID = getIndependentSpecifiedTSIDFromList();
    // Use the list of specified TSID instead of the __IndependentTSID_JComboBox above
    if ( TSListType.SPECIFIED_TSID.equals(IndependentTSList) ) {
        IndependentTSID = IndependentSpecifiedTSID;
    }
    //SetStart = __SetStart_JTextField.getText().trim();
    //SetEnd = __SetEnd_JTextField.getText().trim();
    props = new PropList ( __command.getCommandName() );
    props.add ( "TSID=" + TSID );
    //props.add ( "EnsembleID=" + EnsembleID );
    props.add ( "IndependentTSList=" + IndependentTSList );
    props.add ( "IndependentTSID=" + IndependentTSID );
    props.add ( "IndependentEnsembleID=" + IndependentEnsembleID );
    //props.add ( "SetStart=" + SetStart );
    //props.add ( "SetEnd=" + SetEnd );
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
Setup the IndependentSpecifiedTSID list at initialization,
selecting items in the list that match the IndependentSpecifiedTSID parameter.
@param IndependentSpecifiedTSID The value of the parameter, of form "TSID,TSID,TSID,...".
*/
private void setupIndependentSpecifiedTSID ( String IndependentTSList, String IndependentSpecifiedTSID )
{   String routine = "Add_JDialog.setupAddSelectedTSID";
    // Check all the items in the list and highlight the ones that match the command being edited...
    if ( (IndependentTSList != null) &&
            TSListType.SPECIFIED_TSID.equals(IndependentTSList) && (IndependentSpecifiedTSID != null) ) {
        // Break list by commas since identifiers may have spaces and other "special" characters (but no commas)
        Vector v = StringUtil.breakStringList ( IndependentSpecifiedTSID, ",", StringUtil.DELIM_SKIP_BLANKS );
        int size = v.size();
        int pos = 0;
        Vector selected = new Vector();
        String independent = "";
        for ( int i = 0; i < size; i++ ) {
            independent = (String)v.elementAt(i);
            if ( (pos = JGUIUtil.indexOf( __IndependentSpecifiedTSID_JList, independent, false, true))>= 0 ) {
                // Select it because it is in the command and the list...
                selected.addElement ( "" + pos );
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
                iselected[is] = StringUtil.atoi ( (String)selected.elementAt(is));
            }
            __IndependentSpecifiedTSID_JList.setSelectedIndices( iselected );
        }
    }
}

/**
Handle ListSelectionListener events.
*/
public void valueChanged ( ListSelectionEvent e )
{   refresh ();
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