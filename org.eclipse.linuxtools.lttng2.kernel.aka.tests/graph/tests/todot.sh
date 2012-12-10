#!/bin/sh

for i in $(ls -1 *.dot); do
	echo $i
	dot -Tpng -o $i.png $i
done

