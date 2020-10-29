### (2020-10-29) v1.0.4

- Try to dump the cause when stack trace serialization fails.

### (2020-06-30) v1.0.3

- Fix handling of non-`String` values in `map` directive. (#61)

### (2020-03-18) v1.0.2

- Fix `ArrayIndexOutOfBoundsException` thrown when `stackTrace:text` produces
  an output violating the truncation limit. (#57)

- Implement work around for FasterXML/jackson-core#609 triggered when
  `maxStringLength` is used in combination with
  `emptyPropertyExclusionEnabled=true`. (#55)

### (2020-01-21) v1.0.1

- Fix NPE in `StringTruncatingGeneratorDelegate`. (#53)

### (2020-01-08) v1.0.0

- Switched to semantic versioning.

- Add Java 9 module name: `com.vlkan.log4j2.logstash.layout`.

- Set `maxByteCount` to 16 KiB by default.

- Set `emptyPropertyExclusionEnabled` to `false` by default.

- Remove `timestamp:nanos` and `timestamp:millis` directives in favor of
  `timestamp:epoch[:divisor=<divisor>[,integral]]` directive.

- Make formatted timestamp resolver GC-free.

- Replace object pools with thread locals.

- Add `locale` configuration.

- Fix `JsonGenerator` state corruption in `ExceptionResolver` if
  `LogEvent#getThrown()` is `null`.

- Add `GelfLayout.json` template.

- Add `level:severity` and `level:severity:code` resolvers. (#48)

- Add `timestamp:epoch:divisor=<divisor>` resolver. (#48)

### (2019-10-15) v0.21

- Add feature comparison matrix.

- Update benchmarks.

- Add ECS layout. (#39)

- Add `char[]` caching for serializing stack traces.

- Add serialization context caching.

- Add `eventTemplateAdditionalFields` parameter. (#43)

- Cache and reuse the most recently formatted timestamp. (#42)

- Support user provided `ObjectMapper`s. (#35)

- Support `SimpleMessage` in `${json:message:json}` directive. (#36)

- Support `ObjectMessage` in `${json:message:json}` directive. (#36)

### (2019-08-09) v0.20 \[failed due to Sonatype mishap]

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
