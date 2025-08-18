document.addEventListener('DOMContentLoaded', () => {
    const fileInput = document.getElementById('file-upload');
    const fileInfo = document.getElementById('file-info');
    const fileNameElement = document.getElementById('file-name');
    const removeButton = document.querySelector('.remove-btn');
    const uploadButton = document.getElementById('upload-btn');
    const toggleSwitch = document.getElementById('use-ai-switch');
    const loaderTitle = document.getElementById('loader-title');
    const summaryContent = document.getElementById('summary-content');
    const downloadButton = document.getElementById('download-btn');
    const downloadIcon = document.getElementById('download-icon');
    const downloadText = document.getElementById('download-text');
    const errorList = document.getElementById('error-list');
    const noFileMessage = document.getElementById('no-file-message');
    const dropZone = document.getElementById('drop-zone');

    let isProcessing = false;

    // Função para lidar com a seleção de arquivo
    fileInput.addEventListener('change', handleFileUpload);

    // Função para remover o arquivo
    removeButton.addEventListener('click', removeFile);

    // Eventos de Drag and Drop
    dropZone.addEventListener('dragover', (event) => {
        event.preventDefault(); // Impede o comportamento padrão do navegador
        dropZone.classList.add('dragover'); // Adiciona o efeito visual
    });

    dropZone.addEventListener('dragleave', () => {
        dropZone.classList.remove('dragover'); // Remove o efeito visual
    });

    dropZone.addEventListener('drop', (event) => {
        event.preventDefault(); // Impede o comportamento padrão do navegador
        dropZone.classList.remove('dragover'); // Remove o efeito visual

        // Obtém o arquivo arrastado
        const files = event.dataTransfer.files;

        if (files.length > 0) {
            const file = files[0]; // Pega o primeiro arquivo arrastado

            // Simula o comportamento do input de arquivo
            const dataTransfer = new DataTransfer();
            dataTransfer.items.add(file);
            fileInput.files = dataTransfer.files;

            // Chama a função para processar o arquivo
            handleFileUpload({ target: fileInput });
        }
    });

    // Função para lidar com o clique no botão "Analisar Áudio"
    uploadButton.addEventListener('click', () => {
        console.log("Botão 'Analisar Áudio' clicado."); // Log de depuração

        if (isProcessing) {
            console.log("Cancelando processamento..."); // Log de depuração
            resetProcessingState();
        } else {
            console.log("Iniciando processamento..."); // Log de depuração
            startProcessing();
        }
    });

    function handleFileUpload(event) {
        const file = event.target.files[0]; // Obtém o arquivo selecionado

        if (!file) {
            console.error("Nenhum arquivo encontrado.");
            return;
        }

        console.log("Arquivo selecionado:", file.name); // Log para depuração

        // Limpa a lista de erros anteriores
        errorList.innerHTML = '';
        errorList.style.display = 'none';

        // Valida o arquivo e obtém os erros
        const errors = validateFile(file);

        if (errors.length === 0) {
            // Arquivo válido
            fileNameElement.textContent = file.name;

            // Mostra a área de informações do arquivo
            fileInfo.style.display = 'flex';

            // Adiciona a classe 'valid' para estilizar o arquivo válido
            fileInfo.classList.add('valid');
            fileInfo.classList.remove('invalid'); // Remove classe de erro, se existir

            // Habilita o botão de upload
            uploadButton.disabled = false;

            // Esconde a mensagem "Nenhum arquivo carregado"
            noFileMessage.style.display = 'none';
        } else {
            // Arquivo inválido: exibe os erros
            errors.forEach((error) => {
                const li = document.createElement('li');
                li.textContent = error;
                errorList.appendChild(li);
            });

            // Mostra a lista de erros
            errorList.style.display = 'block';

            // Atualiza o nome do arquivo no DOM
            fileNameElement.textContent = file.name;

            // Mostra a área de informações do arquivo
            fileInfo.style.display = 'flex';

            // Adiciona a classe 'invalid' para estilizar o erro
            fileInfo.classList.add('invalid');
            fileInfo.classList.remove('valid'); // Remove classe de sucesso, se existir

            // Desabilita o botão de upload
            uploadButton.disabled = true;

            // Esconde a mensagem "Nenhum arquivo carregado"
            noFileMessage.style.display = 'none';
        }
    }

    function removeFile() {
        // Remove o arquivo e limpa o input
        resetFileInput();

        // Esconde a área de informações do arquivo
        fileInfo.style.display = 'none';

        // Remove as classes 'valid' e 'invalid'
        fileInfo.classList.remove('valid', 'invalid');

        // Desabilita o botão de upload
        uploadButton.disabled = true;

        // Exibe a mensagem "Nenhum arquivo carregado"
        noFileMessage.style.display = 'block';
    }

    function validateFile(file) {
        const errors = [];
        const allowedTypes = ['audio/mpeg', 'audio/wav', 'audio/mp3'];
        const allowedExtensions = ['mp3', 'wav', 'm4a'];
        const maxSize = 5 * 1024 * 1024; // tamanho máximo 5 mb

        // Verifica o tipo do arquivo
        if (!allowedTypes.includes(file.type)) {
            const fileExtension = file.name.split('.').pop().toLowerCase();
            if (!allowedExtensions.includes(fileExtension)) {
                errors.push(`Formatos aceitos: mp3, m4a e wav.`);
            }
        }

        // Verifica o tamanho do arquivo
        if (file.size > maxSize) {
            errors.push(`Tamanho máximo permitido: 5 MB`);
        }

        return errors;
    }

    function resetFileInput() {
        // Limpa o input de arquivo
        fileInput.value = '';

        // Limpa a lista de erros
        errorList.innerHTML = '';
        errorList.style.display = 'none';

        // Exibe a mensagem "Nenhum arquivo carregado"
        noFileMessage.style.display = 'block';
    }

    function startProcessing() {
        console.log("Iniciando processamento..."); // Log de depuração

        // Verifica se há um arquivo válido
        if (!fileInfo.classList.contains('valid')) {
            alert("Por favor, carregue um arquivo válido antes de iniciar a análise.");
            console.error("Nenhum arquivo válido encontrado."); // Log de depuração
            return;
        }

        // Desabilita o botão de upload
        dropZone.classList.add('disabled');
        fileInput.disabled = true;

        // Altera o estado do arquivo
        fileInfo.classList.remove('valid');
        fileInfo.classList.add('processing');
        removeButton.disabled = true;
        removeButton.innerHTML = '<span class="material-icons">autorenew</span>'; // Substitui pelo spinner

        // Desabilita o switch
        toggleSwitch.disabled = true;

        // Altera o botão de "Analisar Áudio" para "Cancelar"
        uploadButton.textContent = 'Cancelar';
        uploadButton.classList.add('cancel');

        // Mostra o loader ao lado do título
        loaderTitle.style.display = 'inline-block';

        // Mostra a mensagem de processamento
        summaryContent.innerHTML = '<em style="color: #a0aec0;">Processando...</em>';

        // Altera o botão de download
        downloadText.textContent = 'Aguardando transcrição';
        downloadIcon.style.display = 'inline-block';
        downloadIcon.classList.add('loader-file');

        // Define o estado de processamento
        isProcessing = true;
    }

    function resetProcessingState() {
        console.log("Resetando estado de processamento..."); // Log de depuração

        // Habilita o botão de upload
        dropZone.classList.remove('disabled');
        fileInput.disabled = false;

        // Restaura o estado do arquivo
        fileInfo.classList.remove('processing');
        fileInfo.classList.add('valid');
        removeButton.disabled = false;
        removeButton.innerHTML = '<span class="material-icons">close</span>'; // Restaura o ícone de remoção

        // Habilita o switch
        toggleSwitch.disabled = false;

        // Restaura o botão de "Analisar Áudio"
        uploadButton.textContent = 'Analisar Áudio';
        uploadButton.classList.remove('cancel');

        // Esconde o loader ao lado do título
        loaderTitle.style.display = 'none';

        // Limpa a caixa de resumo
        summaryContent.innerHTML = '';

        // Restaura o botão de download
        downloadText.textContent = 'Baixar Transcrição em TXT';
        downloadIcon.style.display = 'none';
        downloadIcon.classList.remove('loader-file');

        // Define o estado de processamento
        isProcessing = false;
    }
});