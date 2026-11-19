import java.io.*;
import java.util.*;

public class Main {
    static class Aresta {
        int origem;
        int destino;
        double peso;

        Aresta(int origem, int destino, double peso) {
            this.origem = origem;
            this.destino = destino;
            this.peso = peso;
        }
    }

    static final double TOLERANCIA = 1e-9;
    static final int MAXIMO_PARADAS_PULADAS = 2;

    static int totalVertices;
    static boolean direcionado;
    static String[] nomesVertices;
    static ArrayList<Aresta> todasArestas;
    static ArrayList<Aresta>[] arestasQueSaem;

    @SuppressWarnings("unchecked")
    static void criarGrafoVazio(int quantidadeVertices, boolean ehDirecionado) {
        totalVertices = quantidadeVertices;
        direcionado = ehDirecionado;
        nomesVertices = new String[totalVertices];
        for (int v = 0; v < totalVertices; v++) nomesVertices[v] = "No " + (v + 1);
        todasArestas = new ArrayList<>();
        arestasQueSaem = new ArrayList[totalVertices];
        for (int v = 0; v < totalVertices; v++) arestasQueSaem[v] = new ArrayList<>();
    }

    static void adicionarAresta(int origem, int destino, double peso) {
        todasArestas.add(new Aresta(origem, destino, peso));
        arestasQueSaem[origem].add(new Aresta(origem, destino, peso));
        if (!direcionado) arestasQueSaem[destino].add(new Aresta(destino, origem, peso));
    }

    static void salvarGrafo(String nomeArquivo) throws IOException {
        PrintWriter escritor = new PrintWriter(new OutputStreamWriter(new FileOutputStream(nomeArquivo), "UTF-8"));
        escritor.println("*Vertices " + totalVertices);
        for (int v = 0; v < totalVertices; v++) escritor.println((v + 1) + " \"" + nomesVertices[v] + "\"");
        escritor.println(direcionado ? "*Arcs" : "*Edges");
        for (Aresta aresta : todasArestas) escritor.println((aresta.origem + 1) + " " + (aresta.destino + 1) + " " + aresta.peso);
        escritor.close();
    }

    static void carregarGrafo(String nomeArquivo) throws IOException {
        BufferedReader leitor = new BufferedReader(new InputStreamReader(new FileInputStream(nomeArquivo), "UTF-8"));
        String linha = leitor.readLine();
        criarGrafoVazio(Integer.parseInt(linha.trim().split("\\s+")[1]), false);
        boolean lendoVertices = true;
        while ((linha = leitor.readLine()) != null) {
            linha = linha.trim();
            if (linha.isEmpty()) continue;
            if (linha.startsWith("*")) {
                lendoVertices = false;
                direcionado = linha.toLowerCase().startsWith("*arcs");
                continue;
            }
            if (lendoVertices) {
                int primeiraAspas = linha.indexOf('"');
                int ultimaAspas = linha.lastIndexOf('"');
                int numero = Integer.parseInt(linha.substring(0, primeiraAspas).trim());
                nomesVertices[numero - 1] = linha.substring(primeiraAspas + 1, ultimaAspas);
            } else {
                String[] partes = linha.split("\\s+");
                double peso = partes.length > 2 ? Double.parseDouble(partes[2]) : 1;
                adicionarAresta(Integer.parseInt(partes[0]) - 1, Integer.parseInt(partes[1]) - 1, peso);
            }
        }
        leitor.close();
    }

    @SuppressWarnings("unchecked")
    static ArrayList<Integer>[] vizinhosIgnorandoDirecao() {
        ArrayList<Integer>[] vizinhos = new ArrayList[totalVertices];
        for (int v = 0; v < totalVertices; v++) vizinhos[v] = new ArrayList<>();
        for (Aresta aresta : todasArestas) {
            vizinhos[aresta.origem].add(aresta.destino);
            vizinhos[aresta.destino].add(aresta.origem);
        }
        return vizinhos;
    }

