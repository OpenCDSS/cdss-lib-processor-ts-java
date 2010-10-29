// class of data to data and model to model maps
//package java_lib.dmiLib;
//Provided by Dave King, Reclamation
package rti.tscommandprocessor.commands.reclamationhdb.java_lib.dmiLib;

// import classes stored in libraries

/**
* A class of data to data and model to model maps
*/

public class RefDmiModelMap extends Object

{
	// Public Variables

	public String objectSlot;
	public String object_name;
	public String slot_name;
	public int datatype_id;
	public int site_id;
	public int siteNumber;
	public String stationName;			// doubles as DSS Part B
	public String parameter;			// doubles as DSS Part C
	public int paramNumber;
	public int site_datatype_id;		// primary site datatype id or other unique identifier
	public int siteDatatypeId;			// secondary site datatype id for rdb to rdb mapping
	public String description;			// optional

	/**
	* HDB to Model type A and RDB to Model type Z constructor (typical hdb RiverWare mapping) as:
	* objectSlot, object_name, slot_name, site_id, datatype_id, site_datatype_id, description
	* String, String, String, int, int, int, String
	*/

	public RefDmiModelMap(String os, String o, String s, int sid, int dtid, int sdtid, String d)
	{
		site_id = sid;
		datatype_id = dtid;
		site_datatype_id = sdtid;
		siteDatatypeId = 0;
		objectSlot = os;
		object_name = o;
		slot_name = s;
		stationName = null;
		parameter = null;
		siteNumber = 0;
		paramNumber = 0;
		description = d;
	}

	/**
	* Model to Model type B Constructor (typical Ascii Ascii mapping) as:
	* object_name, slot_name, stationName, parameter, description
	* String, String, String, String, String
	*/

	public RefDmiModelMap(String os, String o, String s, String sn, String p, String d)
	{
		site_id = 0;
		datatype_id = 0;
		site_datatype_id = 0;
		siteDatatypeId = 0;
		objectSlot = os;
		object_name = o;
		slot_name = s;
		stationName = sn;
		parameter = p;
		siteNumber = 0;
		paramNumber = 0;
		description = d;
	}

	/**
	* HDB to Model type C  and RDB to Model type Y constructor (typical db Sams map) as:
	* siteNumber, stationName, site_id, datatype_id, site_datatype_id, description
	* int, String, int, int, int, String
	*/

	public RefDmiModelMap(int sn, String ns, int sid, int dtid, int sdtid, String d)
	{
		site_id = sid;
		datatype_id = dtid;
		site_datatype_id = sdtid;
		siteDatatypeId = 0;
		objectSlot = null;
		object_name = null;
		slot_name = null;
		stationName = ns;
		parameter = null;
		siteNumber = sn;
		paramNumber = 0;
		description = d;
	}

	/**
	* Model to Model type D Constructor (Sams RiverWare DMI's) as:
	* object_name, slot_name, siteNumber, stationName, description
	* String, String, int, int, String
	*/

	public RefDmiModelMap(String os, String o, String s, int sn, String ns, String d)
	{
		site_id = 0;
		datatype_id = 0;
		site_datatype_id = 0;
		siteDatatypeId = 0;
		objectSlot = os;
		object_name = o;
		slot_name = s;
		stationName = ns;
		parameter = null;
		siteNumber = sn;
		paramNumber = 0;
		description = d;
	}

	/**
	* HDB to Model type E  and RDB to Model type X constructor (not presently used) as:
	* siteNumber, paramNumber, site_id, datatype_id, site_datatype_id, description
	* int, int, int, int, String
	*/

	public RefDmiModelMap(int sn, int pn, int sid, int dtid, int sdtid, String d)
	{
		site_id = sid;
		datatype_id = dtid;
		site_datatype_id = sdtid;
		siteDatatypeId = 0;
		objectSlot = null;
		object_name = null;
		slot_name = null;
		stationName = null;
		parameter = null;
		siteNumber = sn;
		paramNumber = pn;
		description = d;
	}

