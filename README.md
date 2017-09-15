# The Electrostatic Potential Evolutionary Algorithm

This implementation of the algorithm described in "Obtaining Optimal Pareto Front Approximations using Scalarized Preference Information" by Braun et al. was created as part of the seminar [Black Box Challenge](https://www.cs12.tf.fau.de/lehre/lehrveranstaltungen/seminare/black-box-challenge-meta-heuristic-optimization-for-arbitrary-problems/) at the [FAU](https://www.fau.de).

Refer to [report](report/report.pdf) for an in-depth guide to understanding the code (chapter 3, Implementation), also refer to the [Opt4J](http://opt4j.sourceforge.net) website for tutorials if you are not familiar with it yet.

## Usage

You can either use Eclipse or Gradle to build and run the app. After startup the Opt4J control panel opens up from where you can run the optimizer on a selected problem using the _Run_ button on the upper left.

### [Gradle](https://gradle.org)

Build and run the app using

``` Shell
./gradlew run
```

If you have gradle installed you can also use `gradle` instead of `./gradlew`.

### [Eclipse](http://www.eclipse.org)

Open the project in Eclipse (File > Open Projects from File System...) and select the `bbc-ss17-espea.launch` launch configuration in the Navigator, then select _Run_ (in the menu Run > Run).
