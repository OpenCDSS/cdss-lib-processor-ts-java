// SetProperty_JDialog - editor for SetProperty command

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
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

@SuppressWarnings("serial")
public class SetProperty_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private JTabbedPane __main_JTabbedPane = null;
private SetProperty_Command	__command = null;
private JTextArea __command_JTextArea = null;
private SimpleJComboBox __PropertyType_JComboBox = null;
private JTextField __PropertyValue_JTextField = null;
private JTextField __PropertyName_JTextField = null;
private JTextField __EnvironmentVariable_JTextField = null;
private JTextField __JavaProperty_JTextField = null;
private SimpleJComboBox	__IfJavaPropertyUndefined_JComboBox =null;
private SimpleJComboBox	__SetNull_JComboBox = null;
private SimpleJComboBox	__SetNaN_JComboBox = null;
private SimpleJComboBox	__SetEmpty_JComboBox = null;
private SimpleJComboBox	__RemoveProperty_JComboBox = null;
private JTextField __Add_JTextField = null;
private JTextField __Subtract_JTextField = null;
private JTextField __Multiply_JTextField = null;
private JTextField __Divide_JTextField = null;
private boolean __error_wait = false; // Is there an error to be cleared up or Cancel?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK button has been pressed.

/**
Command dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public SetProperty_JDialog ( JFrame parent, SetProperty_Command command ) {
	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event ) {
	Object o = event.getSource();

	if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "SetProperty");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else {
		// Choices.
		refresh();
	}
}

/**
Check the GUI state to make sure that appropriate components are enabled/disabled.
*/
private void checkGUIState () {
    // TODO SAM 2008-01-30 Anything to do?
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag to true.
This should be called before response() is allowed to complete.
*/
private void checkInput () {
	// Put together a list of parameters to check.
	PropList parameters = new PropList ( "" );
	String PropertyName = __PropertyName_JTextField.getText().trim();
    String PropertyType = __PropertyType_JComboBox.getSelected();
    // Don't trim because value can include surrounding whitespace.
	String PropertyValue = __PropertyValue_JTextField.getText();
	String EnvironmentVariable = __EnvironmentVariable_JTextField.getText().trim();
	String JavaProperty = __JavaProperty_JTextField.getText().trim();
	String IfJavaPropertyUndefined = __IfJavaPropertyUndefined_JComboBox.getSelected();
	String SetEmpty = __SetEmpty_JComboBox.getSelected();
	String SetNaN = __SetNaN_JComboBox.getSelected();
	String SetNull = __SetNull_JComboBox.getSelected();
	String RemoveProperty = __RemoveProperty_JComboBox.getSelected();
	String Add = __Add_JTextField.getText().trim();
	String Subtract = __Subtract_JTextField.getText().trim();
	String Multiply = __Multiply_JTextField.getText().trim();
	String Divide = __Divide_JTextField.getText().trim();

	__error_wait = false;

	if ( PropertyName.length() > 0 ) {
	    parameters.set ( "PropertyName", PropertyName );
	}
    if ( PropertyType.length() > 0 ) {
        parameters.set ( "PropertyType", PropertyType );
    }
	if ( PropertyValue.length() > 0 ) {
		parameters.set ( "PropertyValue", PropertyValue );
	}
	if ( EnvironmentVariable.length() > 0 ) {
		parameters.set ( "EnvironmentVariable", EnvironmentVariable );
	}
	if ( JavaProperty.length() > 0 ) {
		parameters.set ( "JavaProperty", JavaProperty );
	}
	if ( IfJavaPropertyUndefined.length() > 0 ) {
		parameters.set ( "IfJavaPropertyUndefined", IfJavaPropertyUndefined );
	}
	if ( SetEmpty.length() > 0 ) {
		parameters.set ( "SetEmpty", SetEmpty );
	}
	if ( SetNaN.length() > 0 ) {
		parameters.set ( "SetNaN", SetNaN );
	}
	if ( SetNull.length() > 0 ) {
		parameters.set ( "SetNull", SetNull );
	}
	if ( RemoveProperty.length() > 0 ) {
		parameters.set ( "RemoveProperty", RemoveProperty );
	}
	if ( Add.length() > 0 ) {
		parameters.set ( "Add", Add );
	}
	if ( Subtract.length() > 0 ) {
		parameters.set ( "Subtract", Subtract );
	}
	if ( Multiply.length() > 0 ) {
		parameters.set ( "Multiply", Multiply );
	}
	if ( Divide.length() > 0 ) {
		parameters.set ( "Divide", Divide );
	}

	try {
	    // This will warn the user.
		__command.checkCommandParameters ( parameters, null, 1 );
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
	String PropertyName = __PropertyName_JTextField.getText().trim();
    String PropertyType = __PropertyType_JComboBox.getSelected();
    // Don't trim because value can include surrounding whitespace.
	String PropertyValue = __PropertyValue_JTextField.getText();
	String EnvironmentVariable = __EnvironmentVariable_JTextField.getText().trim();
	String JavaProperty = __JavaProperty_JTextField.getText().trim();
	String IfJavaPropertyUndefined = __IfJavaPropertyUndefined_JComboBox.getSelected();
	String SetEmpty = __SetEmpty_JComboBox.getSelected();
	String SetNaN = __SetNaN_JComboBox.getSelected();
	String SetNull = __SetNull_JComboBox.getSelected();
	String RemoveProperty = __RemoveProperty_JComboBox.getSelected();
	String Add = __Add_JTextField.getText().trim();
	String Subtract = __Subtract_JTextField.getText().trim();
	String Multiply = __Multiply_JTextField.getText().trim();
	String Divide = __Divide_JTextField.getText().trim();
    __command.setCommandParameter ( "PropertyType", PropertyType );
	__command.setCommandParameter ( "PropertyValue", PropertyValue );
	__command.setCommandParameter ( "PropertyName", PropertyName );
	__command.setCommandParameter ( "EnvironmentVariable", EnvironmentVariable );
	__command.setCommandParameter ( "JavaProperty", JavaProperty);
	__command.setCommandParameter ( "IfJavaPropertyUndefined", IfJavaPropertyUndefined);
	__command.setCommandParameter ( "SetEmpty", SetEmpty );
	__command.setCommandParameter ( "SetNaN", SetNaN );
	__command.setCommandParameter ( "SetNull", SetNull );
	__command.setCommandParameter ( "RemoveProperty", RemoveProperty );
	__command.setCommandParameter ( "Add", Add );
	__command.setCommandParameter ( "Subtract", Subtract );
	__command.setCommandParameter ( "Multiply", Multiply );
	__command.setCommandParameter ( "Divide", Divide );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param title Dialog title.
@param command The command to edit.
*/
private void initialize ( JFrame parent, SetProperty_Command command ) {
	__command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Set (or unset) a property for the processor." +
		"  The property can be referenced in parameters of other commands using ${Property} notation." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Refer to command documentation and command editors for information about support for ${Property} in command parameters." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Properties can be set using the \"Set\", \"Environment Variable\", \"Java Property\", or \"Special Values\" tabs." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Properties can be removed (unset) using the \"Remove (Unset)\" tab." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Existing properties can be modified using basic math.  Set the value using ${Property} (can reference itself) and modify using \"Math\" tab." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    __main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Panel for setting.
    int ySet = -1;
    JPanel set_JPanel = new JPanel();
    set_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Set", set_JPanel );

    JGUIUtil.addComponent(set_JPanel, new JLabel (
		"The property value must be provided in a format that is appropriate for the type." ),
		0, ++ySet, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(set_JPanel, new JLabel (
		"For example, a Boolean property can have a value true or false, and Integer can only contain numbers and the negative sign." ),
		0, ++ySet, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(set_JPanel, new JLabel (
        "Specify date/times using standard notations to appropriate precision (e.g., YYYY-MM-DD hh:mm:ss)." ),
        0, ++ySet, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(set_JPanel, new JLabel (
        "DateTime values also recognize the following syntax (use as appropriate for date/time precision):"),
        0, ++ySet, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(set_JPanel, new JLabel (
        "    CurrentToYear = the current date to year precision"),
        0, ++ySet, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(set_JPanel, new JLabel (
        "    CurrentToMinute = the current date/time to minute precision"),
        0, ++ySet, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(set_JPanel, new JLabel (
        "    CurrentToMinute - 7Day = current date/time minus 7 days"),
        0, ++ySet, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(set_JPanel, new JLabel (
        "    CurrentToMinute + 7Day = current date/time plus 7 days"),
        0, ++ySet, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(set_JPanel, new JLabel (
        "    Use LastSunday, LastMonday, etc. for a date/time for a recent day ('Last' is inclusive of the current day)."),
        0, ++ySet, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(set_JPanel, new JLabel (
        "See also the SetInputPeriod() command for examples of date/time modifiers, such as .Timezone(), which sets the time zone."),
        0, ++ySet, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(set_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++ySet, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(set_JPanel, new JLabel ( "Property name:" ),
        0, ++ySet, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PropertyName_JTextField = new JTextField ( 60 );
    __PropertyName_JTextField.setToolTipText("Property name to set, can use ${Property}");
    __PropertyName_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(set_JPanel, __PropertyName_JTextField,
        1, ySet, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(set_JPanel, new JLabel( "Required - do not use spaces in name."),
        3, ySet, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(set_JPanel, new JLabel ( "Property type:" ),
        0, ++ySet, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PropertyType_JComboBox = new SimpleJComboBox ( false );
    List<String> typeChoices = new ArrayList<>();
    typeChoices.add ( "" ); // Use when setting special values or removing.
    typeChoices.add ( __command._Boolean );
    typeChoices.add ( __command._DateTime );
    typeChoices.add ( __command._Double );
    typeChoices.add ( __command._Integer );
    typeChoices.add ( __command._String );
    __PropertyType_JComboBox.setData(typeChoices);
    __PropertyType_JComboBox.select ( __command._String );
    __PropertyType_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(set_JPanel, __PropertyType_JComboBox,
        1, ySet, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(set_JPanel, new JLabel(
        "Required - to ensure proper initialization and checks."),
        3, ySet, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(set_JPanel, new JLabel ( "Property value:" ),
		0, ++ySet, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__PropertyValue_JTextField = new JTextField ( 60 );
	__PropertyValue_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(set_JPanel, __PropertyValue_JTextField,
		1, ySet, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(set_JPanel, new JLabel( "Required if not specified otherwise - property value, can use ${Property}."),
		3, ySet, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for environment variable.
    int yEnv = -1;
    JPanel env_JPanel = new JPanel();
    env_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Environment Variable", env_JPanel );

    JGUIUtil.addComponent(env_JPanel, new JLabel (
        "A property can be set to the value of an environment variable."),
        0, ++yEnv, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(env_JPanel, new JLabel (
        "This is useful when configurating a workflow based on the user's computer environment."),
        0, ++yEnv, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(env_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++yEnv, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(env_JPanel, new JLabel ( "Environment variable:" ),
		0, ++yEnv, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__EnvironmentVariable_JTextField = new JTextField ( 20 );
	__EnvironmentVariable_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(env_JPanel, __EnvironmentVariable_JTextField,
		1, yEnv, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(env_JPanel, new JLabel( "Optional - environment variable, can use ${Property}."),
		3, yEnv, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for Java property.
    int yJava = -1;
    JPanel java_JPanel = new JPanel();
    java_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Java Property", java_JPanel );

    JGUIUtil.addComponent(java_JPanel, new JLabel (
        "A property can be set to the value of a Java Property."),
        0, ++yJava, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(java_JPanel, new JLabel (
        "Java properties are set using the -DProperty=Value syntax when starting TSTool via the Java Runtime Environment."),
        0, ++yJava, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(java_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++yJava, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(java_JPanel, new JLabel ( "Java property:" ),
		0, ++yJava, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__JavaProperty_JTextField = new JTextField ( 20 );
	__JavaProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(java_JPanel, __JavaProperty_JTextField,
		1, yJava, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(java_JPanel, new JLabel( "Optional - Java property, can use ${Property}."),
		3, yJava, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(java_JPanel, new JLabel ( "If Java property is undefined?:"),
		0, ++yJava, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__IfJavaPropertyUndefined_JComboBox = new SimpleJComboBox ( false );
	List<String> notFoundChoices = new ArrayList<>();
	notFoundChoices.add ( "" );	// Default.
	notFoundChoices.add ( __command._Ignore );
	notFoundChoices.add ( __command._Warn );
	notFoundChoices.add ( __command._Fail );
	__IfJavaPropertyUndefined_JComboBox.setData(notFoundChoices);
	__IfJavaPropertyUndefined_JComboBox.select ( 0 );
	__IfJavaPropertyUndefined_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(java_JPanel, __IfJavaPropertyUndefined_JComboBox,
		1, yJava, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(java_JPanel, new JLabel(
		"Optional - action if property undefined (default=" + __command._Warn + ")."),
		3, yJava, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for special values.
    int ySpecial = -1;
    JPanel special_JPanel = new JPanel();
    special_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Special Values", special_JPanel );

    JGUIUtil.addComponent(special_JPanel, new JLabel (
        "Use the following parameters to set properties to special values, depending on property type."),
        0, ++ySpecial, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(special_JPanel, new JLabel (
        "Using special values ensures that there is no confusion interpreting the property value."),
        0, ++ySpecial, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(special_JPanel, new JLabel (
        "The property name must be specified in the \"Set\" tab."),
        0, ++ySpecial, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(special_JPanel, new JLabel (
        "The property type must be specified as String in the \"Set\" tab if setting to an empty string."),
        0, ++ySpecial, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(special_JPanel, new JLabel (
        "The property type must be specified as Double in the \"Set\" tab if setting to NaN."),
        0, ++ySpecial, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(special_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++ySpecial, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(special_JPanel, new JLabel ( "Set to empty string?"),
		0, ++ySpecial, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__SetEmpty_JComboBox = new SimpleJComboBox ( false );
	List<String> emptyChoices = new ArrayList<>();
	emptyChoices.add ( "" );	// Default.
	emptyChoices.add ( __command._True );
	__SetEmpty_JComboBox.setData(emptyChoices);
	__SetEmpty_JComboBox.select ( 0 );
	__SetEmpty_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(special_JPanel, __SetEmpty_JComboBox,
		1, ySpecial, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(special_JPanel, new JLabel(
		"Optional - set String property to empty string."),
		3, ySpecial, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(special_JPanel, new JLabel ( "Set to NaN?"),
		0, ++ySpecial, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__SetNaN_JComboBox = new SimpleJComboBox ( false );
	List<String> nanChoices = new ArrayList<>();
	nanChoices.add ( "" );	// Default.
	nanChoices.add ( __command._True );
	__SetNaN_JComboBox.setData(nanChoices);
	__SetNaN_JComboBox.select ( 0 );
	__SetNaN_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(special_JPanel, __SetNaN_JComboBox,
		1, ySpecial, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(special_JPanel, new JLabel(
		"Optional - set Double property to \"not a number\" (NaN)."),
		3, ySpecial, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(special_JPanel, new JLabel ( "Set to null?"),
		0, ++ySpecial, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__SetNull_JComboBox = new SimpleJComboBox ( false );
	List<String> nullChoices = new ArrayList<>();
	nullChoices.add ( "" );	// Default.
	nullChoices.add ( __command._True );
	__SetNull_JComboBox.setData(nullChoices);
	__SetNull_JComboBox.select ( 0 );
	__SetNull_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(special_JPanel, __SetNull_JComboBox,
		1, ySpecial, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(special_JPanel, new JLabel(
		"Optional - set any property type to null."),
		3, ySpecial, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for unset/remove.
    int yUnset = -1;
    JPanel unset_JPanel = new JPanel();
    unset_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Remove (Unset)", unset_JPanel );

    JGUIUtil.addComponent(unset_JPanel, new JLabel (
        "Use the following parameter to remove (unset) a property."),
        0, ++yUnset, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(unset_JPanel, new JLabel (
        "The processor will not have access to the property after the command (requests will return null)."),
        0, ++yUnset, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(unset_JPanel, new JLabel (
        "The property name must be specified in the \"Set\" tab."),
        0, ++yUnset, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(unset_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++yUnset, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(unset_JPanel, new JLabel ( "Remove/unset property?"),
		0, ++yUnset, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__RemoveProperty_JComboBox = new SimpleJComboBox ( false );
	List<String> removeChoices = new ArrayList<>();
	removeChoices.add ( "" );	// Default.
	removeChoices.add ( __command._True );
	__RemoveProperty_JComboBox.setData(removeChoices);
	__RemoveProperty_JComboBox.select ( 0 );
	__RemoveProperty_JComboBox.addActionListener ( this );
   JGUIUtil.addComponent(unset_JPanel, __RemoveProperty_JComboBox,
		1, yUnset, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(unset_JPanel, new JLabel(
		"Optional - remove/unset the property"),
		3, yUnset, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for math.
    int yMath = -1;
    JPanel math_JPanel = new JPanel();
    math_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Math", math_JPanel );

    JGUIUtil.addComponent(math_JPanel, new JLabel (
		"Use the following parameters to perform basic math operations on the property." ),
		0, ++yMath, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(math_JPanel, new JLabel (
		"The values can be specified using ${Property} notation."),
		0, ++yMath, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(math_JPanel, new JLabel (
		"The value must be consistent with the property type and math operation, as follows:" ),
		0, ++yMath, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(math_JPanel, new JLabel (
		"  DateTime - can add or subtract an interval such as 1Day, 15Minute" ),
		0, ++yMath, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(math_JPanel, new JLabel (
		"  Double - can add, subtract, multiply, or divide by a number" ),
		0, ++yMath, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(math_JPanel, new JLabel (
		"  Integer - can add, subtract, multiply, or divide by an integer" ),
		0, ++yMath, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(math_JPanel, new JLabel (
		"  String - can concatenate (add), remove (subtract), or replicate (multiply: append inital value multiple times)" ),
		0, ++yMath, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(math_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++yMath, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(math_JPanel, new JLabel ( "Add:" ),
		0, ++yMath, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Add_JTextField = new JTextField ( 40 );
	__Add_JTextField.setToolTipText("Value to add to the property being set, can use ${Property}.");
	__Add_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(math_JPanel, __Add_JTextField,
		1, yMath, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(math_JPanel, new JLabel( "Optional - value to add."),
		3, yMath, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(math_JPanel, new JLabel ( "Subtract:" ),
		0, ++yMath, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Subtract_JTextField = new JTextField ( 40 );
	__Subtract_JTextField.setToolTipText("Value to subtract from the property being set, can use ${Property}.");
	__Subtract_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(math_JPanel, __Subtract_JTextField,
		1, yMath, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(math_JPanel, new JLabel( "Optional - value to subtract."),
		3, yMath, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(math_JPanel, new JLabel ( "Multiply:" ),
		0, ++yMath, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Multiply_JTextField = new JTextField ( 40 );
	__Multiply_JTextField.setToolTipText("Value to multiply the property by, can use ${Property}.");
	__Multiply_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(math_JPanel, __Multiply_JTextField,
		1, yMath, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(math_JPanel, new JLabel( "Optional - value to multiply."),
		3, yMath, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(math_JPanel, new JLabel ( "Divide:" ),
		0, ++yMath, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Divide_JTextField = new JTextField ( 40 );
	__Divide_JTextField.setToolTipText("Value to divide the property by, can use ${Property}.");
	__Divide_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(math_JPanel, __Divide_JTextField,
		1, yMath, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(math_JPanel, new JLabel( "Optional - value to divide."),
		3, yMath, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea ( 4, 55 );
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Refresh the contents.
    checkGUIState();
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
public void itemStateChanged ( ItemEvent e ) {
	checkGUIState();
    refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event ) {
	int code = event.getKeyCode();

	if ( code == KeyEvent.VK_ENTER ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else {
	    // Combo box.
		refresh();
	}
}

public void keyReleased ( KeyEvent event ) {
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
    String PropertyName = "";
    String PropertyType = "";
	String PropertyValue = "";
	String EnvironmentVariable = "";
	String JavaProperty = "";
	String IfJavaPropertyUndefined = "";
	String SetEmpty = "";
	String SetNaN = "";
	String SetNull = "";
	String RemoveProperty = "";
	String Add = "";
	String Subtract = "";
	String Multiply = "";
	String Divide = "";
	PropList props = __command.getCommandParameters();
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command.
		PropertyName = props.getValue ( "PropertyName" );
        PropertyType = props.getValue ( "PropertyType" );
		PropertyValue = props.getValue ( "PropertyValue" );
		EnvironmentVariable = props.getValue ( "EnvironmentVariable" );
		JavaProperty = props.getValue ( "JavaProperty" );
		IfJavaPropertyUndefined = props.getValue ( "IfJavaPropertyUndefined" );
		SetEmpty = props.getValue ( "SetEmpty" );
		SetNaN = props.getValue ( "SetNaN" );
		SetNull = props.getValue ( "SetNull" );
		RemoveProperty = props.getValue ( "RemoveProperty" );
		Add = props.getValue ( "Add" );
		Subtract = props.getValue ( "Subtract" );
		Multiply = props.getValue ( "Multiply" );
		Divide = props.getValue ( "Divide" );
	    if ( PropertyName != null ) {
	         __PropertyName_JTextField.setText ( PropertyName );
	    }
        if ( PropertyType == null ) {
            // Select default.
            __PropertyType_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __PropertyType_JComboBox,PropertyType, JGUIUtil.NONE, null, null ) ) {
                __PropertyType_JComboBox.select ( PropertyType );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nPropertyType value \"" + PropertyType +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
		if ( PropertyValue != null ) {
		    __PropertyValue_JTextField.setText ( PropertyValue );
		}
		if ( EnvironmentVariable != null ) {
		    __EnvironmentVariable_JTextField.setText ( EnvironmentVariable );
		}
		if ( JavaProperty != null ) {
		    __JavaProperty_JTextField.setText (JavaProperty);
		}
		if ( JGUIUtil.isSimpleJComboBoxItem(__IfJavaPropertyUndefined_JComboBox, IfJavaPropertyUndefined,JGUIUtil.NONE, null, null ) ) {
			__IfJavaPropertyUndefined_JComboBox.select ( IfJavaPropertyUndefined );
		}
		else {
            if ( (IfJavaPropertyUndefined == null) || IfJavaPropertyUndefined.equals("") ) {
				// New command...select the default.
				__IfJavaPropertyUndefined_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"IfJavaPropertyUndefined parameter \"" + IfJavaPropertyUndefined +
				"\".  Select a\n value or Cancel." );
			}
		}
        if ( (SetEmpty == null) || SetEmpty.isEmpty() ) {
            // Select default.
            __SetEmpty_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __SetEmpty_JComboBox,SetEmpty, JGUIUtil.NONE, null, null ) ) {
                __SetEmpty_JComboBox.select ( SetEmpty );
                __main_JTabbedPane.setSelectedIndex(1);
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nSetEmpty value \"" + SetEmpty +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( (SetNaN == null) || SetNaN.isEmpty() ) {
            // Select default.
            __SetNull_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __SetNaN_JComboBox,SetNaN, JGUIUtil.NONE, null, null ) ) {
                __SetNaN_JComboBox.select ( SetNaN );
                __main_JTabbedPane.setSelectedIndex(1);
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nSetNaN value \"" + SetNaN +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( (SetNull == null) || SetNull.isEmpty() ) {
            // Select default.
            __SetNull_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __SetNull_JComboBox,SetNull, JGUIUtil.NONE, null, null ) ) {
                __SetNull_JComboBox.select ( SetNull );
                __main_JTabbedPane.setSelectedIndex(1);
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nSetNull value \"" + SetNull +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( (RemoveProperty == null) || RemoveProperty.isEmpty() ) {
            // Select default.
            __RemoveProperty_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __RemoveProperty_JComboBox,RemoveProperty, JGUIUtil.NONE, null, null ) ) {
                __RemoveProperty_JComboBox.select ( RemoveProperty );
               	__main_JTabbedPane.setSelectedIndex(2);
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nRemoveProperty value \"" + RemoveProperty +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
		if ( Add != null ) {
		    __Add_JTextField.setText ( Add );
		}
		if ( Subtract != null ) {
		    __Subtract_JTextField.setText ( Subtract );
		}
		if ( Multiply != null ) {
		    __Multiply_JTextField.setText ( Multiply );
		}
		if ( Divide != null ) {
		    __Divide_JTextField.setText ( Divide );
		}
	}
	// Regardless, reset the command from the fields.
	PropertyName = __PropertyName_JTextField.getText().trim();
    PropertyType = __PropertyType_JComboBox.getSelected();
	PropertyValue = __PropertyValue_JTextField.getText().trim();
	EnvironmentVariable = __EnvironmentVariable_JTextField.getText().trim();
	JavaProperty = __JavaProperty_JTextField.getText().trim();
	IfJavaPropertyUndefined = __IfJavaPropertyUndefined_JComboBox.getSelected();
	SetEmpty = __SetEmpty_JComboBox.getSelected();
	SetNaN = __SetNaN_JComboBox.getSelected();
	SetNull = __SetNull_JComboBox.getSelected();
	RemoveProperty = __RemoveProperty_JComboBox.getSelected();
	Add = __Add_JTextField.getText().trim();
	Subtract = __Subtract_JTextField.getText().trim();
	Multiply = __Multiply_JTextField.getText().trim();
	Divide = __Divide_JTextField.getText().trim();
	props = new PropList ( __command.getCommandName() );
    props.add ( "PropertyType=" + PropertyType );
	props.add ( "PropertyName=" + PropertyName );
	props.add ( "PropertyValue=" + PropertyValue );
	props.add ( "EnvironmentVariable=" + EnvironmentVariable );
	props.add ( "JavaProperty=" + JavaProperty );
	props.add ( "IfJavaPropertyUndefined=" + IfJavaPropertyUndefined );
	props.add ( "SetEmpty=" + SetEmpty );
	props.add ( "SetNaN=" + SetNaN );
	props.add ( "SetNull=" + SetNull );
	props.add ( "RemoveProperty=" + RemoveProperty );
	props.add ( "Add=" + Add );
	props.add ( "Subtract=" + Subtract );
	props.add ( "Multiply=" + Multiply );
	props.add ( "Divide=" + Divide );
	__command_JTextArea.setText( __command.toString ( props ).trim() );
}

/**
React to the user response.
@param ok if false, then the edit is canceled.
If true, the edit is committed and the dialog is closed.
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