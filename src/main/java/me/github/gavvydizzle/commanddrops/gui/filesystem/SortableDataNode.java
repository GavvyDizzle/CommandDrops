package me.github.gavvydizzle.commanddrops.gui.filesystem;

import com.github.mittenmc.serverutils.gui.filesystem.tree.ItemNode;
import com.github.mittenmc.serverutils.gui.filesystem.tree.Node;
import com.github.mittenmc.serverutils.gui.pages.ItemGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SortableDataNode<E extends Comparable<? super E> & ItemGenerator> extends ItemNode<E> {

    public SortableDataNode(@Nullable Node parent, String name, @NotNull E item) {
        super(parent, name, item);
    }

    @Override
    public int compareTo(@NotNull Node o) {
        if (o instanceof SortableDataNode<?> sortableDataNode && sortableDataNode.getData().getClass().equals(this.getData().getClass())) {
            @SuppressWarnings("unchecked")
            SortableDataNode<E> other = (SortableDataNode<E>) sortableDataNode;
            return this.getData().compareTo(other.getData());
        }
        return super.compareTo(o);
    }
}
