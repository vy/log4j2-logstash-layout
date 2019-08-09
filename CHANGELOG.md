### (2019-08-09) v0.20

- Use UTF-8 while serializing stack traces.

### (2019-06-13) v0.19

- Add `maxStringLength` parameter enabling truncation of string fields. (#31)

- Add `${json:map:xxx}` resolver for `MapLookup`s. (#33)

- Fix the fallback to Log4j generic substitution for non-`${json:*}` templates. (#33)

### (2019-04-29) v0.18

- Recover from corrupted `JsonGenerator` state after an exception. (#27)

- Removed thread-locals, the plugin is not garbage-free anymore. It turned out
  to be pretty tricky to reuse a thread-local `JsonGenerator`. That said, the
  change almost had no performance impact. (#27, #29)

### (2019-04-08) v0.17

- Make `LogstashLayout` backward compatible with Log4j 2.8. (#26)

### (2019-02-12) v0.16

- Upgraded Jackson to 2.9.8, which fixes vulnerability CVE-2018-1000873. (#23)

- Bumped Java version to 1.8. This was necessary due to Jackson upgrade. (#23)

- Added `marker:name` resolver. (#21)
