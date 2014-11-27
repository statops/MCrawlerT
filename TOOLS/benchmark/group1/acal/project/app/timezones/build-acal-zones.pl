#!/usr/bin/env perl
#
# A script which builds the aCal strings used for timezones. This
# is a temporary hack until we have a reliable timezone server we
# can use instead.

use warnings;
use strict;

my $debug = 0;
my $output_file = "-";
my $print_strings = 0;
my $helpmeplease = 0;

use Getopt::Long qw(:config permute);  # allow mixed args.

GetOptions ('debug!'    => \$debug,

            'output=s'  => \$output_file,
            'strings'   => \$print_strings,

            'help'      => \$helpmeplease  );

usage() if ( $helpmeplease );


my @zonelist = (
'Europe/London',
'Europe/Lisbon',
'Europe/Paris',
'Europe/Berlin',
'Europe/Bucharest',
'Europe/Prague',
'Europe/Athens',
'America/Sao_Paulo',
'America/Halifax',
'America/New_York',
'America/Chicago',
'America/Denver',
'America/Los_Angeles',
'America/Anchorage',
'Pacific/Honolulu',
'Pacific/Apia',
'Pacific/Auckland',
'Australia/Brisbane',
'Australia/Adelaide',
'Asia/Tokyo',
'Asia/Singapore',
'Asia/Bangkok',
'Asia/Kolkata',
'Asia/Muscat',
'Asia/Tehran',
'Asia/Baghdad',
'Asia/Jerusalem',
'America/St_Johns',
'Atlantic/Azores',
'America/Noronha',
'Africa/Casablanca',
'America/Argentina/Buenos_Aires',
'America/La_Paz',
'America/Indiana/Indianapolis',
'America/Bogota',
'America/Regina',
'America/Tegucigalpa',
'America/Phoenix',
'Pacific/Kwajalein',
'Pacific/Fiji',
'Asia/Magadan',
'Australia/Hobart',
'Pacific/Guam',
'Australia/Darwin',
'Asia/Shanghai',
'Asia/Novosibirsk',
'Asia/Karachi',
'Asia/Kabul',
'Africa/Cairo',
'Africa/Harare',
'Europe/Moscow',
'Atlantic/Cape_Verde',
'Asia/Yerevan',
'America/Panama',
'Africa/Nairobi',
'Asia/Yekaterinburg',
'Europe/Helsinki',
'America/Godthab',
'Asia/Rangoon',
'Asia/Kathmandu',
'Asia/Irkutsk',
'Asia/Krasnoyarsk',
'America/Santiago',
'Asia/Colombo',
'Pacific/Tongatapu',
'Asia/Vladivostok',
'Africa/Ndjamena',
'Asia/Yakutsk',
'Asia/Dhaka',
'Asia/Seoul',
'Australia/Perth',
'Asia/Riyadh',
'Asia/Taipei',
'Australia/Sydney'
);

my $name_strings = "";
my $name_array = "";
my $zone_strings = "";

for my $zone_name ( sort @zonelist ) {
  my $varname = $zone_name;
  $varname =~ s{[/ ]}{}g;
  my $filename = 'vtimezones/'.$zone_name.'.ics';
  my $zone_data = do {
    local $/ = undef;
    open my $fh, "<", $filename or do {
      print STDERR "Skipping $zone_name - could not open $filename: $!";
      next;
    };
    <$fh>;
  };
  $zone_data =~ s/^.*(BEGIN:VTIMEZONE.*END:VTIMEZONE\r?\n).*$/$1/gs;
  $zone_data =~ s/X-LIC-LOCATION:$zone_name\r?\n//g;
  $zone_data =~ s/\r?\n/\\r\\n/g;
  $name_strings .= qq{<string name="tzName$varname">$zone_name</string>\n};
  $zone_strings .= qq{		new String[] {"$zone_name", "$zone_data" },\n};

  $name_array .= "\t<item>\@string/tzName$varname</item>\n";
}

if ( $output_file ne "-" ) {
  close STDOUT;
  open STDOUT, ">", $output_file;
}

if ( $print_strings ) {
  print <<EOHEADER;
<?xml version="1.0" encoding="utf-8"?>
<resources>
EOHEADER

  print $name_strings;
  print "\n";

  print "<string-array name=\"timezoneNameList\">\n";
  print $name_array;
  print "</string-array>\n";

  print "</resources>\n";
}
else {
  print <<EOHEADER;
package com.morphoss.acal.davacal;
public class ZoneData {
	public final static String[][] zones = new String[][] {
EOHEADER

  print $zone_strings;

  print "	};\n}\n";
}

