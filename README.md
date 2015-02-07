## a Cassandra/CQL test client

This was thrown together as a way to quickly get some specific tests running. It is a bit rough around the edges.

Even though the code is not perfect or even pretty in places, It was evident that it might be useful to some. Many users need a starting point for testing application-specific data models with some degree of performance. It is not  a comprehensive testing tool, nor even a feature rich one like the new Cassandra stress tool. It's just a client that can allow users to test application-specific CQL workloads at speed. Sometimes, that's exactly what you need. So it is in that spirit that I share this thing that was supposed to be a quick-n-dirty testing rig.

I will do light maintanaince on it and consider any requests that are submitted. However, this project is not my main focus. Some day, I'd like to release a proper scale testing tool, but this is not that tool. It's just a sapling. Please submit requests via the project page if you need bugs fixed or even small enhancements.

### Requirements

To just use the client as it is, simply download the jar and run it with JVM version 7 or newer.
This test client is released as an executable jar. See the project releases to download the jar directly.

If you intend to use this source code as a starting point for running a dedicated test, then you might need to install maven first.

### Command Line

To see the options supported, simply

    java -jar cqltestclient.jar -help

The basics have been included here as well:

      --activity=<class name>:<cycles>:<threads>:<asyncs>
      --createschema
      --keyspace=<keyspace>                             (default: testks)
      --table=<table>                                   (default: testtable)
    ( If there are several tables in an activity, this will be used as a prefix)
      --splitcycles                                     (default: false)
    [ --host <contact point for cluster> ]              (default: localhost)
    [ --port <CQL native port> ]                        (default: 9042)
    [ --graphite <host> | --graphite <host>:<port> ]
    [ --prefix <telemetry naming prefix> ]

### Example Command Lines

__create the schema for activity WriteTelemetryAsync__

    java -jar cqltestclient.jar --host=10.10.10.10 --activity=WriteTelemetryAsync --createschema

In the example above, cqltestclient looks for a class named WriteTelemetryAsync in the com.metawiring.load.activities package. It then calls the createSchema() method on it and exits.

__insert 1000000 records using 100 threads and 1000 pending async__

    java -jar cqltestclient.jar --host=10.10.10.10 --activity=write-telemetry:1000000:100:1000

Notice that this example is slightly different. The --activity argument is not a proper class name. In this case, cqltestclient will look for a yaml file in the classpath under activities/write-telemetry.yaml. It will then initialize and run a YamlConfigurableActivity with it.

__read the same records back, with 100 threads and 200 pending async__

    java -jar cqltestclient.jar --host=10.10.10.10 --activity=ReadTelemetryAsync:1000000:100:200

__do both at the same time__

    java -jar cqltestclient.jar --host=10.10.10.10 --activity=WriteTelemetryAsync:1000000:100:1000 --activity=ReadTelemetryAsync:1000000:100:1000

### Activities

The contextual workloads are defined as _Activities_, which is just an interface that an ActivityHarness uses to run the activity. You can specify how
the iterations are divided up between the threads. By default, the specific cycles numbers will not be assigned distinctly to the threads, although the
cycle counts will. If you want the cycles to be divided up by range, then use the --splitcycles option. This applies to all activities on the command line for now.
be seen by several threads.

You have the option of using one of the direct Activity types or a yaml configured activity. The preferred way is to use YAML to configure and run your activities, since the internal boilerplate logic is pretty standard. The previous activity implementations were left as examples for those who might want to tinker with or build their own activity implementations.

The remainder of the documentation describes all of the current activities in general. If you implement your own activity, then it may not apply, depending on how you choose to build it.

#### Cycle Semantics

Each activity is run by an executor service under the control of an ActivityHarness for each thread.
Each time an activity harness iterates an activity, it expects the activity to have completed one cycle of work. This should be considered the sementic contract for an Activity. It allows results to be interpreted across activities more trivially.

#### Fault Handling

