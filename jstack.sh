#!/bin/bash +xv

if [ $# -eq 0 ]; then
        echo >&2
	echo >&2 "Usage: $(basename $0) <pid> [  <count> [ [ <delay> ] [ <search pattern> ] ] ]"
	echo >&2 "    Defaults: count = 10, delay = 0.5 (seconds)"
        echo >&2
	exit 1
fi

pid=$1          # required
count=${2:-10}  # defaults to 10 times
delay=${3:-0.5} # defaults to 0.5 seconds
search=$4	# if this pattern is not in jstack file delete file

for ((i=1; i <= $count ; i++))
do
	jstack_file=jstack.$pid.$(date +%H%M%S.%N)
	jstack_out=$(jstack -l $pid)
	if [ -z "${search}" ] ; then
		echo "$jstack_out" > $jstack_file
	else
		[[ $jstack_out == *"$search"* ]] && echo "$jstack_out" > $jstack_file
	fi
	sleep $delay
	echo -n "."
done
echo
 
exit 0