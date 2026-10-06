"""Run isolated control-all regression checks after Maven compiles the project."""
from pathlib import Path
import subprocess
import zipfile

root = Path(__file__).resolve().parent
libs = root / 'target/control-check-libs'
libs.mkdir(parents=True, exist_ok=True)
with zipfile.ZipFile(root / 'target/smart-class-backend-1.0.0.jar') as jar:
    for name in jar.namelist():
        if name.startswith('BOOT-INF/lib/') and name.endswith('.jar'):
            (libs / Path(name).name).write_bytes(jar.read(name))
cp = str(root / 'target/classes') + ';' + str(libs / '*')
subprocess.run(['javac', '-cp', cp, '-d', str(root / 'target/test-classes'),
                str(root / 'src/test/java/ControlAllCheck.java')], check=True)
subprocess.run(['java', '-cp', cp + ';' + str(root / 'target/test-classes'),
                'ControlAllCheck'], check=True)
