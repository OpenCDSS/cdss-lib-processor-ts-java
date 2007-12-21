package rti.tscommandprocessor.commands.custom;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.TS.DayTS;
import RTi.TS.TS;
import RTi.TS.TSUtil;

import RTi.Util.IO.AbstractCommand;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.FileGenerator;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;

/**
<p>
This class initializes, checks, and runs the CustomCommand() command.
</p>
*/
public class CustomCommand_Command extends AbstractCommand implements Command, FileGenerator
{
    
/**
Tests being performed.
*/
private final int __TEST_NO_CHANGE = 0; // Noise
private final int __TEST_DIRECTION = 1; // Not official delivary?
private final int __TEST_VALUE = 2;
private final int __TEST_CHANGE = 3;
private final int __TEST_FINAL = 4;
private final int __TEST_SIZE = 5;

//Test names

String [] __TEST_NAMES = {
    "Noise pass?",
    "Direction pass?",
    "Value pass?",
    "Change pass?",
    "Overall pass?"
};

/**
Bins to receive the results of the analysis.
<pre>
__BIN_THIS_WEEK = Tuesday through Friday of the first week
__BIN_NEXT_WEEK = Monday through Friday of the next week
__BIN_THIS_MONTH = Tuesday of this week through the last day of the month, weekends included (overlap week).
__BIN_MONTH1 = Month + 1 (all days in month)
__BIN_MONTH2 = Month + 2 (all days in month)
__BIN_MONTH3 = Month + 3 (all days in month)
__BIN_END = Remaining end of forecast (all available days)
<pre>
*/
private final int __BIN_THIS_WEEK = 0;
private final int __BIN_NEXT_WEEK = 1;
private final int __BIN_THIS_MONTH = 2;
private final int __BIN_MONTH1 = 3;
private final int __BIN_MONTH2 = 4;
private final int __BIN_MONTH3 = 5;
private final int __BIN_END = 6;
private final int __BIN_SIZE = 7;

// Bin names, used in output and headings

String [] __BIN_NAMES = {
        "This week",
        "Next week",
        "This month",
        "Month + 1",
        "Month + 2",
        "Month + 3",
        "To end"
        };
// Used for headings
String [] __BIN_NAMES2 = {
        "(Tuesday-Friday)",
        "(Monday-Friday)",
        "(all days)",
        "(all days)",
        "(all days)",
        "(all days)",
        "(all days)"
        };
  
/**
Output file that is created by this command.
*/
private File __OutputFile_File = null;

/**
Constructor.
*/
public CustomCommand_Command ()
{	super();
	setCommandName ( "CustomCommand" );
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a
cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor
dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException
{	String warning = "";
	String routine = getCommandName() + ".checkCommandParameters";
	String message;

	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
    CommandProcessor processor = getCommandProcessor();
    
    String CurrentRForecastTSID = parameters.getValue("CurrentRForecastTSID");
    String CurrentNForecastTSID = parameters.getValue ( "CurrentNForecastTSID" );
    String PreviousNForecastTSID = parameters.getValue ( "PreviousNForecastTSID" );
    String ForecastStart = parameters.getValue ( "ForecastStart" );
    String NoiseThreshold = parameters.getValue ( "NoiseThreshold" );
    String ChangeCriteria  = parameters.getValue("ChangeCriteria");
    String ValueCriteria = parameters.getValue("ValueCriteria");
    String OutputFile = parameters.getValue("OutputFile");
    
    if ( (CurrentRForecastTSID == null) || (CurrentRForecastTSID.length() == 0) ) {
        message = "A CurrentRForecast time series identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a time series identifier." ) );
    }
    if ( (CurrentNForecastTSID == null) || (CurrentNForecastTSID.length() == 0) ) {
        message = "A CurrentNForecast time series identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a time series identifier." ) );
    }
    if ( (PreviousNForecastTSID == null) || (PreviousNForecastTSID.length() == 0) ) {
        message = "A PreviousNForecast time series identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a time series identifier." ) );
    }
   
    if ( (ForecastStart != null) && !ForecastStart.equals("") ) {
        try {
            DateTime date = DateTime.parse(ForecastStart);
            if ( date.getWeekDay() != 2 ) {
                message = "The forecast start date/time \"" + ForecastStart + "\" must be a Tuesday.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Specify a forecast start that is a Tuesday." ) );
            }
        }
        catch ( Exception e ) {
            message = "The forecast start date/time \"" + ForecastStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time." ) );
        }
    }
    
    if ( (NoiseThreshold == null) || (NoiseThreshold.length() == 0) ) {
        message = "An noise threshold must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a value for the noise threshold." ) );
    }
    else if ( !StringUtil.isDouble(NoiseThreshold ) ) {
        message = "The noise threshold (" + NoiseThreshold + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a percent value for the noise threshold." ) );
    }
    
    if ( (ChangeCriteria == null) || (ChangeCriteria.length() == 0) ) {
        message = "An change criteria must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a value for the change criteria." ) );
    }
    else if ( !StringUtil.isDouble(ChangeCriteria ) ) {
        message = "The change criteria (" + ChangeCriteria + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a percent value for the change criteria." ) );
    }
    
    if ( (ValueCriteria == null) || (ValueCriteria.length() == 0) ) {
        message = "An value criteria must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a value for the change criteria." ) );
    }
    else if ( !StringUtil.isDouble(ValueCriteria ) ) {
        message = "The value criteria (" + ValueCriteria + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a percent value for the value criteria." ) );
    }
    
    if ( (OutputFile == null) || (OutputFile.length() == 0) ) {
        message = "The output file: \"" + OutputFile + "\" must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an output file." ) );
    }
    else {
        String working_dir = null;
        try { Object o = processor.getPropContents ( "WorkingDir" );
            if ( o != null ) {
                working_dir = (String)o;
            }
        }
        catch ( Exception e ) {
            message = "Error requesting WorkingDir from processor.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Software error - report the problem to support." ) );
        }

        try {
            String adjusted_path = IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir, OutputFile));
            File f = new File ( adjusted_path );
            File f2 = new File ( f.getParent() );
            if ( !f2.exists() ) {
                message = "The output file parent directory does " +
                "not exist: \"" + adjusted_path + "\".";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Create the output directory." ) );
            }
            f = null;
            f2 = null;
        }
        catch ( Exception e ) {
            message = "The output file:\n" +
            "    \"" + OutputFile +
            "\"\ncannot be adjusted using the working directory:\n" +
            "    \"" + working_dir + "\".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that output file and working directory paths are compatible." ) );
        }
    }

	// Check for invalid parameters...
	Vector valid_Vector = new Vector();
    valid_Vector.add ( "CurrentRForecastTSID" );
    valid_Vector.add ( "CurrentNForecastTSID" );
    valid_Vector.add ( "PreviousNForecastTSID" );
    valid_Vector.add ( "ForecastStart" );
    valid_Vector.add ( "NoiseThreshold" );
	valid_Vector.add ( "ChangeCriteria" );
	valid_Vector.add ( "ValueCriteria" );
    valid_Vector.add ( "OutputFile" );
	warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level), routine, warning );
		throw new InvalidCommandParameterException ( warning );
	}
	status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Process the custom command.
