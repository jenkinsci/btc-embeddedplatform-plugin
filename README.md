## Quick Start Checklist

In order to use BTC EmbeddedPlatform steps in your Jenkins Pipeline, here's what should be prepared upfront:

<details>
  <summary>Jenkins Controller</summary>
  Have the btc-embeddedplatform-plugin (BTC DSL for Pipeline) installed on the Jenkins Controller
</details>

<details>
  <summary>Jenkins Agent</summary>
  A Jenkins Agent with:

  - Windows 10 or Windows Server 2019 LTSC
  - BTC EmbeddedPlatformJenkinsAutomation Plugin for BTC EmbeddedPlatform (installed manually or via the BTC Plugin Manager)
  - In order to prevent problems with user access rights it is important to let the Jenkins agent process run as a specific user. In case your connection to the Controller uses a Windows Service, you need to make it log on as a specific user instead of "Local System Accout":
![jenkins-service-user](https://user-images.githubusercontent.com/5657657/109487642-c687ea80-7a84-11eb-9c85-c12f0c275cc6.png)
</details>

<details>
  <summary>Licensing</summary>
  You'll need a multi-site network license for "EmbeddedTester" and "Test Automation Server"
  
  - The license server can be configured via the "licenseLocationString" parameter in the profileLoad / profileCreateXX step
  - it can also be configured directly on the Jenkins Agent via the BTC EmbeddedPlatform GUI on the Jenkins Agent

  In case Jenkins initially fails to connect to BTC EmbeddedPlatform ("400: Timeout while connecting to BTC EmbeddedPlatform") you may have forgotten to install the "Jenkins Automation Plugin for BTC EmbeddedPlatform" (see above: Jenkins Agent). If you did install the plugin, you might want to go to %APPDATA%/BTC/ep and delete the directory matching the EP version you're using. This is normally done during the plugin installation sometimes there are issues with non-admin users or other applications interfering.
</details>

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
  - [Basics](#basics)
    + [Step "startup"](#step-startup)
    + [Step "profileLoad"](#step-profileload)
    + [Step "profileCreateTL"](#step-profilecreatetl)
    + [Step "profileCreateEC"](#step-profilecreateec)
    + [Step "profileCreateSL"](#step-profilecreatesl)
    + [Step "profileCreateC"](#step-profilecreatec)
    + [Step "wrapUp"](#step-wrapup)
  - [Import & Export](#import-export)
    + [Step "vectorImport"](#step-vectorimport)
    + [Step "vectorExport"](#step-vectorexport)
    + [Step "toleranceImport"](#step-toleranceimport)
    + [Step "toleranceExport"](#step-toleranceexport)
    + [Step "inputRestrictionsImport"](#step-inputrestrictionsimport)
    + [Step "inputRestrictionsExport"](#step-inputrestrictionsexport)
    + [Step "executionRecordExport"](#step-executionrecordexport)
  - [Analysis, Verification & Validation](#analysis-verification-validation)
    + [Step "rbtExecution"](#step-rbtexecution)
    + [Step "vectorGeneration"](#step-vectorgeneration)
    + [Step "backToBack"](#step-backtoback)
    + [Step "regressionTest"](#step-regressiontest)
    + [Step "rangeViolationGoals"](#step-rangeviolationgoals)
    + [Step "domainCoverageGoals"](#step-domaincoveragegoals)
    + [Step "addDomainCheckGoals"](#step-adddomaincheckgoals)
    + [Step "addInputCombinationGoals"](#step-addinputcombinationgoals)
    + [Step "defaultTolerances"](#step-defaulttolerances)
    + [Step "formalTest"](#step-formaltest)
    + [Step "formalVerification"](#step-formalverification)
  - [Reporting](#reporting)
    + [Step "testExecutionReport"](#step-testexecutionreport)
    + [Step "xmlReport"](#step-xmlreport)
    + [Step "codeAnalysisReport"](#step-codeanalysisreport)
    + [Step "modelCoverageReport"](#step-modelcoveragereport)
    + [Step "interfaceReport"](#step-interfacereport)
    + [Overall Report](#overallreport)
  - [Misc](#misc)
    + [Step "getStatusSummary"](#step-getstatussummary)
    + ["Error handling"](#error-handling)
  
* [BTC Migration Suite](#btc-migration-suite)
  - [Step "migrationSource"](#step-migrationsource)
  - [Step "migrationTarget"](#step-migrationtarget)
  - [Migration Suite Example: Jenkinsfile](#migration-suite-example-jenkinsfile)
* [Adding the BTC Plugin to Jenkins](#adding-the-btc-plugin-to-jenkins)

## Release Notes

Version | Release Notes | EP Version | Update on Agent | Update on Controller
--------|---------------|------------|-----------------|--------------------
22.1.0 | - Adapted to EP 22.1 | 22.1 | X | 
2.11.0 | - Adapted to EP 2.11 | 2.11 | X | X
2.10.1 | - Adapted to EP 2.10<br>- Made UDCG dependency optional (this prevented the plugin from being loaded if BTC EmbeddedPlatform was installed without the UDCG AddOn) | 2.10 | X | 
2.9.3 | - Added option to control the scopes for input combination goals (instead of toplevel only)<br>- Fixed an issue with the EC wrapper model creation for cases where no slScriptPath is specified<br>- Added enhanced reporting for multi-model migration suite use case | 2.9 | X | X 
2.9.2 | - Improved port handling for btc.startup | 2.9 | | X 
2.9.1 | - Added protection against unsupported execution on agents using "Local System" user<br>- Added support for EC wrapper model creation<br>- Added check for model version change to automatically invoke an architecture update if required (can be forced with "updateRequired = true" or prevented with "disableUpdate = true")<br>- Added defaultTolerances step to add tolerances for RBT or B2B | 2.9 | X | X 
2.9.0 | - Adapted to EP 2.9<br>- Added domain checks step<br>- Added options for parallel execution (vectorGeneration)<br>- Test steps no longer automatically set the Pipeline to unstable | 2.9 | X | X 
2.8.7 | - Added support for EmbeddedCoder Wrapper model creation (requires plugin version 2.9.1 or higher on the Jenkins Controller) | 2.8 | X |  X
2.8.6 | - Fixed an issue with ZERO and CENTER calculation of input combination goals<br>- Added robustness improvement for overview report | 2.8 | X |  
2.8.4 | - Added overview report capabilities when working with more than one project<br>- fixed an alignment issue in the reports<br>- Added overall report option to report on multiple projects<br>Added addInputCombinationGoals step to add input combination goals based on the User Defined Coverage Goals feature | 2.8 | X | X 
2.8.2 | - Added suppport for blacklist/whitelist filtering for rbtExecution based on linked requirements<br>- Added possibility to specify additional jvm arguments on startup (e.g. -Xmx2g)<br>- Added option for DomainCoverageGoals to only apply to inputs/cals (see step btc.domainCoverageGoals) | 2.8 | X | X 
2.8.1 | - Added option to control the rmi port used for the matlab connection (matlabPort -> btc.startup) | 2.8 | | X 
2.8.0 | - Adapted to EP 2.8<br>- NOTE: requires BTC DSL for Pipeline (btc-embeddedplatform-plugin) on the Jenkins master in version 2.8.0 or higher! | 2.8 | X | X 
2.7.3 | - Fixed an encoding issue introduced by a recent windows update (now ensures utf-8 charset)<br>- Fixed an issue that prevented the architecture update for manually created merged architectures (SL + C-Code). NOTE: this fix requires BTC DSL for Pipeline (btc-embeddedplatform-plugin) on the Jenkins master in version 2.8.0 or higher! | 2.7 | X | X 
2.6.2<br>2.5.11<br>2.4.23 | - An issue was introduced by a recent windows update. Added a workaround for that which ensures that the report.json file (migration suite use case) is always encoded in utf-8. | 2.6 | X |  
2.6.1 | - Added Parameters "analyzeSubscopesHierachichally" and "allowDenormalizedFloats" to vectorGeneration step<br>- Added Parameters "tlSubsystemFilter", "tlCalibrationFilter" and "tlCodeFileFilter" to profileCreateTL step<br>- Added vectorExport step for Test Cases & Stimuli Vectors | 2.6 | X |  X
2.7.0 | - Adapted to EP 2.7 | 2.7 | X |  
2.6.0 | - Minor reporting modifications for reference simulation in migration suite use case<br>- Adaptions to EP 2.6 | 2.6 | X |  X
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
pipeline {
    agent none // agent can be defined per stage
    stages {
        stage ('BTC Unit Test') {
            // Stage must run on an agent with the required software, e.g.:
            // - Matlab Simulink + Code Generator
            // - Compiler (Visual Studio / MinGW)
            // - BTC EmbeddedPlatform incl. JenkinsAutomation plugin
            agent { label 'my_btc_agent' }

            steps {
                // checkout files from repository referenced in this pipeline item
                checkout scm
                // Tests with BTC EmbeddedPlatform
                script {
                    // start BTC EmbeddedPlatform on Agent
                    btc.startup {
                        additionalJvmArgs = '-Xmx2g'
                    }
                    
                    // load and update test project
                    btc.profileLoad {
                        profilePath = "my_module.epp"
                        tlModelPath = "my_module.slx"
                        tlScriptPath = "start.m"
                        matlabVersion = "2020b"
                        updateRequired = true
                    }

                    // Execute requirements-based tests
                    btc.rbtExecution {
                        createReport = true
                    }

                    // Generate vectors for statement & mcdc coverage
                    btc.vectorGeneration {
                        pll = "STM; MCDC"
                    }
                    
                    // Generate coverage reports
                    btc.codeAnalysisReport { useCase = "RBT" }
                    btc.codeAnalysisReport { useCase = "B2B" }
                    
                    // B2B test model vs. code
                    btc.backToBack {
                        reference = "TL MIL"
                        comparison = "SIL"
                    }
                
                    // BTC: close EmbeddedPlatform and store reports
                    btc.wrapUp {}
                }
            }
            post {
                always {
                    // make sure that EP is closed, also in case of errors or if the build is aborted
                    script { btc.killEp {} }
                }
            }
        }
    }
}   

```

## Workflow Steps

### Basics

#### Step "startup"

DSL Command: btc.startup {...}

**Description**

Method to connect to BTC EmbeddedPlatform with a specified port. If BTC
EmbeddedPlatform is not available it is started and the method waits
until it is available. The following optional settings are available:

Property | Description | Example Value(s)
---------|-------------|-----------------
port | Port used to connect to EmbeddedPlatform.<br>(default: 29267) | 1234, 29268, 8073
matlabPort | RMI port used to connect EmbeddedPlatform to Matlab.<br>(default: 29300 + (port % 100)) | 1090, 29300, 1234
timeout | Timeout in seconds before the attempt to connect to EmbeddedPlatform is cancelled. This timeout should consider the worst case CPU & IO performance which influences the tool startup.<br>(default: 120) | 40, 60, 120
licensingPackage | Name of the licensing package to use, e.g. to use a EmbeddedTester BASE.<br>(default: ET_COMPLETE) | ET_BASE
installPath | Path to the BTC EmbeddedPlatform installation directory<br>Usually this can be omitted, the "active" EP version will be chosen (queried from the windows registry) | "C:/Program Files/BTC/ep2.8p0"
additionalJvmArgs | String with space separated arguments to be passed to the JVM<br>(expert property - only use if you know what you're doing!) | "-Xmx2g"

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

#### Step "profileLoad"

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
environmentXmlPath | Path to the XML file with additional include paths, etc.. The path can be absolute or relative to the jenkins job's workspace. (only relevant for TargetLink use cases) | "Environment.xml"
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
updateRequired | Boolean flag that controls if the architecture update will be performed, even if the model has not changed.<br>(default: false) | true, false
disableUpdate | Boolean flag that controls if the architecture update will be disabled, even if the model has changed.<br>(default: false) | true, false
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

#### Step "profileCreateTL"

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
tlSubsystemFilter | Regular expression that controls which subsystems will be available for testing. Please note that removing a subsystem will cause its children to be removed as well.<br>(default: empty; all subsystems will be available for testing) | "scope_.*" (matches scope_a and scope_b but not new_scope_a)
tlCalibrationFilter | Regular expression that controls which calibrations will be available for testing. <br>(default: empty; all detected calibrations will be available for testing) | "c.*" (matches cFooBar but noch mpFooBar)
tlCodeFileFilter | Regular expression that controls which code files will be annotated for coverage. <br>(default: empty; all code files will be annotated for coverage) | "mod_a.*" (matches mod_a_controller.c but noch mod_b_library.c)

#### Step "profileCreateEC"

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
createWrapperModel | Boolean flag that controls if the BTC wrapper model shall be created or the specified EmbeddedCoder model. This is required in case of multiple Runnables or Client-Server communication.<br>(default: false) | true, false


#### Step "profileCreateSL"

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

#### Step "profileCreateC"

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

#### Step "wrapUp"

DSL Command: btc.wrapUp {...}

**Description**

Publishes HTML reports and the JUnit XML report to Jenkins and closes
BTC EmbeddedPlatform. The following optional settings are available:

Property | Description | Example Value(s)
---------|-------------|-----------------
closeEp | Boolean flag controlling whether the BTC EmbeddedPlatform will be closed.<br>(default: true) | true, false
archiveProfiles | Boolean flag controlling whether BTC EmbeddedPlatform profiles are archived by Jenkins to be available on the Job Page. You can disable this and control the "archiveArtifacts" option yourself.<br>(default: true) | true, false
publishReports | Boolean flag controlling whether the BTC EmbeddedPlatform reports are published in Jenkins to be available on the Job Page. You can disable this and control the "publishHTML" option yourself.<br>(default: true) | true, false
publishResults | Boolean flag controlling whether BTC EmbeddedPlatform test results (JUnit XML) are published in Jenkins to be available on the Job Page and for further aggregations. You can disable this and control the "junit" option yourself.<br>(default: true) | true, false

### Import & Export

#### Step "vectorImport"

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

#### Step "vectorExport"

DSL Command: btc.vectorExport {...}

**Description**

Exports test cases or stimuli vectors to the specified location. The
following settings are available:

Property | Description | Example Value(s)
---------|-------------|-----------------
**dir** | The directory that contains the vectors to export. The path can be absolute or relative to the jenkins job's workspace.<br>**mandatory** | "io\vectors", "E:\data\ImportExport"
vectorFormat | String to specify the format of the vector export files in Standard BTC EmbeddedPlatform style.<br>(default: EXCEL, TC format only applicable for test cases) | "CSV", "EXCEL", "TC"
vectorKind | A String that defines the type of the vectors to export. Can be "TC" (= Test Case) or "SV" (= Stimuli Vector).<br>(default: TC) | "TC", "SV"

**Possible Return values**

| Return Value | Description                                   |
|--------------|-----------------------------------------------|
| 200          | Successfully exported all vectors.            |
| 300          | No valid vectors were found in the profile  . |
| 400          | Error during vector export.                   |
| 500          | Unexpected Error                              |

#### Step "toleranceImport"

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

#### Step "toleranceExport"

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

#### Step "inputRestrictionsImport"

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

#### Step "inputRestrictionsExport"

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

#### Step "executionRecordExport"

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
requirementsWhitelist	| Comma separated String with requirement names or externalIDs. If this string is not empty, only test cases linked to one of these requirements will be considered.<br>(default: "" - empty String: all test cases will be considered) | "toplevel"<br>"req_a, r124"
requirementsBlacklist | Comma separated String with requirement names or externalIDs. If this string is not empty, test cases linked to one of these requirements will not be considered.<br>(default: "" - empty String: all test cases will be considered) | "toplevel"<br>"req_a, r124"
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

### Analysis, Validation & Verification

#### Step "rbtExecution"

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
requirementsWhitelist	| Comma separated String with requirement names or externalIDs. If this string is not empty, only test cases linked to one of these requirements will be considered.<br>(default: "" - empty String: all test cases will be considered) | "toplevel"<br>"req_a, r124"
requirementsBlacklist | Comma separated String with requirement names or externalIDs. If this string is not empty, test cases linked to one of these requirements will not be considered.<br>(default: "" - empty String: all test cases will be considered) | "toplevel"<br>"req_a, r124"
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


#### Step "vectorGeneration"

DSL Command: btc.vectorGeneration{...}

**Required License**

EmbeddedTester (ET\_COMPLETE)

**Description**

Executes the engines for analysis and stimuli vector generation for
structural coverage. The following optional settings are available:

Property | Description | Example Value(s)
---------|-------------|-----------------
pll | Semicolon separated list of PLLs used to set the goals for automatic stimuli vector generation.<br>(default: all goals will be analyzed) | "STM; D", "STM:3", … (see Back-to-Back & Vector Generation User Guide for more details about PLLs)
engine | Engine to be used for vector generation (guided random, model checker, both)<br>(default: "ATG+CV", combined hierachical approach) | "ATG+CV", "ATG", "CV" (see Back-to-Back & Vector Generation User Guide for more details about engines)
globalTimeout | Global timeout in seconds. 0 means no timeout.<br>(default: 600) | 600
scopeTimeout | Scope timeout in seconds. 0 means no timeout.<br>(default: 300) | 300
perPropertyTimeout | Timeout per coverage property in seconds. 0 means no timeout.<br>(default: 60) | 60
considerSubscopes | Boolean flag controlling whether or not to consider coverage goals from subscopes.<br>(default: true) | true, false
analyzeSubscopesHierachichally | Boolean flag controlling whether or not to analyze subscopes hierachically.<br>(default: true) | true, false
allowDenormalizedFloats | Boolean flag controlling whether or not to allow denormalized floats.<br>(default: true) | true, false
recheckUnreachable | Boolean flag controlling whether or not to recheck already calculated unreachable results.<br>(default: false) | true, false
depthCv | Controls the maximum depth for the CV engine. Set to 0 for infinite depth.<br>(default: 10) | 0, 10, 20, 50
depthAtg | Controls the maximum depth for the ATG engine. Must be greater than 0.<br>(default: 20) | 10, 20, 50
loopUnroll | Number of loop interations to unroll for unpredictable loops.<br>(default: 50) | 10, 20, 50
robustnessTestFailure | Boolean flag controlling whether or not robustness issues are added to the JUnit XML Report as "failed tests".<br>(default: false) | true, false
createReport | Boolean flag controlling whether or not the Code Analysis Report is created by this step. The report can be created explicitly in its own step which is why you might want to tweak this setting.<br>(default: false) | true, false
numberOfThreads | Integer to specify the number of parallel threads for vector generation (CV engine) *Note: this may lead to increased memory consumption*.<br>(default: 1) | 4, 6, 8
parallelExecutionMode | String to specify the parallel execution mode for vector generation with the CV engine. Only takes effect if numberOfThreads is > 1.<br>(default: "BALANCED") | "BALANCED", "ENGINES", "GOALS"

**Possible Return values**

| Return Value     | Description                      |
|------------------|------------------------------------------------------------------------------------------------------------------------------|
| 200              | Successfully generated vectors and reached all selected coverage goals (see PLL property). No robustness goals were covered. |
| 300              | Ran into timeouts before completely analyzing all selected goals (see PLL property). No robustness goals were covered.       |
| 41x              | Covered Robustness Goal: Downcast                                                                                            |
| 4x1              | Covered Robustness Goal: Division by Zero                                                                                    |
| 500              | Unexpected Error                                                                                                             |

#### Step "backToBack"

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

#### Step "regressionTest"

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

#### Step "rangeViolationGoals"

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

#### Step "domainCoverageGoals"

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
addDomainBoundaryForInputs | Flag that controls whether the goals are only applied to inputs / cals.<br>(default: false) | true, false

**Possible Return values**

| Return Value     | Description                                |
|------------------|--------------------------------------------|
| 200              | Success                                    |
| 400              | Domain Coverage Goals plugin not installed |
| 500              | Unexpected Error                           |

#### Step "addDomainCheckGoals"

DSL Command: btc.addDomainCheckGoals{...}

**Required License**

EmbeddedTester (ET\_COMPLETE)

**Description**

*Requires EP 2.9p0 or higher*

Adds Domain Check Goals to the profile which contribute to the Code
Analysis Report and can be considered during vector generation (pll:
"VDCG;IDCG"). The following optional settings are available:

Property | Description | Example Value(s)
---------|-------------|-----------------
scopePath | Hierarchy path to the targeted scope / subsystem. Leave empty to target the toplevel. Use "*" to target all scopes.<br>(default: toplevel subsystem) | "Toplevel/SubA", "*"
dcXmlPath | Path to an xml file containing Domain Coverage specs. | "DomainCoverageGoals.xml"
raster | String to specify a raster in %. Domain Coverage Goals will be created for equal according to the raster.<br>(default: 25) | "10", "25", "30"
activateRangeViolationCheck | flag that controls if range violation checks are added in the form of invalid ranges: [dataTypeMin, specifiedMin) / (specifiedMax, dataTypeMax].<br>(default: false) | true, false
activateBoundaryCheck | flag that controls if boundary value goals are added.<br>(default: false) | true, false

**Possible Return values**

| Return Value     | Description                                |
|------------------|--------------------------------------------|
| 200              | Success                                    |
| 400              | Domain Coverage Goals plugin not installed |
| 500              | Unexpected Error                           |

#### Step "addInputCombinationGoals"

DSL Command: btc.addInputCombinationGoals{...}

**Required License**
- EmbeddedTester (ET\_COMPLETE)
- User Defined Coverage Goals Add-On (ET\_UDEF\_COVGOAL)

**Description**

Adds Input Combination Goals to the profile using the User Defined Coverage Goals feature. You can define certin value regions that shall be covered and this step will add all combinations of those values for all Inputs. Due to the big number of goals this can produce (#ValueRegions<sup>#Inputs</sup>), this step is recommended for library functions or small modules and is not advised bigger systems such as software components.
- The goals created by this step will be listed in the "User Defined Coverage Goals" section of the Code Analysis Report. 
- You can add the PLL "UDCG" to the btc.vectorGeneration step to generate vectors for these goals
- ZERO will only be applied if ZERO is part of the value range. Otherwise the minimum value will be selected. 

Property | Description | Example Value(s)
---------|-------------|-----------------
valueRegions | Comma separated string with value regions.<br>MIN, MAX, CENTER, ZERO | "MIN, MAX"<br>"MIN, CENTER, MAX"

**Possible Return values**

| Return Value     | Description                                |
|------------------|--------------------------------------------|
| 200              | Success                                    |
| 400              | User Defined Goals Add-On not installed    |
| 500              | Unexpected Error                           |

#### Step "defaultTolerances"

DSL Command: btc.defaultTolerances{...}

**Description**

Adds default tolerances for floating point and fixed point outputs / locals. The default setting aims to cover minimal deviations that occur due to the precisions involved. You can change in which cases the tolerances are applied via the applyTo option. When calling this step without parameters the following tolerances will be set:
- a relative tolerance of 1E-10%
- an abolute tolerance of 1E-10 for floating point outputs/locals
- an abolute tolerance of 2*LSB for fixed point outputs/locals

Property | Description | Example Value(s)
---------|-------------|-----------------
applyTo | Controls which kinds of outputs/locals get are considered<br>FLP_OUT -> Floating Point Outputs<br>FXP_OUT -> Fixed-Point outputs<br>FLOAT_FLOAT -> Float outputs, only if there is at least 1 float Input/Parameter<br>(default: all flp & fxp outputs will be considered) | "FLP_OUT"<br>"FXP_OUT"<br>"FLOAT_FLOAT"
useCase | Controls which use case the tolerances will be valid for (B2B, RBT)<br>(default: B2B use case) | "B2B"<br>"RBT"
relTolerance | A decimal value for the relative tolerance (in percent).<br>default: 1E-10 | 2.4E-6<br>7E-14
absToleranceFlp | A decimal value for the abolute tolerance for floating point signals.<br>default: 1E-10 | 2.4E-6<br>7E-14
absToleranceFxp | A decimal value for the abolute tolerance for fixed point signals. Can be a fixed value or a factor of the resolution (lsb, controlled by fxpIsMultipleOfLsb option)<br>default: 2 | 2.4E-6<br>1
fxpIsMultipleOfLsb | Boolean flag that controls if the value specified for absToleranceFxp shall be multiplied by the resolution (lsb).<br>(default: true) | true, false
onlyToplevel | Boolean flag that controls if the tolerances shall only be applied to the toplevel scope or to all scopes<br>(default: false -> apply to all scopes) | true, false


**Possible Return values**

| Return Value     | Description                                |
|------------------|--------------------------------------------|
| 200              | Success                                    |
| 300              | Tolerances were applied to zero outputs    |
| 500              | Unexpected Error                           |

#### Step "formalTest"

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

  

#### Step "formalVerification"

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



### Reporting

#### Step "testExecutionReport"

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

#### Step "xmlReport"

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

#### Step "codeAnalysisReport"

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

#### Step "modelCoverageReport"

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

#### "overallReport"

DSL Command: btc.finalWrapUp{...}

**Description**

Property | Description | Example Value(s)
---------|-------------|-----------------
path* | Relative or absolute path of a workspace directory to temporarily store the reporting data<br>**MANDATORY** | "reports"

Creates an Overall Report to provide an overview over multiple test projects. In cases where you have a set of models and are running tests in multiple test projects, this report will summarize the overall status and provide links to the *.epp files and the individual reports. Requires all projects to have been tested on the same agent (for inter-report linking).

This is a somewhat special step that requires some preparation:
- btc.collectProjectOverview{} needs to be called for each project *before* the wrapUp method is called. This stores the projects overview data.
- btc.wrapUp needs to be called for each project in order to generate the individual project reports (Test Automation report + detailed reports)
- btc.finalWrapUp{} will take the data collected previously to create the overview report. Since the report generation works based on the BTC EmbeddedPlatform report template engine, this requires an instance of BTC EmbeddedPlatform to be available. The finalWrapUp step will then close BTC EmbeddedPlatform.

<details>
  <summary>Example of a pipeline with an overview report</summary>

  ``` groovy
  node {
      // checkout changes from SCM
      checkout scm

      // one profile for each *.slx file
      def models = findFiles glob: '*.slx'
      // start EmbeddedPlatform and connect to it
      btc.startup {}

      // create a test project for each model (for Back-to-Back Test MIL vs. SIL)
      for (modelFile in models) {
        def modelName = "$modelFile".substring(0, "$modelFile".lastIndexOf('.'))
        // load / create / update a profile
        btc.profileCreateTL {
          profilePath = "${this.modelName}.epp"
          tlModelPath = "${this.modelFile}"
          matlabVersion = "2020b"
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
        /*
        * Collect the status information regarding the current
        * project for an overview report (this enables you to
        * get an OverviewReport when you call btc.finalWrapUp later)
        */
        btc.collectProjectOverview{}
        // generate reports for this project but skip archiving, etc.
        // keep EP alive
        btc.wrapUp {
          closeEp = false
          archiveProfiles = false
          publishReports = false
          publishResults = false
        }
      }

      // create the overall report based on the collectedProjectOverview data
      btc.finalWrapUp {
        path = 'reports'
      }
  }
  ```
  
</details>

![btc-embeddedplatform-plugin-createoverallreport](https://user-images.githubusercontent.com/5657657/111972115-62919880-8afd-11eb-95c9-565eeae2e0f6.png)

**Possible Return values**

| Return Value     | Description      |
|------------------|------------------|
| 200              | Success          |
| 500              | Unexpected Error |

#### Step "interfaceReport"

DSL Command: btc.interfaceReport{...}

**Description**

Creates the Interface Report and exports it to the "exportDir"
specified in the "profileLoad" / "profileCreate" step. The following optional settings are available:

Property | Description | Example Value(s)
---------|-------------|-----------------
reportName | The filename (String) for the resulting html file.<br>(default: "InterfaceReport.html") | "report.html", "BTCInterfaceReport.html"
scopeNameRegex | String with regular expression. The report will be created for the first scope that matches this name.<br>(default: undefined -> report is created for the toplevel scope) | "some_expression.*"

**Possible Return values**

| Return Value     | Description      |
|------------------|------------------|
| 200              | Success          |
| 500              | Unexpected Error |

### Misc

#### Step "getStatusSummary"

DSL Command: btc.getStatusSummary()

**Required License**

EmbeddedTester (ET COMPLETE)

**Description**

Retrieves a struct as a json text (see below) which can be passed on to external post-processing or evalulated directly (via readJSON of the Pipeline Utility Steps plugin):

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

#### Error Handling
It is important that the EP process is closed to free the used resources and that the reserved port on the agent is released.
To ensure this in an easy manner, the DSL command btc.handleError(errorMsg) is provided. It will safely close EP, release the used port and then call the Jenkins error(...) command with the given error message.

## BTC Migration Suite

The BTC Migration Suite allows you to perform a fully automatic
regression test between different Matlab or TargetLink versions. This
makes it easy to document that the change of a tool version in a
particular project does not influence the behavior of software
components on model and code level.

**Required License**

EmbeddedTester (ET\_COMPLETE)

#### Step "migrationSource"

DSL Command: btc.migrationSource {...}

**Description**

Creates a profile on the source configuration (e.g. old Matlab /
TargetLink version), generates vectors for full coverage and exports the
simulation results.

#### Step "migrationTarget"

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
