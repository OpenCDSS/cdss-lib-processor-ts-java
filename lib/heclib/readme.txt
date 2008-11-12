This folder contains files needed to support the HEC-DSS binary file format,
used with Army Corps of Engineers software like HEC-RAS.  The files should be
distributed with a runtime distribution by placing in the "bin" folder of
the software.  The files are taken from the DSS-VUE software after installation.

heclib.jar - Java packages for accessing HEC-DSS files via native code
javaHeclib.dll - native interface library
DFORMDD.DLL - FORTRAN library that links FORTRAN to C/C++ libraries
MSVCRTD.DLL - Microsoft Visual C++ runtime, debug version (many systems do not
              have this debug version installed by default so distribute it with
              TSTool)

The above is the minimum distribution needed.  This may change later if using
the higher-level Java packages distributed by HEC, developed for Java 6.
