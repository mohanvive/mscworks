# mscworks
Uni MSc Works

Key Word|Description
---------|---------
`&&`|This allows both conditions of `&&` to be matched by two events in any order.|
&#124; |The state succeeds if either condition of `&#124;` is satisfied. Here the event reference of the other condition is `null`.|
`! <condition1> && <condition2>`| When `!` is included with `&&`, it identifies the events that match <condition2> arriving before any event that match <condition1>.|
`! <condition> for <time period>`| When `!` is included with `for`, it allows you to identify a situation where no event that matches `<condition1>` arrives during the specified `<time period>`.  e.g.,`from ! temperatureStream where (temp > 60) for 5 second`.|


