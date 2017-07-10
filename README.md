# JavaResourceMonitor Agent

JavaResourceMonitor is a java agent that can be attached to IBM Java on startup to monitor its resource usage.
Currently the agent profiles the JVM for CPU usage which is obtained using `com.ibm.lang.management.JvmCpuMonitorMXBean`.
The CPU usage is recorded in a file named `cpulogs` in current directory.

## Usage

Run following command in the directory `javaresourcemonitor` to compile the java agent and generate the jar file `javaresourcemonitor.jar`:

```text
gmake
```

The agent accepts two optional parameters:
* `frequency` - frequency of profiling the JVM. It is specified in milliseconds. Default value is 1000 ms.
* `duration` - duration of profiling the JVM from startup. It is specified in seconds. Default value is 0 which
means profile JVM for its whole duration.

You can use the agent as:

* `java -javaagent:<path to javaresourcemonitor.jar> <Application>`

In the above case, the agent would profile JVM for CPU usage at a frequency of 1000 ms for the duration of the application.

* `java --javaagent:<path to javaresourcemonitor.jar>=frequency=10000,duration=30 <Applicatioin>`

In this case, the agent would profile JVM for CPU usage at a frequency of 10000 ms for a duration of 30 secs or for the duration
of the application, whichever is smaller.

In all cases, the agent records the profiling information in `cpulogs` file in current working directory.

## Explanation of output

As stated earlier, the agent uses `com.ibm.lang.management.JvmCpuMonitorMXBean` to get CPU usage.
The `JvmCpuMonitorMXBean` provides APIs to obtain JVM CPU usage information in terms of thread categories.
The agent records the CPU usage of JVM in terms of following thread categories as defined by `JvmCpuMonitorMXBean`:

* `System-JVM`
* `Application`
* `Resource-Monitor`

For each of these categories, the agent reports the absolute CPU usage in microseconds in the last sample,
and its percentage with respect to overall CPU usage.

`System-JVM` usage is further divided into `GC`, `JIT` and `Others` sub-categories but only their percentage with
respect to `System-JVM` usage is recorded by the agent.

An example of the information recorded in `cpulogs` by the agent is:

```text
    Timestamp(s)      R-Mon(us)   R-Mon(%)        App(us)     App(%)     System(us)  System(%) [     GC(%)     JIT(%)  Others(%)]
           0.000              0          -              0          -              0          - [         -          -          -]
           1.044           9234      0.44%        1868481     89.50%         209967     10.06% [     0.00%    100.00%      0.00%]
           2.059           7700      0.38%        1816295     89.57%         203798     10.05% [     0.00%    100.00%      0.00%]
           3.063           4075      0.20%        1800945     89.75%         201671     10.05% [     0.00%    100.00%      0.00%]
           4.068           4103      0.20%        1799432     89.73%         201880     10.07% [     0.00%    100.00%      0.00%]
           5.072           3778      0.19%        1798003     89.62%         204477     10.19% [     0.00%    100.00%      0.00%]
           6.075           3813      0.19%        1920306     95.74%          81664      4.07% [     0.00%    100.00%      0.00%]
           7.172          17819      0.82%        2131800     97.98%          26127      1.20% [     0.00%    100.00%      0.00%]
           8.176           4223      0.21%        1952540     97.66%          42521      2.13% [     0.00%    100.00%      0.00%]
           9.180           3651      0.18%        1995804     99.58%           4707      0.23% [     0.00%    100.00%      0.00%]
          10.186           3537      0.18%        1985705     98.73%          21941      1.09% [     0.00%    100.00%      0.00%]

```

The output consists of following columns:

* `Timestamp`: records the timestamp of the sample since the start of the application.
* `R-Mon(us)`: records the CPU usage in last sample by `Resource-Monitor` category
* `R-Mon(%)`: records the CPU usage in last sample by `Resource-Monitor` category in terms of percentage
* `App(us)`: records the CPU usage in last sample by `Application` category
* `App(%)`: records the CPU usage in last sample by `Application` category in terms of percentage
* `System(us)`: records the CPU usage in last sample by `System-JVM` category
* `System(%)`: records the CPU usage in last sample by `System-JVM` category in terms of percentage
* `GC(%)`: records the CPU usage in last sample by `GC` sub-category in terms of percentage of `System-JVM` category
* `JIT(%)`: records the CPU usage in last sample by `JIT` sub-category in terms of percentage of `System-JVM` category
* `Others(%)`: records the CPU usage in last sample by `Others` sub-category in terms of percentage of `System-JVM` category

