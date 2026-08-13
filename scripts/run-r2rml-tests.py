import difflib
import os
import re
import shutil
import signal
import subprocess
import sys
import time
from pathlib import Path

PROJECT_DIRECTORY = Path(__file__).resolve().parent.parent
BUILD_DIRECTORY = PROJECT_DIRECTORY / "build" / "r2rml-tests"
CASES_DIRECTORY = PROJECT_DIRECTORY / "R2RML2Datalog-Tests"


def resolve_executable(name: str) -> str:
    if os.name == "nt" and not name.lower().endswith(".cmd"):
        cmd_name = f"{name}.cmd"
        resolved = shutil.which(cmd_name)
        if resolved:
            return resolved
    resolved = shutil.which(name)
    return resolved or name


def get_docker_user() -> str:
    if os.name == "nt":
        return "0:0"
    try:
        return f"{os.getuid()}:{os.getgid()}"
    except AttributeError:
        return "0:0"


MYSQL_IMAGE = (
    "mysql:8.4@sha256:b3b90af2a6552ae30c266fdb7d5dd55f3afb72404bb78d37fe8a23eb857fd3fb"
)
SOUFFLE_IMAGE = (
    "alloka/souffle:v1.0.0@sha256:"
    "0e9288ca6f7a63faf93f4358f210de0ffcab6e3e2405d88c365391da6d54fe89"
)
MYSQL_CONTAINER = f"r2rml-tests-mysql-{os.getpid()}"
MYSQL_PASSWORD = "r2rml-tests"
OUTPUT_FILES = ("output.nq",)
EXPECTED_CASES = 49


def normalize_rdf_row(line: str) -> str:
    row = line.strip()
    if not row:
        return ""
    if "\t" in row:
        parts = [part.strip() for part in row.split("\t")]
        if len(parts) == 3:
            subject, predicate, object_ = parts
            return f"{subject} {predicate} {object_} ."
        if len(parts) >= 4:
            subject, predicate, object_, *graph = parts
            graph_text = graph[0] if graph else ""
            if graph_text:
                return f"{subject} {predicate} {object_} {graph_text} ."
            return f"{subject} {predicate} {object_} ."
    return row.rstrip(" .") + " ." if not row.endswith(".") else row


def materialize_output_nq(case_directory: Path) -> Path:
    output_path = case_directory / "output.nq"
    rows: list[str] = []
    for csv_name in ("triple.csv", "quadruple.csv"):
        csv_path = case_directory / csv_name
        if not csv_path.exists():
            continue
        for line in csv_path.read_text(encoding="utf-8").splitlines():
            normalized = normalize_rdf_row(line)
            if normalized:
                rows.append(normalized)
    if rows:
        output_path.write_text("\n".join(sorted(rows)) + "\n", encoding="utf-8")
    return output_path


def print_log(path: Path) -> None:
    sys.stdout.write(path.read_text(encoding="utf-8"))
    sys.stdout.flush()


def run_logged(
    command: list[str], log_path: Path, input_text: str | None = None
) -> bool:
    with log_path.open("w", encoding="utf-8") as log_file:
        result = subprocess.run(
            command,
            input=input_text,
            text=True,
            stdout=log_file,
            stderr=subprocess.STDOUT,
            check=False,
        )
    return result.returncode == 0


def sorted_contents(path: Path) -> bytes:
    contents = path.read_bytes().replace(b"\r\n", b"\n").replace(b"\r", b"\n")
    if not contents:
        return b""
    normalized_lines = []
    for raw_line in contents.split(b"\n"):
        if not raw_line:
            continue
        line = re.sub(rb"\s+\.$", b" .", raw_line.strip())
        if not line:
            continue
        # normalize multiple spaces between IRI/literal term boundaries
        line = re.sub(rb">\s{2,}([<\"_])", rb"> \1", line)
        normalized_lines.append(line)
    return b"".join(line + b"\n" for line in sorted(normalized_lines))


