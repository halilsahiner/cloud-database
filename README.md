# Cloud Database

This project aims to create a distributed key value database from scratch with Java. It uses the Consistent Hashing algorithm to store key value pairs and Ring Based Leader Election algorithm to manage distributed nodes. 

The project has two main components which are the client and the Key-Value Stores(KVS). The client is a CLI tool that connects to KVSs and write, update, read and delete key value pairs. KVS has the responsibility to manage the given pair and stores it effeciently to respone client as fast as possible. 