	/**
	* Model to Model type F Constructor (not presently used) as:
	* object_name, slot_name, siteNumber, paramNumber, description
	* String, String, int, int, String
	*/

	public RefDmiModelMap(String o, String s, int sn, int pn, String d)
	{
		site_id = 0;
		datatype_id = 0;
		site_datatype_id = 0;
		siteDatatypeId = 0;
		objectSlot = null;
		object_name = o;
		slot_name = s;
		stationName = null;
		parameter = null;
		siteNumber = sn;
		paramNumber = pn;
		description = d;
	}

	/**
	* HDB to Model type G and RDB to Model type W constructor (not presently used) as:
	* site_id, datatype_id, siteNumber, paramNumber, description
	* int, int, int, int, String
	*/

	public RefDmiModelMap(int sid, int did, int sn, int pn, String d)
	{
		site_id = sid;
		datatype_id = did;
		site_datatype_id = 0;
		siteDatatypeId = 0;
		objectSlot = null;
		object_name = null;
		slot_name = null;
		stationName = null;
		parameter = null;
		siteNumber = sn;
		paramNumber = pn;
		description = d;
	}

	/**
	* HDB to Model type H  and RDB to Model type V constructor as:
	* object_name, slot_name, site_id, datatype_id, site_datatype_id, description
	* String, String, int, int, int, String
	*/

	public RefDmiModelMap(String o, String s, int sid, int dtid, int sdtid, String d)
	{
		site_id = sid;
		datatype_id = dtid;
		site_datatype_id = sdtid;
		siteDatatypeId = 0;
		objectSlot = null;
		object_name = o;
		slot_name = s;
		stationName = null;
		parameter = null;
		siteNumber = 0;
		paramNumber = 0;
		description = d;
	}

	/**
	* HDB to Model type I  and RDB to Model type U constructor (typical HDB Ascii map) as:
	* site_id, datatype_id, site_datatype_id, stationName, parameter, description
	* int, int, int, String, String, String
	*/

	public RefDmiModelMap(int sid, int dtid, int sdtid, String s, String p, String d)
	{
		site_id = sid;
		datatype_id = dtid;
		site_datatype_id = sdtid;
		siteDatatypeId = 0;
		objectSlot = null;
		object_name = null;
		slot_name = null;
		stationName = s;
		parameter = p;
		siteNumber = 0;
		paramNumber = 0;
		description = d;
	}

	/**
	* HDB to Model type J and RDB to Model type T constructor (typical RDB Ascii map) as:
	* site_datatype_id, stationName, parameter, description
	* int, String, String, String
	*/

	public RefDmiModelMap(int sdtid, String s, String p, String d)
	{
		site_id = 0;
		datatype_id = 0;
		site_datatype_id = sdtid;
		siteDatatypeId = 0;
		objectSlot = null;
		object_name = null;
		slot_name = null;
		stationName = s;
		parameter = p;
		siteNumber = 0;
		paramNumber = 0;
		description = d;
	}

	/**
	* HDB to RDB type K and RDB to RDB type S constructor (typical HDB to RDB map) as:
	* site_datatype_id, siteDatatypeId, description
	* int, int, String
	*/

	public RefDmiModelMap(int s1, int s2, String d)
	{
		site_id = 0;
		datatype_id = 0;
		site_datatype_id = s1;
		siteDatatypeId = s2;
		objectSlot = null;
		object_name = null;
		slot_name = null;
		stationName = null;
		parameter = null;
		siteNumber = 0;
		paramNumber = 0;
		description = d;
	}

	/**
	* HDB to Model type L and RDB to Model type R constructor (typical rdb RiverWare mapping) as:
	* objectSlot, object_name, slot_name, site_datatype_id, description
	* String, String, String, int, string
	*/

	public RefDmiModelMap(String os, String o, String s, int sdtid, String d)
	{
		site_id = 0;
		datatype_id = 0;
		site_datatype_id = sdtid;
		siteDatatypeId = 0;
		objectSlot = os;
		object_name = o;
		slot_name = s;
		stationName = null;
		parameter = null;
		siteNumber = 0;
		paramNumber = 0;
		description = d;
	}

}	// end class
