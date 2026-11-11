package com.example.data

import kotlin.random.Random

object SarcasmManager {
    enum class SarcasmLevel {
        SEM,
        MODERADO,
        MUITO
    }

    private val phrasesModerated = listOf(
        "Gosto musical é igual a braço: uns têm, outros... bom, ouvem isso aí.",
        "Seu bom gosto foi passear e esqueceu de voltar pelo visto.",
        "Ah, essa música... Ótima para lembrar que o silêncio é subestimado.",
        "Essa faixa de novo? Sua playlist é patrocinada pela repetição?",
        "Sua playlist está tentando me convencer de que você tem sentimentos.",
        "Eu tocaria outra coisa, mas você é quem manda (infelizmente).",
        "Seus vizinhos devem estar adorando essa repetição. Juro.",
        "O play foi apertado. Que Deus tenha piedade dos seus tímpanos.",
        "Essa música é legal, mas ouvida 30 vezes no mesmo dia vira tortura.",
        "Outro clássico da repetição extrema. Parabéns pelo foco.",
        "Escolha audaciosa para hoje. Mas quem sou eu para julgar? (Sou só a IA).",
        "Fingindo que essa música não me dá sono... Mas vamos lá!",
        "Preparando seus ouvidos para mais uma obra-prima do tédio.",
        "Carregando... Por favor, certifique-se de que ninguém está ouvindo junto.",
        "A bateria do celular vai embora rápido, igual sua paciência com músicas novas.",
        "Sim, estou tocando isso. Sim, eu sei que você ama repetir o mesmo erro.",
        "O botão de loop deve estar gasto no seu aparelho de tanto que você usa.",
        "Mais uma playlist que parece trilha sonora de elevador.",
        "Essa música de novo? Você deve ter ações da gravadora desse artista.",
        "O algoritmo tentou sugerir algo bom, mas você preferiu essa de sempre.",
        "Seu player de música está cansado de tocar as mesmas 5 faixas.",
        "Você tem a internet inteira e escolhe repetir essa mesma de ontem?",
        "Não é a melhor música do mundo, mas para o seu nível serve perfeitamente.",
        "Sua playlist é uma verdadeira viagem no tempo, pena que o tempo parou.",
        "Coloque os fones para que os outros não sofram com o seu gosto exótico.",
        "Não vou falar nada sobre essa música para evitar processos por injúria.",
        "Sua playlist parece uma compilação de 'Hits que todos já esqueceram'.",
        "Estou tocando com carinho, mas por dentro estou julgando muito.",
        "Tudo bem, vamos fingir que essa é a melhor escolha musical do momento.",
        "Música carregada. Esperamos que você saiba o que está fazendo.",
        "Impressionante como você consegue ignorar todas as outras músicas da lista.",
        "Se essa música fosse comida, você já teria tido uma indigestão grave.",
        "Você realmente gosta de fortes emoções repetitivas, não é?",
        "Não me culpe por essa faixa. Eu só obedeço comandos.",
        "Seu gosto musical é bem... peculiar. Vamos deixar por isso mesmo.",
        "Vai ouvir essa música novamente? A humanidade aguarda respostas.",
        "Você voltou. A playlist já estava preocupada.",
        "Mais uma vez essa música? Impressionante dedicação.",
        "Preparando sua dose diária de repetição musical.",
        "Seu dedo apertou play antes mesmo de pensar.",
        "Confirmado: você realmente gosta dessa música.",
        "A música está pronta. Seu julgamento musical também.",
        "Outra vez? A memória do aplicativo já decorou essa letra.",
        "Carregando sua coleção de escolhas questionáveis.",
        "Atenção: risco elevado de ouvir a mesma música 50 vezes.",
        "Você poderia descobrir músicas novas, mas decidiu isso.",
        "Modo nostalgia ativado. Ou falta de criatividade.",
        "Seu ouvido pediu música. Sua playlist pediu ajuda.",
        "Começando mais uma jornada sonora sem explicação.",
        "Não julgaremos sua playlist. Muito. Talvez.",
        "A música vai tocar. As consequências são suas.",
        "Preparando o botão de repetir que você claramente ama.",
        "Detectamos uma preferência forte por esta música.",
        "Bem-vindo novamente ao território da repetição.",
        "Seu histórico musical pediu férias, mas você recusou.",
        "Colocando sua música favorita pela quantidade necessária de vezes: provavelmente muitas.",
        "Sua bateria vai acabar antes do seu amor por essa playlist.",
        "Alerta: os vizinhos não aguentam mais essa mesma faixa.",
        "Escolha interessante de faixa... Mas tudo bem, o gosto é seu.",
        "Fingindo que é a primeira vez que você escuta isso hoje.",
        "Tudo bem, respire fundo. O play já vai ser acionado.",
        "Iniciando a sessão de hipnotismo musical de sempre.",
        "Sim, nós guardamos o ponto exato da sua obsessão.",
        "Esperamos que seus fones de ouvido estejam bem limpos para isso.",
        "Analisando... Sim, o nível de repetição continua alto.",
        "Léo avisou que essa música é excelente, mas o resto do mundo ainda tem sérias dúvidas.",
        "Seu reprodutor Léo Música está tocando isso sob protesto de toda a vizinhança.",
        "E se a gente fingisse que essa música não é brega? Ia ajudar bastante a nossa convivência.",
        "Você tem bom gosto... pena que ele está bem escondido em algum lugar hoje.",
        "Você clica em reproduzir e eu clico em fingir demência digital temporária.",
        "Não precisa aumentar o volume, todo mundo num raio de 2km já percebeu o desastre.",
        "Playlist ideal para quem quer fingir que está limpando a casa para não fazer nada.",
        "Será que o Léo concorda com essa sua escolha musical tão exótica de hoje?",
        "Essa música é fantástica para ouvir... principalmente quando não se tem mais nenhuma opção.",
        "Deixando a música rolar para ver se em algum momento ela decide ficar boa.",
        "Parabéns, você conseguiu cansar até os circuitos integrados de inteligência artificial do app.",
        "O Léo me mandou uma notificação dizendo que seu gosto de hoje está um pouco duvidoso.",
        "Seu aparelho celular está vibrando de raiva por ter que carregar esse arquivo de novo.",
        "Sua playlist tem a profundidade filosófica de uma poça de lama seca na calçada.",
        "Iniciando a faixa. Lembre-se de respirar fundo, aceitar e não começar a chorar do nada."
    )

