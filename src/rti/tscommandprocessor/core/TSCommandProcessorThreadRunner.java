// TSCommandProcessorThreadRunner - This class runs a TSCommandProcessor on a thread.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2025 Colorado Department of Natural Resources

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

import RTi.Util.IO.PropList;

/**
This class runs a TSCommandProcessor on a thread.
Its main purpose is to help provide cancel/pause/feedback features for UI applications.
Currently the run() method calls the TSCommandProcessor runCommands() method to run all commands.
Any registered listeners on the command processor will be notified.
*/
public class TSCommandProcessorThreadRunner implements Runnable
{

/**
The TSCommandProcessor instance that is used to run the commands (no run parameters).
*/
private TSCommandProcessor processor = null;

/**
The run request parameters used to run commands, when specifying run parameters.
*/
private PropList requestParams = null;

// TODO smalers 2021-07-29 remove when tested out - initial properties are in the processor
/**
 * Processor properties.
 */
//private PropList processorProps = null;

/**
Construct a TSCommandProcessorThreadRunner using a TSCommandProcessor instance.
When run() is called, all commands will be run using the working directory from the commands file that was originally read.
*/
public TSCommandProcessorThreadRunner ( TSCommandProcessor processor ) {
	this.processor = processor;
}

/**
Construct a TSCommandProcessorThreadRunner using a TSCommandProcessor instance.
When run() is called, all commands will be run using properties as shown.
Properties are passed to the TSCommandProcessor.processRequest("RunCommands") method,
which recognizes parameters as per the following pseudocode:
<pre>
PropList request_params = new PropList ( "" );
requestParams.setUsingObject ( "CommandList", List<Command> commands );
requestParams.setUsingObject ( "InitialWorkingDir", String getInitialWorkingDir() );
requestParams.setUsingObject ( "CreateOutput", new Boolean(create_output) );
</pre>
@param the TSCommandProcessor instance to run
@param requestParams the processor "RunCommands" request properties
*/
public TSCommandProcessorThreadRunner ( TSCommandProcessor processor, PropList requestParams ) {
	this.processor = processor;
	this.requestParams = requestParams;
}

/**
Run the commands in the current command processor.
*/
public void run () {
	try {
		if ( this.requestParams == null ) {
			this.processor.runCommands(
					null, // List of Command instances to run.  If null, run all.
					null ); // Properties to control run.
		}
		else {
			// Run the commands list passed in as a list in this.requestParams.
			this.processor.processRequest(
				"RunCommands",
				this.requestParams);
		}
		// Clean up.
		this.processor = null;
		this.requestParams = null;
	}
	catch ( Exception e ) {
		// FIXME SAM 2007-10-10 Need to handle exception in run.
	}
}

}