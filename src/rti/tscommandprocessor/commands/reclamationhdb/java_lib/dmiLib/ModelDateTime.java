// ModelDateTime class to convert DSS, HDB, RiverWare, SQL, and various model date times
// to an internal java date time and to convert a java date time to various formats

//package java_lib.dmiLib;
// Provided by Dave King, Reclamation
package rti.tscommandprocessor.commands.reclamationhdb.java_lib.dmiLib;

import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.Calendar;
import java.util.Locale;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Date;
import java.sql.Timestamp;

/**
*	 A class to convert DSS, HDB, RiverWare, SQL, and various model date times to an
*	internal java date time and to convert a java date time to various formats
*/

public class ModelDateTime extends GregorianCalendar
{
	private double juldate = -1;
	private String[] month_names = {"jan", "feb", "mar", "apr", "may", "jun",
									"jul", "aug", "sep", "oct", "nov", "dec"};
	private String[] cap_month_names = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
									"Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

	// Overridden constructors

	// no argument constructor returns system date time

	/**
	* System Date Time Constructor by time zone.
	* @param super No argument constructor returns system date time.
	*/

	public ModelDateTime(){super();}

	/**
	* System Date Time Constructor.
	* @param zone Time zone.
	*/

	public ModelDateTime(TimeZone zone){super(zone);}

	/**
	* Locale System Date Time Constructor.
	* @param aLocale Location.
	*/

	public ModelDateTime(Locale aLocale){super(aLocale);}

	/**
	* Locale and Time Zone System Date Time Constructor.
	* @param zone Time zone.
	* @param aLocale Location.
	*/

	public ModelDateTime(TimeZone zone,Locale aLocale){super(zone,aLocale);}

	/**
	* YMD Constructor.
	* @param y Year.
	* @param m Month.
	* @param d Day.
	*/

	public ModelDateTime (int y, int m, int d)
	{
		set (y, m, d, 0, 0, 0, 0);
	}

	/**
	* YMDHM Constructor.
	* @param y Year.
	* @param m Month.
	* @param d Day.
	* @param h Hour.
	* @param m Minute.
	*/

	public ModelDateTime (int y, int m, int d, int h, int mi)
	{
		set (y, m, d, h, mi, 0, 0);
	}

	/**
	* YMDHMS Constructor.
	* @param y Year.
	* @param m Month.
	* @param d Day.
	* @param h Hour.
	* @param m Minute.
	* @param s Second.
	*/

	public ModelDateTime (int y, int m, int d, int h, int mi, int s)
	{
		set (y, m, d, h, mi, s, 0);
	}

// New contstructors

	/**
	* YMDHM double seconds Constructor.
	* @param y Year.
	* @param m Month.
	* @param d Day.
	* @param h Hour.
	* @param m Minute.
	* @param s Second as double.
	*/

	public ModelDateTime (int y, int m, int d, int h, int mi, double s)
	{
		set (y, m, d, h, mi, (int)s, (int)((s - (int)s) * 1000.0));
	}

	/**
	* julian day Constructor.
	* @param jd Julian day.
	*/

	public ModelDateTime (double jd)
	{
		juldate = jd ;
		setJul2Greg (jd);
	}

	/**
	* Julian day and time zone Constructor.
	* @param jd Julian day.
	* @param zone Time zone.
	*/

	public ModelDateTime (double jd, TimeZone zone)
	{
		juldate = jd ;
		setTimeZone (zone);
		setJul2Greg (jd);
	}

	/**
	* String date Constructor.
	* @param date Date as YYYY-MM-DD.
	* @exception SetModelDateTimeException
	*/

	public ModelDateTime (String date) throws SetModelDateTimeException
	{
		set (date);
	}

	/**
	* <pre>
	* Method to construct a ModelDateTime for HDB, DSS, RiverWare, and java.
	* DSS and RiverWare specifies 24:00 as midnight and have no 0.0 time.
    * RiverWare daily and monthly models always specify hour or minute 24:00.
    * RiverWare datetime's have no seconds.
	* Therefore, the following are identical:
	*
	* 		ModelDateTime           DSS and RiverWare
	* 		01/15/2001 0000     =   01/14/2001 2400
	*
    * HDB specifies end of periods as 1 rather for daily, monthly, and annual data
    * HDB specifies smaller end of periods as 0.
	*
    * The existing styles are:
	*
    * 1 RiverWare as 2002-12-01 24:00 YYYY-MM-DD HH:mm
    * 2 HDB as 2002-12-01 23:59 YYYY-MM-DD HH:mm
    * 3 DSS as 12Jan2002 24:00 DDmonYYYY HH:mm
    * 4 DSS as 12/01/2001 24:00 MM/DD/YYYY HH:mm
    * 5 DSS as 2001-12-01 24:00 YYYY-MM-DD HH:mm
    * 6 java as 2001-12-01 23:59 YYYY-MM-DD HH:mm
    * 7 java as 12-01-2001 2359 YYYY-MM-DD HHmm
	*
	* </pre>
	* @param style Parameter to specify input format style and model or data type.
	* @param timeStep Timestep specified in RiverWare terms such as 1DAY, 1MONTH, 1YEAR, etc.
	* @param dateTime Datetime specified in appropriate format for style.
	*/

