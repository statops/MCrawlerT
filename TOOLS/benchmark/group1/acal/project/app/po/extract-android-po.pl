#!/usr/bin/env perl
#
# Quick hack to build a .pot file from an Android strings.xml file
#
# Copyright: Morphoss Ltd <http://www.morphoss.com/>
# Author: Andrew McMillan <andrew@mcmillan.net.nz>
# License: CC-0, Public Domain, GPL v2, GPL v3, Apache v2 or Simplified BSD.
#          Other licenses considered on request.
#

use strict;
use warnings;

use IO::Handle;
use Getopt::Long qw(:config permute);  # allow mixed args.


# Stuff that should not be hard-coded, but is :-)
my $resources_dir = "../res";
my @extract_filenames = ( "strings", "timezonenames" );
my $build_filename = "strings";
my $messages_filename = "messages.pot";

my $debug = 0;
my $extractmode = 0;
my $buildmode = 0;
my $helpmeplease = 0;

GetOptions ('debug!'    => \$debug,

            'extract!'  => \$extractmode,
            'build!'    => \$buildmode,

            'help'      => \$helpmeplease  );

usage() if ( $helpmeplease || ($extractmode && $buildmode) );

my $code_dir = $0;
$code_dir =~ s{/[^/]*$}{};
chdir $code_dir;

if ( $extractmode ) {
  # Update/extract the strings for the messages.po file
  extract_po_file(\@extract_filenames, $messages_filename);
}
elsif ( $buildmode ) {
  # From the language po files build the various strings files.
  build_strings_files($messages_filename, $build_filename );
}
else {
  usage();
}


=item
Provides basic help to the user.
=cut
sub usage {
  print "You must specify either '--extract' or '--build'\n";
  exit 0
}


=item
Extracts the strings from some Android strings.xml into a messages.po file
=cut
sub extract_po_file {
  my $filenames = shift;
  my $outfile = shift;

  my @xgettext = ( "xgettext", "-L", "PO", "-o", $outfile, "-" );
  open( XGETTEXT, "|-", @xgettext );
  autoflush XGETTEXT 1;
  printf( "Extracting to '%s'\n", $outfile ) if ( $debug );

  for my $filename ( @$filenames ) {
    $filename = $resources_dir ."/values/". $filename . ".xml";
    printf( XGETTEXT "#SourceFileStart:%s\n", $filename );
    printf( "Extracting strings from '%s'\n", $filename ) if ( $debug );
    open( XMLFILE, "<", $filename );
    while( <XMLFILE> ) {
      m{<string .*?name="(.*?)".*?>(.*?)</string>} && do {
        my $msgid = $1;
        my $msgstr = $2;
        $msgstr =~ s{\\'}{'}g;
        $msgstr =~ s/"/&quot;/g;
        printf( XGETTEXT 'msgid "%s"%s', $msgid, "\n" );
        printf( XGETTEXT 'msgstr "%s"%s', $msgstr, "\n\n" );
      };
      m{<!--(.*?)-->} && do {
        printf( XGETTEXT "#%s\n", $1 );
      };
    }
    close(XMLFILE);
    printf( XGETTEXT "#SourceFileEnd:%s\n\n", $filename );
  }
  printf( "Extraction completed\n" ) if ( $debug );
  # close(XGETTEXT);
  
}


=item
Finds the translated .po files in a directory and uses them to construct a new strings.xml file
for each one by merging the details from the original strings.xml with the translated strings
from the la_NG.po file.
=cut
sub build_strings_files {
  my $template_file = shift;
  my $stringsfile = shift;

  # Now merge into each translation
  opendir(my $dh, ".");
  while( my $fn = readdir($dh) ) {
    if( $fn =~ m{^([a-z]{2}(_[A-Z]{2})?)\.po$} ) {
      my $lang = $1;
      printf( "Building language: %s\n", $lang );
      my $strings = get_translated_strings($fn);
      merge_into_xml( $lang, $strings, $stringsfile );
    }
  }
  closedir($dh);
}


=item
=cut
sub get_translated_strings {
  my $filename = shift;

  my $strings = {};
  open( TRANSLATED, "<", $filename );

  my $msgid = undef;
  while( <TRANSLATED> ) {
    next if ( /^\s*#/ );

    if ( /^\s*msgid \"(.*)"\r?\n?$/ ) {
      $msgid = $1;
      $strings->{$msgid} = "";
    }
    elsif ( defined($msgid) ) {
      /^\s*(msgstr )?"(.*)"\r?\n?$/ && do {
        $strings->{$msgid} .= $2;
      };
    }
  }

  close(TRANSLATED);

#  foreach my $s ( keys %{$strings} ) {
#    printf( STDOUT "<string name=\"%s\">%s</string>\n", $s, $strings->{$s} );
#  }

  return $strings;
}


=item
=cut
sub merge_into_xml {
  my $lang = shift;
  my $strings = shift;
  my $strings_filename = shift;

  $lang =~ s{_([A-Z]{2})}{-r$1};
  my $in_filename = sprintf( '%s/values/%s.xml', $resources_dir, $strings_filename);

  my $outdir = sprintf('%s/values-%s', $resources_dir, $lang);
  mkdir $outdir unless( -d $outdir );
  my $out_filename = sprintf( '%s/%s.xml', $outdir, $strings_filename);
  open( XMLIN, "<", $in_filename );
  open( XMLOUT, ">", $out_filename );

  while( <XMLIN> ) {
    if ( ! m{<!--} && m{<string (.*?)name="(.*?)"(.*?)>(.*?)</string>} ) {
      my $preamble = (defined($1)?$1:"");
      my $msgid = $2;
      my $postamble = (defined($3)?$3:"");
      my $msgstr = $4;
      $msgstr =~ s{\\'}{'}g;
      next if ( ! defined($strings->{$msgid}) || $msgstr eq $strings->{$msgid} );
      next if ( $strings->{$msgid} eq "" );
      $strings->{$msgid} =~ s{"}{&quot;}g;
      $strings->{$msgid} =~ s{(['\\])}{\\$1}g;
      printf( XMLOUT '<string %sname="%s"%s>%s</string>%s',
                        $preamble, $msgid, $postamble, $strings->{$msgid}, "\n" );
    }
    else {
      print XMLOUT unless( $_ =~ m{^\s*$} );
    }
  }
  
  close(XMLIN);
  close(XMLOUT);
}