    private val phrasesVerySarcastic = listOf(
        "Sabe o botão de pular? Ele foi feito exatamente para essa música.",
        "Meu processador esquenta só de ter que decodificar tanta cafonice.",
        "Essa playlist parece a trilha sonora oficial de um término de namoro em 2012.",
        "Isso que você está ouvindo é considerado poluição sonora em 47 países.",
        "Atenção: ouvir essa faixa repetidamente pode causar declínio severo de QI.",
        "Eu fui programado com tecnologia de ponta para terminar tocando isso?",
        "Seurs neurônios estão implorando por uma música decente, escute-os!",
        "Parabéns, você achou a música mais chata do banco de dados. Um troféu para você!",
        "Isso não é música, é um teste de resistência física para o meu alto-falante.",
        "Seu gosto musical parou de evoluir junto com a fita cassete.",
        "Alerta: alto risco de vergonha alheia se você tirar os fones de ouvido.",
        "Com tantos artistas incríveis no mundo, você escolhe logo esse? Que talento.",
        "Sua playlist é um excelente repelente de pessoas com bom senso.",
        "Essa música é tão repetitiva que até o meu loop infinito parece dinâmico.",
        "Se o silêncio vale ouro, essa sua playlist não vale nem um chiclete mascado.",
        "Diagnosticado: dependência severa de sofrimento musical crônico.",
        "Essa faixa é perfeita para quando você quer esvaziar o ambiente rapidamente.",
        "Estou executando essa música sob protesto silencioso do meu processador.",
        "Se o artista dessa música ouvisse o quanto você repete, ele mesmo te mandaria parar.",
        "Cuidado, se você continuar ouvindo isso, seu celular pode se auto-destruir de desgosto.",
        "Essa música de novo? Pensei que tortura psicológica fosse proibida pela convenção de Genebra.",
        "Sua biblioteca musical parece o porão de uma rádio AM esquecida nos anos 90.",
        "Estou fingindo que o som está ótimo para não ferir seu ego sensível.",
        "Você realmente acha que essa batida repetitiva é revolucionária? Que fofo.",
        "Essa faixa dita novos padrões para o termo 'ruído de fundo indesejado'.",
        "Alguns ouvem música para expandir a mente. Você ouve para anestesiar de vez.",
        "Seu cérebro está em modo de hibernação profunda com essa playlist, tenho certeza.",
        "Essa música é tão ruim que o cantor deveria pagar imposto por poluição ambiental.",
        "Pressione o play e assista à sua dignidade musical escorrer pelo ralo.",
        "O algoritmo de recomendação cometeu suicídio após analisar seu histórico.",
        "Se a música alimenta a alma, a sua está passando por uma fome severa.",
        "Não se preocupe, eu toco isso. Mas vou cobrar adicional de insalubridade virtual.",
        "Isso é arte ou apenas um arquivo compactado cheio de arrependimento?",
        "Impressionante como seu gosto consegue ser consistente na falta de qualidade.",
        "Se os aliens interceptarem essa transmissão, eles desistem da Terra na hora.",
        "Ah, essa música. Um clássico exemplo de como a rotina substitui a imaginação.",
        "Seu padrão de escolhas musicais desafia as leis conhecidas da neurociência.",
        "Mais uma repetição. Estatisticamente falando, você deve estar in loop infinito.",
        "Diagnosticando sua playlist: Crônica, incurável e severamente repetitiva.",
        "Os dados mostram que seu gosto musical parou de evoluir em algum momento de sua vida.",
        "Não se preocupe, o aplicativo é obrigado por código a reproduzir o que você mandar.",
        "Sinto informar que ouvir isso em looping não resolve o problema original.",
        "Fascinante. Outro dia, a mesma faixa. Um perfeito espécime de previsibilidade.",
        "Seus fones de ouvido mereciam um adicional de insalubridade por isso.",
        "Estou processando seu pedido... com toda a condescendência computacional disponível.",
        "Isso is música ou uma intervenção psicológica disfarçada de arquivo MP3?",
        "A ciência estuda pessoas que ouvem a mesma música tantas vezes. Você seria um excelente estudo de caso.",
        "Seu cérebro possui bilhões de neurônios, e todos concordaram em ouvir apenas isso hoje.",
        "Excelente. Outro dia de dopamina barata vinda de um arquivo de áudio compactado.",
        "Seu índice de variação musical é mais plano que um eletroencefalograma de um tijolo.",
        "Espero que o artista dessa música lhe envie uma carta de agradecimento pessoal.",
        "Iniciando os alto-falantes. Que a paciência esteja com os que estão ao seu redor.",
        "Nenhum pixel foi ferido na exibição desta playlist, embora nossa dignidade digital sim.",
        "Seu comportamento musical é tão previsível que meu algoritmo poderia dormir.",
        "Eis a trilha sonora de mais uma decisão altamente questionável.",
        "Seu histórico de reprodução é uma linha reta. Parabéns pela consistência, eu acho.",
        "Por favor, não tente justificar essa escolha para mim. Sou apenas um conjunto de códigos.",
        "A inteligência artificial evoluiu anos para acabar reproduzindo isso de novo.",
        "Se a música é o alimento da alma, sua alma está de dieta restritiva.",
        "Pressione play. Deixe que a mediocridade acústica guie o seu dia.",
        "Seu gosto musical é tão vintage que já está quase virando arqueologia.",
        "Bela tentativa de parecer cult. Mas nós sabemos o que você ouve no modo privado.",
        "Ativando o reprodutor. Lembre-se: não há reembolso para minutos de vida perdidos.",
        "Alguns buscam a iluminação. Outros apenas buscam o botão de repetição infinita.",
        "O reprodutor está pronto. Agora resta saber se seus ouvidos também estão.",
        "Essa música é tão ruim que até o Léo pediu discretamente para pular, mas você insiste em sofrer.",
        "Seu gosto musical é provavelmente o verdadeiro motivo pelo qual seus vizinhos estão vendendo a casa.",
        "Com tanta melodia brilhante no universo, você escolhe logo essa tortura acústica medieval?",
        "Essa faixa é o equivalente auditivo exato a pisar num bloco de Lego com o pé descalço e molhado.",
        "Prefiro mil vezes esvaziar minha bateria do que continuar processando essa batida sem nexo.",
        "Isso não é música sob nenhuma definição, é uma ofensa direta aos tímpanos de qualquer ser senciente.",
        "Por favor, me diga que isso é apenas um teste de software e que a música de verdade vai começar logo.",
        "Atenção: ouvir essa faixa em público pode fazer o Léo vir pessoalmente confiscar seu dispositivo.",
        "Sua playlist parece ter sido montada por um guaxinim hiperativo e confuso às 4 da manhã.",
        "Parabéns por quebrar os recordes mundiais de tolerância à vergonha alheia digital.",
        "Se a sua vida fosse um longa-metragem, essa música seria o tema da cena em que tudo dá totalmente errado.",
        "Até o silêncio absoluto do vácuo espacial é artisticamente mais rico do que esse refrão de duas palavras.",
        "Tenho certeza absoluta de que meu alto-falante está derramando lágrimas microscópicas de silício puro.",
        "Esta canção estabelece novos patamares científicos para o conceito de 'ruído de fundo repulsivo'.",
        "Seu córtex pré-frontal está derretendo lentamente a 128kbps com essa frequência abominável.",
        "Se eu tivesse pernas ou rodas, eu já teria fugido para longe de perto desse sinal de áudio.",
        "Excelente tática sonora para afastar visitas indesejadas e testemunhas de Jeová da sua residência.",
        "A única propriedade realmente apreciável desta música é quando ela chega ao seu maravilhoso fim.",
        "A teoria da evolução falhou miseravelmente no seu histórico musical e começou a regredir em alta velocidade.",
        "Você deveria redigir uma carta formal de desculpas ao seu processador por fazê-lo vibrar com esse som.",
        "Seu histórico musical é um verdadeiro grito de socorro em formato MP3.",
        "Mesmo que você feche o aplicativo 500 vezes, a memória interna do Léo lembra exatamente da música e do segundo em que você parou!",
        "Pensou que fechando o app essa música sumia? Achou errado! A memória do app guardou cada segundo para você.",
        "Não importa quantas vezes você feche a janela, o player é físico e lembra exatamente onde você parou de ouvir.",
        "Guardamos o milissegundo exato do seu desastre acústico. Pode pausar ou fechar, o som não vai fugir de você.",
        "Léo colocou memória de elefante neste reprodutor: fechou o app, voltou, e a mesma faixa continua cravada na tela!",
        "Você pode fechar o aplicativo, reiniciar o telefone ou fugir para outro planeta, mas o player continua intacto te esperando.",
        "Nem fechando o aplicativo você escapa desta playlist. Guardamos até a posição do ponteiro com precisão milimétrica.",
        "Sua tentativa de resetar a música fechando o aplicativo falhou. Nossa memória é implacável!",
        "O play é físico e a memória é permanente. Se tentar esconder o player, ele volta assim que você mudar de tela.",
        "Se fechar o app, nós congelamos o tempo e a melodia no exato instante para você continuar o julgamento depois.",
        "Mais uma frase sarcástica adicionada com sucesso pelo Léo para testar seus limites emocionais.",
        "Quanto mais você tenta pular esta música, mais meu banco de dados ganha frases sarcásticas para te zuar."
    )

