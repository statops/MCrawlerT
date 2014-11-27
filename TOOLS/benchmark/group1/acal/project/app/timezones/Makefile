
all:	vtimezones/US/Eastern.ics vtimezones/US/Pacific.ics vtimezones/US/Mountain.ics vtimezones/US/Central.ics ../res/values/timezonenames.xml ../src/com/morphoss/acal/davacal/ZoneData.java

vtimezones/US/Eastern.ics:	vtimezones/America/New_York.ics
	cd vtimezones/US && ln -sf ../America/New_York.ics Eastern.ics

vtimezones/US/Pacific.ics:	vtimezones/America/Los_Angeles.ics
	cd vtimezones/US && ln -sf ../America/Los_Angeles.ics Pacific.ics

vtimezones/US/Mountain.ics:	vtimezones/America/Denver.ics
	cd vtimezones/US && ln -sf ../America/Denver.ics Mountain.ics

vtimezones/US/Central.ics:	vtimezones/America/Chicago.ics
	cd vtimezones/US && ln -sf ../America/Chicago.ics Central.ics

vtimezones/US: vtimezones/America
	rm -rf vtimezones/US
	mkdir -p vtimezones/US
	touch vtimezones/US

../src/com/morphoss/acal/davacal/ZoneData.java: build-acal-zones.pl vtimezones/Pacific
	./build-acal-zones.pl --output ../src/com/morphoss/acal/davacal/ZoneData.java

../res/values/timezonenames.xml: build-acal-zones.pl vtimezones/Pacific
	./build-acal-zones.pl --strings --output ../res/values/timezonenames.xml
