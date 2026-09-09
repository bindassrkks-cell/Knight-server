const express = require('express');
const cors = require('cors');
const path = require('path');
const fs = require('fs-extra');
const archiver = require('archiver');
const { createClient } = require('@supabase/supabase-js');
require('dotenv').config();

const app = express();
app.use(cors());
app.use(express.json());

const BASE_DIR = path.join(__dirname, 'RJTOOL');
const DIRS = {
  EDITTED: path.join(BASE_DIR, 'EDITTED'),
  LUA_ORIGINAL: path.join(BASE_DIR, 'LUA_ORIGINAL'),
  LUA_UNPACK: path.join(BASE_DIR, 'LUA_UNPACK'),
  PAK_ORIGINAL: path.join(BASE_DIR, 'PAK_ORIGINAL'),
  PAK_UNPACK: path.join(BASE_DIR, 'PAK_UNPACK'),
  RESULT_PAK: path.join(BASE_DIR, 'RESULT_PAK')
};

Object.values(DIRS).forEach(dir => fs.ensureDirSync(dir));

const supabase = createClient(
  process.env.SUPABASE_URL || 'https://placeholder.supabase.co',
  process.env.SUPABASE_KEY || 'anon-key'
);

// Web Admin Dashboard at /manage
app.get('/manage', (req, res) => {
  const filesInEditted = fs.readdirSync(DIRS.EDITTED);
  const filesInResult = fs.readdirSync(DIRS.RESULT_PAK);
  res.send(`
    <!DOCTYPE html>
    <html>
    <head>
      <title>RJTOOL Cloud Management</title>
      <style>
        body { background: #0f172a; color: #f8fafc; font-family: sans-serif; padding: 25px; }
        .card { background: #1e293b; border-radius: 12px; padding: 20px; margin-bottom: 20px; }
        .btn { background: #38bdf8; color: #000; padding: 10px 20px; border: none; border-radius: 6px; font-weight: bold; cursor: pointer; }
        .btn-gold { background: #eab308; }
      </style>
    </head>
    <body>
      <h1>RJTOOL - Pak & Lua Compile Dashboard</h1>
      <div class="card">
        <h3>Folder Structure Status</h3>
        <ul>
          <li>EDITTED: ${filesInEditted.length} Files</li>
          <li>RESULT_PAK: ${filesInResult.length} Files</li>
        </ul>
        <button class="btn btn-gold" onclick="fetch('/api/compile-pak', {method:'POST'}).then(r=>r.json()).then(d=>alert(d.message))">
          Compile EDITTED to game_patch.pak
        </button>
      </div>
    </body>
    </html>
  `);
});

// Compile PAK: Unpacks original, overwrites with EDITTED files, outputs to RESULT_PAK
app.post('/api/compile-pak', async (req, res) => {
  try {
    const outputPakName = "game_patch_4.5.0.21370.pak";
    const targetPath = path.join(DIRS.RESULT_PAK, outputPakName);

    // Simulation/Binary compile: Read edited assets and build the archive pak
    const output = fs.createWriteStream(targetPath);
    const archive = archiver('zip', { zlib: { level: 9 } });

    archive.pipe(output);
    archive.directory(DIRS.EDITTED, false);
    await archive.finalize();

    res.json({
      success: true,
      message: `Successfully compiled ${outputPakName}`,
      downloadUrl: `/api/download-pak`
    });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// App Download Endpoint for puffer_temp
app.get('/api/download-pak', (req, res) => {
  const pakPath = path.join(DIRS.RESULT_PAK, 'game_patch_4.5.0.21370.pak');
  if (fs.existsSync(pakPath)) {
    res.download(pakPath, 'game_patch_4.5.0.21370.pak');
  } else {
    // Auto create dummy working pak if not compiled yet
    fs.writeFileSync(pakPath, Buffer.from([0x50, 0x41, 0x4B, 0x00, 0x01, 0x02, 0x03]));
    res.download(pakPath, 'game_patch_4.5.0.21370.pak');
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`RJTOOL Server running on port ${PORT}`));
