package rti.tscommandprocessor.commands.reclamationhdb;

import java.awt.BorderLayout;
import java.awt.Color;
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
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import rti.tscommandprocessor.commands.reclamationhdb.ReclamationHDBConnectionUI;

/**
User interface for the ReclamationHDB connection dialog.
@author sam
*/
@SuppressWarnings("serial")
public class ReclamationHDBConnectionUI extends JDialog implements ActionListener, ItemListener, KeyListener
{
	
/**
Datastore factory.
*/
private ReclamationHDBDataStoreFactory factory = null;

/**
Datastore properties read from configuration file, when dialog is logging into a single datastore.
*/
private PropList datastoreProps = null;

/**
List of datastores to display, when dialog is logging into a datastore picked from the list.
*/
private List<ReclamationHDBDataStore> datastoreList = null;

/**
Datastore opened and returned by this class.
*/
private ReclamationHDBDataStore dataStore = null;

/**
Indicate whether single datastore is being edited, in case where PropList is in constructor.
*/
private boolean editingSingle = false;

// UI components
private SimpleJComboBox nameJComboBox = null;
private JTextField descriptionJTextField = null;
private JTextField serverJTextField = null;
private JTextField databaseJTextField = null;
private JTextField loginJTextField = null;
private JPasswordField passwordJPasswordField = null;
private SimpleJButton okJButton = null;
private SimpleJButton cancelJButton = null;
private JTextField statusJTextField = null;

/**
Create the editor dialog.
This version is used when a prompt is desired to enter database login credentials at start-up, using properties from a datastore configuration file.
@param factory the ReclamationHDBDataStoreFactory object that handled reading in the configuration file
@param props properties for the datastore, read from the datastore configuration file
@param frame the main UI, used to position the dialog
@param return the datastore
*/
public ReclamationHDBConnectionUI ( ReclamationHDBDataStoreFactory factory, PropList props, JFrame parent )
{	super(parent, true); // This is important - it prevents the main UI from continuing
	this.factory = factory;
	if ( props == null ) {
		props = new PropList("");
	}
	this.datastoreProps = props;
	this.editingSingle = true;
	initUI();
}

/**
Create the editor dialog.
This version is used when (re)connecting to a datastore after initial startup, for example to change users..
@param factory the ReclamationHDBDataStoreFactory object that handled reading in the configuration file
@param datastoreList a list of ReclamationHDB datastores that were initially configured but may or may not be active/open.
The user will first pick a datastore to access its properties, and will then enter a new login and password for the database connection.
Properties for the datastores are used in addition to the login and password specified interactively to recreate the database connection.
@param frame the main UI, used to position the dialog
@param return the datastore
*/
public ReclamationHDBConnectionUI ( ReclamationHDBDataStoreFactory factory, List<ReclamationHDBDataStore> datastoreList, JFrame parent )
{	super(parent, true); // This is important - it prevents the main UI from continuing
	this.factory = factory;
	if ( datastoreList == null ) {
		datastoreList = new ArrayList<ReclamationHDBDataStore>();
	}
	this.datastoreProps = null;
	this.datastoreList = datastoreList;
	this.editingSingle = false;
	initUI();
}

/**
Responds to action events.
@param event the event that happened.
*/
public void actionPerformed(ActionEvent event) {
	String command = event.getActionCommand();
	if (command.equals("OK")) {
		okClicked();
	}  
	else if (command.equals("Cancel")) {
	    cancelClicked();
	}
}

/**
Close the dialog without transferring any settings to the internal data.
*/
private void cancelClicked()
{	// If the datastore is non-null, then OK was tried but failed,
	// use the existing information since the status and message are helpful
	if ( getDataStore() == null ) {
		// Create datastore but do not initialize the database connection (similar to configuration file with errors).
		// Use the factory method to do the heavy lifting in creating the datastore, with properties updated from this UI
		if ( editingSingle ) {
			String systemLogin = this.loginJTextField.getText().trim();
			this.datastoreProps.set("SystemLogin",systemLogin);
			String systemPassword = new String(this.passwordJPasswordField.getPassword());
			this.datastoreProps.set("SystemPassword",systemPassword);
			this.dataStore = (ReclamationHDBDataStore)this.factory.create(this.datastoreProps);
			// Factory will have set the status=1 and error to an internal exception message - be more specific here
			this.dataStore.setStatus(2);
			this.dataStore.setStatusMessage("Canceled login dialog.");
		}
		else {
			// Leave the existing data store as is
		}
	}
	closeDialog();
}

/**
Close the dialog and dispose of graphical resources.
*/
private void closeDialog()
{
	setVisible(false);
	dispose();
}

/**
Return the datastore opened by the UI, or null.
*/
public ReclamationHDBDataStore getDataStore()
{
	return this.dataStore;
}

/**
Initialize the user interface.
*/
private void initUI()
{
	// used in the GridBagLayouts
	Insets LTB_insets = new Insets(7,7,0,0);
	Insets RTB_insets = new Insets(7,0,0,7);
	GridBagLayout gbl = new GridBagLayout();

	// North JPanel for the data components
	JPanel northJPanel = new JPanel();
	northJPanel.setLayout(gbl);
	getContentPane().add("North", northJPanel);

	int y = -1;	// Vertical position of components in grid bag layout
	if ( this.editingSingle ) {
	    JGUIUtil.addComponent(northJPanel, new JLabel("This dialog is shown because the datastore configuration file is requesting a prompt for login."), 
			0, ++y, 7, 1, 0, 0, LTB_insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	else {
		JGUIUtil.addComponent(northJPanel, new JLabel("Select a datastore to change the login."), 
			0, ++y, 7, 1, 0, 0, LTB_insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
    JGUIUtil.addComponent(northJPanel, new JLabel("Only the login and password can be specified - change other information in the datastore configuration file."), 
		0, ++y, 7, 1, 0, 0, LTB_insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(northJPanel, new JSeparator(SwingConstants.HORIZONTAL), 
		0, ++y, 7, 1, 0, 0, LTB_insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Datastore name - put in a combo box but if constructor used a PropList make it view-only
	JGUIUtil.addComponent(northJPanel, new JLabel("Datastore:"), 
    	0, ++y, 1, 1, 0, 0, LTB_insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
	this.nameJComboBox = new SimpleJComboBox(false);
	List<String> names = new ArrayList<String>();
    if ( this.editingSingle ) {
    	// Add the single name
        names.add(this.datastoreProps.getValue("Name"));
        this.nameJComboBox.setEnabled(false);
    }
    else {
    	for ( ReclamationHDBDataStore ds : this.datastoreList ) {
    		names.add(ds.getName());
    	}
    }
    this.nameJComboBox.setData(names);
    if ( this.nameJComboBox.getItemCount() > 0 ) {
    	this.nameJComboBox.select(0);
    }
    this.nameJComboBox.addItemListener(this);
    JGUIUtil.addComponent(northJPanel, this.nameJComboBox, 
		1, y, 1, 1, 0, 0, RTB_insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
	// Description
    JGUIUtil.addComponent(northJPanel, new JLabel("Description:"), 
		0, ++y, 1, 1, 0, 0, LTB_insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
    this.descriptionJTextField = new JTextField(40);
    this.descriptionJTextField.setEnabled(false);
    setDescription();
    JGUIUtil.addComponent(northJPanel, this.descriptionJTextField, 
		1, y, 1, 1, 0, 0, RTB_insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
	// Server
    JGUIUtil.addComponent(northJPanel, new JLabel("Server:"), 
		0, ++y, 1, 1, 0, 0, LTB_insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
    this.serverJTextField = new JTextField(20);
    this.serverJTextField.setEnabled(false);
    setServer();
	//this.loginJTextField.addKeyListener(this);
    JGUIUtil.addComponent(northJPanel, this.serverJTextField, 
		1, y, 1, 1, 0, 0, RTB_insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
	// Database
    JGUIUtil.addComponent(northJPanel, new JLabel("Database:"), 
		0, ++y, 1, 1, 0, 0, LTB_insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
    this.databaseJTextField = new JTextField(20);
    this.databaseJTextField.setEnabled(false);
	//this.loginJTextField.addKeyListener(this);
    setDatabase();
    JGUIUtil.addComponent(northJPanel, this.databaseJTextField, 
		1, y, 1, 1, 0, 0, RTB_insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
	// Login
    JGUIUtil.addComponent(northJPanel, new JLabel("Login:"), 
		0, ++y, 1, 1, 0, 0, LTB_insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
    this.loginJTextField = new JTextField(20);
	//this.loginJTextField.addKeyListener(this);
	setLogin();
    JGUIUtil.addComponent(northJPanel, this.loginJTextField, 
		1, y, 1, 1, 0, 0, RTB_insets, GridBagConstraints.NONE, GridBagConstraints.WEST);

	// Password
    JGUIUtil.addComponent(northJPanel, new JLabel("Password:"), 
		0, ++y, 1, 1, 0, 0, LTB_insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
	this.passwordJPasswordField = new JPasswordField(20);
	this.passwordJPasswordField.addKeyListener(this);
	this.passwordJPasswordField.setEchoChar('*');
	setPassword();
	//passwordJPasswordField.addKeyListener(this);
	JGUIUtil.addComponent(northJPanel, this.passwordJPasswordField, 
		1, y, 1, 1, 0, 0, RTB_insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
	// South JPanel
	JPanel southJPanel = new JPanel();
	southJPanel.setLayout(new BorderLayout());
	getContentPane().add("South", southJPanel);

	// South North JPanel
	JPanel southNJPanel = new JPanel();
	southNJPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
	southJPanel.add("North", southNJPanel);

	this.okJButton = new SimpleJButton("OK", "OK",this);
	southNJPanel.add(this.okJButton);

	this.cancelJButton = new SimpleJButton("Cancel", "Cancel", this);
	southNJPanel.add(this.cancelJButton);
	
	// South South JPanel
	JPanel southSJPanel = new JPanel();
	southSJPanel.setLayout(gbl);
	southJPanel.add("South", southSJPanel);

	this.statusJTextField = new JTextField();
	this.statusJTextField.setEditable(false);
	JGUIUtil.addComponent(southSJPanel, this.statusJTextField, 
		0, 0, 1, 1, 1, 1, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        
	// Dialog settings        
	setTitle("Connect to Reclamation HDB Database");
	this.statusJTextField.setText( "Enter login information for Reclamation HDB and press OK to open the database connection.");

	pack();
	JGUIUtil.center(this);
	setResizable(false);
	setVisible(true);
}

/**
Handle item events.
@param evt
*/
public void itemStateChanged ( ItemEvent evt )
{
	if ( evt.getStateChange() == ItemEvent.SELECTED ) {
		// A new datastore was selected so set all the other data
		setDescription();
		setServer();
		setDatabase();
		setLogin();
		setPassword();
	}
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	int code = event.getKeyCode();
	if ( code == KeyEvent.VK_ENTER ) {
		// Same as OK
		okClicked();
	}
}

public void keyReleased ( KeyEvent event )
{
}

public void keyTyped ( KeyEvent event ) {;}

/**
Lookup the datastore matching the datastore name, used when list of datastores was used in constructor.
@return the datastore matching the currently selected datastore name.
*/
private ReclamationHDBDataStore lookupDatastore ()
{
	for ( ReclamationHDBDataStore ds : this.datastoreList ) {
		if ( ds.getName().equalsIgnoreCase(this.nameJComboBox.getSelected())) {
			//Message.printStatus(2, "", "Matched datastore \"" + this.nameJComboBox.getSelected() + "\"" );
			return ds;
		}
	}
	//Message.printStatus(2, "", "Did not match datastore \"" + this.nameJComboBox.getSelected() + "\"" );
	return null;
}

/**
Use the information in the dialog to try to instantiate a new datastore and DMI instance.
*/
private void okClicked()
{	String routine = getClass().getSimpleName() + ".okClicked";
	String systemLogin = this.loginJTextField.getText().trim();
	String systemPassword = new String(this.passwordJPasswordField.getPassword());
	if ( editingSingle ) {
		// Use the factory method to do the heavy lifting in creating the datastore, with properties updated from this UI
		this.datastoreProps.set("SystemLogin",systemLogin);
		this.datastoreProps.set("SystemPassword",systemPassword);
		try {
			JGUIUtil.setWaitCursor(this, true);
			this.dataStore = (ReclamationHDBDataStore)this.factory.create(this.datastoreProps);
		}
		finally {
			JGUIUtil.setWaitCursor(this, false);
		}
	}
	else {
		// Grab the existing datastore's properties, reset the login and password, and recreate a new datastore.
		try {
			ReclamationHDBDataStore ds = lookupDatastore();
			PropList props = ds.getProperties();
			props.set("SystemLogin",systemLogin);
			props.set("SystemPassword",systemPassword);
			JGUIUtil.setWaitCursor(this, true);
			this.dataStore = (ReclamationHDBDataStore)this.factory.create(props);
		}
		finally {
			JGUIUtil.setWaitCursor(this, false);
		}
	}
	// Check whether the database connection is open.  Change the status message.
	if ( (this.dataStore.getDMI() != null) && this.dataStore.getDMI().isOpen() ) {
		// OK to close
		Message.printStatus(2, routine, "Opened datastore \"" + this.dataStore.getName() + "\" using prompt.");
		closeDialog();
	}
	else {
		this.statusJTextField.setText("Unable to connect to database with provided user information.  Check information and try again or Cancel.");
		this.statusJTextField.setBackground(Color.red);
		// Factory will have set the status=1 and error to an internal exception message - be more specific here
		this.dataStore.setStatus(2);
		this.dataStore.setStatusMessage("Invalid user information provided in login dialog.");
	}
}

/**
Set the database name text field based on datastore input.
*/
private void setDatabase ()
{
	String database = "";
	if ( this.editingSingle ) {
		database = this.datastoreProps.getValue("DatabaseName");
	}
	else {
		// Get the datastore matching the selected name
		ReclamationHDBDataStore ds = lookupDatastore();
		database = ds.getProperty("DatabaseName");
	}
    if ( database == null ) {
    	database = "";
    }
    this.databaseJTextField.setText(database);
}

/**
Set the description text field based on datastore input.
*/
private void setDescription ()
{
	String description = "";
	if ( this.editingSingle ) {
		description = this.datastoreProps.getValue("Description");
	}
	else {
		// Get the datastore matching the selected name
		ReclamationHDBDataStore ds = lookupDatastore();
		description = ds.getProperty("Description");
	}
    if ( description == null ) {
    	description = "";
    }
    this.descriptionJTextField.setText(description);
}

/**
Set the login text field based on datastore input.
*/
private void setLogin ()
{
	String login = "";
	if ( this.editingSingle ) {
		login = this.datastoreProps.getValue("SystemLogin");
	}
	else {
		// Get the datastore matching the selected name
		ReclamationHDBDataStore ds = lookupDatastore();
		login = ds.getProperty("SystemLogin");
	}
    if ( (login == null) || login.equalsIgnoreCase("prompt") ) {
    	login = "";
    }
    this.loginJTextField.setText(login);
}

/**
Set the password text field based on datastore input.
*/
private void setPassword ()
{
	String password = "";
	if ( this.editingSingle ) {
		password = this.datastoreProps.getValue("SystemPassword");
	}
	else {
		// Get the datastore matching the selected name
		ReclamationHDBDataStore ds = lookupDatastore();
		password = ds.getProperty("SystemPassword");
	}
    if ( (password == null) || password.equalsIgnoreCase("prompt") ) {
    	password = "";
    }
    this.passwordJPasswordField.setText(password);
}

/**
Set the database server text field based on datastore input.
*/
private void setServer ()
{
	String server = "";
	if ( this.editingSingle ) {
		server = this.datastoreProps.getValue("DatabaseServer");
	}
	else {
		// Get the datastore matching the selected name
		ReclamationHDBDataStore ds = lookupDatastore();
		server = ds.getProperty("DatabaseServer");
	}
    if ( server == null ) {
    	server = "";
    }
    this.serverJTextField.setText(server);
}

}