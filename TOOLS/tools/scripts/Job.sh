#!/bin/bash

#  Created by stassia Resondry Zafimiharisoa on 20/03/14.
#  Copyright (c) 2014. All rights reserved.
#!/bin/sh
#PARAMETERSFILE=$1


fileExist (){
local file=$1;

if [ -e $file ]; then
echo "file exist"
else
#if true condition stop the program
echo "$file does not exist"
exit 1
fi

#local false='true'
#if [ $2=$false ]; then
#echo 'stop '
#exit 1
#else
##echo 'not stop'
#fi
}


checkFilesExistence (){

#check if all input files exist
#take the first

fileExist $SDK true
echo $?
fileExist $PROJECT true
echo $?
fileExist $TESTDATA true
echo $?
fileExist $CP false
echo $?
fileExist $DB false
echo $?
fileExist $TOOLS true
echo $?
fileExist $MAININFOLDER_OF_PROJECT true
echo $?

}

checkeExportedValues (){


#check if all input files exist
#take the first


#check option
#check sdk
#
#export OPTION=test
echo "TERM_SESSION_ID $TERM_SESSION_ID"
echo "DISPLAY:  $DISPLAY"
echo "OPTION:  $OPTION"

if [ -z $OPTION ]; then
echo "$OPTION"
echo "Please set OPTION variable"
exit 1
fi

if [ -z $SDK ]; then
echo "Please set SDK PATH variable"
exit 1
fi



if [ -z $PROJECT ]; then
echo "Please set PROJECT PATH variable"
exit 1
fi



if [ -z $PROJECTPACKAGE ]; then
echo "Please set PROJECTPACKAGE variable"
exit 1
fi

if [ -z $LAUNCHERACTIVITY ]; then
echo "Please  set LAUNCHERACTIVITY variable"
exit 1
fi

if [ -z $EXPECTACTIVITY ]; then
echo "Please set EXPECTACTIVITY variable"
exit 1
fi

if [ -z $TESTPROJECT ]; then
echo "Please to set TESTPROJECT PATH variable"
exit 1
fi

if [ -z $TESTPROJECTNAME ]; then
echo "Please set TESTPROJECTNAME variable"
exit 1
fi



if [ -z $TESTDATA ]; then
echo "Please  set TESTDATA PATH variable"
exit 1
fi



if [ -z $TESTPROJECTNAME ]; then
echo "Please set TESTPROJECTNAME variable"
exit 1
fi



if [ -z $COVERAGE ]; then
echo "Please to set COVERAGE OtherWise default value 100 will be affected (y/n) ?"
read answer
if [ "$answer"="y" ]; then
$COVERAGE=100
else 
exit 1
fi
fi


if [ -z $STRATEGY ]; then
echo "Please set STRATEGY type OtherWise default value 1 will be affected (y/n) ?"
read answer
if [ "$answer"="y" ]; then
$STRATEGY=1
else
exit 1
fi
fi


#if [ -z $FOURMYTYPE ]; then
#echo "Please set FOURMYTYPE variables otherWise default value 0 will be affected (y/n) ?"
#read answer
#if [ "$answer"="y" ]; then
#$FOURMYTYPE=0
#else
#exit 1
##fi
#fi


if [ -z $MAXEVENT ]; then
echo "Please set MAXEVENT  otherWise default value 100 will be affected (y/n) ?"
read answer
if [ "$answer"="y" ]; then
$MAXEVENT=100
else
exit 1
fi
fi

if [ -z $STOPIFERROR ]; then
echo "Please set STOPIFERROR variable otherWise default value false will be affected (y/n) ?"
read answer
if [ "$answer"="y" ]; then
$STOPIFERROR="false"
else
exit 1
fi
fi


if [ -z $PAIRWISENUMBER ]; then
echo "Please set PAIRWISENUMBER variable otherWise default value 1 will be affected (y/n) ?"
read answer
if [ "$answer"="y" ]; then
$PAIRWISENUMBER=1
else
exit 1
fi
fi


if [ -z $CP ]; then
echo "Please set CP Path variable"
exit 1
fi


if [ -z $DB ]; then
echo "Please set DB Path"
exit 1
fi



if [ -z $THREADNUMBER ]; then
echo "Please set THREADNUMBER variable otherWise default value 1 will be affected (y/n) ?"
read answer
if [ "$answer"="y" ]; then
$THREADNUMBER=1
else
exit 1
fi
fi


if [ -z $ATARGETEMULATOR ]; then
echo "Please set ATARGETEMULATOR variable"
exit 1
fi



if [ -z $OUTPUTDIRECTORY ]; then
echo "Please set OUTPUTDIRECTORY variable"
exit 1
fi


if [ -z $TOOLS ]; then
echo "Please set TOOLS variable"
exit 1
fi


if [ -z $MAXTIME ]; then
echo "Please set MAXTIME variable"
exit 1
fi

if [ -z $MAININFOLDER_OF_PROJECT ]; then
echo "Please set MAININFOLDER_OF_PROJECT variable"
exit 1
fi

}