*/
private int customCommand (
        DayTS R_TS,
        DayTS Nc_TS,
        DayTS Np_TS,
        DateTime ForecastStart_DateTime,
        double NoiseThreshold_double,
        double ChangeCriteria_double,
        double ValueCriteria_double,
        String OutputFile_full,
        CommandStatus status )
{   String routine = "CustomCommand.customCommand";
    int warning_count = 0;
    String message;
    CommandPhaseType command_phase = CommandPhaseType.RUN;
    
    // Start on the forecast date and loop through the data to fill bins of data to fill the
    // following bins.

    // Single values used during iteration...
    
    double R_value = 0.0;
    double Nc_value = 0.0;
    double Np_value = 0.0;
    
    // Values in the bins, used to evaluate accuracy of forecast

    double [] R_total = new double[__BIN_SIZE];
    double [] Nc_total = new double[__BIN_SIZE];
    double [] Np_total = new double[__BIN_SIZE];
    // Count of missing values in each bin
    int [] R_missing = new int[__BIN_SIZE];
    int [] Nc_missing = new int[__BIN_SIZE];
    int [] Np_missing = new int[__BIN_SIZE];
    // Also save the start and end of each bin so that time series ends can be checked
    DateTime [] bin_start_DateTime = new DateTime[__BIN_SIZE];
    DateTime [] bin_end_DateTime = new DateTime[__BIN_SIZE];
    
    for ( int i = 0; i < __BIN_SIZE; i++ ) {
        R_total[i] = 0;
        Nc_total[i] = 0;
        Np_total[i] = 0;
        R_missing[i] = -1;      // To allow check later when bins are not even encountered
        Nc_missing[i] = -1;
        Np_missing[i] = -1;
        bin_start_DateTime[i] = null;
        bin_end_DateTime[i] = null;
    }

    // Upper lower acceptable bounds of the R forecast (e.g., will be .95 to 1.05 of N forecast.
    
    double ValueCriteria_high = 1.0 + ValueCriteria_double/100.0;
    double ValueCriteria_low = 1.0 - ValueCriteria_double/100.0;
    //double ChangeCriteria_high = 1.0 + ChangeCriteria_double/100.0;
    //double ChangeCriteria_low = 1.0 - ChangeCriteria_double/100.0;
    
    // Indicate which band the iterator is in.
    
    boolean in_thisweek = true;     // Always true to start
    boolean in_between_thisweek_and_nextweek = false;
    boolean in_nextweek = false;
    boolean in_thismonth = true;    // Always true to start
    boolean in_month1 = false;
    boolean in_month2 = false;
    boolean in_month3 = false;
    boolean in_end = false;
    
    // Day of week, used to determine some bins.
    
    int day_of_week;
    //final int day_sunday = 0;
    final int day_monday = 1;
    //final int day_tuesday = 2;
    //final int day_wednesday = 3;
    //final int day_thursday = 4;
    //final int day_friday = 5;
    final int day_saturday = 6;
    
    // Loop from the forecast start to the end of available data.
    
    DateTime date = new DateTime(ForecastStart_DateTime);
    DateTime end = new DateTime(Np_TS.getDate2());
    // Initialize the bin end dates to the end of the period in case the loop ends without getting
    // to a bin (due to a short period).
    for ( int i = 0; i < __BIN_SIZE; i++ ) {
        bin_end_DateTime[i] = new DateTime(end);
    }
    Message.printStatus ( 2, routine, "Generating report starting on " + ForecastStart_DateTime + " through " + end );
    for ( int i = 0; date.lessThanOrEqualTo(end); date.addDay(1), i++ ) {
        // Determine which bin the values should go into...
        day_of_week = date.getWeekDay();
        Message.printStatus ( 2, routine, "Processing date " + date + " day of week " + day_of_week );
        // TODO SAM 2007-12-21 May need to move this below to avoid spurious messages at end of period
        // Get the current values corresponding to the date iterator
        R_value = R_TS.getDataValue ( date );
        if ( R_TS.isDataMissing(R_value) ) {
            message = "CurrentRForecast(" + date + ") is missing - treat as zero.";
            Message.printWarning( 3, routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.WARNING,
                            message, "Verify input time series." ) );
            R_value = 0.0;
            ++warning_count;
        }
        else if ( R_value <= 0.0 ) {
            message = "CurrentRForecast(" + date + ") is negative (" +
            R_value + ") - will decrease total.";
            Message.printWarning( 3, routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.WARNING,
                            message, "Verify input time series." ) );
            ++warning_count;
        }
        Nc_value = Nc_TS.getDataValue ( date );
        if ( Nc_TS.isDataMissing(Nc_value) ) {
            message = "CurrentNForecast(" + date + ") is missing - treat as zero.";
            Message.printWarning( 3, routine, message );
            Nc_value = 0.0;
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.WARNING,
                            message, "Verify input time series." ) );
            ++warning_count;
        }
        else if ( Nc_value <= 0.0 ) {
            message = "CurrentNForecast(" + date + ") is negative (" +
            Nc_value + ") - will decrease total.";
            Message.printWarning( 3, routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.WARNING,
                            message, "Verify input time series." ) );
            ++warning_count;
        }
        Np_value = Np_TS.getDataValue ( date );
        if ( Np_TS.isDataMissing(Np_value) ) {
            message = "PreviousNForecast(" + date + ") is missing - treat as zero.";
            Message.printWarning( 3, routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.WARNING,
                            message, "Verify input time series." ) );
            Np_value = 0.0;
            ++warning_count;
        }
        else if ( Np_value <= 0.0 ) {
            message = "PreviousNForecast(" + date + ") is negative (" +
            Np_value + ") - will decrease total.";
            Message.printWarning( 3, routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.WARNING,
                            message, "Verify input time series." ) );
            ++warning_count;
        }
        // Adjust the bin that the value should go into, starting with figururing out the week.
        if ( i == 0 ) {
            // Always in this week with no further adjustments necessary
            in_between_thisweek_and_nextweek = true;
            bin_start_DateTime[__BIN_THIS_WEEK] = new DateTime(date);
        }
        else if ( in_thisweek ) {
            // Switch to next week if Saturday and not the first day being processed
            if ( day_of_week == day_saturday ) {
                in_thisweek = false;
                bin_end_DateTime[__BIN_THIS_WEEK] = new DateTime(date);
                bin_end_DateTime[__BIN_THIS_WEEK].addDay ( -1 );
                in_between_thisweek_and_nextweek = true;
            }
        }
        else if ( in_between_thisweek_and_nextweek ) {
            if ( day_of_week == day_monday ) {
                // Need to start "next week"...
                in_between_thisweek_and_nextweek = false;
                bin_start_DateTime[__BIN_NEXT_WEEK] = new DateTime(date);
                in_nextweek = true;
            }
        }
        else if ( in_nextweek ) {
            // Done with week when get to saturday
            if ( day_of_week == day_saturday ) {
                in_nextweek = false;
                bin_end_DateTime[__BIN_NEXT_WEEK] = new DateTime(date);
                bin_end_DateTime[__BIN_NEXT_WEEK].addDay ( -1 );
            }
        }
        // Now determine the monthly bin.  The "thismonth" bin can overlap the weeks, but includes
        // all days of the month (not just weekdays).
        if ( i == 0 ) {
            // Always in this month with no further adjustments necessary
            in_thismonth = true;
            bin_start_DateTime[__BIN_THIS_MONTH] = new DateTime(date);
        }
        else if ( in_thismonth ) {
            // Switch to next month if not the first day being processed and the day is 1
            if ( date.getDay() == 1 ) {
                in_thismonth = false;
                bin_end_DateTime[__BIN_THIS_MONTH] = new DateTime(date);
                bin_end_DateTime[__BIN_THIS_MONTH].addDay ( -1 );
                in_month1 = true;
                bin_start_DateTime[__BIN_MONTH1] = new DateTime(date);
            }
        }
        else if ( in_month1 ) {
            if ( date.getDay() == 1 ) {
                in_month1 = false;
                bin_end_DateTime[__BIN_MONTH1] = new DateTime(date);
                bin_end_DateTime[__BIN_MONTH1].addDay ( -1 );
                in_month2 = true;
                bin_start_DateTime[__BIN_MONTH2] = new DateTime(date);
            }
        }
        else if ( in_month2 ) {
            if ( date.getDay() == 1 ) {
                in_month2 = false;
                bin_end_DateTime[__BIN_MONTH2] = new DateTime(date);
                bin_end_DateTime[__BIN_MONTH2].addDay ( -1 );
                in_month3 = true;
                bin_start_DateTime[__BIN_MONTH3] = new DateTime(date);
            }
        }
        else if ( in_month3 ) {
            if ( date.getDay() == 1 ) {
                // Done processing...
                in_month3 = false;
                bin_end_DateTime[__BIN_MONTH3] = new DateTime(date);
                bin_end_DateTime[__BIN_MONTH3].addDay ( -1 );
                in_end = true;
                bin_start_DateTime[__BIN_END] = new DateTime(date);
            }
        }
        // Now accually do the processing
        // Weekly metrics...
        int bin_day = -1;
        if ( in_thisweek ) {
            bin_day = __BIN_THIS_WEEK;

        }
        else if ( in_nextweek ) {
            bin_day = __BIN_NEXT_WEEK;
        }
        if ( bin_day >= 0 ) {
            R_total[bin_day] += R_value;
            Nc_total[bin_day] += Nc_value;
            Np_total[bin_day] += Np_value;
      
            if ( Np_missing[bin_day] < 0) {
                Np_missing[bin_day] = 0;
            }
            if ( Np_TS.isDataMissing(Np_value) ) {
                ++Np_missing[bin_day];
            }
            if ( Nc_missing[bin_day] < 0) {
                Nc_missing[bin_day] = 0;
            }
            if ( Nc_TS.isDataMissing(Nc_value) ) {
                ++Nc_missing[bin_day];
            }
            if ( R_missing[bin_day] < 0) {
                R_missing[bin_day] = 0;
            }
            if ( R_TS.isDataMissing(R_value) ) {
                ++R_missing[bin_day];
            }
        }
        // Monthly metrics are computed separately...
        int bin_month = -1;
        if ( in_thismonth ) {   // Does not matter what day of the week
            bin_month = __BIN_THIS_MONTH;
        }
        else if ( in_month1 ) {
            bin_month = __BIN_MONTH1;
        }
        else if ( in_month2 ) {
            bin_month = __BIN_MONTH2;
        }
        else if ( in_month3 ) {
            bin_month = __BIN_MONTH3;
        }
        else if ( in_end ) {
            bin_month = __BIN_END;
        }
        if ( bin_month >= 0 ) {
            Np_total[bin_month] += Np_value;
            Nc_total[bin_month] += Nc_value;
            R_total[bin_month] += R_value;
            
            if ( Np_missing[bin_month] < 0) {
                Np_missing[bin_month] = 0;
            }
            if ( Np_TS.isDataMissing(Np_value) ) {
                ++Np_missing[bin_month];
            }
            if ( Nc_missing[bin_month] < 0) {
                Nc_missing[bin_month] = 0;
            }
            if ( Nc_TS.isDataMissing(Nc_value) ) {
                ++Nc_missing[bin_month];
            }
            if ( R_missing[bin_month] < 0) {
                R_missing[bin_month] = 0;
            }
            if ( R_TS.isDataMissing(R_value) ) {
                ++R_missing[bin_month];
            }
        }
    }
    
    // Write the results with the pass fail results.
    
    PrintWriter fout = null;
    String comment = "#";
    String delim = ",";
    try {
         fout = new PrintWriter ( new FileOutputStream ( OutputFile_full ) );
         IOUtil.printCreatorHeader( fout, comment, 120, 0);
         String format_param = "%-26.26s";
         String format_percent = "%16.2f";
         String format_text = "%16.16s";
         String format_value = "%16.0f";
         String format_int = "%16d";
         fout.println ( comment );
         fout.println ( comment + "--------------------------------------------------------------------------------------");
         fout.println ( comment + " Forecast start: " + ForecastStart_DateTime );
         fout.println ( comment + " Np period: " + Np_TS.getDate1() + " to " + Np_TS.getDate2());
         fout.println ( comment + " Nc period: " + Nc_TS.getDate1() + " to " + Nc_TS.getDate2());
         fout.println ( comment + " R period: " + R_TS.getDate1() + " to " + R_TS.getDate2());
         fout.println ( comment );
         fout.println ( comment + " Units of output are cumulative " + R_TS.getDataUnits());
         fout.println ( comment );
         fout.println ( comment + " Noise threshold (%):  " + StringUtil.formatString(NoiseThreshold_double,format_percent));
         fout.println ( comment + " Change criteria (%):  " + StringUtil.formatString(ChangeCriteria_double,format_percent));
         fout.println ( comment + " Value criteria (%):   " + StringUtil.formatString(ValueCriteria_double,format_percent));
         fout.println ( comment );
         /*
         fout.println ( comment + " For noise to pass:  (|R - Np|/Np < NoiseThreshold) and (|Nc - Np|/Np < NoiseThreshold.");
         fout.println ( comment + " For direction to pass R - Np sign must equal Nc - Np sign.");
         fout.println ( comment + " For magnitude to pass 100*abs(R - Np)/Np must be <= " +
                 StringUtil.formatString(ValueCriteria_double,"%.2f") + "*abs(Nc - Np)/Np.");
         fout.println ( comment + " For no change to pass - THIS IS UNDEFINED AND WILL ALWAYS PASS.");
         */
         fout.println ( comment + "--------------------------------------------------------------------------------------");
         fout.println ( comment );
         printReportResultsLine ( fout, "Parameter", format_param, delim, __BIN_NAMES, format_text );
         printReportResultsLine ( fout, "", format_param, delim, __BIN_NAMES2, format_text );
         printReportResultsLine ( fout, "Bin start", format_param, delim, bin_start_DateTime, format_value );
         printReportResultsLine ( fout, "Bin end", format_param, delim, bin_end_DateTime, format_value );
         // Previous N forecast values....
         printReportDataLine ( fout, "Previous NWS Forecast (Np)", format_param, delim, Np_total, format_value );
         printReportDataLine ( fout, "Np # missing", format_param, delim, Np_missing, format_int );
         // Current N forecast values....
         printReportDataLine ( fout, "Current NWS Forecast (Nc)", format_param, delim, Nc_total, format_value );
         printReportDataLine ( fout, "Nc # missing", format_param, delim, Nc_missing, format_int );
         // Value bound (high), based on Nc
         double [] value_high = new double[__BIN_SIZE];
         for ( int i = 0; i < __BIN_SIZE; i++ ) {
             value_high[i] = Nc_total[i]*ValueCriteria_high;
         }
         printReportDataLine ( fout, "Nc Value bound (high)", format_param, delim, value_high, format_value );
         // Value bound (low), based on Nc
         double [] value_low = new double[__BIN_SIZE];
         for ( int i = 0; i < __BIN_SIZE; i++ ) {
             value_low[i] = Nc_total[i]*ValueCriteria_low;
         }
         printReportDataLine ( fout, "Nc Value bound (low)", format_param, delim, value_low, format_value );
         // Difference and percent difference of Nc - Np values...
         double [] NcNp_diff = new double[__BIN_SIZE];
         for ( int i = 0; i < __BIN_SIZE; i++ ) {
             NcNp_diff[i] = Nc_total[i] - Np_total[i];
         }
         printReportDataLine ( fout, "Nc - Np", format_param, delim, NcNp_diff, format_value );
         double [] NcNp_diff_percent = new double[__BIN_SIZE];
         for ( int i = 0; i < __BIN_SIZE; i++ ) {
             NcNp_diff_percent[i] = (NcNp_diff[i]/Np_total[i])*100.0;
         }
         printReportDataLine ( fout, "Nc - Np (% of Np)", format_param, delim, NcNp_diff_percent, format_percent );
 
         // Raw R values...
         printReportDataLine ( fout, "Current RTi forecast (R)", format_param, delim, R_total, format_value );
         printReportDataLine ( fout, "R # missing", format_param, delim, R_missing, format_int );

         // R minus Np, difference and percent, difference and percent difference of Np values...
         double [] RNp_diff = new double[__BIN_SIZE];
         for ( int i = 0; i < __BIN_SIZE; i++ ) {
             RNp_diff[i] = R_total[i] - Np_total[i];
         }
         printReportDataLine ( fout, "R - Np", format_param, delim, RNp_diff, format_value );
         double [] RNp_diff_percent = new double[__BIN_SIZE];
         for ( int i = 0; i < __BIN_SIZE; i++ ) {
             RNp_diff_percent[i] = (RNp_diff[i]/Np_total[i])*100.0;
         }
         printReportDataLine ( fout, "R - Np (% of Np)", format_param, delim, RNp_diff_percent, format_percent );

         // R minus Nc, difference and percent difference of Nc values...
         double [] RNc_diff = new double[__BIN_SIZE];
         for ( int i = 0; i < __BIN_SIZE; i++ ) {
             RNc_diff[i] = R_total[i] - Nc_total[i];
         }
         printReportDataLine ( fout, "R - Nc", format_param, delim, RNp_diff, format_value );
         double [] RNc_diff_percent = new double[__BIN_SIZE];
         for ( int i = 0; i < __BIN_SIZE; i++ ) {
             RNc_diff_percent[i] = (RNc_diff[i]/Nc_total[i])*100.0;
         }
         printReportDataLine ( fout, "R - Nc (% of Nc)", format_param, delim, RNc_diff_percent, format_percent );

         // Do the final tests, looping through each bin
         String [][] test_results = new String[__TEST_SIZE][__BIN_SIZE];
         for ( int i = 0; i < __BIN_SIZE; i++ ) {
             test ( test_results, i,
                     NoiseThreshold_double, ValueCriteria_double, ChangeCriteria_double,
                     Np_total[i], Nc_total[i], R_total[i], NcNp_diff[i], RNp_diff[i], RNc_diff[i],
                     Np_missing[i], Nc_missing[i], R_missing[i] );
         }
         
         // Now print the test results, looping through each test criteria and printing a corresponding row...
         for ( int i = 0; i < __TEST_SIZE; i++ ) {
             printReportResultsLine ( fout, __TEST_NAMES[i], format_param, delim, test_results[i], format_text );
         }
        
         // Save the output file name...
         fout.close();
         setOutputFile ( new File(OutputFile_full));
    }
    catch ( Exception e ) {
        message = "Unexpected error processing data.";
        Message.printWarning( 3, routine, message );
        Message.printWarning ( 3, routine, e);
        status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.WARNING,
                        message, "Report the problem to software support." ) );
    }

    return warning_count;
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new CustomCommand_JDialog ( parent, this )).ok();
}

