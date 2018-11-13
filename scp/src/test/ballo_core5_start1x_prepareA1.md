# prepare A1

## bump prepared A2

### confirm prepared A2

#### accept commit

##### quorum a2

###### quorum prepared a3

####### Accept more commit A3

* Quorum externalize A3

####### v-blocking accept more A3

* Confirm A3
* Externalize A3
* other nodes Confirm A4..5
* other nodes Externalize A4..5

###### v-blocking

* prepared A3
* prepared A3+B3
* confirm A3

###### Hang - does not switch to B in CONFIRM

* Network EXTERNALIZE
* Network CONFIRMS other ballot at same counter
* Network CONFIRMS other ballot at a different counter

##### v-blocking

###### CONFIRM

* CONFIRM A2
* CONFIRM A3..4
* CONFIRM B2

###### EXTERNALIZE

* EXTERNALIZE A2
* EXTERNALIZE B2

#### get conflicting prepared B

* same counter

* higher counter

### Confirm prepared mixed

* mixed A2
* mixed B2

## switch prepared B1

## switch prepare B1
