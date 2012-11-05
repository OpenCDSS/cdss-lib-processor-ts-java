package rti.tscommandprocessor.commands.nrcs.awdb;

/**
Element code information to allow lookup/translation of element code and description.
This information is not currently available from a web service request but might be in the future.
*/
public class NrcsAwdbElementCode
{
    
/**
Element code.
*/
private String __code;

/**
Element units.
*/
private String __units = "";

/**
Element name.
*/
private String __name = "";
   
/**
Constructor.
@param code site type code
@param name site type description
@param units units for element
*/
public NrcsAwdbElementCode ( String code, String name, String units )
{
    __code = code;
    __name = name;
    __units = units;
}

/**
Return the element code.
*/
public String getCode()
{
    return __code;
}

/**
Return the element code name
*/
public String getName()
{
    return __name;
}

/**
Return the element units
*/
public String getUnits()
{
    return __units;
}

}