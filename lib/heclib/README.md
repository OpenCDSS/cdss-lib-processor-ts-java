# heclib Library

This folder contains files needed to support the HEC-DSS binary file format,
used with Army Corps of Engineers software like HEC-RAS.
The files should be distributed with a runtime distribution by placing in the "bin" folder of the software.
The files are taken from the DSS-VUE software after installation.

## 64-bit Distribution

As of TSTool 14.4.0, the latest HEC-DSS 64-bit libraries are being implemented including
`hec-monolith-3.0.0.rc7.jar` taken from the HEC-DSSView 3.3.11 BETA for Windows
(`HEC-DSSView-win-3.3.8.Beta.zip`) file.
See the [HEC-DSSVue](https://www.hec.usace.army.mil/software/hec-dssvue/downloads.aspx) download page.
After working with the code the following are ultimately used:

* `hec-monolith-3.0.1-rc03.jar` - for most of the API
* `hec-nucleus-metadata-1.1.0.jar` - for the `VerticalDatumException` class

Also distribute the following in the TSTool bin folder:

* `javaHeclib.dll` - Java packages for accessing HEC-DSS files via native code

## Old 32-bit Distribution

Older TSTool versions that were 32-bit used the following files to integrate with HEC-DSS.
These files will be removed from the software files once the 64-bit version is tested.

heclib.jar - Java packages for accessing HEC-DSS files via native code
javaHeclib.dll - native interface library
DFORMDD.DLL - FORTRAN library that links FORTRAN to C/C++ libraries
MSVCRTD.DLL - Microsoft Visual C++ runtime, debug version (many systems do not
              have this debug version installed by default so distribute it with
              TSTool)

The above is the minimum distribution needed.  This may change later if using
the higher-level Java packages distributed by HEC, developed for Java 6.
