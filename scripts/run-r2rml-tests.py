import difflib
import os
import re
import shutil
import signal
import subprocess
import sys
import time
from pathlib import Path
from typing import NamedTuple

PROJECT_DIRECTORY = Path(__file__).resolve().parent.parent
BUILD_DIRECTORY = PROJECT_DIRECTORY / "build" / "r2rml-tests"
DTAI_CASES_DIRECTORY = PROJECT_DIRECTORY / "R2RML2Datalog-Tests"
OFFICIAL_CASES_DIRECTORY = PROJECT_DIRECTORY / "r2rml-test-cases-support"
OFFICIAL_DATABASES_DIRECTORY = OFFICIAL_CASES_DIRECTORY / "databases"


class TestCatalog(NamedTuple):
    slug: str
    label: str
    cases_directory: Path
    case_pattern: str
    expected_cases: int


DTAI_CATALOG = TestCatalog("dtai", "DTAI", DTAI_CASES_DIRECTORY, "R2RMLTC*-MySQL", 49)
OFFICIAL_CATALOG = TestCatalog(
    "official", "Official", OFFICIAL_CASES_DIRECTORY, "R2RMLTC*", 62
)
TEST_CATALOGS = (DTAI_CATALOG, OFFICIAL_CATALOG)


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


MYSQL_IMAGE = "mysql:9.7.1@sha256:8fdf311514c91fa5014e93e98e19d7f5d9eb162a462c13100c956dacc278ee21"
POSTGRES_IMAGE = (
    "postgres:13@sha256:"
    "4689940c683801b4ab839ab3b0a0a3555a5fe425371422310944e89eca7d8068"
)
SOUFFLE_IMAGE = (
    "alloka/souffle:v1.0.0@sha256:"
    "0e9288ca6f7a63faf93f4358f210de0ffcab6e3e2405d88c365391da6d54fe89"
)
MYSQL_CONTAINER = f"r2rml-tests-mysql-{os.getpid()}"
POSTGRES_CONTAINER = f"r2rml-tests-postgres-{os.getpid()}"
DATABASE_NAME = "r2rml"
DATABASE_PASSWORD = "r2rml-tests"
POSTGRES_CASES = {"R2RMLTC0002f", "R2RMLTC0018a"}


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
    output_path.write_text(
        "\n".join(sorted(rows)) + ("\n" if rows else ""), encoding="utf-8"
    )
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
        if not raw_line or raw_line.lstrip().startswith(b"#"):
            continue
        line = re.sub(rb"\s+\.$", b" .", raw_line.strip())
        if not line:
            continue
        # normalize multiple spaces between IRI/literal term boundaries
        line = re.sub(rb">\s{2,}([<\"_])", rb"> \1", line)
        normalized_lines.append(line)
    return b"".join(line + b"\n" for line in sorted(normalized_lines))


def compare_output(expected_path: Path, actual_path: Path) -> bytes:
    expected = sorted_contents(expected_path)
    actual = sorted_contents(actual_path)
    difference = b"".join(
        difflib.diff_bytes(
            difflib.unified_diff,
            expected.splitlines(keepends=True),
            actual.splitlines(keepends=True),
            fromfile=os.fsencode(expected_path),
            tofile=os.fsencode(actual_path),
        )
    )
    if difference:
        (actual_path.parent / "output.nq.diff").write_bytes(difference)
    return difference


def load_mysql_database(database_name: str, script_path: Path, log_path: Path) -> bool:
    database_input = (
        f"DROP DATABASE IF EXISTS `{database_name}`;\n"
        f"CREATE DATABASE `{database_name}`;\n"
        f"USE `{database_name}`;\n" + script_path.read_text(encoding="utf-8")
    )
    return run_logged(
        [
            "docker",
            "exec",
            "--interactive",
            "--env",
            f"MYSQL_PWD={DATABASE_PASSWORD}",
            MYSQL_CONTAINER,
            "mysql",
            "--user=root",
        ],
        log_path,
        database_input,
    )


