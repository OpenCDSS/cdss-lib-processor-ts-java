package rti.tscommandprocessor.commands.ts;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;

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
import RTi.TS.TSIdent;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

// TODO SAM 2009-01-15 Implement field-based time series editor, consistent with that used
// in other commands where NewTSID is entered, like the NewTimeSeries() command.
/**
Editor dialog for time series identifiers.  Currently a simple text field is provided
but in the future separate fields may be used for TSID parts.
*/
@SuppressWarnings("serial")
public class TSID_JDialog extends JDialog
implements ActionListener, KeyListener, WindowListener
{
    
private final String __RemoveWorkingDirectory = "Rel";
private final String __AddWorkingDirectory = "Abs";
    
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private SimpleJButton __path_JButton = null;
private TSID_Command __command = null;
private JTextArea __command_JTextArea=null;
private String __working_dir = null; // Working directory.
private JTextField __TSID_JTextField=null;
private boolean __first_time = true;
private boolean __error_wait = false;
private boolean __ok = false; // Whether OK has been pressed.

TSIdent __tsident = null; // Time series identifier object

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public TSID_JDialog (	JFrame parent, TSID_Command command )
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
		HelpViewer.getInstance().showHelp("command", "TSID");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
    else if ( o == __path_JButton ) {
        // Get the time series identifier
        TSIdent tsident = null;
        try {
            tsident = new TSIdent ( __TSID_JTextField.getText().trim() );
        }
        catch ( Exception e ) {
            tsident = null;
        }
        if ( tsident == null ) {
            // Nothing to do.
            return;
        }
        // Convert the input name
        String inputName = tsident.getInputName();
        if ( __path_JButton.getText().equals( __AddWorkingDirectory) ) {
            tsident.setInputName( IOUtil.toAbsolutePath(__working_dir,inputName ) );
            __TSID_JTextField.setText ( tsident.toString(true) );
        }
        else if ( __path_JButton.getText().equals( __RemoveWorkingDirectory) ) {
            try {
                tsident.setInputName ( IOUtil.toRelativePath ( __working_dir, inputName ) );
                __TSID_JTextField.setText ( tsident.toString(true) );
            }
            catch ( Exception e ) {
                Message.printWarning ( 1, "ReadHecDss_JDialog", "Error converting file to relative path." );
            }
        }
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
    String TSID = __TSID_JTextField.getText().trim();
    __error_wait = false;

    if ( (TSID != null) && (TSID.length() > 0) ) {
        props.set ( "TSID", TSID );
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
{   String TSID = __TSID_JTextField.getText().trim();
    __command.setCommandParameter ( "TSID", TSID );
}

/**
Free memory for garbage collection.
@exception Throwable if there is an error.
*/
protected void finalize ()
throws Throwable
{	__cancel_JButton = null;
	__command_JTextArea = null;
	__command = null;
	__ok_JButton = null;
	__TSID_JTextField = null;
	super.finalize ();
}

/**
Indicate whether the time series identifier has a filename.  This is determined from the input type.
Databases will not have filenames.
@param inputType input type from time series identifier.
*/
private boolean identifierHasFilename ( TSIdent tsident )
{
    if ( tsident == null ) {
        // Initialization?
        return false;
    }
    String inputType = tsident.getInputType();
    if ( inputType.equalsIgnoreCase("HydroBase") || inputType.equalsIgnoreCase("RiversideDB") ) {
        // Databases so no filename
        return false;
    }
    else {
        return true;
    }
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, TSID_Command command )
{	__command = command;
    CommandProcessor processor = __command.getCommandProcessor();
    __working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( (TSCommandProcessor)processor, __command );
    
    try {
        String TSID = __command.getCommandParameters().getValue("TSID");
        __tsident = new TSIdent(TSID);
    }
    catch ( Exception e ) {
        __tsident = null;
    }

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"This command reads a single time series given a time series identifier (TDID) that includes " +
		"the datastore (database, web service) or input name (database, file) information."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The TSID parts are generally consistent but do vary slightly between datastores based on requirements to uniquely identify time series."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"See also the ReadTimeSeries() command, which assigns an alias to a time series. " +
		"The alias may be more convenient to use than the long time series identifier."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"More specific Read commands may also available for a datastore or input type and generally offer more options."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "See also the ReadTimeSeriesList() command, which creates TSIDs from a table and then reads each time series."),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    if ( identifierHasFilename(__tsident) ) {
        JGUIUtil.addComponent(main_JPanel, new JLabel (
            "If the TSID requires a filename, specify a full path or relative path (relative to working directory) for the file to read." ), 
            0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        if ( __working_dir != null ) {
            JGUIUtil.addComponent(main_JPanel, new JLabel (
            "The working directory is: " + __working_dir ), 
            0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        }
        JGUIUtil.addComponent(main_JPanel, new JLabel (
	        "<HTML><B>It is strongly recommended that relative paths are used in commands if possible.</B></HTML>." ), 
	        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel,new JLabel("Time series identifier:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__TSID_JTextField = new JTextField ( 60 );
	__TSID_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __TSID_JTextField,
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );
    // FIXME SAM 2009-01-15 Figure out how to not hard code here.  Basically only want to show the path button
    // for input types that are for files (not databases).
    if ( (__working_dir != null) && identifierHasFilename(__tsident) ) {
		// Add the button to allow conversion to/from relative path...
		__path_JButton = new SimpleJButton(__RemoveWorkingDirectory,this);
	    JGUIUtil.addComponent(main_JPanel, __path_JButton,
	    	7, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	}
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __command_JTextArea = new JTextArea (4,50);
    __command_JTextArea.setLineWrap ( true );
    __command_JTextArea.setWrapStyleWord ( true );
    __command_JTextArea.setEditable ( false );
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
        1, y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);

	// Refresh the contents...
	refresh();

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);
        
    // Refresh here so that the TSID text field is populated
    refresh();

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
	refreshPathControl();   // Sets the __path_JButton status
	setResizable ( false );
    super.setVisible( true );
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
@return true if the edits were committed, false if the user cancelled.
*/
public boolean ok ()
{   return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{   String TSID = "";
    PropList props = __command.getCommandParameters();
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command...
        TSID = props.getValue ( "TSID" );
        if ( TSID != null ) {
            __TSID_JTextField.setText ( TSID );
        }
    }
    // Regardless, reset the command from the fields...
    TSID = __TSID_JTextField.getText().trim();
    props = new PropList ( __command.getCommandName() );
    props.add ( "TSID=" + TSID );
    __command_JTextArea.setText( __command.toString ( props ) );
    // Refresh the Path Control text.
    refreshPathControl();
}

/**
Refresh the PathControl text based on the contents of the input text field contents.
*/
private void refreshPathControl()
{   TSIdent tsident = null;
    try {
        tsident = new TSIdent ( __TSID_JTextField.getText().trim() );
    }
    catch ( Exception e ) {
        tsident = null;
    }
    if ( tsident == null ) {
        // Nothing to do.
        return;
    }
    String InputFile = tsident.getInputName();
    if ( (InputFile == null) || (InputFile.trim().length() == 0) ) {
        if ( __path_JButton != null ) {
            __path_JButton.setEnabled ( false );
        }
        return;
    }

    // Check the path and determine what the label on the path button should be...
    if ( __path_JButton != null ) {
        __path_JButton.setEnabled ( true );
        File f = new File ( InputFile );
        if ( f.isAbsolute() ) {
            __path_JButton.setText( __RemoveWorkingDirectory );
			__path_JButton.setToolTipText("Change path to relative to command file");
        }
        else {
            __path_JButton.setText( __AddWorkingDirectory );
			__path_JButton.setToolTipText("Change path to absolute");
        }
    }
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