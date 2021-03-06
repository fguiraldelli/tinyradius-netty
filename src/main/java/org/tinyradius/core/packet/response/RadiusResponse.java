package org.tinyradius.core.packet.response;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.AttributeHolder;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.PacketType;
import org.tinyradius.core.packet.RadiusPacket;

import java.util.List;

public interface RadiusResponse extends RadiusPacket<RadiusResponse> {

    static RadiusResponse create(Dictionary dictionary, ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException {
        switch (header.getByte(0)) {
            case PacketType.ACCESS_ACCEPT:
                return new AccessResponse.Accept(dictionary, header, attributes);
            case PacketType.ACCESS_REJECT:
                return new AccessResponse.Reject(dictionary, header, attributes);
            case PacketType.ACCESS_CHALLENGE:
                return new AccessResponse.Challenge(dictionary, header, attributes);
            default:
                return new GenericResponse(dictionary, header, attributes);
        }
    }

    /**
     * Creates a RadiusPacket object. Depending on the passed type, an
     * appropriate packet is created. Also sets the type, and the
     * the packet id.
     *
     * @param dictionary    custom dictionary to use
     * @param type          packet type
     * @param id            packet id
     * @param authenticator authenticator for packet, nullable
     * @param attributes    list of attributes for packet
     * @return RadiusPacket object
     */
    static RadiusResponse create(Dictionary dictionary, byte type, byte id, byte[] authenticator, List<RadiusAttribute> attributes) throws RadiusPacketException {
        final ByteBuf header = RadiusPacket.buildHeader(type, id, authenticator, attributes);
        return create(dictionary, header, attributes);
    }


    /**
     * Reads a response from the given input stream and
     * creates an appropriate RadiusPacket/subclass.
     * <p>
     * Decodes the encrypted fields and attributes of the packet, and checks
     * authenticator if appropriate.
     *
     * @param dictionary dictionary to use for attributes
     * @param datagram   DatagramPacket to read packet from
     * @return new RadiusPacket object
     * @throws RadiusPacketException malformed packet
     */
    static RadiusResponse fromDatagram(Dictionary dictionary, DatagramPacket datagram) throws RadiusPacketException {
        // use unpooled heap so we can use ByteBuf freely later without worrying about GC
        // todo use original directBuffer with zero copy

        final ByteBuf byteBuf = Unpooled.copiedBuffer(datagram.content());

        return RadiusResponse.create(dictionary, RadiusPacket.readHeader(byteBuf),
                AttributeHolder.readAttributes(dictionary, -1, byteBuf));
    }

    /**
     * Encode and generate authenticator.
     * <p>
     * Requires request authenticator to generate response authenticator.
     * <p>
     * Must be idempotent.
     *
     * @param sharedSecret shared secret to be used to encode this packet
     * @param requestAuth  request packet authenticator
     * @return new RadiusPacket instance with same properties and valid authenticator
     * @throws RadiusPacketException errors encoding packet
     */
    RadiusResponse encodeResponse(String sharedSecret, byte[] requestAuth) throws RadiusPacketException;

    /**
     * Decodes the response against the supplied shared secret and request authenticator.
     * <p>
     * Must be idempotent.
     *
     * @param sharedSecret shared secret
     * @param requestAuth  authenticator for corresponding request
     * @return verified RadiusResponse with decoded attributes if appropriate
     * @throws RadiusPacketException errors verifying or decoding packet
     */
    RadiusResponse decodeResponse(String sharedSecret, byte[] requestAuth) throws RadiusPacketException;
}
