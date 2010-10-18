package rti.tscommandprocessor.commands.bndss;

import RTi.DMI.DMIDataObject;
import RTi.DMI.DMIUtil;

/**
Class to store data from the DataMetaData views
*/
public class BNDSS_DataMetaData extends DMIDataObject
{

/**
Primary key for for time series metadata record.
*/
protected long _id = DMIUtil.MISSING_LONG;
/**
Primary key for for related subject record.
*/
protected long _subjectID = DMIUtil.MISSING_LONG;
/**
Subject type.
*/
protected String _subject = DMIUtil.MISSING_STRING;
/**
Name of the subject.
*/
protected String _name = DMIUtil.MISSING_STRING;
/**
Data type.
*/
protected String _dataType = DMIUtil.MISSING_STRING;
/**
Data sub-type.
*/
protected String _subType = DMIUtil.MISSING_STRING;
/**
Data units.
*/
protected String _units = DMIUtil.MISSING_STRING;
/**
Data method.
*/
protected String _method = DMIUtil.MISSING_STRING;
/**
Data subMethod.
*/
protected String _subMethod = DMIUtil.MISSING_STRING;
/**
Data source.
*/
protected String _source = DMIUtil.MISSING_STRING;
/**
Data scenario.
*/
protected String _scenario = DMIUtil.MISSING_STRING;

/**
Copy constructor.
*/
public BNDSS_DataMetaData(BNDSS_DataMetaData m) {
	super();
	setName(new String(m.getName()));
	setDataType(new String(m.getDataType()));
	setSubType(new String(m.getSubType()));
	setMethod(new String(m.getMethod()));
	setSubMethod(new String(m.getSubMethod()));
	setSource(new String(m.getSource()));
	setScenario(new String(m.getScenario()));
	setDirty(m.isDirty());
}

/**
Constructor.  
*/
public BNDSS_DataMetaData()
{	super();
}

/**
Cleans up variables when the class is disposed of.  Sets all the member
variables (that aren't primitives) to null.
@exception Throwable if there is an error.
*/
protected void finalize()
throws Throwable
{
    super.finalize();
}

/**
Returns _dataType
@return _dataType
*/
public String getDataType() {
    return _dataType;
}

/**
Returns _id
@return _id
*/
public long getID() {
    return _id;
}

/**
Returns _method
@return _method
*/
public String getMethod() {
    return _method;
}

/**
Returns _name
@return _name
*/
public String getName() {
	return _name;
}

/**
Returns _scenario
@return _scenario
*/
public String getScenario() {
    return _scenario;
}

/**
Returns _source
@return _source
*/
public String getSource() {
    return _source;
}

/**
Returns _subject
@return _subject
*/
public String getSubject() {
    return _subject;
}

/**
Returns _subjectID
@return _subjectID
*/
public long getSubjectID() {
	return _subjectID;
}

/**
Returns _subMethod
@return _subMethod
*/
public String getSubMethod() {
    return _subMethod;
}

/**
Returns _subType
@return _subType
*/
public String getSubType() {
    return _subType;
}

/**
Returns _units
@return _units
*/
public String getUnits() {
    return _units;
}

/**
Sets _dataType
@param dataType value to put in _dataType
*/
public void setDataType(String dataType) {
	if ( dataType != null ) {
		_dataType = dataType;
	}
}

/**
Sets _id
@param id values to put in _id
*/
public void setID(long id) {
    _id = id;
}

/**
Sets _method
@param scenario value to put in _scenario
*/
public void setMethod(String method) {
    if ( method != null ) {
        _method = method;
    }
}

/**
Sets _scenario
@param scenario value to put in _scenario
*/
public void setName(String name) {
    if ( name != null ) {
        _name = name;
    }
}

/**
Sets _scenario
@param scenario value to put in _scenario
*/
public void setScenario(String scenario) {
    if ( scenario != null ) {
        _scenario = scenario;
    }
}

/**
Sets _source
@param source value to put in _source
*/
public void setSource(String source) {
    if ( source != null ) {
        _source = source;
    }
}

/**
Sets _subject
@param subject value to put in _subject
*/
public void setSubject(String subject) {
    if ( subject != null ) {
        _subject = subject;
    }
}

/**
Sets _subjectID
@param subjectID values to put in _subjectID
*/
public void setSubjectID(long subjectID) {
    _subjectID = subjectID;
}

/**
Sets _subMethod
@param subMethod value to put in _subMethod
*/
public void setSubMethod(String subMethod) {
    if ( subMethod != null ) {
        _subMethod = subMethod;
    }
}

/**
Sets _subType
@param subType value to put in _subType
*/
public void setSubType(String subType) {
	if ( subType != null ) {
		_subType = subType;
	}
}

/**
Sets _units
@param units value to put in _units
*/
public void setUnits(String units) {
    if ( units != null ) {
        _units = units;
    }
}

/** 
Returns a string representation of this object
@return a string representation of this object
*/
public String toString() {
    /*
	return  "RiversideDB_MeasType{"			+ " \n" +
		"MeasType_num:      "+ _MeasType_num	+ " \n" +
		"MeasLoc_name:      "+ _MeasLoc_num	+ " \n" +
		"Data_type:        '"+ _Data_type	+ "'\n" +
		"Sub_type:         '"+ _Sub_type	+ "'\n" +
		"Time_step_base:   '"+ _Time_step_base	+ "'\n" + 
		"Time_step_mult:    "+ _Time_step_mult	+ " \n" +
		"Source_abbrev:    '"+ _Source_abbrev	+ "'\n" + 
		"Scenario:         '"+ _Scenario	+ "'\n" +
		"Table_num1:        "+ _Table_num1	+ " \n" + 
		"Dbload_method1:    "+ _Dbload_method1	+ " \n" + 
		"Table_num2:        "+ _Table_num2	+ " \n" +
		"Dbload_method2:    "+ _Dbload_method2	+ " \n" +
		"Description:      '"+ _Description	+ "'\n" +
		"Units_abbrev:     '"+ _Units_abbrev	+ "'\n" + 
		"Create_method:    '"+ _Create_method	+ "'\n" +
		"TransmitProtocol: '"+ _TransmitProtocol+ "'\n" +
		"Status:           '"+ _Status		+ "'\n" +
		"Min_check:         "+ _Min_check	+ " \n" + 
		"Max_check:         "+ _Max_check	+ " \n" +
		"Identifier:       '"+ _Identifier	+ "'\n" + //From MeasLoc
		"MeasLoc_name:     '"+ _MeasLoc_name	+ "'\n" + //From MeasLoc
		"Editable:         '"+ _Editable 	+ "'\n" + //pre 03.00.00
		"IsEditable:       '"+ _IsEditable 	+ "'\n" + //    03.00.00
		"IsVisible:        '"+ _IsVisible 	+ "'\n" +
		"DBUser_num:        "+ _DBUser_num	+ " \n" + 
		"DBGroup_num:       "+ _DBGroup_num	+ " \n" +
		"DBPermissions:    '"+ _DBPermissions	+ " \n" +
		"TS_DBUser_num:     "+ _TS_DBUser_num	+ " \n" + 
		"TS_DBGroup_num:    "+ _TS_DBGroup_num	+ " \n" +
		"TS_DBPermissions: '"+ _TS_DBPermissions+ "}";
		*/
    return super.toString();
}

/**
Create and return a TSIdent instance for the MeasType.
@return a TSIdent instance for the MeasType.
*/
/*
public TSIdent toTSIdent()
throws Exception {
	String data_type = _Data_type;
	if ( !_Sub_type.equals("") ) {
		data_type = _Data_type + "-" + _Sub_type;
	}
	String timestep = _Time_step_base;
	if ( !DMIUtil.isMissing(_Time_step_mult) ) {
		timestep = "" + _Time_step_mult + _Time_step_base;
	}
	return new TSIdent ( _Identifier, _Source_abbrev, data_type,
		timestep, _Scenario );
}
*/

}