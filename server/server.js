const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');
const path = require('path');
const fs = require('fs-extra');
const multer = require('multer');
const { spawn } = require('child_process');
const { createClient } = require('@supabase/supabase-js');
require('dotenv').config();

const app = express();
app.use(cors());
app.use(express.json());

const server = http.createServer(app);
const io = new Server(server, { cors: { origin: "*" } });

const RJTOOL_DIR = path.join(__dirname, 'RJTOOL');
const DIRS = {
  EDITTED: path.join(RJTOOL_DIR, 'EDITTED'),
  PAK_ORIGINAL: path.join(RJTOOL_DIR, 'PAK_ORIGINAL'),
  RESULT_PAK: path.join(RJTOOL_DIR, 'RESULT_PAK')
};
Object.values(DIRS).forEach(d => fs.ensureDirSync(d));

const supabase = createClient(
  process.env.SUPABASE_URL || 'https://dummy.supabase.co',
  process.env.SUPABASE_KEY || 'dummy-key'
);

const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, req.params.type === 'original' ? DIRS.PAK_ORIGINAL : DIRS.EDITTED);
  },
  filename: (req, file, cb) => cb(null, file.originalname)
});
const upload = multer({ storage });

app.get('/manage', (req, res) => {
  const orig = fs.readdirSync(DIRS.PAK_ORIGINAL);
  const edit = fs.readdirSync(DIRS.EDITTED);
  const resFiles = fs.readdirSync(DIRS.RESULT_PAK);
  res.send(`
    <!DOCTYPE html>
    <html>
    <head>
      <title>RJTOOL Management</title>
      <script src="/socket.io/socket.io.js"></script>
      <style>
        body { background: #0b0e14; color: #fff; font-family: monospace; padding: 25px; }
        .card { background: #1a1f2c; padding: 18px; border-radius: 8px; margin-bottom: 16px; border: 1px solid #334155; }
        .btn { background: #38bdf8; padding: 8px 16px; border: none; border-radius: 4px; font-weight: bold; cursor: pointer; }
      </style>
    </head>
    <body>
      <h2>⚡ RJTOOL PAK MANAGER & WEBSOCKET</h2>
      <div class="card">
        <h3>1. Upload Original PAK</h3>
        <form action="/upload/original" method="post" enctype="multipart/form-data">
          <input type="file" name="file" />
          <button class="btn" type="submit">Upload Base PAK</button>
        </form>
        <p>In Folder: ${orig.join(', ') || 'None'}</p>
      </div>

      <div class="card">
        <h3>2. Upload Edited Assets (.uasset, .uexp, .lua)</h3>
        <form action="/upload/edited" method="post" enctype="multipart/form-data">
          <input type="file" name="file" multiple />
          <button class="btn" type="submit">Upload to EDITTED</button>
        </form>
        <p>In EDITTED: ${edit.join(', ') || 'None'}</p>
      </div>

      <div class="card">
        <h3>3. Result Output: ${resFiles.join(', ') || 'Not Compiled'}</h3>
        <button class="btn" style="background:#f59e0b;" onclick="socket.emit('start_custom_build', {trigger:'web'})">Trigger Full Repack</button>
      </div>
      <div id="logs" style="background:#000; color:#22c55e; padding:12px; height:150px; overflow-y:scroll;"></div>
      <script>
        const socket = io();
        socket.on('compile_log', l => {
          const el = document.getElementById('logs');
          el.innerHTML += l + '<br/>';
          el.scrollTop = el.scrollHeight;
        });
      </script>
    </body>
    </html>
  `);
});

app.post('/upload/:type', upload.single('file'), (req, res) => res.redirect('/manage'));

app.get('/api/configs', async (req, res) => {
  try {
    const { data, error } = await supabase.from('configs').select('*');
    if (error || !data || data.length === 0) {
      return res.json([{
        id: "1",
        title: "AIMBOT + ANTENA V3 NEW",
        description: "ALL VERSION - Small Crosshair, 120 FPS",
        image_url: "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=500",
        file_url: "/api/download-result-pak"
      }]);
    }
    res.json(data);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

app.get('/api/download-result-pak', (req, res) => {
  const outPath = path.join(DIRS.RESULT_PAK, 'game_patch_4.5.0.21370.pak');
  if (!fs.existsSync(outPath)) {
    fs.writeFileSync(outPath, Buffer.from([0x50, 0x41, 0x4B, 0x00, 0x01]));
  }
  res.download(outPath, 'game_patch_4.5.0.21370.pak');
});

io.on('connection', (sock) => {
  sock.on('start_custom_build', (data) => {
    sock.emit('compile_log', '[SOCKET] Initiating Unpack-Replace-Repack Pipeline...');
    const py = spawn('python3', [path.join(__dirname, 'rjtool_engine.py'), JSON.stringify(data)]);
    py.stdout.on('data', d => {
      const s = d.toString();
      sock.emit('compile_log', s);
      io.emit('compile_log', s);
    });
    py.stderr.on('data', d => sock.emit('compile_log', `[ERROR] ${d.toString()}`));
    py.on('close', c => sock.emit('compile_log', `[EXIT] Build completed with exit code ${c}`));
  });
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => console.log(`Server running on port ${PORT}`));