	public ModelDateTime (int style, String timeStep, String dateTime)
									throws SetModelDateTimeException
	{
		int year = 0;
		int month = 1;
		int day = 1;
		int hour = 0;
		int minute = 0;
		int second = 0;

		switch (style)
		{
			case 1:

				// RiverWare date specifed as 2002-01-12 24:00

				year = (Integer.valueOf (dateTime.substring (0, 4)).intValue ());

				if (timeStep.equalsIgnoreCase("1YEAR"))
				{
					// annual model

					month = 12;
					day = 31;
					hour = 23;
					minute = 59;
				}
				else
				{
					month = (Integer.valueOf (dateTime.substring (5, 7)).intValue ());
					set (year, month, day, hour, minute, second, 0);
					if (timeStep.equalsIgnoreCase("1MONTH"))
					{
						// monthly model

						day = daysInMonth();
						hour = 23;
						minute = 59;
					}
					else
					{
						day = (Integer.valueOf (dateTime.substring (8, 10)).intValue ());

						if (timeStep.equalsIgnoreCase("1DAY"))
						{
							hour = 23;
							minute = 59;
						}
						else
						{
							// smaller time step model

							hour = (Integer.valueOf (dateTime.substring (11, 13)).intValue ());
							if (hour > 23)
							{
								hour = 23;
								minute = 59;
							}
							else
							{
								minute = (Integer.valueOf (dateTime.substring (14, 16)).intValue ()) - 1;
							}
							if (minute < 0)
							{
								hour = hour - 1;
								minute = 59;
								if (hour < 0)
								{
									day = day - 1;
									hour = 23;
									if (day < 1)
									{
										day = TimeValue.daysInMonth(month, year);
										month = month - 1;
										if (month < 1)
										{
											year = year - 1;
											month = 12;
										}
									}
								}
							}
						}	// day test
					}	// month test
				}  // year test

				set (year, month, day, hour, minute, second, 0);
				break;

			case 2:

				// HDB date specifed as 2002-01-12 23:59

				year = (Integer.valueOf (dateTime.substring (0, 4)).intValue ());

				if (timeStep.equalsIgnoreCase("1YEAR"))
				{
					// annual data

					month = 1;
					day = 1;
				}
				else
				{
					month = (Integer.valueOf (dateTime.substring (5, 7)).intValue ());
					if (timeStep.equalsIgnoreCase("1MONTH"))
					{
						// monthly data

						day = 1;
					}
					else
					{
						day = (Integer.valueOf (dateTime.substring (8, 10)).intValue ());
						if (!timeStep.equalsIgnoreCase("1DAY") && dateTime.length () > 11)
						{
							// smaller time step data

							hour = (Integer.valueOf (dateTime.substring (11, 13)).intValue ());
							if (hour > 23)
							{
								hour = 23;
							}
							if (!timeStep.equalsIgnoreCase("1HOUR") && dateTime.length () > 14)
							{
								minute = (Integer.valueOf (dateTime.substring (14, 16)).intValue ());
								if (dateTime.length () > 17)second = (Integer.valueOf (dateTime.substring (17, 19)).intValue ());
							}	// hour test
						}	// day test
					}	 // month test
				}	// year test

				set (year, month, day, hour, minute, second, 0);
				break;

			case 3:

				// DSS date time specified as 12Jan2001 24:00

				day = (Integer.valueOf (dateTime.substring (0, 2)).intValue ());
				String cMonth = dateTime.substring (2, 5);
				year = (Integer.valueOf (dateTime.substring (5, 9)).intValue ());

				for (int m = 0; m < 11; m++)
				{
					if (cMonth.equalsIgnoreCase(month_names[m]))
					{
						month = m + 1;
						break;
					}
				}

				if (dateTime.length () > 11)
				{
					hour = (Integer.valueOf (dateTime.substring (11, 13)).intValue ());
					if (hour > 23)
					{
						hour = 23;
						minute = 59;
					}
					else
					{
						minute = (Integer.valueOf (dateTime.substring (14, 16)).intValue ()) - 1;
					}
					if (minute < 0)
					{
						hour = hour - 1;
						minute = 59;
						if (hour < 0)
						{
							day = day - 1;
							hour = 23;
							if (day < 1)
							{
								day = TimeValue.daysInMonth(month, year);
								month = month - 1;
								if (month < 1)
								{
									year = year - 1;
									month = 12;
								}
							}
						}
					}
				}
				else
				{
					// hour and minute were not specifed - default to 23:59

					hour = 7;
					minute = 59;
				}

				set (year, month, day, hour, minute, second, 0);
				break;

			case 4:

				// DSS date time specified as 01/12/2000 24:00

				month = (Integer.valueOf (dateTime.substring (0, 2)).intValue ());
				day = (Integer.valueOf (dateTime.substring (3, 5)).intValue ());
				year = (Integer.valueOf (dateTime.substring (6, 10)).intValue ());

				if (dateTime.length () > 10)
				{
					hour = (Integer.valueOf (dateTime.substring (11, 13)).intValue ());
					if (hour > 23)
					{
						hour = 23;
						minute = 59;
					}
					else
					{
						minute = (Integer.valueOf (dateTime.substring (14, 16)).intValue ()) - 1;
					}
					if (minute < 0)
					{
						hour = hour - 1;
						minute = 59;
						if (hour < 0)
						{
							day = day - 1;
							hour = 23;
							if (day < 1)
							{
								day = TimeValue.daysInMonth(month, year);
								month = month - 1;
								if (month < 1)
								{
									year = year - 1;
									month = 12;
								}
							}
						}
					}
				}
				else
				{
					// hour and minute were not specifed - default to 23:59

					hour = 23;
					minute = 59;
				}

				set (year, month, day, hour, minute, second, 0);
				break;

			case 5:

				// DSS date time specified as 2002-01-12 24:00

				year = (Integer.valueOf (dateTime.substring (0, 4)).intValue ());
				month = (Integer.valueOf (dateTime.substring (5, 7)).intValue ());
				day = (Integer.valueOf (dateTime.substring (8, 10)).intValue ());

				if (dateTime.length () > 10)
				{
					hour = (Integer.valueOf (dateTime.substring (11, 13)).intValue ());
					if (hour > 23)
					{
						hour = 23;
						minute = 59;
					}
					else
					{
						minute = (Integer.valueOf (dateTime.substring (14, 16)).intValue ()) - 1;
					}
					if (minute < 0)
					{
						hour = hour - 1;
						minute = 59;
						if (hour < 0)
						{
							day = day - 1;
							hour = 23;
							if (day < 1)
							{
								day = TimeValue.daysInMonth(month, year);
								month = month - 1;
								if (month < 1)
								{
									year = year - 1;
									month = 12;
								}
							}
						}
					}
				}
				else
				{
					// hour and minute were not specifed - default to 23:59

					hour = 23;
					minute = 59;
				}

				set (year, month, day, hour, minute, second, 0);
				break;

			case 6:

				// java date time specified as 2002-01-12 23:59

				year = (Integer.valueOf (dateTime.substring (0, 4)).intValue ());
				month = (Integer.valueOf (dateTime.substring (5, 7)).intValue ());
				day = (Integer.valueOf (dateTime.substring (8, 10)).intValue ());

				if (dateTime.length () > 10)
				{
					hour = (Integer.valueOf (dateTime.substring (11, 13)).intValue ());
					minute = (Integer.valueOf (dateTime.substring (14, 16)).intValue ());
				}
				else
				{
					// hour and minute were not specifed - default to 08:00/07:59

					hour = 7;
					minute = 59;

				}
				if (second > 59)
				{
					second = 0;
					minute++;
				}
				if (minute > 59)
				{
					minute = 0;
					hour++;
				}
				if (hour > 23)
				{
					hour = 0;
					day++;
				}
				set (year, month, day, hour, minute, second, 0);

				if (minute < 0)
				{
					hour = hour - 1;
					minute = 59;
					if (hour < 0)
					{
						day = day - 1;
						hour = 0;
						if (day < 1)
						{
							day = daysInMonth();
							month = month - 1;
							if (month < 1)
							{
								year = year - 1;
								month = 12;
							}
						}
					}
				}
				set (year, month, day, hour, minute, second, 0);
				break;

			case 7:

				// java date time specified as 01-12-2002 2359

				month = (Integer.valueOf (dateTime.substring (0, 2)).intValue ());
				day = (Integer.valueOf (dateTime.substring (3, 5)).intValue ());
				year = (Integer.valueOf (dateTime.substring (6, 10)).intValue ());

				if (dateTime.length () > 9)
				{
					hour = (Integer.valueOf (dateTime.substring (11, 13)).intValue ());
					minute = (Integer.valueOf (dateTime.substring (13, 15)).intValue ());
				}
				else
				{
					// hour and minute were not specifed - default to 08:00/07:59

					hour = 7;
					minute = 59;

				}
				if (second > 59)
				{
					second = 0;
					minute++;
				}
				if (minute > 59)
				{
					minute = 0;
					hour++;
				}
				if (hour > 23)
				{
					hour = 0;
					day++;
				}
				set (year, month, day, hour, minute, second, 0);

				if (minute < 0)
				{
					hour = hour - 1;
					minute = 59;
					if (hour < 0)
					{
						day = day - 1;
						hour = 0;
						if (day < 1)
						{
							day = daysInMonth();
							month = month - 1;
							if (month < 1)
							{
								year = year - 1;
								month = 12;
							}
						}
					}
				}
				set (year, month, day, hour, minute, second, 0);
				break;

			default:
			set (dateTime);
		}	// end case
	}

