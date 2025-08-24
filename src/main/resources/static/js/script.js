// --- Variáveis de UI ---
const fileInput = document.getElementById('fileInput');
const selectBtn = document.getElementById('selectBtn');
const uploadArea = document.getElementById('uploadArea');
const fileRow = document.getElementById('fileRow');
const fileNameEl = document.getElementById('fileName');
const fileMeta = document.getElementById('fileMeta');
const validationChips = document.getElementById('validationChips');

const progress = document.getElementById('progress');
const startBtn = document.getElementById('startBtn');
const summary = document.getElementById('summary');
const downloadBtn = document.getElementById('downloadBtn');
const switchEl = document.getElementById("summarizeSwitch");
const resumoStep = document.getElementById("resumoStep");
const situationEl = document.querySelector(".summary-title span:last-child");
const iconEl = document.querySelector(".summary-title .material-icons");

let currentFile = null;
let currentTaskId = null;
let pollingInterval = null;
let downloadLink = null;

// --- Helpers ---
const kb = v => v / 1024;
const mb = v => kb(v) / 1024;
const fmtSize = b => (mb(b) < 1 ? `${kb(b).toFixed(0)} KB` : `${mb(b).toFixed(2)} MB`);
const ACCEPT_EXT = ['.wav', '.mp3', '.m4a'];
const MAX_BYTES = 5 * 1024 * 1024;

// --- Funções auxiliares ---
function getExt(name) { const dot = name.lastIndexOf('.'); return dot >= 0 ? name.slice(dot).toLowerCase() : ''; }
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
  chipFormat.innerHTML = `${formatOk ? '✔' : '✕'} Formato: mp3, m4a ou wav`;
  const chipSize = document.createElement('div');
  chipSize.className = `chip ${sizeOk ? 'ok' : 'bad'}`;
  chipSize.innerHTML = `${sizeOk ? '✔' : '✕'} Tamanho até 5 MB`;
  validationChips.appendChild(chipFormat);
  validationChips.appendChild(chipSize);
}
function enableStart(enabled) {
  startBtn.disabled = !enabled;
  startBtn.classList.toggle('btn-primary', enabled);
  startBtn.classList.toggle('btn-disabled', !enabled);
}
function setActiveStep(index) {
  const steps = document.querySelectorAll('.steps .step');
  steps.forEach((s, i) => s.classList.toggle('active', i === index));
}

// --- Reset de UI ---
function resetUI() {
  currentFile = null;
  currentTaskId = null;
  downloadLink = null;
  fileInput.value = '';
  fileRow.style.display = 'none';
  validationChips.style.display = 'none';
  progress.style.width = '0%';
  summary.textContent = '';
  summary.style.fontStyle = 'normal';
  summary.style.color = '';
  situationEl.textContent = "Aguardando";
  downloadBtn.disabled = true;
  downloadBtn.classList.add('btn-disabled');
  downloadBtn.classList.remove('btn-primary');
  toggleProcessingUI(false);
  enableStart(false);
  switchEl.disabled = false;
  setActiveStep(0);
  if (pollingInterval) { clearInterval(pollingInterval); pollingInterval = null; }
}

// --- Seleção de arquivo ---
selectBtn.addEventListener('click', e => { e.stopPropagation(); fileInput.click(); });
fileInput.addEventListener('change', handleFile);

uploadArea.addEventListener('dragover', e => { e.preventDefault(); uploadArea.classList.add('dragover'); });
uploadArea.addEventListener('dragleave', () => uploadArea.classList.remove('dragover'));
uploadArea.addEventListener('drop', e => {
  e.preventDefault();
  uploadArea.classList.remove('dragover');
  if (e.dataTransfer.files.length) { fileInput.files = e.dataTransfer.files; handleFile(); }
});

switchEl.addEventListener("change", () => { resumoStep.style.display = switchEl.checked ? "flex" : "none"; });

function handleFile() {
  if (!fileInput.files.length) return;
  const file = fileInput.files[0];
  currentFile = file;
  fileRow.style.display = 'flex';
  fileNameEl.textContent = file.name;
  fileMeta.textContent = `${(file.type || 'audio')} • ${fmtSize(file.size)}`;
  const result = evaluateFile(file);
  renderChips(result);
  enableStart(result.formatOk && result.sizeOk);
  uploadArea.style.borderColor = result.formatOk && result.sizeOk ? 'var(--chip-ok)' : 'var(--chip-bad)';
  uploadArea.style.background = result.formatOk && result.sizeOk ? '#f0fdf4' : '#fef2f2';
  setTimeout(() => { uploadArea.style.borderColor = ''; uploadArea.style.background = ''; }, 450);
}

