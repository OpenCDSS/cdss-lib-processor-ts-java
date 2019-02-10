// WriteTimeSeriesToKml_JDialog - Command editor dialog for the WriteTimeSeriesToKml() command.

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

package rti.tscommandprocessor.commands.spatial;

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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.io.File;
import java.util.List;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;
import RTi.TS.TSFormatSpecifiersJPanel;
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
Command editor dialog for the WriteTimeSeriesToKml() command.
*/
@SuppressWarnings("serial")
public class WriteTimeSeriesToKml_JDialog extends JDialog
implements ActionListener, DocumentListener, KeyListener, ItemListener, WindowListener
{

private final String __AddWorkingDirectory = "Abs";
private final String __RemoveWorkingDirectory = "Rel";

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __browse_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private SimpleJButton __path_JButton = null;
private WriteTimeSeriesToKml_Command __command = null;
private String __working_dir = null;
private JTextArea __command_JTextArea=null;
private JTabbedPane __main_JTabbedPane = null;
private JTextField __OutputFile_JTextField = null;
private JTextField __Name_JTextField = null;
private JTextArea __Description_JTextArea = null;
private JTextField __LongitudeProperty_JTextField = null;
private JTextField __LatitudeProperty_JTextField = null;
private JTextField __ElevationProperty_JTextField = null;
private JTextField __WKTGeometryProperty_JTextField = null;
private JTextArea __GeometryInsert_JTextArea = null;
private SimpleJButton __styleFileBrowse_JButton = null;
private SimpleJButton __styleFilePath_JButton = null;
private TSFormatSpecifiersJPanel __PlacemarkName_JTextField = null;
private TSFormatSpecifiersJPanel __PlacemarkDescription_JTextField = null;
private JTextArea __StyleInsert_JTextArea = null;
private JTextField __StyleFile_JTextField = null;
private JTextField __StyleUrl_JTextField = null;
private JTextField __Precision_JTextField = null;
private JTextField __OutputStart_JTextField = null;
private JTextField __OutputEnd_JTextField = null;
private SimpleJComboBox __TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox __TSID_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private JTextField __MissingValue_JTextField = null;// Missing value for output
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Has user pressed OK to close the dialog.

private final String __ToAbsolute = "To Absolute";
private final String __ToRelative = "To Relative";

/**
Dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public WriteTimeSeriesToKml_JDialog ( JFrame parent, WriteTimeSeriesToKml_Command command )
{   super(parent, true);
    initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{   Object o = event.getSource();

    if ( o == __browse_JButton ) {
        String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
        JFileChooser fc = null;
        if ( last_directory_selected != null ) {
            fc = JFileChooserFactory.createJFileChooser( last_directory_selected );
        }
        else {
            fc = JFileChooserFactory.createJFileChooser( __working_dir );
        }
        fc.setDialogTitle("Select KML File to Write");
        SimpleFileFilter sff = new SimpleFileFilter("kml", "KML File");
        fc.addChoosableFileFilter(sff);
        
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
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
					Message.printWarning ( 1,"WriteTimeSeriesToKml_JDialog", "Error converting file to relative path." );
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
		HelpViewer.getInstance().showHelp("command", "WriteTimeSeriesToKml");
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
            __OutputFile_JTextField.setText (
            IOUtil.toAbsolutePath(__working_dir, __OutputFile_JTextField.getText() ) );
        }
        else if ( __path_JButton.getText().equals(__RemoveWorkingDirectory) ) {
            try {
                __OutputFile_JTextField.setText (
                IOUtil.toRelativePath ( __working_dir, __OutputFile_JTextField.getText() ) );
            }
            catch ( Exception e ) {
                Message.printWarning ( 1, "WriteTimeSeriesToKml_JDialog", "Error converting file to relative path." );
            }
        }
        refresh ();
    }
    else if ( o == __styleFileBrowse_JButton ) {
        String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
        JFileChooser fc = null;
        if ( last_directory_selected != null ) {
            fc = JFileChooserFactory.createJFileChooser( last_directory_selected );
        }
        else {
            fc = JFileChooserFactory.createJFileChooser( __working_dir );
        }
        fc.setDialogTitle("Select Style File to Insert");
        SimpleFileFilter sff = new SimpleFileFilter("xml", "XML File");
        fc.addChoosableFileFilter(sff);
        
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String directory = fc.getSelectedFile().getParent();
            String filename = fc.getSelectedFile().getName(); 
            String path = fc.getSelectedFile().getPath(); 
    
            if (filename == null || filename.equals("")) {
                return;
            }
    
            if (path != null) {
                __StyleFile_JTextField.setText(path );
                JGUIUtil.setLastFileDialogDirectory(directory );
                refresh();
            }
        }
    }
    else if ( o == __styleFilePath_JButton ) {
        if ( __styleFilePath_JButton.getText().equals(__ToAbsolute) ) {
            __StyleFile_JTextField.setText (
            IOUtil.toAbsolutePath(__working_dir, __StyleFile_JTextField.getText() ) );
        }
        else if ( __styleFilePath_JButton.getText().equals(__ToRelative) ) {
            try {
                __StyleFile_JTextField.setText (
                IOUtil.toRelativePath ( __working_dir, __StyleFile_JTextField.getText() ) );
            }
            catch ( Exception e ) {
                Message.printWarning ( 1, "WriteTableToKml_JDialog", "Error converting file to relative path." );
            }
        }
        refresh ();
    }
}

//Start event handlers for DocumentListener...

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void changedUpdate ( DocumentEvent e )
{   checkGUIState();
    refresh();
}

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void insertUpdate ( DocumentEvent e )
{   checkGUIState();
    refresh();
}

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void removeUpdate ( DocumentEvent e )
{   checkGUIState();
    refresh();
}

// ...End event handlers for DocumentListener

/**
Check the GUI state to make sure that appropriate components are enabled/disabled.
*/
private void checkGUIState ()
{
    String TSList = __TSList_JComboBox.getSelected();
    if ( TSListType.ALL_MATCHING_TSID.equals(TSList) ||
        TSListType.FIRST_MATCHING_TSID.equals(TSList) ||
        TSListType.LAST_MATCHING_TSID.equals(TSList) ) {
        __TSID_JComboBox.setEnabled(true);
        __TSID_JLabel.setEnabled ( true );
    }
    else {
        __TSID_JComboBox.setEnabled(false);
        __TSID_JLabel.setEnabled ( false );
    }
    if ( TSListType.ENSEMBLE_ID.equals(TSList)) {
        __EnsembleID_JComboBox.setEnabled(true);
        __EnsembleID_JLabel.setEnabled ( true );
    }
    else {
        __EnsembleID_JComboBox.setEnabled(false);
        __EnsembleID_JLabel.setEnabled ( false );
    }
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{   // Put together a list of parameters to check...
    PropList parameters = new PropList ( "" );
    String OutputFile = __OutputFile_JTextField.getText().trim();
    String Name = __Name_JTextField.getText().trim();
    String Description = __Description_JTextArea.getText().trim();
    String LongitudeProperty = __LongitudeProperty_JTextField.getText().trim();
    String LatitudeProperty = __LatitudeProperty_JTextField.getText().trim();
    String ElevationProperty = __ElevationProperty_JTextField.getText().trim();
    String WKTGeometryProperty = __WKTGeometryProperty_JTextField.getText().trim();
    String GeometryInsert = __GeometryInsert_JTextArea.getText().trim();
    String PlacemarkName = __PlacemarkName_JTextField.getText().trim();
    String PlacemarkDescription = __PlacemarkDescription_JTextField.getText().trim();
    String StyleInsert = __StyleInsert_JTextArea.getText().trim();
    String StyleFile = __StyleFile_JTextField.getText().trim();
    String StyleUrl = __StyleUrl_JTextField.getText().trim();
    String Precision = __Precision_JTextField.getText().trim();
    String OutputStart = __OutputStart_JTextField.getText().trim();
    String OutputEnd = __OutputEnd_JTextField.getText().trim();
    String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String MissingValue = __MissingValue_JTextField.getText().trim();

    __error_wait = false;
    
    if ( TSList.length() > 0 ) {
        parameters.set ( "TSList", TSList );
    }
    if ( TSID.length() > 0 ) {
        parameters.set ( "TSID", TSID );
    }
    if ( EnsembleID.length() > 0 ) {
        parameters.set ( "EnsembleID", EnsembleID );
    }
    if ( OutputFile.length() > 0 ) {
        parameters.set ( "OutputFile", OutputFile );
    }
    if ( Name.length() > 0 ) {
        parameters.set ( "Name", Name );
    }
    if ( Description.length() > 0 ) {
        parameters.set ( "Description", Description );
    }
    if ( LongitudeProperty.length() > 0 ) {
        parameters.set ( "LongitudeProperty", LongitudeProperty );
    }
    if ( LatitudeProperty.length() > 0 ) {
        parameters.set ( "LatitudeProperty", LatitudeProperty );
    }
    if ( ElevationProperty.length() > 0 ) {
        parameters.set ( "ElevationProperty", ElevationProperty );
    }
    if ( WKTGeometryProperty.length() > 0 ) {
        parameters.set ( "WKTGeometryProperty", WKTGeometryProperty );
    }
    if ( GeometryInsert.length() > 0 ) {
        parameters.set ( "GeometryInsert", GeometryInsert );
    }
    if ( PlacemarkName.length() > 0 ) {
        parameters.set ( "PlacemarkName", PlacemarkName );
    }
    if ( PlacemarkDescription.length() > 0 ) {
        parameters.set ( "PlacemarkDescription", PlacemarkDescription );
    }
    if ( StyleInsert.length() > 0 ) {
        parameters.set ( "StyleInsert", StyleInsert );
    }
    if ( StyleFile.length() > 0 ) {
        parameters.set ( "StyleFile", StyleFile );
    }
    if ( StyleUrl.length() > 0 ) {
        parameters.set ( "StyleUrl", StyleUrl );
    }
    if (Precision.length() > 0) {
        parameters.set("Precision", Precision);
    }
    if ( OutputStart.length() > 0 ) {
        parameters.set ( "OutputStart", OutputStart );
    }
    if ( OutputEnd.length() > 0 ) {
        parameters.set ( "OutputEnd", OutputEnd );
    }
    if ( MissingValue.length() > 0 ) {
        parameters.set ( "MissingValue", MissingValue );
    }
    try {
        // This will warn the user...
        __command.checkCommandParameters ( parameters, null, 1 );
    }
    catch ( Exception e ) {
        // The warning would have been printed in the check code.
        Message.printWarning(2,"",e);
        __error_wait = true;
    }
}

/**
Commit the edits to the command.  In this case the command parameters have
already been checked and no errors were detected.
*/
private void commitEdits ()
{   String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();  
    String OutputFile = __OutputFile_JTextField.getText().trim();
    String Name = __Name_JTextField.getText().trim();
    String Description = __Description_JTextArea.getText().trim();
    String LongitudeProperty = __LongitudeProperty_JTextField.getText().trim();
    String LatitudeProperty = __LatitudeProperty_JTextField.getText().trim();
    String ElevationProperty = __ElevationProperty_JTextField.getText().trim();
    String WKTGeometryProperty = __WKTGeometryProperty_JTextField.getText().trim();
    String GeometryInsert = __GeometryInsert_JTextArea.getText().replace('\n', ' ').replace('\t', ' ').trim();
    String PlacemarkName = __PlacemarkName_JTextField.getText().trim();
    String PlacemarkDescription = __PlacemarkDescription_JTextField.getText().trim();
    String StyleInsert = __StyleInsert_JTextArea.getText().replace('\n', ' ').replace('\t', ' ').trim();
    String StyleFile = __StyleFile_JTextField.getText().trim();
    String StyleUrl = __StyleUrl_JTextField.getText().trim();
    String Precision = __Precision_JTextField.getText().trim();
    String OutputStart = __OutputStart_JTextField.getText().trim();
    String OutputEnd = __OutputEnd_JTextField.getText().trim();
    String MissingValue = __MissingValue_JTextField.getText().trim();
    __command.setCommandParameter ( "TSList", TSList );
    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
    __command.setCommandParameter ( "OutputFile", OutputFile );
    __command.setCommandParameter ( "Name", Name );
    __command.setCommandParameter ( "Description", Description );
    __command.setCommandParameter ( "LongitudeProperty", LongitudeProperty );
    __command.setCommandParameter ( "LatitudeProperty", LatitudeProperty );
    __command.setCommandParameter ( "ElevationProperty", ElevationProperty );
    __command.setCommandParameter ( "WKTGeometryProperty", WKTGeometryProperty );
    __command.setCommandParameter ( "GeometryInsert", GeometryInsert );
    __command.setCommandParameter ( "PlacemarkName", PlacemarkName );
    __command.setCommandParameter ( "PlacemarkDescription", PlacemarkDescription );
    __command.setCommandParameter ( "StyleInsert", StyleInsert );
    __command.setCommandParameter ( "StyleFile", StyleFile );
    __command.setCommandParameter ( "StyleUrl", StyleUrl );
    __command.setCommandParameter ( "Precision", Precision );
    __command.setCommandParameter ( "OutputStart", OutputStart );
    __command.setCommandParameter ( "OutputEnd", OutputEnd );
    __command.setCommandParameter ( "MissingValue", MissingValue );
}

/**
Instantiates the GUI components.
@param parent Frame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, WriteTimeSeriesToKml_Command command )
{   __command = command;
    CommandProcessor processor = __command.getCommandProcessor();
    __working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( processor, __command );

    addWindowListener( this );

    Insets insetsTLBR = new Insets(1,2,1,2);

    JPanel main_JPanel = new JPanel();
    main_JPanel.setLayout( new GridBagLayout() );
    getContentPane().add ( "North", main_JPanel );
    int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Write time series to a KML format file, which can be used for map integration." ),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Longitude, latitude, and elevation are taken from time series properties.  In the future, a table will be used " +
        "to set style information." ),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    if ( __working_dir != null ) {
        JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The working directory is: " + __working_dir ), 
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    __TSList_JComboBox = new SimpleJComboBox(false);
    y = CommandEditorUtil.addTSListToEditorDialogPanel ( this, main_JPanel, __TSList_JComboBox, y );

    __TSID_JLabel = new JLabel ("TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __TSID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __TSID_JComboBox.setToolTipText("Select a time series TSID/alias from the list or specify with ${Property} notation");
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, main_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, y );
    
    __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __EnsembleID_JComboBox.setToolTipText("Select a time series ensemble ID from the list or specify with ${Property} notation");
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
        this, this, main_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, y );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "KML file to write:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputFile_JTextField = new JTextField ( 50 );
    __OutputFile_JTextField.setToolTipText("Specify the output file or specify with ${Property} notation");
    __OutputFile_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __OutputFile_JTextField,
        1, y, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    __browse_JButton = new SimpleJButton ( "...", this );
	__browse_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(main_JPanel, __browse_JButton,
        6, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__path_JButton = new SimpleJButton(__RemoveWorkingDirectory,this);
	    JGUIUtil.addComponent(main_JPanel, __path_JButton,
	    	7, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	}
    
    __main_JTabbedPane = new JTabbedPane ();
    //__main_JTabbedPane.setBorder(
    //    BorderFactory.createTitledBorder ( BorderFactory.createLineBorder(Color.black),
    //    "Specify SQL" ));
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
     
    // Panel for general parameters
    int yGen = -1;
    JPanel gen_JPanel = new JPanel();
    gen_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "General", gen_JPanel );
    
    JGUIUtil.addComponent(gen_JPanel, new JLabel (
        "General parameters specify information for main KML elements."),
        0, ++yGen, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(gen_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++yGen, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(gen_JPanel, new JLabel ( "Name:" ),
        0, ++yGen, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Name_JTextField = new JTextField ( "", 30 );
    __Name_JTextField.setToolTipText("Name for layer");
    __Name_JTextField.setToolTipText("Specify the layer name, can use ${Property} notation");
    __Name_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(gen_JPanel, __Name_JTextField,
        1, yGen, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(gen_JPanel, new JLabel ( "Optional - layer name (default=none)."),
        3, yGen, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(gen_JPanel, new JLabel ("Description:"), 
        0, ++yGen, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Description_JTextArea = new JTextArea (3,35);
    __Description_JTextArea.setToolTipText("Specify the layer description, can use ${Property} notation");
    __Description_JTextArea.setLineWrap ( true );
    __Description_JTextArea.setWrapStyleWord ( true );
    __Description_JTextArea.addKeyListener(this);
    JGUIUtil.addComponent(gen_JPanel, new JScrollPane(__Description_JTextArea),
        1, yGen, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(gen_JPanel, new JLabel ( "Optional - layer description (default=none)."),
        3, yGen, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Panel for point data in separate columns
    int yPoint = -1;
    JPanel point_JPanel = new JPanel();
    point_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Point Data", point_JPanel );
    
    JGUIUtil.addComponent(point_JPanel, new JLabel (
        "If the time series are associated with a point layer, then spatial information can be specified from time series properties."),
        0, ++yPoint, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(point_JPanel, new JLabel (
        "Otherwise, specify geometry data using parameters in the Geometry Data tab."),
        0, ++yPoint, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(point_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
          0, ++yPoint, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(point_JPanel, new JLabel ( "Longitude property:" ),
        0, ++yPoint, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __LongitudeProperty_JTextField = new JTextField ( "", 20 );
    __LongitudeProperty_JTextField.setToolTipText("Specify the longitude property, can use ${Property} notation");
    __LongitudeProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(point_JPanel, __LongitudeProperty_JTextField,
        1, yPoint, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(point_JPanel, new JLabel ( "Required - time series property containing longitude."),
        3, yPoint, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(point_JPanel, new JLabel ( "Latitude property:" ),
        0, ++yPoint, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __LatitudeProperty_JTextField = new JTextField ( "", 20 );
    __LatitudeProperty_JTextField.setToolTipText("Specify the latitude property, can use ${Property} notation");
    __LatitudeProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(point_JPanel, __LatitudeProperty_JTextField,
        1, yPoint, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(point_JPanel, new JLabel ( "Required - time series property containing latitude."),
        3, yPoint, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(point_JPanel, new JLabel ( "Elevation property:" ),
        0, ++yPoint, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ElevationProperty_JTextField = new JTextField ( "", 20 );
    __ElevationProperty_JTextField.setToolTipText("Specify the elevation property, can use ${Property} notation");
    __ElevationProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(point_JPanel, __ElevationProperty_JTextField,
        1, yPoint, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(point_JPanel, new JLabel ( "Optional - time series property containing elevation (default=0)."),
        3, yPoint, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Panel for geometry data in WKT column
    int yGeom = -1;
    JPanel geom_JPanel = new JPanel();
    geom_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Geometry Data", geom_JPanel );
    
    JGUIUtil.addComponent(geom_JPanel, new JLabel (
        "Geometry (shape) data can be specified using Well Known Text (WKT) strings in a time series property."),
        0, ++yGeom, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(geom_JPanel, new JLabel (
        "Currently only POINT and POLYGON geometry are recognized but support for other geometry types will be added in the future."),
        0, ++yGeom, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(geom_JPanel, new JLabel (
        "Coordinates in the WKT strings must be geographic (longitude and latitude decimal degrees)."),
        0, ++yGeom, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(geom_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++yGeom, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(geom_JPanel, new JLabel ( "WKT geometry property:" ),
        0, ++yGeom, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __WKTGeometryProperty_JTextField = new JTextField ( "", 20 );
    __WKTGeometryProperty_JTextField.setToolTipText("Specify the WKT geometry property, can use ${Property} notation");
    __WKTGeometryProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(geom_JPanel, __WKTGeometryProperty_JTextField,
        1, yGeom, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(geom_JPanel, new JLabel ( "Optional - time series property WKT geometry."),
        3, yGeom, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Panel for KML inserts
    int yKml = -1;
    JPanel kml_JPanel = new JPanel();
    kml_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "KML Inserts", kml_JPanel );
    
    JGUIUtil.addComponent(kml_JPanel, new JLabel (
        "KML files allow for many properties to be specified to configure the data."),
        0, ++yKml, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(kml_JPanel, new JLabel (
        "The GeometryInsert command parameter value will be inserted within the <Point>, <Polygon>, etc. data element."),
        0, ++yKml, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(kml_JPanel, new JLabel (
        "Refer to the KML reference for information (https://developers.google.com/kml/documentation/kmlreference)."),
        0, ++yKml, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(kml_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++yKml, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(kml_JPanel, new JLabel ("Geometry insert:"), 
        0, ++yKml, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __GeometryInsert_JTextArea = new JTextArea (6,35);
    __GeometryInsert_JTextArea.setToolTipText("Specify the geometry insert, can use ${Property} notation");
    __GeometryInsert_JTextArea.setLineWrap ( true );
    __GeometryInsert_JTextArea.setWrapStyleWord ( true );
    __GeometryInsert_JTextArea.addKeyListener(this);
    JGUIUtil.addComponent(kml_JPanel, new JScrollPane(__GeometryInsert_JTextArea),
        1, yKml, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // Panel for style information
    int yStyle = -1;
    JPanel style_JPanel = new JPanel();
    style_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Marker Styles", style_JPanel );
    
    JGUIUtil.addComponent(style_JPanel, new JLabel (
        "Marker styles control how map layer features are symbolized (colors, etc.) and interact (mouse-over highlight, etc.)."),
        0, ++yStyle, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(style_JPanel, new JLabel (
        "Marker style definitions can be defined by inserting XML text or specifying a file to insert."),
        0, ++yStyle, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(style_JPanel, new JLabel (
        "The URL to a style map is then specified for the layer (currently all features in the layer will have the same style)."),
        0, ++yStyle, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(style_JPanel, new JLabel (
        "In the future features will be enabled to lookup the market style from time series values or statistic)."),
        0, ++yStyle, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(style_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++yStyle, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(style_JPanel, new JLabel("Placemark name:"),
        0, ++yStyle, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PlacemarkName_JTextField = new TSFormatSpecifiersJPanel(30);
    __PlacemarkName_JTextField.setToolTipText(
        "Use %L for location, %T for data type, %I for interval, ${ts:property} for time series property.");
    __PlacemarkName_JTextField.addKeyListener ( this );
    __PlacemarkName_JTextField.getDocument().addDocumentListener(this);
    JGUIUtil.addComponent(style_JPanel, __PlacemarkName_JTextField,
        1, yStyle, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(style_JPanel,
        new JLabel ("Optional - use %L for location, ${ts:property}, etc."),
        3, yStyle, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(style_JPanel, new JLabel("Placemark description:"),
        0, ++yStyle, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PlacemarkDescription_JTextField = new TSFormatSpecifiersJPanel(30);
    __PlacemarkDescription_JTextField.setToolTipText(
        "Use %L for location, %T for data type, %I for interval, ${ts:property} for time series property.");
    __PlacemarkDescription_JTextField.addKeyListener ( this );
    __PlacemarkDescription_JTextField.getDocument().addDocumentListener(this);
    JGUIUtil.addComponent(style_JPanel, __PlacemarkDescription_JTextField,
        1, yStyle, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(style_JPanel,
        new JLabel ("Optional - use %L for location, ${ts:property}, etc."),
        3, yStyle, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(style_JPanel, new JLabel ("Style insert:"), 
        0, ++yStyle, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __StyleInsert_JTextArea = new JTextArea (6,35);
    __StyleInsert_JTextArea.setToolTipText("Specify the style insert, can use ${Property} notation");
    __StyleInsert_JTextArea.setLineWrap ( true );
    __StyleInsert_JTextArea.setWrapStyleWord ( true );
    __StyleInsert_JTextArea.addKeyListener(this);
    JGUIUtil.addComponent(style_JPanel, new JScrollPane(__StyleInsert_JTextArea),
        1, yStyle, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    yStyle += 6;
    JGUIUtil.addComponent(style_JPanel, new JLabel ( "Style file to insert:" ), 
        0, yStyle, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __StyleFile_JTextField = new JTextField ( 35 );
    __StyleFile_JTextField.setToolTipText("Specify the style file or specify with ${Property} notation");
    __StyleFile_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(style_JPanel, __StyleFile_JTextField,
        1, yStyle, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    __styleFileBrowse_JButton = new SimpleJButton ( "Browse", this );
    JGUIUtil.addComponent(style_JPanel, __styleFileBrowse_JButton,
        6, yStyle, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
    __styleFilePath_JButton = new SimpleJButton ( __ToRelative, this );
    JGUIUtil.addComponent(style_JPanel, __styleFilePath_JButton,
        7, yStyle, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
    __styleFilePath_JButton.setToolTipText("Change style file path to/from absolute/relative path.");

    JGUIUtil.addComponent(style_JPanel, new JLabel ( "StyleUrl:" ),
        0, ++yStyle, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __StyleUrl_JTextField = new JTextField ( "", 20 );
    __StyleUrl_JTextField.setToolTipText("Use #exampleStyleMap to match a style map id, can use ${Property} notation.");
    __StyleUrl_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(style_JPanel, __StyleUrl_JTextField,
        1, yStyle, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(style_JPanel, new JLabel ( "Optional - style URL for marker (default=pushpin, etc.)."),
        3, yStyle, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Panel for time series data in separate columns
    int yData = -1;
    JPanel data_JPanel = new JPanel();
    data_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Time Series Data", data_JPanel );
    
    JGUIUtil.addComponent(data_JPanel, new JLabel (
        "Currently time series data are not output. In the future the KML timestamp feature may be implemented.."),
        0, ++yData, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(data_JPanel, new JLabel (
        "Enter date/times to a precision appropriate for output time series."),
        0, ++yData, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(data_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++yData, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        
    JGUIUtil.addComponent(data_JPanel, new JLabel ( "Output precision:" ),
        0, ++yData, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Precision_JTextField = new JTextField ( "", 20 );
    __Precision_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(data_JPanel, __Precision_JTextField,
        1, yData, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(data_JPanel, new JLabel ( "Optional - digits after decimal (default=4)."),
        3, yData, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(data_JPanel, new JLabel ( "Missing value:" ),
        0, ++yData, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MissingValue_JTextField = new JTextField ( "", 20 );
    __MissingValue_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(data_JPanel, __MissingValue_JTextField,
        1, yData, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(data_JPanel, new JLabel (
        "Optional - value to write for missing data (default=initial missing value)."),
        3, yData, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(data_JPanel, new JLabel ("Output start:"), 
        0, ++yData, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputStart_JTextField = new JTextField (20);
    __OutputStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(data_JPanel, __OutputStart_JTextField,
        1, yData, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(data_JPanel, new JLabel (
        "Optional - override the global output start (default=write all data)."),
        3, yData, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(data_JPanel, new JLabel ( "Output end:"), 
        0, ++yData, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputEnd_JTextField = new JTextField (20);
    __OutputEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(data_JPanel, __OutputEnd_JTextField,
        1, yData, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(data_JPanel, new JLabel (
        "Optional - override the global output end (default=write all data)."),
        3, yData, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __command_JTextArea = new JTextArea ( 4, 50 );
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

    __ok_JButton = new SimpleJButton("OK", "OK", this);
	__ok_JButton.setToolTipText("Save changes to command");
    button_JPanel.add ( __ok_JButton );
    __cancel_JButton = new SimpleJButton("Cancel", "Cancel", this);
    button_JPanel.add ( __cancel_JButton );
	__cancel_JButton.setToolTipText("Cancel without saving changes to command");
	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");

    setTitle ( "Edit " + __command.getCommandName() + " Command" );
    
    // Refresh the contents...
    checkGUIState();
    refresh ();
    
    pack();
    JGUIUtil.center( this );
    setResizable ( false );
    super.setVisible( true );
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged (ItemEvent e)
{   checkGUIState();
    refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{   int code = event.getKeyCode();

    if ( code == KeyEvent.VK_ENTER ) {
        refresh ();
        checkInput();
        if ( !__error_wait ) {
            response ( true );
        }
    }
}

public void keyReleased ( KeyEvent event )
{   refresh();
}

public void keyTyped ( KeyEvent event )
{
}

/**
Indicate if the user pressed OK (cancel otherwise).
@return true if the edits were committed, false if the user canceled.
*/
public boolean ok ()
{   return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{   String routine = "WriteTimeSeriesToKml_JDialog.refresh";
    String OutputFile = "";
    String Name = "";
    String Description = "";
    String LongitudeProperty = "";
    String LatitudeProperty = "";
    String ElevationProperty = "";
    String WKTGeometryProperty = "";
    String GeometryInsert = "";
    String PlacemarkName = "";
    String PlacemarkDescription = "";
    String StyleInsert = "";
    String StyleFile = "";
    String StyleUrl = "";
    String Precision = "";
    String MissingValue = "";
    String OutputStart = "";
    String OutputEnd = "";
    String TSList = "";
    String TSID = "";
    String EnsembleID = "";
    __error_wait = false;
    PropList parameters = null;
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command...
        parameters = __command.getCommandParameters();
        OutputFile = parameters.getValue ( "OutputFile" );
        Name = parameters.getValue ( "Name" );
        Description = parameters.getValue ( "Description" );
        LongitudeProperty = parameters.getValue ( "LongitudeProperty" );
        LatitudeProperty = parameters.getValue ( "LatitudeProperty" );
        ElevationProperty = parameters.getValue ( "ElevationProperty" );
        WKTGeometryProperty = parameters.getValue ( "WKTGeometryProperty" );
        GeometryInsert = parameters.getValue ( "GeometryInsert" );
        PlacemarkName = parameters.getValue ( "PlacemarkName" );
        PlacemarkDescription = parameters.getValue ( "PlacemarkDescription" );
        StyleInsert = parameters.getValue ( "StyleInsert" );
        StyleFile = parameters.getValue ( "StyleFile" );
        StyleUrl = parameters.getValue ( "StyleUrl" );
        Precision = parameters.getValue("Precision");
        MissingValue = parameters.getValue("MissingValue");
        OutputStart = parameters.getValue ( "OutputStart" );
        OutputEnd = parameters.getValue ( "OutputEnd" );
        TSList = parameters.getValue ( "TSList" );
        TSID = parameters.getValue ( "TSID" );
        EnsembleID = parameters.getValue ( "EnsembleID" );
        if ( OutputFile != null ) {
            __OutputFile_JTextField.setText (OutputFile);
        }
        if ( Name != null ) {
            __Name_JTextField.setText (Name);
        }
        if ( Description != null ) {
            __Description_JTextArea.setText (Description);
        }
        if ( LongitudeProperty != null ) {
            __LongitudeProperty_JTextField.setText (LongitudeProperty);
        }
        if ( LatitudeProperty != null ) {
            __LatitudeProperty_JTextField.setText (LatitudeProperty);
        }
        if ( ElevationProperty != null ) {
            __ElevationProperty_JTextField.setText (ElevationProperty);
        }
        if ( WKTGeometryProperty != null ) {
            __WKTGeometryProperty_JTextField.setText (WKTGeometryProperty);
        }
        if ( GeometryInsert != null ) {
            __GeometryInsert_JTextArea.setText (GeometryInsert);
        }
        if ( PlacemarkName != null ) {
            __PlacemarkName_JTextField.setText (PlacemarkName);
        }
        if ( PlacemarkDescription != null ) {
            __PlacemarkDescription_JTextField.setText (PlacemarkDescription);
        }
        if ( StyleInsert != null ) {
            __StyleInsert_JTextArea.setText (StyleInsert);
        }
        if ( StyleFile != null ) {
            __StyleFile_JTextField.setText (StyleFile);
        }
        if ( StyleUrl != null ) {
            __StyleUrl_JTextField.setText (StyleUrl);
        }
        if ( Precision != null ) {
            __Precision_JTextField.setText ( Precision );
        }
        if ( MissingValue != null ) {
            __MissingValue_JTextField.setText ( MissingValue );
        }
        if ( OutputStart != null ) {
            __OutputStart_JTextField.setText (OutputStart);
        }
        if ( OutputEnd != null ) {
            __OutputEnd_JTextField.setText (OutputEnd);
        }
        if ( TSList == null ) {
            // Select default...
            __TSList_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __TSList_JComboBox,TSList, JGUIUtil.NONE, null, null ) ) {
                __TSList_JComboBox.select ( TSList );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nTSList value \"" + TSList +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( JGUIUtil.isSimpleJComboBoxItem( __TSID_JComboBox, TSID, JGUIUtil.NONE, null, null ) ) {
                __TSID_JComboBox.select ( TSID );
        }
        else {
            // Automatically add to the list after the blank...
            if ( (TSID != null) && (TSID.length() > 0) ) {
                __TSID_JComboBox.insertItemAt ( TSID, 1 );
                // Select...
                __TSID_JComboBox.select ( TSID );
            }
            else {
                // Select the blank...
                __TSID_JComboBox.select ( 0 );
            }
        }
        if ( EnsembleID == null ) {
            // Select default...
            __EnsembleID_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __EnsembleID_JComboBox,EnsembleID, JGUIUtil.NONE, null, null ) ) {
                __EnsembleID_JComboBox.select ( EnsembleID );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nEnsembleID value \"" + EnsembleID +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
    }
    // Regardless, reset the command from the fields...
    OutputFile = __OutputFile_JTextField.getText().trim();
    Name = __Name_JTextField.getText().trim();
    Description = __Description_JTextArea.getText().trim();
    LongitudeProperty = __LongitudeProperty_JTextField.getText().trim();
    LatitudeProperty = __LatitudeProperty_JTextField.getText().trim();
    ElevationProperty = __ElevationProperty_JTextField.getText().trim();
    WKTGeometryProperty = __WKTGeometryProperty_JTextField.getText().trim();
    GeometryInsert = __GeometryInsert_JTextArea.getText().trim();
    PlacemarkName = __PlacemarkName_JTextField.getText().trim();
    PlacemarkDescription = __PlacemarkDescription_JTextField.getText().trim();
    StyleInsert = __StyleInsert_JTextArea.getText().trim();
    StyleFile = __StyleFile_JTextField.getText().trim();
    StyleUrl = __StyleUrl_JTextField.getText().trim();
    Precision = __Precision_JTextField.getText().trim();
    MissingValue = __MissingValue_JTextField.getText().trim();
    OutputStart = __OutputStart_JTextField.getText().trim();
    OutputEnd = __OutputEnd_JTextField.getText().trim();
    TSList = __TSList_JComboBox.getSelected();
    TSID = __TSID_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
    parameters = new PropList ( __command.getCommandName() );
    parameters.add ( "TSList=" + TSList );
    parameters.add ( "TSID=" + TSID );
    parameters.add ( "EnsembleID=" + EnsembleID );
    parameters.add ( "OutputFile=" + OutputFile );
    parameters.add ( "Name=" + Name );
    parameters.add ( "Description=" + Description );
    parameters.add ( "LongitudeProperty=" + LongitudeProperty );
    parameters.add ( "LatitudeProperty=" + LatitudeProperty );
    parameters.add ( "ElevationProperty=" + ElevationProperty );
    parameters.add ( "WKTGeometryProperty=" + WKTGeometryProperty );
    parameters.add ( "GeometryInsert=" + GeometryInsert );
    parameters.add ( "PlacemarkName=" + PlacemarkName );
    parameters.add ( "PlacemarkDescription=" + PlacemarkDescription );
    parameters.add ( "StyleInsert=" + StyleInsert );
    parameters.add ( "StyleFile=" + StyleFile );
    parameters.add ( "StyleUrl=" + StyleUrl );
    parameters.add ( "Precision=" + Precision );
    parameters.add ( "MissingValue=" + MissingValue );
    parameters.add ( "OutputStart=" + OutputStart );
    parameters.add ( "OutputEnd=" + OutputEnd );
    __command_JTextArea.setText( __command.toString ( parameters ) );
    if ( (OutputFile == null) || (OutputFile.length() == 0) ) {
        if ( __path_JButton != null ) {
            __path_JButton.setEnabled ( false );
        }
    }
    if ( __path_JButton != null ) {
        __path_JButton.setEnabled ( true );
        File f = new File ( OutputFile );
        if ( f.isAbsolute() ) {
            __path_JButton.setText ( __RemoveWorkingDirectory );
			__path_JButton.setToolTipText("Change path to relative to command file");
        }
        else {
            __path_JButton.setText ( __AddWorkingDirectory );
			__path_JButton.setToolTipText("Change path to absolute");
        }
    }
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed and the dialog is closed.
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
{   response ( false );
}

public void windowActivated( WindowEvent evt ){;}
public void windowClosed( WindowEvent evt ){;}
public void windowDeactivated( WindowEvent evt ){;}
public void windowDeiconified( WindowEvent evt ){;}
public void windowIconified( WindowEvent evt ){;}
public void windowOpened( WindowEvent evt ){;}

}
