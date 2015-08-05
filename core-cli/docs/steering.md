# CQLTestClient Activities, In-Depth

## Source Path
Each defined activity has a source path. For YAML-based activities, this is simply the file name, with he '.yaml' part removed. So, an activity defined in 'activities/write-telemetry.yaml' would take the source name 'write-telemetry'.

## Instance Name
When a defined activity is referenced in a cqltestclient command list, it may be given a unique instance name. This is because you may want to have multiple instances from the same source path running together a test. Giving them each a unique instance name allows you to observe and interact with them independently.

## Statements

Activities can have both DDL and DML statements. They are defined in separate sections of the YAML file. You might start a YAML file with:

    ddl:
    - name: create-keyspace
      cql: |
        create keyspace if not exists <<KEYSPACE>> WITH replication =
        {'class': 'SimpleStrategy', 'replication_factor': <<RF>>};

This is YAML for a named __ddl__ section, and a list of sub-items. Each of the sub-items in this case is a named cql statement. The __name__ will be used for any metrics or controls for this statement during the activity's execution. The __cql__ clause is simply the cql syntax that you would normally execute against the database. The "__cql: |__" form is used here to allow for a multi-line value, so that the statement can easier to read and edit.

You might continue the YAML with:

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

This shows that the DDL section can contain multiple statements. When _--createschema_ is used on the command line, each one in turn will be executed as long as there are no errors.

The YAML format for DML statments is the same, with the section name being the only difference. CQLTestClient makes no assumptions about what type of statements you provide in these sections. Any valid CQL syntax will work.

You might continue the YAML activity definition with:

    dml:
     - name: write-telemetry
       cql: |
         insert into <<KEYSPACE>>.<<TABLE>>_telemetry
          (source, epoch_hour, param, ts, data, cycle)
         values (<<source>>,<<epoch_hour>>,<<param>>,<<ts>>,<<data>>,<<cycle>>);

The YAML format for DML is the same as for DDL. The only difference here is that it makes sense for DML to have parameter bindings to data generators.

## Statement Parameters

Above, the example cql contains some parameters inside angle brakets, like __&lt;&lt;KEYSPACE&gt;&gt;__. This is how you define references to named values in a statement. Before these statement are executed, the named arguments are substituted in place of the parameters. However, some of the arguments come from the command line, and others are generated per operation, as explained below.

### CLI Statement Arguments

The follow parameters are avaialable in DDL statements, with the argument values taken from the respective command line argumets.
* __&lt;&lt;KEYSPACE&gt;&gt;__ - from_--keyspace_ on the command line, or "testks" by default
* __&lt;&lt;RF&gt;&gt;__ - from _--rf_ on the command line, or "1" by default
* __&lt;&lt;TABLE&gt;&gt;__ - from _--table_ on the command line, or "testtable" by default

### Generator Statement Arguments

For the named parameter tokens in the write-telemetry example above, the data is generated specially for each operation. This is achieved with the YAML format for generator bindings:

       bindings:
         source: ThreadNumGenerator
         epoch_hour: DateSequenceFieldGenerator:1000:YYYY-MM-dd-HH
         param: LineExtractGenerator:data/variable_words.txt
         ts: DateSequenceGenerator:1000
         data: LoremExtractGenerator:100:200
         cycle: CycleNumberGenerator

This shows named bindings which match up to the statement parameters. For each statement parameter, the associated binding describes how to generate values for each operation. The format for these generator definitions is the class name, followed by optional generator arguments, separated by a single ':'.

# Activity Instances

Taken together, the syntax examples above constitute a whole activity definition. This is actually excerpted from one of the built-in activities which you can use already with any cqltestclient.jar. 

This merely defines a kind of activity that can be run. You can run multiple activities of the same kind together in the same test. Each instance of an activity will have its own name, thread pool, concurrency, and controls.

## Configuring Activity Instances

Note: Within this section, 'Activity' will be used to refer to activities instances at runtime.

TODO: replace "activity instance" with "named activity"

An instance of an activity has the following runtime parameters:

- name -- the logical name used to track and control this instance while it is running
- source -- the source file or address which provides the DDL, DML, and generator bindings for the activity
- cycles -- the range of long values which will be the inputs to the generator functions
- threads -- the number of threads to be allocated for this activity
- async -- the number of async operations which will be kept in-flight at any moment
- delay -- the number of milliseconds to block in between each cycle on each thread
- cyclesplit -- how to divide the cycle range over the threads

It is essential to understand how these parameters work together. Of course, _name_ and _source_ are pretty self-explanatory. 

Things get more interesting with cycles and threads. The cycles represent the possible values that can be generated, according to the generator functions configured. The cycles values are the actual long values which are fed into the generators for each operation to yield the values which are used in the related statements. Rather than per thread, cycles are per-activity. This means that changing the number of threads for an activity doesn't actually change the number of operations that the activity will try to complete. What it does instead is adjust the way that the cycles are distributed to the threads. The cycle range is simply split across the available threads.

TODO: Enhance and standardize the cycles/splits logic and update docs here.

There are short configuration forms for all of the above activity instance parameters: __name=_name_,source=_sourcefile_,cycles=[_start_..]_end_[/_split_],async=_async_,delay=_delay_ __

Only the source is required. If name is not specific, the name will be auto-generated by adding numeric suffixes to the source.

# Advanced Topics

## Phased Tests

## Stop Conditions

## Activity Controls

## Generator Scope

Generators are instantiated and retained for the lifetime of the threads that need them in the generator bindings. If any of them start with the keywords __thread__, __activity__, or __phase__, then the generator instance will be shared within that scope. That means that you can get a monotonically increasing value, for example, across active threads in an activity or phase when needed, or other similar behaviors.

TODO: Implement generator thread-save annotations and safety logic in scoped generators, and update docs here.

## Developing new Activity Implementations

The introductory docs only refer to YAML-configured activities, as this is the way that most users are expected to use cqltestclient. It is possible to create custom activities which do not use the CQLYamlActivity, and which are not even CQL.

If you are interested in developing your own core Activity types, take a look at the following classes to get started:

- ActivityDispenserLocators - entry point for finding definitions
- ActivityDispenserLocator - type-specific activity source locator
- ActivityDispenser - type-specific Activity factory
- Activity - core per-thread logic
- ActivityContext - shared state and controls for an activity instance
- ActivityContextAware - enables optional usage of ActivityContext

Eventually, each activity implementation is expected to be fully modular.

## Auto Rate Optimization

## Auto Latency Optimization

## Auto concurrency Sampling

## PID Rate Targeting

This is a method of using active feedback to create an input control signal which is neither too reactive nor too passive. Details can be found at [the PID Controller wikipedia page](https://en.wikipedia.org/wiki/PID_controller).

It is used in cqltestclient to adjust things such as threads, async, and inter-op delay to reliably run at a target op rate.

This does not handle the fact that effective throughput at different types of saturating load is very non-linear.

TODO: use the term 'control input' everywhere appropriate

This does not overcome the 
TODO: Implement this and upate docs