	// unix date time

	/**
	* Unix date time Constructor.
	* @param s1 Unix date time argument 1.
	* @param s2 Unix date time argument 2.
	* @param s3 Unix date time argument 3.
	* @param s4 Unix date time argument 4.
	* @param s5 Unix date time argument 5.
	* @param s6 Unix date time argument 6.
	* @exception setModelDateTimeException
	*/

	public ModelDateTime (String s1, String s2, String s3, String s4, String s5, String s6)
											throws SetModelDateTimeException
	{
		set (s1, s2, s3, s4, s5, s6);
	}

	/**
	* String and Time Zone Constructor.
	* @param dateTime Date time.
	* @param zone Time zone.
	* @exception SetModelDateTimeException
	*/

	public ModelDateTime (String dateTime, TimeZone zone) throws SetModelDateTimeException
	{
		setTimeZone (zone);
		set (dateTime);
	}

// Methods that overload Calendar methods

	/**
	* Method to set model date time.
	* @param y Year.
	* @param m Month.
	* @param d Day.
	* @param h Hour.
	* @param mi Minute.
	* @param s Second.
	* @param ms Millisecond.
	*/

	public void set (int y, int m, int d, int h, int mi, int s, int ms)
	{
//		System.out.println("Constructing ModelDateTime with " + y + ":" + m + ":" + d + ":" + h + ":" + mi + ":" + s + ":" + ms);
		set (Calendar.YEAR, y);
		set (Calendar.MONTH, (m - 1));
		set (Calendar.DAY_OF_MONTH, d);
		set (Calendar.HOUR_OF_DAY, h);
		set (Calendar.MINUTE, mi);
		set (Calendar.SECOND, s);
		set (Calendar.MILLISECOND, ms);
	}

	// standard date time creation

	/**
	* Method to set model date time for standard date time.
	* @param dateTime Date time.
	*/

	public void set (String dateTime)
	{
		int year, month, day, hour, minute, second;

		year = (Integer.valueOf (dateTime.substring (0, 4)).intValue ());
		month = (Integer.valueOf (dateTime.substring (5, 7)).intValue ());
		day = (Integer.valueOf (dateTime.substring (8, 10)).intValue ());
		hour = 0;
		minute = 0;
		second = 0;

		if (dateTime.length () > 15)
		{
			hour = (Integer.valueOf (dateTime.substring (11, 13)).intValue ());
			minute = (Integer.valueOf (dateTime.substring (14, 16)).intValue ());
		}

		if (dateTime.length () == 19)
		{
			second = (Integer.valueOf (dateTime.substring (17, 19)).intValue ());
		}

		set (year, month, day, hour, minute, second, 0);
	}

	/**
	* Method to set model date time for unix date time.
	* @param s1 Unix string parameter 1.
	* @param s2 Unix string parameter 2.
	* @param s3 Unix string parameter 3.
	* @param s4 Unix string parameter 4.
	* @param s5 Unix string parameter 5.
	* @param s6 Unix string parameter 6.
	*/

	public void set (String s1, String s2, String s3, String s4, String s5, String s6)
	{
		int year, day, hour, minute, second;
		int month = 0;

		year = (Integer.valueOf (s6).intValue () );
		day = (Integer.valueOf (s3).intValue () );
		hour = (Integer.valueOf (s4.substring (0, 2)).intValue ());
		minute = (Integer.valueOf (s4.substring (3, 5)).intValue ());
		second = (Integer.valueOf (s4.substring (6, 8)).intValue ());
		for (int m = 0; m < 11; m++)
		{
			if (s2.equalsIgnoreCase(month_names[m]))
			{
				month = m + 1;
				break;
			}
		}
		set (year, month, day, hour, minute, second, 0);
	}

	// Output methods

	/**
	* Method to get Mms Date Time.
	* @return Mms date time.
	*/

	public String getMmsDateTime ()
	{
		Format f = new Format ("%02d");
		Format sf = new Format ("%05.2f");

		float sec 	= (float)(getSecond())
					+ (float)(get (Calendar.MILLISECOND) * 0.001);

		return (getYear() + " "
			+ f.form (getMonth()) + " "
			+ f.form (getDay()) + " "
			+ f.form (getHour()) + " "
			+ f.form (getMinute()) + " "
			+ sf.form (sec));
	}

	/**
	* Method to get tab delimited date time.
	* @return Tab date time.
	*/

	public String getTabDateTime ()
	{
		Format f = new Format ("%02d");
		return (getYear() + "\t"
			+ f.form (getMonth()) + "\t"
			+ f.form (getDay()) + "\t"
			+ f.form (getHour()) + "\t"
			+ f.form (getMinute()) + "\t"
			+ f.form (getSecond()));
	}

	/**
	* Method to get mms control file date time for oui.
	* @return control file date time.
	*/

	public String getControlFileDateTime ()
	{
		Format f = new Format ("%02d");
		float sec 	= (float)(get (Calendar.SECOND))
					+ (float)(get (Calendar.MILLISECOND) * 0.001);
		return (get (Calendar.YEAR) + ","
			+ f.form (get (Calendar.MONTH) + 1) + ","
			+ f.form (get (Calendar.DAY_OF_MONTH)) + ","
			+ f.form (get (Calendar.HOUR_OF_DAY)) + ","
			+ f.form (get (Calendar.MINUTE)) + ","
			+ f.form (get (Calendar.SECOND)));
	}

	/**
	* Method to convert to string.
	* @return SQL date time as string.
	*/

	public String toString ()
	{
		return (getSQLDate ());
	}

	/**
	* Method to get month and day.
	* @return Month and daty as 'jan-31'
	*/

	public String getMonthDay ()
	{
		Format f = new Format ("%02d");
		return (get3CharMonth() + "-" + f.form (get (Calendar.DAY_OF_MONTH)));
	}

	/**
	* Method to get Month, Day, and Year.
	* @return MonDayYear in requested style
	*/

	public String getMonDayYear (int style)
	{
		Format f = new Format ("%02d");
		switch (style)
		{
			case 1:

				//	return Date as '01jan2001'

				return (f.form (getDay()) + get3CharMonth() + f.form (getYear()));
			case 2:

				//	return Date as '01/01/2001'

				return (f.form (getMonth()) + "/" + f.form (getDay()) + "/" + f.form (getYear()));
			case 3:

				//	return Date as '01-15-2001'

				return (f.form (getMonth()) + "-" + f.form (getDay()) + "-" + f.form (getYear()));
			default:
				return (f.form (getMonth()) + "/" + f.form (getDay()) + "/" + f.form (getYear()));
		}
	}

	/**
	* Method to get Month and Year.
	* @return MonYear in requested style
	*/

	public String getMonYear (int style)
	{
		Format f = new Format ("%02d");
		switch (style)
		{
			case 1:

				//	return Date as 'jan2001'

				return (get3CharMonth() + f.form (getYear()));
			case 2:

				//	return Date as '01/2001'

				return (f.form (getMonth()) + "/" + f.form (getYear()));
			case 3:

				//	return Date as '01-2001'

				return (f.form (getMonth()) + "-" + "-" + f.form (getYear()));
			default:
				return (f.form (getMonth()) + "/" + f.form (getYear()));
		}
	}

	/**
	* Method to get date.
	* @return Date as YYYY-MM-DD.
	*/

