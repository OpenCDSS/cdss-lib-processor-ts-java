The WaterML20.jar file is from an Open Water Foundation project that created the Java API from WaterML 2.0 XML schema.
See the OWF GitHub projects for WaterML.

Although the built-in JAXB functionality could be used for XML read/write, the Xerces package seems to be used because
it is in the classpath from other packages.  This seems to work OK.