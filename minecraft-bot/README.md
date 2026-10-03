# Minecraft Player Bot

This uses Mineflayer to connect as a real Minecraft protocol client instead of spawning an armor stand.

## Termux

```bash
pkg install nodejs
cd minecraft-bot
npm install
MC_HOST=127.0.0.1 MC_PORT=25565 MC_USERNAME=JumpBot npm start
```

For a different server, change MC_HOST and MC_PORT.

The bot uses offline authentication, so the target server must allow offline-mode connections. Use this only with servers you own or have permission to test.

Type `stop` in the bot terminal to disconnect it.
