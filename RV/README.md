# Runtime Verification
Source code for the runtime verification of the simulation in the paper "Security-Minded Verification of Cooperative Awareness Messages" currently submitted to IEEE Transactions on Dependable and Secure Computing.

To run, first install LamaConv by following their instructions at (usually just downloading a zip and extracting it) https://www.isp.uni-luebeck.de/lamaconv

Compile our code with:
```bash
javac Monitor.java
```

Run our code with:
```bash
java Monitor <path_to_lamaconv> LogFile QueueLimit
```

Example:
```bash
java Monitor /home/cpscsc/s01rc2/lamaconv/ secp256-50cars.txt 71
```

Everything can be customised (LTL property, LTL alphabet, etc.) by editing the file Monitor.java.

After running, you should see in the terminal a line being printed for every violation, including the details of the event that violated the property.   
A final print will say how many times the property was violated while reading from the log file.
