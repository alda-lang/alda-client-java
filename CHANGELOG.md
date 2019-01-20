# CHANGELOG

## 0.6.1 (2019-01-19)

* New CLI command: `alda instruments` and REPL command: `:instruments`

  This new command lists all available instruments, which is currently the 128
  instruments in the General MIDI spec, as well as `midi-percussion`.

  NB: Alda does have a number of aliases for these instruments, e.g. `piano` is
  recognized as `midi-acoustic-grand-piano`. These aliases are not included in
  the list.

* Made adjustments to the command that prints for Windows users when running
  `alda update` to update the Alda client.

  For details, see issue
  [#35](https://github.com/alda-lang/alda-client-java/issues/35).

## 0.6.0 (2018-11-24)

* Fixed the mechanism for determining whether the Alda client version is the
  same as the latest release (i.e. whether or not it's up to date).

  What we had up until now happened to work for the 1.0.0-rcXX series of
  releases, but there is an edge case involving upgrading from 1.0.0-rcXX to
  1.0.0. We were doing a substring search, whereas we needed to do an exact
  match.

## 0.5.3 (2018-08-25)

* Handled an edge case re: what to display when playing extremely short scores.

* Improved the help text descriptions of the `:info` and `:save` REPL commands.

## 0.5.2 (2018-08-25)

* Implemented a `--no-color` CLI option, which suppresses ANSI color codes.

## 0.5.1 (2018-07-01)

* Upgraded JLine dependency to 2.14.6 to fix the platform-specific "Failed to
  construct terminal" part of [this
  issue](https://github.com/alda-lang/alda-sound-engine-clj/issues/13).

## 0.5.0 (2018-03-08)

* Added an `:info` command to the Alda REPL. It prints some user-readable
  information about the current score, for example:

  ```
  p> :info
  Instruments: piano-sFz6g
  Current instruments: piano-sFz6g
  Events: 6
  Markers: start, one, two, three
  ```

  Thanks, [TBuc], for implementing this new feature!

## 0.4.8 (2018-02-03)

* Fixed a bug in the Alda REPL where the `:play from` and `to` options were
  being completely ignored.

* Fixed issues where `!` is not escaped properly in the Alda REPL.

  This is something that had been fixed previously in the Clojure version of the
  Alda REPL, but not ported over when we rewrote the REPL as part of the Java
  client.

  For context, see [this issue](https://github.com/alda-lang/alda/issues/125).

* Enabled persistent history for the Alda REPL. For example, if you start a REPL
  session and enter a bunch of lines of input, then close the session and start
  a new one, you can access the lines you typed in the previous session by
  pressing the Up arrow.

  History is stored in a file at `~/.alda-repl-history`.

## 0.4.7 (2017-10-12)

* Fixed a weird issue where, after successfully updating Alda via `alda update`,
  the line `ExitCode.SUCCESS.exit()` would result in a `NoClassDefFoundError`.

  This is black magic to me and I don't understand why it was happening (maybe a
  Java guru out there can enlighten me), but in any case, I noticed that
  `ExitCode.SUCCESS.exit()` is called after the `updateAlda` method returns
  anyway, so we can just replace the first `ExitCode.SUCCESS.exit()` with a
  `return;` (letting the second `ExitCode.SUCCESS.exit()` take care of exiting)
  and that ought to fix it.

## 0.4.6 (2017-10-12)

* Improved the timing of waiting for the server to stop before starting a new
  one when running the `alda downup` command.

  Before, this was just guesswork, and often times, the server wouldn't be down
  yet, so when a new server tried to start, it would fail with the message:

  ```
  There is already a server trying to start on this port. Please be patient -- this can take a while.
  ```

  Now, we're actually checking to see when the server stops responding, and
  waiting until that's the case before we try to start a new server.

  Unfortunately, there is still a bit of guesswork here because the message
  above is triggered by (assuming you're running OS X or Linux) a check to see
  if there is an Alda server process in your OS that was started on the same
  port you are trying to use. There is still a brief window of time between when
  the server stops responding to requests and when the process has terminated.

  As such, I think there is room for improvement in the future, and you might
  still see the message above from time to time. But, with this release, things
  should at least be better than they were before.

* Expanded the scope of the `-t` / `--timeout` option to include how long (in
  seconds) the Alda command-line client should wait, after running `alda down`
  or `alda downup`, for confirmation that the server has gone down. The default
  value is still 30 seconds, which should be more than enough time on most
  systems.

## 0.4.5 (2017-10-07)

* Added an `--output` (`-o`) option to the `alda parse` command that allows you
  to specify what should be output. Valid values are:

  * `data` (default) is the map of score data that includes instruments, events,
    etc.

  * `events` is the sequence of events parsed from the score.

## 0.4.4 (2017-07-28)

* Added a `--history-file` (`-I`) option. It's like `--history`, except that it
  takes the name of a file containing Alda code to be used as history.

## 0.4.3 (2017-07-16)

* Bugfix: reversed logic in the `alda update` command so that the Windows update
  logic happens if you HAVE Windows, not if you don't have it. Whoops.

## 0.4.2 (2017-07-16)

* Fixed a bug where the `--history` option to the `alda play` command was being
  ignored if the `--file` option was used or code was being piped into STDIN.

## 0.4.1 (2017-07-07)

* Running `alda update` has been known not to work on Windows because of
  limitations of the OS: Windows apparently will not let you download a new
  version of a program to replace the program while it is running.

  As a workaround, if your OS is Windows and you run `alda update`, we now print
  detailed instructions with a command to run in your terminal that will update
  alda.exe.

## 0.4.0 (2017-07-01)

* Prior to this release, the client would almost always exit with an exit code
  of 0, even if there was an error of some kind.

  Starting with this release, Alda has a handful of meaningful [exit
  codes](https://github.com/alda-lang/alda-client-java/blob/master/src/alda/error/ExitCode.java).
  Crucially, there is now a distinction between 0 (success) and non-0
  (error/failure).

* Minor improvements to a handful of error messages.

## 0.3.2 (2017-06-17)

* Fixed a bug where if an error occurs while trying to download a new version of
  Alda, the client would incorrectly report that the update was successful.

* Fixed a bug where, when reading Alda code from STDIN, newlines were omitted.
  This could break scores in some cases, e.g. scores containing single-line
  variable definitions.

## 0.3.1 (2017-06-11)

* Added an `alda stop` command which stops playback.

* Added a corresponding `:stop` command in the Alda REPL.

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
[TBuc]: https://github.com/TBuc
