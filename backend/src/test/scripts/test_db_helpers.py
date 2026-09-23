"""Run with python3 -m unittest discover -s src/test/scripts (no MySQL required)."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

BACKEND = Path(__file__).resolve().parents[3]


class DatabaseHelpersTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="backend helpers ")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        for name in ("load-env.sh", "setup-db.sh", "run-backend.sh"):
            shutil.copy2(BACKEND / name, self.root / name)
        self.env = {k: v for k, v in os.environ.items()
                    if not k.startswith(("DB_", "SPRING_"))}
        self.env["PATH"] = str(self.root) + ":" + self.env["PATH"]
        self.write_executable("mvnw", '''#!/usr/bin/env bash
[[ "$PWD" == "$(dirname "$0")" || -f ./load-env.sh ]] || exit 1
printf '%s\\n' "$DB_NAME" "$DB_USERNAME" "$DB_PASSWORD" "$@"
''')
        self.write_executable("sudo", '''#!/usr/bin/env bash
[[ "$1" == -v ]] && exit 0
exec "$@"
''')
        self.write_executable("mysql", "#!/usr/bin/env bash\ncat > \"$SQL_CAPTURE\"\n")
        self.env["SQL_CAPTURE"] = str(self.root / "capture.sql")

    def write_executable(self, name, contents):
        path = self.root / name
        path.write_text(contents)
        path.chmod(0o755)

    def run_script(self, name):
        return subprocess.run([str(self.root / name)], env=self.env,
                              cwd="/tmp", text=True, capture_output=True)

    def test_defaults_and_launcher_directory(self):
        result = self.run_script("run-backend.sh")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(result.stdout.splitlines(), ["verified_career_marketplace",
                         "marketplace", "marketplace123", "spring-boot:run"])

    def test_literal_file_and_environment_precedence(self):
        (self.root / ".env").write_text("DB_NAME=from_file\r\nDB_PASSWORD=\"a&b$HOME'c\"\r\n")
        self.env["DB_NAME"] = "from_environment"
        result = self.run_script("run-backend.sh")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(result.stdout.splitlines()[:3],
                         ["from_environment", "marketplace", "a&b$HOME'c"])

    def test_missing_production_password_and_root_rejected(self):
        self.env["SPRING_PROFILES_ACTIVE"] = "prod"
        self.assertNotEqual(self.run_script("run-backend.sh").returncode, 0)
        self.env.update(DB_PASSWORD="test-only", DB_USERNAME="root")
        self.assertNotEqual(self.run_script("run-backend.sh").returncode, 0)

    def test_setup_quotes_password_and_limits_grant(self):
        self.env.update(DB_PASSWORD="test'\\&only", DB_NAME="local_demo")
        for _ in range(2):
            result = self.run_script("setup-db.sh")
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertNotIn(self.env["DB_PASSWORD"], result.stdout + result.stderr)
        sql = (self.root / "capture.sql").read_text()
        self.assertIn("IDENTIFIED BY 'test''\\&only'", sql)
        self.assertIn("CREATE DATABASE IF NOT EXISTS `local_demo`", sql)
        self.assertIn("GRANT ALL PRIVILEGES ON `local\\_demo`.*", sql)
        self.assertNotIn("ON *.*", sql)
        self.assertIn("ALTER USER 'marketplace'@'localhost'", sql)

    def test_invalid_identifier_and_remote_setup_rejected(self):
        self.env["DB_NAME"] = "invalid`;DROP DATABASE mysql"
        self.assertNotEqual(self.run_script("setup-db.sh").returncode, 0)
        self.env.update(DB_NAME="demo", DB_HOST="remote.example")
        self.assertNotEqual(self.run_script("setup-db.sh").returncode, 0)
        self.assertFalse((self.root / "capture.sql").exists())

    def test_malformed_env_does_not_echo_secret(self):
        (self.root / ".env").write_text("not-an-assignment-secret\n")
        result = self.run_script("run-backend.sh")
        self.assertNotEqual(result.returncode, 0)
        self.assertNotIn("not-an-assignment-secret", result.stderr)
