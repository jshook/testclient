TestClient
==========

## What is TestClient?
TestClient is a programmable swiss army knife of performance testing. It makes it possible, even easy to test meaningful workloads against a variety of distributed systems. It is not a verification system. For now, TestClient focuses simply on running workloads for performance characterization.

At its core, TestClient is a sophisticated test harness. Why sophisticated? Because it handles many of the common testing "ilities" that other testing tools lack. Being sophisticated does not require being complicated. Instead, the more advanced features of TestClient are exposed as you need them, with simple tests being simple to run.

## Concepts

#### Activities

The simplest unit of configuration in TestClient is the __activity__. Activities simply represent a named sets of operations that can be run by a thread pool. It is possible to run one or more activities concurrently.

###### Baked-In Activities

There are a number of activities which are pre-defined. These may be used fro the command line with just a few required parameters. To see the baked-in activities, simply run java -jar testclient.jar --list-activities.

###### Activity Definitions

Activities are normally defined in YAML configuration files. Even the baked-in activities are defined in this way, although they are loaded from internal resources when needed The name of the file (or jar resource), such as "load-some-data.yaml" will be used to find the activity definition when it is referenced.

#### Scenarios

Scenarios are simply scripts that run activities. A scenario can be from a script file directly, or it can be defined directly in the arguments of a command line. In all cases, a runtime testing cycle is controlled by a scenario.


###### Step Commands

Unlike step conditions, a step command is simply a call to change the current state of the running test. Step commands may have have parameters.

Step commands may get more sophisticated in the future, but for now they are simply marshalled commands for a few limitied cases, as described below.

___to start an activity___

    start activity load-some-data;source=load-data-custom.yaml;threads=200;

___to modify a parameter of a running activity___

    set load-some-data;threads=500;

___to stop an activity___

    stop activity load-some-data
    
###### Step Conditions

The sequencing and advancement of steps in a test is controlled by step conditions. They are merely step commands which block further command evaluation until the condition is true.

    wait for testtime>300s

Wait for the global test scenario parameter __testtime__ to advance beyond 300 seconds. Some commands and conditions will automatically convert basic time units into the appropriate unit.

    wait for testtime>+300s

Wait for the global test scenario parameter __testtime__ to advance beyond 300 __more__ seconds from the moment this command is first encountered. This form allows for relative delays.

## Basic TestClient Usage

The simplest way to get started with a testclient is to run it with a command line like this:

    java -jar testclient.jar --activity=load-some-data;cycles=1..1000;threads=100

This is the simplest possible invocation of a test client. It represents a test scenario with a single command: __start activity load-some-data:1..1000:100__

###### activity calls
This also illustrates an equivalent activity calling syntax. Named activity parameters and a positional short form are both supported. All activities have a number of standard parameters and defaults:

__alias__:"" -- The name to be used for measuring or controlling this activity while the scenario runs
__source__:"&lt;alias&gt;.yaml" -- The source file containing the definition of this activity. If this is not provided, it is assumed to be &lt;alias&gt;.yaml
__cycles__:1..1 -- The cycle range for this activity. This controls generator logic, which is an advanced topic for later. "m" is short for "1..m" here.
__threads__:1 -- The number of threads over which the cycles will be spread.
__async__:1 -- The number of asynchronous messages which this activity will allow to remain in flight
__delay__:0 -- The number of milliseconds to delay between thread cycles. (for when you really want to make a test go slow(er))

Normally, these would be represented in an activity call as:

    name=activity-foo;source=activity-foo.yaml;cycles=1..1;threads=1;async=1;delay=0;

The parser wants a trailing semicolon here, but it will add one for you if that is the only thing missing.

.. and with additional scenario-specific parameters:

    name=activity-foo;source=activity-foo.yaml;cycles=1..1;threads=1;async=1;delay=0;aparam1=avalue1;aparam2=avalue2;

As long as the parameters are kept in the proper order as listed above, the parameter names will be inserted automatically. So a fully-qualified, but shortened form of the above would be: 

    activity-foo;1;1;1;0;aparam1=avalue1;aparam2=avalue2;

Also, since these are mostly default values, it could be simplified to:

    activity-foo;aparam1=avalue1;aparam2=avalue2;

This also shows that the numeric activity parameters may be skipped in order to take the defaults, with the activity-specific parameters following immediately.

## Scenario Recipes

Using these simple commands, you can create a variety of test plans, such as those shown below.

#### Grouping activities into phases

	// starting phase 1
    start activity loader1_1;load-some-data;1..1E6;datafile=testdata1.csv
    start activity loader1_2;load-some-data;1..1E6;datafile=testdata2.csv
	wait for activities.running==0
    // starting phase 2
    start activity loader2_1;load-some-data;1..1E6;datafile=testdata3.csv
    start activity loader2_2;load-some-data;1..1E6;datafile=testdata4.csv

#### Runing activities for specific durations

    start activity dostuff:1..1E9
    wait for scenario.duration>=15m
    stop activity dostuff
    start activity domorestuff:1E9
    wait for scenario.duration>=30m
    stop activity domorestuff

#### Changing an activity while it is running

    start activity alias=dostuff;cycles=1..1E9;threads=100;async=300;
    wait for scenario.duration >= 5m;
    set activities.dostuff.threads=50;
    wait for scenario.duration >= 10m;
    set activities.dostuff.threads=10;
    ramp activities.dostuff.threads to 100 over 5m

If you want to do something more advanced than what is shown here, consult the [Scenario Scripting Guide](scripting.md)


## Sandbox Parameters

## Expected vs Achieved... what is monitored?