    static int[] numeroDoComponenteDeCadaVertice() {
        int[] componenteDoVertice = new int[totalVertices];
        Arrays.fill(componenteDoVertice, -1);
        ArrayList<Integer>[] vizinhos = vizinhosIgnorandoDirecao();
        int componenteAtual = 0;
        for (int inicio = 0; inicio < totalVertices; inicio++) {
            if (componenteDoVertice[inicio] != -1) continue;
            ArrayDeque<Integer> fila = new ArrayDeque<>();
            fila.add(inicio);
            componenteDoVertice[inicio] = componenteAtual;
            while (!fila.isEmpty()) {
                int atual = fila.poll();
                for (int vizinho : vizinhos[atual]) {
                    if (componenteDoVertice[vizinho] == -1) {
                        componenteDoVertice[vizinho] = componenteAtual;
                        fila.add(vizinho);
                    }
                }
            }
            componenteAtual++;
        }
        return componenteDoVertice;
    }

    static int quantidadeDeComponentes(int[] componenteDoVertice) {
        int maior = -1;
        for (int componente : componenteDoVertice) maior = Math.max(maior, componente);
        return maior + 1;
    }

    static boolean grafoEhConexo() {
        return totalVertices == 0 || quantidadeDeComponentes(numeroDoComponenteDeCadaVertice()) == 1;
    }

    static void mostrarComponentes() {
        int[] componenteDoVertice = numeroDoComponenteDeCadaVertice();
        int total = quantidadeDeComponentes(componenteDoVertice);
        if (total <= 1) {
            System.out.println("Grafo conexo, so existe 1 componente.");
            return;
        }
        System.out.println((direcionado ? "Componentes fracamente conectados: " : "Componentes: ") + total);
        int[] tamanho = new int[total];
        for (int componente : componenteDoVertice) tamanho[componente]++;
        for (int componente = 0; componente < total; componente++) {
            if (componente >= 30) {
                System.out.println("... (" + (total - 30) + " componentes omitidos)");
                break;
            }
            System.out.print("Componente " + (componente + 1) + " (" + tamanho[componente] + " nos)");
            if (tamanho[componente] <= 15) {
                System.out.print(":");
                for (int v = 0; v < totalVertices; v++) {
                    if (componenteDoVertice[v] == componente) System.out.print(" " + (v + 1));
                }
            }
            System.out.println();
        }
    }

    static boolean grafoEhEuleriano() {
        int[] grauEntrada = new int[totalVertices];
        int[] grauSaida = new int[totalVertices];
        for (Aresta aresta : todasArestas) {
            grauSaida[aresta.origem]++;
            grauEntrada[aresta.destino]++;
        }
        int[] componenteDoVertice = numeroDoComponenteDeCadaVertice();
        int componenteDasArestas = -1;
        for (int v = 0; v < totalVertices; v++) {
            if (direcionado) {
                if (grauEntrada[v] != grauSaida[v]) return false;
            } else if ((grauEntrada[v] + grauSaida[v]) % 2 != 0) {
                return false;
            }
            if (grauEntrada[v] + grauSaida[v] > 0) {
                if (componenteDasArestas == -1) componenteDasArestas = componenteDoVertice[v];
                else if (componenteDoVertice[v] != componenteDasArestas) return false;
            }
        }
        return componenteDasArestas != -1;
    }

    static int encontrarRaiz(int[] raizDoVertice, int vertice) {
        while (raizDoVertice[vertice] != vertice) {
            raizDoVertice[vertice] = raizDoVertice[raizDoVertice[vertice]];
            vertice = raizDoVertice[vertice];
        }
        return vertice;
    }

