// Normalize_JDialog - editor for Normalize command

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2019 Colorado Department of Natural Resources

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
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;   

@SuppressWarnings("serial")
public class Normalize_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private Normalize_Command __command = null;
private JTextArea __command_JTextArea=null;
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
private SimpleJComboBox	__TSID_JComboBox = null;
private SimpleJComboBox	__MinValueMethod_JComboBox = null;
private JTextField __MinValue_JTextField = null;
private JTextField __MaxValue_JTextField = null;
private boolean __error_wait = false;	// Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false;       // Whether OK has been pressed.

/**
Command editor dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public Normalize_JDialog ( JFrame parent, Normalize_Command command )
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
		HelpViewer.getInstance().showHelp("command", "Normalize");
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
{
    refresh();
}

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void insertUpdate ( DocumentEvent e )
{
    refresh();
}

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void removeUpdate ( DocumentEvent e )
{
    refresh();
}

// ...End event handlers for DocumentListener

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{   // Put together a list of parameters to check...
    PropList props = new PropList ( "" );
    String Alias = __Alias_JTextField.getText().trim();
    String TSID = __TSID_JComboBox.getSelected();
    String MinValue = __MinValue_JTextField.getText().trim();
    String MaxValue = __MaxValue_JTextField.getText().trim();
    String MinValueMethod = __MinValueMethod_JComboBox.getSelected();
    __error_wait = false;

    if ( Alias.length() > 0 ) {
        props.set ( "Alias", Alias );
    }
    if ( (TSID != null) && (TSID.length() > 0) ) {
        props.set ( "TSID", TSID );
    }
    if ( (MinValue != null) && (MinValue.length() > 0) ) {
        props.set ( "MinValue", MinValue );
    }
    if ( (MaxValue != null) && (MaxValue.length() > 0) ) {
        props.set ( "MaxValue", MaxValue );
    }
    if ( (MinValueMethod != null) && (MinValueMethod.length() > 0) ) {
        props.set ( "MinValueMethod", MinValueMethod );
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
{   String Alias = __Alias_JTextField.getText().trim();
    String TSID = __TSID_JComboBox.getSelected();
    String MinValue = __MinValue_JTextField.getText().trim();
    String MaxValue = __MaxValue_JTextField.getText().trim();
    String MinValueMethod = __MinValueMethod_JComboBox.getSelected();
    __command.setCommandParameter ( "Alias", Alias );
    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "MinValue", MinValue );
    __command.setCommandParameter ( "MaxValue", MaxValue );
    __command.setCommandParameter ( "MinValueMethod", MinValueMethod );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, Normalize_Command command )
{	__command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Create a new time series by normalizing the data from a time series."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Use the alias to reference the new time series." +
		"  Data units are set to blank because the result is dimensionless."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
	JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++y, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel("Time Series to Normalize:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TSID_JComboBox = new SimpleJComboBox ( true );    // Allow edit
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    __TSID_JComboBox.setData ( tsids );
    __TSID_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __TSID_JComboBox,
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel("Alias to assign:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Alias_JTextField = new TSFormatSpecifiersJPanel(15);
    __Alias_JTextField.addKeyListener ( this );
    __Alias_JTextField.getDocument().addDocumentListener(this);
    JGUIUtil.addComponent(main_JPanel, __Alias_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Required - use %L for location, etc."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Minimum data value to process:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MinValueMethod_JComboBox = new SimpleJComboBox ( false );
    List<String> minChoices = new ArrayList<String>();
    minChoices.add ( __command._MinFromTS );
    minChoices.add ( __command._MinZero );
    __MinValueMethod_JComboBox.setData(minChoices);
    __MinValueMethod_JComboBox.addItemListener ( this );
        JGUIUtil.addComponent(main_JPanel, __MinValueMethod_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Required."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel,new JLabel ("Minimum output value:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__MinValue_JTextField = new JTextField ( "0.0", 10 );
	JGUIUtil.addComponent(main_JPanel, __MinValue_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	__MinValue_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Required - for example 0.0."), 
    3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel("Maximum output value:"), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__MaxValue_JTextField = new JTextField ( "1.0", 10 );
	JGUIUtil.addComponent(main_JPanel, __MaxValue_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	__MaxValue_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Required - for example 1.0."), 
            3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __command_JTextArea = new JTextArea (4,50);
    __command_JTextArea.setLineWrap ( true );
    __command_JTextArea.setWrapStyleWord ( true );
    __command_JTextArea.setEditable ( false );
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);

	// Refresh the contents...
	refresh();

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
{	refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	int code = event.getKeyCode();

	if ( code == KeyEvent.VK_ENTER ) {
		refresh();
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
{   String routine = "Normalize_JDialog.refresh";
    String Alias = "";
    String TSID = "";
    String MinValue = "";
    String MaxValue = "";
    String MinValueMethod = "";
    PropList props = __command.getCommandParameters();
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command...
        Alias = props.getValue ( "Alias" );
        TSID = props.getValue ( "TSID" );
        MinValue = props.getValue ( "MinValue" );
        MaxValue = props.getValue ( "MaxValue" );
        MinValueMethod = props.getValue ( "MinValueMethod" );
        if ( Alias != null ) {
            __Alias_JTextField.setText ( Alias );
        }
        // Now select the item in the list.  If not a match, print a warning.
        if ( JGUIUtil.isSimpleJComboBoxItem( __TSID_JComboBox, TSID, JGUIUtil.NONE, null, null ) ) {
            __TSID_JComboBox.select ( TSID );
        }
        else {
            // Automatically add to the list...
            if ( (TSID != null) && (TSID.length() > 0) ) {
                __TSID_JComboBox.insertItemAt ( TSID, 0 );
                // Select...
                __TSID_JComboBox.select ( TSID );
            }
            else {
                // Select the first choice...
                if ( __TSID_JComboBox.getItemCount() > 0 ) {
                    __TSID_JComboBox.select ( 0 );
                }
            }
        }
        if ( MinValue != null ) {
            __MinValue_JTextField.setText ( MinValue );
        }
        if ( MaxValue != null ) {
            __MaxValue_JTextField.setText ( MaxValue );
        }
        if ( MinValueMethod == null ) {
            // Select default...
            __MinValueMethod_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __MinValueMethod_JComboBox, MinValueMethod, JGUIUtil.NONE, null, null ) ) {
                __MinValueMethod_JComboBox.select ( MinValueMethod );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nMinValueMethod value \"" +
                MinValueMethod + "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
    }
    // Regardless, reset the command from the fields...
    Alias = __Alias_JTextField.getText().trim();
    TSID = __TSID_JComboBox.getSelected();
    MinValue = __MinValue_JTextField.getText().trim();
    MaxValue = __MaxValue_JTextField.getText().trim();
    MinValueMethod = __MinValueMethod_JComboBox.getSelected();
    props = new PropList ( __command.getCommandName() );
    props.add ( "Alias=" + Alias );
    props.add ( "TSID=" + TSID );
    props.add ( "MinValue=" + MinValue );
    props.add ( "MaxValue=" + MaxValue );
    props.add ( "MinValueMethod=" + MinValueMethod );
    __command_JTextArea.setText( __command.toString ( props ).trim() );
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