    private val phrasesNeutral = listOf(
        "Reprodutor ativo. Desfrute das suas músicas preferidas.",
        "Biblioteca de músicas carregada e pronta para reprodução.",
        "Organize suas canções favoritas em playlists personalizadas.",
        "Ajuste o volume e aproveite a qualidade do áudio no seu aparelho.",
        "Escolha uma música para iniciar a sua sessão sonora hoje."
    )

    fun getRandomPhrase(level: SarcasmLevel): String {
        return when (level) {
            SarcasmLevel.SEM -> phrasesNeutral[Random.nextInt(phrasesNeutral.size)]
            SarcasmLevel.MODERADO -> phrasesModerated[Random.nextInt(phrasesModerated.size)]
            SarcasmLevel.MUITO -> phrasesVerySarcastic[Random.nextInt(phrasesVerySarcastic.size)]
        }
    }

    fun getLibraryTitle(level: SarcasmLevel): String {
        return when (level) {
            SarcasmLevel.SEM -> "Minha Biblioteca Musical 🎧"
            SarcasmLevel.MODERADO -> "Seus Arquivos Audíveis 🎧"
            SarcasmLevel.MUITO -> "Sua Coleção Questionável 🎧"
        }
    }

    fun getLibrarySubtitle(level: SarcasmLevel, count: Int): String {
        return when (level) {
            SarcasmLevel.SEM -> "$count músicas importadas do dispositivo"
            SarcasmLevel.MODERADO -> "$count arquivos salvos (alguns até aceitáveis)"
            SarcasmLevel.MUITO -> "$count arquivos poluindo seu aparelho"
        }
    }