Some details about how async activities work:
- Before any inner loop is called, the activity is initialized, including prepared statements and data generator bindings.
- The inner loop always tries to fill the async pipeline up to the configured allowance.
- After async the pipeline is primed, the second phase of the inner loop does the following:
 - tries to get the async result
 - if this fails before the 10th try, then the op is resubmitted and the thread sleeps for 0.1 * tries seconds
 - if the op failed and at least 10 tries have been used, then the op is not retried

##### activity: WriteTelemetryAsync AKA write-telemetry

The schema for this activity is

    CREATE TABLE testtable (
      source int,
      epoch_hour text,
      param text,
      ts timestamp,
      cycle bigint,
      data text,
      PRIMARY KEY ((source, epoch_hour), param, ts)
    ) WITH CLUSTERING ORDER BY (param ASC, ts DESC)

CQL for this activity:

    insert into KEYSPACE.TABLE (source,epoch_hour,param,ts,data,cycle) values (?,?,?,?,?,?)

where the fields are, respectively:

- __source__ - The thread id (This simulates a topological or taxonomical source id)
- __epoch_hour__ - A time bucket, in the form _"2005-10-29-22"_, walking forward from the epoch start, advancing 1 second each time it is used for each thread.
- __param__ - A randomly selected parameter name, from a list of 100 selected at random from the 'net
- __ts__ - A timestamp, generated forward in time, which coincides with the epoch_hour bucket, advancing at the same rate.
- __cycle__ - The cycle number of the activity thread that created this row
- __data__ - a random extract of the full lorem ipsum text, randome size between 100-200 characters

As this activity runs, it creates data that moves forward in time, starting at the beginning of the epoch. This is suitable to DTCS testing and general time-series or temporally-ordered testing. If you want to control the number of rows written, overall, then the cycle count in the activity option does this. If you want to control the specific times that are used, then the cycle range in (min..max] format can do this. However, the math is thrown off if you change the number of threads, since the cycles are distributed among all threads, while the starting cycle set on all of them.

##### activity: ReadTelemetryAsync AKA read-telemetry

This activity uses the same schema as WriteTelemetryAsync. 

CQL for this activity:

    select * from KEYSPACE.TABLE where source=? and epoch_hour=? and param=? limit 10

where the fields are, respectively:

- __source__ - same semantics and behavior as above
- __epoch_hour__ - same semantics and behavior as above
- __param__ - same semantics and behavior as above

This means that reads will be mostly constrained by partition, which is good. However, the logic doesn't automatically walk backwards in time to previous epoch_hour buckets to get a full 10 items. This compromise was made until the testing tool internals can be refined to support such cases without overcomplicating things. (A specific plan is in the works.)

#### Generators and Thread affinity

Internally, the data that is used in the operations is produced by type-parameterized generators. This means that if you want a second-resolution DateTime object, then you have to have a generator of type Generator&lt;DateTime&gt; with the implementation and instance details to handle the second resolution.

The generator library handles these details as well as when generator instances are shared between activity threads. There is a special type of generator, ThreadNumGenerator, which uses markers on the thread to extract thread enumeration. This is used by the two initial activities above as a way to make each thread align to a partition. This isn't required, but for the type of testing that this tool was built for, it effectively guaranteed isochronous data rates evenly across the partitions. The point of calling this out here is to acknowledge that your testing might not need this, and would benefit from wider data dispersion at the partition level. There is nothing preventing such use-- It merely isn't the default for these activities.

#### Extending cqltestclient

If you need to build a test client for a particular workload, you might need to add to the generator library. The generators can be browsed in the source tree.

##### Generator Conventions

If you are going to add generators, follow these guidlines:

Generators constructors which take parameters should provide a constructor which uses all String arguments at the very least. Generators which are threadsafe should implement ThreadSafeGenerator, and those which can be advanced to a particular point in the cycle count should also implement FastForwardableGenerator.

ThreadSafeGenerator is simply a tagging interface which allows the generator resolver to avoid sharing generators which are not thread-safe.

FastForwardableGenerator is an interface that allows an activity to advance the starting point for a generator so that you can control the range of cycles used in your test.

