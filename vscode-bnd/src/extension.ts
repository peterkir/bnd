import * as vscode from 'vscode';

interface CompletionData {
    label: string;
    detail: string;
    documentation: string;
    insertText?: string;
}

const INSTRUCTIONS: CompletionData[] = [
    { label: '-augment', detail: "-augment PARAMETER ( ',' PARAMETER ) *", documentation: 'Add requirements and capabilities to the resources during resolving.' },
    { label: '-baseline', detail: '-baseline selector', documentation: 'Control what bundles are enabled for baselining and optionally specify the baseline repository.' },
    { label: '-baselinerepo', detail: '-baselinerepo qname', documentation: 'Define the repository to calculate baselining against.' },
    { label: '-bnd-driver', detail: '-bnd-driver', documentation: 'Sets the driver property.' },
    { label: '-builderignore', detail: "-builderignore PATH-SPEC ( ',' PATH-SPEC ) *", documentation: 'List of project-relative directories to be ignored by the builder.' },
    { label: '-buildpath', detail: '-buildpath PATH', documentation: 'Provides the class path for building the jar, the entries are references to the workspace repositories.' },
    { label: '-buildrepo', detail: "-buildrepo repo ( ',' repo ) *", documentation: 'After building a JAR, release the JAR to the given repositories.' },
    { label: '-buildtool', detail: '-buildtool toolspec', documentation: 'A specification for the bnd CLI to install a build tool, like gradle, in the workspace.' },
    { label: '-bumppolicy', detail: '-bumppolicy', documentation: 'The policy for the bump command.' },
    { label: '-bundleannotations', detail: '-bundleannotations SELECTORS', documentation: 'Selects the classes that need processing for standard OSGi Bundle annotations.' },
    { label: '-cdiannotations', detail: '-cdiannotations SELECTORS', documentation: 'Selects the packages that need processing for CDI annotations.' },
    { label: '-check', detail: "-check 'ALL' | ( 'IMPORTS' | 'EXPORTS' ) *", documentation: 'Enable additional checking.' },
    { label: '-classpath', detail: "-classpath FILE (',' FILE) *", documentation: 'Specify additional file based entries (either directories or JAR files) to add to the classpath.' },
    { label: '-compression', detail: '-compression DEFLATE | STORE', documentation: 'Set the compression level for the generated JAR, the default is DEFLATE.' },
    { label: '-conditionalpackage', detail: "-conditionalpackage PACKAGE-SPEC ( ',' PACKAGE-SPEC ) *", documentation: 'Recursively add packages from the class path when referred and when they match one of the package specs.' },
    { label: '-conduit', detail: '-conduit', documentation: 'This project is a front to one or more JARs in the file system.' },
    { label: '-connection-settings', detail: '-connection-settings', documentation: 'Setting up the communications for bnd.' },
    { label: '-consumer-policy', detail: '-consumer-policy VERSION-MASK', documentation: 'Specify the default version bump policy for a consumer when a binary incompatible change is detected.' },
    { label: '-contract', detail: '-contract', documentation: 'Establishes a link to a contract and handles the low level details.' },
    { label: '-define-contract', detail: '-define-contract', documentation: 'Define a contract when one cannot be added to the buildpath.' },
    { label: '-dependson', detail: '-dependson SELECTORS', documentation: 'Add dependencies from the current project to other projects, before this project is built.' },
    { label: '-deploy', detail: '-deploy', documentation: 'Deploy the current project to a repository through Deploy plugins (e.g. MavenDeploy).' },
    { label: '-deployrepo', detail: '-deployrepo', documentation: 'Specifies to which repo the project should be deployed.' },
    { label: '-diffignore', detail: '-diffignore SELECTORS', documentation: 'Manifest header names and resource paths to ignore during baseline comparison.' },
    { label: '-diffpackages', detail: '-diffpackages SELECTORS', documentation: 'The names of exported packages to baseline.' },
    { label: '-digests', detail: "-digests DIGEST ( ',' DIGEST ) *", documentation: 'Set the digest algorithms to use.' },
    { label: '-distro', detail: "-distro REPO (',' REPO)", documentation: 'Resolve against pre-defined system capabilities.' },
    { label: '-donotcopy', detail: '-donotcopy', documentation: 'Set the default filters for file resources that should not be copied.' },
    { label: '-dsannotations', detail: '-dsannotations SELECTORS', documentation: 'Selects the packages that need processing for standard OSGi DS annotations.' },
    { label: '-dsannotations-options', detail: '-dsannotations-options SELECTORS', documentation: 'Options for controlling DS annotation processing.' },
    { label: '-eeprofile', detail: "-eeprofile 'auto' | PROFILE +", documentation: 'Provides control over what Java 8 profile to use.' },
    { label: '-executable', detail: '-executable', documentation: 'Process an executable jar to strip optional directories of the contained bundles.' },
    { label: '-export', detail: "-export PATH ( ';' PARAMETER )* ( ',' PATH ( ';' PARAMETER )* )*", documentation: 'Turns a bndrun file into its deployable format.' },
    { label: '-export-apiguardian', detail: "-export-apiguardian PACKAGE-SPEC, ( ',' PACKAGE-SPEC )*", documentation: 'Exports the given packages where the `@API` annotation is found on contained classes.' },
    { label: '-exportcontents', detail: "-exportcontents PACKAGE-SPEC, ( ',' PACKAGE-SPEC )*", documentation: 'Exports the given packages but does not try to include them from the class path.' },
    { label: '-exportreport', detail: "-exportreport report-def ( ',' report-def )*", documentation: 'Configure a list of reports to be exported.' },
    { label: '-exporttype', detail: '-exporttype', documentation: 'This specifies the type of the exported content.' },
    { label: '-extension', detail: '-extension', documentation: 'A plugin that is loaded to its url, downloaded and then provides a header used in OSGi.' },
    { label: '-failok', detail: "-failok ('true' | 'false')?", documentation: 'Will ignore any error during building and assume all went ok.' },
    { label: '-fixupmessages', detail: '-fixupmessages SELECTOR', documentation: 'Fixup errors and warnings.' },
    { label: '-generate', detail: "-generate srcs ';output=' DIR", documentation: 'Generate sources.' },
    { label: '-gestalt', detail: '-gestalt', documentation: 'Provides access to the gestalt properties that describe the environment.' },
    { label: '-groupid', detail: '-groupid groupId', documentation: 'Set the default Maven groupId.' },
    { label: '-include', detail: "-include PATH-SPEC ( ',' PATH-SPEC ) *", documentation: 'Include a number of files from the file system.' },
    { label: '-includepackage', detail: "-includepackage PACKAGE-SPEC, ( ',' PACKAGE-SPEC )*", documentation: 'Include a number of packages from the class path.' },
    { label: '-includeresource', detail: '-includeresource iclause', documentation: 'Include resources from the file system.' },
    { label: '-init', detail: '-init ${MACRO}', documentation: 'Executes the macros while initializing the project for building.' },
    { label: '-invalidfilenames', detail: '-invalidfilenames', documentation: 'Specify file/directory names that should not be used because they are not portable.' },
    { label: '-javaagent', detail: '-javaagent BOOLEAN', documentation: 'Specify if classpath jars with Premain-Class headers are to be used as java agents.' },
    { label: '-jpms-module-info', detail: '-jpms-module-info modulename', documentation: 'Used to generate the `module-info.class`.' },
    { label: '-jpms-module-info-options', detail: '-jpms-module-info-options module-infos+', documentation: 'Used to control generation of the `module-info.class`.' },
    { label: '-jpms-multi-release', detail: '-jpms-multi-release BOOLEAN', documentation: 'Enables generating manifests and module infos for multi release JARs.' },
    { label: '-launcher', detail: '-launcher', documentation: 'Options for the runtime launcher.' },
    { label: '-library', detail: "-library library ( ',' library )*", documentation: 'Apply a bnd library to the workspace, project, or bndrun file.' },
    { label: '-make', detail: '-make', documentation: 'If a resource is not found, specify a recipe to make it.' },
    { label: '-manifest', detail: '-manifest FILE', documentation: 'Override manifest calculation and set fixed manifest.' },
    { label: '-manifest-name', detail: '-manifest-name RESOURCE', documentation: 'Set the resource path to the manifest.' },
    { label: '-maven-dependencies', detail: "-maven-dependencies* entry ( ',' entry )*", documentation: 'Configure maven dependency information for the generated pom.' },
    { label: '-maven-release', detail: "-maven-release ('local'|'remote') ( ',' option )*", documentation: 'Set the Maven release options for the Maven Bnd Repository.' },
    { label: '-maven-scope', detail: '-maven-scope dependency-scope', documentation: 'Set the default Maven dependency scope to use when generating dependency information.' },
    { label: '-metainf-services', detail: '-metainf-services', documentation: 'Controls how META-INF/services files are processed.' },
    { label: '-metatypeannotations', detail: '-metatypeannotations SELECTORS', documentation: 'Selects the packages that need processing for standard OSGi Metatype annotations.' },
    { label: '-metatypeannotations-options', detail: '-metatypeannotations-options SELECTORS', documentation: 'Restricts the use of Metatype Annotation to a minimum version.' },
    { label: '-namesection', detail: "-namesection RESOURCE-SPEC ( ',' RESOURCE-SPEC ) *", documentation: 'Create a name section (second part of manifest) with optional property expansion.' },
    { label: '-nobuildincache', detail: '-nobuildincache BOOLEAN', documentation: 'Do not use a build in cache for the launcher and JUnit.' },
    { label: '-nobundles', detail: '-nobundles BOOLEAN', documentation: 'Do not build the project.' },
    { label: '-noclassforname', detail: '-noclassforname BOOLEAN', documentation: 'Do not add package reference to classes loaded with Class.forName(String).' },
    { label: '-nodefaultversion', detail: '-nodefaultversion BOOLEAN', documentation: 'Do not add a default version to exported packages when no version is present.' },
    { label: '-noee', detail: '-noee BOOLEAN', documentation: 'Do not add an automatic requirement on an EE capability based on the class format.' },
    { label: '-noextraheaders', detail: '-noextraheaders BOOLEAN', documentation: 'Do not add any extra headers specific for bnd.' },
    { label: '-noimportjava', detail: '-noimportjava BOOLEAN', documentation: 'Do not import java.* packages.' },
    { label: '-nojunit', detail: '-nojunit BOOLEAN', documentation: 'Indicate that this project does not have JUnit tests.' },
    { label: '-nojunitosgi', detail: '-nojunitosgi BOOLEAN', documentation: 'Indicate that this project does not have JUnit OSGi tests.' },
    { label: '-nomanifest', detail: '-nomanifest BOOLEAN', documentation: 'Do not save the manifest in the JAR.' },
    { label: '-noparallel', detail: '-noparallel CATEGORY;task=TASKS', documentation: 'Prevent Gradle tasks in the same category from executing in parallel.' },
    { label: '-noproxyinterfaces', detail: '-noproxyinterfaces BOOLEAN', documentation: "Do not calculate Import-Package references for 'Proxy.newProxyInstance' usage." },
    { label: '-nosubstitution', detail: '-nosubstitution', documentation: 'Setting this to true disables package substitution globally (default is false).' },
    { label: '-nouses', detail: '-nouses BOOLEAN', documentation: 'Do not calculate uses directives on package exports or on capabilities.' },
    { label: '-output', detail: '-output FILE', documentation: 'Specify the output directory or file.' },
    { label: '-outputmask', detail: '-outputmask TEMPLATE', documentation: 'If set, is used as a template to calculate the output file.' },
    { label: '-packageinfotype', detail: '-packageinfotype', documentation: 'Sets the different types of package info.' },
    { label: '-pedantic', detail: '-pedantic BOOLEAN', documentation: 'Warn about things that are not really wrong but still not right.' },
    { label: '-plugin', detail: "-plugin.* plugin-def ( ',' plugin-def )*", documentation: 'Load plugins and their parameters.' },
    { label: '-pluginpath', detail: '-pluginpath* PARAMETERS', documentation: 'Define JARs to be loaded in the local classloader for plugins.' },
    { label: '-pom', detail: '-pom BOOLEAN | PROPERTIES', documentation: 'Generate a maven pom in the JAR.' },
    { label: '-prepare', detail: "-prepare makespec ( ',' makespec )*", documentation: 'Execute a number of shell commands before every build.' },
    { label: '-preprocessmatchers', detail: '-preprocessmatchers SELECTOR', documentation: 'Specify which files can be preprocessed.' },
    { label: '-privatepackage', detail: '-privatepackage PACKAGE-SPEC', documentation: 'Specify the private packages; these packages are included from the class path.' },
    { label: '-profile', detail: '-profile KEY', documentation: 'Sets a prefix that is used when a variable is not found, it is then re-searched with the prefix.' },
    { label: '-provider-policy', detail: '-provider-policy VERSION-MASK', documentation: 'Specify the default version bump policy for a provider when a binary incompatible change is detected.' },
    { label: '-releaserepo', detail: "-releaserepo* NAME ( ',' NAME ) *", documentation: 'Define the names of the repositories to use for a release.' },
    { label: '-remoteworkspace', detail: '-remoteworkspace (true|false)', documentation: 'Enable the workspace to serve remote requests from the local system.' },
    { label: '-removeheaders', detail: '-removeheaders KEY-SELECTOR', documentation: 'Remove matching headers from the manifest.' },
    { label: '-reportconfig', detail: "-reportconfig plugin-def ( ',' plugin-def )*", documentation: 'Configure the content of a report.' },
    { label: '-reportnewer', detail: '-reportnewer BOOLEAN', documentation: 'Report any entries that were added to the build since the last JAR was made.' },
    { label: '-reproducible', detail: '-reproducible BOOLEAN | TIMESTAMP', documentation: 'Ensure the bundle can be built in a reproducible manner.' },
    { label: '-require-bnd', detail: '-require-bnd (FILTER)*', documentation: 'Require a minimum version of bnd. The filter can test against `version`.' },
    { label: '-resolve', detail: '-resolve (manual|auto|beforelaunch|batch|cache)', documentation: 'Defines when/how resolving is done to calculate the -runbundles.' },
    { label: '-resolve.effective', detail: "-resolve.effective qname (',' qname )", documentation: 'Set the use effectives for the resolver.' },
    { label: '-resolve.excludesystem', detail: '-resolve.excludesystem true|false', documentation: 'If set to true (default) it excludes the system bundle from the resolve.' },
    { label: '-resolve.preferences', detail: "-resolve.preferences qname ( ',' qname )", documentation: 'Override the default order and selection of repositories.' },
    { label: '-resolve.reject', detail: '-resolve.reject namespace', documentation: 'Controls rejection of capabilities during resolving.' },
    { label: '-resolvedebug', detail: '-resolvedebug INTEGER', documentation: 'Display debugging information for a resolve operation.' },
    { label: '-resourceonly', detail: '-resourceonly BOOLEAN', documentation: 'Ignores warning if the bundle only contains resources and no classes.' },
    { label: '-runblacklist', detail: "-runblacklist requirement (',' requirement)", documentation: 'Blacklist a set of bundles for a resolve operation.' },
    { label: '-runbuilds', detail: '-runbuilds BOOLEAN', documentation: 'Defines if this should add the bundles built by this project to the -runbundles.' },
    { label: '-runbundles', detail: "-runbundles* REPO-ENTRY ( ',' REPO-ENTRY )*", documentation: 'Add additional bundles, specified with their bsn and version like in -buildpath, to the runtime.' },
    { label: '-runee', detail: '-runee EE', documentation: 'Define the runtime Execution Environment capabilities, default Java 6.' },
    { label: '-runenv', detail: '-runenv PROPERTIES', documentation: 'Specify environment variables for the launched JVM process.' },
    { label: '-runframework', detail: "-runframework ( 'none' | 'services' | ANY )?", documentation: "Sets the type of framework to run. If 'none', an internal dummy framework is used." },
    { label: '-runframeworkrestart', detail: '-runframeworkrestart BOOLEAN', documentation: 'Restart the framework in the same VM if the framework is stopped or updated.' },
    { label: '-runfw', detail: '-runfw REPO-ENTRY', documentation: "Specify the framework JAR's entry in a repository." },
    { label: '-runjdb', detail: '-runjdb ADDRESS', documentation: 'Specify a JDB socket transport address on invocation when launched outside a debugger.' },
    { label: '-runkeep', detail: '-runkeep true | false', documentation: 'Decides whether to keep the framework storage directory between launches.' },
    { label: '-runnoreferences', detail: '-runnoreferences BOOLEAN', documentation: 'Do not use the `reference:` URL scheme for installing a bundle in the installer.' },
    { label: '-runoptions', detail: '-runoptions', documentation: 'Options for the launch.' },
    { label: '-runpath', detail: "-runpath REPO-ENTRY ( ',' REPO-ENTRY )", documentation: 'Additional JARs for the remote VM path, should include the framework.' },
    { label: '-runprogramargs', detail: '-runprogramargs', documentation: 'Additional arguments for the program invocation.' },
    { label: '-runproperties', detail: '-runproperties PROPERTIES', documentation: 'Define system properties for the remote VM.' },
    { label: '-runprovidedcapabilities', detail: '-runprovidedcapabilities', documentation: 'Extra capabilities for a distro resolve.' },
    { label: '-runremote', detail: '-runremote', documentation: 'It provides remote debugging support for bnd projects.' },
    { label: '-runrepos', detail: "-runrepos REPO-NAME ( ',' REPO-NAME )*", documentation: 'Order and select the repository for resolving against.' },
    { label: '-runrequires', detail: "-runrequires REQUIREMENT ( ',' REQUIREMENT )*", documentation: 'The root requirements for a resolve intended to create a constellation for the -runbundles.' },
    { label: '-runstartlevel', detail: '-runstartlevel ( order | begin | step )*', documentation: 'Assign a start level to each run-bundle after resolving.' },
    { label: '-runstorage', detail: '-runstorage FILE', documentation: "Define the directory to use for the framework's work area." },
    { label: '-runsystemcapabilities', detail: "-runsystemcapabilities* CAPABILITY (',' CAPABILITY )", documentation: 'Define extra capabilities for the remote VM.' },
    { label: '-runsystempackages', detail: '-runsystempackages* PARAMETERS', documentation: 'Define extra system packages (packages exported from the remote VM -runpath).' },
    { label: '-runtimeout', detail: '-runtimeout DURATION', documentation: 'Set a timeout for the test run.' },
    { label: '-runtrace', detail: '-runtrace BOOLEAN', documentation: 'Trace the launched process in detail.' },
    { label: '-runvm', detail: '-runvm KEYS', documentation: 'Additional arguments for the VM invocation. Arguments are added as-is.' },
    { label: '-savemanifest', detail: '-savemanifest FILE', documentation: 'Write out the manifest to a separate file after it has been calculated.' },
    { label: '-sign', detail: '-sign PARAMETERS', documentation: 'Sign the JAR.' },
    { label: '-snapshot', detail: '-snapshot STRING', documentation: 'String to substitute for "SNAPSHOT" in the bundle version\'s qualifier.' },
    { label: '-sourcepath', detail: '-sourcepath', documentation: 'List of directory names used to find sources.' },
    { label: '-sources', detail: '-sources BOOLEAN', documentation: 'Include the source code (if available on the -sourcepath) in the bundle at OSGI-OPT/src.' },
    { label: '-stalecheck', detail: "-stalecheck srcs ';newer=' depends", documentation: 'Perform a stale check of files and directories before building a jar.' },
    { label: '-standalone', detail: "-standalone repo-spec (, repo-spec )", documentation: 'Disconnects the bndrun file from the workspace and defines its own repositories.' },
    { label: '-strict', detail: '-strict BOOLEAN', documentation: 'If strict is true, then extra verification is done.' },
    { label: '-sub', detail: "-sub FILE-SPEC ( ',' FILE-SPEC )*", documentation: 'Enable sub-bundles to build a set of .bnd files that use bnd.bnd file as a basis.' },
    { label: '-systemproperties', detail: '-systemproperties PROPERTIES', documentation: 'These system properties are set in the local JVM when a workspace is started.' },
    { label: '-testcontinuous', detail: '-testcontinuous BOOLEAN', documentation: 'Do not exit after running the test suites but keep watching the bundles and rerun.' },
    { label: '-tester', detail: '-tester REPO-SPEC', documentation: 'Specifies the tester (bundle) that is supposed to test the code.' },
    { label: '-testpackages', detail: "-testpackages PACKAGE-SPEC ( ',' PACKAGE-SPEC )", documentation: 'Specifies the test packages.' },
    { label: '-testpath', detail: "-testpath REPO-SPEC ( ',' REPO-SPEC )", documentation: "The specified JARs from a repository are added to the remote JVM's classpath if testing." },
    { label: '-testsources', detail: "-testsources REGEX ( ',' REGEX )*", documentation: 'Specification to find JUnit test cases by traversing the test src directory.' },
    { label: '-testunresolved', detail: '-testunresolved BOOLEAN', documentation: 'Will execute a JUnit testcase ahead of any other test case that will abort if there are unresolved bundles.' },
    { label: '-undertest', detail: '-undertest true', documentation: 'Will be set by the project when it builds a JAR in test mode.' },
    { label: '-upto', detail: '-upto VERSION', documentation: 'Specify the highest compatibility version; will disable any incompatible features.' },
    { label: '-wab', detail: "-wab FILE ( ',' FILE )*", documentation: 'Create a Web Archive Bundle (WAB) or a WAR.' },
    { label: '-wablib', detail: "-wablib FILE ( ',' FILE )*", documentation: 'Specify the libraries that must be included in a Web Archive Bundle (WAB) or WAR.' },
    { label: '-workingset', detail: "-workingset PARAMETER ( ',' PARAMETER ) *", documentation: 'Group the workspace into different working sets.' },
    { label: '-workspace-templates', detail: '-workspace-templates', documentation: 'Define workspace templates for a new workspace.' },
    { label: '-x-overwritestrategy', detail: '-x-overwritestrategy', documentation: 'On Windows we sometimes cannot delete a file because someone holds a lock.' },
];

