MCrawlerT
=========

Formal Models generation (guided by exploration strategies) and automatic testing Tool for Mobile application .


Requirements for the execution of MCrawlerT:

- GNU/Linux distribution(Ubuntu 12.04)/MacOSX(10.x)
- JDK v.1.7.+
- Android sdk v.22 or later
- Libraries for Android 2.3.4 or later 
- Ant version 1.9.3 or later
- Eclipse indigo
- System variables: 
 - ANDROID\_HOME must be set to the sdk path of Android
 - JAVA\_HOME must be set to the Java sdk path
 - Add the path of Ant, android platform-tools and apktool directories to the environment variable. 



Quick Start:

0. Check out the project and go to TOOLS/source
1. Unzip the tools library available in  https://www.dropbox.com/sh/lkabg121duddkwq/AADA55vVegJ85fi2sl2GRXCza?dl=0 into the source directory (source/MCrawlerTLibs)
2. Launch the following script to build the main and its sub-projects: 
 android update lib-project -p droid/sgdAndroidKit
 android update lib-project -p droid/sga
 ant -buildfile droid/sgd/build.xml -Dsdk.args="../adt-bundle-mac-x86_64-20140321/sdk" -Dall.project.dir="../" -Dtools.args="McrawlerT"  generate_tools
Absolute path of the following files must be set as ant arguments: the android sdk, the project directory and the output directory name 'e.g: McrawlerT'.
3. Copy all the content of the checked out source/tools in your tool's directory
You must have the following files in your tool directory: 
(1) apktool
(2) KEYSTORE
(3) libs
(4) projectTemplate
(5) sga.apk
(6) scripts
(7) sgd
(8) stsDisplayer
4. A system variable called MCRAWLERT must be set to the MCrwalerT tool path. 
5. Export environment variables, then call the script mct.sh.
A simple export.sh file where a list of useful runtime information must be filled.
Examples are available in the 'exemples' repertory 
6. For more detailed, please refer to wiki.pdf file. 



