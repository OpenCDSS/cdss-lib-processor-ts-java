//------------------------------------------------------------------------------
// TSCommandFactory - a factory to create Commands given a command name
//------------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
//------------------------------------------------------------------------------
// History:
//
// 2005-04-29	Steven A. Malers, RTi	Initial version with fillRegression.
// 2005-05-09	SAM, RTi		Add sortTimeSeries.
// 2005-05-10	SAM, RTi		Add compareTimeSeries.
// 2005-05-13	SAM, RTi		Add startLog.
// 2005-05-15	SAM, RTi		Add readNWSCard.
// 2005-05-17	SAM, RTi		Add fillHistMonthAverage.
// 2005-05-18	SAM, RTi		Add fillHistYearAverage.
// 2005-05-19	SAM, RTi		Move from TSTool package.
// 2005-05-23	SAM, RTi		Add analyzePattern.
// 2005-05-25	SAM, RTi		Add changeInterval.
// 2005-05-25	Luiz Teixeira, RTi	Add fillMixedStation.
// 2005-05-30	SAM, RTi		Add writeRiverWare.
// 2005-06-08	SAM, RTi		Add openHydroBase.
// 2005-07-11	SAM, RTi		Add lagK.
// 2005-08-24	SAM, RTi		Add scale.
// 2005-08-25	SAM, RTi		Add copy.
// 2005-08-30	SAM, RTi		Add writeStateMod.  This depends on the
//					DWR.StateMod package.
// 2005-09-02	SAM, RTi		Add readStateMod.  This depends on the
//					DWR.StateMod package.
// 2005-09-08	SAM, RTi		Add fillConstant.
//					Add newStatisticYearTS.
// 2005-09-20	SAM, RTi		Add newTimeSeries.
// 2005-09-29	SAM, RTi		Add cumulate, readStateModB.
// 2005-10-18	SAM, RTi		Add processTSProduct.
// 2005-12-06	J. Thomas Sapienza, RTi	Add TS Alias = readNwsCard().
// 2005-12-14	SAM, RTi		Add setInputPeriod.
// 2006-01-17	JTS, RTi		Add writeNWSRFSESPTraceEnsemble.
// 2006-01-18	JTS, RTi		readNWSCard_* moved into the
//					RTi.DMI.NWSRFS_DMI package.
// 2006-01-31	SAM, RTi		Add TS Alias = readNDFD() and
//					openNDFD() commands (dummy in
//					until the code is ready).
// 2006-04-13	SAM, RTi		Add fillMOVE2().
// 2006-04-19	SAM, RTi		Add compareFiles().
// 2006-04-21	SAM, RTi		Add readHydroBase().
// 2006-05-02	SAM, RTi		Add runCommands().
//------------------------------------------------------------------------------
// EndHeader

package rti.tscommandprocessor.core;

// TODO SAM 2005-07-10 Need to evaluate where command classes should live to
// avoid mixing with lower-level code.
//import RTi.DataServices.Adapter.NDFD.openNDFD_Command;
//import RTi.DataServices.Adapter.NDFD.readNDFD_Command;

import RTi.Util.IO.Command;
import RTi.Util.IO.CommandFactory;
import RTi.Util.IO.GenericCommand;
import RTi.Util.IO.UnknownCommandException;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;

// DataTest commands
// FIXME SAM 2007-08-30 Need to work with Ian to pull in new data test features

import rti.tscommandprocessor.commands.datatest.newDataTest_Command;
import rti.tscommandprocessor.commands.datatest.runDataTest_Command;

// DateValue commands

import rti.tscommandprocessor.commands.datevalue.WriteDateValue_Command;

// GRTS commands (time series products).

import rti.tscommandprocessor.commands.products.processTSProduct_Command;

// HydroBase commands.

import rti.tscommandprocessor.commands.hydrobase.fillUsingDiversionComments_Command;
import rti.tscommandprocessor.commands.hydrobase.openHydroBase_Command;
import rti.tscommandprocessor.commands.hydrobase.readHydroBase_Command;

// Logging commands.

import rti.tscommandprocessor.commands.logging.startLog_Command;

// NWSRFS commands.

import rti.tscommandprocessor.commands.nwsrfs.readNwsCard_Command;
import rti.tscommandprocessor.commands.nwsrfs.writeNWSRFSESPTraceEnsemble_Command;

// RiverWare commands

