@echo off
cd /d %~dp0

call mvn clean compile package -Duser.name=%1

del /Q discord-bcdicebot
mkdir discord-bcdicebot
copy target\discord-bcdicebot-jar-with-dependencies.jar discord-bcdicebot
copy start.bat discord-bcdicebot
copy validate.bat discord-bcdicebot
copy start.sh discord-bcdicebot
copy index.html discord-bcdicebot

cd discord-bcdicebot
mkdir originalDiceBots

rename discord-bcdicebot-jar-with-dependencies.jar discord-bcdicebot.jar
rename index.html README.html
copy ..\originalDiceBots originalDiceBots

jar -cvfM discord-bcdicebot.zip discord-bcdicebot.jar
jar -uvf discord-bcdicebot.zip validate.bat
jar -uvf discord-bcdicebot.zip start.bat
jar -uvf discord-bcdicebot.zip start.sh
jar -uvf discord-bcdicebot.zip README.html
jar -uvf discord-bcdicebot.zip originalDiceBots

cd ../
pause