$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
$fixtureDirectory=Join-Path $projectRoot 'target\fixtures'
New-Item -ItemType Directory -Force $fixtureDirectory | Out-Null
Add-Type -AssemblyName System.Speech
$speechSynth=[System.Speech.Synthesis.SpeechSynthesizer]::new()
try {
    $englishVoice=$speechSynth.GetInstalledVoices() | Where-Object { $_.VoiceInfo.Culture.Name -like 'en-*' } | Select-Object -First 1
    if ($englishVoice) { $speechSynth.SelectVoice($englishVoice.VoiceInfo.Name) }
    $speechSynth.SetOutputToWaveFile((Join-Path $fixtureDirectory 'speech.wav'))
    $speechSynth.Speak('The meeting is on Friday at nine in the morning.')
} finally { $speechSynth.Dispose() }
Write-Host '已生成不含个人资料的语音测试文件。'