#================================================================================================================================================================

printVariable(){

echo 'Option: ' $OPTION


echo 'SDK: ' $SDK 
echo 'PROJECT: ' $PROJECT 
echo 'PROJECTMANIFEST ' $PROJECTMANIFEST
echo 'PROJECTAPK ' $PROJECTAPK
echo 'PROJECTPACKAGE: ' $PROJECTPACKAGE 
echo 'LAUNCHERACTIVITY: ' $LAUNCHERACTIVITY 
echo 'EXPECTACTIVITY: ' $EXPECTACTIVITY

echo 'TESTPROJECT: ' $TESTPROJECT
echo 'TESTPROJECTNAME: ' $TESTPROJECTNAME
echo 'TESTPACKAGE ' $TESTPACKAGE
echo 'TESTPROJECTAPK' $TESTPROJECTAPK



echo 'TESTDATA: ' $TESTDATA
echo 'CP: ' $CP
echo 'DB: ' $DB

echo 'Stop if Error: ' $STOPIFERROR 
echo 'MaxEvent: ' $MAXEVENT 
echo 'MaxTime:  ' $MAXTIME

echo 'PAIRWISENUMBER:  ' $PAIRWISENUMBER
}

#================================================================================================================================================================
function clean_up
{
if [ "$pid" != "" ]; then
kill $pid
fi
}

function error_exit 
{
echo "${PROGNAME}: ${1:-"Unknown Error"}" >&2
clean_up
exit 1
}

#================================================================================================================================================================
initVariables(){


if [[ $PARAMETERSFILE == *param* ]]
then
#lire les parametres à partir d'un fichier et affecter les valeur
while read -r line
do
#construire une liste 
name=$line
echo "param - $name"
done < "$PARAMETERSFILE"
else
echo "not param file " $SDK
fi
}

initProjectApk()
{

PROJECTMANIFEST=$PROJECT/AndroidManifest.xml
#echo $PROJECT
fileExist $PROJECTMANIFEST true
TESTPACKAGE=$PROJECTPACKAGE.test
TESTPROJECTAPK=$TESTPROJECT/bin/$TESTPROJECTNAME.apk
OUTPUTDIRECTORYSTRESS=$OUTPUTDIRECTORY'Stress'
OUTPUTDIRECTORYINJREQUEST=$OUTPUTDIRECTORY'INJRequest'
OUTPUTDIRECTORYINJRANDOM=$OUTPUTDIRECTORY'INJRAndom' 
SGAAPK=$TOOLS/sga.apk
SGAPACKAGE='fr.openium.sga'

CLASSMODEL=$TESTPACKAGE.MainTest
CLASSSTRESS=$TESTPACKAGE.StressTest
CLASSSINJREQUEST=$TESTPACKAGE.InjTest
CLASSSRANDOM=$TESTPACKAGE.InjRandomTest


SGD=$TOOLS/sgd.jar
fileExist $SGD true
}