import rti.tscommandprocessor.commands.riverware.writeRiverWare_Command;

// SHEF commands.

import rti.tscommandprocessor.commands.shef.WriteSHEF_Command;

// StateCU commands.

import rti.tscommandprocessor.commands.statecu.readStateCU_Command;

// StateMod commands.

import rti.tscommandprocessor.commands.statemod.writeStateMod_Command;
import rti.tscommandprocessor.commands.statemod.readStateMod_Command;
import rti.tscommandprocessor.commands.statemod.readStateModB_Command;

// Summary commands.

import rti.tscommandprocessor.commands.summary.WriteSummary_Command;

// Table commands.

import rti.tscommandprocessor.commands.table.ReadTableFromDelimitedFile_Command;

// Time-related commands.

import rti.tscommandprocessor.commands.time.SetInputPeriod_Command;
import rti.tscommandprocessor.commands.time.SetOutputPeriod_Command;

// Time series general commands.
 
import rti.tscommandprocessor.commands.ts.analyzePattern_Command;
import rti.tscommandprocessor.commands.ts.changeInterval_Command;
import rti.tscommandprocessor.commands.ts.compareTimeSeries_Command;
import rti.tscommandprocessor.commands.ts.copy_Command;
import rti.tscommandprocessor.commands.ts.CreateEnsemble_Command;
import rti.tscommandprocessor.commands.ts.cumulate_Command;
import rti.tscommandprocessor.commands.ts.fillConstant_Command;
import rti.tscommandprocessor.commands.ts.fillHistMonthAverage_Command;
import rti.tscommandprocessor.commands.ts.fillHistYearAverage_Command;
import rti.tscommandprocessor.commands.ts.fillMixedStation_Command;
import rti.tscommandprocessor.commands.ts.fillMOVE2_Command;
import rti.tscommandprocessor.commands.ts.fillRegression_Command;
import rti.tscommandprocessor.commands.ts.lagK_Command;
import rti.tscommandprocessor.commands.ts.NewStatisticTimeSeries_Command;
import rti.tscommandprocessor.commands.ts.NewStatisticTimeSeriesFromEnsemble_Command;
import rti.tscommandprocessor.commands.ts.newStatisticYearTS_Command;
import rti.tscommandprocessor.commands.ts.NewPatternTimeSeries_Command;
import rti.tscommandprocessor.commands.ts.newTimeSeries_Command;
import rti.tscommandprocessor.commands.ts.ResequenceTimeSeriesData_Command;
import rti.tscommandprocessor.commands.ts.scale_Command;
import rti.tscommandprocessor.commands.ts.SetTimeSeriesProperty_Command;
import rti.tscommandprocessor.commands.ts.sortTimeSeries_Command;

// Utility commands.

import rti.tscommandprocessor.commands.util.compareFiles_Command;
import rti.tscommandprocessor.commands.util.CreateRegressionTestCommandFile_Command;
import rti.tscommandprocessor.commands.util.Free_Command;
import rti.tscommandprocessor.commands.util.mergeListFileColumns_Command;
import rti.tscommandprocessor.commands.util.RemoveFile_Command;
import rti.tscommandprocessor.commands.util.runCommands_Command;
import rti.tscommandprocessor.commands.util.StartRegressionTestResultsReport_Command;
import rti.tscommandprocessor.commands.util.testCommand_Command;
import rti.tscommandprocessor.commands.util.WriteProperty_Command;

