package rti.tscommandprocessor.commands.ts;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Vector;

import RTi.Util.Math.BestFitIndicatorType;
import RTi.Util.Math.DataTransformationType;
import RTi.Util.Math.MathUtil;
import RTi.Util.Math.NumberOfEquationsType;
import RTi.Util.Math.RegressionType;
import RTi.TS.TSRegression;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeUtil;
import RTi.TS.TS;
import RTi.TS.TSUtil;
import RTi.Util.IO.IOUtil;
import RTi.Util.Time.StopWatch;

/**
This class executes the Mixed Station Analysis computing a series of
time series regressions between the dependent and independent time series.
This class relies on the TSRegression object for the regression analysis.
Multiple time series can be analyzed at the same time to promote bulk analysis, and
a report can be created to summarize the results.
This class only does analysis (no filling).
*/
public class MixedStationAnalysis
{
    
/**
Value used to indicate no best fit statistic.  This works because all errors will be >= 0 and

*/
private double __MISSING = -999.0;

/**
Dependent time series to be analyzed.
*/
private List<TS> __dependentTSList = null;

/**
Independent time series to be analyzed.
*/
private List<TS> __independentTSList = null;

/**
For each dependent time series, this is a list of TSRegression that were
created for the dependent time series during analysis.
*/
private List<List> __dependentTSRegressionList = null;

/**
Regression analysis methods to use.
*/
private List<RegressionType> __regressionTypeList = null;

/**
Transformations to use (see TRANSFORM*).
*/
private List<DataTransformationType> __transformationList = null;

/**
Number of equations to use.
*/
private NumberOfEquationsType __numberOfEquations = null;

/**
Analysis start date/time.
*/
private DateTime __analysisStart = null;

/**
Analysis end date/time.
*/
private DateTime __analysisEnd = null;

/**
Best fit indicator.
*/
private BestFitIndicatorType __bestFitIndicator  = null;

/**
Fill Start date/time.
*/
private DateTime __fillStart = null;

/**
Fill End date/time.
*/
private DateTime __fillEnd = null;

/**
Intercept when using OLS regression (null or 0 is allowed).
*/
private Double __intercept = null;

/**
Minimum overlapping data count required for acceptable analysis.
*/
private Integer __minimumDataCount = null;

/**
Minimum correlation coefficient value required for acceptable analysis.
*/
private Double __minimumR = null;

private String __nl;
String __format12p2f = "%12.2f";
String __format8p2f = "%8.2f";
String __format7d = "%7d";
String __format12d = "%12d";
String __format7p2f = "%7.2f";

// Member populated by the method rank() and used by the method
// createReportSummary() (and potentially others) to access the ranked
// TSRegressions for each dependent time series.
private int [][][] __sortedOrder = null;

/**
Construct a MixedStationAnalysis object.  Use analyizeAndRank() and createReport() for full execution.
@param bestFitIndicator the indicator to use when determining which relationship is the best fit.
@param analysisMethodList list of regression analysis methods that should be tried when finding a best fit -
if specified as null, OLS regression will be used.
@param numberOfEquations number of equations to compute, either one or monthly. 
n the future support for seasonal equations may be added.
@param analysisStart Date/time indicating the analysis period start.
@param analysisEnd Date/time indicating the analysis period end.
@param fillStart Date/time indicating the fill period start (used if filled results are generated).
@param fillEnd Date/time indicating the fill period end (used if filled results are generated).
@param transformationList list of data transformations that should be tried when finding a best fit -
if specified as null, then data will be used as is to ensure that some analysis occurs.
@param intercept the intercept y-axis intercept ("A" value in best fit equation) that
should be forced when analyzing the data.  This is currently only implemented
for a transformation of None for OLS regression and can currently only have a
value of 0.  If specified as blank, no intercept is used.  This feature is
typically only used when analyzing data for filling.
@param minimumDataCount the minimum number of overlapping data points that are required for a valid
analysis (N1 in FillRegression() and FillMOVE2() documentation).
If the minimum count is not met, then the independent time series is ignored
for the specific combination of parameters.  For example, if monthly equations
are used, the independent time series may be ignored for the specific month;
however, it may still be analyzed for other months.
@param minimumR minimum correlation coefficient that should be considered a good fit.
*/
public MixedStationAnalysis ( List<TS> dependentTSList, List<TS> independentTSList,
    BestFitIndicatorType bestFitIndicator,
    List<RegressionType> regressionTypeList, NumberOfEquationsType numberOfEquations,
    DateTime analysisStart, DateTime analysisEnd,
    DateTime fillStart, DateTime fillEnd,
    List<DataTransformationType> transformationList, Double intercept,
    Integer minimumDataCount, Double minimumR )
{
	super ();

	__dependentTSList = dependentTSList;
	__independentTSList = independentTSList;
    __bestFitIndicator = bestFitIndicator;
    __regressionTypeList = regressionTypeList;
    if ( (__regressionTypeList == null) || (__regressionTypeList.size() == 0) ) {
        // Default is OLS regression
        __regressionTypeList = new Vector();
        __regressionTypeList.add ( RegressionType.OLS_REGRESSION );
    }
    __numberOfEquations = numberOfEquations;
	__analysisStart = analysisStart;
	__analysisEnd = analysisEnd;
	__fillStart = fillStart;
	__fillEnd = fillEnd;
	__transformationList = transformationList;
	if ( (__transformationList == null) || (__transformationList.size() == 0) ) {
	    // Default is None
	    __transformationList = new Vector();
	    __transformationList.add ( DataTransformationType.NONE );
	}
	__intercept = intercept;
	__minimumDataCount = minimumDataCount;
	__minimumR = minimumR;
}

/**
Perform the mixed station analysis, for each dependent time series,
using all independent time series (except the dependent, if in the list), for
all Analysis Methods and Transformations.
@throws Exception in case of error.
*/
private void analyze()
{
	String mthd = "MixedStationAnalysis.analyze", mssg;

	StopWatch sw = new StopWatch();
	double previousSecCount;
	double currentSecCount;

	int warningCount = 0;

	int nDependentTS = __dependentTSList.size();
	int nIndependentTS = __independentTSList.size();
	int nAnalysisMethods = __regressionTypeList.size();
	int nTransformations = __transformationList.size();

	TS independentTS = null;
	TS dependentTS = null;

	TSRegression tsRegression = null;

	__dependentTSRegressionList = new Vector( nDependentTS );

	List dependentResults = null;

	int initialCapacity = nIndependentTS * nAnalysisMethods * nTransformations;
	
	StringBuffer warningText = new StringBuffer();

	// Loop for each one of the dependent time series
	for ( int iDependentTS = 0; iDependentTS < nDependentTS; iDependentTS++ ) {
	    dependentTS = __dependentTSList.get(iDependentTS);
	    dependentResults = new Vector( initialCapacity );

	    // Loop for each one of the independent time series, but skip the the dependent times series
	    // if present in the independent list so that a self-comparison does not occur.
	    for ( int iIndependentTS = 0; iIndependentTS < nIndependentTS; iIndependentTS++ ) {
	    	independentTS = (TS) __independentTSList.get(iIndependentTS);
	    	// Make sure the time series are not the same.
			if ( independentTS == dependentTS) {
				continue;
			}

			// Make sure that both time series have the same interval
			if ( independentTS.getIdentifier().getInterval() != dependentTS.getIdentifier().getInterval() ) {
		     	continue;
			}

 			// Loop for each analysis method, resetting controlling parameters to specify a unique combination.
	    	for ( int iAnalysisMethod = 0; iAnalysisMethod < nAnalysisMethods; iAnalysisMethod++ ) {
	    		// Reset properties for AnalysisMethod,
	    		RegressionType analysisMethod = __regressionTypeList.get(iAnalysisMethod);

	    		// Loop for each transformation...
	    		for ( int iTransformation = 0; iTransformation < nTransformations; iTransformation++ ) {
	    		    // Reset properties for Transformation,
	    		    DataTransformationType transformation = __transformationList.get(iTransformation);

				    // Make sure to remove the property Intercept from the prop list if the
				    // transformation is not linear and the the analysis method not OLS.
				    // Otherwise set the property.
	    		    Double intercept = null;
				    if ( analysisMethod != RegressionType.OLS_REGRESSION ) {
				        intercept = __intercept;
				    }

				    // Status message
				    mssg = "Computing regression - dependent (" + (iDependentTS+1) + " out of "
			    	+ nDependentTS + ") X Independent (" + (iIndependentTS+1) + " out of " + nIndependentTS + ")";
		    	    setParentStatusText ( mssg );

		    	    // Process the TSRegression
		    	    previousSecCount = sw.getSeconds();
		    	    sw.start();
		    	    try {
		    	    	dependentResults.add (
		    	    	    new TSRegression (independentTS, dependentTS,
		    	    	            true, // analyze for filling
		    	    	            analysisMethod,
		    	    	            intercept, __numberOfEquations,
		    	    	            null, // include all months in analysis
		    	    	            transformation,
		    	    	            __analysisStart, __analysisEnd, // Dependent analysis
		    	    	            __analysisStart, __analysisEnd, // Independent analysis (same as dependent)
		    	    	            __fillStart, __fillEnd ) );
		    	    }
		    	    catch ( Exception e ) {
		    	    	Message.printWarning( 3, mthd, e );
		    	    	warningText.append ( "\n" + e );
		    	    	++warningCount;
		    	    }
		    	    sw.stop();
		    	    currentSecCount = sw.getSeconds();
		    	    double elapsed = currentSecCount - previousSecCount;
		    	    Message.printStatus ( 2, mthd, "Dependent, Independent, Elapsed (sec) "
		    		+ iDependentTS + ", " + iIndependentTS + ", " + elapsed );
		    	}
	    	}
	    }
	    __dependentTSRegressionList.add ( dependentResults );
	}

	if ( warningCount > 0 ) {
		// Try to save the partial report and throw an exception.
		mssg = warningCount + " error(s) performing Mixed Station Analysis (" + warningText
		    + ").  Check the log file for additional information.";
		throw new RuntimeException (mssg);
	}

	sw = null;
}

/**
Analyze and rank the results.
*/
public void analyzeAndRank ()
{
    analyze();
    rank();
}

// FIXME SAM 2009-08-29 Need to evaluate this - need to pass in the equation coefficients directly because
// using multiple fill commands on the same time series will cause the coefficients to recompute, each time
// considering more filled data, rather than the original raw data values.
/**
Creates the FillRegression() and FillMOVE2() commands for each combination of
dependent, independent, transformation and analysis method.
Returns a list containing command strings.
*/
public List createFillCommands()
{
	String mthd = "MixedStationAnalysis.createFillCommands", mssg;

	TS IndependentTS = null;
	TS DependentTS   = null;
	String Transformation = "";
	String NumberOfEquations = "";
	String DependentAnalysisStart = "";
	String DependentAnalysisEnd = "";
	String IndependentAnalysisStart = "";
	String IndependentAnalysisEnd = "";
	String FillStart = "";
	String FillEnd = "";
	String Intercept = "";

	// __MinimumDataCount as integer.
	int MinimumDataCount = __minimumDataCount.intValue();

	// __MinimumR as integer.
	double MinimumR = __minimumR.doubleValue();

	List commands_Vector = new Vector();

    try {
    	// Loop for each one of the dependent time series
    	int nDependent = __dependentTSRegressionList.size();
    
    	String fillCommand = null;
    
    	for ( int dep = 0; dep < nDependent; dep++ ) {
    
    		// Get the independent list (regressions) for this dependent
    		List independentList = (List)__dependentTSRegressionList.get( dep );
    		int nIndependent = independentList.size();
    
    		// Using the same code to deal with monthly and single equation
    		// For single equation set the variable nMonth to 1.
    		int nMonth = 1;
    		if ( __numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS ) {
    		    nMonth = 12;
    		}
    
    		for ( int month = 1; month <= nMonth; month++ ) {
    
    			// Loop through the regressions for this dependent, find the first best fit,
    		    // fill the dependent and set the break from the independent loop.
    
    			for ( int ind = 0; ind < nIndependent; ind++ ) {
    
    				// Get the regression object.
    				TSRegression tsRegression = null;
    				tsRegression = (TSRegression)independentList.get(__sortedOrder[dep][month-1][ind]);
    
    	    		// Use only if regression was properly analyzed
    	    		if ( __numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS ) {
    	    			if ( !tsRegression.isAnalyzed( month ) ) {
    	    			    continue;
    	    			}
    				}
    	    		else {
    					if ( !tsRegression.isAnalyzed() ) {
    					    continue;
    					}
    				}
    
    				// Use only if the number of data points used in the analysis was >= to MinimumDataCount
    				int n;
    				try {
    	    	    	if ( __numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS ) {
    						n = tsRegression.getN1( month );
    					}
    	    	    	else {
    						n = tsRegression.getN1();
    					}
    				} catch ( Exception e ) {
    				    // FIXME SAM 2009-06-15 Handle better
    					continue;
    				}
    				if ( n < MinimumDataCount ) {
    					continue;
    				}
    
    				// Use only if the correlation coefficient is greater than the MinimumR
    				double r;
    				try {
    	    	    	if ( __numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS ) {
    						r = tsRegression.getCorrelationCoefficient( month );
    					}
    	    	    	else {
    						r = tsRegression.getCorrelationCoefficient();
    					}
    				} catch ( Exception e ) {
    				    // FIXME SAM 2009-06-15 Handle better
    					continue;
    				}
    				if ( r < MinimumR ) {
    					continue;
    				}
    
    				// Get the Dependent time series
    		    	DependentTS = tsRegression.getDependentTS();
    		    	String DependentTSID = DependentTS.getAlias();
    		    	if ( DependentTSID.length() == 0 ) {
    		    		DependentTSID = DependentTS.getIdentifierString();
    		    	}
    
    				// Get the Independent TS
      	    		IndependentTS = tsRegression.getIndependentTS();
    	    		String IndependentTSID=IndependentTS.getAlias();
    	    		if ( IndependentTSID.length() == 0 ) {
    	    			IndependentTSID = IndependentTS.getIdentifierString();
    	    		}
    
    	    	    // Number of equations
    			  	NumberOfEquations = "" + tsRegression.getNumberOfEquations();
    
    	    		// Get the Transformation
    	    		Transformation = "" + tsRegression.getTransformation();
    
    	  		  	// Get AnalysisMethod
      	    		RegressionType analysisMethod = tsRegression.getAnalysisMethod();
    
      	    		// Get the dependent analysis period start and end
      	    		DependentAnalysisStart = "" + tsRegression.getDependentAnalysisStart();
      	    		DependentAnalysisEnd = "" + tsRegression.getDependentAnalysisEnd();
    
      	    		// Get the independent analysis period start and end
      	    		IndependentAnalysisStart = "" + tsRegression.getIndependentAnalysisStart();
      	    		IndependentAnalysisEnd = "" + tsRegression.getIndependentAnalysisEnd();
    
      	    		// Get the fill period start and end
      	    		FillStart = "" + tsRegression.getFillStart();
      	    		FillEnd = "" + tsRegression.getFillEnd();
    
      	    		// Building the command (FillRegression or FillMOVE2)
    				if ( analysisMethod == RegressionType.OLS_REGRESSION ) {
      	    			fillCommand = "FillRegression";
      	    		}
    				else if ( analysisMethod == RegressionType.MOVE2 ) {
    				    fillCommand = "FillMOVE2";
      	    		}
    
    				// StringBuffer for the command parameters
    				StringBuffer b = new StringBuffer();
    
    				// Adding the DependentTSID
    				if ( DependentTSID.length() > 0 ) {
    					if ( b.length() > 0 ) {
    						b.append ( "," );
    					}
    					b.append ( "DependentTSID=\"" + DependentTSID + "\"" );
    				}
    
    				// Adding the IndependentTSID
    				if ( IndependentTSID.length() > 0 ) {
    					if ( b.length() > 0 ) {
    						b.append ( "," );
    					}
    					b.append ( "IndependentTSID=\"" + IndependentTSID + "\"" );
    				}
    
    				// Adding the NumberOfEquations
    				if ( NumberOfEquations.length() > 0 ) {
    					if ( b.length() > 0 ) {
    						b.append ( "," );
    					}
    					b.append ( "NumberOfEquations=" + NumberOfEquations );
    				}
    
    				// Adding the AnalysisMonth
    				if ( b.length() > 0 ) {
    					b.append ( "," );
    				}
    				b.append ( "AnalysisMonth=" + String.valueOf(month) );
    
    				// Adding the Transformation
    				if ( Transformation.length() > 0 ) {
    					if ( b.length() > 0 ) {
    						b.append ( "," );
    					}
    					b.append ( "Transformation=" + Transformation );
    				}
    
    				if ( analysisMethod == RegressionType.OLS_REGRESSION ) {
    
    				    // Adding the AnalysisStart
    				    if ( DependentAnalysisStart.length() > 0 ) {
    				    	if ( b.length() > 0 ) {
    				    		b.append ( "," );
    				    	}
    				    	b.append ( "AnalysisStart=\"" + DependentAnalysisStart + "\"" );
    				    }
    
    				    // Adding the AnalysisEnd
    				    if ( DependentAnalysisEnd.length() > 0 ) {
    				    	if ( b.length() > 0 ) {
    				    		b.append ( "," );
    				    	}
    				    	b.append ( "AnalysisEnd=\"" + DependentAnalysisEnd + "\"");
    				    }
    
    				}
    				else {
    				    // Adding the DependentAnalysisStart
    				    if ( DependentAnalysisStart.length() > 0 ) {
    				    	if ( b.length() > 0 ) {
    				    		b.append ( "," );
    				    	}
    				    	b.append ( "DependentAnalysisStart=\"" + DependentAnalysisStart + "\"");
    				    }
    
    				    // Adding the DependentAnalysisEnd
    				    if ( DependentAnalysisEnd.length() > 0 ) {
    				    	if ( b.length() > 0 ) {
    				    		b.append ( "," );
    				    	}
    				    	b.append ( "DependentAnalysisEnd=\"" + DependentAnalysisEnd + "\"");
    				    }
    				    // Adding the IndependentAnalysisStart
    				    if ( IndependentAnalysisStart.length()>0 ) {
    				    	if ( b.length() > 0 ) {
    				    		b.append ( "," );
    				    	}
    				    	b.append ( "IndependentAnalysisStart=\"" + IndependentAnalysisStart + "\"");
    				    }
    
    				    // Adding the IndependentAnalysisEnd
    				    if ( IndependentAnalysisEnd.length() > 0 ) {
    				    	if ( b.length() > 0 ) {
    				    		b.append ( "," );
    				    	}
    				    	b.append ( "IndependentAnalysisEnd=\"" + IndependentAnalysisEnd + "\"");
    				    }
    				}
    
    				// Adding the FillStart
    				if ( FillStart.length() > 0 ) {
    					if ( b.length() > 0 ) {
    						b.append ( "," );
    					}
    					b.append ( "FillStart=\"" + FillStart + "\"");
    				}
    
    				// Adding the FillEnd
    				if ( FillEnd.length() > 0 ) {
    					if ( b.length() > 0 ) {
    						b.append ( "," );
    					}
    					b.append ( "FillEnd=\"" + FillEnd + "\"");
    				}
    
    				// Adding the Intercept
    				if ( Intercept.length() > 0 ) {
    					if ( b.length() > 0 ) {
    						b.append ( "," );
    					}
    					b.append ( "Intercept=" + Intercept );
    				}
    
    	  	    	// Build the command, update the __command_Vector
    				String command = fillCommand + "(" + b.toString() + ")";
    				commands_Vector.add ( command );
    				Message.printStatus ( 2, mthd, command );
    
      	    		// Done with this month (or all)
      	    		break;
    			}
    	    }
    	}
    }
    catch ( Exception e ) {
    	Message.printWarning( 3, mthd, e );
    }

    return commands_Vector;
}

/**
Create the report with results.
@param outputFileFull path to output file to create.
@param outputCommentsList list of comments to add to the top of the header (typically information about
database versions, etc. used to supply data).
@param maxResultsPerIndependent maximum number of results to show for each independent time series (to limit
the report to a reasonable length and complexity).
*/
public void createReport ( File outputFileFull, List<String>outputCommentsList, Integer maxResultsPerIndependent )
throws FileNotFoundException
{
    // Original code was written to create parts of the report as string lists and then save at the end
    
    StringBuffer header = createReportHeaderText( maxResultsPerIndependent );
    StringBuffer statistics = createReportStatisticsText();
    StringBuffer summary = createReportSummaryText( maxResultsPerIndependent );
    saveReport( outputFileFull, header, statistics, summary );
    
    // TODO SAM 2009-08-30 if implemented to use HTML, will write as formatting occurs
}

/**
*/
private void createReportAppendTableHeader ( StringBuffer buffer, String depend, TSRegression tsRegression )
{
    String AnalyzeForFilling = null;
    String DependentAnalysisStart = null;
    String DependentAnalysisEnd = null;
    String FillEnd = null;
    String FillStart = null;
    String Intercept = null;
    String NumberOfEquations = null;
    String MinimumDataCount = null;
    String BestFitIndicator = null;

    buffer.append ( "Dependent Time Series: " + depend + __nl );

    // Analyze For Filling (not needed in the report)
//  AnalyzeForFilling = tsRegressionProps.getValue ("AnalyzeForFilling");
//  buffer.append ( "Analysis for filling: "+ AnalyzeForFilling + __nl );

    // Dependent Period.
    DependentAnalysisStart = "" + tsRegression.getDependentAnalysisStart();
    DependentAnalysisEnd = "" + tsRegression.getDependentAnalysisEnd();
    buffer.append ( "Dependent analysis period: from \""
        + DependentAnalysisStart + "\" to \"" + DependentAnalysisEnd   + "\"" + __nl );

    // Fill Period.
    FillStart = "" + tsRegression.getFillStart();
    FillEnd = "" + tsRegression.getFillEnd();
    // REVISIT [LT] FillEnd is returning null???
    buffer.append ( "Fill period: from \"" + FillStart + "\" to \"" + FillEnd + "\"" + __nl );

    // Intercept
    Intercept = "" + tsRegression.getIntercept();
    buffer.append ( "Intercept (only if OLS Linear): ");
    if ( Intercept.equalsIgnoreCase( "-999.0" ) ) {
        buffer.append ( __nl );
    }
    else {
        buffer.append ( Intercept + __nl );
    }

    // Number of equations
    NumberOfEquations = "" + tsRegression.getNumberOfEquations();
    buffer.append ( "Number of equations: " + NumberOfEquations + __nl);

    // Minimum data count
    // Not available from tsRegression since added by this class
    buffer.append ( "Minimum data count: " + __minimumDataCount + __nl);

    // Minimum R
    // Not available from tsRegression since added by this class
    buffer.append ( "Minimum R: " + __minimumR + __nl);

    // Best fit indicator (used here only)
    // Not available from tsRegression since added by this class.
    buffer.append ( "Best fit indicator: " + __bestFitIndicator + __nl + __nl);
}

/**
Create the report header, containing the description of the sections in the report
*/
private StringBuffer createReportHeaderText ( Integer maxResultsPerIndependent )
{
	String mthd = "MixedStationAnalysis.createReportHeader";
    StringBuffer header = new StringBuffer ();

    try {
		// Description of the reports.
		header.append ( __nl + __nl + __nl );
		header.append ( "# Mixed Station Analysis Report Format:" + __nl );
		header.append ( "#" + __nl );
		header.append ( "# Mixed Station Analysis Summary" + __nl );
		if ( maxResultsPerIndependent == null ) {
		    header.append ( "#       Summary, listed by dependent time series" );
		}
		else {
		    header.append ( "#       Summary, listed by dependent time series, with the top " + maxResultsPerIndependent );
		}
		if ( __numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS ) {
			header.append ( __nl + "#       best fit results listed per dependent and month." + __nl);
		}
		else {
			header.append ( __nl + "#       best fit results listed per dependent." + __nl);
		}
		header.append ( "#" + __nl );
		header.append ( "# Mixed Station Analysis Details" + __nl );
		if ( __numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS ) {
			header.append ( "#       This section contains three tables per dependent time series:" + __nl
			+ "#          The first lists the number of points used in the analysis." + __nl
			+ "#          The second lists the correlation coeficients." + __nl
			+ "#          The third lists the SEP values." + __nl
			+ "#       Each table lists the results per combination of independent," + __nl
			+ "#       transformation and analysis method." + __nl );
		}
		else {
			header.append (
			  "#       This section contains one table per dependent time series." + __nl
			+ "#       It lists the results per combination of independent," + __nl
			+ "#       transformation and analysis method." + __nl );
		}

		header.append ( __nl + __nl );
		return header;
	}
	catch ( Exception e ) {
		Message.printWarning( 3, mthd, e );
	}
	return header;
}

/**
Create the first section of the report, containing the statistics results from the Regressions.
*/
private StringBuffer createReportStatisticsText()
{
	String mthd = "MixedStationAnalysis.createReportStatistics", mssg;

	// TSRegression properties
	String AnalysisMethod = null;
	String AnalysisMonth = null;
	String AnalyzeForFilling = null;
	String DependentAnalysisStart = null;
	String DependentAnalysisEnd	= null;
	String FillEnd = null;
	String FillStart = null;
	String IndependentAnalysisEnd = null;
	String IndependentAnalysisStart	= null;
	String Intercept = null;
	String NumberOfEquations = null;
	String Transformation = null;
	String MinimumDataCount = null;
	String MinimumR = null;
	String BestFitIndicator = null;

	List independentList = null;
	TSRegression tsRegression = null;
	TS dependentTS = null;
	TS independentTS = null;

	String line;

    StringBuffer statistics = new StringBuffer ();
    
	try {

		StringBuffer sDataCount = null;
		StringBuffer sCorrelation = null;
		StringBuffer sRMSE = null;

		int dependentStatisticsSize = __dependentTSRegressionList.size();

		for ( int dp = 0; dp < dependentStatisticsSize; dp++ ) {
			sDataCount = new StringBuffer ();
			sCorrelation = new StringBuffer ();
			sRMSE = new StringBuffer ();

			// Get the regression list for this dependent time series.
			independentList = (List)__dependentTSRegressionList.get( dp );

			// Find out the number of regression analysis for this dependent time series
			int independentListSize = independentList.size();

			// Loop through the regression for this dependent time series and create the report.
			tsRegression = null;
			boolean firstTime = true;
			tsRegression = null;

			sDataCount.append ( __nl + __nl );
			sCorrelation.append ( __nl  );
			sRMSE.append ( __nl  );

			String previousIndependent = "";

			String endLine3Data = "";
			String endLine3Corr = "";
	    	String endLine3RMSE = "";

			for ( int rl = 0; rl < independentListSize; rl++ ) {

    			// Alternative to the report below.
    			//sCorrelation.append(tsRegression.toString());

				// Get the regression object.
				tsRegression = (TSRegression)independentList.get(rl);
			    NumberOfEquationsType numberOfEquations = tsRegression.getNumberOfEquations();
			    RegressionType analysisMethod = tsRegression.getAnalysisMethod();
			    DataTransformationType transformation;

    			// If first time create the header
				if ( firstTime ) {
					// Using the TSRegression stored in the __dependentStatisticsVector
					// create the analysis summary, saving it in the output file.
					statistics.append( __nl + "# Mixed Station Analysis Details");
					statistics.append( __nl + "# Landscape printing");

    				// Dependent time series
    				dependentTS = tsRegression.getDependentTS();
    				String depend = dependentTS.getIdentifierString();
	    			//	String depend = dependentTS.getAlias();
	    			//	if ( depend.length() == 0 ) {
	    			//		depend = dependentTS.getLocation();
	    			//	}

		  	    	// Append the report header
    				createReportAppendTableHeader (sDataCount, depend, tsRegression );

	   				// Preparing for table headers
    				String monthsA = " ";
    				for ( int i = 1; i <= 12; i++ ) {
    					monthsA += "   " + TimeUtil.monthAbbreviation(i) + " ";
    				}
    				String monthsB = " ";
    				for ( int i = 1; i <= 12; i++ ) {
    					monthsB += "        " + TimeUtil.monthAbbreviation(i) + " ";
    				}

    				String endLine1Data = "";
    				String endLine2Data = "";
    				String endLine1Corr = "";
    				String endLine2Corr = "";
    				String endLine1RMSE = "";
    				String endLine2RMSE = "";

    				if ( numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS ) {
  	    				endLine1Data =  "    " + "Number of points (N1)" + __nl;
  	    				endLine2Data = monthsA + __nl;
  	    				endLine3Data = "-------------------------------------------------"
    				    + "------------------------------------------------------------------"
    				    + "--------------------------------" + __nl;

    				   	endLine1Corr = "    Correlation Coefficients (r)" + __nl;
  	    				endLine2Corr = monthsA + __nl;
  	    				endLine3Corr = "-------------------------------------------------"
    				    + "------------------------------------------------------------------"
    				    + "--------------------------------" + __nl;

  	    				endLine1RMSE =  "    SEP" + __nl;
  	    				endLine2RMSE = monthsB + "|   SEP Total" + __nl;
  	    				endLine3RMSE = "-------------------------------------------------"
    				    + "------------------------------------------------------------------"
    				    + "----------------------------------------------------------" + __nl;
  	    			}
    				else {
  	    				endLine1Data = " # of points | Correlation |    SEP total" + __nl;
    					endLine2Data = "        (N1) |  Coeff. (r) |            " + __nl;
    					endLine3Data = "-----------------------------------------"
    				    	+ "--------------------------------------------------------------" + __nl;
  	    			}

    				// --------- sDataCount Table header
    				// First line
    				sDataCount.append( "  Independent | Transfor-| Analysis |     Period |     Period |" + endLine1Data );
    				// Second line
    				sDataCount.append( "  Time Series |   mation |   Method |      start |        end |" + endLine2Data );

    				if ( numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS ) {
  	    			    // ------- sCorrelation Table header
  	    			    // First line
    				    sCorrelation.append ( "  Independent | Transfor-| Analysis |     Period |     Period |" + endLine1Corr );
    				    // Second line
    				    sCorrelation.append ( "  Time Series |   mation |   Method |      start |        end |" + endLine2Corr );

    				    // -------------- sRMSE Table header
    				    // First line
    				    sRMSE.append ( "  Independent |" + endLine1RMSE );
    				    // Second line
    				    sRMSE.append ( "  Time Series |" + endLine2RMSE );
    				}

    				firstTime = false;
	    		}

  	    		// Get the independent TS
  	    		independentTS = tsRegression.getIndependentTS();
	    		String indep = independentTS.getAlias();
	    		if ( indep.length() == 0 ) {
	    			indep = independentTS.getLocation();
	    		}
	    	   	if(!previousIndependent.equalsIgnoreCase(indep)) {
				    previousIndependent = indep;
        			// Table header 3rd line & group divider
        			sDataCount.append ( endLine3Data );
    				sCorrelation.append ( endLine3Corr );
    				sRMSE.append ( endLine3RMSE );
    			}

    	    	// Add the independent TS to the line.
  	    		line = StringUtil.formatString( indep,"%13.13s" ) + " |";

				// Get the Transformation
	    		transformation = tsRegression.getTransformation();
				// Add Transformation to the correlation table
	  		  	line += StringUtil.formatString(""+Transformation,"   %6.6s |");

	  		  	// Get the AnalysisMethod
  	    		analysisMethod = tsRegression.getAnalysisMethod();
  	    		// Analysis Method to the correlation table
  	    		if ( analysisMethod == RegressionType.OLS_REGRESSION ) {
  	    			line += "      OLS |";
  	    		}
  	    		else if ( analysisMethod == RegressionType.MOVE2 ){
  	    			line += "    MOVE2 |";
  	    		}

				// Add the independent time series period
  	    		IndependentAnalysisStart = "" + tsRegression.getIndependentAnalysisStart();
  	    		IndependentAnalysisEnd = "" + tsRegression.getIndependentAnalysisEnd();
  	    		line += StringUtil.formatString(IndependentAnalysisStart," %10.10s |");
	  		  	line += StringUtil.formatString(IndependentAnalysisEnd," %10.10s |");

				// Save this portion of the line for use also by the SDataCount table.
				String dataCountLine = line;

  	    		if ( numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS ) {
  	    			// Adding Data Count
  	    			for ( int m=1; m<=12; m++ ) {
  	    			    try {
  	    			        int n1 = tsRegression.getN1(m);
  	    				    dataCountLine += StringUtil.formatString(n1, __format7d );
  	    			    }
  	    			    catch ( Exception e ) {
  	    			        dataCountLine += "    ***";
  	    			    }
  	    			}
  	    			dataCountLine += __nl;
  	    			sDataCount.append (dataCountLine);

  	    			// Adding Correlations Coefficients
  	    			for ( int m=1; m<=12; m++ ) {
  	    			    try {
  	    			        if (tsRegression.isAnalyzed(m)) {
      	    			        double cc = tsRegression.getCorrelationCoefficient(m);
      	    				    line += StringUtil.formatString(cc, __format7p2f );
	  	    			    }
	  	    			    else {
	  	    			      	line += "    ...";
	  	    			    }
  	    			    }
  	    			    catch ( Exception e ) {
  	    			        line += "    ***";
  	    			    }
  	    			}
  	    			line += __nl;
  	    			sCorrelation.append (line);

  	    			// Adding the independent TS to the line for the RMSE table.
  	    			line = StringUtil.formatString(indep, "%13.13s") + " |";

  	    			// Adding the monthly RMSE
  	    		    for ( int m=1; m<=12; m++ ) {
  	    			    try {
  	    			        if (tsRegression.isAnalyzed(m)) {
	  	    				    double rmse = tsRegression.getRMSE(m);
	  	    				    line += StringUtil.formatString(rmse, __format12p2f );
	  	    			    }
  	    			        else {
  	    			            line += "         ...";
  	    			        }
  	    			    }
  	    			    catch ( Exception e ) {
  	    			        line += "         ***";
  	    			    }
  	    		    }

    		        line += " |";

    		        // Adding the total RMSE
    		        try {
    		           if (tsRegression.isAnalyzed()) {
    		               double rmse = tsRegression.getRMSE();
    		               line += StringUtil.formatString(rmse, __format12p2f );
    		           }
    		           else {
    		               line += "         ...";
    		           }
    		        }
    		        catch ( Exception e ) {
    		            line += "         ***";
    		        }

  	    			line += __nl;
	    			sRMSE.append (line);
	  	    	}
	  	    	else {
					String f = "";

  	    			// Adding the Data Count
  	    			try {
  	    				f = __format12d +" |";
  	    			    int n1 = tsRegression.getN1();
  	    			    line += StringUtil.formatString(n1, f );
  	    			}
  	    			catch ( Exception e ) {
  	    			    line += "         *** |";
  	    			}
  	    			sDataCount.append (line);

  	    			// Adding Correlation Coefficient
  	    			line="";
  	    			try {
  	    			    if ( tsRegression.isAnalyzed() ) {
  	    		 	        f = __format12p2f + " |";
  	    			        double cc = tsRegression.getCorrelationCoefficient();
  	    			        line += StringUtil.formatString( cc, f );
  	    			    }
  	    			    else {
  	    			  	    line += "         ... |";
  	    			    }
  	    			}
  	    			catch ( Exception e ) {
  	    			    line += "         *** |";
  	    			}
  	    			sDataCount.append (line);

  	    			// Adding the RMSE
    		    	line = "";
    		    	try {
    		    	    if ( tsRegression.isAnalyzed () ) {
    		    	        double rmse = tsRegression.getRMSE();
    		    	        line += StringUtil.formatString(rmse, __format12p2f ) + __nl;
  	    			    }
  	    		    	else {
  	    			    	line += "         ..." + __nl;
  	    			    }
    		    	}
    		    	catch ( Exception e ) {
    		    	    line += "         ***" + __nl;
    		    	}
    		    	sDataCount.append (line);
  	    		}
			}

			// Append sDataCount to the __sStatistics table
			// For the "all" case all the information is in the
			// temporary sDataCount. for the monthly case the
			// temporary sCorrelation and sRMSE need to be added.
	    	statistics.append ( sDataCount );
			if ( __numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS ) {
	    		statistics.append ( sCorrelation );
	    		statistics.append ( sRMSE );
			}
			statistics.append ( __nl );
		}
	}
	catch ( Exception e ) {
	    // FIXME SAM 2009-06-15 Log it
		Message.printWarning( 3, mthd, e );
	}

	statistics.append ( __nl + __nl );
	
	return statistics;
}

/**
Create the third section of the report, containing the final summary.
*/
private StringBuffer createReportSummaryText( Integer maxResultsPerIndependent )
{
	String mthd = "MixedStationAnalysis.createReportSummary", mssg;

	// __MinimumDataCount as integer.
	int MinimumDataCount = __minimumDataCount.intValue();

	// __MinimumR as double.
	double MinimumR = __minimumR.doubleValue();

	// TSRegression properties
	RegressionType analysisMethod;
	int [] AnalysisMonth;
	boolean analyzeForFilling;
	DateTime DependentAnalysisStart;
	DateTime DependentAnalysisEnd;
	DateTime FillEnd;
	DateTime FillStart;
	DateTime IndependentAnalysisEnd;
	DateTime IndependentAnalysisStart;
	Double Intercept;
	NumberOfEquationsType numberOfEquations;
	DataTransformationType transformation;
	BestFitIndicatorType bestFitIndicator;

	List independentList = null;
	TSRegression tsRegression = null;

	TS dependentTS = null;
	TS independentTS = null;

	String line;
	String formatF12p2;
	
    StringBuffer summary = new StringBuffer ();

    try {
    	int nDependent = __dependentTSRegressionList.size();
    
    	String previousDependent = "";
    
    	// Loop the dependent objects
    	for ( int dep = 0; dep < nDependent; dep++ ) {
    
    		// Get the independent list (regressions) for this dependent time series.
    		independentList = (List)__dependentTSRegressionList.get( dep );
    
    		int nIndependent = independentList.size();
    
    		// Using the same code to deal with monthly and single equation
    		// For single equation set the variable nMonth to 1.
    		int nMonth = 1;
    		if ( __numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS ) {
    		    nMonth = 12;
    		}
    
    		int previousMonth = 0;
    		boolean firstTime = true;
    
    		mssg  =  __nl + "# Mixed Station Analysis Summary";
    		mssg +=  __nl + "# Landscape printing";
            if ( maxResultsPerIndependent != null ) {
                mssg +=  __nl + "# Top " + maxResultsPerIndependent +
                    " independent best fit relationships are listed per dependent";
    		}
    		else {
    			mssg+= __nl + "# All independent best fit relationships are listed per dependent";
    		}
    		if ( __numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS ) {
    			mssg += " and month" + __nl;
    		}
    		else {
    			mssg += __nl;
    		}
    		mssg += __nl;
    
    		summary.append ( mssg );
    
    		// Using TSRegression stored in the __dependentStatisticsVector
    		// create the analysis summary, saving it in the output file.
    		for ( int month = 1; month <= nMonth; month++ ) {
    			// Loop through the regression for this dependent time series and create the report.
    			tsRegression = null;
    
    			boolean firstIndependent = true;
    			int independentCount = 0;
    
    			for ( int ind = 0; ind < nIndependent; ind++ ) {
    
    				// Get the regression object.
    				tsRegression = null;
    				tsRegression = (TSRegression)independentList.get(__sortedOrder[dep][month-1][ind]);
    
    				// Dependent time series
			    	dependentTS = tsRegression.getDependentTS();
			    	String depend = dependentTS.getIdentifierString();
    			    //	String depend = dependentTS.getAlias();
    			    // if ( depend.length() == 0 ) {
    			    //		depend = dependentTS.getLocation();
    			    //	}
    
			    	// Append the report header
    				if ( !previousDependent.equalsIgnoreCase(depend ) ) {
    					previousDependent = depend;
    					createReportAppendTableHeader ( summary, depend, tsRegression );
    			    }
    
    			    // Number of equations
    			  	numberOfEquations = tsRegression.getNumberOfEquations();
    
    		 		// If first time create the header
    				if ( firstTime ) {
	    				// ---------- __sSummary Table header
	    				// First line
	    				summary.append ( "     |  Independent | Transfor-| Analysis |     Period |     Period |        Analysis Summary" + __nl );

	    				// __sSummary Table Second line
	    				summary.append ( "     |  Time Series |   mation |   method |      start |        end |     N1       r         SEP           A           B |   SEP Total" + __nl);
	    				firstTime = false;
    	    		}
    	    		// Table header 3rd line or month divider
    	    		if ( previousMonth != month ) {
    				summary.append (  "--------------------------------------------------------------"
    			    	+ "------------------------------------------------------------------------" + __nl);
    			    	previousMonth = month;
    	    	}
    
    	    	// List this independent only if the output list is still shorter than NumberOfBestRegressions
				if ( independentCount >= maxResultsPerIndependent.intValue() ) {
					continue;
				}
    
    	    	// List this independent only if the regression was properly analyzed.
	    		if ( numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS  ) {
	    			if ( !tsRegression.isAnalyzed( month ) ) {
	    			    continue;
	    			}
    			}
	    		else {
					if ( !tsRegression.isAnalyzed() ) {
					    continue;
					}
				}
    
    			// List this independent only if the number of data points used to compute the regression
	    		// was greater or equal to MinimumDataCount
	    		if ( numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS  ) {
    				try {
    					int n = tsRegression.getN1(month );
    					if ( n < MinimumDataCount ) {
    						continue;
    					}
    				}
    				catch ( Exception e ) {
    				    // FIXME SAM 2009-06-15 log
    					continue;
    				}
				}
	    		else {
					try {
						int n = tsRegression.getN1();
						if ( n < MinimumDataCount ) {
							continue;
						}
					}
					catch ( Exception e ) {
                        // FIXME SAM 2009-06-15 log
						continue;
					}
				}
    
    				// Use only if the correlation coefficient is greater than the MinimumR
    				double r;
    				try {
    	    			if ( numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS  ) {
	    	    			if(tsRegression.isAnalyzed( month )) {
        						r = tsRegression.getCorrelationCoefficient(	month );
        					}
    	    	    		else {
        					  	continue;
        					}
    					}
    	    			else {
    					    if(tsRegression.isAnalyzed()) {
    						    r = tsRegression.getCorrelationCoefficient();
    					    }
    					    else {
    					  	    continue;
    					    }
    					}
    				}
    				catch ( Exception e ) {
    					continue;
    				}
    				if ( r < MinimumR ) {
    					continue;
    				}
    
    				line = "";
    
    	    		// Month or Year
    	    		if ( firstIndependent ) {
    					if ( numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS  ) {
    						line += " " + TimeUtil.monthAbbreviation(month) + " |";
	    				}
    					else {
	    					line += " All |";
	    				}
	    				firstIndependent = false;
	    			}
    	    		else {
	    				line += "     |";
	    			}
    
      	    		// Get the independent TS
      	    		independentTS = tsRegression.getIndependentTS();
    	    		String indep = independentTS.getAlias();
    	    		if ( indep.length() == 0 ) {
    	    			indep = independentTS.getLocation();
    	    		}

    	    		// Add the independent TS to the table
    	    		line += StringUtil.formatString( indep,"%13.13s") + " |";
    
    				// Get the Transformation
    	    		transformation = tsRegression.getTransformation();
    				// Add Transformation to the table
    	  		  	line += StringUtil.formatString(""+transformation,"   %6.6s |");
    
    	  		  	// Get the AnalysisMethod
	  	    		analysisMethod = tsRegression.getAnalysisMethod();
	  	    		// Analysis Method to the table
	  	    		if ( analysisMethod == RegressionType.OLS_REGRESSION ) {
	  	    			line += "      OLS |";
	  	    		}
	  	    		else if ( analysisMethod == RegressionType.MOVE2 ){
	  	    			line += "    MOVE2 |";
	  	    		}
    
    				// Add the independent time series period to the table
	  	    		IndependentAnalysisStart = tsRegression.getIndependentAnalysisStart();
	  	    		IndependentAnalysisEnd = tsRegression.getIndependentAnalysisEnd();
	  	    		line += StringUtil.formatString(""+IndependentAnalysisStart," %10.10s |");
    	  		  	line += StringUtil.formatString(""+IndependentAnalysisEnd," %10.10s |");
    
	  	    		// Add the statistics (N1, r, SEP, A, B, SEP Total)
	  	    		if ( numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS  ) {
	  	    			line += getStatistics(tsRegression, month );
	  	    		}
	  	    		else {
	  	    			line += getStatistics(tsRegression );
	  	    		}
    
	  	    		// Done with this independent time series for the correlation table.
	  	    		summary.append (line);

	  	    		independentCount++;
    			}
    			// This is the one	__sSummary.append ( __nl );
    	    }
    	}
    }
    catch ( Exception e ) {
    	Message.printWarning( 2, mthd, e );
    }
	summary.append ( __nl + __nl );
	return summary;
}

/**
Fill the dependent time series using the dependent time series and regression
parameter defined during the analysis.  This method will typically be called
from the TSTool TSEngine object (do_fillMixedStation() method) after the
MixedStationAnalysis is instantiated.  Notice that the Regression analysis is
processed during the instantiation process.

All the information needed to fill the dependent time series can be obtained
from the regressions. Notice yet that only one independent time series will
be used to fill part of a dependent. The first one in the list satisfying the
properties requirement will be used.
*/
public void fill()
{
	String mthd = "MixedStationAnalysis.fill", mssg;

	Message.printStatus ( 2, mthd, "Starting..." );

	TS independentTS = null;
	TS dependentTS = null;
	DataTransformationType transformation;
	NumberOfEquationsType numberOfEquations;
	RegressionType analysisMethod;
	DateTime dependentAnalysisStart;
	DateTime dependentAnalysisEnd;
	DateTime independentAnalysisStart;
	DateTime independentAnalysisEnd;
	DateTime fillStart;
	DateTime fillEnd;
	int N1;
	double r, RMSE, A, B;

	// __MinimumDataCount as integer.
	int MinimumDataCount = __minimumDataCount.intValue();

	// __MinimumR as integer.
	double MinimumR = __minimumR.doubleValue();

    try {
    	// Loop for each one of the dependent time series
    	int nDependent = __dependentTSRegressionList.size();
    	for ( int dep = 0; dep < nDependent; dep++ ) {
    		// Get the independent list (regressions) for this dependent
    		List independentList = (List)__dependentTSRegressionList.get( dep );
    		int nIndependent = independentList.size();
    
    		// Using the same code to deal with monthly and single equation
    		// For single equation set the variable nMonth to 1.
    		int nMonth = 1;
    		if ( __numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS  ) {
    		    nMonth = 12;
    		}
    
    		for ( int month = 1; month <= nMonth; month++ ) {
    			// Loop through the regressions for this dependent, find the first best fit,
    		    // fill the dependent and set the break from the independent loop.
    
    			for ( int ind = 0; ind < nIndependent; ind++ ) {
    				// Get the regression object.
    				TSRegression tsRegression = null;
    				tsRegression = (TSRegression)independentList.get(__sortedOrder[dep][month-1][ind]);
    				analysisMethod = tsRegression.getAnalysisMethod();
    				numberOfEquations = tsRegression.getNumberOfEquations();
    				transformation = tsRegression.getTransformation();
    
    		  		// Dependent time series
			    	dependentTS = tsRegression.getDependentTS();
			    	String depend = dependentTS.getAlias();
			    	if ( depend.length() == 0 ) {
			    		depend = dependentTS.getLocation();
			    	}

    	    		// Use only if regression was properly analyzed
    	    		if ( numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS  ) {
    	    			if ( !tsRegression.isAnalyzed( month ) ) {
    	    			    continue;
    	    			}
    				}
    	    		else {
    					if ( !tsRegression.isAnalyzed() ) {
    					    continue;
    					}
    				}
    	    		
    	    		// Fill only if the minimum R is satisfied.
    	    		
                    if ( __minimumR != null  ) {
                        if ( (numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS) &&
                            (tsRegression.getCorrelationCoefficient( month ) < __minimumR.doubleValue())) {
                            continue;
                        }
                    }
                    else {
                        if ( tsRegression.getCorrelationCoefficient() < __minimumR.doubleValue() ) {
                            continue;
                        }
                    }

    				// Use only if the number of data points used in the analysis was >= to MinimumDataCount
    
    				int n;
    				try {
    	    	    	if ( numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS  ) {
    						n = tsRegression.getN1( month );
    					}
    	    	    	else {
    						n = tsRegression.getN1();
    					}
    				}
    				catch ( Exception e ) {
    					continue;
    				}
    				if ( n < MinimumDataCount ) {
    					continue;
    				}
    
    				// Get the independent TS
      	    		independentTS = tsRegression.getIndependentTS();
    	    		String indep = independentTS.getAlias();
    	    		if ( indep.length() == 0 ) {
    	    			indep = independentTS.getLocation();
    	    		}

    	    		dependentAnalysisStart = tsRegression.getDependentAnalysisStart();
    	    		dependentAnalysisEnd = tsRegression.getDependentAnalysisEnd();
    	    		independentAnalysisStart = tsRegression.getIndependentAnalysisStart();
    	    		independentAnalysisEnd = tsRegression.getIndependentAnalysisEnd();
	  	    		fillStart = tsRegression.getFillStart();
	  	    		fillEnd = tsRegression.getFillEnd();

    				// The following will recompute the statistics.  This is OK for now because it causes
	  	    		// normal logging, etc. to occur.
	  	    		// TODO SAM 2009-08-30 In the future, perhaps pass a TSRegression object so that the
	  	    		// analysis does not need to be performed again.
	  	    		TSUtil.fillRegress(dependentTS, independentTS,
	  	    		    analysisMethod, numberOfEquations,
	  	    		    __intercept, null, // no analysis months specified
	  	    		    transformation,
	  	    		    dependentAnalysisStart, dependentAnalysisEnd,
	  	    		    independentAnalysisStart, independentAnalysisEnd,
	  	    		    fillStart, fillEnd,
	  	    		    null, // No fill flag
	  	    		    null ); // No description string
	  	    		// Done with this month (or all if one equation)
	  	    		break;
    			}
	        }
    	}
    }
    catch ( Exception e ) {
    	Message.printWarning( 3, mthd, e );
    }
}

/**
Returns a string containing the number of data point used in the analysis,
the correlation coefficient, the SEP, A and B values.  This string is used
by the method createReportSummary to prepare report lines.
@param tsRegression the reference to the regression to get the statistics from.
Returns the string.
*/
private String getStatistics ( TSRegression tsRegression, int month )
{
	String retString = "";

	if ( tsRegression.isAnalyzed( month ) ) {
		// Number of points in the analysis
		try {
			int N1 = tsRegression.getN1(month);
			retString += StringUtil.formatString( N1, __format7d );
		}
		catch ( Exception e ) {
			retString += "    ???";
		}

		// Coefficient of correlation (r)
		try {
			double cc = tsRegression.getCorrelationCoefficient(month );
			retString += StringUtil.formatString (cc, __format8p2f );
		}
		catch ( Exception e ) {
			retString += "     ???";
		}

		// SEP (RMSE)
		try {
			double rmse = tsRegression.getRMSE( month );
			retString += StringUtil.formatString(rmse, __format12p2f );
		}
		catch ( Exception e ) {
			retString += "         ???";
		}

		// A
		try {
			double a = tsRegression.getA(month);
			retString += StringUtil.formatString(a, __format12p2f );
		}
		catch ( Exception e ) {
			retString += "         ???";
		}

		// B
		try {
			double b = tsRegression.getB(month);
			retString += StringUtil.formatString( b, __format12p2f );
		}
		catch ( Exception e ) {
			retString += "         ???";
		}
	}
	else {
		// The analysis was not performed for this month.
		retString += "    ...";			// N1
		retString += "     ...";		// r
		retString += "         ...";		// SEP RMSE
		retString += "         ...";		// A
		retString += "         ...";		// B
	}

	// Sep total
	retString += " |";
	try {
		if ( tsRegression.isAnalyzed() ) {
			double b = tsRegression.getRMSE();
			retString += StringUtil.formatString(b, __format12p2f );
		}
		else {
			retString += "         ...";
		}
	}
	catch ( Exception e ) {
		retString += "         ???";
	}

	retString += "\n";

	return retString;
}

/**
Returns a string containing the number of data point used in the analysis,
the correlation coefficient, the SEP, A and B values.  This string is used
by the method createReportSummary to prepare report lines.
@param tsRegression the reference to the regression to get the statistics from.
Returns the string.
*/
private String getStatistics( TSRegression tsRegression )
{
	String retString = "";

	if ( tsRegression.isAnalyzed() ) {

		// Number of points in the analysis
		try {
			int n1 = tsRegression.getN1();
			retString += StringUtil.formatString( n1, __format7d );
		}
		catch ( Exception e ) {
			retString += "    ???";
		}

		// Correlation Coefficient
		try {
			double cc = tsRegression.getCorrelationCoefficient();
			retString += StringUtil.formatString( cc, __format8p2f );
		}
		catch ( Exception e ) {
			retString += "     ???";
		}

		// SEP (RMSE)
		try {
			double rmse = tsRegression.getRMSE();
			retString += StringUtil.formatString( rmse, __format12p2f );
		}
		catch ( Exception e ) {
			retString += "         ???";
		}

		// A
		try {
			double a = tsRegression.getA();
			retString += StringUtil.formatString( a, __format12p2f );
		}
		catch ( Exception e ) {
			retString += "         ???";
		}

		// B
		try {
			double b = tsRegression.getB();
			retString += StringUtil.formatString( b, __format12p2f );
		}
		catch ( Exception e ) {
			retString += "         ???";
		}

		// Sep total
		retString += " |";
		try {
			double b = tsRegression.getRMSE();
			retString += StringUtil.formatString(b, __format12p2f);
		}
		catch ( Exception e ) {
			retString += "         ???";
		}

	}
	else {
		// The analysis was not performed for this month.
		retString += "    ...";			// N1
		retString += "     ...";		// r
		retString += "         ...";		// SEP RMSE
		retString += "         ...";		// A
		retString += "         ...";		// B
		retString += " |";
		retString += "         ...";		// SEP Total
	}

	retString += "\n";

	return retString;
}

/**
Rank the TSRegression in ascending order based on the best fit indicator.
*/
private void rank()
{
	String mthd = "MixedStationAnalysis.rank", mssg;

	List independentList    = null;
	TSRegression tsRegression = null;

	double[] values;

	int nDependent = __dependentTSRegressionList.size();
	__sortedOrder  = new int [nDependent][][];

    try {
    	// Loop over the dependent objects, which each have a list of statistics, stored in TSRegression objects.
        for ( int dep = 0; dep < nDependent; dep++ ) {
            // Get the independent list ( regressions ) for this independent
            independentList = (List)__dependentTSRegressionList.get( dep );
            int nIndependent = independentList.size();
            values = new double [nIndependent];
            
            if ( __numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS ) {
                // Result for each month are sorted (12 months)
                __sortedOrder[dep] = new int [12][nIndependent];
            
            	for ( int month = 1; month <= 12; month++ ) {
               		// Loop the independent objects, processing correlations coef. or RMSE values.
            		for ( int ind = 0; ind < nIndependent; ind++ ) {
            		    // Get the regression object.
                		tsRegression = null;
                		tsRegression = (TSRegression)independentList.get(ind);
            
                		try {
                		    switch ( __bestFitIndicator ) {
                		        case R:
                			        values[ind]= tsRegression.getCorrelationCoefficient ( month );
                			        break;
                		        case SEP:
                		            values[ind]= tsRegression.getRMSE( month );
                			    break;
                		        case SEP_TOTAL:
                		            // TODO[LT 2005-04-30] For total we could do this just once.
                		            // The sort will be the same for all months.  Keeping as is for now!
                		            values[ind]= tsRegression.getRMSE();
                		            break;
                	    	    default:
                	    	        values[ind]= tsRegression.getRMSE( month );
                			        break;
                		    }
                	    }
                		catch (Exception e ) {
            	    		// If something went wrong with the TSRegression, the getRMSE
            	    		// and getCorrelationCoefficient will throw and exception, but
            	    		// still need to keep the independent in the list of values to be able to relate
            	    		// the sorted indexes to the TSRegressions references in the independentList vectors,
            	    		// so set it to -999.99.
                			values[ind] = __MISSING;
                		}
                	}
                	try {
                    	MathUtil.sort ( values, MathUtil.SORT_QUICK, MathUtil.SORT_ASCENDING,
            	    	    __sortedOrder[dep][month-1], true );
                	}
                	catch ( Exception e ) {
                		Message.printWarning (3,"",e);
                		mssg = "Error sorting!";
              	    	rankMessage (tsRegression,mssg);
              	    }
              	}
            }
            else {
            	// Year ( 1 equation )
                __sortedOrder[dep] = new int [1][nIndependent];
            
            	// Loop the independent objects, processing correlations coefficient or RMSE values.
            	for ( int ind = 0; ind < nIndependent; ind++ ) {
                	// Get the regression object.
                	tsRegression = null;
                	tsRegression = (TSRegression)independentList.get(ind);
                
                	try {
                		switch ( __bestFitIndicator ) {
                		    case R:
                		        values[ind] = tsRegression.getCorrelationCoefficient();
                		        break;
                		    case SEP:
                		    	values[ind] = tsRegression.getRMSE();
                		    	break;
                		    case SEP_TOTAL:
                		        values[ind] = tsRegression.getRMSE();
                		        break;
            	    	    default:
            	    	        values[ind] = tsRegression.getRMSE();
                			    break;
                		}
                	} catch (Exception e ) {
                		// If something went wrong with the TSRegression, the getRMSE
            	    	// and getCorrelationCoefficient will throw and exception, but
            	    	// still need to keep the independent in the list of values to be able to relate
            	    	// the sorted indexes to the TSRegressions references in the independentList vectors,
            	    	// so set it to -999.99.
                		values[ind]= __MISSING;
                	}
                }
                try {
                   	MathUtil.sort ( values, MathUtil.SORT_QUICK, MathUtil.SORT_ASCENDING, __sortedOrder[dep][0], true );
                }
                catch ( Exception e ) {
                    Message.printWarning (3,"",e);
                    mssg = "Error sorting!";
            		rankMessage (tsRegression,mssg);
    		  	}
    		}
    	}
    } catch ( Exception e ) {
    		Message.printWarning( 1, mthd, e );
    }
}

/**
Update the status bar of the calling object, if that object instantiate this
class using the construct overload and pass in a valid not null reference to itself (this).
*/
private void rankMessage ( TSRegression tsRegression, String text )
{
	String mssg = "TSRegression (";

	// Dependent time series
	TS dependentTS = tsRegression.getDependentTS();
	String dependent = dependentTS.getAlias();
	if ( dependent.length() == 0 ) {
		dependent = dependentTS.getLocation();
	}
	mssg = mssg + dependent + ", ";

	// Independent time series
	TS independentTS = tsRegression.getIndependentTS();
	String independent = independentTS.getAlias();
	if ( independent.length() == 0 ) {
		independent = independentTS.getLocation();
	}
	mssg = mssg + independent + ", ";

	// Transformation
	DataTransformationType transformation = tsRegression.getTransformation();
	// Add Transformation to the correlation table
	mssg = mssg + transformation + ", ";

	// AnalysisMethod
	RegressionType analysisMethod = tsRegression.getAnalysisMethod();
	mssg = mssg + analysisMethod + ") - " + text + "\n";
}

/**
Save all parts of the report created by different stages of the analysis.
*/
private void saveReport( File outputFileFull, StringBuffer header, StringBuffer statistics, StringBuffer summary )
throws FileNotFoundException
{
	PrintWriter pw = null;
	try {
		FileOutputStream fos = new FileOutputStream ( outputFileFull );
		pw = new PrintWriter (fos);

		IOUtil.printCreatorHeader ( pw, "#", 80, 0);

		// Report header section
		if ( header != null ) {
			pw.print (header.toString());
		}
		else {
			pw.print("\n\nMixed Station Analysis report header section is empty - an error has occurred.\n\n");
		}

		// Summary section
		if ( summary != null ) {
			pw.print (summary.toString());
		}
		else {
			pw.print("\n\nMixed Station Analysis report summary section is empty - an error has occurred.\n\n");
		}

		// Statistics section
		if ( statistics != null ) {
			pw.print (statistics.toString());
		}
		else {
			pw.print("\n\nMixed Station Analysis report statistics section is empty - an error has occurred.\n\n");
		}

	}
	finally {
	    if ( pw != null ) {
            pw.flush();
            pw.close(); 
	    }
	}
}

// TODO [2005-04-20] This will not work if this class is moved to the TS
// library, because it should not be allowed to import the class
// fillMixedStation_JDialog, which resides in the TSTool application.
// The solution may be to implement a listener with interface as it is done in C++.
/**
Update the status bar of the calling object, if that object instantiate this
class using the construct overload and pass in a valid not null reference to itself (this).
*/
private void setParentStatusText( String text )
{
	// FIXME SAM 2009-08-30 Need to implement listener
}

}