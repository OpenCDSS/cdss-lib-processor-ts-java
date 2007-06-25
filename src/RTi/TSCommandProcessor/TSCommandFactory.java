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

package RTi.TSCommandProcessor;

// REVISIT SAM 2005-07-10 Need to evaluate where command classes should live to
// avoid mixing with lower-level code.
//import RTi.DataServices.Adapter.NDFD.openNDFD_Command;
//import RTi.DataServices.Adapter.NDFD.readNDFD_Command;

import RTi.Util.IO.Command;
import RTi.Util.IO.CommandFactory;
import RTi.Util.IO.compareFiles_Command;
import RTi.Util.IO.UnknownCommandException;

import RTi.Util.Message.startLog_Command;

import RTi.Util.String.StringUtil;

// DataTest commands

import RTi.DataTest.newDataTest_Command;
import RTi.DataTest.runDataTest_Command;

// GRTS commands (time series products).

import RTi.GRTS.processTSProduct_Command;

// HydroBase commands.

import DWR.DMI.HydroBaseDMI.fillUsingDiversionComments_Command;
import DWR.DMI.HydroBaseDMI.openHydroBase_Command;
import DWR.DMI.HydroBaseDMI.readHydroBase_Command;

// NWSRFS commands.

import RTi.DMI.NWSRFS_DMI.readNwsCard_Command;
import RTi.DMI.NWSRFS_DMI.writeNWSRFSESPTraceEnsemble_Command;

// StateCU commands.

import DWR.StateCU.readStateCU_Command;

// StateMod commands.

import DWR.StateMod.writeStateMod_Command;
import DWR.StateMod.readStateMod_Command;
import DWR.StateMod.readStateModB_Command;

// TS commands (basic functionality).

import RTi.TS.analyzePattern_Command;
import RTi.TS.changeInterval_Command;
import RTi.TS.compareTimeSeries_Command;
import RTi.TS.copy_Command;
import RTi.TS.cumulate_Command;
import RTi.TS.fillConstant_Command;
import RTi.TS.fillHistMonthAverage_Command;
import RTi.TS.fillHistYearAverage_Command;
import RTi.TS.fillMixedStation_Command;
import RTi.TS.fillMOVE2_Command;
import RTi.TS.fillRegression_Command;
import RTi.TS.lagK_Command;
import RTi.TS.newStatisticYearTS_Command;
import RTi.TS.newTimeSeries_Command;
import RTi.TS.runCommands_Command;
import RTi.TS.scale_Command;
import RTi.TS.setInputPeriod_Command;
import RTi.TS.sortTimeSeries_Command;
import RTi.TS.writeRiverWare_Command;

