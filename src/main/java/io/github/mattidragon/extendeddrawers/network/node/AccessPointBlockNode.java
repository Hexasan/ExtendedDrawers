package io.github.mattidragon.extendeddrawers.network.node;

import com.kneelawk.graphlib.api.graph.user.BlockNodeType;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import static io.github.mattidragon.extendeddrawers.ExtendedDrawers.id;

public class AccessPointBlockNode implements DrawerNetworkBlockNode {
    public static final Identifier ID = id("access_point");
    public static final AccessPointBlockNode INSTANCE = new AccessPointBlockNode();
    public static final BlockNodeType TYPE = BlockNodeType.of(ID, () -> INSTANCE);

    private AccessPointBlockNode() {
    }

    @Override
    public @NotNull BlockNodeType getType() {
        return TYPE;
    }
}