// --- Processamento ---
startBtn.addEventListener('click', async e => {
  e.stopPropagation();
  if (!currentFile) return;

  const formData = new FormData();
  formData.append('audioFile', currentFile);
  formData.append('summarize', switchEl.checked);

  // Limpa summary e download
  summary.textContent = '';
  summary.style.fontStyle = 'normal';
  summary.style.color = '';
  downloadBtn.disabled = true;
  downloadBtn.classList.add('btn-disabled');
  downloadLink = null;

  // Se switch desligado, mostra frase em itálico imediatamente
  if (!switchEl.checked) {
    summary.textContent = "Não foi habilitado um resumo pra este processamento.";
    summary.style.fontStyle = 'italic';
    summary.style.color = '#64748b';
  }

  enableStart(false);
  switchEl.disabled = true;

  const resp = await fetch('/idox/process', { method: 'POST', body: formData });
  const data = await resp.json();
  if (!data.taskId) { console.error("Nenhum taskId retornado"); return; }
  currentTaskId = data.taskId;

  toggleProcessingUI(true);

  // Step inicial: Transcrevendo
  setActiveStep(1);

  pollingInterval = setInterval(async () => {
    const statusResp = await fetch(`/idox/status/${currentTaskId}`);
    const statusData = await statusResp.json();

    // Atualiza situação
    situationEl.textContent = statusData.situation || "Aguardando";

    // Atualiza download
    if (statusData.link) {
      downloadLink = statusData.link;
      downloadBtn.disabled = false;
      downloadBtn.classList.remove("btn-disabled");
      downloadBtn.classList.add("btn-primary");
    }

    // Atualiza resumo se switch ligado
    if (switchEl.checked && statusData.summary) {
      summary.textContent = statusData.summary;
      summary.style.fontStyle = 'normal';
      summary.style.color = '';
    }

    // Atualiza steps
    if (statusData.status === "PROCESSANDO") {
      if (statusData.situation.includes("Whisper")) setActiveStep(1);
      else if (statusData.situation.includes("Llama")) setActiveStep(2);
    } else if (statusData.status === "FINALIZADO" && statusData.situation === "Processamento finalizado.") {
      const lastStep = switchEl.checked ? 3 : 2; // último step depende do switch
      setActiveStep(lastStep);
      toggleProcessingUI(false);
      switchEl.disabled = false;
      clearInterval(pollingInterval);
      pollingInterval = null;
    }

  }, 1500);
});

// --- Cancelar ---
function toggleProcessingUI(isProcessing) {
  if (isProcessing) {
    startBtn.innerText = "Cancelar";
    startBtn.classList.add("cancel-mode");
    startBtn.disabled = false;
    startBtn.style.cursor = "pointer";
    startBtn.onclick = cancelProcess;

    downloadBtn.disabled = true;
    iconEl.innerHTML = `<div class="spinner"></div>`;
  } else {
    startBtn.innerText = "Iniciar Processamento";
    startBtn.classList.remove("cancel-mode");
    startBtn.onclick = null;
    startBtn.disabled = !currentFile;
    startBtn.style.cursor = currentFile ? "pointer" : "not-allowed";
    downloadBtn.disabled = !downloadLink;
    iconEl.innerHTML = `<span class="material-icons">graphic_eq</span>`;
  }
}

function cancelProcess() {
  if (!currentTaskId) { toggleProcessingUI(false); switchEl.disabled = false; return; }

  fetch(`http://localhost:8080/idox/cancel/${currentTaskId}`, { method: 'DELETE' })
    .finally(() => {
      if (pollingInterval) clearInterval(pollingInterval);
      pollingInterval = null;

      // Limpa apenas o estado de processamento
      toggleProcessingUI(false);
      switchEl.disabled = false;
      situationEl.textContent = "Aguardando";
      setActiveStep(0);
    });
}

// --- Download ---
downloadBtn.addEventListener('click', () => {
  if (!downloadLink) return;
  const a = document.createElement('a');
  a.href = downloadLink;
  a.download = (currentFile?.name?.replace(/\.[^.]+$/, '') || 'transcricao') + '.txt';
  document.body.appendChild(a);
  a.click();
  a.remove();
});
