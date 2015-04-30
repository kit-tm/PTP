# Tor-based P2P Library Specification (early draft)

**WARNING: this is heavily outdated!** Please refer to the examples,
the JUnit test cases, javadoc and the sources themselves...

## 0. Scope

This document describes the API and functionality of a Peer-to-Peer (P2P)
middleware library that uses the Tor network and Tor hidden services for
establishing P2P connections. Through the use of Tor, NATs can easily be
circumvented and the identities of peers as well as their peering
relationships can remain hidden.
  
## 1. Base functionality

### 1.1 Constants and Parameters

  NaCL approach? (as little choice as possible, good defaults)

### 1.2 Functions

#### init()

  Start Tor. Set up a new Hidden Service Identifier if there is none.
  Register Listener.

#### createNewID()

#### getID()

#### sendMessage(peerID, message)
    
  Needs connection manager, otherwise too many new TCP
  connections.

#### registerListener(Listener)

### 1.3 Connection manager

  Hidden from user. Sets up TCP connections to peers and reuses them
  across sendMessage() calls.

### 1.4 Evaluation
  
* Ping-Pong App
  * Measuring latency
  * Also for measuring delay after pseudonym change?
* Something useful for testing API - one-hop DHT?

## 2. Direct connections

* The idea is to use the Tor link for NAT traversal or forming local
  communication links. This way: more speed.
* Tor link is used by default, direct/local connection upon request (after Tor
  link is established).
* Implement this before public release, to test API.
  
### 2.1 Functions

#### initDirectConnection(peerID)

* After this new sendMessage() function or different Library object?
  In order to be clear how message is sent.

* sendMessage parameter format for large data pieces? (files?)

### 2.2 Evaluation

* File transfer App a la Omnibus or Onionshare

## 3. Future: crypto

* Communication with Tor hidden services has crypto built in.

* For local/direct connections - NaCl?

## 4. Future: Pseudonym registration / sybil attack protection

Using Bitcoin passports or something similar...

## 5. Android compatibility

Must be checked. Also bundling with Tor.
