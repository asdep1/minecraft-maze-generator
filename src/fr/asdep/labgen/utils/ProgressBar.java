package fr.asdep.labgen.utils;

public class ProgressBar {
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
        }
    }

    public synchronized void update(int value) {
        this.current = value;
        print();
    }

    public synchronized void step() {
        this.current++;
        print();
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

        System.out.print(sb.toString());
        System.out.flush();
        if (current >= total) {
            System.out.println();
        }
    }
}
