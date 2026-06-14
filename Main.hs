{-# LANGUAGE ScopedTypeVariables #-}

module Main where

import Data.Maybe (mapMaybe)
import Control.Exception (catch, IOException)
import Data.List (maximumBy, sortBy)
import qualified Data.Map as Map
import Data.Ord (comparing, Down(Down))
import Data.Time (UTCTime, getCurrentTime)
import System.IO (hFlush, stdout)

-- =============================================================================
-- Tipos de Dados (Arquiteto de Dados)
-- =============================================================================

data Item = Item
  { itemID      :: String
  , nome        :: String
  , quantidade  :: Int
  , categoria   :: String
  } deriving (Show, Read, Eq)

type Inventario = Map.Map String Item

data AcaoLog = Add | Remove | Update | QueryFail
  deriving (Show, Read, Eq)

data StatusLog = Sucesso | Falha String
  deriving (Show, Read, Eq)

data LogEntry = LogEntry
  { timestamp :: UTCTime
  , acao      :: AcaoLog
  , detalhes  :: String
  , status    :: StatusLog
  } deriving (Show, Read, Eq)

type ResultadoOperacao = (Inventario, LogEntry)

inventarioVazio :: Inventario
inventarioVazio = Map.empty

-- =============================================================================
-- Funções Puras de Negócio (Lógica de Negócio)
-- =============================================================================

mkDetalhes :: String -> String -> String
mkDetalhes itemId info = "id=" ++ itemId ++ "; " ++ info

mkLogSucesso :: UTCTime -> AcaoLog -> String -> LogEntry
mkLogSucesso t a d = LogEntry t a d Sucesso

addItem
  :: UTCTime
  -> String
  -> String
  -> Int
  -> String
  -> Inventario
  -> Either String ResultadoOperacao
addItem _ _ _ qtd _ _
  | qtd <= 0 = Left "Quantidade deve ser maior que zero"
addItem t id nome qtd cat inv
  | Map.member id inv =
      Left ("Item com ID '" ++ id ++ "' ja existe")
  | null id =
      Left "ID do item nao pode ser vazio"
  | null nome =
      Left "Nome do item nao pode ser vazio"
  | null cat =
      Left "Categoria do item nao pode ser vazia"
  | otherwise =
      Right
        ( Map.insert id item inv
        , mkLogSucesso t Add (mkDetalhes id ("nome=" ++ nome ++ "; qtd=" ++ show qtd ++ "; cat=" ++ cat))
        )
  where
    item = Item id nome qtd cat

removeItem
  :: UTCTime
  -> String
  -> Int
  -> Inventario
  -> Either String ResultadoOperacao
removeItem _ _ qtd _ | qtd <= 0 = Left "Quantidade a remover deve ser maior que zero"
removeItem t id qtd inv =
  case Map.lookup id inv of
    Nothing ->
      Left ("Item com ID '" ++ id ++ "' nao encontrado")
    Just item ->
      if quantidade item < qtd
        then Left ("Estoque insuficiente para '" ++ id ++ "': disponivel "
                   ++ show (quantidade item) ++ ", solicitado " ++ show qtd)
        else
          let novaQtd = quantidade item - qtd
              novoInv =
                if novaQtd == 0
                  then Map.delete id inv
                  else Map.insert id (item {quantidade = novaQtd}) inv
          in Right
               ( novoInv
               , mkLogSucesso t Remove (mkDetalhes id ("removido=" ++ show qtd ++ "; restante=" ++ show novaQtd))
               )

updateQty
  :: UTCTime
  -> String
  -> Int
  -> Inventario
  -> Either String ResultadoOperacao
updateQty _ _ qtd _ | qtd < 0 = Left "Quantidade nao pode ser negativa"
updateQty t id qtd inv =
  case Map.lookup id inv of
    Nothing ->
      Left ("Item com ID '" ++ id ++ "' nao encontrado")
    Just item ->
      let novoInv =
            if qtd == 0
              then Map.delete id inv
              else Map.insert id (item {quantidade = qtd}) inv
      in Right
           ( novoInv
           , mkLogSucesso t Update (mkDetalhes id ("nova_qtd=" ++ show qtd))
           )

-- =============================================================================
-- Relatórios (Análise de Logs)
-- =============================================================================

extrairId :: LogEntry -> Maybe String
extrairId entry =
  case break (== '=') (detalhes entry) of
    ("id", '=':rest) -> Just (takeWhile (/= ';') rest)
    _                -> Nothing

historicoPorItem :: String -> [LogEntry] -> [LogEntry]
historicoPorItem itemId =
  filter (\e -> extrairId e == Just itemId)

logsDeErro :: [LogEntry] -> [LogEntry]
logsDeErro = filter (\e -> case status e of Falha _ -> True; _ -> False)

itemMaisMovimentado :: [LogEntry] -> Maybe (String, Int)
itemMaisMovimentado logs =
  let movimentacoes = filter (\e -> acao e /= QueryFail) logs
      contagem = Map.fromListWith (+) [(id, 1) | id <- mapMaybe extrairId movimentacoes]
  in case Map.toList contagem of
       []    -> Nothing
       pairs -> Just (maximumBy (comparing (Down . snd)) pairs)

-- =============================================================================
-- Persistência e I/O
-- =============================================================================

arquivoInventario :: FilePath
arquivoInventario = "Inventario.dat"

arquivoAuditoria :: FilePath
arquivoAuditoria = "Auditoria.log"

serializarInventario :: Inventario -> String
serializarInventario = show . Map.toList

desserializarInventario :: String -> Inventario
desserializarInventario s = Map.fromList (read s)

carregarInventario :: IO Inventario
carregarInventario =
  catch
    ( do
        conteudo <- readFile arquivoInventario
        return (desserializarInventario conteudo)
    )
    ( \(_ :: IOException) -> return inventarioVazio
    )

carregarLogs :: IO [LogEntry]
carregarLogs =
  catch
    ( do
        conteudo <- readFile arquivoAuditoria
        return [read linha | linha <- lines conteudo, not (null linha)]
    )
    ( \(_ :: IOException) -> return []
    )

salvarInventario :: Inventario -> IO ()
salvarInventario inv = writeFile arquivoInventario (serializarInventario inv)

appendLog :: LogEntry -> IO ()
appendLog entry = appendFile arquivoAuditoria (show entry ++ "\n")

mkLogFalha :: UTCTime -> AcaoLog -> String -> String -> LogEntry
mkLogFalha t acao det msg = LogEntry t acao det (Falha msg)

registrarFalha :: UTCTime -> AcaoLog -> String -> String -> IO ()
registrarFalha t acao det msg = appendLog (mkLogFalha t acao det msg)

-- =============================================================================
-- Interface de Linha de Comando
-- =============================================================================

mostrarAjuda :: IO ()
mostrarAjuda = putStrLn $ unlines
  [ "Comandos disponiveis:"
  , "  add <id> <nome> <quantidade> <categoria>"
  , "  remove <id> <quantidade>"
  , "  update <id> <quantidade>"
  , "  list"
  , "  report"
  , "  report item <id>"
  , "  help"
  , "  quit"
  ]

mostrarItem :: Item -> String
mostrarItem item =
  unwords
    [ itemID item
    , "|"
    , nome item
    , "| qtd:"
    , show (quantidade item)
    , "| cat:"
    , categoria item
    ]

listarInventario :: Inventario -> IO ()
listarInventario inv
  | Map.null inv = putStrLn "Inventario vazio."
  | otherwise =
      mapM_ (putStrLn . mostrarItem) (sortBy (comparing itemID) (Map.elems inv))

executarRelatorio :: [LogEntry] -> IO ()
executarRelatorio logs = do
  putStrLn "\n=== Relatorio de Auditoria ==="
  putStrLn "\n--- Logs de Erro ---"
  let erros = logsDeErro logs
  if null erros
    then putStrLn "Nenhum erro registrado."
    else mapM_ print erros
  putStrLn "\n--- Item Mais Movimentado ---"
  case itemMaisMovimentado logs of
    Nothing  -> putStrLn "Nenhuma movimentacao registrada."
    Just (id, n) -> putStrLn ("ID: " ++ id ++ " | movimentacoes: " ++ show n)
  putStrLn "\n--- Historico por Item ---"
  putStrLn "Use o comando: report item <id>"

executarRelatorioItem :: String -> [LogEntry] -> IO ()
executarRelatorioItem itemId logs = do
  putStrLn ("\n=== Historico do item '" ++ itemId ++ "' ===")
  let hist = historicoPorItem itemId logs
  if null hist
    then putStrLn "Nenhum registro encontrado para este item."
    else mapM_ print hist

processarComando :: UTCTime -> Inventario -> [LogEntry] -> String -> IO (Inventario, [LogEntry])
processarComando utc inv logs cmd =
  case words cmd of
    ["list"] ->
      listarInventario inv >> return (inv, logs)
    ["help"] ->
      mostrarAjuda >> return (inv, logs)
    ["report"] ->
      executarRelatorio logs >> return (inv, logs)
    ["report", "item", itemId] ->
      executarRelatorioItem itemId logs >> return (inv, logs)
    ["add", id, nome, qtdStr, cat] ->
      case reads qtdStr of
        [(qtd, "")] ->
          tratarOperacao utc inv logs Add (mkDetalhes id ("nome=" ++ nome ++ "; qtd=" ++ qtdStr ++ "; cat=" ++ cat))
            (addItem utc id nome qtd cat inv)
        _ -> do
          entry <- tratarErroParse utc Add (mkDetalhes id "quantidade invalida") "Quantidade invalida"
          return (inv, entry : logs)
    ["remove", id, qtdStr] ->
      case reads qtdStr of
        [(qtd, "")] ->
          tratarOperacao utc inv logs Remove (mkDetalhes id ("removido=" ++ qtdStr))
            (removeItem utc id qtd inv)
        _ -> do
          entry <- tratarErroParse utc Remove (mkDetalhes id "quantidade invalida") "Quantidade invalida"
          return (inv, entry : logs)
    ["update", id, qtdStr] ->
      case reads qtdStr of
        [(qtd, "")] ->
          tratarOperacao utc inv logs Update (mkDetalhes id ("nova_qtd=" ++ qtdStr))
            (updateQty utc id qtd inv)
        _ -> do
          entry <- tratarErroParse utc Update (mkDetalhes id "quantidade invalida") "Quantidade invalida"
          return (inv, entry : logs)
    ["quit"] -> return (inv, logs)
    ["exit"] -> return (inv, logs)
    [] -> return (inv, logs)
    _ -> do
      let det = "comando=" ++ cmd
      registrarFalha utc QueryFail det ("Comando invalido: " ++ cmd)
      putStrLn "Erro: comando invalido. Digite 'help' para ajuda."
      return (inv, mkLogFalha utc QueryFail det ("Comando invalido: " ++ cmd) : logs)

tratarOperacao
  :: UTCTime
  -> Inventario
  -> [LogEntry]
  -> AcaoLog
  -> String
  -> Either String ResultadoOperacao
  -> IO (Inventario, [LogEntry])
tratarOperacao utc inv _logs acao det operacao =
  case operacao of
    Right (novoInv, entry) -> do
      salvarInventario novoInv
      appendLog entry
      putStrLn "Operacao realizada com sucesso."
      return (novoInv, entry : _logs)
    Left msg -> do
      registrarFalha utc acao det msg
      putStrLn ("Erro: " ++ msg)
      let entry = mkLogFalha utc acao det msg
      return (inv, entry : _logs)

tratarErroParse :: UTCTime -> AcaoLog -> String -> String -> IO LogEntry
tratarErroParse utc acao det msg = do
  registrarFalha utc acao det msg
  putStrLn ("Erro: " ++ msg)
  return (mkLogFalha utc acao det msg)

loop :: Inventario -> [LogEntry] -> IO ()
loop inv logs = do
  putStr "\n>>> "
  hFlush stdout
  cmd <- getLine
  case words cmd of
    ["quit"] -> putStrLn "Encerrando sistema. Ate logo!"
    ["exit"] -> putStrLn "Encerrando sistema. Ate logo!"
    _ -> do
      utc <- getCurrentTime
      (novoInv, novosLogs) <- processarComando utc inv logs cmd
      loop novoInv novosLogs

main :: IO ()
main = do
  putStrLn "=============================================="
  putStrLn "  Sistema de Gerenciamento de Inventario"
  putStrLn "  PUCPR - Programacao Logica e Funcional"
  putStrLn "=============================================="
  inv <- carregarInventario
  logs <- carregarLogs
  putStrLn ("Itens carregados: " ++ show (Map.size inv))
  putStrLn ("Registros de auditoria: " ++ show (length logs))
  mostrarAjuda
  loop inv logs