	public String getSQLDate ()
	{
		Format f = new Format ("%02d");
		return (getYear() + "-"
			+ f.form (getMonth()) + "-"
			+ f.form (getDay()));
	}

	/**
	* Method to get non delimited date.
	* @return Date as YYYYMMDD.
	*/

	public String getNoDelimDate ()
	{
		Format f = new Format ("%02d");
		return (getYear() + f.form (getMonth()) + f.form (getDay()));
	}

	/**
	* Method to get a SQL Date time.
	* @return Date as YYYY-MM-DD:hh:mm:ss where ss is decimal seconds.
	*/

	public String getSQLDateTime ()
	{
		Format f = new Format ("%02d");
		Format sf = new Format ("%04.1f");
		float sec 	= (float)(getSecond())
					+ (float)(get (Calendar.MILLISECOND) * 0.001);
		return (getYear() + "-"
			+ f.form (getMonth()) + "-"
			+ f.form (getDay()) + " "
			+ f.form (getHour()) + ":"
			+ f.form (getMinute()) + ":"
			+ sf.form (sec));
	}

	/**
	* Method to get an Access compatable Date time.
	* @return Date as YYYY-MM-DD:hh:mm:ss where ss is integer seconds.
	*/

	public String getAccessDateTime ()
	{
		Format f = new Format ("%02d");
		Format sf = new Format ("%02.0f");
		float sec 	= (float)(getSecond())
					+ (float)(get (Calendar.MILLISECOND) * 0.001);
		return (getYear() + "-"
			+ f.form (getMonth()) + "-"
			+ f.form (getDay()) + " "
			+ f.form (getHour()) + ":"
			+ f.form (getMinute()) + ":"
			+ sf.form (sec));
	}

	/**
	* Method to get a DMI date time with no time transformations.
	* @return Date as YYYY-MM-DD:hh:mm where ss is decimal seconds.
	*/

	public String getDMIDateTime ()
	{
		Format f = new Format ("%02d");
		Format sf = new Format ("%04.1f");
		float sec 	= (float)(getSecond())
					+ (float)(get (Calendar.MILLISECOND) * 0.001);
		return (getYear() + "-"
			+ f.form (getMonth()) + "-"
			+ f.form (getDay()) + " "
			+ f.form (getHour()) + ":"
			+ f.form (getMinute()));
	}

	/**
	* Method to get an Oracle date.
	* @return Date as YYYY-MM-DD.
	*/

	public String getOracleDate ()
	{
		Format f = new Format ("%02d");
		return (getYear() + "-"
			+ f.form (getMonth()) + "-"
			+ f.form (getDay()));
	}

	/**
	* Method to get an Oracle date time.
	* @return Date as YYYY-MM-DD:mm:ss where ss is seconds.
	*/

	public String getOracleDateTime ()
	{
		Format f = new Format ("%02d");
		return (getYear() + "-"
			+ f.form (getMonth()) + "-"
			+ f.form (getDay()) + " "
			+ f.form (getHour()) + ":"
			+ f.form (getMinute()) + ":"
			+ f.form (getSecond()));
	}

	/**
	/**
	* Method to get RiverWare date time.
	* @param timeStep Time step as 1YEAR, 1DAY, or 1MONTH.
	* @if timestep is year, month, or day, return end of period.
	* @return Date as 'YYYY-MM-DD 24:00'.
	* @Account for 1 second difference from a ModelDateTime.
	*/

	public String getRiverWareDateTime (String timeStep)
	{
		Format f = new Format ("%02d");
		int hour, min, day;
		int year = getYear();
		int month = getMonth();

		if (timeStep.equalsIgnoreCase("1YEAR")
			|| timeStep.equalsIgnoreCase("1MONTH")
			|| timeStep.equalsIgnoreCase("1DAY"))
		{
			hour = 24;
			min = 0;
			if (timeStep.equalsIgnoreCase("1YEAR"))
			{
				day = 31;
				month = 12;
			}
			else if (timeStep.equalsIgnoreCase("1MONTH"))
			{
				day = daysInMonth();
			}
			else
			{
				day = getDay();
			}
		}
		else
		{
			day = getDay();
			hour = getHour();
			min = getMinute() + 1;
			if (min > 59)
			{
				min = 0;
				hour = hour + 1;
				if (hour > 24)
				{
					hour = 0;
					day = day + 1;
					if (day > daysInMonth())
					{
						day = 1;
						month = month + 1;
						if (month > 12)
						{
							month = 1;
							year = year + 1;
						}
					}
				}
			}
		}
		return (year + "-"
			+ f.form (month) + "-"
			+ f.form (day) + " "
			+ f.form (hour) + ":"
			+ f.form (min));
	}	// end of method

	/**
	* Method to get a DSS date time.
	* @param style - style to return.
	* @return date time string in requested style.
	* @Account for 1 second difference from a ModelDateTime.
	*/

	public String getDssDateTime (int style)
	{
		Format f = new Format ("%02d");
		int hour, min, day;
		int year = getYear();
		int month = getMonth();

		year = getYear();
		month = getMonth();
		day = getDay();
		hour = getHour();
		min = getMinute() + 1;

		if (min > 59)
		{
			min = 0;
			hour = hour + 1;
			if (hour > 24)
			{
				hour = 0;
				day = day + 1;
				if (day > daysInMonth())
				{
					day = 1;
					month = month + 1;
					if (month > 12)
					{
						month = 1;
						year = year + 1;
					}
				}
			}
		}

		switch (style)
		{
			case 1:

				// jan/01/2001 24:00

				return (get3CharMonth() + "/"
					+ f.form (day) + "/"
					+ year + " "
					+ f.form (hour) + ":"
					+ f.form (min));

			case 2:

				// 01-12-2002 24:00

				return (f.form (month) + "/"
					+ f.form (day) + "/"
					+ year + " "
					+ f.form (hour) + ":"
					+ f.form (min));

			case 3:

				// 2002-01-12 24:00

				return (year + "/"
					+ f.form (month) + "/"
					+ f.form (day) + " "
					+ f.form (hour) + ":"
					+ f.form (min));

			case 4:

				// 01jan2001 2400

				return (f.form (day)
					+ get3CharMonth()
					+ year + " "
					+ f.form (hour)
					+ f.form (min));

			case 5:

				// 01Jan2001 24:00

				return (f.form (day)
					+ get3CharCapMonth()
					+ year + " "
					+ f.form (hour) + ":"
					+ f.form (min));

			default:
				return (year + "/"
					+ f.form (month) + "/"
					+ f.form (day) + " "
					+ f.form (hour) + ":"
					+ f.form (min));

		}	// end of switch

	}	// end of method

	/**
	* Method to get HDB date time.
	* @param timeStep Time step as 1YEAR, 1MONTH, 1HOUR, etc.
	* @Otherwise, use actual day.
	* @return Date as Hdb datetime.
	*/

	public String getHDBDateTime (String timeStep)
	{
		Format f = new Format ("%02d");
		int day = getDay();
		int month = getMonth();
		int hour = getHour();
		int min = getMinute();
		int sec = getSecond();

		if (timeStep.equalsIgnoreCase("1YEAR"))
		{
			month = 1;
			day = 1;
			hour = 0;
			min = 0;
			sec = 0;
		}
		else
		{
			if (timeStep.equalsIgnoreCase("1MONTH"))
			{
				day = 1;
				hour = 0;
				min = 0;
				sec = 0;
			}
			if (timeStep.equalsIgnoreCase("1DAY"))
			{
				hour = 0;
				min = 0;
				sec = 0;
			}
		}

		return (getYear() + "-"
			+ f.form (month) + "-"
			+ f.form (day) + " "
			+ f.form (hour) + " "
			+ f.form (min) + " "
			+ f.form (sec));
	}

