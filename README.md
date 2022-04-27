# ersap-coda

### Building
1. Define ERSAP_HOME and ERSAP_USER_DATA environmental variables

2. Clone and build ersap-java project from the Git repository


    $ clone https://github.com/JeffersonLab/ersap-java.git
    $ ./gradlew deploy

3. Clone and build ersap-coda project from the Git repository


    $ clone https://github.com/JeffersonLab/ersap-coda.git
    $ ./gradlew deploy

### Configuring
ersap-coda project provides an example for a CODA data processing 
application design and configuration. Design and configuration of an 
ERSAP based application is defined as a yaml file. 

1. Copy ersap-coda.yaml to your user_data directory.


     $ cp ersap-coda.yaml $ERSAP_USER_DATA/config

There are a few conventions for this application configuration. Namely:
channel name/ID is defined as:

    crate-slot-channel

This example application assumes only a single crate, thus crate = 1.
Using service.yaml one can configure event identification algorithm 
by selecting sliding window size and sliding step, beam center channel, as well as 
histogram parameters, such as histogram/grid assignments, histogram bins, min, max, title, etc.

Service configuration section of the example application is shown below:
    
    configuration:
    io-services:
    writer:
    frame_title: "ERSAP"
    frame_width: 1200
    frame_height: 600
    # hist_titles is a string containing the list of crate-slot-channel separated by ,
    hist_titles: "1-3-0, 1-3-4, 1-3-8, 1-3-14"
    hist_bins: 100
    hist_min: 1000
    hist_max: 9000
    # grid_size defines a layout for histogram visualization
    # (e.g. 5 will plot 25 histograms in 5x5 matrix)
    grid_size: 2
    services:
    AggProcessor:
    sliding_widow_size: 32
    sliding_step: 4
    #  beam_center is a string: crate_slot_channel.
    #  If not defined (commented out) algorithm will not consider a cell with a max charge
    beam_center: "1-3-12"

### Event identification algorithm
For the requested channels the algorithm requires only single hit per channel
within the sliding window, as well as the beam-center channel 
(a channel that is expected to get most of the beam events) to have
a maximum charge. So, if there are multiple hits of the same channel within the 
sliding window, that window is not considered to have a beam event. Also, if we 
have single hits within the sliding window, but max charge is detected for other 
than expected beam-hit channel, then that window will not be considered as a beam event.

### Input file

This package contains FileReader service that reads EVIO format data from the CODA VTP/DC.
In the future we will provide a service that gets data off the ET system.
Even though one can define the input file location using ERSAP CLI, the default location of the 
input files (EVIO format) is:

    $ ERSAP_USER_DATA/data/input

To define the data-file the name (just the name, not the PATH) should be recorded in the following file:

    $ERSAP_USER_DATA/config/files.txt

### Running

The code is compiled with the JDK 11. If you do not trust local JAVA installation download 
JRE 11 from:

    https://userweb.jlab.org/~gurjyan/clara-cre/linux-64-11.tar.gz

and point $JAVA_HOME where you unpacked the above JRE.

I suggest using ERSAP SLI to run the application. This shell will allow request/control of 
resources (e.g. threads for data processing), as well as modifications of the application 
algorithm (enable/disable beam-center channel request), and many more.

So, to run the CLI type:

    $ERSAP_HOME/bin/ersap-shell

To run the application type:

    ersap> run local

To edit application configuration type:

    ersap> edit services

To learn more what can ERSAP do type:

    ersap> help

### Binary distribution
If you do not want to compile the binaries of this package, along with the ERSAP installation can be downloaded (~2GB) from:
    
    https://userweb.jlab.org/~gurjyan/ersap-cre/ersap-coda.tar.gz

This includes myErsap and user_data directories that you can use by setting proper 
environmental variables:

    $ setenv ERSAP_HOME myErsap
    $ setenv ERSAP_USSR_DATA user_data



