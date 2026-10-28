# Calculadora Penal

Aplicação web simples (API em Kotlin + Javalin e Frontend em HTML/JS) que calcula as datas estimadas de:

- Progressão para o regime **Semiaberto**
- Progressão para o regime **Aberto**
- **Livramento Condicional**

conforme a Lei 13.964/19 (Pacote Anticrime), para crimes comuns e hediondos, réus primários e reincidentes.

## Pré-requisitos

- **JDK 17 ou superior** instalado (`java -version` para conferir)
- Não precisa instalar o Gradle: o projeto usa o *Gradle Wrapper*, ou você pode usar o Gradle da sua IDE

## Como rodar

### Opção 1 - IntelliJ IDEA (mais fácil para iniciantes)

1. Abra a pasta do projeto (`File > Open`) e escolha o arquivo `build.gradle.kts`, "Open as Project".
2. Aguarde o IntelliJ baixar as dependências.
3. Abra `src/main/kotlin/Main.kt` e clique no botão verde ▶ ao lado da função `main`.

### Opção 2 - Terminal

Com o Gradle instalado:

```bash
gradle run
```

Se o projeto tiver o wrapper (`gradlew`), use:

```bash
./gradlew run        # Linux / Mac
gradlew.bat run      # Windows
```

> Dica: para gerar o wrapper, rode `gradle wrapper` uma vez na pasta do projeto.

## Acessando

Com o servidor rodando, abra no navegador:

**http://localhost:7070**

## Estrutura do projeto

```
calculadora-penal/
├── settings.gradle.kts
├── build.gradle.kts
├── README.md
├── Documentacao_Projeto.md
└── src/main/
    ├── kotlin/Main.kt                  <- API (Javalin) + lógica de cálculo
    └── resources/public/index.html     <- Frontend (HTML/CSS/JS)
```

## Endpoints

| Método | Rota        | Descrição                                              |
|--------|-------------|--------------------------------------------------------|
| GET    | `/`         | Serve o `index.html`                                   |
| POST   | `/calcular` | Recebe os dados da condenação e devolve as datas       |
| POST   | `/leads`    | Grava o contato do formulário no arquivo `leads.csv`   |

### Exemplo de chamada

```bash
curl -X POST http://localhost:7070/calcular \
  -H "Content-Type: application/json" \
  -d '{"anos":8,"meses":0,"dias":0,"dataInicio":"2024-01-10","detracaoDias":120,"tipoCrime":"HEDIONDO","status":"PRIMARIO"}'
```

## Problemas comuns

- **Porta 7070 em uso:** altere o número em `app.start(7070)` no `Main.kt`.
- **Erro de versão do Java:** confirme que está usando JDK 17+.

## Aviso

Ferramenta informativa. Os resultados são estimativas e **não substituem** a análise de um advogado nem a decisão do Juízo da Execução Penal.
