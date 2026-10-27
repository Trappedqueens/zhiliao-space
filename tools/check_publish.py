"""Validate and summarize the staged public snapshot without printing file contents."""
import hashlib
import json
import os
import re
import subprocess
from pathlib import Path

root = Path(__file__).resolve().parents[1]
files = subprocess.check_output(['git', 'ls-files', '-z'], cwd=root).decode('utf-8').strip('\0').split('\0')
blocked = []
findings = []
manifest = []
patterns = {
    'GitHub token': rb'(?:gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{30,})',
    'API secret': rb'sk-[A-Za-z0-9_-]{32,}',
    'Private key': rb'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----',
}
profile = os.environ.get('USERPROFILE')
if profile:
    patterns['Personal machine path'] = rb'(?:' + re.escape(profile.encode()) + rb'|' + re.escape(profile.replace('\\', '/').encode()) + rb')'
for name in files:
    if not name:
        continue
    if set(Path(name).parts) & {'data', 'target', '.venv', '.idea', 'node_modules', 'dist'} or name == 'deploy/nginx.conf' or name.startswith('deploy/nginx.original-'):
        blocked.append(name)
    data = subprocess.check_output(['git', 'show', ':' + name], cwd=root)
    if Path(name).suffix.lower() not in ('.png', '.ico'):
        for label, pattern in patterns.items():
            if re.search(pattern, data):
                findings.append({'file': name, 'kind': label})
    manifest.append({'path': name, 'bytes': len(data), 'sha256': hashlib.sha256(data).hexdigest()})
result = {'files': len(manifest), 'bytes': sum(item['bytes'] for item in manifest), 'blocked': blocked,
          'findings': findings, 'manifest': manifest}
(root / 'target').mkdir(exist_ok=True)
(root / 'target/publish-manifest.json').write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding='utf-8')
print(json.dumps({key: result[key] for key in ('files', 'bytes', 'blocked', 'findings')}, ensure_ascii=False))
if blocked or findings:
    raise SystemExit(1)
