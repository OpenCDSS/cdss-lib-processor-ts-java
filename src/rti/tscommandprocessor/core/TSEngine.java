//------------------------------------------------------------------------------
// TSEngine - class to process time series
//------------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
//------------------------------------------------------------------------------
// Notes:	(1)	This class processes time series in bulk fashion and
//			understands tstool command-line options.
//------------------------------------------------------------------------------
// History:
//
// 07 Apr 1998	Steven A. Malers, RTi	Created class to handle bulk processing
//					of time series.
// 30 Jul 1998	SAM, RTi		Pass the requested time series to the
//					getTimeSeries calls.  The low-level
//					code will interpret correctly if the
//					values are zero.
// 31 Aug 1998	SAM, RTi		Finish the enhancements to frost dates
//					and time series filling.
//					For regression, base the regression on
//					the full period but only fill the
//					requested period.
// 22 Sep 1998	SAM, RTi		Add _missing for use with StateMod.
// 28 Sep 1998	SAM, RTi		Allow blank line in commands file.
//					Make so that if time series returned for
//					fillconst() is missing to allocate an
//					empty time series.
// 03 Dec 1998	SAM, RTi		Add the global _fatal_error_count to
//					help track errors.  Wrap each parse
//					option with exception handling.
// 02 Jan 1999	SAM, RTi		Finish up 3.07 enhancements, including
//					extending -fillhistave to monthly data
//					and adding the -averageperiod option.
// 07 Jan 1999	SAM, RTi		Make so -fillhistave works the same
//					for frost dates and other data.  Enable
//					the add() function for frost dates.
// 27 Jan 1999	SAM, RTi		Update to 3.09.  React to feedback from
//					users.
// 23 Feb 1999	SAM, CEN, RTi		Add features to support RGDSS.  Change
//					imports to only import needed packages.
// 06 Apr 1999	CEN, RTi		Added StateModDayTS print call and
//					changed StateMod*TS import to proper 
//					location.
// 17 May 1999	SAM, RTi		Test with Java 1.1.8.  Add genesis
//					output to StateMod output.  Update to
//					pass PropList for StateMod output and
//					include more information in the header.
//					Add the -detailedheader option.
// 09 Jun 1999	SAM, RTi		Add -ofilterdatacoverage option.
//					Allow reports to be exported without
//					preview.  Start adding support for file
//					formats other than StateMod.
// 27 Jul 1999	SAM, RTi		Add day_to_month_reservoir data filling.
// 28 Oct 1999	SAM, RTi		Add ignore <= zero feature for
//					averages.
// 01 Dec 1999	SAM, CEN, RTi		Add ability to fill diversions using
//					diversion comments.
// 27 Jan 2000	SAM, RTi		Add -missing Value1,Value2 to set the
//					range of missing values.  This will
//					allow data from the NWS to be properly
//					processed (-998 and -999 are typically
//					used for missing data).  Minor change
//					to free _tslist when done and tslist
//					before adding. 
// 19 Jul 2000	SAM, RTi		When writing a large number of StateMod
//					files, use a binary file to process.
//					Add -slist ability and binary command
//					line options.  Add ability to write
//					commands file.  Print list of time
//					series not found to the log file.
// 25 Sep 2000	SAM, RTi		Add -include_missing_ts functionality.
// 04 Oct 2000	SAM, RTi		Enable DateValue format.
// 11 Oct 2000	SAM, RTi		Add -oannual_traces_graph option.
//					Add -oyear_to_date_report.
// 25 Oct 2000	SAM, RTi		Enable -oscatter_plot.
// 30 Oct 2000	SAM, RTi		Enable -oduration_graph, -obar_graph.
// 01 Nov 2000	SAM, RTi		Add runInterpreter() to allow multi-step
//					processing.  Track Vector of dates to
//					simplify commands.
// 30 Nov 2000	SAM, RTi		Change -oscatter_plot to
//					-oxyscatter_plot.
// 10 Dec 2000	SAM, RTi		Change NWSHourTS to NWSCardTS.  Add
//					-onwscard to write NWS Card file.
//					Add processTimeSeries() to generate
//					output.  Add finalize() method.
//					Add support for new syntax for
//					fillRegression(), etc.  In most cases
//					treat old and new functionality
//					separately so that code can be migrated
//					more easily without mixing approaches.
// 31 Dec 2000	SAM, RTi		Remove old running average code.  A time
//					series can now be converted to a running
//					average and then output in any format.
// 02 Jan 2001	SAM, RTi		Begin reworking so that old options are
//					specifically noted as such to provide
//					backward compatibility while allowing
//					new code.  In particular, historic
//					averages are now computed as time series
//					are read and are saved with the time
//					series.  Therefore the processCommands()
// 07 Jan 2001	SAM, RTi		Change IO to IOUtil, GUI to GUIUtil.
// 15 Jan 2001	SAM, RTi		Add backward-compatibility features so
//					that time series not found in memory are
//					temporarily queried, used, and
//					discarded.  Enable this for old commands
//					so that old command files can be run as
//					they were before.  Add TEMPTS feature
//					as needed to support
//					backward-compatibility and streamline
//					processing.  Convert preference commands
//					to full expressions to allow toggle.
// 20 Feb 2001	SAM, RTi		Finalize SQL/Daily feature release.
//					Add:
//						TS X = normalize()
//						TS X = newTimeSerie()
//					Change _missing to -999.  This thing
//					has outlived its time.
// 15 Mar 2001	SAM, RTi		Update to allow warning messages in
//					the main processing loop to be
//					temporarily turned off to minimize user
//					mouse clicks.  Replace _isBatch with
//					IOUtil.isBatch().
// 09 May 2001	SAM, RTi		Add multiply(), divide().
// 23 Aug 2001	SAM, RTi		Add relativeDiff(), replaceValue(),
//					runProgram().  When parsing
//					setOutputPeriod(), do not use the space
//					as a delimiter since it can be used for
//					periods with times.  Trim the resulting
//					tokens.
// 30 Aug 2001	SAM, RTi		Fix bug where graphs were reverting to
//					last output time series list contents.
//					Verify that copy() works.  Fix problems
//					in TS library with clone().  Allow
//					missing time series to be automatically
//					inserted with missing data, even in the
//					GUI (this is used especially by the
//					createFromList() command).  Add
//					blend(), cumulate().  Add newGraph() and
//					graph property set commands.  To do so
//					need to wire together some
//					WindowListener code so that graphs can
//					be managed within a TSEngine.
//					Fix fillFrostDatesTSWithHistAve to
//					always fill with the historic average.
//					Previously, it would only do so if the
//					old -fillhistave option was used.
//					Add writeSummary() to support frost date
//					output.  For bar graphs, allow bars to
//					be drawn to the left or right of the
//					date, or centered on the date.
// 2001-11-01	SAM, RTi		Remove references to the old Visualize
//					graphing package.  It is now completely
//					phased out.
// 2001-11-20	SAM, RTi		Change a couple of String.indexOf()
//					calls to StringUtil.indexOfIgnoreCase()
//					to be more flexible.  Fix a bug where
//					the user-specified precision for
//					writeStateMod() was not being
//					recognized.
// 2002-01-14	SAM, RTi		Enable NWSRFS data source.  Use new
//					GRTS classes that will support Swing.
// 2002-01-31	SAM, RTi		Update to handle TS identifiers where
//					input sources can be specified after the
//					TSID:  TSID~input_type~input_name.
// 2002-02-09	SAM, RTi		Modify getTSIdentifiersFromExpressions()
//					to return only the part between the
//					input type and name, so that time series
//					commands only work on the identifiers.
//					Change so createTraces() commands sets
//					the scenario to include the sequence
//					number.
// 2002-02-25	SAM, RTi		Add fillMOVE2().
// 2002-03-22	SAM, RTi		Use analysis period for fillMOVE*()
//					commands.  Change some regression code
//					and parameters to be more generic (e.g.,
//					change RegressMonthly property to
//					AnalyzeMonthly).  Change some
//					"expression" methods to "command"
//					methods.  Overload processCommands() to
//					set properties for use in the TSTool
//					GUI.  Remove the determineQueryPeriod()
//					method since the query, output,
//					and analysis periods are now explicitly
//					defined.  Sort data declarations
//					alphabetically (had become
//					disorganized).  Change so flags are not
//					declared as static since they are only
//					used internally.  Remove code that
//					controlled computation of historic
//					averages - just do it always.
//					setIncludeMissingTS() was not being
//					handled by the command parser.
//					Add support for:
//					fillFromTS(),
//					readUsgsNwis(),
//					setAutoExtendPeriod(),
//					setBinaryTSPeriod(),
//					setFromTS(),
//					setQueryPeriod().
// 2002-04-15	SAM, RTi		Add support for shiftTimeByInterval().
//					Update scale() to accept analysis
//					period.
// 2002-04-17	SAM, RTi		Add support for ARMA().  Add
//					setRegressionPeriod() to help with
//					backward compatibility.
// 2002-04-18	SAM, RTi		Add disaggregate().
// 2002-04-19	SAM, RTi		Fix ARMA() to handle ARMA interval
//					(TSTool 05.05.06).
// 2002-04-22	SAM, RTi		Update to 05.05.07 - final cosmetic
//					changes on existing dialogs for public
//					release.
// 2002-04-23	SAM, RTi		Figure out why free() does not seem to
//					be working.  Had a problem in that the
//					do_XXX() methods were not properly
//					handling the resetting of time series.
//					Update to version 05.05.08.
// 2002-04-23	SAM, RTi		Update to version 05.05.09 - more
//					cosmetic changes.  Make replaceValue()
//					more robust. Add TS x = readDateValue().
//					Add processTSProduct() and consequently
//					implement TSSupplier.
// 2002-04-26	SAM, RTi		Update to version 05.05.10.  Fix bug
//					where readTimeSeries() was not properly
//					handling spaces in file names.
// 2002-04-26	SAM, RTi		Update to version 05.05.11.  Fix some
//					problems with dates in editor dialogs.
// 2002-04-26	SAM, RTi		Update to version 05.05.12.  Expand ARMA
//					features.  Support left and right legend
//					position in graphs.
// 2002-05-08	SAM, RTi		Update to version 05.05.14.  Fix
//					disaggregate(Ormsbee) to set result to
//					zero if the input is all zeros.
// 2002-05-09	SAM, RTi		Fix newTimeSeries() to allow * dates.
// 2002-05-12	SAM, RTi		Update to version 05.06.00.  Add
//					readNwsCard() and writeNwsCard()
//					commands.  Move the log file creation to
//					the tstool.java file (main program).
// 2002-05-26	SAM, RTi		Enable cancel in the warning dialog in
//					the main processing loop - use the new
//					MessageDialogListener feature.  Add
//					adjustExtremes().  Add
//					addTSViewWindowListener() to allow
//					TSTool to listing to TSViewFrame
//					closing.  Add support for RiverWare data
//					type with TS X = readRiverWare() and
//					writeRiverWare().
// 2002-06-05	SAM, RTi		Add monthly mean and summary reports.
// 2002-06-06	SAM, RTi		Add -oriverware support to allow GUI to
//					use File...Save as RiverWare.
// 2002-06-13	SAM, RTi		Add support for MODSIM.
// 2002-06-25	SAM, RTi		Add support to read RiversideDB time
//					series.
// 2002-07-25	SAM, RTi		Fix a bug in writeRiverWare().
// 2002-08-23	SAM, RTi		Update indexOf() to not care if the
//					alias is "" or not when checking the
//					identifiers.  This is because there are
//					cases where the alias is specified in
//					addition to the identifier but the ID
//					is used in commands.  Add addConstant()
//					command.
// 2002-08-27	SAM, RTi		Add TS X = newEndOfMonthTSFromDayTS() to
//					replace the day_to_month_reservoir()
//					command.  The former operates on a TSID
//					whereas the latter actually reads the
//					time series.
// 2002-10-11	SAM, RTi		Update ProcessManager to ProcessManager1
//					to allow transition to Java 1.4.x.
// 2002-10-17	SAM, RTi		Change back to ProcessManager since the
//					update version seems to work well for
//					java 1.1.8 and 1.4.0.
// 2002-11-25	SAM, RTi		Add support for Mexico CSMN time series.
// 2003-03-13	SAM, RTi		* Add support for StateModX time series.
//					* Fix bug in readUsgsNwis() command
//					  where spaces in file names were
//					  causing an error.
//					* Add convertDataUnits() command.
// 2003-04-04	SAM, RTi		* Add support for DIADvisor database.
// 2003-04-18	SAM, RTi		* Add support for RiverWare in the
//					  TSSupplier.
//					* Begin adding more do_XXX() methods -
//					  it appears that the
//					  processTimeSeriesCommands() method may
//					  be having some type of memory bound
//					  issues because of its size.
//					* Remove support for graph() command.
// 2003-05-14	SAM, RTi		* Allow fillRegression() to have an
//					  Intercept property.
// 2003-06-12	SAM, RTi		Update to version 06.00.00.
//					* Start using Swing components.
//					* Use new TS package (DateTime instead
//					  of TSDate, etc.).
//					* Use new HydroBaseDMI instead of HBDMI.
//					* Use new StateMod_TS.
//					* Add support for templates in time
//					  series products.
// 2003-07-29	SAM, RTi		Add ESPTraceEnsemble support.
// 2003-11-02	SAM, RTi		* Add SHEF A support.
// 2003-12-01	SAM, RTi		* Change to calendar year as the
//					  default year type.
//					* Default createFromList() to use
//					  a HydroBase input type.
// 2004-01-15	SAM, RTi		* Enabled writing StateMod files!
// 2004-01-29	SAM, RTi		* Update add() to take * for the list
//					  of time series identifiers.
//					* Remove support for old-style() add -
//					  the code is very old!
// 2004-01-31	SAM, RTi		Version 06.00.08 Beta.
//					* Enabled StateMod daily format write
//					  and optimized performance of that
//					  code some.
//					* Add a StopWatch to record how fast
//					  the commands are processed.
//					* Enable openHydroBase() command and
//					  deprecate setDatabaseEngine(),
//					  setDatabaseHost(), and
//					  setDataSource().
// 2004-02-05	SAM, RTi		Version 06.00.09 Beta.
//					* Enable reading StateCU input type time
//					  series.
//					* Add writeStateCU() command to write
//					  frost dates.
//					* Enable reading StateCU frost dates
//					  time series.
//					* Enable fillHistYearAverage() on any
//					  yearly time series.
//					* Enable statemodMax().
//					* Enable setIgnoreLEZero().
//					* Phase out setUseDiversionComments()
//					  and add fillUsingDiversionComments()
//					  for HydroBase where filling occurs
//					  when the command is executed.
// 2004-02-21	SAM, RTi		Version 06.01.00.
//					* Add fillRepeat().
//					* Unrecognized command was not resulting
//					  in a warning - fix.
//					* Add fillProrate().
//					* Add isTSID() to help identify pure
//					  time series identifiers.
//					* Throw an Exception of the period
//					  cannot be changed after a read - this
//					  was letting some errors through.
// 2004-03-15	SAM, RTi		* Add createYearStatisticsReport()
//					  command for testing bulk analysis of
//					  data.
//					* Enable the deselectTimeSeries() and
//					  selectTimeSeries() commands.
//					* Update writeDateValue() to use
//					  parameters.
//					* Update readStateModB() to take a
//					  free-format parameter list.
// 2004-03-22	SAM, RTi		* Make normal TS read and TSProcessor
//					  use the same readTimeSeries0() method
//					  to actually read time series - that
//					  way there is full support for
//					  TSProducts.
// 2004-03-28	SAM, RTi		* Update createYearStatisticsReport() to
//					  put in a second column with the
//					  time series description, to simplify
//					  reading the results and because the
//					  GeoView summary layer currently
//					  defaults to ID,Name,Values...
// 2004-04-13	SAM, RTi		* Change ESPTraceEnsemble to
//					  NWSRFS_ESPTraceEnsemble.
//					* Remove commented code - TSSupplier and
//					  normal code seem to be able to share
//					  code OK.
// 2004-05-11	Scott 	  		Changed the package to get
//		Townsend, RTi		NWSRFS_ESPTraceEnsemble from NWSRFS to
//					NWSRFS_DMI.
// 2004-05-21	SAM, RTi		* Change the ESPTraceEnsemble to use
//					  input type "NWSRFS_ESPTraceEnsemble".
//					* Enable support for HydroBase WIS time
//					  series.
//					* Fix problem where limits summary
//					  report is not printing properly
//					  because the newlines in the output
//					  are not handled.
//					* Change readESP... to readNWSRFSESP...,
//					  and similar for the write methods.
// 2004-05-28	SAM, RTi		* Expand selectTimeSeries() and
//					  deselectTimeSeries() to take time
//					  series position.
//					* Add TSList parameter to
//					  writeNWSRFSESPTraceEnsemble().
// 2004-07-11	SAM, RTi		* Update deselectTimeSeries() and
//					  selectTimeSeries() to have
//					  "(de)selectAllFirst" parameters to
//					  clear selections.
//					* Update free() to use parameter lists.
//					* Add readStateCU() that reads one or
//					  more time series with wildcards for
//					  the TSID.
// 2004-07-20	SAM, RTi		* For summary time series, if the input
//					  type is HydroBase, print the header
//					  from the comments.  Else, print the
//					  default header.  The __non_co_detected
//					  data member and is removed.
//					* Update the createFromList() command
//					  to use new free-format notation and
//					  use a standard table to read the
//					  file.  Strip the binary time series
//					  capability from the command since
//					  computers typically have enough
//					  memory now.
//					* Remove _graph_list - not used.
// 2004-07-29	SAM, RTi		* Fix bug where createFromList() with
//					  no input name was throwing a null
//					  pointer exception.
//					* Fix bug where writeDateValue was not
//					  recognizing OutputStart, OutputEnd
//					  parameters.
// 2004-08-12	SAM, RTi		* Update setConstant() to free-format
//					  parameters and allow monthly constant
//					  values and set period to be specified.
// 2004-08-25	SAM, RTi		* Add TS X = readHydroBase().
// 2004-09-07	SAM, RTi		* Add NWSRFS_DMI to constructor and
//					  add read commands for NWSRFS FS5Files.
// 2004-11-29	SAM, RTi		* Update to allow individual ESP
//					  traces to be read with an ID.
// 2005-02-17	SAM, RTi		Update to version 06.10.00.
//					* Reenable changeInterval() with generic
//					  functionality.
// 2005-03-14	SAM, RTi		* Add OutputFillMethod and
//					  HandleMissingInputHow parameters to
//					  changeInterval.
// 2005-04-05	SAM, RTi		* Start using the new message viewer
//					  with message tags.
//					* Start phasing in some StateDMI
//					  concepts like prepended commands and
//					  application status bar messages.
//					* Add getTimeSeriesList() to return the
//					  full list of time series.
//					* Deprecate getSelectedTimeSeries() in
//					  favor of getSelectedTimeSeriesList().
//					* Update openHydroBase() to include
//					  UseStoredProcedures and InputName
//					  parameters.
//					* Replace __hbdmi with __hbdmi_Vector to
//					  allow multiple instances of
//					  HydroBaseDMI to be open at the same
//					  time.  Test with the openHydroBase()
//					  method.
//					* When reading from HydroBase, request
//					  the HydroBaseDMI that matches the
//					  requested InputName.
//					* Fix typo in openHydroBase() where
//					  "BatchAndGUI" was used instead of
//					  "GUIAndBatch".
//					* Overload
//					  getTSIdentifiersFromCommands() to
//					  process the entire identifier.  All
//					  commands will likely transition to
//					  using full identifiers or aliases.
//					* Update fillPattern() to use new
//					  free-format parameters.
//					* Add point graph type -opointgraph.
//					* Implement CommandProcessor and start
//					  to prototype a more generalized
//					  handling of time series.
//					* Totally remove setRegressionPeriod(),
//					  other than obsolete warning.
//					* Add -oPredictedValue_graph and
//					  PredictedValueResidual_graph.
// 2005-05-17	SAM, RTi		* Phase out fillHistMonthAverage() code
//					  in favor of the separate command
//					  class.
//					* Change getTimeSeriesToProcess() from
//					  private to protected sine it is now
//					  being called from commands.
//					* Remove the boolean to track warnings
//					  about filling non-month data with
//					  monthly averages.
// 2005-05-18	SAM, RTi		* Phase out fillHistYearAverage() code
//					  in favor of the separate command
//					  class.
// 2005-05-19	SAM, RTi		* Migrate new Command-based code to
//					  RTi.Util.IO and RTi.TS to allow more
//					  general design and sharing with other
//					  applications like StateDMI.
// 2005-05-20	SAM, RTi		* Change getTSIdentifiersFromCommands()
//					  to support block-style comments.
//					* Add warning in add() when FrostDate
//					  time series are being promised, to
//					  help CDSS users transition.
//					* Add command_tag to a number of private
//					  methods to facilitate using the new
//					  log viewer.
//					* Convert TS X = changeInterval() to
//					  new commands.
// 2005-05-26	SAM, RTi		* Phase in new message command tags, to
//					  allow use of the new message log
//					  viewer.
// 2005-05-31	SAM, RTi		* Convert writeRiverWare() to new
//					  command design.
//					* Change getDateTime() to return null if
//					  a null string is passed.
// 2004-06-08	SAM, RTi		Update to version 06.10.02.
//					* Convert openHydroBase() to command and
//					  move to HydroBaseDMI package.
//					* Update readStateCU() to read the CDS
//					  and IPY files.
// 2005-07-05	SAM, RTi		* When creating TSViewJFrame, add the
//					  HydroBaseDMI and RiversideDB_DMI as
//					  interfaces that can save products.
// 2005-07-17	SAM, RTi		* Update selectTimeSeries() to allow the
//					  pattern and position to be specified,
//					  to allow more power for selects.
//					* Update fillProrate() to compute the
//					  factor based on an average of the
//					  independent time series.
// 2005-08-01	SAM, RTi		* Update the fillProrate() InitialValue
//					  parameter to include NearestBackward
//					  and NearestForward.  The default is
//					  now not to automatically look for an
//					  initial value.
// 2005-08-24	SAM, RTi		Update to version 06.10.07.
//					* Convert scale() to command class.
//					* Change TS X = ... to TS Alias = ...
//					  in comments etc.
//					* Convert TS Alias = copy() to command
//					  class.
// 2005-08-30	SAM, RTi		* Convert writeStateMod() to command
//					  class and enhance parameters to
//					  include the output period, missing
//					  data value, and the ability to select
//					  the time series to output.
//					* In getDateTime() return null if a
//					  named DateTime is requested and the
//					  matching object value is null.
//					* In getDateTime() add support for
//					  InputStart and InputEnd, equivalent to
//					  QueryStart and QueryEnd.
//					* Convert readStateMod() to a command
//					  class and enhance parameters to
//					  include the input period.
//					* Add public readTimeSeries2(Vector) to
//					  facilitate use by command classes.
// 2005-09-07	SAM, RTi		Update to version 06.10.08.
//					* Convert fillConstant() to a command
//					  class.
// 2005-09-20	SAM, RTi		* Convert newTimeSeries() to a command
//					  class.
//					* Add the newStatisticYearTS() command.
//					* Update openHydroBase() to take a
//					  database name.
// 2005-09-28	SAM, RTi		Update to version 06.10.08.
//					* Update cumulate() to command.
//					* Add more statistics to
//					  newStatisticYearTS().
//					* Convert readStateModB() to use the
//					  command class.
// 2005-10-18	SAM, RTi		Update to version 06.11.00.
//					* Fix so that processTSProduct() in
//					  batch mode can still preview.
//					* Add addTSViewTSProductAnnotation
//					  Providers().
//					* Add support for the ColoradoSMS
//					  database.  This is only used for
//					  annotating HydroBase data, so there is
//					  currently no need for most of the
//					  time-series logic for this input type.
//					* Convert processTSProduct() to a
//					  command class.
// 2005-11-13	SAM, RTi		* Disable ESP Trace Ensemble code if not
//					  in the jar file.
// ...					Update to version 06.14.00.
// 2005-12-06	J. Thomas Sapienza, RTi	* Moved readNwsCard() command out to
//					  a separate command class.
//					* Added code to print Exceptions thrown
//					  when running commands to the log file
//					  at Debug level 3, if being run with
//					  Debug on.  This is to speed up 
//					  debugging and development.
// 2005-12-14	SAM, RTi		* Convert setQueryPeriod() to the
//					  command class setInputPeriod().
// 2006-01-18	JTS, RTi		NWSCardTS is now in RTi.DMI.NWSRFS_DMI.
//					Note:  writeNwsCard() and read*ESP*()
//					have not been converted to a command
//					class yet.
// 2006-01-31	SAM, RTi		Update to version 06.16.00.
//					* Add NDFD command support.
// 2006-03-27	SAM, RTi		* For newEndOfMonthTSFromDayTS(), save
//					  the original data limits after
//					  creation.
// 2006-04-13	SAM, RTi		* Reenable MOVE2, which was accidentally
//					  commented out - use command classes.
// 2006-04-20	SAM, RTi		Update to version 06.17.00.
//					* Convert readHydroBase() commands to
//					  use a command class.
// 2006-05-18	SAM, RTi		Update to version 06.19.00.
//					* Fix bug where disaggregate was calling
//					  getTimeSeries() with the wrong
//					  parameters.
// 2006-01-19	SAM, RTi		* Change fillUsingDiversionComments() to
//					  automatically extend the data period
//					  if no output period has been
//					  specified.
//					* Change the output period data members
//					  from __output_date1/__output_date2 to
//					  __OutputStart_DateTime/
//					  __OutputEnd_DateTime to be more
//					  consistent with other code.
// 2006-07-13	SAM, RTi		* Manage NDFD Adapters.
// 2006-10-30   Kurt Tometich, RTi  * Commented out variables and packages
//                    pertaining to the legacy dataServices code
// 2006-11-02   KAT, RTi        * Fixed the newDayTSFromMonthAndDayTS
//                    to multiply the monthly volume by (1 / 1.9835).
//                    The old code was multiplying by 1.9385 and should 
//                    have been the reciprocal.  Algorithm is tested with
//                    with regression test commands.
// 2006-11-02   KAT, RTi    * Fixed bug where TS wasn't being ignored when 
//                    daily or monthly data was missing.  Fixed in the
//                    setUsingMonthAndDay method.  Tested by regression
//                    test commands under test/regression/commands.
// 2007-01-11   KAT, RTi    * Fixed a bug in do_readStateCU() where the
//                    file from the command was not taking into account
//                    the current working directory like all other commands.
// 2007-01-16   KAT, RTi    * Fixed a bug in do_writeStateCU() where the
//                    file from the command was not taking into account
//                    the current working directory like all other commands.
// 2007-01-25 	KAT, RTi	Deleted several import statements that were not
//							needed.
// 2007-01-26	KAT, RTi	Moved the do_fillUsingDiversionComments() method
//							to the HydroBase package.  Also deleted the old way
//							of handling this command in processCommands() by
//							allowing it to hit the generic code at the end and 
//							call the TSCommandFactory.  The command was copied
//							to HydroBase similar to the openHydroBase() command.
// 2007-02-08	SAM, RTi	Rename to TSCommandProcessor package.
//					Clean up code based on Eclipse feedback.
//					Change so TSEngine does not implement CommandProcessor -
//					force all interaction to go through the TSCommandProcessor,
//					which has an instance of TSEngine.
//					Change getPropContents() to protected since only
//					TSCommandProcessor should call.
//					Internally change QueryStart/End to InputStart/End -
//					need to transition code at command level as edits occur.
//					Remove setProp() and setPropContents() since these are
//					now handled by the TSCommandProcessor.
// 2007-03-01	SAM, RTi	Fix fillInterpolate() to handle TSID with spaces.
//
// EndHeader

package rti.tscommandprocessor.core;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.lang.String;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

// Code that will be used when service code is fully implemented...
//import rti.common.transfer.TSIdentityTO;
//import rti.common.transfer.TSRequestTO;
//import rti.common.type.PersistenceType;
//import rti.domain.timeseries.TimeSeries;
//import rti.domain.timeseries.TimeSeriesFabricator;
//import rti.transfer.TimeSeriesIdBean;
//import rti.type.TimeSeriesInputType;

import DWR.DMI.HydroBaseDMI.HydroBaseDMI;

import DWR.DMI.SatMonSysDMI.SatMonSysDMI;

import DWR.StateCU.StateCU_CropPatternTS;
import DWR.StateCU.StateCU_IrrigationPracticeTS;
import DWR.StateCU.StateCU_TS;

import DWR.StateMod.StateMod_TS;
import DWR.StateMod.StateMod_BTS;

//import RTi.DataServices.Adapter.NDFD.Adapter;
import RTi.DMI.DIADvisorDMI.DIADvisorDMI;
import RTi.DMI.NWSRFS_DMI.NWSCardTS;
import RTi.DMI.NWSRFS_DMI.NWSRFS_ESPTraceEnsemble;
import RTi.DMI.NWSRFS_DMI.NWSRFS_DMI;

import RTi.DMI.RiversideDB_DMI.RiversideDB_DMI;

import RTi.GRTS.TSProductAnnotationProvider;
import RTi.GRTS.TSProductDMI;
import RTi.GRTS.TSViewJFrame;

import RTi.TS.BinaryTS;
import RTi.TS.DateValueTS;
import RTi.TS.DayTS;
import RTi.TS.MexicoCsmnTS;
import RTi.TS.ModsimTS;
import RTi.TS.MonthTS;
import RTi.TS.MonthTSLimits;
import RTi.TS.RiverWareTS;
import RTi.TS.ShefATS;
import RTi.TS.StringMonthTS;
import RTi.TS.TS;
import RTi.TS.TSAnalyst;
import RTi.TS.TSIdent;
import RTi.TS.TSLimits;
import RTi.TS.TSSupplier;
import RTi.TS.TSUtil;
import RTi.TS.UsgsNwisTS;
import RTi.TS.YearTS;

import RTi.Util.GUI.ReportJFrame;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusProvider;
import RTi.Util.IO.CommandStatusProviderUtil;
import RTi.Util.IO.CommandStatusUtil;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.DataFormat;
import RTi.Util.IO.DataUnits;
import RTi.Util.IO.GenericCommand;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.ProcessManager;
import RTi.Util.IO.PropList;
import RTi.Util.IO.UnknownCommandException;
import RTi.Util.Math.MathUtil;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageJDialog;
import RTi.Util.Message.MessageJDialogListener;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableRecord;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.StopWatch;
import RTi.Util.Time.TimeInterval;
import RTi.Util.Time.TimeUtil;

public class TSEngine implements MessageJDialogListener,
TSSupplier, WindowListener
{
	
/**
"ts_action" values for processTimeSeriesAction, indicating how to process
a time series from a command.  These will eventually go away when all processing
is handled in the commands.
*/
public final static int
			INSERT_TS = 1,	
			UPDATE_TS = 2,
			EXIT = 3,
			NONE = 4;

/**
Indicate that temporary time series should be retrieved within a command but not
managed in the processor.  This may go away due to limited use and issues with
design.
*/
public final String TEMPTS = "TEMPTS";
public final String TEMPTS_SP="TEMPTS ";

/**
Used to specify which time series are to be processed with the TSList command
parameter.
*/
public final String __AllTS = "AllTS";
public final String __AllMatchingTSID = "AllMatchingTSID";
public final String __LastMatchingTSID = "LastMatchingTSID";
public final String __SelectedTS = "SelectedTS";
						

public final int BINARY_MONTH_CUTOFF = 100000;	// Number of time series to
						// trigger use of a binary file
						// to process monthly files.
						// LARGE SINCE NOT IMPLEMENTED.
public final int BINARY_DAY_CUTOFF = 10000;	// Number of time series to
						// trigger use of a binary file
						// to process dialy files.
						// TEMPORARILY MAKE LARGE UNTIL
						// THERE IS TIME TO REVISIT THE
						// USE OF THE BINARY FILE WITH
						// TSTool 05.0x FEATURES.

public final int OUTPUT_NONE = 0;		// Initial value for
						// _output_format.
public final int OUTPUT_STATEMOD = 1;		// Formats for outputting the
						// time series.
public final int OUTPUT_SUMMARY = 2;		// Time series summary
public final int OUTPUT_LINEGRAPH = 3;		// Line graph
public final int OUTPUT_LINELOGYGRAPH = 4;	// Could find a way to modify
						// the graph but do like this
						// for now.
public final int OUTPUT_PORGRAPH = 5;		// Period of record graph.
public final int OUTPUT_SUMMARY_NO_STATS = 8;	// Special output for Ayres
						// software.  Just remove the
						// statistics lines.
public final int OUTPUT_PERCENT_EXCEED_GRAPH =9;// Percent exceedance curve.
public final int OUTPUT_DOUBLE_MASS_GRAPH = 10; // Double mass graph.
public final int OUTPUT_DATEVALUE = 11;		// Output in DateValue format.
public final int OUTPUT_ANNUAL_TRACES_GRAPH =12;// Annual traces graph.
public final int OUTPUT_YEAR_TO_DATE_REPORT =13;// Year to date totals.
public final int OUTPUT_XY_SCATTER_GRAPH = 14;	// Scatter plot
public final int OUTPUT_DURATION_GRAPH = 15;	// Duration graph 
public final int OUTPUT_BAR_GRAPH = 16;		// Bar graph (parallel to each
						// other)
public final int OUTPUT_NWSCARD_FILE = 17;	// Output in NWS Card format.
public final int OUTPUT_DATA_LIMITS_REPORT = 18;// Data limits report
public final int OUTPUT_DATA_COVERAGE_REPORT=19;// Data limits report
public final int OUTPUT_MONTH_MEAN_SUMMARY_REPORT=20;
public final int OUTPUT_MONTH_TOTAL_SUMMARY_REPORT=21;
						// Monthly summary reports.
public final int OUTPUT_RIVERWARE_FILE = 22;	// Output in RiverWare format.
public final int OUTPUT_SHEFA_FILE = 23;	// Output SHEF .A format.
public final int OUTPUT_NWSRFSESPTRACEENSEMBLE_FILE = 24;
						// Output NWSRFS ESP Trace
						// Ensemble file
public final int OUTPUT_TABLE = 25;		// Output a table (curently only
						// for display).
public final int OUTPUT_POINT_GRAPH = 26;	// Point graph
public final int OUTPUT_PredictedValue_GRAPH = 27;	
						// Predicted Value graph
public final int OUTPUT_PredictedValueResidual_GRAPH = 28;
						// Predicted Value Residual
						// graph

/**
Filter indicating that output should be data (default).
*/
public final int OUTPUT_FILTER_DATA = 1;
/**
Filter indicating that output should be data coverage (% non-missing).
*/
public final int OUTPUT_FILTER_DATA_COVERAGE = 2;

/**
Output in water year, for use with setOutputYearType().
*/
private final int __WATER_YEAR = 1;

/**
Output in calendar year.
*/
private final int __CALENDAR_YEAR = 2;

// Data members...

/**
If true, then if the output period is specified, time series will be extended to
at least include the output period.
*/
private boolean __AutoExtendPeriod_boolean = true;

/**
Start date/time for averaging.
*/
private DateTime __AverageStart_DateTime = null;

/**
End date for averaging.
*/
private DateTime __AverageEnd_DateTime = null;

// TODO SAM 2007-11-16 Evaluate removing binary time series
/**
Number of time series to trigger use of a binary file to process daily files.
*/
private int __binary_day_cutoff = BINARY_DAY_CUTOFF;

/**
Number of time series to trigger use of a binary file to process monthly files.
*/
private int __binary_month_cutoff = BINARY_MONTH_CUTOFF;

/**
Binary time series.  This is null unless the number of time series is high.
*/
private BinaryTS __binary_ts = null;

/**
Start date for BinaryTS.
*/
private DateTime __binaryts_date1 = null;

/**
End date for BinaryTS.
*/
private DateTime __binaryts_date2 = null;

/**
File for BinaryTS output.
*/
private String __binary_ts_file = "C:\\temp\\tstool.bts";

/**
Indicates whether a BinaryTS file is being used to process output.
*/
private boolean __BinaryTSUsed_boolean = false;

/**
List of data tests that are defined.
*/
private Vector __datatestlist = null;

/**
List of DateTime initialized from commands.
*/
private Hashtable __datetime_Hashtable = new Hashtable ();

/**
DMI for DIADvisor operational database.
*/
private DIADvisorDMI __DIADvisor_dmi = null;

/**
DMI for DIADvisor archive database.
*/
private DIADvisorDMI __DIADvisor_archive_dmi = null;

// TODO 2007-11-16 - why can't this be local in the method that uses it?
/**
Count of errors.
*/
private int	_fatal_error_count = 0;

/**
Vector of StringMonthTS fill patterns used with fillPattern().
*/
private Vector	__fill_pattern_ts = new Vector (20,10);

/**
HydroBase DMI instance Vector, to allow more than one database instance to
be open at a time, only used with openHydroBase().
*/
private Vector __hbdmi_Vector = new Vector();

/**
Indicates whether values <= 0 should be treated as missing when calculating
historical averages.
*/
private boolean __IgnoreLEZero_boolean = false;

/**
Indicates whether missing time series should be added automatically.
*/
private boolean __IncludeMissingTS_boolean = false;

/**
Start date for read.
*/
private DateTime __InputStart_DateTime = null;

/**
End date for read.
*/
private DateTime __InputEnd_DateTime = null;        

// TODO SAM 2007-11-16 Move to local StateMod command parameter.
/**
Missing data value to use with StateMod output.
*/
private double	__missing = -999.0;

/**
Missing data value to use with StateMod output.
*/
private double	__missing_range[] = null;

/**
Vector to save list of time series identifiers that are not found.
*/
private Vector	__missing_ts = new Vector();

// TODO SAM 2007-02-18 need to reenable NDFD
//private Vector __NDFDAdapter_Vector = new Vector();
						// NDFD Adapter instance
						// Vector, to allow more than
						// one database instance to
						// be open at a time, only
						// used with openNDFD().

// TODO SAM 2007-11-16 Can this be removed now that InitialWorkingDir is specified at startup?
/**
The number of commands that are at the beginning of the __commands, which have been
automatically added (e.g., a setWorkingDir() command to initialize the working
directory).
*/
private int __num_prepended_commands = 0;

/**
Number of TS xxx expressions, which is used to size the BinaryTS, if needed.
*/
private int	__num_TS_expressions = 0;

/**
List of NWSRFS_DMI to use to read from NWSRFS FS5Files.
*/
private Vector __nwsrfs_dmi_Vector = new Vector();

/**
Default output file name.
*/
private String	__output_file = "tstool.out";

/**
Indicates whether detailed header should be added to output.
*/
private boolean __OutputDetailedHeader_boolean = false; 

/**
Year type for output (calendar year is the default).
*/
private int __OutputYearType_int = __CALENDAR_YEAR;

/**
Global output start date/time.
*/
private DateTime __OutputStart_DateTime = null;

/**
Global output end date/time.
*/
private DateTime __OutputEnd_DateTime = null;

/**
Indicates whether exported output should be previewed.  The default is false.
 */
private	boolean	__PreviewExportedOutput_boolean = false;

/**
Properties generated/maintained by the processor.  These are initialized when calling
processCommands() and may be modified during command processing.  There are
methods to return key values such as the end result WorkingDir.  This is available to
allow the GUI to process setWorkingDir() commands prior to displaying a command editor,
which requires the working directory at that point of the workflow.
*/
private PropList __processor_PropList = null;

/**
DMI for RiversideDB.
*/
private RiversideDB_DMI __rdmi = null;

/**
Reference date for year to date report (only use month and day).
*/
private DateTime __reference_date = null;

/**
DMI for DIADvisor archive database.
*/
private SatMonSysDMI __smsdmi = null;

/**
The TSCommandProcessor instance that is managing this TSEngine instance.
A valid instance should be passed during construction.
*/
private TSCommandProcessor __ts_processor = null;

/**
Vector of time series vector that is the result of processing.
This will always be non-null so check _binary_ts_used.
*/
private Vector	__tslist = new Vector(50,50);

/**
WindowListener for TSViewJFrame objects, used when calling app wants to listen for
window events on lot windows.
*/
private WindowListener _tsview_window_listener = null;

/**
Construct a TSEngine to work in parallel with a TSCommandProcessor.
@param ts_processor TSCommandProcessor instance that is controlling processing.
*/
protected TSEngine ( TSCommandProcessor ts_processor )
{
	__ts_processor = ts_processor;
	
	// TODO SAM 2007-11-06
	// The following is used to allow cancel during processing.  However, it may
	// be phased out if the CommandStatus handling approach works.
	// Put the code here so that the listener is not re-registered each time that the
	// processor is run.
	
	MessageJDialog.addMessageJDialogListener ( this );
}

/**
Add the annotation provider property to TSView properties.  This examines
DMI instances to see if they implement TSProductAnnotationProvider.  If so,
call the TSViewJFrame.addTSProductAnnotationProvider() method with the instance.
*/
private void addTSViewTSProductAnnotationProviders ( TSViewJFrame view )
{	Vector ap_Vector = getTSProductAnnotationProviders();
	int size = ap_Vector.size();
	for ( int i = 0; i < size; i++ ) {
		view.addTSProductAnnotationProvider(
			(TSProductAnnotationProvider)ap_Vector.elementAt(i),
			null );
	}
}

/**
Make the TSProeductDMI instances known to a TSViewJFrame.  This examines
DMI instances to see if they implement TSProductDMI.  If so, the
TSView.addTSProductDMI() method is called with the instance.
*/
private void addTSViewTSProductDMIs ( TSViewJFrame view )
{	// Check the HydroBase instances...
	int hsize = __hbdmi_Vector.size();
	HydroBaseDMI hbdmi = null;
	for ( int ih = 0; ih < hsize; ih++ ) {
		hbdmi = (HydroBaseDMI)__hbdmi_Vector.elementAt(ih);
		if ((hbdmi != null) && (hbdmi instanceof TSProductDMI)){
			view.addTSProductDMI(hbdmi);
		}
	}
	// Check the RiversideDB_DMI instances...
	if ( (__rdmi != null) && (__rdmi instanceof TSProductDMI) ) {
		view.addTSProductDMI(__rdmi);
	}
}

/**
Add a WindowListener for TSViewJFrame instances that are created.  Currently
only one listener can be set.  This is needed to be able to close down the
application when simple plot interfaces are displayed.
@param listener WindowListener to listen to TSViewJFrame WindowEvents.
*/
private void addTSViewWindowListener ( WindowListener listener )
{	_tsview_window_listener = listener;
}

/**
Append time series to the results list.
@param ts Time series to append
@throws Exception if there is an error appending the time series
*/
protected void appendTimeSeries ( TS ts )
throws Exception
{
	// Position is zero index so request one more than the actual size.
	int size = 0;
	if ( __tslist != null ) {
		size = __tslist.size();
	}
	setTimeSeries ( ts, size );
}

/**
Calculate the average values for a time series.
@return the average data for a time series using the averaging and
output period.  If a monthly time series, a MonthTSLimits will be returned.
Otherwise, a TSLimits will be returned.
The overloaded method is called with a time series counter having the return
value of getTimeSeriesSize().
@param ts Monthly time series to process.
@exception Exception if there is an error getting the limits.
*/
protected TSLimits calculateTSAverageLimits ( TS ts )
throws Exception
{	return calculateTSAverageLimits ( getTimeSeriesSize(), ts );
}

/**
Calculate the average data limits for a time series using the averaging period
if specified (otherwise use the available period).
@return the average data for a time series.
If a monthly time series, a MonthTSLimits will be returned.
@param i Counter for time series being processed (starting at zero), used to
control printing of messages.
@param ts Monthly time series to process.
Currently only limits for monthly time series are supported.
@exception Exception if there is an error getting the limits.
*/
private TSLimits calculateTSAverageLimits ( int i, TS ts )
throws Exception
{	String message, routine = "TSEngine.calculateTSAverageLimits";
	TSLimits average_limits = null;

	if ( ts == null ) {
		message =
		"Time series is null.  Can't calculate average limits.";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}
	if ( Message.isDebugOn ) {
		Message.printDebug ( 1, routine,
		"Trying to calculate limits for " + ts.getIdentifierString() );
	}
	if ( ts.getDataIntervalBase() == TimeInterval.MONTH ) {
		// Set the flag to pass to the limits code...
		int limits_flag = 0;
		if ( getIgnoreLEZero() ) {
			limits_flag = TSLimits.IGNORE_LESS_THAN_OR_EQUAL_ZERO;
		}
		try {	if ( haveAveragingPeriod() ) {
				// Get the average values from the averaging period...
				if ( i <= 0 ) {
					Message.printStatus ( 2, routine, "Specified averaging period is:  " +
					getAverageStart() + " to " + getAverageEnd() );
				}
				average_limits = new MonthTSLimits( (MonthTS)ts, getAverageStart(), getAverageEnd(), limits_flag);
			}
			else {	// Get the average values from the available
				// period...
				if ( i <= 0 ) {
					// Print the message once...
					Message.printStatus ( 2, routine,
					"No averaging period specified. " +
					"Will use available period.");
				}
				average_limits = new MonthTSLimits( (MonthTS)ts,
					ts.getDate1(), ts.getDate2(),
					limits_flag );
			}
		}
		catch ( Exception e ) {
			message = "Error getting limits for time series.";
			Message.printWarning ( 2, routine, message );
			throw new Exception ( message );
		}
	}
	else if ( ts.getDataIntervalBase() == TimeInterval.YEAR ) {
		// Set the flag to pass to the limits code...
		int limits_flag = 0;
		if ( getIgnoreLEZero() ) {
			limits_flag = TSLimits.IGNORE_LESS_THAN_OR_EQUAL_ZERO;
		}
		try {	if ( haveAveragingPeriod() ) {
				// Get the average values from the averaging
				// period...
				if ( i <= 0 ) {
					Message.printStatus ( 2, routine, "Specified averaging period is:  " +
					getAverageStart() + " to " + getAverageEnd().toString() );
				}
				average_limits = new TSLimits( ts, getAverageStart(), getAverageEnd(), limits_flag);
			}
			else {	// Get the average values from the available
				// period...
				if ( i <= 0 ) {
					// Print the message once...
					Message.printStatus ( 2, routine,
					"No averaging period specified. " +
					"Will use available period.");
				}
				average_limits = new TSLimits ( ts,
					ts.getDate1(), ts.getDate2(),
					limits_flag );
			}
		}
		catch ( Exception e ) {
			message = "Error getting limits for time series.";
			Message.printWarning ( 2, routine, message );
			throw new Exception ( message );
		}
	}
	else {	// For now we probably won't use average limits for other
		// time steps so to increase performance don't compute...
		// If historical averages are used for filling daily, then add
		// daily at some point.
		if ( Message.isDebugOn ) {
			Message.printStatus ( 2, routine,
			"Calculation of historic average limits for other " +
			"than monthly and yearly data is not enabled: " +
			ts.getIdentifierString() + ")" );
		}
	}
	return average_limits;
}

/**
Clear the time series results.  The commands will need to be rerun to regenerate
the results.
*/
protected void clearResults ( )
{
	if ( __tslist != null ) {
		__tslist.removeAllElements();
	}
}

/**
Create data limits report.  Currently this creates a report for the available
period for the time series (it does not check the output period).
@param tslist list of time series to analyze.
*/
private Vector createDataLimitsReport ( Vector tslist )
{	int size = tslist.size();
	Vector report = new Vector ();

	report.addElement ( "" );
	report.addElement ( "DATA LIMITS REPORT" );
	report.addElement ( "" );

	TS ts = null;
	TSLimits limits = null;
	for ( int i = 0; i < size; i++ ) {
		ts = (TS)tslist.elementAt(i);
		if ( ts == null ) {
			continue;
		}
		if ( ts.getDataIntervalBase() == TimeInterval.MONTH ) {
			try {	limits = new MonthTSLimits ( (MonthTS)ts );
			}
			catch ( Exception e ) {
				limits = null;
			}
		}
		else {	limits = TSUtil.getDataLimits ( ts,
				ts.getDate1(), ts.getDate2() );
		}
		if ( limits == null ) {
			continue;
		}
		report.addElement ( "" );
		report.addElement ( ts.getDescription() );
		report.addElement ( ts.getIdentifierString() );
		report.addElement ( "" );
		// The limits come back as a string with line breaks.  This
		// apparenltly is displayed correctly but does not print
		// correctly so break into separate strings here in order to
		// keep the strings consistent...
		StringUtil.addListToStringList ( report,
			StringUtil.breakStringList(limits.toString(),
			"\n", 0) );
		report.addElement ( "" );
	}
	ts = null;
	limits = null;
	return report;
}

/**
Create monthly summary reports.  Currently this creates a report for the
available period for the time series (it does not check the output period).
@param tslist list of time series to analyze.
@param props Properties to control report (see TSUtil.createMonthSummary()).
*/
private Vector createMonthSummaryReport ( Vector tslist, PropList props )
{	int size = tslist.size();
	Vector report = new Vector ();
	String routine = "TSEngine.createMonthSummaryReport";

	String prop_val = props.getValue ( "DayType" );
	
	report.addElement ( "" );
	if ( (prop_val != null) && prop_val.equalsIgnoreCase("Total") ) {
		report.addElement ( "MONTHLY SUMMARY REPORT (Daily Totals)" );
	}
	else {	report.addElement ( "MONTHLY SUMMARY REPORT (Daily Means)" );
	}
	report.addElement ( "" );

	TS ts = null;
	Vector summary = null;
	int interval_base;
	int error_count = 0;
	for ( int i = 0; i < size; i++ ) {
		ts = (TS)tslist.elementAt(i);
		if ( ts == null ) {
			continue;
		}
		interval_base = ts.getDataIntervalBase();
		if (	(interval_base == TimeInterval.DAY) ||
			(interval_base == TimeInterval.HOUR) ||
			(interval_base == TimeInterval.MINUTE) ) {
			try {	summary = TSUtil.createMonthSummary ( ts,
					ts.getDate1(), ts.getDate2(), props );
				if ( summary == null ) {
					continue;
				}
			}
			catch ( Exception e ) {
				Message.printWarning ( 2, "", e );
				++error_count;
				continue;
			}
			report.addElement ( "" );
			StringUtil.addListToStringList ( report, summary );
			report.addElement ( "" );
		}
		else {	report.addElement ( "" );
			report.addElement ( ts.getDescription() );
			report.addElement ( ts.getIdentifierString() );
			report.addElement ( "" );
			report.addElement ( "Summary report is not supported" +
				" for time series interval." );
			report.addElement ( "" );
			++error_count;
		}
	}
	if ( error_count > 0 ) {
		Message.printWarning ( 1, routine,
		"There was an error creating the report.\n" +
		"See the log file for details." );
	}
	ts = null;
	summary = null;
	return report;
}

/**
Create a year to date report listing the total accumulation of water volume for
each year to the specified date.  Handle real-time and historic.  But only
CFS units.
@param tslist list of time series to analyze.
@param end_date Ending date for annual total (precision day!).
@param props Properties to control output (currently the only property is
SortTotals=true/false).
*/
private Vector createYearToDateReport ( Vector tslist, DateTime end_date,
				PropList props )
{	int size = tslist.size();
	Vector report = new Vector ();

	report.addElement ( "" );
	report.addElement ( "Annual totals to date ending on " +
			TimeUtil.MONTH_ABBREVIATIONS[end_date.getMonth() - 1]
			+ " " + end_date.getDay() );
	report.addElement ( "" );

	TS ts = null;
	DateTime start = null;
	DateTime end = null;
	int nyears = 0;
	double totals[] = null;
	int years[] = null;
	int missing[] = null;
	int ypos = 0;
	for ( int i = 0; i < size; i++ ) {
		ts = (TS)tslist.elementAt(i);
		if ( ts == null ) {
			continue;
		}
		if (	!(((ts.getDataIntervalBase() == TimeInterval.DAY) ||
			(ts.getDataIntervalBase() == TimeInterval.IRREGULAR)) &&
			ts.getDataUnits().equalsIgnoreCase("CFS")) ) {
			report.addElement ( "" );
			report.addElement ( ts.getIdentifierString() +
			" - " + ts.getDescription() + " Not CFS" );
			report.addElement ( "" );
			continue;
		}
		report.addElement ( "" );
		report = StringUtil.addListToStringList ( report,
			ts.getComments());
		report.addElement ( "" );
		report.addElement ( "         Total           Number" );
		report.addElement ( "         ACFT through    of missing" );
		report.addElement ( "Year     "
			+ TimeUtil.MONTH_ABBREVIATIONS[end_date.getMonth() - 1]
			+ " "
			+ StringUtil.formatString(end_date.getDay(),"%02d")
			+ "  days" );

		// If an irregular time series, convert to a daily...

		if ( ts.getDataIntervalBase() == TimeInterval.IRREGULAR ) {
			// Convert to a daily time series...
			ts = TSUtil.changeInterval ( ts, TimeInterval.DAY, 1 );
		}

		int interval_base = ts.getDataIntervalBase();
		int interval_mult = ts.getDataIntervalMult();
		start = new DateTime (ts.getDate1());	// Overall start
		start.setPrecision ( DateTime.PRECISION_DAY );
		int year1 = start.getYear();
		end = new DateTime(ts.getDate2());	// Overall end
		end.setPrecision ( DateTime.PRECISION_DAY );
		nyears = end.getYear() - start.getYear() + 1;
		// Should not hit the following now since put in a check above.
		if ( interval_base == TimeInterval.MONTH ) {
			// Should make comparisons work...
			end.setDay ( TimeUtil.numDaysInMonth(end.getMonth(),
				end.getYear()) );
		}
		// Memory for yearly values...
		totals = new double[nyears];
		missing = new int[nyears];
		years = new int[nyears];
		for ( int j = 0; j < nyears; j++ ) {
			totals[j] = 0.0;	// OK to initialize to zero
						// since we are tracking the
						// number of missing now.
			years[j] = year1 + j;
			missing[j] = 0;
		}
		double value = 0.0;
		// Loop using addInterval...
		DateTime date = new DateTime ( start );
		// Date to stop accumulating for the year, passed in...
		DateTime compare_date = new DateTime ( end_date );
		compare_date.setYear ( date.getYear());
		// Use next_year as a quick check for whether we have gone to
		// the next year...
		int next_year = date.getYear() + 1;
		for ( ;
			date.lessThanOrEqualTo( end );
			date.addInterval(interval_base, interval_mult)){
			value = ts.getDataValue ( date );
			if (	date.lessThanOrEqualTo(compare_date) ) {
				// Got the data value...
				;
			}
			// This checks exactly for the next year, which will
			// work with daily or other regular data data...
			else if ( date.getYear() == next_year ) {
				// Have read the next starting date (start of
				// next year)...
				// Need to set the new compare date...
				compare_date.setYear ( date.getYear());
				++next_year;
				// Data value still in memory and will be
				// checked below...
			}
			else {	// Period after compare_date and before the
				// end of the year.  Ignore the data in the
				// total...
				continue;
			}
			// Below here have a valid piece of data so check it
			// for correctness...
			ypos = date.getYear() - year1;
			if ( ts.isDataMissing(value) ) {
				// Keep track of how many missing in the
				// year...
				++missing[ypos];
			}
			else {	if ( interval_base == TimeInterval.DAY ) {
					// Convert daily average flow to ACFT...
					value = value*1.9835;
				}
				//Message.printStatus ( 1, "",
				//"Adding " + value + " to year " +
				//years[ypos] +
				//" " + date.toString());
				if ( totals[ypos] < 0.0 ) {
					// Initial value of total so set it...
					totals[ypos] = value;
				}
				else {	// Just add to existing total...
					totals[ypos] += value;
				}
			}
		}
		// Print the sorted totals...
		int sort_order[] = new int[nyears];
		MathUtil.sort ( totals, MathUtil.SORT_QUICK,
				MathUtil.SORT_ASCENDING, sort_order, true );
		for ( int j = 0; j < nyears; j++ ) {
			report.addElement ( years[sort_order[j]] + "  "
			+ StringUtil.formatString(totals[j], "%11.0f") +
			"          " +
			StringUtil.formatString(missing[sort_order[j]],
			"%5d"));
		}
		sort_order = null;
		totals = null;
		years = null;
		start = null;
		missing = null;
		end = null;
	}
	return report;
}

/**
Delete the BinaryTS associated with the TSEngine, containing the results of
time series expression processing.
*/
private void deleteBinaryTS ()
{	try {	if ( getBinaryTS() != null ) {
			__binary_ts.delete ();
			setBinaryTS ( null );
			setBinaryTSUsed ( false );
		}
	}
	catch ( Exception e ) {
		// For now ignore...
		Message.printWarning ( 2, "TSEngine.deleteBinaryTS", "Error deleting BinaryTS file" );
		Message.printWarning ( 2, "TSEngine.deleteBinaryTS", e );
	}
}

/**
Create the data coverage report dialog.
@return contents of the report.
@param orig_tslist Original list of time series to process.
@exception Exception if there is an error.
*/
private Vector createDataCoverageReport ( Vector orig_tslist )
throws Exception
{	String routine = "TSEngine.createDataCoverageReport";
	// If the output is to be a data coverage time series, convert each
	// time series to the monthly statistics format and feed into the
	// report.  This may be slower than before TSTool 05.00.xx but is
	// consistent with how TSTool handles time series in memory now.

	MonthTS newts = null;
	Message.printStatus ( 1, routine, "Starting report at:  " +
		new DateTime(DateTime.DATE_CURRENT).toString() );
	TSAnalyst analyst = new TSAnalyst();
	// Start the data coverage report...
	try {	PropList props = new PropList ( "tsanalyst" );
		if ( getOutputYearTypeInt() == __WATER_YEAR ) {
			props.set( "CalendarType=WaterYear" );
		}
		else {	// Default...
		}
		if ( haveOutputPeriod() ) {
			// Use it...
			analyst.startDataCoverageReport (
				__OutputStart_DateTime,
				__OutputEnd_DateTime, props );
		}
		else {	// Figure out the maximum period and start the report
			// with that...
			TSLimits report_limits = TSUtil.getPeriodFromTS (
				__tslist, TSUtil.MAX_POR );
			analyst.startDataCoverageReport (
				report_limits.getDate1(),
				report_limits.getDate2(), props );
		}
		props = null;
	}
	catch ( Exception e ) {
		// Data coverage report was a failure, just ignore for now.
		throw new Exception ( "Error starting data coverage report." );
	}
	// Convert each time series to a monthly statistics time series.
	Message.printStatus ( 1, routine,
	"Changing time series to data coverage time series." );
	int ntslist = 0;
	if ( orig_tslist != null ) {
		ntslist = orig_tslist.size();
	}
	for ( int i = 0; i < ntslist; i++ ) {
		try {	newts = TSAnalyst.createStatisticMonthTS(
				(TS)orig_tslist.elementAt(i), null );
			analyst.appendToDataCoverageSummaryReport ( newts );
			// Don't actually need the time series...
			newts = null;
		}
		catch ( Exception e ) {
			Message.printWarning ( 2, routine,
			"Error creating Statistic Month TS for \"" +
			((TS)orig_tslist.elementAt(i)).getIdentifierString() +
			"\"" );
			continue;
		}
	}
	// Now return the report contents...
	Vector report = analyst.getDataCoverageReport();
	analyst = null;
	newts = null;
	return report;
}

/**
Helper method for add() and subtract() commands.
@param command_tag Command number used for messaging.
@param command Command being evaluated.
@exception Exception if there is an error processing the time series.
*/
private void do_add ( String command_tag, String command )
throws Exception
{	String command_name = "add";
	if ( StringUtil.startsWithIgnoreCase(command,"add") ) {
		command_name = "add";
	}
	else {	command_name = "subtract";
	}
	String routine = "TSEngine.do_" + command_name;
	String TSID = null;
	String TSList = null;
	String HandleMissingHow = null;
	String AddTSID = null;
	if ( command.indexOf('=') < 0 ) {
		// Old syntax...
		// Don't use space because TEMPTS will not parse right.
		Vector v = StringUtil.breakStringList(command,
			"(),\t", StringUtil.DELIM_SKIP_BLANKS); 
		if ( (v == null) || (v.size() < 3) ) {
			Message.printWarning ( 2, routine,
			"Syntax error in \"" + command + "\"" );
			throw new Exception ( "Bad command syntax: " +
				command );
		}

		// Get the individual tokens of the expression...

		TSList = "SpecifiedTS";
		TSID = ((String)v.elementAt(1)).trim();
		HandleMissingHow = ((String)v.elementAt(2)).trim();
		if (	!HandleMissingHow.equalsIgnoreCase( "IgnoreMissing" ) &&
			!HandleMissingHow.equalsIgnoreCase(
			"SetMissingIfOtherMissing") && 
			!HandleMissingHow.equalsIgnoreCase(
			"SetMissingIfAnyMissing") ) {
			// Old style syntax.
			String message =
			"Old-style add() command is obsolete.\n" +
			"Please update to new syntax.  Not processing.";
			Message.printWarning ( 2, routine, message );
			throw new Exception ( message );
		}
		StringBuffer b = new StringBuffer();
		int size = v.size();
		for ( int i = 3; i < size; i++ ) {
			if ( i != 3 ) {
				b.append ( "," );
			}
			b.append ( (String)v.elementAt(i) );
		}
		AddTSID = b.toString ();
	}
	else {	// New syntax...
		Vector tokens = StringUtil.breakStringList ( command,
			"()", StringUtil.DELIM_SKIP_BLANKS );
		if ( (tokens == null) || (tokens.size() < 2) ) {
			// Should never happen because the command name was
			// parsed before...
			throw new Exception (
			"Bad command: \"" + command + "\"" );
		}
		// Get the input needed to process the file...
		PropList props = PropList.parse (
			(String)tokens.elementAt(1), routine, "," );
		TSID = props.getValue ( "TSID" );
		TSList = props.getValue ( "TSList" );
		HandleMissingHow = props.getValue ( "HandleMissingHow" );
		if ( command_name.equalsIgnoreCase("add") ) {
			AddTSID = props.getValue ( "AddTSID" );
		}
		else {	AddTSID = props.getValue ( "SubtractTSID" );
		}
	}

	if ( TSID == null ) {
		String message =
		"Need TSID or pattern to " + command_name + " time series.";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}
	if ( StringUtil.indexOfIgnoreCase(TSID, "FrostDate", 0) >= 0 ) {
		// TODO - SAM 2005-05-20
		// This is a special check because the add() command used to
		// be used in TSTool to add frost date time series.  Now the
		// add() command is not suitable.
		String message = "The " + command_name + "() command is not suitable for frost dates." +
		"Use Blend(), SetFromTS(), or similar commands.";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}
	if ( TSList == null ) {
		String message = "TSList must be defined for: " + command_name;
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}
	if ( TSList.equalsIgnoreCase("SpecifiedTS") && (AddTSID == null) ) {
		if ( command_name.equalsIgnoreCase("add") ) {
			String message = "AddTSID must be defined for: " + command_name;
			Message.printWarning ( 2, routine, message );
			throw new Exception ( message );
		}
		else if ( command_name.equalsIgnoreCase("subtract") ) {
			String message =
			"SubtractTSID must be defined for: " + command_name;
			Message.printWarning ( 2, routine, message );
			throw new Exception ( message );
		}
	}

	int imissing_type = TSUtil.IGNORE_MISSING;
	if ( HandleMissingHow == null ) {
		HandleMissingHow = "IgnoreMissing";
	}
	if ( HandleMissingHow.equalsIgnoreCase( "IgnoreMissing" ) ) {
		imissing_type = TSUtil.IGNORE_MISSING;
	}
	else if ( HandleMissingHow.equalsIgnoreCase(
		"SetMissingIfOtherMissing" ) ){
		imissing_type = TSUtil.SET_MISSING_IF_OTHER_MISSING;
	}
	else if ( HandleMissingHow.equalsIgnoreCase(
		"SetMissingIfAnyMissing" ) ) {
		imissing_type=TSUtil.SET_MISSING_IF_ANY_MISSING;
	}

	// Make sure there are time series available to operate on...

	int ts_pos = indexOf ( TSID );
	TS ts = getTimeSeries ( ts_pos );
	if ( ts == null ) {
		String message = "Unable to find time series \"" + TSID +
			"\" for " + command + "().";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}
	// Now loop through the remaining time series and put into a Vector for
	// processing...
	Vector v = StringUtil.breakStringList ( AddTSID,
			",", StringUtil.DELIM_SKIP_BLANKS );
	int vsize = v.size();
	Vector vTS = null;
	TS independentTS = null;
	
	if (	TSList.equalsIgnoreCase("AllTS") ||
		(TSList.equalsIgnoreCase("SpecifiedTS") &&
		(vsize == 1) && ((String)v.elementAt(0)).trim().equals("*")) ) {
		// Process everything in memory...
		int nts = getTimeSeriesSize();
		vTS = new Vector ( nts );
		for ( int its = 0; its < nts; its++ ) {
			independentTS = getTimeSeries ( its );
			// Do not add a time series to itself...
			if (	(independentTS != null) &&
				(independentTS != ts) ) {
				vTS.addElement (independentTS);
			}
		}
	}
	else if ( TSList.equalsIgnoreCase("SelectedTS") ) {
		vTS = getSelectedTimeSeriesList ( false );
	}
	else if ( TSList.equalsIgnoreCase("SpecifiedTS") ) {
		// Get the requested time series...
		vTS = new Vector ( vsize );
		String independent = null;
		int error_count = 0;
		for (	int its = 0; its < vsize; its++ ) {
			independent = ((String)v.elementAt(its)).trim();
			// The following may or may not have TEMPTS at the
			// front but is handled transparently by getTimeSeries.
			independentTS = getTimeSeries (
				command_tag, independent );
			if ( independentTS == null ) {
				Message.printWarning ( 2, routine,
				"Unable to find time series \""+
				independent + "\" for\n\"" +
				command + "\"." );
				++error_count;
			}
			else {	vTS.addElement ( independentTS);
			}
		}
		if ( error_count > 0 ) {
			throw new Exception (
			"Error finding one or more time series in \"" +
				command + "\"" );
		}
	}
	if ( command_name.equalsIgnoreCase("add") ) {
		Message.printStatus ( 2, routine,
		"Adding " + vTS.size() + " time series." );
			ts = TSUtil.add ( ts, vTS, imissing_type );
	}
	else {	ts = TSUtil.subtract ( ts, vTS, imissing_type );
	}
	v = null;
	vTS = null;
	independentTS = null;
	// Update the time series...
	processTimeSeriesAction ( UPDATE_TS, ts, ts_pos );
}

/**
Helper method for adjustExtremes() command.
@param command Command being evaluated.
@exception Exception if there is an error processing the time series.
*/
private void do_adjustExtremes ( String command )
throws Exception
{	// Don't parse with spaces because a TEMPTS or dates with hours may be
	// present.
	Vector v = StringUtil.breakStringList(command,
		"(),", StringUtil.DELIM_SKIP_BLANKS|
		StringUtil.DELIM_ALLOW_STRINGS ); 
	String message, routine = "TSEngine.do_adjustExtremes";
	if ( (v == null) || (v.size() < 8) ) {
		message = "Syntax error in \"" + command + "\"";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}
	// Get the individual tokens of the expression...
	String tsident = ((String)v.elementAt(1)).trim();
	// Adjust method...
	String adjust_method = ((String)v.elementAt(2)).trim();
	// Extreme to adjust...
	String extreme_flag = ((String)v.elementAt(3)).trim();
	// Extreme value...
	String extreme_value = ((String)v.elementAt(4)).trim();
	// Max intervals...
	String max_intervals = ((String)v.elementAt(5)).trim();
	// Analysis period...
	String analysis_period_start = ((String)v.elementAt(6)).trim();
	DateTime start = getDateTime ( analysis_period_start );
	String analysis_period_end = ((String)v.elementAt(7)).trim();
	DateTime end = getDateTime ( analysis_period_end );
	// Apply adjustExtremes()...  If the identifier is "*", apply to
	// all the time series...
	TS ts;
	if ( tsident.equals("*") ) {
		// Fill everything in memory...
		int nts = getTimeSeriesSize();
		// Set first in case there is an exception...
		for ( int its = 0; its < nts; its++ ) {
			ts = getTimeSeries(its);
			TSUtil.adjustExtremes ( ts, adjust_method, extreme_flag,
				StringUtil.atod(extreme_value),
				StringUtil.atoi(max_intervals), start, end );
			processTimeSeriesAction ( UPDATE_TS, ts, its );
		}
	}
	else {	// Operate on a single time series...
		int ts_pos = indexOf ( tsident );
		ts = getTimeSeries ( ts_pos );
		if ( ts == null ) {
			message = "Unable to find time series \"" + tsident +
			"\" for adjustExtremes().";
			Message.printWarning ( 1, routine, message );
			throw new Exception ( message );
		}
		TSUtil.adjustExtremes ( ts, adjust_method, extreme_flag,
			StringUtil.atod(extreme_value),
			StringUtil.atoi(max_intervals), start, end );
		processTimeSeriesAction ( UPDATE_TS, ts, ts_pos );
	}
}

/**
Helper method for ARMA() command.
@param command Command being evaluated.
@exception Exception if there is an error processing the time series.
*/
private TS do_ARMA ( String command )
throws Exception
{	// Don't parse with spaces because a TEMPTS may be present.
	Vector v = StringUtil.breakStringList(command,
		"(),\t", StringUtil.DELIM_SKIP_BLANKS); 
	String message, routine = "TSEngine.do_ARMA";
	if ( (v == null) || (v.size() < 5) ) {
		message = "Syntax error in \"" + command + "\"";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}
	// Get the individual tokens of the expression...
	String dependent = ((String)v.elementAt(1)).trim();
	// Make sure there are time series available to operate on...
	int ts_pos = indexOf ( dependent );
	TS dependentTS = getTimeSeries ( ts_pos );
	if ( dependentTS == null ) {
		message = "Unable to find time series \"" + dependent +
		"\" for ARMA().";
		Message.printWarning ( 1, routine, message );
		throw new Exception ( message );
	}
	// Get the ARMA interval...
	String ARMA_interval = ((String)v.elementAt(2)).trim();
	// Get the a and b coefficients...
	double [] a = null, b = null;
	String token;
	boolean in_p = false;
	boolean in_q = false;
	int n_a = 0, n_b = 0;
	for ( int i = 3; i < v.size(); i++ ) {
		token = ((String)v.elementAt(i)).trim();
		if ( token.regionMatches(true,0,"p",0,1) ) {
			// Start A coefficients...
			a = new double[StringUtil.atoi (token.substring(1))];
			in_p = true;
			in_q = false;
			continue;
		}
		else if ( token.regionMatches(true,0,"q",0,1) ) {
			// Start B coefficients...
			b = new double[StringUtil.atoi(token.substring(1)) + 1];
			in_p = false;
			in_q = true;
			continue;
		}
		if ( in_p ) {
			a[n_a++] = StringUtil.atod (
				((String)v.elementAt(i)).trim() );
		}
		else if ( in_q ) {
			b[n_b++] = StringUtil.atod (
				((String)v.elementAt(i)).trim() );
		}
	}
	// Apply ARMA...
	TS ts = TSUtil.ARMA ( dependentTS, ARMA_interval, a, b );
	processTimeSeriesAction ( UPDATE_TS, dependentTS, ts_pos );
	return ts;
}

/**
Helper method for convertDataUnits() command.
@param command Command being evaluated.
@exception Exception if there is an error processing the time series.
*/
private void do_convertDataUnits ( String command )
throws Exception
{	// Don't parse with spaces because a TEMPTS or dates with hours may be
	// present.
	Vector v = StringUtil.breakStringList(command,
		"(),", StringUtil.DELIM_SKIP_BLANKS|
		StringUtil.DELIM_ALLOW_STRINGS ); 
	String message, routine = "TSEngine.do_convertDataUnits";
	if ( (v == null) || (v.size() != 3) ) {
		message = "Syntax error in \"" + command + "\"";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}
	// Get the individual tokens of the expression...
	String tsident = ((String)v.elementAt(1)).trim();
	// New units...
	String new_units = ((String)v.elementAt(2)).trim();
	// Apply adjustExtremes()...  If the identifier is "*", apply to
	// all the time series...
	TS ts;
	if ( tsident.equals("*") ) {
		// Process every time series in memory...
		int nts = getTimeSeriesSize();
		// Set first in case there is an exception...
		for ( int its = 0; its < nts; its++ ) {
			ts = getTimeSeries(its);
			TSUtil.convertUnits ( ts, new_units );
			processTimeSeriesAction ( UPDATE_TS, ts, its );
		}
	}
	else {	// Operate on a single time series...
		int ts_pos = indexOf ( tsident );
		ts = getTimeSeries ( ts_pos );
		if ( ts == null ) {
			message = "Unable to find time series \"" + tsident +
			"\" for convertDataUnits().";
			Message.printWarning ( 1, routine, message );
			throw new Exception ( message );
		}
		TSUtil.convertUnits ( ts, new_units );
		processTimeSeriesAction ( UPDATE_TS, ts, ts_pos );
	}
}

/**
Execute the CreateFromList() command.
@param wl Warning level for important warnings (1=popup, 2=to log only).
@param command_tag Command number used for messaging.
@param command Command to parse.
@exception Exception if there is an error.
*/
private void do_createFromList ( int wl, String command_tag, String command )
throws Exception
{	String routine = "TSEngine.do_createFromList";
	String message;

	String ListFile = null;
	String IDCol =  null;
	String Delim =  null;
	String ID =  null;
	String DataSource = null;
	String DataType = null;
	String Interval = null;
	String Scenario = null;
	String InputType = null;
	String InputName = null;
	String HandleMissingTSHow = null;
	String DefaultUnits = null;
	if ( command.indexOf('=') < 0 ) {
		// Old syntax...
		Vector tokens = StringUtil.breakStringList ( command,
			" \t(),", StringUtil.DELIM_SKIP_BLANKS |
			StringUtil.DELIM_ALLOW_STRINGS );
		if ( (tokens == null) || (tokens.size() != 6) ) {
			throw new Exception ( "Bad command: \"" + command+"\"");
		}
		// Get the input needed to process the file...
		ListFile = (String)tokens.elementAt(1);
		DataSource = (String)tokens.elementAt(2);
		DataType = (String)tokens.elementAt(3);
		Interval = (String)tokens.elementAt(4);
		InputType = "HydroBase";	// old default
		HandleMissingTSHow = (String)tokens.elementAt(5);
	}
	else {	// New syntax...
		Vector tokens = StringUtil.breakStringList ( command,
			"()", StringUtil.DELIM_SKIP_BLANKS );
		if ( (tokens == null) || (tokens.size() < 2) ) {
			// Should never happen because the command name was
			// parsed before...
			throw new Exception (
			"Bad command: \"" + command + "\"" );
		}
		// Get the input needed to process the file...
		PropList props = PropList.parse ( (String)tokens.elementAt(1), routine, "," );
		ListFile = props.getValue ( "ListFile" );
		IDCol = props.getValue ( "IDCol" );
		Delim = props.getValue ( "Delim" );
		ID = props.getValue ( "ID" );
		DataSource = props.getValue ( "DataSource" );
		DataType = props.getValue ( "DataType" );
		Interval = props.getValue ( "Interval" );
		Scenario = props.getValue ( "Scenario" );
		InputType = props.getValue ( "InputType" );
		InputName = props.getValue ( "InputName" );
		HandleMissingTSHow=props.getValue("HandleMissingTSHow");
		DefaultUnits=props.getValue("DefaultUnits");
	}

	if ( ListFile == null ) {
		message = "List file must be specified.";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}

	if ( Delim == null ) {
		Delim = " ,";
	}

	if ( ID == null ) {
		ID = "*";
	}
	String idpattern_Java = StringUtil.replaceString(ID,"*",".*");

	int IDCol_int = 0;
	if ( IDCol != null ) {
		if ( !StringUtil.isInteger(IDCol) ) {
			message = "IDCol \"" + IDCol + "\" is not an integer.";
			Message.printWarning ( 2, routine, message );
			throw new Exception ( message );
		}
		IDCol_int = StringUtil.atoi ( IDCol ) - 1;
	}

	if ( DataSource == null ) {
		DataSource = "";
	}

	if ( DataType == null ) {
		DataType = "";
	}

	if ( Interval == null ) {
		Interval = "";
	}

	if ( Scenario == null ) {
		Scenario = "";
	}

	if ( InputType == null ) {
		message = "Input type must be specified.";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}
	if ( InputName == null ) {
		// Set to empty string so check to facilitate processing...
		InputName = "";
	}

	boolean	include_missing_ts = false;
	if (	!HandleMissingTSHow.equalsIgnoreCase("IgnoreMissingTS") &&
		!HandleMissingTSHow.equalsIgnoreCase("DefaultMissingTS") ) {
		message = "Unknown HandleMissingTSHow flag \"" +
			HandleMissingTSHow + "\" for \""+ command + "\"";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}
	if ( HandleMissingTSHow.equalsIgnoreCase("IgnoreMissingTS") ) {
		include_missing_ts = false;
	}
	else if ( HandleMissingTSHow.equalsIgnoreCase("DefaultMissingTS") ) {
		include_missing_ts = true;
	}

	// Read using the table...

	PropList props = new PropList ("");
	props.set ( "Delimiter=" + Delim );	// see existing prototype
	props.set ( "CommentLineIndicator=#" );	// New - skip lines that start
						// with this
	props.set ( "TrimStrings=True" );	// If true, trim strings after reading.
    String ListFile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(__ts_processor),ListFile));
	DataTable table = DataTable.parseFile (	IOUtil.getPathUsingWorkingDir(ListFile_full), props );
	
	int tsize = 0;
	if ( table != null ) {
		tsize = table.getNumberOfRecords();
	}

	Message.printStatus ( 2, "", "List file has " + tsize +
		" records and " + table.getNumberOfFields() + " fields" );

	// Loop through the records in the table and match the identifiers...

	StringBuffer tsident_string = new StringBuffer();
	TableRecord rec = null;
	String id;
	boolean include_missing_ts_old;
	TS ts = null;
	for ( int i = 0; i < tsize; i++ ) {
		rec = table.getRecord ( i );
		id = (String)rec.getFieldValue ( IDCol_int );
		if ( !StringUtil.matchesIgnoreCase(id,idpattern_Java) ) {
			// Does not match...
			continue;
		}

		tsident_string.setLength(0);
		tsident_string.append ( id + "." +
				DataSource + "." +
				DataType + "." +
				Interval + "~" +
				InputType );
		if ( InputName.length() > 0 ) {
			tsident_string.append ( "~" + InputName );
		}
		// Now read the time series (the following will create
		// an empty time series if requested).
		// Temporarily reset the __include_missing_ts flag based
		// on this command because the global flag is used in the
		// readTimeSeries method.
		// TODO SAM 2007-03-12 Need to avoid global flag.
		include_missing_ts_old = getIncludeMissingTS();
		try {
            setIncludeMissingTS ( include_missing_ts );
			ts = readTimeSeries ( wl, command_tag, tsident_string.toString() );
			setIncludeMissingTS ( include_missing_ts_old );
			if ( ts != null ) {
				if ( (DefaultUnits != null) && (ts.getDataUnits().length() == 0) ) {
					// Time series has no units so assign default.
					ts.setDataUnits ( DefaultUnits );
				}
				processTimeSeriesAction ( INSERT_TS, ts, getTimeSeriesSize());
			}
		}
		catch ( Exception e ) {
			setIncludeMissingTS ( include_missing_ts_old );
		}
		// Cancel processing if the user has indicated to do so...
		if ( __ts_processor.getCancelProcessingRequested() ) {
			return;
		}
	}
}

// TODO SAM 2005-09-14 Evaluate how this works with other TSAnalyst capabilities
/**
Execute the following command:
<pre>
createYearStatisticsReport(OutputFile="x",TSOutputFile="x")
</pre>
@param command Command to parse.
@exception Exception if there is an error.
*/
private void do_createYearStatisticsReport ( String command )
throws Exception
{	String routine = "TSEngine.do_createYearStatisticsReport", message;
	Vector tokens = StringUtil.breakStringList ( command,
			"()", StringUtil.DELIM_SKIP_BLANKS );
	if ( (tokens == null) || (tokens.size() < 1) ) {
		// Should never happen because the command name was parsed
		// before...
		throw new Exception ( "Bad command: \"" + command + "\"" );
	}
	// Get the input needed to process the file...
	PropList props = PropList.parse (
		(String)tokens.elementAt(1), routine, "," );
	String OutputFile = props.getValue ( "OutputFile" );
	String TSOutputFile = props.getValue ( "TSOutputFile" );
	String MeasTimeScale = props.getValue ( "MeasTimeScale" );
	String Statistic = props.getValue ( "Statistic" );

	if ( OutputFile == null ) {
		message = "The output file for the report is not specified.";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}

	if ( MeasTimeScale == null ) {
		message = "The measurement time scale must be specified as " +
			"Accm, Mean, or Inst.";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}

	if ( Statistic == null ) {
		message = "The statistic time must be specified as Mean or" +
			" Sum";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}

	// Loop through the time series and convert to yearly...  Do it the
	// brute force way right now...

	int nts = getTimeSeriesSize();
	Vector yts_Vector = new Vector(nts);
	YearTS yts = null;
	TS ts = null;
	TSIdent tsident = null;
	DateTime datetime, datetime2;
	double total = 0.0, value = 0.0;
	int count = 0;
	for ( int its = 0; its < nts; its++ ) {
		ts = getTimeSeries(its);	// Will throw Exception
		if ( ts.getDataIntervalBase() == TimeInterval.YEAR ) {
			// Just add to the list...
			yts = (YearTS)ts;
		}
		else if ( ts.getDataIntervalBase() == TimeInterval.MONTH ) {
			// Create a new time series and accumulate...
			tsident = new TSIdent ( ts.getIdentifier() );
			tsident.setInterval ( "Year" );
			yts = new YearTS ();
			yts.setIdentifier ( tsident );
			yts.setDescription ( ts.getDescription() );
			yts.setDate1 ( ts.getDate1() );
			yts.setDate2 ( ts.getDate2() );
			yts.setDataUnits ( ts.getDataUnits() );
			yts.allocateDataSpace();
			datetime = new DateTime(ts.getDate1());
			// Accumulate in calendar time...
			datetime.setMonth(1);
			datetime2 = ts.getDate2();
			datetime2.setMonth ( 12 );
			for ( ; datetime.lessThanOrEqualTo(datetime2);
				datetime.addMonth(1) ) {
				value = ts.getDataValue(datetime);
				if ( !ts.isDataMissing(value) ) {
					total += value;
					++count;
				}
				if ( datetime.getMonth() == 12 ) {
					// Transfer to year time series only if
					// all data are available in month...
					if ( count == 12 ) {
						if (	MeasTimeScale.
							equalsIgnoreCase(
							"Mean") ) {
							yts.setDataValue(
							datetime,
							total/(double)count);
						}
						else if ( MeasTimeScale.
							equalsIgnoreCase(
							"Accm") ) {
							yts.setDataValue(
							datetime, total);
						}
					}
					// Reset the accumulators...
					total = 0.0;
					count = 0;
				}
			}
		}
		// Add to the list...
		yts_Vector.addElement ( yts );
	}
	// Now open the output file and average all the values...
	PrintWriter out = null;
	String full_filename = IOUtil.getPathUsingWorkingDir ( OutputFile );
	out = new PrintWriter(new FileOutputStream(full_filename));
	IOUtil.printCreatorHeader ( out, "#", 80, 0 );
	int size = yts_Vector.size();
	double statistic_val;
	double [] yts_data;
	for ( int i = 0; i < size; i++ ) {
		yts = (YearTS)yts_Vector.elementAt(i);
		yts_data = TSUtil.toArray ( yts, null, null );
		statistic_val = -999.0;
		if ( Statistic.equalsIgnoreCase("Mean") ) {
			statistic_val = MathUtil.mean ( yts_data.length,
				yts_data, yts.getMissing() );
		}
		else if ( Statistic.equalsIgnoreCase("Sum") ) {
			statistic_val = MathUtil.sum ( yts_data.length,
				yts_data, yts.getMissing() );
		}
		out.println("\"" + yts.getLocation() + "\",\"" +
			yts.getDescription() + "\"," +
			StringUtil.formatString(statistic_val,"%.6f") );
	}
	out.flush();
	out.close();
	// If the time series output file was specified, write out the time
	// series that were analyzed...
	if ( TSOutputFile != null ) {
		DateValueTS.writeTimeSeriesList ( yts_Vector,
			IOUtil.getPathUsingWorkingDir ( TSOutputFile ),
			null, null, null, true );
	}
}

/**
Execute the DateTime x = xxxxx command.
@param command Command to parse.
@exception Exception if there is an error.
*/
private void do_DateTime ( String command )
throws Exception
{	String routine = "TSEngine.doDateTime";
	Vector tokens = StringUtil.breakStringList ( command,
		" =\t", StringUtil.DELIM_SKIP_BLANKS );
	if ( tokens.size() < 3 ) {
		throw new Exception ( "Bad command \"" + command + "\"" );
	}
	// Set the date...
	try {	__datetime_Hashtable.put ( (String)tokens.elementAt(1),
			(Object)DateTime.parse((String)tokens.elementAt(2)));
	}
	catch ( Exception e ) {
		Message.printWarning ( 1, routine,
		"Unable to set date using \"" + command + "\"" );
		Message.printWarning ( 2, routine, e );
	}
}

/**
Helper method for fillFromTS() command.
@param command_tag Command number used for messaging.
@param command Command being evaluated.
@exception Exception if there is an error processing the time series.
*/
private TS do_fillFromTS ( String command_tag, String command )
throws Exception
{	// Don't parse with spaces because a TEMPTS may be present.
	Vector v = StringUtil.breakStringList(command,
		"(),\t", StringUtil.DELIM_SKIP_BLANKS); 
	String message, routine = "TSEngine.do_fillFromTS";
	if ( (v == null) || (v.size() < 5) ) {
		message = "Syntax error in \"" + command + "\"";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}
	// Get the individual tokens of the expression...
	String dependent = ((String)v.elementAt(1)).trim();
	String independent = ((String)v.elementAt(2)).trim();
	String analysis_period_start_string = ((String)v.elementAt(3)).trim();
	String analysis_period_end_string = ((String)v.elementAt(4)).trim();
	v = null;
	// Make sure there are time series available to operate on...
	int ts_pos = indexOf ( dependent );
	TS dependentTS = getTimeSeries ( ts_pos );
	if ( dependentTS == null ) {
		message = "Unable to find time series \"" + dependent +
		"\" for fillFromTS().";
		Message.printWarning ( 1, routine, message );
		throw new Exception ( message );
	}
	// The independent identifier may or may not have TEMPTS at the front
	// but is handled by getTimeSeries...
	TS independentTS = getTimeSeries ( command_tag, independent );
	if ( independentTS == null ) {
		message = "Unable to find time series \"" + independent +
				"\" for fillFromTS().";
		Message.printWarning ( 1, routine, message );
		throw new Exception ( message );
	}

	DateTime analysis_period_start =
		getDateTime(analysis_period_start_string);
	DateTime analysis_period_end = getDateTime(analysis_period_end_string);
	// Fill the dependent time series for the analysis period...
	try {	TSUtil.fillFromTS (	dependentTS, independentTS,
					analysis_period_start,
					analysis_period_end );
		processTimeSeriesAction ( UPDATE_TS, dependentTS, ts_pos );
	}
	catch ( Exception e ) {
		message = "Error executing command: \"" + command + "\"";
		Message.printWarning ( 2, routine, message );
		Message.printWarning ( 2, routine, e );
		throw new Exception ( message );
	}
	return dependentTS;
}

/**
Helper method to execute the fillMixedStation() command.
@param command_tag Command number used for messaging.
@param command Command to process.
@exception Exception if there is an error processing the command.
*/
/* TODO SAM 2005-05-27 Delete when in the command class
private void do_fillMixedStation ( String command_tag, String command )
throws Exception
{	String message, routine = "TSEngine.do_fillMixedStation";
	Vector tokens = StringUtil.breakStringList ( command,
		"()", StringUtil.DELIM_SKIP_BLANKS );
	if ( (tokens == null) || tokens.size() < 2 ) {
		// Must have at least the command name and a TSID...
		message = "Bad command \"" + command + "\"";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}
	// Get the input needed to process the file...
	PropList props = PropList.parse (
		(String)tokens.elementAt(1), "fillMixedStation","," );
	String DependentTSList = props.getValue ( "DependentTSList" );
	String DependentTSID = props.getValue ( "DependentTSID" );
	String IndependentTSList = props.getValue ( "IndependentTSList" );
	String IndependentTSID = props.getValue ( "IndependentTSID" );

	if ( DependentTSList == null ) {
		DependentTSList = "AllTS";
	}
	if ( IndependentTSList == null ) {
		IndependentTSList = "AllTS";
	}

	// Most of the work is done by the MixedStationAnalysis object.
	// Determine the dependent and independent time series lists and pass
	// in all the properties...

	try {	Vector dependent_tslist = null;
		Vector independent_tslist = null;
		// Get the list of dependent time series to process...
		if (	DependentTSList.equalsIgnoreCase("AllTS") ||
			((DependentTSID != null) && DependentTSID.equals("*"))){
			// All the time series...
			dependent_tslist = getTimeSeriesList(null);
		}
		else if ( DependentTSList.equalsIgnoreCase("SelectedTS") ) {
			// Get the selected time series...
			dependent_tslist = getSelectedTimeSeries( false );
		}
		else if ( DependentTSList.equalsIgnoreCase("SpecifiedTS") ) {
			// Get the specified matching time series...
			dependent_tslist = getSpecifiedTimeSeries(
				command_tag,
				StringUtil.breakStringList(DependentTSID,
				",",StringUtil.DELIM_ALLOW_STRINGS),
				routine, command );
			// TODO SAM 2005-04-12 how should errors be handled?
		}
		// Get the list of independent time series to process...
		if (	IndependentTSList.equalsIgnoreCase("AllTS") ||
			((IndependentTSID != null) &&
			IndependentTSID.equals("*"))){
			// All the time series...
			independent_tslist = getTimeSeriesList(null);
		}
		else if ( IndependentTSList.equalsIgnoreCase("SelectedTS") ) {
			// Get the selected time series...
			independent_tslist = getSelectedTimeSeries( false );
		}
		else if ( IndependentTSList.equalsIgnoreCase("SpecifiedTS") ) {
			// Get the specified matching time series...
			independent_tslist = getSpecifiedTimeSeries(
				command_tag,
				StringUtil.breakStringList(IndependentTSID,
				",",StringUtil.DELIM_ALLOW_STRINGS),
				routine, command );
			// TODO SAM 2005-04-12 how should errors be handled?
		}
		// Create the analysis object and analyze...
		MixedStationAnalysis msa = new MixedStationAnalysis (
			dependent_tslist, independent_tslist, props );
		// TODO SAM 2005-04-12
		// Separate method to fill??
	}
	catch ( Exception e ) {
		message =
		"There were warnings performing the mixed station analysis.";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}
}
*/

/**
Helper method to execute the fillPattern() command:
<pre>
New:

fillpattern(TSList=X,TSID="X",PatternID="X")

Old:

fillpattern(TSID,pattern)
fillpattern(*,pattern)
</pre>
@param command Command to process.
@exception Exception if there is an error processing the command.
*/
private void do_fillPattern ( String command )
throws Exception
{	String routine = "TSEngine.do_fillPattern", message;
	TS ts;			// Time series to fill
	String TSList = __AllMatchingTSID;	// Defaults..
	String TSID = "*";
	String PatternID = null;
	if ( command.indexOf('=') < 0 ) {
		// Old command syntax...
		Vector tokens = StringUtil.breakStringList ( command,
				"() ,", StringUtil.DELIM_SKIP_BLANKS );
		if ( (tokens == null) || (tokens.size() != 3) ) {
			throw new Exception ( "Bad command: \"" + command+"\"");
		}
		TSID = ((String)tokens.elementAt(1)).trim();
		PatternID = ((String)tokens.elementAt(2)).trim();
		if ( TSID.indexOf("*") >= 0 ) {	// Default in new syntax
			TSList = __AllMatchingTSID;
		}
		else {	TSList = __LastMatchingTSID;
		}
	}
	else {	// New syntax...
		Vector tokens = StringUtil.breakStringList ( command,
			"()", StringUtil.DELIM_SKIP_BLANKS );
		if ( (tokens == null) || (tokens.size() < 2) ) {
			// Should never happen because the command name was
			// parsed before...
			throw new Exception("Bad command: \"" + command + "\"");
		}
		// Get the input needed to process the file...
		PropList props = PropList.parse (
			(String)tokens.elementAt(1), routine, "," );
		TSList = props.getValue ( "TSList" );
		TSID = props.getValue ( "TSID" );
		PatternID = props.getValue ( "PatternID" );
	}
	if ( !isParameterValid("TSList",TSList) ) {
		message = "TSList value \"" + TSList + "\" is not valid.";
		Message.printWarning ( 2, routine, message );
		throw new Exception("Invalid TSList parameter: \"" + command +
		"\"");
	}
	Message.printStatus ( 2, routine, "Filling with pattern \"" + PatternID
	+ "\" for TSList=\"" + TSList + "\" TSID=\"" + TSID + "\"" );
	int warning_count = 0;
	// Get the pattern time series to use...
	StringMonthTS patternts = searchForFillPatternTS ( PatternID );
	if ( patternts == null ) {
		++warning_count;
		message = "Unable to find fill pattern time series \"" + PatternID + "\" to fill time series.";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}
	// Properties used when filling the time series...
	PropList fillprops = new PropList ( "fillprops" );
	if ( getIgnoreLEZero() ) {
		fillprops.set ( "IgnoreLessThanOrEqualZero", "true" );
	}
	// Get the time series to process...
	Vector v = getTimeSeriesToProcess ( TSList, TSID );
	Vector tslist = (Vector)v.elementAt(0);
	int [] tspos = (int [])v.elementAt(1);
	int nts = tslist.size();
	if ( nts == 0 ) {
		++warning_count;
		message = "Unable to find time series to fill using TSID \"" + TSID + "\".";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}
	int pos = 0;
	for ( int its = 0; its < nts; its++ ) {
		try {	ts = getTimeSeries(pos = tspos[its]);
		}
		catch ( Exception e ) {
			continue;
		}
		// Do the filling...
		Message.printStatus ( 2, routine, "Filling \"" +
		ts.getIdentifier()+"\" with pattern \"" + PatternID + "\"" );
		try {	TSUtil.fillPattern ( ts, patternts, fillprops );
			processTimeSeriesAction ( UPDATE_TS, ts, pos );
		}
		catch ( Exception e ) {
			message = "Unable to fill time series \""+
				ts.getIdentifier() + "\" with "+"pattern ID \""+
				 PatternID + "\".";
			++warning_count;
			Message.printWarning(2,routine,message);
		}
	}
	patternts = null;
	fillprops = null;
	if ( warning_count > 0 ) {
		throw new Exception (
		"One or more warnings ocurred processing command \"" +
			command + "\"" );
	}
}

/**
Helper method to execute the fillProrate() command.
@param command Command to process.
@exception Exception if there is an error processing the command.
*/
private void do_fillProrate ( String command )
throws Exception
{	String message, routine = "TSEngine.do_fillProrate";
	Vector tokens = StringUtil.breakStringList ( command,
		"()", StringUtil.DELIM_SKIP_BLANKS );
	if ( (tokens == null) || tokens.size() < 2 ) {
		// Must have at least the command name and a TSID...
		message = "Bad command \"" + command + "\"";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}
	// Get the input needed to process the file...
	PropList props = PropList.parse (
		(String)tokens.elementAt(1), "fillProrate","," );
	String TSID = props.getValue ( "TSID" );
	String IndependentTSID = props.getValue ( "IndependentTSID" );
	String FillStart = props.getValue ( "FillStart" );
	String FillEnd = props.getValue ( "FillEnd" );
	//String CalculateFactorHow = props.getValue ( "CalculateFactorHow" );
	String AnalysisStart = props.getValue ( "AnalysisStart" );
	String AnalysisEnd = props.getValue ( "AnalysisEnd" );
	String InitialValue = props.getValue ( "InitialValue" );
	String FillDirection = props.getValue ( "FillDirection" );
	//String FillFlag = props.getValue ( "FillFlag" );

	// The props are passed on to some methods...

	DateTime start = null;
	DateTime end = null;
	if ( FillStart != null ) {
		try {	start = DateTime.parse(FillStart);
		}
		catch ( Exception e ) {
			message =
			"Fill start is not a valid date/time...ignoring.";
			Message.printWarning ( 2, routine, message );
			throw new Exception ( message );
		}
	}
	if ( FillEnd != null ) {
		try {	end = DateTime.parse(FillEnd);
		}
		catch ( Exception e ) {
			message =
			"Fill end is not a valid date/time...ignoring.";
			Message.printWarning ( 1, routine, message );
			throw new Exception ( message );
		}
	}
	//DateTime AnalysisStart_DateTime = null;
	//DateTime AnalysisEnd_DateTime = null;
	if ( AnalysisStart != null ) {
		try {	start = DateTime.parse(AnalysisStart);
		}
		catch ( Exception e ) {
			message =
			"Analysis start is not a valid date/time...ignoring.";
			Message.printWarning ( 2, routine, message );
			throw new Exception ( message );
		}
	}
	if ( AnalysisEnd != null ) {
		try {	end = DateTime.parse(AnalysisEnd);
		}
		catch ( Exception e ) {
			message =
			"Analysis end is not a valid date/time...ignoring.";
			Message.printWarning ( 1, routine, message );
			throw new Exception ( message );
		}
	}
	// Set defaults if not specified...
	if ( InitialValue != null ) {
		if (	!InitialValue.equalsIgnoreCase("NearestBackward") &&
			!InitialValue.equalsIgnoreCase("NearestForeward") &&
			!StringUtil.isDouble(InitialValue) ) {
			message = "InitialValue \"" + InitialValue +
				"\" must be a number, NearestBackward " +
				"or NearestForeward.";
			Message.printWarning ( 2, routine, message );
			throw new Exception ( message );
		}
	}
	if ( FillDirection == null ) {
		FillDirection = "Forward";
	}
	if ( 	!FillDirection.equalsIgnoreCase("Forward") &&
		!FillDirection.equalsIgnoreCase("Backward") ) {
		message = "FillDirection \"" + FillDirection +
			"\" is not an Forward or Backward.";
		Message.printWarning ( 2, routine, message );
	}
	// Get the independent time series...
	int ts_pos = indexOf ( IndependentTSID );
	if ( ts_pos < 0 ) {
		message = "Unable to find independent time series \"" +
		IndependentTSID + "\".  Cannot fill.";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}
	TS independent_ts = getTimeSeries ( ts_pos );
	TS ts = null;	// Time series instance to update
	if ( TSID.equals("*") ) {
		// Fill everything in memory...
		int nts = getTimeSeriesSize();
		for ( int its = 0; its < nts; its++ ) {
			ts = getTimeSeries(its);	// Will throw Exception
			// Do not fill the independent time series...
			if ( ts == independent_ts ) {
				continue;
			}
			if ( InitialValue == null ) {
				TSUtil.fillProrate (
				ts, independent_ts, start, end, props );
			}
			else {	TSUtil.fillProrate (
				ts, independent_ts, start, end, props );
			}
			processTimeSeriesAction ( UPDATE_TS, ts, its );
		}
	}
	else {	// Fill one time series...
		ts_pos = indexOf ( TSID );
		if ( ts_pos >= 0 ) {
			ts = getTimeSeries ( ts_pos );
			if ( InitialValue == null ) {
				TSUtil.fillProrate (
				ts, independent_ts, start, end, props );
			}
			else {	TSUtil.fillProrate (
				ts, independent_ts, start, end, props );
			}
			processTimeSeriesAction ( UPDATE_TS, ts, ts_pos );
		}
		else {	message = "Unable to find time series to fill \"" +
				TSID + "\".  Cannot fill.";
			Message.printWarning ( 2, routine, message );
			throw new Exception ( message );
		}
	}
	tokens = null;
}

// TODO SAM 2005-05-19 Remove when MOVE2 code is transferred to a command.
// Currently fillRegression is run in a separate command class but the MOVE2
// code is used below.
/**
Helper method for old and new style regression, and also MOVE1, and MOVE2.
@param command_string Command being evaluated.
@param ts_to_fill time series to fill.
@param ts_independent independent time series.
@param props Properties to use for regression, passed to the TSRegression
command.  In particular, the FillPeriodStart and FillPeriodEnd
propeties can be set to define the analysis period for filling.
@exception Exception if there is an error processing the time series.
*/
/*
private TS do_fillRegression ( String command_string )
throws Exception
{	String routine = "TSEngine.do_fillRegression";
	Command command = TSCommandFactory ( command_string ); 
	if (	(command_string.indexOf('=') < 0) ||
		(StringUtil.patternCount(command_string,"=") == 1) ) {
		// Old fixed-format syntax, which below has "new" style for
		// the case where Intercept=X is included at the end.
			// New-style regression...
			// Parse up front.  Don't parse with spaces because a
			// TEMPTS may be present.
			Vector v = StringUtil.breakStringList(expression,
				"(),\t", StringUtil.DELIM_SKIP_BLANKS |
				StringUtil.DELIM_ALLOW_STRINGS );
			int ntokens = 0;
			if ( v != null ) {
				ntokens = v.size();
			}
			if ( ntokens < 5 ) {
				Message.printWarning ( 2, routine,
				"Syntax error in \"" + expression + "\"" );
				++error_count;
				v = null;
				continue;
			}

			// Get the individual tokens of the expression...

			int ic = 0;
			String command = ((String)v.elementAt(ic++)).trim();
			alias = ((String)v.elementAt(ic++)).trim();
			String independent = ((String)v.elementAt(ic++)).trim();
			String num_equations=((String)v.elementAt(ic++)).trim();
			String transformation =
				((String)v.elementAt(ic++)).trim();
			String dep_analysis_period_start = "*";
			String intercept = null;
			String dep_analysis_period_end = "*";
			String ind_analysis_period_start = "*";
			String ind_analysis_period_end = "*";
			String fill_period_start = "*";	// All available
			String fill_period_end = "*";	// All available
			if ( command.equalsIgnoreCase("fillMOVE2") ) {
				// Have dependent and independent analysis
				// periods...
				dep_analysis_period_start =
					((String)v.elementAt(ic++)).trim();
				dep_analysis_period_end =
					((String)v.elementAt(ic++)).trim();
				ind_analysis_period_start =
					((String)v.elementAt(ic++)).trim();
				ind_analysis_period_end =
					((String)v.elementAt(ic++)).trim();
			}
			else {	// fillMOVE1() and fillRegression() may have
				// the dependent analysis period.  If they
				// have that they will also have the fill period
				// below...
				int icmax = ic + 1;
				if ( ntokens >= icmax ) {
					dep_analysis_period_start =
					((String)v.elementAt(ic++)).trim();
				}
				if ( ntokens >= (icmax + 1) ) {
					dep_analysis_period_end =
					((String)v.elementAt(ic++)).trim();
				}
			}
			// All others have the fill period...
			int icmax = ic + 1;
			if ( ntokens >= icmax ) {
				fill_period_start =
					((String)v.elementAt(ic++)).trim();
			}
			if ( ntokens >= (icmax + 1) ) {
				fill_period_end =
					((String)v.elementAt(ic++)).trim();
			}

			// Check for new-style properties...

			String token, token0;
			Vector v2;
			for ( ic = 0; ic < ntokens; ic++ ) {
				// Check for an '=' in the token...
				token = (String)v.elementAt(ic);
				if ( token.indexOf('=') < 0 ) {
					continue;
				}
				v2 = StringUtil.breakStringList (
					token, "=", 0 );
				if ( v2.size() < 2 ) {
					continue;
				}
				token0 = ((String)v2.elementAt(0)).trim();
				if ( token0.equalsIgnoreCase("Intercept") ) {
					intercept =
					((String)v2.elementAt(1)).trim();
				}
			}
			v = null;

			// Check for regression date override...

			if ( command.equalsIgnoreCase("fillRegression") ) {
				// Set the fill and analysis period if not
				// specified and the regression period is not
				// null...
				if (	(dep_analysis_period_start.equals("*")||
					dep_analysis_period_start.equals(""))&&
					_regression_date1 != null ) {
					dep_analysis_period_start =
					_regression_date1.toString();
				}
				if (	(dep_analysis_period_end.equals("*") ||
					dep_analysis_period_end.equals(""))&&
					_regression_date2 != null ) {
					dep_analysis_period_end =
					_regression_date2.toString();
				}
				if (	(fill_period_start.equals("*") ||
					fill_period_start.equals(""))&&
					_regression_date1 != null ) {
					fill_period_start =
					_regression_date1.toString();
				}
				if (	(fill_period_end.equals("*") ||
					fill_period_end.equals(""))&&
					_regression_date2 != null ) {
					fill_period_end =
					_regression_date2.toString();
				}
			}

			// Make sure there are time series available to
			// operate on...

			ts_pos = indexOf ( alias );
			TS firstTS = getTimeSeries ( ts_pos );
			if ( firstTS == null ) {
				Message.printWarning ( 1, routine,
				"Unable to find time series \"" + alias +
				"\" for " + command + "()." );
				++error_count;
				continue;
			}
			// The independent identifier may or may not have
			// TEMPTS at the front but is handled by getTimeSeries
			TS secondTS = getTimeSeries ( independent );
			if ( secondTS == null ) {
				Message.printWarning ( 1, routine,
				"Unable to find time series \"" + independent +
				"\" for " + command + "()." );
				++error_count;
				continue;
			}

			// Now set the fill properties...

			PropList props = new PropList ( "TS Analysis" );
			if (	num_equations.equalsIgnoreCase(
				"MonthlyEquations") ||
				num_equations.equalsIgnoreCase("OneEquation")) {
				props.set ( "NumberOfEquations", num_equations);
			}
			else {	Message.printWarning ( 1, routine,
				"Bad number of equations \"" + num_equations +
				"\" for \"" + expression + "\"" );
				++error_count;
				continue;
			}

			if (	transformation.equalsIgnoreCase( "Log") ||
				transformation.equalsIgnoreCase( "Linear") ||
				transformation.equalsIgnoreCase( "None") ){
				props.set ( "Transformation", transformation );
			}
			else {	Message.printWarning ( 1, routine,
				"Bad transformation type \"" + transformation +
				"\" for \"" + expression + "\"" );
				++error_count;
				continue;
			}

			// Check whether the MOVE2 algorithm should be used...

			if ( command.equalsIgnoreCase("fillMOVE1") ) {
				props.set ( "AnalysisMethod", "MOVE1" );
			}
			else if ( command.equalsIgnoreCase("fillMOVE2") ) {
				props.set ( "AnalysisMethod", "MOVE2" );
			}
			else {	// Ordinary least squares...
				props.set ( "AnalysisMethod", "OLSRegression" );
			}

			// Indicate that the analysis is being done for filling
			// (this property is used in the TSRegression class).
			// The default for TSRegression to compute RMSE to
			// compare time series (e.g., for calibration).  If
			// filling is indicated, then RMSE is computed from the
			// dependent and the estimated dependent...

			props.set ( "AnalyzeForFilling", "true" );

			// Set the analysis/fill periods...

			//if ( command.equalsIgnoreCase("fillMOVE2") ) {
				props.set ( "DependentAnalysisPeriodStart=" +
					dep_analysis_period_start );
				props.set ( "DependentAnalysisPeriodEnd=" +
					dep_analysis_period_end );
				props.set ( "IndependentAnalysisPeriodStart=" +
					ind_analysis_period_start );
				props.set ( "IndependentAnalysisPeriodEnd=" +
					ind_analysis_period_end );
			//}
			props.set ( "FillPeriodStart=" + fill_period_start );
			props.set ( "FillPeriodEnd=" + fill_period_end );
			if ( intercept != null ) {
				props.set ( "Intercept=" + intercept );
			}

			// Call the code that is used by both the old and new
			// version...

			ts = do_fillRegression (expression, firstTS,
						secondTS, props );
			ts_action = UPDATE_TS;
	// Figure out the dates to use for the analysis...
	DateTime fill_period_start = null;
	DateTime fill_period_end = null;
	String prop_val = props.getValue ( "FillPeriodStart" );
	if ( prop_val != null ) {
		try {	fill_period_start = getDateTime ( prop_val );
		}
		catch ( Exception e ) {
			fill_period_start = null;
		}
	}
	prop_val = props.getValue ( "FillPeriodEnd" );
	if ( prop_val != null ) {
		try {	fill_period_end = getDateTime ( prop_val );
		}
		catch ( Exception e ) {
			fill_period_end = null;
		}
	}
	// Fill the dependent time series...
	try {	TSRegression regress_results = TSUtil.fillRegress ( 
			ts_to_fill, ts_independent, fill_period_start,
			fill_period_end, props );
		// Print the results to the log file...
		if ( regress_results != null ) {
			Message.printStatus ( 1, routine,
			"Analysis results are..." );
			Message.printStatus ( 1, routine,
			regress_results.toString() );
		}
		else {	String message =
			"Null regression results for \"" + expression + "\"";
			Message.printWarning ( 2, routine, message );
			throw new Exception ( message );
		}
	}
	catch ( Exception e ) {
		String message =
		"Error performing regression for \"" + expression + "\"";
		Message.printWarning ( 2, routine, message );
		Message.printWarning ( 2, routine, e );
		throw new Exception ( message );
	}
	return ts_to_fill;
	return null;
}
*/

/**
Helper method to execute the fillRepeat() command.
@param command Command to process.
@exception Exception if there is an error processing the command.
*/
private void do_fillRepeat ( String command )
throws Exception
{	String message, routine = "TSEngine.do_fillRepeat";
	Vector tokens = StringUtil.breakStringList ( command,
		"()", StringUtil.DELIM_SKIP_BLANKS );
	if ( (tokens == null) || tokens.size() < 2 ) {
		// Must have at least the command name and a TSID...
		message = "Bad command \"" + command + "\"";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}
	// Get the input needed to process the file...
	PropList props = PropList.parse (
		(String)tokens.elementAt(1), "fillRepeat","," );
	String TSID = props.getValue ( "TSID" );
	String FillStart = props.getValue ( "FillStart" );
	String FillEnd = props.getValue ( "FillEnd" );
	String MaxIntervals = props.getValue ( "MaxIntervals" );
	String FillDirection = props.getValue ( "FillDirection" );
	DateTime start = null;
	DateTime end = null;
	try {	if ( FillStart != null ) {
			start = DateTime.parse(FillStart);
		}
	}
	catch ( Exception e ) {
		message = "Fill start is not a valid date/time...ignoring.";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}
	try {	if ( FillEnd != null ) {
			end = DateTime.parse(FillEnd);
		}
	}
	catch ( Exception e ) {
		message = "Fill end is not a valid date/time...ignoring.";
		Message.printWarning ( 1, routine, message );
		throw new Exception ( message );
	}
	// Set defaults if not specified...
	if ( MaxIntervals == null ) {
		MaxIntervals = "0";
	}
	if ( !StringUtil.isInteger(MaxIntervals) ) {
		message = "MaxIntervals \"" + MaxIntervals +
			"\" is not an integer.";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}
	int imax = StringUtil.atoi ( MaxIntervals );
	if ( FillDirection == null ) {
		FillDirection = "Forward";
	}
	if ( 	!FillDirection.equalsIgnoreCase("Forward") &&
		!FillDirection.equalsIgnoreCase("Backward") ) {
		message = "FillDirection \"" + FillDirection +
			"\" is not an Forward or Backward.";
		Message.printWarning ( 2, routine, message );
	}
	int idir = 1;	// Forward
	if ( FillDirection.equalsIgnoreCase("Backward") ) {
		idir = -1;
	}
	TS ts = null;	// Time series instance to update
	if ( TSID.equals("*") ) {
		// Fill everything in memory...
		int nts = getTimeSeriesSize();
		for ( int its = 0; its < nts; its++ ) {
			ts = getTimeSeries(its);
			TSUtil.fillRepeat ( ts, start, end, idir, imax );
			processTimeSeriesAction ( UPDATE_TS, ts, its );
		}
	}
	else {	// Fill one time series...
		int ts_pos = indexOf ( TSID );
		if ( ts_pos >= 0 ) {
			ts = getTimeSeries ( ts_pos );
			TSUtil.fillRepeat ( ts, start, end, idir, imax );
			processTimeSeriesAction ( UPDATE_TS, ts, ts_pos );
		}
		else {	message = "Unable to find time series \"" +
				TSID + "\" for fillRepeat() command.";
			Message.printWarning ( 2, routine, message );
			throw new Exception ( message );
		}
	}
	tokens = null;
}

/**
Execute the newEndOfMonthTSFromDayTS() command:
<pre>
TS Alias = newEndOfMonthTSFromDayTS(TSID,Days)
</pre>
@param command Command to parse.
@exception Exception if there is an error.
*/
private TS do_newEndOfMonthTSFromDayTS ( String command )
throws Exception
{	String routine = "TSEngine.do_newEndOfMonthTSFromDayTS";
	// Reparse to strip quotes from file name...
	Vector tokens = StringUtil.breakStringList ( command, "=(,)",
			StringUtil.DELIM_ALLOW_STRINGS);
	String tsid = ((String)tokens.elementAt(2)).trim();
	String ndays = ((String)tokens.elementAt(3)).trim();

	// Locate the input daily time series...

	int ts_pos = indexOf ( tsid );
	TS dayts = getTimeSeries ( ts_pos );
	if ( dayts == null ) {
		String message = "Unable to find time series \"" + tsid +
		"\" for newEndOfMonthTSFromDayTS().";
		Message.printWarning ( 1, routine, message );
		throw new Exception ( message );
	}

	// Now have a daily time series... Convert to monthly using the nearest
	// values...
	PropList changeprops = new PropList ( "newEndOfMonthTSFromDayTS" );
	changeprops.set ( "UseNearestToEnd", ndays );
	Message.printStatus ( 1, routine, "Changing interval..." );
	MonthTS monthts = (MonthTS)TSUtil.changeInterval ( dayts, TimeInterval.MONTH, 1, changeprops );
	// Save the original data limits...
	if ( needHistoricalAverages(monthts) ) {
		try {	monthts.setDataLimitsOriginal (
				calculateTSAverageLimits(monthts) );
		}
		catch ( Exception e ) {
			// TODO SAM 2006-03-27
			// Make the messages more friendly by checking for
			// nulls.
			Message.printWarning ( 2, routine,
			"Error getting original data limits for \"" +
			monthts.getIdentifierString() + "\""  );
			Message.printWarning ( 3, routine, e );
		}
	}
	changeprops = null;
	return monthts;
}

/**
Execute the TS Alias = readMODSIM() command:
<pre>
TS Alias = readMODSIM(file,TSID,units,start,end)
</pre>
@param command Command to parse.
@exception Exception if there is an error.
*/
private TS do_readMODSIM ( String command_tag, String command_string, GenericCommand command )
throws Exception
{	String routine = "TSEngine.do_readMODSIM";
    String message;
    int warning_level = 2;
    int warning_count = 0;

    CommandStatus status = command.getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);

	// Reparse to strip quotes from file name...
	Vector tokens = StringUtil.breakStringList ( command_string, "=(,)",
			StringUtil.DELIM_ALLOW_STRINGS);
	String infile = ((String)tokens.elementAt(2)).trim();
    String infile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(__ts_processor),infile) );
	String tsid = ((String)tokens.elementAt(3)).trim();
	//String units = ((String)tokens.elementAt(4)).trim();
	String date1_string = ((String)tokens.elementAt(5)).trim();
	String date2_string = ((String)tokens.elementAt(6)).trim();
	DateTime query_date1 = getDateTime ( date1_string );
	DateTime query_date2 = getDateTime ( date2_string );
	Message.printStatus ( 1, routine, "Reading MODSIM file \"" + infile_full + "\"" );
	TS ts = null;
	try {
        ts = ModsimTS.readTimeSeries ( tsid, infile_full, query_date1, query_date2, null, true );
		// Now post-process...
		readTimeSeries2 ( ts, null, true );
	}
	catch ( Exception e ) {
		message = "Error reading MODSIM file \"" + infile_full + "\".";
		Message.printWarning ( 3, routine, e );
           Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag(command_tag,
                    ++warning_count), routine, message );
            Message.printWarning(3, routine, e);
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that the file is a valid MODSIM time series file." ) );
            throw new CommandException ( message );
	}
	if ( ts != null ) {
		// Set the alias...
		ts.setAlias ( ((String)tokens.elementAt(2)).trim() );
	}
    status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
	return ts;
}

/**
Execute the readNWSRFSESPTraceEnsemble() command:
<pre>
readNWSRFSESPTraceEnsemble(InputFile="X")
</pre>
@param command Command to parse.
@exception Exception if there is an error.
*/
private void do_readNWSRFSESPTraceEnsemble ( String command )
throws Exception
{	String routine = "TSEngine.do_readNWSRFSESPTraceEnsemble";
	Vector tokens = StringUtil.breakStringList ( command, "()", StringUtil.DELIM_SKIP_BLANKS );
	if ( (tokens == null) || (tokens.size() < 1) ) {
		// Should never happen because the command name was parsed before...
		throw new Exception ( "Bad command: \"" + command + "\"" );
	}
	if ( !IOUtil.classCanBeLoaded("RTi.DMI.NWSRFS_DMI.NWSRFS_ESPTraceEnsemble") ) {
		Message.printWarning ( 2, routine,
		"Features for command are unavailable:  \"" + command + "\"" );
		throw new Exception ( "Command unavailable: \"" + command +	"\"" );
	}
	// Get the input needed to process the file...
	PropList props = PropList.parse ((String)tokens.elementAt(1), routine, "," );
	String InputFile = props.getValue ( "InputFile" );

	Vector tslist = null;
	Message.printStatus ( 1, routine, "Reading NWSRFS ESPTraceEnsemble file \"" + InputFile + "\"" );
	// TODO - need to pass requested date, units, etc. to the constructor??
    String InputFile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(__ts_processor),InputFile));
	NWSRFS_ESPTraceEnsemble ensemble = new NWSRFS_ESPTraceEnsemble ( InputFile_full, true );
	tslist = ensemble.getTimeSeriesVector ();
	// Add the time series to the end of the normal list...
	if ( tslist != null ) {
		// Further process the time series...
		// This makes sure the period is at least that of the output period...
		int vsize = tslist.size();
		Message.printStatus ( 1, routine, "Read " + vsize + " NWSRFS ESPTraceEnsemble time series" );
		readTimeSeries2 ( tslist, true );
		int ts_pos = getTimeSeriesSize();
		for ( int iv = 0; iv < vsize; iv++ ) {
			setTimeSeries ((TS)tslist.elementAt(iv), (ts_pos + iv));
		}
	}
	// Free resources from ESP list...
	tslist = null;
	// Force a garbage collect because this is an intensive task...
	System.gc();
}

/**
Execute the readNWSRFSFS5Files() commands:
<pre>
readNWSRFSFS5Files(TSID="x",QueryStart="X",QueryEnd="X",Units="X")
</pre>
@param command Command to parse.
@exception Exception if there is an error.
*/
private void do_readNWSRFSFS5Files ( String command )
throws Exception
{	//String routine = "TSEngine.do_readNWSRFSFS5Files";
	Vector tokens = StringUtil.breakStringList ( command,
			"()", StringUtil.DELIM_SKIP_BLANKS );
	if ( (tokens == null) || (tokens.size() < 2) ) {
		// Should never happen because the command name was parsed
		// before...
		throw new Exception ( "Bad command: \"" + command + "\"" );
	}
	// Get the input needed to process the file...
	//PropList props = PropList.parse (
	//	(String)tokens.elementAt(1), routine, "," );
	//String TSID = props.getValue ( "TSList" );
	//String QueryStart = props.getValue ( "QueryStart" );
	//String QueryEnd = props.getValue ( "QueryEnd" );
	//String Units = props.getValue ( "Units" );

	/* TODO SAM 2004-09-11 need to enable
	DateTime QueryStart_DateTime = getDateTime ( QueryStart );
	if ( QueryStart_DateTime == null ) {
		QueryStart_DateTime = __query_date1;
	}
	DateTime QueryEnd_DateTime = getDateTime ( QueryEnd );
	if ( QueryEnd_DateTime == null ) {
		QueryEnd_DateTime = __query_date2;
	}
	Message.printStatus ( 2, routine,
	"Reading NWSRFS FS5Files time series \"" + TSID + "\"" );
	TS ts = null;
	ts = __nwsrfs_dmi.readTimeSeries (
		TSID, QueryStart_DateTime, QueryEnd_DateTime, Units, true );
	// Now post-process...
	readTimeSeries2 ( ts, null, true );
	if ( ts != null ) {
		// Set the alias...
		ts.setAlias ( ((String)tokens.elementAt(2)).trim() );
	}
	return ts;
	*/
}

/**
Execute the readRiverWare() command:
<pre>
TS Alias = readRiverWare(file,units,start,end)
</pre>
@param command Command to parse.
@exception Exception if there is an error.
*/
private TS do_readRiverWare ( String command_tag, String command_string, GenericCommand command )
throws Exception
{	String routine = "TSEngine.do_readRiverWare";
    int warning_count = 0;
    int warning_level = 2;
    String message;

    CommandStatus status = command.getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);

	// Reparse to strip quotes from file name...
	Vector tokens = StringUtil.breakStringList ( command_string, "=(,)",
			StringUtil.DELIM_ALLOW_STRINGS);
	String infile = ((String)tokens.elementAt(2)).trim();
	//String tsid = ((String)tokens.elementAt(3)).trim();
	//String units = ((String)tokens.elementAt(3)).trim();
	String date1_string = ((String)tokens.elementAt(4)).trim();
	String date2_string = ((String)tokens.elementAt(5)).trim();
	DateTime query_date1 = getDateTime ( date1_string );
	if ( query_date1 == null ) {
		query_date1 = __InputStart_DateTime;
	}
	DateTime query_date2 = getDateTime ( date2_string );
	if ( query_date2 == null ) {
		query_date2 = __InputEnd_DateTime;
	}
    String infile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(__ts_processor),infile) );
	Message.printStatus ( 1, routine, "Reading RiverWare file \"" + infile_full + "\"" );
	TS ts = null;
	try {
        ts = RiverWareTS.readTimeSeries ( infile_full, query_date1, query_date2, null, true );
		// Now post-process...
		readTimeSeries2 ( ts, null, true );
	}
	catch ( Exception e ) {
		message = "Unexpected error reading RiverWare file \"" + infile_full + "\".";
        Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag(command_tag,
                ++warning_count), routine, message );
        Message.printWarning(3, routine, e);
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the file is a valid RiverWare time series file." ) );
        throw new CommandException ( message );
	}
	if ( ts != null ) {
		// Set the alias...
		ts.setAlias ( ((String)tokens.elementAt(2)).trim() );
	}
    status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
	return ts;
}

/**
Execute the TS Alias = readDateValue() command:
<pre>
TS Alias = readDateValue(file,TSID,units,start,end)
</pre>
@param command Command to parse.
@exception Exception if there is an error.
*/
private TS do_TS_readDateValue ( String command_tag, String command_string, GenericCommand command )
throws Exception
{	String routine = "TSEngine.do_TS_readDateValue";
    int warning_level = 2;
    int warning_count = 0;
    String message;

    CommandStatus status = command.getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);
	// Reparse to strip quotes from file name...
	Vector tokens = StringUtil.breakStringList ( command_string, "=(,)", StringUtil.DELIM_ALLOW_STRINGS);
	String infile = ((String)tokens.elementAt(2)).trim();
	String tsid = ((String)tokens.elementAt(3)).trim();
	//String units = ((String)tokens.elementAt(4)).trim();
	String date1_string = ((String)tokens.elementAt(5)).trim();
	String date2_string = ((String)tokens.elementAt(6)).trim();
	DateTime query_date1 = getDateTime ( date1_string );
	if ( query_date1 == null ) {
		query_date1 = __InputStart_DateTime;
	}
	DateTime query_date2 = getDateTime ( date2_string );
	if ( query_date2 == null ) {
		query_date2 = __InputEnd_DateTime;
	}
    String infile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(__ts_processor),infile) );
	Message.printStatus ( 2, routine, "Reading DateValue file \"" + infile_full + "\"" );
	TS ts = null;
	try {	if ( tsid.equals("") || tsid.equals("*") ) {
			ts = DateValueTS.readTimeSeries (
			infile_full, query_date1, query_date2, null, true );
		}
		else {
            ts = DateValueTS.readTimeSeries (
			tsid, infile_full, query_date1, query_date2, null, true );
		}
		// Now post-process...
		readTimeSeries2 ( ts, null, true );
	}
	catch ( Exception e ) {
		message = "Unexpected error reading DateValue file \"" + infile_full + "\".";
		Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag(command_tag,
                    ++warning_count), routine, message );
        Message.printWarning ( 3, routine, e );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that the file is a DateValue time series file." ) );
        throw new CommandException ( message );
	}
	if ( ts != null ) {
		// Set the alias...
		ts.setAlias ( ((String)tokens.elementAt(2)).trim() );
	}
    status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
	return ts;
}

/**
Execute the TS Alias = readNWSRFSFS5Files() command:
<pre>
TS Alias = readNWSRFSFS5Files(TSID="x",QueryStart="X",QueryEnd="X",Units="X")
</pre>
@param command Command to parse.
@exception Exception if there is an error.
*/
private TS do_TS_readNWSRFSFS5Files ( String command )
throws Exception
{	String message, routine = "TSEngine.do_TS_readNWSRFSFS5Files";
	// Get the alias as the second token...
	String Alias = StringUtil.getToken(command," ",
			StringUtil.DELIM_SKIP_BLANKS,1);
	// Split out the command...
	int pos = command.indexOf('=');
	Vector tokens = StringUtil.breakStringList (
			command.substring(pos + 1).trim(),
			"()", StringUtil.DELIM_SKIP_BLANKS );
	PropList props = PropList.parse(
		(String)tokens.elementAt(1),routine,",");
	String TSID = props.getValue ( "TSID" );
	String QueryStart = props.getValue ( "QueryStart" );
	String QueryEnd = props.getValue ( "QueryEnd" );
	String Units = props.getValue ( "Units" );

	if ( TSID == null ) {
		message = "TSID is null - must be specified.";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}
	DateTime query_date1 = null, query_date2 = null;
	if ( QueryStart != null ) {
		query_date1 = DateTime.parse ( QueryStart );
	}
	else {	query_date1 = __InputStart_DateTime;
	}
	if ( QueryEnd != null ) {
		query_date2 = DateTime.parse ( QueryEnd );
	}
	else {	query_date2 = __InputEnd_DateTime;
	}
	Message.printStatus ( 2, routine, "Reading NWSRFS FS5Files time series \"" + TSID + "\"" );
	// Get the TSIdent for the TSID string.  The input name is checked to
	// see if it is a directory.
	// Default to the DMI instance from the calling code, which will
	// be used if a path is not specified in the input name.  In this case,
	// the user has possibly indicated that files should be found using the
	// Apps Defaults.

	TSIdent tsident = new TSIdent ( TSID );
	String input_name = tsident.getInputName();
	// Convert to a full path because this what will be stored in the __nwsrfs_dmi.
	String input_name_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(__ts_processor),input_name) );
	NWSRFS_DMI nwsrfs_dmi = getNWSRFSFS5FilesDMI( input_name_full, true );
	if ( nwsrfs_dmi == null ) {
		Message.printStatus( 2, routine, "No NWSRFS FS5Files are currently open.  Opening using path \"" +
					input_name_full + "\"" );
		nwsrfs_dmi = new NWSRFS_DMI( input_name_full );
	}
	TS ts = null;
	try {
        ts = nwsrfs_dmi.readTimeSeries ( TSID, query_date1, query_date2, Units, true );
		// Now post-process...
		readTimeSeries2 ( ts, null, true );
	}
	catch ( Exception e ) {
		message = "Error reading NWSRFS FS5Files time series \"" + TSID + "\".";
		Message.printWarning ( 2, routine, message );
		Message.printWarning ( 2, routine, e );
		throw new Exception ( message );
	}
	if ( ts != null ) {
		// Set the alias...
		ts.setAlias ( Alias );
	}
	return ts;
}

/**
Execute the readUsgsNwis() command:
<pre>
TS Alias = readUsgsNwis(file,start,end)
</pre>
@param command Command to parse.
@exception Exception if there is an error.
*/
private TS do_readUsgsNwis ( String command_tag, String command_string, GenericCommand command )
throws Exception
{	String routine = "TSEngine.do_readUsgsNwis";
    int warning_level = 2;
    int warning_count = 0;
    String message;
    
    CommandStatus status = command.getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);
    
	// Reparse to strip quotes from file name...
	Vector tokens = StringUtil.breakStringList ( command_string, "=(,)",
			StringUtil.DELIM_ALLOW_STRINGS);
	String infile = ((String)tokens.elementAt(2)).trim();
	String date1_string = ((String)tokens.elementAt(3)).trim();
	String date2_string = ((String)tokens.elementAt(4)).trim();
	DateTime query_date1 = null;
	DateTime query_date2 = null;
	if ( date1_string.equals("*") || date1_string.equals("") ) {
		query_date1 = null;
	}
	else if ( date1_string.equalsIgnoreCase("OutputStart") ) {
		query_date1 = __OutputStart_DateTime;
	}
	else {	try {	query_date1 = DateTime.parse ( date1_string);
		}
		catch ( Exception e ) {
			query_date1 = null;
		}
	}
	if ( date2_string.equals("*") || date2_string.equals("") ) {
		query_date2 = null;
	}
	else if ( date2_string.equalsIgnoreCase("OutputEnd") ) {
		query_date2 = __OutputEnd_DateTime;
	}
	else {	try {	query_date2 = DateTime.parse ( date2_string);
		}
		catch ( Exception e ) {
			query_date2 = null;
		}
	}
    String infile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(__ts_processor),infile) );
	Message.printStatus ( 2, routine, "Reading USGS NWIS file \"" + infile_full + "\"" );
	TS ts = null;
	try {
        ts = UsgsNwisTS.readTimeSeries ( infile_full, query_date1, query_date2, null, true );
		// Now post-process...
		readTimeSeries2 ( ts, null, true );
	}
	catch ( Exception e ) {
        message = "Unexpected error reading USGS NWIS file \"" + infile_full + "\".";
        Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag(command_tag,
                ++warning_count), routine, message );
        Message.printWarning ( 3, routine, e );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the file format is daily USGS NWIS flow from the web site." ) );
	}
	if ( ts != null ) {
		// Set the alias...
		ts.setAlias ( ((String)tokens.elementAt(2)).trim() );
	}
    if ( warning_count > 0 ) {
        message = "There were " + warning_count + " warnings processing the command.";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(
            command_tag, ++warning_count),
            routine,message);
        throw new CommandWarningException ( message );
    }
    status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
	return ts;
}

/**
Execute the runProgram() command:
<pre>
TS Alias = runProgram("Program with arguments",timeout)
</pre>
@param command Command to parse.
@exception Exception if there is an error.
*/
private void do_runProgram ( String command )
throws Exception
{	String routine = "TSEngine.do_runProgram";
	Vector tokens = StringUtil.breakStringList ( command,
		" (,)", StringUtil.DELIM_SKIP_BLANKS|
		StringUtil.DELIM_ALLOW_STRINGS );
	if ( tokens.size() != 3 ) {
		throw new Exception ( "Bad command \"" + command + "\"" );
	}
	// Parse the program and timeout...
	String program = (String)tokens.elementAt(1);
	double timeout = StringUtil.atod((String)tokens.elementAt(2));
	// Do the following if it is hard to track what is going on...
	//PropList props = new PropList ( "PM" );
	//ProcessManagerDialog pmg =
	//new ProcessManagerDialog ( program, props );
	//props = null;
	// pmg = null;
	// Normally can do this, although TSTool may sit for awhile until the
	// process is finished (need to figure out a way to make TSTool wait on
	// the thread without hanging.
	ProcessManager pm = new ProcessManager (program, (int)(timeout*1000.0));
	//pm.saveOutput ( true );
	pm.run();
	if ( pm.getExitStatus() == 996 ) {
		Message.printWarning ( 1, routine, "runProgram(" +
		program.substring(0,35) + "...) timed out.\n" +
		"Full output may not be available." );
	}
	else if ( pm.getExitStatus() > 0 ) {
		Message.printWarning ( 1, routine, "RunProgram(" +
		program.substring(0,35) + "...) exited with status " +
		pm.getExitStatus() + "\n" +
		"Full output may not be available." );
	}
	pm = null;
	tokens = null;
}

/**
Execute the commands:
<pre>
selectTimeSeries(TSID="pattern",Pos="positions",DeselectAllFirst="true|false")
deselectTimeSeries(TSID="pattern",Pos="positions,SelectAllFirst="true|false"")
</pre>
@param command Command to execute.
@param do_select If true, then a selectTimeSeries() is occurring.  Otherwise
a deselectTimeSeries() is occurring.
@exception Exception if there is an error.
*/
private void do_selectTimeSeries ( String command, boolean do_select )
throws Exception
{	String routine = "TSEngine.do_selectTimeSeries";
	String action = "select";
	if ( !do_select ) {
		routine = "TSEngine.do_deselectTimeSeries";
		action = "deselect";
	}
	Vector tokens = StringUtil.breakStringList ( command,
			"()", StringUtil.DELIM_SKIP_BLANKS );
	if ( (tokens == null) || (tokens.size() < 1) ) {
		// Should never happen because the command name was parsed
		// before...
		throw new Exception ( "Bad command: \"" + command + "\"" );
	}
	// Get the input needed to process the file...
	PropList props = PropList.parse (
		(String)tokens.elementAt(1), routine, "," );
	String TSID = props.getValue ( "TSID" );
	String Pos = props.getValue ( "Pos" );
	String DeselectAllFirst = props.getValue ( "DeselectAllFirst" );
	String SelectAllFirst = props.getValue ( "SelectAllFirst" );
	if ( (TSID == null) && (Pos == null) ) {
		String message =
		"Need TSID pattern or position to " + action + " time series.";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}
	// If using positions, create an array of position ranges that will
	// be matched...
	int [] start_pos = null;
	int [] end_pos = null;
	int npos = 0;
	String token = null;
	if ( Pos != null ) {
		tokens = StringUtil.breakStringList ( Pos,
			",", StringUtil.DELIM_SKIP_BLANKS );
		if ( tokens != null ) {
			npos = tokens.size();
		}
		start_pos = new int[npos];
		end_pos = new int[npos];
		for ( int i = 0; i < npos; i++ ) {
			token = (String)tokens.elementAt(i);
			if ( token.indexOf("-") >= 0 ) {
				// Range...
				start_pos[i] = StringUtil.atoi(
					StringUtil.getToken(token, "-",0,0).
					trim());
				end_pos[i] = StringUtil.atoi(
					StringUtil.getToken(token, "-",0,1).
					trim());
			}
			else {	// Single value.  Treat as a range of 1.
				start_pos[i] = StringUtil.atoi(token);
				end_pos[i] = start_pos[i];
			}
			Message.printStatus ( 1, "",
			"Range " + i + " from " + token + " is " +
			start_pos[i] + "," + end_pos[i] );
		}
	}
	// Clear if requested...
	int nts = getTimeSeriesSize();
	TS ts = null;
	if (	(DeselectAllFirst != null) &&
		DeselectAllFirst.equalsIgnoreCase("true") ) {
		Message.printStatus ( 2, routine,
		"Deselecting all time series first." );
		for ( int its = 0; its < nts; its++ ) {
			ts = getTimeSeries(its);	// Will throw Exception
			ts.setSelected ( false );
		}
	}
	if (	(SelectAllFirst != null) &&
		SelectAllFirst.equalsIgnoreCase("true") ) {
		Message.printStatus ( 2, routine,
		"Selecting all time series first." );
		for ( int its = 0; its < nts; its++ ) {
			ts = getTimeSeries(its);	// Will throw Exception
			ts.setSelected ( true );
		}
	}
	// Search through all the time series in memory, selecting/deselecting
	// those with identifiers that match the pattern...
	int count = 0;
	boolean match = false;	// Indicates whether a time series is matched.
	for ( int its = 0; its < nts; its++ ) {
		ts = getTimeSeries(its);	// Will throw Exception
		if ( TSID != null ) {
			if ( !ts.getIdentifier().matches(TSID) ) {
				continue;
			}
		}
		if ( Pos != null ) {
			// Evaluate whether the position matches one of the
			// requested...
			match = false;
			for ( int j = 0; j < npos; j++ ) {
				if (	((its + 1) >= start_pos[j]) &&
					((its + 1) <= end_pos[j]) ) {
					match = true;
					break;
				}
			}
			if ( !match ) {
				continue;
			}
		}
		// Else select/deselect the time series...
		++count;
		if ( do_select ) {
			ts.setSelected ( true );
			Message.printStatus ( 2, routine,
			"Selecting [" + its + "]:" + ts.getIdentifierString() );
		}
		else {	ts.setSelected ( false );
			Message.printStatus ( 2, routine,
			"Deselecting [" + its + "]:"+ts.getIdentifierString() );
		}
	}
	if ( count == 0 ) {
		// Probably an error.
		String message =
		"No time series were matched for \"" + command + "\"";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}
}

/**
Execute the setAutoExtendPeriod() command.
@param command Command to parse.
@exception Exception if there is an error.
*/
private void do_setAutoExtendPeriod ( String command )
throws Exception
{	String routine = "TSEngine.do_setAutoExtendPeriod";
	Vector tokens = StringUtil.breakStringList ( command,
			" (,)", StringUtil.DELIM_SKIP_BLANKS );
	if ( (tokens == null) || (tokens.size() != 2) ) {
		throw new Exception ( "Bad command \"" + command + "\"" );
	}
	// Parse the flag...
	String toggle = ((String)tokens.elementAt(1)).trim();
	if ( toggle.equalsIgnoreCase("True") ) {
		setAutoExtendPeriod ( true );
		Message.printStatus ( 1, routine, "TS period will " +
		"automatically be extended to the output period." );
	}
	else if ( toggle.equalsIgnoreCase("False") ) {
		setAutoExtendPeriod ( false );
		Message.printStatus ( 1, routine, "TS period will NOT " +
		"automatically be extended to the output period." );
	}
	else {	throw new Exception (
		"Unrecognized value \"" +toggle+"\" (expecting true or false)");
	}
	tokens = null;
	toggle = null;
}

/**
Execute the new setAveragePeriod() or old -averageperiod command.
@param command Command to parse.
@exception Exception if there is an error.
*/
private void do_setAveragePeriod ( String command )
throws Exception
{	Vector	tokens = null;
	String	date1_string = null, date2_string = null;
	DateTime	averaging_date1 = null, averaging_date2 = null;
	if ( command.regionMatches( true,0,"setAveragePeriod",0,16) ) {
		// New syntax...
		tokens = StringUtil.breakStringList ( command,
				" (,)", StringUtil.DELIM_SKIP_BLANKS );
	}
	else if ( command.regionMatches(true,0,"-averageperiod",0,14) ) {
		// Old syntax...
		tokens = StringUtil.breakStringList ( command,
				" ", StringUtil.DELIM_SKIP_BLANKS );
	}
	if ( (tokens == null) || (tokens.size() != 3) ) {
		tokens = null;
		throw new Exception ( "Bad command \"" + command + "\"" );
	}
	date1_string = ((String)tokens.elementAt(1)).trim();
	date2_string = ((String)tokens.elementAt(2)).trim();
	// Parse the dates.  They may be named dates......
	// Need to figure out how this impacts averaging, etc.
	if ( !date1_string.equals("*") && !date1_string.equals("")) {
		// Try getting a named date/time...
		averaging_date1=(DateTime)__datetime_Hashtable.get(date1_string);
		if ( averaging_date1 == null ) {
			// Need to parse the string...
			try {	averaging_date1 = DateTime.parse(date1_string);
			}
			catch ( Exception e ) {
				throw new Exception (
				"Error processing start date for \"" +
				command + "\"." );
			}
		}
	}
	if ( !date2_string.equals("*") && !date1_string.equals("") ) {
		averaging_date2=(DateTime)__datetime_Hashtable.get(date2_string);
		if ( averaging_date2 == null ) {
			// Need to parse the string...
			try {	averaging_date2 = DateTime.parse(date2_string);
			}
			catch ( Exception e ) {
				throw new Exception ( "Error processing end date for \"" + command+	"\"." );
			}
		}
	}
	// If we have gotten to here, then we have dates that can be set (using
	// strings, which is a little awkward)...
	if ( date1_string.equals("*") || date1_string.equals("") ) {
		setAverageStart (null);
	}
	else {
        setAverageStart ( new DateTime ( averaging_date1 ) );
	}
	if ( date2_string.equals("*") || date2_string.equals("") ) {
		setAverageEnd ( null );
	}
	else {
        setAverageEnd ( new DateTime ( averaging_date2 ) );
	}
	Message.printStatus ( 2, "TSEngine.do_SetAveragePeriod",
		"Average period set to " + getAverageStart() + " to " + getAverageEnd() );
	// Clean up...
	tokens = null;
	date1_string = null;
	date2_string = null;
	averaging_date1 = null;
	averaging_date2 = null;
}

/**
Execute the new setBinaryTSDayCutoff() or old -binary_day_cutoff command.
@param expression Expression to parse.
@exception Exception if there is an error.
*/
private void do_setBinaryTSDayCutoff ( String expression )
throws Exception
{	// Works for old and new...
	Vector tokens = StringUtil.breakStringList ( expression,
			"() \t", StringUtil.DELIM_SKIP_BLANKS );
	if ( (tokens == null) || (tokens.size() != 2) ) {
		throw new Exception ("Bad command: \"" + expression + "\"" );
	}
	__binary_day_cutoff = StringUtil.atoi(((String)tokens.elementAt(1)).trim() );
	Message.printStatus ( 1, "TSEngine.do_setBinaryTSDayCutoff",
	"BinaryTS file will be used for daily data if >= " + __binary_day_cutoff + " daily time series." );
	tokens = null;
}

/**
Execute the new setBinaryTSFile() or old -binary_ts_file command.
@param expression Expression to parse.
@exception Exception if there is an error.
*/
private void do_setBinaryTSFile ( String expression )
throws Exception
{	// Works for old and new...
	Vector tokens = StringUtil.breakStringList ( expression,
			"() \t", StringUtil.DELIM_SKIP_BLANKS |
			StringUtil.DELIM_ALLOW_STRINGS );
	if ( (tokens == null) || (tokens.size() != 2) ) {
		throw new Exception (
		"Bad command: \"" + expression + "\"" );
	}
	__binary_ts_file = ((String)tokens.elementAt(1)).trim();
	Message.printStatus ( 1, "TSEngine.do_setBinaryTSFile",
	"BinaryTS file base name is \"" + __binary_ts_file + "\"" );
	tokens = null;
}

/**
Execute the setBinaryTSPeriod() command.  Dates must be specified (* or blank
dates are not allowed).
@param command Command to parse.
@exception Command if there is an error.
*/
private void do_setBinaryTSPeriod ( String command )
throws Exception
{	Vector tokens = StringUtil.breakStringList ( command,
			"(,)", StringUtil.DELIM_SKIP_BLANKS );
	if ( (tokens == null) || (tokens.size() != 3) ) {
		throw new Exception (
		"Bad command \"" + command + "\"" );
	}
	String date1 = ((String)tokens.elementAt(1)).trim();
	String date2 = ((String)tokens.elementAt(2)).trim();
	// Parse the dates.  They may be named dates...
	__binaryts_date1 = (DateTime)__datetime_Hashtable.get( date1);
	if ( __binaryts_date1 == null ) {
		// Did not find a date in the hash table so parse it...
		try {	__binaryts_date1 = DateTime.parse ( date1 );
		}
		catch ( Exception e ) {
			Message.printWarning ( 1,
			"TSEngine.do_setBinaryTSPeriod",
			"Error parsing setBinaryTSPeriod() date \""+date1+"\"");
		}
	}
	__binaryts_date2 = (DateTime)__datetime_Hashtable.get( date2);
	if ( __binaryts_date2 == null ) {
		// Did not find a date in the hash table so parse it...
		try {	__binaryts_date2 = DateTime.parse ( date2 );
		}
		catch ( Exception e ) {
			Message.printWarning ( 1,
			"TSEngine.do_setBinaryTSPeriod",
			"Error parsing setBinaryTSPeriod() date \""+date2+"\"");
		}
	}
	Message.printStatus ( 1, "TSEngine.do_setBinaryTSPeriod",
	"BinaryTS period set to " + __binaryts_date1 + " to " + __binaryts_date2);
	tokens = null;
}

/**
Execute the setConstant() command.
@param command Command to parse.
@exception Exception if there is an error.
*/
private void do_setConstant ( String command )
throws Exception
{	String routine = "TSEngine.do_setConstant", message;
	String TSID = null;
	String ConstantValue = null;
	String MonthValues = null;
	String SetStart = null;
	String SetEnd = null;
	DateTime start = null, end = null;
	if ( command.indexOf('=') < 0 ) {
		// Old syntax...
		Vector tokens = StringUtil.breakStringList ( command,
			"(,)", StringUtil.DELIM_SKIP_BLANKS );
		if ( tokens.size() != 3 ) {
			throw new Exception("Bad command \"" + command + "\"" );
		}
		// Parse the identifier...
		TSID = (String)tokens.elementAt(1);
		ConstantValue = (String)tokens.elementAt(2);
	}
	else {	// New syntax...
		Vector tokens = StringUtil.breakStringList ( command,
			"()", StringUtil.DELIM_SKIP_BLANKS );
		if ( (tokens == null) || (tokens.size() < 2) ) {
			// Should never happen because the command name was
			// parsed before...
			throw new Exception("Bad command: \"" + command + "\"");
		}
		// Get the input needed to process the file...
		PropList props = PropList.parse ( (String)tokens.elementAt(1), routine, "," );
		TSID = props.getValue ( "TSID" );
		ConstantValue = props.getValue ( "ConstantValue" );
		MonthValues = props.getValue ( "MonthValues" );
		SetStart = props.getValue ( "SetStart" );
		SetEnd = props.getValue ( "SetEnd" );
	}
	if ( ConstantValue == null ) {
		ConstantValue = "";
	}
	if ( MonthValues == null ) {
		MonthValues = "";
	}
	if (	((ConstantValue.length() == 0) && (MonthValues.length() == 0))||
		((ConstantValue.length() > 0) && (MonthValues.length() > 0)) ) {
		message = "Choose a single value or monthly values, but not both.";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}
	double constant = 0.0;
	if ( ConstantValue.length() > 0 ) {
		if ( !StringUtil.isDouble(ConstantValue) ) {
			message = "Constant \"" + ConstantValue + "\" is not a number.\n";
			Message.printWarning ( 2, routine, message );
			throw new Exception ( message );
		}
		else {
            constant = StringUtil.atod ( ConstantValue );
            Message.printStatus( 2, routine, "Setting time series to constant " + constant );
		}
	}
	double [] mconstant = null;
	if ( MonthValues.length() > 0 ) {
		mconstant = new double[12];
		Vector v = StringUtil.breakStringList ( MonthValues,",", 0 );
		if ( (v == null) || (v.size() != 12) ) {
			message = "12 monthly values must be specified.";
			Message.printWarning ( 2, routine, message );
			throw new Exception ( message );
		}
		else {
            String val;
			for ( int i = 0; i < 12; i++ ) {
				val = ((String)v.elementAt(i)).trim();
				if ( !StringUtil.isDouble(val) ) {
					message = "\nMonthly value \"" + val + " is not a number.";
					throw new Exception ( message );
				}
				mconstant[i] = StringUtil.atod ( val );
			}
		}
	}
	try {	if ( SetStart != null ) {
			start = DateTime.parse(SetStart);
		}
	}
	catch ( Exception e ) {
		message = "Set start is not a valid date/time...ignoring.";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}
	try {	if ( SetEnd != null ) {
			end = DateTime.parse(SetEnd);
		}
	}
	catch ( Exception e ) {
		message = "Set end is not a valid date/time...ignoring.";
		Message.printWarning ( 1, routine, message );
		throw new Exception ( message );
	}
	TS ts = null;
	if ( TSID.equals("*") ) {
		// Fill everything in memory...
		int nts = getTimeSeriesSize();
		// Set first in case there is an exception...
		for ( int its = 0; its < nts; its++ ) {
			ts = getTimeSeries(its);
			if ( mconstant == null ) {
				TSUtil.setConstant ( ts, start, end, constant );
			}
			else {	TSUtil.setConstantByMonth ( ts,
					start, end, mconstant );
			}
			// Update...
			processTimeSeriesAction ( UPDATE_TS, ts, its );
		}
	}
	else {	// Fill one time series...
		int ts_pos = indexOf ( TSID );
		if ( ts_pos >= 0 ) {
			ts = getTimeSeries ( ts_pos );
			if ( mconstant == null ) {
				TSUtil.setConstant ( ts, start, end, constant );
			}
			else {	TSUtil.setConstantByMonth ( ts, start, end,	mconstant );
			}
			processTimeSeriesAction (UPDATE_TS, ts, ts_pos);
		}
		else {	message = "Unable to find time series \"" + TSID + "\" for setConstant() command.";
			Message.printWarning ( 2, routine, message );
			throw new Exception ( message );
		}
	}
}

/**
Execute the setDataValue() command.
@param command Command to parse.
@exception Exception if there is an error.
*/
private void do_setDataValue ( String command )
throws Exception
{	String routine = "TSEngine.do_setDataValue";
	Vector tokens = StringUtil.breakStringList ( command,
		"(, )\t", StringUtil.DELIM_SKIP_BLANKS );
	if ( tokens.size() != 4 ) {
		throw new Exception ( "Bad command \"" + command + "\"" );
	}
	// Parse the identifier...
	String alias = ((String)tokens.elementAt(1)).trim();
	String datestring =((String)tokens.elementAt(2)).trim();
	DateTime setdate = DateTime.parse ( datestring );
	String constant_string = ((String)tokens.elementAt(3)).trim();
	double constant = StringUtil.atod(constant_string);
	if ( (setdate == null) || (setdate.getYear() == 0) ) {
		String message = "Set date \"" + datestring +
			"\" is not a valid date";
		Message.printWarning ( 1, routine, message );
		throw new Exception ( message );
	}
	int ts_pos = indexOf ( alias );
	if ( ts_pos >= 0 ) {
		TS ts = getTimeSeries ( ts_pos );
		ts.setDataValue ( setdate, constant );
		ts.addToGenesis ( "Set " + constant_string +
			" on " + datestring );
		processTimeSeriesAction ( UPDATE_TS, ts, ts_pos );
	}
	else {	String message = "Unable to find time series \"" +
			alias + "\" for setDataValue() command.";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}
	tokens = null;
	setdate = null;
	datestring = null;
}

/**
Execute the new setDebugLevel() or old -d command.
@param expression Expression to parse.
@exception Exception if there is an error.
*/
private void do_setDebugLevel ( String expression )
throws Exception
{	Vector	tokens = null;
	if ( expression.regionMatches(true,0, "setDebugLevel", 0,13) ) {
		// New style...
		tokens = StringUtil.breakStringList ( expression,
				" (,)", StringUtil.DELIM_SKIP_BLANKS );
	}
	else if (expression.length() >= 2 ) {
		// Old style -d#... or -d #...
		if (	(expression.length() == 2) ||
			((expression.length() > 2) &&
			(expression.charAt(2) == ' ')) ) {
			// Parse the whole thing...
			tokens = StringUtil.breakStringList ( expression,
				" ,", StringUtil.DELIM_SKIP_BLANKS );
		}
		else {	// Parse from the 3rd character on and then insert a
			// dummy command...
			tokens = StringUtil.breakStringList (
				expression.substring(2),
				" ,", StringUtil.DELIM_SKIP_BLANKS );
			tokens.insertElementAt("-d",0);
		}
	}
	// Now the same token structure...
	if ( tokens == null ) {
		throw new Exception (
		"Bad command \"" + expression + "\"" );
	}
	int size = tokens.size();
	if ( size == 1 ) {
		// Set debug level to 1...
		Message.isDebugOn = true;
		Message.setDebugLevel( Message.TERM_OUTPUT, 1 );
		Message.setDebugLevel( Message.LOG_OUTPUT, 1 );
	}
	else if ( size == 2 ) {
		// Set debug level to same value for all...
		int debug = StringUtil.atoi(
			((String)tokens.elementAt(1)).trim());
		if ( debug == 0 ) {
			Message.isDebugOn = false;
		}
		else {	Message.isDebugOn = true;
		}
		Message.setDebugLevel( Message.TERM_OUTPUT, debug );
		Message.setDebugLevel( Message.LOG_OUTPUT, debug );
	}
	else if ( size == 3 ) {
		// Set debug level to different values for log and display...
		int debug1 = StringUtil.atoi(
			((String)tokens.elementAt(1)).trim());
		int debug2 = StringUtil.atoi(
			((String)tokens.elementAt(2)).trim());
		if ( (debug1 == 0) && (debug2 == 0) ) {
			Message.isDebugOn = false;
		}
		else {	Message.isDebugOn = true;
		}
		Message.setDebugLevel( Message.TERM_OUTPUT, debug1 );
		Message.setDebugLevel( Message.LOG_OUTPUT, debug2 );
	}
	else {	tokens = null;
		throw new Exception (
		"Bad command \"" + expression + "\"" );
	}
	// Clean up...
	tokens = null;
}

/**
Helper method for setFromTS() command.
@param command_tag Command number used for messaging.
@param command Command being evaluated.
@exception Exception if there is an error processing the time series.
*/
private TS do_setFromTS ( String command_tag, String command )
throws Exception
{	// Don't parse with spaces because a TEMPTS may be present.
	Vector v = StringUtil.breakStringList(command,
		"(),\t", StringUtil.DELIM_SKIP_BLANKS); 
	String message, routine = "TSEngine.do_setFromTS";
	if ( (v == null) || (v.size() < 5) ) {
		message = "Syntax error in \"" + command + "\"";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}
	// Get the individual tokens of the expression...
	String dependent = ((String)v.elementAt(1)).trim();
	String independent = ((String)v.elementAt(2)).trim();
	String analysis_period_start_string = ((String)v.elementAt(3)).trim();
	String analysis_period_end_string = ((String)v.elementAt(4)).trim();
	v = null;
	// Make sure there are time series available to operate on...
	int ts_pos = indexOf ( dependent );
	TS dependentTS = getTimeSeries ( ts_pos );
	if ( dependentTS == null ) {
		message = "Unable to find time series \"" + dependent +
		"\" for setFromTS().";
		Message.printWarning ( 1, routine, message );
		throw new Exception ( message );
	}
	// The independent identifier may or may not have TEMPTS at the front
	// but is handled by getTimeSeries...
	TS independentTS = getTimeSeries ( command_tag, independent );
	if ( independentTS == null ) {
		message = "Unable to find time series \"" + independent +
				"\" for setFromTS().";
		Message.printWarning ( 1, routine, message );
		throw new Exception ( message );
	}

	DateTime analysis_period_start =
		getDateTime(analysis_period_start_string);
	DateTime analysis_period_end = getDateTime(analysis_period_end_string);
	// Fill the dependent time series for the analysis period...
	try {	TSUtil.setFromTS (	dependentTS, independentTS,
					analysis_period_start,
					analysis_period_end, null );
		processTimeSeriesAction ( UPDATE_TS, dependentTS, ts_pos );
	}
	catch ( Exception e ) {
		message = "Error executing command: \"" + command + "\"";
		Message.printWarning ( 2, routine, message );
		Message.printWarning ( 2, routine, e );
		throw new Exception ( message );
	}
	return dependentTS;
}

/**
Execute the new setIgnoreLEZero() or old -ignorelezero command.
@param command Command to parse.
@exception Exception if there is an error.
*/
private void do_setIgnoreLEZero ( String command )
throws Exception
{	String routine = "TSEngine.doSetIgnoreLEZero";
	if ( command.regionMatches(true,0,"-ignorelezero",0,13) ) {
		setIgnoreLEZero ( true );
		Message.printStatus ( 2, routine, "Values <= 0 will be treated as missing when computing historic averages." );
	}
	else if ( command.regionMatches(true,0, "setIgnoreLEZero", 0,15) ) {
		// Check the second token for "true" or "false".
		Vector tokens = StringUtil.breakStringList ( command, " (,)", StringUtil.DELIM_SKIP_BLANKS );
		if ( (tokens == null) || (tokens.size() != 2) ) {
			throw new Exception ( "Bad command \"" + command+"\"" );
		}
		// Parse the flag...
		String toggle = (String)tokens.elementAt(1);
		if ( toggle.equalsIgnoreCase("True") ) {
			setIgnoreLEZero ( true );
			Message.printStatus ( 2, routine,
			"Values <= 0 WILL be treated as missing when computing historic averages." );
		}
		else if ( toggle.equalsIgnoreCase("False") ) {
            setIgnoreLEZero ( false );
			Message.printStatus ( 2, routine,
			"Values <= 0 WILL NOT be treated as missing when computing historic averages." );
		}
		else {	throw new Exception (
			"Unrecognized value \"" + toggle + "\" (expecting true or false)");
		}
		tokens = null;
		toggle = null;
	}
}

/**
Execute the new setIncludeMissingTS() or old -include_missing_ts command.
@param expression Expression to parse.
@exception Exception if there is an error.
*/
private void do_setIncludeMissingTS ( String expression )
throws Exception
{	String routine = "TSEngine.do_setIncludeMissingTS";
	if ( expression.regionMatches(true,0,"-include_missing_ts",0,19) ) {
		setIncludeMissingTS ( true );
		Message.printStatus ( 2, routine,"Missing time series will have default time series inserted." );
	}
	else if ( expression.regionMatches(true,0, "setIncludeMissingTS",0,19)){
		// Check the second token for "true" or "false".
		Vector tokens = StringUtil.breakStringList ( expression,
				" (,)", StringUtil.DELIM_SKIP_BLANKS );
		if ( (tokens == null) || (tokens.size() != 2) ) {
			throw new Exception ( "Bad command \"" + expression + "\"" );
		}
		// Parse the flag...
		String toggle = (String)tokens.elementAt(1);
		if ( toggle.equalsIgnoreCase("True") ) {
			setIncludeMissingTS ( true );
			Message.printStatus ( 1, routine, "Missing time series will have default time series inserted." );
		}
		else if ( toggle.equalsIgnoreCase("False") ) {
			setIncludeMissingTS ( false );
			Message.printStatus ( 1, routine, "Missing time series will not have default time series inserted." );
		}
		else {	throw new Exception ("Unrecognized value \"" + toggle + "\" (expecting true or false)");
		}
		tokens = null;
		toggle = null;
	}
}

/**
Execute the setMax() command.
@param command_tag Command number used for messaging.
@param command Command to parse.
@exception Exception if there is an error.
*/
private void do_setMax ( String command_tag, String command )
throws Exception
{	String routine = "TSEngine.do_setMax", message;
	int warning_level = 2;
	int warning_count = 0;
	Vector v = StringUtil.breakStringList ( command,
		"(),\t", StringUtil.DELIM_SKIP_BLANKS |
		StringUtil.DELIM_ALLOW_STRINGS ); 
	if ( (v == null) || (v.size() < 3) ) {
		throw new Exception ( "Bad command \"" + command + "\"" );
	}
	String alias = ((String)v.elementAt(1)).trim();
	String independent = null;

	// Make sure there are time series available to operate on...

	int ts_pos = indexOf ( alias );
	TS ts = getTimeSeries ( ts_pos );
	if ( ts == null ) {
		message = "Unable to find time series \""
		+ alias + "\" for\n" + command + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,
		++warning_count), routine, message );
		throw new CommandException ( message );
	}
	// Now loop through the remaining time series and put into a Vector for
	// processing...
	int vsize = v.size();
	Vector vTS = new Vector ( vsize - 1 );
	TS independentTS = null;
	for ( int its = 2; its < vsize; its++ ) {
		independent = ((String)v.elementAt(its)).trim();
		// The following may or may not have TEMPTS at the front but is
		// handled transparently by getTimeSeries.
		independentTS = getTimeSeries ( command_tag, independent );
		if ( independentTS == null ) {
			message = "Unable to find time series \"" + independent+
			"\" for \"" + command + "\".";
			Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(command_tag,
			++warning_count), routine, message );
			v = null;
			vTS = null;
			independentTS = null;
			continue;
		}
		else {	vTS.addElement ( independentTS );
		}
	}
	ts = TSUtil.max ( ts, vTS );
	v = null;
	vTS = null;
	independentTS = null;
	processTimeSeriesAction ( UPDATE_TS, ts, ts_pos );
	if ( warning_count > 0 ) {
		message = "There were " + warning_count +
			" warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag, ++warning_count),
			routine,message);
		throw new CommandWarningException ( message );
	}
}

/**
Execute the new setMissingDataValue() or old -missing command.
@param expression Expression to parse.
@exception Exception if there is an error.
*/
private void do_setMissingDataValue ( String expression )
throws Exception
{	// Works for both....
	Vector tokens = StringUtil.breakStringList ( expression,
				"( ,)", StringUtil.DELIM_SKIP_BLANKS );
	if ( (tokens == null) || (tokens.size() < 2) ) {
		throw new Exception (
		"Bad command \"" + expression + "\"" );
	}
	if ( tokens.size() == 2 ) {
		__missing_range = new double[1];
		__missing_range[0] =StringUtil.atod((String)tokens.elementAt(1));
	}
	else {
        __missing_range = new double[2];
		__missing_range[0] =StringUtil.atod((String)tokens.elementAt(1));
		__missing_range[1] =StringUtil.atod((String)tokens.elementAt(2));
	}
	tokens = null;
}

/**
Execute the new setOutputDetailedHeaders() or old -detailedheader command.
@param expression Expression to parse.
@exception Exception if there is an error.
*/
private void do_setOutputDetailedHeaders ( String expression )
throws Exception
{	if ( expression.regionMatches( true,0,"-detailedheader",0,15) ) {
		setOutputDetailedHeader ( true );
	}
	else if ( expression.regionMatches(true,0,"setOutputDetailedHeaders", 0,24) ) {
		// Check the second token for "true" or "false".
		Vector tokens = StringUtil.breakStringList ( expression,
				" (,)", StringUtil.DELIM_SKIP_BLANKS );
		if ( (tokens == null) || (tokens.size() != 2) ) {
			throw new Exception ( "Bad command \"" + expression + "\"" );
		}
		// Parse the name and dates...
		String toggle = (String)tokens.elementAt(1);
		if ( toggle.equalsIgnoreCase("True") ) {
			setOutputDetailedHeader ( true );
		}
		else if ( toggle.equalsIgnoreCase("False") ) {
			setOutputDetailedHeader ( false );
		}
		else {	throw new Exception (
			"Unrecognized value \"" + toggle +
			"\" (expecting true or false) for \"" + expression + "\"" );
		}
		tokens = null;
		toggle = null;
	}
}

/**
Execute the new setOutputYearType() or old -cy, -wy command.
@param expression Expression to parse.
@exception Exception if there is an error.
*/
private void do_setOutputYearType ( String expression )
throws Exception
{	String routine = "TSEngine.do_setOutputYearType";	
	if ( expression.equalsIgnoreCase("-cy") ) {
		Message.printStatus ( 2, routine, "Output will be in calendar year." );
		setOutputYearType ( __CALENDAR_YEAR );
	}
	else if ( expression.equalsIgnoreCase("-wy") ) {
		Message.printStatus ( 2, routine, "Output will be in water year." );
		setOutputYearType ( __WATER_YEAR );
	}
	else if ( expression.regionMatches(true,0,"setOutputYearType", 0,17) ) {
		// Check the second token for the year type.
		Vector tokens = StringUtil.breakStringList ( expression,
			" (,)", StringUtil.DELIM_SKIP_BLANKS );
		if ( tokens.size() != 2 ) {
			throw new Exception ("Bad command \"" + expression + "\"" );
		}
		// Parse the name and dates...
		String year_type = (String)tokens.elementAt(1);
		if ( year_type.equalsIgnoreCase("Water") ) {
			setOutputYearType ( __WATER_YEAR );
			Message.printStatus ( 2, routine, "Output will be in Water year" );
		}
		else if ( year_type.equalsIgnoreCase("Calendar") ) {
			setOutputYearType ( __CALENDAR_YEAR );
			Message.printStatus ( 2, routine, "Output will be in Calendar year" );
		}
		else {	throw new Exception (
			"Unrecognized year type \"" + year_type + "\" for \"" +	expression + "\".");
		}
		tokens = null;
		year_type = null;
	}
}

/**
Execute the new setPatternFile() or old -filldata.
@param command Command to parse.
@exception Exception if there is an error.
*/
private void do_setPatternFile ( String command )
throws Exception
{	String routine = "TSEngine.do_setPatternFile";
	// The following works with old and new...
	Vector tokens = StringUtil.breakStringList ( command, " (,)",
		StringUtil.DELIM_SKIP_BLANKS|StringUtil.DELIM_ALLOW_STRINGS);
	if ( (tokens == null) || (tokens.size() != 2) ) {
		throw new Exception ( "Bad command \"" + command + "\"" );
	}
	String fillpatternfile = ((String)tokens.elementAt(1)).trim();
    String fillpatternfile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(__ts_processor),fillpatternfile) );
	Message.printStatus ( 1, routine, "Using \"" + fillpatternfile_full + "\" for fill pattern file." );
	// Read the fill pattern file.  Since multiple options are allowed,
	// create a temporary Vector and then append to the main vector...
	Vector fill_pattern_ts = StateMod_TS.readPatternTimeSeriesList(	fillpatternfile_full, true );
	if ( fill_pattern_ts == null ) {
		throw new Exception ("No pattern time series read from \"" + fillpatternfile_full + "\"");
	}
	else {	int listsize = fill_pattern_ts.size();
		Message.printStatus ( 2, routine,
		"Read "+listsize+" pattern time series from \""+ fillpatternfile_full + "\"" );
		for ( int j = 0; j < listsize; j++ ) {
			__fill_pattern_ts.addElement (fill_pattern_ts.elementAt(j) );
		}
	}
	fill_pattern_ts = null;
	tokens = null;
	fillpatternfile = null;
}

/**
Execute the setToMin() command.
@param command_tag Command number used for messaging.
@param command Command to parse.
@exception Exception if there is an error.
*/
private void do_setToMin ( String command_tag, String command )
throws Exception
{	String routine = "TSEngine.do_setToMin", message;
	int warning_level = 2;
	int warning_count = 0;
	Vector v = StringUtil.breakStringList ( command,
		"(),\t", StringUtil.DELIM_SKIP_BLANKS |
		StringUtil.DELIM_ALLOW_STRINGS ); 
	if ( (v == null) || (v.size() < 3) ) {
		throw new Exception ( "Bad command \"" + command + "\"" );
	}
	String alias = ((String)v.elementAt(1)).trim();
	String independent = null;

	// Make sure there are time series available to operate on...

	int ts_pos = indexOf ( alias );
	TS ts = getTimeSeries ( ts_pos );
	if ( ts == null ) {
		message = "Unable to find time series \""
		+ alias + "\" for\n" + command + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,
		++warning_count), routine, message );
		throw new CommandException ( message );
	}
	// Now loop through the remaining time series and put into a Vector for
	// processing...
	int vsize = v.size();
	Vector vTS = new Vector ( vsize - 1 );
	TS independentTS = null;
	for ( int its = 2; its < vsize; its++ ) {
		independent = ((String)v.elementAt(its)).trim();
		// The following may or may not have TEMPTS at the front but is
		// handled transparently by getTimeSeries.
		independentTS = getTimeSeries ( command_tag, independent );
		if ( independentTS == null ) {
			message = "Unable to find time series \"" + independent+
			"\" for \"" + command + "\".";
			Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(command_tag,
			++warning_count), routine, message );
			v = null;
			vTS = null;
			independentTS = null;
			continue;
		}
		else {	vTS.addElement ( independentTS );
		}
	}
	ts = TSUtil.min ( ts, vTS );
	v = null;
	vTS = null;
	independentTS = null;
	processTimeSeriesAction ( UPDATE_TS, ts, ts_pos );
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag, ++warning_count),
			routine,message);
		throw new CommandWarningException ( message );
	}
}

/**
Execute the new setWorkingDir() command.
@param command Command to parse.
@param app_PropList properties from an application.  If not null, the working
directory will be set in the "WorkingDir" property.
@exception Exception if there is an error.
*/
private void do_setWorkingDir ( String command, PropList app_PropList )
throws Exception
{	Vector tokens = StringUtil.breakStringList ( command,
			" (,)", StringUtil.DELIM_SKIP_BLANKS |
			StringUtil.DELIM_ALLOW_STRINGS );
	String routine = "TSEngine.setWorkingDir";
	if ( tokens.size() < 2 ) {
		throw new Exception ( "Bad command \"" + command + "\"" );
	}
	// Parse the directory and mode...
	String dir = ((String)tokens.elementAt(1)).trim();
	String type = "GUIAndBatch";
	if ( tokens.size() == 3 ) {
		// Type is specified ...
		type = ((String)tokens.elementAt(2)).trim();
	}
	if (	!type.equalsIgnoreCase("BatchOnly") &&
		!type.equalsIgnoreCase("GUIOnly") &&
		!type.equalsIgnoreCase("GUIAndBatch") ) {
		String message = "Unrecognized value \"" + type +
		"\" (expecting GUIOnly or GUIAndBatch)";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}
	if (	(!IOUtil.isBatch() && type.equalsIgnoreCase("GUIOnly")) ||
		(IOUtil.isBatch() && type.equalsIgnoreCase("BatchOnly")) ||
		type.equalsIgnoreCase("GUIAndBatch") ) {
		// Set the working directory...
		// TODO SAM 2007-08-22 Currently does not allow adjustments to the
		// path - only absolute resets.
		if ( IOUtil.fileExists(dir) ) {
			// TODO SAM 2007-08-22 Evaluate the ramifications of this
			// being application-wide vs. just the processor instance.
			IOUtil.setProgramWorkingDir(dir);
            __ts_processor.setWorkingDir(dir);
			if ( app_PropList != null ) {
				app_PropList.set ( "WorkingDir", dir );
			}
			Message.printStatus ( 1, routine, "Setting working directory to \"" + dir + "\"" );
		}
		else {	String message = "Working directory \"" + dir +	"\" does not exist.  Not setting.";
			Message.printWarning ( 2, routine, message );
			throw new Exception ( message );
		}
	}
	tokens = null;
	dir = null;
	type = null;
}

/**
Execute the new setWarningLevel() or old -w command.
@param expression Expression to parse.
@exception Exception if there is an error.
*/
private void do_setWarningLevel ( String expression )
throws Exception
{	Vector	tokens = null;
	if ( expression.regionMatches(true,0, "setWarningLevel", 0,15) ) {
		// New style...
		tokens = StringUtil.breakStringList ( expression, " (,)", StringUtil.DELIM_SKIP_BLANKS );
	}
	else if ( expression.length() >= 2 ) {
		// Old style -w#... or -w #...
		if ((expression.length() == 2) || ((expression.length() > 2) &&	(expression.charAt(2) == ' ')) ){
			// Parse the whole thing...
			tokens = StringUtil.breakStringList ( expression," ,", StringUtil.DELIM_SKIP_BLANKS );
		}
		else {	// Parse from the 3rd character on and then insert a
			// dummy command...
			tokens = StringUtil.breakStringList (expression.substring(2),
				" ,", StringUtil.DELIM_SKIP_BLANKS );
			tokens.insertElementAt("-w",0);
		}
	}
	// Now the same token structure...
	if ( tokens == null ) {
		throw new Exception ("Bad command \"" + expression + "\"" );
	}
	int size = tokens.size();
	if ( size == 1 ) {
		// Set warning level to 1...
		Message.setWarningLevel( Message.TERM_OUTPUT, 1 );
		Message.setWarningLevel( Message.LOG_OUTPUT, 1 );
	}
	else if ( size == 2 ) {
		// Set warning level to same value for all...
		int warning = StringUtil.atoi(((String)tokens.elementAt(1)).trim());
		Message.setWarningLevel( Message.TERM_OUTPUT, warning );
		Message.setWarningLevel( Message.LOG_OUTPUT, warning );
	}
	else if ( size == 3 ) {
		// Set warning level to different values for log and display...
		int warning = StringUtil.atoi(((String)tokens.elementAt(1)).trim());
		Message.setWarningLevel( Message.TERM_OUTPUT, warning );
		warning = StringUtil.atoi(((String)tokens.elementAt(2)).trim());
		Message.setWarningLevel( Message.LOG_OUTPUT, warning );
	}
	else {	tokens = null;
		throw new Exception ("Bad command \"" + expression + "\"" );
	}
	// Clean up...
	tokens = null;
}

/**
Execute the stateModMax() command.
@param command Command to parse.
@exception Exception if there is an error.
*/
private void do_stateModMax ( String command_tag, String command_string, GenericCommand command )
throws Exception
{	String routine = "TSEngine.do_StateModMax";
    int warning_count = 0;
    int warning_level = 2;
    String message;
    
    CommandStatus status = command.getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);
    
	Vector tokens = StringUtil.breakStringList ( command_string,
		" (,)", StringUtil.DELIM_SKIP_BLANKS|StringUtil.DELIM_ALLOW_STRINGS );
	if ( tokens.size() != 3 ) {
		throw new Exception ( "Bad command \"" + command_string + "\"" );
	}
	String infile1 = ((String)tokens.elementAt(1)).trim();
    String infile1_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(__ts_processor),infile1));
	int interval1 = StateMod_TS.getFileDataInterval ( infile1_full );
	Vector tslist1 = null;
	String infile2 = ((String)tokens.elementAt(2)).trim();
    String infile2_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(__ts_processor),infile2));
	int interval2 = StateMod_TS.getFileDataInterval ( infile2_full );
	Vector tslist2 = null;
	// Intervals must be the same...
	if ( interval1 != interval2 ) {
        message = "Data intervals for files are not the same:\n" + "\"" + command_string + "\"";
        Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag(command_tag,
                ++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the StateMod data files have the same interval for data." ) );
        throw new CommandException ( message );
	}
	Message.printStatus ( 1, routine, "Reading StateMod file \"" + infile1_full + "\"" );
	try {	tslist1 = StateMod_TS.readTimeSeriesList ( infile1_full,
			__InputStart_DateTime, __InputEnd_DateTime, null, true );
	}
	catch ( Exception e ) {
		message = "Error reading StateMod file \"" + infile1_full + "\".";
        Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag(command_tag,
                ++warning_count), routine, message );
        Message.printWarning(3, routine, e);
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the file is a valid StateMod time series file." ) );
        throw new CommandException ( message );
	}
	Message.printStatus ( 2, routine, "Reading StateMod file \"" + infile2_full + "\"" );
	try {	tslist2 = StateMod_TS.readTimeSeriesList ( infile2_full,
			__InputStart_DateTime, __InputEnd_DateTime, null, true );
	}
	catch ( Exception e ) {
		message = "Error reading StateMod file \"" + infile2_full + "\".";
        Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag(command_tag,
                ++warning_count), routine, message );
        Message.printWarning(3, routine, e);
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the file is a valid StateMod time series file." ) );
        throw new CommandException ( message );
	}
	// Process the time series to clean up.  This extends the periods to the
	// output period if necessary...
	readTimeSeries2 ( tslist1, true );
	// Now loop through the time series in the first list and compare with
	// the matching time series in the second list, saving the maximum at
	// each time step...
	int vsize = 0;
	if ( tslist1 != null ) {
		vsize = tslist1.size();
	}
	TS ts1 = null;
	int pos = 0;
	for ( int iv = 0; iv < vsize; iv++ ) {
		// Get a time series...
		ts1 = (TS)tslist1.elementAt(iv);
		if ( ts1 == null ) {
			continue;
		}
		// Find the same time series in the second list...
		pos = TSUtil.indexOf (	tslist2, ts1.getLocation(),	"Location", 1 );
		if ( pos < 0 ) {
			message = "Cannot find matching 2nd time series for \"" +
			ts1.getLocation() + "\" in \"" + infile2 + "\"";
            Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag(command_tag,
                    ++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that the files contain matching time series identifiers." ) );
		}
		else {	// The "ts1" instance will be modified..
			TSUtil.max ( ts1, (TS)tslist2.elementAt(pos) );
		}
	}
	// Now add the time series to the end of the normal list...
	Message.printStatus ( 2, routine, "Created " + vsize + " StateModMax() time series" );
	readTimeSeries2 ( tslist1, true );
	int ts_pos = getTimeSeriesSize();
	for ( int iv = 0; iv < vsize; iv++ ) {
		setTimeSeries ( (TS)tslist1.elementAt(iv), (ts_pos + iv) );
	}
	// Free resources from StateMod list...
	ts1 = null;
	tokens = null;
	infile1 = null;
	infile2 = null;
	tslist1 = null;
	tslist2 = null;
	// Force a garbage collect because this is an intensive task...
	System.gc();
    if ( warning_count > 0 ) {
        message = "There were " + warning_count + " warnings processing the command.";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(
            command_tag, ++warning_count),
            routine,message);
        throw new CommandWarningException ( message );
    }
    status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
Execute the writeNwsCard() command.
@param command Command to parse.
@exception Exception if there is an error.
*/
private void do_writeNwsCard ( String command )
throws Exception
{	String routine = "TSEngine.do_writeNwsCard";
	Vector tokens = StringUtil.breakStringList ( command,
		" (,)", StringUtil.DELIM_SKIP_BLANKS|
		StringUtil.DELIM_ALLOW_STRINGS );
	if ( tokens.size() != 2 ) {
		throw new Exception ( "Bad command \"" + command + "\"" );
	}
	String outfile = ((String)tokens.elementAt(1)).trim();
    String outfile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(__ts_processor),outfile));
	Message.printStatus ( 1, routine, "Writing NWS Card file \"" + outfile_full + "\"" );
	// Only write the first time series...
	TS tsout = (TS)__tslist.elementAt(0);
	NWSCardTS.writeTimeSeries ( tsout, outfile_full, __OutputStart_DateTime, __OutputEnd_DateTime, "", true );
	tokens = null;
}

/**
Execute the writeStateCU() command.
@param command Command to parse.
@param comments Comments for output header.
@exception Exception if there is an error.
*/
private void do_writeStateCU ( String command_tag, String command_string, String[] comments, GenericCommand command )
throws Exception
{	String routine = "TSEngine.do_writeStateCU";
    int warning_count = 0;
    int warning_level = 2;
    String message;

    CommandStatus status = command.getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);

	Vector tokens = StringUtil.breakStringList ( command_string,
		" (,)", StringUtil.DELIM_SKIP_BLANKS|
		StringUtil.DELIM_ALLOW_STRINGS );
	if ( (tokens.size() != 2) && (tokens.size() != 3) ) {
		throw new Exception ( "Bad command \"" + command + "\"" );
	}
	String outfile = ((String)tokens.elementAt(1)).trim();
    String outfile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(__ts_processor),outfile) );
	
	try {
        StateCU_TS.writeFrostDatesFile ( __tslist, outfile_full,
			comments, __OutputStart_DateTime, __OutputEnd_DateTime);
	}
	catch ( Exception e ) {
        message = "Unexpected error writing StateCU file \"" + outfile_full + "\".";
        Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag(command_tag,
                ++warning_count), routine, message );
        Message.printWarning(3, routine, e);
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Check the log file." ) );
        throw new CommandException ( message );
	}
    status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
Execute the shift() command.
@param command Command to parse.
@exception Exception if there is an error.
*/
private void do_shift ( String command )
throws Exception
{	String routine = "TSEngine.do_shift";
	Vector tokens = StringUtil.breakStringList ( command,
		" (,)", StringUtil.DELIM_SKIP_BLANKS );
	if ( tokens.size() != 4 ) {
		throw new Exception ( "Bad command \"" + command + "\"" );
	}
	// Parse the name and dates...
	String alias = (String)tokens.elementAt(1);
	DateTime olddate = (DateTime)__datetime_Hashtable.get(
		(String)tokens.elementAt(2));
	if ( olddate == null ) {
		Message.printStatus(1,routine, "Unable to look up date \"" +
		(String)tokens.elementAt(2) + "\"" );
		olddate = DateTime.parse ( (String)tokens.elementAt(2) );
	}
	DateTime newdate = (DateTime)__datetime_Hashtable.get((String)tokens.elementAt(3));
	if ( newdate == null ) {
		Message.printStatus(1,routine, "Unable to look up date \"" +
		(String)tokens.elementAt(3) + "\"" );
		newdate = DateTime.parse ( (String)tokens.elementAt(3) );
	}
	if ( Message.isDebugOn ) {
		Message.printDebug ( 1, routine, "Shifting TS \"" + alias +
		"\" from " + olddate.toString() + " to " + newdate.toString() );
	}
	int ts_pos = indexOf ( alias );
	if ( ts_pos >= 0 ) {
		TS ts = TSUtil.shift ( getTimeSeries(ts_pos),
			newdate, olddate ); //olddate, newdate );
		processTimeSeriesAction ( UPDATE_TS, ts, ts_pos );
	}
	else {	String message = "Unable to find time series \"" +
			alias + "\" for shift() command.";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}
}

/**
Helper method for shiftTimeByInterval() command.
@param command Command being evaluated.
@exception Exception if there is an error processing the time series.
*/
private TS do_shiftTimeByInterval ( String command )
throws Exception
{	// Don't parse with spaces because a TEMPTS may be present.
	Vector v = StringUtil.breakStringList(command,
		"(),\t", StringUtil.DELIM_SKIP_BLANKS); 
	String message, routine = "TSEngine.do_shiftTimeByInterval";
	if ( (v == null) || (v.size() < 4) || (v.size()%2 != 0) ) {
		message = "Syntax error in \"" + command + "\"";
		Message.printWarning ( 2, routine, message );
		throw new Exception ( message );
	}
	// Get the individual tokens of the expression...
	String dependent = ((String)v.elementAt(1)).trim();
	// Make sure there are time series available to operate on...
	int ts_pos = indexOf ( dependent );
	TS dependentTS = getTimeSeries ( ts_pos );
	if ( dependentTS == null ) {
		message = "Unable to find time series \"" + dependent +
		"\" for shiftTimeByInterval().";
		Message.printWarning ( 1, routine, message );
		throw new Exception ( message );
	}
	// Get the interval offsets and weights...
	int intervals[] = new int[(v.size() - 2)/2];
	double weights[] = new double[(v.size() - 2)/2];
	int npairs = 0;
	for ( int i = 2; i < v.size(); i++ ) {
		if ( (i%2) == 0 ) {
			intervals[npairs] = StringUtil.atoi (
				(String)v.elementAt(i) );
		}
		else {	weights[npairs++] = StringUtil.atod (
				(String)v.elementAt(i) );
		}
	}
	// Fill the dependent time series for the analysis period...
	TS ts = TSUtil.shiftTimeByInterval ( dependentTS, intervals, weights );
	processTimeSeriesAction ( UPDATE_TS, ts, ts_pos );
	return ts;
}

/**
Free memory for garbage collection.
@exception Throwable if there is an error.
*/
protected void finalize ()
throws Throwable
{	__AverageStart_DateTime = null;
	__AverageEnd_DateTime = null;
	__binary_ts_file = null;
	if ( getBinaryTSUsed() ) {
		// Free the binary file...
		if ( __binary_ts != null ) {
			__binary_ts.delete ();
			__binary_ts = null;
		}
	}
	__datetime_Hashtable = null;
	__OutputStart_DateTime = null;
	__OutputEnd_DateTime = null;
	__InputStart_DateTime = null;
	__InputEnd_DateTime = null;
	__binaryts_date1 = null;
	__binaryts_date2 = null;
	__fill_pattern_ts = null;
	__hbdmi_Vector = null;
	__missing_range = null;
	__missing_ts = null;
	// TODO SAM 2007-02-18 Need to enable NDFD
	//__NDFDAdapter_Vector = null;
	__output_file = null;
	__reference_date = null;
	__tslist = null;
	super.finalize();
}

/**
Format comments for the header of output files.  The comments
include the commands file and database version information.
@param commands The commands that are being processed.
@param include_commands Indicate whether the commands should be included
in the comments.  IOUtil.printCreatorHeader will automatically include the
commands if a commands file has been set but is probably better to move
to a case where the commands are passed to the write methods.
*/
protected String [] formatOutputHeaderComments ( Vector commands )
{	Vector comments = new Vector();
	// Commands.  Show the file name but all commands may be in memory.
	comments.addElement (
		"--------------------------------------------------" +
			"---------------------" );
	String commands_filename = __ts_processor.getCommandsFileName();
	if ( commands_filename == null ) {
		comments.addElement ( "Commands file name:  COMMANDS NOT SAVED TO FILE" );
	}
	else { comments.addElement ( "Commands file name: \"" + commands_filename + "\"" );
	}
	comments.addElement ( "Commands: " );
	int size_commands = commands.size();
	for ( int i = 0; i < size_commands; i++ ) {
		comments.addElement ( ((Command)commands.elementAt(i)).toString() );
	}
	// Save information about data sources.
	HydroBaseDMI hbdmi = null;
	int hsize = __hbdmi_Vector.size();
	String db_comments[] = null;
	for ( int ih = 0; ih < hsize; ih++ ) {
		hbdmi = (HydroBaseDMI)__hbdmi_Vector.elementAt(ih);
		if ( hbdmi != null ) {
			try {	db_comments = hbdmi.getVersionComments ();
			}
			catch ( Exception e ) {
				db_comments = null;
			}
		}
		if ( db_comments != null ) {
			for ( int i = 0; i < db_comments.length; i++ ) {
				comments.addElement(db_comments[i]);
			}
		}
	}
	// Convert to an array...
	int size = comments.size();
	String [] comments_array = new String[size];
	for ( int i = 0; i < size; i++ ) {
		comments_array[i] = (String)comments.elementAt(i);
	}
	return comments_array;
}

/**
Indicate whether a time series' data period should automatically be extended
to the output period (to allow for filling).
@return True if the period should be automatically extended.
*/
protected boolean getAutoExtendPeriod()
{   return __AutoExtendPeriod_boolean;
}

/**
Return the average period end, or null if all available data are to be used.
@return the average period end, or null if all available data are to be used.
*/
protected DateTime getAverageEnd()
{   return __AverageEnd_DateTime;
}

/**
Return the average start, or null if all available data are to be used.
@return the average period start, or null if all available data are to be used.
*/
protected DateTime getAverageStart()
{   return __AverageStart_DateTime;
}

/**
Get the BinaryTS reference.  This should be used only to call the BinaryTS
get*() methods.
@return the BinaryTS associated with the TSEngine, or null if not used.
*/
private BinaryTS getBinaryTS ()
{	return __binary_ts;
}

/**
Indicate whether a BinaryTS is used for temporary data.
@return True if a BinaryTS is being used.
*/
protected boolean getBinaryTSUsed()
{   return __BinaryTSUsed_boolean;
}

/**
Return the list of DataTest.
@return Vector of DataTest that can be used for data test commands.
*/
protected Vector getDataTestList ()
{	return __datatestlist;
}

/**
Get a date/time from a string.  This is done using the following rules:
<ol>
<li>	If the string is null, "*" or "", return null.</li>
<li>	If the string uses a standard name InputStart (QueryStart),
	InputEnd (QueryEnd), OutputStart, OutputEnd,
	return the corresponding DateTime.</li>
<li>	Check the date/time hash table for user-defined date/times.
<li>	Parse the string.
</ol>
@param date_string Date/time string to parse.
@exception if the date cannot be determined using the defined procedure.
*/
protected DateTime getDateTime ( String date_string )
throws Exception
{	if (	(date_string == null) ||
		date_string.equals("") || date_string.equals("*") ) {
		// Want to use all available...
		return null;
	}

	// Check for user DateTime instances...

	DateTime date = (DateTime)__datetime_Hashtable.get ( date_string );
	if ( date != null ) {
		// Found date in the hash table so use it...
		return date;
	}

	// Check for named DateTime instances...

	if (	date_string.equalsIgnoreCase("OutputEnd") ||
		date_string.equalsIgnoreCase("OutputPeriodEnd") ) {
		return __OutputEnd_DateTime;
	}
	else if(date_string.equalsIgnoreCase("OutputStart") ||
		date_string.equalsIgnoreCase("OutputPeriodStart") ) {
		return __OutputStart_DateTime;
	}
	else if(date_string.equalsIgnoreCase("InputEnd") ||
		date_string.equalsIgnoreCase("QueryEnd") ||
		date_string.equalsIgnoreCase("QueryPeriodEnd") ) {
		return __InputEnd_DateTime;
	}
	else if(date_string.equalsIgnoreCase("InputStart") ||
		date_string.equalsIgnoreCase("QueryStart") ||
		date_string.equalsIgnoreCase("QueryPeriodStart") ) {
		return __InputStart_DateTime;
	}

	// Else did not find a date time so try parse the string...

	return DateTime.parse ( date_string );
}

/**
Return an empty time series.
@return An empty time series.  The time series will have the output period set
by the setOutputPeriod() command or no data if the output period is not set.
Because little can be assumed for the time series, information like units, etc.
may not be set correctly (units currently default to "ACFT" and the data type
is taken from the current GUI choice).
@exception Exception if the time series identifier is bad.
*/
private MonthTS getEmptyTimeSeries ( String tsident_string )
throws Exception
{	MonthTS ts = new MonthTS ();
	// Allocate the data space...  If the dates have been set, use them.
	// Otherwise, leave the data as null so that it will return missing
	// when accessed.
	if ( haveOutputPeriod() ) {
		ts.setDate1 ( __OutputStart_DateTime );
		ts.setDate2 ( __OutputEnd_DateTime );
		ts.allocateDataSpace ();
	}
	// Now set the rest of the information...
	TSIdent ident = new TSIdent ( tsident_string );
	ts.setIdentifier ( ident );
	ts.setDescription ( "Empty time series" );
	return ts;
}

/**
Get a list of graph identifiers from a list of commands.  See documentation
for fully loaded method.  The list is not sorted
@param commands Time series commands to search.
@return list of graph identifiers or an empty non-null Vector if nothing found.
*/
private static Vector getGraphIdentifiersFromCommands ( Vector commands )
{	return getGraphIdentifiersFromCommands ( commands, false );
}

/**
Get a list of graph identifiers from a list of commands.  Commands that
start with "Graph ? = " are returned.
These strings are suitable for drop-down lists, etc.
@param commands Time series commands to search.
@param sort Should output be sorted by identifier.
@return list of graph identifiers or an empty non-null Vector if nothing found.
*/
private static Vector getGraphIdentifiersFromCommands ( Vector commands,
						boolean sort )
{	if ( commands == null ) {
		return new Vector();
	}
	Vector v = new Vector ( 10, 10 );
	int size = commands.size();
	String command = null;
	Vector tokens = null;
	for ( int i = 0; i < size; i++ ) {
		command = ((String)commands.elementAt(i)).trim();
		if (	(command == null) ||
			command.startsWith("#") ||
			(command.length() == 0) ) {
			// Make sure comments are ignored...
			continue;
		}
		else if ( command.regionMatches(true,0,"Graph ",0,6) ) {
			// Use the alias...
			tokens = StringUtil.breakStringList(
				command.substring(6)," =",
				StringUtil.DELIM_SKIP_BLANKS);
			if ( (tokens != null) && (tokens.size() > 0) ) {
				v.addElement ( (String)tokens.elementAt(0) );
			}
			tokens = null;
		}
	}
	tokens = null;
	return v;
}

/**
Return the HydroBaseDMI that is being used.  Use a blank input name to get
the default.
@param input_name Input name for the DMI, can be blank.
@return the HydroBaseDMI that is being used (may return null).
*/
protected HydroBaseDMI getHydroBaseDMI ( String input_name )
{	int size = __hbdmi_Vector.size();
	if ( input_name == null ) {
		input_name = "";
	}
	HydroBaseDMI hbdmi = null;
	for ( int i = 0; i < size; i++ ) {
		hbdmi = (HydroBaseDMI)__hbdmi_Vector.elementAt(i);
		if ( hbdmi.getInputName().equalsIgnoreCase(input_name) ) {
			if ( Message.isDebugOn ) {
				Message.printDebug ( 1, "",
				"Returning HydroBaseDMI[" + i +"] InputName=\""+
				hbdmi.getInputName() + "\"" );
			}
			return hbdmi;
		}
	}
	return null;
}

/**
Return the list of HydroBaseDMI.
@return Vector of open HydroBaseDMI.
*/
protected Vector getHydroBaseDMIList ()
{	return __hbdmi_Vector;
}

/**
Indicate whether a values less than or equal zero should be excluded when computing historical averages.
@return True if values <= 0 should be excluded when computing historical averages.
*/
protected boolean getIgnoreLEZero()
{   return __IgnoreLEZero_boolean;
}

/**
Indicate whether missing time series should result in an empty time series (rather
than a warning and no time series).
@return True if blank time series should be generated when they cannot be read.
*/
protected boolean getIncludeMissingTS()
{   return __IncludeMissingTS_boolean;
}

/**
Return the input period end, or null if all available data are to be queried.
@return the input period end, or null if all available data are to be queried.
*/
protected DateTime getInputEnd()
{	return __InputEnd_DateTime;
}

/**
Return the input start, or null if all available data are to be read at input.
@return the input period start, or null if all available data are to be read.
*/
protected DateTime getInputStart()
{	return __InputStart_DateTime;
}

/**
Return the list of TSID strings for which time series could not be read.
@return the list of TSID strings for which time series could not be read.
*/
protected Vector getMissingTS()
{   return __missing_ts;
}

// TODO SAM 2006-07-13
// May need to fully qualify Adapter but for now there is no conflict with
// other packages.
/**
Return the NDFD Adapter that is being used.
@param input_name Input name for the adapter, can be blank.
@return the NDFD Adapter that is being used (may return null).
*/

// TODO KAT 2006-10-30
// Commenting this out for now to get it to compile
// Danny will be adding service stuff in the future
// which should replace this ...
/**
public Adapter getNDFDAdapter (String input_name )
{	int size = __NDFDAdapter_Vector.size();
	if ( input_name == null ) {
		input_name = "";
	}
	Adapter adapter = null;
	for ( int i = 0; i < size; i++ ) {
		adapter = (Adapter)__NDFDAdapter_Vector.elementAt(i);
		/* TODO SAM 2006-07-15
		Currently the adapters don't have and ID or name so for
		now return the first instance.
		if ( adapter.getInputName().equalsIgnoreCase(input_name) ) {
			if ( Message.isDebugOn ) {
				Message.printDebug ( 1, "",
				"Returning NDFD Adapter[" + i +"] InputName=\""+
				adapter.getInputName() + "\"" );
			}
			return adapter;
		}
		
		return adapter;
	}
	return null;
}
*/

/**
Return the NWSRFS_DMI that is being used.  Use a blank input name to get
the default.
@param input_name Input name for the DMI, can be blank.  Typically is the
path to the FS5Files.
@return the NWSRFS_DMI that is being used (may return null).
*/
protected NWSRFS_DMI getNWSRFSFS5FilesDMI ( String input_name, boolean open_if_not_found )
{	String routine = getClass().getName() + ".getNWSRFSFS5FilesDMI";
	int size = __nwsrfs_dmi_Vector.size();
	if ( input_name == null ) {
		input_name = "";
	}
	NWSRFS_DMI nwsrfs_dmi = null;
	for ( int i = 0; i < size; i++ ) {
		nwsrfs_dmi = (NWSRFS_DMI)__nwsrfs_dmi_Vector.elementAt(i);
		if ( nwsrfs_dmi.getInputName().equalsIgnoreCase(input_name) ) {
			if ( Message.isDebugOn ) {
				Message.printDebug ( 1, routine,
				"Returning NWSRFS_DMI[" + i +"] InputName=\""+
				nwsrfs_dmi.getInputName() + "\"" );
			}
			return nwsrfs_dmi;
		}
	}
	if ( Message.isDebugOn ) {
		Message.printDebug ( 1, "",
		"Could not find a matching NWSRFS FS5Files DMI for InputName=\""+
		input_name + "\"" );
	}
	if ( open_if_not_found ) {
			try {	Message.printStatus( 2, routine,
					"Opening new NWSRFS FS5Files DMI using path \"" +
					input_name + "\"" );
					nwsrfs_dmi = new NWSRFS_DMI( input_name );
					nwsrfs_dmi.open();
					// Save so we can get to it again when we need it...
					setNWSRFSFS5FilesDMI ( nwsrfs_dmi, true );
					return nwsrfs_dmi;
			}
			catch ( Exception e ) {
				Message.printWarning ( 3, routine,
						"Could not open NWSRFS FS5Files." );
				Message.printWarning(3, routine, e);
			}
	}
	return null;
}


/**
Indicate whether output should include detailed time series information in headers.
@return True if the output should include detailed headers.
*/
protected boolean getOutputDetailedHeader()
{   return __OutputDetailedHeader_boolean;
}

/**
Return the output period end, or null if all data are to be output.
@return the output period end, or null if all data are to be output.
*/
protected DateTime getOutputEnd()
{	return __OutputEnd_DateTime;
}

/**
Return the output period start, or null if all data are to be output.
@return the output period start, or null if all data are to be output.
*/
protected DateTime getOutputStart()
{	return __OutputStart_DateTime;
}

/**
Return the output year type, to be used for commands that create output.
@return the output year type, as "Calendar" or "Water".
*/
protected String getOutputYearType()
{	int OutputYearType_int = getOutputYearTypeInt();
	if ( OutputYearType_int == __CALENDAR_YEAR ) {
		return "Calendar";
	}
	else if ( OutputYearType_int == __WATER_YEAR ) {
		return "Water";
	}
	else {
		return "";
	}
}

/**
Return the output year type as an internal integer, to be used for commands that create output.
@return the output year type, as "Calendar" or "Water".
*/
private int getOutputYearTypeInt()
{   
    return __OutputYearType_int;
}

/**
Indicate whether output that is exported should be previewed first.
@return True if output that is exported should be previewed first..
*/
protected boolean getPreviewExportedOutput()
{   return __PreviewExportedOutput_boolean;
}

/**
Return a Vector of the selected time series, or all time series if none are
specifically selected.
@return the selected time series (those where ts.setSelected(true) has been
called).
@param return_all_if_none_selected If true return all if no time series happen
to be selected.
*/
private Vector getSelectedTimeSeriesList ( boolean return_all_if_none_selected )
{	Vector selected = new Vector();
	int size = 0;
	if ( __tslist != null ) {
		size = __tslist.size();
	}
	TS ts = null;
	for ( int i = 0; i < size; i++ ) {
		ts = (TS)__tslist.elementAt(i);
		if ( ts.isSelected() ) {
			selected.addElement ( ts );
		}
	}
	if ( (selected.size() == 0) && return_all_if_none_selected ) {
		// Nothing selected but requested to return all...
		selected = null;
		return __tslist;
	}
	else {	// Return what we have...
		return selected;
	}
}

/**
Return a Vector of the specified time series, determined by matching the
specified time series identifiers with the available time series in memory.
@return the specified time series (those whose TSIdent match).  The result
is guaranteed to be non-null but may be empty.
@param command_tag Command number used for messaging.
@param tsids A Vector of TSID strings to match.
@param routine The routine that is calling this helper method (for messaging).
@param command Command that is being processed (for messaging).
*/
/* TODO SAM 2007-02-08
Is this needed?
private Vector getSpecifiedTimeSeries ( String command_tag, Vector tsids,
					String routine, String command )
throws Exception
{	int size = 0;
	if ( tsids != null ) {
		size = tsids.size();
	}
	Vector v = new Vector ( size );
	String tsid = null;
	TS ts = null;
	int warning_count = 0;
	for ( int its = 0; its < size; its++ ) {
		tsid = ((String)v.elementAt(its)).trim();
		// The following may or may not have TEMPTS at the
		// front but is handled transparently by getTimeSeries.
		try {	ts = getTimeSeries ( command_tag, tsid );
		}
		catch ( Exception e ) {
			ts = null;	// to trigger next check and message
		}
		if ( ts == null ) {
			Message.printWarning ( 2, routine,
			"Unable to find time series \"" +
			tsid + "\" for\n\"" + command + "\"." );
			++warning_count;
		}
		else {	v.addElement ( tsid );
		}
	}
	if ( warning_count > 0 ) {
		throw new Exception (
		"Error finding one or more time series in \"" +
			command + "\"" );
	}
	return v;
}
*/

/**
Return a time series from either the __tslist vector or the BinaryTS file,
as appropriate.  If a BinaryTS is returned, it is a new instance from the file
and should be set to null when done.
The search is performed backwards in the
list, assuming that the commands are being processed sequentially and therefore
any reference to a duplicate ID would intuitively be referring to the latest
instance in the list.  For this version of the method, the trace (sequence
number) is ignored.
@param id Time series identifier (either an alias or TSIdent string).
@return a time series from the requested position or null if none is available.
@exception Exception if there is an error getting the time series.
*/
protected TS getTimeSeries ( String command_tag, String id )
throws Exception
{	return getTimeSeries ( command_tag, id, -1 );
}

/**
Return a time series from either the __tslist vector or the BinaryTS file,
as appropriate.  If a BinaryTS is returned, it is a new instance from the file
and should be set to null when done.
The search is performed backwards in the
list, assuming that the commands are being processed sequentially and therefore
any reference to a duplicate ID would intuitively be referring to the latest
instance in the list.
@param id Time series identifier (either an alias or TSIdent string).
@param sequence_number If >= 0, the sequence number of the time series is
also checked to make a match.  Currently this is only done for in-memory time
series and not BinaryTS.
@return a time series from the requested position or null if none is available.
@exception Exception if there is an error getting the time series.
*/
protected TS getTimeSeries ( String command_tag, String id, int sequence_number )
throws Exception
{	String tsident = id.trim();
	if ( tsident.regionMatches(true,0,TEMPTS,0,6) ) {
		// Temporary time series.  Read it, mark as temporary, and
		// return...
		Message.printStatus ( 1, "TSEngine.getTimeSeries",
		"Reading temporary time series \"" +
		tsident.substring(6).trim() );
		TS ts = readTimeSeries ( 2, command_tag,
			tsident.substring(6).trim() );
		if ( ts != null ) {
			ts.setStatus ( TEMPTS );
		}
		return ts;
	}
	else {	// Expect the time series to be in memory or BinaryTS file...
		int pos = indexOf ( id, sequence_number );
		if ( pos < 0 ) {
			return null;
		}
		return getTimeSeries ( pos );
	}
}

/**
Return a time series from either the __tslist vector or the BinaryTS file,
as appropriate.  If a BinaryTS is returned, it is a new instance from the file
and should be set to null when done.
@param position Position in time series list (0 index).
@return a time series from the requested position or null if none is available.
@exception Exception if there is an error reading the time series.
*/
protected TS getTimeSeries ( int position )
throws Exception
{	if ( position < 0 ) {
		return null;
	}
	if ( getBinaryTSUsed() ) {
		if ( __binary_ts != null ) {
			return __binary_ts.readTimeSeries ( position );
		}
	}
	else {	if ( __tslist == null ) {
			return null;
		}
		else if ( position > __tslist.size() ) {
			return null;
		}
		return (TS)__tslist.elementAt(position);
	}
	return null;
}

/**
Return the list of time series.  See also getSelectedTimeSeriesList().
@return the list of time series.  The list can be null or zero size.
@param indices a list of indices to return or null to return all.  Only indices
within the time series list size will be returned.
*/
protected Vector getTimeSeriesList ( int [] indices )
{	if ( indices == null ){
		return __tslist;
	}
	else {	// Return only the requested indices...
		Vector v = new Vector ();
		int size = 0;
		if ( __tslist != null ) {
			size = __tslist.size();
		}
		for ( int i = 0; i < indices.length; i++ ) {
			if ( indices[i] < size ) {
				v.addElement ( __tslist.elementAt(indices[i]) );
			}
		}
		return v;
	}
}

/**
Return number of time series that have been processed and are available for
output.  The size is determined from the in-memory time series list or the
BinaryTS, as appropriate.
@return number of time series available for output.
*/
protected int getTimeSeriesSize ()
{	if ( getBinaryTSUsed() ) {
		return __binary_ts.size();
	}
	else {	return __tslist.size();
	}
}

/**
Return the list of time series to process, based on information that indicates
how the list can be determined.
@param TSList Indicates how the list of time series for processing is to be
determined, with one of the following values:
<ol>
<li>	"AllTS" will result in true being returned.</li>
<li>	"AllMatchingTSID" will use the TSID value to match time series.</li>
<li>	"LastMatchingTSID" will use the TSID value to match time series,
	returning the last match.  This is necessary for backward compatibility.
	</li>
<li>	"SelectedTS" will result in true being returned only if the
	time series is selected.</li>
</ol>
@return A Vector that has as its first element a Vector of TS to process and as
its second element an int[] indicating the positions in the time series list,
to be used to update the time series.  Use the size of the Vector (in the first
element) to determine the number of time series to process.  The order of the
time series will be from first to last.  A non-null list is guaranteed to be
returned.
*/
protected Vector getTimeSeriesToProcess ( String TSList, String TSID )
{	Vector tslist = new Vector();	// List of time series to process
	int nts = getTimeSeriesSize(); // OK to be zero for logic below - zero size array will result.
	int [] tspos = new int[nts];	// Positions of time series to process.
					// Size to match the full list but may
					// not be filled initially - trim before returning.
	// Setup the return data...
	Vector v = new Vector ( 2 );
	v.addElement ( tslist );
	v.addElement ( tspos );
	// Loop through the time series in memory...
	int count = 0;
	TS ts = null;
	Message.printStatus( 2, "", "TSList=\"" + TSList + "\" TSID=\"" + TSID + "\"");
	if ( TSList.equalsIgnoreCase("LastMatchingTSID") ) {
		// Search backwards for the last single matching time series...
		for ( int its = (nts - 1); its >= 0; its-- ) {
			try {	ts = getTimeSeries ( its );
			}
			catch ( Exception e ) {
				// Don't add...
				continue;
			}
			if ( TSID.indexOf("~") > 0 ) {
				// Include the input type...
				if (ts.getIdentifier().matches(TSID,true,true)){
					tslist.addElement ( ts );
					tspos[count++] = its;
					// Only return the single index...
					int [] tspos2 = new int[1];
					tspos2[0] = tspos[0];
					v.setElementAt(tspos2,1);
					// Only want one match...
					return v;
				}
			}
			else {	// Just check the main information...
				if(ts.getIdentifier().matches(TSID,true,false)){
					tslist.addElement ( ts );
					tspos[count++] = its;
					// Only return the single index...
					int [] tspos2 = new int[1];
					tspos2[0] = tspos[0];
					v.setElementAt(tspos2,1);
					// Only want one match...
					return v;
				}
			}
		}
	}
	// Else will loop through all the time series from first to last and
	// find matches...
	boolean found = false;
	for ( int its = 0; its < nts; its++ ) {
		found = false;
		try {	ts = getTimeSeries ( its );
		}
		catch ( Exception e ) {
			// Don't add...
			continue;
		}
		if ( TSList.equalsIgnoreCase(__AllTS) ) {
			found = true;
		}
		else if( TSList.equalsIgnoreCase(__SelectedTS) &&
			ts.isSelected() ) {
			found = true;
		}
		else if ( TSList.equalsIgnoreCase(__AllMatchingTSID) ) {
			if ( TSID.indexOf("~") > 0 ) {
				// Include the input type...
				if (ts.getIdentifier().matches(TSID,true,true)){
					found = true;
				}
			}
			else {	// Just check the main information...
				if(ts.getIdentifier().matches(TSID,true,false)){
					found = true;
				}
			}
		}
		if ( found ) {
			// Add the time series and increment the count...
			tslist.addElement ( ts );
			tspos[count++] = its;
		}
	}
	// Trim down the "tspos" array to only include matches so that other
	// code does not mistakenly iterate through a longer array...
	if ( count == 0 ) {
		v.setElementAt ( new int[0], 1 );
	}
	else {
		int [] tspos2 = new int[count];
		for ( int i = 0; i < count; i++ ) {
			tspos2[i] = tspos[i];
		}
		v.setElementAt ( tspos2, 1 );
	}
	Message.printStatus( 2, "", tslist.toString() );
	return v;
}

/**
Get a list of time series identifiers for traces from a list of commands.
See documentation for fully loaded method.  The list is not sorted
@param commands Time series commands to search.
@return list of time series identifiers or an empty non-null Vector if nothing
found.
*/
protected static Vector getTraceIdentifiersFromCommands ( Vector commands )
{	return getTraceIdentifiersFromCommands ( commands, false );
}

/**
Get a list of time series identifiers for traces from a list of commands.
Time series identifiers from createTraces() commands are returned.
These strings are suitable for drop-down lists, etc.
@param commands Time series commands to search.
@param sort Should output be sorted by identifier.
@return list of time series identifiers or an empty non-null Vector if nothing
found.
*/
protected static Vector getTraceIdentifiersFromCommands ( Vector commands, boolean sort )
{	if ( commands == null ) {
		return new Vector();
	}
	Vector v = new Vector ( 10, 10 );
	int size = commands.size();
	String command = null, string = null;
	Vector tokens = null;
	for ( int i = 0; i < size; i++ ) {
		command = ((String)commands.elementAt(i)).trim();
		if (	(command == null) ||
			command.startsWith("#") ||
			(command.length() == 0) ) {
			// Make sure comments are ignored...
			continue;
		}
		tokens = StringUtil.breakStringList(
			command," =(),",
			StringUtil.DELIM_SKIP_BLANKS );
		string = ((String)tokens.elementAt(0)).trim();
		if ( !string.regionMatches ( true,0,"createTraces",0,12) ) {
			// Not a command we are looking for...
			continue;
		}
		string = ((String)tokens.elementAt(1)).trim();
		if ( string.regionMatches ( true,0,"tempts",0,6) ) {
			// Assume the next item is the identifier...
			v.addElement ( ((String)tokens.elementAt(2)).trim() );
		}
		else {	// Assume this item is the identifier...
			v.addElement ( ((String)tokens.elementAt(1)).trim() );
		}
	}
	tokens = null;
	return v;
}

/**
Return a Vector of objects (currently open DMI instances) that implement
TSProductAnnotationProvider. This is a helper method for other methods.
@return a non-null Vector of TSProductAnnotationProviders.
*/
protected Vector getTSProductAnnotationProviders ()
{	Vector ap_Vector = new Vector();
	// Check the HydroBase instances...
	int hsize = __hbdmi_Vector.size();
	HydroBaseDMI hbdmi = null;
	for ( int ih = 0; ih < hsize; ih++ ) {
		hbdmi = (HydroBaseDMI)__hbdmi_Vector.elementAt(ih);
		if (	(hbdmi != null) &&
			(hbdmi instanceof TSProductAnnotationProvider)){
			ap_Vector.addElement ( hbdmi );
		}
	}
	// Check the ColoradoSMS instances...
	if (	(__smsdmi != null) &&
		(__smsdmi instanceof TSProductAnnotationProvider) ) {
		ap_Vector.addElement ( __smsdmi );
	}
	// Check the RiversideDB_DMI instances...
	if (	(__rdmi != null) &&
		(__rdmi instanceof TSProductAnnotationProvider) ) {
		ap_Vector.addElement ( __rdmi );
	}
	return ap_Vector;
}

/**
Return the TSSupplier name.
@return the TSSupplier name ("TSEngine").
*/
public String getTSSupplierName()
{	return "TSEngine";
}

/**
Return the WindowListener that wants to track TSView windows.
This is used when the TSCommandProcessor is run as a supporting
tool (e.g., in TSTool with no main GUI).
@return the WindowListener for TSView windows.
*/
protected WindowListener getTSViewWindowListener ()
{	return _tsview_window_listener;
}

/**
Determine whether the averaging period is set (start and end dates need to be
non-null and years not equal to zero).
@return true if the averaging period has been specified (and we can use its
dates without fear of nulls or zero years).
*/
private boolean haveAveragingPeriod ()
{	if ( (getAverageStart() == null) || (getAverageEnd() == null) ) {
		return false;
	}
	if ( (getAverageStart().getYear() == 0) || (getAverageEnd().getYear() == 0) ) {
		return false;
	}
	return true;
}

/**
Indicate whether the input period has been specified.
@return true if the input period has been specified (and we can use its
dates without fear of nulls or zero years).
*/
private boolean haveInputPeriod ()
{	if ( (__InputStart_DateTime == null) ||
		(__InputEnd_DateTime == null) ) {
		return false;
	}
	if ( (__InputStart_DateTime.getYear() == 0) ||
			(__InputEnd_DateTime.getYear() == 0) ){
		return false;
	}
	return true;
}

/**
Indicate whether the output period has been specified.
@return true if the output period has been specified (and we can use its
dates without fear of nulls or zero years).
*/
protected boolean haveOutputPeriod ()
{	if ((__OutputStart_DateTime == null) || (__OutputEnd_DateTime == null)){
		return false;
	}
	if (	(__OutputStart_DateTime.getYear() == 0) ||
		(__OutputEnd_DateTime.getYear() == 0) ) {
		return false;
	}
	return true;
}

/**
Indicate whether missing time series are automatically included in output.
@return true if missing time series are automatically included in output.
@deprecated use getIncludeMissingTS
*/
protected boolean includeMissingTS()
{	return getIncludeMissingTS();
}

/**
Return the position of a time series from either the __tslist vector or the
BinaryTS file, as appropriate.  See the overloaded method for full
documentation.  This version assumes that no sequence number is used.
@param string the alias and/or time series identifier to look for.
@return Position in time series list (0 index), or -1 if not in the list.
*/
protected int indexOf ( String string )
{	return indexOf ( string, -1 );
}

/**
Return the position of a time series from either the __tslist vector or the
BinaryTS file, as appropriate.  The search is done as follows:
<ol>
<li>	If string matches a TS alias matches, return the TS index.  This is
	the most specific match where an alias is being specified in the
	search string.</li>
<li>	Else, if the string does match a series identifier (using
	TSIdent.equals()), return the index.  This is
	a more general search where the string is an TS identifier and therefore
	safely does NOT match an alias.</li>
<li>	Return -1.</li>
</ol>
The search is performed backwards in the list, assuming that the commands are
being processed sequentially and therefore any reference to a duplicate ID would
intuitively be referring to the latest instance in the list.
@param string the alias and/or time series identifier to look for.
@param sequence_number If specified as >= 0, the sequence number is also
checked to find a match.  Currently this is only used for in-memory time series
(not BinaryTS).
@return Position in time series list (0 index), or -1 if not in the list.
*/
private int indexOf ( String string, int sequence_number )
{	// First search the aliases in the BinaryTS and in memory list...
	int pos = -1;
	if ( (string == null) || string.equals("") ) {
		return -1;
	}
	if ( getBinaryTSUsed() ) {
		if ( __binary_ts == null ) {
			return -1;
		}
		else {	pos = __binary_ts.indexOf ( string, "Alias", -1 );
		}
	}
	else {	if ( sequence_number >= 0 ) {
			pos = TSUtil.indexOf ( __tslist, string, "Alias",
						sequence_number, -1 );
		}
		else {	pos = TSUtil.indexOf ( __tslist, string, "Alias", -1 );
		}
	}
	if ( pos >= 0 ) {
		return pos;
	}
	// Now search the identifiers (can't totally rely on indexOf() because
	// the alias must also be empty)...
	int size = 0;
	if ( getBinaryTSUsed() ) {
		if ( __binary_ts == null ) {
			return -1;
		}
		else {	size = __binary_ts.size();
			for ( int i = (size - 1); i >= 0; i-- ) {
				try {	if (	__binary_ts.getIdentifier(i).equals( string) ) { //&&
						//_binary_ts.getAlias(i
						//).equals("") ) {}
						return i;
					}
				}
				catch ( Exception e ) {
					;	// Treat as not equal.
				}
			}
		}
	}
	else {	if ( __tslist == null ) {
			return -1;
		}
		else {	TS ts = null;
			size = __tslist.size();
			for ( int i = (size - 1); i >= 0; i-- ) {
				ts = (TS)__tslist.elementAt(i);
				if ( ts == null ) {
					continue;
				}
				if (	ts.getIdentifier().equals(string) ){//&&
					//ts.getAlias().equals("") ) {}
					if ( sequence_number >= 0 ) {
						if ( ts.getSequenceNumber() ==
							sequence_number ) {
							return i;
						}
					}
					else {	return i;
					}
				}
					
			}
		}
	}
	return -1;
}

/**
Initialize data for TSEngine.
@param hbdmi HydroBase DMI instance.
@param rdmi RiversideDB DMI instance.
@param DIADvisor_dmi DIADvisor DMI instance for operational database.
@param DIADvisor_archive_dmi DIADvisor DMI instance for archive database.
@param nwsrfs_dmi NWSRFS_DMI instance for NWSRFS FS5Files.
@param smsdmi SatMonSysDMI instance for ColoradoSMS input type.
@param commands Vector of commands to process (e.g., from commands file).
*/
/* FIXME SAM 2007-08-20 Remove if not used after rework
private void initialize (	HydroBaseDMI hbdmi, RiversideDB_DMI rdmi,
				DIADvisorDMI DIADvisor_dmi,
				DIADvisorDMI DIADvisor_archive_dmi,
				NWSRFS_DMI nwsrfs_dmi,
				SatMonSysDMI smsdmi,
				Vector commands )
{	// Add to the Vector.  Do not close the old connection because it may
	// be used in the application.
	setHydroBaseDMI ( hbdmi, false );
	__rdmi = rdmi;
	__smsdmi = smsdmi;
	__DIADvisor_dmi = DIADvisor_dmi;
	__DIADvisor_archive_dmi = DIADvisor_archive_dmi;
	__nwsrfs_dmi = nwsrfs_dmi;
	__tslist = new Vector ( 50, 50 );// Always allocate something since
					// we don't know how many time series
					// will be processed
	if ( IOUtil.isUNIXMachine() ) {
		_binary_ts_file = "/tmp/tstool.bts";
	}
	else {	_binary_ts_file = "C:\\temp\\tstool.bts";
	}
}
*/

/**
Indicate whether a BinaryTS is used.
@return true if a BinaryTS is used for time series, false if not.
*/
protected boolean isBinaryTSUsed ()
{	return __BinaryTSUsed_boolean;
}

/**
Determine whether a command parameter is valid.  This is used for common/shared
parameters.
@param parameter Parameter name.
@param value Parameter value to check.
@return true if a parameter value is valid.
*/
public boolean isParameterValid ( String parameter, String value )
{	if ( parameter.equalsIgnoreCase("TSList") ) {
		if (	value.equalsIgnoreCase(__AllTS) ||
			value.equalsIgnoreCase(__AllMatchingTSID) ||
			value.equalsIgnoreCase(__LastMatchingTSID) ||
			value.equalsIgnoreCase(__SelectedTS) ) {
			return true;
		}
		else {	return false;
		}
	}
	return false;
}

/**
Process the events from the MessageJDialog class.  If the "Cancel" button has
been pressed, then indicate that the time series processing should stop.
@param command If "Cancel", then a request will be made to cancel processing.
*/
public void messageJDialogAction ( String command )
{	if ( command.equalsIgnoreCase("Cancel") ) {
		__ts_processor.setCancelProcessingRequested(true);
	}
}

/**
Determine whether a historic average is needed.  This is called in
readTimeSeries().  If a historic average is needed, processing is slowed.
Currently, always check the _fillhistave_old and _fillhistave_new
settings, which are checked for in the processTimeSeriesCommands() method.
In the future, it may be possible to check the time series itself, compared
to the expression list.
ACTUALLY, ALWAYS RETURN TRUE BECAUSE THIS INFORMATION IS USEFUL IN THE TIME
SERIES PROPERTY DISPLAY.
*/
private boolean needHistoricalAverages ( TS ts )
{	// For now always return true...
	return true;
	//if ( _fillhistave_old || _fillhistave_new ) {
	//	return true;
	//}
	//else {	return false;
	//}
}

/**
Read an NWSRFS time series.  This code needs to be moved into the
RTi.DMI.NWSRFSDMI when it takes form.
The time series is generated by running the nwsrfssh script.  The nwsrfssh is
run via batch mode using the 'tsdata' command.  A sample command: <BR><I>
nwsrfssh batch "open ofs; tsdata PEQUENI QINE CMS; quit" </I>  
@return a time series matching the identifier.
@param tsident_string Time series identifier string.
@param req_date1 Requested start date for data.
@param req_date2 Requested end date for data.
@param req_units Requested data units.
@param read_data Indicates whether data should be read (true) or only header
information (false).
@exception Exception if there is an error reading the time series.
*/
public static TS NWSRFSDMI_readTimeSeries (	String tsident_string,
						DateTime req_date1,
						DateTime req_date2,
						String req_units,
						boolean read_data )
throws Exception
{	String routine = "NWSRFSDMI.readTimeSeries";
	int dl = 30;

	// Create DateValueTS data by running nwsrfssh.  Store the output in
	// a vector...

	if ( req_units == null ) {
		req_units = "";
	}
	TSIdent tsident = new TSIdent ( tsident_string );
	String shell_cmd = "nwsrfssh batch 'open ofs;tsdata " +
		tsident.getLocation() + " " + tsident.getType() + " " +
		req_units + ";quit'";
	tsident = null;

	if (Message.isDebugOn) {
		Message.printDebug(dl, routine, "nwsrfssh batch command: " +
		shell_cmd );
	}

	Vector dateValue_list = new Vector();
	try {	int exitstat = -999;
		ProcessManager pm = new ProcessManager(shell_cmd, 12000 );
		pm.saveOutput(true);
		pm.run();
		dateValue_list = pm.getOutputVector();
		exitstat = pm.getExitStatus();
		if (( exitstat != 0 ) || (dateValue_list.size()== 0))  {
			String message = "The \"" + shell_cmd +
				" command failed.";
			Message.printWarning(2, routine, message );
			throw new IOException ( message );
		}
		else  {	String firstline = null;
			firstline = (String)dateValue_list.firstElement();
			if (	(firstline == null) ||
				(firstline.indexOf("ERROR") != -1 ) ||
				(firstline.indexOf("WARN") != -1 ))  {
				String message = "The \"" + shell_cmd +
					" command failed.";
				Message.printWarning(2, routine, message );
				dateValue_list = null;
				throw new IOException ( message );
			}
		}
	}
	catch (Exception e ) {
		String message = "The \"" + shell_cmd + " command failed.";
		Message.printWarning(2, routine, message );
		dateValue_list = null;
		throw new IOException ( message );
	}
			
	// If the vector is still not null, remove that "Stop x" line at end of
	// the vector.

	if ( dateValue_list.size() > 0 ) {
		dateValue_list.removeElementAt(dateValue_list.size() - 1);
	}

	// Create a DateValue time series using the vector...

	TS ts = DateValueTS.readFromStringList ( dateValue_list, tsident_string,
						req_date1, req_date2, req_units,
						read_data );
	dateValue_list = null;
	// SAMX - should be done now...
	// Hard-code this for now - need to update the nwssrfssh to put NWSRFS
	// for the source...
	//if ( ts != null ) {
	//	ts.setIdentifier ( tsident_string );
	//}
	return ts;
}

/**
Process a list of commands.  This version supports the runCommands() command
via the TSCommandsProcessor.
@param commands Commands strings to process.
@param app_PropList if not null, set properties as commands are processed:
<table width=100% cellpadding=10 cellspacing=0 border=2>
<tr>
<td><b>Property</b></td>	<td><b>Description</b></td>
</tr>

<tr>
<td><b>WorkingDir</b></td>
<td>Will be set if a setWorkingDir() command is encountered.
</td>
<td>Working directory will not be set.</td>
</tr>

<tr>
<td><b>Recursive</b></td>
<td>If set to true, indicates that the commands string list being processed is
from a recursive call (e.g., when processing runCommands()).  Consequently, the
second commands list is processed, not the original one that started
processing.
</td>
<td>False</td>
</tr>
</table>
*/
/* TODO SAM 2007-08-20 Evaluate how to integrate with new design.
public void processCommands ( Vector commands, PropList app_PropList )
throws Exception
{	// Run in batch mode...
	processCommands ( (HydroBaseDMI)null, commands, app_PropList );
}
*/

/**
Process a tstool commands file.  This routine parses the commands file
and calls processCommands with the information found in that file.
This is only called when running in batch mode or from the GUI when running
a commands file without reading into the GUI.
@param hbdmi Database connection.
@param filename File name containing tstool commands.
*/
/* FIXME SAM 2007-08-20 Evaluate how to integrate with new design.
protected void processCommands ( HydroBaseDMI hbdmi, String filename )
throws Exception
{	// Run in batch mode...
	processCommands ( hbdmi, filename, true );
}
*/

// FIXME SAM 2007-08-20 Evaluate how to integrate with new design.
/**
Process a tstool commands file.  This routine parses the commands file
and calls processCommands with the information found in that file.
This is only called when running in batch mode or from the GUI when running
a commands file without reading into the GUI.
@param hbdmi HydroBase connection.
@param filename File name containing tstool commands.
@param is_batch Indicates whether a batch mode is run.  True means no GUI.
False means either a full (main GUI) or partial (plots only) GUI.
*/
/* FIXME SAM 2007-08-20 Need to enable in some form
private void processCommands (	HydroBaseDMI hbdmi, String filename,
				boolean is_batch )
throws Exception
{	String message, routine = "TSEngine.processCommands";
	IOUtil.isBatch ( is_batch );

	String iline;
	BufferedReader in = null;
	Vector cmdVec = null;
	try {	in = new BufferedReader ( new FileReader ( filename ));
		cmdVec = new Vector ( 10, 10 );
	}
	catch ( Exception e ) {
		message = "Error opening commands file \"" + filename + "\"";
		Message.printWarning ( 1, routine, message );
		throw new Exception ( message );
	}

	// add each line to the vector of commands
	//
	while (( iline = in.readLine()) != null ) {
		try { // try around each line...
		// If empty line, skip...

		if ( iline.trim().length() == 0 ) {
			// Blank line.  Do not add to list...
			continue;
		}

		// first remove any () around the time series, if they exist
		if ( iline.startsWith("(") && iline.endsWith(")"))
			iline = iline.substring(1, iline.length()-1);

		// next also remove any ' within the id
		if ( iline.indexOf('\'') >= 0 ) {
			StringBuffer sb = new StringBuffer();
			StringTokenizer st = new StringTokenizer ( iline, "'" );
			while ( st.hasMoreElements())
				sb.append ( st.nextToken());
			iline = "" + sb;
		}

		cmdVec.addElement ( iline );	
		}
		catch ( Exception e ) {
			Message.printWarning ( 1, routine,
			"Error processing command \"" + iline + "\"" );
		}
	}

	try {	processCommands ( cmdVec, null );
	} catch ( Exception e ) {
		message = "Error processing command strings.";
		Message.printWarning ( 1, routine, message );
		Message.printWarning ( 2, routine, e );
		throw new Exception ( message );
	}
}

/**
Process a set of TSTool commands.
@param commands Vector of Command instances to process.
@param app_PropList if not null, set properties as commands are processed:
<table width=100% cellpadding=10 cellspacing=0 border=2>
<tr>
<td><b>Property</b></td>	<td><b>Description</b></td>
</tr>

<tr>
<td><b>WorkingDir</b></td>
<td>Will be set if a setWorkingDir() command is encountered.
</td>
<td>Working directory will not be set.</td>
</tr>

<tr>
<td><b>Recursive</b></td>
<td>If set to true, indicates that the commands string list being processed is
from a recursive call (e.g., when processing runCommands()).  Consequently, the
second commands list is processed, not the original one that started
processing.
</td>
<td>False</td>
</tr>
</table>
@exception java.lang.Exception If there is an error in the commands file.
*/
/* FIXME SAM Need to reenable in some form
protected void processCommands ( Vector commands,
				PropList app_PropList )
throws Exception
{	String	routine = "TSEngine.processCommands";
	int	dl = 10;

	if ( commands == null ) {
		// This is OK because the TSEngine may be acting as a
		// TSSupplier...
		return;
	}
	int size = commands.size ();
	if ( Message.isDebugOn ) {
		Message.printDebug ( dl, routine, 
		"Processing commands of length " + size );
	}

	DateTime now = new DateTime (
		DateTime.DATE_CURRENT|DateTime.PRECISION_SECOND );
	Message.printStatus ( 1, routine,
		"Start processing commands at: " + now.toString() );
	/ * TODO SAM 2007-08-09 This should be unneeded as the commands
	 * are stored in the TSCommandProcessor list now and can be
	 * regenerated with toString().
	 * Remove when confirmed.
	if ( !IOUtil.isBatch() ) {
		// Running in the GUI and we need to put together a list of
		// commands as an array that will get saved in file output...
		_commands_array = new String[size + 1];
		_commands_array[0] = "# Commands from TSTool GUI:";
		for ( int i = 0; i < size; i++ ) {
			_commands_array[i + 1] = (String)commands.elementAt(i);
		}
	}
	* /

	// As of TSTool 05.xx.xx+, the time series expression list contains all
	// commands...
	boolean Recursive_boolean = false;
	if ( app_PropList != null ) {
		String Recursive = app_PropList.getValue ( "Recursive" );
		if ( (Recursive != null) && Recursive.equalsIgnoreCase("True")){
			Recursive_boolean = true;
		}
	}
	/ * FIXME SAM 2007-08-10 Need to reenable processing
	if ( Recursive_boolean ) {
		// A recursive call to the processing is occurring.  Save the
		// commands to process in a second list so that it won't step
		// on the original list and cause confusion...
		_tsexpression_list2 = commands;
	}
	else {	_tsexpression_list = commands;
	}
	* /

	// Now process the time series expressions...

	// FIXME SAM 2007-08-10 Need to enable in some form
	// processTimeSeriesCommands ( app_PropList );

	now = new DateTime ( DateTime.DATE_CURRENT|DateTime.PRECISION_SECOND );
	Message.printStatus ( 1, routine,
	"End processing commands at:   " + now.toString() );
}
*/

//TODO SAM 2006-05-02
//Need to phase out app_PropList or make the exchange of control information
//more robust
/**
Process a list of time series commands, resulting in a vector of time series
(and/or setting data members and properties in memory).  The resulting time series are
saved in memory (or a BinaryTS file) and can be output using the processTimeSeries() method).
<b>Filling with historic averages is handled for monthly time series
so that original data averages are used.</b>
@param command_Vector The Vector of Command from the TSCommandProcessor,
to be processed.  If null, process all.  Non-null is typically only used, for example,
if a user has selected commands in a GUI.
@param app_PropList if not null, then properties are set as the commands are
run.  This is typically used when running commands prior to using an edit
dialog in the TSTool GUI, as follows:
<table width=100% cellpadding=10 cellspacing=0 border=2>
<tr>
<td><b>Property</b></td>	<td><b>Description</b></td>
</tr>

<tr>
<td><b>CreateOutput</b></td>
<td>Indicate whether output files should be created.  False is faster but
results in incomplete products.
</td>
<td>True - create output files.</td>
</tr>

<tr>
<td><b>Recursive</b></td>
<td>If set to true, indicates that the commands string list being processed is
from a recursive call (e.g., when processing runCommands()).  Consequently, the
second commands list is processed, not the original one that started
processing.
</td>
<td>False</td>
</tr>

<tr>
<td><b>WorkingDir</b></td>
<td>Will be set if a setWorkingDir() command is encountered.
</td>
<td>Working directory will not be set.</td>
</tr>

</table>
*/
protected void processCommands ( Vector command_Vector,	PropList app_PropList )
throws Exception
{	String	message, routine = "TSEngine.processTimeSeriesCommands";
	String message_tag = "ProcessCommands";
		// Tag used with messages generated in this method.
	int error_count = 0;	// For errors during time series retrieval
	int update_count = 0;	// For warnings about command updates
	if ( command_Vector == null ) {
		// Process all commands if a subset has not been provided.
		command_Vector = __ts_processor.getCommands();
	}
	
	// Save the passed in properties (formed in the TSCommandProcessor) request
	// call, so that they can be retrieved with other requests.
	
	if ( app_PropList == null ) {
		app_PropList = new PropList ( "TSEngine" );
	}
	// Save class version...
	__processor_PropList = app_PropList;

	// Initialize the working directory to the initial directory that is
	// passed in.  Do this because software may request the working directory that
	// is the result of processing and the initial directory may never have
	// been changed dynamically.
	
	/* TODO SAM 2007-10-13 Remove when test out.  The initial working dir is no
	 * longer dynamic but is a data member on the processor.
	String InitialWorkingDir = __processor_PropList.getValue ( "InitialWorkingDir" );
	*/
	String InitialWorkingDir = __ts_processor.getInitialWorkingDir();
	if ( InitialWorkingDir != null ) {
		__processor_PropList.set ( "WorkingDir", InitialWorkingDir );
	}
	Message.printStatus(2, routine,"InitialWorkingDir=" + __processor_PropList.getValue("WorkingDir"));
	
	// Indicate whether output products/files should be created, or
	// just time series (to allow interactive graphing).
	boolean CreateOutput_boolean = __ts_processor.getCreateOutput().booleanValue();
	if ( __processor_PropList != null ) {
		String CreateOutput = app_PropList.getValue ( "CreateOutput" );
		if ( (CreateOutput != null) && CreateOutput.equalsIgnoreCase("False")){
			__ts_processor.setCreateOutput ( new Boolean(false));
			CreateOutput_boolean = false;
		}
		else {
			__ts_processor.setCreateOutput ( new Boolean(true));
		}
	}
	Message.printStatus(2, routine,"CreateOutput=" + __processor_PropList.getValue("CreateOutput") +
			" => " + CreateOutput_boolean );
	
	// Indicate whether time series should be cleared between runs.
	// If true, do not clear the time series between recursive
	// calls.  This is somewhat experimental to evaluate a master
	// commands file that runs other commands files.
	boolean AppendResults_boolean = false;
	// Indicate whether a recursive run of the processor is
	// being made (e.g., because runCommands() is used).
	boolean Recursive_boolean = false;
	if ( __processor_PropList != null ) {
		String Recursive = app_PropList.getValue ( "Recursive" );
		if ( (Recursive != null) && Recursive.equalsIgnoreCase("True")){
			Recursive_boolean = true;
			// Default for recursive runs is to NOT append
			// results...
			AppendResults_boolean = false;
		}
	}
	Message.printStatus(2, routine,"Recursive=" + __processor_PropList.getValue("Recursive") +
			" => " + Recursive_boolean );
	/* FIXME SAM 2007-08-10 Need to enable recursion.
	 * For now the runCommands() command uses the TSCommandFileRunner, but this
	 * is a separate processor and does not have the state of the current
	 * processor in memory.  This is OK for now because it is being used mainly for
	 * testing.
	Vector tsexpression_list = null;	// Local copy of command strings
						// to process.
	if ( Recursive_boolean ) {
		tsexpression_list = _tsexpression_list2;
	}
	else {	tsexpression_list = _tsexpression_list;
	}
	*/

	int size = command_Vector.size();
	Message.printStatus ( 1, routine, "Processing " + size + " commands..." );
	StopWatch stopwatch = new StopWatch();
	stopwatch.start();
	//String expression = null;
	String command_String = null;

	TS ts = null;
	Vector tokens = null;	// For parsing commands.
	String method = null;	// Method to execute
	String	alias = null,	// First (and often only) alias
				// command(alias,...)
	alias2 = null,	// Second time series alias (if needed)
	tsalias = null;	// TS xxx = alias
				// Aliases for time series

	// Go through the expressions up front one time and set some important
	// flags to help performance, etc....

	String first_token = null;
	boolean in_comment = false;
	Command command = null;	// The command to process
	CommandStatus command_status = null;	// Put outside of may try to be able to use in catch.
	__num_TS_expressions = 0;				// Used with BinaryTS to size the file
	for ( int i = 0; i < size; i++ ) {
		ts = null;	// Initialize each time to allow for checks
				// below.
		tokens = null;
		command = (Command)command_Vector.elementAt(i);
		command_String = command.toString();
		if ( command_String == null ) {
			continue;
		}
		command_String = command_String.trim();
		if ( command_String.equals("") ) {
			continue;
		}
		// This is just a rough check to see if we have to go through
		// the effort of computing limits, which slows down the
		// processing...
		if ( command_String.startsWith("/*") ) {
			in_comment = true;
			continue;
		}
		else if ( command_String.startsWith("*/") ) {
			in_comment = false;
			continue;
		}
		if ( in_comment ) {
			continue;
		}
		if ( command_String.regionMatches(true,0,"TS ",0,3) ) {
			// Time series instances.  Keep a count which will be
			// used to size the BinaryTS file...
			++__num_TS_expressions;
		}
		else {	// If the first token has at least 3 ".", assume it is
			// a time series identifier (until we figure out a
			// better way to find out - could check interval!?)...
			first_token = StringUtil.getToken ( command_String, " \t",
				StringUtil.DELIM_SKIP_BLANKS, 0);
			if ( StringUtil.patternCount(first_token,".") >= 3 ) {
				++__num_TS_expressions;
			}
		}
	}

	// Change setting to allow warning messages to be turned off during
	// the main loop.  This capability should not be needed if a command
	// uses the new command status processing.

	int popup_warning_level = 2;		// Do not popup warnings (only to log)
	boolean popup_warning_dialog = false;// Do not popup warnings (handle in command status)
	if ( popup_warning_dialog ) {
		popup_warning_level = 1;
		Message.setPropValue ( "WarningDialogOKNoMoreButton=true" );
		Message.setPropValue ( "WarningDialogCancelButton=true" );
		Message.setPropValue ( "WarningDialogViewLogButton=true" );
	}
    else {
        // Turn off interactive warnings to pretent overload on user in loops.
        Message.setPropValue ( "ShowWarningDialog=false" );
    }
    
    // Clear any settings that may have been left over from the previous run and which
    // can impact the current run.
    
    processCommands_ResetDataForRunStart ( AppendResults_boolean );

	// Now loop through the expressions, query time series, and manipulate
	// to produce a list of final time series.  The following loop does the
	// initial queries.  A final loop does global post-processing compatible
	// with legacy command files (e.g., filling with historic averages)...
	// Use continue for commands that are not TS-related.
	//
	// The end of the loop calls the setTimeSeries() method that will either
	// add the time series to the list or replace what is already in the
	// list, depending on the value of "action".

	first_token = null;
	int ts_action = NONE;	// Action to take at end of loop (insert/update)
	int ts_pos = 0;		// Position of time series in list, for
				// insert/update.
	in_comment = false;
	int i_for_message;	// This will be adjusted by
				// __num_prepended_commands - the user will
				// see command numbers in messages like (12),
				// indicating the twelfth command.

	String command_tag = null;	// String used in messages to allow
					// link back to the application
					// commands, for use with each command.
	//String message_tag = "ProcessCommands";
					// Tag used with messages generated in
					// this method.
	int i;	// Put here so can check count outside of end of loop
	boolean prev_command_complete_notified = false;// If previous command completion listeners were notified
										// May not occur if "continue" in loop.
	Command command_prev = null;	// previous command in loop
	// Indicat the state of the processor...
	__ts_processor.setIsRunning ( true );
	for ( i = 0; i < size; i++ ) {
		// For example, setWorkingDir() is often prepended automatically
		// to start in the working directory.  In this case,
		// the first user-defined command will have:
		// i_for_message = 1 - 1 + 1 = 1
		i_for_message = i - __num_prepended_commands + 1;
		command_tag = ""+i_for_message;	// Command number as integer 1+,
						// for message/log handler.
		// If for some reason the previous command did not notify listeners of its completion (e.g., due to
		// continue in loop, do it now)...
		if ( !prev_command_complete_notified && (command_prev != null) ) {
			__ts_processor.notifyCommandProcessorListenersOfCommandCompleted ( (i - 1), size, command_prev );
		}
		prev_command_complete_notified = false;
		// Save the previous command before resetting to new command below.
		if ( i > 0 ) {
			command_prev = command;
		}
		// Check for a cancel, which would have been set by pressing
		// the cancel button on the warning dialog or by using the
		// other TSTool menus...
		if ( __ts_processor.getCancelProcessingRequested() ) {
			// Set Warning dialog settings back to normal...
			if ( popup_warning_dialog ) {
				Message.setPropValue ( "WarningDialogViewLogButton=false" );
				Message.setPropValue ( "WarningDialogOKNoMoreButton=false" );
				Message.setPropValue ( "WarningDialogCancelButton=false" );
				Message.setPropValue ( "ShowWarningDialog=true" );
			}
            else {
                // Turn on interactive warnings again.
                Message.setPropValue ( "ShowWarningDialog=true" );
            }
			// Set flag so code interested in processor knows it is not running...
			__ts_processor.setIsRunning ( false );
			// Reset the cancel processing request and let interested code know that
			// processing has been cancelled.
			__ts_processor.setCancelProcessingRequested ( false );
			__ts_processor.notifyCommandProcessorListenersOfCommandCancelled (	i, size, command );
			return;
		}
		try {	// Catch errors in all the expressions.
			// Do not indent the body inside the try!
		ts = null;
		ts_action = NONE;
		//expression = (String)tsexpression_list.elementAt(i);
		command = (Command)command_Vector.elementAt(i);
		command_String = command.toString();
		if ( command_String == null ) {
			continue;
		}
		command_String = command_String.trim();
		// All commands will implement CommandStatusProvider so get it...
		command_status = ((CommandStatusProvider)command).getCommandStatus();
		// Clear the run status (internally will set to UNKNOWN).
		command_status.clearLog(CommandPhaseType.RUN);
		Message.printStatus ( 1, routine,
			">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		Message.printStatus ( 1, routine,
			"Start processing command " + (i + 1) + " of " + size + ": \"" + command_String + "\" " );
		// Notify any listeners that the command is running...
		__ts_processor.notifyCommandProcessorListenersOfCommandStarted ( i, size, command );

		if ( command_String.equals("") ) {
			// Empty line.  Mark as processing successful.
			command_status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
			command_status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
			continue;
		}
		if ( command_String.startsWith("#") ) {
			// Comment.  Mark as processing successful.
			command_status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
			command_status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
			continue;
		}
		else if ( command_String.startsWith("/*") ) {
			in_comment = true;
			command_status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
			command_status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
			continue;
		}
		else if ( command_String.startsWith("*/") ) {
			in_comment = false;
			command_status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
			command_status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
			continue;
		}
		if ( in_comment ) {
			command_status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
			command_status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
			continue;
		}
		if ( command_String.regionMatches(true,0,"addConstant",0,11) ) {
			// addConstant command (don't use space because it may
			// occur in dates)...
			tokens = StringUtil.breakStringList ( command_String,
				"(,)", StringUtil.DELIM_SKIP_BLANKS|
				StringUtil.DELIM_ALLOW_STRINGS );
			if ( tokens.size() < 3 ) {
				Message.printStatus ( 1, routine,
				"Bad command \"" + command_String + "\"" );
				continue;
			}
			// First see if a time series is in memory that
			// matches the alias...
			alias = (String)tokens.elementAt(1);
			double d = StringUtil.atod((String)tokens.elementAt(2));
			DateTime start = null;
			DateTime end = null;
			if ( tokens.size() >= 4 ) {
				String analysis_period_start =
				((String)tokens.elementAt(3)).trim();
				start = getDateTime ( analysis_period_start );
			}
			if ( tokens.size() >= 5 ) {
				String analysis_period_end =
				((String)tokens.elementAt(4)).trim();
				end = getDateTime ( analysis_period_end );
			}
			if ( alias.equals("*") ) {
				// Fill everything in memory...
				int nts = getTimeSeriesSize();
				// Set first in case there is an exception...
				ts_action = NONE;
				for ( int its = 0; its < nts; its++ ) {
					ts = getTimeSeries(its);
					TSUtil.addConstant ( ts, start, end, d);
					// Update...
					setTimeSeries ( ts, its );
				}
			}
			else {	// Fill one time series...
				ts_pos = indexOf ( alias );
				if ( ts_pos >= 0 ) {
					ts = getTimeSeries ( ts_pos );
					TSUtil.addConstant ( ts, start, end, d);
				}
				else {	Message.printWarning ( 1, routine,
					"Unable to find time series \"" +
					alias +"\" for addConstant() command.");
				}
				ts_action = UPDATE_TS;
			}
			tokens = null;
		}
		// SAMX - start
		else if ( command_String.regionMatches(true,0,"add",0,3) ||
				command_String.regionMatches(true,0,"subtract",0,8) ) {
			do_add ( command_tag, command_String );
			continue;
		}
		else if(command_String.regionMatches(true,0,"adjustExtremes",0,14)){
			// Adjust extreme values...
			do_adjustExtremes ( command_String );
			continue;
		}
		else if ( command_String.regionMatches( true,0,"ARMA",0,4) ) {
			// Apply ARMA algorithm...
			do_ARMA ( command_String );
			continue;
		}
		else if ( command_String.regionMatches(true,0,"blend",0,5)) {
			// Don't use space because TEMPTS will not parse right.
			Vector v = StringUtil.breakStringList(command_String,
				"(),\t", StringUtil.DELIM_SKIP_BLANKS); 
			if ( (v == null) || (v.size() < 3) ) {
				Message.printWarning ( 2, routine,
				"Syntax error in \"" + command_String + "\"" );
				++error_count;
				v = null;
				continue;
			}

			// Get the individual tokens of the expression...

			alias = ((String)v.elementAt(1)).trim();
			String independent = null;

			// Make sure there are time series available to
			// operate on...

			ts_pos = indexOf ( alias );
			ts = getTimeSeries ( ts_pos );
			if ( ts == null ) {
				Message.printWarning ( 1, routine,
				"Unable to find time series \"" + alias +
				"\" for\n" + command_String + "\"." );
				++error_count;
				v = null;
				continue;
			}
			independent = ((String)v.elementAt(2)).trim();
			TS independentTS = null;
			// The following may or may not have
			// TEMPTS at the front but is handled
			// transparently by getTimeSeries.
			independentTS = getTimeSeries (command_tag,independent);
			if ( independentTS == null ) {
				Message.printWarning ( 1, routine,
				"Unable to find time series \"" + independent +
				"\" for \"" + command_String + "\"." );
				++error_count;
			}
			else {	TSUtil.blend ( ts, independentTS );
			}
			v = null;
			independentTS = null;
			ts_action = UPDATE_TS;
		}
		else if(command_String.regionMatches(true,0,"createFromList",0,14)){
			do_createFromList ( popup_warning_level, command_tag, command_String );
			continue;
		}
		// TODO SAM 2005-09-14
		// Evaluate how this works with other TSAnalyst capabilities
		else if ( command_String.regionMatches(true,0,"createYearStatisticsReport",0,26)){
			do_createYearStatisticsReport ( command_String );
			continue;
		}
		else if ( command_String.regionMatches(true,0,"convertDataUnits",0,16)){
			do_convertDataUnits ( command_String );
			continue;
		}
		else if ( command_String.regionMatches(true,0,"DateTime ",0,9) ) {
			// Declare a DateTime and set to a literal
			do_DateTime ( command_String );
			continue;
		}
		else if ( command_String.regionMatches(true,0,"deselectTimeSeries",0,18)){
			// Deselect time series for output...
			do_selectTimeSeries ( command_String, false );
			ts_action = NONE;
		}
		else if ( command_String.regionMatches(true,0,"divide",0,6)) {
			// Don't use space because TEMPTS will not parse right.
			Vector v = StringUtil.breakStringList(command_String,
				"(),\t", StringUtil.DELIM_SKIP_BLANKS); 
			if ( (v == null) || (v.size() < 3) ) {
				Message.printWarning ( 2, routine,
				"Syntax error in \"" + command_String + "\"" );
				++error_count;
				v = null;
				continue;
			}

			// Get the individual tokens of the expression...

			alias = ((String)v.elementAt(1)).trim();
			String independent = null;

			// Make sure there are time series available to
			// operate on...

			ts_pos = indexOf ( alias );
			ts = getTimeSeries ( ts_pos );
			if ( ts == null ) {
				Message.printWarning ( 1, routine,
				"Unable to find time series \"" + alias +
				"\" for\n" + command_String + "\"." );
				++error_count;
				v = null;
				continue;
			}
			independent = ((String)v.elementAt(2)).trim();
			TS independentTS = null;
			// The following may or may not have
			// TEMPTS at the front but is handled
			// transparently by getTimeSeries.
			independentTS = getTimeSeries(command_tag, independent);
			if ( independentTS == null ) {
				Message.printWarning ( 1, routine,
				"Unable to find time series \"" + independent +
				"\" for \"" + command_String + "\"." );
				++error_count;
			}
			else {	TSUtil.divide ( ts, independentTS );
			}
			v = null;
			independentTS = null;
			ts_action = UPDATE_TS;
		}
		else if ( command_String.equalsIgnoreCase("end") ||	command_String.equalsIgnoreCase("exit") ||
                command_String.equalsIgnoreCase("exit()") ) {
			// Exit the processing, but do historic average filling...
			Message.printStatus ( 1, routine, "Exit - stop processing time series." );
			ts_action = EXIT;
			break;
		}
		else if ( command_String.regionMatches(	true,0,"FillCarryForward",0,16) ) {
			// Just warn, but the old code still works.  At some point it will be totally disabled...
            message = "FillCarryForward() is obsolete.";
			Message.printWarning ( 2, routine, message );
            command_status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.WARNING,
                            message, "Use FillRepeat()." ) );
			++update_count;
			// Fill missing data in the time series by carrying
			// forward the last known value...
			tokens = StringUtil.breakStringList ( command_String, "(,)", StringUtil.DELIM_SKIP_BLANKS );
			if ( tokens.size() != 2 ) {
				Message.printStatus ( 1, routine, "Bad command \"" + command_String + "\"" );
				continue;
			}
			// Parse the identifier...
			alias = (String)tokens.elementAt(1);
			if ( alias.equals("*") ) {
				// Fill everything in memory...
				int nts = getTimeSeriesSize();
				// Set first in case there is an exception...
				ts_action = NONE;
				for ( int its = 0; its < nts; its++ ) {
					ts = getTimeSeries(its);
					TSUtil.fillCarryForward ( ts, (DateTime)null, (DateTime)null );
					// Update...
					setTimeSeries ( ts, its );
				}
			}
			else {	// Fill one time series...
				ts_pos = indexOf ( alias );
				if ( ts_pos >= 0 ) {
					ts = getTimeSeries ( ts_pos );
					TSUtil.fillCarryForward ( ts );
				}
				else {
                    message = "Unable to find time series \"" +	alias + "\" for " +	"FillCarryForward() command.";
					Message.printWarning(2,routine,message);
					throw new Exception ( message );
				}
				ts_action = UPDATE_TS;
			}
			tokens = null;
		}
		else if ( command_String.regionMatches(	true,0,"fillDayTSFrom2MonthTSAnd1DayTS",0,30) ) {
			// Fill missing data in the time series using D1 = D2*M1/M2
			tokens = StringUtil.breakStringList ( command_String,
				" (,)", StringUtil.DELIM_SKIP_BLANKS );
			if ( tokens.size() != 5 ) {
				Message.printStatus ( 1, routine,
				"Bad command \"" + command_String + "\"" );
				continue;
			}
			// Parse the identifier...
			alias = ((String)tokens.elementAt(1)).trim();
			String monthts1 = ((String)tokens.elementAt(2)).trim();
			String monthts2 = ((String)tokens.elementAt(3)).trim();
			String dayts2 = ((String)tokens.elementAt(4)).trim();
			ts_pos = indexOf ( alias );
			int ts_pos_monthts1 = indexOf ( monthts1 );
			int ts_pos_monthts2 = indexOf ( monthts2 );
			int ts_pos_dayts2 = indexOf ( dayts2 );
			if (	(ts_pos >= 0) && (ts_pos_monthts1 >=0) &&
				(ts_pos_dayts2 >= 0) && (ts_pos_monthts2 >=0)) {
				ts = getTimeSeries ( ts_pos );
				TSUtil.fillDayTSFrom2MonthTSAnd1DayTS (
					(DayTS)ts,
					(MonthTS)(getTimeSeries(
						ts_pos_monthts1)),
					(DayTS)(getTimeSeries(ts_pos_dayts2)),
					(MonthTS)(getTimeSeries(
						ts_pos_monthts2)),
					(DateTime)null, (DateTime)null );
			}
			ts_action = UPDATE_TS;
			tokens = null;
		}
		else if(command_String.regionMatches(true,0,"fillFromTS",0,10)) {
			ts = do_fillFromTS(command_tag,command_String);
			// Update occurs in the method.
			ts_action = NONE;
		}
		else if ( command_String.regionMatches(true,0,"fillInterpolate",0,15) ) {
			// Fill missing data in the time series using
			// interpolation...
			tokens = StringUtil.breakStringList ( command_String,
				"(,) ", StringUtil.DELIM_SKIP_BLANKS |
				StringUtil.DELIM_ALLOW_STRINGS );
			if ( tokens.size() != 4 ) {
				message = "Bad command \"" + command_String + "\"";
				Message.printWarning ( 1, routine, message );
				++error_count;
				continue;
			}

			// Parse the identifier...
			alias = (String)tokens.elementAt(1);
			int maxint = StringUtil.atoi(
				(String)tokens.elementAt(2) );
			// Currently don't care about method - it is always
			// linear.
			if ( alias.equals("*") ) {
				// Fill everything in memory...
				int nts = getTimeSeriesSize();
				// Set first in case there is an exception...
				ts_action = NONE;
				for ( int its = 0; its < nts; its++ ) {
					ts = getTimeSeries(its);
					TSUtil.fillInterpolate ( ts,
					(DateTime)null,
					(DateTime)null, maxint, 0 );
					// Update...
					setTimeSeries ( ts, its );
				}
			}
			else {	// Fill one time series...
				ts_pos = indexOf ( alias );
				if ( ts_pos >= 0 ) {
					ts = getTimeSeries ( ts_pos );
					TSUtil.fillInterpolate ( ts,
					(DateTime)null,
					(DateTime)null, maxint, 0 );
					ts_action = UPDATE_TS;
				}
				else {	Message.printWarning ( 1, routine,
					"Unable to find TS referenced in \"" +
					command_String + "\"" );
					ts_action = NONE;
				}
			}
			tokens = null;
		}
		else if ( command_String.regionMatches( true,0,"fillPattern",0,11)){
			// Fill a monthly time series using historical monthly
			// pattern averages.
			do_fillPattern ( command_String );
		}
		else if ( command_String.regionMatches( true,0,"fillProrate",0,11)){
			// Fill missing data in the time series by prorating
			// one time series to another...
			do_fillProrate ( command_String );
			continue;
		}
		else if ( command_String.regionMatches( true,0,"fillRepeat",0,10) ){
			// Fill missing data in the time series by repeating values...
			do_fillRepeat ( command_String );
			continue;
		}
		else if ( command_String.regionMatches(true,0,"setIncludeMissingTS",0,19)) {
			do_setIncludeMissingTS ( command_String );
			continue;
		}
		else if ( command_String.regionMatches(true,0,"multiply",0,8)) {
			// Don't use space because TEMPTS will not parse right.
			Vector v = StringUtil.breakStringList(command_String,
				"(),\t", StringUtil.DELIM_SKIP_BLANKS); 
			if ( (v == null) || (v.size() < 3) ) {
				Message.printWarning ( 2, routine,
				"Syntax error in \"" + command_String + "\"" );
				++error_count;
				v = null;
				continue;
			}

			// Get the individual tokens of the expression...

			alias = ((String)v.elementAt(1)).trim();
			String independent = null;

			// Make sure there are time series available to
			// operate on...

			ts_pos = indexOf ( alias );
			ts = getTimeSeries ( ts_pos );
			if ( ts == null ) {
				Message.printWarning ( 1, routine,
				"Unable to find time series \"" + alias +
				"\" for\n" + command_String + "\"." );
				++error_count;
				v = null;
				continue;
			}
			independent = ((String)v.elementAt(2)).trim();
			TS independentTS = null;
			// The following may or may not have
			// TEMPTS at the front but is handled
			// transparently by getTimeSeries.
			independentTS = getTimeSeries ( command_tag,
				independent );
			if ( independentTS == null ) {
				Message.printWarning ( 1, routine,
				"Unable to find time series \"" + independent +
				"\" for \"" + command_String + "\"." );
				++error_count;
			}
			else {	TSUtil.multiply ( ts, independentTS );
			}
			v = null;
			independentTS = null;
			ts_action = UPDATE_TS;
		}
		else if (command_String.regionMatches(true,0,"readDateValue",0,13)){
			// Read the DateValue file, putting all the time
			// series into memory...
			tokens = StringUtil.breakStringList ( command_String,
				" (,)", StringUtil.DELIM_SKIP_BLANKS|
				StringUtil.DELIM_ALLOW_STRINGS );
			if ( tokens.size() != 2 ) {
				Message.printStatus ( 1, routine, "Bad command \"" + command_String + "\"" );
				continue;
			}
			String infile = ((String)tokens.elementAt(1)).trim();
            String infile_full = IOUtil.verifyPathForOS(
                    IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(__ts_processor),infile));
			Vector tslist = null;
			Message.printStatus ( 1, routine, "Reading DateValue file \"" + infile_full + "\"" );
			try {	tslist = DateValueTS.readTimeSeriesList (
					infile_full, __InputStart_DateTime, __InputEnd_DateTime,
					null, true );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1, routine, "Error reading DateValue file \"" + infile_full + "\"." );
				Message.printWarning ( 2, routine, e );
			}
			// Add the time series to the end of the normal list...
			if ( tslist != null ) {
				// Further process the time series...
				// This makes sure the period is at least that
				// of the output period...
				int vsize = tslist.size();
				Message.printStatus ( 2, routine, "Read " + vsize + " DateValue time series" );
				readTimeSeries2 ( tslist, true );
				ts_pos = getTimeSeriesSize();
				for ( int iv = 0; iv < vsize; iv++ ) {
					setTimeSeries (	(TS)tslist.elementAt(iv), (ts_pos + iv) );
				}
			}
			// Free resources from StateMod list...
			tslist = null;
			// Force a garbage collect because this is an
			// intensive task...
			System.gc();
			// No action needed at end...
			continue;
		}
		else if (command_String.regionMatches(true,0,"readMODSIM",0,10)){
			// Read the MODSIM file, putting all the time series into memory...
            CommandStatus status = ((GenericCommand)command).getCommandStatus();
            int warning_level = 2;
            int warning_count = 0;
            status.clearLog(CommandPhaseType.RUN);
			tokens = StringUtil.breakStringList ( command_String,
				" (,)", StringUtil.DELIM_SKIP_BLANKS|
				StringUtil.DELIM_ALLOW_STRINGS );
			if ( tokens.size() != 2 ) {
				Message.printStatus ( 1, routine,
				"Bad command \"" + command_String + "\"" );
				continue;
			}
			String infile = ((String)tokens.elementAt(1)).trim();
            String infile_full = IOUtil.verifyPathForOS(
                    IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(__ts_processor),infile));
			Vector tslist = null;
			Message.printStatus ( 1, routine, "Reading MODSIM file \"" + infile_full + "\"" );
			try {	tslist = ModsimTS.readTimeSeriesList (
					infile_full, __InputStart_DateTime, __InputEnd_DateTime,
					null, true );
			}
			catch ( Exception e ) {
				message = "Unexpected error reading MODSIM file \"" + infile_full + "\".";

                   Message.printWarning ( warning_level,
                            MessageUtil.formatMessageTag(command_tag,
                            ++warning_count), routine, message );
                    Message.printWarning(3, routine, e);
                    status.addToLog ( CommandPhaseType.RUN,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Verify that the file is a valid MODSIM time series file." ) );
                    Message.printWarning ( 3, routine, e );
                    throw new CommandException ( message );
			}
			// Add the time series to the end of the normal list...
			if ( tslist != null ) {
				// Further process the time series.  This makes sure the period is at least that
				// of the output period...
				int vsize = tslist.size();
				Message.printStatus ( 1, routine, "Read " + vsize + " MODSIM time series" );
				readTimeSeries2 ( tslist, true );
				ts_pos = getTimeSeriesSize();
				for ( int iv = 0; iv < vsize; iv++ ) {
					setTimeSeries (	(TS)tslist.elementAt(iv), (ts_pos + iv) );
				}
			}
			// Free resources from StateMod list...
			tslist = null;
			// Force a garbage collect because this is an intensive task...
			System.gc();
			// No action needed at end...
			continue;
		}
		else if (command_String.regionMatches(true,0,"readNWSRFSESPTraceEnsemble",0,26) ) {
			// Read an ESPTraceEnsemble file, putting all the time
			// series traces into memory...
			do_readNWSRFSESPTraceEnsemble ( command_String );
			// No action needed at end...
			continue;
		}
		else if (command_String.regionMatches(true,0,"readNWSRFSFS5Files",0,13)){
			// Read 1+ time series from NWSRFS FS5Files, putting all
			// the time series traces into memory...
			do_readNWSRFSFS5Files ( command_String );
			// No action needed at end...
			continue;
		}
		else if ( command_String.regionMatches(true,0,"replaceValue",0,12)){
			// Replace values in the time series with a constant
			// value.
			tokens = StringUtil.breakStringList ( command_String,
				"(,)", StringUtil.DELIM_SKIP_BLANKS );
			if ( tokens.size() != 7 ) {
				Message.printWarning ( 2, routine,
				"Bad command \"" + command_String + "\"" );
				++error_count;
				continue;
			}
			// Parse the identifier...
			alias = ((String)tokens.elementAt(1)).trim();
			String minvalue_string = ((String)tokens.elementAt(2)).trim();
			String maxvalue_string = ((String)tokens.elementAt(3)).trim();
			String constant_string = ((String)tokens.elementAt(4)).trim();
			String date1_string = ((String)tokens.elementAt(5)).trim();
			String date2_string = ((String)tokens.elementAt(6)).trim();
			DateTime date1 = null;
			DateTime date2 = null;
			// Convert the string arguments to values that can be
			// used in the replaceValue() method...
			if ( !StringUtil.isDouble(constant_string) ) {
				Message.printWarning ( 2, routine,
				"Bad constant value in \"" + command_String + "\"");
				++error_count;
				continue;
			}
			double constant = StringUtil.atod(constant_string);
			if ( !StringUtil.isDouble(minvalue_string) ) {
				Message.printWarning ( 2, routine,
				"Bad minimum value in \"" + command_String + "\"");
				++error_count;
				continue;
			}
			double minvalue = StringUtil.atod(minvalue_string);
			if (	!maxvalue_string.equals("") &&
				!StringUtil.isDouble(maxvalue_string) ) {
				Message.printWarning ( 2, routine,
				"Bad maximum value in \"" + command_String + "\"");
				++error_count;
				continue;
			}
			double maxvalue = minvalue;
			if (	!maxvalue_string.equals("") &&
				!maxvalue_string.equals("*") ) {
				maxvalue = StringUtil.atod ( maxvalue_string );
			}
			date1 = getDateTime ( date1_string );
			date2 = getDateTime ( date2_string );
			if ( alias.equals("*") ) {
				// Fill everything in memory...
				int nts = getTimeSeriesSize();
				// Set first in case there is an exception...
				ts_action = NONE;
				for ( int its = 0; its < nts; its++ ) {
					ts = getTimeSeries(its);
					TSUtil.replaceValue ( ts, date1, date2,
					minvalue, maxvalue, constant );
					// Update...
					setTimeSeries ( ts, its );
				}
			}
			else {	// Fill one time series...
				ts_pos = indexOf ( alias );
				if ( ts_pos >= 0 ) {
					ts = getTimeSeries ( ts_pos );
					TSUtil.replaceValue ( ts, date1, date2,
					minvalue, maxvalue, constant );
				}
				else {	message =
						"Unable to find time series \""+
						alias + "\" for " +
						"replaceValue() command.";
					Message.printWarning(2,routine,message);
					throw new Exception ( message );
				}
				ts_action = UPDATE_TS;
			}
			tokens = null;
			minvalue_string = null;
			maxvalue_string = null;
			constant_string = null;
			date1_string = null;
			date2_string = null;
			date1 = null;
			date2 = null;
		}
		else if(command_String.regionMatches(true,0,"runningAverage",0,14)){
			// Convert the time series to a running average...
			tokens = StringUtil.breakStringList ( command_String,
				" (,)", StringUtil.DELIM_SKIP_BLANKS );
			if ( tokens.size() != 4 ) {
				Message.printStatus ( 1, routine,
				"Bad command \"" + command_String + "\"" );
				continue;
			}
			// Parse the name and dates...
			alias = (String)tokens.elementAt(1);
			String average_type = (String)tokens.elementAt(2);
			String bracket = (String)tokens.elementAt(3);
			if ( Message.isDebugOn ) {
				Message.printDebug ( 1, routine,
				"Converting TS \"" + alias +
				"\" to " + average_type +
				" running average with bracket " + bracket );
			}
			if ( alias.equals("*") ) {
				// Process everything in memory...
				int nts = getTimeSeriesSize();
				// Set first in case there is an exception...
				ts_action = NONE;
				for ( int its = 0; its < nts; its++ ) {
					if (	average_type.equalsIgnoreCase(
						"Centered") ) {
						ts =
						TSUtil.createRunningAverageTS (
						getTimeSeries ( its ),
						StringUtil.atoi(bracket),
						TSUtil.RUNNING_AVERAGE_CENTER);
					}
					else {	ts =
						TSUtil.createRunningAverageTS (
						getTimeSeries ( its ),
						StringUtil.atoi(bracket),
						TSUtil.RUNNING_AVERAGE_NYEAR);
					}
					// Update...
					setTimeSeries ( ts, its );
				}
			}
			else {	// Fill one time series...
				ts_pos = indexOf ( alias );
				if ( ts_pos >= 0 ) {
					if (	average_type.equalsIgnoreCase(
						"Centered") ) {
						ts =
						TSUtil.createRunningAverageTS (
						getTimeSeries ( ts_pos ),
						StringUtil.atoi(bracket),
						TSUtil.RUNNING_AVERAGE_CENTER);
					}
					else {	ts =
						TSUtil.createRunningAverageTS (
						getTimeSeries ( ts_pos ),
						StringUtil.atoi(bracket),
						TSUtil.RUNNING_AVERAGE_NYEAR);
					}
				}
				else {	message =
						"Unable to find time series \""+
						alias + "\" for " +
						"runningAverage() command.";
						Message.printWarning ( 2,
						routine, message );
					throw new Exception ( message );
				}
				ts_action = UPDATE_TS;
			}
			tokens = null;
		}
		else if ( command_String.regionMatches( true,0,"runProgram",0,10)){
			// Run an external program...
			do_runProgram ( command_String );
			ts_action = NONE;
		}
		else if ( command_String.regionMatches( true,0,"selectTimeSeries",0,16)){
			// Select time series for output...
			do_selectTimeSeries ( command_String, true );
			ts_action = NONE;
		}
		else if ( command_String.regionMatches(	true,0,"setAutoExtendPeriod",0,19) ) {
			// Set the _auto_exend_period flag...
			do_setAutoExtendPeriod ( command_String );
			continue;
		}
		else if ( command_String.regionMatches( true,0,"setAveragePeriod",0,16) ) {
			// Set the averaging period (_averaging_date1 and
			// _averaging_date2)...
			do_setAveragePeriod ( command_String );
			continue;
		}
		else if ( command_String.regionMatches( true,0,"setBinaryTSDayCutoff",0,20)) {
			do_setBinaryTSDayCutoff ( command_String );
			continue;
		}
		else if ( command_String.regionMatches(	true,0,"setBinaryTSPeriod",0,17) ) {
			// Set the BinaryTS period (_binaryts_date1 and _binaryts_date2)...
			do_setBinaryTSPeriod ( command_String );
			continue;
		}
		else if ( command_String.regionMatches(true,0,"setConstantBefore",0,17) ) {
			// PUT THIS BEFORE setConstant!!!
            message = "SetConstantBefore() is obsolete.";
            Message.printWarning ( 2, routine, message );
            command_status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.WARNING,
                            message, "Use SetConstant()." ) );
			++update_count;
			++error_count;
			// Set all data in the time series to a constant
			// value, but only on or before the given date...
			tokens = StringUtil.breakStringList ( command_String,
				"(,)", StringUtil.DELIM_SKIP_BLANKS );
			if ( tokens.size() != 4 ) {
				Message.printWarning ( 2, routine,
				"Bad command \"" + command_String + "\"" );
				++error_count;
				continue;
			}
			// Parse the identifier...
			alias = (String)tokens.elementAt(1);
			double constant = StringUtil.atod(
				(String)tokens.elementAt(2) );
			String datestring = (String)tokens.elementAt(3);
			DateTime beforedate = DateTime.parse ( datestring );
			if (	(beforedate == null) ||
				(beforedate.getYear() == 0) ) {
				message = "Date \"" + datestring +
					"\" is not a valid date";
				Message.printWarning ( 1, routine, message );
				throw new Exception ( message );
			}
			if ( alias.equals("*") ) {
				// Fill everything in memory...
				int nts = getTimeSeriesSize();
				// Set first in case there is an exception...
				ts_action = NONE;
				for ( int its = 0; its < nts; its++ ) {
					ts = getTimeSeries(its);
					TSUtil.setConstant ( ts, ts.getDate1(),
					beforedate, constant );
					// Update...
					setTimeSeries ( ts, its );
				}
			}
			else {	// Fill one time series...
				ts_pos = indexOf ( alias );
				if ( ts_pos >= 0 ) {
					ts = getTimeSeries ( ts_pos );
					TSUtil.setConstant ( ts, ts.getDate1(),
					beforedate, constant );
				}
				ts_action = UPDATE_TS;
			}
			tokens = null;
			beforedate = null;
			datestring = null;
		}
		else if ( command_String.regionMatches(true,0,"setConstant",0,11) ){
			do_setConstant ( command_String );
			continue;
		}
		else if ( command_String.regionMatches(true,0,"setDataValue",0,12)){
			// Set a single value in the time series...
			do_setDataValue ( command_String );
			continue;
		}
		else if (command_String.regionMatches(true,0,"setDebugLevel",0,13)){
			do_setDebugLevel ( command_String );
			continue;
		}
		else if(command_String.regionMatches(true,0,"setFromTS",0,9)) {
			ts = do_setFromTS(command_tag, command_String);
			// Update occurs in method.
			ts_action = NONE;
		}
		else if ( command_String.regionMatches(true,0,"setIgnoreLEZero", 0,15) ) {
			do_setIgnoreLEZero ( command_String );
			continue;
		}
		else if ( command_String.regionMatches(true,0,"setMax",0,6)) {
			// Don't use space because TEMPTS will not parse right.
			do_setMax ( command_tag, command_String );
			continue;
		}
		else if ( command_String.regionMatches(true,0,"setMissingDataValue",0,19) ) {
			do_setMissingDataValue ( command_String );
			continue;
		}
		else if ( command_String.regionMatches(true,0,"setToMin",0,8)) {
			// Don't use space because TEMPTS will not parse right.
			do_setToMin ( command_tag, command_String );
			continue;
		}
		else if ( command_String.regionMatches(true,0,"setOutputDetailedHeaders", 0,24) ) {
			do_setOutputDetailedHeaders ( command_String );
			continue;
		}
		else if ( command_String.regionMatches(true,0,"setOutputYearType",0,17) ) {
			// Set the output year type.
			do_setOutputYearType ( command_String );
			continue;
		}
		else if(command_String.regionMatches(true,0,"setPatternFile",0,14)){
			// Support old and new...
			do_setPatternFile ( command_String );
			continue;
		}
		else if ( command_String.regionMatches(true,0,"setWorkingDir", 0,13) ) {
			do_setWorkingDir ( command_String, app_PropList );
			continue;
		}
		else if ( command_String.regionMatches(true,0,"setWarningLevel", 0,13) ) {
			do_setWarningLevel ( command_String );
			continue;
		}
		// Put the following before the "shift" command...
		else if ( command_String.regionMatches(true,0,"shiftTimeByInterval",0,15) ) {
			// Shift a time series temporally...
			do_shiftTimeByInterval ( command_String );
			continue;
		}
		else if ( command_String.regionMatches( true,0,"shift",0,5) ) {
			// Shift the time series from one date to another...
			do_shift ( command_String );
			continue;
		}
		else if ( command_String.regionMatches(true,0,"StateModMax",0,11) ){
			// Read two StateMod files and create a new list of
			// time series that has the maximum of each time series
			do_stateModMax ( command_tag, command_String, (GenericCommand)command );
			// No action needed at end...
			continue;
		}
		//else if ( command_String.regionMatches(true,0,"subtract",0,8) ) {
		//
		// This is handled in the "add" code since it is so similar.
		//
		//}
		else if ( command_String.regionMatches(true,0,"TS ",0,3) &&
			!StringUtil.getToken(command_String," =(",
				StringUtil.DELIM_SKIP_BLANKS,2).equalsIgnoreCase( "changeInterval") &&
			!StringUtil.getToken(command_String," =(",
				StringUtil.DELIM_SKIP_BLANKS,2).equalsIgnoreCase( "copy") &&
			!StringUtil.getToken(command_String," =(",
				StringUtil.DELIM_SKIP_BLANKS,2).equalsIgnoreCase( "lagK") &&
			!StringUtil.getToken(command_String," =(",
				StringUtil.DELIM_SKIP_BLANKS,2).equalsIgnoreCase( "NewPatternTimeSeries") &&
			!StringUtil.getToken(command_String," =(",
				StringUtil.DELIM_SKIP_BLANKS,2).equalsIgnoreCase( "NewStatisticTimeSeries") &&
			!StringUtil.getToken(command_String," =(",
				StringUtil.DELIM_SKIP_BLANKS,2).equalsIgnoreCase( "newStatisticYearTS") &&
			!StringUtil.getToken(command_String," =(",
				StringUtil.DELIM_SKIP_BLANKS,2).equalsIgnoreCase( "newTimeSeries") &&
			!StringUtil.getToken(command_String," =(",
				StringUtil.DELIM_SKIP_BLANKS,2).equalsIgnoreCase( "readHydroBase") &&
			!StringUtil.getToken(command_String," =(",
				StringUtil.DELIM_SKIP_BLANKS,2).equalsIgnoreCase( "readNDFD") &&
			!StringUtil.getToken(command_String," =(",
				StringUtil.DELIM_SKIP_BLANKS,2).equalsIgnoreCase( "readNwsCard") &&
			!StringUtil.getToken(command_String," =(",
				StringUtil.DELIM_SKIP_BLANKS,2).equalsIgnoreCase( "readStateMod") ) {
			// All these cases are time series to be inserted.
			// Declare a time series and set it to some function
			// Do not handle because they are handled in the TSCommandFactory.
			tokens = StringUtil.breakStringList ( command_String,
				" =(,)", StringUtil.DELIM_SKIP_BLANKS );
			// Position to add is the next in the list...
			ts_pos = getTimeSeriesSize();
			// Parse the tokens...
			// Alias for new time series TS tsalias = ...
			tsalias = ((String)tokens.elementAt(1)).trim();
			// The command used to create the new time series...
			method = ((String)tokens.elementAt(2)).trim();
			// All TS methods result in a new time series being inserted...
			ts_action = INSERT_TS;
			if ( method.equalsIgnoreCase("newEndOfMonthTSFromDayTS") ) {
				// TS Alias =
				// newEndOfMonthTSFromDayTS(TSID,Days)
				// Reparse to allow spaces in the dates...
				tokens = StringUtil.breakStringList ( command_String, "=(,)",
					StringUtil.DELIM_ALLOW_STRINGS);
				if ( tokens.size() != 4 ) {
					Message.printWarning ( 1, routine,
					"Bad command \"" + command_String +"\"");
					++error_count;
					continue;
				}
				ts = do_newEndOfMonthTSFromDayTS ( command_String );
			}
			else if ( method.equalsIgnoreCase("disaggregate") ) {
				// Can't use " " as delimiter because of
				// TEMPTS...
				tokens = StringUtil.breakStringList (command_String,
				"=(,)", StringUtil.DELIM_SKIP_BLANKS );
				if ( tokens.size() < 7 ) {
					Message.printWarning ( 1, routine,
					"Bad command \"" + command_String + "\"" );
					++error_count;
					continue;
				}
				// Change time series interval by disaggregating...
				alias = ((String)tokens.elementAt(2)).trim();
				TS its = getTimeSeries ( command_tag, alias );
				if ( its != null ) {
					String dmethod = ((String)tokens.elementAt(3)).trim();
					String interval_string = (String)tokens.elementAt(4);
					// TODO - catch exception
					TimeInterval interval = TimeInterval.parseInterval(interval_string );
					String datatype_string =(String)tokens.elementAt(5);
					String units_string =(String)tokens.elementAt(6);
					ts = TSUtil.disaggregate (
						its, dmethod, datatype_string,
						units_string,
						interval.getBase(),
						interval.getMultiplier() );
					its = null;
				}
				else {	Message.printWarning ( 1, routine,
					"Unable to find time series \"" + alias + "\" to disaggregate." );
					++error_count;
					ts = null;
				}
			}
			else if ( method.equalsIgnoreCase("newDayTSFromMonthAndDayTS") ) {
				if ( tokens.size() < 3 ) {
					Message.printWarning ( 1, routine,
					"Bad command \"" + command_String + "\"" );
					++error_count;
					continue;
				}
				// Create a new time series...
				alias = (String)tokens.elementAt(3);
				ts = TSUtil.newTimeSeries(alias,true);
				if ( (__OutputStart_DateTime == null) || (__OutputEnd_DateTime == null) ) {
					Message.printWarning ( 1, routine,
					"You must specify the output period "+
					"when using newDayTSFromMonthAnd" +
					"DayTScreateDayTS()." );
					++error_count;
					continue;
				}
				// Make sure the date has a day precision...
				DateTime date1 = new DateTime ( __OutputStart_DateTime );
				date1.setPrecision(DateTime.PRECISION_DAY);
				date1.setDay ( 1 );
				ts.setDate1 ( date1 );
				DateTime date2 = new DateTime ( __OutputEnd_DateTime);
				date2.setPrecision(DateTime.PRECISION_DAY);
				date2.setDay ( TimeUtil.numDaysInMonth(	date2.getMonth(), date2.getYear()) );
				ts.setDate2 ( date2 );
				ts.allocateDataSpace ();
				ts.setIdentifier ( (String)
					tokens.elementAt(3) );
				TS monthTS = getTimeSeries ( command_tag,
					(String)tokens.elementAt(4));
				if ( monthTS == null ) {
					Message.printWarning ( 1, routine,
					"Unable to find monthly time series \""+
					alias + "\" for createDayTS()." );
					++error_count;
					continue;
				}
				TS dayTS = getTimeSeries ( command_tag,
					(String)tokens.elementAt(5));
				if ( dayTS == null ) {
					Message.printWarning ( 1, routine,
					"Unable to find daily time series \"" +
					dayTS + "\" for createDayTS()." );
					++error_count;
					continue;
				}
				setUsingMonthAndDay ( (DayTS)ts,
						(MonthTS)monthTS,
						(DayTS)dayTS );
			}
			else if ( method.equalsIgnoreCase("normalize") ) {
				if ( tokens.size() != 7 ) {
					Message.printWarning ( 1, routine,
					"Bad command \"" + command_String +"\"");
					++error_count;
					continue;
				}
				// Normalize the time series interval
				alias = ((String)tokens.elementAt(3)).trim();
				int pos = indexOf ( alias );
				if ( pos >= 0 ) {
					String minval =
						(String)tokens.elementAt(5);
					String maxval =
						(String)tokens.elementAt(6);
					// Make a copy of the found time
					// series...
					ts = (TS)(getTimeSeries(pos).clone());
					// Now normalize...
					TSUtil.normalize (
						ts, true,
						StringUtil.atod(minval),
						StringUtil.atod(maxval) );
				}
				else {	ts = null;
				}
			}
			else if ( method.equalsIgnoreCase("readDateValue") ) {
				// TS Alias =
				// readDateValue(file,TSID,Units,start,end)
				// Reparse to allow spaces in the dates...
				tokens = StringUtil.breakStringList (
					command_String, "=(,)",
					StringUtil.DELIM_ALLOW_STRINGS);
				if ( tokens.size() != 7 ) {
					Message.printWarning ( 1, routine,
					"Bad command \"" + command_String +"\"");
					++error_count;
					continue;
				}
				ts = do_TS_readDateValue ( command_tag, command_String, (GenericCommand)command );
			}
			else if ( method.equalsIgnoreCase("readMODSIM") ) {
				// TS Alias =
				// readMODSIM(file,TSID,Units,start,end)
				// Reparse to allow spaces in the dates...
				tokens = StringUtil.breakStringList (
						command_String, "=(,)",
					StringUtil.DELIM_ALLOW_STRINGS);
				if ( tokens.size() != 7 ) {
					Message.printWarning ( 1, routine,"Bad command \"" + command_String +"\"");
					++error_count;
					continue;
				}
				ts = do_readMODSIM ( command_tag, command_String, (GenericCommand)command );
			}
			else if (method.equalsIgnoreCase("readNWSRFSFS5Files")){
				ts = do_TS_readNWSRFSFS5Files ( command_String );
			}
			else if ( method.equalsIgnoreCase("readRiverWare") ) {
				// TS Alias= readRiverWare(file,Units,start,end)
				// Reparse to allow spaces in the dates...
				tokens = StringUtil.breakStringList (
					command_String, "=(,)",
					StringUtil.DELIM_ALLOW_STRINGS);
				if ( tokens.size() != 6 ) {
					Message.printWarning ( 1, routine, "Bad command \"" + command_String +"\"");
					++error_count;
					continue;
				}
				ts = do_readRiverWare ( command_tag, command_String, (GenericCommand)command );
			}
			// SAMX - in the GUI as general case...
			else if ( method.equalsIgnoreCase("readTimeSeries") ) {
				// Reparse to strip quotes from file name...
				tokens = StringUtil.breakStringList (
						command_String, "=()",
					StringUtil.DELIM_ALLOW_STRINGS);
				if ( tokens.size() != 3 ) {
					Message.printWarning ( 1, routine,
					"Bad command \"" + command_String +"\"");
					++error_count;
					continue;
				}
				// Read the time series...
				ts = readTimeSeries( popup_warning_level, command_tag,
					((String)tokens.elementAt(2)).trim());
			}
			else if ( method.equalsIgnoreCase("readUsgsNwis") ) {
				// TS Alias = readUsgsNwis(file,start,end)
				tokens = StringUtil.breakStringList (
						command_String, "=(),",
					StringUtil.DELIM_ALLOW_STRINGS);
				if ( tokens.size() != 5 ) {
					Message.printWarning ( 1, routine,
					"Bad command \"" + command_String +"\"");
					++error_count;
					continue;
				}
				ts = do_readUsgsNwis ( command_tag, command_String, (GenericCommand)command );
			}
			else if ( method.equalsIgnoreCase("relativeDiff") ) {
				// Relative diff of time series...
				if ( tokens.size() != 6 ) {
					Message.printWarning ( 1, routine, "Bad command \"" + command_String + "\"");
					++error_count;
					continue;
				}
				alias = ((String)tokens.elementAt(3)).trim();
				int pos1 = indexOf ( alias );
				alias2 = ((String)tokens.elementAt(4)).trim();
				int pos2 = indexOf ( alias2 );
				if ( (pos1 >= 0) && (pos2 >= 0) ) {
					// Make a copy of the found time
					// series...
					ts = (TS)(getTimeSeries(pos1).clone());
					TS ts2 = (TS)(getTimeSeries(pos2));
					String divisor =
						(String)tokens.elementAt(5);
					// Now do relativeDiff()...
					if (	divisor.equalsIgnoreCase(
						"DivideByTS1") ) {
						TSUtil.relativeDiff (
						ts, ts2, ts );
					}
					else {	TSUtil.relativeDiff (
						ts, ts2, ts2 );
					}
					divisor = null;
					ts2 = null;
				}
				else {	ts = null;
					Message.printWarning ( 1, routine,
					"Bad command (TS not found) \"" +
					command_String +"\"");
					++error_count;
				}
			}
			else if ( method.equalsIgnoreCase("weightTraces") ) {
				if ( tokens.size() < 7 ) {
					Message.printWarning ( 1, routine,
					"Bad command \"" + command_String +	"\"" );
					++error_count;
					continue;
				}
				// Weight traces and create a new time series...
				alias2 = ((String)tokens.elementAt(3)).trim();
				String weight_method =
					((String)tokens.elementAt(4)).trim();
				if ( !weight_method.equalsIgnoreCase(
					"AbsoluteWeights") ) {
					Message.printWarning ( 1, routine,
					"Weight method is not recognized for \""
					+ command_String + "\"" );
					++error_count;
					continue;
				}
				// Get the trace numbers and weights...
				Vector traces = new Vector();
				Vector weights = new Vector();
				int tsize = tokens.size();
				for ( int iw = 5; iw < tsize; iw++ ) {
					if ( (iw%2) != 0 ) {
						traces.addElement( ((String)
						tokens.elementAt(iw)).trim() );
						if (	!StringUtil.isInteger(
							(String)
							tokens.elementAt(iw))){
							Message.printWarning(1,
							routine, "Non-integer "+
							"trace number in \"" +
							command_String + "\"" );
							++error_count;
							continue;
						}
					}
					else {	weights.addElement( ((String)
						tokens.elementAt(iw)).trim() );
						if (	!StringUtil.isDouble(
							(String)
							tokens.elementAt(iw))){
							Message.printWarning(1,
							routine, "Non-number "+
							"weight in \"" +
							command_String + "\"" );
							++error_count;
							continue;
						}
					}
				}
				// Now loop through.  Copy the first trace to
				// a new time series (set the alias).  Then
				// loop through the other traces and add to the
				// first.  Later, if normalized weights are
				// added, will need to do more in TSUtil...
				TS its = getTimeSeries ( command_tag, alias2,
					StringUtil.atoi(
					(String)traces.elementAt(0)) );
				if ( its == null ) {
					Message.printWarning ( 1, routine,
					"Unable to find time series \"" +
					alias2 + "\" trace " +
					(String)traces.elementAt(0) );
					++error_count;
					continue;
				}
				ts = (TS)its.clone();
				// Set all the data to missing since the values
				// will be set during the add...
				TSUtil.setConstant(ts,ts.getMissing());
				// Set the description to empty since it will
				// be reset in the TSUtil.add call below.
				ts.setDescription("");
				tsize = traces.size();
				double addfac[] = new double[1];
				Vector addvec = new Vector(1);
				for ( int iw = 0; iw < tsize; iw++ ) {
					its = getTimeSeries ( command_tag,
						alias2,
						StringUtil.atoi(
						(String)traces.elementAt(iw)));
					if ( its == null ) {
						Message.printWarning ( 1,
						routine,
						"Unable to find time series \""+
						alias2 + "\" trace " +
						(String)traces.elementAt(iw) );
						++error_count;
						continue;
					}
					// Else do the add...
					addvec.removeAllElements();
					addvec.addElement(its);
					addfac[0] = StringUtil.atod((String)
						weights.elementAt(iw));
					ts = TSUtil.add ( ts, addvec, addfac,
						TSUtil.IGNORE_MISSING );
				}
			}
			// Set the alias for the new time series to the value
			// originally read after the TS...
			if ( ts != null ) {
				ts.setAlias ( tsalias );
			}
			tokens = null;
		}
		else if ( command_String.regionMatches(true,0,"writeNwsCard",0,12)){
			// Write the time series in memory to an NWS Card
			// time series file...
			if ( !CreateOutput_boolean ) {
				Message.printStatus ( 1, routine,
				"Skipping \"" + command_String +
				"\" because output is to be ignored." );
				continue;
			}
			do_writeNwsCard(command_String);
			// No action needed at end...
			continue;
		}
		else if(command_String.regionMatches(true,0,"writeStateCU",0,12) ){
			// Write the time series in memory to a StateCU time
			// series file...
			if ( !CreateOutput_boolean ) {
				Message.printStatus ( 1, routine,
				"Skipping \"" + command_String +
				"\" because output is to be ignored." );
				continue;
			}
			do_writeStateCU ( command_tag, command_String,
					formatOutputHeaderComments(command_Vector), (GenericCommand)command );
			// No action needed at end...
			continue;
		}

		// Detect a time series identifier, which needs to be processed
		// to read the time series...
		else if ( TSCommandProcessorUtil.isTSID(command_String) ) {
			// Probably a raw time series identifier.
			// Try to read it...
			if ( Message.isDebugOn ) {
				Message.printDebug ( 1, routine,
				"Reading TS now..." );
			}
			ts = readTimeSeries ( popup_warning_level, command_tag, command_String );
			if ( Message.isDebugOn ) {
				Message.printDebug ( 1, routine, "...read TS" );
			}
			if ( ts == null ) {
				// Need to make sure we don't get redundant
				// warnings.  Throw an exception and catch in
				// the main loop...
				++error_count;
				throw new Exception (
				"Null TS from read.  Unable to process " +
				"command \"" + command_String + "\"" );
			}
			ts_action = INSERT_TS;
			// Append to the end of the time series list...
			ts_pos = getTimeSeriesSize();
		}
	
		// Check for obsolete commands (do this last to minimize the
		// amount of processing through this code)...
		
		else if ( processCommands_CheckForObsoleteCommands(command_String, message_tag, i_for_message) ) {
			// Had a match so increment the counters.
			++update_count;
			++error_count;
		}
		// Command factory for remaining commands...
		else {
            // Try the Command class code...
			try {
                // Make sure the command is valid...
				// Initialize the command (parse)...
				// TODO SAM 2007-09-05 Need to evaluate where the
				// initialization occurs (probably the initial edit or load)?
				if ( Message.isDebugOn ) {
					Message.printDebug ( 1, routine,
					"Initializing the Command for \"" +	command_String + "\"" );
				}
				if ( command instanceof CommandStatusProvider ) {
					((CommandStatusProvider)command).getCommandStatus().clearLog(CommandPhaseType.INITIALIZATION);
				}
				if ( command instanceof CommandStatusProvider ) {
					((CommandStatusProvider)command).getCommandStatus().clearLog(CommandPhaseType.DISCOVERY);
				}
				command.initializeCommand ( command_String, __ts_processor, true );
				// TODO SAM 2005-05-11 Is this the best place for this or should it be in the
				// runCommand()?...
				// Check the command parameters...
				if ( Message.isDebugOn ) {
					Message.printDebug ( 1, routine, "Checking the parameters for command \"" + command_String + "\"" );
				}
				command.checkCommandParameters ( command.getCommandParameters(), command_tag, 2 );
				// Clear the run status for the command...
				if ( command instanceof CommandStatusProvider ) {
					((CommandStatusProvider)command).getCommandStatus().clearLog(CommandPhaseType.RUN);
				}
				// Run the command...
				if ( Message.isDebugOn ) {
					Message.printDebug ( 1, routine, "Running command through new code..." );
				}
				command.runCommand ( i_for_message );
				if ( Message.isDebugOn ) {
					Message.printDebug ( 1, routine, "...back from running command." );
				}
			}
			catch ( InvalidCommandSyntaxException e ) {
				message = "Unable to process command (invalid syntax).";
				if ( command instanceof CommandStatusProvider ) {
				       if (	CommandStatusUtil.getHighestSeverity((CommandStatusProvider)command).
                               greaterThan(CommandStatusType.UNKNOWN) ) {
				           // No need to print a message to the screen because a visual marker will be shown, but log...
				           Message.printWarning ( 2,
				                   MessageUtil.formatMessageTag(command_tag,
				                           ++error_count), routine, message );
                       }
                       if ( command instanceof GenericCommand ) {
                            // The command class will not have added a log record so do it here...
                            ((CommandStatusProvider)command).getCommandStatus().addToLog ( CommandPhaseType.RUN,
                                    new CommandLogRecord(CommandStatusType.FAILURE,
                                            message, "Check the log for more details." ) );
                       }
				}
				else {	// Command has not been updated to set warning/failure in status so show here
					Message.printWarning ( popup_warning_level,
						MessageUtil.formatMessageTag(command_tag,
						++error_count), routine, message );
				}
				// Log the exception.
				if (Message.isDebugOn) {
					Message.printDebug(3, routine, e);
				}
				continue;
			}
			catch ( InvalidCommandParameterException e ) {
				message = "Unable to process command invalid parameter).";
				if ( command instanceof CommandStatusProvider ) {
				    if ( CommandStatusUtil.getHighestSeverity((CommandStatusProvider)command).
                            greaterThan(CommandStatusType.UNKNOWN) ) {
				        // No need to print a message to the screen because a visual marker will be shown, but log...
				        Message.printWarning ( 2,
				                MessageUtil.formatMessageTag(command_tag,
				                        ++error_count), routine, message );
                    }
                    if ( command instanceof GenericCommand ) {
                        // The command class will not have added a log record so do it here...
                        ((CommandStatusProvider)command).getCommandStatus().addToLog ( CommandPhaseType.RUN,
                                new CommandLogRecord(CommandStatusType.FAILURE,
                                        message, "Check the log for more details." ) );
                    }
				}
				else {	// Command has not been updated to set warning/failure in status so show here
					Message.printWarning ( popup_warning_level,
							MessageUtil.formatMessageTag(command_tag,
									++error_count), routine, message );
				}
				if (Message.isDebugOn) {
					Message.printWarning(3, routine, e);
				}
				continue;
			}
			catch ( CommandWarningException e ) {
				message = "Warnings were generated processing command (output may be incomplete).";
				if ( command instanceof CommandStatusProvider ) {
				    if ( CommandStatusUtil.getHighestSeverity((CommandStatusProvider)command).
                            greaterThan(CommandStatusType.UNKNOWN) ) {
				        // No need to print a message to the screen because a visual marker will be shown, but log...
				        Message.printWarning ( 2,
				                MessageUtil.formatMessageTag(command_tag,
				                        ++error_count), routine, message );
                    }
                    if ( command instanceof GenericCommand ) {
                        // The command class will not have added a log record so do it here...
                        ((CommandStatusProvider)command).getCommandStatus().addToLog ( CommandPhaseType.RUN,
                                new CommandLogRecord(CommandStatusType.FAILURE,
                                        message, "Check the log file for more details." ) );
                    }
				}
				else {	// Command has not been updated to set warning/failure in status so show here
					Message.printWarning ( popup_warning_level,
							MessageUtil.formatMessageTag(command_tag,
									++error_count), routine, message );
				}
				if (Message.isDebugOn) {
					Message.printDebug(3, routine, e);
				}
				continue;
			}
			catch ( CommandException e ) {
				message = "Error processing command (unable to complete command).";
				if ( command instanceof CommandStatusProvider ) {
				    if ( CommandStatusUtil.getHighestSeverity((CommandStatusProvider)command).
                            greaterThan(CommandStatusType.UNKNOWN) ) {
				        // No need to print a message to the screen because a visual marker will be shown, but log...
				        Message.printWarning ( 2,
				                MessageUtil.formatMessageTag(command_tag,++error_count), routine, message );
                    }
                    if ( command instanceof GenericCommand ) {
                        // The command class will not have added a log record so do it here.  Because the
                        // exception thrown by GenericCommand.runCommand() is not very specific, verify that
                        // this is a recognized command...
                        TSCommandFactory cf = new TSCommandFactory ();
                        try {
                            cf.newCommand(command.toString());
                        }
                        catch ( UnknownCommandException e2 ) {
                            ((CommandStatusProvider)command).getCommandStatus().addToLog ( CommandPhaseType.RUN,
                                    new CommandLogRecord(CommandStatusType.FAILURE,
                                            "Unknown command \"" + command.toString() + "\".",
                                            "Verify the spelling of the command name - " +
                                            "recreate with command editor if necessary." ) );
                        }
                        ((CommandStatusProvider)command).getCommandStatus().addToLog ( CommandPhaseType.RUN,
                                new CommandLogRecord(CommandStatusType.FAILURE,
                                        message, "Check the log file for more details." ) );
                    }
				}
				else {	// Command has not been updated to set warning/failure in status so show here
					Message.printWarning ( popup_warning_level,
						MessageUtil.formatMessageTag(command_tag,
						++error_count), routine, message );
				}
				if (Message.isDebugOn) {
					Message.printDebug(3, routine, e);
				}
				continue;
			}
			catch ( Exception e ) {
				message = "Unexpected error processing command (unable to complete command).";
				if ( command instanceof CommandStatusProvider ) {
					// Add to the log as a failure...
					Message.printWarning ( 2,
						MessageUtil.formatMessageTag(command_tag,++error_count), routine, message );
                    // Always add to the log because this type of exception is unexpected from a Command object.
					command_status.addToLog(CommandPhaseType.RUN,
							new CommandLogRecord(CommandStatusType.FAILURE,
									"Unexpected exception \"" + e.getMessage() + "\"",
									"See log file for details.") );
				}
				else {
					Message.printWarning ( popup_warning_level,
							MessageUtil.formatMessageTag(command_tag,
									++error_count), routine, message );
				}
				Message.printWarning ( 3, routine, e );
				continue;
			}
		}

		if ( ts_action == NONE ) {
			// Don't do anything...
			continue;
		}
		else {	// Need to insert or update a time series...
			processTimeSeriesAction ( ts_action, ts, ts_pos );
		}

		Message.printStatus ( 1, routine,
		"Retrieved time series for \"" + command_String + "\" (" +  (i + 1)
		+ " of " + size + " commands)" );

		} // Main catch
		catch ( Exception e ) {
			Message.printWarning ( popup_warning_level,
			MessageUtil.formatMessageTag(command_tag,
			++error_count), routine,
			"There was an error processing command: \"" +
			command_String + "\"" );
			Message.printWarning ( 3, routine, e );
			if ( command instanceof CommandStatusProvider ) {
				// Add to the command log as a failure...
				command_status.addToLog(CommandPhaseType.RUN,
						new CommandLogRecord(CommandStatusType.FAILURE,
								"Unexpected error \"" + e.getMessage() + "\"",
								"See log file for details.") );
			}
			// Then continue with next expression, unless the binary...
			if ( getBinaryTSUsed() ) {
				return;
			}
		}
		catch ( OutOfMemoryError e ) {
			Message.printWarning ( popup_warning_level,
			MessageUtil.formatMessageTag(command_tag,
			++error_count), routine,
			"The command processor ran out of memory.  Exit and restart.\n" +
			"Try increasing the -Xmx setting for the Java Runtime Environment." );
			if ( command instanceof CommandStatusProvider ) {
				// Add to the command log as a failure...
				command_status.addToLog(CommandPhaseType.RUN,
					new CommandLogRecord(CommandStatusType.FAILURE,
						"Out of memory error \"" + e.getMessage() + "\"",
						"Try increasing JRE memory with -Xmx.  See log file for details.  See troubleshooting documentation.") );
			}
			Message.printWarning ( 2, routine, e );
			System.gc();
			// May be able to save commands.
		}
		finally {
			// Always want to get to here for each command.
		}
		// Notify any listeners that the command is done running...
		prev_command_complete_notified = true;
		__ts_processor.notifyCommandProcessorListenersOfCommandCompleted ( i, size, command );
	}
	// If necessary, do a final notify for the last command...
	if ( !prev_command_complete_notified ) {
		if ( i == size ) {
			--i;
		}
		__ts_processor.notifyCommandProcessorListenersOfCommandCompleted ( i, size, command );
	}
	
	// Indicate that processing is done and now there is no need to worry about
	// cancelling.
	__ts_processor.setIsRunning ( false );
	if ( __ts_processor.getCancelProcessingRequested() ) {
		// Have gotten to here probably because the last command was processed
		// and need to notify the listeners.
		__ts_processor.notifyCommandProcessorListenersOfCommandCancelled (	i, size, command );
	}
	__ts_processor.setCancelProcessingRequested ( false );
	
	if ( popup_warning_dialog ) {
		// Change so from this point user always has to acknowledge the
		// warnings...
		Message.setPropValue ( "WarningDialogOKNoMoreButton=false" );
		Message.setPropValue ( "WarningDialogCancelButton=false" );
		Message.setPropValue ( "WarningDialogViewLogButton=false" );
	}
    // Make sure that important warnings are shown to the user...
    Message.setPropValue ( "ShowWarningDialog=true" );

	if ( getBinaryTSUsed() ) {
		if ( __tslist != null ) {
			// Clear out the __tslist because it is not used -  Needed?
			__tslist.removeAllElements();
		}

		Message.printStatus ( 1, routine, "Retrieved " + getTimeSeriesSize() + " time series." );
		Message.printStatus ( 1, routine, "Time series have been saved"+
		" to the temporary BinaryTS file \"" +
		__binary_ts_file + "\" and can now be used for output." );
	}
	else {	Message.printStatus ( 1, routine, "Retrieved " + __tslist.size() + " time series." );
	}

	size = getTimeSeriesSize();

	// Get the final time - note this includes intervening warnings if
	// any occurred...

	stopwatch.stop();
	Message.printStatus ( 1, routine, "Processing took " +
		StringUtil.formatString(stopwatch.getSeconds(),"%.2f") +
		" seconds" );

	// Check for fatal errors (for Command classes, only warn if failures since
	// others are likely not a problem)...

	int ml = 2;	// Message level for cleanup warnings
	if ( popup_warning_dialog ) {
		Message.setPropValue ( "WarningDialogViewLogButton=true" );
		ml = 1;
	}
	CommandStatusType max_severity = CommandStatusProviderUtil.getHighestSeverity ( command_Vector );
	if ( (_fatal_error_count > 0) || (error_count > 0) || max_severity.greaterThan(CommandStatusType.WARNING)) {

		if ( IOUtil.isBatch() ) {
			size = getMissingTS().size();
			if ( size > 0 ) {
				// FIXME SAM 2007-11-06 Put in CheckTimeSeries()...
				Message.printWarning ( ml, routine,"The following time series were not found:" );
				for ( int i2 = 0; i2 < size; i2++ ) {
					Message.printWarning ( ml, routine,"   "+(String)getMissingTS().elementAt(i2));
				}
			}
			Message.printWarning ( ml, routine,
			"There were warnings or failures processing commands.  The output may be incomplete." );
			// FIXME SAM 2007-08-20 Need to figure out how to get the
			// exit status out of the processor and allow calling
			// code to exit.
			//__gui.quitProgram ( 1 );
		}
		else {	Message.printWarning ( ml, routine,
			"There were warnings processing commands.  The output may be incomplete.\n" +
			"See the log file for information." );
		}
	}
	if ( update_count > 0 ) {
		Message.printWarning ( ml, routine,
		"There were warnings printed for obsolete commands.\n" +
		"See the log file for information.  The output may be " +
		"incomplete." );
	}
	if ( popup_warning_dialog ) {
		Message.setPropValue ( "WarningDialogViewLogButton=false" );
	}

	// Clean up...
	message = null;
	ts = null;
}

/**
Check for obsolete commands and print an appropriate message.  Handling of warning increments is done in the
calling code.
@param command_String Command as string to check.
@param message_tag Message tag for logging.
@param i_for_message Command number for messages (1+).
*/
private boolean processCommands_CheckForObsoleteCommands( String command_String, String message_tag, int i_for_message)
throws Exception
{	String routine = getClass().getName() + ".processCommands_CheckForObsoleteCommands";
	// Check for obsolete commands.  Do this at the end
	// because this logic may seldom be hit if valid commands are
	// processed above.  Print at level 1 because these messages
	// need to be addressed.
	
	boolean is_obsolete = false;
	if (command_String.regionMatches(true,0,"-averageperiod",0,14)){
		Message.printWarning ( 1, routine,
		"-averageperiod is obsolete.\n" +
		"Use setAveragePeriod().\n" +
		"Automatically using setAveragePeriod()." );
		is_obsolete = true;
		do_setAveragePeriod ( command_String );
	}
	else if ( command_String.regionMatches(true,0,"-batch",0,6)) {
		// Old syntax command.  Leave around because this
		// functionality has never really been implemented but
		// needs to be (e.g., call TSTool from web site and
		// tell it to create a plot?).
		Message.printStatus ( 1, routine,"Running in batch mode." );
		is_obsolete = true;
		IOUtil.isBatch ( true );
	}
	else if ( command_String.regionMatches(true,0,"-binary_day_cutoff",0,18)) {
		Message.printWarning ( 1, routine,
		"-binary_day_cutoff is obsolete.\n" +
		"Use setBinaryTSDayCutoff().\n" +
		"Automatically using setBinaryTSDayCutoff()." );
		is_obsolete = true;
		do_setBinaryTSDayCutoff ( command_String );
	}
	else if ( command_String.regionMatches(true,0,"-binary_ts_file",0,20)) {
		Message.printWarning ( 1, routine,
		"-binary_ts_file is obsolete.\n" +
		"Use setBinaryTSFile().\n" +
		"Automatically using setBinaryTSFile()." );
		is_obsolete = true;
		do_setBinaryTSFile ( command_String );
	}
	else if ( command_String.equalsIgnoreCase("-cy") ) {
		Message.printWarning ( 1, routine,
		"-cy is obsolete.\n" +
		"Use setOutputYearType().\n" +
		"Automatically using setOutputYearType()." );
		is_obsolete = true;
		do_setOutputYearType ( command_String );
	}
	else if(command_String.regionMatches(true,0,"-data_interval",0,14)){
		Message.printWarning ( 1, routine,"-data_interval is obsolete.  Use CreateFromList()." );
		is_obsolete = true;
	}
	else if ( command_String.regionMatches(true,0,"-datasource",0,11)) {
		// Handled by HBParse.  Ignore here.  The data source
		// name is in the same arg so don't need to increment.
	}
	else if(command_String.regionMatches(true,0,"-data_type",0,10)){
		Message.printWarning ( 1, routine,"-data_type is obsolete.  Use CreateFromList()." );
		is_obsolete = true;
	}
	else if ( command_String.regionMatches(
		true,0,"-detailedheader",0,15) ) {
		Message.printWarning ( 1, routine,
		"-detailedheader is obsolete.\n" +
		"Use setOutputDetailedHeaders().\n" +
		"Automatically using setOutputDetailedHeaders()." );
		is_obsolete = true;
		do_setOutputDetailedHeaders ( command_String );
	}
	else if ( command_String.regionMatches(true,0,"-d",0,2) ) {
		is_obsolete = true;
		do_setDebugLevel ( command_String );
	}
    else if ( command_String.regionMatches(true,0,"day_to_month_reservoir",0,22) ) {
        is_obsolete = true;
        // Leave this code in for backward compatibility but the
        // new TSTool (as of 2002-08-27) uses
        // TS Alias = dayToMonthReservoir()...
        // Convert daily time series to monthly using reservoir
        // conventions (end of month)...
        // Expect that TSID, nearness limit and flag for
        // linear interpolation are set.
        // THIS IS AN OLD STYLE COMMAND.  READ THE TIME SERIES
        // AND THEN INSERT.
        Message.printWarning ( 1, routine,
        "day_to_month_reservoir is obsolete.\n" +
        "Use dayToMonthReservoir()." );
        /*
        ++update_count;
        Message.printStatus ( 1, routine,
        "Processing reservoir storage..." );
        Vector v = 
            StringUtil.breakStringList(command_String, "(,) ",
            StringUtil.DELIM_SKIP_BLANKS );
        if ( v.size() != 4) {
            Message.printWarning ( 1, routine, 
            "Invalid day_to_month_reservoir(): \"" +
            command_String + "\"" );
            ++error_count;
            continue;
        }
        ts = readTimeSeries ( popup_warning_level, command_tag,
            ((String)v.elementAt(1)).trim() );
        if ( ts != null ) {
            // Now have a daily time series... Convert to
            // monthly using the nearest values...
            PropList changeprops = new PropList (
            "day_to_month_reservoir" );
            changeprops.set ( "UseNearestToEnd",
                ((String)v.elementAt(2)).trim() );
            Message.printStatus ( 1, routine,
            "Changing interval..." );
            MonthTS monthts = (MonthTS)
                TSUtil.changeInterval (
                ts, TimeInterval.MONTH, 1,
                changeprops );
            // Now fill with linear interpolation...
            if ( ((String)v.elementAt(3)).equalsIgnoreCase(
                "true") ) {
                Message.printStatus ( 1, routine,
                "Filling by interpolation..." );
                TSUtil.fillInterpolate ( monthts,
                (DateTime)null, (DateTime)null );
            }
            ts_pos = getTimeSeriesSize();
            changeprops = null;
            ts = monthts;
        }
        ts_action = INSERT_TS;
        v = null;
        */
    }
	else if ( command_String.regionMatches(true,0,"fillconst(",0,10) ) {
		is_obsolete = true;
		String message ="fillconst() is obsolete.  Use fillConstant().";
		Message.printWarning ( 1,
		//MessageUtil.formatMessageTag(command_tag,
		//++error_count),
		routine, message );
	}
	else if ( command_String.regionMatches(	true,0,"-filldata",0,9) ) {
		Message.printWarning ( 1, routine,
		"-filldata is obsolete.\n" +
		"Use setPatternFile().\n" +
		"Automatically using setPatternFile()." );
		is_obsolete = true;
		do_setPatternFile ( command_String );
	}
	else if ( command_String.regionMatches(true,0,"-fillhistave",0,12)){
		Message.printWarning ( 1, routine,
		"-fillhistave is obsolete.\n" +
		"Use fillHistMonthAverage() or " +
		"fillHistYearAverage().\n" +
		"Automatically using appropriate new command." );
		is_obsolete = true;
	}
	else if ( command_String.regionMatches( true,0,"fillpattern_setconstbefore",0,26)){
		Message.printWarning ( 1, routine,
		"fillpattern_setconstbefore() is obsolete.  " +
		"Use fillpattern() and setConstant()." );
		is_obsolete = true;
	}
	else if ( command_String.regionMatches(true,0,"-fillUsingComments",0,18)){
		Message.printWarning ( 1, routine,
		"-fillUsingComments is obsolete.\n" +
		"Use fillUsingDiversionComments().");
		is_obsolete = true;
	}
	else if(command_String.regionMatches(true,0,"-ignorelezero",0,13) ){
		Message.printWarning ( 1, routine,
		"-ignorelezero is obsolete.\n" +
		"Use setIgnoreLEZero().\n" +
		"Automatically using setIgnoreLEZero()." );
		is_obsolete = true;
		do_setIgnoreLEZero ( command_String );
	}
	else if ( command_String.regionMatches(true,0,"-include_missing_ts",0,19)) {
		Message.printWarning ( 1, routine,
		"-include_missing_ts is obsolete.\n" +
		"Use setIncludeMissingTS().  Automatically using\n" +
		"setIncludeMissingTS(true)" );
		is_obsolete = true;
		do_setIncludeMissingTS ( command_String );
	}
	else if ( command_String.regionMatches( true,0,"-missing",0,8) ) {
		Message.printWarning ( 1, routine,
		"-missing is obsolete.\n" +
		"Use setMissingDataValue().\n" +
		"Automatically using setMissingDataValue()." );
		is_obsolete = true;
		do_setMissingDataValue ( command_String );
	}
	else if ( command_String.regionMatches( true,0,"-ostatemod",0,10) ){
		Message.printWarning ( 1, routine,
		"-ostatemod is obsolete.\nUse writeStateMod()." );
		is_obsolete = true;
	}
	else if ( command_String.regionMatches( true,0,"-osummary",0,9) ){
		Message.printStatus ( 1, routine,
			"\"-osummary\" is obsolete.  Use WriteSummary() or other output commands." );
		is_obsolete = true;
	}
	else if ( command_String.regionMatches(true,0,"-osummarynostats",0,16) ){
		Message.printStatus ( 1, routine,
			"\"-osummarynostats\" is obsolete.  Use WriteSummary() or other output commands." );
		is_obsolete = true;
	}
	// Put this after all the other -o options...
	else if ( command_String.regionMatches( true,0,"-o",0,2) ){
		// Output in StateMod format...
		Message.printWarning ( 1, routine,
		"\"-o File\" is obsolete.\n" +
		"Use writeStateMod() or other output commands." );
		is_obsolete = true;
	}
	else if ( command_String.regionMatches(true,0,"regress",0,7) ) {
		Message.printWarning ( 1, routine,
		"regress() is obsolete.  Use fillRegression()." );
		is_obsolete = true;
	}
	else if ( command_String.regionMatches(true,0,"setconst(",0,9) ) {
		is_obsolete = true;
		String message = "setconst() is obsolete.  Use setConstant().";
		Message.printWarning ( 1, routine, message );
	}
	else if(command_String.regionMatches(true,0,"setconstbefore",0,14)){
		Message.printWarning ( 1, routine, "setconstbefore() " +
		"is obsolete.  Use setConstant()." );
		is_obsolete = true;
	}
	else if ( command_String.regionMatches(true,0,"setDatabaseEngine", 0,17) ) {
		Message.printWarning ( 1, routine,
		"setDatabaseEngine is obsolete.\nUse openHydroBase().");
		is_obsolete = true;
	}
	else if ( command_String.regionMatches(true,0,"setDatabaseHost", 0,15) ) {
		Message.printWarning ( 1, routine,
		"setDatabaseHost is obsolete.\nUse openHydroBase()." );
		is_obsolete = true;
	}
	else if (command_String.regionMatches(true,0,"setDataSource",0,13)){
		Message.printWarning ( 1, routine,
		"setDataSource is obsolete.\nUse openHydroBase()." );
		is_obsolete = true;
	}
	else if ( command_String.regionMatches(true,0,"setUseDiversionComments", 0,23) ) {
		Message.printWarning ( 1, routine,
		"setUseDiversionComments() is obsolete.\n" +
		"Use fillUsingDiversionComments()." );
		is_obsolete = true;
	}
	else if ( command_String.regionMatches(true,0,"-slist",0,6) ) {
		Message.printWarning ( 1, routine,"-slist is obsolete.  Use CreateFromList()." );
		is_obsolete = true;
	}
	else if ( command_String.regionMatches(true,0,"-units",0,6) ) {
		Message.printWarning ( 1, routine, "-units is obsolete.  Other commands now handle units." );
		is_obsolete = true;
	}
	else if ( command_String.equalsIgnoreCase("-wy") ) {
		Message.printWarning ( 1, routine,
		"-wy is obsolete.  Use setOutputYearType().  Automatically using setOutputYearType()." );
		is_obsolete = true;
		do_setOutputYearType ( command_String );
	}
	// Put after -wy...
	else if ( command_String.regionMatches(true,0,"-w",0,2) ) {
		do_setWarningLevel ( command_String );
		Message.printWarning ( 1, routine,
			"-w is obsolete.\n" +
			"Use setWarningLevel() instead.");
		is_obsolete = true;
	}
	else if ( command_String.regionMatches(true,0,"setRegressionPeriod",0,19) ) {
		// Set the regression period - this is no longer needed.
		Message.printWarning ( 1, routine,
		"setRegressionPeriod() is used for " +
		"backward-compatibility).\n" +
		"Set dates in fillRegression() instead.  Ignoring.");
		is_obsolete = true;
	}
	else if ( TimeUtil.isDateTime ( StringUtil.getToken(command_String,
		" \t", StringUtil.DELIM_SKIP_BLANKS,0) ) ) { 
		// Old-style date...
		Message.printWarning ( 1, routine, "Setting output " +
		"period with MM/YYYY MM/YYYY is obsolete.\n" +
		"Use SetOutputPeriod()." );
		is_obsolete = true;
	}
	return is_obsolete;
}

/**
Reset all the data to initial values, to be controlled by the commands.
In particular, reset the dates that might have been set in a previous run and which
need to be reset for the current run.
@param AppendResults_boolean Indicates whether the results from the current run should be
appended to a previous run.
*/
private void processCommands_ResetDataForRunStart ( boolean AppendResults_boolean )
throws Exception
{
    // The following are the initial defaults...
    setAutoExtendPeriod ( true );
    setBinaryTSUsed ( false );
    __datatestlist = null;
    __datetime_Hashtable.clear();
    __fill_pattern_ts.removeAllElements();
    setIncludeMissingTS ( false );
    setIgnoreLEZero ( false );
    setInputStart ( null );
    setInputEnd ( null );
    getMissingTS().removeAllElements();
    setOutputDetailedHeader ( false );
    setOutputStart ( null );
    setOutputEnd ( null );
    setOutputYearType ( __CALENDAR_YEAR );
    setPreviewExportedOutput ( false );
    __reference_date = null;
    // Free all data from the previous run...
    if ( !AppendResults_boolean ) {
        clearResults ();
    }
    setBinaryTS ( null );
}

/**
Process a list of time series to produce an output product.
The time series are typically generated from a previous call to
processCommands() or processTimeSeriesCommands().
@param ts_indices List of time series indices to process from the internal
time series list.  If null, all are processed.  The indices do not have to be
in order.
@param proplist List of properties to define the output:
<table width=100% cellpadding=10 cellspacing=0 border=2>
<tr>
<td><b>Property</b></td>	<td><b>Description</b></td>	<td><b>Default</b></td>
</tr>

<tr>
<td><b>OutputFormat</b></td>
<td>
<ul>
<li>-ostatemod  Output StateMod file.</li>
</ul>
</td>
<td>No default.</td>
</tr>

<tr>
<td><b>OutputFile</b></td>
<td>Output file.</td>
<td>No default.</td>
</tr>
</table>
@param comments Comments to add to the to of output files.
@exception IOException if there is an error generating the results.
*/
protected void processTimeSeries ( int ts_indices[], PropList proplist )
throws IOException
{	String	message = null,	// Message string
		routine = "TSEngine.processTimeSeries";

    // List of time series to output, determined from the time series in memory
    // and a list of selected array positions (e.g., from a GUI.

    Vector tslist_output = null;

	// Define a local proplist to better deal with null...
	PropList props = proplist;
	if ( props == null ) {
		props = new PropList ( "" );
	}

	if ( getBinaryTSUsed() ) {
		Message.printStatus ( 2, routine, "Creating output from BinaryTS file..." );
	}
	else {	if ( (__tslist == null) || (__tslist.size()==0) ) {
			message = "No time series to process.";
			Message.printWarning ( 1, routine, message );
			throw new IOException ( message );
		} 
		Message.printStatus ( 1, routine, "Creating output from previously queried time series..." );
	}

	// Put together the Vector to output, given the requested ts_indices.
	// Need to do more work if BinaryTS, but hopefully if BinaryTS
	// batch mode will be used?!  Use a member __tslist_output so we can
	// manage memory and not leave used if an exception occurs (clean up
	// the next time).

	int nts = __tslist.size();
	if ( ts_indices == null ) {
		// Use the entire list...
		tslist_output = __tslist;
	}
	else {
        int ts_indices_size = ts_indices.length;
		if ( tslist_output == null ) {
			tslist_output = new Vector ( ts_indices_size );
		}
		else {	// This does not work because if the Vector is passed
			// to TSViewJFrame, etc., the Vector is reused but the
			// contents will have changed.  Therefore, need to
			// create a new Vector for each output product.  The
			// time series themselves can be re-used.
			//__tslist_output.removeAllElements();
			tslist_output = new Vector ( ts_indices_size );
		}
		for ( int i = 0; i < ts_indices_size; i++ ) {
			if ( (ts_indices[i] >= 0) && (ts_indices[i] < nts) ) {
				tslist_output.addElement ( __tslist.elementAt(ts_indices[i]) );
			}
		}
	}

	// Figure out the output.  This method is going to be called with
	// legacy -o options (batch mode) as well as new PropList syntax.  Make
	// sure to support legacy first and then phase in new approach...

	int output_format = OUTPUT_NONE;
	String precision_string = "*";
	setPreviewExportedOutput ( false );

	String prop_value = props.getValue ( "OutputFormat" );
	Message.printStatus ( 1, routine,"Output format is \""+prop_value+"\"");
	String parameters = props.getValue ( "Parameters" );
	if ( prop_value != null ) {
		// Reports...

		if ( prop_value.equalsIgnoreCase("-odata_coverage_report") ) {
			output_format = OUTPUT_DATA_COVERAGE_REPORT;
		}
		else if ( prop_value.equalsIgnoreCase("-odata_limits_report")) {
			output_format = OUTPUT_DATA_LIMITS_REPORT;
		}
		else if ( prop_value.equalsIgnoreCase(
			"-omonth_mean_summary_report")) {
			output_format = OUTPUT_MONTH_MEAN_SUMMARY_REPORT;
		}
		else if ( prop_value.equalsIgnoreCase(
			"-omonth_total_summary_report")) {
			output_format = OUTPUT_MONTH_TOTAL_SUMMARY_REPORT;
		}
		else if ( prop_value.equalsIgnoreCase("-oyear_to_date_report")){
			output_format = OUTPUT_YEAR_TO_DATE_REPORT;
			try {
                __reference_date = DateTime.parse(parameters);
			}
			catch ( Exception e ) {
				__reference_date = null;
			}
		}

		// Time series file output...

		else if ( prop_value.equalsIgnoreCase("-odatevalue") ) {
			output_format = OUTPUT_DATEVALUE;
		}
		else if ( prop_value.equalsIgnoreCase("-onwsrfsesptraceensemble") ) {
			output_format = OUTPUT_NWSRFSESPTRACEENSEMBLE_FILE;
		}
		else if ( prop_value.equalsIgnoreCase("-onwscard") ) {
			output_format = OUTPUT_NWSCARD_FILE;
		}
		else if ( prop_value.equalsIgnoreCase("-oriverware") ) {
			output_format = OUTPUT_RIVERWARE_FILE;
		}
		else if ( prop_value.equalsIgnoreCase("-oshefa") ) {
			output_format = OUTPUT_SHEFA_FILE;
		}
		else if ( prop_value.equalsIgnoreCase("-ostatemod") ) {
			output_format = OUTPUT_STATEMOD;
		}
		else if ( prop_value.equalsIgnoreCase("-osummary") ) {
			output_format = OUTPUT_SUMMARY;
		}
		else if ( prop_value.equalsIgnoreCase("-otable") ) {
			output_format = OUTPUT_TABLE;
		}
		else if ( prop_value.equalsIgnoreCase("-osummarynostats") ) {
			output_format = OUTPUT_SUMMARY_NO_STATS;
		}

		// Graph output...

		else if ( prop_value.equalsIgnoreCase("-oannual_traces_graph")){
			output_format = OUTPUT_ANNUAL_TRACES_GRAPH;
		}
		else if ( prop_value.equalsIgnoreCase("-obar_graph")){
			output_format = OUTPUT_BAR_GRAPH;
			// Use parameters for the position of the bars.
		}
		else if ( prop_value.equalsIgnoreCase("-odoublemass_graph")){
			output_format = OUTPUT_DOUBLE_MASS_GRAPH;
		}
		else if ( prop_value.equalsIgnoreCase("-oduration_graph")){
			output_format = OUTPUT_DURATION_GRAPH;
		}
		else if ( prop_value.equalsIgnoreCase("-olinegraph")){
			output_format = OUTPUT_LINEGRAPH;
		}
		else if ( prop_value.equalsIgnoreCase("-olinelogygraph")){
			output_format = OUTPUT_LINELOGYGRAPH;
		}
		else if ( prop_value.equalsIgnoreCase("-opointgraph")){
			output_format = OUTPUT_POINT_GRAPH;
		}
		else if ( prop_value.equalsIgnoreCase("-oporgraph")){
			output_format = OUTPUT_PORGRAPH;
		}
		else if (prop_value.equalsIgnoreCase("-oPredictedValue_graph")){
			output_format = OUTPUT_PredictedValue_GRAPH;
		}
		else if ( prop_value.equalsIgnoreCase("-oPredictedValueResidual_graph")){
			output_format = OUTPUT_PredictedValueResidual_GRAPH;
		}
		else if ( prop_value.equalsIgnoreCase("-oxyscatter_graph")){
			output_format = OUTPUT_XY_SCATTER_GRAPH;
		}
	}
	prop_value = props.getValue ( "OutputFile" );
	if ( prop_value != null ) {
		if ( prop_value.equalsIgnoreCase("-preview") ) {
			setPreviewExportedOutput ( true );
		}
		else {
            __output_file = prop_value;
		}
	}
	prop_value = props.getValue ( "Precision" );
	if ( prop_value != null ) {
		if ( prop_value.equals("*") || StringUtil.isInteger(prop_value) ) {
			precision_string = prop_value;
		}
		else {
            __output_file = prop_value;
		}
	}

	if ( output_format == OUTPUT_STATEMOD ) {
		try {
            writeStateModTS ( tslist_output, __output_file,
			precision_string, formatOutputHeaderComments(__ts_processor.getCommands()) );
		} catch ( Exception e ) {
			message = "Error writing StateMod file \"" + __output_file + "\"";
			Message.printWarning ( 1, routine, message );
			Message.printWarning ( 2, routine, e );
			// Why is this done - batch mode only???
			// Free the binary file...
			if ( getBinaryTS() != null ) {
				__binary_ts.delete ();
				__binary_ts = null;
			}
			throw new IOException ( message );
		}
	}
	else if ( output_format == OUTPUT_DATA_COVERAGE_REPORT ) {
		PropList reportProps = new PropList ("ReportJFrame.props");
		reportProps.set ( "HelpKey", "TSTool" );
		reportProps.set ( "TotalWidth", "750" );
		reportProps.set ( "TotalHeight", "550" );
		reportProps.set ( "Title", "Data Coverage Report" );
		reportProps.set ( "DisplayFont", "Courier" );
		reportProps.set ( "DisplaySize", "11" );
		// reportProps.set ( "DisplayStyle", Font.PLAIN );
		reportProps.set ( "PrintFont", "Courier" );
		// reportProps.set ( "PrintFont", Font.PLAIN );
		reportProps.set ( "PrintSize", "7" );
		reportProps.set ( "PageLength", "5000" );
		reportProps.set ( "Search", "true" );

		try {	// For now, put the code in here at the bottom of this
			// file...
			Vector report = createDataCoverageReport ( tslist_output );
			new ReportJFrame ( report, reportProps );
		}
		catch ( Exception e ) {
			Message.printWarning ( 1, routine,
			"Error printing summary." );
			Message.printWarning ( 2, routine, e );
		}
	}
	else if ( output_format == OUTPUT_DATA_LIMITS_REPORT ) {
		PropList reportProps = new PropList ("ReportJFrame.props");
		reportProps.set ( "HelpKey", "TSTool" );
		reportProps.set ( "TotalWidth", "750" );
		reportProps.set ( "TotalHeight", "550" );
		reportProps.set ( "Title", "Data Limits Report" );
		reportProps.set ( "DisplayFont", "Courier" );
		reportProps.set ( "DisplaySize", "11" );
		// reportProps.set ( "DisplayStyle", Font.PLAIN );
		reportProps.set ( "PrintFont", "Courier" );
		// reportProps.set ( "PrintFont", Font.PLAIN );
		reportProps.set ( "PrintSize", "7" );
		reportProps.set ( "PageLength", "5000" );
		reportProps.set ( "Search", "true" );

		try {	// For now, put the code in here at the bottom of this
			// file...
			Vector report = createDataLimitsReport(tslist_output);
			new ReportJFrame ( report, reportProps );
		}
		catch ( Exception e ) {
			Message.printWarning ( 1, routine, "Error printing summary." );
			Message.printWarning ( 2, routine, e );
		}
	}
	else if ( output_format == OUTPUT_DATEVALUE ) {
		if ( getBinaryTSUsed() ) {
			message = "BinaryTS cannot be used with year to date report.";
			Message.printWarning ( 2, routine, message );
			throw new IOException ( message );
		}
		try {
		TS	tspt = null;
		if ( (tslist_output != null) && (tslist_output.size() > 0)){
			tspt = (TS)tslist_output.elementAt(0);
		}
		String units = null;
		if ( tspt != null ) {
			units = tspt.getDataUnits();
			Message.printStatus ( 1, "", "Data units are " + units);
		}

		// Format the comments to add to the top of the file.  In this
		// case, add the commands used to generate the file...
		if ( TSUtil.intervalsMatch ( tslist_output )) {
			// Need date precision to be day...
			//DateTime date1 = _date1;
			// Why was this set here????
			// SAM - 2001-08-27
			//if ( _date1 != null ) {
			//	date1 = new DateTime ( _date1 );
			//	date1.setPrecision (DateTime.PRECISION_DAY);
			//	date1.setDay ( 1 );
			//}
			//DateTime date2 = _date2;
			//if ( _date2 != null ) {
			//	date2 = new DateTime ( _date2 );
			//	date2.setPrecision (DateTime.PRECISION_DAY);
			//	date2.setDay ( TimeUtil.numDaysInMonth(
			//		date2.getMonth(), date2.getYear() ));
			//}
			//DateValueTS.writeTimeSeries ( __tslist_output,
			//	_output_file,
			//	date1, date2, units, true );
			
			DateValueTS.writeTimeSeriesList ( tslist_output,
				__output_file, __OutputStart_DateTime,
				__OutputEnd_DateTime, units, true );
		}
		else {
            Message.printWarning ( 1, routine, "Unable to write " +
			"DateValue time series of different intervals." );
		}
		} catch ( Exception e ) {
			message = "Error writing DateValue file \"" + __output_file + "\"";
			Message.printWarning ( 1, routine, message );
			Message.printWarning ( 2, routine, e );
			// Free the binary file...
			if ( __binary_ts != null ) {
				__binary_ts.delete ();
				setBinaryTS ( null );
			}
			throw new IOException ( message );
		}
	}
	else if ( output_format == OUTPUT_NWSRFSESPTRACEENSEMBLE_FILE ) {
		try {	
			PropList esp_props = new PropList ( "esp" );
			NWSRFS_ESPTraceEnsemble esp = new NWSRFS_ESPTraceEnsemble (	tslist_output, esp_props );
			esp.writeESPTraceEnsembleFile ( __output_file );
		} catch ( Exception e ) {
			message = "Error writing NWSRFS ESP Trace Ensemble file \"" + __output_file + "\"";
			Message.printWarning ( 1, routine, message );
			Message.printWarning ( 2, routine, e );
			throw new IOException ( message );
		}
	}
	else if ( (output_format == OUTPUT_MONTH_MEAN_SUMMARY_REPORT) ||
		(output_format == OUTPUT_MONTH_TOTAL_SUMMARY_REPORT) ) {
		String daytype = "Mean";
		PropList reportProps = new PropList ("ReportJFrame.props");
		reportProps.set ( "HelpKey", "TSTool" );
		reportProps.set ( "TotalWidth", "750" );
		reportProps.set ( "TotalHeight", "550" );
		if ( output_format == OUTPUT_MONTH_MEAN_SUMMARY_REPORT ) {
			reportProps.set ( "Title",
			"Monthly Summary Report (Daily Means)" );
			daytype = "Mean";
		}
		else {	reportProps.set ( "Title",
			"Monthly Summary Report (Daily Totals)" );
			daytype = "Total";
		}
		reportProps.set ( "DisplayFont", "Courier" );
		reportProps.set ( "DisplaySize", "11" );
		// reportProps.set ( "DisplayStyle", Font.PLAIN );
		reportProps.set ( "PrintFont", "Courier" );
		// reportProps.set ( "PrintFont", Font.PLAIN );
		reportProps.set ( "PrintSize", "7" );
		reportProps.set ( "PageLength", "5000" );
		reportProps.set ( "Search", "true" );

		PropList sumprops = new PropList ( "" );
		sumprops.set ( "DayType", daytype );
		if ( getOutputYearTypeInt() == __WATER_YEAR ) {
			sumprops.set ( "CalendarType", "WaterYear" );
		}
		else {	sumprops.set ( "CalendarType", "CalendarYear" );
		}

		try {	// For now, put the code in here at the bottom of this
			// file...
			Vector report = createMonthSummaryReport ( tslist_output, sumprops );
			new ReportJFrame ( report, reportProps );
		}
		catch ( Exception e ) {
			Message.printWarning ( 1, routine, "Error printing summary." );
			Message.printWarning ( 2, routine, e );
		}
	}
	else if ( output_format == OUTPUT_NWSCARD_FILE ) {
		if ( getBinaryTSUsed() ) {
			message = "BinaryTS cannot be used with NWS Card output.";
			Message.printWarning ( 1, routine, message );
			throw new IOException ( message );
		}
		// Write a NWS Card file containing a single time series.
		try {
		TS	tspt = null;
		int	list_size = 0;
		if ( (tslist_output != null) && (tslist_output.size() > 0)){
			tspt = (TS)tslist_output.elementAt(0);
			list_size = tslist_output.size();
		}
		// NWS Card files can only contain one time series...
		if ( list_size != 1 ) {
			message = "Only 1 time series can be written to a NWS Card file";
			Message.printWarning ( 1, routine, message );
			throw new Exception ( message );
		}
		String units = null;
		if ( tspt != null ) {
			units = tspt.getDataUnits();
			Message.printStatus ( 1, "", "Data units are " + units);
		}

		// Format the comments to add to the top of the file.  In this
		// case, add the commands used to generate the file...
		int interval_mult = 1;
		if ( getBinaryTSUsed() ) {
			interval_mult = __binary_ts.getDataIntervalMult();
		}
		else {
			interval_mult = ((TS)tslist_output.elementAt(0)).getDataIntervalMult();
		}
		// Need date precision to be hour and NWS uses hour 1 - 24 so
		// adjust dates accordingly.
		DateTime date1 = __OutputStart_DateTime;
		if ( __OutputStart_DateTime != null ) {
			date1 = new DateTime ( __OutputStart_DateTime );
			date1.setPrecision (DateTime.PRECISION_HOUR);
			date1.setDay ( 1 );
			date1.setHour ( interval_mult );
		}
		DateTime date2 = __OutputEnd_DateTime;
		if ( __OutputEnd_DateTime != null ) {
			date2 = new DateTime ( __OutputEnd_DateTime );
			date2.setPrecision (DateTime.PRECISION_HOUR);
			date2.setDay ( TimeUtil.numDaysInMonth(	date2.getMonth(), date2.getYear() ));
			date2.addDay ( 1 );
			date2.setHour ( 0 );
		}
		NWSCardTS.writeTimeSeries ( (TS)tslist_output.elementAt(0),	__output_file, date1, date2, units, true );
		} catch ( Exception e ) {
			message = "Error writing NWS Card file \"" + __output_file + "\"";
			Message.printWarning ( 1, routine, message );
			Message.printWarning ( 2, routine, e );
			// Free the binary file...
			if ( getBinaryTS() != null ) {
				__binary_ts.delete ();
				setBinaryTS ( null );
			}
			throw new IOException ( message );
		}
	}
	else if ( output_format == OUTPUT_RIVERWARE_FILE ) {
		if ( getBinaryTSUsed() ) {
			message = "BinaryTS cannot be used with RiverWare file.";
			Message.printWarning ( 1, routine, message );
			throw new IOException ( message );
		}
		// Write a RiverWare file containing a single time series.
		try {
		TS	tspt = null;
		int	list_size = 0;
		if ( (tslist_output != null) && (tslist_output.size() > 0)){
			tspt = (TS)tslist_output.elementAt(0);
			list_size = tslist_output.size();
		}
		// RiverWare files can only contain one time series...
		if ( list_size != 1 ) {
			message =
			"Only 1 time series can be written to a RiverWare file";
			Message.printWarning ( 1, routine, message );
			throw new Exception ( message );
		}
		String units = null;
		if ( tspt != null ) {
			units = tspt.getDataUnits();
			Message.printStatus ( 1, "", "Data units are " + units);
		}

		// Format the comments to add to the top of the file.  In this
		// case, add the commands used to generate the file...
		RiverWareTS.writeTimeSeries ( (TS)tslist_output.elementAt(0),
			__output_file, __OutputStart_DateTime,
			__OutputEnd_DateTime, units, 1.0, null, -1.0, true );
		} catch ( Exception e ) {
			message = "Error writing RiverWare file \"" + __output_file + "\"";
			Message.printWarning ( 1, routine, message );
			Message.printWarning ( 2, routine, e );
			// Free the binary file...
			if ( __binary_ts != null ) {
				__binary_ts.delete ();
				__binary_ts = null;
			}
			throw new IOException ( message );
		}
	}
	else if ( output_format == OUTPUT_SHEFA_FILE ) {
		try {
			Vector units_Vector = null;
			Vector PE_Vector = ShefATS.getPEForTimeSeries (tslist_output );
			Vector Duration_Vector = null;
			Vector AltID_Vector = null;
			PropList shef_props = new PropList ( "SHEF" );
			shef_props.set ( "HourMax=24" );
			ShefATS.writeTimeSeriesList ( tslist_output,
				__output_file, __OutputStart_DateTime,
				__OutputEnd_DateTime,
				units_Vector, PE_Vector,
				Duration_Vector, AltID_Vector, shef_props );
		} catch ( Exception e ) {
			message = "Error writing SHEF A file \"" + __output_file + "\"";
			Message.printWarning ( 1, routine, message );
			Message.printWarning ( 2, routine, e );
			throw new IOException ( message );
		}
	}
	else if ( (output_format == OUTPUT_SUMMARY) || (output_format == OUTPUT_SUMMARY_NO_STATS) ) {
		if ( getBinaryTSUsed() ) {
			message = "BinaryTS cannot be used with year to date report.";
			Message.printWarning ( 1, routine, message );
			throw new IOException ( message );
		}
		try {
		// First need to get the summary strings...
		PropList sumprops = new PropList ( "Summary" );
		sumprops.set ( "Format", "Summary" );
		if ( getOutputYearTypeInt() == __WATER_YEAR ) {
			sumprops.set ( "CalendarType", "WaterYear" );
		}
		else {
            sumprops.set ( "CalendarType", "CalendarYear" );
		}
		if ( getOutputDetailedHeader() ) {
			sumprops.set("PrintGenesis","true");
		}
		else {	sumprops.set("PrintGenesis","false");
		}
		// Check the first time series.  If NWSCARD or DateValue, don't
		// use comments for header...
		/* TODO SAM 2004-07-20 - try default header always -
			transition to HydroBase comments being only additional
			information
		else {	// HydroBase, so use the comments for the header...
			sumprops.set ( "UseCommentsForHeader", "true" );
		}
		if ( _non_co_detected ) {
			// Data other that HydroBase, so format using standard
			// headers...
			sumprops.set ( "UseCommentsForHeader", "false" );
		}
		*/
		sumprops.set ( "PrintHeader", "true" );
		sumprops.set ( "PrintComments", "true" );
		if ( output_format == OUTPUT_SUMMARY ) {
			// Get the statistics...
			sumprops.set ( "PrintMinStats", "true" );
			sumprops.set ( "PrintMaxStats", "true" );
			sumprops.set ( "PrintMeanStats", "true" );
			sumprops.set ( "PrintNotes", "true" );
		}
		else if ( output_format == OUTPUT_SUMMARY_NO_STATS ) {
			// Don't want the statistics or the notes but do want
			// a line at the bottom (kludge for Ayres software)...
			sumprops.set ( "PrintMinStats", "false" );
			sumprops.set ( "PrintMaxStats", "false" );
			sumprops.set ( "PrintMeanStats", "false" );
			sumprops.set ( "PrintNotes", "false" );
		}

		if ( IOUtil.isBatch() || !getPreviewExportedOutput() ) {
			try {
                Vector summary = TSUtil.formatOutput (__output_file, tslist_output, sumprops );	
				// Just write the summary to the given file...
				IOUtil.printStringList ( __output_file, summary);
			} catch ( Exception e ) {
				Message.printWarning ( 1, routine,"Unable to print summary to file \"" + __output_file + "\"" );
			}
		}
		else {
            PropList reportProps=new PropList("ReportJFrame.props");
			reportProps.set ( "HelpKey", "TSTool" );
			reportProps.set ( "TotalWidth", "750" );
			reportProps.set ( "TotalHeight", "550" );
			reportProps.set ( "Title", "Summary" );
			reportProps.set ( "DisplayFont", "Courier" );
			reportProps.set ( "DisplaySize", "11" );
			// reportProp.set ( "DisplayStyle", Font.PLAIN );
			reportProps.set ( "PrintFont", "Courier" );
			// reportProp.set ( "PrintFont", Font.PLAIN );
			reportProps.set ( "PrintSize", "7" );
			//reportProps.set ( "PageLength", "100" );
			reportProps.set ( "PageLength", "100000" );

			try {
				Vector summary = TSUtil.formatOutput ( tslist_output, sumprops );
				// Now display (the user can save as a file,
				// etc.).
/*
				if (	(_output_filter ==
					OUTPUT_FILTER_DATA_COVERAGE)
					&& !_frost_dates_detected ) {
					// Add the summary to the results...
					summary.addElement ( "" );
					summary =
					StringUtil.addListToStringList (
					summary,
					TSAnalyst.getDataCoverageReport() );
				}
*/
				new ReportJFrame ( summary, reportProps );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1, routine,
				"Error printing summary." );
				Message.printWarning ( 2, routine, e );
			}
		}
		}
		catch ( Exception e ) {
			Message.printWarning ( 2, routine, e );
			message = "Error creating summary";
			Message.printWarning ( 1, routine, message );
			throw new IOException ( message );
		}
	}
	else if ( output_format == OUTPUT_TABLE ) {
		// A table output.  For now, this does not understand BinaryTS
		// output.  Just copy the graph code and change for table.  AT
		// some point, need to initialize all the view data at the
		// same time in case the user changes views interactively after
		// the initial view.
		if ( getBinaryTSUsed() ) {
			message = "BinaryTS cannot be used with tables.";
			Message.printWarning ( 1, routine, message );
			throw new IOException ( message );
		}
		// Temporary copy of data...
		Vector tslist = tslist_output;
		try {
		if ( IOUtil.isBatch() ) {
			Message.printWarning ( 1, routine,
			"Can only generate a table from GUI" );
			return;
		}

		PropList graphprops = new PropList ( "Table" );
		// Set graph properties for a simple graph...
		graphprops.set ( "ExtendedLegend", "true" );
		graphprops.set ( "HelpKey", "TSTool.TableMenu" );
		graphprops.set ( "DataUnits", ((TS)tslist.elementAt(0)).getDataUnits() );
		graphprops.set ( "YAxisLabelString", ((TS)tslist.elementAt(0)).getDataUnits() );
		if ( getOutputYearTypeInt() == __WATER_YEAR ) {
			graphprops.set ( "CalendarType", "WaterYear" );
		}
		else {	graphprops.set ( "CalendarType", "CalendarYear" );
		}
		// Check the first time series.  If NWSCARD or DateValue, don't
		// use comments for header...
		/* TODO SAM 2004-07-20 - try default header always -
			transition to HydroBase comments being only additional
			information
		if ( _non_co_detected ) {
			graphprops.set ( "UseCommentsForHeader", "false" );
		}
		else {	graphprops.set ( "UseCommentsForHeader", "true" );
		}
		*/
		// Set the total size of the graph window...
		graphprops.set ( "TotalWidth", "600" );
		graphprops.set ( "TotalHeight", "400" );

		// Default properties...
		graphprops.set("GraphType=Line");

		graphprops.set ( "InitialView", "Table" );
		// Summary properties for secondary displays (copy
		// from summary output)...
		//graphprops.set ( "HelpKey", "TSTool.ExportMenu" );
		graphprops.set ( "TotalWidth", "600" );
		graphprops.set ( "TotalHeight", "400" );
		//graphprops.set ( "Title", "Summary" );
		graphprops.set ( "DisplayFont", "Courier" );
		graphprops.set ( "DisplaySize", "11" );
		graphprops.set ( "PrintFont", "Courier" );
		graphprops.set ( "PrintSize", "7" );
		graphprops.set ( "PageLength", "100" );
		TSViewJFrame view = new TSViewJFrame ( tslist, graphprops );
		addTSViewTSProductDMIs ( view );
		addTSViewTSProductAnnotationProviders ( view );
		// For garbage collection...
		view = null;
		tslist = null;
		}
		catch ( Exception e ) {
			Message.printWarning ( 2, routine, e );
			message = "Error creating table";
			Message.printWarning ( 1, routine, message );
			throw new IOException ( message );
		}
	}
	else if ( output_format == OUTPUT_YEAR_TO_DATE_REPORT ) {
		if ( getBinaryTSUsed() ) {
			message = "BinaryTS cannot be used with year to date report.";
			Message.printWarning ( 1, routine, message );
			throw new IOException ( message );
		}
		PropList reportProps = new PropList ("ReportJFrame.props");
		reportProps.set ( "HelpKey", "TSTool" );
		reportProps.set ( "TotalWidth", "750" );
		reportProps.set ( "TotalHeight", "550" );
		reportProps.set ( "Title", "Year to Date Report" );
		reportProps.set ( "DisplayFont", "Courier" );
		reportProps.set ( "DisplaySize", "11" );
		// reportProps.set ( "DisplayStyle", Font.PLAIN );
		reportProps.set ( "PrintFont", "Courier" );
		// reportProps.set ( "PrintFont", Font.PLAIN );
		reportProps.set ( "PrintSize", "7" );
		reportProps.set ( "PageLength", "5000" );
		reportProps.set ( "Search", "true" );

		try {
            Vector report = createYearToDateReport (tslist_output,__reference_date, null );
			new ReportJFrame ( report, reportProps );
		}
		catch ( Exception e ) {
			Message.printWarning ( 1, routine,"Error printing summary." );
			Message.printWarning ( 2, routine, e );
		}
	}
	else if (	(output_format == OUTPUT_ANNUAL_TRACES_GRAPH) ||
			(output_format == OUTPUT_BAR_GRAPH) ||
			(output_format == OUTPUT_DOUBLE_MASS_GRAPH) ||
			(output_format == OUTPUT_DURATION_GRAPH) ||
			(output_format == OUTPUT_LINEGRAPH) ||
			(output_format == OUTPUT_LINELOGYGRAPH) ||
			(output_format == OUTPUT_PERCENT_EXCEED_GRAPH) ||
			(output_format == OUTPUT_POINT_GRAPH) ||
			(output_format == OUTPUT_PORGRAPH) ||
			(output_format == OUTPUT_PredictedValue_GRAPH) ||
			(output_format == OUTPUT_PredictedValueResidual_GRAPH)||
			(output_format == OUTPUT_XY_SCATTER_GRAPH) ) {
		// A graph type.  For now, this does not understand BinaryTS
		// output...
		if ( getBinaryTSUsed() ) {
			message = "BinaryTS cannot be used with graphs.";
			Message.printWarning ( 1, routine, message );
			throw new IOException ( message );
		}
		// Temporary copy of data...
		Vector tslist = tslist_output;
		try {
		if ( IOUtil.isBatch() ) {
			Message.printWarning ( 1, routine, "Can only graph from GUI" );
			return;
		}

		PropList graphprops = new PropList ( "Graph" );
		graphprops.set ( "ExtendedLegend", "true" );
		graphprops.set ( "HelpKey", "TSTool.GraphMenu" );
		graphprops.set ( "DataUnits", ((TS)tslist.elementAt(0)).getDataUnits() );
		graphprops.set ( "YAxisLabelString",((TS)tslist.elementAt(0)).getDataUnits() );
		if ( getOutputYearTypeInt() == __WATER_YEAR ) {
			graphprops.set ( "CalendarType", "WaterYear" );
		}
		else {	graphprops.set ( "CalendarType", "CalendarYear" );
		}
		// Check the first time series.  If NWSCARD or DateValue, don't
		// use comments for header...
		/* TODO SAM 2004-07-20 - try default header always -
			transition to HydroBase comments being only additional
			information
		if ( _non_co_detected ) {
			graphprops.set ( "UseCommentsForHeader", "false" );
		}
		else {	graphprops.set ( "UseCommentsForHeader", "true" );
		}
		*/
		// Set the total size of the graph window...
		graphprops.set ( "TotalWidth", "600" );
		graphprops.set ( "TotalHeight", "400" );

		if (	(tslist != null) &&
			(output_format == OUTPUT_ANNUAL_TRACES_GRAPH) ) {
/* Currently disabled...
			// Go through each time series in the list and break
			// into annual traces, and then plot those results...
			Message.printStatus ( 1, routine,
			"Splitting time series into traces..." );
			int size = tslist.size();
			Vector new_tslist = new Vector ( size );
			Vector traces = null;
			DateTime reference_date = new DateTime (
				DateTime.PRECISION_DAY );
			reference_date.setYear ( _reference_year );
			reference_date.setMonth ( 1 );
			reference_date.setDay ( 1 );
			size = tslist.size();
			for ( int i = 0; i < size; i++ ) {
				ts = (TS)tslist.elementAt(i);
				if ( ts == null ) {
					continue;
				}
				// Get the trace time series, using Jan 1 of the
				// reference year.
				// This allows the raw data to temporally
				// correct but is a pain to deal with in the
				// plot code...
				//traces = TSUtil.getTracesFromTS ( ts,
				//		reference_date, null );
				// Using this results in some funny labels but
				// at least things plot...
				traces = TSUtil.getTracesFromTS ( ts,
						reference_date, reference_date);
				if ( traces == null ) {
					// If real-time data, add to the
					// output...
					if (	ts.getDataIntervalBase() ==
						TimeInterval.IRREGULAR ) {
						new_tslist.addElement ( ts );
					}
				}
				else {	// Add the traces to the tslist.  Set
					// the legend explicitly because the
					// default is to print the period which
					// is not useful...
					int traces_size = traces.size();
					Message.printStatus ( 1, routine,
					"Split " + ts.getIdentifier() +
					" into " + traces_size + " traces" );
					TS ts2 = null;
					for ( int j = 0; j < traces_size; j++ ){
						ts2 = (TS)traces.elementAt(j);
						ts2.setLegend ( "%D, %F" );
						new_tslist.addElement ( ts2 );
					}
				}
			}
			// Now replace the original list with the new one...
			tslist = new_tslist;
			// The offset only affects the graph.  All other data
			// are as if they were slices out of the original
			// data...
			//graphprops.set ("ReferenceDate",
			//reference_date.toString());
			graphprops.set ( "Title", "Annual Traces" );
			graphprops.set ( "XAxis.Format", "MM-DD" );
*/
		}
		else if ( output_format == OUTPUT_BAR_GRAPH ) {
			graphprops.set("GraphType=Bar");
			graphprops.set("BarPosition=" + parameters );
		}
		else if ( output_format == OUTPUT_DOUBLE_MASS_GRAPH ) {
			graphprops.set("GraphType=Double-Mass");
		}
		else if ( output_format == OUTPUT_DURATION_GRAPH ) {
			graphprops.set("GraphType=Duration");
		}
		else if ( output_format == OUTPUT_LINELOGYGRAPH ) {
			graphprops.set("YAxisType=Log");
			// Handle flags...
			/* TODO SAM 2006-05-22
			Can be very slow because blank labels are not ignored
			in low-level code.
			GRTS_Util.addDefaultPropertiesForDataFlags (
				tslist, graphprops );
			*/
		}
		else if ( output_format == OUTPUT_PERCENT_EXCEED_GRAPH ) {
			graphprops.set("GraphType=PercentExceedance");
			graphprops.set("Title=Period Exceedance Curve");
			graphprops.set("XAxisLabelString=" +
				"Percent of Time Exceeded");
		}
		else if ( output_format == OUTPUT_POINT_GRAPH ) {
			graphprops.set("GraphType=Point");
			// Handle flags...
			/* TODO SAM 2006-05-22
			Can be very slow because blank labels are not ignored
			in low-level code.
			GRTS_Util.addDefaultPropertiesForDataFlags (
				tslist, graphprops );
			*/
		}
		else if ( output_format == OUTPUT_PORGRAPH ) {
			graphprops.set("GraphType=PeriodOfRecord");
			graphprops.set("LineWidth=Thick");
			graphprops.set("Title=Period of Record");
			graphprops.set("YAxisLabelString=Legend Index");
		}
		else if ( output_format == OUTPUT_PredictedValue_GRAPH ) {
			graphprops.set("GraphType=PredictedValue");
		}
		else if ( output_format == OUTPUT_PredictedValueResidual_GRAPH){
			graphprops.set("GraphType=PredictedValueResidual");
		}
		else if ( output_format == OUTPUT_XY_SCATTER_GRAPH ) {
			graphprops.set("GraphType=XY-Scatter");
		}
		else {	// Default properties...
			graphprops.set("GraphType=Line");
			// Handle flags...
			/* TODO SAM 2006-05-22
			Can be very slow because blank labels are not ignored
			in low-level code.
			GRTS_Util.addDefaultPropertiesForDataFlags (
				tslist, graphprops );
			*/
		}

		// For now always use new graph...
		graphprops.set ( "InitialView", "Graph" );
		// Summary properties for secondary displays (copy
		// from summary output)...
		//graphprops.set ( "HelpKey", "TSTool.ExportMenu" );
		graphprops.set ( "TotalWidth", "600" );
		graphprops.set ( "TotalHeight", "400" );
		//graphprops.set ( "Title", "Summary" );
		graphprops.set ( "DisplayFont", "Courier" );
		graphprops.set ( "DisplaySize", "11" );
		graphprops.set ( "PrintFont", "Courier" );
		graphprops.set ( "PrintSize", "7" );
		graphprops.set ( "PageLength", "100" );
		TSViewJFrame view = new TSViewJFrame ( tslist, graphprops );
		// Connect dynamic data objects...
		addTSViewTSProductDMIs ( view );
		addTSViewTSProductAnnotationProviders ( view );
		// For garbage collection...
		view = null;
		tslist = null;
		}
		catch ( Exception e ) {
			Message.printWarning ( 2, routine, e );
			message = "Error creating graph";
			Message.printWarning ( 1, routine, message );
			throw new IOException ( message );
		}
	}

	Message.printStatus ( 1, routine, "Ending time series output at:  " +
	new DateTime(DateTime.DATE_CURRENT).toString() );
}

/**
Process a time series action, meaning insert or update in the list.
@param ts_action INSERT_TS to insert at the position, UPDATE_TS to update at
the position, NONE to do to nothing.
@param ts Time series to act on.
@param ts_pos Position in the time series list to perform action.
@exception Exception if an error occurs.
*/
protected void processTimeSeriesAction ( int ts_action, TS ts, int ts_pos )
throws Exception
{	if ( ts_action == INSERT_TS ) {
		// Add new time series to the list...
		setTimeSeries ( ts, ts_pos );
		updateComments ( ts );
	}
	else if ( ts_action == UPDATE_TS ) {
		// Update in the time series list...
		setTimeSeries ( ts, ts_pos );
		updateComments ( ts );
	}
}

/**
Get a time series from the database/file using current date and units settings.
@param wl Warning level if the time series is not found.  Typically this will be 1 if
mimicing the old processing, and 2+ during transition to the new command status
approach.
@param tsident_string Time series identifier.
@return the time series.
@exception Exception if there is an error reading the time series.
*/
protected TS readTimeSeries ( int wl, String command_tag, String tsident_string )
throws Exception
{	return readTimeSeries ( wl, command_tag, tsident_string, false );
}

/**
Get a time series from the database/file using current date and units settings.
@param wl Warning level if the time series is not found.  Typically this will be 1 if
mimicing the old processing, and 2+ during transition to the new command status
approach.
@param tsident_string Time series identifier.
@param full_period Indicates whether the full period should be queried.
@return the time series.
@exception Exception if there is an error reading the time series.
*/
protected TS readTimeSeries ( int wl, String command_tag, String tsident_string,
				boolean full_period )
throws Exception
{	return readTimeSeries ( wl, command_tag, tsident_string, full_period, false);
}

/**
Read a time series from a database or file.  The following occur related to
periods:
<ol>
<li>	If the query period has been specified, then it is used to limit the
	read/query.  Otherwise, the full period is retrieved.  Normally the
	query period is only specified to improve performance (e.g., to get
	a short period of real-time data or a short period from a long
	time series file).</li>
<li>	If the averaging period is specified, it will be used to compute
	averages.  Otherwise, the full period is used.</li>
<li>	If the output period is specified, then the time series that are read
	will be extended to the output period if necessary.  If the output
	period is within the returned data, the period is not changed (it is not
	shortened).</li>
</ol>
@param wl Warning level if the time series is not found.  Typically this will be 1 if
mimicing the old processing, and 2+ during transition to the new command status
approach.
@param tsident_string Time series identifier for time series.
@param full_period If true, indicates that the full period is to be queried.
If false, the output period will be queried.
@param ignore_errors If true, ignore errors and return a null time series.
If false, return a zero-filled time series.
SAMX - need to phase out "full_period".
@exception Exception if there is an error reading the time series.
*/
private TS readTimeSeries (	int wl, String command_tag, String tsident_string,
		boolean full_period,
		boolean ignore_errors )
throws Exception
{	TS	ts = null;
	String	routine = "TSEngine.readTimeSeries";
	
	// Figure out what dates to use for the query...

	DateTime query_date1 = null;	// Default is to query all data
	DateTime query_date2 = null;
	if ( haveInputPeriod() ) {
		// Use the query period...
		query_date1 = __InputStart_DateTime;
		query_date2 = __InputEnd_DateTime;
	}

	// Read the time series using the generic code...

	ts = readTimeSeries0 (	tsident_string,
				query_date1,
				query_date2,
				null,		// units
				true );		// read data

	if ( ts == null ) {
		// Not able to retrieve the time series.  Print a warning...
		if ( ignore_errors ) {
			// Return null...
			return ts;
		}
		// Else do not ignore errors...
		if ( getIncludeMissingTS() && haveOutputPeriod() ) {
			// Even if time series is missing, create an empty one
			// for output.
			ts = TSUtil.newTimeSeries ( tsident_string, true );
			// else leave null and ignore
			if ( ts != null ) {
				ts.setDate1 ( __OutputStart_DateTime );
				ts.setDate2 ( __OutputEnd_DateTime );
				// Leave original dates as is.  The
				// following will fill with missing...
				ts.allocateDataSpace();
				Vector v = StringUtil.breakStringList (	tsident_string, "~", 0 );
				// Version without the input...
				String tsident_string2;
				tsident_string2 = (String)v.elementAt(0);
				ts.setIdentifier ( tsident_string2 );
				Message.printStatus ( 1, routine,
				"Created empty time series for \"" +
				tsident_string2 + "\" - not in DB and" +
				" setIncludeMissingTS(true) is specified." );
			}
		}
		else {	// Not able to query the time series and we are not
			// supposed to create empty time series...
			String message = 
				"Null TS from read and not creating blank - unable to" +
				" process command:\n\""+ tsident_string +"\".\n" +
				"You must correct the command.  " +
				"Make sure that the data are in "+
				"the database or\ninput file" +
				" or use time series creation commands.";
			Message.printWarning ( wl,
			MessageUtil.formatMessageTag(command_tag,
			++_fatal_error_count), routine, message );
			getMissingTS().addElement(tsident_string);
			throw new TimeSeriesNotFoundException ( message );
		}
	}
	else {
        if ( Message.isDebugOn ) {
			Message.printDebug ( 1, routine, "Retrieved TS" );
		}
		// Now do the second set of processing on the time series (e.g.,
		// to guarantee a period that is at least as long as the output
		// period.
		// SAMX - passing tsident_string2 causes problems - the
		// input is lost...
		readTimeSeries2 ( ts, tsident_string, full_period );
	}
	return ts;
}

/**
Read a time series.  This method is called internally by TSEngine code and when
TSEngine serves as a TSSupplier when processing TSProducts.  It actually tries
to read a time series from a file or database.
@param tsident_string Time series identifier to read.
@param query_date1 First date to query.  If specified as null the entire period
will be read.
@param query_date2 Last date to query.  If specified as null the entire period
will be read.
@param units Requested units to return data.  If specified as null or an
empty string the units will not be converted.
@param read_data if true, the data will be read.  If false, only the time series
header will be read.
@return the requested time series or null if an error.
@exception Exception if there is an error reading the time series.
*/
private TS readTimeSeries0 (	String tsident_string,
				DateTime query_date1, DateTime query_date2,
				String units, boolean read_data )
throws Exception
{	String routine = "TSEngine.readTimeSeries0";

	if ( Message.isDebugOn ) {
		Message.printDebug ( 10, routine,
		"Getting time series \"" + tsident_string + "\"" );
	}

	// Separate out the input from the TSID...

	Vector v = StringUtil.breakStringList ( tsident_string, "~", 0 );
	String tsident_string2;	// Version without the input...
	String input_type = null;
	String input_name = null;
    String input_name_full = null;  // input name with full path
	tsident_string2 = (String)v.elementAt(0);
	if ( v.size() == 2 ) {
		input_type = (String)v.elementAt(1);
	}
	else if ( v.size() == 3 ) {
		input_type = (String)v.elementAt(1);
		input_name = (String)v.elementAt(2);
        input_name_full = IOUtil.verifyPathForOS(
                IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(__ts_processor),input_name) );
	}

	// TSIdent uses only the first part of the identifier...
	// TODO SAM 2005-05-22 (why? to avoid confusing the following code?)

	TSIdent tsident = new TSIdent ( tsident_string2 );
	String source = tsident.getSource();

	// Now make a decision about which code to call to read the time
	// series.  Always check the new convention first.

	TS ts = null;
	if ( (input_type != null) && input_type.equalsIgnoreCase("DateValue") ) {
		// New style TSID~input_type~input_name
		try {	ts = DateValueTS.readTimeSeries ( tsident_string2,
				input_name_full, query_date1, query_date2, units,
				true );
		}
		catch ( Exception te ) {
			Message.printWarning ( 2, routine, te );
			ts = null;
		}
	}
	else if ( source.equalsIgnoreCase("DateValue") ) {
		// Old style (scenario may or may not be used to find the file)...
		try {	ts = DateValueTS.readTimeSeries ( tsident_string,
				query_date1, query_date2, units, true );
		}
		catch ( Exception e ) {
			Message.printWarning ( 2, routine, e );
			ts = null;
		}
	}
	/* TODO SAM Re-enable services code
	else if (	(input_type != null) &&
		input_type.equalsIgnoreCase(PersistenceType.DATE_VALUES.getTSIdentString()) ) {
		// Test new data services for DateValue file...
		try {	TSIdent tsident_full = new TSIdent ( tsident_string );
		        TSRequestTO tsRequestTO = new TSRequestTO ();// tsident_full 
		        TSIdentityTO tsIdentityTO = new TSIdentityTO();
		        tsIdentityTO.setPersistenceTypeStr(PersistenceType.DATE_VALUES.toString());
		        tsIdentityTO.setTsIdentString(tsident_full.toString());
		        tsRequestTO.setTsIdentityTO(tsIdentityTO);
				boolean GET_ALL_DATA = false;
                TimeSeries timeseries = new TimeSeriesFabricator().getDomainObject(tsRequestTO, GET_ALL_DATA );
				ts = timeseries.getTS();
		}
		catch ( Exception e ) {
			Message.printWarning ( 2, routine, e );
			ts = null;
		}
	}
*/
	else if (	(input_type != null) &&
			input_type.equalsIgnoreCase("DIADvisor") ) {
		// New style TSID~input_type~input_name for DIADvisor...
		try {	ts = __DIADvisor_dmi.readTimeSeries ( tsident_string2,
				query_date1, query_date2, units, true );
		}
		catch ( Exception te ) {
			Message.printWarning ( 2, routine,
			"Error reading time series \"" + tsident_string2 + "\" from DIADvisor" );
			Message.printWarning ( 2, routine, te );
			Message.printWarning ( 2, routine, "Op:" +__DIADvisor_dmi.getLastSQLString() );
			Message.printWarning ( 2, routine, "Archive:" +	__DIADvisor_archive_dmi.getLastSQLString() );
			ts = null;
		}
		// For now, if the time series does not have data, set it to null...
		if ( ts != null ) {
			if ( !ts.hasData() ) {
				Message.printWarning ( 2, routine,
				"Time series \"" + tsident_string2 + "\" does not have data.  Treating as null." );
				ts = null;
			}
		}
	}
	else if ((input_type != null) && input_type.equalsIgnoreCase("HydroBase") ) {
		if ( Message.isDebugOn ) {
			Message.printDebug ( 10, routine, "Reading time series..." +
			tsident_string + "," + query_date1 + "," + query_date2);
		}
		try {	HydroBaseDMI hbdmi = getHydroBaseDMI ( input_name );
			if ( hbdmi == null ) {
				Message.printWarning ( 2, routine, "Unable to get HydroBase connection for " +
				"input name \"" + input_name +	"\".  Unable to read time series." );
				ts = null;
			}
			else {	ts = hbdmi.readTimeSeries ( tsident_string, query_date1, query_date2, units, true,
                    null );	// HydroBaseDMI read props Use defaults here
			}
			if ( Message.isDebugOn ) {
				Message.printStatus ( 10, routine, "...done reading time series." );
			}
		}
		catch ( Exception e ) {
			Message.printWarning ( 3, routine, "Error from HydroBaseDMI.readTimeSeries" );
			Message.printWarning ( 3, routine, e );
			ts = null;
		}
	}
	else if ((input_type != null) && input_type.equalsIgnoreCase("MexicoCSMN") ) {
		// New style TSID~input_type~input_name for MexicoCSMN...
		// Pass the front TSID as the first argument and the
		// input_name file as the second...
		try {	ts = MexicoCsmnTS.readTimeSeries ( tsident_string2,
				input_name_full, query_date1, query_date2, units,
				true );
		}
		catch ( Exception te ) {
			ts = null;
		}
	}
	else if ((input_type != null) && input_type.equalsIgnoreCase("MODSIM") ) {
		// New style TSID~input_type~input_name
		try {	ts = ModsimTS.readTimeSeries ( tsident_string2,
				input_name_full, query_date1, query_date2, units,
				true );
		}
		catch ( Exception te ) {
			ts = null;
		}
	}
	else if ((input_type != null) && input_type.equalsIgnoreCase("NWSCARD") ) {
		// New style TSID~input_type~input_name for NWSCardTS...
		//Message.printStatus ( 1, routine, "Trying to read \"" +
		//		tsident_string2 + "\" \"" + input_name + "\"" );
		ts = NWSCardTS.readTimeSeries ( tsident_string2, input_name_full,
			query_date1, query_date2, units, true );
		//Message.printStatus ( 1, "", "SAMX TSEngine TS id = \"" +
			//ts.getIdentifier().toString(true) + "\"" );
	}
	else if ((input_type != null) && input_type.equalsIgnoreCase("NWSRFS_ESPTraceEnsemble") ) {
		// Binary ESP Trace Ensemble file...
		try {
            // For now read the file for each trace...
			// TODO SAM 2004-11-29 need to optimize so the
			// file does not need to get reread...
			NWSRFS_ESPTraceEnsemble ensemble = new NWSRFS_ESPTraceEnsemble ( input_name_full, true );
			Vector tslist = ensemble.getTimeSeriesVector ();
			// Loop through and find a matching time series...
			int size = 0;
			boolean found = false;
			TS ts2 = null;
			ts = null;		// Value if not found.
			if ( tslist != null ) {
				size = tslist.size();
				for ( int i = 0; i < size; i++ ) {
					ts2 = (TS)tslist.elementAt(i);
					// This compares the sequence number
					// but does not include the input
					// type/name since that was already used
					// to read the file...
					if ( tsident.matches( ts2.getIdentifier().toString())){
						found = true;
						break;
					}
				}
			}
			if ( found ) {
				ts = ts2;
			}
		}
		catch ( Exception te ) {
			Message.printWarning ( 2, routine, "Error reading \"" +	tsident_string2 +
			"\" from ESP trace ensemble binary file." );
			Message.printWarning ( 2, routine, te );
			ts = null;
		}
	}
	else if ((input_type != null) &&
		input_type.equalsIgnoreCase("NWSRFS_FS5Files") ) {
		NWSRFS_DMI nwsrfs_dmi = getNWSRFSFS5FilesDMI ( input_name_full, true );
		ts = nwsrfs_dmi.readTimeSeries ( tsident_string, query_date1, query_date2, units, true );
	}
	else if ((input_type != null) && input_type.equalsIgnoreCase("RiversideDB") ) {
		// New style TSID~input_type~input_name for RiversideDB...
		if ( __rdmi == null ) {
			Message.printWarning ( 2, routine, "RiversideDB connection has not been opened." );
			ts = null;
		}
		else {	try {	ts = __rdmi.readTimeSeries ( tsident_string2, query_date1, query_date2, units, true );
			}
			catch ( Exception te ) {
				Message.printWarning ( 2, routine,
				"Error reading time series \""+tsident_string2 + "\" from RiversideDB" );
				Message.printWarning ( 2, routine, te );
				ts = null;
			}
		}
		// Because there are other issues to resolve, for now, if the
		// time series does not have data, set it to null...
		if ( ts != null ) {
			if ( !ts.hasData() ) {
				Message.printWarning ( 2, routine,
				"Time series \"" + tsident_string2 + "\" does not have data.  Treating as null." );
				ts = null;
			}
		}
	}
	else if ((input_type != null) && input_type.equalsIgnoreCase("RiverWare") ) {
		// New style TSID~input_type~input_name for RiverWare...
		try {	ts = RiverWareTS.readTimeSeries ( tsident_string2,
				input_name_full, query_date1, query_date2, units,
				true );
		}
		catch ( Exception te ) {
			ts = null;
		}
	}
	else if ((input_type != null) && input_type.equalsIgnoreCase("StateCU") ) {
		// New style TSID~input_type~input_name for StateCU...
		try {
            Vector ipy_types = StateCU_IrrigationPracticeTS.getTimeSeriesDataTypes(true, false);
            boolean is_ipy_type = false;
            int ipy_types_size = ipy_types.size();
            for ( int i_ipy = 0; i_ipy < ipy_types_size; i_ipy++ ) {
                if ( StringUtil.startsWithIgnoreCase(tsident.getType(), (String)ipy_types.elementAt(i_ipy))) {
                    is_ipy_type = true;
                    break;
                }
            }
            if ( is_ipy_type ) {
				ts = StateCU_IrrigationPracticeTS.readTimeSeries (
					tsident_string2, input_name_full,
					query_date1, query_date2, units, true );
			}
			else if(StringUtil.startsWithIgnoreCase(
				tsident.getType(), "CropArea-") &&
				!StringUtil.startsWithIgnoreCase(
				tsident.getType(), "CropArea-AllCrops") ) {
				// The second is in the StateCU_TS code.
				ts = StateCU_CropPatternTS.readTimeSeries (
					tsident_string2, input_name_full,
					query_date1, query_date2, units, true );
			}
			else {
                // Funnel through one class - the following will
				// read StateCU output report and frost dates
				// input files...
				ts = StateCU_TS.readTimeSeries (
					tsident_string2, input_name_full,
					query_date1, query_date2, units, true );
			}
		}
		catch ( Exception te ) {
			Message.printWarning ( 2, routine, "Error reading \"" + tsident_string2 + "\" from StateCU file." );
			Message.printWarning ( 2, routine, te );
			ts = null;
		}
	}
	else if ((input_type != null) && input_type.equalsIgnoreCase("StateMod") ) {
		// New style TSID~input_type~input_name for StateMod...
		try {	ts = StateMod_TS.readTimeSeries ( tsident_string2,
				input_name_full, query_date1, query_date2, units,
				true );
		}
		catch ( Exception te ) {
			Message.printWarning ( 2, routine, "Error reading \"" + tsident_string2 + "\" from StateMod file." );
			Message.printWarning ( 2, routine, te );
			ts = null;
		}
	}
	else if ((input_type != null) && input_type.equalsIgnoreCase("StateModB") ) {
		// New style TSID~input_type~input_name for StateModB...
		try {	ts = StateMod_BTS.readTimeSeries ( tsident_string2,
				input_name_full, query_date1, query_date2, units,
				true );
		}
		catch ( Exception te ) {
			Message.printWarning ( 2, routine, "Error reading \"" +
			tsident_string2 + "\" from StateMod binary file." );
			Message.printWarning ( 2, routine, te );
			ts = null;
		}
	}
	else if ((input_type != null) &&
		input_type.equalsIgnoreCase("USGSNWIS") ) {
		// New style TSID~input_type
		try {	ts = UsgsNwisTS.readTimeSeries ( tsident_string2,
				input_name, query_date1, query_date2, units,
				true );
		}
		catch ( Exception te ) {
			ts = null;
		}
	}
	return ts;
}

/**
Method for TSSupplier interface.
Read a time series given a time series identifier string.  The string may be
a file name if the time series are stored in files, or may be a true identifier
string if the time series is stored in a database.  The specified period is
read.  The data are converted to the requested units.
@param tsident_string Time series identifier or file name to read.
@param req_date1 First date to query.  If specified as null the entire period
will be read.
@param req_date2 Last date to query.  If specified as null the entire period
will be read.
@param req_units Requested units to return data.  If specified as null or an
empty string the units will not be converted.
@param read_data if true, the data will be read.  If false, only the time series
header will be read.
@return Time series of appropriate type (e.g., MonthTS, HourTS).
@exception Exception if an error occurs during the read.
*/
public TS readTimeSeries (	String tsident_string,
				DateTime req_date1, DateTime req_date2,
				String req_units,
				boolean read_data )
throws Exception
{	String routine = "TSEngine.readTimeSeries";
	Message.printStatus ( 1, routine,"Reading \"" + tsident_string + "\"" );

	// First look for time series in the in-memory list.  If called with
	// an alias, a match will be found in the aliases.  If called with
	// a TSID, a match will be found in the TSIDs.

	int size = 0;
	if ( __tslist != null ) {
		size = __tslist.size();
	}

	// If the tsident_string contains a ~, then use the full identifier with
	// input fields to search below.  Otherwise, just use the main part of
	// the identifier...

	boolean full_tsid_check = false;
	if ( tsident_string.indexOf("~") >= 0 ) {
		full_tsid_check = true;
	}

	// If the tsident_string contains a [, then assume that the TSID is
	// actually a template TSID and a wildcard array is being used.  This
	// "trick" is used to avoid requiring another argument to the TSSupplier
	// interface, although an argument may be added at some point.

	// FIXME SAM 2007-06-21 This is a problem because TSIDs allow sequence numbers
	// for ensembles.
	// TSIDs with sequence number have a [] at the end so do a
	// fix - if 3 "." are before the [], the assume the [] is for
	// ensembles.
	boolean is_template = false;
	int pos = tsident_string.indexOf("[");
	int period_count = 0;
	for ( int i = 0; i < pos; i++ ) {
		if ( tsident_string.charAt(i) == '.' ) {
			++period_count;
		}
	}
	int array_index = -1;	// Indicates an array index of * (implement
				// later).
	if ( (pos >= 0) && (period_count <3) ) {
		is_template = true;
		Message.printStatus ( 2, routine, "Requested TSID \"" + tsident_string +
				" is a template.");
		// Figure out the array position...
		String array_index_string = StringUtil.getToken (
			tsident_string.substring(pos + 1), "]", 0, 0 );
		if ( array_index_string.equals("*") ) {
			array_index = -1;
		}
		else if ( StringUtil.isInteger(array_index_string) ) {
			array_index = StringUtil.atoi(array_index_string);
		}
		else {	// Invalid...
			Message.printWarning ( 2, routine,
			"TSID \"" + tsident_string + "\" array index is invalid" );
			throw new Exception ( "TSID \"" + tsident_string +
			"\" array index is invalid" );
		}
		// Strip off the array information because it will confuse the
		// following code...
		tsident_string = tsident_string.substring(0,pos);
	}

	if ( size != 0 ) {
		TS ts = null;
		TSIdent tsident = null;

		//  First try the aliases (not supported for templates)...

		if ( !is_template ) {
			for ( int i = 0; i < size; i++ ) {
				ts = (TS)__tslist.elementAt(i);
				if ( ts == null ) {
					continue;
				}
				if (	ts.getAlias().equalsIgnoreCase(
					tsident_string) ) {
					Message.printStatus ( 2, routine,
					"Matched alias." );
					return ts;
				}
			}
		}

		// Now try the TSIDs, including the input fields if necessary...

		int match_count = 0;
		for ( int i = 0; i < size; i++ ) {
			ts = (TS)__tslist.elementAt(i);
			if ( ts == null ) {
				continue;
			}
			tsident = ts.getIdentifier();
			if ( Message.isDebugOn ) {
				Message.printDebug ( 2, routine, 
						"Checking tsid \"" + tsident.toString(true) + "\"" );
			}
			if ( is_template ) {
				if ( tsident.matches(tsident_string, true, full_tsid_check)){
					Message.printStatus ( 2, routine,
					"Matched TSID for template." );
					// See if this is the one we want -
					// both are zero initial value...
					if (	(array_index < 0) ||
						(match_count == array_index) ) {
						// TODO - need way to track
						// * matches to not return the
						// same TS each match
						return ts;
					}
					// Else increment...
					++match_count;
				}
			}
			else {	// Check the identifier strings using full identifiers.
				// The TSID being requested controls the level of comparison.
				// If it has the input fields, then they will be checked.
				if ( tsident.equals(tsident_string,full_tsid_check) ) {
					Message.printStatus ( 1, routine,"Matched TSID using TSID with input fields." );
					return ts;
				}
			}
		}
	}

	if ( is_template ) {
		// If a matching time series was not found, there has been
		// an error because there is no way to easily read a list of
		// time series with wildcards and get the list back in the order
		// that is expected by the template.  The calling code should
		// handle the exception and use a null time series if needed.
		Message.printWarning ( 2, routine,
		"TSID \"" + tsident_string + "\" could not be matched." );
		throw new Exception ( "TSID \"" + tsident_string +
			"\" could not be matched." );
	}
	else {	Message.printStatus ( 2, routine,
			"TSID \"" + tsident_string + "\" could not be matched in memory." +
					"  Trying to read using TSID." );
	}

	// If not found, try reading from a persistent source.  If called with
	// an alias, this will fail.  If called with a TSID, this should
	// succeed...

	TS ts = readTimeSeries0 ( tsident_string, req_date1, req_date2,
				req_units, read_data );
	if ( ts == null ) {
		Message.printStatus ( 2, routine,
				"TSID \"" + tsident_string +
				"\" could not read using TSID.  Not able to provide." );
	}
	else {
		Message.printStatus ( 2, routine,
				"TSID \"" + tsident_string + "\" successfully read using TSID." );
	}
	return ts;
}

/**
Method for TSSupplier interface.
Read a time series given an existing time series and a file name.
The specified period is read.
The data are converted to the requested units.
@param req_ts Requested time series to fill.  If null, return a new time series.
If not null, all data are reset, except for the identifier, which is assumed
to have been set in the calling code.  This can be used to query a single
time series from a file that contains multiple time series.
@param fname File name to read.
@param date1 First date to query.  If specified as null the entire period will
be read.
@param date2 Last date to query.  If specified as null the entire period will
be read.
@param req_units Requested units to return data.  If specified as null or an
empty string the units will not be converted.
@param read_data if true, the data will be read.  If false, only the time series
header will be read.
@return Time series of appropriate type (e.g., MonthTS, HourTS).
@exception Exception if an error occurs during the read.
*/
public TS readTimeSeries (	TS req_ts, String fname,
				DateTime date1, DateTime date2,
				String req_units,
				boolean read_data )
throws Exception
{	return null;
}

/**
Method for TSSupplier interface.
Read a time series list from a file (this is typically used used where a time
series file can contain one or more time series).
The specified period is
read.  The data are converted to the requested units.
@param fname File to read.
@param date1 First date to query.  If specified as null the entire period will
be read.
@param date2 Last date to query.  If specified as null the entire period will
be read.
@param req_units Requested units to return data.  If specified as null or an
empty string the units will not be converted.
@param read_data if true, the data will be read.  If false, only the time series
header will be read.
@return Vector of time series of appropriate type (e.g., MonthTS, HourTS).
@exception Exception if an error occurs during the read.
*/
public Vector readTimeSeriesList (	String fname,
					DateTime date1, DateTime date2,
					String req_units,
					boolean read_data )
throws Exception
{	return null;
}

/**
Method for TSSupplier interface.
Read a time series list from a file or database using the time series identifier
information as a query pattern.
The specified period is
read.  The data are converted to the requested units.
@param tsident A TSIdent instance that indicates which time series to query.
If the identifier parts are empty, they will be ignored in the selection.  If
set to "*", then any time series identifier matching the field will be selected.
If set to a literal string, the identifier field must match exactly to be
selected.
@param fname File to read.
@param date1 First date to query.  If specified as null the entire period will
be read.
@param date2 Last date to query.  If specified as null the entire period will
be read.
@param req_units Requested units to return data.  If specified as null or an
empty string the units will not be converted.
@param read_data if true, the data will be read.  If false, only the time series
header will be read.
@return Vector of time series of appropriate type (e.g., MonthTS, HourTS).
@exception Exception if an error occurs during the read.
*/
public Vector readTimeSeriesList (	TSIdent tsident, String fname,
					DateTime date1, DateTime date2,
					String req_units,
					boolean read_data )
throws Exception {
	return null;
}

/**
Perform common actions on time series after reading.  This method should
be called after:
<ol>
<li>	calling readTimeSeries()</li>
<li>	bulk reads are done indepent of readTimeSeries() (e.g., when calling
	readDateValue() directly to read multiple time series</li>
<li>	single reads are done independent of readTimeSeries() (e.g., when
	calling readUsgsNwis() directly to read a single time series</li>
</ol>
This method does the following:
<ol>
<li>	Sets the legend to "" (this unsets legend information that was
	previously required with the old graph package).</li>
<li>	If the description is not set, sets it to the location.</li>
<li>	If a missing data range has been set, indicate it to the time series.
	This may be phased out.</li>
<li>	If the time series identifier needs to be reset to something known to
	the read code, reset it (using the non-null tsident_string parameter
	that is passed in).</li>
<li>	Compute the historic averages for the raw data so that it is available
	later for filling.</li>
<li>	If the output period is specified, make sure that the time series
	period includes the output period.  For important time series, the
	available period may already include the output period.  For time series
	that are being filled, it is likely that the available period will need
	to be extended to include the output period.</li>
</ol>
@param ts Time series to process.
@param tsident_string Time series identifier string.  If null, take from the
time series.
@param full_period If true, indicates that the full period is to be queried.
If false, the output period will be queried.
@exception Exception if there is an error processing the time series.
*/
private void readTimeSeries2 (	TS ts, String tsident_string,
				boolean full_period )
throws Exception
{	String routine = "TSEngine.readTimeSeries2";
	if ( ts == null ) {
		return;
	}

	// Do not want to use the abbreviated legend that was required
	// with the old graph package...

	ts.setLegend ( "" );

	// If no description, set to the location...

	if ( ts.getDescription().length() == 0 ) {
		ts.setDescription ( ts.getLocation() );
	}

	// If a missing data range has been set, indicate this to the time
	// series now so that it can be used for filling, etc...
	// TODO SAM 2005-09-02
	// This is really a hold-over and might need to be phased out...

	if ( __missing_range != null ) {
		if ( __missing_range.length == 2 ) {
			ts.setMissingRange ( __missing_range );
		}
		else {
            ts.setMissing ( __missing_range[0] );
		}
	}

	// Compute the historic average here rather than having to put this code
	// in each clause in the processTimeSeriesCommands() method.  Currently
	// needHistoricalAverages() always returns true...

	if ( needHistoricalAverages(ts) ) {
		try {	ts.setDataLimitsOriginal (calculateTSAverageLimits(ts));
		}
		catch ( Exception e ) {
			Message.printWarning ( 2, routine,
			"Error getting original data limits for \"" +
			ts.getIdentifierString() + "\""  );
			Message.printWarning ( 3, routine, e );
		}
	}

	// To ensure that new and old time series identifiers can be used, reset
	// the identifier in the queried string to that which was specified in
	// the input commands.

	if ( tsident_string != null ) {
		ts.setIdentifier ( tsident_string );
	}
	//Message.printStatus ( 1,"","SAMX TSEngine.readTimeSeries2 TS id = \""+
		//ts.getIdentifier().toString(true) + "\"" );

	// If the output period has been specified, make sure that the time
	// series has a period at least that long.  This will allow for data	
	// filling and other manipulation.  Do not change the interval if the
	// time series has irregular data or if the auto extend feature has
	// been turned off.
	//
	// Check by getting the maximum overlapping period of the time series
	// and output period.  Then if the max period start is before the
	// time series start or the max period end is after the time series end
	// a change interval is needed...

	if ( haveOutputPeriod() && getAutoExtendPeriod() ) {
		Vector v = new Vector ( 2 );
		TSLimits limits = new TSLimits();
		limits.setDate1 ( ts.getDate1() );
		limits.setDate2 ( ts.getDate2() );
		v.addElement ( limits );
		limits = new TSLimits();
		limits.setDate1 ( __OutputStart_DateTime );
		limits.setDate2 ( __OutputEnd_DateTime );
		v.addElement ( limits );
		try {	limits = TSUtil.getPeriodFromLimits( v, TSUtil.MAX_POR);
			if (	limits.getDate1().lessThan(ts.getDate1()) ||
				limits.getDate2().greaterThan(ts.getDate2()) ) {
				ts.changePeriodOfRecord ( limits.getDate1(),
					limits.getDate2() );
			}
		}
		catch ( Exception e ) {
			String message = "Unable to extend period for \"" +
			ts.getIdentifierString() + "\" to output period.";
			Message.printWarning ( 2, routine, message );
			throw new Exception ( message );
		}
	}
}

/**
Call readTimeSeries2() for every time series in the Vector, with the
full_period parameter having a value of true.  This version is called by
read commands.
@param tslist Vector of TS to process.
@exception Exception if there is an error processing the time series.
*/
protected void readTimeSeries2 ( Vector tslist )
throws Exception
{	readTimeSeries2 ( tslist, true );
}

/**
Call readTimeSeries2() for every time series in the Vector.
@param tslist Vector of TS to process.
@param full_period If true, indicates that the full period is to be queried.
If false, the output period will be queried.
@exception Exception if there is an error processing the time series.
*/
private void readTimeSeries2 ( Vector tslist, boolean full_period )
throws Exception
{	int size = 0;
	if ( tslist != null ) {
		size = tslist.size();
	}
	TS ts = null;
	for ( int i = 0; i < size; i++ ) {
		ts = (TS)tslist.elementAt(i);
		if ( ts == null ) {
			continue;
		}
		readTimeSeries2 ( ts, null, full_period );
	}
	ts = null;
}

/**
Remove all time series in the results list.
*/
protected void removeAllTimeSeries ()
{
    __tslist.removeAllElements();
}

/**
Remove the time series at the specified index.
@param index Index of the time series to remove.
*/
protected void removeTimeSeries ( int index )
{
    __tslist.removeElementAt ( index );
}

/**
Run the interpreter, parsing each line and then executing it.  This allows
more flexibility but is currently somewhat specific.  Phase in all the old
functionality as time allows.
THIS CODE IS NOT CURRENTLY FUNCTIONAL.
*/
/* TODO SAM 2007-02-08
Need to decide whether this should be supported.
private void runInterpreter ( Vector commands )
{	if ( commands == null ) {
		return;
	}
	String command = null;
	int size = commands.size();
	for ( int i = 0; i < size; i++ ) {
		command = (String)commands.elementAt(i);
		if ( command == null ) {
			continue;
		}
		command = command.trim();
		if ( command.length() == 0 ) {
			continue;
		}
		if ( command.regionMatches(true,0,"runInterpreter",0,14)) {
			// We already know this because that command triggered
			// a call to this method...
			continue;
		}
	}
}
*/

/**
Search for a fill pattern TS.
@return reference to found StringMonthTS instance.
@param fill_pattern Fill pattern identifier to search for.
*/
private StringMonthTS searchForFillPatternTS ( String fill_pattern )
{	if ( fill_pattern == null ) {
		return null;
	}
	if ( __fill_pattern_ts == null ) {
		return null;
	}
	
	int nfill_pattern_ts = __fill_pattern_ts.size();

	StringMonthTS fill_pattern_ts_i = null;
	for ( int i = 0; i < nfill_pattern_ts; i++ ) {
		fill_pattern_ts_i =(StringMonthTS)__fill_pattern_ts.elementAt(i);
		if ( fill_pattern_ts_i == null ) {
			continue;
		}
		if ( fill_pattern.equalsIgnoreCase(	fill_pattern_ts_i.getLocation()) ) {
			return fill_pattern_ts_i;
		}
	}
	fill_pattern_ts_i = null;
	return null;
}

/**
Set the value of the AutoExtendPeriod property.
@param AutoExtendPeriod_boolean Value of property.
*/
private void setAutoExtendPeriod ( boolean AutoExtendPeriod_boolean )
{
    __AutoExtendPeriod_boolean = AutoExtendPeriod_boolean;
}

/**
Set the average period end.
@param end Average period end.
*/
protected void setAverageEnd ( DateTime end )
{   __AverageEnd_DateTime = end;
}

/**
Set the average period start.
@param start Average period start.
*/
protected void setAverageStart ( DateTime start )
{   __AverageStart_DateTime = start;
}

/**
Set the binary time series used with the processor.
@param BinaryTSUsed_boolean true if a BinaryTS is used, false if not.
*/
protected void setBinaryTS ( BinaryTS binary_ts )
{   __binary_ts = binary_ts;
}

/**
Set whether a binary time series is used for temporary data.
@param BinaryTSUsed_boolean true if a BinaryTS is used, false if not.
*/
protected void setBinaryTSUsed ( boolean BinaryTSUsed_boolean )
{   __BinaryTSUsed_boolean = BinaryTSUsed_boolean;
}

/**
Set the list of DataTest, for example, when set with TSCommandsProcessor.
@param DataTest_Vector List of data test definitions, as Vector of DataTest.
*/
protected void setDataTestList ( Vector DataTest_Vector )
{	
	__datatestlist = (Vector)DataTest_Vector;
}

/**
Set a HydroBaseDMI instance in the Vector that is being maintained for use.
The input name in the DMI is used to lookup the instance.  If a match is found,
the old instance is optionally closed and the new instance is set in the same
location.  If a match is not found, the new instance is added at the end.
@param hbdmi HydroBaseDMI to add to the list.  Null will be ignored.
@param close_old If an old DMI instance is matched, close the DMI instance if
true.  The main issue is that if something else is using the DMI instance (e.g.,
the TSTool GUI) it may be necessary to leave the old instance open.
*/
protected void setHydroBaseDMI ( HydroBaseDMI hbdmi, boolean close_old )
{	if ( hbdmi == null ) {
		return;
	}
	int size = __hbdmi_Vector.size();
	HydroBaseDMI hbdmi2 = null;
	String input_name = hbdmi.getInputName();
	for ( int i = 0; i < size; i++ ) {
		hbdmi2 = (HydroBaseDMI)__hbdmi_Vector.elementAt(i);
		if ( hbdmi2.getInputName().equalsIgnoreCase(input_name)){
			// The input name of the current instance
			// matches that of the instance in the Vector.
			// Replace the instance in the Vector by the
			// new instance...
			if ( close_old ) {
				try {	hbdmi2.close();
				}
				catch ( Exception e ) {
					// Probably can ignore.
				}
			}
			__hbdmi_Vector.setElementAt ( hbdmi, i );
			return;
		}
	}
	// Add a new instance to the Vector...
	__hbdmi_Vector.addElement ( hbdmi );
}

/**
Set the list of HydroBaseDMI (e.g., when manipulated by an openHydroBase() command.
@param dmilist Vector of HydroBaseDMI.
*/
protected void setHydroBaseDMIList ( Vector dmilist )
{	__hbdmi_Vector = dmilist;
}

/**
Set the value of the IgnoreLEZero property.
@param IgnoreLEZero_boolean Value of property.
*/
private void setIgnoreLEZero ( boolean IgnoreLEZero_boolean )
{
    __IgnoreLEZero_boolean = IgnoreLEZero_boolean;
}

/**
Set the value of the IncludeMissingTS property.
@param IncludeMissingTS_boolean Value of property.
*/
private void setIncludeMissingTS ( boolean IncludeMissingTS_boolean )
{
    __IncludeMissingTS_boolean = IncludeMissingTS_boolean;
}

/**
Set the input period end.
@param end Input period end.
*/
protected void setInputEnd ( DateTime end )
{	__InputEnd_DateTime = end;
}

/**
Set the input period start.
@param start Input period start.
*/
protected void setInputStart ( DateTime start )
{	__InputStart_DateTime = start;
}

/**
Set an NDFD Adapter instance in the Vector that is being maintained for use.
The input name for the adapter is used to lookup the instance.  If a match is
found, the old instance is optionally closed and the new instance is set in the
same location.  If a match is not found, the new instance is added at the end.
@param adapter NDFD Adapter to add to the list.  Null will be ignored.
@param close_old If an old adapter is matched, remove the instance if true.
This is mainly for memory management since NDFD Adapters do not currently keep
a connection open.
*/

//TODO KAT 2006-10-30
//Commenting this out for now to get it to compile
//Danny will be adding service stuff in the future
//which should replace this ...
/**
private void setNDFDAdapter ( Adapter adapter, boolean close_old )
{	if ( adapter == null ) {
		return;
	}
	int size = __NDFDAdapter_Vector.size();
	Adapter adapter2 = null;
	//TODO SAM 2006-07-15
	NDFD Adapters do not have names/identfiers so put in the first slot
	String input_name = adapter.getIdentifier();
	for ( int i = 0; i < size; i++ ) {
		adapter2 = (Adapter)__NDFDAdapter_Vector.elementAt(i);
		if ( adapter2.getIdentifier().equalsIgnoreCase(input_name)){
			// The identifier of the current instance
			// matches that of the instance in the Vector.
			// Replace the instance in the Vector by the
			// new instance...
			if ( close_old ) {
				/ * TODO SAM 2006-07-13
				Probably not needed - evaluate for later
				try {	hbdmi2.close();
				}
				catch ( Exception e ) {
					// Probably can ignore.
				}
				* /
			}
			__NDFDAdapter_Vector.setElementAt ( adapter, i );
			return;
		}
	}
	// Add a new instance to the Vector...
	__NDFDAdapter_Vector.addElement ( adapter );
	
	if ( __NDFDAdapter_Vector.size() == 0 ) {
		// Add as the first item...
		__NDFDAdapter_Vector.addElement ( adapter );
	}
	else {	__NDFDAdapter_Vector.setElementAt ( adapter, 0 );
	}
}

*/

/**
Set a NWSRFS_DMI (NWSRFS FS5Files DMI) instance in the Vector that is being maintained for use.
The input name in the DMI is used to lookup the instance.  If a match is found,
the old instance is optionally closed and the new instance is set in the same
location.  If a match is not found, the new instance is added at the end.
@param nwsrfs_dmi NWSRFS_DMI to add to the list.  Null will be ignored.
@param close_old If an old DMI instance is matched, close the DMI instance if
true.  The main issue is that if something else is using the DMI instance (e.g.,
the TSTool GUI) it may be necessary to leave the old instance open.
*/
protected void setNWSRFSFS5FilesDMI ( NWSRFS_DMI nwsrfs_dmi, boolean close_old )
{	if ( nwsrfs_dmi == null ) {
		return;
	}
	int size = __nwsrfs_dmi_Vector.size();
	NWSRFS_DMI nwsrfs_dmi2 = null;
	String input_name = nwsrfs_dmi.getInputName();
	for ( int i = 0; i < size; i++ ) {
		nwsrfs_dmi2 = (NWSRFS_DMI)__nwsrfs_dmi_Vector.elementAt(i);
		if ( nwsrfs_dmi2.getInputName().equalsIgnoreCase(input_name)){
			// The input name of the current instance
			// matches that of the instance in the Vector.
			// Replace the instance in the Vector by the
			// new instance...
			if ( close_old ) {
				try {	nwsrfs_dmi2.close();
				}
				catch ( Exception e ) {
					// Probably can ignore.
				}
			}
			__nwsrfs_dmi_Vector.setElementAt ( nwsrfs_dmi, i );
			return;
		}
	}
	// Add a new instance to the Vector...
	__nwsrfs_dmi_Vector.addElement ( nwsrfs_dmi );
}


/**
Set the value of the OutputDetailedHeader property.
@param OutputDetailedHeader_boolean Value of property.
*/
private void setOutputDetailedHeader ( boolean OutputDetailedHeader_boolean )
{
    __OutputDetailedHeader_boolean = OutputDetailedHeader_boolean;
}

/**
Set the output period end.
@param end Output period end.
*/
protected void setOutputEnd ( DateTime end )
{	__OutputEnd_DateTime = end;
}

/**
Set the output period start.
@param start Output period start.
*/
protected void setOutputStart ( DateTime start )
{	__OutputStart_DateTime = start;
}

// TODO SAM 2007-11-16 Convert to enumeration when moved to Java 1.5.
/**
Set the output year type.
@param OutputYearType_int Output year type as an integer.
*/
protected void setOutputYearType ( int OutputYearType_int )
{   __OutputYearType_int = OutputYearType_int;
}

/**
Set the value of the PreviewExportedOutput property.
@param PreviewExportedOutput_boolean Value of property.
*/
private void setPreviewExportedOutput ( boolean PreviewExportedOutput_boolean )
{
    __PreviewExportedOutput_boolean = PreviewExportedOutput_boolean;
}

/**
Set the time series in either the __tslist vector or the BinaryTS file,
as appropriate.  If a BinaryTS is set, it updates the time series that is in
the file, within the constraints of the BinaryTS file (e.g., the period cannot
be changed from the BinaryTS file).  This method does not currently change
from in-memory to BinaryTS file if the number of time series exceeds the
in-memory limits.
@param id Identifier for time series (alias or TSIdent string).
@exception Exception if there is an error saving the time series.
*/
protected void setTimeSeries ( String id, TS ts )
throws Exception
{	int position = indexOf ( id );
	setTimeSeries ( ts, position );
}

/**
Set the time series in either the __tslist vector or the BinaryTS file,
as appropriate.  If a BinaryTS is set, it updates the time series that is in
the file, within the constraints of the BinaryTS file (e.g., the period cannot
be changed from the BinaryTS file).  This method does not currently change
from in-memory to BinaryTS file if the number of time series exceeds the
in-memory limits.
@param ts time series to set.
@param position Position in time series list (0 index).
@exception Exception if there is an error saving the time series.
*/
protected void setTimeSeries ( TS ts, int position )
throws Exception
{	String routine = "TSEngine.setTimeSeries";

	if ( ts == null ) {
		Message.printStatus ( 1, routine,
		"Setting null time series at position " + (position + 1) );
	}
	else {	Message.printStatus ( 1, routine,
		"Setting time series \"" + ts.getIdentifierString() +
		"\" at position " + (position + 1) );
	}
	if ( position < 0 ) {
		return;
	}

	// Check to see if the time series should be saved to a BinaryTS file
	// and open the BinaryTS file if necessary...

	if (	(((ts.getDataIntervalBase() == TimeInterval.MONTH) &&
		(__num_TS_expressions > __binary_month_cutoff)) || 
		((ts.getDataIntervalBase() == TimeInterval.DAY) &&
		(__num_TS_expressions > __binary_day_cutoff))) &&
		(getTimeSeriesSize() == 0) ) {
		//
		// Need to use the binary TS file...
		//
		setBinaryTSUsed ( true );
		// First time series so open the file...
		try {	// Remove it first...
			if ( IOUtil.fileReadable(__binary_ts_file) ) {
				File bts = new File( __binary_ts_file );
				bts.delete();
			}
			// Now create.  Make sure to set the dates to the right
			// precision...
			if ( (__binaryts_date1 == null) || (__binaryts_date2 == null) ) {
				Message.printWarning ( 1, routine,
				"The BinaryTS period must be specified when using a BinaryTS temporary file." );
				return;
			}
			DateTime date1 = new DateTime (__binaryts_date1);
			DateTime date2 = new DateTime (__binaryts_date2);
			if ( ts.getDataIntervalBase() == TimeInterval.DAY ) {
				// Reset the precision on the dates...
				date1.setPrecision ( DateTime.PRECISION_DAY);
				date1.setDay(1);
				date2.setPrecision ( DateTime.PRECISION_DAY);
				date2.setDay( TimeUtil.numDaysInMonth(
					date2.getMonth(), date2.getYear()));
			}
			Message.printStatus ( 1, routine,
				"Creating BinaryTS file \"" + __binary_ts_file +
				"\" to hold " + __num_TS_expressions +
				" time series for " + date1.toString() + " to "+
				date2.toString() );
			__binary_ts = new BinaryTS (
					__binary_ts_file, __num_TS_expressions,
					ts.getDataIntervalBase(),
					ts.getDataIntervalMult(),
					date1, date2, "rw", true );
		}
		catch ( Exception e ) {
			String message =
				"Unable to open temporary BinaryTS file \"" + __binary_ts_file + "\"";
				Message.printWarning ( 2, routine, message );
				Message.printWarning ( 2, routine, e );
				throw new Exception ( message );
		}
	}
	else if ( getTimeSeriesSize() == 0 ) {
		Message.printStatus ( 1, routine, "Time series will be held " +
		"in memory for output (estimating " + __num_TS_expressions +
		" time series)." );
	}

	if ( getBinaryTSUsed() ) {
		if ( getBinaryTS() != null ) {
			__binary_ts.writeTimeSeries ( ts, position );
		}
	}
	else {	if ( __tslist == null ) {
			// Create a new Vector.
			__tslist = new Vector ( 50, 50 );
		}
		// Position is zero index...
		if ( position >= __tslist.size() ) {
			// Append to the list.  Fill in intervening positions
			// with null references...
			for ( int i = __tslist.size(); i <= position; i++ ) {
				__tslist.addElement ( (TS)null );
			}
		}
		// Now update at the requested position...
		__tslist.removeElementAt ( position );
		__tslist.insertElementAt ( ts, position );
	}
}

/**
Set the time series list, for example, when being processed through
TSCommandsProcessor.
@param tslist List of time series results, as Vector of TS.
*/
protected void setTimeSeriesList ( Vector tslist )
{	__tslist = tslist;
}

/**
Set the contents of a daily time series using a monthly time series for the
total and a daily time series for the pattern.
*/
protected void setUsingMonthAndDay ( DayTS ts, MonthTS monthts, DayTS dayts )
{	if ( (ts == null) || (monthts == null) || (dayts == null) ) {
		return;
	}
	if (	!((ts.getDataIntervalBase() == TimeInterval.DAY) &&
		(ts.getDataIntervalMult() == 1)) ) {
		return;
	}
	if (	!((dayts.getDataIntervalBase() == TimeInterval.DAY) &&
		(dayts.getDataIntervalMult() == 1)) ) {
		return;
	}
	if (	!((monthts.getDataIntervalBase() == TimeInterval.MONTH) &&
		(monthts.getDataIntervalMult() == 1)) ) {
		return;
	}

	// Loop through the time series to set...

	DateTime daydate = new DateTime ( ts.getDate1() );
	// Make sure the day is 1...
	daydate.setDay(1);
	DateTime monthdate = new DateTime ( daydate );
	monthdate.setPrecision ( DateTime.PRECISION_MONTH );
	DateTime monthend = new DateTime ( ts.getDate2() );
	monthend.setPrecision ( DateTime.PRECISION_MONTH );
	double dayvalue = 0.0;
	double daytotal = 0.0;
	double monthvalue = 0.0;
	int num_days_in_month = 0;
	int i = 0;
    boolean found_missing_day = false;
    
	// Loop on the months for the time series being filled...
	for (	;
		monthdate.lessThanOrEqualTo ( monthend );
		monthdate.addInterval(TimeInterval.MONTH, 1) ) {
		// Get the monthly value...
		monthvalue = monthts.getDataValue(monthdate);
		if ( monthts.isDataMissing(monthvalue) ) {
			// Don't do anything for the month...
			continue;
		}
		num_days_in_month = TimeUtil.numDaysInMonth(
				monthdate.getMonth(),
				monthdate.getYear() );
		// Set the starting date for the daily date to the first of the
		// month...
		daydate.setYear(monthdate.getYear());
		daydate.setMonth(monthdate.getMonth());
		daydate.setDay(1);
		// Get the total of the values in daily time series being used
		// for the distribution...
		daytotal = 0.0;
        
        // reset the found missing data flag
        found_missing_day = false;
        
		for (	i = 1; i <= num_days_in_month;
			i++, daydate.addInterval(TimeInterval.DAY,1) ) {
			dayvalue = dayts.getDataValue ( daydate );
           
            if ( dayts.isDataMissing( dayvalue )) {    
               found_missing_day = true;
               break;
            }
            daytotal += dayvalue;
		}
        
        // If data is missing for the day, skip to next month
        if ( found_missing_day ) {
           continue;
        }
//		Message.printStatus ( 1, "", "Day total for " +
//			monthdate.toString() + " is " + daytotal );
		// Now loop through again and fill in the time series to be
		// created by taking the monthly total value and multiplying it
		// by the ratio of the specific day to the total of the days...
		if ( daytotal > 0.0 ) {
			daydate.setYear(monthdate.getYear());
			daydate.setMonth(monthdate.getMonth());
			daydate.setDay(1);
			for (	i = 1; i <= num_days_in_month;
				i++, daydate.addInterval(TimeInterval.DAY,1) ) {
				dayvalue = dayts.getDataValue ( daydate );
                  
				// For now hard-code the conversion factor
				// from ACFT to CFS...
				if ( !dayts.isDataMissing(dayvalue) ) {
					dayvalue =
					(monthvalue * (1 / 1.9835)) * (dayvalue/daytotal) ;
//		Message.printStatus ( 1, "", "Setting " +
//			daydate.toString() + " to " + daytotal );
					ts.setDataValue(daydate,dayvalue);
				}
			}
		}
	}
	ts.setDataUnits ( "CFS" );
}

/**
Update the comments in the time series in case some information has changed
(currently units and description only).  If this is not called, then looking at
a summary may show the wrong information.  This method is needed because the
comments are often used in the header and comments are generated from HydroBase.
Currently only the units are updated.  THIS METHOD IS HIGHLY
DEPENDENT ON THE SPECIFIC TIME SERIES COMMENTS USED WITH CDSS.
@param ts Time series to update.
*/
private void updateComments ( TS ts )
{	if ( ts == null ) {
        return;
    }
    Vector comments = ts.getComments();
	int size = 0;
	if ( comments != null ) {
		size = comments.size();
	}
	String comment = null;
	int pos = 0;
	for ( int i = 0; i < size; i++ ) {
		comment = (String)comments.elementAt(i);
		if ( comment.regionMatches(true,0,"Data units",0,10) ) {
			pos = comment.indexOf ( "=" );
			comments.setElementAt(
			comment.substring(0,pos + 2) + ts.getDataUnits(), i);
		}
		else if ( comment.regionMatches(true,0,"Description",0,11) ) {
			pos = comment.indexOf ( "=" );
			comments.setElementAt(
			comment.substring(0,pos + 2) + ts.getDescription(), i);
		}
	}
	comments = null;
	comment = null;
}

/**
Write a StateMod time series file given the current time series.  This can be
called using both the in-memory list of time series or the list to be output.
Other than the time series list and the filename, all other parameters are taken
from settings previously set.
@param tslist Vector of time series to write.  Currently this is ignored if
a BinaryTS has been created for daily data.
@param output_file Name of file to write.
@param precision_string If "*", then use the default rules that have been in
place for some time.  If an integer, use as the precision parameter for
StateMod.writePersistent().
@param comments Comments to include at the top of the StateMod file, consisting
of the commands as text and database version information.
*/
private void writeStateModTS (	Vector tslist, String output_file,
				String precision_string, String[] comments )
{	String routine = "TSEngine.writeStateModTS";
	// Type of calendar for output...
	String calendar = "";
	if ( getOutputYearTypeInt() == __WATER_YEAR ) {
		calendar = "WaterYear";
	}
	else {	calendar = "CalendarYear";
	}
	// Set the precision default precision for output (-2 generally works
	// OK)...
	int	precision = -2;
	if (	!precision_string.equals("*") && !precision_string.equals("") &&
		StringUtil.isInteger(precision_string) ) {
		// Use the specified precision because the user has specified
		// an integer precision...
		precision = StringUtil.atoi(precision_string);
	}
	else {	// Precision is determined from units and possibly data type...
		// Default, get the precision from the units of the first time
		// series...
		if ( getBinaryTSUsed() ) {
			try {
                String units = __binary_ts.getDataUnits(0);
				Message.printStatus ( 1, "", "Data units are " + units );
				DataFormat outputformat = DataUnits.getOutputFormat(units,10);
				if ( outputformat != null ) {
					precision = outputformat.getPrecision();
					if ( precision > 0 ) {
						// Change to negative so output
						// code will handle overflow...
						precision *= -1;
					}
				}
				outputformat = null;
				Message.printStatus ( 1, "", "Precision from output format is " + precision);
			}
			catch ( Exception e ) {
				// use default precision...
				precision = -2;
			}
		}
		else {	int	list_size = 0;
			TS	tspt = null;
			if ( (tslist != null) && (tslist.size() > 0) ) {
				tspt = (TS)tslist.elementAt(0);
				list_size = tslist.size();
			}
			if ( tspt != null ) {
				String units = tspt.getDataUnits();
				Message.printStatus ( 1, "",
				"Data units are " + units );
				DataFormat outputformat =
					DataUnits.getOutputFormat(units,10);
				if ( outputformat != null ) {
					precision = outputformat.getPrecision();
					if ( precision > 0 ) {
						// Change to negative so output
						// code will handle overflow...
						precision *= -1;
					}
				}
				outputformat = null;
				Message.printStatus ( 1, "",
				"Precision from units output format *-1 is " +
				precision);
			}
			// Old code that we still need to support...
			// In year 2, we changed the precision to 0 for RSTO.
			// See if any of the TS in the list are RSTO...
			for ( int ilist = 0; ilist < list_size; ilist++ ) {
				tspt = (TS)tslist.elementAt(ilist);
				if ( tspt == null ) {
					continue;
				}
				if (	tspt.getIdentifier().getType(
					).equalsIgnoreCase( "RSTO") ) {
					precision = 0;
					break;
				}
			}
		}
	}

	if ( getBinaryTSUsed() || TSUtil.intervalsMatch ( tslist )) {
		// The time series to write have the same interval so write
		// using the first interval....
		int interval = 0;
		if ( getBinaryTSUsed() ) {
			interval = __binary_ts.getDataIntervalBase();
		}
		else {	interval = ((TS)tslist.elementAt(0)).getDataIntervalBase();
		}
		if ( getBinaryTS() != null ) {
			/* TODO
			StateMod_TS.writeTimeSeriesList (
				(String)null, output_file, comments,
				_binary_ts, calendar, _missing,
				precision );
			*/
			Message.printWarning ( 1, routine,
				"Writing StateMod time series from a binary file is not currently supported." );
		}
		else if((interval == TimeInterval.DAY) ||(interval == TimeInterval.MONTH) ) {
			PropList smprops = new PropList ( "StateMod" );
			// Don't set input file since it is null...
			smprops.set ( "OutputFile", output_file );
			if ( comments != null ) {
				smprops.setUsingObject ( "NewComments",
				(Object)comments );
			}
			if ( __OutputStart_DateTime != null ) {
				smprops.set("OutputStart=" + __OutputStart_DateTime.toString());
			}
			if ( __OutputEnd_DateTime != null ) {
				smprops.set ( "OutputEnd=" + __OutputEnd_DateTime.toString());
			}
			smprops.set ( "CalendarType", calendar );
			smprops.set ( "MissingDataValue", "" + __missing );
			smprops.set ( "OutputPrecision", "" + precision );
			if ( getOutputDetailedHeader() ) {
				smprops.set ( "PrintGenesis", "true" );
			}
			else {
                smprops.set ( "PrintGenesis", "false" );
			}
			try {
                StateMod_TS.writeTimeSeriesList ( tslist,smprops );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1, routine,
				"Unable to write StateMod file \"" +
				output_file + "\"" );
			}
		}
		else {	Message.printWarning ( 1, routine,
			"Unable to write StateMod output for interval "
			+ interval );
		}
		comments = null;
	}
	else {	Message.printWarning ( 1, routine, "Unable to write " +
		"StateMod time series of different intervals." );
	}
}

public void windowActivated ( WindowEvent e )
{
}

/**
TODO SAM 2004-07-22 - not needed since _graph_list was removed?
*/
public void windowClosed ( WindowEvent e )
{	
}

/**
*/
public void windowClosing ( WindowEvent e )
{
}

public void windowDeactivated ( WindowEvent e )
{
}

public void windowDeiconified ( WindowEvent e )
{
}

public void windowIconified ( WindowEvent e )
{
}

public void windowOpened ( WindowEvent e )
{
}

} // End of TSEngine
