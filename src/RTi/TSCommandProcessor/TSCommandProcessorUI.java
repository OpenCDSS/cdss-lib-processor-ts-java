//
// History:
//
// 2007-02-08   Steven A. Malers, RTi   Javadoc the interface.

package RTi.TSCommandProcessor;

import java.util.Vector;

/**
Interface to define methods that allow the TSCommandProcessor to interact
with a user interface.  This is currently in place to bridge functionality
previously implemented in TSTool.
*/
public interface TSCommandProcessorUI
{
	
/**
Quit the program (e.g., from an exit command).
@param exitStatus The exit status to for the application.
0=success.  Non-zero means failure (see lot file).
*/
void quitProgram( int exitStatus );

/**
Return the commands above a selected command.  This allows, in TSTool for
example, the determination of previous commands so that they can be
examined for time series results (usually as time series identifiers).
*/
Vector getCommandsAboveSelected();

/**
Tell the main application to set its cursor, for example to wait on the
command processor.
@param wait If true, then the application should indicate that it is waiting
on the command processor.  If false, it is not waiting.
*/
void setWaitCursor ( boolean wait );

}
