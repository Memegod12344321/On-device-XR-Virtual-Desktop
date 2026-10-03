const mineflayer = require('mineflayer');

const host = process.env.MC_HOST || '127.0.0.1';
const port = Number(process.env.MC_PORT || 25565);
const username = process.env.MC_USERNAME || 'JumpBot';
const version = process.env.MC_VERSION || false;
const reconnectDelay = Number(process.env.MC_RECONNECT_DELAY || 10000);

let bot = null;
let stopping = false;
let reconnectTimer = null;

function connect() {
  if (stopping) return;

  console.log(`Connecting to ${host}:${port} as ${username}...`);

  bot = mineflayer.createBot({
    host,
    port,
    username,
    version,
    auth: 'offline'
  });

  bot.once('spawn', () => {
    console.log(`Joined ${host}:${port} as ${username}`);
    bot.setControlState('jump', true);
  });

  bot.on('kicked', reason => {
    console.log('Kicked:', reason);
  });

  bot.on('error', err => {
    console.error('Bot error:', err.message);
  });

  bot.on('end', () => {
    console.log(`Disconnected. Reconnecting in ${reconnectDelay / 1000}s...`);

    if (!stopping && !reconnectTimer) {
      reconnectTimer = setTimeout(() => {
        reconnectTimer = null;
        connect();
      }, reconnectDelay);
    }
  });
}

function stop() {
  stopping = true;

  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }

  if (bot) {
    try {
      bot.quit('Stopping');
    } catch {}
  }

  process.exit(0);
}

process.stdin.setEncoding('utf8');
process.stdin.on('data', data => {
  const command = data.trim().toLowerCase();

  if (command === 'stop') {
    stop();
  } else if (command === 'jump' && bot?.entity) {
    bot.setControlState('jump', true);
    setTimeout(() => {
      if (!stopping && bot) bot.setControlState('jump', false);
    }, 250);
  }
});

connect();
