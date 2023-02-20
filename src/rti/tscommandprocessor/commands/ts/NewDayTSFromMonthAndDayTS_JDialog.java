// NewDayTSFromMonthAndDayTS_JDialog - editor for NewDayTSFromMonthAndDayTS

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
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.TS.TSIdent;
import RTi.TS.TSIdent_JDialog;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

@SuppressWarnings("serial")
public class NewDayTSFromMonthAndDayTS_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{

private JFrame __parent_JFrame = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private NewDayTSFromMonthAndDayTS_Command __command = null;
private JTextArea __command_JTextArea=null;// Command as JTextField
private TSFormatSpecifiersJPanel __Alias_JTextField =null;
private JTextArea __NewTSID_JTextArea = null; // New TSID.
private SimpleJButton __edit_JButton = null;    // Edit button
private SimpleJButton __clear_JButton = null;   // Clear NewTSID button
private SimpleJComboBox	__MonthTSID_JComboBox = null;
private SimpleJComboBox	__DayTSID_JComboBox = null;
private boolean __error_wait = false;
private boolean __first_time = true;
private boolean __ok = false;       // Whether OK has been pressed.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public NewDayTSFromMonthAndDayTS_JDialog ( JFrame parent, NewDayTSFromMonthAndDayTS_Command command )
{	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	String routine = "NewDayTSFromMonthAndDayTS_JDialog.actionPerformed";
    Object o = event.getSource();

	if ( o == __cancel_JButton ) {
		response ( false );
	}
    else if ( o == __clear_JButton ) {
        __NewTSID_JTextArea.setText ( "" );
        refresh();
    }
    else if ( o == __edit_JButton ) {
        // Edit the NewTSID in the dialog.  It is OK for the string to be blank.
        String NewTSID = __NewTSID_JTextArea.getText().trim();
        TSIdent tsident;
        try {
            if ( NewTSID.length() == 0 ) {
                tsident = new TSIdent();
            }
            else {
                tsident = new TSIdent ( NewTSID );
            }
            TSIdent tsident2=(new TSIdent_JDialog ( __parent_JFrame, true, tsident, null )).response();
            if ( tsident2 != null ) {
                __NewTSID_JTextArea.setText ( tsident2.toString(true) );
                refresh();
            }
        }
        catch ( Exception e ) {
            Message.printWarning ( 1, routine, "Error creating time series identifier from \"" +
            NewTSID + "\"." );
            Message.printWarning ( 3, routine, e );
        }
    }
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "NewDayTSFromMonthAndDayTS");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
}

