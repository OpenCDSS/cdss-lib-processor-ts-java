// Message_JDialog - Editor dialog for the Message() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2024 Colorado Department of Natural Resources

CDSS Time Series Processor Java Library is free software:  you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    CDSS Time Series Processor Java Library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with CDSS Time Series Processor Java Library.  If not, see <https://www.gnu.org/licenses/>.

NoticeEnd */

package rti.tscommandprocessor.commands.logging;

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

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Editor dialog for the Message() command.
*/
@SuppressWarnings("serial")
public class Message_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private Message_Command __command = null;
private JTextArea __command_JTextArea = null;
private JTextArea __Message_JTextArea = null;
private JTextField __PromptActions_JTextField = null;
private SimpleJComboBox __CommandStatus_JComboBox;
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether user pressed OK to close the dialog.

/**
Command dialog editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public Message_JDialog ( JFrame parent, Message_Command command ) {
 	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event ) {
	Object o = event.getSource();

	if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "Message");
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
Check the input.  If errors exist, warn the user and set the __error_wait flag to true.
This should be called before response() is allowed to complete.
*/
private void checkInput () {
    // Put together a list of parameters to check.
    PropList props = new PropList ( "" );
    String Message = __Message_JTextArea.getText().trim();
    String PromptActions = __PromptActions_JTextField.getText().trim();
    String CommandStatus = __CommandStatus_JComboBox.getSelected();
    if ( Message.length() > 0 ) {
        props.set ( "Message", Message );
    }
    if ( PromptActions.length() > 0 ) {
        props.set ( "PromptActions", PromptActions );
    }
    if ( CommandStatus.length() > 0 ) {
        props.set ( "CommandStatus", CommandStatus );
    }
    try {
        // This will warn the user.
        __command.checkCommandParameters ( props, null, 1 );
    }
    catch ( Exception e ) {
        // The warning would have been printed in the check code.
        __error_wait = true;
    }
}

