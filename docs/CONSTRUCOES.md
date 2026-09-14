# Construções e ambientação ZMine

O Overworld tem cidades abandonadas separadas por campos, ligadas por estradas regionais. A malha é determinística por seed, inclusive em coordenadas negativas e nas divisas dos chunks.

## O que encontrar

| Construção | Interior / detalhes | ID para `/place feature` |
| --- | --- | --- |
| Casa | Varanda, sala, cozinha, quarto e telhado danificado | `zmine:abandoned_house` |
| Prédio | Recepção, quatro apartamentos, três lances de escada, sacadas e caixa-d'água | `zmine:abandoned_apartments` |
| Mercadinho | Prateleiras, caixa, geladeiras e estoque | `zmine:abandoned_market` |
| Clínica | Recepção, enfermaria, consultório e kits médicos | `zmine:abandoned_clinic` |
| Depósito | Estantes, caixas empilhadas e bancada de manutenção | `zmine:abandoned_warehouse` |
| Lanchonete | Piso quadriculado, mesas, bancos e cozinha | `zmine:abandoned_diner` |
| Posto | Cobertura danificada, bombas e loja de conveniência | `zmine:abandoned_gas_station` |
| Oficina | Carro abandonado, ferramentas e escritório | `zmine:abandoned_garage` |
| Posto de controle | Torre com escada, barricadas e abrigo militar | `zmine:abandoned_checkpoint` |
| Acampamento | Barracas, camas, fogueira apagada e suprimentos | `zmine:survivor_camp` |

As ruas urbanas aparecem a cada **48 blocos**, com pistas de **6, 8 ou 12 blocos de largura**. As regiões têm 384 blocos. Rodovias ligam cidades vizinhas quando há terreno seco e acesso válido nas duas pontas; não avançam indefinidamente pelo oceano. As faixas acompanham a pista. Esquinas com duas saídas vizinhas usam curvas de 90 graus; entroncamentos em T e cruzamentos abrem apenas os braços que realmente têm continuação. Ramais sem destino são retirados da malha antes da construção.

As cidades reservam quatro lotes por quarteirão. Além dos modelos da tabela, há casas de tijolo, sobrados, casas rurais, bases militares, torres de apartamentos, hospitais e torres parcialmente desabadas. Os complexos de 32×32 são montados por partes com a mesma planta e altura, independentemente da ordem de geração.

Prédios naturais são gerados exclusivamente pelo distrito, com acesso à malha viária. O gerador avulso `abandoned_building` continua disponível por comando, mas não é adicionado aos biomas para espalhar prédios isolados.

**Terreno e construções:** as ruas e entradas de cada cidade compartilham uma plataforma de referência, com calçadas que acompanham as curvas e se prolongam até fachadas existentes; as ligações rurais mudam de altura gradualmente. As fachadas são orientadas para ruas que existem na malha final, inclusive nos complexos de quatro chunks. Edifícios têm fundações completas até o terreno, telhados com inclinação e orientação corrigidas e pisos de sacada alinhados às portas. Portas, camas e decorações são montadas antes de atualizar as conexões dos blocos. Quarteirões com água ou mais de 6 blocos de desnível em relação à plataforma preservam o terreno, em vez de gerar edifícios suspensos ou enterrados.

**Calçadas e encostas:** a faixa de pedestres acompanha o contorno da pista, inclusive em esquinas, com meio-fio de pedra lisa. Há dois blocos de calçada nas ruas menores e um nas avenidas; o restante do lote conserva uma margem natural quando não há fachada. Aterros e cortes recebem taludes arredondados, com transição gradual até o relevo original. Arenito, areia e terracota conservam os materiais locais. Objetos acompanham a calçada, e a vegetação de borda não é colocada sobre as faixas de trânsito.

**Montanhas e túneis:** cada seção transversal precisa de cobertura natural sobre a largura inteira do arco. Encostas assimétricas não recebem metades de portal suspensas. As paredes ficam por fora da calçada, deixando cinco blocos livres de altura para circulação; o teto tem revestimento duplo e iluminação embutida. Curvas e cruzamentos também usam seções completas. O planejamento consulta o terreno original, e as encostas vizinhas respeitam a cobertura dos túneis e os leitos das pontes. A suavização escreve somente no próprio chunk.

**Carregamento:** a construção da estrada usa o mapa de altura já calculado do próprio chunk. O planejamento das margens reutiliza amostras originais em uma grade de quatro blocos, compartilhadas com a malha urbana e os lotes. A suavização termina a 24 blocos da margem e só calcula um perfil vizinho quando o terreno precisa de ajuste; campos distantes e superfícies já compatíveis evitam esse trabalho. Os caches de amostras e perfis são limitados e descartados junto com o mundo.

**Pontes:** cada cidade reserva **no máximo duas travessias úteis sobre água**, ligando margens com malha viária existente. Se já existe uma rota por terra, a ponte redundante é dispensada; o oceano não recebe quarteirões de pontes. A pista, o tabuleiro e as proteções compartilham o desenho de curva. O tabuleiro fino combina pedra lisa e vigas de andesito; pilares espaçados alcançam o fundo, mantendo água sob a ponte. Bordas de meia altura usam lajes de andesito e tijolos de pedra, trechos musgosos, falhas e ferragens aparentes. Danos preservam o piso estrutural e uma passagem utilizável.

**Ambientação:** trechos comuns recebem um poste a cada 48 blocos, com um poste adicional por cruzamento. Nos trechos bombardeados e tomados pela vegetação, parte deles aparece caída, com haste horizontal, braço dobrado e luminária quebrada. Os tanques têm casco baixo, esteiras, frente inclinada, torre menor, canhão e suprimentos acessíveis na traseira. As crateras são irregulares, com borda queimada, profundidade de até três blocos e fundo sólido. Objetos grandes só aparecem em superfícies planas, evitando modelos deformados nas subidas. São cenários feitos com blocos, sem mecânica de direção.

