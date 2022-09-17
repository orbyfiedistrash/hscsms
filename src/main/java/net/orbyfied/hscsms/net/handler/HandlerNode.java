package net.orbyfied.hscsms.net.handler;

import net.orbyfied.hscsms.net.NetworkHandler;
import net.orbyfied.hscsms.net.Packet;
import net.orbyfied.hscsms.net.PacketType;
import net.orbyfied.j8.util.functional.TriPredicate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HandlerNode {

    public enum Chain {
        CONTINUE,
        HALT
    }

    public enum NodeAction {
        REMOVE,
        KEEP
    }

    public static class Result {

        Chain chain;
        NodeAction nodeAction = NodeAction.KEEP;

        public Result(Chain chain) {
            this.chain = chain;
        }

        public Chain chain() {
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

        Result handle(NetworkHandler handler, HandlerNode node, Packet packet);

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
        node.predicate = (handler, node1, packet) -> packet.getType() == type;
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

    public Result handle(NetworkHandler handler, Packet packet) {
        // temp result
        Result result;

        // call these handlers
        for (Handler<? extends Packet> h : handlers) {
            if ((result = h.handle(handler, this, packet)).chain() == Chain.HALT)
                return result;
            if (result.nodeAction() == NodeAction.REMOVE)
                remove();
        }

        // try and get fast mapped children
        HandlerNode child;
        if ((child = childDirectPredicateType.get(packet.getType())) != null) {
            if ((result = child.handle(handler, packet)).chain() == Chain.HALT)
                return result;
        }

        // iterate children and call
        for (HandlerNode node : children) {
            if (node.predicate.test(handler, node, packet))
                if ((result = node.handle(handler, packet)).chain() == Chain.HALT)
                    return result;
        }

        // return
        return new Result(Chain.CONTINUE);
    }

}
