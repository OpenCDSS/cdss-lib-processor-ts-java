package rti.tscommandprocessor.commands.nwsrfs;

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
import java.io.File;
//import java.util.List;
//import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.TS.TSIdent;

//import RTi.Util.GUI.InputFilter_JPanel;
import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Editor dialog for the the ReadNwsrfsFS5Files().
*/
public class ReadNwsrfsFS5Files_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{
private final String
    __RemoveWorkingDirectory = "Remove Working Directory",
    __AddWorkingDirectory = "Add Working Directory";
    
private SimpleJButton	__cancel_JButton = null,// Cancel Button
			__ok_JButton = null,	// Ok Button
			__browse_JButton = null,	// To pick FS5Files dir
			__path_JButton = null;	// Convert between relative and absolute paths
private ReadNwsrfsFS5Files_Command __command = null;
private TSFormatSpecifiersJPanel __Alias_JTextField = null;// Alias for time series.
private JTextField __Location_JTextField, // Location part of TSID
			__DataSource_JTextField,// Data source part of TSID
			__DataType_JTextField,	// Data type part of TSID
			__Interval_JTextField,	// Interval part of TSID
			__InputName_JTextField,	// Input name (FS5Files dir)
			__TSID_JTextField,	// Full TSID
			__InputStart_JTextField,
			__InputEnd_JTextField,
			__Units_JTextField;	// Units to return
private JTextArea __command_JTextArea = null;
//private List __input_filter_JPanel_Vector = new Vector();
//private InputFilter_JPanel __input_filter_NWSRFS_FS5Files_JPanel = null;
private boolean __error_wait = false;	// Is there an error waiting to be cleared up?
private String __working_dir = null;	// Working directory.
private boolean __first_time = true;
private boolean __ok = false;

/**
Command editor dialog constructor.
@param parent JFrame class instantiating this class.
@param command Time series command to edit.
*/
public ReadNwsrfsFS5Files_JDialog( JFrame parent, ReadNwsrfsFS5Files_Command command )
{	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	Object o = event.getSource();

	if ( o == __browse_JButton ) {
		String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
		JFileChooser fc = null;
		if ( last_directory_selected != null ) {
			fc = JFileChooserFactory.createJFileChooser(last_directory_selected );
		}
		else {
		    fc = JFileChooserFactory.createJFileChooser( __working_dir );
		}
        fc.setFileSelectionMode (JFileChooser.DIRECTORIES_ONLY );
		fc.setDialogTitle( "Select NWSRFS FS5Files Directory");
		
		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			String path = fc.getSelectedFile().getPath(); 
			if (path == null || path.equals("")) {
				return;
			}
			if (path != null) {
				__InputName_JTextField.setText(path );
				JGUIUtil.setLastFileDialogDirectory(path );
				refresh();
			}
		}
	}
	else if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput ();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else if ( o == __path_JButton ) {
		if ( __path_JButton.getText().equals( __AddWorkingDirectory) ) {
			__InputName_JTextField.setText (
			IOUtil.toAbsolutePath(__working_dir,__InputName_JTextField.getText() ) );
		}
		else if ( __path_JButton.getText().equals( __RemoveWorkingDirectory) ) {
			try {	
				String relative_path = IOUtil.toRelativePath ( __working_dir,__InputName_JTextField.getText() );
				// TODO SAM 2007-07-13 Evaluate
				// Problem is if blank then the run code tries to use Apps Defaults.
				// Most often the commands file will not be saved with the FS5Files
				//if ( relative_path.equals(".") ) {
				//		relative_path = "";
				//}
				__InputName_JTextField.setText ( relative_path );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1, "ReadNwsrfsFS5Files_JDialog",
				"Error converting directory to relative path." );
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
{
    // Put together a list of parameters to check...
    PropList props = new PropList ( "" );
    String TSID = __TSID_JTextField.getText().trim();
    String InputStart = __InputStart_JTextField.getText().trim();
    String InputEnd = __InputEnd_JTextField.getText().trim();
    String Units = __Units_JTextField.getText().trim();
    String Alias = __Alias_JTextField.getText().trim();
    
    __error_wait = false;
    
    if (TSID.length() > 0) {
        props.set("TSID", TSID);
    }
    if (InputStart.length() > 0 && !InputStart.equals("*")) {
        props.set("InputStart", InputStart);
    }
    if (InputEnd.length() > 0 && !InputEnd.equals("*")) {
        props.set("InputEnd", InputEnd);
    }
    if (Units.length() > 0 && !Units.equals("*")) {
        props.set("Units", Units);
    }
    if (Alias != null && Alias.length() > 0) {
        props.set("Alias", Alias);
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
private void commitEdits()
{   String Alias = __Alias_JTextField.getText().trim();
    String TSID = __TSID_JTextField.getText().trim();
    String InputStart = __InputStart_JTextField.getText().trim();
    String InputEnd = __InputEnd_JTextField.getText().trim();
    String Units = __Units_JTextField.getText().trim();

    __command.setCommandParameter("TSID", TSID);
    __command.setCommandParameter("InputStart", InputStart);
    __command.setCommandParameter("InputEnd", InputEnd);
    __command.setCommandParameter("Units", Units);
    __command.setCommandParameter("Alias", Alias);
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__Alias_JTextField = null;
	__cancel_JButton = null;
	__command_JTextArea = null;
	__command = null;
	__ok_JButton = null;
	super.finalize ();
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, ReadNwsrfsFS5Files_Command command )
{
	__command = command;
    __working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( (TSCommandProcessor)__command.getCommandProcessor(), __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Read a single time series from the NWSRFS FS5Files and assign it an alias."),
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Refer to the NWSRFS FS5Files Input Type documentation for " +
		"possible data type and interval values." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specifying the period will limit data that are available " +
		"for other commands but can increase performance." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"If not specified, the period defaults to the global input period, or all data."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
	"If the FS5Files directory is not specified, App Defaults will be used."),
	0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Location:"),
	0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Location_JTextField = new JTextField ( "" );
	__Location_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Location_JTextField,
	1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Required - station or area ID."),
	3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

   	JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data source:"),
	0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__DataSource_JTextField = new JTextField ( "NWSRFS" );
	__DataSource_JTextField.setEditable ( false );
    	JGUIUtil.addComponent(main_JPanel, __DataSource_JTextField,
	1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Default to NWSRFS."),
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data type:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__DataType_JTextField = new JTextField ( "" );
	__DataType_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __DataType_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Required - for example: MAP, QIN."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Data interval:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Interval_JTextField = new JTextField ( "" );
	__Interval_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Interval_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Required - for example: 6Hour, 24Hour."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    /*
	if ( !__isAliasVersion ) {
		int buffer = 3;
		Insets insets = new Insets(0,buffer,0,0);

		try {	// NWSRFS FS5Files time series...

			PropList filter_props = new PropList ( "" );
			filter_props.set ( "NumFilterGroups=6" );
			__input_filter_NWSRFS_FS5Files_JPanel = new
			NWSRFS_TS_InputFilter_JPanel ();
       		JGUIUtil.addComponent(main_JPanel, __input_filter_NWSRFS_FS5Files_JPanel,
				0, ++y, 7, 1, 0.0, 0.0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );
			__input_filter_JPanel_Vector.add ( __input_filter_NWSRFS_FS5Files_JPanel);
			__input_filter_NWSRFS_FS5Files_JPanel.addEventListeners ( this );
		}
		catch ( Exception e ) {
			Message.printWarning ( 2, routine, "Unable to initialize input filter for NWSRFS FS5Files." );
			Message.printWarning ( 2, routine, e );
		}
	}
	*/

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "FS5Files directory:" ), 
    	0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputName_JTextField = new JTextField ( 50 );
    __InputName_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __InputName_JTextField,
    		1, y, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    __browse_JButton = new SimpleJButton ( "Browse", "Browse", this );
    JGUIUtil.addComponent(main_JPanel, __browse_JButton,
    			6, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "If blank, use Apps Defaults."),
	3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
   	JGUIUtil.addComponent(main_JPanel, new JLabel ( "TSID (full):"),
	0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__TSID_JTextField = new JTextField ( "" );
	__TSID_JTextField.setEditable ( false );
	// No listeners because field is not directly editable and other input will trigger refresh
    	JGUIUtil.addComponent(main_JPanel, __TSID_JTextField,
	1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    	
    JGUIUtil.addComponent(main_JPanel, new JLabel("Alias to assign:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Alias_JTextField = new TSFormatSpecifiersJPanel(10);
    __Alias_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Alias_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Required - use %L for location, etc."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Input start:"), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputStart_JTextField = new JTextField (20);
    __InputStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __InputStart_JTextField,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - overrides the global input start."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Input end:"), 
        0, ++y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputEnd_JTextField = new JTextField (20);
    __InputEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __InputEnd_JTextField,
        1, y, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - overrides the global input end."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Units:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Units_JTextField = new JTextField ( "" );
	__Units_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Units_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
	    "Optional - output units (e.g., CFS if database is CMS)."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __command_JTextArea = new JTextArea(4, 55);
    __command_JTextArea.setLineWrap ( true );
    __command_JTextArea.setWrapStyleWord ( true );  
    __command_JTextArea.setEditable ( false );
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

   	if ( __working_dir != null ) {
   		// Add the button to allow conversion to/from relative path...
   		__path_JButton = new SimpleJButton( __RemoveWorkingDirectory, "Remove Working Directory", this);
   		button_JPanel.add ( __path_JButton );
   	}
	__cancel_JButton = new SimpleJButton( "Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__ok_JButton = new SimpleJButton("OK", this);
	button_JPanel.add ( __ok_JButton );

    setTitle( "Edit " + __command.getCommandName() + " Command" );
	setResizable ( true );
    pack();
    JGUIUtil.center( this );
	refresh();	// Sets the __path_JButton status
    super.setVisible( true );
}

/**
Process ItemEvents.
*/
public void itemStateChanged ( ItemEvent event )
{	refresh();
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
@return true if the edits were committed, false if the user cancelled.
*/
public boolean ok() {
    return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{	//String routine = "ReadNwsrfsFS5Files_JDialog.refresh";
	String Alias = "";
	__error_wait = false;
	String TSID = "";
	String InputName = "";
	//String filter_delim = ";";
	String InputStart = "";
	String InputEnd = "";
	String Units = "";
    PropList props = null;

    if (__first_time) {
        __first_time = false;

        // Get the properties from the command
        props = __command.getCommandParameters();
        InputStart = props.getValue("InputStart");
        InputEnd = props.getValue("InputEnd");
        Units = props.getValue("Units");
        Alias = props.getValue("Alias");
		TSID = props.getValue ( "TSID" );
		
		/*
		if ( !__isAliasVersion ) {
			if ( DataType != null ) {
				__DataType_JTextField.setText(DataType);
			}
			if ( Interval != null ) {
				__Interval_JTextField.setText(Interval);
			}
		}
		*/
	    if ( Alias != null ) {
	        __Alias_JTextField.setText ( Alias );
	    }
	    if ( TSID != null ) {
	        try {
	            TSIdent tsident = new TSIdent ( TSID );
				if ( __Location_JTextField != null ) {
					__Location_JTextField.setText ( tsident.getLocation() );
				}
				if ( __DataSource_JTextField != null ) {
					__DataSource_JTextField.setText( tsident.getSource() );
				}
				__DataType_JTextField.setText (	tsident.getType() );
				__Interval_JTextField.setText (	tsident.getInterval() );
				__InputName_JTextField.setText ( tsident.getInputName() );
			}
			catch ( Exception e ) {
				// For now do nothing.
			}
	    }
	    /*
		if ( !__isAliasVersion ) {
		    InputFilter_JPanel filter_panel = __input_filter_NWSRFS_FS5Files_JPanel;
    		int nfg = filter_panel.getNumFilterGroups();
    		String where;
    		for ( int ifg = 0; ifg < nfg; ifg ++ ) {
    			where = props.getValue ( "Where" + (ifg + 1) );
    			if ( where != null ) {
    				// Set the filter...
    				try {
    				    filter_panel.setInputFilter ( ifg, where, filter_delim );
    				}
    				catch ( Exception e ) {
    					Message.printWarning ( 1, routine,
    					"Error setting where information using \"" + where + "\"" );
    					Message.printWarning ( 2, routine, e );
    				}
    			}
    		}
		}*/
		if ( InputStart != null ) {
			__InputStart_JTextField.setText ( InputStart );
		}
		if ( InputEnd != null ) {
			__InputEnd_JTextField.setText ( InputEnd );
		}
		if ( Units != null ) {
			__Units_JTextField.setText ( Units );
		}
	}
	// Regardless, reset the command from the fields...
	InputName = __InputName_JTextField.getText().trim();
	InputStart = __InputStart_JTextField.getText().trim();
	InputEnd = __InputEnd_JTextField.getText().trim();
	Units = __Units_JTextField.getText().trim();
	Alias = __Alias_JTextField.getText().trim();
	//String fs5files_dir = "";	// Unknown
	//if ( __nwsrfs_dmi != null ) {
	//	fs5files_dir = __nwsrfs_dmi.getFS5FilesLocation();
	//}
	if ( InputName.length() == 0 ) {
		// Don't show a file path...
		// TODO SAM 2007-07-12 Evaluate whether should always show or
		// use more formal data source
		TSID =	__Location_JTextField.getText().trim() + "." +
		__DataSource_JTextField.getText().trim() + "." +
		__DataType_JTextField.getText().trim() + "." +
		__Interval_JTextField.getText().trim() +
		"~NWSRFS_FS5Files";
		__TSID_JTextField.setText ( TSID );
	}
	else {
		// Else show the path that was selected...
		TSID =	__Location_JTextField.getText().trim() + "." +
		__DataSource_JTextField.getText().trim() + "." +
		__DataType_JTextField.getText().trim() + "." +
		__Interval_JTextField.getText().trim() +
		"~NWSRFS_FS5Files~" + InputName;
		__TSID_JTextField.setText ( TSID );
	}
	/*
	else {
	    DataType = __DataType_JTextField.getText().trim();
		Interval = __Interval_JTextField.getText().trim();
	}*/

	/*
	if ( !__isAliasVersion ) {
		// Add the where clause...
		InputFilter_JPanel filter_panel = __input_filter_NWSRFS_FS5Files_JPanel;
		int nfg = filter_panel.getNumFilterGroups();
		String where;
		String delim = ";";	// To separate input filter parts
		for ( int ifg = 0; ifg < nfg; ifg ++ ) {
			where = filter_panel.toString(ifg,delim).trim();
			// Make sure there is a field that is being checked in a where clause...
			if (	(where.length() > 0) &&
				!where.startsWith(delim) ) {
				if ( b.length() > 0 ) {
					b.append ( "," );
				}
				b.append ( "Where" + (ifg + 1) + "=\"" + where + "\"" );
			}
		}
	}
	*/
	
    props = new PropList(__command.getCommandName());
    props.add("InputStart=" + InputStart);
    props.add("InputEnd=" + InputEnd);
    props.add("Units=" + Units);
    props.add("Alias=" + Alias);
    props.add("TSID=" + TSID);
    __command_JTextArea.setText( __command.toString(props) );
    
	// Check the path and determine what the label on the path button should be...
	if ( __path_JButton != null ) {
		__path_JButton.setEnabled ( true );
		File f = new File ( InputName );
		if ( f.isAbsolute() ) {
			__path_JButton.setText ( __RemoveWorkingDirectory );
		}
		else {
		    __path_JButton.setText ( __AddWorkingDirectory );
		}
	}
}

/**
React to the user response.
@param ok if false, then the edit is cancelled.  If true, the edit is committed
and the dialog is closed.
*/
public void response ( boolean ok ) {
    __ok = ok;
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

} // end readNWSRFSFS5Files_JDialog
