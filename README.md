# Peer-Tor-Peer (PTP)

A proof-of-concept prototyping library for peer-to-peer
apps using Tor and Tor hidden services for connectivity.

Good starting points:

* the examples, especially `edu.kit.tm.ptp.examples.PTPSendExample`
* the JUnit tests, especially `edu.kit.tm.ptp.test.PTPTest`
* javadoc (generate it yourself please)

## How to build

PeerTorPeer uses the gradle build system.
Run `gradle assemble` to compile the code.

If you want to use eclipse run `gradle eclipse` to create proper
`.classpath` and `.project` files.

*WORK IN PROGRESS*

## Tests

Start `gradle test` to run the tests.

You can also set up your own tor network for testing purposes and run the tests there.
For this to work you need chutney (https://git.torproject.org/chutney.git).
Set the location of chutney in the variable `CHUTNEY_DIR` in chutneyTests.sh.
Run `./chutneyTests.sh` to set up a local tor network and start the tests.
