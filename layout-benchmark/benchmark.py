#!/usr/bin/env python
# coding=utf-8


import json
import logging
import os
import re
import subprocess
import time


logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)7s [%(name)s] %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S")
LOGGER = logging.getLogger(__name__)


BENCHMARK_DIR = os.path.dirname(os.path.realpath(__file__))
PROJECT_DIR = os.path.abspath(os.path.join(BENCHMARK_DIR, ".."))


def get_json_output_file(tla_enabled):
    filename = "results-tla-{}.json".format(tla_enabled)
    return os.path.join(BENCHMARK_DIR, filename)


def get_mvn_output_file(tla_enabled):
    filename = "results-tla-{}.out".format(tla_enabled)
    return os.path.join(BENCHMARK_DIR, filename)


def run_benchmark(tla_enabled):
    LOGGER.info("Starting benchmark... (tla_enabled={})".format(tla_enabled))
    start_instant_seconds = time.time()
    json_output_file = get_json_output_file(tla_enabled)
    mvn_output_file = get_mvn_output_file(tla_enabled)
    with open(mvn_output_file, "w") as mvn_output_file_handle:
        popen = subprocess.Popen(
            ["time",
             "mvn",
             "-pl", "layout",
             "exec:java",
             "-Dlog4j2.garbagefreeThreadContextMap=true",
             "-Dlog4j2.enableDirectEncoders=true",
             "-Dlog4j2.enable.threadlocals={}".format(str(tla_enabled).lower()),
             "-Dlog4j2.logstashLayoutBenchmark.jsonOutputFile={}".format(json_output_file)],
            bufsize=1,
            universal_newlines=True,
            cwd=PROJECT_DIR,
            stderr=subprocess.PIPE,
            stdout=mvn_output_file_handle)
        popen.communicate()
        return_code = popen.returncode
        if return_code != 0:
            raise Exception("benchmark failure (tla_enabled={}, return_code={})", tla_enabled, return_code)
        stop_instant_seconds = time.time()
        total_duration_seconds = stop_instant_seconds - start_instant_seconds
        LOGGER.info("Completed benchmark... (tla_enabled={}, total_duration_seconds={})".format(tla_enabled, total_duration_seconds))


def run_benchmarks():
    LOGGER.info("Starting benchmarks...")
    start_instant_seconds = time.time()
    for tla_enabled in [True, False]:
        run_benchmark(tla_enabled)
    stop_instant_seconds = time.time()
    total_duration_seconds = stop_instant_seconds - start_instant_seconds
    LOGGER.info("Completed benchmarks... (total_duration_seconds={})".format(total_duration_seconds))


def read_results():

    # Collect results.
    results = []
    for tla_enabled in [True, False]:
        json_output_file = get_json_output_file(tla_enabled)
        with open(json_output_file) as json_output_file_handle:
            json_dicts = json.load(json_output_file_handle)
            for json_dict in json_dicts:
                results.append({
                    "tla_enabled": tla_enabled,
                    "benchmark": json_dict["benchmark"],
                    "op_rate": json_dict["primaryMetric"]["scorePercentiles"]["99.0"],
                    "gc_rate": json_dict["secondaryMetrics"][u"·gc.alloc.rate"]["scorePercentiles"]["99.0"]
                })

    # Enrich results with normalized op rate slowdown.
    max_op_rate = max([result["op_rate"] for result in results])
    for result in results:
        result["op_rate_norm"] = result["op_rate"] / max_op_rate

    # Sort and return results.
    results.sort(key=lambda result: result["op_rate"], reverse=True)
    return results


def plot_results():
    results = read_results()
    html_file = os.path.join(BENCHMARK_DIR, "results.html")
    with open(html_file, "w") as html_file_handle:
        html_file_handle.write("""
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title>log4j2-logstash-layout Benchmark Results</title>
</head>
<body>
    <style>
        #results th, #results td { padding: 0.3em }
        #results .tla { text-align: center }
        #results .op_rate, #results .gc_rate { text-align: right }
        #results th { background-color: #cfcfcf }
        #results tr[data-benchmark $= "LogstashLayout"] td { color: green }
        #results tr[data-benchmark $= "LogstashLayout"] td.benchmark,
        #results tr[data-benchmark $= "LogstashLayout"] td.tla,
        #results tr[data-benchmark $= "LogstashLayout"] td.op_rate,
        #results tr[data-benchmark $= "LogstashLayout"] td.gc_rate
        { font-weight: bold }
        #results #footnotes { font-size: 0.8em; }
    </style>
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
            <tbody>""")
        for result in results:
            benchmark_name = re.sub(r".*\.([a-zA-Z0-9]+)", r"\1", result["benchmark"])
            html_file_handle.write("""
                <tr data-benchmark="{}">
                    <td class="benchmark">{}</td>
                    <td class="tla">{}</td>
                    <td class="op_rate">{}</td>
                    <td class="op_rate_bar">{}</td>
                    <td class="gc_rate">{}</td>
                </tr>""".format(
                benchmark_name,
                benchmark_name,
                "✓" if result["tla_enabled"] else "✗",
                "{:,.0f}".format(result["op_rate"] * 1e3),
                ("▉" * (1 + int(19 * result["op_rate_norm"]))) + (" ({:.0f}%)".format(100 * result["op_rate_norm"])),
                "{:,.1f}".format(result["gc_rate"])))
        html_file_handle.write("""
            </tbody>
        </table>
        <p id="footnotes">
            <sup>*</sup> Thread local allocations (i.e., <code>log4j2.enable.threadlocals</code> flag) enabled?<br/>
            <sup>**</sup> 99<sup>th</sup> percentile
        </p>
    </div>
</body>
""")


def main():
    run_benchmarks()
    plot_results()


if __name__ == "__main__":
    main()
