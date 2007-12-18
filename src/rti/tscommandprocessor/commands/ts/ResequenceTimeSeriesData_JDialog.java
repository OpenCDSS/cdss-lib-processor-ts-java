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

import javax.swing.JFileChooser;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import java.io.File;
import java.util.Vector;

import rti.tscommandprocessor.commands.riverware.writeRiverWare_Command;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

/**
Command editor dialog for the ResequenceTimeSeriesData() command.
*/
public class ResequenceTimeSeriesData_JDialog extends JDialog
implements ActionListener, KeyListener, ItemListener, WindowListener
{

private SimpleJButton	__cancel_JButton = null,// Cancel Button
			__ok_JButton = null;	// Ok Button
private ResequenceTimeSeriesData_Command __command = null;// Command to edit
private JTextArea	__command_JTextArea=null;// Command as TextField
private SimpleJComboBox	__TSList_JComboBox = null;
private SimpleJComboBox __TSID_JComboBox = null;
private SimpleJComboBox __TableID_JComboBox = null;
private SimpleJComboBox __TableRow_JComboBox = null;
private SimpleJComboBox __TableColumnStart_JComboBox = null;
private SimpleJComboBox __TableColumnEnd_JComboBox = null;
private JTextField  __OutputStart_JTextField = null;// Start of period for output
private JTextField  __OutputEnd_JTextField = null;// End of period for output
private JTextField __NewScenario_JTextField = null;
private boolean		__error_wait = false;	// Is there an error that we
						// are waiting to be cleared up
						// or Cancel?
private boolean		__first_time = true;
private boolean		__ok = false;		// Indicates whether the user
									// has pressed OK to close the
									// dialog.

/**
WriteDateValue_JDialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public ResequenceTimeSeriesData_JDialog (	JFrame parent, Command command )
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
    if ( TSList.equalsIgnoreCase(__command._AllMatchingTSID)) {
        __TSID_JComboBox.setEnabled(true);
    }
    else {
        __TSID_JComboBox.setEnabled(false);
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
    String TableID = __TableID_JComboBox.getSelected();
    String TableRow = __TableRow_JComboBox.getSelected();
    String TableColumnStart = __TableColumnStart_JComboBox.getSelected();
    String TableColumnEnd = __TableColumnEnd_JComboBox.getSelected();
    String OutputStart = __OutputStart_JTextField.getText().trim();
    String OutputEnd = __OutputEnd_JTextField.getText().trim();
    String NewScenario = __NewScenario_JTextField.getText().trim();

	__error_wait = false;
	
	if ( TSList.length() > 0 ) {
		parameters.set ( "TSList", TSList );
	}
	if ( TSID.length() > 0 ) {
		parameters.set ( "TSID", TSID );
	}
	if ( TableID.length() > 0 ) {
		parameters.set ( "TableID", TableID );
	}
    if ( TableRow.length() > 0 ) {
        parameters.set ( "TableRow", TableRow );
    }
    if ( TableColumnStart.length() > 0 ) {
        parameters.set ( "TableColumnStart", TableColumnStart );
    }
    if ( TableColumnEnd.length() > 0 ) {
        parameters.set ( "TableColumnEnd", TableColumnEnd );
    }
    if ( OutputStart.length() > 0 ) {
        parameters.set ( "OutputStart", OutputStart );
    }
    if ( OutputEnd.length() > 0 ) {
        parameters.set ( "OutputEnd", OutputEnd );
    }
    if ( NewScenario.length() > 0 ) {
        parameters.set ( "NewScenario", NewScenario );
    }
	try {	// This will warn the user...
		__command.checkCommandParameters ( parameters, null, 1 );
	}
	catch ( Exception e ) {
		// The warning would have been printed in the check code.
		Message.printWarning(2,"",e);
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
    String TableID = __TableID_JComboBox.getSelected();
    String TableRow = __TableRow_JComboBox.getSelected();
    String TableColumnStart = __TableColumnStart_JComboBox.getSelected();
    String TableColumnEnd = __TableColumnEnd_JComboBox.getSelected();
    String OutputStart = __OutputStart_JTextField.getText().trim();
    String OutputEnd = __OutputEnd_JTextField.getText().trim();
    String NewScenario = __NewScenario_JTextField.getText().trim();
	__command.setCommandParameter ( "TSList", TSList );
	__command.setCommandParameter ( "TSID", TSID );
	__command.setCommandParameter ( "TableID", TableID );
    __command.setCommandParameter ( "TableRow", TableRow );
    __command.setCommandParameter ( "TableColumnStart", TableColumnStart );
    __command.setCommandParameter ( "TableColumnEnd", TableColumnEnd );
    __command.setCommandParameter ( "OutputStart", OutputStart );
    __command.setCommandParameter ( "OutputEnd", OutputEnd );
    __command.setCommandParameter ( "NewScenario", NewScenario );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__cancel_JButton = null;
	__command_JTextArea = null;
	__TSList_JComboBox = null;
	__command = null;
	__ok_JButton = null;
	super.finalize ();
}

/**
Put together a list of row/column numbers (a list of numbers) for use in choices.
*/
private Vector getRowColumnList ()
{
    Vector v = new Vector();
    v.add( "");
    for ( int i = 1; i <= 200; i++ ) {
        v.add("" + i);
    }
    return v;
}

/**
Instantiates the GUI components.
@param parent Frame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, Command command )
{	__command = (ResequenceTimeSeriesData_Command)command;
	CommandProcessor processor = __command.getCommandProcessor();

	addWindowListener( this );

    Insets insetsTLBR = new Insets(1,2,1,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Resequence time series data by \"shuffling\" the original years of data, creating new time series." ),
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Currently, only resequencing of monthly time series using calendar years is supported." ),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"An identifier for the table with the new year sequence must be specified."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "A new scenario for the time series must be specified to differentiate the output from" +
        " the input time series."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("TS list:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	Vector tslist_Vector = new Vector();
	tslist_Vector.addElement ( "" );
	tslist_Vector.addElement ( __command._SelectedTS );
	tslist_Vector.addElement ( __command._AllTS );
    tslist_Vector.addElement ( __command._AllMatchingTSID );
	__TSList_JComboBox = new SimpleJComboBox(false);
	__TSList_JComboBox.setData ( tslist_Vector );
	__TSList_JComboBox.addItemListener (this);
	JGUIUtil.addComponent(main_JPanel, __TSList_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Indicates the time series to output (default=AllTS)."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("TSID (for " + __command._AllMatchingTSID + "):"),
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    // Allow edits...
    Vector tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    __TSID_JComboBox = new SimpleJComboBox ( true );
    int size = 0;
    if ( tsids == null ) {
        tsids = new Vector ();
    }
    size = tsids.size();
    // Blank for default
    if ( size > 0 ) {
        tsids.insertElementAt ( "", 0 );
    }
    else {  tsids.addElement ( "" );
    }
    // Always allow a "*" to let all time series be filled (put at end)...
    tsids.addElement ( "*" );
    __TSID_JComboBox.setData ( tsids );
    __TSID_JComboBox.addItemListener ( this );
    __TSID_JComboBox.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __TSID_JComboBox,
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Table ID for year sequence:"),
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    // Allow edits...
    Vector tableids = TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    __TableID_JComboBox = new SimpleJComboBox ( true );
    size = 0;
    if ( tableids == null ) {
        tableids = new Vector ();
    }
    size = tableids.size();
    // No blank (default) or wildcard is allowed.
    __TableID_JComboBox.setData ( tableids );
    __TableID_JComboBox.addItemListener ( this );
    __TableID_JComboBox.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __TableID_JComboBox,
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Row in table for year sequence:"),
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    // Allow edits...
    __TableRow_JComboBox = new SimpleJComboBox ( true );
    __TableRow_JComboBox.setData ( getRowColumnList() );
    __TableRow_JComboBox.select( 0 ); // Default
    __TableRow_JComboBox.addItemListener ( this );
    __TableRow_JComboBox.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __TableRow_JComboBox,
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ("First column in table for year sequence:"),
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    // Allow edits...
    __TableColumnStart_JComboBox = new SimpleJComboBox ( true );
    __TableColumnStart_JComboBox.setData ( getRowColumnList() );
    __TableColumnStart_JComboBox.select( 0 ); // Default
    __TableColumnStart_JComboBox.addItemListener ( this );
    __TableColumnStart_JComboBox.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __TableColumnStart_JComboBox,
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
            
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Last column in table for year sequence:"),
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    // Allow edits...
    __TableColumnEnd_JComboBox = new SimpleJComboBox ( true );
    __TableColumnEnd_JComboBox.setData ( getRowColumnList() );
    __TableColumnEnd_JComboBox.select( 0 ); // Default
    __TableColumnEnd_JComboBox.addItemListener ( this );
    __TableColumnEnd_JComboBox.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __TableColumnEnd_JComboBox,
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Output start:"), 
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputStart_JTextField = new JTextField (20);
    __OutputStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __OutputStart_JTextField,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Overrides the global output start."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Output end:"), 
        0, ++y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputEnd_JTextField = new JTextField (20);
    __OutputEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __OutputEnd_JTextField,
        1, y, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Overrides the global output end."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ("New scenario:"),
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NewScenario_JTextField = new JTextField (10);
    __NewScenario_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __NewScenario_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
        "For TSID of new time series."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

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
	refresh ();

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	__cancel_JButton = new SimpleJButton("Cancel", "Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__ok_JButton = new SimpleJButton("OK", "OK", this);
	button_JPanel.add ( __ok_JButton );

	setTitle ( "Edit " + __command.getCommandName() + "() Command" );
	setResizable ( true );
    pack();
    JGUIUtil.center( this );
	refresh();
    super.setVisible( true );
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged (ItemEvent e) {
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
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
}

public void keyReleased ( KeyEvent event )
{	refresh();
}

public void keyTyped ( KeyEvent event )
{
}

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
{	String routine = "ResequenceTimeSeriesData_JDialog.refresh";
	String TSList = "";
    String TSID = "";
    String TableID = "";
    String TableRow = "";
    String TableColumnStart = "";
    String TableColumnEnd = "";
    String OutputStart = "";
    String OutputEnd = "";
    String NewScenario = "";
	__error_wait = false;
	PropList parameters = null;
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		parameters = __command.getCommandParameters();
		TSList = parameters.getValue ( "TSList" );
        TSID = parameters.getValue ( "TSID" );
        TableID = parameters.getValue ( "TableID" );
        TableRow = parameters.getValue ( "TableRow" );
        TableColumnStart = parameters.getValue ( "TableColumnStart" );
        TableColumnEnd = parameters.getValue ( "TableColumnEnd" );
        OutputStart = parameters.getValue("OutputStart");
        OutputEnd = parameters.getValue("OutputEnd");
        NewScenario = parameters.getValue ( "NewScenario" );
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
				"Existing command references an invalid\nTSList value \"" +	TSList +
				"\".  Select a different value or Cancel.");
				__error_wait = true;
			}
		}
        if ( TSID == null ) {
            // Select default...
            __TSID_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __TSID_JComboBox,TSID, JGUIUtil.NONE, null, null ) ) {
                __TSID_JComboBox.select ( TSID );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command " +
                "references an invalid\nTSID value \"" +
                TSID +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( TableID == null ) {
            // Select default...
            if ( __TableID_JComboBox.getItemCount() > 0 ) {
                __TableID_JComboBox.select ( 0 );
            }
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __TableID_JComboBox,TableID, JGUIUtil.NONE, null, null ) ) {
                __TableID_JComboBox.select ( TableID );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nTableID value \"" +
                TableID +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( TableRow == null ) {
            // Select default...
            if ( __TableRow_JComboBox.getItemCount() > 0 ) {
                __TableRow_JComboBox.select ( 0 );
            }
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __TableRow_JComboBox,TableRow, JGUIUtil.NONE, null, null ) ) {
                __TableRow_JComboBox.select ( TableRow );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nTableRow value \"" + TableRow +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( TableColumnStart == null ) {
            // Select default...
            if ( __TableColumnStart_JComboBox.getItemCount() > 0 ) {
                __TableColumnStart_JComboBox.select ( 0 );
            }
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __TableColumnStart_JComboBox,TableColumnStart, JGUIUtil.NONE, null, null ) ) {
                __TableColumnStart_JComboBox.select ( TableColumnStart );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nTableColumnStart value \"" +
                TableColumnStart +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( TableColumnEnd == null ) {
            // Select default...
            if ( __TableColumnEnd_JComboBox.getItemCount() > 0 ) {
                __TableColumnEnd_JComboBox.select ( 0 );
            }
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __TableColumnEnd_JComboBox,TableColumnEnd, JGUIUtil.NONE, null, null ) ) {
                __TableColumnEnd_JComboBox.select ( TableColumnEnd );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nTableColumnEnd value \"" +
                TableColumnEnd +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( OutputStart != null ) {
            __OutputStart_JTextField.setText ( OutputStart );
        }
        if ( OutputEnd != null ) {
            __OutputEnd_JTextField.setText ( OutputEnd );
        }
        if ( NewScenario != null ) {
            __NewScenario_JTextField.setText(NewScenario);
        }
	}
	// Regardless, reset the command from the fields...
	TSList = __TSList_JComboBox.getSelected();
    TSID = __TSID_JComboBox.getSelected();
    TableID = __TableID_JComboBox.getSelected();
    TableRow = __TableRow_JComboBox.getSelected();
    TableColumnStart = __TableColumnStart_JComboBox.getSelected();
    TableColumnEnd = __TableColumnEnd_JComboBox.getSelected();
    OutputStart = __OutputStart_JTextField.getText().trim();
    OutputEnd = __OutputEnd_JTextField.getText().trim();
    NewScenario = __NewScenario_JTextField.getText().trim();
	parameters = new PropList ( __command.getCommandName() );
	parameters.add ( "TSList=" + TSList );
	parameters.add ( "TSID=" + TSID );
	parameters.add ( "TableID=" + TableID );
    parameters.add ( "TableRow=" + TableRow );
    parameters.add ( "TableColumnStart=" + TableColumnStart );
    parameters.add ( "TableColumnEnd=" + TableColumnEnd );
    parameters.add ( "OutputStart=" + OutputStart );
    parameters.add ( "OutputEnd=" + OutputEnd );
    parameters.add ( "NewScenario=" + NewScenario );
	__command_JTextArea.setText( __command.toString ( parameters ) );
}

/**
React to the user response.
@param ok if false, then the edit is cancelled.  If true, the edit is committed
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

