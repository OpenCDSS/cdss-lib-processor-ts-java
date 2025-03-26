# Jython #

This folder contains the library `jar` files needed to use the Jython library,
which allows running Python scripts from Java.

The `fix-jython.bash` script removes the `org.w3c.dom` classes from the `jython.jar` file and repackages.
This is necessary because the classes are redundant with the built-in classes provided by the JDK/JRE.
The original and modified `jar` files are kept in the repository but only the modified file is used in Eclipse and the distribution.
This should be fine unless the design of the classes is different.
