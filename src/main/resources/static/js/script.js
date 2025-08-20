const fileInput = document.getElementById('fileInput');
const selectBtn = document.getElementById('selectBtn');
const uploadArea = document.getElementById('uploadArea');
const fileRow = document.getElementById('fileRow');
const fileNameEl = document.getElementById('fileName');
const fileMeta = document.getElementById('fileMeta');
const removeBtn = document.getElementById('removeBtn');
const validationChips = document.getElementById('validationChips');

const progress = document.getElementById('progress');
const startBtn = document.getElementById('startBtn');
const summary = document.getElementById('summary');
const downloadBtn = document.getElementById('downloadBtn');

let processInterval;
let currentFile = null;

// Helpers
const kb = v => v / 1024;
const mb = v => kb(v) / 1024;
const fmtSize = b => (mb(b) < 1 ? `${kb(b).toFixed(0)} KB` : `${mb(b).toFixed(2)} MB`);

const ACCEPT_EXT = ['.wav', '.mp3', '.m4a'];
const MAX_BYTES = 5 * 1024 * 1024; // 5 MB

function getExt(name) {
  const dot = name.lastIndexOf('.');
  return dot >= 0 ? name.slice(dot).toLowerCase() : '';
}

function evaluateFile(file) {
  const ext = getExt(file.name);
  const sizeOk = file.size <= MAX_BYTES;
  const formatOk = ACCEPT_EXT.includes(ext);
  return { formatOk, sizeOk };
}

function renderChips({ formatOk, sizeOk }) {
  validationChips.innerHTML = '';
  validationChips.style.display = 'flex';

  const chipFormat = document.createElement('div');
  chipFormat.className = `chip ${formatOk ? 'ok' : 'bad'}`;
  chipFormat.innerHTML = `${formatOk ? '✔' : '✕'} Formato (${ACCEPT_EXT.join(', ')})`;

  const chipSize = document.createElement('div');
  chipSize.className = `chip ${sizeOk ? 'ok' : 'bad'}`;
  chipSize.innerHTML = `${sizeOk ? '✔' : '✕'} Tamanho (≤ 5 MB)`;

  validationChips.appendChild(chipFormat);
  validationChips.appendChild(chipSize);
}

function enableStart(enabled) {
  startBtn.disabled = !enabled;
  startBtn.classList.toggle('btn-primary', enabled);
  startBtn.classList.toggle('btn-disabled', !enabled);
}

function resetUI() {
  currentFile = null;
  fileInput.value = '';
  fileRow.style.display = 'none';
  validationChips.style.display = 'none';
  progress.style.width = '0%';
  summary.value = '';
  downloadBtn.disabled = true;
  downloadBtn.classList.add('btn-disabled');
  downloadBtn.classList.remove('btn-primary');
  enableStart(false);
}

// Interações
selectBtn.addEventListener('click', (e) => {
  e.stopPropagation();
  fileInput.click();
});

// Clicar na área abre o seletor (menos quando o clique vem de elementos internos com stopPropagation)
uploadArea.addEventListener('click', () => fileInput.click());

fileInput.addEventListener('change', handleFile);

uploadArea.addEventListener('dragover', (e) => {
  e.preventDefault();
  uploadArea.classList.add('dragover');
});
uploadArea.addEventListener('dragleave', () => uploadArea.classList.remove('dragover'));
uploadArea.addEventListener('drop', (e) => {
  e.preventDefault();
  uploadArea.classList.remove('dragover');
  if (e.dataTransfer.files.length) {
    fileInput.files = e.dataTransfer.files;
    handleFile();
  }
});

removeBtn.addEventListener('click', (e) => {
  e.stopPropagation();
  resetUI();
});

// IMPORTANTE: impedir que o clique no botão dispare o clique da upload-area
startBtn.addEventListener('click', async (e) => {
  e.stopPropagation();
  if (!currentFile) return;

  const formData = new FormData();
  formData.append('audioFile', currentFile);
  formData.append('summarize', summarizeSwitch.checked);

  enableStart(false); // bloqueia botão durante o processamento
  progress.style.width = '0%';

  // 1. Inicia processamento no servidor
  const resp = await fetch('/process', { method: 'POST', body: formData });
  const { taskId } = await resp.json();

  if (!taskId) {
    console.error('Nenhum taskId retornado pelo backend. Abortando polling.');
    return; // sai da função, não faz polling
  }

  // 2. Exibir step "Transcrevendo"
  setActiveStep(1);

  // 3. Polling para status
  const interval = setInterval(async () => {
    const statusResp = await fetch(`/status/${taskId}`);
    const data = await statusResp.json();

    if(data.status === 'PROCESSANDO') setActiveStep(1);
    else if(data.status === 'GERANDO') setActiveStep(2);
    else if(data.status === 'IMPRESSOES') {
      if(summarizeSwitch.checked) {
        document.getElementById('impressaoStep').style.display = 'flex';
        setActiveStep(3);
      }
    }
    else if(data.status === 'CONCLUIDO') {
      if(summarizeSwitch.checked) document.getElementById('concluidoStep').style.display = 'flex';
      setActiveStep(summarizeSwitch.checked ? 4 : 3);
      summary.value = data.summary || '';
      downloadBtn.disabled = false;
      downloadBtn.classList.remove('btn-disabled');
      downloadBtn.classList.add('btn-primary');
      clearInterval(interval);
    }
  }, 1500);
});

function setActiveStep(index){
  const steps = document.querySelectorAll('.steps .step');
  steps.forEach((s, i) => s.classList.toggle('active', i === index));
}

downloadBtn.addEventListener('click', () => {
  if (!summary.value) return;
  const blob = new Blob([summary.value], { type: 'text/plain;charset=utf-8' });
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = (currentFile?.name?.replace(/\.[^.]+$/, '') || 'transcricao') + '.txt';
  document.body.appendChild(a);
  a.click();
  URL.revokeObjectURL(a.href);
  a.remove();
});

function handleFile() {
  if (!fileInput.files.length) return;
  const file = fileInput.files[0];
  currentFile = file;

  // Mostrar linha com nome e meta
  fileRow.style.display = 'flex';
  fileNameEl.textContent = file.name; // elipse é via CSS
  fileMeta.textContent = `${(file.type || 'audio')} • ${fmtSize(file.size)}`;

  // Avaliar validações
  const result = evaluateFile(file);
  renderChips(result);

  // Habilitar/desabilitar processamento
  const canStart = result.formatOk && result.sizeOk;
  enableStart(canStart);

  // Feedback rápido no contêiner
  uploadArea.style.borderColor = canStart ? 'var(--chip-ok)' : 'var(--chip-bad)';
  uploadArea.style.background = canStart ? '#f0fdf4' : '#fef2f2';
  setTimeout(() => { // animação suave retornando ao normal
    uploadArea.style.borderColor = '';
    uploadArea.style.background = '';
  }, 450);
}