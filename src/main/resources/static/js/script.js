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
const summaryStatus = document.getElementById('summaryStatus');
const downloadBtn = document.getElementById('downloadBtn');
const downloadIcon = document.getElementById('downloadIcon');
const downloadText = document.getElementById('downloadText');
const switchEl = document.getElementById("summarizeSwitch");
const resumoStep = document.getElementById("resumoStep");
const concluidoStep = document.getElementById("concluidoStep");
const iconEl = document.querySelector(".summary-title .material-icons");
const noSummaryMessage = document.getElementById("noSummaryMessage");
const statusMessage = document.getElementById("statusMessage");

let currentFile = null;
let currentTaskId = null;
let pollingInterval = null;
let downloadLink = null;
let isProcessing = false;
let isCancelling = false;
let cancelRequest = null;

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

function resetUI() {
  currentFile = null;
  currentTaskId = null;
  downloadLink = null;
  fileInput.value = '';
  fileRow.style.display = 'none';
  validationChips.style.display = 'none';
  progress.style.width = '0%';
  summary.textContent = '';
  summaryStatus.textContent = 'Aguardando';
  noSummaryMessage.style.display = 'none';
  downloadBtn.disabled = true;
  downloadBtn.classList.add('btn-disabled');
  downloadBtn.classList.remove('btn-primary');
  downloadIcon.textContent = 'description';
  downloadText.textContent = 'Baixar Transcrição';
  toggleProcessingUI(false);
  enableStart(false);
  setActiveStep(0);
  hideStatusMessage();

  if (pollingInterval) {
    clearInterval(pollingInterval);
    pollingInterval = null;
  }

  if (cancelRequest) {
    cancelRequest.abort();
    cancelRequest = null;
  }

  isProcessing = false;
  isCancelling = false;

  document.querySelectorAll('.disabled-element').forEach(el => {
    el.classList.remove('disabled-element');
  });

  uploadArea.classList.remove('canceling-state');
}

// File selection
selectBtn.addEventListener('click', e => { e.stopPropagation(); fileInput.click(); });
fileInput.addEventListener('change', handleFile);

uploadArea.addEventListener('dragover', e => { e.preventDefault(); uploadArea.classList.add('dragover'); });
uploadArea.addEventListener('dragleave', () => uploadArea.classList.remove('dragover'));
uploadArea.addEventListener('drop', e => {
  e.preventDefault();
  uploadArea.classList.remove('dragover');
  if (e.dataTransfer.files.length) {
    fileInput.files = e.dataTransfer.files;
    handleFile();
  }
});

removeBtn.addEventListener('click', e => { e.stopPropagation(); resetUI(); });

switchEl.addEventListener("change", () => {
  resumoStep.style.display = switchEl.checked ? "flex" : "none";
  concluidoStep.querySelector('.circle').textContent = switchEl.checked ? '4' : '3';
});

