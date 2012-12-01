#!/bin/sh -x

DESTDIR=traceset
URL="http://secretaire.dorsal.polymtl.ca/~fgiraldeau/traceset/lttng-traceset-current.tar.bz2"

mkdir -p $DESTDIR
wget -O - "$URL" | bzip2 -d | tar -C $DESTDIR -x
