// Referências aos elementos HTML
const fileUpload = document.getElementById('file-upload');
const fileInfo = document.getElementById('file-info');
const fileNameSpan = document.getElementById('file-name');
const uploadBtn = document.getElementById('upload-btn');
const downloadBtn = document.getElementById('download-btn');
const copyBtn = document.getElementById('copy-btn');
const summaryBox = document.getElementById('summary-box');
const toast = document.getElementById('toast');
const fileError = document.getElementById('file-error');
const loader = document.getElementById('loader');
const summaryTitle = document.getElementById('summary-title');
const container = document.querySelector('.container');

const summarizeSwitch = document.getElementById('use-ai-switch');
const removeFileBtn = document.querySelector('.remove-btn');
const aboutButton = document.querySelector('.about-button');

const badgeContainer = document.getElementById('badge-container');

const acceptedTypes = [
  'audio/mp3', 'audio/mpeg', 'audio/wav', 'audio/x-wav',
  'audio/mp4', 'audio/aac', 'audio/x-m4a', 'audio/ogg'
];

let isProcessing = false;
let currentFetchController = null;

// Reseta badges removendo a classe 'active'
function resetBadges() {
  const badges = badgeContainer.querySelectorAll('.badge.active');
  badges.forEach(badge => badge.classList.remove('active'));
}

// Controla visibilidade e interatividade do container de badges
function setBadgeContainerEnabled(enabled) {
  if (enabled) {
    badgeContainer.style.display = 'flex'; // mostra container
    badgeContainer.style.opacity = '1';
    badgeContainer.style.pointerEvents = 'auto'; // habilita clique
  } else {
    badgeContainer.style.display = 'none'; // esconde container
    badgeContainer.style.opacity = '0.5';
    badgeContainer.style.pointerEvents = 'none'; // desabilita clique
  }
}

// Atualiza a exibição do arquivo e o estado dos controles
function updateFileDisplay(file) {
  const isValid = acceptedTypes.includes(file.type);

  fileInfo.style.display = 'flex';
  fileNameSpan.textContent = file.name.length > 30 ? file.name.slice(0, 27) + '...' : file.name;
  fileNameSpan.title = file.name;

  fileInfo.className = 'file-info ' + (isValid ? 'valid' : 'invalid');
  fileError.style.display = isValid ? 'none' : 'block';
  fileError.textContent = isValid ? '' : 'Formatos aceitos: mp3, m4a, wav';

  uploadBtn.disabled = !isValid;
  uploadBtn.classList.toggle('disabled', !isValid);
  uploadBtn.textContent = isValid ? 'Analisar Áudio' : 'Analisar Áudio';

  summarizeSwitch.disabled = !isValid;
  if (removeFileBtn) removeFileBtn.disabled = false; // pode remover arquivo independente da validade

  resetBadges();

  // Mostrar badges somente se arquivo válido
  setBadgeContainerEnabled(isValid);
}

// Remove o arquivo e reseta a interface para estado inicial
function removeFile() {
  fileUpload.value = '';
  fileInfo.style.display = 'none';
  fileError.style.display = 'none';

  uploadBtn.disabled = true;           // desabilita botão
  uploadBtn.classList.add('disabled'); // aplica classe visual de botão inativo
  uploadBtn.classList.remove('cancel'); // garante que não tenha classe de cancelar
  uploadBtn.textContent = 'Analisar Áudio'; // texto padrão

  summarizeSwitch.disabled = true;
  if (removeFileBtn) removeFileBtn.disabled = true;

  fileNameSpan.textContent = '';
  fileInfo.className = 'file-info';

  resetUploadButton(false);

  resetBadges();
  setBadgeContainerEnabled(false);
}

// Mostra ou oculta o loader no botão de remover arquivo
function showRemoveLoader(show) {
  if (!removeFileBtn) return;

  if (show) {
    removeFileBtn.innerHTML = '<div class="loader-file"></div>';
    fileInfo.className = 'file-info processing';
    removeFileBtn.style.cursor = 'default';
  } else {
    removeFileBtn.innerHTML = '<span class="material-icons">close</span>';
    removeFileBtn.style.cursor = 'pointer';
  }
}

// Reseta o botão de upload para estado inicial
function resetUploadButton(enableControls = true) {
  isProcessing = false;
  loader.style.display = 'none';
  uploadBtn.disabled = true; // botão desabilitado no reset (igual ao estado inicial)
  uploadBtn.classList.add('disabled'); // aplicar visual de botão desabilitado
  uploadBtn.classList.remove('cancel');
  uploadBtn.textContent = "Analisar Áudio";
  if (enableControls) {
    setControlsDisabled(false);
  }
  showRemoveLoader(false);
}