	/**
	* Method to get time portion of a ModelDateTime.
	* @return several variations of time.
	*/

	public String getTime (int style)
	{
		switch (style)
		{
			case 1:
				return getMilTime1();
			case 2:
				return getMilTime2();
			case 3:
				return getMilTime3();
			case 4:
				return getMilTime4();
			case 5:
				return getTime5();
			case 6:
				return getTime6();
			case 7:
				return getTime7();
			default:
				return getTime7();
		}
	}

	/**
	* Method to get a ModelDateTime military time.
	* @return time as '2330' for 0000 to 2359 times
	*/

	public String getMilTime1 ()
	{
		Format f = new Format ("%02d");
		return (f.form (getHour()) + f.form (getMinute()));
	}

	/**
	* Method to get a ModelDateTime military time.
	* @return time as '23:30' for 0000 to 2359 times
	*/

	public String getMilTime2 ()
	{
		Format f = new Format ("%02d");
		return (f.form (getHour()) + ":" + f.form (getMinute()));
	}

	/**
	* Method to get a ModelDateTime military time.
	* @return time as '2330' for 0001 to 2400 times
	*/

	public String getMilTime3 ()
	{
		Format f = new Format ("%02d");
		int min = getMinute() + 1;
		int hour = getHour();
		if (min > 59)
		{
			min = 0;
			hour = hour + 1;
			if(hour > 24) hour = 0;
		}
		return (f.form (hour) + f.form (min));
	}

	/**
	* Method to get a ModelDateTime military time.
	* @return time as '23:30' for 0001 to 2400 times
	*/

	public String getMilTime4 ()
	{
		Format f = new Format ("%02d");
		int min = getMinute() + 1;
		int hour = getHour();
		if (min > 59)
		{
			min = 0;
			hour = hour + 1;
			if(hour > 24) hour = 0;
		}
		return (f.form (hour) + ":" + f.form (min));
	}

	/**
	* Method to get a ModelDateTime time.
	* @return time as HH:mm:ss (decimal seconds)
	*/

	public String getTime5 ()
	{
		Format f = new Format ("%02d");
		Format sf = new Format ("%04.1f");
		float sec 	= (float)(getSecond())
					+ (float)(get (Calendar.MILLISECOND) * 0.001);
		return (f.form (getHour()) + ":"
			+ f.form (getMinute()) + ":"
			+ sf.form (sec));
	}

	/**
	* Method to get a ModelDateTime time.
	* @return time as HH:mm:ss (whole seconds)
	*/

	public String getTime6 ()
	{
		Format f = new Format ("%02d");
		return (f.form (getHour()) + ":"
			+ f.form (getMinute()) + ":" + f.form (getSecond()));
	}

	/**
	* Method to get a ModelDateTime time.
	* @return tab delimited time as HH mm ss
	*/

	public String getTime7 ()
	{
		Format f = new Format ("%02d");
		return (f.form (getHour()) + "\t"
			+ f.form (getMinute()) + "\t" + f.form (getSecond()));
	}

	/**
	* Method to get a java Date.
	* @return Date that keeps JDBC happy and is Y2K compliant
	*/

	public Date getJDBCDate ()
	{

		return new Date (getTimeInMillis ());
	}

	/**
	* Method to get a java Timestamp.
	* @return Timestamp that keeps JDBC happy and is Y2K compliant
	*/

	public Timestamp getJDBCDateTime ()
	{
		return new Timestamp (getTimeInMillis ());
	}

	/**
	* Method to get timesteps between datetimes.
	* @param endDate End date.
	* @return number of timesteps between 2 date times.
	*/

	public int timestepsBetween(String timeStep, ModelDateTime endDate)
	{
		if (timeStep.equalsIgnoreCase("1DAY"))return daysBetween(endDate);
		else if (timeStep.equalsIgnoreCase("1YEAR"))return yearsBetween(endDate);
		else if (timeStep.equalsIgnoreCase("1MONTH"))return monthsBetween(endDate);
		else if (timeStep.equalsIgnoreCase("6HOUR"))return sixHoursBetween(endDate);
		else if (timeStep.equalsIgnoreCase("1HOUR"))return hoursBetween(endDate);
		else if (timeStep.equalsIgnoreCase("INSTANT"))return hoursBetween(endDate);
		else if (timeStep.equalsIgnoreCase("1MINUTE"))return minutesBetween(endDate);
		else if (timeStep.equalsIgnoreCase("1SECOND"))return secondsBetween(endDate);
		else return daysBetween(endDate);
	}

	/**
	* Method to get seconds between datetimes.
	* @param endDate End date.
	* @return seconds between 2 date times
	*/

	public int secondsBetween(ModelDateTime endDate)
	{
		return (int) ( Math.round(86400.0 * endDate.getJulian()) - Math.round(86400.0 * getJulian()) );
	}

	/**
	* Method to get minutes between datetimes.
	* @param endDate End date.
	* @return minutes between 2 date times
	*/

	public int minutesBetween(ModelDateTime endDate)
	{
		return (int) ( Math.round(1440.0 * endDate.getJulian()) - Math.round(1440.0 * getJulian()) );
	}

	/**
	* Method to get hours between datetimes.
	* @param endDate End date.
	* @return hours between 2 date times
	*/

	public int hoursBetween(ModelDateTime endDate)
	{
		return (int) ( Math.round(24.0 * endDate.getJulian()) - Math.round(24.0 * getJulian()) );
	}

	/**
	* Method to get 6 hours between datetimes.
	* @param endDate End date time.
	* @return number of 6 hour intervals between 2 dates
	*/

	public int sixHoursBetween(ModelDateTime endDate)
	{
		return (int) ( ( Math.round(24 * endDate.getJulian()) - Math.round(24 * getJulian()) ) / 6.0);
	}

	/**
	* Method to get days between dates.
	* @param endDate End date.
	* @return days between 2 date times
	*/

	public int daysBetween(ModelDateTime endDate)
	{
		return endDate.getIntJulian() - getIntJulian();
	}

	/**
	* Method to get months between dates.
	* @param endDate End date.
	* @return Months between 2 dates (use with monthly models).
	*/

	public int monthsBetween(ModelDateTime endDate)
	{
		int numMonths;
		if (endDate.getMonth() < getMonth())
			numMonths	= ((endDate.getYear() - getYear() - 1) * 12)
						+ (12 + endDate.getMonth() - getMonth());

		else
			numMonths	= ((endDate.getYear() - getYear()) * 12)
						+ (endDate.getMonth() - getMonth());
		return numMonths;
	}

	/**
	* Method to get years between dates.
	* @param endDate End date.
	* @return Years between 2 dates.
	*/

	public int yearsBetween(ModelDateTime endDate)
	{
		return endDate.getYear() - getYear();
	}

	/**
	* Method to increment or decrement n timesteps.
	* @param nDays Number of days to add or subtract.
	* @return Altered date time.
	*/

