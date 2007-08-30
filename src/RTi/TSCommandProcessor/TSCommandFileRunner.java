package RTi.TSCommandProcessor;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import RTi.Util.Message.Message;

/**
This class allows a commands file to be be run.  For example, it can be
used to make a batch run of a commands file.  An instance of TSCommandProcessor
is created to process the commands.
*/
public class TSCommandFileRunner {

private TSCommandProcessor __processor = new TSCommandProcessor();

/**
Read the commands from a file.
@param filename Name of command file to run, should be absolute.
*/
public void readCommandFile ( String path )
throws FileNotFoundException, IOException
{	String routine = "TSCommandFileRunner.readCommandFile";
	BufferedReader br = null;
	try {	br = new BufferedReader( new FileReader(path) );
	}
	catch ( Exception e ) {
		// Error opening the file (should not happen but maybe
		// a read permissions problem)...
		String message = "Error opening file \"" + path + "\"";
		Message.printWarning ( 2, routine, message );
		Message.printWarning ( 2, routine, e );
		throw new FileNotFoundException ( message );
	}
	// Successfully have a file so now go ahead and remove
	// the list contents and update the list...
	__processor.removeAllCommands ();
	//setCommandsFileName(path);
	String line;
	TSCommandFactory cf = new TSCommandFactory();
	try {
		while ( true ) {
			line = br.readLine();
			if ( line == null ) {
				break;
			}
			// Add the command in all cases.  Validation will occur
			// when trying to run.
			__processor.addCommand ( cf.newCommand(line,true));
		}			
		br.close();
	}
	catch ( Exception e ) {
		String message = "Error reading from file \"" + path + "\"";
		Message.printWarning (2, routine, message );
		Message.printWarning (2, routine, e );
		throw new IOException ( message );
	}
}

/**
Run the commands.
*/
public void runCommands ()
throws Exception
{
	__processor.runCommands(null,null);
}

}
