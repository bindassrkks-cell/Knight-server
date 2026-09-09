const express = require('express');
const cors = require('cors');
const { createClient } = require('@supabase/supabase-js');
require('dotenv').config();

const app = express();
app.use(cors());
app.use(express.json());

const SUPABASE_URL = process.env.SUPABASE_URL || 'https://xyzcompany.supabase.co';
const SUPABASE_KEY = process.env.SUPABASE_KEY || 'public-anon-key';
const supabase = createClient(SUPABASE_URL, SUPABASE_KEY);

app.get('/api/settings', async (req, res) => {
  try {
    const { data, error } = await supabase.from('app_settings').select('*').limit(1).single();
    if (error) return res.status(200).json({ success: true, maxFps: 120, vip: true });
    res.json({ success: true, data });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

app.post('/api/custom-server', async (req, res) => {
  try {
    const { data, error } = await supabase.from('custom_server_profiles').insert([req.body]);
    if (error) throw error;
    res.json({ success: true, data });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`GFX Server live on port ${PORT}`));
