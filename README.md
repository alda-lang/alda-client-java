# alda-client-java

A Java command-line client for [Alda](https://github.com/alda-lang/alda), a music programming language for musicians.

## Development

### Overview

The Alda client is a fairly straightforward Java CLI app that uses [JCommander](http://jcommander.org) to parse command-line arguments.

Interaction with servers is done via [ZeroMQ](http://zeromq.org) TCP requests with a JSON payload. The Alda client takes command-line arguments, translates them into a JSON request, and sends the request to the server. For more details about the way we use ZeroMQ, see [ZeroMQ Architecture](https://github.com/alda-lang/alda/blob/master/doc/zeromq-architecture.md).

Unless specified via the `-H/--host` option, the Alda client assumes the server is running locally and sends requests to localhost. The default port is 27713.

Running `alda start` forks a new Alda process in the background, passing it the (hidden) `server` command to start the [server](https://github.com/alda-lang/alda-server-clj). Server output is hidden from the user, although the client will report if there is an error.

To see server output (including error stacktraces) for development purposes, you can start a server in the foreground by running `alda server`. You may specify a port via the `-p/--port` option (e.g. `alda -p 2000 server`) -- just make sure you're sending requests from the client to the right port (e.g. `alda -p 2000 play -f my-score.alda`).

To stop a running server, run `alda stop`.

### `boot dev` task

To run the Alda client locally to test changes you've made to the code, you can run:

    boot dev -x "args here"

> This requires that you have the [Boot](http://boot-clj.com) build tool installed.

The `-x` argument must be a single string containing everything you would put after `alda` when using a release build of the Alda client.

For example, to test changes to the way the `alda play` command plays a file, you can run:

    boot dev -x "play --file /path/to/file.alda"

One caveat to running the client this way is that the `boot` process does not have the necessary permissions to start a new process. This means you won't be able to start an Alda server via the client by running `boot dev -x "up"` or `boot dev -x "server"`, for example.

You can, however, start a server via a release build (e.g. `alda -v -p 12345 server`) and use the `boot dev` task to talk to that server on that port.

#### Example

Run a server in verbose (debug) mode in one terminal:

```
$ alda -v -p 12345 server
16-Nov-17 09:08:44 skeggox.local INFO [alda.server] - Binding frontend socket on port 12345...
16-Nov-17 09:08:44 skeggox.local INFO [alda.server] - Binding backend socket on port 61004...
16-Nov-17 09:08:44 skeggox.local INFO [alda.server] - Spawning 2 workers...
16-Nov-17 09:09:37 skeggox.local DEBUG [alda.server] - Receiving message from frontend...
16-Nov-17 09:09:37 skeggox.local DEBUG [alda.server] - Forwarding message to worker 008B96F216...
16-Nov-17 09:09:37 skeggox.local DEBUG [alda.server] - Forwarding backend response to frontend...
16-Nov-17 09:09:37 skeggox.local DEBUG [alda.server] - Forwarding message to worker 008B96F216...
16-Nov-17 09:09:37 skeggox.local DEBUG [alda.server] - Forwarding backend response to frontend...
16-Nov-17 09:09:37 skeggox.local DEBUG [alda.server] - Forwarding message to worker 008B96F216...
16-Nov-17 09:09:37 skeggox.local DEBUG [alda.server] - Forwarding backend response to frontend...
16-Nov-17 09:09:44 skeggox.local DEBUG [alda.server] - Supervisor approves of the current number of workers.
16-Nov-17 09:09:47 skeggox.local INFO [alda.server] - Murdering workers...
16-Nov-17 09:09:47 skeggox.local INFO [alda.server] - Destroying zmq context...
16-Nov-17 09:09:47 skeggox.local INFO [alda.server] - Exiting.
```

Test client changes in another terminal:

```
$ boot dev -x "-p 12345 status"
Compiling 12 Java source files...
[12345] Server up (2/2 workers available, backend port: 61004)

$ boot dev -x "-p 12345 play -c 'piano: c d e'"
Compiling 12 Java source files...
[12345] Parsing/evaluating...
[12345] Playing...

$ boot dev -x "-p 12345 down"
Compiling 12 Java source files...
[12345] Stopping Alda server...
[12345] Server down ✓
```

## License

Copyright © 2012-2018 Dave Yarwood et al

Distributed under the Eclipse Public License version 1.0.