/**
Return the list of files that were created by this command.
*/
public List getGeneratedFileList ()
{
    Vector list = new Vector();
    if ( getOutputFile() != null ) {
        list.addElement ( getOutputFile() );
    }
    return list;
}

/**
Return the output file generated by this file.  This method is used internally.
*/
private File getOutputFile ()
{
    return __OutputFile_File;
}

// Use base class parseCommand()

/**
Print a line in the report.
@param fout PrintWriter to receive output.
@param param_title parameter title for first column.
@param format_param StringUtil.formatString() format for parameter column.
@param delim Delimiter between columns.
@param values Values to output.
@parma format_values StringUtil.formtString() format for value columns.
*/
private void printReportDataLine ( PrintWriter fout, String param_title, String format_param,
        String delim, double [] values, String format_value )
{
    fout.println (
        StringUtil.formatString(param_title,format_param) + delim +
        StringUtil.formatString(values[__BIN_THIS_WEEK],format_value) + delim +
        StringUtil.formatString(values[__BIN_NEXT_WEEK],format_value) + delim +
        StringUtil.formatString(values[__BIN_THIS_MONTH],format_value) + delim +
        StringUtil.formatString(values[__BIN_MONTH1],format_value) + delim +
        StringUtil.formatString(values[__BIN_MONTH2],format_value) + delim +
        StringUtil.formatString(values[__BIN_MONTH3],format_value) + delim +
        StringUtil.formatString(values[__BIN_END],format_value) );
}