//Start event handlers for DocumentListener...

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
    String NewTSID = __NewTSID_JTextArea.getText().trim();
    String MonthTSID = __MonthTSID_JComboBox.getSelected();
    String DayTSID = __DayTSID_JComboBox.getSelected();
    __error_wait = false;

    if ( Alias.length() > 0 ) {
        props.set ( "Alias", Alias );
    }
    if ( (NewTSID != null) && (NewTSID.length() > 0) ) {
        props.set ( "NewTSID", NewTSID );
    }
    if ( (MonthTSID != null) && (MonthTSID.length() > 0) ) {
        props.set ( "MonthTSID", MonthTSID );
    }
    if ( (DayTSID != null) && (DayTSID.length() > 0) ) {
        props.set ( "DayTSID", DayTSID );
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
    String NewTSID = __NewTSID_JTextArea.getText().trim();
    String MonthTSID = __MonthTSID_JComboBox.getSelected();
    String DayTSID = __DayTSID_JComboBox.getSelected();
    __command.setCommandParameter ( "Alias", Alias );
    __command.setCommandParameter ( "NewTSID", NewTSID );
    __command.setCommandParameter ( "MonthTSID", MonthTSID );
    __command.setCommandParameter ( "DayTSID", DayTSID );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, NewDayTSFromMonthAndDayTS_Command command )
{	__parent_JFrame = parent;
    __command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Create a daily time series by distributing a monthly total using a daily pattern." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Currently this command is only implemented to convert from acre-feet to CFS." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The period is taken from the global output period if set, or the daily time series." ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
	JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++y, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Monthly time series for total:"), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MonthTSID_JComboBox = new SimpleJComboBox ( true );    // Allow edit
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    __MonthTSID_JComboBox.setData ( tsids );
    __MonthTSID_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __MonthTSID_JComboBox,
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Daily time series for distribution:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DayTSID_JComboBox = new SimpleJComboBox ( true );    // Allow edit
    __DayTSID_JComboBox.setData ( tsids );
    __DayTSID_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __DayTSID_JComboBox,
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

   JGUIUtil.addComponent(main_JPanel, new JLabel ( "New time series ID:" ),
       0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
   __NewTSID_JTextArea = new JTextArea ( 3, 25 );
   __NewTSID_JTextArea.setEditable(false);
   __NewTSID_JTextArea.setLineWrap ( true );
   __NewTSID_JTextArea.setWrapStyleWord ( true );
   __NewTSID_JTextArea.addKeyListener ( this );
   // Make 3-high to fit in the edit button...
   JGUIUtil.addComponent(main_JPanel, new JScrollPane(__NewTSID_JTextArea),
       1, y, 2, 3, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
   JGUIUtil.addComponent(main_JPanel, new JLabel(
       "Specify to avoid confusion with TSID from original TS."), 
       3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   y += 2;
   JGUIUtil.addComponent(main_JPanel, (__edit_JButton = new SimpleJButton ( "Edit", "Edit", this ) ),
       3, y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
   JGUIUtil.addComponent(main_JPanel, (__clear_JButton =
       new SimpleJButton ( "Clear", "Clear", this ) ),
       4, y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
   
   JGUIUtil.addComponent(main_JPanel, new JLabel("Alias to assign:"),
       0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
   __Alias_JTextField = new TSFormatSpecifiersJPanel(15);
   __Alias_JTextField.addKeyListener ( this );
   __Alias_JTextField.getDocument().addDocumentListener ( this );
   JGUIUtil.addComponent(main_JPanel, __Alias_JTextField,
       1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
   JGUIUtil.addComponent(main_JPanel, new JLabel ("Required - use %L for location, etc."),
       3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

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
{	// Any change needs to refresh the command...
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
@return true if the edits were committed, false if the user canceled.
*/
public boolean ok ()
{   return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{   String Alias = "";
    String NewTSID = "";
    String MonthTSID = "";
    String DayTSID = "";

    PropList props = __command.getCommandParameters();
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command...
        Alias = props.getValue ( "Alias" );
        NewTSID = props.getValue ( "NewTSID" );
        MonthTSID = props.getValue ( "MonthTSID" );
        DayTSID = props.getValue ( "DayTSID" );
        if ( Alias != null ) {
            __Alias_JTextField.setText ( Alias );
        }
        if ( NewTSID != null ) {
            __NewTSID_JTextArea.setText ( NewTSID );
        }
        // Now select the item in the list.  If not a match, print a warning.
        if ( JGUIUtil.isSimpleJComboBoxItem( __MonthTSID_JComboBox, MonthTSID, JGUIUtil.NONE, null, null ) ) {
            __MonthTSID_JComboBox.select ( MonthTSID );
        }
        else {
            // Automatically add to the list...
            if ( (MonthTSID != null) && (MonthTSID.length() > 0) ) {
                __MonthTSID_JComboBox.insertItemAt ( MonthTSID, 0 );
                // Select...
                __MonthTSID_JComboBox.select ( MonthTSID );
            }
            else {
                // Select the first choice...
                if ( __MonthTSID_JComboBox.getItemCount() > 0 ) {
                    __MonthTSID_JComboBox.select ( 0 );
                }
            }
        }
        if ( JGUIUtil.isSimpleJComboBoxItem( __DayTSID_JComboBox, DayTSID, JGUIUtil.NONE, null, null ) ) {
            __DayTSID_JComboBox.select ( DayTSID );
        }
        else {
            // Automatically add to the list...
            if ( (DayTSID != null) && (DayTSID.length() > 0) ) {
                __DayTSID_JComboBox.insertItemAt ( DayTSID, 0 );
                // Select...
                __DayTSID_JComboBox.select ( DayTSID );
            }
            else {
                // Select the first choice...
                if ( __DayTSID_JComboBox.getItemCount() > 0 ) {
                    __DayTSID_JComboBox.select ( 0 );
                }
            }
        }
    }
    // Regardless, reset the command from the fields...
    Alias = __Alias_JTextField.getText().trim();
    NewTSID = __NewTSID_JTextArea.getText().trim();
    MonthTSID = __MonthTSID_JComboBox.getSelected();
    DayTSID = __DayTSID_JComboBox.getSelected();
    props = new PropList ( __command.getCommandName() );
    props.add ( "Alias=" + Alias );
    props.add ( "NewTSID=" + NewTSID );
    props.add ( "MonthTSID=" + MonthTSID );
    props.add ( "DayTSID=" + DayTSID );
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