/**
Commit the edits to the command.
*/
private void commitEdits () {
   // Allow newlines in the dialog to be saved as escaped newlines.
   //String Message = __Message_JTextArea.getText().replace('\n', ' ').replace('\t', ' ').trim();
   String Message2 = __Message_JTextArea.getText().replace("\n", "\\n").replace('\t', ' ').trim();
    String PromptActions = __PromptActions_JTextField.getText().trim();
    // Newlines are allowed but are replaced with literal backslash in the command.
    String CommandStatus = __CommandStatus_JComboBox.getSelected();
    __command.setCommandParameter ( "Message", Message2 );
    __command.setCommandParameter ( "PromptActions", PromptActions );
    __command.setCommandParameter ( "CommandStatus", CommandStatus );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param title Dialog title.
@param command Command to edit.
*/
private void initialize ( JFrame parent, Message_Command command ) {
    __command = command;

	addWindowListener( this );

    Insets insetsNONE = new Insets(1,1,1,1);
    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel.

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Output a message to the log file and optionally set the command status to indicate a problem."),
        0, ++y, 7, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Messages are useful for troubleshooting run-time properties."),
        0, ++y, 7, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Messages can contain ${Property} to output processor property values."),
        0, ++y, 7, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Use 'Enter' to insert a new line, which will be shown as \\n in the message parameter."),
        0, ++y, 7, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Setting the command status to " + CommandStatusType.WARNING + " or " + CommandStatusType.FAILURE +
		" will show a command status indicator for a problem."),
		0, ++y, 7, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Setting the command status to " + CommandStatusType.NOTIFICATION +
		" will treat the status as success and show a notification indicator."),
		0, ++y, 7, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++y, 7, 1, 0, 0, insetsNONE, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Message text area is resizable.
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Message:" ),
        0, ++y, 1, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Message_JTextArea = new JTextArea (8,60);
    __Message_JTextArea.setLineWrap ( true );
    __Message_JTextArea.setWrapStyleWord ( true );
    __Message_JTextArea.addKeyListener(this);
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__Message_JTextArea),
        1, y, 1, 1, 1.0, 1.0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Prompt actions:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__PromptActions_JTextField = new JTextField ( 40 );
	__PromptActions_JTextField.setToolTipText("Specify actions separated by commas, which will display as buttons.  Possible values: Cancel, Continue");
	__PromptActions_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __PromptActions_JTextField,
		1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional actions if message is a prompt."),
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel("Command status:"),
		0, ++y, 1, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CommandStatus_JComboBox = new SimpleJComboBox ( false );
    __CommandStatus_JComboBox.add ( "" );
    __CommandStatus_JComboBox.add ( "" + CommandStatusType.NOTIFICATION );
    __CommandStatus_JComboBox.add ( "" + CommandStatusType.SUCCESS );
    __CommandStatus_JComboBox.add ( "" + CommandStatusType.WARNING );
    __CommandStatus_JComboBox.add ( "" + CommandStatusType.FAILURE );
    __CommandStatus_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __CommandStatus_JComboBox,
        1, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Optional - default is " + CommandStatusType.SUCCESS + "."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:"),
		0, ++y, 1, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __command_JTextArea = new JTextArea ( 4, 80 );
    __command_JTextArea.setLineWrap ( true );
    __command_JTextArea.setWrapStyleWord ( true );
    __command_JTextArea.setEditable ( false );
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 6, 1, 1, 0, insetsNONE, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	// Refresh the contents.
	refresh ();

	// Panel for buttons.
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
    JGUIUtil.addComponent(main_JPanel, button_JPanel,
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	__ok_JButton = new SimpleJButton("OK", this);
	button_JPanel.add ( __ok_JButton );
	__ok_JButton.setToolTipText("Save changes to command");
	__cancel_JButton = new SimpleJButton("Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__cancel_JButton.setToolTipText("Cancel without saving changes to command");
	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");

	setTitle ( "Edit " + __command.getCommandName() + " command" );
    pack();
    JGUIUtil.center( this );
	setResizable ( false );
    super.setVisible( true );
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged ( ItemEvent e ) {
    refresh();
}

/**
Respond to KeyEvents.
@param event KeyEvent to handle
*/
public void keyPressed ( KeyEvent event ) {
	int code = event.getKeyCode();

   	if ( code == KeyEvent.VK_ENTER ) {
   		if ( event.getSource() == this.__Message_JTextArea ) {
    		// Do not allow "Enter" in message because newlines in the message are allowed.
    		return;
    	}
   		else {
    		refresh ();
	  		checkInput();
	  		if ( !__error_wait ) {
		  		response ( false );
	  		}
		}
    }
}

/**
 * Handle key released event.
 * @param event KeyEvent to handle
 */
public void keyReleased ( KeyEvent event ) {
	refresh();
}

/**
 * Handle key typed event.
 * @param event KeyEvent to handle
 */
public void keyTyped ( KeyEvent event ) {
}

/**
Indicate if the user pressed OK (cancel otherwise).
*/
public boolean ok () {
    return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh () {
	String routine = getClass().getSimpleName() + ".refresh";
    String Message0 = "";
    String PromptActions = "";
	String CommandStatus = "";
	__error_wait = false;
	PropList props = __command.getCommandParameters();
	if ( __first_time ) {
		__first_time = false;
		Message0 = props.getValue( "Message" );
		if ( Message0 != null ) {
			// Replace escaped newline with actual newline so it will display on multiple lines.
			Message.printStatus(2,routine,"First time - replacing escaped newline with actual newline.");
			Message0 = Message0.replace("\\n","\n");
		}
		PromptActions = props.getValue( "PromptActions" );
		CommandStatus = props.getValue( "CommandStatus" );
		if ( Message0 != null ) {
		    __Message_JTextArea.setText( Message0 );
		}
		if ( PromptActions != null ) {
			__PromptActions_JTextField.setText ( PromptActions );
		}
        if ( (CommandStatus == null) || (CommandStatus.length() == 0) ) {
            // Select default.
            __CommandStatus_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __CommandStatus_JComboBox, CommandStatus, JGUIUtil.NONE, null, null ) ) {
                __CommandStatus_JComboBox.select ( CommandStatus );
            }
            else {
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                "CommandStatus \"" + CommandStatus + "\" parameter.  Select a\ndifferent value or Cancel." );
            }
        }
	}
	// Regardless, reset the command from the fields.
	Message0 = __Message_JTextArea.getText().trim();
    if ( Message0 != null ) {
    	// Replace internal newline with escaped string for command text.
		Message.printStatus(2,routine,"Replacing actual newline with escaped newline in Message parameter value.");
    	Message0 = Message0.replace("\n", "\\n");
    }
	PromptActions = __PromptActions_JTextField.getText().trim();
	CommandStatus = __CommandStatus_JComboBox.getSelected();
    props = new PropList ( __command.getCommandName() );
    props.add ( "Message=" + Message0 );
    props.add ( "PromptActions=" + PromptActions );
    props.add ( "CommandStatus=" + CommandStatus );
    __command_JTextArea.setText( __command.toString(props).trim() );
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed and the dialog is closed.
*/
public void response ( boolean ok ) {
    __ok = ok;
    if ( ok ) {
        // Commit the changes.
        commitEdits ();
        if ( __error_wait ) {
            // Not ready to close out.
            return;
        }
    }
    // Now close out.
    setVisible( false );
    dispose();
}

/**
Responds to WindowEvents.
@param event WindowEvent object
*/
public void windowClosing( WindowEvent event ) {
	response ( false );
}

public void windowActivated( WindowEvent evt ) {
}

public void windowClosed( WindowEvent evt ) {
}

public void windowDeactivated( WindowEvent evt ) {
}

public void windowDeiconified( WindowEvent evt ) {
}

public void windowIconified( WindowEvent evt ) {
}

public void windowOpened( WindowEvent evt ) {
}

}