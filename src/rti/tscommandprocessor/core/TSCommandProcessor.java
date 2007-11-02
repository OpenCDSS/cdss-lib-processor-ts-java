//------------------------------------------------------------------------------
// TSCommandProcessor - a class to process time series commands and manage
//				relevant data
//------------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
//------------------------------------------------------------------------------
// History:
//
// 2005-04-19	Steven A. Malers, RTi	Initial version as a wrapper around
//					TSEngine.
// 2005-09-06	SAM, RTi		Add readTimeSeries2().
// 2005-10-18	SAM, RTi		Implement TSSupplier to allow
//					processing of processTSProduct()
//					commands.
// 2006-05-02	SAM, RTi		Add processCommands() to allow a
//					call from the runCommands() command.
// 2007-01-26	Kurt Tometich, RTi	Moved this class to new product
//							TSCommandProcessor.  Added several methods that
//							call or mimic TSEngine methods to allow generic
//							command implementations to be able to call TSEngine
//							without directly depending or being a member of that
//							class.
// 2007-02-08	SAM, RTi		Change class to TSCommandProcessor package.
//					Explicitly include TS package classes.
//					To remove circular dependencies in commands that are in
//					other lower-level packages, force all interaction with the
//					processor to occur through the properties.
//------------------------------------------------------------------------------
// EndHeader

package rti.tscommandprocessor.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

import java.awt.event.WindowListener;	// To know when graph closes to close app

// RTi utility code.

import RTi.Util.IO.Command;
import RTi.Util.IO.CommandListListener;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorListener;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.GenericCommand;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.Message.Message;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.IO.RequestParameterInvalidException;
import RTi.Util.IO.RequestParameterNotFoundException;
import RTi.Util.IO.UnknownCommandException;
import RTi.Util.IO.UnrecognizedRequestException;
import RTi.Util.Time.DateTime;

import RTi.TS.TS;
import RTi.TS.TSIdent;
import RTi.TS.TSLimits;
import RTi.TS.TSSupplier;

// HydroBase commands.

import DWR.DMI.HydroBaseDMI.HydroBaseDMI;

// NWSRFS_DMI commands.

import RTi.DMI.NWSRFS_DMI.NWSRFS_DMI;

// TS general commands.

