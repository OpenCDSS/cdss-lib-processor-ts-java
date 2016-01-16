package rti.tscommandprocessor.commands.riverware;

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

import javax.swing.JFileChooser;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.io.File;

import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Editor dialog for the ReadRiverWare() command.
*/
public class ReadRiverWare_JDialog extends JDialog
implements ActionListener, DocumentListener, KeyListener, WindowListener
{
    
private final String
    __RemoveWorkingDirectory = "Remove Working Directory",
    __AddWorkingDirectory = "Add Working Directory";
    
private SimpleJButton __browse_JButton = null;
private SimpleJButton __path_JButton = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private ReadRiverWare_Command __command = null;
private String __working_dir = null;
private SimpleJComboBox __Output_JComboBox = null;
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
private JTextField __InputStart_JTextField;
private JTextField __InputEnd_JTextField;
private JTextField __InputFile_JTextField = null;
//__Units_JTextField = null;// Units to convert to at read
private JTabbedPane __rw_JTabbedPane = null;
private JTextArea __command_JTextArea = null;
private boolean __error_wait = false;	// Is there an error to be cleared up
private boolean __first_time = true;
private boolean __ok = false;

/**
Command editor dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public ReadRiverWare_JDialog ( JFrame parent, ReadRiverWare_Command command )
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
		// Browse for the file to read...
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle( "Select RiverWare Time Series File");
        SimpleFileFilter sff = new SimpleFileFilter("rdf","RiverWare Data Format (RDF) File (*.rdf)");
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
				JGUIUtil.setLastFileDialogDirectory(directory );
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
			__InputFile_JTextField.setText (
			IOUtil.toAbsolutePath(__working_dir, __InputFile_JTextField.getText() ) );
		}
		else if ( __path_JButton.getText().equals( __RemoveWorkingDirectory) ) {
			try {
			    __InputFile_JTextField.setText (
				IOUtil.toRelativePath ( __working_dir, __InputFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1,
				"ReadRiverWare_JDialog", "Error converting file to relative path." );
			}
		}
		refresh ();
	}
	else {
	    refresh();
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
{
    // Put together a list of parameters to check...
    PropList props = new PropList ( "" );
    String InputFile = __InputFile_JTextField.getText().trim();
    String Output = __Output_JComboBox.getSelected();
    String InputStart = __InputStart_JTextField.getText().trim();
    String InputEnd = __InputEnd_JTextField.getText().trim();
    //String Units = __Units_JTextField.getText().trim();
    String Alias = __Alias_JTextField.getText().trim();
    
    __error_wait = false;

    if (Output.length() > 0) {
        props.set("Output", Output);
    }
    if (InputFile.length() > 0) {
        props.set("InputFile", InputFile);
    }
    if (InputStart.length() > 0 && !InputStart.equals("*")) {
        props.set("InputStart", InputStart);
    }
    if (InputEnd.length() > 0 && !InputEnd.equals("*")) {
        props.set("InputEnd", InputEnd);
    }
   // if (Units.length() > 0 && !Units.equals("*")) {
    //    props.set("Units", Units);
    //}
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
{
    String InputFile = __InputFile_JTextField.getText().trim();
    String Output = __Output_JComboBox.getSelected();
    String InputStart = __InputStart_JTextField.getText().trim();
    String InputEnd = __InputEnd_JTextField.getText().trim();
    //String Units = __Units_JTextField.getText().trim();
    String Alias = __Alias_JTextField.getText().trim();

    __command.setCommandParameter("InputFile", InputFile);
    __command.setCommandParameter("Output", Output);
    __command.setCommandParameter("InputStart", InputStart);
    __command.setCommandParameter("InputEnd", InputEnd);
    //__command.setCommandParameter("Units", Units);
    __command.setCommandParameter("Alias", Alias);
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, ReadRiverWare_Command command )
{   __command = command;
    __working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( (TSCommandProcessor)__command.getCommandProcessor(), __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Read time series from a RiverWare time series file." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specify a full path or relative path (relative to working directory) for a RiverWare file to read." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specifying the period will limit data that are available for fill commands but can increase performance." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   //	JGUIUtil.addComponent(main_JPanel, new JLabel (
	//	"Specifying units causes conversion during the read " +
	//	"(new units must be understood by TSTool).  See also Scale() and ConvertDataUnits()."),
	//	0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"If not specified, the period defaults to the global input period."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	if ( __working_dir != null ) {
        JGUIUtil.addComponent(main_JPanel, new JLabel ( "The working directory is: " + __working_dir ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
   	JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "RiverWare file to read:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputFile_JTextField = new JTextField ( 50 );
	__InputFile_JTextField.setToolTipText("Specify the path to the input file or use ${Property} notation");
	__InputFile_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __InputFile_JTextField,
		1, y, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browse_JButton = new SimpleJButton ( "Browse", this );
    JGUIUtil.addComponent(main_JPanel, __browse_JButton,
		6, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
    
    __rw_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __rw_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
     
    // Panel for single column output
    int ySingle = -1;
    JPanel singleJPanel = new JPanel();
    singleJPanel.setLayout( new GridBagLayout() );
    __rw_JTabbedPane.addTab ( "Time Series File", singleJPanel );
    
    JGUIUtil.addComponent(singleJPanel, new JLabel (
        "Single time series files are read into a single time series in TSTool."),
        0, ++ySingle, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(singleJPanel, new JLabel (
        "It is assumed that the filename starts with ObjectName.SlotName."),
        0, ++ySingle, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(singleJPanel, new JLabel (
        "The ObjectName and SlotName will be used for the time series identifier location and data type, respectively." ),
        0, ++ySingle, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    int yRdf = -1;
    JPanel rdfJPanel = new JPanel();
    rdfJPanel.setLayout( new GridBagLayout() );
    __rw_JTabbedPane.addTab ( "RDF File", rdfJPanel );
    
    JGUIUtil.addComponent(rdfJPanel, new JLabel (
        "RiverWare Data Format (RDF) files are read into a list of time series and optionally ensemble(s)."),
        0, ++yRdf, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(rdfJPanel, new JLabel (
        "Each ensemble time series has location ID = ObjectName, data type = SlotName, and the sequence identifier = run number (1+)."),
        0, ++yRdf, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(rdfJPanel, new JLabel (
        "The ensemble ID is set to ObjectName_SlotName."),
        0, ++yRdf, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(rdfJPanel, new JLabel ( "Output"),
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Output_JComboBox = new SimpleJComboBox ( false );
    __Output_JComboBox.addItem ( "" );
    __Output_JComboBox.addItem ( __command._TimeSeries );
    __Output_JComboBox.addItem ( __command._TimeSeriesAndEnsembles );
    __Output_JComboBox.select ( 0 );
    __Output_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(rdfJPanel, __Output_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(rdfJPanel, new JLabel(
        "Optional - output to generate (default=" + __command._TimeSeries + ")."), 
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel("Alias to assign:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Alias_JTextField = new TSFormatSpecifiersJPanel(15);
    __Alias_JTextField.addKeyListener ( this );
    __Alias_JTextField.getDocument().addDocumentListener(this);
    JGUIUtil.addComponent(main_JPanel, __Alias_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Required - use %L for location, etc."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    /*
    JGUIUtil.addComponent(main_JPanel, new JLabel("Units to convert to:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Units_JTextField = new JTextField ( "", 10 );
	__Units_JTextField.addKeyListener ( this );
	__Units_JTextField.setEnabled ( false );
	JGUIUtil.addComponent(main_JPanel, __Units_JTextField,
		1, y, 3, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
		*/

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Input start:"), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputStart_JTextField = new JTextField (20);
    __InputStart_JTextField.setToolTipText("Specify the input start using a date/time string or ${Property} notation");
    __InputStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __InputStart_JTextField,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - overrides the global input start."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Input end:"), 
        0, ++y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputEnd_JTextField = new JTextField (20);
    __InputEnd_JTextField.setToolTipText("Specify the input end using a date/time string or ${Property} notation");
    __InputEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __InputEnd_JTextField,
        1, y, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - overrides the global input end."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

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
		__path_JButton = new SimpleJButton( __RemoveWorkingDirectory, this);
		button_JPanel.add ( __path_JButton );
	}
	__cancel_JButton = new SimpleJButton("Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__ok_JButton = new SimpleJButton("OK", this);
	button_JPanel.add ( __ok_JButton );

    setTitle("Edit " + __command.getCommandName() + "() Command");
	setResizable ( true );
    pack();
    JGUIUtil.center( this );
	refresh();	// Sets the __path_JButton status
    super.setVisible( true );
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	int code = event.getKeyCode();

	// Don't want key input to refresh dates unless enter or tab...
	if (	(event.getSource() == __InputStart_JTextField) ||
		(event.getSource() == __InputEnd_JTextField) ) {
		if ( (code == KeyEvent.VK_ENTER) || (code == KeyEvent.VK_TAB) ) {
			refresh ();
		}
	}
	else {	refresh();
	}
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
public boolean ok() {
    return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh()
{   String routine = "ReadRiverWare_JDialog.refresh";
    String InputFile = "";
    String Output = "";
    String InputStart = "";
    String InputEnd = "";
    //String NewUnits = "";
    String Alias = "";

    PropList props = null;

    if (__first_time) {
        __first_time = false;

        // Get the properties from the command
        props = __command.getCommandParameters();
        InputFile = props.getValue("InputFile");
        Output = props.getValue("Output");
        InputStart = props.getValue("InputStart");
        InputEnd = props.getValue("InputEnd");
        //NewUnits = props.getValue("NewUnits");
        Alias = props.getValue("Alias");

        // Set the control fields
        if (Alias != null) {
            __Alias_JTextField.setText(Alias.trim());
        }
        if (InputFile != null) {
            __InputFile_JTextField.setText(InputFile);
            if ( InputFile.toUpperCase().endsWith("RDF") ) {
                __rw_JTabbedPane.setSelectedIndex(1);
            }
        }
        if ( JGUIUtil.isSimpleJComboBoxItem(__Output_JComboBox, Output, JGUIUtil.NONE, null, null ) ) {
            __Output_JComboBox.select ( Output );
        }
        else {
            if ( (Output == null) || Output.equals("") ) {
                // New command...select the default...
                __Output_JComboBox.select ( 0 );
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                "Output parameter \"" + Output + "\".  Select a\ndifferent value or Cancel." );
            }
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
        }*/
    }

    // Regardless, reset the command from the fields.  This is only  visible
    // information that has not been committed in the command.
    InputFile = __InputFile_JTextField.getText().trim();
    Output = __Output_JComboBox.getSelected();
    InputStart = __InputStart_JTextField.getText().trim();
    InputEnd = __InputEnd_JTextField.getText().trim();
    //NewUnits = __NewUnits_JTextField.getText().trim();
    Alias = __Alias_JTextField.getText().trim();

    props = new PropList(__command.getCommandName());
    props.add("InputFile=" + InputFile);
    props.add("Output=" + Output);
    props.add("InputStart=" + InputStart);
    props.add("InputEnd=" + InputEnd);
    //props.add("NewUnits=" + NewUnits);
    if (Alias != null) {
        props.add("Alias=" + Alias);
    }
    
    __command_JTextArea.setText( __command.toString(props) );

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
            __path_JButton.setText( __RemoveWorkingDirectory);
        }
        else {
            __path_JButton.setText( __AddWorkingDirectory);
        }
    }
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed and the dialog is closed.
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