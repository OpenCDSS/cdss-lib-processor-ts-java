// WriteTableToMarkdown_JDialog - editor dialog for WriteTableToMarkdown command

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

package rti.tscommandprocessor.commands.table;

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
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import RTi.Util.GUI.DictionaryJDialog;
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

@SuppressWarnings("serial")
public class WriteTableToMarkdown_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{

private final String __AddWorkingDirectory = "Abs";
private final String __RemoveWorkingDirectory = "Rel";

private JTabbedPane __main_JTabbedPane = null;
private SimpleJButton __browseOutput_JButton = null;
private SimpleJButton __browsePrepend_JButton = null;
private SimpleJButton __browseAppend_JButton = null;
//private SimpleJButton __browseSchema_JButton = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private SimpleJButton __pathOutput_JButton = null;
private SimpleJButton __pathPrepend_JButton = null;
private SimpleJButton __pathAppend_JButton = null;
//private SimpleJButton __pathSchema_JButton = null;
private WriteTableToMarkdown_Command __command = null;
private JTextArea __command_JTextArea=null;
// Table.
private SimpleJComboBox __TableID_JComboBox = null;
private JTextField __IncludeColumns_JTextField = null;
private JTextField __ExcludeColumns_JTextField = null;
//private SimpleJComboBox __WriteColumnNames_JComboBox = null;
//private JTextField __Delimiter_JTextField = null;
//private SimpleJComboBox __AlwaysQuoteDateTimes_JComboBox = null;
//private SimpleJComboBox __AlwaysQuoteStrings_JComboBox = null;
// Formatting.
private JTextArea __LinkColumns_JTextArea = null;
private JTextField __NewlineReplacement_JTextField = null;
private JTextField __NaNValue_JTextField = null;
// Output inserts.
private JTextArea __PrependText_JTextArea = null;
private JTextField __PrependFile_JTextField = null;
private JTextArea __AppendText_JTextArea = null;
private JTextField __AppendFile_JTextField = null;
// Output
private JTextField __OutputFile_JTextField = null;
private SimpleJComboBox __Append_JComboBox = null;
private SimpleJComboBox __WriteHeaderComments_JComboBox = null;
// Output Schema:
// - TODO smalers 2025-01-04 schema files are not really useful for Markdown, better for CSV, remove if not needed later
//private JTextField __OutputSchemaFile_JTextField = null;
//private SimpleJComboBox __OutputSchemaFormat_JComboBox = null;

private String __working_dir = null; // Working directory.
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether the user has pressed OK.
private JFrame __parent = null;

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit
*/
public WriteTableToMarkdown_JDialog ( JFrame parent, WriteTableToMarkdown_Command command, List<String> tableIDChoices ) {
	super(parent, true);
	initialize ( parent, command, tableIDChoices );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event ) {
	Object o = event.getSource();

	if ( o == __browseAppend_JButton ) {
        String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
        JFileChooser fc = null;
        if ( last_directory_selected != null ) {
            fc = JFileChooserFactory.createJFileChooser( last_directory_selected );
        }
        else {
            fc = JFileChooserFactory.createJFileChooser( __working_dir );
        }
        fc.setDialogTitle("Select Append File");
        SimpleFileFilter sff = new SimpleFileFilter("md", "Append File (markdown)");
        fc.addChoosableFileFilter(sff);
        sff = new SimpleFileFilter("txt", "Append File (text)");
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
					__AppendFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1,"WriteTableToMarkdown_JDialog", "Error converting file to relative path." );
				}
                JGUIUtil.setLastFileDialogDirectory(directory );
                refresh();
            }
        }
    }
	else if ( o == __browseOutput_JButton ) {
		String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
		JFileChooser fc = null;
		if ( last_directory_selected != null ) {
			fc = JFileChooserFactory.createJFileChooser( last_directory_selected );
		}
		else {
		    fc = JFileChooserFactory.createJFileChooser(__working_dir );
		}
		fc.setDialogTitle("Select Markdown File to Write");
	    SimpleFileFilter sff_md = new SimpleFileFilter("md", "Markdown File");
	    fc.addChoosableFileFilter(sff_md);

		if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			String directory = fc.getSelectedFile().getParent();
			String path = fc.getSelectedFile().getPath();

			if (path != null) {
				if ( fc.getFileFilter() == sff_md ) {
					// Enforce extension.
					path = IOUtil.enforceFileExtension(path, "md");
				}
				// Convert path to relative path by default.
				try {
					__OutputFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1,"WriteTableToMarkdown_JDialog", "Error converting file to relative path." );
				}
				JGUIUtil.setLastFileDialogDirectory( directory);
				refresh();
			}
		}
	}
	else if ( o == __browsePrepend_JButton ) {
        String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
        JFileChooser fc = null;
        if ( last_directory_selected != null ) {
            fc = JFileChooserFactory.createJFileChooser( last_directory_selected );
        }
        else {
            fc = JFileChooserFactory.createJFileChooser( __working_dir );
        }
        fc.setDialogTitle("Select Prepend File");
        SimpleFileFilter sff = new SimpleFileFilter("md", "Prepend File (markdown)");
        fc.addChoosableFileFilter(sff);
        sff = new SimpleFileFilter("txt", "Prepend File (text)");
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
					__PrependFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1,"WriteTableToMarkdown_JDialog", "Error converting file to relative path." );
				}
                JGUIUtil.setLastFileDialogDirectory(directory );
                refresh();
            }
        }
    }
	/*
	else if ( o == __browseSchema_JButton ) {
		String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
		JFileChooser fc = null;
		if ( last_directory_selected != null ) {
			fc = JFileChooserFactory.createJFileChooser( last_directory_selected );
		}
		else {
		    fc = JFileChooserFactory.createJFileChooser(__working_dir );
		}
		fc.setDialogTitle("Select Schema File to Write");
	    SimpleFileFilter sff_csv = new SimpleFileFilter("json", "Table Schema File (JSON)");
	    fc.addChoosableFileFilter(sff_csv);

		if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			String directory = fc.getSelectedFile().getParent();
			String path = fc.getSelectedFile().getPath();

			if (path != null) {
				if ( fc.getFileFilter() == sff_csv ) {
					// Enforce extension.
					path = IOUtil.enforceFileExtension(path, "json");
				}

				// Convert path to relative path by default.
				try {
					__OutputSchemaFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1,"WriteTableToMarkdown_JDialog", "Error converting file to relative path." );
				}
				JGUIUtil.setLastFileDialogDirectory( directory);
				refresh();
			}
		}
	}
	*/
	else if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "WriteTableToMarkdown");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else if ( o == __pathOutput_JButton ) {
		if ( __pathOutput_JButton.getText().equals(__AddWorkingDirectory) ) {
			__OutputFile_JTextField.setText (
			IOUtil.toAbsolutePath(__working_dir, __OutputFile_JTextField.getText() ) );
		}
		else if ( __pathOutput_JButton.getText().equals(__RemoveWorkingDirectory) ) {
			try {
			    __OutputFile_JTextField.setText (
				IOUtil.toRelativePath ( __working_dir, __OutputFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1,
				"WriteTableToMarkdown_JDialog", "Error converting file to relative path." );
			}
		}
		refresh ();
	}
	/*
	else if ( o == __pathSchema_JButton ) {
		if ( __pathSchema_JButton.getText().equals(__AddWorkingDirectory) ) {
			__OutputSchemaFile_JTextField.setText (
			IOUtil.toAbsolutePath(__working_dir, __OutputSchemaFile_JTextField.getText() ) );
		}
		else if ( __pathSchema_JButton.getText().equals(__RemoveWorkingDirectory) ) {
			try {
				__OutputSchemaFile_JTextField.setText (
				IOUtil.toRelativePath ( __working_dir, __OutputSchemaFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1,
				"WriteTableToMarkdown_JDialog", "Error converting schema file to relative path." );
			}
		}
		refresh ();
	}
	*/
    else if ( event.getActionCommand().equalsIgnoreCase("EditLinkColumns") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String LinkColumns = __LinkColumns_JTextArea.getText().trim();
        String [] notes = {
            "Specify link columns and corresponding column with the link text.",
            "The link text will be visible in the rendered Markdown file.",
        };
        String dict = (new DictionaryJDialog ( __parent, true, LinkColumns,
            "Edit LinkColumns Parameter", notes, "Column Name for Link", "Column Name for Link Text",10)).response();
        if ( dict != null ) {
            __LinkColumns_JTextArea.setText ( dict );
            refresh();
        }
    }
	else {
		// Choices.
		refresh ();
	}
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag to true.
This should be called before response() is allowed to complete.
*/
private void checkInput () {
	// Put together a list of parameters to check.
	PropList parameters = new PropList ( "" );
	// Table.
    String TableID = __TableID_JComboBox.getSelected();
	String IncludeColumns = __IncludeColumns_JTextField.getText().trim();
	String ExcludeColumns = __ExcludeColumns_JTextField.getText().trim();
    //String WriteColumnNames = __WriteColumnNames_JComboBox.getSelected();
    //String Delimiter = __Delimiter_JTextField.getText().trim();
    //String AlwaysQuoteDateTimes = __AlwaysQuoteDateTimes_JComboBox.getSelected();
    //String AlwaysQuoteStrings = __AlwaysQuoteStrings_JComboBox.getSelected();
    // Formatting.
	String LinkColumns = __LinkColumns_JTextArea.getText().trim().replace("\n"," ");
    String NewlineReplacement = __NewlineReplacement_JTextField.getText().trim();
    String NaNValue = __NaNValue_JTextField.getText().trim();
    // Inserts.
    String PrependText = __PrependText_JTextArea.getText().trim();
    String PrependFile = __PrependFile_JTextField.getText().trim();
    String AppendText = __AppendText_JTextArea.getText().trim();
    String AppendFile = __AppendFile_JTextField.getText().trim();
    // Output File.
	String OutputFile = __OutputFile_JTextField.getText().trim();
	String Append = __Append_JComboBox.getSelected();
    String WriteHeaderComments = __WriteHeaderComments_JComboBox.getSelected();
    // Output schema.
    //String OutputSchemaFile = __OutputSchemaFile_JTextField.getText().trim();
    //String OutputSchemaFormat = __OutputSchemaFormat_JComboBox.getSelected();

	__error_wait = false;

	// Table.
    if ( (TableID != null) && TableID.length() > 0 ) {
        parameters.set ( "TableID", TableID );
    }
    if ( IncludeColumns.length() > 0 ) {
        parameters.set ( "IncludeColumns", IncludeColumns );
    }
    if ( ExcludeColumns.length() > 0 ) {
        parameters.set ( "ExcludeColumns", ExcludeColumns );
    }
    /*
    if ( (WriteColumnNames != null) && (WriteColumnNames.length() > 0) ) {
        parameters.set ( "WriteColumnNames", WriteColumnNames );
    }
    if ( (Delimiter != null) && (Delimiter.length() > 0) ) {
        parameters.set ( "Delimiter", Delimiter );
    }
    if ( (AlwaysQuoteDateTimes != null) && (AlwaysQuoteDateTimes.length() > 0) ) {
        parameters.set ( "AlwaysQuoteDateTimes", AlwaysQuoteDateTimes );
    }
    if ( (AlwaysQuoteStrings != null) && (AlwaysQuoteStrings.length() > 0) ) {
        parameters.set ( "AlwaysQuoteStrings", AlwaysQuoteStrings );
    }
    */
    // Formatting.
    if ( LinkColumns.length() > 0 ) {
        parameters.set ( "LinkColumns", LinkColumns );
    }
    if ( (NewlineReplacement != null) && (NewlineReplacement.length() > 0) ) {
        parameters.set ( "NewlineReplacement", NewlineReplacement );
    }
    if ( (NaNValue != null) && (NaNValue.length() > 0) ) {
        parameters.set ( "NaNValue", NaNValue );
    }
    // Inserts
    if ( PrependText.length() > 0 ) {
        parameters.set ( "PrependText", PrependText );
    }
    if ( PrependFile.length() > 0 ) {
        parameters.set ( "PrependFile", PrependFile );
    }
    if ( AppendText.length() > 0 ) {
        parameters.set ( "AppendText", AppendText );
    }
    if ( AppendFile.length() > 0 ) {
        parameters.set ( "AppendFile", AppendFile );
    }
    // Output FIle.
	if ( OutputFile.length() > 0 ) {
		parameters.set ( "OutputFile", OutputFile );
	}
	if ( Append.length() > 0 ) {
		parameters.set ( "Append", Append );
	}
    if ( (WriteHeaderComments != null) && (WriteHeaderComments.length() > 0) ) {
        parameters.set ( "WriteHeaderComments", WriteHeaderComments );
    }
	// Output schema.
    /*
    if ( (OutputSchemaFile != null) && (OutputSchemaFile.length() > 0) ) {
        parameters.set ( "OutputSchemaFile", OutputSchemaFile );
    }
    if ( (OutputSchemaFormat != null) && (OutputSchemaFormat.length() > 0) ) {
        parameters.set ( "OutputSchemaFormat", OutputSchemaFormat );
    }
    */
	try {
	    // This will warn the user.
		__command.checkCommandParameters ( parameters, null, 1 );
	}
	catch ( Exception e ) {
		// The warning would have been printed in the check code.
		Message.printWarning(2,"",e);
		__error_wait = true;
	}
}

/**
Commit the edits to the command.
In this case the command parameters have already been checked and no errors were detected.
*/
private void commitEdits () {
	// Table.
    String TableID = __TableID_JComboBox.getSelected();
	String IncludeColumns = __IncludeColumns_JTextField.getText().trim();
	String ExcludeColumns = __ExcludeColumns_JTextField.getText().trim();
	// Formatting.
	String LinkColumns = __LinkColumns_JTextArea.getText().trim().replace("\n"," ");
	String NewlineReplacement = __NewlineReplacement_JTextField.getText().trim();
	String NaNValue = __NaNValue_JTextField.getText().trim();
	// Inserts:
	// - don't trim text because trailing newlines may be present
	String PrependFile = __PrependFile_JTextField.getText().trim();
    String PrependText = __PrependText_JTextArea.getText().replace("\n", "\\n");
	String AppendFile = __AppendFile_JTextField.getText().trim();
    String AppendText = __AppendText_JTextArea.getText().replace("\n", "\\n");
    // Output File.
	String OutputFile = __OutputFile_JTextField.getText().trim();
	String Append = __Append_JComboBox.getSelected();
	String WriteHeaderComments = __WriteHeaderComments_JComboBox.getSelected();
	// Schema File.
    //String OutputSchemaFile = __OutputSchemaFile_JTextField.getText().trim();
    //String OutputSchemaFormat = __OutputSchemaFormat_JComboBox.getSelected();
	/*
    String WriteColumnNames = __WriteColumnNames_JComboBox.getSelected();
    String Delimiter = __Delimiter_JTextField.getText().trim();
	String AlwaysQuoteDateTimes = __AlwaysQuoteDateTimes_JComboBox.getSelected();
	String AlwaysQuoteStrings = __AlwaysQuoteStrings_JComboBox.getSelected();
	*/
	// Table.
    __command.setCommandParameter ( "TableID", TableID );
    __command.setCommandParameter ( "IncludeColumns", IncludeColumns );
    __command.setCommandParameter ( "ExcludeColumns", ExcludeColumns );
    /*
	__command.setCommandParameter ( "WriteColumnNames", WriteColumnNames );
	__command.setCommandParameter ( "Delimiter", Delimiter );
	__command.setCommandParameter ( "AlwaysQuoteDateTimes", AlwaysQuoteDateTimes );
	__command.setCommandParameter ( "AlwaysQuoteStrings", AlwaysQuoteStrings );
	*/
	// Formatting.
	__command.setCommandParameter ( "LinkColumns", LinkColumns );
	__command.setCommandParameter ( "NewlineReplacement", NewlineReplacement );
	__command.setCommandParameter ( "NaNValue", NaNValue );
	// Inserts.
	__command.setCommandParameter ( "PrependText", PrependText );
	__command.setCommandParameter ( "PrependFile", PrependFile );
	__command.setCommandParameter ( "AppendText", AppendText );
	__command.setCommandParameter ( "AppendFile", AppendFile );
    // Output File.
	__command.setCommandParameter ( "OutputFile", OutputFile );
	__command.setCommandParameter ( "Append", Append );
	__command.setCommandParameter ( "WriteHeaderComments", WriteHeaderComments );
	// Schema File.
	//__command.setCommandParameter ( "OutputSchemaFile", OutputSchemaFile );
	//__command.setCommandParameter ( "OutputSchemaFormat", OutputSchemaFormat );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, WriteTableToMarkdown_Command command, List<String> tableIDChoices ) {
	__parent = parent;
	__command = command;
	CommandProcessor processor = __command.getCommandProcessor();
	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( processor, __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);
    Insets insetsNONE = new Insets(1,1,1,1);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Write a table to a Markdown format file, which can be specified using a full or " +
		"relative path (relative to the working directory)."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	 if ( __working_dir != null ) {
     	JGUIUtil.addComponent(main_JPanel, new JLabel ( "The working directory is: " ),
     		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
     	JGUIUtil.addComponent(main_JPanel, new JLabel ( "    " + __working_dir ),
     		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	 }
  	JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

  	// Tabbed pane.
    __main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Panel for table parameters.
    int yTable = -1;
    JPanel table_JPanel = new JPanel();
    table_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Table to Write", table_JPanel );

    JGUIUtil.addComponent(table_JPanel, new JLabel (
        "Specify the table and columns to write."),
        0, ++yTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(table_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Table to write:" ),
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableID_JComboBox = new SimpleJComboBox ( true ); // Allow edits for ${Property}.
    __TableID_JComboBox.setToolTipText("Specify the table ID for statistic output or use ${Property} notation");
    __TableID_JComboBox.setData(tableIDChoices);
    __TableID_JComboBox.addItemListener ( this );
    __TableID_JComboBox.getJTextComponent().addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __TableID_JComboBox,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Required - table identifier."),
    	3, yTable, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(table_JPanel, new JLabel ("Table columns to include:"),
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __IncludeColumns_JTextField = new JTextField (30);
    __IncludeColumns_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(table_JPanel, __IncludeColumns_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Optional - columns to include, separated by commas (default=write all)."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(table_JPanel, new JLabel ("Table columns to exclude:"),
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ExcludeColumns_JTextField = new JTextField (30);
    __ExcludeColumns_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(table_JPanel, __ExcludeColumns_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Optional - columns to exclude, separated by commas (default=write all)."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    // Panel for output formatting.
    int yFormat = -1;
    JPanel format_JPanel = new JPanel();
    format_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Formatting", format_JPanel );

    JGUIUtil.addComponent(format_JPanel, new JLabel (
        "These parameters specify how to format the markdown in the table."),
        0, ++yFormat, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(format_JPanel, new JLabel (
        "Formatting is currently very basic.  Strings that are URLs will be output as links."),
        0, ++yFormat, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(format_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yFormat, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(format_JPanel, new JLabel ("Link columns:"),
        0, ++yFormat, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __LinkColumns_JTextArea = new JTextArea (6,35);
    __LinkColumns_JTextArea.setLineWrap ( true );
    __LinkColumns_JTextArea.setWrapStyleWord ( true );
    __LinkColumns_JTextArea.setToolTipText("LinkColumn1:LinkTextColumn1,LinkColumn2:LinkTextColumn2");
    __LinkColumns_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(format_JPanel, new JScrollPane(__LinkColumns_JTextArea),
        1, yFormat, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(format_JPanel, new JLabel ("Optional - link properties (default=link is used for link text)."),
        3, yFormat, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(format_JPanel, new SimpleJButton ("Edit","EditLinkColumns",this),
        3, ++yFormat, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(format_JPanel, new JLabel ("Newline replacement:"),
        0, ++yFormat, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NewlineReplacement_JTextField = new JTextField (10);
    __NewlineReplacement_JTextField.setToolTipText("Replacement for newline character (use \\t for tab, \\s for space, and <br> for HTML break).");
    __NewlineReplacement_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(format_JPanel, __NewlineReplacement_JTextField,
        1, yFormat, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(format_JPanel, new JLabel (
        "Optional - replacement for newline character (default=single space)."),
        3, yFormat, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(format_JPanel, new JLabel ("NaN value:"),
        0, ++yFormat, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NaNValue_JTextField = new JTextField (10);
    __NaNValue_JTextField.setToolTipText("Specify Blank to write a blank in the output");
    __NaNValue_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(format_JPanel, __NaNValue_JTextField,
        1, yFormat, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(format_JPanel, new JLabel (
        "Optional - value to use for NaN (use " + __command._Blank + " to write a blank)."),
        3, yFormat, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    // Panel for inserts.
    int yInsert = -1;
    JPanel insert_JPanel = new JPanel();
    insert_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Inserts", insert_JPanel );

    JGUIUtil.addComponent(insert_JPanel, new JLabel (
        "These parameters specify content to prepend before the table and append after the table."),
        0, ++yInsert, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(insert_JPanel, new JLabel (
        "The inserts should contain markdown content."),
        0, ++yInsert, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(insert_JPanel, new JLabel (
        "${Property} found in the inserts will be expanded with processor properties."),
        0, ++yInsert, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(insert_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yInsert, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Prepend text area is resizable.
    JGUIUtil.addComponent(insert_JPanel, new JLabel ( "Prepend text:" ),
        0, ++yInsert, 1, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PrependText_JTextArea = new JTextArea (8,60);
    __PrependText_JTextArea.setToolTipText("Optional text to prepend, OK to include line breaks, can use ${Property}.");
    __PrependText_JTextArea.setLineWrap ( true );
    __PrependText_JTextArea.setWrapStyleWord ( true );
    __PrependText_JTextArea.addKeyListener(this);
    //JGUIUtil.addComponent(insert_JPanel, new JScrollPane(__PrependText_JTextArea),
    //    1, yInsert, 1, 1, 1.0, 1.0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    //JGUIUtil.addComponent(insert_JPanel, new JLabel("Optional."),
    //    3, yInsert, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(insert_JPanel, new JScrollPane(__PrependText_JTextArea),
        1, yInsert, 7, 1, 1.0, 1.0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);

    JGUIUtil.addComponent(insert_JPanel, new JLabel ("Prepend file:" ),
		0, ++yInsert, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__PrependFile_JTextField = new JTextField ( 50 );
	__PrependFile_JTextField.setToolTipText("Specify the file to prepend, can use ${Property} notation");
	__PrependFile_JTextField.addKeyListener ( this );
    // Input file layout fights back with other rows so put in its own panel.
	JPanel PrependFile_JPanel = new JPanel();
	PrependFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(PrependFile_JPanel, __PrependFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browsePrepend_JButton = new SimpleJButton ( "...", this );
	__browsePrepend_JButton.setToolTipText("Browse for file to prepend");
    JGUIUtil.addComponent(PrependFile_JPanel, __browsePrepend_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path.
		__pathPrepend_JButton = new SimpleJButton(__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(PrependFile_JPanel, __pathPrepend_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(insert_JPanel, PrependFile_JPanel,
		1, yInsert, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Append text area is resizable.
    JGUIUtil.addComponent(insert_JPanel, new JLabel ( "Append text:" ),
        0, ++yInsert, 1, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __AppendText_JTextArea = new JTextArea (8,60);
    __AppendText_JTextArea.setToolTipText("Optional text to append, OK to include line breaks, can use ${Property}.");
    __AppendText_JTextArea.setLineWrap ( true );
    __AppendText_JTextArea.setWrapStyleWord ( true );
    __AppendText_JTextArea.addKeyListener(this);
    //JGUIUtil.addComponent(insert_JPanel, new JScrollPane(__AppendText_JTextArea),
    //    1, yInsert, 1, 1, 1.0, 1.0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    //JGUIUtil.addComponent(main_JPanel, new JLabel("Optional."),
    //    3, yInsert, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(insert_JPanel, new JScrollPane(__AppendText_JTextArea),
        1, yInsert, 7, 1, 1.0, 1.0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);

    JGUIUtil.addComponent(insert_JPanel, new JLabel ("Append file:" ),
		0, ++yInsert, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__AppendFile_JTextField = new JTextField ( 50 );
	__AppendFile_JTextField.setToolTipText("Specify the file to append, can use ${Property} notation");
	__AppendFile_JTextField.addKeyListener ( this );
    // Input file layout fights back with other rows so put in its own panel.
	JPanel AppendFile_JPanel = new JPanel();
	AppendFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(AppendFile_JPanel, __AppendFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browseAppend_JButton = new SimpleJButton ( "...", this );
	__browseAppend_JButton.setToolTipText("Browse for file to append");
    JGUIUtil.addComponent(AppendFile_JPanel, __browseAppend_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path.
		__pathAppend_JButton = new SimpleJButton(__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(AppendFile_JPanel, __pathAppend_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(insert_JPanel, AppendFile_JPanel,
		1, yInsert, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Panel for output file.
    int yOutput = -1;
    JPanel output_JPanel = new JPanel();
    output_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Output File", output_JPanel );

    JGUIUtil.addComponent(output_JPanel, new JLabel (
        "These parameters specify the output file to write."),
        0, ++yOutput, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel (
        "If apending, the existing file will be opened and appended at the end.  Content from the 'Inserts' will bracket the appended table."),
        0, ++yOutput, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel (
        "Comments are written with surrounding HTML <!-- and --> and will not be visible in a markdown viewer."),
        0, ++yOutput, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(output_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++yOutput, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(output_JPanel, new JLabel ( "Output file to write:" ),
		0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OutputFile_JTextField = new JTextField ( 50 );
	__OutputFile_JTextField.setToolTipText("Specify the path to the output file or use ${Property} notation");
	__OutputFile_JTextField.addKeyListener ( this );
    // Output file layout fights back with other rows so put in its own panel.
	JPanel OutputFile_JPanel = new JPanel();
	OutputFile_JPanel.setLayout(new GridBagLayout());
	JGUIUtil.addComponent(OutputFile_JPanel, __OutputFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );
	__browseOutput_JButton = new SimpleJButton ( "...", this );
	__browseOutput_JButton.setToolTipText("Browse for Markdown output file");
	JGUIUtil.addComponent(OutputFile_JPanel, __browseOutput_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path.
		__pathOutput_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(OutputFile_JPanel, __pathOutput_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	}
	JGUIUtil.addComponent(output_JPanel, OutputFile_JPanel,
		1, yOutput, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(output_JPanel, new JLabel ( "Append?:"),
		0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Append_JComboBox = new SimpleJComboBox ( false );
	List<String> appendChoices = new ArrayList<>();
	appendChoices.add ( "" );	// Default.
	appendChoices.add ( __command._False );
	appendChoices.add ( __command._True );
	__Append_JComboBox.setData(appendChoices);
	__Append_JComboBox.select ( 0 );
	__Append_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(output_JPanel, __Append_JComboBox,
		1, yOutput, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel(
		"Optional - whether to append to the output file (default=" + __command._False + ")."),
		3, yOutput, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(output_JPanel, new JLabel ("Write file header comments?:"),
        0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __WriteHeaderComments_JComboBox = new SimpleJComboBox ( false );
    __WriteHeaderComments_JComboBox.setToolTipText("Header comments indicate the author and other information, will use <!--- --> HTML comments.");
    List<String> writeHeaderCommentsList = new ArrayList<>();
    writeHeaderCommentsList.add("");
    writeHeaderCommentsList.add(__command._False);
    writeHeaderCommentsList.add(__command._True);
    __WriteHeaderComments_JComboBox.setData ( writeHeaderCommentsList );
    __WriteHeaderComments_JComboBox.select(0);
    __WriteHeaderComments_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(output_JPanel, __WriteHeaderComments_JComboBox,
        1, yOutput, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel (
        "Optional - should file header comments be written? (default=" + __command._False + ")."),
        3, yOutput, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    /*
     JGUIUtil.addComponent(main_JPanel, new JLabel ("Write column names?:"),
         0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
     __WriteColumnNames_JComboBox = new SimpleJComboBox ( false );
     List<String> writeColumnNamesList = new ArrayList<>();
     writeColumnNamesList.add("");
     writeColumnNamesList.add(__command._False);
     writeColumnNamesList.add(__command._True);
     __WriteColumnNames_JComboBox.setData ( writeColumnNamesList );
     __WriteColumnNames_JComboBox.select(0);
     __WriteColumnNames_JComboBox.addItemListener (this);
     JGUIUtil.addComponent(main_JPanel, __WriteColumnNames_JComboBox,
         1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
     JGUIUtil.addComponent(main_JPanel, new JLabel (
         "Optional - should column names be written? (default=" + __command._True + ")."),
         3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
         */

    // Panel for output schema file.
   	/*
    int ySchema = -1;
    JPanel schema_JPanel = new JPanel();
    schema_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Schema File", schema_JPanel );

    JGUIUtil.addComponent(schema_JPanel, new JLabel (
        "Specify the optional schema file to write."),
        0, ++ySchema, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(schema_JPanel, new JLabel (
        "A schema file provides metadata about the markdown file columns, which can be used by other software."),
        0, ++ySchema, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(schema_JPanel, new JLabel (
        "These parameters may be removed in the future because a schema file is more appropriate for CSV and other data file formats."),
        0, ++ySchema, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(schema_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
    	0, ++ySchema, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(schema_JPanel, new JLabel ( "Output schema file to write:" ),
		0, ++ySchema, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OutputSchemaFile_JTextField = new JTextField ( 50 );
	__OutputSchemaFile_JTextField.setToolTipText("Specify the path to the output schema file or use ${Property} notation");
	__OutputSchemaFile_JTextField.addKeyListener ( this );
	// Output file layout fights back with other rows so put in its own panel.
	JPanel OutputSchemaFile_JPanel = new JPanel();
	OutputSchemaFile_JPanel.setLayout(new GridBagLayout());
	JGUIUtil.addComponent(OutputSchemaFile_JPanel, __OutputSchemaFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );
    __browseSchema_JButton = new SimpleJButton ( "...", this );
	__browseSchema_JButton.setToolTipText("Browse for Schema output file");
	JGUIUtil.addComponent(OutputSchemaFile_JPanel, __browseSchema_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path.
		__pathSchema_JButton = new SimpleJButton( __RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(OutputSchemaFile_JPanel, __pathSchema_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	}
	JGUIUtil.addComponent(schema_JPanel, OutputSchemaFile_JPanel,
		1, ySchema, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(schema_JPanel, new JLabel ("Output schema format:"),
        0, ++ySchema, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputSchemaFormat_JComboBox = new SimpleJComboBox ( false );
    List<String> schemaFormatChoices = new ArrayList<>();
    schemaFormatChoices.add("");
    schemaFormatChoices.add(__command._GoogleBigQuery);
    schemaFormatChoices.add(__command._JSONTableSchema);
    __OutputSchemaFormat_JComboBox.setData ( schemaFormatChoices );
    __OutputSchemaFormat_JComboBox.select(0);
    __OutputSchemaFormat_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(schema_JPanel, __OutputSchemaFormat_JComboBox,
        1, ySchema, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(schema_JPanel, new JLabel (
        "Optional - schema format (default=" + __command._JSONTableSchema + ")."),
        3, ySchema, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
   	*/

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __command_JTextArea = new JTextArea ( 4, 50 );
    __command_JTextArea.setLineWrap ( true );
    __command_JTextArea.setWrapStyleWord ( true );
    __command_JTextArea.setEditable ( false );
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
    	1, y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

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

    refresh();

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
public void itemStateChanged (ItemEvent e) {
    refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event ) {
	int code = event.getKeyCode();

	/* Don't respond to enter because newlines can be entered for inserts.
	if ( code == KeyEvent.VK_ENTER ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	*/
}

public void keyReleased ( KeyEvent event ) {
	// Only refresh if the event is in the file TextField.
	refresh();
}

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
	String routine = getClass().getSimpleName() + ".refresh";
	// Table.
    String TableID = "";
    String IncludeColumns = "";
    String ExcludeColumns = "";
    //String WriteColumnNames = "";
    //String Delimiter = "";
    //String AlwaysQuoteDateTimes = "";
    //String AlwaysQuoteStrings = "";
    // Formatting.
    String LinkColumns = "";
    String NewlineReplacement = "";
    String NaNValue = "";
    // Insert.
    String PrependText = "";
    String PrependFile = "";
    String AppendText = "";
    String AppendFile = "";
    // Output FIle.
	String OutputFile = "";
	String Append = "";
    String WriteHeaderComments = "";
    // Schema file.
    String OutputSchemaFile = "";
    String OutputSchemaFormat = "";
	__error_wait = false;
	PropList parameters = null;
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command.
		parameters = __command.getCommandParameters();
		// Table.
        TableID = parameters.getValue ( "TableID" );
        IncludeColumns = parameters.getValue ( "IncludeColumns" );
        ExcludeColumns = parameters.getValue ( "ExcludeColumns" );
        WriteHeaderComments = parameters.getValue ( "WriteHeaderComments" );
        //WriteColumnNames = parameters.getValue ( "WriteColumnNames" );
        //Delimiter = parameters.getValue ( "Delimiter" );
        //AlwaysQuoteDateTimes = parameters.getValue ( "AlwaysQuoteDateTimes" );
        //AlwaysQuoteStrings = parameters.getValue ( "AlwaysQuoteStrings" );
        // Formatting.
        LinkColumns = parameters.getValue ( "LinkColumns" );
        NewlineReplacement = parameters.getValue ( "NewlineReplacement" );
        NaNValue = parameters.getValue ( "NaNValue" );
        // Inserts.
		PrependText = parameters.getValue( "PrependText" );
		if ( PrependText != null ) {
			PrependText = PrependText.replace("\\n","\n");
		}
		PrependFile = parameters.getValue ( "PrependFile" );
		AppendText = parameters.getValue( "AppendText" );
		if ( AppendText != null ) {
			AppendText = AppendText.replace("\\n","\n");
		}
		AppendFile = parameters.getValue ( "AppendFile" );
        // Output File.
		OutputFile = parameters.getValue ( "OutputFile" );
		Append = parameters.getValue ( "Append" );
		// Schema file.
        OutputSchemaFile = parameters.getValue ( "OutputSchemaFile" );
        OutputSchemaFormat = parameters.getValue ( "OutputSchemaFormat" );
        // Populate the UI components.
        // Table.
        if ( TableID == null ) {
            // Select default.
            if ( __TableID_JComboBox.getItemCount() > 0 ) {
                __TableID_JComboBox.select ( 0 );
            }
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __TableID_JComboBox,TableID, JGUIUtil.NONE, null, null ) ) {
                __TableID_JComboBox.select ( TableID );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nTableID value \"" + TableID +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( IncludeColumns != null ) {
            __IncludeColumns_JTextField.setText ( IncludeColumns );
        }
        if ( ExcludeColumns != null ) {
            __ExcludeColumns_JTextField.setText ( ExcludeColumns );
        }
        /*
        if ( JGUIUtil.isSimpleJComboBoxItem(__WriteColumnNames_JComboBox, WriteColumnNames, JGUIUtil.NONE, null, null ) ) {
            __WriteColumnNames_JComboBox.select ( WriteColumnNames );
        }
        else {
            if ( (WriteColumnNames == null) || WriteColumnNames.equals("") ) {
                // New command...select the default.
                if ( __WriteColumnNames_JComboBox.getItemCount() > 0 ) {
                    __WriteColumnNames_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command.
                Message.printWarning ( 1, routine, "Existing command references an invalid "+
                  "WriteColumnNames parameter \"" + WriteColumnNames + "\".  Select a different value or Cancel." );
            }
        }
        if (Delimiter != null) {
            __Delimiter_JTextField.setText(Delimiter);
        }
        if ( JGUIUtil.isSimpleJComboBoxItem(__AlwaysQuoteDateTimes_JComboBox, AlwaysQuoteDateTimes, JGUIUtil.NONE, null, null ) ) {
            __AlwaysQuoteDateTimes_JComboBox.select ( AlwaysQuoteDateTimes );
        }
        else {
            if ( (AlwaysQuoteDateTimes == null) || AlwaysQuoteDateTimes.equals("") ) {
                // New command...select the default.
                if ( __AlwaysQuoteDateTimes_JComboBox.getItemCount() > 0 ) {
                    __AlwaysQuoteDateTimes_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command.
                Message.printWarning ( 1, routine, "Existing command references an invalid "+
                  "AlwaysQuoteDateTimes parameter \"" + AlwaysQuoteDateTimes + "\".  Select a different value or Cancel." );
            }
        }
        if ( JGUIUtil.isSimpleJComboBoxItem(__AlwaysQuoteStrings_JComboBox, AlwaysQuoteStrings, JGUIUtil.NONE, null, null ) ) {
            __AlwaysQuoteStrings_JComboBox.select ( AlwaysQuoteStrings );
        }
        else {
            if ( (AlwaysQuoteStrings == null) || AlwaysQuoteStrings.equals("") ) {
                // New command...select the default.
                if ( __AlwaysQuoteStrings_JComboBox.getItemCount() > 0 ) {
                    __AlwaysQuoteStrings_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command.
                Message.printWarning ( 1, routine, "Existing command references an invalid "+
                  "AlwaysQuoteStrings parameter \"" + AlwaysQuoteStrings + "\".  Select a different value or Cancel." );
            }
        }
        */
        // Formatting.
        if ( LinkColumns != null ) {
            __LinkColumns_JTextArea.setText ( LinkColumns );
        }
        if (NewlineReplacement != null) {
            __NewlineReplacement_JTextField.setText(NewlineReplacement);
        }
        if ( NaNValue != null ) {
            __NaNValue_JTextField.setText(NaNValue);
        }
        // Inserts.
		if ( PrependText != null ) {
		    __PrependText_JTextArea.setText( PrependText );
		}
		if ( PrependFile != null ) {
			__PrependFile_JTextField.setText (PrependFile);
		}
		if ( AppendText != null ) {
		    __AppendText_JTextArea.setText( AppendText );
		}
		if ( AppendFile != null ) {
			__AppendFile_JTextField.setText (AppendFile);
		}
        // Output File.
		if ( OutputFile != null ) {
			__OutputFile_JTextField.setText (OutputFile);
		}
		if ( JGUIUtil.isSimpleJComboBoxItem(__Append_JComboBox, Append,JGUIUtil.NONE, null, null ) ) {
			__Append_JComboBox.select ( Append );
		}
		else {
            if ( (Append == null) || Append.equals("") ) {
				// New command...select the default.
				__Append_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"Append parameter \"" +	Append +
				"\".  Select a\n value or Cancel." );
			}
		}
        if ( JGUIUtil.isSimpleJComboBoxItem(__WriteHeaderComments_JComboBox, WriteHeaderComments, JGUIUtil.NONE, null, null ) ) {
            __WriteHeaderComments_JComboBox.select ( WriteHeaderComments );
        }
        else {
            if ( (WriteHeaderComments == null) || WriteHeaderComments.equals("") ) {
                // New command...select the default.
                if ( __WriteHeaderComments_JComboBox.getItemCount() > 0 ) {
                    __WriteHeaderComments_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command.
                Message.printWarning ( 1, routine, "Existing command references an invalid "+
                  "WriteHeaderComments parameter \"" + WriteHeaderComments + "\".  Select a different value or Cancel." );
            }
        }
        /*
		// Schema file.
		if ( OutputSchemaFile != null ) {
			__OutputSchemaFile_JTextField.setText (OutputSchemaFile);
		}
		// TODO smalers 2025-01-04 Schema format would go here.
		*/
	}
	// Regardless, reset the command from the fields.
	// Table.
    TableID = __TableID_JComboBox.getSelected();
	IncludeColumns = __IncludeColumns_JTextField.getText().trim();
	ExcludeColumns = __ExcludeColumns_JTextField.getText().trim();
	/*
    WriteColumnNames = __WriteColumnNames_JComboBox.getSelected();
    Delimiter = __Delimiter_JTextField.getText().trim();
    AlwaysQuoteDateTimes = __AlwaysQuoteDateTimes_JComboBox.getSelected();
    AlwaysQuoteStrings = __AlwaysQuoteStrings_JComboBox.getSelected();
    */
	// Formatting.
	LinkColumns = __LinkColumns_JTextArea.getText().trim().replace("\n"," ");
    NewlineReplacement = __NewlineReplacement_JTextField.getText().trim();
    NaNValue = __NaNValue_JTextField.getText().trim();
    // Inserts:
    // - don't trim because trailing newline may be used
	PrependText = __PrependText_JTextArea.getText();
    if ( PrependText != null ) {
    	// Replace internal newline with escaped string for command text.
    	PrependText = PrependText.replace("\n", "\\n");
    }
	PrependFile = __PrependFile_JTextField.getText().trim();
	AppendText = __AppendText_JTextArea.getText();
    if ( AppendText != null ) {
    	// Replace internal newline with escaped string for command text.
    	AppendText = AppendText.replace("\n", "\\n");
    }
	AppendFile = __AppendFile_JTextField.getText().trim();
    // Output File.
	OutputFile = __OutputFile_JTextField.getText().trim();
	Append = __Append_JComboBox.getSelected();
    WriteHeaderComments = __WriteHeaderComments_JComboBox.getSelected();
    // Schema File.
    //OutputSchemaFile = __OutputSchemaFile_JTextField.getText().trim();
    //OutputSchemaFormat = __OutputSchemaFormat_JComboBox.getSelected();
	parameters = new PropList ( __command.getCommandName() );
	// Table.
	if ( TableID != null ) {
	    parameters.add ( "TableID=" + TableID );
	}
    parameters.add ( "IncludeColumns=" + IncludeColumns );
    parameters.add ( "ExcludeColumns=" + ExcludeColumns );
    /*
    if ( WriteColumnNames != null ) {
        parameters.add ( "WriteColumnNames=" + WriteColumnNames );
    }
    parameters.add("Delimiter=" + Delimiter );
    if ( AlwaysQuoteDateTimes != null ) {
        parameters.add ( "AlwaysQuoteDateTimes=" + AlwaysQuoteDateTimes );
    }
    if ( AlwaysQuoteStrings != null ) {
        parameters.add ( "AlwaysQuoteStrings=" + AlwaysQuoteStrings );
    }
    */
    // Formatting.
	parameters.add ( "LinkColumns=" + LinkColumns );
    parameters.add("NewlineReplacement=" + NewlineReplacement );
    parameters.add("NaNValue=" + NaNValue );
    // Inserts.
	parameters.add ( "PrependText=" + PrependText );
	parameters.add ( "PrependFile=" + PrependFile );
	parameters.add ( "AppendText=" + AppendText );
	parameters.add ( "AppendFile=" + AppendFile );
	// Output FIle.
	parameters.add ( "OutputFile=" + OutputFile );
	parameters.add ( "Append=" + Append );
    if ( WriteHeaderComments != null ) {
        parameters.add ( "WriteHeaderComments=" + WriteHeaderComments );
    }
    // Schema File.
    parameters.add ( "OutputSchemaFile=" + OutputSchemaFile );
    if ( OutputSchemaFormat != null ) {
        parameters.add ( "OutputSchemaFormat=" + OutputSchemaFormat );
    }
	__command_JTextArea.setText( __command.toString ( parameters ).trim() );
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
	/*
	if ( __pathSchema_JButton != null ) {
		if ( (OutputSchemaFile != null) && !OutputSchemaFile.isEmpty() ) {
			__pathSchema_JButton.setEnabled ( true );
			File f = new File ( OutputSchemaFile );
			if ( f.isAbsolute() ) {
				__pathSchema_JButton.setText ( __RemoveWorkingDirectory );
				__pathSchema_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
            	__pathSchema_JButton.setText ( __AddWorkingDirectory );
            	__pathSchema_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__pathSchema_JButton.setEnabled(false);
		}
	}
	*/
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