const MACROS: CompletionData[] = [
    { label: 'apply', detail: "apply ';' MACRO (';' LIST)*", documentation: 'Convert a list to an invocation with arguments.' },
    { label: 'average', detail: "average (';' LIST )*", documentation: 'Calculate the arithmetic mean (average) of numeric values in one or more lists.' },
    { label: 'base64', detail: "base64 ';' FILE [';' LONG ]", documentation: "Encode a file's contents as Base64 text." },
    { label: 'basedir', detail: 'basedir', documentation: 'Get the base directory of the current processor context.' },
    { label: 'basename', detail: "basename ( ';' FILEPATH ) +", documentation: 'Extract the filename from one or more file paths.' },
    { label: 'basenameext', detail: "basenameext ';' PATH ( ';' EXTENSION )", documentation: 'The basename of the given path optionally minus a specified extension.' },
    { label: 'bndversion', detail: 'bndversion', documentation: 'Returns the current running bnd version as full major.minor.micro.' },
    { label: 'bsn', detail: 'bsn', documentation: 'Get the Bundle Symbolic Name (BSN) of the current bundle being built.' },
    { label: 'bytes', detail: "bytes ( ';' LONG )*", documentation: 'Format a byte count into human-readable size unit.' },
    { label: 'cat', detail: "cat ';' FILEPATH", documentation: 'Read and return the contents of a file or URL.' },
    { label: 'classes', detail: "classes ( ; QUERY ( ; PATTERN )? )*", documentation: 'A list of class names filtered by a query language.' },
    { label: 'compare', detail: 'compare STRING STRING', documentation: 'Compare two strings by using the compareTo method of the String class.' },
    { label: 'currenttime', detail: 'currenttime', documentation: 'Get the current system time as milliseconds since epoch.' },
    { label: 'decorated', detail: "decorated ';' NAME [ ';' BOOLEAN ]", documentation: 'The merged and decorated Parameters object.' },
    { label: 'def', detail: "def ';' KEY (';' STRING)?", documentation: 'Get a property value with an optional default.' },
    { label: 'digest', detail: "digest ';' ALGORITHM ';' FILE", documentation: 'Calculate a cryptographic digest (hash) of a file.' },
    { label: 'dir', detail: "dir ( ';' FILE )*", documentation: 'Extract the directory path from one or more file paths.' },
    { label: 'driver', detail: "driver ( ';' NAME )?", documentation: 'Get or check the build environment driver (gradle, eclipse, intellij, etc.).' },
    { label: 'ee', detail: 'ee', documentation: 'Get the highest Java Execution Environment (EE) required by the bundle.' },
    { label: 'endswith', detail: "endswith ';' STRING ';' SUFFIX", documentation: 'Check if a string ends with a specific suffix.' },
    { label: 'env', detail: "env ';' KEY (';' STRING)?", documentation: 'Get an environment variable with an optional default.' },
    { label: 'error', detail: "error ( ';' STRING )*", documentation: 'Generate a build error with a custom message.' },
    { label: 'exporters', detail: "exporters ';' PACKAGE", documentation: 'List JARs on the classpath that export a given package.' },
    { label: 'exports', detail: 'exports', documentation: 'Get the list of packages exported by the current bundle.' },
    { label: 'extension', detail: "extension ';' PATH", documentation: 'The file extension of the given path or empty string if no extension.' },
    { label: 'fileuri', detail: "fileuri ';' PATH", documentation: 'Return a file URI for the specified path. Relative paths are resolved against the domain processor base directory.' },
    { label: 'filter', detail: "filter ';' LIST ';' REGEX", documentation: 'Filter a list to include only entries matching a regular expression.' },
    { label: 'filterout', detail: "filterout ';' LIST ';' REGEX", documentation: 'Filter a list to exclude entries matching a regular expression.' },
    { label: 'find', detail: "find ';' VALUE ';' SEARCHED", documentation: 'Find the index position of a substring in a string.' },
    { label: 'findfile', detail: "findfile ';' PATH ( ';' FILTER )", documentation: 'Get filtered list of file paths from a directory tree.' },
    { label: 'findlast', detail: "findlast ';' VALUE ';' SEARCHED", documentation: 'Find the last occurrence of a substring in a string.' },
    { label: 'findname', detail: "findname ';' REGEX ( ';' REPLACEMENT )?", documentation: 'Find bundle resources by filename with optional regex replacement.' },
    { label: 'findpath', detail: "findpath ';' REGEX ( ';' REPLACE )?", documentation: 'Find bundle resources by full path with optional regex replacement.' },
    { label: 'findproviders', detail: "findproviders ';' namespace ( ';' FILTER ( ';' STRATEGY)? )?", documentation: 'Find resources in the workspace repository matching the given namespace and optional filter.' },
    { label: 'first', detail: "first (';' LIST )*", documentation: 'Get the first element from one or more lists.' },
    { label: 'fmodified', detail: "fmodified ( ';' RESOURCE )+", documentation: 'Get the latest modification timestamp from a list of files.' },
    { label: 'foreach', detail: "foreach ';' MACRO (';' LIST)*", documentation: 'Iterate over a list, calling a macro for each element with value and index.' },
    { label: 'format', detail: "format ';' STRING (';' ANY )*", documentation: 'Print a formatted string using Locale.ROOT.' },
    { label: 'frange', detail: "frange ';' VERSION ( ';' BOOLEAN )?", documentation: 'Generate an OSGi filter expression for a version range.' },
    { label: 'gestalt', detail: "gestalt ';' NAME ( ';' NAME (';' ANY )? )?", documentation: 'Access environment description properties (gestalt).' },
    { label: 'get', detail: "get ';' INDEX (';' LIST )*", documentation: 'Get an element from a list at a specific index.' },
    { label: 'githead', detail: 'githead', documentation: 'Get the Git commit SHA of the current HEAD.' },
    { label: 'glob', detail: "glob ';' GLOBEXP", documentation: 'Convert a glob pattern to a regular expression.' },
    { label: 'global', detail: "global ';' KEY ( ';' DEFAULT )?", documentation: 'Access user settings from the ~/.bnd/settings.json file.' },
    { label: 'ide', detail: "ide ';' ( 'javac.target' | 'javac.source' )", documentation: 'Read Java compiler settings from Eclipse IDE configuration.' },
    { label: 'if', detail: "if ';' STRING ';' STRING ( ';' STRING )?", documentation: 'Conditional macro that depending on a condition returns either a value for true or false.' },
    { label: 'imports', detail: 'imports', documentation: 'Get the list of packages imported by the current bundle.' },
    { label: 'indexof', detail: "indexof ';' STRING (';' LIST )*", documentation: 'Find the index position of a value in one or more lists.' },
    { label: 'is', detail: "is ( ';' ANY )*", documentation: 'Check if all given values are equal.' },
    { label: 'isdir', detail: "isdir ( ';' FILE )+", documentation: 'Check if all specified paths are directories.' },
    { label: 'isempty', detail: "isempty ( ';' STRING )*", documentation: 'Check if all given strings are empty.' },
    { label: 'isfile', detail: "isfile (';' FILE )+", documentation: 'Check if all specified paths are regular files.' },
    { label: 'isnumber', detail: "isnumber ( ';' STRING )*", documentation: 'Check if all given strings are valid numbers.' },
    { label: 'join', detail: "join ( ';' LIST )+", documentation: 'Combine multiple lists into a single comma-separated list.' },
    { label: 'js', detail: "js (';' JAVASCRIPT )*", documentation: 'Execute JavaScript expressions and return the result.' },
    { label: 'last', detail: "last (';' LIST )*", documentation: 'Get the last element from one or more lists.' },
    { label: 'lastindexof', detail: "lastindexof ';' STRING (';' LIST )*", documentation: 'Find the last index position of a value in one or more lists.' },
    { label: 'length', detail: 'length STRING', documentation: 'Get the length of a string in characters.' },
    { label: 'list', detail: "list (';' KEY)*", documentation: 'Returns a list of the values of the named properties with escaped semicolons.' },
    { label: 'literal', detail: "literal ';' STRING", documentation: 'Prevent macro expansion by wrapping a value with macro delimiters.' },
    { label: 'long2date', detail: 'long2date', documentation: 'Convert a millisecond timestamp to a readable date string.' },
    { label: 'lsa', detail: "lsa ';' DIR (';' SELECTORS )", documentation: 'List files with absolute paths, optionally filtered.' },
    { label: 'lsr', detail: "lsr ';' DIR (';' SELECTORS )", documentation: 'List files with relative paths, optionally filtered.' },
    { label: 'map', detail: "map ';' MACRO (';' LIST)*", documentation: 'Transform each element of a list using a macro function.' },
    { label: 'matches', detail: 'matches STRING REGEX', documentation: 'Check if a string matches a regular expression pattern.' },
    { label: 'maven_version', detail: "maven_version ';' MAVEN-VERSION", documentation: 'Cleanup a potential maven version to make it match an OSGi Version syntax.' },
    { label: 'max', detail: "max (';' LIST )*", documentation: 'Find the maximum string in one or more lists.' },
    { label: 'md5', detail: "md5 ';' RESOURCE", documentation: 'Calculate MD5 digest of a resource in the bundle.' },
    { label: 'min', detail: "min (';' LIST )*", documentation: 'Find the minimum string in one or more lists.' },
    { label: 'native_capability', detail: 'native_capability', documentation: 'Generate OSGi native capability string for current or specified platform.' },
    { label: 'ncompare', detail: 'ncompare NUMBER NUMBER', documentation: 'Compare two numbers by using the Double.compare method.' },
    { label: 'nmax', detail: "nmax (';' LIST )*", documentation: 'Find the maximum number in one or more lists.' },
    { label: 'nmin', detail: "nmin (';' LIST )*", documentation: 'Find the minimum number in one or more lists.' },
    { label: 'now', detail: "now ( 'long' | DATEFORMAT )", documentation: 'Get current date and time in various formats.' },
    { label: 'nsort', detail: "nsort (';' LIST )+", documentation: 'Sort lists numerically by treating values as numbers.' },
    { label: 'osfile', detail: "osfile ';' DIR ';' NAME", documentation: 'Create an OS-specific absolute file path.' },
    { label: 'p_allsourcepath', detail: 'p_allsourcepath', documentation: 'Get paths to all source directories.' },
    { label: 'p_bootclasspath', detail: 'p_bootclasspath', documentation: "Get the project's boot classpath." },
    { label: 'p_buildpath', detail: 'p_buildpath', documentation: "Get the project's buildpath as a list." },
    { label: 'p_dependson', detail: 'p_dependson', documentation: 'Get list of project names this project depends on.' },
    { label: 'p_output', detail: 'p_output', documentation: "Get the absolute path to the project's output directory." },
    { label: 'p_sourcepath', detail: 'p_sourcepath', documentation: "Get the project's source directories." },
    { label: 'p_testpath', detail: 'p_testpath', documentation: "Get the project's test runtime path." },
    { label: 'packageattribute', detail: "packageattribute ';' PACKAGE (';' ATTRIBUTE)?", documentation: 'The value of a package attribute.' },
    { label: 'packages', detail: 'packages', documentation: 'A list of package names filtered by a query language.' },
    { label: 'path', detail: "path ( ';' FILES )+", documentation: 'Join file paths with the OS path separator.' },
    { label: 'pathseparator', detail: 'pathseparator', documentation: "Get the operating system's path separator character." },
    { label: 'permissions', detail: "permissions (';' ( 'packages' | 'admin' | 'permissions' ) )+", documentation: 'Generate OSGi permission declarations for the bundle.' },
    { label: 'propertiesdir', detail: 'propertiesdir', documentation: 'Get the directory containing the current properties file.' },
    { label: 'propertiesname', detail: 'propertiesname', documentation: 'Get the filename of the current properties file.' },
    { label: 'rand', detail: "rand (';' MIN (';' MAX )?)?", documentation: 'Generate a random number within a specified range.' },
    { label: 'random', detail: 'random', documentation: 'Generate a random string that is a valid Java identifier.' },
    { label: 'range', detail: "range ';' RANGE_MASK ( ';' VERSION )", documentation: 'Create a semantic version range out of a version using a mask to control the bump of the ceiling.' },
    { label: 'reject', detail: "reject ';' LIST ';' REGEX", documentation: 'Rejects a list by matching it against a regular expression.' },
    { label: 'removeall', detail: "removeall ';' LIST ';' LIST", documentation: 'Return the first list where items from the second list are removed.' },
    { label: 'replace', detail: "replace ';' LIST ';' REGEX (';' STRING (';' STRING)? )?", documentation: 'Replace parts of list elements using regex patterns.' },
    { label: 'replacelist', detail: "replacelist ';' LIST ';' REGEX (';' STRING (';' STRING)? )?", documentation: 'Replace parts of list elements using regex with quoted section support.' },
    { label: 'replacestring', detail: "replacestring ';' STRING ';' REGEX (';' STRING )?", documentation: 'Replace parts of a string using regex patterns.' },
    { label: 'repo', detail: "repo ';' BSN ( ';' VERSION ( ';' STRATEGY )? )?", documentation: 'Provides the file paths to artifact in the repositories.' },
    { label: 'repodigests', detail: "repodigests ( ';' NAME )*", documentation: 'Get cryptographic digests of repository contents.' },
    { label: 'repos', detail: 'repos', documentation: 'Get a list of configured repository names.' },
    { label: 'retainall', detail: "retainall ';' LIST ';' LIST", documentation: 'Return the first list where items not in the second list are removed.' },
    { label: 'reverse', detail: "reverse (';' LIST )*", documentation: 'Reverse the order of elements in one or more lists.' },
    { label: 'select', detail: "select ';' LIST ';' REGEX", documentation: 'Selects entries in a list that match a regular expression.' },
    { label: 'separator', detail: 'separator', documentation: "Get the operating system's file separator character." },
    { label: 'sha1', detail: "sha1 ';' RESOURCE", documentation: 'Calculate SHA-1 digest of a resource in the bundle.' },
    { label: 'size', detail: "size ( ';' LIST )*", documentation: 'Count the total number of elements in one or more lists.' },
    { label: 'sjoin', detail: "sjoin ';' SEPARATOR ( ';' LIST )+", documentation: 'Join lists with a custom separator.' },
    { label: 'sort', detail: "sort (';' LIST )+", documentation: 'Sort lists alphabetically.' },
    { label: 'split', detail: "split ';' REGEX (';' STRING )*", documentation: 'Split strings into a list using a regular expression.' },
    { label: 'startswith', detail: "startswith ';' STRING ';' PREFIX", documentation: 'Check if a string starts with a specific prefix.' },
    { label: 'stem', detail: "stem ';' STRING", documentation: 'Extract the portion of a string before the first dot.' },
    { label: 'sublist', detail: "sublist ';' START ';' END (';' LIST )*", documentation: 'Extract a portion of a list with support for negative indices.' },
    { label: 'subst', detail: "subst ';' STRING ';' REGEX (';' STRING (';' NUMBER )? )?", documentation: 'Substitute all the regex matches in the target for the given value.' },
    { label: 'substring', detail: "substring ';' STRING ';' START ( ';' END )?", documentation: 'Extract a substring from a string with support for negative indices.' },
    { label: 'sum', detail: "sum (';' LIST )*", documentation: 'Calculate the sum of numeric values in one or more lists.' },
    { label: 'system', detail: "system ';' STRING ( ';' STRING )?", documentation: 'Execute a system command and return its output.' },
    { label: 'system_allow_fail', detail: "system_allow_fail ';' STRING ( ';' STRING )?", documentation: 'Execute a system command, returning output and ignoring failures.' },
    { label: 'template', detail: "template ';' NAME [ ';' template ]+", documentation: 'Expand the entries of a merged and decorated Parameters object using a template.' },
    { label: 'thisfile', detail: 'thisfile', documentation: 'Get the absolute path of the current properties file.' },
    { label: 'toclasspath', detail: "toclasspath ';' LIST ( ';' BOOLEAN )?", documentation: 'Convert class names to file paths.' },
    { label: 'toclassname', detail: "toclassname ';' FILES", documentation: 'Convert file paths to fully qualified class names.' },
    { label: 'tolower', detail: 'tolower STRING', documentation: 'Convert a string to lowercase.' },
    { label: 'toupper', detail: 'toupper STRING', documentation: 'Convert a string to uppercase.' },
    { label: 'trim', detail: "trim ';' STRING", documentation: 'Remove leading and trailing whitespace from a string.' },
    { label: 'tstamp', detail: "tstamp ( ';' DATEFORMAT ( ';' TIMEZONE ( ';' LONG )? )? )?", documentation: 'Generate a formatted timestamp.' },
    { label: 'unescape', detail: "unescape ( ';' STRING )*", documentation: 'Convert escape sequences to their control characters.' },
    { label: 'uniq', detail: "uniq (';' LIST )*", documentation: 'Remove duplicate elements from one or more lists.' },
    { label: 'uri', detail: "uri ';' URI (';' URI)?", documentation: 'Resolve a URI against a base URI.' },
    { label: 'user', detail: "user ';' KEY ( ';' DEFAULT )?", documentation: 'A current user setting from the ~/.bnd/settings.json file.' },
    { label: 'vcompare', detail: 'vcompare VERSION VERSION', documentation: 'Compare two version strings.' },
    { label: 'version', detail: 'version MASK VERSION?', documentation: 'Modify a version using a template. This is an alias to the versionmask macro.' },
    { label: 'version_cleanup', detail: "version_cleanup ';' VERSION", documentation: 'Cleanup a potential maven version to make it match the OSGi Version syntax.' },
    { label: 'versionmask', detail: 'versionmask MASK VERSION?', documentation: 'Modify a version using a template.' },
    { label: 'vmax', detail: "vmax (';' LIST )*", documentation: 'Find the maximum version in one or more lists.' },
    { label: 'vmin', detail: "vmin (';' LIST )*", documentation: 'Find the minimum version in one or more lists.' },
    { label: 'warning', detail: "warning ( ';' STRING )*", documentation: 'Generate a build warning with a custom message.' },
    { label: 'workspace', detail: 'workspace', documentation: 'Get the absolute path to the workspace directory.' },
];

