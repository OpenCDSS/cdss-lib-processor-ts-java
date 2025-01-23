// ProcessRasterGraph_JDialog - Command editor dialog.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2025 Colorado Department of Natural Resources

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

package rti.tscommandprocessor.commands.products;

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

import javax.swing.JFileChooser;
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

import java.io.File;

import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Command editor dialog.
*/
@SuppressWarnings("serial")
public class ProcessRasterGraph_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{

private final String __AddWorkingDirectory = "Abs";
private final String __RemoveWorkingDirectory = "Rel";

private SimpleJButton __browse_JButton = null;
private SimpleJButton __browseOutput_JButton = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private SimpleJButton __path_JButton = null;
private SimpleJButton __pathOutput_JButton = null;
private ProcessRasterGraph_Command __command = null;
private String __working_dir = null;
private JTextArea __command_JTextArea = null;
private JTextField __TSProductFile_JTextField=null;
private SimpleJComboBox	__RunMode_JComboBox = null;
private SimpleJComboBox	__View_JComboBox = null;
private JTextField __OutputFile_JTextField=null;
private JTextField __VisibleStart_JTextField = null;
private JTextField __VisibleEnd_JTextField = null;
private JTextField __CommandStatusProperty_JTextField = null;
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether the user has pressed OK to close the dialog.

/**
Command editor dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public ProcessRasterGraph_JDialog ( JFrame parent, ProcessRasterGraph_Command command ) {
	super ( parent, true );
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event ) {
	Object o = event.getSource();

	if ( o == __browse_JButton ) {
		String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
		JFileChooser fc = null;
		if ( last_directory_selected != null ) {
			fc = JFileChooserFactory.createJFileChooser(last_directory_selected );
		}
		else {
		    fc = JFileChooserFactory.createJFileChooser(__working_dir );
		}
		fc.setDialogTitle("Select Time Series Product File");
		SimpleFileFilter sff = new SimpleFileFilter("tsp", "Time Series Product File");
		fc.addChoosableFileFilter(sff);

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
					__TSProductFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1,"ProcessRasterGraph_JDialog", "Error converting file to relative path." );
				}
				JGUIUtil.setLastFileDialogDirectory( directory);
				refresh();
			}
		}
	}
	else if ( o == __browseOutput_JButton ) {
		String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
		JFileChooser fc = null;
		if ( last_directory_selected != null ) {
			fc = JFileChooserFactory.createJFileChooser(last_directory_selected );
		}
		else {
		    fc = JFileChooserFactory.createJFileChooser(__working_dir );
		}
		fc.setDialogTitle("Select Output Image File");
		SimpleFileFilter sff = new SimpleFileFilter("png", "PNG Image File");
		fc.addChoosableFileFilter(sff);

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
					__OutputFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1,"ProcessTSProduct_JDialog", "Error converting file to relative path." );
				}
				JGUIUtil.setLastFileDialogDirectory( directory);
				refresh();
			}
		}
	}
	else if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "ProcessRasterGraph");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else if ( o == __path_JButton ) {
		if ( __path_JButton.getText().equals( __AddWorkingDirectory) ) {
			__TSProductFile_JTextField.setText ( IOUtil.toAbsolutePath(__working_dir,
			__TSProductFile_JTextField.getText() ) );
		}
		else if ( __path_JButton.getText().equals( __RemoveWorkingDirectory) ) {
			try {
			    __TSProductFile_JTextField.setText ( IOUtil.toRelativePath ( __working_dir,
				__TSProductFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1,
				"processTSProduct_JDialog",	"Error converting file to relative path." );
			}
		}
		refresh ();
	}
	else if ( o == __pathOutput_JButton ) {
		if ( __pathOutput_JButton.getText().equals( __AddWorkingDirectory) ) {
			__OutputFile_JTextField.setText ( IOUtil.toAbsolutePath(__working_dir,
			__OutputFile_JTextField.getText() ) );
		}
		else if ( __pathOutput_JButton.getText().equals( __RemoveWorkingDirectory) ) {
			try {
				__OutputFile_JTextField.setText ( IOUtil.toRelativePath ( __working_dir,
						__OutputFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1,
				"processTSProduct_JDialog",	"Error converting output file to relative path." );
			}
		}
		refresh ();
	}
	else {
	    // Other combo boxes, etc.
		refresh();
	}
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag to true.
This should be called before response() is allowed to complete.
*/
private void checkInput () {
	// Put together a list of parameters to check.
	PropList props = new PropList ( "" );
	String TSProductFile = __TSProductFile_JTextField.getText().trim();
	String RunMode = __RunMode_JComboBox.getSelected();
	String View = __View_JComboBox.getSelected();
	String OutputFile = __OutputFile_JTextField.getText().trim();
    String VisibleStart = __VisibleStart_JTextField.getText().trim();
    String VisibleEnd = __VisibleEnd_JTextField.getText().trim();
    String CommandStatusProperty = __CommandStatusProperty_JTextField.getText().trim();
	__error_wait = false;
	if ( TSProductFile.length() > 0 ) {
		props.set ( "TSProductFile", TSProductFile );
	}
	if ( RunMode.length() > 0 ) {
		props.set ( "RunMode", RunMode );
	}
	if ( View.length() > 0 ) {
		props.set ( "View", View );
	}
	if ( OutputFile.length() > 0 ) {
		props.set ( "OutputFile", OutputFile );
	}
    if ( VisibleStart.length() > 0 ) {
        props.set ( "VisibleStart", VisibleStart );
    }
    if ( VisibleEnd.length() > 0 ) {
        props.set ( "VisibleEnd", VisibleEnd );
    }
    if ( CommandStatusProperty.length() > 0 ) {
        props.set ( "CommandStatusProperty", CommandStatusProperty );
    }
	try {
	    // This will warn the user.
		__command.checkCommandParameters ( props, null, 1 );
	}
	catch ( Exception e ) {
		// The warning would have been printed in the check code.
		__error_wait = true;
	}
}

/**
Commit the edits to the command.
In this case the command parameters have already been checked and no errors were detected.
*/
private void commitEdits () {
	String TSProductFile = __TSProductFile_JTextField.getText().trim();
	String RunMode = __RunMode_JComboBox.getSelected();
	String View = __View_JComboBox.getSelected();
	String OutputFile = __OutputFile_JTextField.getText().trim();
    String VisibleStart = __VisibleStart_JTextField.getText().trim();
    String VisibleEnd = __VisibleEnd_JTextField.getText().trim();
    String CommandStatusProperty = __CommandStatusProperty_JTextField.getText().trim();
	__command.setCommandParameter ( "TSProductFile", TSProductFile );
	__command.setCommandParameter ( "RunMode", RunMode );
	__command.setCommandParameter ( "View", View );
	__command.setCommandParameter ( "OutputFile", OutputFile );
    __command.setCommandParameter ( "VisibleStart", VisibleStart );
    __command.setCommandParameter ( "VisibleEnd", VisibleEnd );
    __command.setCommandParameter ( "CommandStatusProperty", CommandStatusProperty );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, ProcessRasterGraph_Command command ) {
	__command = command;
	CommandProcessor processor = __command.getCommandProcessor();
	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( (TSCommandProcessor)processor, __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel.

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Process a time series product definition file (typically named *.tsp) to create a raster (heat map) graph product." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"This command is equivalent to the ProcessTSProduct command with graph (sub-product) having `GraphType=Raster`."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The command may be enhanced in the future to have features specific to raster graphs." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specify paths relative to the working directory, or use an absolute path."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	if ( __working_dir != null ) {
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The working directory is: " + __working_dir ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}

	JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++y, 8, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel (	"TS product file (TSP):" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__TSProductFile_JTextField = new JTextField ( 50 );
	__TSProductFile_JTextField.addKeyListener ( this );
    // Input file layout fights back with other rows so put in its own panel.
	JPanel TSProductFile_JPanel = new JPanel();
	TSProductFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(TSProductFile_JPanel, __TSProductFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browse_JButton = new SimpleJButton ( "...", this );
	__browse_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(TSProductFile_JPanel, __browse_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path.
		__path_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(TSProductFile_JPanel, __path_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(main_JPanel, TSProductFile_JPanel,
		1, y, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Run mode:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__RunMode_JComboBox = new SimpleJComboBox ( false );
	__RunMode_JComboBox.add ( "" );
	__RunMode_JComboBox.add ( __command._BatchOnly );
	__RunMode_JComboBox.add ( __command._GUIOnly );
	__RunMode_JComboBox.add ( __command._GUIAndBatch );
	__RunMode_JComboBox.addItemListener ( this );
        JGUIUtil.addComponent(main_JPanel, __RunMode_JComboBox,
		1, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel,
		new JLabel ( "Optional - when to process products (default=" + __command._GUIAndBatch + ")." ),
		2, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "View:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__View_JComboBox = new SimpleJComboBox ( false );
	__View_JComboBox.add ( "" );
	__View_JComboBox.add ( __command._False );
	__View_JComboBox.add ( __command._True );
	__View_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __View_JComboBox,
		1, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel,
		new JLabel ( "Optional - display product in window (default=" + __command._True + ")." ),
		2, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Output file:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OutputFile_JTextField = new JTextField (30);
	__OutputFile_JTextField.addKeyListener ( this );
	__OutputFile_JTextField.setEditable ( true );
    // Output file layout fights back with other rows so put in its own panel.
	JPanel OutputFile_JPanel = new JPanel();
	OutputFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(OutputFile_JPanel, __OutputFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );
	__browseOutput_JButton = new SimpleJButton ( "...", this );
	__browseOutput_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(OutputFile_JPanel, __browseOutput_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path.
		__pathOutput_JButton = new SimpleJButton(__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(OutputFile_JPanel, __pathOutput_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	}
	JGUIUtil.addComponent(main_JPanel, OutputFile_JPanel,
		1, y, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Visible start:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __VisibleStart_JTextField = new JTextField (20);
    __VisibleStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __VisibleStart_JTextField,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - start of (initial) visible period (default=all data visible)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Visible end:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __VisibleEnd_JTextField = new JTextField (20);
    __VisibleEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __VisibleEnd_JTextField,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - end of (initial) visible period (default=all data visible)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel("Command status property:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CommandStatusProperty_JTextField = new JTextField ( "", 20 );
    __CommandStatusProperty_JTextField.setToolTipText("Specify the property name for the command exit status, can use ${Property} notation");
    __CommandStatusProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __CommandStatusProperty_JTextField,
        1, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - processor property to set as the command status." ),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea (4,50);
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );

	// Refresh the contents.
	refresh();

	// Panel for buttons.
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
	refresh();	// Sets the __path_JButton status.
   	setResizable ( false );
    super.setVisible( true );
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged ( ItemEvent e ) {
	refresh();
}

/**
Handle key pressed event.
@param event KeyEvent to handle
*/
public void keyPressed ( KeyEvent event ) {
	int code = event.getKeyCode();

	if ( code == KeyEvent.VK_ENTER ) {
		refresh();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
}

/**
Handle key released event.
@param event KeyEvent to handle
*/
public void keyReleased ( KeyEvent event ) {
	refresh();
}

/**
Handle key typed event.
@param event KeyEvent to handle
*/
public void keyTyped ( KeyEvent event ) {
}

/**
Indicate if the user pressed OK (cancel otherwise).
@return true if the edits were committed, false if the user canceled.
*/
public boolean ok () {
	return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh () {
	String TSProductFile = "";
	String RunMode = "";
	String View = "";
	String OutputFile = "";
    String VisibleStart = "";
    String VisibleEnd = "";
    String CommandStatusProperty = "";
	PropList props = null;
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command.
		props = __command.getCommandParameters();
		TSProductFile = props.getValue ( "TSProductFile" );
		RunMode = props.getValue ( "RunMode" );
		View = props.getValue ( "View" );
		OutputFile = props.getValue("OutputFile");
		VisibleStart = props.getValue ( "VisibleStart" );
		VisibleEnd = props.getValue ( "VisibleEnd" );
		CommandStatusProperty = props.getValue ( "CommandStatusProperty" );
		if ( TSProductFile != null ) {
			__TSProductFile_JTextField.setText( TSProductFile );
		}
		// Now select the item in the list.  If not a match, print a warning.
		if ( (RunMode == null) || (RunMode.length() == 0) ) {
			// Select default.
			__RunMode_JComboBox.select ( 0 );
		}
		else {
		    if ( JGUIUtil.isSimpleJComboBoxItem(__RunMode_JComboBox,
				RunMode, JGUIUtil.NONE, null, null ) ) {
				__RunMode_JComboBox.select ( RunMode );
			}
			else {
			    Message.printWarning ( 1,
				"processTSProduct_JDialog.refresh", "Existing "+
				"command references an invalid\n"+
				"run mode flag \"" + RunMode +
				"\".  Select a\ndifferent value or Cancel." );
			}
		}
		if ( (View == null) || (View.length() == 0) ) {
			// Select default.
			__View_JComboBox.select ( 0 );
		}
		else {
		    if ( JGUIUtil.isSimpleJComboBoxItem( __View_JComboBox,
				View, JGUIUtil.NONE, null, null ) ) {
				__View_JComboBox.select ( View );
			}
			else {
			    Message.printWarning ( 1,
				"processTSProduct_JDialog.refresh", "Existing "+
				"command references an invalid\n"+
				"view flag \"" + View +
				"\".  Select a\ndifferent value or Cancel." );
			}
		}
	    if ( OutputFile != null ) {
	         __OutputFile_JTextField.setText( OutputFile );
	    }
        if ( VisibleStart != null ) {
            __VisibleStart_JTextField.setText (VisibleStart);
        }
        if ( VisibleEnd != null ) {
            __VisibleEnd_JTextField.setText (VisibleEnd);
        }
        if ( CommandStatusProperty != null ) {
            __CommandStatusProperty_JTextField.setText (CommandStatusProperty);
        }
	}
	// Regardless, reset the command from the fields.
	TSProductFile = __TSProductFile_JTextField.getText().trim();
	RunMode = __RunMode_JComboBox.getSelected();
	View = __View_JComboBox.getSelected();
	OutputFile = __OutputFile_JTextField.getText().trim();
    VisibleStart = __VisibleStart_JTextField.getText().trim();
    VisibleEnd = __VisibleEnd_JTextField.getText().trim();
    CommandStatusProperty = __CommandStatusProperty_JTextField.getText().trim();
	props = new PropList ( __command.getCommandName() );
	props.add ( "TSProductFile=" + TSProductFile );
	props.add ( "RunMode=" + RunMode );
	props.add ( "View=" + View );
	props.add ( "OutputFile=" + OutputFile );
	props.add ( "VisibleStart=" + VisibleStart );
	props.add ( "VisibleEnd=" + VisibleEnd );
	props.add ( "CommandStatusProperty=" + CommandStatusProperty );
	__command_JTextArea.setText(__command.toString(props).trim() );
	// Check the path and determine what the label on the path button should be.
	if ( __path_JButton != null ) {
		if ( (TSProductFile != null) && !TSProductFile.isEmpty() ) {
			__path_JButton.setEnabled ( true );
			File f = new File ( TSProductFile );
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
	// Check the path and determine what the label on the path button should be.
	if ( __pathOutput_JButton != null ) {
		if ( (OutputFile != null) && !OutputFile.isEmpty() ) {
			__pathOutput_JButton.setEnabled ( true );
			File f = new File ( OutputFile );
			if ( f.isAbsolute() ) {
				__pathOutput_JButton.setText ( __RemoveWorkingDirectory );
				__pathOutput_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
            	__pathOutput_JButton.setText ( __AddWorkingDirectory );
            	__pathOutput_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__pathOutput_JButton.setEnabled(false);
		}
	}
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed and the dialog is closed.
*/
private void response ( boolean ok ) {
	__ok = ok;	// Save to be returned by ok().
	if ( ok ) {
		// Commit the changes.
		commitEdits ();
		if ( __error_wait ) {
			// Not ready to close out.
			return;
		}
	}
	// Now close out.
	setVisible( false );
	dispose();
}

/**
Responds to WindowEvents.
@param event WindowEvent object
*/
public void windowClosing( WindowEvent event ) {
	response ( false );
}

public void windowActivated( WindowEvent evt ) {
}

public void windowClosed( WindowEvent evt ) {
}

public void windowDeactivated( WindowEvent evt ) {
}

public void windowDeiconified( WindowEvent evt ) {
}

public void windowIconified( WindowEvent evt ) {
}

public void windowOpened( WindowEvent evt ) {
}

}