## Testar no jogo

Crie um **mundo novo** para ver a nova geração, ou explore chunks ainda não visitados. Reinicie o jogo com o `.jar` atualizado. Os edifícios antigos já gravados em um save não são apagados automaticamente.

Para inspecionar um modelo imediatamente, habilite comandos e procure um terreno seco e relativamente plano com pelo menos 16×16 blocos livres:

```mcfunction
/place feature zmine:abandoned_gas_station ~32 ~ ~
/place feature zmine:abandoned_garage ~64 ~ ~
/place feature zmine:abandoned_diner ~96 ~ ~
/place feature zmine:abandoned_checkpoint ~128 ~ ~
/place feature zmine:survivor_camp ~160 ~ ~
```

Use `/place feature zmine:apocalypse_district ~ ~ ~` para gerar o lote previsto pela malha naquela posição; ele usa a altura da cidade e recusa uma área já construída. Use `zmine:abandoned_building` para sortear um dos treze modelos pequenos. A posição é alinhada ao início do chunk que contém as coordenadas; a altura é calculada pelo terreno. Uma tentativa pode falhar em água, encostas íngremes ou terreno já ocupado. São features; `/locate structure` não localiza esses edifícios.

## Caixas e loot

Os sete modelos de `images/Boxes` funcionam como inventários de **27 espaços**. Botão direito abre a caixa. Elas aparecem na aba de blocos funcionais do Criativo, com nomes em português e inglês, ou por `/give @s zmine:box1` etc.

- **Comida:** mantimentos e garrafas de água.
- **Doméstico:** pertences, papel, carvão, ferramentas simples e comida.
- **Ferramentas:** metais, carvão, ferramentas e materiais.
- **Médico:** poções de cura/regeneração, mel, água e suprimentos.
- **Militar:** flechas, equipamento e rações.

As caixas geradas sorteiam 4–7 entradas ao primeiro acesso. O conteúdo e a seed são salvos; esvaziar ou reabrir não renova o loot. Quebrar a caixa derruba o inventário e o próprio bloco. Caixas colocadas pelo jogador começam vazias, para não criar loot infinito.

Para encher uma caixa de teste já colocada:

```mcfunction
/loot insert <x> <y> <z> loot zmine:chests/medical
```

O loot está propositalmente generoso para inspeção, sem balanceamento definitivo.

## Geração e manutenção

- `DistrictLayout.java`: cidades e campos em regiões de 24 chunks, com ruas urbanas a cada 3 chunks e rodovias entre regiões. O desenho é estável para a seed e não consulta chunks vizinhos.
- `AbandonedBuildings.java` e `StreetBuildings.java`: treze plantas pequenas, mobiliário, veículos e pontos de loot.
- `CityNetwork.java`, `CityTerrain.java`, `CityElevation.java`, `RoadLandform.java`, `RoadGeometry.java`, `CityRoads.java` e `CityLots.java`: cotas, fundações, túneis, transições de terreno, curvas, pontes e espaços públicos.
- `ApocalypseScenery.java`: árvores secas, ruínas e colocação avulsa de ruas.
- Cada construção ocupa um lote de 15×15; o prédio chega a 21 blocos de altura. Fachadas dos lotes urbanos apontam para a rua.
- A geração preserva quarteirões inadequados para construções e atravessa água com pontes. As features avulsas de edifícios continuam recusando água e terreno íngreme para permitir testes sem grandes alterações no entorno.
- A cidade é gerada na última etapa de decoração. Árvores, lagos, fontes e decoração de superfície vanilla são substituídos para não invadirem as ruas de chunks vizinhos.
- `tools/generate_city_resources.py` gera 24 tags que desativam estruturas vanilla de superfície (vilas, templos, naufrágios etc.), evitando sobreposição com os lotes. Estruturas subterrâneas, Nether e End permanecem disponíveis.
- A preparação verifica inventários antes de qualquer alteração e aborta se encontrar um. Na colocação manual, também recusa blocos de construção que não sejam terreno natural. Não há regeneração automática de chunks salvos; fronteiras com a geração antiga podem exigir um mundo novo para uma cidade inteiramente uniforme.
- `data/zmine/loot_table/chests/`: cinco tabelas editáveis.
- `tools/generate_structure_resources.py`: reconstrói os recursos a partir dos modelos originais, sem alterar `images/Boxes`.

Validação:

```powershell
.\gradlew.bat build
.\gradlew.bat runGameTest
.\gradlew.bat runClientGameTest -PvisualTests
```

Os testes de servidor verificam persistência, interação, drops, diversidade de loot, terrenos recusados, rotação das plantas, suporte de portas e camas e árvores. Incluem curvas em quatro seeds e nos dois eixos, esquinas de 90 graus, conexões recíprocas, limite de duas pontes e costas sem ruas no oceano, coordenadas negativas, pontes com pilares e água preservada, túneis sem corte do topo, postes espaçados e caídos, cruzamentos, crateras, divisas de chunks e proteção de inventários. O teste opcional de cliente cria mundos separados em `build/run/clientGameTest`: uma galeria em terreno plano e um mundo normal com geração natural da cidade. Ele confere a geração natural e salva capturas das construções, ruas danificadas, ponte curva, esquina, portal de túnel, bairro e rodovia em `screenshots`.

As APIs de inventário e testes seguem a [documentação Fabric 26.2](https://docs.fabricmc.net/develop/blocks/block-entities) e o [framework de testes do Fabric](https://docs.fabricmc.net/develop/automatic-testing).
