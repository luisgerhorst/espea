all: compile test

ARGS = -cp ../lib/*:./lib/*:../build/libs/trunk.jar:.

compile:
	cd ./hv && make OPT_CFLAGS="-O2 -g"
	javac $(ARGS) *.java

run:
	java $(ARGS) -server -Xmx16g -d64 RunBenchmark $(t)

runq:
	java $(ARGS) RunBenchmark skipgen $(t)

.PHONY: compile test run runq