/**
Print a line in the report.
@param fout PrintWriter to receive output.
@param param_title parameter title for first column.
@param format_param StringUtil.formatString() format for parameter column.
@param delim Delimiter between columns.
@param values Values to output.
@parma format_values StringUtil.formtString() format for value columns.
*/
private void printReportDataLine ( PrintWriter fout, String param_title, String format_param,
        String delim, int [] values, String format_value )
{
    fout.println (
        StringUtil.formatString(param_title,format_param) + delim +
        StringUtil.formatString(values[__BIN_THIS_WEEK],format_value) + delim +
        StringUtil.formatString(values[__BIN_NEXT_WEEK],format_value) + delim +
        StringUtil.formatString(values[__BIN_THIS_MONTH],format_value) + delim +
        StringUtil.formatString(values[__BIN_MONTH1],format_value) + delim +
        StringUtil.formatString(values[__BIN_MONTH2],format_value) + delim +
        StringUtil.formatString(values[__BIN_MONTH3],format_value) + delim +
        StringUtil.formatString(values[__BIN_END],format_value) );
}

/**
Print a line in the report.
@param fout PrintWriter to receive output.
@param param_title parameter title for first column.
@param format_param StringUtil.formatString() format for parameter column.
@param delim Delimiter between columns.
@param values Values to output (test results strings).
@parma format_values StringUtil.formtString() format for value columns.
*/
private void printReportResultsLine ( PrintWriter fout, String param_title, String format_param,
        String delim, Object [] values, String format_value )
{
    fout.println (
        StringUtil.formatString(param_title,format_param) + delim +
        StringUtil.formatString(values[__BIN_THIS_WEEK],format_value) + delim +
        StringUtil.formatString(values[__BIN_NEXT_WEEK],format_value) + delim +
        StringUtil.formatString(values[__BIN_THIS_MONTH],format_value) + delim +
        StringUtil.formatString(values[__BIN_MONTH1],format_value) + delim +
        StringUtil.formatString(values[__BIN_MONTH2],format_value) + delim +
        StringUtil.formatString(values[__BIN_MONTH3],format_value) + delim +
        StringUtil.formatString(values[__BIN_END],format_value) );
}

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "CustomCommand_Command.runCommandInternal", message;
	int warning_level = 2;
    int log_level = 3;  // Non-user warning level
	String command_tag = "" + command_number;
	int warning_count = 0;

    CommandPhaseType command_phase = CommandPhaseType.RUN;
    CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
	status.clearLog(command_phase);
    
	PropList parameters = getCommandParameters();
    String CurrentRForecastTSID = parameters.getValue("CurrentRForecastTSID");
    String CurrentNForecastTSID = parameters.getValue ( "CurrentNForecastTSID" );
    String PreviousNForecastTSID = parameters.getValue ( "PreviousNForecastTSID" );
    String ForecastStart = parameters.getValue ( "ForecastStart" );
    String NoiseThreshold = parameters.getValue ( "NoiseThreshold" );
    String ChangeCriteria  = parameters.getValue("ChangeCriteria");
    String ValueCriteria = parameters.getValue("ValueCriteria");
    String OutputFile = parameters.getValue("OutputFile");

    // Get the time series to process.  The time series list is searched backwards until the first match...

    PropList request_params = new PropList ( "" );
    request_params.set ( "CommandTag", command_tag );
    request_params.set ( "TSID", CurrentRForecastTSID );
    CommandProcessorRequestResultsBean bean = null;
    try {
        bean = processor.processRequest( "GetTimeSeriesForTSID", request_params);
    }
    catch ( Exception e ) {
        message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + CurrentRForecastTSID +
        "\") from processor.";
        Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
    }
    PropList bean_PropList = bean.getResultsPropList();
    Object o_TS = bean_PropList.getContents ( "TS");
    TS CurrentRForecastTSID_TS = null;
    if ( o_TS == null ) {
        message = "Null TS requesting GetTimeSeriesForTSID(TSID=\"" + CurrentRForecastTSID +
        "\") from processor.";
        Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
    }
    else {
        CurrentRForecastTSID_TS = (TS)o_TS;
    }
     
    if ( CurrentRForecastTSID_TS == null ) {
        message = "Unable to find time series to analyze using TSID \"" + CurrentRForecastTSID + "\".";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( command_phase,
        new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
        throw new CommandWarningException ( message );
    }
    
    // CurrentNForecastTSID...
    
    request_params = new PropList ( "" );
    request_params.set ( "CommandTag", command_tag );
    request_params.set ( "TSID", CurrentNForecastTSID );
    try {
        bean = processor.processRequest( "GetTimeSeriesForTSID", request_params);
    }
    catch ( Exception e ) {
        message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + CurrentNForecastTSID +
        "\") from processor.";
        Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
    }
    bean_PropList = bean.getResultsPropList();
    o_TS = bean_PropList.getContents ( "TS");
    TS CurrentNForecastTSID_TS = null;
    if ( o_TS == null ) {
        message = "Null TS requesting GetTimeSeriesForTSID(TSID=\"" + CurrentNForecastTSID +
        "\") from processor.";
        Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
    }
    else {
        CurrentNForecastTSID_TS = (TS)o_TS;
    }
     
    if ( CurrentNForecastTSID_TS == null ) {
        message = "Unable to find time series to analyze using TSID \"" + CurrentNForecastTSID + "\".";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( command_phase,
        new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
        throw new CommandWarningException ( message );
    }
    
    // PreviousNForecastTSID...
    
    request_params.set ( "CommandTag", command_tag );
    request_params.set ( "TSID", PreviousNForecastTSID );
    try {
        bean = processor.processRequest( "GetTimeSeriesForTSID", request_params);
    }
    catch ( Exception e ) {
        message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + PreviousNForecastTSID +
        "\") from processor.";
        Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
    }
    bean_PropList = bean.getResultsPropList();
    o_TS = bean_PropList.getContents ( "TS");
    TS PreviousNForecastTSID_TS = null;
    if ( o_TS == null ) {
        message = "Null TS requesting GetTimeSeriesForTSID(TSID=\"" + PreviousNForecastTSID +
        "\") from processor.";
        Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
    }
    else {
        PreviousNForecastTSID_TS = (TS)o_TS;
    }
     
    if ( PreviousNForecastTSID_TS == null ) {
        message = "Unable to find time series to analyze using TSID \"" + PreviousNForecastTSID + "\".";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( command_phase,
        new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
        throw new CommandWarningException ( message );
    }
    
    // Check the intervals...
    
    if ( CurrentRForecastTSID_TS.getDataIntervalBase() != TimeInterval.DAY ) {
        message = "CurrentRForecast time series does not have interval of Day";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( command_phase,
        new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a day interval time series." ) );
        throw new CommandWarningException ( message );
    }
    if ( CurrentNForecastTSID_TS.getDataIntervalBase() != TimeInterval.DAY ) {
        message = "CurrentNForecast time series does not have interval of Day";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( command_phase,
        new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a day interval time series." ) );
        throw new CommandWarningException ( message );
    }
    if ( PreviousNForecastTSID_TS.getDataIntervalBase() != TimeInterval.DAY ) {
        message = "PreviousNForecast time series does not have interval of Day";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( command_phase,
        new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a day interval time series." ) );
        throw new CommandWarningException ( message );
    }
    
    // Forecast start
    
    DateTime ForecastStart_DateTime = null;
    if ( (ForecastStart != null) && (ForecastStart.length() > 0) ) {
        request_params = new PropList ( "" );
        request_params.set ( "DateTime", ForecastStart );
        try {
            bean = processor.processRequest( "DateTime", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting DateTime(DateTime=" + ForecastStart + ") from processor.";
            Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                       message, "Report problem to software support." ) );
        }
        bean_PropList = bean.getResultsPropList();
        Object prop_contents = bean_PropList.getContents ( "DateTime" );
        if ( prop_contents == null ) {
            message = "Null value for DateTime(DateTime=" + ForecastStart + "\") returned from processor.";
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                       message, "Verify that a valid InputStart string has been specified." ) );
        }
        else {
            ForecastStart_DateTime = (DateTime)prop_contents;
        }
    }
    else {  // Get from the processor...
        try {
            Object o = processor.getPropContents ( "ForecastStart" );
            if ( o != null ) {
                ForecastStart_DateTime = (DateTime)o;
            }
        }
        catch ( Exception e ) {
            message = "Error requesting ForecastStart from processor.";
            Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
                status.addToLog ( command_phase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                           message, "Report problem to software support." ) );
        }
    }
    
    // Check units...
    
    Vector tslist = new Vector(3);
    tslist.addElement ( CurrentRForecastTSID_TS );
    tslist.addElement ( CurrentNForecastTSID_TS );
    tslist.addElement ( PreviousNForecastTSID_TS );
    if ( !TSUtil.areUnitsCompatible(tslist,true) ) {
        message = "Units for time series are different.";
        Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                       message, "Verify that the time series units are the same." ) );
            throw new CommandException ( message );
    }
    
    // Parameters for the analysis...
    
    double NoiseThreshold_double = StringUtil.atod ( NoiseThreshold );
    double ChangeCriteria_double = StringUtil.atod ( ChangeCriteria );
    double ValueCriteria_double = StringUtil.atod ( ValueCriteria );
    
	// Now try to process.
    
    try {
        String OutputFile_full = IOUtil.verifyPathForOS(
                IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),OutputFile));
        warning_count += customCommand (
                (DayTS)CurrentRForecastTSID_TS,
                (DayTS)CurrentNForecastTSID_TS,
                (DayTS)PreviousNForecastTSID_TS,
                ForecastStart_DateTime,
                NoiseThreshold_double,
                ChangeCriteria_double,
                ValueCriteria_double,
                OutputFile_full,
                status
                );
    }
    catch ( Exception e ) {
        message = "Unexpected error processing command";
        Message.printWarning ( warning_level, 
                MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
        Message.printWarning ( 3, routine, e );
        status.addToLog ( command_phase,
			new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Check log file for details." ) );
        throw new CommandException ( message );
    }

    if ( warning_count > 0 ) {
        message = "There were " + warning_count + " warnings processing the command.";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(
            command_tag, ++warning_count),
            routine,message);
        throw new CommandWarningException ( message );
    }
	
	status.refreshPhaseSeverity(command_phase,CommandStatusType.SUCCESS);
}

