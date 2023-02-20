// PrintTextFile_JDialog - Editor for PrintTextFile command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2019 Colorado Department of Natural Resources

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

package rti.tscommandprocessor.commands.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
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

import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PrintUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

/**
Editor for PrintTextFile command.
*/
@SuppressWarnings("serial")
public class PrintTextFile_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{
private final String __AddWorkingDirectory = "Abs";
private final String __RemoveWorkingDirectory = "Rel";

private SimpleJButton __browse_JButton = null; // input file
private SimpleJButton __browse2_JButton = null; // output file
private SimpleJButton __path_JButton = null;
private SimpleJButton __path2_JButton = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private JTextField __InputFile_JTextField = null;
private SimpleJComboBox __PrinterName_JComboBox = null;
private SimpleJComboBox __PaperSize_JComboBox = null;
private SimpleJComboBox __PaperSource_JComboBox = null;
private SimpleJComboBox __Orientation_JComboBox = null;
private JTextField __MarginLeft_JTextField = null;
private JTextField __MarginRight_JTextField = null;
private JTextField __MarginTop_JTextField = null;
private JTextField __MarginBottom_JTextField = null;
private JTextField __LinesPerPage_JTextField = null;
private JTextField __Header_JTextField = null;
private JTextField __Footer_JTextField = null;
private SimpleJComboBox __ShowLineCount_JComboBox = null;
private SimpleJComboBox __ShowPageCount_JComboBox = null;
private JTextField __Pages_JTextField = null;
private SimpleJComboBox __DoubleSided_JComboBox = null;
private JTextField __OutputFile_JTextField = null;
private SimpleJComboBox __IfNotFound_JComboBox = null;
private SimpleJComboBox __ShowDialog_JComboBox = null;
private JTextArea __command_JTextArea = null;
private String __working_dir = null; // Working directory.
private boolean __error_wait = false;
private boolean __first_time = true;
private PrintTextFile_Command __command = null;
private boolean __ok = false; // Has user pressed OK to close the dialog.

//private PrinterJob __printerJob = null; // Used to fill out parameter choices

private PrintService [] __printServiceArray = null; // Used to fill out parameter choices

private PrintService __selectedPrintService = null; // The print service that is selected for printing

private PrintService __defaultPrintService = null; // The default print service for the computer

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public PrintTextFile_JDialog ( JFrame parent, PrintTextFile_Command command )
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
		    fc = JFileChooserFactory.createJFileChooser(__working_dir );
		}
		fc.setDialogTitle( "Select File to Print");
		
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
					Message.printWarning ( 1,"PrintTextFile_JDialog", "Error converting file to relative path." );
				}
				JGUIUtil.setLastFileDialogDirectory(directory);
				refresh();
			}
		}
	}
    else if ( o == __browse2_JButton ) {
        String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
        JFileChooser fc = null;
        if ( last_directory_selected != null ) {
            fc = JFileChooserFactory.createJFileChooser(last_directory_selected );
        }
        else {
            fc = JFileChooserFactory.createJFileChooser(__working_dir );
        }
        fc.setDialogTitle("Specify Output (Print) File to Write");
    
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String directory = fc.getSelectedFile().getParent();
            String path = fc.getSelectedFile().getPath(); 
            JGUIUtil.setLastFileDialogDirectory(directory);
			// Convert path to relative path by default.
			try {
				__OutputFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
			}
			catch ( Exception e ) {
				Message.printWarning ( 1,"PrintTextFile_JDialog", "Error converting file to relative path." );
			}
            refresh();
        }   
    }
	else if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "PrintTextFile");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else if ( o == __path_JButton ) {
		if ( __path_JButton.getText().equals(__AddWorkingDirectory) ) {
			__InputFile_JTextField.setText (IOUtil.toAbsolutePath(__working_dir,__InputFile_JTextField.getText() ) );
		}
		else if ( __path_JButton.getText().equals(__RemoveWorkingDirectory) ) {
			try {
                __InputFile_JTextField.setText ( IOUtil.toRelativePath ( __working_dir,
                        __InputFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1, "PrintTextFile_JDialog",
				"Error converting file name to relative path." );
			}
		}
		refresh ();
	}
    else if ( o == __path2_JButton ) {
        if (__path2_JButton.getText().equals(__AddWorkingDirectory)) {
            __OutputFile_JTextField.setText (
            IOUtil.toAbsolutePath(__working_dir, __OutputFile_JTextField.getText()));
        }
        else if (__path2_JButton.getText().equals(__RemoveWorkingDirectory)) {
            try {
                __OutputFile_JTextField.setText (
                IOUtil.toRelativePath (__working_dir, __OutputFile_JTextField.getText()));
            }
            catch (Exception e) {
                Message.printWarning (1, "PrintNetwork_JDialog", "Error converting file to relative path.");
            }
        }
        refresh ();
    }
	else {
	    // Choices...
		refresh();
	}
}

