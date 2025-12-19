package fr.asdep.labgen.mc;

public class BlockState {
    private final String id;
    private final int meta;

    public BlockState(String id) {
        this(id, 0);
    }

    public BlockState(String id, int meta) {
        this.id = id;
        this.meta = meta;
    }

    public String getId() {
        return id;
    }

    public int getMeta() {
        return meta;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockState that = (BlockState) o;
        return meta == that.meta && java.util.Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, meta);
    }

    @Override
    public String toString() {
        return id + (meta != 0 ? ":" + meta : "");
    }
}
