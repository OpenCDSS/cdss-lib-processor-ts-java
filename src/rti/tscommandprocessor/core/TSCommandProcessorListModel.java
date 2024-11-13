// TSCommandProcessorListModel - This class provides a way for the Swing JList and other components to
// display time series commands that are managed in a TSCommandProcessor.

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

package rti.tscommandprocessor.core;

import javax.swing.AbstractListModel;

import RTi.Util.IO.Command;
import RTi.Util.IO.CommandListListener;

/**
This class provides a way for the Swing JList and other components to display commands that are managed in a TSCommandProcessor.
It allows the TSCommandProcessor to be the data model (contain commands) for the UI components.
Therefore, there is a single data model for UI and processing.
*/
@SuppressWarnings("serial")
public class TSCommandProcessorListModel extends AbstractListModel<Object>
implements CommandListListener
{

/**
The TSCommand processor that the list model maps to.
*/
private TSCommandProcessor __processor;

/**
 * Whether to ignore events, used to optimize bulk operations like loading a command file.
 */
private boolean ignoreEvents = false;

/**
Constructor for ListModel for TSCommandProcessor instance.
@param TSCommandProcessor processor A TSCommandProcessor instance that can be displayed in a JList or other list via this ListModel.
*/
public TSCommandProcessorListModel ( TSCommandProcessor processor ) {
	this.__processor = processor;
	processor.addCommandListListener ( this );
}

/**
Add a command at the end of the list.
@param command_string Command string for command.
*/
public void addElement ( Command command ) {
	this.__processor.addCommand ( command );
}

/**
Add a command at the end of the list using the string text.
This should currently only be used for commands that do not have command classes, which perform additional validation on the commands.
A GenericCommand instance will be instantiated to maintain the string and allow command status to be set.
@param command_string Command string for command.
*/
public void addElement ( String command_string ) {
	this.__processor.addCommand ( command_string );
}

/**
Called when one or more commands have been added in the TSCommandProcessor,
will notify the UI list of the change.
@param index0 The index (0+) of the first command that is added.
@param index1 The index (0+) of the last command that is added.
*/
public void commandAdded ( int index0, int index1 ) {
	if ( this.ignoreEvents ) {
		// Don't pass on events to the UI.
		return;
	}
	fireIntervalAdded ( this, index0, index1 );
}

/**
Called when one or more commands have changed in the TSCommandProcessor, for example in a change in definition or status,
will notify the UI list of the change.
@param index0 The index (0+) of the first command that is changed.
@param index1 The index (0+) of the last command that is changed.
*/
public void commandChanged ( int index0, int index1 ) {
	if ( this.ignoreEvents ) {
		// Don't pass on events to the UI.
		return;
	}
	fireContentsChanged ( this, index0, index1 );
}

/**
Handle when one or more commands have been removed in the TSCommandProcessor,
will notify the UI list of the change.
@param index0 The index (0+) of the first command that is removed.
@param index1 The index (0+) of the last command that is removed.
*/
public void commandRemoved ( int index0, int index1 ) {
	if ( this.ignoreEvents ) {
		// Don't pass on events to the UI.
		return;
	}
	fireIntervalRemoved ( this, index0, index1 );
}

/**
Finalize the class before garbage collection.
*/
protected void finalize ()
throws Throwable {
	// Remove the listener from the processor.
	this.__processor.removeCommandListListener ( this );
	super.finalize();
}

/**
Get the Command at the requested position.  This simply calls get().
@param pos Command position, 0+.
@return the Command instance at the requested position.
*/
public Object get ( int pos ) {
	return this.__processor.get ( pos );
}

/**
Get the Command at the requested position.
@param pos Command position, 0+.
@return the Command instance at the requested position.
*/
public Object getElementAt ( int pos ) {
	return get ( pos );
}

/**
Get the number of Command objects being managed by the TSCommandProcessor.
@return the number of commands being managed by the command processor.
*/
public int getSize() {
	return this.__processor.size();
}

/**
Add a command using the string text.
This should currently only be used for commands that do not have command classes,
which perform additional validation on the commands.
A GenericCommand instance will be instantiated to maintain the string and allow command status to be set.
@param command_string Command string for command.
@param index Position (0+) at which to add the command.
*/
public void insertElementAt ( String command_string, int index ) {
	this.__processor.insertCommandAt ( command_string, index );
}

/**
Add a command using a Command instance, for example as created from TSCommandFactory.
@param command Command to add.
@param index Position (0+) at which to add the command.
*/
public void insertElementAt ( Command command, int index ) {
	this.__processor.insertCommandAt ( command, index );
}

/**
Remove all commands.
*/
public void removeAllElements ( ) {
	this.__processor.removeAllCommands ();
}

/**
Remove a command at an index.
@param index Position (0+) at which to remove the command.
*/
public void removeElementAt ( int index ) {
	this.__processor.removeCommandAt ( index );
}

/**
 * Whether to ignore sending events to the UI data model.
 * @param ignoreEvents whether to ignore sending events to the UI data model
 */
public void setIgnoreEvents ( boolean ignoreEvents ) {
	this.ignoreEvents = ignoreEvents;
}

/**
Get the number of Command objects being managed by the TSCommandProcessor.  This method calls getSize().
@return the number of commands being managed by the command processor.
*/
public int size() {
	return getSize();
}

}