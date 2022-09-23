package net.orbyfied.hscsms.network.handler;

import net.orbyfied.hscsms.network.NetworkHandler;
import net.orbyfied.hscsms.network.Packet;
import net.orbyfied.hscsms.network.PacketType;
import net.orbyfied.j8.util.functional.TriPredicate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("rawtypes")
public class HandlerNode {

    public static class Result {

        ChainAction chain;
        NodeAction nodeAction = NodeAction.KEEP;

        public Result(ChainAction chain) {
            this.chain = chain;
        }

        public ChainAction chain() {
            return chain;
        }

        public NodeAction nodeAction() {
            return nodeAction;
        }

        public Result nodeAction(NodeAction action) {
            this.nodeAction = action;
            return this;
        }

    }

    public interface Handler<P extends Packet> {

        Result handle(NetworkHandler handler, HandlerNode node, P packet);

    }

    ///////////////////////////////

    public HandlerNode(HandlerNode parent) {
        this.parent = parent;
    }

    // the parent of this node
    final HandlerNode parent;

    // the children of this node
    List<HandlerNode> children = new ArrayList<>();
    Map<PacketType<? extends Packet>, HandlerNode> childDirectPredicateType
            = new HashMap<>();

    // the predicate
    TriPredicate<NetworkHandler, HandlerNode, Packet> predicate;
    // direct predicate: type
    // can be mapped instantly
    PacketType<? extends Packet> directPredicateType;

    // the handler
    List<Handler<? extends Packet>> handlers = new ArrayList<>();

    public HandlerNode childWhen(TriPredicate<NetworkHandler, HandlerNode, Packet> predicate) {
        HandlerNode node = new HandlerNode(this);
        node.predicate = predicate;
        children.add(node);
        return node;
    }

    public HandlerNode childForType(final PacketType<? extends Packet> type) {
        HandlerNode node = new HandlerNode(this);
        node.directPredicateType = type;
        node.predicate = (handler, node1, packet) -> packet.type() == type;
        children.add(node);
        childDirectPredicateType.put(type, node);
        return node;
    }

    public <P extends Packet> HandlerNode withHandler(Handler<P> handler) {
        handlers.add(handler);
        return this;
    }

    public void remove() {
        // remove node from parent
        if (parent != null) {
            parent.children.remove(this);
            if (directPredicateType != null) {
                parent.childDirectPredicateType.remove(directPredicateType);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public Result handle(NetworkHandler handler, Packet packet) {
        // check
        if (packet == null)
            return new Result(ChainAction.HALT);

        // temp result
        Result result;

        // call these handlers
        for (Handler h : handlers) {
            if ((result = h.handle(handler, this, packet)).chain() == ChainAction.HALT)
                return result;
            if (result.nodeAction() == NodeAction.REMOVE)
                remove();
        }

        // try and get fast mapped children
        HandlerNode child;
        if ((child = childDirectPredicateType.get(packet.type())) != null) {
            if ((result = child.handle(handler, packet)).chain() == ChainAction.HALT)
                return result;
        }

        // iterate children and call
        for (HandlerNode node : children) {
            if (node.predicate.test(handler, node, packet))
                if ((result = node.handle(handler, packet)).chain() == ChainAction.HALT)
                    return result;
        }

        // return
        return new Result(ChainAction.CONTINUE);
    }

}
