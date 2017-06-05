# CHANGELOG

## 0.3.0 (2017-06-05)

This release adds a bunch of features and improvements to the new, faster Alda
REPL. Thanks to [jgkamat] for helping me implement all this stuff!

* Added 6 new REPL commands, each equivalent to its command-line counterpart:

  * `:status` - display the status of the server
  * `:list` - list running Alda processes
  * `:version` - display Alda client + server versions
  * `:down` - stop the server
  * `:up` - start the server
  * `:downup` - restart the server

* When starting the Alda REPL, we now check to see if there is an Alda server
  running. If there is not, then we offer to start one for you.

  We also offer to start the server *after* the REPL has started, in the event
  that the server fails to respond, e.g. if it gets shut down from outside of
  the REPL.

* Added a confirmation prompt when running the `:new`, `:load` or `:quit`
  commands in the REPL. This is to avoid accidentally losing unsaved changes to
  your score.

* Fixed miscellaneous bugs in the REPL related to sending requests to the server
  and receiving responses. This behavior is more reliable now.

* Fixed a bug where if something goes wrong when trying to start a server, the
  program does not exit, but instead waits forever for workers to start. Now,
  the program prints the error and exits.

## 0.2.0 (2017-05-30)

* Removed the deprecated `--lisp` and `--map` options to the `parse` command.
  There is now only one mode of output, and it corresponds to what `--map` used
  to be.

## 0.1.5 (2017-05-28)

* Fixed bugs re: the current filename of a score when using the `:new`, `:load`
  and `:save` commands in the REPL.

  Thanks to [jgkamat] for all of your work on our shiny new REPL! :fireworks:

## 0.1.4 (2017-05-27)

* Minor bugfixes.

## 0.1.3 (2017-05-27)

* Added generous retry logic when submitting requests to the server via the
  REPL. See [#12](https://github.com/alda-lang/alda-client-java/issues/12) for
  context.

## 0.1.2 (2017-05-19)

* Added a `--history` option to the `play` command. This can be used to provide context when playing new code. For example:

```
$ alda play --history "trumpet: (tempo 200) c8 d e" --code "f g a b > c"
```

The command above will result in the notes `f g a b > c` being played as eighth notes, on a trumpet, at 200 bpm.

This option is mainly useful for editor plugins and the upcoming client-side rewrite of the Alda REPL.

* Work in progress client-side rewrite of the Alda REPL.

## 0.1.1 (2017-01-14)

* Fixed 2 bugs re: `alda list` output:
  * It didn't work correctly on non-OS X systems like Ubuntu due to differences in the `ps` command across Unix distributions.
  * Fixed buggy output when running multiple Alda servers ([#4](https://github.com/alda-lang/alda-client-java/issues/4)).

Major thanks to [tobiasriedling] for both fixes!

## 0.1.0 (2016-11-19)

* * Extracted alda-client-java from the [main Alda repo](https://github.com/alda-lang/alda) as of version 1.0.0-rc50.

[tobiasriedling]: https://github.com/tobiasriedling
[jgkamat]: https://github.com/jgkamat
