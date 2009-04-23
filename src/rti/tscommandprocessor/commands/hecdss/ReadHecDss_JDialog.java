package rti.tscommandprocessor.commands.hecdss;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;

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

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.GUI.SimpleJButton;

import RTi.Util.IO.Command;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;

import RTi.Util.Message.Message;

/**
Editor for the ReadHecDss() command.
*/
public class ReadHecDss_JDialog extends JDialog
implements ActionListener, KeyListener, WindowListener
{
private SimpleJButton	__browse_JButton = null,// File browse button
			__path_JButton = null,	// Convert between relative and absolute path.
			__cancel_JButton = null,// Cancel Button
			__ok_JButton = null;	// Ok Button
private Command __command = null;
private String __working_dir = null; // Working directory.
private JTextField
            __A_JTextField = null,
            __B_JTextField = null,
            __C_JTextField = null,
            __E_JTextField = null,
            __F_JTextField = null,
            __Pathname_JTextField = null,
			__InputStart_JTextField,
			__InputEnd_JTextField,
			__InputFile_JTextField = null,
			__Location_JTextField = null,
			__Alias_JTextField = null; // Alias for time series.
			//__NewUnits_JTextField = null; // Units to convert to at read
private JTextArea __Command_JTextArea = null;
private boolean __error_wait = false;	// Is there an error to be cleared up?
private boolean __first_time = true;

private boolean __ok = false;			
private final String __RemoveWorkingDirectory = "Remove Working Directory";
private final String __AddWorkingDirectory = "Add Working Directory";

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public ReadHecDss_JDialog ( JFrame parent, Command command )
{
	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	Object o = event.getSource();

	if ( o == __browse_JButton ) {
		// Browse for the file to read...
		JFileChooser fc = new JFileChooser();
        fc.setDialogTitle( "Select HEC-DSS File");
        SimpleFileFilter sff = new SimpleFileFilter("dss","HEC-DSS File");
        fc.addChoosableFileFilter(sff);
		
		String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
		if ( last_directory_selected != null ) {
			fc.setCurrentDirectory(	new File(last_directory_selected));
		}
		else {
            fc.setCurrentDirectory(new File(__working_dir));
		}
		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			String directory = fc.getSelectedFile().getParent();
			String filename = fc.getSelectedFile().getName(); 
			String path = fc.getSelectedFile().getPath(); 
	
			if (filename == null || filename.equals("")) {
				return;
			}
	
			if (path != null) {
				__InputFile_JTextField.setText(path );
				JGUIUtil.setLastFileDialogDirectory( directory);
				refresh();
			}
		}
	}
	else if ( o == __cancel_JButton ) {
		response(false);
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput ();
		if (!__error_wait) {
			response(true);
		}
	}
    else if ( o == __path_JButton ) {
        if ( __path_JButton.getText().equals( __AddWorkingDirectory) ) {
            __InputFile_JTextField.setText ( IOUtil.toAbsolutePath(__working_dir,__InputFile_JTextField.getText() ) );
        }
        else if ( __path_JButton.getText().equals( __RemoveWorkingDirectory) ) {
            try {
                __InputFile_JTextField.setText ( IOUtil.toRelativePath ( __working_dir, __InputFile_JTextField.getText() ) );
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
private void checkInput () {
	
	// Put together a list of parameters to check...
	PropList props = new PropList ( "" );
	String InputFile = __InputFile_JTextField.getText().trim();
	String A = __A_JTextField.getText().trim();
	String B = __B_JTextField.getText().trim();
	String C = __C_JTextField.getText().trim();
	String E = __E_JTextField.getText().trim();
	String F = __F_JTextField.getText().trim();
	String Pathname = __Pathname_JTextField.getText().trim();
	String InputStart = __InputStart_JTextField.getText().trim();
	String InputEnd = __InputEnd_JTextField.getText().trim();
	//String NewUnits = __NewUnits_JTextField.getText().trim();
	String Location = __Location_JTextField.getText().trim();
	String Alias = __Alias_JTextField.getText().trim();

	__error_wait = false;
	
	if (InputFile.length() > 0) {
		props.set("InputFile", InputFile);
	}
    if (A.length() > 0) {
        props.set("A", A);
    }
    if (B.length() > 0) {
        props.set("B", B);
    }
    if (C.length() > 0) {
        props.set("C", C);
    }
    if (E.length() > 0) {
        props.set("E", E);
    }
    if (F.length() > 0) {
        props.set("F", F);
    }
    if (Pathname.length() > 0) {
        props.set("Pathname", Pathname);
    }
	if (InputStart.length() > 0 && !InputStart.equals("*")) {
		props.set("InputStart", InputStart);
	}
	if (InputEnd.length() > 0 && !InputEnd.equals("*")) {
		props.set("InputEnd", InputEnd);
	}
	/*
	if (NewUnits.length() > 0 && !NewUnits.equals("*")) {
		props.set("NewUnits", NewUnits);
	}
	*/
    if (Location.length() > 0) {
        props.set("Location", Location);
    }
    if (Alias.length() > 0) {
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
{
	String InputFile = __InputFile_JTextField.getText().trim();
    String A = __A_JTextField.getText().trim();
    String B = __B_JTextField.getText().trim();
    String C = __C_JTextField.getText().trim();
    String E = __E_JTextField.getText().trim();
    String F = __F_JTextField.getText().trim();
    String Pathname = __Pathname_JTextField.getText().trim();
	String InputStart = __InputStart_JTextField.getText().trim();
	String InputEnd = __InputEnd_JTextField.getText().trim();
	//String NewUnits = __NewUnits_JTextField.getText().trim();
	String Location = __Location_JTextField.getText().trim();
	String Alias = __Alias_JTextField.getText().trim();

	__command.setCommandParameter("InputFile", InputFile);
	__command.setCommandParameter("A", A);
    __command.setCommandParameter("B", B);
    __command.setCommandParameter("C", C);
    __command.setCommandParameter("E", E);
    __command.setCommandParameter("F", F);
    __command.setCommandParameter("Pathname", Pathname);
	__command.setCommandParameter("InputStart", InputStart);
	__command.setCommandParameter("InputEnd", InputEnd);
	//__command.setCommandParameter("NewUnits", NewUnits);
	__command.setCommandParameter("Location", Location);
	__command.setCommandParameter("Alias", Alias);
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable {
	__browse_JButton = null;
	__path_JButton = null;
	__cancel_JButton = null;
	__ok_JButton = null;
	__command = null;
	__working_dir = null;
	__Alias_JTextField = null;
	__InputStart_JTextField = null;
	__InputEnd_JTextField = null;
	__InputFile_JTextField = null;
	//__NewUnits_JTextField = null;
	__Command_JTextArea = null;

	super.finalize();
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param app_PropList Properties from application.
@param command Command to edit.
*/
private void initialize(JFrame parent, Command command) {
	__command = command;
	CommandProcessor processor = __command.getCommandProcessor();
	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( (TSCommandProcessor)processor, __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Read time series from a HEC-DSS file."),
        0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Use * in the A, B, C, E, and F parts to filter the time series that are read (or leave blank to read all)." ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
    "The D part (start of period) is handled by specifying the input period." ), 
    0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
    "Or, instead of specifying parts, specify the DSS pathname to read a specific time series " +
    "(the path will be used before the parts)." ), 
    0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
    "The alias can be assigned for time series based on time series properties, for example use " +
    "%L for location (A-part:B-part), %T for data type (C-part), %Z for scenario (F-part)." ), 
    0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specify a full path or relative path (relative to working directory) for a HEC-DSS file to read." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	if ( __working_dir != null ) {
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The working directory is: " + __working_dir ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specifying the input period will limit data that are " +
		"available for fill commands but can increase performance." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel (	"HEC-DSS file to read:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputFile_JTextField = new JTextField ( 50 );
	__InputFile_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __InputFile_JTextField,
		1, y, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browse_JButton = new SimpleJButton ( "Browse", this );
        JGUIUtil.addComponent(main_JPanel, __browse_JButton,
		6, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
        
    JGUIUtil.addComponent(main_JPanel, new JLabel("A part (basin):"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __A_JTextField = new JTextField ( "", 30 );
    __A_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __A_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - A part to match (default=match all)."),
    3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel("B part (location):"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __B_JTextField = new JTextField ( "", 30 );
    __B_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __B_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - B part to match (default=match all)."),
    3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel("C part (parameter):"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __C_JTextField = new JTextField ( "", 30 );
    __C_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __C_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - C part to match (default=match all)."),
    3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel("E part (interval):"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __E_JTextField = new JTextField ( "", 30 );
    __E_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __E_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - E part to match (default=match all)."),
    3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel("F part (scenario):"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __F_JTextField = new JTextField ( "", 30 );
    __F_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __F_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - F part to match (default=match all)."),
    3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel("DSS pathname:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Pathname_JTextField = new JTextField ( "", 30 );
    __Pathname_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Pathname_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - DSS pathname to read (default=use parts from above)."),
    3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    /* TODO - Add later
    JGUIUtil.addComponent(main_JPanel, new JLabel("Units to convert to:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__NewUnits_JTextField = new JTextField ( "", 10 );
	__NewUnits_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(main_JPanel, __NewUnits_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
		*/

	JGUIUtil.addComponent(main_JPanel, new JLabel ( "Period to read:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputStart_JTextField = new JTextField ( "", 15 );
	__InputStart_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(main_JPanel, __InputStart_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel ( "to" ), 
		3, y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	__InputEnd_JTextField = new JTextField ( "", 15 );
	__InputEnd_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(main_JPanel, __InputEnd_JTextField,
		4, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
    JGUIUtil.addComponent(main_JPanel, new JLabel("TSID location to assign:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Location_JTextField = new JTextField ( 30 );
    __Location_JTextField.addKeyListener ( this );
    __Location_JTextField.setToolTipText("%A for A-part, %B for B-part, etc.");
    JGUIUtil.addComponent(main_JPanel, __Location_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - use %A for A-part, etc. (default=%A:%B)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
    JGUIUtil.addComponent(main_JPanel, new JLabel("Alias to assign:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Alias_JTextField = new JTextField ( 30 );
    __Alias_JTextField.addKeyListener ( this );
    __Alias_JTextField.setToolTipText("%L for location (A-part:B-part), %T for data type (C-part), %Z for scenario (F-part).");
    JGUIUtil.addComponent(main_JPanel, __Alias_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - use %L for location, etc. (default=no alias)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Command_JTextArea = new JTextArea(4, 55);
	__Command_JTextArea.setLineWrap ( true );
	__Command_JTextArea.setWrapStyleWord ( true );	
	__Command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__Command_JTextArea),
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__path_JButton = new SimpleJButton(	__RemoveWorkingDirectory, this);
		button_JPanel.add ( __path_JButton );
	}
	__cancel_JButton = new SimpleJButton("Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__ok_JButton = new SimpleJButton("OK", this);
	button_JPanel.add ( __ok_JButton );

	setTitle("Edit " + __command.getCommandName() + "() Command");
	
	// Refresh the contents...
    refresh ();

	setResizable ( true );
    pack();
    JGUIUtil.center( this );
	refreshPathControl();	// Sets the __path_JButton status
    super.setVisible( true );
}

/**
Respond to KeyEvents.
*/
public void keyPressed(KeyEvent event) {
	int code = event.getKeyCode();
	if (code == KeyEvent.VK_ENTER) {
		refresh();
		checkInput();
		if (!__error_wait) {
			response(true);
		}
	}
}

/**
Need this to properly capture key events, especially deletes.
*/
public void keyReleased(KeyEvent event) {
	refresh();
}

public void keyTyped(KeyEvent event) {
	refresh();
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
private void refresh()
{
	String InputFile = "",
    A = "",
    B = "",
    C = "",
    E = "",
    F = "",
    Pathname = "",
    InputStart = "",
    InputEnd = "",
    NewUnits = "",
    Location = "",
    Alias = "";

	PropList props = null;

	if (__first_time) {
		__first_time = false;

		// Get the properties from the command
		props = __command.getCommandParameters();
		InputFile = props.getValue("InputFile");
		A = props.getValue("A");
		B = props.getValue("B");
		C = props.getValue("C");
		E = props.getValue("E");
		F = props.getValue("F");
		Pathname = props.getValue("Pathname");
		InputStart = props.getValue("InputStart");
		InputEnd = props.getValue("InputEnd");
		NewUnits = props.getValue("NewUnits");
		Location = props.getValue("Location");
        Alias = props.getValue("Alias");
		// Set the control fields
        if (A != null) {
            __A_JTextField.setText(A);
        }
        if (B != null) {
            __B_JTextField.setText(B);
        }
        if (C != null) {
            __C_JTextField.setText(C);
        }
        if (E != null) {
            __E_JTextField.setText(E);
        }
        if (F != null) {
            __F_JTextField.setText(F);
        }
        if (Pathname != null) {
            __Pathname_JTextField.setText(Pathname);
        }
		if (InputFile != null) {
			__InputFile_JTextField.setText(InputFile);
		}
		if (InputStart != null) {
			__InputStart_JTextField.setText(InputStart);
		}
		if (InputEnd != null) {
			__InputEnd_JTextField.setText(InputEnd);
		}
		/*
		if (NewUnits != null) {
			__NewUnits_JTextField.setText(NewUnits);
		}
		*/
        if (Location != null ) {
            __Location_JTextField.setText(Location.trim());
        }
        if (Alias != null ) {
            __Alias_JTextField.setText(Alias.trim());
        }
	}

	// Regardless, reset the command from the fields.  This is only  visible
	// information that has not been committed in the command.
	InputFile = __InputFile_JTextField.getText().trim();
    A = __A_JTextField.getText().trim();
    B = __B_JTextField.getText().trim();
    C = __C_JTextField.getText().trim();
    E = __E_JTextField.getText().trim();
    F = __F_JTextField.getText().trim();
    Pathname = __Pathname_JTextField.getText().trim();
	InputStart = __InputStart_JTextField.getText().trim();
	InputEnd = __InputEnd_JTextField.getText().trim();
	//NewUnits = __NewUnits_JTextField.getText().trim();
	Location = __Location_JTextField.getText().trim();
	Alias = __Alias_JTextField.getText().trim();

	props = new PropList(__command.getCommandName());
	props.add("InputFile=" + InputFile);
    props.add("A=" + A);
    props.add("B=" + B);
    props.add("C=" + C);
    props.add("E=" + E);
    props.add("F=" + F);
    props.add("Pathname=" + Pathname);
	props.add("InputStart=" + InputStart);
	props.add("InputEnd=" + InputEnd);
	props.add("NewUnits=" + NewUnits);
	props.add("Location=" + Location);
	props.add("Alias=" + Alias);

	__Command_JTextArea.setText( __command.toString(props) );

	// Refresh the Path Control text.
	refreshPathControl();
}

/**
Refresh the PathControl text based on the contents of the input text field contents.
*/
private void refreshPathControl()
{
	String InputFile = __InputFile_JTextField.getText().trim();
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
			__path_JButton.setText(	__RemoveWorkingDirectory );
		}
		else {
            __path_JButton.setText(	__AddWorkingDirectory );
		}
	}
}

/**
React to the user response.
@param ok if false, then the edit is cancelled.  If true, the edit is committed and the dialog is closed.
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
{	response(false);
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