#### YAML Activity Configuration

Here is an example activity as configured in YAML:

    ddl:
    - name: create-keyspace
      cql: |
        create keyspace if not exists <<KEYSPACE>> WITH replication =
        {'class': 'SimpleStrategy', 'replication_factor': <<RF>>};
    - name: create-telemetry-table
      cql: |
        create table if not exists <<KEYSPACE>>.<<TABLE>>_telemetry (
        source int,      // data source id
        epoch_hour text, // time bucketing
        param text,      // variable name for a type of measurement
        ts timestamp,    // timestamp of measurement
        cycle bigint,    // cycle, for diagnostics
        data text,       // measurement data
        PRIMARY KEY ((source, epoch_hour), param, ts)
        ) WITH CLUSTERING ORDER BY (param ASC, ts DESC)
    dml:
     - name: write-telemetry
       cql: |
         insert into <<KEYSPACE>>.<<TABLE>> (source, epoch_hour, param, ts, data, cycle)
         values (<<source>>,<<epoch_hour>>,<<param>>,<<ts>>,<<data>>,<<cycle>>);
       bindings:
         source: threadnum
         epoch_hour: date-epoch-hour
         param: varnames
         ts: datesecond
         data: loremipsum:100:200
         cycle: cycle

The __ddl__ section contains the statements needed to configure your keyspace and tables. They will get called in order when you are executing cqltestclient with --createSchema.
The __dml__ section contains the statements that you want to run for each cycle. Just as with the DDL statements, you can have as many as you like. The activity will use one of these for each cycle, rotating through them in order.

The format of the __cql__ section shows how to use multi-line statements in YAML while preserving newlines. As long as you preserve newlines, you're free to use // comments to explain the parameters. If you do not maintain the newlines, then you will have syntax errors because of invalid comments.

The __bindings__ sections are how named place-holders are mapped to data generator functions. Currently, these names are not cross-checked between the cql and binding names.

The &lt;&lt;word&gt;&gt; convention is used for parameter substitution. KEYSPACE, TABLE, and RF are all substituted automatically from the command line options. The _create table_ clause above shows a convention that uses both the configured TABLE name as well as a _tablename value. This is a useful way to have a common configurable prefix when you are using multiple tables.

Both the __ddl__ and __dml__ sections contain exactly the same thing strcuturally. In fact, it's exactly the same configuration type internally. Both contain a list of named statements with their cql template and a set of associated bindings. You don't see any bindings under ddl because they are meaningless there for this example activity.


## Metrics

The instrumentation uses the dropwizard metrics library, formerly the yammer metrics library, formerly the coda hale metrics library.. It might be named something different next year, but it's still quite useful.

By default, the metrics will be logged to console via the console log and logging metrics reporter. At the end of a run, the long form of the metrics summaries are dumped to console. The reporting interval for this method is every minute. Once you start the client, you'll see a periodic report to the screen showing the current testing time as a heartbeat that the test is running.

### Configuration

The only metrics configuration exposed at this time is the --graphite CLI option. You can pass &lt;host&gt; or &lt;host&gt;:&lt;port&gt; in order to capture and analyze your metrics on a compatible server.

### Interpretation & Examples

    22:27:20.544 [metrics-logger-reporter-thread-1] INFO  c.m.load.core.MetricReporters - type=COUNTER, name=ReadTelemetryAsyncActivity.async-pending, count=100

This line shows the basic format of a log line. The important bits here start with __type=COUNTER__. The remainder of this section will consist of only that part and everything after it. As well, the lines will be wrapped and indented to provide easier reading, and numbers shortened.

There will be a basic explanation of each type, followed by an explanation about the specific metric names.

__counters__

    type=COUNTER, name=ReadTelemetryAsyncActivity.async-pending, count=100

This is a basic counter. It simply tells you the number that the app was reporting at the time the reporter triggered. Counters are not montonically increasing. They can go up and down. In this example, the count shows you how many _async-pending_s there were, or the number of async operations in flight for the ReadTelemetryAsync activity.

