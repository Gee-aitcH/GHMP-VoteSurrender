## Features

Grant Players the Ability to Vote for Surrender.

Usage:

`ghvs mode [on/off]` to Turn On/Off the Plugin. (Admin Level)

`ghvs vr [float]` to Change the Vote Requirement. (Host Level)

`ghvs [y/n]` to (Host & )Vote.

`ghvs help` to get the help message.

Note 1: 2 x 'y' = -1 x 'n'. 2 Votes on Yes are equivalent to Only 1 Vote on No.

Default Vote Requirement is `50%`.

Note 2: Without the key argument(`[]`), server will return the current status of that specific subject.

### Setup

Clone this repository first.
To edit the plugin display name and other data, take a look at `src/main.resources/plugin.json`.
Edit the name of the project itself by going into `settings.gradle`.

### Basic Usage

See `src/main/java/ghvotesurrender/ExamplePlugin.java` for some basic commands and event handlers.
Every main plugin class must extend `Plugin`. Make sure that `plugin.json` points to the correct main plugin class.

Please note that the plugin system is in **early alpha**, and is subject to major changes.

### Building a Jar

`gradlew jar` / `./gradlew jar`

Output jar should be in `build/libs`.


### Installing

Simply place the output jar from the step above in your server's `config/plugins` directory and restart the server.
List your currently installed plugins by running the `plugins` command.
