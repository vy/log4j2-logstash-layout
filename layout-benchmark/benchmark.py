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


def get_json_output_file():
    return os.path.join(BENCHMARK_DIR, "results.json")


def get_mvn_output_file():
    return os.path.join(BENCHMARK_DIR, "results.out")


def run_benchmark():
    LOGGER.info("Starting benchmark...")
    start_instant_seconds = time.time()
    json_output_file = get_json_output_file()
    mvn_output_file = get_mvn_output_file()
    with open(mvn_output_file, "w") as mvn_output_file_handle:
        env = os.environ.copy()
        env["MAVEN_OPTS"] = "-XX:+TieredCompilation -XX:+AggressiveOpts"
        popen = subprocess.Popen(
            ["taskset",
             "-c", "0",
             "time",
             "mvn",
             "-pl", "layout",
             "exec:java",
             "-Dlog4j2.garbagefreeThreadContextMap=true",
             "-Dlog4j2.enableDirectEncoders=true",
             "-Dlog4j2.logstashLayoutBenchmark.jsonOutputFile={}".format(json_output_file)],
            env=env,
            bufsize=1,
            universal_newlines=True,
            cwd=PROJECT_DIR,
            stderr=subprocess.PIPE,
            stdout=mvn_output_file_handle)
        popen.communicate()
        return_code = popen.returncode
        if return_code != 0:
            raise Exception("benchmark failure (return_code={})".format(return_code))
        stop_instant_seconds = time.time()
        total_duration_seconds = stop_instant_seconds - start_instant_seconds
        LOGGER.info("Completed benchmark... (total_duration_seconds={})".format(total_duration_seconds))


def read_results():

    # Collect results.
    results = []
    json_output_file = get_json_output_file()
    with open(json_output_file) as json_output_file_handle:
        json_dicts = json.load(json_output_file_handle)
        for json_dict in json_dicts:
            results.append({
                "benchmark": json_dict["benchmark"],
                "op_rate": json_dict["primaryMetric"]["scorePercentiles"]["99.0"],
                "gc_rate": json_dict["secondaryMetrics"][u"·gc.alloc.rate.norm"]["scorePercentiles"]["99.0"]
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
        #results .op_rate, #results .gc_rate { text-align: right }
        #results th { background-color: #cfcfcf }
        #results tr[data-benchmark $= "LogstashLayout"] td { color: green }
        #results tr[data-benchmark $= "LogstashLayout"] td.benchmark,
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
                    <th colspan="2">ops/sec<sup>*</sup></th>
                    <th>B/op<sup>*</sup></th>
                </tr>
            </thead>
            <tbody>""")
        for result in results:
            benchmark_name = re.sub(r".*\.([a-zA-Z0-9]+)", r"\1", result["benchmark"])
            html_file_handle.write("""
                <tr data-benchmark="{}">
                    <td class="benchmark">{}</td>
                    <td class="op_rate">{}</td>
                    <td class="op_rate_bar">{}</td>
                    <td class="gc_rate">{}</td>
                </tr>""".format(
                benchmark_name,
                benchmark_name,
                "{:,.0f}".format(result["op_rate"] * 1e3),
                ("▉" * (1 + int(19 * result["op_rate_norm"]))) + (" ({:.0f}%)".format(100 * result["op_rate_norm"])),
                "{:,.1f}".format(max(0, result["gc_rate"]))))
        html_file_handle.write("""
            </tbody>
        </table>
        <p id="footnotes">
            <sup>*</sup> 99<sup>th</sup> percentile
        </p>
    </div>
</body>
""")


def main():
    run_benchmark()
    plot_results()


if __name__ == "__main__":
    main()
