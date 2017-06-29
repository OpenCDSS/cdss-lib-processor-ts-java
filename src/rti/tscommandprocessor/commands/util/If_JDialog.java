package rti.tscommandprocessor.commands.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.List;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

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

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Editor dialog for the If() command.
*/
@SuppressWarnings("serial")
public class If_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private If_Command __command = null;
private JTextField __Name_JTextField = null;
private JTabbedPane __main_JTabbedPane = null;
private JTextArea __Condition_JTextArea = null;
private SimpleJComboBox __CompareAsStrings_JComboBox = null;
private JTextField __PropertyIsNotDefinedOrIsEmpty_JTextField = null;
private JTextField __PropertyIsDefined_JTextField = null;
private JTextField __TSExists_JTextField = null;
private JTextArea __command_JTextArea = null;
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether user pressed OK to close the dialog.

/**
Command dialog editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public If_JDialog ( JFrame parent, If_Command command )
{ 	super(parent, true);
	initialize ( parent, command );
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
    PropList props = new PropList ( "" );
    String Name = __Name_JTextField.getText().trim();
    String Condition = __Condition_JTextArea.getText().trim();
    String CompareAsStrings = __CompareAsStrings_JComboBox.getSelected();
    String PropertyIsNotDefinedOrIsEmpty = __PropertyIsNotDefinedOrIsEmpty_JTextField.getText().trim();
    String PropertyIsDefined = __PropertyIsDefined_JTextField.getText().trim();
    String TSExists = __TSExists_JTextField.getText().trim();
    if ( Name.length() > 0 ) {
        props.set ( "Name", Name );
    }
    if ( Condition.length() > 0 ) {
        props.set ( "Condition", Condition );
    }
    if ( CompareAsStrings.length() > 0 ) {
        props.set ( "CompareAsStrings", CompareAsStrings );
    }
    if ( PropertyIsNotDefinedOrIsEmpty.length() > 0 ) {
        props.set ( "PropertyIsNotDefinedOrIsEmpty", PropertyIsNotDefinedOrIsEmpty );
    }
    if ( PropertyIsDefined.length() > 0 ) {
        props.set ( "PropertyIsDefined", PropertyIsDefined );
    }
    if ( TSExists.length() > 0 ) {
        props.set ( "TSExists", TSExists );
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
Commit the edits to the command.
*/
private void commitEdits ()
{   String Name = __Name_JTextField.getText().trim();
    String Condition = __Condition_JTextArea.getText().replace('\n', ' ').replace('\t', ' ').trim();
    String CompareAsStrings = __CompareAsStrings_JComboBox.getSelected();
    String PropertyIsNotDefinedOrIsEmpty = __PropertyIsNotDefinedOrIsEmpty_JTextField.getText().trim();
    String PropertyIsDefined = __PropertyIsDefined_JTextField.getText().trim();
    String TSExists = __TSExists_JTextField.getText().trim();
    __command.setCommandParameter ( "Name", Name );
    __command.setCommandParameter ( "Condition", Condition );
    __command.setCommandParameter ( "CompareAsStrings", CompareAsStrings );
    __command.setCommandParameter ( "PropertyIsNotDefinedOrIsEmpty", PropertyIsNotDefinedOrIsEmpty );
    __command.setCommandParameter ( "PropertyIsDefined", PropertyIsDefined );
    __command.setCommandParameter ( "TSExists", TSExists );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, If_Command command )
{   __command = command;

	addWindowListener( this );

    Insets insetsNONE = new Insets(1,1,1,1);
    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "This command evaluates a condition and if true the commands between this command and " +
        "the matching EndIf() command will be executed."),
        0, ++y, 7, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The evaluation can check an expression or whether a time series exists."),
        0, ++y, 7, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++y, 7, 1, 0, 0, insetsNONE, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "If name:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Name_JTextField = new JTextField ( 20 );
    __Name_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Name_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Required - the name will be matched against an EndIf() command name."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    __main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
     
    // Panel for expression
    int yCond = -1;
    JPanel cond_JPanel = new JPanel();
    cond_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Condition", cond_JPanel );
    
    JGUIUtil.addComponent(cond_JPanel, new JLabel (
        "Currently the condition can only consist of the syntax:"),
        0, ++yCond, 7, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(cond_JPanel, new JLabel (
        "   Value1 operator Value2"),
        0, ++yCond, 7, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(cond_JPanel, new JLabel (
        "where operator is <, <=, >, >=, ==, or !=, and values are integers, floating point numbers, booleans, or strings."),
        0, ++yCond, 7, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(cond_JPanel, new JLabel (
        "Values can use ${Property} processor property syntax."),
        0, ++yCond, 7, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(cond_JPanel, new JLabel (
        "In the future the ability to evaluate more complex conditions will be enabled."),
        0, ++yCond, 7, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(cond_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++yCond, 7, 1, 0, 0, insetsNONE, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(cond_JPanel, new JLabel ( "Condition:" ), 
        0, ++yCond, 1, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Condition_JTextArea = new JTextArea (5,40);
    __Condition_JTextArea.setLineWrap ( true );
    __Condition_JTextArea.setWrapStyleWord ( true );
    __Condition_JTextArea.addKeyListener(this);
    JGUIUtil.addComponent(cond_JPanel, new JScrollPane(__Condition_JTextArea),
        1, yCond, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(cond_JPanel, new JLabel("Optional - condition to evaluate."), 
        3, yCond, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(cond_JPanel, new JLabel ( "Compare as strings?:" ), 
        0, ++yCond, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CompareAsStrings_JComboBox = new SimpleJComboBox ( false );
    List<String> compareChoices = new ArrayList<String>();
    compareChoices.add ( "" );
    compareChoices.add ( __command._False );
    compareChoices.add ( __command._True );
    __CompareAsStrings_JComboBox.setData(compareChoices);
    __CompareAsStrings_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(cond_JPanel, __CompareAsStrings_JComboBox,
        1, yCond, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(cond_JPanel, new JLabel(
        "Optional - compare values as strings (default = " + __command._False + ")."), 
        3, yCond, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Panel for whether property has been defined
    int yPropDefined = -1;
    JPanel propDefined_JPanel = new JPanel();
    propDefined_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Property Defined?", propDefined_JPanel );
    
    JGUIUtil.addComponent(propDefined_JPanel, new JLabel (
        "This parameter, if specified, checks whether the specified property name is defined (not null) or is empty."),
        0, ++yPropDefined, 7, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(propDefined_JPanel, new JLabel (
        "Double is considered empty if value is NaN."),
        0, ++yPropDefined, 7, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(propDefined_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++yPropDefined, 7, 1, 0, 0, insetsNONE, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(propDefined_JPanel, new JLabel ( "If property is not defined or is empty:" ), 
        0, ++yPropDefined, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PropertyIsNotDefinedOrIsEmpty_JTextField = new JTextField ( 20 );
    __PropertyIsNotDefinedOrIsEmpty_JTextField.setToolTipText("Specify a property name to check.");
    __PropertyIsNotDefinedOrIsEmpty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(propDefined_JPanel, __PropertyIsNotDefinedOrIsEmpty_JTextField,
        1, yPropDefined, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(propDefined_JPanel, new JLabel(
        "Optional - If() will be true if the specified property is not defined or is empty."), 
        3, yPropDefined, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(propDefined_JPanel, new JLabel ( "If property is defined:" ), 
        0, ++yPropDefined, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PropertyIsDefined_JTextField = new JTextField ( 20 );
    __PropertyIsDefined_JTextField.setToolTipText("Specify a property name to check.");
    __PropertyIsDefined_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(propDefined_JPanel, __PropertyIsDefined_JTextField,
        1, yPropDefined, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(propDefined_JPanel, new JLabel(
        "Optional - If() will be true if the specified property is defined (not null)."), 
        3, yPropDefined, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Panel for whether time series exists
    int yTs = -1;
    JPanel ts_JPanel = new JPanel();
    ts_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Time Series Exists?", ts_JPanel );
    
    JGUIUtil.addComponent(ts_JPanel, new JLabel (
        "The TSExists parameter, if specified, checks whether a time series with the specified TSID or alias exists."),
        0, ++yTs, 7, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++yTs, 7, 1, 0, 0, insetsNONE, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "If TSID exists:" ), 
        0, ++yTs, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TSExists_JTextField = new JTextField ( 40 );
    __TSExists_JTextField.setToolTipText("Specify a TSID or alias to match or use ${Property}.");
    __TSExists_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(ts_JPanel, __TSExists_JTextField,
        1, yTs, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel(
        "Optional - If() will be true if the specified TSID exists."), 
        3, yTs, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:"),
		0, ++y, 1, 1, 0, 0, insetsNONE, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __command_JTextArea = new JTextArea ( 4, 60 );
    __command_JTextArea.setLineWrap ( true );
    __command_JTextArea.setWrapStyleWord ( true );
    __command_JTextArea.setEditable ( false );
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 6, 1, 1, 0, insetsNONE, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	// Refresh the contents...
	refresh ();

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
    JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	__cancel_JButton = new SimpleJButton("Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__ok_JButton = new SimpleJButton("OK", this);
	button_JPanel.add ( __ok_JButton );

	setTitle ( "Edit " + __command.getCommandName() + "() command" );
	setResizable ( true );
    pack();
    JGUIUtil.center( this );
    super.setVisible( true );
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged ( ItemEvent e )
{	//checkGUIState();
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
			response ( false );
		}
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
{   return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{	String routine = getClass().getSimpleName() + ".refresh";
	String Name = "";
	String Condition = "";
	String CompareAsStrings = "";
	String PropertyIsNotDefinedOrIsEmpty = "";
	String PropertyIsDefined = "";
	String TSExists = "";
	__error_wait = false;
	PropList props = __command.getCommandParameters();
	if ( __first_time ) {
		__first_time = false;
		Name = props.getValue( "Name" );
		Condition = props.getValue( "Condition" );
		CompareAsStrings = props.getValue( "CompareAsStrings" );
		PropertyIsNotDefinedOrIsEmpty = props.getValue( "PropertyIsNotDefinedOrIsEmpty" );
		PropertyIsDefined = props.getValue( "PropertyIsDefined" );
		TSExists = props.getValue( "TSExists" );
		if ( Name != null ) {
		    __Name_JTextField.setText( Name );
		}
        if ( Condition != null ) {
            __Condition_JTextArea.setText( Condition );
            if ( !Condition.isEmpty() ) {
            	__main_JTabbedPane.setSelectedIndex(0);
            }
        }
        if ( CompareAsStrings == null ) {
            // Select default...
            __CompareAsStrings_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __CompareAsStrings_JComboBox,CompareAsStrings, JGUIUtil.NONE, null, null ) ) {
                __CompareAsStrings_JComboBox.select ( CompareAsStrings );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nCompareAsStrings value \"" + CompareAsStrings +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( PropertyIsNotDefinedOrIsEmpty != null ) {
            __PropertyIsNotDefinedOrIsEmpty_JTextField.setText( PropertyIsNotDefinedOrIsEmpty );
            if ( !PropertyIsNotDefinedOrIsEmpty.isEmpty() ) {
            	__main_JTabbedPane.setSelectedIndex(1);
            }
        }
        if ( PropertyIsDefined != null ) {
            __PropertyIsDefined_JTextField.setText( PropertyIsDefined );
            if ( !PropertyIsDefined.isEmpty() ) {
            	__main_JTabbedPane.setSelectedIndex(1);
            }
        }
        if ( TSExists != null ) {
            __TSExists_JTextField.setText( TSExists );
            if ( !TSExists.isEmpty() ) {
            	__main_JTabbedPane.setSelectedIndex(2);
            }
        }
	}
	// Regardless, reset the command from the fields...
	Name = __Name_JTextField.getText().trim();
	Condition = __Condition_JTextArea.getText().trim();
	CompareAsStrings = __CompareAsStrings_JComboBox.getSelected();
	PropertyIsNotDefinedOrIsEmpty = __PropertyIsNotDefinedOrIsEmpty_JTextField.getText().trim();
	PropertyIsDefined = __PropertyIsDefined_JTextField.getText().trim();
    TSExists = __TSExists_JTextField.getText().trim();
    props = new PropList ( __command.getCommandName() );
    props.add ( "Name=" + Name );
    props.set ( "Condition", Condition ); // May contain = so handle differently
    props.add ( "CompareAsStrings=" + CompareAsStrings );
    props.add ( "PropertyIsNotDefinedOrIsEmpty=" + PropertyIsNotDefinedOrIsEmpty );
    props.add ( "PropertyIsDefined=" + PropertyIsDefined );
    props.add ( "TSExists=" + TSExists );
    __command_JTextArea.setText( __command.toString(props) );
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed and the dialog is closed.
*/
public void response ( boolean ok )
{   __ok = ok;
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