	public ModelDateTime advanceNTimesteps(String timeStep, int nTimesteps)
	{
		if (timeStep.equalsIgnoreCase("1YEAR"))
		{
			return advanceNYears(nTimesteps);
		}
		else if (timeStep.equalsIgnoreCase("1MONTH"))
		{
			return advanceNMonths(nTimesteps);
		}
		else if (timeStep.equalsIgnoreCase("1DAY"))
		{
			return advanceNDays(nTimesteps);
		}
		else if (timeStep.equalsIgnoreCase("6HOUR"))
		{
			return new ModelDateTime(getJulian() + 0.25 * ((double) nTimesteps));
		}
		else if (timeStep.equalsIgnoreCase("1HOUR"))
		{
			return advanceNHours(nTimesteps);
		}
		else if (timeStep.equalsIgnoreCase("1MINUTE"))
		{
			return advanceNMinutes(nTimesteps);
		}
		else if (timeStep.equalsIgnoreCase("1SECOND"))
		{
			return advanceNSeconds(nTimesteps);
		}
		else if (timeStep.equalsIgnoreCase("INSTANT"))
		{
			return advanceNHours(nTimesteps);
		}
		else
		{
			System.out.println("Not Yet Implemented.");
			return null;
		}
	}

	/**
	* Method to increment or decrement n seconds.
	* @param nDays Number of seconds to add or subtract.
	* @return Altered date time.
	*/

	public ModelDateTime advanceNSeconds(int nSeconds)
	{
		return new ModelDateTime(getJulian() + 1.0 / 86400.0 * ((double) nSeconds));
	}

	/**
	* Method to increment or decrement n minutes.
	* @param nDays Number of minutes to add or subtract.
	* @return Altered date time.
	*/

	public ModelDateTime advanceNMinutes(int nMinutes)
	{
		return new ModelDateTime(getJulian() + 1.0 / 1440.0 * ((double) nMinutes));
	}

	/**
	* Method to increment or decrement n hours.
	* @param nDays Number of hours to add or subtract.
	* @return Altered date time.
	*/

	public ModelDateTime advanceNHours(int nHours)
	{
/*
System.out.println("inbound dt is " + getSQLDateTime());
System.out.println("jd is " + getJulian());
ModelDateTime temp = new ModelDateTime(getJulian());
System.out.println("jd's sqldt is " + temp.getSQLDateTime());
ModelDateTime temp2 = new ModelDateTime(getJulian() + 1.0 / 24.0 * ((double) nHours));
System.out.println("advanced sqldt is " + temp2.getSQLDateTime());
*/
		return new ModelDateTime(getJulian() + 1.0 / 24.0 * ((double) nHours));
	}

	/**
	* Method to increment or decrement integer n days.
	* @param nDays Number of days to add or subtract.
	* @return Altered date time.
	*/

	public ModelDateTime advanceNDays(int nDays)
	{
		return new ModelDateTime(getJulian() + (double) nDays);
	}

	/**
	* Method to increment or decrement double n days.
	* @param nDays Number of days to add or subtract.
	* @return Altered date time.
	*/

	public ModelDateTime advanceNDays(double nDays)
	{
		return new ModelDateTime(getJulian() + nDays);
	}

	/**
	* Method to increment or decrement months (use with monthly models).
	* @param nMonths Number of months to add or subtract.
	* @return Altered date time.
	*/

	public ModelDateTime advanceNMonths(int nMonths)
	{
		ModelDateTime mdt2 = null;
		ModelDateTime mdt = new ModelDateTime(getJulian());
		mdt.add(Calendar.MONTH, nMonths);

		// Some weirdness going on here????  Returning above mdt
		// produced correct individual values but julian was
		// incorrect.  Recreating date as follows gets
		// everything consistent.

		mdt2 = new ModelDateTime(mdt.getYear(), mdt.getMonth(), mdt.daysInMonth(), mdt.getHour(), mdt.getMinute());

		return mdt2;
	}

	/**
	* Method to increment or decrement n years.
	* @param nYears Number of years to add or subtract.
	* @return Altered date time.
	*/

	public ModelDateTime advanceNYears(int nYears)
	{
		return advanceNMonths(nYears * 12);
	}

	/**
	* Method to return days in month.
	* @return Number days.
	*/

	public int daysInMonth()
	{
		int ndays;
		int days[] = {31,28,31,30,31,30,31,31,30,31,30,31};
		int month = get(Calendar.MONTH);
		if (month == 1 && isLeapYear(get(Calendar.YEAR)))
			ndays = 29;
		else
			ndays = days[month];
		return ndays;

	}

	/**
	* Method to return days in year.
	* @return Number days.
	*/

	public int daysInYear()
	{
		int ndays;
		if (isLeapYear(get(Calendar.YEAR)))
			ndays = 366;
		else
			ndays = 365;
		return ndays;
	}

	/**
	* Method to return year as int.
	* @return Year.
	*/

	public int getYear ()
	{
		return get(Calendar.YEAR);
	}

	/**
	* Method to return month as int.
	* @return Month.
	*/

	public int getMonth ()
	{
		return (get(Calendar.MONTH) + 1);
	}

	/**
	* Method to get month.
	* @return Month as 'jan'
	*/

	public String get3CharMonth ()
	{
		return month_names[get (Calendar.MONTH)];
	}

	/**
	* Method to get month.
	* @return Month as 'jan'
	*/

	public String get3CharCapMonth ()
	{
		return cap_month_names[get (Calendar.MONTH)];
	}

	/**
	* Method to return day as int.
	* @return Day.
	*/

	public int getDay ()
	{
		return get(Calendar.DAY_OF_MONTH);
	}

	/**
	* Method to return day of year.
	* @return Day.
	*/

	public int getDayOfYear ()
	{
		return get(Calendar.DAY_OF_YEAR);
	}

	/**
	* Method to return hour as int.
	* @return Hour.
	*/

	public int getHour ()
	{
		return get(Calendar.HOUR_OF_DAY);
	}

	/**
	* Method to return minute as int.
	* @return Minute.
	*/

	public int getMinute ()
	{
		return get(Calendar.MINUTE);
	}

	/**
	* Method to return second as int.
	* @return Second.
	*/

	public int getSecond ()
	{
		return get(Calendar.SECOND);
	}

	/**
	* Method to return milliseconds.
	* @return Millisecond.
	*/

	public long getMillis ()
	{
		return (getTimeInMillis ());
	}

	/**
	* Method to convert Julian date to Gregorian
	*
	* The Julian day starts at noon of the Gregorian day and extends
	* to noon the next Gregorian day. The Gregorian day is assumed
	* to begin at midnight.
	*
	* Julian date converter. Takes a julian date (the number of days since
	* some distant epoch or other), and returns an int pointer to static space.
	* Copied from Algorithm 199 in Collected algorithms of the CACM
	* Author: Robert G. Tantzen, Translator: Nat Howard
	*/