def load_postgres_database(script_path: Path, log_path: Path) -> bool:
    reset_command = [
        "docker",
        "exec",
        "--env",
        f"PGPASSWORD={DATABASE_PASSWORD}",
        POSTGRES_CONTAINER,
        "psql",
        "--username=postgres",
        "--dbname=postgres",
        "--set=ON_ERROR_STOP=1",
        "--command",
        f"DROP DATABASE IF EXISTS {DATABASE_NAME};",
        "--command",
        f"CREATE DATABASE {DATABASE_NAME};",
    ]
    if not run_logged(reset_command, log_path):
        return False
    return run_logged(
        [
            "docker",
            "exec",
            "--interactive",
            "--env",
            f"PGPASSWORD={DATABASE_PASSWORD}",
            POSTGRES_CONTAINER,
            "psql",
            "--username=postgres",
            f"--dbname={DATABASE_NAME}",
            "--set=ON_ERROR_STOP=1",
        ],
        log_path,
        script_path.read_text(encoding="utf-8"),
    )


def remove_case_logs(case_directory: Path) -> None:
    for log_path in case_directory.glob("*.log"):
        log_path.unlink()


def run_case(
    catalog: TestCatalog,
    source_directory: Path,
    classpath: str,
    mysql_port: str,
    postgres_port: str,
    docker_user: str,
) -> bool:
    case_name = source_directory.name
    case_label = f"{catalog.label}/{case_name}"
    case_directory = BUILD_DIRECTORY / "cases" / catalog.slug / case_name
    case_directory.mkdir(parents=True)

    database_log = case_directory / "database.log"
    if catalog == DTAI_CATALOG:
        mapping_source = source_directory / "mapping.ttl"
        expected_output = source_directory / "output.nq"
        database_name = case_name.lower().replace("-mysql", "")
        database_ready = load_mysql_database(
            database_name, source_directory / "resource.sql", database_log
        )
        database_dsn = (
            f"jdbc:mysql://127.0.0.1:{mysql_port}/{database_name}"
            "?allowPublicKeyRetrieval=true&padCharsWithSpace=true"
        )
        database_user = "root"
    else:
        use_postgres = case_name in POSTGRES_CASES
        mappings = sorted(source_directory.glob("r2rml*.ttl"))
        mysql_mappings = [path for path in mappings if path.stem.endswith("-mysql")]
        if mysql_mappings and not use_postgres:
            mapping_source = mysql_mappings[0]
        else:
            mapping_source = next(
                path for path in mappings if not path.stem.endswith("-mysql")
            )
        expected_outputs = sorted(source_directory.glob("mapped*.nq"))
        expected_output = expected_outputs[0] if expected_outputs else None
        database_script = OFFICIAL_DATABASES_DIRECTORY / f"d{case_name[8:11]}.sql"
        if use_postgres:
            database_ready = load_postgres_database(database_script, database_log)
            database_dsn = (
                f"jdbc:postgresql://127.0.0.1:{postgres_port}/{DATABASE_NAME}"
            )
            database_user = "postgres"
        else:
            database_ready = load_mysql_database(
                DATABASE_NAME, database_script, database_log
            )
            database_dsn = (
                f"jdbc:mysql://127.0.0.1:{mysql_port}/{DATABASE_NAME}"
                "?allowPublicKeyRetrieval=true&padCharsWithSpace=true"
            )
            database_user = "root"

    mapping_path = case_directory / "mapping.ttl"
    shutil.copy2(mapping_source, mapping_path)
    if not database_ready:
        print(f"{case_label}: FAIL (database setup)", flush=True)
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
        str(mapping_path),
        "-dsn",
        database_dsn,
        "-u",
        database_user,
        "-p",
        DATABASE_PASSWORD,
        "-o",
        str(program_path),
        "-bt",
    ]
    translated = run_logged(translation_command, translation_log)
    if expected_output is None and not translated:
        remove_case_logs(case_directory)
        print(f"{case_label}: PASS", flush=True)
        return True
    if not translated:
        print(f"{case_label}: FAIL (translation)", flush=True)
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
        f"/work/cases/{catalog.slug}/{case_name}",
        "--entrypoint",
        "/souffle/bin/souffle",
        SOUFFLE_IMAGE,
        "-L",
        "/work",
        "Datalog_rules.rs",
    ]
    executed = run_logged(souffle_command, souffle_log)
    if expected_output is None and not executed:
        remove_case_logs(case_directory)
        print(f"{case_label}: PASS", flush=True)
        return True
    if not executed:
        print(f"{case_label}: FAIL (Soufflé execution)", flush=True)
        print_log(souffle_log)
        return False

    actual_output = materialize_output_nq(case_directory)
    if expected_output is None:
        print(f"{case_label}: FAIL (invalid mapping was accepted)", flush=True)
        return False

    difference = compare_output(expected_output, actual_output)
    if difference:
        print(f"{case_label}: FAIL (output comparison)", flush=True)
        sys.stdout.buffer.write(difference)
        sys.stdout.buffer.flush()
        return False

    remove_case_logs(case_directory)
    print(f"{case_label}: PASS", flush=True)
    return True


