# Peer-Tor-Peer (PTP)

A proof-of-concept prototyping library for peer-to-peer
apps using Tor and Tor hidden services for connectivity.

Good starting points:

* the examples, especially `edu.kit.tm.ptp.examples.PTPSendExample`
* the JUnit tests, especially `edu.kit.tm.ptp.test.PTPTest`
* javadoc (generate it yourself please)

*WORK IN PROGRESS*

## How to build

PeerTorPeer uses the gradle build system.
Run `./gradlew assemble` to compile the code.

If you want to use eclipse run `./gradlew eclipse` to create proper
`.classpath` and `.project` files.

## Tests

Start `./gradlew test` to run the tests.

You can also set up your own tor network for testing purposes and run the tests there.
For this to work you need chutney (https://git.torproject.org/chutney.git).
Set the location of chutney in the variable `CHUTNEY_DIR` in chutneyTests.sh.
Run `./chutneyTests.sh` to set up a local tor network and start the tests.

## Android support

- https://git.scc.kit.edu/TM/ptp-android

- https://git.scc.kit.edu/TM/ptp-androidexample

## Using PTP in a Java Application

To use PTP in your Java application build PTP and add `build/libs/PTP.jar` to your classpath.
For example: `javac -cp .:pathtojar/PTP.jar ...` and `java -cp .:pathtojar/PTP.jar ...` 
where pathtojar is the path to the folder containing PTP.jar.
To be able to use PTP you also have to copy the `config` folder to the folder from where you run your application. 

## Projects using PTP

[AluShare](https://github.com/weichweich/AluShare)
