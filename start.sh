#!/bin/sh

DISCORD_BOT_TOKEN=
BCDICE_API_URL=
IGNORE_ERROR=
# BCDICE_PASSWORD=PleaseChangeMeIfYouUseThis

java -jar discord-bcdicebot.jar "$DISCORD_BOT_TOKEN" "$BCDICE_API_URL" "$IGNORE_ERROR"