/**
Set the output file that is created by this command.  This is only used internally.
*/
private void setOutputFile ( File file )
{
    __OutputFile_File = file;
}

/**
Perform the test on the forecast to determine whether it passes or fails the criteria.
The returned values are "YES" (pass), "NO" (fail), or " - " (not applicable).  Returned
test results are for:
<ol>
<li>    [0] - 
</ol>
*/
private void test ( String [][] test_results, int bin,
        double NoiseThreshold_double, double ValueCriteria_double, double ChangeCriteria_double,
        double Np_total, double Nc_total, double R_total, double NcNp_diff, double RNp_diff, double RNc_diff,
        int Np_missing, int Nc_missing, int R_missing )
{   String routine = "CustomCommand_test";
        double NcNp_diff_percent = (NcNp_diff/Np_total)*100.0;
    double RNp_diff_percent = (RNp_diff/Np_total)*100.0;
    double RNc_diff_percent = (RNc_diff/Nc_total)*100.0;

    String YES = "YES";
    String NO = "NO";
    String NA = "-";
    String NO_DATA = "NO DATA";
    // Initialize to unknown

    for ( int i = 0; i < __TEST_SIZE; i++ ) {
        test_results[i][bin] = NA;
    }

    // Noise test...
    if ( testIsNoise( NoiseThreshold_double, NcNp_diff_percent, RNp_diff_percent) ) {
        // FIXME SAM 2007-12-20 Need to define what happens when in the noise condition
        if ( (Math.abs(R_missing) > 0) || (Math.abs(Np_missing) > 0) || (Math.abs(Nc_missing) > 0) ) {
            test_results[__TEST_NO_CHANGE][bin] = NO_DATA;
            test_results[__TEST_FINAL][bin] = NO_DATA;
        }
        else {
            test_results[__TEST_NO_CHANGE][bin] = YES;
            test_results[__TEST_FINAL][bin] = YES;
        }
    }
    else {
        test_results[__TEST_NO_CHANGE][bin] = NO;
        // Direction test...
        // Check the direction of RTi's forecast.
        if ( (Math.abs(R_missing) > 0) || (Math.abs(Np_missing) > 0) || (Math.abs(Nc_missing) > 0) ) {
            test_results[__TEST_DIRECTION][bin] = NO_DATA;
        }
        else if ( NcNp_diff >= 0.0 ) {
            // Positive change
            if ( (R_total - Np_total) >= 0.0 ) {
                // RTi matches.
                test_results[__TEST_DIRECTION][bin] = YES;
            }
            else {
                // RTi does not match.
                test_results[__TEST_DIRECTION][bin] = NO;
            }
        }
        else {
            // Negative change
            if ( (R_total - Np_total) <= 0.0 ) {
                // RTi matches.
                test_results[__TEST_DIRECTION][bin] = YES;
            }
            else {
                // RTi does not match.
                test_results[__TEST_DIRECTION][bin] = NO;
            }
        }
        // Value test
        if ( (Math.abs(R_missing) > 0) || (Math.abs(Nc_missing) > 0) ) {
            test_results[__TEST_VALUE][bin] = NO_DATA;
            test_results[__TEST_FINAL][bin] = NO_DATA;
        }
        else if ( Math.abs(RNc_diff_percent) <= ValueCriteria_double ) {
            // RTi forecast is within the magnitude tolerance (but direction may be off - see the other test).
            Message.printStatus ( 2, routine, "Value test passed for bin " + __BIN_NAMES[bin] );
            test_results[__TEST_VALUE][bin] = YES;
            test_results[__TEST_FINAL][bin] = YES;
        }
        else {
            test_results[__TEST_VALUE][bin] = NO;
            Message.printStatus ( 2, routine, "Value test failed for bin " + __BIN_NAMES[bin] +
                    ".  Going to change test." );
            // Change test
            if ( Math.abs((RNp_diff - NcNp_diff)/NcNp_diff)*100.0 <= ValueCriteria_double ) {
                // RTi forecast is within the magnitude tolerance (but direction may be off - see the other test).
                Message.printStatus ( 2, routine, "Change test passed for bin " + __BIN_NAMES[bin] );
                test_results[__TEST_CHANGE][bin] = YES;
                test_results[__TEST_FINAL][bin] = YES;
            }
            else {
                Message.printStatus ( 2, routine, "Change test failed for bin " + __BIN_NAMES[bin] );
                test_results[__TEST_CHANGE][bin] = NO;
                test_results[__TEST_FINAL][bin] = NO;
            }
        }
    }
    /* Final test result is as per above.  Only change below if the criteria change.
    if ( (test_results[__TEST_DIRECTION][bin].equals(YES) || test_results[__TEST_DIRECTION][bin].equals(NA)) &&
            (test_results[__TEST_NO_CHANGE][bin].equals(YES) || test_results[__TEST_NO_CHANGE][bin].equals(NA)) &&
            (test_results[__TEST_CHANGE][bin].equals(YES) || test_results[__TEST_CHANGE][bin].equals(NA)) &&
            (test_results[__TEST_VALUE][bin].equals(YES) || test_results[__TEST_VALUE][bin].equals(NA)) ) {
        // Overall success
        test_results[__TEST_FINAL][bin] = YES;
    }
    else {
        test_results[__TEST_FINAL][bin] = NO;
    }
    */
}

