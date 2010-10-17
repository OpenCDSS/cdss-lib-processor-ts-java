package rti.tscommandprocessor.commands.ipp;

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
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.Util.GUI.InputFilter_JPanel;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Editor for he ReadColoradoIPP() command.
*/
public class ReadColoradoIPP_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private ReadColoradoIPP_Command __command = null;
//private JTextField __InputName_JTextField;
private JTextField __InputStart_JTextField;
private JTextField __InputEnd_JTextField;
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
			
private JTextArea __command_JTextArea = null; // Command as JTextArea
private InputFilter_JPanel __inputFilter_JPanel =null;
private IppDMI __ippdmi = null; // Colorado IPP DMI to do queries.
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK was pressed when closing the dialog.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public ReadColoradoIPP_JDialog ( JFrame parent, ReadColoradoIPP_Command command )
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
		checkInput ();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else {
		refresh();
	}
}

/**
Check the state of the dialog, disabling/enabling components as appropriate.
*/
private void checkGUIState()
{
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
	PropList props = new PropList ( "" );
	__error_wait = false;
	/*
	String InputName = __InputName_JTextField.getText().trim();
	if ( InputName.length() > 0 ) {
		props.set ( "InputName", InputName );
	}
	*/
	int numWhere = __inputFilter_JPanel.getNumFilterGroups();
	for ( int i = 1; i <= numWhere; i++ ) {
	    String where = getWhere ( i - 1 );
	    if ( where.length() > 0 ) {
	        props.set ( "Where" + i, where );
	    }
    }
	String InputStart = __InputStart_JTextField.getText().trim();
	if ( InputStart.length() > 0 ) {
		props.set ( "InputStart", InputStart );
	}
	String InputEnd = __InputEnd_JTextField.getText().trim();
	if ( InputEnd.length() > 0 ) {
		props.set ( "InputEnd", InputEnd );
	}
    String Alias = __Alias_JTextField.getText().trim();
    if ( Alias.length() > 0 ) {
        props.set ( "Alias", Alias );
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
{	//String InputName = __InputName_JTextField.getText().trim();
	//__command.setCommandParameter ( "InputName", InputName );
	String delim = ";";
	int numWhere = __inputFilter_JPanel.getNumFilterGroups();
	for ( int i = 1; i <= numWhere; i++ ) {
	    String where = getWhere ( i - 1 );
	    if ( where.startsWith(delim) ) {
	        where = "";
	    }
	    __command.setCommandParameter ( "Where" + i, where );
	}
	// Both versions of the commands use these...
	String InputStart = __InputStart_JTextField.getText().trim();
	__command.setCommandParameter ( "InputStart", InputStart );
	String InputEnd = __InputEnd_JTextField.getText().trim();
	__command.setCommandParameter ( "InputEnd", InputEnd );
	String Alias = __Alias_JTextField.getText().trim();
    __command.setCommandParameter ( "Alias", Alias );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__Alias_JTextField = null;
	//__InputName_JTextField = null;
	__InputStart_JTextField = null;
	__InputEnd_JTextField = null;
	__cancel_JButton = null;
	__command_JTextArea = null;
	__command = null;
	__ok_JButton = null;
	super.finalize ();
}

/**
Return the "WhereN" parameter for the requested input filter.
@return the "WhereN" parameter for the requested input filter.
@param ifg the Input filter to process (zero index).
*/
private String getWhere ( int ifg )
{
	// TODO SAM 2006-04-24 Need to enable other input filter panels
	String delim = ";";	// To separate input filter parts
	InputFilter_JPanel filter_panel = __inputFilter_JPanel;
	String where = filter_panel.toString(ifg,delim).trim();
	return where;
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, ReadColoradoIPP_Command command )
{	String routine = "ReadColoradoIPP_JDialog.initialize";
	__command = command;
	CommandProcessor processor = __command.getCommandProcessor();

	try {
	    Object o = processor.getPropContents("ColoradoIppDMIList");
		if ( o != null ) {
			// Use the first ColoradoIppDMI instance, since input filter
			// information should be relatively consistent...
			List v = (List)o;
			if ( v.size() > 0 ) {
				__ippdmi = (IppDMI)v.get(0);
			}
			else {
				String message = "No Colorado IPP connection is available to use with command editing.\n" +
					"Make sure that Colorado IPP is open.";
				Message.printWarning(1, routine, message );
			}
		}
	}
	catch ( Exception e ){
		// Not fatal, but of use to developers.
		String message = "No Colorado IPP connection is available to use with command editing.\n" +
			"Make sure that Colorado IPP is open.";
		Message.printWarning(1, routine, message );
	}

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"Read one or more time series from the State of Colorado's IPP database."),
    	0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"Constrain the query by specifying time series metadata to match." ), 
    	0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Refer to the Colorado IPP Database Input Type documentation for possible values." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"If not specified, the period defaults to the input period from SetInputPeriod()."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	int buffer = 3;
	Insets insets = new Insets(0,buffer,0,0);
	try {
	    // Add input filters for IPP time series...
		__inputFilter_JPanel = new IPP_DataMetaData_InputFilter_JPanel(__ippdmi, null, __command.getNumFilterGroups() );
   			JGUIUtil.addComponent(main_JPanel, __inputFilter_JPanel,
			0, ++y, 7, 1, 0.0, 0.0, insets, GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST );
   		__inputFilter_JPanel.addEventListeners ( this );
   	    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Subject is required, otherwise optional query filters."),
   	         3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	}
	catch ( Exception e ) {
		Message.printWarning ( 2, routine, "Unable to initialize IPP input filter." );
		Message.printWarning ( 2, routine, e );
	}

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Input start:"), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputStart_JTextField = new JTextField (20);
    __InputStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __InputStart_JTextField,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - YYYY, override the global input start."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Input end:"), 
        0, ++y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputEnd_JTextField = new JTextField (20);
    __InputEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __InputEnd_JTextField,
        1, y, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - YYYY, override the global input end."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    /* TODO SAM 2010-05-23 Currently only support one connection
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Input name:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputName_JTextField = new JTextField ( "" );
    __InputName_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __InputName_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - Colorado IPP connection name (blank for default)."),
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        */
	
    JGUIUtil.addComponent(main_JPanel, new JLabel("Alias to assign:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Alias_JTextField = new TSFormatSpecifiersJPanel(10);
    __Alias_JTextField.setToolTipText("Use %L for location, %T for data type, %I for interval.");
    __Alias_JTextField.addKeyListener ( this );
    __Alias_JTextField.setToolTipText("%L for location, %T for data type.");
    JGUIUtil.addComponent(main_JPanel, __Alias_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - use %L for location, etc. (default=no alias)."),
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
	refresh ();

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
    JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	__cancel_JButton = new SimpleJButton( "Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__ok_JButton = new SimpleJButton("OK", this);
	button_JPanel.add ( __ok_JButton );

	setTitle ( "Edit " + __command.getCommandName() + " Command" );

	// Dialogs do not need to be resizable...
	setResizable ( true );
    pack();
    JGUIUtil.center( this );
	refresh();	// Sets the __path_JButton status
    super.setVisible( true );
}

/**
Handle ItemListener events.
*/
public void itemStateChanged ( ItemEvent event )
{
    refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	refresh();
}

/**
Need this to properly capture key events, especially deletes.
*/
public void keyReleased ( KeyEvent event )
{	refresh();	
}

public void keyTyped ( KeyEvent event )
{
}

/**
Indicate if the user pressed OK (cancel otherwise).
@return true if the edits were committed, false if the user canceled.
*/
public boolean ok ()
{	return __ok;
}

/**
Refresh the command string from the dialog contents.
*/
private void refresh ()
{	String routine = "ReadColoradoIPP_JDialog.refresh";
	__error_wait = false;
	//String InputName = "";
	String filter_delim = ";";
	String InputStart = "";
	String InputEnd = "";
	String Alias = "";
	PropList props = null;
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		props = __command.getCommandParameters();
		//Interval = props.getValue ( "Interval" );
		//InputName = props.getValue ( "InputName" );
		InputStart = props.getValue ( "InputStart" );
		InputEnd = props.getValue ( "InputEnd" );
		Alias = props.getValue ( "Alias" );
        InputFilter_JPanel filter_panel = __inputFilter_JPanel;
        int nfg = filter_panel.getNumFilterGroups();
        String where;
        for ( int ifg = 0; ifg < nfg; ifg ++ ) {
            where = props.getValue ( "Where" + (ifg + 1) );
            if ( (where != null) && (where.length() > 0) ) {
                // Set the filter...
                try {
                    filter_panel.setInputFilter (ifg, where, filter_delim );
                }
                catch ( Exception e ) {
                    Message.printWarning ( 1, routine, "Error setting where information using \"" + where + "\"" );
                    Message.printWarning ( 3, routine, e );
                }
            }
        }
		//if ( InputName != null ) {
		//	__InputName_JTextField.setText (InputName );
		//}
		if ( InputStart != null ) {
			__InputStart_JTextField.setText ( InputStart );
		}
		if ( InputEnd != null ) {
			__InputEnd_JTextField.setText ( InputEnd );
		}
        if ( Alias != null ) {
            __Alias_JTextField.setText ( Alias );
        }
	}
	// Regardless, reset the command from the fields...
	//InputName = __InputName_JTextField.getText().trim();
	//props.add ( "InputName=" + InputName );
	Alias = __Alias_JTextField.getText().trim();
	// Regardless, reset the command from the fields...
	props = new PropList ( __command.getCommandName() );
	// Add the where clause(s)...
	InputFilter_JPanel filter_panel = __inputFilter_JPanel;
	int nfg = filter_panel.getNumFilterGroups();
	String where;
	String delim = ";";	// To separate input filter parts
	for ( int ifg = 0; ifg < nfg; ifg ++ ) {
		where = filter_panel.toString(ifg,delim).trim();
		// Make sure there is a field that is being checked in a where clause...
		if ( (where.length() > 0) && !where.startsWith(delim) ) {
			props.add ( "Where" + (ifg + 1) + "=" + where );
		}
	}
	InputStart = __InputStart_JTextField.getText().trim();
	props.add ( "InputStart=" + InputStart );
	InputEnd = __InputEnd_JTextField.getText().trim();
	props.add ( "InputEnd=" + InputEnd );
	props.add ( "Alias=" + Alias );
	__command_JTextArea.setText( __command.toString ( props ) );

	// Check the GUI state to determine whether some controls should be disabled.

	checkGUIState();
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed
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