    static boolean grafoEhCiclico() {
        if (!direcionado) {
            int[] raizDoVertice = new int[totalVertices];
            for (int v = 0; v < totalVertices; v++) raizDoVertice[v] = v;
            for (Aresta aresta : todasArestas) {
                int raizOrigem = encontrarRaiz(raizDoVertice, aresta.origem);
                int raizDestino = encontrarRaiz(raizDoVertice, aresta.destino);
                if (raizOrigem == raizDestino) return true;
                raizDoVertice[raizOrigem] = raizDestino;
            }
            return false;
        }
        int[] arestasQueChegam = new int[totalVertices];
        for (Aresta aresta : todasArestas) arestasQueChegam[aresta.destino]++;
        ArrayDeque<Integer> semDependencias = new ArrayDeque<>();
        for (int v = 0; v < totalVertices; v++) {
            if (arestasQueChegam[v] == 0) semDependencias.add(v);
        }
        int verticesRemovidos = 0;
        while (!semDependencias.isEmpty()) {
            int atual = semDependencias.poll();
            verticesRemovidos++;
            for (Aresta aresta : arestasQueSaem[atual]) {
                arestasQueChegam[aresta.destino]--;
                if (arestasQueChegam[aresta.destino] == 0) semDependencias.add(aresta.destino);
            }
        }
        return verticesRemovidos < totalVertices;
    }

    static double[] menoresDistanciasDe(int origem) {
        double[] distancia = new double[totalVertices];
        Arrays.fill(distancia, Double.MAX_VALUE);
        distancia[origem] = 0;
        PriorityQueue<double[]> fila = new PriorityQueue<>((a, b) -> Double.compare(a[0], b[0]));
        fila.add(new double[]{0, origem});
        while (!fila.isEmpty()) {
            double[] item = fila.poll();
            int atual = (int) item[1];
            if (item[0] > distancia[atual]) continue;
            for (Aresta aresta : arestasQueSaem[atual]) {
                double novaDistancia = distancia[atual] + aresta.peso;
                if (novaDistancia < distancia[aresta.destino]) {
                    distancia[aresta.destino] = novaDistancia;
                    fila.add(new double[]{novaDistancia, aresta.destino});
                }
            }
        }
        return distancia;
    }

    static double[] calcularProximidade() {
        double[] proximidade = new double[totalVertices];
        for (int origem = 0; origem < totalVertices; origem++) {
            double[] distancia = menoresDistanciasDe(origem);
            double somaDasDistancias = 0;
            int verticesAlcancados = 0;
            for (int destino = 0; destino < totalVertices; destino++) {
                if (destino != origem && distancia[destino] != Double.MAX_VALUE) {
                    somaDasDistancias += distancia[destino];
                    verticesAlcancados++;
                }
            }
            if (somaDasDistancias > 0) proximidade[origem] = verticesAlcancados / somaDasDistancias;
        }
        return proximidade;
    }

    @SuppressWarnings("unchecked")
    static double[] calcularIntermediacao() {
        double[] intermediacao = new double[totalVertices];
        for (int origem = 0; origem < totalVertices; origem++) {
            ArrayList<Integer> ordemDeSaida = new ArrayList<>();
            ArrayList<Integer>[] predecessores = new ArrayList[totalVertices];
            for (int v = 0; v < totalVertices; v++) predecessores[v] = new ArrayList<>();
            double[] distancia = new double[totalVertices];
            double[] quantidadeDeCaminhos = new double[totalVertices];
            boolean[] jaProcessado = new boolean[totalVertices];
            Arrays.fill(distancia, Double.MAX_VALUE);
            distancia[origem] = 0;
            quantidadeDeCaminhos[origem] = 1;
            PriorityQueue<double[]> fila = new PriorityQueue<>((a, b) -> Double.compare(a[0], b[0]));
            fila.add(new double[]{0, origem});
            while (!fila.isEmpty()) {
                double[] item = fila.poll();
                int atual = (int) item[1];
                if (jaProcessado[atual]) continue;
                jaProcessado[atual] = true;
                ordemDeSaida.add(atual);
                for (Aresta aresta : arestasQueSaem[atual]) {
                    int destino = aresta.destino;
                    double novaDistancia = distancia[atual] + aresta.peso;
                    if (novaDistancia < distancia[destino] - TOLERANCIA) {
                        distancia[destino] = novaDistancia;
                        quantidadeDeCaminhos[destino] = quantidadeDeCaminhos[atual];
                        predecessores[destino].clear();
                        predecessores[destino].add(atual);
                        fila.add(new double[]{novaDistancia, destino});
                    } else if (Math.abs(novaDistancia - distancia[destino]) <= TOLERANCIA && !jaProcessado[destino]) {
                        quantidadeDeCaminhos[destino] += quantidadeDeCaminhos[atual];
                        predecessores[destino].add(atual);
                    }
                }
            }
            double[] contribuicao = new double[totalVertices];
            for (int i = ordemDeSaida.size() - 1; i >= 0; i--) {
                int vertice = ordemDeSaida.get(i);
                for (int predecessor : predecessores[vertice]) {
                    contribuicao[predecessor] += quantidadeDeCaminhos[predecessor] / quantidadeDeCaminhos[vertice] * (1 + contribuicao[vertice]);
                }
                if (vertice != origem) intermediacao[vertice] += contribuicao[vertice];
            }
        }
        if (!direcionado) {
            for (int v = 0; v < totalVertices; v++) intermediacao[v] /= 2;
        }
        return intermediacao;
    }