/**
Check the GUI state to make sure that appropriate components are enabled/disabled.
*/
private void checkGUIState ()
{

}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
	PropList props = new PropList ( "" );
	String InputFile = __InputFile_JTextField.getText().trim();
	String PrinterName = __PrinterName_JComboBox.getSelected();
	String PaperSize = __PaperSize_JComboBox.getSelected();
	String PaperSource = __PaperSource_JComboBox.getSelected();
	String Orientation = __Orientation_JComboBox.getSelected();
	String MarginLeft = __MarginLeft_JTextField.getText().trim();
	String MarginRight = __MarginRight_JTextField.getText().trim();
	String MarginTop = __MarginTop_JTextField.getText().trim();
	String MarginBottom = __MarginBottom_JTextField.getText().trim();
	String LinesPerPage = __LinesPerPage_JTextField.getText().trim();
	String Header = __Header_JTextField.getText().trim();
	String Footer = __Footer_JTextField.getText().trim();
	String ShowLineCount = __ShowLineCount_JComboBox.getSelected();
	String ShowPageCount = __ShowPageCount_JComboBox.getSelected();
	String Pages = __Pages_JTextField.getText().trim();
	String DoubleSided = __DoubleSided_JComboBox.getSelected();
	String OutputFile = __OutputFile_JTextField.getText().trim();
	String ShowDialog = __ShowDialog_JComboBox.getSelected();
	String IfNotFound = __IfNotFound_JComboBox.getSelected();
	
	__error_wait = false;
	if ( InputFile.length() > 0 ) {
		props.set ( "InputFile", InputFile );
	}
    if ( PrinterName.length() > 0 ) {
        props.set ( "PrinterName", PrinterName );
    }
    if ( PaperSize.length() > 0 ) {
        props.set ( "PaperSize", getShortPaperSize(PaperSize) );
    }
    if ( PaperSource.length() > 0 ) {
        props.set ( "PaperSource", PaperSource );
    }
    if ( Orientation.length() > 0 ) {
        props.set ( "Orientation", Orientation );
    }
    if ( MarginLeft.length() > 0 ) {
        props.set ( "MarginLeft", MarginLeft );
    }
    if ( MarginRight.length() > 0 ) {
        props.set ( "MarginRight", MarginRight );
    }
    if ( MarginTop.length() > 0 ) {
        props.set ( "MarginTop", MarginTop );
    }
    if ( MarginBottom.length() > 0 ) {
        props.set ( "MarginBottom", MarginBottom );
    }
    if ( LinesPerPage.length() > 0 ) {
        props.set ( "LinesPerPage", LinesPerPage );
    }
    if ( Header.length() > 0 ) {
        props.set ( "Header", Header );
    }
    if ( Footer.length() > 0 ) {
        props.set ( "Footer", Footer );
    }
    if ( ShowLineCount.length() > 0 ) {
        props.set ( "ShowLineCount", ShowLineCount );
    }
    if ( ShowPageCount.length() > 0 ) {
        props.set ( "ShowPageCount", ShowPageCount );
    }
    if ( Pages.length() > 0 ) {
        props.set ( "Pages", Pages );
    }
    if ( DoubleSided.length() > 0 ) {
        props.set ( "DoubleSided", DoubleSided );
    }
    if (OutputFile.length() > 0) {
        props.set ( "OutputFile", OutputFile );
    }
    if ( ShowDialog.length() > 0 ) {
        props.set ( "ShowDialog", ShowDialog );
    }
	if ( IfNotFound.length() > 0 ) {
		props.set ( "IfNotFound", IfNotFound );
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
{	String InputFile = __InputFile_JTextField.getText().trim();
    String PrinterName = __PrinterName_JComboBox.getSelected();
    String PaperSize = __PaperSize_JComboBox.getSelected();
    String PaperSource = __PaperSource_JComboBox.getSelected();
    String Orientation = __Orientation_JComboBox.getSelected();
    String MarginLeft = __MarginLeft_JTextField.getText().trim();
    String MarginRight = __MarginRight_JTextField.getText().trim();
    String MarginTop = __MarginTop_JTextField.getText().trim();
    String MarginBottom = __MarginBottom_JTextField.getText().trim();
    String LinesPerPage = __LinesPerPage_JTextField.getText().trim();
    String Header = __Header_JTextField.getText().trim();
    String Footer = __Footer_JTextField.getText().trim();
    String ShowLineCount = __ShowLineCount_JComboBox.getSelected();
    String ShowPageCount = __ShowPageCount_JComboBox.getSelected();
    String Pages = __Pages_JTextField.getText().trim();
    String DoubleSided = __DoubleSided_JComboBox.getSelected();
    String OutputFile = __OutputFile_JTextField.getText().trim();
    String ShowDialog = __ShowDialog_JComboBox.getSelected();
	String IfNotFound = __IfNotFound_JComboBox.getSelected();
	__command.setCommandParameter ( "InputFile", InputFile );
	__command.setCommandParameter ( "PrinterName", PrinterName );
	__command.setCommandParameter ( "PaperSize", getShortPaperSize(PaperSize) );
	__command.setCommandParameter ( "PaperSource", PaperSource );
	__command.setCommandParameter ( "Orientation", Orientation );
	__command.setCommandParameter ( "MarginLeft", MarginLeft );
	__command.setCommandParameter ( "MarginRight", MarginRight );
	__command.setCommandParameter ( "MarginTop", MarginTop );
	__command.setCommandParameter ( "MarginBottom", MarginBottom );
	__command.setCommandParameter ( "LinesPerPage", LinesPerPage );
	__command.setCommandParameter ( "Header", Header );
	__command.setCommandParameter ( "Footer", Footer );
	__command.setCommandParameter ( "ShowLineCount", ShowLineCount );
	__command.setCommandParameter ( "ShowPageCount", ShowPageCount );
	__command.setCommandParameter ( "Pages", Pages );
	__command.setCommandParameter ( "DoubleSided", DoubleSided );
	__command.setCommandParameter ( "OutputFile", OutputFile );
	__command.setCommandParameter ( "ShowDialog", ShowDialog );
	__command.setCommandParameter ( "IfNotFound", IfNotFound );
}

/**
Return the short page size, including only the string before the note.
*/
private String getShortPaperSize ( String longPaperSize )
{
    if ( (longPaperSize == null) || (longPaperSize.length() == 0) ) {
        return "";
    }
    else {
        int pos = longPaperSize.indexOf ( " " );
        if ( pos < 0 ) {
            // No note
            return longPaperSize;
        }
        else {
            // Has note
            return longPaperSize.substring(0,pos).trim();
        }
    }
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, PrintTextFile_Command command )
{	__command = command;
	CommandProcessor processor =__command.getCommandProcessor();
	
	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( processor, __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"This command prints a text file using basic formatting." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Determining supported printer settings may take a few seconds." ),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The margins agree with the orientation (e.g., for letter size portrait orientation, " +
        "left margin is long edge; for landscape, left margin is for short edge)." ),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    if ( __working_dir != null ) {
    	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"It is recommended that the file name is relative to the working directory, which is:"),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"    " + __working_dir),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("File to print:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputFile_JTextField = new JTextField ( 50 );
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
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Printer name:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    //this.__printerJob = PrinterJob.getPrinterJob();
    this.__printServiceArray = PrinterJob.lookupPrintServices();
    this.__defaultPrintService = PrintServiceLookup.lookupDefaultPrintService();
    List<String> printerNames = new Vector<String>();
    printerNames.add ( "" ); // Corresponds to default printer
    for ( int i = 0; i < this.__printServiceArray.length; i++ ) {
        printerNames.add (this.__printServiceArray[i].getName());
    }
    Collections.sort(printerNames);
    __PrinterName_JComboBox = new SimpleJComboBox ( false );
    __PrinterName_JComboBox.setData ( printerNames );
    __PrinterName_JComboBox.select ( 0 );
    __PrinterName_JComboBox.addItemListener ( this );
   JGUIUtil.addComponent(main_JPanel, __PrinterName_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - printer name (default=default printer)."), 
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Paper size:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PaperSize_JComboBox = new SimpleJComboBox ( 30, false );
    List<String> paperSizeChoices = new ArrayList<String>();
    paperSizeChoices.add ( "" );  // Default
    // TODO SAM 2011-06-24 Get from a PrinterJob
    __PaperSize_JComboBox.setData(paperSizeChoices);
    __PaperSize_JComboBox.select ( 0 );
    __PaperSize_JComboBox.addActionListener ( this );
   JGUIUtil.addComponent(main_JPanel, __PaperSize_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - paper size name (default=printer-specific)."), 
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);    
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Paper source (tray):"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PaperSource_JComboBox = new SimpleJComboBox ( false );
    __PaperSource_JComboBox.setEnabled(false); // TODO SAM 2011-06-24 For now disable
    List<String> sourceChoices = new ArrayList<String>();
    sourceChoices.add ( "" );  // Default
    // TODO SAM 2011-06-24 Get from a PrinterJob
    __PaperSource_JComboBox.setData(sourceChoices);
    __PaperSource_JComboBox.select ( 0 );
    __PaperSource_JComboBox.addActionListener ( this );
   JGUIUtil.addComponent(main_JPanel, __PaperSource_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - paper source (default=default source/tray)."), 
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Orientation:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Orientation_JComboBox = new SimpleJComboBox ( false );
    List<String> orientChoices = new ArrayList<String>();
    orientChoices.add ( "" ); // Default
    // TODO SAM 2011-06-25 Figure out the reverse orientations
    orientChoices.add ( PrintUtil.getOrientationAsString(PageFormat.LANDSCAPE) );
    orientChoices.add ( PrintUtil.getOrientationAsString(PageFormat.PORTRAIT) );
    // TODO SAM 2011-06-24 Get from a PrinterJob
    __Orientation_JComboBox.setData(orientChoices);
    __Orientation_JComboBox.select ( 0 );
    __Orientation_JComboBox.addActionListener ( this );
   JGUIUtil.addComponent(main_JPanel, __Orientation_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - paper orientation (default=portrait)."), 
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Left margin:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MarginLeft_JTextField = new JTextField ( 10 );
    __MarginLeft_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __MarginLeft_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - left margin, inches (default=.75)."), 
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Right margin:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MarginRight_JTextField = new JTextField ( 10 );
    __MarginRight_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __MarginRight_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - right margin, inches (default=.75)."), 
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Top margin:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MarginTop_JTextField = new JTextField ( 10 );
    __MarginTop_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __MarginTop_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - top margin, inches (default=.75)."), 
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Bottom margin:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MarginBottom_JTextField = new JTextField ( 10 );
    __MarginBottom_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __MarginBottom_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - bottom margin, inches (default=.75)."), 
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Lines per page:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __LinesPerPage_JTextField = new JTextField ( 10 );
    __LinesPerPage_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __LinesPerPage_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - lines per page (default=depends on page size)."), 
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Header:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Header_JTextField = new JTextField ( 20 );
    __Header_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Header_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - for top of each page (default=none)."), 
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Footer:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Footer_JTextField = new JTextField ( 20 );
    __Footer_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Footer_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - for bottom of each page (default=none)."), 
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Show line count:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ShowLineCount_JComboBox = new SimpleJComboBox ( false );
    List<String> countChoices = new ArrayList<String>();
    countChoices.add ( "" );  // Default
    countChoices.add ( __command._False );
    countChoices.add ( __command._True );
    __ShowLineCount_JComboBox.setData(countChoices);
    __ShowLineCount_JComboBox.select ( 0 );
    __ShowLineCount_JComboBox.addActionListener ( this );
   JGUIUtil.addComponent(main_JPanel, __ShowLineCount_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - show line count at left (default=" + __command._False + ")."), 
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Show page count:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ShowPageCount_JComboBox = new SimpleJComboBox ( false );
    List<String> pageCountChoices = new ArrayList<String>();
    pageCountChoices.add ( "" );  // Default
    pageCountChoices.add ( __command._False );
    pageCountChoices.add ( __command._True );
    __ShowPageCount_JComboBox.setData(pageCountChoices);
    __ShowPageCount_JComboBox.select ( 0 );
    __ShowPageCount_JComboBox.addActionListener ( this );
   JGUIUtil.addComponent(main_JPanel, __ShowPageCount_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - show page count at bottom (default=" + __command._True + ")."), 
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Pages:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Pages_JTextField = new JTextField ( 20 );
    __Pages_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Pages_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - pages to print N,N-N, etc. (default=print all)."), 
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Double-sided?:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DoubleSided_JComboBox = new SimpleJComboBox ( false );
    __DoubleSided_JComboBox.setEnabled ( false ); // TODO SAM 2011-06-24 Enable later (Sides attribute)
    List<String> doubleChoices = new ArrayList<String>();
    doubleChoices.add ( "" );  // Default
    doubleChoices.add ( __command._False );
    doubleChoices.add ( __command._True );
    __DoubleSided_JComboBox.setData(doubleChoices);
    __DoubleSided_JComboBox.select ( 0 );
    __DoubleSided_JComboBox.addActionListener ( this );
   JGUIUtil.addComponent(main_JPanel, __DoubleSided_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - print double-sided? (default=" + __command._False + ")."), 
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Print file:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputFile_JTextField = new JTextField (35);
    __OutputFile_JTextField.addKeyListener (this);
    // Output file layout fights back with other rows so put in its own panel
	JPanel OutputFile_JPanel = new JPanel();
	OutputFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(OutputFile_JPanel, __OutputFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );
	__browse2_JButton = new SimpleJButton ( "...", this );
	__browse2_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(OutputFile_JPanel, __browse2_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__path2_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(OutputFile_JPanel, __path2_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	}
	JGUIUtil.addComponent(main_JPanel, OutputFile_JPanel,
		1, y, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Show dialog?:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ShowDialog_JComboBox = new SimpleJComboBox ( false );
    List<String> dialogChoices = new ArrayList<String>();
    dialogChoices.add ( "" );  // Default
    dialogChoices.add ( __command._False );
    dialogChoices.add ( __command._True );
    __ShowDialog_JComboBox.setData(dialogChoices);
    __ShowDialog_JComboBox.select ( 0 );
    __ShowDialog_JComboBox.addActionListener ( this );
   JGUIUtil.addComponent(main_JPanel, __ShowDialog_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - show printer dialog? (default=" + __command._False + ")."), 
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

   JGUIUtil.addComponent(main_JPanel, new JLabel ( "If not found?:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__IfNotFound_JComboBox = new SimpleJComboBox ( false );
	List<String> notFoundChoices = new ArrayList<String>();
	notFoundChoices.add ( "" );	// Default
	notFoundChoices.add ( __command._Ignore );
	notFoundChoices.add ( __command._Warn );
	notFoundChoices.add ( __command._Fail );
	__IfNotFound_JComboBox.setData(notFoundChoices);
	__IfNotFound_JComboBox.select ( 0 );
	__IfNotFound_JComboBox.addActionListener ( this );
   JGUIUtil.addComponent(main_JPanel, __IfNotFound_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional - action if file not found (default=" + __command._Warn + ")."), 
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea ( 4, 60 );
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.addKeyListener ( this );
	__command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	button_JPanel.add ( __ok_JButton = new SimpleJButton("OK", this) );
	__ok_JButton.setToolTipText("Save changes to command");
	button_JPanel.add(__cancel_JButton = new SimpleJButton("Cancel", this));	__cancel_JButton.setToolTipText("Cancel without saving changes to command");
	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");

	setTitle ( "Edit " + __command.getCommandName() + " command" );
	
	// Refresh the contents...
    checkGUIState();
    refresh ();

    pack();
    JGUIUtil.center( this );
	// Dialogs do not need to be resizable...
	setResizable ( false );
    super.setVisible( true );
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged ( ItemEvent e )
{   //checkGUIState();
    Object source = e.getSource();
    if ( source == __PrinterName_JComboBox ) {
        // First get the print service instance that goes with the printer name
        String printerName = __PrinterName_JComboBox.getSelected().trim();
        if ( (printerName == null) || printerName.equals("") ) {
            // Use default printer
            this.__selectedPrintService = this.__defaultPrintService;
        }
        else {
            // Specific printer has been selected...
            for ( int i = 0; i < __printServiceArray.length; i++ ) {
                if ( printerName.equalsIgnoreCase(__printServiceArray[i].getName())) {
                    this.__selectedPrintService = __printServiceArray[i];
                    break;
                }
            }
        }
        Message.printStatus(2, "", "Printer for PrintTextFile choices:  " + this.__selectedPrintService.getName() );
        // Need to update other choices
        // Available page sizes...
        List<String> supportedMediaSizes = PrintUtil.getSupportedMediaSizeNames(this.__selectedPrintService,
            true, // Include notes
            true ); // Include dimensions );
        // Add a blank
        supportedMediaSizes.add(0, "");
        // Reset the available page sizes...
        this.__PaperSize_JComboBox.setData(supportedMediaSizes);
    }
    refresh();
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
Refresh the command from the other text field contents.
*/
private void refresh ()
{	String routine = "PrintTextFile_JDialog.refresh";
	String InputFile = "";
	String PrinterName = "";
	String PaperSize = "";
	String PaperSource = "";
	String Orientation = "";
	String MarginLeft = "";
	String MarginRight = "";
	String MarginTop = "";
	String MarginBottom = "";
	String LinesPerPage = "";
	String Header = "";
	String Footer = "";
	String ShowLineCount = "";
	String ShowPageCount = "";
	String Pages = "";
	String DoubleSided = "";
	String OutputFile = "";
	String ShowDialog = "";
	String IfNotFound = "";
    PropList parameters = null;
	if ( __first_time ) {
		__first_time = false;
        parameters = __command.getCommandParameters();
		InputFile = parameters.getValue ( "InputFile" );
		PrinterName = parameters.getValue ( "PrinterName" );
		PaperSize = parameters.getValue ( "PaperSize" );
		PaperSource = parameters.getValue ( "PaperSource" );
		Orientation = parameters.getValue ( "Orientation" );
		MarginLeft = parameters.getValue ( "MarginLeft" );
		MarginRight = parameters.getValue ( "MarginRight" );
		MarginTop = parameters.getValue ( "MarginTop" );
		MarginBottom = parameters.getValue ( "MarginBottom" );
		LinesPerPage = parameters.getValue ( "LinesPerPage" );
		Header = parameters.getValue ( "Header" );
		Footer = parameters.getValue ( "Footer" );
	    ShowLineCount = parameters.getValue ( "ShowLineCount" );
		ShowPageCount = parameters.getValue ( "ShowPageCount" );
		Pages = parameters.getValue ( "Pages" );
		DoubleSided = parameters.getValue ( "DoubleSided" );
	    OutputFile = parameters.getValue ( "OutputFile" );
		ShowDialog = parameters.getValue ( "ShowDialog" );
		IfNotFound = parameters.getValue ( "IfNotFound" );
		if ( InputFile != null ) {
			__InputFile_JTextField.setText ( InputFile );
		}
        if ( JGUIUtil.isSimpleJComboBoxItem(__PrinterName_JComboBox, PrinterName,JGUIUtil.NONE, null, null ) ) {
            __PrinterName_JComboBox.select ( PrinterName );
        }
        else {
            if ( (PrinterName == null) || PrinterName.equals("") ) {
                // New command...select the default...
                __PrinterName_JComboBox.select ( 0 );
            }
            else {  // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                "PrinterName parameter \"" + PrinterName + "\".  Select a\n value or Cancel." );
            }
        }
        // Page size is handled differently because the choices contain notes that are not saved in the
        // command parameter.  Notes are after " - " but use space because - may occur in primary data
        try {
            JGUIUtil.selectTokenMatches(__PaperSize_JComboBox, true, " ", 0, 0, PaperSize, null );
        }
        catch ( Exception e ) {
            if ( (PaperSize == null) || PaperSize.equals("") ) {
                // New command...select the default...
                __PaperSize_JComboBox.select ( 0 );
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                "PaperSize parameter \"" + PaperSize + "\".  Select a\n value or Cancel." );
            }
        }
        if ( JGUIUtil.isSimpleJComboBoxItem(__PaperSource_JComboBox, PaperSource,JGUIUtil.NONE, null, null ) ) {
            __PaperSource_JComboBox.select ( PaperSource );
        }
        else {
            if ( (PaperSource == null) || PaperSource.equals("") ) {
                // New command...select the default...
                __PaperSource_JComboBox.select ( 0 );
            }
            else {  // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                "PaperSource parameter \"" + PaperSource + "\".  Select a\n value or Cancel." );
            }
        }
        if ( JGUIUtil.isSimpleJComboBoxItem(__Orientation_JComboBox, Orientation,JGUIUtil.NONE, null, null ) ) {
            __Orientation_JComboBox.select ( Orientation );
        }
        else {
            if ( (Orientation == null) || Orientation.equals("") ) {
                // New command...select the default...
                __Orientation_JComboBox.select ( 0 );
            }
            else {  // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                "Orientation parameter \"" + Orientation + "\".  Select a\n value or Cancel." );
            }
        }
        if ( MarginLeft != null ) {
            __MarginLeft_JTextField.setText ( MarginLeft );
        }
        if ( MarginRight != null ) {
            __MarginRight_JTextField.setText ( MarginRight );
        }
        if ( MarginTop != null ) {
            __MarginTop_JTextField.setText ( MarginTop );
        }
        if ( MarginBottom != null ) {
            __MarginBottom_JTextField.setText ( MarginBottom );
        }
        if ( LinesPerPage != null ) {
            __LinesPerPage_JTextField.setText ( LinesPerPage );
        }
        if ( Header != null ) {
            __Header_JTextField.setText ( Header );
        }
        if ( Footer != null ) {
            __Footer_JTextField.setText ( Footer );
        }
        if ( JGUIUtil.isSimpleJComboBoxItem(__ShowLineCount_JComboBox, ShowLineCount,JGUIUtil.NONE, null, null ) ) {
            __ShowLineCount_JComboBox.select ( ShowLineCount );
        }
        else {
            if ( (ShowLineCount == null) || ShowLineCount.equals("") ) {
                // New command...select the default...
                __ShowLineCount_JComboBox.select ( 0 );
            }
            else {  // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                "ShowLineCount parameter \"" + ShowLineCount + "\".  Select a\n value or Cancel." );
            }
        }
        if ( JGUIUtil.isSimpleJComboBoxItem(__ShowPageCount_JComboBox, ShowPageCount,JGUIUtil.NONE, null, null ) ) {
            __ShowPageCount_JComboBox.select ( ShowPageCount );
        }
        else {
            if ( (ShowPageCount == null) || ShowPageCount.equals("") ) {
                // New command...select the default...
                __ShowPageCount_JComboBox.select ( 0 );
            }
            else {  // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                "ShowPageCount parameter \"" + ShowPageCount + "\".  Select a\n value or Cancel." );
            }
        }
        if ( Pages != null ) {
            __Pages_JTextField.setText ( Pages );
        }
        if ( JGUIUtil.isSimpleJComboBoxItem(__DoubleSided_JComboBox, DoubleSided,JGUIUtil.NONE, null, null ) ) {
            __DoubleSided_JComboBox.select ( DoubleSided );
        }
        else {
            if ( (DoubleSided == null) || DoubleSided.equals("") ) {
                // New command...select the default...
                __DoubleSided_JComboBox.select ( 0 );
            }
            else {  // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                "DoubleSided parameter \"" + DoubleSided + "\".  Select a\n value or Cancel." );
            }
        }
        if ( OutputFile != null ) {
            __OutputFile_JTextField.setText ( OutputFile );
        }
        if ( JGUIUtil.isSimpleJComboBoxItem(__ShowDialog_JComboBox, ShowDialog,JGUIUtil.NONE, null, null ) ) {
            __ShowDialog_JComboBox.select ( ShowDialog );
        }
        else {
            if ( (ShowDialog == null) || ShowDialog.equals("") ) {
                // New command...select the default...
                __ShowDialog_JComboBox.select ( 0 );
            }
            else {  // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                "ShowDialog parameter \"" + ShowDialog + "\".  Select a\n value or Cancel." );
            }
        }
		if ( JGUIUtil.isSimpleJComboBoxItem(__IfNotFound_JComboBox, IfNotFound,JGUIUtil.NONE, null, null ) ) {
			__IfNotFound_JComboBox.select ( IfNotFound );
		}
		else {
            if ( (IfNotFound == null) || IfNotFound.equals("") ) {
				// New command...select the default...
				__IfNotFound_JComboBox.select ( 0 );
			}
			else {	// Bad user command...
				Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
				"IfNotFound parameter \"" +	IfNotFound + "\".  Select a\n value or Cancel." );
			}
		}
	}
	// Regardless, reset the command from the fields.  This is only  visible
	// information that has not been committed in the command.
	InputFile = __InputFile_JTextField.getText().trim();
    PrinterName = __PrinterName_JComboBox.getSelected();
    PaperSize = __PaperSize_JComboBox.getSelected();
    PaperSource = __PaperSource_JComboBox.getSelected();
    Orientation = __Orientation_JComboBox.getSelected();
    MarginLeft = __MarginLeft_JTextField.getText().trim();
    MarginRight = __MarginRight_JTextField.getText().trim();
    MarginTop = __MarginTop_JTextField.getText().trim();
    MarginBottom = __MarginBottom_JTextField.getText().trim();
    LinesPerPage = __LinesPerPage_JTextField.getText().trim();
    Header = __Header_JTextField.getText().trim();
    Footer = __Footer_JTextField.getText().trim();
    ShowLineCount = __ShowLineCount_JComboBox.getSelected();
    ShowPageCount = __ShowPageCount_JComboBox.getSelected();
    Pages = __Pages_JTextField.getText().trim();
    DoubleSided = __DoubleSided_JComboBox.getSelected();
    OutputFile = __OutputFile_JTextField.getText().trim();
    ShowDialog = __ShowDialog_JComboBox.getSelected();
	IfNotFound = __IfNotFound_JComboBox.getSelected();
	PropList props = new PropList ( __command.getCommandName() );
	props.add ( "InputFile=" + InputFile );
	props.add ( "PrinterName=" + PrinterName );
	props.add ( "PaperSize=" + getShortPaperSize(PaperSize) );
	props.add ( "PaperSource=" + PaperSource );
	props.add ( "Orientation=" + Orientation );
	props.add ( "MarginLeft=" + MarginLeft );
	props.add ( "MarginRight=" + MarginRight );
	props.add ( "MarginTop=" + MarginTop );
	props.add ( "MarginBottom=" + MarginBottom );
	props.add ( "LinesPerPage=" + LinesPerPage );
	props.add ( "Header=" + Header );
	props.add ( "Footer=" + Footer );
	props.add ( "ShowLineCount=" + ShowLineCount );
	props.add ( "ShowPageCount=" + ShowPageCount );
	props.add ( "Pages=" + Pages );
	props.add ( "DoubleSided=" + DoubleSided );
	props.add ( "OutputFile=" + OutputFile );
	props.add ( "ShowDialog=" + ShowDialog );
	props.add ( "IfNotFound=" + IfNotFound );
	__command_JTextArea.setText( __command.toString(props).trim() );
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
	// Check the path and determine what the label on the path button should be...
	if ( __path2_JButton != null ) {
		if ( (OutputFile != null) && !OutputFile.isEmpty() ) {
			__path2_JButton.setEnabled ( true );
			File f = new File ( OutputFile );
			if ( f.isAbsolute() ) {
				__path2_JButton.setText ( __RemoveWorkingDirectory );
				__path2_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
            	__path2_JButton.setText ( __AddWorkingDirectory );
            	__path2_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__path2_JButton.setEnabled(false);
		}
	}
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed
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

}