#================================================================================================================================================================
createTestProject(){

android update project -p $PROJECT
#echo'============================================ createTestProject ===================================================================================================================='
android=$1/tools/android 
android create test-project -m $PROJECT -n $TESTPROJECTNAME -p $TESTPROJECT
addLibs
#supprimer les classes crées par 
rm -R $TESTPROJECT/src/*
}


updateManifest () {
#echo'============================================ UpdateManifest ===================================================================================================================='

local manifest=$PROJECT/AndroidManifest.xml
local manifestTemp=$PROJECT/AndroidmanifestTemp.xml


#check if a line exist : android.permission.WRITE_EXTERNAL_STORAGE
awk '/android.permission.WRITE_EXTERNAL_STORAGE/{print NR;exit}' $manifest > temp
local line=$(cat temp)

if [ -z $line ]; then
echo "No permission android.permission.WRITE_EXTERNAL_STORAGE"
awk '/<application/{print NR;exit}' $manifest > temp
local line=$(cat temp)
echo 'line ' $line
rm temp
#edit manifest file
awk 'NR=='$line'{print "<uses-permission android:name=\"android.permission.WRITE_EXTERNAL_STORAGE\" />"}1' $manifest > $manifestTemp
forcemv $manifestTemp $manifest
else 
rm temp
fi

awk '/android.permission.GET_TASKS/{print NR;exit}' $manifest > temp
local line=$(cat temp)
if [ -z $line ]; then
echo "No permission android.permission.WRITE_EXTERNAL_STORAGE"
awk '/<application/{print NR;exit}' $manifest > temp
local line=$(cat temp)
echo 'line ' $line
rm temp
#edit manifest file
awk 'NR=='$line'{print "<uses-permission android:name=\"android.permission.GET_TASKS\" />"}1' $manifest > $manifestTemp
forcemv $manifestTemp $manifest
else 
rm temp
fi


#awk '/<application/{print NR;exit}' $manifest > temp
#local line=$(cat temp)
#echo 'line ' $line
#rm temp
#edit manifest file
#awk 'NR=='$line'{print "<uses-permission android:name=\"android.permission.WRITE_EXTERNAL_STORAGE\" /><uses-permission android:name=\"android.permission.GET_TASKS\"/>"}1' $manifest > $manifestTemp
#forcemv $manifestTemp $manifest
}



addLibs(){
# A utiliser addclasspath.sh plutard
cp -R $TOOLS/libs $TESTPROJECT
}

#A faire pour tous les build.xml des projets
addSgdInstrumentationLineInManifestAndBuildFile(){
local build=$TESTPROJECT/build.xml
local buildTemp=$testproject'/buildTemp.xml'
echo 'build =' $build
updateManifestFileOfTestProject
#delete pmd.dir
local pmd='<import file="${pmd.dir}/droid/analysis.xml" />'
local target_pmd='<target name="-post-compile" depends="pmd,cpd"/>'
local empty=''

sed -e 's/'$pmd'/'$empty'/g' $build > $buildTemp
c
mv $buildTemp $build

sed -e 's/'$target_pmd'/'$empty'/g' $build > $buildTemp
mv $buildTemp $build

#in build.xml
#sed -e 's/'$polidea'/'$manifest_testrunner'/g' $build > $buildTemp
#mv $buildTemp $build

}
updateBuildFile(){
echo 'updateBuildFile of : ' $1
local build=$1/build.xml
local buildTemp=$1'/buildTemp.xml'
echo 'build =' $build
chmod 777 $build
sed '/pmd/d' $build > $buildTemp
mv $buildTemp $build

}
addSgdInstrumentationLineInManifestAndBuildFileInMultipleFile()
{
local projectFile=$(ls $1)
local arr=$(echo $projectFile | tr "\ " "\n")
for x in $arr
do
local project=$x;
android update project -p $1/$project #-t android-10
updateBuildFile $1/$project
updateManifestFileOfTestProject
done
}


updateManifestFileOfTestProject(){

local manifest=$TESTPROJECT/AndroidManifest.xml
echo 'manifest =' $manifest
local manifestTemp=$TESTPROJECT/AndroidManifestTemp.xml
echo 'manifestTemp ='$manifestTemp
local manifest_testrunner='fr.openium.sgdInstrumentationTestRunner.SgdInstrumentationTestRunner'
echo 'testRunner ='$manifest_testrunner
local androidtestRunner='android.test.InstrumentationTestRunner'
local polidea='pl.polidea.instrumentation.PolideaInstrumentationTestRunner'
#delete polideaInstTestRunner and replace with sgdInstTestRunner
#delete androidInstTestRunner and replace with sgdInstTestRunner

sed -e 's/'$polidea'/'$manifest_testrunner'/g' $manifest > $manifestTemp
forcemv $manifestTemp $manifest

sed -e 's/'$androidtestRunner'/'$manifest_testrunner'/g' $manifest > $manifestTemp
forcemv $manifestTemp $manifest

#nom du package de test
initialname=$PROJECTPACKAGE.tests
sed -e 's/'$initialname'/'$TESTPACKAGE'/g' $manifest > $manifestTemp
forcemv $manifestTemp $manifest
}

forcemv (){
chmod 777 $1
chmod 777 $2
mv $1 $2
}

#================================================================================================================================================================
generateCrawlerCode(){
#echo'#============================================ generateCrawlerCode ===================================================================================================================='
echo 'strategy' $STRATEGY 
echo '$PAIRWISENUMBER ' $PAIRWISENUMBER  
echo 'expectedAct ' $EXPECTACTIVITY
echo 'MAnifest ' $PROJECTMANIFEST

java -cp $SGD fr.openium.sga.Main codeGen -tp $TESTPROJECT -manifest $PROJECTMANIFEST -pn $PAIRWISENUMBER -expectedAct $EXPECTACTIVITY -strategy $STRATEGY
#read validCodeGenCall
#scripts/genCode.sh tools/sgd.jar $TESTPROJECT $PROJECT $PAIRWISENUMBER $STRATEGY $EXPECTACT
}


generateModelTestCode()
{
echo "Model Code"
java -cp $SGD fr.openium.sga.Main stress -p $PROJECT -manifest $PROJECTMANIFEST -expectedAct EXPECTACTIVITY -strategy $STRATEGY -pn $PAIRWISENUMBER
}


#================================================================================================================================================================
build(){

echo 'build the projects'
echo '$MAININFOLDER_OF_PROJECT :' $MAININFOLDER_OF_PROJECT
#read checkMAININFOLDER_OF_PROJECT
local string=$MAININFOLDER_OF_PROJECT
if [ "$string"!="" ];then
echo 'handle multiple project file'
#read checkMAININFOLDER_OF_PROJECT
#supprimer les pmd de chaque projet.
addSgdInstrumentationLineInManifestAndBuildFileInMultipleFile $MAININFOLDER_OF_PROJECT
else
echo 'no multiple project'
#read checkMAININFOLDER_OF_PROJECT
android update project -p $PROJECT #-t android-10
android update test-project -m $PROJECT -p $TESTPROJECT
echo 'android update'
addSgdInstrumentationLineInManifestAndBuildFile
fi

echo 'build is not yet completed'
#-Dadb.device.arg="-s emulator-5554"
ant -buildfile $TESTPROJECT/build.xml clean emma debug

echo 'setProjectApk variables'

local apk=$(ls $PROJECT/bin/*instrumented.apk)
PROJECTAPK=$apk
fileExist $PROJECTAPK true

installProjectAndTestProject
}


installProjectAndTestProject(){

local devices=$(adb devices)
local arr=$(echo $devices | tr "\ " "\n")
for x in $arr
do
if [ $x != device ] && [ $x != devices ] && [ $x != List ] && [ $x != device ] && [ $x != offline ] && [ $x != attached ] && [ $x != of ];
then
echo "installOnMobile"
ant -Dadb.device.arg="-s $x" -buildfile $TESTPROJECT/build.xml emma installd
echo 'create remote file'
adb -s $x shell mkdir /mnt/sdcard/testResults
fi
done

}


installOnMobile(){
# il faut avoir la liste des emulateurs au moins
# get list of available emulator 

local devices=$(adb devices)
local arr=$(echo $devices | tr "\ " "\n")
for x in $arr
do
if [ $x != device ] && [ $x != devices ] && [ $x != List ] && [ $x != device ] && [ $x != offline ] && [ $x != attached ] && [ $x != of ];
then
uninstall_install $SGAPACKAGE $SGAAPK $x
fi
done


}

uninstall_install (){
package=$1
apk=$2
emulator=$3

echo "package : $package"
echo "apk : $apk"
echo "emulator : $emulator"

echo "> uninstall $SGAPACKAGE on [$x]";
adb -s $emulator uninstall $package 
echo "> install $SGAAPK on [$x]";
adb -s $emulator install $apk 
}

#================================================================================================================================================================
MAIN=fr.openium.sga.Main
generateModel()
{
#echo '==============================================================generateModel========================================================================================'

echo "PROJECTAPK : $PROJECTAPK"
echo "command : java -cp tools/sgd.jar fr.openium.sga.Main explore -p $PROJECT -manifest $PROJECTMANIFEST -pPackage $PROJECTPACKAGE -pApk $PROJECTAPK -tp $TESTPROJECT -sdk $SDK -strategy $STRATEGY -arv $TESTDATA -thread $THREADNUMBER -out $OUTPUTDIRECTORY -emu $ATARGETEMULATOR -class $CLASSMODEL -stopError $STOPIFERROR" -stopError false -launcherActivity $LAUNCHERACTIVITY -coverage $COVERAGE -maxTime $MAXTIME -cp $CP -db $DB

java -cp $SGD $MAIN explore -p $PROJECT -manifest $PROJECTMANIFEST -pPackage $PROJECTPACKAGE -pApk $PROJECTAPK -tp $TESTPROJECT -tpackage $TESTPACKAGE -sdk $SDK -strategy $STRATEGY -arv $TESTDATA -thread $THREADNUMBER -out $OUTPUTDIRECTORY -emu $ATARGETEMULATOR -class $CLASSMODEL -stopError false -launcherActivity $LAUNCHERACTIVITY -coverage $COVERAGE -maxTime $MAXTIME -cp $CP -db $DB
MODEL=$OUTPUTDIRECTORY/out.xml
#-fourmy $FOURMYTYPE
#getActivityCoverage
}


#================================================================================================================================================================
generateSecurityTestCode()
{

echo "==============================================================generateSecurityTestCode==========================================================================================="

generateStressTestCode

generateInjTestCode

}

generateInjTestCode()
{

echo "==============================================================generateInjTestCode==========================================================================================="

generateRequestInjTestCode
generateRandomTestCode

}

generateRequestInjTestCode()
{
echo "==============================================================generateRequestInjTestCode============================================================================================"


echo "InjRandom Code"

generateCodeGen 112 
}

generateRandomTestCode()
{

echo "==============================================================generateRandomTestCode============================================================================================"

echo "InjRequest Code"

generateCodeGen 111
}


generateStressTestCode()
{
echo "==============================================================generateStressTestCode============================================================================================="

echo "Stress Code"

generateCodeGen 100

}

generateCodeGen()
{
echo "==============================================================generateCodeGen============================================================================================="

strategy=$1;
java -cp $SGD $MAIN codeGen -tp $TESTPROJECT -manifest $PROJECTMANIFEST -expectedAct $EXPECTACTIVITY -strategy $strategy -pn $PAIRWISENUMBER
}
#===============================================================================================================================================================

ExecuteSecurityTest()
{
echo "===============================================================ExecuteSecurityTest============================================================================================="
local outpudirectory=$OUTPUTDIRECTORY/out.xml
echo "model  "  $1
StressSecurityTest
InjSecurityTest

}

StressSecurityTest()
{
echo "===============================================================StressSecurityTest============================================================================================="


local outpudirectory=$OUTPUTDIRECTORY/out.xml
set -e

echo "Stress Test"
java -cp $SGD $MAIN stress -p $PROJECT -manifest $PROJECTMANIFEST -pPackage $PROJECTPACKAGE -pApk $PROJECTAPK -tp $TESTPROJECT -tpackage $TESTPACKAGE -sdk $SDK -strategy 100 -arv $TESTDATA -thread $THREADNUMBER -out $OUTPUTDIRECTORYSTRESS -emu $ATARGETEMULATOR -class $CLASSSTRESS -stopError true -launcherActivity $LAUNCHERACTIVITY -coverage $COVERAGE -maxTime $MAXTIME -model $outpudirectory


}

InjSecurityTest()
{
echo "===============================================================InjSecurityTest============================================================================================="
outpudirectory=$OUTPUTDIRECTORY/out.xml
set -e

echo "InjRequest Test"
java -cp $SGD $MAIN inj -p $PROJECT -manifest $PROJECTMANIFEST -pPackage $PROJECTPACKAGE -pApk $PROJECTAPK -tp $TESTPROJECT -tpackage $TESTPACKAGE -sdk $SDK -strategy 111 -arv $TESTDATA -thread $THREADNUMBER -out $OUTPUTDIRECTORYINJREQUEST -emu $ATARGETEMULATOR -class $CLASSSINJREQUEST -stopError true -launcherActivity $LAUNCHERACTIVITY -coverage $COVERAGE -maxTime $MAXTIME -model $outpudirectory -db $DB

echo "InjRandom Test"
java -cp $SGD $MAIN inj -p $PROJECT -manifest $PROJECTMANIFEST -pPackage $PROJECTPACKAGE -pApk $PROJECTAPK -tp $TESTPROJECT -tpackage $TESTPACKAGE -sdk $SDK -strategy 112 -arv $TESTDATA -thread $THREADNUMBER -out $OUTPUTDIRECTORYINJRANDOM -emu $ATARGETEMULATOR -class $CLASSSRANDOM -stopError true -launcherActivity $LAUNCHERACTIVITY -coverage $COVERAGE -maxTime $MAXTIME -model $outpudirectory -db $DB

}


#===============================================================Command=============================================================================================
getActivityCoverage(){
echo "===============================================================GetActivityCoverage============================================================================================="
java -cp $SGD fr.openium.sga.Main activityCoverage -manifest $PROJECTMANIFEST -scen $MODEL 
}


initVariablesStepByStep(){
echo "===============================================================step by step ============================================================================================"
echo "The sdk path?" 
read SDK
fileExist $SDK true
echo "The main folder containing the target project and the dependecies ?"
echo "example: the folder MAIN contains the target project TOTEST and others project OTHER1,OTHER2, etc... "
read MAININFOLDER_OF_PROJECT
fileExist $MAININFOLDER_OF_PROJECT true
echo "The main option:  All , explore, stress, inj or display ?" 
read OPTION
if [ "$OPTION" = "All" -o "$OPTION" = "explore" -o "$OPTION" = "stress" -o "$OPTION" = "inj" -o "$OPTION" = "display" ]; then 
echo "has valid Option" 
else 
echo "Please choose a valid option:  All , explore, stress, inj or display ?" 
exit 1
fi
echo "OPTION: $OPTION" 
echo "project path ?" 
read PROJECT
fileExist $PROJECT true
echo "project package ?" 
read PROJECTPACKAGE
echo "launcher activity ?" 
read LAUNCHERACTIVITY
echo "expected activity ?" 
read EXPECTACTIVITY
echo "test project path?" 
read TESTPROJECT
echo "test project name?" 
read TESTPROJECTNAME
echo "testData path?" 
read TESTDATA
fileExist $TESTDATA true
echo "a target emulator? (put null otherwise)" 
read ATARGETEMULATOR
echo "the maximum coverage? (100 recommanded)" 
read COVERAGE
echo "the type of strategy ? (0,1,100,111,112)" 
read STRATEGY
echo "the thread number ? " 
read THREADNUMBER
#echo "the type of ant strategy (dfs-bfs:0)" 
#read FOURMYTYPE
echo "the number maximum of sequences to generate during exploration?" 
read PAIRWISENUMBER
echo "would you stop the operation if there is a crash on the devices? " 
read STOPIFERROR
echo "the number maximum of event during securituy test" 
read MAXEVENT
echo "the path of the list of system's content provider" 
read CP
echo "the path of db name" 
read DB 
fileExist $DB true
echo "the output path?" 
read OUTPUTDIRECTORY
echo "the tools path?" 
read TOOLS 
fileExist $TOOLS true 
echo "the maximum  time of testing in millisecondes" 
read MAXTIME
}
#===============================================================Help=========================================

displayHelp () {

echo 'Please export the following variables as environment variable the launch the script without variables. '
echo 'eg: export OPTION=display '

echo OPTION ': display, explore, All, stress, inj '
echo SDK ': sdk path '
echo PROJECT ': the project to crawl and to test path '
echo PROJECTPACKAGE ': project package'
echo LAUNCHERACTIVITY ':the launcher activity'
echo EXPECTACTIVITY ': the expected actvity'
echo TESTPROJECT ': test project path'
echo TESTPROJECTNAME ': test project name'
echo TESTDATA ': testdata path'
echo ATARGETEMULATOR ': null or a target emulator if the number of thread =1 '
echo COVERAGE ': stop if a coverage reached '
echo STRATEGY ': the strategy type 0 (simple) or 1 (fourmy recommanded),for security testing 100,111,112 '
echo THREADNUMBER ': the number of emulator to run the tasks '
echo PAIRWISENUMBER ': the number of test data sequences to generate for editexts set '
echo STOPIFERROR ': true or false ? stop if there is a crash '
echo MAXEVENT ': the maxEvent for stressTesting '
echo CP ': the list of system content providers path '
echo DB ': the path where database name is written '
echo OUTPUTDIRECTORY ': the output Directory of testing'
echo TOOLS ': folder containing the jar files'
echo MAXTIME ': Maxtime to run the target tasks in milliseconsd '
echo MAININFOLDER_OF_PROJECT ': the folder containing the project to test and other library or parents projects  '
echo "==================================end help =================="

}

#===============================================================Command=============================================================================================

launchSGD()
{


if [ "$OPTION" = "display" ];then
java -cp $TOOLS/sgd.jar $MAIN $OPTION $MODEL  
exit 1
fi

if [ "$OPTION" = "step" ];then 
initVariablesStepByStep
fi

if [ "$OPTION" = "-h" ];then 
displayHelp
exit 1
fi

if [ "$OPTION" = "help" ];then 
displayHelp
exit 1
fi


initProjectApk
checkeExportedValues
checkFilesExistence
printVariable

echo "please check inserted value. OK ?" 
read VALIDATED 

echo '$PROJECTAPK' $PROJECTAPK

if [ "$OPTION" = "explore" ];then 
echo "===============================================================Explore============================================================================================="
updateManifest
createTestProject
generateCrawlerCode
build
installOnMobile
#
local apk=$(ls $PROJECT/bin/*instrumented.apk)
PROJECTAPK=$apk
fileExist $PROJECTAPK true
generateModel
exit 1
fi

if [ "$OPTION" = "stress" ];then 
echo "===============================================================Stress============================================================================================="
createTestProject
generateStressTestCode
build
#installProjectAndTestProject
installOnMobile
#local apk=$(ls $PROJECT/bin/*instrumented.apk)
#PROJECTAPK=$apk
#fileExist $PROJECTAPK true

set -x
StressSecurityTest  
exit 1
fi

if [ "$OPTION" = "inj" ];then 
echo "===============================================================Injection============================================================================================="
createTestProject
generateInjTestCode
build
#local apk=$(ls $PROJECT/bin/*instrumented.apk)
#PROJECTAPK=$apk
#fileExist $PROJECTAPK true
installOnMobile
InjSecurityTest 
exit 1
fi

if [ "$OPTION" = "All" ];then 
	#echo'===============================================================All============================================================================================='
createTestProject
generateCrawlerCode
build
#local apk=$(ls $PROJECT/bin/*instrumented.apk)
#PROJECTAPK=$apk
#fileExist $PROJECTAPK true
#installProjectAndTestProject
installOnMobile
#read beginModelGeneration
generateModel
#read endModelGeneration
generateSecurityTestCode
build
ExecuteSecurityTest 
exit 1
fi

echo 'INVALID OPTION ' $OPTION

}


#if has option

setArguments () {
set -x
if [ $# -ne 23 ]; then
echo 'the number of argument is not sufficient, you need 23 arguments'
exit 1
else
OPTION=$1
SDK=$2
PROJECT=$3
#nom de l'activité qui lance + instrumented.apk
PROJECTPACKAGE=${4}
LAUNCHERACTIVITY=${5}
EXPECTACTIVITY=$6
TESTPROJECT=${7}
TESTPROJECTNAME=${8}
TESTDATA=${9}
ATARGETEMULATOR=${10} #facultatif
COVERAGE=${11}
STRATEGY=${12}
THREADNUMBER=${13}
#FOURMYTYPE=${14}
PAIRWISENUMBER=${14}
STOPIFERROR=${15}
MAXEVENT=${16}
CP=${17}
DB=${18}
OUTPUTDIRECTORY=${19}
TOOLS=${20}
MAXTIME=${21}

MAININFOLDER_OF_PROJECT=${22}
return 0
fi


}


handleArguments () {

#environnement variable may be setted
if [ $# -eq 0 ]; then
return 0;
fi


#may be step or help
echo $# 'argument (s)'
if [ $# -eq 1 ]; then
OPTION=$1
return 0;
fi

echo 'args ' $1

# may be Display operation'
if [ $# -eq 3 ]; then
OPTION=$1
MODEL=$2
TOOLS=$3
echo 'Display operation'
return 0
fi

if [ $# -gt 1 ]; then
setArguments $*
else 
echo 'incomplete arguments Arguments'
return 1;
fi

}

set -e
set -x
handleArguments $*
ARGUMENSNUMBER=$#
launchSGD $OPTION
