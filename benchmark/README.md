# Benchmarking

Enusure the [`hv`](http://lopez-ibanez.eu/hypervolume) executeable available
under `./hv/hv` (due to licensing it is not included in the repository). Then
run `make` to compile both the script and `hv` and `make run t=TEST` to run a
benchmark. `TEST` can be either `general`, `cachemut`, `cachemut_rl`, `parcompl`
or `gensize`. Have a look at the constants in `RunBenchmark.java` to adjust
repetitions (200 by default, takes some time), warmup reps, evaluation limit and
other options. The results for the according test are available in the `data` directory. `data-archive` contains results referenced in the report.