// Toggle da seleção das badges com limite de 3 selecionadas
function onBadgeClick(event) {
  if (!event.target.classList.contains('badge')) return;

  const activeBadges = badgeContainer.querySelectorAll('.badge.active');
  const isActive = event.target.classList.contains('active');

  if (!isActive && activeBadges.length >= 1) {
    // Limite de 3 badges selecionados
    return;
  }

  event.target.classList.toggle('active');
}

// Eventos

fileUpload.addEventListener('change', () => {
  if (fileUpload.files.length > 0) {
    updateFileDisplay(fileUpload.files[0]);
  }
});

removeFileBtn?.addEventListener('click', () => {
  removeFile();
});

uploadBtn.addEventListener('click', () => {
  if (isProcessing) {
    if (currentFetchController) {
      currentFetchController.abort();
    }
    resetUploadButton();
    return;
  }

  const file = fileUpload.files[0];
  const summarize = summarizeSwitch.checked;

  if (!file || !acceptedTypes.includes(file.type)) return;

  isProcessing = true;

  uploadBtn.classList.add('cancel');
  uploadBtn.classList.remove('disabled');
  uploadBtn.textContent = "Cancelar";

  setControlsDisabled(true);
  showRemoveLoader(true);

  loader.style.display = 'inline-block';
  summaryBox.innerText = '';
  summaryTitle.innerText = `Processando: ${file.name}`;
  downloadBtn.disabled = true;
  copyBtn.disabled = true;

  const formData = new FormData();
  formData.append('audioFile', file);
  formData.append('summarize', summarize);

  // Adiciona os badges ativos ao FormData
  const activeBadges = Array.from(badgeContainer.querySelectorAll('.badge.active')).map(b => b.dataset.badge);
  activeBadges.forEach(badge => formData.append('badges', badge));

  currentFetchController = new AbortController();

  fetch('http://localhost:8080/idox/process', {
    method: 'POST',
    body: formData,
    signal: currentFetchController.signal
  })
    .then(res => res.json())
    .then(data => {
      isProcessing = false;
      fileInfo.className = 'file-info valid';
      uploadBtn.classList.remove('cancel');
      uploadBtn.textContent = 'Refazer';
      uploadBtn.disabled = false;
      uploadBtn.classList.remove('disabled');

      setControlsDisabled(false);
      showRemoveLoader(false);

      loader.style.display = 'none';
      copyBtn.disabled = false;
      downloadBtn.disabled = false;

      if (data.summarize && data.summary) {
        summaryBox.innerText = data.summary;
        summaryTitle.innerText = `Resumo do áudio: ${file.name}`;
      } else {
        summaryBox.innerText = '';
        summaryTitle.innerText = `Transcrição do áudio: ${file.name}`;
      }

      downloadBtn.onclick = () => {
        const downloadUrl = `http://localhost:8080${data.textFileLink}`;
        const a = document.createElement('a');
        a.href = downloadUrl;
        a.download = data.textFileLink.split('/').pop();
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
      };
    })
    .catch(err => {
      if (err.name === 'AbortError') {
        console.log('Upload cancelado.');
      } else {
        alert('Erro ao processar o áudio. Tente novamente.');
      }
      resetUploadButton();
    });
});

copyBtn.addEventListener('click', () => {
  const text = summaryBox.innerText.trim();
  if (!text) return;

  navigator.clipboard.writeText(text).then(() => {
    toast.classList.add('show');
    setTimeout(() => toast.classList.remove('show'), 3000);
  });
});

function toggleAboutCard() {
  const card = document.getElementById('about-card');
  card.style.display = card.style.display === 'block' ? 'none' : 'block';
}

document.addEventListener('click', function (event) {
  const card = document.getElementById('about-card');
  const button = document.querySelector('.about-button');
  if (!card.contains(event.target) && !button.contains(event.target)) {
    card.style.display = 'none';
  }
});

// Drag & Drop

const dropZone = document.getElementById('drop-zone');

dropZone.addEventListener('dragover', event => {
  event.preventDefault();
  dropZone.classList.add('dragover');
});

dropZone.addEventListener('dragleave', event => {
  dropZone.classList.remove('dragover');
});

dropZone.addEventListener('drop', event => {
  dropZone.classList.remove('dragover');
  handleDrop(event);
});

function handleDrop(event) {
  event.preventDefault();
  const droppedFile = event.dataTransfer.files[0];
  if (droppedFile) {
    fileUpload.files = event.dataTransfer.files;
    updateFileDisplay(droppedFile);
  }
}

// Badge click toggle

badgeContainer.querySelectorAll('.badge').forEach(badge => {
  badge.addEventListener('click', onBadgeClick);
});
