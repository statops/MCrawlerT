 
#!/bin/bash

#  Created by stassia Resondry Zafimiharisoa on 20/03/14.
#  Copyright (c) 2014 Openium. All rights reserved.
#!/bin/sh
#PARAMETERSFILE=$1
export INPUT=$DOC_HOME/ApkProject/BUDGET/input
export OUTPUT=$DOC_HOME/ApkProject/BUDGET/budgetSamsungOutput
export MAININFOLDER_OF_PROJECT=$DOC_HOME/ApkProject/BUDGET/project
export TESTDATA=$INPUT/testData.xml
export STRATEGY=12
export PROJECT=$MAININFOLDER_OF_PROJECT/bg
export TOOLS=$(echo $MCrawlerT)
export DB=$INPUT/db
export TESTPROJECTNAME=bgTest
export PAIRWISENUMBER=2
export PROJECTPACKAGE=com.siri.budgetdemo
export THREADNUMBER=1
export SDK=$(echo $ANDROID_HOME)
export MAXTIME=14400000
export LAUNCHERACTIVITY=com.siri.budgetdemo.Home
export EXPECTACTIVITY=com.siri.budgetdemo.Menu
export OPTION=explore
export ATARGETEMULATOR=null
export CP=$INPUT/cp
export OUTPUTDIRECTORY=$OUTPUT
export TESTPROJECT=$MAININFOLDER_OF_PROJECT/bgTest
export STOPIFERROR=true
export MAXEVENT=10
export COVERAGE=80
export BRUTEDICO=$INPUT/dico 

$TOOLS/scripts/mctApk.sh
