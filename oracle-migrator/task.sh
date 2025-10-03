#!/bin/bash

code . &
sleep 3

WINDOWS=$(xdotool search --onlyvisible --class code)
LAST_WINDOW=$(echo "$WINDOWS" | tail -n 1)

xdotool windowactivate --sync $LAST_WINDOW
sleep 0.5

xdotool key ctrl+alt+i
sleep 0.5

xdotool key ctrl+n
sleep 0.5

xdotool key ctrl+period
sleep 0.3

xdotool key Up
sleep 0.3

xdotool key Return
sleep 0.3

xdotool type "refactor procedures to java classes"
xdotool key Return