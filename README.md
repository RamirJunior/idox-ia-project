# iDox ©

iDox é um sistema interno de transcrição e resumo de arquivos de áudio utilizando inteligência artificial local. Desenvolvido como um MVP para uso na Procuradoria Geral do Estado do Maranhão, o objetivo é acelerar a análise de conteúdo falado com foco em privacidade e agilidade.

## Descrição

O sistema permite que arquivos de áudio sejam transcritos automaticamente por meio do modelo Whisper da OpenAI, e posteriormente resumidos com o modelo LLaMA. Ambos os modelos rodam localmente em versões compiladas em C++ e são executados via chamadas com `ProcessBuilder`.

A aplicação foi desenvolvida com Java 17 e utiliza o ecossistema Spring Boot, com interface web baseada em Thymeleaf. Não há persistência de dados: os arquivos são processados temporariamente e removidos automaticamente por tarefas agendadas.

## Tecnologias Utilizadas

- Java 17
- Spring Boot
- Maven
- Thymeleaf
- Lombok
- Whisper (OpenAI) - transcrição de áudio
- LLaMA (Meta) - geração de resumo
- Execução de modelos via ProcessBuilder (C++ binários locais)
- Tarefas agendadas (`@Scheduled`) para limpeza de arquivos
- Execução assíncrona com `@Async` e controle de fila

## Características Técnicas

- Arquitetura baseada no padrão MVC.
- IA’s rodam localmente para garantir confidencialidade dos dados.
- Métodos de execução de IA são assíncronos (`@Async`) e controlados por fila para evitar sobrecarga.
- Sistema sem banco de dados: os arquivos são armazenados temporariamente no sistema de arquivos e excluídos periodicamente.
- Utilização de Scheduled Tasks para exclusão automática de arquivos após um período definido.
- Aplicação projetada para execução em ambiente Linux, com os binários de IA previamente compilados com os recursos dentro das disposições técnicas na PGE-MA.

## Requisitos

- Java 17
- Maven 3.x
- Whisper Open AI versão compilada em C++
- LLaMA versão compilada em C++

### Observação
- _Os módulos de IA - Whisper e Llama - foram removidos do repositório para fins de praticidade na documentação._
- _Serão documentados e anexados em breve bem como Screenshot da aplicação._

