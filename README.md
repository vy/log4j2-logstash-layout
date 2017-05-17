[![Build Status](https://secure.travis-ci.org/vy/log4j2-logstash-layout.svg)](http://travis-ci.org/vy/log4j2-logstash-layout)
[![Maven Central](https://img.shields.io/maven-central/v/com.vlkan.log4j2/log4j2-logstash-layout-parent.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.vlkan.log4j2%22)

`LogstashLayout` plugin provides a [Log4j 2.x](https://logging.apache.org/log4j/2.x/)
layout formatted in JSON that is consumable by
[Logstash](https://www.elastic.co/products/logstash). Compared to
[JSONLayout](https://logging.apache.org/log4j/2.x/manual/layouts.html#JSONLayout)
shipped with Log4j 2.x, `LogstashLayout` provides a more Logstash-friendly
structure and allows certain other flexibilities such as customizable
timestamp formatting (`dateTimeFormatPattern`) and additional JSON fields
(`rootTemplate`).

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
                            rootTemplate='{"application": "awesome", "version": [1, 5, 2]}'
                            prettyPrintEnabled="true"
                            locationInfoEnabled="true"
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
  "application" : "awesome",
  "version" : [ 1, 5, 2 ],
  "@version" : 1,
  "@timestamp" : "2017-05-16T16:05:22.381+02:00",
  "@source_host" : "varlik",
  "@message" : "Hello, error!",
  "@fields" : {
    "logger_name" : "com.vlkan.log4j2.logstash.layout.demo.LogstashLayoutDemo",
    "level" : "ERROR",
    "thread_name" : "main",
    "exception" : {
      "exception_class" : "java.lang.RuntimeException",
      "exception_message" : "test",
      "stacktrace" : "java.lang.RuntimeException: test\n\tat com.vlkan.log4j2.logstash.layout.demo.LogstashLayoutDemo.main(LogstashLayoutDemo.java:10)\n"
    },
    "file" : "LogstashLayoutDemo.java",
    "line_number" : 11,
    "class" : "com.vlkan.log4j2.logstash.layout.demo.LogstashLayoutDemo",
    "method" : "main"
  }
}
```

`LogstashLayout` is configured with the following parameters:

| Parameter Name | Type | Description |
|----------------|------|-------------|
| `charset` | String | output charset (defaults to `UTF-8`) |
| `prettyPrintEnabled` | boolean | enables pretty-printer (defaults to `false`) |
| `locationInfoEnabled` | boolean | includes the filename and line number in the output (defaults to `false`) |
| `stackTraceEnabled` | boolean | includes stack traces (defaults to `false`) |
| `dateTimeFormatPattern` | String | timestamp formatter pattern (defaults to `yyyy-MM-dd'T'HH:mm:ss.SSSZZZ`) |
| `timeZoneId` | String | time zone id (defaults to `TimeZone.getDefault().getID()`) |
| `sourceHost` | String | `@source_host` field (defaults to local hostname) |
| `rootTemplate` | String | root JSON template to be extended while generating the output |

See `layout-demo` directory for a sample application using the `LogstashLayout`.

Fat JAR
=======

Project also contains a `log4j2-logstash-layout-fatjar` artifact which
includes all its transitive dependencies in a separate shaded package (to
avoid the JAR Hell) with the exception of `log4j-core`, that you need to
include separately.

This might come handy if you want to use this plugin along with already
compiled applications, e.g., Elasticsearch 5.x, which requires Log4j 2.x.

# License

Copyright &copy; 2017 [Volkan Yazıcı](http://vlkan.com/)

log4j2-logstash-layout is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by the Free
Software Foundation, either version 3 of the License, or (at your option) any
later version.

log4j2-logstash-layout is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
details.

You should have received a copy of the GNU General Public License along with
this program. If not, see http://www.gnu.org/licenses/.
