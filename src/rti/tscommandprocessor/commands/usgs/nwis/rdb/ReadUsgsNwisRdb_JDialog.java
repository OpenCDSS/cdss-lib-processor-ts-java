package rti.tscommandprocessor.commands.usgs.nwis.rdb;

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
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
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
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Time.TimeInterval;

/**
Editor dialog for the ReadUsgsNwis() command.
*/
@SuppressWarnings("serial")
public class ReadUsgsNwisRdb_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{
    
private final String __RemoveWorkingDirectory = "Rel";
private final String __AddWorkingDirectory = "Abs";
    
private SimpleJButton __browse_JButton = null;
private SimpleJButton __path_JButton = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private ReadUsgsNwisRdb_Command __command = null;// Command to edit
private String __working_dir = null; // Working directory.
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
private JTextField __DataType_JTextField = null;
private SimpleJComboBox __Interval_JComboBox = null;
private JTextField __Units_JTextField = null;
private JTextField __InputStart_JTextField = null;
private JTextField __InputEnd_JTextField = null;
private JTextField __InputFile_JTextField = null;
private JTextArea __command_JTextArea = null;
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false;

/**
Editor dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public ReadUsgsNwisRdb_JDialog ( JFrame parent, ReadUsgsNwisRdb_Command command )
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
		fc.setDialogTitle( "Select USGS NWIS Time Series File");
		SimpleFileFilter sff = new SimpleFileFilter("txt", "USGS NWIS Daily Time Series File");
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
				// Convert path to relative path by default.
				try {
					__InputFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1,"ReadUsgsNwisRdb_JDialog", "Error converting file to relative path." );
				}
				JGUIUtil.setLastFileDialogDirectory(directory );
				refresh();
			}
		}
	}
	else if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "ReadUsgsNwisRdb");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput ();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else if ( o == __path_JButton ) {
		if ( __path_JButton.getText().equals(__AddWorkingDirectory) ) {
			__InputFile_JTextField.setText (
			IOUtil.toAbsolutePath(__working_dir, __InputFile_JTextField.getText() ) );
		}
		else if ( __path_JButton.getText().equals( __RemoveWorkingDirectory) ) {
			try {	__InputFile_JTextField.setText (
				IOUtil.toRelativePath ( __working_dir, __InputFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1,
				"ReadUsgsNwis_JDialog", "Error converting file to relative path." );
			}
		}
		refresh ();
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
Check the GUI state to make sure that appropriate components are enabled/disabled.
*/
private void checkGUIState ()
{
    // Add to this as more functionality is added
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{
    // Put together a list of parameters to check...
    PropList props = new PropList ( "" );
    String DataType = __DataType_JTextField.getText().trim();
	String Interval = __Interval_JComboBox.getSelected();
	String Units = __Units_JTextField.getText().trim();
    String InputFile = __InputFile_JTextField.getText().trim();
    String InputStart = __InputStart_JTextField.getText().trim();
    String InputEnd = __InputEnd_JTextField.getText().trim();
    //String NewUnits = __NewUnits_JTextField.getText().trim();
    String Alias = __Alias_JTextField.getText().trim();
    
    __error_wait = false;
    
    if (InputFile.length() > 0) {
        props.set("InputFile", InputFile);
    }
    if (DataType.length() > 0) {
        props.set("DataType", DataType);
    }
    if (Interval.length() > 0) {
        props.set("Interval", Interval);
    }
    if (Units.length() > 0) {
        props.set("Units", Units);
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
    String InputFile = __InputFile_JTextField.getText().trim();
    String DataType = __DataType_JTextField.getText().trim();
	String Interval = __Interval_JComboBox.getSelected();
	String Units = __Units_JTextField.getText().trim();
    String InputStart = __InputStart_JTextField.getText().trim();
    String InputEnd = __InputEnd_JTextField.getText().trim();
    //String NewUnits = __NewUnits_JTextField.getText().trim();

    __command.setCommandParameter("InputFile", InputFile);
    __command.setCommandParameter("DataType", DataType);
	__command.setCommandParameter("Interval", Interval);
	__command.setCommandParameter("Units", Units);
    __command.setCommandParameter("InputStart", InputStart);
    __command.setCommandParameter("InputEnd", InputEnd);
    //__command.setCommandParameter("NewUnits", NewUnits);
    __command.setCommandParameter("Alias", Alias);
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, ReadUsgsNwisRdb_Command command )
{	__command = command;
    __working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( (TSCommandProcessor)__command.getCommandProcessor(), __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Read a single time series from a USGS NWIS RDB format file (only first data value and accompanying flag will be read)." ), 
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specifying the period will limit data that are available " +
		"for fill commands but can increase performance." ), 
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"If not specified, the period defaults to the global input period, or read all."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	if ( __working_dir != null ) {
      	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The working directory is: " + __working_dir ), 
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	
    JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL), 
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.EAST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "USGS NWIS RDB file to read:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputFile_JTextField = new JTextField ( 50 );
	__InputFile_JTextField.setToolTipText("Specify the path to the input file or use ${Property} notation");
	__InputFile_JTextField.addKeyListener ( this );
    // Input file layout fights back with other rows so put in its own panel
	JPanel InputFile_JPanel = new JPanel();
	InputFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(InputFile_JPanel, __InputFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browse_JButton = new SimpleJButton ( "...", this );
	__browse_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(InputFile_JPanel, __browse_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__path_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(InputFile_JPanel, __path_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(main_JPanel, InputFile_JPanel,
		1, y, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        
    JGUIUtil.addComponent(main_JPanel, new JLabel("Alias to assign:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Alias_JTextField = new TSFormatSpecifiersJPanel(15);
    __Alias_JTextField.addKeyListener ( this );
    __Alias_JTextField.getDocument().addDocumentListener(this);
    JGUIUtil.addComponent(main_JPanel, __Alias_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Required - use %L for location, etc."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel("Data type:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__DataType_JTextField = new JTextField ( 10 );
	__DataType_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(main_JPanel, __DataType_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - data type (default=column heading from file)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Data interval:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Interval_JComboBox = new SimpleJComboBox ( false );
    List<String> intervals = new ArrayList<String>();
    intervals.add(""+TimeInterval.getName(TimeInterval.DAY, 0));
    intervals.add("15"+TimeInterval.getName(TimeInterval.MINUTE, 0));
    intervals.add(""+TimeInterval.getName(TimeInterval.IRREGULAR, 0));
    __Interval_JComboBox.setData ( intervals );
    // Select a default...
    __Interval_JComboBox.select ( 0 );
    __Interval_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Interval_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Required - data interval for time series."),
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel("Data units:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Units_JTextField = new JTextField ( 10 );
	__Units_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(main_JPanel, __Units_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - units for data (default=blank)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

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
        1, y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

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

    setTitle("Edit " + __command.getCommandName() + " Command");
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
{   checkGUIState();
    refresh();
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
private void refresh() {
	String message, routine = getClass().getSimpleName() + ".refresh";
    String InputFile = "";
    String DataType = "";
    String Interval = "";
    String Units = "";
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
        DataType = props.getValue("DataType");
        Interval = props.getValue("Interval");
        Units = props.getValue("Units");
        InputStart = props.getValue("InputStart");
        InputEnd = props.getValue("InputEnd");
        //NewUnits = props.getValue("NewUnits");
        Alias = props.getValue("Alias");

        // Set the control fields
        if ( Alias != null ) {
            __Alias_JTextField.setText(Alias.trim());
        }
        if (InputFile != null) {
            __InputFile_JTextField.setText(InputFile);
        }
        if (DataType != null) {
            __DataType_JTextField.setText(DataType);
        }
        if ( Interval == null || Interval.equals("") ) {
            // Select a default...
            __Interval_JComboBox.select ( 0 );
        } 
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __Interval_JComboBox, Interval, JGUIUtil.NONE, null, null ) ) {
                __Interval_JComboBox.select ( Interval );
            }
            else {
                message = "Existing command references an invalid\nInterval \"" + Interval + "\".  "
                    +"Select a different choice or Cancel.";
                Message.printWarning ( 1, routine, message );
            }
        }
        if (Units != null) {
            __Units_JTextField.setText(Units);
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
    DataType = __DataType_JTextField.getText().trim();
    Interval = __Interval_JComboBox.getSelected();
    Units = __Units_JTextField.getText().trim();
    InputStart = __InputStart_JTextField.getText().trim();
    InputEnd = __InputEnd_JTextField.getText().trim();
    //NewUnits = __NewUnits_JTextField.getText().trim();
    Alias = __Alias_JTextField.getText().trim();
    props = new PropList(__command.getCommandName());
    props.add("InputFile=" + InputFile);
    props.add("DataType=" + DataType );
    props.add("Interval=" + Interval );
    props.add("Units=" + Units );
    props.add("InputStart=" + InputStart);
    props.add("InputEnd=" + InputEnd);
    //props.add("NewUnits=" + NewUnits);
    if (Alias != null) {
        props.add("Alias=" + Alias);
    }
    
    __command_JTextArea.setText( __command.toString(props).trim() );

    // Refresh the Path Control text.
    refreshPathControl();
}

/**
Refresh the PathControl text based on the contents of the input text field contents.
*/
private void refreshPathControl() {
    String InputFile = __InputFile_JTextField.getText().trim();
    if ( (InputFile == null) || (InputFile.trim().length() == 0) ) {
        if ( __path_JButton != null ) {
            __path_JButton.setEnabled ( false );
        }
        return;
    }

	// Check the path and determine what the label on the path button should be...
	if ( __path_JButton != null ) {
		if ( (InputFile != null) && !InputFile.isEmpty() ) {
			__path_JButton.setEnabled ( true );
			File f = new File ( InputFile );
			if ( f.isAbsolute() ) {
				__path_JButton.setText ( __RemoveWorkingDirectory );
				__path_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
		    	__path_JButton.setText ( __AddWorkingDirectory );
		    	__path_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__path_JButton.setEnabled(false);
		}
	}
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed
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

}
