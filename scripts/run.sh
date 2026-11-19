#!/bin/bash
case $1 in
    "6502" )
	java -jar /home/jpoffen/git/simpleCPU/modules/cpu/6502/target/emu6502-0.0.7-SNAPSHOT-jar-with-dependencies.jar
	;;
    "z80" )
	java -jar /home/jpoffen/git/simpleCPU/modules/cpu/z80/target/emuZ80-0.0.1-SNAPSHOT-jar-with-dependencies.jar
	;;
esac
