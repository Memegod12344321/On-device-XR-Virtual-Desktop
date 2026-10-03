const mineflayer = require('mineflayer');
const readline = require('readline');

const host = process.env.MC_HOST || '127.0.0.1';
const port = Number(process.env.MC_PORT || 25565);
const username = process.env.MC_USERNAME || 'JumpBot';
const version = process.env.MC_VERSION || false;

const bot = mineflayer.createBot({
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

bot.on('kicked', reason => console.log('Kicked:', reason));
bot.on('error', err => console.error('Bot error:', err.message));
bot.on('end', () => console.log('Disconnected'));

const rl = readline.createInterface({ input: process.stdin, output: process.stdout });
rl.on('line', line => {
  const command = line.trim();

  if (command === 'jump') {
    bot.setControlState('jump', true);
    setTimeout(() => bot.setControlState('jump', false), 250);
  } else if (command === 'stop') {
    bot.quit('Stopping');
    process.exit(0);
  }
});