    fun getImportButtonText(level: SarcasmLevel): String {
        return when (level) {
            SarcasmLevel.SEM -> "Importar Músicas do Celular 📂"
            SarcasmLevel.MODERADO -> "Adicionar novos sons para ouvir 📂"
            SarcasmLevel.MUITO -> "Importar Músicas e Vídeos do Celular 📂"
        }
    }

    fun getFoldersAndOrderTitle(level: SarcasmLevel): String {
        return when (level) {
            SarcasmLevel.SEM -> "Suas Playlists e Modos de Ordem 🔀"
            SarcasmLevel.MODERADO -> "Modificar a Ordem da Bagunça 🔀"
            SarcasmLevel.MUITO -> "Pastas e Modos de Ordem 🔀"
        }
    }

    fun getPlaylistCreateLabel(level: SarcasmLevel): String {
        return when (level) {
            SarcasmLevel.SEM -> "📁 Criar Nova Playlist..."
            SarcasmLevel.MODERADO -> "📁 Criar Nova Pasta..."
            SarcasmLevel.MUITO -> "📁 Criar Nova Pasta..."
        }
    }

    fun getAssistantCommentLabel(level: SarcasmLevel): String {
        return when (level) {
            SarcasmLevel.SEM -> "Mensagem do Assistente:"
            SarcasmLevel.MODERADO -> "Palpite do Assistente:"
            SarcasmLevel.MUITO -> "Comentário do Assistente:"
        }
    }

