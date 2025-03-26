# apache-poi #

This folder contains the library `jar` files needed to use the Apache POI library,
which allows reading and writing Excel files.

The `fix-xmlbeans.bash` script removes the `org.w3c.dom` classes from the `xmlbeans-2.3.0.jar` file and repackages.
This is necessary because the classes are redundant with the built-in classes provided by the JDK/JRE.
The original and modified `jar` files are kept in the repository but only the modified file is used in Eclipse and the distribution.
This should be fine unless the design of the classes is different.