def compare_output(source_directory: Path, case_directory: Path, name: str) -> bytes:
    expected = sorted_contents(source_directory / name)
    actual = sorted_contents(case_directory / name)
    difference = b"".join(
        difflib.diff_bytes(
            difflib.unified_diff,
            expected.splitlines(keepends=True),
            actual.splitlines(keepends=True),
            fromfile=os.fsencode(source_directory / name),
            tofile=os.fsencode(case_directory / name),
        )
    )
    if difference:
        (case_directory / f"{name}.diff").write_bytes(difference)
    return difference


def run_case(
    source_directory: Path,
    classpath: str,
    mysql_port: str,
    docker_user: str,
) -> bool:
    case_name = source_directory.name
    database_name = case_name.lower().replace("-mysql", "")
    case_directory = BUILD_DIRECTORY / "cases" / case_name
    case_directory.mkdir(parents=True)
    shutil.copy2(source_directory / "mapping.ttl", case_directory / "mapping.ttl")

    database_log = case_directory / "database.log"
    database_input = f"CREATE DATABASE `{database_name}`;\nUSE `{database_name}`;\n" + (
        source_directory / "resource.sql"
    ).read_text(encoding="utf-8")
    database_command = [
        "docker",
        "exec",
        "--interactive",
        "--env",
        f"MYSQL_PWD={MYSQL_PASSWORD}",
        MYSQL_CONTAINER,
        "mysql",
        "--user=root",
    ]
    if not run_logged(database_command, database_log, database_input):
        print(f"{case_name}: FAIL (database setup)", flush=True)
        print_log(database_log)
        return False

    program_path = case_directory / "Datalog_rules.rs"
    translation_log = case_directory / "translation.log"
    translation_command = [
        "java",
        "-cp",
        classpath,
        "translator.r2rml.datalog.Main",
        "-m",
        str(case_directory / "mapping.ttl"),
        "-dsn",
        (
            f"jdbc:mysql://127.0.0.1:{mysql_port}/{database_name}"
            "?allowPublicKeyRetrieval=true"
        ),
        "-u",
        "root",
        "-p",
        MYSQL_PASSWORD,
        "-o",
        str(program_path),
        "-bt",
    ]
    if not run_logged(translation_command, translation_log) or (
        not program_path.is_file() or program_path.stat().st_size == 0
    ):
        print(f"{case_name}: FAIL (translation)", flush=True)
        print_log(translation_log)
        return False

    souffle_log = case_directory / "souffle.log"
    souffle_command = [
        "docker",
        "run",
        "--rm",
        "--user",
        docker_user,
        "--volume",
        f"{BUILD_DIRECTORY}:/work",
        "--workdir",
        f"/work/cases/{case_name}",
        "--entrypoint",
        "/souffle/bin/souffle",
        SOUFFLE_IMAGE,
        "-L",
        "/work",
        "Datalog_rules.rs",
    ]
    if not run_logged(souffle_command, souffle_log):
        print(f"{case_name}: FAIL (Souffle execution)", flush=True)
        print_log(souffle_log)
        return False

    materialize_output_nq(case_directory)
    case_passed = True
    for output_file in OUTPUT_FILES:
        difference = compare_output(source_directory, case_directory, output_file)
        if difference:
            print(f"{case_name}: FAIL ({output_file} comparison)", flush=True)
            sys.stdout.buffer.write(difference)
            sys.stdout.buffer.flush()
            case_passed = False

    if case_passed:
        for log_path in case_directory.glob("*.log"):
            log_path.unlink()
        print(f"{case_name}: PASS", flush=True)
    return case_passed


def remove_mysql_container() -> None:
    subprocess.run(
        ["docker", "rm", "--force", MYSQL_CONTAINER],
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
        check=False,
    )


def stop_on_signal(signum, _frame) -> None:
    raise SystemExit(128 + signum)


