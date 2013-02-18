package rti.tscommandprocessor.core;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
This class allows a commands file to be be run.  For example, it can be
used to make a batch run of a commands file.  An instance of TSCommandProcessor
is created to process the commands.
*/
public class TSCommandFileRunner
{

/**
The TSCommandProcessor instance that is used to run the commands.
*/
private TSCommandProcessor __processor = new TSCommandProcessor();

/**
Read the commands from a file.
@param filename name of command file to run, should be absolute.
@param runDiscoveryOnLoad indicates whether to run discovery mode on commands when loading (this is
a noticable performance hit for large command files)
*/
public void readCommandFile ( String path, boolean runDiscoveryOnLoad )
throws FileNotFoundException, IOException
{	__processor.readCommandFile (
		path, // InitialWorkingDir will be set to commands file location
		true, // Create GenericCommand instances for unknown commands
		false, // Do not append the commands.
		runDiscoveryOnLoad );
}

/**
Run the commands.
*/
public void runCommands ()
throws Exception
{
	__processor.runCommands(
			null, // Subset of Command instances to run - just run all
			null ); // Properties to control run
}

/**
Return the command processor used by the runner.
@return the command processor used by the runner
*/
public TSCommandProcessor getProcessor() {
    return __processor;
}

}