const HEADERS: CompletionData[] = [
    { label: 'Automatic-Module-Name', detail: 'Automatic-Module-Name', documentation: 'The module name of an automatic module is derived from the JAR file used to include the artifact if no Automatic-Module-Name is set.' },
    { label: 'Bnd-AddXmlToTest', detail: "Bnd-AddXmlToTest RESOURCE ( ',' RESOURCE )", documentation: 'Add XML resources from the tested bundle to the output of a test report.' },
    { label: 'Bnd-LastModified', detail: 'Bnd-LastModified LONG', documentation: 'Timestamp from bnd, aggregated last modified time of its resources.' },
    { label: 'Bundle-ActivationPolicy', detail: 'Bundle-ActivationPolicy ::= policy', documentation: 'The Bundle-ActivationPolicy specifies how the framework should activate the bundle once started.' },
    { label: 'Bundle-Activator', detail: 'Bundle-Activator CLASS', documentation: 'The Bundle-Activator header specifies the name of the class used to start and stop the bundle.' },
    { label: 'Bundle-Blueprint', detail: "Bundle-Blueprint RESOURCE (',' RESOURCE )", documentation: 'The Bundle-Blueprint header specifies Blueprint XML descriptor resources in the bundle.' },
    { label: 'Bundle-Category', detail: "Bundle-Category STRING (',' STRING )", documentation: 'The categories this bundle belongs to, can be set through the BundleCategory annotation.' },
    { label: 'Bundle-ClassPath', detail: "Bundle-ClassPath ::= entry ( ',' entry )*", documentation: 'The Bundle-ClassPath header defines a comma-separated list of JAR file path names or directories (inside the bundle) containing classes and resources.' },
    { label: 'Bundle-ContactAddress', detail: 'Bundle-ContactAddress', documentation: 'The Bundle-ContactAddress header provides the contact address of the vendor.' },
    { label: 'Bundle-Contributors', detail: 'Bundle-Contributors ...', documentation: 'Lists the bundle contributors according to the Maven bundle-contributors pom entry.' },
    { label: 'Bundle-Copyright', detail: 'Bundle-Copyright STRING', documentation: 'The Bundle-Copyright header contains the copyright specification for this bundle.' },
    { label: 'Bundle-Description', detail: 'Bundle-Description STRING', documentation: 'The Bundle-Description header defines a short description of this bundle.' },
    { label: 'Bundle-Developers', detail: 'Bundle-Developers ...', documentation: 'Lists the bundle developers according to the Maven bundle-developers pom entry.' },
    { label: 'Bundle-DocURL', detail: 'Bundle-DocURL STRING', documentation: 'The Bundle-DocURL headers must contain a URL pointing to documentation about this bundle.' },
    { label: 'Bundle-Icon', detail: 'Bundle-Icon', documentation: 'The optional Bundle-Icon header provides a list of (relative) URLs to icons representing this bundle.' },
    { label: 'Bundle-License', detail: "Bundle-License ::= '<<[EXTERNAL]>>' | ( license ( ',' license ) * )", documentation: 'The Bundle-License header provides an optional machine readable form of license information.' },
    { label: 'Bundle-Localization', detail: 'Bundle-Localization', documentation: 'The Bundle-Localization header contains the location in the bundle where localization files can be found.' },
    { label: 'Bundle-ManifestVersion', detail: 'Bundle-ManifestVersion ::= 2', documentation: 'The Bundle-ManifestVersion is always set to 2, there is no way to override this.' },
    { label: 'Bundle-Name', detail: 'Bundle-Name STRING', documentation: 'The Bundle-Name header defines a readable name for this bundle. This should be a short, human-readable name.' },
    { label: 'Bundle-NativeCode', detail: 'Bundle-NativeCode ::= nativecode', documentation: 'The Bundle-NativeCode header contains a specification of native code libraries contained in this bundle.' },
    { label: 'Bundle-RequiredExecutionEnvironment', detail: "Bundle-RequiredExecutionEnvironment ::= ee-name ( ',' ee-name )*", documentation: 'The Bundle-RequiredExecutionEnvironment contains a comma-separated list of execution environments that must be present on the Service Platform.' },
    { label: 'Bundle-SCM', detail: 'Bundle-SCM', documentation: 'Defines the information about the source code of the bundle.' },
    { label: 'Bundle-SymbolicName', detail: "Bundle-SymbolicName ::= symbolic-name ( ';' parameter ) *", documentation: 'The Bundle-SymbolicName header specifies a non-localizable name for this bundle. The bundle symbolic name together with a version uniquely identifies a bundle.' },
    { label: 'Bundle-UpdateLocation', detail: 'Bundle-UpdateLocation', documentation: 'The Bundle-UpdateLocation header specifies a URL where an update for this bundle should come from.' },
    { label: 'Bundle-Vendor', detail: 'Bundle-Vendor', documentation: 'The Bundle-Vendor header contains a human-readable description of the bundle vendor.' },
    { label: 'Bundle-Version', detail: 'Bundle-Version ::= version', documentation: 'The Bundle-Version header specifies the version of this bundle.' },
    { label: 'Conditional-Package', detail: "Conditional-Package PACKAGE-SPEC ( ',' PACKAGE-SPEC ) *", documentation: 'Recursively add packages from the class path when referred and when they match one of the package specs.' },
    { label: 'Created-By', detail: 'Created-By STRING', documentation: 'Java version used in build.' },
    { label: 'DynamicImport-Package', detail: "DynamicImport-Package ::= dynamic-description ( ',' dynamic-description )*", documentation: 'The DynamicImport-Package header contains a comma-separated list of package names that should be dynamically imported when needed.' },
    { label: 'Export-Package', detail: "Export-Package ::= export ( ',' export)*", documentation: 'The Export-Package header contains a declaration of exported packages.' },
    { label: 'Export-Service', detail: 'Export-Service', documentation: 'Deprecated. Specifies the service interfaces exported by this bundle.' },
    { label: 'Fragment-Host', detail: 'Fragment-Host ::= bundle-description', documentation: 'The Fragment-Host header defines the host bundles for this fragment.' },
    { label: 'Ignore-Package', detail: 'Ignore-Package', documentation: 'The Ignore-Package is used to ignore a package from being packaged inside the bundle.' },
    { label: 'Import-Package', detail: "Import-Package ::= import ( ',' import )*", documentation: 'The Import-Package header declares the imported packages for this bundle.' },
    { label: 'Meta-Persistence', detail: "Meta-Persistence ::= ( RESOURCE ( ',' RESOURCE )* )?", documentation: 'A Persistence Bundle is a bundle that contains the Meta-Persistence header.' },
    { label: 'Private-Package', detail: "Private-Package PACKAGE-SPEC ( ',' PACKAGE-SPEC )*", documentation: 'Specifies what packages to include as private (not exported) packages.' },
    { label: 'Provide-Capability', detail: "Provide-Capability ::= capability (',' capability )*", documentation: 'Specifies that a bundle provides a set of Capabilities.' },
    { label: 'Require-Bundle', detail: "Require-Bundle ::= bundle-description ( ',' bundle-description )*", documentation: 'The Require-Bundle header specifies that all exported packages from another bundle must be imported.' },
    { label: 'Require-Capability', detail: "Require-Capability ::= requirement ( ',' requirement )*", documentation: 'Specifies that a bundle requires other bundles to provide a capability.' },
    { label: 'Service-Component', detail: "Service-Component ::= RESOURCE ( ',' RESOURCE )", documentation: 'XML documents containing component descriptions must be specified by the Service-Component header in the bundle manifest.' },
    { label: 'Test-Cases', detail: "Test-Cases CLASS ( ',' CLASS ) *", documentation: 'Header to automatically execute tests in the bnd JUnit tester.' },
    { label: 'Tester-Plugin', detail: 'Tester-Plugin', documentation: 'It points to a class that must extend the aQute.bnd.build.ProjectTester class.' },
    { label: 'Tool', detail: 'Tool STRING', documentation: 'Bnd version used to build this bundle.' },
    { label: 'javac', detail: 'javac', documentation: 'Java Compiler Specific Settings.' },
    { label: 'javac.encoding', detail: 'javac.encoding', documentation: 'Sets the Java Compiler Encoding Type.' },
    { label: 'javac.profile', detail: 'javac.profile', documentation: 'When using compact profiles, this option specifies the profile name when compiling.' },
    { label: 'javac.source', detail: 'javac.source', documentation: 'Sets the Java source compatibility version.' },
    { label: 'javac.target', detail: 'javac.target', documentation: 'Sets the Java target compatibility version.' },
];

