Scripting Scenarios with TestClient
===================================

Every scenario that is run with testclient is controlled by a script. A scenario controller reads the script, interpreting each command in sequence.

#### Variables
The variables that are used in scenario commands can come from various places:
- global state
  - wait_trigger_interval
  - scenario_name
  - scenario_start_time
  - scenario_time_elapsed
- activity state -- each activity's variables can be accessed as &lt;activity_name&gt;.&lt;varname&gt;, or with only &lt;varname&gt;, in which case the most recently started activity will be referenced.
  - activity_name
  - activity_start_time
  - activity_time_elapsed
  - metrics
  - activity controls

#### Eventing
Rather than polling an expression for a given output, such as "...." it is better to ask the scenario runtime to invoke a .... when the appropriate event ...


#### Language
The scenario scripting language is simple and focused. It is intentional that this language is limited to simple step-wise scenarios. However, it is really only a sanitized layer of commands. Internally, the scenario is controlled by an actual javascript interpreter. The commands are translated directly into 

If you want to see what the javascript equivalent of a given 