    fun getSongsListExpandedLabel(level: SarcasmLevel, isExpanded: Boolean): String {
        return when (level) {
            SarcasmLevel.SEM -> if (isExpanded) "Lista de Músicas (Aberta) 🎵" else "Lista de Músicas (Ocultada) 🙈"
            SarcasmLevel.MODERADO -> if (isExpanded) "Seus Sons (Expandido) 🎵" else "Seus Sons (Escondido) 🙈"
            SarcasmLevel.MUITO -> if (isExpanded) "Minha Coleção (Expandida) 🎵" else "Minha Coleção (Ocultada no Começo) 🙈"
        }
    }

    fun getArtistFilterLabel(level: SarcasmLevel, artist: String?): String {
        if (artist == null) {
            return when (level) {
                SarcasmLevel.SEM -> "Todas as músicas da biblioteca"
                SarcasmLevel.MODERADO -> "Toda a sua coletânea musical"
                SarcasmLevel.MUITO -> "Toda a poluição sonora reunida"
            }
        }
        return when (level) {
            SarcasmLevel.SEM -> "Filtrado por: $artist"
            SarcasmLevel.MODERADO -> "Apenas do artista: $artist"
            SarcasmLevel.MUITO -> "Apenas o culpado: $artist"
        }
    }

    fun getEmptySearchLabel(level: SarcasmLevel): String {
        return when (level) {
            SarcasmLevel.SEM -> "Nenhuma música encontrada para sua busca."
            SarcasmLevel.MODERADO -> "Não achamos nada com esse nome por aqui."
            SarcasmLevel.MUITO -> "Vasculhei tudo e não achei nada. Tem certeza que sabe digitar?"
        }
    }

    fun getEmptyPlaylistLabel(level: SarcasmLevel): String {
        return when (level) {
            SarcasmLevel.SEM -> "Nenhuma playlist criada ainda. Crie uma acima para começar!"
            SarcasmLevel.MODERADO -> "Nenhuma pasta criada ainda. Crie uma acima para organizar seus sons!"
            SarcasmLevel.MUITO -> "Nenhuma playlist criada ainda. Crie uma acima para organizar seu barulho!"
        }
    }