def main() -> int:
    shutil.rmtree(BUILD_DIRECTORY, ignore_errors=True)
    case_directories = sorted(CASES_DIRECTORY.glob("R2RMLTC*-MySQL"))
    if len(case_directories) != EXPECTED_CASES:
        print(
            f"Expected {EXPECTED_CASES} R2RML cases, found {len(case_directories)}",
            file=sys.stderr,
        )
        return 1

    translator_directory = BUILD_DIRECTORY / "translator"
    translator_directory.mkdir(parents=True)
    shutil.copy2(
        PROJECT_DIRECTORY / "translator" / "pom.xml",
        translator_directory / "pom.xml",
    )
    shutil.copytree(
        PROJECT_DIRECTORY / "translator" / "src", translator_directory / "src"
    )

    mvn_executable = resolve_executable("mvn")
    if subprocess.run(
        [
            mvn_executable,
            "--quiet",
            "--file",
            str(translator_directory / "pom.xml"),
            "compile",
            "dependency:build-classpath",
            f"-Dmdep.outputFile={BUILD_DIRECTORY / 'classpath.txt'}",
        ],
        check=False,
    ).returncode:
        print("Build failed", file=sys.stderr)
        return 1

    docker_user = get_docker_user()
    if subprocess.run(
        [
            "docker",
            "run",
            "--rm",
            "--user",
            docker_user,
            "--volume",
            f"{PROJECT_DIRECTORY}:/source:ro",
            "--volume",
            f"{BUILD_DIRECTORY}:/work",
            "--entrypoint",
            "g++",
            SOUFFLE_IMAGE,
            "-std=c++17",
            "-shared",
            "-fPIC",
            "/source/functors.cpp",
            "-o",
            "/work/libfunctors.so",
        ],
        check=False,
    ).returncode:
        print("Functor build failed", file=sys.stderr)
        return 1

    if subprocess.run(
        [
            "docker",
            "run",
            "--detach",
            "--rm",
            "--name",
            MYSQL_CONTAINER,
            "--env",
            f"MYSQL_ROOT_PASSWORD={MYSQL_PASSWORD}",
            "--publish",
            "127.0.0.1::3306",
            MYSQL_IMAGE,
        ],
        stdout=subprocess.DEVNULL,
        check=False,
    ).returncode:
        print("MySQL startup failed", file=sys.stderr)
        return 1

    signal.signal(signal.SIGTERM, stop_on_signal)
    try:
        for _ in range(60):
            if (
                subprocess.run(
                    [
                        "docker",
                        "exec",
                        "--env",
                        f"MYSQL_PWD={MYSQL_PASSWORD}",
                        MYSQL_CONTAINER,
                        "mysql",
                        "--user=root",
                        "--execute",
                        "SELECT 1",
                    ],
                    stdout=subprocess.DEVNULL,
                    stderr=subprocess.DEVNULL,
                    check=False,
                ).returncode
                == 0
            ):
                break
            time.sleep(1)
        else:
            subprocess.run(
                ["docker", "logs", MYSQL_CONTAINER],
                stdout=sys.stderr,
                stderr=sys.stderr,
                check=False,
            )
            print("MySQL did not become ready", file=sys.stderr)
            return 1

        mysql_port = (
            subprocess.run(
                ["docker", "port", MYSQL_CONTAINER, "3306/tcp"],
                check=True,
                capture_output=True,
                text=True,
            )
            .stdout.strip()
            .rsplit(":", maxsplit=1)[1]
        )
        dependencies = (
            (BUILD_DIRECTORY / "classpath.txt").read_text(encoding="utf-8").strip()
        )
        classpath = os.pathsep.join(
            [str(translator_directory / "target" / "classes"), dependencies]
        )

        passed = 0
        failed = 0
        for source_directory in case_directories:
            if run_case(source_directory, classpath, mysql_port, docker_user):
                passed += 1
            else:
                failed += 1

        print(f"\n{passed} passed, {failed} failed, {passed + failed} total")
        return 1 if failed else 0
    finally:
        remove_mysql_container()


if __name__ == "__main__":
    raise SystemExit(main())
