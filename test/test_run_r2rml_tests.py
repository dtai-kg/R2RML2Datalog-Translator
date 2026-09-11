import importlib.util
import tempfile
import unittest
from pathlib import Path


MODULE_PATH = (
    Path(__file__).resolve().parent / "run-r2rml-tests.py"
)


def load_module():
    spec = importlib.util.spec_from_file_location("run_r2rml_tests", MODULE_PATH)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


class LoadPostgresDatabaseTests(unittest.TestCase):
    def test_retries_transient_errors(self):
        module = load_module()
        with tempfile.TemporaryDirectory() as temp_dir:
            temp_path = Path(temp_dir)
            script_path = temp_path / "db.sql"
            log_path = temp_path / "database.log"
            script_path.write_text("SELECT 1;", encoding="utf-8")

            call_number = 0
            sleep_calls = []

            def fake_run_logged(command, current_log_path, input_text=None):
                nonlocal call_number
                call_number += 1
                if call_number == 1:
                    current_log_path.write_text(
                        "FATAL: the database system is shutting down",
                        encoding="utf-8",
                    )
                    return False
                return True

            original_run_logged = module.run_logged
            original_sleep = module.time.sleep
            module.run_logged = fake_run_logged
            module.time.sleep = sleep_calls.append
            try:
                self.assertTrue(module.load_postgres_database(script_path, log_path))
            finally:
                module.run_logged = original_run_logged
                module.time.sleep = original_sleep

            self.assertEqual(call_number, 3)
            self.assertEqual(sleep_calls, [module.POSTGRES_SETUP_RETRY_DELAY_SECONDS])

    def test_does_not_retry_non_transient_error(self):
        module = load_module()
        with tempfile.TemporaryDirectory() as temp_dir:
            temp_path = Path(temp_dir)
            script_path = temp_path / "db.sql"
            log_path = temp_path / "database.log"
            script_path.write_text("SELECT 1;", encoding="utf-8")

            call_number = 0
            sleep_calls = []

            def fake_run_logged(command, current_log_path, input_text=None):
                nonlocal call_number
                call_number += 1
                current_log_path.write_text("syntax error at or near \"BAD\"", encoding="utf-8")
                return False

            original_run_logged = module.run_logged
            original_sleep = module.time.sleep
            module.run_logged = fake_run_logged
            module.time.sleep = sleep_calls.append
            try:
                self.assertFalse(module.load_postgres_database(script_path, log_path))
            finally:
                module.run_logged = original_run_logged
                module.time.sleep = original_sleep

            self.assertEqual(call_number, 1)
            self.assertEqual(sleep_calls, [])


if __name__ == "__main__":
    unittest.main()
