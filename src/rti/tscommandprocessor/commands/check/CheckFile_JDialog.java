// CheckFile_JDialog - editor for CheckFile command

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

package rti.tscommandprocessor.commands.check;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

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

import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

@SuppressWarnings("serial")
public class CheckFile_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{
private final String __AddWorkingDirectory = "Abs";
private final String __RemoveWorkingDirectory = "Rel";

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private SimpleJButton __path_JButton = null;
private CheckFile_Command __command = null;
private JTextArea __command_JTextArea=null;
private JTabbedPane __main_JTabbedPane = null;
private JTextField	__InputFile_JTextField = null;
private SimpleJButton __browse_JButton = null;
private SimpleJComboBox	__IfNotFound_JComboBox =null;
private SimpleJComboBox __Statistic_JComboBox = null;
private JTextField __SearchPattern_JTextField = null;
private SimpleJComboBox __TableID_JComboBox = null;
private JTextField __TableFilenameColumn_JTextField = null;
private JTextField __TableStatisticColumn_JTextField = null;
private SimpleJComboBox __CheckCriteria_JComboBox = null;
private JTextField __CheckValue1_JTextField = null;
private JTextField __CheckValue2_JTextField = null;
private SimpleJComboBox __IfCriteriaMet_JComboBox = null;
private JTextField __ProblemType_JTextField = null; // Field for problem type
private JTextField __PropertyName_JTextField = null;
private JTextField __PropertyValue_JTextField = null;
//private SimpleJComboBox __Action_JComboBox = null;
private String __working_dir = null;
private boolean __error_wait = false; // Is there an error to be cleared up or Cancel?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK button has been pressed.

/**
Command dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public CheckFile_JDialog ( JFrame parent, CheckFile_Command command,
    List<String> tableIDChoices )
{	super(parent, true);
	initialize ( parent, command, tableIDChoices );
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
		else {	fc = JFileChooserFactory.createJFileChooser(__working_dir );
		}
		fc.setDialogTitle( "Select File to Remove");
		
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
					Message.printWarning ( 1,"CheckFile_JDialog", "Error converting file to relative path." );
				}
				JGUIUtil.setLastFileDialogDirectory(directory);
				refresh();
			}
		}
	}
	else if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "CheckFile");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
	PropList parameters = new PropList ( "" );
    String InputFile = __InputFile_JTextField.getText().trim();
	String IfNotFound = __IfNotFound_JComboBox.getSelected();
    String Statistic = __Statistic_JComboBox.getSelected();
    String SearchPattern = __SearchPattern_JTextField.getText().trim();
    String TableID = __TableID_JComboBox.getSelected();
    String TableFilenameColumn = __TableFilenameColumn_JTextField.getText().trim();
    String TableStatisticColumn = __TableStatisticColumn_JTextField.getText().trim();
    String CheckCriteria = __CheckCriteria_JComboBox.getSelected();
	String CheckValue1 = __CheckValue1_JTextField.getText().trim();
	String CheckValue2 = __CheckValue2_JTextField.getText().trim();
	String IfCriteriaMet = __IfCriteriaMet_JComboBox.getSelected();
	String ProblemType = __ProblemType_JTextField.getText().trim();
    String PropertyName = __PropertyName_JTextField.getText().trim();
    String PropertyValue = __PropertyValue_JTextField.getText().trim();
    //String Action = __Action_JComboBox.getSelected();
	__error_wait = false;

	if ( InputFile.length() > 0 ) {
		parameters.set ( "InputFile", InputFile );
	}
	if ( IfNotFound.length() > 0 ) {
		parameters.set ( "IfNotFound", IfNotFound );
	}
    if ( Statistic.length() > 0 ) {
        parameters.set ( "Statistic", Statistic );
    }
    if ( SearchPattern.length() > 0 ) {
        parameters.set ( "SearchPattern", SearchPattern );
    }
    if ( TableID.length() > 0 ) {
        parameters.set ( "TableID", TableID );
    }
    if ( TableFilenameColumn.length() > 0 ) {
        parameters.set ( "TableFilenameColumn", TableFilenameColumn );
    }
    if ( TableStatisticColumn.length() > 0 ) {
        parameters.set ( "TableStatisticColumn", TableStatisticColumn );
    }
    if ( CheckCriteria.length() > 0 ) {
        parameters.set ( "CheckCriteria", CheckCriteria );
    }
	if ( CheckValue1.length() > 0 ) {
		parameters.set ( "CheckValue1", CheckValue1 );
	}
    if ( CheckValue2.length() > 0 ) {
        parameters.set ( "CheckValue2", CheckValue2 );
    }
    if ( IfCriteriaMet.length() > 0 ) {
        parameters.set ( "IfCriteriaMet", IfCriteriaMet );
    }
	if ( ProblemType.length() > 0 ) {
		parameters.set ( "ProblemType", ProblemType );
	}
    if ( PropertyName.length() > 0 ) {
        parameters.set ( "PropertyName", PropertyName );
    }
    if ( PropertyValue.length() > 0 ) {
        parameters.set ( "PropertyValue", PropertyValue );
    }
    //if ( Action.length() > 0 ) {
    //    parameters.set ( "Action", Action );
    //}
	try {
	    // This will warn the user...
		__command.checkCommandParameters ( parameters, null, 1 );
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
private void commitEdits () {	
    String InputFile = __InputFile_JTextField.getText().trim();
	String IfNotFound = __IfNotFound_JComboBox.getSelected();
    String Statistic = __Statistic_JComboBox.getSelected();
    String SearchPattern = __SearchPattern_JTextField.getText().trim();
    String TableID = __TableID_JComboBox.getSelected();
    String TableFilenameColumn = __TableFilenameColumn_JTextField.getText().trim();
    String TableStatisticColumn = __TableStatisticColumn_JTextField.getText().trim();
    String CheckCriteria = __CheckCriteria_JComboBox.getSelected();
	String CheckValue1 = __CheckValue1_JTextField.getText().trim();
	String CheckValue2 = __CheckValue2_JTextField.getText().trim();
	String IfCriteriaMet = __IfCriteriaMet_JComboBox.getSelected();
	String ProblemType = __ProblemType_JTextField.getText().trim();
    String PropertyName = __PropertyName_JTextField.getText().trim();
	String PropertyValue = __PropertyValue_JTextField.getText().trim();
    //String Action = __Action_JComboBox.getSelected();
    __command.setCommandParameter ( "InputFile", InputFile );
    __command.setCommandParameter ( "IfNotFound", IfNotFound );
    __command.setCommandParameter ( "Statistic", Statistic );
    __command.setCommandParameter ( "SearchPattern", SearchPattern );
    __command.setCommandParameter ( "TableID", TableID );
    __command.setCommandParameter ( "TableFilenameColumn", TableFilenameColumn );
    __command.setCommandParameter ( "TableStatisticColumn", TableStatisticColumn );
    __command.setCommandParameter ( "CheckCriteria", CheckCriteria );
	__command.setCommandParameter ( "CheckValue1", CheckValue1 );
	__command.setCommandParameter ( "CheckValue2", CheckValue2 );
	__command.setCommandParameter ( "IfCriteriaMet", IfCriteriaMet );
	__command.setCommandParameter ( "ProblemType", ProblemType );
    __command.setCommandParameter ( "PropertyName", PropertyName );
	__command.setCommandParameter ( "PropertyValue", PropertyValue );
	//__command.setCommandParameter ( "Action", Action );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param title Dialog title.
@param command The command to edit.
*/
private void initialize ( JFrame parent, CheckFile_Command command, List<String> tableIDChoices )
{	__command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Check a file's statistics against criteria."),
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "A warning will be generated if the statistic matches the specified condition(s)." ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("File to check:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputFile_JTextField = new JTextField ( 50 );
	__InputFile_JTextField.setToolTipText("Specify the file to check or use ${Property} notation");
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
		"Optional - action if file not found (default=" + __command._Warn + ")"), 
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    __main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
     
    // Panel for statistic
    int yStat = -1;
    JPanel stat_JPanel = new JPanel();
    stat_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Statistic", stat_JPanel );
    
    JGUIUtil.addComponent(stat_JPanel, new JLabel (
        "The following parameters define how to compute the statistic."),
        0, ++yStat, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(stat_JPanel, new JLabel (
        "Currently minimum sample size and number of missing allowed cannot be specified."),
        0, ++yStat, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(stat_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yStat, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(stat_JPanel, new JLabel ( "Statistic to calculate:" ), 
        0, ++yStat, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Statistic_JComboBox = new SimpleJComboBox ( 12, false ); // Do not allow edit
    __Statistic_JComboBox.setData ( this.__command.getStatisticChoicesAsStrings() );
    __Statistic_JComboBox.addItemListener ( this );
    //__Statistic_JComboBox.setMaximumRowCount(statisticChoices.size());
    JGUIUtil.addComponent(stat_JPanel, __Statistic_JComboBox,
        1, yStat, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(stat_JPanel, new JLabel(
        "Required - may require other parameters."), 
        3, yStat, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(stat_JPanel, new JLabel ( "Search pattern:" ), 
        0, ++yStat, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SearchPattern_JTextField = new JTextField ( 30 );
    __SearchPattern_JTextField.setToolTipText("Search pattern should use * at start and/or end if necessary.");
    __SearchPattern_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(stat_JPanel, __SearchPattern_JTextField,
        1, yStat, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(stat_JPanel, new JLabel(
        "Optional - use with PatternCount statistic."), 
        3, yStat, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Panel for table output
    int yOut = -1;
    JPanel out_JPanel = new JPanel();
    out_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Output", out_JPanel );
    
    JGUIUtil.addComponent(out_JPanel, new JLabel (
        "The statistic that is calculated can be saved in a table containing columns for the file name and statistic value."),
        0, ++yOut, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(out_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yOut, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(out_JPanel, new JLabel ( "Table ID for output:" ), 
        0, ++yOut, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableID_JComboBox = new SimpleJComboBox ( 12, true );    // Allow edit
    tableIDChoices.add(0,""); // Add blank to ignore table
    __TableID_JComboBox.setData ( tableIDChoices );
    __TableID_JComboBox.addItemListener ( this );
    //__TableID_JComboBox.setMaximumRowCount(tableIDChoices.size());
    JGUIUtil.addComponent(out_JPanel, __TableID_JComboBox,
        1, yOut, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(out_JPanel, new JLabel(
        "Optional - table to save the statistic."), 
        3, yOut, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(out_JPanel, new JLabel ( "Table file name column:" ), 
        0, ++yOut, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableFilenameColumn_JTextField = new JTextField ( 10 );
    __TableFilenameColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(out_JPanel, __TableFilenameColumn_JTextField,
        1, yOut, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(out_JPanel, new JLabel( "Required if using table - column name for file name."), 
        3, yOut, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(out_JPanel, new JLabel ( "Table statistic column:" ), 
        0, ++yOut, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableStatisticColumn_JTextField = new JTextField ( 10 );
    __TableStatisticColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(out_JPanel, __TableStatisticColumn_JTextField,
        1, yOut, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(out_JPanel, new JLabel(
        "Required if using table - column name for statistic."), 
        3, yOut, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for check and actions
    int yCheck = -1;
    JPanel check_JPanel = new JPanel();
    check_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Check Criteria and Actions", check_JPanel );
    
    JGUIUtil.addComponent(check_JPanel, new JLabel (
        "The following parameters are used to check the statistic value against a criteria."),
        0, ++yCheck, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel (
        "If the statistic value matches the criteria, then an action can be taken and a property can be set."),
        0, ++yCheck, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yCheck, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(check_JPanel, new JLabel ( "Check criteria:" ), 
        0, ++yCheck, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CheckCriteria_JComboBox = new SimpleJComboBox ( 12, false );    // Do not allow edit
    List<String> checkCriteriaChoices = __command.getCheckCriteriaChoicesAsStrings();
    __CheckCriteria_JComboBox.setData ( checkCriteriaChoices );
    __CheckCriteria_JComboBox.addItemListener ( this );
    __CheckCriteria_JComboBox.setMaximumRowCount(checkCriteriaChoices.size());
    JGUIUtil.addComponent(check_JPanel, __CheckCriteria_JComboBox,
        1, yCheck, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel("Required - may require other parameters."), 
        3, yCheck, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(check_JPanel, new JLabel ( "Check value1:" ), 
		0, ++yCheck, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__CheckValue1_JTextField = new JTextField ( 10 );
	__CheckValue1_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(check_JPanel, __CheckValue1_JTextField,
		1, yCheck, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel(
		"Optional - minimum (or only) statistic value to check."), 
		3, yCheck, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(check_JPanel, new JLabel ( "Check value2:" ), 
        0, ++yCheck, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CheckValue2_JTextField = new JTextField ( 10 );
    __CheckValue2_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(check_JPanel, __CheckValue2_JTextField,
        1, yCheck, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel(
        "Optional - maximum value in range, or other statistic value to check."), 
        3, yCheck, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(check_JPanel, new JLabel ( "Problem type:" ), 
		0, ++yCheck, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__ProblemType_JTextField = new JTextField ( 10 );
	__ProblemType_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(check_JPanel, __ProblemType_JTextField,
		1, yCheck, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel(
		"Optional - problem type to use in output (default=Statistic-CheckCriteria)."), 
		3, yCheck, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(check_JPanel,new JLabel("If criteria met?:"),
        0, ++yCheck, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __IfCriteriaMet_JComboBox = new SimpleJComboBox ( false );
    List<String> criteriaChoices = new ArrayList<String>();
    criteriaChoices.add ( "" );
    criteriaChoices.add ( __command._Ignore );
    criteriaChoices.add ( __command._Warn );
    criteriaChoices.add ( __command._Fail );
    __IfCriteriaMet_JComboBox.setData(criteriaChoices);
    __IfCriteriaMet_JComboBox.select ( 0 );
    __IfCriteriaMet_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(check_JPanel, __IfCriteriaMet_JComboBox,
        1, yCheck, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel (
        "Optional - should warning/failure be generated (default=" + __command._Warn + ")."),
        3, yCheck, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(check_JPanel, new JLabel ( "Property name:" ), 
        0, ++yCheck, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PropertyName_JTextField = new JTextField ( 20 );
    __PropertyName_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(check_JPanel, __PropertyName_JTextField,
        1, yCheck, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel(
        "Optional - name of property to set when criteria are met."), 
        3, yCheck, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(check_JPanel, new JLabel ( "Property value:" ), 
        0, ++yCheck, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PropertyValue_JTextField = new JTextField ( 20 );
    __PropertyValue_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(check_JPanel, __PropertyValue_JTextField,
        1, yCheck, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel(
        "Optional - value of property to set when criteria are met."), 
        3, yCheck, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    /*
    JGUIUtil.addComponent(check_JPanel, new JLabel ( "Action:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Action_JComboBox = new SimpleJComboBox ( 12, false );    // Do not allow edit
    List<String> actionChoices = new Vector();
    actionChoices.add("");
    actionChoices.add(__command._Remove);
    actionChoices.add(__command._SetMissing);
    __Action_JComboBox.setData ( actionChoices );
    __Action_JComboBox.select(0);
    __Action_JComboBox.addItemListener ( this );
    __Action_JComboBox.setMaximumRowCount(actionChoices.size());
    JGUIUtil.addComponent(check_JPanel, __Action_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(check_JPanel, new JLabel("Optional - action for matched values (default=no action)."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        */

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea ( 4, 55 );
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Refresh the contents...
	refresh ();

	// South Panel: North
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
public void itemStateChanged ( ItemEvent e )
{
    refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	int code = event.getKeyCode();

	if ( code == KeyEvent.VK_ENTER ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else {
	    // Combo box...
		refresh();
	}
}

public void keyReleased ( KeyEvent event )
{	refresh();
}

public void keyTyped ( KeyEvent event ) {;}

/**
Indicate if the user pressed OK (cancel otherwise).
@return true if the edits were committed, false if the user canceled.
*/
public boolean ok ()
{	return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{	String routine = getClass().getSimpleName() + ".refresh";
    String InputFile = "";
    String IfNotFound = "";
    String Statistic = "";
    String SearchPattern = "";
    String TableID = "";
    String TableFilenameColumn = "";
    String TableStatisticColumn = "";
    String CheckCriteria = "";
	String CheckValue1 = "";
	String CheckValue2 = "";
	String IfCriteriaMet = "";
	String ProblemType = "";
    String PropertyName = "";
    String PropertyValue = "";
	//String Action = "";
	PropList props = __command.getCommandParameters();
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
        InputFile = props.getValue ( "InputFile" );
		IfNotFound = props.getValue ( "IfNotFound" );
        Statistic = props.getValue ( "Statistic" );
        SearchPattern = props.getValue ( "SearchPattern" );
        TableID = props.getValue ( "TableID" );
        TableFilenameColumn = props.getValue ( "TableFilenameColumn" );
        TableStatisticColumn = props.getValue ( "TableStatisticColumn" );
        CheckCriteria = props.getValue ( "CheckCriteria" );
		CheckValue1 = props.getValue ( "CheckValue1" );
		CheckValue2 = props.getValue ( "CheckValue2" );
		IfCriteriaMet = props.getValue ( "IfCriteriaMet" );
		ProblemType = props.getValue ( "ProblemType" );
		PropertyName = props.getValue ( "PropertyName" );
		PropertyValue = props.getValue ( "PropertyValue" );
		//Action = props.getValue ( "Action" );
		if ( InputFile != null ) {
			__InputFile_JTextField.setText ( InputFile );
		}
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
        if ( Statistic == null ) {
            // Select default...
            __Statistic_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __Statistic_JComboBox,Statistic, JGUIUtil.NONE, null, null ) ) {
                __Statistic_JComboBox.select ( Statistic );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nStatistic value \"" + Statistic +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( SearchPattern != null ) {
            __SearchPattern_JTextField.setText ( SearchPattern );
        }
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
        if ( TableFilenameColumn != null ) {
            __TableFilenameColumn_JTextField.setText ( TableFilenameColumn );
        }
        if ( TableStatisticColumn != null ) {
            __TableStatisticColumn_JTextField.setText ( TableStatisticColumn );
        }
        if ( CheckCriteria == null ) {
            // Select default...
            __CheckCriteria_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __CheckCriteria_JComboBox,CheckCriteria, JGUIUtil.NONE, null, null ) ) {
                __CheckCriteria_JComboBox.select ( CheckCriteria );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nCheckType value \"" + CheckCriteria +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
		if ( CheckValue1 != null ) {
			__CheckValue1_JTextField.setText ( CheckValue1 );
		}
        if ( CheckValue2 != null ) {
            __CheckValue2_JTextField.setText ( CheckValue2 );
        }
        if ( __IfCriteriaMet_JComboBox != null ) {
            if ( IfCriteriaMet == null ) {
                // Select default...
                __IfCriteriaMet_JComboBox.select ( "" );
            }
            else {  if (    JGUIUtil.isSimpleJComboBoxItem(
                    __IfCriteriaMet_JComboBox,
                    IfCriteriaMet, JGUIUtil.NONE, null, null ) ) {
                    __IfCriteriaMet_JComboBox.select ( IfCriteriaMet );
                }
                else {  Message.printWarning ( 1, routine,
                    "Existing command references an invalid\n"+
                    "IfCriteriaMet \"" + IfCriteriaMet + "\".  Select a\ndifferent value or Cancel." );
                }
            }
        }
		if ( ProblemType != null ) {
			__ProblemType_JTextField.setText ( ProblemType );
		}
        if ( PropertyName != null ) {
            __PropertyName_JTextField.setText ( PropertyName );
        }
        if ( PropertyValue != null ) {
            __PropertyValue_JTextField.setText ( PropertyValue );
        }
        /*
        if ( Action == null ) {
            // Select default...
            __Action_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __Action_JComboBox,Action, JGUIUtil.NONE, null, null ) ) {
                __Action_JComboBox.select ( Action );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nAction value \"" + Action +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }*/
	}
	// Regardless, reset the command from the fields...
    InputFile = __InputFile_JTextField.getText().trim();
    IfNotFound = __IfNotFound_JComboBox.getSelected();
    Statistic = __Statistic_JComboBox.getSelected();
    SearchPattern = __SearchPattern_JTextField.getText().trim();
    TableID = __TableID_JComboBox.getSelected();
    TableFilenameColumn = __TableFilenameColumn_JTextField.getText().trim();
    TableStatisticColumn = __TableStatisticColumn_JTextField.getText().trim();
    CheckCriteria = __CheckCriteria_JComboBox.getSelected();
    CheckValue2 = __CheckValue2_JTextField.getText().trim();
	CheckValue1 = __CheckValue1_JTextField.getText().trim();
    IfCriteriaMet = __IfCriteriaMet_JComboBox.getSelected();
	ProblemType = __ProblemType_JTextField.getText().trim();
	PropertyName = __PropertyName_JTextField.getText().trim();
    PropertyValue = __PropertyValue_JTextField.getText().trim();
	//Action = __Action_JComboBox.getSelected();
	props = new PropList ( __command.getCommandName() );
    props.add ( "InputFile=" + InputFile );
	props.add ( "IfNotFound=" + IfNotFound );
    props.add ( "Statistic=" + Statistic );
    props.add ( "SearchPattern=" + SearchPattern );
    props.add ( "TableID=" + TableID );
    props.add ( "TableFilenameColumn=" + TableFilenameColumn );
    props.add ( "TableStatisticColumn=" + TableStatisticColumn );
    // Have to set in such a way that = at start of CheckCriteria does not foul up the method
    props.set ( "CheckCriteria", CheckCriteria );
    props.add ( "CheckValue1=" + CheckValue1 );
    props.add ( "CheckValue2=" + CheckValue2 );
    props.add ( "IfCriteriaMet=" + IfCriteriaMet );
	props.add ( "ProblemType=" + ProblemType );
	props.add ( "PropertyName=" + PropertyName );
	props.add ( "PropertyValue=" + PropertyValue );
	//props.add ( "Action=" + Action );
	__command_JTextArea.setText( __command.toString ( props ) );
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
@param ok if false, then the edit is canceled.  If true, the edit is committed and the dialog is closed.
*/
private void response ( boolean ok )
{	__ok = ok;	// Save to be returned by ok()
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