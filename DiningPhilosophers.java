import java.util.Random;

public class DiningPhilosophers {
    public static void main(String[] args) {
        final int N = 5;                    // número de filósofos
        Table table = new Table(N);

        Philosopher[] philosophers = new Philosopher[N];
        for (int i = 0; i < N; i++) {
            philosophers[i] = new Philosopher(i, table);
            philosophers[i].start();
        }

        // opcional: deixar rodar por um tempo e depois interromper
        try {
            Thread.sleep(20000); // executa por 20s (mude se desejar)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // interrompe as threads (apenas para finalizar o exemplo)
        for (Philosopher p : philosophers) {
            p.shutdown();
        }
    }
}

/**
 * Monitor (mesa) que gerencia o estado dos filósofos e os hashis.
 * Usa um array 'state' com estados: THINKING, HUNGRY, EATING.
 * Métodos pickUp e putDown são synchronized (monitor).
 */
class Table {
    enum State { THINKING, HUNGRY, EATING }

    private final State[] state;
    private final int n;

    public Table(int n) {
        this.n = n;
        state = new State[n];
        for (int i = 0; i < n; i++) state[i] = State.THINKING;
    }

    // índice do vizinho à esquerda
    private int left(int i) {
        return (i + n - 1) % n;
    }

    // índice do vizinho à direita
    private int right(int i) {
        return (i + 1) % n;
    }

    /**
     * Filósofo i tenta pegar os dois hashis (entrar em EATING).
     * Se não for possível, espera (wait).
     */
    public synchronized void pickUp(int i) throws InterruptedException {
        state[i] = State.HUNGRY;
        test(i); // tenta entrar em EATING
        while (state[i] != State.EATING) {
            wait();
        }
    }

    /**
     * Filósofo i larga os hashis (volta a THINKING) e "acorda" vizinhos
     * que possam estar aguardando.
     */
    public synchronized void putDown(int i) {
        state[i] = State.THINKING;
        // tenta acordar vizinhos, pois agora os recursos podem estar disponíveis
        test(left(i));
        test(right(i));
        // acorda possíveis threads esperando
        notifyAll();
    }

    /**
     * Se i está HUNGRY e ambos os vizinhos não estão EATING,
     * então i pode passar a EATING.
     */
    private void test(int i) {
        if (state[i] == State.HUNGRY &&
            state[left(i)] != State.EATING &&
            state[right(i)] != State.EATING) {
            state[i] = State.EATING;
        }
    }

    // Método auxiliar para log do estado (opcional)
    public synchronized String snapshot() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(i).append(":").append(state[i].name()).append(" ");
        }
        return sb.toString();
    }
}

/**
 * Thread que simula o filósofo: pensa, pede hashis, come e devolve hashis.
 */
class Philosopher extends Thread {
    private final int id;
    private final Table table;
    private final Random rnd = new Random();
    private volatile boolean running = true;

    public Philosopher(int id, Table table) {
        this.id = id;
        this.table = table;
        setName("Filosofo-" + id);
    }

    @Override
    public void run() {
        try {
            while (running && !isInterrupted()) {
                // Pensar
                System.out.printf("[%s] pensando...%n", getName());
                sleepRandom(500, 2000);

                // Ficar com fome e tentar pegar os hashis
                System.out.printf("[%s] com fome, tentando pegar hashis...%n", getName());
                table.pickUp(id);

                // Comer
                System.out.printf("[%s] comendo! (%s)%n", getName(), table.snapshot());
                sleepRandom(400, 1500);

                // Devolver hashis
                table.putDown(id);
                System.out.printf("[%s] terminou de comer e devolveu os hashis.%n", getName());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.printf("[%s] finalizando.%n", getName());
    }

    private void sleepRandom(int minMs, int maxMs) throws InterruptedException {
        int t = rnd.nextInt(maxMs - minMs + 1) + minMs;
        Thread.sleep(t);
    }

    // para encerrar a thread no main de forma controlada
    public void shutdown() {
        running = false;
        interrupt();
    }
}
