[![Build Status](https://secure.travis-ci.org/vy/log4j2-logstash-layout.svg)](http://travis-ci.org/vy/log4j2-logstash-layout)
[![Maven Central](https://img.shields.io/maven-central/v/com.vlkan.log4j2/log4j2-logstash-layout-parent.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.vlkan.log4j2%22)
[![License](https://img.shields.io/github/license/vy/log4j2-logstash-layout.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)

`LogstashLayout` is **the fastest** garbage-free [Log4j 2.x](https://logging.apache.org/log4j/2.x/)
layout with customizable and [Logstash](https://www.elastic.co/products/logstash)-friendly
JSON formatting.

By default, `LogstashLayout` ships the official `JSONEventLayoutV1` stated by
[log4j-jsonevent-layout](https://github.com/logstash/log4j-jsonevent-layout)
Log4j 1.x plugin. Compared to
[JSONLayout](https://logging.apache.org/log4j/2.x/manual/layouts.html#JSONLayout)
included in Log4j 2.x and `log4j-jsonevent-layout`, `LogstashLayout` provides
the following additional features:

- Superior garbage-free [performance](#performance)
- Customizable JSON schema (see `template` and `templateUri` parameters)
- Customizable timestamp formatting (see `dateTimeFormatPattern` and `timeZoneId` parameters)

# Table of Contents

- [Usage](#usage)
- [FAT JAR](#fat-jar)
- [Appender Support](#appender-support)
- [Performance](#performance)
- [Contributors](#contributors)
- [License](#license)

<a name="usage"></a>

# Usage

Add the `log4j2-logstash-layout` dependency to your POM file

```xml
<dependency>
    <groupId>com.vlkan.log4j2</groupId>
    <artifactId>log4j2-logstash-layout</artifactId>
    <version>${log4j2-logstash-layout.version}</version>
</dependency>
```

together with a valid `log4j-core` dependency:

```xml
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-core</artifactId>
    <version>${log4j2.version}</version>
</dependency>
```

Below you can find a sample `log4j2.xml` snippet employing `LogstashLayout`.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="warn">
    <Appenders>
        <Console name="CONSOLE" target="SYSTEM_OUT">
            <LogstashLayout dateTimeFormatPattern="yyyy-MM-dd'T'HH:mm:ss.SSSZZZ"
                            templateUri="classpath:LogstashJsonEventLayoutV1.json"
                            prettyPrintEnabled="true"
                            stackTraceEnabled="true"/>
        </Console>
    </Appenders>
    <Loggers>
        <Root level="info">
            <AppenderRef ref="CONSOLE"/>
        </Root>
    </Loggers>
</Configuration>
```

This generates an output as follows:

```json
{
  "exception": {
    "exception_class": "java.lang.RuntimeException",
    "exception_message": "test",
    "stacktrace": "java.lang.RuntimeException: test\n\tat com.vlkan.log4j2.logstash.layout.demo.LogstashLayoutDemo.main(LogstashLayoutDemo.java:11)\n"
  },
  "line_number": 12,
  "class": "com.vlkan.log4j2.logstash.layout.demo.LogstashLayoutDemo",
  "@version": 1,
  "source_host": "varlik",
  "message": "Hello, error!",
  "thread_name": "main",
  "@timestamp": "2017-05-25T19:56:23.370+02:00",
  "level": "ERROR",
  "file": "LogstashLayoutDemo.java",
  "method": "main",
  "logger_name": "com.vlkan.log4j2.logstash.layout.demo.LogstashLayoutDemo"
}
```

`LogstashLayout` is configured with the following parameters:

| Parameter Name | Type | Description |
|----------------|------|-------------|
| `prettyPrintEnabled` | boolean | enables pretty-printer (defaults to `false`) |
| `locationInfoEnabled` | boolean | includes the filename and line number in the output (defaults to `false`) |
| `stackTraceEnabled` | boolean | includes stack traces (defaults to `false`) |
| `emptyPropertyExclusionEnabled` | boolean | exclude empty and null properties (defaults to `true`) |
| `dateTimeFormatPattern` | String | timestamp formatter pattern (defaults to `yyyy-MM-dd'T'HH:mm:ss.SSSZZZ`) |
| `timeZoneId` | String | time zone id (defaults to `TimeZone.getDefault().getID()`) |
| `mdcKeyPattern` | String | regex to filter MDC keys (does not apply to direct `mdc:key` access) |
| `ndcPattern` | String | regex to filter NDC items |
| `template` | String | inline JSON template for generating the output (has priority over `templateUri`) |
| `templateUri` | String | JSON template for generating the output (defaults to `classpath:LogstashJsonEventLayoutV1.json`) |
| `lineSeparator` | String | used to separate log outputs (defaults to `System.lineSeparator()`) |
| `maxByteCount` | int | used to cap the internal `byte[]` buffer used for serialization (defaults to 512 KiB) |

`templateUri` denotes the URI pointing to the JSON template that will be used
while formatting the log events. By default, `LogstashLayout` ships the
[JSONEventLayoutV1](https://github.com/logstash/log4j-jsonevent-layout)
in `LogstashJsonEventLayoutV1.json` within the classpath:

```json
{
  "mdc": "${json:mdc}",
  "ndc": "${json:ndc}",
  "exception": {
    "exception_class": "${json:exceptionClassName}",
    "exception_message": "${json:exceptionMessage}",
    "stacktrace": "${json:exceptionStackTrace:text}"
  },
  "line_number": "${json:source:lineNumber}",
  "class": "${json:source:className}",
  "@version": 1,
  "source_host": "${hostName}",
  "message": "${json:message}",
  "thread_name": "${json:thread:name}",
  "@timestamp": "${json:timestamp}",
  "level": "${json:level}",
  "file": "${json:source:fileName}",
  "method": "${json:source:methodName}",
  "logger_name": "${json:logger:name}"
}
```

In case of need, you can create your own templates with a structure tailored
to your needs. That is, you can add new fields, remove or rename existing
ones, change the structure, etc. Please note that `templateUri` parameter only
supports `file` and `classpath` URI schemes. 

Below is the list of known template variables that will be replaced while
rendering the JSON output.

| Variable Name | Description |
|---------------|-------------|
| `endOfBatch` | `logEvent.isEndOfBatch()` |
| `exceptionClassName` | `logEvent.getThrown().getClass().getCanonicalName()` |
| `exceptionMessage` | `logEvent.getThrown().getMessage()` |
| `exceptionStackTrace` | `logEvent.getThrown().getStackTrace()` (inactive when `stackTraceEnabled=false`) |
| `exceptionStackTrace:text` | `logEvent.getThrown().printStackTrace()` (inactive when `stackTraceEnabled=false`) |
| `exceptionRootCauseClassName` | the innermost `exceptionClassName` in causal chain |
| `exceptionRootCauseMessage` | the innermost `exceptionMessage` in causal chain |
| `exceptionRootCauseStackTrace[:text]` | the innermost `exceptionStackTrace[:text]` in causal chain |
| `level` | `logEvent.getLevel()` |
| `logger:fqcn` | `logEvent.getLoggerFqcn()` |
| `logger:name` | `logEvent.getLoggerName()` |
| `mdc` | Mapped Diagnostic Context `Map<String, String>` returned by `logEvent.getContextData()` |
| `mdc:<key>` | Mapped Diagnostic Context `String` associated with `key` (`mdcKeyPattern` is discarded) |
| `message` | `logEvent.getFormattedMessage()` |
| `message:json` | if `logEvent.getMessage()` is of type `MultiformatMessage` and supports JSON, its read value, otherwise, `{"message": <formattedMessage>}` object |
| `ndc` | Nested Diagnostic Context `String[]` returned by `logEvent.getContextStack()` |
| `source:className` | `logEvent.getSource().getClassName()` |
| `source:fileName` | `logEvent.getSource().getFileName()` (inactive when `locationInfoEnabled=false`) |
| `source:lineNumber` | `logEvent.getSource().getLineNumber()` (inactive when `locationInfoEnabled=false`) |
| `source:methodName` | `logEvent.getSource().getMethodName()` |
| `thread:id` | `logEvent.getThreadId()` |
| `thread:name` | `logEvent.getThreadName()` |
| `thread:priority` | `logEvent.getThreadPriority()` |
| `timestamp` | `logEvent.getTimeMillis()` formatted using `dateTimeFormatPattern` and `timeZoneId` |
| `timestamp:millis` | `logEvent.getTimeMillis()` |
| `timestamp:nanos` | `logEvent.getNanoTime()` |

JSON field lookups are performed using the `${json:<variable-name>}` scheme
where `<variable-name>` is defined as `<resolver-name>[:<resolver-key>]`.
Characters following colon (`:`) are treated as the `resolver-key`.

[Log4j 2.x Lookups](https://logging.apache.org/log4j/2.0/manual/lookups.html)
(e.g., `${java:version}`, `${env:USER}`, `${date:MM-dd-yyyy}`) are supported
in templates too. Though note that while `${json:...}` template variables is
expected to occupy an entire field, that is, `"level": "${json:level}"`, a
lookup can be mixed within a regular text: `"myCustomField": "Hello, ${env:USER}!"`.

See `layout-demo` directory for a sample application using the `LogstashLayout`.

<a name="fat-jar"></a>

# Fat JAR

Project also contains a `log4j2-logstash-layout-fatjar` artifact which
includes all its transitive dependencies in a separate shaded package (to
avoid the JAR Hell) with the exception of `log4j-core`, that you need to
include separately.

This might come handy if you want to use this plugin along with already
compiled applications, e.g., Elasticsearch 5.x, which requires Log4j 2.x.

<a name="appender-support"></a>

# Appender Support

`log4j2-logstash-layout` is all about providing a highly customizable JSON
schema for your logs. Though this does not necessarily mean that all of its
features are expected to be supported by every appender in the market. For
instance, while `prettyPrintEnabled=true` works fine with
[log4j2-redis-appender](/vy/log4j2-redis-appender), it should be turned off
for Logstash's `log4j-json` file input type. (See
[Pretty printing in Logstash](/vy/log4j2-logstash-layout/issues/8) issue.)
Make sure you configure `log4j2-logstash-layout` properly in a way that
is aligned with your appender of preference.

<a name="performance"></a>

# Performance

The source code ships a `LogstashLayout`-vs-[`JsonLayout`](https://logging.apache.org/log4j/2.0/manual/layouts.html#JSONLayout)
(the one shipped by default in Log4j 2.X) [JMH](https://openjdk.java.net/projects/code-tools/jmh/)
benchmark assessing the rendering performance of both plugins. There two
different `LogEvent` profiles are employed:

- **full**: `LogEvent` contains MDC, NDC, and an exception.
- **lite:** `LogEvent` has no MDC, NDC, or exception attachment.

To give an idea, we ran the benchmark with the following settings:

- **CPU:** Intel i7 2.70GHz (x86-64)
- **JVM:** Java HotSpot 1.8.0_161
- **OS:** Xubuntu 18.04.1 (4.15.0-34-generic, x86-64)
- **`LogstashLayout:`** used default settings with the following exceptions:
  - **`stackTraceEnabled`:** `true`
  - employed custom [`Log4j2JsonLayout.json`](layout/src/test/resources/Log4j2JsonLayout.json)
    template adopted from the JSON schema used by `JsonLayout`
- **`JsonLayout`:** used in two different flavors
  - **`DefaultJsonLayout`:** default settings
  - **`CustomJsonLayout`:** default settings with an additional `"@version": 1`
    field (this forces instantiation of a wrapper class to obtain the necessary
    Jackson view)

The summary (see [`layout-benchmark`](layout-benchmark) directory) of the
results for single-threaded run are as follows:

<div id="results">
    <table>
        <thead>
            <tr>
                <th>Benchmark</th>
                <th>TLA?<sup>*</sup></th>
                <th colspan="2">ops/sec<sup>**</sup></th>
                <th>MB/sec<sup>**</sup></th>
            </tr>
        </thead>
        <tbody>
            <tr data-benchmark="liteLogstashLayout">
                <td class="benchmark">liteLogstashLayout</td>
                <td class="tla">✓</td>
                <td class="op_rate">1,309,305</td>
                <td class="op_rate_bar">▉▉▉▉▉▉▉▉▉▉▉▉▉▉▉▉▉▉▉▉ (100%)</td>
                <td class="gc_rate">0.4</td>
            </tr>
            <tr data-benchmark="liteLogstashLayout">
                <td class="benchmark">liteLogstashLayout</td>
                <td class="tla">✗</td>
                <td class="op_rate">1,308,970</td>
                <td class="op_rate_bar">▉▉▉▉▉▉▉▉▉▉▉▉▉▉▉▉▉▉▉ (100%)</td>
                <td class="gc_rate">0.4</td>
            </tr>
            <tr data-benchmark="liteDefaultJsonLayout">
                <td class="benchmark">liteDefaultJsonLayout</td>
                <td class="tla">✓</td>
                <td class="op_rate">625,116</td>
                <td class="op_rate_bar">▉▉▉▉▉▉▉▉▉▉ (48%)</td>
                <td class="gc_rate">2,915.2</td>
            </tr>
            <tr data-benchmark="liteDefaultJsonLayout">
                <td class="benchmark">liteDefaultJsonLayout</td>
                <td class="tla">✗</td>
                <td class="op_rate">615,219</td>
                <td class="op_rate_bar">▉▉▉▉▉▉▉▉▉ (47%)</td>
                <td class="gc_rate">2,869.1</td>
            </tr>
            <tr data-benchmark="liteCustomJsonLayout">
                <td class="benchmark">liteCustomJsonLayout</td>
                <td class="tla">✓</td>
                <td class="op_rate">551,004</td>
                <td class="op_rate_bar">▉▉▉▉▉▉▉▉ (42%)</td>
                <td class="gc_rate">2,781.3</td>
            </tr>
            <tr data-benchmark="liteCustomJsonLayout">
                <td class="benchmark">liteCustomJsonLayout</td>
                <td class="tla">✗</td>
                <td class="op_rate">534,704</td>
                <td class="op_rate_bar">▉▉▉▉▉▉▉▉ (41%)</td>
                <td class="gc_rate">2,699.0</td>
            </tr>
            <tr data-benchmark="fullLogstashLayout">
                <td class="benchmark">fullLogstashLayout</td>
                <td class="tla">✗</td>
                <td class="op_rate">133,726</td>
                <td class="op_rate_bar">▉▉ (10%)</td>
                <td class="gc_rate">13.5</td>
            </tr>
            <tr data-benchmark="fullLogstashLayout">
                <td class="benchmark">fullLogstashLayout</td>
                <td class="tla">✓</td>
                <td class="op_rate">130,923</td>
                <td class="op_rate_bar">▉▉ (10%)</td>
                <td class="gc_rate">13.2</td>
            </tr>
            <tr data-benchmark="fullCustomJsonLayout">
                <td class="benchmark">fullCustomJsonLayout</td>
                <td class="tla">✗</td>
                <td class="op_rate">15,509</td>
                <td class="op_rate_bar">▉ (1%)</td>
                <td class="gc_rate">3,412.6</td>
            </tr>
            <tr data-benchmark="fullDefaultJsonLayout">
                <td class="benchmark">fullDefaultJsonLayout</td>
                <td class="tla">✓</td>
                <td class="op_rate">15,368</td>
                <td class="op_rate_bar">▉ (1%)</td>
                <td class="gc_rate">3,378.8</td>
            </tr>
            <tr data-benchmark="fullDefaultJsonLayout">
                <td class="benchmark">fullDefaultJsonLayout</td>
                <td class="tla">✗</td>
                <td class="op_rate">15,362</td>
                <td class="op_rate_bar">▉ (1%)</td>
                <td class="gc_rate">3,377.3</td>
            </tr>
            <tr data-benchmark="fullCustomJsonLayout">
                <td class="benchmark">fullCustomJsonLayout</td>
                <td class="tla">✓</td>
                <td class="op_rate">15,324</td>
                <td class="op_rate_bar">▉ (1%)</td>
                <td class="gc_rate">3,371.9</td>
            </tr>
        </tbody>
    </table>
    <p id="footnotes">
        <sup>*</sup> Thread local allocations (i.e., <code>log4j2.enable.threadlocals</code> flag) enabled?<br/>
        <sup>**</sup> 99<sup>th</sup> percentile
    </p>
</div>

As results point out, `log4j2-logstash-layout` is the fastest JSON layout in the town.

<a name="contributors"></a>

# Contributors

- [Eric Schwartz](https://github.com/emschwar)
- [Michael K. Edwards](https://github.com/mkedwards)
- [Yaroslav Skopets](https://github.com/yskopets)

<a name="license"></a>

# License

Copyright &copy; 2017-2018 [Volkan Yazıcı](http://vlkan.com/)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
