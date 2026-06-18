let rooms = [];
let ezs = [];

function updateThemes() {
    const v = document.getElementById('version').value;
    const sel = document.getElementById('theme');
    sel.innerHTML = '';
    (themes[v] || []).forEach(t => {
        const opt = document.createElement('option');
        opt.value = t; opt.textContent = t;
        if (t === 'stone') opt.selected = true;
        sel.appendChild(opt);
    });
}

function updateSize() {
    const w = parseInt(document.getElementById('width').value) || 0;
    const d = parseInt(document.getElementById('depth').value) || 0;
    const h = parseInt(document.getElementById('height').value) || 0;
    const cw = parseInt(document.getElementById('corridorWidth').value) || 0;
    const ww = parseInt(document.getElementById('wallWidth').value) || 0;
    const ceil = document.getElementById('ceiling').checked;
    const tx = w * (cw + ww) + ww;
    const tz = d * (cw + ww) + ww;
    const ty = h + (ceil ? 2 : 1);
    document.getElementById('size-display').textContent = `Taille totale: ${tx} x ${ty} x ${tz} (blocs)`;
}

function addRoom() {
    const r = {
        x: document.getElementById('room_x').value,
        z: document.getElementById('room_z').value,
        w: document.getElementById('room_w').value,
        d: document.getElementById('room_d').value,
        e: document.getElementById('room_e').value
    };
    rooms.push(r);
    updateRooms();
}

function updateRooms() {
    const list = document.getElementById('room_list'); list.innerHTML = '';
    const inputs = document.getElementById('room_inputs'); inputs.innerHTML = '';
    rooms.forEach((r, i) => {
        const div = document.createElement('div'); div.className = 'list-item';
        div.innerHTML = `<span>Salle: [${r.x}, ${r.z}] ${r.w}x${r.d} (${r.e} entrées)</span> <button class='remove-btn' type='button' onclick='rooms.splice(${i},1);updateRooms()'>X</button>`;
        list.appendChild(div);
        ['x', 'z', 'w', 'd', 'e'].forEach(k => {
            const inp = document.createElement('input'); inp.type = 'hidden'; inp.name = 'room_' + k; inp.value = r[k];
            inputs.appendChild(inp);
        });
    });
}

function addEZ() {
    const ez = {
        x: document.getElementById('ez_x').value,
        z: document.getElementById('ez_z').value,
        w: document.getElementById('ez_w').value,
        d: document.getElementById('ez_d').value,
        f: document.getElementById('ez_f').value
    };
    ezs.push(ez);
    updateEZs();
}

function updateEZs() {
    const list = document.getElementById('ez_list'); list.innerHTML = '';
    const inputs = document.getElementById('ez_inputs'); inputs.innerHTML = '';
    ezs.forEach((ez, i) => {
        const div = document.createElement('div'); div.className = 'list-item';
        div.innerHTML = `<span>Erosion: [${ez.x}, ${ez.z}] ${ez.w}x${ez.d} (F: ${ez.f})</span> <button class='remove-btn' type='button' onclick='ezs.splice(${i},1);updateEZs()'>X</button>`;
        list.appendChild(div);
        ['x', 'z', 'w', 'd', 'f'].forEach(k => {
            const inp = document.createElement('input'); inp.type = 'hidden'; inp.name = 'ez_' + k; inp.value = ez[k];
            inputs.appendChild(inp);
        });
    });
}

function startGeneration() {
    const form = document.getElementById('genForm');
    const formData = new FormData(form);
    const params = new URLSearchParams();
    for (const pair of formData) params.append(pair[0], pair[1]);

    document.getElementById('progress-container').style.display = 'block';
    document.getElementById('progress-fill').style.width = '0%';
    document.getElementById('progress-task').textContent = 'Initialisation...';

    fetch('/generate', { method: 'POST', body: params })
        .then(r => r.json())
        .then(data => {
            if (data.status === 'started' && data.id) {
                startPolling(data.id);
            }
        });
}

let progInterval;
function startPolling(genId) {
    if (progInterval) clearInterval(progInterval);
    progInterval = setInterval(() => {
        fetch('/progress?id=' + genId).then(r => r.json()).then(data => {
            if (data.task) {
                document.getElementById('progress-task').textContent = data.task;
                const p = Math.round((data.current / data.total) * 100);
                document.getElementById('progress-fill').style.width = p + '%';
                document.getElementById('progress-text').textContent = p + '% (' + data.current + '/' + data.total + ')';

                if (data.finished) {
                    clearInterval(progInterval);
                    window.location.href = '/download?id=' + genId;
                    setTimeout(() => { document.getElementById('progress-container').style.display = 'none'; }, 3000);
                } else if (data.task.startsWith('Erreur:')) {
                    clearInterval(progInterval);
                    alert(data.task);
                }
            }
        });
    }, 500);
}

document.addEventListener('DOMContentLoaded', () => {
    updateThemes();
    updateSize();
});
