package fr.asdep.labgen.utils;

public class ProgressBar {
    private static final ThreadLocal<TriConsumer<String, Integer, Integer>> listener = new ThreadLocal<>();
    private final String taskName;
    private final int total;
    private int current;
    private int lastBarWidth = -1;

    public ProgressBar(String taskName, int total) {
        this.taskName = taskName;
        this.total = total;
        this.current = 0;
        if (total > 0) {
            print();
            notifyListener();
        }
    }

    public static void setListener(TriConsumer<String, Integer, Integer> listener) {
        ProgressBar.listener.set(listener);
    }

    public static void clearListener() {
        ProgressBar.listener.remove();
    }

    public synchronized void update(int value) {
        this.current = value;
        print();
        notifyListener();
    }

    public synchronized void step() {
        this.current++;
        print();
        notifyListener();
    }

    private void notifyListener() {
        TriConsumer<String, Integer, Integer> l = listener.get();
        if (l != null && total > 0) {
            l.accept(taskName, current, total);
        }
    }

    @FunctionalInterface
    public interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }

    private synchronized void print() {
        if (total <= 0) return;
        int width = 50;
        float progress = Math.min(1.0f, (float) current / total);
        int filledWidth = (int) (width * progress);
        int percent = (int) (progress * 100);

        if (filledWidth == lastBarWidth && percent != 100 && current != total) return;
        lastBarWidth = filledWidth;

        StringBuilder sb = new StringBuilder("\r");
        sb.append(String.format("%-20s", taskName)).append(" [");
        for (int i = 0; i < width; i++) {
            if (i < filledWidth) sb.append("#");
            else sb.append("-");
        }
        sb.append("] ")
                .append(String.format("%3d%%", percent))
                .append(" (")
                .append(current)
                .append("/")
                .append(total)
                .append(")");

        System.out.print(sb);
        System.out.flush();
        if (current >= total) {
            System.out.println();
        }
    }
}
