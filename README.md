# Mic Voicenator (Fabric 1.20.1)

Mod que adiciona um item **Microfone**. Clique com o botão direito nele em
cima de qualquer entidade viva para abrir uma tela de gravação com 3
categorias (Ambient / Hurt / Death), gravar com o **seu microfone real**,
ouvir a prévia, e salvar. A partir daí, sempre que aquela entidade
específica for tocar aquele som, o jogo toca sua gravação no lugar.

## Como compilar

Pré-requisitos: **JDK 17** e conexão com a internet (o Gradle precisa baixar
o Fabric Loom, o Minecraft, o Yarn e a Fabric API na primeira vez).

```bash
cd micmod-fabric-1.20.1
./gradlew build
```

> Se não existir `gradlew` no seu sistema, rode `gradle wrapper` uma vez
> (com Gradle 8.x instalado) para gerá-lo, ou abra o projeto no IntelliJ
> IDEA com o plugin do Fabric — ele cuida disso sozinho.

O `.jar` final aparece em `build/libs/micmod-1.0.0.jar`.

## Como instalar

1. Instale o [Fabric Loader](https://fabricmc.net/use/) para 1.20.1.
2. Baixe a [Fabric API](https://modrinth.com/mod/fabric-api) para 1.20.1 e
   coloque na pasta `mods`.
3. Copie o `micmod-1.0.0.jar` gerado para a pasta `mods`.

## Como testar/rodar sem instalar

Dentro do projeto:

```bash
./gradlew runClient
```

Isso abre uma instância do Minecraft de desenvolvimento já com o mod
carregado.

## Como usar no jogo

1. Pegue o item **Microfone** (aba Ferramentas na criativa, ou dê a si
   mesmo com `/give @s micmod:microphone`).
2. Clique com o botão direito em qualquer mob.
3. Escolha a categoria (Ambient, Hurt ou Death).
4. Clique em **● Gravar** (fala no seu microfone do PC), depois em
   **■ Parar** (ou espere os 10s acabarem sozinho).
5. Clique em **▶ Ouvir** para conferir a prévia.
6. Clique em **Salvar**.
7. Pronto — a partir de agora, aquele mob específico (aquele UUID exato,
   não a espécie inteira) vai usar sua gravação para aquele tipo de som.

As gravações ficam salvas em:
```
.minecraft/config/micmod/recordings/<uuid-da-entidade>/<categoria>.wav
```

## Como funciona por baixo dos panos

- **Gravação**: `javax.sound.sampled.TargetDataLine` captura o áudio do
  microfone padrão do sistema operacional em PCM 16-bit/44.1kHz mono,
  limitado a 10 segundos, e empacota como `.wav`.
- **Substituição do som**: um Mixin injeta no método
  `LivingEntity.playSound(SoundEvent, float, float)` (o método comum por
  onde passam os sons de ambiente, dano e morte). Se existir uma gravação
  salva para aquele UUID de entidade + categoria, o som vanilla é
  cancelado e sua gravação é tocada no lugar.
- **Reprodução**: em vez de tentar injetar o `.wav` dentro do motor OpenAL
  do próprio Minecraft (que só entende `.ogg` vindo de resource packs),
  o mod toca o áudio por fora, usando `javax.sound.sampled.Clip`, com
  volume calculado pela distância até a câmera do jogador e um
  panorâmico estéreo simples baseado na direção da entidade.

## Limitações conhecidas

- **Só cliente / não sincroniza em multiplayer**: cada jogador ouve as
  próprias gravações. Em servidor, outro jogador não vai ouvir a sua
  gravação a não ser que vocês implementem rede para enviar o `.wav`
  (dá pra evoluir o mod com um `PayloadChannel` da Fabric API, mas fica
  fora do escopo inicial).
- **Categoria "Hurt" é um fallback**: como `getHurtSound` depende do tipo
  de dano (não temos essa informação no ponto onde interceptamos),
  qualquer som de `playSound` que não seja o ambiente nem a morte da
  entidade é tratado como "hurt". Isso cobre a grande maioria dos casos,
  mas pode ocasionalmente também substituir sons raros (ex: splash de
  alguns mobs aquáticos) se você tiver uma gravação "hurt" salva.
- **Áudio não é 100% posicional como o do motor do jogo** (sem reverb,
  oclusão etc.), mas tem volume por distância e pan estéreo.
- Cada gravação é amarrada ao **UUID daquela entidade específica**, não à
  espécie inteira (assim como sugere o "Princess Sparkle" na sua imagem
  de referência). Se quiser aplicar a todos os mobs de um tipo, dá pra
  adaptar `RecordingStorage` para usar o `EntityType` em vez do UUID.
