package rti.tscommandprocessor.commands.ts;

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
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Time.TimeInterval;

/**
Editor for the ReadTimeSeriesList() command.
*/
public class ReadTimeSeriesList_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private ReadTimeSeriesList_Command __command = null;
private JTextArea __command_JTextArea = null;
private SimpleJComboBox __TableID_JComboBox = null;
private JTextField __LocationColumn_JTextField = null;
private JTextField __DataSource_JTextField = null;
private JTextField __DataType_JTextField = null;
private SimpleJComboBox __Interval_JComboBox = null;
private JTextField __Scenario_JTextField = null;
private JTextField __DataStore_JTextField = null;
private JTextField __InputName_JTextField = null;
private SimpleJComboBox	__IfNotFound_JComboBox = null;
private JTextField __DefaultUnits_JTextField = null; // Default units when blank time series is created.
private boolean __error_wait = false;	// Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK button has been pressed.

/**
Editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public ReadTimeSeriesList_JDialog (	JFrame parent, ReadTimeSeriesList_Command command, List<String> tableIDChoices )
{	super ( parent, true );
	initialize ( parent, command, tableIDChoices );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	Object o = event.getSource();

    if ( o == __cancel_JButton ) {
		response ( false );
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
{   // Put together a list of parameters to check...
    PropList parameters = new PropList ( "" );
    String TableID = __TableID_JComboBox.getSelected();
    String LocationColumn = __LocationColumn_JTextField.getText().trim();
    String DataSource = __DataSource_JTextField.getText().trim();
    String DataType = __DataType_JTextField.getText().trim();
    String Interval = __Interval_JComboBox.getSelected();
    String Scenario = __Scenario_JTextField.getText().trim();
    String DataStore = __DataStore_JTextField.getText().trim();
    String InputName = __InputName_JTextField.getText().trim();
    String IfNotFound = __IfNotFound_JComboBox.getSelected();
    String DefaultUnits = __DefaultUnits_JTextField.getText().trim();
    
    __error_wait = false;

    if ( TableID.length() > 0 ) {
        parameters.set ( "TableID", TableID );
    }
    if ( LocationColumn.length() > 0 ) {
        parameters.set ( "LocationColumn", LocationColumn );
    }
    if ( DataSource.length() > 0 ) {
        parameters.set ( "DataSource", DataSource );
    }
    if ( DataType.length() > 0 ) {
        parameters.set ( "DataType", DataType );
    }
    if ( Interval.length() > 0 ) {
        parameters.set ( "Interval", Interval );
    }
    if ( Scenario.length() > 0 ) {
        parameters.set ( "Scenario", Scenario );
    }
    if ( DataStore.length() > 0 ) {
        parameters.set ( "DataStore", DataStore );
    }
    if ( InputName.length() > 0 ) {
        parameters.set ( "InputName", InputName );
    }
    if ( IfNotFound.length() > 0 ) {
        parameters.set ( "IfNotFound", IfNotFound );
    }
    if ( DefaultUnits.length() > 0 ) {
        parameters.set ( "DefaultUnits", DefaultUnits );
    }
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
private void commitEdits ()
{   String TableID = __TableID_JComboBox.getSelected();
    String LocationColumn = __LocationColumn_JTextField.getText().trim();
    String DataSource = __DataSource_JTextField.getText().trim();
    String DataType = __DataType_JTextField.getText().trim();
    String Interval = __Interval_JComboBox.getSelected();
    String Scenario = __Scenario_JTextField.getText().trim();
    String DataStore = __DataStore_JTextField.getText().trim();
    String InputName = __InputName_JTextField.getText().trim();
    String IfNotFound = __IfNotFound_JComboBox.getSelected();
    String DefaultUnits = __DefaultUnits_JTextField.getText().trim();
    __command.setCommandParameter ( "TableID", TableID );
    __command.setCommandParameter ( "LocationColumn", LocationColumn );
    __command.setCommandParameter ( "DataSource", DataSource );
    __command.setCommandParameter ( "DataType", DataType );
    __command.setCommandParameter ( "Interval", Interval );
    __command.setCommandParameter ( "Scenario", Scenario );
    __command.setCommandParameter ( "DataStore", DataStore );
    __command.setCommandParameter ( "InputName", InputName );
    __command.setCommandParameter ( "IfNotFound", IfNotFound );
    __command.setCommandParameter ( "DefaultUnits", DefaultUnits );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{
	__cancel_JButton = null;
	__command_JTextArea = null;
	__command = null;
	__IfNotFound_JComboBox = null;
	__DataType_JTextField = null;
	__DefaultUnits_JTextField = null;
	__ok_JButton = null;
	super.finalize ();
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, ReadTimeSeriesList_Command command, List<String> tableIDChoices )
{	__command = (ReadTimeSeriesList_Command)command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Read a list of time series using location identifiers in a table." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The information specified below is used with the location identifiers" +
		" to create time series identifiers, which are then used to read the time series."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The time series identifiers (TSIDs) are of the form:"),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"  LocationID.DataSource.DataType.Interval.Scenario~DataStore~InputName"),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The term \"DataStore\" is used generically to mean a database, web service, or file supplying time series data " +
        "(also called \"Input Type\" elsewhere)."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Use the SetInputPeriod() command to specify the period to read."),
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
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Required - table containing list of location IDs."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Location ID column:"), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __LocationColumn_JTextField = new JTextField (10);
    __LocationColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __LocationColumn_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Required - name of column containing location IDs."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data source:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__DataSource_JTextField = new JTextField ( "", 20 );
	__DataSource_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __DataSource_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional or required depending on datastore."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data type:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__DataType_JTextField = new JTextField ( "", 20 );
	__DataType_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __DataType_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional or required depending on datastore."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data interval:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Interval_JComboBox = new SimpleJComboBox ( false );
    List<String> intervalChoices = TimeInterval.getTimeIntervalChoices(TimeInterval.MINUTE, TimeInterval.YEAR, false, -1, true);
    __Interval_JComboBox.setData(intervalChoices);
    __Interval_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Interval_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - data interval (time step) for time series."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Scenario:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Scenario_JTextField = new JTextField ( "", 20 );
	__Scenario_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __Scenario_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Datastore:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__DataStore_JTextField = new JTextField ( "", 20 );
	__DataStore_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __DataStore_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Required - needed to identify input database, file, etc."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Input name:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputName_JTextField = new JTextField ( "", 20 );
	__InputName_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __InputName_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - file name if required for datastore."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel,new JLabel("If time series not found?:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__IfNotFound_JComboBox = new SimpleJComboBox ( false );
	__IfNotFound_JComboBox.addItem ( __command._Default );
	__IfNotFound_JComboBox.addItem ( __command._Ignore );
	__IfNotFound_JComboBox.addItem ( __command._Warn );
	__IfNotFound_JComboBox.select ( __command._Warn );
	__IfNotFound_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __IfNotFound_JComboBox,
		1, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Required - how to handle time series that are not found."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Default units:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DefaultUnits_JTextField = new JTextField ( "", 20 );
    __DefaultUnits_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __DefaultUnits_JTextField,
    1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
    "Optional - units when IfNotFound=" + __command._Default + "."),
    3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __command_JTextArea = new JTextArea ( 4, 55 );
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

	__cancel_JButton = new SimpleJButton("Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__ok_JButton = new SimpleJButton("OK", this);
	button_JPanel.add ( __ok_JButton );

	setTitle ( "Edit " + __command.getCommandName() + "() Command" );
	setResizable ( true );
    pack();
    JGUIUtil.center( this );
	refresh();	// Sets the __path_JButton status
    super.setVisible( true );
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged ( ItemEvent e )
{	refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	int code = event.getKeyCode();

    refresh();
	if ( code == KeyEvent.VK_ENTER ) {
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
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
{   return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{   String routine = "ReadTimeSeriesList_JDialog.refresh";
    String TableID = "";
    String LocationColumn = "";
    String DataSource = "";
    String DataType = "";
    String Interval = "";
    String Scenario = "";
    String DataStore = "";
    String InputName = "";
    String IfNotFound = "";
    String DefaultUnits = "";
    PropList props = __command.getCommandParameters();
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command...
        TableID = props.getValue ( "TableID" );
        LocationColumn = props.getValue ( "LocationColumn" );
        DataSource = props.getValue ( "DataSource" );
        DataType = props.getValue ( "DataType" );
        Interval = props.getValue ( "Interval" );
        Scenario = props.getValue ( "Scenario" );
        DataStore = props.getValue ( "DataStore" );
        InputName = props.getValue ( "InputName" );
        IfNotFound = props.getValue ( "IfNotFound" );
        DefaultUnits = props.getValue ( "DefaultUnits" );
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
        if ( LocationColumn != null ) {
            __LocationColumn_JTextField.setText ( LocationColumn );
        }
        if ( DataSource != null ) {
            __DataSource_JTextField.setText ( DataSource );
        }
        if ( DataType != null ) {
            __DataType_JTextField.setText ( DataType );
        }
        if ( JGUIUtil.isSimpleJComboBoxItem(__Interval_JComboBox, Interval, JGUIUtil.NONE, null, null ) ) {
            __Interval_JComboBox.select ( Interval );
        }
        else {
            if ( (Interval == null) || Interval.equals("") ) {
                // New command...select the default...
                __Interval_JComboBox.select ( 0 );
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "Interval parameter \"" + Interval + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        if ( Scenario != null ) {
            __Scenario_JTextField.setText ( Scenario );
        }
        if ( DataStore != null ) {
            __DataStore_JTextField.setText ( DataStore );
        }
        if ( InputName != null ) {
            __InputName_JTextField.setText ( InputName );
        }
        if ( __IfNotFound_JComboBox != null ) {
            if ( IfNotFound == null ) {
                // Select default...
                __IfNotFound_JComboBox.select ( __command._Warn );
            }
            else {  if (    JGUIUtil.isSimpleJComboBoxItem(
                    __IfNotFound_JComboBox,
                    IfNotFound, JGUIUtil.NONE, null, null ) ) {
                    __IfNotFound_JComboBox.select ( IfNotFound );
                }
                else {  Message.printWarning ( 1, routine,
                    "Existing command references an invalid\n"+
                    "IfNotFound \"" + IfNotFound + "\".  Select a\ndifferent value or Cancel." );
                }
            }
        }
        if ( DefaultUnits != null ) {
            __DefaultUnits_JTextField.setText ( DefaultUnits );
        }
    }
    // Regardless, reset the command from the fields...
    TableID = __TableID_JComboBox.getSelected();
    LocationColumn = __LocationColumn_JTextField.getText().trim();
    DataSource = __DataSource_JTextField.getText().trim();
    DataType = __DataType_JTextField.getText().trim();
    Interval = __Interval_JComboBox.getSelected();
    Scenario = __Scenario_JTextField.getText().trim();
    DataStore = __DataStore_JTextField.getText().trim();
    InputName = __InputName_JTextField.getText().trim();
    IfNotFound = __IfNotFound_JComboBox.getSelected();
    DefaultUnits = __DefaultUnits_JTextField.getText().trim();
    props = new PropList ( __command.getCommandName() );
    props.add ( "TableID=" + TableID );
    props.add ( "LocationColumn=" + LocationColumn );
    props.add ( "DataSource=" + DataSource );
    props.add ( "DataType=" + DataType );
    props.add ( "Interval=" + Interval );
    props.add ( "Scenario=" + Scenario );
    props.add ( "DataStore=" + DataStore );
    props.add ( "InputName=" + InputName );
    props.add ( "IfNotFound=" + IfNotFound );
    props.add ( "DefaultUnits=" + DefaultUnits );
    __command_JTextArea.setText( __command.toString ( props ) );
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