/**
Indicate whether the data pass the noise test (difference of R and N forecasts) are less than the
noise tolerance.
@param NoiseThreshold_double the Noise threshold, as percent (0 to 100).
@param NcNp_diff_percent the difference Nc - Np, as percent of Np.
@param RNp_diff_percent the difference R - Np, as percent of Np.
*/
private boolean testIsNoise ( double NoiseThreshold_double, double NcNp_diff_percent, double RNp_diff_percent )
{
    if ( (Math.abs(NcNp_diff_percent) <= NoiseThreshold_double) &&
            (Math.abs(RNp_diff_percent) <= NoiseThreshold_double) ) {
        return true;
    }
    else {
        return false;
    }
}

/**
Return the string representation of the command.
@param parameters Command parameters as strings.
*/
public String toString ( PropList parameters )
{	if ( parameters == null ) {
		return getCommandName() + "()";
	}
    String CurrentRForecastTSID = parameters.getValue("CurrentRForecastTSID");
    String CurrentNForecastTSID = parameters.getValue ( "CurrentNForecastTSID" );
    String PreviousNForecastTSID = parameters.getValue ( "PreviousNForecastTSID" );
    String ForecastStart = parameters.getValue ( "ForecastStart" );
    String NoiseThreshold = parameters.getValue ( "NoiseThreshold" );
    String ChangeCriteria  = parameters.getValue("ChangeCriteria");
    String ValueCriteria = parameters.getValue("ValueCriteria");
    String OutputFile = parameters.getValue("OutputFile");
    
	StringBuffer b = new StringBuffer ();
	if ( (CurrentRForecastTSID != null) && (CurrentRForecastTSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "CurrentRForecastTSID=\"" + CurrentRForecastTSID + "\"" );
	}
    if ( (CurrentNForecastTSID != null) && (CurrentNForecastTSID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "CurrentNForecastTSID=\"" + CurrentNForecastTSID + "\"" );
    }
    if ( (PreviousNForecastTSID != null) && (PreviousNForecastTSID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "PreviousNForecastTSID=\"" + PreviousNForecastTSID + "\"" );
    }
    if ( (ForecastStart != null) && (ForecastStart.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ForecastStart=\"" + ForecastStart + "\"" );
    }
    if ( (NoiseThreshold != null) && (NoiseThreshold.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "NoiseThreshold=" + NoiseThreshold );
    }
    if ( (ChangeCriteria != null) && (ChangeCriteria.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ChangeCriteria=" + ChangeCriteria );
    }
    if ( (ValueCriteria != null) && (ValueCriteria.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ValueCriteria=" + ValueCriteria );
    }
    if ( (OutputFile != null) && (OutputFile.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputFile=\"" + OutputFile + "\"");
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}
