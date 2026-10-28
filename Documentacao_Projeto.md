# Documentação do Projeto - Calculadora Penal

## 1. Visão geral

A Calculadora Penal estima as datas de **progressão de regime** (semiaberto e aberto) e de **livramento condicional** de uma pessoa condenada, com base na Lei 13.964/19 (Pacote Anticrime), que alterou o art. 112 da Lei de Execução Penal (LEP) e o art. 83 do Código Penal (CP).

O projeto foi desenhado para ser o mais simples possível: **um único processo** (a JVM) serve tanto a API quanto a página web.

## 2. Arquitetura

```
┌──────────────────────────┐   POST /calcular (JSON)    ┌─────────────────────────────┐
│  Navegador               │ ─────────────────────────► │  Servidor Javalin (Kotlin)  │
│  index.html (HTML/CSS/JS)│ ◄───────────────────────── │  Main.kt                    │
│  Bootstrap via CDN       │   JSON com as datas        │  - arquivos estáticos       │
└──────────────────────────┘                            │  - POST /calcular           │
                                                        │  - POST /leads              │
                                                        └─────────────────────────────┘
```

### 2.1 Backend - Kotlin + Javalin

- **Javalin 6.1.3**: micro-framework web leve para Kotlin/Java. Sobe um servidor embutido (Jetty), sem necessidade de instalar Tomcat.
- **Jackson (módulo Kotlin)**: converte JSON em objetos Kotlin (`CalculoRequest`) e o resultado em JSON.
- **slf4j-simple**: exibe logs no console.
- **Arquivos estáticos**: `config.staticFiles.add("/public", Location.CLASSPATH)` serve tudo que está em `src/main/resources/public`. Por isso `index.html` abre em `http://localhost:7070/`.

Endpoints:

| Rota | Função |
|------|--------|
| `POST /calcular` | Valida a entrada, executa o cálculo e devolve as datas em JSON |
| `POST /leads` | Salva o contato (nome, WhatsApp, e-mail, nº do processo) em `leads.csv` |

Erros de validação (ex.: pena zerada) retornam **HTTP 400** com `{"erro": "mensagem"}`.

### 2.2 Frontend - HTML/CSS/JS

Arquivo único `index.html`:

1. Formulário com Pena Total (anos/meses/dias), Data de Início, Detração, Tipo de Crime e Status.
2. `fetch('/calcular')` envia os dados como JSON e exibe o resultado na tela.
3. Abaixo do resultado: formulário de captação de leads (`fetch('/leads')`) e o botão de WhatsApp apontando para `https://api.whatsapp.com/send/?phone=5511989498044`.

## 3. Contrato da API

### Requisição `POST /calcular`

```json
{
  "anos": 8, "meses": 0, "dias": 0,
  "dataInicio": "2024-01-10",
  "detracaoDias": 120,
  "tipoCrime": "HEDIONDO",
  "status": "PRIMARIO"
}
```

Valores de `tipoCrime`: `COMUM_SEM_VIOLENCIA`, `COMUM_COM_VIOLENCIA`, `HEDIONDO`, `HEDIONDO_MORTE`.
Valores de `status`: `PRIMARIO`, `REINCIDENTE`.

### Resposta

```json
{
  "penaTotalDias": 2920,
  "detracaoDias": 120,
  "percentualProgressao": 40,
  "diasParaSemiaberto": 1168,
  "dataSemiaberto": "2026-...",
  "diasParaAberto": 701,
  "dataAberto": "2028-...",
  "livramentoPermitido": true,
  "fracaoLivramento": "2/3",
  "dataLivramento": "2029-...",
  "dataFimPena": "2031-...",
  "observacoes": ["..."]
}
```

## 4. Lógicas matemáticas

### 4.1 Conversão da pena para dias

Convenção adotada: **1 ano = 365 dias** e **1 mês = 30 dias**.

```
penaTotalDias = anos × 365 + meses × 30 + dias
```

### 4.2 Fração com arredondamento para cima

Para evitar erros de ponto flutuante, usa-se apenas aritmética inteira:

```
teto(a × n / d) = (a × n + d − 1) ÷ d     (divisão inteira)
```

O arredondamento para cima é favorável à segurança do cálculo (não antecipa o direito).

### 4.3 Percentuais de progressão (art. 112 da LEP)

| Perfil | Primário | Reincidente |
|--------|:-------:|:-----------:|
| Crime comum, sem violência/grave ameaça | 16% | 20% |
| Crime comum, com violência/grave ameaça | 25% | 30% |
| Hediondo ou equiparado | 40% | 60% |
| Hediondo com resultado morte | 50% | 70% |

### 4.4 Detração

A detração (art. 42 do CP) é o tempo de prisão provisória já cumprido. Ela **antecipa** as datas, pois é descontada do tempo a cumprir:

```
dataProgressão = dataInício + diasExigidos − detração
```

### 4.5 Progressão para o Semiaberto

```
diasSemiaberto = teto(penaTotalDias × percentual / 100)
dataSemiaberto = dataInício + diasSemiaberto − detração
```

### 4.6 Progressão para o Aberto

O percentual incide sobre a **pena restante** após a primeira progressão (entendimento majoritário):

```
penaRestante = penaTotalDias − diasSemiaberto
diasAberto   = teto(penaRestante × percentual / 100)
dataAberto   = dataSemiaberto + diasAberto
```

### 4.7 Livramento Condicional (art. 83 do CP)

| Perfil | Fração exigida |
|--------|:--------------:|
| Primário, crime comum | 1/3 |
| Reincidente, crime comum | 1/2 |
| Hediondo/equiparado, primário | 2/3 |
| Hediondo, reincidente específico | **Vedado** |
| Hediondo com resultado morte | **Vedado** (art. 112, VI-a e VIII, LEP) |

```
diasLivramento = teto(penaTotalDias × fração)
dataLivramento = dataInício + diasLivramento − detração
```

### 4.8 Exemplo trabalhado

Pena de 8 anos, hediondo, primário, início em 10/01/2024, detração de 120 dias:

1. `penaTotalDias = 8 × 365 = 2920`
2. Semiaberto: `teto(2920 × 40 / 100) = 1168` dias → `10/01/2024 + 1168 − 120 = 1048` dias depois
3. Pena restante: `2920 − 1168 = 1752`; aberto: `teto(1752 × 40 / 100) = 701` dias após o semiaberto
4. Livramento: `teto(2920 × 2 / 3) = 1947` dias → `10/01/2024 + 1947 − 120`

## 5. Limitações e premissas

- Os **requisitos subjetivos** (bom comportamento carcerário, exame, aptidão para trabalho etc.) não são avaliados.
- Considera-se que "reincidente" em crime hediondo é **reincidente específico** (vedação ao livramento).
- Não trata remição por trabalho/estudo, regressões, faltas graves, unificação ou soma de penas, nem casos especiais do art. 112 da LEP (ex.: mulheres gestantes/mães, §3º; organização criminosa, milícia).
- A conversão mês = 30 dias é uma simplificação; o Juízo pode usar calendário civil.
- O cálculo é **estimativo** e informativo, não substitui a decisão judicial.

## 6. Possíveis evoluções

- Remição de pena (3 dias de trabalho = 1 dia; estudo)
- Cálculo com várias condenações somadas
- Banco de dados para leads (SQLite/PostgreSQL) no lugar do `leads.csv`
- Exportação do resultado em PDF
