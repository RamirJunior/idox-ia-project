
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

const acceptedTypes = ['audio/mp3', 'audio/mpeg', 'audio/wav', 'audio/x-wav', 'audio/mp4', 'audio/aac', 'audio/x-m4a', 'audio/ogg'];

function updateFileDisplay(file) {
  const isValid = acceptedTypes.includes(file.type);
  fileInfo.style.display = 'flex';
  fileNameSpan.textContent = file.name.length > 30 ? file.name.slice(0, 27) + '...' : file.name;
  fileNameSpan.title = file.name;
  fileInfo.className = 'file-info ' + (isValid ? 'valid' : 'invalid');
  fileError.style.display = isValid ? 'none' : 'block';
  fileError.textContent = isValid ? '' : 'Formatos aceitos: mp3, m4a, wav';
  uploadBtn.disabled = !isValid;
  if (!isValid) {
    uploadBtn.innerText = 'Criar Resumo';
  }
}

fileUpload.addEventListener('change', () => {
  if (fileUpload.files.length > 0) {
    updateFileDisplay(fileUpload.files[0]);
  }
});

function handleDrop(event) {
  event.preventDefault();
  const droppedFile = event.dataTransfer.files[0];
  if (droppedFile) {
    fileUpload.files = event.dataTransfer.files;
    updateFileDisplay(droppedFile);
  }
}

function removeFile() {
  fileUpload.value = '';
  fileInfo.style.display = 'none';
  fileError.style.display = 'none';
  uploadBtn.disabled = true;
  fileNameSpan.textContent = '';
  fileInfo.className = 'file-info';
  uploadBtn.innerText = 'Criar Resumo';
}

uploadBtn.addEventListener('click', () => {
  const file = fileUpload.files[0];
  const summarizeSwitch = document.getElementById('use-ai-switch');
  const summarize = summarizeSwitch.checked;

  if (!file || !acceptedTypes.includes(file.type)) return;

  uploadBtn.disabled = true;
  uploadBtn.textContent = "Processando áudio...";
  loader.style.display = 'inline-block';
  summaryBox.innerText = '';
  summaryTitle.innerText = `Processando: ${file.name}`;
  downloadBtn.disabled = true;
  copyBtn.disabled = true;

  const formData = new FormData();
  formData.append('audioFile', file);
  formData.append('summarize', summarize); // 👈 Aqui incluímos o parâmetro booleano

  fetch('http://localhost:8080/idox/process', {
    method: 'POST',
    body: formData
  })
  .then(res => res.json())
  .then(data => {
    setTimeout(() => {
      fileInfo.className = 'file-info processed';
      uploadBtn.disabled = false;
      uploadBtn.innerText = 'Refazer';
      loader.style.display = 'none';
      copyBtn.disabled = false;
      downloadBtn.disabled = false;

      // Atualiza a caixa de resumo apenas se vier summary
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
        a.download = data.textFileLink.split('/').pop(); // nome sugerido
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
      };
    });
  })
  .catch(() => {
    loader.style.display = 'none';
    uploadBtn.disabled = false;
    alert('Erro ao processar o áudio. Tente novamente.');
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

// Fecha o card se clicar fora dele
document.addEventListener('click', function (event) {
  const card = document.getElementById('about-card');
  const button = document.querySelector('.about-button');
  if (!card.contains(event.target) && !button.contains(event.target)) {
    card.style.display = 'none';
  }
});