export function activate(context: vscode.ExtensionContext): void {
    const completionProvider = vscode.languages.registerCompletionItemProvider(
        { language: 'bnd' },
        {
            provideCompletionItems(document: vscode.TextDocument, position: vscode.Position): vscode.CompletionItem[] {
                const lineText = document.lineAt(position).text;
                const textBeforeCursor = lineText.substring(0, position.character);

                // Inside a macro ${...}
                if (/\$\{[^}]*$/.test(textBeforeCursor)) {
                    return MACROS.map(m => {
                        const item = new vscode.CompletionItem(m.label, vscode.CompletionItemKind.Function);
                        item.detail = m.detail;
                        item.documentation = new vscode.MarkdownString(m.documentation);
                        return item;
                    });
                }

                // At the start of a line – offer instructions and headers
                if (/^\s*-?[a-zA-Z0-9._-]*$/.test(textBeforeCursor)) {
                    const items: vscode.CompletionItem[] = [];

                    for (const instr of INSTRUCTIONS) {
                        const item = new vscode.CompletionItem(instr.label, vscode.CompletionItemKind.Property);
                        item.detail = instr.detail;
                        item.documentation = new vscode.MarkdownString(instr.documentation);
                        item.insertText = new vscode.SnippetString(`${instr.label}: $0`);
                        items.push(item);
                    }

                    for (const header of HEADERS) {
                        const item = new vscode.CompletionItem(header.label, vscode.CompletionItemKind.Field);
                        item.detail = header.detail;
                        item.documentation = new vscode.MarkdownString(header.documentation);
                        item.insertText = new vscode.SnippetString(`${header.label}: $0`);
                        items.push(item);
                    }

                    return items;
                }

                return [];
            }
        },
        '-', '$', '{', ':'
    );

    const hoverProvider = vscode.languages.registerHoverProvider(
        { language: 'bnd' },
        {
            provideHover(document: vscode.TextDocument, position: vscode.Position): vscode.Hover | undefined {
                const range = document.getWordRangeAtPosition(position, /[-\w.]+/);
                if (!range) { return undefined; }
                const word = document.getText(range);

                // Check instructions (with or without leading -)
                const instr = INSTRUCTIONS.find(i => i.label === word || i.label === `-${word}`);
                if (instr) {
                    return new vscode.Hover([
                        new vscode.MarkdownString(`**${instr.detail}**`),
                        new vscode.MarkdownString(instr.documentation),
                    ]);
                }

                // Check headers
                const header = HEADERS.find(h => h.label === word);
                if (header) {
                    return new vscode.Hover([
                        new vscode.MarkdownString(`**${header.detail}**`),
                        new vscode.MarkdownString(header.documentation),
                    ]);
                }

                // Check macros
                const macro = MACROS.find(m => m.label === word);
                if (macro) {
                    return new vscode.Hover([
                        new vscode.MarkdownString(`**${macro.detail}**`),
                        new vscode.MarkdownString(macro.documentation),
                    ]);
                }

                return undefined;
            }
        }
    );

    context.subscriptions.push(completionProvider, hoverProvider);
}

export function deactivate(): void { /* no-op */ }
