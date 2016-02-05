set gui=-gui
echo %gui%

echo "%1"
if "%1"=="" (
	start "" %~dp0\gui\FireSim
	goto sim
)

echo "%1"
if "%1"=="-non-gui" (
	set gui=""
	goto sim
)

:sim
start javaw -cp bushfire-1.0.1-SNAPSHOT.jar io.github.agentsoz.bushfire.BushfireMain -c scenarios/halls_gap/halls_gap.xml -l halls-gap.log -level INFO %gui%