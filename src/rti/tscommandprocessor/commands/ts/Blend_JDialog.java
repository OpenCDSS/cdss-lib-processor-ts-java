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

import java.util.List;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.IO.Command;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Editor dialog for the Blend() command.
*/
public class Blend_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{

private SimpleJButton	__cancel_JButton = null,// Cancel Button
			__ok_JButton = null;	// Ok Button
private Blend_Command __command = null;// Command as Vector of String
private JTextArea __command_JTextArea = null;// Command to edit
private SimpleJComboBox	__TSID_JComboBox = null;
private SimpleJComboBox __IndependentTSID_JComboBox = null;
private SimpleJComboBox	__BlendMethod_JComboBox = null; // Choice for blend methods.
private boolean __error_wait = false;
private boolean __first_time = true;
private boolean __ok = false;       // Was OK pressed last (false=cancel)?

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public Blend_JDialog ( JFrame parent, Command command )
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
		refresh();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
   else {
        // A combo box.  Refresh the command...
        refresh ();
    }
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{   // Put together a list of parameters to check...
    PropList props = new PropList ( "" );
    String TSID = __TSID_JComboBox.getSelected();
    String IndependentTSID = __IndependentTSID_JComboBox.getSelected ();
    String BlendMethod = __BlendMethod_JComboBox.getSelected();
    
    __error_wait = false;

    if ( TSID.length() > 0 ) {
        props.set ( "TSID", TSID );
    }
    if ( IndependentTSID.length() > 0 ) {
        props.set ( "IndependentTSID", IndependentTSID );
    }
    if ( BlendMethod.length() > 0 ) {
        props.set ( "BlendMethod", BlendMethod );
    }
    try {   // This will warn the user...
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
    String IndependentTSID = __IndependentTSID_JComboBox.getSelected();
    String BlendMethod = __BlendMethod_JComboBox.getSelected();
    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "IndependentTSID", IndependentTSID );
    __command.setCommandParameter ( "BlendMethod", BlendMethod );
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
	__IndependentTSID_JComboBox = null;
	__BlendMethod_JComboBox = null;
	__ok_JButton = null;
	super.finalize ();
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, Command command )
{   __command = (Blend_Command)command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Blend one time series into the start or end of another."), 
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The BlendAtEnd blend method will use data from the second (independent) time series at the end of the first."), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The overall period will be that of both time series."), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Additional blend methods (e.g., interpolating the blend) may be added in the future."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"See also the SetFromTS() and Add() commands."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Time series to modify:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__TSID_JComboBox = new SimpleJComboBox ( false );
	
	List tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    
	__TSID_JComboBox.setData ( tsids );
	__TSID_JComboBox.addItemListener ( this );
        JGUIUtil.addComponent(main_JPanel, __TSID_JComboBox,
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Independent time series:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __IndependentTSID_JComboBox = new SimpleJComboBox ( true );
    __IndependentTSID_JComboBox.setData ( tsids );
    __IndependentTSID_JComboBox.addKeyListener ( this );
    __IndependentTSID_JComboBox.addActionListener ( this );
        JGUIUtil.addComponent(main_JPanel, __IndependentTSID_JComboBox,
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel( "Blend method:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__BlendMethod_JComboBox = new SimpleJComboBox ( false );
	__BlendMethod_JComboBox.addItem ( __command._BlendAtEnd );
	__BlendMethod_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __BlendMethod_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST );
    __command_JTextArea = new JTextArea ( 5, 65 );
    __command_JTextArea.setLineWrap ( true );
    __command_JTextArea.setWrapStyleWord ( true );
    __command_JTextArea.setEditable ( false );
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Refresh the contents...
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

	setTitle ( "Edit " + __command.getCommandName() + "() command" );
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
{	refresh();
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
       // One of the combo boxes...
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
{	String routine = "Blend_JDialog.refresh";
    String TSID = "";
	String IndependentTSID = "";
	String BlendMethod = "";
    PropList props = null;      // Parameters as PropList.
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command...
        props = __command.getCommandParameters ();
        TSID = props.getValue ( "TSID" );
        IndependentTSID = props.getValue ( "IndependentTSID" );
        BlendMethod = props.getValue ( "BlendMethod" );
		// Now check the information and set in the GUI...
		if ( JGUIUtil.isSimpleJComboBoxItem( __TSID_JComboBox, TSID, JGUIUtil.NONE, null, null ) ) {
			__TSID_JComboBox.select ( TSID );
		}
		else {
		    Message.printWarning ( 1,
			routine, "Existing command references a non-existent\n"+
			"time series \"" + TSID + "\".  Select a\n" +
			"different time series or Cancel." );
		}
		
        if ( JGUIUtil.isSimpleJComboBoxItem( __IndependentTSID_JComboBox, IndependentTSID, JGUIUtil.NONE, null, null ) ) {
            __IndependentTSID_JComboBox.select ( IndependentTSID );
        }
        else {
            Message.printWarning ( 1,
            routine, "Existing command references a non-existent\n"+
            "independent time series \"" + IndependentTSID + "\".  Select a\n" +
            "different time series or Cancel." );
        }

		if ( JGUIUtil.isSimpleJComboBoxItem( __BlendMethod_JComboBox, BlendMethod, JGUIUtil.NONE, null, null ) ) {
			__BlendMethod_JComboBox.select ( BlendMethod );
		}
		else {	Message.printWarning ( 1,
			routine, "Existing command references an invalid "+
			"blend method choice \"" + BlendMethod + "\".\nSelect a different choice or Cancel." );
		}
	}
	// Regardless, reset the command from the fields...
	TSID = __TSID_JComboBox.getSelected();
	IndependentTSID = __IndependentTSID_JComboBox.getSelected();
	BlendMethod = __BlendMethod_JComboBox.getSelected();
	props = new PropList ("");
    props.add ( "TSID=" + TSID );
    props.add ( "IndependentTSID=" + IndependentTSID );
    props.add ( "BlendMethod=" + BlendMethod );
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

} // end Blend_JDialog
