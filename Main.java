import java.io.*;
import java.util.*;

public class Main {
    static int n;
    static boolean dirigido;
    static String[] rotulos;
    static ArrayList<double[]> arestas = new ArrayList<>();
    static ArrayList<double[]>[] adj;
    static final double EPS = 1e-9;

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

    static void addAresta(int u, int v, double w) {
        arestas.add(new double[]{u, v, w});
        adj[u].add(new double[]{v, w});
        if (!dirigido) adj[v].add(new double[]{u, w});
    }

    static void salvar(String arquivo) throws IOException {
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(arquivo), "UTF-8"));
        pw.println("*Vertices " + n);
        for (int i = 0; i < n; i++) pw.println((i + 1) + " \"" + rotulos[i] + "\"");
        pw.println(dirigido ? "*Arcs" : "*Edges");
        for (double[] a : arestas) pw.println(((int) a[0] + 1) + " " + ((int) a[1] + 1) + " " + a[2]);
        pw.close();
    }

    static void carregar(String arquivo) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(arquivo), "UTF-8"));
        String linha = br.readLine();
        iniciar(Integer.parseInt(linha.trim().split("\\s+")[1]), false);
        boolean lendoVertices = true;
        while ((linha = br.readLine()) != null) {
            linha = linha.trim();
            if (linha.isEmpty()) continue;
            if (linha.startsWith("*")) {
                lendoVertices = false;
                dirigido = linha.toLowerCase().startsWith("*arcs");
                continue;
            }
            if (lendoVertices) {
                int p = linha.indexOf('"');
                int q = linha.lastIndexOf('"');
                int id = Integer.parseInt(linha.substring(0, p).trim());
                rotulos[id - 1] = linha.substring(p + 1, q);
            } else {
                String[] t = linha.split("\\s+");
                double w = t.length > 2 ? Double.parseDouble(t[2]) : 1;
                addAresta(Integer.parseInt(t[0]) - 1, Integer.parseInt(t[1]) - 1, w);
            }
        }
        br.close();
    }

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

    @SuppressWarnings("unchecked")
    static ArrayList<Integer>[] vizinhosFracos() {
        ArrayList<Integer>[] viz = new ArrayList[n];
        for (int i = 0; i < n; i++) viz[i] = new ArrayList<>();
        for (double[] a : arestas) {
            viz[(int) a[0]].add((int) a[1]);
            viz[(int) a[1]].add((int) a[0]);
        }
        return viz;
    }

    static int contar(int[] comp) {
        int max = -1;
        for (int c : comp) max = Math.max(max, c);
        return max + 1;
    }

    static boolean conexo() {
        return n == 0 || contar(componentes()) == 1;
    }

    static void mostrarComponentes() {
        int[] comp = componentes();
        int total = contar(comp);
        if (total == 1) {
            System.out.println("Grafo conexo, so existe 1 componente.");
            return;
        }
        System.out.println((dirigido ? "Componentes fracamente conectados: " : "Componentes: ") + total);
        int[] tam = new int[total];
        for (int c : comp) tam[c]++;
        for (int c = 0; c < total; c++) {
            if (c >= 30) {
                System.out.println("... (" + (total - 30) + " componentes omitidos)");
                break;
            }
            System.out.print("Componente " + (c + 1) + " (" + tam[c] + " nos)");
            if (tam[c] <= 15) {
                System.out.print(":");
                for (int i = 0; i < n; i++) if (comp[i] == c) System.out.print(" " + (i + 1));
            }
            System.out.println();
        }
    }

    static boolean euleriano() {
        int[] entrada = new int[n];
        int[] saida = new int[n];
        for (double[] a : arestas) {
            saida[(int) a[0]]++;
            entrada[(int) a[1]]++;
        }
        int[] comp = componentes();
        int alvo = -1;
        for (int i = 0; i < n; i++) {
            if (dirigido) {
                if (entrada[i] != saida[i]) return false;
            } else {
                if ((entrada[i] + saida[i]) % 2 != 0) return false;
            }
            if (entrada[i] + saida[i] > 0) {
                if (alvo == -1) alvo = comp[i];
                else if (comp[i] != alvo) return false;
            }
        }
        return alvo != -1;
    }

    static int achar(int[] pai, int x) {
        while (pai[x] != x) {
            pai[x] = pai[pai[x]];
            x = pai[x];
        }
        return x;
    }

    static boolean ciclico() {
        if (!dirigido) {
            int[] pai = new int[n];
            for (int i = 0; i < n; i++) pai[i] = i;
            for (double[] a : arestas) {
                int x = achar(pai, (int) a[0]);
                int y = achar(pai, (int) a[1]);
                if (x == y) return true;
                pai[x] = y;
            }
            return false;
        }
        int[] grauEntrada = new int[n];
        for (double[] a : arestas) grauEntrada[(int) a[1]]++;
        ArrayDeque<Integer> fila = new ArrayDeque<>();
        for (int i = 0; i < n; i++) if (grauEntrada[i] == 0) fila.add(i);
        int removidos = 0;
        while (!fila.isEmpty()) {
            int v = fila.poll();
            removidos++;
            for (double[] e : adj[v]) {
                if (--grauEntrada[(int) e[0]] == 0) fila.add((int) e[0]);
            }
        }
        return removidos < n;
    }

    static double[] dijkstra(int s) {
        double[] dist = new double[n];
        Arrays.fill(dist, Double.MAX_VALUE);
        dist[s] = 0;
        PriorityQueue<double[]> pq = new PriorityQueue<>((a, b) -> Double.compare(a[0], b[0]));
        pq.add(new double[]{0, s});
        while (!pq.isEmpty()) {
            double[] topo = pq.poll();
            int v = (int) topo[1];
            if (topo[0] > dist[v]) continue;
            for (double[] e : adj[v]) {
                int w = (int) e[0];
                double nd = dist[v] + e[1];
                if (nd < dist[w]) {
                    dist[w] = nd;
                    pq.add(new double[]{nd, w});
                }
            }
        }
        return dist;
    }

    static double[] proximidade() {
        double[] c = new double[n];
        for (int s = 0; s < n; s++) {
            double[] d = dijkstra(s);
            double soma = 0;
            int alcancados = 0;
            for (int i = 0; i < n; i++) {
                if (i != s && d[i] != Double.MAX_VALUE) {
                    soma += d[i];
                    alcancados++;
                }
            }
            c[s] = soma > 0 ? alcancados / soma : 0;
        }
        return c;
    }

    @SuppressWarnings("unchecked")
    static double[] intermediacao() {
        double[] cb = new double[n];
        for (int s = 0; s < n; s++) {
            ArrayList<Integer> pilha = new ArrayList<>();
            ArrayList<Integer>[] pred = new ArrayList[n];
            for (int i = 0; i < n; i++) pred[i] = new ArrayList<>();
            double[] dist = new double[n];
            double[] sigma = new double[n];
            boolean[] feito = new boolean[n];
            Arrays.fill(dist, Double.MAX_VALUE);
            dist[s] = 0;
            sigma[s] = 1;
            PriorityQueue<double[]> pq = new PriorityQueue<>((a, b) -> Double.compare(a[0], b[0]));
            pq.add(new double[]{0, s});
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
                        dist[w] = nd;
                        sigma[w] = sigma[v];
                        pred[w].clear();
                        pred[w].add(v);
                        pq.add(new double[]{nd, w});
                    } else if (Math.abs(nd - dist[w]) <= EPS && !feito[w]) {
                        sigma[w] += sigma[v];
                        pred[w].add(v);
                    }
                }
            }
            double[] delta = new double[n];
            for (int i = pilha.size() - 1; i >= 0; i--) {
                int w = pilha.get(i);
                for (int v : pred[w]) delta[v] += sigma[v] / sigma[w] * (1 + delta[w]);
                if (w != s) cb[w] += delta[w];
            }
        }
        if (!dirigido) for (int i = 0; i < n; i++) cb[i] /= 2;
        return cb;
    }

    static void gerarAleatorio(int nos, int numArestas, boolean conexo) {
        iniciar(nos, false);
        Random r = new Random();
        HashSet<Long> usadas = new HashSet<>();
        int usaveis = conexo ? nos : nos - 1;
        long maximo = (long) usaveis * (usaveis - 1) / 2;
        if (numArestas > maximo) throw new IllegalArgumentException("Arestas demais para esse numero de nos (maximo " + maximo + ")");
        if (conexo && numArestas < nos - 1) throw new IllegalArgumentException("Para ser conexo precisa de pelo menos " + (nos - 1) + " arestas");
        int criadas = 0;
        if (conexo) {
            for (int i = 1; i < nos; i++) {
                int j = r.nextInt(i);
                usadas.add((long) j * nos + i);
                addAresta(j, i, 1 + r.nextInt(20));
                criadas++;
            }
        }
        while (criadas < numArestas) {
            int a = r.nextInt(usaveis);
            int b = r.nextInt(usaveis);
            if (a == b) continue;
            int u = Math.min(a, b);
            int v = Math.max(a, b);
            if (!usadas.add((long) u * nos + v)) continue;
            addAresta(u, v, 1 + r.nextInt(20));
            criadas++;
        }
    }

    static ArrayList<String> campos(String linha) {
        ArrayList<String> lista = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean aspas = false;
        for (char c : linha.toCharArray()) {
            if (c == '"') aspas = !aspas;
            else if (c == ',' && !aspas) {
                lista.add(sb.toString());
                sb.setLength(0);
            } else sb.append(c);
        }
        lista.add(sb.toString());
        return lista;
    }

    static double distancia(double la1, double lo1, double la2, double lo2) {
        double dla = Math.toRadians(la2 - la1);
        double dlo = Math.toRadians(lo2 - lo1);
        double a = Math.sin(dla / 2) * Math.sin(dla / 2)
                + Math.cos(Math.toRadians(la1)) * Math.cos(Math.toRadians(la2)) * Math.sin(dlo / 2) * Math.sin(dlo / 2);
        return 6371000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    static void construirRedeOnibus(String arquivo, int k) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(arquivo), "UTF-8"));
        br.readLine();
        ArrayList<String> nomes = new ArrayList<>();
        ArrayList<double[]> pos = new ArrayList<>();
        String linha;
        while ((linha = br.readLine()) != null) {
            if (linha.trim().isEmpty()) continue;
            ArrayList<String> c = campos(linha);
            nomes.add(c.get(0) + " - " + c.get(2).replace('"', '\''));
            pos.add(new double[]{Double.parseDouble(c.get(4)), Double.parseDouble(c.get(5))});
        }
        br.close();
        iniciar(nomes.size(), false);
        for (int i = 0; i < n; i++) rotulos[i] = nomes.get(i);
        HashSet<Long> usadas = new HashSet<>();
        for (int i = 0; i < n; i++) {
            int[] viz = new int[k];
            double[] dv = new double[k];
            Arrays.fill(dv, Double.MAX_VALUE);
            for (int j = 0; j < n; j++) {
                if (i == j) continue;
                double dx = pos.get(i)[0] - pos.get(j)[0];
                double dy = pos.get(i)[1] - pos.get(j)[1];
                double d = dx * dx + dy * dy;
                if (d >= dv[k - 1]) continue;
                int p = k - 1;
                while (p > 0 && dv[p - 1] > d) {
                    dv[p] = dv[p - 1];
                    viz[p] = viz[p - 1];
                    p--;
                }
                dv[p] = d;
                viz[p] = j;
            }
            for (int t = 0; t < k; t++) {
                int j = viz[t];
                int u = Math.min(i, j);
                int v = Math.max(i, j);
                if (!usadas.add((long) u * n + v)) continue;
                double m = distancia(pos.get(u)[0], pos.get(u)[1], pos.get(v)[0], pos.get(v)[1]);
                addAresta(u, v, Math.round(m * 10) / 10.0);
            }
        }
    }

    static void mostrarRanking(double[] valores, String arquivo) throws IOException {
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(arquivo), "UTF-8"));
        for (int i = 0; i < n; i++) pw.println((i + 1) + " \"" + rotulos[i] + "\" " + valores[i]);
        pw.close();
        Integer[] ordem = new Integer[n];
        for (int i = 0; i < n; i++) ordem[i] = i;
        Arrays.sort(ordem, (a, b) -> Double.compare(valores[b], valores[a]));
        System.out.println("Top 10 (resultado completo em " + arquivo + "):");
        for (int i = 0; i < Math.min(10, n); i++) System.out.println((i + 1) + ". " + rotulos[ordem[i]] + " = " + valores[ordem[i]]);
    }

    public static void main(String[] args) throws IOException {
        Scanner sc = new Scanner(System.in);
        iniciar(0, false);
        while (true) {
            System.out.println();
            System.out.println("Grafo atual: " + n + " nos, " + arestas.size() + " arestas, " + (dirigido ? "direcionado" : "nao direcionado"));
            System.out.println("1 Construir rede de onibus (stops.txt)");
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
            if (!sc.hasNext()) break;
            String op = sc.next();
            try {
                if (op.equals("0")) break;
                else if (op.equals("1")) {
                    construirRedeOnibus("stops.txt", 8);
                    System.out.println("Rede criada.");
                } else if (op.equals("2")) {
                    System.out.print("Arquivo: ");
                    carregar(sc.next());
                    System.out.println("Grafo carregado.");
                } else if (op.equals("3")) {
                    System.out.print("Arquivo: ");
                    salvar(sc.next());
                    System.out.println("Grafo salvo.");
                } else if (op.equals("4")) {
                    System.out.print("Nos: ");
                    int a = sc.nextInt();
                    System.out.print("Arestas: ");
                    int b = sc.nextInt();
                    System.out.print("Conexo (s/n): ");
                    boolean c = sc.next().equalsIgnoreCase("s");
                    gerarAleatorio(a, b, c);
                    System.out.println("Grafo gerado.");
                } else if (op.equals("5")) System.out.println(conexo() ? "O grafo e conexo." : "O grafo e desconexo.");
                else if (op.equals("6")) mostrarComponentes();
                else if (op.equals("7")) System.out.println(euleriano() ? "O grafo e Euleriano." : "O grafo nao e Euleriano.");
                else if (op.equals("8")) System.out.println(ciclico() ? "O grafo e ciclico." : "O grafo e aciclico.");
                else if (op.equals("9")) mostrarRanking(proximidade(), "proximidade.txt");
                else if (op.equals("10")) mostrarRanking(intermediacao(), "intermediacao.txt");
                else System.out.println("Opcao invalida.");
            } catch (Exception e) {
                System.out.println("Erro: " + e.getMessage());
            }
        }
    }
}
