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
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import java.io.File;
import java.util.List;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Command editor dialog for the WriteTableToKml() command.
*/
@SuppressWarnings("serial")
public class WriteTableToKml_JDialog extends JDialog
implements ActionListener, KeyListener, ItemListener, WindowListener
{

private final String __AddWorkingDirectory = "Add Working Directory";
private final String __RemoveWorkingDirectory = "Remove Working Directory";

private final String __ToAbsolute = "To Absolute";
private final String __ToRelative = "To Relative";

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __browse_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __path_JButton = null;
private SimpleJButton __styleFileBrowse_JButton = null;
private SimpleJButton __styleFilePath_JButton = null;
private WriteTableToKml_Command __command = null;
private String __working_dir = null;
private JTextArea __command_JTextArea=null;
private SimpleJComboBox __TableID_JComboBox = null;
private JTextField __OutputFile_JTextField = null;
private JTabbedPane __main_JTabbedPane = null;
private JTextField __Name_JTextField = null;
private JTextArea __Description_JTextArea = null;
private JTextField __PlacemarkNameColumn_JTextField = null;
private JTextField __PlacemarkDescriptionColumn_JTextField = null;
private JTextField __LongitudeColumn_JTextField = null;
private JTextField __LatitudeColumn_JTextField = null;
private JTextField __ElevationColumn_JTextField = null;
private JTextField __WKTGeometryColumn_JTextField = null;
private JTextArea __GeometryInsert_JTextArea = null;
private JTextArea __StyleInsert_JTextArea = null;
private JTextField __StyleFile_JTextField = null;
private JTextField __StyleUrl_JTextField = null;
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Has user pressed OK to close the dialog.

/**
Dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices list of table identifiers to provide as choices
*/
public WriteTableToKml_JDialog ( JFrame parent, WriteTableToKml_Command command, List<String> tableIDChoices )
{   super(parent, true);
    initialize ( parent, command, tableIDChoices );
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
                __OutputFile_JTextField.setText(path );
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
                Message.printWarning ( 1, "WriteTableToKml_JDialog", "Error converting file to relative path." );
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
{   // Put together a list of parameters to check...
    PropList parameters = new PropList ( "" );
    String TableID = __TableID_JComboBox.getSelected();
    String OutputFile = __OutputFile_JTextField.getText().trim();
    String Name = __Name_JTextField.getText().trim();
    String Description = __Description_JTextArea.getText().trim();
    String PlacemarkNameColumn = __PlacemarkNameColumn_JTextField.getText().trim();
    String PlacemarkDescriptionColumn = __PlacemarkDescriptionColumn_JTextField.getText().trim();
    String LongitudeColumn = __LongitudeColumn_JTextField.getText().trim();
    String LatitudeColumn = __LatitudeColumn_JTextField.getText().trim();
    String ElevationColumn = __ElevationColumn_JTextField.getText().trim();
    String WKTGeometryColumn = __WKTGeometryColumn_JTextField.getText().trim();
    String GeometryInsert = __GeometryInsert_JTextArea.getText().trim();
    String StyleInsert = __StyleInsert_JTextArea.getText().trim();
    String StyleFile = __StyleFile_JTextField.getText().trim();
    String StyleUrl = __StyleUrl_JTextField.getText().trim();

    __error_wait = false;

    if ( TableID.length() > 0 ) {
        parameters.set ( "TableID", TableID );
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
    if ( PlacemarkNameColumn.length() > 0 ) {
        parameters.set ( "PlacemarkNameColumn", PlacemarkNameColumn );
    }
    if ( PlacemarkDescriptionColumn.length() > 0 ) {
        parameters.set ( "PlacemarkDescriptionColumn", PlacemarkDescriptionColumn );
    }
    if ( LongitudeColumn.length() > 0 ) {
        parameters.set ( "LongitudeColumn", LongitudeColumn );
    }
    if ( LatitudeColumn.length() > 0 ) {
        parameters.set ( "LatitudeColumn", LatitudeColumn );
    }
    if ( ElevationColumn.length() > 0 ) {
        parameters.set ( "ElevationColumn", ElevationColumn );
    }
    if ( WKTGeometryColumn.length() > 0 ) {
        parameters.set ( "WKTGeometryColumn", WKTGeometryColumn );
    }
    if ( GeometryInsert.length() > 0 ) {
        parameters.set ( "GeometryInsert", GeometryInsert );
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
{   String TableID = __TableID_JComboBox.getSelected();
    String OutputFile = __OutputFile_JTextField.getText().trim();
    String Name = __Name_JTextField.getText().trim();
    String Description = __Description_JTextArea.getText().replace('\n', ' ').replace('\t', ' ').trim();
    String PlacemarkNameColumn = __PlacemarkNameColumn_JTextField.getText().trim();
    String PlacemarkDescriptionColumn = __PlacemarkDescriptionColumn_JTextField.getText().trim();
    String LongitudeColumn = __LongitudeColumn_JTextField.getText().trim();
    String LatitudeColumn = __LatitudeColumn_JTextField.getText().trim();
    String ElevationColumn = __ElevationColumn_JTextField.getText();
    String WKTGeometryColumn = __WKTGeometryColumn_JTextField.getText();
    String GeometryInsert = __GeometryInsert_JTextArea.getText().trim();
    String StyleInsert = __StyleInsert_JTextArea.getText().replace('\n', ' ').replace('\t', ' ').trim();
    String StyleFile = __StyleFile_JTextField.getText().trim();
    String StyleUrl = __StyleUrl_JTextField.getText().trim();
    __command.setCommandParameter ( "TableID", TableID );
    __command.setCommandParameter ( "OutputFile", OutputFile );
    __command.setCommandParameter ( "Name", Name );
    __command.setCommandParameter ( "Description", Description );
    __command.setCommandParameter ( "PlacemarkNameColumn", PlacemarkNameColumn );
    __command.setCommandParameter ( "PlacemarkDescriptionColumn", PlacemarkDescriptionColumn );
    __command.setCommandParameter ( "LongitudeColumn", LongitudeColumn );
    __command.setCommandParameter ( "LatitudeColumn", LatitudeColumn );
    __command.setCommandParameter ( "ElevationColumn", ElevationColumn );
    __command.setCommandParameter ( "WKTGeometryColumn", WKTGeometryColumn );
    __command.setCommandParameter ( "GeometryInsert", GeometryInsert );
    __command.setCommandParameter ( "StyleInsert", StyleInsert );
    __command.setCommandParameter ( "StyleFile", StyleFile );
    __command.setCommandParameter ( "StyleUrl", StyleUrl );
}

/**
Instantiates the GUI components.
@param parent Frame class instantiating this class.
@param command Command to edit.
@param tableIDChoices list of table identifiers to provide as choices
*/
private void initialize ( JFrame parent, WriteTableToKml_Command command, List<String> tableIDChoices )
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
        "Write a table to a KML format file, which can be used for map integration." ),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Longitude, latitude, elevation, and other attributes are taken from table columns.  The working directory is:" ),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    if ( __working_dir != null ) {
        JGUIUtil.addComponent(main_JPanel, new JLabel ( "  " + __working_dir ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The output filename can be specified using ${Property} notation to utilize global properties."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Table ID:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableID_JComboBox = new SimpleJComboBox ( 12, true );    // Allow edit
    tableIDChoices.add(0,""); // Add blank to ignore table
    __TableID_JComboBox.setData ( tableIDChoices );
    __TableID_JComboBox.addItemListener ( this );
    //__TableID_JComboBox.setMaximumRowCount(tableIDChoices.size());
    JGUIUtil.addComponent(main_JPanel, __TableID_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Required - table to output."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "KML file to write:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputFile_JTextField = new JTextField ( 50 );
    __OutputFile_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __OutputFile_JTextField,
        1, y, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    __browse_JButton = new SimpleJButton ( "Browse", this );
    JGUIUtil.addComponent(main_JPanel, __browse_JButton,
        6, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
    
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

    JGUIUtil.addComponent(gen_JPanel, new JLabel ( "Name:" ),
        0, ++yGen, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Name_JTextField = new JTextField ( "", 30 );
    __Name_JTextField.setToolTipText("Name for layer");
    __Name_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(gen_JPanel, __Name_JTextField,
        1, yGen, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(gen_JPanel, new JLabel ( "Optional - layer name (default=table ID)."),
        3, yGen, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(gen_JPanel, new JLabel ("Description:"), 
        0, ++yGen, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Description_JTextArea = new JTextArea (3,35);
    __Description_JTextArea.setLineWrap ( true );
    __Description_JTextArea.setWrapStyleWord ( true );
    __Description_JTextArea.addKeyListener(this);
    JGUIUtil.addComponent(gen_JPanel, new JScrollPane(__Description_JTextArea),
        1, yGen, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(gen_JPanel, new JLabel ( "Optional - layer description (default=none)."),
        3, yGen, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(gen_JPanel, new JLabel ( "Placemark name column:" ),
        0, ++yGen, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PlacemarkNameColumn_JTextField = new JTextField ( "", 20 );
    __PlacemarkNameColumn_JTextField.setToolTipText("Short phrase to label markers");
    __PlacemarkNameColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(gen_JPanel, __PlacemarkNameColumn_JTextField,
        1, yGen, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(gen_JPanel, new JLabel ( "Optional - short name for map marker."),
        3, yGen, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(gen_JPanel, new JLabel ( "Placemark description column:" ),
        0, ++yGen, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PlacemarkDescriptionColumn_JTextField = new JTextField ( "", 20 );
    __PlacemarkDescriptionColumn_JTextField.setToolTipText("Short phrase to label markers");
    __PlacemarkDescriptionColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(gen_JPanel, __PlacemarkDescriptionColumn_JTextField,
        1, yGen, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(gen_JPanel, new JLabel ( "Optional - longer description for map popup."),
        3, yGen, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Panel for point data in separate columns
    int yPoint = -1;
    JPanel point_JPanel = new JPanel();
    point_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Point Data", point_JPanel );
    
    JGUIUtil.addComponent(point_JPanel, new JLabel (
        "If the data are for a point layer, then spatial information can be specified from separate table columns (below)."),
        0, ++yPoint, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(point_JPanel, new JLabel (
        "Otherwise, specify shape data using parameters in the Geometry Data tab."),
        0, ++yPoint, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(point_JPanel, new JLabel ( "Longitude (X) column:" ),
        0, ++yPoint, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __LongitudeColumn_JTextField = new JTextField ( "", 20 );
    __LongitudeColumn_JTextField.setToolTipText("Longitude is negative if in the Western Hemisphere");
    __LongitudeColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(point_JPanel, __LongitudeColumn_JTextField,
        1, yPoint, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(point_JPanel, new JLabel ( "Required - column containing longitude, decimal degrees."),
        3, yPoint, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(point_JPanel, new JLabel ( "Latitude (Y) column:" ),
        0, ++yPoint, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __LatitudeColumn_JTextField = new JTextField ( "", 20 );
    __LatitudeColumn_JTextField.setToolTipText("Latitude is negative if in the Southern Hemisphere");
    __LatitudeColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(point_JPanel, __LatitudeColumn_JTextField,
        1, yPoint, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(point_JPanel, new JLabel ( "Required - column containing latitude, decimal degrees."),
        3, yPoint, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(point_JPanel, new JLabel ( "Elevation (Z) column:" ),
        0, ++yPoint, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ElevationColumn_JTextField = new JTextField ( "", 20 );
    __ElevationColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(point_JPanel, __ElevationColumn_JTextField,
        1, yPoint, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(point_JPanel, new JLabel ( "Optional - column containing elevation."),
        3, yPoint, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Panel for geometry data in WKT column
    int yGeom = -1;
    JPanel geom_JPanel = new JPanel();
    geom_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Geometry Data", geom_JPanel );
    
    JGUIUtil.addComponent(geom_JPanel, new JLabel (
        "Geometry (shape) data can be specified using Well Known Text (WKT) strings in a table column."),
        0, ++yGeom, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(geom_JPanel, new JLabel (
        "Currently only POINT and POLYGON geometry are recognized but support for other geometry types will be added in the future."),
        0, ++yGeom, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(geom_JPanel, new JLabel (
        "Coordinates in the WKT strings must be geographic (longitude and latitude decimal degrees)."),
        0, ++yGeom, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(geom_JPanel, new JLabel ( "WKT geometry column:" ),
        0, ++yGeom, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __WKTGeometryColumn_JTextField = new JTextField ( "", 20 );
    __WKTGeometryColumn_JTextField.setToolTipText("Longitude is negative if in the Western Hemisphere");
    __WKTGeometryColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(geom_JPanel, __WKTGeometryColumn_JTextField,
        1, yGeom, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(geom_JPanel, new JLabel ( "Required for geometry data - column containing WKT strings."),
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
    
    JGUIUtil.addComponent(kml_JPanel, new JLabel ("Geometry insert:"), 
        0, ++yKml, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __GeometryInsert_JTextArea = new JTextArea (6,35);
    __GeometryInsert_JTextArea.setLineWrap ( true );
    __GeometryInsert_JTextArea.setWrapStyleWord ( true );
    __GeometryInsert_JTextArea.addKeyListener(this);
    JGUIUtil.addComponent(kml_JPanel, new JScrollPane(__GeometryInsert_JTextArea),
        1, yKml, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
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
        "Features will be added in the future to determine the style from a table value column."),
        0, ++yStyle, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(style_JPanel, new JLabel ("Style insert:"), 
        0, ++yStyle, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __StyleInsert_JTextArea = new JTextArea (6,50);
    __StyleInsert_JTextArea.setLineWrap ( true );
    __StyleInsert_JTextArea.setWrapStyleWord ( true );
    __StyleInsert_JTextArea.addKeyListener(this);
    JGUIUtil.addComponent(style_JPanel, new JScrollPane(__StyleInsert_JTextArea),
        1, yStyle, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    yStyle += 6;
    JGUIUtil.addComponent(style_JPanel, new JLabel ( "Style file to insert:" ), 
        0, yStyle, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __StyleFile_JTextField = new JTextField ( 50 );
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
    __StyleUrl_JTextField.setToolTipText("Use #exampleStyleMap to match a style map id");
    __StyleUrl_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(style_JPanel, __StyleUrl_JTextField,
        1, yStyle, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(style_JPanel, new JLabel ( "Optional - style URL for marker (default=pushpin, etc.)."),
        3, yStyle, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __command_JTextArea = new JTextArea ( 4, 50 );
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
        __path_JButton = new SimpleJButton( __RemoveWorkingDirectory, __RemoveWorkingDirectory, this);
        button_JPanel.add ( __path_JButton );
    }
    __cancel_JButton = new SimpleJButton("Cancel", "Cancel", this);
    button_JPanel.add ( __cancel_JButton );
    __ok_JButton = new SimpleJButton("OK", "OK", this);
    button_JPanel.add ( __ok_JButton );

    setTitle ( "Edit " + __command.getCommandName() + "() Command" );
    
    // Refresh the contents...
    checkGUIState();
    refresh ();
    
    setResizable ( true );
    pack();
    JGUIUtil.center( this );
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
{   String routine = "WriteTableToKml_JDialog.refresh";
    String TableID = "";
    String OutputFile = "";
    String Name = "";
    String Description = "";
    String PlacemarkNameColumn = "";
    String PlacemarkDescriptionColumn = "";
    String LongitudeColumn = "";
    String LatitudeColumn = "";
    String ElevationColumn = "";
    String WKTGeometryColumn = "";
    String GeometryInsert = "";
    String StyleInsert = "";
    String StyleFile = "";
    String StyleUrl = "";
    __error_wait = false;
    PropList parameters = null;
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command...
        parameters = __command.getCommandParameters();
        TableID = parameters.getValue ( "TableID" );
        OutputFile = parameters.getValue ( "OutputFile" );
        Name = parameters.getValue ( "Name" );
        Description = parameters.getValue ( "Description" );
        PlacemarkNameColumn = parameters.getValue ( "PlacemarkNameColumn" );
        PlacemarkDescriptionColumn = parameters.getValue ( "PlacemarkDescriptionColumn" );
        LongitudeColumn = parameters.getValue ( "LongitudeColumn" );
        LatitudeColumn = parameters.getValue ( "LatitudeColumn" );
        ElevationColumn = parameters.getValue ( "ElevationColumn" );
        WKTGeometryColumn = parameters.getValue ( "WKTGeometryColumn" );
        GeometryInsert = parameters.getValue ( "GeometryInsert" );
        StyleInsert = parameters.getValue ( "StyleInsert" );
        StyleFile = parameters.getValue ( "StyleFile" );
        StyleUrl = parameters.getValue ( "StyleUrl" );
        if ( TableID == null ) {
            // Select default...
            __TableID_JComboBox.select ( 0 );
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
        if ( OutputFile != null ) {
            __OutputFile_JTextField.setText (OutputFile);
        }
        if ( Name != null ) {
            __Name_JTextField.setText (Name);
        }
        if ( Description != null ) {
            __Description_JTextArea.setText (Description);
        }
        if ( PlacemarkNameColumn != null ) {
            __PlacemarkNameColumn_JTextField.setText (PlacemarkNameColumn);
        }
        if ( PlacemarkDescriptionColumn != null ) {
            __PlacemarkDescriptionColumn_JTextField.setText (PlacemarkDescriptionColumn);
        }
        if ( LongitudeColumn != null ) {
            __LongitudeColumn_JTextField.setText (LongitudeColumn);
        }
        if ( LatitudeColumn != null ) {
            __LatitudeColumn_JTextField.setText (LatitudeColumn);
        }
        if ( ElevationColumn != null ) {
            __ElevationColumn_JTextField.setText (ElevationColumn);
        }
        if ( WKTGeometryColumn != null ) {
            __WKTGeometryColumn_JTextField.setText (WKTGeometryColumn);
        }
        if ( GeometryInsert != null ) {
            __GeometryInsert_JTextArea.setText (GeometryInsert);
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
    }
    // Regardless, reset the command from the fields...
    TableID = __TableID_JComboBox.getSelected();
    OutputFile = __OutputFile_JTextField.getText().trim();
    Name = __Name_JTextField.getText().trim();
    Description = __Description_JTextArea.getText().trim();
    PlacemarkNameColumn = __PlacemarkNameColumn_JTextField.getText().trim();
    PlacemarkDescriptionColumn = __PlacemarkDescriptionColumn_JTextField.getText().trim();
    LongitudeColumn = __LongitudeColumn_JTextField.getText().trim();
    LatitudeColumn = __LatitudeColumn_JTextField.getText().trim();
    ElevationColumn = __ElevationColumn_JTextField.getText().trim();
    WKTGeometryColumn = __WKTGeometryColumn_JTextField.getText().trim();
    GeometryInsert = __GeometryInsert_JTextArea.getText().trim();
    StyleInsert = __StyleInsert_JTextArea.getText().trim();
    StyleFile = __StyleFile_JTextField.getText().trim();
    StyleUrl = __StyleUrl_JTextField.getText().trim();
    parameters = new PropList ( __command.getCommandName() );
    parameters.add ( "TableID=" + TableID );
    parameters.add ( "OutputFile=" + OutputFile );
    parameters.add ( "Name=" + Name );
    parameters.add ( "Description=" + Description );
    parameters.add ( "PlacemarkNameColumn=" + PlacemarkNameColumn );
    parameters.add ( "PlacemarkDescriptionColumn=" + PlacemarkDescriptionColumn );
    parameters.add ( "LongitudeColumn=" + LongitudeColumn );
    parameters.add ( "LatitudeColumn=" + LatitudeColumn );
    parameters.add ( "ElevationColumn=" + ElevationColumn );
    parameters.add ( "WKTGeometryColumn=" + WKTGeometryColumn );
    parameters.add ( "GeometryInsert=" + GeometryInsert );
    parameters.add ( "StyleInsert=" + StyleInsert );
    parameters.add ( "StyleFile=" + StyleFile );
    parameters.add ( "StyleUrl=" + StyleUrl );
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
        }
        else {
            __path_JButton.setText ( __AddWorkingDirectory );
        }
    }
    // Style file
    if ( (StyleFile == null) || (StyleFile.length() == 0) ) {
        if ( __styleFilePath_JButton != null ) {
            __styleFilePath_JButton.setEnabled ( false );
        }
    }
    if ( __styleFilePath_JButton != null ) {
        __styleFilePath_JButton.setEnabled ( true );
        File f = new File ( StyleFile );
        if ( f.isAbsolute() ) {
            __styleFilePath_JButton.setText ( __ToRelative );
        }
        else {
            __styleFilePath_JButton.setText ( __ToAbsolute );
        }
    }
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed
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
{   response ( false );
}

public void windowActivated( WindowEvent evt ){;}
public void windowClosed( WindowEvent evt ){;}
public void windowDeactivated( WindowEvent evt ){;}
public void windowDeiconified( WindowEvent evt ){;}
public void windowIconified( WindowEvent evt ){;}
public void windowOpened( WindowEvent evt ){;}

}