__histograms__

	type=HISTOGRAM, name=ReadTelemetryAsyncActivity.tries-histogram,
     count=184148, min=1, max=1, mean=1.0, stddev=0.0, median=1.0,
     p75=1.0, p95=1.0, p98=1.0, p99=1.0, p999=1.0

	type=HISTOGRAM, name=ReadTelemetryAsyncActivity.ops-histogram,
     count=184148, min=2327168, max=176278812,
     mean=3.42E7,stddev=2.2264E7, median=2.938E7,
     p75=4.41E7, p95=7.8723E7, p98=9.61628E7, p99=1.12606E8, p999=1.703E8

This is a histogram of all the values submitted to it. It also contains basic stats of the values submitted, but no timing data apart from the value semantics of the samples themselves. In this case, the values are indicating the distribution of tries to complete the ReadTelemetryAsyncActivity. p999 is 1.0, so there is less than a 1/10000 chance that there was even a single retry.


__meters__

    type=METER, name=WriteTelemetryAsyncActivity.exceptions.PlaceHolderException,
    count=0, mean_rate=0.0, m1=0.0, m5=0.0, m15=0.0,
    rate_unit=events/second

A meter is merely a way to capture the rate of a type of an event. This one is the infamous PlaceHolderException, which is how I make sure that I'm reporting exactly 0 of something important to ensure that the downstream monitoring system can see it (and that the admin is putting something on the dashboard.) It captures not only the count, but also the mean_rate since start (which can skew away from recent trends) and the 1, 5 and 15 minute moving average.

__timers__

    type=TIMER, name=ReadTelemetryAsyncActivity.ops-total,
     count=184190, min=2.7, max=143.56, mean=34.48, stddev=22.24, median=29.52,
     p75=44.68, p95=80.63, p98=97.15, p99=108.48, p999=143.46,
     mean_rate=3081.74, m1=3489.71, m5=4171.31, m15=4312.,
     rate_unit=events/second, duration_unit=milliseconds

Timers are a combo of histograms and meters and counters. They include all the information we tend to want when when profiling something. In this case, millisecond measurements of the total time an operation took. Also, the moving average rates are included.The p999 duration is 143ms, not bad for my desktop system, and the median value (aka p50) is 34 microseconds. The average op rate for the duration of this test was 3081.

The remainder of this section describes the chosen metrics in more detail.

###### Cycle Position (counters)

- ReadTelemetryAsyncActivity.cycles
- WriteTelemetryAsyncActivity.cycles

The cycle position of the named activity. This is a trace of the progress between the start cycle and the end cycle, as specified on the command line.

###### Op rates & client latencies (timers)

- ReadTelemetryAsyncActivity.ops-total
- WriteTelemetryAsyncActivity.ops-total

The timing of an operation, from the time it was submitted asynchronously to the time it completed, including all retries, or the time it took to fail after exceeding retries.

###### Pending Async Ops (counters)

- WriteTelemetryAsyncActivity.async-pending
- WriteTelemetryAsyncActivity.async-pending

The number of asyncronous operations pending for the named activity.

###### Async Wait (timers)
- ReadTelemetryAsyncActivity.ops-wait
- WriteTelemetryAsyncActivity.ops-wait

The wait time between starting the getUninterruptibly() call and when it returns.

###### Tries, Retries (histograms)

- ReadTelemetryAsyncActivity.tries-histogram
- WriteTelemetryAsyncActivity.tries-histogram

###### Exception Rates (meters)

- WriteTelemetryAsyncActivity.exceptions.*
- ReadTelemetryAsyncActivity.exceptions.*

The rate of exceptions of the given name. There could be various names of this metric, so it's safer to monitor for everything under this name and then break them out individuall if you need.

###### Activity Count (counters)

- ActivityExecutorService.activities

The number of configured activities reported at the start of this test run.

# LICENSE

This is licensed under the Apache Public License. See the included LICENSE.txt for details.
