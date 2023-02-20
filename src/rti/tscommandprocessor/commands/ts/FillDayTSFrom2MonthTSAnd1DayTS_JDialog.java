// FillDayTSFrom2MonthTSAnd1DayTS_JDialog - Editor for FillDayTSFrom2MonthTSAnd1DayTS() command.

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
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.Command;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Editor for FillDayTSFrom2MonthTSAnd1DayTS() command.
*/
@SuppressWarnings("serial")
public class FillDayTSFrom2MonthTSAnd1DayTS_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private FillDayTSFrom2MonthTSAnd1DayTS_Command __command = null;// Command as Vector of String
private JTextArea __command_JTextArea=null;// Command as JTextField
private SimpleJComboBox	__TSID_D1_JComboBox = null;// Daily time series to fill
private SimpleJComboBox	__TSID_M1_JComboBox = null;// Field for related monthly time series.
private SimpleJComboBox	__TSID_D2_JComboBox = null;// Independent daily time series
private SimpleJComboBox	__TSID_M2_JComboBox = null;// Independent monthly time series
private JTextField __FillStart_JTextField;
private JTextField __FillEnd_JTextField;
//private JTextField __FillFlag_JTextField;
private boolean __error_wait = false;
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK button has been pressed.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public FillDayTSFrom2MonthTSAnd1DayTS_JDialog (	JFrame parent, Command command )
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
		HelpViewer.getInstance().showHelp("command", "FillDayTSFrom2MonthTSAnd1DayTS");
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
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{   // Put together a list of parameters to check...
    PropList props = new PropList ( "" );
    String TSID_D1 = __TSID_D1_JComboBox.getSelected();
    String TSID_M1 = __TSID_M1_JComboBox.getSelected();
    String TSID_M2 = __TSID_M2_JComboBox.getSelected();
    String TSID_D2 = __TSID_D2_JComboBox.getSelected();
    String FillStart = __FillStart_JTextField.getText().trim();
    String FillEnd = __FillEnd_JTextField.getText().trim();
    //String FillFlag = __FillFlag_JTextField.getText().trim();
    __error_wait = false;

    if ( TSID_D1.length() > 0 ) {
        props.set ( "TSID_D1", TSID_D1 );
    }
    if ( TSID_M1.length() > 0 ) {
        props.set ( "TSID_M1", TSID_M1 );
    }
    if ( TSID_M2.length() > 0 ) {
        props.set ( "TSID_M2", TSID_M2 );
    }
    if ( TSID_D2.length() > 0 ) {
        props.set ( "TSID_D2", TSID_D2 );
    }
    if ( FillStart.length() > 0 ) {
        props.set ( "FillStart", FillStart );
    }
    if ( FillEnd.length() > 0 ) {
        props.set ( "FillEnd", FillEnd );
    }
    /*
    if ( FillFlag.length() > 0 ) {
        props.set ( "FillFlag", FillFlag );
    }
    */
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
{   String TSID_D1 = __TSID_D1_JComboBox.getSelected();
    String TSID_M1 = __TSID_M1_JComboBox.getSelected();
    String TSID_M2 = __TSID_M2_JComboBox.getSelected();
    String TSID_D2 = __TSID_D2_JComboBox.getSelected();
    String FillStart = __FillStart_JTextField.getText().trim();
    String FillEnd = __FillEnd_JTextField.getText().trim();
    //String FillFlag = __FillFlag_JTextField.getText().trim();
    __command.setCommandParameter ( "TSID_D1", TSID_D1 );
    __command.setCommandParameter ( "TSID_M1", TSID_M1 );
    __command.setCommandParameter ( "TSID_M2", TSID_M2 );
    __command.setCommandParameter ( "TSID_D2", TSID_D2 );
    __command.setCommandParameter ( "FillStart", FillStart );
    __command.setCommandParameter ( "FillEnd", FillEnd );
    //__command.setCommandParameter ( "FillFlag", FillFlag );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, Command command )
{	__command = (FillDayTSFrom2MonthTSAnd1DayTS_Command)command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Fill a daily time series using the relationship:   D1[i] = D2[i]*M1[i]/M2[i]." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The monthly values M1/M2 give an average estimate of the volume " +
		"ratio and D2 provides an estimate of the daily pattern in the month." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "<HTML><B>Note that TSTool cannot verify whether time series are daily or " +
        "monthly until the command is run.</B></HTML>" ),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    
	JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++y, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Daily time series to fill (D1):" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__TSID_D1_JComboBox = new SimpleJComboBox ();

	__TSID_D1_JComboBox.setData ( tsids );
	__TSID_D1_JComboBox.addItemListener ( this );
        JGUIUtil.addComponent(main_JPanel, __TSID_D1_JComboBox,
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel (	"Associated monthly time series (M1):"), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__TSID_M1_JComboBox = new SimpleJComboBox ( false );
	__TSID_M1_JComboBox.setData ( tsids );
	__TSID_M1_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __TSID_M1_JComboBox,
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Monthly time series for total (M2):"), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__TSID_M2_JComboBox = new SimpleJComboBox ( false );
	__TSID_M2_JComboBox.setData ( tsids );
	__TSID_M2_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __TSID_M2_JComboBox,
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Daily time series for distribution (D2):" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__TSID_D2_JComboBox = new SimpleJComboBox ( false );
	__TSID_D2_JComboBox.setData ( tsids );
	__TSID_D2_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __TSID_D2_JComboBox,
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel,new JLabel( "Fill start date/time:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __FillStart_JTextField = new JTextField ( "", 10 );
    __FillStart_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __FillStart_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - default=fill all."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel,new JLabel("Fill end date/time:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __FillEnd_JTextField = new JTextField ( "", 10 );
    __FillEnd_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __FillEnd_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - default=fill all."),
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

    refresh ();
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
@return true if the edits were committed, false if the user cancelled.
*/
public boolean ok ()
{   return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{   String routine = "FillDayTSFrom2MonthTSAnd1DayTS_JDialog.refresh";
    String TSID_D1 = "";
    String TSID_M1 = "";
    String TSID_M2 = "";
    String TSID_D2 = "";
    String FillStart = "";
    String FillEnd = "";
    //String FillFlag = "";
    PropList props = __command.getCommandParameters();
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command...
        TSID_D1 = props.getValue ( "TSID_D1" );
        TSID_M1 = props.getValue ( "TSID_M1" );
        TSID_M2 = props.getValue ( "TSID_M2" );
        TSID_D2 = props.getValue ( "TSID_D2" );
        FillStart = props.getValue("FillStart");
        FillEnd = props.getValue("FillEnd");
        //FillFlag = props.getValue("FillFlag");
        if ( TSID_D1 == null ) {
            // Select default...
            __TSID_D1_JComboBox.select ( 0 );
        }
        else {  if (    JGUIUtil.isSimpleJComboBoxItem(
                __TSID_D1_JComboBox,
                TSID_D1, JGUIUtil.NONE, null, null ) ) {
                __TSID_D1_JComboBox.select ( TSID_D1 );
            }
            else {  Message.printWarning ( 1, routine,
                "Existing command " +
                "references an invalid\nTSID_D1 value \"" +
                TSID_D1 +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( TSID_M1 == null ) {
            // Select default...
            __TSID_M1_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __TSID_M1_JComboBox,TSID_M1, JGUIUtil.NONE, null, null ) ) {
                __TSID_M1_JComboBox.select ( TSID_M1 );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nTSID_M1 value \"" + TSID_M1 +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( TSID_M2 == null ) {
            // Select default...
            __TSID_M2_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __TSID_M2_JComboBox,TSID_M2, JGUIUtil.NONE, null, null ) ) {
                __TSID_M2_JComboBox.select ( TSID_M2 );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nTSID_M2 value \"" + TSID_M2 +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( TSID_D2 == null ) {
            // Select default...
            __TSID_D2_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __TSID_D2_JComboBox,TSID_D2, JGUIUtil.NONE, null, null ) ) {
                __TSID_D2_JComboBox.select ( TSID_D2 );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nTSID_D2 value \"" + TSID_D2 +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( FillStart != null ) {
            __FillStart_JTextField.setText( FillStart );
        }
        if ( FillEnd != null ) {
            __FillEnd_JTextField.setText ( FillEnd );
        }
        /*
        if ( FillFlag != null ) {
            __FillFlag_JTextField.setText ( FillFlag );
        }
        */
    }
    // Regardless, reset the command from the fields...
    TSID_D1 = __TSID_D1_JComboBox.getSelected();
    TSID_M1 = __TSID_M1_JComboBox.getSelected();
    TSID_M2 = __TSID_M2_JComboBox.getSelected();
    TSID_D2 = __TSID_D2_JComboBox.getSelected();
    FillStart = __FillStart_JTextField.getText().trim();
    FillEnd = __FillEnd_JTextField.getText().trim();
    //FillFlag = __FillFlag_JTextField.getText().trim();
    props = new PropList ( __command.getCommandName() );
    props.add ( "TSID_D1=" + TSID_D1 );
    props.add ( "TSID_M1=" + TSID_M1 );
    props.add ( "TSID_M2=" + TSID_M2 );
    props.add ( "TSID_D2" + TSID_D2 );
    props.add ( "FillStart=" + FillStart );
    props.add ( "FillEnd=" + FillEnd );
    //props.add ( "FillFlag=" + FillFlag );
    __command_JTextArea.setText( __command.toString ( props ).trim() );
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

}