// REVISIT SAM 2005-05-19 When TSEngine is sufficiently clean, merge its code
// into this class and rework the connection to the TSTool application.
/**
This class processes time series commands and manages the relevant data.
*/
public class TSCommandProcessor implements CommandProcessor, TSSupplier
{

/**
The legacy TSEngine class did all of the processing in TSTool.  It is now
wrapped by this TSCommandProcessor class and code will continue to be moved
to Command classes and this class.
*/
private TSEngine __tsengine = null;

/**
Actions that are used when processing time series, indicating
whether new time series are created or existing ones modified.
*/
public final int	INSERT_TS = 1,
					UPDATE_TS = 2,
					EXIT = 3,
					NONE = 4;

/**
The list of commands managed by this command processor,
guaranteed to be non-null.
*/
private Vector __Command_Vector = new Vector();

/**
The name of the file to which the commands are saved, or null if not saved.
Note that the in-memory commands should always be used for printing file headers.
*/
private String __commands_filename = null;

/**
The array of CommandListListeners to be called when the command list changes.
*/
private CommandListListener [] __CommandListListener_array = null;

/**
The array of CommandProcessorListeners to be called when the commands are
running, to indicate progress.
*/
private CommandProcessorListener [] __CommandProcessorListener_array = null;

/**
The initial working directory for processing, typically the location of the commands
file from read/write.  This is used to adjust the working directory with
setWorkingDir() commands.
*/
private String __InitialWorkingDir_String = null;

/**
The current workding directory for processing, which was initialized to
InitialWorkingDir and modified by setWorkingDir() commands.
*/
private String __WorkingDir_String = null;

/**
Indicates whether the processor is currently running (false if has not started
or has completed).  The running occurs in TSEngine.processCommands().
*/
private volatile boolean __is_running = false;

/**
Indicates whether the processing loop should be cancelled.  This is a request
(e.g., from a GUI) that needs to be handled as soon as possible during command
processing.  It is envisioned that cancel can always occur between commands and
as time allows it will also be enabled within a command.
*/
private volatile boolean __cancel_processing_requested = false;

/**
Construct a command processor with no commands.
*/
public TSCommandProcessor ()
{	super();

	// Create a TSEngine that works parallel to this class.
	__tsengine = new TSEngine ( this );
}

/**
Add a command at the end of the list and notify command list listeners of the
add.
@param command Command to add.
*/
public void addCommand ( Command command )
{
	addCommand ( command, true );
}

/**
Add a command at the end of the list.
@param command Command to add.
@param notifyCommandListListeners Indicate whether registered CommandListListeners should
be notified.
*/
public void addCommand ( Command command, boolean notifyCommandListListeners )
{	String routine = "TSCommandProcessor.addCommand";
	__Command_Vector.addElement( command );
	if ( notifyCommandListListeners ) {
		notifyCommandListListenersOfAdd ( __Command_Vector.size() - 1,
			__Command_Vector.size() - 1 );
	}
	Message.printStatus(2, routine, "Added command object \"" +	command + "\"." );
}

/**
Add a command at the end of the list using the string text.  This should currently only be
used for commands that do not have command classes, which perform
additional validation on the commands.  A GenericCommand instance will
be instantiated to maintain the string and allow command status to be
set.
@param command_string Command string for command.
@param index Index (0+) at which to insert the command.
*/
public void addCommand ( String command_string )
{	String routine = "TSCommandProcessor.addCommand";
	Command command = new GenericCommand ();
	command.setCommandString ( command_string );
	addCommand ( command );
	Message.printStatus(2, routine, "Creating generic command from string \"" +
			command_string + "\"." );
}

/**
Add a CommandListListener, to be notified when commands are added, removed,
or change (are edited or execution status is updated).
If the listener has already been added, the listener will remain in
the list in the original order.
*/
public void addCommandListListener ( CommandListListener listener )
{
		// Use arrays to make a little simpler than Vectors to use later...
		if ( listener == null ) {
			return;
		}
		// See if the listener has already been added...
		// Resize the listener array...
		int size = 0;
		if ( __CommandListListener_array != null ) {
			size = __CommandListListener_array.length;
		}
		for ( int i = 0; i < size; i++ ) {
			if ( __CommandListListener_array[i] == listener ) {
				return;
			}
		}
		if ( __CommandListListener_array == null ) {
			__CommandListListener_array = new CommandListListener[1];
			__CommandListListener_array[0] = listener;
		}
		else {	// Need to resize and transfer the list...
			size = __CommandListListener_array.length;
			CommandListListener [] newlisteners =
				new CommandListListener[size + 1];
			for ( int i = 0; i < size; i++ ) {
					newlisteners[i] = __CommandListListener_array[i];
			}
			__CommandListListener_array = newlisteners;
			__CommandListListener_array[size] = listener;
			newlisteners = null;
		}
}

/**
Add a CommandProcessorListener, to be notified when commands are started,
progress made, and completed.  This is useful to allow calling software to
report progress.
If the listener has already been added, the listener will remain in
the list in the original order.
*/
public void addCommandProcessorListener ( CommandProcessorListener listener )
{
		// Use arrays to make a little simpler than Vectors to use later...
		if ( listener == null ) {
			return;
		}
		// See if the listener has already been added...
		// Resize the listener array...
		int size = 0;
		if ( __CommandProcessorListener_array != null ) {
			size = __CommandProcessorListener_array.length;
		}
		for ( int i = 0; i < size; i++ ) {
			if ( __CommandProcessorListener_array[i] == listener ) {
				return;
			}
		}
		if ( __CommandProcessorListener_array == null ) {
			__CommandProcessorListener_array = new CommandProcessorListener[1];
			__CommandProcessorListener_array[0] = listener;
		}
		else {	// Need to resize and transfer the list...
			size = __CommandProcessorListener_array.length;
			CommandProcessorListener [] newlisteners =
				new CommandProcessorListener[size + 1];
			for ( int i = 0; i < size; i++ ) {
					newlisteners[i] = __CommandProcessorListener_array[i];
			}
			__CommandProcessorListener_array = newlisteners;
			__CommandProcessorListener_array[size] = listener;
			newlisteners = null;
		}
}

/**
Clear the results of processing.  This resets the list of time series to
empty.
*/
public void clearResults()
{
	__tsengine.clearResults();
}

/**
Return the Command instance at the requested position.
@return The number of commands being managed by the processor
*/
public Command get( int pos )
{
	return (Command)__Command_Vector.get(pos);
}

/**
Indicate whether cancelling processing has been requested.
@return true if cancelling has been requested.
*/
public boolean getCancelProcessingRequested ()
{
	return __cancel_processing_requested;
}

/**
Return the list of commands.
@return the list of commands.
*/
public Vector getCommands ()
{
	return __Command_Vector;
}

/**
Get the name of the commands file associated with the processor.
*/
public String getCommandsFileName ()
{
	return __commands_filename;
}

/**
Return the initial working directory for the processor.
@return the initial working directory for the processor.
*/
public String getInitialWorkingDir ()
{	return __InitialWorkingDir_String;
}

/**
Indicate whether the processing is running.
@return true if the command processor is running, false if not.
*/
public boolean getIsRunning ()
{	return __is_running;
}

/**
Return data for a named property, required by the CommandProcessor
interface.  See getPropcontents() for a list of properties that are
handled.
@param prop Property to set.
@return the named property, or null if a value is not found.
@exception Exception if the property cannot be found or there is an
error determining the property.
*/
public Prop getProp ( String prop ) throws Exception
{	Object o = getPropContents ( prop );
	if ( o == null ) {
		return null;
	}
	else {	// Contents will be a Vector, etc., so convert to a full
		// property.
		// TODO SAM 2005-05-13 This will work seamlessly for strings
		// but may have a side-effects (conversions) for non-strings...
		Prop p = new Prop ( prop, o, o.toString() );
		return p;
	}
}

// TODO SAM 2007-02-18 Need to enable NDFD Adapter
/**
Return the contents for a named property, required by the CommandProcessor
interface. Currently the following properties are handled:
<table width=100% cellpadding=10 cellspacing=0 border=2>
<tr>
<td><b>Property</b></td>	<td><b>Description</b></td>
</tr>

<td><b>CreateOutput</b></td>
<td>ndicate if output should be created, as Boolean.  If True, commands that create output
should do so.  If False, the commands should be skipped.  This is used to
speed performance during initial testing.
</td>
</tr>

<tr>
<td><b>DataTestList</b></td>
<td>The Vector of DataTests.</td>
</tr>

<tr>
<td><b>HaveOutputPeriod</b></td>
<td>Indicate whether the output period has been specified, as a Boolean.
</td>
</tr>

<tr>
<td><b>HydroBaseDMIList</b></td>
<td>A Vector of open HydroBaseDMI, available for reading.
</td>
</tr>

<tr>
<td><b>InitialWorkingDir</b></td>
<td>The initial working directory for the processor, typically the directory
where the commands file lives.
</td>
</tr>

<tr>
<td><b>InputEnd</b></td>
<td>The input end from the setInputPeriod() command, as a DateTime object.
</td>
</tr>

<tr>
<td><b>InputStart</b></td>
<td>The input start from the setInputPeriod() command, as a DateTime object.
</td>
</tr>

<tr>
<td><b>OutputComments</b></td>
<td>A Vector of String with comments suitable for output.  The comments
DO NOT contain the leading comment character (specific code that writes the
output should add the comment characters).  Currently the comments contain
open HydroBase connection information, if available.
</td>
</tr>

<tr>
<td><b>OutputEnd</b></td>
<td>The output end from the setOutputPeriod() command, as a DateTime object.
</td>
</tr>

<tr>
<td><b>OutputStart</b></td>
<td>The output start from the setOutputPeriod() command, as a DateTime object.
</td>
</tr>

<tr>
<td><b>OutputYearType</b></td>
<td>The output year type, as a String ("Calendar" or "Water", Oct-Nov).
</td>
</tr>

<tr>
<td><b>NDFDAdapterList</b></td>
<td>A Vector of available NDFDAdapter, available for reading.
THIS HAS NOT BEEN IMPLEMENTED.
</td>
</tr>

<tr>
<td><b>TSProductAnnotationProviderList</b></td>
<td>A Vector of TSProductAnnotationProvider (for example, this is requested
by the processTSProduct() command).
</td>
</tr>

<tr>
<td><b>TSResultsList</b></td>
<td>The Vector of time series results.</td>
</tr>

<tr>
<td><b>TSResultsListSize</b></td>
<td>The size of the TSResultsList as an Integer</td>
</tr>

<tr>
<td><b>TSViewWindowListener</b></td>
<td>The WindowListener that is interested in listing to TSView window events.
This is used when processing a TSProduct in batch mode so that the main
application can close when the TSView window is closed.</td>
</tr>

<tr>
<td><b>WorkingDir</b></td>
<td>The current working directory for the processor, reflecting the initial
working directory and changes from setWorkingDir() commands.
</td>
</tr>

</table>
@return the contents for a named property, or null if a value is not found.
@exception UnrecognizedRequestException if an unknown property is requested.
*/
public Object getPropContents ( String prop ) throws Exception
{	if ( prop.equalsIgnoreCase("AutoExtendPeriod") ) {
		return getPropContents_AutoExtendPeriod();
	}
	else if ( prop.equalsIgnoreCase("DataTestList") ) {
		return getPropContents_DataTestList();
	}
	else if ( prop.equalsIgnoreCase("HaveOutputPeriod") ) {
		return getPropContents_HaveOutputPeriod();
	}
	else if ( prop.equalsIgnoreCase("IncludeMissingTS") ) {
		return getPropContents_IncludeMissingTS();
	}
	else if ( prop.equalsIgnoreCase("InitialWorkingDir") ) {
		return getPropContents_InitialWorkingDir();
	}
	else if ( prop.equalsIgnoreCase("InputEnd") ) {
		return getPropContents_InputEnd();
	}
	else if ( prop.equalsIgnoreCase("InputStart") ) {
		return getPropContents_InputStart();
	}
	else if ( prop.equalsIgnoreCase("OutputComments") ) {
		return getPropContents_OutputComments();
	}
	else if ( prop.equalsIgnoreCase("OutputEnd") ) {
		return getPropContents_OutputEnd();
	}
	else if ( prop.equalsIgnoreCase("OutputStart") ) {
		return getPropContents_OutputStart();
	}
	else if ( prop.equalsIgnoreCase("OutputYearType") ) {
		return getPropContents_OutputYearType();
	}
	else if ( prop.equalsIgnoreCase("HydroBaseDMIList") ) {
		return getPropContents_HydroBaseDMIList();
	}
	else if ( prop.equalsIgnoreCase("TSProductAnnotationProviderList") ) {
		return getPropContents_TSProductAnnotationProviderList();
	}
	else if ( prop.equalsIgnoreCase("TSResultsList") ) {
		return getPropContents_TSResultsList();
	}
	else if ( prop.equalsIgnoreCase("TSResultsListSize") ) {
		return getPropContents_TSResultsListSize();
	}
	else if ( prop.equalsIgnoreCase("TSViewWindowListener") ) {
		return getPropContents_TSViewWindowListener();
	}
	else if ( prop.equalsIgnoreCase("WorkingDir") ) {
		return getPropContents_WorkingDir();
	}
	else {	String warning = "Unknown GetPropContents request \"" + prop + "\"";
			// TODO SAM 2007-02-07 Need to figure out a way to indicate
			// an error and pass back useful information.
			throw new UnrecognizedRequestException ( warning );
	}
}

/**
Handle the AutoExtendPeriod property request.
@return Boolean depending on whether time series periods should automatically
be extended during reads.
*/
private Boolean getPropContents_AutoExtendPeriod()
{
	boolean b = __tsengine.autoExtendPeriod();
	return new Boolean ( b );
}

/**
Handle the DataTestList property request.
@return Vector of DataTest instances.
 */
private Vector getPropContents_DataTestList()
{
	return __tsengine.getDataTestList();
}

/**
Handle the HaveOutputPeriod property request.
@return Boolean depending on whether the output period has been set.
 */
private Boolean getPropContents_HaveOutputPeriod()
{
	boolean b = __tsengine.haveOutputPeriod();
	return new Boolean ( b );
}

/**
Handle the HydroBaseDMIList property request.
@return Vector of open HydroBaseDMI instances.
 */
private Vector getPropContents_HydroBaseDMIList()
{
	return __tsengine.getHydroBaseDMIList();
}

/**
Handle the IncludeMissingTS property request.
@return Boolean indicating whether missing time series should be created when no data found.
 */
private Boolean getPropContents_IncludeMissingTS()
{
	boolean b = __tsengine.includeMissingTS();
	return new Boolean ( b );
}

/**
Handle the InitialWorkingDir property request.  The initial working directory is the home of the
commands file if read/saved and should be passed by the calling code when
running commands.
Use getPropContents_WorkingDir to get the working directory after processing.
@return The working directory, as a String.
 */
private String getPropContents_InitialWorkingDir()
{
	return getInitialWorkingDir();
}

/**
Handle the InputEnd property request.
@return DateTime for InputEnd, or null if not set.
 */
private DateTime getPropContents_InputEnd()
{
	return __tsengine.getInputEnd();
}

/**
Handle the InputStart property request.
@return DateTime for InputStart, or null if not set.
 */
private DateTime getPropContents_InputStart()
{
	return __tsengine.getInputStart();
}

/**
Handle the OutputComments property request.  This includes, for example,
the commands that are active and HydroBase version information that documents
data available for a commands.
run.
@return Vector of String containing comments for output.
 */
private Vector getPropContents_OutputComments()
{
	String [] array = __tsengine.formatOutputHeaderComments(getCommands());
	Vector v = new Vector();
	if ( array != null ) {
		for ( int i = 0; i < array.length; i++ ) {
			v.addElement( array[i]);
		}
	}
	return v;
}

/**
Handle the OutputEnd property request.
@return DateTime for OutputEnd, or null if not set.
 */
private DateTime getPropContents_OutputEnd()
{
	return __tsengine.getOutputEnd();
}

/**
Handle the OutputStart property request.
@return DateTime for OutputStart, or null if not set.
 */
private DateTime getPropContents_OutputStart()
{
	return __tsengine.getOutputStart();
}

/**
Handle the OutputYearType property request.
@return DateTime for OutputYearType, or null if not set.
 */
private String getPropContents_OutputYearType()
{
	return __tsengine.getOutputYearType();
}

/**
Handle the TSProductAnnotationProviderList property request.
@return The time series product annotation provider list,
as a Vector of TSProductAnnotationProvider.
 */
private Vector getPropContents_TSProductAnnotationProviderList()
{
	return __tsengine.getTSProductAnnotationProviders();
}

/**
Handle the TSResultsList property request.
@return The time series results list, as a Vector of TS.
 */
private Vector getPropContents_TSResultsList()
{
	return __tsengine.getTimeSeriesList(null);
}

/**
Handle the TSResultsListSize property request.
@return Size of the time series results list, as an Integer.
 */
private Integer getPropContents_TSResultsListSize()
{
	return new Integer( __tsengine.getTimeSeriesList(null).size());
}

/**
Handle the TSViewWindowListener property request.
@return TSViewWindowListener that listens for plot windows closing.
 */
private WindowListener getPropContents_TSViewWindowListener()
{
	return __tsengine.getTSViewWindowListener();
}

/**
Handle the WorkingDir property request.  The working directory is set based on
the initial working directory and subsequent setWorkingDir() commands.
@return The working directory, as a String.
 */
private String getPropContents_WorkingDir()
{
	return getWorkingDir();
}

/**
Return the TSSupplier name.
@return the TSSupplier name ("TSEngine").
*/
public String getTSSupplierName()
{	return __tsengine.getTSSupplierName();
}

/**
Return the initial working directory for the processor.
@return the initial working directory for the processor.
*/
public String getWorkingDir ()
{	return __WorkingDir_String;
}

/**
Determine the index of a command in the processor.  A reference comparison occurs.
@param command A command to search for in the processor.
@return the index (0+) of the matching command, or -1 if not found.
*/
public int indexOf ( Command command )
{	// Uncomment to troubleshoot
	//String routine = getClass().getName() + ".indexOf";
	int size = size();
	Command c;
	//Message.printStatus ( 2, routine, "Checking " + size + " commands for command " + command );
	for ( int i = 0; i < size; i++ ) {
		c = (Command)__Command_Vector.elementAt(i);
		//Message.printStatus ( 2, routine, "Comparing to command " + c );
		if ( c == command ) {
			//Message.printStatus ( 2, routine, "Found command." );
			return i;
		}
	}
	//Message.printStatus ( 2, routine, "Did not find command." );
	return -1;
}

/**
Add a command using the Command instance.
@param command Command to insert.
@param index Index (0+) at which to insert the command.
*/
public void insertCommandAt ( Command command, int index )
{	String routine = "TSCommandProcessor.insertCommandAt";
	__Command_Vector.insertElementAt( command, index);
	notifyCommandListListenersOfAdd ( index, index );
	Message.printStatus(2, routine, "Inserted command object \"" +
			command + "\" at [" + index + "]" );
}

/**
Add a command using the string text.  This should currently only be
used for commands that do not have command classes, which perform
additional validation on the commands.  A GenericCommand instance will
be instantiated to maintain the string and allow command status to be
set.
@param command_string Command string for command.
@param index Index (0+) at which to insert the command.
*/
public void insertCommandAt ( String command_string, int index )
{	String routine = "TSCommandProcessor.insertCommandAt";
	Command command = new GenericCommand ();
	command.setCommandString ( command_string );
	insertCommandAt ( command, index );
	Message.printStatus(2, routine, "Creating generic command from string \"" +
			command_string + "\"." );
}

/**
Return a setWorkingDir(xxx) command where xxx is the initial working directory.
This command should be prepended to the list of setWorkingDir() commands that
are processed when determining the working directory for an edit dialog or other
command-context action.
*/
private Command newInitialSetWorkingDirCommand()
{	// For now put in a generic command since no specific Command class is available...
	GenericCommand c = new GenericCommand ();
	c.setCommandString( "setWorkingDir(\"" + getInitialWorkingDir() + "\")" );
	return c;
	// TODO SAM 2007-08-22 Need to implement the command class
}

/**
Notify registered CommandListListeners about one or more commands being added.
@param index0 The index (0+) of the first command that is added.
@param index1 The index (0+) of the last command that is added.
*/
private void notifyCommandListListenersOfAdd ( int index0, int index1 )
{	if ( __CommandListListener_array != null ) {
		for ( int i = 0; i < __CommandListListener_array.length; i++ ) {
			__CommandListListener_array[i].commandAdded(index0, index1);
		}
	}
}

/**
Notify registered CommandListListeners about one or more commands being changed.
@param index0 The index (0+) of the first command that is added.
@param index1 The index (0+) of the last command that is added.
*/
private void notifyCommandListListenersOfChange ( int index0, int index1 )
{	if ( __CommandListListener_array != null ) {
		for ( int i = 0; i < __CommandListListener_array.length; i++ ) {
			__CommandListListener_array[i].commandChanged(index0, index1);
		}
	}
}

/**
Notify registered CommandListListeners about one or more commands being removed.
@param index0 The index (0+) of the first command that is added.
@param index1 The index (0+) of the last command that is added.
*/
private void notifyCommandListListenersOfRemove ( int index0, int index1 )
{	if ( __CommandListListener_array != null ) {
		for ( int i = 0; i < __CommandListListener_array.length; i++ ) {
			__CommandListListener_array[i].commandRemoved(index0, index1);
		}
	}
}

/**
Notify registered CommandProcessorListeners about a command being cancelled.
@param icommand The index (0+) of the command that is cancelled.
@param ncommand The number of commands being processed.  This will often be the
total number of commands but calling code may process a subset.
@param command The instance of the neareset command that is being cancelled.
*/
protected void notifyCommandProcessorListenersOfCommandCancelled (
		int icommand, int ncommand, Command command )
{	// This method is protected to allow TSEngine to call
	if ( __CommandProcessorListener_array != null ) {
		for ( int i = 0; i < __CommandProcessorListener_array.length; i++ ) {
			__CommandProcessorListener_array[i].commandCancelled(icommand,ncommand,command,-1.0F,"Command cancelled.");
		}
	}
}

/**
Notify registered CommandProcessorListeners about a command completing.
@param icommand The index (0+) of the command that is completing.
@param ncommand The number of commands being processed.  This will often be the
total number of commands but calling code may process a subset.
@param command The instance of the command that is completing.
*/
protected void notifyCommandProcessorListenersOfCommandCompleted (
		int icommand, int ncommand, Command command )
{	// This method is protected to allow TSEngine to call
	if ( __CommandProcessorListener_array != null ) {
		for ( int i = 0; i < __CommandProcessorListener_array.length; i++ ) {
			__CommandProcessorListener_array[i].commandCompleted(icommand,ncommand,command,-1.0F,"Command completed.");
		}
	}
}

/**
Notify registered CommandProcessorListeners about a command starting.
@param icommand The index (0+) of the command that is starting.
@param ncommand The number of commands being processed.  This will often be the
total number of commands but calling code may process a subset.
@param command The instance of the command that is starting.
*/
protected void notifyCommandProcessorListenersOfCommandStarted (
		int icommand, int ncommand, Command command )
{	// This method is protected to allow TSEngine to call
	if ( __CommandProcessorListener_array != null ) {
		for ( int i = 0; i < __CommandProcessorListener_array.length; i++ ) {
			__CommandProcessorListener_array[i].commandStarted(icommand,ncommand,command,-1.0F,"Command started.");
		}
	}
}

/**
Process a request, required by the CommandProcessor interface.
This is a generalized way to allow commands to call specialized functionality
through the interface without directly naming a processor.  For example, the
request may involve data that only the TSCommandProcessor has access to and
that a command does not.
Currently the following requests are handled:
<table width=100% cellpadding=10 cellspacing=0 border=2>
<tr>
<td><b>Request</b></td>	<td><b>Description</b></td>
</tr>

<tr>
<td><b>CalculateTSAverageLimits</b></td>
<td>Calculate the average data limits for a time series using the averaging period
	if specified (otherwise use the available period).
	Currently only limits for monthly time series are supported.
	Parameters to this request are:
<ol>
<li>	<b>TS</b> Monthly time series to process, as TS (MonthTS) object.</li>
<li>	<b>Index</b> The index (0+) of the time series identifier being processed,
		as an Integer.</l>
</ol>
Returned values from this request are:
<ol>
<li>	<b>TSLimits</b>the average data for a time series.
If a monthly time series, a MonthTSLimits will be returned.</li>
</ol>
</td>
</tr>

<tr>
<td><b>DateTime</b></td>
<td>Get a date/time from a string.  This is done using the following rules:
<ol>
<li>	If the string is "*" or "", return null.</li>
<li>	If the string uses a standard name OutputStart, OutputEnd,
		InputStart (previously QueryStart),
		InputEnd (previously QueryEnd), return the corresponding DateTime.</li>
<li>	Check the date/time hash table for user-defined date/times.
<li>	Parse the string.
</ol>
	Parameters to this request are:
<ol>
<li>	<b>DateTime</b> The date/time to parse, as a String.</li>
</ol>
Returned values from this request are:
<ol>
<li>	<b>DateTime</b>  The resulting date/time, as a DateTime object.</li>
</ol>
</td>
</tr>

<tr>
<td><b>GetHydroBaseDMI</b></td>
<td>	Return the HydroBaseDMI that is being used.
	Parameters for this request are:
	<ol>
	<li><b>InputName</b> Input name for the DMI as a String, can be blank.</li>
	</ol>
	Returned values from this request are:
	<ol>
	<li><b>HydroBaseDMI</b> The HydroBaseDMI instance matching the input name
	 			(may return null).</li>
	</ol>
</td>
</tr>

<tr>
<td><b>GetTimeSeries</b></td>
<td>Return a time series from either the __tslist vector or the BinaryTS file,
as appropriate.  If a BinaryTS is returned, it is a new instance from the file
and should be set to null when done.  Parameters to this request are:
<ol>
<li>	<b>Index</b> The index (0+) of the time series identifier being requested,
		as an Integer.</li>
</ol>
Returned values from this request are:
<ol>
<li>	<b>TS</b> The requested time series or null if none is available,
		as a TS instance.</li>
</ol>
</td>
</tr>

<tr>
<td><b>GetTimeSeriesForTSID</b></td>
<td>Return a time series from either the __tslist vector or the BinaryTS file,
as appropriate.  If a BinaryTS is returned, it is a new instance from the file
and should be set to null when done.
The search is performed backwards in the
list, assuming that the commands are being processed sequentially and therefore
any reference to a duplicate ID would intuitively be referring to the latest
instance in the list.  For this version of the method, the trace (sequence
number) is ignored.  Parameters to this request are:
<ol>
<li>	<b>TSID</b> The time series identifier of the time series being requested
		(either an alias or TSIdent string), as a String.</li>
<li>	</li>
</ol>
Returned values from this request are:
<ol>
<li>	<b>TS</b> The requested time series or null if none is available,
		as a TS instance.</li>
</ol>
</td>
</tr>

<tr>
<td><b>GetTimeSeriesToProcess</b></td>
<td>Return the list of time series to process, based on information that indicates
how the list can be determined.  Parameters to this request are:
<ol>
<li>	<b>TSList</b> Indicates how the list of time series for processing is to be
determined, with one of the following values:</li>
	<ol>
	<li>	"AllTS" will result in true being returned.</li>
	<li>	"AllMatchingTSID" will use the TSID value to match time series.</li>
	<li>	"LastMatchingTSID" will use the TSID value to match time series,
		returning the last match.  This is necessary for backward compatibility.
		</li>
	<li>	"SelectedTS" will result in true being returned only if the
		time series is selected.</li>
	</ol>
</ol>
Returned values from this request are:
<ol>
<li>	<b>TSToProcessList</b> The requested time series to process
		or null if none are available,
		as a Vector of TS.  Use the size of the Vector (in the first
		element) to determine the number of time series to process.  The order of
		the time series will be from first to last.</li>
<li>	<b>Indices</b> An int[] indicating the positions in the time series list,
		to be used to update the time series.</li>
</ol>
</td>
</tr>

<tr>
<td><b>IndexOf</b></td>
<td>Return the position of a time series from either the __tslist vector or the
	BinaryTS file, as appropriate.  See the similar method in TSEngine for full
	documentation.  This version assumes that no sequence number is used in the TSID.
	The search is performed backwards in order to find the time series from the
	most recent processed command.  Parameters to this request are:
<ol>
<li>	<b>TSID</b> The time series identifier or alias being requested,
		as a String.</li>
</ol>
Returned values from this request are:
<ol>
<li>	<b>Index</b> The index (0+) of the time series identifier being requested,
		as an Integer.</li>
</ol>
</td>
</tr>

<tr>
<td><b>ProcessCommands</b></td>
<td>Process a list of commands (recursively), for example when called by the
runCommands() command.
	Parameters to this request are:
<ol>
<li>	<b>Commands</b> A list of commands to run, as a Vector of String.</li>
<li>	<b>Properties</b> Properties to control the commands, as a PropList - note
		that all properties should be String, as per the TSEngine properties.  Valid
		properties are InitialWorkingDir=PathToWorkingDir, CreateOutput=True|False.</li>
</ol>
Returned values from this request are:
<ol>
<li>	None.</li>
</ol>
</td>
</tr>

<tr>
<td><b>ProcessTimeSeriesAction</b></td>
<td>Process a time series action, meaning insert or update in the list.
	Parameters to this request are:
<ol>
<li>	<b>Action</b> "INSERT" to insert at the position,
		"UPDATE" to update at the position,
		"NONE" to do to nothing, as a String.</li>
<li>	<b>TS</b> The time series to act on, as TS object.</li>
<li>	<b>Index</b> The index (0+) of the time series identifier being processed,
		as an Integer.</li>
</ol>
Returned values from this request are:
<ol>
<li>	None.</li>
</ol>
</td>
</tr>

<tr>
<td><b>ProcessTimeSeriesResultsList</b></td>
<td>Process the processor's time series results into a graph, file, etc.
	Parameters to this request are:
<ol>
<li>	<b>Indices</b> Indices (0+) indicating the time series to process.</li>
<li>	<b>Properties</b> Properties indicating the type of output.  These are
		currently documented internally in TSEngine.
		</ol>
Returned values from this request are:
<ol>
<li>	None - results display or are created as files.</li>
</ol>
</td>
</tr>

<tr>
<td><b>ReadTimeSeries2</b></td>
<td> Process a list of time series after the initial read.  This does NOT add the
time series to the list (use setTimeSeries() or other commands to do this).
This method should be called after time series are read.
This method does the following:
<ol>
<li>	Sets the legend to "" (this unsets legend information that was
	previously required with the old graph package).</li>
<li>	If the description is not set, sets it to the location.</li>
<li>	If a missing data range has been set, indicate it to the time series.
	This may be phased out.</li>
<li>	If the time series identifier needs to be reset to something known to
	the read code, reset it (using the non-null tsident_string parameter
	that is passed in).</li>
<li>	Compute the historic averages for the raw data so that it is available
	later for filling.</li>
<li>	If the output period is specified, make sure that the time series
	period includes the output period.  For important time series, the
	available period may already include the output period.  For time series
	that are being filled, it is likely that the available period will need
	to be extended to include the output period.</li>
</ol>
Parameters to this request are:
<ol>
<li>	<b>TSList</b> List of time series to process, as a Vector of TS.</li>
</ol>
Returned values from this request are:
<ol>
<li>	None.</li>
</ol>
</td>
</tr>

<tr>
<td><b>RunCommands</b></td>
<td>Run commands to create the results:
<ol>
<li>	<b>CommandList</b> A Vector of Command instances to run.</li>
<li>	<b>InitialWorkingDir</b> The initial working directory as a String, to initialize paths.</li>
</ol>
Returned values from this request are:
<ol>
<li>	None - time series results will contain the results.</li>
</ol>
</td>
</tr>

<tr>
<td><b>SetHydroBaseDMI</b></td>
<td>Set a HydroBaseDMI instance for use in database queries.
	Parameters to this request are:
<ol>
<li>	<b>HydroBaseDMI</b> An open HydroBaseDMI instance, where the input name
		can be used to uniquely identify the instance.</li>
</ol>
Returned values from this request are:
<ol>
<li>	None.</li>
</ol>
</td>
</tr>

<tr>
<td><b>TSIDListNoInputAboveCommand</b></td>
<td>The Vector of time series identifiers that are available (as String), without the
input type and name.  The time series identifiers from commands above the
selected command are returned.  This property will normally only be used with
command editor dialogs.  Parameters for this request are:</td>
<ol>
<li>	<b>Command</b> A Command instance, typically being edited, above which time series
identifiers are determined.</li>
</ol>
Returned values from this request are:
<ol>
<li>	<b>TSIDList</b> The list of time series identifiers as a Vector of String.</li>
</ol>
</td>
</tr>

</table>
@param request_params An optional list of parameters to be used in the request.
@exception Exception if the request cannot be processed.
@return the results of a request, or null if a value is not found.
*/
public CommandProcessorRequestResultsBean processRequest ( String request, PropList request_params )
throws Exception
{	//return __tsengine.getPropContents ( prop );
	if ( request.equalsIgnoreCase("AppendTimeSeries") ) {
		return processRequest_AppendTimeSeries ( request, request_params );
	}
	else if ( request.equalsIgnoreCase("CalculateTSAverageLimits") ) {
		return processRequest_CalculateTSAverageLimits ( request, request_params );
	}
	else if ( request.equalsIgnoreCase("DateTime") ) {
		return processRequest_DateTime ( request, request_params );
	}
	else if ( request.equalsIgnoreCase("GetHydroBaseDMI") ) {
		return processRequest_GetHydroBaseDMI ( request, request_params );
	}
	else if ( request.equalsIgnoreCase("GetTimeSeries") ) {
		return processRequest_GetTimeSeries ( request, request_params );
	}
	else if ( request.equalsIgnoreCase("GetTimeSeriesForTSID") ) {
		return processRequest_GetTimeSeriesForTSID ( request, request_params );
	}
	else if ( request.equalsIgnoreCase("GetTimeSeriesToProcess") ) {
		return processRequest_GetTimeSeriesToProcess ( request, request_params );
	}
	else if ( request.equalsIgnoreCase("GetTSIDListNoInputAboveCommand") ) {
		return processRequest_GetTSIDListNoInputAboveCommand ( request, request_params );
	}
	else if ( request.equalsIgnoreCase("GetWorkingDirForCommand") ) {
		return processRequest_GetWorkingDirForCommand ( request, request_params );
	}
	else if ( request.equalsIgnoreCase("IndexOf") ) {
		return processRequest_IndexOf ( request, request_params );
	}
	else if ( request.equalsIgnoreCase("ProcessCommands") ) {
		return processRequest_ProcessCommands ( request, request_params );
	}
	else if ( request.equalsIgnoreCase("ProcessTimeSeriesAction") ) {
		return processRequest_ProcessTimeSeriesAction ( request, request_params );
	}
	else if ( request.equalsIgnoreCase("ProcessTimeSeriesResultsList") ) {
		return processRequest_ProcessTimeSeriesResultsList ( request, request_params );
	}
	else if ( request.equalsIgnoreCase("ReadTimeSeries2") ) {
		return processRequest_ReadTimeSeries2 ( request, request_params );
	}
	else if ( request.equalsIgnoreCase("RunCommands") ) {
		return processRequest_RunCommands ( request, request_params );
	}
	else if ( request.equalsIgnoreCase("SetHydroBaseDMI") ) {
		return processRequest_SetHydroBaseDMI ( request, request_params );
	}
	else if ( request.equalsIgnoreCase("SetNWSRFSFS5FilesDMI") ) {
		return processRequest_SetNWSRFSFS5FilesDMI ( request, request_params );
	}
	else if ( request.equalsIgnoreCase("SetTimeSeries") ) {
		return processRequest_SetTimeSeries ( request, request_params );
	}
	else {
		TSCommandProcessorRequestResultsBean bean =
			new TSCommandProcessorRequestResultsBean();
		String warning = "Unknown TSCommandProcessor request \"" +
		request + "\"";
		bean.setWarningText( warning );
		Message.printWarning(3, "TSCommandProcessor.processRequest", warning);
		// TODO SAM 2007-02-07 Need to figure out a way to indicate
		// an error and pass back useful information.
		throw new UnrecognizedRequestException ( warning );
	}
}

/**
Process the AppendTimeSeries request.
*/
private CommandProcessorRequestResultsBean processRequest_AppendTimeSeries (
		String request, PropList request_params )
throws Exception
{	TSCommandProcessorRequestResultsBean bean =
		new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters...
	Object o = request_params.getContents ( "TS" );
	if ( o == null ) {
			String warning = "Request AppendTimeSeries() does not provide a TS parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	TS ts = (TS)o;
	__tsengine.appendTimeSeries ( ts );
	// No data are returned in the bean.
	return bean;
}

/**
Process the CalculateTSAverageLimits request.
*/
private CommandProcessorRequestResultsBean processRequest_CalculateTSAverageLimits (
		String request, PropList request_params )
throws Exception
{	TSCommandProcessorRequestResultsBean bean =
		new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters...
	// Time series...
	Object o_TS = request_params.getContents ( "TS" );
	if ( o_TS == null ) {
			String warning =
				"Request ProcessTimeSeriesAction() does not provide a TS parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	TS ts = (TS)o_TS;
	TSLimits tslimits = __tsengine.calculateTSAverageLimits(ts);
	// Return the limits.
	PropList results = bean.getResultsPropList();
	results.setUsingObject ( "TSLimits", tslimits );
	return bean;
}

/**
Process the DateTime request.
*/
private CommandProcessorRequestResultsBean processRequest_DateTime (
		String request, PropList request_params )
throws Exception
{	TSCommandProcessorRequestResultsBean bean =
		new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters...
	Object o = request_params.getContents ( "DateTime" );
	if ( o == null ) {
			String warning =
				"Request DateTime() does not provide a DateTime parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	String DateTime = (String)o;
	DateTime dt = __tsengine.getDateTime ( DateTime );
	PropList results = bean.getResultsPropList();
	// This will be set in the bean because the PropList is a reference...
	results.setUsingObject("DateTime", dt );
	return bean;
}

/**
Process the GetHydroBaseDMI request.
*/
private CommandProcessorRequestResultsBean processRequest_GetHydroBaseDMI (
		String request, PropList request_params )
throws Exception
{	TSCommandProcessorRequestResultsBean bean =
		new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters...
	Object o = request_params.getContents ( "InputName" );
	if ( o == null ) {
			String warning =
				"Request GetHydroBaseDMI() does not provide an InputName parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	String InputName = (String)o;
	HydroBaseDMI dmi = __tsengine.getHydroBaseDMI( InputName );
	PropList results = bean.getResultsPropList();
	// This will be set in the bean because the PropList is a reference...
	results.setUsingObject("HydroBaseDMI", dmi );
	return bean;
}

/**
Process the GetTimeSeries request.
*/
private CommandProcessorRequestResultsBean processRequest_GetTimeSeries (
		String request, PropList request_params )
throws Exception
{	TSCommandProcessorRequestResultsBean bean =
		new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters...
	Object o = request_params.getContents ( "Index" );
	if ( o == null ) {
			String warning = "Request GetTimeSeries() does not provide an Index parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	Integer Index = (Integer)o;
	TS ts = __tsengine.getTimeSeries ( Index.intValue() );
	PropList results = bean.getResultsPropList();
	// This will be set in the bean because the PropList is a reference...
	results.setUsingObject("TS", ts );
	return bean;
}

/**
Process the GetTimeSeriesForTSID request.
*/
private CommandProcessorRequestResultsBean processRequest_GetTimeSeriesForTSID (
		String request, PropList request_params )
throws Exception
{	TSCommandProcessorRequestResultsBean bean =
		new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters...
	Object o = request_params.getContents ( "TSID" );
	if ( o == null ) {
			String warning =
				"Request GetTimeSeriesForTSID() does not provide a TSID parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	String TSID = (String)o;
	o = request_params.getContents ( "CommandTag" );
	String CommandTag = "";
	if ( o != null ) {
		CommandTag = (String)o;
	}
	TS ts = __tsengine.getTimeSeries ( CommandTag, TSID );
	PropList results = bean.getResultsPropList();
	// This will be set in the bean because the PropList is a reference...
	results.setUsingObject("TS", ts );
	return bean;
}

/**
Process the GetTimeSeriesToProcess request.
*/
private CommandProcessorRequestResultsBean processRequest_GetTimeSeriesToProcess (
		String request, PropList request_params )
throws Exception
{	TSCommandProcessorRequestResultsBean bean =
		new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters...
	Object o = request_params.getContents ( "TSList" );
	if ( o == null ) {
			String warning = "Request GetTimeSeriesToProcess() does not provide a TSList parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	// Else continue...
	String TSList = (String)o;
	// The following can be null.  Let the called code handle it.
	Object o_TSID = request_params.getContents ( "TSID" );
	String TSID = null;
	if ( o_TSID != null ) {
		TSID = (String)o_TSID;
	}
	// Get the information from TSEngine, which is returned as a Vector
	// with the first element being the matching time series list and the second
	// being the indices of those time series in the time series results list.
	Vector tslist = __tsengine.getTimeSeriesToProcess ( TSList, TSID );
	PropList results = bean.getResultsPropList();
	// This will be set in the bean because the PropList is a reference...
	Message.printStatus(2,"From TSEngine",
			((Vector)(tslist.elementAt(0))).toString() );
	results.setUsingObject("TSToProcessList", (Vector)(tslist.elementAt(0)) );
	results.setUsingObject("Indices", (int [])(tslist.elementAt(1)) );
	return bean;
}

/**
Process the GetTimeSeriesForTSID request.
*/
private CommandProcessorRequestResultsBean processRequest_GetTSIDListNoInputAboveCommand (
		String request, PropList request_params )
throws Exception
{	TSCommandProcessorRequestResultsBean bean =
		new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters...
	Object o = request_params.getContents ( "Command" );
	if ( o == null ) {
			String warning =
				"Request GetTSIDListNoInputAboveCommand() does not provide a TSID parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	Command command = (Command)o;
	Vector tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand( this, command );
	PropList results = bean.getResultsPropList();
	// This will be set in the bean because the PropList is a reference...
	results.setUsingObject("TSIDList", tsids );
	return bean;
}

/**
Process the GetWorkingDirForCommand request.
*/
private CommandProcessorRequestResultsBean processRequest_GetWorkingDirForCommand (
		String request, PropList request_params )
throws Exception
{	TSCommandProcessorRequestResultsBean bean =
		new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters...
	Object o = request_params.getContents ( "Command" );
	if ( o == null ) {
			String warning = "Request GetWorkingDirForCommand() does not provide a Command parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	Command command = (Command)o;
	// Get the index of the requested command...
	int index = indexOf ( command );
	// Get the setWorkingDir() commands...
	Vector needed_commands_String_Vector = new Vector();
	needed_commands_String_Vector.addElement ( "setWorkingDir" );
	Vector setWorkingDir_CommandVector = TSCommandProcessorUtil.getCommandsBeforeIndex (
			index,
			this,
			needed_commands_String_Vector,
			false );	// Get all, not just last
	// Always add the starting working directory to the top to
	// make sure an initial condition is set...
	setWorkingDir_CommandVector.insertElementAt ( newInitialSetWorkingDirCommand(), 0 );
	// Create a local command processor
	TSCommandProcessor ts_processor = new TSCommandProcessor();
	ts_processor.setPropContents("InitialWorkingDir", getPropContents("InitialWorkingDir"));
	int size = setWorkingDir_CommandVector.size();
	// Add all the commands (currently no method to add all because this is normally
	// not done).
	for ( int i = 0; i < size; i++ ) {
		ts_processor.addCommand ( (Command)setWorkingDir_CommandVector.elementAt(i));
	}
	// Run the commands to set the working directory in the temporary processor...
	try {	ts_processor.runCommands(
			null,	// Process all commands in this processor
			null );	// No need for controlling properties since controlled by commands
	}
	catch ( Exception e ) {
		// This is a software problem.
		String routine = getClass().getName() + ".processRequest_GetWorkingDirForCommand";
		Message.printWarning(2, routine, "Error getting working directory for command." );
		Message.printWarning(2, routine, e);
	}
	// Return the working directory as a String.  This can then be used in editors, for
	// example.  The WorkingDir property will have been set in the temporary processor.
	PropList results = bean.getResultsPropList();
	results.set( "WorkingDir", (String)ts_processor.getPropContents ( "WorkingDir") );
	return bean;
}

/**
Process the IndexOf request.
*/
private CommandProcessorRequestResultsBean processRequest_IndexOf (
		String request, PropList request_params )
throws Exception
{	TSCommandProcessorRequestResultsBean bean =
		new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters...
	Object o = request_params.getContents ( "TSID" );
	if ( o == null ) {
			String warning = "Request IndexOf() does not provide a TSID parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	String TSID = (String)o;
	int index = __tsengine.indexOf ( TSID );
	PropList results = bean.getResultsPropList();
	// This will be set in the bean because the PropList is a reference...
	results.setUsingObject("Index", new Integer(index));
	return bean;
}

/**
Process the ProcessTimeSeriesAction request.
*/
private CommandProcessorRequestResultsBean processRequest_ProcessCommands (
		String request, PropList request_params )
throws Exception
{	TSCommandProcessorRequestResultsBean bean =
		new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters...
	// Time series...
	Object o_Commands = request_params.getContents ( "Commands" );
	if ( o_Commands == null ) {
			String warning =
				"Request ProcessCommands() does not provide a Commands parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	Vector commands = (Vector)o_Commands;
	Object o_Properties = request_params.getContents ( "Properties" );
	if ( o_Properties == null ) {
			String warning =
				"Request ProcessCommands() does not provide a Properties parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	__tsengine.processCommands( commands, (PropList)o_Properties );
	// No results need to be set in the bean.
	return bean;
}

/**
Process the ProcessTimeSeriesAction request.
*/
private CommandProcessorRequestResultsBean processRequest_ProcessTimeSeriesAction (
		String request, PropList request_params )
throws Exception
{	TSCommandProcessorRequestResultsBean bean =
		new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters...
	// Time series...
	Object o_TS = request_params.getContents ( "TS" );
	if ( o_TS == null ) {
			String warning =
				"Request ProcessTimeSeriesAction() does not provide a TS parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	TS ts = (TS)o_TS;
	// Action...
	Object o_Action = request_params.getContents ( "Action" );
	if ( o_Action == null ) {
			String warning =
				"Request ProcessTimeSeriesAction() does not provide an Action parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	String Action = (String)o_Action;
	// TODO SAM 2007-02-11 Need to handle actions as strings cleaner.
	int Action_int = NONE;
	if ( Action.equalsIgnoreCase("Insert") ) {
		Action_int = INSERT_TS;
	}
	else if ( Action.equalsIgnoreCase("Update") ) {
		Action_int = UPDATE_TS;
	}
	else { String warning = "Request ProcessTimeSeriesAction() Action value \"" +
			Action + "\" is invalid.";
			throw new RequestParameterInvalidException ( warning );
	}
	// Index...
	Object o_Index = request_params.getContents ( "Index" );
	if ( o_Index == null ) {
			String warning =
				"Request ProcessTimeSeriesAction() does not provide an Index parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	Integer Index = (Integer)o_Index;
	__tsengine.processTimeSeriesAction( Action_int, ts, Index.intValue() );
	// No results need to be set in the bean.
	return bean;
}

/**
Process the ProcessTimeSeriesResultsList request.
This processes the time series into a product such as a graph or report.
*/
private CommandProcessorRequestResultsBean processRequest_ProcessTimeSeriesResultsList (
		String request, PropList request_params )
throws Exception
{	TSCommandProcessorRequestResultsBean bean =
		new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters...
	// Indices for time series...
	Object o_Indices = request_params.getContents ( "Indices" );
	int [] Indices_array = null;
	if ( o_Indices == null ) {
		/* OK FOR NOW since null means process all
			String warning =
				"Request ProcessTimeSeriesResultsList() does not provide an Indices parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
			*/
	}
	else {
		Indices_array = (int [])o_Indices;
	}
	// Properties for processing, as per legacy code...
	Object o_Properties = request_params.getContents ( "Properties" );
	PropList Properties = null;
	if ( o_Properties == null ) {
			/* OK for now - use defaults
			String warning =
				"Request ProcessTimeSeriesResultsList() does not provide a Properties parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
			*/
	}
	else {
		Properties = (PropList)o_Properties;
	}
	__tsengine.processTimeSeries( Indices_array, Properties );
	// No results need to be set in the bean.
	return bean;
}

/**
Process the ReadTimeSeries2 request.
*/
private CommandProcessorRequestResultsBean processRequest_ReadTimeSeries2 (
		String request, PropList request_params )
throws Exception
{	TSCommandProcessorRequestResultsBean bean =
		new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters...
	Object o = request_params.getContents ( "TSList" );
	if ( o == null ) {
			String warning = "Request ReadTimeSeries2() does not provide a TSList parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	Vector TSList = (Vector)o;
	__tsengine.readTimeSeries2 ( TSList );
	//PropList results = bean.getResultsPropList();
	// No data are returned in the bean.
	return bean;
}

/**
Process the RunCommands request.
*/
private CommandProcessorRequestResultsBean processRequest_RunCommands (
		String request, PropList request_params )
throws Exception
{	TSCommandProcessorRequestResultsBean bean =
		new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters...
	// Command list.
	Object o = request_params.getContents ( "CommandList" );
	if ( o == null ) {
			String warning =
				"Request RunCommands() does not provide a CommandList parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	Vector commands = (Vector)o;
	// Whether commands should create output...
	Object o3 = request_params.getContents ( "CreateOutput" );
	if ( o3 == null ) {
			String warning =
				"Request RunCommands() does not provide a CreateOutput parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	Boolean CreateOutput_Boolean = (Boolean)o3;
	// Set properties as per the legacy application.
	PropList props = new PropList ( "TSEngine");
	props.set ( "CreateOutput", "" + CreateOutput_Boolean );
	// TODO SAM 2007-08-22 Need to evaluate recursion for complex workflow and testing
	// Call the TSEngine method.
	// TODO SAM 2007-08-22 Consider threading to improve perforamance and interaction
	runCommands ( commands, props );
	// No results need to be returned.
	return bean;
}

/**
Process the SetHydroBaseDMI request.
*/
private CommandProcessorRequestResultsBean processRequest_SetHydroBaseDMI (
		String request, PropList request_params )
throws Exception
{	TSCommandProcessorRequestResultsBean bean =
		new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters...
	Object o = request_params.getContents ( "HydroBaseDMI" );
	if ( o == null ) {
			String warning =
				"Request SetHydroBaseDMI() does not provide a HydroBaseDMI parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	HydroBaseDMI dmi = (HydroBaseDMI)o;
	// Add an open HydroBaseDMI instance, closing a previous connection
	// of the same name if it exists.
	__tsengine.setHydroBaseDMI( dmi, true );
	// No results need to be returned.
	return bean;
}

/**
Process the SetNWSRFSFS5FilesDMI request.
*/
private CommandProcessorRequestResultsBean processRequest_SetNWSRFSFS5FilesDMI (
		String request, PropList request_params )
throws Exception
{	TSCommandProcessorRequestResultsBean bean =
		new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters...
	Object o = request_params.getContents ( "NWSRFSFS5FilesDMI" );
	if ( o == null ) {
			String warning =
				"Request SetNWSRFSFS5FilesDMI() does not provide a NWSRFSFS5FilesDMI parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	NWSRFS_DMI dmi = (NWSRFS_DMI)o;
	// Add an open NWSRFS_DMI instance, closing a previous connection
	// of the same name if it exists.
	__tsengine.setNWSRFSFS5FilesDMI( dmi, true );
	// No results need to be returned.
	return bean;
}

/**
Process the SetTimeSeries request.
*/
private CommandProcessorRequestResultsBean processRequest_SetTimeSeries (
		String request, PropList request_params )
throws Exception
{	TSCommandProcessorRequestResultsBean bean =
		new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters...
	Object o = request_params.getContents ( "TS" );
	if ( o == null ) {
			String warning = "Request SetTimeSeries() does not provide a TS parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	TS ts = (TS)o;
	Object o2 = request_params.getContents ( "Index" );
	if ( o2 == null ) {
			String warning = "Request SetTimeSeries() does not provide an Index parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	Integer Index = (Integer)o2;
	__tsengine.setTimeSeries ( ts, Index.intValue() );
	//PropList results = bean.getResultsPropList();
	// No data are returned in the bean.
	return bean;
}

/**
Read the commands file and initialize new commands.
@param path Path to the commands file - this should be an absolute path.
@param create_generic_command_if_not_recognized If true, create a GenericCommand
if the command is not recognized.  This is being used during transition of old
string commands to full Command classes.
@param append If true, the commands will be appended to the existing commands.
@exception IOException if there is a problem reading the file.
@exception FileNotFoundException if the specified commands file does not exist.
*/
public void readCommandsFile ( String path,
		boolean create_generic_command_if_not_recognized,
		boolean append )
throws IOException, FileNotFoundException
{	//String routine = getClass().getName() + ".readCommandsFile";
	BufferedReader br = null;
	br = new BufferedReader( new FileReader(path) );
	File path_File = new File(path);
	setInitialWorkingDir ( path_File.getParent() );
	String line;
	Command command = null;
	TSCommandFactory cf = new TSCommandFactory();
	// Use this to control whether listeners should be notified for each
	// insert.  Why would this be done?  If, for example, a GUI should display
	// the progress in reading/initializing the commands.
	//
	// Why would this not be done?  Becuse of performance issues.
	boolean notify_listeners_for_each_add = true;
	// If not appending, remove all...
	if ( !append ) {
		removeAllCommands();
	}
	// Now process each line in the file and turn into a command...
	int num_added = 0;
	while ( true ) {
		line = br.readLine();
		if ( line == null ) {
			break;
		}
		// Create a command from the line...
		if ( create_generic_command_if_not_recognized ) {
			try {	command = cf.newCommand ( line, create_generic_command_if_not_recognized );
			}
			catch ( UnknownCommandException e ) {
				// Should not happen because of parameter passed above
			}
		}
		else {
			try {	command = cf.newCommand ( line, create_generic_command_if_not_recognized );
			}
			catch ( UnknownCommandException e ) {
				// TODO SAM 2007-09-08 Evaluate how to handle unknown commands at load without stopping the load
				// In this case skip the command, although the above case
				// may always be needed?
			}
		}
		// Initialize the command...
		try {
			command.initializeCommand(
				line,	// Command string, needed to do full parse on parameters
				this,	// Processor, needed to make requests
				true);	// Do full initialization
		}
		catch ( InvalidCommandSyntaxException e ) {
			// TODO SAM 2007-09-08 Need to decide whether to handle here or in command with CommandStatus
		}
		catch ( InvalidCommandParameterException e2) {
			// TODO SAM 2007-09-08 Need to decide whether to handle here or in command with CommandStatus
		}
		// TODO SAM 2007-10-09 Evaluate whether to call listeners each time.
		// Could be good to indicate progress of load
		// Add the command, without notifying listeners of changes...
		addCommand ( command, notify_listeners_for_each_add );
		++num_added;
	}
	// Close the file...
	br.close();
	// Now notify listeners about the add one time (only need to do if it
	// was not getting done for each add...
	if ( !notify_listeners_for_each_add ) {
		notifyCommandListListenersOfAdd ( 0, (num_added - 1) );
	}
}

/**
Method for TSSupplier interface.
Read a time series given a time series identifier string.  The string may be
a file name if the time series are stored in files, or may be a true identifier
string if the time series is stored in a database.  The specified period is
read.  The data are converted to the requested units.
@param tsident_string Time series identifier or file name to read.
@param req_date1 First date to query.  If specified as null the entire period
will be read.
@param req_date2 Last date to query.  If specified as null the entire period
will be read.
@param req_units Requested units to return data.  If specified as null or an
empty string the units will not be converted.
@param read_data if true, the data will be read.  If false, only the time series
header will be read.
@return Time series of appropriate type (e.g., MonthTS, HourTS).
@exception Exception if an error occurs during the read.
*/
public TS readTimeSeries (	String tsident_string,
				DateTime req_date1, DateTime req_date2,
				String req_units,
				boolean read_data )
throws Exception
{	return __tsengine.readTimeSeries ( tsident_string, req_date1,
		req_date2, req_units, read_data );
}

/**
Method for TSSupplier interface.
Read a time series given an existing time series and a file name.
The specified period is read.
The data are converted to the requested units.
@param req_ts Requested time series to fill.  If null, return a new time series.
If not null, all data are reset, except for the identifier, which is assumed
to have been set in the calling code.  This can be used to query a single
time series from a file that contains multiple time series.
@param fname File name to read.
@param date1 First date to query.  If specified as null the entire period will
be read.
@param date2 Last date to query.  If specified as null the entire period will
be read.
@param req_units Requested units to return data.  If specified as null or an
empty string the units will not be converted.
@param read_data if true, the data will be read.  If false, only the time series
header will be read.
@return Time series of appropriate type (e.g., MonthTS, HourTS).
@exception Exception if an error occurs during the read.
*/
public TS readTimeSeries (	TS req_ts, String fname,
				DateTime date1, DateTime date2,
				String req_units,
				boolean read_data )
throws Exception
{	return __tsengine.readTimeSeries ( req_ts, fname, date1, date2,
			req_units, read_data );
}

/**
Method for TSSupplier interface.
Read a time series list from a file (this is typically used used where a time
series file can contain one or more time series).
The specified period is
read.  The data are converted to the requested units.
@param fname File to read.
@param date1 First date to query.  If specified as null the entire period will
be read.
@param date2 Last date to query.  If specified as null the entire period will
be read.
@param req_units Requested units to return data.  If specified as null or an
empty string the units will not be converted.
@param read_data if true, the data will be read.  If false, only the time series
header will be read.
@return Vector of time series of appropriate type (e.g., MonthTS, HourTS).
@exception Exception if an error occurs during the read.
*/
public Vector readTimeSeriesList (	String fname,
					DateTime date1, DateTime date2,
					String req_units,
					boolean read_data )
throws Exception
{	return __tsengine.readTimeSeriesList ( fname, date1, date2, req_units,
				read_data );
}

/**
Method for TSSupplier interface.
Read a time series list from a file or database using the time series identifier
information as a query pattern.
The specified period is
read.  The data are converted to the requested units.
@param tsident A TSIdent instance that indicates which time series to query.
If the identifier parts are empty, they will be ignored in the selection.  If
set to "*", then any time series identifier matching the field will be selected.
If set to a literal string, the identifier field must match exactly to be
selected.
@param fname File to read.
@param date1 First date to query.  If specified as null the entire period will
be read.
@param date2 Last date to query.  If specified as null the entire period will
be read.
@param req_units Requested units to return data.  If specified as null or an
empty string the units will not be converted.
@param read_data if true, the data will be read.  If false, only the time series
header will be read.
@return Vector of time series of appropriate type (e.g., MonthTS, HourTS).
@exception Exception if an error occurs during the read.
*/
public Vector readTimeSeriesList (	TSIdent tsident, String fname,
					DateTime date1, DateTime date2,
					String req_units,
					boolean read_data )
throws Exception {
	return __tsengine.readTimeSeriesList ( tsident, fname, date1, date2,
				req_units, read_data );
}

/**
Remove all commands.
*/
public void removeAllCommands ()
{	int size = __Command_Vector.size();
	if ( size > 0 ) {
		__Command_Vector.removeAllElements ();
		notifyCommandListListenersOfRemove ( 0, size - 1 );
	}
}

/**
Remove a command at a position.
@param index Position (0+) at which to remove command.
*/
public void removeCommandAt ( int index )
{	String routine = "TSCommandProcessor.removeCommandAt";
	__Command_Vector.removeElementAt ( index );
	notifyCommandListListenersOfRemove ( index, index );
	Message.printStatus(2, routine, "Remove command object at [" + index + "]" );
}

/**
Remove a CommandListListener.
@param listener CommandListListener to remove.
*/
public void removeCommandListListener ( CommandListListener listener )
{	if ( listener == null ) {
		return;
	}
	if ( __CommandListListener_array != null ) {
		// Loop through and set to null any listeners that match the
		// requested listener...
		int size = __CommandListListener_array.length;
		int count = 0;
		for ( int i = 0; i < size; i++ ) {
			if (	(__CommandListListener_array[i] != null) &&
				(__CommandListListener_array[i] == listener) ) {
				__CommandListListener_array[i] = null;
			}
			else {	++count;
			}
		}
		// Now resize the listener array...
		CommandListListener [] newlisteners =
			new CommandListListener[count];
		count = 0;
		for ( int i = 0; i < size; i++ ) {
			if ( __CommandListListener_array[i] != null ) {
				newlisteners[count++] = __CommandListListener_array[i];
			}
		}
		__CommandListListener_array = newlisteners;
		newlisteners = null;
	}
}

/**
Run the specified commands.  If no commands are specified, run all that are being managed.
@param commands Vector of Command to process.
@param props Properties to control run, including:  InitialWorkingDir=PathToWorkingDir, CreateOutput=True|False.
*/
public void runCommands ( Vector commands, PropList props )
throws Exception
{
	__tsengine.processCommands ( commands, props );
}

/**
Request that processing be cancelled.  This sets a flag that is detected in the
TSEngine.processCommands() method.  Processing will be cancelled as soon as the current command
completes its processing.
@param cancel_processing_requested Set to true to cancel processing.
*/
public void setCancelProcessingRequested ( boolean cancel_processing_requested )
{	__cancel_processing_requested = cancel_processing_requested;
}

/**
Set the name of the commands file where the commands are saved.
@param filename Name of commands file (should be absolute since
it will be used in output headers).
*/
public void setCommandsFileName ( String filename )
{
	__commands_filename = filename;
}

/**
Set the initial working directory for the processor.  This is typically the location
of the commands file, or a temporary directory if the commands have not been saved.
Also set the current working directory by calling setWorkingDir() with the same
information.
@param WorkingDir The current working directory.
*/
public void setInitialWorkingDir ( String InitialWorkingDir )
{
	__InitialWorkingDir_String = InitialWorkingDir;
	setWorkingDir ( __InitialWorkingDir_String );
}

/**
Indicate whether the processor is running.  This should be set in processCommands()
and can be monitored by code (e.g., GUI) that has behavior that depends on whether
the processor is running.  The method is protected to allow it to be called from
TSEngine but would normally not be called from other code.
@param is_running indicates whether the processor is running (processing commands).
*/
protected void setIsRunning ( boolean is_running )
{
	__is_running = is_running;
}

/**
Set the data for a named property, required by the CommandProcessor
interface.  See the getPropContents method for a list of properties that are
handled.  This method simply calls setPropContents () using the information in
the Prop instance.
@param prop Property to set.
@return the named property, or null if a value is not found.
@exception Exception if there is an error setting the property.
*/
public void setProp ( Prop prop ) throws Exception
{	setPropContents ( prop.getKey(), prop.getContents() );
}

/**
Set the contents for a named property, required by the CommandProcessor
interface.  The following properties are handled.
<table width=100% cellpadding=10 cellspacing=0 border=2>
<tr>
<td><b>Property</b></td>	<td><b>Description</b></td>
</tr>

<tr>
<td><b>DataTestList</b></td>
<td>A Vector of DataTest, to be processed when evaluating data.
</td>
</tr>

<tr>
<td><b>HydroBaseDMIList</b></td>
<td>A Vector of open HydroBaseDMI, to be used by other code for reading data.
</td>
</tr>

<tr>
<td><b>InitialWorkingDir</b></td>
<td>A String containing the path to the initial working directory, from which all
paths are determined.  This is usually the directory to the commands file, or the
startup directory.
</td>
</tr>

<tr>
<td><b>InputEnd</b></td>
<td>The date/time for the end of reading data, as a DateTime.
</td>
</tr>

<tr>
<td><b>InputStart</b></td>
<td>The date/time for the start of reading data, as a DateTime.
</td>
</tr>

<tr>
<td><b>TSResultsList</b></td>
<td>The list of time series results, as a Vector of TS.
</td>
</tr>

</table>
@exception Exception if there is an error setting the properties.
*/
public void setPropContents ( String prop, Object contents ) throws Exception
{	if ( prop.equalsIgnoreCase("DataTestList" ) ) {
		__tsengine.setDataTestList ( (Vector)contents );
	}
	else if ( prop.equalsIgnoreCase("HydroBaseDMIList" ) ) {
		__tsengine.setHydroBaseDMIList ( (Vector)contents );
	}
	else if ( prop.equalsIgnoreCase("InitialWorkingDir" ) ) {
		setInitialWorkingDir ( (String)contents );
	}
	else if ( prop.equalsIgnoreCase("InputEnd") ) {
		__tsengine.setInputEnd ( (DateTime)contents );
	}
	else if ( prop.equalsIgnoreCase("InputStart") ) {
		__tsengine.setInputStart ( (DateTime)contents );
	}
	else if ( prop.equalsIgnoreCase("TSResultsList") ) {
		__tsengine.setTimeSeriesList ( (Vector)contents );
	}
	else {// Not recognized...
		String message = "Unable to set data for unknown property \"" +
		prop +	"\".";
		throw new UnrecognizedRequestException ( message );
	}
}

/**
Set the working directory for the processor.  This is typically set by
setInitialWorkingDir() and setWorkingDir() commands.
@param WorkingDir The current working directory.
*/
public void setWorkingDir ( String WorkingDir )
{
	__WorkingDir_String = WorkingDir;
}

/**
Return the number of commands being managed by this processor.  This
matches the Collection interface, although that is not yet fully implemented.
@return The number of commands being managed by the processor
*/
public int size()
{
	return __Command_Vector.size();
}

}