def remove_container(container: str) -> None:
    subprocess.run(
        ["docker", "rm", "--force", container],
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
        check=False,
    )


def stop_on_signal(signum, _frame) -> None:
    raise SystemExit(128 + signum)


def discover_test_catalogs() -> list[tuple[TestCatalog, list[Path]]] | None:
    discovered_catalogs = []
    for catalog in TEST_CATALOGS:
        case_directories = sorted(
            path
            for path in catalog.cases_directory.glob(catalog.case_pattern)
            if path.is_dir()
        )
        if len(case_directories) != catalog.expected_cases:
            print(
                f"Expected {catalog.expected_cases} {catalog.label} R2RML cases, "
                f"found {len(case_directories)}",
                file=sys.stderr,
            )
            return None
        discovered_catalogs.append((catalog, case_directories))
    return discovered_catalogs


def main() -> int:
    shutil.rmtree(BUILD_DIRECTORY, ignore_errors=True)
    discovered_catalogs = discover_test_catalogs()
    if discovered_catalogs is None:
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
            f"MYSQL_ROOT_PASSWORD={DATABASE_PASSWORD}",
            "--publish",
            "127.0.0.1::3306",
            MYSQL_IMAGE,
            "--sql-mode=ANSI_QUOTES,PAD_CHAR_TO_FULL_LENGTH",
        ],
        stdout=subprocess.DEVNULL,
        check=False,
    ).returncode:
        print("MySQL startup failed", file=sys.stderr)
        return 1

    if subprocess.run(
        [
            "docker",
            "run",
            "--detach",
            "--rm",
            "--name",
            POSTGRES_CONTAINER,
            "--env",
            f"POSTGRES_PASSWORD={DATABASE_PASSWORD}",
            "--publish",
            "127.0.0.1::5432",
            POSTGRES_IMAGE,
        ],
        stdout=subprocess.DEVNULL,
        check=False,
    ).returncode:
        print("PostgreSQL startup failed", file=sys.stderr)
        remove_container(MYSQL_CONTAINER)
        return 1

    signal.signal(signal.SIGTERM, stop_on_signal)
    try:
        for _ in range(60):
            mysql_ready = (
                subprocess.run(
                    [
                        "docker",
                        "exec",
                        "--env",
                        f"MYSQL_PWD={DATABASE_PASSWORD}",
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
            )
            postgres_ready = (
                subprocess.run(
                    [
                        "docker",
                        "exec",
                        POSTGRES_CONTAINER,
                        "pg_isready",
                        "--username=postgres",
                    ],
                    stdout=subprocess.DEVNULL,
                    stderr=subprocess.DEVNULL,
                    check=False,
                ).returncode
                == 0
            )
            if mysql_ready and postgres_ready:
                break
            time.sleep(1)
        else:
            subprocess.run(
                ["docker", "logs", MYSQL_CONTAINER],
                stdout=sys.stderr,
                stderr=sys.stderr,
                check=False,
            )
            subprocess.run(
                ["docker", "logs", POSTGRES_CONTAINER],
                stdout=sys.stderr,
                stderr=sys.stderr,
                check=False,
            )
            print("Databases did not become ready", file=sys.stderr)
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
        postgres_port = (
            subprocess.run(
                ["docker", "port", POSTGRES_CONTAINER, "5432/tcp"],
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

        total_passed = 0
        total_failed = 0
        for catalog, case_directories in discovered_catalogs:
            print(f"\n{catalog.label} catalog")
            catalog_passed = 0
            catalog_failed = 0
            for source_directory in case_directories:
                if run_case(
                    catalog,
                    source_directory,
                    classpath,
                    mysql_port,
                    postgres_port,
                    docker_user,
                ):
                    catalog_passed += 1
                else:
                    catalog_failed += 1

            print(
                f"\n{catalog.label}: {catalog_passed} passed, "
                f"{catalog_failed} failed, "
                f"{catalog_passed + catalog_failed} total"
            )
            total_passed += catalog_passed
            total_failed += catalog_failed

        print(
            f"\nAll catalogs: {total_passed} passed, {total_failed} failed, "
            f"{total_passed + total_failed} total"
        )
        return 1 if total_failed else 0
    finally:
        remove_container(MYSQL_CONTAINER)
        remove_container(POSTGRES_CONTAINER)


if __name__ == "__main__":
    raise SystemExit(main())
