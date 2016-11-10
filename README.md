# Peer-Tor-Peer (PTP)

A proof-of-concept prototyping library for peer-to-peer
apps using Tor and Tor hidden services for connectivity.

Good starting points:

* [Usage](README.md#Usage)
* javadoc (see [Javadoc](README.md#javadoc))
* the examples, especially `edu.kit.tm.ptp.examples.PTPSendExample`
* the JUnit tests, especially `edu.kit.tm.ptp.test.PTPTest`

*WORK IN PROGRESS*

## How to build

PeerTorPeer uses the gradle build system.
Run `./gradlew assemble` to compile the code.

If you want to use eclipse run `./gradlew eclipse` to create proper
`.classpath` and `.project` files.

## Javadoc

Run `./gradlew javadoc` to create the Javadoc in `build/docs/javadoc/`.

## Adding PTP to a Java Application

To use PTP in your Java application build PTP and add `build/libs/PTP-full.jar` to your classpath.
For example: `javac -cp .:pathtojar/PTP-full.jar ...` and `java -cp .:pathtojar/PTP-full.jar ...` 
where pathtojar is the path to the folder containing PTP-full.jar.
To be able to use PTP you also have to copy the `config` folder to the folder from where you run your application. 

## Usage

First a new PTP object has to be constructed: `PTP ptp = new PTP();`.
After that you should register all classes you want to be able to send and receive using `ptp.registerClass(Class)`.
Notice that the order of registrations is important and has to be the same both at the sender and receiver.
Also note that classes referenced (e.g. in instance variables) by a registered class also have to be registered themselves.
The following classes are already registered by default: `int, String, float, boolean, byte, char, short, long, double, byte[]`.
PTP uses [Kryo](https://github.com/EsotericSoftware/kryo) to serialize objects.


You can set listeners for sent and received messages.
After that `ptp.init()` should be called which sets up PTP and starts Tor.
Next call `ptp.reuseHiddenService()` or `ptp.createHiddenService()` to use an existing hidden service or create a new one.
You can now send and receive messages. Keep in mind that it can take some time for a hidden service to be reachable (about 1-2 minutes). 


The method `ptp.sendMessage()` allows to send `byte[]` messages and objects of previously registered classes.
It is save to call the method from several threads in parallel.
It also allows to pass a timeout in milliseconds which defaults to infinity.
If the timeout expires before the message was sent successfully the send listener gets informed.
Keep in mind that the service PTP provides is not reliable.
A listener might be informed that a message was sent even though the receiver never gets the message. 
To close PTP cleanly call `ptp.exit()`. If you want to delete your hidden service you can call `ptp.deleteHiddenService()` after `ptp.exit()`.

## Tests

Start `./gradlew test` to run the tests.

You can also set up your own tor network for testing purposes and run the tests there.
For this to work you need chutney (https://git.torproject.org/chutney.git).
Set the location of chutney in the variable `CHUTNEY_DIR` in chutneyTests.sh.
Run `./chutneyTests.sh` to set up a local tor network and start the tests.

## Android support

- https://git.scc.kit.edu/TM/ptp-android

- https://git.scc.kit.edu/TM/ptp-androidexample


## Projects using PTP

[AluShare](https://github.com/weichweich/AluShare)
