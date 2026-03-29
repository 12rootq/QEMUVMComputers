$src = 'C:\dev_forge\build\reobfShadowJar\output.jar'
$dst = 'C:\Users\artem\AppData\Roaming\PrismLauncher\instances\test 1.20.1 forge\minecraft\mods\newvmcomputers-1.5-1.20.1.jar'

Copy-Item -LiteralPath $src -Destination $dst -Force
Get-FileHash -LiteralPath $dst -Algorithm SHA256 | Select-Object -ExpandProperty Hash
