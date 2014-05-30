#!/bin/sh
# les entrees sont le noms de l'\apk et le nom de l'emulateur où il faudra le lancer l'emulateur
apk=$1
emulateur=$2


signed=$apk'signed.apk'
meta=META-INF

unsigned=$apk'unsigned.apk'

#cd $3
#dezziper l'apk

unzipapk ()
{
echo Unzip $1
if [ -f $1 ]
then
unzip -o $1
else 
echo $1 does not exist
fi
}

deleteSignature()
{
echo Singature deletion
if [ -d $1 ]
then
rm -r $1
echo $1 deleted
else 
echo $1 does not exist
fi
}

rezipapk (){
echo file name : $1
if [ -f $1 ]
then
rm $1
echo old $1 deleted
zip -9 -y -r -q $1 * 
echo zipping done
else 
echo $1 does not exist, zipping …
zip -9 -y -r -q $1 *  
echo done 
fi

}

sign () {
echo sign $1
#KEYSTORE= ../KEYSTORE/specGen-test-key.keystore

jarsigner -verbose -sigalg MD5withRSA -digestalg SHA1 -keystore ../KEYSTORE/specGen-test-key.keystore -storepass 'openium' $1 test_key 
 
}

allign(){
#if [ -f $signed ]
#then
#rm $signed
#zipalign -v 4 $1 $2
#else
#zipalign -v 4 $1 $2
#fi

zipalign -v 4 $1 $2
echo delete unsigned
rm $1
}


install () {
adb -s $2 install $1
}


getmanifestandstrin(){

#use apktool
apktool d -f $1.apk 
#insert getTasks and write-storage in Manifest


}

encode(){
echo 're build'

#use apktool
apktool b $1
}

updateManifest () {
local manifest=$1/AndroidManifest.xml
local manifestTemp=$1/AndroidmanifestTemp.xml


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

forcemv (){
chmod 777 $1
chmod 777 $2
mv $1 $2
}

signApk(){
unzipapk $apk.apk
deleteSignature $meta
rezipapk $unsigned 
sign $unsigned
allign $unsigned $signed
install $signed $emulateur

}

resignAPK(){
getmanifestandstrin $apk
updateManifest $apk
encode $apk $unsigned 
disApk=$apk/dist/$apk.apk
sign $disApk
allign $disApk $signed
#install $signed $emulateur
}

set -e
set -x
resignAPK













