// ExpandTemplateFile_JDialog - editor dialog for ExpandTemplateFile command

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2024 Colorado Department of Natural Resources

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

package rti.tscommandprocessor.commands.template;

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
import java.io.File;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import RTi.Util.GUI.DictionaryJDialog;
import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

@SuppressWarnings("serial")
public class ExpandTemplateFile_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{
private final String __AddWorkingDirectory = "Abs";
private final String __RemoveWorkingDirectory = "ReL";

private SimpleJButton __browseInput_JButton = null;
private SimpleJButton __pathInput_JButton = null;
private SimpleJButton __browseOutput_JButton = null;
private SimpleJButton __pathOutput_JButton = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private JTabbedPane __main_JTabbedPane = null;
private JTextField __InputFile_JTextField = null;
private JTextArea __InputText_JTextArea = null;
private JTextArea __StringProperties_JTextArea = null;
private JTextArea __TableColumnProperties_JTextArea = null;
private JTextField __OutputFile_JTextField = null;
private JTextField __OutputProperty_JTextField = null;
private SimpleJComboBox __UseTables_JComboBox = null;
private SimpleJComboBox __ListInResults_JComboBox = null;
//private SimpleJComboBox __IfNotFound_JComboBox =null;
private JTextArea __command_JTextArea = null;
private String __working_dir = null; // Working directory.
private boolean __error_wait = false;
private boolean __first_time = true;
private ExpandTemplateFile_Command __command = null; // Command to edit.
private boolean __ok = false; // Indicates whether the user has pressed OK.
private JFrame __parent = null;

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public ExpandTemplateFile_JDialog ( JFrame parent, ExpandTemplateFile_Command command ) {
	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event ) {
	String routine = getClass().getSimpleName() + ".actionPerformed";
    Object o = event.getSource();

	if ( o == __browseInput_JButton ) {
		String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
		JFileChooser fc = null;
		if ( last_directory_selected != null ) {
			fc = JFileChooserFactory.createJFileChooser(last_directory_selected );
		}
		else {
		    fc = JFileChooserFactory.createJFileChooser(__working_dir );
		}
		fc.setDialogTitle( "Select Template File");

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
					Message.printWarning ( 1,"ExpandTemplateFile_JDialog", "Error converting file to relative path." );
				}
				JGUIUtil.setLastFileDialogDirectory(directory);
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
        fc.setDialogTitle( "Specify Expanded File to Create");

        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION ) {
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
					Message.printWarning ( 1,"ExpandTemplateFile_JDialog", "Error converting file to relative path." );
				}
                JGUIUtil.setLastFileDialogDirectory(directory);
                refresh();
            }
        }
    }
	else if ( o == __cancel_JButton ) {
		response ( false );
	}
    else if ( event.getActionCommand().equalsIgnoreCase("EditStringProperties") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String StringProperties = __StringProperties_JTextArea.getText().trim();
        String [] notes = {
            "Specify string properties to use in the template expansion."
        };
        String dict = (new DictionaryJDialog ( __parent, true, StringProperties,
            "Edit StringProperties Parameter", notes, "Property Name", "Property Value",10)).response();
        if ( dict != null ) {
            __StringProperties_JTextArea.setText ( dict );
            refresh();
        }
    }
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "ExpandTemplateFile");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else if ( o == __pathInput_JButton ) {
		if ( __pathInput_JButton.getText().equals(__AddWorkingDirectory) ) {
			__InputFile_JTextField.setText (IOUtil.toAbsolutePath(__working_dir,__InputFile_JTextField.getText() ) );
		}
		else if ( __pathInput_JButton.getText().equals(__RemoveWorkingDirectory) ) {
			try {
                __InputFile_JTextField.setText ( IOUtil.toRelativePath ( __working_dir, __InputFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1, routine, "Error converting input file name to relative path." );
			}
		}
		refresh ();
	}
    else if ( o == __pathOutput_JButton ) {
        if ( __pathOutput_JButton.getText().equals(__AddWorkingDirectory) ) {
            __OutputFile_JTextField.setText (IOUtil.toAbsolutePath(__working_dir,__OutputFile_JTextField.getText() ) );
        }
        else if ( __pathOutput_JButton.getText().equals(__RemoveWorkingDirectory) ) {
            try {
                __OutputFile_JTextField.setText ( IOUtil.toRelativePath ( __working_dir, __OutputFile_JTextField.getText() ) );
            }
            catch ( Exception e ) {
                Message.printWarning ( 1, routine, "Error converting output file name to relative path." );
            }
        }
        refresh ();
    }
	else {
	    // Choices.
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
	String InputFile = __InputFile_JTextField.getText().trim();
	String InputText = __InputText_JTextArea.getText().trim();
	String StringProperties = __StringProperties_JTextArea.getText().trim();
	String TableColumnProperties = __TableColumnProperties_JTextArea.getText().trim();
	String OutputFile = __OutputFile_JTextField.getText().trim();
	String OutputProperty = __OutputProperty_JTextField.getText().trim();
	//String IfNotFound = __IfNotFound_JComboBox.getSelected();
	String UseTables = __UseTables_JComboBox.getSelected();
	String ListInResults = __ListInResults_JComboBox.getSelected();
	__error_wait = false;
	if ( InputFile.length() > 0 ) {
		props.set ( "InputFile", InputFile );
	}
    if ( InputText.length() > 0 ) {
        props.set ( "InputText", InputText );
    }
    if ( StringProperties.length() > 0 ) {
        props.set ( "StringProperties", StringProperties );
    }
    if ( TableColumnProperties.length() > 0 ) {
        props.set ( "TableColumnProperties", TableColumnProperties );
    }
    if ( OutputFile.length() > 0 ) {
        props.set ( "OutputFile", OutputFile );
    }
    if ( OutputProperty.length() > 0 ) {
        props.set ( "OutputProperty", OutputProperty );
    }
    if ( UseTables.length() > 0 ) {
        props.set ( "UseTables", UseTables );
    }
    if ( ListInResults.length() > 0 ) {
        props.set ( "ListInResults", ListInResults );
    }
	//if ( IfNotFound.length() > 0 ) {
	//	props.set ( "IfNotFound", IfNotFound );
	//}
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
	String InputFile = __InputFile_JTextField.getText().trim();
    String InputText = __InputText_JTextArea.getText().replace('\n', ' ').replace('\t', ' ').trim();
	String StringProperties = __StringProperties_JTextArea.getText().trim();
	String TableColumnProperties = __TableColumnProperties_JTextArea.getText().trim();
    String OutputFile = __OutputFile_JTextField.getText().trim();
    String OutputProperty = __OutputProperty_JTextField.getText().trim();
    String UseTables = __UseTables_JComboBox.getSelected();
    String ListInResults = __ListInResults_JComboBox.getSelected();
	//String IfNotFound = __IfNotFound_JComboBox.getSelected();
	__command.setCommandParameter ( "InputFile", InputFile );
	__command.setCommandParameter ( "InputText", InputText );
	__command.setCommandParameter ( "StringProperties", StringProperties );
	__command.setCommandParameter ( "TableColumnProperties", TableColumnProperties );
	__command.setCommandParameter ( "OutputFile", OutputFile );
	__command.setCommandParameter ( "OutputProperty", OutputProperty );
	__command.setCommandParameter ( "UseTables", UseTables );
	__command.setCommandParameter ( "ListInResults", ListInResults );
	//__command.setCommandParameter ( "IfNotFound", IfNotFound );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, ExpandTemplateFile_Command command ) {
	__parent = parent;
	__command = command;
	CommandProcessor processor =__command.getCommandProcessor();

	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( processor, __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel.

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"This command expands a template (file or text) into a fully-expanded file and/or processor property." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Template functionality is implemented using the FreeMarker package (freemarker.org)." ),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Template markup language can be applied to command files to implement conditional logic, loops, etc." ),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The template data model receives properties set with SetProperty() and one-column tables are passed " +
        "as a list using the table identifier as the list name." ),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    if ( __working_dir != null ) {
    	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"It is recommended that file names be relative to the working directory, which is:"),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"    " + __working_dir),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    __main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Panel for input template.
    int yIn = -1;
    JPanel in_JPanel = new JPanel();
    in_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Input Template", in_JPanel );

    JGUIUtil.addComponent(in_JPanel, new JLabel (
        "The template to be expanded can be specified with a file or text."),
        0, ++yIn, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(in_JPanel, new JLabel (
        "Use a file when the template text conflicts with normal command syntax (quotes, etc.)."),
        0, ++yIn, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(in_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++yIn, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(in_JPanel, new JLabel ("Template file:" ),
		0, ++yIn, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputFile_JTextField = new JTextField ( 50 );
	__InputFile_JTextField.setToolTipText("Specify the path to the input file or use ${Property} notation");
	__InputFile_JTextField.addKeyListener ( this );
    // Input file layout fights back with other rows so put in its own panel.
	JPanel InputFile_JPanel = new JPanel();
	InputFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(InputFile_JPanel, __InputFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browseInput_JButton = new SimpleJButton ( "...", this );
	__browseInput_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(InputFile_JPanel, __browseInput_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path.
		__pathInput_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(InputFile_JPanel, __pathInput_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(in_JPanel, InputFile_JPanel,
		1, yIn, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(in_JPanel, new JLabel ("OR template text:"),
        0, ++yIn, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputText_JTextArea = new JTextArea (9,50);
    __InputText_JTextArea.setLineWrap ( true );
    __InputText_JTextArea.setWrapStyleWord ( true );
    __InputText_JTextArea.addKeyListener(this);
    JGUIUtil.addComponent(in_JPanel, new JScrollPane(__InputText_JTextArea),
        1, yIn, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Panel for input properties.
    int yProp = -1;
    JPanel prop_JPanel = new JPanel();
    prop_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Input Properties", prop_JPanel );

    JGUIUtil.addComponent(prop_JPanel, new JLabel (
        "TSTool processor properties accessible with ${Property} are automatically passed to the template processor."),
        0, ++yProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel (
        "The following allows additional string properties to be defined only for this command.  Use the following syntax:"),
        0, ++yProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel (
        "   Property1Name:Property1Value,Property2Name:Property2Value"),
        0, ++yProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++yProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(prop_JPanel, new JLabel ("String properties:"),
        0, ++yProp, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __StringProperties_JTextArea = new JTextArea (6,35);
    __StringProperties_JTextArea.setLineWrap ( true );
    __StringProperties_JTextArea.setWrapStyleWord ( true );
    __StringProperties_JTextArea.setToolTipText("Property1Name:Property1Value,Property2Name:Property2Value");
    __StringProperties_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(prop_JPanel, new JScrollPane(__StringProperties_JTextArea),
        1, yProp, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel ("Optional - additional properties for template."),
        3, yProp, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(prop_JPanel, new SimpleJButton ("Edit","EditStringProperties",this),
        3, ++yProp, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    // Panel for input properties (tables).
    int yTableProp = -1;
    JPanel tableProp_JPanel = new JPanel();
    tableProp_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Input Properties (Table)", tableProp_JPanel );

    JGUIUtil.addComponent(tableProp_JPanel, new JLabel (
        "One column tables can be passed to the template processor to use as lists."),
        0, ++yTableProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(tableProp_JPanel, new JLabel (
        "These one-column tables can be created with TSTool commands prior to this command, or created dynamically using information below."),
        0, ++yTableProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(tableProp_JPanel, new JLabel (
        "Table column properties specified below should use the following syntax.  ${Property} will be replaced with the matching TSTool property:"),
        0, ++yTableProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(tableProp_JPanel, new JLabel (
        "    Table1Name,Column1Name,ListProperty1Name;Table2Name,Column2Name,ListProperty2Name"),
        0, ++yTableProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(tableProp_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++yTableProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(tableProp_JPanel, new JLabel ( "Use tables as input?:" ),
        0, ++yTableProp, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __UseTables_JComboBox = new SimpleJComboBox ( false );
    __UseTables_JComboBox.add ( "" );
    __UseTables_JComboBox.add ( __command._False );
    __UseTables_JComboBox.add ( __command._True );
    __UseTables_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(tableProp_JPanel, __UseTables_JComboBox,
        1, yTableProp, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(tableProp_JPanel,
        new JLabel ( "Optional - use 1-column tables as input lists (default=" + __command._True + ")." ),
        2, yTableProp, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(tableProp_JPanel, new JLabel ("Table column properties:"),
        0, ++yTableProp, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableColumnProperties_JTextArea = new JTextArea (9,50);
    __TableColumnProperties_JTextArea.setLineWrap ( true );
    __TableColumnProperties_JTextArea.setWrapStyleWord ( true );
    __TableColumnProperties_JTextArea.addKeyListener(this);
    JGUIUtil.addComponent(tableProp_JPanel, new JScrollPane(__TableColumnProperties_JTextArea),
        1, yTableProp, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Panel for output.
    int yOut = -1;
    JPanel out_JPanel = new JPanel();
    out_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Expanded Output", out_JPanel );

    JGUIUtil.addComponent(out_JPanel, new JLabel (
        "The expanded output can be saved to a file and/or set to a processor property (access later with ${Property})."),
        0, ++yOut, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(out_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++yOut, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(out_JPanel, new JLabel ("Expanded file:" ),
        0, ++yOut, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputFile_JTextField = new JTextField ( 50 );
    __OutputFile_JTextField.setToolTipText("Specify the path to the output file or use ${Property} notation");
    __OutputFile_JTextField.addKeyListener ( this );
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
		__pathOutput_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(OutputFile_JPanel, __pathOutput_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	}
	JGUIUtil.addComponent(out_JPanel, OutputFile_JPanel,
		1, yOut, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(out_JPanel, new JLabel ( "Expanded property:" ),
        0, ++yOut, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputProperty_JTextField = new JTextField ( 20 );
    __OutputProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(out_JPanel, __OutputProperty_JTextField,
        1, yOut, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(out_JPanel,
        new JLabel ( "Optional - output string property (default=no output property)." ),
        2, yOut, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(out_JPanel, new JLabel ( "List output in results?:" ),
        0, ++yOut, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ListInResults_JComboBox = new SimpleJComboBox ( false );
    __ListInResults_JComboBox.add ( "" );
    __ListInResults_JComboBox.add ( __command._False );
    __ListInResults_JComboBox.add ( __command._True );
    __ListInResults_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(out_JPanel, __ListInResults_JComboBox,
        1, yOut, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(out_JPanel,
        new JLabel ( "Optional - list expanded file in results (default=" + __command._True + ")." ),
        2, yOut, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    /*
   JGUIUtil.addComponent(main_JPanel, new JLabel ( "If not found?:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__IfNotFound_JComboBox = new SimpleJComboBox ( false );
	__IfNotFound_JComboBox.addItem ( "" );	// Default
	__IfNotFound_JComboBox.addItem ( __command._Ignore );
	__IfNotFound_JComboBox.addItem ( __command._Warn );
	__IfNotFound_JComboBox.addItem ( __command._Fail );
	__IfNotFound_JComboBox.select ( 0 );
	__IfNotFound_JComboBox.addActionListener ( this );
   JGUIUtil.addComponent(main_JPanel, __IfNotFound_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional - action if file not found (default=" + __command._Warn + ")"),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
		*/

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea ( 4, 60 );
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.addKeyListener ( this );
	__command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Panel for buttons.
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel,
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	button_JPanel.add ( __ok_JButton = new SimpleJButton("OK", this) );
	__ok_JButton.setToolTipText("Save changes to command");
	button_JPanel.add(__cancel_JButton = new SimpleJButton("Cancel", this));
	__cancel_JButton.setToolTipText("Cancel without saving changes to command");
	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");

	setTitle ( "Edit " + __command.getCommandName() + " command" );

	// Refresh the contents.
    refresh ();

    pack();
    JGUIUtil.center( this );
	// Dialogs do not need to be resizable.
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
Respond to KeyEvents.
@param event KeyEvent to handle
*/
public void keyPressed ( KeyEvent event ) {
	int code = event.getKeyCode();

	if ( code == KeyEvent.VK_ENTER ) {
		refresh ();
	}
}

/**
Respond to key release event.
@param event KeyEvent to handle
*/
public void keyReleased ( KeyEvent event ) {
	refresh();
}

/**
Respond to key typed event.
@param event KeyEvent to handle
*/
public void keyTyped ( KeyEvent event ) {
	// Ignore since handled by press and release events.
}

/**
Indicate if the user pressed OK (cancel otherwise).
@return true if OK was pressed, false otherwise
*/
public boolean ok () {
	return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh () {
	String routine = getClass().getSimpleName() + ".refresh";
	String InputFile = "";
	String InputText = "";
	String StringProperties = "";
	String TableColumnProperties = "";
	String OutputFile = "";
	String OutputProperty = "";
	String UseTables = "";
	String ListInResults = "";
	//String IfNotFound = "";
    PropList parameters = null;
	if ( __first_time ) {
		__first_time = false;
        parameters = __command.getCommandParameters();
		InputFile = parameters.getValue ( "InputFile" );
		InputText = parameters.getValue ( "InputText" );
		StringProperties = parameters.getValue ( "StringProperties" );
		TableColumnProperties = parameters.getValue ( "TableColumnProperties" );
		OutputFile = parameters.getValue ( "OutputFile" );
		OutputProperty = parameters.getValue ( "OutputProperty" );
		UseTables = parameters.getValue ( "UseTables" );
		ListInResults = parameters.getValue ( "ListInResults" );
		//IfNotFound = parameters.getValue ( "IfNotFound" );
		if ( InputFile != null ) {
			__InputFile_JTextField.setText ( InputFile );
		}
        if ( InputText != null ) {
            __InputText_JTextArea.setText ( InputText );
        }
        if ( StringProperties != null ) {
            __StringProperties_JTextArea.setText ( StringProperties );
        }
        if ( TableColumnProperties != null ) {
            __TableColumnProperties_JTextArea.setText ( TableColumnProperties );
        }
        if ( OutputFile != null ) {
            __OutputFile_JTextField.setText ( OutputFile );
        }
        if ( OutputProperty != null ) {
            __OutputProperty_JTextField.setText ( OutputProperty );
        }
        if ( (UseTables == null) || (UseTables.length() == 0) ) {
            // Select default.
            __UseTables_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __UseTables_JComboBox,
                UseTables, JGUIUtil.NONE, null, null ) ) {
                __UseTables_JComboBox.select ( UseTables );
            }
            else {
                Message.printWarning ( 1, routine, "Existing "+
                "command references an invalid\n"+
                "UseTables \"" + UseTables +
                "\" parameter.  Select a\ndifferent value or Cancel." );
            }
        }
        if ( (ListInResults == null) || (ListInResults.length() == 0) ) {
            // Select default.
            __ListInResults_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __ListInResults_JComboBox,
                ListInResults, JGUIUtil.NONE, null, null ) ) {
                __ListInResults_JComboBox.select ( ListInResults );
            }
            else {
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                "ListInResults \"" + ListInResults + "\" parameter.  Select a\ndifferent value or Cancel." );
            }
        }
        /*
		if ( JGUIUtil.isSimpleJComboBoxItem(__IfNotFound_JComboBox, IfNotFound,JGUIUtil.NONE, null, null ) ) {
			__IfNotFound_JComboBox.select ( IfNotFound );
		}
		else {
            if ( (IfNotFound == null) ||	IfNotFound.equals("") ) {
				// New command...select the default...
				__IfNotFound_JComboBox.select ( 0 );
			}
			else {	// Bad user command...
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"IfNotFound parameter \"" +	IfNotFound +
				"\".  Select a\n value or Cancel." );
			}
		}
		*/
	}
	// Regardless, reset the command from the fields.  This is only  visible
	// information that has not been committed in the command.
	InputFile = __InputFile_JTextField.getText().trim();
	InputText = __InputText_JTextArea.getText().trim();
	StringProperties = __StringProperties_JTextArea.getText().trim();
	TableColumnProperties = __TableColumnProperties_JTextArea.getText().trim();
	OutputFile = __OutputFile_JTextField.getText().trim();
	OutputProperty = __OutputProperty_JTextField.getText().trim();
	UseTables = __UseTables_JComboBox.getSelected();
	ListInResults = __ListInResults_JComboBox.getSelected();
	//IfNotFound = __IfNotFound_JComboBox.getSelected();
	PropList props = new PropList ( __command.getCommandName() );
	props.add ( "InputFile=" + InputFile );
	props.add ( "InputText=" + InputText );
	props.add ( "StringProperties=" + StringProperties );
	props.add ( "TableColumnProperties=" + TableColumnProperties );
	props.add ( "OutputFile=" + OutputFile );
	props.add ( "OutputProperty=" + OutputProperty );
	props.add ( "UseTables=" + UseTables );
	props.add ( "ListInResults=" + ListInResults );
	//props.add ( "IfNotFound=" + IfNotFound );
	__command_JTextArea.setText( __command.toString(props).trim() );
	// Check the path and determine what the label on the path button should be...
	if ( __pathInput_JButton != null ) {
		// Check the path and determine what the label on the path button should be...
		if ( __pathInput_JButton != null ) {
			if ( (InputFile != null) && !InputFile.isEmpty() ) {
				__pathInput_JButton.setEnabled ( true );
				File f = new File ( InputFile );
				if ( f.isAbsolute() ) {
					__pathInput_JButton.setText ( __RemoveWorkingDirectory );
					__pathInput_JButton.setToolTipText("Change path to relative to command file");
				}
				else {
			    	__pathInput_JButton.setText ( __AddWorkingDirectory );
			    	__pathInput_JButton.setToolTipText("Change path to absolute");
				}
			}
			else {
				__pathInput_JButton.setEnabled(false);
			}
		}
	}
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
@param ok if false, then the edit is canceled.
If true, the edit is committed and the dialog is closed.
*/
public void response ( boolean ok ) {
	__ok = ok;
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
Respond to window closing event.
@param event WindowEvent object
*/
public void windowClosing( WindowEvent event ) {
	response ( false );
}

/**
Respond to window activated event.
@param event WindowEvent object
*/
public void windowActivated( WindowEvent evt ) {
}

/**
Respond to window closed event.
@param event WindowEvent object
*/
public void windowClosed( WindowEvent evt ) {
}

/**
Respond to window deactivated event.
@param event WindowEvent object
*/
public void windowDeactivated( WindowEvent evt ) {
}

/**
Respond to window deiconified event.
@param event WindowEvent object
*/
public void windowDeiconified( WindowEvent evt ) {
}

/**
Respond to window iconified event.
@param event WindowEvent object
*/
public void windowIconified( WindowEvent evt ) {
}

/**
Respond to window opened event.
@param event WindowEvent object
*/
public void windowOpened( WindowEvent evt ) {
}

}
