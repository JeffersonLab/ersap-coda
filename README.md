# ERSAP-CODA

**ERSAP-CODA** provides integration between the [ERSAP](https://github.com/JeffersonLab/ersap-java) streaming framework and the [CODA](https://coda.jlab.org) (CEBAF Online Data Acquisition) system at Jefferson Lab.  
It enables real-time, distributed data processing by connecting ERSAP Java/C++ services into CODA workflows.

---

## 🧩 Dependencies

To build and run `ersap-coda`, you must install:

- [xmsg-java](https://github.com/JeffersonLab/xmsg-java)
- [ersap-java](https://github.com/JeffersonLab/ersap-java)
- [ersap-cpp](https://github.com/JeffersonLab/ersap-cpp)

### System Requirements

- Java JDK 8 or higher
- C++14-compliant compiler (e.g., GCC 5+, Clang 3.4+)
- CMake 3.5 or newer
- [ZeroMQ](https://zeromq.org/) (v4.x)
- [Protocol Buffers](https://developers.google.com/protocol-buffers) (v3.x)
- Access to Maven local repository (`~/.m2/repository`)

---

## ⚙️ Environment Setup

Set the ERSAP installation path before building:

```bash
export ERSAP_HOME=$HOME/ersap
export PATH=$ERSAP_HOME/bin:$PATH
export LD_LIBRARY_PATH=$ERSAP_HOME/lib:$LD_LIBRARY_PATH
```

This variable is **critical**: all ERSAP components will deploy into `$ERSAP_HOME`, and `ersap-coda` relies on this layout.

---

## 📦 System Package Installation

### Ubuntu

```bash
sudo apt update
sudo apt install build-essential cmake \
                 libzmq5-dev libprotobuf-dev protobuf-compiler \
                 openjdk-11-jdk
```

### macOS

```bash
xcode-select --install
brew install cmake zeromq protobuf openjdk@11
```

Make sure Java is available:

```bash
export JAVA_HOME=$(/usr/libexec/java_home)
export PATH=$JAVA_HOME/bin:$PATH
```

---

## 🚀 Installing ERSAP and XMsg

### 1. `xmsg-java`

```bash
git clone https://github.com/JeffersonLab/xmsg-java.git
cd xmsg-java
./gradlew publishToMavenLocal
./gradlew deploy
```

---

### 2. `ersap-java`

```bash
git clone https://github.com/JeffersonLab/ersap-java.git
cd ersap-java
./gradlew publishToMavenLocal
./gradlew deploy
```

Optional IDE integration:

```bash
./gradlew cleanIdea idea        # IntelliJ
./gradlew cleanEclipse eclipse  # Eclipse
```

---

### 3. `ersap-cpp`

```bash
git clone https://github.com/JeffersonLab/ersap-cpp.git
cd ersap-cpp
./configure --prefix="$ERSAP_HOME"
make
make install
```

This installs ERSAP C++ libraries and services to `$ERSAP_HOME`.

---

## 🛠️ Building ERSAP-CODA

### Java

From the top of the `ersap-coda` source directory:

```bash
./gradlew publishToMavenLocal
./gradlew deploy
```

### C++

```bash
cd main/cpp
mkdir build && cd build
cmake ..
make
make install
```

This builds the native C++ integration components and installs them into `$ERSAP_HOME`.

---

## ✅ Final Verification

Ensure binaries and libraries are correctly deployed:

```bash
ls $ERSAP_HOME/bin
ls $ERSAP_HOME/lib
```

You are now ready to configure and run `ersap-coda` within your DAQ workflows.

---


# How-To Guide: Starting the CODA Data Processing Pipeline with ERSAP from Scratch

This document provides technical instructions for initializing and managing a real-time CODA data processing pipeline using the ERSAP framework. The pipeline includes integration with the Event Transfer (ET) system, CODA DAQ, and an ERSAP-based stream processing chain.

---

## Launch the ET System Interface for ERSAP

In a new terminal window, start the ET system which acts as a buffer between the DAQ and ERSAP:


    et_start -f /tmp/et_SRO_ERSAP -v -d -n 1000 -s 1000000 -p 23911
Parameters:

-f: Path to the ET file (ET system name)

-n: Number of events in memory

-s: Event size in bytes

-p: TCP port number for the ET server

## Start CODA DAQ System
Use the CODA Run Control GUI or terminal interface to:

Select the **_spPilot_** configuration.

Execute the standard run sequence: **_Configure_** → **_Download_** → **_Prestart_** → **_Go_**.

## Launch the ERSAP Data Processing Pipeline
In a new terminal window:

    cd SRO/ersap
    . set_env.sh                   # Run this once per terminal session
    $ERSAP_HOME/bin/ersap_shell
Once inside the ERSAP shell, start the local processing pipeline:

    ersap> set threades 1
    ersap> run_local

## Stopping the ERSAP Pipeline
Graceful Exit: Press **_CTRL+C_** and wait for a clean shutdown. ERSAP ensures all threads terminate properly.

Forced Termination: Use the following command to forcefully stop the pipeline:

    /home/hatdaq/SRO/ersap/kill_ersap

## Restarting the Pipeline Without Restarting CODA DAQ
You may stop and restart ERSAP independently from CODA:

If you performed a hard kill, reinitialize using:

    ersap_shell
    run_local

If you exited gracefully, start a new pipeline session by repeating only:

    run_local

## Configuring the ERSAP Processing Pipeline
To edit the processing pipeline configuration, launch the ERSAP shell:

    ersap> edit services
This opens a YAML file describing the service composition:

    ---
    io-services:
    reader:
    class: org.jlab.ersap.actor.coda.engine.CodaEtSourceEngine
    name: Source
    writer:
    class: org.jlab.ersap.actor.coda.engine.binary.CodaSinkBinaryEngine
    name: Sink
    
    services:
    - class: org.jlab.ersap.actor.coda.engine.binary.CodaHitFinderBinaryEngine
      name: HitFinder
    
    - class: SROPrinterService
      name: SoftTrig
      lang: cpp
    
    - class: org.jlab.ersap.actor.coda.engine.binary.MultiChannelDigitizerDisplayBinary
      name: Histogram
    
    configuration:
    io-services:
    reader:
      et_name: "/tmp/et_SRO_ERSAP"
      et_port: 23911
      et_station: "ersap"
      fifo_capacity: 128
    writer:
      output_file: "/tmp/output_sro_data.bin"
      frames_per_file: 1000
    services:
    HitFinder:
      stream_source: "et"
      verbose: "no"
    SoftTrig:
      max_hits_to_show: 100
    Histogram:
      hist_bins: 100
      hist_min: 100
      hist_max: 8000
      roc_id: 2
      slot: 15
    mime-types:
      - binary/data-evio
      - binary/sro-data

## Output Histogram Configuration
To control histogram rendering in the accumulation mode, adjust parameters under the 

    Histogram:
      hist_bins: 100
      hist_min: 100
      hist_max: 8000
      roc_id: 2
      slot: 15
    mime-types:
      - binary/data-evio
      - binary/sro-data

hist_bins: Number of bins

hist_min, hist_max: Range for binning

hist_titles: Histogram channel identifiers

grid_size: Layout matrix (e.g., 4 for 4x4 visualization)

## Notes
The CODA DAQ system can be restarted independently without affecting the ERSAP pipeline.

It is recommended to monitor pipeline behavior during long-running sessions to ensure data integrity and thread consistency.

## CodaSinkBinaryEngine Output File Splitting

The `CodaSinkBinaryEngine` now supports automatic file splitting based on the number of frames written:

- `output_file`: The base path for the output binary file (e.g., `/tmp/output_sro_data.bin`).
- `frames_per_file`: The maximum number of frames to write to each file before rolling over to a new file. When the limit is reached, a new file is created with a numeric postfix (e.g., `output_sro_data-1.bin`, `output_sro_data-2.bin`, etc.).

**Example configuration:**

```yaml
writer:
  output_file: "/tmp/output_sro_data.bin"
  frames_per_file: 1000
```

This ensures that large data sets are automatically split into manageable files for easier handling and post-processing.

### Output Configuration for `CodaSinkFileEngine`

The `CodaSinkFileEngine` writes CSV files to the `$ERSAP_USER_DATA/data/output` directory.  
By default, each file uses the prefix `out_` in its name.

To customize the output file prefix, use the following command in the `ersap-shell` CLI:
```
set outputFilePrefix
```

---

### Using EVIO File Replay in Streaming Mode

To replay a stored EVIO file in streaming mode, you must use the `CodaFileSourceEngine`  
instead of the `CodaEtSourceEngine`.

Below is an example YAML configuration:

```yaml
reader:
  class: org.jlab.ersap.actor.coda.engine.CodaFileSourceEngine
  name: Source
```

