# CHANGELOG

## 0.1.1 (2017-01-14)

* Fixed 2 bugs re: `alda list` output:
  * It didn't work correctly on non-OS X systems like Ubuntu due to differences in the `ps` command across Unix distributions.
  * Fixed buggy output when running multiple Alda servers ([#4](https://github.com/alda-lang/alda-client-java/issues/4)).

Major thanks to [tobiasriedling] for both fixes!

## 0.1.0 (2016-11-19)

* * Extracted alda-client-java from the [main Alda repo](https://github.com/alda-lang/alda) as of version 1.0.0-rc50.

[tobiasriedling]: https://github.com/tobiasriedling