/**
This class instantiates Commands for time series processing.
*/
public class TSCommandFactory implements CommandFactory
{

/**
Return a new time series command, based on the command name.
@return a new time series command, based on the command name.
@param command_string The command string to process.
@throws UnknownCommandException if the command name is not recognized.
*/
public Command newCommand ( String command_string )
throws UnknownCommandException
{	command_string = command_string.trim();

	// "a" commands...

	if ( StringUtil.startsWithIgnoreCase(
		command_string,"analyzePattern") ) {
		return new analyzePattern_Command ();
	}

	// "c" commands...

	else if ( StringUtil.startsWithIgnoreCase(
		command_string,"compareFiles") ) {
		return new compareFiles_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(
		command_string,"compareTimeSeries") ) {
		return new compareTimeSeries_Command ();
	}
	else if ( StringUtil.getToken(command_string,"( =",
		StringUtil.DELIM_SKIP_BLANKS,0).equalsIgnoreCase( "TS") &&
		StringUtil.getToken(command_string,"( =",
		StringUtil.DELIM_SKIP_BLANKS,2).equalsIgnoreCase( "copy") ) {
		return new copy_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(
		command_string,"cumulate") ) {
		return new cumulate_Command ();
	}

	// "f" commands...

	else if ( StringUtil.startsWithIgnoreCase(
		command_string,"fillConstant") ) {
		return new fillConstant_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(
		command_string,"fillHistMonthAverage") ) {
		return new fillHistMonthAverage_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(
		command_string,"fillHistYearAverage") ) {
		return new fillHistYearAverage_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(
		command_string,"fillMixedStation") ) {
		return new fillMixedStation_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(
		command_string,"fillMOVE2") ) {
		return new fillMOVE2_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(
		command_string,"fillRegression") ) {
		return new fillRegression_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(
			command_string,"fillUsingDiversionComments") ) {
			return new fillUsingDiversionComments_Command ();
	}
	
	// "l" commands...

	else if ( StringUtil.getToken(command_string,"( =",
		StringUtil.DELIM_SKIP_BLANKS,0).equalsIgnoreCase( "TS") &&
		StringUtil.getToken(command_string,"( =",
		StringUtil.DELIM_SKIP_BLANKS,2).equalsIgnoreCase( "lagK") ) {
		return new lagK_Command ();
	}

	// "n" commands...

	else if ( StringUtil.getToken(command_string,"( =",
		StringUtil.DELIM_SKIP_BLANKS,0).equalsIgnoreCase( "DataTest") &&
		StringUtil.getToken(command_string,"( =",
		StringUtil.DELIM_SKIP_BLANKS,2).equalsIgnoreCase(
		"newDataTest") ) {
		return new newDataTest_Command();
	}
	else if ( StringUtil.getToken(command_string,"( =",
		StringUtil.DELIM_SKIP_BLANKS,0).equalsIgnoreCase( "TS") &&
		StringUtil.getToken(command_string,"( =",
		StringUtil.DELIM_SKIP_BLANKS,2).equalsIgnoreCase(
		"newStatisticYearTS") ) {
		return new newStatisticYearTS_Command ();
	}
	else if ( StringUtil.getToken(command_string,"( =",
		StringUtil.DELIM_SKIP_BLANKS,0).equalsIgnoreCase( "TS") &&
		StringUtil.getToken(command_string,"( =",
		StringUtil.DELIM_SKIP_BLANKS,2).equalsIgnoreCase(
		"newTimeSeries") ) {
		return new newTimeSeries_Command ();
	}

	// "o" commands...

	else if ( StringUtil.startsWithIgnoreCase(
		command_string,"openHydroBase") ) {
		return new openHydroBase_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(
		command_string,"openNDFD") ) {
		//return new openNDFD_Command ();
	}

	// "p" commands...

	else if ( StringUtil.startsWithIgnoreCase(
		command_string,"processTSProduct") ) {
		return new processTSProduct_Command ();
	}

	// "r" commands...

	else if ( StringUtil.startsWithIgnoreCase(
		command_string,"readHydroBase") ) {
		return new readHydroBase_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(
		command_string,"readNwsCard") ) {
		return new readNwsCard_Command ();
	}
	// Put before shorter command name...
	else if ( StringUtil.startsWithIgnoreCase(
		command_string,"readStateModB") ) {
		return new readStateModB_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(
			command_string,"readStateCU") ) {
			return new readStateCU_Command ();
		}
	else if ( StringUtil.startsWithIgnoreCase(
		command_string,"readStateMod") ) {
		return new readStateMod_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(
		command_string,"runCommands") ) {
		return new runCommands_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(
		command_string,"runDataTest") ) {
		return new runDataTest_Command ();
	}

	// "s" commands...

	else if ( StringUtil.startsWithIgnoreCase(
		command_string,"scale") ) {
		return new scale_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(
		command_string,"setInputPeriod") ) {
		return new setInputPeriod_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(
		command_string,"setQueryPeriod") ) {
		// Phasing into new syntax...
		return new setInputPeriod_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(
		command_string,"sortTimeSeries") ) {
		return new sortTimeSeries_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase( command_string,"startLog") ){
		return new startLog_Command ();
	}

	// "TS Alias = " commands...

	else if ( StringUtil.getToken(command_string,"( =",
		StringUtil.DELIM_SKIP_BLANKS,0).equalsIgnoreCase( "TS") &&
		StringUtil.getToken(command_string,"( =",
		StringUtil.DELIM_SKIP_BLANKS,2).equalsIgnoreCase(
		"readHydroBase") ) {
		return new readHydroBase_Command();
	}
	else if ( StringUtil.getToken(command_string,"( =",
		StringUtil.DELIM_SKIP_BLANKS,0).equalsIgnoreCase( "TS") &&
		StringUtil.getToken(command_string,"( =",
		StringUtil.DELIM_SKIP_BLANKS,2).equalsIgnoreCase(
		"changeInterval") ) {
		return new changeInterval_Command ();
	}
	else if ( StringUtil.getToken(command_string,"( =",
		StringUtil.DELIM_SKIP_BLANKS,0).equalsIgnoreCase( "TS") &&
		StringUtil.getToken(command_string,"( =",
		StringUtil.DELIM_SKIP_BLANKS,2).equalsIgnoreCase(
		"readNDFD") ) {
		//return new readNDFD_Command ();
	}
	else if ( StringUtil.getToken(command_string,"( =",
		StringUtil.DELIM_SKIP_BLANKS,0).equalsIgnoreCase( "TS") &&
		StringUtil.getToken(command_string,"( =",
		StringUtil.DELIM_SKIP_BLANKS,2).equalsIgnoreCase(
		"readNwsCard") ) {
		return new readNwsCard_Command();
	}
	else if ( StringUtil.getToken(command_string,"( =",
		StringUtil.DELIM_SKIP_BLANKS,0).equalsIgnoreCase( "TS") &&
		StringUtil.getToken(command_string,"( =",
		StringUtil.DELIM_SKIP_BLANKS,2).equalsIgnoreCase(
		"readStateMod") ) {
		return new readStateMod_Command ();
	}

	// "w" commands...

	else if ( StringUtil.startsWithIgnoreCase(
		command_string, "writeNWSRFSESPTraceEnsemble")) {
		return new writeNWSRFSESPTraceEnsemble_Command();
	}
	else if ( StringUtil.startsWithIgnoreCase(
		command_string,"writeRiverWare") ) {
		return new writeRiverWare_Command ();
	}
	else if ( StringUtil.startsWithIgnoreCase(
		command_string,"writeStateMod") ) {
		return new writeStateMod_Command ();
	}

	// Did not match a command...

	throw new UnknownCommandException ( "Unknown command \"" +
			command_string + "\"" );
}

}
