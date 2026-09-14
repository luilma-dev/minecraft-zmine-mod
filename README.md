<div align="center">
  <img src="images/for%20README/minecraft.svg" alt="Minecraft Logo" height="60" />
  <img src="images/for%20README/forge.svg" alt="Forge Logo" height="60" />
  <h1>ZMine Mod</h1>
</div>

O ZMine é um mod para Minecraft desenvolvido sobre o ecossistema Fabric. O objetivo principal do projeto é expandir as mecânicas de sobrevivência do jogo base, introduzindo um sistema de hidratação e novos desafios ambientais.

O desenvolvimento deste mod demonstra o uso prático da Fabric API e do framework Mixin para alterar comportamentos fundamentais do motor do jogo (como renderização de tela, manipulação de entidades e itens) mantendo a estabilidade e a performance.

## Arquitetura e Funcionalidades

- **Geração de Mundo e Construções Absurdas**: O mod contém uma geração de mundo avançada com estruturas espalhadas pelo mapa, simulando a complexidade e o visual de um mapa de aventura ou de sobrevivência feito à mão no Minecraft. Contudo, tudo isso é alcançado organicamente através da geração procedural de mundos e estruturas dinâmicas, utilizando as tecnologias de *World Generation* e *Jigsaw* do Minecraft.
- **Cidades abandonadas e campos**: ruas curvas, rodovias regionais, pontes sobre água, túneis nas montanhas, treze construções pequenas e quatro grandes complexos. Postes espaçados e caídos, tanques abandonados e crateras completam os bairros. As sete caixas personalizadas possuem inventário persistente e loot aleatório. Veja [construções, comandos de teste e configuração](docs/CONSTRUCOES.md).
- **Mecânica de Sede (Thirst System)**: Introduz um sistema completo de hidratação no loop principal do jogo. O jogador precisa buscar ativamente fontes de água e gerenciar seus níveis de sede ao lado da mecânica padrão de fome.
- **Interface Customizada (HUD)**: Implementação de uma nova barra renderizada diretamente na interface nativa do Minecraft, utilizando as APIs de sobreposição do Fabric para exibir o nível de hidratação em tempo real.
- **Geração de Terreno**: Interceptação e alteração na geração procedural de mundos para introduzir novos tipos de biomas com condições de sobrevivência extremas (Wasteland).
- **Injeção de Código Não-Intrusiva**: Uso avançado do ecossistema SpongePowered Mixin para interagir com o código-fonte ofuscado do Minecraft. Lógicas customizadas são injetadas em classes núcleo (como `PlayerEntity` e `Item`) sem substituir os arquivos originais.

## Tecnologias Utilizadas

- **Java**
- **Fabric Mod Loader & Fabric API**
- **SpongePowered Mixin**
- **Minecraft World Generation API & Jigsaw Blocks**
- **Gradle**

## Como Executar

### Para Usuários
1. Instale o [Fabric Loader](https://fabricmc.net/use/installer/).
2. Adicione a **Fabric API** no diretório `.minecraft/mods` do seu sistema.
3. Adicione o arquivo `.jar` compilado deste projeto no mesmo diretório.
4. Inicie o jogo selecionando o perfil do Fabric no Launcher.

### Para Desenvolvedores (Build e Ambiente)
Para rodar o projeto localmente ou inspecionar a base de código:

1. Clone o repositório em sua máquina:
   ```bash
   git clone https://github.com/luilma-dev/minecraft-zmine-mod.git
   ```
2. Na raiz do projeto, execute o build através do Gradle Wrapper:
   ```bash
   ./gradlew build
   ```
3. O pacote compilado (`.jar`) será gerado automaticamente no diretório `build/libs/`.

Para executar o cliente de testes localmente e verificar o mod em tempo real:
```bash
./gradlew runClient
```