// Processing
startBtn.addEventListener('click', async e => {
  e.stopPropagation();
  if (!currentFile || isProcessing) return;

  const formData = new FormData();
  formData.append('audioFile', currentFile);
  formData.append('summarize', switchEl.checked);

  enableStart(false);
  progress.style.width = '0%';

  if (!switchEl.checked) {
    noSummaryMessage.style.display = 'block';
    summary.textContent = '';
  } else {
    noSummaryMessage.style.display = 'none';
  }

  document.getElementById('selectBtn').classList.add('disabled-element');
  document.getElementById('removeBtn').classList.add('disabled-element');
  document.getElementById('summarizeSwitch').parentElement.classList.add('disabled-element');

  try {
    const resp = await fetch('/idox/process', { method: 'POST', body: formData });
    const data = await resp.json();
    if (!data.taskId) {
      showStatusMessage("Erro ao iniciar processamento. Tente novamente.", "error");
      resetUIButKeepFile();
      return;
    }

    currentTaskId = data.taskId;
    isProcessing = true;
    toggleProcessingUI(true);
    setActiveStep(1);

    pollingInterval = setInterval(async () => {
      try {
        const statusResp = await fetch(`/idox/status/${currentTaskId}`);
        const statusData = await statusResp.json();

        if (statusData.situation) summaryStatus.textContent = statusData.situation;

        updateSteps(statusData);

        // Quando FINALIZADO, para polling e libera UI
        if (statusData.status === "FINALIZADO") {
          toggleProcessingUI(false);
          clearInterval(pollingInterval);
          pollingInterval = null;
          isProcessing = false;

          document.getElementById('selectBtn').classList.remove('disabled-element');
          document.getElementById('removeBtn').classList.remove('disabled-element');
          document.getElementById('summarizeSwitch').parentElement.classList.remove('disabled-element');
        }

        // Atualiza download e resumo
        if (statusData.link) {
          downloadLink = statusData.link;
          downloadBtn.disabled = false;
          downloadBtn.classList.remove("btn-disabled");
          downloadBtn.classList.add("btn-primary");
          downloadIcon.textContent = 'description';
          downloadText.textContent = 'Baixar Transcrição';
        }

        if (switchEl.checked && statusData.summary) {
          summary.textContent = statusData.summary;
          noSummaryMessage.style.display = 'none';
        }

      } catch (error) {
        console.error("Erro ao verificar status:", error);
      }
    }, 1500);



  } catch (error) {
    console.error("Erro ao iniciar processamento:", error);
    showStatusMessage("Erro ao iniciar processamento. Tente novamente.", "error");
    resetUIButKeepFile();
  }
});

function updateSteps(statusData) {
  const steps = document.querySelectorAll('.steps .step');

  // Step 1 sempre pintado
  steps[0].classList.add('completed');
  steps[0].classList.remove('active');

  // Step 2 pintado durante processamento
  steps[1].classList.add('active');

  // Step 3 só se tiver Llama e switch ativo
  if (switchEl.checked && statusData.situation?.includes("Llama")) {
    steps[1].classList.remove('active');
    steps[1].classList.add('completed');
    steps[2].classList.add('active');
  } else {
    steps[1].classList.remove('active');
    steps[1].classList.add('completed');
    steps[2].classList.remove('completed');
  }

  // Step 4 só se FINALIZADO
  if (statusData.status === "FINALIZADO") {
    steps.forEach(s => s.classList.add('completed'));
  }
}


function showStatusMessage(message, type) {
  statusMessage.textContent = message;
  statusMessage.className = `status-message status-${type}`;
  statusMessage.style.display = 'block';
  setTimeout(hideStatusMessage, 5000);
}

function hideStatusMessage() {
  statusMessage.style.display = 'none';
}

function toggleProcessingUI(isProcessing) {
  if (isProcessing) {
    startBtn.innerText = "Cancelar";
    startBtn.classList.add("cancel-mode");
    startBtn.onclick = cancelProcess;
    downloadBtn.disabled = true;
    downloadIcon.innerHTML = '<div class="spinner"></div>';
    downloadText.textContent = 'Aguardando transcrição...';
    iconEl.innerHTML = `<div class="spinner"></div>`;
  } else {
    startBtn.innerText = "Iniciar Processamento";
    startBtn.classList.remove("cancel-mode");
    startBtn.onclick = null;
    downloadBtn.disabled = downloadLink ? false : true;
    iconEl.innerHTML = `<span class="material-icons">graphic_eq</span>`;
  }
}