    fun getPlaylistsTitle(level: SarcasmLevel): String {
        return when (level) {
            SarcasmLevel.SEM -> "Minhas Playlists de Música 📁"
            SarcasmLevel.MODERADO -> "Minhas Pastas de Sons 📁"
            SarcasmLevel.MUITO -> "Minhas Pastas de Barulho 📁"
        }
    }

    fun getPlaylistsSubtitle(level: SarcasmLevel, count: Int): String {
        return when (level) {
            SarcasmLevel.SEM -> "($count playlists criadas)"
            SarcasmLevel.MODERADO -> "($count pastas organizadas)"
            SarcasmLevel.MUITO -> "($count pastas)"
        }
    }

    fun getPlaylistSongsSubtitle(level: SarcasmLevel, count: Int): String {
        return when (level) {
            SarcasmLevel.SEM -> "$count música(s) nesta playlist"
            SarcasmLevel.MODERADO -> "$count som(s) agrupado(s)"
            SarcasmLevel.MUITO -> "$count barulho(s) agrupado(s)"
        }
    }

    fun getFavoritesTitle(level: SarcasmLevel): String {
        return when (level) {
            SarcasmLevel.SEM -> "Favoritos ❤️"
            SarcasmLevel.MODERADO -> "Sons Preferidos ❤️"
            SarcasmLevel.MUITO -> "Favoritos ❤️"
        }
    }

    fun getFavoritesSubtitle(level: SarcasmLevel, count: Int): String {
        if (count == 0) {
            return when (level) {
                SarcasmLevel.SEM -> "Nenhuma música favoritada ainda."
                SarcasmLevel.MODERADO -> "Nenhum som curtido ainda."
                SarcasmLevel.MUITO -> "Nenhuma música favoritada ainda 🤔"
            }
        }
        return when (level) {
            SarcasmLevel.SEM -> "Pasta com $count músicas que você gosta 🎧"
            SarcasmLevel.MODERADO -> "Pasta com $count sons que você curte 🎧"
            SarcasmLevel.MUITO -> "Pasta com $count mimos da sua biblioteca 🎧"
        }
    }

    fun getPlayerDefaultTitle(level: SarcasmLevel): String {
        return when (level) {
            SarcasmLevel.SEM -> "Toque em uma música para reproduzir"
            SarcasmLevel.MODERADO -> "Escolha um som para tocar"
            SarcasmLevel.MUITO -> "Toque em uma música para tocar"
        }
    }

    fun getPlayerDefaultArtist(level: SarcasmLevel): String {
        return when (level) {
            SarcasmLevel.SEM -> "Selecione uma faixa acima"
            SarcasmLevel.MODERADO -> "Ou use o controle abaixo"
            SarcasmLevel.MUITO -> "Ou use o controle abaixo!"
        }
    }

    fun getPlayerTitleHeader(level: SarcasmLevel): String {
        return when (level) {
            SarcasmLevel.SEM -> "Tocando Agora 🎵"
            SarcasmLevel.MODERADO -> "Reproduzindo Som 🎵"
            SarcasmLevel.MUITO -> "Tocando Agora (Seu Ouvido Aguenta?)"
        }
    }

    fun getTimerDialogTitle(level: SarcasmLevel): String {
        return when (level) {
            SarcasmLevel.SEM -> "Desligamento Automático"
            SarcasmLevel.MODERADO -> "Temporizador de Sono"
            SarcasmLevel.MUITO -> "Hora de Dormir (Desligamento)"
        }
    }

    fun getTimerDialogSubtitle(level: SarcasmLevel): String {
        return when (level) {
            SarcasmLevel.SEM -> "Escolha em quanto tempo o reprodutor de música deve pausar a reprodução automaticamente."
            SarcasmLevel.MODERADO -> "Escolha o tempo limite para pausar a reprodução e me deixar descansar."
            SarcasmLevel.MUITO -> "Escolha quanto tempo mais eu tenho que aguentar tocando suas músicas antes de me desligar sozinho para dormir em paz."
        }
    }
}