	public void setJul2Greg (double julian_day)
	{

		int month, day, year, hour, minute;
		double second;

		long j = (long)(julian_day);
		double tmp, frac = julian_day - j;

		if (frac >= 0.5)
		{
			frac = frac - 0.5;
			j++;
		}
		else
		{
			frac = frac + 0.5;
		}

		j -= 1721119L;

		year = (int)((4L * j - 1L) / 146097L);
		j = 4L * j - 1L - 146097L * year;
		day = (int)(j / 4L);
		j = (4L * day + 3L) / 1461L;
		day = (int)(4L * day + 3L - 1461L * j);
		day = (int)((day + 4L) / 4L);
		month = (int)((5L * day - 3L) / 153L);
		day = (int)(5L * day - 3 - 153L * month);
		day = (int)((day + 5L) / 5L);
		year = (int)(100L * year + j);
		if (month < 10) month += 3;
		else
		{
	    	month -= 9;
			year += 1;
		}
		tmp = 3600.0 * (frac * 24.0);
		hour = (int) (tmp / 3600.0);
		tmp = tmp - hour * 3600.0;

/* following code appears to lose some time in the translation

		minute = (int) (tmp / 60.0);

*/
		minute = (int) Math.round(tmp / 60.0);

		second = tmp - minute * 60.0;

		juldate = julian_day;
		set (year, month, day, hour, minute, (int)second,
		     (int)((second - (int)second) * 1000.0));
	}

	/**
	*  Method to take a date, and return a Julian day. A Julian day is the number of
	*  days since some base date  (in the very distant past).
	*  Handy for getting date of x number of days after a given Julian date
	*  (use jdate to get that from the Gregorian date).
	*  Author: Robert G. Tantzen, translator: Nat Howard
	*  Translated from the algol original in Collected Algorithms of CACM
	*  (This and jdate are algorithm 199).
	* @return Julian date as double.
	*/

	public double getJulian ()
	{
		if (juldate > 0.0) return (juldate);

		long m = (long)(get (Calendar.MONTH) + 1);
		long d = (long)(get (Calendar.DAY_OF_MONTH));
		long y = (long)(get (Calendar.YEAR));
		long c, ya, j;

		double seconds = (double)(get (Calendar.HOUR_OF_DAY) * 3600.0)
		               + (double)(get (Calendar.MINUTE) * 60)
		               + (double)(get (Calendar.SECOND))
		               + (double)(get (Calendar.MILLISECOND) * 0.001);

		if (m > 2) m -= 3;
		else
		{
		   m += 9;
		   --y;
		}

		c = y / 100L;
		ya = y - (100L * c);
		j	= (146097L * c) / 4L + (1461L * ya) / 4L + (153L * m + 2L)
			/ 5L + d + 1721119L;
		if (seconds < 12 * 3600.0)
		{
		   j--;
		   seconds += 12.0 * 3600.0;
		}
		else
		{
		   seconds = seconds - 12.0 * 3600.0;
		}

		juldate = j + (seconds / 3600.0) / 24.0;

		return (juldate);
	}

	/**
	*  Method to take a date, and return a Julian day. A Julian day is the number of
	*  days since some base date  (in the very distant past).
	*  Handy for getting date of x number of days after a given Julian date
	*  (use jdate to get that from the Gregorian date).
	*  Author: Robert G. Tantzen, translator: Nat Howard
	*  Translated from the algol original in Collected Algorithms of CACM
	*  (This and jdate are algorithm 199).
	* @return Julian date as integer.
	*/

	public int getIntJulian ()
	{

		long m = (long)(get (Calendar.MONTH) + 1);
		long d = (long)(get (Calendar.DAY_OF_MONTH));
		long y = (long)(get (Calendar.YEAR));
		long c, ya, j;
		int ijd;

		double seconds = (double)(get (Calendar.HOUR_OF_DAY) * 3600.0)
		               + (double)(get (Calendar.MINUTE) * 60)
		               + (double)(get (Calendar.SECOND))
		               + (double)(get (Calendar.MILLISECOND) * 0.001);

		if (m > 2) m -= 3;
		else
		{
		   m += 9;
		   --y;
		}

		c = y / 100L;
		ya = y - (100L * c);
		j	= (146097L * c) / 4L + (1461L * ya) / 4L + (153L * m + 2L)
			/ 5L + d + 1721119L;
		if (seconds < 12 * 3600.0)
		{
		   j--;
		   seconds += 12.0 * 3600.0;
		}
		else
		{
		   seconds = seconds - 12.0 * 3600.0;
		}
		ijd = (int) j;

		return (ijd);
	}

	// static methods to do String to String manipulatations

	/**
	* yearMonDayToMonDayYear
	* @param stringDate is date portion of a date as "2002/01/12"
	*/

	public static String yearMonDayToMonDayYear (String stringDate)
	{
		String iString = null;
		iString	= stringDate.substring(5,7) + "-"		// month
				+ stringDate.substring(8,10) + "-"		// day
				+ stringDate.substring(0,4);			// year
		return iString;
	}

	/**
	* yearMonDayToMonDayYear
	* @param stringDate is date portion of a date as "01/12/2002"
	*/

	public static String monDayYearToYearMonDay (String stringDate)
	{
		String iString = null;
		iString	= stringDate.substring(6,10) + "-"		// year
				+ stringDate.substring(0,2) + "-"		// month
				+ stringDate.substring(3,5);			// day
		return iString;
	}

	/**
	* A method to test the class.
	* @param arg[] The list of the arguments for the unix date time constructor.
	*/