async function cancelProcess() {
  if (isCancelling) return;

  isCancelling = true;
  uploadArea.classList.add('canceling-state');
  startBtn.innerText = "Cancelando...";
  startBtn.disabled = true;
  showStatusMessage("Solicitando cancelamento...", "info");

  if (pollingInterval) {
    clearInterval(pollingInterval);
    pollingInterval = null;
  }

  try {
    cancelRequest = new XMLHttpRequest();

    return new Promise((resolve) => {
      cancelRequest.open('DELETE', `http://localhost:8080/idox/cancel/${currentTaskId}`, true);

      cancelRequest.onreadystatechange = function () {
        if (cancelRequest.readyState === 4) {
          if (cancelRequest.status === 200) {
            showStatusMessage("Processamento cancelado com sucesso.", "success");
          } else {
            showStatusMessage("Não foi possível cancelar o processamento.", "error");
          }

          setTimeout(() => {
            resetUIButKeepFile();
            isCancelling = false;
            cancelRequest = null;
            resolve();
          }, 1000);
        }
      };

      cancelRequest.onerror = function () {
        showStatusMessage("Erro de conexão ao tentar cancelar.", "error");
        setTimeout(() => {
          resetUIButKeepFile();
          isCancelling = false;
          cancelRequest = null;
          resolve();
        }, 1000);
      };

      cancelRequest.ontimeout = function () {
        showStatusMessage("Tempo limite excedido para cancelamento.", "warning");
        setTimeout(() => {
          resetUIButKeepFile();
          isCancelling = false;
          cancelRequest = null;
          resolve();
        }, 1000);
      };

      cancelRequest.timeout = 10000;
      cancelRequest.send();
    });
  } catch (error) {
    console.error("Erro ao executar cancelamento:", error);
    showStatusMessage("Erro inesperado ao tentar cancelar.", "error");
    setTimeout(() => {
      resetUIButKeepFile();
      isCancelling = false;
    }, 1000);
  }
}

function resetUIButKeepFile() {
  currentTaskId = null;
  downloadLink = null;
  progress.style.width = '0%';
  summary.textContent = '';
  summaryStatus.textContent = 'Aguardando';
  noSummaryMessage.style.display = 'none';
  downloadBtn.disabled = true;
  downloadBtn.classList.add('btn-disabled');
  downloadBtn.classList.remove('btn-primary');
  downloadIcon.textContent = 'description';
  downloadText.textContent = 'Baixar Transcrição';
  toggleProcessingUI(false);
  setActiveStep(0);
  isProcessing = false;

  document.getElementById('selectBtn').classList.remove('disabled-element');
  document.getElementById('removeBtn').classList.remove('disabled-element');
  document.getElementById('summarizeSwitch').parentElement.classList.remove('disabled-element');
  uploadArea.classList.remove('canceling-state');

  if (currentFile) {
    const result = evaluateFile(currentFile);
    enableStart(result.formatOk && result.sizeOk);
  }

  startBtn.disabled = false;
  startBtn.innerText = "Iniciar Processamento";
}

function setActiveStep(index) {
  const steps = document.querySelectorAll('.steps .step');
  steps.forEach((s, i) => {
    if (i <= index) {
      s.classList.add('completed');
    } else {
      s.classList.remove('completed');
    }
    s.classList.toggle('active', i === index);
  });
}



// Download
downloadBtn.addEventListener('click', () => {
  if (!downloadLink) return;
  const a = document.createElement('a');
  a.href = downloadLink;
  a.download = (currentFile?.name?.replace(/\.[^.]+$/, '') || 'transcricao') + '.txt';
  document.body.appendChild(a);
  a.click();
  a.remove();
});

// File handling
function handleFile() {
  if (!fileInput.files.length) return;
  const file = fileInput.files[0];
  currentFile = file;

  fileRow.style.display = 'flex';
  fileNameEl.textContent = file.name;
  fileMeta.textContent = `${(file.type || 'audio')} • ${fmtSize(file.size)}`;

  const result = evaluateFile(file);
  renderChips(result);

  const canStart = result.formatOk && result.sizeOk;
  enableStart(canStart);

  uploadArea.style.borderColor = canStart ? 'var(--chip-ok)' : 'var(--chip-bad)';
  uploadArea.style.background = canStart ? '#f0fdf4' : '#fef2f2';
  setTimeout(() => { uploadArea.style.borderColor = ''; uploadArea.style.background = ''; }, 450);
}