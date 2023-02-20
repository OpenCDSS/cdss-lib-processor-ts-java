// ReadTimeSeries_JDialog - editor dialog for ReadTimeSeries command

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
import java.util.ArrayList;
import java.util.List;
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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

@SuppressWarnings("serial")
public class ReadTimeSeries_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private ReadTimeSeries_Command __command = null;
private JTextArea __command_JTextArea=null;
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
private JTextField __TSID_JTextField=null;
private SimpleJComboBox __ReadData_JComboBox = null;
private SimpleJComboBox __IfNotFound_JComboBox = null;
private JTextField __DefaultUnits_JTextField = null;
private boolean __first_time = true;
private boolean __error_wait = false;
private boolean __ok = false; // Whether OK has been pressed.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public ReadTimeSeries_JDialog (	JFrame parent, ReadTimeSeries_Command command )
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
		HelpViewer.getInstance().showHelp("command", "ReadTimeSeries");
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
    String TSID = __TSID_JTextField.getText().trim();
    String ReadData = __ReadData_JComboBox.getSelected();
    String IfNotFound = __IfNotFound_JComboBox.getSelected();
    String DefaultUnits = __DefaultUnits_JTextField.getText().trim();
    __error_wait = false;

    if ( Alias.length() > 0 ) {
        props.set ( "Alias", Alias );
    }
    if ( (TSID != null) && (TSID.length() > 0) ) {
        props.set ( "TSID", TSID );
    }
    if ( ReadData.length() > 0 ) {
        props.set ( "ReadData", ReadData );
    }
    if ( IfNotFound.length() > 0 ) {
        props.set ( "IfNotFound", IfNotFound );
    }
    if ( DefaultUnits.length() > 0 ) {
        props.set ( "DefaultUnits", DefaultUnits );
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
{   String Alias = __Alias_JTextField.getText().trim();
    String TSID = __TSID_JTextField.getText().trim();
    String ReadData = __ReadData_JComboBox.getSelected();
    String IfNotFound = __IfNotFound_JComboBox.getSelected();
    String DefaultUnits = __DefaultUnits_JTextField.getText().trim();
    __command.setCommandParameter ( "Alias", Alias );
    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "ReadData", ReadData );
    __command.setCommandParameter ( "IfNotFound", IfNotFound );
    __command.setCommandParameter ( "DefaultUnits", DefaultUnits );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, ReadTimeSeries_Command command )
{	__command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"This command is a general time series read command."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Its main purpose is to assign an alias to a time series, " +
		"which is more convenient to use than the long time series identifier."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Read commands for specific input types generally offer more options and " +
		"should be used if available."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The alias should be descriptive and should not contain spaces, periods, or parentheses."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Specify the period to read using the SetInputPeriod() command."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "See also the ReadTimeSeriesList() command."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel,new JLabel("Time series identifier:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__TSID_JTextField = new JTextField ( 40 );
	__TSID_JTextField.setToolTipText("Specify a time series identifier, can contain ${Property} notation");
	__TSID_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __TSID_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Required - needed to locate data to read."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    JGUIUtil.addComponent(main_JPanel, new JLabel("Alias to assign:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Alias_JTextField = new TSFormatSpecifiersJPanel(15);
    __Alias_JTextField.addKeyListener ( this );
    __Alias_JTextField.getDocument().addDocumentListener(this);
    JGUIUtil.addComponent(main_JPanel, __Alias_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Required - use %L for location, etc."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel,new JLabel("Read data?:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ReadData_JComboBox = new SimpleJComboBox ( false );
    __ReadData_JComboBox.setToolTipText("Read data (true) or only read properties (false).");
    List<String> readDataChoices = new ArrayList<String>();
    readDataChoices.add ( "" );
    readDataChoices.add ( __command._False );
    readDataChoices.add ( __command._True );
    __ReadData_JComboBox.setData(readDataChoices);
    __ReadData_JComboBox.select ( __command._Warn );
    __ReadData_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __ReadData_JComboBox,
        1, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - read data (default="+ __command._True + ")."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    JGUIUtil.addComponent(main_JPanel,new JLabel("If time series not found?:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __IfNotFound_JComboBox = new SimpleJComboBox ( false );
    List<String> notFoundChoices = new ArrayList<String>();
    notFoundChoices.add ( __command._Default );
    notFoundChoices.add ( __command._Ignore );
    notFoundChoices.add ( __command._Warn );
    __IfNotFound_JComboBox.setData(notFoundChoices);
    __IfNotFound_JComboBox.select ( __command._Warn );
    __IfNotFound_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __IfNotFound_JComboBox,
        1, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Required - how to handle time series that are not found."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Default units:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DefaultUnits_JTextField = new JTextField ( "", 20 );
    __DefaultUnits_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __DefaultUnits_JTextField,
    1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
    "Optional - units when IfNotFound=" + __command._Default + "."),
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
	refresh();	// Sets the __path_JButton status
	setResizable ( false );
    super.setVisible( true );
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged ( ItemEvent e )
{   refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	int code = event.getKeyCode();

	if ( code == KeyEvent.VK_ENTER ) {
		checkInput();
		if ( !__error_wait ) {
			response ( false );
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
{   String routine = "ReadTimeSeries_JDialog.refresh";
    String Alias = "";
    String TSID = "";
    String ReadData = "";
    String IfNotFound = "";
    String DefaultUnits = "";
    PropList props = __command.getCommandParameters();
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command...
        Alias = props.getValue ( "Alias" );
        TSID = props.getValue ( "TSID" );
        ReadData = props.getValue ( "ReadData" );
        IfNotFound = props.getValue ( "IfNotFound" );
        DefaultUnits = props.getValue ( "DefaultUnits" );
        if ( Alias != null ) {
            __Alias_JTextField.setText ( Alias );
        }
        if ( TSID != null ) {
            __TSID_JTextField.setText ( TSID );
        }
        if ( __ReadData_JComboBox != null ) {
            if ( ReadData == null ) {
                // Select default...
                __ReadData_JComboBox.select ( __command._Warn );
            }
            else {  if (    JGUIUtil.isSimpleJComboBoxItem(
                    __ReadData_JComboBox,
                    ReadData, JGUIUtil.NONE, null, null ) ) {
                    __ReadData_JComboBox.select ( ReadData );
                }
                else {  Message.printWarning ( 1, routine,
                    "Existing command references an invalid\n"+
                    "ReadData \"" + ReadData + "\".  Select a different value or Cancel." );
                }
            }
        }
        if ( __IfNotFound_JComboBox != null ) {
            if ( IfNotFound == null ) {
                // Select default...
                __IfNotFound_JComboBox.select ( __command._Warn );
            }
            else {  if (    JGUIUtil.isSimpleJComboBoxItem(
                    __IfNotFound_JComboBox,
                    IfNotFound, JGUIUtil.NONE, null, null ) ) {
                    __IfNotFound_JComboBox.select ( IfNotFound );
                }
                else {  Message.printWarning ( 1, routine,
                    "Existing command references an invalid\n"+
                    "IfNotFound \"" + IfNotFound + "\".  Select a different value or Cancel." );
                }
            }
        }
        if ( DefaultUnits != null ) {
            __DefaultUnits_JTextField.setText ( DefaultUnits );
        }
    }
    // Regardless, reset the command from the fields...
    Alias = __Alias_JTextField.getText().trim();
    TSID = __TSID_JTextField.getText().trim();
    ReadData = __ReadData_JComboBox.getSelected();
    IfNotFound = __IfNotFound_JComboBox.getSelected();
    DefaultUnits = __DefaultUnits_JTextField.getText().trim();
    props = new PropList ( __command.getCommandName() );
    props.add ( "Alias=" + Alias );
    props.add ( "TSID=" + TSID );
    props.add ( "ReadData=" + ReadData );
    props.add ( "IfNotFound=" + IfNotFound );
    props.add ( "DefaultUnits=" + DefaultUnits );
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

public void windowActivated( WindowEvent evt )
{
}

public void windowClosed( WindowEvent evt )
{
}

public void windowDeactivated( WindowEvent evt )
{
}

public void windowDeiconified( WindowEvent evt )
{
}

public void windowIconified( WindowEvent evt )
{
}

public void windowOpened( WindowEvent evt )
{
}

}
