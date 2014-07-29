 
#!/bin/bash

#  Created by stassia Resondry Zafimiharisoa on 20/03/14.
#  Copyright (c) 2014 Openium. All rights reserved.
#!/bin/sh
#PARAMETERSFILE=$1
export DIRECTORY=$HOME/Documents/OpenSourceProject/2013/ASK
export INPUT=$DIRECTORYY/input
export OUTPUT=$DIRECTORY/out
export MAININFOLDER_OF_PROJECT=$DIRECTORY/project
export TESTDATA=$INPUT/testData.xml
export STRATEGY=1
export PROJECT=$MAININFOLDER_OF_PROJECT/ask
export TOOLS=$(echo $MCrawlerT)
export DB=$INPUT/db
export TESTPROJECTNAME=askTest
export PAIRWISENUMBER=100
export PROJECTPACKAGE=com.askfm
export THREADNUMBER=1
export SDK=$(echo $ANDROID_HOME)
export MAXTIME=24000000
export LAUNCHERACTIVITY=com.askfm.AskfmActivity
export EXPECTACTIVITY=com.askfm.AskfmActivity
export OPTION=explore
export ATARGETEMULATOR=null
export CP=$INPUT/cp
export OUTPUTDIRECTORY=$OUTPUT/askSamsunfS2Output
export TESTPROJECT=$MAININFOLDER_OF_PROJECT/askTest
export STOPIFERROR=true
export MAXEVENT=10
export COVERAGE=80
export BRUTEDICO=$INPUT/dico 

$TOOLS/scripts/mctApk.sh