	public static void main (String arg[])
	{

		PrintWriter pw = null;
		try
		{
			pw = new PrintWriter(new FileWriter("ModelDateTime.log"));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("Unable to open log file.");
			System.exit(1);
		}

		System.out.println("Log is in file ModelDateTime.log");
		pw.println("ModelDateTime Test Run Log");
		pw.println("");
		try
		{

			ModelDateTime mdt = new ModelDateTime ();
			pw.println ("Current time");
			pw.println ("Julian = " + mdt.getJulian ());
			pw.println ("MMS datetime = " + mdt.getMmsDateTime ());
			pw.println ("SQL date = " + mdt.getSQLDate ());
			pw.println ("RiverWare datetime = " + mdt.getRiverWareDateTime ("1DAY"));
			pw.println ("Current TZ = " + (mdt.getTimeZone ()).getID ());
			pw.println ("Year is " + mdt.getYear());
			pw.println ("Days in month = " + mdt.daysInMonth());
			pw.println ("Days in year = " + mdt.daysInYear());
			if (mdt.isLeapYear(mdt.get(Calendar.YEAR)))
				pw.println ("This is a leap year");
			else
				pw.println ("This is not a leap year");

			mdt = new ModelDateTime (2440000.0);
			pw.println ("\nSet JD = 2440000.0");
			pw.println ("Julian = " + mdt.getJulian ());
			pw.println ("MMS datetime = " + mdt.getMmsDateTime ());
			pw.println ("SQL date = " + mdt.getSQLDate ());
			pw.println ("RiverWare datetime = " + mdt.getRiverWareDateTime ("1DAY"));
			pw.println ("Month is " + mdt.getMonth());
			pw.println ("Day is " + mdt.getDay());
			pw.println ("Hour is " + mdt.getHour());
			pw.println ("Minute is " + mdt.getMinute());
			pw.println ("Second is " + mdt.getSecond());
			pw.println ("JDBC date is " + mdt.getJDBCDate ().toString());

			mdt = new ModelDateTime (2000, 2, 23, 12, 0, 0);
			pw.println ("\nSet to 2000, 2 , 23, 12, 0, 0");
			pw.println ("Julian = " + mdt.getJulian ());
			pw.println ("MMS datetime = " + mdt.getMmsDateTime ());
			pw.println ("SQL date = " + mdt.getSQLDate ());
			pw.println ("RiverWare datetime = " + mdt.getRiverWareDateTime("1DAY"));
			pw.println ("Year is " + mdt.getYear());
			pw.println ("Days in month = " + mdt.daysInMonth());
			pw.println ("Days in year = " + mdt.daysInYear());
			if (mdt.isLeapYear(mdt.get(Calendar.YEAR)))
				pw.println ("This is a leap year");
			else
				pw.println ("This is not a leap year");

			mdt = new ModelDateTime ("1999-01-18");
			pw.println ("\nSet DMI  date 1999-01-18");
			pw.println ("Julian = " + mdt.getJulian ());
			pw.println ("MMS datetime = " + mdt.getMmsDateTime ());
			pw.println ("SQL date = " + mdt.getSQLDate ());
			pw.println ("Daily RiverWare datetime = " + mdt.getRiverWareDateTime ("1DAY"));
			pw.println ("Monthly RiverWare datetime = " + mdt.getRiverWareDateTime ("1MONTH"));
			mdt = new ModelDateTime (1, "1MONTH","1976-01-31 24:00");
			pw.println ("\n Set Monthly RiverWare datetime 1976-01-31 24:00");
			pw.println ("RiverWare datetime returned is " + mdt.getRiverWareDateTime("1MONTH"));
			pw.println ("Julian = " + mdt.getJulian ());
			pw.println ("MMS datetime = " + mdt.getMmsDateTime ());
			pw.println ("SQL date = " + mdt.getSQLDate ());
			pw.println ("First RiverWare datetime = " + mdt.getRiverWareDateTime ("1MONTH"));
			ModelDateTime mdt3 = mdt.advanceNMonths(2);
			pw.println ("Third RiverWare datetime = " + mdt3.getRiverWareDateTime ("1MONTH"));
			pw.println ("Months between = " + mdt.monthsBetween(mdt3));
			ModelDateTime mdt4 = mdt.advanceNMonths(0);
			pw.println ("Fourth RiverWare datetime = " + mdt4.getRiverWareDateTime ("1MONTH"));
			mdt = new ModelDateTime (3, "1DAY","31Jan1976 24:00");
			pw.println ("\nSet Daily DSS datetime 31Jan1976 24:00");
			pw.println ("RiverWare datetime = " + mdt.getRiverWareDateTime("1MONTH"));
			pw.println ("MMS datetime = " + mdt.getMmsDateTime ());
			pw.println ("SQL datetime = " + mdt.getSQLDateTime ());

			mdt = new ModelDateTime (4, "1DAY","01/31/1976 24:00");
			pw.println ("\nSet Daily DSS datetime 01/31/1976 24:00");
			pw.println ("RiverWare datetime = " + mdt.getRiverWareDateTime("1MONTH"));
			pw.println ("MMS datetime = " + mdt.getMmsDateTime ());
			pw.println ("SQL datetime = " + mdt.getSQLDateTime ());

			mdt = new ModelDateTime (5, "1DAY","1976-01-31 24:00");
			pw.println ("\nSet Daily DSS datetime 1976-01-31 24:00");
			pw.println ("RiverWare datetime = " + mdt.getRiverWareDateTime("1MONTH"));
			pw.println ("MMS datetime = " + mdt.getMmsDateTime ());
			pw.println ("SQL datetime = " + mdt.getSQLDateTime ());

			mdt = new ModelDateTime (1, "1MINUTE", "1968-05-23 12:01:05");
			pw.println ("\nSet RiverWare datetime with seconds 1968-05-23 12:01:05");
			pw.println ("Julian = " + mdt.getJulian ());
			pw.println ("MMS datetime = " + mdt.getMmsDateTime ());
			pw.println ("SQL date = " + mdt.getSQLDate ());
			pw.println ("RiverWare minute datetime = " + mdt.getRiverWareDateTime ("1MINUTE"));

			mdt = new ModelDateTime (1, "1DAY", "1968-05-23 12:01:05");
			pw.println ("RiverWare daily datetime = " + mdt.getRiverWareDateTime ("1DAY"));
			pw.println ("Year is " + mdt.getYear());
			if (mdt.isLeapYear(mdt.get(Calendar.YEAR)))
				pw.println ("This is a leap year");
			else
				pw.println ("This is not a leap year");
			pw.println ("Month is " + mdt.getMonth());
			pw.println ("Day is " + mdt.getDay());
			pw.println ("Hour is " + mdt.getHour());
			pw.println ("Minute is " + mdt.getMinute());
			pw.println ("Second is " + mdt.getSecond());
			pw.println ("JDBC date is " + mdt.getJDBCDate ().toString());
			mdt = new ModelDateTime (2, "1MONTH", "1968-05-23 12:01:05");
			pw.println ("HDB monthly datetime = " + mdt.getHDBDateTime ("1MONTH"));
			pw.println("Mon Day Year Style 1 = " + mdt.getMonDayYear(1));
			pw.println("Mon Day Year Style 2 = " + mdt.getMonDayYear(2));
			pw.println("Mon Day Year Style 3 = " + mdt.getMonDayYear(3));

			mdt = new ModelDateTime (1975, 12, 29, 0, 0, 0);
			pw.println ("\nStart date is " + mdt.getSQLDateTime());
			mdt.add(Calendar.DAY_OF_MONTH,1);
			pw.println ("Next day by adding is " + mdt.getSQLDateTime());
			mdt.roll(Calendar.DAY_OF_MONTH,true);
			pw.println ("Next day by rolling is " + mdt.getSQLDateTime());
			mdt.add(Calendar.MONTH,1);
			pw.println ("Next month by adding is " + mdt.getSQLDateTime());
			mdt.roll(Calendar.MONTH,true);
			pw.println ("Next month by rolling is " + mdt.getSQLDateTime());
			ModelDateTime mdt1 = new ModelDateTime (1968, 5, 24, 12, 0, 0);

			pw.println ();
			if (mdt.before (mdt1))
			{
				pw.println (mdt.getSQLDateTime () + " is before "
								+ mdt1.getSQLDateTime ());
			}

			if (mdt.after (mdt1))
			{
				pw.println (mdt.getSQLDateTime () + " is after "
								+ mdt1.getSQLDateTime ());
			}

			if (mdt.equals (mdt1))
			{
				   pw.println (mdt.getSQLDateTime () + " is after "
					+ mdt1.getSQLDateTime ());
			}

			String mmsDT = "1999 01 31 12 00 00.00";
			pw.println("\nInput MMS datetime string is " + mmsDT);
			mdt = new ModelDateTime (mmsDT);
			pw.println ("MMS datetime = " + mdt.getMmsDateTime ());
			pw.println ("Tab datetime = " + mdt.getTabDateTime ());
			pw.println ("SQL datetime = " + mdt.getSQLDateTime ());
			pw.println ("Oracle datetime = " + mdt.getOracleDateTime ());

			pw.println("\nYou sent me " + arg.length + " arguments.");
			if (arg.length == 6)
			{

				// test unix date time constructor

				mdt = new ModelDateTime (arg[0], arg[1], arg[2], arg[3], arg[4], arg[5]);
				pw.println ("\nUnix datetime = " + mdt.getSQLDateTime ());
			}
		}
		catch (SetModelDateTimeException e)
		{
			System.out.println("SetModelDateTimeException found.");
			e.printStackTrace();
		}
		catch (Exception e)
		{
			System.out.println("Exception found.");
			e.printStackTrace();
		}
		pw.close();

	}  // end of main

}	// end of class
