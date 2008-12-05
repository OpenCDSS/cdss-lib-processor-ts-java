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

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;

import java.util.List;
import java.util.Vector;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.Command;
import RTi.Util.IO.DataUnits;
import RTi.Util.IO.DataDimension;
import RTi.Util.IO.PropList;
//import RTi.Util.IO.DataDimensionData;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;

/**
Editor for ConvertDataUnits() command.
*/
public class ConvertDataUnits_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton	__cancel_JButton = null,// Cancel Button
			__ok_JButton = null;	// Ok Button
private ConvertDataUnits_Command __command = null;// Command to edit
private JTextArea __command_JTextArea=null;
private SimpleJComboBox __TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox __TSID_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private SimpleJComboBox	__Dimension_JComboBox=null; // Field for data dimensions
private SimpleJComboBox	__NewUnits_JComboBox=null;
private boolean	__error_wait = false;	// Is there an error to be cleared up
private boolean	__first_time = true;
private boolean __ok = false;       // Indicates whether OK button has been pressed.

/**
Command dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public ConvertDataUnits_JDialog ( JFrame parent, Command command )
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
}

/**
Check the user input for errors and set __error_wait accordingly.
*/
private void checkInput ()
{	PropList parameters = new PropList ( "" );
    String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String NewUnits = StringUtil.getToken(__NewUnits_JComboBox.getSelected()," -",StringUtil.DELIM_SKIP_BLANKS,0);
    if ( TSList.length() > 0 ) {
        parameters.set ( "TSList", TSList );
    }
    if ( TSID.length() > 0 ) {
        parameters.set ( "TSID", TSID );
    }
    if ( EnsembleID.length() > 0 ) {
        parameters.set ( "EnsembleID", EnsembleID );
    }
    if ( NewUnits.length() > 0 ) {
        parameters.set ( "NewUnits", NewUnits );
    }

    try {   // This will warn the user...
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
    String NewUnits = StringUtil.getToken(__NewUnits_JComboBox.getSelected()," -",StringUtil.DELIM_SKIP_BLANKS,0);;
    __command.setCommandParameter ( "TSList", TSList );
    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
    __command.setCommandParameter ( "NewUnits", NewUnits );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__TSID_JComboBox = null;
	__Dimension_JComboBox = null;
	__NewUnits_JComboBox = null;
	__cancel_JButton = null;
	__command_JTextArea = null;
	__command = null;
	__ok_JButton = null;
	super.finalize ();
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param title Dialog title.
@param command The command to edit.
*/
private void initialize ( JFrame parent, Command command )
{   __command = (ConvertDataUnits_Command)command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(0,2,0,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The units of the selected time series will be converted to the new data units." ), 
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The old and new data units must have the same dimension (e.g., both are length)." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"However, the dimension is not checked until time series are actually processed." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "If desired units are not recognized, try using the Scale() command." ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    __TSList_JComboBox = new SimpleJComboBox(false);
    y = CommandEditorUtil.addTSListToEditorDialogPanel ( this, main_JPanel, __TSList_JComboBox, y );

    __TSID_JLabel = new JLabel ("TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __TSID_JComboBox = new SimpleJComboBox ( true );  // Allow edits
    List tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, main_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, y );
    
    __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    List EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
            this, this, main_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, y );

    JGUIUtil.addComponent(main_JPanel, new JLabel (	"Dimension:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Dimension_JComboBox = new SimpleJComboBox ( false );
	List dimension_data_Vector0 = DataDimension.getDimensionData();
	List dimension_data_Vector = null;
	int size = 0;
	if ( dimension_data_Vector0 != null ) {
		size = dimension_data_Vector0.size();
		Message.printStatus ( 2, "", "Number of dimension: " + size );
		dimension_data_Vector = new Vector(size);
		DataDimension dim;
		for ( int i = 0; i < size; i++ ) {
			dim =(DataDimension)dimension_data_Vector0.get(i);
			if ( dim.getAbbreviation().length() > 0 ) {
				dimension_data_Vector.add ( dim.getAbbreviation() + " - " + dim.getLongName() );
			}
		}
		dimension_data_Vector =	StringUtil.sortStringList(dimension_data_Vector);
	}
	else {
		Message.printStatus ( 2, "", "Number of dimension (null): " + 0);
	}
	size = 0;
	if ( dimension_data_Vector != null ) {
		size = dimension_data_Vector.size();
	}
	for ( int i = 0; i < size; i++ ) {
		if ( ((String)dimension_data_Vector.get(i)).length() > 0){
			__Dimension_JComboBox.add (	(String)dimension_data_Vector.get(i) );
		}
	}
	if ( size > 0 ) {
		__Dimension_JComboBox.select ( 0 );
	}
	__Dimension_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Dimension_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Select the dimension first, to list corresponding units."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "New data units:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__NewUnits_JComboBox = new SimpleJComboBox ( false );
	__NewUnits_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __NewUnits_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

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
{	Object o = e.getItemSelectable();
	if ( o == __Dimension_JComboBox ) {
		// Refresh the units...
		refreshUnits ();
	}
	else {
        checkGUIState();
    }
    refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	int code = event.getKeyCode();

	if ( code == KeyEvent.VK_ENTER ) {
		refresh ();
		checkInput ();
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
{	String routine = "ConvertDataUnits_JDialog.refresh";
    String TSList = "";
    String TSID = "";
    String EnsembleID = "";
	String NewUnits = "";
    PropList props = __command.getCommandParameters();
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command...
        TSList = props.getValue ( "TSList" );
        TSID = props.getValue ( "TSID" );
        EnsembleID = props.getValue ( "EnsembleID" );
        NewUnits = props.getValue ( "NewUnits" );
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
		// Figure out the dimension of the units and set the dimension choice as well as the units choice...
		DataUnits dataunits = null;
        if ( NewUnits == null ) {
            // Select first dimension and then refresh...
            __Dimension_JComboBox.select ( 0 );
            refreshUnits();
        }
        else {
            // Have units to try to display
    		try {
                dataunits = DataUnits.lookupUnits ( NewUnits);
    		}
    		catch ( Exception e ) {
    			Message.printWarning ( 1, routine,
    			"Existing command references unrecognized\n"+ "data units \"" + NewUnits +
    			"\".  Select recognized data units or Cancel.");
    			Message.printWarning ( 3,"",e);
    			__error_wait = true;
    		}
    		if ( !__error_wait ) {
    			try {
                    // First select the dimension...
    				JGUIUtil.selectTokenMatches ( __Dimension_JComboBox,
    				true, " ", 0, 0, dataunits.getDimension().getAbbreviation(), null );
    			}
    			catch ( Exception e ) {
    				Message.printWarning ( 1, routine,
    				"Existing command references units with unrecognized\n"+ "data dimension \"" +
    				dataunits.getDimension().getAbbreviation() +
    				"\".  Select recognized data units or Cancel.");
    				Message.printWarning ( 3, "", e );
    				__error_wait = true;
    			}
    		}
    		if ( !__error_wait ) {
    			try {
                    // Now select the units...
    				refreshUnits();
    				JGUIUtil.selectTokenMatches (__NewUnits_JComboBox,true, " ", 0, 0, NewUnits, null );
    			}
    			catch ( Exception e ) {
    				Message.printWarning ( 1, routine,
    				"Existing command references unrecognized\n"+ "data units \"" + NewUnits +
    				"\".  Select recognized data units or Cancel.");
    				Message.printWarning ( 2,"",e);
    				__error_wait = true;
    			}
    		}
    		else {
                refreshUnits ();
    		}
        }
	}
	// Regardless, reset the command from the fields...
    TSList = __TSList_JComboBox.getSelected();
    TSID = __TSID_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
	NewUnits = StringUtil.getToken(__NewUnits_JComboBox.getSelected()," -",StringUtil.DELIM_SKIP_BLANKS,0);
    props = new PropList ( __command.getCommandName() );
    props.add ( "TSList=" + TSList );
    props.add ( "TSID=" + TSID );
    props.add ( "EnsembleID=" + EnsembleID );
    props.add ( "NewUnits=" + NewUnits );
    __command_JTextArea.setText( __command.toString ( props ) );
}

/**
Refresh the data units based on the dimension.
*/
private void refreshUnits ()
{	String dimension = __Dimension_JComboBox.getSelected();
	List units_Vector = DataUnits.lookupUnitsForDimension ( null, StringUtil.getToken(dimension," ",0,0) );
	int size = 0;
	if ( units_Vector != null ) {
		size = units_Vector.size();
	}
	__NewUnits_JComboBox.removeAll ();
	DataUnits units = null;
	List units_sorted_Vector = new Vector();
	for ( int i = 0; i < size; i++ ) {
		units = (DataUnits)units_Vector.get(i);
		units_sorted_Vector.add ( units.getAbbreviation() + " - " + units.getLongName() );
	}
	units_sorted_Vector = StringUtil.sortStringList ( units_sorted_Vector );
	__NewUnits_JComboBox.setData ( units_sorted_Vector );
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

} // end convertDataUnits_JDialog
