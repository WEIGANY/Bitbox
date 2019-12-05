# Bitbox project 

## About the App
It is a bitbox - like P2P file sync system 

## How to run the App
1. run the target file at the location: ./target/bitbox-0.0.1-SNAPSHOT-jar-with-dependencies.jar
2. use command : java -cp bitbox.jar unimelb.bitbox.Peer

## How to build the App
1. Run `maven build` to install all dependencies.
2. You can set app configuration at the file at ./configuration.properties


##

## Note 

1. The private key file should be .der file and it should be named private_key
2. In Udp mode the Clientport can be same as the udpPort
3. But in tcp mode it should be different with port
4. In udp mode blocksize is suggested to be equal or less than 5000 

