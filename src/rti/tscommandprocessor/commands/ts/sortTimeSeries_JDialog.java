// ----------------------------------------------------------------------------
// sortTimeSeries_JDialog - editor for sortTimeSeries()
// ----------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
// ----------------------------------------------------------------------------
// History: 
//
// 2005-05-03	Steven A. Malers, RTi	Initial version (copy and modify
//					setOutputYearType_Dialog).
// 2005-05-19	SAM, RTi		Move from TSTool package to TS.
// 2007-05-08	SAM, RTi		Cleanup code based on Eclipse feedback.
// ----------------------------------------------------------------------------

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
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.IO.Command;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;

public class sortTimeSeries_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton	__cancel_JButton = null,	// Cancel Button
			__ok_JButton = null;		// Ok Button
private JTextArea	__command_JTextArea = null;	// Command as JTextField
// REVISIT SAM 2005-05-03 likely will enable in the future...
//private SimpleJComboBox	__SortField_JComboBox = null;
private boolean		__error_wait = false;
private boolean		__first_time = true;
private Command		__command = null;	// Command to edit
private boolean		__ok = false;		// Indicates whether the user
						// has pressed OK to close the
						// dialog.

/**
sortTimeSeries_JDialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public sortTimeSeries_JDialog ( JFrame parent, Command command )
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
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
	/* REVISIT SAM 2005-05-10 Need to enable sort properties..
	PropList props = new PropList ( "" );
	String Tolerance = __Tolerance_JTextField.getText().trim();
	__error_wait = false;
	if ( Tolerance.length() > 0 ) {
		props.set ( "Tolerance", Tolerance );
	}
	try {	// This will warn the user...
		__command.checkCommandParameters ( props, null, 1 );
	}
	catch ( Exception e ) {
		// The warning would have been printed in the check code.
		__error_wait = true;
	}
	*/
}

/**
Commit the edits to the command.  In this case the command should be reparsed
to check its low-level values.
*/
private void commitEdits ()
{	// Nothing to do because no parameters.
	/* REVISIT SAM 2005-05-10 Enable when parameters are added...
	String Tolerance = __Tolerance_JTextField.getText().trim();
	__command.setCommandParameter ( "Tolerance", Tolerance );
	*/
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__cancel_JButton = null;
	__command_JTextArea = null;
	__ok_JButton = null;
	super.finalize ();
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, Command command )
{	__command = command;

	addWindowListener( this );

        Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"This command sorts time series.  Currently the sort is " +
		"alphabetical by the full identifier." ),
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	/* REVISIT SAM 2005-05-03 Enable SortField or similar later
        JGUIUtil.addComponent(main_JPanel, new JLabel ( "Output Year Type:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__year_type_JComboBox = new SimpleJComboBox ( false );
	__year_type_JComboBox.add ( __YEAR_TYPE_CALENDAR );
	__year_type_JComboBox.add ( __YEAR_TYPE_WATER );
	__year_type_JComboBox.addItemListener ( this );
        JGUIUtil.addComponent(main_JPanel, __year_type_JComboBox,
		1, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	*/

        JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea ( 4, 60 );
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.addKeyListener ( this );
	__command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Refresh the contents...
	refresh ();

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	button_JPanel.add(__cancel_JButton = new SimpleJButton("Cancel", this));
	button_JPanel.add ( __ok_JButton = new SimpleJButton("OK", this) );

	setTitle ( "Edit " + __command.getCommandName() + "() command" );

	// Dialogs do not need to be resizable...
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
	}
}

public void keyReleased ( KeyEvent event )
{	refresh();
}

public void keyTyped ( KeyEvent event ) {;}

/**
Indicate if the user pressed OK (cancel otherwise).
*/
public boolean ok ()
{	return __ok;
}

/**
Refresh the command from the other text field contents.  The command is
of the form:
<pre>
sortTimeSeries()
</pre>
*/
private void refresh ()
{	String routine = "sortTimeSeries_JDialog.refresh";
	if ( __first_time ) {
		__first_time = false;
		Vector v = StringUtil.breakStringList (
			__command.toString(),"()",
			StringUtil.DELIM_SKIP_BLANKS );
		PropList props = null;
		if (	(v != null) && (v.size() > 1) &&
			(((String)v.elementAt(1)).indexOf("=") > 0) ) {
			props = PropList.parse (
				(String)v.elementAt(1), routine, "," );
		}
		if ( props == null ) {
			props = new PropList ( __command.getCommandName() );
		}
		//SortField = props.getValue ( "SortField" );
	}
	// Regardless, reset the command from the fields.  This is only  visible
	// information that has not been committed in the command.
	//Tolerance = __Tolerance_JTextField.getText().trim();
	PropList props = new PropList ( __command.getCommandName() );
	//props.add ( "Tolerance=" + Tolerance );
	__command_JTextArea.setText( __command.toString(props) );
}

/**
React to the user response.
@param ok if false, then the edit is cancelled.  If true, the edit is committed
and the dialog is closed.
*/
public void response ( boolean ok )
{	__ok = ok;
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

} // end sortTimeSeries_JDialog
