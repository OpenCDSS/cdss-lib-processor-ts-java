# cdss-lib-processor-ts-java #

This repository contains
[Colorado's Decision Support Systems (CDSS)](https://www.colorado.gov/cdss))
Java library for time series processing, used by TSTool.
The library has been enhanced over time to include additional capabilities,
including processing tables, Excel spreadsheets, etc.,
with functionality included in TSTool commands.
In addition to code that has been written for the library,
the library also contains the following major components that are auto-generated:

* `src/gov/usda/nrcs/` - auto-generated code for NRCS AWDB SOAP web services
* `src/org/cuahsi/` - auto-generated code for CUAHSI WaterML SOAP web services

Most datastores and commands are currently "built in",
meaning that they are hard-linked in the development environment and run-time
environment so that no errors exist.
However, in the future, commands that are not part of the core TSTool
functionality, such as third-party database support,
may be moved to plugin commands that load dynamically so that maintenance on those tools
can more easily occur by contributors.

Commands that are used in
[StateDMI](https://github.com/OpenCDSS/cdss-apb-statedmi-main)
are copied to StateDMI because of
technical issues in using this library (mainly dependencies that are not
relevant to StateDMI and would bloat its code size).
Use of the library without copying command code may be implemented in the future
when some libraries are converted to plugins and thereby minimize core
built-in command dependencies.

Eclipse is used for development and repositories currently contain Eclipse project files to facilitate
setting up the Eclipse development environment.
Development on library code usually occurs while developing an application such as TSTool,
rather than the library alone, because software requirements come from application development.

See the following online resources:

* [Colorado's Decision Support Systems](https://www.colorado.gov/cdss)
* [OpenCDSS](http://learn.openwaterfoundation.org/cdss-website-opencdss/) - currently
hosted on the Open Water Foundation website while OpenCDSS server is being configured
* [TSTool Developer Documentation](http://learn.openwaterfoundation.org/cdss-app-tstool-doc-dev/) - primary application that uses this library

See the following sections in this page:

* [Repository Folder Structure](#repository-folder-structure)
* [Repository Dependencies](#repository-dependencies)
* [Development Environment Folder Structure](#development-environment-folder-structure)
* [Version](#version)
* [Contributing](#contributing)
* [License](#license)
* [Contact](#contact)

--------

## Repository Folder Structure ##

The following are the main folders and files in this repository, listed alphabetically.
See also the [Development Environment Folder Structure](#development-environment-folder-structure)
for overall folder structure recommendations.

```
cdss-lib-processor-ts-java/        Source code and development working files.
  .classpath                       Eclipse configuration file.
  .git/                            Git repository folder (DO NOT MODIFY THIS except with Git tools).
  .gitattributes                   Git configuration file for repository.
  .gitignore                       Git configuration file for repository.
  .project                         Eclipse configuration file.
  bin/                             Eclipse folder for compiled files (dynamic so ignored from repo).
  conf/                            Configuration files for installer build tools.
  dist/                            Folder used to build distributable installer (ignored from repo).
  doc/                             Working folder for Javadoc (contents ignored from repo).
  graphics/                        Graphics used by software.
  lib/                             Third-party libraries.
  LICENSE.md                       Library license file.
  nbproject/                       NetBeans project files (may be removed in the future).
  README.md                        This file.
  resources/                       Additional resources.
  scripts/                         Utility scripts (may be moved/removed).
  src/                             Source code.
  test/                            Unit tests using JUnit.
```

## Repository Dependencies ##

Repository dependencies fall into two categories as indicated below.

### Repository Dependencies for this Repository ###

This library depends on other repositories listed in the following table.
The repository contains Eclipse configuration files that describe the dependencies so that
this repository and others can be cloned when setting up a development environment,
with minimal additional configuration (for example see TSTool Developer documentation).

|**Repository**|**Description**|
|-------------------------------------------------------------------------------------------------------------|----------------------------------------------------|
|[`cdss-lib-cdss-java`](https://github.com/OpenCDSS/cdss-lib-cdss-java)                                       |Library for shared CDSS code.|
|[`cdss-lib-common-java`](https://github.com/OpenCDSS/cdss-lib-common-java)                                   |Library of core utility code used by multiple repos.|
|[`cdss-lib-dmi-hydrobase-java`](https://github.com/OpenCDSS/cdss-lib-dmi-hydrobase-java)                     |Library to directly access Colorado's HydroBase database.|
|[`cdss-lib-dmi-hydrobase-rest-java`](https://github.com/OpenCDSS/cdss-lib-dmi-hydrobase-rest-java)           |Library to access Colorado's HydroBase REST API.|
|[`cdss-lib-dmi-nwsrfs-java`](https://github.com/OpenCDSS/cdss-lib-dmi-nwsrfs-java)                           |Legacy library to access National Weather Service River Forecast System (NWSRFS) data files.|
|[`cdss-lib-dmi-riversidedb-java`](https://github.com/OpenCDSS/cdss-lib-dmi-riversidedb-java)                 |Legacy library to access Riverside Technology forecast system database.|
|[`cdss-lib-dmi-satmonsys-java`](https://github.com/OpenCDSS/cdss-lib-dmi-satmonsys-java)                     |Legacy library to directly access Colorado's Satellite Monitoring System database.|
|[`cdss-lib-models-java`](https://github.com/OpenCDSS/cdss-lib-models-java)                                   |Library to read/write CDSS StateCU and StateMod model files.|

### Repositories that Depend on this Repository ###

The following repositories are known to depend on this repository:

|**Repository**|**Description**|
|----------------------------------------------------------------------------------------------------------------|----------------------------------------------------|
|[`cdss-app-tstool-main`](https://github.com/OpenCDSS/cdss-app-tstool-main)                                      |Main TSTool application code.|

## Development Environment Folder Structure ##

The following folder structure is recommended for software development.
Top-level folders should be created as necessary.
Repositories are expected to be on the same folder level to allow cross-referencing
scripts in those repositories to work.
TSTool is used as an example, and normally use of this repository will occur
through development of the main CDSS applications.
See the application's developer documentation for more information.

```
C:\Users\user\                               Windows user home folder (typical development environment).
/home/user/                                  Linux user home folder (not tested).
/cygdrive/C/Users/user                       Cygdrive home folder (not tested).
  cdss-dev/                                  Projects that are part of Colorado's Decision Support Systems.
    TSTool/                                  TSTool product folder (will be similar for other applications).
      eclipse-workspace/                     Folder for Eclipse workspace, which references Git repository folders.
                                             The workspace folder is not maintained in Git.
      git-repos/                             Git repositories for TSTool.
        cdss-lib-processor-ts-java/          This repository.
        ...others...                         See application developer documenation and README for full list.

```

## Version ##

A version for the library is typically not defined.
Instead, tags are used to cross-reference the library version with commit of application code such as TSTool.
This allows checking out a version of the library consistent with an application version.
This approach might need to change if the library is seen as an independent resource that is used by many third-party applications.

## Contributing ##

Contributions to this project can be submitted using the following options:

1. Software developers with commit privileges can write to this repository
as per normal OpenCDSS development protocols.
2. Post an issue on GitHub with suggested change.  Provide information using the issue template.
3. Fork the repository, make changes, and do a pull request.
Contents of the current master branch should be merged with the fork to minimize
code review before committing the pull request.

See also the [OpenCDSS / protocols](http://learn.openwaterfoundation.org/cdss-website-opencdss/)
for each software application.

## License ##

Copyright Colorado Department of Natural Resources.

The software is licensed under GPL v3+. See the [LICENSE.md](LICENSE.md) file.

## Contact ##

See the [OpenCDSS information](http://learn.openwaterfoundation.org/cdss-website-opencdss) for overall contacts and contacts for each software product.