/**
This class instantiates Commands for time series processing.
*/
public class TSCommandFactory implements CommandFactory
{
    
/**
Constructor.
*/
public TSCommandFactory ()
{
    super();
}
	
/**
Return a new time series command, based on the command name.  DO NOT create a
GenericCommand if the command is not recognized.
@return a new time series command, based on the command name.
@param command_string The command string to process.
@throws UnknownCommandException if the command name is not recognized.
*/
public Command newCommand ( String command_string )
throws UnknownCommandException
{
	return newCommand ( command_string, false );
}

/**
Return a new time series command, based on the command name.
@return a new time series command, based on the command name.
@param command_string The command string to process.
@param create_generic_command_if_not_recognized If true and the command is
not recognized, create a GenericCommand instance that holds the command string.
This is useful for code that is being migrated to the full command class design.
@throws UnknownCommandException if the command name is not recognized.
*/
public Command newCommand ( String command_string,
		boolean create_generic_command_if_not_recognized )
throws UnknownCommandException
{	command_string = command_string.trim();
    String routine = "TSCommandFactory.newCommand";

	// Parse out arguments for TS alias = foo() commands to be able to
	// handle nulls here

	boolean isTScommand = false;	// Whether TS alias = foo() command.
	boolean isDataTest_command = false;	// Whether DataTest alias = foo() command.
	String TScommand = "";			// Don't use null, to allow string compares below
	String DataTest_command = "";
	String token0 = StringUtil.getToken(command_string,"( =",StringUtil.DELIM_SKIP_BLANKS,0);
	if ( (token0 != null) && token0.equalsIgnoreCase( "TS") ) {
		isTScommand = true;
		TScommand = StringUtil.getToken(command_string,"( =",StringUtil.DELIM_SKIP_BLANKS,2);
		if ( TScommand == null ) {
			TScommand = "";
		}
	}
	else if ( (token0 != null) && token0.equalsIgnoreCase( "DataTest") ) {
		isDataTest_command = true;
		DataTest_command = StringUtil.getToken(command_string,"( =",StringUtil.DELIM_SKIP_BLANKS,2);
		if ( DataTest_command == null ) {
			DataTest_command = "";
		}
	}

	// "a" commands...

	if ( StringUtil.startsWithIgnoreCase(command_string,"AnalyzePattern") ) {
		return new analyzePattern_Command ();
	}

	// "c" commands...

	else if ( isTScommand && TScommand.equalsIgnoreCase("ChangeInterval") ) {
		return new changeInterval_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string,"CompareFiles") ) {
		return new compareFiles_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string,"CompareTimeSeries") ) {
		return new compareTimeSeries_Command ();
	}
	else if ( isTScommand && TScommand.equalsIgnoreCase("Copy") ) {
		return new copy_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(command_string,"CreateEnsemble") ) {
        return new CreateEnsemble_Command ();
    }
	else if ( StringUtil.startsWithIgnoreCase(command_string,"CreateRegressionTestCommandFile") ) {
		return new CreateRegressionTestCommandFile_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string,"Cumulate") ) {
		return new cumulate_Command ();
	}
	
	// "e" commands...
	
	else if ( StringUtil.startsWithIgnoreCase(command_string,"Exit") ) {
		Command command = new GenericCommand();
		command.setCommandString("Exit");
		return command;
	}

	// "f" commands...

	else if ( StringUtil.startsWithIgnoreCase(command_string,"FillConstant") ) {
		return new fillConstant_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string,"FillHistMonthAverage") ) {
		return new fillHistMonthAverage_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string,"FillHistYearAverage") ) {
		return new fillHistYearAverage_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string,"FillMixedStation") ) {
		return new fillMixedStation_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string,"FillMOVE2") ) {
		return new fillMOVE2_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string,"FillRegression") ) {
		return new fillRegression_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string,"FillUsingDiversionComments") ) {
		return new fillUsingDiversionComments_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(command_string,"Free(") ||
            StringUtil.startsWithIgnoreCase(command_string,"Free (")) {
        return new Free_Command ();
    }
	
	// "l" commands...

	else if ( isTScommand && TScommand.equalsIgnoreCase("LagK") ) {
		return new lagK_Command ();
	}
	
	// "m" commands...
	
	else if ( StringUtil.startsWithIgnoreCase(command_string,"MergeListFileColumns") ) {
		return new mergeListFileColumns_Command ();
	}

	// "n" commands...

	else if ( isDataTest_command && DataTest_command.equalsIgnoreCase("NewDataTest") ) {
		return new newDataTest_Command();
	}
    // Put the following before the shorter NewStatisticTimeSeries() command.
    else if ( isTScommand && TScommand.equalsIgnoreCase("NewStatisticTimeSeriesFromEnsemble") ) {
        return new NewStatisticTimeSeriesFromEnsemble_Command ();
    }
	else if ( isTScommand && TScommand.equalsIgnoreCase("NewStatisticTimeSeries") ) {
		return new NewStatisticTimeSeries_Command ();
	}
	else if ( isTScommand && TScommand.equalsIgnoreCase("NewStatisticYearTS") ) {
		return new newStatisticYearTS_Command ();
	}
	else if ( isTScommand && TScommand.equalsIgnoreCase("NewPatternTimeSeries") ) {
		return new NewPatternTimeSeries_Command ();
	}
	else if ( isTScommand && TScommand.equalsIgnoreCase("NewTimeSeries") ) {
		return new newTimeSeries_Command ();
	}

	// "o" commands...

	else if ( StringUtil.startsWithIgnoreCase(command_string,"OpenHydroBase") ) {
		return new openHydroBase_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string,"OpenNDFD") ) {
		//return new openNDFD_Command ();
	}

	// "p" commands...

	else if ( StringUtil.startsWithIgnoreCase(command_string,"ProcessTSProduct") ) {
		return new processTSProduct_Command ();
	}

	// "r" commands...

	else if ( StringUtil.startsWithIgnoreCase(command_string,"ReadHydroBase") ) {
		return new readHydroBase_Command ();
	}
	else if ( isTScommand && TScommand.equalsIgnoreCase("ReadHydroBase") ) {
		return new readHydroBase_Command();
	}
	else if ( isTScommand && TScommand.equalsIgnoreCase("ReadNDFD") ) {
		//return new readNDFD_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string,"ReadNwsCard") ) {
		return new readNwsCard_Command ();
	}
	else if ( isTScommand && TScommand.equalsIgnoreCase("ReadNwsCard") ) {
		return new readNwsCard_Command();
	}
	// Put before shorter command name...
	else if ( StringUtil.startsWithIgnoreCase(command_string,"ReadStateModB") ) {
		return new readStateModB_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string,"ReadStateCU") ) {
		return new readStateCU_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string,"ReadStateMod") ) {
		return new readStateMod_Command ();
	}
	else if ( isTScommand && TScommand.equalsIgnoreCase("ReadStateMod") ) {
		return new readStateMod_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(command_string,"ReadTableFromDelimitedFile") ) {
        return new ReadTableFromDelimitedFile_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(command_string,"RemoveFile") ) {
        return new RemoveFile_Command ();
    }
    else if ( StringUtil.startsWithIgnoreCase(command_string,"ResequenceTimeSeriesData") ) {
        return new ResequenceTimeSeriesData_Command ();
    }
	else if ( StringUtil.startsWithIgnoreCase(command_string,"RunCommands") ) {
		return new runCommands_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string,"RunDataTest") ) {
		return new runDataTest_Command ();
	}

	// "s" commands...

	else if ( StringUtil.startsWithIgnoreCase(command_string,"Scale") ) {
		return new scale_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string,"SetInputPeriod") ) {
		return new SetInputPeriod_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string,"SetOutputPeriod") ) {
		return new SetOutputPeriod_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string,"SetQueryPeriod") ) {
		// Phasing into new syntax...
		return new SetInputPeriod_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string,"SetTimeSeriesProperty") ) {
		return new SetTimeSeriesProperty_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(command_string,"SortTimeSeries") ) {
        return new sortTimeSeries_Command ();
    }
	else if ( StringUtil.startsWithIgnoreCase( command_string,"StartLog") ){
		return new startLog_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase( command_string,"StartRegressionTestResultsReport") ){
        return new StartRegressionTestResultsReport_Command ();
    }
	
	// "t" commands...

	else if ( StringUtil.startsWithIgnoreCase(command_string,"TestCommand") ) {
		return new testCommand_Command ();
	}

	// "w" commands...

	else if ( StringUtil.startsWithIgnoreCase(command_string,"WriteDateValue") ) {
		return new WriteDateValue_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string, "WriteNWSRFSESPTraceEnsemble")) {
		return new writeNWSRFSESPTraceEnsemble_Command();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string,"WriteProperty") ) {
		return new WriteProperty_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string,"WriteRiverWare") ) {
		return new writeRiverWare_Command ();
	}
    else if ( StringUtil.startsWithIgnoreCase(command_string,"WriteSHEF") ) {
        return new WriteSHEF_Command ();
    }
	else if ( StringUtil.startsWithIgnoreCase(command_string,"WriteStateMod") ) {
		return new writeStateMod_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(command_string,"WriteSummary") ) {
		return new WriteSummary_Command ();
	}

	// Did not match a command...

	if ( create_generic_command_if_not_recognized ) {
		// Create a generic command...
		Command c = new GenericCommand ();
		c.setCommandString( command_string );
        Message.printStatus ( 2, routine, "Creating GenericCommand for unknown command \"" +
                command_string + "\"" );
		return c;
	}
	else {
		// Throw an exception if the command is not recognized.
		throw new UnknownCommandException ( "Unknown command \"" + command_string + "\"" );
	}
}

}
