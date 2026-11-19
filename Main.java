import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Main {
    // Quantidade de vertices do grafo atual
    static int n;
    // true = grafo direcionado (arcos, *Arcs no Pajek); false = nao direcionado (arestas, *Edges)
    static boolean dirigido;
    // rotulos[i] e o nome do vertice i (o vertice i aparece como i+1 para o usuario e no arquivo Pajek)
    static String[] rotulos;
    // Lista de todas as arestas na ordem em que foram criadas; cada item e {origem, destino, peso}
    static ArrayList<double[]> arestas = new ArrayList<>();
    // Lista de adjacencia: adj[v] guarda as arestas que SAEM de v, cada item e {destino, peso}
    static ArrayList<double[]>[] adj;
    // Tolerancia para comparar numeros decimais (dois caminhos com diferenca menor que isso sao considerados empatados)
    static final double EPS = 1e-9;

    // Cria um grafo novo e vazio com 'total' vertices. dir diz se sera direcionado.
    @SuppressWarnings("unchecked")
    static void iniciar(int total, boolean dir) {
        // Passo 1: guarda o numero de vertices e se o grafo e direcionado
        n = total;
        dirigido = dir;
        // Passo 2: cria o vetor de rotulos e coloca um nome padrao "No 1", "No 2", ... em cada vertice
        rotulos = new String[n];
        for (int i = 0; i < n; i++) rotulos[i] = "No " + (i + 1);
        // Passo 3: descarta as arestas antigas criando uma lista nova
        arestas = new ArrayList<>();
        // Passo 4: cria uma lista de adjacencia vazia para cada vertice
        adj = new ArrayList[n];
        for (int i = 0; i < n; i++) adj[i] = new ArrayList<>();
    }

    // Insere a aresta/arco u -> v com peso w (u e v comecam em 0)
    static void addAresta(int u, int v, double w) {
        // Passo 1: se algum vertice nao existe (fora de 0 .. n-1), interrompe com erro
        if (u < 0 || u >= n || v < 0 || v >= n)
            throw new IllegalArgumentException("Vertice invalido: " + (u + 1) + " -> " + (v + 1));
        // Passo 2: guarda a aresta na lista geral (usada para salvar, contar graus, union-find...)
        arestas.add(new double[]{u, v, w});
        // Passo 3: registra v como vizinho de u
        adj[u].add(new double[]{v, w});
        // Passo 4: se NAO e direcionado, a aresta vale nos dois sentidos, entao registra u como vizinho de v
        // (se u == v e um laco, entao nao duplica)
        if (!dirigido && u != v) adj[v].add(new double[]{u, w});
    }

    // Descobre onde esta o arquivo: na pasta atual ou, se nao existir la, na subpasta PJBL2
    static File resolverArquivo(String nome) {
        // Passo 1: tenta o nome exatamente como foi digitado
        File f = new File(nome);
        if (f.exists()) return f;
        // Passo 2: tenta dentro da pasta PJBL2
        File sub = new File("PJBL2", nome);
        // Passo 3: se existir na subpasta usa ela, senao devolve o original (que vai dar erro de arquivo nao encontrado)
        return sub.exists() ? sub : f;
    }

    // Remove comentarios de uma linha de arquivo Pajek: tudo depois de "//" e os espacos das pontas
    static String limparLinha(String l) {
        // Passo 1: procura a posicao de "//"
        int c = l.indexOf("//");
        // Passo 2: se achou, fica so com o que vem antes; senao usa a linha inteira; depois tira espacos das pontas
        // (necessario porque o exemplo do enunciado tem "*Edges // para nao direcionado ou *Arcs ...")
        return (c >= 0 ? l.substring(0, c) : l).trim();
    }

    // 1. Gravacao em Formato Pajek
    // Grava o grafo atual (ponderado e rotulado) em um arquivo no formato Pajek
    static void salvar(String arquivo) throws IOException {
        // Passo 1: abre o arquivo para escrita em UTF-8 (o try fecha o arquivo sozinho no fim)
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(arquivo), StandardCharsets.UTF_8))) {
            // Passo 2: primeira linha com o numero de vertices
            pw.println("*Vertices " + n);
            // Passo 3: uma linha por vertice: numero (a partir de 1) e rotulo entre aspas (aspas internas viram ')
            for (int i = 0; i < n; i++) pw.println((i + 1) + " \"" + rotulos[i].replace("\"", "'") + "\"");
            // Passo 4: cabecalho da secao de ligacoes: *Arcs se direcionado, *Edges se nao
            pw.println(dirigido ? "*Arcs" : "*Edges");
            // Passo 5: uma linha por aresta: origem destino peso (soma 1 para voltar a numeracao a partir de 1)
            for (double[] a : arestas) pw.println(((int) a[0] + 1) + " " + ((int) a[1] + 1) + " " + a[2]);
        }
    }

    // 2. Carregamento de Arquivo Pajek
    // Le um arquivo Pajek e substitui o grafo atual por ele
    static void carregar(String arquivo) throws IOException {
        // Passo 1: abre o arquivo (procurando tambem em PJBL2) em UTF-8
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(resolverArquivo(arquivo)), StandardCharsets.UTF_8))) {
            String linha;
            int total = -1;
            // Passo 2: le linhas ate achar "*Vertices N" e guarda N (linhas vazias e comentarios sao ignorados)
            while ((linha = br.readLine()) != null) {
                linha = limparLinha(linha);
                if (!linha.isEmpty() && linha.toLowerCase().startsWith("*vertices")) {
                    total = Integer.parseInt(linha.split("\\s+")[1]);
                    break;
                }
            }
            // Passo 3: se nunca encontrou o cabecalho, o arquivo nao e valido
            if (total < 0) throw new IOException("Cabecalho *Vertices nao encontrado.");
            // Passo 4: cria o grafo vazio com N vertices (ainda nao direcionado)
            iniciar(total, false);
            // Passo 5: comeca lendo vertices; quando aparecer uma linha com * passa para as arestas
            boolean lendoVertices = true;
            while ((linha = br.readLine()) != null) {
                linha = limparLinha(linha);
                if (linha.isEmpty()) continue;
                // Passo 6: linha que comeca com * = novo cabecalho (*Edges ou *Arcs)
                if (linha.startsWith("*")) {
                    lendoVertices = false;
                    // *Arcs significa grafo direcionado; qualquer outro cabecalho (*Edges) significa nao direcionado
                    dirigido = linha.toLowerCase().startsWith("*arcs");
                    continue;
                }
                if (lendoVertices) {
                    // Passo 7a: linha de vertice. Procura a primeira e a ultima aspas
                    int p = linha.indexOf('"'), q = linha.lastIndexOf('"');
                    if (p >= 0 && q > p) {
                        // Com aspas: o numero esta antes da primeira aspa e o rotulo entre as aspas
                        rotulos[Integer.parseInt(linha.substring(0, p).trim()) - 1] = linha.substring(p + 1, q);
                    } else {
                        // Sem aspas: primeiro valor e o numero, o resto da linha e o rotulo (ou "No N" se nao houver)
                        String[] t = linha.split("\\s+", 2);
                        rotulos[Integer.parseInt(t[0]) - 1] = t.length > 1 ? t[1] : ("No " + t[0]);
                    }
                } else {
                    // Passo 7b: linha de aresta "origem destino [peso]"; subtrai 1 dos vertices; peso padrao 1.0
                    String[] t = linha.split("\\s+");
                    addAresta(Integer.parseInt(t[0]) - 1, Integer.parseInt(t[1]) - 1, t.length > 2 ? Double.parseDouble(t[2]) : 1.0);
                }
            }
        }
    }

    // 3 e 4. Conexidade e Componentes Fracamente Conectados
    // Monta, para cada vertice, a lista de vizinhos IGNORANDO a direcao das arestas
    @SuppressWarnings("unchecked")
    static ArrayList<Integer>[] vizinhosFracos() {
        // Passo 1: cria uma lista vazia de vizinhos para cada vertice
        ArrayList<Integer>[] viz = new ArrayList[n];
        for (int i = 0; i < n; i++) viz[i] = new ArrayList<>();
        // Passo 2: para cada aresta u-v, coloca v na lista de u e u na lista de v
        for (double[] a : arestas) {
            int u = (int) a[0], v = (int) a[1];
            viz[u].add(v);
            // laco (u == v) nao e duplicado
            if (u != v) viz[v].add(u);
        }
        return viz;
    }

    // Retorna um vetor onde comp[i] e o numero do componente do vertice i (0, 1, 2, ...)
    static int[] componentes() {
        // Passo 1: -1 significa "vertice ainda nao visitado"
        int[] comp = new int[n];
        Arrays.fill(comp, -1);
        // Passo 2: vizinhos sem considerar direcao (conexidade fraca)
        ArrayList<Integer>[] viz = vizinhosFracos();
        // c e o numero do proximo componente
        int c = 0;
        // Passo 3: para cada vertice ainda nao visitado, comeca um novo componente
        for (int i = 0; i < n; i++) {
            if (comp[i] != -1) continue;
            // Passo 4: busca em largura (BFS) a partir de i
            ArrayDeque<Integer> fila = new ArrayDeque<>();
            fila.add(i);
            comp[i] = c;
            while (!fila.isEmpty()) {
                // tira um vertice da fila
                int v = fila.poll();
                // todos os vizinhos ainda nao visitados entram no mesmo componente e na fila
                for (int w : viz[v]) {
                    if (comp[w] == -1) {
                        comp[w] = c;
                        fila.add(w);
                    }
                }
            }
            // Passo 5: terminou um componente, o proximo vertice nao visitado abre o seguinte
            c++;
        }
        return comp;
    }

    // Conta quantos componentes existem: o maior numero de componente mais 1
    static int contar(int[] comp) {
        int max = -1;
        for (int c : comp) max = Math.max(max, c);
        return max + 1;
    }

    // 3. Funcao que verifica se o grafo e conexo
    // Conexo = existe um unico componente (grafo vazio conta como conexo)
    static boolean conexo() {
        return n == 0 || contar(componentes()) == 1;
    }

    // 4. Funcao que mostra os componentes do grafo caso seja desconexo
    @SuppressWarnings("unchecked")
    static void mostrarComponentes() {
        // Passo 1: grafo vazio nao tem componentes
        if (n == 0) { System.out.println("Grafo vazio."); return; }
        // Passo 2: calcula os componentes e quantos sao
        int[] comp = componentes();
        int total = contar(comp);
        // Passo 3: se ha so um, o grafo e conexo e nao ha o que listar
        if (total == 1) { System.out.println("O grafo e conexo (possui apenas 1 componente)."); return; }
        // Passo 4: mostra quantos componentes (fracamente conectados se for direcionado)
        System.out.println((dirigido ? "Componentes fracamente conectados: " : "Componentes: ") + total);
        // Passo 5: agrupa os vertices por componente (numerando os vertices a partir de 1)
        ArrayList<Integer>[] grupos = new ArrayList[total];
        for (int c = 0; c < total; c++) grupos[c] = new ArrayList<>();
        for (int i = 0; i < n; i++) grupos[comp[i]].add(i + 1);
        // Passo 6: imprime cada componente com seu tamanho; se tem mais de 20 vertices mostra so os 20 primeiros
        for (int c = 0; c < total; c++) {
            ArrayList<Integer> g = grupos[c];
            System.out.print("Componente " + (c + 1) + " (" + g.size() + " nos): ");
            System.out.println(g.size() <= 20 ? g : g.subList(0, 20) + " ... (+" + (g.size() - 20) + " nos)");
        }
    }

    // 5. Verificacao de Grafo Euleriano
    // Verdadeiro se existe um circuito que passa por todas as arestas exatamente uma vez
    static boolean euleriano() {
        // Passo 1: sem arestas nao ha circuito Euleriano
        if (arestas.isEmpty()) return false;
        // Passo 2: conta grau de entrada (ent) e de saida (sai) de cada vertice
        int[] ent = new int[n], sai = new int[n];
        for (double[] a : arestas) { sai[(int) a[0]]++; ent[(int) a[1]]++; }
        // Passo 3: descobre o componente de cada vertice
        int[] comp = componentes();
        // alvo guarda o componente do primeiro vertice que tem aresta (-1 = ainda nao achou)
        int alvo = -1;
        for (int i = 0; i < n; i++) {
            // Passo 4: regra dos graus. Direcionado: entrada deve ser igual a saida.
            // Nao direcionado: grau (ent + sai) deve ser par. Se falhar em um vertice, nao e Euleriano
            if (dirigido ? ent[i] != sai[i] : (ent[i] + sai[i]) % 2 != 0) return false;
            // Passo 5: regra da conectividade. Todos os vertices COM aresta devem estar no mesmo componente
            // (vertices isolados sao ignorados)
            if (ent[i] + sai[i] > 0) {
                if (alvo == -1) alvo = comp[i];
                else if (comp[i] != alvo) return false;
            }
        }
        // Passo 6: passou nas duas regras (e achou ao menos um vertice com aresta)
        return alvo != -1;
    }

    // 6. Verificacao de Grafo Ciclico
    // Union-find: encontra a "raiz" do conjunto do vertice x, encurtando o caminho no processo
    static int achar(int[] pai, int x) {
        // sobe de pai em pai ate chegar na raiz (quem e pai de si mesmo)
        while (pai[x] != x) { pai[x] = pai[pai[x]]; x = pai[x]; }
        return x;
    }

    // Verdadeiro se o grafo possui pelo menos um ciclo
    static boolean ciclico() {
        // Passo 1: sem vertices ou sem arestas nao ha ciclo
        if (n == 0 || arestas.isEmpty()) return false;
        if (!dirigido) {
            // NAO DIRECIONADO: usa union-find
            // Passo 2: no inicio cada vertice e seu proprio conjunto
            int[] pai = new int[n];
            for (int i = 0; i < n; i++) pai[i] = i;
            // Passo 3: para cada aresta, olha a raiz dos dois lados
            for (double[] a : arestas) {
                int x = achar(pai, (int) a[0]), y = achar(pai, (int) a[1]);
                // se ja estao no mesmo conjunto, essa aresta fecha um ciclo
                if (x == y) return true;
                // senao junta os dois conjuntos
                pai[x] = y;
            }
            // Passo 4: nenhuma aresta fechou ciclo
            return false;
        }
        // DIRECIONADO: algoritmo de Kahn (ordenacao topologica)
        // Passo 2: conta quantos arcos chegam em cada vertice
        int[] grauEnt = new int[n];
        for (double[] a : arestas) grauEnt[(int) a[1]]++;
        // Passo 3: comeca com os vertices em que nao chega nenhum arco
        ArrayDeque<Integer> fila = new ArrayDeque<>();
        for (int i = 0; i < n; i++) if (grauEnt[i] == 0) fila.add(i);
        int removidos = 0;
        // Passo 4: remove um vertice, "apaga" os arcos que saem dele e, se algum destino ficar sem
        // arcos chegando, ele tambem entra na fila
        while (!fila.isEmpty()) {
            int v = fila.poll();
            removidos++;
            for (double[] e : adj[v]) if (--grauEnt[(int) e[0]] == 0) fila.add((int) e[0]);
        }
        // Passo 5: se sobraram vertices que nunca foram removidos, eles estao em ciclo
        return removidos < n;
    }

    // 7. Centralidade de Proximidade - Dijkstra + Wasserman-Faust
    // Dijkstra: menor distancia (soma dos pesos) do vertice s ate todos os outros
    static double[] dijkstra(int s) {
        // Passo 1: todas as distancias comecam infinitas, exceto a de s para ele mesmo (0)
        double[] dist = new double[n];
        Arrays.fill(dist, Double.POSITIVE_INFINITY);
        dist[s] = 0.0;
        // Passo 2: fila de prioridade que sempre entrega o vertice de menor distancia {distancia, vertice}
        PriorityQueue<double[]> pq = new PriorityQueue<>((a, b) -> Double.compare(a[0], b[0]));
        pq.add(new double[]{0.0, s});
        while (!pq.isEmpty()) {
            // Passo 3: tira o vertice mais proximo ainda nao tratado
            double[] topo = pq.poll();
            int v = (int) topo[1];
            // se a distancia guardada na fila e velha (ja achamos uma melhor), ignora
            if (topo[0] > dist[v] + EPS) continue;
            // Passo 4: relaxamento: para cada vizinho w, testa se passar por v e mais curto
            for (double[] e : adj[v]) {
                int w = (int) e[0];
                double nd = dist[v] + e[1];
                if (nd < dist[w] - EPS) { dist[w] = nd; pq.add(new double[]{nd, w}); }
            }
        }
        // Passo 5: vertices inalcancaveis ficam com distancia infinita
        return dist;
    }

    // Centralidade de proximidade de cada no (considera a distancia do melhor caminho)
    static double[] proximidade() {
        // c[s] vai guardar a proximidade do vertice s
        double[] c = new double[n];
        // Passo 1: com 0 ou 1 vertice nao ha distancias para medir
        if (n <= 1) return c;
        for (int s = 0; s < n; s++) {
            // Passo 2: menores distancias de s para todos os outros
            double[] d = dijkstra(s);
            double soma = 0.0;
            int alc = 0;
            // Passo 3: soma as distancias e conta quantos vertices s consegue alcancar (sem contar ele mesmo)
            for (int i = 0; i < n; i++) {
                if (i != s && Double.isFinite(d[i])) { soma += d[i]; alc++; }
            }
            // Passo 4: formula de Wasserman-Faust: alc^2 / ((n-1) * soma).
            // Quanto menores as distancias (soma pequena) e mais vertices alcancados, maior o valor.
            // Quem nao alcanca ninguem fica com 0
            if (alc > 0 && soma > 0.0) c[s] = ((double) alc * alc) / ((n - 1.0) * soma);
        }
        return c;
    }

    // 8. Centralidade de Intermediacao - Brandes Ponderado
    // Mede o quanto cada no esta "no meio" dos melhores caminhos entre os outros
    @SuppressWarnings("unchecked")
    static double[] intermediacao() {
        // cb[v] acumula a intermediacao de cada vertice
        double[] cb = new double[n];
        // Com 2 vertices ou menos nenhum vertice fica no meio de um caminho
        if (n <= 2) return cb;
        // Repete o processo tendo cada vertice s como origem
        for (int s = 0; s < n; s++) {
            // pilha: vertices na ordem em que o Dijkstra os fecha (usada de tras para frente depois)
            ArrayList<Integer> pilha = new ArrayList<>();
            // pred[w]: vertices que vem imediatamente antes de w em algum melhor caminho de s ate w
            ArrayList<Integer>[] pred = new ArrayList[n];
            for (int i = 0; i < n; i++) pred[i] = new ArrayList<>();
            // dist: menor distancia desde s; sigma[w]: quantos melhores caminhos existem de s ate w
            double[] dist = new double[n], sigma = new double[n];
            // feito[v]: o vertice ja foi fechado pelo Dijkstra
            boolean[] feito = new boolean[n];
            Arrays.fill(dist, Double.POSITIVE_INFINITY);
            // Passo 1: s esta a distancia 0 e ha exatamente 1 caminho ate ele mesmo
            dist[s] = 0.0;
            sigma[s] = 1.0;
            PriorityQueue<double[]> pq = new PriorityQueue<>((a, b) -> Double.compare(a[0], b[0]));
            pq.add(new double[]{0.0, s});
            // Passo 2: Dijkstra que alem das distancias conta os melhores caminhos
            while (!pq.isEmpty()) {
                double[] topo = pq.poll();
                int v = (int) topo[1];
                // ignora entradas repetidas de vertices ja fechados
                if (feito[v]) continue;
                feito[v] = true;
                pilha.add(v);
                for (double[] e : adj[v]) {
                    int w = (int) e[0];
                    double nd = dist[v] + e[1];
                    if (nd < dist[w] - EPS) {
                        // achou um caminho estritamente melhor: zera os antigos e fica so com v como predecessor
                        dist[w] = nd; sigma[w] = sigma[v];
                        pred[w].clear(); pred[w].add(v);
                        pq.add(new double[]{nd, w});
                    } else if (Math.abs(nd - dist[w]) <= EPS && !feito[w]) {
                        // achou outro caminho de mesmo tamanho: soma os caminhos e guarda mais um predecessor
                        sigma[w] += sigma[v]; pred[w].add(v);
                    }
                }
            }
            // Passo 3: delta[v] = quanto v contribui para a intermediacao considerando s como origem
            double[] delta = new double[n];
            // Percorre os vertices do mais distante para o mais proximo de s
            for (int i = pilha.size() - 1; i >= 0; i--) {
                int w = pilha.get(i);
                // cada predecessor v de w recebe a parte dos caminhos de w que passa por ele
                for (int v : pred[w]) delta[v] += (sigma[v] / sigma[w]) * (1.0 + delta[w]);
                // o proprio s nao conta para si mesmo
                if (w != s) cb[w] += delta[w];
            }
        }
        // Passo 4: em grafo nao direcionado cada par foi contado duas vezes (A->B e B->A), entao divide por 2
        if (!dirigido) for (int i = 0; i < n; i++) cb[i] /= 2.0;
        return cb;
    }

    // 9. Gerador de Grafo Aleatorio
    // Gera um grafo nao direcionado com 'nos' vertices e 'numArestas' arestas, conexo ou nao
    static void gerarAleatorio(int nos, int numArestas, boolean conexo) {
        // Passo 1: validacoes das entradas
        if (nos <= 0) throw new IllegalArgumentException("Numero de nos deve ser positivo.");
        if (!conexo && nos < 2) throw new IllegalArgumentException("Grafo desconexo exige pelo menos 2 nos.");
        // Se for desconexo, o ultimo vertice ficara de fora (isolado), entao so 'nos - 1' vertices recebem arestas
        int usaveis = conexo ? nos : nos - 1;
        // Maximo de arestas sem repeticao entre 'usaveis' vertices: usaveis*(usaveis-1)/2
        long max = (long) usaveis * (usaveis - 1) / 2;
        if (numArestas < 0 || numArestas > max) throw new IllegalArgumentException("Arestas invalidas (maximo " + max + ").");
        if (conexo && numArestas < nos - 1) throw new IllegalArgumentException("Conexo exige pelo menos " + (nos - 1) + " arestas.");
        // Passo 2: cria o grafo vazio
        iniciar(nos, false);
        Random r = new Random();
        // pares ja usados, para nao repetir aresta (chave = menor * nos + maior)
        HashSet<Long> usadas = new HashSet<>();
        int criadas = 0;
        // Passo 3: se precisa ser conexo, cria primeiro uma arvore: cada vertice i liga a um vertice anterior sorteado.
        // Isso usa nos-1 arestas e ja garante que tudo esta conectado
        if (conexo) {
            for (int i = 1; i < nos; i++) {
                int j = r.nextInt(i);
                usadas.add((long) j * nos + i);
                addAresta(j, i, 1 + r.nextInt(50));
                criadas++;
            }
        }
        // Passo 4: completa com arestas sorteadas entre vertices usaveis, descartando lacos e pares repetidos
        while (criadas < numArestas) {
            int a = r.nextInt(usaveis), b = r.nextInt(usaveis);
            if (a == b) continue;
            int u = Math.min(a, b), v = Math.max(a, b);
            // usadas.add devolve false se o par ja existia
            if (usadas.add((long) u * nos + v)) { addAresta(u, v, 1 + r.nextInt(50)); criadas++; }
        }
    }

    // 10. Modelagem GTFS: stops.txt + trips.txt + stop_times.txt
    // Separa uma linha CSV em campos, sem quebrar nas virgulas que estao dentro de aspas
    static ArrayList<String> campos(String linha) {
        ArrayList<String> lista = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean aspas = false;
        // Passo 1: percorre a linha caractere por caractere
        for (char c : linha.toCharArray()) {
            // aspas: liga/desliga o modo "dentro de aspas" (a aspa em si nao e guardada)
            if (c == '"') aspas = !aspas;
            // virgula fora de aspas: fecha o campo atual
            else if (c == ',' && !aspas) { lista.add(sb.toString().trim()); sb.setLength(0); }
            // qualquer outro caractere faz parte do campo atual
            else sb.append(c);
        }
        // Passo 2: guarda o ultimo campo
        lista.add(sb.toString().trim());
        return lista;
    }

    // Distancia em metros entre dois pontos (latitude/longitude) pela formula de Haversine
    static double distancia(double la1, double lo1, double la2, double lo2) {
        // Passo 1: diferencas de latitude e longitude em radianos
        double dla = Math.toRadians(la2 - la1), dlo = Math.toRadians(lo2 - lo1);
        // Passo 2: formula de Haversine
        double a = Math.sin(dla / 2) * Math.sin(dla / 2)
                + Math.cos(Math.toRadians(la1)) * Math.cos(Math.toRadians(la2)) * Math.sin(dlo / 2) * Math.sin(dlo / 2);
        // Passo 3: converte para metros usando o raio da Terra (6.371.000 m)
        return 6371000.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // Constroi o grafo da rede de onibus: paradas = vertices, trechos entre paradas da mesma viagem = arcos
    static void construirRedeGTFS() throws IOException {
        // Passo 1: localiza os tres arquivos e exige que existam
        File fStops = resolverArquivo("stops.txt"), fTrips = resolverArquivo("trips.txt"), fTimes = resolverArquivo("stop_times.txt");
        if (!fStops.exists() || !fTrips.exists() || !fTimes.exists())
            throw new FileNotFoundException("Arquivos stops.txt, trips.txt e stop_times.txt sao obrigatorios.");

        // Passo 2: le o stops.txt. Cada parada vira um vertice.
        // nomes = rotulo; pos = {latitude, longitude}; stopIdx = de stop_id (texto) para o numero do vertice
        ArrayList<String> nomes = new ArrayList<>();
        ArrayList<double[]> pos = new ArrayList<>();
        HashMap<String, Integer> stopIdx = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fStops), StandardCharsets.UTF_8))) {
            // pula a primeira linha (cabecalho com os nomes das colunas)
            br.readLine();
            String l;
            while ((l = br.readLine()) != null) {
                if (l.trim().isEmpty()) continue;
                ArrayList<String> c = campos(l);
                // coluna 0 = stop_id, 2 = stop_name, 4 = latitude, 5 = longitude; ignora ids repetidos
                if (c.size() >= 6 && !stopIdx.containsKey(c.get(0))) {
                    stopIdx.put(c.get(0), nomes.size());
                    nomes.add(c.get(0) + " - " + c.get(2).replace('"', '\''));
                    pos.add(new double[]{Double.parseDouble(c.get(4)), Double.parseDouble(c.get(5))});
                }
            }
        }

        // Passo 3: le o trips.txt e escolhe UMA viagem por itinerario (shape_id).
        // Viagens com o mesmo shape passam pelas mesmas paradas, entao uma so representa todas.
        // shapes = itinerarios ja vistos; tripsSel = viagens escolhidas
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
                // coluna 7 = shape_id; se estiver vazio, usa linha+viagem como itinerario proprio
                String shape = (c.size() > 7 && !c.get(7).isEmpty()) ? c.get(7) : (c.get(0) + "-" + c.get(2));
                // shapes.add devolve true so na primeira vez que o shape aparece: essa viagem e a escolhida (coluna 2 = trip_id)
                if (shapes.add(shape)) tripsSel.add(c.get(2));
            }
        }
        System.out.println("Paradas (stops.txt): " + nomes.size() + " | Viagens (trips.txt): " + totalTrips + " (" + tripsSel.size() + " itinerarios)");

        // Passo 4: le o stop_times.txt guardando, para cada viagem escolhida, a lista {vertice da parada, stop_sequence}
        HashMap<String, ArrayList<int[]>> seqTrip = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fTimes), StandardCharsets.UTF_8))) {
            br.readLine();
            String l;
            while ((l = br.readLine()) != null) {
                if (l.trim().isEmpty()) continue;
                ArrayList<String> c = campos(l);
                // coluna 0 = trip_id, 3 = stop_id, 4 = stop_sequence (ordem da parada na viagem)
                if (c.size() >= 5 && tripsSel.contains(c.get(0)) && stopIdx.containsKey(c.get(3)))
                    seqTrip.computeIfAbsent(c.get(0), k -> new ArrayList<>()).add(new int[]{stopIdx.get(c.get(3)), Integer.parseInt(c.get(4))});
            }
        }

        // Passo 5: cria o grafo DIRECIONADO (onibus tem sentido) com um vertice por parada e coloca os nomes
        iniciar(nomes.size(), true);
        for (int i = 0; i < n; i++) rotulos[i] = nomes.get(i);
        // usadas: pares origem->destino ja criados, para nao repetir arco (so pares distintos)
        HashSet<Long> usadas = new HashSet<>();
        for (ArrayList<int[]> lista : seqTrip.values()) {
            // Passo 6: coloca as paradas da viagem na ordem correta (stop_sequence)
            lista.sort((a, b) -> Integer.compare(a[1], b[1]));
            for (int i = 0; i < lista.size() - 1; i++) {
                // u = parada de partida
                int u = lista.get(i)[0];
                double distAcum = 0.0;
                // Passo 7: liga u a cada uma das 5 paradas seguintes da mesma viagem
                for (int passo = 1; passo <= 5 && i + passo < lista.size(); passo++) {
                    // ant = parada anterior a v na viagem; v = parada de destino deste passo
                    int ant = lista.get(i + passo - 1)[0], v = lista.get(i + passo)[0];
                    // distancia em metros do trecho ant -> v (arredondada a 0,1 e no minimo 1)
                    double trecho = Math.max(1.0, Math.round(distancia(pos.get(ant)[0], pos.get(ant)[1], pos.get(v)[0], pos.get(v)[1]) * 10.0) / 10.0);
                    // o peso do arco u -> v e a soma dos trechos percorridos desde u
                    distAcum = Math.round((distAcum + trecho) * 10.0) / 10.0;
                    // cria o arco so se for entre paradas diferentes e se esse par ainda nao existir
                    if (u != v && usadas.add((long) u * n + v)) addAresta(u, v, distAcum);
                }
            }
        }
    }

    // Grava o valor calculado de cada no em um arquivo e mostra os 10 maiores na tela
    static void mostrarRanking(double[] valores, String arquivo) throws IOException {
        // Passo 1: grava uma linha por vertice: numero, rotulo entre aspas e valor
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(arquivo), StandardCharsets.UTF_8))) {
            for (int i = 0; i < n; i++) pw.println((i + 1) + " \"" + rotulos[i] + "\" " + valores[i]);
        }
        // Passo 2: ordena os vertices do maior para o menor valor
        Integer[] ordem = new Integer[n];
        for (int i = 0; i < n; i++) ordem[i] = i;
        Arrays.sort(ordem, (a, b) -> Double.compare(valores[b], valores[a]));
        // Passo 3: mostra os 10 primeiros na tela
        System.out.println("Top 10 (completo em " + arquivo + "):");
        for (int i = 0; i < Math.min(10, n); i++)
            System.out.printf(Locale.US, "%d. %s = %.8f%n", (i + 1), rotulos[ordem[i]], valores[ordem[i]]);
    }

    // Menu de texto: le a opcao digitada e chama a funcao correspondente
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        // Comeca com um grafo vazio
        iniciar(0, false);
        // Repete o menu ate o usuario escolher 0 (ou acabar a entrada)
        while (true) {
            // Mostra o resumo do grafo atual e as opcoes
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
            // Qualquer erro (arquivo inexistente, numero invalido...) e capturado no catch e mostrado, sem fechar o programa
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
