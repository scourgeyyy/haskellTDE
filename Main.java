import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Main {
    static int n;
    static boolean dirigido;
    static String[] rotulos;
    static ArrayList<double[]> arestas = new ArrayList<>();
    static ArrayList<double[]>[] adj;
    static final double EPS = 1e-9;

    // Cria um grafo vazio com 'total' vertices; dir indica se e direcionado
    @SuppressWarnings("unchecked")
    static void iniciar(int total, boolean dir) {
        n = total;
        dirigido = dir;
        rotulos = new String[n];
        for (int i = 0; i < n; i++) rotulos[i] = "No " + (i + 1);
        arestas = new ArrayList<>();
        adj = new ArrayList[n];
        for (int i = 0; i < n; i++) adj[i] = new ArrayList<>();
    }

    // Adiciona uma aresta/arco com peso; em grafo nao direcionado tambem registra o sentido contrario
    static void addAresta(int u, int v, double w) {
        if (u < 0 || u >= n || v < 0 || v >= n)
            throw new IllegalArgumentException("Vertice invalido: " + (u + 1) + " -> " + (v + 1));
        arestas.add(new double[]{u, v, w});
        adj[u].add(new double[]{v, w});
        if (!dirigido && u != v) adj[v].add(new double[]{u, w});
    }

    // Procura o arquivo na pasta atual e, se nao achar, na subpasta PJBL2
    static File resolverArquivo(String nome) {
        File f = new File(nome);
        if (f.exists()) return f;
        File sub = new File("PJBL2", nome);
        return sub.exists() ? sub : f;
    }

    // Remove comentarios iniciados por // e espacos das pontas (o exemplo Pajek do enunciado tem // na linha *Edges)
    static String limparLinha(String l) {
        int c = l.indexOf("//");
        return (c >= 0 ? l.substring(0, c) : l).trim();
    }

    // 1. Gravacao em Formato Pajek
    // Escreve *Vertices, os rotulos entre aspas, depois *Arcs (direcionado) ou *Edges com 'origem destino peso' (indices a partir de 1)
    static void salvar(String arquivo) throws IOException {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(arquivo), StandardCharsets.UTF_8))) {
            pw.println("*Vertices " + n);
            for (int i = 0; i < n; i++) pw.println((i + 1) + " \"" + rotulos[i].replace("\"", "'") + "\"");
            pw.println(dirigido ? "*Arcs" : "*Edges");
            for (double[] a : arestas) pw.println(((int) a[0] + 1) + " " + ((int) a[1] + 1) + " " + a[2]);
        }
    }

    // 2. Carregamento de Arquivo Pajek
    // Le o cabecalho *Vertices, os rotulos e as arestas; *Arcs marca o grafo como direcionado; peso ausente vale 1.0
    static void carregar(String arquivo) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(resolverArquivo(arquivo)), StandardCharsets.UTF_8))) {
            String linha;
            int total = -1;
            while ((linha = br.readLine()) != null) {
                linha = limparLinha(linha);
                if (!linha.isEmpty() && linha.toLowerCase().startsWith("*vertices")) {
                    total = Integer.parseInt(linha.split("\\s+")[1]);
                    break;
                }
            }
            if (total < 0) throw new IOException("Cabecalho *Vertices nao encontrado.");
            iniciar(total, false);
            boolean lendoVertices = true;
            while ((linha = br.readLine()) != null) {
                linha = limparLinha(linha);
                if (linha.isEmpty()) continue;
                if (linha.startsWith("*")) {
                    lendoVertices = false;
                    dirigido = linha.toLowerCase().startsWith("*arcs");
                    continue;
                }
                if (lendoVertices) {
                    int p = linha.indexOf('"'), q = linha.lastIndexOf('"');
                    if (p >= 0 && q > p) {
                        rotulos[Integer.parseInt(linha.substring(0, p).trim()) - 1] = linha.substring(p + 1, q);
                    } else {
                        String[] t = linha.split("\\s+", 2);
                        rotulos[Integer.parseInt(t[0]) - 1] = t.length > 1 ? t[1] : ("No " + t[0]);
                    }
                } else {
                    String[] t = linha.split("\\s+");
                    addAresta(Integer.parseInt(t[0]) - 1, Integer.parseInt(t[1]) - 1, t.length > 2 ? Double.parseDouble(t[2]) : 1.0);
                }
            }
        }
    }

    // 3 e 4. Conexidade e Componentes Fracamente Conectados
    // Lista de vizinhos ignorando a direcao das arestas (base da conexidade fraca)
    @SuppressWarnings("unchecked")
    static ArrayList<Integer>[] vizinhosFracos() {
        ArrayList<Integer>[] viz = new ArrayList[n];
        for (int i = 0; i < n; i++) viz[i] = new ArrayList<>();
        for (double[] a : arestas) {
            int u = (int) a[0], v = (int) a[1];
            viz[u].add(v);
            if (u != v) viz[v].add(u);
        }
        return viz;
    }

    // BFS a partir de cada vertice nao visitado; retorna o numero do componente de cada vertice
    static int[] componentes() {
        int[] comp = new int[n];
        Arrays.fill(comp, -1);
        ArrayList<Integer>[] viz = vizinhosFracos();
        int c = 0;
        for (int i = 0; i < n; i++) {
            if (comp[i] != -1) continue;
            ArrayDeque<Integer> fila = new ArrayDeque<>();
            fila.add(i);
            comp[i] = c;
            while (!fila.isEmpty()) {
                int v = fila.poll();
                for (int w : viz[v]) {
                    if (comp[w] == -1) {
                        comp[w] = c;
                        fila.add(w);
                    }
                }
            }
            c++;
        }
        return comp;
    }

    // Quantidade de componentes (maior numero de componente + 1)
    static int contar(int[] comp) {
        int max = -1;
        for (int c : comp) max = Math.max(max, c);
        return max + 1;
    }

    // 3. Funcao que verifica se o grafo e conexo: verdadeiro quando existe um unico componente
    static boolean conexo() {
        return n == 0 || contar(componentes()) == 1;
    }

    // 4. Funcao que mostra os componentes: imprime quantidade, tamanho e os vertices de cada um (fracamente conectados se direcionado)
    @SuppressWarnings("unchecked")
    static void mostrarComponentes() {
        if (n == 0) { System.out.println("Grafo vazio."); return; }
        int[] comp = componentes();
        int total = contar(comp);
        if (total == 1) { System.out.println("O grafo e conexo (possui apenas 1 componente)."); return; }
        System.out.println((dirigido ? "Componentes fracamente conectados: " : "Componentes: ") + total);
        ArrayList<Integer>[] grupos = new ArrayList[total];
        for (int c = 0; c < total; c++) grupos[c] = new ArrayList<>();
        for (int i = 0; i < n; i++) grupos[comp[i]].add(i + 1);
        for (int c = 0; c < total; c++) {
            ArrayList<Integer> g = grupos[c];
            System.out.print("Componente " + (c + 1) + " (" + g.size() + " nos): ");
            System.out.println(g.size() <= 20 ? g : g.subList(0, 20) + " ... (+" + (g.size() - 20) + " nos)");
        }
    }

    // 5. Verificacao de Grafo Euleriano
    // Verdadeiro se existe circuito Euleriano: graus pares (ou entrada = saida no direcionado) e arestas todas em um unico componente
    static boolean euleriano() {
        if (arestas.isEmpty()) return false;
        int[] ent = new int[n], sai = new int[n];
        for (double[] a : arestas) { sai[(int) a[0]]++; ent[(int) a[1]]++; }
        int[] comp = componentes();
        int alvo = -1;
        for (int i = 0; i < n; i++) {
            if (dirigido ? ent[i] != sai[i] : (ent[i] + sai[i]) % 2 != 0) return false;
            if (ent[i] + sai[i] > 0) {
                if (alvo == -1) alvo = comp[i];
                else if (comp[i] != alvo) return false;
            }
        }
        return alvo != -1;
    }

    // 6. Verificacao de Grafo Ciclico
    // Union-find: encontra a raiz do conjunto de x com compressao de caminho
    static int achar(int[] pai, int x) {
        while (pai[x] != x) { pai[x] = pai[pai[x]]; x = pai[x]; }
        return x;
    }

    // Nao direcionado: aresta ligando dois vertices do mesmo conjunto fecha ciclo (union-find). Direcionado: se a ordenacao topologica de Kahn nao remove todos os vertices, ha ciclo
    static boolean ciclico() {
        if (n == 0 || arestas.isEmpty()) return false;
        if (!dirigido) {
            int[] pai = new int[n];
            for (int i = 0; i < n; i++) pai[i] = i;
            for (double[] a : arestas) {
                int x = achar(pai, (int) a[0]), y = achar(pai, (int) a[1]);
                if (x == y) return true;
                pai[x] = y;
            }
            return false;
        }
        int[] grauEnt = new int[n];
        for (double[] a : arestas) grauEnt[(int) a[1]]++;
        ArrayDeque<Integer> fila = new ArrayDeque<>();
        for (int i = 0; i < n; i++) if (grauEnt[i] == 0) fila.add(i);
        int removidos = 0;
        while (!fila.isEmpty()) {
            int v = fila.poll();
            removidos++;
            for (double[] e : adj[v]) if (--grauEnt[(int) e[0]] == 0) fila.add((int) e[0]);
        }
        return removidos < n;
    }

    // 7. Centralidade de Proximidade - Dijkstra + Wasserman-Faust
    // Menores distancias (soma dos pesos) do vertice s para todos os outros, com fila de prioridade
    static double[] dijkstra(int s) {
        double[] dist = new double[n];
        Arrays.fill(dist, Double.POSITIVE_INFINITY);
        dist[s] = 0.0;
        PriorityQueue<double[]> pq = new PriorityQueue<>((a, b) -> Double.compare(a[0], b[0]));
        pq.add(new double[]{0.0, s});
        while (!pq.isEmpty()) {
            double[] topo = pq.poll();
            int v = (int) topo[1];
            if (topo[0] > dist[v] + EPS) continue;
            for (double[] e : adj[v]) {
                int w = (int) e[0];
                double nd = dist[v] + e[1];
                if (nd < dist[w] - EPS) { dist[w] = nd; pq.add(new double[]{nd, w}); }
            }
        }
        return dist;
    }

    // Para cada no: alcancados^2 / ((n-1) * soma das distancias dos melhores caminhos); nos sem alcance ficam com 0
    static double[] proximidade() {
        double[] c = new double[n];
        if (n <= 1) return c;
        for (int s = 0; s < n; s++) {
            double[] d = dijkstra(s);
            double soma = 0.0;
            int alc = 0;
            for (int i = 0; i < n; i++) {
                if (i != s && Double.isFinite(d[i])) { soma += d[i]; alc++; }
            }
            if (alc > 0 && soma > 0.0) c[s] = ((double) alc * alc) / ((n - 1.0) * soma);
        }
        return c;
    }

    // 8. Centralidade de Intermediacao - Brandes Ponderado
    // Para cada origem faz um Dijkstra contando caminhos minimos (sigma) e predecessores; depois acumula, na ordem inversa, a fracao de caminhos que passa por cada no. Nao direcionado divide por 2
    @SuppressWarnings("unchecked")
    static double[] intermediacao() {
        double[] cb = new double[n];
        if (n <= 2) return cb;
        for (int s = 0; s < n; s++) {
            ArrayList<Integer> pilha = new ArrayList<>();
            ArrayList<Integer>[] pred = new ArrayList[n];
            for (int i = 0; i < n; i++) pred[i] = new ArrayList<>();
            double[] dist = new double[n], sigma = new double[n];
            boolean[] feito = new boolean[n];
            Arrays.fill(dist, Double.POSITIVE_INFINITY);
            dist[s] = 0.0;
            sigma[s] = 1.0;
            PriorityQueue<double[]> pq = new PriorityQueue<>((a, b) -> Double.compare(a[0], b[0]));
            pq.add(new double[]{0.0, s});
            while (!pq.isEmpty()) {
                double[] topo = pq.poll();
                int v = (int) topo[1];
                if (feito[v]) continue;
                feito[v] = true;
                pilha.add(v);
                for (double[] e : adj[v]) {
                    int w = (int) e[0];
                    double nd = dist[v] + e[1];
                    if (nd < dist[w] - EPS) {
                        dist[w] = nd; sigma[w] = sigma[v];
                        pred[w].clear(); pred[w].add(v);
                        pq.add(new double[]{nd, w});
                    } else if (Math.abs(nd - dist[w]) <= EPS && !feito[w]) {
                        sigma[w] += sigma[v]; pred[w].add(v);
                    }
                }
            }
            double[] delta = new double[n];
            for (int i = pilha.size() - 1; i >= 0; i--) {
                int w = pilha.get(i);
                for (int v : pred[w]) delta[v] += (sigma[v] / sigma[w]) * (1.0 + delta[w]);
                if (w != s) cb[w] += delta[w];
            }
        }
        if (!dirigido) for (int i = 0; i < n; i++) cb[i] /= 2.0;
        return cb;
    }

    // 9. Gerador de Grafo Aleatorio
    // Conexo: cria uma arvore ligando cada vertice a um anterior e depois completa com arestas sem repetir. Desconexo: o ultimo vertice nunca recebe aresta. Pesos de 1 a 50
    static void gerarAleatorio(int nos, int numArestas, boolean conexo) {
        if (nos <= 0) throw new IllegalArgumentException("Numero de nos deve ser positivo.");
        if (!conexo && nos < 2) throw new IllegalArgumentException("Grafo desconexo exige pelo menos 2 nos.");
        int usaveis = conexo ? nos : nos - 1;
        long max = (long) usaveis * (usaveis - 1) / 2;
        if (numArestas < 0 || numArestas > max) throw new IllegalArgumentException("Arestas invalidas (maximo " + max + ").");
        if (conexo && numArestas < nos - 1) throw new IllegalArgumentException("Conexo exige pelo menos " + (nos - 1) + " arestas.");
        iniciar(nos, false);
        Random r = new Random();
        HashSet<Long> usadas = new HashSet<>();
        int criadas = 0;
        if (conexo) {
            for (int i = 1; i < nos; i++) {
                int j = r.nextInt(i);
                usadas.add((long) j * nos + i);
                addAresta(j, i, 1 + r.nextInt(50));
                criadas++;
            }
        }
        while (criadas < numArestas) {
            int a = r.nextInt(usaveis), b = r.nextInt(usaveis);
            if (a == b) continue;
            int u = Math.min(a, b), v = Math.max(a, b);
            if (usadas.add((long) u * nos + v)) { addAresta(u, v, 1 + r.nextInt(50)); criadas++; }
        }
    }

    // 10. Modelagem GTFS: stops.txt + trips.txt + stop_times.txt
    // Separa uma linha CSV em campos respeitando virgulas dentro de aspas
    static ArrayList<String> campos(String linha) {
        ArrayList<String> lista = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean aspas = false;
        for (char c : linha.toCharArray()) {
            if (c == '"') aspas = !aspas;
            else if (c == ',' && !aspas) { lista.add(sb.toString().trim()); sb.setLength(0); }
            else sb.append(c);
        }
        lista.add(sb.toString().trim());
        return lista;
    }

    // Distancia em metros entre duas coordenadas (formula de Haversine)
    static double distancia(double la1, double lo1, double la2, double lo2) {
        double dla = Math.toRadians(la2 - la1), dlo = Math.toRadians(lo2 - lo1);
        double a = Math.sin(dla / 2) * Math.sin(dla / 2)
                + Math.cos(Math.toRadians(la1)) * Math.cos(Math.toRadians(la2)) * Math.sin(dlo / 2) * Math.sin(dlo / 2);
        return 6371000.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // Requisito de modelagem: paradas (stops.txt) sao vertices; para cada itinerario (um por shape_id do trips.txt) liga cada parada as 5 seguintes da viagem (stop_times.txt), peso = distancia acumulada em metros, sem pares repetidos
    static void construirRedeGTFS() throws IOException {
        File fStops = resolverArquivo("stops.txt"), fTrips = resolverArquivo("trips.txt"), fTimes = resolverArquivo("stop_times.txt");
        if (!fStops.exists() || !fTrips.exists() || !fTimes.exists())
            throw new FileNotFoundException("Arquivos stops.txt, trips.txt e stop_times.txt sao obrigatorios.");

        ArrayList<String> nomes = new ArrayList<>();
        ArrayList<double[]> pos = new ArrayList<>();
        HashMap<String, Integer> stopIdx = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fStops), StandardCharsets.UTF_8))) {
            br.readLine();
            String l;
            while ((l = br.readLine()) != null) {
                if (l.trim().isEmpty()) continue;
                ArrayList<String> c = campos(l);
                if (c.size() >= 6 && !stopIdx.containsKey(c.get(0))) {
                    stopIdx.put(c.get(0), nomes.size());
                    nomes.add(c.get(0) + " - " + c.get(2).replace('"', '\''));
                    pos.add(new double[]{Double.parseDouble(c.get(4)), Double.parseDouble(c.get(5))});
                }
            }
        }

        HashSet<String> shapes = new HashSet<>(), tripsSel = new HashSet<>();
        int totalTrips = 0;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fTrips), StandardCharsets.UTF_8))) {
            br.readLine();
            String l;
            while ((l = br.readLine()) != null) {
                if (l.trim().isEmpty()) continue;
                ArrayList<String> c = campos(l);
                if (c.size() < 3) continue;
                totalTrips++;
                String shape = (c.size() > 7 && !c.get(7).isEmpty()) ? c.get(7) : (c.get(0) + "-" + c.get(2));
                if (shapes.add(shape)) tripsSel.add(c.get(2));
            }
        }
        System.out.println("Paradas (stops.txt): " + nomes.size() + " | Viagens (trips.txt): " + totalTrips + " (" + tripsSel.size() + " itinerarios)");

        HashMap<String, ArrayList<int[]>> seqTrip = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fTimes), StandardCharsets.UTF_8))) {
            br.readLine();
            String l;
            while ((l = br.readLine()) != null) {
                if (l.trim().isEmpty()) continue;
                ArrayList<String> c = campos(l);
                if (c.size() >= 5 && tripsSel.contains(c.get(0)) && stopIdx.containsKey(c.get(3)))
                    seqTrip.computeIfAbsent(c.get(0), k -> new ArrayList<>()).add(new int[]{stopIdx.get(c.get(3)), Integer.parseInt(c.get(4))});
            }
        }

        iniciar(nomes.size(), true);
        for (int i = 0; i < n; i++) rotulos[i] = nomes.get(i);
        HashSet<Long> usadas = new HashSet<>();
        for (ArrayList<int[]> lista : seqTrip.values()) {
            lista.sort((a, b) -> Integer.compare(a[1], b[1]));
            for (int i = 0; i < lista.size() - 1; i++) {
                int u = lista.get(i)[0];
                double distAcum = 0.0;
                for (int passo = 1; passo <= 5 && i + passo < lista.size(); passo++) {
                    int ant = lista.get(i + passo - 1)[0], v = lista.get(i + passo)[0];
                    double trecho = Math.max(1.0, Math.round(distancia(pos.get(ant)[0], pos.get(ant)[1], pos.get(v)[0], pos.get(v)[1]) * 10.0) / 10.0);
                    distAcum = Math.round((distAcum + trecho) * 10.0) / 10.0;
                    if (u != v && usadas.add((long) u * n + v)) addAresta(u, v, distAcum);
                }
            }
        }
    }

    // Grava o valor de cada no no arquivo e mostra os 10 maiores na tela
    static void mostrarRanking(double[] valores, String arquivo) throws IOException {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(arquivo), StandardCharsets.UTF_8))) {
            for (int i = 0; i < n; i++) pw.println((i + 1) + " \"" + rotulos[i] + "\" " + valores[i]);
        }
        Integer[] ordem = new Integer[n];
        for (int i = 0; i < n; i++) ordem[i] = i;
        Arrays.sort(ordem, (a, b) -> Double.compare(valores[b], valores[a]));
        System.out.println("Top 10 (completo em " + arquivo + "):");
        for (int i = 0; i < Math.min(10, n); i++)
            System.out.printf(Locale.US, "%d. %s = %.8f%n", (i + 1), rotulos[ordem[i]], valores[ordem[i]]);
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        iniciar(0, false);
        while (true) {
            System.out.println("\n==========================================================");
            System.out.println("Grafo: " + n + " nos | " + arestas.size() + (dirigido ? " arcos (direcionado)" : " arestas (nao direcionado)"));
            System.out.println("1 - Construir rede GTFS (stops + trips + stop_times)");
            System.out.println("2 - Carregar grafo Pajek   | 3 - Salvar grafo Pajek");
            System.out.println("4 - Gerar grafo aleatorio  | 5 - Verificar conexidade");
            System.out.println("6 - Mostrar componentes    | 7 - Verificar Euleriano");
            System.out.println("8 - Verificar Ciclico      | 9 - Centralidade Proximidade");
            System.out.println("10 - Centralidade Intermediacao | 0 - Sair");
            System.out.print("Opcao: ");
            if (!sc.hasNext()) break;
            String op = sc.next();
            try {
                if (op.equals("0")) break;
                else if (op.equals("1")) { construirRedeGTFS(); System.out.println("Rede GTFS construida!"); }
                else if (op.equals("2")) { System.out.print("Arquivo: "); carregar(sc.next()); System.out.println("Carregado!"); }
                else if (op.equals("3")) { System.out.print("Arquivo: "); salvar(sc.next()); System.out.println("Salvo!"); }
                else if (op.equals("4")) {
                    System.out.print("Nos: "); int a = sc.nextInt();
                    System.out.print("Arestas: "); int b = sc.nextInt();
                    System.out.print("Conexo (s/n): "); boolean c = sc.next().equalsIgnoreCase("s");
                    gerarAleatorio(a, b, c); System.out.println("Gerado!");
                }
                else if (op.equals("5")) System.out.println(conexo() ? "O grafo E conexo." : "O grafo NAO e conexo.");
                else if (op.equals("6")) mostrarComponentes();
                else if (op.equals("7")) System.out.println(euleriano() ? "O grafo E Euleriano." : "O grafo NAO e Euleriano.");
                else if (op.equals("8")) System.out.println(ciclico() ? "O grafo E ciclico." : "O grafo NAO e ciclico.");
                else if (op.equals("9")) mostrarRanking(proximidade(), "proximidade.txt");
                else if (op.equals("10")) mostrarRanking(intermediacao(), "intermediacao.txt");
                else System.out.println("Opcao invalida.");
            } catch (Exception e) { System.out.println("Erro: " + e.getMessage()); }
        }
    }
}