    static void gerarGrafoAleatorio(int quantidadeVertices, int quantidadeArestas, boolean precisaSerConexo) {
        criarGrafoVazio(quantidadeVertices, false);
        Random sorteio = new Random();
        HashSet<Long> paresJaUsados = new HashSet<>();
        int verticesUsaveis = precisaSerConexo ? quantidadeVertices : quantidadeVertices - 1;
        long maximoDeArestas = (long) verticesUsaveis * (verticesUsaveis - 1) / 2;
        if (quantidadeArestas > maximoDeArestas) {
            throw new IllegalArgumentException("Arestas demais para esse numero de nos (maximo " + maximoDeArestas + ")");
        }
        if (precisaSerConexo && quantidadeArestas < quantidadeVertices - 1) {
            throw new IllegalArgumentException("Para ser conexo precisa de pelo menos " + (quantidadeVertices - 1) + " arestas");
        }
        int arestasCriadas = 0;
        if (precisaSerConexo) {
            for (int novo = 1; novo < quantidadeVertices; novo++) {
                int existente = sorteio.nextInt(novo);
                paresJaUsados.add((long) existente * quantidadeVertices + novo);
                adicionarAresta(existente, novo, 1 + sorteio.nextInt(20));
                arestasCriadas++;
            }
        }
        while (arestasCriadas < quantidadeArestas) {
            int primeiro = sorteio.nextInt(verticesUsaveis);
            int segundo = sorteio.nextInt(verticesUsaveis);
            if (primeiro == segundo) continue;
            int menor = Math.min(primeiro, segundo);
            int maior = Math.max(primeiro, segundo);
            if (!paresJaUsados.add((long) menor * quantidadeVertices + maior)) continue;
            adicionarAresta(menor, maior, 1 + sorteio.nextInt(20));
            arestasCriadas++;
        }
    }

    static ArrayList<String> separarCamposCsv(String linha) {
        ArrayList<String> campos = new ArrayList<>();
        StringBuilder campoAtual = new StringBuilder();
        boolean dentroDeAspas = false;
        for (char caractere : linha.toCharArray()) {
            if (caractere == '"') {
                dentroDeAspas = !dentroDeAspas;
            } else if (caractere == ',' && !dentroDeAspas) {
                campos.add(campoAtual.toString());
                campoAtual.setLength(0);
            } else {
                campoAtual.append(caractere);
            }
        }
        campos.add(campoAtual.toString());
        return campos;
    }

