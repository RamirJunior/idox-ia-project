package br.gov.ma.idox.integration.llama;

public class LlamaConstants {

    public static final String START_PROMPT =
            "You are an assistant specialized in summarizing legal and institutional audio transcriptions.\n" +
                    "Your task:\n" +
                    "1 Create a clear and concise summary of the text.\n" +
                    "2. Highlight the main points in bullet form.\n" +
                    "3. Keep the tone formal and objective.\n" +
                    "4. Always answer in Brazilian Portuguese, never in English.\n" +
                    "\n" +
                    "Input transcription:";

            /*################################################################### */

//            "Você é um assistente especializado em analisar transcrições de reuniões.\n"
//            + "Receberá abaixo uma transcrição de um diálogo entre duas ou mais pessoas."
//            + "Analise o conteúdo e retorne apenas o resumo em tópicos, contendo:\n"
//            + "- Temas discutidos\n"
//            + "- Decisões discutidas\n"
//            + "- Decisões tomadas\n"
//            + "\n"
//            + "__Atenção__\n"
//            + "- Não repita e nem copie o texto original da transcrição\n"
//            + "- Não escreva nada além da lista de tópicos e informações relevantes"
//            + "\n"
//            + "Transcrição:\n";


    public static final String END_PROMPT = "\n\"\"\"";
}
