# Sistema de Gerenciamento de Inventário

**Instituição:** PUCPR  
**Disciplina:** Programação Lógica e Funcional  
**Professor:** Frank Coelho de Alcântara  
**Atividade:** RA2 – Sistema de Inventário em Haskell

## Integrantes (ordem alfabética)

| Nome | GitHub |
|------|--------|
| Pedro Gulin | [@Pedro-Gulin](https://github.com/Pedro-Gulin) |
| Rayssa Grafetti | [@T-800-888](https://github.com/T-800-888) |
| Victor Willian | [@scourgeyyy](https://github.com/scourgeyyy) |

## Ambiente Online

- **Online GDB:** [https://www.onlinegdb.com/](https://www.onlinegdb.com/)
- Selecione a linguagem **Haskell**, cole o conteúdo de `Main.hs`, faça upload de `Inventario.dat` e `Auditoria.log` (ou deixe o programa criá-los na primeira execução) e clique em **Run**.

## Como compilar e executar localmente

### Pré-requisitos

- [GHC](https://www.haskell.org/ghc/) (8.10 ou superior)
- [Cabal](https://www.haskell.org/cabal/) (opcional, recomendado)

### Com Cabal

```bash
cabal build
cabal run inventario
```

### Com GHC diretamente

```bash
ghc -o inventario Main.hs
./inventario
```

O programa lê e grava dois arquivos no diretório atual:

| Arquivo | Descrição |
|---------|-----------|
| `Inventario.dat` | Estado atual do inventário (sobrescrito a cada operação bem-sucedida) |
| `Auditoria.log` | Log append-only de todas as operações (sucesso e falha) |

## Comandos disponíveis

```
add <id> <nome> <quantidade> <categoria>   Adiciona um item ao inventário
remove <id> <quantidade>                     Remove unidades de um item
update <id> <quantidade>                     Atualiza a quantidade de um item
list                                         Lista todos os itens
report                                       Gera relatório geral de auditoria
report item <id>                             Histórico de movimentações de um item
help                                         Exibe a ajuda
quit / exit                                  Encerra o programa
```

### Exemplos

```
>>> add 013 Pasta Termica 50 Escritorio
>>> remove 001 5
>>> update 002 30
>>> list
>>> report
>>> report item 001
>>> quit
```

## Estrutura do projeto

```
haskellTDE/
├── Main.hs           # Código-fonte completo (tipos, lógica, I/O, relatórios)
├── Inventario.dat    # Dados de exemplo (12 itens)
├── Auditoria.log     # Log de exemplo
├── inventario.cabal  # Configuração de build
└── README.md         # Este arquivo
```

### Divisão de responsabilidades

| Integrante | Papel | Módulo / Entrega |
|------------|-------|------------------|
| Pedro Gulin | Arquiteto de Dados | Tipos `Item`, `Inventario`, `LogEntry`, `AcaoLog`, `StatusLog` |
| Rayssa Grafetti | Lógica Pura e Relatórios | `addItem`, `removeItem`, `updateQty`, `historicoPorItem`, `logsDeErro`, `itemMaisMovimentado` |
| Victor Willian | I/O, Loop Principal e Documentação | `main`, persistência, parser de comandos, README, testes manuais |

## Cenários de teste manuais (Seção 4.1)

### Cenário 1: Persistência de estado (sucesso)

| Passo | Ação | Resultado esperado |
|-------|------|--------------------|
| 1 | Apagar `Inventario.dat` e `Auditoria.log` (se existirem) | Arquivos removidos |
| 2 | Iniciar o programa | Sistema inicia com inventário vazio |
| 3 | Executar 3 comandos `add` | Mensagem de sucesso para cada um |
| 4 | Encerrar com `quit` | Programa encerra normalmente |
| 5 | Verificar arquivos criados | `Inventario.dat` e `Auditoria.log` existem |
| 6 | Reiniciar o programa | Itens carregados corretamente |
| 7 | Executar `list` | Os 3 itens adicionados aparecem na listagem |

**Resultado:** ✅ Persistência confirmada — inventário e log são gravados e recarregados corretamente.

---

### Cenário 2: Erro de lógica (estoque insuficiente)

| Passo | Ação | Resultado esperado |
|-------|------|--------------------|
| 1 | `add 099 Teclado 10 Perifericos` | Sucesso — 10 unidades |
| 2 | `remove 099 15` | Erro: "Estoque insuficiente..." |
| 3 | `list` | Item 099 ainda com 10 unidades |
| 4 | Verificar `Inventario.dat` | Quantidade permanece 10 |
| 5 | Verificar `Auditoria.log` | Entrada com `status = Falha ...` registrada |

**Resultado:** ✅ Falha tratada corretamente — inventário inalterado e erro registrado no log.

---

### Cenário 3: Geração de relatório de erros

| Passo | Ação | Resultado esperado |
|-------|------|--------------------|
| 1 | Após o Cenário 2, executar `report` | Relatório exibido no terminal |
| 2 | Verificar seção "Logs de Erro" | Falha do Cenário 2 aparece na listagem |

**Resultado:** ✅ Função `logsDeErro` retorna corretamente a entrada de falha do estoque insuficiente.

## Funções puras implementadas

### Operações de inventário

```haskell
addItem    :: UTCTime -> String -> String -> Int -> String -> Inventario -> Either String ResultadoOperacao
removeItem :: UTCTime -> String -> Int -> Inventario -> Either String ResultadoOperacao
updateQty  :: UTCTime -> String -> Int -> Inventario -> Either String ResultadoOperacao
```

### Relatórios

```haskell
historicoPorItem    :: String -> [LogEntry] -> [LogEntry]
logsDeErro          :: [LogEntry] -> [LogEntry]
itemMaisMovimentado :: [LogEntry] -> Maybe (String, Int)
```

## Licença

Projeto acadêmico — uso exclusivo para fins educacionais.