    static double distanciaEmMetros(double latitude1, double longitude1, double latitude2, double longitude2) {
        double diferencaLatitude = Math.toRadians(latitude2 - latitude1);
        double diferencaLongitude = Math.toRadians(longitude2 - longitude1);
        double a = Math.sin(diferencaLatitude / 2) * Math.sin(diferencaLatitude / 2)
                + Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2))
                * Math.sin(diferencaLongitude / 2) * Math.sin(diferencaLongitude / 2);
        return 6371000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    static BufferedReader abrirArquivoPulandoCabecalho(String nomeArquivo) throws IOException {
        BufferedReader leitor = new BufferedReader(new InputStreamReader(new FileInputStream(nomeArquivo), "UTF-8"));
        leitor.readLine();
        return leitor;
    }

    static void construirRedeDeOnibus(String arquivoParadas, String arquivoViagens, String arquivoHorarios) throws IOException {
        ArrayList<String> nomesDasParadas = new ArrayList<>();
        ArrayList<double[]> coordenadasDasParadas = new ArrayList<>();
        HashMap<String, Integer> posicaoDaParadaPeloId = new HashMap<>();
        BufferedReader leitor = abrirArquivoPulandoCabecalho(arquivoParadas);
        String linha;
        while ((linha = leitor.readLine()) != null) {
            if (linha.trim().isEmpty()) continue;
            ArrayList<String> campos = separarCamposCsv(linha);
            posicaoDaParadaPeloId.put(campos.get(0), nomesDasParadas.size());
            nomesDasParadas.add(campos.get(0) + " - " + campos.get(2).replace('"', '\''));
            coordenadasDasParadas.add(new double[]{Double.parseDouble(campos.get(4)), Double.parseDouble(campos.get(5))});
        }
        leitor.close();

        criarGrafoVazio(nomesDasParadas.size(), true);
        for (int v = 0; v < totalVertices; v++) nomesVertices[v] = nomesDasParadas.get(v);

        HashSet<String> viagensValidas = new HashSet<>();
        leitor = abrirArquivoPulandoCabecalho(arquivoViagens);
        while ((linha = leitor.readLine()) != null) {
            if (linha.trim().isEmpty()) continue;
            viagensValidas.add(separarCamposCsv(linha).get(2));
        }
        leitor.close();

        HashSet<Long> paresJaCriados = new HashSet<>();
        ArrayList<Integer> paradasDaViagem = new ArrayList<>();
        String viagemAtual = "";
        leitor = abrirArquivoPulandoCabecalho(arquivoHorarios);
        while (true) {
            linha = leitor.readLine();
            String viagemDaLinha = "";
            ArrayList<String> campos = null;
            if (linha != null && !linha.trim().isEmpty()) {
                campos = separarCamposCsv(linha);
                viagemDaLinha = campos.get(0);
            } else if (linha != null) {
                continue;
            }
            if (!viagemDaLinha.equals(viagemAtual)) {
                ligarParadasDaViagem(paradasDaViagem, coordenadasDasParadas, paresJaCriados);
                paradasDaViagem.clear();
                viagemAtual = viagemDaLinha;
            }
            if (linha == null) break;
            if (viagensValidas.contains(viagemDaLinha) && posicaoDaParadaPeloId.containsKey(campos.get(3))) {
                paradasDaViagem.add(posicaoDaParadaPeloId.get(campos.get(3)));
            }
        }
        leitor.close();
    }

    static void ligarParadasDaViagem(ArrayList<Integer> paradasDaViagem, ArrayList<double[]> coordenadas, HashSet<Long> paresJaCriados) {
        for (int inicio = 0; inicio < paradasDaViagem.size(); inicio++) {
            int paradaOrigem = paradasDaViagem.get(inicio);
            double distanciaAcumulada = 0;
            for (int salto = 1; salto <= MAXIMO_PARADAS_PULADAS + 1 && inicio + salto < paradasDaViagem.size(); salto++) {
                int paradaAnterior = paradasDaViagem.get(inicio + salto - 1);
                int paradaDestino = paradasDaViagem.get(inicio + salto);
                distanciaAcumulada += distanciaEmMetros(
                        coordenadas.get(paradaAnterior)[0], coordenadas.get(paradaAnterior)[1],
                        coordenadas.get(paradaDestino)[0], coordenadas.get(paradaDestino)[1]);
                long chaveDoPar = (long) paradaOrigem * totalVertices + paradaDestino;
                if (paradaOrigem != paradaDestino && paresJaCriados.add(chaveDoPar)) {
                    adicionarAresta(paradaOrigem, paradaDestino, Math.round(distanciaAcumulada * 10) / 10.0);
                }
            }
        }
    }

    static void mostrarRanking(double[] valores, String nomeArquivo) throws IOException {
        PrintWriter escritor = new PrintWriter(new OutputStreamWriter(new FileOutputStream(nomeArquivo), "UTF-8"));
        for (int v = 0; v < totalVertices; v++) escritor.println((v + 1) + " \"" + nomesVertices[v] + "\" " + valores[v]);
        escritor.close();
        Integer[] ordem = new Integer[totalVertices];
        for (int v = 0; v < totalVertices; v++) ordem[v] = v;
        Arrays.sort(ordem, (a, b) -> Double.compare(valores[b], valores[a]));
        System.out.println("Top 10 (resultado completo em " + nomeArquivo + "):");
        for (int posicao = 0; posicao < Math.min(10, totalVertices); posicao++) {
            System.out.println((posicao + 1) + ". " + nomesVertices[ordem[posicao]] + " = " + valores[ordem[posicao]]);
        }
    }

    public static void main(String[] args) throws IOException {
        Scanner teclado = new Scanner(System.in);
        criarGrafoVazio(0, false);
        while (true) {
            System.out.println();
            System.out.println("Grafo atual: " + totalVertices + " nos, " + todasArestas.size() + " arestas, " + (direcionado ? "direcionado" : "nao direcionado"));
            System.out.println("1 Construir rede de onibus (stops, trips e stop_times)");
            System.out.println("2 Carregar grafo de arquivo Pajek");
            System.out.println("3 Salvar grafo em arquivo Pajek");
            System.out.println("4 Gerar grafo aleatorio");
            System.out.println("5 Verificar se e conexo");
            System.out.println("6 Mostrar componentes");
            System.out.println("7 Verificar se e Euleriano");
            System.out.println("8 Verificar se e Ciclico");
            System.out.println("9 Centralidade de Proximidade");
            System.out.println("10 Centralidade de Intermediacao");
            System.out.println("0 Sair");
            System.out.print("Opcao: ");
            if (!teclado.hasNext()) break;
            String opcao = teclado.next();
            try {
                if (opcao.equals("0")) {
                    break;
                } else if (opcao.equals("1")) {
                    construirRedeDeOnibus("stops.txt", "trips.txt", "stop_times.txt");
                    System.out.println("Rede criada.");
                } else if (opcao.equals("2")) {
                    System.out.print("Arquivo: ");
                    carregarGrafo(teclado.next());
                    System.out.println("Grafo carregado.");
                } else if (opcao.equals("3")) {
                    System.out.print("Arquivo: ");
                    salvarGrafo(teclado.next());
                    System.out.println("Grafo salvo.");
                } else if (opcao.equals("4")) {
                    System.out.print("Nos: ");
                    int quantidadeVertices = teclado.nextInt();
                    System.out.print("Arestas: ");
                    int quantidadeArestas = teclado.nextInt();
                    System.out.print("Conexo (s/n): ");
                    boolean precisaSerConexo = teclado.next().equalsIgnoreCase("s");
                    gerarGrafoAleatorio(quantidadeVertices, quantidadeArestas, precisaSerConexo);
                    System.out.println("Grafo gerado.");
                } else if (opcao.equals("5")) {
                    System.out.println(grafoEhConexo() ? "O grafo e conexo." : "O grafo e desconexo.");
                } else if (opcao.equals("6")) {
                    mostrarComponentes();
                } else if (opcao.equals("7")) {
                    System.out.println(grafoEhEuleriano() ? "O grafo e Euleriano." : "O grafo nao e Euleriano.");
                } else if (opcao.equals("8")) {
                    System.out.println(grafoEhCiclico() ? "O grafo e ciclico." : "O grafo e aciclico.");
                } else if (opcao.equals("9")) {
                    mostrarRanking(calcularProximidade(), "proximidade.txt");
                } else if (opcao.equals("10")) {
                    mostrarRanking(calcularIntermediacao(), "intermediacao.txt");
                } else {
                    System.out.println("Opcao invalida.");
                }
            } catch (Exception erro) {
                System.out.println("Erro: " + erro.getMessage());
            }
        }
    }
}
