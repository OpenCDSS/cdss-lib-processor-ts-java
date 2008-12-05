// ----------------------------------------------------------------------------
// MixedStationAnalysis -
// ----------------------------------------------------------------------------
// History:
//
// 2005-04-08	Luiz Teixeira, RTi	Created initial version.
// 2005-05-26	Luiz Teixeira, RTi	Copied the original class
//					MixedStationAnalysis() from TSTool while
//					copying fillMixedStation_JDialog and
//					splitting it into the new
//					fillMixedStation_JDialog() and
//					fillMixedStation_Command().
// 2007-05-08	SAM, RTi		Cleanup code based on Eclipse feedback.
// ----------------------------------------------------------------------------
package rti.tscommandprocessor.commands.ts;

import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Vector;

import RTi.Util.Math.MathUtil;
import RTi.Util.IO.PropList;
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
This class rely on the TSRegression object for the regression analysis.
*/
public class MixedStationAnalysis
{

private double __MISSING 	 = -999.99;
private String __ANALYSIS_OLS    = "OLSRegression";
//private String __ANALYSIS_MOVE2  = "MOVE2";

private String __TRANSFORMATION_NONE = "None";
//private String __TRANSFORMATION_LOG  = "Log";

private String __NUM_OF_EQUATIONS_ONE_EQUATION      = "OneEquation";
private String __NUM_OF_EQUATIONS_MONTHLY_EQUATIONS = "MonthlyEquations";

private String __BEST_FIT_R        = "r";
private String __BEST_FIT_SEP      = "SEP";
private String __BEST_FIT_SEPTOTAL = "SEPTotal";

private final int __INT_BEST_FIT_R        = 0;
private final int __INT_BEST_FIT_SEP      = 1;
private final int __INT_BEST_FIT_SEPTOTAL = 2;

private PropList __analysisProperties = null;
					// Stores the analysis properties.
private List __independentTSList = null;
					// Stores the independent ts identifier
					// strings
private List __dependentTSList   = null;
					// Stores the dependent ts identifier
					// strings
private List __dependentStatisticsVector = null;
					// This vector contains a list of
					// objects representing each dependent
					// time series.
					// Each of these objects is another
					// vector (refered as dependentResults)
					// containing all reference to all the
					// TSRegressions between the dependent
					// and all the independent time series.

// Analysis properties listed in logical order
private List   __AnalysisMethod_Vector = null;
					// Stores the Analysis Methods to use
private List   __Transformation_Vector = null;
					// Stores the Transformations to use
private String   __NumberOfEquations = null;
					// Stores the Num Equations to use
private boolean  __monthly;
private DateTime __AnalysisStart     = null;
					// Stores the Analysis Start to use
private DateTime __AnalysisEnd	     = null;
					// Stores the Analysis End to use
private String   __BestFitIndicator  = null;
					// Stores the Best Fit Indicator to use
private int	 __intBestFitIndicator;	// Used to store a integer id for the
					// BestFitIndicator to be used in
					// switchs
private DateTime __FillStart         = null;
					// Stores the Fill Start to use
private DateTime __FillEnd           = null;
					// Stores the Fill End Start to use
private String   __Intercept         = null;
					// Stores the Intercept to use
private String	 __MinimumDataCount  = null;
private String	 __MinimumR          = null;
private int	 __NumberOfBestRegressions;
private String   __OutputFile        = null;
					// Stores the File name to output

// Member used to store a reference to the caller object. It will be used to
// update the caller status field with information related to the processing
private FillMixedStation_JDialog __parent;

// Members used to create different sections of the report.
private StringBuffer __sHeader;		// Created by createReportStatistics
private StringBuffer __sStatistics;	// Created by createReportStatistics
private StringBuffer __sSummary;	// Created by createReportSummary

private String __nl;
String __format12p2f = "%12.2f";
String __format8p2f  =  "%8.2f";
String __format7d    =    "%7d";
String __format12d   =   "%12d";
String __format7p2f  =  "%7.2f";

// Member populated by the method rank() and used by the method
// createReportSummary() (and potentially others) to access the ranked
// TSRegressions for each dependent time series.
private int [][][] __sortedOrder = null;

/**
Perform the mixed stations analysis and create the report.

@param independentTSList The list of independent time series references (X).
@param dependentTSList   The list of dependent time series references (Y).
@param props Property list indicating how the regression should be performed.
Possible properties are listed in the table below:
<p>

<table width=100% cellpadding=10 cellspacing=0 border=2>
<tr>
<td><b>Property</b></td>   <td><b>Description</b></td>   <td><b>Default</b></td>
</tr

<tr>
<td><b>AnalysisMethod</b></td>
<td>Set to "OLSRegression" if a ordinary least squares regression, "MOVE2" if
using the MOVE.2 or "OLSRegression,MOVE2" if using both.
<td>"OLSRegression"</td>
</tr>

<tr>
<td><b>BestFitIndicator</b></td>
<td>Specifies the indicator to use when determining the best fit.  Currently
only SEP (Standard Error of Prediction) is available, defined as the square root
of the sum of differences between the known dependent value, and the value
determined from the equation of best fit.
<td>SEP</td>
</tr>

<tr>
<td><b>DependentAnalysisEnd</b></td>
<td>Date/time as a string indicating analysis period end for the dependent time
series.  This can be specified for all analysis methods.  For OLS and MOVE2,
this period will also be used for the independent time series.
<td>Full period.</td>
</tr>

<tr>
<td><b>DependentAnalysisStart</b></td>
<td>Date/time as a string indicating analysis period start for the dependent
time series.  This can be specified for all analysis methods.  For OLS and
MOVE2, this period will also be used for the independent time series.
<td>Full period.</td>
</tr>

<tr>
<td><b>FillEnd</b></td>
<td>Date/time as a string indicating filling period end.  Specify when
AnalyzeForFilling is true.
<td>Full period.</td>
</tr>

<tr>
<td><b>FillStart</b></td>
<td>Date/time as a string indicating filling period start.  Specify when
AnalyzeForFilling is true.
<td>Full period.</td>
</tr>

<tr>
<td><b>IndependentAnalysisEnd</b></td>
<td>Date/time as a string indicating analysis period end for the independent
time series.  This can be specified for the MOVE2 analysis method.
<td>Full period.</td>
</tr>

<tr>
<td><b>IndependentAnalysisStart</b></td>
<td>Date/time as a string indicating analysis period start for the independent
time series.  This can be specified for the MOVE2 analysis method.
<td>Full period.</td>
</tr>

<tr>
<td><b>Intercept</b></td>
<td>If specified, indicate the intercept (A value in best fit equation) that
should be forced when analyzing the data.  This is currently only implemented
for None (no transformation) for OLS regression and can currently only have a
value of 0.  If specified as blank, no intercept is used.  This feature is
typically only used when analyzing data for filling.
<td>Not used - intercept is calculated.</td>
</tr>

<tr>
<td><b>NumberOfEquations</b></td>
<td>Set to "OneEquation" to calculate one relationship.  Set to
"MonthlyEquations" if a monthly analysis should be done (in which
case 12 relationships will be determined).  In the future support for seasonal
equations may be added.</td>
<td>OneEquation</td>
</tr>

<tr>
<td><b>MinimumDataCount</b></td>
<td>The minimum number of overlapping data points that are required for a valid
analysis (N1 in fillRegression() and fillMOVE2() documentation).
If the minimum count is not met, then the independent time series is ignored
for the specific combination of parameters.  For example, if monthly equations
are used, the independent time series may be ignored for the specific month;
however, it may still be analyzed for other months.</td>
<td>0</td>
</tr>

<tr>
<td><b>NumberOfBestRegressions</b></td>
<td>The maximun number regression information to be listed in the report for each
dependent time series.</td>
<td>5</td>
</tr>

<tr>
<td><b>OutputFile</b></td>
<td>The file to save the reports.</td>
<td>Required</td>
</tr>

<tr>
<td><b>Transformation</b></td>
<td>Set to "Log" if a "log10" regression should be done, "None" if "No
Transformation" is required or "Log,None" if both are required.</td>
<td>None</td>
</tr>


</table>
//REVISIT [LT 2005-04-20] Need to revise how all this default values are
//                        handled
@throws Exception if an invalid results passed in.
*/
public MixedStationAnalysis ( 	List dependentTSList,
		List independentTSList,
				PropList props )
throws Exception
{
	super ();

	__dependentTSList   = dependentTSList;
	__independentTSList = independentTSList;
	__analysisProperties= new PropList ( props );
	__parent            = null;

	initialize ();
	analyze();
	createReportHeader();
	createReportStatistics();
	rank();
	createReportSummary();
	saveReport();
}

/**
Same as the default constructor, except that this overload
enable the capability of this object to update the status bar of the calling object.
The list of parameters are the same as the default construction
plus the following extra parameter:
@param parent Reference to the parent calling object.
@throws Exception if an invalid results passed in.
*/
public MixedStationAnalysis ( FillMixedStation_JDialog parent,
		List dependentTSList,
		List independentTSList,
				PropList props )
throws Exception
{
	super ();

	__dependentTSList    = dependentTSList;
	__independentTSList  = independentTSList;
	__analysisProperties = new PropList ( props );
	__parent             = parent;

	initialize ();
	analyze();
	createReportHeader();
	createReportStatistics();
	rank();
	createReportSummary();
	saveReport();
}

/**
Perform the mixed station analysis, for each dependent time series,
using all independent time series (except the dependent, if in the list), for
all Analysis Methods and Transformations.
@throws Exception in case of error.
*/
private void analyze()
throws Exception
{
	String mthd = "MixedStationAnalysis.analyze", mssg;

	StopWatch sw = new StopWatch();
	double previousSecCount;
	double currentSecCount;

	int warningCount = 0;

	try {
		int nDependentTS = 0;
		if ( __dependentTSList != null ) {
			nDependentTS = __dependentTSList.size();
		}

		int nIndependentTS = 0;
		if ( __independentTSList != null ) {
			nIndependentTS = __independentTSList.size();
		}

		int nAnalysisMethods = 0;
		if ( __AnalysisMethod_Vector != null ) {
			nAnalysisMethods = __AnalysisMethod_Vector.size();
		}

		int nTransformations = 0;
		if ( __Transformation_Vector != null ) {
			nTransformations = __Transformation_Vector.size();
		}

		TS independentTS = null;
		TS dependentTS   = null;

		TSRegression tsRegression = null;

		__dependentStatisticsVector = new Vector( nDependentTS );

		List dependentResults = null;

		int initialCapacity = nIndependentTS
				    * nAnalysisMethods
				    * nTransformations;

		// Loop for each one of the dependent time series
		for ( int nD = 0; nD < nDependentTS; nD++ ) {

		    dependentTS = (TS) __dependentTSList.get(nD);
		    dependentResults = new Vector( initialCapacity );

		    // Loop for each one of the independent time series, except
		    // the dependent times series if present in the independent
		    // list.
		    for ( int nI = 0; nI < nIndependentTS; nI++ ) {

		    	independentTS = (TS) __independentTSList.get(nI);

		    	// Make sure the time series are not the same.
			if ( independentTS == dependentTS) {
				continue;
			}

			// Make sure both time series have the same interval
			if ( independentTS.getIdentifier().getInterval() !=
			       dependentTS.getIdentifier().getInterval() ) {
			     	continue;
			}

 			// Loop for each analysis method, resetting the
		    	// proplist to contain only one analysis method.
		    	for ( int nA = 0; nA < nAnalysisMethods; nA++ ) {
		    		// Reset properties for AnalysisMethod,
		    		String AnalysisMethod = (String)
		    			__AnalysisMethod_Vector.get(nA);
		    		__analysisProperties.set(
		    			"AnalysisMethod", AnalysisMethod );

		    		// Loop for each transformation, resetting the
		    		// proplist to contain only one transformation.
		    		for ( int nT = 0; nT < nTransformations; nT++ ) {
		    		    // Reset properties for Transformation,
		    		    String Transformation =  (String)
		    		    	__Transformation_Vector.get(nT);
		    		    __analysisProperties.set(
		    		    	"Transformation", Transformation );

				    // Make sure to remove the property
				    // Intercept from the prop list if the
				    // transformation is not linear and the
				    // the analysis method not OLS.
				    // Otherwise set the property.
				    if ( __Intercept != null ) {
				    	if ( !Transformation.equalsIgnoreCase(
				    		__TRANSFORMATION_NONE) ||
					     !AnalysisMethod.equalsIgnoreCase(
				    		__ANALYSIS_OLS) ) {
				    		__analysisProperties.unSet(
				    			"Intercept");
				    	} else {
				    		__analysisProperties.set(
				    			"Intercept",
				    			__Intercept );
				    	}
				    }

				    // Status message
				    mssg = "Computing regression - "
				        + "Dependent (" + (nD+1) + " out of "
				    	+ nDependentTS
				   	+ ") X Independent (" + (nI+1) + " of "
				   	+ nIndependentTS + ")";
			    	    setParentStatusText ( mssg );

			    	    // Process the TSRegression
			    	    previousSecCount = sw.getSeconds();
				    sw.start();
			    	    try {
			    	    	dependentResults.add (
			    		    new TSRegression (
			    			independentTS,
			    			dependentTS,
			    			new PropList(
			    			   __analysisProperties) ) );
			    	    } catch ( Exception e ) {
			    	    	Message.printWarning( 1, mthd, e );
			    	    	++warningCount;
			    	    }
			    	    sw.stop();
			    	    currentSecCount = sw.getSeconds();
			    	    double elapsed =
			    	    currentSecCount - previousSecCount;
			    	    Message.printStatus ( 2, mthd,
			    		"Dependent, Independent, Elapsed (sec) "
			    		+ nD + ", " + nI + ", " + elapsed );
			    	}
			}
		    }
		    __dependentStatisticsVector.add ( dependentResults );
		}
	} catch ( Exception e ) {
		// Error other than from the TSRegression object.
		Message.printWarning( 1, mthd, e );
		throw new Exception ( e );
	}

	if ( warningCount > 0 ) {
		// Try to save the partial report and throw an exception.
		createReportStatistics();
		mssg = warningCount + " error(s) processing TSRegression. "
		+ "Please check the log file for additional information.";
		throw new Exception (mssg);
	}

	sw = null;
}

/**
*/
private void appendTableHeader (StringBuffer buffer,
				String depend,
				PropList tsRegressionProps)
{
	String AnalyzeForFilling		= null;
	String DependentAnalysisStart	= null;
	String DependentAnalysisEnd	= null;
	String FillEnd			= null;
	String FillStart			= null;
	String Intercept			= null;
	String NumberOfEquations		= null;
	String MinimumDataCount			= null;
	String BestFitIndicator			= null;

	buffer.append ( "Dependent Time Series: " + depend + __nl );

	// Analyze For Filling (not needed in the report)
//	AnalyzeForFilling =
//		tsRegressionProps.getValue ("AnalyzeForFilling");
//	buffer.append ( "Analysis for filling: "+ AnalyzeForFilling + __nl );

	// Dependent Period.
	DependentAnalysisStart =
	 	tsRegressionProps.getValue ( "DependentAnalysisStart" );
	DependentAnalysisEnd =
	 	tsRegressionProps.getValue ( "DependentAnalysisEnd" );
	buffer.append ( "Dependent analysis period: from \""
		+ DependentAnalysisStart + "\" to \""
		+ DependentAnalysisEnd   + "\"" + __nl );

	// Fill Period.
	FillStart = tsRegressionProps.getValue ( "FillStart");
	FillEnd   = tsRegressionProps.getValue ( "FillEnd"  );
	// REVISIT [LT] FillEnd is returning null???
	buffer.append ( "Fill period: from \"" + FillStart
			+ "\" to \"" + FillEnd + "\"" + __nl );

	// Intercept
	Intercept = tsRegressionProps.getValue ( "Intercept");
	buffer.append ( "Intercept (only if OLS Linear): ");
	if ( Intercept.equalsIgnoreCase( "-999.0" ) ) {
		buffer.append ( __nl );
	} else {
		buffer.append ( Intercept + __nl );
	}

	// Number of equations
	NumberOfEquations = tsRegressionProps.getValue ( "NumberOfEquations");
	buffer.append ( "Number of equations: " + NumberOfEquations + __nl);

	// Minimum data count
	// Not available from tsRegressionProps.
	buffer.append ( "Minimum data count: " + __MinimumDataCount + __nl);

	// Minimum R
	// Not available from tsRegressionProps.
	buffer.append ( "Minimum R: " + __MinimumR + __nl);

	// Best fit indicator (used here only)
	// Not available from tsRegressionProps.
	buffer.append ( "Best fit indicator: " + __BestFitIndicator + __nl + __nl);
}

/**
Creates the fillRegression() and fillMOVE2() commands for each combination of
dependent, independent, Transformation and Analysis Method,
Returns a Vector containing command strings.
*/
public List createFillCommands()
{
	String mthd = "MixedStationAnalysis.createFillCommands", mssg;

	TS IndependentTS = null;
	TS DependentTS   = null;
	String Transformation           = "";
	String AnalysisMethod           = "";
	String NumberOfEquations        = "";
	String DependentAnalysisStart   = "";
	String DependentAnalysisEnd     = "";
	String IndependentAnalysisStart = "";
	String IndependentAnalysisEnd   = "";
	String FillStart                = "";
	String FillEnd                  = "";
	String Intercept                = "";

	// __MinimumDataCount as integer.
	int MinimumDataCount = StringUtil.atoi( __MinimumDataCount );

	// __MinimumR as integer.
	int MinimumR = StringUtil.atoi( __MinimumR );

	List commands_Vector = new Vector();

try {
	// Loop for each one of the dependent time series
	int nDependent = __dependentStatisticsVector.size();

	String fillCommand = null;

	for ( int dep = 0; dep < nDependent; dep++ ) {

		// Get the independent list (regressions) for this dependent
		List independentList = (List)
			__dependentStatisticsVector.get( dep );
		int nIndependent = independentList.size();

		// Using the same code to deal with monthly and single equation
		// For single equation set the variable nMonth to 1.
		int nMonth = 1;
		if ( __monthly ) nMonth = 12;

		for ( int month = 1; month <= nMonth; month++ ) {

			// Loop throught the regressions for this dependent,
			// find the first best fit, fill the dependent and set
			// the break from the independent loop.

			for ( int ind = 0; ind < nIndependent; ind++ ) {

				// Get the regression object.
				TSRegression tsRegression = null;
				tsRegression = (TSRegression)
		    			independentList.get(
		    				__sortedOrder[dep][month-1][ind]);
		  		PropList tsRegressionProps =
		  			tsRegression.getPropList();

	    	    		// Use only if regression was properly analyzed
	    	    		if ( __monthly ) {
	    	    			if ( tsRegression.isAnalyzed( month )
	    	    				== false ) continue;
				} else {
					if ( tsRegression.isAnalyzed()
						== false ) continue;
				}

				// Use only if the number of data points used
				// in the analysis was >= to MinimumDataCount
				int n;
				try {
	    	    			if ( __monthly ) {
						n = tsRegression.getN1( month );
					} else {
						n = tsRegression.getN1();
					}
				} catch ( Exception e ) {
					continue;
				}
				if ( n < MinimumDataCount ) {
					continue;
				}

				// Use only if the correlation coefficient
				// is greater than the MinimumR
				double r;
				try {
	    	    			if ( __monthly ) {
						r = tsRegression.
						   getCorrelationCoefficient(
						   month );
					} else {
						r = tsRegression.
						   getCorrelationCoefficient();
					}
				} catch ( Exception e ) {
					continue;
				}
				if ( r < MinimumR ) {
						continue;
				}

				// Get the Dependent time series
			    	DependentTS = tsRegression.getDependentTS();
			    	String DependentTSID = DependentTS.getAlias();
			    	if ( DependentTSID.length() == 0 ) {
			    		DependentTSID = DependentTS.
			    			getIdentifierString();
			    	}

				// Get the Independent TS
	      	    		IndependentTS = tsRegression.getIndependentTS();
	    	    		String IndependentTSID=IndependentTS.getAlias();
	    	    		if ( IndependentTSID.length() == 0 ) {
	    	    			IndependentTSID = IndependentTS.
	    	    				getIdentifierString();
	    	    		}

	    	    		// Number of equations
			  	NumberOfEquations = tsRegressionProps.getValue (
			  	    	"NumberOfEquations" );

	    	    		// Get the Transformation
	    	    		Transformation = tsRegressionProps.getValue (
	  	    			"Transformation");

	  		  	// Get AnalysisMethod
	  	    		AnalysisMethod = tsRegressionProps.getValue (
	  	    			"AnalysisMethod");

	  	    		// Get the dependent analysis period start and end
	  	    		DependentAnalysisStart =
	  	    			tsRegressionProps.getValue (
	  	    			"DependentAnalysisStart");
	  	    		DependentAnalysisEnd   =
	  	    			tsRegressionProps.getValue (
	  	    			"DependentAnalysisEnd");

	  	    		// Get the independent analysis period start
	  	    		// and end
	  	    		IndependentAnalysisStart =
	  	    		 	tsRegressionProps.getValue (
	  	    			"IndependentAnalysisStart");
	  	    		IndependentAnalysisEnd =
	  	    		 	tsRegressionProps.getValue (
	  	    			"IndependentAnalysisEnd");

	  	    		// Get the fill period start and end
	  	    		FillStart = tsRegressionProps.getValue (
	  	    			"FillStart" );
	  	    		FillEnd = tsRegressionProps.getValue (
	  	    			"FillEnd" );

	  	    		// Building the command
	  	    		// (fillRegression or fillMove2)
				if ( AnalysisMethod.equalsIgnoreCase(
					__ANALYSIS_OLS ) ) {
	  	    			fillCommand = "fillRegression";
	  	    		} else {
					fillCommand = "fillMOVE2";
	  	    		}

				// StringBuffer for the command parameters
				StringBuffer b = new StringBuffer();

				// Adding the DependentTSID
				if ( DependentTSID.length() > 0 ) {
					if ( b.length() > 0 ) {
						b.append ( "," );
					}
					b.append ( "DependentTSID=\""
						+ DependentTSID + "\"" );
				}

				/// Adding the IndependentTSID
				if ( IndependentTSID.length() > 0 ) {
					if ( b.length() > 0 ) {
						b.append ( "," );
					}
					b.append ( "IndependentTSID=\""
						+ IndependentTSID + "\"" );
				}

				// Adding the NumberOfEquations
				if ( NumberOfEquations.length() > 0 ) {
					if ( b.length() > 0 ) {
						b.append ( "," );
					}
					b.append ( "NumberOfEquations="
						+ NumberOfEquations );
				}

				// Adding the AnalysisMonth
				if ( b.length() > 0 ) {
					b.append ( "," );
				}
				b.append ( "AnalysisMonth="
					+ String.valueOf(month) );

				// Adding the Transformation
				if ( Transformation.length() > 0 ) {
					if ( b.length() > 0 ) {
						b.append ( "," );
					}
					b.append ( "Transformation="
						+ Transformation );
				}

				if ( AnalysisMethod.equalsIgnoreCase(
					__ANALYSIS_OLS ) ) {

				    // Adding the AnalysisStart
				    if ( DependentAnalysisStart.length() > 0 ) {
				    	if ( b.length() > 0 ) {
				    		b.append ( "," );
				    	}
				    	b.append ( "AnalysisStart="
				    		+ DependentAnalysisStart );
				    }

				    // Adding the AnalysisEnd
				    if ( DependentAnalysisEnd.length() > 0 ) {
				    	if ( b.length() > 0 ) {
				    		b.append ( "," );
				    	}
				    	b.append ( "AnalysisEnd="
				    		+ DependentAnalysisEnd );
				    }

				} else {

				    // Adding the DependentAnalysisStart
				    if ( DependentAnalysisStart.length() > 0 ) {
				    	if ( b.length() > 0 ) {
				    		b.append ( "," );
				    	}
				    	b.append ( "DependentAnalysisStart="
				    		+ DependentAnalysisStart );
				    }

				    // Adding the DependentAnalysisEnd
				    if ( DependentAnalysisEnd.length() > 0 ) {
				    	if ( b.length() > 0 ) {
				    		b.append ( "," );
				    	}
				    	b.append ( "DependentAnalysisEnd="
				    		+ DependentAnalysisEnd );
				    }
				    // Adding the IndependentAnalysisStart
				    if ( IndependentAnalysisStart.length()>0 ) {
				    	if ( b.length() > 0 ) {
				    		b.append ( "," );
				    	}
				    	b.append ( "IndependentAnalysisStart="
				    		+ IndependentAnalysisStart );
				    }

				    // Adding the IndependentAnalysisEnd
				    if ( IndependentAnalysisEnd.length() > 0 ) {
				    	if ( b.length() > 0 ) {
				    		b.append ( "," );
				    	}
				    	b.append ( "IndependentAnalysisEnd="
				    		+ IndependentAnalysisEnd );
				    }
				}

				// Adding the FillStart
				if ( FillStart.length() > 0 ) {
					if ( b.length() > 0 ) {
						b.append ( "," );
					}
					b.append ( "FillStart=" + FillStart );
				}

				// Adding the FillEnd
				if ( FillEnd.length() > 0 ) {
					if ( b.length() > 0 ) {
						b.append ( "," );
					}
					b.append ( "FillEnd=" + FillEnd );
				}

				// Adding the Intercept
				if ( Intercept.length() > 0 ) {
					if ( b.length() > 0 ) {
						b.append ( "," );
					}
					b.append ( "Intercept=" + Intercept );
				}

	  	    		// Build the command, update the __command_Vector
				String command =
					fillCommand + "(" + b.toString() + ")";
				commands_Vector.add ( command );
				Message.printStatus ( 1, mthd, command );

	  	    		// Done with this month (or all)
	  	    		break;
			}
	        }
	}
} catch ( Exception e ) {
	Message.printWarning( 1, mthd, e );
}

return commands_Vector;
}

/**
Create the report header, containing the description of the sections in the
report
*/
private void createReportHeader ()
{
	String mthd = "MixedStationAnalysis.createReportHeader", mssg;

	try {
		__sHeader = new StringBuffer ();

		// Description of the reports.
		__sHeader.append ( __nl + __nl + __nl );
		__sHeader.append ( "# Mixed Station Analysis Report Format:" + __nl );
		__sHeader.append ( "#" + __nl );
		__sHeader.append ( "# Mixed Station Analysis Summary" + __nl );
		__sHeader.append (
			  "#       Summary, listed by dependent time series, "
			+ "with the top " + __NumberOfBestRegressions );
		if ( __monthly ) {
			__sHeader.append ( __nl
			  + "#       best fit results"
			  + " listed per dependent and month." + __nl);
		} else {
			__sHeader.append ( __nl
			  + "#       best fit results"
			  + " listed per dependent." + __nl);
		}
		__sHeader.append ( "#" + __nl );
		__sHeader.append ( "# Mixed Station Analysis Details" + __nl );
		if ( __monthly ) {
			__sHeader.append (
			"#       This section contains three tables per dependent"
			+ " time series:"
			+ __nl
			+ "#          The first lists the number of points used "
			+ "in the analysis."
			+ __nl
			+ "#          The second lists the correlation coeficients."
			+ __nl
			+ "#          The third lists the SEP values."
			+ __nl
			+ "#       Each table lists the results per combination"
			+ " of independent,"
			+ __nl
			+ "#       transformation and analysis method."
			+ __nl );
		} else {
			__sHeader.append (
			  "#       This section contains one table per dependent "
			+ "time series."
			+ __nl
			+ "#       It lists the results per combination of "
			+ "independent,"
			+ __nl
			+ "#       transformation and analysis method."
			+ __nl );
		}

		__sHeader.append ( __nl + __nl );

	} catch ( Exception e ) {
		Message.printWarning( 1, mthd, e );
	}
}

/**
Create the first section of the report, containing the statistics results from
the Regressions.
*/
private void createReportStatistics()
{
	String mthd = "MixedStationAnalysis.createReportStatistics", mssg;

	// TSRegression properties
	String AnalysisMethod		= null;
	String AnalysisMonth		= null;
	String AnalyzeForFilling	= null;
	String DependentAnalysisStart	= null;
	String DependentAnalysisEnd	= null;
	String FillEnd			= null;
	String FillStart		= null;
	String IndependentAnalysisEnd	= null;
	String IndependentAnalysisStart	= null;
	String Intercept		= null;
	String NumberOfEquations	= null;
	String Transformation		= null;
	String MinimumDataCount		= null;
	String MinimumR			= null;
	String BestFitIndicator		= null;

	List independentList     = null;
	TSRegression tsRegression  = null;
	TS dependentTS		   = null;
	TS independentTS	   = null;
	PropList tsRegressionProps = null;

	String line;

	try {
		__sStatistics = new StringBuffer ();

		StringBuffer sDataCount   = null;
		StringBuffer sCorrelation = null;
		StringBuffer sRMSE        = null;

		int dependentStatisticsSize = __dependentStatisticsVector.size();

		for ( int dp = 0; dp < dependentStatisticsSize; dp++ ) {

			sDataCount   = new StringBuffer ();
			sCorrelation = new StringBuffer ();
			sRMSE        = new StringBuffer ();

			// Get the regression list for this dependent time series.
			independentList = (List)
				__dependentStatisticsVector.get( dp );

			// Find out the number of regression analysis for this
			// dependent time series
			int independentListSize = independentList.size();

			// Loop throught the regression for this dependent time
			// series and create the report.
			tsRegression      = null;
			boolean firstTime = true;
			tsRegression      = null;

			sDataCount.append   ( __nl + __nl );
			sCorrelation.append ( __nl  );
			sRMSE.append        ( __nl  );

			String previousIndependent = "";

			String endLine3Data = "";
			String endLine3Corr = "";
	    		String endLine3RMSE = "";

			for ( int rl = 0; rl < independentListSize; rl++ ) {

	    			// Alternative to the report below.
	    			//sCorrelation.append(tsRegression.toString());

				// Get the regression object.
				tsRegression = (TSRegression)
		    			independentList.get(rl);
		  		tsRegressionProps = tsRegression.getPropList();

	    			// If first time create the header
				if ( firstTime ) {

					// Using the TSRegression stored in the
					// __dependentStatisticsVector
					// create the analysis summary, saving
					// it in the output file.
					__sStatistics.append( __nl +
					      "# Mixed Station Analysis Details");
					__sStatistics.append( __nl
					    + "# Landscape printing");

	    				// Dependent time series
	    				dependentTS =
	    					tsRegression.getDependentTS();
	    				String depend = dependentTS.
	    					getIdentifierString();
	    			//	String depend = dependentTS.getAlias();
	    			//	if ( depend.length() == 0 ) {
	    			//		depend =
	    			//		    dependentTS.getLocation();
	    			//	}

			  	    	// Append the report header
	    				appendTableHeader (sDataCount,
	    					depend, tsRegressionProps );

	    				// Number of equations
			  	    	NumberOfEquations  =
			  	    		tsRegressionProps.getValue (
			  	    		"NumberOfEquations");

	   				// Preparing for table headers
	    				String monthsA = " ";
	    				for ( int i = 1; i <= 12; i++ ) {
	    					monthsA += "   "
	    					+ TimeUtil.monthAbbreviation(i)
	    					+ " ";
	    				}
	    				String monthsB = " ";
	    				for ( int i = 1; i <= 12; i++ ) {
	    					monthsB += "        "
	    					+ TimeUtil.monthAbbreviation(i)
	    					+ " ";
	    				}

	    				String endLine1Data = "";
	    				String endLine2Data = "";
	    				String endLine1Corr = "";
	    				String endLine2Corr = "";
	    				String endLine1RMSE = "";
	    				String endLine2RMSE = "";

	    				if ( __monthly ) {

	  	    				endLine1Data =  "    " +
	  	    				"Number of points (N1)" + __nl;
	  	    				endLine2Data = monthsA + __nl;
	  	    				endLine3Data = "----------------"
	    				    + "---------------------------------"
	    				    + "---------------------------------"
	    				    + "---------------------------------"
	    				    + "--------------------------------"
	    				    + __nl;

	    				    	endLine1Corr = "    " +
	  	    				"Correlation Coefficients (r)"
	  	    				+ __nl;
	  	    				endLine2Corr = monthsA + __nl;
	  	    				endLine3Corr = "----------------"
	    				    + "---------------------------------"
	    				    + "---------------------------------"
	    				    + "---------------------------------"
	    				    + "--------------------------------"
	    				    + __nl;

	  	    				endLine1RMSE =  "    " +
	  	    				"SEP" + __nl;
	  	    				endLine2RMSE = monthsB
	  	    				+ "|   SEP Total" + __nl;
	  	    				endLine3RMSE = "----------------"
	    				    + "---------------------------------"
	    				    + "---------------------------------"
	    				    + "---------------------------------"
	    				    + "---------------------------------"
	    				    + "-------------------------"
	    				    + __nl;

	  	    			} else {
	  	    				endLine1Data = " # of points |"
	  	    					     + " Correlation |"
	  	    				             + "    SEP total"
	  	    				             + __nl;
	    					endLine2Data = "        (N1) |"
	    						     + "  Coeff. (r) |"
	    					             + "            "
	    					             + __nl;
	    					endLine3Data = "--------------"
	    				   	+ "---------------------------"
	    				    	+ "---------------------------"
	    				    	+ "---------------------------"
	    				    	+ "--------" + __nl;
	  	    			}

	    				// --------- sDataCount Table header
	    				// First line
	    				sDataCount.append( "  Independent |"
	    				+ " Transfor-| Analysis |"
	    				+ "     Period |     Period |"
	    				+ endLine1Data );
	    				// Second line
	    				sDataCount.append( "  Time Series |"
	    				+ "   mation |   Method |"
	    				+ "      start |        end |"
	    				+ endLine2Data );

	    				if ( __monthly ) {

	  	    			    // ------- sCorrelation Table header
	  	    			    // First line
	    				    sCorrelation.append (
	    				      "  Independent |"
	    				    + " Transfor-| Analysis |"
	    				    + "     Period |     Period |"
	    				    + endLine1Corr );
	    				    // Second line
	    				    sCorrelation.append (
	    				      "  Time Series |"
	    				    + "   mation |   Method |"
	    				    + "      start |        end |"
	    				    + endLine2Corr );

	    				    // -------------- sRMSE Table header
	    				    // First line
	    				    sRMSE.append ( "  Independent |"
	    				    + endLine1RMSE );
	    				    // Second line
	    				    sRMSE.append ( "  Time Series |"
	    				    + endLine2RMSE );
	    				}

	    				firstTime = false;
	    	    		}

	      	    		// Get the independent TS
	      	    		independentTS = tsRegression.getIndependentTS();
	    	    		String indep = independentTS.getAlias();
	    	    		if ( indep.length() == 0 ) {
	    	    			indep = independentTS.getLocation();
	    	    		}
	    	    		if(!previousIndependent.equalsIgnoreCase(indep))
	    	    		{
				      	previousIndependent = indep;
					// Table header 3rd line & group divider
					sDataCount  .append ( endLine3Data );
	    				sCorrelation.append ( endLine3Corr );
	    				sRMSE       .append ( endLine3RMSE );
	    			}

	    	    		// Add the independent TS to the line.
	  	    		line= StringUtil.formatString( indep,"%13.13s" )
	  	    			+ " |";

				// Get the Transformation
	    	    		Transformation = tsRegressionProps.getValue (
	  	    			"Transformation");
				// Add Transformation to the correlation table
	  		  	line += StringUtil.formatString(
	  		  		Transformation,"   %6.6s |");

	  		  	// Get the AnalysisMethod
	  	    		AnalysisMethod = tsRegressionProps.getValue (
	  	    			"AnalysisMethod");
	  	    		// Analysis Method to the correlation table
	  	    		if ( AnalysisMethod.equalsIgnoreCase(
	  	    			__ANALYSIS_OLS) ) {
	  	    			line += "      OLS |";
	  	    		} else {
	  	    			line += "    MOVE2 |";
	  	    		}

				// Add the independent time series period
	  	    		IndependentAnalysisStart =
	  	    		 	tsRegressionProps.getValue (
	  	    			"IndependentAnalysisStart");
	  	    		IndependentAnalysisEnd =
	  	    		 	tsRegressionProps.getValue (
	  	    			"IndependentAnalysisEnd");
	  	    		line += StringUtil.formatString(
	  		  		IndependentAnalysisStart,
	  		  		" %10.10s |");
	  		  	line += StringUtil.formatString(
	  		  		IndependentAnalysisEnd,
	  		  		" %10.10s |");

				// Save this portion of the line for use also by
				// the SDataCount table.
				String dataCountLine = line;

	  	    		if ( __monthly ) {

	  	    			// Adding Data Count
	  	    			for ( int m=1; m<=12; m++ ) {
	  	    			    try {
	  	    				int n1 = tsRegression.
	  	    				   getN1(m);
	  	    				dataCountLine +=
	  	    				   StringUtil.formatString(
	  	    					n1, __format7d );
	  	    			    } catch ( Exception e ) {
	  	    				dataCountLine += "    ***";
	  	    			    }
	  	    			}
	  	    			dataCountLine += __nl;
	  	    			sDataCount.append (dataCountLine);

	  	    			// Adding Correlations Coefficients
	  	    			for ( int m=1; m<=12; m++ ) {
	  	    			    try {
	  	    			      if (tsRegression.isAnalyzed(m)) {
	  	    				double cc = tsRegression.
	  	    				   getCorrelationCoefficient(m);
	  	    				line +=
	  	    				   StringUtil.formatString(
	  	    					cc, __format7p2f );
	  	    			      } else {
	  	    			      	line += "    ...";
	  	    			      }
	  	    			    } catch ( Exception e ) {
	  	    				line += "    ***";
	  	    			    }
	  	    			}
	  	    			line += __nl;
	  	    			sCorrelation.append (line);

	  	    			// Adding the independent TS to the line
	  	    			// for the RMSE table.
	  	    			line = StringUtil.formatString(
	  	    				indep, "%13.13s") + " |";

	  	    			// Adding the monthly RMSE
	  	    		    	for ( int m=1; m<=12; m++ ) {
	  	    			    try {
	  	    			      if (tsRegression.isAnalyzed(m)) {
	  	    				double rmse = tsRegression.
	  	    					getRMSE(m);
	  	    				line += StringUtil.formatString(
	  	    					rmse, __format12p2f );
	  	    			      } else {
	  	    			      	line += "         ...";
	  	    			      }
	  	    			    } catch ( Exception e ) {
	  	    			        line += "         ***";
	  	    			    }
	  	    		        }

	  	    		        line += " |";

	  	    		        // Adding the total RMSE
	  	    		        try {
	  	    		          if (tsRegression.isAnalyzed()) {
	  	    			    double rmse = tsRegression.getRMSE();
	  	    			    line += StringUtil.formatString(
	  	    					rmse, __format12p2f );
	  	    			  } else {
	  	    			  	line += "         ...";
	  	    			  }
	  	    			} catch ( Exception e ) {
	  	    			    line += "         ***";
	  	    			}

	  	    			line += __nl;

		    			sRMSE.append (line);

	  	    		} else {

					String f = "";

	  	    			// Adding the Data Count
	  	    			try {
	  	    				f = __format12d +" |";
	  	    			    int n1 = tsRegression.getN1();
	  	    			    line += StringUtil.formatString(
	  	    			    	n1, f );
	  	    			} catch ( Exception e ) {
	  	    			    line += "         *** |";
	  	    			}
	  	    			sDataCount.append (line);

	  	    			// Adding Correlation Coefficient
	  	    			line="";
	  	    			try {
	  	    			  if ( tsRegression.isAnalyzed() ) {
	  	    		 	    f = __format12p2f + " |";
	  	    			    double cc = tsRegression.
	  	    				   getCorrelationCoefficient();
	  	    			    line += StringUtil.formatString(
	  	    			    	cc, f );
	  	    			  } else {
	  	    			  	line += "         ... |";
	  	    			  }
	  	    			} catch ( Exception e ) {
	  	    			    line += "         *** |";
	  	    			}
	  	    			sDataCount.append (line);

	  	    			// Adding the RMSE
	  	    		    	line = "";
	  	    		    	try {
	  	    		    	    if ( tsRegression.isAnalyzed () ) {
	  	    				double rmse = tsRegression.
	  	    				   getRMSE();
	  	    				line += StringUtil.formatString(
	  	    			    	rmse, __format12p2f ) + __nl;
	  	    			    } else {
	  	    			    	line += "         ..." + __nl;
	  	    			    }
	  	    		    	} catch ( Exception e ) {
	  	    				line += "         ***" + __nl;
	  	    		    	}
		    		    	sDataCount.append (line);
	  	    		}
			}

			// Append sDataCount to the __sStatistics table
			// For the "all" case all the information is in the
			// temporary sDataCount. for the monthly case the
			// temporary sCorrelation and sRMSE need to be added.
		    	__sStatistics.append ( sDataCount );
			if ( __monthly ) {
		    		__sStatistics.append ( sCorrelation );
		    		__sStatistics.append ( sRMSE );
			}
			__sStatistics.append ( __nl );
		}

	} catch ( Exception e ) {
		Message.printWarning( 1, mthd, e );
	}

	__sStatistics.append ( __nl + __nl );
}

/**
Create the third section of the report, containing the final summary.
*/
private void createReportSummary()
{
	String mthd = "MixedStationAnalysis.createReportSummary", mssg;

	// __MinimumDataCount as integer.
	int MinimumDataCount = StringUtil.atoi( __MinimumDataCount );

	// __MinimumR as double.
	double MinimumR = StringUtil.atof( __MinimumR );

	// TSRegression properties
	String AnalysisMethod		= null;
	String AnalysisMonth		= null;
	String AnalyzeForFilling	= null;
	String DependentAnalysisStart	= null;
	String DependentAnalysisEnd	= null;
	String FillEnd			= null;
	String FillStart		= null;
	String IndependentAnalysisEnd	= null;
	String IndependentAnalysisStart	= null;
	String Intercept		= null;
	String NumberOfEquations	= null;
	String Transformation		= null;
	String BestFitIndicator		= null;

	List independentList    = null;
	TSRegression tsRegression = null;

	TS dependentTS	= null;
	TS independentTS= null;

	PropList tsRegressionProps = null;

	String line;
	String formatF12p2;

try {
	__sSummary = new StringBuffer ();

	int nDependent = __dependentStatisticsVector.size();

	String previousDependent = "";

	// Loop the dependent objects
	for ( int dep = 0; dep < nDependent; dep++ ) {

		// Get the independent list (regressions) for this dependent
		// time series.
		independentList = (List)
			__dependentStatisticsVector.get( dep );

		int nIndependent = independentList.size();

		// Using the same code to deal with monthly and single equation
		// For single equation set the variable nMonth to 1.
		int nMonth = 1;
		if ( __monthly ) nMonth = 12;

		int previousMonth = 0;
		boolean firstTime = true;

		mssg  =  __nl + "# Mixed Station Analysis Summary";
		mssg +=  __nl + "# Landscape printing";
		mssg +=  __nl + "# Top " + __NumberOfBestRegressions;
		if ( __NumberOfBestRegressions > 1 ) {
			mssg+= " independent best fit are listed per dependent";
		} else {
			mssg+= " independent best fit is listed per dependent";
		}
		if ( __monthly ) {
			mssg += " and month" + __nl;
		} else {
			mssg += __nl;
		}
		mssg += __nl;

		__sSummary.append ( mssg );

		// Using TSRegression stored in the __dependentStatisticsVector
		// create the analysis summary, saving it in the output file.
		for ( int month = 1; month <= nMonth; month++ ) {
			// Loop throught the regression for this dependent time
			// series and create the report.
			tsRegression = null;

			boolean firstIndependent = true;
			int independentCount = 0;

			for ( int ind = 0; ind < nIndependent; ind++ ) {

				// Get the regression object.
				tsRegression = null;
				tsRegression = (TSRegression)
		    			independentList.get(
		    				__sortedOrder[dep][month-1][ind]);
		  		tsRegressionProps = tsRegression.getPropList();

				// Dependent time series
			    	dependentTS = tsRegression.getDependentTS();
			    	String depend = dependentTS.getIdentifierString();
			    //	String depend = dependentTS.getAlias();
			    // if ( depend.length() == 0 ) {
			    //		depend = dependentTS.getLocation();
			    //	}

			    	// Append the report header
				if ( !previousDependent.equalsIgnoreCase(
					depend ) )
				{
					previousDependent = depend;
					appendTableHeader ( __sSummary, depend,
						tsRegressionProps );
			    	}

			    	// Number of equations
			  	 NumberOfEquations  =
			  	    	tsRegressionProps.getValue (
			  	    		"NumberOfEquations" );

		 		// If first time create the header
				if ( firstTime ) {

	    				// ---------- __sSummary Table header
	    				// First line
	    				__sSummary.append ( "     |"
	    				+ "  Independent |"
	    				+ " Transfor-| Analysis |"
	    				+ "     Period |     Period |"
	    				+ "        Analysis Summary" + __nl );

	    				// __sSummary Table Second line
	    				__sSummary.append ( "     |"
	    				+ "  Time Series |"
	    				+ "   mation |   method |"
	    				+ "      start |        end |"
	    				+ "     N1       r         SEP"
	    				+ "           A           B |"
	    				+ "   SEP Total"
	    				+ __nl);

	    				firstTime = false;
	    	    		}

	    	    		// Table header 3rd line or month divider
	    	    		if ( previousMonth != month ) {
	    				__sSummary.append (  "--------"
	    			    	+ "-------------------------"
	    			    	+ "-----------------------------"
	    			    	+ "-----------------------------"
	    			    	+ "-----------------------------"
	    			    	+ "--------------" + __nl);
	    			    	previousMonth = month;
	    			}

	    	    		// List this independent only if the output list
				// is still shorter than NumberOfBestRegressions
				if( independentCount >=
					__NumberOfBestRegressions ) {
					continue;
				}

	    	    		// List this independent only if the regression
	    	    		// was properly analyzed.
	    	    		if ( __monthly ) {
	    	    			if ( tsRegression.isAnalyzed( month )
	    	    				== false ) continue;
				} else {
					if ( tsRegression.isAnalyzed()
						== false) continue;
				}

				// List this independent only if the number of
				// data points used to compute the regression
	    	    		// was greater or equal to MinimumDataCount
	    	    		if ( __monthly ) {
					try {
						int n = tsRegression.getN1(
							month );
						if ( n < MinimumDataCount ) {
							continue;
						}
					} catch ( Exception e ) {
						continue;
					}
				} else {
					try {
						int n = tsRegression.getN1();
						if ( n < MinimumDataCount ) {
							continue;
						}
					} catch ( Exception e ) {
						continue;
					}
				}

				// Use only if the correlation coefficient
				// is greater than the MinimumR
				double r;
				try {
	    	    			if ( __monthly ) {
	    	    			  if(tsRegression.isAnalyzed( month )) {
						r = tsRegression.
						   getCorrelationCoefficient(
						   	month );
					  } else {
					  	continue;
					  }
					} else {
					  if(tsRegression.isAnalyzed()) {
						r = tsRegression.
						   getCorrelationCoefficient();
					  } else {
					  	continue;
					  }
					}
				} catch ( Exception e ) {
					continue;
				}
				if ( r < MinimumR ) {
					continue;
				}

				line = "";

	    	    		// Month or Year
	    	    		if ( firstIndependent ) {
					if ( __monthly ) {
						line += " "
						+ TimeUtil.monthAbbreviation(
	    						month)
	    					+ " |";
	    				} else {
	    					line += " All |";
	    				}
	    				firstIndependent = false;
	    			} else {
	    				line += "     |";
	    			}

	      	    		// Get the independent TS
	      	    		independentTS = tsRegression.getIndependentTS();
	    	    		String indep = independentTS.getAlias();
	    	    		if ( indep.length() == 0 ) {
	    	    			indep = independentTS.getLocation();
	    	    		}

	    	    		// Add the independent TS to the table
	  	    		line += StringUtil.formatString( indep,"%13.13s")
	  	    			+ " |";

				// Get the Transformation
	    	    		Transformation = tsRegressionProps.getValue (
	  	    			"Transformation");
				// Add Transformation to the table
	  		  	line += StringUtil.formatString(
	  		  		Transformation,"   %6.6s |");

	  		  	// Get the AnalysisMethod
	  	    		AnalysisMethod = tsRegressionProps.getValue (
	  	    			"AnalysisMethod");
	  	    		// Analysis Method to the table
	  	    		if ( AnalysisMethod.equalsIgnoreCase(
	  	    			__ANALYSIS_OLS) ) {
	  	    			line += "      OLS |";
	  	    		} else {
	  	    			line += "    MOVE2 |";
	  	    		}

				// Add the independent time series period
				// to the table
	  	    		IndependentAnalysisStart =
	  	    		 	tsRegressionProps.getValue (
	  	    			"IndependentAnalysisStart");
	  	    		IndependentAnalysisEnd =
	  	    		 	tsRegressionProps.getValue (
	  	    			"IndependentAnalysisEnd");
	  	    		line += StringUtil.formatString(
	  		  			IndependentAnalysisStart,
	  		  			" %10.10s |");
	  		  	line += StringUtil.formatString(
	  		  			IndependentAnalysisEnd,
	  		  			" %10.10s |");

	  	    		// Add the statistics
	  	    		// (N1, r, SEP, A, B, SEP Total)
	  	    		if ( __monthly ) {
	  	    			line += getStatistics(
	  	    				tsRegression, month );
	  	    		} else {
	  	    			line += getStatistics(
	  	    				tsRegression );
	  	    		}

	  	    		// Done with this independent time series
	  	    		// for the correlation table.
	  	    		__sSummary.append (line);

	  	    		independentCount++;
			}

		// This is the one	__sSummary.append ( __nl );
	        }
	}
} catch ( Exception e ) {
	Message.printWarning( 1, mthd, e );
}

	__sSummary.append ( __nl + __nl );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
{
	__independentTSList      = null;
	__dependentTSList        = null;
	__analysisProperties     = null;

	__AnalysisMethod_Vector  = null;
	__Transformation_Vector  = null;
	__AnalysisStart          = null;
	__AnalysisEnd	         = null;
	__FillStart              = null;
	__FillEnd                = null;

	__BestFitIndicator	 = null;
	__Intercept	 	 = null;
	__MinimumDataCount	 = null;
	__MinimumR	 	 = null;
	__OutputFile	 	 = null;

	__dependentStatisticsVector = null;

	__sHeader     = null;
	__sStatistics = null;
	__sSummary    = null;

	__nl = null;
}

/**
Fill the dependent time series using the dependent time series and regression
parameter defined during the analysis.  This method will typically be called
from the TSTool TSEngine object (do_fillMixedStation() method) after the
MixedStationAnalysis is instantiated.  Notice that the Regression analysis is
processed during the instantiation process.

All the information needed to fill the dependent time series can be obtained
from the regressions. Notice yet that only one independent time series will
be used to fill par of a dependent. The first one in the list satisfying the
properties requirement will be used.
*/
public void fill()
{
	String mthd = "MixedStationAnalysis.fill", mssg;

	Message.printStatus ( 1, mthd, "Stating ..." );

	TS independentTS = null;
	TS dependentTS   = null;
	String Transformation = "";
	String AnalysisMethod = "";
	String DependentAnalysisStart   = "";
	String DependentAnalysisEnd     = "";
	String IndependentAnalysisStart = "";
	String IndependentAnalysisEnd   = "";
	String FillStart = "";
	String FillEnd   = "";
	int N1;
	double r, RMSE, A, B;

	// __MinimumDataCount as integer.
	int MinimumDataCount = StringUtil.atoi( __MinimumDataCount );

	// __MinimumR as integer.
	double MinimumR = StringUtil.atof( __MinimumR );

try {
	// Loop for each one of the dependent time series
	int nDependent = __dependentStatisticsVector.size();

	for ( int dep = 0; dep < nDependent; dep++ ) {

		// Get the independent list (regressions) for this dependent
		List independentList = (List)
			__dependentStatisticsVector.get( dep );
		int nIndependent = independentList.size();

		// Using the same code to deal with monthly and single equation
		// For single equation set the variable nMonth to 1.
		int nMonth = 1;
		if ( __monthly ) nMonth = 12;

		for ( int month = 1; month <= nMonth; month++ ) {

			// Loop throught the regressions for this dependent,
			// find the first best fit, fill the dependent and set
			// the break from the independent loop.

			for ( int ind = 0; ind < nIndependent; ind++ ) {

				// Get the regression object.
				TSRegression tsRegression = null;
				tsRegression = (TSRegression)
		    			independentList.get(
		    				__sortedOrder[dep][month-1][ind]);
		  		PropList tsRegressionProps =
		  			tsRegression.getPropList();

		  		// Dependent time series
			    	dependentTS = tsRegression.getDependentTS();
			    	String depend = dependentTS.getAlias();
			    	if ( depend.length() == 0 ) {
			    		depend = dependentTS.getLocation();
			    	}

	    	    		// Use only if regression was properly analyzed
	    	    		if ( __monthly ) {
	    	    			if ( tsRegression.isAnalyzed( month )
	    	    				== false ) continue;
				} else {
					if ( tsRegression.isAnalyzed()
						== false ) continue;
				}

				// Use only if the number of data points used
				// in the analysis was >= to MinimumDataCount

				int n;
				try {
	    	    			if ( __monthly ) {
						n = tsRegression.getN1( month );
					} else {
						n = tsRegression.getN1();
					}
				} catch ( Exception e ) {
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

	  	    		// Get fill time series period
	  	    		FillStart = tsRegressionProps.getValue (
	  	    			"FillStart");
	  	    		FillEnd   = tsRegressionProps.getValue (
	  	    			"FillEnd");

				// REVISIT [LT 2005-04-30] LT Comment from SAM
				// TSRegression has an AnalysisMonth parameter
				// that you could use to fill one month. I (SAM)
				// may need to do some work in fillRegression()
				// but the parameter is in place.
				// DO I NEED TO DO ANYTHING DIFFERENT HERE?
				DateTime fillStart = DateTime.parse(FillStart);
				DateTime fillEnd = DateTime.parse(FillEnd);
	  	    		TSUtil.fillRegress(dependentTS,
	  	    				   independentTS,
	  	    				   fillStart,
	  	    				   fillEnd,
	  	    				   tsRegressionProps );

	  	    		// Done with this month (or all)
	  	    		break;
			}
	        }
	}
} catch ( Exception e ) {
	Message.printWarning( 1, mthd, e );
}
}

/**
Retuns a string containing the number of data point used in the analysis,
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
		} catch ( Exception e ) {
			retString += "    ???";
		}

		// Coefficient of correlation (r)
		try {
			double cc = tsRegression.getCorrelationCoefficient(
				month );
			retString += StringUtil.formatString (
				cc, __format8p2f );
		} catch ( Exception e ) {
			retString += "     ???";
		}

		// SEP (RMSE)
		try {
			double rmse = tsRegression.getRMSE( month );
			retString += StringUtil.formatString(
				rmse, __format12p2f );
		} catch ( Exception e ) {
			retString += "         ???";
		}

		// A
		try {
			double a = tsRegression.getA(month);
			retString += StringUtil.formatString(
				a, __format12p2f );
		} catch ( Exception e ) {
			retString += "         ???";
		}

		// B
		try {
			double b = tsRegression.getB(month);
			retString += StringUtil.formatString( b, __format12p2f );
		} catch ( Exception e ) {
			retString += "         ???";
		}
	} else {
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
			retString += StringUtil.formatString(
				b, __format12p2f );
		} else {
			retString += "         ...";
		}
	} catch ( Exception e ) {
		retString += "         ???";
	}

	retString += "\n";

	return retString;
}

/**
Retuns a string containing the number of data point used in the analysis,
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
		} catch ( Exception e ) {
			retString += "    ???";
		}

		// Correlation Coefficient
		try {
			double cc = tsRegression.getCorrelationCoefficient();
			retString += StringUtil.formatString( cc, __format8p2f );
		} catch ( Exception e ) {
			retString += "     ???";
		}

		// SEP (RMSE)
		try {
			double rmse = tsRegression.getRMSE();
			retString += StringUtil.formatString( rmse, __format12p2f );
		} catch ( Exception e ) {
			retString += "         ???";
		}

		// A
		try {
			double a = tsRegression.getA();
			retString += StringUtil.formatString( a, __format12p2f );
		} catch ( Exception e ) {
			retString += "         ???";
		}

		// B
		try {
			double b = tsRegression.getB();
			retString += StringUtil.formatString( b, __format12p2f );
		} catch ( Exception e ) {
			retString += "         ???";
		}

		// Sep total
		retString += " |";
		try {
			double b = tsRegression.getRMSE();
			retString += StringUtil.formatString(b, __format12p2f);
		} catch ( Exception e ) {
			retString += "         ???";
		}

	} else {
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
Initialize
*/
private void initialize()
throws Exception
{
	// Line separator to use in the reports.
	__nl = System.getProperty ("line.separator");

	// Dealling with properties not expected to be passed in from the
	// calling object.

	// AnalyzeForFilling - Set to "true" since analysis is being
	// done as part of a data filling process, in which case the
	// FillStart and FillEnd can be specified.
	String analyzeForFilling = __analysisProperties.getValue (
		"AnalyzeForFilling" );
	if ( analyzeForFilling == null ) {
		__analysisProperties.set(
			 "AnalyzeForFilling", "true" );
	}

	// Dealling with properties expected to be passed in from the calling
	// object.

	// Populate the __AnalysisMethod_Vector from the __analysisProperties.
	// This vector will be used to reset the AnalysisMethod property to
	// contain only one value, during the analysis, before passing the
	// __analysisProperties to the TSRegression.
	String analysisMethod = __analysisProperties.getValue (
		"AnalysisMethod" );
	if ( analysisMethod == null ) {
		// Set to default.
		analysisMethod = __ANALYSIS_OLS;
	}
	__AnalysisMethod_Vector = StringUtil.breakStringList(
		analysisMethod,	" ,", StringUtil.DELIM_SKIP_BLANKS);

	// Populate the __Transformation_Vector from the __analysisProperties.
	// This vector will be used to reset the Transformation property to
	// contain only one value, during the analysis, before passing the
	// __analysisProperties to the TSRegression.
	String transformation = __analysisProperties.getValue (
		"Transformation" );
	if ( transformation == null ) {
		// Set to default.
		transformation = __TRANSFORMATION_NONE;
	}
	__Transformation_Vector = StringUtil.breakStringList(
		transformation,	" ,", StringUtil.DELIM_SKIP_BLANKS);

	// Number of equations. If MonthlyEquations there will be a need to
	// execute on analysis per month.
	__NumberOfEquations = __analysisProperties.getValue (
		"NumberOfEquations" );
	if ( __NumberOfEquations == null ) {
		// Set to default.
		__NumberOfEquations = __NUM_OF_EQUATIONS_ONE_EQUATION;
		__analysisProperties.set(
			 "NumberOfEquations", __NumberOfEquations );
	}
	// Using boolean as flags for __NumberOfEquations to improve efficience
	__monthly = false;
	if ( __NumberOfEquations.equalsIgnoreCase(
		__NUM_OF_EQUATIONS_MONTHLY_EQUATIONS ) ) {
		__monthly = true;
	}

	// Best Fit Indicator ( not used in TSRegression ).
	__BestFitIndicator = __analysisProperties.getValue (
		"BestFitIndicator" );
	if ( __BestFitIndicator == null ) {
		// Set to default.
		__BestFitIndicator = __BEST_FIT_SEP;
	}
	// Remove from the __analysisProperties
	__analysisProperties.unSet ("BestFitIndicator");
	// Using int as flags for ___BestFitIndicator to improve efficience
	__intBestFitIndicator = __INT_BEST_FIT_SEP;
	if ( __BestFitIndicator.equalsIgnoreCase( __BEST_FIT_R ) ) {
		__intBestFitIndicator = __INT_BEST_FIT_R;
	} else if ( __BestFitIndicator.equalsIgnoreCase(__BEST_FIT_SEPTOTAL) ) {
		__intBestFitIndicator = __INT_BEST_FIT_SEPTOTAL;
	}

	// MinimumDataCount  ( not used in TSRegression ).
	__MinimumDataCount = __analysisProperties.getValue ("MinimumDataCount");
	if ( __MinimumDataCount == null || __MinimumDataCount.equals("") ) {
		__MinimumDataCount = "0";
	}
	// Remove from the __analysisProperties
	__analysisProperties.unSet ("MinimumDataCount");

	// MinimumR ( not used in TSRegression ).
	__MinimumR = __analysisProperties.getValue ("MinimumR");
	if ( __MinimumR == null || __MinimumR.equals("") ) {
		__MinimumR = "0.5";	// Default
	}
	// Remove from the __analysisProperties
	__analysisProperties.unSet ("MinimumR");

	// Intercept
	// REVISIT - This is confusing. Discuss with SAM. Comparing TSRegression
	// and MixedStations documentation.
	// DO NOT SET IF NULL.
	__Intercept = __analysisProperties.getValue ( "Intercept" );

	// NumberOfBestRegressions.
	__NumberOfBestRegressions = 5;	// Default
	String NumberOfBestRegressions = __analysisProperties.getValue (
		"NumberOfBestRegressions");
	if ( NumberOfBestRegressions != null ) {
		__NumberOfBestRegressions =
			StringUtil.atoi(NumberOfBestRegressions);
	}

	// OutputFile
	__OutputFile = __analysisProperties.getValue ( "OutputFile" );
	if ( __OutputFile == null ) {
		throw new Exception ( "Output file is required!" );
	}
}

/**
Rank the TSRegression in ascending order base either on the correlation
coefficient or the RMSE values.
*/
private void rank()
{
	String mthd = "MixedStationAnalysis.rank", mssg;

	List independentList    = null;
	TSRegression tsRegression = null;

	double[] values;

	int nDependent = __dependentStatisticsVector.size();
	__sortedOrder  = new int [nDependent][][];

try {
	// Loop the dependent objects.
	for ( int dep = 0; dep < nDependent; dep++ ) {

	    	// Get the independent list ( regressions ) for this independent
		independentList = (List)
			__dependentStatisticsVector.get( dep );

		int nIndependent = independentList.size();

		values = new double [nIndependent];

		if ( __monthly ) {

			// Months (12 equations)
			__sortedOrder[dep] = new int [12][nIndependent];

	  	    	for ( int month = 1; month <= 12; month++ ) {

	  	    		// Loop the independent objects, processing
	  	    		// correlations coef. or RMSE values.
	  	    		for ( int ind = 0; ind < nIndependent; ind++ ) {

					// Get the regression object.
		  	    		tsRegression = null;
			    		tsRegression = (TSRegression)
			       			independentList.get(ind);

		  	    		try {
		  	    		    switch ( __intBestFitIndicator ) {

		  	    		       case __INT_BEST_FIT_R:
		  	    			    values[ind]= tsRegression.
		  	    			       getCorrelationCoefficient
		  	    					( month );
		  	    			    break;
		  	    		       case __INT_BEST_FIT_SEP:
		  	    			    values[ind]= tsRegression.
		  	    			       getRMSE( month );
		  	    			    break;
		  	    		       case __INT_BEST_FIT_SEPTOTAL:
		  	    		           // REVISIT[LT 2005-04-30] For
		  	    		           // total we could do this
		  	    		           // just once. The sort will be
		  	    		           // the same for all months.
		  	    		           // Keeping as is for now!
		  	    			    values[ind]= tsRegression.
		  	    			       getRMSE();
		  	    			    break;
		  	    	    	       default:
		  	    	    	            values[ind]= tsRegression.
		  	    			       getRMSE( month );
		  	    			    break;
		  	    		    }
		  	    	    	} catch (Exception e ) {
		  	    	    		// If something went wrong with
		  	    	    		// the TSRegression, the getRMSE
		  	    	    		// and getCorrelationCoefficient
		  	    	    		// will throw and exception, but
		  	    	    		// we still need to keep the
		  	    	    		// independent in the list of
		  	    	    		// values to be able to relate
		  	    	    		// the sorted indexes to the
		  	    	    		// TSRegressions references in
		  	    	    		// the independentList vectors,
		  	    	    		// so set it to -999.99.
		  	    			values[ind]= __MISSING;
		  	    		}
		  		}
		  		try {
		  	    	   	MathUtil.sort ( values,
		  	    	    	    MathUtil.SORT_QUICK,
		  	    	    	    MathUtil.SORT_ASCENDING,
		  	    	    	    __sortedOrder[dep][month-1],
		  	    	    	    true );
		  	    	} catch ( Exception e ) {
		  	    		Message.printWarning (1,"",e);
		  	    		mssg = "Error sorting!";
		  	    		rankMessage (tsRegression,mssg);
		  	    	}
		  	}

		} else {

			// Year ( 1 equation )
			__sortedOrder[dep] = new int [1][nIndependent];

	  	    	// Loop the independent objects, processing
	  	    	// correlations coef. or RMSE values.
	  	    	for ( int ind = 0; ind < nIndependent; ind++ ) {

				// Get the regression object.
		  		tsRegression = null;
				tsRegression = (TSRegression)
					independentList.get(ind);

		  		try {
					switch ( __intBestFitIndicator ) {
		  	    		    case __INT_BEST_FIT_R:
		  	    		        values[ind]= tsRegression.
		  	    			    getCorrelationCoefficient();
		  	    			break;
		  	    		    case __INT_BEST_FIT_SEP:
		  	    		    	values[ind]= tsRegression.
		  	    			    getRMSE();
		  	    			break;
		  	    		    case __INT_BEST_FIT_SEPTOTAL:
		  	    		    values[ind]= tsRegression.
		  	    			    getRMSE();
		  	    			break;
		  	    	    	    default:
		  	    	    	        values[ind]= tsRegression.
		  	    			    getRMSE();
		  	    			break;
		  	    		}
		  	    	} catch (Exception e ) {
		  	    		// If something went wrong with
		  	    	    	// the TSRegression, the getRMSE
		  	    	    	// and getCorrelationCoefficient
		  	    	    	// will throw and exception, but
		  	    	    	// we still need to keep the
		  	    	    	// independent in the list of
		  	    	    	// values to be able to relate
		  	    	    	// the sorted indexes to the
		  	    	    	// TSRegressions references in
		  	    	    	// the independentList vectors,
		  	    	    	// so set it to -999.99.
		  	    		values[ind]= __MISSING;
		  	    	}
		  	}
		  	try {
		  	   	MathUtil.sort ( values,
		  	    		MathUtil.SORT_QUICK,
		  	    		MathUtil.SORT_ASCENDING,
		  	    		__sortedOrder[dep][0],
		  	    		true );
		  	} catch ( Exception e ) {
		  		Message.printWarning (1,"",e);
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
class using the construct overload and passin a valid not null reference to
itself (this).
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

	PropList tsRegressionProps = tsRegression.getPropList();

	// Transformation
	String Transformation = tsRegressionProps.getValue ( "Transformation" );
	// Add Transformation to the correlation table
	mssg = mssg + Transformation + ", ";

	// AnalysisMethod
	String AnalysisMethod = tsRegressionProps.getValue ( "AnalysisMethod" );
	mssg = mssg + Transformation + ") - " + text + "\n";
}

/**
Save all parts of the report created by different stages of the analysis.
*/
private void saveReport()
{
	String mthd = "MixedStationAnalysis.saveReport", mssg;

	String full_fname = IOUtil.getPathUsingWorkingDir (__OutputFile);

	try {

		FileOutputStream fos = new FileOutputStream ( full_fname );
		PrintWriter pw = new PrintWriter (fos);

		IOUtil.printCreatorHeader ( pw, "#", 80, 0);

		// Report header section
		if ( __sHeader != null ) {
			pw.print (__sHeader.toString());
		} else {
			pw.print("\n\nHeader is still empty!\n\n");
		}

		// Summary section
		if ( __sSummary != null ) {
			pw.print (__sSummary.toString());
		} else {
			pw.print("\n\nSummary section is still empty!\n\n");
		}

		// Statistics section
		if ( __sStatistics != null ) {
			pw.print (__sStatistics.toString());
		} else {
			pw.print("\n\nSummary section is still empty!\n\n");
		}

		// Alternative to save the report.
		pw.flush();
		pw.close();
	} catch (Exception e ) {
		mssg = "Error saving the output file '" + __OutputFile + "'";
		Message.printWarning ( 1, mthd, mssg );
	}
}

// REVISIT [2005-04-20] This will not work if this class is moved to the TS
// library, because it should not be allowed to import the class
// fillMixedStation_JDialog, which resides in the TSTool application.
// The solution may be to implement a listener with interface as it is done
// in C++.
/**
Update the status bar of the calling object, if that object instantiate this
class using the construct overload and passin a valid not null reference to
itself (this).
*/
private void setParentStatusText( String text )
{
	if (__parent != null ) __parent.setStatusText (text);
}

} // end MixedStationAnalysis
