## Description

The Jenkins Automation Plugin for BTC EmbeddedPlatform provides easy to
use steps for Jenkins Pipeline enabling you to execute test workflow
steps from Jenkins. The BTC EmbeddedPlatform HTML reports become
available automatically on the Jenkins job page and a JUnit XML report
is generated that feeds into Jenkins test trends and status overview
dashboards.

The following workflow steps are available for Jenkins Pipeline:

-   Profile Load / Create / Update
-   Import of Test Cases and Stimuli Vectors
-   Import / Export of Tolerances and Input Restrictions
-   Execution of Functional Tests
-   Automatic Vector Generation & Analysis
-   Back-to-Back Test Execution
-   Regression Test Execution
-   Formal Verification
-   Formal Test
-   Configuration of Additional Coverage Goals
-   Export of different reports
-   BTC Migration Suite

For these workflow steps many additional settings can be configured
(optional) in order to adapt the automated test runs to your individual
project needs. In addition, two utility methods help you to connect to
instances of BTC EmbeddedPlatform and to close them if again required.
In addition, the Migration Suite use case can be addressed (see section
Migration Suite below).



**Table of Contents**

* [Description](#description)
* [Release Notes](#release-notes)
* [Prerequisites](#prerequisites)
* [Jenkins Pipeline](#jenkins-pipeline)
  - [Overview](#overview)
  - [Licensing for Jenkins Integration](#licensing-for-jenkins-integration)
  - [Configuration](#configuration)
* [Workflow Steps](#workflow-steps)
  - [Step "startup"](#step-startup)
  - [Step "profileLoad"](#step-profileload)
  - [Step "profileCreateTL"](#step-profilecreatetl)
  - [Step "profileCreateEC"](#step-profilecreateec)
  - [Step "profileCreateSL"](#step-profilecreatesl)
  - [Step "profileCreateC"](#step-profilecreatec)
  - [Step "vectorImport"](#step-vectorimport)
  - [Step "toleranceImport"](#step-toleranceimport)
  - [Step "toleranceExport"](#step-toleranceexport)
  - [Step "inputRestrictionsImport"](#step-inputrestrictionsimport)
  - [Step "executionRecordExport"](#step-executionrecordexport)
  - [Step "inputRestrictionsExport"](#step-inputrestrictionsexport)
  - [Step "rbtExecution"](#step-rbtexecution)
  - [Step "testExecutionReport"](#step-testexecutionreport)
  - [Step "xmlReport"](#step-xmlreport)
  - [Step "codeAnalysisReport"](#step-codeanalysisreport)
  - [Step "modelCoverageReport"](#step-modelcoveragereport)
  - [Step "formalTest"](#step-formaltest)
  - [Step "rangeViolationGoals"](#step-rangeviolationgoals)
  - [Step "domainCoverageGoals"](#step-domaincoveragegoals)
  - [Step "vectorGeneration"](#step-vectorgeneration)
  - [Step "backToBack"](#step-backtoback)
  - [Step "regressionTest"](#step-regressiontest)
  - [Step "formalVerification"](#step-formalverification)
  - [Step "wrapUp"](#step-wrapup)
* [BTC Migration Suite](#btc-migration-suite)
  - [Step "migrationSource"](#step-migrationsource)
  - [Step "migrationTarget"](#step-migrationtarget)
  - [Migration Suite Example: Jenkinsfile](#migration-suite-example-jenkinsfile)
* [Adding the BTC Plugin to Jenkins](#adding-the-btc-plugin-to-jenkins)

## Release Notes

Version | Release Notes | EP Version | Update BTC-part | Update Jenkins-part
--------|---------------|------------|-----------------|--------------------
2.5.10 | - Matlab log is now also available for c-code workflows that include matlab startupScripts | 2.5 | X | X 
2.5.8 | - Fixed: B2B Tests with status "FAILED_ACCPETED" will now yield the appropriate return code and will not be treated as failures in the JUnit report<br>- Added testsuite attribute "time" which carries the execution time of the test suites + the overall execution time<br>- Fixed an issue that caused execution records to be used twice in the migration suite context<br>- Ouput from the Matlab console will now be available in a log file that is archived automatically in the wrapUp step | 2.5 | X | X 
2.5.5 | - Profile Creation steps now add some more information to the overview report<br>- Fixed an issue that could occur with certain versions of the DomainCoverageGoals plugin<br>- The headless application now only spawns one tasks: ep.exe (down from two: ep & javaw) | 2.5 | X | X 
2.5.4 | - The Jenkins Automation plugin now writes its log messages to the default log file of BTC EmbeddedPlatform (usually found at %APPDATA%/BTC/ep/<version>/<port>/logs/current.log) | 2.5 | X | 
2.5.3 | - Added workaround that allows EmbeddedCoder architecture udpate in Jenkins scenarios that currently don't work out of the box. CodeModel and Mapping XML must be provided in the btc.profileLoad step. | 2.5 | X | 
2.5.2 | - Fixed: updating the default compiler in a model-based profile needed some additional efforts and could lead to problems before now. | 2.5 | X | 
2.4.20 | - Added method to retrieve coverage and test status data as a struct (see step getStatusSummary)<br>- Fixed engine selection for vectorGeneration and added scope selection | 2.4.1 | X | 
2.4.19 | - Fixed: updating the default compiler in a model-based profile needed some additional efforts and could lead to problems before now. | 2.4.1 | X | 
2.4.17 | - Available execution configs can now be queried for a profile allowing a generic way to handle steps on different kinds of existing profiles. | 2.4.1 | X | X
2.4.15 | - Fixed: model paths were only updated if updateRequired option was true. However, there are use cases where the model paths need to be updated even though updateRequired is false. | 2.4.1 | X | X
2.4.14 | - Fixed: model paths were only updated if updateRequired option was true. However, there are use cases where the model paths need to be updated even though updateRequired is false. | 2.4.1 | X | 
2.4.13 | - Fixed: robustnessTestFailure property of vectorGeneration step was not properly handled | 2.4.1 | X | 
2.4.12 | - Added option to control Matlab Instance Policy<br>- Fixed path conversion issues for Jenkins related archiving tasks | 2.4.1 | X | X
2.4.11 | - Added explicit step to create Test Execution Reports: btc.testExecutionReport<br>- The step btc.rbtExecution no longer creates Test Execution Reports by default but this can still be achieved by setting its new property "createReport" to true.<br>- Added robustness for duplicate separators when using absolute paths | 2.4.1 | | X
2.4.10 | - Adaptions for EP 2.4.1 | 2.4.1 |  |
2.4.1 | - Fixed a bug that caused Test Execution and Regression Test steps to return the wrong return code. | 2.4 | X | 
2.4.0 | - Support for EmbeddedTester BASE (see parameter "licensingPackage" in the "startup" step for more details). | 2.4 | X | X
2.3.0 | - Added filters for scopes, folders and testcase names. For each of the three a whitelist and blacklist can be defined as a comma separated string. | 2.4 | X | X
2.2.0 | - Added a step to generate and export a model coverage report (RBT & B2B). Requires the Simulink V&V / Simulink Coverage license.<br>- Test cases (so far only test cases, no SVs, ERs, etc.) that come from a different architecture can now be imported. An automatic scope-mapping is done using the scope name (first match). Smart mapping is only performed if the direct import (which is based on the full scope path) is not possible. | 2.4 | X | X
2.1.0 | - Adaptions for EP 2.4 | 2.4 | X | X 
2.0.11 | - Added the possibility to select requirement sources as the report source for test execution reports. | 2.3 |  | 
2.0.10 | - Fixed issue: Architecture Update doesn't work if model path changes (relative and absolute) | 2.3 |  | 
2.0.9 | - Added CalibrationHandling as a parameter for the TargetLin profile creation<br>- Added Export of Tolerances and Input Restrictions to support programmatic modification & re-import | 2.3 |  | 
2.0.8 | - Fixed a bug that prevented combined PLL strings of default and contributed coverage goals (e.g. Statements and Domain Coverage Goals together in one PLL String).<br>- Fixed the CSV vector import for test cases and stimuli vectors<br>- Fixed an issue with backslashes in absolute paths which point to a location inside the workspace<br>- For Domain Coverage and Range Violation goals users can now set a scopePath of "*" to consider all scopes. Not settings a scope path will still default to the toplevel scope.<br>- Added the parameter "useCase" for the Code Analysis Report Step (RBT / B2B)<br>- Removed obsolete parameter "inputRestrictions" from the Vector Generation step. Input Restrictions can be defined through their own step.<br>- Users can now select the use case for the codeAnalysisReport step (RBT / B2B)<br>- The codeAnalysisReport now exports a csv file ("BTCCoverageOverview_B2B.csv" / "BTCCoverageOverview_RBT.csv") to the report directory which contains overall coverage percentage (Statement, Decision and MCDC). This CSV file can be used by other Jenkins plugins to display the coverage. | 2.3 |  | 
2.0.7 | - Fixed missing parameters (reuseExistingCode, pilConfig, pilTimeout)<br>- Added parameters for environmentXmlPath (TL Profile Creation), testMode (grey box / black box) and startupScriptPath | 2.3 |  | 
2.0.3 | - Adapted to EP 2.3<br>- A Failed Profile Creation / Profile load will now always throw an exception to break the build<br>- In case of failures during profile creation profile messages will be exported and made available in Jenkins (if possible)<br>- Fixed a bug that prevented the use of some settings for the wrapUp step.<br>- Fixed a bug that caused a specified PLL to be ignored.<br>- Jenkins: HPI plugin ("btc-embeddedplatform-plugin") is now available in the official Jenkins plugin repository | 2.3 |  | 
2.0.1 | - The Vector Generation step now supports dummy toplevels. If the toplevel subsystem is a dummy scope (no C-function available) then the vector generation will be done on the direct children of the toplevel.<br>- Domain Coverage and Range Violation goals can now be added to the profile and considered during vector generation (requires additional plugins)<br>- Fixed a bug that caused execution records to not be available for debugging in the migration suite use case.<br>- archiveArtifacts and stash commands used by the btc-embeddedplatform plugin are now only called if needed and were changed to be more specific<br>- Fixed an issue that could cause existing profiles to be loaded in the migration suite scenario. From now on the migrationSuite will always create a new profile.<br>- Profile Creation can now be invoked explicitly.<br>- The Code Analysis Report can now be created explicitly. It was formerly created by the Vector Generation step. That's still possible but not enabled by default (controlled by property "createReport" in the btc.vectorGeneration step). | 2.2p2 |  | 

## Prerequisites

This plugin only works in combination with BTC EmbeddedPlatform which
needs to be installed and licensed separately.

![](https://wiki.jenkins.io/download/attachments/173703174/Jenkins-EP.png?version=1&modificationDate=1558695678000&api=v2)

## Jenkins Pipeline

### Overview

Integrating test runs with BTC EmbeddedPlatform in your Jenkins
workflows combines the automation and traceability concepts and results
in great benefits:

1.  The automated workflows scale for multiple components / projects
    with low configuration effort
2.  You are easily able to trace changes made to your system under test
    from the Source Code Management to the integrated product and
    recognize test failures early in the process
3.  The pipeline visualization intuitively shows how much time each
    phase of the testing process takes  
    ![](https://wiki.jenkins.io/download/attachments/173703174/StageView_Tracability.png?version=1&modificationDate=1558695687000&api=v2)
4.  The Jenkins Automation Plugin produces an XML report in the JUnit
    format that can be analyzed by Jenkins to provide test status trends
    over multiple executions and projects  
    ![](https://wiki.jenkins.io/download/attachments/173703174/JUnit-Test-Result-Trend.png?version=1&modificationDate=1558695680000&api=v2)
5.  Comprehensive HTML Reports from BTC EmbeddedPlatform are available
    directly from the Jenkins job page  
      

    Jenkins' Content Security Policy can prevent the reports from being
    displayed properly. See [Configuring Content Security
    Policy](https://wiki.jenkins.io/display/JENKINS/Configuring+Content+Security+Policy)
    for further details.

6.  Relevant artifacts like the test profile are accessible for easy
    debugging and analysis

    ![](https://wiki.jenkins.io/download/thumbnails/173703174/save-profile.png?version=1&modificationDate=1558695685000&api=v2)

### Licensing for Jenkins Integration

In addition to the basic license requirements that depend on the chosen
workflow steps which require EmbeddedTester or EmbeddedValidator the
Jenkins Automation use case requires the "Test Automation Server"
floating network license (ET\_AUTOMATION\_SERVER).

Since v2.4.0 it's possible to run ET\_BASE use cases (Architecture
Import, Test Case Import, Test Execution, Reporting) with an
EmbeddedTester BASE installation and the license
ET\_AUTOMATION\_SERVER\_BASE.

### Configuration

In a Jenkins Pipeline the configuration of a job can be defined as
simple groovy code which can be versioned alongside the main source
files of the component. A full documentation of the Jenkins Pipeline can
be found **[here](https://jenkins.io/doc/book/pipeline/)**. The
following example shows of how BTC EmbeddedPlatform can be automated
from Jenkins via the BTC DSL for Pipeline Plugin. The Plugin needs to be
installed in Jenkins and dedicated BTC methods to create a test
automation workflow.

**Pipeline Example**

``` groovy
node {
    // checkout changes from SCM
    checkout scm
 
    // start EmbeddedPlatform and connect to it
    btc.startup {}
 
    // load / create / update a profile
    btc.profileCreateTL {
        profilePath = "profile.epp"
        tlModelPath = "powerwindow_tl_v01.slx"
        tlScriptPath = "start.m"
        matlabVersion = "2017b"
    }
 
    // generate stimuli vectors
    btc.vectorGeneration {
        pll = "STM, D, MCDC"
        createReport = true
    }
 
    // execute back-to-back test MIL vs. SIL
    btc.backToBack {
        reference = "TL MIL"
        comparison = "SIL"
    }
 
    // close EmbeddedPlatform and store reports
    btc.wrapUp {}
}
```

## Workflow Steps

### Step "startup"

DSL Command: btc.startup {...}

**Description**

Method to connect to BTC EmbeddedPlatform with a specified port. If BTC
EmbeddedPlatform is not available it is started and the method waits
until it is available. The following optional settings are available:

Property | Description | Example Value(s)
---------|-------------|-----------------
port | Port used to connect to EmbeddedPlatform.<br>(default: 29267) | 1234, 29268, 8073
timeout | Timeout in seconds before the attempt to connect to EmbeddedPlatform is cancelled. This timeout should consider the worst case CPU & IO performance which influences the tool startup.<br>(default: 120) | 40, 60, 120
licensingPackage | Name of the licensing package to use, e.g. to use a EmbeddedTester BASE.<br>(default: ET_COMPLETE) | ET_BASE

**Possible Return values**

Return Value | Description
-----------------|----------------
200 | Started a new instance of BTC EmbeddedPlatform and successfully connected to it.
201 | Successfully connected to an already running instance of BTC EmbeddedPlatform.
400 | Timeout while connecting to BTC EmbeddedPlatform (either manually specified or 120 seconds).
500 | Unexpected Error

_Jenkins will always connect to the active version of EmbeddedPlatform
since many tasks will only work with the version that is integrated into
Matlab. Please ensure that the correct EP version is active by
choosing Activate BTC EmbeddedPlatform in your start menu for the
desired version and also ensure that the Jenkins Automation Plugin is
installed for this version of EmbeddedPlatform._

### Step "profileLoad"

DSL Command: btc.profileLoad {...}

**Description**

Opens the profile if the specified profile exists, otherwise creates a
new profile. A profile update is only performed if this is required.
Profile Creation requires either a TargetLink model or C-Code in
combination with a CodeModel.xml architecture description.

_The "profileLoad" step or any of the "profileCreate" steps are a
mandatory starting point for all automation workflows._

Property | Description | Example Value(s)
---------|-------------|-----------------
**profilePath** | Path of the profile. If it does not exist, it will be created. The path can be absolute or relative to the jenkins job's workspace.<br>**mandatory** | "profile.epp"
tlModelPath | Path of the TargetLink model. The path can be absolute or relative to the jenkins job's workspace. | "model.slx"
tlScriptPath | Path of the model init script. The path can be absolute or relative to the jenkins job's workspace. | "init.m"
tlSubsystem | Name of the Subsystem representing the TL top-level subsystem for the analysis. Note: Argument is mandatory if there is more than one top-level system in the model. | "Controller"
environmentXmlPath | Path to the XML file with additional include paths, etc.. The path can be absolute or relative to the jenkins job's workspace. | "Environment.xml"
startupScriptPath | Path to a Startup Script which can be used to initialize matlab (e.g. toolchain startup, etc.). The path can be absolute or relative to the jenkins job's workspace. | "startup_toolchain.m"
codeModelPath | Path of the hand code description file. The path can be absolute or relative to the jenkins job's workspace. | "CodeModel.xml"
compilerShortName | Short name of the compiler that should be used (C-Code Use Case). Fallback will be an already selected compiler or, if undefined, the first one that is found. | "MSSDK71", "MSVC140", "MinGW64"
slModelPath | Path of the Simulink model. The path can be absolute or relative to the jenkins job's workspace. | "slModel.slx"
slScriptPath | Path of the model init script for the Simulink model. The path can be absolute or relative to the jenkins job's workspace. | "init.m"
addModelInfoPath | Path to the XML file with additional model info for SL use case. The path can be absolute or relative to the jenkins job's workspace. | "AddGenModelInfo.xml"
pilConfig | Name of the PIL configuration to use. This config must exist in TargetLink. Setting a PIL Config will activate PIL in the profile and enable you to choose "PIL" as an execution config. | "default EVM"
pilTimeout | Timeout in seconds for the download process to the PIL board.<br>(default: 60) | 60, 120
calibrationHandling | The calibration handling controls how calibrations are recognized during architecture import.<br>(default: "EXPLICIT PARAM") | "EXPLICIT PARAM", "LIMITED BLOCKSET", "OFF"
testMode | The test mode controls whether local displayables will be available for testing (GREY BOX) or not (BLACK BOX). | "GREY BOX", "BLACK BOX"
reuseExistingCode | Boolean flag that controls if EmbeddedPlatform will use existing generated code from TargetLink. Requires the Code and the linking information in the data dictionary to be available.<br>(default: false) | true, false
matlabVersion | Controls which matlab version will be used by the tool.<br>String containing the release version (e.g. "2016b"), optionally followed by "32-bit" or "64-bit". The version and 32/64-bit part should be separated by a space character. | "2010a 32-bit"<br>"2013b"<br>"2016b 64-bit"
matlabInstancePolicy | String that controls when EmbeddedPlatform will start a new Matlab instance. When selecting "NEVER" another process needs to ensure that a Matlab instance is available on the agent machine.<br>Default: "AUTO" (i.e. a new instance is only started if no instance of the specified version is available) | "AUTO", "ALWAYS", "NEVER"
exportPath | Path to a folder where reports shall be stored. The path can be absolute or relative to the jenkins job's workspace. | "reports" (default)
updateRequired | Boolean flag that controls whether or not the profile is being update after loading.<br>(default: false) | true, false
saveProfileAfterEachStep | Boolean flag that controls whether or not the profile is being saved after each step.(default: false) | true, false
logFilePath | Path for the log file. The path can be absolute or relative to the jenkins job's workspace. | "log.txt" (default)
licenseLocationString | String containing the license locations in the order of their priority. Multiple locations are to be separated by a semicolon. If not specified explicitly, the license locations will still be retrieved from the registry (via FlexLM) in the way they have been configured in the EP license dialog. | "C:\Licenses\EP21_30.01.2019.lic"<br>"@192.168.0.1"<br>"9000@myserver.com"

**Possible Return values**

| Return Value | Description                                                                                                       |
|--------------|-------------------------------------------------------------------------------------------------------------------|
| 200          | Successfully loaded an existing profile.                                                                          |
| 201          | Successfully loaded an existing profile and performed an architecture update (see updateRequired property above). |
| 202          | Successfully created a new profile.                                                                               |
| 400          | Error during profile creation. Throws an exception because further testing is not possible.                       |
| 500          | Unexpected Error. Throws an exception because further testing is not possible.                                    |

### Step "profileCreateTL"

DSL Command: btc.profileCreateTL {...}

**Description**

Creates a new profile for a TargetLink model.

_Please note:_ The listed properties only show the TargetLink specific properties. Each
of the properties listed in the "profileLoad" step also apply here.

Property | Description | Example Value(s)
---------|-------------|-----------------
**profilePath** | Path of the profile. If it does not exist, it will be created. The path can be absolute or relative to the jenkins job's workspace.<br>**mandatory** | "profile.epp"
**tlModelPath** | Path of the TargetLink model. The path can be absolute or relative to the jenkins job's workspace.<br>**mandatory for TL use case** | "model.slx"
tlScriptPath | Path of the model init script. The path can be absolute or relative to the jenkins job's workspace. | "init.m"
tlSubsystem | Name of the Subsystem representing the TL top-level subsystem for the analysis. Note: Argument is mandatory if there is more than one top-level system in the model. | "Controller"
environmentXmlPath | Path to the XML file with additional include paths, etc.. The path can be absolute or relative to the jenkins job's workspace. | "Environment.xml"
reuseExistingCode | Boolean flag that controls if EmbeddedPlatform will use existing generated code from TargetLink. Requires the Code and the linking information in the data dictionary to be available.<br>(default: false) | true, false


### Step "profileCreateEC"

DSL Command: btc.profileCreateEC {...}

**Description**

Creates a new profile for an EmbeddedCoder model.

_Please note:_ The listed properties only show the EmbeddedCoder specific properties. Each
of the properties listed in the "profileLoad" step also apply here.

Property | Description | Example Value(s)
---------|-------------|-----------------
**profilePath** | Path of the profile. If it does not exist, it will be created. The path can be absolute or relative to the jenkins job's workspace.<br>**mandatory** | "profile.epp"
**slModelPath** | Path of the EmbeddedCoder model. The path can be absolute or relative to the jenkins job's workspace.<br>**mandatory for EC use case** | "model.slx"
slScriptPath | Path of the model init script. The path can be absolute or relative to the jenkins job's workspace. | "init.m"
**compilerShortName** | Short name of the compiler that should be used (C-Code Use Case). Fallback will be an already selected compiler or, if undefined, the first one that is found.<br>**mandatory for hand code use case** | "MSSDK71", "MSVC140", "MinGW64"
codeModelPath | Path of the code description file. The path can be absolute or relative to the jenkins job's workspace.<br>_This currently required for the architecture update to work!_ | "CodeModel.xml"
mappingFilePath | Path of the mapping file. The path can be absolute or relative to the jenkins job's workspace.<br>_This currently required for the architecture update to work!_ | "Mapping.xml"


### Step "profileCreateSL"

DSL Command: btc.profileCreateSL {...}

**Description**

Creates a new profile for a Simulink model.

_Please note:_ The listed properties only show the Simulink specific properties. Each
of the properties listed in the "profileLoad" step also apply here.

Property | Description | Example Value(s)
---------|-------------|-----------------
**profilePath** | Path of the profile. If it does not exist, it will be created. The path can be absolute or relative to the jenkins job's workspace.<br>**mandatory** | "profile.epp"
**slModelPath** | Path of the EmbeddedCoder model. The path can be absolute or relative to the jenkins job's workspace.<br>**mandatory for EC use case** | "model.slx"
slScriptPath | Path of the model init script. The path can be absolute or relative to the jenkins job's workspace. | "init.m"
**addModelInfoPath** | Path to the XML file with additional model info for SL use case. The path can be absolute or relative to the jenkins job's workspace.<br>**mandatory for SL use case** | "AddGenModelInfo.xml"

### Step "profileCreateC"

DSL Command: btc.profileCreateC {...}

**Description**

Creates a new profile for supported ansi C-Code.

_Please note:_ The listed properties only show the C-Code specific properties. Each of
the properties listed in the "profileLoad" step also apply here.

Property | Description | Example Value(s)
---------|-------------|-----------------
**profilePath** | Path of the profile. If it does not exist, it will be created. The path can be absolute or relative to the jenkins job's workspace.<br>**mandatory** | "profile.epp"
**compilerShortName** | Short name of the compiler that should be used (C-Code Use Case). Fallback will be an already selected compiler or, if undefined, the first one that is found.<br>**mandatory for hand code use case** | "MSSDK71", "MSVC140", "MinGW64"
**codeModelPath** | Path of the code description file. The path can be absolute or relative to the jenkins job's workspace.<br>**mandatory for hand code use case** | "CodeModel.xml"

### Step "vectorImport"

DSL Command: btc.vectorImport {...}

**Description**

Imports test cases or stimuli vectors from the specified location. The
following settings are available:

Property | Description | Example Value(s)
---------|-------------|-----------------
**importDir** | The directory that contains the vectors to import. The path can be absolute or relative to the jenkins job's workspace.<br>**mandatory** | "io\vectors", "E:\data\ImportExport"
vectorFormat | String to specify the format of the vector import files in Standard BTC EmbeddedPlatform style.<br>(default: EXCEL) | "CSV", "EXCEL", "TC"
vectorKind | A String that defines the type of the vectors to import. Can be "TC" (= Test Case) or "SV" (= Stimuli Vector).<br>(default: TC) | "TC", "SV"

**Possible Return values**

| Return Value | Description                                   |
|--------------|-----------------------------------------------|
| 200          | Successfully imported all vectors.            |
| 300          | No valid vectors were found in the importDir. |
| 400          | Error during vector import.                   |
| 500          | Unexpected Error                              |

### Step "toleranceImport"

DSL Command: btc.toleranceImport {...}

**Description**

Imports tolerance settings from the specified file. The following
options are available:

**Possible Return values**

Property | Description | Example Value(s)
---------|-------------|-----------------
**path** | The tolerance settings file. The path can be absolute or relative to the jenkins job's workspace.<br>**mandatory** | "io\tolerances.xml", "E:\data\tolerances.xml"
useCase | String to specify the use case for the Tolerances (Back-to-Back or Requirements-Based Testing).<br>(default: B2B) | "B2B", "RBT"


**Possible Return values**

| Return Value     | Description                                   |
|------------------|-----------------------------------------------|
| 200              | Successfully imported the tolerance settings. |
| 400              | No path specified.                            |
| 401              | The file at specified path does not exist.    |
| 402              | The specified useCase is invalid.             |
| 500              | Unexpected Error                              |

### Step "toleranceExport"

DSL Command: btc.toleranceExport {...}

**Description**

Exports tolerance settings to the specified file. The following options
are available:

**Possible Return values**

Property | Description | Example Value(s)
---------|-------------|-----------------
**path** | The tolerance settings file. The path can be absolute or relative to the jenkins job's workspace.<br>**mandatory** | "io\tolerances.xml", "E:\data\tolerances.xml"
useCase | String to specify the use case for the Tolerances (Back-to-Back or Requirements-Based Testing).<br>(default: B2B) | "B2B", "RBT"

**Possible Return values**

| Return Value     | Description                                   |
|------------------|-----------------------------------------------|
| 200              | Successfully exported the tolerance settings. |
| 400              | No path specified.                            |
| 402              | The specified useCase is invalid.             |
| 500              | Unexpected Error                              |

### Step "inputRestrictionsImport"

DSL Command: btc.inputRestrictionsImport {...}

**Required License**

EmbeddedTester (ET\_COMPLETE)

**Description**

Imports Input Restrictions from the specified file. The following
options are available:

**Possible Return values**

Property | Description | Example Value(s)
---------|-------------|-----------------
**path** | The file that contains the Input Restrictions. The path can be absolute or relative to the jenkins job's workspace.<br>**mandatory** | "io\inputrestrictions.xml", "E:\data\inputrestrictions.xml"

**Possible Return values**

| Return Value     | Description                                   |
|------------------|-----------------------------------------------|
| 200              | Successfully imported the tolerance settings. |
| 400              | No path specified.                            |
| 401              | The file at specified path does not exist.    |
| 500              | Unexpected Error                              |

### Step "executionRecordExport"

DSL Command: btc.executionRecordExport {...}

**Required License**

EmbeddedTester (ET\_COMPLETE)

**Description**

Exports Execution Records to the specified directory. The following
options are available:

**Possible Return values**

Property | Description | Example Value(s)
---------|-------------|-----------------
**dir** | The target directory. The path can be absolute or relative to the jenkins job's workspace.<br>**mandatory** | "exectutionrecords\SIL", "E:\data\er\MIL"
**executionConfig** | Execution configs for the Test Execution (String)<br>**mandatory** | "TL MIL", "SL MIL", "SIL", "PIL"
exportFormat | String specifying the export format for the execution records<br>(default: mdf) | "mdf", "excel"
scopesWhitelist	| Comma separated String with scopes to include. If this string is not empty, only scopes that are listed here will be considered.<br>(default: "" - empty String: all scopes will be considered) | "toplevel"<br>"toplevel, subA, subB"
scopesBlacklist | Comma separated String with scopes to exclude. If this string is not empty, only scopes that are not listed here will be considered.<br>(default: "" - empty String: no scopes will be excluded) | "toplevel"<br>"toplevel, subA, subB"
foldersWhitelist | Comma separated String with folders to include. If this string is not empty, only folders that are listed here will be considered.<br>(default: "" - empty String: all folders will be considered) | "Old Execution Records"<br>"FolderA, FolderB"
foldersBlacklist | Comma separated String with folders to exclude. If this string is not empty, only folders that are not listed here will be considered.<br>(default: "" - empty String: no folders will be excluded) | "Old Execution Records"<br>"FolderA, FolderB"
testCasesWhitelist | Comma separated String with testcases to include. If this string is not empty, only testcases that are listed here will be considered.<br>(default: "" - empty String: all testcases will be considered) | "tc1"<br>"tc1, tc2, tc44"
testCasesBlacklist | Comma separated String with testcases to exclude. If this string is not empty, only testcases that are not listed here will be considered.<br>(default: "" - empty String: no testcases will be excluded) | "tc1"<br>"tc1, tc2, tc44"

*You can define whitelists and blacklists for scopes, folders and test
cases. Everything will be merged resulting in a filtered set of test
cases. Blacklists always have precedence over whitelists (i.e. if
something is whitelisted and blacklisted it will be excluded).*

  

**Possible Return values**

| Return Value     | Description                                  |
|------------------|----------------------------------------------|
| 200              | Successfully exported the execution records. |
| 500              | Unexpected Error                             |

### Step "inputRestrictionsExport"

DSL Command: btc.inputRestrictionsExport {...}

**Required License**

EmbeddedTester (ET\_COMPLETE)

**Description**

Exports Input Restrictions to the specified file. The following options
are available:

**Possible Return values**

Property | Description | Example Value(s)
---------|-------------|-----------------
**path** | The Input Restrictions xml file. The path can be absolute or relative to the jenkins job's workspace.<br>**mandatory** | "io\inputrestrictions.xml", "E:\data\inputrestrictions.xml"

**Possible Return values**

|   Return Value   | Description                                   |
|------------------|-----------------------------------------------|
| 200              | Successfully exported the tolerance settings. |
| 400              | No path specified.                            |
| 500              | Unexpected Error                              |

### Step "rbtExecution"

DSL Command: btc.rbtExecution {...}

**Required License**

EmbeddedTester (ET\_BASE)

**Description**

Executes all functional test cases in the profile. A Test Execution
Report will be exported to the "exportDir" specified in the
"profileLoad" step. The following optional settings are available:

Property | Description | Example Value(s)
---------|-------------|-----------------
executionConfigString | Execution configs for the Test Execution (comma separated list)<br>(default: all available configs) | "TL MIL, SIL, PIL"<br>"SIL, PIL"<br>"SL MIL"
reportSource | String that specified if the report is based on scopes or requirement sources. Setting the report source to "REQUIREMENT" has no effect if no requirements are available in the profile.<br>*Please note: Test Execution Reports based on requirements only consider test cases that are linked to these requirements. Unlinked test cases will not be considered in the report.*<br>(default: SCOPE) | "SCOPE"<br>"REQUIREMENT"
createReport | Boolean flag controlling whether or not the Test Execution Report is created by this step. The report can be created explicitly in its own step (see step "testExecutionReport").<br>(default: false) | true, false
scopesWhitelist	| Comma separated String with scopes to include. If this string is not empty, only scopes that are listed here will be considered.<br>(default: "" - empty String: all scopes will be considered) | "toplevel"<br>"toplevel, subA, subB"
scopesBlacklist | Comma separated String with scopes to exclude. If this string is not empty, only scopes that are not listed here will be considered.<br>(default: "" - empty String: no scopes will be excluded) | "toplevel"<br>"toplevel, subA, subB"
foldersWhitelist | Comma separated String with folders to include. If this string is not empty, only folders that are listed here will be considered.<br>(default: "" - empty String: all folders will be considered) | "Old Execution Records"<br>"FolderA, FolderB"
foldersBlacklist | Comma separated String with folders to exclude. If this string is not empty, only folders that are not listed here will be considered.<br>(default: "" - empty String: no folders will be excluded) | "Old Execution Records"<br>"FolderA, FolderB"
testCasesWhitelist | Comma separated String with testcases to include. If this string is not empty, only testcases that are listed here will be considered.<br>(default: "" - empty String: all testcases will be considered) | "tc1"<br>"tc1, tc2, tc44"
testCasesBlacklist | Comma separated String with testcases to exclude. If this string is not empty, only testcases that are not listed here will be considered.<br>(default: "" - empty String: no testcases will be excluded) | "tc1"<br>"tc1, tc2, tc44"

**Filtering via White- & Blacklists**

*You can define whitelists and blacklists for scopes, folders and test
cases. Everything will be merged resulting in a filtered set of test
cases. Blacklists always have precedence over whitelists (i.e. if
something is whitelisted and blacklisted it will be excluded).*


**Possible Return values**

| Return Value     | Description                                                   |
|------------------|---------------------------------------------------------------|
| 200              | All test cases passed (status: PASSED)                        |
| 201              | Nothing to Execute (no functional test cases in the profile). |
| 300              | There were failed test cases (status: FAILED)                 |
| 400              | There were errors during test case execution (status: ERROR)  |
| 500              | Unexpected Error                                              |

### Step "testExecutionReport"

DSL Command: btc.testExecutionReport{...}

**Required License**

EmbeddedTester (ET\_BASE)

**Description**

Creates the Test Execution Report and exports it to the "exportDir"
specified in the "profileLoad" / "profileCreate" step. If no reportName
is specified the reports will be placed into a subdirectory in order to
avoid multiple reports overwriting each other.

Property | Description | Example Value(s)
---------|-------------|-----------------
reportName | The filename (String) for the resulting html file (suffix is optional).<br>(default: "TestExecutionReport_SIL.html", or "TL MIL" / "SL MIL" / "PIL" respectively) | "MyReport"<br>"Foo.html"
executionConfigString | Execution configs for the Test Execution (comma separated list)<br>(default: all available configs) | "TL MIL, SIL, PIL"<br>"SIL, PIL"<br>"SL MIL"
reportSource | String that specified if the report is based on scopes or requirement sources. Setting the report source to "REQUIREMENT" has no effect if no requirements are available in the profile.<br>Please note: Test Execution Reports based on requirements only consider test cases that are linked to these requirements. Unlinked test cases will not be considered in the report.<br>(default: SCOPE) | "SCOPE"<br>"REQUIREMENT"


**Possible Return values**

| Return Value     | Description      |
|------------------|------------------|
| 200              | Success          |
| 500              | Unexpected Error |

### Step "xmlReport"

DSL Command: btc.xmlReport{...}

**Required License**

EmbeddedTester (ET\_COMPLETE)

**Description**

Creates the XML Report and exports it to the "exportDir" specified in
the "profileLoad" / "profileCreate" step. Requires BTC Plugin for
XMLReports. The following optional settings are available:

Property | Description | Example Value(s)
---------|-------------|-----------------
path | Path to the report (relative paths will be resolved to the exportDir).<br>(default: "BTCXmlReport_<USECASE>.xml") | true, false
useCase | Controls for which use case the coverage is reported.<br>(default: B2B) | "B2B", "RBT"

**Possible Return values**

| Return Value     | Description      |
|------------------|------------------|
| 200              | Success          |
| 500              | Unexpected Error |

### Step "codeAnalysisReport"

DSL Command: btc.codeAnalysisReport{...}

**Required License**

EmbeddedTester (ET\_COMPLETE) for B2B use case.

**Description**

Creates the Code Analysis Report and exports it to the "exportDir"
specified in the "profileLoad" / "profileCreate" step. The following
optional settings are available:

Property | Description | Example Value(s)
---------|-------------|-----------------
reportName | The filename (String) for the resulting html file.<br>(default: "report.html") | "report.html", "BTCCodeCoverage.html"
includeSourceCode | Boolean flag controlling whether the annotated source code will be included in the Code Analysis Report.<br>(default: false) | true, false
useCase | Controls for which use case the coverage is reported.<br>(default: B2B) | "B2B", "RBT"

**Possible Return values**

| Return Value     | Description      |
|------------------|------------------|
| 200              | Success          |
| 500              | Unexpected Error |

In addition, this step creates a CSV file
"BTCCoverageOverview\_*USECASE*.csv" (with USECASE being B2B or RBT)
which can be used by other Jenkins Plugins like the [Plot
Plugin](https://plugins.jenkins.io/plot) to report coverage.

**Example content of the CSV File:**

``` c
Statement Coverage, Decision Coverage, MC/DC Coverage
100.0, 90.0, 91.98
```

  

**Example of plots created by the [Plot
Plugin](https://plugins.jenkins.io/plot):**

![](https://wiki.jenkins.io/download/attachments/173703174/plots.png?version=1&modificationDate=1558695682000&api=v2)

``` groovy
plot csvFileName: 'plot-b2b-codecoverage.csv', csvSeries: [[displayTableFlag: false, exclusionValues: '', file: "reports/BTCCoverageOverview_B2B.csv", inclusionFlag: 'OFF', url: '']], group: 'BTC Code Coverage Overview', style: 'line', title: 'B2B Code Coverage (Structural)', yaxis: 'Coverage Percentage'
```

### Step "modelCoverageReport"

DSL Command: btc.modelCoverageReport{...}

**Required License**

EmbeddedTester (ET\_COMPLETE) for B2B use case

Simulink Coverage (formerly V&V)

**Description**

Creates the Model Coverage Report and exports it to the "exportDir"
specified in the "profileLoad" / "profileCreate" step. The following
optional settings are available:

Property | Description | Example Value(s)
---------|-------------|-----------------
executionConfig | The execution config for the MIL execution used for model coverage measurement.<br>(default: first available MIL execution config, arbitrary if more than one exists) | "TL MIL", "SL MIL"
reportName | The filename (String) for the resulting html file.<br>(default: "report.html") | "report.html", "BTCCodeCoverage.html"
useCase | Controls for which use case the coverage is reported.<br>(default: RBT) | "B2B", "RBT"

**Possible Return values**

| Return Value     | Description      |
|------------------|------------------|
| 200              | Success          |
| 500              | Unexpected Error |

  

### Step "formalTest"

DSL Command: btc.formalTest{...}

**Required License**

EmbeddedTester (ET\_BASE) + Formal Test Add-On

**Description**

Executes a Formal Test based on all formal requirements in the profile.
A Formal Test Report will be exported to the "exportDir" specified in
the "profileLoad" step (and will be linked in the overview report). The
following optional settings are available:

Property | Description | Example Value(s)
---------|-------------|-----------------
executionConfigString | Execution configs on which the Formal Test should run (comma separated list)<br>(default: all available configs) | "TL MIL, SIL, PIL"<br>"SIL, PIL"<br>"SL MIL"

**Possible Return values**

| Return Value     | Description                                                  |
|------------------|--------------------------------------------------------------|
| 200              | All test cases passed (status: PASSED / FULLFILLED)          |
| 201              | Nothing to Execute (no formal requirements in the profile).  |
| 300              | There were violations (status: FAILED / VIOLATED)            |
| 400              | There were errors during test case execution (status: ERROR) |
| 500              | Unexpected Error                                             |

  

### Step "rangeViolationGoals"

DSL Command: btc.rangeViolationGoals{...}

**Required License**

EmbeddedTester (ET\_COMPLETE)

**Required Plugin**

Plugin RangeViolationGoals

**Description**

Adds Range Violation Goals to the profile which contribute to the Code
Analysis Report and can be considered during vector generation (pll:
"RVG"). The following optional settings are available:

Property | Description | Example Value(s)
---------|-------------|-----------------
scopePath | Hierarchy path to the targeted scope / subsystem. Leave empty to target the toplevel. Use "*" to target all scopes.<br>(default: toplevel subsystem) | "Toplevel/SubA", "*"
rvXmlPath | Path to an xml file containing Range Violation specs. | "RangeViolationGoals.xml"
considerOutputs | Boolean flag controlling whether the goals should be created for Outputs.<br>(default: true) | true, false
considerLocals | Boolean flag controlling whether the goals should be created for local displayables.<br>(default: true) | true, false
checkRangeSpecification | Boolean flag controlling whether the goals should only be created if a signal has Min/Max values other than the data type range.<br>(default: true) | true, false

**Possible Return values**

| Return Value     | Description                                |
|------------------|--------------------------------------------|
| 200              | Success                                    |
| 400              | Range Violation Goals plugin not installed |
| 500              | Unexpected Error                           |

### Step "domainCoverageGoals"

DSL Command: btc.domainCoverageGoals{...}

**Required License**

EmbeddedTester (ET\_COMPLETE)

**Required Plugin**

Plugin DomainCoverageGoals

**Description**

Adds Domain Coverage Goals to the profile which contribute to the Code
Analysis Report and can be considered during vector generation (pll:
"DCG"). The following optional settings are available:

Property | Description | Example Value(s)
---------|-------------|-----------------
scopePath | Hierarchy path to the targeted scope / subsystem. Leave empty to target the toplevel. Use "*" to target all scopes.<br>(default: toplevel subsystem) | "Toplevel/SubA", "*"
dcXmlPath | Path to an xml file containing Domain Coverage specs. | "DomainCoverageGoals.xml"
raster | String to specify a raster in %. Domain Coverage Goals will be created for equal according to the raster.<br>(default: 25) | "10", "25", "30"

**Possible Return values**

| Return Value     | Description                                |
|------------------|--------------------------------------------|
| 200              | Success                                    |
| 400              | Domain Coverage Goals plugin not installed |
| 500              | Unexpected Error                           |

### Step "vectorGeneration"

DSL Command: btc.vectorGeneration{...}

**Required License**

EmbeddedTester (ET\_COMPLETE)

**Description**

Executes the engines for analysis and stimuli vector generation for
structural coverage. The following optional settings are available:

Property | Description | Example Value(s)
---------|-------------|-----------------
pll | Semicolon separated list of PLLs used to set the goals for automatic stimuli vector generation.<br>(default: all goals will be analyzed) | "STM; D", "STM:3", … (see Back-to-Back & Vector Generation User Guide for more details about PLLs)
engine | Engine to be used for vector generation (guided random, model checker, both)<br>
(default: "ATG+CV", combined hierachical approach) | "ATG+CV", "ATG", "CV" (see Back-to-Back & Vector Generation User Guide for more details about engines)
globalTimeout | Global timeout in seconds. 0 means no timeout.<br>(default: 600) | 600
scopeTimeout | Scope timeout in seconds. 0 means no timeout.<br>(default: 300) | 300
perPropertyTimeout | Timeout per coverage property in seconds. 0 means no timeout.<br>(default: 60) | 60
considerSubscopes | Boolean flag controlling whether or not to consider coverage goals from subscopes.<br>(default: true) | true, false
recheckUnreachable | Boolean flag controlling whether or not to recheck already calculated unreachable results.<br>(default: false) | true, false
depthCv | Controls the maximum depth for the CV engine. Set to 0 for infinite depth.<br>(default: 10) | 0, 10, 20, 50
depthAtg | Controls the maximum depth for the ATG engine. Must be greater than 0.<br>(default: 20) | 10, 20, 50
loopUnroll | Number of loop interations to unroll for unpredictable loops.<br>(default: 50) | 10, 20, 50
robustnessTestFailure | Boolean flag controlling whether or not robustness issues are added to the JUnit XML Report as "failed tests".<br>(default: false) | true, false
createReport | Boolean flag controlling whether or not the Code Analysis Report is created by this step. The report can be created explicitly in its own step which is why you might want to tweak this setting.<br>(default: false) | true, false

**Possible Return values**

| Return Value     | Description                      |
|------------------|------------------------------------------------------------------------------------------------------------------------------|
| 200              | Successfully generated vectors and reached all selected coverage goals (see PLL property). No robustness goals were covered. |
| 300              | Ran into timeouts before completely analyzing all selected goals (see PLL property). No robustness goals were covered.       |
| 41x              | Covered Robustness Goal: Downcast                                                                                            |
| 4x1              | Covered Robustness Goal: Division by Zero                                                                                    |
| 500              | Unexpected Error                                                                                                             |

### Step "backToBack"

DSL Command: btc.backToBack {...}

**Required License**

EmbeddedTester (ET\_COMPLETE)

**Description**

Executes a Back-to-Back Test between the specified reference and
comparison configuration (e.g. TL MIL vs. SIL). This step requires
stimuli vectors or functional test cases in the profile. A Back-to-Back
Test Report will be exported to the "exportDir" specified in the
"profileLoad" step.

In automated scenarios the effort for manual reviews of frequently
executed Back-to-Back tests can become quite high. The BTC plugin
"ApplyFailedAccepted" deals with this challenge by applying your
manually accepted deviations to all future Back-to-Back Tests as long as
the deviating values are equal. For more information, please contact
<support@btc-es.de>.

  

The following optional settings are available:

Property | Description | Example Value(s)
---------|-------------|-----------------
reference | Execution config for the Back-to-Back test reference simulation.<br>(default: "TL MIL") | TL MIL, SIL, PIL, SL MIL
comparison | Execution config for the Back-to-Back test comparison simulation.<br>(default: "SIL") | TL MIL, SIL, PIL, SL MIL

**Possible Return values**

| Return Value     | Description                                                                              |
|------------------|------------------------------------------------------------------------------------------|
| 200              | Back-to-Back Test passed (status: PASSED)                                                |
| 201              | Back-to-Back Test has accepted failures (status: FAILED ACCEPTED)                        |
| 300              | There were deviations between the reference and comparison architecture (status: FAILED) |
| 400              | There were errors during the execution (status: ERROR)                                   |
| 500              | Unexpected Error                                                                         |

### Step "regressionTest"

DSL Command: btc.regressionTest {...}

**Required License**

EmbeddedTester (ET\_COMPLETE)

**Description**

Executes a Regression Test between the current SUT and old Execution
Records that have been saved. If no saved Execution Records are
available, the vectors will only be executed on the current SUT and the
Execution Records will be stored for a later Regression Test. This
requires stimuli vectors or functional test cases in the profile.  A
Regression Test Report will be exported to the "exportDir" specified in
the "profileLoad" step. The following optional settings are available:

Property | Description | Example Value(s)
---------|-------------|-----------------
executionConfigString | Execution configs for the simulation on the current SUT (comma separated list).<br>(default: SIL) | "TL MIL, SIL, PIL", "SL MIL", "SIL"

**Possible Return values**

| Return Value     | Description                                                                |
|------------------|----------------------------------------------------------------------------|
| 200              | Regression Test passed (status: PASSED)                                    |
| 201              | Nothing to compare. Simulation results stored for later Regression Tests.  |
| 300              | There were deviations between the old and the new version (status: FAILED) |
| 400              | There were errors during the execution (status: ERROR)                     |
| 500              | Unexpected Error                                                           |

### Step "formalVerification"

DSL Command: btc.formalVerification {...}

**Required License**

EmbeddedValidator (EV)

**Description**

Executes all existing proofs in the profile and generates a Formal
Verification Report. The following optional settings are available:

Property | Description | Example Value(s)
---------|-------------|-----------------
loopUnroll | If the code that is to be analyzed contains loops without explicit maximum number of iterations, e.g. a while(true) loop, these loops need to be unrolled. The given number provides the number of iterations for these loops that is used for the analysis. This unrolling is done in such a way that a "fulfilled" or "fulfilled (n steps)" result can only be obtained if this limit is not exceeded by any possible execution. Conversely, a trace that violates this limit can be found as a witness trace despite not strictly violating the formal requirement itself. This is indicated in the termination reason as "Loop unroll limit (n) exceeded".<br>(default: 32) | 10, 30, 50
memoryLimit | Memory Limit (MB)<br>The maximum memory footprint to be used by the analysis tools of the EmbeddedPlatform. If an analysis cannot be completed within these limits, this may lead to termination reason "Memory limit (n MB) exceeded"<br>(default: unlimited)
 | 0 (= unlimited), 1024, 3456
timeLimit | Time Limit (Seconds)<br>The maximum duration the proof execution may take (excluding some of the pre- and postprocessing tasks, which in general are less time intensive than the actual model checking). This limit should be used especially whenever the proof execution is left unattended and multiple proofs are to be executed in batch. Otherwise, a single proof execution may consume most of the run time and no results would be obtained for the other proofs.<br>(default: unlimited) | 0 (= unlimited), 60, 1000
searchDepth | Search depth (Steps)<br>the number of executions of the scope under test (i.e. how many execution steps may a counter example be long). This limit corresponds to the term "unwinding depth" employed in bounded model checking. Again, if no such limit is provided, the search for a counter example may in the worst case spend large amounts of time looking for longer and longer counter examples.<br>(default: unlimited) | 0 (= unlimited), 10, 50

**Possible Return values**

| Return Value     | Description                                    |
|------------------|------------------------------------------------|
| 200              | All proofs are fulfilled (status: FULFILLED)   |
| 300              | There was a violation (status: VIOLATED)       |
| 301              | Unknown (status: UNKNOWN)                      |
| 400              | BTC EmbeddedValidator package is not installed |
| 500              | Unexpected Error                               |

### Step "getStatusSummary"

DSL Command: btc.getStatusSummary {...}

**Required License**

EmbeddedTester (ET COMPLETE)

**Description**

Retrieves the following struct:

- ProfileName
- Coverage
  - RBT
    - RequirementsCoverage
    - StatementCoverage
    - ConditionCoverage
    - DecisionCoverage
    - ModifiedConditionDecisionCoverage
    - RelationalOperatorCoverage
    - FunctionCoverage
    - FunctionCallCoverage
    - SwitchCaseCoverage
    - UserDefinedCoverageGoalsCoverage
    - DomainCoverageGoalsCoverage
    - RangeViolationGoalsCoverage
    - DivisionByZeroRobustnessCheck
    - DowncastRobustnessCheck
  - B2B
    - StatementCoverage
    - ConditionCoverage
    - DecisionCoverage
    - ModifiedConditionDecisionCoverage
    - RelationalOperatorCoverage
    - FunctionCoverage
    - FunctionCallCoverage
    - SwitchCaseCoverage
    - UserDefinedCoverageGoalsCoverage
    - DomainCoverageGoalsCoverage
    - RangeViolationGoalsCoverage
    - DivisionByZeroRobustnessCheck
    - DowncastRobustnessCheck
  - TestCases (list of objects with the following properties)
    - Name
    - Description
    - Result
    - VerifiedRequirements (list of strings)
    - Length
    - CreatedOn
    - CreatedBy

### Step "wrapUp"

DSL Command: btc.wrapUp {...}

**Description**

Publishes HTML reports and the JUnit XML report to Jenkins and closes
BTC EmbeddedPlatform. The following optional settings are available:

Property | Description | Example Value(s)
---------|-------------|-----------------
archiveProfiles | Boolean flag controlling whether BTC EmbeddedPlatform profiles are archived by Jenkins to be available on the Job Page. You can disable this and control the "archiveArtifacts" option yourself.<br>(default: true) | true, false
publishReports | Boolean flag controlling whether the BTC EmbeddedPlatform reports are published in Jenkins to be available on the Job Page. You can disable this and control the "publishHTML" option yourself.<br>(default: true) | true, false
publishResults | Boolean flag controlling whether BTC EmbeddedPlatform test results (JUnit XML) are published in Jenkins to be available on the Job Page and for further aggregations. You can disable this and control the "junit" option yourself.<br>(default: true) | true, false
  

## BTC Migration Suite

The BTC Migration Suite allows you to perform a fully automatic
regression test between different Matlab or TargetLink versions. This
makes it easy to document that the change of a tool version in a
particular project does not influence the behavior of software
components on model and code level.

**Required License**

EmbeddedTester (ET\_COMPLETE)

### Step "migrationSource"

DSL Command: btc.migrationSource {...}

**Description**

Creates a profile on the source configuration (e.g. old Matlab /
TargetLink version), generates vectors for full coverage and exports the
simulation results.

### Step "migrationTarget"

DSL Command: btc.migrationTarget {...}

**Description**

Creates a profile on the target configuration (e.g. newMatlab /
TargetLink version), imports the simulation results from the source
config and runs a regression test.

  

For both steps (migrationSource and migrationTarget) the following
parameters are mandatory:

Property | Description | Example Value(s)
---------|-------------|-----------------
**tlModelPath** | Path of the TargetLink model. The path can be absolute or relative to the jenkins job's workspace.< | "tlModel.slx", "model.mdl"
matlabVersion | Controls which matlab version will be used by the tool.<br>String containing the release version (e.g. "2016b"), optionally followed by "32-bit" or "64-bit". The version and 32/64-bit part should be separated by a space character. | "2010a 32-bit"<br>"2016b"

In Addition, you can add all other the parameters from the
steps btc.profileLoad and btc.vectorGeneration if required.

### Migration Suite Example: Jenkinsfile

The following example shows a Jenkinsfile for a complete migration of a
TargetLink model. The labels that are specified for the node describe
the resource requirements for each configuration and allow Jenkins to
provide a suitable agent. Any data produced by the migrationSource step
which is needed by the migrationTarget step will be automatically
handled by Jenkins. This way, the agent for the target config (which is
obviously a different one machine due to the different Windows versions)
will have access to the simulation results from the source config
required for the regression test.

**Migration Suite Example**

``` groovy
node ('Win7 && TL41 && ML2015b') {

    checkout scm
 
    stage ("Migration Source") {
        btc.migrationSource {
            matlabVersion = "2015b"
            tlModelPath = "source/model.mdl"
            tlScriptPath = "start.m"
        }
    }
}

node ('Win10 && TL43 && ML2017b') {

    checkout scm
 
    stage ("Migration Target") {
        btc.migrationTarget {
            matlabVersion = "2017b"
            tlModelPath = "target/model.mdl"
            tlScriptPath = "start.m"
        }
    }
}
```

## Adding the BTC Plugin to Jenkins

In order to use the convenient pipeline syntax described above you need
to add the BTC Plugin to Jenkins. This is very easy and can be done with
the following steps:

On your Jenkins web UI go to Jenkins \> Manage Jenkins \> Manage Plugins

Click on the Available tab and search for btc-embeddedplatform

Select the plugin btc-embeddedplatform and install it

1.  Once the plugin is installed new versions which are available will
    appear in the Updates section
2.  Updating a plugin usually requires you to restart your Jenkins
    master

The BTC Pipeline plugin is now available in Jenkins and you can benefit
from the convenient BTC pipeline methods described in the sections
above. Enjoy!

If your Jenkins master can't access the internet you can manually
download the plugin
[here](https://updates.jenkins-ci.org/latest/btc-embeddedplatform.hpi)
and upload it to the server via the advanced section of the "Manage
Plugins" page.
